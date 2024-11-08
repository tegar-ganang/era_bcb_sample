package org.tn5250jlpr;

import java.util.*;
import java.text.*;
import java.net.Socket;
import java.net.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.reflect.*;

public final class LPRStream implements Runnable {

    Socket sock;

    BufferedInputStream bin;

    BufferedOutputStream bout;

    Vector bf;

    Stream bk;

    public int ascii[] = new int[256];

    private int ebcdic[] = new int[256];

    private boolean waitingForInput;

    boolean invited;

    private boolean dumpBytes = true;

    private boolean negotiated = false;

    private Thread me;

    private boolean cursorOn = false;

    private String session = "";

    private int port = 23;

    private boolean connected = false;

    ByteArrayOutputStream baosp = null;

    ByteArrayOutputStream baosrsp = null;

    ByteArrayOutputStream baosin = null;

    byte[] saveStream;

    private boolean proxySet = false;

    private String proxyHost = null;

    private String proxyPort = "1080";

    private int devSeq = -1;

    private String devName;

    private String devNameUsed;

    private FileOutputStream fw;

    private BufferedOutputStream dw;

    private boolean fileOpen;

    private LPRInterface scs2;

    LPRStream() {
        bf = new Vector();
        setCodePage("37");
        baosp = new ByteArrayOutputStream();
        baosrsp = new ByteArrayOutputStream();
        baosin = new ByteArrayOutputStream();
    }

    public String getHostName() {
        return session;
    }

    public void setDeviceName(String name) {
        devName = name;
    }

    public String getDeviceName() {
        return devName;
    }

    public String getAllocatedDeviceName() {
        return devNameUsed;
    }

    public boolean isConnected() {
        return connected;
    }

    public final boolean connect() {
        return connect(session, port);
    }

    public final void setProxy(String proxyHost, String proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        proxySet = true;
        Properties systemProperties = System.getProperties();
        systemProperties.put("socksProxySet", "true");
        systemProperties.put("socksProxyHost", proxyHost);
        systemProperties.put("socksProxyPort", proxyPort);
        System.setProperties(systemProperties);
    }

    public final boolean connect(String s, int port) {
        try {
            session = s;
            this.port = port;
            sock = new Socket(s, port);
            if (sock == null) System.out.println("I did not get a socket");
            connected = true;
            sock.setTcpNoDelay(true);
            sock.setSoLinger(false, 0);
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
            bin = new BufferedInputStream(in, 8192);
            bout = new BufferedOutputStream(out);
            byte abyte0[];
            while (negotiate(abyte0 = readNegotiations())) ;
            negotiated = true;
            try {
                loadStream(abyte0, 0);
                processStream();
            } catch (IOException ioe) {
            } catch (Exception ex1) {
            }
            System.out.println("starting up ");
            me = new Thread(this);
            me.start();
        } catch (Exception exception) {
            System.out.println("connect() " + exception.getMessage());
            if (sock == null) System.out.println("I did not get a socket");
            disconnect();
            return false;
        }
        return true;
    }

    public final boolean disconnect() {
        if (me != null && me.isAlive()) me.interrupt();
        try {
            if (bin != null) bin.close();
            if (bout != null) bout.close();
            if (sock != null) {
                System.out.println("Closing socket");
                sock.close();
            }
            connected = false;
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            connected = false;
            devSeq = -1;
            return false;
        }
        devSeq = -1;
        return true;
    }

    public static void main(String[] args) {
        final String argSession;
        final int argPort;
        final LPRStream lpr = new LPRStream();
        if (args.length > 0) {
            argSession = args[0];
            if (isSpecified("-dn", args)) lpr.setDeviceName(getParm("-dn", args));
            if (isSpecified("-p", args)) argPort = Integer.parseInt(getParm("-p", args)); else argPort = 23;
            if (isSpecified("-cp", args)) lpr.setCodePage(getParm("-cp", args)); else lpr.setCodePage("37");
        } else {
            usage();
            return;
        }
        Runnable connectIt = new Runnable() {

            public void run() {
                lpr.connect(argSession, argPort);
            }
        };
        Thread ct = new Thread(connectIt);
        ct.setDaemon(true);
        ct.start();
        while (true) {
        }
    }

