package com.reserveamerica.elastica.appserver.jboss;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.TransactionRolledbackException;
import org.jboss.aspects.remoting.ClusterConstants;
import org.jboss.ha.framework.interfaces.ClusteringTargetsRepository;
import org.jboss.ha.framework.interfaces.FamilyClusterInfo;
import org.jboss.ha.framework.interfaces.GenericClusteringException;
import org.jboss.ha.framework.interfaces.LoadBalancePolicy;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.Invoker;
import org.jboss.invocation.MarshalledInvocation;
import org.jboss.invocation.PayloadKey;
import org.jboss.invocation.ServiceUnavailableException;
import org.jboss.invocation.jrmp.interfaces.JRMPInvokerProxyHA;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.UnicastRef2;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;
import com.reserveamerica.commons.ChangeTrackingHashMap;
import com.reserveamerica.commons.ChangeTrackingMap;
import com.reserveamerica.elastica.appserver.AppServerConstants;
import com.reserveamerica.elastica.appserver.NodeRolesView;
import com.reserveamerica.elastica.appserver.NodeStateContext;
import com.reserveamerica.elastica.appserver.NodeStatesManager;
import com.reserveamerica.elastica.appserver.NodeStatesView;
import com.reserveamerica.elastica.cluster.ClientContext;
import com.reserveamerica.elastica.cluster.GlobalContext;
import com.reserveamerica.elastica.cluster.Node;
import com.reserveamerica.elastica.cluster.NodeFactory;
import com.reserveamerica.elastica.cluster.NodeKey;
import com.reserveamerica.elastica.server.ServerState;
import com.reserveamerica.elastica.server.ServerStateView;

/**
 * Subclasses of this JRMP invoker proxy may be used in place of the default JBoss JRMP invoker proxy
 * in order to enable dynamic load balancing (clustering).
 * 
 * @author BStasyszyn
 * @author David Ranalli
 */
public class DynamicClusterJRMPInvokerProxyHA extends JRMPInvokerProxyHA implements DynamicClusterInvokerProxy {

