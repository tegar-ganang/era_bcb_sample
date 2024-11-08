package de.fraunhofer.ipsi.ipsixq.ui.xqts;

import java.io.*;
import java.util.*;
import com.infonyte.ds.fds.FileDataServerFactory;
import com.infonyte.pdom.PDOM;
import com.infonyte.pdom.PDOMFactory;
import com.infonyte.pdom.PDOMParser;
import com.infonyte.pdom.PDOMParserFactory;
import de.fraunhofer.ipsi.ipsixq.api.XQIQueryHandler;
import de.fraunhofer.ipsi.ipsixq.api.XQIQueryHandlerFactory;
import de.fraunhofer.ipsi.xquery.datamodel.DocumentNode;
import de.fraunhofer.ipsi.xquery.errors.XQueryException;
import de.fraunhofer.ipsi.xquery.io.SerializationException;
import de.fraunhofer.ipsi.xquery.util.PositionInfoImpl;
import java.text.SimpleDateFormat;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

public class XQTSExecuter {

    private static final String version = "0.9.0";

    private static final String xquery = " XQTS Executer for IPSI-XQ";

    private static final String ns = "http://www.w3.org/2005/02/query-test-XQTSCatalog";

    private static final String XML = "XML";

    private static final String FRAGMENT = "Fragment";

    private final List<String> includes;

    private final Set<String> excludes;

    private File xqtsRoot, sourcePath, resultPath, queryPath;

    private String xqueryFileExtension;

    private Map<String, String> sources;

    private Map<String, String> sourcesToSchema;

    private Map<String, String> schemaToLocation;

    private LSParser lsParser;

    private LSInput lsInput;

    private PDOM catalog;

    private PrintWriter out;

