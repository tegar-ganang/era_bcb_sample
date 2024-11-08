package com.safi.workshop.part;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.workspace.WorkspaceEditingDomainFactory;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.common.core.util.Log;
import org.eclipse.gmf.runtime.common.core.util.Trace;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIDebugOptions;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIPlugin;
import org.eclipse.gmf.runtime.common.ui.internal.CommonUIStatusCodes;
import org.eclipse.gmf.runtime.common.ui.internal.l10n.CommonUIMessages;
import org.eclipse.gmf.runtime.diagram.core.services.ViewService;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IPrimaryEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramGraphicalViewer;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.gmf.runtime.emf.commands.core.command.AbstractTransactionalCommand;
import org.eclipse.gmf.runtime.emf.core.resources.GMFResource;
import org.eclipse.gmf.runtime.emf.core.util.EMFCoreUtil;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.safi.core.initiator.Initiator;
import com.safi.core.saflet.Saflet;
import com.safi.db.Query;
import com.safi.db.server.config.Prompt;
import com.safi.server.manager.SafiServerRemoteManager;
import com.safi.server.saflet.util.FileUtils;
import com.safi.workshop.SafiNavigator;
import com.safi.workshop.TelephonyModulePlugin;
import com.safi.workshop.edit.parts.HandlerEditPart;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin.ActionStepProfile;
import com.safi.workshop.sqlexplorer.plugin.editors.SQLEditorInput;
import com.safi.workshop.util.SafletPersistenceManager;

/**
 * @generated
 */
public class SafiWorkshopEditorUtil {

    private static Map<Integer, File> promptCache = new HashMap<Integer, File>();

    private static volatile AsteriskDiagramEditor currentAsteriskEditor;

    public static synchronized AsteriskDiagramEditor getCurrentAsteriskEditor() {
        return currentAsteriskEditor;
    }

    public static synchronized void setCurrentAsteriskEditor(AsteriskDiagramEditor currentAsteriskEditor) {
        SafiWorkshopEditorUtil.currentAsteriskEditor = currentAsteriskEditor;
    }

    /**
   * @generated
   */
    public static Map getSaveOptions() {
        Map saveOptions = new HashMap();
        saveOptions.put(XMLResource.OPTION_ENCODING, "UTF-8");
        saveOptions.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
        return saveOptions;
    }

