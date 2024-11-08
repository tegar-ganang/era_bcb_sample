package org.henkels.drawcode.editors.nsdiagram;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.henkels.drawcode.editors.nsdiagram.graphics.Visitors.LayoutCalculatorVisitor;
import org.henkels.drawcode.editors.nsdiagram.model.NSDiagramHandler;
import org.henkels.drawcode.editors.nsdiagram.model.nodes.ICommandVisitable;
import org.henkels.drawcode.editors.nsdiagram.parts.NSPartFactory;
import org.henkels.drawcode.plugin.DrawCodePlugin;

public class NSDiagramEditor extends GraphicalEditorWithFlyoutPalette {

    IFile file;

    private static PaletteRoot palette;

    private NSDiagramHandler nsDiagram = new NSDiagramHandler();

    public NSDiagramEditor() {
        super();
        System.out.println("NSDiagramEditor.NSDiagramEditor");
        setEditDomain(new DefaultEditDomain(this));
        getCommandStack().setUndoLimit(-1);
    }

    public void commandStackChanged(EventObject event) {
        System.out.println("NSDiagramEditor.commandStackChanged");
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    protected FlyoutPreferences getPalettePreferences() {
        System.out.println("NSDiagramEditor.getPalettePreferences");
        return NSDiagramEditorPaletteFactory.createPalettePreferences();
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        System.out.println("NSDiagramEditor.getPaletteRoot");
        if (palette == null) {
            palette = NSDiagramEditorPaletteFactory.createPalette();
        }
        return palette;
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        System.out.println("NSDiagramEditor.getPaletteRoot");
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(DrawCodePlugin.getDefault().getActiveNSDiagram().root);
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    private org.eclipse.jface.util.TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        System.out.println("NSDiagramEditor.configureGraphicalViewer");
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new NSPartFactory(this));
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        GraphicalViewerKeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer);
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        viewer.setKeyHandler(keyHandler);
        ContextMenuProvider cmProvider = new NSDiagramEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        System.out.println("NSDiagramEditor.createPaletteViewerProvider");
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    public void setInput(IEditorInput input) {
        super.setInput(input);
        System.out.println("NSDiagramEditor.setInput");
        try {
            file = ((IFileEditorInput) input).getFile();
            DrawCodePlugin.getDefault().addOpenedNSDiagram(nsDiagram);
            DrawCodePlugin.getDefault().setActiveNSDiagram(nsDiagram);
            nsDiagram.ReadDiagram(file.getRawLocation().toString());
            setPartName(file.getName());
        } catch (Exception e) {
            Shell shell = getEditorSite().getShell();
            System.out.println("Error opening file.");
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        System.out.println("NSDiagramEditor.doSave");
        try {
            nsDiagram.SaveDiagram(file.getRawLocation().toString());
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<ICommandVisitable, Rectangle> rectmap = null;

    public Rectangle getNodeRect(ICommandVisitable node) {
        if (rectmap == null) {
            rectmap = new LayoutCalculatorVisitor().getLayout(nsDiagram.root);
        }
        Rectangle ret = rectmap.get(node);
        if (ret == null) {
            rectmap = new LayoutCalculatorVisitor().getLayout(nsDiagram.root);
            ret = rectmap.get(node);
        }
        return ret;
    }

    public List<ICommandVisitable> getChildrens(ICommandVisitable node) {
        System.out.println("NSDiagramEditort.getChildrens");
        return nsDiagram.getChildrens(node);
    }

    public NSDiagramHandler getNSDHandler() {
        return nsDiagram;
    }

    public void RefreshVisual() {
        nsDiagram.rooteditpart.removeChildrens();
        rectmap = null;
        nsDiagram.rooteditpart.refreshChildren();
    }
}
