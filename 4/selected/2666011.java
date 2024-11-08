package com.emental.mindraider.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import com.emental.mindraider.Messages;
import com.emental.mindraider.MindRaiderConstants;
import com.emental.mindraider.core.MindRaider;
import com.emental.mindraider.core.notebook.NotebookCustodian;
import com.emental.mindraider.core.rdf.MindRaiderVocabulary;
import com.emental.mindraider.core.rdf.RdfModel;
import com.emental.mindraider.core.rest.ResourceDescriptor;
import com.emental.mindraider.core.rest.properties.ResourcePropertyGroup;
import com.emental.mindraider.core.rest.resource.ConceptResource;
import com.emental.mindraider.core.rest.resource.FolderResource;
import com.emental.mindraider.core.rest.resource.NotebookResource;
import com.emental.mindraider.core.search.SearchCommander;
import com.emental.mindraider.ui.dialogs.ProgressDialogJFrame;
import com.emental.mindraider.ui.outline.treetable.NotebookOutlineTreeInstance;
import com.emental.mindraider.ui.panels.ExplorerJPanel;
import com.emental.mindraider.utils.Utils;
import com.emental.mindraider.utils.Zipper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * This class is responsible for installation of the prepared notebooks, XSLs,
 * CSSs, etc.
 * 
 * @author Martin.Dvorak
 * @version $Revision: 1.15 $ ($Author: mindraider $)
 */
public class Installer {

    /**
     * The profile hostname.
     */
    private static String profileHostname = "";

    /**
     * The profile username.
     */
    private static String profileUsername = "";

    /**
     * Logger for this class.
     */
    private static final Logger logger = Logger.getLogger(Installer.class);

    /**
     * The XML document declaration constant.
     */
    private static final String XML_DOCDECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * The directory distribution skeleton constant.
     */
    private static final String DIR_DISTRIBUTION_SKELETON = "/dist";

    /**
     * The home directory of folders, notebooks, etc.
     */
    private String resourceDirectoryHome;

    /**
     * Constructor.
     * 
     * @param resourceDirectoryHome
     *            the resource directory home
     */
    public Installer(String resourceDirectoryHome) {
        this.resourceDirectoryHome = resourceDirectoryHome;
    }

    /**
     * Install everything :-) .
     * 
     * @param hostname
     *            the hostname
     * @param username
     *            the username
     */
    public void install(String hostname, String username) {
        profileHostname = hostname;
        profileUsername = username;
        try {
            logger.debug(Messages.getString("Installer.installingFrom", new String[] { MindRaider.installationDirectory, DIR_DISTRIBUTION_SKELETON }));
            logger.debug(Messages.getString("Installer.installingTo", resourceDirectoryHome));
            File folders = new File(resourceDirectoryHome + "/Folders");
            if (folders.exists()) {
                logger.debug(Messages.getString("Installer.repositoryAlreadyInitialized"));
                return;
            }
            String repositorySkeleton = MindRaider.installationDirectory + DIR_DISTRIBUTION_SKELETON;
            if (logger.isDebugEnabled()) {
                logger.debug(Messages.getString("Installer.installingRepository") + " <");
            }
            gatherDirectoryFiles(new File(repositorySkeleton), resourceDirectoryHome, repositorySkeleton.length());
            logger.debug(">");
        } catch (Exception e) {
            logger.error(Messages.getString("Installer.unableToInitializeRepository"), e);
        }
    }

