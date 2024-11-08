package webserg.pazzlers.ch5;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamField {

    static void copy(String scr, String dest) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(scr);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        } finally {
            closeIgnoringException(in);
            closeIgnoringException(out);
        }
    }

    private static void closeIgnoringException(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            File path = new File("c:\\devel\\workingSnap\\webserg\\pazzlers\\ch5\\");
            if (path.exists()) copy(path.getPath() + File.separator + "src.txt", path.getPath() + File.separator + "dest.txt"); else System.out.println("wrong path");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
