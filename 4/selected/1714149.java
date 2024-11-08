package polysema.virtuamea.data;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CommonData {

    public static final String ALGORITHM_DIRECTORY = "./Resources/Algorithms/";

    private static String snapshotOutputPath = "./Resources/Snapshots/";

    private static String mpeg7XmlSchemaPath = "./Resources/XMLSchema/Mpeg7-2001.xsd";

    private static Dimension defaultSnapshotDimension = new Dimension(162, 120);

    private static ArrayList<AutoShotAlgoInfo> autoshotalgos = new ArrayList<AutoShotAlgoInfo>(5);

    private static final String algoConfigFile = "./Resources/autoshot_algo.xml";

    public static String getSnapshotOutputPath() {
        return snapshotOutputPath;
    }

    public static void setSnapshotOutputPath(String outputPath) {
        CommonData.snapshotOutputPath = outputPath;
    }

    public static String getMPEG7XmlSchemaPath() {
        return mpeg7XmlSchemaPath;
    }

    public static void setMPEG7XmlSchemaPath(String xmlSchemaPath) {
        CommonData.mpeg7XmlSchemaPath = xmlSchemaPath;
    }

    public static Dimension getDefaultSnapshotDimension() {
        return defaultSnapshotDimension;
    }

    public static void setDefaultSnapshotDimension(Dimension defaultSnapshotDimension) {
        CommonData.defaultSnapshotDimension = defaultSnapshotDimension;
    }

    public static boolean loadAutoShotAlgo() {
        Document document;
        if ((document = getDocumentFromXMLFile(algoConfigFile)) != null) {
            List<Element> elts = document.getRootElement().elements();
            for (Element elt : elts) {
                String name = elt.elementText("name").trim();
                String classname = elt.attributeValue("className").trim();
                String isDefault = elt.attributeValue("isDefault").trim();
                String author = elt.elementText("author").trim();
                String description = elt.elementText("description").trim();
                autoshotalgos.add(new AutoShotAlgoInfo(classname, name, description, author, new Boolean(isDefault)));
            }
            return true;
        }
        return false;
    }

    private static Document getDocumentFromXMLFile(String path) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
            SAXReader reader = new SAXReader(parser.getXMLReader());
            reader.setValidation(false);
            reader.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException exception) throws SAXException {
                    System.err.println("Line " + exception.getLineNumber() + ": " + exception.getMessage());
                    throw new SAXException();
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    System.err.println("Line " + exception.getLineNumber() + ": " + exception.getMessage());
                    throw new SAXException();
                }

                public void warning(SAXParseException exception) throws SAXException {
                    System.err.println("Line " + exception.getLineNumber() + ": " + exception.getMessage());
                }
            });
            return reader.read(new File(algoConfigFile).toURI().toURL());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void addAutoShotAlgorithm(AutoShotAlgoInfo algo) {
        Document document;
        if ((document = getDocumentFromXMLFile(algoConfigFile)) != null) {
            Element newAlgo = document.getRootElement().addElement("algorithm");
            newAlgo.addAttribute("className", algo.getClassName());
            newAlgo.addAttribute("isDefault", String.valueOf(algo.isDefault()));
            newAlgo.addElement("name").setText(algo.getName());
            newAlgo.addElement("description").setText(algo.getDescription());
            newAlgo.addElement("author").setText(algo.getAuthor());
            writeAlgosToXML(document);
        }
    }

    private static void writeAlgosToXML(Document document) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(algoConfigFile);
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(fos, format);
            writer.write(document);
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<AutoShotAlgoInfo> getAutoshotalgos() {
        return autoshotalgos;
    }

    public static boolean removeAutoShotAlgorithm(String classname) {
        Document document;
        if ((document = getDocumentFromXMLFile(algoConfigFile)) != null) {
            Node node = document.getRootElement().selectSingleNode("//algorithm[@className='" + classname + "']");
            document.getRootElement().remove(node);
            writeAlgosToXML(document);
            loadAutoShotAlgo();
            return true;
        }
        return false;
    }

    public static void saveAlgos(ArrayList<AutoShotAlgoInfo> algos) {
        Document document;
        if ((document = getDocumentFromXMLFile(algoConfigFile)) != null) {
            document.getRootElement().clearContent();
            writeAlgosToXML(document);
            for (AutoShotAlgoInfo algo : algos) {
                addAutoShotAlgorithm(algo);
            }
        }
    }

    public static void copyFile(File src, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        java.nio.channels.FileChannel channelSrc = fis.getChannel();
        java.nio.channels.FileChannel channelDest = fos.getChannel();
        channelSrc.transferTo(0, channelSrc.size(), channelDest);
        fis.close();
        fos.close();
    }
}
