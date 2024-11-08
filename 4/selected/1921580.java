package org.xaware.server.engine.channel;

import org.xaware.server.engine.IChannelKey;

/**
 * This class represents the key to uniquely identify a local Channel instance.
 * This implementation simply wraps a String as the backing datastore for the key.
 * 
 * @author Tim Uttormark
 */
public class LocalChannelKey implements IChannelKey {

    /** IChannelKey type */
    private static final Type TYPE = IChannelKey.Type.LOCAL;

    /** The String representing the underlying datastore for this key */
    private final String keyString;

    /**
     * Construct a new instance.
     * 
     * @param keyString
     *            a String representing the unique key for the Channel.
     */
    public LocalChannelKey(String keyString) {
        this.keyString = keyString;
    }

    /**
     * Get the channel key type for this channel key instance.
     * 
     * @return the IChannelKey.Type for this IChannelKey instance.
     */
    public Type getChannelKeyType() {
        return TYPE;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LocalChannelKey) && keyString.equals(((LocalChannelKey) obj).keyString);
    }

    /**
     * Returns a hash code value for the object.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return keyString.hashCode();
    }

    /**
     * Returns a String representation for the object
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return keyString;
    }
}
