import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import org.javasock.*;

public class JSTest {

    private static void printHexByte(int b) {
        String hex = Integer.toHexString(b & 0xff);
        if (hex.length() == 1) {
            System.out.print('0');
        }
        System.out.print(hex);
    }

    public static void printPacket(String prefix, java.nio.ByteBuffer packet) {
        System.out.print(prefix);
        System.out.print(": ");
        System.out.print(packet.remaining());
        System.out.println(" bytes received:");
        if (byteLimit != -1) {
            packet = packet.duplicate();
            packet.limit(Math.min(packet.position() + byteLimit, packet.limit()));
        }
        for (int ii = packet.position(); ii < packet.limit(); ii += 0x10) {
            int iii;
            for (iii = 0; iii < 0x10 && iii + ii < packet.limit(); ++iii) {
                printHexByte(packet.get(ii + iii));
                System.out.print(' ');
            }
            for (; iii < 0x10; ++iii) System.out.print("   ");
            System.out.print("   ");
            for (iii = 0; iii < 0x10 && iii + ii < packet.limit(); ++iii) {
                byte b = packet.get(ii + iii);
                if (b >= ' ' && b <= '~') System.out.print((char) b); else System.out.print(' ');
            }
            System.out.println();
        }
    }

    private static void showMac(byte[] macAddr) {
        printHexByte(macAddr[0]);
        for (int ii = 1; ii < macAddr.length; ++ii) {
            System.out.print(':');
            printHexByte(macAddr[ii]);
        }
    }

    /**
	  * @return next argument to check, minus one.
	  */
    private static int extractFromTo(String[] args, int start, String[] out) {
        int ii = start;
        out[0] = out[1] = null;
        if (ii < args.length && args[ii].charAt(0) != '-') {
            if (!"null".equals(args[ii])) out[0] = args[ii];
            ++ii;
            if (ii < args.length && args[ii].charAt(0) != '-') {
                if (!"null".equals(args[ii])) out[1] = args[ii];
                ++ii;
            }
        }
        return ii - 1;
    }

    private static class MyPacketListener implements PacketListener, Serializable {

        MyPacketListener(String prefix) {
            this.prefix = prefix;
        }

        public void packetReceived(PacketHandler handler, java.nio.ByteBuffer data, java.util.List chain, PacketStatistics ps) {
            printPacket(prefix, data);
        }

        private final String prefix;
    }

    private static class DumpPacketListener implements PacketListener {

        DumpPacketListener(String fnamePrefix) {
            this.fnamePrefix = fnamePrefix;
        }

