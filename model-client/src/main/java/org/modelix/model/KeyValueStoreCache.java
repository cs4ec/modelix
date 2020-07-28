package org.modelix.model;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import java.util.Set;
import jetbrains.mps.internal.collections.runtime.SetSequence;
import java.util.HashSet;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.IMapping;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import org.modelix.model.persistent.HashUtil;
import jetbrains.mps.internal.collections.runtime.Sequence;
import java.util.LinkedHashMap;
import java.util.Iterator;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;
import jetbrains.mps.internal.collections.runtime.IVisitor;

public class KeyValueStoreCache implements IKeyValueStore {
  private static final Logger LOG = LogManager.getLogger(KeyValueStoreCache.class);

  private IKeyValueStore store;
  private SynchronizedSLRUMap<String, String> cache = new SynchronizedSLRUMap<String, String>(300000, 300000, true);
  private final Set<String> pendingPrefetches = SetSequence.fromSet(new HashSet<String>());
  private final List<GetRequest> activeRequests = ListSequence.fromList(new ArrayList<GetRequest>());

  public KeyValueStoreCache(IKeyValueStore store) {
    this.store = store;
  }

  @Override
  public void prefetch(String rootKey) {
    Set<String> processedKeys = SetSequence.fromSet(new HashSet<String>());
    SetSequence.fromSet(processedKeys).addElement(rootKey);
    List<String> newKeys = ListSequence.fromListAndArray(new ArrayList<String>(), rootKey);
    while (ListSequence.fromList(newKeys).isNotEmpty() && SetSequence.fromSet(processedKeys).count() + ListSequence.fromList(newKeys).count() <= 100000) {
      synchronized (pendingPrefetches) {
        ListSequence.fromList(newKeys).removeSequence(SetSequence.fromSet(pendingPrefetches));
      }
      List<String> currentKeys = newKeys;
      newKeys = ListSequence.fromList(new ArrayList<String>());
      Map<String, String> loadedEntries;
      synchronized (pendingPrefetches) {
        SetSequence.fromSet(pendingPrefetches).addSequence(ListSequence.fromList(currentKeys));
      }
      try {
        loadedEntries = getAll(currentKeys);
        for (IMapping<String, String> entry : MapSequence.fromMap(loadedEntries)) {
          SetSequence.fromSet(processedKeys).addElement(entry.key());
          for (String childKey : HashUtil.extractSha256(entry.value())) {
            if (SetSequence.fromSet(processedKeys).contains(childKey)) {
              continue;
            }
            ListSequence.fromList(newKeys).addElement(childKey);
          }
        }
      } finally {
        SetSequence.fromSet(pendingPrefetches).removeSequence(ListSequence.fromList(currentKeys));
      }
    }
  }

  @Override
  public String get(String key) {
    return MapSequence.fromMap(getAll(Sequence.<String>singleton(key))).get(key);
  }

  @Override
  public Map<String, String> getAll(Iterable<String> keys_) {
    List<String> remainingKeys = ListSequence.fromListWithValues(new ArrayList<String>(), keys_);
    Map<String, String> result = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));
    synchronized (cache) {
      Iterator<String> itr = ListSequence.fromList(remainingKeys).iterator();
      while (itr.hasNext()) {
        String key = itr.next();
        String value = cache.get(key);
        // always put even if null to have the same order in the linked hash map as in the input 
        MapSequence.fromMap(result).put(key, value);
        if (value != null) {
          itr.remove();
        }
      }
    }

    if (ListSequence.fromList(remainingKeys).isNotEmpty()) {
      List<GetRequest> requiredRequest = ListSequence.fromList(new ArrayList<GetRequest>());
      GetRequest newRequest = null;
      synchronized (activeRequests) {
        for (final GetRequest r : ListSequence.fromList(activeRequests)) {
          if (ListSequence.fromList(remainingKeys).any(new IWhereFilter<String>() {
            public boolean accept(String it) {
              return SetSequence.fromSet(r.keys).contains(it);
            }
          })) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Reusing an active request: " + SetSequence.fromSet(r.keys).intersect(ListSequence.fromList(remainingKeys)).first() + " (" + SetSequence.fromSet(r.keys).intersect(ListSequence.fromList(remainingKeys)).count() + ")");
            }
            ListSequence.fromList(requiredRequest).addElement(r);
            ListSequence.fromList(remainingKeys).removeSequence(SetSequence.fromSet(r.keys));
          }
        }
        if (ListSequence.fromList(remainingKeys).isNotEmpty()) {
          newRequest = new GetRequest(SetSequence.fromSetWithValues(new HashSet<String>(), remainingKeys));
          ListSequence.fromList(requiredRequest).addElement(newRequest);
          ListSequence.fromList(activeRequests).addElement(newRequest);
        }
      }

      if (newRequest != null) {
        try {
          newRequest.execute();
        } finally {
          synchronized (activeRequests) {
            ListSequence.fromList(activeRequests).removeElement(newRequest);
          }
        }
      }

      for (GetRequest req : ListSequence.fromList(requiredRequest)) {
        Map<String, String> reqResult = req.waitForResult();
        for (IMapping<String, String> entry : MapSequence.fromMap(reqResult)) {
          if (MapSequence.fromMap(result).containsKey(entry.key())) {
            MapSequence.fromMap(result).put(entry.key(), entry.value());
          }
        }
      }
    }

    return result;
  }

  @Override
  public void listen(final String key, final IKeyListener listener) {
    store.listen(key, listener);
  }

  @Override
  public void put(String key, String value) {
    cache.put(key, value);
    store.put(key, value);
  }

  @Override
  public void putAll(Map<String, String> entries) {
    MapSequence.fromMap(entries).visitAll(new IVisitor<IMapping<String, String>>() {
      public void visit(IMapping<String, String> it) {
        cache.put(it.key(), it.value());
      }
    });
    store.putAll(entries);
  }

  @Override
  public void removeListener(final String key, final IKeyListener listener) {
    store.removeListener(key, listener);
  }

  private class GetRequest {
    private final Set<String> keys;
    private Map<String, String> result;
    private Exception exception;

    public GetRequest(Set<String> keys) {
      this.keys = keys;
    }

    public void execute() {
      try {
        Map<String, String> entriesFromStore = store.getAll(keys);
        for (IMapping<String, String> entry : MapSequence.fromMap(entriesFromStore)) {
          cache.put(entry.key(), entry.value());
        }
        putResult(entriesFromStore);
      } catch (Exception ex) {
        putException(ex);
      }
    }

    public synchronized void putException(Exception ex) {
      this.exception = ex;
      this.notifyAll();
    }

    public synchronized void putResult(Map<String, String> result) {
      this.result = result;
      this.notifyAll();
    }

    public synchronized Map<String, String> waitForResult() {
      while (result == null && exception == null) {
        try {
          this.wait();
        } catch (InterruptedException ex) {
          throw new RuntimeException();
        }
      }
      if (result != null) {
        return result;
      } else {
        throw new RuntimeException(exception);
      }
    }
  }
}