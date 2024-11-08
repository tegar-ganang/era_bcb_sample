package org.apache.xmlbeans.impl.common;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.Reader;
import java.io.FileWriter;
import java.io.FileReader;
import java.net.URI;
import java.nio.channels.FileChannel;

public class IOUtil {

    public static void copyCompletely(InputStream input, OutputStream output) throws IOException {
        if ((output instanceof FileOutputStream) && (input instanceof FileInputStream)) {
            try {
                FileChannel target = ((FileOutputStream) output).getChannel();
                FileChannel source = ((FileInputStream) input).getChannel();
                source.transferTo(0, Integer.MAX_VALUE, target);
                source.close();
                target.close();
                return;
            } catch (Exception e) {
            }
        }
        byte[] buf = new byte[8192];
        while (true) {
            int length = input.read(buf);
            if (length < 0) break;
            output.write(buf, 0, length);
        }
        try {
            input.close();
        } catch (IOException ignore) {
        }
        try {
            output.close();
        } catch (IOException ignore) {
        }
    }

    public static void copyCompletely(Reader input, Writer output) throws IOException {
        char[] buf = new char[8192];
        while (true) {
            int length = input.read(buf);
            if (length < 0) break;
            output.write(buf, 0, length);
        }
        try {
            input.close();
        } catch (IOException ignore) {
        }
        try {
            output.close();
        } catch (IOException ignore) {
        }
    }

    public static void copyCompletely(URI input, URI output) throws IOException {
        try {
            InputStream in = null;
            try {
                File f = new File(input);
                if (f.exists()) in = new FileInputStream(f);
            } catch (Exception notAFile) {
            }
            File out = new File(output);
            File dir = out.getParentFile();
            dir.mkdirs();
            if (in == null) in = input.toURL().openStream();
            IOUtil.copyCompletely(in, new FileOutputStream(out));
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot copy to " + output);
        }
    }

    public static File createDir(File rootdir, String subdir) {
        File newdir = (subdir == null) ? rootdir : new File(rootdir, subdir);
        boolean created = (newdir.exists() && newdir.isDirectory()) || newdir.mkdirs();
        assert (created) : "Could not create " + newdir.getAbsolutePath();
        return newdir;
    }
}
