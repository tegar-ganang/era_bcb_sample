package org.jnetpcap.nio;

import java.nio.ByteBuffer;

/**
 * A peered number pointer class that stores and retrieves number values from
 * native/direct memory locations. This class facilitates exchange of number
 * values (from bytes to doubles) to various native functions. The key being
 * that these numbers at JNI level can be passed in as pointers and thus allows
 * natives methods to both send and receive values between native and java
 * space. The methods are named similarly like java.lang.Number class, with the
 * exception of existance of setter methods.
 * <p>
 * Typical usage for JNumber is to use it wherever a function requests a
 * primitive type pointer.
 * </p>
 * 
 * @author Mark Bednarczyk
 * @author Sly Technologies, Inc.
 */
@SuppressWarnings("unused")
public class JNumber extends JMemory {

    /**
	 * Used to request a specific type of primitive that this number will be
	 * dealing with possibly allocating memory more efficiently to fit the
	 * primitive type.
	 * 
	 * @author Mark Bednarczyk
	 * @author Sly Technologies, Inc.
	 */
    public enum Type {

        BYTE, CHAR, INT, SHORT, LONG, LONGLONG, FLOAT, DOUBLE;

        /**
		 * Size in bytes for this native type on this machine
		 */
        public final int size;

        private static int biggestSize = 0;

        Type() {
            size = JNumber.sizeof(ordinal());
        }

        public static int getBiggestSize() {
            if (biggestSize == 0) {
                for (Type t : values()) {
                    if (t.size > biggestSize) {
                        biggestSize = t.size;
                    }
                }
            }
            return biggestSize;
        }
    }

    @SuppressWarnings("unused")
    private static final int BYTE_ORDINAL = 0;

    private static final int CHAR_ORDINAL = 1;

    private static final int INT_ORDINAL = 2;

    private static final int SHORT_ORDINAL = 3;

    private static final int LONG_ORDINAL = 4;

    private static final int LONG_LONG_ORDINAL = 5;

    private static final int FLOAT_ORDINAL = 6;

    private static final int DOUBLE_ORDINAL = 7;

    private static final int MAX_SIZE_ORDINAL = 8;

    public JNumber() {
        super(Type.getBiggestSize());
    }

    /**
	 * Allocates a number of the specified size and type.
	 * 
	 * @param type
	 *          primitive type for which to allocate memory
	 */
    public JNumber(Type type) {
        super(type.size);
    }

    /**
	 * Creates a number pointer, which does not allocate any memory on its own,
	 * but needs to be peered with primitive pointer.
	 */
    public JNumber(JMemory.Type type) {
        super(type);
    }

    private static native int sizeof(int oridnal);

    public native int intValue();

    public native void intValue(int value);

    public native byte byteValue();

    public native void byteValue(byte value);

    public native short shortValue();

    public native void shortValue(short value);

    public native long longValue();

    public native void longValue(long value);

    public native float floatValue();

    public native void floatValue(float value);

    public native double doubleValue();

    public native void doubleValue(double value);

    public int peer(JNumber number) {
        return super.peer(number);
    }

    public int peer(JBuffer buffer) {
        return super.peer(buffer, 0, size());
    }

    public int peer(JBuffer buffer, int offset) {
        return super.peer(buffer, offset, size());
    }

    public int transferFrom(ByteBuffer peer) {
        return super.transferFrom(peer);
    }
}
