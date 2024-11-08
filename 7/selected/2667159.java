package genestudio.GML;

import java.util.Vector;

/**
 *
 * @author User
 */
public class GMLCache {

    private Vector<GML> vctGMLs;

    private int intPointer = 0;

    public static int CAPACITY = 6;

    private int[] cursors = new int[CAPACITY];

    public GMLCache(GML init, int cursor) {
        vctGMLs = new Vector<GML>();
        vctGMLs.add(init.clone());
        cursors[0] = cursor;
        for (int i = 1; i < cursors.length; i++) {
            cursors[i] = -1;
        }
    }

    public GML getUndo() {
        if (intPointer > 0) {
            return vctGMLs.elementAt(--intPointer).clone();
        } else return null;
    }

    public GML getRedo() {
        intPointer++;
        if (vctGMLs.size() <= intPointer) {
            intPointer--;
            return null;
        } else return vctGMLs.elementAt(intPointer).clone();
    }

    public boolean hasNext() {
        if (vctGMLs.size() <= (intPointer + 1)) return false; else return true;
    }

    public boolean hasPreviouse() {
        if (intPointer > 0) return true; else return false;
    }

    public int getCurrentCursor() {
        return cursors[intPointer];
    }

    public void setCurrentCursor(int cursor) {
        cursors[intPointer] = cursor;
    }

    @Deprecated
    public GML getGML(int order) {
        return vctGMLs.elementAt(order);
    }

    public void put(GML gmlNew, int cursor) {
        if (intPointer < CAPACITY - 1) {
            intPointer++;
            vctGMLs.add(intPointer, gmlNew.clone());
            cursors[intPointer] = cursor;
            if ((vctGMLs.lastElement() != null) && (!vctGMLs.lastElement().equals(gmlNew))) {
                while (vctGMLs.size() > intPointer + 1) vctGMLs.removeElementAt(intPointer + 1);
                for (int i = intPointer + 1; i < cursors.length; i++) cursors[i] = -1;
            }
        } else {
            vctGMLs.remove(0);
            for (int i = 0; i < cursors.length - 1; i++) {
                cursors[i] = cursors[i + 1];
            }
            vctGMLs.add(gmlNew.clone());
            cursors[intPointer] = cursor;
        }
    }
}
