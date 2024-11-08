package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.browser.OS;
import org.eclipse.swt.internal.xhtml.CSSStyle;
import org.eclipse.swt.internal.xhtml.Element;
import org.eclipse.swt.internal.xhtml.document;

/**
 * Instances of this class provide an area for dynamically
 * positioning the items they contain.
 * <p>
 * The item children that may be added to instances of this class
 * must be of type <code>CoolItem</code>.
 * </p><p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add <code>Control</code> children to it,
 * or set a layout on it.
 * </p><p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>FLAT</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * 
 * @j2sPrefix
 * $WTC$$.registerCSS ("$wt.widgets.CoolBar");
 */
public class CoolBar extends Composite {

    CoolItem[] items;

    CoolItem[] originalItems;

    boolean locked;

    boolean ignoreResize;

    static final int SEPARATOR_WIDTH = 2;

    static final int MAX_WIDTH = 0x7FFF;

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
 * @see SWT
 * @see Widget#checkSubclass
 * @see Widget#getStyle
 */
    public CoolBar(Composite parent, int style) {
        super(parent, checkStyle(style));
    }

    static int checkStyle(int style) {
        style |= SWT.NO_FOCUS;
        return style & ~(SWT.H_SCROLL | SWT.V_SCROLL);
    }

    protected void checkSubclass() {
        if (!isValidSubclass()) error(SWT.ERROR_INVALID_SUBCLASS);
    }

    public Point computeSize(int wHint, int hHint, boolean changed) {
        checkWidget();
        int width = 0, height = 0;
        int border = getBorderWidth();
        int count = items.length;
        if (count != 0) {
            ignoreResize = true;
            boolean redraw = false;
            int rowHeight = 0;
            int rowWidth = 0;
            int separator = (style & SWT.FLAT) == 0 ? SEPARATOR_WIDTH : 0;
            for (int i = 0; i < count; i++) {
                if (items[i].wrap) {
                    width = Math.max(width, rowWidth - separator);
                    rowWidth = 0;
                    height += rowHeight;
                    height += 2;
                    rowHeight = 0;
                }
                if (items[i].ideal) {
                    rowWidth += 7 + 2 + Math.max(items[i].idealWidth, items[i].minimumWidth + 2) + separator;
                    if (items[i].control == null) {
                        rowHeight = Math.max(rowHeight, 4);
                    } else {
                        rowHeight = Math.max(rowHeight, items[i].idealHeight);
                    }
                } else {
                    if (items[i].control != null) {
                        rowWidth += items[i].control.getSize().x + 9 + 2 + 2 + separator;
                    } else {
                        rowWidth += 9 + 2 + 2 + separator;
                        rowHeight = Math.max(rowHeight, 4);
                    }
                }
            }
            width = Math.max(width, rowWidth - separator);
            height += rowHeight;
            if (redraw) {
            }
            ignoreResize = false;
        }
        if (width == 0) width = DEFAULT_WIDTH;
        if (height == 0) height = DEFAULT_HEIGHT;
        if (wHint != SWT.DEFAULT) width = wHint;
        if (hHint != SWT.DEFAULT) height = hHint;
        height += border * 2;
        width += border * 2;
        return new Point(width, height);
    }

    protected void createHandle() {
        super.createHandle();
        state &= ~CANVAS;
        String cssName = " cool-bar-default";
        if ((style & SWT.FLAT) != 0) {
            cssName += " cool-bar-flat";
        }
        handle.className += cssName;
    }

