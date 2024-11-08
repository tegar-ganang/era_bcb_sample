package org.frameworkset.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.frameworkset.mq.SSLHelper;
import org.frameworkset.spi.ApplicationContext;
import org.frameworkset.spi.assemble.ProMap;
import org.frameworkset.spi.remote.RPCAddress;
import org.frameworkset.spi.remote.RPCMessage;
import org.frameworkset.spi.remote.RemoteException;
import org.frameworkset.spi.remote.Target;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * <p>Title: NettyClinentTransport.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2007</p>
 * @Date 2010-4-19 ����11:41:28
 * @author biaoping.yin
 * @version 1.0
 */
public class NettyClinentTransport {

    private String host;

    private RPCAddress local_addr;

    private int port;

    private Channel cc;

    private ClientBootstrap cb;

    private ChannelFactory channelFactory;

    protected ChannelFactory newClientSocketChannelFactory() {
        return new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }

    private static Map<String, NettyClinentTransport> rpcClients = new HashMap<String, NettyClinentTransport>();

    /**
     * У���ַ�Ƿ���Ч
     * @param address
     * @return
     */
    public static boolean validateAddress(RPCAddress address) {
        NettyClinentTransport transport = null;
        try {
            transport = NettyClinentTransport.queryAndCreateClinentTransport(address);
            if (transport.isdummy()) {
                transport.disconnect();
            } else {
                return transport.isConnected();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 
     * @param host
     * @param port
     * @param corr
     * @return
     */
    public static NettyClinentTransport getClinentTransport(String ip, int port, NettyIOHandler corr) {
        RPCAddress address = new RPCAddress(ip, port, null, Target.BROADCAST_TYPE_NETTY);
        return getClinentTransport(address, corr);
    }

    /**
     * 
     * @param host
     * @param port
     * @param corr
     * @param longconnection
     * @return
     */
    public static NettyClinentTransport getClinentTransport(RPCAddress address, ChannelUpstreamHandler corr) {
        {
            String key = address.getIp() + ":" + address.getPort();
            NettyClinentTransport instance = rpcClients.get(key);
            if (instance != null) {
                if (instance.validate()) return instance; else {
                    rpcClients.remove(key);
                    synchronized (rpcClients) {
                        instance = rpcClients.get(key);
                        if (instance != null) return instance;
                        instance = new NettyClinentTransport(address, corr);
                        ApplicationContext.addShutdownHook(new ShutDownNetty());
                        rpcClients.put(key, instance);
                    }
                    return instance;
                }
            }
            synchronized (rpcClients) {
                instance = rpcClients.get(key);
                if (instance != null) return instance;
                instance = new NettyClinentTransport(address, corr);
                ApplicationContext.addShutdownHook(new ShutDownNetty());
                rpcClients.put(key, instance);
            }
            return instance;
        }
    }

    public static NettyClinentTransport queryAndCreateClinentTransport(RPCAddress address) {
        String key = address.getIp() + ":" + address.getPort();
        NettyClinentTransport instance = rpcClients.get(key);
        if (instance != null) return instance; else {
            boolean dummy = true;
            instance = new NettyClinentTransport(address, dummy);
            return instance;
        }
    }

    static class ShutDownNetty implements Runnable {

        public void run() {
            Collection<NettyClinentTransport> ClinentTransports = rpcClients.values();
            if (ClinentTransports != null && ClinentTransports.size() > 0) {
                Iterator<NettyClinentTransport> it = ClinentTransports.iterator();
                NettyClinentTransport t = null;
                while (it.hasNext()) {
                    t = it.next();
                    try {
                        t.disconnect();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private Object object = new Object();

    public RPCAddress buildRPCAddress(Channel session) {
        if (this.local_addr != null) return this.local_addr;
        synchronized (object) {
            if (this.local_addr != null) return this.local_addr;
            InetSocketAddress inet = (InetSocketAddress) session.getLocalAddress();
            local_addr = new RPCAddress(inet.getAddress(), inet.getPort(), null, Target.BROADCAST_TYPE_NETTY);
            return local_addr;
        }
    }

    public RPCAddress getLocalAddress() {
        return local_addr;
    }

    /**
     * If the sender is null, set our own address. We cannot just go ahead and
     * set the address anyway, as we might be sending a message on behalf of
     * someone else ! E.gin case of retransmission, when the original sender has
     * crashed, or in a FLUSH protocol when we have to return all unstable
     * messages with the FLUSH_OK response.
     */
    private void setSourceAddress(RPCMessage msg) {
        if (msg.getSrc() == null) {
            buildRPCAddress(cc);
            msg.setSrc(local_addr);
        }
    }

    private RPCAddress rpcaddress;

    public RPCAddress getRpcaddress() {
        return rpcaddress;
    }

    /**
     * �Ƿ�ʹ�ó�����
     * 
     * @param host
     *            ���ӵ������ַ
     * @param port
     *            ���ӵĶ˿ڵ�ַ
     * @param corr
     *            Э��������
     * @param longconnection
     *            �Ƿ�ʹ�ó�����
     */
    private NettyClinentTransport(RPCAddress rpcaddress, ChannelUpstreamHandler corr) {
        this.host = rpcaddress.getIp();
        this.port = rpcaddress.getPort();
        this.rpcaddress = rpcaddress;
        this.start(corr);
    }

    private boolean dummy = false;

    /**
     * �ǲ���ģ����������true-�ǣ�false-��
     * @return
     */
    public boolean isdummy() {
        return this.dummy;
    }

    public void start(ChannelUpstreamHandler corr) {
        try {
            channelFactory = newClientSocketChannelFactory();
            cb = new ClientBootstrap(channelFactory);
            ProMap commons = ApplicationContext.getApplicationContext().getMapProperty("rpc.protocol.netty.params");
            boolean enablessl = commons.getBoolean("enablessl", false);
            SSLEngine eg = null;
            if (enablessl) {
                eg = buildSSLEngine(true);
            }
            if (eg != null) cb.getPipeline().addFirst("ssl", new SslHandler(eg));
            cb.getPipeline().addLast("decoder", new ObjectDecoder(commons.getInt("maxFramgeLength_", NettyChannelPipelineFactory.maxFramgeLength_)));
            cb.getPipeline().addLast("encoder", new ObjectEncoder(commons.getInt("estimatedLength_", NettyChannelPipelineFactory.estimatedLength_)));
            cb.getPipeline().addLast("handler", corr);
            cb.setOption("connectTimeoutMillis", commons.getInt("connection.timeout", 10) * 1000);
            ChannelFuture ccf = cb.connect(new InetSocketAddress(host, port));
            boolean success = ccf.awaitUninterruptibly().isSuccess();
            if (!success) {
                throw new NettyRunException("can not connect to:" + host + ":" + port);
            }
            cc = ccf.getChannel();
        } catch (Exception e) {
            this.disconnect();
        }
    }

    public static SSLEngine buildSSLEngine(boolean isclient) {
        ProMap commons = ApplicationContext.getApplicationContext().getMapProperty("rpc.protocol.netty.params");
        String name = "rpc.protocol.netty.ssl.client";
        if (isclient) name = "rpc.protocol.netty.ssl.client"; else name = "rpc.protocol.netty.ssl.server";
        try {
            ProMap ssls = null;
            ssls = ApplicationContext.getApplicationContext().getMapProperty(name);
            if (ssls == null) {
                throw new NettyRunException("������sslģʽ�� ����û��ָ��" + name + " ���������ļ�org/frameworkset/spi/manager-rpc-netty.xml�Ƿ���ȷ�����˸ò���");
            }
            String keyStore = ssls.getString("keyStore");
            String keyStorePassword = ssls.getString("keyStorePassword");
            String trustStore = ssls.getString("trustStore");
            String trustStorePassword = ssls.getString("trustStorePassword");
            SSLContext context = SSLHelper.createSSLContext(keyStore, keyStorePassword, trustStore, trustStorePassword);
            SSLEngine sse = context.createSSLEngine();
            sse.setUseClientMode(isclient);
            String[] enabledCipherSuites = (String[]) commons.getObject("enabledCipherSuites", SSLHelper.enabledCipherSuites);
            sse.setEnabledCipherSuites(enabledCipherSuites);
            String[] protocols = (String[]) commons.getObject("enabledProtocols");
            if (protocols != null) sse.setEnabledProtocols(protocols);
            if (!isclient) {
                boolean needClientAuth = commons.getBoolean("needClientAuth", false);
                boolean wantClientAuth = commons.getBoolean("wantClientAuth", false);
                sse.setNeedClientAuth(needClientAuth);
                sse.setWantClientAuth(wantClientAuth);
            }
            return sse;
        } catch (GeneralSecurityException e) {
            throw new NettyRunException("������sslģʽ�� �����ļ�org/frameworkset/spi/manager-rpc-netty.xml�Ƿ���ȷ�����˿ͷ��˵�ssl����" + name + "��", e);
        } catch (IOException e) {
            throw new NettyRunException("������sslģʽ�� �����ļ�org/frameworkset/spi/manager-rpc-netty.xml�Ƿ���ȷ�����˿ͷ��˵�ssl����" + name + "��", e);
        } catch (NettyRunException e) {
            throw e;
        } catch (Exception e) {
            throw new NettyRunException("������sslģʽ�� �����ļ�org/frameworkset/spi/manager-rpc-netty.xml�Ƿ���ȷ�����˿ͷ��˵�ssl����" + name + "��", e);
        }
    }

    public NettyClinentTransport(RPCAddress rpcaddress, boolean dummy) {
        this.host = rpcaddress.getIp();
        this.port = rpcaddress.getPort();
        this.rpcaddress = rpcaddress;
        this.dummy = dummy;
        this.start(new DummyIOHandler());
    }

    public boolean isConnected() {
        return (cc != null && cc.isConnected());
    }

    public void disconnect() {
        if (cc != null) {
            try {
                cc.close().awaitUninterruptibly();
                cc = null;
            } catch (Exception e) {
            }
        }
        try {
            if (channelFactory != null) this.channelFactory.releaseExternalResources();
        } catch (Exception e) {
        }
        try {
            if (cb != null) this.cb.releaseExternalResources();
        } catch (Exception e) {
        }
    }

    public void write(RPCMessage message) {
        boolean active = validate();
        if (!active) throw new RemoteException(message, 0);
        try {
            setSourceAddress(message);
            this.cc.write(message);
        } catch (Exception e) {
            throw new RemoteException(message, e);
        }
    }

    public boolean validate() {
        if (cc == null) return false;
        boolean active = this.cc.isBound() || this.cc.isConnected() || this.cc.isOpen();
        return active;
    }
}
