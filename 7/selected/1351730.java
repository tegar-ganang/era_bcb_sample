package org.maveryx.jRobot.guiObject;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import org.jdom.Element;
import abbot.tester.JTabbedPaneLocation;
import abbot.tester.JTabbedPaneTester;

public class AUTPageTabList extends AUTObject {

    private JTabbedPaneTester tab = new JTabbedPaneTester();

    public Object selectTabIndex(Component c, Element e, String[] index) {
        Object obj = null;
        tab.actionSelectTab(c, new JTabbedPaneLocation(Integer.parseInt(index[0])));
        return obj;
    }

    public Object selectTabTitle(Component c, Element e, String[] tabTitle) {
        Object obj = null;
        tab.actionSelectTab(c, new JTabbedPaneLocation(tabTitle[0]));
        return obj;
    }

    public Integer getItemCount(Component c, Element e) {
        return c.getAccessibleContext().getAccessibleChildrenCount();
    }

    public String[] getItemIndex(Component c, Element e, String[] index) {
        int indice = Integer.parseInt(index[0]);
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

    public String[] getItemString(Component c, Element e, String[] tabTitle) {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            out[i] = null;
        }
        Element parent = e;
        String childName = null;
        AccessibleRole childRole = null;
        AccessibleContext menu = c.getAccessibleContext();
        Accessible child = null;
        int count = menu.getAccessibleChildrenCount();
        for (int k = 0; k < count; k++) {
            child = menu.getAccessibleChild(k);
            childName = child.getAccessibleContext().getAccessibleName();
            childRole = child.getAccessibleContext().getAccessibleRole();
            if (childName == null) childName = "null";
            if (childName.matches(tabTitle[0])) break;
        }
        if (childName.matches(tabTitle[0])) {
            out[0] = childName;
            out[1] = childRole.toDisplayString(Locale.UK);
            out[2] = parent.getAttributeValue("accessibleName");
            out[3] = parent.getAttributeValue("accessibleRole");
        } else {
            out = null;
        }
        return out;
    }

    public String[] getSelectedItem(Component c, Element e) {
        String[] out = new String[4];
        for (int i = 0; i < 4; i++) {
            out[i] = null;
        }
        Element parent = e;
        AccessibleContext menu = c.getAccessibleContext();
        Accessible child = null;
        int count = menu.getAccessibleChildrenCount();
        for (int k = 0; k < count; k++) {
            child = menu.getAccessibleChild(k);
            if (child.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED)) break;
        }
        if (child.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED)) {
            if (child.getAccessibleContext().getAccessibleName() != null) {
                out[0] = child.getAccessibleContext().getAccessibleName();
            }
            out[1] = child.getAccessibleContext().getAccessibleRole().toDisplayString(Locale.UK);
            out[2] = parent.getAttributeValue("accessibleName");
            out[3] = parent.getAttributeValue("accessibleRole");
        } else {
            out = null;
        }
        return out;
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

    public Boolean hasItems(Component c, Element e, String[] tabItems) {
        int size = tabItems.length;
        List<Accessible> items = new ArrayList<Accessible>(size);
        for (int i = 0; i < size; i++) {
            items.add(i, getItem(c, e, tabItems[i]));
        }
        boolean out = items.contains(null);
        return !out;
    }

    private Accessible getItem(Component c, Element e, String item) {
        String childName = null;
        AccessibleContext menu = c.getAccessibleContext();
        Accessible child = null;
        Accessible out = null;
        int count = menu.getAccessibleChildrenCount();
        for (int k = 0; k < count; k++) {
            child = menu.getAccessibleChild(k);
            childName = child.getAccessibleContext().getAccessibleName();
            if (childName == null) childName = "null";
            if (childName.matches(item)) {
                out = child;
                break;
            }
        }
        return out;
    }
}
