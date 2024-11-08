package gps;

import bt747.sys.Convert;
import bt747.sys.Settings;
import bt747.sys.Vm;
import bt747.util.Vector;
import gps.port.GPSPort;
import gps.port.GPSWabaPort;

/** This class implements the low level driver of the GPS device.
 * It extracs NMEA strings.
 * The getResponse function should be called regurarly to get the GPS
 * device's response.
 * @author Mario De Weerd
 */
public class GPSrxtx {

    private static final boolean GPS_DEBUG = false;

    private GPSPort gpsPort;

    private Semaphore m_writeOngoing = new Semaphore(1);

    private boolean ignoreNMEA = false;

    /** Class constructor.
     */
    public GPSrxtx() {
        gpsPort = new GPSWabaPort();
        setDefaults();
    }

    /** Set the defaults for the device according to the given parameters.
     * @param port
     * @param speed
     */
    public void setDefaults(final int port, final int speed) {
        gpsPort.setPort(port);
        gpsPort.setSpeed(speed);
    }

    /** Set the defaults of the device according to preset, guessed values.
     */
    public void setDefaults() {
        String Platform = Settings.platform;
        if ((Platform.equals("Java")) || (Platform.equals("Win32")) || (Platform.equals("Posix")) || (Platform.equals("Linux"))) {
            gpsPort.setUSB();
        } else if (Platform.startsWith("PalmOS")) {
            gpsPort.setBlueTooth();
        } else {
            gpsPort.setPort(0);
        }
    }

    public void setBluetoothAndOpen() {
        gpsPort.setBlueTooth();
        gpsPort.openPort();
    }

    public final void setUSBAndOpen() {
        gpsPort.setUSB();
        gpsPort.openPort();
    }

    public final void closePort() {
        gpsPort.closePort();
    }

    public final void openPort() {
        gpsPort.openPort();
    }

    /** Set and open a normal port (giving the port number)
     * 
     * @param port Port number of the port to open
     * @return result of opening the port, 0 if success.
     */
    public int setPortAndOpen(int port) {
        gpsPort.setPort(port);
        return gpsPort.openPort();
    }

    public int getPort() {
        return gpsPort.getPort();
    }

    public int getSpeed() {
        return gpsPort.getSpeed();
    }

    public void setSpeed(int speed) {
        gpsPort.setSpeed(speed);
    }

    private static final int C_INITIAL_STATE = 0;

    private static final int C_START_STATE = 1;

    private static final int C_FIELD_STATE = 2;

    private static final int C_STAR_STATE = 3;

    private static final int C_CHECKSUM_CHAR1_STATE = 4;

    private static final int C_CHECKSUM_CHAR2_STATE = 5;

    private static final int C_EOL_STATE = 6;

    private static final int C_ERROR_STATE = 7;

    private static final int C_FOUND_STATE = 8;

    private static final int C_BUF_SIZE = 0x1100;

    private static final int C_CMDBUF_SIZE = 0x1100;

    private static int current_state = C_INITIAL_STATE;

    private byte[] read_buf = new byte[C_BUF_SIZE];

    private char[] cmd_buf = new char[C_CMDBUF_SIZE];

    private int read_buf_p = 0;

    private int cmd_buf_p = 0;

    private int bytesRead = 0;

    private int checksum = 0;

    private int read_checksum;

    static final int ERR_NOERROR = 0;

    static final int ERR_CHECKSUM = 1;

    static final int ERR_INCOMPLETE = 2;

    static final int ERR_TOO_LONG = 3;

    private Vector vCmd = new Vector();

    private static final String[] Empty_vCmd = {};

    private static final char[] EOL_BYTES = { '\015', '\012' };

    StringBuffer rec = new StringBuffer(256);

    public final boolean isConnected() {
        return (gpsPort != null) && gpsPort.isConnected();
    }

    public void sendPacket(final String p_Packet) {
        if (isConnected()) {
            if (GPS_DEBUG) {
                waba.sys.Vm.debug(">" + p_Packet);
            }
            int z_Index = p_Packet.length();
            byte z_Checksum = 0;
            int z_Result = 0;
            while (--z_Index >= 0) {
                z_Checksum ^= (byte) p_Packet.charAt(z_Index);
            }
            m_writeOngoing.down();
            rec.setLength(0);
            rec.append('$');
            rec.append(p_Packet);
            rec.append('*');
            rec.append(Convert.unsigned2hex(z_Checksum, 2));
            rec.append(EOL_BYTES);
            gpsPort.write(rec.toString());
            m_writeOngoing.up();
        }
    }

