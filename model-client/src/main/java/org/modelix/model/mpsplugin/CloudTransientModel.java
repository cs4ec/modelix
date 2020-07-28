package org.modelix.model.mpsplugin;

import jetbrains.mps.smodel.EditableModelDescriptor;
import org.jetbrains.mps.openapi.model.EditableSModel;
import de.q60.mps.shadowmodels.runtime.util.IUserObjectContainer;
import jetbrains.mps.extapi.model.TransientSModel;
import de.q60.mps.shadowmodels.runtime.util.pmap.CustomPMap;
import de.q60.mps.shadowmodels.runtime.util.pmap.SmallPMap;
import jetbrains.mps.smodel.SModelId;
import org.jetbrains.mps.openapi.persistence.NullDataSource;
import de.q60.mps.shadowmodels.runtime.util.UserObjectKey;
import org.jetbrains.annotations.NotNull;
import jetbrains.mps.smodel.ModelLoadResult;
import jetbrains.mps.smodel.SModel;
import jetbrains.mps.smodel.SNodeUndoableAction;
import jetbrains.mps.smodel.loading.ModelLoadingState;
import org.jetbrains.mps.openapi.model.SModelReference;
import org.jetbrains.mps.openapi.module.SModuleReference;
import org.jetbrains.mps.openapi.persistence.PersistenceFacade;

public class CloudTransientModel extends EditableModelDescriptor implements EditableSModel, IUserObjectContainer, TransientSModel {

  private final boolean myReadOnly;
  private final boolean myTrackUndo;
  private CustomPMap<Object, Object> userObjects = SmallPMap.empty();
  private ModelSynchronizer synchronizer;

  public CloudTransientModel(CloudTransientModule module, String name, SModelId modelId, final IIndirectBranch branch, long modelNodeId) {
    super(createModelRef(name, module.getModuleReference(), modelId), new NullDataSource());
    myReadOnly = false;
    myTrackUndo = false;

    synchronizer = new ModelSynchronizer(branch, modelNodeId, this);
    runFullSync();
  }

  public ModelSynchronizer getSynchronizer() {
    return this.synchronizer;
  }

  public void runFullSync() {
    synchronizer.runFullSync();
  }

  public void dispose() {
    check_6y45qy_a0a21(synchronizer);
    synchronizer = null;
  }

  @Override
  public <T> T getUserObject(UserObjectKey<T> key) {
    return (T) userObjects.get(key);
  }
  @Override
  public <T> void putUserObject(UserObjectKey<T> key, T value) {
    userObjects = userObjects.put(key, value);
  }
  @Override
  public void updateTimestamp() {
  }
  @Override
  public boolean needsReloading() {
    return false;
  }
  @NotNull
  @Override
  protected ModelLoadResult<SModel> createModel() {
    SModel smodel = new SModel(getReference()) {
      @Override
      protected void performUndoableAction(@NotNull SNodeUndoableAction action) {
        if (myTrackUndo) {
          super.performUndoableAction(action);
        }
      }
    };
    return new ModelLoadResult(smodel, ModelLoadingState.FULLY_LOADED);
  }
  @Override
  public boolean isChanged() {
    return false;
  }
  @Override
  public void save() {
  }
  @Override
  public void rename(String newModelName, boolean changeFile) {
    throw new UnsupportedOperationException();
  }
  @Override
  public boolean isReadOnly() {
    return myReadOnly;
  }
  @Override
  public void reloadFromSource() {
    throw new UnsupportedOperationException();
  }
  private static SModelReference createModelRef(String modelName, SModuleReference moduleReference, SModelId modelId) {
    return PersistenceFacade.getInstance().createModelReference(moduleReference, modelId, modelName);
  }

  private static void check_6y45qy_a0a21(ModelSynchronizer checkedDotOperand) {
    if (null != checkedDotOperand) {
      checkedDotOperand.dispose();
    }

  }
}