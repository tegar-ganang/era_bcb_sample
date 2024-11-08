package net.sourceforge.insim4j.client;

import java.nio.channels.SelectableChannel;
import net.sourceforge.insim4j.i18n.ExceptionMessages;
import net.sourceforge.insim4j.utils.FormatUtils;

public class ChangeRequest {

    private static final FormatUtils FORMAT_UTILS = FormatUtils.getInstance();

    public static final int REGISTER = 1;

    public static final int CHANGEOPS = 2;

    public static final int CANCEL = 3;

    private SelectableChannel fChannel;

    private int fType;

    private int fOps;

    /**
	 * Constructor.
	 * 
	 * @param pChannel
	 * @param pType
	 * @param pOps
	 * 
	 * @throws IllegalArgumentException
   *                 if channel is null
	 */
    public ChangeRequest(final SelectableChannel pChannel, final int pType, final int pOps) {
        setChannel(pChannel);
        setType(pType);
        setOps(pOps);
    }

    /**
	 * Setter.
	 * 
	 * @param pChannel the channel to set
	 * 
	 * @throws IllegalArgumentException
   *                 if channel is null
	 */
    private void setChannel(final SelectableChannel pChannel) {
        if (pChannel == null) {
            throw new IllegalArgumentException(FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNullType"), "Type of packet"));
        }
        fChannel = pChannel;
    }

    /**
	 * Setter.
	 * 
	 * @param pType the type to set
	 */
    private void setType(final int pType) {
        fType = pType;
    }

    /**
	 * Setter.
	 * 
	 * @param pOps the ops to set
	 */
    private void setOps(final int pOps) {
        fOps = pOps;
    }

    /**
	 * Getter.
	 * 
	 * @return the channel
	 */
    public synchronized SelectableChannel getChannel() {
        return fChannel;
    }

    /**
	 * Getter.
	 * 
	 * @return the type
	 */
    public synchronized int getType() {
        return fType;
    }

    /**
	 * Getter.
	 * 
	 * @return the ops
	 */
    public synchronized int getOps() {
        return fOps;
    }

    @Override
    public String toString() {
        return "Channel: " + getChannel() + ", Type: " + getType() + ", Ops: " + getOps();
    }
}
