package com.ibm.realtime.flexotask.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import com.ibm.realtime.flexotask.tools.FlexotaskXMLParser;
import com.ibm.realtime.flexotask.editor.model.GlobalTiming;
import com.ibm.realtime.flexotask.editor.model.TaskDiagram;
import com.ibm.realtime.flexotask.editor.parts.ShapesEditPartFactory;

/**
 * A graphical editor with flyout palette that can edit .shapes files.
 * The binding between the .ftg file extension and this editor is done in plugin.xml
 */
public class FlexotaskEditor extends GraphicalEditorWithFlyoutPalette {

    /** Palette component, holding the tools and shapes. */
    private static PaletteRoot PALETTE_MODEL;

    /** This is the root of the editor's model. */
    private TaskDiagram diagram;

    /** Create a new FlexotaskEditor instance. This is called by the Workspace. */
    public FlexotaskEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    public void doSave(IProgressMonitor monitor) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            createOutputStream(out);
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor);
            getCommandStack().markSaveLocation();
        } catch (CoreException ce) {
            ce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

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

                    public void execute(final IProgressMonitor monitor) {
                        try {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            createOutputStream(out);
                            file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
                        } catch (CoreException ce) {
                            ce.printStackTrace();
                        } catch (IOException ioe) {
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

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) {
            PropertySheetPage page = new NonSortingPropertySheetPage();
            page.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
            return page;
        }
        return super.getAdapter(type);
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        try {
            IFile file = ((IFileEditorInput) input).getFile();
            InputStream contents = file.getContents();
            if (contents.available() == 0) {
                diagram = new TaskDiagram();
            } else {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Element top = factory.newDocumentBuilder().parse(new InputSource(contents)).getDocumentElement();
                if ("FlexotaskEditSession".equals(top.getTagName())) {
                    diagram = new TaskDiagram(top);
                } else {
                    diagram = new TaskDiagram(FlexotaskXMLParser.parse(top, GlobalTiming.getParsers()));
                }
            }
            diagram.setResource(file.getParent());
            setPartName(file.getName());
        } catch (Exception e) {
            throw new PartInitException(e.toString(), e);
        }
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
   * Configure the graphical viewer before it receives contents.
   * <p>This is the place to choose an appropriate RootEditPart and EditPartFactory
   * for your editor. The RootEditPart determines the behavior of the editor's "work-area".
   * For example, GEF includes zoomable and scrollable root edit parts. The EditPartFactory
   * maps model elements to edit parts (controllers).</p>
   * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
   */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ShapesEditPartFactory());
        viewer.setRootEditPart(new ShapesOnTopRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ContextMenuProvider cmProvider = new EEditContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
        openPropertyView();
    }

    protected void createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = new ScrollingGraphicalViewer() {

            protected LightweightSystem createLightweightSystem() {
                LightweightSystem lws = super.createLightweightSystem();
                lws.setUpdateManager(new CautiousUpdateManager());
                return lws;
            }
        };
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = EEditPaletteFactory.createPalette();
        return PALETTE_MODEL;
    }

    /**
   * Set up the editor's inital content (after creation).
   * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
   */
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    TaskDiagram getModel() {
        return diagram;
    }

    /**
   * Given an OutputStream, create an image of what is to be saved in it
   * @param os the OutputStream to use
   * @throws IOException if anything goes wrong
   */
    private void createOutputStream(OutputStream os) throws IOException {
        PrintWriter wtr = new PrintWriter(os);
        wtr.println(getModel().toXML());
        wtr.close();
    }

    /**
   * Create a transfer drop target listener. When using a CombinedTemplateCreationEntry
   * tool in the palette, this will enable model element creation by dragging from the palette.
   * @see #createPaletteViewerProvider()
   */
    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            @SuppressWarnings("unchecked")
            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    /**
   * Open the property view if it is currently hidden
   */
    public static void openPropertyView() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                try {
                    page.showView("org.eclipse.ui.views.PropertySheet");
                } catch (PartInitException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
   * Open the existing class in a JDT editor
   */
    public static void openSource(IType toOpen) {
        try {
            JavaUI.openInEditor(toOpen);
        } catch (PartInitException e) {
        } catch (JavaModelException e) {
        }
    }

    /**
   * The name says it all
   */
    private static class DoNothingSorter extends PropertySheetSorter {

        public void sort(IPropertySheetEntry[] entries) {
        }
    }

    /**
   * Extension to PropertySheetPage that doesn't sort
   */
    private static class NonSortingPropertySheetPage extends PropertySheetPage {

        public NonSortingPropertySheetPage() {
            super();
            setSorter(new DoNothingSorter());
        }
    }
}
