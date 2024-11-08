package net.sf.jncu.fdil;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;
import net.sf.jncu.dil.InvalidParameterException;

/**
 * Newton Streamed Object Format - Large Binary Object.<br>
 * Also known as Virtual Binary Object (VBO), or BLOB.
 * <p>
 * A large binary object mimics the functionality of a virtual binary object
 * (VBO). It contains a large amount of unformatted binary data, that is paged
 * in from a backing store, and optionally compressed.
 * <p>
 * A compandor/compander (compressor-expander) is an object that transparently
 * compresses data as it is stored and expands data as it is read.
 * 
 * @author Moshe
 */
public class NSOFLargeBinary extends NSOFBinaryObject {

    /** Default large binary class. */
    public static final NSOFSymbol CLASS_LARGE_BINARY = new NSOFSymbol("largeBinary");

    /**
	 * Specifies the use of the Lempel-Ziv compressor-expander.
	 */
    public static final String COMPANDER_LZ = "TLZStoreCompander";

    /**
	 * Specifies the use of a compander specialized for pixel map data. (A
	 * bitmap is a pixel map having a bit depth of 1.) This compander assumes
	 * that the data in the VBO is a pixel map and that the pixel map data is
	 * 32-bit aligned; that is, the length of the rows in the pixel map is an
	 * even multiple of 4 bytes.
	 */
    public static final String COMPANDER_PIXELMAP = "TPixelMapCompander";

    /** Data is not compressed. */
    protected static final byte UNCOMPRESSED = 0;

    /** Data is compressed. */
    protected static final byte COMPRESSED = 1;

    private boolean compressed = false;

    /**
	 * A string value specifying the implementation of the store compander
	 * protocol used when the VBO created by this object is written to or read
	 * from a soup entry. If the value of this parameter is nil, an uncompressed
	 * object is created.
	 * 
	 * @see #COMPANDER_LZ
	 * @see #COMPANDER_PIXELMAP
	 */
    private String companderName;

    /**
	 * Arguments for instantiating the specified compander. In the current
	 * implementation, always pass {@code nil} as the value of this parameter.
	 * <p>
	 * "Because both companders provided by the current system initialize
	 * themselves automatically, you must always pass {@code nil} as the value
	 * of the {@code companderArgs} parameter."
	 */
    private byte[] companderArgs;

    /**
	 * Constructs a new large binary object.
	 */
    public NSOFLargeBinary() {
        super();
        setObjectClass(CLASS_LARGE_BINARY);
    }

    @Override
    public void inflate(InputStream in, NSOFDecoder decoder) throws IOException {
        NSOFSymbol symbol = (NSOFSymbol) decoder.inflate(in);
        setObjectClass(symbol);
        int compressed = in.read();
        if (compressed == -1) {
            throw new EOFException();
        }
        setCompressed(compressed != UNCOMPRESSED);
        int numBytesData = ntohl(in);
        int numBytesCompanderName = ntohl(in);
        int numBytesCompanderArgs = ntohl(in);
        ntohl(in);
        if (numBytesCompanderName == 0) {
            setCompanderName(null);
        } else {
            byte[] companderName = new byte[numBytesCompanderName];
            readAll(in, companderName);
            setCompanderName(new String(companderName));
        }
        if (numBytesCompanderArgs == 0) {
            setCompanderArguments(null);
        } else {
            byte[] companderArgs = new byte[numBytesCompanderArgs];
            readAll(in, companderArgs);
            setCompanderArguments(companderArgs);
        }
        if (numBytesData == 0) {
            setValue((Blob) null);
        } else {
            byte[] data = new byte[numBytesData];
            readAll(in, data);
            Blob blob = null;
            try {
                blob = new SerialBlob(data);
                setValue(blob);
            } catch (SerialException se) {
                throw new IOException(se);
            } catch (SQLException se) {
                throw new IOException(se);
            }
        }
    }

