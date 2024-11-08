package com.ivis.xprocess.ui.processdesigner.diagram.dialogs;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.properties.ProcessDesignerMessages;
import com.ivis.xprocess.ui.util.FontAndColorManager;

public class OverviewDialog extends Window implements MouseListener, MouseMoveListener {

    private static final int TRACKER_WIDTH = 3;

    private static final int TRACKER_DELTA = 25;

    private static Image ourDockImage = UIType.overview_dock.image;

    private static Image ourCloseImage = UIType.overview_close.image;

    private Canvas myCanvas;

    private ScrollableThumbnail myThumbnail;

    private ControlAdapter myShellMoveListener;

    private ControlAdapter myParentResizeListener;

    private Shell myDockPlaceShell;

    private Region myDockRegion;

    private boolean myIsDragStarted = false;

    private Point myDragStart;

    private FocusAdapter myFocusAdapter;

    private boolean myIsDockable = true;

    private ControlAdapter myResizeListener;

    private GraphicalViewer myGraphicalViewer;

    private boolean myOverviewVisible;

    private Point myCloseLocation;

    private Point myCloseSize;

    public OverviewDialog(GraphicalViewer viewer) {
        super(viewer.getControl().getShell());
        myGraphicalViewer = viewer;
        setShellStyle(SWT.RESIZE);
        myOverviewVisible = false;
        myFocusAdapter = new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                mouseUp(null);
            }
        };
    }

    public boolean isOpened() {
        return myThumbnail != null;
    }

    public void setInput(GraphicalViewer graphicalViewer) {
        myGraphicalViewer = graphicalViewer;
        ScalableFreeformRootEditPart root = getRootEditPart();
        if ((root != null) && (myThumbnail != null)) {
            myThumbnail.setViewport((Viewport) root.getFigure());
            myThumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
        }
    }

    public boolean isVisible() {
        return myOverviewVisible;
    }

    @Override
    public int open() {
        myOverviewVisible = true;
        return super.open();
    }

    @Override
    public boolean close() {
        if (myThumbnail != null) {
            myThumbnail.deactivate();
            myThumbnail = null;
        }
        if ((myGraphicalViewer.getControl() != null) && !myGraphicalViewer.getControl().isDisposed()) {
            myGraphicalViewer.getControl().getShell().removeControlListener(myShellMoveListener);
            myGraphicalViewer.getControl().removeControlListener(myParentResizeListener);
        }
        myCanvas.removeControlListener(myResizeListener);
        myCanvas.removeFocusListener(myFocusAdapter);
        hideTracker();
        myCloseLocation = getShell().getLocation();
        myCloseSize = getShell().getSize();
        return super.close();
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        if (myCloseLocation != null) {
            return myCloseLocation;
        }
        return getDockableLocation(initialSize);
    }

    @Override
    protected Point getInitialSize() {
        if (myCloseSize != null) {
            return myCloseSize;
        }
        org.eclipse.draw2d.geometry.Rectangle viewportBounds = getRootEditPart().getFigure().getBounds();
        Point size = new Point(viewportBounds.width / 3, viewportBounds.height / 3);
        return size;
    }

    @Override
    protected Control createContents(Composite parent) {
        final Color titleColor = ColorConstants.lightBlue;
        GridLayout windowLayout = new GridLayout();
        windowLayout.marginHeight = 0;
        windowLayout.marginWidth = 0;
        windowLayout.verticalSpacing = 1;
        parent.setLayout(windowLayout);
        final Composite titleComposite = new Composite(parent, SWT.NONE);
        GridLayout titleLayout = new GridLayout(2, false);
        titleLayout.marginHeight = 0;
        titleLayout.marginWidth = 1;
        titleLayout.verticalSpacing = 0;
        GridData titleGridData = new GridData(GridData.FILL_HORIZONTAL);
        titleGridData.heightHint = 16;
        titleComposite.setLayout(titleLayout);
        titleComposite.setLayoutData(titleGridData);
        titleComposite.setBackground(titleColor);
        titleComposite.addMouseListener(this);
        titleComposite.addMouseMoveListener(this);
        Label title = new Label(titleComposite, SWT.NONE);
        title.setBackground(titleColor);
        title.setForeground(ColorConstants.white);
        title.setText(ProcessDesignerMessages.OverviewDialog_Title);
        title.setAlignment(SWT.CENTER);
        GridData labelGridData = new GridData();
        labelGridData.horizontalIndent = 3;
        title.setLayoutData(labelGridData);
        title.addMouseMoveListener(this);
        title.addMouseListener(this);
        FontData fontData = title.getFont().getFontData()[0];
        FontData newFontData = new FontData(fontData.getName(), 8, fontData.getStyle() | SWT.BOLD);
        title.setFont(FontAndColorManager.getInstance().getFont(newFontData));
        Composite buttonsComposite = new Composite(titleComposite, SWT.NONE);
        GridData buttonsGridData = new GridData(SWT.END, SWT.CENTER, true, true);
        buttonsGridData.heightHint = 12;
        buttonsGridData.widthHint = 26;
        buttonsComposite.setLayoutData(buttonsGridData);
        buttonsComposite.setBackground(titleColor);
        buttonsComposite.addMouseMoveListener(this);
        buttonsComposite.addMouseListener(this);
        GridLayout buttonsLayout = new GridLayout(2, true);
        buttonsLayout.marginHeight = 0;
        buttonsLayout.marginWidth = 0;
        buttonsLayout.horizontalSpacing = 2;
        buttonsComposite.setLayout(buttonsLayout);
        Label dockButton = new Label(buttonsComposite, SWT.NONE);
        GridData dockGridData = new GridData(SWT.BEGINNING, SWT.CENTER, true, true);
        dockGridData.heightHint = 12;
        dockGridData.widthHint = 12;
        dockButton.setLayoutData(dockGridData);
        dockButton.setImage(ourDockImage);
        dockButton.setBackground(titleColor);
        dockButton.setCursor(Cursors.HAND);
        Label closeButton = new Label(buttonsComposite, SWT.NONE);
        GridData closeGridData = new GridData(SWT.END, SWT.CENTER, true, false);
        closeGridData.heightHint = 12;
        closeGridData.widthHint = 12;
        closeButton.setLayoutData(closeGridData);
        closeButton.setImage(ourCloseImage);
        closeButton.setBackground(titleColor);
        closeButton.setCursor(Cursors.HAND);
        dockButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseUp(MouseEvent e) {
                moveToDockLocation();
            }
        });
        closeButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseUp(MouseEvent e) {
                myOverviewVisible = false;
                close();
            }
        });
        myCanvas = new Canvas(parent, SWT.NONE);
        myCanvas.setLayoutData(new GridData(GridData.FILL_BOTH));
        myCanvas.setBackground(ColorConstants.white);
        initializeOverview();
        myShellMoveListener = new ControlAdapter() {

            @Override
            public void controlMoved(ControlEvent e) {
                if (myIsDockable) {
                    moveToDockLocation();
                }
            }
        };
        myGraphicalViewer.getControl().getShell().addControlListener(myShellMoveListener);
        myParentResizeListener = new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                if (myIsDockable) {
                    moveToDockLocation();
                }
            }
        };
        myGraphicalViewer.getControl().addControlListener(myParentResizeListener);
        myResizeListener = new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                if (!getShell().getLocation().equals(getDockableLocation(getShell().getSize()))) {
                    myIsDockable = false;
                }
            }
        };
        myCanvas.addControlListener(myResizeListener);
        myCanvas.addFocusListener(myFocusAdapter);
        return myCanvas;
    }

    public Control getContents() {
        return myCanvas;
    }

    public void mouseDoubleClick(MouseEvent e) {
    }

    public void mouseDown(MouseEvent e) {
        if (e.button == 1) {
            setDragStarted(true);
            myDragStart = new Point(e.x, e.y);
        }
    }

    public void mouseUp(MouseEvent e) {
        if (isDragStarted()) {
            setDragStarted(false);
            if (computeDistance() < TRACKER_DELTA) {
                moveToDockLocation();
            }
            hideTracker();
        }
    }

    public void mouseMove(MouseEvent e) {
        if (isDragStarted()) {
            myIsDockable = false;
            Point location = getShell().getLocation();
            int deltaX = e.x - myDragStart.x;
            int deltaY = e.y - myDragStart.y;
            location.x += deltaX;
            location.y += deltaY;
            getShell().setLocation(location);
            if (computeDistance() < TRACKER_DELTA) {
                showTracker();
            } else {
                hideTracker();
            }
        }
    }

    private void initializeOverview() {
        LightweightSystem lws = new LightweightSystem(myCanvas);
        ScalableFreeformRootEditPart root = getRootEditPart();
        if (root != null) {
            myThumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
            myThumbnail.setBorder(new MarginBorder(0));
            myThumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
            lws.setContents(myThumbnail);
        }
    }

    private ScalableFreeformRootEditPart getRootEditPart() {
        RootEditPart rep = myGraphicalViewer.getRootEditPart();
        if (rep instanceof ScalableFreeformRootEditPart) {
            return (ScalableFreeformRootEditPart) rep;
        }
        return null;
    }

    private void moveToDockLocation() {
        Shell shell = getShell();
        Point dockableLocation = getDockableLocation(shell.getSize());
        shell.setLocation(dockableLocation);
        myIsDockable = true;
    }

    private Point getDockableLocation(Point size) {
        Control control = myGraphicalViewer.getControl();
        Rectangle controlBounds = control.getBounds();
        controlBounds = Geometry.toDisplay(control.getParent(), controlBounds);
        org.eclipse.draw2d.geometry.Rectangle viewportBounds = getRootEditPart().getFigure().getBounds();
        int x = (controlBounds.x + viewportBounds.width) - size.x;
        int y = (controlBounds.y + viewportBounds.height) - size.y;
        return new Point(x, y);
    }

    private void showTracker() {
        if (myDockPlaceShell != null) {
            return;
        }
        myCanvas.removeFocusListener(myFocusAdapter);
        myDockPlaceShell = new Shell(SWT.NO_TRIM | SWT.ON_TOP | SWT.NO_FOCUS);
        myDockPlaceShell.setBackground(ColorConstants.lightGray);
        myDockPlaceShell.addFocusListener(new FocusAdapter() {

            public void focusGained(FocusEvent e) {
                myCanvas.addFocusListener(myFocusAdapter);
                myCanvas.setFocus();
            }
        });
        Point dockableLocation = getDockableLocation(getShell().getSize());
        Rectangle rect = new Rectangle(dockableLocation.x, dockableLocation.y, dockableLocation.x + getShell().getSize().x, dockableLocation.y + getShell().getSize().y);
        Rectangle innerRect = new Rectangle(rect.x + TRACKER_WIDTH, rect.y + TRACKER_WIDTH, rect.width - rect.x - (TRACKER_WIDTH * 2), rect.height - rect.y - (TRACKER_WIDTH * 2));
        myDockRegion = new Region();
        myDockRegion.add(rect);
        myDockRegion.subtract(innerRect);
        myDockPlaceShell.setRegion(myDockRegion);
        myDockPlaceShell.setSize(myDockRegion.getBounds().width, myDockRegion.getBounds().height);
        myDockPlaceShell.setLocation(1, 1);
        myDockPlaceShell.open();
    }

    private void hideTracker() {
        if (myDockPlaceShell == null) {
            return;
        }
        myDockRegion.dispose();
        myDockPlaceShell.close();
        myDockPlaceShell = null;
    }

    private int computeDistance() {
        Point location = getShell().getLocation();
        Point dockableLocation = getDockableLocation(getShell().getSize());
        int distanceX = location.x - dockableLocation.x;
        int distanceY = location.y - dockableLocation.y;
        return (int) Math.sqrt(((distanceX * distanceX) + (distanceY * distanceY)));
    }

    private boolean isDragStarted() {
        return myIsDragStarted;
    }

    private void setDragStarted(boolean isDrag) {
        myIsDragStarted = isDrag;
    }
}
