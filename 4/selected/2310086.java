package org.iosgi.outpost.operations;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import org.iosgi.outpost.Operation;

/**
 * @author Sven Schulz
 */
public class Put implements Operation<Void>, Serializable {

    private static final long serialVersionUID = 22698184740008990L;

    static byte[] toByteArray(File f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) f.length());
        FileInputStream fis = new FileInputStream(f);
        try {
            byte[] buf = new byte[1024 * 16];
            int read;
            while ((read = fis.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            baos.flush();
        } finally {
            baos.close();
            fis.close();
        }
        return baos.toByteArray();
    }

    private final File target;

    private final byte[] data;

    public Put(File target, byte[] data) {
        this.target = target;
        this.data = data;
    }

    public Put(File source, File target) throws IOException {
        this(target, toByteArray(source));
    }

    @Override
    public Void perform() throws Exception {
        FileOutputStream fos = new FileOutputStream(target);
        fos.write(data);
        fos.flush();
        fos.close();
        return null;
    }
}
