package uk.ac.bolton.archimate.editor.model.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import uk.ac.bolton.archimate.compatibility.CompatibilityHandlerException;
import uk.ac.bolton.archimate.compatibility.IncompatibleModelException;
import uk.ac.bolton.archimate.compatibility.LaterModelVersionException;
import uk.ac.bolton.archimate.compatibility.ModelCompatibility;
import uk.ac.bolton.archimate.editor.ArchimateEditorPlugin;
import uk.ac.bolton.archimate.editor.Logger;
import uk.ac.bolton.archimate.editor.diagram.util.AnimationUtil;
import uk.ac.bolton.archimate.editor.model.IArchiveManager;
import uk.ac.bolton.archimate.editor.model.IEditorModelManager;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.editor.ui.services.EditorManager;
import uk.ac.bolton.archimate.editor.utils.FileUtils;
import uk.ac.bolton.archimate.model.FolderType;
import uk.ac.bolton.archimate.model.IArchimateFactory;
import uk.ac.bolton.archimate.model.IArchimateModel;
import uk.ac.bolton.archimate.model.IDiagramModel;
import uk.ac.bolton.archimate.model.ModelVersion;
import uk.ac.bolton.archimate.model.util.ArchimateResourceFactory;
import uk.ac.bolton.jdom.JDOMUtils;

/**
 * Editor Model Manager.<p>
 * <p>
 * Acts as an adapter to the Archimate Models passing on notifications to listeners
 * so that clients only have to register here once rather than for each model.<p>
 * Also can pass on arbitrary PropertyChangeEvents to registered listeners.<br>
 * Also manages CommandStacks for models.<br>
 * Also handles persistence of models.
 * 
 * @author Phillip Beauvoir
 */
public class EditorModelManager implements IEditorModelManager {

    /**
     * Listener list
     */
    private PropertyChangeSupport fListeners = new PropertyChangeSupport(this);

    /**
     * Models Open
     */
    private List<IArchimateModel> fModels;

    /**
     * Backing File
     */
    private File backingFile = new File(ArchimateEditorPlugin.INSTANCE.getUserDataFolder(), "models.xml");

