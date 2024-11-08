package com.googlecode.xmpplib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import com.googlecode.xmpplib.provider.AuthenticationController;
import com.googlecode.xmpplib.provider.MessageController;
import com.googlecode.xmpplib.provider.RosterController;
import com.googlecode.xmpplib.stanzas.AuthQuery;
import com.googlecode.xmpplib.stanzas.AuthSasl;
import com.googlecode.xmpplib.stanzas.Bind;
import com.googlecode.xmpplib.stanzas.IQ;
import com.googlecode.xmpplib.stanzas.Message;
import com.googlecode.xmpplib.stanzas.PacketProcessor;
import com.googlecode.xmpplib.stanzas.RenewStreamException;
import com.googlecode.xmpplib.stanzas.Roster;
import com.googlecode.xmpplib.stanzas.Stream;
import com.googlecode.xmpplib.utils.XmlWriter;

public class StreamProcessor {

    /**
	 * 
	 */
    private long id = 0;

    /**
	 * XmppServer.
	 */
    private XmppServer xmppServer;

    /**
	 * XmppFactory.
	 */
    private XmppFactory xmppFactory;

    /**
	 * The socket reader.
	 */
    private Reader reader;

    /**
	 * The socket writer.
	 */
    private Writer writer;

    /**
	 * The pullparser for the xmpp stream.
	 */
    private XmlPullParser xmlPullParser;

    /**
	 * The xml writer for the xmpp stream.
	 */
    private XmlWriter xmlWriter;

    /**
	 * 
	 */
    public PacketProcessor processor = new PacketProcessor(this);

    /**
	 * The current streaming tag.
	 */
    public Stream stream = new Stream(this);

    public IQ iq = new IQ(this);

    public AuthQuery authQuery;

    public AuthSasl authSasl;

    /**
	 * The current bind.
	 */
    public Bind bind = new Bind(this);

    /**
	 * The current roster.
	 */
    public Roster roster = null;

    public Message message = null;

    public StreamProcessor(XmppServer xmppServer, XmppFactory xmppFactory, Reader reader, Writer writer) throws IOException, XmlPullParserException {
        this.xmppServer = xmppServer;
        this.xmppFactory = xmppFactory;
        if (reader instanceof BufferedReader) {
            this.reader = reader;
        } else {
            this.reader = new BufferedReader(reader);
        }
        if (writer instanceof BufferedWriter) {
            this.writer = writer;
        } else {
            this.writer = new BufferedWriter(writer);
        }
        AuthenticationController authenticationController = xmppFactory.createAuthenticationController();
        authQuery = new AuthQuery(this, authenticationController);
        authSasl = new AuthSasl(this, authenticationController);
        authenticationController.addListener(authQuery);
        authenticationController.addListener(authSasl);
        RosterController rosterController = xmppFactory.createRosterController();
        roster = new Roster(this, rosterController);
        MessageController messageController = xmppFactory.createMessageController();
        message = new Message(this, messageController);
        xmlPullParser = xmppFactory.createXmlPullParser(this.reader);
        xmlWriter = new XmlWriter(this.writer);
    }

    public StreamProcessor(XmppServer xmppServer, XmppFactory xmppFactory, InputStream inputStream, OutputStream outputStream) throws IOException, XmlPullParserException {
        this(xmppServer, xmppFactory, new InputStreamReader(inputStream, "UTF-8"), new OutputStreamWriter(outputStream, "UTF-8"));
    }

    public XmppServer getXmppServer() {
        return xmppServer;
    }

    public XmppFactory getXmppFactory() {
        return xmppFactory;
    }

    public XmlPullParser getXmlPullParser() {
        return xmlPullParser;
    }

    public XmlWriter getXmlWriter() {
        return xmlWriter;
    }

    public long createNextId() {
        return id++;
    }

    /**
	 * Started via {@link Thread#start()} and handles the complete stream. The
	 * thread dies if the stream is finished or an error occurs.
	 */
    public void parse() throws XmlPullParserException, IOException {
        boolean again = true;
        while (again) {
            again = false;
            try {
                while (xmlPullParser.nextTag() != XmlPullParser.START_TAG) {
                    System.out.println("stream processor#parse ignore " + xmlPullParser.getEventType());
                }
                stream.parse();
            } catch (RenewStreamException e) {
                System.out.println("renew stream!");
                xmlPullParser = xmppFactory.resetXmlPullParser(xmlPullParser, this.reader);
                again = true;
            }
        }
    }
}
