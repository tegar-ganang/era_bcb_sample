package fi.hiit.framework.network.dev;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import jtun.JtunDevice;
import org.savarese.vserv.tcpip.ARPPacket;
import org.savarese.vserv.tcpip.IPPacket;
import fi.hiit.framework.utils.Helpers;

public class DummyDevice implements GenericDevice {

    public static final int MAX_DELEGATES = 3;

    public static final Properties defaultConfiguration = new Properties();

    public DummyDevice() {
    }

    public boolean send(IPPacket packet) {
        byte[] writebuf = new byte[__tun.MAX_PACKET_LENGTH];
        byte[] __full_frame;
        try {
            if (packet != null) {
                packet.getData(writebuf);
                __full_frame = new byte[writebuf.length + 14];
                if (getAppendEther()) __tun.setEthHeader(__full_frame, writebuf, __tun.getMACAddress(), __tun.getMACAddress(), (short) (packet.getIPVersion() == 4 ? 0x0800 : 0x86DD));
                if (__tun.write(__full_frame, 0, __full_frame.length) < packet.size()) {
                    System.out.println("Error sending packet " + packet.size());
                } else {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeNotifier(PacketReceiverDelegate listener) {
        return __listeners.remove(listener);
    }

    public boolean setNotifier(PacketReceiverDelegate listener) {
        if (listener != null) {
            if (__listeners.size() < MAX_DELEGATES) {
                __listeners.add(listener);
                return true;
            }
        }
        return false;
    }

    public void run() {
        byte[] readbuf = new byte[__tun.MAX_PACKET_LENGTH];
        byte[] writebuf = new byte[__tun.MAX_PACKET_LENGTH];
        byte[] no_eth_frame;
        byte[] src4 = new byte[4];
        byte[] src6 = new byte[16];
        byte[] dst4 = new byte[4];
        byte[] dst6 = new byte[16];
        byte[] mac = new byte[6];
        byte[] __full_frame;
        ARPPacket __arp = new ARPPacket(1);
        int readNum = 0;
        int writeNum = 0;
        while (true) {
            try {
                if ((readNum = __tun.read(readbuf)) > 0) {
                    readbuf = Helpers.toUnsignedByteArray(readbuf);
                    no_eth_frame = new byte[readNum - JtunDevice.ETH_HDR_LENGTH];
                    System.arraycopy(readbuf, JtunDevice.ETH_HDR_LENGTH, no_eth_frame, 0, readNum - JtunDevice.ETH_HDR_LENGTH);
                    if (__handleARPPackets && JtunDevice.isARPRequest(readbuf)) {
                        System.out.println("ARP packet");
                        __full_frame = new byte[no_eth_frame.length + 14];
                        __arp.setData(no_eth_frame);
                        __arp.getSenderHdwAddr(mac);
                        __arp.setDestHdwAddr(mac);
                        __arp.setOpCode(ARPPacket.ARP_REPLY);
                        __arp.getDestProtoAddr(dst4);
                        __arp.getSourceProtoAddr(src4);
                        __arp.setDestProtoAddr(src4);
                        __arp.setSourceProtoAddr(dst4);
                        if (getAppendEther()) __tun.setEthHeader(__full_frame, no_eth_frame, mac, mac, (short) 0x0806);
                        try {
                            if ((writeNum = __tun.write(__full_frame)) < readNum) {
                                System.out.println("ARP reply not sent");
                            } else {
                                System.out.println("ARP reply sent " + writeNum + " bytes");
                            }
                        } catch (Exception ex) {
                        }
                    } else if (JtunDevice.isIPv4Packet(readbuf) || JtunDevice.isIPv6Packet(readbuf)) {
                        for (int i = 0; i < __listeners.size(); i++) {
                            IPPacket packet = new IPPacket(no_eth_frame.length);
                            packet.setData(no_eth_frame);
                            if (__listeners.get(i).matches(packet, this)) __listeners.get(i).receivePacket(packet, this);
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public boolean open() {
        if (__listeners.size() == 0) return false;
        __tun = JtunDevice.getTunDeviceInstance();
        __tun.setUseSelectTimeout(false);
        System.out.println("Timeout not set");
        if (!__tun.open()) {
            System.out.println("Device not ready");
            return false;
        }
        __runner = new Thread(this);
        __runner.start();
        return true;
    }

    public boolean close() {
        return __tun.close();
    }

    public boolean getAppendEther() {
        return __appendEthernetHeader;
    }

    public boolean getSupportARP() {
        return __handleARPPackets;
    }

    public int getDefaultMTU() {
        return __defaultMTU;
    }

    public void setAppendEther(boolean append) {
        __appendEthernetHeader = append;
    }

    public void setSupportARP(boolean support) {
        __handleARPPackets = support;
    }

    public void setDefaultMTU(int mtu) {
        __defaultMTU = mtu;
        if (__tun.setMTU(__defaultMTU)) __mtuConfigured = true;
    }

    public ArrayList<DeviceAddress> getDeviceAddrs() {
        return __addressList;
    }

    public void setDeviceAddrs(DeviceAddress addr) {
        if (__tun.addAddress(addr.getInetAddress(), addr.getPrefix())) {
            __addressList.add(addr);
            __addressConfigured = true;
        }
    }

    public boolean isConfigured() {
        return (__mtuConfigured && __addressConfigured);
    }

    private ConcurrentLinkedQueue<IPPacket> __sendQueue = new ConcurrentLinkedQueue<IPPacket>();

    private ArrayList<DeviceAddress> __addressList = new ArrayList<DeviceAddress>();

    private ArrayList<PacketReceiverDelegate> __listeners = new ArrayList<PacketReceiverDelegate>();

    private JtunDevice __tun = null;

    private boolean __handleARPPackets = true;

    private boolean __appendEthernetHeader = true;

    private int __defaultMTU = 1460;

    private boolean __mtuConfigured = false;

    private boolean __addressConfigured = false;

    private Thread __runner = null;

    public static class DeviceAddress {

        public DeviceAddress(InetAddress addr, int prefix) {
            if (addr instanceof Inet4Address) __isIPv4 = true;
            if (addr instanceof Inet6Address) __isIPv4 = false;
            __address = addr.getHostAddress();
            __inetAddr = addr;
            __prefix = prefix;
        }

        public InetAddress getInetAddress() {
            return __inetAddr;
        }

        public String getAddress() {
            return __address;
        }

        public int getPerfixLength() {
            return __prefix;
        }

        public boolean isIPv4() {
            return __isIPv4;
        }

        public boolean isIPv6() {
            return !__isIPv4;
        }

        public int getPrefix() {
            return __prefix;
        }

        private String __address = "";

        private int __prefix = 0;

        private boolean __isIPv4 = true;

        private InetAddress __inetAddr = null;
    }
}
