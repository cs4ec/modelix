package de.q60.mps.shadowmodels.runtime.model.persistent;

import io.vavr.collection.Multimap;
import java.util.Objects;

public class UserObjectIndex<K> extends IncrementalIndex<K> {

  public UserObjectIndex(final Object userObjectKey) {
    super(new IncrementalIndex.IndexUpdater<K>() {
      public Multimap<K, Long> updateIndex(final PTree oldTree, final PTree newTree, final Multimap<K, Long> oldIndex) {
        final Multimap<K, Long>[] newIndex = new Multimap[1];
        newIndex[0] = oldIndex;

        newTree.visitChanges(oldTree, new ITreeChangeVisitor() {
          public void containmentChanged(long nodeId) {
          }
          public void childrenChanged(long nodeId, String role) {
          }
          public void referenceChanged(long nodeId, String role) {
          }
          public void propertyChanged(long nodeId, String role) {
          }
          public void userObjectChanged(long nodeId, Object key) {
            if (Objects.equals(key, userObjectKey)) {
              K oldValue = (K) oldTree.getUserObject(nodeId, userObjectKey);
              K newValue = (K) newTree.getUserObject(nodeId, userObjectKey);

              if (oldValue != newValue) {
                if (oldValue != null) {
                  newIndex[0] = newIndex[0].remove(oldValue, nodeId);
                }
                if (newValue != null) {
                  newIndex[0] = newIndex[0].put(newValue, nodeId);
                }
              }
            }
          }
          public void nodeRemoved(long nodeId) {
            K oldValue = (K) oldTree.getUserObject(nodeId, userObjectKey);
            if (oldValue != null) {
              newIndex[0] = newIndex[0].remove(oldValue, nodeId);
            }
          }
          public void nodeAdded(long nodeId) {
            K newValue = (K) newTree.getUserObject(nodeId, userObjectKey);
            if (newValue != null) {
              newIndex[0] = newIndex[0].put(newValue, nodeId);
            }
          }
          public void nodeLoaded(long nodeId) {
          }

          public void nodeUnloaded(long nodeId) {
          }
        });
        return newIndex[0];
      }
    });
  }
}