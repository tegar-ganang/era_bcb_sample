package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.RunnableCompatibility;
import org.eclipse.swt.internal.browser.OS;
import org.eclipse.swt.internal.xhtml.Clazz;
import org.eclipse.swt.internal.xhtml.Element;
import org.eclipse.swt.internal.xhtml.HTMLEvent;
import org.eclipse.swt.internal.xhtml.document;

/** 
 * Instances of this class implement a selectable user interface
 * object that displays a list of images and strings and issue
 * notification when selected.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>TableItem</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add <code>Control</code> children to it,
 * or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SINGLE, MULTI, CHECK, FULL_SELECTION, HIDE_SELECTION, VIRTUAL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection</dd>
 * </dl>
 * <p>
 * Note: Only one of the styles SINGLE, and MULTI may be specified.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * 
 * @j2sPrefix
 * $WTC$$.registerCSS ("$wt.widgets.Table");
 */
public class Table extends Composite {

    TableItem[] items;

    TableColumn[] columns;

    ImageList imageList;

    TableItem currentItem;

    Element tbody;

    TableItem lastSelection;

    TableItem[] selection;

    int lastIndexOf, lastWidth;

    boolean customDraw, cancelMove, dragStarted, fixScrollWidth, tipRequested;

    boolean wasSelected, ignoreActivate, ignoreSelect, ignoreShrink, ignoreResize;

    boolean headerVisible, lineVisible;

    private Element tbodyTRTemplate;

    int lineWidth;

    int focusIndex = -1;

    TableItem focusItem;

    int columnMaxWidth[];

    static final int INSET = 4;

    static final int GRID_WIDTH = 1;

    static final int HEADER_MARGIN = 10;

    private Element tableHandle;

    private Element theadHandle;

    private Object hTableKeyDown;

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
 * @see SWT#FULL_SELECTION
 * @see SWT#HIDE_SELECTION
 * @see SWT#VIRTUAL
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 * 
 * @j2sIgnore
 */
    public Table(Composite parent, int style) {
        super(parent, checkStyle(style));
    }

    TableItem _getItem(int index) {
        if (items[index] != null) return items[index];
        return items[index] = new TableItem(this, SWT.NONE, -1, false);
    }

