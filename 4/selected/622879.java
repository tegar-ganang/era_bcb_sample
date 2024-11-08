package info.monitorenter.unicode.decoder;

import info.monitorenter.unicode.decoder.html.HtmlEntityDecoderLexer;
import info.monitorenter.unicode.decoder.html.HtmlEntityDecoderParser;
import info.monitorenter.unicode.decoder.html.HtmlEntityDecoderReader;
import info.monitorenter.util.FileUtil;
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;

/**
 * Easy to use utility functions with scope on decoding to unicode.
 * <p>
 * 
 * Be careful with the methods that work on String data (vs. Streams): Large
 * documents will cause an <code>{@link OutOfMemoryError}</code>.
 * <p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
 * 
 * 
 * @version $Revision: 1.8 $
 */
public final class DecodeUtil {

    /**
	 * Utility class constructor.
	 * <p>
	 * 
	 */
    private DecodeUtil() {
    }

    /**
	 * Decodes <tt>HTML Entities</tt>(e.g. &amp;nbsp;) in the given String into
	 * the unicode representation.
	 * <p>
	 * 
	 * This method should perform quick as an <a href="http://www.antlr.org"
	 * target="_blank">ANTLR</a> generated parser is used.
	 * <p>
	 * 
	 * HTML entities are described in <a
	 * href="http://www.w3.org/TR/html401/sgml/entities.html">
	 * http://www.w3.org/TR/html401/sgml/entities.html</a>
	 * <p>
	 * 
	 * For enterprise support of arbitrary large files prefer the approach of
	 * 
	 * <code>{@link info.monitorenter.unicode.decoder.html.HtmlEntityDecoderReader}</code>.
	 * <p>
	 * 
	 * @param html
	 *            the html data to decode <tt>HTML Entities</tt> in.
	 * 
	 * @param recursive
	 *            if true the input will be processed until there are no
	 *            character entity references contained any more (decoding
	 *            &amp;ouml; will produce &ouml;).
	 * 
	 * @return a new String with the unicode representation of the HTML Entities
	 *         in the input html.
	 * 
	 * 
	 * @throws IOException
	 *             if something goes wrong.
	 * 
	 * @throws TokenStreamException
	 *             if invalid character data was found in the underlying stream.
	 *             This is unlikely to happen as the lexer covers all
	 *             characters, but if it should happen (ANTLR error?) this
	 *             method cannot deal with the problem and does not catch the
	 *             exception.
	 * 
	 * @throws RecognitionException
	 *             if invalid format was found in the given html. This is
	 *             unlikely to happen as the grammar accepts any tokens , but if
	 *             it should happen (ANTLR error?) this method cannot deal with
	 *             the problem and does not catch the exception.
	 * 
	 */
    public static String decodeHtmlEntities(final String html, final boolean recursive) throws RecognitionException, TokenStreamException, IOException {
        String result = html;
        boolean again = false;
        do {
            StringBuffer resultBuffer = new StringBuffer();
            HtmlEntityDecoderLexer lexer = new HtmlEntityDecoderLexer(new StringReader(result));
            HtmlEntityDecoderParser decoder = new HtmlEntityDecoderParser(lexer);
            Token token = decoder.decodeNext();
            while (token.getType() != Token.EOF_TYPE) {
                resultBuffer.append(token.getText());
                token = decoder.decodeNext();
            }
            result = resultBuffer.toString();
            again = decoder.isPotentialRemainingEntity();
        } while (again && recursive);
        return result;
    }

    /**
	 * Main hook for command line use.
	 * <p>
	 * 
	 * @param args
	 *            ignored.
	 * 
	 * @throws RecognitionException
	 *             if something in the parser goes wrong.
	 * 
	 * @throws TokenStreamException
	 *             if something in the lexer goes wrong.
	 * 
	 * @throws IOException
	 *             if something in I/O goes wrong.
	 * 
	 * @throws UnknownOptionException
	 *             if arguments are wrong.
	 * 
	 * @throws IllegalOptionValueException
	 *             if arguments are wronger.
	 */
    public static void main(final String[] args) throws RecognitionException, TokenStreamException, IOException, IllegalOptionValueException, UnknownOptionException {
        try {
            CmdLineParser cmdLineParser = new CmdLineParser();
            Option formatOption = cmdLineParser.addStringOption('f', "format");
            Option outputEncodingOption = cmdLineParser.addStringOption('c', "outcharset");
            Option inputEncodingOption = cmdLineParser.addStringOption('i', "incharset");
            cmdLineParser.parse(args);
            String format = (String) cmdLineParser.getOptionValue(formatOption);
            String outputEncoding = (String) cmdLineParser.getOptionValue(outputEncodingOption);
            if (outputEncoding == null || outputEncoding.trim().equals("")) {
                outputEncoding = "utf-8";
                System.out.println("Defaulting to output charset utf-8 as argument -c is missing or not valid.");
            }
            String inputEncoding = (String) cmdLineParser.getOptionValue(inputEncodingOption);
            if (inputEncoding == null || outputEncoding.trim().equals("")) {
                inputEncoding = "utf-8";
                System.out.println("Defaulting to input charset utf-8 as argument -i is missing or not valid.");
            }
            String[] remainingArgs = cmdLineParser.getRemainingArgs();
            if (remainingArgs.length != 2) {
                printUsage("Input and output file are not specified correctly. ");
            }
            File inputFile = new File(remainingArgs[0]);
            if (!inputFile.exists()) {
                printUsage("Input file " + remainingArgs[0] + " does not exist. ");
            }
            if (format == null || format.trim().equals("")) {
                format = (String) FileUtil.cutExtension(inputFile.getName()).getValue();
            }
            File outputFile = new File(remainingArgs[1]);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            System.out.println("format detected: " + format);
            if ("html".equals(format)) {
                Reader reader = new HtmlEntityDecoderReader(new InputStreamReader(new FileInputStream(inputFile), inputEncoding));
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outputFile), outputEncoding);
                char[] buffer = new char[1024];
                int read;
                do {
                    read = reader.read(buffer);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    }
                } while (read != -1);
                out.flush();
                out.close();
            } else {
                printUsage("Format not specified via argument -f. Also guessing for the extension of input file " + inputFile.getName() + " failed");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            printUsage(ex.getMessage());
        }
    }

    /**
	 * Prints out usage for command line use and does the bad
	 * <code>{@link System#exit(int)}</code> with -1.
	 * <p>
	 * 
	 * @param errmsg
	 *            the cause for the termination.
	 * 
	 * @throws IllegalArgumentException
	 *             always to terminate.
	 */
    private static void printUsage(final String errmsg) throws IllegalArgumentException {
        StringBuffer msg = new StringBuffer("java -jar juniencoder-<versionnumber>.jar <options> infile outfile\n");
        msg.append("  options: \n");
        msg.append("    -f [--format] : target format to encode, one of [html]\n");
        msg.append("    -c [--outcharset] : a charset name for the output encoding as described here: http://java.sun.com/javase/6/docs/api/java/nio/charset/Charset.html\n");
        msg.append("    -i [--incharset] : a charset name for the input encoding as described here: http://java.sun.com/javase/6/docs/api/java/nio/charset/Charset.html\n");
        msg.append(errmsg);
        System.err.println(msg.toString());
        System.exit(-1);
    }
}
