package de.q60.mps.shadowmodels.runtime.model.persistent;

import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;

public interface IBranch {
  void runRead(_FunctionTypes._void_P0_E0 runnable);
  <T> T computeRead(_FunctionTypes._return_P0_E0<? extends T> computable);
  void runWrite(_FunctionTypes._void_P0_E0 runnable);
  <T> T computeWrite(_FunctionTypes._return_P0_E0<? extends T> computable);

  boolean canRead();
  boolean canWrite();

  ITransaction getTransaction();
  IReadTransaction getReadTransaction();
  IWriteTransaction getWriteTransaction();

  void addListener(IBranchListener l);
  void removeListener(IBranchListener l);
}
