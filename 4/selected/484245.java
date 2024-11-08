package examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import net.sf.japi.io.args.ArgParser;
import net.sf.japi.io.args.BasicCommand;
import net.sf.japi.io.args.Option;
import static net.sf.japi.io.args.OptionType.REQUIRED;
import org.jetbrains.annotations.NotNull;

/**
 * Recode is a Java program that recodes files from one encoding to another.
 * Warning: Unlike its UNIX pendants, this program changes permissions and eventually ownership of the processed files.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class Recode extends BasicCommand {

    /** Size of the internal buffer for performing the Recode. */
    private static final int BUF_SIZE = 4096;

    /** The InputEncoding to use. */
    private String inputEncoding;

    /** The OutputEncoding to use. */
    private String outputEncoding;

    /**
     * Sets the input encoding to use.
     * @param inputEncoding Input encoding to use.
     */
    @Option(value = { "i", "inputEncoding" }, type = REQUIRED)
    public void setInputEncoding(final String inputEncoding) {
        this.inputEncoding = inputEncoding;
        Charset.forName(inputEncoding);
    }

    /**
     * Sets the output encoding to use.
     * @param outputEncoding Output encoding to use.
     */
    @Option(value = { "o", "outputEncoding" }, type = REQUIRED)
    public void setOutputEncoding(final String outputEncoding) {
        this.outputEncoding = outputEncoding;
        Charset.forName(outputEncoding);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "InstanceMethodNamingConvention", "ProhibitedExceptionDeclared" })
    public int run(@NotNull final List<String> args) throws Exception {
        int returnCode = 0;
        if (args.size() == 0) {
            recode(System.in, System.out);
        } else {
            for (final String filename : args) {
                try {
                    recode(filename);
                } catch (final IOException e) {
                    returnCode = 1;
                    System.err.println(e);
                }
            }
        }
        return returnCode;
    }

    /**
     * Recodes a File from the specified input encoding to the specified output encoding.
     * @param filename Filename of the file to recode
     * @throws IOException In case of I/O problems.
     */
    public void recode(final String filename) throws IOException {
        final File file = new File(filename);
        final File tmpFile = File.createTempFile("recode", null, file.getParentFile());
        final InputStream in = new FileInputStream(file);
        try {
            final OutputStream out = new FileOutputStream(tmpFile);
            try {
                recode(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        try {
            copy(tmpFile, file);
        } finally {
            if (!tmpFile.delete()) {
                System.err.println("Couldn't delete temporary file " + tmpFile);
            }
        }
    }

    /**
     * Recodes a Stream and writes the data to another Stream.
     * @param in InputStream to recode
     * @param out OutputStream to recode
     * @throws IOException In case of I/O problems.
     */
    public void recode(final InputStream in, final OutputStream out) throws IOException {
        @SuppressWarnings({ "IOResourceOpenedButNotSafelyClosed" }) final Reader cin = new InputStreamReader(in, inputEncoding);
        @SuppressWarnings({ "IOResourceOpenedButNotSafelyClosed" }) final Writer cout = new OutputStreamWriter(out, outputEncoding);
        final char[] buf = new char[BUF_SIZE];
        for (int charsRead; (charsRead = cin.read(buf)) != -1; ) {
            cout.write(buf, 0, charsRead);
        }
        cout.flush();
    }

    /**
     * Copies form one file to another.
     * @param source  File to copy.
     * @param dest    File to copy to.
     * @throws IOException In case of I/O problems.
     */
    public void copy(final File source, final File dest) throws IOException {
        final FileInputStream in = new FileInputStream(source);
        try {
            final FileOutputStream out = new FileOutputStream(dest);
            try {
                final FileChannel inChannel = in.getChannel();
                final FileChannel outChannel = out.getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Main method.
     * @param args Command line arguments
     */
    public static void main(final String... args) {
        ArgParser.simpleParseAndRun(new Recode(), args);
    }
}
