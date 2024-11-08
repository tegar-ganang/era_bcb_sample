package net.propero.rdp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import net.propero.rdp.crypto.CryptoException;
import net.propero.rdp.rdp5.VChannels;
import net.propero.rdp_src1_4.RdpPacket_Localised;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import com.zhai.RdpClient.Bitmaps;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.widget.Toast;

public class Rdp {

    public static int RDP5_DISABLE_NOTHING = 0x00;

    public static int RDP5_NO_WALLPAPER = 0x01;

    public static int RDP5_NO_FULLWINDOWDRAG = 0x02;

    public static int RDP5_NO_MENUANIMATIONS = 0x04;

    public static int RDP5_NO_THEMING = 0x08;

    public static int RDP5_NO_CURSOR_SHADOW = 0x20;

    public static int RDP5_NO_CURSORSETTINGS = 0x40;

    protected static Logger logger = Logger.getLogger(Rdp.class);

    public static final int RDP_LOGON_NORMAL = 0x33;

    public static final int RDP_LOGON_AUTO = 0x8;

    public static final int RDP_LOGON_BLOB = 0x100;

    private static final int RDP_PDU_DEMAND_ACTIVE = 1;

    private static final int RDP_PDU_CONFIRM_ACTIVE = 3;

    private static final int RDP_PDU_DEACTIVATE = 6;

    private static final int RDP_PDU_DATA = 7;

    private static final int RDP_DATA_PDU_UPDATE = 2;

    private static final int RDP_DATA_PDU_CONTROL = 20;

    private static final int RDP_DATA_PDU_POINTER = 27;

    private static final int RDP_DATA_PDU_INPUT = 28;

    private static final int RDP_DATA_PDU_SYNCHRONISE = 31;

    private static final int RDP_DATA_PDU_BELL = 34;

    private static final int RDP_DATA_PDU_LOGON = 38;

    private static final int RDP_DATA_PDU_FONT2 = 39;

    private static final int RDP_DATA_PDU_DISCONNECT = 47;

    private static final int RDP_CTL_REQUEST_CONTROL = 1;

    private static final int RDP_CTL_GRANT_CONTROL = 2;

    private static final int RDP_CTL_DETACH = 3;

    private static final int RDP_CTL_COOPERATE = 4;

    private static final int RDP_UPDATE_ORDERS = 0;

    private static final int RDP_UPDATE_BITMAP = 1;

    private static final int RDP_UPDATE_PALETTE = 2;

    private static final int RDP_UPDATE_SYNCHRONIZE = 3;

    private static final int RDP_POINTER_SYSTEM = 1;

    private static final int RDP_POINTER_MOVE = 3;

    private static final int RDP_POINTER_COLOR = 6;

    private static final int RDP_POINTER_CACHED = 7;

    private static final int RDP_NULL_POINTER = 0;

    private static final int RDP_DEFAULT_POINTER = 0x7F00;

    private static final int RDP_INPUT_SYNCHRONIZE = 0;

    private static final int RDP_INPUT_CODEPOINT = 1;

    private static final int RDP_INPUT_VIRTKEY = 2;

    private static final int RDP_INPUT_SCANCODE = 4;

    private static final int RDP_INPUT_MOUSE = 0x8001;

    private static final int RDP_CAPSET_GENERAL = 1;

    private static final int RDP_CAPLEN_GENERAL = 0x18;

    private static final int OS_MAJOR_TYPE_UNIX = 4;

    private static final int OS_MINOR_TYPE_XSERVER = 7;

    private static final int RDP_CAPSET_BITMAP = 2;

    private static final int RDP_CAPLEN_BITMAP = 0x1C;

    private static final int RDP_CAPSET_ORDER = 3;

    private static final int RDP_CAPLEN_ORDER = 0x58;

    private static final int ORDER_CAP_NEGOTIATE = 2;

    private static final int ORDER_CAP_NOSUPPORT = 4;

    private static final int RDP_CAPSET_BMPCACHE = 4;

    private static final int RDP_CAPLEN_BMPCACHE = 0x28;

    private static final int RDP_CAPSET_CONTROL = 5;

    private static final int RDP_CAPLEN_CONTROL = 0x0C;

    private static final int RDP_CAPSET_ACTIVATE = 7;

    private static final int RDP_CAPLEN_ACTIVATE = 0x0C;

    private static final int RDP_CAPSET_POINTER = 8;

    private static final int RDP_CAPLEN_POINTER = 0x08;

    private static final int RDP_CAPSET_SHARE = 9;

    private static final int RDP_CAPLEN_SHARE = 0x08;

    private static final int RDP_CAPSET_COLCACHE = 10;

    private static final int RDP_CAPLEN_COLCACHE = 0x08;

    private static final int RDP_CAPSET_UNKNOWN = 13;

    private static final int RDP_CAPLEN_UNKNOWN = 0x9C;

    private static final int RDP_CAPSET_BMPCACHE2 = 19;

    private static final int RDP_CAPLEN_BMPCACHE2 = 0x28;

    private static final int BMPCACHE2_FLAG_PERSIST = (1 << 31);

    public static final int BMPCACHE2_C0_CELLS = 0x78;

