package vehikel.ide.views.recorder;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import vehikel.datamodel.TopLevelModel;
import vehikel.schema.IOType;

public class RecorderViewPart extends ViewPart {

    public static final String ID = "vehikel.ide.views.recorder.Recorder";

    private ScrollingGraphicalViewer viewer;

    private final EditDomain editDomain = new EditDomain();

    private Action removeChannelAction;

    public RecorderViewPart() {
    }

    @Override
    public void createPartControl(Composite parent) {
        createGraphicalViewer(parent);
        makeActions();
        hookContextMenu();
        contributeToActionBars();
    }

    protected void createGraphicalViewer(Composite parent) {
        final RulerComposite rc = new RulerComposite(parent, SWT.NONE);
        viewer = new ScrollingGraphicalViewer();
        viewer.createControl(rc);
        editDomain.addViewer(viewer);
        rc.setGraphicalViewer(viewer);
        viewer.getControl().setBackground(ColorConstants.white);
        viewer.setEditPartFactory(new EditPartFactory() {

            public EditPart createEditPart(EditPart context, Object model) {
                return new RecorderEditPart(TopLevelModel.getRecorderModel());
            }
        });
        viewer.setContents(TopLevelModel.getRecorderModel());
        Control control = viewer.getControl();
        System.out.println("widget: " + control);
        DropTarget dt = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        dt.setTransfer(new Transfer[] { TextTransfer.getInstance() });
        dt.addDropListener(new SensorTransferDropTargetListener(viewer));
    }

    @Override
    public void setFocus() {
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == GraphicalViewer.class) return viewer;
        if (type == CommandStack.class) return editDomain.getCommandStack();
        if (type == EditPart.class && viewer != null) return viewer.getRootEditPart();
        if (type == IFigure.class && viewer != null) return ((GraphicalEditPart) viewer.getRootEditPart()).getFigure();
        if (type == RecorderEditPart.class && viewer != null) return viewer.getContents();
        return super.getAdapter(type);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                RecorderViewPart.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(removeChannelAction);
        manager.add(new Separator());
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(removeChannelAction);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(removeChannelAction);
    }

    private void makeActions() {
        removeChannelAction = new Action() {

            @Override
            public void run() {
                ((RecorderEditPart) viewer.getContents()).removeAllChannels();
            }
        };
        removeChannelAction.setText("Remove all channels");
        removeChannelAction.setToolTipText("Remove all channels from recorder.");
        removeChannelAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
    }

    class SensorTransferDropTargetListener extends AbstractTransferDropTargetListener {

        private final EditPartViewer viewer;

        public SensorTransferDropTargetListener(EditPartViewer viewer) {
            super(viewer, TextTransfer.getInstance());
            this.viewer = viewer;
        }

        @Override
        protected void updateTargetRequest() {
        }

        @Override
        public void drop(DropTargetEvent event) {
            System.out.println("drop" + event);
            ((RecorderEditPart) viewer.getContents()).addChannel((IOType) event.data);
        }
    }
}
