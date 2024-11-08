package com.controltier.ctl.execution.script;

import com.controltier.ctl.NodesetEmptyException;
import com.controltier.ctl.cli.NodeCallableFactory;
import com.controltier.ctl.cli.NodeDispatcher;
import com.controltier.ctl.common.Framework;
import com.controltier.ctl.common.FrameworkProject;
import com.controltier.ctl.common.INodeEntry;
import com.controltier.ctl.common.NodeEntryImpl;
import com.controltier.ctl.dispatcher.DispatchedScriptImpl;
import com.controltier.ctl.dispatcher.IDispatchedScript;
import com.controltier.ctl.execution.FailedNodesListener;
import com.controltier.ctl.tasks.controller.node.NodeSet;
import com.controltier.ctl.tools.CtlTest;
import com.controltier.ctl.utils.FileUtils;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class TestCommandAction extends CtlTest {

    CommandAction commandAction;

    FrameworkProject project;

    private static final String TEST_PROJ = "TestCommandAction";

    private static final String TEST_NODES_XML = "src/test/com/controltier/ctl/tasks/controller/node/TestNodeDispatchAction.nodes.xml";

    public TestCommandAction(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCommandAction.class);
    }

    protected void setUp() {
        super.setUp();
        project = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(TEST_PROJ);
    }

    protected void tearDown() throws Exception {
        FrameworkProject d = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(TEST_PROJ);
        FileUtils.deleteDir(d.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove(TEST_PROJ);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCreateCommandProxy() throws Exception {
        Framework fw = getFrameworkInstance();
        NodeSet nodeset = new NodeSet();
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);
        }
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id", "&&", "hostname" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            nodeentry.setOsFamily("unix");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);
        }
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);
        }
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id", "potato", "hell" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);
        }
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "echo", "test belief" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            assertTrue("command action should return true", action.isCommandAction());
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            nodeentry.setOsFamily("windows");
            Task t = action.createCommandProxy(nodeentry);
            assertNotNull("shouldn't be null", t);
        }
    }

    public void testDoAction() throws Exception {
        Framework fw = getFrameworkInstance();
        NodeSet nodeset = new NodeSet();
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            final boolean wascalled[] = { false };
            action.setNodeDispatcher(new NodeDispatcher() {

                public void executeNodedispatch(Project project, Collection<INodeEntry> nodes, int threadcount, boolean keepgoing, FailedNodesListener failedListener, NodeCallableFactory factory) {
                    wascalled[0] = true;
                }
            });
            try {
                action.doAction();
                fail("should not succeed.");
            } catch (Exception e) {
                assertTrue(e instanceof NodesetEmptyException);
            }
            assertFalse(wascalled[0]);
        }
        File destNodesFile = new File(project.getEtcDir(), "resources.xml");
        File testNodesFile = new File(TEST_NODES_XML);
        try {
            FileUtils.copyFileStreams(testNodesFile, destNodesFile);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        {
            IDispatchedScript dsc = new DispatchedScriptImpl(nodeset, TEST_PROJ, null, null, null, new String[] { "id" }, 0);
            CommandAction action = new CommandAction(fw, dsc, null);
            final NodeEntryImpl nodeentry = new NodeEntryImpl(CtlTest.localNodeHostname, CtlTest.localNodeHostname);
            final boolean wascalled[] = { false };
            action.setNodeDispatcher(new NodeDispatcher() {

                public void executeNodedispatch(Project project, Collection<INodeEntry> nodes, int threadcount, boolean keepgoing, FailedNodesListener failedListener, NodeCallableFactory factory) {
                    wascalled[0] = true;
                }
            });
            action.doAction();
            assertTrue(wascalled[0]);
        }
    }
}