    public String[] getResponse() {
        boolean continueReading;
        boolean readAgain = true;
        int myError = ERR_NOERROR;
        final boolean skipError = true;
        continueReading = gpsPort.isConnected();
        if (gpsPort.debugActive()) {
            gpsPort.writeDebug("\r\n:" + Convert.toString(Vm.getTimeStamp()) + ":");
        }
        if (current_state == C_FOUND_STATE) {
            current_state = C_START_STATE;
        }
        while (continueReading) {
            while (continueReading && (read_buf_p < bytesRead)) {
                char c;
                c = (char) read_buf[read_buf_p++];
                switch(current_state) {
                    case C_EOL_STATE:
                        if (((c == 10) || (c == 13))) {
                            current_state = C_FOUND_STATE;
                            continueReading = false;
                            if (ignoreNMEA) {
                                continueReading = ((String) vCmd.items[0]).startsWith("GP");
                            }
                        } else {
                            current_state = C_ERROR_STATE;
                        }
                        break;
                    case C_FOUND_STATE:
                    case C_INITIAL_STATE:
                    case C_START_STATE:
                        vCmd.removeAllElements();
                        if (c == '$') {
                            current_state = C_FIELD_STATE;
                            cmd_buf_p = 0;
                            checksum = 0;
                        } else if (!((c == 10) || (c == 13))) {
                            if (current_state == C_START_STATE) {
                                myError = ERR_INCOMPLETE;
                                current_state = C_ERROR_STATE;
                            }
                        }
                        break;
                    case C_FIELD_STATE:
                        if ((c == 10) || (c == 13)) {
                            current_state = C_EOL_STATE;
                        } else if (c == '*') {
                            current_state = C_STAR_STATE;
                            vCmd.add(new String(cmd_buf, 0, cmd_buf_p));
                        } else if (c == ',') {
                            checksum ^= c;
                            vCmd.add(new String(cmd_buf, 0, cmd_buf_p));
                            cmd_buf_p = 0;
                        } else {
                            cmd_buf[cmd_buf_p++] = c;
                            checksum ^= c;
                        }
                        break;
                    case C_STAR_STATE:
                        if ((c == 10) || (c == 13)) {
                            current_state = C_ERROR_STATE;
                        } else if (((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                            if (c >= '0' && c <= '9') {
                                read_checksum = (c - '0') << 4;
                            } else if (c >= 'A' && c <= 'F') {
                                read_checksum = (c - 'A' + 10) << 4;
                            } else {
                                read_checksum = (c - 'a' + 10) << 4;
                            }
                            current_state = C_CHECKSUM_CHAR1_STATE;
                        } else {
                            myError = ERR_INCOMPLETE;
                            current_state = C_ERROR_STATE;
                        }
                        break;
                    case C_CHECKSUM_CHAR1_STATE:
                        if ((c == 10) || (c == 13)) {
                            myError = ERR_INCOMPLETE;
                            current_state = C_ERROR_STATE;
                        } else if (((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) {
                            if (c >= '0' && c <= '9') {
                                read_checksum += c - '0';
                            } else if (c >= 'A' && c <= 'F') {
                                read_checksum += c - 'A' + 10;
                            } else {
                                read_checksum += c - 'a' + 10;
                            }
                            if (read_checksum != checksum) {
                                myError = ERR_CHECKSUM;
                                current_state = C_ERROR_STATE;
                            }
                            current_state = C_EOL_STATE;
                        } else {
                            myError = ERR_INCOMPLETE;
                            current_state = C_ERROR_STATE;
                        }
                        break;
                    case C_ERROR_STATE:
                        if (((c == 10) || (c == 13))) {
                            current_state = C_START_STATE;
                        }
                }
                if (cmd_buf_p > (C_BUF_SIZE - 1)) {
                    myError = ERR_TOO_LONG;
                    current_state = C_ERROR_STATE;
                }
                if (current_state == C_ERROR_STATE) {
                    current_state = C_INITIAL_STATE;
                    vCmd.removeAllElements();
                    if (!skipError) {
                        continueReading = false;
                    }
                }
            }
            if (continueReading) {
                read_buf_p = 0;
                bytesRead = 0;
                if (isConnected()) {
                    if (readAgain) {
                        readAgain = false;
                        try {
                            int max = gpsPort.readCheck();
                            if (max > C_BUF_SIZE) {
                                max = C_BUF_SIZE;
                            }
                            if (max > 0) {
                                bytesRead = gpsPort.readBytes(read_buf, 0, max);
                            }
                        } catch (Exception e) {
                            bytesRead = 0;
                        }
                    }
                    if (bytesRead == 0) {
                        continueReading = false;
                    } else {
                        if (gpsPort.debugActive()) {
                            String q = "(" + Convert.toString(Vm.getTimeStamp()) + ")";
                            gpsPort.writeDebug(q.getBytes(), 0, q.length());
                            gpsPort.writeDebug(read_buf, 0, bytesRead);
                        }
                    }
                }
            }
        }
        if (myError == C_ERROR_STATE) {
            vCmd.removeAllElements();
        }
        if (current_state == C_FOUND_STATE) {
            return (String[]) vCmd.toObjectArray();
        } else {
            return null;
        }
    }

    /**
     * @return Returns the ignoreNMEA.
     */
    public boolean isIgnoreNMEA() {
        return ignoreNMEA;
    }

    /**
     * @param ignoreNMEA The ignoreNMEA to set.
     */
    public void setIgnoreNMEA(boolean ignoreNMEA) {
        this.ignoreNMEA = ignoreNMEA;
    }
}