    /**
     * Process only files under dir.
     * 
     * @param dir
     *            the File
     * @param destinationDirectory
     *            the destination directory
     * @param prefixLng
     *            the prefix length
     * @throws Exception
     *             a generic exception
     */
    public static void gatherDirectoryFiles(File dir, String destinationDirectory, int prefixLng) throws Exception {
        String fromFile = dir.getPath();
        if (dir.isDirectory()) {
            String toFile = destinationDirectory + (fromFile.substring(prefixLng));
            logger.debug("Dir: " + fromFile);
            logger.debug(" =-> " + toFile);
            if (logger.isDebugEnabled()) {
                logger.debug("gatherDirectoryFiles() - :");
            }
            Utils.createDirectory(toFile);
            String[] children = dir.list();
            for (String filename : children) {
                gatherDirectoryFiles(new File(dir, filename), destinationDirectory, prefixLng);
            }
        } else {
            String toFile = destinationDirectory + (fromFile.substring(prefixLng));
            logger.debug("File: " + fromFile);
            logger.debug(" =-> " + toFile);
            if (logger.isDebugEnabled()) {
                logger.debug("gatherDirectoryFiles() - .");
            }
            FileUtils.copyFile(new File(fromFile), new File(toFile));
            fixUris(toFile);
        }
    }

