package org.galab.saveableobject;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;
import java.awt.*;
import org.galab.util.*;
import org.galab.saveableobject.controller.*;
import org.galab.saveableobject.bot.*;
import org.galab.compare.*;
import org.galab.frame.*;
import org.galab.saveableobject.world.*;

public class SaveableObject extends Object {

    public SaveableObject() {
        reset();
    }

    private void reset() {
        setParent(null);
        setChildren(null);
        setName(null);
        setGenotype(null);
    }

    public SaveableObject(boolean isParent, SaveableObject newParent) {
        setParent(newParent);
        setChildren(null);
        setGenotype(null);
    }

    public SaveableObject(SaveableObject object) {
        setParent(object.getParent());
        setChildren(null);
        setGenotype(object.getGenotype());
    }

    public void setupFamily() {
        setup();
        for (int i = 0; i < getNumChildren(); i++) {
            SaveableObject child = getChild(i);
            child.setupFamily();
        }
    }

    public void setup() {
    }

    public void process() {
    }

    public void end() {
    }

    public void postBuild() {
    }

    public void orderChildrenByDrawDepth() {
        Collections.sort(getChildren(), new CompareSaveableObjectsByDrawDepth());
    }

    public SaveableObject getTopParent() {
        if (parent == null) {
            return this;
        } else {
            return parent.getTopParent();
        }
    }

    public Vector getChildren() {
        return children;
    }

    public void addChild(SaveableObject newChild) {
        children.add(newChild);
        if (this instanceof Bot && newChild instanceof Controller) {
            ((VisualObject) newChild).setIsDrawn(false);
        }
    }

    public void addChildren(Vector newChildren) {
        if (newChildren != null) {
            for (int i = 0; i < newChildren.size(); i++) {
                addChild((SaveableObject) newChildren.get(i));
            }
        }
    }

    public void setChildren(Vector newChildren) {
        children = new Vector();
        if (newChildren != null) {
            addChildren(newChildren);
        }
    }

    public SaveableObject getChild(int index) {
        return (SaveableObject) children.get(index);
    }

    public void removeChildren(Vector childrenToRemove) {
        children.removeAll(childrenToRemove);
    }

    public void removeChild(int index) {
        SaveableObject c = (SaveableObject) children.get(index);
        if (index >= 0 && index < children.size()) {
            children.remove(index);
            if (c.getParent() == this) {
                c.removeParent();
            }
        }
    }

    public void removeChild(SaveableObject object) {
        int index = children.indexOf(object);
        if (index == -1) {
            System.out.println("SaveableObject::removeChild - trying to remove a child that is not part of the saveable object");
            return;
        }
        removeChild(index);
    }

    public void removeChildWithoutComplaining(SaveableObject object) {
        int index = children.indexOf(object);
        if (index > -1) {
            removeChild(index);
        }
    }

    public void removeChild(String newName) {
        boolean removed = false;
        for (int i = 0; i < children.size(); i++) {
            SaveableObject child = getChild(i);
            if (child.getName().compareTo(newName) == 0) {
                if (removed == true) {
                    System.out.println("SaveableObject::removeChild - more than one child with the same name, only the first child removed");
                } else {
                    removed = true;
                    removeChild(child);
                }
            }
        }
    }

    public void removeAllChildren() {
        children.removeAllElements();
    }

    public void removeDescendant(SaveableObject object) {
        int index = children.indexOf(object);
        if (index != -1) {
            removeChild(index);
            return;
        }
        for (int i = 0; i < getNumChildren(); i++) {
            getChild(i).removeDescendant(object);
        }
    }

    public int getNumChildren() {
        return children.size();
    }

