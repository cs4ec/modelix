package de.q60.mps.shadowmodels.runtime.model.persistent;

import de.q60.mps.incremental.runtime.DependencyKey;

public class PNodeDependency extends DependencyKey {
  private IBranch branch;
  private long nodeId;

  public PNodeDependency(IBranch branch, long nodeId) {
    this.branch = branch;
    this.nodeId = nodeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    PNodeDependency that = (PNodeDependency) o;
    if ((branch != null ? !(branch.equals(that.branch)) : that.branch != null)) {
      return false;
    }
    if (nodeId != that.nodeId) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + ((branch != null ? ((Object) branch).hashCode() : 0));
    result = 31 * result + (int) (nodeId ^ (nodeId >> 32));
    return result;
  }

  @Override
  public String toString() {
    return "PNodeDependency{" + "branch=" + branch + ", nodeId=" + nodeId + "}";
  }
}