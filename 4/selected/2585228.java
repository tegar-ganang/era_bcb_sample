package com.emental.mindraider.core.notebook;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.emental.mindraider.MindRaiderConstants;
import com.emental.mindraider.core.MindRaider;
import com.emental.mindraider.core.rdf.MindRaiderVocabulary;
import com.emental.mindraider.core.rdf.RdfModel;
import com.emental.mindraider.core.rest.Data;
import com.emental.mindraider.core.rest.Metadata;
import com.emental.mindraider.core.rest.Resource;
import com.emental.mindraider.core.rest.ResourceDescriptor;
import com.emental.mindraider.core.rest.properties.AnnotationProperty;
import com.emental.mindraider.core.rest.properties.LabelProperty;
import com.emental.mindraider.core.rest.properties.ResourcePropertyGroup;
import com.emental.mindraider.core.rest.properties.SourceTwikiFileProperty;
import com.emental.mindraider.core.rest.resource.NotebookResource;
import com.emental.mindraider.core.rest.resource.NotebookResourceExpanded;
import com.emental.mindraider.ui.dialogs.ProgressDialogJFrame;
import com.emental.mindraider.ui.outline.NotebookOutlineJPanel;
import com.emental.mindraider.ui.outline.treetable.NotebookOutlineTreeInstance;
import com.emental.mindraider.ui.panels.ExplorerJPanel;
import com.emental.mindraider.ui.panels.bars.StatusBar;
import com.emental.mindraider.utils.Opmlizer;
import com.emental.mindraider.utils.TWikifier;
import com.emental.mindraider.utils.Utils;
import com.emental.mindraider.utils.Xsl;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Seq;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.mindcognition.mindraider.commons.representation.twiki.TwikiToHtml;

/**
 * Notebooks custodian. <br>
 * <br>
 * Notebook is represented as a resource and an RDF model. Note that notebooks are independent of folders (i.e. notebook
 * is not contained in a folder, but it is just linked). <br>
 * On the other hand, notebooks contain a set of resources. Relationships among these resources are captured by the RDF
 * model associated with notebook resource.
 * <ul>
 * <li>notebook.rdf.xml ... notebook metadata i.e. RDF model holding properties of resources and their relationships.
 * Model contains:
 * <ul>
 * <li>Notebook resource - MR type, rdfs:seq, rdfs:label, sub-concepts (no annotation, xlink:href is present in the
 * folders model) </li>
 * <li>Concept resource - MR type, rdfs:seq, rdfs:label, rdfs:comment (annotation snippet), mr:attachment, xlink:href,
 * dc:created </li>
 * </ul>
 * Primary source of information are resources - RDF model is just metadata/search layer.
 * <li>notebook.xml ... resource representing notebook itself
 * <li>dc.rdf.xml ... Dublin Core annotation
 * </ul>
 * Maintenance and handling of notebooks is driven by URIs i.e. labels, NCNames and other characteristics are not
 * importaint - an only think that matters is uniquie notebook's URI.
 * @author Martin.Dvorak
 */
public class NotebookCustodian {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(NotebookCustodian.class);

    public static final String FILENAME_XML_RESOURCE = "notebook.xml";

    public static final String FILENAME_RDF_MODEL = "notebook.rdf.xml";

    public static final String FILENAME_DC = "dc.rdf.xml";

    public static final String DIRECTORY_CONCEPTS = "concepts";

    public static final String DIRECTORY_ANNOTATIONS = "annotations";

    public static String MR_DOC_FOLDER_LOCAL_NAME = "MR";

    public static String MR_DOC_NOTEBOOK_INTRODUCTION_LOCAL_NAME = "Introduction";

    public static String MR_DOC_NOTEBOOK_DOCUMENTATION_LOCAL_NAME = "MR_Documentation";

    public static String MR_DOC_NOTEBOOK_FOR_DEVELOPERS_LOCAL_NAME = "For_Developers";

    /**
     * Notebooks location.
     */
    private String notebooksDirectory;

    /**
     * Subscribers array.
     */
    private ArrayList subscribers;