    public boolean isChild(SaveableObject possChild) {
        if (possChild.getParent() == this) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isChildOf(SaveableObject possParent) {
        Vector kids = possParent.getChildren();
        if (kids.contains(this)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDescendant(SaveableObject object) {
        if (object.getParent() == null) {
            return false;
        }
        if (object.getParent() == this) {
            return true;
        }
        return object.getParent().isDescendant(object);
    }

    public void setParent(SaveableObject newParent) {
        if (newParent != null) {
            parent = newParent;
            if (!parent.getChildren().contains(this)) {
                parent.addChild(this);
            }
        } else {
            removeParent();
        }
    }

    public void removeParent() {
        SaveableObject tmp = parent;
        parent = null;
        if (tmp != null && tmp.getChildren().contains(this)) {
            tmp.removeChild(this);
        }
    }

    public SaveableObject getParent() {
        return parent;
    }

    public SaveableObject getFirstChildOfClass(String strcls) {
        try {
            Class cls = Class.forName(strcls);
            Vector children = getChildren();
            for (int i = 0; i < children.size(); i++) {
                if (cls.isInstance(children.get(i))) {
                    return (SaveableObject) children.get(i);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Vector getChildrenOfClass(String strcls) {
        Vector ans = new Vector();
        try {
            Class cls = Class.forName(strcls);
            Vector children = getChildren();
            for (int i = 0; i < children.size(); i++) {
                if (cls.isInstance(children.get(i))) {
                    ans.add(children.get(i));
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return ans;
    }

    public Vector getChildrenIfPhysObj() {
        return getChildrenOfClass("org.galab.saveableobject.PhysicalObject");
    }

    public Vector getChildrenIfVisObj() {
        return getChildrenOfClass("org.galab.saveableobject.VisualObject");
    }

    public PhysicalObject getParentIfPhysObj() {
        SaveableObject p = getParent();
        if (p != null && p instanceof PhysicalObject) {
            return (PhysicalObject) p;
        } else {
            return null;
        }
    }

    public VisualObject getParentIfVisObj() {
        SaveableObject p = getParent();
        if (p != null && p instanceof VisualObject) {
            return (VisualObject) p;
        } else {
            return null;
        }
    }

    public Vector getHierarchyAsVector() {
        Vector vector = new Vector();
        vector.add(this);
        for (int i = 0; i < children.size(); i++) {
            vector.addAll(((SaveableObject) children.get(i)).getHierarchyAsVector());
        }
        return vector;
    }

    public void step() {
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        if (name != null) {
            return name;
        } else {
            return "-unnamed-";
        }
    }

    public Vector getNamesOfChildren() {
        Vector ans = new Vector();
        int n = 0;
        Vector children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            String nme = ((SaveableObject) children.get(i)).getName();
            if (nme.equals("-unnamed-")) {
                ans.add(new String(getName() + " (" + n + ")"));
                n++;
            } else {
                ans.add(nme);
            }
        }
        return ans;
    }

    public Vector getFitnessesOfChildren() {
        Vector ans = new Vector();
        int n = 0;
        Vector children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            SaveableObject sve = (SaveableObject) children.get(i);
            Double fitness;
            if (sve instanceof Bot) {
                fitness = new Double(((Bot) sve).getFitness());
            } else {
                fitness = new Double(-1);
            }
            ans.add(fitness);
        }
        return ans;
    }

    public Vector getGenotype() {
        return genotype;
    }

    public void setGenotype(Vector newGenotype) {
        if (newGenotype != null) {
            genotype = new Vector();
            for (int i = 0; i < newGenotype.size(); i++) {
                genotype.add(newGenotype.get(i));
            }
        } else {
            genotype = new Vector();
        }
    }

    public void copyGenotype(Vector newGenotype) {
        if (newGenotype != null) {
            genotype = new Vector();
            for (int i = 0; i < newGenotype.size(); i++) {
                genotype.add(new String((String) newGenotype.get(i)));
            }
        } else {
            genotype = new Vector();
        }
    }

    public void buildFromGenotype() {
        gtpPos = -1;
        ids = new Vector();
        returnedContent = new String();
        removeAllChildren();
        isOpeningTag(readLine());
        String tag = getTag(returnedContent);
        buildObject(this, tag);
    }

    public void buildObject(SaveableObject curObj, String openingTag) {
        String closeTag = "</" + openingTag + ">";
        String tag = "";
        do {
            boolean istag = isOpeningTag(readLine());
            tag = getTag(returnedContent);
            if (istag) {
                String cls = getAttribute("class", returnedContent).replace('_', '.');
                if (tag.equals("attr")) {
                    String name = getAttribute("name", returnedContent);
                    int fxd = Integer.parseInt(getAttribute("fixed", returnedContent));
                    Class objCls = curObj.getClass();
                    try {
                        Field attr = objCls.getField(name);
                        double min = getMin(name, curObj);
                        double max = getMax(name, curObj);
                        Object builtAttr = buildAttr(cls, tag, min, max);
                        attr.set(curObj, builtAttr);
                        Field attr_FIXED = objCls.getField(name + "_FIXED");
                        attr_FIXED.set(curObj, new Integer(fxd));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        System.out.println("SaveableObject::buildObject - Error reading genotype: This attribute (" + name + ") cannot be used with objects of " + objCls + ".");
                        e.printStackTrace();
                    }
                } else {
                    try {
                        SaveableObject newObj = (SaveableObject) Class.forName(cls).newInstance();
                        newObj.setParent(curObj);
                        if (cls.equals("org.galab.saveableobject.controller.Node")) {
                            int id = Integer.parseInt(getAttribute("id", returnedContent));
                            if (id >= ids.size()) {
                                ids.setSize(id + 1);
                            }
                            ids.add(id, newObj);
                            if (curObj instanceof ControllerComponent) {
                                ((Node) newObj).setControllerComponent((ControllerComponent) curObj);
                            } else {
                                System.out.println("SaveableObject::buildObject - Node with a non-ControllerComponent parent at line " + gtpPos + ".");
                            }
                        }
                        buildObject(newObj, getTag(returnedContent));
                    } catch (ClassNotFoundException e) {
                        System.out.println("SaveableObject::buildObject - Error reading genotype on line " + gtpPos + ".");
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        System.out.println("SaveableObject::buildObject - Error reading genotype on line " + gtpPos + ".");
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        System.out.println("SaveableObject::buildObject - Error reading genotype on line " + gtpPos + ".");
                        e.printStackTrace();
                    }
                }
            }
        } while (!tag.equals(closeTag));
        curObj.postBuild();
    }

    private Object buildAttr(String cls, String tag, double min, double max) {
        cls = cls.replace('.', '_');
        try {
            Object[] args = new Object[3];
            args[0] = tag;
            args[1] = new Double(min);
            args[2] = new Double(max);
            Class[] params = new Class[3];
            params[0] = args[0].getClass();
            params[1] = Double.TYPE;
            params[2] = Double.TYPE;
            Method wrtAttr = this.getClass().getMethod("buildAttr_" + cls, params);
            return wrtAttr.invoke(this, args);
        } catch (NoSuchMethodException e) {
            System.out.println("SaveableObject::buildAttr - Unrecognised class when reading genotype: " + cls + ". Please write the custom methods writeAttr_" + cls + " and readAttr_" + cls + " as public methods in PhysicalObject or a subclass, with the correct parameter types.  See e.g. writeAttr_java_awt_Polygon and readAttr_java_awt_Polygon for examples.");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.out.println("SaveableObject::buildAttr - Reading genotype line " + gtpPos + ", method buildAttr_" + cls + " threw an exception.");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Polygon buildAttr_java_awt_Polygon(String openingTag, double min, double max) {
        int npoints = 0;
        int[] xpoints = new int[1000];
        int[] ypoints = new int[1000];
        String tag;
        String closeTag = "</" + openingTag + ">";
        do {
            boolean istag = isOpeningTag(readLine());
            tag = getTag(returnedContent);
            if (istag && tag.equals("point")) {
                Point p = buildAttr_java_awt_Point(tag, min, max);
                xpoints[npoints] = p.x;
                ypoints[npoints] = p.y;
                npoints++;
            }
        } while (!tag.equals(closeTag));
        if (npoints > 0) {
            return new Polygon(xpoints, ypoints, npoints);
        } else {
            return null;
        }
    }

    public Point buildAttr_java_awt_Point(String openingTag, double min, double max) {
        String[] wanted = new String[2];
        wanted[0] = "x";
        wanted[1] = "y";
        double[] gotBits = getBits(wanted, openingTag, min, max);
        return new Point((int) gotBits[0], (int) gotBits[1]);
    }

    public Color buildAttr_java_awt_Color(String openingTag, double min, double max) {
        String[] wanted = new String[3];
        wanted[0] = "red";
        wanted[1] = "green";
        wanted[2] = "blue";
        double[] gotBits = getBits(wanted, openingTag, min, max);
        return new Color((int) gotBits[0], (int) gotBits[1], (int) gotBits[2]);
    }

    public Double buildAttr_java_lang_Double(String openingTag, double min, double max) {
        Double d = new Double(Util.scaleUp(readNumericLine(), min, max));
        readLine();
        return d;
    }

    public Integer buildAttr_java_lang_Integer(String openingTag, double min, double max) {
        Integer i = new Integer((int) (Util.scaleUp(readNumericLine(), min, max)));
        readLine();
        return i;
    }

    public String buildAttr_java_lang_String(String openingTag, double min, double max) {
        String s = readLine().trim();
        readLine();
        return s;
    }

    public Boolean buildAttr_java_lang_Boolean(String openingTag, double min, double max) {
        double d = readNumericLine();
        readLine();
        if (d > 0.5) {
            return new Boolean(true);
        } else {
            return new Boolean(false);
        }
    }

    public V2 buildAttr_org_galab_util_V2(String openingTag, double min, double max) {
        String[] wanted = new String[2];
        wanted[0] = "x";
        wanted[1] = "y";
        double[] gotBits = getBits(wanted, openingTag, min, max);
        return new V2(gotBits[0], (int) gotBits[1]);
    }

    public Angle buildAttr_org_galab_util_Angle(String openingTag, double min, double max) {
        Angle a = new Angle(Util.scaleUp(readNumericLine(), min, max));
        readLine();
        return a;
    }

    public Node buildAttr_org_galab_saveableobject_controller_Node(String openingTag, double min, double max) {
        int i = (int) readNumericLine();
        readLine();
        return (Node) ids.get(i);
    }

    public Vector buildAttr_java_util_Vector(String openingTag, double min, double max) {
        Vector ans = new Vector();
        String closeTag = "</" + openingTag + ">";
        String tag;
        do {
            boolean istag = isOpeningTag(readLine());
            tag = getTag(returnedContent);
            if (istag && tag.equals("object")) {
                try {
                    String clsName = getAttribute("class", returnedContent);
                    Object[] args = new Object[3];
                    args[0] = tag;
                    args[1] = new Double(min);
                    args[2] = new Double(max);
                    Class[] params = new Class[3];
                    params[0] = args[0].getClass();
                    params[1] = Double.TYPE;
                    params[2] = Double.TYPE;
                    Method bldAttr = this.getClass().getMethod("buildAttr_" + clsName, params);
                    Object o = bldAttr.invoke(this, args);
                    ans.add(o);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } while (!tag.equals(closeTag));
        return ans;
    }

    public double[] getBits(String[] wanted, String openingTag, double min, double max) {
        String tag;
        double[] ans = new double[wanted.length];
        String closeTag = "</" + openingTag + ">";
        do {
            boolean istag = isOpeningTag(readLine());
            tag = getTag(returnedContent);
            if (istag) {
                for (int i = 0; i < wanted.length; i++) {
                    if (tag.equals((String) wanted[i])) {
                        ans[i] = Util.scaleUp(readNumericLine(), min, max);
                        readLine();
                    }
                }
            } else if (returnedContent.equals(closeTag)) {
            } else {
                System.out.println("SaveableObject::getBits - Strange content encountered reading genotype at line " + gtpPos + ": " + returnedContent);
            }
        } while (tag != null && !returnedContent.equals(closeTag));
        return ans;
    }

    public static String getAttribute(String attr, String input) {
        int begin = input.indexOf(attr);
        int end;
        if (begin > -1) {
            begin = input.indexOf("'", begin) + 1;
            end = input.indexOf("'", begin);
            return input.substring(begin, end);
        } else {
            return new String();
        }
    }

    public static String getTag(String input) {
        if (input != null) {
            int end = input.indexOf(" ");
            if (end > -1) {
                return input.substring(0, end);
            } else {
                return input;
            }
        } else {
            return null;
        }
    }

    public boolean isOpeningTag(String newStr) {
        if (newStr != null) {
            String str = newStr.trim();
            if ((str.charAt(0) == '<') && (str.charAt(str.length() - 1) == '>') && str.charAt(1) != '/') {
                returnedContent = str.substring(1, str.length() - 1);
                return true;
            } else {
                returnedContent = new String(str);
                return false;
            }
        } else {
            System.out.println("SaveableObject::isOpeningTag - Null String reading genotype at line " + gtpPos);
            returnedContent = null;
            return false;
        }
    }

    public void loadGenotype(BufferedReader f) {
        gtpIn = f;
        buildFromGenotype();
        gtpIn = null;
    }

    public void saveGenotype(BufferedWriter f) {
        gtpOut = f;
        createGenotype();
        gtpOut = null;
    }

    public void createGenotype() {
        ids = new Vector();
        gtpPos = 0;
        genotype = new Vector();
        addToGenotype(this, 0);
    }

    private void addToGenotype(SaveableObject object, int lev) {
        String cls = object.getClass().getName();
        String gencls = getWhatTheTagWillBe(object);
        if (object instanceof Node) {
            writeln("<" + gencls + " class='" + cls + "' id='" + ids.size() + "'>", lev);
            ids.add(object);
        } else {
            writeln("<" + gencls + " class='" + cls + "'>", lev);
        }
        object.orderChildren();
        Vector kids = object.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            addToGenotype((SaveableObject) kids.get(i), lev + 1);
        }
        Field[] attrs = getAttrs(object.getClass());
        for (int i = 0; i < attrs.length; i++) {
            String attrName = attrs[i].getName();
            String attrFixed = getFixed(attrName, object);
            try {
                Class attrClass = attrs[i].getType();
                double attrMin = getMin(attrName, object);
                double attrMax = getMax(attrName, object);
                writeAttr(attrs[i].get(object), attrName, attrFixed, attrClass, attrMin, attrMax, lev + 1);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        writeln("</" + gencls + ">", lev);
    }

    public void orderChildren() {
    }

    public Field[] getAttrs(Class cls) {
        Vector ansV = new Vector();
        Field[] flds = cls.getFields();
        for (int i = 0; i < flds.length; i++) {
            Class fldcls = flds[i].getType();
            if (!fldcls.isPrimitive()) {
                ansV.add(flds[i]);
            }
        }
        Field[] ans = new Field[ansV.size()];
        for (int i = 0; i < ans.length; i++) {
            ans[i] = (Field) ansV.get(i);
        }
        return ans;
    }

    public static int getFixedState(String name, Object object) {
        try {
            return object.getClass().getField(name + "_FIXED").getInt(object);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void setFixedState(String name, Object object, int state) {
        try {
            object.getClass().getField(name + "_FIXED").setInt(object, state);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static String getFixed(String name, Object object) {
        try {
            return String.valueOf(object.getClass().getField(name + "_FIXED").getInt(object));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        System.out.println("SaveableObject::getFixed - Could not find the fixed state of " + name + " (Class " + object.getClass().getName() + ") - returning SET_AND_FIXED by default");
        return String.valueOf(SET_AND_FIXED);
    }

    public static double getMin(String name, Object object) {
        try {
            return object.getClass().getField(name + "_MIN").getDouble(object);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        System.out.println("SaveableObject::getMin - Could not find the min value of " + name + " (Class " + object.getClass().getName() + ") - returning 0 by default");
        return 0;
    }

    public static double getMax(String name, Object object) {
        try {
            return object.getClass().getField(name + "_MAX").getDouble(object);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        System.out.println("SaveableObject::getMax - Could not find the max value of " + name + " (Class " + object.getClass().getName() + ") - returning 1 by default");
        return 1;
    }

    private void writeAttr(Object attr, String name, String fixed, Class cls, double min, double max, int lev) {
        if (attr != null) {
            String clsName = cls.toString().substring(6).replace('.', '_');
            writeln("<attr class='" + clsName + "' name='" + name + "' fixed='" + fixed + "'>", lev);
            try {
                Class[] params = new Class[4];
                params[0] = cls;
                params[1] = Double.TYPE;
                params[2] = Double.TYPE;
                params[3] = Integer.TYPE;
                Method wrtAttr = this.getClass().getMethod("writeAttr_" + clsName, params);
                Object[] args = new Object[4];
                args[0] = attr;
                args[1] = new Double(min);
                args[2] = new Double(max);
                args[3] = new Integer(lev + 1);
                wrtAttr.invoke(this, args);
            } catch (NoSuchMethodException e) {
                System.out.println("SaveableObject::writeAttr - Unrecognised " + cls.toString() + " when writing genotype (line " + gtpPos + "): " + clsName + ". Please write the custom methods writeAttr_" + clsName + " and buildAttr_" + clsName + " as public methods in SaveableObject or a subclass, with the correct parameter types.  See e.g. writeAttr_java_awt_Polygon and buildAttr_java_awt_Polygon for examples.");
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                System.out.println("SaveableObject::writeAttr - Writing line " + gtpPos + " of genotype, method writeAttr_" + clsName + " threw an exception: " + e.getTargetException());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            writeln("</attr>", lev);
        }
    }

    public void writeAttr_java_awt_Polygon(Polygon p, double min, double max, int lev) {
        for (int i = 0; i < p.npoints; i++) {
            writeln("<point>", lev);
            writeAttr_java_awt_Point(new Point(p.xpoints[i], p.ypoints[i]), min, max, lev + 1);
            writeln("</point>", lev);
        }
    }

    public void writeAttr_java_awt_Point(Point p, double min, double max, int lev) {
        writeln("<x>", lev);
        writeln(Util.scaleDown(p.x, min, max), lev);
        writeln("</x>", lev);
        writeln("<y>", lev);
        writeln(Util.scaleDown(p.y, min, max), lev);
        writeln("</y>", lev);
    }

    public void writeAttr_java_awt_Color(Color c, double min, double max, int lev) {
        writeln("<red>", lev);
        writeln(Util.scaleDown(c.getRed(), min, max), lev);
        writeln("</red>", lev);
        writeln("<green>", lev);
        writeln(Util.scaleDown(c.getGreen(), min, max), lev);
        writeln("</green>", lev);
        writeln("<blue>", lev);
        writeln(Util.scaleDown(c.getBlue(), min, max), lev);
        writeln("</blue>", lev);
    }

    public void writeAttr_java_lang_Double(Double d, double min, double max, int lev) {
        writeln(Util.scaleDown(d.doubleValue(), min, max), lev);
    }

    public void writeAttr_java_lang_Integer(Integer i, double min, double max, int lev) {
        writeln(Util.scaleDown(i.intValue(), min, max), lev);
    }

    public void writeAttr_java_lang_String(String s, double min, double max, int lev) {
        writeln(s, lev);
    }

    public void writeAttr_java_lang_Boolean(Boolean b, double min, double max, int lev) {
        if (b.booleanValue()) {
            writeln(0.9, lev);
        } else {
            writeln(0.1, lev);
        }
    }

    public void writeAttr_org_galab_util_V2(V2 v, double min, double max, int lev) {
        writeln("<x>", lev);
        writeln(Util.scaleDown(v.getX(), min, max), lev);
        writeln("</x>", lev);
        writeln("<y>", lev);
        writeln(Util.scaleDown(v.getY(), min, max), lev);
        writeln("</y>", lev);
    }

    public void writeAttr_org_galab_util_Angle(Angle a, double min, double max, int lev) {
        writeln(Util.scaleDown(a.toDouble(), min, max), lev);
    }

    public void writeAttr_org_galab_saveableobject_controller_Node(Node n, double min, double max, int lev) {
        if (n == null) {
            System.out.println("SaveableObject::writeAttr_org_galab_saveableobject_controller_Node - Null node on line " + gtpPos + ".");
        }
        int i = ids.indexOf(n);
        if (i == -1) {
            System.out.println("SaveableObject::writeAttr_org_galab_saveableobject_controller_Node - Node missing from ids list on line " + gtpPos + ".");
        }
        writeln(i, lev);
    }

    public void writeAttr_java_util_Vector(Vector v, double min, double max, int lev) {
        for (int i = 0; i < v.size(); i++) {
            Object obj = v.get(i);
            Class cls = obj.getClass();
            String clsName = cls.toString().substring(6).replace('.', '_');
            writeln("<object class='" + clsName + "'>", lev);
            try {
                Class[] params = new Class[4];
                params[0] = cls;
                params[1] = Double.TYPE;
                params[2] = Double.TYPE;
                params[3] = Integer.TYPE;
                Method wrtAttr = this.getClass().getMethod("writeAttr_" + clsName, params);
                Object[] args = new Object[4];
                args[0] = obj;
                args[1] = new Double(min);
                args[2] = new Double(max);
                args[3] = new Integer(lev + 1);
                wrtAttr.invoke(this, args);
            } catch (NoSuchMethodException e) {
                System.out.println("SaveableObject::writeAttr - Unrecognised " + cls.toString() + " when writing genotype (line " + gtpPos + "): " + clsName + ". Please write the custom methods writeAttr_" + clsName + " and buildAttr_" + clsName + " as public methods in SaveableObject or a subclass, with the correct parameter types.  See e.g. writeAttr_java_awt_Polygon and buildAttr_java_awt_Polygon for examples.");
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                System.out.println("SaveableObject::writeAttr - Writing line " + gtpPos + " of genotype, method writeAttr_" + clsName + " threw an exception:");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            writeln("</object>", lev);
        }
    }

    public void createGenotype(Vector parents, SaveableObject child, double crossoverRate) {
        Vector newGenotype;
        Vector genotypes = new Vector();
        Vector tags[];
        String tag;
        int numParentsNotChild = 0;
        Vector reader;
        String currentLine;
        int readerIndex = 0;
        if (parents != null) {
            newGenotype = new Vector();
            int indexes[] = new int[parents.size()];
            boolean validParent[] = new boolean[parents.size()];
            tags = new Vector[parents.size()];
            Vector readerTags = null;
            for (int i = 0; i < parents.size(); i++) {
                SaveableObject parent = (SaveableObject) parents.get(i);
                if (parent.getGenotype() == null || parent.getGenotype().size() == 0) {
                    parent.createGenotype();
                } else {
                    System.out.println("SaveableObject::createGenotype(Vector,SaveableObject,double) - parent unexpectedly has a genotype.");
                }
                genotypes.add(parent.getGenotype());
                indexes[i] = 0;
                validParent[i] = true;
                tags[i] = new Vector();
                if (parent != child) {
                    numParentsNotChild++;
                }
            }
            int index;
            if (numParentsNotChild == 0) {
                reader = (Vector) genotypes.get(0);
                index = 0;
            } else {
                do {
                    index = (int) (Math.random() * genotypes.size());
                    reader = (Vector) genotypes.get(index);
                } while (reader == child.getGenotype());
            }
            readerIndex = indexes[index];
            readerTags = tags[index];
            validParent[index] = false;
            currentLine = (String) reader.get(readerIndex);
            tag = getTagType(currentLine);
            moveAllToLine(genotypes, indexes, tag);
            for (int i = 0; i < genotypes.size(); i++) {
                tags[i].add(0, tag);
            }
            while (true) {
                newGenotype.add(reader.get(readerIndex));
                if (readerTags == null) {
                    System.out.println("SaveableObject::CreateGenotype(Vector,SaveableObject,double) - Unexpected readerTags==null");
                    break;
                }
                if (readerTags.size() == 0) {
                    break;
                }
                if (Math.random() < crossoverRate) {
                    int numPotentialParents = 0;
                    for (int i = 0; i < genotypes.size(); i++) {
                        if (validParent[i] == true) {
                            numPotentialParents++;
                        }
                    }
                    if (numPotentialParents > 0) {
                        do {
                            index = (int) (Math.random() * genotypes.size());
                        } while (validParent[index] == false);
                        reader = (Vector) genotypes.get(index);
                        readerIndex = indexes[index];
                        readerTags = tags[index];
                        validParent[index] = false;
                    }
                }
                readerIndex++;
                currentLine = (String) reader.get(readerIndex);
                tag = getTagType(currentLine);
                if (tag != null && tag.indexOf('/') == -1) {
                    int nextTag, nextEndTag;
                    for (int i = 0; i < genotypes.size(); i++) {
                        Vector genotype = (Vector) genotypes.get(i);
                        nextTag = getNextLineWithTag(genotype, indexes[i], tag);
                        String firstTag = (String) tags[i].get(0);
                        nextEndTag = getNextLineWithTag(genotype, indexes[i], createEndTag(firstTag));
                        if (nextTag < nextEndTag) {
                            indexes[i] = nextTag;
                            if (genotype != reader) {
                                validParent[i] = true;
                            } else {
                                validParent[i] = false;
                            }
                            tags[i].add(0, tag);
                        } else {
                            validParent[i] = false;
                        }
                    }
                } else {
                    String firstTag = (String) readerTags.get(0);
                    String endTag = createEndTag(firstTag);
                    if (currentLine.indexOf(endTag) != -1) {
                        readerTags.remove(0);
                        for (int i = 0; i < genotypes.size(); i++) {
                            while (tags[i].size() > readerTags.size()) {
                                tags[i].remove(0);
                            }
                            if (tags[i].size() > 0) {
                                Vector genotype = (Vector) genotypes.get(i);
                                int newIndex = getNextLineWithTag(genotype, indexes[i], endTag);
                                firstTag = (String) tags[i].get(0);
                                int nextEndTag = getNextLineWithTag(genotype, indexes[i], createEndTag(firstTag));
                                if (newIndex < nextEndTag) {
                                    indexes[i] = newIndex;
                                    if (genotype != reader) {
                                        validParent[i] = true;
                                    } else {
                                        validParent[i] = false;
                                    }
                                } else {
                                    validParent[i] = false;
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < genotypes.size(); i++) {
                            validParent[i] = false;
                        }
                    }
                }
            }
            genotype = newGenotype;
        } else {
            genotype = new Vector();
        }
        for (int i = 0; i < parents.size(); i++) {
            SaveableObject parent = (SaveableObject) parents.get(i);
            parent.setGenotype(null);
        }
    }

    public void mateGenotypes(Population parents, SaveableObject child, double crossoverRate, double mutationChance, double mutationRate, int mutationType, int wrapType) {
        createGenotype(parents.getChildren(), child, crossoverRate);
        mutate(mutationChance, mutationRate, mutationType, wrapType);
    }

    public void mutate(double mutationChance, double mutationRate, int mutationType, int wrapType) {
        Vector objClassStack = new Vector();
        gtpPos = -1;
        String tag;
        String closeTag = "";
        do {
            boolean istag = isOpeningTag(readLine());
            tag = getTag(returnedContent);
            if (istag) {
                if (tag.equals("attr")) {
                    String name = getAttribute("name", returnedContent);
                    int fxd = Integer.parseInt(getAttribute("fixed", returnedContent));
                    if (fxd == SET_AND_EVOLVABLE || fxd == RANDOM_AND_EVOLVABLE) {
                        mutateAttr(name, (String) objClassStack.lastElement(), mutationChance, mutationRate, mutationType, wrapType);
                    } else {
                        skipAttr();
                    }
                } else {
                    if (closeTag.equals("")) {
                        closeTag = "</" + tag + ">";
                    }
                    String cls = getAttribute("class", returnedContent).replace('_', '.');
                    objClassStack.add(cls);
                }
            } else {
                objClassStack.removeElementAt(objClassStack.size() - 1);
            }
        } while (!tag.equals(closeTag));
    }

    private void skipAttr() {
        String tag = "";
        while (!tag.equals("</attr>")) {
            isOpeningTag(readLine());
            tag = returnedContent;
        }
    }

    private void mutateAttr(String attrName, String objClsStr, double mutationChance, double mutationRate, int mutationType, int wrapType) {
        try {
            Class objCls = Class.forName(objClsStr);
            Field minFld = objCls.getField(attrName + "_MIN");
            Field maxFld = objCls.getField(attrName + "_MAX");
            double min = minFld.getDouble(null);
            double max = maxFld.getDouble(null);
            Field attr = objCls.getField(attrName);
            Class attrCls = attr.getType();
            do {
                isOpeningTag(readLine());
                try {
                    double value = Double.parseDouble(returnedContent);
                    if (Math.random() < mutationChance) {
                        double newVal = mutateNumber(value, mutationType, wrapType, mutationRate, attrCls, min, max);
                        String newValue = Double.toString(newVal);
                        genotype.set(gtpPos, newValue);
                    }
                } catch (NumberFormatException e) {
                }
            } while (!returnedContent.equals("</attr>"));
        } catch (ClassNotFoundException f) {
            f.printStackTrace();
        } catch (IllegalAccessException f) {
            f.printStackTrace();
        } catch (NoSuchFieldException f) {
            f.printStackTrace();
        }
    }

    public double mutateNumber(double value, int mutationType, int wrapType, double mutationRate, Class cls, double min, double max) {
        String strCls = cls.getName().replace('.', '_');
        try {
            Class[] params = new Class[6];
            params[0] = Double.TYPE;
            params[1] = Integer.TYPE;
            params[2] = Integer.TYPE;
            params[3] = Double.TYPE;
            params[4] = Double.TYPE;
            params[5] = Double.TYPE;
            Method mutNo = this.getClass().getMethod("mutateNumber_" + strCls, params);
            Object[] args = new Object[6];
            args[0] = new Double(value);
            args[1] = new Integer(mutationType);
            args[2] = new Integer(wrapType);
            args[3] = new Double(mutationRate);
            args[4] = new Double(min);
            args[5] = new Double(max);
            return ((Double) mutNo.invoke(this, args)).doubleValue();
        } catch (NoSuchMethodException e) {
            System.out.println("SaveableObject::mutateNumber - Method does not exist: mutateNumber_" + strCls);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double mutateNumber_java_lang_Double(double value, int mutationType, int wrapType, double mutationRate, double min, double max) {
        Random r = new Random();
        double ans;
        switch(mutationType) {
            case UNIFORM:
                ans = (value - mutationRate) + (r.nextDouble() * mutationRate * 2);
                break;
            case GAUSSIAN:
                ans = value + (r.nextGaussian() * mutationRate);
                break;
            default:
                System.out.println("SaveableObject::mutateAttr - Unrecognised mutation type.");
                ans = value;
        }
        boolean gr1 = (ans >= 1);
        boolean le0 = (ans < 0);
        if (gr1 || le0) {
            switch(wrapType) {
                case STICK:
                    if (gr1) {
                        ans = 0.9999;
                    } else {
                        ans = 0;
                    }
                    break;
                case BOUNCE:
                    if (gr1) {
                        ans = 1 - (ans - Math.floor(ans));
                        if (ans == 1) {
                            ans = 0.9999;
                        }
                    } else {
                        ans = -Math.ceil(ans);
                    }
                    break;
                case WRAP:
                    if (gr1) {
                        ans = ans - Math.floor(ans);
                    } else {
                        ans = 1 - (ans - Math.ceil(ans));
                        if (ans == 1) {
                            ans = 0.9999;
                        }
                    }
                    break;
                default:
                    System.out.println("SaveableObject::mutateAttr - Unrecognised wrap type.");
                    ans = 0;
            }
        }
        return ans;
    }

    public double mutateNumber_java_lang_Integer(double value, int mutationType, int wrapType, double mutationRate, double min, double max) {
        return Math.random();
    }

    public double mutateNumber_java_lang_Boolean(double value, int mutationType, int wrapType, double mutationRate, double min, double max) {
        return Math.random();
    }

    public void randomise() {
        boolean safeValue;
        String string;
        String value;
        String newValue;
        int val;
        double newVal;
        double oldVal;
        for (int i = 0; i < genotype.size(); i++) {
            string = (String) genotype.get(i);
            value = getAttribute("fixed", string);
            if (value.length() > 0) {
                try {
                    val = Integer.valueOf(value).intValue();
                } catch (NumberFormatException e) {
                    val = -1;
                }
                if (val == RANDOM_AND_EVOLVABLE) {
                    while (true) {
                        i++;
                        string = (String) genotype.get(i);
                        if (string.indexOf("</attr>") != -1) {
                            break;
                        } else {
                            safeValue = true;
                            try {
                                oldVal = Double.valueOf(string).doubleValue();
                            } catch (NumberFormatException e) {
                                safeValue = false;
                            }
                            if (safeValue == true) {
                                newVal = Math.random();
                                newValue = Double.toString(newVal);
                                genotype.set(i, newValue);
                            }
                        }
                    }
                }
            }
        }
    }

    public String getWhatTheTagWillBe(SaveableObject object) {
        String gencls = getGenericClassName(object).toLowerCase();
        int dotI = gencls.lastIndexOf('.');
        if (dotI > -1 && dotI < gencls.length() - 1) {
            gencls = gencls.substring(dotI + 1);
        }
        return gencls;
    }

    public String getGenericClassName(SaveableObject object) {
        Class physicalobjectClass = PhysicalObject.class;
        Class visualobjectClass = VisualObject.class;
        Class saveableobjectClass = SaveableObject.class;
        Class sub = object.getClass();
        if (sub.equals(physicalobjectClass) || sub.equals(visualobjectClass) || sub.equals(saveableobjectClass)) {
            return sub.getName();
        } else {
            Class sup = object.getClass().getSuperclass();
            while (!(sup.equals(physicalobjectClass) || sup.equals(visualobjectClass) || sup.equals(saveableobjectClass))) {
                sub = sub.getSuperclass();
                sup = sup.getSuperclass();
            }
            return sub.getName();
        }
    }

    public void writeln(int i, int indent) {
        writeln(new Integer(i).toString(), indent);
    }

    public void writeln(double d, int indent) {
        writeln(new Double(d).toString(), indent);
    }

    public void writeln(String str, int indent) {
        if (gtpOut == null) {
            genotype.add(gtpPos, str);
        } else {
            try {
                gtpOut.write(str);
                gtpOut.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        incGtpPos();
    }

    private void incGtpPos() {
        gtpPos++;
    }

    public double readNumericLine() {
        return Double.parseDouble(readLine());
    }

    public String readLine() {
        incGtpPos();
        String ans = "";
        if (gtpIn == null) {
            if (gtpPos >= 0 && gtpPos < genotype.size()) {
                ans = (String) (genotype.get(gtpPos));
            } else {
                ans = null;
            }
        } else {
            try {
                ans = gtpIn.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ans;
    }

    public static String createEndTag(String string) {
        String tag = getTagType(string);
        if (tag == null) {
            return null;
        }
        return "</" + tag.substring(1, tag.length()) + ">";
    }

    public static void moveAllToLine(Vector genotypes, int indexes[], String tag) {
        if (genotypes == null) {
            return;
        }
        for (int i = 0; i < genotypes.size(); i++) {
            indexes[i] = getNextLineWithTag((Vector) genotypes.get(i), indexes[i], tag);
        }
    }

    public static int getNextLineWithTag(Vector genotype, int index, String tag) {
        String string;
        int currentIndex = index;
        if (genotype == null || tag == null) {
            System.out.println("SaveableObject::getNextLineWithTag - genotype or tag null");
            return currentIndex;
        }
        while (currentIndex < genotype.size()) {
            string = ((String) genotype.get(currentIndex));
            if (string.startsWith(tag)) {
                return currentIndex;
            }
            currentIndex++;
        }
        System.out.println("SaveableObject::getNextLineWithTag - cannot find the tag anywhere on the genotype past point " + index);
        return currentIndex;
    }

    public static String getTagType(String string) {
        int index, index2;
        index = string.indexOf('<');
        if (index == -1) {
            return null;
        }
        index2 = string.indexOf(' ', index);
        if (index2 == -1) {
            index2 = string.indexOf('>', index);
            if (index2 == -1) {
                index2 = string.length();
            }
        }
        return string.substring(index, index2);
    }

    public Vector getValidEvolvableVariables() {
        SaveableObject baseClass = new VisualObject();
        Field fields[] = getClass().getFields();
        Field field;
        int arraySize = java.lang.reflect.Array.getLength(fields);
        Vector variables = new Vector();
        for (int i = 0; i < arraySize; i++) {
            String variable = fields[i].getName();
            Class cls = fields[i].getType();
            Class nodeCls = Node.class;
            Class vectCls = Vector.class;
            if (!(cls.isPrimitive() || cls.equals(nodeCls) || cls.equals(vectCls))) {
                try {
                    field = baseClass.getClass().getField(variable);
                } catch (NoSuchFieldException e) {
                    variables.add(variable);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
        return variables;
    }

    public Field getField(String variable) {
        Field field;
        try {
            field = getClass().getField(variable);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
        return field;
    }

    public Object getValue(String variable) {
        Field field = getField(variable);
        Object value = null;
        if (field == null) {
            System.out.println("SaveableObject::getValue - variable " + variable + " did not get a valid field");
        } else {
            try {
                value = field.get(this);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public Class getFieldType(String variable) {
        Field field = getField(variable);
        Class value = null;
        if (field == null) {
            System.out.println("SaveableObject::getValue - variable " + variable + " did not get a valid field");
        } else {
            try {
                value = field.getType();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public int getEvolvability(String variable) {
        String variable_FIXED = variable + "_FIXED";
        Field field = getField(variable_FIXED);
        Class variableClass = field.getType();
        Object value = null;
        try {
            value = field.get(this);
        } catch (IllegalArgumentException e) {
            return -1;
        } catch (IllegalAccessException e) {
            return -1;
        }
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        return -1;
    }

    public void setValue(String variable, Object value) {
        try {
            getField(variable).set(this, value);
        } catch (IllegalArgumentException e) {
            System.out.println("SaveableObject::setValue - failed to set the value of an object");
        } catch (IllegalAccessException e) {
            System.out.println("SaveableObject::setValue - failed to set the value of an object");
        }
    }

    public void setEvolvability(String variable, int evolvability) {
        setFixedState(variable, this, evolvability);
    }

    public World getWorld() {
        if (getParent() != null) {
            return getParent().getWorld();
        }
        return null;
    }

    public String name;

    public static final double name_MIN = 0;

    public static final double name_MAX = 1;

    public int name_FIXED = SET_AND_FIXED;

    private SaveableObject parent;

    private Vector children;

    private String returnedContent;

    private Vector ids;

    private Vector genotype;

    private int gtpPos;

    private BufferedReader gtpIn;

    private BufferedWriter gtpOut;

    public static final int SET_AND_FIXED = 0;

    public static final int SET_AND_EVOLVABLE = 1;

    public static final int RANDOM_AND_EVOLVABLE = 2;

    public static final int UNIFORM = 0;

    public static final int GAUSSIAN = 1;

    public static final int STICK = 0;

    public static final int BOUNCE = 1;

    public static final int WRAP = 2;

    public static final int RANK_SELECTION = 0;

    public static final int TOURNAMENT_SELECTION = 1;

    public static final int RANDOM_SELECTION = 2;
}
