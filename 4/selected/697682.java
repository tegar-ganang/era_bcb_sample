package de.fraunhofer.isst.axbench.rca.views;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import de.fraunhofer.isst.axbench.axlang.model.api.IAXLangElement;
import de.fraunhofer.isst.axbench.axlang.model.api.IAXLangListener;
import de.fraunhofer.isst.axbench.axlang.model.api.converter.IAXLangWriter;
import de.fraunhofer.isst.axbench.axlang.model.converter.writer.TextWriter;
import de.fraunhofer.isst.axbench.axlang.model.elements.Model;
import de.fraunhofer.isst.axbench.rca.AXBench;
import de.fraunhofer.isst.axbench.rca.RCAConstants;
import de.fraunhofer.isst.axbench.rca.Session;
import de.fraunhofer.isst.axbench.rca.adapters.AXLElementAdapter;
import de.fraunhofer.isst.axbench.rca.adapters.AXLElementWorkbenchContentProvider;
import de.fraunhofer.isst.axbench.rca.advisors.AXBenchActionBarAdvisor;
import de.fraunhofer.isst.axbench.rca.editors.AXLElementEditor;
import de.fraunhofer.isst.axbench.rca.editors.AXLElementEditorInput;

/**
 * @brief Display aXLang model as tree.
 * 
 * This is the simple tree, which is built as follows:
 * - model
 *   - feature model 
 *     - features 
 *   - application model 
 *     - components
 *     - interfaces 
 *   - configuration model
 *     - component configurations
 *     
 * @author Ekkart Kleinod
 * @version 0.3.0
 * @since 0.1.0
 */
public class TreeView extends ViewPart implements ISaveablePart {

    /** Unique id for the perspective. */
    public static final String ID = RCAConstants.ID_VIEW_TREE;

    protected TreeViewer jfTreeViewer = null;

    private boolean bDirty = false;

    /**
	 * @brief Constructs the GUI.
	 * 
	 * This method is called für initialization of the GUI, we instantiate and
	 * configure the GUI elements.
	 * 
	 * @param theParent parent composite
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    public void createPartControl(Composite theParent) {
        jfTreeViewer = new TreeViewer(theParent);
        jfTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
        jfTreeViewer.setContentProvider(new AXLElementWorkbenchContentProvider(AXLElementAdapter.class));
        getSite().setSelectionProvider(jfTreeViewer);
        initContextMenu();
        initOpenAction();
        initDoubleClick();
        initDragAndDrop();
        initContextHelp();
        setModel(Session.getAXLangModel());
    }

    /**
	 * @brief Sets the focus to the tree.
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
    @Override
    public void setFocus() {
        jfTreeViewer.getControl().setFocus();
    }

    /**
	 * @brief Sets the aXLang model.
	 * @param aXLang model
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
    private void setModel(Model axlModel) {
        if (axlModel == null) {
            setPartName("No model");
        } else {
            jfTreeViewer.setInput(axlModel);
            jfTreeViewer.expandToLevel(2);
            axlModel.addAXLangListener(new IAXLangListener() {

                public void elementAdded(IAXLangElement axlElement) {
                    jfTreeViewer.refresh(axlElement.getModel());
                    if (!bDirty) {
                        bDirty = true;
                        firePropertyChange(PROP_DIRTY);
                    }
                }

                public void elementChanged(IAXLangElement axlElement) {
                    jfTreeViewer.refresh(axlElement.getParent());
                    if (!bDirty) {
                        bDirty = true;
                        firePropertyChange(PROP_DIRTY);
                    }
                }
            });
            String sTitle = "Model '" + axlModel.getIdentifier() + "'";
            if (Session.getFile() != null) {
                sTitle += " (" + Session.getFile().getName() + ")";
            }
            setPartName(sTitle);
        }
    }

    /**
	 * @brief Initializes context menu.
	 */
    private void initContextMenu() {
        MenuManager theManager = new MenuManager();
        theManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        Menu theContextMenu = theManager.createContextMenu(jfTreeViewer.getControl());
        jfTreeViewer.getControl().setMenu(theContextMenu);
        getSite().registerContextMenu(theManager, jfTreeViewer);
    }

