package org.gcreator.pineapple.project.standard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.gcreator.pineapple.core.PineappleCore;
import org.gcreator.pineapple.managers.EventManager;
import org.gcreator.pineapple.project.Project;
import org.gcreator.pineapple.project.ProjectElement;
import org.gcreator.pineapple.project.ProjectFolder;
import org.gcreator.pineapple.project.io.BasicFile;
import org.gcreator.pineapple.project.io.ProjectManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Defualt implementation of {@link ProjectManager}
 * for {@link DefaultProject} that uses the file system
 * for storing files.
 * 
 * @author Serge Humphrey
 */
public class DefaultProjectManager implements ProjectManager {

    protected DefaultProject project;

    public static final float PROJECT_VERSION = 1.08F;

    /**
     * Creates a new manger, with a given project.
     * 
     * @param p The {@link Project} that manager belongs for.
     * This may not be <tt>null</tt>.
     * 
     * @throws NullPointerException If the given project is <tt>null</tt>.
     */
    public DefaultProjectManager(DefaultProject p) throws NullPointerException {
        if (p == null) {
            throw new NullPointerException("Project may not be null.");
        }
        this.project = p;
    }

    /**
     * Creates a new manger, and loads the project from a file.
     * 
     * @param f The {@link java.io.File} to load.
     * @param folder The folder for the project.
     * @param type The project type for the project.
     * @param initial The {@link DefaultProject} to load the file to.
     * If this is <tt>null</tt>, a new {@link DefaultProject} will be created.
     * @throws NullPointerException If the given project is <tt>null</tt>.
     */
    public DefaultProjectManager(File f, File folder, DefaultProjectType type, DefaultProject initial) throws NullPointerException {
        if (f == null) {
            throw new NullPointerException("File may not be null.");
        }
        this.project = load(f, folder, type, initial);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFile createFile(ProjectFolder folder, String name, String type) {
        File f = new File(((folder == null) ? project.getProjectDataFolder() : ((DefaultFile) folder.getFile()).file), name + "." + type);
        try {
            f.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        DefaultFile ff = new DefaultFile(f, folder, project);
        if (folder == null) {
            ProjectElement e = project.createElement(ff);
            project.getFiles().reload();
        } else {
            folder.reload();
        }
        updateTreeUI();
        return ff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFile createFolder(ProjectFolder folder, String name) {
        File f = new File(((folder == null) ? project.getProjectDataFolder() : ((DefaultFile) folder.getFile()).file), name);
        f.mkdir();
        DefaultFile ff = new DefaultFile(f, folder, project);
        if (folder == null) {
            project.createElement(ff);
            project.getFiles().reload();
        } else {
            folder.reload();
        }
        updateTreeUI();
        return ff;
    }

    /**
     * Creates a new {@link Project} from a {@link java.io.File}.
     * 
     * @param f The {@link java.io.File} to be loaded.
     * @param folder The folder dor the project.
     * @param t The project type for te project.
     * @param initial The {@link DefaultProject} to load the file to.
     * If this is <tt>null</tt>, a new {@link DefaultProject} will be created.
     * @return A new {@link Project} created from the given {@link java.io.File}.
     * 
     * @see #getProjectFileTypes() 
     * @see #allowsProject(java.io.File) 
     */
    public DefaultProject load(File f, File folder, DefaultProjectType t, DefaultProject initial) {
        if (initial == null) {
            initial = new DefaultProject(null, folder, t, this, false);
        }
        String format;
        int i = f.getName().lastIndexOf('.');
        if (i == -1 || i == f.getName().length()) {
            format = null;
        } else {
            format = f.getName().substring(i + 1);
        }
        if (format == null) {
            System.err.println("Error: File " + f + " has null format");
            return null;
        }
        if (format.equals(t.FILE_TYPE)) {
            loadFromManifest(f, initial);
        }
        return initial;
    }

    /**
     * Saves the project to a manifest.
     * 
     */
    protected synchronized void saveToManifest() {
        if (project == null || project.getProjectFolder() == null || project.managing) {
            return;
        }
        try {
            project.managing = true;
            File f = new File(project.getProjectFolder().getPath() + File.separator + "project." + project.getProjectType().getProjectFileTypes()[0]);
            if (!f.exists()) {
                f.createNewFile();
            }
            DocumentBuilder builder = createDocumentBuilder();
            if (builder == null) {
                System.err.println("Error: can't save projct XML to null builder.");
                return;
            }
            Document doc = builder.newDocument();
            doc.setXmlStandalone(true);
            doc.setXmlVersion("1.0");
            Element root = doc.createElement("pineapple-project");
            root.setAttribute("version", Float.toString(PROJECT_VERSION));
            root.setAttribute("name", project.getName());
            Element settings = doc.createElement("settings");
            for (String s : project.settings.keySet()) {
                Element setting = doc.createElement("setting");
                setting.setAttribute("key", s);
                setting.setAttribute("value", project.settings.get(s));
                settings.appendChild(setting);
            }
            root.appendChild(settings);
            doc.appendChild(root);
            Source source = new DOMSource(doc);
            Result result = null;
            Transformer xformer;
            result = new StreamResult(new FileOutputStream(f));
            xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            xformer.transform(source, result);
        } catch (IOException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            project.managing = false;
        }
    }

    protected static DocumentBuilder createDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Loads a project from a manifest.
     * 
     * @param f The manifest file to load.
     * @param project The project to apply the manifest to.
     */
    protected synchronized void loadFromManifest(File f, DefaultProject project) {
        if (project.managing) {
            return;
        }
        try {
            project.managing = true;
            synchronized (this) {
                new ProjectXMLHandler(f, project);
            }
            this.project = project;
        } catch (SAXException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            project.managing = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Project getProject() {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFile copyFileToProject(File file, ProjectFolder folder, String newName) {
        File newFile = copyFile(file, newName, ((folder != null) ? ((DefaultFile) folder.getFile()).file : project.getProjectDataFolder()));
        BasicFile f = new DefaultFile(newFile, folder, project);
        if (folder != null) {
            folder.reload();
        } else {
            project.createElement(f);
            project.getFiles().reload();
        }
        updateTreeUI();
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFile copyFile(BasicFile file, ProjectFolder folder, String newName) {
        File newFile = copyFile(((DefaultFile) file).file, newName, ((folder != null) ? ((DefaultFile) folder.getFile()).file : project.getProjectFolder()));
        BasicFile f = new DefaultFile(newFile, folder, project);
        if (folder != null) {
            folder.reload();
        } else {
            project.createElement(f);
            project.getFiles().reload();
        }
        updateTreeUI();
        return f;
    }

    private File copyFile(File file, String newName, File folder) {
        File newFile = null;
        if (!file.exists()) {
            System.out.println("File " + file + " does not exist");
            return null;
        }
        if (file.isFile()) {
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                newFile = new File(folder, newName);
                if (!newFile.exists()) {
                    newFile.createNewFile();
                }
                out = new BufferedOutputStream(new FileOutputStream(newFile));
                int read;
                byte[] buffer = new byte[8192];
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                updateTreeUI();
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (file.isDirectory()) {
            newFile = new File(folder, newName);
            if (!newFile.exists()) {
                newFile.mkdir();
            }
            for (File f : file.listFiles()) {
                copyFile(f, f.getName(), newFile);
            }
        }
        return newFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFile getFile(String path) throws FileNotFoundException {
        return getFile(path, project);
    }

    private BasicFile getFile(String path, Project p) throws FileNotFoundException {
        DefaultFile f = new DefaultFile(new File(p.getProjectDataFolder(), path), null, p);
        f.element = p.createElement(f);
        return f;
    }

    private final class ProjectXMLHandler implements ContentHandler {

        private Locator locator;

        private boolean files, settings;

        private boolean loading;

        private DefaultProject project;

        public ProjectXMLHandler(File f, DefaultProject p) throws SAXException, FileNotFoundException, IOException {
            this.project = p;
            XMLReader r = XMLReaderFactory.createXMLReader();
            r.setContentHandler(this);
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            r.parse(new InputSource(in));
            in.close();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startDocument() throws SAXException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endDocument() throws SAXException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            System.out.println("Nobody cares about Namespace '" + prefix + "', Namespace URI " + uri);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            System.out.println("A request was made to end Namespace '" + prefix + "', but no one cares");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            Thread.yield();
            if (localName.equalsIgnoreCase("pineapple-project")) {
                String version = atts.getValue("version");
                if (version == null) {
                    System.err.println("FATAL ERROR: No project version.");
                    loading = false;
                } else if (version.equals(Float.toString(PROJECT_VERSION))) {
                    loading = true;
                } else {
                    System.err.println("WARNING: Wrong project version: " + version + " :: required: " + PROJECT_VERSION + " (oh well)");
                    loading = true;
                }
                String name = atts.getValue("name");
                if (name == null) {
                    System.out.println("WARNING: No 'name' attribute to project. Using default.");
                    project.setName("Project");
                } else {
                    project.setName(name);
                }
                return;
            } else if (localName.equalsIgnoreCase("files")) {
                files = true;
                return;
            } else if (localName.equalsIgnoreCase("settings")) {
                settings = true;
                return;
            }
            if (!loading) {
                System.err.println("WARINING: invalid request to load element '" + qName + "' when pineapple-project element has not yet been parsed.");
                return;
            }
            if (files) {
                String path = atts.getValue("path");
                if (path == null) {
                    System.err.println("ERROR: No path attribute for file.");
                } else {
                    try {
                        getFile(path, this.project);
                        project.getFiles().reload();
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DefaultProjectManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    updateTreeUI();
                }
            } else if (settings) {
                String key = atts.getValue("key");
                String value = atts.getValue("value");
                if (key == null) {
                    System.err.println("ERROR: no 'key' attribute for setting.");
                    return;
                } else if (value == null) {
                    System.err.println("ERROR: no 'value' attribute for setting.");
                    return;
                }
                project.settings.put(key, value);
            } else {
                System.err.println("WARNING: unrecegnized element '" + qName + "'");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (!loading) {
                return;
            }
            if (localName.equalsIgnoreCase("pineapple-project")) {
                loading = false;
            } else if (localName.equalsIgnoreCase("files")) {
                files = false;
            } else if (localName.equalsIgnoreCase("settings")) {
                settings = false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            System.out.println("NOTE: Processing '" + target + "', data '" + data + "'");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void skippedEntity(String name) throws SAXException {
            System.out.println("NOTE: Skipped Entity '" + name + "'");
        }
    }

    private void updateTreeUI() {
        EventManager.fireEvent(this, PineappleCore.TREE_CHANGED);
    }
}
