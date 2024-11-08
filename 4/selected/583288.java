package com.controltier.ctl.execution.defined;

import com.controltier.ctl.cli.CtlGenMain;
import com.controltier.ctl.cli.CtlProjectMain;
import com.controltier.ctl.common.CmdModule;
import com.controltier.ctl.common.FrameworkProject;
import com.controltier.ctl.common.FrameworkType;
import com.controltier.ctl.common.IModuleLookup;
import com.controltier.ctl.common.context.*;
import com.controltier.ctl.execution.*;
import com.controltier.ctl.tasks.controller.node.TestNodeDispatchAction;
import com.controltier.ctl.tools.CtlTest;
import com.controltier.ctl.utils.FileUtils;
import org.apache.tools.ant.BuildException;
import java.io.*;
import java.util.Properties;

/**
 * TestDefinedCommandInput tests the commandline input defined command execution.
 *
 * @author Greg Schueler <a href="mailto:greg@controltier.com">greg@controltier.com</a>
 * @version $Revision$
 */
public class TestDefinedCommandInput extends CtlTest {

    static final String PROJECT = "TestDefinedCommandInput";

    static final String TEST_USER_INPUT_COMMAND = "TestUserInput";

    static final String TEST_HANDLERS_PATH = "src/test/com/controltier/ctl/execution/defined/handlers";

    FrameworkProject project;

    private static final String MODULE_NAME = "TestModule";

    private CmdModule module;

    private static final String TEST_MODULE_LIB_DIR = "src/test/modules/Module/lib";

    InputStream originalSystemIn;

    public TestDefinedCommandInput(String name) {
        super(name);
    }

    protected void setUp() {
        super.setUp();
        project = createFrameworkProject();
        final IModuleLookup moduleLookup = getFrameworkInstance().getModuleLookup();
        final File baseDir = new File(moduleLookup.getBaseDir(), MODULE_NAME);
        baseDir.mkdirs();
        final File commandsDir = new File(baseDir, "commands");
        commandsDir.mkdirs();
        final File libDir = new File(baseDir, "lib");
        libDir.mkdirs();
        Properties moduleProperties = new Properties();
        moduleProperties.put("module.name", MODULE_NAME);
        Properties commandProperties = new Properties();
        final String prefix = "command." + TEST_USER_INPUT_COMMAND;
        commandProperties.put(prefix + ".command-type", "ant");
        commandProperties.put(prefix + ".controller", "TestModule");
        commandProperties.put(prefix + ".doc", "Test user input");
        final File fromFile = new File(TEST_HANDLERS_PATH + "/" + TEST_USER_INPUT_COMMAND + ".xml");
        final File toFile = new File(commandsDir, TEST_USER_INPUT_COMMAND + ".xml");
        final File libxml = new File(TEST_MODULE_LIB_DIR + "/command.xml");
        assertTrue(libxml.exists());
        final File destlibxml = new File(libDir, "command.xml");
        try {
            moduleProperties.store(new FileOutputStream(new File(baseDir, "module.properties")), "Module properties");
            commandProperties.store(new FileOutputStream(new File(baseDir, "commands.properties")), "Command properties");
            FileUtils.copyFileStreams(fromFile, toFile);
            FileUtils.copyFileStreams(libxml, destlibxml);
        } catch (IOException e) {
            fail("caught exception creating module.properties test data: " + e.getMessage());
        }
        assertTrue(toFile.exists());
        assertTrue(destlibxml.exists());
        module = new CmdModule(MODULE_NAME, baseDir, moduleLookup);
    }

    public FrameworkProject createFrameworkProject() {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT);
        FrameworkProject project = null;
        try {
            FrameworkProject.createFileStructure(projectDir, true);
            final CtlProjectMain setup = new CtlProjectMain();
            final String[] args = new String[] { "-p", PROJECT, "-b", "src/ant/controllers/ctl/projectsetupCmd.xml", "-o", "-v" };
            setup.parseArgs(args);
            setup.executeAction();
            final File templateBasedir = new File("src/templates/ant");
            final File modulesDir = new File(projectDir, "modules");
            assertTrue("project modules lib dir does not exist", modulesDir.exists());
            final CtlGenMain creator = CtlGenMain.create(templateBasedir, modulesDir);
            project = getFrameworkInstance().getFrameworkProjectMgr().createFrameworkProject(PROJECT);
            final FrameworkType mType = project.createType(MODULE_NAME);
            File destNodesFile = new File(project.getEtcDir(), "resources.xml");
            File testNodesFile = new File(TestNodeDispatchAction.TEST_NODES_XML);
            try {
                FileUtils.copyFileStreams(testNodesFile, destNodesFile);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        } catch (Throwable t) {
            fail(t.getMessage());
        }
        return project;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDir(project.getBaseDir());
        getFrameworkInstance().getFrameworkProjectMgr().remove(PROJECT);
    }

    public void testInput() throws BuildException {
        final ExecutionService service = ExecutionServiceFactory.instance().createExecutionService(getFrameworkInstance());
        final FrameworkProjectContext projectContext = FrameworkProjectContext.create(PROJECT);
        final UserContext userContext = UserContext.create("TEST");
        final CommandContext cmdcontext = CommandContext.create(TEST_USER_INPUT_COMMAND, MODULE_NAME, projectContext);
        final ObjectContext objectcontext = ObjectContext.create(null, MODULE_NAME, projectContext);
        final ExecutionContext executionContext = ExecutionContext.create(objectcontext, cmdcontext, userContext);
        final DispatchedCommandExecutionItem executionItem = ExecutionServiceFactory.createDispatchedCommandExecutionItem(executionContext, null);
        ExecutionResult result = null;
        final InputStream originalSystemIn = System.in;
        final PrintStream originalSystemErr = System.err;
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream("ABCDEF\n".getBytes());
            System.setIn(stream);
            stream.close();
            result = service.executeItem(executionItem);
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail("exception: " + e.getMessage());
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        } finally {
            if (null != originalSystemIn) {
                System.setIn(originalSystemIn);
            }
        }
        assertNotNull(result);
        if (null != result.getException()) {
            result.getException().printStackTrace(originalSystemErr);
        }
        assertTrue("result failed: " + result.getResultObject() + ", exception: " + result.getException(), result.isSuccess());
    }
}
