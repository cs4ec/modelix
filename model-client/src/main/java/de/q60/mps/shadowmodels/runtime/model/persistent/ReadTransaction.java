package de.q60.mps.shadowmodels.runtime.model.persistent;

import java.util.Set;
import jetbrains.mps.internal.collections.runtime.SetSequence;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import de.q60.mps.shadowmodels.runtime.model.INodeReference;
import jetbrains.mps.internal.collections.runtime.Sequence;
import java.util.Collections;

public class ReadTransaction extends Transaction implements IReadTransaction {
  private final ITree tree;
  private Set<IMissingNode> missingNodes = SetSequence.fromSet(new HashSet<IMissingNode>());

  public ReadTransaction(@NotNull ITree tree, PBranch branch) {
    super(branch);
    this.tree = tree;
  }
  @Override
  public ITree getTree() {
    return tree;
  }
  @Override
  public PTree getPTree() {
    return (PTree) tree;
  }

  @Override
  public Iterable<IMissingNode> getMissingNodes() {
    return missingNodes;
  }

  @Override
  public void registerMissingNode(IMissingNode missingNode) {
    SetSequence.fromSet(missingNodes).addElement(missingNode);
  }

  @Override
  public String getProperty(long nodeId, String role) {
    if (isLoaded(nodeId)) {
      return getTree().getProperty(nodeId, role);
    } else {
      registerMissingNode(new MissingNodeId(nodeId));
      return null;
    }
  }

  @Override
  public INodeReference getReferenceTarget(long sourceId, String role) {
    if (isLoaded(sourceId)) {
      return getTree().getReferenceTarget(sourceId, role);
    } else {
      registerMissingNode(new MissingNodeId(sourceId));
      return null;
    }
  }

  @Override
  public Iterable<Long> getChildren(long parentId, String role) {
    if (isLoaded(parentId)) {
      return getTree().getChildren(parentId, role);
    } else {
      registerMissingNode(new MissingNodeId(parentId));
      return Sequence.fromIterable(Collections.<Long>emptyList());
    }
  }

  @Override
  public Iterable<Long> getAllChildren(long parentId) {
    if (isLoaded(parentId)) {
      return getTree().getAllChildren(parentId);
    } else {
      registerMissingNode(new MissingNodeId(parentId));
      return Sequence.fromIterable(Collections.<Long>emptyList());
    }
  }
}
