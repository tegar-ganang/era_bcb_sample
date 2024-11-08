package gpsxml.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author PLAYER, Keith Ralph
 */
public class IOUtility {

    private static final char[] INDENT = "\n         ".toCharArray();

    /** Creates a new instance of IOUtility */
    public IOUtility() {
    }

    /**
     * Fast file copy method.
     */
    public static void copy(File sourceFile, File destinationFile) throws IOException {
        FileChannel sourceFileChannel = (new FileInputStream(sourceFile)).getChannel();
        FileChannel destinationFileChannel = (new FileOutputStream(destinationFile)).getChannel();
        sourceFileChannel.transferTo(0, sourceFile.length(), destinationFileChannel);
        sourceFileChannel.close();
        destinationFileChannel.close();
    }

    public static void writeSimpleSAXTag(TransformerHandler hd, AttributesImpl atts, String tagName, String text, int indentAmount) throws Exception {
        if (text == null || text.equals("")) {
            if (atts.getLength() < 1) {
                return;
            }
        }
        hd.ignorableWhitespace(INDENT, 0, indentAmount);
        hd.startElement("", "", tagName, atts);
        if (text != null && !text.equals("")) {
            char[] chars = text.toCharArray();
            hd.characters(chars, 0, chars.length);
        }
        hd.endElement("", "", tagName);
    }
}
