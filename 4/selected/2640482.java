package org.effdom.me;

import org.effdom.me.impl.DocumentImpl;
import org.effdom.me.io.DocumentReader;
import org.effdom.me.io.DocumentWriter;
import org.effdom.me.io.impl.DocParserImpl;

/**
 * The <code>DocumentFactory</code> is the entry point for creating
 * <code>Document</code> and document readers and writers.
 * 
 * @author <a href="mailto:mattias@effcode.com">Mattias Jonsson</a>
 * 
 * @see Document
 */
public class DocumentFactory {

    public static byte COMPRESS_TYPE_NONE = 50;

    public static byte COMPRESS_TYPE_GZIP = 51;

    private DocumentFactory() {
    }

    /**
     * Creates and instance of the <code>DocumentFactory</code>.
     * 
     * @return an instance of the <code>DocumentFactory</code>
     */
    public static DocumentFactory createInstance() {
        return new DocumentFactory();
    }

    /**
     * Creates an empty <code>Document.</code>
     * 
     * @return the default <code>Document</code> implementation
     */
    public Document createDocument() {
        return new DocumentImpl();
    }

    /**
     * Creates an empty <code>Document</code> of the specified version.
     * 
     * @param version the wanted <code>Document</code>
     * @return the <code>Document</code>
     * @throws UnsupportedOperationException if the version isn't supported
     */
    public Document createDocument(byte version) throws UnsupportedOperationException {
        switch(version) {
            case DocParserImpl.VERSION:
                return new DocumentImpl();
            default:
                throw new UnsupportedOperationException("Only supports version: " + DocParserImpl.VERSION);
        }
    }

    /**
     * Creates a <code>DocumentReader</code>
     * @return
     * @throws UnsupportedOperationException
     */
    public DocumentReader createDocumentReader() throws UnsupportedOperationException {
        return new DocParserImpl(COMPRESS_TYPE_NONE);
    }

    public DocumentReader createDocumentReader(byte compressType) throws UnsupportedOperationException {
        return new DocParserImpl(compressType);
    }

    public DocumentWriter createDocumentWriter() throws UnsupportedOperationException {
        return new DocParserImpl(COMPRESS_TYPE_NONE);
    }

    public DocumentWriter createDocumentWriter(byte compressType) throws UnsupportedOperationException {
        if (compressType != COMPRESS_TYPE_NONE) {
            throw new UnsupportedOperationException("Compression is not supported for document writer, only for document reader");
        }
        return new DocParserImpl(compressType);
    }
}
