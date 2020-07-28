package org.modelix.model.mpsplugin.history;

import jetbrains.mps.ide.ui.tree.TextTreeNode;
import org.modelix.model.mpsplugin.CloudRepository;
import org.jetbrains.mps.openapi.model.SNode;
import org.modelix.model.lazy.TreeId;
import org.modelix.model.mpsplugin.ActiveBranch;
import org.modelix.model.mpsplugin.CloudIcons;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SPropertyOperations;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import javax.swing.tree.TreeNode;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranchListener;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITree;
import javax.swing.SwingUtilities;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.LinkedHashMap;
import jetbrains.mps.ide.ThreadUtils;
import jetbrains.mps.internal.collections.runtime.Sequence;
import org.modelix.model.mpsplugin.SharedExecutors;
import java.util.List;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import jetbrains.mps.internal.collections.runtime.IListSequence;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.internal.collections.runtime.ISelector;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import de.q60.mps.shadowmodels.runtime.model.persistent.PNodeAdapter;
import de.q60.mps.shadowmodels.runtime.model.persistent.PTree;
import java.util.Objects;
import jetbrains.mps.internal.collections.runtime.IVisitor;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;
import org.modelix.model.mpsplugin.ModelBinding;
import org.jetbrains.mps.openapi.language.SProperty;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import org.jetbrains.mps.openapi.language.SContainmentLink;

public class CloudTreeTreeNode extends TextTreeNode {

  private CloudRepository cloudRepository;
  private SNode treeInfo;
  private TreeId treeId;
  private ActiveBranch activeBranch;
  private TextTreeNode dataTreeNode = new TextTreeNode("data");
  private TextTreeNode branchesTreeNode = new TextTreeNode("branches");
  private TextTreeNode bindingsTreeNode = new TextTreeNode("bindings");

  public CloudTreeTreeNode(CloudRepository cloudRepository, SNode treeInfo) {
    super(CloudIcons.TREE_ICON, SPropertyOperations.getString(treeInfo, PROPS.name$tAp1) + " (" + SPropertyOperations.getString(treeInfo, PROPS.id$ECO6) + ")");
    this.cloudRepository = cloudRepository;
    this.treeInfo = treeInfo;
    this.treeId = new TreeId(SPropertyOperations.getString(treeInfo, PROPS.id$ECO6));
    this.activeBranch = cloudRepository.getActiveBranch(new TreeId(SPropertyOperations.getString(treeInfo, PROPS.id$ECO6)));
    setAllowsChildren(true);
    TreeModelUtil.setChildren(this, ListSequence.fromListAndArray(new ArrayList<TreeNode>(), dataTreeNode, branchesTreeNode, bindingsTreeNode));
    activeBranch.addListener(new IBranchListener() {
      @Override
      public void treeChanged(ITree oldTree, ITree newTree) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ((CloudView.CloudViewTree) getTree()).runRebuildAction(new Runnable() {
              public void run() {
                updateData();
              }
            }, true);
          }
        });
      }
    });
    updateData();
  }

  public SNode getTreeInfo() {
    return treeInfo;
  }

  public TreeId getTreeId() {
    return treeId;
  }

  public CloudRepository getCloudRepository() {
    return this.cloudRepository;
  }

  public void updateChildren() {
    updateBranches();
    updateBindings();
  }

  public void updateBranches() {
    final Map<SNode, CloudBranchTreeNode> existing = MapSequence.fromMap(new LinkedHashMap<SNode, CloudBranchTreeNode>(16, (float) 0.75, false));
    ThreadUtils.runInUIThreadAndWait(new Runnable() {
      public void run() {
        if (Sequence.fromIterable(TreeModelUtil.getChildren(CloudTreeTreeNode.this)).isEmpty()) {
          TreeModelUtil.setChildren(CloudTreeTreeNode.this, Sequence.<TreeNode>singleton(LoadingIcon.apply(new TextTreeNode("loading ..."))));
        }
        for (CloudBranchTreeNode node : Sequence.fromIterable(TreeModelUtil.getChildren(branchesTreeNode)).ofType(CloudBranchTreeNode.class)) {
          MapSequence.fromMap(existing).put(node.getBranchInfo(), node);
        }
      }
    });

    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        final List<TreeNode> newChildren = cloudRepository.getInfoBranch().computeRead(new _FunctionTypes._return_P0_E0<IListSequence<TreeNode>>() {
          public IListSequence<TreeNode> invoke() {
            return ListSequence.fromList(SLinkOperations.getChildren(treeInfo, LINKS.branches$ECCX)).select(new ISelector<SNode, TreeNode>() {
              public TreeNode select(SNode it) {
                TreeNode tn = (MapSequence.fromMap(existing).containsKey(it) ? MapSequence.fromMap(existing).get(it) : new CloudBranchTreeNode(cloudRepository, it));
                return tn;
              }
            }).toListSequence();
          }
        });
        ThreadUtils.runInUIThreadNoWait(new Runnable() {
          public void run() {
            TreeModelUtil.setChildren(branchesTreeNode, newChildren);
          }
        });
      }
    });
  }

  public void updateData() {
    TreeModelUtil.setTextAndRepaint(dataTreeNode, "data [" + activeBranch.getBranchName() + "]");
    final IBranch branch = activeBranch.getBranch();
    PNodeAdapter rootNode = new PNodeAdapter(PTree.ROOT_ID, branch);
    Iterable<TreeNode> childTreeNodes = Sequence.fromIterable(TreeModelUtil.getChildren(dataTreeNode)).toListSequence();
    if (Sequence.fromIterable(childTreeNodes).count() != 1 || !(Objects.equals(((CloudNodeTreeNode) Sequence.fromIterable(childTreeNodes).first()).getNode(), rootNode))) {
      CloudNodeTreeNode newTreeNode = new CloudNodeTreeNode(branch, rootNode);
      TreeModelUtil.setChildren(dataTreeNode, ListSequence.fromListAndArray(new ArrayList<TreeNode>(), newTreeNode));
    }
    Sequence.fromIterable(TreeModelUtil.getChildren(dataTreeNode)).ofType(CloudNodeTreeNode.class).visitAll(new IVisitor<CloudNodeTreeNode>() {
      public void visit(CloudNodeTreeNode it) {
        it.update();
      }
    });
  }

  public void updateBindings() {
    TreeModelUtil.setChildren(bindingsTreeNode, Sequence.fromIterable(cloudRepository.getBindings()).where(new IWhereFilter<ModelBinding>() {
      public boolean accept(ModelBinding it) {
        return Objects.equals(it.getTreeId(), treeId);
      }
    }).select(new ISelector<ModelBinding, TextTreeNode>() {
      public TextTreeNode select(ModelBinding it) {
        return new TextTreeNode(it.toString());
      }
    }));
  }

  private static final class PROPS {
    /*package*/ static final SProperty name$tAp1 = MetaAdapterFactory.getProperty(0xceab519525ea4f22L, 0x9b92103b95ca8c0cL, 0x110396eaaa4L, 0x110396ec041L, "name");
    /*package*/ static final SProperty id$ECO6 = MetaAdapterFactory.getProperty(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc0L, 0x62b7d9b07cecbcc6L, "id");
  }

  private static final class LINKS {
    /*package*/ static final SContainmentLink branches$ECCX = MetaAdapterFactory.getContainmentLink(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc0L, 0x62b7d9b07cecbcc4L, "branches");
  }
}