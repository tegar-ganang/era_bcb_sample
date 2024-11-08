package org.frameworkset.spi.remote;

import java.net.UnknownHostException;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.frameworkset.spi.BaseSPIManager;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.stack.IpAddress;

/**
 * 
 * <p>
 * Title: JGroupHelper.java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * bboss workgroup
 * </p>
 * <p>
 * Copyright (c) 2007
 * </p>
 * 
 * @Date Apr 24, 2009 10:49:22 PM
 * @author biaoping.yin
 * @version 1.0
 */
public class JGroupHelper {

    private static final Logger log = Logger.getLogger(JGroupHelper.class);

    RpcDispatcher remoteDispatcher;

    Channel channel;

    JGroupConfig JGroupConfig;

    String clusterName = "Cluster";

    public String getClusterName() {
        return clusterName;
    }

    String REMOTE_CLUSTER_NAME = "REMOTE." + clusterName;

    boolean clusterstarted = false;

    boolean inited = false;

    private static JGroupHelper JGroupHelper;

    public static JGroupHelper getJGroupHelper() {
        if (JGroupHelper == null) {
            synchronized (JGroupHelper.class) {
                if (JGroupHelper != null) return JGroupHelper;
                JGroupHelper = new JGroupHelper(Util.clusterName);
                return JGroupHelper;
            }
        } else {
            return JGroupHelper;
        }
    }

    private JGroupHelper(String clusterName) {
        this.clusterName = clusterName;
        this.REMOTE_CLUSTER_NAME = "REMOTE." + clusterName;
    }

    private void assertStarted() {
        if (!this.inited) throw new RuntimeException("JGroup protocol û��������");
    }

    public Channel getChannel() {
        assertStarted();
        return channel;
    }

    public boolean clusterstarted() {
        return this.clusterstarted;
    }

    public RpcDispatcher getRpcDispatcher() {
        assertStarted();
        return this.remoteDispatcher;
    }

    /**
     * ��ȡ��Ⱥ/��ʵ�������������Ϣ
     * 
     * @return
     */
    public Vector<Address> getAppservers() {
        if (clusterstarted()) {
            return channel.getView().getMembers();
        }
        return new Vector<Address>();
    }

    public Address getAddress(String address) {
        Vector<Address> addresses = this.getAppservers();
        for (int i = 0; i < addresses.size(); i++) {
            Address address_ = addresses.get(i);
            if (address_.toString().equals(address)) return address_;
        }
        return null;
    }

    public Address getLocalAddress() {
        assertStarted();
        if (clusterstarted()) {
            return channel.getAddress();
        }
        return null;
    }

    public Address getPhysicalAddress(Address uuid) {
        assertStarted();
        if (clusterstarted()) {
            return channel.getLocalPhysicalAddress(uuid);
        }
        return null;
    }

    public static Vector buildAddresses(String ip, int port) throws UnknownHostException {
        IpAddress address = new IpAddress(ip, port);
        Vector dests = new Vector();
        dests.add(address);
        return dests;
    }

    public void start() {
        if (inited) {
            return;
        }
        synchronized (this) {
            if (inited) {
                return;
            }
            try {
                JGroupConfig = new JGroupConfig();
                PropertyConfigurator config = new PropertyConfigurator();
                String cluster_protocol_configfile = Util.getProtocolConfigFile();
                log.info("Load jgroup[" + REMOTE_CLUSTER_NAME + "] config from  [" + cluster_protocol_configfile + "]");
                config.configure(JGroupConfig, cluster_protocol_configfile);
                log.info("REMOTE_CLUSTER_NAME = " + REMOTE_CLUSTER_NAME);
                log.info("Start remote service begin.");
                channel = new JChannel(JGroupConfig.getClusterProperties());
                channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
                DefaultRemoteHandler remoteHander = (DefaultRemoteHandler) BaseSPIManager.getBeanObject("rpc.server_object");
                remoteDispatcher = new RpcDispatcher(channel, null, null, remoteHander);
                channel.connect(REMOTE_CLUSTER_NAME);
                clusterstarted = true;
                log.info("Start remote service successed.");
                BaseSPIManager.addShutdownHook(new ShutDownJGroup(this));
            } catch (ChannelException e) {
                log.error("Start remote service failed.", e);
            } catch (ConfigureException e) {
                log.error("Start remote service failed.", e);
            } catch (Exception e) {
                log.error("Start remote service failed.", e);
            }
            this.inited = true;
        }
    }

    public void stop() {
        try {
            if (!clusterstarted()) return;
            log.info("Shutdown remoteDispatcher begin.");
            if (remoteDispatcher != null) {
                remoteDispatcher.stop();
            }
            log.info("Shutdown remoteDispatcher complete.");
        } catch (Exception e) {
            log.error("Shutdown remoteDispatcher failed.");
        }
        try {
            if (channel != null) {
                log.info("Shutdown channel begin.");
                channel.close();
                log.info("Shutdown channel complete.");
            }
        } catch (Exception e) {
            log.error("Shutdown channel failed.", e);
        }
    }

    public static class ShutDownJGroup implements Runnable {

        JGroupHelper jgroup;

        public ShutDownJGroup(JGroupHelper jgroup) {
            this.jgroup = jgroup;
        }

        public void run() {
            if (jgroup != null) jgroup.stop();
        }
    }

    public boolean validateAddress(Object address) {
        RPCAddress temp = null;
        IpAddress ip = null;
        if (address instanceof RPCAddress) {
            temp = (RPCAddress) address;
            ip = (IpAddress) temp.getOrigineAddress();
        } else {
            ip = (IpAddress) address;
        }
        if (ip != null) {
            Vector<Address> servers = getAppservers();
            for (int i = 0; i < servers.size(); i++) {
                Address ipAddress = servers.get(0);
                if (ipAddress.equals(ip)) return true;
            }
            return false;
        } else if (temp != null) {
            Vector<Address> servers = getAppservers();
            for (int i = 0; i < servers.size(); i++) {
                IpAddress ipAddress = (IpAddress) servers.get(0);
                if (ipAddress.getIpAddress().getHostAddress().equals(temp.getIp()) && ipAddress.getPort() == temp.getPort()) return true;
            }
            return false;
        }
        return false;
    }

    public Vector getMembers() {
        this.assertStarted();
        return getChannel().getView().getMembers();
    }
}
