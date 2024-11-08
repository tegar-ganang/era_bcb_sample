package de.iritgo.aktera.importer;

import de.iritgo.aktera.i18n.I18N;
import de.iritgo.aktera.logger.Logger;
import de.iritgo.aktera.model.ModelException;
import de.iritgo.aktera.model.ModelRequest;
import de.iritgo.simplelife.string.StringTools;
import org.apache.avalon.framework.configuration.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class ImportManagerImpl implements ImportManager {

    /** Logger service */
    public Logger logger;

    /** The I18N service */
    private I18N i18n;

    /** Import handler configuration */
    private Configuration configuration;

    /** Import configs sorted by dependencies. */
    private List<ImportHandlerConfig> importHandlerConfigs;

    /** CSV imports. */
    private Map<String, CsvImportHandlerConfig> csvImportHandlerConfigs;

    /**
	 * Set the I18N service.
	 */
    public void setI18n(I18N i18n) {
        this.i18n = i18n;
    }

    /**
	 * Set the logger service.
	 */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
	 * Set the import handler configuration.
	 */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
	 * Set the import handler configs.
	 */
    public void setImportHandlerConfigs(List<ImportHandlerConfig> importHandlerConfigs) {
        this.importHandlerConfigs = importHandlerConfigs;
    }

    /**
	 * Service initialization.
	 */
    public void initialize() {
        logger.info("Reading import handler definitions");
        List<Configuration> sortedConfigs = new LinkedList<Configuration>();
        List<String> resolvedImportIds = new LinkedList<String>();
        LinkedList<Configuration> configs = new LinkedList<Configuration>();
        Configuration[] importConfigs = configuration.getChildren("import");
        for (Configuration config : importConfigs) {
            configs.add(config);
        }
        while (!configs.isEmpty()) {
            Configuration config = configs.poll();
            String importId = config.getAttribute("id", "unknown");
            Configuration[] dependConfigs = config.getChildren("depends");
            boolean resolved = true;
            for (Configuration dependConfig : dependConfigs) {
                String dependendImportId = dependConfig.getAttribute("import", null);
                if (!StringTools.isTrimEmpty(dependendImportId) && !resolvedImportIds.contains(dependendImportId)) {
                    configs.addLast(config);
                    resolved = false;
                }
            }
            if (resolved) {
                sortedConfigs.add(config);
                resolvedImportIds.add(importId);
            }
        }
        importHandlerConfigs = new LinkedList<ImportHandlerConfig>();
        for (Configuration config : sortedConfigs) {
            try {
                Class<?> handlerClass = Class.forName(config.getAttribute("class"));
                ImportHandler handler = (ImportHandler) handlerClass.newInstance();
                ImportHandlerConfig ihc = new ImportHandlerConfig(config.getAttribute("id"), config.getAttribute("root"), handler);
                importHandlerConfigs.add(ihc);
            } catch (Exception x) {
                logger.error("Unable to read import handler configuration '" + config.getAttribute("id", "?") + "'", x);
            }
        }
        csvImportHandlerConfigs = new HashMap<String, CsvImportHandlerConfig>();
        for (Configuration config : configuration.getChildren("csvImport")) {
            try {
                CsvImportHandlerConfig cihc = new CsvImportHandlerConfig(config.getAttribute("id"), config.getAttribute("xsl"));
                csvImportHandlerConfigs.put(cihc.getId(), cihc);
            } catch (Exception x) {
                logger.error("Unable to read csv import handler configuration '" + config.getAttribute("id", "?") + "'", x);
            }
        }
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#getImportHandlerConfigs()
	 */
    public List<ImportHandlerConfig> getImportHandlerConfigs() {
        return importHandlerConfigs;
    }

    /**
	 * Find an import handler for the specified root tag.
	 *
	 * @param root The name of the root tag
	 * @return An import handler config or null if none was found
	 */
    protected ImportHandlerConfig getImportHandlerConfigByRoot(String root) {
        for (ImportHandlerConfig importHandlerConfig : importHandlerConfigs) {
            if (root.equals(importHandlerConfig.getRoot())) {
                return importHandlerConfig;
            }
        }
        return null;
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#getCsvImportHandlerIds()
	 */
    public Collection<String> getCsvImportHandlerIds() {
        return csvImportHandlerConfigs.keySet();
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#getCsvImportHandlerXsl(java.lang.String)
	 */
    public String getCsvImportHandlerXsl(String id) {
        return csvImportHandlerConfigs.get(id).getXsl();
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#validateXml(java.io.File)
	 */
    public boolean validateXmlFile(File importFile) {
        XMLReader reader = null;
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new DefaultHandler() {
            });
            reader.setErrorHandler(new DefaultHandler() {
            });
        } catch (SAXException x) {
            logger.error("Unable to create XML reader");
            return false;
        }
        try {
            reader.parse(new InputSource(new FileReader(importFile)));
        } catch (FileNotFoundException x) {
            return false;
        } catch (IOException x) {
            return false;
        } catch (SAXException x) {
            return false;
        }
        return true;
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#importXmlFile(java.io.File, java.io.PrintWriter)
	 */
    public void importXmlFile(File importFile, final PrintWriter reporter) {
        importXmlFile(importFile, null, reporter, new Properties());
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#importXmlFile(java.io.File, java.io.PrintWriter, java.util.Properties)
	 */
    public void importXmlFile(File importFile, final PrintWriter reporter, Properties properties) {
        importXmlFile(importFile, null, reporter, properties);
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#importXmlFile(java.io.File, java.lang.String, java.io.PrintWriter)
	 */
    public void importXmlFile(File importFile, final String importHandler, final PrintWriter reporter) {
        importXmlFile(importFile, importHandler, reporter, new Properties());
    }

    /**
	 * @see de.iritgo.aktera.importer.ImportManager#importXmlFile(java.io.File, java.lang.String, java.io.PrintWriter)
	 */
    public void importXmlFile(File importFile, final String importHandler, final PrintWriter reporter, final Properties properties) {
        XMLReader reader = null;
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new DefaultHandler() {

                boolean importNodeFound = false;

                String currentTag = null;

                ImportHandler currentHandler = null;

                StringBuilder tagContent = new StringBuilder();

                @Override
                public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
                    if (!importNodeFound) {
                        if ("import".equals(localName)) {
                            importNodeFound = true;
                        }
                        return;
                    }
                    if (currentTag == null) {
                        ImportHandlerConfig ihc = getImportHandlerConfigByRoot(localName);
                        if (ihc == null || (importHandler != null && !ihc.getId().equals(importHandler))) {
                            return;
                        }
                        currentTag = localName;
                        currentHandler = ihc.getHandler();
                        currentHandler.startRootElement(uri, localName, name, attributes, reporter, properties);
                        return;
                    }
                    if (currentHandler != null) {
                        tagContent = new StringBuilder();
                        currentHandler.startElement(uri, localName, name, attributes, reporter, properties);
                    }
                }

                @Override
                public void endElement(String uri, String localName, String name) throws SAXException {
                    if (localName.equals(currentTag)) {
                        currentHandler.endRootElement(uri, localName, name, reporter, properties);
                        currentTag = null;
                        currentHandler = null;
                        return;
                    }
                    if (currentHandler != null) {
                        boolean allwhite = true;
                        int j = 0;
                        while ((j < tagContent.length()) && (allwhite)) {
                            allwhite = Character.isWhitespace(tagContent.charAt(j));
                            ++j;
                        }
                        if (!allwhite) {
                            currentHandler.elementContent(uri, localName, name, StringTools.trim(tagContent.toString()), reporter, properties);
                        }
                        tagContent = new StringBuilder();
                        currentHandler.endElement(uri, localName, name, reporter, properties);
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    tagContent.append(ch, start, length);
                }
            });
            reader.setErrorHandler(new DefaultHandler() {

                @Override
                public void error(SAXParseException e) throws SAXException {
                    reporter.write("Error: " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    reporter.write("Error: " + e.getMessage());
                }

                @Override
                public void warning(SAXParseException e) throws SAXException {
                    reporter.write("Error: " + e.getMessage());
                }
            });
        } catch (SAXException x) {
            reporter.write("Error: Unable to create XML reader");
            return;
        }
        try {
            reader.parse(new InputSource(new FileReader(importFile)));
        } catch (FileNotFoundException x) {
            reporter.write("Error: Import file '" + importFile.getAbsolutePath() + "' not found: " + x.getMessage());
        } catch (IOException x) {
            reporter.write("Error: " + x.getMessage());
        } catch (SAXException x) {
            reporter.write("Error: " + x.getMessage());
        }
    }

    /**
	 * Analyse an import with all registered handlers.
	 */
    public boolean analyzeImport(ModelRequest req, Document doc, Node importElem, PrintWriter reporter, I18N i18n, Properties properties) throws ModelException {
        return analyzeImport(req, doc, importElem, reporter, i18n, null, properties);
    }

    /**
	 * Analyse an import with a specified handler.
	 */
    public boolean analyzeImport(ModelRequest req, Document doc, Node importElem, PrintWriter reporter, I18N i18n, String handlerId, Properties properties) throws ModelException {
        ImportManager im = (ImportManager) req.getSpringBean(ImportManager.ID);
        boolean res = true;
        for (ImportHandlerConfig importHandlerConfig : im.getImportHandlerConfigs()) {
            if (!StringTools.isTrimEmpty(handlerId) && !handlerId.equals(importHandlerConfig.getId())) {
                continue;
            }
            try {
                res = res && importHandlerConfig.getHandler().analyze(req, doc, importElem, reporter, i18n, properties);
            } catch (Exception x) {
                throw new ModelException("ImportManager: Unable to call analyze method in import handler '" + importHandlerConfig.getId() + "': " + x);
            }
        }
        return res;
    }

    /**
	 * Import an import with all registered handlers.
	 */
    public boolean performImport(ModelRequest req, Document doc, Node importElem, PrintWriter reporter, I18N i18n, Properties properties) throws ModelException {
        return performImport(req, doc, importElem, reporter, i18n, null, properties);
    }

    /**
	 * Import an import with a specified handler.
	 */
    public boolean performImport(ModelRequest req, Document doc, Node importElem, PrintWriter reporter, I18N i18n, String handlerId, Properties properties) throws ModelException {
        ImportManager im = (ImportManager) req.getSpringBean(ImportManager.ID);
        boolean res = true;
        for (ImportHandlerConfig importHandlerConfig : im.getImportHandlerConfigs()) {
            if (!StringTools.isTrimEmpty(handlerId) && !handlerId.equals(importHandlerConfig.getId())) {
                continue;
            }
            try {
                res = res && importHandlerConfig.getHandler().perform(req, doc, importElem, reporter, i18n, properties);
            } catch (Exception x) {
                throw new ModelException("ImportManager: Unable to call perform method in import handler '" + importHandlerConfig.getId() + "': " + x);
            }
        }
        return res;
    }
}
