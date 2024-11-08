package org.coos.messaging.transport;

import org.coos.messaging.COOS;
import org.coos.messaging.COOSFactory;
import org.coos.messaging.Channel;
import org.coos.messaging.ChannelServer;
import org.coos.messaging.Message;
import org.coos.messaging.MessageContext;
import org.coos.messaging.Processor;
import org.coos.messaging.ProcessorException;
import org.coos.messaging.Transport;
import org.coos.messaging.impl.DefaultProcessor;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;

/**
 * The JVM transport is used between COOS instances and Plugins (COOS to COOS and COOS to/from
 * Plugin) residing in the same VM.
 *
 * @author Knut Eilif Husa, Tellu AS
 */
public class JvmTransport extends DefaultProcessor implements Transport {

    private static final String PROPERTY_COOS_INSTANCE_NAME = "COOSInstanceName";

    private static final String PROPERTY_CHANNEL_SERVER_NAME = "ChannelServerName";

    private static final String PROPERTY_RETRY = "retry";

    private static final String PROPERTY_RETRY_TIME = "retryTime";

    protected static final Log logger = LogFactory.getLog(JvmTransport.class.getName());

    private Channel channel;

    private ChannelServer channelServer;

    private InternalTransport intr = new InternalTransport();

    private Processor chainedProcessor;

    private boolean running = false;

    protected int retryTime = 100;

    protected boolean retry;

    private Message storedConnectMsg = null;

    /**
     * Processes the message
     *
     * @param msg
     *            the message to be processed
     */
    public void processMessage(Message msg) throws ProcessorException {
        msg.setMessageContext(new MessageContext());
        if (intr.chainedProcessor != null) {
            if (!running) {
                throw new ProcessorException("JVMTransport: " + name + " is stopped.");
            }
            intr.chainedProcessor.processMessage(msg);
        } else {
            storedConnectMsg = msg;
        }
    }

    /**
     * Sets the Processor that this Processor will call after finished
     * processing
     *
     * @param chainedProcessor
     */
    public void setChainedProcessor(Processor chainedProcessor) {
        this.chainedProcessor = chainedProcessor;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setChannelServer(ChannelServer channelServer) {
        this.channelServer = channelServer;
    }

    /**
     * Starts the service
     *
     * @throws Exception
     *             Exception thrown if starting of service fails
     */
    public void start() throws Exception {
        String retryStr = (String) properties.get(PROPERTY_RETRY);
        if ((retryStr != null) && retryStr.equals("true")) {
            retry = true;
        } else {
            retry = false;
        }
        if (properties.get(PROPERTY_RETRY_TIME) != null) {
            retryTime = Integer.valueOf((String) properties.get(PROPERTY_RETRY_TIME));
        }
        if (!running) {
            if (retry) {
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        try {
                            doStart(true);
                        } catch (Exception e) {
                            logger.warn("Exception ignored", e);
                        }
                    }
                });
                t.start();
            } else {
                doStart(false);
            }
        }
    }

    private void doStart(boolean retry) throws Exception {
        if (channelServer == null) {
            String coosInstanceName = (String) properties.get(PROPERTY_COOS_INSTANCE_NAME);
            COOS coos;
            if (coosInstanceName != null) {
                coos = COOSFactory.getCOOSInstance(coosInstanceName);
                while (((coos = COOSFactory.getCOOSInstance(coosInstanceName)) == null) && retry) {
                    logger.warn("Establishing transport to JVM coos " + coosInstanceName + " failed. Retrying in " + retryTime + " millisec.");
                    Thread.sleep(retryTime);
                }
                if (coos == null) {
                    throw new NullPointerException("No COOS instance " + coosInstanceName + " defined in this vm!");
                }
            } else {
                coos = COOSFactory.getDefaultCOOSInstance();
                while (((coos = COOSFactory.getDefaultCOOSInstance()) == null) && retry) {
                    logger.warn("Establishing transport to JVM defaultCOOS failed. Retrying in " + retryTime + " millisec.");
                    Thread.sleep(retryTime);
                }
                if (coos == null) {
                    throw new NullPointerException("No defaultCOOS defined in this vm!");
                }
            }
            String channelServerName = (String) properties.get(PROPERTY_CHANNEL_SERVER_NAME);
            if (channelServerName == null) {
                channelServerName = "default";
            }
            channelServer = coos.getChannelServer(channelServerName);
            if (channelServer == null) {
                if (!retry) throw new NullPointerException("ChannelServer: " + channelServerName + " is not declared within COOS instance: " + coosInstanceName);
                while (((channelServer = coos.getChannelServer(channelServerName)) == null) && retry) {
                    Thread.sleep(retryTime);
                    logger.warn("Establishing transport to JVM channelserver failed. Retrying in " + retryTime + " millisec.");
                }
            }
            logger.debug("Established transport");
        }
        running = true;
        intr.start();
        channelServer.initializeChannel(intr);
        if (storedConnectMsg != null) {
            intr.chainedProcessor.processMessage(storedConnectMsg);
            storedConnectMsg = null;
        }
    }

    /**
     * Stops the service
     *
     * @throws Exception
     *             Exception thrown if stopping of service fails
     */
    public void stop() throws Exception {
        if (running) {
            running = false;
            channel.disconnect();
            intr.stop();
            channelServer = null;
        }
    }

    @Override
    public Processor copy() {
        JvmTransport transport = (JvmTransport) super.copy();
        transport.setChannel(channel);
        return transport;
    }

    private class InternalTransport extends DefaultProcessor implements Transport {

        private boolean running = false;

        Processor chainedProcessor;

        Channel channel;

        /**
         * Processes the message
         *
         * @param msg
         *            the message to be processed
         */
        public void processMessage(Message msg) throws ProcessorException {
            if (!running) {
                throw new ProcessorException("JVMTransport: " + name + " is stopped.");
            }
            msg.setMessageContext(new MessageContext());
            JvmTransport.this.chainedProcessor.processMessage(msg);
        }

        /**
         * Sets the Processor that this Processor will call after finished
         * processing
         *
         * @param chainedProcessor
         */
        public void setChainedProcessor(Processor chainedProcessor) {
            this.chainedProcessor = chainedProcessor;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        /**
         * Starts the service
         *
         * @throws Exception
         *             Exception thrown if starting of service fails
         */
        public void start() throws Exception {
            if (!running) {
                running = true;
            }
        }

        /**
         * Stops the service
         *
         * @throws Exception
         *             Exception thrown if stopping of service fails
         */
        public void stop() throws Exception {
            if (running) {
                running = false;
                if (channel != null) {
                    channel.disconnect();
                }
                JvmTransport.this.stop();
            }
        }
    }
}
