package com.qarks.util.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipperInputStream extends InputStream {

    private boolean eof = false;

    private InputStream streamToZip;

    private CorePipedInputStream pipeIn;

    private CorePipedOutputStream pipeOut;

    private ZipOutputStream zos;

    private byte array[];

    public ZipperInputStream(InputStream streamToZip) throws IOException {
        this.streamToZip = streamToZip;
        pipeIn = new CorePipedInputStream();
        pipeOut = new CorePipedOutputStream(pipeIn);
        zos = new ZipOutputStream(pipeOut);
        zos.putNextEntry(new ZipEntry("content"));
        array = new byte[1024];
    }

    public int read() throws IOException {
        int result;
        while (pipeIn.available() == 0 && !eof) {
            int nbread = streamToZip.read(array);
            if (nbread == -1) {
                eof = true;
                zos.closeEntry();
            } else {
                zos.write(array, 0, nbread);
            }
        }
        if (eof && pipeIn.available() == 0) {
            result = -1;
        } else {
            result = pipeIn.read();
        }
        return result;
    }
}
