package freemind.main;

import java.io.*;
import java.util.Enumeration;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Create a new XHTMLWriter which is able to save a HTMLDocument as XHTML.
 * <p>
 * The result will be a valid XML file, but it is not granted that the file
 * will really be XHTML 1.0 transitional conformous. The basic purpose of
 * this class is to give an XSL processor access to plain HTML files.
 *
 * @author    Richard "Shred" K�rber
 */
public class XHTMLWriter extends FixedHTMLWriter {

    private final MutableAttributeSet convAttr = new SimpleAttributeSet();

    private boolean writeLineSeparatorEnabled = true;

    /**
   * Create a new XHTMLWriter that will write the entire HTMLDocument.
   *
   * @param   writer        Writer to write to
   * @param   doc           Source document
   */
    public XHTMLWriter(Writer writer, HTMLDocument doc) {
        this(writer, doc, 0, doc.getLength());
    }

    /**
   * Create a new XHTMLWriter that will write a part of a HTMLDocument.
   *
   * @param   writer        Writer to write to
   * @param   doc           Source document
   * @param   pos           Starting position
   * @param   len           Length
   */
    public XHTMLWriter(Writer writer, HTMLDocument doc, int pos, int len) {
        super(new XHTMLFilterWriter(writer), doc, pos, len);
        setLineLength(Integer.MAX_VALUE);
    }

    /**
   * Start the writing process. An XML and DOCTYPE header will be written
   * prior to the XHTML output.
   */
    public void write() throws IOException, BadLocationException {
        super.write();
    }

    protected void writeOption(Option option) throws IOException {
        writeLineSeparatorEnabled = false;
        super.writeOption(option);
        writeLineSeparatorEnabled = true;
        write("</option>");
        writeLineSeparator();
    }

    protected void writeLineSeparator() throws IOException {
        if (writeLineSeparatorEnabled) super.writeLineSeparator();
    }

    /**
   * Read HTML from the Reader, and send XHTML to the writer. Common mistakes
   * in the HTML code will also be corrected. The result is pretty-printed.
   *
   * @param   reader      HTML source
   * @param   writer      XHTML target
   */
    public static void html2xhtml(Reader reader, Writer writer) throws IOException, BadLocationException {
        HTMLEditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();
        kit.read(reader, doc, doc.getLength());
        XHTMLWriter xhw = new XHTMLWriter(writer, (HTMLDocument) doc);
        xhw.write();
    }

    /**
   * External call to convert a source HTML file to a target XHTML file.
   * <p>
   * Usage: <tt>java XHTMLWriter &lt;source file&gt; &lt;target file&gt;</tt>
   *
   * @param   args        Shell arguments
   */
    public static void main(String[] args) {
        try {
            FileReader reader = new FileReader(args[0]);
            FileWriter writer = new FileWriter(args[1]);
            html2xhtml(reader, writer);
            writer.close();
            reader.close();
        } catch (Exception e) {
            freemind.main.Resources.getInstance().logException(e);
        }
    }

    /**
   * This FilterWriter will convert the output of Swing's HTMLWriter to
   * XHTML format. This is done by converting tags like &lt;br&gt; to
   * &lt;br&nbsp;/&gt;. Also, special characters in tag attributes are
   * escaped.
   * <p>
   * This filter relies on known flaws of the HTMLWriter. It is known to
   * work with Java 1.4, but might not work with future Java releases.
   */
    public static class XHTMLFilterWriter extends FilterWriter {

        private boolean insideTag = false;

        private boolean insideValue = false;

        private boolean readTag = false;

        private String tag = "";

        /**
     * Create a new XHTMLFilterWriter.
     *
     * @param   writer        Writer to write to
     */
        public XHTMLFilterWriter(Writer writer) {
            super(writer);
        }

        /**
     * Write a single char to the Writer.
     *
     * @param   c             Char to be written
     */
        public void write(int c) throws IOException {
            if (insideValue) {
                if (c == '&') {
                    super.write("&amp;", 0, 5);
                    return;
                } else if (c == '<') {
                    super.write("&lt;", 0, 4);
                    return;
                } else if (c == '>') {
                    super.write("&gt;", 0, 4);
                    return;
                } else if (c == '"') {
                    insideValue = false;
                }
            } else if (insideTag) {
                if (readTag) {
                    if (c == ' ' || c == '>') {
                        readTag = false;
                    } else {
                        tag += (char) c;
                    }
                }
                if (c == '"') {
                    insideValue = true;
                } else if (c == '>') {
                    if (tag.equals("img") || tag.equals("br") || tag.equals("hr") || tag.equals("input") || tag.equals("meta") || tag.equals("link") || tag.equals("area") || tag.equals("base") || tag.equals("basefont") || tag.equals("frame") || tag.equals("iframe") || tag.equals("col")) {
                        super.write(" /");
                    }
                    insideTag = false;
                    readTag = false;
                }
            } else if (c == '<') {
                tag = "";
                insideTag = true;
                readTag = true;
            }
            super.write(c);
        }

        /**
     * Write a char array to the Writer.
     *
     * @param   cbuf        Char array to be written
     * @param   off         Start offset within the array
     * @param   len         Number of chars to be written
     */
        public void write(char[] cbuf, int off, int len) throws IOException {
            while (len-- > 0) {
                write((int) cbuf[off++]);
            }
        }

        /**
     * Write a String to the Writer.
     *
     * @param   str         String to be written
     * @param   off         Start offset within the String
     * @param   len         Number of chars to be written
     */
        public void write(String str, int off, int len) throws IOException {
            write(str.toCharArray(), off, len);
        }
    }
}
