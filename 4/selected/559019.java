package com.simconomy.xmpp.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import com.simconomy.xmpp.exceptions.PacketException;
import com.simconomy.xmpp.exceptions.XMPPWriteException;
import com.simconomy.xmpp.model.Packet;
import com.simconomy.xmpp.service.ConnectionService;

public class ConnectionServiceImpl implements ConnectionService {

    private static final Logger log = Logger.getLogger(ConnectionServiceImpl.class);

    private XMLEventWriter eventWriter = null;

    private XMLStreamReader reader = null;

    private Thread readerThread;

    private Thread writerThread;

    private String serviceName;

    private final BlockingQueue<Packet> queue = new ArrayBlockingQueue<Packet>(500, true);

    public ConnectionServiceImpl(String host, int port, String serviceName) throws XMPPException {
        this.serviceName = serviceName;
        try {
            Socket socket = new Socket(host, port);
            InputStreamReader in = new InputStreamReader(socket.getInputStream(), "UTF-8");
            XMLInputFactory inFactory = XMLInputFactory.newInstance();
            reader = inFactory.createXMLStreamReader(in);
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
            eventWriter = outFactory.createXMLEventWriter(out);
            readerThread = new Thread() {

                public void run() {
                    parsePackets(this);
                }
            };
            readerThread.setName("XMPP Packet Reader (" + host + ":" + port + ")");
            readerThread.setDaemon(true);
            writerThread = new Thread() {

                public void run() {
                    writePackets(this);
                }
            };
            writerThread.setName("XMPP Packet Writer (" + host + ":" + port + ")");
            writerThread.setDaemon(true);
            startup();
        } catch (UnknownHostException uhe) {
            String errorMessage = "Could not connect to " + host + ":" + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(XMPPError.Condition.remote_server_timeout, errorMessage), uhe);
        } catch (IOException ioe) {
            String errorMessage = "XMPPError connecting to " + host + ":" + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(XMPPError.Condition.remote_server_error, errorMessage), ioe);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    protected void writePackets(Thread thisThread) {
        try {
            writeStart();
            while ((writerThread == thisThread)) {
                Packet packet = nextPacket();
                if (packet != null) {
                    synchronized (eventWriter) {
                        eventWriter.add(packet.getEventReader());
                        eventWriter.flush();
                    }
                }
            }
            try {
                synchronized (eventWriter) {
                    while (!queue.isEmpty()) {
                        Packet packet = queue.remove();
                        eventWriter.add(packet.getEventReader());
                    }
                    eventWriter.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            queue.clear();
            try {
                XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
                eventWriter.add(xmlEventFactory.createEndElement("stream", "http://etherx.jabber.org/streams", "stream"));
                eventWriter.flush();
            } catch (Exception e) {
            } finally {
                try {
                    eventWriter.close();
                } catch (Exception e) {
                }
            }
        } catch (XMPPWriteException ioe) {
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (PacketException e) {
            e.printStackTrace();
        }
    }

    private Packet nextPacket() {
        Packet packet = null;
        while ((packet = queue.poll()) == null) {
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException ie) {
            }
        }
        return packet;
    }

    protected void parsePackets(Thread thread) {
        while (true) {
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.END_DOCUMENT) {
                        reader.close();
                        break;
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        log.info(reader.getLocalName());
                    }
                    if (event == XMLStreamConstants.END_ELEMENT) {
                        log.info(reader.getLocalName());
                    }
                    if (event == XMLStreamConstants.SPACE) {
                        log.info("SPACE: '" + reader.getLocalName() + "'");
                    }
                }
            } catch (XMLStreamException e) {
            }
        }
    }

    public void write(String xml) throws XMPPWriteException {
        try {
            InputStream in = new ByteArrayInputStream(xml.getBytes());
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader parser = factory.createXMLEventReader(in);
            while (parser.hasNext()) {
                XMLEvent xmlEvent = parser.nextEvent();
                write(xmlEvent);
            }
        } catch (XMLStreamException e) {
            throw new XMPPWriteException(e);
        }
    }

    public void write(XMLEvent xmlEvent) throws XMPPWriteException {
        try {
            eventWriter.add(xmlEvent);
        } catch (XMLStreamException e) {
            throw new XMPPWriteException(e);
        }
    }

    public void writeStart() throws XMPPWriteException {
        try {
            XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
            eventWriter.add(xmlEventFactory.createStartElement("stream", "stream", "http://etherx.jabber.org/streams"));
            eventWriter.add(xmlEventFactory.createNamespace("xmlns", "jabber:client"));
            eventWriter.add(xmlEventFactory.createNamespace("stream", "http://etherx.jabber.org/streams"));
            eventWriter.add(xmlEventFactory.createAttribute("to", serviceName));
            eventWriter.add(xmlEventFactory.createAttribute("version", "1.0"));
            eventWriter.flush();
        } catch (XMLStreamException e) {
            throw new XMPPWriteException(e);
        }
    }

    public void sendPacket(Packet packet) {
        try {
            queue.put(packet);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            return;
        }
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public void startup() throws XMPPException {
        writerThread.start();
        Semaphore connectionSemaphore = new Semaphore(1);
        readerThread.start();
        try {
            connectionSemaphore.acquire();
            int waitTime = SmackConfiguration.getPacketReplyTimeout();
            connectionSemaphore.tryAcquire(3 * waitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
        }
    }
}
