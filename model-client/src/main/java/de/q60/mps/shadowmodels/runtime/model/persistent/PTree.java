package de.q60.mps.shadowmodels.runtime.model.persistent;

import de.q60.mps.shadowmodels.runtime.util.pmap.LongKeyPMap;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import de.q60.mps.shadowmodels.runtime.model.IConcept;
import java.util.Objects;
import de.q60.mps.shadowmodels.runtime.model.INodeReference;
import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.baseLanguage.tuples.runtime.Tuples;
import jetbrains.mps.baseLanguage.tuples.runtime.MultiTuple;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ArrayUtils;
import jetbrains.mps.internal.collections.runtime.ISelector;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;
import jetbrains.mps.internal.collections.runtime.ITranslator2;
import de.q60.mps.shadowmodels.runtime.util.pmap.CustomPMap;
import de.q60.mps.shadowmodels.runtime.util.pmap.SmallPMap;
import java.util.Collections;
import de.q60.mps.shadowmodels.runtime.util.pmap.COWArrays;

public class PTree implements ITree {
  public static final long ROOT_ID = 1;
  public static final PTree EMPTY = new PTree(new LongKeyPMap<PNode>().put(ROOT_ID, new LoadedNode(ROOT_ID, null)));
  private static final long[] EMPTY_LONG_ARRAY = new long[0];

  private final LongKeyPMap<PNode> nodes;

  protected PTree(LongKeyPMap<PNode> nodes) {
    this.nodes = nodes;
  }

  public boolean containsNode(long nodeId) {
    return nodes.get(nodeId) != null;
  }

  public boolean isLoaded(long nodeId) {
    return nodes.get(nodeId) instanceof LoadedNode;
  }

  public PTree loadNode(long nodeId) {
    PNode existing = nodes.get(nodeId);
    if (!(existing instanceof LazyNode)) {
      throw new RuntimeException("Node is already loaded: " + nodeId);
    }
    return new PTree(nodes.put(nodeId, ((LazyNode) existing).toLoadedNode()));
  }

  @Override
  public void visitChanges(ITree oldVersion, final ITreeChangeVisitor visitor) {
    nodes.visitChanges(((PTree) oldVersion).nodes, new LongKeyPMap.IChangeVisitor<PNode>() {
      public void entryAdded(long key, PNode value) {
        visitor.nodeAdded(key);
      }
      public void entryRemoved(long key, PNode value) {
        visitor.nodeRemoved(key);
      }
      public void entryChanged(long key, PNode oldValue, PNode newValue) {
        newValue.visitChanges(key, oldValue, visitor);
      }
    });
  }

  public void visitNodes(final _FunctionTypes._return_P2_E0<? extends Boolean, ? super Long, ? super PNode> visitor) {
    nodes.visitEntries(visitor);
  }

  protected PNode getNode(long nodeId) {
    PNode node = nodes.get(nodeId);
    if (node == null) {
      throw new PNodeIdMissingException(nodeId);
    }
    return node;
  }

  public IConcept getConcept(long nodeId) {
    return getNode(nodeId).getConcept();
  }

  public long getParent(long nodeId) {
    return getNode(nodeId).getParent();
  }

  public String getRole(long nodeId) {
    return getNode(nodeId).getRole();
  }

  public String getProperty(long nodeId, String role) {
    return getNode(nodeId).getProperty(role);
  }

  public PTree setProperty(long nodeId, String role, String value) {
    PNode node = getNode(nodeId);
    if (Objects.equals(node.getProperty(role), value)) {
      return this;
    }
    return new PTree(nodes.put(nodeId, node.setProperty(role, value)));
  }

  public Object getUserObject(long nodeId, Object key) {
    return getNode(nodeId).getUserObject(key);
  }

  public PTree setUserObject(long nodeId, Object key, Object value) {
    PNode node = getNode(nodeId);
    if (Objects.equals(node.getUserObject(key), value)) {
      return this;
    }
    return new PTree(nodes.put(nodeId, node.setUserObject(key, value)));
  }

  public INodeReference getReferenceTarget(long sourceId, String role) {
    return getNode(sourceId).getReferenceTarget(role);
  }

  public PTree setReferenceTarget(long sourceId, String role, INodeReference target) {
    PNode node = getNode(sourceId);
    if (Objects.equals(node.getReferenceTarget(role), target)) {
      return this;
    }
    return new PTree(nodes.put(sourceId, node.setReferenceTarget(role, target)));
  }

