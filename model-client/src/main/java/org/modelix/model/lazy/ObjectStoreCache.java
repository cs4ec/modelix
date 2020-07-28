package org.modelix.model.lazy;

import org.modelix.model.IKeyValueStore;
import org.modelix.model.SynchronizedSLRUMap;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.Sequence;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.HashMap;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import jetbrains.mps.internal.collections.runtime.IMapping;
import jetbrains.mps.internal.collections.runtime.ISelector;

public class ObjectStoreCache implements IDeserializingKeyValueStore {
  private static final Object NULL = new Object();

  private IKeyValueStore store;
  private final SynchronizedSLRUMap<String, Object> cache = new SynchronizedSLRUMap(100000, 100000);

  public ObjectStoreCache(IKeyValueStore store1) {
    store = store1;
  }

  @Override
  public IKeyValueStore getKeyValueStore() {
    return store;
  }

  @Override
  public <T> Iterable<T> getAll(Iterable<String> hashes_, _FunctionTypes._return_P2_E0<? extends T, ? super String, ? super String> deserializer) {
    List<String> hashes = Sequence.fromIterable(hashes_).toListSequence();
    final Map<String, T> result = MapSequence.fromMap(new HashMap<String, T>());
    List<String> nonCachedHashes = ListSequence.fromList(new ArrayList<String>(ListSequence.fromList(hashes).count()));

    for (String hash : hashes) {
      T deserialized = (T) cache.get(hash);
      if (deserialized == null) {
        ListSequence.fromList(nonCachedHashes).addElement(hash);
      } else {
        MapSequence.fromMap(result).put(hash, (deserialized == NULL ? null : deserialized));
      }
    }

    if (ListSequence.fromList(nonCachedHashes).isNotEmpty()) {
      for (IMapping<String, String> entry : MapSequence.fromMap(store.getAll(nonCachedHashes))) {
        String hash = entry.key();
        String serialized = entry.value();
        if (serialized == null) {
          MapSequence.fromMap(result).put(hash, null);
        } else {
          T deserialized = deserializer.invoke(hash, serialized);
          cache.put(hash, (deserialized == null ? NULL : deserialized));
          MapSequence.fromMap(result).put(hash, deserialized);
        }
      }
    }

    return ListSequence.fromList(hashes).select(new ISelector<String, T>() {
      public T select(String it) {
        return MapSequence.fromMap(result).get(it);
      }
    });
  }

  public <T> T get(String hash, _FunctionTypes._return_P1_E0<? extends T, ? super String> deserializer) {
    if (hash == null) {
      return null;
    }
    T deserialized = (T) cache.get(hash);
    if (deserialized == null) {
      String serialized = store.get(hash);
      if (serialized == null) {
        return null;
      }
      deserialized = deserializer.invoke(serialized);
      cache.put(hash, (deserialized == null ? NULL : deserialized));
    }
    return (deserialized == NULL ? null : deserialized);
  }

  public void put(String hash, Object deserialized, String serialized) {
    store.put(hash, serialized);
    cache.put(hash, (deserialized == null ? NULL : deserialized));
  }

  public void clearCache() {
    cache.clear();
  }

  @Override
  public void prefetch(String hash) {
    store.prefetch(hash);
  }
}