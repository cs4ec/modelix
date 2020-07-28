package de.q60.mps.shadowmodels.runtime.model.persistent;

import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;

public interface INodeLoader {
  void loadNode(long id, IWriteTransaction transaction);
  void loadNode(IMissingNode missingNode, IWriteTransaction transaction);
  void runRead(_FunctionTypes._void_P0_E0 r);
}