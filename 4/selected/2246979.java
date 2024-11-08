package org.tranche.configuration;

/**
 * <p>Simple byte flags for server mode (permissions such as read, write). Mode information applies to all clients, and must be enforced.</p>
 * <p>Note that this is a byte. Hence:</p>
 * <ul>
 *  <li>Eight bits for storing up to eight distinct flags. (Right now, only 'read' and 'write'.)</li>
 *  <li>Up to 256 ways to combine the 'elementary' distinct flags above into compound flags. Right now, there four: none, read-only, write-only, and read/write.</li>
 * </ul>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ServerModeFlag {

    /**
     * <p>Flag: server data can be read by clients, but cannot write data to server.</p>
     */
    public static final byte CAN_READ = (1 << 0);

    /**
     * <p>Flag: clients can write data to server, but cannot read it.</p>
     */
    public static final byte CAN_WRITE = (1 << 1);

    /**
     * <p>Flag: clients cannot read nor write data from nor to server.</p>
     */
    public static final byte NONE = 0;

    /**
     * <p>Flag: clients can write and read data to and from server.</p>
     */
    public static final byte CAN_READ_WRITE = CAN_READ + CAN_WRITE;

    /**
     * <p>Convert server mode flag byte to String representation.</p>
     * @param flag
     * @return
     */
    public static final String toString(byte flag) {
        switch(flag) {
            case NONE:
                return "none";
            case CAN_READ:
                return "can read";
            case CAN_WRITE:
                return "can write";
            case CAN_READ_WRITE:
                return "can read and write";
            default:
                throw new RuntimeException("Unrecognized flag: " + flag);
        }
    }

    /**
     * <p>Based on flag byte, can client read data from server?</p>
     * @param flag
     * @return Ture if and only if read bit is set in flag
     */
    public static boolean canRead(byte flag) {
        return (flag & CAN_READ) == CAN_READ;
    }

    /**
     * <p>Based on flag byte, can client write data to server?</p>
     * @param flag
     * @return True if and only if write bit is set in flag
     */
    public static boolean canWrite(byte flag) {
        return (flag & CAN_WRITE) == CAN_WRITE;
    }
}
