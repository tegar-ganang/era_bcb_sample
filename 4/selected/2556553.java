package dynamator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import org.w3c.tidy.Tidy;
import org.w3c.tidy.Configuration;

/**
    Interface to Tidy.
**/
public class HtmlToXml_Tidy implements HtmlToXml {

    private static final int defaultEncoding_ = Configuration.ASCII;

    private Tidy tidy_;

    private Configuration configuration_;

    public HtmlToXml_Tidy() {
        tidy_ = new Tidy();
        tidy_.setTidyMark(false);
        configuration_ = tidy_.getConfiguration();
        Properties properties = new Properties();
        properties.put("output-xml", "true");
        properties.put("tidy-mark", "false");
        properties.put("doctype", "omit");
        properties.put("numeric-entities", "true");
        properties.put("wrap", "0");
        properties.put("quiet", "true");
        configuration_.addProps(properties);
    }

    public void convert(String inputFilename, String outputFilename, PrintStream errorStream) throws IOException {
        InputStream input;
        OutputStream output;
        try {
            input = new BufferedInputStream(new FileInputStream(inputFilename));
            output = new BufferedOutputStream(new FileOutputStream(outputFilename));
        } catch (FileNotFoundException x) {
            throw new FatalError("File not found: " + x.getMessage());
        } catch (IOException x) {
            throw new FatalError("IO exception: " + x.getMessage());
        }
        convert_(input, output, errorStream, inputFilename, defaultEncoding_);
    }

    public void convert(String inputFilename, String outputFilename, PrintStream errorStream, String encoding) throws IOException {
        InputStream input;
        OutputStream output;
        try {
            input = new BufferedInputStream(new FileInputStream(inputFilename));
            output = new BufferedOutputStream(new FileOutputStream(outputFilename));
        } catch (FileNotFoundException x) {
            throw new FatalError("File not found: " + x.getMessage());
        } catch (IOException x) {
            throw new FatalError("IO exception: " + x.getMessage());
        }
        try {
            convert(input, output, errorStream, inputFilename, encoding);
        } catch (IOException x) {
            delete(outputFilename);
            throw x;
        } catch (RuntimeException x) {
            delete(outputFilename);
            throw x;
        }
    }

    private void delete(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
    }

    public void convert(InputStream input, OutputStream output, PrintStream errorStream, String inputFilename, String encoding) throws IOException {
        int tidyEncoding = tidyEncodingFor(encoding);
        if (tidyEncoding >= 0) {
            convert_(input, output, errorStream, inputFilename, tidyEncoding);
        } else {
            convertAsUtf8_(input, output, errorStream, inputFilename, encoding);
        }
    }

    static final String ampReplacement_ = "^dyn:amp^";

    static final int ampReplacementFirstChar_ = (int) ampReplacement_.charAt(0);

    private static class EntityPreserverInputStream extends InputStream {

        private InputStream input_;

        private String buf_ = null;

        private int pBuf_ = -1;

        public EntityPreserverInputStream(InputStream input) {
            super();
            input_ = input;
        }

        public int read() throws IOException {
            if (buf_ != null) {
                if (++pBuf_ >= buf_.length()) {
                    buf_ = null;
                    pBuf_ = -1;
                } else {
                    return buf_.charAt(pBuf_);
                }
            }
            int result = input_.read();
            if (result == '&') {
                buf_ = ampReplacement_;
                result = read();
            }
            return result;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int offset, int length) throws IOException {
            int c;
            int i = -1;
            while (++i < length) {
                c = read();
                if (c == -1) {
                    return i == 0 ? -1 : i;
                }
                b[offset + i] = (byte) c;
            }
            return i;
        }

        public void close() throws IOException {
            input_.close();
        }
    }

    private static class EntityPreserverOutputStream extends OutputStream {

        private OutputStream output_;

        private StringBuffer buf_ = new StringBuffer();

        public EntityPreserverOutputStream(OutputStream output) {
            super();
            output_ = output;
        }

        public void close() throws IOException {
            output_.close();
        }

        public void flush() throws IOException {
            output_.flush();
        }

