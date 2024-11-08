package net.sourceforge.plantuml.code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionGZip implements Compression {

    class MyGZIPOutputStream extends GZIPOutputStream {

        public MyGZIPOutputStream(OutputStream baos) throws IOException {
            super(baos);
            def.setLevel(9);
        }
    }

    public byte[] compress(byte[] in) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final GZIPOutputStream gz = new MyGZIPOutputStream(baos);
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
        final GZIPInputStream gz = new GZIPInputStream(bais);
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
