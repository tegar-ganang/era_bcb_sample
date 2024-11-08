package javacream.swing.document;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javacream.swing.DefaultFileKit;
import javacream.swing.FileExtensionFilter;
import javacream.swing.application.ApplicationFrame;
import javacream.swing.desktop.ScrollableDesktopPane;

/**
 * DocumentDesktop
 * 
 * @author Glenn Powell
 *
 */
public class DocumentDesktop extends ScrollableDesktopPane {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(DocumentDesktop.class.getName());

    private DocumentManager documentManager;

    private Vector<DocumentType> documentTypes = new Vector<DocumentType>();

    private String newDocumentBaseName = "Untitled";

    private int newDocumentCount = 0;

    private Vector<FileFilter> fileFilters = new Vector<FileFilter>();

    public DocumentDesktop(ApplicationFrame frame) {
        documentManager = new DocumentManager(this);
        frame.getExitAction().addExitListener(documentManager);
        updateGUI();
    }

    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    public void createFileMenu(JMenu menu) {
        createFileMenu(menu, null);
    }

    public void createFileMenu(JMenu menu, URI[] recentURIs) {
        createFileMenuItems(menu);
        if (recentURIs != null && recentURIs.length > 0) createFileRecentMenuItems(menu, recentURIs);
    }

    public void createFileMenuItems(JMenu menu) {
        if (menu.getMenuComponentCount() > 0) menu.addSeparator();
        menu.add(new JMenuItem(documentManager.getNewFileAction()));
        menu.add(new JMenuItem(documentManager.getOpenFileAction()));
        menu.add(new JMenuItem(documentManager.getSaveFileAction()));
        menu.add(new JMenuItem(documentManager.getSaveAsFileAction()));
        menu.add(new JMenuItem(documentManager.getCloseFileAction()));
    }

    public void createFileRecentMenuItems(JMenu menu, URI[] recentURIs) {
        if (menu.getMenuComponentCount() > 0) menu.addSeparator();
        for (int i = 0; i < recentURIs.length; ++i) {
            OpenDocumentAction action = new OpenDocumentAction(recentURIs[i].getPath(), recentURIs[i]);
            action.setFileChooserAction(documentManager.getOpenFileAction());
            menu.add(new JMenuItem(action));
        }
    }

    public String getNewDocumentBaseName() {
        return newDocumentBaseName;
    }

    public void setNewDocumentBaseName(String newDocumentName) {
        this.newDocumentBaseName = newDocumentName;
    }

    public String getNewDocumentName() {
        return newDocumentBaseName + (++newDocumentCount);
    }

    public Document findDocument(URI uri) {
        for (Iterator<Document> itr = getDocuments().iterator(); itr.hasNext(); ) {
            Document document = itr.next();
            if (uri.equals(document.getURI())) return document;
        }
        return null;
    }

    public void addDocument(Document document) {
        document.setDesktop(this);
        document.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        document.addInternalFrameListener(documentManager);
        document.pack();
        add(document);
        document.setVisible(true);
        updateGUI();
    }

