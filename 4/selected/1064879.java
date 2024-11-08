package uk101.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import uk101.machine.Data;

/**
 * Utility class to take a stream of output bytes written by the UK101 and convert
 * them to Java output characters using a Java Writer.  This should create a file
 * that is viewable and can be edited easily on the PC.
 *
 * The conversion rules are:
 *
 * - Any standard ASCII character (i.e. code value 32 to 126, except for a
 *   backslash) is written directly to the output.
 *
 * - Any non-ASCII character (i.e. code <32 or >126 or a backslash) is written
 *   as a hex escape beginning with a backslash '\nn'.
 *
 * - The sequence CR <NUL>+ LF is written as a line end (where <NUL>+
 *   means one or more NUL characters).
 *
 * @author Baldwin
 */
public class UK101OutputStream extends OutputStream {

    PrintWriter outputWriter;

    boolean pendingNewline;

    public UK101OutputStream(Writer out) {
        this(new PrintWriter(out, true));
    }

    public UK101OutputStream(PrintWriter out) {
        outputWriter = out;
    }

    public UK101OutputStream(PrintStream out) {
        this(new OutputStreamWriter(out));
    }

    public void write(int ch) throws IOException {
        if (ch != 0x00) {
            if (ch == 0x0A && pendingNewline) {
                outputWriter.println();
                pendingNewline = false;
            } else {
                checkCR();
                if (ch == 0x0D) {
                    pendingNewline = true;
                } else if (ch > 31 && ch < 127 && ch != '\\') {
                    outputWriter.write(ch);
                } else {
                    outputWriter.write("\\" + Data.toHexString((byte) ch));
                }
            }
        }
    }

    public void flush() throws IOException {
        checkCR();
        outputWriter.flush();
    }

    public void close() throws IOException {
        checkCR();
        outputWriter.close();
    }

    void checkCR() {
        if (pendingNewline) {
            outputWriter.write("\\0D");
            pendingNewline = false;
        }
    }

    public void write(InputStream input) throws IOException {
        for (int ch = input.read(); ch != -1; ch = input.read()) write(ch);
        input.close();
    }
}
