package net.sourceforge.plantuml.code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class CompressionHuffman implements Compression {

    public byte[] compress(byte[] in) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Deflater deflater = new Deflater(Deflater.HUFFMAN_ONLY);
        deflater.setLevel(9);
        final DeflaterOutputStream gz = new DeflaterOutputStream(baos, deflater);
        try {
            gz.write(in);
            gz.close();
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    public byte[] decompress(byte[] in) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayInputStream bais = new ByteArrayInputStream(in);
        final InflaterInputStream gz = new InflaterInputStream(bais);
        int read;
        while ((read = gz.read()) != -1) {
            baos.write(read);
        }
        gz.close();
        bais.close();
        baos.close();
        return baos.toByteArray();
    }
}