    /**
     * Active notebook resource (note that model is contained in it).
     */
    private NotebookResource activeNotebookResource;

    /**
     * Constructor.
     */
    public NotebookCustodian(String notebooksDirectory) {
        this.notebooksDirectory = notebooksDirectory;
        subscribers = new ArrayList();
        logger.debug("  Notebooks directory is: " + notebooksDirectory);
        Utils.createDirectory(notebooksDirectory);
    }

    /**
     * Create new notebook - directories and associated model.
     * <ul>
     * <li>notebook.rdf.xml ... notebook metadata i.e. RDF model holding properties of resources and their
     * relationships
     * <li>notebook.xml ... resource representing notebook itself
     * </ul>
     * @param label
     * @param uri
     * @param renderUri
     * @return URI of the newly create notebook
     */
    public String create(String label, String uri, String annotation, boolean renderUi) throws Exception {
        logger.debug("Creating new notebook: " + label + " (" + uri + ")");
        String notebookDirectory = getNotebookDirectory(uri);
        if (new File(notebookDirectory).exists()) {
            return "EXISTS";
        }
        Utils.createDirectory(notebookDirectory + DIRECTORY_CONCEPTS);
        Utils.createDirectory(notebookDirectory + DIRECTORY_ANNOTATIONS);
        Resource resource = new Resource();
        Metadata meta = resource.getMetadata();
        meta.setAuthor(new URI(MindRaider.profile.getProfileName()));
        meta.setCreated(System.currentTimeMillis());
        meta.setRevision(1);
        meta.setTimestamp(meta.getCreated());
        meta.setUri(new URI(uri));
        resource.addProperty(new LabelProperty(label));
        Data data = resource.getData();
        data.addPropertyGroup(new ResourcePropertyGroup(NotebookResource.PROPERTY_GROUP_LABEL_CONCEPTS, new URI(NotebookResource.PROPERTY_GROUP_URI_CONCEPTS)));
        resource.setData(data);
        if (annotation == null) {
            annotation = "'" + label + "' notebook.";
        }
        resource.addProperty(new AnnotationProperty(annotation));
        String notebookResourceFilename = notebookDirectory + FILENAME_XML_RESOURCE;
        resource.toXmlFile(notebookResourceFilename);
        String notebookModelFilename = getModelFilenameByDirectory(notebookDirectory);
        MindRaider.spidersGraph.newModel(notebookModelFilename);
        RdfModel rdfModel = MindRaider.spidersGraph.getRdfModel();
        rdfModel.setFilename(notebookModelFilename);
        rdfModel.setType(RdfModel.FILE_MODEL_TYPE);
        com.hp.hpl.jena.rdf.model.Resource rdfResource = (com.hp.hpl.jena.rdf.model.Resource) rdfModel.newResource(uri, false);
        ResourceDescriptor resourceDescriptor = new ResourceDescriptor(label, uri);
        resourceDescriptor.setCreated(resource.getMetadata().getCreated());
        resourceDescriptor.setAnnotationCite(annotation);
        createNotebookRdfResource(resourceDescriptor, rdfModel.getModel(), rdfResource);
        rdfModel.save();
        MindRaider.profile.setActiveNotebook(new URI(uri));
        activeNotebookResource = new NotebookResource(resource);
        activeNotebookResource.rdfModel = rdfModel;
        if (renderUi) {
            MindRaider.spidersGraph.renderModel();
        }
        for (int i = 0; i < subscribers.size(); i++) {
            ((NotebookCustodianListener) subscribers.get(i)).notebookCreated(activeNotebookResource);
        }
        return uri;
    }