    /**
	 * @brief Initializes the open action.
	 * 
	 * - set always enabled
	 * - connect with open action of AXBenchActionBarAdvisor
	 * @see AXBenchActionBarAdvisor#OPEN
	 */
    private void initOpenAction() {
        IAction actOpen = new Action() {

            @Override
            public void run() {
                if (isDirty()) {
                    MessageBox dlgSave = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO | SWT.CANCEL);
                    dlgSave.setMessage("Current file modified. Save?");
                    dlgSave.setText("Unsaved File");
                    int iResult = dlgSave.open();
                    if (iResult == SWT.YES) {
                        doSave(null);
                    }
                    if (iResult == SWT.CANCEL) {
                        return;
                    }
                }
                FileDialog dlgOpen = new FileDialog(getSite().getShell(), SWT.OPEN);
                dlgOpen.setText("Open aXLang model");
                if (Session.getFile() != null) {
                    dlgOpen.setFilterPath(Session.getFile().getPath());
                }
                String[] sFilters = { "*" + RCAConstants.EXT_AXL, "*" + RCAConstants.EXT_XML };
                dlgOpen.setFilterExtensions(sFilters);
                String sFileName = dlgOpen.open();
                if (sFileName == null) {
                    return;
                }
                File fleOpen = new File(sFileName);
                try {
                    Session.loadFile(fleOpen);
                    setModel(Session.getAXLangModel());
                    bDirty = false;
                    TreeView.this.firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
                } catch (IOException e) {
                    IStatus theStatus = new Status(IStatus.ERROR, AXBench.class.getCanonicalName(), "Error while loading", e);
                    ErrorDialog dlgError = new ErrorDialog(getSite().getShell(), "Load Error", "Could not load file.", theStatus, IStatus.ERROR);
                    dlgError.open();
                }
            }
        };
        actOpen.setEnabled(true);
        getViewSite().getActionBars().setGlobalActionHandler(RCAConstants.ID_ACTION_OPEN, actOpen);
    }

    /**
	 * @brief Is the view dirty (does it contain non-saved model changes)?
	 * @return dirty or not
	 *  @retval true dirty
	 *  @retval false not dirty
	 */
    public boolean isDirty() {
        return bDirty;
    }

    /**
	 * @brief Saves the model.
	 * @param theMonitor a progress monitor
	 */
    public void doSave(IProgressMonitor theMonitor) {
        if (Session.getFile() == null) {
            doSaveAs();
            return;
        }
        try {
            Session.writeFile();
            bDirty = false;
            firePropertyChange(PROP_DIRTY);
        } catch (IOException e) {
            IStatus theStatus = new Status(IStatus.ERROR, AXBench.class.getCanonicalName(), "Error while saving", e);
            ErrorDialog dlgError = new ErrorDialog(getSite().getShell(), "Save Error", "Could not save file.", theStatus, IStatus.ERROR);
            dlgError.open();
        }
    }

    /**
	 * @brief Saves the model under a new name.
	 */
    public void doSaveAs() {
        FileDialog dlgSave = new FileDialog(getSite().getShell(), SWT.SAVE);
        dlgSave.setText("Save aXLang model as...");
        if (Session.getFile() != null) {
            dlgSave.setFilterPath(Session.getFile().getPath());
        }
        String[] sFilters = { "*" + RCAConstants.EXT_AXL, "*" + RCAConstants.EXT_XML };
        dlgSave.setFilterExtensions(sFilters);
        String sFileName = dlgSave.open();
        if (sFileName == null) {
            return;
        }
        File theFile = new File(sFileName);
        if (theFile.exists()) {
            MessageBox msgOverwrite = new MessageBox(getSite().getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            msgOverwrite.setMessage("File " + sFileName + " already exists.  Overwrite?");
            msgOverwrite.setText("File exists.");
            int iResult = msgOverwrite.open();
            if (iResult != SWT.YES) {
                return;
            }
        }
        Session.setFile(theFile);
        doSave(null);
    }

    /**
	 * @brief Returns, if "save as" is allowed
	 * @return "save as" allowed?
	 *  @retval true allowed
	 *  @retval false forbidden
	 */
    public boolean isSaveAsAllowed() {
        return (Session.getAXLangModel() != null);
    }

    /**
	 * @brief Returns, if save is needed on close
	 * @return save is needed on close?
	 *  @retval true needed
	 *  @retval false not needed
	 */
    public boolean isSaveOnCloseNeeded() {
        return true;
    }

    /**
	 * @brief Initializes double click.
	 * 
	 * On double click the element editor is opened.
	 */
    private void initDoubleClick() {
        jfTreeViewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent theEvent) {
                IAXLangElement axlElement = getSelectedElement();
                AXLElementEditorInput theInput = new AXLElementEditorInput(axlElement.getUID(), axlElement.getIdentifier(), Session.getImageDescriptor(axlElement));
                try {
                    getSite().getPage().openEditor(theInput, AXLElementEditor.ID);
                } catch (PartInitException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	 * @brief Returns the selected aXLang element.
	 * @return selected aXLang element
	 */
    private IAXLangElement getSelectedElement() {
        IStructuredSelection selection = (IStructuredSelection) jfTreeViewer.getSelection();
        return (IAXLangElement) selection.getFirstElement();
    }

    /**
     * @brief Initializes drag'n'drop features.
     * @todo implement drag'n'drop with real values
     */
    private void initDragAndDrop() {
        Transfer[] arrDragTypes = new Transfer[] { TextTransfer.getInstance() };
        jfTreeViewer.addDragSupport(DND.DROP_MOVE, arrDragTypes, new DragSourceListener() {

            public void dragStart(DragSourceEvent event) {
            }

            public void dragFinished(DragSourceEvent event) {
            }

            public void dragSetData(DragSourceEvent event) {
                IStructuredSelection theSelection = (IStructuredSelection) jfTreeViewer.getSelection();
                IAXLangElement axlElement = (IAXLangElement) theSelection.getFirstElement();
                if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                    ByteArrayOutputStream stmOut = new ByteArrayOutputStream();
                    IAXLangWriter theWriter = new TextWriter();
                    try {
                        theWriter.writeAXLangElement(axlElement, null, stmOut);
                        stmOut.close();
                        event.data = stmOut.toString();
                    } catch (Exception e) {
                        event.data = "error converting '" + axlElement.getIdentifier() + "'\n";
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * @brief Initialize context help.
     */
    private void initContextHelp() {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(jfTreeViewer.getTree(), RCAConstants.ID_AXBENCH + ".treeview");
    }
}
