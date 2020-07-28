package org.modelix.model.lazy;

import java.util.List;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.internal.collections.runtime.ISelector;

public class NonBulkQuery implements IBulkQuery {

  private IDeserializingKeyValueStore store;

  public NonBulkQuery(IDeserializingKeyValueStore store) {
    this.store = store;
  }

  @Override
  public <I, O> IBulkQuery.Value<List<O>> map(Iterable<I> input, final _FunctionTypes._return_P1_E0<? extends IBulkQuery.Value<O>, ? super I> f) {
    List<O> list = Sequence.fromIterable(input).select(new ISelector<I, IBulkQuery.Value<O>>() {
      public IBulkQuery.Value<O> select(I it) {
        return f.invoke(it);
      }
    }).select(new ISelector<IBulkQuery.Value<O>, O>() {
      public O select(IBulkQuery.Value<O> it) {
        return it.execute();
      }
    }).toListSequence();
    return new Value<List<O>>(list);
  }

  @Override
  public <T> IBulkQuery.Value<T> constant(T value) {
    return new Value<T>(value);
  }

  @Override
  public <T> IBulkQuery.Value<T> get(String hash, _FunctionTypes._return_P1_E0<? extends T, ? super String> deserializer) {
    return constant(store.get(hash, deserializer));
  }

  public class Value<T> implements IBulkQuery.Value<T> {
    private T value;
    public Value(T value) {
      this.value = value;
    }
    @Override
    public T execute() {
      return value;
    }
    @Override
    public <R> IBulkQuery.Value<R> mapBulk(final _FunctionTypes._return_P1_E0<? extends IBulkQuery.Value<R>, ? super T> handler) {
      return handler.invoke(value);
    }
    @Override
    public <R> IBulkQuery.Value<R> map(_FunctionTypes._return_P1_E0<? extends R, ? super T> handler) {
      return new Value<R>(handler.invoke(value));
    }
    @Override
    public void onSuccess(_FunctionTypes._void_P1_E0<? super T> handler) {
      handler.invoke(value);
    }
  }
}