    /**
     * Check whether concept exists within active notebook.
     * @param uri
     * @return
     */
    public boolean conceptExists(String uri) {
        if (MindRaider.profile.getActiveNotebookUri() != null && activeNotebookResource != null && activeNotebookResource.rdfModel != null) {
            if (activeNotebookResource.rdfModel.getModel().containsResource(ResourceFactory.createResource(uri))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get notebook filename.
     * @param notebookDirectory
     * @return
     */
    public String getModelFilenameByDirectory(String notebookDirectory) {
        return notebookDirectory + FILENAME_RDF_MODEL;
    }

    /**
     * Get notebook RDF model filename.
     * @param notebookDirectory
     * @return
     */
    public String getResourceFilenameByDirectory(String notebookDirectory) {
        return notebookDirectory + FILENAME_XML_RESOURCE;
    }

    /**
     * Get notebook directory.
     * @param uri The uri.
     * @return Returns the notebook directory.
     */
    public String getNotebookDirectory(String uri) {
        String notebookDirectory = notebooksDirectory + File.separator + Utils.getNcNameFromUri(uri) + File.separator;
        return notebookDirectory;
    }

    /**
     * Return the resource file name.
     * @param uri The uri.
     * @return Returns the resource file name.
     */
    public String getResourceFilename(String uri) {
        return getResourceFilenameByDirectory(getNotebookDirectory(uri));
    }

    /**
     * Returns the model file name.
     * @param uri The uri
     * @return Returns the model file name.
     */
    public String getModelFilename(String uri) {
        return getModelFilenameByDirectory(getNotebookDirectory(uri));
    }

    /**
     * Get notebook resource. Load all the notebooks and verify whether URI fits.
     * @param uri notebook URI
     * @return <code>null</code> if notebook not found.
     */
    public Resource get(String uri) {
        String notebookPath = getResourceFilenameByDirectory(getNotebookDirectory(uri));
        if (notebookPath != null) {
            try {
                return new Resource(notebookPath);
            } catch (Exception e) {
                logger.debug("Unable to load notebook: " + uri, e);
            }
        }
        return null;
    }

    /**
     * Get descriptors of the concepts from the active notebook.
     * @return concept descriptors.
     */
    public ResourceDescriptor[] getConceptDescriptors() {
        if (activeNotebookResource != null) {
            String[] conceptUris = activeNotebookResource.getConceptUris();
            if (conceptUris != null && conceptUris.length > 0) {
                ArrayList result = new ArrayList();
                for (int i = 0; i < conceptUris.length; i++) {
                    try {
                        result.add(new ResourceDescriptor(activeNotebookResource.rdfModel.getModel().getResource(conceptUris[i]).getProperty(RDFS.label).getObject().toString(), conceptUris[i]));
                    } catch (Exception e) {
                        logger.debug("Error: ", e);
                    }
                }
                return (ResourceDescriptor[]) result.toArray(new ResourceDescriptor[result.size()]);
            }
        }
        return null;
    }

    /**
     * Get notebook path on the file system.
     * @param uri notebook URI
     * @return <code>null</code> if notebook not found.
     */
    public String fsGetPath(String uri) {
        if (uri != null) {
            File f = new File(notebooksDirectory);
            File[] s = f.listFiles();
            if (s != null) {
                Resource resource;
                for (int i = 0; i < s.length; i++) {
                    if (s[i].isDirectory()) {
                        String notebookPath = s[i].getAbsolutePath() + File.separator + FILENAME_XML_RESOURCE;
                        try {
                            resource = new Resource(notebookPath);
                        } catch (Exception e) {
                            logger.error("fsGetPath(String)", e);
                            continue;
                        }
                        if (uri.equals(resource.getMetadata().getUri().toASCIIString())) {
                            logger.debug("  Got resource path: " + notebookPath);
                            return notebookPath;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get URIs of all notebooks.
     * @return Returns the array of notebook uri.
     */
    public String[] fsGetNotebooksUris() {
        File f = new File(notebooksDirectory);
        File[] s = f.listFiles();
        ArrayList result = new ArrayList();
        if (s != null) {
            Resource resource;
            for (int i = 0; i < s.length; i++) {
                if (s[i].isDirectory()) {
                    try {
                        resource = new Resource(s[i].getAbsolutePath() + File.separator + FILENAME_XML_RESOURCE);
                    } catch (Exception e) {
                        logger.error("fsGetNotebooksUris()", e);
                        continue;
                    }
                    result.add(resource.getMetadata().getUri().toASCIIString());
                }
            }
        }
        return (String[]) (result.toArray(new String[result.size()]));
    }

    /**
     * Get notebook resource. Load all the notebooks and verify whether URI fits.
     * @param uri notebook URI
     * @return <code>null</code> if notebook not found.
     */
    public Resource fsGet(String uri) {
        if (uri != null) {
            File f = new File(notebooksDirectory);
            File[] s = f.listFiles();
            if (s != null) {
                Resource resource;
                for (int i = 0; i < s.length; i++) {
                    if (s[i].isDirectory()) {
                        try {
                            resource = new Resource(s[i].getAbsolutePath() + File.separator + FILENAME_XML_RESOURCE);
                        } catch (Exception e) {
                            logger.error("fsGet(String)", e);
                            continue;
                        }
                        if (uri.equals(resource.getMetadata().getUri().toASCIIString())) {
                            logger.debug("  Got resource for uri: " + uri);
                            return resource;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get Notebook URI by it's local name.
     * @param localName
     * @return
     * @throws Exception
     */
    public String getNotebookUriByLocalName(String localName) throws Exception {
        return MindRaiderVocabulary.getNotebookUri(localName);
    }

    /**
     * Rename notebook.
     * @param notebookUri
     * @param newLabel
     * @throws Exception
     */
    public void rename(String notebookUri, String newLabel) throws Exception {
        logger.debug("Renaming notebook '" + notebookUri + "' to '" + newLabel + "'");
        if (notebookUri != null && newLabel != null) {
            NotebookResource notebookResource = new NotebookResource(get(notebookUri));
            notebookResource.getLabelProperty().setLabelContent(newLabel);
            notebookResource.save();
            MindRaider.folderCustodian.renameNotebook(notebookUri, newLabel);
        }
    }

    /**
     * Load a notebook.
     * @param uri notebook URI.
     */
    public boolean loadNotebook(URI uri) {
        MindRaider.setModeMindRaider();
        if (uri == null || "".equals(uri)) {
            StatusBar.show("Unable to load Notebook - URI is null!");
            return false;
        }
        if (uri.equals(MindRaider.profile.getActiveNotebook()) && activeNotebookResource != null) {
            StatusBar.show("Notebook '" + uri.toString() + "' already loaded ;-)");
            return false;
        }
        StatusBar.show("Loading notebook '" + uri + "'...");
        String notebookResourceFilename = getResourceFilenameByDirectory(getNotebookDirectory(uri.toString()));
        Resource resource;
        try {
            resource = new Resource(notebookResourceFilename);
        } catch (Exception e) {
            logger.debug("Unable to load notebook " + uri, e);
            StatusBar.show("Error: Unable to load notebook " + uri + "! " + e.getMessage(), Color.RED);
            return false;
        }
        String notebookModelFilename = getModelFilenameByDirectory(getNotebookDirectory(uri.toString()));
        try {
            MindRaider.spidersGraph.load(notebookModelFilename);
        } catch (Exception e1) {
            logger.debug("Unable to load notebook model: " + e1.getMessage(), e1);
            MindRaider.profile.setActiveNotebook(null);
            MindRaider.profile.setActiveNotebookUri(null);
            activeNotebookResource = null;
            return false;
        }
        MindRaider.spidersGraph.selectNodeByUri(uri.toString());
        MindRaider.masterToolBar.setModelLocation(notebookModelFilename);
        MindRaider.profile.setActiveNotebook(uri);
        activeNotebookResource = new NotebookResource(resource);
        activeNotebookResource.rdfModel = MindRaider.spidersGraph.getRdfModel();
        if (resource != null && resource.getMetadata() != null && resource.getMetadata().getUri() != null) {
            MindRaider.history.add(resource.getMetadata().getUri().toString());
        } else {
            logger.error("Resource " + uri + "not loaded is null!");
            return false;
        }
        return true;
    }

    /**
     * Save notebook resource.
     * @param resource
     */
    public void save(Resource resource) throws Exception {
        if (resource != null) {
            resource.toXmlFile(getResourceFilename(resource.getMetadata().getUri().toString()));
        }
    }

    /**
     * Close active notebook.
     */
    public void close() {
        MindRaider.profile.setActiveNotebookUri(null);
        activeNotebookResource = null;
        MindRaider.spidersGraph.clear();
        MindRaider.spidersGraph.renderModel();
    }

    /**
     * Check whether notebook exists.
     * @param uri
     */
    public boolean exists(String uri) {
        return MindRaider.folderCustodian.exists(uri);
    }

    /**
     * Get directory of the active notebook.
     * @return active notebook directory
     */
    public String getActiveNotebookDirectory() {
        if (activeNotebookResource != null) {
            String uri = activeNotebookResource.resource.getMetadata().getUri().toASCIIString();
            return notebooksDirectory + File.separator + Utils.getNcNameFromUri(uri);
        }
        return null;
    }

    /**
     * Subscribe.
     * @param listener
     */
    public void subscribe(NotebookCustodianListener listener) {
        subscribers.add(listener);
    }

    /**
     * Get active notebook NCName.
     */
    public String getActiveNotebookNcName() {
        if (activeNotebookResource != null) {
            return Utils.getNcNameFromUri(activeNotebookResource.resource.getMetadata().getUri().toASCIIString());
        }
        return null;
    }

    /**
     * Get model associated with the notebook.
     * @param uri notebook URI.
     * @return model.
     * @throws Exception
     */
    public Model getModel(String uri) throws Exception {
        if (uri != null) {
            String notebookModelPath = getModelFilename(uri);
            RdfModel rdfModel = new RdfModel(notebookModelPath);
            if (rdfModel != null) {
                return rdfModel.getModel();
            }
        }
        return null;
    }

    /**
     * export to TWiki format
     */
    public static final int FORMAT_TWIKI = 1;

    /**
     * export to OPML format
     */
    public static final int FORMAT_OPML = 2;

    /**
     * export to OPML format and save resulting HTML file
     */
    public static final int FORMAT_OPML_HTML = 3;

    /**
     * export to TWiki format and save resulting HTML file
     */
    public static final int FORMAT_TWIKI_HTML = 5;

    /**
     * Import from source file and create new notebook.
     * 
     * @param importType
     * @param srcFileName
     */
    public void importNotebook(int importType, String srcFileName, ProgressDialogJFrame progressDialogJFrame) {
        logger.debug("=-> notebook import: " + srcFileName);
        if (srcFileName == null) {
            return;
        }
        try {
            switch(importType) {
                case FORMAT_TWIKI:
                    twikiImport(srcFileName, progressDialogJFrame);
                    break;
            }
        } catch (Exception e) {
            logger.debug("Unable to import: ", e);
            return;
        }
        MindRaider.mainJFrame.requestFocus();
    }

    /**
     * TWiki import.
     * @param srcFileName
     */
    private void twikiImport(String srcFileName, ProgressDialogJFrame progressDialogJFrame) throws Exception {
        logger.debug("=-> TWiki import: " + srcFileName);
        FileReader fileReader = new FileReader(srcFileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String folderUri = MindRaider.folderCustodian.FOLDER_TWIKI_IMPORT_URI;
        String notebookUri = null;
        MindRaider.folderCustodian.create("TWiki Import", MindRaider.folderCustodian.FOLDER_TWIKI_IMPORT_URI);
        String[] parentConceptUris = new String[50];
        String notebookLabel, line;
        String lastConceptName = null;
        StringBuffer annotation = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            if (Pattern.matches("^---[+]+ .*", line)) {
                if (Pattern.matches("^---[+]{1} .*", line)) {
                    notebookLabel = line.substring(5);
                    logger.debug("LABEL: " + notebookLabel);
                    notebookUri = MindRaiderVocabulary.getNotebookUri(Utils.toNcName(notebookLabel));
                    String createdUri;
                    while (MindRaiderConstants.EXISTS.equals(createdUri = create(notebookLabel, notebookUri, null, false))) {
                        notebookUri += "_";
                    }
                    notebookUri = createdUri;
                    MindRaider.folderCustodian.addNotebook(folderUri, notebookUri);
                    activeNotebookResource.resource.addProperty(new SourceTwikiFileProperty(srcFileName));
                    activeNotebookResource.save();
                    logger.debug("Notebook created: " + notebookUri);
                } else {
                    twikiImportProcessLine(progressDialogJFrame, notebookUri, parentConceptUris, lastConceptName, annotation);
                    lastConceptName = line;
                }
                logger.debug(" SECTION: " + line);
            } else {
                annotation.append(line);
                annotation.append("\n");
            }
        }
        twikiImportProcessLine(progressDialogJFrame, notebookUri, parentConceptUris, lastConceptName, annotation);
        fileReader.close();
        ExplorerJPanel.getInstance().refresh();
        NotebookOutlineJPanel.getInstance().refresh();
        MindRaider.spidersGraph.renderModel();
    }

    /**
     * Process line when importing from TWiki.
     * 
     * @param progressDialogJFrame
     * @param notebookUri
     * @param parentConceptUris
     * @param lastConceptName
     * @param annotation
     * @throws Exception
     */
    private void twikiImportProcessLine(ProgressDialogJFrame progressDialogJFrame, String notebookUri, String[] parentConceptUris, String lastConceptName, StringBuffer annotation) throws Exception {
        if (lastConceptName == null) {
            activeNotebookResource.setAnnotation(annotation.toString());
            activeNotebookResource.save();
            annotation.setLength(0);
        } else {
            logger.debug("ANNOTATION:\n" + annotation.toString());
            StatusBar.show("Creating concept '" + lastConceptName + "'...");
            int depth = lastConceptName.indexOf(' ');
            lastConceptName = lastConceptName.substring(depth + 1);
            depth -= 4;
            logger.debug("Depth is: " + depth);
            logger.debug("Label is: " + lastConceptName);
            parentConceptUris[depth] = MindRaider.conceptCustodian.create(activeNotebookResource, parentConceptUris[depth - 1], lastConceptName, MindRaiderVocabulary.getConceptUri(Utils.getNcNameFromUri(notebookUri), "tempConcept" + System.currentTimeMillis()), annotation.toString(), false, MindRaiderConstants.MR_OWL_CONTENT_TYPE_TWIKI);
            if (progressDialogJFrame != null) {
                progressDialogJFrame.setProgressMessage(lastConceptName);
            }
            annotation.setLength(0);
        }
    }

    /**
     * Export active notebook.
     * @param exportType
     * @param dstFileName
     */
    public void exportNotebook(int exportType, String dstFileName) {
        logger.debug("=-> notebook export: " + dstFileName);
        if (MindRaider.notebookCustodian.activeNotebookResource != null) {
            NotebookResourceExpanded notebookResourceExpanded;
            String resourcesFilePrefix = MindRaider.installationDirectory + File.separator + "java" + File.separator + "src" + File.separator + "resources" + File.separator;
            String xslFilePrefix = resourcesFilePrefix + "xsl" + File.separator;
            String cssFilePrefix = resourcesFilePrefix + "css" + File.separator;
            String jsFilePrefix = resourcesFilePrefix + "js" + File.separator;
            try {
                switch(exportType) {
                    case FORMAT_TWIKI:
                        notebookResourceExpanded = new NotebookResourceExpanded(MindRaider.notebookCustodian.activeNotebookResource, TWikifier.getInstance());
                        notebookResourceExpanded.save(dstFileName, xslFilePrefix + "export2TWiki.xsl");
                        break;
                    case FORMAT_TWIKI_HTML:
                        notebookResourceExpanded = new NotebookResourceExpanded(MindRaider.notebookCustodian.activeNotebookResource, TWikifier.getInstance());
                        String tmpOpml = dstFileName + ".tmp";
                        notebookResourceExpanded.save(tmpOpml, xslFilePrefix + "export2TWiki.xsl");
                        FileInputStream fileInputStream = new FileInputStream(new File(tmpOpml));
                        String htmlContent = "<html>" + " <head>" + "   <style type='text/css'>" + "     ul, ol {" + "         margin-top: 0px;" + "         margin-bottom: 3px;" + "         margin-left: 25px;" + "     }" + "     body {" + "         font-family: arial, helvetica, sans-serif; " + "         font-size: small;" + "     }" + "   </style>" + " </head>" + "<body>\n" + TwikiToHtml.translate(fileInputStream) + "\n</body>" + "</html>";
                        File twikiHtmlFile = new File(dstFileName);
                        FileWriter fileWriter = null;
                        try {
                            fileWriter = new FileWriter(twikiHtmlFile);
                            fileWriter.write(htmlContent);
                        } finally {
                            fileWriter.flush();
                            fileWriter.close();
                        }
                        break;
                    case FORMAT_OPML:
                        notebookResourceExpanded = new NotebookResourceExpanded(MindRaider.notebookCustodian.activeNotebookResource, Opmlizer.getInstance());
                        notebookResourceExpanded.save(dstFileName, xslFilePrefix + "export2Opml.xsl");
                        break;
                    case FORMAT_OPML_HTML:
                        notebookResourceExpanded = new NotebookResourceExpanded(MindRaider.notebookCustodian.activeNotebookResource, Opmlizer.getInstance());
                        tmpOpml = dstFileName + ".tmp";
                        notebookResourceExpanded.save(tmpOpml, xslFilePrefix + "export2OpmlInternal.xsl");
                        Xsl.xsl(tmpOpml, dstFileName, xslFilePrefix + "opml2Html.xsl");
                        File dstDir = new File(dstFileName);
                        String dstDirectory = dstDir.getParent();
                        String srcOpmlCss = cssFilePrefix + "opml.css";
                        String destOpmlCss = dstDirectory + File.separator + "opml.css";
                        FileUtils.copyFile(new File(srcOpmlCss), new File(destOpmlCss));
                        String srcOpmlJs = jsFilePrefix + "opml.js";
                        String destOpmlJs = dstDirectory + File.separator + "opml.js";
                        FileUtils.copyFile(new File(srcOpmlJs), new File(destOpmlJs));
                        break;
                }
            } catch (Exception e) {
                logger.error("Unable to export notebook!", e);
            }
        }
    }

    /**
     * Returns the active notebook label.
     * @return the notebook label string.
     */
    public String getActiveNotebookLabel() {
        if (activeNotebookResource != null) {
            return activeNotebookResource.getLabel();
        }
        return null;
    }

    /**
     * Returns the notebook creation timestamp.
     * @return the long timestamp.
     */
    public long getActiveNotebookCreationTimestamp() {
        if (activeNotebookResource != null) {
            return activeNotebookResource.resource.getMetadata().getCreated();
        }
        return 0;
    }

    /**
     * Returns the notebook annotation.
     * @return the annotation string.
     */
    public String getActiveNotebookAnnotation() {
        if (activeNotebookResource != null) {
            AnnotationProperty annotationProperty = activeNotebookResource.getAnnotationProperty();
            if (annotationProperty != null) {
                return annotationProperty.getAnnotation();
            }
        }
        return null;
    }

    /**
     * Returns the notebook childred count.
     * @return the number of children.
     */
    public int getActiveNotebookChildCount() {
        if (activeNotebookResource != null) {
            Seq seq = activeNotebookResource.rdfModel.getModel().getSeq(activeNotebookResource.resource.getMetadata().getUri().toString());
            return seq.size();
        }
        return 0;
    }

    /**
     * Return the resource descriptor for child the given indexed position.
     * @param i the index.
     * @return the resource descriptor.
     */
    public ResourceDescriptor getActiveNotebookChildAt(int i) {
        if (activeNotebookResource != null) {
            Seq seq = activeNotebookResource.rdfModel.getModel().getSeq(activeNotebookResource.resource.getMetadata().getUri().toString());
            return getFullResourceDescriptor(i, seq);
        }
        return null;
    }

    /**
     * Returns the resource descriptor for notebook concept child at index position.
     * @param i the index position.
     * @param conceptUri the concept string.
     * @return rhe resource descriptor.
     */
    public ResourceDescriptor getActiveNotebookConceptChildAt(int i, String conceptUri) {
        if (activeNotebookResource != null && conceptUri != null) {
            Seq seq = activeNotebookResource.rdfModel.getModel().getSeq(conceptUri);
            return getFullResourceDescriptor(i, seq);
        }
        return null;
    }

    /**
     * Returns the children count for active notebook concept.
     * @param conceptUri the concept uri string.
     * @return the number of children.
     */
    public int getActiveNotebookConceptChildCount(String conceptUri) {
        if (activeNotebookResource != null && conceptUri != null && conceptUri.length() > 0) {
            Seq seq = activeNotebookResource.rdfModel.getModel().getSeq(conceptUri);
            return seq.size();
        }
        return 0;
    }

    /**
     * Returns the full resource descriptor for the given concept uri.
     * 
     * @param conceptUri the concept uri string.
     * @return the resource descriptor.
     */
    public ResourceDescriptor getFullResourceDescriptor(String conceptUri) {
        if (activeNotebookResource != null && conceptUri != null && conceptUri.length() > 0) {
            com.hp.hpl.jena.rdf.model.Resource resource = activeNotebookResource.rdfModel.getModel().getResource(conceptUri);
            return getFullResourceDescriptor(resource);
        }
        return null;
    }

    /**
     * Get detailed resource descriptor.
     * @param i the index
     * @param seq the Seq
     */
    private ResourceDescriptor getFullResourceDescriptor(int i, Seq seq) {
        ResourceDescriptor result = null;
        if (i < seq.size()) {
            com.hp.hpl.jena.rdf.model.Resource resource = seq.getResource(i + 1);
            if (resource != null) {
                result = getFullResourceDescriptor(resource);
            }
        }
        return result;
    }

    /**
     * Get full resource descriptor.
     * @param seq the Seq
     * @param resource the resource
     * @return Returns the ResourceDescriptor
     */
    public ResourceDescriptor getFullResourceDescriptor(com.hp.hpl.jena.rdf.model.Resource resource) {
        ResourceDescriptor result;
        Statement statement;
        result = new ResourceDescriptor();
        result.setUri(resource.getURI());
        if ((statement = resource.getProperty(RDFS.label)) != null) {
            result.setLabel(statement.getObject().toString());
        } else {
            result.setLabel(Utils.getNcNameFromUri(result.getUri()));
        }
        if ((statement = resource.getProperty(RDFS.comment)) != null) {
            result.setAnnotationCite(statement.getObject().toString());
        } else {
            result.setAnnotationCite("");
        }
        if ((statement = resource.getProperty(DC.date)) != null) {
            if (statement.getObject().toString() != null) {
                result.setCreated(Long.valueOf(statement.getObject().toString()).longValue());
            }
        } else {
            result.setCreated(0);
        }
        return result;
    }

    /**
     * Create notebook RDF resource.
     * @param notebook the notebook
     * @param oldModel the old model
     * @param notebookRdf the notebook RDF
     */
    public static void createNotebookRdfResource(ResourceDescriptor notebook, Model oldModel, com.hp.hpl.jena.rdf.model.Resource notebookRdf) {
        notebookRdf.addProperty(RDF.type, RDF.Seq);
        notebookRdf.addProperty(RDF.type, oldModel.createResource(MindRaiderConstants.MR_OWL_CLASS_NOTEBOOK));
        notebookRdf.addProperty(RDFS.label, oldModel.createLiteral(notebook.getLabel()));
        notebookRdf.addProperty(DC.date, oldModel.createLiteral(notebook.getCreated()));
        notebookRdf.addProperty(RDFS.comment, oldModel.createLiteral(NotebookOutlineTreeInstance.getAnnotationCite(notebook.getAnnotationCite())));
    }

    /**
     * Getter for <code>activeNotebookResource</code>.
     * @return Returns the activeNotebookResource.
     */
    public NotebookResource getActiveNotebookResource() {
        return this.activeNotebookResource;
    }

    /**
     * Setter for <code>activeNotebookResource</code>.
     * @param activeNotebookResource The activeNotebookResource to set.
     */
    public void setActiveNotebookResource(NotebookResource activeNotebookResource) {
        this.activeNotebookResource = activeNotebookResource;
    }
}
