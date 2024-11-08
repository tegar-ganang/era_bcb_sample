package org.coos.messaging.transport;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Knut Eilif Husa, Tellu AS
 *
 * A TCP transport
 */
public class TCPTransport extends DefaultProcessor implements Transport, Service {

    static final String PROPERTY_HOST = "host";

    static final String PROPERTY_PORT = "port";

    static final String PROPERTY_RETRY = "retry";

    static final String PROPERTY_RETRY_TIME = "retryTime";

    static final int CONNECT_TIMEOUT = 4000;

    protected static final int MAX_LENGTH = (16 * 1024);

    protected static final int MAX_BODY_LENGTH = (8 * 1024);

    protected static final Log logger = LogFactory.getLog(TCPTransport.class.getName());

    protected String hostName;

    protected int hostPort = 15656;

    protected int retryTime = 10000;

    protected boolean retry = false;

    protected Socket socket;

    protected Reader reader;

    protected Writer writer;

    protected List<Message> mailbox = Collections.synchronizedList(new LinkedList<Message>());

    protected Processor transportProcessor;

    protected boolean running = false;

    protected Channel channel;

    TCPTransportManager tm;

    public TCPTransport() {
    }

    public TCPTransport(String hostIP, int hostPort) {
        this.hostName = hostIP;
        this.hostPort = hostPort;
    }

    TCPTransport(Socket socket, TCPTransportManager tm) {
        this.socket = socket;
        this.tm = tm;
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
                    synchronized (TCPTransport.this) {
                        boolean connecting = true;
                        while (connecting && running) {
                            try {
                                socket = createClient(hostName, hostPort);
                                connecting = false;
                            } catch (Exception e) {
                                logger.warn("Establishing transport to " + hostName + ":" + hostPort + " failed. Retrying in " + retryTime + " millisec.");
                                try {
                                    TCPTransport.this.wait(retryTime);
                                } catch (InterruptedException e1) {
                                    logger.warn("Interrupted while waiting for retry.", e);
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
                }
            });
            t.start();
        } else {
            try {
                socket = createClient(hostName, hostPort);
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

    public Socket createClient(String serverHost, int port) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(hostName, hostPort), CONNECT_TIMEOUT);
        return socket;
    }

    public synchronized void stop() throws Exception {
        if (!running) {
            return;
        }
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
        if (tm != null) tm.disconnected(this);
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
            logger.info("Reader started on :" + socket.getLocalSocketAddress());
            failed = false;
        }

        public void stop() {
            stopped = true;
            running = false;
            if (!failed) retry = false;
        }

        public void run() {
            try {
                is = socket.getInputStream();
                DataInputStream din = new DataInputStream(is);
                while (running) {
                    Message msg = null;
                    try {
                        int size = din.readInt();
                        if (size + 4 > MAX_LENGTH) {
                            throw new IOException("Packet too big");
                        }
                        byte[] buf = new byte[size + 1];
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(bos);
                        dos.writeInt(size);
                        is.read(buf, 0, size);
                        dos.write(buf);
                        ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
                        DataInputStream di = new DataInputStream(bais);
                        msg = new DefaultMessage(di);
                    } catch (SocketException e) {
                        logger.info("Connection closing");
                        running = false;
                        failed = true;
                    } catch (EOFException e) {
                        logger.info("Connection closing EOF");
                        running = false;
                        failed = true;
                    } catch (Exception e) {
                        logger.error("Error in Message deserialization. Aborting", e);
                        running = false;
                        failed = true;
                    }
                    try {
                        if (running) {
                            if ((msg.getSerializedBody() == null) || ((msg.getSerializedBody() != null) && (msg.getSerializedBody().length <= MAX_BODY_LENGTH))) {
                                transportProcessor.processMessage(msg);
                            } else {
                                throw new IOException("Body too big, length=" + msg.getSerializedBody().length);
                            }
                        }
                    } catch (ProcessorException e) {
                        logger.error("ProcessorException caught when processing message " + msg.getName() + " Ignored.", e);
                    } catch (Exception e) {
                        logger.error("Unknown Exception caught when processing message " + msg.getName() + " Ignored.", e);
                    }
                }
                is.close();
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
            logger.info("Writer started on :" + socket.getLocalSocketAddress());
        }

        public void stop() {
            running = false;
            writerThread.interrupt();
        }

        public void run() {
            try {
                os = socket.getOutputStream();
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
                        Message msg = mailbox.remove(0);
                        try {
                            os.write(msg.serialize());
                            os.flush();
                        } catch (SocketException e) {
                            running = false;
                            if (e.getMessage().equals("Socket closed")) {
                                logger.info("Connection closing");
                            } else {
                                logger.error("Error in Message writing. Aborting", e);
                            }
                        } catch (Exception e) {
                            logger.error("Error in Message writing. Aborting", e);
                            running = false;
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
