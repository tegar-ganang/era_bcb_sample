package org.dengues.designer.ui.process.editors;

import java.util.EventObject;
import org.dengues.commons.IDenguesCoreContants;
import org.dengues.core.DenguesCorePlugin;
import org.dengues.core.process.EConnectionType;
import org.dengues.designer.ui.editors.GEFEditorContextMenuProvider;
import org.dengues.designer.ui.editors.actions.GEFCopyAction;
import org.dengues.designer.ui.editors.actions.GEFDeleteAction;
import org.dengues.designer.ui.editors.actions.GEFPasteAction;
import org.dengues.designer.ui.editors.dnd.GEFEditorDropTargetListener;
import org.dengues.designer.ui.editors.outline.ProcessEditorOutlinePage;
import org.dengues.designer.ui.editors.palette.CompEditorPaletteFactory;
import org.dengues.designer.ui.i18n.Messages;
import org.dengues.designer.ui.process.actions.ConnectionCreateAction;
import org.dengues.designer.ui.process.models.CompProcess;
import org.dengues.designer.ui.process.parts.CompProcessPartFactory;
import org.dengues.designer.ui.properties.DenguesEditorTabbedPropertySheetPage;
import org.dengues.ui.editors.AbstractGenericGEFEditor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf Qiang.Zhang.Adolf@gmail.com 2008-3-20 qiang.zhang $
 * 
 */
public class GEFComponentsEditor extends AbstractGenericGEFEditor implements ITabbedPropertySheetPageContributor {

    private static PaletteRoot paletteRoot;

    private CompProcess process;

    /**
     * Qiang.Zhang.Adolf@gmail.com GEFComponentsEditor constructor comment.
     */
    public GEFComponentsEditor() {
        super();
        process = new CompProcess();
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = CompEditorPaletteFactory.create(factory);
        }
        return paletteRoot;
    }

    @Override
    public void commandStackChanged(EventObject event) {
        super.commandStackChanged(event);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GEFDeleteAction deleteAction = new GEFDeleteAction(this);
        getActionRegistry().registerAction(deleteAction);
        getSelectionActions().add(deleteAction.getId());
        SelectionAction action = new GEFCopyAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new GEFPasteAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        for (EConnectionType type : EConnectionType.values()) {
            ConnectionCreateAction createAction = new ConnectionCreateAction(this, type);
            getActionRegistry().registerAction(createAction);
            getSelectionActions().add(createAction.getId());
        }
        ContextMenuProvider menuProvider = new GEFEditorContextMenuProvider(this, viewer, getActionRegistry());
        viewer.setContextMenu(menuProvider);
        getGraphicalViewer().setEditPartFactory(new CompProcessPartFactory());
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(process);
        getGraphicalViewer().addDropTargetListener(new GEFEditorDropTargetListener(this));
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof ProcessEditorInput) {
            ProcessEditorInput processEditorInput = ((ProcessEditorInput) input);
            process = processEditorInput.getProcess();
        }
    }

    public String getProcessTitle() {
        return DenguesCorePlugin.getDefault().getDenguesTitle(((ProcessEditorInput) getEditorInput()).getFile().getFullPath().toPortableString().substring(1));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doSave(IProgressMonitor monitor) {
        monitor.beginTask(Messages.getString("GEFDatabaseEditor.TaskName"), 100);
        monitor.worked(10);
        try {
            ProcessEditorInput processEditorInput = (ProcessEditorInput) getEditorInput();
            processEditorInput.saveEMF(process);
            getCommandStack().markSaveLocation();
            setDirty(false);
            monitor.worked(10);
            if (!process.isBlock()) {
                processEditorInput.getCodeFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            monitor.setCanceled(true);
        } finally {
            monitor.done();
        }
    }

    public String getContributorId() {
        return IDenguesCoreContants.ID_VIEW_PROP;
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) {
            return new DenguesEditorTabbedPropertySheetPage(this);
        } else if (type == IContentOutlinePage.class) {
            return new ProcessEditorOutlinePage(new TreeViewer());
        }
        return super.getAdapter(type);
    }

    /**
     * Getter for process.
     * 
     * @return the process
     */
    public CompProcess getProcess() {
        return this.process;
    }

    public IPath getPath() {
        return ((ProcessEditorInput) getEditorInput()).getPath();
    }
}