  public Iterable<String> getReferenceRoles(long sourceId) {
    return getNode(sourceId).getReferenceRoles();
  }

  public Iterable<String> getPropertyRoles(long sourceId) {
    return getNode(sourceId).getPropertyRoles();
  }

  public Iterable<String> getChildRoles(long sourceId) {
    return getNode(sourceId).getChildRoles();
  }

  public Iterable<Long> getChildren(long parentId, String role) {
    return getNode(parentId).getChildren(role);
  }

  public Iterable<Long> getAllChildren(long parentId) {
    return getNode(parentId).getAllChildren();
  }

  public PTree moveChild(long newParentId, String newRole, int newIndex, long childId) {
    final PNode oldChild = getNode(childId);
    PNode child = oldChild;
    long oldParentId = child.getParent();
    final PNode oldParent = getNode(child.getParent());
    PNode previousParent = oldParent;
    PNode newParent = getNode(newParentId);

    LongKeyPMap<PNode> newMap = nodes;

    previousParent = previousParent.removeChild(child.getRole(), childId);
    child = child.setParent(newParentId, newRole);
    if (previousParent.id == newParentId) {
      newParent = previousParent;
    } else {
      newMap = newMap.put(oldParentId, previousParent);
    }
    newParent = newParent.insertChild(newRole, newIndex, childId);

    if (Objects.equals(oldChild.getRole(), child.getRole()) && Objects.equals(oldParent.id, newParent.id) && Objects.equals(Sequence.fromIterable(oldParent.getChildren(oldChild.getRole())).indexOf(oldChild.id), Sequence.fromIterable(newParent.getChildren(oldChild.getRole())).indexOf(oldChild.id))) {
      return this;
    }

    newMap = newMap.put(newParentId, newParent);
    newMap = newMap.put(childId, child);

    return new PTree(newMap);
  }

  @Override
  public PTree addNewChild(long parentId, String role, int index, long childId, IConcept concept) {
    Tuples._2<Long, PTree> result = addNewChild(parentId, role, index, childId, concept, false);
    return result._1();
  }

  public Tuples._2<Long, PTree> addNewChild(long parentId, String role, int index, long newId, IConcept concept, boolean lazy) {
    PNode parent = getNode(parentId);
    LongKeyPMap<PNode> newMap = nodes;

    PNode newChild = (lazy ? new LazyNode(newId, concept) : new LoadedNode(newId, concept));
    newChild = newChild.setParent(parentId, role);
    parent = parent.insertChild(role, index, newId);

    newMap = newMap.put(parentId, parent);
    newMap = newMap.put(newId, newChild);

    return MultiTuple.<Long,PTree>from(newId, new PTree(newMap));
  }

  @Override
  public PTree addNewChildren(long parentId, String role, int index, long[] newIds, IConcept[] concepts) {
    if (newIds.length != concepts.length) {
      throw new IllegalArgumentException("Array length mismatch: " + newIds.length + " != " + concepts.length);
    }

    PNode parent = getNode(parentId);
    LongKeyPMap<PNode> newMap = nodes;

    PNode[] newChildren = new PNode[newIds.length];
    for (int i = 0; i < newIds.length; i++) {
      newChildren[i] = new LoadedNode(newIds[i], concepts[i]);
      newChildren[i] = newChildren[i].setParent(parentId, role);
      newMap = newMap.put(newIds[i], newChildren[i]);
    }
    parent = parent.insertChildren(role, index, newIds);
    newMap = newMap.put(parentId, parent);

    return new PTree(newMap);
  }

  public PTree deleteNode(long nodeId) {
    PNode node = getNode(nodeId);
    if (node.getParent() == 0) {
      throw new RuntimeException("Cannot delete the root node");
    }
    PNode parent = getNode(node.getParent());

    LongKeyPMap<PNode> newMap = nodes;
    parent = parent.removeChild(node.getRole(), nodeId);
    newMap = newMap.put(node.getParent(), parent);
    for (long id : getDescendants(nodeId, true)) {
      newMap = newMap.remove(id);
    }

    return new PTree(newMap);
  }

