package org.modelix.model.mpsplugin;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.modelix.model.lazy.TreeId;
import java.util.Map;
import jetbrains.mps.internal.collections.runtime.MapSequence;
import java.util.HashMap;
import java.util.List;
import jetbrains.mps.internal.collections.runtime.ListSequence;
import java.util.ArrayList;
import com.intellij.ide.util.PropertiesComponent;
import jetbrains.mps.baseLanguage.closures.runtime._FunctionTypes;
import org.apache.log4j.Level;
import org.modelix.common.AuthorOverride;
import org.jetbrains.mps.openapi.model.SNode;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SNodeOperations;
import de.q60.mps.shadowmodels.runtime.smodel.SNodeAPI;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SPropertyOperations;
import jetbrains.mps.lang.smodel.generator.smodelAdapter.SLinkOperations;
import jetbrains.mps.internal.collections.runtime.IWhereFilter;
import java.util.Objects;
import de.q60.mps.shadowmodels.runtime.model.persistent.IBranch;
import org.jetbrains.annotations.NotNull;
import de.q60.mps.shadowmodels.runtime.model.persistent.ITransaction;
import jetbrains.mps.internal.collections.runtime.Sequence;
import de.q60.mps.shadowmodels.runtime.model.persistent.PTree;
import jetbrains.mps.internal.collections.runtime.ISelector;
import de.q60.mps.shadowmodels.runtime.smodel.NodeToSNodeAdapter;
import de.q60.mps.shadowmodels.runtime.model.persistent.PNodeAdapter;
import de.q60.mps.shadowmodels.runtime.model.persistent.IWriteTransaction;
import de.q60.mps.shadowmodels.runtime.smodel.SConceptAdapter;
import jetbrains.mps.smodel.MPSModuleRepository;
import org.jetbrains.mps.openapi.language.SContainmentLink;
import jetbrains.mps.smodel.adapter.structure.MetaAdapterFactory;
import org.jetbrains.mps.openapi.language.SConcept;
import org.jetbrains.mps.openapi.language.SProperty;

public class CloudRepository {
  private static final Logger LOG = LogManager.getLogger(CloudRepository.class);
  private static final TreeId INFO_TREE_ID = new TreeId("info");
  private String SETTINGS_KEY_PREFIX = CloudRepository.class.getName() + ".token/";

  private String baseUrl;
  private RestWebModelClient client;
  private ReplicatedTree infoTree;
  private final Map<TreeId, ActiveBranch> activeBranches = MapSequence.fromMap(new HashMap<TreeId, ActiveBranch>());
  private List<ModelBinding> bindings = ListSequence.fromList(new ArrayList<ModelBinding>());
  private List<IListener> listeners = ListSequence.fromList(new ArrayList<IListener>());
  private boolean connected = false;
  private String id;
  private String authToken;
  private String email;

  public CloudRepository(String baseUrl) {
    this.baseUrl = baseUrl;
    authToken = PropertiesComponent.getInstance().getValue(SETTINGS_KEY_PREFIX + baseUrl);
    reconnect();
  }

