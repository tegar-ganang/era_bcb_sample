package com.volantis.mcs.eclipse.ab.editors.devices;

import com.volantis.devrep.repository.accessors.MDPRArchiveAccessor;
import com.volantis.devrep.repository.api.devices.DeviceRepositorySchemaConstants;
import com.volantis.mcs.eclipse.ab.ABPlugin;
import com.volantis.mcs.eclipse.ab.editors.EditorMessages;
import com.volantis.mcs.eclipse.ab.editors.devices.odom.DeviceODOMElementFactory;
import com.volantis.mcs.eclipse.ab.editors.dom.MultiPageODOMEditor;
import com.volantis.mcs.eclipse.ab.editors.dom.ODOMEditorContext;
import com.volantis.mcs.eclipse.common.EclipseCommonPlugin;
import com.volantis.mcs.eclipse.common.odom.ODOMChangeSupport;
import com.volantis.mcs.eclipse.common.odom.ODOMElementSelection;
import com.volantis.mcs.eclipse.common.odom.ODOMFactory;
import com.volantis.mcs.eclipse.controls.ControlUtils;
import com.volantis.mcs.eclipse.core.DeviceRepositoryAccessorManager;
import com.volantis.mcs.eclipse.core.MCSProjectNature;
import com.volantis.mcs.eclipse.core.ProjectDeviceRepositoryProvider;
import com.volantis.mcs.repository.RepositoryException;
import com.volantis.synergetics.cornerstone.utilities.xml.jaxp.JAXPTransformerMetaFactory;
import com.volantis.synergetics.io.IOUtils;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.jdom.Element;

/**
 * Multipage editor that allows devices to be edited
 */
public class DeviceEditor extends MultiPageODOMEditor implements IGotoMarker, ISelectionProvider {

    /**
     * The prefix for resources used by this class.
     */
    private static final String RESOURCE_PREFIX = "DeviceEditor.";

    /**
     * The message asking for permission to automatically modify the
     * device repository.
     */
    private static final String AUTO_MOD_MESSAGE = EditorMessages.getString(RESOURCE_PREFIX + "autoModification.message");

    /**
     * The message asking for permission to automatically modify the
     * device repository.
     */
    private static final String AUTO_MOD_TITLE = EditorMessages.getString(RESOURCE_PREFIX + "autoModification.title");

    /**
     * The hierarchy is the default root element for the device editor.
     */
    private static String ROOT_ELEMENT_NAME = DeviceRepositorySchemaConstants.HIERARCHY_ELEMENT_NAME;

    /**
     * The intro message for the editor's Information page.
     */
    private static final String INTRO_MESSAGE = EditorMessages.getString(RESOURCE_PREFIX + "intro.text");

    /**
     * The revision message for the editor's Information page.
     */
    private static final String REVISION_MESSAGE = EditorMessages.getString(RESOURCE_PREFIX + "revision.text");

    /**
     * The version message for the editor's Information page.
     */
    private static final String VERSION_MESSAGE = EditorMessages.getString(RESOURCE_PREFIX + "version.text");

    /**
     * Initializes a <code>DeviceEditor</code> instance
     */
    public DeviceEditor() {
        super(ROOT_ELEMENT_NAME);
    }

    public Object getAdapter(Class adapterClass) {
        return getEditor(0).getAdapter(adapterClass);
    }

    protected void createPages() {
        try {
            DeviceEditorContext editorContext = (DeviceEditorContext) getODOMEditorContext();
            int index = addPage(new DeviceOverviewPart(editorContext), getEditorInput());
            setPageText(index, EditorMessages.getString(RESOURCE_PREFIX + "overview.label"));
            index = addPage(new DevicePoliciesPart(editorContext), getEditorInput());
            setPageText(index, EditorMessages.getString(RESOURCE_PREFIX + "policies.label"));
            index = addPage(new DeviceStructurePart(editorContext), getEditorInput());
            setPageText(index, EditorMessages.getString(RESOURCE_PREFIX + "structure.label"));
            index = addPage(createDeviceInformationPage(editorContext.getDeviceRepositoryAccessorManager()));
            setPageText(index, EditorMessages.getString(RESOURCE_PREFIX + "information.label"));
        } catch (PartInitException e) {
            EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
        }
    }

    /**
     * Creates and returns the Composite for the device Information page.
     *
     * @param deviceRAM the manager to use for retrieving information
     * @return the Composite
     */
    private Composite createDeviceInformationPage(DeviceRepositoryAccessorManager deviceRAM) {
        final Object[] formatArgs = new Object[1];
        MessageFormat versionFormat = new MessageFormat(VERSION_MESSAGE);
        formatArgs[0] = deviceRAM.getVersion();
        String versionMessage = versionFormat.format(formatArgs);
        MessageFormat revisionFormat = new MessageFormat(REVISION_MESSAGE);
        formatArgs[0] = deviceRAM.getRevision();
        String revisionMessage = revisionFormat.format(formatArgs);
        return ControlUtils.createMessageComposite(this.getContainer(), SWT.LEFT, new String[] { INTRO_MESSAGE, versionMessage, revisionMessage });
    }

