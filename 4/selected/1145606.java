package guie.util;

import javax.swing.JTextArea;
import java.io.*;
import java.util.regex.Pattern;

/**
 * A thread that connects an input stream to a text area.
 *
 * @author Carlos Rueda
 * @version $Id: IOThread.java,v 1.4 2008/06/28 01:32:53 carueda Exp $
 */
public class IOThread extends Thread {

    private InputStream is;

    private OutputStream os;

    private BufferedReader br;

    private JTextArea textArea;

    /** the textArea won't get (approx) bigger than this size. */
    private int ta_maxSize = 1000 * 1000;

    /** Approximate size that will be kept if textArea gets bigger than ta_maxSize */
    private int ta_keepSize = ta_maxSize / 4;

    /** Size to search for line when keeping textArea to approx ta_keepSize 
     * so the text gets adjusted on line boundaries. */
    private int ta_searchLineSize = 512;

    /** Creates a IOThread to connect an input stream to an output stream. */
    public IOThread(InputStream is, OutputStream os) {
        super("IOThread");
        this.is = is;
        this.os = os;
    }

    /** Creates a IOThread to connect an input stream to a text area. */
    public IOThread(InputStream is, JTextArea textArea) {
        super("IOThread");
        br = new BufferedReader(new InputStreamReader(is), 32 * 1024);
        this.textArea = textArea;
    }

    public void run() {
        try {
            if (os != null) {
                assert is != null;
                assert br == null;
                assert textArea == null;
                byte[] buffer = new byte[256];
                for (; ; ) {
                    int numread = is.read(buffer, 0, buffer.length);
                    if (numread < 0) {
                        break;
                    }
                    os.write(buffer, 0, numread);
                }
            } else {
                assert br != null;
                assert textArea != null;
                char[] buffer = new char[256];
                for (; ; ) {
                    int numread = br.read(buffer, 0, buffer.length);
                    if (numread < 0) {
                        break;
                    }
                    String s = new String(buffer, 0, numread);
                    writeString(s);
                }
            }
        } catch (IOException e) {
        }
    }

    /** Effective last line. */
    private String effectiveLastLine = "";

    private static final String prefix = "";

    protected int pos;

    /** Pattern to separate lines for both Unix (\n) and Windows (\r\n).
	  * Note that the Unix one should be also effective for MacOS X */
    private static Pattern lf_pattern = Pattern.compile("(\n|\r\n)");

    /**
	 * Appends a \n to the text area.
	 */
    private void ta_append_lf() {
        textArea.append("\n" + prefix);
        textArea.setCaretPosition(textArea.getText().length());
        effectiveLastLine = "";
    }

    /**
	 * \b and \r processing.
	 * No prefix management at all.
	 */
    private String process_br(String line) {
        line = line.substring(1 + line.lastIndexOf('\r'));
        String line2;
        do {
            line2 = line;
            line = line2.replaceAll("[^\b]\b", "");
        } while (!line.equals(line2));
        line = line.replaceAll("\b", "");
        if (line.indexOf('\r') >= 0 || line.indexOf('\b') >= 0) throw new RuntimeException(line);
        return line;
    }

    /**
	 * Writes a line to the text area.
	 * Called by writeString().
	 * Returns the new caret position.
	 */
    private int writeStringToCurrentLine(String s) {
        effectiveLastLine = process_br(effectiveLastLine + s);
        String text = textArea.getText();
        int idx = 1 + text.lastIndexOf('\n');
        String taLastLine = text.substring(idx);
        if (taLastLine.startsWith(prefix)) {
            idx += prefix.length();
            taLastLine = taLastLine.substring(prefix.length());
        }
        if (effectiveLastLine.length() <= taLastLine.length()) textArea.replaceRange(effectiveLastLine, idx, idx + effectiveLastLine.length()); else textArea.replaceRange(effectiveLastLine, idx, text.length());
        return idx + effectiveLastLine.length();
    }

    /**
	 * Writes a string to the text area.
	 */
    private void writeString(String s) {
        int caret = 0;
        String[] lines = lf_pattern.split(s, -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                ta_append_lf();
            }
            String text = textArea.getText();
            if (text.length() >= ta_maxSize) {
                int begIndex = text.length() - ta_keepSize;
                int idx = 1 + text.lastIndexOf('\n', begIndex);
                if (begIndex - idx <= ta_searchLineSize) {
                    begIndex = idx;
                } else {
                    idx = 1 + text.indexOf('\n', begIndex);
                    if (idx - begIndex <= ta_searchLineSize) {
                        begIndex = idx;
                    }
                }
                text = text.substring(begIndex);
                textArea.setText(text);
            }
            caret = writeStringToCurrentLine(lines[i]);
        }
        textArea.setCaretPosition(caret);
        pos = caret;
    }
}
