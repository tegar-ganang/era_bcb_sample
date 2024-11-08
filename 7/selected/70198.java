package com.lixia.rdp;

import java.io.*;
import java.net.*;
import com.lixia.rdp.RdesktopSwing;
import com.lixia.rdp.crypto.*;
import com.lixia.rdp.rdp5.VChannels;
import java.awt.*;
import java.awt.image.*;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

public class RdpJPanel {

    public static int RDP5_DISABLE_NOTHING = 0x00;

    public static int RDP5_NO_WALLPAPER = 0x01;

    public static int RDP5_NO_FULLWINDOWDRAG = 0x02;

    public static int RDP5_NO_MENUANIMATIONS = 0x04;

    public static int RDP5_NO_THEMING = 0x08;

    public static int RDP5_NO_CURSOR_SHADOW = 0x20;

    public static int RDP5_NO_CURSORSETTINGS = 0x40;

    protected static Logger logger = Logger.getLogger("RdpJPanel");

    private static int INFO_MOUSE = 0x00000001;

    private static int INFO_DISABLECTRLALTDEL = 0x00000002;

    private static int INFO_AUTOLOGON = 0x00000008;

    private static int INFO_UNICODE = 0x00000010;

    private static int INFO_MAXIMIZESHELL = 0x00000020;

    private static int INFO_LOGONNOTIFY = 0x00000040;

    private static int INFO_COMPRESSION = 0x00000080;

    private static int INFO_ENABLEWINDOWSKEY = 0x00000100;

    private static int INFO_REMOTECONSOLEAUDIO = 0x00002000;

    private static int INFO_FORCE_ENCRYPTED_CS_PDU = 0x00004000;

    private static int INFO_RAIL = 0x00008000;

    private static int INFO_LOGONERRORS = 0x00010000;

    private static int INFO_MOUSE_HAS_WHEEL = 0x00020000;

    private static int INFO_PASSWORD_IS_SC_PIN = 0x00040000;

    private static int INFO_NOAUDIOPLAYBACK = 0x00080000;

    private static int INFO_USING_SAVED_CREDS = 0x00100000;

    private static int RNS_INFO_AUDIOCAPTURE = 0x00200000;

    private static int RNS_INFO_VIDEO_DISABLE = 0x00400000;

    private static int CompressionTypeMask = 0x00001E00;

    private static int PACKET_COMPR_TYPE_8K = 0x00000100;

    private static int PACKET_COMPR_TYPE_64K = 0x00000200;

    private static int PACKET_COMPR_TYPE_RDP6 = 0x00000300;

    private static int PACKET_COMPR_TYPE_RDP61 = 0x00000400;

    private static int INFO_NORMALLOGON = (INFO_MOUSE | INFO_DISABLECTRLALTDEL | INFO_UNICODE | INFO_MAXIMIZESHELL);

    private static int CLIENT_INFO_AF_INET = 0x0002;

    private static int CLIENT_INFO_AF_INET6 = 0x0017;

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

    private RdesktopJFrame frame = null;

    private RdesktopJPanel surface = null;

    protected OrdersJPanel orders = null;

    private Cache cache = null;

    private Cursor g_null_cursor = null;

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
     * @param data Packet containing capability set data at current read position
     */
    static void processGeneralCaps(RdpPacket_Localised data) {
        int pad2octetsB;
        data.incrementPosition(10);
        pad2octetsB = data.getLittleEndian16();
        if (pad2octetsB == 0) Options.use_rdp5 = false;
    }

    /**
     * Process a bitmap capability set
     * @param data Packet containing capability set data at current read position
     */
    static void processBitmapCaps(RdpPacket_Localised data) {
        int width, height, depth;
        depth = data.getLittleEndian16();
        data.incrementPosition(6);
        width = data.getLittleEndian16();
        height = data.getLittleEndian16();
        logger.debug("setting desktop size and depth to: " + width + "x" + height + "x" + depth);
        if (Options.server_bpp != depth) {
            logger.warn("colour depth changed from " + Options.server_bpp + " to " + depth);
            Options.set_bpp(depth);
        }
        if (Options.width != width || Options.height != height) {
            logger.warn("screen size changed from " + Options.width + "x" + Options.height + " to " + width + "x" + height);
            Options.width = width;
            Options.height = height;
        }
    }

    /**
     * Process server capabilities
     * @param data Packet containing capability set data at current read position
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
     * @param data Packet containing disconnect PDU at current read position
     * @return Code specifying the reason for disconnection
     */
    protected int processDisconnectPdu(RdpPacket_Localised data) {
        logger.debug("Received disconnect PDU");
        return data.getLittleEndian32();
    }

