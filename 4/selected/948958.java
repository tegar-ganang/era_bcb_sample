package to_do_o.core.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author Ruediger Gad
 *
 */
public class FileUtil {

    public static void deleteDir(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] childs = file.listFiles();
                if (childs == null || childs.length == 0) {
                    file.delete();
                } else {
                    for (int i = 0; i < childs.length; i++) {
                        deleteDir(childs[i]);
                    }
                }
            } else {
                file.delete();
            }
        }
    }

    public static void writeInputStreamToFile(InputStream in, String fileName) throws IOException {
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        if (read >= 0) {
            File file = new File(fileName);
            if (!file.exists()) {
                File dir = file.getParentFile();
                if (dir != null && !dir.exists()) {
                    dir.mkdirs();
                }
                file.createNewFile();
            }
            OutputStream out = new FileOutputStream(file);
            while (read >= 0) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
            out.flush();
            out.close();
        }
    }
}