        public void packetReceived(PacketHandler handler, java.nio.ByteBuffer data, java.util.List chain, PacketStatistics ps) {
            try {
                String fname = fnamePrefix + "." + countFormat.format(count++) + ".dat";
                FileOutputStream fos = new FileOutputStream(fname);
                int sz = data.remaining();
                data.get(buffer, 0, sz);
                fos.write(buffer, 0, sz);
                fos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        private int count = 0;

        private final String fnamePrefix;

        private final java.text.Format countFormat = new java.text.DecimalFormat("000");

        private final byte[] buffer = new byte[2048];
    }

    public static void listDevices(OSIDataLinkDevice[] oslds) throws Exception {
        for (int ii = 0; ii < oslds.length; ++ii) {
            System.out.println("" + ii + ": " + oslds[ii]);
            if (showXML) System.out.println(oslds[ii].getXMLDescription());
        }
    }

    public static void showHelp() {
        System.err.println("JSTest options\n\nwhere options are one of:\n" + "\t-dev n|file           connect to device n or to best match\n" + "\t                          for XML file\n" + "\t-l                    list devices and exit\n" + "\t-xml                  prints the device's XML description\n" + "\t-ip [from] [to]       add IP handler with optional filter\n" + "\t                      use 'null' to accept any\n" + "\t-udp [port-from] [to] add UDP handler\n" + "\t-tcp [port-from] [to] add TCP handler\n" + "\t-icmp                 add ICMP handler\n" + "\t-arp                  add ARP handler\n" + "\t-raw                  show raw frames\n" + "\t-n bytes              print only n bytes\n" + "\t-dump prefix          dump packets to file prefix.###.dat\n" + "\t-write fname          write out raw bytes in file\n");
    }

    public static void addHandlers(IPHandler iph, String prefix) {
        if (addTcp) {
            if (tcpFilters.size() == 0) {
                TCPHandler tcph = new TCPHandler(iph);
                tcph.addPacketListener(new MyPacketListener("TCP Packet"));
            } else for (Iterator iter = tcpFilters.iterator(); iter.hasNext(); ) {
                TCPHandler tcph = new TCPHandler(iph);
                TCPFilter tcpf = (TCPFilter) iter.next();
                tcph.addFilter(tcpf);
                tcph.addPacketListener(new MyPacketListener(tcpf.toString()));
            }
        }
        if (addUdp) {
            if (udpFilters.size() == 0) {
                UDPHandler udph = new UDPHandler(iph);
                udph.addPacketListener(new MyPacketListener("UDP Packet"));
            } else for (Iterator iter = udpFilters.iterator(); iter.hasNext(); ) {
                UDPHandler udph = new UDPHandler(iph);
                UDPFilter udpf = (UDPFilter) iter.next();
                udph.addFilter(udpf);
                udph.addPacketListener(new MyPacketListener(udpf.toString()));
            }
        }
        if (addIcmp) {
            ICMPHandler icmp = new ICMPHandler(iph);
            icmp.addPacketListener(new MyPacketListener("ICMP Packet"));
        }
        {
            iph.addPacketListener(new MyPacketListener(prefix));
        }
    }

    public static void main(String args[]) throws Exception {
        int device = 0;
        byte[] writeArray = null;
        String dumpPrefix = null;
        OSIDataLinkDevice[] devices = OSIDataLinkDevice.getDevices();
        try {
            for (int ii = 0; ii < args.length; ++ii) {
                if ("-dev".equals(args[ii])) {
                    String devStr = args[++ii];
                    try {
                        device = Integer.parseInt(devStr);
                    } catch (NumberFormatException nfe) {
                        FileInputStream fis = new FileInputStream(devStr);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int ch;
                        while ((ch = fis.read()) != -1) baos.write(ch);
                        device = OSIDataLinkDevice.findBestMatch(devices, new String(baos.toByteArray()));
                    }
                } else if ("-l".equals(args[ii])) {
                    listDev = true;
                } else if ("-xml".equals(args[ii])) {
                    showXML = true;
                } else if ("-ip".equals(args[ii])) {
                    addIp = true;
                    String[] fromTo = new String[2];
                    ii = extractFromTo(args, ii + 1, fromTo);
                    if (fromTo[0] != null || fromTo[1] != null) {
                        InetAddress fromIP = null, toIP = null;
                        if (fromTo[0] != null) fromIP = InetAddress.getByName(fromTo[0]);
                        if (fromTo[1] != null) toIP = InetAddress.getByName(fromTo[1]);
                        ipFilters.add(new IPFromToFilter(fromIP, toIP));
                    }
                } else if ("-udp".equals(args[ii])) {
                    addIp = addUdp = true;
                    String[] fromTo = new String[2];
                    ii = extractFromTo(args, ii + 1, fromTo);
                    if (fromTo[0] != null || fromTo[1] != null) {
                        udpFilters.add(new UDPPortFilter(fromTo[0] != null ? Short.parseShort(fromTo[0]) : UDPPortFilter.UNSPECIFIED_PORT, fromTo[1] != null ? Short.parseShort(fromTo[1]) : UDPPortFilter.UNSPECIFIED_PORT));
                    }
                } else if ("-tcp".equals(args[ii])) {
                    addIp = addTcp = true;
                    String[] fromTo = new String[2];
                    ii = extractFromTo(args, ii + 1, fromTo);
                    if (fromTo[0] != null || fromTo[1] != null) {
                        tcpFilters.add(new TCPPortFilter(fromTo[0] != null ? Short.parseShort(fromTo[0]) : TCPPortFilter.UNSPECIFIED_PORT, fromTo[1] != null ? Short.parseShort(fromTo[1]) : TCPPortFilter.UNSPECIFIED_PORT));
                    }
                } else if ("-icmp".equals(args[ii])) {
                    addIp = addIcmp = true;
                } else if ("-arp".equals(args[ii])) {
                    addArp = true;
                } else if ("-raw".equals(args[ii])) {
                    showRaw = true;
                } else if ("-dump".equals(args[ii])) {
                    dumpPrefix = args[++ii];
                } else if ("-write".equals(args[ii])) {
                    FileInputStream fis = new FileInputStream(args[++ii]);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = fis.read()) != -1) baos.write(ch);
                    writeArray = baos.toByteArray();
                } else if ("-n".equals(args[ii])) {
                    byteLimit = Integer.parseInt(args[++ii]);
                } else {
                    showHelp();
                    return;
                }
            }
        } catch (ArrayIndexOutOfBoundsException aie) {
            aie.printStackTrace();
            System.err.println("Invalid arguments.");
            showHelp();
            return;
        }
        if (listDev) {
            listDevices(devices);
            return;
        }
        OSIDataLinkDevice osld = devices[device];
        devices = null;
        System.out.println(osld);
        if (showXML) System.out.println(osld.getXMLDescription());
        if (addIp) {
            if (ipFilters.size() == 0) {
                IPHandler iph = new IPHandler(osld);
                addHandlers(iph, "IP Packet");
            } else for (Iterator iter = ipFilters.iterator(); iter.hasNext(); ) {
                IPHandler iph = new IPHandler(osld);
                IPFilter f = (IPFilter) iter.next();
                iph.addFilter(f);
                addHandlers(iph, f.toString());
            }
        } else if (addArp) {
            ARPHandler arph = new ARPHandler(osld);
            arph.addPacketListener(new MyPacketListener("ARP Packet"));
        } else showRaw = true;
        if (showRaw) osld.addPacketListener(new MyPacketListener("Raw"));
        if (dumpPrefix != null) osld.addPacketListener(new DumpPacketListener(dumpPrefix));
        osld.startCapture();
        if (writeArray != null) osld.sendPacket(writeArray);
        while (System.in.read() != '\n') ;
        osld.stopCapture();
    }

    private static boolean addIp = false, addTcp = false, addUdp = false, addIcmp = false, addArp = false, showXML = false, listDev = false, showRaw = false;

    private static int byteLimit = -1;

    private static final ArrayList ipFilters = new ArrayList(), udpFilters = new ArrayList(), tcpFilters = new ArrayList();
}
