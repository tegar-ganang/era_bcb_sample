package com.sun.pdfview.decode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFParseException;

/**
 * decode a deFlated byte array
 * @author Mike Wessler
 */
public class FlateDecode {

    /**
     * decode a byte buffer in Flate format.
     * <p>
     * Flate is a built-in Java algorithm.  It's part of the java.util.zip
     * package.
     *
     * @param buf the deflated input buffer
     * @param params parameters to the decoder (unused)
     * @return the decoded (inflated) bytes
     */
    public static ByteBuffer decode(PDFObject dict, ByteBuffer buf, PDFObject params) throws IOException {
        Inflater inf = new Inflater(false);
        int bufSize = buf.remaining();
        byte[] data = new byte[bufSize];
        buf.get(data);
        inf.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] decomp = new byte[bufSize];
        int loc = 0;
        int read = 0;
        try {
            while (!inf.finished()) {
                read = inf.inflate(decomp);
                if (read <= 0) {
                    if (inf.needsDictionary()) {
                        throw new PDFParseException("Don't know how to ask for a dictionary in FlateDecode");
                    } else {
                        return ByteBuffer.allocate(0);
                    }
                }
                baos.write(decomp, 0, read);
            }
        } catch (DataFormatException dfe) {
            throw new PDFParseException("Data format exception:" + dfe.getMessage());
        }
        ByteBuffer outBytes = ByteBuffer.wrap(baos.toByteArray());
        if (params != null && params.getDictionary().containsKey("Predictor")) {
            Predictor predictor = Predictor.getPredictor(params);
            if (predictor != null) {
                outBytes = predictor.unpredict(outBytes);
            }
        }
        return outBytes;
    }
}
