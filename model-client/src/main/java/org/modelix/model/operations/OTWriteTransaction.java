package org.modelix.model.operations;

import de.q60.mps.shadowmodels.runtime.model.persistent.IWriteTransaction;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import de.q60.mps.shadowmodels.runtime.model.persistent.IIdGenerator;
import jetbrains.mps.internal.collections.runtime.Sequence;
import de.q60.mps.shadowmodels.runtime.model.INodeReference;
import de.q60.mps.shadowmodels.runtime.model.IConcept;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITree;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import de.q60.mps.shadowmodels.runtime.model.INode;
import de.q60.mps.shadowmodels.runtime.model.persistent.PNodeAdapter;

public class OTWriteTransaction implements IWriteTransaction {
  private static final Logger LOG = LogManager.getLogger(OTWriteTransaction.class);

  private IWriteTransaction transaction;
  private OTBranch otBranch;
  protected IIdGenerator idGenerator;

  public OTWriteTransaction(IWriteTransaction transaction, OTBranch otBranch, IIdGenerator idGenerator) {
    this.otBranch = otBranch;
    this.transaction = transaction;
    this.idGenerator = idGenerator;
  }

  protected void apply(IOperation op) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("apply: " + op);
    }
    IAppliedOperation appliedOp = op.apply(transaction);
    otBranch.operationApplied(appliedOp);
  }

  @Override
  public void moveChild(long newParentId, String newRole, int newIndex, long childId) {
    long oldparent = getParent(childId);
    String oldRole = getRole(childId);
    int oldIndex = Sequence.fromIterable(getChildren(oldparent, oldRole)).indexOf(childId);
    if (newIndex == -1) {
      newIndex = Sequence.fromIterable(getChildren(newParentId, newRole)).count();
    }
    apply(new MoveNodeOp(childId, oldparent, oldRole, oldIndex, newParentId, newRole, newIndex));
  }

  @Override
  public void setProperty(long nodeId, String role, String value) {
    apply(new SetPropertyOp(nodeId, role, value));
  }

  @Override
  public void setReferenceTarget(long sourceId, String role, INodeReference target) {
    apply(new SetReferenceOp(sourceId, role, target));
  }

  @Override
  public void addNewChild(long parentId, String role, int index, long childId, IConcept concept) {
    if (index == -1) {
      index = Sequence.fromIterable(getChildren(parentId, role)).count();
    }
    apply(new AddNewChildOp(parentId, role, index, childId, concept));
  }

  @Override
  public void deleteNode(long nodeId) {
    long parent = getParent(nodeId);
    String role = getRole(nodeId);
    int index = Sequence.fromIterable(getChildren(parent, role)).indexOf(nodeId);
    apply(new DeleteNodeOp(parent, role, index, nodeId));
  }

  @Override
  public long addNewChild(long parentId, String role, int index, IConcept concept) {
    long childId = idGenerator.generate();
    addNewChild(parentId, role, index, childId, concept);
    return childId;
  }
  @Override
  public long addNewLazyChild(long parentId, String role, int index, IConcept concept) {
    throw new UnsupportedOperationException();
  }
  @Override
  public boolean containsNode(long nodeId) {
    return transaction.containsNode(nodeId);
  }
  @Override
  public void ensureLoaded(long nodeId) {
    throw new UnsupportedOperationException();
  }
  @Override
  public Iterable<Long> getAllChildren(long parentId) {
    return transaction.getAllChildren(parentId);
  }
  @Override
  public IBranch getBranch() {
    return otBranch;
  }
  @Override
  public Iterable<Long> getChildren(long parentId, String role) {
    return transaction.getChildren(parentId, role);
  }
  @Override
  public IConcept getConcept(long nodeId) {
    return transaction.getConcept(nodeId);
  }
  @Override
  public long getParent(long nodeId) {
    return transaction.getParent(nodeId);
  }
  @Override
  public String getProperty(long nodeId, String role) {
    return transaction.getProperty(nodeId, role);
  }
  @Override
  public INodeReference getReferenceTarget(long sourceId, String role) {
    return transaction.getReferenceTarget(sourceId, role);
  }
  @Override
  public String getRole(long nodeId) {
    return transaction.getRole(nodeId);
  }
  @Override
  public ITree getTree() {
    return transaction.getTree();
  }
  @Override
  public Object getUserObject(long nodeId, Object key) {
    return transaction.getUserObject(nodeId, key);
  }
  @Override
  public boolean isLoaded(long nodeId) {
    throw new UnsupportedOperationException();
  }
  @Override
  public void loadNode(long nodeId) {
    throw new UnsupportedOperationException();
  }
  @Override
  public void setTree(ITree tree) {
    throw new UnsupportedOperationException();
  }
  @Override
  public void setUserObject(long nodeId, Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitNodes(final _FunctionTypes._return_P1_E0<? extends Boolean, ? super INode> visitor) {
    transaction.visitNodes(new _FunctionTypes._return_P1_E0<Boolean, INode>() {
      public Boolean invoke(INode node) {
        return visitor.invoke(wrap(node));
      }
    });
  }

  protected INode wrap(INode node) {
    return (node instanceof PNodeAdapter ? new PNodeAdapter(((PNodeAdapter) node).getNodeId(), otBranch) : node);
  }
}