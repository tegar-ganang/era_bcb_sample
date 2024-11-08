package ant.antsys;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

/**
 * grep class to grep file content
 * @author johnson ma
 */
class Grep {

    private static Charset charset = Charset.forName("ISO-8859-15");

    private static CharsetDecoder decoder = charset.newDecoder();

    private static Pattern linePattern = Pattern.compile(".*\r?\n");

    private static String grep(File f, CharBuffer cb, Pattern pattern) {
        Matcher lm = linePattern.matcher(cb);
        Matcher pm = null;
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();
            if (pm == null) pm = pattern.matcher(cs); else pm.reset(cs);
            if (pm.find()) return (f + ":" + lines + ":" + cs);
            if (lm.end() == cb.limit()) break;
        }
        return null;
    }

    public static String grep(File f, String regExp) throws IOException {
        FileChannel fc = null;
        try {
            Pattern pattern = Pattern.compile(regExp);
            FileInputStream fis = new FileInputStream(f);
            fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            CharBuffer cb = decoder.decode(bb);
            return grep(f, cb, pattern);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (Exception e) {
                }
                fc = null;
            }
        }
    }
}
