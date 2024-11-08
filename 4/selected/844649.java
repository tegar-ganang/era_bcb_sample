package org.ourgrid.acceptance.util;

import static org.ourgrid.peer.PeerConstants.ACCOUNTING_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.CLIENT_MONITOR_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.DS_MONITOR_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.LOCAL_ACCESS_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.REMOTE_ACCESS_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.REMOTE_WORKER_MANAGEMENT_CLIENT;
import static org.ourgrid.peer.PeerConstants.REMOTE_WORKER_MONITOR_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.REMOTE_WORKER_PROVIDER_CLIENT_MONITOR;
import static org.ourgrid.peer.PeerConstants.WORKER_MANAGEMENT_CLIENT_OBJECT_NAME;
import static org.ourgrid.peer.PeerConstants.WORKER_MONITOR_OBJECT_NAME;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.apache.commons.io.FileUtils;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.ourgrid.common.config.Configuration;
import org.ourgrid.common.interfaces.DiscoveryServiceClient;
import org.ourgrid.common.interfaces.LocalWorkerProvider;
import org.ourgrid.common.interfaces.PeerAccounting;
import org.ourgrid.common.interfaces.RemoteWorkerProvider;
import org.ourgrid.common.interfaces.RemoteWorkerProviderClient;
import org.ourgrid.common.interfaces.WorkerSpecListener;
import org.ourgrid.common.interfaces.control.PeerControl;
import org.ourgrid.common.interfaces.management.RemoteWorkerManagementClient;
import org.ourgrid.common.interfaces.management.WorkerManagementClient;
import org.ourgrid.common.interfaces.status.PeerStatusProvider;
import org.ourgrid.peer.PeerComponent;
import org.ourgrid.peer.PeerConfiguration;
import org.ourgrid.peer.PeerConstants;
import org.ourgrid.peer.controller.RemoteWorkerFailureController;
import org.ourgrid.peer.controller.RemoteWorkerProviderClientFailureController;
import org.ourgrid.peer.controller.WorkerNotificationController;
import org.ourgrid.peer.controller.WorkerProviderClientFailureController;
import org.ourgrid.peer.controller.ds.DiscoveryServiceClientController;
import org.ourgrid.peer.controller.ds.DiscoveryServiceNotificationController;
import br.edu.ufcg.lsd.commune.Application;
import br.edu.ufcg.lsd.commune.container.ContainerContext;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.control.ApplicationManager;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.container.servicemanager.actions.RepetitionRunnable;
import br.edu.ufcg.lsd.commune.identification.ContainerID;
import br.edu.ufcg.lsd.commune.identification.DeploymentID;
import br.edu.ufcg.lsd.commune.identification.ServiceID;
import br.edu.ufcg.lsd.commune.network.xmpp.CommuneNetworkException;
import br.edu.ufcg.lsd.commune.processor.ProcessorStartException;
import br.edu.ufcg.lsd.commune.test.AcceptanceTestUtil;
import br.edu.ufcg.lsd.commune.test.TestObjectsRegistry;

public class PeerAcceptanceUtil extends AcceptanceUtil {

    public PeerAcceptanceUtil(ContainerContext context) {
        super(context);
    }

    public static final int DEFAULT_ADVERT_TTL = 10000;

    public static final String TEST_FILES_PATH = "test" + File.separator + "acceptance" + File.separator;

    @Before
    public static void setUp() throws Exception {
        System.setProperty("OGROOT", ".");
        Configuration.getInstance(PeerConfiguration.PEER);
    }

    @After
    public static void tearDown() throws Exception {
        if (application != null && !application.getContainerDAO().isStopped()) {
            application.stop();
        }
        TestObjectsRegistry.reset();
        cleanBD();
    }

