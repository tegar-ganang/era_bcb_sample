package com.ohua.tests.jms;

import java.util.logging.Level;
import junit.framework.Assert;
import org.junit.Test;
import com.ohua.eai.SimpleProcessListener;
import com.ohua.engine.OhuaProcessRunner;
import com.ohua.engine.UserRequest;
import com.ohua.engine.UserRequestType;
import com.ohua.engine.operators.ConsumerOperator;
import com.ohua.engine.resource.management.ResourceManager;
import com.ohua.engine.utils.OhuaLoggerFactory;

public class testJMSSupport extends AbstractJMSTestCase {

    /**
   * Two simple flows: one writes to a queue and the other one reads from that queue. <br>
   * Execution: first writer and then the reader.
   */
    @Test
    public void testWriterReaderFlow1() throws Throwable {
        outputLogToFile(OhuaLoggerFactory.getLogIDForOperator(ConsumerOperator.class, "TestConsumer", "2") + "-result", Level.INFO);
        runFlowNoAssert(getTestMethodInputDirectory() + "jms-writer-flow.xml");
        OhuaProcessRunner runner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-reader-flow.xml");
        SimpleProcessListener listener = new SimpleProcessListener();
        runner.register(listener);
        new Thread(runner, "jms-reader-process").start();
        runner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        waitForShutDown();
        runner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        listener.awaitProcessingCompleted();
        assertBaselines();
        Assert.assertFalse(ResourceManager.getInstance().hasExternalActivators());
    }

    /**
   * Two simple flows: one writes to a queue and the other one reads from that queue. <br>
   * Execution: first reader, sleep(2000), then writer. Tests the waiting protocol and the
   * reactivation of the reader once the writer has written something.
   */
    @Test
    public void testWriterReaderFlow2() throws Throwable {
        outputLogToFile(OhuaLoggerFactory.getLogIDForOperator(ConsumerOperator.class, "TestConsumer", "2") + "-result", Level.INFO);
        OhuaProcessRunner writerRunner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-writer-flow.xml");
        SimpleProcessListener writerListener = new SimpleProcessListener();
        writerRunner.register(writerListener);
        new Thread(writerRunner, "jms-writer-process").start();
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        writerListener.awaitProcessingCompleted();
        writerListener.reset();
        OhuaProcessRunner readerRunner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-reader-flow.xml");
        SimpleProcessListener readerListener = new SimpleProcessListener();
        readerRunner.register(readerListener);
        new Thread(readerRunner, "jms-reader-process").start();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        readerListener.awaitProcessingCompleted();
        readerListener.reset();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        Thread.sleep(2000);
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        writerListener.awaitProcessingCompleted();
        waitForShutDown();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        readerListener.awaitProcessingCompleted();
        writerListener.reset();
        readerListener.reset();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        readerListener.awaitProcessingCompleted();
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        writerListener.awaitProcessingCompleted();
        assertBaselines();
        Assert.assertFalse(ResourceManager.getInstance().hasExternalActivators());
    }

    /**
   * Two simple flows: one writes to a queue and the other one reads from that queue.<br>
   * Execution: Kick off the writer first, wait a bit and then kick-off the reader.
   */
    @Test
    public void testWriterReaderFlow3() throws Throwable {
        outputLogToFile(OhuaLoggerFactory.getLogIDForOperator(ConsumerOperator.class, "TestConsumer", "2") + "-result", Level.INFO);
        OhuaProcessRunner writerRunner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-writer-flow.xml");
        SimpleProcessListener writerListener = new SimpleProcessListener();
        writerRunner.register(writerListener);
        new Thread(writerRunner, "jms-writer-process").start();
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        writerListener.awaitProcessingCompleted();
        writerListener.reset();
        OhuaProcessRunner readerRunner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-reader-flow.xml");
        SimpleProcessListener readerListener = new SimpleProcessListener();
        readerRunner.register(readerListener);
        new Thread(readerRunner, "jms-reader-process").start();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        readerListener.awaitProcessingCompleted();
        readerListener.reset();
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        Thread.sleep(500);
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        writerListener.awaitProcessingCompleted();
        waitForShutDown();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        readerListener.awaitProcessingCompleted();
        writerListener.reset();
        readerListener.reset();
        readerRunner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        readerListener.awaitProcessingCompleted();
        writerRunner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        writerListener.awaitProcessingCompleted();
        assertBaselines();
        Assert.assertFalse(ResourceManager.getInstance().hasExternalActivators());
    }

    /**
   * This test case shows that it works to interrupt a waiting reader and shut down the process.<br>
   * Execution: We just start a reader process, perform a sleep(1000) and then request a stop.
   */
    @Test
    public void testShutDown() throws Throwable {
        OhuaProcessRunner runner = new OhuaProcessRunner(getTestClassInputDirectory() + "jms-reader-flow.xml");
        SimpleProcessListener listener = new SimpleProcessListener();
        runner.register(listener);
        new Thread(runner, "jms-reader-process").start();
        runner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        Thread.sleep(2000);
        runner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        listener.awaitProcessingCompleted();
        Assert.assertFalse(ResourceManager.getInstance().hasExternalActivators());
    }

    /**
   * A flow with a bounded and an unbounded connection. The flow should not shut down unless the
   * user says so!
   */
    @Test
    public void testMultipleSources() throws Throwable {
        OhuaProcessRunner runner = new OhuaProcessRunner(getTestMethodInputDirectory() + "multiple-sources-flow.xml");
        runner.loadRuntimeConfiguration(getTestMethodInputDirectory() + "ft-runtime-parameters.properties");
        SimpleProcessListener listener = new SimpleProcessListener();
        runner.register(listener);
        new Thread(runner, "multiple-sources-process").start();
        runner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        long start = System.currentTimeMillis();
        waitForShutDown(3000);
        long l = System.currentTimeMillis() - start;
        Assert.assertTrue("shutdown time: " + l, l >= 3000);
        runner.submitUserRequest(new UserRequest(UserRequestType.FINISH_COMPUTATION));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        listener.awaitProcessingCompleted();
        Assert.assertFalse(ResourceManager.getInstance().hasExternalActivators());
    }
}