        public void write(int b) throws IOException {
            if (buf_.length() != 0) {
                buf_.append((char) b);
                String s = buf_.toString();
                if (s.equals(ampReplacement_)) {
                    output_.write('&');
                    buf_.setLength(0);
                } else if (!ampReplacement_.startsWith(s)) {
                    output_.write(s.getBytes());
                    buf_.setLength(0);
                }
            } else if (ampReplacementFirstChar_ == b) {
                buf_.append((char) b);
            } else {
                output_.write(b);
            }
        }
    }

    private void convert_(InputStream input, OutputStream output, PrintStream errorStream, String inputFilename, int tidyEncoding) throws IOException {
        tidy_.setInputStreamName(inputFilename);
        tidy_.setCharEncoding(tidyEncoding);
        StringWriter errout;
        errout = new StringWriter();
        PrintWriter erroutWriter = new PrintWriter(errout, true);
        tidy_.setErrout(erroutWriter);
        try {
            tidy_.parse(new EntityPreserverInputStream(input), new EntityPreserverOutputStream(output));
        } finally {
            input.close();
            output.close();
        }
        erroutWriter.flush();
        StringBuffer messages = errout.getBuffer();
        if (messages.length() > 0) {
            errorStream.print("Tidy messages for ");
            errorStream.println(inputFilename);
            String messageText = messages.toString();
            errorStream.print(messageText);
            if (messageText.indexOf("missing </script>") > 0) {
                throw new FatalError(inputFilename + ": Script tag errors must be fixed before" + " Dynamator can process this file");
            }
        }
        if (tidy_.getParseErrors() > 0) {
            throw new FatalError();
        }
    }

    private void convertAsUtf8_(InputStream input, OutputStream output, PrintStream errorStream, String inputFilename, String encoding) throws IOException {
        InputStream utf8Input = convertEncoding(input, encoding, "UTF8");
        ByteArrayOutputStream utf8Output = new ByteArrayOutputStream(4096);
        convert_(utf8Input, utf8Output, errorStream, inputFilename, Configuration.UTF8);
        InputStreamReader reader = null;
        OutputStreamWriter writer = null;
        try {
            reader = new InputStreamReader(new ByteArrayInputStream(utf8Output.toByteArray()), "UTF8");
            writer = new OutputStreamWriter(output, encoding);
            copyIO(reader, writer);
            writer.flush();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static void copyIO(Reader reader, Writer writer) throws IOException {
        char[] buffer = new char[4096];
        int len;
        while ((len = reader.read(buffer)) >= 0) {
            writer.write(buffer, 0, len);
        }
    }

    private static InputStream convertEncoding(InputStream input, String fromEncoding, String toEncoding) throws IOException {
        InputStreamReader reader = null;
        OutputStreamWriter writer = null;
        ByteArrayOutputStream toStream = new ByteArrayOutputStream(4096);
        try {
            reader = new InputStreamReader(input, fromEncoding);
            writer = new OutputStreamWriter(toStream, toEncoding);
            copyIO(reader, writer);
            writer.flush();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                }
            }
        }
        return new ByteArrayInputStream(toStream.toByteArray());
    }

    private static int tidyEncodingFor(String encoding) {
        int result;
        if ("ascii".equalsIgnoreCase(encoding) || "us-ascii".equalsIgnoreCase(encoding)) {
            result = Configuration.ASCII;
        } else if ("utf-8".equalsIgnoreCase(encoding) || "UTF8".equalsIgnoreCase(encoding)) {
            result = Configuration.UTF8;
        } else if ("iso-8859-1".equalsIgnoreCase(encoding) || "ISO8859_1".equalsIgnoreCase(encoding)) {
            result = Configuration.LATIN1;
        } else if (encoding.startsWith("iso-2022") || encoding.startsWith("iso2022") || encoding.startsWith("ISO-2022") || encoding.startsWith("ISO2022")) {
            result = Configuration.ISO2022;
        } else {
            result = -1;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String encoding = "ascii";
        int iArgs = 0;
        HtmlToXml_Tidy theObject = new HtmlToXml_Tidy();
        if (args.length == 0) {
            System.out.println("Usage: java " + theObject.getClass().getName() + "{-e encoding} " + "input-file-name ");
            return;
        }
        if ("-e".equals(args[0])) {
            encoding = args[1];
            iArgs = 2;
        }
        InputStream input = new BufferedInputStream(new FileInputStream(args[iArgs]));
        theObject.convert(input, System.out, System.err, args[iArgs], encoding);
    }
}
