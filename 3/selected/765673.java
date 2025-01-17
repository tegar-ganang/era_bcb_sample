package ee.webmedia.xtee.typegen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ee.webmedia.xtee.model.XmlBeansXTeeMetadata;
import ee.webmedia.xtee.typegen.database.DatabaseClasses;
import ee.webmedia.xtee.typegen.database.DatabaseGenerator;
import ee.webmedia.xtee.typegen.xmlbeans.BasepackageBinder;
import ee.webmedia.xtee.typegen.xmlbeans.SimpleFiler;
import ee.webmedia.xtee.typegen.xmlbeans.XteeSchemaCodePrinter;
import ee.webmedia.xtee.util.SOAPUtil;
import ee.webmedia.xtee.util.XTeeUtil;
import freemarker.template.TemplateException;

/**
 * XMLBeans types generator
 * 
 * @author Dmitri Danilkin
 */
public class TypeGen {

    private static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";

    private static final String SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

    private static final String NS_PREFIX = "xmlns";

    private static final String WSDL_DIR = "wsdldir";

    private static final String WSDL_SUFFIX = ".wsdl";

    private static final String XSD_SUFFIX = ".xsd";

    private static final String OUTPUT_DIR = "sourcedir";

    static final String XSB_DIR = "xsbdir";

    private static final String BASE_PACKAGE = "basepackage";

    private static final String DB_CLASSES_PACKAGE = "dbclassespackage";

    static Map<String, String> argMap = new HashMap<String, String>();

    private static Map<String, XmlBeansXTeeMetadata> metadata = new HashMap<String, XmlBeansXTeeMetadata>();

    private static List<XmlObject> schemas = new ArrayList<XmlObject>();

    private static File curWsdl;

    private static File hashFile;

