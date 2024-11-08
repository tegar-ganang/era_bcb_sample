package transport.packet;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Packet implements Serializable, Cloneable {

    private static transient Log LOG = LogFactory.getLog(Packet.class);

    private PacketTypeEnum type;

    private long serial;

    private long createDate = System.currentTimeMillis();

    protected Object payload;

    private InetAddress ip;

    private int port;

    protected transient int channelID;

    public Packet(PacketTypeEnum t, Object o) {
        type = t;
        payload = o;
    }

    public void setChannelID(int id) {
        channelID = id;
    }

    public int getChannelID() {
        return channelID;
    }

    public PacketTypeEnum getType() {
        return type;
    }

    public long getSerial() {
        return serial;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("type=");
        buf.append(type);
        buf.append("; serial=");
        buf.append(Long.toString(serial));
        buf.append("; created=");
        buf.append(new Date(createDate));
        return buf.toString();
    }

    public void setPayload(Object o) {
        payload = o;
    }

    public Object getPayload() {
        return payload;
    }

    public synchronized byte[] toBytes() {
        byte[] bytes;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            bytes = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            bytes = new byte[0];
        }
        return bytes;
    }

    public boolean isType(PacketTypeEnum t) {
        return t.equals(type);
    }

    public static Packet fromBytes(byte[] bytes) throws MalformedPacketException {
        Packet new_packet = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            new_packet = (Packet) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new MalformedPacketException(e.toString());
        } catch (IOException e) {
            throw new MalformedPacketException(e.toString());
        }
        return new_packet;
    }

    /**
     * @return
     */
    public Calendar getCreateDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(createDate);
        return cal;
    }

    /**
     * @param serial
     */
    public void setSerial(long serial) {
        this.serial = serial;
    }

    /**
     * @param type
     */
    public void setType(PacketTypeEnum type) {
        this.type = type;
    }

    public void setIP(InetAddress ia) {
        ip = ia;
    }

    public InetAddress getIP() {
        return ip;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPort() {
        return port;
    }
}