  @Override
  public PTree deleteNodes(long[] nodeIds) {
    if (nodeIds.length == 0) {
      return this;
    }
    List<PNode> childNodes = Sequence.fromIterable(ArrayUtils.fromLongArray(nodeIds)).select(new ISelector<Long, PNode>() {
      public PNode select(Long it) {
        return getNode(it);
      }
    }).toListSequence();
    final long parentId = ListSequence.fromList(childNodes).first().getParent();
    final String role = ListSequence.fromList(childNodes).first().getRole();
    if (parentId == 0) {
      throw new RuntimeException("Cannot delete the root node");
    }
    if (ListSequence.fromList(childNodes).any(new IWhereFilter<PNode>() {
      public boolean accept(PNode it) {
        return it.getParent() != parentId;
      }
    })) {
      throw new RuntimeException("All nodes are expected to have the same parent");
    }
    if (ListSequence.fromList(childNodes).any(new IWhereFilter<PNode>() {
      public boolean accept(PNode it) {
        return !(Objects.equals(it.getRole(), role));
      }
    })) {
      throw new RuntimeException("All nodes are expected to have the same role");
    }
    PNode parent = getNode(parentId);

    LongKeyPMap<PNode> newMap = nodes;
    parent = parent.removeChildren(role, nodeIds);
    newMap = newMap.put(parentId, parent);
    for (long nodeId : Sequence.fromIterable(ArrayUtils.fromLongArray(nodeIds)).translate(new ITranslator2<Long, Long>() {
      public Iterable<Long> translate(Long it) {
        return getDescendants(it, true);
      }
    })) {
      newMap = newMap.remove(nodeId);
    }

    return new PTree(newMap);
  }

  public Iterable<Long> getDescendants(long nodeId, boolean includeSelf) {
    if (includeSelf) {
      return Sequence.fromIterable(Sequence.<Long>singleton(nodeId)).concat(Sequence.fromIterable(getDescendants(nodeId, false)));
    } else {
      return Sequence.fromIterable(getAllChildren(nodeId)).translate(new ITranslator2<Long, Long>() {
        public Iterable<Long> translate(Long it) {
          return getDescendants(it, true);
        }
      });
    }
  }

  public static abstract class PNode {
    protected final IConcept concept;
    protected final long id;
    protected final long parentId;
    protected final String roleInParent;
    protected final CustomPMap<Object, Object> userObjects;

    protected PNode(long id, IConcept concept, long parentId, String role, CustomPMap<Object, Object> userObjects) {
      this.id = id;
      this.concept = concept;
      this.parentId = parentId;
      this.roleInParent = role;
      this.userObjects = userObjects;
    }

    public void visitChanges(long ownId, PNode oldVersion, ITreeChangeVisitor visitor) {
      if (!(Objects.equals(oldVersion.parentId, this.parentId)) || !(Objects.equals(oldVersion.roleInParent, this.roleInParent))) {
        visitor.containmentChanged(ownId);
      }
      if (oldVersion.userObjects != this.userObjects) {
        for (Object key : Sequence.fromIterable(keys(oldVersion.userObjects)).union(Sequence.fromIterable(keys(this.userObjects)))) {
          if (!(Objects.equals(oldVersion.userObjects.get(key), this.userObjects.get(key)))) {
            visitor.userObjectChanged(ownId, key);
          }
        }
      }
    }

    protected abstract PNode setProperty(String role, String value);
    protected abstract String getProperty(String role);
    protected abstract PNode setUserObject(Object key, Object value);
    protected abstract PNode setReferenceTarget(String role, INodeReference targetId);
    protected abstract INodeReference getReferenceTarget(String role);
    protected abstract Iterable<String> getReferenceRoles();
    protected abstract Iterable<String> getPropertyRoles();
    protected abstract Iterable<String> getChildRoles();
    protected abstract Iterable<Long> getChildren(String role);
    protected abstract Iterable<Long> getAllChildren();
    protected abstract PNode setParent(long parent, String role);
    protected abstract PNode insertChild(String role, int index, long childId);
    protected abstract PNode insertChildren(String role, int index, long[] childIds);
    protected abstract PNode removeChild(String role, long childId);
    protected abstract PNode removeChildren(String role, long[] childId);

    protected Object getUserObject(Object key) {
      return userObjects.get(key);
    }

    protected String getRole() {
      return roleInParent;
    }

    protected long getParent() {
      return parentId;
    }

