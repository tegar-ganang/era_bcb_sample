package org.coos.messaging.cldc.transport;

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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

/**
 * Created by IntelliJ IDEA. User: Knut Eilif
 *
 *
 *
 */
public class TCPTransport extends DefaultProcessor implements Transport, Service {

    static final String PROPERTY_HOST = "host";

    static final String PROPERTY_PORT = "port";

    static final String PROPERTY_RETRY = "retry";

    static final String PROPERTY_RETRY_TIME = "retryTime";

    static final int CONNECT_TIMEOUT = 4000;

    protected static final Log logger = LogFactory.getLog(TCPTransport.class.getName());

    protected String hostName;

    protected int hostPort = 15656;

    protected int retryTime = 10000;

    protected boolean retry = false;

    protected SocketConnection socket;

    protected Reader reader;

    protected Writer writer;

    protected Vector mailbox = new Vector();

    protected Processor transportProcessor;

    protected boolean running = false;

    protected Channel channel;

    public TCPTransport() {
    }

    public TCPTransport(String hostIP, int hostPort) {
        this.hostName = hostIP;
        this.hostPort = hostPort;
    }

    TCPTransport(SocketConnection socket) {
        this.socket = socket;
    }

    public Processor getTransportProcessor() {
        return transportProcessor;
    }

    public void setChainedProcessor(Processor transportProcessor) {
        this.transportProcessor = transportProcessor;
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public void processMessage(Message msg) throws ProcessorException {
        if (!running) {
            throw new ProcessorException("Transport :" + name + " is stopped");
        }
        String priStr = msg.getHeader(Message.PRIORITY);
        if (priStr != null) {
            int pri = Integer.parseInt(priStr);
            int idx = 0;
            for (int i = 0; i < mailbox.size(); i++) {
                Message message = (Message) mailbox.elementAt(i);
                String pr = message.getHeader(Message.PRIORITY);
                if (pr != null) {
                    int p = Integer.parseInt(pr);
                    if (pri < p) {
                        mailbox.insertElementAt(msg, idx);
                        synchronized (this) {
                            this.notify();
                        }
                        break;
                    }
                }
                idx++;
            }
        } else {
            synchronized (this) {
                mailbox.addElement(msg);
                this.notify();
            }
        }
    }

    public synchronized void start() throws Exception {
        if (running) {
            return;
        }
        running = true;
        if (socket != null) {
            reader = new Reader();
            writer = new Writer();
            return;
        }
        if (hostName == null) {
            hostName = (String) properties.get(PROPERTY_HOST);
        }
        if (properties.get(PROPERTY_PORT) != null) {
            hostPort = Integer.valueOf((String) properties.get(PROPERTY_PORT)).intValue();
        }
        String retryStr = (String) properties.get(PROPERTY_RETRY);
        if ((retryStr != null) && retryStr.equals("true")) {
            retry = true;
        } else {
            retry = false;
        }
        if (properties.get(PROPERTY_RETRY_TIME) != null) {
            retryTime = Integer.valueOf((String) properties.get(PROPERTY_RETRY_TIME)).intValue();
        }
        logger.info("Establishing transport to " + hostName + ":" + hostPort);
        if (retry) {
            Thread t = new Thread(new Runnable() {

                public void run() {
                    if (socket == null) {
                        boolean connecting = true;
                        while (connecting && running) {
                            try {
                                socket = (SocketConnection) Connector.open("socket://" + hostName + ":" + hostPort);
                                connecting = false;
                            } catch (IOException e) {
                                String retryStr = (String) properties.get("retry");
                                if ((retryStr == null) || retryStr.equals("true")) {
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    connecting = false;
                                }
                            }
                        }
                        if (running) {
                        }
                    }
                    reader = new Reader();
                    writer = new Writer();
                }
            });
            t.start();
        } else {
            try {
                socket = (SocketConnection) Connector.open("socket://" + hostName + ":" + hostPort);
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
        if (!running) {
            return;
        }
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

        InputStream is;

        boolean running = true, failed, stopped;

        Reader() {
            readerThread = new Thread(this);
            readerThread.start();
            failed = false;
        }

        public void stop() {
            stopped = true;
            running = false;
            if (!failed) retry = false;
        }

        public void run() {
            try {
                is = socket.openInputStream();
                DataInputStream din = new DataInputStream(is);
                while (running) {
                    Message msg = null;
                    try {
                        msg = new DefaultMessage(din);
                    } catch (EOFException e) {
                        logger.info("Connection closing EOF");
                        running = false;
                        failed = true;
                    } catch (Exception e) {
                        if (e.getMessage().equals("Socket closed")) {
                            logger.info("Connection closing");
                        } else {
                            logger.error("Error in Message writing. Aborting", e);
                        }
                    }
                    try {
                        if (running) {
                            transportProcessor.processMessage(msg);
                        }
                    } catch (ProcessorException e) {
                        logger.error("ProcessorException caught when processing message " + msg.getName() + " Ignored.", e);
                    } catch (Exception e) {
                        logger.error("Unknown Exception caught when processing message " + msg.getName() + " Ignored.", e);
                    }
                }
                if (channel != null) {
                    if (!stopped) {
                        channel.disconnect();
                    }
                    if (retry) {
                        Thread.sleep(retryTime);
                        channel.connect(channel.getLinkManager());
                    }
                }
            } catch (Exception e) {
                logger.error("Unknown Error in Reader", e);
            }
        }
    }

    class Writer implements Runnable {

        Thread writerThread;

        OutputStream os;

        boolean running = true;

        Writer() {
            writerThread = new Thread(this);
            writerThread.start();
        }

        public void stop() {
            running = false;
            writerThread.interrupt();
        }

        public void run() {
            synchronized (TCPTransport.this) {
                try {
                    os = socket.openOutputStream();
                    while (running) {
                        synchronized (TCPTransport.this) {
                            try {
                                if (mailbox.isEmpty()) {
                                    TCPTransport.this.wait();
                                }
                            } catch (InterruptedException e) {
                                if (!running) return;
                            }
                        }
                        while (!mailbox.isEmpty()) {
                            Message msg = (Message) mailbox.elementAt(0);
                            mailbox.removeElement(msg);
                            try {
                                os.write(msg.serialize());
                                os.flush();
                            } catch (Exception e) {
                                running = false;
                                if (e.getMessage().equals("Socket closed")) {
                                    logger.info("Connection closing");
                                } else {
                                    logger.error("Error in Message writing. Aborting", e);
                                }
                            }
                        }
                    }
                    os.close();
                } catch (Exception e) {
                    logger.error("Unknown Error in Writer", e);
                }
            }
        }
    }
}
