package eulergui.parser.n3.impl.parser4j.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import n3_project.IOManager;
import net.sf.parser4j.parser.entity.ParseResult;
import net.sf.parser4j.parser.entity.data.NonTerminal;
import net.sf.parser4j.parser.entity.data.TerminalCharRange;
import net.sf.parser4j.parser.entity.parsestate.ParseStack;
import net.sf.parser4j.parser.service.IParserListener;
import net.sf.parser4j.parser.service.ParserException;
import net.sf.parser4j.parser.service.ParserFileReader;
import net.sf.parser4j.parsetreeinspector.service.ParseTreeToXML;
import org.apache.log4j.Logger;
import eulergui.n3model.IN3Model;
import eulergui.n3model.service.util.N3ModelToStringUtil;
import eulergui.parser.n3.impl.parser4j.entity.FromNetRecord;
import eulergui.parser.n3.impl.parser4j.entity.N3ParseResult;

/**
 * 
 * @author luc peuvrier
 * 
 */
public class TestForJos extends TestCase implements IParserListener {

    private static final Logger _log = Logger.getLogger(TestForJos.class);

    private static final N3ModelToStringUtil n3ModelToStringUtil = N3ModelToStringUtil.getInstance();

    private static final ParseTreeToXML parseTreeToXML = ParseTreeToXML.getInstance();

    private static final NetParserFileReaderFactory parserFileReaderFactory = NetParserFileReaderFactory.getInstance();

    private static final N3Parser n3Parser = N3Parser.getInstance();

    /** url set by {@link #createParserFileReader(FromNetRecord)} */
    private transient URI url;

    @Override
    protected void setUp() throws Exception {
        n3Parser.initialize(this);
        super.setUp();
    }

    public void testFromNet1() throws Exception {
        doTheTest("test/parser/n3/impl/parser4j/n3-from-net1.txt");
    }

    public void testFromNet2() throws Exception {
        doTheTest("test/parser/n3/impl/parser4j/n3-from-net2.txt");
    }

    public void testFromEulerSharp() throws Exception {
        doTheTest("test/parser/n3/impl/parser4j/eulersharp.url.txt");
    }

    public void xtestFromNet1Long() throws Exception {
        doTheTest("test/parser/n3/impl/parser4j/n3-from-net1-long.txt");
    }

    private void doTheTest(final String fileName) throws Exception {
        String runTests = System.getProperty("PARSER4J_TESTS");
        if ("no".equals(runTests)) return;
        final List<FromNetRecord> list = obtainsFromNetRecordList(fileName);
        for (FromNetRecord record : list) {
            doTheTestForOneFile(record);
        }
    }

    private void doTheTestForOneFile(final FromNetRecord record) throws IOException, ParserException, URISyntaxException {
        if (_log.isInfoEnabled()) {
            _log.info(record.getStrUrl());
        }
        final ParserFileReader parserFileReader = createParserFileReader(record);
        if (parserFileReader != null) {
            final N3ParseResult n3ParseResult = n3Parser.parseN3(record.getStrUrl(), IOManager.getInputStream(new URI(record.getStrUrl())));
            final ParseResult parseResult = n3ParseResult.getParseResult();
            if (record.isInError()) {
                if (!parseResult.isInError()) {
                    writeModelInFile(n3ParseResult);
                    fail("parsing must fail for\n" + record.getStrUrl() + "\n" + parseResult);
                }
                final int prLineNumber = parseResult.getLineNumber();
                final int prColNumber = parseResult.getColumnNumber();
                if (prLineNumber != record.getLineNumber() || prColNumber != record.getColumnNumber()) {
                    fail("not expected error position in text " + prLineNumber + "," + prColNumber + "\n" + record.getStrUrl() + "\n" + parseResult);
                }
                final int characterExpected = record.getCharacterExpected();
                if (characterExpected != 0) {
                    final Set<TerminalCharRange> set = parseResult.getExpectedTerminalSet();
                    final Iterator<TerminalCharRange> iterator = set.iterator();
                    boolean found = false;
                    while (!found && iterator.hasNext()) {
                        final TerminalCharRange range = iterator.next();
                        found = range.contains(characterExpected);
                    }
                    if (!found) {
                        fail("character \"" + ((char) record.getCharacterExpected()) + "\" must be expected\n" + record.getStrUrl() + "\n" + parseResult.toString());
                    }
                }
            } else {
                if (parseResult.isInError()) {
                    final ParseStack[] parseStacks = parseResult.getParseStacks();
                    final Map<Integer, NonTerminal> nonTerminalByIdentifierMap = parseResult.getNonTerminalByIdentifierMap();
                    parseTreeToXML.toXml("runtime/tests/parsetree", parseStacks, nonTerminalByIdentifierMap);
                    fail("parsing must succeed\n" + record.getStrUrl() + "\n" + parseResult);
                }
                writeModelInFile(n3ParseResult);
            }
        } else {
            _log.error("failed get file reader for \"" + record.getStrUrl() + "\"");
        }
    }

