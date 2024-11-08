package com.ohua.tests.checkpoint.operators;

import static org.junit.Assert.assertFalse;
import java.util.logging.Level;
import org.junit.Ignore;
import org.junit.Test;
import com.ohua.engine.operators.DatabaseReaderOperator;
import com.ohua.engine.utils.FileUtils;
import com.ohua.engine.utils.OhuaLoggerFactory;
import com.ohua.tests.AbstractIOTestCase;
import com.ohua.tests.checkpoint.CheckpointRegressionAsserts;
import com.ohua.tests.checkpoint.CrashRestartRunner;

public class testCPDatabaseOperators extends AbstractIOTestCase {

    /**
   * This test will assure that we are able to produce checkpoints and that the checkpointing
   * algorithm in the operators does not have any endless loops or deadlocks.
   */
    @Test
    public void testRunWithCheckpoints() throws Throwable {
        outputLogToFile(OhuaLoggerFactory.getLogIDForOperator(DatabaseReaderOperator.class, "DerbyReader", "1") + "-result", Level.INFO);
        fillTable("test_reader_table", 30000, "NAME", "ADDRESS", "PHONE");
        runFlow(getTestClassInputDirectory() + "Database-reader-writer-flow.xml", getTestMethodInputDirectory() + "runtime-parameters.properties");
        CheckpointRegressionAsserts.assertCheckpointsTaken(getTestMethodOutputDirectory(), 4, 3);
        CheckpointRegressionAsserts.assertCheckpointBalance(getTestMethodOutputDirectory(), new int[] { 1 }, new int[] { 3 });
        tableRegressionCheck("test_writer_table", 30000);
    }

    /**
   * This test case makes sure that we can restart a flow from a given checkpoint and arrive at
   * the same result as in a crash-free run.
   */
    @Test
    @Ignore
    public void testRestartFromCheckpoint() throws Throwable {
    }

    /**
   * This is the full scenario. We start a flow and crash it at some point of processing.
   * Afterwards we restart it again and let it complete its computation.
   * @throws Throwable
   */
    @Test
    public void testCrashRestart() throws Throwable {
        outputLogToFile(CrashRestartRunner.class);
        fillTable("test_reader_table", 30000, "NAME", "ADDRESS", "PHONE");
        CrashRestartRunner runner = new CrashRestartRunner();
        runner.setPathToFlow(getTestClassInputDirectory() + "Database-reader-writer-flow.xml");
        runner.applyDefaultConfiguration(getTestMethodInputDirectory());
        runner.crashAndRestart(8000, false);
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_0/checkpoints"));
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_1/checkpoints"));
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_2/checkpoints"));
        testStartDerbyServer();
        tableRegressionCheck("test_writer_table", 30000);
        assertBaselines();
    }

    /**
   * Same flow as above but with a non-batch database writer operator.
   * @throws Throwable
   */
    @Test
    public void testCrashRestartNonBatchWriter() throws Throwable {
        outputLogToFile(CrashRestartRunner.class);
        fillTable("test_reader_table", 30000, "NAME", "ADDRESS", "PHONE");
        CrashRestartRunner runner = new CrashRestartRunner();
        runner.setPathToFlow(getTestMethodInputDirectory() + "Database-reader-writer-flow.xml");
        runner.setPathToRuntimeConfiguration(getTestMethodInputDirectory() + "runtime-parameters.properties");
        runner.setPathToRestartRuntimeConfiguration(getTestMethodInputDirectory() + "restart-runtime-parameters.properties");
        runner.setLoggingConfiguration(getTestMethodInputDirectory() + "logging-configuration.properties");
        runner.crashAndRestart(8000, false);
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_0/checkpoints"));
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_1/checkpoints"));
        assertFalse(FileUtils.isDirectoryEmpty(getTestMethodOutputDirectory() + "operators/operator_2/checkpoints"));
        testStartDerbyServer();
        tableRegressionCheck("test_writer_table", 30000);
        assertBaselines();
    }
}
