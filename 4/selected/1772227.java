package org.ourgrid.acceptance.peer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.ourgrid.acceptance.util.PeerAcceptanceUtil;
import org.ourgrid.common.spec.exception.JobSpecificationException;
import org.ourgrid.common.spec.exception.TaskSpecificationException;
import org.ourgrid.common.spec.job.IOBlock;
import org.ourgrid.common.spec.job.JobSpec;
import org.ourgrid.common.spec.job.TaskSpec;
import org.ourgrid.common.spec.worker.WorkerSpec;
import org.ourgrid.peer.PeerComponent;
import org.ourgrid.peer.PeerComponentContextFactory;
import org.ourgrid.peer.PeerConfiguration;
import org.ourgrid.worker.WorkerConstants;
import br.edu.ufcg.lsd.commune.Application;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.contextfactory.PropertiesFileParser;
import br.edu.ufcg.lsd.commune.functionaltests.util.TestContext;
import br.edu.ufcg.lsd.commune.identification.ContainerID;
import br.edu.ufcg.lsd.commune.identification.DeploymentID;
import br.edu.ufcg.lsd.commune.network.xmpp.XMPPProperties;
import br.edu.ufcg.lsd.commune.test.AcceptanceTestCase;

public class PeerAcceptanceTestCase extends AcceptanceTestCase {

    public static final String SEP = File.separator;

    public static final String PEER_TEST_DIR = "test" + SEP + "acceptance" + SEP + "peer";

    private static final String COMM_FILE_PATH = "test" + File.separator + "acceptance" + File.separator + "req_011";

    public static final String PEER_PROP_FILEPATH = PEER_TEST_DIR + SEP + "peer.properties";

    protected PeerAcceptanceUtil peerAcceptanceUtil = new PeerAcceptanceUtil(getComponentContext());

    @BeforeClass
    public static void recreateSchema() {
    }

    @Before
    public void setUp() throws Exception {
        PeerAcceptanceUtil.setUp();
        super.setUp();
    }

    public ObjectDeployment getPeerControlDeployment(PeerComponent component) {
        return component.getObject(Application.CONTROL_OBJECT_NAME);
    }

    @After
    public void tearDown() throws Exception {
        PeerAcceptanceUtil.tearDown();
    }

    /**
	 * Assumes that PeerConfiguration.PROP_DS_NETWORK contains a single address 
	 * @return DiscoveryService component user name 
	 */
    protected String getDSUserName() {
        return getComponentContext().getProperty(PeerConfiguration.PROP_DS_NETWORK).split("@")[0];
    }

    /**
	 * Assumes that PeerConfiguration.PROP_DS_NETWORK contains a single address 
	 * @return DiscoveryService component server name
	 */
    protected String getDSServerName() {
        return getComponentContext().getProperty(PeerConfiguration.PROP_DS_NETWORK).split("@")[1];
    }

    protected void copyTrustFile(String fileName) throws IOException {
        File origFile = new File(getRootForTrustFile() + File.separator + fileName);
        FileUtils.copyFile(origFile, new File(PeerConfiguration.TRUSTY_COMMUNITIES_FILENAME));
    }

    protected String getRootForTrustFile() {
        return COMM_FILE_PATH;
    }

    @Override
    protected TestContext createComponentContext() {
        return new TestContext(new PeerComponentContextFactory(new PropertiesFileParser(PEER_PROP_FILEPATH)).createContext());
    }

    public DeploymentID createWorkerDeploymentID(WorkerSpec workerSpec, String publicKey) {
        String user = workerSpec.getAttribute(WorkerSpec.ATT_USERNAME);
        String server = workerSpec.getAttribute(WorkerSpec.ATT_SERVERNAME);
        DeploymentID workerDeploymentID = new DeploymentID(new ContainerID(user, server, WorkerConstants.MODULE_NAME, publicKey), WorkerConstants.WORKER);
        return workerDeploymentID;
    }

    protected JobSpec createJobSpec(String label) throws TaskSpecificationException, JobSpecificationException {
        JobSpec jobSpec = new JobSpec(label);
        List<TaskSpec> taskList = new ArrayList<TaskSpec>();
        taskList.add(new TaskSpec(new IOBlock(), "echo test", new IOBlock(), null));
        jobSpec.setTaskSpecs(taskList);
        return jobSpec;
    }

    protected JobSpec createJobSpec(String label, String requirements) throws TaskSpecificationException, JobSpecificationException {
        JobSpec createdJobSpec = createJobSpec(label);
        createdJobSpec.setRequirements(requirements);
        return createdJobSpec;
    }

    protected String getPeerAddress() {
        String user = getComponentContext().getProperty(XMPPProperties.PROP_USERNAME);
        String server = getComponentContext().getProperty(XMPPProperties.PROP_XMPP_SERVERNAME);
        return user + "@" + server;
    }
}
