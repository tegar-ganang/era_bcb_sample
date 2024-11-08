package org.dengues.designer.ui.database.editors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import org.dengues.commons.IDenguesCoreContants;
import org.dengues.core.DenguesCorePlugin;
import org.dengues.core.resource.WarehouseResourceFactory;
import org.dengues.designer.ui.database.parts.DBEditPartFactory;
import org.dengues.designer.ui.editors.GEFEditorContextMenuProvider;
import org.dengues.designer.ui.editors.actions.GEFCopyAction;
import org.dengues.designer.ui.editors.actions.GEFCutAction;
import org.dengues.designer.ui.editors.actions.GEFDeleteAction;
import org.dengues.designer.ui.editors.actions.GEFPasteAction;
import org.dengues.designer.ui.editors.dnd.GEFEditorDropTargetListener;
import org.dengues.designer.ui.editors.palette.DBEditorPaletteFactory;
import org.dengues.designer.ui.i18n.Messages;
import org.dengues.designer.ui.properties.DenguesEditorTabbedPropertySheetPage;
import org.dengues.model.database.DatabaseDiagram;
import org.dengues.ui.editors.AbstractGenericGEFEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 2007-12-13 qiang.zhang $
 * 
 */
public class GEFDatabaseEditor extends AbstractGenericGEFEditor implements ITabbedPropertySheetPageContributor {

    public static final String ID = "org.dengues.designer.databaseEditor";

    public static final String DEFAULT_EXTENSION = ".dbe";

    private static PaletteRoot paletteRoot;

    private DatabaseDiagram databaseDiagram;

    public static final int GRID_SIZE = 8;

    /**
     * Qiang.Zhang.Adolf@gmail.com GEFDatabaseEditor constructor comment.
     */
    public GEFDatabaseEditor() {
        super();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        viewer.setEditPartFactory(new DBEditPartFactory());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        GEFDeleteAction deleteAction = new GEFDeleteAction(this);
        getActionRegistry().registerAction(deleteAction);
        getSelectionActions().add(deleteAction.getId());
        SelectionAction action = new GEFCopyAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new GEFCutAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new GEFPasteAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        ContextMenuProvider menuProvider = new GEFEditorContextMenuProvider(this, viewer, getActionRegistry());
        viewer.setContextMenu(menuProvider);
        getGraphicalViewer().setKeyHandler(new GraphicalViewerKeyHandler(getGraphicalViewer()).setParent(getCommonKeyHandler()));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(GRID_SIZE, GRID_SIZE));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, new Boolean(true));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, new Boolean(true));
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getDatabaseTitle".
     * 
     * @return
     */
    public String getDatabaseTitle() {
        return DenguesCorePlugin.getDefault().getDenguesTitle(((DBFileEditorInput) getEditorInput()).getFile().getFullPath().toPortableString().substring(1));
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) {
            return new DenguesEditorTabbedPropertySheetPage(this);
        }
        return super.getAdapter(type);
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof DBFileEditorInput) {
            databaseDiagram = ((DBFileEditorInput) input).getDbDiagram();
            setPartName(databaseDiagram.getName());
        }
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(databaseDiagram);
        getGraphicalViewer().addDropTargetListener(new GEFEditorDropTargetListener(this));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        monitor.beginTask(Messages.getString("GEFDatabaseEditor.TaskName"), 100);
        monitor.worked(10);
        try {
            ((DBFileEditorInput) getEditorInput()).getSqlScriptFile();
            WarehouseResourceFactory.saveStorage(getDatabaseDiagram());
            getCommandStack().markSaveLocation();
            setDirty(false);
            monitor.worked(10);
        } catch (Exception e) {
            e.printStackTrace();
            monitor.setCanceled(true);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void doSaveAs() {
        Shell shell = getSite().getWorkbenchWindow().getShell();
        SaveAsDialog dialog = new SaveAsDialog(shell);
        dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
        dialog.open();
        IPath path = dialog.getResult();
        if (path != null) {
            final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            try {
                new ProgressMonitorDialog(shell).run(false, false, new WorkspaceModifyOperation() {

                    @Override
                    public void execute(final IProgressMonitor monitor) {
                        try {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
                        } catch (CoreException ce) {
                            ce.printStackTrace();
                        } catch (Exception ioe) {
                            ioe.printStackTrace();
                        }
                    }
                });
                setInput(new FileEditorInput(file));
                getCommandStack().markSaveLocation();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
        }
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = DBEditorPaletteFactory.createPalette();
        }
        return paletteRoot;
    }

    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        super.selectionChanged(part, selection);
        if (this.equals(part)) {
            updateActions(getSelectionActions());
        }
    }

    public String getContributorId() {
        return IDenguesCoreContants.ID_VIEW_PROP;
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getDatabaseDiagram".
     * 
     * @return
     */
    public DatabaseDiagram getDatabaseDiagram() {
        return databaseDiagram;
    }
}
