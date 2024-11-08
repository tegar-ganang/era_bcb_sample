package com.ohua.tests.checkpoint.operators;

import org.junit.Test;
import com.ohua.eai.SimpleProcessListener;
import com.ohua.eai.resources.ActiveMQEmbeddedBroker.BrokerProtocol;
import com.ohua.engine.OhuaProcessRunner;
import com.ohua.engine.ProcessNature;
import com.ohua.engine.UserRequest;
import com.ohua.engine.UserRequestType;
import com.ohua.tests.jms.AbstractJMSTestCase;
import com.ohua.tests.jms.BrokerNature;
import com.ohua.tests.jms.JMSCrashRestartRunner;

public class testCRJMSReader extends AbstractJMSTestCase {

    /**
   * The broker remains in the JUnit JVM! (We don't want to kill this one for sure!)
   * @throws Throwable
   */
    @Test
    @BrokerNature(protocol = BrokerProtocol.TCP, port = "61616")
    public void testCrashRestartJMSReader() throws Throwable {
        JMSCrashRestartRunner readerRunner = new JMSCrashRestartRunner();
        readerRunner.setPathToFlow(getTestMethodInputDirectory() + "jms-reader-flow.xml");
        readerRunner.applyDefaultConfiguration(getTestMethodInputDirectory());
        OhuaProcessRunner runner = new OhuaProcessRunner(getTestMethodInputDirectory() + "jms-writer-flow.xml");
        SimpleProcessListener listener = new SimpleProcessListener();
        runner.register(listener);
        new Thread(runner, "jms-writer-process").start();
        runner.submitUserRequest(new UserRequest(UserRequestType.INITIALIZE));
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.START_COMPUTATION));
        readerRunner.crashAndRestart(10000, false, ProcessNature.USER_DRIVEN);
        System.out.println("reader process is done");
        listener.awaitProcessingCompleted();
        listener.reset();
        runner.submitUserRequest(new UserRequest(UserRequestType.SHUT_DOWN));
        listener.awaitProcessingCompleted();
        assertBaselines();
    }
}
