import org.javasock.*;
import org.javasock.windows.*;

public class RadioTest {

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

    public static class AirPcapListener implements PacketListener {

        public void packetReceived(PacketHandler handler, java.nio.ByteBuffer data, java.util.List chain, PacketStatistics ps) {
            boolean malformed = false;
            printPacket("RAW", data);
            try {
                RadioTapLayer rtl = RadioTapLayer.createFromBytes(data);
                int crc = Layer.crc32(data, data.limit() - 4 - data.position());
                WirelessLanLayer wll = WirelessLanLayer.createFromBytes(data);
                if (wll.getFCS() != crc) malformed = true;
                System.out.println("CRC check: " + Integer.toHexString(wll.getFCS()) + (malformed ? " != " : " == ") + Integer.toHexString(crc));
                System.out.println(rtl);
                System.out.println(wll);
            } catch (java.nio.BufferUnderflowException bufe) {
                malformed = true;
            } catch (IllegalArgumentException iae) {
                malformed = true;
            }
            if (malformed) {
                System.err.println("Malformed packet!");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        OSIDataLinkDevice[] devices = OSIDataLinkDevice.getDevices();
        AirPcapDevice airDev = null;
        for (int ii = 0; ii < devices.length; ++ii) if (devices[ii] instanceof AirPcapDevice) {
            airDev = (AirPcapDevice) devices[ii];
            break;
        }
        if (airDev == null) {
            System.err.println("No AirPcap devices found!");
            return;
        }
        if (args.length > 0) {
            airDev.setChannel(Integer.parseInt(args[0]));
        }
        System.out.println("" + airDev + " channel " + airDev.getChannel());
        airDev.addPacketListener(new AirPcapListener());
        try {
            airDev.startCapture();
            System.in.read();
        } finally {
            airDev.stopCapture();
        }
    }

    static int byteLimit = -1;
}
