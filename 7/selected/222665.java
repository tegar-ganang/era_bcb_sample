package communication;

/**
 * Representation of a Packet
 * 
 * @author Marcus Fredriksson
 * @author updated by Assar Lindstrom, (added Node setters and getters)
 * 
 *         Headers NEXTMASTER=0 COORD=1 DIRECTION=2
 */
public abstract class Packet {

    public static Packet fromByteArray(byte[] arr) {
        switch(arr[0]) {
            case NEXTMASTER:
                return new NextMasterPacket(arr);
            case COORD:
                return new CoordPacket(arr);
            case DIRECTION:
                return new DirectionPacket(arr);
            default:
                System.out.println("Unknown packet type " + arr[0] + "!");
                return null;
        }
    }

    public static final byte NEXTMASTER = (byte) 0;

    public static final byte COORD = (byte) 1;

    public static final byte DIRECTION = (byte) 2;

    private Node sender, receiver;

    byte header;

    byte[] data;

    public Packet(byte type) {
        this.header = type;
        this.data = new byte[3];
    }

    /**
	 * Constructs an empty packet
	 */
    public Packet(byte type, byte[] data) {
        this.header = type;
        this.data = new byte[3];
        for (int i = 0; i < 3; i++) {
            this.data[i] = data[i];
        }
    }

    /**
	 * Constructs a packet from an byte array
	 * 
	 * @param byte array of size 4 containing header in first byte and data in
	 *        the rest
	 */
    public Packet(byte[] arr) {
        this.header = arr[0];
        this.data = new byte[3];
        for (int i = 0; i < 3; i++) {
            this.data[i] = arr[i + 1];
        }
    }

    /**
	 * 
	 * @return Returns the header of a general packet
	 */
    public byte getPacketType() {
        return this.header;
    }

    /**
	 * 
	 * @param Packet
	 *            P
	 * @return A Packet to byte array that is of size 4
	 */
    public byte[] toByteArray() {
        byte[] sendable = new byte[4];
        sendable[0] = this.header;
        for (int i = 1; i < 4; i++) {
            sendable[i] = this.data[i - 1];
        }
        return sendable;
    }

    /**
	 * 
	 * @return
	 */
    public Node getSender() {
        return sender;
    }

    /**
	 * 
	 * @return
	 */
    public Node getReceiver() {
        return receiver;
    }

    /**
	 * 
	 * @param sender
	 */
    public void setSender(Node sender) {
        this.sender = sender;
    }

    /**
	 * 
	 * @param receiver
	 */
    public void setReceiver(Node receiver) {
        this.receiver = receiver;
    }
}