  public void reconnect() {
    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        try {
          if (client == null) {
            client = new RestWebModelClient(baseUrl);
          }
          if (authToken != null) {
            client.setAuthToken(authToken);
          }
          id = client.get("repositoryId");
          if ((id == null || id.length() == 0)) {
            throw new RuntimeException(baseUrl + " doesn't provide an ID");
          }
          if (infoTree == null) {
            infoTree = new ReplicatedTree(client, INFO_TREE_ID, ActiveBranch.DEFAULT_BRANCH_NAME, new _FunctionTypes._return_P0_E0<String>() {
              public String invoke() {
                return getAuthor();
              }
            });
          }
          try {
            email = client.getEmail();
          } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
              LOG.error("Failed to read the users e-mail address", ex);
            }
          }
          connected = true;
          for (IListener l : ListSequence.fromList(listeners)) {
            l.connectionStatusChanged(true);
          }
        } catch (Exception ex) {
          if (LOG.isEnabledFor(Level.ERROR)) {
            LOG.error("Failed to connected to " + baseUrl, ex);
          }
        }
      }
    });
  }

  public void setAuthToken(String token) {
    this.authToken = token;
    PropertiesComponent.getInstance().setValue(SETTINGS_KEY_PREFIX + baseUrl, token);
    reconnect();
  }

  public String getEmail() {
    return email;
  }

  private String getAuthor() {
    return AuthorOverride.apply(getEmail());
  }

  public boolean isConnected() {
    return connected;
  }

  public String getId() {
    return this.id;
  }

  protected void checkConnected() {
    if (!(connected)) {
      throw new IllegalStateException("Not connected.");
    }
  }

  public SNode addTree(final String name, final String id) {
    return getInfoBranch().computeWrite(new _FunctionTypes._return_P0_E0<SNode>() {
      public SNode invoke() {
        SNode info = getInfo();
        SNode treeInfo = SNodeOperations.cast(SNodeAPI.addNewChild(info, LINKS.trees$ECBY), CONCEPTS.TreeInfo$mh);
        SPropertyOperations.assign(treeInfo, PROPS.name$tAp1, name);
        SPropertyOperations.assign(treeInfo, PROPS.id$ECO6, id);
        SNode branchInfo = SNodeOperations.cast(SNodeAPI.addNewChild(treeInfo, LINKS.branches$ECCX), CONCEPTS.BranchInfo$mK);
        SPropertyOperations.assign(branchInfo, PROPS.name$tAp1, ActiveBranch.DEFAULT_BRANCH_NAME);
        return treeInfo;
      }
    });
  }

  public void removeTree(final String id) {
    getInfoBranch().computeWrite(new _FunctionTypes._return_P0_E0<SNode>() {
      public SNode invoke() {
        SNode info = getInfo();
        return SNodeOperations.deleteNode(ListSequence.fromList(SLinkOperations.getChildren(info, LINKS.trees$ECBY)).findFirst(new IWhereFilter<SNode>() {
          public boolean accept(SNode it) {
            return Objects.equals(SPropertyOperations.getString(it, PROPS.id$ECO6), id);
          }
        }));
      }
    });
  }

  public void addBinding(final ModelBinding binding) {
    ListSequence.fromList(bindings).addElement(binding);
    SharedExecutors.FIXED.execute(new Runnable() {
      public void run() {
        binding.activate(CloudRepository.this);
      }
    });
  }

  public boolean hasBinding(final TreeId treeId, final long moduleNodeId) {
    return ListSequence.fromList(bindings).any(new IWhereFilter<ModelBinding>() {
      public boolean accept(ModelBinding it) {
        return Objects.equals(it.getTreeId(), treeId) && Objects.equals(it.getNodeId(), moduleNodeId);
      }
    });
  }

  public Iterable<ModelBinding> getBindings() {
    return bindings;
  }

  public IBranch getInfoBranch() {
    checkConnected();
    return infoTree.getBranch();
  }

  @NotNull
  public SNode getInfo() {
    checkConnected();
    SNode result = infoTree.getBranch().computeRead(new _FunctionTypes._return_P0_E0<SNode>() {
      public SNode invoke() {
        ITransaction t = infoTree.getBranch().getTransaction();
        return Sequence.fromIterable(SNodeOperations.ofConcept(Sequence.fromIterable(t.getAllChildren(PTree.ROOT_ID)).select(new ISelector<Long, SNode>() {
          public SNode select(Long it) {
            return (SNode) NodeToSNodeAdapter.wrap(new PNodeAdapter(it, infoTree.getBranch()));
          }
        }), CONCEPTS.RepositoryInfo$lM)).first();
      }
    });
    if (result == null) {
      result = infoTree.getBranch().computeWrite(new _FunctionTypes._return_P0_E0<SNode>() {
        public SNode invoke() {
          IWriteTransaction t = infoTree.getBranch().getWriteTransaction();
          long id = t.addNewChild(PTree.ROOT_ID, "info", -1, SConceptAdapter.wrap(CONCEPTS.RepositoryInfo$lM));
          SNode repoInfo = SNodeOperations.cast(NodeToSNodeAdapter.wrap(new PNodeAdapter(id, infoTree.getBranch())), CONCEPTS.RepositoryInfo$lM);

          addTree("default tree", "default");

          return repoInfo;
        }
      });
    }
    return result;
  }

  public ActiveBranch getActiveBranch(TreeId treeId) {
    checkConnected();
    synchronized (activeBranches) {
      ActiveBranch ab = MapSequence.fromMap(activeBranches).get(treeId);
      if (ab == null) {
        ab = new ActiveBranch(client, treeId, null, new _FunctionTypes._return_P0_E0<String>() {
          public String invoke() {
            return getAuthor();
          }
        });
        MapSequence.fromMap(activeBranches).put(treeId, ab);
      }
      return ab;
    }
  }

  public synchronized void dispose() {
    try {
      check_t1xzmn_a0a0a64(client);
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }
    try {
      check_t1xzmn_a0a1a64(infoTree);
    } catch (Exception ex) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("", ex);
      }
    }
    synchronized (activeBranches) {
      for (ActiveBranch ab : Sequence.fromIterable(MapSequence.fromMap(activeBranches).values())) {
        try {
          ab.dispose();
        } catch (Exception ex) {
          if (LOG.isEnabledFor(Level.ERROR)) {
            LOG.error("", ex);
          }
        }
      }
      MapSequence.fromMap(activeBranches).clear();
    }
    List<ModelBinding> bindingsToDeacvtivate = ListSequence.fromListWithValues(new ArrayList<ModelBinding>(), bindings);
    ListSequence.fromList(bindings).clear();

    WriteAccessUtil.runWrite(MPSModuleRepository.getInstance(), new Runnable() {
      public void run() {
        for (ModelBinding binding : ListSequence.fromList(bindings)) {
          try {
            binding.deactivate();
          } catch (Exception ex) {
            if (LOG.isEnabledFor(Level.ERROR)) {
              LOG.error("", ex);
            }
          }
        }
      }
    });
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public IModelClient getClient() {
    checkConnected();
    return this.client;
  }

  @Override
  public String toString() {
    return baseUrl;
  }

  public void addListener(IListener l) {
    List<IListener> newListeners = ListSequence.fromListWithValues(new ArrayList<IListener>(), listeners);
    ListSequence.fromList(newListeners).addElement(l);
    listeners = newListeners;
  }

  public void removeListener(IListener l) {
    List<IListener> newListeners = ListSequence.fromListWithValues(new ArrayList<IListener>(), listeners);
    ListSequence.fromList(newListeners).removeElement(l);
    listeners = newListeners;
  }

  public interface IListener {
    void connectionStatusChanged(boolean connected);
  }
  private static void check_t1xzmn_a0a0a64(RestWebModelClient checkedDotOperand) {
    if (null != checkedDotOperand) {
      checkedDotOperand.dispose();
    }

  }
  private static void check_t1xzmn_a0a1a64(ReplicatedTree checkedDotOperand) {
    if (null != checkedDotOperand) {
      checkedDotOperand.dispose();
    }

  }

  private static final class LINKS {
    /*package*/ static final SContainmentLink trees$ECBY = MetaAdapterFactory.getContainmentLink(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcbfL, 0x62b7d9b07cecbcc2L, "trees");
    /*package*/ static final SContainmentLink branches$ECCX = MetaAdapterFactory.getContainmentLink(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc0L, 0x62b7d9b07cecbcc4L, "branches");
  }

  private static final class CONCEPTS {
    /*package*/ static final SConcept TreeInfo$mh = MetaAdapterFactory.getConcept(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc0L, "org.modelix.model.runtimelang.structure.TreeInfo");
    /*package*/ static final SConcept BranchInfo$mK = MetaAdapterFactory.getConcept(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc1L, "org.modelix.model.runtimelang.structure.BranchInfo");
    /*package*/ static final SConcept RepositoryInfo$lM = MetaAdapterFactory.getConcept(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcbfL, "org.modelix.model.runtimelang.structure.RepositoryInfo");
  }

  private static final class PROPS {
    /*package*/ static final SProperty name$tAp1 = MetaAdapterFactory.getProperty(0xceab519525ea4f22L, 0x9b92103b95ca8c0cL, 0x110396eaaa4L, 0x110396ec041L, "name");
    /*package*/ static final SProperty id$ECO6 = MetaAdapterFactory.getProperty(0xb6980ebdf01d459dL, 0xa95238740f6313b4L, 0x62b7d9b07cecbcc0L, 0x62b7d9b07cecbcc6L, "id");
  }
}
