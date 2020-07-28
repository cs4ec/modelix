package org.modelix.model.mpsplugin.plugin;

import jetbrains.mps.workbench.action.BaseAction;
import javax.swing.Icon;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.Map;
import org.modelix.model.mpsplugin.history.CloudNodeTreeNode;
import jetbrains.mps.ide.actions.MPSCommonDataKeys;
import de.q60.mps.shadowmodels.runtime.model.INode;
import de.q60.mps.shadowmodels.runtime.model.persistent.PNodeAdapter;
import de.q60.mps.shadowmodels.runtime.model.persistent.PTree;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import javax.swing.tree.TreeNode;
import com.intellij.openapi.ui.Messages;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import de.q60.mps.shadowmodels.runtime.smodel.SConceptAdapter;
import org.jetbrains.mps.openapi.language.SConcept;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import org.jetbrains.mps.openapi.language.SProperty;

public class AddModuleNode_Action extends BaseAction {
  private static final Icon ICON = null;

  public AddModuleNode_Action() {
    super("Add Module", "", ICON);
    this.setIsAlwaysVisible(false);
    this.setExecuteOutsideCommand(true);
  }
  @Override
  public boolean isDumbAware() {
    return true;
  }
  @Override
  public boolean isApplicable(AnActionEvent event, final Map<String, Object> _params) {
    CloudNodeTreeNode nodeTreeNode = as_9d86s3_a0a0a4(event.getData(MPSCommonDataKeys.TREE_NODE), CloudNodeTreeNode.class);
    if (nodeTreeNode == null) {
      return false;
    }
    INode node = nodeTreeNode.getNode();
    if (!(node instanceof PNodeAdapter)) {
      return false;
    }
    return ((PNodeAdapter) node).getNodeId() == PTree.ROOT_ID;
  }
  @Override
  public void doUpdate(@NotNull AnActionEvent event, final Map<String, Object> _params) {
    this.setEnabledState(event.getPresentation(), this.isApplicable(event, _params));
  }
  @Override
  protected boolean collectActionData(AnActionEvent event, final Map<String, Object> _params) {
    if (!(super.collectActionData(event, _params))) {
      return false;
    }
    {
      Project p = event.getData(CommonDataKeys.PROJECT);
      if (p == null) {
        return false;
      }
    }
    {
      TreeNode p = event.getData(MPSCommonDataKeys.TREE_NODE);
      if (p == null) {
        return false;
      }
    }
    return true;
  }
  @Override
  public void doExecute(@NotNull final AnActionEvent event, final Map<String, Object> _params) {
    final CloudNodeTreeNode nodeTreeNode = (CloudNodeTreeNode) event.getData(MPSCommonDataKeys.TREE_NODE);

    final String name = Messages.showInputDialog(event.getData(CommonDataKeys.PROJECT), "Name", "Add Module", null);
    if ((name == null || name.length() == 0)) {
      return;
    }

    nodeTreeNode.getBranch().runWrite(new _FunctionTypes._void_P0_E0() {
      public void invoke() {
        INode newModule = nodeTreeNode.getNode().addNewChild("modules", -1, SConceptAdapter.wrap(CONCEPTS.Module$8V));
        newModule.setPropertyValue(PROPS.name$tAp1.getName(), name);
      }
    });
  }
  private static <T> T as_9d86s3_a0a0a4(Object o, Class<T> type) {
    return (type.isInstance(o) ? (T) o : null);
  }

  private static final class CONCEPTS {
    /*package*/ static final SConcept Module$8V = MetaAdapterFactory.getConcept(0xbf7bc3bb11d42e4L, 0xb16093d72af96397L, 0x69652614fd1c50fL, "de.q60.mps.shadowmodels.runtimelang.structure.Module");
  }

  private static final class PROPS {
    /*package*/ static final SProperty name$tAp1 = MetaAdapterFactory.getProperty(0xceab519525ea4f22L, 0x9b92103b95ca8c0cL, 0x110396eaaa4L, 0x110396ec041L, "name");
  }
}