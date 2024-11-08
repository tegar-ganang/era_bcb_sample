import java.io.*;
import java.util.*;
import java.lang.reflect.*;

class LFileCopy {

    static void copy(String src, String dest) throws IOException {
        File ifp = new File(src);
        File ofp = new File(dest);
        if (ifp.exists() == false) {
            throw new IOException("file '" + src + "' does not exist");
        }
        FileInputStream fis = new FileInputStream(ifp);
        FileOutputStream fos = new FileOutputStream(ofp);
        byte[] b = new byte[1024];
        while (fis.read(b) > 0) fos.write(b);
        fis.close();
        fos.close();
    }

    public static void main(String[] args) {
        int len = Array.getLength(args);
        System.out.println("copying '" + args[0] + "' to '" + args[1] + "'");
        try {
            copy(args[0], args[1]);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
