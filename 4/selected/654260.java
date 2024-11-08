package elliott803.telecode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Utility class to take a stream of telecode characters and write them as
 * Java characters using a Writer.  The telecode characters are converted
 * to Java characters according to the rules in the TelecodeToChar class.
 *
 * To handle line ends the telecode [CR] character is ignored and the telecode
 * [LF] is written as a line end.  This is not quite perfect but works well for
 * standard text data.
 *
 * @author Baldwin
 */
public class TelecodeOutputStream extends OutputStream {

    Writer outputWriter;

    TelecodeToChar converter;

    byte[] bb = new byte[1];

    char[] cc = new char[1];

    public TelecodeOutputStream(Writer out, boolean useASCII) {
        this(new BufferedWriter(out), useASCII);
    }

    public TelecodeOutputStream(BufferedWriter out, boolean useASCII) {
        outputWriter = out;
        converter = new TelecodeToChar(useASCII);
    }

    public TelecodeOutputStream(PrintWriter out, boolean useASCII) {
        outputWriter = out;
        converter = new TelecodeToChar(useASCII);
    }

    public TelecodeOutputStream(PrintStream out, boolean useASCII) {
        this(new PrintWriter(new OutputStreamWriter(out), true), useASCII);
    }

    public void write(int tc) throws IOException {
        if (tc == Telecode.TELE_LF) {
            if (outputWriter instanceof BufferedWriter) ((BufferedWriter) outputWriter).newLine(); else ((PrintWriter) outputWriter).println();
        } else if (tc != Telecode.TELE_CR) {
            bb[0] = (byte) tc;
            if (converter.convert(bb, 1, cc) > 0) outputWriter.write(cc[0]);
        }
    }

    public void flush() throws IOException {
        outputWriter.flush();
    }

    public void close() throws IOException {
        outputWriter.close();
    }

    public void write(InputStream input) throws IOException {
        for (int ch = input.read(); ch != -1; ch = input.read()) write(ch);
        input.close();
    }
}
