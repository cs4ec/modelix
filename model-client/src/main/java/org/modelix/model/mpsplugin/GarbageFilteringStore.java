package org.modelix.model.mpsplugin;

import org.modelix.model.IKeyValueStore;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import jetbrains.mps.internal.collections.runtime.IMapping;
import org.modelix.model.persistent.HashUtil;
import org.modelix.model.IKeyListener;

public class GarbageFilteringStore implements IKeyValueStore {

  private IKeyValueStore store;
  private Map<String, String> pendingEntries = MapSequence.fromMap(new HashMap<String, String>());

  public GarbageFilteringStore(IKeyValueStore store) {
    this.store = store;
  }

  @Override
  public String get(String key) {
    return (MapSequence.fromMap(pendingEntries).containsKey(key) ? MapSequence.fromMap(pendingEntries).get(key) : store.get(key));
  }

  @Override
  public void put(String key, String value) {
    putAll(Collections.singletonMap(key, value));
  }

  @Override
  public Map<String, String> getAll(Iterable<String> keys_) {
    List<String> keys = ListSequence.fromListWithValues(new ArrayList<String>(), keys_);
    Map<String, String> result = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));
    synchronized (pendingEntries) {
      Iterator<String> itr = ListSequence.fromList(keys).iterator();
      while (itr.hasNext()) {
        String key = itr.next();
        // always put even if null to have the same order in the linked hash map as in the input 
        MapSequence.fromMap(result).put(key, MapSequence.fromMap(pendingEntries).get(key));
        if (MapSequence.fromMap(pendingEntries).containsKey(key)) {
          itr.remove();
        }
      }
    }
    if (ListSequence.fromList(keys).isNotEmpty()) {
      MapSequence.fromMap(result).putAll(store.getAll(keys));
    }
    return result;
  }

  @Override
  public void putAll(Map<String, String> entries) {
    Map<String, String> entriesToWrite = MapSequence.fromMap(new LinkedHashMap<String, String>(16, (float) 0.75, false));
    for (IMapping<String, String> entry : MapSequence.fromMap(entries)) {
      if (HashUtil.isSha256(entry.key())) {
        MapSequence.fromMap(pendingEntries).put(entry.key(), entry.value());
      } else {
        collectDependencies(entry.key(), entry.value(), entriesToWrite);
      }
    }
    if (MapSequence.fromMap(entriesToWrite).isNotEmpty()) {
      if (MapSequence.fromMap(entriesToWrite).count() == 1) {
        IMapping<String, String> entry = MapSequence.fromMap(entriesToWrite).first();
        store.put(entry.key(), entry.value());
      } else {
        store.putAll(entriesToWrite);
      }
    }
  }

  protected void collectDependencies(String key, String value, Map<String, String> acc) {
    for (String depKey : HashUtil.extractSha256(value)) {
      if (MapSequence.fromMap(pendingEntries).containsKey(depKey)) {
        String depValue = MapSequence.fromMap(pendingEntries).removeKey(depKey);
        collectDependencies(depKey, depValue, acc);
      }
    }
    MapSequence.fromMap(acc).put(key, value);
  }

  @Override
  public void prefetch(String key) {
    store.prefetch(key);
  }

  @Override
  public void listen(final String key, final IKeyListener listener) {
    store.listen(key, listener);
  }

  @Override
  public void removeListener(final String key, final IKeyListener listener) {
    store.removeListener(key, listener);
  }
}