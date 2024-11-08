package org.maveryx.jRobot.guiObject;

import java.awt.Component;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import org.jdom.Element;
import abbot.tester.JListTester;

public class AUTList extends AUTObject {

    private JListTester list = new JListTester();

    public Object selectIndex(Component c, Element e, String[] index) {
        Object obj = null;
        int indice = Integer.valueOf(index[0]);
        list.actionSelectIndex(c, indice);
        return obj;
    }

    public Object selectIndexs(Component c, Element e, String[] indexs) {
        Object obj = null;
        int length = indexs.length;
        for (int i = 0; i < length; i++) {
            int indice = Integer.valueOf(indexs[i]);
            list.actionSelectIndex(c, indice);
        }
        return obj;
    }

    public Object selectItem(Component c, Element e, String[] item) {
        Object obj = null;
        list.actionSelectItem(c, item[0]);
        return obj;
    }

    public Object selectItems(Component c, Element e, String[] items) {
        Object obj = null;
        int length = items.length;
        for (int i = 0; i < length; i++) {
            list.actionSelectItem(c, items[i]);
        }
        return obj;
    }

    public Object selectRange(Component c, Element e, String[] range) {
        Object obj = null;
        int start = Integer.valueOf(range[0]);
        int end = Integer.valueOf(range[1]);
        for (int i = start; i < end; i++) {
            list.actionSelectIndex(c, i);
        }
        return obj;
    }

    public Object selectAll(Component c, Element e) {
        Object obj = null;
        list.actionActionMap(c, "select-all");
        return obj;
    }

    public Object deselectIndex(Component c, Element e, String[] index) {
        Object obj = null;
        int indice = Integer.valueOf(index[0]);
        list.actionSelectIndex(c, indice);
        return obj;
    }

    public Object deselectIndexs(Component c, Element e, String[] indexs) {
        Object obj = null;
        int length = indexs.length;
        for (int i = 0; i < length; i++) {
            int indice = Integer.valueOf(indexs[i]);
            list.actionSelectIndex(c, indice);
        }
        return obj;
    }

    public Object deselectItem(Component c, Element e, String[] item) {
        Object obj = null;
        list.actionSelectItem(c, item[0]);
        return obj;
    }

    public Object deselectItems(Component c, Element e, String[] items) {
        Object obj = null;
        int length = items.length;
        for (int i = 0; i < length; i++) {
            list.actionSelectItem(c, items[i]);
        }
        return obj;
    }

    public Object deselectRange(Component c, Element e, String[] range) {
        Object obj = null;
        int start = Integer.valueOf(range[0]);
        int end = Integer.valueOf(range[1]);
        for (int i = start; i < end; i++) {
            list.actionSelectIndex(c, i);
        }
        return obj;
    }

    public Object deselectAll(Component c, Element e) {
        Object obj = null;
        list.actionActionMap(c, "select-all");
        return obj;
    }

    public String[] getItemIndex(Component c, Element e, String[] index) {
        int indice = 2 * Integer.parseInt(index[0]);
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            out[i] = null;
        }
        Element parent = e;
        String childName = null;
        String childRole = null;
        String[] children = null;
        AccessibleContext menu = c.getAccessibleContext();
        Accessible child = null;
        int count = menu.getAccessibleChildrenCount();
        if (count != 0) {
            children = new String[(2 * count)];
            for (int i = 0; i < (2 * count); i++) {
                children[i] = null;
            }
        }
        if (children != null) {
            int j = 0;
            for (int i = 0; i < count; i++) {
                child = menu.getAccessibleChild(i);
                childName = child.getAccessibleContext().getAccessibleName();
                childRole = child.getAccessibleContext().getAccessibleRole().toDisplayString(Locale.UK);
                if (stringToRole(childRole) != null) {
                    children[j] = childName;
                    children[j + 1] = childRole;
                    j = j + 2;
                }
            }
            out[0] = children[indice];
            out[1] = children[indice + 1];
            out[2] = parent.getAttributeValue("accessibleName");
            out[3] = parent.getAttributeValue("accessibleRole");
        } else {
            out = null;
        }
        return out;
    }

    public Integer getItemCount(Component c, Element e) {
        return c.getAccessibleContext().getAccessibleChildrenCount();
    }

    public String[] getItems(Component c, Element e) {
        String[] out = null;
        Element parent = e;
        AccessibleContext scambio = c.getAccessibleContext();
        int max = scambio.getAccessibleChildrenCount();
        if (max != 0) {
            out = new String[max * 4];
            for (int p = 0; p < max * 4; p++) {
                out[p] = null;
            }
            int j = 0;
            for (int k = 0; k < max; k++) {
                Accessible child = scambio.getAccessibleChild(k);
                out[j] = child.getAccessibleContext().getAccessibleName();
                out[j + 1] = child.getAccessibleContext().getAccessibleRole().toDisplayString(Locale.UK);
                out[j + 2] = parent.getAttributeValue("accessibleName");
                out[j + 3] = parent.getAttributeValue("accessibleRole");
                j = j + 4;
            }
        }
        return out;
    }

    public String[] getSelection(Component c, Element e) {
        String[] out = null;
        Element parent = e;
        int sel = c.getAccessibleContext().getAccessibleSelection().getAccessibleSelectionCount();
        if (sel != 0) {
            out = new String[4 * sel];
            for (int i = 0; i < 4 * sel; i++) {
                out[i] = null;
            }
            int k = 0;
            for (int i = 0; i < sel; i++) {
                AccessibleContext selected = c.getAccessibleContext().getAccessibleSelection().getAccessibleSelection(i).getAccessibleContext();
                if (selected.getAccessibleName() != null) {
                    out[k] = selected.getAccessibleName();
                }
                out[k + 1] = selected.getAccessibleRole().toDisplayString(Locale.UK);
                out[k + 2] = parent.getAttributeValue("accessibleName");
                out[k + 3] = parent.getAttributeValue("accessibleRole");
                k = k + 4;
            }
        }
        return out;
    }

    public Integer[] getSelectionIndices(Component c, Element e) {
        Integer[] out = null;
        int sel = c.getAccessibleContext().getAccessibleSelection().getAccessibleSelectionCount();
        if (sel != 0) {
            out = new Integer[sel];
            for (int i = 0; i < sel; i++) {
                out[i] = null;
            }
            for (int i = 0; i < sel; i++) {
                out[i] = c.getAccessibleContext().getAccessibleSelection().getAccessibleSelection(i).getAccessibleContext().getAccessibleIndexInParent();
            }
        }
        return out;
    }
}
