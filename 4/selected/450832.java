package eu.popeye.network;

import java.net.UnknownHostException;
import java.util.Vector;
import org.jgroups.*;
import org.jgroups.protocols.ClusterHeader;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.jgroups.structures.ClusterStruct;

/**
 *
 * @author micky
 */
public class Prueba1SuperPeer implements ChannelListener {

    JChannel canal = null;

    JChannel canal1 = null;

    JChannel[] canal_WG = new JChannel[10];

    boolean entra = false;

    /** Creates a new instance of Prueba1SuperPeer */
    public Prueba1SuperPeer() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.library.path", "/home/micky/POPEYEv1/");
        String props = "UDP(mcast_addr=228.8.8.8;ip_ttl=32):" + "DRM:" + "PDS(speer=true;mcast=228.8.8.8)";
        String props1 = "UDP(mcast_addr=224.0.0.8;ip_ttl=32):" + "DRM:" + "PDS(speer=true;mcast=224.0.0.8)";
        try {
            canal = new JChannel(props);
            canal1 = new JChannel(props1);
            Protocol udp = canal.getProtocolStack().findProtocol("UDP");
            udp.setWarn(false);
            Protocol udp1 = canal1.getProtocolStack().findProtocol("UDP");
            udp1.setWarn(false);
            canal.addChannelListener(this);
            canal1.addChannelListener(this);
            canal.setOpt(Channel.AUTO_RECONNECT, new Boolean("true"));
            canal1.setOpt(Channel.AUTO_RECONNECT, new Boolean("true"));
            canal1.connect("SuperPeer");
            canal.connect("MANET");
            System.out.println("local_addr canal: " + canal.getLocalAddress().toString());
            System.out.println("local_addr canal1: " + canal1.getLocalAddress().toString());
        } catch (ChannelException ex) {
            ex.printStackTrace();
            System.out.println("Falla " + ex.toString());
        }
    }

    public void channelConnected(Channel channel) {
        final Channel c = channel;
        new Thread() {

            public void run() {
                if (c.getChannelName().equals("SuperPeer")) bucle1(); else if (c.getChannelName().equals("MANET")) bucle();
            }
        }.start();
    }

    public void channelDisconnected(Channel channel) {
    }

    public void channelClosed(Channel channel) {
    }

    public void channelShunned() {
    }

    public void channelReconnected(Address addr) {
        System.out.println("Reconnected");
    }

    public static void main(String[] args) {
        new Prueba1SuperPeer();
    }

    public void bucle1() {
        boolean fl = true;
        Object tmp;
        Message msg;
        while (fl) {
            try {
                tmp = canal1.receive(0);
                if (tmp == null) continue;
                if (!(tmp instanceof Message)) continue;
                msg = (Message) tmp;
                ClusterHeader head = (ClusterHeader) msg.getHeader("Cluster");
                if (head != null) {
                    if (canal.isConnected()) canal.send(msg);
                }
            } catch (ChannelNotConnectedException not) {
                break;
            } catch (ChannelClosedException closed) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void bucle() {
        boolean fl = true;
        Object tmp;
        Message msg;
        while (fl) {
            try {
                tmp = canal.receive(0);
                if (tmp == null) continue;
                if (!(tmp instanceof Message)) continue;
                msg = (Message) tmp;
                ClusterHeader head = (ClusterHeader) msg.getHeader("Cluster");
                if (head != null) {
                    if (canal1.isConnected()) canal1.send(msg);
                }
            } catch (ChannelNotConnectedException not) {
                break;
            } catch (ChannelClosedException closed) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