    private void writeModelInFile(final N3ParseResult n3ParseResult) throws IOException {
        final IN3Model n3Model = n3ParseResult.getModel();
        final String string = n3ModelToStringUtil.toString(n3Model);
        final String outFileName = urlToFile("runtime/tests", url, ".n3model.txt");
        final File outFile = new File(outFileName);
        outFile.getParentFile().mkdirs();
        writeFile(outFileName, string);
    }

    private List<FromNetRecord> obtainsFromNetRecordList(final String fileName) throws IOException {
        final List<FromNetRecord> list = new LinkedList<FromNetRecord>();
        final FileInputStream fileInputStream = new FileInputStream(fileName);
        final Reader reader = new InputStreamReader(fileInputStream, "UTF-8");
        final BufferedReader bufferedReader = new BufferedReader(reader);
        final Pattern pattern = Pattern.compile("[\\x00- ]*");
        String line;
        int lineCount = 0;
        while ((line = bufferedReader.readLine()) != null && !line.startsWith("#end")) {
            lineCount++;
            if (line.startsWith("#clear")) {
                list.clear();
            } else if (!line.startsWith("#") && !pattern.matcher(line).matches()) {
                final String[] strings = line.split(" ");
                if (strings.length < 2) {
                    fail("line " + lineCount + " : " + line);
                }
                final String strUrl = strings[0];
                final boolean inError = "fail".equals(strings[1]);
                final FromNetRecord record;
                if (inError && strings.length >= 5) {
                    final int lineNumber = Integer.parseInt(strings[2]);
                    final int columnNumber = Integer.parseInt(strings[3]);
                    final String characterExpected = strings[4];
                    record = new FromNetRecord(strUrl, inError, lineNumber, columnNumber, characterExpected);
                } else {
                    record = new FromNetRecord(strUrl, inError);
                }
                list.add(record);
            }
        }
        bufferedReader.close();
        return list;
    }

    private ParserFileReader createParserFileReader(final FromNetRecord record) throws IOException {
        final String strUrl = record.getStrUrl();
        ParserFileReader parserFileReader;
        try {
            parserFileReader = parserFileReaderFactory.create(strUrl);
        } catch (Exception exception) {
            _log.error("can not create reader for \"" + strUrl + "\"", exception);
            parserFileReader = null;
        }
        url = parserFileReaderFactory.getUrl();
        if (parserFileReader != null) {
            parserFileReader.mark();
            final String outFileName = urlToFile("runtime/tests", url, "");
            final File outFile = new File(outFileName);
            outFile.getParentFile().mkdirs();
            final Writer writer = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
            int readed;
            while ((readed = parserFileReader.read()) != -1) {
                writer.write(readed);
            }
            writer.close();
            parserFileReader.reset();
        }
        return parserFileReader;
    }

    private String urlToFile(final String directory, final URI url2, final String suffix) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(directory);
        if (!directory.endsWith("/")) {
            stringBuilder.append("/");
        }
        final String host = url2.getHost();
        if (host != null && host.length() != 0) {
            stringBuilder.append(host);
            stringBuilder.append("/");
        }
        final String path = url2.getPath();
        if (path.startsWith("/")) {
            stringBuilder.append(path.substring(1));
        } else {
            stringBuilder.append(path);
        }
        return stringBuilder.toString();
    }

    private void writeFile(final String outFileName, final String string) throws IOException {
        final Writer writer = new OutputStreamWriter(new FileOutputStream(outFileName), "UTF-8");
        writer.write(string);
        writer.close();
    }

    @Override
    public void newLine(final int line) {
    }
}
