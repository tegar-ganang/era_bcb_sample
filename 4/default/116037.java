import java.awt.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;

public final class GovZipOutputStream extends ZipOutputStream {

    public GovZipOutputStream(OutputStream o) {
        super(o);
    }

    public synchronized void writeFile(String filename) throws IOException {
        int amount;
        byte buffer[] = new byte[4096];
        File f = new File(filename);
        FileInputStream in = new FileInputStream(f);
        putNextEntry(new ZipEntry(f.getName()));
        while ((amount = in.read(buffer)) != -1) write(buffer, 0, amount);
        closeEntry();
        in.close();
    }

    public synchronized void writeFile(String filename, String target) throws IOException {
        int amount;
        byte buffer[] = new byte[4096];
        FileInputStream in = new FileInputStream(filename);
        putNextEntry(new ZipEntry(target));
        while ((amount = in.read(buffer)) != -1) write(buffer, 0, amount);
        closeEntry();
        in.close();
    }
}
