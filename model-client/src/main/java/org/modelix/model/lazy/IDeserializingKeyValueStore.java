package org.modelix.model.lazy;

import org.modelix.model.IKeyValueStore;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;

public interface IDeserializingKeyValueStore {
  IKeyValueStore getKeyValueStore();
  <T> T get(String hash, _FunctionTypes._return_P1_E0<? extends T, ? super String> deserializer);
  <T> Iterable<T> getAll(Iterable<String> hash, _FunctionTypes._return_P2_E0<? extends T, ? super String, ? super String> deserializer);
  void put(String hash, Object deserialized, String serialized);
  void prefetch(String hash);
}