    /**
     * Initialise RDP comms layer, and register virtual channels
     * @param channels Virtual channels to be used in connection
     */
    public RdpJPanel(VChannels channels) {
        this.SecureLayer = new Secure(channels);
        Common.secure = SecureLayer;
        this.orders = new OrdersJPanel();
        this.cache = new Cache();
        orders.registerCache(cache);
    }

    /**
     * Initialise a packet for sending data on the RDP layer
     * @param size Size of RDP data
     * @return Packet initialised for RDP
     * @throws RdesktopException
     */
    private RdpPacket_Localised initData(int size) throws RdesktopException {
        RdpPacket_Localised buffer = null;
        buffer = SecureLayer.init(Constants.encryption ? Secure.SEC_ENCRYPT : 0, size + 18);
        buffer.pushLayer(RdpPacket_Localised.RDP_HEADER, 18);
        return buffer;
    }

    /**
     * Send a packet on the RDP layer
     * @param data Packet to send
     * @param data_pdu_type Type of data
     * @throws RdesktopException
     * @throws IOException
     * @throws CryptoException
     */
    private void sendData(RdpPacket_Localised data, int data_pdu_type) throws RdesktopException, IOException, CryptoException {
        CommunicationMonitor.lock(this);
        int length;
        data.setPosition(data.getHeader(RdpPacket_Localised.RDP_HEADER));
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
     * @param type Type of PDU received, stored in type[0]
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
     * @param username Username for log on
     * @param server Server to connect to
     * @param flags Flags defining logon type
     * @param domain Domain for log on
     * @param password Password for log on
     * @param command Alternative shell for session
     * @param directory Initial working directory for connection
     * @throws ConnectionException
     */
    public void connect(String username, InetAddress server, String domain, String password, String command, String directory) throws ConnectionException {
        int connect_flags = INFO_NORMALLOGON;
        if (Options.bulk_compression) {
            connect_flags |= INFO_COMPRESSION | PACKET_COMPR_TYPE_64K;
        }
        if (Options.autologin) {
            connect_flags |= INFO_AUTOLOGON;
        }
        if (Options.console_audio) {
            connect_flags |= INFO_REMOTECONSOLEAUDIO;
        }
        connect_flags |= INFO_UNICODE;
        connect_flags |= INFO_LOGONERRORS;
        connect_flags |= INFO_LOGONNOTIFY;
        connect_flags |= INFO_ENABLEWINDOWSKEY;
        connect_flags |= RNS_INFO_AUDIOCAPTURE;
        try {
            SecureLayer.connect(server);
            this.connected = true;
            rdp_send_client_info(connect_flags, domain, username, password, command, directory);
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
     * @return True if connection to RDP session
     */
    public boolean isConnected() {
        return this.connected;
    }

    boolean deactivated;

    int ext_disc_reason;

    /**
     * RDP receive loop
     * @param deactivated On return, stores true in deactivated[0] if the session disconnected cleanly
     * @param ext_disc_reason On return, stores the reason for disconnection in ext_disc_reason[0]
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
                case (RdpJPanel.RDP_PDU_DEMAND_ACTIVE):
                    logger.debug("Rdp.RDP_PDU_DEMAND_ACTIVE");
                    NDC.push("processDemandActive");
                    this.processDemandActive(data);
                    logger.debug("ready to send (got past licence negotiation)");
                    RdesktopSwing.readytosend = true;
                    frame.triggerReadyToSend();
                    NDC.pop();
                    deactivated[0] = false;
                    break;
                case (RdpJPanel.RDP_PDU_DEACTIVATE):
                    deactivated[0] = true;
                    this.stream = null;
                    break;
                case (RdpJPanel.RDP_PDU_DATA):
                    NDC.push("processData");
                    try {
                        disc = this.processData(data, ext_disc_reason);
                    } catch (Exception ex) {
                        logger.error(ex.getStackTrace());
                    }
                    NDC.pop();
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

    private void rdp_out_client_timezone_info(RdpPacket_Localised data) {
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
    }

    /**
     * Send user logon details to the server
     * @param flags Set of flags defining logon type
     * @param domain Domain for logon
     * @param username Username for logon
     * @param password Password for logon
     * @param command Alternative shell for session
     * @param directory Starting working directory for session
     * @throws RdesktopException
     * @throws IOException
     * @throws CryptoException
     */
    private void rdp_send_client_info(int flags, String domain, String username, String password, String command, String directory) throws RdesktopException, IOException, CryptoException {
        int len_ip = 2 * "127.0.0.1".length();
        int len_dll = 2 * "C:\\WINNT\\System32\\mstscax.dll".length();
        int packetlen = 0;
        int sec_flags = Secure.SEC_INFO_PKT | (Constants.encryption ? Secure.SEC_ENCRYPT : 0);
        int domainlen = 2 * domain.length();
        int userlen = 2 * username.length();
        int passlen = 2 * password.length();
        int commandlen = 2 * command.length();
        int dirlen = 2 * directory.length();
        packetlen = 8 + (5 * 4) + domainlen + userlen + passlen + commandlen + dirlen;
        if (Options.use_rdp5 && 1 != Options.server_rdp_version) {
            packetlen += 180 + (2 * 4) + len_ip + len_dll;
        }
        RdpPacket_Localised data = SecureLayer.init(sec_flags, packetlen);
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
        if (Options.use_rdp5 && 1 != Options.server_rdp_version) {
            logger.debug("Sending RDP5-style Logon packet");
            data.setLittleEndian16(CLIENT_INFO_AF_INET);
            data.setLittleEndian16(len_ip + 2);
            data.outUnicodeString("127.0.0.1", len_ip);
            data.setLittleEndian16(len_dll + 2);
            data.outUnicodeString("C:\\WINNT\\System32\\mstscax.dll", len_dll);
            rdp_out_client_timezone_info(data);
            data.setLittleEndian32(2);
            data.setLittleEndian32(0);
            data.setLittleEndian32(Options.rdp5_performanceflags);
            data.setLittleEndian32(0);
        }
        data.markEnd();
        byte[] buffer = new byte[data.getEnd()];
        data.copyToByteArray(buffer, 0, 0, data.getEnd());
        SecureLayer.send(data, sec_flags);
    }

    /**
     * Process an activation demand from the server (received between licence negotiation and 1st order)
     * @param data Packet containing demand at current read position
     * @throws RdesktopException
     * @throws IOException
     * @throws CryptoException
     * @throws OrderException
     */
    private void processDemandActive(RdpPacket_Localised data) throws RdesktopException, IOException, CryptoException, OrderException {
        int type[] = new int[1];
        int len_src_descriptor, len_combined_caps;
        this.rdp_shareid = data.getLittleEndian32();
        len_src_descriptor = data.getLittleEndian16();
        len_combined_caps = data.getLittleEndian16();
        data.incrementPosition(len_src_descriptor);
        processServerCaps(data, len_combined_caps);
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
        this.orders.resetOrderState();
    }

    /**
     * Process a data PDU received from the server
     * @param data Packet containing data PDU at current read position
     * @param ext_disc_reason If a disconnect PDU is received, stores disconnection reason at ext_disc_reason[0]
     * @return True if disconnect PDU was received
     * @throws RdesktopException
     * @throws OrderException
     */
    private boolean processData(RdpPacket_Localised data, int[] ext_disc_reason) throws RdesktopException, OrderException {
        @SuppressWarnings("unused") int data_type, ctype, clen, len, roff, rlen;
        data_type = 0;
        data.incrementPosition(6);
        len = data.getLittleEndian16();
        data_type = data.get8();
        ctype = data.get8();
        clen = data.getLittleEndian16();
        clen -= 18;
        switch(data_type) {
            case (RdpJPanel.RDP_DATA_PDU_UPDATE):
                this.processUpdate(data);
                break;
            case RDP_DATA_PDU_CONTROL:
                logger.debug(("Received Control PDU\n"));
                break;
            case RDP_DATA_PDU_SYNCHRONISE:
                logger.debug(("Received Sync PDU\n"));
                break;
            case (RdpJPanel.RDP_DATA_PDU_POINTER):
                logger.debug("Received pointer PDU");
                this.processPointer(data);
                break;
            case (RdpJPanel.RDP_DATA_PDU_BELL):
                logger.debug("Received bell PDU");
                Toolkit tx = Toolkit.getDefaultToolkit();
                tx.beep();
                break;
            case (RdpJPanel.RDP_DATA_PDU_LOGON):
                logger.debug("User logged on");
                RdesktopSwing.loggedon = true;
                break;
            case RDP_DATA_PDU_DISCONNECT:
                ext_disc_reason[0] = processDisconnectPdu(data);
                logger.info("Received disconnect PDU");
                if (RdesktopSwing.loggedon || ext_disc_reason[0] > 0) {
                    return true;
                }
                break;
            default:
                logger.warn("Unimplemented Data PDU type " + data_type);
        }
        return false;
    }

    private void processUpdate(RdpPacket_Localised data) throws OrderException, RdesktopException {
        int update_type = 0;
        update_type = data.getLittleEndian16();
        switch(update_type) {
            case (RdpJPanel.RDP_UPDATE_ORDERS):
                data.incrementPosition(2);
                int n_orders = data.getLittleEndian16();
                data.incrementPosition(2);
                this.orders.processOrders(data, next_packet, n_orders);
                break;
            case (RdpJPanel.RDP_UPDATE_BITMAP):
                this.processBitmapUpdates(data);
                break;
            case (RdpJPanel.RDP_UPDATE_PALETTE):
                this.processPalette(data);
                break;
            case (RdpJPanel.RDP_UPDATE_SYNCHRONIZE):
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
        if (Options.use_rdp5) {
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
        data.setLittleEndian16(0);
        data.setLittleEndian16(0);
        data.setLittleEndian16(Options.use_rdp5 ? 0x40d : 0);
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
            RdesktopSwing.error(e, this, frame, false);
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
            if (Common.rdp.isConnected()) RdesktopSwing.error(r, Common.rdp, Common.frame, true);
            Common.exit();
        } catch (CryptoException c) {
            if (Common.rdp.isConnected()) RdesktopSwing.error(c, Common.rdp, Common.frame, true);
            Common.exit();
        } catch (IOException i) {
            if (Common.rdp.isConnected()) RdesktopSwing.error(i, Common.rdp, Common.frame, true);
            Common.exit();
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
            case (RdpJPanel.RDP_POINTER_MOVE):
                logger.debug("Rdp.RDP_POINTER_MOVE");
                x = data.getLittleEndian16();
                y = data.getLittleEndian16();
                if (data.getPosition() <= data.getEnd()) {
                    surface.movePointer(x, y);
                }
                break;
            case (RdpJPanel.RDP_POINTER_COLOR):
                process_colour_pointer_pdu(data);
                break;
            case (RdpJPanel.RDP_POINTER_CACHED):
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
                surface.setCursor(null);
                setSubCursor(null);
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
        minX = surface.getWidth();
        minY = surface.getHeight();
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
                surface.displayImage(Bitmap.convertImage(pixel, Bpp), width, height, left, top, cx, cy);
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
                if (pixel != null) surface.displayImage(Bitmap.convertImage(pixel, Bpp), width, height, left, top, cx, cy); else logger.warn("Could not decompress bitmap");
            } else {
                if (Options.bitmap_decompression_store == Options.INTEGER_BITMAP_DECOMPRESSION) {
                    int[] pixeli = Bitmap.decompressInt(width, height, size, data, Bpp);
                    if (pixeli != null) surface.displayImage(pixeli, width, height, left, top, cx, cy); else logger.warn("Could not decompress bitmap");
                } else if (Options.bitmap_decompression_store == Options.BUFFEREDIMAGE_BITMAP_DECOMPRESSION) {
                    Image pix = Bitmap.decompressImg(width, height, size, data, Bpp, null);
                    if (pix != null) surface.displayImage(pix, left, top); else logger.warn("Could not decompress bitmap");
                } else {
                    surface.displayCompressed(left, top, width, height, size, data, Bpp, null);
                }
            }
        }
        surface.repaint(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    protected void processPalette(RdpPacket_Localised data) {
        int n_colors = 0;
        IndexColorModel cm = null;
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
        cm = new IndexColorModel(8, n_colors, red, green, blue);
        surface.registerPalette(cm);
    }

    public void registerDrawingSurface(RdesktopJFrame fr) {
        this.frame = fr;
        RdesktopJPanel ds = (RdesktopJPanel) fr.getContentPane();
        this.surface = ds;
        orders.registerDrawingSurface(ds);
    }

    protected void process_null_system_pointer_pdu(RdpPacket_Localised s) throws RdesktopException {
        if (g_null_cursor == null) {
            byte[] null_pointer_mask = new byte[1], null_pointer_data = new byte[24];
            null_pointer_mask[0] = (byte) 0x80;
            g_null_cursor = surface.createCursor(0, 0, 1, 1, null_pointer_mask, null_pointer_data, 0);
        }
        surface.setCursor(g_null_cursor);
        setSubCursor(g_null_cursor);
    }

    protected void process_colour_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
        logger.debug("Rdp.RDP_POINTER_COLOR");
        int x = 0, y = 0, width = 0, height = 0, cache_idx = 0, masklen = 0, datalen = 0;
        byte[] mask = null, pixel = null;
        Cursor cursor = null;
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
        cursor = surface.createCursor(x, y, width, height, mask, pixel, cache_idx);
        surface.setCursor(cursor);
        setSubCursor(cursor);
        cache.putCursor(cache_idx, cursor);
    }

    protected void process_cached_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
        int cache_idx = data.getLittleEndian16();
        surface.setCursor(cache.getCursor(cache_idx));
        setSubCursor(cache.getCursor(cache_idx));
    }

    private void setSubCursor(Cursor cursor) {
        surface.setCursor(cursor);
        if (RdesktopSwing.seamlessSetcursor != null) {
            try {
                RdesktopSwing.seamlessSetcursor.invoke(RdesktopSwing.seamlessChannel, cursor);
            } catch (Exception e) {
            }
        }
    }
}
