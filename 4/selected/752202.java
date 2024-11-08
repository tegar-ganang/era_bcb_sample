package org.reddwarfserver.maven.plugin.sgs;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the {@code BootMojo} class
 */
@RunWith(JUnit4.class)
public class TestBootAndStopMojo extends AbstractTestSgsMojo {

    private static final String POM = "target/test-classes/unit/boot/config.xml";

    private File outputDirectory;

    private AbstractSgsMojo mojo;

    private File sgsHome;

    protected AbstractSgsMojo buildEmptyMojo() {
        return new BootMojo();
    }

    @Before
    public void buildDummyHomeMojo() throws Exception {
        mojo = this.lookupDummyMojo(BootMojo.class, "boot", POM);
        sgsHome = (File) this.getVariableValueFromObject(mojo, "sgsHome");
        outputDirectory = new File(getBasedir(), "target" + File.separator + "test-classes" + File.separator + "unit" + File.separator + "boot");
    }

    @After
    public void scrubHome() throws Exception {
        if (sgsHome.exists()) {
            FileUtils.deleteDirectory(sgsHome);
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testExecuteNoInstallation() throws Exception {
        mojo.execute();
    }

    @Test
    public void testExecuteNoDeployment() throws Exception {
        this.executeInstall(POM, outputDirectory);
        TestLog dummyLog = new TestLog();
        mojo.setLog(dummyLog);
        mojo.execute();
        Assert.assertTrue(dummyLog.seen(".*Missing required property.*"));
    }

    @Test(timeout = 90000)
    public void testExecuteValidDeployment() throws Exception {
        this.executeInstall(POM, outputDirectory);
        TestLog dummyLog = new TestLog();
        mojo.setLog(dummyLog);
        File tutorial = new File(sgsHome, "tutorial" + File.separator + "tutorial.jar");
        File dest = new File(sgsHome, "deploy" + File.separator + "tutorial.jar");
        FileUtils.copyFile(tutorial, dest);
        new Thread(new Stopper()).start();
        mojo.execute();
        Assert.assertTrue(dummyLog.seen(".*Kernel is ready.*"));
        Assert.assertTrue(dummyLog.seen(".*Controller issued node shutdown.*"));
    }

    @Test(timeout = 90000)
    public void testExecuteValidDeploymentAlternateBoot() throws Exception {
        this.executeInstall(POM, outputDirectory);
        TestLog dummyLog = new TestLog();
        mojo.setLog(dummyLog);
        File alternateBoot = new File(sgsHome, "tutorial" + File.separator + "conf" + File.separator + "HelloWorld.boot");
        this.setVariableValueToObject(mojo, "alternateBoot", alternateBoot);
        new Thread(new Stopper()).start();
        mojo.execute();
        Assert.assertTrue(dummyLog.seen(".*Kernel is ready.*"));
        Assert.assertTrue(dummyLog.seen(".*Controller issued node shutdown.*"));
    }

    /**
     * Wait a few seconds and then initiate an sgs-stop command
     */
    private class Stopper implements Runnable {

        public void run() {
            try {
                Thread.sleep(3000);
                StopMojo stop = TestBootAndStopMojo.this.lookupDummyMojo(StopMojo.class, "stop", POM);
                System.err.println("Initiating sgs-stop");
                stop.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is used to verify that the boot mojo boots the server
     * correctly by intercepting the logging output.
     */
    private static class TestLog extends SystemStreamLog {

        public List<String> history = new ArrayList<String>();

        @Override
        public void info(CharSequence content) {
            history.add(content.toString());
        }

        public boolean seen(String exp) {
            for (String line : history) {
                if (line.matches(exp)) {
                    return true;
                }
            }
            return false;
        }
    }
}
