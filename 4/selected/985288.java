package org.akrogen.core.codegen.template;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import org.akrogen.core.xml.utils.XMLUtils;
import org.w3c.dom.Document;

/**
 * Base class for template engine.
 * 
 * @version 1.0.0
 * @author <a href="mailto:angelo.zerr@gmail.com">Angelo ZERR</a>
 * 
 */
public abstract class AbstractTemplateEngine implements ITemplateEngine {

    /**
	 * Document builder used to remove DocType (DTD, Schema) of the XML file in
	 * order to load even if DTD doesn't exist.
	 * 
	 */
    private static DocumentBuilder documentBuilder;

    /**
	 * Key used into template to call XML document. (usefull with Freemarker and
	 * Velocity)
	 */
    protected static final String XML_DOCUMENT_CONTEXT = "doc";

    /**
	 * Template base dir.
	 */
    protected String templateBaseDir;

    /**
	 * Configuration ID (defined into XML Spring configuration)
	 */
    private String configurationId;

    /**
	 * Template path (must be setted if templateReader is not defined).
	 */
    protected String templatePath;

    /**
	 * Template reader (must be setted if templatePath is not defined).
	 */
    protected Reader templateReader;

    /**
	 * Set template reader.
	 */
    public void setTemplate(Reader templateReader) {
        this.templateReader = templateReader;
    }

    /**
	 * Set template path.
	 */
    public void setTemplate(String templatePath) {
        this.templatePath = templatePath;
    }

    public void putXML(String key, String xmlContent, boolean omitDocumentType) throws Exception {
        if (!omitDocumentType) putXML(key, xmlContent); else {
            Document document = getDocumentBuilder().parse(new ByteArrayInputStream(xmlContent.getBytes()));
            InputStream inputStream = XMLUtils.getInputStream(document, omitDocumentType);
            putXML(key, inputStream);
        }
    }

    public void putXML(String key, File xmlFileSource, boolean omitDocumentType) throws Exception {
        if (!omitDocumentType) putXML(key, xmlFileSource); else {
            Document document = getDocumentBuilder().parse(new FileInputStream(xmlFileSource));
            InputStream inputStream = XMLUtils.getInputStream(document, omitDocumentType);
            putXML(key, inputStream);
        }
    }

    public void putXML(String key, InputStream inXMLSource, boolean omitDocumentType) throws Exception {
        if (!omitDocumentType) putXML(key, inXMLSource); else {
            Document document = getDocumentBuilder().parse(inXMLSource);
            InputStream inputStream = XMLUtils.getInputStream(document, omitDocumentType);
            putXML(key, inputStream);
        }
    }

    /**
	 * Merge template with parameters putted and XML document (if it exists).
	 * Result merge is written into the file with name fileName.
	 * 
	 * @param fileName
	 *            name of file to merge into.
	 */
    public void merge(String fileName) throws Exception {
        merge(new File(fileName));
    }

    /**
	 * Merge template with parameters putted and XML document (if it exists).
	 * Result merge is written into the file.
	 * 
	 * @param file
	 *            file to merge into.
	 */
    public void merge(File file) throws Exception {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            merge(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 * Merge template with parameters putted and XML document (if it exists).
	 * Result merge is returned into a Reader.
	 * 
	 * @return
	 */
    public Reader merge() throws Exception {
        StringWriter writer = new StringWriter();
        merge(writer);
        writer.flush();
        writer.close();
        StringReader reader = new StringReader(writer.getBuffer().toString());
        return reader;
    }

    /**
	 * Return the configuration ID (defined into the Spring configuration) if
	 * template engine wait a configuration (Configuration for Freemarker,
	 * VelocityEngine for Velocity,...) otherwise return null.
	 * 
	 * @return
	 */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
	 * Set Configuration ID.
	 * 
	 * @param configurationId
	 */
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    /**
	 * Set template base dir.
	 * 
	 * @param templateBaseDir
	 */
    public void setTemplateBaseDir(String templateBaseDir) {
        this.templateBaseDir = templateBaseDir;
    }

    /**
	 * Set the configuration waited by template engine.
	 * 
	 * @param configuration
	 */
    public void setConfiguration(ITemplateConfiguration configuration) {
    }

    private static DocumentBuilder getDocumentBuilder() throws Exception {
        if (documentBuilder == null) {
            documentBuilder = XMLUtils.getDocumentBuilderEmptyEntityResolver();
        }
        return documentBuilder;
    }
}