    private static final long serialVersionUID = -7284192546049782111L;

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DynamicClusterJRMPInvokerProxyHA.class);

    private static final Random random = new Random(System.currentTimeMillis());

    private transient FamilyClusterInfo familyClusterInfo;

    private transient String cluster;

    private transient NodeStatesManager nodeStatesManager;

    private NodeStatesView nodeStatesInitialView;

    private NodeRolesView nodeRolesInitialView;

    private transient NodeToTargetManager<NodeView<Invoker>, Invoker> nodeToTargetManager;

    /**
   * Default constructor.
   */
    public DynamicClusterJRMPInvokerProxyHA() {
        super();
    }

    /**
   * This constructor fully initializes the object.
   * 
   * @param targets - A list of available targets in the cluster.
   * @param policy - The JBoss load balance policy.
   * @param proxyFamilyName - This is the partition name appended with the bean name: <partition>/<bean>
   * @param viewId - The unique ID of the view which is used by the client to determine when the list of targets should be refreshed.
   * @param cluster - The logical name of the cluster in which the targets reside.
   */
    public DynamicClusterJRMPInvokerProxyHA(ArrayList<RemoteObject> targets, LoadBalancePolicy policy, String proxyFamilyName, long viewId, String cluster, NodeStatesManager nodeStatesManager) {
        super(targets, policy, proxyFamilyName, viewId);
        this.familyClusterInfo = ClusteringTargetsRepository.getFamilyClusterInfo(proxyFamilyName);
        this.familyClusterInfo.setObject(new AtomicReference<TargetResolver>());
        this.cluster = cluster;
        this.nodeStatesManager = nodeStatesManager;
        this.nodeToTargetManager = new NodeToTargetManager<NodeView<Invoker>, Invoker>(proxyFamilyName, cluster, familyClusterInfo, new JRMPNodeToTargetMapper());
    }

    /**
   * The logical name of the cluster with which this proxy is communicating.
   * 
   * @return String
   */
    public String getCluster() {
        return cluster;
    }

    public Collection<NodeView<Invoker>> getNodeViews() {
        return Collections.unmodifiableCollection(nodeToTargetManager.getNodeViews());
    }

    public Object invoke(Invocation invocation) throws Exception {
        NodeStateContext nodeStateContext = new NodeStateContext(invocation.getMethod());
        try {
            return innerInvoke(invocation);
        } finally {
            nodeStateContext.close();
        }
    }

    private Object innerInvoke(final Invocation invocation) throws Exception {
        int failoverCounter = 0;
        invocation.setValue("FAILOVER_COUNTER", Integer.valueOf(failoverCounter), PayloadKey.AS_IS);
        final MarshalledInvocation mi = createMarshalledInvocation(invocation);
        mi.setTransactionPropagationContext(getTransactionPropagationContext());
        mi.setValue(ClusterConstants.CLUSTER_VIEW_ID, Long.valueOf(getCurrentViewId()));
        nodeToTargetManager.checkNodeToTargetMap();
        NodeView<Invoker> nodeView = chooseNode(invocation, mi);
        final String overrideNodeId = ClientContext.getInstance().getOverrideNodeId();
        if (overrideNodeId != null) {
            mi.setValue(AppServerConstants.OVERRIDE_OFFLINE_NODE, ClientContext.getInstance().getOverrideState());
        }
        boolean failoverAuthorized = true;
        Exception lastException = null;
        while (nodeView != null && failoverAuthorized && (failoverCounter < 3)) {
            boolean definitivlyRemoveNodeOnFailure = true;
            try {
                mi.setValue(AppServerConstants.STATE_VIEW_ID, nodeView.getStateView().getViewId());
                mi.setValue(AppServerConstants.NODE_ROLES_VIEW_ID, nodeView.getRolesView().getViewId());
                if (!GlobalContext.getInstance().isEmpty()) {
                    Map<String, Object> properties = GlobalContext.getInstance().getProperties();
                    if (properties instanceof ChangeTrackingHashMap) {
                        properties = new HashMap<String, Object>(properties);
                    }
                    mi.setValue(AppServerConstants.GLOBAL_CONTEXT, properties);
                }
                RAHARMIResponse rsp;
                Long requestId = nodeView.startRequest();
                try {
                    rsp = invokeTarget(nodeView.getRemoteTarget(), mi);
                } finally {
                    nodeView.endRequest(requestId);
                }
                if (nodeToTargetManager.checkForViewUpdate(new NodeViewUpdateResponseWrapper(rsp), nodeView)) {
                    nodeView = nodeToTargetManager.getNodeView(nodeView);
                }
                if (!rsp.invoked && (mi.getValue(AppServerConstants.REFRESH_VIEW) == null)) {
                    ServerState state = (nodeView != null) ? nodeView.getState() : ServerState.UNAVAILABLE;
                    if (log.isDebugEnabled()) log.debug("invoke - The EJB was not invoked. Server's state is [" + state + "].");
                    if ((overrideNodeId != null) && !overrideNodeId.equals(ClientContext.ALL_NODES_OVERRIDE_PROPERTY_VALUE)) {
                        if (log.isInfoEnabled()) log.info("invoke - The EJB was not invoked since the server's state is [" + state + "]. Unable to select another node since the override was set to [" + overrideNodeId + "].");
                        throw new RemoteException("The EJB was not invoked since the server's state is [" + state + "]. Unable to select another node since the override was set to [" + overrideNodeId + "].");
                    }
                    if ((failoverCounter == 0) && (nodeView != null)) {
                        if (log.isDebugEnabled()) log.debug("invoke - Attempting to choose a new target. Current target is [" + state + "].");
                        NodeView<Invoker> previousNodeView = nodeView;
                        nodeView = chooseNode(invocation, mi);
                        if (nodeView == previousNodeView) {
                            if (log.isInfoEnabled()) log.info("invoke - The attempt to choose a new target resulted in the same target being chosen. Cannot allow the invocation to proceed since the server's state is [" + state + "].");
                            throw new RemoteException("Unable to invoke the server since the server's state is [" + state + "].");
                        }
                        if (log.isDebugEnabled()) log.debug("invoke - Retrying invocation with a new target since the last target was [" + state + "].");
                        failoverCounter++;
                        continue;
                    }
                    if (log.isInfoEnabled()) log.info("invoke - The retry attempt has failed with the new target. Cannot allow the invocation to proceed since the server's state is [" + state + "].");
                    throw new RemoteException("Unable to invoke the server since the server's state is [" + state + "].");
                }
                invocationHasReachedAServer(invocation);
                if (nodeView != null) {
                    invocation.setValue("cluster-node", nodeView, PayloadKey.AS_IS);
                }
                if (rsp.serverData != null) {
                    GlobalContext.getInstance().addProperties(rsp.serverData);
                    if (rsp.serverData instanceof ChangeTrackingMap) {
                        for (String key : ((ChangeTrackingMap<String, Object>) rsp.serverData).getRemovedKeys()) {
                            GlobalContext.getInstance().removeProperty(key);
                        }
                    }
                }
                return rsp.response;
            } catch (java.net.ConnectException e) {
                lastException = e;
            } catch (java.net.UnknownHostException e) {
                lastException = e;
            } catch (java.rmi.ConnectException e) {
                lastException = e;
            } catch (java.rmi.ConnectIOException e) {
                lastException = e;
            } catch (java.rmi.NoSuchObjectException e) {
                lastException = e;
            } catch (java.rmi.UnknownHostException e) {
                lastException = e;
            } catch (java.rmi.MarshalException e) {
                lastException = e;
            } catch (java.rmi.UnmarshalException e) {
                lastException = e;
            } catch (GenericClusteringException e) {
                lastException = e;
                if (e.getCompletionStatus() == GenericClusteringException.COMPLETED_NO) {
                    if (totalNumberOfTargets() >= failoverCounter) {
                        if (!e.isDefinitive()) definitivlyRemoveNodeOnFailure = false;
                    }
                } else {
                    invocationHasReachedAServer(invocation);
                    throw new ServerException("Clustering error", e);
                }
            } catch (ServerException e) {
                invocationHasReachedAServer(invocation);
                if (e.detail instanceof TransactionRolledbackException) {
                    throw (TransactionRolledbackException) e.detail;
                }
                if (e.detail instanceof RemoteException) {
                    throw (RemoteException) e.detail;
                }
                throw e;
            } catch (Exception e) {
                invocationHasReachedAServer(invocation);
                throw e;
            } catch (Throwable e) {
                lastException = new Exception("Unable to invoke target. Reason: " + e.getMessage(), e);
            }
            ServerStateView stateView = nodeView.getStateView();
            if (stateView.getState() == ServerState.ONLINE) {
                if (nodeView.compareAndSetStateView(stateView, new ServerStateView(random.nextLong(), ServerState.UNAVAILABLE))) {
                    log.warn("invoke - Unable to invoke target " + nodeView + ". Set its state to UNAVAILABLE.");
                }
            }
            nodeToTargetManager.checkNodeToTargetMap();
            failoverAuthorized = txContextAllowsFailover(invocation);
            nodeView = chooseNode(invocation, mi);
            failoverCounter++;
            mi.setValue("FAILOVER_COUNTER", Integer.valueOf(failoverCounter), PayloadKey.AS_IS);
        }
        nodeToTargetManager.checkNodeToTargetMap();
        String msg = "Service unavailable.";
        if (failoverAuthorized == false) {
            msg = "Service unavailable (failover not possible inside a user transaction).";
        }
        throw new ServiceUnavailableException(msg, lastException);
    }

    protected RAHARMIResponse invokeTarget(Invoker target, MarshalledInvocation mi) throws Exception {
        Object rtnObj = target.invoke(mi);
        RAHARMIResponse rsp;
        if (rtnObj instanceof MarshalledObject) {
            MarshalledObject marshalledRtnObj = (MarshalledObject) rtnObj;
            rsp = (RAHARMIResponse) marshalledRtnObj.get();
        } else {
            rsp = (RAHARMIResponse) rtnObj;
        }
        return rsp;
    }

    protected MarshalledInvocation createMarshalledInvocation(Invocation invocation) {
        return new MarshalledInvocation(invocation);
    }

    /**
   * The current view ID stored in the Family Cluster Info.
   * 
   * @return long
   */
    protected long getCurrentViewId() {
        return familyClusterInfo.getCurrentViewId();
    }

    /**
   * The list of targets stored in the Family Cluster Info.
   * @return List
   */
    @SuppressWarnings("unchecked")
    protected List<Invoker> getTargets() {
        return familyClusterInfo.getTargets();
    }

    /**
   * Chooses a {@link NodeView} from the given invocation. This method performs a refresh-view request if the
   * {@link NodeView} comes back as <tt>null</tt> from the load balancer. If the response of the refresh-view
   * request indicates that the views need to be refreshed then a refresh is performed and
   * the load balancer is invoked again in order to choose another {@link NodeView} .
   * 
   * @param invocation
   * @param mi
   * @return NodeToTargetMap.NodeView - The node view - can be <tt>null</tt>.
   */
    protected NodeView<Invoker> chooseNode(Invocation invocation, MarshalledInvocation mi) {
        NodeView<Invoker> nodeView = chooseNode(invocation);
        if (nodeView != null) {
            return nodeView;
        }
        String overrideNodeId = ClientContext.getInstance().getOverrideNodeId();
        if ((overrideNodeId != null) && !overrideNodeId.equals(ClientContext.ALL_NODES_OVERRIDE_PROPERTY_VALUE)) {
            return null;
        }
        boolean targetsUpdated = false;
        mi.setValue(AppServerConstants.REFRESH_VIEW, "");
        try {
            for (Invoker refreshTarget : getTargets()) {
                NodeView<Invoker> nv = nodeToTargetManager.getNodeView(refreshTarget);
                if (nv == null) {
                    continue;
                }
                try {
                    Object rtnObj = refreshTarget.invoke(mi);
                    RAHARMIResponse rsp = null;
                    if (rtnObj instanceof MarshalledObject) {
                        rsp = (RAHARMIResponse) ((MarshalledObject) rtnObj).get();
                    } else {
                        rsp = (RAHARMIResponse) rtnObj;
                    }
                    if (nodeToTargetManager.checkForViewUpdate(new NodeViewUpdateResponseWrapper(rsp), nv)) {
                        targetsUpdated = true;
                        break;
                    }
                } catch (Throwable t) {
                    log.debug("An error occurred while attempting to retrieve target information from target [" + refreshTarget + "].", t);
                }
            }
        } finally {
            mi.setValue(AppServerConstants.REFRESH_VIEW, null);
        }
        if (targetsUpdated) {
            nodeView = chooseNode(invocation);
            mi.setValue(ClusterConstants.CLUSTER_VIEW_ID, Long.valueOf(getCurrentViewId()));
        }
        return nodeView;
    }

    /**
   * Chooses a {@link NodeToTargetMap.NodeView} from the given invocation.
   * 
   * @param invocation
   * @return NodeToTargetMap.NodeView - The node view - can be <tt>null</tt>.
   */
    protected NodeView<Invoker> chooseNode(Invocation invocation) {
        return nodeToTargetManager.getNodeViewFromTarget(getRemoteTarget(invocation));
    }

    private static NodeKey getNodeKeyFromTarget(RemoteObject remoteObject) throws Exception {
        UnicastRef2 ref = (UnicastRef2) remoteObject.getRef();
        LiveRef liveRef = (LiveRef) getField(ref, UnicastRef.class, "ref");
        TCPEndpoint endpoint = (TCPEndpoint) liveRef.getChannel().getEndpoint();
        String host = endpoint.getHost();
        int port = endpoint.getPort();
        return NodeKey.getKey(host, port);
    }

    private static Object getField(Object src, Class<?> clazz, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(src);
    }

    /**
  *  Externalize this instance.
  */
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(cluster);
        out.writeObject(nodeStatesManager.getNodeStatesView());
        out.writeObject(nodeStatesManager.getNodeRolesView());
    }

    /**
   *  Un-externalize this instance.
   */
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.cluster = (String) in.readObject();
        this.nodeStatesInitialView = (NodeStatesView) in.readObject();
        this.nodeRolesInitialView = (NodeRolesView) in.readObject();
        this.familyClusterInfo = ClusteringTargetsRepository.getFamilyClusterInfo(proxyFamilyName);
        this.familyClusterInfo.setObject(new AtomicReference<TargetResolver>());
        this.nodeToTargetManager = new NodeToTargetManager<NodeView<Invoker>, Invoker>(proxyFamilyName, cluster, familyClusterInfo, new JRMPNodeToTargetMapper());
        NodeViewUpdateImpl initialView = new NodeViewUpdateImpl();
        initialView.setServerStatesViewId(nodeStatesInitialView.getViewId());
        initialView.setServerStates(nodeStatesInitialView.getStates());
        initialView.setServerRolesViewId(nodeRolesInitialView.getViewId());
        initialView.setServerRoles(nodeRolesInitialView.getNodeRoleIds());
        try {
            nodeToTargetManager.initializeNodeToTargetMap(initialView);
        } catch (ServiceUnavailableException ex) {
            log.error("readExternal - Unable to initialize node-to-target map.", ex);
        }
    }

    private static class JRMPNodeToTargetMapper implements NodeToTargetMapper<NodeView<Invoker>, Invoker> {

        public NodeToTargetMap<NodeView<Invoker>, Invoker> mapNodesToTargets(long clusterConfigVersion, long clusterFamilyViewId, List<Invoker> targets, Map<NodeKey, Node> nodeMap, NodeFactory nodeFactory) throws Exception {
            NodeToTargetMap<NodeView<Invoker>, Invoker> nodeToTargetMap = new NodeToTargetMap<NodeView<Invoker>, Invoker>(clusterConfigVersion, clusterFamilyViewId);
            for (Invoker target : targets) {
                NodeKey nodeKey = getNodeKeyFromTarget((RemoteObject) target);
                Node node = nodeMap.get(nodeKey);
                if (node == null) {
                    node = nodeFactory.createNode(nodeKey);
                    if (log.isInfoEnabled()) log.info("mapNodesToTargets - A dynamic target [" + target + "] has become available. Adding dynamic node: " + node);
                }
                nodeToTargetMap.add(new NodeView<Invoker>(node, target));
            }
            return nodeToTargetMap;
        }
    }
}