    @Override
    public void flatten(OutputStream out, NSOFEncoder encoder) throws IOException {
        out.write(NSOF_LARGE_BINARY);
        String companderName = getCompanderName();
        byte[] companderNameBytes = (companderName == null) ? null : companderName.getBytes();
        byte[] args = getCompanderArguments();
        Blob data = getBlob();
        int numBytesData = 0;
        if (data != null) {
            try {
                numBytesData = (int) data.length();
            } catch (SQLException se) {
                throw new IOException(se);
            }
        }
        encoder.flatten(getObjectClass(), out);
        out.write(isCompressed() ? COMPRESSED : UNCOMPRESSED);
        htonl(numBytesData, out);
        htonl((companderNameBytes == null) ? 0 : companderNameBytes.length, out);
        htonl((args == null) ? 0 : args.length, out);
        htonl(0, out);
        if (companderNameBytes != null) {
            out.write(companderNameBytes);
        }
        if (args != null) {
            out.write(args);
        }
        if (data != null) {
            InputStream in;
            try {
                in = data.getBinaryStream();
            } catch (SQLException se) {
                throw new IOException(se);
            }
            for (int i = 0; i < numBytesData; i++) out.write(in.read());
        }
    }

    /**
	 * Is the data compressed?
	 * 
	 * @return compressed?
	 */
    public boolean isCompressed() {
        return compressed;
    }

    /**
	 * Set data compression.
	 * 
	 * @param compressed
	 *            compress?
	 */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    /**
	 * Get the compander name.
	 * 
	 * @return the compander name.
	 */
    public String getCompanderName() {
        return companderName;
    }

    /**
	 * Set the compander name.
	 * 
	 * @param companderName
	 *            the compander name.
	 */
    public void setCompanderName(String companderName) {
        this.companderName = companderName;
    }

    /**
	 * Get the compander arguments/parameters.
	 * 
	 * @return the compander arguments.
	 */
    public byte[] getCompanderArguments() {
        return companderArgs;
    }

    /**
	 * Set the compander arguments/parameters.
	 * 
	 * @param companderArgs
	 *            the compander arguments.
	 */
    public void setCompanderArguments(byte[] companderArgs) {
        this.companderArgs = companderArgs;
    }

    public Blob getBlob() {
        Blob blob = null;
        try {
            blob = new SerialBlob(getValue());
        } catch (SerialException se) {
            throw new InvalidParameterException(se);
        } catch (SQLException se) {
            throw new InvalidParameterException(se);
        }
        return blob;
    }

    /**
	 * Set the value.
	 * 
	 * @param value
	 *            the value.
	 */
    public void setBlob(Blob value) {
        byte[] buf;
        try {
            buf = value.getBytes(0, (int) value.length());
        } catch (SQLException se) {
            throw new InvalidParameterException(se);
        }
        setValue(buf);
    }

    /**
	 * Set the value.
	 * 
	 * @param value
	 *            the value.
	 */
    public void setValue(Blob value) {
        setBlob(value);
    }

    @Override
    public void setObject(NSOFObject value) {
        super.setObject(null);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        NSOFLargeBinary copy = new NSOFLargeBinary();
        copy.setObjectClass(this.getObjectClass());
        copy.setValue(this.getValue());
        copy.companderArgs = this.companderArgs;
        copy.companderName = this.companderName;
        copy.compressed = this.compressed;
        return copy;
    }

    @Override
    public NSOFObject deepClone() throws CloneNotSupportedException {
        NSOFLargeBinary copy = new NSOFLargeBinary();
        copy.setObjectClass(this.getObjectClass());
        byte[] value = this.getValue();
        if (value != null) {
            byte[] value2 = new byte[value.length];
            System.arraycopy(value, 0, value2, 0, value.length);
            copy.setValue(value2);
        }
        copy.companderArgs = this.companderArgs;
        copy.companderName = this.companderName;
        copy.compressed = this.compressed;
        return copy;
    }
}
