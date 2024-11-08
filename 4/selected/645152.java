package org.coos.messaging.transport;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.coos.messaging.Channel;
import org.coos.messaging.Message;
import org.coos.messaging.Processor;
import org.coos.messaging.ProcessorException;
import org.coos.messaging.Service;
import org.coos.messaging.Transport;
import org.coos.messaging.impl.DefaultMessage;
import org.coos.messaging.impl.DefaultProcessor;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;

/**
 * @author Knut Eilif Husa, Tellu AS UDP transport
 */
public class UDPTransport extends DefaultProcessor implements Transport, Service {

    private static final String PROPERTY_HOST = "host";

    private static final String PROPERTY_PORT = "port";

    private static final String PROPERTY_RETRY = "retry";

    private static final String PROPERTY_RETRY_TIME = "retryTime";

    public static int MAX_UDP_SIZE = 2 << 16;

    protected static final Log logger = LogFactory.getLog(UDPTransport.class.getName());

    protected String hostName;

    protected int hostPort = 15656;

    protected int retryTime = 10000;

    protected boolean retry = false;

    protected DatagramSocket socket;

    private Reader reader;

    private Writer writer;

    protected List<Message> mailbox = Collections.synchronizedList(new LinkedList<Message>());

    protected Processor transportProcessor;

    protected boolean running = true;

    protected Channel channel;

    public UDPTransport() {
    }

    public UDPTransport(String hostIP, int hostPort) {
        this.hostName = hostIP;
        this.hostPort = hostPort;
    }

    UDPTransport(DatagramSocket socket, String hostIP, int hostPort) {
        this.socket = socket;
        this.hostName = hostIP;
        this.hostPort = hostPort;
    }

    public Processor getTransportProcessor() {
        return transportProcessor;
    }

    public void setChainedProcessor(Processor transportProcessor) {
        this.transportProcessor = transportProcessor;
    }

    public void processMessage(Message msg) throws ProcessorException {
        if (!running) {
            return;
        }
        String priStr = msg.getHeader(Message.PRIORITY);
        if (priStr != null) {
            int pri = Integer.valueOf(priStr);
            int idx = 0;
            for (Message message : mailbox) {
                String pr = message.getHeader(Message.PRIORITY);
                if (pr != null) {
                    int p = Integer.valueOf(pr);
                    if (pri < p) {
                        synchronized (this) {
                            mailbox.add(idx, msg);
                            this.notify();
                        }
                        break;
                    }
                }
                idx++;
            }
        } else {
            synchronized (this) {
                mailbox.add(msg);
                this.notify();
            }
        }
    }

    public void receivedMessage(byte[] data) {
        try {
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
            Message msg = null;
            msg = new DefaultMessage(din);
            transportProcessor.processMessage(msg);
        } catch (SocketException e) {
            logger.info("Connection closing");
            running = false;
        } catch (EOFException e) {
            logger.info("Connection closing EOF");
            running = false;
        } catch (Exception e) {
            logger.error("Error in Message deserialization. Aborting", e);
            running = false;
        }
    }

    public void start() throws Exception {
        running = true;
        if (socket != null) {
            writer = new Writer();
            return;
        }
        if (hostName == null) {
            hostName = (String) properties.get(PROPERTY_HOST);
        }
        if (properties.get(PROPERTY_PORT) != null) {
            hostPort = Integer.valueOf((String) properties.get(PROPERTY_PORT));
        }
        String retryStr = (String) properties.get(PROPERTY_RETRY);
        if ((retryStr != null) && retryStr.equals("true")) {
            retry = true;
        } else {
            retry = false;
        }
        if (properties.get(PROPERTY_RETRY_TIME) != null) {
            retryTime = Integer.valueOf((String) properties.get(PROPERTY_RETRY_TIME));
        }
        logger.info("Establishing transport to " + hostName + ":" + hostPort);
        if (retry) {
            Thread t = new Thread(new Runnable() {

                public void run() {
                    boolean connecting = true;
                    while (connecting && running) {
                        try {
                            socket = new DatagramSocket();
                            socket.connect(new InetSocketAddress(hostName, hostPort));
                            connecting = false;
                        } catch (IOException e) {
                            logger.warn("Establishing transport to " + hostName + ":" + hostPort + " failed. Retrying in " + retryTime + " millisec.");
                            try {
                                Thread.sleep(retryTime);
                            } catch (InterruptedException e1) {
                                logger.warn("InterruptedException ignored.", e);
                            }
                        }
                        if (!connecting && running) {
                            logger.info("Transport from " + socket.getLocalSocketAddress() + " to " + socket.getRemoteSocketAddress() + " established.");
                        }
                    }
                    if (running) {
                        reader = new Reader();
                        writer = new Writer();
                    }
                }
            });
            t.start();
        } else {
            try {
                socket = new DatagramSocket();
                socket.connect(new InetSocketAddress(hostName, hostPort));
            } catch (IOException e) {
                running = false;
                logger.warn("Establishing transport to " + hostName + ":" + hostPort + " failed.");
                throw e;
            }
            if (running) {
                reader = new Reader();
                writer = new Writer();
            }
        }
    }

    public void stop() throws Exception {
        logger.info("Closing transport: " + hostName + ":" + hostPort);
        running = false;
        if (reader != null) {
            reader.stop();
        }
        if (writer != null) {
            writer.stop();
        }
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    public int getQueueSize() {
        return mailbox.size();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    class Reader implements Runnable {

        Thread readerThread;

        boolean running = true;

        Reader() {
            readerThread = new Thread(this);
            readerThread.start();
            logger.info("Reader started on :" + socket.getLocalSocketAddress());
        }

        public void stop() {
            running = false;
        }

        public void run() {
            try {
                while (running) {
                    byte[] buffer = new byte[MAX_UDP_SIZE];
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    socket.receive(dp);
                    receivedMessage(dp.getData());
                }
                if (channel != null) {
                    channel.disconnect();
                }
            } catch (IOException e) {
                if (running) {
                    logger.error("IOException ignored", e);
                }
            }
        }
    }

    class Writer implements Runnable {

        Thread writerThread;

        boolean running = true;

        Writer() {
            writerThread = new Thread(this);
            writerThread.start();
            logger.info("Writer started on :" + socket.getLocalSocketAddress());
        }

        public void stop() {
            running = false;
            writerThread.interrupt();
        }

        public void run() {
            while (running) {
                synchronized (UDPTransport.this) {
                    try {
                        if (mailbox.isEmpty()) {
                            UDPTransport.this.wait();
                        }
                    } catch (InterruptedException e) {
                        if (!running) return;
                    }
                }
                if (!mailbox.isEmpty()) {
                    Message msg = mailbox.remove(0);
                    try {
                        byte[] data = msg.serialize();
                        DatagramPacket dp = new DatagramPacket(data, data.length, new InetSocketAddress(hostName, hostPort));
                        socket.send(dp);
                    } catch (SocketException e) {
                        if (e.getMessage().equals("socket closed")) {
                            logger.info("Connection closing");
                            running = false;
                        }
                    } catch (Exception e) {
                        logger.error("Error in Message writing. Aborting", e);
                        running = false;
                    }
                }
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
