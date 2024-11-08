package com.continuent.tungsten.router.smoke;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import com.continuent.tungsten.router.config.ConfigurationException;

/**
 * This class defines a SmokeTest for the sql-router. The intention of this test
 * is to exercise the basic functionality of the SQL Router under load.
 * 
 * @author <a href="mailto:edward.archibald@continuent.com">Ed Archibald</a>
 * @version 1.0
 */
public class SmokeTest extends TestCase {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(SmokeTest.class);

    private static final String SMOKETEST_CONFIG = "smoke";

    private static final String READER_CONFIG = "cycle.reader";

    private static final String WRITER_CONFIG = "cycle.writer";

    private SmokeConfiguration smokeConfig = null;

    private CycleRunnerConfiguration readerConfig = null;

    private CycleRunnerConfiguration writerConfig = null;

    private Vector<CycleRunner> readers = null;

    private Vector<CycleRunner> writers = null;

    private CountDownLatch startLatch = new CountDownLatch(1);

    private CountDownLatch doneLatch = null;

    private int tableCount = 10;

    public SmokeTest() throws Exception {
        logger.info("Loading the router:" + SmokeConfiguration.SQL_ROUTER_CLASS);
        loadRouter();
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            loadTestConfig();
            loadWriterConfig();
            loadReaderConfig();
        } catch (Exception c) {
            throw new Exception("Failed to load the test configuration:" + c);
        }
    }

    private void loadTestConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the test configuration: " + getConfigName(SMOKETEST_CONFIG)));
            smokeConfig = new SmokeConfiguration(getConfigName(SMOKETEST_CONFIG));
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(SMOKETEST_CONFIG)));
            smokeConfig = new SmokeConfiguration(getDefaultConfigName(SMOKETEST_CONFIG));
        }
    }

    private void loadReaderConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the reader configuration: " + getConfigName(READER_CONFIG)));
            readerConfig = new CycleRunnerConfiguration(getConfigName(READER_CONFIG));
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(READER_CONFIG)));
            readerConfig = new CycleRunnerConfiguration(getDefaultConfigName(READER_CONFIG));
        }
    }

    private void loadWriterConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the writer configuration: " + getConfigName(WRITER_CONFIG)));
            writerConfig = new CycleRunnerConfiguration(getConfigName(WRITER_CONFIG));
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(WRITER_CONFIG)));
            writerConfig = new CycleRunnerConfiguration(getDefaultConfigName(WRITER_CONFIG));
        }
    }

    public void loadRouter() throws Exception {
        logger.info(formatMessage("Loading the SQL Router driver:" + SmokeConfiguration.SQL_ROUTER_CLASS));
        Class.forName(SmokeConfiguration.SQL_ROUTER_CLASS);
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This test fires up a set of reader/writer thread pairs and just runs to
     * completion without doing anything at all with the router state.
     * 
     * @throws Exception
     */
    public void testBasic() throws Exception {
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        BasicRouterHandler routerCtrl = new BasicRouterHandler(smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING ROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR ROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("ROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        assertTrue(formatMessage("TEST FAILED DURING ROUTER SETUP=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        logger.info(formatMessage("WAITING FOR ALL THREADS TO EXIT"));
        doneLatch.await();
        logger.info(formatMessage("ALL THREADS COMPLETED. EXITING"));
        Summary summary = runnerSummary();
        if (!summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SOME THREADS FAILED: " + summary, summary.isSuccess());
    }

    /**
     * Starts up a load and cycles through router on and offline as fast as
     * possible.
     * 
     * @throws Exception
     */
    public void testFlipFlop() throws Exception {
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopHandler routerCtrl = new FlipFlopHandler(smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING ROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR ROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("ROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        if (routerResult.getStatus() != ExecutionStatus.SUCCEEDED) {
            cancelRunners();
        }
        assertTrue(formatMessage("TEST FAILED=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR RUNNERS TO FINISH"));
        doneLatch.await();
        logger.info(formatMessage("TEST RESULTS=" + routerResult));
        Summary summary = runnerSummary();
        if (!summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SOME THREADS FAILED: " + summary, summary.isSuccess());
    }

    /**
     * Starts up a load and cycles through router on and offline as fast as
     * possible.
     * 
     * @throws Exception
     */
    public void testFlipFlopDS() throws Exception {
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopDSHandler routerCtrl = new FlipFlopDSHandler(smokeConfig.getRunTime(), smokeConfig.getThinkTime(), routerStartLatch);
        logger.info(formatMessage("STARTING ROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        logger.info(formatMessage("WAITING FOR ROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("ROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        if (routerResult.getStatus() != ExecutionStatus.SUCCEEDED) {
            cancelRunners();
        }
        assertTrue(formatMessage("TEST FAILED=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR RUNNERS TO FINISH"));
        doneLatch.await();
        logger.info(formatMessage("TEST RESULTS=" + routerResult));
        Summary summary = runnerSummary();
        if (!summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SOME THREADS FAILED: " + summary, summary.isSuccess());
    }

    /**
     * This test exercises the code that manages the datastore active
     * connections in the router. In this case, a connection will be removed
     * from the active list because a thread will exit, with an error, because a
     * table it was accessing has been dropped.
     * 
     * @throws Exception
     */
    public void testDisconnectHandling() throws Exception {
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopDSHandler routerCtrl = new FlipFlopDSHandler(smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING ROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        Thread.sleep(10000);
        CycleRunnerUtils dropper = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.DROP, 0);
        (new Thread(dropper)).start();
        logger.info(formatMessage("WAITING FOR DROP TABLES COMPLETION"));
        ExecutionResult dropperResult = dropper.get();
        assertTrue("DROP TABLES FAILED:" + dropperResult, dropperResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR ROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("ROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        if (routerResult.getStatus() != ExecutionStatus.SUCCEEDED) {
            cancelRunners();
        }
        assertTrue(formatMessage("TEST FAILED=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR RUNNERS TO FINISH"));
        doneLatch.await();
        logger.info(formatMessage("TEST RESULTS=" + routerResult));
        Summary summary = runnerSummary();
        if (summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SUCCESS WAS UNEXPECTED: " + summary, !summary.isSuccess());
    }

    public void testCancelHandling() throws Exception {
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        BasicRouterHandler routerCtrl = new BasicRouterHandler(smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING ROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR ROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("ROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        assertTrue(formatMessage("TEST FAILED DURING ROUTER SETUP=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        Thread.sleep(20000);
        CycleRunnerUtils dropper = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.DROP, 1);
        (new Thread(dropper)).start();
        logger.info(formatMessage("WAITING FOR DROP TABLES COMPLETION"));
        ExecutionResult dropperResult = dropper.get();
        assertTrue("DROP TABLES FAILED:" + dropperResult, dropperResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR ALL THREADS TO EXIT"));
        doneLatch.await();
        logger.info(formatMessage("ALL THREADS COMPLETED. EXITING"));
        Summary summary = runnerSummary();
        if (!summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SOME THREADS SUCCEEDED: " + summary, !summary.isSuccess());
    }

    /**
     * Cancel all runners.
     * 
     * @throws Exception
     */
    public void cancelRunners() throws Exception {
        if (readers != null) {
            for (CycleRunner reader : readers) {
                reader.setCancelled(true);
            }
        }
        if (writers != null) {
            for (CycleRunner writer : writers) {
                writer.setCancelled(true);
            }
        }
    }

    public void setupRunners() throws Exception {
        logger.info(formatMessage("SETTING UP TABLES"));
        CycleRunnerUtils setup = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.CREATE, 0);
        (new Thread(setup)).start();
        logger.info(formatMessage("WAITING FOR SETUP COMPLETION"));
        ExecutionResult setupResult = setup.get();
        assertTrue("SETUP FAILED:" + setupResult, setupResult.getStatus() == ExecutionStatus.SUCCEEDED);
        doneLatch = new CountDownLatch(smokeConfig.getThreads() * 2);
        readers = new Vector<CycleRunner>();
        writers = new Vector<CycleRunner>();
        for (int i = 0; i < smokeConfig.getThreads(); i++) {
            CycleRunner writer = new CycleRunner(i, smokeConfig.getRowCount(), smokeConfig.getRunTime(), writerConfig, null, startLatch, doneLatch, setup);
            writers.add(writer);
            CycleRunner reader = new CycleRunner(i, smokeConfig.getRowCount(), smokeConfig.getRunTime(), readerConfig, writer, startLatch, doneLatch, setup);
            readers.add(reader);
            writer.setCompanion(reader);
            (new Thread(writer)).start();
            (new Thread(reader)).start();
        }
    }

    private class Summary {

        private int failed;

        private int cancelled;

        private int success;

        private int undefined;

        private Vector<ExecutionResult> issues = new Vector<ExecutionResult>();

        public boolean isSuccess() {
            if (failed > 0 || cancelled > 0 || undefined > 0) {
                return false;
            }
            if (success == 0) {
                return false;
            }
            return true;
        }

        public void addIssue(ExecutionResult issue) {
            issues.add(issue);
        }

        /**
         * Returns the failed value.
         * 
         * @return Returns the failed.
         */
        public int getFailed() {
            return failed;
        }

        /**
         * Sets the failed value.
         * 
         * @param failed The failed to set.
         */
        public void setFailed(int failed) {
            this.failed = failed;
        }

        /**
         * Returns the cancelled value.
         * 
         * @return Returns the cancelled.
         */
        public int getCancelled() {
            return cancelled;
        }

        /**
         * Sets the cancelled value.
         * 
         * @param cancelled The cancelled to set.
         */
        public void setCancelled(int cancelled) {
            this.cancelled = cancelled;
        }

        /**
         * Returns the success value.
         * 
         * @return Returns the success.
         */
        public int getSuccess() {
            return success;
        }

        /**
         * Sets the success value.
         * 
         * @param success The success to set.
         */
        public void setSuccess(int success) {
            this.success = success;
        }

        /**
         * Returns the undefined value.
         * 
         * @return Returns the undefined.
         */
        public int getUndefined() {
            return undefined;
        }

        /**
         * Sets the undefined value.
         * 
         * @param undefined The undefined to set.
         */
        public void setUndefined(int undefined) {
            this.undefined = undefined;
        }

        public String toString() {
            return "SUMMARY: success=" + success + ", failed=" + failed + ", cancelled=" + cancelled + ", undefined=" + undefined;
        }

        public void log() {
            for (ExecutionResult issue : getIssues()) {
                logger.info(issue.getMessage());
            }
        }

        /**
         * Returns the issues value.
         * 
         * @return Returns the issues.
         */
        public Vector<ExecutionResult> getIssues() {
            return issues;
        }

        /**
         * Sets the issues value.
         * 
         * @param issues The issues to set.
         */
        public void setIssues(Vector<ExecutionResult> issues) {
            this.issues = issues;
        }
    }

    private Summary runnerSummary() {
        Summary summary = new Summary();
        for (Future<ExecutionResult> future : writers) {
            updateSummary(future, summary);
        }
        for (Future<ExecutionResult> future : readers) {
            updateSummary(future, summary);
        }
        return summary;
    }

    private void updateSummary(Future<ExecutionResult> future, Summary summary) {
        ExecutionResult result = null;
        if (future.isDone()) {
            try {
                result = future.get();
            } catch (Exception e) {
                summary.setUndefined(summary.getUndefined() + 1);
                return;
            }
        }
        if (result != null) {
            if (result.getStatus() == ExecutionStatus.CANCELLED) {
                summary.setCancelled(summary.getCancelled() + 1);
                summary.addIssue(result);
            } else if (result.getStatus() == ExecutionStatus.SUCCEEDED) {
                summary.setSuccess(summary.getSuccess() + 1);
            } else if (result.getStatus() == ExecutionStatus.FAILED) {
                summary.setFailed(summary.getFailed() + 1);
                summary.addIssue(result);
            }
        } else {
            summary.setUndefined(summary.getUndefined() + 1);
            summary.addIssue(result);
        }
    }

    /**
     * return a config file name according to conventions.
     * 
     * @param testName
     * @param configType
     * @return
     */
    private String getConfigName(String configType) {
        return getName() + "." + configType + ".properties";
    }

    private String getDefaultConfigName(String configType) {
        return "default." + configType + ".properties";
    }

    public String formatMessage(String message) {
        String msgFormat = "[" + this.getClass().getSimpleName() + "]: %s";
        return String.format(msgFormat, message);
    }

    /**
     * Returns the tableCount value.
     * 
     * @return Returns the tableCount.
     */
    public int getTableCount() {
        return tableCount;
    }

    /**
     * Sets the tableCount value.
     * 
     * @param tableCount The tableCount to set.
     */
    public void setTableCount(int tableCount) {
        this.tableCount = tableCount;
    }
}