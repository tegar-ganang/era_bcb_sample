package org.owasp.orizon.xml;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.owasp.orizon.About;
import org.owasp.orizon.OrizonLog;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This is an abstract for classes reading XML files
 * 
 * @author sp0nge
 */
public abstract class Reader extends org.owasp.orizon.O {

    protected String xmlFilename;

    boolean extractedXMLPresent;

    protected boolean result_ready;

    protected DocumentBuilderFactory docBuilderFactory;

    protected DocumentBuilder docBuilder;

    protected Document doc;

    protected boolean readFlag;

    private static void process(InputStream input, String name) throws IOException {
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);
        String line;
        OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(name)), "8859_1");
        while ((line = reader.readLine()) != null) out.write(line);
        reader.close();
        out.close();
    }

    public boolean readCalled() {
        return readFlag;
    }

    /**
	 * Creates a new Reader object
	 * 
	 * @param xmlFileName
	 *            the name of the XML file being read.
	 */
    public Reader(String xmlFileName) {
        super(Reader.class);
        readFlag = false;
        if (Reader.class.getResource("/lib/orizon_log4j.props") != null) {
            extractedXMLPresent = true;
            try {
                JarFile jar = new JarFile(About.ORIZON_JAR_NAME);
                JarEntry entry = jar.getJarEntry(xmlFileName);
                xmlFileName = "extracted.xml";
                InputStream is = jar.getInputStream(entry);
                process(is, xmlFileName);
                initialized = true;
            } catch (Exception e) {
                initialized = false;
            }
            if (!initialized) {
                try {
                    JarFile jar = new JarFile("./Orizon.app/Contents/Resources/Java/" + About.ORIZON_JAR_NAME);
                    JarEntry entry = jar.getJarEntry(xmlFileName);
                    xmlFileName = "extracted.xml";
                    InputStream is = jar.getInputStream(entry);
                    process(is, xmlFileName);
                    initialized = true;
                } catch (IOException e) {
                    initialized = false;
                    log.error("something were wrong creating Reader (" + e.getMessage() + ")");
                }
            }
        }
        if (xmlFileName == null) {
            log.error("Reader(): null parameter");
            initialized = false;
            return;
        }
        this.xmlFilename = xmlFileName;
        xmlDocumentSetup(new File(xmlFileName));
    }

    /**
	 * Creates a new Reader object
	 * 
	 * Only used by Source class... it tells the Reader not trying to extract
	 * the XML file from orizon JAR.
	 * 
	 * @param xmlFileName
	 *            the name of the XML file being read.
	 */
    public Reader(String xmlFileName, boolean outSideJar) {
        super(Reader.class);
        readFlag = false;
        if (xmlFileName == null) {
            log.error("Reader(): null parameter");
            initialized = false;
            return;
        }
        this.xmlFilename = xmlFileName;
        xmlDocumentSetup(new File(xmlFileName));
    }

    public boolean clean() {
        if (extractedXMLPresent) {
            File f = new File("extracted.xml");
            return f.delete();
        }
        return false;
    }

    private boolean xmlDocumentSetup(File f) {
        initialized = true;
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.parse(f);
        } catch (ParserConfigurationException e) {
            log.error(xmlFilename + ": a ParserConfigurationException occured...");
            log.error(xmlFilename + ": " + e.getLocalizedMessage());
            initialized = false;
        } catch (SAXException e) {
            log.error(xmlFilename + ": a SAXException occured...");
            log.error(xmlFilename + ": " + e.getLocalizedMessage());
            initialized = false;
        } catch (IOException e) {
            log.error(xmlFilename + ": an IOException occured...");
            log.error(xmlFilename + ": " + e.getLocalizedMessage());
            initialized = false;
        }
        if (doc != null) {
            if (doc.getDocumentElement() != null) doc.getDocumentElement().normalize(); else log.warning("getDocumentElement() returned null...");
        } else {
            log.error("doc is null???");
            initialized = false;
        }
        return initialized;
    }

    public String getXmlFilename() {
        if (!initialized) {
            log.error("reader has not been initialized");
            return null;
        }
        return xmlFilename;
    }

    protected Document getDocumentRoot() {
        if (!initialized) {
            log.error("reader has not been initialized");
            return null;
        }
        return doc;
    }

    public abstract boolean read();

    public boolean areResultsReady() {
        return result_ready;
    }

    /**
	 * Check if this version of Orizon supports this file XML
	 * 
	 * @param version
	 * @return
	 */
    protected boolean isSupported(String version) {
        int req_maj, req_min;
        String[] result = version.split("[.]");
        if (result.length < 2) {
            log.error("invalid version string \"" + version + "\"");
            return false;
        }
        req_maj = (new Integer(result[0])).intValue();
        req_min = (new Integer(result[1])).intValue();
        return ((req_maj <= About.ORIZON_MAJOR) && (req_min <= About.ORIZON_MINOR)) ? true : false;
    }

    protected final boolean isGoodDocumentRoot(String expectedTag) {
        if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase(expectedTag)) {
            log.debug("<" + expectedTag + "> expected while <" + doc.getDocumentElement().getNodeName() + "> found");
            return false;
        } else return true;
    }

    public final boolean dispose() {
        File f = new File(xmlFilename);
        return f.delete();
    }

    @Override
    public boolean readXmlNode(Node n) {
        return false;
    }
}
