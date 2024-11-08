package de.fu_berlin.inf.dpp.whiteboard.gef.editor;

import java.util.ArrayList;
import org.apache.batik.util.SVGConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SharedImages;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.CreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import de.fu_berlin.inf.dpp.whiteboard.gef.actions.CopyRecordAction;
import de.fu_berlin.inf.dpp.whiteboard.gef.actions.PasteRecordAction;
import de.fu_berlin.inf.dpp.whiteboard.gef.actions.SXEDeleteAction;
import de.fu_berlin.inf.dpp.whiteboard.gef.part.RecordPartFactory;
import de.fu_berlin.inf.dpp.whiteboard.gef.tools.CreationToolWithoutSelection;
import de.fu_berlin.inf.dpp.whiteboard.gef.tools.PanningTool.PanningToolEntry;
import de.fu_berlin.inf.dpp.whiteboard.gef.tools.PointlistCreationTool;
import de.fu_berlin.inf.dpp.whiteboard.gef.util.IconUtils;
import de.fu_berlin.inf.dpp.whiteboard.net.WhiteboardManager;
import de.fu_berlin.inf.dpp.whiteboard.standalone.WhiteboardContextMenuProvider;
import de.fu_berlin.inf.dpp.whiteboard.sxe.ISXEMessageHandler.MessageAdapter;
import de.fu_berlin.inf.dpp.whiteboard.sxe.ISXEMessageHandler.NotificationListener;
import de.fu_berlin.inf.dpp.whiteboard.sxe.net.SXEMessage;
import de.fu_berlin.inf.dpp.whiteboard.sxe.records.ElementRecord;

/**
 * <p>
 * The editor creates the GUI using the GEF API and initializes to listen to the
 * WhiteboardManager.
 * </p>
 * 
 * @author jurke
 * 
 */
public class WhiteboardEditor extends SarosPermissionsGraphicalEditor {

    public static final String ID = "de.fu_berlin.inf.dpp.whiteboard.whiteboardeditor";

    private KeyHandler keyHandler;

    /**
	 * Creates the editor with a custom command stack
	 * 
	 * @see SXECommandStack
	 */
    public WhiteboardEditor() {
        DefaultEditDomain editDomain = new DefaultEditDomain(this);
        editDomain.setCommandStack(new SXECommandStack());
        setEditDomain(editDomain);
    }