    /**
     * Listen to the App closing so we can ask to save
     */
    private IWorkbenchListener workBenchListener = new IWorkbenchListener() {

        public void postShutdown(IWorkbench workbench) {
        }

        public boolean preShutdown(IWorkbench workbench, boolean forced) {
            for (IArchimateModel model : getModels()) {
                if (isModelDirty(model)) {
                    try {
                        boolean result = askSaveModel(model);
                        if (!result) {
                            return false;
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return true;
        }
    };

    public EditorModelManager() {
        PlatformUI.getWorkbench().addWorkbenchListener(workBenchListener);
    }

    @Override
    public List<IArchimateModel> getModels() {
        if (fModels == null) {
            fModels = new ArrayList<IArchimateModel>();
            try {
                loadState();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return fModels;
    }

    @Override
    public IArchimateModel createNewModel() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        model.setName(Messages.EditorModelManager_0);
        model.setDefaults();
        IDiagramModel diagramModel = IArchimateFactory.eINSTANCE.createArchimateDiagramModel();
        diagramModel.setName(Messages.EditorModelManager_1);
        model.getFolder(FolderType.DIAGRAMS).getElements().add(diagramModel);
        getModels().add(model);
        createNewCommandStack(model);
        createNewArchiveManager(model);
        firePropertyChange(this, PROPERTY_MODEL_CREATED, null, model);
        model.eAdapters().add(new ECoreAdapter());
        return model;
    }

    @Override
    public IArchimateModel openModel(File file) {
        if (file == null || !file.exists() || isModelLoaded(file)) {
            return null;
        }
        IArchimateModel model = loadModel(file);
        if (model != null) {
            if (Preferences.doOpenDiagramsOnLoad()) {
                for (IDiagramModel dm : model.getDiagramModels()) {
                    EditorManager.openDiagramEditor(dm);
                }
            }
            firePropertyChange(this, PROPERTY_MODEL_OPENED, null, model);
        }
        return model;
    }

    @Override
    public void openModel(IArchimateModel model) {
        getModels().add(model);
        createNewCommandStack(model);
        createNewArchiveManager(model);
        model.eAdapters().add(new ECoreAdapter());
        firePropertyChange(this, PROPERTY_MODEL_OPENED, null, model);
    }

    @Override
    public IArchimateModel loadModel(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        boolean useArchiveFormat = IArchiveManager.FACTORY.isArchiveFile(file);
        ResourceSet resourceSet = ArchimateResourceFactory.createResourceSet();
        Resource resource = resourceSet.createResource(useArchiveFormat ? IArchiveManager.FACTORY.createArchiveModelURI(file) : URI.createFileURI(file.getAbsolutePath()));
        try {
            resource.load(null);
        } catch (IOException ex) {
            try {
                ModelCompatibility.checkErrors(resource);
            } catch (IncompatibleModelException ex1) {
                MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.EditorModelManager_2, NLS.bind(Messages.EditorModelManager_3, file) + "\n" + ex1.getMessage());
                return null;
            }
        }
        try {
            ModelCompatibility.checkVersion(resource);
        } catch (LaterModelVersionException ex) {
            boolean answer = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.EditorModelManager_4, NLS.bind(Messages.EditorModelManager_5, file, ex.getVersion()));
            if (!answer) {
                return null;
            }
        }
        try {
            ModelCompatibility.fixCompatibility(resource);
        } catch (CompatibilityHandlerException ex) {
        }
        IArchimateModel model = (IArchimateModel) resource.getContents().get(0);
        model.setFile(file);
        model.setDefaults();
        getModels().add(model);
        model.eAdapters().add(new ECoreAdapter());
        createNewCommandStack(model);
        createNewArchiveManager(model);
        markDiagramModelsAsSaved(model);
        firePropertyChange(this, PROPERTY_MODEL_LOADED, null, model);
        return model;
    }

    @Override
    public boolean closeModel(IArchimateModel model) throws IOException {
        if (isModelDirty(model)) {
            boolean result = askSaveModel(model);
            if (!result) {
                return false;
            }
        }
        EditorManager.closeDiagramEditors(model);
        getModels().remove(model);
        model.eAdapters().clear();
        firePropertyChange(this, PROPERTY_MODEL_REMOVED, null, model);
        deleteCommandStack(model);
        deleteArchiveManager(model);
        return true;
    }

    /**
     * Show dialog to save modified model
     * @param model
     * @return true if the user chose to save the model, false otherwise
     * @throws IOException 
     */
    private boolean askSaveModel(IArchimateModel model) throws IOException {
        MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(), Messages.EditorModelManager_6, null, NLS.bind(Messages.EditorModelManager_7, model.getName()), MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        int result = dialog.open();
        if (result == 0) {
            return saveModel(model);
        } else if (result == 2) {
            return false;
        }
        return true;
    }

    @Override
    public boolean saveModel(IArchimateModel model) throws IOException {
        if (model.getFile() == null) {
            File file = askSaveModel();
            if (file == null) {
                return false;
            }
            model.setFile(file);
        }
        File file = model.getFile();
        if (file.exists()) {
            FileUtils.copyFile(file, new File(model.getFile().getAbsolutePath() + ".bak"), false);
        }
        model.setVersion(ModelVersion.VERSION);
        IArchiveManager archiveManager = (IArchiveManager) model.getAdapter(IArchiveManager.class);
        archiveManager.saveModel();
        CommandStack stack = (CommandStack) model.getAdapter(CommandStack.class);
        stack.markSaveLocation();
        firePropertyChange(model, COMMAND_STACK_CHANGED, true, false);
        markDiagramModelsAsSaved(model);
        firePropertyChange(this, PROPERTY_MODEL_SAVED, null, model);
        return true;
    }

    @Override
    public boolean saveModelAs(IArchimateModel model) throws IOException {
        File file = askSaveModel();
        if (file == null) {
            return false;
        }
        model.setFile(file);
        return saveModel(model);
    }

    @Override
    public boolean isModelLoaded(File file) {
        if (file != null) {
            for (IArchimateModel model : getModels()) {
                if (file.equals(model.getFile())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isModelDirty(IArchimateModel model) {
        if (model == null) {
            return false;
        }
        CommandStack stack = (CommandStack) model.getAdapter(CommandStack.class);
        return stack != null && stack.isDirty();
    }

    /**
     * Ask user for file name to save model
     * @return the file or null
     */
    private File askSaveModel() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        shell.setActive();
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setFilterExtensions(new String[] { ARCHIMATE_FILE_WILDCARD, "*.*" });
        String path = dialog.open();
        if (path == null) {
            return null;
        }
        if (dialog.getFilterIndex() == 0 && !path.endsWith(ARCHIMATE_FILE_EXTENSION)) {
            path += ARCHIMATE_FILE_EXTENSION;
        }
        File file = new File(path);
        for (IArchimateModel m : getModels()) {
            if (file.equals(m.getFile())) {
                MessageDialog.openWarning(shell, Messages.EditorModelManager_8, NLS.bind(Messages.EditorModelManager_9, file));
                return null;
            }
        }
        if (file.exists()) {
            boolean result = MessageDialog.openQuestion(shell, Messages.EditorModelManager_10, NLS.bind(Messages.EditorModelManager_11, file));
            if (!result) {
                return null;
            }
        }
        return file;
    }

    /**
     * Create a new ComandStack for the Model
     * @param model
     */
    private void createNewCommandStack(final IArchimateModel model) {
        CommandStack cmdStack = new CommandStack();
        cmdStack.addCommandStackListener(new CommandStackListener() {

            public void commandStackChanged(EventObject event) {
                firePropertyChange(model, COMMAND_STACK_CHANGED, false, true);
            }
        });
        AnimationUtil.registerCommandStack(cmdStack);
        model.setAdapter(CommandStack.class, cmdStack);
    }

    /**
     * Remove a CommandStack
     * @param model
     */
    private void deleteCommandStack(IArchimateModel model) {
        CommandStack stack = (CommandStack) model.getAdapter(CommandStack.class);
        if (stack != null) {
            stack.dispose();
        }
    }

    /**
     * Set all diagram models in a model to be marked as "saved" - this for the editor view persistence
     */
    private void markDiagramModelsAsSaved(IArchimateModel model) {
        for (IDiagramModel dm : model.getDiagramModels()) {
            dm.setAdapter(ADAPTER_PROPERTY_MODEL_SAVED, true);
        }
    }

    /**
     * Create a new ArchiveManager for the model
     */
    private IArchiveManager createNewArchiveManager(IArchimateModel model) {
        IArchiveManager archiveManager = IArchiveManager.FACTORY.createArchiveManager(model);
        model.setAdapter(IArchiveManager.class, archiveManager);
        try {
            archiveManager.loadImages();
        } catch (IOException ex) {
            Logger.logError("Could not load images", ex);
            ex.printStackTrace();
        }
        return archiveManager;
    }

    /**
     * Remove the model's ArchiveManager
     */
    private void deleteArchiveManager(IArchimateModel model) {
        IArchiveManager archiveManager = (IArchiveManager) model.getAdapter(IArchiveManager.class);
        if (archiveManager != null) {
            archiveManager.dispose();
        }
    }

    public void saveState() throws IOException {
        Document doc = new Document();
        Element rootElement = new Element("models");
        doc.setRootElement(rootElement);
        for (IArchimateModel model : getModels()) {
            File file = model.getFile();
            if (file != null) {
                Element modelElement = new Element("model");
                modelElement.setAttribute("file", file.getAbsolutePath());
                rootElement.addContent(modelElement);
            }
        }
        JDOMUtils.write2XMLFile(doc, backingFile);
    }

    private void loadState() throws IOException, JDOMException {
        if (backingFile.exists()) {
            Document doc = JDOMUtils.readXMLFile(backingFile);
            if (doc.hasRootElement()) {
                Element rootElement = doc.getRootElement();
                for (Object e : rootElement.getChildren("model")) {
                    Element modelElement = (Element) e;
                    String filePath = modelElement.getAttributeValue("file");
                    if (filePath != null) {
                        loadModel(new File(filePath));
                    }
                }
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        fListeners.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        fListeners.removePropertyChangeListener(listener);
    }

    public void firePropertyChange(Object source, String prop, Object oldValue, Object newValue) {
        fListeners.firePropertyChange(new PropertyChangeEvent(source, prop, oldValue, newValue));
    }

    /**
     * Adapter listener class.
     * Forwards on messages so that listeners don't have to adapt to ECore objects
     */
    private class ECoreAdapter extends EContentAdapter {

        @Override
        public void notifyChanged(Notification msg) {
            super.notifyChanged(msg);
            firePropertyChange(this, PROPERTY_ECORE_EVENT, null, msg);
        }
    }
}
