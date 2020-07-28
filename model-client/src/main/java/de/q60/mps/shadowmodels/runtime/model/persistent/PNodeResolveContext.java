package de.q60.mps.shadowmodels.runtime.model.persistent;

import de.q60.mps.shadowmodels.runtime.model.INodeResolveContext;
import de.q60.mps.shadowmodels.runtime.model.INode;
import de.q60.mps.shadowmodels.runtime.model.INodeReference;

public class PNodeResolveContext implements INodeResolveContext {
  private IBranch branch;

  public PNodeResolveContext(IBranch branch) {
    this.branch = branch;
  }

  public IBranch getBranch() {
    return branch;
  }

  @Override
  public INode resolve(INodeReference ref) {
    if (ref instanceof PNodeReference) {
      return new PNodeAdapter(((PNodeReference) ref).getId(), branch);
    } else {
      return null;
    }
  }
}