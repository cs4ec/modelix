package org.modelix.model.redis;

import org.modelix.model.IKeyValueStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import jetbrains.mps.internal.collections.runtime.IVisitor;
import jetbrains.mps.internal.collections.runtime.IMapping;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import java.util.Objects;
import org.modelix.model.IKeyListener;

public class RedisStore implements IKeyValueStore {
  private RedisClient redisClient;
  private StatefulRedisConnection<String, String> connection;
  private RedisCommands<String, String> syncCommands;

  private StatefulRedisPubSubConnection<String, String> subConnection;
  private RedisPubSubCommands<String, String> subCommands;
  private StatefulRedisPubSubConnection<String, String> pubConnection;
  private RedisPubSubCommands<String, String> pubCommands;

  public RedisStore() {
    Thread thread = Thread.currentThread();
    ClassLoader prevLoader = thread.getContextClassLoader();
    thread.setContextClassLoader(getClass().getClassLoader());
    try {
      redisClient = RedisClient.create("redis://localhost:6379");
      connection = redisClient.connect();
      syncCommands = connection.sync();

      subConnection = redisClient.connectPubSub();
      subCommands = subConnection.sync();

      pubConnection = redisClient.connectPubSub();
      pubCommands = pubConnection.sync();
    } finally {
      thread.setContextClassLoader(prevLoader);
    }
  }

  public void dispose() {
    connection.close();
    subConnection.close();
    pubConnection.close();
    redisClient.shutdown();
  }

  @Override
  public String get(String key) {
    return syncCommands.get(key);
  }

  @Override
  public void put(String key, String value) {
    syncCommands.set(key, value);
  }

  @Override
  public Map<String, String> getAll(Iterable<String> keys) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void putAll(Map<String, String> entries) {
    MapSequence.fromMap(entries).visitAll(new IVisitor<IMapping<String, String>>() {
      public void visit(IMapping<String, String> it) {
        put(it.key(), it.value());
      }
    });
  }

  @Override
  public void prefetch(String key) {
  }

  public void subscribe(final String channel, final _FunctionTypes._void_P1_E0<? super String> listener) {
    subCommands.subscribe(channel);
    subConnection.addListener(new RedisPubSubAdapter<String, String>() {
      @Override
      public void message(String chan, String message) {
        if (Objects.equals(channel, chan)) {
          listener.invoke(message);
        }
      }
    });
  }

  public void publish(String channel, String message) {
    pubCommands.publish(channel, message);
  }

  @Override
  public void listen(final String key, final IKeyListener listener) {
    throw new UnsupportedOperationException();
  }
  @Override
  public void removeListener(final String key, final IKeyListener listener) {
    throw new UnsupportedOperationException();
  }
}
