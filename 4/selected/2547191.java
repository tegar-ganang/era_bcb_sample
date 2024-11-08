package info.monitorenter.unicode.encoder;

import info.monitorenter.unicode.encoder.html.HtmlEntityEncoderLexer;
import info.monitorenter.unicode.encoder.html.HtmlEntityEncoderParser;
import info.monitorenter.unicode.encoder.latex.LatexEncoderLexer;
import info.monitorenter.unicode.encoder.latex.LatexEncoderParser;
import info.monitorenter.unicode.encoder.latex.LatexEncoderReader;
import info.monitorenter.util.FileUtil;
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;

/**
 * Utility class to encode unicode to various target formats.
 * <p>
 * 
 * Be careful with the methods that work on String data (vs. Streams): Large
 * documents will cause an <code>{@link OutOfMemoryError}</code>.
 * <p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
 * 
 * 
 * @version $Revision: 1.7 $
 */
public final class EncodeUtil {

    /**
	 * Utility class ctor.
	 * <p>
	 */
    private EncodeUtil() {
    }

    /**
	 * Encodes the given unicode input <code>String</code> to latex - compliant
	 * input data.
	 * <p>
	 * 
	 * This covers special characters as german "Umlauts", French accented
	 * characters,... .
	 * <p>
	 * 
	 * 
	 * @param unicode
	 *            the unicode <code>String</code> to encode to latex format.
	 * 
	 * @return the latex - encoded version of the input <code>String</code>.
	 * 
	 * @throws TokenStreamException
	 *             if something goes wrong while lexing for the next token.
	 * 
	 * @throws RecognitionException
	 *             if something goes wrong while parsing the next token.
	 */
    public static String encodeLatex(final String unicode) throws RecognitionException, TokenStreamException {
        StringBuffer result = new StringBuffer();
        LatexEncoderLexer lexer = new LatexEncoderLexer(new StringReader(unicode));
        LatexEncoderParser encoder = new LatexEncoderParser(lexer);
        Token token = encoder.encodeNext();
        while (token.getType() != Token.EOF_TYPE) {
            result.append(token.getText());
            token = encoder.encodeNext();
        }
        return result.toString();
    }

    /**
	 * Encodes the given unicode input <code>String</code> to HTML in true ASCII
	 * format.
	 * <p>
	 * Named Entity References (http://www.w3.org/TR/html401/sgml/entities.html)
	 * will be preferred, the remaining non-ASCII characters will be encoded as
	 * Numerical Character References
	 * (http://www.w3.org/TR/html4/charset.html#h-5.3.1).
	 * <p>
	 * *
	 * 
	 * 
	 * @param unicode
	 *            the unicode <code>String</code> to encode to HTML format.
	 * 
	 * @return the non-ASCII HTML version of the input <code>String</code>.
	 * 
	 * @throws TokenStreamException
	 *             if something goes wrong while lexing for the next token.
	 * 
	 * @throws RecognitionException
	 *             if something goes wrong while parsing the next token.
	 */
    public static String encodeHtml(final String unicode) throws RecognitionException, TokenStreamException {
        StringBuffer result = new StringBuffer();
        HtmlEntityEncoderLexer lexer = new HtmlEntityEncoderLexer(new StringReader(unicode));
        HtmlEntityEncoderParser encoder = new HtmlEntityEncoderParser(lexer);
        Token token = encoder.encodeNext();
        while (token.getType() != Token.EOF_TYPE) {
            result.append(token.getText());
            token = encoder.encodeNext();
        }
        return result.toString();
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
            Option encodingOption = cmdLineParser.addStringOption('c', "charset");
            cmdLineParser.parse(args);
            String format = (String) cmdLineParser.getOptionValue(formatOption);
            String encoding = (String) cmdLineParser.getOptionValue(encodingOption);
            if (encoding == null || encoding.trim().equals("")) {
                encoding = "utf-8";
                System.out.println("Defaulting to output charset utf-8 as argument -c is missing or not valid.");
            }
            String[] remainingArgs = cmdLineParser.getRemainingArgs();
            if (remainingArgs.length != 2) {
                printUsage("Input and output file are not specified correctly. ");
            }
            File inputFile = new File(remainingArgs[0]);
            if (!inputFile.exists()) {
                printUsage("Input file " + remainingArgs[0] + " does not exist. ");
            }
            File outputFile = new File(remainingArgs[1]);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            if (format == null || format.trim().equals("")) {
                format = (String) FileUtil.cutExtension(outputFile.getName()).getValue();
            }
            if ("tex".equals(format)) {
                Reader reader = new LatexEncoderReader(new FileReader(inputFile));
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outputFile), encoding);
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
                printUsage("Format not specified via argument -f. Also guessing for the extension of output file " + outputFile.getName() + " failed");
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
        StringBuffer msg = new StringBuffer("java -jar junidecoder-<versionnumber>.jar <options> infile outfile\n");
        msg.append("  options: \n");
        msg.append("    -f [--format] : input format to decode, one of [tex](optional, if empty file extension is used for guessing)\n");
        msg.append("    -c [--charset] : a charset name for the output encoding as described here: http://java.sun.com/javase/6/docs/api/java/nio/charset/Charset.html\n");
        msg.append(errmsg);
        System.err.println(msg.toString());
        System.exit(-1);
    }
}
