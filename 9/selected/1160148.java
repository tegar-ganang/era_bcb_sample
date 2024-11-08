package net.sf.jleonardo.v2d.editors;

import java.util.EventObject;
import net.sf.jleonardo.core.BusinessObject;
import net.sf.jleonardo.v2d.palette.View2DPaletteFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.dnd.TransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.PropertySheetPage;

public class View2DEditor extends GraphicalEditorWithFlyoutPalette {

    public static final String ID = "net.sf.jleonardo.v2d.editors.view2D";

    private PropertySheetPage _propertySheetPage;

    private ScalableFreeformRootEditPart _root;

    private PaletteRoot PALETTE_MODEL;

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    public void createPartControl(Composite parent) {
        Composite c = new Composite(parent, SWT.None);
        c.setLayout(new GridLayout(1, true));
        CoolBar tb = new CoolBar(c, SWT.HORIZONTAL);
        CoolItem ti1 = new CoolItem(tb, SWT.NONE);
        Button button = new Button(tb, SWT.PUSH);
        button.setText("Button 1");
        Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        ti1.setPreferredSize(ti1.computeSize(size.x, size.y));
        ti1.setControl(button);
        CoolItem ti2 = new CoolItem(tb, SWT.PUSH);
        ti2.setText("Tool item 2");
        CoolItem ti3 = new CoolItem(tb, SWT.PUSH);
        ti3.setText("Tool item 3");
        CoolItem ti4 = new CoolItem(tb, SWT.PUSH);
        ti4.setText("Tool item 4");
        Composite composite = new Composite(c, SWT.None);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout());
        super.createPartControl(composite);
    }

    /**
	 * 
	 */
    public View2DEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    /**
	 * @param parent
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        final GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new View2DPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ContextMenuProvider cmProvider = new View2DContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                viewer.addSelectionChangedListener(new ISelectionChangedListener() {

                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                        if (!sel.isEmpty()) System.out.println(sel.getFirstElement());
                    }
                });
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    /**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
    @Override
    public void doSaveAs() {
    }

    /**
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        return super.getAdapter(adapter);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPaletteRoot()
	 */
    @Override
    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = View2DPaletteFactory.createPaletteRoot();
        return PALETTE_MODEL;
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    /**
	 * @return
	 */
    private BusinessObject getModel() {
        return ((View2DEditorInput) getEditorInput()).getData();
    }
}