    protected ODOMEditorContext createODOMEditorContext(String rootElementName, final IFile file) {
        ODOMEditorContext context = null;
        try {
            final JAXPTransformerMetaFactory transformerMetaFactory = new JAXPTransformerMetaFactory();
            MDPRArchiveAccessor archiveAccessor = new MDPRArchiveAccessor(file.getLocation().toOSString(), transformerMetaFactory);
            boolean ok = true;
            if (archiveAccessor.willBeModifiedOnLoad()) {
                ok = confirmAndModify(archiveAccessor);
            }
            if (ok) {
                ODOMChangeSupport.ChangeSupportDisabledCommand command = new ODOMChangeSupport.ChangeSupportDisabledCommand() {

                    public Object execute() {
                        ODOMEditorContext context = null;
                        try {
                            boolean isAdminProject = file.getProject().hasNature(DeviceEditorContext.MCS_ADMIN_NATURE_ID);
                            DeviceRepositoryAccessorManager dram = new DeviceRepositoryAccessorManager(file.getLocation().toOSString(), transformerMetaFactory, new DeviceODOMElementFactory(), isAdminProject);
                            if (dram.getDeviceRepositoryName().equals(MCSProjectNature.getDeviceRepositoryName(file.getProject()))) {
                                ProjectDeviceRepositoryProvider.getSingleton().setDeviceRepositoryAccessorManager(file.getProject(), dram);
                            }
                            context = DeviceEditorContext.createDeviceEditorContext(file, createUndoRedoMementoOriginator(), dram);
                        } catch (RepositoryException e) {
                            EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
                        } catch (IOException e) {
                            EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
                        } catch (CoreException e) {
                            EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
                        }
                        return context;
                    }
                };
                context = (ODOMEditorContext) ODOMChangeSupport.executeWithoutChangeSupport(command);
            }
        } catch (RepositoryException e) {
            EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
        }
        return context;
    }

    /**
     * Display a question dialog that asks the user to give permission for
     * automatic modification of the device repository that will be
     * performed by the MDPRArchiveAccessor. If the user gives permission
     * then take a backup of the device repository, modify the current
     * repository and save it.
     *
     * @return true if the user allows the modification and the modification
     *         was successful; false otherwise
     */
    private boolean confirmAndModify(MDPRArchiveAccessor archiveAccessor) {
        String candidateBackupName = archiveAccessor.getArchiveFileName() + ".old";
        String backupName = createUniqueFileName(candidateBackupName);
        MessageFormat format = new MessageFormat(AUTO_MOD_MESSAGE);
        String message = format.format(new String[] { backupName });
        boolean ok = MessageDialog.openQuestion(new Shell(Display.getDefault()), AUTO_MOD_TITLE, message);
        if (ok) {
            File orig = new File(archiveAccessor.getArchiveFileName());
            try {
                IOUtils.copyFiles(orig, new File(backupName));
                DeviceRepositoryAccessorManager dram = new DeviceRepositoryAccessorManager(archiveAccessor, new ODOMFactory());
                dram.writeRepository();
            } catch (IOException e) {
                EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
            } catch (RepositoryException e) {
                EclipseCommonPlugin.handleError(ABPlugin.getDefault(), e);
            }
        }
        return ok;
    }

    /**
     * Ensure the given file name is unique and append an index
     * increment until it is.
     *
     * @param fileName the file name upon which to base the unique name.
     * @return a unique file name that will be fileName if it is already
     *         unique otherwise will be fileName with a suffix that makes it unique.
     */
    private String createUniqueFileName(String fileName) {
        File file = new File(fileName);
        String origName = file.getAbsolutePath();
        int i = 1;
        while (file.exists()) {
            StringBuffer nextName = new StringBuffer(origName.length() + 2);
            nextName.append(origName).append(".").append(i);
            file = new File(nextName.toString());
            i++;
        }
        return file.getAbsolutePath();
    }

    public void gotoMarker(IMarker marker) {
    }

    public boolean isSaveAsAllowed() {
        boolean allowed = false;
        if (getActiveEditor() != null) {
            allowed = super.isSaveAsAllowed();
        } else {
            allowed = getEditor(0).isSaveAsAllowed();
        }
        return allowed;
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        getODOMEditorContext().getODOMSelectionManager().addSelectionChangedListener(listener);
    }

    /**
     * Gets the current selection by delegating to the ODOMSelectionManager
     * associated with this DeviceEditor's ODOMEditorContext.
     *
     * @return an ODOMElementSelection representing the current selection
     */
    public ISelection getSelection() {
        return getODOMEditorContext().getODOMSelectionManager().getSelection();
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        getODOMEditorContext().getODOMSelectionManager().removeSelectionChangedListener(listener);
    }

    /**
     * Replaces the current selection with the given selection by delegating to
     * the ODOMSelectionManager associated with this DeviceEditor's
     * ODOMEditorContext.
     *
     * @param selection the new selection. Must be an ODOMElementSelection.
     */
    public void setSelection(ISelection selection) {
        getODOMEditorContext().getODOMSelectionManager().setSelection(selection);
    }

    /**
     * Set the selection to the named device assuming the named device
     * is in the device repository.
     * <p/>
     * This is a convenience method and allows callers to circumvent using
     * DeviceRespositoryAccessor manager to get the hierarchy element
     * for the named device.
     *
     * @param deviceName the name of the device
     */
    public void selectDevice(String deviceName) {
        DeviceEditorContext context = (DeviceEditorContext) getODOMEditorContext();
        DeviceRepositoryAccessorManager dram = context.getDeviceRepositoryAccessorManager();
        Element device = dram.getHierarchyDeviceElement(deviceName);
        List selection = new ArrayList();
        selection.add(device);
        setSelection(new ODOMElementSelection(selection));
    }
}
