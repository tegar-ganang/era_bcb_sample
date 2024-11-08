package com.ecmdeveloper.plugin.diagrams.editors;

import java.io.IOException;
import java.util.EventObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import com.ecmdeveloper.plugin.core.model.IClassDescription;
import com.ecmdeveloper.plugin.diagrams.actions.AddClassDiagramClassAction;
import com.ecmdeveloper.plugin.diagrams.actions.ExportDiagramAction;
import com.ecmdeveloper.plugin.diagrams.actions.ExportDiagramClassAction;
import com.ecmdeveloper.plugin.diagrams.actions.RefreshDiagramClassAction;
import com.ecmdeveloper.plugin.diagrams.actions.ShowPropertiesAction;
import com.ecmdeveloper.plugin.diagrams.model.ClassDiagram;
import com.ecmdeveloper.plugin.diagrams.model.ClassDiagramFile;
import com.ecmdeveloper.plugin.diagrams.parts.ClassesEditPartFactory;
import com.ecmdeveloper.plugin.diagrams.util.PluginMessage;

/**
 * @author Ricardo Belfor
 *
 */
public class ClassDiagramEditor extends GraphicalEditorWithFlyoutPalette {

    private static final String FILE_READ_MESSAGE = "Reading Class Diagram File failed.";

    private static final String CLASS_DIAGRAM_EDITOR_NAME = "Class Diagram Editor";

    private ClassDiagram model;

    public static final String ID = "com.ecmdeveloper.plugin.diagrams.classDiagramEditor";

    public ClassDiagramEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    public ClassDiagram getClassDiagram() {
        return model;
    }

    public GraphicalViewer getViewer() {
        return getGraphicalViewer();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = configureViewer();
        configureContextMenu(viewer);
    }

    private GraphicalViewer configureViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ClassesEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        return viewer;
    }

    private void configureContextMenu(GraphicalViewer viewer) {
        MenuManager menuManager = new ClassDiagramContextMenuManager(getActionRegistry());
        viewer.setContextMenu(menuManager);
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(model);
        getGraphicalViewer().addDropTargetListener((TransferDropTargetListener) new ClassDiagramDropTargetListener(getGraphicalViewer()));
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        ClassDiagramFile classDiagramFile = new ClassDiagramFile(file);
        try {
            model = classDiagramFile.read();
            setPartName(file.getName());
            setTitleToolTip(file.getLocation().toString());
        } catch (Exception e) {
            PluginMessage.openError(getSite().getShell(), CLASS_DIAGRAM_EDITOR_NAME, FILE_READ_MESSAGE, e);
        }
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        return ClassDiagramEditorPaletteFactory.createPalette();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        ClassDiagramFile classDiagramFile = new ClassDiagramFile(file);
        try {
            classDiagramFile.save(model, monitor);
            getCommandStack().markSaveLocation();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    @SuppressWarnings("deprecation")
    public void deleteSelection() {
        getActionRegistry().getAction(DeleteAction.ID).run();
    }

    public void addClassDiagramClass(IClassDescription classDescription) {
        AddClassDiagramClassAction action = (AddClassDiagramClassAction) getActionRegistry().getAction(AddClassDiagramClassAction.ID);
        action.setClassDescription(classDescription);
        action.run();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == ClassDiagram.class) return model;
        return super.getAdapter(type);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    @Override
    protected void createActions() {
        super.createActions();
        registerExportDiagramAction();
        registerPropertiesAction();
        registerExportDiagramClassAction();
        registerPrintAction();
        registerAddClassDiagramClassAction();
        registerRefreshDiagramClassAction();
    }

    @SuppressWarnings("unchecked")
    private void registerPrintAction() {
        IAction printAction = new PrintAction(this);
        getActionRegistry().registerAction(printAction);
        getSelectionActions().add(printAction.getId());
    }

    @SuppressWarnings("unchecked")
    private void registerPropertiesAction() {
        IAction propertiesAction = new ShowPropertiesAction(this);
        getActionRegistry().registerAction(propertiesAction);
        getSelectionActions().add(propertiesAction.getId());
    }

    @SuppressWarnings("unchecked")
    private void registerExportDiagramAction() {
        IAction exportAction = new ExportDiagramAction(this);
        getActionRegistry().registerAction(exportAction);
        getSelectionActions().add(exportAction.getId());
    }

    @SuppressWarnings("unchecked")
    private void registerExportDiagramClassAction() {
        IAction exportAction = new ExportDiagramClassAction(this);
        getActionRegistry().registerAction(exportAction);
        getSelectionActions().add(exportAction.getId());
    }

    @SuppressWarnings("unchecked")
    private void registerAddClassDiagramClassAction() {
        IAction addAction = new AddClassDiagramClassAction(this);
        getActionRegistry().registerAction(addAction);
        getSelectionActions().add(addAction.getId());
    }

    @SuppressWarnings("unchecked")
    private void registerRefreshDiagramClassAction() {
        IAction refreshAction = new RefreshDiagramClassAction(this);
        getActionRegistry().registerAction(refreshAction);
        getSelectionActions().add(refreshAction.getId());
    }
}
