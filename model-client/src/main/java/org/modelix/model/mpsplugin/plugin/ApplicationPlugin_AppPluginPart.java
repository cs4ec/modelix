package org.modelix.model.mpsplugin.plugin;

import jetbrains.mps.plugins.part.ApplicationPluginPart;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import com.intellij.openapi.application.ApplicationManager;
import org.modelix.model.mpsplugin.CloudRepositories;
import jetbrains.mps.internal.collections.runtime.Sequence;
import org.modelix.model.lazy.TreeId;
import org.apache.log4j.Level;
import org.modelix.model.mpsplugin.CloudTransientModules;
import org.modelix.model.mpsplugin.SharedExecutors;

public class ApplicationPlugin_AppPluginPart extends ApplicationPluginPart {
  private static final Logger LOG = LogManager.getLogger(ApplicationPlugin_AppPluginPart.class);
  private BindAllModulesToTransient bindAll;
  public ApplicationPlugin_AppPluginPart() {
  }
  @Override
  public void init() {
    try {
      if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
        String treeId = System.getProperty("TREE_ID");
        if ((treeId != null && treeId.length() > 0)) {
          ApplicationPlugin_AppPluginPart.this.bindAll = new BindAllModulesToTransient(CloudRepositories.getInstance(), Sequence.<TreeId>singleton(new TreeId(treeId)));
        } else {
          ApplicationPlugin_AppPluginPart.this.bindAll = new BindAllModulesToTransient(CloudRepositories.getInstance());
        }
      }
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }
  }
  @Override
  public void dispose() {
    try {
      if (ApplicationPlugin_AppPluginPart.this.bindAll != null) {
        ApplicationPlugin_AppPluginPart.this.bindAll.dispose();
      }
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }

    try {
      CloudRepositories.getInstance().dispose();
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }


    try {
      CloudTransientModules.getInstance().dispose();
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }

    try {
      SharedExecutors.shutdownAll();
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }
  }
}
