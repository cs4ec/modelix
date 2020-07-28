package de.q60.mps.shadowmodels.runtime.model.persistent;

import io.vavr.collection.Multimap;
import io.vavr.collection.HashMultimap;
import de.q60.mps.shadowmodels.runtime.model.INodeReference;
import io.vavr.collection.List;

public class ReverseReferences {
  private final PTree tree;
  private final Multimap<Long, NodeAndRole> index;

  public ReverseReferences() {
    this(PTree.EMPTY, HashMultimap.withSet().<Long,NodeAndRole>empty());
  }

  private ReverseReferences(PTree tree, Multimap<Long, NodeAndRole> index) {
    this.tree = tree;
    this.index = index;
  }

  public synchronized ReverseReferences update(final PTree newTree) {
    if (newTree == tree) {
      return this;
    }

    final Multimap<Long, NodeAndRole>[] newIndex = new Multimap[]{index};

    newTree.visitChanges(tree, new ITreeChangeVisitor() {
      public void containmentChanged(long nodeId) {
      }
      public void childrenChanged(long nodeId, String role) {
      }
      public void referenceChanged(long sourceId, String role) {
        long oldTarget = getId(tree.getReferenceTarget(sourceId, role));
        long newTarget = getId(newTree.getReferenceTarget(sourceId, role));
        if (oldTarget != 0L) {
          newIndex[0] = newIndex[0].remove(oldTarget, new NodeAndRole(sourceId, role));
        }
        if (newTarget != 0L) {
          newIndex[0] = newIndex[0].put(newTarget, new NodeAndRole(sourceId, role));
        }
      }
      public void propertyChanged(long nodeId, String role) {
      }
      public void userObjectChanged(long nodeId, Object key) {
      }
      public void nodeRemoved(long nodeId) {
        for (String role : tree.getReferenceRoles(nodeId)) {
          long oldTarget = getId(tree.getReferenceTarget(nodeId, role));
          if (oldTarget != 0L) {
            newIndex[0] = newIndex[0].remove(oldTarget, new NodeAndRole(nodeId, role));
          }
        }
      }
      public void nodeAdded(long nodeId) {
        for (String role : tree.getReferenceRoles(nodeId)) {
          long newTarget = getId(tree.getReferenceTarget(nodeId, role));
          if (newTarget != 0L) {
            newIndex[0] = newIndex[0].put(newTarget, new NodeAndRole(nodeId, role));
          }
        }
      }
      public void nodeLoaded(long nodeId) {
        for (String role : tree.getReferenceRoles(nodeId)) {
          long newTarget = getId(tree.getReferenceTarget(nodeId, role));
          if (newTarget != 0L) {
            newIndex[0] = newIndex[0].put(newTarget, new NodeAndRole(nodeId, role));
          }
        }
      }
      public void nodeUnloaded(long nodeId) {
        for (String role : tree.getReferenceRoles(nodeId)) {
          long oldTarget = getId(tree.getReferenceTarget(nodeId, role));
          if (oldTarget != 0L) {
            newIndex[0] = newIndex[0].remove(oldTarget, new NodeAndRole(nodeId, role));
          }
        }
      }
      public long getId(INodeReference ref) {
        return (ref instanceof PNodeReference ? ((PNodeReference) ref).getId() : 0);
      }
    });

    return new ReverseReferences(newTree, newIndex[0]);
  }

  public synchronized Iterable<NodeAndRole> get(long target) {
    return index.get(target).getOrElse(List.<NodeAndRole>empty());
  }

  public static class NodeAndRole {
    public final long nodeId;
    public final String role;
    public NodeAndRole(long nodeId, String role) {
      this.nodeId = nodeId;
      this.role = role;
    }
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }

      NodeAndRole that = (NodeAndRole) o;
      if (nodeId != that.nodeId) {
        return false;
      }
      if ((role != null ? !(((Object) role).equals(that.role)) : that.role != null)) {
        return false;
      }

      return true;
    }
    @Override
    public int hashCode() {
      int result = 0;
      result = 31 * result + (int) (nodeId ^ (nodeId >> 32));
      result = 31 * result + ((role != null ? String.valueOf(role).hashCode() : 0));
      return result;
    }
    @Override
    public String toString() {
      return nodeId + "." + role;
    }
  }
}
