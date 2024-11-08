package uk.ekiwi.messaging.mq;

import com.ibm.mq.*;
import java.io.*;
import com.ibm.mq.pcf.*;
import oracle.xml.differ.*;

/**
 *
 * <p>Title: MQUtil</p>
 * <p>Description: Provides functions for connecting to a mreote queue manager
 *    and writing to a specified queue.</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * @author Bryce Cummock
 * @version 1.0
 */
public class MqConnection {

    private MqQueueManager qm = null;

    private MQQueueManager _queueManager = null;

    /**
    *
    * @param QueueManager Object contain details of queue manager to connect
    * @throws MQException
    */
    protected MqConnection(MqQueueManager qm) throws MQException {
        this.qm = qm;
        MQEnvironment.hostname = qm.getHostName();
        MQEnvironment.channel = qm.getChannel();
        MQEnvironment.port = qm.getPort();
        _queueManager = new MQQueueManager(qm.getQManager());
    }

    public MQQueue connectQueue(String queueName, int openOptions) throws MQException {
        MQQueue queue = _queueManager.accessQueue(queueName, openOptions, null, null, null);
        return queue;
    }

    protected MQQueueManager getMQManager() {
        return _queueManager;
    }

    protected PCFAgent getPCFAgent() throws MQException {
        PCFAgent pcfAgent = new PCFMessageAgent(_queueManager);
        return pcfAgent;
    }

    public void close() throws MQException {
        if (_queueManager.isConnected()) _queueManager.disconnect();
        _queueManager = null;
    }

    protected void finalize() throws Throwable {
        close();
    }
}
