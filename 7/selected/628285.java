package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.RunnableCompatibility;
import org.eclipse.swt.internal.browser.OS;
import org.eclipse.swt.internal.xhtml.Clazz;
import org.eclipse.swt.internal.xhtml.Element;
import org.eclipse.swt.internal.xhtml.HTMLEvent;
import org.eclipse.swt.internal.xhtml.document;

/**
 * Instances of this class provide a selectable user interface object
 * that displays a hierarchy of items and issue notification when an
 * item in the hierarchy is selected.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>TreeItem</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add <code>Control</code> children to it,
 * or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SINGLE, MULTI, CHECK, FULL_SELECTION</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection, Collapse, Expand</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles SINGLE and MULTI may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * 
 * @j2sPrefix
 * $WTC$$.registerCSS ("$wt.widgets.Tree");
 */
public class Tree extends Composite {

    TreeItem[] items;

    TreeColumn[] columns;

    ImageList imageList;

    boolean dragStarted, gestureCompleted, insertAfter;

    boolean ignoreSelect, ignoreExpand, ignoreDeselect, ignoreResize;

    boolean lockSelection, oldSelected, newSelected;

    boolean linesVisible, customDraw, printClient;

    static final int INSET = 3;

    static final int GRID_WIDTH = 1;

    static final int HEADER_MARGIN = 10;

    TreeItem[] selections;

    TreeItem lastSelection;

    TreeItem[] directChildrens;

    boolean headerVisible, lineVisible;

    int focusIndex = -1;

    TreeItem focusItem;

    private Element tableHandle;

    private Element theadHandle;

    Element tbody;

    private Object hTreeKeyDown;

    /**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a composite control which will be the parent of the new instance (cannot be null)
 * @param style the style of control to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see SWT#SINGLE
 * @see SWT#MULTI
 * @see SWT#CHECK
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 * 
 * @j2sIgnore
 */
    public Tree(Composite parent, int style) {
        super(parent, checkStyle(style));
    }

    static int checkStyle(int style) {
        style |= SWT.H_SCROLL | SWT.V_SCROLL;
        return checkBits(style, SWT.SINGLE, SWT.MULTI, 0, 0, 0, 0);
    }

    /**
 * Adds the listener to the collection of listeners who will
 * be notified when the receiver's selection changes, by sending
 * it one of the messages defined in the <code>SelectionListener</code>
 * interface.
 * <p>
 * When <code>widgetSelected</code> is called, the item field of the event object is valid.
 * If the reciever has <code>SWT.CHECK</code> style set and the check selection changes,
 * the event object detail field contains the value <code>SWT.CHECK</code>.
 * <code>widgetDefaultSelected</code> is typically called when an item is double-clicked.
 * The item field of the event object is valid for default selection, but the detail field is not used.
 * </p>
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #removeSelectionListener
 * @see SelectionEvent
 */
    public void addSelectionListener(SelectionListener listener) {
        checkWidget();
        if (listener == null) error(SWT.ERROR_NULL_ARGUMENT);
        TypedListener typedListener = new TypedListener(listener);
        addListener(SWT.Selection, typedListener);
        addListener(SWT.DefaultSelection, typedListener);
    }

    /**
 * Adds the listener to the collection of listeners who will
 * be notified when an item in the receiver is expanded or collapsed
 * by sending it one of the messages defined in the <code>TreeListener</code>
 * interface.
 *
 * @param listener the listener which should be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see TreeListener
 * @see #removeTreeListener
 */
    public void addTreeListener(TreeListener listener) {
        checkWidget();
        if (listener == null) error(SWT.ERROR_NULL_ARGUMENT);
        TypedListener typedListener = new TypedListener(listener);
        addListener(SWT.Expand, typedListener);
        addListener(SWT.Collapse, typedListener);
    }

