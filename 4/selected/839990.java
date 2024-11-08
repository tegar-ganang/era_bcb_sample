package zipperSwing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JProgressBar;

public class Zip {

    static final int BUFFER = 2048;

    public void zipFile(final File childfile, final ZipOutputStream out, final byte[] data) {
        System.out.println("Adding: " + childfile.getName());
        FileInputStream fi;
        try {
            fi = new FileInputStream(childfile);
            final BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
            final ZipEntry entry = new ZipEntry(childfile.getName());
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) out.write(data, 0, count);
            origin.close();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void zipDirectory(final File childfile, final ZipOutputStream out, final byte[] data, final JProgressBar t) {
        final File files[] = childfile.listFiles();
        t.setMaximum(files.length);
        int i = 1;
        for (final File children : files) {
            if (children.isFile()) {
                zipFile(children, out, data);
            } else {
                System.err.println(children.getName() + "is Directory\n" + "This Program cant zip Directory/Directory");
            }
            t.setValue(i++);
        }
    }
}
