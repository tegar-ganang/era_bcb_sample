package com.continuent.tungsten.router.smoke;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import com.continuent.tungsten.commons.config.cluster.ConfigurationException;
import com.continuent.tungsten.commons.utils.ExecutionResult;
import com.continuent.tungsten.commons.utils.ExecutionStatus;
import com.continuent.tungsten.router.AbstractRouterTestCase;
import com.continuent.tungsten.router.client.RouterMgrHelper;
import com.continuent.tungsten.router.jdbc.TSRDriver;
import com.continuent.tungsten.router.utils.SetupHelper;

/**
 * This class defines a SmokeTest for the sql-router. The intention of this test
 * is to exercise the basic functionality of the SQL SQLRouter under load.
 * 
 * @author <a href="mailto:edward.archibald@continuent.com">Ed Archibald</a>
 * @version 1.0
 */
public class DisabledTstRouterForSmoke extends AbstractRouterTestCase {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(DisabledTstRouterForSmoke.class);

    private SetupHelper setupHelper;

    private RouterMgrHelper routerHelper;

    private static final String SMOKETEST_CONFIG = "smoke";

    private static final String READER_CONFIG = "cycle.reader";

    private static final String WRITER_CONFIG = "cycle.writer";

    private static final String SERVICE_NAME = "smoke-service";

    private SmokeConfiguration smokeConfig = null;

    private CycleRunnerConfiguration readerConfig = null;

    private CycleRunnerConfiguration writerConfig = null;

    private Vector<CycleRunner> readers = null;

    private Vector<CycleRunner> writers = null;

    private CountDownLatch startLatch = new CountDownLatch(1);

    private CountDownLatch doneLatch = null;

    private int tableCount = 10;

    private boolean runOne = false;