    /**
	 * Constructor. Analysis the commandline arguments.
	 *
	 * @param args The arguments to specify the details of the query. Look at printHelp for syntax.
	 * @see printHelp
	 */
    public XQTSExecuter(String args[]) throws Exception {
        System.out.println("Options: " + java.util.Arrays.asList(args));
        if (args.length == 0) {
            printHelp();
        }
        boolean overwrite = false;
        includes = new ArrayList<String>();
        excludes = new HashSet<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-output:")) {
                String s = args[i];
                s = s.substring(s.indexOf(":") + 1);
                File outFile = new File(s);
                if (outFile.exists() && !overwrite) throw new IllegalArgumentException("The output file already exists!");
                outFile.getParentFile().mkdirs();
                out = new PrintWriter(new FileWriter(outFile));
            } else if (args[i].equalsIgnoreCase("-overwrite")) {
                overwrite = true;
            } else if (args[i].startsWith("-include:")) {
                String s = args[i];
                s = s.substring(s.indexOf(":") + 1);
                StringTokenizer tok = new StringTokenizer(s, ",");
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken().toLowerCase();
                    System.err.println("including: " + token);
                    includes.add(token);
                }
            } else if (args[i].startsWith("-exclude:")) {
                String s = args[i];
                s = s.substring(s.indexOf(":") + 1);
                StringTokenizer tok = new StringTokenizer(s, ",");
                while (tok.hasMoreTokens()) {
                    excludes.add(tok.nextToken().toLowerCase());
                }
            } else if (args[i].equals("-?") || args[i].equals("-h") || args[i].equals("-help")) {
                printHelp();
            } else if (i == args.length - 1) {
                xqtsRoot = new File(args[i]);
                File catalogFile = new File(xqtsRoot, "XQTSCatalog.xml");
                if (!catalogFile.exists() || !catalogFile.canRead()) throw new IllegalArgumentException("Cannot read catalog file: " + args[i]);
                catalog = new PDOMFactory(new FileDataServerFactory()).create();
                SAXParserFactory fac = SAXParserFactory.newInstance();
                fac.setNamespaceAware(true);
                PDOMParserFactory parserFac = new PDOMParserFactory();
                parserFac.setSAXParserFactory(fac);
                PDOMParser parser = parserFac.newParser();
                System.err.println("Parsing catalog...");
                parser.parse(catalogFile, catalog);
                System.err.println("Done.");
            } else {
                System.err.println("Unrecognized option: " + args[i]);
                printHelp();
            }
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Missing xqts root argument!");
        } else if (includes.isEmpty()) {
            throw new IllegalArgumentException("No test-groups specified! Use the -include option to specify test-groups.");
        } else if (out == null) {
            File outFile = new File("XQTSResults.xml");
            if (outFile.exists() && !overwrite) throw new IllegalArgumentException("The default output file already exists!");
            out = new PrintWriter(new FileWriter(outFile));
            initOutFile();
        }
        System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMImplementationSourceImpl");
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        lsParser = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        lsInput = impl.createLSInput();
    }

    /**
	 * Method initOutFile
	 *
	 */
    private void initOutFile() {
        out.println("<test-suite-result xmlns='http://www.w3.org/2005/02/query-test-XQTSResult'>");
        out.println("   <implementation name='IPSI-XQ-open' version='0.9.1'>");
        out.println("      <organization name='Fraunhofer IPSI'/>");
        out.println("      <submittor name='Patrick Lehti'/>");
        out.println("   </implementation>");
        out.println("   <syntax>XQuery</syntax>");
        out.print("   <test-run dateRun='");
        out.print(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        out.println("'>");
        out.print("      <test-suite version='");
        out.print(version);
        out.println("'/>");
        out.println("   </test-run>");
    }

    /**
	 * Method closeOutFile
	 *
	 */
    private void closeOutFile() {
        out.println("</test-suite-result>");
        out.close();
    }

    /**
	 * Executes the catalog file.
	 */
    public void run() throws IOException {
        try {
            Element root = catalog.getDocument().getDocumentElement();
            queryPath = new File(xqtsRoot, root.getAttribute("XQueryQueryOffsetPath"));
            resultPath = new File(xqtsRoot, root.getAttribute("ResultOffsetPath"));
            sourcePath = new File(xqtsRoot, root.getAttribute("SourceOffsetPath"));
            xqueryFileExtension = root.getAttribute("XQueryFileExtension");
            sources = new HashMap<String, String>();
            sourcesToSchema = new HashMap<String, String>();
            schemaToLocation = new HashMap<String, String>();
            NodeList sourceList = root.getElementsByTagNameNS(ns, "source");
            for (int i = 0; i < sourceList.getLength(); i++) {
                Element source = (Element) sourceList.item(i);
                String id = source.getAttribute("ID");
                String path = source.getAttribute("FileName");
                File f = new File(sourcePath, path);
                sources.put(id, f.getAbsolutePath());
                String schema = source.getAttribute("schema");
                if (!schema.equals("")) sourcesToSchema.put(id, schema);
            }
            NodeList schemaList = root.getElementsByTagNameNS(ns, "schema");
            for (int i = 0; i < schemaList.getLength(); i++) {
                Element schema = (Element) schemaList.item(i);
                String id = schema.getAttribute("ID");
                String uri = schema.getAttribute("uri");
                String path = schema.getAttribute("FileName");
                File f = new File(sourcePath, path);
                sources.put(id, f.getAbsolutePath());
                System.setProperty(uri, f.getAbsolutePath());
                schemaToLocation.put(id, uri + " " + f.getAbsolutePath());
            }
            NodeList rootTestGroups = root.getChildNodes();
            for (int i = 0; i < rootTestGroups.getLength(); i++) {
                Node node = rootTestGroups.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equalsIgnoreCase("test-group")) {
                    Element elem = (Element) node;
                    String name = elem.getAttribute("name");
                    if (includes.contains(name.toLowerCase())) {
                        processGroup(elem);
                    } else {
                        System.err.println("skipping: " + name);
                    }
                }
            }
        } catch (Exception e) {
            System.err.print(e.getMessage());
            e.printStackTrace();
        } finally {
            closeOutFile();
        }
    }

    /**
	 * Method processGroup
	 *
	 * @param    elem                an Element
	 *
	 */
    private void processGroup(Element elem) {
        System.err.println("---------------Processing group: " + elem.getAttribute("name") + "---------------");
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String name = e.getAttribute("name");
                if (e.getLocalName().equalsIgnoreCase("test-group") && !excludes.contains(name.toLowerCase())) {
                    processGroup(e);
                } else if (e.getLocalName().equalsIgnoreCase("test-case")) {
                    executeTest(e);
                }
            }
        }
    }

    /**
	 * Method executeTest
	 *
	 * @param    elem                an Element
	 *
	 */
    private void executeTest(Element elem) {
        String name = elem.getAttribute("name");
        System.err.println("Processing case: " + name);
        String filePath = elem.getAttribute("FilePath");
        String queryFile = null, compare = null;
        List<String> inputFiles = new ArrayList<String>();
        List<String> inputVars = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();
        List<String> outputFiles = new ArrayList<String>();
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                if (e.getLocalName().equalsIgnoreCase("query")) {
                    String query = e.getAttribute("name");
                    queryFile = new File(queryPath, filePath + query + xqueryFileExtension).getAbsolutePath();
                } else if (e.getLocalName().equalsIgnoreCase("input-file")) {
                    inputFiles.add(e.getFirstChild().getNodeValue());
                    inputVars.add(e.getAttribute("variable"));
                } else if (e.getLocalName().equalsIgnoreCase("output-file")) {
                    String output = e.getFirstChild().getNodeValue();
                    outputFiles.add(new File(resultPath, filePath + output).getAbsolutePath());
                    compare = e.getAttribute("compare");
                } else if (e.getLocalName().equalsIgnoreCase("expected-error")) {
                    errors.add(e.getFirstChild().getNodeValue());
                }
            }
        }
        boolean pass = false;
        try {
            String query = "";
            StringBuffer q = new StringBuffer();
            InputStreamReader in = new InputStreamReader(new FileInputStream(queryFile), "UTF-8");
            char last = ' ';
            for (int c = in.read(); c != -1; c = in.read()) {
                char ch = (char) c;
                if (last == '\r') if (ch != '\n') q.append('\n');
                if (ch != '\r') q.append(ch);
                last = ch;
            }
            query = q.toString();
            XQIQueryHandlerFactory fac = XQIQueryHandlerFactory.getInstance();
            XQIQueryHandler handler = fac.getQueryHandler(query);
            for (int i = 0; i < inputFiles.size(); i++) {
                String sourceId = inputFiles.get(i);
                String schemaLocation = null;
                if (sourcesToSchema.containsKey(sourceId)) {
                    schemaLocation = schemaToLocation.get(sourcesToSchema.get(sourceId));
                }
                DocumentNode doc = fac.getDocumentManager().getDocument(sources.get(sourceId), schemaLocation);
                handler.getDynamicContext().variable_values.declare(new QName(inputVars.get(i)), doc);
            }
            String result = "";
            try {
                result = handler.getQueryResult().toString();
            } catch (RuntimeException e) {
                SerializationException ex = (SerializationException) e.getCause();
                if (ex != null) throw new XQueryException(new PositionInfoImpl(0, 0, 0, 0), ex.getError(), ex.getDetailArguments());
            }
            if (outputFiles.isEmpty() && !errors.isEmpty()) System.err.println("+++Test failed!\nResult was: " + result + "\nExpected error: " + errors); else {
                List<String> expectedResults = new ArrayList<String>();
                for (String outputFile : outputFiles) {
                    q = new StringBuffer();
                    in = new InputStreamReader(new FileInputStream(outputFile), "UTF-8");
                    last = ' ';
                    for (int c = in.read(); c > -1; c = in.read()) {
                        char ch = (char) c;
                        if (last == '\r') if (ch != '\n') q.append('\n');
                        if (ch != '\r') q.append(ch);
                        last = ch;
                    }
                    expectedResults.add(q.toString());
                }
                if (compare.equals(FRAGMENT)) {
                    result = "<r>" + result + "</r>";
                    for (int i = 0; i < expectedResults.size(); i++) expectedResults.set(i, "<r>" + expectedResults.get(i) + "</r>");
                    compare = XML;
                }
                if (compare.equals(XML)) {
                    lsInput.setStringData(result);
                    Document resultDoc = lsParser.parse(lsInput);
                    for (int i = 0; i < expectedResults.size(); i++) {
                        lsInput.setStringData(expectedResults.get(i));
                        Document expectedDoc = lsParser.parse(lsInput);
                        if (resultDoc.isEqualNode(expectedDoc)) {
                            pass = true;
                            break;
                        }
                    }
                } else for (String expectedResult : expectedResults) {
                    if (result.equals(expectedResult)) {
                        pass = true;
                        break;
                    }
                }
                if (!pass) System.err.println("+++Test failed!\nResult was: " + result + "\nExpected was: " + expectedResults);
            }
        } catch (XQueryException e) {
            if (e.getErrorDescription() != null) {
                System.err.println("Error: " + e.getErrorDescription().getCode() + " expected: " + errors);
                if (!errors.isEmpty() && errors.contains(e.getErrorDescription().getCode())) {
                    pass = true;
                } else {
                    System.err.println("+++Test failed: " + e.getErrorDescription().getShortMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("+++Test failed: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Throwable e) {
            System.err.println("+++Test failed: " + e.getMessage());
            e.printStackTrace();
        }
        out.print("   <test-case name='");
        out.print(name);
        out.print("' result='");
        if (pass) out.print("pass"); else out.print("fail");
        out.println("'/>");
    }

    /**
	 * Method main
	 *
	 * @param    args                a  String[]
	 *
	 */
    public static void main(String[] args) {
        System.out.println("\n" + xquery + " Version " + version);
        System.out.println("----------------------------------------");
        System.out.println();
        try {
            XQTSExecuter executer = new XQTSExecuter(args);
            executer.run();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            printHelp();
        }
    }

    /**
	 * Shows help on XQuery Interpreter and explains the syntax.
	 */
    private static void printHelp() {
        System.out.println(" Usage is:");
        System.out.println("   java de.fraunhofer.ipsi.xquery.xqts.XQTSExecuter [options] XQTSROOT\n");
        System.out.println("     options :\n");
        System.out.println("        -output:outputfile     -  direct output to outputfile (default: XQTSResult.xml)");
        System.out.println("        -overwrite             -  overwrites an outputfile, if it already exists");
        System.out.println("        -include:testgroups    -  a comma separated list of top-level test-groups that should be executed");
        System.out.println("        -exclude:testgroups    -  a comma separated list of test-groups that should not be executed");
        System.out.println();
        System.out.println("     XQTSROOT   - the root folder of the XQTS installation (containing the XQTSCatalog.xml)");
        System.out.println();
        System.exit(0);
    }
}
