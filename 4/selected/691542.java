package onepoint.express;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import onepoint.express.util.XConstants;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.service.XClient;
import onepoint.service.XMessage;
import onepoint.util.XCache;
import onepoint.util.XCalendar;

public class XDisplay extends XView {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    public static final String DIALOG_MAP_FILTER = "filter";

    public static final String DIALOG_MAP_FILE_NAME = "fileName";

    public static final String DIALOG_MAP_DIR_PATH = "dirPath";

    public static final String RESTORE_STATE = "restore";

    private static final XLog logger = XLogFactory.getLogger(XDisplay.class);

    private transient XViewer viewer;

    private transient XCache resourceCache;

    private transient XCache imageCache;

    private transient ArrayList layers;

    private static XComponent focusedLayer;

    private static XComponent cursor;

    private static XComponent cursorOwner;

    private static XTimer timer;

    private static XTimer scrollBarTimer;

    private transient XView moveSource;

    private transient XComponent dragSource;

    private transient XView previousDragView;

    private transient Rectangle dirtyArea;

    /**
    * Will be used to hold the stack of layers for the opened pop-ups.
    */
    private static List popUpLayers;

    private transient HashMap dottedLinesX;

    private transient HashMap dottedLinesX2;

    private transient HashMap dottedLinesY;

    private static XCalendar calendar;

    private static XDisplay defaultDisplay;

    public static final int GRADIENT_SIZE = 256;

    static final Cursor WAITING_CURSOR;

    public static final Object RESOURCE_MUTEX = new Object();

    private static HashMap fonts = new HashMap();

    private static HashMap gradients = new HashMap();

    private static XComponent clipboard = new XComponent(XComponent.DATA_SET);

    private static volatile boolean loadingResource;

    public static final int OK_OPTION = 1;

    public static final int YES_OPTION = 2;

    public static final int NO_OPTION = 4;

    public static final int CANCEL_OPTION = 8;

    public static final int OK_CANCEL_OPTION = OK_OPTION | CANCEL_OPTION;

    public static final int YES_NO_OPTION = YES_OPTION | NO_OPTION;

    public static final int YES_NO_CANCEL_OPTION = YES_OPTION | NO_OPTION | CANCEL_OPTION;

    public static final int ERROR_MESSAGE = 0;

    public static final int INFORMATION_MESSAGE = 1;

    public static final int WARNING_MESSAGE = 2;

    public static final int QUESTION_MESSAGE = 3;

    /**
    * A map containing for each focused layer, the corresponding focused view.
    */
    private transient Map focusedViews = new IdentityHashMap();

    private static File fileChooserLocation = null;

