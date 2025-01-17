package org.exist.xquery.value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.util.io.MemoryMappedFileFilterInputStreamCache;
import org.exist.xquery.XPathException;

/**
 * Representation of an XSD binary value e.g. (xs:base64Binary or xs:hexBinary)
 * whose source is backed by an InputStream
 *
 * @author Adam Retter <adam@existsolutions.com>
 */
public class BinaryValueFromInputStream extends BinaryValue {

    private static final Logger LOG = Logger.getLogger(BinaryValueFromInputStream.class);

    private final CachingFilterInputStream is;

    private MemoryMappedFileFilterInputStreamCache cache;

    protected BinaryValueFromInputStream(BinaryValueManager manager, BinaryValueType binaryValueType, InputStream is) throws XPathException {
        super(manager, binaryValueType);
        try {
            this.cache = new MemoryMappedFileFilterInputStreamCache();
            this.is = new CachingFilterInputStream(cache, is);
        } catch (IOException ioe) {
            throw new XPathException(ioe.getMessage(), ioe);
        }
        is.mark(Integer.MAX_VALUE);
    }

    public static BinaryValueFromInputStream getInstance(BinaryValueManager manager, BinaryValueType binaryValueType, InputStream is) throws XPathException {
        BinaryValueFromInputStream binaryInputStream = new BinaryValueFromInputStream(manager, binaryValueType, is);
        manager.registerBinaryValueInstance(binaryInputStream);
        return binaryInputStream;
    }

    @Override
    public BinaryValue convertTo(BinaryValueType binaryValueType) throws XPathException {
        BinaryValueFromInputStream binaryInputStream = new BinaryValueFromInputStream(getManager(), binaryValueType, new CachingFilterInputStream(is));
        getManager().registerBinaryValueInstance(binaryInputStream);
        return binaryInputStream;
    }

    @Override
    public void streamBinaryTo(OutputStream os) throws IOException {
        try {
            int read = -1;
            byte data[] = new byte[READ_BUFFER_SIZE];
            while ((read = is.read(data)) > -1) {
                os.write(data, 0, read);
            }
        } finally {
            try {
                is.reset();
            } catch (IOException ioe) {
                LOG.error("Unable to reset stream: " + ioe.getMessage(), ioe);
            }
        }
    }

    @Override
    public InputStream getInputStream() {
        return new CachingFilterInputStream(is);
    }

    @Override
    public void close() throws IOException {
        try {
            if (cache != null) {
                cache.invalidate();
            }
        } finally {
            is.close();
        }
    }
}
