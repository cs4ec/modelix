package de.q60.mps.shadowmodels.runtime.model.persistent;

public class PNodeIdMissingException extends RuntimeException {

  private long id;

  public PNodeIdMissingException(long id) {
    super("Node doesn't exist: " + id);
    this.id = id;
  }

  public long getId() {
    return id;
  }
}
