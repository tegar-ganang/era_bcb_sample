package org.soda.dpws.transport.local;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.stream.XMLStreamReader;
import org.soda.dpws.DPWSException;
import org.soda.dpws.DPWSRuntimeException;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.internal.DPWS;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.registry.ServiceEndpoint;
import org.soda.dpws.transport.AbstractChannel;
import org.soda.dpws.transport.Channel;
import org.soda.dpws.transport.Session;
import org.soda.dpws.util.STAXUtils;
import org.soda.dpws.util.serialize.XMLSerializer;

/**
 * @author pdaum
 * 
 */
public class LocalChannel extends AbstractChannel {

    protected static final String SENDER_URI = "senderUri";

    protected static final String OLD_CONTEXT = "urn:xfire:transport:local:oldContext";

    private final Session session;

    /**
   * @param uri
   * @param transport
   * @param session
   */
    public LocalChannel(String uri, LocalTransport transport, Session session) {
        this.session = session;
        setUri(uri);
        setTransport(transport);
    }

    public void open() {
    }

    public void send(final DPWSContextImpl context, final OutMessage message) throws DPWSException {
        if (message.getUri().equals(Channel.BACKCHANNEL_URI)) {
            final OutputStream out = (OutputStream) context.getProperty(Channel.BACKCHANNEL_URI);
            if (out != null) {
                try {
                    XMLSerializer ser = new XMLSerializer(out, message.getEncoding());
                    message.getSerializer().writeMessage(message, ser, context);
                } catch (UnsupportedEncodingException e) {
                    throw new DPWSException("Couldn't create serializer.", e);
                }
                try {
                    out.close();
                } catch (IOException e) {
                    throw new DPWSException("Couldn't close stream.", e);
                }
            } else {
                DPWSContextImpl oldContext = (DPWSContextImpl) context.getProperty(OLD_CONTEXT);
                sendViaNewChannel(context, oldContext, message, (String) context.getProperty(SENDER_URI));
            }
        } else {
            DPWSContextImpl receivingContext = new DPWSContextImpl();
            receivingContext.setDpws(context.getDpws());
            receivingContext.setService(getService(context.getDpws(), message.getUri()));
            receivingContext.setProperty(OLD_CONTEXT, context);
            receivingContext.setProperty(SENDER_URI, getUri());
            receivingContext.setSession(session);
            sendViaNewChannel(context, receivingContext, message, message.getUri());
        }
    }

    protected ServiceEndpoint getService(DPWS dpws, String uri) throws DPWSException {
        if (null == dpws) {
            return null;
        }
        int i = uri.indexOf("//");
        if (i == -1) {
            throw new DPWSException("Malformed service URI");
        }
        ServiceEndpoint service = null;
        if (null == service) {
        }
        return service;
    }

    private void sendViaNewChannel(final DPWSContextImpl context, final DPWSContextImpl receivingContext, final OutMessage message, final String uri) throws DPWSException {
        try {
            final PipedInputStream stream = new PipedInputStream();
            final PipedOutputStream outStream = new PipedOutputStream(stream);
            final Channel channel;
            try {
                channel = getTransport().createChannel(uri);
            } catch (Exception e) {
                throw new DPWSException("Couldn't create channel.", e);
            }
            Thread writeThread = new Thread(new Runnable() {

                public void run() {
                    try {
                        final XMLSerializer ser = new XMLSerializer(outStream, message.getEncoding());
                        message.getSerializer().writeMessage(message, ser, context);
                        outStream.close();
                    } catch (Exception e) {
                        throw new DPWSRuntimeException("Couldn't write stream.", e);
                    }
                }

                ;
            });
            Thread readThread = new Thread(new Runnable() {

                public void run() {
                    try {
                        final XMLStreamReader reader = STAXUtils.createXMLStreamReader(stream, message.getEncoding(), context);
                        final InMessage inMessage = new InMessage(reader, uri);
                        inMessage.setEncoding(message.getEncoding());
                        channel.receive(receivingContext, inMessage);
                        reader.close();
                        stream.close();
                    } catch (Exception e) {
                        throw new DPWSRuntimeException("Couldn't read stream.", e);
                    }
                }

                ;
            });
            writeThread.start();
            readThread.start();
            try {
                writeThread.join();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            throw new DPWSRuntimeException("Couldn't create stream.", e);
        }
    }

    public void close() {
    }

    public boolean isAsync() {
        return true;
    }

    public void sendEmptyResponse(DPWSContextImpl context) throws DPWSException {
        throw new UnsupportedOperationException();
    }
}