    private static String getParm(String parm, String[] args) {
        for (int x = 0; x < args.length; x++) {
            if (args[x].equals(parm)) return args[x + 1];
        }
        return null;
    }

    static boolean isSpecified(String parm, String[] args) {
        if (args == null) return false;
        for (int x = 0; x < args.length; x++) {
            if (args[x] != null && args[x].equals(parm)) return true;
        }
        return false;
    }

    static void usage() {
        System.out.println("tn5250jlpr usage:");
        System.out.println("\tjava -jar tn5250jlpr host -dnDeviceName -pPort -cpCodePage");
        System.out.println("\thost is MANDATORY");
    }

    private final ByteArrayOutputStream appendByteStream(byte abyte0[]) {
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        for (int i = 0; i < abyte0.length; i++) {
            bytearrayoutputstream.write(abyte0[i]);
            if (abyte0[i] == -1) bytearrayoutputstream.write(-1);
        }
        return bytearrayoutputstream;
    }

    public final void run() {
        boolean done = false;
        if (Thread.currentThread() == me) {
            while (!done) {
                try {
                    waitingForInput = false;
                    byte[] abyte0 = readIncoming();
                    loadStream(abyte0, 0);
                    if (!bf.isEmpty()) {
                        invited = false;
                        me.yield();
                        processStream();
                    }
                } catch (SocketException se) {
                    System.out.println(se.getMessage());
                    done = true;
                    disconnect();
                    System.exit(-1);
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                    invited = true;
                    if (me.isInterrupted()) done = true;
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    invited = true;
                    if (me.isInterrupted()) done = true;
                }
            }
        }
    }

    private final byte[] readNegotiations() throws IOException {
        int i = bin.read();
        if (i < 0) {
            throw new IOException("Connection closed.");
        } else {
            int j = bin.available();
            byte abyte0[] = new byte[j + 1];
            abyte0[0] = (byte) i;
            bin.read(abyte0, 1, j);
            return abyte0;
        }
    }

    private final void loadStream(byte abyte0[], int i) throws IOException {
        int j = 0;
        int size = 0;
        if (saveStream == null) {
            j = (abyte0[i] & 0xff) << 8 | abyte0[i + 1] & 0xff;
            size = abyte0.length;
        } else {
            size = saveStream.length + abyte0.length;
            byte[] inter = new byte[size];
            System.arraycopy(saveStream, 0, inter, 0, saveStream.length);
            System.arraycopy(abyte0, 0, inter, saveStream.length, abyte0.length);
            abyte0 = new byte[size];
            System.arraycopy(inter, 0, abyte0, 0, size);
            saveStream = null;
            inter = null;
            j = (abyte0[i] & 0xff) << 8 | abyte0[i + 1] & 0xff;
        }
        if (j > size) {
            saveStream = new byte[abyte0.length];
            System.arraycopy(abyte0, 0, saveStream, 0, abyte0.length);
        } else {
            byte abyte1[];
            try {
                abyte1 = new byte[j + 2];
                System.arraycopy(abyte0, i, abyte1, 0, j + 2);
                bf.addElement(new Stream(abyte1));
                if (abyte0.length > abyte1.length + i) loadStream(abyte0, i + j + 2);
            } catch (Exception ex) {
                System.out.println("load stream error " + ex.getMessage());
            }
        }
    }

    private final void writeByte(byte abyte0[]) throws IOException {
        bout.write(abyte0);
        bout.flush();
    }

    private final void writeByte(byte byte0) throws IOException {
        bout.write(byte0);
        bout.flush();
    }

