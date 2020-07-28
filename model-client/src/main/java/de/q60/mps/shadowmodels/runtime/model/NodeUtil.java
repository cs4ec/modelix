package de.q60.mps.shadowmodels.runtime.model;

import jetbrains.mps.internal.collections.runtime.Sequence;
import jetbrains.mps.internal.collections.runtime.ITranslator2;

public class NodeUtil {
  public static Iterable<INode> getDescendants(INode node, boolean includeSelf) {
    if (includeSelf) {
      return Sequence.fromIterable(Sequence.<INode>singleton(node)).concat(Sequence.fromIterable(getDescendants(node, false)));
    } else {
      return Sequence.fromIterable(node.getAllChildren()).translate(new ITranslator2<INode, INode>() {
        public Iterable<INode> translate(INode it) {
          return getDescendants(it, true);
        }
      });
    }
  }
}