    void enableWidget(boolean enabled) {
        super.enableWidget(enabled);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i].enableWidget(enabled);
        }
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

    static int checkStyle(int style) {
        style |= SWT.H_SCROLL | SWT.V_SCROLL;
        return checkBits(style, SWT.SINGLE, SWT.MULTI, 0, 0, 0, 0);
    }

    boolean checkData(TableItem item, boolean redraw) {
        if (item.cached) return true;
        if ((style & SWT.VIRTUAL) != 0) {
            item.cached = true;
            Event event = new Event();
            event.display = display;
            event.item = item;
            currentItem = item;
            sendEvent(SWT.SetData, event);
            currentItem = null;
            if (isDisposed() || item.isDisposed()) return false;
            if (redraw) {
                if (!setScrollWidth(item, false)) {
                    item.redraw();
                }
            }
        }
        return true;
    }

    protected void checkSubclass() {
        if (!isValidSubclass()) error(SWT.ERROR_INVALID_SUBCLASS);
    }

    /**
 * Clears the item at the given zero-relative index in the receiver.
 * The text, icon and other attributes of the item are set to the default
 * value.  If the table was created with the SWT.VIRTUAL style, these
 * attributes are requested again as needed.
 *
 * @param index the index of the item to clear
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see SWT#VIRTUAL
 * @see SWT#SetData
 * 
 * @since 3.0
 */
    public void clear(int index) {
        checkWidget();
        int count = items.length;
        if (!(0 <= index && index < count)) error(SWT.ERROR_INVALID_RANGE);
    }

    /**
 * Removes the items from the receiver which are between the given
 * zero-relative start and end indices (inclusive).  The text, icon
 * and other attribues of the items are set to their default values.
 * If the table was created with the SWT.VIRTUAL style, these attributes
 * are requested again as needed.
 *
 * @param start the start index of the item to clear
 * @param end the end index of the item to clear
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see SWT#VIRTUAL
 * @see SWT#SetData
 * 
 * @since 3.0
 */
    public void clear(int start, int end) {
        checkWidget();
        if (start > end) return;
    }

    /**
 * Clears the items at the given zero-relative indices in the receiver.
 * The text, icon and other attribues of the items are set to their default
 * values.  If the table was created with the SWT.VIRTUAL style, these
 * attributes are requested again as needed.
 *
 * @param indices the array of indices of the items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 *    <li>ERROR_NULL_ARGUMENT - if the indices array is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see SWT#VIRTUAL
 * @see SWT#SetData
 * 
 * @since 3.0
 */
    public void clear(int[] indices) {
        checkWidget();
        if (indices == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (indices.length == 0) return;
    }

    /**
 * Clears all the items in the receiver. The text, icon and other
 * attribues of the items are set to their default values. If the
 * table was created with the SWT.VIRTUAL style, these attributes
 * are requested again as needed.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see SWT#VIRTUAL
 * @see SWT#SetData
 * 
 * @since 3.0
 */
    public void clearAll() {
        checkWidget();
    }

    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        int width = 0;
        int height = 0;
        if (items.length == 0 && columns.length == 0) {
            width = 10;
            height = 2;
        } else if (columns.length == 0) {
            width = 10;
            height = 2;
            height += 14 * items.length;
            height += (getHeaderVisible() ? 14 : 0);
            int maxWidth = 1;
            for (int i = 0; i < items.length; i++) {
                String text = items[i].getText();
                if (text != null) {
                    maxWidth = Math.max(OS.getStringPlainWidth(text), maxWidth);
                }
            }
            width += maxWidth - 1;
        } else {
            width = 0;
            for (int i = 0; i < columns.length; i++) {
                width += columns[i].getWidth();
            }
            height = 2;
            height += 14 * items.length;
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
        selection = new TableItem[0];
        items = new TableItem[0];
        columns = new TableColumn[0];
        columnMaxWidth = new int[0];
        lineWidth = 0;
        tbody = null;
        super.createHandle();
        state &= ~CANVAS;
        handle.className += " table-default";
        if ((style & SWT.V_SCROLL) != 0 && (style & SWT.H_SCROLL) != 0) {
            handle.style.overflow = "auto";
        } else {
            if ((style & SWT.V_SCROLL) != 0) {
                handle.className += " table-v-scroll";
            } else if ((style & SWT.H_SCROLL) != 0) {
                handle.className += " table-h-scroll";
            }
        }
        tableHandle = document.createElement("TABLE");
        String cssTable = "table-content";
        if ((style & SWT.FULL_SELECTION) != 0) {
            cssTable += " table-full-selection";
        }
        if ((style & SWT.CHECK) != 0) {
            cssTable += " table-check";
        }
        tableHandle.className = cssTable;
        handle.appendChild(tableHandle);
        hTableKeyDown = new RunnableCompatibility() {

            public void run() {
                HTMLEvent evt = (HTMLEvent) getEvent();
                int index = focusIndex;
                switch(evt.keyCode) {
                    case 13:
                        TableItem item = getItem(index);
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
                        TableItem item2 = getItem(index);
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
        Clazz.addEvent(handle, "keydown", hTableKeyDown);
    }

    private Element createCSSElement(Object parent, String css) {
        Element div = document.createElement("DIV");
        div.className = css;
        if (parent != null) {
            ((Element) parent).appendChild(div);
        }
        return div;
    }

    void createItem(TableColumn column, int index) {
        if (columns == null) {
            columns = new TableColumn[0];
        }
        if (handle == null) {
            return;
        }
        Element table = handle.childNodes[0];
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
            theadTD.className = "table-column-last";
            theadTR.appendChild(theadTD);
            createCSSElement(theadTD, "table-head-text").appendChild(document.createTextNode("" + (char) 160));
        }
        Element theadTD = document.createElement("TD");
        theadTD.innerHTML = "<div class=\"table-head-text\">&#160;</div>";
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

    void createItem(TableItem item, int index) {
        if (items == null) {
            items = new TableItem[0];
        }
        item.index = index;
        items[index] = item;
        if (handle == null) {
            return;
        }
        Element table = handle.childNodes[0];
        if (tbody == null) for (int i = 0; i < table.childNodes.length; i++) {
            if ("TBODY".equals(table.childNodes[i].nodeName)) {
                tbody = table.childNodes[i];
                break;
            }
        }
        if (tbody == null) {
            tbody = document.createElement("TBODY");
            table.appendChild(tbody);
        }
        if (tbodyTRTemplate == null) {
            tbodyTRTemplate = document.createElement("TR");
            int length = Math.max(1, this.columns.length);
            Element td = document.createElement("TD");
            td.className = "table-column-first";
            String str = "<div class=\"table-text\">";
            boolean isRTL = (style & SWT.RIGHT_TO_LEFT) != 0;
            if ((style & SWT.CHECK) != 0) {
                String checkClass = (isRTL && false) ? "table-check-box-rtl image-p-4" : "table-check-box image-p-4";
                str += "<input class=\"" + checkClass + "\" type=\"checkbox\"/>";
            }
            str += "<div class=\"table-text-inner\"></div>";
            str += "</div>";
            tbodyTRTemplate.appendChild(td);
            td.innerHTML = str;
            str = "";
            for (int i = 1; i < length; i++) {
                td = document.createElement("TD");
                td.innerHTML = "<div class=\"table-text-inner\"></div>";
                tbodyTRTemplate.appendChild(td);
            }
            td = document.createElement("TD");
            td.className = "table-column-last";
            td.innerHTML = "<div class=\"table-text-inner\"></div>";
            tbodyTRTemplate.appendChild(td);
        }
        Element tbodyTR = tbodyTRTemplate.cloneNode(true);
        if ((style & SWT.CHECK) != 0) {
            Element[] nl = tbodyTR.getElementsByTagName("INPUT");
            item.check = (Element) nl[0];
        }
        if (index == 0) {
            OS.addCSSClass(tbodyTR, "table-row-first");
        }
        if (index < 0 || index >= tbody.childNodes.length) {
            tbody.appendChild(tbodyTR);
            items[index] = item;
        } else {
            tbody.insertBefore(tbodyTR, tbody.childNodes[index]);
            for (int i = items.length; i > index; i--) {
                items[i] = items[i - 1];
                items[i].index = i;
            }
            items[index] = item;
        }
        item.handle = tbodyTR;
    }

    protected void createWidget() {
        super.createWidget();
        items = new TableItem[0];
        columns = new TableColumn[0];
        if ((style & SWT.VIRTUAL) != 0) customDraw = true;
    }

    /**
 * Deselects the items at the given zero-relative indices in the receiver.
 * If the item at the given zero-relative index in the receiver 
 * is selected, it is deselected.  If the item at the index
 * was not selected, it remains deselected. Indices that are out
 * of range and duplicate indices are ignored.
 *
 * @param indices the array of indices for the items to deselect
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the set of indices is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void deselect(int[] indices) {
        checkWidget();
        if (indices == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (indices.length == 0) return;
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] >= 0) {
                items[indices[i]].showSelection(false);
            }
        }
        removeFromSelection(indices);
    }

    /**
 * Deselects the item at the given zero-relative index in the receiver.
 * If the item at the index was already deselected, it remains
 * deselected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to deselect
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void deselect(int index) {
        checkWidget();
        if (index < 0) return;
        items[index].showSelection(false);
        removeFromSelection(new int[] { index });
    }

    /**
 * Deselects the items at the given zero-relative indices in the receiver.
 * If the item at the given zero-relative index in the receiver 
 * is selected, it is deselected.  If the item at the index
 * was not selected, it remains deselected.  The range of the
 * indices is inclusive. Indices that are out of range are ignored.
 *
 * @param start the start index of the items to deselect
 * @param end the end index of the items to deselect
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void deselect(int start, int end) {
        checkWidget();
        int count = items.length;
        if (start == 0 && end == count - 1) {
            deselectAll();
        } else {
            start = Math.max(0, start);
            int[] indices = new int[end - start + 1];
            for (int i = start; i <= end; i++) {
                items[i].showSelection(false);
                indices[i - start] = i;
            }
            removeFromSelection(indices);
        }
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
        TableItem[] items = getSelection();
        for (int i = 0; i < items.length; i++) {
            items[i].showSelection(false);
        }
        selection = new TableItem[0];
    }

    void destroyItem(TableColumn column) {
    }

    void destroyItem(TableItem item) {
    }

    void fixCheckboxImageList() {
        if ((style & SWT.CHECK) == 0) return;
    }

    int getTextWidth(String t) {
        int columnWidth = 0;
        if (t == null || t.length() == 0) {
            columnWidth = 0;
        } else {
            columnWidth = OS.getStringPlainWidth(t);
        }
        return columnWidth;
    }

    /**
 * Returns the column at the given, zero-relative index in the
 * receiver. Throws an exception if the index is out of range.
 * If no <code>TableColumn</code>s were created by the programmer,
 * this method will throw <code>ERROR_INVALID_RANGE</code> despite
 * the fact that a single column of data may be visible in the table.
 * This occurs when the programmer uses the table like a list, adding
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
 */
    public TableColumn getColumn(int index) {
        checkWidget();
        return columns[index];
    }

    /**
 * Returns the number of columns contained in the receiver.
 * If no <code>TableColumn</code>s were created by the programmer,
 * this value is zero, despite the fact that visually, one column
 * of items may be visible. This occurs when the programmer uses
 * the table like a list, adding items but never creating a column.
 *
 * @return the number of columns
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getColumnCount() {
        checkWidget();
        if (columns == null) {
            return 0;
        }
        return columns.length;
    }

    /**
 * Returns an array of zero-relative integers that map
 * the creation order of the receiver's items to the
 * order in which they are currently being displayed.
 * <p>
 * Specifically, the indices of the returned array represent
 * the current visual order of the items, and the contents
 * of the array represent the creation order of the items.
 * </p><p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the current visual order of the receiver's items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see Table#setColumnOrder(int[])
 * @see TableColumn#getMoveable()
 * @see TableColumn#setMoveable(boolean)
 * @see SWT#Move
 * 
 * @since 3.1
 */
    public int[] getColumnOrder() {
        checkWidget();
        return new int[0];
    }

    /**
 * Returns an array of <code>TableColumn</code>s which are the
 * columns in the receiver. If no <code>TableColumn</code>s were
 * created by the programmer, the array is empty, despite the fact
 * that visually, one column of items may be visible. This occurs
 * when the programmer uses the table like a list, adding items but
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
 */
    public TableColumn[] getColumns() {
        checkWidget();
        int count = columns.length;
        if (count == 1 && columns[0] == null) count = 0;
        TableColumn[] result = new TableColumn[count];
        System.arraycopy(columns, 0, result, 0, count);
        return result;
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
 * @since 2.0 
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
 */
    public boolean getHeaderVisible() {
        checkWidget();
        return headerVisible;
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
 */
    public TableItem getItem(int index) {
        checkWidget();
        int count = items.length;
        if (!(0 <= index && index < count)) error(SWT.ERROR_INVALID_RANGE);
        return _getItem(index);
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
    public TableItem getItem(Point point) {
        checkWidget();
        if (point == null) error(SWT.ERROR_NULL_ARGUMENT);
        return null;
    }

    /**
 * Returns the number of items contained in the receiver.
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
        if (items == null) {
            return 0;
        }
        return items.length;
    }

    /**
 * Returns the height of the area which would be used to
 * display <em>one</em> of the items in the receiver's.
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
 * Returns a (possibly empty) array of <code>TableItem</code>s which
 * are the items in the receiver. 
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
 */
    public TableItem[] getItems() {
        checkWidget();
        int count = items.length;
        TableItem[] result = new TableItem[count];
        if ((style & SWT.VIRTUAL) != 0) {
            for (int i = 0; i < count; i++) {
                result[i] = _getItem(i);
            }
        } else {
            System.arraycopy(items, 0, result, 0, count);
        }
        return result;
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
 */
    public boolean getLinesVisible() {
        checkWidget();
        return lineVisible;
    }

    /**
 * Returns an array of <code>TableItem</code>s that are currently
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
    public TableItem[] getSelection() {
        checkWidget();
        return selection;
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
        return selection.length;
    }

    /**
 * Returns the zero-relative index of the item which is currently
 * selected in the receiver, or -1 if no item is selected.
 *
 * @return the index of the selected item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getSelectionIndex() {
        checkWidget();
        if (selection.length == 0) {
            return -1;
        }
        return indexOf(selection[0]);
    }

    /**
 * Returns the zero-relative indices of the items which are currently
 * selected in the receiver. The order of the indices is unspecified.
 * The array is empty if no items are selected.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its selection, so modifying the array will
 * not affect the receiver. 
 * </p>
 * @return the array of indices of the selected items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int[] getSelectionIndices() {
        checkWidget();
        int[] result = new int[selection.length];
        for (int i = 0; i < selection.length; i++) {
            result[i] = indexOf(selection[i]);
        }
        return result;
    }

    /**
 * Returns the zero-relative index of the item which is currently
 * at the top of the receiver. This index can change when items are
 * scrolled or new items are added or removed.
 *
 * @return the index of the top item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int getTopIndex() {
        checkWidget();
        return 0;
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
 */
    public int indexOf(TableColumn column) {
        checkWidget();
        if (column == null) error(SWT.ERROR_NULL_ARGUMENT);
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
 *    <li>ERROR_NULL_ARGUMENT - if the string is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int indexOf(TableItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        int count = items.length;
        if (1 <= lastIndexOf && lastIndexOf < count - 1) {
            if (items[lastIndexOf] == item) return lastIndexOf;
            if (items[lastIndexOf + 1] == item) return ++lastIndexOf;
            if (items[lastIndexOf - 1] == item) return --lastIndexOf;
        }
        if (lastIndexOf < count / 2) {
            for (int i = 0; i < count; i++) {
                if (items[i] == item) return lastIndexOf = i;
            }
        } else {
            for (int i = count - 1; i >= 0; --i) {
                if (items[i] == item) return lastIndexOf = i;
            }
        }
        return -1;
    }

    /**
 * Returns <code>true</code> if the item is selected,
 * and <code>false</code> otherwise.  Indices out of
 * range are ignored.
 *
 * @param index the index of the item
 * @return the visibility state of the item at the index
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public boolean isSelected(int index) {
        checkWidget();
        return false;
    }

    void removeItems(int[] indices) {
        if (indices == null && indices.length > items.length) return;
        Element table = handle.childNodes[0];
        Element tbody = null;
        for (int i = 0; i < table.childNodes.length; i++) {
            if ("TBODY".equals(table.childNodes[i].nodeName)) {
                tbody = table.childNodes[i];
                break;
            }
        }
        int count = items.length;
        if (tbody == null) return;
        for (int i = 0; i < indices.length; i++) {
            int index = i;
            if (index < 0 || index >= items.length) return;
            TableItem item = items[index];
            if (item == null) return;
            if (item != null) {
                System.arraycopy(items, index + 1, items, index, --count - index);
                items[count] = null;
            }
            OS.destroyHandle(item.handle);
        }
    }

    protected void releaseHandle() {
        if (hTableKeyDown != null) {
            Clazz.removeEvent(handle, "keydown", hTableKeyDown);
            hTableKeyDown = null;
        }
        if (tbodyTRTemplate != null) {
            OS.deepClearChildren(tbodyTRTemplate);
            OS.destroyHandle(tbodyTRTemplate);
            tbodyTRTemplate = null;
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
        currentItem = null;
        lastSelection = null;
        selection = null;
        super.releaseHandle();
    }

    protected void releaseWidget() {
        int columnCount = columns.length;
        if (columnCount == 1 && columns[0] == null) columnCount = 0;
        int itemCount = items.length;
        for (int i = 0; i < itemCount; i++) {
            TableItem item = items[i];
            if (item != null && !item.isDisposed()) item.releaseResources();
        }
        customDraw = false;
        currentItem = null;
        items = null;
        for (int i = 0; i < columnCount; i++) {
            TableColumn column = columns[i];
            if (!column.isDisposed()) column.releaseResources();
        }
        columns = null;
        if (imageList != null) {
            display.releaseImageList(imageList);
        }
        imageList = null;
        super.releaseWidget();
    }

    /**
 * Removes the items from the receiver's list at the given
 * zero-relative indices.
 *
 * @param indices the array of indices of the items
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 *    <li>ERROR_NULL_ARGUMENT - if the indices array is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void remove(int[] indices) {
        checkWidget();
        if (indices == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (indices.length == 0) return;
        int[] newIndices = new int[indices.length];
        System.arraycopy(indices, 0, newIndices, 0, indices.length);
        Element table = handle.childNodes[0];
        Element tbody = null;
        for (int i = 0; i < table.childNodes.length; i++) {
            if ("TBODY".equals(table.childNodes[i].nodeName)) {
                tbody = table.childNodes[i];
                break;
            }
        }
        if (tbody == null) return;
        int start = newIndices[newIndices.length - 1], end = newIndices[0];
        int count = items.length;
        if (!(0 <= start && start <= end && end < count)) {
            return;
        }
        deselect(indices);
        TableItem[] newItems = new TableItem[count - 1];
        int last = -1;
        for (int i = 0; i < newIndices.length; i++) {
            int index = newIndices[i];
            if (index != last) {
                TableItem item = items[index];
                if (item != null) {
                    item.releaseHandle();
                    System.arraycopy(items, 0, newItems, 0, index);
                    System.arraycopy(items, index + 1, newItems, index, --count - index);
                    items = newItems;
                    last = index;
                }
            }
        }
    }

    /**
 * Removes the item from the receiver at the given
 * zero-relative index.
 *
 * @param index the index for the item
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void remove(int index) {
        checkWidget();
        remove(new int[] { index });
    }

    /**
 * Removes the items from the receiver which are
 * between the given zero-relative start and end 
 * indices (inclusive).
 *
 * @param start the start of the range
 * @param end the end of the range
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void remove(int start, int end) {
        checkWidget();
        if (start > end) return;
        int count = items.length;
        if (!(0 <= start && start <= end && end < count)) {
            return;
        }
        Element table = handle.childNodes[0];
        Element tbody = null;
        for (int i = 0; i < table.childNodes.length; i++) {
            if ("TBODY".equals(table.childNodes[i].nodeName)) {
                tbody = table.childNodes[i];
                break;
            }
        }
        if (tbody == null) return;
        deselect(start, end);
        int index = start;
        while (index <= end) {
            TableItem item = items[index];
            if (item != null && !item.isDisposed()) {
                item.releaseHandle();
            }
            index++;
        }
        TableItem[] newItems = new TableItem[count - (index - start)];
        System.arraycopy(items, 0, newItems, 0, start);
        System.arraycopy(items, index, newItems, start, count - index);
        items = newItems;
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
        remove(0, items.length - 1);
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
 * @see #addSelectionListener(SelectionListener)
 */
    public void removeSelectionListener(SelectionListener listener) {
        checkWidget();
        if (listener == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (eventTable == null) return;
        eventTable.unhook(SWT.Selection, listener);
        eventTable.unhook(SWT.DefaultSelection, listener);
    }

    /**
 * Selects the items at the given zero-relative indices in the receiver.
 * The current selection is not cleared before the new items are selected.
 * <p>
 * If the item at a given index is not selected, it is selected.
 * If the item at a given index was already selected, it remains selected.
 * Indices that are out of range and duplicate indices are ignored.
 * If the receiver is single-select and multiple indices are specified,
 * then all indices are ignored.
 *
 * @param indices the array of indices for the items to select
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see Table#setSelection(int[])
 */
    public void select(int[] indices) {
        checkWidget();
        if (indices == null) error(SWT.ERROR_NULL_ARGUMENT);
        int length = indices.length;
        if (length == 0 || ((style & SWT.SINGLE) != 0 && length > 1)) return;
        deselectAll();
        selection = new TableItem[length];
        for (int i = 0; i < length; i++) {
            int index = indices[i];
            items[index].showSelection(true);
            selection[i] = this.items[index];
        }
        int focusIndex = indices[0];
        if (focusIndex != -1) setFocusIndex(focusIndex);
    }

    /**
 * Selects the item at the given zero-relative index in the receiver. 
 * If the item at the index was already selected, it remains
 * selected. Indices that are out of range are ignored.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void select(int index) {
        checkWidget();
        if (index < 0) return;
        deselectAll();
        items[index].showSelection(true);
        selection = new TableItem[1];
        selection[0] = this.items[index];
        setFocusIndex(index);
    }

    /**
 * Selects the items in the range specified by the given zero-relative
 * indices in the receiver. The range of indices is inclusive.
 * The current selection is not cleared before the new items are selected.
 * <p>
 * If an item in the given range is not selected, it is selected.
 * If an item in the given range was already selected, it remains selected.
 * Indices that are out of range are ignored and no items will be selected
 * if start is greater than end.
 * If the receiver is single-select and there is more than one item in the
 * given range, then all indices are ignored.
 *
 * @param start the start of the range
 * @param end the end of the range
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see Table#setSelection(int,int)
 */
    public void select(int start, int end) {
        checkWidget();
        if (end < 0 || start > end || ((style & SWT.SINGLE) != 0 && start != end)) return;
        int count = items.length;
        if (count == 0 || start >= count) return;
        deselectAll();
        start = Math.max(0, start);
        end = Math.min(end, count - 1);
        if (start == 0 && end == count - 1) {
            selectAll();
        } else {
            selection = new TableItem[end - start + 1];
            for (int i = start; i <= end; i++) {
                items[i].showSelection(true);
                selection[i - start] = items[i];
            }
        }
        setFocusIndex(start);
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
        selection = new TableItem[items.length];
        for (int i = 0; i < items.length; i++) {
            items[i].showSelection(true);
            selection[i] = items[i];
        }
    }

    void setBounds(int x, int y, int width, int height, int flags) {
        boolean fixResize = false;
        if (fixResize) setRedraw(false);
        super.setBounds(x, y, width, height, flags);
        if (fixResize) setRedraw(true);
    }

    /**
 * Sets the order that the items in the receiver should 
 * be displayed in to the given argument which is described
 * in terms of the zero-relative ordering of when the items
 * were added.
 *
 * @param order the new order to display the items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item order is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item order is not the same length as the number of items</li>
 * </ul>
 * 
 * @see Table#getColumnOrder()
 * @see TableColumn#getMoveable()
 * @see TableColumn#setMoveable(boolean)
 * @see SWT#Move
 * 
 * @since 3.1
 */
    public void setColumnOrder(int[] order) {
        checkWidget();
        if (order == null) error(SWT.ERROR_NULL_ARGUMENT);
    }

    void setCheckboxImageListColor() {
        if ((style & SWT.CHECK) == 0) return;
    }

    void setCheckboxImageList(int width, int height) {
        if ((style & SWT.CHECK) == 0) return;
    }

    void setFocusIndex(int index) {
        checkWidget();
        if (index < 0) return;
        if (index == focusIndex) {
            return;
        }
        TableItem item = getItem(index);
        if (item == null) {
            return;
        }
        if (this.focusItem != null) {
            OS.removeCSSClass(focusItem.handle, "table-item-focus");
        }
        this.focusItem = item;
        this.focusIndex = index;
        OS.addCSSClass(item.handle, "table-item-focus");
    }

    public void setFont(Font font) {
        checkWidget();
        int topIndex = getTopIndex();
        setRedraw(false);
        setTopIndex(0);
        super.setFont(font);
        setTopIndex(topIndex);
        setScrollWidth(null, true);
        setRedraw(true);
        setItemHeight();
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
 */
    public void setHeaderVisible(boolean show) {
        checkWidget();
        headerVisible = show;
        if (theadHandle != null) {
            theadHandle.style.display = (show ? "" : "none");
        }
    }

    /**
 * Sets the number of items contained in the receiver.
 *
 * @param count the number of items
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.0
 */
    public void setItemCount(int count) {
        checkWidget();
        count = Math.max(0, count);
    }

    void setItemHeight() {
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
 */
    public void setLinesVisible(boolean show) {
        checkWidget();
        lineVisible = show;
        OS.updateCSSClass(tableHandle, "table-grid-line", show);
    }

    public void setRedraw(boolean redraw) {
        checkWidget();
    }

    boolean setScrollWidth(TableItem item, boolean force) {
        if (currentItem != null) {
            if (currentItem != item) fixScrollWidth = true;
            return false;
        }
        return false;
    }

    /**
 * Selects the items at the given zero-relative indices in the receiver.
 * The current selection is cleared before the new items are selected.
 * <p>
 * Indices that are out of range and duplicate indices are ignored.
 * If the receiver is single-select and multiple indices are specified,
 * then all indices are ignored.
 *
 * @param indices the indices of the items to select
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int[])
 */
    public void setSelection(int[] indices) {
        checkWidget();
        if (indices == null) error(SWT.ERROR_NULL_ARGUMENT);
        deselectAll();
        int length = indices.length;
        if (length == 0 || ((style & SWT.SINGLE) != 0 && length > 1)) return;
        select(indices);
        showSelection();
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
 * @see Table#deselectAll()
 * @see Table#select(int[])
 * @see Table#setSelection(int[])
 */
    public void setSelection(TableItem[] items) {
        checkWidget();
        if (items == null) error(SWT.ERROR_NULL_ARGUMENT);
        deselectAll();
        int length = items.length;
        if (length == 0 || ((style & SWT.SINGLE) != 0 && length > 1)) return;
        int focusIndex = -1;
        selection = items;
        for (int i = length - 1; i >= 0; --i) {
            int index = indexOf(items[i]);
            items[i].showSelection(true);
            if (index != -1) {
                focusIndex = index;
            }
        }
        if (focusIndex != -1) setFocusIndex(focusIndex);
        showSelection();
    }

    /**
 * Selects the item at the given zero-relative index in the receiver. 
 * The current selection is first cleared, then the new item is selected.
 *
 * @param index the index of the item to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int)
 */
    public void setSelection(int index) {
        checkWidget();
        deselectAll();
        select(index);
    }

    /**
 * Selects the items in the range specified by the given zero-relative
 * indices in the receiver. The range of indices is inclusive.
 * The current selection is cleared before the new items are selected.
 * <p>
 * Indices that are out of range are ignored and no items will be selected
 * if start is greater than end.
 * If the receiver is single-select and there is more than one item in the
 * given range, then all indices are ignored.
 * 
 * @param start the start index of the items to select
 * @param end the end index of the items to select
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Table#deselectAll()
 * @see Table#select(int,int)
 */
    public void setSelection(int start, int end) {
        deselectAll();
        if (end < 0 || start > end || ((style & SWT.SINGLE) != 0 && start != end)) return;
        int count = items.length;
        if (count == 0 || start >= count) return;
        start = Math.max(0, start);
        end = Math.min(end, count - 1);
        select(start, end);
        selection = new TableItem[end - start + 1];
        for (int i = start; i <= end; i++) {
            selection[i - start] = items[i];
        }
        showSelection();
    }

    void setTableEmpty() {
    }

    private void removeFromSelection(int[] indices) {
        if (selection.length < indices.length) {
            return;
        }
        TableItem[] newSelection = new TableItem[selection.length - indices.length];
        int j = 0;
        for (int i = 0; i < indices.length; i++) {
            if (selection[i].isSelected()) {
                newSelection[j++] = selection[i];
            }
        }
        selection = newSelection;
    }

    boolean toggleSelection(TableItem item, boolean isCtrlKeyHold, boolean isShiftKeyHold) {
        if (item == null) {
            return false;
        }
        if ((style & SWT.MULTI) != 0 && (isCtrlKeyHold || isShiftKeyHold)) {
            if (isCtrlKeyHold) {
                for (int i = 0; i < selection.length; i++) {
                    if (item == selection[i]) {
                        TableItem[] newSelections = new TableItem[selection.length];
                        for (int j = 0; j < i; j++) {
                            newSelections[j] = selection[j];
                        }
                        for (int j = i; j < selection.length - 1; j++) {
                            newSelections[j] = selection[j + 1];
                        }
                        selection = newSelections;
                        item.showSelection(false);
                        lastSelection = item;
                        return false;
                    }
                }
                selection[selection.length] = item;
                lastSelection = item;
                item.showSelection(true);
            } else {
                for (int i = 0; i < selection.length; i++) {
                    if (selection[i] != null) {
                        selection[i].showSelection(false);
                    }
                }
                if (lastSelection != null) {
                    int idx1 = Math.min(indexOf(lastSelection), indexOf(item));
                    int idx2 = Math.max(indexOf(lastSelection), indexOf(item));
                    selection = new TableItem[0];
                    for (int i = idx1; i <= idx2; i++) {
                        TableItem ti = items[i];
                        selection[selection.length] = ti;
                        ti.showSelection(true);
                    }
                    return true;
                } else {
                    if (selection.length != 1) {
                        selection = new TableItem[1];
                    }
                    selection[0] = item;
                }
            }
        } else {
            item.showSelection(true);
            for (int i = 0; i < selection.length; i++) {
                if (selection[i] != null && selection[i] != item) {
                    selection[i].showSelection(false);
                }
            }
            if (selection.length != 1) {
                selection = new TableItem[1];
            }
            selection[0] = item;
        }
        lastSelection = item;
        return true;
    }

    /**
 * Sets the zero-relative index of the item which is currently
 * at the top of the receiver. This index can change when items
 * are scrolled or new items are added and removed.
 *
 * @param index the index of the top item
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void setTopIndex(int index) {
        checkWidget();
    }

    /**
 * Shows the column.  If the column is already showing in the receiver,
 * this method simply returns.  Otherwise, the columns are scrolled until
 * the column is visible.
 *
 * @param column the column to be shown
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the column is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the column has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.0
 */
    public void showColumn(TableColumn column) {
        checkWidget();
        if (column == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (column.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        if (column.parent != this) return;
        int index = indexOf(column);
        if (index == -1) return;
    }

    void showItem(int index) {
    }

    /**
 * Shows the item.  If the item is already showing in the receiver,
 * this method simply returns.  Otherwise, the items are scrolled until
 * the item is visible.
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
 * @see Table#showSelection()
 */
    public void showItem(TableItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (item.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        int index = indexOf(item);
        if (index != -1) showItem(index);
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
 * @see Table#showItem(TableItem)
 */
    public void showSelection() {
        checkWidget();
    }

    void updateMoveable() {
    }

    protected Control[] _getChildren() {
        return new Control[0];
    }

    protected boolean useNativeScrollBar() {
        return true;
    }
}
