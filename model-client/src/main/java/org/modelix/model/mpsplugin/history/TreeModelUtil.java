package org.modelix.model.mpsplugin.history;

import javax.swing.tree.TreeNode;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.Sequence;
import java.util.Objects;
import javax.swing.tree.DefaultTreeModel;
import jetbrains.mps.ide.ThreadUtils;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import javax.swing.tree.MutableTreeNode;
import jetbrains.mps.util.IterableUtil;
import javax.swing.tree.TreeModel;
import jetbrains.mps.ide.ui.tree.MPSTree;
import jetbrains.mps.ide.ui.tree.MPSTreeNode;
import javax.swing.tree.TreePath;

public class TreeModelUtil {
  public static void setChildren(TreeNode parent, Iterable<TreeNode> children_) {
    List<TreeNode> children = Sequence.fromIterable(children_).toListSequence();
    if (Objects.equals(Sequence.fromIterable(getChildren(parent)).toListSequence(), children)) {
      return;
    }
    boolean wasExpanded = isExpanded(parent);
    clearChildren(parent);
    DefaultTreeModel model = as_spdlqu_a0a4a0(getModel(parent), DefaultTreeModel.class);
    if (model != null) {
      ThreadUtils.assertEDT();
      int i = 0;
      for (TreeNode child : ListSequence.fromList(children)) {
        model.insertNodeInto((MutableTreeNode) child, (MutableTreeNode) parent, i);
        i++;
      }
    } else {
      int i = 0;
      for (TreeNode child : ListSequence.fromList(children)) {
        ((MutableTreeNode) parent).insert((MutableTreeNode) child, i);
        i++;
      }
    }
    if (wasExpanded) {
      getTree(parent).expandPath(getPath(parent));
    }
  }
  public static Iterable<TreeNode> getChildren(TreeNode parent) {
    Iterable<? extends TreeNode> result = IterableUtil.asIterable(parent.children().asIterator());
    return Sequence.fromIterable(result).ofType(TreeNode.class);
  }

  public static void clearChildren(TreeNode parent) {
    DefaultTreeModel model = as_spdlqu_a0a0a3(getModel(parent), DefaultTreeModel.class);
    if (model != null) {
      ThreadUtils.assertEDT();
      while (model.getChildCount(parent) > 0) {
        model.removeNodeFromParent((MutableTreeNode) model.getChild(parent, 0));
      }
    } else {
      while (parent.getChildCount() > 0) {
        ((MutableTreeNode) parent).remove(0);
      }
    }
  }

  public static TreeModel getModel(TreeNode node) {
    return check_spdlqu_a0a5(getTree(node));
  }

  public static MPSTree getTree(TreeNode node) {
    return (node instanceof MPSTreeNode ? ((MPSTreeNode) node).getTree() : null);
  }

  public static void repaint(final TreeNode node) {
    ThreadUtils.runInUIThreadAndWait(new Runnable() {
      public void run() {
        check_spdlqu_a0a0a0a9(getTree(node));
      }
    });
  }

  public static void setTextAndRepaint(MPSTreeNode node, String text) {
    node.setText(text);
    repaint(node);
  }

  public static boolean isExpanded(TreeNode node) {
    return check_spdlqu_a0a31(getTree(node), node);
  }

  public static TreePath getPath(TreeNode node) {
    if (node.getParent() == null) {
      return new TreePath(node);
    } else {
      return getPath(node.getParent()).pathByAddingChild(node);
    }
  }
  private static DefaultTreeModel check_spdlqu_a0a5(MPSTree checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getModel();
    }
    return null;
  }
  private static void check_spdlqu_a0a0a0a9(MPSTree checkedDotOperand) {
    if (null != checkedDotOperand) {
      checkedDotOperand.repaint();
    }

  }
  private static boolean check_spdlqu_a0a31(MPSTree checkedDotOperand, TreeNode node) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.isExpanded(getPath(node));
    }
    return false;
  }
  private static <T> T as_spdlqu_a0a4a0(Object o, Class<T> type) {
    return (type.isInstance(o) ? (T) o : null);
  }
  private static <T> T as_spdlqu_a0a0a3(Object o, Class<T> type) {
    return (type.isInstance(o) ? (T) o : null);
  }
}
