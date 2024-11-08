package net.sf.vgap4.assistant.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import es.ftgroup.gef.model.AbstractPropertyChanger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import net.sf.vgap4.assistant.actions.ImportTurnDataAction;
import net.sf.vgap4.assistant.actions.ZoomMinusDataCommand;
import net.sf.vgap4.assistant.actions.ZoomPlusDataCommand;
import net.sf.vgap4.assistant.factories.AssistantEditPartFactory;
import net.sf.vgap4.assistant.factories.AssistantFigureFactory;
import net.sf.vgap4.assistant.models.AssistantMap;
import net.sf.vgap4.assistant.ui.Activator;
import net.sf.vgap4.projecteditor.editparts.VGAPScalableFreeformRootEditPart;

public class MainMapPage extends AbstractVGAP4Page implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger("net.sf.vgap4.assistant.editor");

    /**
	 * Reference to the <code>MultiPageEditor</code> that is the root element for the map presentation. This
	 * reference allows access to the model and other Editor structures.
	 */
    private final AssistantMapEditor editor;

    public MainMapPage(final AssistantMapEditor mapViewer) {
        editor = mapViewer;
        this.setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    public void dispose() {
        ((AbstractPropertyChanger) this.getMapModel()).removePropertyChangeListener(this);
        super.dispose();
    }

    /**
	 * Delegate this operations to the parent editor that is a MultiPageEditor and that keeps all i/o
	 * references.
	 */
    @Override
    public final void doSave(final IProgressMonitor monitor) {
        editor.doSave(monitor);
    }

    /**
	 * Delegate this operations to the parent editor that is a MultiPageEditor and that keeps all i/o
	 * references.
	 */
    @Override
    public final void doSaveAs() {
        editor.doSaveAs();
    }

    public AssistantMap getMapModel() {
        return editor.getModel();
    }

    public String getPageName() {
        return "MAIN MAP " + this.getMapModel().getZoomFactor() * 10 + " %";
    }

    @Override
    public String getTitle() {
        return "Present and Display the complete known Map";
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        this.setSite(site);
        this.setInput(input);
    }

    /**
	 * Delegate this operations to the parent editor that is a MultiPageEditor and that keeps all i/o
	 * references.
	 */
    @Override
    public final boolean isDirty() {
        return editor.isDirty();
    }

    /**
	 * Delegate this operations to the parent editor that is a MultiPageEditor and that keeps all i/o
	 * references.
	 */
    @Override
    public final boolean isSaveAsAllowed() {
        return editor.isSaveAsAllowed();
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        final String prop = evt.getPropertyName();
        if (AssistantMap.CHANGE_ZOOMFACTOR.equals(prop)) {
            this.setPartName(this.getPageName());
            this.setContentDescription(this.getPageName());
            this.setTitleToolTip(this.getPageName());
            return;
        }
    }

    public void setDirty(final boolean dirty) {
        editor.setDirty(dirty);
    }

    private void configureContextMenu(final GraphicalViewer viewer) {
        final ContextMenuProvider cmProvider = new VGAP4MapViewerContextMenuProvider(viewer, this.getActionRegistry());
        ((VGAP4MapViewerContextMenuProvider) cmProvider).registerAction(new ImportTurnDataAction(this));
        ((VGAP4MapViewerContextMenuProvider) cmProvider).registerAction(new WorkbenchPartAction(this) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void run() {
                super.run();
                this.execute(new ZoomPlusDataCommand("Zoom +", MainMapPage.this.getMapModel()));
            }

            @Override
            protected boolean calculateEnabled() {
                return true;
            }

            @Override
            protected void init() {
                super.init();
                this.setId("ZoomPlus");
                this.setDescription("Make the Map bigger with positive zoom.");
                this.setEnabled(true);
                this.setLazyEnablementCalculation(false);
                this.setImageDescriptor(Activator.getImageDescriptor("icons/zoomplus_on.gif"));
                this.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/zoomplus.gif"));
                this.setText("Zoom +");
                this.setToolTipText("Make zoom to Map.");
            }
        });
        ((VGAP4MapViewerContextMenuProvider) cmProvider).registerAction(new WorkbenchPartAction(this) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void run() {
                super.run();
                this.execute(new ZoomMinusDataCommand("Zoom -", MainMapPage.this.getMapModel()));
            }

            @Override
            protected boolean calculateEnabled() {
                return true;
            }

            @Override
            protected void init() {
                super.init();
                this.setId("ZoomMinus");
                this.setDescription("Make the Map smaller with positive zoom.");
                this.setEnabled(true);
                this.setLazyEnablementCalculation(false);
                this.setImageDescriptor(Activator.getImageDescriptor("icons/zoomminus_on.gif"));
                this.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/zoomminus.gif"));
                this.setText("Zoom -");
                this.setToolTipText("Reduce zoom to Map.");
            }
        });
        ((VGAP4MapViewerContextMenuProvider) cmProvider).registerAction(new WorkbenchPartAction(this) {

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void run() {
                super.run();
                this.execute(new ZoomMinusDataCommand("Zoom 	Reset", MainMapPage.this.getMapModel()));
            }

            @Override
            protected boolean calculateEnabled() {
                return true;
            }

            @Override
            protected void init() {
                super.init();
                this.setId(VGAP4MapViewerContextMenuProvider.ZOOMRESET_ACTION);
                this.setDescription("Reset the zoom to the 100% factor.");
                this.setEnabled(true);
                this.setLazyEnablementCalculation(false);
                this.setImageDescriptor(Activator.getImageDescriptor("icons/zoom_on.gif"));
                this.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/zoom.gif"));
                this.setText("Zoom Reset");
                this.setToolTipText("Reset the zoom to the 100% factor.");
            }
        });
        viewer.setContextMenu(cmProvider);
        this.getSite().registerContextMenu(cmProvider, viewer);
    }

    /**
	 * Create a transfer drop target listener. When using a CombinedTemplateCreationEntry tool in the palette,
	 * this will enable model element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(this.getGraphicalViewer()) {

            @Override
            protected CreationFactory getFactory(final Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    /**
	 * Configure the graphical viewer before it receives contents.
	 * <p>
	 * This is the place to choose an appropriate RootEditPart and EditPartFactory for your editor. The
	 * RootEditPart determines the behavior of the editor's "work-area". For example, GEF includes zoomable and
	 * scrollable root edit parts. The EditPartFactory maps model elements to edit parts (controllers).
	 * </p>
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        final GraphicalViewer viewer = this.getGraphicalViewer();
        viewer.setEditPartFactory(new AssistantEditPartFactory(new AssistantFigureFactory()));
        viewer.setRootEditPart(new VGAPScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        this.configureContextMenu(viewer);
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        return editor.getPaletteRoot();
    }

    /**
	 * Set up the editor's initial content (after creation).
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
	 */
    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        final GraphicalViewer viewer = this.getGraphicalViewer();
        viewer.setContents(this.getMapModel());
        ((AbstractPropertyChanger) this.getMapModel()).addPropertyChangeListener(this);
        viewer.addDropTargetListener(this.createTransferDropTargetListener());
        this.getCommandStack().markSaveLocation();
    }
}
