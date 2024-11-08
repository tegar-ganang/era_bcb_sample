package com.funda.xmppserver.reader;

import com.funda.xmppserver.ComponentParser;
import com.funda.xmppserver.EventHandler;
import com.funda.xmppserver.handlers.AuthHandler;
import com.funda.xmppserver.handlers.EndStreamHandler;
import com.funda.xmppserver.handlers.IQHandler;
import com.funda.xmppserver.handlers.MessageHandler;
import com.funda.xmppserver.handlers.PresenceHandler;
import com.funda.xmppserver.handlers.StreamHandler;
import com.funda.xmppserver.usermanager.Users;
import com.funda.xmppserver.writer.XMPPPacketWriter;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author kurinchi
 */
public class XMPPPacketReader extends Thread {

    DataInputStream reader;

    XMLInputFactory inputfactory;

    XMLStreamReader streamreader;

    EventHandler handler;

    Map delegates;

    protected XMPPPacketWriter packetwriter;

    /**
     * Get the value of packetwriter
     *
     * @return the value of packetwriter
     */
    public XMPPPacketWriter getPacketwriter() {
        return packetwriter;
    }

    /**
     * Set the value of packetwriter
     *
     * @param packetwriter new value of packetwriter
     */
    public void setPacketwriter(XMPPPacketWriter packetwriter) {
        this.packetwriter = packetwriter;
    }

    public XMPPPacketReader(DataInputStream reader, XMPPPacketWriter packetwriter) {
        try {
            this.reader = reader;
            this.packetwriter = packetwriter;
            inputfactory = XMLInputFactory.newInstance();
            streamreader = inputfactory.createXMLStreamReader(this.reader);
            delegates = new HashMap();
            handler = new EventHandler(delegates);
            handler.registerParser("stream", new StreamHandler(packetwriter));
            handler.registerParser("auth", new AuthHandler(packetwriter, packetwriter.getClient()));
            handler.registerParser("iq", new IQHandler(packetwriter, packetwriter.getClient()));
            handler.registerParser("presence", new PresenceHandler(packetwriter, packetwriter.getClient()));
            handler.registerParser("message", new MessageHandler(packetwriter, packetwriter.getClient()));
        } catch (XMLStreamException ex) {
            Logger.getLogger(XMPPPacketReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void run() {
        try {
            if (XMLEvent.START_DOCUMENT == streamreader.getEventType()) {
                System.out.println("Jabber Client Connected...");
            }
            while (streamreader.hasNext()) {
                streamreader.next();
                int event = streamreader.getEventType();
                if (event == XMLEvent.START_ELEMENT) {
                    String localname = streamreader.getLocalName();
                    if (delegates.containsKey(localname)) {
                        ComponentParser parser = (ComponentParser) delegates.get(localname);
                        parser.parseElement(streamreader);
                    }
                } else if (event == XMLEvent.END_ELEMENT) {
                    if (streamreader.getLocalName().equalsIgnoreCase("stream")) {
                        EndStreamHandler endstreamhandle = new EndStreamHandler(packetwriter);
                        endstreamhandle.closeStream();
                    }
                }
            }
        } catch (XMLStreamException ex) {
            System.out.println("Client unavailable :" + packetwriter.getClient().getUsername());
            packetwriter.updateAllRoster(packetwriter.getClient().getPresencemanager(), packetwriter.getClient().getPresencemanager().getShowlist(), "unavailable");
            List list = (List) Users.getUserjidlist().get(packetwriter.getClient().getUsername());
            list.remove(packetwriter.getClient().getJid().toString());
            if (list.isEmpty()) {
                Users.getUserjidlist().remove(packetwriter.getClient().getUsername());
            } else {
                Users.getUserjidlist().put(packetwriter.getClient().getUsername(), list);
            }
            Users.getUserlist().remove(packetwriter.getClient().getJid().toString());
            Logger.getLogger(XMPPPacketReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
