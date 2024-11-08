package medieveniti.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PlugInUtils {

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                delete(sub);
            }
        }
        f.delete();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int read = 0;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void copy(File src, File target) throws IOException {
        System.out.println("copy " + src.getPath() + " -> " + target.getPath());
        if (src.isFile()) {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(target);
            copy(in, out);
            in.close();
            out.close();
        } else if (src.isDirectory()) {
            target.mkdirs();
            for (File f : src.listFiles()) {
                String diff = f.getPath().substring(src.getPath().length());
                copy(new File(src.getPath() + diff), new File(target.getPath() + diff));
            }
        }
    }
}