    void createItem(final CoolItem item, int index) {
        int count = items.length;
        if (!(0 <= index && index <= count)) error(SWT.ERROR_INVALID_RANGE);
        int id = 0;
        id = items.length;
        if ((item.style & SWT.DROP_DOWN) != 0) {
        }
        int lastIndex = getLastIndexOfRow(index - 1);
        boolean fixLast = index == lastIndex + 1;
        if (fixLast) {
        }
        if (index == 0 && count > 0) {
            getItem(0).setWrap(false);
        }
        Element el = document.createElement("DIV");
        el.className = "cool-item-default";
        if (index == count) {
            handle.appendChild(el);
        } else {
            handle.insertBefore(el, items[index].handle);
        }
        item.handle = el;
        el = document.createElement("DIV");
        el.className = "cool-item-handler";
        item.handle.appendChild(el);
        if ((item.style & SWT.DROP_DOWN) != 0) {
            el = document.createElement("DIV");
            el.className = "cool-item-more";
            item.handle.appendChild(el);
            item.moreHandle = el;
            el = document.createElement("SPAN");
            el.appendChild(document.createTextNode(">"));
            item.moreHandle.appendChild(el);
            el = document.createElement("SPAN");
            el.appendChild(document.createTextNode(">"));
            el.className = "cool-item-more-arrow";
            item.moreHandle.appendChild(el);
            item.configure();
        }
        item.configureDND(el);
        el = document.createElement("DIV");
        el.className = "cool-item-content";
        item.handle.appendChild(el);
        item.contentHandle = el;
        if (fixLast) {
            resizeToPreferredWidth(lastIndex);
        }
        item.wrap = false;
        items[item.id = id] = item;
        int length = originalItems.length;
        CoolItem[] newOriginals = new CoolItem[length + 1];
        System.arraycopy(originalItems, 0, newOriginals, 0, index);
        System.arraycopy(originalItems, index, newOriginals, index + 1, length - index);
        newOriginals[index] = item;
        originalItems = newOriginals;
    }

