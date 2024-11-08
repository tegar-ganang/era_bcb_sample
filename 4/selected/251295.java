package org.red5.server.net.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class DebugProxyHandler extends IoHandlerAdapter implements ResourceLoaderAware {

    protected static Log log = LogFactory.getLog(DebugProxyHandler.class.getName());

    private ResourceLoader loader;

    private ProtocolCodecFactory codecFactory = null;

    private InetSocketAddress forward = null;

    private String dumpTo = "./dumps/";

    public void setResourceLoader(ResourceLoader loader) {
        this.loader = loader;
    }

    public void setCodecFactory(ProtocolCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    public void setForward(String forward) {
        int split = forward.indexOf(':');
        String host = forward.substring(0, split);
        int port = Integer.parseInt(forward.substring(split + 1, forward.length()));
        this.forward = new InetSocketAddress(host, port);
    }

    public void setDumpTo(String dumpTo) {
        this.dumpTo = dumpTo;
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        SocketSessionConfig ssc = (SocketSessionConfig) session.getConfig();
        ssc.setTcpNoDelay(true);
        super.sessionOpened(session);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        boolean isClient = session.getRemoteAddress().equals(forward);
        if (log.isDebugEnabled()) {
            log.debug("Is downstream: " + isClient);
            session.setAttribute(ProtocolState.SESSION_KEY, new RTMP(isClient));
            session.getFilterChain().addFirst("protocol", new ProtocolCodecFilter(codecFactory));
        }
        session.getFilterChain().addFirst("proxy", new ProxyFilter(isClient ? "client" : "server"));
        if (true) {
            String fileName = System.currentTimeMillis() + "_" + forward.getHostName() + "_" + forward.getPort() + "_" + (isClient ? "DOWNSTREAM" : "UPSTREAM");
            File headersFile = loader.getResource(dumpTo + fileName + ".cap").getFile();
            headersFile.createNewFile();
            File rawFile = loader.getResource(dumpTo + fileName + ".raw").getFile();
            rawFile.createNewFile();
            FileOutputStream headersFos = new FileOutputStream(headersFile);
            WritableByteChannel headers = headersFos.getChannel();
            FileOutputStream rawFos = new FileOutputStream(rawFile);
            WritableByteChannel raw = rawFos.getChannel();
            ByteBuffer header = ByteBuffer.allocate(1);
            header.put((byte) (isClient ? 0x00 : 0x01));
            header.flip();
            headers.write(header.buf());
            session.getFilterChain().addFirst("dump", new NetworkDumpFilter(headers, raw));
        }
        if (!isClient) {
            log.debug("Connecting..");
            SocketConnector connector = new SocketConnector();
            ConnectFuture future = connector.connect(forward, this);
            future.join();
            if (future.isConnected()) {
                log.debug("Connected: " + forward);
                IoSession client = future.getSession();
                client.setAttribute(ProxyFilter.FORWARD_KEY, session);
                session.setAttribute(ProxyFilter.FORWARD_KEY, client);
            }
        }
        super.sessionCreated(session);
    }

    @Override
    public void messageReceived(IoSession session, Object in) {
        if (!log.isDebugEnabled()) {
            if (false) {
                if (in instanceof ByteBuffer) {
                    ByteBuffer buf = (ByteBuffer) in;
                    buf.release();
                }
            }
            return;
        }
        if (in instanceof ByteBuffer) {
            log.debug("Handskake");
            return;
        }
        try {
            final Packet packet = (Packet) in;
            final Object message = packet.getMessage();
            final Header source = packet.getHeader();
            log.debug(source);
            log.debug(message);
        } catch (RuntimeException e) {
            log.error("Exception", e);
        }
    }
}