    /**
   * @generated
   */
    public static boolean openDiagram(Resource diagram) throws PartInitException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.openEditor(new URIEditorInput(diagram.getURI()), AsteriskDiagramEditor.ID);
        return true;
    }

    public static IEditorPart getCurrentEditor() {
        Display d = PlatformUI.getWorkbench().getDisplay();
        if (Thread.currentThread() == d.getThread()) return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor(); else {
            final IEditorPart[] parta = new IEditorPart[] { null };
            d.syncExec(new Runnable() {

                public void run() {
                    parta[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                }

                ;
            });
            return parta[0];
        }
    }

    public static IEditorPart openDiagram(URI fileURI, final boolean isDebugDiagram, final boolean activate) {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = workbenchWindow.getActivePage();
        IEditorDescriptor editorDescriptor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(fileURI.toFileString());
        if (editorDescriptor == null) {
            MessageDialog.openError(workbenchWindow.getShell(), Messages.DiagramEditorActionBarAdvisor_DefaultFileEditorTitle, NLS.bind(Messages.DiagramEditorActionBarAdvisor_DefaultFileEditorMessage, fileURI.toFileString()));
            return null;
        } else {
            try {
                URIEditorInput editorInput = new URIEditorInput(fileURI) {

                    @Override
                    public IPersistableElement getPersistable() {
                        if (isDebugDiagram) return null;
                        return super.getPersistable();
                    }
                };
                return page.openEditor(editorInput, editorDescriptor.getId(), activate);
            } catch (PartInitException exception) {
                MessageDialog.openError(workbenchWindow.getShell(), Messages.DiagramEditorActionBarAdvisor_DefaultEditorOpenErrorTitle, exception.getMessage());
            }
        }
        return null;
    }

    public static void initializePalette() {
        String viewId = "org.eclipse.gef.ui.palette_view";
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow != null) {
            IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
            if (workbenchPage != null) {
                try {
                    workbenchPage.showView(viewId, null, IWorkbenchPage.VIEW_CREATE);
                } catch (PartInitException pie) {
                    Trace.catching(CommonUIPlugin.getDefault(), CommonUIDebugOptions.EXCEPTIONS_CATCHING, SafiWorkshopEditorUtil.class, pie.getMessage(), pie);
                    Log.warning(CommonUIPlugin.getDefault(), CommonUIStatusCodes.GENERAL_UI_FAILURE, pie.getMessage(), pie);
                    String message = MessageFormat.format(CommonUIMessages.WorkbenchPartActivator_ErrorMessage, new Object[] { viewId });
                    ErrorDialog.openError(Display.getCurrent().getActiveShell(), null, message, new Status(IStatus.ERROR, CommonUIPlugin.getPluginId(), CommonUIStatusCodes.GENERAL_UI_FAILURE, pie.getLocalizedMessage(), pie));
                }
            }
        }
    }

    /**
   * @generated
   */
    public static String getUniqueFileName(IPath containerFullPath, String fileName, String extension) {
        if (containerFullPath == null) {
            containerFullPath = new Path("");
        }
        if (fileName == null || fileName.trim().length() == 0) {
            fileName = "default";
        }
        IPath filePath = containerFullPath.append(fileName);
        if (extension != null && !extension.equals(filePath.getFileExtension())) {
            filePath = filePath.addFileExtension(extension);
        }
        extension = filePath.getFileExtension();
        fileName = filePath.removeFileExtension().lastSegment();
        int i = 1;
        while (filePath.toFile().exists()) {
            i++;
            filePath = containerFullPath.append(fileName + i);
            if (extension != null) {
                filePath = filePath.addFileExtension(extension);
            }
        }
        return filePath.lastSegment();
    }

    public static String getUniqueFileName(IProject project, String fileName, String extension) {
        if (fileName == null || fileName.trim().length() == 0) {
            fileName = "callFlow";
        }
        int i = 0;
        String name = fileName + "." + extension;
        while (project.getFile(name).exists()) name = fileName + ++i + "." + extension;
        return name;
    }

    /**
   * Allows user to select file and loads it as a model.
   * 
   * @generated
   */
    public static Resource openModel(Shell shell, String description, TransactionalEditingDomain editingDomain) {
        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        if (description != null) {
            fileDialog.setText(description);
        }
        fileDialog.open();
        String fileName = fileDialog.getFileName();
        if (fileName == null || fileName.length() == 0) {
            return null;
        }
        if (fileDialog.getFilterPath() != null) {
            fileName = fileDialog.getFilterPath() + File.separator + fileName;
        }
        URI uri = URI.createFileURI(fileName);
        Resource resource = null;
        try {
            resource = editingDomain.getResourceSet().getResource(uri, true);
        } catch (WrappedException we) {
            AsteriskDiagramEditorPlugin.getInstance().logError("Unable to load resource: " + uri, we);
            MessageDialog.openError(shell, Messages.AsteriskDiagramEditorUtil_OpenModelResourceErrorDialogTitle, NLS.bind(Messages.AsteriskDiagramEditorUtil_OpenModelResourceErrorDialogMessage, fileName));
        }
        return resource;
    }

    /**
   * Runs the wizard in a dialog.
   * 
   * @generated
   */
    public static void runWizard(Shell shell, Wizard wizard, String settingsKey) {
        IDialogSettings pluginDialogSettings = AsteriskDiagramEditorPlugin.getInstance().getDialogSettings();
        IDialogSettings wizardDialogSettings = pluginDialogSettings.getSection(settingsKey);
        if (wizardDialogSettings == null) {
            wizardDialogSettings = pluginDialogSettings.addNewSection(settingsKey);
        }
        wizard.setDialogSettings(wizardDialogSettings);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.create();
        dialog.getShell().setSize(Math.max(500, dialog.getShell().getSize().x), 500);
        dialog.open();
    }

    /**
   * Store model element in the resource. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
    private static void attachModelToResource(Saflet model, Resource resource) {
        resource.getContents().add(model);
    }

    /**
   * @generated
   */
    public static void selectElementsInDiagram(IDiagramWorkbenchPart diagramPart, List editParts) {
        diagramPart.getDiagramGraphicalViewer().deselectAll();
        EditPart firstPrimary = null;
        for (Iterator it = editParts.iterator(); it.hasNext(); ) {
            EditPart nextPart = (EditPart) it.next();
            diagramPart.getDiagramGraphicalViewer().appendSelection(nextPart);
            if (firstPrimary == null && nextPart instanceof IPrimaryEditPart) {
                firstPrimary = nextPart;
            }
        }
        if (!editParts.isEmpty()) {
            diagramPart.getDiagramGraphicalViewer().reveal(firstPrimary != null ? firstPrimary : (EditPart) editParts.get(0));
        }
    }

    /**
   * @generated
   */
    private static int findElementsInDiagramByID(DiagramEditPart diagramPart, EObject element, List editPartCollector) {
        IDiagramGraphicalViewer viewer = (IDiagramGraphicalViewer) diagramPart.getViewer();
        final int intialNumOfEditParts = editPartCollector.size();
        if (element instanceof View) {
            EditPart editPart = (EditPart) viewer.getEditPartRegistry().get(element);
            if (editPart != null) {
                editPartCollector.add(editPart);
                return 1;
            }
        }
        String elementID = EMFCoreUtil.getProxyID(element);
        List associatedParts = viewer.findEditPartsForElement(elementID, IGraphicalEditPart.class);
        for (Iterator editPartIt = associatedParts.iterator(); editPartIt.hasNext(); ) {
            EditPart nextPart = (EditPart) editPartIt.next();
            EditPart parentPart = nextPart.getParent();
            while (parentPart != null && !associatedParts.contains(parentPart)) {
                parentPart = parentPart.getParent();
            }
            if (parentPart == null) {
                editPartCollector.add(nextPart);
            }
        }
        if (intialNumOfEditParts == editPartCollector.size()) {
            if (!associatedParts.isEmpty()) {
                editPartCollector.add(associatedParts.iterator().next());
            } else {
                if (element.eContainer() != null) {
                    return findElementsInDiagramByID(diagramPart, element.eContainer(), editPartCollector);
                }
            }
        }
        return editPartCollector.size() - intialNumOfEditParts;
    }

    /**
   * @generated
   */
    public static View findView(DiagramEditPart diagramEditPart, EObject targetElement, LazyElement2ViewMap lazyElement2ViewMap) {
        boolean hasStructuralURI = false;
        if (targetElement.eResource() instanceof XMLResource) {
            hasStructuralURI = ((XMLResource) targetElement.eResource()).getID(targetElement) == null;
        }
        View view = null;
        if (hasStructuralURI && !lazyElement2ViewMap.getElement2ViewMap().isEmpty()) {
            view = (View) lazyElement2ViewMap.getElement2ViewMap().get(targetElement);
        } else if (findElementsInDiagramByID(diagramEditPart, targetElement, lazyElement2ViewMap.editPartTmpHolder) > 0) {
            EditPart editPart = (EditPart) lazyElement2ViewMap.editPartTmpHolder.get(0);
            lazyElement2ViewMap.editPartTmpHolder.clear();
            view = editPart.getModel() instanceof View ? (View) editPart.getModel() : null;
        }
        return (view == null) ? diagramEditPart.getDiagramView() : view;
    }

    /** @generated NOT */
    public static File getFile(Resource resource) {
        URI resourceUri = resource.getURI();
        if (resourceUri != null && resourceUri.isFile()) {
            File file = new File(resourceUri.toFileString());
            if (!file.isDirectory()) {
                return file;
            }
        }
        return null;
    }

    public static IResource getCoreResourceForEMFResource(Resource resource) {
        URI eUri = resource.getURI();
        if (eUri.isPlatformResource()) {
            String platformString = eUri.toPlatformString(true);
            return ResourcesPlugin.getWorkspace().getRoot().findMember(platformString);
        }
        return null;
    }

    /**
   * @generated
   */
    public static class LazyElement2ViewMap {

        /**
     * @generated
     */
        private Map element2ViewMap;

        /**
     * @generated
     */
        private View scope;

        /**
     * @generated
     */
        private Set elementSet;

        /**
     * @generated
     */
        public final List editPartTmpHolder = new ArrayList();

        /**
     * @generated
     */
        public LazyElement2ViewMap(View scope, Set elements) {
            this.scope = scope;
            this.elementSet = elements;
        }

        /**
     * @generated
     */
        public final Map getElement2ViewMap() {
            if (element2ViewMap == null) {
                element2ViewMap = new HashMap();
                for (Iterator it = elementSet.iterator(); it.hasNext(); ) {
                    EObject element = (EObject) it.next();
                    if (element instanceof View) {
                        View view = (View) element;
                        if (view.getDiagram() == scope.getDiagram()) {
                            element2ViewMap.put(element, element);
                        }
                    }
                }
                buildElement2ViewMap(scope, element2ViewMap, elementSet);
            }
            return element2ViewMap;
        }

        /**
     * @generated
     */
        static Map buildElement2ViewMap(View parentView, Map element2ViewMap, Set elements) {
            if (elements.size() == element2ViewMap.size()) return element2ViewMap;
            if (parentView.isSetElement() && !element2ViewMap.containsKey(parentView.getElement()) && elements.contains(parentView.getElement())) {
                element2ViewMap.put(parentView.getElement(), parentView);
                if (elements.size() == element2ViewMap.size()) return element2ViewMap;
            }
            for (Iterator it = parentView.getChildren().iterator(); it.hasNext(); ) {
                buildElement2ViewMap((View) it.next(), element2ViewMap, elements);
                if (elements.size() == element2ViewMap.size()) return element2ViewMap;
            }
            for (Iterator it = parentView.getSourceEdges().iterator(); it.hasNext(); ) {
                buildElement2ViewMap((View) it.next(), element2ViewMap, elements);
                if (elements.size() == element2ViewMap.size()) return element2ViewMap;
            }
            for (Iterator it = parentView.getSourceEdges().iterator(); it.hasNext(); ) {
                buildElement2ViewMap((View) it.next(), element2ViewMap, elements);
                if (elements.size() == element2ViewMap.size()) return element2ViewMap;
            }
            return element2ViewMap;
        }
    }

    private static SafiNavigator safiNavigator;

    public static AsteriskDiagramEditor openDebugEditor(String diagramName, String fileExt, String currentPath, boolean activate) throws IOException {
        String suffix = "_debug." + fileExt;
        AsteriskDiagramEditorPlugin.getDefault().logInfo("Trying to create temp file " + (diagramName + fileExt));
        File tempFile = null;
        try {
            tempFile = File.createTempFile(diagramName, suffix);
        } catch (Exception e) {
            throw new IOException("Couldn't create temp file " + (diagramName + fileExt));
        }
        AsteriskDiagramEditorPlugin.getDefault().logInfo("Created temp file " + tempFile.getAbsolutePath());
        tempFile.deleteOnExit();
        FileUtils.copyFile(currentPath, tempFile.getAbsolutePath());
        SafiWorkshopEditorUtil.openDiagram(URI.createFileURI(tempFile.getAbsolutePath()), true, activate);
        IEditorPart activeEditor = AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (!(activeEditor instanceof AsteriskDiagramEditor)) {
            return null;
        }
        AsteriskDiagramEditor asteriskDiagramEditor = ((AsteriskDiagramEditor) activeEditor);
        HandlerEditPart handlerEditPart = (HandlerEditPart) asteriskDiagramEditor.getDiagramEditPart();
        handlerEditPart.setDebug(true);
        for (Iterator iter = handlerEditPart.getChildren().iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if (obj instanceof IGraphicalEditPart) {
                ((IGraphicalEditPart) obj).disableEditMode();
            }
        }
        return asteriskDiagramEditor;
    }

    public static File getPromptFile(Prompt p) throws Exception {
        File f = promptCache.get(p.getId());
        if (f != null) return f;
        byte[] bytes = SafiServerRemoteManager.getInstance().getPromptFile(p.getId());
        String promptName = p.getName().replace('/', '_');
        f = File.createTempFile(promptName, "." + p.getExtension());
        f.deleteOnExit();
        FileUtils.writeFile(f, bytes);
        promptCache.put(p.getId(), f);
        return f;
    }

    public static SafiNavigator getSafiNavigator() {
        return getSafiNavigator(true);
    }

    public static synchronized SafiNavigator getSafiNavigator(boolean create) {
        if (safiNavigator != null) return safiNavigator;
        if (create) {
            IWorkbenchPage page = AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
            if (page != null) {
                safiNavigator = (SafiNavigator) page.findView("com.safi.eclipse.NavigatorView");
                if (safiNavigator == null) try {
                    safiNavigator = (SafiNavigator) page.showView("com.safi.eclipse.NavigatorView");
                } catch (PartInitException e) {
                    AsteriskDiagramEditorPlugin.getDefault().logError("Couldn't show SafiNavigator", e);
                }
            }
        }
        return safiNavigator;
    }

    public static boolean hasDebugFile(ResourceSet set) {
        GMFResource gmfResource = null;
        for (Resource r : set.getResources()) {
            if (r instanceof GMFResource && ("saflet".equalsIgnoreCase(r.getURI().fileExtension()))) {
                gmfResource = (GMFResource) r;
                break;
            }
        }
        if (gmfResource != null) {
            String file = null;
            URI uri = gmfResource.getURI();
            if (uri.isFile()) file = uri.toFileString(); else {
                file = uri.segment(uri.segmentCount() - 1);
            }
            if (file != null && file.endsWith("_debug.saflet")) {
                return true;
            }
        }
        return false;
    }

    public static void activateWorkbenchShell() {
        final Shell[] shells = PlatformUI.getWorkbench().getDisplay().getShells();
        if (shells.length > 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    shells[0].forceActive();
                    shells[0].forceFocus();
                }
            });
        }
    }

    public static Shell getActiveShell() {
        return PlatformUI.getWorkbench().getDisplay().getActiveShell();
    }

    public static void setSafiNavigator(SafiNavigator safiNavigator2) {
        safiNavigator = safiNavigator2;
    }

    public static AsteriskDiagramEditor getEditorForResource(IResource platformResource) {
        IWorkbenchPage[] pages = (AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages());
        for (IWorkbenchPage page : pages) {
            IEditorReference refs[] = page.getEditorReferences();
            for (IEditorReference ref : refs) {
                IEditorPart part = ref.getEditor(false);
                if (part == null) continue;
                if (part instanceof AsteriskDiagramEditor) {
                    ResourceSet set = ((AsteriskDiagramEditor) part).getEditingDomain().getResourceSet();
                    if (set.getResources().isEmpty()) return null;
                    Resource r = set.getResources().get(0);
                    if (r == null) return null;
                    if (platformResource.equals(WorkspaceSynchronizer.getFile(r))) {
                        return (AsteriskDiagramEditor) part;
                    }
                }
            }
        }
        return null;
    }

    public static boolean closeEditor(AsteriskDiagramEditor editor) {
        IWorkbenchPage[] pages = (AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages());
        for (IWorkbenchPage page : pages) {
            IEditorReference refs[] = page.getEditorReferences();
            for (IEditorReference ref : refs) {
                IEditorPart part = ref.getEditor(false);
                if (part == editor) {
                    page.closeEditor(editor, false);
                    return true;
                } else continue;
            }
        }
        return false;
    }

    public static Object[] getEObjectByID(String uid) {
        IWorkbenchPage[] pages = (AsteriskDiagramEditorPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages());
        for (IWorkbenchPage page : pages) {
            IEditorReference refs[] = page.getEditorReferences();
            for (IEditorReference ref : refs) {
                IEditorPart part = ref.getEditor(false);
                if (part == null) continue;
                if (part instanceof AsteriskDiagramEditor && ((AsteriskDiagramEditor) part).isDebug()) {
                    ResourceSet set = ((AsteriskDiagramEditor) part).getEditingDomain().getResourceSet();
                    if (set.getResources().isEmpty()) return null;
                    Resource r = set.getResources().get(0);
                    if (r == null) return null;
                    final EObject object = r.getEObject(uid);
                    if (object == null) continue;
                    return new Object[] { object, (AsteriskDiagramEditor) part };
                }
            }
        }
        return null;
    }

    public static Resource createDiagram(URI diagramURI, final TelephonyModulePlugin module, final ActionStepProfile asp, IProgressMonitor progressMonitor) {
        {
            TransactionalEditingDomain editingDomain = WorkspaceEditingDomainFactory.INSTANCE.createEditingDomain();
            progressMonitor.beginTask(Messages.AsteriskDiagramEditorUtil_CreateDiagramProgressTask, 3);
            final Resource diagramResource = editingDomain.getResourceSet().createResource(diagramURI);
            final String diagramName = diagramURI.lastSegment();
            final URI fURI = diagramURI;
            AbstractTransactionalCommand command = new AbstractTransactionalCommand(editingDomain, Messages.AsteriskDiagramEditorUtil_CreateDiagramCommandLabel, Collections.EMPTY_LIST) {

                @Override
                protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
                    Saflet model = module.createInitialSaflet();
                    attachModelToResource(model, diagramResource);
                    Diagram diagram = ViewService.createDiagram(model, HandlerEditPart.MODEL_ID, AsteriskDiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT);
                    String safletName = fURI.trimFileExtension().lastSegment();
                    if (diagram != null) {
                        diagramResource.getContents().add(diagram);
                        diagram.setName(diagramName);
                        diagram.setElement(model);
                        Saflet handler = (Saflet) diagram.getElement();
                        handler.setName(safletName);
                        Initiator initiator = (Initiator) asp.modelFactory.getModel(Integer.valueOf(asp.semanticHint));
                        initiator.createDefaultOutputs();
                        initiator.setName(asp.displayName);
                        handler.setInitiator(initiator);
                        initiator.setSaflet(handler);
                        module.preProcessSaflet(handler);
                    }
                    try {
                        diagramResource.save(com.safi.workshop.part.SafiWorkshopEditorUtil.getSaveOptions());
                        try {
                            WorkspaceSynchronizer.getFile(diagramResource).setPersistentProperty(SafletPersistenceManager.SAFLET_NAME_KEY, safletName);
                        } catch (CoreException e) {
                            AsteriskDiagramEditorPlugin.getDefault().logError("Unable to set persistent property", e);
                        }
                    } catch (IOException e) {
                        AsteriskDiagramEditorPlugin.getDefault().logError("Unable to store model and diagram resources", e);
                    }
                    return CommandResult.newOKCommandResult();
                }
            };
            try {
                OperationHistoryFactory.getOperationHistory().execute(command, new SubProgressMonitor(progressMonitor, 1), null);
            } catch (ExecutionException e) {
                AsteriskDiagramEditorPlugin.getDefault().logError("Unable to create model and diagram", e);
            }
            return diagramResource;
        }
    }

    public static void uploadPromptFile(Prompt prompt, File f) throws Exception {
        if (!f.exists()) {
            throw new FileNotFoundException("Couldn't find actionpak file " + f);
        }
        Date lm = new Date(f.lastModified());
        String promptName = prompt.getName();
        String pid = prompt.getProject() == null ? null : prompt.getProject().getName();
        if (prompt.getExtension() != null) promptName += "." + prompt.getExtension();
        if (pid != null) promptName = pid + "/" + promptName;
        String promptFileName = prompt.getName() + '.' + prompt.getExtension();
        boolean promptNeedsUpdate = SafiServerRemoteManager.getInstance().promptNeedsUpdate(pid, promptFileName, lm);
        if (!promptNeedsUpdate) {
            promptNeedsUpdate = MessageDialog.openConfirm(getActiveShell(), "Newer File Exists", "A newer prompt file by this name already exists on the server.  Do you still want to overwrite?");
        }
        if (promptNeedsUpdate) {
            byte[] data = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(data);
            bis.close();
            SafiServerRemoteManager.getInstance().transferPrompt(pid, promptFileName, data);
            promptCache.remove(prompt.getId());
        }
    }

    public static void closeSQLEditors(Query qry) {
        IEditorReference[] editorRefs = AsteriskDiagramEditorPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
        List<IEditorReference> editors = new ArrayList<IEditorReference>();
        for (IEditorReference ed : editorRefs) {
            try {
                if (ed.getEditorInput() instanceof SQLEditorInput) {
                    SQLEditorInput input = (SQLEditorInput) ed.getEditorInput();
                    if (input.getQuery() == qry) {
                        editors.add(ed);
                    }
                }
            } catch (PartInitException e) {
                e.printStackTrace();
            }
        }
        AsteriskDiagramEditorPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditors(editors.toArray(new IEditorReference[editors.size()]), false);
    }
}