    boolean moveDelta(int index, int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        boolean needResize = false;
        boolean needLayout = false;
        if (dy == 0) {
            int[] ws = new int[items.length];
            for (int i = 0; i < ws.length; i++) {
                ws[i] = items[i].idealWidth;
            }
            boolean needCalculate = false;
            CoolItem item = items[index];
            if (item.wrap && (dx < 0 || isLastItemOfRow(index))) {
                return false;
            }
            if ((index == 0 && items.length > 1) || (item.wrap && !isLastItemOfRow(index))) {
                if (dx >= item.lastCachedWidth) {
                    CoolItem next = items[index + 1];
                    items[index] = next;
                    items[index + 1] = item;
                    if (item.wrap) {
                        next.wrap = true;
                        item.wrap = false;
                    }
                    int width = next.idealWidth;
                    next.idealWidth = item.idealWidth;
                    item.idealWidth = width;
                    dx = dx - item.lastCachedWidth;
                    index++;
                    needLayout = true;
                }
            }
            if (dx != 0 && index > 0 && !(item.wrap && !isLastItemOfRow(index))) {
                CoolItem cur = item;
                CoolItem prev = items[index - 1];
                int idx = index - 1;
                while (dx < 0) {
                    if (prev.lastCachedWidth + dx < minWidth(prev)) {
                        int ddx = prev.lastCachedWidth - minWidth(prev);
                        prev.idealWidth -= ddx;
                        item.idealWidth += ddx;
                        needCalculate = true;
                        dx += ddx;
                        if (dx < 0) {
                            if (idx - 1 >= 0 && !items[idx].wrap) {
                                idx--;
                                prev = items[idx];
                            } else {
                                if (dx + 11 <= 0) {
                                    CoolItem swpItem = prev;
                                    int swpIndex = index;
                                    while (dx + minWidth(swpItem) <= 0) {
                                        dx += minWidth(swpItem);
                                        swpItem = items[swpIndex - 1];
                                        items[swpIndex - 1] = items[swpIndex];
                                        items[swpIndex] = swpItem;
                                        if (swpItem.wrap) {
                                            items[swpIndex - 1].wrap = true;
                                            swpItem.wrap = false;
                                        }
                                        needLayout = true;
                                        swpIndex--;
                                        if (swpIndex == 0 || swpItem.wrap) {
                                            break;
                                        }
                                    }
                                }
                                dx = 0;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                CoolItem next = null;
                idx = index;
                while (dx > 0 && cur.lastCachedWidth - dx < minWidth(cur)) {
                    int dxx = cur.lastCachedWidth - minWidth(cur);
                    prev.idealWidth += dxx;
                    cur.idealWidth -= dxx;
                    needCalculate = true;
                    dx -= dxx;
                    if (dx > 0) {
                        if (idx + 1 < items.length && !isLastItemOfRow(idx)) {
                            idx++;
                            cur = items[idx];
                            if (next == null) {
                                next = cur;
                            }
                        } else {
                            if (dx >= 11 && next != null) {
                                CoolItem swpItem = next;
                                int swpIndex = index;
                                while (dx >= minWidth(swpItem)) {
                                    items[swpIndex + 1] = items[swpIndex];
                                    items[swpIndex] = swpItem;
                                    if (swpItem.wrap) {
                                        items[swpIndex].wrap = true;
                                        swpItem.wrap = false;
                                    }
                                    swpItem = items[swpIndex + 1];
                                    needLayout = true;
                                    dx -= minWidth(swpItem);
                                    swpIndex++;
                                    if (swpIndex >= items.length || isLastItemOfRow(swpIndex)) {
                                        break;
                                    }
                                }
                            }
                            dx = 0;
                            break;
                        }
                    }
                }
                prev.idealWidth += dx;
                if (dx != 0) {
                    needCalculate = true;
                }
                if (item != cur) {
                    if (cur.idealWidth - dx < 0) {
                        if (cur.idealWidth != 0) {
                            needCalculate = true;
                        }
                        cur.idealWidth = 0;
                    } else {
                        cur.idealWidth -= dx;
                    }
                } else {
                    item.idealWidth -= dx;
                }
            }
            if (needCalculate && !needLayout) {
                for (int i = 0; i < ws.length; i++) {
                    if (ws[i] != items[i].idealWidth) {
                        needLayout = true;
                        break;
                    }
                }
            }
        } else {
            int line = verticalLine(index);
            if (line + dy < 0) {
                if (index == 0 && isLastItemOfRow(index)) {
                } else {
                    CoolItem ci = items[index];
                    if ((index == 0 && items.length > 1) || (ci.wrap && index < items.length - 1)) {
                        items[index + 1].wrap = true;
                    }
                    for (int i = index; i > 0; i--) {
                        items[i] = items[i - 1];
                    }
                    items[0] = ci;
                    items[1].wrap = true;
                    ci.wrap = false;
                    needLayout = true;
                    needResize = true;
                }
            } else if (line + dy < getVerticalLines()) {
                int lineNumber = line + dy;
                int i = 0;
                for (i = 0; i < items.length; i++) {
                    if (lineNumber == 0) {
                        break;
                    }
                    if (items[i].wrap) {
                        lineNumber--;
                    }
                }
                if (i > 0) i--;
                CoolItem ci = items[index];
                if (index == 0 && isLastItemOfRow(index)) {
                    needResize = true;
                }
                if (ci.wrap) {
                    if (isLastItemOfRow(index)) {
                        needResize = true;
                    }
                    if (index < items.length - 1) {
                        items[index + 1].wrap = true;
                    }
                }
                int x = ci.getPosition().x + dx;
                if (x <= 0) {
                    if (i == 0) {
                        ci.wrap = false;
                    } else {
                        if (index == 0 && i == 1) {
                        } else {
                            ci.wrap = true;
                        }
                        if (i < items.length - 1) {
                            items[i + 1].wrap = false;
                        }
                    }
                } else {
                    int rowWidth = 0;
                    int separator = 2;
                    for (; i < items.length; i++) {
                        CoolItem item = items[i];
                        int minimum = item.minimumWidth + (item.minimumWidth != 0 ? 2 : 0);
                        rowWidth += 7 + 2 + Math.max(item.idealWidth, minimum) + separator;
                        int xx = item.getPosition().x;
                        if (xx < x && (x <= rowWidth || isLastItemOfRow(i))) {
                            item.idealWidth = Math.max(0, x - xx - (7 + 2 + minimum + separator));
                            minimum = ci.minimumWidth + (ci.minimumWidth != 0 ? 2 : 0);
                            int mw = 7 + 2 + minimum + separator;
                            ci.idealWidth = Math.max(item.minimumWidth, Math.max(ci.idealWidth, rowWidth - x - mw));
                            if (rowWidth - x - mw < ci.idealWidth) {
                                needResize = true;
                            }
                            break;
                        }
                    }
                    ci.wrap = false;
                }
                if (dy < 0 && x > 0 && i < items.length - 1) {
                    i++;
                }
                if (dy > 0) {
                    for (int j = index; j < i; j++) {
                        items[j] = items[j + 1];
                    }
                } else {
                    for (int j = index; j > i; j--) {
                        items[j] = items[j - 1];
                    }
                }
                items[i] = ci;
                items[0].wrap = false;
                needLayout = true;
            } else {
                if ((items[index].wrap || index == 0) && isLastItemOfRow(index)) {
                } else {
                    CoolItem ci = items[index];
                    if (index > 0 && ci.wrap) {
                        items[index + 1].wrap = true;
                    }
                    for (int i = index; i < items.length - 1; i++) {
                        items[i] = items[i + 1];
                    }
                    items[items.length - 1] = ci;
                    ci.wrap = true;
                    needLayout = true;
                    needResize = true;
                }
            }
        }
        int w = width;
        int h = height;
        if (needResize) {
            Point computeSize = computeSize(-1, -1, false);
            w = computeSize.x;
            h = computeSize.y;
        }
        if (needLayout) {
            SetWindowPos(handle, null, left, top, width, h, -1);
        }
        if (w > width) {
            for (int i = index; i < items.length; i++) {
                if (isLastItemOfRow(i)) {
                    moveDelta(i, width - height, 0);
                    break;
                }
            }
        }
        if (h != height && !ignoreResize) {
            setBounds(left, top, Math.max(0, width), Math.max(0, h), SWT.NONE);
            sendEvent(SWT.Resize);
        }
        return needLayout;
    }

    int getVerticalLines() {
        int lines = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].wrap) {
                lines++;
            }
        }
        return lines + 1;
    }

    int verticalLine(int index) {
        int lines = 0;
        for (int i = 0; i <= index; i++) {
            if (items[i].wrap) {
                lines++;
            }
        }
        return lines;
    }

    int verticalLineByPixel(int px) {
        if (px < 0) {
            return -1;
        }
        int lines = 0;
        int rowHeight = 0;
        int height = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].wrap) {
                height += rowHeight + 2;
                rowHeight = 0;
                if (px < height) {
                    return lines;
                }
                lines++;
            }
            if (items[i].control == null) {
                rowHeight = Math.max(rowHeight, 4);
            } else if (items[i].ideal) {
                rowHeight = Math.max(rowHeight, items[i].idealHeight);
            }
        }
        height += rowHeight;
        if (px < height) {
            return lines;
        }
        return lines + 1;
    }

    protected void createWidget() {
        super.createWidget();
        items = new CoolItem[0];
        originalItems = new CoolItem[0];
    }

    void destroyItem(CoolItem item) {
        int index = indexOf(item);
        int count = items.length;
        if (count != 0) {
            int lastIndex = getLastIndexOfRow(index);
            if (index == lastIndex) {
                resizeToMaximumWidth(lastIndex - 1);
            }
        }
        Control control = item.control;
        boolean wasVisible = control != null && !control.isDisposed() && control.getVisible();
        CoolItem nextItem = null;
        if (item.getWrap()) {
            if (index + 1 < count) {
                nextItem = getItem(index + 1);
                ignoreResize = !nextItem.getWrap();
            }
        }
        OS.destroyHandle(items[index].handle);
        items[item.id] = null;
        item.id = -1;
        if (ignoreResize) {
            nextItem.setWrap(true);
            ignoreResize = false;
        }
        if (wasVisible) control.setVisible(true);
        index = 0;
        while (index < originalItems.length) {
            if (originalItems[index] == item) break;
            index++;
        }
        int length = originalItems.length - 1;
        CoolItem[] newOriginals = new CoolItem[length];
        System.arraycopy(originalItems, 0, newOriginals, 0, index);
        System.arraycopy(originalItems, index + 1, newOriginals, index, length - index);
        originalItems = newOriginals;
    }

    int getMargin(int index) {
        int margin = 0;
        margin = 7 + 2;
        if (!isLastItemOfRow(index)) {
            margin += 2;
        }
        return margin;
    }

    int minWidth(CoolItem item) {
        return 7 + 2 + item.minimumWidth + (item.minimumWidth != 0 ? 4 : 0) + (!isLastItemOfRow(indexOf(item)) ? 2 : 0);
    }

    void enableWidget(boolean enabled) {
        OS.updateCSSClass(handle, "cool-bar-disabled", !enabled);
    }

    Control findThemeControl() {
        return null;
    }

    /**
 * Returns the item that is currently displayed at the given,
 * zero-relative index. Throws an exception if the index is
 * out of range.
 *
 * @param index the visual index of the item to return
 * @return the item at the given visual index
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public CoolItem getItem(int index) {
        checkWidget();
        int count = items.length;
        if (!(0 <= index && index < count)) error(SWT.ERROR_INVALID_RANGE);
        return items[index];
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
        return items.length;
    }

    /**
 * Returns an array of zero-relative ints that map
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
 */
    public int[] getItemOrder() {
        checkWidget();
        int count = items.length;
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            CoolItem item = items[i];
            int index = 0;
            while (index < originalItems.length) {
                if (originalItems[index] == item) break;
                index++;
            }
            if (index == originalItems.length) error(SWT.ERROR_CANNOT_GET_ITEM);
            indices[i] = index;
        }
        return indices;
    }

    /**
 * Returns an array of <code>CoolItem</code>s in the order
 * in which they are currently being displayed.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of items, so modifying the array will
 * not affect the receiver. 
 * </p>
 *
 * @return the receiver's items in their current visual order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public CoolItem[] getItems() {
        checkWidget();
        int count = items.length;
        CoolItem[] result = new CoolItem[count];
        for (int i = 0; i < count; i++) {
            result[i] = items[i];
        }
        return result;
    }

    /**
 * Returns an array of points whose x and y coordinates describe
 * the widths and heights (respectively) of the items in the receiver
 * in the order in which they are currently being displayed.
 *
 * @return the receiver's item sizes in their current visual order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public Point[] getItemSizes() {
        checkWidget();
        int count = items.length;
        Point[] sizes = new Point[count];
        int separator = (style & SWT.FLAT) == 0 ? SEPARATOR_WIDTH : 0;
        for (int i = 0; i < count; i++) {
            Point size = items[i].getSize();
            if (!isLastItemOfRow(i)) size.x += separator;
            sizes[i] = size;
        }
        return sizes;
    }

    int getLastIndexOfRow(int index) {
        int count = items.length;
        if (count == 0) return -1;
        for (int i = index + 1; i < count; i++) {
            if (items[i].wrap) {
                return i - 1;
            }
        }
        return count - 1;
    }

    boolean isLastItemOfRow(int index) {
        int count = items.length;
        if (index + 1 == count) return true;
        return (items[index + 1].wrap);
    }

    /**
 * Returns whether or not the receiver is 'locked'. When a coolbar
 * is locked, its items cannot be repositioned.
 *
 * @return true if the coolbar is locked, false otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 2.0
 */
    public boolean getLocked() {
        checkWidget();
        return locked;
    }

    /**
 * Returns an array of ints that describe the zero-relative
 * indices of any item(s) in the receiver that will begin on
 * a new row. The 0th visible item always begins the first row,
 * therefore it does not count as a wrap index.
 *
 * @return an array containing the receiver's wrap indices, or an empty array if all items are in one row
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int[] getWrapIndices() {
        checkWidget();
        CoolItem[] items = getItems();
        int[] indices = new int[items.length];
        int count = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i].getWrap()) indices[count++] = i;
        }
        int[] result = new int[count];
        System.arraycopy(indices, 0, result, 0, count);
        return result;
    }

    /**
 * Searches the receiver's items in the order they are currently
 * being displayed, starting at the first item (index 0), until
 * an item is found that is equal to the argument, and returns
 * the index of that item. If no item is found, returns -1.
 *
 * @param item the search item
 * @return the visual order index of the search item, or -1 if the item is not found
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the item is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the item is disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public int indexOf(CoolItem item) {
        checkWidget();
        if (item == null) error(SWT.ERROR_NULL_ARGUMENT);
        if (item.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
        for (int i = 0; i < items.length; i++) {
            if (item == items[i]) {
                return i;
            }
        }
        return -1;
    }

    void resizeToPreferredWidth(int index) {
        int count = items.length;
        if (0 <= index && index < count) {
        }
    }

    void resizeToMaximumWidth(int index) {
        int count = items.length;
        if (0 <= index && index < count) {
        }
    }

    protected void releaseWidget() {
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            if (item != null && !item.isDisposed()) {
                item.releaseResources();
            }
        }
        items = null;
        super.releaseWidget();
    }

    protected void removeControl(Control control) {
        super.removeControl(control);
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            if (item != null && item.control == control) {
                item.setControl(null);
            }
        }
    }

    void setBackgroundPixel(int pixel) {
        if (background == pixel) return;
        background = pixel;
    }

    void setForegroundPixel(int pixel) {
        if (foreground == pixel) return;
        foreground = pixel;
    }

    void setItemColors(int foreColor, int backColor) {
    }

    /**
 * Sets the receiver's item order, wrap indices, and item sizes
 * all at once. This method is typically used to restore the
 * displayed state of the receiver to a previously stored state.
 * <p>
 * The item order is the order in which the items in the receiver
 * should be displayed, given in terms of the zero-relative ordering
 * of when the items were added.
 * </p><p>
 * The wrap indices are the indices of all item(s) in the receiver
 * that will begin on a new row. The indices are given in the order
 * specified by the item order. The 0th item always begins the first
 * row, therefore it does not count as a wrap index. If wrap indices
 * is null or empty, the items will be placed on one line.
 * </p><p>
 * The sizes are specified in an array of points whose x and y
 * coordinates describe the new widths and heights (respectively)
 * of the receiver's items in the order specified by the item order.
 * </p>
 *
 * @param itemOrder an array of indices that describe the new order to display the items in
 * @param wrapIndices an array of wrap indices, or null
 * @param sizes an array containing the new sizes for each of the receiver's items in visual order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if item order or sizes is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if item order or sizes is not the same length as the number of items</li>
 * </ul>
 */
    public void setItemLayout(int[] itemOrder, int[] wrapIndices, Point[] sizes) {
        checkWidget();
        setRedraw(false);
        setItemOrder(itemOrder);
        setWrapIndices(wrapIndices);
        setItemSizes(sizes);
        setRedraw(true);
    }

    void setItemOrder(int[] itemOrder) {
        if (itemOrder == null) error(SWT.ERROR_NULL_ARGUMENT);
        int itemCount = items.length;
        if (itemOrder.length != itemCount) error(SWT.ERROR_INVALID_ARGUMENT);
        boolean[] set = new boolean[itemCount];
        for (int i = 0; i < itemOrder.length; i++) {
            int index = itemOrder[i];
            if (index < 0 || index >= itemCount) error(SWT.ERROR_INVALID_RANGE);
            if (set[index]) error(SWT.ERROR_INVALID_ARGUMENT);
            set[index] = true;
        }
        for (int i = 0; i < itemOrder.length; i++) {
            int id = originalItems[itemOrder[i]].id;
            int index = id;
            if (index != i) {
                int lastItemSrcRow = getLastIndexOfRow(index);
                int lastItemDstRow = getLastIndexOfRow(i);
                if (index == lastItemSrcRow) {
                    resizeToPreferredWidth(index);
                }
                if (i == lastItemDstRow) {
                    resizeToPreferredWidth(i);
                }
                if (i == handle.childNodes.length - 1) {
                    handle.appendChild(items[index].handle);
                } else {
                    handle.insertBefore(items[index].handle, handle.childNodes[i]);
                }
                if (index == lastItemSrcRow && index - 1 >= 0) {
                    resizeToMaximumWidth(index - 1);
                }
                if (i == lastItemDstRow) {
                    resizeToMaximumWidth(i);
                }
            }
        }
    }

    void setItemSizes(Point[] sizes) {
        if (sizes == null) error(SWT.ERROR_NULL_ARGUMENT);
        int count = items.length;
        if (sizes.length != count) error(SWT.ERROR_INVALID_ARGUMENT);
        for (int i = 0; i < count; i++) {
            items[i].setSize(sizes[i].x, sizes[i].y);
        }
    }

    /**
 * Sets whether or not the receiver is 'locked'. When a coolbar
 * is locked, its items cannot be repositioned.
 *
 * @param locked lock the coolbar if true, otherwise unlock the coolbar
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @since 2.0
 */
    public void setLocked(boolean locked) {
        checkWidget();
        this.locked = locked;
        OS.updateCSSClass(handle, "cool-bar-locked", locked);
    }

    /**
 * Sets the indices of all item(s) in the receiver that will
 * begin on a new row. The indices are given in the order in
 * which they are currently being displayed. The 0th item
 * always begins the first row, therefore it does not count
 * as a wrap index. If indices is null or empty, the items
 * will be placed on one line.
 *
 * @param indices an array of wrap indices, or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
    public void setWrapIndices(int[] indices) {
        checkWidget();
        if (indices == null) indices = new int[0];
        int count = getItemCount();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0 || indices[i] >= count) {
                error(SWT.ERROR_INVALID_RANGE);
            }
        }
        setRedraw(false);
        CoolItem[] items = getItems();
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            if (item.getWrap()) {
                resizeToPreferredWidth(i - 1);
                item.setWrap(false);
            }
        }
        resizeToMaximumWidth(count - 1);
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            if (0 <= index && index < items.length) {
                CoolItem item = items[index];
                item.setWrap(true);
                resizeToMaximumWidth(index - 1);
            }
        }
        setRedraw(true);
    }

    protected boolean SetWindowPos(Object hWnd, Object hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags) {
        int lines = getVerticalLines();
        int lineNo = 0, itemNo = 0;
        for (int i = 0; i < items.length; i++) {
            CoolItem item = items[i];
            if (items[i].wrap) {
                lineNo++;
                itemNo = 0;
            }
            CSSStyle s = item.handle.style;
            Rectangle bounds = item.getBounds();
            s.left = bounds.x + "px";
            s.top = bounds.y + "px";
            int w = bounds.width - 11;
            s.width = (w > 0 ? w : 0) + "px";
            s.height = bounds.height + "px";
            String hCSS = "none", vCSS = "none";
            if (lineNo == 0) {
                if (lines > 1) {
                    hCSS = "bottom";
                }
            } else if (0 < lineNo && lineNo < lines - 1) {
                hCSS = "both";
            } else {
                hCSS = "top";
            }
            if (itemNo == 0) {
                if (!isLastItemOfRow(i)) {
                    vCSS = "right";
                }
            } else if (0 < lineNo && !isLastItemOfRow(i)) {
                vCSS = "both";
            } else {
                vCSS = "left";
            }
            Element e = item.handle;
            String key = "cool-item-border-";
            String cssClazz = key + hCSS + "-" + vCSS;
            String className = e.className;
            if (className == null || className.length() == 0) {
                e.className = cssClazz;
            } else {
                String[] newClazz = new String[0];
                newClazz[0] = cssClazz;
                String[] clazz = className.split("\\s");
                for (int k = 0; k < clazz.length; k++) {
                    if (clazz[k].indexOf(key) == -1) {
                        newClazz[newClazz.length] = clazz[k];
                    }
                }
                {
                }
            }
            if (item.control != null) {
                int ww = w - 2 - (isLastItemOfRow(i) ? 0 : 2);
                boolean more = false;
                if ((item.style & SWT.DROP_DOWN) != 0) {
                    more = item.control.computeSize(SWT.DEFAULT, bounds.height).x + 8 >= ww;
                    OS.updateCSSClass(item.handle, "cool-item-more-enabled", more);
                }
                if (more) {
                    item.moreHandle.style.height = (bounds.height - 6 > 0 ? bounds.height - 6 : 0) + "px";
                    s.width = (w - 12 > 0 ? w - 12 : 0) + "px";
                    item.control.setSize(ww - 8, bounds.height);
                } else {
                    item.control.setSize(ww, bounds.height);
                }
            }
            itemNo++;
        }
        if (uFlags == -1) {
            return false;
        }
        return super.SetWindowPos(hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
    }
}