    /**
    * Initialize the waiting cursor (taking into account the headless mode)
    */
    static {
        Image waitingCursorImage = Toolkit.getDefaultToolkit().createImage(XDisplay.class.getResource("progress_cursor.gif"));
        Cursor waitingCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        try {
            waitingCursor = Toolkit.getDefaultToolkit().createCustomCursor(waitingCursorImage, new Point(0, 0), "waiting_cursor");
        } catch (HeadlessException e) {
            logger.info("Running in headless mode");
        }
        WAITING_CURSOR = waitingCursor;
        setSystemLookAndFeel();
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("Error on getting the system's look and feel", e);
        }
    }

    public XDisplay(XViewer viewer) {
        setSystemLookAndFeel();
        this.viewer = viewer;
        resourceCache = new XCache();
        imageCache = new XCache();
        layers = new ArrayList();
        focusedLayer = null;
        moveSource = null;
        dragSource = null;
        previousDragView = null;
        if (cursor == null) {
            cursor = new XComponent(XComponent.CURSOR);
            cursor.setParent(this);
            cursor.setBounds(new Rectangle());
            cursor.setVisible(false);
            cursor.setFocusable(false);
        }
        if (timer == null) {
            timer = new XTimer("XDisplay");
        }
        if (scrollBarTimer == null) {
            scrollBarTimer = new XTimer();
        }
        dottedLinesX = new HashMap();
        dottedLinesX2 = new HashMap();
        dottedLinesY = new HashMap();
        if (defaultDisplay == null) {
            defaultDisplay = this;
        }
        popUpLayers = new ArrayList();
    }

    public Rectangle getDirtyArea() {
        return dirtyArea;
    }

    public void setDirtyArea(Rectangle dirtyArea) {
        this.dirtyArea = dirtyArea;
    }

    public List getLayers() {
        return layers;
    }

    public final XViewer getViewer() {
        return viewer;
    }

    public XCalendar getCalendar() {
        return calendar != null ? calendar : XCalendar.getDefaultCalendar();
    }

    public static XDisplay getDefaultDisplay() {
        return defaultDisplay;
    }

    /**
    * Disposes default display. Used when refreshing applet page.
    */
    public static void disposeDefaultDisplay() {
        defaultDisplay = null;
        cursor = null;
        timer = null;
    }

    public static XClient getClient() {
        return getDefaultDisplay().viewer.getClient();
    }

    public static XComponent getClipboard() {
        return clipboard;
    }

    public final void setDragSource(XComponent drag_source) {
        setDragSource(drag_source, false);
    }

    public final void setDragSource(XComponent drag_source, boolean enable_drop) {
        dragSource = drag_source;
        if (dragSource != null) {
            dragSource.setDropEnabled(Boolean.valueOf(enable_drop));
            dragSource.setWasDragged(Boolean.FALSE);
        }
    }

    public final XView getDragSource() {
        return dragSource;
    }

    public final void setFocusedLayer(XComponent focused_layer) {
        focusedLayer = focused_layer;
    }

    public static XComponent getActiveLayer() {
        if (focusedLayer != null && focusedLayer.getComponentType() != XComponent.DIALOG) {
            return focusedLayer;
        } else {
            return null;
        }
    }

    public static XComponent getActiveDialog() {
        if (focusedLayer != null && focusedLayer.getComponentType() != XComponent.DIALOG) {
            XView parent = focusedLayer.getParent();
            while (parent != null) {
                if (!(parent instanceof XComponent)) {
                    return null;
                }
                if (((XComponent) parent).getComponentType() == XComponent.DIALOG) {
                    return (XComponent) parent;
                }
                parent = parent.getParent();
            }
            return null;
        } else {
            return focusedLayer;
        }
    }

    public static XComponent getActiveWindow() {
        return focusedLayer;
    }

    public static XComponent getCursor() {
        return cursor;
    }

    static void setCursorOwner(XComponent cursor_owner) {
        cursorOwner = cursor_owner;
    }

    public static XComponent getCursorOwner() {
        return cursorOwner;
    }

    public static XComponent getActiveForm() {
        return _findForm(focusedLayer);
    }

    protected static XComponent _findForm(XComponent component) {
        if (component.getComponentType() == XComponent.FORM) {
            return component;
        } else {
            XComponent form;
            for (int index = 0; index < component.getChildCount(); index++) {
                form = _findForm((XComponent) (component.getChild(index)));
                if (form != null) {
                    return form;
                }
            }
        }
        return null;
    }

    public static XComponent getRootFrame() {
        XDisplay display = getDefaultDisplay();
        if (display.layers.isEmpty()) {
            return null;
        }
        return ((XComponent) display.layers.get(0));
    }

    public static XComponent findFrame(String id) {
        XDisplay display = getDefaultDisplay();
        if (display != null) {
            XComponent layer;
            XComponent component;
            for (int index = display.layers.size() - 1; index >= 0; index--) {
                layer = (XComponent) display.layers.get(index);
                if (layer.getID().equals(id)) {
                    return layer;
                }
                component = layer.findDescendent(id);
                if ((component != null) && (component.getComponentType() == XComponent.FRAME)) {
                    return component;
                }
            }
        }
        return null;
    }

    /**
    * Returns the <code>XComponent</code> representing the parent of the focused view component.
    *
    * @return the <code>XComponent</code> representing the parent of the focused view component
    */
    public final XView getDisplayFocusedViewParent() {
        if (focusedLayer != null) {
            if (focusedLayer.getComponentType() == XComponent.DIALOG) {
                XComponent form = (XComponent) (focusedLayer.getChild(1));
                if (form != null) {
                    return form;
                }
            } else {
                return focusedLayer;
            }
        }
        return null;
    }

    public final XView getDisplayFocusedView() {
        XView focused_view = null;
        if (focusedLayer != null) {
            if (focusedLayer.getComponentType() == XComponent.DIALOG) {
                if (focusedLayer.getChildCount() > 1) {
                    XComponent form = (XComponent) (focusedLayer.getChild(1));
                    focused_view = form.getFocusedView();
                }
            } else {
                focused_view = focusedLayer.getFocusedView();
            }
        }
        return focused_view;
    }

    public final void setDisplayFocusedView(XView view) {
        XComponent layer = view.getForm();
        if (layer != null) {
            XView parent = layer.getParent();
            if ((parent instanceof XComponent) && (((XComponent) parent).getComponentType() == XComponent.DIALOG)) {
                focusedLayer = (XComponent) parent;
            } else {
                focusedLayer = layer;
            }
            layer.setFocusedView(view);
            focusedLayer.setVisible(true);
        }
    }

    public static final XTimer getTimer() {
        return timer;
    }

    public static final XTimer getScrollBarTimer() {
        return scrollBarTimer;
    }

    /**
    * @see XView#processKeyboardEvent(HashMap,int,int,char,int)
    */
    public void processKeyboardEvent(HashMap event, int action, int key_code, char key_char, int modifiers) {
        if (isLoadingResource()) {
            return;
        }
        logger.debug("XDisplay.processKeyboardEvent");
        XView focused_view = getDisplayFocusedView();
        if (focused_view != null) {
            focused_view.processEvent(event);
            if (!XView.isEventConsumed(event)) {
                if ((action == KEY_DOWN) && (focused_view.getParent() instanceof XComponent)) {
                    XComponent parent = (XComponent) focused_view.getParent();
                    switch(key_code) {
                        case TAB_KEY:
                            {
                                if ((parent != null) && (parent.getComponentType() == XComponent.TAB)) {
                                    if ((modifiers & SHIFT_KEY_DOWN) == SHIFT_KEY_DOWN) {
                                        focused_view.transferFocusBackward();
                                    } else {
                                        focused_view.transferFocusForward();
                                    }
                                }
                            }
                            break;
                        case ENTER_KEY:
                            {
                                XComponent form = focused_view.getForm();
                                if (form != null) {
                                    String buttonId = form.getDefaultButton();
                                    if (buttonId != null) {
                                        XComponent button = form.findComponent(buttonId);
                                        if (button != null) {
                                            button.triggerDefaultFormButtonAction(event);
                                        }
                                    }
                                }
                            }
                            break;
                    }
                } else {
                    if (action == KEY_DOWN && key_code == ESCAPE_KEY) {
                        closeLastOpenedLayer();
                    }
                }
            }
        } else {
            if (action == KEY_DOWN && key_code == ESCAPE_KEY) {
                closeLastOpenedLayer();
            }
        }
    }

    /**
    * Performs closing of the last opened pop up or dialog layer.
    */
    public void closeLastOpenedLayer() {
        int popUpLayersNumber = popUpLayers.size();
        if (popUpLayersNumber > 0) {
            XComponent popUpLayer = (XComponent) popUpLayers.get(popUpLayersNumber - 1);
            popUpLayer.sendClosePopUpEvent();
            return;
        }
        int layersNumber = layers.size();
        if (layersNumber > 1) {
            XComponent lastOpenedLayer = (XComponent) layers.get(layersNumber - 1);
            if (lastOpenedLayer.getComponentType() == XComponent.DIALOG) {
                lastOpenedLayer.close();
            }
        }
    }

    public void processPointerEvent(HashMap event, int action, int x, int y, int modifiers) {
        logger.debug("XDisplay.processPointerEvent");
        if (isLoadingResource()) {
            return;
        }
        if (isEventConsumed(event)) {
            return;
        }
        if (layers.size() > 0) {
            if (action == POINTER_EXITED || action == POINTER_ENTERED) {
                XComponent.closeTooltips();
                return;
            }
            if (action == POINTER_MOVE) {
                XView source = find(x, y);
                XViewer viewer = getDisplay().getViewer();
                Cursor curentCursor = viewer.getCursor();
                if (curentCursor != source.getMouseCursor()) {
                    viewer.setCursor(source.getMouseCursor());
                }
                if (source != moveSource && source.isEnterLeaveEventSource()) {
                    if (moveSource != null) {
                        XComponent.closeTooltips();
                    }
                    Point position = source.relativePosition(x, y);
                    HashMap pointer_enter_event = new HashMap(event);
                    pointer_enter_event.put(XComponent.ACTION, new Integer(XComponent.POINTER_ENTER));
                    pointer_enter_event.put(X, new Integer(position.x));
                    pointer_enter_event.put(Y, new Integer(position.y));
                    source.processEvent(pointer_enter_event);
                    moveSource = source;
                } else {
                    if (source == moveSource) {
                        Point position = source.relativePosition(x, y);
                        HashMap pointer_enter_event = new HashMap(event);
                        pointer_enter_event.put(XComponent.ACTION, new Integer(XComponent.POINTER_MOVE));
                        pointer_enter_event.put(X, new Integer(position.x));
                        pointer_enter_event.put(Y, new Integer(position.y));
                        source.processEvent(pointer_enter_event);
                    }
                }
                return;
            }
            if ((action == POINTER_UP) && (dragSource != null)) {
                if ((dragSource.getDropEnabled() != null && dragSource.getDropEnabled().booleanValue()) && (dragSource.wasDragged() != null && dragSource.wasDragged().booleanValue())) {
                    XView drop_target = find(x, y, dragSource);
                    logger.debug("   DROP target = " + drop_target);
                    Point position = drop_target.relativePosition(x, y);
                    logger.debug("   DROP rel-pos = " + position);
                    HashMap drop_event = new HashMap(event);
                    drop_event.put(XComponent.ACTION, new Integer(XComponent.POINTER_DROP));
                    drop_event.put(X, new Integer(position.x));
                    drop_event.put(Y, new Integer(position.y));
                    drop_target.processEvent(drop_event);
                }
                Point position = dragSource.relativePosition(x, y);
                HashMap drag_end_event = new HashMap(event);
                drag_end_event.put(XComponent.ACTION, new Integer(XComponent.POINTER_DRAG_END));
                drag_end_event.put(X, new Integer(position.x));
                drag_end_event.put(Y, new Integer(position.y));
                dragSource.processEvent(drag_end_event);
                if (dragSource != null) {
                    dragSource.setWasDragged(Boolean.FALSE);
                }
                dragSource = null;
            }
            if (action == POINTER_DRAG) {
                if (dragSource != null) {
                    dragSource.setWasDragged(Boolean.TRUE);
                    if (dragSource.getDropEnabled() != null && dragSource.getDropEnabled().booleanValue()) {
                        XView drag_view = find(x, y);
                        if (previousDragView != drag_view) {
                            if (previousDragView != null) {
                                HashMap drag_leave_event = new HashMap(event);
                                drag_leave_event.put(XComponent.ACTION, new Integer(XView.POINTER_DRAG_LEAVE));
                                previousDragView.processEvent(drag_leave_event);
                            }
                            HashMap drag_enter_event = new HashMap(event);
                            drag_enter_event.put(XComponent.ACTION, new Integer(XView.POINTER_DRAG_ENTER));
                            drag_view.processEvent(drag_enter_event);
                        }
                        HashMap drag_over_event = new HashMap(event);
                        drag_over_event.put(XComponent.ACTION, new Integer(XView.POINTER_DRAG_OVER));
                        Point position = drag_view.relativePosition(x, y);
                        drag_over_event.put(X, new Integer(position.x));
                        drag_over_event.put(Y, new Integer(position.y));
                        drag_view.processEvent(drag_over_event);
                        previousDragView = drag_view;
                    }
                    if (dragSource != null) {
                        Point position = dragSource.relativePosition(x, y);
                        event.put(X, new Integer(position.x));
                        event.put(Y, new Integer(position.y));
                        dragSource.processEvent(event);
                    }
                }
                return;
            }
            for (int index = layers.size() - 1; index >= 0; index--) {
                XView component = (XView) (layers.get(index));
                if (component.getVisible()) {
                    Rectangle bounds = component.getBounds();
                    if (bounds != null) {
                        if ((x >= bounds.x) && (y >= bounds.y) && (x <= bounds.x + bounds.width) && (y <= bounds.y + bounds.height)) {
                            event.put(X, new Integer(x - bounds.x));
                            event.put(Y, new Integer(y - bounds.y));
                            component.processEvent(event);
                            return;
                        }
                    }
                }
                if (component.getModal() != null && component.getModal().booleanValue()) {
                    return;
                }
            }
        }
    }

    /**
    * @see XView#processMouseWheelEvent(java.util.HashMap,int)
    */
    public void processMouseWheelEvent(HashMap event, int action) {
        if (isLoadingResource()) {
            return;
        }
        logger.debug("XDisplay.processMouseWheelEvent");
        int x = ((Integer) event.get(X)).intValue();
        int y = ((Integer) event.get(Y)).intValue();
        XView view = find(x, y);
        if (!view.equals(this)) {
            view.processEvent(event);
        }
    }

    public void paint(Graphics g, Rectangle clip_area) {
        logger.debug("XDisplay.paint(): clip_area = " + clip_area);
        g.setClip(clip_area);
        g.setColor(getStyleAttributes().background);
        g.fillRect(clip_area.x, clip_area.y, clip_area.width, clip_area.height);
        Rectangle child_clip_area;
        for (int index = 0; index < layers.size(); index++) {
            XView component = (XView) (layers.get(index));
            if (component.getVisible()) {
                Rectangle bounds = component.getBounds();
                if (bounds != null) {
                    if (!((clip_area.x + clip_area.width < bounds.x) || (clip_area.x > bounds.x + bounds.width + 1) || (clip_area.y + clip_area.height < bounds.y) || (clip_area.y > bounds.y + bounds.height + 1))) {
                        g.translate(bounds.x, bounds.y);
                        child_clip_area = new Rectangle(clip_area.x - bounds.x, clip_area.y - bounds.y, clip_area.width, clip_area.height);
                        component.paint(g, child_clip_area);
                        g.translate(-bounds.x, -bounds.y);
                    }
                }
            }
        }
    }

    public final void repaint(Rectangle clip_area) {
        viewer.repaintDisplay(new Rectangle(clip_area.x - 1, clip_area.y - 1, clip_area.width + 2, clip_area.height + 2));
    }

    public void doLayout() {
        if (layers.isEmpty()) {
            return;
        }
        XComponent firstLayer = (XComponent) layers.get(0);
        firstLayer.setBounds(this.getBounds());
        firstLayer.doLayout();
        for (int i = 1; i < layers.size(); i++) {
            XComponent layer = (XComponent) layers.get(i);
            layer.doLayout();
        }
    }

    protected void _addLayer(XView layer) {
        layer.setParent(this);
        layers.add(layer);
    }

    /**
    * Will open a pop up at the given coordinates.
    * Try to open pop-up to the right/bottom side of component_bounds. For y, If it does not fit "below" , try it "above"
    * If neither above nor below: Then as much below as possible
    *
    * @param layer
    * @param component_bounds - the bounds of the "owning" component (in order to compute where to show the pop-up)
    * @param owner            the component that "owns" the pop-up.
    *                         (The logical parent, not the phisical one)
    */
    public void openPopUp(XComponent layer, Rectangle component_bounds, XComponent owner) {
        openPopUp(layer, component_bounds, owner, true);
    }

    public void openPopUp(XComponent layer, Rectangle component_bounds, XComponent owner, boolean focus) {
        layer.setOldActiveForm(getDisplay().getDisplayFocusedView());
        Point p = getPopUpPositionOnDisplay(layer, component_bounds);
        openLayer(layer, p.x, p.y, focus);
        addPopUpLayer(layer);
        if (owner != null) {
            layer.registerEventHandler(owner, COMPONENT_EVENT);
        }
    }

    public Point getPopUpPositionOnDisplay(XComponent popUpLayer, Rectangle component_bounds) {
        Dimension layer_size = popUpLayer.getPreferredSize();
        Rectangle display_bounds = getBounds();
        int x = component_bounds.x;
        int y = component_bounds.y + component_bounds.height;
        if (x + layer_size.width > display_bounds.width) {
            x -= x + layer_size.width - display_bounds.width;
        }
        if (y + layer_size.height > display_bounds.height) {
            y = component_bounds.y - layer_size.height;
            if (y < 0) {
                y = display_bounds.height - layer_size.height;
            }
        }
        return new Point(x, y);
    }

    /**
    * Will open a new layer at the given coordinates.
    *
    * @param layer a <code>XComponent</code> representing the display layer
    * @param x     a <code>int</code> representing x coodrinate
    * @param y     a <code>int</code> representing y coodrinate
    * @param focus a <code>boolean<code> indicating that the <code>layer<code> requests focus or not
    */
    public void openLayer(XComponent layer, int x, int y, boolean focus) {
        Dimension preferred_size = layer.getPreferredSize();
        openLayer(layer, x, y, preferred_size.width, preferred_size.height, focus);
    }

    /**
    * Will open a new layer at the given coordinates.
    *
    * @param layer  a <code>XComponent</code> representing the display layer
    * @param x      a <code>int</code> representing x coodrinate
    * @param y      a <code>int</code> representing y coodrinate
    * @param width  a <code>int</code> representing width of the <code>layer</code>
    * @param height a <code>int</code> representing height of the <code>layer</code>
    * @param focus  a <code>boolean<code> indicating that the <code>layer<code> requests focus or not
    */
    public synchronized void openLayer(XComponent layer, int x, int y, int width, int height, boolean focus) {
        logger.debug("XDisplay.openLayer()");
        XView previousLayer = null;
        if (!layers.isEmpty()) {
            previousLayer = (XView) layers.get(layers.size() - 1);
        }
        _addLayer(layer);
        layer.setVisible(true);
        layer.setBounds(new Rectangle(x, y, width, height));
        if (focus) {
            if (previousLayer != null) {
                focusedViews.put(previousLayer, this.getDisplayFocusedView());
            }
            layer.requestFocus();
            focusedLayer = layer;
            focusedLayer.sendShowEvent();
        }
        repaint(layer.getBounds());
    }

    public void closeLayer(XComponent layer) {
        layer.setVisible(false);
        if (focusedLayer == layer) {
            XView focused_view = getDisplayFocusedView();
            if (focused_view != null) {
                focused_view.setFocused(false);
            }
        }
        layer.traverseFormChildrenOnUnload(false, new ArrayList(), new ArrayList());
        if (layers.remove(layer)) {
            layer.setParent(null);
            if (layers.size() > 0) {
                focusedLayer = (XComponent) (layers.get(layers.size() - 1));
                repaint();
            }
        }
        XView previousFocusedView = (XView) focusedViews.remove(focusedLayer);
        if (previousFocusedView != null) {
            previousFocusedView.requestFocus();
        }
    }

    public XComponent previousLayer(XComponent layer) {
        logger.debug("pL l " + layer);
        for (int i = layers.size() - 1; i > 0; i--) {
            logger.debug("   " + i);
            logger.debug("XDisplay.previousLayer(): id = " + layers.get(i));
            if (layer == layers.get(i)) {
                logger.debug("   Previous is " + (layers.get(i - 1)));
                return (XComponent) (layers.get(i - 1));
            }
        }
        return null;
    }

    public final Image createImage(int width, int height) {
        return viewer.createImage(width, height);
    }

    public final Image createImage(int width, int height, int[] pixels, int offset, int scan_line) {
        return viewer.createImage(width, height, pixels, offset, scan_line);
    }

    public final Image getDottedLineX(int rgb) {
        Image image = (Image) (dottedLinesX.get(new Integer(rgb)));
        if (image == null) {
            image = _createDottedLineX(rgb);
        }
        return image;
    }

    public final Image getDottedLineX2(int rgb) {
        Image image = (Image) (dottedLinesX2.get(new Integer(rgb)));
        if (image == null) {
            image = _createDottedLineX2(rgb);
        }
        return image;
    }

    public final Image getDottedLineY(int rgb) {
        Image image = (Image) (dottedLinesY.get(new Integer(rgb)));
        if (image == null) {
            image = _createDottedLineY(rgb);
        }
        return image;
    }

    private Image _createDottedLineX(int rgb) {
        int width = getBounds().width;
        if ((width % 2) != 0) {
            width++;
        }
        int[] pixels_x = new int[width];
        for (int index = 0; index < width; index++) {
            pixels_x[index] = 0;
            index++;
            pixels_x[index] = rgb;
        }
        BufferedImage dotted_line_x = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
        dotted_line_x.setRGB(0, 0, width, 1, pixels_x, 0, width);
        dottedLinesX.put(new Integer(rgb), dotted_line_x);
        return dotted_line_x;
    }

    private Image _createDottedLineX2(int rgb) {
        int width = getBounds().width;
        if ((width % 2) != 0) {
            width++;
        }
        int[] pixels_x = new int[width];
        for (int index = 0; index < width; index++) {
            pixels_x[index] = rgb;
            index++;
            pixels_x[index] = 0;
        }
        BufferedImage dotted_line_x2 = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
        dotted_line_x2.setRGB(0, 0, width, 1, pixels_x, 0, width);
        dottedLinesX2.put(new Integer(rgb), dotted_line_x2);
        return dotted_line_x2;
    }

    private Image _createDottedLineY(int rgb) {
        int height = getBounds().height;
        if ((height % 2) != 0) {
            height++;
        }
        int[] pixels_y = new int[height];
        for (int index = 0; index < height; index++) {
            pixels_y[index] = rgb;
            index++;
            pixels_y[index] = 0;
        }
        BufferedImage dotted_line_y = new BufferedImage(1, height, BufferedImage.TYPE_INT_ARGB);
        dotted_line_y.setRGB(0, 0, 1, height, pixels_y, 0, 1);
        dottedLinesY.put(new Integer(rgb), dotted_line_y);
        return dotted_line_y;
    }

    public synchronized void addChild(XView child) {
        _addLayer(child);
        child.setVisible(true);
    }

    public XView find(int x, int y, XView exclude_view) {
        if (layers.size() > 0) {
            XView layer;
            Rectangle bounds;
            for (int index = layers.size() - 1; index >= 0; index--) {
                layer = (XView) (layers.get(index));
                bounds = layer.getBounds();
                if ((bounds != null) && (x >= bounds.x) && (y >= bounds.y) && (x < bounds.x + bounds.width) && (y < bounds.y + bounds.height)) {
                    return layer.find(x - bounds.x, y - bounds.y, exclude_view);
                }
            }
        }
        return this;
    }

    public final void showForm(String location) {
        int delim = location.indexOf('?');
        HashMap params = null;
        if (delim >= 0) {
            params = new HashMap();
            String[] paramsVal = location.substring(delim + 1).split("&");
            location = location.substring(0, delim);
            for (int pos = 0; pos < paramsVal.length; pos++) {
                delim = paramsVal[pos].indexOf('=');
                if (delim >= 0) {
                    params.put(paramsVal[pos].substring(0, delim), paramsVal[pos].substring(delim + 1));
                }
            }
        }
        showForm(location, params);
    }

    public final void removeAllLayers() {
        _removeAllLayers();
    }

    protected final void _removeAllLayers() {
        ArrayList layers = new ArrayList();
        int index;
        for (index = 0; index < this.layers.size(); index++) {
            layers.add(this.layers.get(index));
        }
        for (index = 0; index < layers.size(); index++) {
            closeLayer((XComponent) layers.get(index));
        }
    }

    public void showForm(String location, Map parameters) {
        _removeAllLayers();
        XComponent form = loadForm(location, parameters, null, null);
        _addLayer(form);
        doLayout();
        repaint();
        form.sendShowEvent();
    }

    public static XComponent loadForm(String location, Map parameters, ArrayList previousFormIds, ArrayList previousStateMaps) {
        boolean changeFocusedLayer = false;
        if (focusedLayer != null && !focusedLayer.getVisible()) {
            focusedLayer.setFocusedView(null);
            changeFocusedLayer = true;
        }
        XComponent form = _loadForm(location, parameters, previousFormIds, previousStateMaps);
        defaultDisplay.viewer.waitForImages();
        if (changeFocusedLayer) {
            focusedLayer = form;
            focusedLayer.setVisible(true);
        }
        return form;
    }

    /**
    * Closes all the dialog layers that have been opened by this display.
    */
    public void closeDialogLayers() {
        List layersToClose = new ArrayList();
        for (int i = 0; i < layers.size(); i++) {
            XView layer = (XView) layers.get(i);
            if (layer instanceof XComponent && ((XComponent) layer).getComponentType() == XComponent.DIALOG) {
                layersToClose.add(layer);
            }
        }
        for (Iterator it = layersToClose.iterator(); it.hasNext(); ) {
            XComponent layer = (XComponent) it.next();
            layer.close();
        }
    }

    private static XComponent _loadForm(String location, Map parameters, ArrayList previousFormIds, ArrayList previousStateMaps) {
        logger.info("XDisplay.loadForm(): location = " + location);
        XMessage request = new XMessage("Express.loadResource");
        request.setArgument(XConstants.LOCATION_ARGUMENT, location);
        request.setArgument(XConstants.PARAMETERS_ARGUMENT, parameters);
        if (previousFormIds != null) {
            request.setArgument(XConstants.PREVIOUS_FORM_IDS_ARGUMENT, previousFormIds);
        }
        if (previousStateMaps != null) {
            request.setArgument(XConstants.PREVIOUS_STATE_MAPS_ARGUMENT, previousStateMaps);
        }
        XMessage response = defaultDisplay.viewer.getClient().invokeMethod(request);
        byte[] content = (byte[]) (response.getArgument(XConstants.CONTENT_ARGUMENT));
        if (content == null) {
            logger.warn("XDisplay.loadForm(): Load form resource returns null");
            return null;
        }
        XComponent form = null;
        try {
            ObjectInputStream object_input = new ObjectInputStream(new ByteArrayInputStream(content));
            form = (XComponent) (object_input.readObject());
            object_input.close();
        } catch (IOException e) {
            logger.error("Error on deserializing form: " + e);
        } catch (ClassNotFoundException e) {
            logger.error("Error on deserializing form: " + e);
        }
        ArrayList resources = new ArrayList();
        ArrayList images = new ArrayList();
        ArrayList frames = new ArrayList();
        defaultDisplay._queryResourcesAndFrames(form, form, resources, images, frames);
        defaultDisplay.preLoadImages(images);
        defaultDisplay.preLoadSimpleResources(resources);
        XComponent frame;
        for (int index = 0; index < frames.size(); index++) {
            frame = (XComponent) (frames.get(index));
            String frame_content = frame.getContent();
            if (frame_content != null) {
                frame.loadForm(frame_content, parameters);
            }
        }
        if (parameters == null || !Boolean.FALSE.equals(parameters.get(RESTORE_STATE))) {
            HashMap componentStateMap = (HashMap) (response.getArgument(XConstants.STATE_ARGUMENT));
            if (componentStateMap != null) {
                Iterator componentIds = componentStateMap.keySet().iterator();
                XComponent component;
                while (componentIds.hasNext()) {
                    String componentId = (String) componentIds.next();
                    component = form.findStatefulComponent(componentId);
                    if (component != null) {
                        component.restoreState((Serializable) componentStateMap.get(componentId));
                    }
                }
            }
        }
        return form;
    }

    private void _queryResourcesAndFrames(XComponent form, XView view, ArrayList resources, ArrayList images, ArrayList frames) {
        if (view == null) {
            logger.debug("query resources: " + view);
        }
        int index;
        if (view instanceof XComponent) {
            ArrayList view_resources = ((XComponent) view).requiredResources();
            if (view_resources != null) {
                for (index = 0; index < view_resources.size(); index++) {
                    resources.add(view_resources.get(index));
                }
            }
            List view_images = ((XComponent) view).requiredImages();
            if (view_images != null) {
                for (index = 0; index < view_images.size(); index++) {
                    images.add(view_images.get(index));
                }
            }
            if (((XComponent) view).getComponentType() == XComponent.FRAME) {
                frames.add(view);
            }
        }
        for (index = 0; index < view.getChildCount(); index++) {
            _queryResourcesAndFrames(form, view._getChild(index), resources, images, frames);
        }
    }

    private byte[] _loadResource(String location) {
        XMessage request = new XMessage("Express.loadResource");
        request.setArgument(XConstants.LOCATION_ARGUMENT, location);
        XMessage response = viewer.getClient().invokeMethod(request);
        return (byte[]) (response.getArgument(XConstants.CONTENT_ARGUMENT));
    }

    /**
    * Implements image-level caching.
    *
    * @param location
    * @return
    */
    public Image loadImage(String location) {
        Image image = (Image) (imageCache.get(location));
        if (image != null) {
            return image;
        } else {
            byte[] content = _loadResource(location);
            if (content != null) {
                image = Toolkit.getDefaultToolkit().createImage(content);
                Toolkit.getDefaultToolkit().checkImage(image, -1, -1, null);
                viewer.trackImage(image);
                imageCache.put(location, image);
            }
            return image;
        }
    }

    public byte[] loadResource(String location) {
        byte[] content = (byte[]) resourceCache.get(location);
        if (content != null) {
            return content;
        } else {
            content = _loadResource(location);
            if (content != null) {
                resourceCache.put(location, content);
            }
            return content;
        }
    }

    public void preLoadImages(ArrayList locations) {
        ArrayList uncachedLocations = new ArrayList();
        String location = null;
        int i = 0;
        for (i = 0; i < locations.size(); i++) {
            location = (String) locations.get(i);
            if (imageCache.get(location) == null) uncachedLocations.add(location);
        }
        if (uncachedLocations.size() == 0) return;
        XMessage request = new XMessage("Express.loadAuxiliaryResources");
        logger.info("   Uncached: " + uncachedLocations.size());
        request.setArgument(XConstants.LOCATIONS_ARGUMENT, uncachedLocations);
        XMessage response = viewer.getClient().invokeMethod(request);
        HashMap uncachedResources = (HashMap) (response.getArgument(XConstants.RESOURCE_MAP_ARGUMENT));
        byte[] resource = null;
        Image image = null;
        for (i = 0; i < uncachedLocations.size(); i++) {
            location = (String) uncachedLocations.get(i);
            resource = (byte[]) uncachedResources.get(location);
            if (resource != null) {
                image = Toolkit.getDefaultToolkit().createImage(resource);
                Toolkit.getDefaultToolkit().checkImage(image, -1, -1, null);
                viewer.trackImage(image);
                imageCache.put(location, image);
            }
        }
    }

    public void preLoadSimpleResources(ArrayList locations) {
        logger.info("preLoadSimpleResources: #" + locations.size());
        ArrayList uncachedLocations = new ArrayList();
        String location = null;
        int i = 0;
        for (i = 0; i < locations.size(); i++) {
            location = (String) locations.get(i);
            if (resourceCache.get(location) == null) uncachedLocations.add(location);
        }
        if (uncachedLocations.size() == 0) return;
        XMessage request = new XMessage("Express.loadAuxiliaryResources");
        request.setArgument(XConstants.LOCATIONS_ARGUMENT, uncachedLocations);
        XMessage response = viewer.getClient().invokeMethod(request);
        HashMap uncachedResources = (HashMap) (response.getArgument(XConstants.RESOURCE_MAP_ARGUMENT));
        byte[] resource = null;
        for (i = 0; i < uncachedLocations.size(); i++) {
            location = (String) uncachedLocations.get(i);
            resource = (byte[]) uncachedResources.get(location);
            if (resource != null) resourceCache.put(location, resource);
        }
    }

    public static Font font(String name, int size, int style) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(name);
        buffer.append('-');
        buffer.append(style);
        buffer.append('-');
        buffer.append(size);
        String key = buffer.toString();
        Font font = (Font) (fonts.get(key));
        if (font == null) {
            font = new Font(name, size, style);
            fonts.put(key, font);
        }
        return font;
    }

    private static Image _newLinearGradient(int orientation, Color color1, Color color2) {
        int[] pixels = new int[GRADIENT_SIZE];
        int red1 = color1.getRed();
        int green1 = color1.getGreen();
        int blue1 = color1.getBlue();
        int red2 = color2.getRed();
        int green2 = color2.getGreen();
        int blue2 = color2.getBlue();
        int red;
        int green;
        int blue;
        for (int index = 0; index < GRADIENT_SIZE; index++) {
            red = red1 - (red1 - red2) * index / GRADIENT_SIZE;
            green = green1 - (green1 - green2) * index / GRADIENT_SIZE;
            blue = blue1 - (blue1 - blue2) * index / GRADIENT_SIZE;
            pixels[index] = (255 << 24) | (red << 16) | (green << 8) | blue;
        }
        if (orientation == HORIZONTAL) {
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(GRADIENT_SIZE, 1, pixels, 0, GRADIENT_SIZE));
        } else {
            return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(1, GRADIENT_SIZE, pixels, 0, 1));
        }
    }

    private static Image _newMetalCenterGradient(int orientation, Color color1, Color color2) {
        int[] pixels = new int[GRADIENT_SIZE * GRADIENT_SIZE];
        int red1 = color1.getRed();
        int green1 = color1.getGreen();
        int blue1 = color1.getBlue();
        int red2 = color2.getRed();
        int green2 = color2.getGreen();
        int blue2 = color2.getBlue();
        int red;
        int green;
        int blue;
        Color[] base_colors = new Color[GRADIENT_SIZE];
        Color base_color;
        int gradient2 = GRADIENT_SIZE / 2;
        int index;
        for (index = 0; index < gradient2; index++) {
            red = red1 - (red1 - red2) * index / gradient2;
            green = green1 - (green1 - green2) * index / gradient2;
            blue = blue1 - (blue1 - blue2) * index / gradient2;
            base_color = new Color(red, green, blue);
            base_colors[index] = base_color;
            base_colors[GRADIENT_SIZE - index - 1] = base_color;
            logger.debug("###GRADIENT r " + red);
        }
        for (int y = 0; y < GRADIENT_SIZE; y++) {
            for (int x = 0; x < GRADIENT_SIZE; x++) {
                if (orientation == HORIZONTAL) {
                    index = y * GRADIENT_SIZE + x;
                } else {
                    index = x * GRADIENT_SIZE + y;
                }
                red = base_colors[x].getRed();
                green = base_colors[x].getGreen();
                blue = base_colors[x].getBlue();
                pixels[index] = (255 << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        return Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(GRADIENT_SIZE, GRADIENT_SIZE, pixels, 0, GRADIENT_SIZE));
    }

    /**
    * Configures XDisplay's calendar. Since the <code>OpProjectCalendar</code> is a singleton, after the calendar is initialized
    * it will remain so throughout the client (this method is called when the user signs in).
    *
    * @param newCalendar
    */
    public void setCalendar(XCalendar newCalendar) {
        calendar = newCalendar;
    }

    public static Image gradient(String name, int orientation, Color color1, Color color2) {
        StringBuffer key = new StringBuffer(name);
        if (orientation == HORIZONTAL) {
            key.append("-h-");
        } else {
            key.append("-v-");
        }
        key.append(color1.getRGB());
        key.append('-');
        key.append(color2.getRGB());
        Image gradient = (Image) (gradients.get(key.toString()));
        if (gradient == null) {
            if (name == XStyle.LINEAR_GRADIENT) {
                gradient = _newLinearGradient(orientation, color1, color2);
            } else if (name == XStyle.METAL_CENTER_GRADIENT) {
                gradient = _newMetalCenterGradient(orientation, color1, color2);
            }
            gradients.put(key.toString(), gradient);
        }
        return gradient;
    }

    /**
    * Opens a file dialog used for loading/saving files.
    *
    * @param title       a <code>String</code> representing the title of the dialog.
    * @param load        a <code>boolean</code> indicating whether a load or a save is wanted. If it is <code>true</code>, then
    *                    a load should be performed. Otherwise, a save.
    * @param filters     a <code>Map</code> containing the type of files that are going to be saved/loaded: *.csv, *.png etc
    * @param filesOnly   TRUE if only files can be selected. FALSE if only folders can be selected.
    * @param defaultFile The default file to be selected
    * @return a <code>String</code> representing the full path to a file.
    */
    public String showFileDialog(String title, Boolean load, String defaultFile, Map filters, Boolean filesOnly) {
        Map dialogMap = fileChooserDialog(title, load, defaultFile, filters, filesOnly);
        if (dialogMap != null) {
            String fileName = dialogMap.get(XDisplay.DIALOG_MAP_FILE_NAME).toString();
            int extensionIndex = fileName.lastIndexOf(".");
            if (extensionIndex <= -1) {
                if (dialogMap.get(XDisplay.DIALOG_MAP_FILTER) != null) {
                    String filterExtension = dialogMap.get(XDisplay.DIALOG_MAP_FILTER).toString();
                    filterExtension = filterExtension.substring(filterExtension.lastIndexOf("."));
                    fileName += filterExtension;
                }
            }
            File f = new File(dialogMap.get(XDisplay.DIALOG_MAP_DIR_PATH).toString() + fileName);
            return f.getAbsolutePath();
        }
        return null;
    }

    /**
    * Opens a file dialog used for loading/saving files.
    *
    * @param title       a <code>String</code> representing the title of the dialog.
    * @param load        a <code>boolean</code> indicating whether a load or a save is wanted. If it is <code>true</code>, then
    *                    a load should be performed. Otherwise, a save.
    * @param filters     a <code>Map</code> containing the type of files that are going to be saved/loaded: *.csv, *.png etc
    * @param filesOnly   TRUE if only files can be selected. FALSE if only folders can be selected.
    * @param defaultFile The default file to be selected
    * @return a <code>Map</code> containing the chosen file filter, the file name entered by the user and the file's directory path.
    */
    public Map fileChooserDialog(String title, Boolean load, String defaultFile, Map filters, Boolean filesOnly) {
        return awtFileChooserDialog(title, load, defaultFile, filters, filesOnly);
    }

    /**
    * Opens a file dialog used for loading/saving files.
    *
    * @param title a <code>String</code> representing the title of the dialog.
    * @param load  a <code>boolean</code> indicating whether a load or a save is wanted. If it is <code>true</code>, then
    *              a load should be performed. Otherwise, a save.
    * @return a <code>String</code> representing the full path to a file.
    */
    public Map awtFileChooserDialog(String title, Boolean load, String defaultFile, Map filters, Boolean filesOnly) {
        int mode = load.booleanValue() ? FileDialog.LOAD : FileDialog.SAVE;
        FileDialog fileDialog = new FileDialog(viewer.getFrame());
        if (fileChooserLocation != null) {
            fileDialog.setDirectory(fileChooserLocation.getAbsolutePath());
        }
        fileDialog.setTitle(title);
        fileDialog.setMode(mode);
        if (defaultFile != null) {
            File file = new File(defaultFile);
            boolean dirOnly = false;
            if (filesOnly != null) {
                if (!filesOnly.booleanValue()) {
                    dirOnly = true;
                }
            }
            if (dirOnly) {
                while (file != null && (!file.exists() || (file.exists() && !file.isDirectory()))) {
                    file = file.getParentFile();
                }
            } else {
                while (file != null && !file.exists()) {
                    file = file.getParentFile();
                }
            }
            if (file != null) {
                if (file.isDirectory()) {
                    fileDialog.setDirectory(file.getAbsolutePath());
                } else {
                    fileDialog.setFile(file.getName());
                    fileDialog.setDirectory(file.getParent());
                }
            }
        }
        boolean customFilter = false;
        XCompoundFileFilter compountFilter = null;
        String nameFilter = "*";
        if (filters != null) {
            List fileFilters = new LinkedList();
            Iterator iter = filters.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Entry) iter.next();
                String fileTypes = (String) entry.getValue();
                if (fileTypes != null) {
                    if (!fileTypes.equalsIgnoreCase("*.*") && fileTypes.startsWith("*.")) {
                        XFileFilter filter = new XFileFilter((String) entry.getKey(), fileTypes);
                        fileFilters.add(filter);
                        customFilter = true;
                    }
                }
            }
            compountFilter = new XCompoundFileFilter(fileFilters);
            nameFilter = compountFilter.getExtensions();
        }
        fileDialog.setFilenameFilter(compountFilter);
        fileDialog.setVisible(true);
        requestViewerFocus();
        File file = new File(fileDialog.getDirectory(), fileDialog.getFile());
        if (fileDialog.getFile() != null) {
            Map resultMap = new HashMap();
            resultMap.put(DIALOG_MAP_FILE_NAME, file.getName());
            resultMap.put(DIALOG_MAP_DIR_PATH, file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(file.getName())));
            fileChooserLocation = new File(fileDialog.getDirectory());
            return resultMap;
        }
        return null;
    }

    /**
    * Opens a file dialog used for loading/saving files.
    *
    * @param title       a <code>String</code> representing the title of the dialog.
    * @param load        a <code>boolean</code> indicating whether a load or a save is wanted. If it is <code>true</code>, then
    *                    a load should be performed. Otherwise, a save.
    * @param filters     a <code>Map</code> containing the type of files that are going to be saved/loaded: *.csv, *.png etc
    * @param filesOnly   TRUE if only files can be selected. FALSE if only folders can be selected.
    * @param defaultFile The default file to be selected
    * @return a <code>Map</code> containing the chosen file filter, the file name entered by the user and the file's directory path.
    */
    public Map swingFileChooserDialog(String title, Boolean load, String defaultFile, Map filters, Boolean filesOnly) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooserLocation != null) {
            fileChooser.setCurrentDirectory(fileChooserLocation);
        }
        fileChooser.resetChoosableFileFilters();
        fileChooser.rescanCurrentDirectory();
        fileChooser.setDialogTitle(title);
        boolean dirOnly = false;
        if (filesOnly != null) {
            if (filesOnly.booleanValue()) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            } else {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                dirOnly = true;
            }
        }
        fileChooser.setAcceptAllFileFilterUsed(true);
        boolean customFilter = false;
        if (filters != null) {
            Iterator iter = filters.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Entry) iter.next();
                String fileTypes = (String) entry.getValue();
                if (fileTypes != null) {
                    if (!fileTypes.equalsIgnoreCase("*.*") && fileTypes.startsWith("*.")) {
                        XFileFilter filter = new XFileFilter((String) entry.getKey(), fileTypes);
                        fileChooser.addChoosableFileFilter(filter);
                        fileChooser.setAcceptAllFileFilterUsed(false);
                        customFilter = true;
                    }
                }
            }
        }
        if (defaultFile != null) {
            File file = new File(defaultFile);
            if (dirOnly) {
                while (file != null && (!file.exists() || (file.exists() && !file.isDirectory()))) {
                    file = file.getParentFile();
                }
            } else {
                while (file != null && !file.exists()) {
                    file = file.getParentFile();
                }
            }
            if (file != null) {
                fileChooser.setSelectedFile(file);
            }
        }
        int returnVal = load.booleanValue() ? fileChooser.showOpenDialog(viewer.getFocusableView()) : fileChooser.showSaveDialog(viewer.getFocusableView());
        viewer.getFrame().requestFocus();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            fileChooser.setSelectedFile(null);
            if (!file.exists() && load.booleanValue()) {
                return null;
            }
            Map resultMap = new HashMap();
            if (customFilter) {
                resultMap.put(DIALOG_MAP_FILTER, ((XFileFilter) fileChooser.getFileFilter()).fileTypes);
            }
            resultMap.put(DIALOG_MAP_FILE_NAME, file.getName());
            resultMap.put(DIALOG_MAP_DIR_PATH, file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(file.getName())));
            fileChooserLocation = file;
            return resultMap;
        } else {
            return null;
        }
    }

    /**
    * @see XViewer#showURL(String,String)
    */
    public void showURL(String url, String title) {
        viewer.showURL(url, title);
    }

    /**
    * @see XViewer#showURL(URL,String)
    */
    public void showURL(URL url, String title) {
        viewer.showURL(url, title);
    }

    /**
    * @see XViewer#showDocument(String,String)
    */
    public void showDocument(String url, String contentId) {
        viewer.showDocument(url, contentId);
    }

    public void showStartForm(Map parameters) {
        viewer.showStartForm(parameters);
    }

    /**
    * Will set the active popUpLayer attribute.
    *
    * @param layer
    */
    public static void addPopUpLayer(XComponent layer) {
        popUpLayers.add(layer);
    }

    /**
    * Will remove the given layer from the list of pop-ups.
    *
    * @param layer
    */
    public static void removePopUpLayer(XComponent layer) {
        popUpLayers.remove(layer);
    }

    public static List getPopUpLayers() {
        return popUpLayers;
    }

    /**
    * Will notify all the popups that ar on other laer that the focused one
    * to send the CLOSE_POP_UP event.
    *
    * @param focusedView - the view that has the focus when the notification is sent,
    *                    in order to restore it after the pop-up will be closed.
    *                    If it is null the focused view will not be changed (= will have the same
    *                    focused view after closing the pop-up as before opening it)
    */
    public static void sendPopUpNotification(XView focusedView) {
        if (popUpLayers.size() != 0) {
            for (int i = popUpLayers.size() - 1; i >= 0; i--) {
                XComponent popUpLayer = (XComponent) popUpLayers.get(i);
                if (focusedLayer != popUpLayer) {
                    if (popUpLayer == popUpLayers.get(popUpLayers.size() - 1)) {
                        if (focusedView != null) {
                            XView oldView = popUpLayer.getOldActiveForm();
                            if (oldView != null) {
                                oldView.setFocused(false);
                            }
                            popUpLayer.setOldActiveForm(focusedView);
                        }
                        popUpLayer.sendClosePopUpEvent();
                    }
                }
            }
        }
    }

    /**
    * Closes all pop-ups
    */
    public static void closeAllPopUps() {
        Object[] popUps = popUpLayers.toArray();
        for (int i = 0; i < popUps.length; i++) {
            XComponent popUp = (XComponent) popUps[i];
            popUp.sendClosePopUpEvent();
        }
    }

    /**
    * Requests focus on the viewer frame AWT component
    */
    public void requestViewerFocus() {
        viewer.getFocusableView().requestFocus();
    }

    /**
    * @see XCache#setMaxCacheSize(int)
    */
    public void setResourceCacheSize(int cacheSize) {
        resourceCache.setMaxCacheSize(cacheSize);
    }

    /**
    * @see onepoint.util.XCache#getMaxCacheSize()
    */
    public int getResourceCacheSize() {
        return resourceCache.getMaxCacheSize();
    }

    /**
    * Clears the resource cache
    */
    public void clearResourceCache() {
        resourceCache.clear();
    }

    /**
    * Clears the image cache
    */
    public void clearImageCache() {
        imageCache.clear();
    }

    public static void setLoadingResources(boolean load) {
        synchronized (RESOURCE_MUTEX) {
            loadingResource = load;
            if (!load) {
                RESOURCE_MUTEX.notify();
            }
        }
    }

    public static boolean isLoadingResource() {
        return loadingResource;
    }

    public static void waitForLoadingResources() {
        synchronized (RESOURCE_MUTEX) {
            while (loadingResource) {
                try {
                    RESOURCE_MUTEX.wait(100);
                } catch (InterruptedException exc) {
                }
            }
        }
    }

    /**
    * Checks whether the given keyboard modifiers are pressed.
    *
    * @param modifiers an <code>int</code> representing the keyboard modifiers.
    * @param ctrl      a <code>boolean</code> indicating whether to check for Control.
    * @param shift     a <code>boolean</code> indicating whether to check for Shift.
    * @param alt       a <code>boolean</code> indicating whether to check for Alt.
    * @return <code>true</code> if only the specified modifiers are down.
    */
    public static boolean areModifiersDown(int modifiers, boolean ctrl, boolean shift, boolean alt) {
        boolean isCtrlDown = (modifiers & CTRL_KEY_DOWN) == CTRL_KEY_DOWN;
        boolean isShiftDown = (modifiers & SHIFT_KEY_DOWN) == SHIFT_KEY_DOWN;
        boolean isAltDown = (modifiers & ALT_KEY_DOWN) == ALT_KEY_DOWN;
        return (ctrl == isCtrlDown) & (shift == isShiftDown) & (alt == isAltDown);
    }

    /**
    * @see XView#processFocusEvent(java.util.HashMap,int)
    */
    public void processFocusEvent(HashMap event, int action) {
        XView focusedView = this.getDisplayFocusedView();
        if (focusedView != null) {
            if (action == FOCUS_GAINED) {
                focusedView.requestFocus();
                focusedView.processFocusEvent(event, action);
            } else {
                focusedView.processFocusEvent(event, action);
                focusedView.setFocused(false);
                focusedView.repaint();
            }
        }
    }

    /**
    * Custom file filter used by the file chooser
    */
    private class XCompoundFileFilter implements FilenameFilter {

        private List filters;

        public XCompoundFileFilter(List filters) {
            this.filters = filters;
        }

        public boolean accept(File dir, String name) {
            Iterator iter = filters.iterator();
            while (iter.hasNext()) {
                XFileFilter filter = (XFileFilter) iter.next();
                if (filter.accept(new File(dir, name))) {
                    return true;
                }
            }
            return false;
        }

        public String getExtensions() {
            StringBuffer buffer = new StringBuffer();
            Iterator iter = filters.iterator();
            while (iter.hasNext()) {
                XFileFilter filter = (XFileFilter) iter.next();
                buffer.append(filter.getExtensionsString());
                if (iter.hasNext()) {
                    buffer.append("; ");
                }
            }
            return buffer.toString();
        }
    }

    /**
    * Custom file filter used by the file chooser
    */
    private class XFileFilter extends FileFilter {

        /**
       * Indicates the file types which should be accepted by the filter in the form (*.extension)
       */
        private String fileTypes = null;

        /**
       * A list of the accepted extensions, taken from the fileTypes
       */
        private List acceptedExtensions = new ArrayList();

        private String description;

        /**
       *
       */
        public XFileFilter(String description, String fileTypes) {
            this.description = description;
            setFileTypes(fileTypes);
        }

        /**
       * Sets the accepted file types for the filter.
       *
       * @param fileTypes a <code>String</code> representing an array of accepted file types.
       */
        public void setFileTypes(String fileTypes) {
            this.fileTypes = fileTypes;
            this.acceptedExtensions.clear();
            StringTokenizer tokenizer = new StringTokenizer(fileTypes, ",");
            while (tokenizer.hasMoreTokens()) {
                String fileType = tokenizer.nextToken();
                if (fileType.lastIndexOf('.') != -1 && fileType.lastIndexOf('.') < fileType.length() - 1) {
                    acceptedExtensions.add(fileType.substring(fileType.lastIndexOf('.') + 1).toLowerCase());
                } else {
                    acceptedExtensions.add(fileType.toLowerCase());
                }
            }
        }

        /**
       * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
       */
        public boolean accept(File file) {
            if (file.isDirectory() && !file.isHidden()) {
                return true;
            }
            String fileExtension = file.getName();
            int pointPos = fileExtension.lastIndexOf('.');
            if (pointPos != -1 && pointPos < fileExtension.length() - 1) {
                fileExtension = fileExtension.substring(pointPos + 1).toLowerCase();
            }
            fileExtension = fileExtension.toLowerCase();
            return acceptedExtensions.contains(fileExtension);
        }

        /**
       * @see javax.swing.filechooser.FileFilter#getDescription()
       */
        public String getDescription() {
            if (description == null) {
                return fileTypes;
            }
            return (description);
        }

        public String getExtensionsString() {
            StringBuffer buffer = new StringBuffer();
            Iterator iter = acceptedExtensions.iterator();
            while (iter.hasNext()) {
                String ext = (String) iter.next();
                buffer.append("*.");
                buffer.append(ext);
                if (iter.hasNext()) {
                    buffer.append("; ");
                }
            }
            return buffer.toString();
        }
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new NotSerializableException("XDisplay can't be serialized");
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throw new NotSerializableException("XDisplay can't be serialized");
    }

    /**
    * Filters a map of given formats, by excluding unsupported formats.
    *
    * @param requestedFormats Map of formats to filter.
    * @param read             Exclude formats that can't be read.
    * @param write            Exclude formats that can't be wrote.
    * @return new map of filtered formats
    */
    public static Map filterSupportedFileFormats(Map requestedFormats, boolean read, boolean write) {
        Map resultFormats = new HashMap();
        Set formats = requestedFormats.keySet();
        Iterator iterator = formats.iterator();
        while (iterator.hasNext()) {
            String format = (String) iterator.next();
            Iterator writer = ImageIO.getImageWritersByFormatName(format);
            Iterator reader = ImageIO.getImageReadersByFormatName(format);
            if ((write && !writer.hasNext()) || (read && !reader.hasNext())) {
                continue;
            } else {
                resultFormats.put(format, requestedFormats.get(format));
            }
        }
        return resultFormats;
    }
}
