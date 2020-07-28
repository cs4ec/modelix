package org.modelix.model.mpsplugin.history;

import jetbrains.mps.ide.ui.tree.TextTreeNode;
import org.modelix.model.mpsplugin.CloudRepositories;
import org.modelix.model.mpsplugin.CloudIcons;
import org.modelix.model.mpsplugin.CloudRepository;
import jetbrains.mps.internal.collections.runtime.Sequence;

public class CloudRootTreeNode extends TextTreeNode {
  private boolean myInitialized = false;
  private CloudRepositories.IListener repositoriesListener = new CloudRepositories.IListener() {
    @Override
    public void repositoriesChanged() {
      update();
      init();
    }
  };

  public CloudRootTreeNode() {
    super(CloudIcons.ROOT_ICON, "Cloud");
    setAllowsChildren(true);
    init();
  }

  @Override
  public boolean isInitialized() {
    return myInitialized;
  }

  @Override
  protected void doInit() {
    myInitialized = true;
    populate();
  }

  @Override
  protected void doUpdate() {
    removeAllChildren();
    myInitialized = false;
  }

  protected void populate() {
    for (CloudRepository repo : Sequence.fromIterable(CloudRepositories.getInstance().getRepositories())) {
      add(new CloudRepositoryTreeNode(repo));
    }
  }

  @Override
  protected void onAdd() {
    super.onAdd();
    CloudRepositories.getInstance().addListener(repositoriesListener);
  }

  @Override
  protected void onRemove() {
    super.onRemove();
    CloudRepositories.getInstance().removeListener(repositoriesListener);
  }
}