    /**
	 * Initializes the graphical viewer with the root element, from now it
	 * listens to applied remote records to update action enablement and to
	 * document root changes to update the root.
	 */
    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        WhiteboardManager.getInstance().getSXEMessageHandler().addMessageListener(new MessageAdapter() {

            @Override
            public void sxeStateMessageApplied(SXEMessage message, ElementRecord root) {
                updateViewerContents(root);
            }
        });
        viewer.setContents(WhiteboardManager.getInstance().getSXEMessageHandler().getDocumentRecord().getRoot());
        viewer.addDropTargetListener(new TemplateTransferDropTargetListener(viewer) {

            /**
			 * Overridden by the superclass method because selecting the created
			 * object here does not make sense as it differs from the one that
			 * will be created by the command (and finally by the DocumentRecord
			 * as it should be).
			 */
            @Override
            protected void handleDrop() {
                updateTargetRequest();
                updateTargetEditPart();
                if (getTargetEditPart() != null) {
                    Command command = getCommand();
                    if (command != null && command.canExecute()) getViewer().getEditDomain().getCommandStack().execute(command); else getCurrentEvent().detail = DND.DROP_NONE;
                } else getCurrentEvent().detail = DND.DROP_NONE;
            }
        });
        super.initializeGraphicalViewer();
    }

    protected void updateViewerContents(ElementRecord root) {
        if (root == null) return;
        getGraphicalViewer().setContents(root);
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        final GraphicalViewer viewer = new ScrollingGraphicalViewer() {

            protected boolean isNotifying = false;

            {
                WhiteboardManager.getInstance().getSXEMessageHandler().addNotificationListener(new NotificationListener() {

                    @Override
                    public void beforeNotification() {
                        isNotifying = true;
                    }

                    @Override
                    public void afterNotificaion() {
                        isNotifying = false;
                        fireSelectionChanged();
                        updateActions();
                    }
                });
            }

            @Override
            protected void fireSelectionChanged() {
                if (isNotifying) return;
                super.fireSelectionChanged();
            }
        };
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    @Override
    protected void initializePaletteViewer() {
        super.initializePaletteViewer();
        getPaletteViewer().addDragSourceListener(new TemplateTransferDragSourceListener(getPaletteViewer()));
    }

    @Override
    protected void configureGraphicalViewer() {
        double[] zoomLevels;
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new RecordPartFactory());
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        ZoomManager manager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(manager));
        getActionRegistry().registerAction(new ZoomOutAction(manager));
        zoomLevels = new double[] { 0.1, 0.25, 0.5, 0.75, 1, 1.5, 2.0, 2.5, 3, 4, 5, 10 };
        manager.setZoomLevels(zoomLevels);
        manager.setZoom(1);
        ArrayList<String> zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        manager.setZoomLevelContributions(zoomContributions);
        keyHandler = new KeyHandler() {

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.keyCode == 127) {
                    return performDelete();
                }
                return super.keyPressed(event);
            }

            private boolean performDelete() {
                IAction action = getActionRegistry().getAction(ActionFactory.DELETE.getId());
                if (action == null) return false;
                if (action.isEnabled()) action.run();
                return true;
            }
        };
        ;
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
        keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE), MouseWheelZoomHandler.SINGLETON);
        viewer.setKeyHandler(keyHandler);
        ContextMenuProvider provider = new WhiteboardContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createActions() {
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new UndoAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new RedoAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new SelectAllAction(this) {

            {
                setHoverImageDescriptor(SharedImages.DESC_MARQUEE_TOOL_16);
                setImageDescriptor(SharedImages.DESC_MARQUEE_TOOL_16);
            }
        };
        registry.registerAction(action);
        action = new SXEDeleteAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SaveAction(this);
        registry.registerAction(action);
        getPropertyActions().add(action.getId());
        registry.registerAction(new PrintAction(this));
        action = new CopyRecordAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PasteRecordAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == ZoomManager.class) return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        return super.getAdapter(type);
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        PaletteRoot root = new PaletteRoot();
        PaletteGroup manipGroup = new PaletteGroup("Manipulate elements");
        root.add(manipGroup);
        PanningSelectionToolEntry selectionToolEntry = new PanningSelectionToolEntry();
        manipGroup.add(selectionToolEntry);
        MarqueeToolEntry marqueeToolEntry = new MarqueeToolEntry();
        manipGroup.add(marqueeToolEntry);
        PanningToolEntry panningToolEntry = new PanningToolEntry();
        manipGroup.add(panningToolEntry);
        PaletteSeparator sep2 = new PaletteSeparator();
        root.add(sep2);
        PaletteGroup instGroup = new PaletteGroup("Create elements");
        root.add(instGroup);
        instGroup.add(createPolylineToolEntry());
        instGroup.add(createRectangleToolEntry());
        instGroup.add(createEllipseToolEntry());
        root.setDefaultEntry(selectionToolEntry);
        return root;
    }

    protected static ToolEntry createEllipseToolEntry() {
        CombinedTargetRecordCreationFactory template = new CombinedTargetRecordCreationFactory(SVGConstants.SVG_ELLIPSE_TAG);
        CreationToolEntry entry = new CombinedTemplateCreationEntry("Ellipse", "Creation of an ellipse", template, template, ImageDescriptor.createFromImage(IconUtils.getEllipseImage()), ImageDescriptor.createFromImage(IconUtils.getEllipseImage()));
        entry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        entry.setToolClass(CreationToolWithoutSelection.class);
        return entry;
    }

    protected static ToolEntry createRectangleToolEntry() {
        CombinedTargetRecordCreationFactory template = new CombinedTargetRecordCreationFactory(SVGConstants.SVG_RECT_TAG);
        CreationToolEntry entry = new CombinedTemplateCreationEntry("Rectangle", "Creation of a rectangle", template, template, ImageDescriptor.createFromImage(IconUtils.getRectImage()), ImageDescriptor.createFromImage(IconUtils.getRectImage()));
        entry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        entry.setToolClass(CreationToolWithoutSelection.class);
        return entry;
    }

    protected static ToolEntry createPolylineToolEntry() {
        CombinedTargetRecordCreationFactory template = new CombinedTargetRecordCreationFactory(SVGConstants.SVG_POLYLINE_TAG);
        CreationToolEntry entry = new CombinedTemplateCreationEntry("Pencil", "Free hand drawing", template, template, ImageDescriptor.createFromImage(IconUtils.getPencilImage()), ImageDescriptor.createFromImage(IconUtils.getPencilImage()));
        entry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, false);
        entry.setToolClass(PointlistCreationTool.class);
        return entry;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    public void updateSelectionActions() {
        updateActions(getSelectionActions());
    }
}