    protected IConcept getConcept() {
      return concept;
    }

  }

  public static class LazyNode extends PNode {
    public LazyNode(long id, IConcept concept) {
      this(id, concept, 0, null, SmallPMap.<Object,Object>empty());
    }

    public LazyNode(long id, IConcept concept, long parentId, String role, CustomPMap<Object, Object> userObjects) {
      super(id, concept, parentId, role, userObjects);
    }

    public LoadedNode toLoadedNode() {
      return new LoadedNode(id, concept, parentId, roleInParent, SmallPMap.<String,String>empty(), SmallPMap.<String,INodeReference>empty(), SmallPMap.<String,long[]>empty(), userObjects);
    }

    @Override
    protected LazyNode setUserObject(Object key, Object value) {
      return new LazyNode(id, concept, parentId, roleInParent, (value == null ? userObjects.remove(key) : userObjects.put(key, value)));
    }
    @Override
    public void visitChanges(long ownId, PNode oldVersion_, ITreeChangeVisitor visitor) {
      if (!((oldVersion_ instanceof LazyNode))) {
        visitor.nodeUnloaded(ownId);
      }
      super.visitChanges(ownId, oldVersion_, visitor);
    }
    @Override
    protected Iterable<Long> getAllChildren() {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected Iterable<Long> getChildren(String role) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected String getProperty(String role) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected INodeReference getReferenceTarget(String role) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected LazyNode insertChild(String role, int index, long childId) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected LazyNode insertChildren(String role, int index, long[] childIds) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected LazyNode removeChild(String role, long childId) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected PNode removeChildren(String role, long[] childId) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected LazyNode setParent(long parent, String role) {
      return new LazyNode(id, concept, parent, role, userObjects);
    }
    @Override
    protected LazyNode setProperty(String role, String value) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected LazyNode setReferenceTarget(String role, INodeReference target) {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected Iterable<String> getChildRoles() {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected Iterable<String> getPropertyRoles() {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
    @Override
    protected Iterable<String> getReferenceRoles() {
      throw new NodeNotLoadedException(new MissingNodeId(id));
    }
  }

  public static class LoadedNode extends PNode {

    private final IConcept concept;
    private final long parentId;
    private final String roleInParent;
    private final CustomPMap<String, String> properties;
    private final CustomPMap<String, INodeReference> references;
    private final CustomPMap<String, long[]> children;

    protected LoadedNode(long id, IConcept concept) {
      this(id, concept, 0, null, SmallPMap.<String,String>empty(), SmallPMap.<String,INodeReference>empty(), SmallPMap.<String,long[]>empty(), SmallPMap.<Object,Object>empty());
    }

    protected LoadedNode(long id, IConcept concept, long parentId, String role, CustomPMap<String, String> properties, CustomPMap<String, INodeReference> references, CustomPMap<String, long[]> children, CustomPMap<Object, Object> userObjects) {
      super(id, concept, parentId, role, userObjects);
      this.concept = concept;
      this.parentId = parentId;
      this.roleInParent = role;
      this.children = children;
      this.properties = properties;
      this.references = references;
    }

    @Override
    public void visitChanges(long ownId, PNode oldVersion_, ITreeChangeVisitor visitor) {
      super.visitChanges(ownId, oldVersion_, visitor);

      if (oldVersion_ instanceof LazyNode) {
        visitor.nodeLoaded(ownId);
      } else {
        LoadedNode oldVersion = ((LoadedNode) oldVersion_);
        if (!(Objects.equals(oldVersion.parentId, this.parentId)) || !(Objects.equals(oldVersion.roleInParent, this.roleInParent))) {
          visitor.containmentChanged(ownId);
        }
        if (oldVersion.properties != this.properties) {
          for (String key : Sequence.fromIterable(keys(oldVersion.properties)).union(Sequence.fromIterable(keys(this.properties)))) {
            if (!(Objects.equals(oldVersion.properties.get(key), this.properties.get(key)))) {
              visitor.propertyChanged(ownId, key);
            }
          }
        }
        if (oldVersion.references != this.references) {
          for (String key : Sequence.fromIterable(keys(oldVersion.references)).union(Sequence.fromIterable(keys(this.references)))) {
            if (!(Objects.equals(oldVersion.references.get(key), this.references.get(key)))) {
              visitor.referenceChanged(ownId, key);
            }
          }
        }
        if (oldVersion.children != this.children) {
          for (String key : Sequence.fromIterable(keys(oldVersion.children)).union(Sequence.fromIterable(keys(this.children)))) {
            if (!(Objects.equals(oldVersion.children.get(key), this.children.get(key)))) {
              visitor.childrenChanged(ownId, key);
            }
          }
        }
      }
    }

    @Override
    protected LoadedNode setProperty(String role, String value) {
      return new LoadedNode(id, concept, parentId, this.roleInParent, (value == null ? properties.remove(role) : properties.put(role, value)), references, children, userObjects);
    }

    @Override
    protected String getProperty(String role) {
      return properties.get(role);
    }

    @Override
    protected LoadedNode setUserObject(Object key, Object value) {
      return new LoadedNode(id, concept, parentId, roleInParent, properties, references, children, (value == null ? userObjects.remove(key) : userObjects.put(key, value)));
    }

    @Override
    protected LoadedNode setReferenceTarget(String role, INodeReference targetId) {
      return new LoadedNode(id, concept, parentId, this.roleInParent, properties, (targetId == null ? references.remove(role) : references.put(role, targetId)), children, userObjects);
    }

    @Override
    protected INodeReference getReferenceTarget(String role) {
      return references.get(role);
    }

    @Override
    protected Iterable<Long> getChildren(String role) {
      long[] value = children.get(role);
      return (value == null ? Collections.<Long>emptyList() : ArrayUtils.fromLongArray(value));
    }

    @Override
    protected Iterable<Long> getAllChildren() {
      Iterable<long[]> values = children.values();
      return Sequence.fromIterable(values).translate(new ITranslator2<long[], Long>() {
        public Iterable<Long> translate(long[] it) {
          return ArrayUtils.fromLongArray(it);
        }
      });
    }

    @Override
    protected LoadedNode setParent(long parent, String role) {
      return new LoadedNode(id, concept, parent, role, properties, references, children, userObjects);
    }

    @Override
    protected LoadedNode insertChild(String role, int index, long childId) {
      long[] childrenInRole = children.get(role);
      if (childrenInRole == null) {
        childrenInRole = EMPTY_LONG_ARRAY;
      }
      childrenInRole = (index == -1 ? COWArrays.add(childrenInRole, childId) : COWArrays.insert(childrenInRole, index, childId));
      return new LoadedNode(id, concept, parentId, roleInParent, properties, references, children.put(role, childrenInRole), userObjects);
    }

    @Override
    protected LoadedNode insertChildren(String role, int index, long[] childIds) {
      long[] childrenInRole = children.get(role);
      if (childrenInRole == null) {
        childrenInRole = EMPTY_LONG_ARRAY;
      }
      childrenInRole = (index == -1 ? COWArrays.add(childrenInRole, childIds) : COWArrays.insert(childrenInRole, index, childIds));
      return new LoadedNode(id, concept, parentId, roleInParent, properties, references, children.put(role, childrenInRole), userObjects);
    }

    @Override
    protected LoadedNode removeChild(String role, long childId) {
      long[] childrenInRole = children.get(role);
      childrenInRole = COWArrays.remove(childrenInRole, childId);
      return new LoadedNode(id, concept, parentId, roleInParent, properties, references, (childrenInRole.length == 0 ? children.remove(role) : children.put(role, childrenInRole)), userObjects);
    }

    @Override
    protected PNode removeChildren(String role, long[] childIds) {
      long[] childrenInRole = children.get(role);
      childrenInRole = COWArrays.removeAll(childrenInRole, childIds);
      return new LoadedNode(id, concept, parentId, roleInParent, properties, references, (childrenInRole.length == 0 ? children.remove(role) : children.put(role, childrenInRole)), userObjects);
    }

    @Override
    protected Iterable<String> getChildRoles() {
      return children.keys();
    }

    @Override
    protected Iterable<String> getPropertyRoles() {
      return properties.keys();
    }

    @Override
    protected Iterable<String> getReferenceRoles() {
      return references.keys();
    }
  }

  protected static <T> Iterable<T> keys(CustomPMap<T, ?> map) {
    return map.keys();
  }

  protected static long unbox(Long value) {
    return (value == null ? 0 : value.longValue());
  }
}