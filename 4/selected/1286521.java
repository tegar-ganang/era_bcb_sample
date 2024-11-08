package deltree.file;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Stack;
import org.xml.sax.Attributes;

/**
 * @author demangep
 * 
 * This generate a XML file without using an XML parser
 * Some other implementation could write on a different stream 
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class XmlChannelWriter {

    public static final String header = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>";

    private Stack tagStack;

    private boolean INDEV = true;

    static final int BUFFER_SIZE = 512;

    FileChannel destC;

    ByteBuffer bbuffer;

    CharBuffer buffer;

    CharsetEncoder encoder;

    /**
	 * Creates a new XmlWriter for giving file
	 * @param filename
	 */
    public XmlChannelWriter(String filename) throws FileNotFoundException, IOException {
        FileOutputStream dest = new FileOutputStream(filename);
        destC = dest.getChannel();
        bbuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        buffer = CharBuffer.allocate(BUFFER_SIZE);
        encoder = Charset.forName("ISO-8859-1").newEncoder();
        tagStack = new Stack();
        buffer.put(header);
        flush();
    }

    /**
	 * Writes current charBuffer to disk
	 * @throws IOException
	 */
    private void flush() throws IOException {
        buffer.flip();
        CoderResult result = encoder.encode(buffer, bbuffer, false);
        bbuffer.flip();
        destC.write(bbuffer);
        buffer.compact();
        bbuffer.compact();
    }

    /**
	 * Close all needed tag for gived level
	 * if level==1 close all tags
	 * Warning, this raises an EmptyStackException if level is < 1
	 * This is not tested because it's onl used in private mode.
	 * @param level
	 * @throws IOException
	 */
    private void closeTags(int level) throws IOException {
        if (tagStack.isEmpty()) return;
        while (level <= tagStack.size()) {
            if (this.INDEV) {
                for (int i = 1; i < tagStack.size(); i++) buffer.put("\t");
            }
            buffer.put("</");
            String tmp = (String) tagStack.pop();
            buffer.put(tmp);
            buffer.put(">");
            if (this.INDEV) buffer.put("\n");
        }
        flush();
    }

    /**
	 * Main API to use with this class:
	 * Writes a tag with given attributes. The xml tree is managed by user,  by using the "level" param.
	 * Level 1 is the root, level 2 are childs of root etc.
	 * @param level
	 * @param tag
	 * @param att
	 */
    public void writeTag(int level, String tag, Attributes att) throws Exception {
        if (level < 1) throw new Exception("Invalid tree level in xml generator:" + tag + " " + level);
        if (level > (tagStack.size() + 1)) throw new Exception("Invalid tree level in xml generator:" + tag + " " + level);
        closeTags(level);
        tagStack.push(tag);
        if (this.INDEV) {
            for (int i = 1; i < level; i++) buffer.put("\t");
        }
        buffer.put("<");
        buffer.put(tag);
        if (att != null) {
            for (int i = 0; i < att.getLength(); i++) {
                buffer.put(" ");
                buffer.put(att.getLocalName(i));
                buffer.put("=");
                buffer.put(att.getValue(i));
            }
        }
        buffer.put(">");
        if (this.INDEV) buffer.put("\n");
        flush();
    }

    /**
	 * Must be called by the user of this class at the end of xml generation
	 * this close all opened tag, flushes the encoder, and write it on disk.
	 * @throws IOException
	 */
    public void close() throws IOException {
        closeTags(1);
        this.encoder.encode(buffer, bbuffer, true);
        this.encoder.flush(bbuffer);
        destC.write(bbuffer);
        destC.close();
    }

    public static void main(String[] argv) {
        try {
            XmlChannelWriter test = new XmlChannelWriter("c:\\tmp\\tset.xml");
            test.writeTag(1, "base", null);
            test.writeTag(2, "test2", null);
            test.writeTag(2, "test2", null);
            test.writeTag(3, "test3", null);
            test.writeTag(3, "test3", null);
            test.writeTag(3, "test3", null);
            test.writeTag(2, "test2", null);
            test.close();
            ;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