    public final void writeGDS(int flags, int opcode, byte abyte0[]) throws IOException {
        int length;
        if (abyte0 != null) length = abyte0.length + 7; else length = 7;
        baosrsp.write(length >> 8);
        baosrsp.write(length & 0xff);
        baosrsp.write(18);
        baosrsp.write(160);
        baosrsp.write(0x01);
        baosrsp.write(0x02);
        baosrsp.write(abyte0.length + 1);
        if (abyte0 != null) baosrsp.write(abyte0, 0, abyte0.length);
        baosrsp = appendByteStream(baosrsp.toByteArray());
        baosrsp.write(IAC);
        baosrsp.write(EOR);
        baosrsp.writeTo(bout);
        bout.flush();
        baosrsp.reset();
    }

    public final int getDataFlowType() {
        return bk.getDataFlowType();
    }

    private final void sendNotify() throws IOException {
        writeGDS(0, 0, null);
    }

    private final void processStream() throws Exception {
        while (!bf.isEmpty()) {
            me.yield();
            cursorOn = false;
            bk = (Stream) bf.elementAt(0);
            bf.removeElementAt(0);
            System.out.println("data flow type: " + Integer.toBinaryString(getDataFlowType()) + " op code: " + Integer.toHexString(bk.getOpCode()) + " record flag: " + Integer.toBinaryString(bk.getFlag()));
            switch(bk.getOpCode()) {
                case 00:
                    while (bk.hasNext()) System.out.print(getASCIIChar(bk.getNextByte()));
                    System.out.println();
                    sendPrintCompleteRecord();
                    break;
                case 1:
                    if (scs2 == null) getWriter();
                    if (!fileOpen) {
                        scs2.openOutputFile();
                        fileOpen = true;
                    }
                    if (bk.getStreamSize() == 17) {
                        fileOpen = false;
                        scs2.closeOutputFile();
                        scs2 = null;
                    } else scs2.process_print_record(bk);
                    sendPrintCompleteRecord();
                    break;
                case 2:
                    while (bk.hasNext()) System.out.print(getASCIIChar(bk.getNextByte()));
                    System.out.println();
                    break;
                default:
                    break;
            }
        }
    }

    private final void getWriter() {
        scs2 = new SCS2PDF();
    }

    private final void sendPrintCompleteRecord() throws IOException {
        byte[] byte0 = new byte[3];
        byte0[0] = 0x00;
        byte0[1] = 0x00;
        byte0[2] = 0x01;
        writeGDS(0, 0, byte0);
    }

    public final byte[] readIncoming() throws IOException {
        boolean done = false;
        boolean negotiate = false;
        baosin.reset();
        int j = -1;
        int i = 0;
        while (!done) {
            i = bin.read();
            if (j == 255 && i == 255) {
                j = -1;
                continue;
            } else {
                baosin.write(i);
                if (j == 255 && i == 239) done = true;
                if (i == 253 && j == 255) {
                    done = true;
                    negotiate = true;
                }
                j = i;
            }
        }
        if (negotiate) {
            baosin.write(bin.read());
            negotiate(baosin.toByteArray());
        }
        if (dumpBytes) {
            System.out.println("dumping");
            dump(baosin.toByteArray());
        }
        return baosin.toByteArray();
    }