    private static byte[] computedHash;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting source generation...");
        Timer timer = new Timer();
        Timer timer2 = new Timer();
        timer2.start();
        parseArgs(args);
        String outputdir = argMap.get(OUTPUT_DIR);
        File dirfile = new File(argMap.get(WSDL_DIR));
        if (dirfile.exists()) {
            File[] wsdls = getWsdls(dirfile);
            if (wsdls.length > 0) {
                System.out.println("Parsing " + wsdls.length + " WSDL file(s)...");
                timer.start();
                loadWsdlSchemasAndGenerateMetadata(wsdls);
                System.out.println("WSDL files parsed, time taken: " + timer.finishStr());
                System.out.println("Generating sources to " + outputdir + ", base package is: " + argMap.get(BASE_PACKAGE));
                timer.start();
                generateSource(outputdir, argMap.get(XSB_DIR), argMap.get(BASE_PACKAGE));
                System.out.println("Sources generated, time taken: " + timer.finishStr());
                System.out.println("Post-processing sources for attachment support...");
                timer.start();
                AttachmentPostprocessor.process(argMap.get(BASE_PACKAGE), new File(outputdir));
                System.out.println("Post-processing completed, time taken: " + timer.finishStr());
                System.out.println("Serializing metadata...");
                timer.start();
                saveMetadata(argMap.get(XSB_DIR));
                System.out.println("Metadata serialized, time taken: " + timer.finishStr());
                if (argMap.get(DB_CLASSES_PACKAGE) != null) {
                    System.out.println("Generating database classes...");
                    timer.start();
                    generateDatabaseClasses(outputdir);
                    System.out.println("Database classes generated, time taken: " + timer.finishStr());
                }
                writeHash();
            }
        }
        System.out.println("All done, total time taken: " + timer2.finishStr());
    }

    private static void writeHash() throws Exception {
        hashFile.delete();
        hashFile.getParentFile().mkdirs();
        hashFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(hashFile);
        fos.write(computedHash);
        fos.close();
    }

    /**
   * Serializes metadata to specified directory.
   * 
   * @param dir
   */
    private static void saveMetadata(String dir) throws Exception {
        File metafile = new File(dir, "xtee.metadata");
        metafile.createNewFile();
        FileOutputStream fos = new FileOutputStream(metafile);
        ObjectOutputStream stream = new ObjectOutputStream(fos);
        stream.writeObject(metadata);
        stream.close();
        fos.close();
    }

    /**
   * Generates the XMLBeans source files.
   * 
   * @param outputdir
   * @param basepackage
   * @throws XmlException
   * @throws URISyntaxException
   */
    private static void generateSource(String outputdir, String xsbdir, String basepackage) throws XmlException, URISyntaxException {
        XmlObject[] schemasarr = new XmlObject[schemas.size()];
        schemas.toArray(schemasarr);
        XmlOptions options = new XmlOptions();
        options.setCompileDownloadUrls();
        options.setGenerateJavaVersion("1.5");
        options.setSchemaCodePrinter(new XteeSchemaCodePrinter(options));
        XmlBeans.compileXmlBeans(null, null, schemasarr, new BasepackageBinder(basepackage), null, new SimpleFiler(outputdir, xsbdir), options);
    }

    /**
   * Parses command line arguments to a map
   * 
   * @param args
   */
    private static void parseArgs(String[] args) {
        for (String arg : args) {
            String[] pair = arg.split("=", 2);
            argMap.put(pair[0], pair[1]);
        }
    }

    /**
   * Gets all WSDL files in a directory and returns them as an array
   * 
   * @param dir
   * @return File array
   */
    private static File[] getWsdls(File dirfile) throws Exception {
        File[] allfiles = dirfile.listFiles();
        List<File> files = new ArrayList<File>();
        if (allfiles != null) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String outputDir = argMap.get(OUTPUT_DIR);
            for (File file : allfiles) {
                if (file.getName().endsWith(WSDL_SUFFIX)) {
                    files.add(file);
                }
                if (file.getName().endsWith(WSDL_SUFFIX) || file.getName().endsWith(XSD_SUFFIX)) {
                    md.update(FileUtil.getBytes(file));
                }
            }
            computedHash = md.digest();
            hashFile = new File(outputDir + File.separator + argMap.get(BASE_PACKAGE).replace('.', File.separatorChar) + File.separator + "hash.md5");
            if (hashFile.exists()) {
                byte[] readHash = FileUtil.getBytes(hashFile);
                if (Arrays.equals(readHash, computedHash)) {
                    System.out.println("Skipping generation, files not changed.");
                    files.clear();
                }
            }
        }
        File[] filesarr = new File[files.size()];
        files.toArray(filesarr);
        return filesarr;
    }

    /**
   * Parse WSDL files - extract types (schemas) and generate metadata for marshalling XmlBeans objects to XTee queries.
   * 
   * @param wsdls
   * @throws Exception
   */
    private static void loadWsdlSchemasAndGenerateMetadata(File[] wsdls) throws Exception {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        fac.setValidating(false);
        DocumentBuilder builder = fac.newDocumentBuilder();
        for (File wsdl : wsdls) {
            System.out.println(wsdl.getName());
            Document xmlWsdl = builder.parse(wsdl);
            curWsdl = wsdl;
            schemas.addAll(getSchemas(xmlWsdl.getElementsByTagNameNS(WSDL_NS, "types").item(0), getNamespaces(xmlWsdl), wsdl.getParent()));
            createMetadata(xmlWsdl);
        }
    }

    /**
   * Creates a map of the namespaces from the WSDL definitions element for XmlBeans
   * 
   * @param wsdlDoc
   */
    private static Map<String, String> getNamespaces(Document wsdlDoc) {
        Map<String, String> additionalNs = new HashMap<String, String>();
        NamedNodeMap map = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "definitions").item(0).getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attribute = map.item(i);
            if (NS_PREFIX.equals(attribute.getPrefix()) && attribute.getLocalName() != null) {
                additionalNs.put(attribute.getLocalName(), attribute.getNodeValue());
            }
        }
        return additionalNs;
    }

    /**
   * Return the schemas contained in the given wsdl:types node.
   * 
   * @param typesNode
   * @param additionalNs
   * @return A collection of schemas found under the Node
   * @throws Exception
   */
    private static Collection<XmlObject> getSchemas(Node typesNode, Map<String, String> additionalNs, String schemaPath) throws Exception {
        List<XmlObject> schemas = new ArrayList<XmlObject>();
        NodeList schemaNodes = typesNode.getChildNodes();
        XmlOptions options = new XmlOptions();
        options.setLoadAdditionalNamespaces(additionalNs);
        for (int i = 0; i < schemaNodes.getLength(); i++) {
            Node schemaNode = schemaNodes.item(i);
            if (SCHEMA_NS.equals(schemaNode.getNamespaceURI()) && "schema".equals(schemaNode.getLocalName())) {
                XmlObject schema = XmlObject.Factory.parse(schemaNode, options);
                schema.documentProperties().setSourceName("file://" + schemaPath.replace(File.separator, "/") + "/");
                schemas.add(schema);
            } else if (schemaNode.getNodeType() != Node.TEXT_NODE && schemaNode.getNodeType() != Node.COMMENT_NODE) {
                throw new IllegalStateException("Encountered unsupported element in WSDL types definition: ({" + schemaNode.getNamespaceURI() + "}" + schemaNode.getLocalName() + ")!");
            }
        }
        return schemas;
    }

    /**
   * Creates metadata needed to marshal XmlBeans objects to valid XTee requests.
   * 
   * @param wsdlDoc
   * @throws Exception
   */
    private static void createMetadata(Document wsdlDoc) throws Exception {
        Map<String, QName> messageMap = getMessageMap(wsdlDoc);
        String opNs = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "definitions").item(0).getAttributes().getNamedItem("targetNamespace").getNodeValue().toLowerCase();
        String database;
        if (opNs.matches("http://producers\\..+?\\.xtee\\.riik\\.ee/producer/.+?$")) {
            database = opNs.substring(opNs.lastIndexOf("/") + 1);
        } else {
            System.out.println("WARNING: WSDL namespace does not match X-tee convention (found: " + opNs + "), setting database name from WSDL filename!");
            database = curWsdl.getName().substring(0, curWsdl.getName().toLowerCase().indexOf(".wsdl"));
        }
        Node binding = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "binding").item(0);
        NodeList bindingChildren = binding.getChildNodes();
        Map<String, String> versionMap = new HashMap<String, String>();
        for (int i = 0; i < bindingChildren.getLength(); i++) {
            Node bindingChild = bindingChildren.item(i);
            if (WSDL_NS.equals(bindingChild.getNamespaceURI()) && "operation".equals(bindingChild.getLocalName())) {
                Node operation = bindingChild;
                String opname = operation.getAttributes().getNamedItem("name").getNodeValue();
                NodeList operationChildren = operation.getChildNodes();
                String version = null;
                for (int j = 0; j < operationChildren.getLength(); j++) {
                    Node operationChild = operationChildren.item(j);
                    if (XTeeUtil.XTEE_NS_URI.equals(operationChild.getNamespaceURI()) && "version".equals(operationChild.getLocalName())) {
                        version = SOAPUtil.getTextContent(operationChild);
                        break;
                    }
                }
                if (version == null) {
                    System.out.println("WARNING: Did not find version of operation \"" + opname + "\". Assuming version 1");
                    version = "v1";
                }
                versionMap.put(opname, version);
            }
        }
        Node portType = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "portType").item(0);
        NodeList portTypeChildren = portType.getChildNodes();
        mainLoop: for (int i = 0; i < portTypeChildren.getLength(); i++) {
            Node portTypeChild = portTypeChildren.item(i);
            if (WSDL_NS.equals(portTypeChild.getNamespaceURI()) && "operation".equals(portTypeChild.getLocalName())) {
                Node operation = portTypeChild;
                String opname = operation.getAttributes().getNamedItem("name").getNodeValue();
                if (!versionMap.containsKey(opname)) {
                    continue;
                }
                String requestElementName = null;
                String requestElementNs = null;
                String responseElementName = null;
                String responseElementNs = null;
                NodeList operationChildren = operation.getChildNodes();
                for (int j = 0; j < operationChildren.getLength(); j++) {
                    Node operationChild = operationChildren.item(j);
                    if (WSDL_NS.equals(operationChild.getNamespaceURI()) && ("input".equals(operationChild.getLocalName()) || "output".equals(operationChild.getLocalName()))) {
                        String message = operationChild.getAttributes().getNamedItem("message").getNodeValue().split(":", 2)[1];
                        QName elementQName = messageMap.get(message);
                        if (elementQName == null) {
                            System.out.println("WARNING: Did not find \"keha\" part in message \"" + message + "\" (operation \"" + opname + "\").");
                            continue mainLoop;
                        }
                        if ("input".equals(operationChild.getLocalName())) {
                            requestElementName = elementQName.getLocalPart();
                            requestElementNs = elementQName.getNamespaceURI();
                        } else if ("output".equals(operationChild.getLocalName())) {
                            responseElementName = elementQName.getLocalPart();
                            responseElementNs = elementQName.getNamespaceURI();
                        }
                    }
                }
                if (requestElementName == null || responseElementName == null) {
                    System.out.println("WARNING: Did not find \"input\" or \"output\" of operation \"" + opname + "\"");
                    continue mainLoop;
                }
                String version = versionMap.get(opname);
                metadata.put(database + opname.toLowerCase(), new XmlBeansXTeeMetadata(opname, opNs, requestElementName, requestElementNs, responseElementName, responseElementNs, version));
            }
        }
    }

    /**
   * Creates a map between message names and their response elements.
   * 
   * @param wsdlDoc
   * @return
   */
    private static Map<String, QName> getMessageMap(Document wsdlDoc) {
        Map<String, QName> messageMap = new HashMap<String, QName>();
        NodeList messages = wsdlDoc.getElementsByTagNameNS(WSDL_NS, "message");
        for (int i = 0; i < messages.getLength(); i++) {
            Node message = messages.item(i);
            NodeList parts = message.getChildNodes();
            for (int j = 0; j < parts.getLength(); j++) {
                Node part = parts.item(j);
                if (WSDL_NS.equals(part.getNamespaceURI()) && "part".equals(part.getLocalName()) && "keha".equals(part.getAttributes().getNamedItem("name").getNodeValue())) {
                    Node element = part.getAttributes().getNamedItem("element");
                    String[] type = element == null ? part.getAttributes().getNamedItem("type").getNodeValue().split(":", 2) : element.getNodeValue().split(":", 2);
                    messageMap.put(message.getAttributes().getNamedItem("name").getNodeValue(), new QName(wsdlDoc.lookupNamespaceURI(type[0]), type[1]));
                    break;
                }
            }
        }
        return messageMap;
    }

    private static void generateDatabaseClasses(String outputdir) throws IOException, TemplateException {
        DatabaseClasses classes = new DatabaseClasses(argMap.get(XSB_DIR), argMap.get(DB_CLASSES_PACKAGE));
        for (Map.Entry<String, XmlBeansXTeeMetadata> entry : metadata.entrySet()) {
            XmlBeansXTeeMetadata serviceMetadata = entry.getValue();
            String key = entry.getKey();
            String database = key.substring(0, key.indexOf(serviceMetadata.getOperationName().toLowerCase()));
            classes.add(database, serviceMetadata);
        }
        DatabaseGenerator.generate(classes, outputdir);
    }

    private static class Timer {

        private double start;

        public void start() {
            start = System.currentTimeMillis();
        }

        public double finish() {
            return System.currentTimeMillis() - start;
        }

        public String finishStr() {
            return finish() / 1000 + " seconds.";
        }
    }
}
