package org.modelix.model.mpsplugin;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.modelix.model.lazy.TreeId;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import org.modelix.model.operations.OTBranch;
import org.modelix.model.VersionMerger;
import org.modelix.model.lazy.CLVersion;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledFuture;
import org.jetbrains.mps.openapi.repository.CommandListener;
import jetbrains.mps.internal.collections.runtime.Sequence;
import de.q60.mps.shadowmodels.runtime.model.persistent.PTree;
import de.q60.mps.shadowmodels.runtime.model.persistent.IWriteTransaction;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import jetbrains.mps.baseLanguage.closures.runtime.Wrappers;
import org.modelix.model.lazy.CLTree;
import org.modelix.model.operations.IOperation;
import de.q60.mps.shadowmodels.runtime.model.persistent.PBranch;
import java.util.Objects;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranchListener;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITree;
import jetbrains.mps.smodel.MPSModuleRepository;
import jetbrains.mps.baseLanguage.tuples.runtime.Tuples;
import java.util.List;
import org.modelix.model.operations.IAppliedOperation;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import jetbrains.mps.internal.collections.runtime.ISelector;
import java.time.LocalDateTime;

public class ReplicatedTree {
  private static final Logger LOG = LogManager.getLogger(ReplicatedTree.class);

  private IModelClient client;
  private TreeId treeId;
  private String branchName;
  private _FunctionTypes._return_P0_E0<? extends String> user;
  private IBranch localBranch;
  private OTBranch localOTBranch;

  private final Object mergeLock = new Object();
  private VersionMerger merger;
  private volatile CLVersion localVersion;
  private volatile CLVersion remoteVersion;

  private VersionChangeDetector versionChangeDetector;
  private final AtomicBoolean isInCommand = new AtomicBoolean(false);
  private boolean disposed = false;
  private int divergenceTime = 0;
  private ScheduledFuture<?> convergenceWatchdog;

  private CommandListener commandListener = new CommandListener() {
    @Override
    public void commandStarted() {
      isInCommand.set(true);
    }

    @Override
    public void commandFinished() {
      if (disposed) {
        return;
      }
      try {
        synchronized (mergeLock) {
          boolean hasDetachedNodes = localOTBranch.computeRead(new _FunctionTypes._return_P0_E0<Boolean>() {
            public Boolean invoke() {
              return Sequence.fromIterable(localOTBranch.getTransaction().getChildren(PTree.ROOT_ID, ModelSynchronizer.DETACHED_NODES_ROLE)).isNotEmpty();
            }
          });
          // avoid unnecessary write 
          if (hasDetachedNodes) {
            localOTBranch.runWrite(new _FunctionTypes._void_P0_E0() {
              public void invoke() {
                // clear detached nodes 
                IWriteTransaction t = localOTBranch.getWriteTransaction();
                for (long nodeId : t.getChildren(PTree.ROOT_ID, ModelSynchronizer.DETACHED_NODES_ROLE)) {
                  t.deleteNode(nodeId);
                }
              }
            });
          }
          createAndMergeLocalVersion();
        }
      } catch (Exception ex) {
        if (LOG.isEnabledFor(Level.ERROR)) {
          LOG.error("", ex);
        }
      } finally {
        isInCommand.set(false);
      }
    }
  };


