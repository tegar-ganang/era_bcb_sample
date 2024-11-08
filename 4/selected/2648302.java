package installer;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzip {

    private static final boolean DEBUG = false;

    public Unzip() {
    }

    public static void main(String args[]) throws IOException {
        unzip(args);
    }

    public static void unzip(String s) throws IOException {
        StringTokenizer stringtokenizer = new StringTokenizer(s);
        Vector vector = new Vector();
        for (; stringtokenizer.hasMoreTokens(); vector.add(stringtokenizer.nextToken())) ;
        unzip((String[]) vector.toArray(new String[1]));
    }

    /**
     * Unzips a file.
     * @param zipinputstream
     * @param s relative name of the file to unzip
     * @param s1 base output directory 
     */
    public static void unzip(ZipInputStream zipinputstream, String s, String s1) throws IOException {
        if (s.endsWith("/")) {
            File file = (new File(s1 + File.separator + s + "dummy")).getParentFile();
            file.mkdirs();
            return;
        }
        FileOutputStream fileoutputstream = new FileOutputStream(s1 + File.separator + s);
        byte abyte0[] = new byte[512];
        for (int i = 0; (i = zipinputstream.read(abyte0)) != -1; ) fileoutputstream.write(abyte0, 0, i);
        fileoutputstream.close();
    }

    public static void unzip(String as[]) throws IOException {
        BufferedInputStream bufferedinputstream = new BufferedInputStream(new FileInputStream(as[0]));
        ZipInputStream zipinputstream = new ZipInputStream(bufferedinputstream);
        String s = ".";
        if (as.length == 2) {
            if (as[1].startsWith("-C")) {
                s = as[1].substring(2);
                ZipEntry zipentry;
                while ((zipentry = zipinputstream.getNextEntry()) != null) unzip(zipinputstream, zipentry.getName(), s);
            } else {
                ZipEntry zipentry1;
                for (; (zipentry1 = zipinputstream.getNextEntry()) != null; unzip(zipinputstream, zipentry1.getName(), s)) {
                    if (as.length <= 1 || !zipentry1.getName().equals(as[1])) continue;
                    unzip(zipinputstream, as[1], s);
                    break;
                }
            }
            zipinputstream.close();
        }
    }
}