    public static final int BMPCACHE2_C1_CELLS = 0x78;

    public static final int BMPCACHE2_C2_CELLS = 0x150;

    public static final int BMPCACHE2_NUM_PSTCELLS = 0x9f6;

    private static final int RDP5_FLAG = 0x0030;

    private static final byte[] RDP_SOURCE = { (byte) 0x4D, (byte) 0x53, (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0x00 };

    protected Secure SecureLayer = null;

    private int next_packet = 0;

    private int rdp_shareid = 0;

    private boolean connected = false;

    private RdpPacket_Localised stream = null;

    private final byte[] canned_caps = { 0x01, 0x00, 0x00, 0x00, 0x09, 0x04, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0E, 0x00, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x10, 0x00, 0x34, 0x00, (byte) 0xfe, 0x00, 0x04, 0x00, (byte) 0xfe, 0x00, 0x04, 0x00, (byte) 0xFE, 0x00, 0x08, 0x00, (byte) 0xFE, 0x00, 0x08, 0x00, (byte) 0xFE, 0x00, 0x10, 0x00, (byte) 0xFE, 0x00, 0x20, 0x00, (byte) 0xFE, 0x00, 0x40, 0x00, (byte) 0xFE, 0x00, (byte) 0x80, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x40, 0x00, 0x00, 0x08, 0x00, 0x01, 0x00, 0x01, 0x02, 0x00, 0x00, 0x00 };

    static byte caps_0x0d[] = { 0x01, 0x00, 0x00, 0x00, 0x09, 0x04, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    static byte caps_0x0c[] = { 0x01, 0x00, 0x00, 0x00 };

    static byte caps_0x0e[] = { 0x01, 0x00, 0x00, 0x00 };

    static byte caps_0x10[] = { (byte) 0xFE, 0x00, 0x04, 0x00, (byte) 0xFE, 0x00, 0x04, 0x00, (byte) 0xFE, 0x00, 0x08, 0x00, (byte) 0xFE, 0x00, 0x08, 0x00, (byte) 0xFE, 0x00, 0x10, 0x00, (byte) 0xFE, 0x00, 0x20, 0x00, (byte) 0xFE, 0x00, 0x40, 0x00, (byte) 0xFE, 0x00, (byte) 0x80, 0x00, (byte) 0xFE, 0x00, 0x00, 0x01, 0x40, 0x00, 0x00, 0x08, 0x00, 0x01, 0x00, 0x01, 0x02, 0x00, 0x00, 0x00 };

    /**
	 * Process a general capability set
	 * 
	 * @param data
	 *            Packet containing capability set data at current read position
	 */
    static void processGeneralCaps(RdpPacket_Localised data) {
        int pad2octetsB;
        data.incrementPosition(10);
        pad2octetsB = data.getLittleEndian16();
        if (pad2octetsB != 0) Options.use_rdp5 = false;
    }

    /**
	 * Process a bitmap capability set
	 * 
	 * @param data
	 *            Packet containing capability set data at current read position
	 */
    static void processBitmapCaps(RdpPacket_Localised data) {
        int width, height, bpp;
        bpp = data.getLittleEndian16();
        data.incrementPosition(6);
        width = data.getLittleEndian16();
        height = data.getLittleEndian16();
        logger.debug("setting desktop size and bpp to: " + width + "x" + height + "x" + bpp);
        if (Options.server_bpp != bpp) {
            logger.warn("colour depth changed from " + Options.server_bpp + " to " + bpp);
            Options.server_bpp = bpp;
        }
        if (Options.width != width || Options.height != height) {
            logger.warn("screen size changed from " + Options.width + "x" + Options.height + " to " + width + "x" + height);
            Options.width = width;
            Options.height = height;
        }
    }

    /**
	 * Process server capabilities
	 * 
	 * @param data
	 *            Packet containing capability set data at current read position
	 */
    void processServerCaps(RdpPacket_Localised data, int length) {
        int n;
        int next, start;
        int ncapsets, capset_type, capset_length;
        start = data.getPosition();
        ncapsets = data.getLittleEndian16();
        data.incrementPosition(2);
        for (n = 0; n < ncapsets; n++) {
            if (data.getPosition() > start + length) return;
            capset_type = data.getLittleEndian16();
            capset_length = data.getLittleEndian16();
            next = data.getPosition() + capset_length - 4;
            switch(capset_type) {
                case RDP_CAPSET_GENERAL:
                    processGeneralCaps(data);
                    break;
                case RDP_CAPSET_BITMAP:
                    processBitmapCaps(data);
                    break;
            }
            data.setPosition(next);
        }
    }

    /**
	 * Process a disconnect PDU
	 * 
	 * @param data
	 *            Packet containing disconnect PDU at current read position
	 * @return Code specifying the reason for disconnection
	 */
    protected int processDisconnectPdu(RdpPacket_Localised data) {
        logger.debug("Received disconnect PDU");
        return data.getLittleEndian32();
    }

    /**
	 * Initialise RDP comms layer, and register virtual channels
	 * 
	 * @param channels
	 *            Virtual channels to be used in connection
	 */
    public Rdp(VChannels channels) {
        this.SecureLayer = new Secure(channels);
        Common.secure = SecureLayer;
    }

    /**
	 * Initialise a packet for sending data on the RDP layer
	 * 
	 * @param size
	 *            Size of RDP data
	 * @return Packet initialised for RDP
	 * @throws RdesktopException
	 */
    private RdpPacket_Localised initData(int size) throws RdesktopException {
        RdpPacket_Localised buffer = null;
        buffer = SecureLayer.init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, size + 18);
        buffer.pushLayer(RdpPacket.RDP_HEADER, 18);
        return buffer;
    }

    /**
	 * Send a packet on the RDP layer
	 * 
	 * @param data
	 *            Packet to send
	 * @param data_pdu_type
	 *            Type of data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
    private void sendData(RdpPacket_Localised data, int data_pdu_type) throws RdesktopException, IOException, CryptoException {
        CommunicationMonitor.lock(this);
        int length;
        data.setPosition(data.getHeader(RdpPacket.RDP_HEADER));
        length = data.getEnd() - data.getPosition();
        data.setLittleEndian16(length);
        data.setLittleEndian16(RDP_PDU_DATA | 0x10);
        data.setLittleEndian16(SecureLayer.getUserID() + 1001);
        data.setLittleEndian32(this.rdp_shareid);
        data.set8(0);
        data.set8(1);
        data.setLittleEndian16(length - 14);
        data.set8(data_pdu_type);
        data.set8(0);
        data.setLittleEndian16(0);
        SecureLayer.send(data, Constants.encryption ? Secure.SEC_ENCRYPT : 0);
        CommunicationMonitor.unlock(this);
    }

    /**
	 * Receive a packet from the RDP layer
	 * 
	 * @param type
	 *            Type of PDU received, stored in type[0]
	 * @return Packet received from RDP layer
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws CryptoException
	 * @throws OrderException
	 */
    private RdpPacket_Localised receive(int[] type) throws IOException, RdesktopException, CryptoException, OrderException {
        int length = 0;
        if ((this.stream == null) || (this.next_packet >= this.stream.getEnd())) {
            this.stream = SecureLayer.receive();
            if (stream == null) return null;
            this.next_packet = this.stream.getPosition();
        } else {
            this.stream.setPosition(this.next_packet);
        }
        length = this.stream.getLittleEndian16();
        if (length == 0x8000) {
            logger.warn("32k packet keepalive fix");
            next_packet += 8;
            type[0] = 0;
            return stream;
        }
        type[0] = this.stream.getLittleEndian16() & 0xf;
        if (stream.getPosition() != stream.getEnd()) {
            stream.incrementPosition(2);
        }
        this.next_packet += length;
        return stream;
    }

    /**
	 * Connect to a server
	 * 
	 * @param username
	 *            Username for log on
	 * @param server
	 *            Server to connect to
	 * @param flags
	 *            Flags defining logon type
	 * @param domain
	 *            Domain for log on
	 * @param password
	 *            Password for log on
	 * @param command
	 *            Alternative shell for session
	 * @param directory
	 *            Initial working directory for connection
	 * @throws ConnectionException
	 */
    public void connect(String username, InetAddress server, int flags, String domain, String password, String command, String directory) throws ConnectionException {
        try {
            SecureLayer.connect(server);
            this.connected = true;
            this.sendLogonInfo(flags, domain, username, password, command, directory);
        } catch (UnknownHostException e) {
            throw new ConnectionException("Could not resolve host name: " + server);
        } catch (ConnectException e) {
            throw new ConnectionException("Connection refused when trying to connect to " + server + " on port " + Options.port);
        } catch (NoRouteToHostException e) {
            throw new ConnectionException("Connection timed out when attempting to connect to " + server);
        } catch (IOException e) {
            throw new ConnectionException("Connection Failed");
        } catch (RdesktopException e) {
            throw new ConnectionException(e.getMessage());
        } catch (OrderException e) {
            throw new ConnectionException(e.getMessage());
        } catch (CryptoException e) {
            throw new ConnectionException(e.getMessage());
        }
    }

    /**
	 * Disconnect from an RDP session
	 */
    public void disconnect() {
        this.connected = false;
        SecureLayer.disconnect();
    }

    /**
	 * Retrieve status of connection
	 * 
	 * @return True if connection to RDP session
	 */
    public boolean isConnected() {
        return this.connected;
    }

    boolean deactivated;

    int ext_disc_reason;

    /**
	 * RDP receive loop
	 * 
	 * @param deactivated
	 *            On return, stores true in deactivated[0] if the session
	 *            disconnected cleanly
	 * @param ext_disc_reason
	 *            On return, stores the reason for disconnection in
	 *            ext_disc_reason[0]
	 * @throws IOException
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
    public void mainLoop(boolean[] deactivated, int[] ext_disc_reason) throws IOException, RdesktopException, OrderException, CryptoException {
        int[] type = new int[1];
        boolean disc = false;
        boolean cont = true;
        RdpPacket_Localised data = null;
        while (cont) {
            try {
                data = this.receive(type);
                if (data == null) return;
            } catch (EOFException e) {
                return;
            }
            switch(type[0]) {
                case (Rdp.RDP_PDU_DEMAND_ACTIVE):
                    logger.debug("Rdp.RDP_PDU_DEMAND_ACTIVE");
                    this.processDemandActive(data);
                    deactivated[0] = false;
                    break;
                case (Rdp.RDP_PDU_DEACTIVATE):
                    deactivated[0] = true;
                    this.stream = null;
                    break;
                case (Rdp.RDP_PDU_DATA):
                    disc = this.processData(data, ext_disc_reason);
                    break;
                case 0:
                    break;
                default:
                    throw new RdesktopException("Unimplemented type in main loop :" + type[0]);
            }
            if (disc) return;
        }
        return;
    }

    /**
	 * Send user logon details to the server
	 * 
	 * @param flags
	 *            Set of flags defining logon type
	 * @param domain
	 *            Domain for logon
	 * @param username
	 *            Username for logon
	 * @param password
	 *            Password for logon
	 * @param command
	 *            Alternative shell for session
	 * @param directory
	 *            Starting working directory for session
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
    private void sendLogonInfo(int flags, String domain, String username, String password, String command, String directory) throws RdesktopException, IOException, CryptoException {
        int len_ip = 2 * "127.0.0.1".length();
        int len_dll = 2 * "C:\\WINNT\\System32\\mstscax.dll".length();
        int packetlen = 0;
        int sec_flags = Constants.encryption ? (Secure.SEC_LOGON_INFO | Secure.SEC_ENCRYPT) : Secure.SEC_LOGON_INFO;
        int domainlen = 2 * domain.length();
        int userlen = 2 * username.length();
        int passlen = 2 * password.length();
        int commandlen = 2 * command.length();
        int dirlen = 2 * directory.length();
        RdpPacket_Localised data;
        if (!Options.use_rdp5 || 1 == Options.server_rdp_version) {
            logger.debug("Sending RDP4-style Logon packet");
            data = SecureLayer.init(sec_flags, 18 + domainlen + userlen + passlen + commandlen + dirlen + 10);
            data.setLittleEndian32(0);
            data.setLittleEndian32(flags);
            data.setLittleEndian16(domainlen);
            data.setLittleEndian16(userlen);
            data.setLittleEndian16(passlen);
            data.setLittleEndian16(commandlen);
            data.setLittleEndian16(dirlen);
            data.outUnicodeString(domain, domainlen);
            data.outUnicodeString(username, userlen);
            data.outUnicodeString(password, passlen);
            data.outUnicodeString(command, commandlen);
            data.outUnicodeString(directory, dirlen);
        } else {
            flags |= RDP_LOGON_BLOB;
            logger.debug("Sending RDP5-style Logon packet");
            packetlen = 4 + 4 + 2 + 2 + ((flags & RDP_LOGON_AUTO) != 0 ? 2 : 0) + ((flags & RDP_LOGON_BLOB) != 0 ? 2 : 0) + 2 + 2 + (0 < domainlen ? domainlen + 2 : 2) + userlen + ((flags & RDP_LOGON_AUTO) != 0 ? passlen : 0) + 0 + ((flags & RDP_LOGON_BLOB) != 0 && (flags & RDP_LOGON_AUTO) == 0 ? 2 : 0) + (0 < commandlen ? commandlen + 2 : 2) + (0 < dirlen ? dirlen + 2 : 2) + 2 + 2 + len_ip + 2 + len_dll + 2 + 2 + 64 + 20 + 64 + 32 + 6;
            data = SecureLayer.init(sec_flags, packetlen);
            data.setLittleEndian32(0);
            data.setLittleEndian32(flags);
            data.setLittleEndian16(domainlen);
            data.setLittleEndian16(userlen);
            if ((flags & RDP_LOGON_AUTO) != 0) {
                data.setLittleEndian16(passlen);
            }
            if ((flags & RDP_LOGON_BLOB) != 0 && ((flags & RDP_LOGON_AUTO) == 0)) {
                data.setLittleEndian16(0);
            }
            data.setLittleEndian16(commandlen);
            data.setLittleEndian16(dirlen);
            if (0 < domainlen) data.outUnicodeString(domain, domainlen); else data.setLittleEndian16(0);
            data.outUnicodeString(username, userlen);
            if ((flags & RDP_LOGON_AUTO) != 0) {
                data.outUnicodeString(password, passlen);
            }
            if ((flags & RDP_LOGON_BLOB) != 0 && (flags & RDP_LOGON_AUTO) == 0) {
                data.setLittleEndian16(0);
            }
            if (0 < commandlen) {
                data.outUnicodeString(command, commandlen);
            } else {
                data.setLittleEndian16(0);
            }
            if (0 < dirlen) {
                data.outUnicodeString(directory, dirlen);
            } else {
                data.setLittleEndian16(0);
            }
            data.setLittleEndian16(2);
            data.setLittleEndian16(len_ip + 2);
            data.outUnicodeString("127.0.0.1", len_ip);
            data.setLittleEndian16(len_dll + 2);
            data.outUnicodeString("C:\\WINNT\\System32\\mstscax.dll", len_dll);
            data.setLittleEndian16(0xffc4);
            data.setLittleEndian16(0xffff);
            data.outUnicodeString("GTB, normaltid", 2 * "GTB, normaltid".length());
            data.incrementPosition(62 - 2 * "GTB, normaltid".length());
            data.setLittleEndian32(0x0a0000);
            data.setLittleEndian32(0x050000);
            data.setLittleEndian32(3);
            data.setLittleEndian32(0);
            data.setLittleEndian32(0);
            data.outUnicodeString("GTB, sommartid", 2 * "GTB, sommartid".length());
            data.incrementPosition(62 - 2 * "GTB, sommartid".length());
            data.setLittleEndian32(0x30000);
            data.setLittleEndian32(0x050000);
            data.setLittleEndian32(2);
            data.setLittleEndian32(0);
            data.setLittleEndian32(0xffffffc4);
            data.setLittleEndian32(0xfffffffe);
            data.setLittleEndian32(Options.rdp5_performanceflags);
            data.setLittleEndian32(0);
        }
        data.markEnd();
        byte[] buffer = new byte[data.getEnd()];
        data.copyToByteArray(buffer, 0, 0, data.getEnd());
        SecureLayer.send(data, sec_flags);
    }

    /**
	 * Process an activation demand from the server (received between licence
	 * negotiation and 1st order)
	 * 
	 * @param data
	 *            Packet containing demand at current read position
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 * @throws OrderException
	 */
    private void processDemandActive(RdpPacket_Localised data) throws RdesktopException, IOException, CryptoException, OrderException {
        int type[] = new int[1];
        this.rdp_shareid = data.getLittleEndian32();
        this.sendConfirmActive();
        this.sendSynchronize();
        this.sendControl(RDP_CTL_COOPERATE);
        this.sendControl(RDP_CTL_REQUEST_CONTROL);
        this.receive(type);
        this.receive(type);
        this.receive(type);
        this.sendInput(0, RDP_INPUT_SYNCHRONIZE, 0, 0, 0);
        this.sendFonts(1);
        this.sendFonts(2);
        this.receive(type);
    }

    /**
	 * Process a data PDU received from the server
	 * 
	 * @param data
	 *            Packet containing data PDU at current read position
	 * @param ext_disc_reason
	 *            If a disconnect PDU is received, stores disconnection reason
	 *            at ext_disc_reason[0]
	 * @return True if disconnect PDU was received
	 * @throws RdesktopException
	 * @throws OrderException
	 */
    private boolean processData(RdpPacket_Localised data, int[] ext_disc_reason) throws RdesktopException, OrderException {
        int data_type, ctype, clen, len, roff, rlen;
        data_type = 0;
        data.incrementPosition(6);
        len = data.getLittleEndian16();
        data_type = data.get8();
        ctype = data.get8();
        clen = data.getLittleEndian16();
        clen -= 18;
        switch(data_type) {
            case (Rdp.RDP_DATA_PDU_UPDATE):
                logger.debug("Rdp.RDP_DATA_PDU_UPDATE");
                this.processUpdate(data);
                break;
            case RDP_DATA_PDU_CONTROL:
                logger.debug(("Received Control PDU\n"));
                break;
            case RDP_DATA_PDU_SYNCHRONISE:
                logger.debug(("Received Sync PDU\n"));
                break;
            case (Rdp.RDP_DATA_PDU_POINTER):
                logger.debug("Received pointer PDU");
                this.processPointer(data);
                break;
            case (Rdp.RDP_DATA_PDU_BELL):
                logger.debug("Received bell PDU");
                break;
            case (Rdp.RDP_DATA_PDU_LOGON):
                logger.debug("User logged on");
                break;
            case RDP_DATA_PDU_DISCONNECT:
                ext_disc_reason[0] = processDisconnectPdu(data);
                logger.debug(("Received disconnect PDU\n"));
                return true;
            default:
                logger.warn("Unimplemented Data PDU type " + data_type);
        }
        return false;
    }

    private void processUpdate(RdpPacket_Localised data) throws OrderException, RdesktopException {
        int update_type = 0;
        update_type = data.getLittleEndian16();
        switch(update_type) {
            case (Rdp.RDP_UPDATE_ORDERS):
                data.incrementPosition(2);
                int n_orders = data.getLittleEndian16();
                data.incrementPosition(2);
                break;
            case (Rdp.RDP_UPDATE_BITMAP):
                this.processBitmapUpdates(data);
                break;
            case (Rdp.RDP_UPDATE_PALETTE):
                this.processPalette(data);
                break;
            case (Rdp.RDP_UPDATE_SYNCHRONIZE):
                break;
            default:
                logger.warn("Unimplemented Update type " + update_type);
        }
    }

    private void sendConfirmActive() throws RdesktopException, IOException, CryptoException {
        int caplen = RDP_CAPLEN_GENERAL + RDP_CAPLEN_BITMAP + RDP_CAPLEN_ORDER + RDP_CAPLEN_BMPCACHE + RDP_CAPLEN_COLCACHE + RDP_CAPLEN_ACTIVATE + RDP_CAPLEN_CONTROL + RDP_CAPLEN_POINTER + RDP_CAPLEN_SHARE + RDP_CAPLEN_UNKNOWN + 4;
        int sec_flags = Options.encryption ? (RDP5_FLAG | Secure.SEC_ENCRYPT) : RDP5_FLAG;
        RdpPacket_Localised data = SecureLayer.init(sec_flags, 6 + 14 + caplen + RDP_SOURCE.length);
        data.setLittleEndian16(2 + 14 + caplen + RDP_SOURCE.length);
        data.setLittleEndian16((RDP_PDU_CONFIRM_ACTIVE | 0x10));
        data.setLittleEndian16(Common.mcs.getUserID() + 1001);
        data.setLittleEndian32(this.rdp_shareid);
        data.setLittleEndian16(0x3ea);
        data.setLittleEndian16(RDP_SOURCE.length);
        data.setLittleEndian16(caplen);
        data.copyFromByteArray(RDP_SOURCE, 0, data.getPosition(), RDP_SOURCE.length);
        data.incrementPosition(RDP_SOURCE.length);
        data.setLittleEndian16(0xd);
        data.incrementPosition(2);
        this.sendGeneralCaps(data);
        this.sendBitmapCaps(data);
        this.sendOrderCaps(data);
        if (Options.use_rdp5 && Options.persistent_bitmap_caching) {
            logger.info("Persistent caching enabled");
            this.sendBitmapcache2Caps(data);
        } else this.sendBitmapcacheCaps(data);
        this.sendColorcacheCaps(data);
        this.sendActivateCaps(data);
        this.sendControlCaps(data);
        this.sendPointerCaps(data);
        this.sendShareCaps(data);
        this.sendUnknownCaps(data, 0x0d, 0x58, caps_0x0d);
        this.sendUnknownCaps(data, 0x0c, 0x08, caps_0x0c);
        this.sendUnknownCaps(data, 0x0e, 0x08, caps_0x0e);
        this.sendUnknownCaps(data, 0x10, 0x34, caps_0x10);
        data.markEnd();
        logger.debug("confirm active");
        Common.secure.send(data, sec_flags);
    }

    private void sendGeneralCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_GENERAL);
        data.setLittleEndian16(RDP_CAPLEN_GENERAL);
        data.setLittleEndian16(1);
        data.setLittleEndian16(3);
        data.setLittleEndian16(0x200);
        data.setLittleEndian16(Options.use_rdp5 ? 0x40d : 0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
    }