  public ReplicatedTree(final IModelClient client, final TreeId treeId, @NotNull final String branchName, _FunctionTypes._return_P0_E0<? extends String> user) {
    this.client = client;
    this.treeId = treeId;
    this.branchName = branchName;
    this.user = user;

    String versionHash = client.get(treeId.getBranchKey(branchName));
    CLVersion initialVersion = CLVersion.loadFromHash(versionHash, client.getStoreCache());
    final Wrappers._T<CLTree> initialTree = new Wrappers._T<CLTree>();
    if (initialVersion == null) {
      initialTree.value = new CLTree(treeId, client.getStoreCache());
      initialVersion = createVersion(initialTree.value, new IOperation[0], null);
      client.put(treeId.getBranchKey(this.branchName), initialVersion.getHash());
    } else {
      initialTree.value = new CLTree(initialVersion.getTreeHash(), client.getStoreCache());
    }

    // prefetch to avoid HTTP request in command listener 
    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        initialTree.value.getChildren(PTree.ROOT_ID, ModelSynchronizer.DETACHED_NODES_ROLE);
      }
    });

    localVersion = initialVersion;
    remoteVersion = initialVersion;

    localBranch = new PBranch(initialTree.value);
    localOTBranch = new OTBranch(localBranch, client.getIdGenerator());
    merger = new VersionMerger(client.getStoreCache(), client.getIdGenerator());
    versionChangeDetector = new VersionChangeDetector(client, treeId.getBranchKey(this.branchName)) {
      @Override
      protected void processVersionChange(String oldVersionHash, String newVersionHash) {
        if (disposed) {
          return;
        }
        if ((newVersionHash == null || newVersionHash.length() == 0)) {
          return;
        }
        if (Objects.equals(newVersionHash, check_6omb18_a0c0a0a0a91a22(remoteVersion))) {
          return;
        }
        final CLVersion newRemoteVersion = CLVersion.loadFromHash(newVersionHash, client.getStoreCache());
        if (newRemoteVersion == null) {
          return;
        }

        final Wrappers._T<CLVersion> localBase = new Wrappers._T<CLVersion>();
        synchronized (mergeLock) {
          localBase.value = localVersion;
          remoteVersion = newRemoteVersion;
        }

        _FunctionTypes._return_P0_E0<? extends Boolean> doMerge = new _FunctionTypes._return_P0_E0<Boolean>() {
          public Boolean invoke() {
            CLVersion mergedVersion;
            try {
              mergedVersion = merger.mergeChange(localBase.value, newRemoteVersion);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Merged remote " + newRemoteVersion.getHash() + " with local " + localBase.value.getHash() + " -> " + mergedVersion.getHash());
              }
            } catch (Exception ex) {
              if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("", ex);
              }
              mergedVersion = newRemoteVersion;
            }
            CLTree mergedTree = mergedVersion.getTree();
            synchronized (mergeLock) {
              remoteVersion = mergedVersion;
              if (localVersion == localBase.value) {
                writeLocalVersion(mergedVersion);
                writeRemoteVersion(mergedVersion);
                return true;
              } else {
                localBase.value = localVersion;
                return false;
              }
            }
          }
        };

        // Avoid locking during the merge as it may require communication with the model server 
        for (int mergeAttempt = 0; mergeAttempt < 3; mergeAttempt++) {
          if (doMerge.invoke()) {
            return;
          }
        }
        synchronized (mergeLock) {
          localBase.value = localVersion;
          doMerge.invoke();
        }
      }
    };

    localOTBranch.addListener(new IBranchListener() {
      @Override
      public void treeChanged(ITree oldTree, ITree newTree) {
        if (disposed) {
          return;
        }
        if (isInCommand.get()) {
          return;
        }
        SharedExecutors.FIXED.execute(new Runnable() {
          public void run() {
            if (isInCommand.get()) {
              return;
            }
            createAndMergeLocalVersion();
          }
        });
      }
    });

    MPSModuleRepository.getInstance().getModelAccess().addCommandListener(commandListener);

    convergenceWatchdog = SharedExecutors.fixDelay(1000, new Runnable() {
      public void run() {
        if (Objects.equals(check_6omb18_a0a0b0a52a22(localVersion), check_6omb18_a0a0b0a52a22_0(remoteVersion))) {
          divergenceTime = 0;
        } else {
          divergenceTime++;
        }
        if (divergenceTime > 5) {
          synchronized (mergeLock) {
            divergenceTime = 0;
          }
        }
      }
    });
  }

  public IBranch getBranch() {
    checkDisposed();
    return localOTBranch;
  }

  public CLVersion getVersion() {
    return localVersion;
  }

  protected void createAndMergeLocalVersion() {
    checkDisposed();

    Tuples._2<List<IAppliedOperation>, ITree> opsAndTree;
    CLVersion localBase;
    final Wrappers._T<CLVersion> remoteBase = new Wrappers._T<CLVersion>();
    final Wrappers._T<CLVersion> newLocalVersion = new Wrappers._T<CLVersion>();
    synchronized (mergeLock) {
      opsAndTree = localOTBranch.getOperationsAndTree();
      localBase = localVersion;
      remoteBase.value = remoteVersion;
      IOperation[] ops = ListSequence.fromList(opsAndTree._0()).select(new ISelector<IAppliedOperation, IOperation>() {
        public IOperation select(IAppliedOperation it) {
          return it.getOriginalOp();
        }
      }).toGenericArray(IOperation.class);
      if (ops.length == 0) {
        return;
      }
      newLocalVersion.value = createVersion((CLTree) opsAndTree._1(), ops, localBase.getHash());
      localVersion = newLocalVersion.value;
      divergenceTime = 0;
    }

    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        _FunctionTypes._return_P0_E0<? extends Boolean> doMerge = new _FunctionTypes._return_P0_E0<Boolean>() {
          public Boolean invoke() {
            CLVersion mergedVersion;
            try {
              mergedVersion = merger.mergeChange(remoteBase.value, newLocalVersion.value);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Merged local " + newLocalVersion.value.getHash() + " with remote " + remoteBase.value.getHash() + " -> " + mergedVersion.getHash());
              }
            } catch (Exception ex) {
              if (LOG.isEnabledFor(Level.ERROR)) {
                LOG.error("", ex);
              }
              mergedVersion = newLocalVersion.value;
            }

            synchronized (mergeLock) {
              writeLocalVersion(localVersion);
              if (remoteVersion == remoteBase.value) {
                writeRemoteVersion(mergedVersion);
                return true;
              } else {
                remoteBase.value = remoteVersion;
                return false;
              }
            }
          }
        };

        // Avoid locking during the merge as it may require communication with the model server 
        for (int mergeAttempt = 0; mergeAttempt < 3; mergeAttempt++) {
          if (doMerge.invoke()) {
            return;
          }
        }
        synchronized (mergeLock) {
          remoteBase.value = remoteVersion;
          doMerge.invoke();
        }
      }
    });
  }

  protected void writeRemoteVersion(CLVersion version) {
    synchronized (mergeLock) {
      if (!(Objects.equals(remoteVersion.getHash(), version.getHash()))) {
        remoteVersion = version;
        client.getAsyncStore().put(treeId.getBranchKey(branchName), version.getHash());
      }
    }
  }

  protected void writeLocalVersion(final CLVersion version) {
    synchronized (mergeLock) {
      if (!(Objects.equals(localVersion.getHash(), version.getHash()))) {
        localVersion = version;
        divergenceTime = 0;
        localBranch.runWrite(new _FunctionTypes._void_P0_E0() {
          public void invoke() {
            CLTree newTree = version.getTree();
            CLTree currentTree = (CLTree) localBranch.getTransaction().getTree();
            if (!(Objects.equals(check_6omb18_a0c0a0a2a0a0a23_0(newTree), check_6omb18_a0c0a0a2a0a0a23(currentTree)))) {
              localBranch.getWriteTransaction().setTree(newTree);
            }
          }
        });
      }
    }
  }

  public CLVersion createVersion(CLTree tree, IOperation[] operations, String previousVersion) {
    checkDisposed();
    String time = LocalDateTime.now().toString();
    return new CLVersion(client.getIdGenerator().generate(), time, user.invoke(), tree.getHash(), previousVersion, operations, client.getStoreCache());
  }

  public void dispose() {
    checkDisposed();
    disposed = true;
    versionChangeDetector.dispose();
    MPSModuleRepository.getInstance().getModelAccess().removeCommandListener(commandListener);
    convergenceWatchdog.cancel(false);
  }

  public void checkDisposed() {
    if (disposed) {
      throw new RuntimeException("Already disposed");
    }
  }
  private static String check_6omb18_a0c0a0a0a91a22(CLVersion checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getHash();
    }
    return null;
  }
  private static String check_6omb18_a0a0b0a52a22(CLVersion checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getHash();
    }
    return null;
  }
  private static String check_6omb18_a0a0b0a52a22_0(CLVersion checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getHash();
    }
    return null;
  }
  private static String check_6omb18_a0c0a0a2a0a0a23(CLTree checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getHash();
    }
    return null;
  }
  private static String check_6omb18_a0c0a0a2a0a0a23_0(CLTree checkedDotOperand) {
    if (null != checkedDotOperand) {
      return checkedDotOperand.getHash();
    }
    return null;
  }
}