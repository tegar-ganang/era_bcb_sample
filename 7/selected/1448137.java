package org.maveryx.jRobot.guiObject;

import java.awt.Component;
import java.util.Locale;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import org.jdom.Element;
import abbot.tester.JPopupMenuTester;

public class AUTPopupMenu extends AUTObject {

    private JPopupMenuTester popupMenu = new JPopupMenuTester();

    public Object click(Component c, Element e) {
        Object object = null;
        popupMenu.actionClick(c);
        return object;
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

    public String[] getItemString(Component c, Element e, String[] item) {
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
            if (childName.matches(item[0])) break;
        }
        if (childName.matches(item[0])) {
            out[0] = childName;
            out[1] = childRole.toDisplayString(Locale.UK);
            out[2] = parent.getAttributeValue("accessibleName");
            out[3] = parent.getAttributeValue("accessibleRole");
        } else {
            out = null;
        }
        return out;
    }

    public String[] getItemAtPath(Component c, Element e, String[] path) {
        String[] out = null;
        String sep = "/";
        int tmpLen = path[0].length();
        String[] item = new String[1];
        item[0] = null;
        int inizio = 0;
        if (path[0].length() != 0) {
            out = new String[4];
            for (int i = 0; i < 4; i++) {
                out[i] = null;
            }
            int posizione = path[0].indexOf(sep);
            item[0] = path[0].substring(inizio, posizione - 1);
            if (posizione + 1 < tmpLen) {
                inizio = posizione + 1;
                path[0] = path[0].substring(inizio);
                out = getItemAtPath(c, e, path);
            } else {
                out = getItemString(c, e, item);
            }
        }
        return out;
    }

    public String[] getSubMenu(Component c, Element e, String[] item) {
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
        int j = 0;
        do {
            child = menu.getAccessibleChild(j);
            childName = child.getAccessibleContext().getAccessibleName();
            childRole = child.getAccessibleContext().getAccessibleRole();
            j++;
            if (childName == null) childName = "null";
        } while ((!childName.equals(item[0]) && childRole.equals(AccessibleRole.MENU)) || j < count);
        if (childName.equals(item[0]) && childRole.equals(AccessibleRole.MENU)) {
            out[0] = childName;
            out[1] = childRole.toDisplayString(Locale.UK);
            out[2] = parent.getAttributeValue("accessibleName");
            out[3] = parent.getAttributeValue("accessibleRole");
        } else {
            out = null;
        }
        return out;
    }
}
