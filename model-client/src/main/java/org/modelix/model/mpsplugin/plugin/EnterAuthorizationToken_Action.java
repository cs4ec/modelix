package org.modelix.model.mpsplugin.plugin;

import jetbrains.mps.workbench.action.BaseAction;
import javax.swing.Icon;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.util.Map;
import jetbrains.mps.ide.actions.MPSCommonDataKeys;
import org.modelix.model.mpsplugin.history.CloudRepositoryTreeNode;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import javax.swing.tree.TreeNode;
import org.modelix.model.mpsplugin.CloudRepository;
import com.intellij.openapi.ui.Messages;
import org.modelix.model.mpsplugin.CloudIcons;

public class EnterAuthorizationToken_Action extends BaseAction {
  private static final Icon ICON = null;

  public EnterAuthorizationToken_Action() {
    super("Enter Authorization Token", "", ICON);
    this.setIsAlwaysVisible(false);
    this.setExecuteOutsideCommand(true);
  }
  @Override
  public boolean isDumbAware() {
    return true;
  }
  @Override
  public boolean isApplicable(AnActionEvent event, final Map<String, Object> _params) {
    return event.getData(MPSCommonDataKeys.TREE_NODE) instanceof CloudRepositoryTreeNode;
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
    CloudRepository cloudRepository = ((CloudRepositoryTreeNode) event.getData(MPSCommonDataKeys.TREE_NODE)).getCloudRepository();

    String token = Messages.showInputDialog(event.getData(CommonDataKeys.PROJECT), "Token", "Set Authorization Token", CloudIcons.TREE_ICON);
    if ((token == null || token.length() == 0)) {
      return;
    }
    cloudRepository.setAuthToken(token);
  }
}