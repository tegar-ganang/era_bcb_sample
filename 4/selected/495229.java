package com.jxva.dao.type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author  The Jxva Framework Foundation
 * @since   1.0
 * @version 2009-03-19 16:29:51 by Jxva
 */
public class ByteArray {

    private final InputStream in;

    public ByteArray(InputStream in) {
        this.in = in;
    }

    public byte[] detach() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int readed = in.read(buffer);
        while (readed > -1) {
            bos.write(buffer, 0, readed);
            readed = in.read(buffer);
        }
        in.close();
        bos.flush();
        return bos.toByteArray();
    }
}