    public DisabledTstRouterForSmoke() throws Exception {
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Before
    public void setUp() throws Exception {
        if (setupHelper == null) setupHelper = new SetupHelper();
        setupHelper.createDefaultConfiguration(SERVICE_NAME);
        setupHelper.createServiceDataSources(setupHelper.getReadwriteUrl(), setupHelper.getReadwriteUrl());
        Class.forName(TSRDriver.class.getName()).newInstance();
        if (routerHelper == null) routerHelper = new RouterMgrHelper();
    }

    @After
    public void tearDown() throws Exception {
    }

    public void loadRouter() throws Exception {
        logger.info(formatMessage("Loading the SQL SQLRouter driver:" + SmokeConfiguration.SQL_ROUTER_CLASS));
        Class.forName(SmokeConfiguration.SQL_ROUTER_CLASS);
    }

    /**
     * This test fires up a set of reader/writer thread pairs and just runs to
     * completion without doing anything at all with the router state.
     * 
     * @throws Exception
     */
    public void testBasic() throws Exception {
        if (runOne) return;
        smokeConfig = getDefaultSmokeConfiguration();
        readerConfig = getDefaultReaderConfiguration();
        writerConfig = getDefaultWriterConfiguration();
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        BasicRouterHandler routerCtrl = new BasicRouterHandler(setupHelper.getDataServiceName(), smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING SQLROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR SQLROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("SQLROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        assertTrue(formatMessage("TEST FAILED DURING SQLROUTER SETUP=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
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
     * possible. As long as the router is configured to have threads wait if it
     * is offline, there should be no exceptions for the entire session.
     * 
     * @throws Exception
     */
    public void testFlipFlop() throws Exception {
        if (runOne) return;
        smokeConfig = getDefaultSmokeConfiguration();
        smokeConfig.setThreads(50);
        readerConfig = getDefaultReaderConfiguration();
        writerConfig = getDefaultWriterConfiguration();
        routerHelper.setDynamicProperty("waitIfDisabled", "true");
        routerHelper.setDynamicProperty("waitIfUnavailable", "true");
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopHandler routerCtrl = new FlipFlopHandler(setupHelper.getDataServiceName(), smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING SQLROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR SQLROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("SQLROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
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
        if (runOne) return;
        smokeConfig = getDefaultSmokeConfiguration();
        smokeConfig.setThreads(50);
        smokeConfig.setThinkTime(5000);
        readerConfig = getDefaultReaderConfiguration();
        writerConfig = getDefaultWriterConfiguration();
        routerHelper.setDynamicProperty("waitIfDisabled", "true");
        routerHelper.setDynamicProperty("waitIfUnavailable", "true");
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopDSHandler routerCtrl = new FlipFlopDSHandler(setupHelper.getDataServiceName(), smokeConfig.getRunTime(), smokeConfig.getThinkTime(), routerStartLatch);
        logger.info(formatMessage("STARTING SQLROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        logger.info(formatMessage("WAITING FOR SQLROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("SQLROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
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
        if (runOne) return;
        smokeConfig = getDefaultSmokeConfiguration();
        smokeConfig.setThreads(50);
        smokeConfig.setRunTime(180);
        readerConfig = getDefaultReaderConfiguration();
        writerConfig = getDefaultWriterConfiguration();
        routerHelper.setDynamicProperty("waitIfDisabled", "true");
        routerHelper.setDynamicProperty("waitIfUnavailable", "true");
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        FlipFlopDSHandler routerCtrl = new FlipFlopDSHandler(setupHelper.getDataServiceName(), smokeConfig.getRunTime(), 1000, routerStartLatch);
        logger.info(formatMessage("STARTING SQLROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerCtrl.start();
        routerStartLatch.countDown();
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        Thread.sleep(10000);
        CycleRunnerUtils dropper = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.DROP, 5, false);
        (new Thread(dropper, dropper.getClass().getSimpleName())).start();
        logger.info(formatMessage("WAITING FOR DROP TABLES COMPLETION"));
        ExecutionResult dropperResult = dropper.get();
        if (dropperResult.getStatus() != ExecutionStatus.SUCCEEDED) {
            routerCtrl.cancel(true);
            cancelRunners();
        }
        assertTrue("DROP TABLES FAILED:" + dropperResult, dropperResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info(formatMessage("WAITING FOR RUNNERS TO FINISH"));
        doneLatch.await();
        Summary summary = runnerSummary();
        if (summary.isSuccess()) {
            summary.log();
        }
        assertTrue("SUCCESS WAS UNEXPECTED: " + summary, !summary.isSuccess());
        logger.info(formatMessage("CANCELLING THE SQLROUTER CONTROL"));
        routerCtrl.cancel(true);
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("SQLROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        assertTrue(formatMessage("TEST FAILED=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED || routerResult.getStatus() == ExecutionStatus.CANCELLED);
    }

    public void testCancelHandling() throws Exception {
        if (runOne && false) return;
        smokeConfig = getDefaultSmokeConfiguration();
        smokeConfig.setThreads(50);
        smokeConfig.setRunTime(180);
        smokeConfig.setThinkTime(1000);
        readerConfig = getDefaultReaderConfiguration();
        writerConfig = getDefaultWriterConfiguration();
        routerHelper.setDynamicProperty("waitIfDisabled", "true");
        routerHelper.setDynamicProperty("waitIfUnavailable", "true");
        CountDownLatch routerStartLatch = new CountDownLatch(1);
        BasicRouterHandler routerCtrl = new BasicRouterHandler(setupHelper.getDataServiceName(), smokeConfig.getRunTime(), smokeConfig.getThinkTime(), routerStartLatch);
        logger.info(formatMessage("STARTING SQLROUTER CONTROLLER:" + routerCtrl.getClass().getSimpleName()));
        routerStartLatch.countDown();
        logger.info(formatMessage("WAITING FOR SQLROUTER CONTROLLER TO COMPLETE"));
        ExecutionResult routerResult = routerCtrl.get();
        logger.info(formatMessage("SQLROUTER CONTROLLER FINISHED, RESULT=" + routerResult));
        assertTrue(formatMessage("TEST FAILED DURING SQLROUTER SETUP=" + routerResult), routerResult.getStatus() == ExecutionStatus.SUCCEEDED);
        logger.info("SQLROUTER STATUS:\n" + routerHelper.status(setupHelper.getDataServiceName()));
        logger.info(formatMessage("SETTING UP RUNNERS"));
        setupRunners();
        logger.info(formatMessage("SIGNALING START"));
        startLatch.countDown();
        CycleRunnerUtils dropper = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.DROP, 5, false);
        (new Thread(dropper, dropper.getClass().getSimpleName())).start();
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
        CycleRunnerUtils setup = new CycleRunnerUtils(writerConfig, CycleRunnerUtils.CREATE, 0, true);
        (new Thread(setup, setup.getClass().getSimpleName())).start();
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
            (new Thread(writer, writer.getClass().getSimpleName() + "-Writer-" + i)).start();
            (new Thread(reader, reader.getClass().getSimpleName() + "-Reader-" + i)).start();
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
                logger.info(String.format("status=%s, message=%s, last exception=%s", issue.getStatus(), (issue.getMessage() == null ? "none" : issue.getMessage()), (issue.getLastException() == null ? "none" : issue.getLastException())));
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

    private CycleRunnerConfiguration getDefaultReaderConfiguration() {
        CycleRunnerConfiguration config = new CycleRunnerConfiguration();
        config.setDriver("com.continuent.tungsten.router.jdbc.TSRDriver");
        config.setUser(setupHelper.getUser());
        config.setPassword(setupHelper.getPassword());
        config.setUrl(setupHelper.getRouterURL("RO_RELAXED"));
        config.setReadOnly(true);
        config.setRetryCount(0);
        config.setRetryInterval(0);
        config.setVerbose(false);
        config.setThinkTime(100);
        return config;
    }

    private CycleRunnerConfiguration getDefaultWriterConfiguration() {
        CycleRunnerConfiguration config = new CycleRunnerConfiguration();
        config.setDriver("com.continuent.tungsten.router.jdbc.TSRDriver");
        config.setUser(setupHelper.getUser());
        config.setPassword(setupHelper.getPassword());
        config.setUrl(setupHelper.getRouterURL("RW_STRICT"));
        config.setReadOnly(false);
        config.setRetryCount(0);
        config.setRetryInterval(0);
        config.setVerbose(false);
        config.setThinkTime(100);
        return config;
    }

    private SmokeConfiguration getDefaultSmokeConfiguration() {
        SmokeConfiguration config = new SmokeConfiguration();
        config.setThreads(50);
        config.setRunTime(60);
        config.setRowCount(20);
        return config;
    }

    private void loadTestConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the test configuration: " + getConfigName(SMOKETEST_CONFIG)));
            smokeConfig = new SmokeConfiguration(setupHelper.getDataServiceName(), getConfigName(SMOKETEST_CONFIG));
            smokeConfig.load();
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(SMOKETEST_CONFIG)));
            smokeConfig = new SmokeConfiguration(setupHelper.getDataServiceName(), getDefaultConfigName(SMOKETEST_CONFIG));
        }
    }

    private void loadReaderConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the reader configuration: " + getConfigName(READER_CONFIG)));
            readerConfig = new CycleRunnerConfiguration(getConfigName(READER_CONFIG));
            readerConfig.load();
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(READER_CONFIG)));
            readerConfig = new CycleRunnerConfiguration(getDefaultConfigName(READER_CONFIG));
        }
    }

    private void loadWriterConfig() throws Exception {
        try {
            logger.info(formatMessage("Loading the writer configuration: " + getConfigName(WRITER_CONFIG)));
            writerConfig = new CycleRunnerConfiguration(getConfigName(WRITER_CONFIG));
            writerConfig.load();
        } catch (ConfigurationException c) {
            logger.info(formatMessage("Specific config not found, loading the default configuration: " + getDefaultConfigName(WRITER_CONFIG)));
            writerConfig = new CycleRunnerConfiguration(getDefaultConfigName(WRITER_CONFIG));
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
