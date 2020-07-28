package org.modelix.model.mpsplugin.plugin;

import jetbrains.mps.plugins.applicationplugins.BaseApplicationPlugin;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import jetbrains.mps.plugins.part.ApplicationPluginPart;

public class Mpsplugin_ApplicationPlugin extends BaseApplicationPlugin {
  private final PluginId myId = PluginId.getId("org.modelix.model.mpsplugin");

  public Mpsplugin_ApplicationPlugin() {
  }

  @NotNull
  public PluginId getId() {
    return myId;
  }

  public void createGroups() {
    // actions w/o parameters 
    addAction(new AddBranch_Action());
    addAction(new AddCloudRepository_Action());
    addAction(new AddModelNode_Action());
    addAction(new AddModuleNode_Action());
    addAction(new AddTransientModuleBinding_Action());
    addAction(new AddTree_Action());
    addAction(new EnterAuthorizationToken_Action());
    addAction(new GetAuthorizationToken_Action());
    addAction(new LoadHistoryForBranch_Action());
    addAction(new LoadHistoryForTree_Action());
    addAction(new RemoveCloudRepository_Action());
    addAction(new RemoveTree_Action());
    addAction(new SwitchBranch_Action());
    // groups 
    addGroup(new CloudBranchGroup_ActionGroup(this));
    addGroup(new CloudNodeGroup_ActionGroup(this));
    addGroup(new CloudRepositoryGroup_ActionGroup(this));
    addGroup(new CloudRootGroup_ActionGroup(this));
    addGroup(new CloudTreeGroup_ActionGroup(this));
  }
  public void adjustRegularGroups() {
  }
  @Override
  public void fillCustomParts(List<ApplicationPluginPart> parts) {
    parts.add(new ApplicationPlugin_AppPluginPart());
  }
}