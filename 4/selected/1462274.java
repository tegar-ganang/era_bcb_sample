package jhomenet.commons.hw;

import java.io.Serializable;

/**
 * This class represents a non-mutable hardware communication channel.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
	 * The channel number (zero-based).
	 */
    private Integer channelNum;

    /**
	 * The channel description.
	 */
    private String description;

    /**
	 * Default channel description.
	 */
    public static final String DEFAULT_DESC = "Default";

    /**
	 * Constructor.
	 * 
	 * @param channelNum The communication channel number
	 * @param desc A description of the communication channel
	 */
    public Channel(Integer channelNum, String desc) {
        super();
        if (channelNum == null) throw new IllegalArgumentException("Channel number cannot be null!");
        if (desc == null) this.description = DEFAULT_DESC; else this.description = desc;
        this.channelNum = channelNum;
    }

    /**
	 * Constructor.
	 * 
	 * @param hardwareAddrRef The hardware address reference
	 * @param channelNum The communication channel number
	 */
    public Channel(Integer channelNum) {
        this(channelNum, DEFAULT_DESC);
    }

    /**
	 * Private non-instantiable constructor (used primarily for persistence).
	 */
    private Channel() {
    }

    /**
	 * @return the channelNum
	 */
    public final Integer getChannelNum() {
        return channelNum;
    }

    /**
	 * 
	 * @param channelNum
	 */
    private final void setChannelNum(Integer channelNum) {
        this.channelNum = channelNum;
    }

    /**
	 * @return the description
	 */
    public final String getDescription() {
        return description;
    }

    /**
	 * @param description the description to set
	 */
    public final void setDescription(String description) {
        this.description = description;
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return "[CH-" + channelNum + ": " + getDescription() + "]";
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channelNum == null) ? 0 : channelNum.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        return result;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Channel other = (Channel) obj;
        if (channelNum == null) {
            if (other.channelNum != null) return false;
        } else if (!channelNum.equals(other.channelNum)) return false;
        if (description == null) {
            if (other.description != null) return false;
        } else if (!description.equals(other.description)) return false;
        return true;
    }
}