    protected void checkSubclass() {
        if (!isValidSubclass()) error(SWT.ERROR_INVALID_SUBCLASS);
    }

    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        int width = 0, height = 0;
        if (items.length == 0 && columns.length == 0 && !headerVisible) {
        } else if (columns.length == 0 && !headerVisible) {
            width = 10 + 13;
            height = 2;
            height += 14 * items.length + 2 * (items.length - 1);
            height += (getHeaderVisible() ? 14 : 0);
            int maxWidth = 1;
            for (int i = 0; i < items.length; i++) {
                String text = items[i].getText();
                if (text != null) {
                    maxWidth = Math.max(1 + OS.getStringPlainWidth(text), maxWidth);
                }
            }
            width += maxWidth - 1;
        } else {
            width = 0;
            for (int i = 0; i < columns.length; i++) {
                int colWidth = columns[i].getWidth();
                width += colWidth;
            }
            height = 2;
            height += 14 * items.length + 2 * (items.length - 1);
            height += (getHeaderVisible() ? 14 + 3 : 0);
        }
        if (width == 0) width = DEFAULT_WIDTH;
        if (height == 0) height = DEFAULT_HEIGHT;
        if (wHint != SWT.DEFAULT) width = wHint;
        if (hHint != SWT.DEFAULT) height = hHint;
        int border = getBorderWidth();
        width += border * 2;
        height += border * 2;
        if ((style & SWT.V_SCROLL) != 0) {
            width += 16;
        }
        if ((style & SWT.H_SCROLL) != 0) {
            height += 16;
        }
        return new Point(width, height);
    }

    protected void createHandle() {
        selections = new TreeItem[0];
        items = new TreeItem[0];
        super.createHandle();
        state &= ~CANVAS;
        handle.className += " tree-default";
        if ((style & SWT.V_SCROLL) != 0 && (style & SWT.H_SCROLL) != 0) {
            handle.style.overflow = "auto";
        } else {
            if ((style & SWT.V_SCROLL) != 0) {
                handle.className += " tree-v-scroll";
            } else if ((style & SWT.H_SCROLL) != 0) {
                handle.className += " tree-h-scroll";
            }
        }
        tableHandle = document.createElement("TABLE");
        String cssTable = "tree-content tree-no-columns";
        if ((style & SWT.FULL_SELECTION) != 0) {
            cssTable += " tree-full-selection";
        }
        if ((style & SWT.CHECK) != 0) {
            cssTable += " tree-check";
        }
        tableHandle.className = cssTable;
        handle.appendChild(tableHandle);
        hTreeKeyDown = new RunnableCompatibility() {

            public void run() {
                HTMLEvent evt = (HTMLEvent) getEvent();
                int index = focusIndex;
                switch(evt.keyCode) {
                    case 13:
                        TreeItem item = getItem(index);
                        if (item == null) return;
                        toggleSelection(item, evt.ctrlKey, evt.shiftKey);
                        if (item.isSelected()) {
                            Event e = new Event();
                            e.display = display;
                            e.type = SWT.DefaultSelection;
                            e.detail = SWT.NONE;
                            e.item = item;
                            e.widget = item;
                            sendEvent(e);
                            toReturn(false);
                        }
                        break;
                    case 32:
                        TreeItem item2 = getItem(index);
                        if (item2 == null) return;
                        toggleSelection(item2, evt.ctrlKey, evt.shiftKey);
                        if (item2.isSelected()) {
                            Event eDefault = new Event();
                            eDefault.display = display;
                            eDefault.type = SWT.Selection;
                            eDefault.detail = SWT.NONE;
                            eDefault.item = item2;
                            eDefault.widget = item2;
                            sendEvent(eDefault);
                            toReturn(false);
                        }
                        break;
                    case 38:
                        if (index > 0) {
                            setFocusIndex(index - 1);
                            toReturn(false);
                        }
                        break;
                    case 40:
                        if (index < getItemCount() - 1) {
                            setFocusIndex(index + 1);
                            toReturn(false);
                        }
                        break;
                    default:
                        toReturn(true);
                }
            }
        };
        Clazz.addEvent(handle, "keydown", hTreeKeyDown);
    }

    void setFocusIndex(int index) {
        checkWidget();
        if (index < 0) return;
        if (index == focusIndex) {
            return;
        }
        TreeItem item = getItem(index);
        if (item == null) {
            return;
        }
        if (this.focusItem != null) {
            OS.removeCSSClass(focusItem.handle, "tree-item-focus");
        }
        this.focusItem = item;
        this.focusIndex = index;
        OS.addCSSClass(item.handle, "tree-item-focus");
    }

    void createItem(TreeColumn column, int index) {
        if (columns == null) {
            columns = new TreeColumn[0];
        }
        if (handle == null) {
            return;
        }
        Element table = handle.childNodes[0];
        OS.removeCSSClass(handle, "tree-no-columns");
        theadHandle = null;
        for (int i = 0; i < table.childNodes.length; i++) {
            if ("THEAD".equals(table.childNodes[i].nodeName)) {
                theadHandle = table.childNodes[i];
                break;
            }
        }
        if (theadHandle == null) {
            theadHandle = document.createElement("THEAD");
            table.appendChild(theadHandle);
        }
        Element theadTR = null;
        if (theadHandle.childNodes != null && theadHandle.childNodes.length != 0) {
            for (int i = 0; i < theadHandle.childNodes.length; i++) {
                if (theadHandle.childNodes[i] != null && "TR".equals(theadHandle.childNodes[i].nodeName)) {
                    theadTR = theadHandle.childNodes[i];
                }
            }
        }
        if (theadTR == null) {
            theadTR = document.createElement("TR");
            theadHandle.appendChild(theadTR);
            Element theadTD = document.createElement("TD");
            theadTD.className = "tree-column-last";
            theadTR.appendChild(theadTD);
            createCSSElement(theadTD, "tree-head-text").appendChild(document.createTextNode("" + (char) 160));
        }
        Element theadTD = document.createElement("TD");
        theadTR.insertBefore(theadTD, theadTR.childNodes[theadTR.childNodes.length - 1]);
        createCSSElement(theadTD, "tree-head-text").appendChild(document.createTextNode("" + (char) 160));
        if (index < 0 || index >= theadTR.childNodes.length) {
            theadTR.appendChild(theadTD);
            columns[index] = column;
        } else {
            theadTR.insertBefore(theadTD, theadTR.childNodes[index]);
            for (int i = columns.length; i > index; i--) {
                columns[i] = columns[i - 1];
            }
            columns[index] = column;
            for (int i = 0; i < items.length; i++) {
            }
        }
        column.handle = theadTD;
    }

    private Element createCSSElement(Object parent, String css) {
        Element div = document.createElement("DIV");
        div.className = css;
        if (parent != null) {
            ((Element) parent).appendChild(div);
        }
        return div;
    }

    void createItem(TreeItem item, Object hParent, int index) {
        if (items == null) {
            items = new TreeItem[0];
        }
        TreeItem[] itemList = (hParent == null ? directChildrens : item.parentItem.items);
        int idx = -1;
        if (index < 0 || index >= itemList.length) {
            if (hParent == null) {
                idx = items.length;
            } else {
                if (itemList.length == 0) {
                    idx = item.parentItem.index + 1;
                } else {
                    idx = findSiblingNextItem(item.parentItem.index);
                    if (idx != -1) {
                    } else {
                        idx = items.length;
                    }
                }
            }
            index = itemList.length;
            itemList[index] = item;
        } else {
            idx = findSiblingNextItem(itemList[index].index);
            if (idx != -1) {
            } else {
                idx = items.length;
            }
            for (int i = itemList.length; i > index; i--) {
                itemList[i] = itemList[i - 1];
            }
            itemList[index] = item;
        }
        for (int i = items.length - 1; i >= idx; i--) {
            items[i + 1] = items[i];
            items[i + 1].index = i + 1;
        }
        items[idx] = item;
        item.index = idx;
        Element table = handle.childNodes[0];
        if (tbody == null) {
            tbody = document.createElement("TBODY");
            table.appendChild(tbody);
        }
        Element tbodyTR = document.createElement("TR");
        item.handle = tbodyTR;
        if (idx == 0) {
            tbodyTR.className = "tree-row-first";
            if (tbody.childNodes.length != 0) {
                tbody.childNodes[0].className = "";
            }
        }
        if (idx >= tbody.childNodes.length || tbody.childNodes[idx] == null) {
            tbody.appendChild(tbodyTR);
        } else {
            tbody.insertBefore(tbodyTR, tbody.childNodes[idx]);
        }
        Element td = document.createElement("TD");
        td.className = "tree-column-first";
        tbodyTR.appendChild(td);
        Element treeLine = createCSSElement(td, "tree-line");
        if (OS.isIE && columns != null && columns[0] != null && columns[0].cachedWidth != 0) {
            treeLine.style.width = columns[0].cachedWidth + "px";
        }
        Element lineWrapper = createCSSElement(treeLine, "tree-line-wrapper");
        TreeItem[] chains = new TreeItem[0];
        chains[0] = item;
        TreeItem parentItem = item.getParentItem();
        while (parentItem != null) {
            chains[chains.length] = parentItem;
            parentItem = parentItem.getParentItem();
        }
        item.depth = chains.length;
        TreeItem lastItem = null;
        for (int i = chains.length - 1; i >= 0; i--) {
            TreeItem currentItem = chains[i];
            String cssClass = "tree-anchor";
            if ((style & SWT.RIGHT_TO_LEFT) != 0) {
                cssClass += " tree-anchor-rtl";
            }
            TreeItem[] listItems = (lastItem == null ? directChildrens : lastItem.items);
            if (listItems.length > 1) {
                int j = 0;
                boolean isNoBreak = true;
                for (; j < listItems.length; j++) {
                    if (listItems[j] == currentItem) {
                        isNoBreak = false;
                        break;
                    }
                }
                if (isNoBreak) {
                }
                if (j == listItems.length - 1) {
                    if (i == 0) {
                        cssClass += " tree-anchor-end";
                    } else {
                        cssClass += " tree-anchor-single";
                    }
                } else if (j == 0) {
                    if (i == 0) {
                        cssClass += " tree-anchor-begin";
                    } else if (listItems.length == 1) {
                        cssClass += " tree-anchor-single";
                    } else {
                        cssClass += " tree-anchor-middle";
                    }
                } else {
                    cssClass += " tree-anchor-middle";
                }
            } else if (hParent != null && i == 0) {
                cssClass += " tree-anchor-end";
            } else {
                cssClass += " tree-anchor-single";
            }
            Element anchor = createCSSElement(lineWrapper, cssClass);
            if (OS.isIE && (style & SWT.RIGHT_TO_LEFT) != 0) {
                anchor.style.position = "static";
            }
            createCSSElement(anchor, "tree-anchor-v");
            cssClass = "tree-anchor-h";
            if (i == 0) {
                if (currentItem.items == null || currentItem.items.length == 0) {
                    cssClass += " tree-anchor-line";
                } else {
                    cssClass += " tree-anchor-plus";
                }
            }
            createCSSElement(anchor, cssClass);
            lastItem = currentItem;
        }
        Element textEl = createCSSElement(lineWrapper, "tree-text");
        if ((style & SWT.RIGHT_TO_LEFT) != 0) textEl.className += " tree-text-rtl";
        if ((style & SWT.CHECK) != 0) {
            Element input = document.createElement("INPUT");
            input.type = "checkbox";
            input.className = "tree-check-box image-p-4";
            textEl.appendChild(input);
            item.checkElement = input;
        }
        createCSSElement(textEl, "tree-text-inner");
        int length = Math.max(1, this.columns.length);
        for (int i = 1; i < length; i++) {
            td = document.createElement("TD");
            createCSSElement(td, "tree-text-inner");
            tbodyTR.appendChild(td);
        }
        td = document.createElement("TD");
        td.className = "tree-column-last";
        createCSSElement(td, "tree-text-inner");
        tbodyTR.appendChild(td);
        if (item.parentItem != null) {
        }
        int elIndex = chains.length - 1;
        if (index == itemList.length - 1) {
            int prevIndex = 0;
            if (itemList.length == 1) {
                prevIndex = (hParent == null ? 0 : item.parentItem.index + 1);
            } else {
                prevIndex = itemList[index - 1].index;
            }
            for (int k = prevIndex; k < item.index; k++) {
                TreeItem ti = items[k];
                Element anchor = ti.handle.childNodes[0].childNodes[0].childNodes[0].childNodes[elIndex];
                String cssClass = "tree-anchor";
                if ((style & SWT.RIGHT_TO_LEFT) != 0) {
                    cssClass += " tree-anchor-rtl";
                }
                if (ti.parentItem == item.parentItem) {
                    int i = 0;
                    for (i = 0; i < itemList.length; i++) {
                        if (ti == itemList[i]) {
                            break;
                        }
                    }
                    if (i == 0) {
                        if (ti.parentItem == null) {
                            cssClass += " tree-anchor-begin";
                        } else if (itemList.length > 1) {
                            cssClass += " tree-anchor-middle";
                        } else {
                            cssClass += " tree-anchor-end";
                        }
                    } else if (i == itemList.length - 1) {
                        cssClass += " tree-anchor-end";
                    } else {
                        cssClass += " tree-anchor-middle";
                    }
                } else {
                    cssClass += " tree-anchor-middle";
                }
                anchor.className = cssClass;
            }
            if (itemList.length == 1 && hParent != null) {
                Element[] parentInnerChildren = item.parentItem.handle.childNodes[0].childNodes[0].childNodes[0].childNodes;
                Element anchorV = parentInnerChildren[elIndex - 1];
                anchorV.childNodes[1].className = item.parentItem.expandStatus ? "tree-anchor-h tree-anchor-minus" : "tree-anchor-h tree-anchor-plus";
            }
        }
        boolean visible = true;
        while (item.parentItem != null && (visible = item.parentItem.getExpanded())) {
            item = item.parentItem;
        }
        if (!visible) {
            tbodyTR.style.display = "none";
        }
    }

    boolean toggleSelection(TreeItem item, boolean isCtrlKeyHold, boolean isShiftKeyHold) {
        if (item == null) {
            return false;
        }
        if ((style & SWT.MULTI) != 0 && (isCtrlKeyHold || isShiftKeyHold)) {
            if (isCtrlKeyHold) {
                for (int i = 0; i < selections.length; i++) {
                    if (item == selections[i]) {
                        TreeItem[] newSelections = new TreeItem[selections.length];
                        for (int j = 0; j < i; j++) {
                            newSelections[j] = selections[j];
                        }
                        for (int j = i; j < selections.length - 1; j++) {
                            newSelections[j] = selections[j + 1];
                        }
                        selections = newSelections;
                        item.showSelection(false);
                        lastSelection = item;
                        return false;
                    }
                }
                selections[selections.length] = item;
                lastSelection = item;
                item.showSelection(true);
            } else {
                for (int i = 0; i < selections.length; i++) {
                    if (selections[i] != null) {
                        selections[i].showSelection(false);
                    }
                }
                if (lastSelection != null) {
                    int idx1 = Math.min(lastSelection.index, item.index);
                    int idx2 = Math.max(lastSelection.index, item.index);
                    selections = new TreeItem[0];
                    for (int i = idx1; i <= idx2; i++) {
                        TreeItem ti = items[i];
                        if (ti.handle.style.display != "none") {
                            selections[selections.length] = ti;
                            ti.showSelection(true);
                        }
                    }
                    return true;
                } else {
                    if (selections.length != 1) {
                        selections = new TreeItem[1];
                    }
                    selections[0] = item;
                }
            }
        } else {
            item.showSelection(true);
            for (int i = 0; i < selections.length; i++) {
                if (selections[i] != null && selections[i] != item) {
                    selections[i].showSelection(false);
                }
            }
            if (selections.length != 1) {
                selections = new TreeItem[1];
            }
            selections[0] = item;
        }
        lastSelection = item;
        return true;
    }

    int skipItems(int index) {
        TreeItem parentItem = items[index];
        index++;
        while (items[index] != null) {
            TreeItem item = items[index];
            if (item.parentItem != parentItem) {
                if (item.parentItem == items[index - 1]) {
                    index = skipItems(index - 1);
                    if (index == -1) {
                        return -1;
                    }
                    TreeItem ti = items[index];
                    boolean outOfHierarchies = true;
                    while (ti != null) {
                        ti = ti.parentItem;
                        if (ti == parentItem) {
                            outOfHierarchies = false;
                            break;
                        }
                    }
                    if (outOfHierarchies) {
                        return index;
                    }
                } else {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    void createParent() {
        forceResize();
        register();
    }

    protected void createWidget() {
        super.createWidget();
        items = new TreeItem[0];
        columns = new TreeColumn[0];
        directChildrens = new TreeItem[0];
    }

    /**
 * Deselects all selected items in the receiver.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void deselectAll() {
        checkWidget();
        for (int i = 0; i < selections.length; i++) {
            selections[i].showSelection(false);
        }
    }

    void destroyItem(TreeColumn column) {
    }

    void destroyItem(TreeItem item) {
        int length = selections.length;
        int index = -1;
        for (int i = 0; i < length; i++) {
            if (selections[i].equals(item)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            TreeItem[] oldSelection = selections;
            selections = new TreeItem[length - 1];
            System.arraycopy(oldSelection, 0, selections, 0, index);
            System.arraycopy(oldSelection, index + 1, selections, index, length - index - 1);
        }
        boolean found = false;
        length = items.length;
        for (int i = 0; i < length; i++) {
            if (found) {
                items[i - 1] = items[i];
                items[i - 1].index = i - 1;
            }
            if (items[i].equals(item)) {
                found = true;
            }
        }
        {
        }
        updateScrollBar();
    }

    void enableWidget(boolean enabled) {
        super.enableWidget(enabled);
    }

    Widget findItem(int id) {
        return null;
    }

    /**
 * Returns the width in pixels of a grid line.
 *
 * @return the width of a grid line in pixels
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public int getGridLineWidth() {
        checkWidget();
        return GRID_WIDTH;
    }

    /**
 * Returns the height of the receiver's header 
 *
 * @return the height of the header or zero if the header is not visible
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1 
 */
    public int getHeaderHeight() {
        checkWidget();
        return 16;
    }

    /**
 * Returns <code>true</code> if the receiver's header is visible,
 * and <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the receiver's header's visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public boolean getHeaderVisible() {
        checkWidget();
        return headerVisible;
    }

    Point getImageSize() {
        if (imageList != null) return imageList.getImageSize();
        return new Point(0, getItemHeight());
    }

    /**
 * Returns the column at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 * If no <code>TreeColumn</code>s were created by the programmer,
 * this method will throw <code>ERROR_INVALID_RANGE</code> despite
 * the fact that a single column of data may be visible in the tree.
 * This occurs when the programmer uses the tree like a list, adding
 * items but never creating a column.
 *
 * @param index the index of the column to return
 * @return the column at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public TreeColumn getColumn(int index) {
        checkWidget();
        return columns[index];
    }

    /**
 * Returns the number of columns contained in the receiver.
 * If no <code>TreeColumn</code>s were created by the programmer,
 * this value is zero, despite the fact that visually, one column
 * of items may be visible. This occurs when the programmer uses
 * the tree like a list, adding items but never creating a column.
 *
 * @return the number of columns
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public int getColumnCount() {
        checkWidget();
        return columns.length;
    }

    /**
 * Returns an array of <code>TreeColumn</code>s which are the
 * columns in the receiver. If no <code>TreeColumn</code>s were
 * created by the programmer, the array is empty, despite the fact
 * that visually, one column of items may be visible. This occurs
 * when the programmer uses the tree like a list, adding items but
 * never creating a column.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items in the receiver
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public TreeColumn[] getColumns() {
        checkWidget();
        return columns;
    }

    TreeItem[] getDescendantItems(int index) {
        int nextSiblingIdx = findNextSiblingItem(index);
        if (nextSiblingIdx == -1) {
            nextSiblingIdx = items.length;
        }
        TreeItem[] children = new TreeItem[nextSiblingIdx - index - 1];
        for (int i = index + 1; i < nextSiblingIdx; i++) {
            children[i - index - 1] = items[i];
        }
        return children;
    }

    /**
 * Find direct child item in the given parent item.
 * 
 * @param parentIndex
 * @param index
 * @return index in the items array
 */
    int findItem(int parentIndex, int index) {
        if (parentIndex < 0) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].parentItem == null) {
                    if (index == 0) {
                        return i;
                    }
                    index--;
                }
            }
            return -1;
        }
        TreeItem parentItem = items[parentIndex];
        parentIndex++;
        while (items[parentIndex] != null) {
            TreeItem item = items[parentIndex];
            if (item.parentItem != parentItem) {
                if (item.parentItem == items[parentIndex - 1]) {
                    parentIndex = skipItems(parentIndex - 1);
                    if (parentIndex == -1) {
                        return -1;
                    }
                } else {
                    return -1;
                }
            } else {
                if (index == 0) {
                    return parentIndex;
                }
                index--;
            }
            parentIndex++;
        }
        return -1;
    }

    /**
 * Find index of next sibling item. If no next sibling item, return -1. 
 * @param parentIndex
 * @return
 */
    int findNextSiblingItem(int parentIndex) {
        if (parentIndex < 0) {
            parentIndex = 0;
        }
        TreeItem parentItem = items[parentIndex];
        parentIndex++;
        if (items[parentIndex] != null) {
            TreeItem item = items[parentIndex];
            if (item.parentItem != parentItem.parentItem) {
                if (item.parentItem == items[parentIndex - 1]) {
                    parentIndex = skipItems(parentIndex - 1);
                    if (parentIndex == -1) {
                        return -1;
                    }
                    TreeItem ti = items[parentIndex];
                    boolean outOfHierarchies = true;
                    while (ti != null) {
                        ti = ti.parentItem;
                        if (ti == parentItem) {
                            outOfHierarchies = false;
                            break;
                        }
                    }
                    if (outOfHierarchies) {
                        return parentIndex;
                    }
                } else {
                    return -1;
                }
            } else {
                return parentIndex;
            }
        }
        return -1;
    }

    int findSiblingNextItem(int parentIndex) {
        if (parentIndex < 0) {
            parentIndex = 0;
        }
        TreeItem parentItem = items[parentIndex];
        parentIndex++;
        if (items[parentIndex] != null) {
            TreeItem item = items[parentIndex];
            if (item.parentItem != parentItem.parentItem) {
                if (item.parentItem == items[parentIndex - 1]) {
                    parentIndex = skipItems(parentIndex - 1);
                    if (parentIndex == -1) {
                        return -1;
                    }
                    TreeItem ti = items[parentIndex];
                    boolean outOfHierarchies = true;
                    while (ti != null) {
                        ti = ti.parentItem;
                        if (ti == parentItem) {
                            outOfHierarchies = false;
                            break;
                        }
                    }
                    if (outOfHierarchies) {
                        return parentIndex;
                    }
                } else {
                    return parentIndex;
                }
            } else {
                return parentIndex;
            }
        }
        return -1;
    }

    int indexOf(int parentIndex, TreeItem ti) {
        int index = 0;
        if (parentIndex < 0) {
            if (ti.parentItem != null) {
                return -1;
            }
            for (int i = 0; i < items.length; i++) {
                if (items[i] == ti) {
                    return index;
                } else if (items[i].parentItem == null) {
                    index++;
                }
            }
            return -1;
        }
        TreeItem parentItem = items[parentIndex];
        parentIndex++;
        while (items[parentIndex] != null) {
            TreeItem item = items[parentIndex];
            if (item.parentItem != parentItem) {
                if (item.parentItem == items[parentIndex - 1]) {
                    parentIndex = skipItems(parentIndex - 1);
                    if (parentIndex == -1) {
                        return -1;
                    }
                    if (items[parentIndex].parentItem == parentItem.parentItem) {
                        return -1;
                    } else {
                        if (items[parentIndex] == ti) {
                            return index;
                        }
                        index++;
                    }
                } else {
                    return -1;
                }
            } else {
                if (item == ti) {
                    return index;
                }
                index++;
            }
            parentIndex++;
        }
        return -1;
    }

    /**
 * Returns the item at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 *
 * @param index the index of the item to return
 * @return the item at the given index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public TreeItem getItem(int index) {
        checkWidget();
        if (index < 0) error(SWT.ERROR_INVALID_RANGE);
        return getItems()[index];
    }

    /**
 * Returns the item at the given point in the receiver
 * or null if no such item exists. The point is in the
 * coordinate system of the receiver.
 *
 * @param point the point used to locate the item
 * @return the item at the given point
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public TreeItem getItem(Point point) {
        checkWidget();
        if (point == null) error(SWT.ERROR_NULL_ARGUMENT);
        return null;
    }

    /**
 * Returns the number of items contained in the receiver
 * that are direct item children of the receiver.  The
 * number that is returned is the number of roots in the
 * tree.
 *
 * @return the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getItemCount() {
        checkWidget();
        return getItems().length;
    }

    int getItemCount(int hItem) {
        return items.length;
    }

    /**
 * Returns the height of the area which would be used to
 * display <em>one</em> of the items in the tree.
 *
 * @return the height of one item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getItemHeight() {
        checkWidget();
        return 16;
    }

    /**
 * Returns a (possibly empty) array of items contained in the
 * receiver that are direct item children of the receiver.  These
 * are the roots of the tree.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public TreeItem[] getItems() {
        checkWidget();
        TreeItem[] copiedItems = new TreeItem[0];
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].parentItem == null) {
                copiedItems[copiedItems.length] = items[i];
            }
        }
        return copiedItems;
    }

    TreeItem[] getItems(int hTreeItem) {
        TreeItem[] children = new TreeItem[0];
        if (hTreeItem < 0) {
            hTreeItem = 0;
        }
        TreeItem parentItem = items[hTreeItem];
        hTreeItem++;
        while (items[hTreeItem] != null) {
            TreeItem item = items[hTreeItem];
            if (item.parentItem != parentItem) {
                if (item.parentItem == items[hTreeItem - 1]) {
                    hTreeItem = skipItems(hTreeItem - 1);
                    if (hTreeItem == -1) {
                        return children;
                    }
                    if (items[hTreeItem].parentItem == parentItem.parentItem) {
                        return children;
                    } else {
                        children[children.length] = items[hTreeItem];
                    }
                } else {
                    return children;
                }
            } else {
                children[children.length] = item;
            }
            hTreeItem++;
        }
        return children;
    }

    /**
 * Returns <code>true</code> if the receiver's lines are visible,
 * and <code>false</code> otherwise.
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, this method
 * may still indicate that it is considered visible even though
 * it may not actually be showing.
 * </p>
 *
 * @return the visibility state of the lines
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public boolean getLinesVisible() {
        checkWidget();
        return linesVisible;
    }

    /**
 * Returns the receiver's parent item, which must be a
 * <code>TreeItem</code> or null when the receiver is a
 * root.
 *
 * @return the receiver's parent item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public TreeItem getParentItem() {
        checkWidget();
        return null;
    }

    /**
 * Returns an array of <code>TreeItem</code>s that are currently
 * selected in the receiver. The order of the items is unspecified.
 * An empty array indicates that no items are selected. 
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its selection, so modifying the array will
 * not affect the receiver. 
 * </p>
 * @return an array representing the selection
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public TreeItem[] getSelection() {
        checkWidget();
        return selections;
    }

    /**
 * Returns the number of selected items contained in the receiver.
 *
 * @return the number of selected items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getSelectionCount() {
        checkWidget();
        return selections.length;
    }

    /**
 * Returns the item which is currently at the top of the receiver.
 * This item can change when items are expanded, collapsed, scrolled
 * or new items are added or removed.
 *
 * @return the item at the top of the receiver 
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 2.1
 */
    public TreeItem getTopItem() {
        checkWidget();
        return items[0];
    }

    /**
 * Searches the receiver's list starting at the first column
 * (index 0) until a column is found that is equal to the 
 * argument, and returns the index of that column. If no column
 * is found, returns -1.
 *
 * @param column the search column
 * @return the index of the column
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public int indexOf(TreeColumn column) {
        checkWidget();
        if (column == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (column.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        int count = columns.length;
        for (int i = 0; i < count; i++) {
            if (columns[i] == column) return i;
        }
        return -1;
    }

    /**
 * Searches the receiver's list starting at the first item
 * (index 0) until an item is found that is equal to the 
 * argument, and returns the index of that item. If no item
 * is found, returns -1.
 *
 * @param item the search item
 * @return the index of the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the tool item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the tool item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public int indexOf(TreeItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (item.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == item) {
                return i;
            }
        }
        return -1;
    }

    protected void releaseHandle() {
        if (hTreeKeyDown != null) {
            Clazz.removeEvent(handle, "keydown", hTreeKeyDown);
            hTreeKeyDown = null;
        }
        if (theadHandle != null) {
            OS.deepClearChildren(theadHandle);
            OS.destroyHandle(theadHandle);
            theadHandle = null;
        }
        if (tbody != null) {
            OS.deepClearChildren(tbody);
            OS.destroyHandle(tbody);
            tbody = null;
        }
        if (tableHandle != null) {
            OS.deepClearChildren(tableHandle);
            OS.destroyHandle(tableHandle);
            tableHandle = null;
        }
        focusItem = null;
        lastSelection = null;
        directChildrens = null;
        selections = null;
        super.releaseHandle();
    }

    protected void releaseWidget() {
        int columnCount = columns.length;
        for (int i = 0; i < items.length; i++) {
            TreeItem item = items[i];
            if (item != null && !item.isDisposed()) {
                item.releaseResources();
            }
        }
        items = null;
        for (int i = 0; i < columnCount; i++) {
            TreeColumn column = columns[i];
            if (!column.isDisposed()) column.releaseResources();
        }
        columns = null;
        super.releaseWidget();
    }

    /**
 * Removes all of the items from the receiver.
 * <p>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void removeAll() {
        checkWidget();
        ignoreDeselect = ignoreSelect = true;
        TreeItem[] items = getItems();
        int length = items.length;
        for (int i = 0; i < length; i++) {
            items[i].dispose();
        }
        this.items = new TreeItem[0];
        updateScrollBar();
    }

    /**
 * Removes the listener from the collection of listeners who will
 * be notified when the receiver's selection changes.
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SelectionListener
 * @see #addSelectionListener
 */
    public void removeSelectionListener(SelectionListener listener) {
        checkWidget();
        if (listener == null) error(SWT.ERROR_NULL_ARGUMENT);
        eventTable.unhook(SWT.Selection, listener);
        eventTable.unhook(SWT.DefaultSelection, listener);
    }

    /**
 * Removes the listener from the collection of listeners who will
 * be notified when items in the receiver are expanded or collapsed..
 *
 * @param listener the listener which should no longer be notified
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see TreeListener
 * @see #addTreeListener
 */
    public void removeTreeListener(TreeListener listener) {
        checkWidget();
        if (listener == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (eventTable == null) return;
        eventTable.unhook(SWT.Expand, listener);
        eventTable.unhook(SWT.Collapse, listener);
    }

    /**
 * Display a mark indicating the point at which an item will be inserted.
 * The drop insert item has a visual hint to show where a dragged item 
 * will be inserted when dropped on the tree.
 * 
 * @param item the insert item.  Null will clear the insertion mark.
 * @param before true places the insert mark above 'item'. false places 
 *	the insert mark below 'item'.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void setInsertMark(TreeItem item, boolean before) {
        checkWidget();
    }

    /**
 * Marks the receiver's lines as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise. 
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param show the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public void setLinesVisible(boolean show) {
        checkWidget();
        if (linesVisible == show) return;
        linesVisible = show;
    }

    /**
 * Selects all of the items in the receiver.
 * <p>
 * If the receiver is single-select, do nothing.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void selectAll() {
        checkWidget();
        if ((style & SWT.SINGLE) != 0) return;
    }

    void setBounds(int x, int y, int width, int height, int flags) {
        super.setBounds(x, y, width, height, flags);
    }

    void setCheckboxImageList() {
        if ((style & SWT.CHECK) == 0) return;
    }

    /**
 * Marks the receiver's header as visible if the argument is <code>true</code>,
 * and marks it invisible otherwise. 
 * <p>
 * If one of the receiver's ancestors is not visible or some
 * other condition makes the receiver not visible, marking
 * it visible may not actually cause it to be displayed.
 * </p>
 *
 * @param show the new visibility state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.1
 */
    public void setHeaderVisible(boolean show) {
        checkWidget();
        headerVisible = show;
        if (theadHandle != null) {
            theadHandle.style.display = (show ? "" : "none");
        }
        Element table = handle.childNodes[0];
        OS.updateCSSClass(table, "tree-no-columns", !show || columns == null || columns.length == 0);
        setScrollWidth();
        updateScrollBar();
    }

    public void setRedraw(boolean redraw) {
        checkWidget();
    }

    void setScrollWidth() {
    }

    /**
 * Sets the receiver's selection to the given item.
 * The current selection is cleared before the new item is selected.
 * <p>
 * If the item is not in the receiver, then it is ignored.
 * </p>
 *
 * @param item the item to select
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 3.2
 */
    public void setSelection(TreeItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        setSelection(new TreeItem[] { item });
    }

    /**
 * Sets the receiver's selection to be the given array of items.
 * The current selection is cleared before the new items are selected.
 * <p>
 * Items that are not in the receiver are ignored.
 * If the receiver is single-select and multiple items are specified,
 * then all items are ignored.
 *
 * @param items the array of items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of items is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if one of the items has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Tree#deselectAll()
 */
    public void setSelection(TreeItem[] items) {
        checkWidget();
        if (items == null) error(SWT.ERROR_NULL_ARGUMENT);
        int length = items.length;
        if (length == 0 || ((style & SWT.SINGLE) != 0 && length > 1)) {
            deselectAll();
            return;
        }
        this.selections = items;
        for (int i = 0; i < items.length; i++) {
            items[i].showSelection(true);
        }
    }

    /**
 * Sets the item which is currently at the top of the receiver.
 * This item can change when items are expanded, collapsed, scrolled
 * or new items are added or removed.
 *
 * @param item the item to be shown
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Tree#getTopItem()
 * 
 * @since 2.1
 */
    public void setTopItem(TreeItem item) {
        checkWidget();
        if (item == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
        if (item.isDisposed()) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        updateScrollBar();
    }

    protected boolean SetWindowPos(Object hWnd, Object hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags) {
        if (OS.isIE && (columns == null || columns[0] == null || columns[0].cachedWidth <= 0)) {
            int cachedWidth = cx - 8;
            if (columns == null || columns[0] == null) {
            } else if (columns[0].cachedWidth == 0) {
                int w = cx - 10;
                int count = 1;
                for (int i = 1; i < columns.length; i++) {
                    if (columns[i] != null && columns[i].cachedWidth > 0) {
                        w -= columns[i].cachedWidth;
                    } else {
                        count++;
                    }
                }
                cachedWidth = w / count;
            }
            for (int i = 0; i < items.length; i++) {
                items[i].handle.childNodes[0].childNodes[0].style.width = cachedWidth + "px";
            }
        }
        return super.SetWindowPos(hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
    }

    void showItem(Element hItem) {
        updateScrollBar();
    }

    /**
 * Shows the column.  If the column is already showing in the receiver,
 * this method simply returns.  Otherwise, the columns are scrolled until
 * the column is visible.
 *
 * @param column the column to be shown
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.1
 */
    public void showColumn(TreeColumn column) {
        checkWidget();
        if (column == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (column.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        if (column.parent != this) return;
        int index = indexOf(column);
        if (index == -1) return;
    }

    /**
 * Shows the item.  If the item is already showing in the receiver,
 * this method simply returns.  Otherwise, the items are scrolled
 * and expanded until the item is visible.
 *
 * @param item the item to be shown
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Tree#showSelection()
 */
    public void showItem(TreeItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (item.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        showItem(item.handle);
    }

    /**
 * Shows the selection.  If the selection is already showing in the receiver,
 * this method simply returns.  Otherwise, the items are scrolled until
 * the selection is visible.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Tree#showItem(TreeItem)
 */
    public void showSelection() {
        checkWidget();
    }

    void showWidget(boolean visible) {
        super.showWidget(visible);
    }

    void updateScrollBar() {
    }

    protected boolean useNativeScrollBar() {
        return true;
    }
}
