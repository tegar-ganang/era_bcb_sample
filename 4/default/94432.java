import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.net.*;

/**
 * Loads the configuration file
 * 
 * @author aaronmckenna
 * 
 */
public class ConfigurationObject {

    private Document dom = null;

    private String filename = null;

    private String baseDir = null;

    /**
	 * creates a configuration file
	 * 
	 * @param filename
	 */
    public ConfigurationObject(String filename) {
        this.filename = filename;
        loadFile();
    }

    private void loadFile() {
        try {
            File newfile = new File(filename);
            BufferedReader data = null;
            if (!newfile.exists()) {
                try {
                    InputStream st = this.getClass().getResourceAsStream("/configuration.xml");
                    data = new BufferedReader(new InputStreamReader(st));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BufferedWriter out;
                out = new BufferedWriter(new FileWriter(filename));
                String readln = data.readLine();
                while (readln != null) {
                    out.write(readln);
                    System.out.println(readln);
                    readln = data.readLine();
                }
                out.close();
            }
            parseXmlFile(new File(filename));
            parseDocument();
        } catch (Exception e) {
            System.out.println("Failed to load file");
            e.printStackTrace();
        }
    }

    private void parseXmlFile(File f) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (f.exists()) {
                dom = db.parse(f);
            } else {
                try {
                    throw new FileNotFoundException(f.toString() + " Not found");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void parseDocument() {
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("dataDir");
        if (nl != null && nl.getLength() == 1) {
            Element el = (Element) nl.item(0);
            String val = el.getFirstChild().getTextContent();
            if (val != null) {
                baseDir = val;
            }
        }
    }

    private void setBaseDir() {
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("dataDir");
        if (nl != null && nl.getLength() == 1) {
            Element el = (Element) nl.item(0);
            el.getFirstChild().setNodeValue(baseDir);
            writeConfiguration();
        }
    }

    public void writeConfiguration() {
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(dom);
            trans.transform(source, result);
            String xmlString = sw.toString();
            System.out.println("Here's the xml:\n\n" + xmlString);
            BufferedWriter out;
            out = new BufferedWriter(new FileWriter(filename));
            out.write(xmlString);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * @return the baseDir
	 */
    public String getBaseDir() {
        return baseDir;
    }

    /**
	 * @param baseDir
	 *            the baseDir to set
	 */
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        setBaseDir();
    }
}