    public boolean addNewDocument() {
        Document document = newDocument();
        if (document != null) {
            addDocument(document);
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create new Document.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public boolean addNewDocument(Class<? extends Document> documentClass) {
        Document document = newDocument(documentClass);
        if (document != null) {
            addDocument(document);
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create new Document.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    protected Document newDocument() {
        if (documentTypes.size() > 0) {
            Class<? extends Document> documentClass = null;
            if (documentTypes.size() == 1) {
                documentClass = documentTypes.get(0).getDocumentClass();
            } else {
                JComboBox content = new JComboBox(documentTypes);
                JOptionPane.showConfirmDialog(this, content, "New Document", JOptionPane.OK_CANCEL_OPTION);
                documentClass = documentTypes.get(content.getSelectedIndex()).getDocumentClass();
            }
            return newDocument(documentClass);
        }
        return null;
    }

    protected Document newDocument(Class<? extends Document> documentClass) {
        if (documentClass != null) {
            try {
                Constructor<? extends Document> cons = documentClass.getConstructor(String.class);
                return cons.newInstance(getNewDocumentName());
            } catch (NoSuchMethodException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            } catch (InstantiationException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            } catch (InvocationTargetException e) {
                logger.log(Level.SEVERE, "Error initializing Document", e);
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            }
        }
        return null;
    }

    public boolean addOpenDocument(URI uri) {
        Document document = findDocument(uri);
        if (document != null) {
            setSelectedFrame(document);
            return true;
        }
        document = openDocument(uri);
        if (document != null) {
            addDocument(document);
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Failed to open File.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    protected Document openDocument(URI uri) {
        Document document = findDocument(uri);
        if (document != null) {
            setSelectedFrame(document);
            return document;
        }
        Class<? extends Document> documentClass = findDocumentClass(uri);
        if (documentClass != null) {
            try {
                Constructor<? extends Document> cons = documentClass.getConstructor(URI.class);
                document = cons.newInstance(uri);
                if (document.open()) {
                    document.setTitle(uri.getPath());
                    return document;
                }
            } catch (NoSuchMethodException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            } catch (InstantiationException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            } catch (InvocationTargetException e) {
                logger.log(Level.SEVERE, "Error initializing Document", e);
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, "Invalid Document Class", e);
            }
        }
        return null;
    }

    public boolean saveSelectedDocument() {
        Document document = getSelectedDocument();
        if (document != null) {
            return saveDocument(document, true);
        }
        return false;
    }

    public boolean saveAsSelectedDocument(File file) {
        Document document = getSelectedDocument();
        if (document != null) {
            setDocumentFile(document, file);
            return saveDocument(document, false);
        }
        return false;
    }

    protected boolean saveDocument(Document document, boolean overwrite) {
        while (true) {
            boolean write = (document.getURI() != null);
            if (!overwrite && document.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "Overwrite file?", "File Already Exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) break;
                write = (result == JOptionPane.YES_OPTION);
            }
            if (write) {
                boolean success = document.save();
                if (success) updateGUI(); else JOptionPane.showMessageDialog(this, "Failed to save Document", "Error", JOptionPane.ERROR_MESSAGE);
                return success;
            } else {
                int result = DefaultFileKit.getFileChooser().showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) setDocumentFile(document, DefaultFileKit.getFileChooser().getSelectedFile());
                if (result == JFileChooser.CANCEL_OPTION) break;
            }
        }
        return false;
    }

    public boolean closeSelectedDocument() {
        Document document = getSelectedDocument();
        if (document != null) {
            return closeDocument(document);
        }
        return false;
    }

    protected boolean closeDocument(Document document) {
        if (document.isModified()) {
            setSelectedFrame(document);
            int result = JOptionPane.showConfirmDialog(this, "You have unsaved changes to this document.  Do you want to save now?", "Unsaved Changes!", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) return false;
            if (result == JOptionPane.YES_OPTION) {
                if (saveDocument(document, true)) updateGUI(); else return false;
            }
        }
        document.dispose();
        return true;
    }

    public void registerDocumentType(String name, String extension, Class<? extends Document> documentClass) {
        documentTypes.add(new DocumentType(name, extension, documentClass));
        fileFilters.add(new FileExtensionFilter(name, new String[] { extension }));
        documentManager.getOpenFileAction().setFileFilters(fileFilters);
        documentManager.getSaveAsFileAction().setFileFilters(fileFilters);
    }

    public void unregisterDocumentType(Class<? extends Document> documentClass) {
        DocumentType documentType = findDocumentType(documentClass);
        if (documentType != null) {
            documentTypes.remove(documentType);
            for (Iterator<FileFilter> itr = fileFilters.iterator(); itr.hasNext(); ) {
                FileFilter fileFilter = itr.next();
                if (fileFilter instanceof FileExtensionFilter) {
                    FileExtensionFilter fileExtensionFilter = (FileExtensionFilter) fileFilter;
                    if (fileExtensionFilter.getName().equals(documentType.getName())) {
                        itr.remove();
                        break;
                    }
                }
            }
            documentManager.getOpenFileAction().setFileFilters(fileFilters);
        }
    }

    public Class<? extends Document> findDocumentClass(URI uri) {
        String[] parts = uri.getPath().split("\\.");
        String extension = parts[parts.length - 1];
        for (Iterator<DocumentType> itr = documentTypes.iterator(); itr.hasNext(); ) {
            DocumentType documentType = itr.next();
            if (documentType.getExtension().equalsIgnoreCase(extension)) return documentType.getDocumentClass();
        }
        return null;
    }

    public DocumentType findDocumentType(Class<? extends Document> documentClass) {
        for (Iterator<DocumentType> itr = documentTypes.iterator(); itr.hasNext(); ) {
            DocumentType documentType = itr.next();
            if (documentType.getDocumentClass().equals(documentClass)) return documentType;
        }
        return null;
    }

    public void setDocumentFile(Document document, File file) {
        String extension = DefaultFileKit.getFileExtension(file);
        if (extension == null) {
            DocumentType type = findDocumentType(document.getClass());
            if (type != null) file = new File(file.getAbsolutePath() + "." + type.getExtension());
        }
        document.setFile(file);
        document.setTitle(file.getPath());
    }

    public int getDocumentCount() {
        return getAllFrames().length;
    }

    public Vector<Document> getDocuments() {
        JInternalFrame[] frames = getAllFrames();
        Vector<Document> documents = new Vector<Document>();
        for (int i = 0; i < frames.length; ++i) {
            if (frames[i] instanceof Document) documents.add((Document) frames[i]);
        }
        return documents;
    }

    public Document getSelectedDocument() {
        JInternalFrame frame = getSelectedFrame();
        if (frame instanceof Document) return (Document) frame;
        return null;
    }

    protected void updateGUI() {
        Document document = getSelectedDocument();
        documentManager.getSaveFileAction().setEnabled(document != null && document.exists() && document.isModified());
        if (document != null) {
            DocumentType documentType = findDocumentType(document.getClass());
            FileExtensionFilter saveFilter = new FileExtensionFilter(documentType.getName(), new String[] { documentType.getExtension() });
            documentManager.getSaveAsFileAction().setFileFilter(saveFilter);
        }
        documentManager.getSaveAsFileAction().setEnabled(document != null);
        documentManager.getCloseFileAction().setEnabled(document != null);
        if (document != null) document.updateGUI();
    }
}