    private void sendBitmapCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_BITMAP);
        data.setLittleEndian16(RDP_CAPLEN_BITMAP);
        data.setLittleEndian16(Options.server_bpp);
        data.setLittleEndian16(1);
        data.setLittleEndian16(1);
        data.setLittleEndian16(1);
        data.setLittleEndian16(Options.width);
        data.setLittleEndian16(Options.height);
        data.setLittleEndian16(0);
        data.setLittleEndian16(1);
        data.setLittleEndian16(Options.bitmap_compression ? 1 : 0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(1);
        data.setLittleEndian16(0);
    }

    private void sendOrderCaps(RdpPacket_Localised data) {
        byte[] order_caps = new byte[32];
        order_caps[0] = 1;
        order_caps[1] = 1;
        order_caps[2] = 1;
        order_caps[3] = (byte) (Options.bitmap_caching ? 1 : 0);
        order_caps[4] = 0;
        order_caps[8] = 1;
        order_caps[9] = 1;
        order_caps[10] = 1;
        order_caps[11] = (Constants.desktop_save ? 1 : 0);
        order_caps[13] = 1;
        order_caps[14] = 1;
        order_caps[20] = (byte) (Options.polygon_ellipse_orders ? 1 : 0);
        order_caps[21] = (byte) (Options.polygon_ellipse_orders ? 1 : 0);
        order_caps[22] = 1;
        order_caps[25] = (byte) (Options.polygon_ellipse_orders ? 1 : 0);
        order_caps[26] = (byte) (Options.polygon_ellipse_orders ? 1 : 0);
        order_caps[27] = 1;
        data.setLittleEndian16(RDP_CAPSET_ORDER);
        data.setLittleEndian16(RDP_CAPLEN_ORDER);
        data.incrementPosition(20);
        data.setLittleEndian16(1);
        data.setLittleEndian16(20);
        data.setLittleEndian16(0);
        data.setLittleEndian16(1);
        data.setLittleEndian16(0x147);
        data.setLittleEndian16(0x2a);
        data.copyFromByteArray(order_caps, 0, data.getPosition(), 32);
        data.incrementPosition(32);
        data.setLittleEndian16(0x6a1);
        data.incrementPosition(6);
        data.setLittleEndian32(Constants.desktop_save ? 0x38400 : 0);
        data.setLittleEndian32(0);
        data.setLittleEndian32(0x4e4);
    }

    private void sendBitmapcacheCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_BMPCACHE);
        data.setLittleEndian16(RDP_CAPLEN_BMPCACHE);
        data.incrementPosition(24);
        data.setLittleEndian16(0x258);
        data.setLittleEndian16(0x100);
        data.setLittleEndian16(0x12c);
        data.setLittleEndian16(0x400);
        data.setLittleEndian16(0x106);
        data.setLittleEndian16(0x1000);
    }

    private void sendBitmapcache2Caps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_BMPCACHE2);
        data.setLittleEndian16(RDP_CAPLEN_BMPCACHE2);
        data.setLittleEndian16(Options.persistent_bitmap_caching ? 2 : 0);
        data.setBigEndian16(3);
        data.setLittleEndian32(BMPCACHE2_C0_CELLS);
        data.setLittleEndian32(BMPCACHE2_C1_CELLS);
        if (PstCache.pstcache_init(2)) {
            logger.info("Persistent cache initialized");
            data.setLittleEndian32(BMPCACHE2_NUM_PSTCELLS | BMPCACHE2_FLAG_PERSIST);
        } else {
            logger.info("Persistent cache not initialized");
            data.setLittleEndian32(BMPCACHE2_C2_CELLS);
        }
        data.incrementPosition(20);
    }

    private void sendColorcacheCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_COLCACHE);
        data.setLittleEndian16(RDP_CAPLEN_COLCACHE);
        data.setLittleEndian16(6);
        data.setLittleEndian16(0);
    }

    private void sendActivateCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_ACTIVATE);
        data.setLittleEndian16(RDP_CAPLEN_ACTIVATE);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
    }

    private void sendControlCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_CONTROL);
        data.setLittleEndian16(RDP_CAPLEN_CONTROL);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(2);
        data.setLittleEndian16(2);
    }

    private void sendPointerCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_POINTER);
        data.setLittleEndian16(RDP_CAPLEN_POINTER);
        data.setLittleEndian16(0);
        data.setLittleEndian16(20);
    }

    private void sendShareCaps(RdpPacket_Localised data) {
        data.setLittleEndian16(RDP_CAPSET_SHARE);
        data.setLittleEndian16(RDP_CAPLEN_SHARE);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
    }

    private void sendUnknownCaps(RdpPacket_Localised data, int id, int length, byte[] caps) {
        data.setLittleEndian16(id);
        data.setLittleEndian16(length);
        data.copyFromByteArray(caps, 0, data.getPosition(), length - 4);
        data.incrementPosition(length - 4);
    }

    private void sendSynchronize() throws RdesktopException, IOException, CryptoException {
        RdpPacket_Localised data = this.initData(4);
        data.setLittleEndian16(1);
        data.setLittleEndian16(1002);
        data.markEnd();
        logger.debug("sync");
        this.sendData(data, RDP_DATA_PDU_SYNCHRONISE);
    }

    private void sendControl(int action) throws RdesktopException, IOException, CryptoException {
        RdpPacket_Localised data = this.initData(8);
        data.setLittleEndian16(action);
        data.setLittleEndian16(0);
        data.setLittleEndian32(0);
        data.markEnd();
        logger.debug("control");
        this.sendData(data, RDP_DATA_PDU_CONTROL);
    }

    public void sendInput(int time, int message_type, int device_flags, int param1, int param2) {
        RdpPacket_Localised data = null;
        try {
            data = this.initData(16);
        } catch (RdesktopException e) {
        }
        data.setLittleEndian16(1);
        data.setLittleEndian16(0);
        data.setLittleEndian32(time);
        data.setLittleEndian16(message_type);
        data.setLittleEndian16(device_flags);
        data.setLittleEndian16(param1);
        data.setLittleEndian16(param2);
        data.markEnd();
        try {
            this.sendData(data, RDP_DATA_PDU_INPUT);
        } catch (RdesktopException r) {
            if (Common.rdp.isConnected()) Common.exit();
        } catch (CryptoException c) {
            if (Common.rdp.isConnected()) Common.exit();
        } catch (IOException i) {
            if (Common.rdp.isConnected()) Common.exit();
        }
    }

    private void sendFonts(int seq) throws RdesktopException, IOException, CryptoException {
        RdpPacket_Localised data = this.initData(8);
        data.setLittleEndian16(0);
        data.setLittleEndian16(0x3e);
        data.setLittleEndian16(seq);
        data.setLittleEndian16(0x32);
        data.markEnd();
        logger.debug("fonts");
        this.sendData(data, RDP_DATA_PDU_FONT2);
    }

    private void processPointer(RdpPacket_Localised data) throws RdesktopException {
        int message_type = 0;
        int x = 0, y = 0;
        message_type = data.getLittleEndian16();
        data.incrementPosition(2);
        switch(message_type) {
            case (Rdp.RDP_POINTER_MOVE):
                logger.debug("Rdp.RDP_POINTER_MOVE");
                x = data.getLittleEndian16();
                y = data.getLittleEndian16();
                if (data.getPosition() <= data.getEnd()) {
                }
                break;
            case (Rdp.RDP_POINTER_COLOR):
                process_colour_pointer_pdu(data);
                break;
            case (Rdp.RDP_POINTER_CACHED):
                process_cached_pointer_pdu(data);
                break;
            case RDP_POINTER_SYSTEM:
                process_system_pointer_pdu(data);
                break;
            default:
                break;
        }
    }

    private void process_system_pointer_pdu(RdpPacket_Localised data) {
        int system_pointer_type = 0;
        data.getLittleEndian16(system_pointer_type);
        switch(system_pointer_type) {
            case RDP_NULL_POINTER:
                logger.debug("RDP_NULL_POINTER");
                break;
            default:
                logger.warn("Unimplemented system pointer message 0x" + Integer.toHexString(system_pointer_type));
        }
    }

    protected void processBitmapUpdates(RdpPacket_Localised data) throws RdesktopException {
        int n_updates = 0;
        int left = 0, top = 0, right = 0, bottom = 0, width = 0, height = 0;
        int cx = 0, cy = 0, bitsperpixel = 0, compression = 0, buffersize = 0, size = 0;
        byte[] pixel = null;
        int minX, minY, maxX, maxY;
        maxX = maxY = 0;
        minX = 300;
        minY = 400;
        minX = 480;
        minY = 800;
        n_updates = data.getLittleEndian16();
        for (int i = 0; i < n_updates; i++) {
            left = data.getLittleEndian16();
            top = data.getLittleEndian16();
            right = data.getLittleEndian16();
            bottom = data.getLittleEndian16();
            width = data.getLittleEndian16();
            height = data.getLittleEndian16();
            bitsperpixel = data.getLittleEndian16();
            int Bpp = (bitsperpixel + 7) / 8;
            compression = data.getLittleEndian16();
            buffersize = data.getLittleEndian16();
            cx = right - left + 1;
            cy = bottom - top + 1;
            if (minX > left) minX = left;
            if (minY > top) minY = top;
            if (maxX < right) maxX = right;
            if (maxY < bottom) maxY = bottom;
            if (Options.server_bpp != bitsperpixel) {
                logger.warn("Server limited colour depth to " + bitsperpixel + " bits");
                Options.set_bpp(bitsperpixel);
            }
            if (compression == 0) {
                pixel = new byte[width * height * Bpp];
                for (int y = 0; y < height; y++) {
                    data.copyToByteArray(pixel, (height - y - 1) * (width * Bpp), data.getPosition(), width * Bpp);
                    data.incrementPosition(width * Bpp);
                }
                int[] bitmap = Bitmap.convertImage(pixel, Bpp);
                continue;
            }
            if ((compression & 0x400) != 0) {
                size = buffersize;
            } else {
                data.incrementPosition(2);
                size = data.getLittleEndian16();
                data.incrementPosition(4);
            }
            if (Bpp == 1) {
                pixel = Bitmap.decompress(width, height, size, data, Bpp);
                if (pixel != null) {
                } else logger.warn("Could not decompress bitmap");
            } else {
                if (Options.bitmap_decompression_store == Options.INTEGER_BITMAP_DECOMPRESSION) {
                    int[] pixeli = Bitmap.decompressInt(width, height, size, data, Bpp);
                    if (pixeli != null) {
                        int[] colors = new int[pixeli.length];
                        int value = 0;
                        for (int ii = 0; i < pixeli.length; i++) {
                            value = new Integer(pixeli[i]).intValue();
                            colors[i] = Color.argb(Color.alpha(value), Color.red(value), Color.green(value), Color.blue(value));
                        }
                        android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(colors, width, height, Config.RGB_565);
                        if (bm.getHeight() > 100) {
                            int ii = 0;
                            ii++;
                        }
                    } else logger.warn("Could not decompress bitmap");
                } else if (Options.bitmap_decompression_store == Options.BUFFEREDIMAGE_BITMAP_DECOMPRESSION) {
                    logger.warn("Could not decompress bitmap");
                } else {
                }
            }
        }
    }

    protected void processPalette(RdpPacket_Localised data) {
        int n_colors = 0;
        byte[] palette = null;
        byte[] red = null;
        byte[] green = null;
        byte[] blue = null;
        int j = 0;
        data.incrementPosition(2);
        n_colors = data.getLittleEndian16();
        data.incrementPosition(2);
        palette = new byte[n_colors * 3];
        red = new byte[n_colors];
        green = new byte[n_colors];
        blue = new byte[n_colors];
        data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
        data.incrementPosition(palette.length);
        for (int i = 0; i < n_colors; i++) {
            red[i] = palette[j];
            green[i] = palette[j + 1];
            blue[i] = palette[j + 2];
            j += 3;
        }
    }

    protected void process_null_system_pointer_pdu(RdpPacket_Localised s) throws RdesktopException {
    }

    protected void process_colour_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
        logger.debug("Rdp.RDP_POINTER_COLOR");
        int x = 0, y = 0, width = 0, height = 0, cache_idx = 0, masklen = 0, datalen = 0;
        byte[] mask = null, pixel = null;
        cache_idx = data.getLittleEndian16();
        x = data.getLittleEndian16();
        y = data.getLittleEndian16();
        width = data.getLittleEndian16();
        height = data.getLittleEndian16();
        masklen = data.getLittleEndian16();
        datalen = data.getLittleEndian16();
        mask = new byte[masklen];
        pixel = new byte[datalen];
        data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
        data.incrementPosition(datalen);
        data.copyToByteArray(mask, 0, data.getPosition(), masklen);
        data.incrementPosition(masklen);
    }

    protected void process_cached_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
        logger.debug("Rdp.RDP_POINTER_CACHED");
        int cache_idx = data.getLittleEndian16();
    }

    public void displayImage(int[] data, int w, int h, int x, int y, int cx, int cy) {
    }
}
