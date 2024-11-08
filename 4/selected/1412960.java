package hu.sztaki.sztakipediaparser.wiki.parser;

import hu.sztaki.sztakipediaparser.wiki.converter.DefaultWikiInterpreter;
import hu.sztaki.sztakipediaparser.wiki.converter.IWikiInterpreter;
import hu.sztaki.sztakipediaparser.wiki.converter.InterpreterUtils;
import hu.sztaki.sztakipediaparser.wiki.converter.PlainWikiInterpreter;
import hu.sztaki.sztakipediaparser.wiki.parser.cc.JavaCCWikiParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Main class of the parser.
 * 
 * <p>
 * This class has a main method which is the entry point of the command line
 * version. It also has the parse method which should be called to generate the
 * html output from the source wikitext.
 * </p>
 * 
 * @author <a href="mailto:sztakipedia@sztaki.hu">Tibor Olah</a>, <a
 *         href="http://sztaki.hu">MTA SZTAKI</a>
 * @since 2011
 */
public class Parser {

    private String[] sections;

    private Splitter splitter = new Splitter("\n==[=]?[=]?([^=]*)==[=]?[=]?");

    private IWikiInterpreter interpreter;

    private boolean rewriteExistingFile = false;

    public void setRewriteExistingFile(boolean rewriteExistingFile) {
        this.rewriteExistingFile = rewriteExistingFile;
    }

    public Parser(IWikiInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
	 * Break wikitext to and array of strings. Each element in the array will be
	 * a section of the original wikitext.
	 * 
	 * @param wikitext
	 */
    private void breakToSections(String wikitext) {
        sections = splitter.split(wikitext);
    }

    /**
	 * Process single file, which includes reading it, parsing and writing out
	 * parsed content.
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 * @throws IOException
	 */
    public void parseFile(String inputFileName, String outputFileName) throws IOException {
        File inputFile = new File(inputFileName);
        if (inputFile.isFile()) {
            File outputFile = new File(outputFileName);
            if ((!outputFile.exists()) || (outputFile.exists() && rewriteExistingFile == true)) {
                System.out.print(inputFileName);
                String wikitext = readFileAsString(inputFile);
                String output = parse(wikitext);
                writeOutput(output, outputFileName);
                System.out.println(" ... OK");
            }
        }
    }

    /**
	 * Process input as a directory, processing each contained file separately.
	 * 
	 * @param inputDirName
	 * @param outputDirName
	 * @throws IOException
	 */
    public void parseDirectory(String inputDirName, String outputDirName) throws IOException {
        File inputDirectory = new File(inputDirName);
        String[] fileList = inputDirectory.list();
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                String fileName = fileList[i];
                parseFile(inputDirName + File.separator + fileName, outputDirName + File.separator + fileName);
            }
        }
    }

    /**
	 * Writes the given content to the specified file.
	 * 
	 * @param content
	 * @param outputFileName
	 * @throws IOException
	 */
    public void writeOutput(String content, String outputFileName) throws IOException {
        FileWriter writer = new FileWriter(outputFileName);
        writer.write(content);
        writer.flush();
        writer.close();
    }

    /**
	 * Parse the given wikitext and return the html output in a string. Parsing
	 * is done separately for each section.
	 * 
	 * @param wikitext
	 * @return Html output of the parser.
	 */
    public String parse(String wikitext) {
        StringBuilder b = new StringBuilder();
        interpreter.reInitialize();
        wikitext = interpreter.preprocess(wikitext);
        breakToSections(wikitext);
        for (int i = 0; i < sections.length; i++) {
            if (!sections[i].isEmpty()) {
                interpreter.reset();
                if (sections[i].matches("(?s)==[=]?[=]?([^=]*)==[=]?[=]?.*")) {
                    sections[i] = "\n" + sections[i];
                }
                if (sections[i].matches("(?s).*==[=]?[=]?([^=]*)==[=]?[=]?")) {
                    sections[i] += "\n";
                }
                JavaCCWikiParser p = new JavaCCWikiParser(new StringReader(sections[i]));
                p.parse(interpreter);
                interpreter.postprocess(wikitext);
                interpreter.render(b, true);
            }
        }
        return InterpreterUtils.trim(b.toString());
    }

    public static void printUsage() {
        System.out.println("Usage: java -jar SZPParser.jar inFile|inDir outfile|outDir locale [html|plain] [true|false] " + "\n if third parameter omitted, default is html" + "\n 5th parameter specifies that existing files could be overwritten, or skipped" + "\n locales currently supported: en, hu");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            System.exit(0);
        }
        String inputFileName = args[0];
        File infile = new File(inputFileName);
        String outputFileName = args[1];
        File outfile = new File(outputFileName);
        boolean isIODirectory = infile.isDirectory();
        if ((infile.isDirectory() && outfile.isFile()) || (infile.isFile() && outfile.isDirectory())) {
            printUsage();
            System.out.println("Input and output must be the same type: both files, or directories!");
        }
        String locale = args[2];
        String renderer = "default";
        if (args.length > 3) {
            renderer = args[3];
        }
        String rewrite = "";
        if (args.length > 4) {
            rewrite = args[4];
        }
        IWikiInterpreter wikiInterpreter;
        if ("html".startsWith(renderer)) {
            wikiInterpreter = new DefaultWikiInterpreter(new Locale(locale));
        } else if ("plain".startsWith(renderer)) {
            wikiInterpreter = new PlainWikiInterpreter(new Locale(locale));
        } else {
            wikiInterpreter = new DefaultWikiInterpreter(new Locale(locale));
        }
        Parser p = new Parser(wikiInterpreter);
        if ("true".startsWith(rewrite)) {
            p.setRewriteExistingFile(true);
        }
        if (isIODirectory) {
            p.parseDirectory(inputFileName, outputFileName);
        } else {
            p.parseFile(inputFileName, outputFileName);
        }
    }

    /**
	 * 
	 * @param file
	 * @return
	 * @author Illes Solt
	 * @throws IOException
	 */
    public String readFileAsString(File file) throws IOException {
        return readFileAsString(file, Charset.defaultCharset());
    }

    /**
	 * 
	 * @param file
	 * @param charset
	 * @return
	 * @author Illes Solt
	 * @throws IOException
	 */
    public String readFileAsString(File file, Charset charset) throws IOException {
        final byte[] contents = readFileAsBytes(file);
        return new String(contents, charset);
    }

    /**
	 * Efficiently read a file into a byte array.
	 * 
	 * @param file
	 * @return
	 * @author Illes Solt
	 * @throws IOException
	 */
    protected byte[] readFileAsBytes(File file) throws IOException {
        final byte[] contents;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            if (bb.hasArray()) {
                contents = bb.array();
            } else {
                contents = new byte[sz];
                bb.get(contents);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read file: " + file.getCanonicalPath(), e);
        } finally {
            if (fis != null) fis.close();
        }
        return contents;
    }

    public void setInterpreter(IWikiInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public IWikiInterpreter getInterpreter() {
        return interpreter;
    }
}