    /**
     * Fix URIs - remove dvorka & savant in order to replace it with user's
     * hostname.
     * 
     * @param filename
     *            the filename
     * @todo replace reading/closing file with commons-io functions
     */
    public static void fixUris(String filename) {
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(new File(filename)));
            String line;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line);
                stringBuffer.append("\n");
            }
        } catch (IOException e) {
            logger.debug(Messages.getString("Installer.unableToReadFile", filename), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    logger.debug(Messages.getString("Installer.unableToCloseReader"));
                }
            }
        }
        String old = stringBuffer.toString();
        if (old != null) {
            String replacement = "http://" + profileHostname + "/e-mentality/mindmap#" + profileUsername;
            old = old.replaceAll("http://dvorka/e-mentality/mindmap#dvorka", replacement);
            old = old.replaceAll("http://dvorka/", "http://" + profileHostname + "/");
            old = old.replaceAll("http://savant/", "http://" + profileHostname + "/");
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename));
            out.write(old);
        } catch (Exception e) {
            logger.debug(Messages.getString("Installer.unableToWriteFixedFile", filename), e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e1) {
                    logger.debug(Messages.getString("Installer.unableToCloseFile", filename), e1);
                }
            }
        }
    }

    /**
     * Called in case of existing repository in order to perform update. There
     * are two phases - current version detection and upgrade itself.
     */
    public static void upgrade() {
        profileHostname = MindRaider.profile.getHostname();
        profileUsername = MindRaider.user.getName();
        String upgradeInfo = Messages.getString("Installer.upgradeInfo", new String[] { profileUsername, profileHostname, MindRaider.getVersion(), MindRaider.profile.getVersion() });
        logger.debug(upgradeInfo);
        boolean doUpgrade = false;
        int profileMajor = 1024;
        int profileMinor = 1024;
        if (MindRaider.profile.getVersion() != null && MindRaider.profile.getVersion().length() >= 3) {
            int dotIdx = MindRaider.profile.getVersion().indexOf('.');
            profileMajor = NumberUtils.createInteger(MindRaider.profile.getVersion().substring(0, dotIdx));
            profileMinor = NumberUtils.createInteger(MindRaider.profile.getVersion().substring(dotIdx + 1));
            if ((profileMajor <= MindRaiderConstants.majorVersion) && (profileMinor < MindRaiderConstants.minorVersion)) {
                doUpgrade = true;
            }
        } else {
            doUpgrade = true;
        }
        if (!doUpgrade) {
            return;
        }
        logger.debug(Messages.getString("Installer.goingToUpgrade"));
        if (profileMajor == 0) {
            if (profileMinor < 506) {
                upgradeTo0506();
            }
            if (profileMinor < 507) {
                upgradeTo0507();
            }
            if (profileMinor < 511) {
                upgradeTo0511();
            }
            if (profileMinor < 512) {
                upgradeTo0512();
            }
            if (profileMinor < 601) {
                upgradeTo0601();
            }
        }
    }

    /**
     * upgrade to 0.506 re-save all: 1. notebook models; 2. notebook resources;
     * 3. concept resources
     */
    private static void upgradeTo0506() {
        logger.debug(Messages.getString("Installer.upgradingTo", "0.506"));
        String title = Messages.getString("Installer.notebookConceptUpgradeTo", MindRaider.getVersion());
        String action = "<html>&nbsp;&nbsp;<b>" + Messages.getString("Installer.upgradingResources") + ":</b>&nbsp;&nbsp;</html>";
        ProgressDialogJFrame progress = new ProgressDialogJFrame(title, action);
        try {
            ResourceDescriptor[] folders = MindRaider.folderCustodian.getFolderDescriptors();
            if (folders != null) {
                for (int i = 0; i < folders.length; i++) {
                    String uri = folders[i].getUri();
                    progress.setProgressMessage(uri);
                    try {
                        new FolderResource(MindRaider.folderCustodian.get(uri)).save();
                    } catch (Exception e2) {
                        logger.debug(Messages.getString("Installer.unableToResaveFolder", uri), e2);
                    }
                    ResourceDescriptor[] notebooks = MindRaider.folderCustodian.getNotebookDescriptors(uri);
                    if (notebooks != null) {
                        for (int j = 0; j < notebooks.length; j++) {
                            String notebookUri = notebooks[j].getUri();
                            progress.setProgressMessage(notebookUri);
                            String notebookModelFilename = MindRaider.notebookCustodian.getModelFilenameByDirectory(MindRaider.notebookCustodian.getNotebookDirectory(notebookUri));
                            Model oldModel = RdfModel.loadModel(notebookModelFilename);
                            Resource notebookRdf = oldModel.getResource(notebookUri);
                            notebookRdf.removeAll(RDF.type);
                            NotebookCustodian.createNotebookRdfResource(notebooks[j], oldModel, notebookRdf);
                            com.emental.mindraider.core.rest.Resource notebookR = MindRaider.notebookCustodian.get(notebookUri);
                            NotebookResource notebookResource = new NotebookResource(notebookR);
                            try {
                                notebookResource.save();
                            } catch (Exception e1) {
                                logger.error("Unable to save notebook!", e1);
                            }
                            String[] conceptUris = notebookResource.getConceptUris();
                            if (!ArrayUtils.isEmpty(conceptUris)) {
                                for (String conceptUri : conceptUris) {
                                    try {
                                        Resource conceptRdf = oldModel.getResource(conceptUri);
                                        ConceptResource conceptResource = MindRaider.conceptCustodian.get(notebookUri, conceptUri);
                                        if (!conceptResource.attachmentsExist()) {
                                            conceptResource.resource.getData().addPropertyGroup(new ResourcePropertyGroup(ConceptResource.PROPERTY_GROUP_LABEL_ATTACHMENTS, new URI(ConceptResource.PROPERTY_GROUP_URI_ATTACHMENTS)));
                                            StmtIterator a = oldModel.listStatements(conceptRdf, MindRaiderVocabulary.attachment, (RDFNode) null);
                                            while (a.hasNext()) {
                                                String url = a.nextStatement().getObject().toString();
                                                logger.debug(Messages.getString("Installer.attachmentUrl", url));
                                                conceptResource.addAttachment(null, url);
                                            }
                                        }
                                        conceptResource.save();
                                        conceptRdf.removeAll(RDF.type);
                                        conceptRdf.addProperty(RDF.type, RDF.Seq);
                                        conceptRdf.addProperty(RDF.type, oldModel.createResource(MindRaiderConstants.MR_OWL_CLASS_CONCEPT));
                                        conceptRdf.addProperty(RDFS.label, oldModel.createLiteral(conceptResource.getLabel()));
                                        conceptRdf.addProperty(DC.date, oldModel.createLiteral(conceptResource.resource.getMetadata().getCreated()));
                                        conceptRdf.addProperty(RDFS.comment, oldModel.createLiteral(NotebookOutlineTreeInstance.getAnnotationCite(conceptResource.getAnnotation())));
                                        conceptRdf.addProperty(MindRaiderVocabulary.xlinkHref, MindRaider.profile.getRelativePath(MindRaider.conceptCustodian.getConceptResourceFilename(notebookUri, conceptUri)));
                                    } catch (Exception e) {
                                        logger.error(Messages.getString("Installer.unableToUpgradeConcept"), e);
                                    }
                                }
                            }
                            RdfModel.saveModel(oldModel, notebookModelFilename);
                        }
                    }
                }
            }
            MindRaider.profile.setVersion(MindRaider.getVersion());
            MindRaider.profile.save();
        } finally {
            progress.dispose();
        }
    }

    /**
     * upgrade to 0.507 version of MR.
     */
    private static void upgradeTo0507() {
        logger.debug(Messages.getString("Installer.upgradingTo", "0.507"));
        Resource resource = MindRaider.profile.getModel().getResource(MindRaiderConstants.MR_RDF_NS + MindRaider.profile.getProfileName());
        resource.removeAll(RDF.type);
        resource.addProperty(RDF.type, MindRaider.profile.getModel().createResource(MindRaiderConstants.MR_OWL_CLASS_PROFILE));
        MindRaider.profile.setVersion(MindRaider.getVersion());
        MindRaider.profile.save();
    }

    /**
     * upgrade to 0.511 version of MR.
     */
    private static void upgradeTo0511() {
        logger.debug(Messages.getString("Installer.upgradingTo", "0.511"));
        String title = Messages.getString("Installer.upgradeTo") + MindRaider.getVersion() + ":" + Messages.getString("Installer.repositoryBackup");
        String action = "<html>&nbsp;&nbsp;<b>" + Messages.getString("Installer.zipping") + ":</b>&nbsp;&nbsp;</html>";
        ProgressDialogJFrame progress = new ProgressDialogJFrame(title, action);
        backupRepository(progress);
        if (logger.isDebugEnabled()) {
            logger.debug("upgradeTo0511() -      Upgrading documentation... <");
        }
        try {
            String mrFolderUri = MindRaiderVocabulary.getFolderUri(NotebookCustodian.MR_DOC_FOLDER_LOCAL_NAME);
            if (!MindRaider.folderCustodian.exists(mrFolderUri)) {
                logger.debug(Messages.getString("Installer.creatingMindRaiderFolder"));
                MindRaider.folderCustodian.create("MR", mrFolderUri);
            }
            String introNotebookUri = MindRaiderVocabulary.getNotebookUri(NotebookCustodian.MR_DOC_NOTEBOOK_INTRODUCTION_LOCAL_NAME);
            if (!MindRaider.notebookCustodian.exists(introNotebookUri)) {
                logger.debug(Messages.getString("Installer.creatingIntroductionNotebook"));
                MindRaider.notebookCustodian.create("Introduction", introNotebookUri, "MR Introduction", false);
                MindRaider.folderCustodian.addNotebook(mrFolderUri, introNotebookUri);
            }
            String docNotebookUri = MindRaiderVocabulary.getNotebookUri(NotebookCustodian.MR_DOC_NOTEBOOK_DOCUMENTATION_LOCAL_NAME);
            if (!MindRaider.notebookCustodian.exists(docNotebookUri)) {
                logger.debug(Messages.getString("Installer.creatingDocumentationNotebook"));
                MindRaider.notebookCustodian.create("Documentation", docNotebookUri, "MR Documentation", false);
                MindRaider.folderCustodian.addNotebook(mrFolderUri, docNotebookUri);
            }
            String developersNotebookUri = MindRaiderVocabulary.getNotebookUri(NotebookCustodian.MR_DOC_NOTEBOOK_FOR_DEVELOPERS_LOCAL_NAME);
            if (!MindRaider.notebookCustodian.exists(developersNotebookUri)) {
                logger.debug(Messages.getString("Installer.creatingForDevelopersNotebook"));
                MindRaider.notebookCustodian.create("For Developers", developersNotebookUri, "For Developers", false);
                MindRaider.folderCustodian.addNotebook(mrFolderUri, developersNotebookUri);
            }
            upgradeDocumentationNotebook(NotebookCustodian.MR_DOC_NOTEBOOK_INTRODUCTION_LOCAL_NAME);
            upgradeDocumentationNotebook(NotebookCustodian.MR_DOC_NOTEBOOK_DOCUMENTATION_LOCAL_NAME);
            upgradeDocumentationNotebook(NotebookCustodian.MR_DOC_NOTEBOOK_FOR_DEVELOPERS_LOCAL_NAME);
            ExplorerJPanel.getInstance().refresh();
        } catch (Exception e) {
            logger.debug("upgradeTo0511(): unable to upgrade documentation!");
        }
        logger.debug(">\n" + Messages.getString("Installer.documentationUpgraded"));
        try {
            String categoriesOntologySuffix = File.separator + MindRaiderConstants.MR_DIR_CATEGORIES_DIR + File.separator + "notebook.rdf.xml";
            String target = MindRaider.profile.getHomeDirectory() + categoriesOntologySuffix;
            File file = new File(target);
            file.getParentFile().mkdirs();
            FileUtils.copyFile(new File(MindRaider.installationDirectory + DIR_DISTRIBUTION_SKELETON + categoriesOntologySuffix), new File(target));
        } catch (Exception e2) {
            logger.error(Messages.getString("Installer.unableToCopyNotebooksCategoriesOntology"), e2);
            logger.error("upgradeTo0511()", e2);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.getString("Installer.internationalization") + " <");
        }
        title = Messages.getString("Installer.internationalizationUpgradeTo", MindRaider.getVersion());
        action = "<html>&nbsp;&nbsp;<b>XML file:</b>&nbsp;&nbsp;</html>";
        progress = new ProgressDialogJFrame(title, action);
        try {
            internationalizationUpgradeTo511(new File(MindRaider.profile.getHomeDirectory()), progress);
        } catch (Exception e) {
            logger.debug(Messages.getString("Installer.unableToInternationalize"), e);
        } finally {
            progress.dispose();
            logger.debug(">\n" + Messages.getString("Installer.internationalizationUpgradeFinished"));
        }
        SearchCommander.rebuildSearchAndTagIndices();
        MindRaider.profile.setVersion(MindRaider.getVersion());
        MindRaider.profile.save();
    }

    /**
     * upgrade to 0.512. there are no changes in the model - just rebuild search
     * index and upgrade content type properties are set automatically, backup
     * repository and update version in the profile.
     */
    private static void upgradeTo0512() {
        SearchCommander.rebuildSearchAndTagIndices();
        Installer.backupRepositoryAsync();
        MindRaider.profile.version = MindRaider.getVersion();
        MindRaider.profile.save();
    }

    /**
     * upgrade to 0.601 - the tag release: tag index is build.
     */
    private static void upgradeTo0601() {
        SearchCommander.rebuildSearchAndTagIndices();
        Installer.backupRepositoryAsync();
        MindRaider.profile.version = MindRaider.getVersion();
        MindRaider.profile.save();
    }

    /**
     * Asynchronous repository backup ensuring dialog refreshing.
     */
    public static void backupRepositoryAsync() {
        final ProgressDialogJFrame progress = new ProgressDialogJFrame(Messages.getString("Installer.repositoryBackup"), "<html>&nbsp;&nbsp;<b>" + Messages.getString("Installer.zipping") + "</b>&nbsp;&nbsp;</html>");
        new Thread() {

            public void run() {
                String targetFile;
                if ((targetFile = backupRepository(progress)) == null) {
                    JOptionPane.showMessageDialog(MindRaider.mainJFrame, Messages.getString("Installer.UnableToBackupRepository"), Messages.getString("Installer.backupError"), JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MindRaider.mainJFrame, Messages.getString("Installer.repositoryBackupStoredTo", targetFile), Messages.getString("Installer.backupResult"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.start();
    }

    /**
     * Backup repository.
     * 
     * @param progress
     *            the progress dialog JFrame
     * @return location of the directory where was the directory backup.
     */
    public static String backupRepository(ProgressDialogJFrame progress) {
        logger.debug(Messages.getString("Installer.makingBackupRepository", MindRaider.profile.getHomeDirectory()));
        try {
            String zipFileName = new File(MindRaider.profile.getHomeDirectory()).getParent() + File.separator + "MindRaiderRepositoryBackup-" + MindRaiderConstants.majorVersion + "." + MindRaiderConstants.minorVersion + "-" + System.currentTimeMillis() + ".zip";
            Zipper.zip(zipFileName, MindRaider.profile.getHomeDirectory(), progress);
            logger.debug(Messages.getString("Installer.backupCreated", zipFileName));
            return zipFileName;
        } catch (Exception e1) {
            logger.error(Messages.getString("Installer.unableToBackupRepositoryDirectory"), e1);
        } finally {
            progress.dispose();
        }
        return null;
    }

    /**
     * Upgrade documentation notebook.
     * 
     * @param notebookLocalName
     *            the notebook local name
     * @throws Exception
     *             a generic exception
     */
    private static void upgradeDocumentationNotebook(String notebookLocalName) throws Exception {
        String relativePath = File.separator + MindRaiderConstants.MR_DIR_NOTEBOOKS_DIR + File.separator + notebookLocalName;
        File file = new File(MindRaider.profile.getHomeDirectory() + relativePath);
        logger.debug(Messages.getString("Installer.checkingNotebookExistence", file.getAbsolutePath()));
        if (file.exists()) {
            logger.debug(Messages.getString("Installer.renewing", file.getAbsolutePath()));
            Utils.deleteSubtree(file);
            file.mkdirs();
            String sourceSkeleton = MindRaider.installationDirectory + DIR_DISTRIBUTION_SKELETON + relativePath;
            File sourceSkeletonFile = new File(sourceSkeleton);
            gatherDirectoryFiles(sourceSkeletonFile, file.getAbsolutePath(), sourceSkeleton.length());
        }
    }

    /**
     * Process only files under dir.
     * 
     * @param dir
     *            the file
     * @param progress
     *            the progress dialog JFrame
     * @throws Exception
     *             a generic exception
     */
    public static void internationalizationUpgradeTo511(File dir, ProgressDialogJFrame progress) throws Exception {
        String fromFile = dir.getPath();
        if (dir.isDirectory()) {
            logger.debug("Dir: " + fromFile);
            progress.setProgressMessage(fromFile);
            if (logger.isDebugEnabled()) {
                logger.debug("internationalizationUpgradeTo511()");
            }
            String[] children = dir.list();
            for (String child : children) {
                internationalizationUpgradeTo511(new File(dir, child), progress);
            }
        } else {
            logger.debug("File: " + fromFile);
            progress.setProgressMessage(fromFile);
            if (fromFile.endsWith(".xml")) {
                internationalizationUpgradeTo511(fromFile);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("internationalizationUpgradeTo511()");
            }
        }
    }

    /**
     * Change XML Decl to <?xml version="1.0" encoding="UTF-8"?>.
     * 
     * @param filename
     *            the filename
     */
    public static void internationalizationUpgradeTo511(String filename) {
        String content = null;
        try {
            content = FileUtils.readFileToString(new File(filename), "UTF-8");
        } catch (IOException e) {
            logger.debug(Messages.getString("Installer.unableToReadFile", filename), e);
        }
        String s;
        if (content.indexOf("<?xml") == 0) {
            s = content.substring(content.indexOf("\n") + 1);
            s = XML_DOCDECL + "\n" + s;
        } else {
            s = (XML_DOCDECL + "\n").toString();
        }
        try {
            s = new String(s.getBytes(), "UTF-8");
        } catch (Exception e) {
            logger.debug(Messages.getString("Installer.unableToReencode"), e);
            return;
        }
        try {
            FileUtils.writeStringToFile(new File(filename), s, "UTF-8");
        } catch (Exception e) {
            logger.debug(Messages.getString("Installer.unableToWriteFixedFile", filename), e);
        }
    }
}
