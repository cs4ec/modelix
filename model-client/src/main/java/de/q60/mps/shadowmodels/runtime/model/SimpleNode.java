package de.q60.mps.shadowmodels.runtime.model;

import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.HashMap;
import org.jetbrains.annotations.Nullable;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;
import java.util.Objects;

public class SimpleNode implements INode, INodeReference {

  private IConcept concept;
  private __IContainment containment = __KnownContainment.NULL;
  private List<INode> children = ListSequence.fromList(new ArrayList<INode>());
  private Map<String, INode> references = MapSequence.fromMap(new HashMap<String, INode>());
  private Map<String, String> properties = MapSequence.fromMap(new HashMap<String, String>());

  public SimpleNode(IConcept concept) {
    this.concept = concept;
  }

  @Override
  public boolean isValid() {
    return true;
  }
  @Override
  public INodeReference getReference() {
    return this;
  }
  @Nullable
  @Override
  public INode resolveNode(INodeResolveContext context) {
    return this;
  }
  @Override
  public IConcept getConcept() {
    return concept;
  }
  @Override
  public String getRoleInParent() {
    return containment.getRoleInParent();
  }
  @Override
  public INode getParent() {
    return containment.getParent();
  }
  @Override
  public Iterable<INode> getChildren(final String role) {
    return ListSequence.fromList(children).where(new IWhereFilter<INode>() {
      public boolean accept(INode it) {
        return Objects.equals(it.getRoleInParent(), role);
      }
    });
  }
  @Override
  public Iterable<INode> getAllChildren() {
    return children;
  }
  @Override
  public void addChild(final String role, int indexInRole, INode child) {
    if (!(child instanceof SimpleNode)) {
      throw new RuntimeException("Not a " + SimpleNode.class.getSimpleName() + ": " + child.getClass().getName());
    }
    if (child.getParent() != null) {
      throw new RuntimeException("Already has a parent: " + child);
    }

    if (indexInRole == -1) {
      ListSequence.fromList(children).addElement(child);
    } else {
      INode childAfter = ListSequence.fromList(children).where(new IWhereFilter<INode>() {
        public boolean accept(INode it) {
          return Objects.equals(it.getRoleInParent(), role);
        }
      }).skip(indexInRole).first();
      if (childAfter == null) {
        ListSequence.fromList(children).addElement(child);
      } else {
        int indexInAll = ListSequence.fromList(children).indexOf(childAfter);
        ListSequence.fromList(children).insertElement(indexInAll, child);
      }
    }
    ((SimpleNode) child).containment = __KnownContainment.create(this, role);
  }
  @Override
  public INode addNewChild(String role, int index, @Nullable IConcept concept) {
    SimpleNode newChild = new SimpleNode(concept);
    addChild(role, index, newChild);
    return newChild;
  }
  @Override
  public void removeChild(INode child) {
    int index = ListSequence.fromList(children).indexOf(child);
    if (index == -1) {
      throw new RuntimeException(child + " is not a child of " + this);
    }
    ((SimpleNode) child).containment = __KnownContainment.NULL;
    ListSequence.fromList(children).removeElementAt(index);
  }
  @Override
  public INode getReferenceTarget(String role) {
    return MapSequence.fromMap(references).get(role);
  }
  @Override
  public void setReferenceTarget(String role, INode target) {
    MapSequence.fromMap(references).put(role, target);
  }
  @Override
  public String getPropertyValue(String role) {
    return MapSequence.fromMap(properties).get(role);
  }
  @Override
  public void setPropertyValue(String role, String value) {
    MapSequence.fromMap(properties).put(role, value);
  }

  @Override
  public String toString() {
    return "SimpleNode[" + concept + "]";
  }
}
