package jsystem.treeui.client;

import java.io.File;
import java.io.FileInputStream;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This utility class provides a subset of the grep functionality.
 * 
 * @author Guy Chen
 */
public class GrepLog {

    private static Charset charset = Charset.forName("ISO-8859-15");

    private static CharsetDecoder decoder = charset.newDecoder();

    private static Pattern linePattern = Pattern.compile(".*\r?\n");

    private static Pattern pattern;

    private static void compile(String pat) {
        try {
            pattern = Pattern.compile(pat);
        } catch (PatternSyntaxException x) {
            System.err.println(x.getMessage());
            System.exit(1);
        }
    }

    public boolean grep(File file, String strToSearch) {
        compile(strToSearch);
        boolean bFound = false;
        try {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            CharBuffer cb = decoder.decode(bb);
            Matcher lm = linePattern.matcher(cb);
            Matcher pm = null;
            int lines = 0;
            while (lm.find()) {
                lines++;
                CharSequence cs = lm.group();
                if (pm == null) pm = pattern.matcher(cs); else pm.reset(cs);
                if (pm.find()) {
                    System.out.print(file + ":" + lines + ":" + cs);
                    bFound = true;
                }
                if (lm.end() == cb.limit()) break;
            }
            fc.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            return bFound;
        }
        return bFound;
    }
}
