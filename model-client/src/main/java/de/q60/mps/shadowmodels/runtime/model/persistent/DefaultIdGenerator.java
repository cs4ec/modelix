package de.q60.mps.shadowmodels.runtime.model.persistent;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultIdGenerator implements IIdGenerator {
  private static final DefaultIdGenerator INSTANCE = new DefaultIdGenerator();

  public static DefaultIdGenerator getInstance() {
    return INSTANCE;
  }

  protected final AtomicLong ID_SEQUENCE = new AtomicLong(Math.round(Math.abs(Math.random()) * 1000000000.0) * 1000000000L);

  private DefaultIdGenerator() {
  }

  @Override
  public long generate() {
    return ID_SEQUENCE.incrementAndGet();
  }
}