    public static void cleanBD() {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            String database = "jdbc:derby:db/peer";
            Connection con = DriverManager.getConnection(database, "", "");
            Statement s = con.createStatement();
            s.execute("DELETE FROM ATTRIBUTE");
            s.execute("DELETE FROM BALANCE_VALUE");
            s.execute("DELETE FROM BALANCE");
            s.execute("DELETE FROM COMMAND");
            s.execute("DELETE FROM EXECUTION");
            s.execute("DELETE FROM WORKER_STATUS_CHANGE");
            s.execute("DELETE FROM PEER_STATUS_CHANGE");
            s.execute("DELETE FROM TASK");
            s.execute("DELETE FROM JOB");
            s.execute("DELETE FROM LOGIN");
            s.execute("DELETE FROM T_USERS");
            s.execute("DELETE FROM WORKER");
            s.execute("DELETE FROM PEER");
            s.close();
            con.close();
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public void deleteNOFRankingFile() {
        deleteFile(context.getProperty(PeerConfiguration.PROP_RANKINGFILE));
    }

    public void deleteFile(String filepath) {
        File localUsersFile = new File(filepath);
        localUsersFile.delete();
    }

    public PeerComponent createPeerComponent(ContainerContext context) throws CommuneNetworkException, ProcessorStartException {
        application = new PeerComponent(context);
        return (PeerComponent) application;
    }

    public static ServiceManager getServiceManager() {
        ObjectDeployment deployment = application.getObject(Application.CONTROL_OBJECT_NAME);
        return deployment.getServiceManager();
    }

    public PeerControl getPeerControl() {
        ObjectDeployment deployment = getPeerControlDeployment();
        return (PeerControl) deployment.getObject();
    }

    public ObjectDeployment getPeerControlDeployment() {
        return getContainerObject(application, Application.CONTROL_OBJECT_NAME);
    }

    public WorkerNotificationController getWorkerMonitor() {
        ObjectDeployment deployment = getWorkerMonitorDeployment();
        return (WorkerNotificationController) deployment.getObject();
    }

    public ObjectDeployment getWorkerMonitorDeployment() {
        return getContainerObject(application, WORKER_MONITOR_OBJECT_NAME);
    }

    public DiscoveryServiceNotificationController getDiscoveryServiceMonitor() {
        ObjectDeployment deployment = getDiscoveryServiceMonitorDeployment();
        return (DiscoveryServiceNotificationController) deployment.getObject();
    }

    public ObjectDeployment getDiscoveryServiceMonitorDeployment() {
        return getContainerObject(application, DS_MONITOR_OBJECT_NAME);
    }

    public PeerStatusProvider getStatusProvider() {
        ObjectDeployment deployment = getContainerObject(application, Application.CONTROL_OBJECT_NAME);
        return (PeerStatusProvider) deployment.getObject();
    }

    public PeerStatusProvider getStatusProviderProxy() {
        ObjectDeployment deployment = getStatusProviderObjectDeployment();
        return (PeerStatusProvider) deployment.getObject();
    }

    public ObjectDeployment getStatusProviderObjectDeployment() {
        return getTestProxy(application, Application.CONTROL_OBJECT_NAME);
    }

    public PeerAccounting getAccountingAggregatorProxy() {
        ObjectDeployment deployment = getAccountingAggregatorDeployment();
        return (PeerAccounting) deployment.getObject();
    }

    public PeerAccounting getAccountingAggregator() {
        ObjectDeployment deployment = getContainerObject(application, ACCOUNTING_OBJECT_NAME);
        return (PeerAccounting) deployment.getObject();
    }

    public ObjectDeployment getAccountingAggregatorDeployment() {
        return getTestProxy(application, ACCOUNTING_OBJECT_NAME);
    }

    public ObjectDeployment getRemoteWorkerMonitorDeployment() {
        return getContainerObject(application, REMOTE_WORKER_MONITOR_OBJECT_NAME);
    }

    public RemoteWorkerFailureController getRemoteWorkerMonitor() {
        ObjectDeployment deployment = getRemoteWorkerMonitorDeployment();
        return (RemoteWorkerFailureController) deployment.getObject();
    }

    public WorkerSpecListener getWorkerSpecListener() {
        ObjectDeployment deployment = getWorkerSpecListenerDeployment();
        return (WorkerSpecListener) deployment.getObject();
    }

    public ObjectDeployment getWorkerSpecListenerDeployment() {
        return getContainerObject(application, PeerConstants.WORKER_SPEC_LISTENER_OBJECT_NAME);
    }

    public WorkerManagementClient getWorkerManagementClient() {
        ObjectDeployment deployment = getContainerObject(application, WORKER_MANAGEMENT_CLIENT_OBJECT_NAME);
        return (WorkerManagementClient) deployment.getObject();
    }

    public WorkerManagementClient getWorkerManagementClientProxy() {
        ObjectDeployment deployment = getWorkerManagementClientDeployment();
        return (WorkerManagementClient) deployment.getObject();
    }

    public ObjectDeployment getWorkerManagementClientDeployment() {
        return getTestProxy(application, WORKER_MANAGEMENT_CLIENT_OBJECT_NAME);
    }

    public RemoteWorkerManagementClient getRemoteWorkerManagementClient() {
        ObjectDeployment deployment = getContainerObject(application, REMOTE_WORKER_MANAGEMENT_CLIENT);
        return (RemoteWorkerManagementClient) deployment.getObject();
    }

    public RemoteWorkerManagementClient getRemoteWorkerManagementClientProxy() {
        return (RemoteWorkerManagementClient) getRemoteWorkerManagementClientDeployment().getObject();
    }

    public ObjectDeployment getRemoteWorkerManagementClientDeployment() {
        return getTestProxy(application, REMOTE_WORKER_MANAGEMENT_CLIENT);
    }

    public LocalWorkerProvider getLocalWorkerProviderProxy() {
        ObjectDeployment deployment = getLocalWorkerProviderDeployment();
        return (LocalWorkerProvider) deployment.getObject();
    }

    public LocalWorkerProvider getLocalWorkerProvider() {
        ObjectDeployment deployment = getContainerObject(application, LOCAL_ACCESS_OBJECT_NAME);
        return (LocalWorkerProvider) deployment.getObject();
    }

    public ObjectDeployment getLocalWorkerProviderDeployment() {
        return getTestProxy(application, LOCAL_ACCESS_OBJECT_NAME);
    }

    public RemoteWorkerProvider getRemoteWorkerProviderProxy() {
        ObjectDeployment deployment = getRemoteWorkerProviderDeployment();
        return (RemoteWorkerProvider) deployment.getObject();
    }

    public RemoteWorkerProvider getRemoteWorkerProvider() {
        ObjectDeployment deployment = getContainerObject(application, REMOTE_ACCESS_OBJECT_NAME);
        return (RemoteWorkerProvider) deployment.getObject();
    }

    public ObjectDeployment getRemoteWorkerProviderDeployment() {
        return getTestProxy(application, REMOTE_ACCESS_OBJECT_NAME);
    }

    public RemoteWorkerProviderClient getRemoteWorkerProviderClient() {
        ObjectDeployment deployment = getContainerObject(application, PeerConstants.REMOTE_WORKER_PROVIDER_CLIENT);
        return (RemoteWorkerProviderClient) deployment.getObject();
    }

    public RemoteWorkerProviderClient getRemoteWorkerProviderClientProxy() {
        ObjectDeployment deployment = getRemoteWorkerProviderClientDeployment();
        return (RemoteWorkerProviderClient) deployment.getObject();
    }

    public ObjectDeployment getRemoteWorkerProviderClientDeployment() {
        return getTestProxy(application, PeerConstants.REMOTE_WORKER_PROVIDER_CLIENT);
    }

    public DiscoveryServiceClient getDiscoveryServiceClientProxy() {
        ObjectDeployment deployment = getTestProxy(application, PeerConstants.DS_CLIENT);
        return (DiscoveryServiceClient) deployment.getObject();
    }

    public DiscoveryServiceClientController getDiscoveryServiceClient() {
        ObjectDeployment deployment = getContainerObject(application, PeerConstants.DS_CLIENT);
        return (DiscoveryServiceClientController) deployment.getObject();
    }

    public ObjectDeployment getDiscoveryServiceClientDeployment() {
        return getContainerObject(application, PeerConstants.DS_CLIENT);
    }

    public WorkerProviderClientFailureController getClientMonitor() {
        ObjectDeployment deployment = getContainerObject(application, CLIENT_MONITOR_OBJECT_NAME);
        return (WorkerProviderClientFailureController) deployment.getObject();
    }

    public RemoteWorkerProviderClientFailureController getRemoteClientMonitor() {
        ObjectDeployment deployment = getRemoteWorkerProviderMonitorDeployment();
        return (RemoteWorkerProviderClientFailureController) deployment.getObject();
    }

    public ObjectDeployment getRemoteWorkerProviderMonitorDeployment() {
        return getContainerObject(application, REMOTE_WORKER_PROVIDER_CLIENT_MONITOR);
    }

    public boolean isPeerInterestedOnBroker(ServiceID workerProviderClientID) {
        ObjectDeployment deployment = getContainerObject(application, CLIENT_MONITOR_OBJECT_NAME);
        return AcceptanceTestUtil.isInterested(application, workerProviderClientID, deployment.getDeploymentID());
    }

    public boolean isPeerInterestedOnLocalWorker(ServiceID workerManagementID) {
        ObjectDeployment deployment = getWorkerMonitorDeployment();
        return AcceptanceTestUtil.isInterested(application, workerManagementID, deployment.getDeploymentID());
    }

    public boolean isPeerInterestedOnRemoteClient(ServiceID remoteWorkerProviderClient) {
        ObjectDeployment deployment = getTestProxy(application, REMOTE_WORKER_PROVIDER_CLIENT_MONITOR);
        return AcceptanceTestUtil.isInterested(application, remoteWorkerProviderClient, deployment.getDeploymentID());
    }

    public boolean isPeerInterestedOnRemoteWorker(ServiceID rwmOID) {
        ObjectDeployment deployment = getContainerObject(application, PeerConstants.REMOTE_WORKER_MONITOR_OBJECT_NAME);
        return AcceptanceTestUtil.isInterested(application, rwmOID, deployment.getDeploymentID());
    }

    public boolean isPeerInterestedOnRemoteWorkerProvider(ServiceID rwpOID) {
        ObjectDeployment deployment = getTestProxy(application, PeerConstants.REMOTE_WORKER_MONITOR_OBJECT_NAME);
        return AcceptanceTestUtil.isInterested(application, rwpOID, deployment.getDeploymentID());
    }

    public static void copyTrustFile(String fileName) throws IOException {
        File origFile = new File(TEST_FILES_PATH + File.separator + fileName);
        FileUtils.copyFile(origFile, new File(PeerConfiguration.TRUSTY_COMMUNITIES_FILENAME));
    }

    public RepetitionRunnable createRequestWorkersRunnable(PeerComponent peerComponent, Long requestID) {
        ObjectDeployment objectDeployment = getPeerControlDeployment();
        return new RepetitionRunnable(peerComponent.getContainer(), (ApplicationManager) objectDeployment.getObject(), PeerConstants.REQUEST_WORKERS_ACTION_NAME, requestID);
    }

    public static DeploymentID createRemoteConsumerID(String user, String server, String consumer1PublicKey) {
        ContainerID consumer1APID = new ContainerID(user, server, PeerConstants.MODULE_NAME, consumer1PublicKey);
        return new DeploymentID(consumer1APID, PeerConstants.REMOTE_WORKER_PROVIDER_CLIENT);
    }

    public static void reset(Object... mocks) {
        for (Object mock : mocks) {
            EasyMock.reset(mock);
        }
    }

    public static void replay(Object... mocks) {
        for (Object mock : mocks) {
            EasyMock.replay(mock);
        }
    }

    public static void verify(Object... mocks) {
        for (Object mock : mocks) {
            EasyMock.verify(mock);
        }
    }
}
