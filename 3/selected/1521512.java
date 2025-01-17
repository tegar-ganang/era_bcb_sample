package com.googlecode.ehcache.annotations.key;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Dalquist
 * @version $Revision: 656 $
 */
public class MessageDigestCacheKeyGenerator extends AbstractHashingCacheKeyGenerator<MessageDigestOutputStream, String> {

    /**
     * Name of the bean this generator is registered under using the default constructor.
     */
    public static final String DEFAULT_BEAN_NAME = "com.googlecode.ehcache.annotations.key.MessageDigestCacheKeyGenerator.DEFAULT_BEAN_NAME";

    public static final String DEFAULT_ALGORITHM = "SHA-1";

    protected static final int DEFAULT_BYTE_BUFFER_SIZE = 64;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MessageDigest baseMessageDigest;

    private boolean cloneNotSupported;

    /**
     * Uses {@link #DEFAULT_ALGORITHM} for the algorithm
     * @see AbstractCacheKeyGenerator#AbstractCacheKeyGenerator() 
     */
    public MessageDigestCacheKeyGenerator() throws NoSuchAlgorithmException {
        this(DEFAULT_ALGORITHM);
    }

    /**
     * @see AbstractCacheKeyGenerator#AbstractCacheKeyGenerator() 
     */
    public MessageDigestCacheKeyGenerator(String algorithm) throws NoSuchAlgorithmException {
        this.baseMessageDigest = MessageDigest.getInstance(algorithm);
    }

    /**
     * Uses {@link #DEFAULT_ALGORITHM} for the algorithm
     * @see AbstractCacheKeyGenerator#AbstractCacheKeyGenerator(boolean, boolean) 
     */
    public MessageDigestCacheKeyGenerator(boolean includeMethod, boolean includeParameterTypes) throws NoSuchAlgorithmException {
        this(DEFAULT_ALGORITHM, includeMethod, includeParameterTypes);
    }

    /**
     * @see AbstractCacheKeyGenerator#AbstractCacheKeyGenerator(boolean, boolean) 
     */
    public MessageDigestCacheKeyGenerator(String algorithm, boolean includeMethod, boolean includeParameterTypes) throws NoSuchAlgorithmException {
        super(includeMethod, includeParameterTypes);
        this.baseMessageDigest = MessageDigest.getInstance(algorithm);
    }

    @Override
    public MessageDigestOutputStream getGenerator(Object... data) {
        final MessageDigest messageDigest = this.getMessageDigest();
        return new MessageDigestOutputStream(messageDigest);
    }

    @Override
    public String generateKey(MessageDigestOutputStream generator) {
        final MessageDigest messageDigest = generator.getMessageDigest();
        final byte[] digest = messageDigest.digest();
        return this.encodeHash(digest);
    }

    /**
     * Encode the digested hash bytes as a String
     */
    protected String encodeHash(final byte[] digest) {
        return Base64.encodeBase64URLSafeString(digest);
    }

    /**
     * Tries to clone the {@link MessageDigest} that was created during construction. If the clone fails
     * that is remembered and from that point on new {@link MessageDigest} instances will be created on
     * every call.
     * 
     * @return Generates a {@link MessageDigest} to use during a call to {@link #generateKey(Object...)}
     */
    protected MessageDigest getMessageDigest() {
        if (this.cloneNotSupported) {
            final String algorithm = this.baseMessageDigest.getAlgorithm();
            try {
                return MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MessageDigest algorithm '" + algorithm + "' was supported when " + this.getClass().getSimpleName() + " was created but is not now. This should not be possible.", e);
            }
        }
        try {
            return (MessageDigest) this.baseMessageDigest.clone();
        } catch (CloneNotSupportedException e) {
            this.cloneNotSupported = true;
            this.logger.warn("Could not clone MessageDigest using algorithm '" + this.baseMessageDigest.getAlgorithm() + "'. MessageDigest.getInstance will be used from now on which will be much more expensive.", e);
            return this.getMessageDigest();
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, boolean[] a) {
        for (final boolean element : a) {
            generator.writeBoolean(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, byte[] a) {
        generator.write(a);
    }

    @Override
    protected void append(MessageDigestOutputStream generator, char[] a) {
        for (final char element : a) {
            generator.writeChar(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, double[] a) {
        for (final double element : a) {
            generator.writeDouble(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, float[] a) {
        for (final float element : a) {
            generator.writeFloat(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, int[] a) {
        for (final int element : a) {
            generator.writeInt(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, long[] a) {
        for (final long element : a) {
            generator.writeLong(element);
        }
    }

    @Override
    protected void append(MessageDigestOutputStream generator, short[] a) {
        for (final short element : a) {
            generator.writeShort(element);
        }
    }

    @Override
    protected void appendGraphCycle(MessageDigestOutputStream generator, Object o) {
        generator.write(0);
    }

    @Override
    protected void appendNull(MessageDigestOutputStream generator) {
        generator.write(0);
    }

    @Override
    protected void appendHash(MessageDigestOutputStream generator, Object e) {
        if (e instanceof String) {
            generator.writeUTF((String) e);
        } else if (e instanceof Boolean) {
            generator.writeBoolean((Boolean) e);
        } else if (e instanceof Byte) {
            generator.writeByte((Byte) e);
        } else if (e instanceof Character) {
            generator.writeChar((Character) e);
        } else if (e instanceof Double) {
            generator.writeDouble((Double) e);
        } else if (e instanceof Float) {
            generator.writeFloat((Float) e);
        } else if (e instanceof Integer) {
            generator.writeInt((Integer) e);
        } else if (e instanceof Long) {
            generator.writeLong((Long) e);
        } else if (e instanceof Short) {
            generator.writeShort((Short) e);
        } else {
            generator.writeInt(e.hashCode());
        }
    }
}
