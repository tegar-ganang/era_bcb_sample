package pyrasun.eio.protocols.object;

import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.Externalizable;
import java.io.IOException;

public class Packet implements Externalizable {

    private String channel;

    private Object object;

    public static final int MAGIC = 0x8fa12ee;

    static final long serialVerionUID = 1;

    public Packet(final String channel, final Object object) {
        this.channel = channel;
        this.object = object;
    }

    public Packet() {
    }

    public String getChannel() {
        return (channel);
    }

    public Object getObject() {
        return (object);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(channel);
        out.writeObject(object);
    }

    public void readExternal(ObjectInput in) throws IOException {
        try {
            channel = in.readUTF();
            object = in.readObject();
        } catch (ClassNotFoundException cnfe) {
            throw new IOException("ERROR: ClassNotFoundException " + cnfe.toString());
        }
    }
}