    private final boolean negotiate(byte abyte0[]) throws IOException {
        int i = 0;
        if (abyte0[i] == IAC) {
            while (i < abyte0.length && abyte0[i++] == -1) switch(abyte0[i++]) {
                case WONT:
                default:
                    break;
                case DO:
                    switch(abyte0[i]) {
                        case TERMINAL_TYPE:
                            baosp.write(IAC);
                            baosp.write(WILL);
                            baosp.write(TERMINAL_TYPE);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                        case OPT_END_OF_RECORD:
                            baosp.write(IAC);
                            baosp.write(WILL);
                            baosp.write(OPT_END_OF_RECORD);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                        case TRANSMIT_BINARY:
                            baosp.write(IAC);
                            baosp.write(WILL);
                            baosp.write(TRANSMIT_BINARY);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                        case TIMING_MARK:
                            baosp.write(IAC);
                            baosp.write(WONT);
                            baosp.write(TIMING_MARK);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                        case NEW_ENVIRONMENT:
                            if (devName == null) {
                                baosp.write(IAC);
                                baosp.write(WONT);
                                baosp.write(NEW_ENVIRONMENT);
                                writeByte(baosp.toByteArray());
                                baosp.reset();
                            } else {
                                System.out.println(devName);
                                baosp.write(IAC);
                                baosp.write(WILL);
                                baosp.write(NEW_ENVIRONMENT);
                                writeByte(baosp.toByteArray());
                                baosp.reset();
                            }
                            break;
                        default:
                            baosp.write(IAC);
                            baosp.write(WONT);
                            baosp.write(abyte0[i]);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                    }
                    i++;
                    break;
                case WILL:
                    switch(abyte0[i]) {
                        case OPT_END_OF_RECORD:
                            baosp.write(IAC);
                            baosp.write(DO);
                            baosp.write(OPT_END_OF_RECORD);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                        case TRANSMIT_BINARY:
                            baosp.write(IAC);
                            baosp.write(DO);
                            baosp.write(TRANSMIT_BINARY);
                            writeByte(baosp.toByteArray());
                            baosp.reset();
                            break;
                    }
                    i++;
                    break;
                case SB:
                    if (abyte0[i] == NEW_ENVIRONMENT && abyte0[i + 1] == 1) {
                        byte[] seed = new byte[8];
                        int offset = i + 11;
                        for (int x = 0; x < 8; x++) {
                            seed[x] = abyte0[offset + x];
                        }
                        negNewEnvironment(seed);
                        i++;
                    }
                    if (abyte0[i] == TERMINAL_TYPE && abyte0[i + 1] == 1) {
                        baosp.write(IAC);
                        baosp.write(SB);
                        baosp.write(TERMINAL_TYPE);
                        baosp.write(QUAL_IS);
                        baosp.write((new String("IBM-3812-1")).getBytes());
                        baosp.write(IAC);
                        baosp.write(SE);
                        writeByte(baosp.toByteArray());
                        baosp.reset();
                        i++;
                    }
                    i++;
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    private void negNewEnvironment(byte[] seed) throws IOException {
        baosp.write(IAC);
        baosp.write(SB);
        baosp.write(NEW_ENVIRONMENT);
        baosp.write(IS);
        baosp.write(USERVAR);
        baosp.write((new String("IBMRSEED")).getBytes());
        baosp.write(seed);
        baosp.write(VAR);
        baosp.write(USERVAR);
        baosp.write((new String("DEVNAME")).getBytes());
        baosp.write(VALUE);
        baosp.write(negDeviceName().getBytes());
        baosp.write(USERVAR);
        baosp.write((new String("IBMMSGQNAME")).getBytes());
        baosp.write(VALUE);
        baosp.write((new String("QSYSOPR")).getBytes());
        baosp.write(USERVAR);
        baosp.write((new String("IBMMSGQLIB")).getBytes());
        baosp.write(VALUE);
        baosp.write((new String("*LIBL")).getBytes());
        baosp.write(USERVAR);
        baosp.write((new String("IBMFONT")).getBytes());
        baosp.write(VALUE);
        baosp.write((new String("10")).getBytes());
        baosp.write(USERVAR);
        baosp.write((new String("IBMTRANSFORM")).getBytes());
        baosp.write(VALUE);
        baosp.write((new String("0")).getBytes());
        baosp.write(IAC);
        baosp.write(SE);
        writeByte(baosp.toByteArray());
        baosp.reset();
    }

    /**
    * This will negotiate a device name with controller.
    *    if the sequence is less than zero then it will send the device name
    *    as specified.  On each unsuccessful attempt a sequential number is
    *    appended until we find one or the controller says no way.
    */
    private String negDeviceName() {
        if (devSeq++ == -1) {
            devNameUsed = devName;
            return devName;
        } else {
            StringBuffer sb = new StringBuffer(devName + devSeq);
            int ei = 1;
            while (sb.length() > 10) {
                sb.setLength(0);
                sb.append(devName.substring(0, devName.length() - ei++));
                sb.append(devSeq);
            }
            devNameUsed = sb.toString();
            return devNameUsed;
        }
    }

    public final void setCodePage(String codePage) {
        int i = 0;
        int[] cp = CharMappings.getCodePage(codePage);
        do {
            ebcdic[i] = cp[i];
            ascii[cp[i]] = i;
        } while (++i < 256);
    }

    public byte getEBCDIC(int index) {
        return (byte) ascii[index & 0xff];
    }

    public char getEBCDICChar(int index) {
        return (char) ascii[index & 0xff];
    }

    public byte getASCII(int index) {
        return (byte) ascii[index];
    }

    public char getASCIIChar(int index) {
        return (char) ebcdic[index & 0xff];
    }

    public void dump(byte[] abyte0) {
        try {
            if (fw == null) {
                fw = new FileOutputStream("log.txt");
                dw = new BufferedOutputStream(fw);
            }
            System.out.print("\n Buffer Dump of data from AS400: ");
            dw.write("\r\n Buffer Dump of data from AS400: ".getBytes());
            StringBuffer h = new StringBuffer();
            for (int x = 0; x < abyte0.length; x++) {
                if (x % 16 == 0) {
                    System.out.println("  " + h.toString());
                    dw.write(("  " + h.toString() + "\r\n").getBytes());
                    h.setLength(0);
                    h.append("+0000");
                    h.setLength(5 - Integer.toHexString(x).length());
                    h.append(Integer.toHexString(x).toUpperCase());
                    System.out.print(h.toString());
                    dw.write(h.toString().getBytes());
                    h.setLength(0);
                }
                char ac = getASCIIChar(abyte0[x]);
                if (ac < ' ') h.append('.'); else h.append(ac);
                if (x % 4 == 0) {
                    System.out.print(" ");
                    dw.write((" ").getBytes());
                }
                if (Integer.toHexString(abyte0[x] & 0xff).length() == 1) {
                    System.out.print("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write(("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());
                } else {
                    System.out.print(Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write((Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());
                }
            }
            System.out.println();
            dw.write("\r\n".getBytes());
            dw.flush();
        } catch (EOFException _ex) {
        } catch (Exception _ex) {
            System.out.println("Cannot dump from host\n\r");
        }
    }

    public void dumpHexBytes(byte[] abyte) {
        byte shit[] = abyte;
        for (int i = 0; i < shit.length; i++) System.out.println(i + ">" + shit[i] + "< hex >" + Integer.toHexString((shit[i] & 0xff)));
    }

    private static final byte IAC = (byte) -1;

    private static final byte DONT = (byte) -2;

    private static final byte DO = (byte) -3;

    private static final byte WONT = (byte) -4;

    private static final byte WILL = (byte) -5;

    private static final byte SB = (byte) -6;

    private static final byte SE = (byte) -16;

    private static final byte EOR = (byte) -17;

    private static final byte TERMINAL_TYPE = (byte) 24;

    private static final byte OPT_END_OF_RECORD = (byte) 25;

    private static final byte TRANSMIT_BINARY = (byte) 0;

    private static final byte QUAL_IS = (byte) 0;

    private static final byte TIMING_MARK = (byte) 6;

    private static final byte NEW_ENVIRONMENT = (byte) 39;

    private static final byte IS = (byte) 0;

    private static final byte SEND = (byte) 1;

    private static final byte INFO = (byte) 2;

    private static final byte VAR = (byte) 0;

    private static final byte VALUE = (byte) 1;

    private static final byte NEGOTIATE_ESC = (byte) 2;

    private static final byte USERVAR = (byte) 3;

    private static final byte ESC = 0x04;

    private static final char char0 = 0;
}
