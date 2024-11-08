package bluetoothgateway;

import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * This parser decodes a byte stream from the bluetooth sensor modules. This 
 * class follows the implementation of FrameParser2 of the DScope project by 
 * Daniel Roggen.
 * 
 *	A frame-based data stream has the following format:
 *
 *	<FRAMEHEADER><data0><data1><data2><data3>...(<CHECKSUM><FRAMEFOOTER>)
 *
 *	With :
 *		FRAMEHEADER:	A set of characters delimiting the start of the frame.
 *		data0...n		A set of payload data, each of which may be in a different data format.
 *		CHECKSUM:		Not implemented - typically a frame checksum to ensure.
 *		FRAMEFOOTER:	Not implemented - optional - delimits frame end - The checksum or a longer header can play this role.
 *
 *		The 'format' string defines the frame header and data format as follows:
 *			format: "FRAMEHEADER;<data0><data1><data2>..."
 *		The frame header is delimited by a semicolon from the definition of the data types <data0><data1>....
 *		Data types have the following format:
 *			<datan>: [-]cÂ¦sÂ¦SÂ¦iÂ¦I
 *			-:		indicates that the next item is signed; by default unsigned is assumed.	
 *			c:		8-bit character
 *			s:		16-bit short, little endian
 *			S:		16-bit short, big endian
 *			i:		32-bit int, little endian
 *			I:		32-bit int, big endian
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class FrameParser extends Observable implements StreamDecoder {

    private String m_sensorname;

    private String[] m_channels;

    private String m_format;

    private static final char CHAR_8BIT_SIGNED = 'c';

    private static final char SHORT_16BIT_LITTLE_ENDIAN_SIGNED = 's';

    private static final char SHORT_16BIT_BIG_ENDIAN_SIGNED = 'S';

    private static final char INT_32BIT_LITTLE_ENDIAN_SIGNED = 'i';

    private static final char INT_32BIT_BIG_ENDIAN_SIGNED = 'I';

    private static final char CHAR_8BIT_UNSIGNED = 'd';

    private static final char SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED = 't';

    private static final char SHORT_16BIT_BIG_ENDIAN_UNSIGNED = 'T';

    private static final char INT_32BIT_LITTLE_ENDIAN_UNSIGNED = 'j';

    private static final char INT_32BIT_BIG_ENDIAN_UNSIGNED = 'J';

    private byte[] m_frame;

    private byte[] m_header;

    private int m_error;

    public static final int ERROR_NONE = 0;

    public static final int ERROR_FRAMEFORMAT = 1;

    public FrameParser(String sensorname, String format, String[] channels) {
        setFormat(format);
        m_sensorname = sensorname;
        m_channels = channels;
    }

    /**
    * Returns the last error that occurred.
    * @return one of the FrameParser.ERROR_* values
    */
    public int getLastError() {
        return m_error;
    }

    /**
    * Parses the format string and initializes buffers
    */
    private void setFormat(String format) {
        int header_sep = format.indexOf(";");
        if (header_sep == -1 || header_sep == 0) {
            System.out.println("FrameParser: format header type invalid");
            m_error = ERROR_FRAMEFORMAT;
            return;
        }
        int framebytes = 0;
        m_header = format.substring(0, header_sep).getBytes();
        framebytes += m_header.length;
        boolean bSigned = false;
        m_format = "";
        for (int i = header_sep + 1; i < format.length(); i++) {
            char type = format.charAt(i);
            switch(type) {
                case '-':
                    if (i == format.length() - 1) {
                        System.out.println("FrameParser: found '-' without following type");
                        m_error = ERROR_FRAMEFORMAT;
                        return;
                    }
                    bSigned = true;
                    continue;
                case 'c':
                    m_format += bSigned ? CHAR_8BIT_SIGNED : CHAR_8BIT_UNSIGNED;
                    framebytes += 1;
                    break;
                case 's':
                    m_format += bSigned ? SHORT_16BIT_LITTLE_ENDIAN_SIGNED : SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED;
                    framebytes += 2;
                    break;
                case 'S':
                    m_format += bSigned ? SHORT_16BIT_BIG_ENDIAN_SIGNED : SHORT_16BIT_BIG_ENDIAN_UNSIGNED;
                    framebytes += 2;
                    break;
                case 'i':
                    m_format += bSigned ? INT_32BIT_LITTLE_ENDIAN_SIGNED : INT_32BIT_LITTLE_ENDIAN_UNSIGNED;
                    framebytes += 4;
                    break;
                case 'I':
                    m_format += bSigned ? INT_32BIT_BIG_ENDIAN_SIGNED : INT_32BIT_BIG_ENDIAN_UNSIGNED;
                    framebytes += 4;
                    break;
                default:
                    System.out.println("FrameParser: Unknown type in format");
                    m_error = ERROR_FRAMEFORMAT;
                    return;
            }
            bSigned = false;
        }
        System.out.println("FrameParser: format read successfully: " + m_format);
        m_frame = new byte[framebytes];
    }

    /**
    * Reports bytes read from a stream. This function will try to synchronize to 
    * establish a frame, which is then decoded.
    * 
    * @param b the byte read.
    */
    public void read(byte[] bytes) {
        if (m_error != 0) return;
        for (int i = 0; i < bytes.length; i++) {
            shiftFrame(bytes[i]);
            if (!isHeaderLeading()) continue;
            Date recDate = new Date();
            Vector framedata = new Vector(m_format.length());
            int framepos = m_header.length;
            for (int j = 0; j < m_format.length(); j++) {
                switch(m_format.charAt(j)) {
                    case CHAR_8BIT_SIGNED:
                        framedata.add(new Character((char) m_frame[framepos++]));
                        break;
                    case SHORT_16BIT_LITTLE_ENDIAN_SIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Short((short) (a | (b << 8))));
                            break;
                        }
                    case SHORT_16BIT_BIG_ENDIAN_SIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Short((short) (b | (a << 8))));
                            break;
                        }
                    case INT_32BIT_LITTLE_ENDIAN_SIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short c = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short d = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Integer(a | (b << 8) | (c << 16) | (d << 24)));
                            break;
                        }
                    case INT_32BIT_BIG_ENDIAN_SIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short c = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short d = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Integer(d | (c << 8) | (b << 16) | (a << 24)));
                            break;
                        }
                    case CHAR_8BIT_UNSIGNED:
                        framedata.add(new Character((char) m_frame[framepos++]));
                        break;
                    case SHORT_16BIT_LITTLE_ENDIAN_UNSIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Short((short) (a | (b << 8))));
                            break;
                        }
                    case SHORT_16BIT_BIG_ENDIAN_UNSIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Short((short) (b | (a << 8))));
                            break;
                        }
                    case INT_32BIT_LITTLE_ENDIAN_UNSIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short c = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short d = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Integer(a | (b << 8) | (c << 16) | (d << 24)));
                            break;
                        }
                    case INT_32BIT_BIG_ENDIAN_UNSIGNED:
                        {
                            short a = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short b = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short c = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            short d = (short) ((short) 0xFF & (short) m_frame[framepos++]);
                            framedata.add(new Integer(d | (c << 8) | (b << 16) | (a << 24)));
                            break;
                        }
                    default:
                        System.out.println("PANIC: Encountered unknown symbol in frame format. Exiting");
                        System.exit(-1);
                }
            }
            DataPacket dp = new DataPacket(recDate, framedata, m_sensorname, m_channels);
            setChanged();
            notifyObservers(dp);
        }
    }

    private void shiftFrame(byte b) {
        for (int i = 0; i < m_frame.length - 1; i++) {
            m_frame[i] = m_frame[i + 1];
        }
        m_frame[m_frame.length - 1] = b;
    }

    /**
    * Checks whether m_frame starts with a 
    * @return
    */
    private boolean isHeaderLeading() {
        for (int i = 0; i < m_header.length; i++) {
            if (m_header[i] != m_frame[i]) {
                return false;
            }
        }
        return true;
    }

    /**
    * This is a test function for the class
    * @param args
    */
    public static void main(String[] args) {
        String format = "DX4;cs-s-s-s-s-s-s-s-s-s-s-s";
        byte[] data = { 'D', 'X', '4', 'a', (byte) 0x80, 0x01, 0x01, (byte) 0x80, 0x01, 0x02, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d };
        System.out.println("Running FrameParser test");
        System.out.println("Passing format: " + format);
        String[] channels = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" };
        FrameParser fp = new FrameParser("ch_test", format, channels);
        fp.addObserver(new Observer() {

            public void update(Observable o, Object arg) {
                System.out.println(((DataPacket) arg));
            }
        });
        System.out.println("Sending data");
        fp.read(data);
    }
}
