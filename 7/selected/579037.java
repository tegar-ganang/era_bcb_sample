package ircam.jmax.editors.bpf;

import ircam.fts.client.*;
import ircam.jmax.*;
import ircam.jmax.fts.*;
import ircam.jmax.toolkit.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
* A concrete implementation of the SequenceDataModel,
 * this class represents a model of a set of tracks.
 */
public class FtsBpfObject extends FtsObjectWithEditor implements BpfDataModel {

    static {
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("addPoint"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).addPoint(args.getInt(0), (float) args.getDouble(1), (float) args.getDouble(2));
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("removePoints"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).removePoints(args.getInt(0), args.getInt(1));
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("setPoint"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).setPoint(args.getInt(0), (float) args.getDouble(1), (float) args.getDouble(2));
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("setPoints"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).setPoints(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("clear"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).clear();
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("set"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).set(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsBpfObject.class, FtsSymbol.get("append"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsBpfObject) obj).append(args.getLength(), args.getAtoms());
            }
        });
    }

    /**
* constructor.
 */
    public FtsBpfObject(FtsServer server, FtsObject parent, int objId, String classname, FtsAtom args[], int offset, int length) {
        super(server, parent, objId, classname, args, offset, length);
        listeners = new MaxVector();
    }

    public void createEditor() {
        if (getEditorFrame() == null) setEditorFrame(new BpfWindow(this));
    }

    public void reinitEditorFrame() {
        if (getEditorFrame() != null) setEditorFrame(new BpfWindow((BpfWindow) getEditorFrame()));
    }

    public void destroyEditor() {
        disposeEditor();
        listeners.removeAllElements();
        System.gc();
    }

    public void addPoint(int index, float time, float value) {
        addPoint(index, new BpfPoint(time, value));
        notifyPointAdded(index);
    }

    public void removePoints(int index, int size) {
        for (int i = size - 1; i >= 0; i--) removePoint(index + i);
        notifyPointsDeleted(index, size);
    }

    public void setPoint(int oldIndex, float newTime, float newValue) {
        int newIndex = oldIndex;
        BpfPoint point = getPointAt(oldIndex);
        point.setValue(newValue);
        point.setTime(newTime);
        notifyPointChanged(oldIndex, newIndex, newTime, newValue);
    }

    public void setPoints(int nArgs, FtsAtom args[]) {
        BpfPoint point;
        int firstIndex = args[0].intValue;
        int n = (nArgs - 1) / 2;
        for (int i = 0; i < n; i++) {
            float time = (float) args[i * 2 + 1].doubleValue;
            float value = (float) args[i * 2 + 2].doubleValue;
            point = getPointAt(firstIndex + i);
            point.setValue(value);
            point.setTime(time);
        }
        notifyPointsChanged();
    }

    public void clear() {
        removeAllPoints();
    }

    public void set(int nArgs, FtsAtom args[]) {
        removeAllPoints();
        if (nArgs > 0) {
            int j = 0;
            for (int i = 0; i < nArgs; i += 2) addPoint(j++, new BpfPoint((float) args[i].doubleValue, (float) args[i + 1].doubleValue));
            notifyPointAdded(j - 1);
        }
    }

    public void append(int nArgs, FtsAtom args[]) {
        int j = length();
        if (nArgs > 1) {
            for (int i = 0; i < nArgs; i += 2) addPoint(j++, new BpfPoint((float) args[i].doubleValue, (float) args[i + 1].doubleValue));
            notifyPointAdded(j - 1);
        }
    }

    public void nameChanged(String name) {
        super.nameChanged(name);
        notifyNameChanged(name);
    }

    public String getName() {
        return super.getVariableName();
    }

    public void requestPointCreation(int index, float time, float value) {
        args.clear();
        args.addInt(index);
        args.addDouble((double) time);
        args.addDouble((double) value);
        try {
            send(FtsSymbol.get("add_point"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending add_point Message!");
            e.printStackTrace();
        }
    }

    public void requestSetPoint(int index, float time, float value) {
        args.clear();
        args.addInt(index);
        args.addDouble((double) time);
        args.addDouble((double) value);
        try {
            send(FtsSymbol.get("set_points"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending set_point Message!");
            e.printStackTrace();
        }
    }

    public void requestSetPoints(int index, float[] times, float[] values) {
        args.clear();
        args.addInt(index);
        for (int i = 0; i < times.length; i++) {
            args.addDouble((double) times[i]);
            args.addDouble((double) values[i]);
        }
        try {
            send(FtsSymbol.get("set_points"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending set_points Message!");
            e.printStackTrace();
        }
    }

    public void requestPointRemove(int index) {
        args.clear();
        args.addInt(index);
        try {
            send(FtsSymbol.get("remove_points"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending remove_points Message!");
            e.printStackTrace();
        }
    }

    public void requestPointsRemove(int start, int size) {
        args.clear();
        args.addInt(start);
        args.addInt(size);
        try {
            send(FtsSymbol.get("remove_points"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending remove_points Message!");
            e.printStackTrace();
        }
    }

    public void requestPointsRemove(Enumeration en, int size) {
        int[] ids = new int[size];
        int i = 0;
        for (Enumeration e = en; en.hasMoreElements(); ) ids[i++] = indexOf((BpfPoint) e.nextElement());
        bubbleSort(ids);
        for (int j = 0; j < size; j++) args.addInt(ids[j++]);
        try {
            send(FtsSymbol.get("remove_points"), args);
        } catch (IOException e) {
            System.err.println("FtsBpfObject: I/O Error sending remove_points Message!");
            e.printStackTrace();
        }
    }

    void bubbleSort(int ids[]) {
        boolean flag = true;
        int temp;
        while (flag) {
            flag = false;
            for (int i = 0; i < ids.length - 1; i++) if (ids[i] < ids[i + 1]) {
                temp = ids[i];
                ids[i] = ids[i + 1];
                ids[i + 1] = temp;
                flag = true;
            }
        }
    }

    public void addPoint(int index, BpfPoint pt) {
        points.insertElementAt(pt, index);
    }

    public void removePoint(int index) {
        points.removeElementAt(index);
    }

    public BpfPoint getPointAt(int index) {
        if (index < points.size() && (index >= 0)) return (BpfPoint) points.elementAt(index); else return null;
    }

    public void removeAllPoints() {
        points.removeAllElements();
    }

    public Enumeration getPoints() {
        return points.elements();
    }

    public BpfPoint getLastPoint() {
        if (points.size() != 0) return (BpfPoint) points.lastElement(); else return null;
    }

    public BpfPoint getPreviousPoint(BpfPoint pnt) {
        int id = indexOf(pnt);
        if ((id != -1) && (id < length())) return getPointAt(id - 1); else return null;
    }

    public BpfPoint getPreviousPoint(float time) {
        int i = getPreviousPointIndex(time);
        if ((i < 0) || (i >= points.size())) return null; else return getPointAt(i);
    }

    public int getPreviousPointIndex(float time) {
        int i = -1;
        for (Enumeration e = points.elements(); e.hasMoreElements(); ) {
            BpfPoint point = (BpfPoint) e.nextElement();
            if (point.getTime() >= time) return i;
            i++;
        }
        return i;
    }

    public BpfPoint getNextPoint(float time) {
        if (length() == 0) return null;
        int i = 0;
        for (Enumeration e = points.elements(); e.hasMoreElements(); ) {
            if (((BpfPoint) e.nextElement()).getTime() > time) return getPointAt(i);
            i++;
        }
        return null;
    }

    public BpfPoint getNextPoint(BpfPoint pnt) {
        int id = indexOf(pnt);
        if ((id != -1) && (id < length())) return getPointAt(id + 1); else return null;
    }

    public int getNextPointIndex(float time) {
        int i = 0;
        for (Enumeration e = points.elements(); e.hasMoreElements(); ) {
            BpfPoint point = (BpfPoint) e.nextElement();
            if (point.getTime() > time) return i;
            i++;
        }
        return i;
    }

    public int indexOf(BpfPoint pnt) {
        int i = 0;
        for (Enumeration e = points.elements(); e.hasMoreElements(); ) {
            if ((BpfPoint) e.nextElement() == pnt) return i;
            i++;
        }
        return -1;
    }

    public int length() {
        return points.size();
    }

    public int movePointTo(int oldIndex, float newTime) {
        BpfPoint point = getPointAt(oldIndex);
        point.setTime(newTime);
        points.removeElementAt(oldIndex);
        int newIndex = getPreviousPointIndex(newTime) + 1;
        points.insertElementAt(point, newIndex);
        return newIndex;
    }

    /**
* Require to be notified when database change
 */
    public void addBpfListener(BpfDataListener theListener) {
        listeners.addElement(theListener);
    }

    /**
* Remove the listener
 */
    public void removeBpfListener(BpfDataListener theListener) {
        listeners.removeElement(theListener);
    }

    /**
* utility to notify the data base change to all the listeners
 */
    private void notifyPointAdded(int index) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).pointAdded(index);
    }

    private void notifyPointsDeleted(int index, int size) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).pointsDeleted(index, size);
    }

    private void notifyPointChanged(int oldIndex, int newIndex, float newTime, float newValue) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).pointChanged(oldIndex, newIndex, newTime, newValue);
    }

    private void notifyPointsChanged() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).pointsChanged();
    }

    private void notifyClear() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).cleared();
    }

    private void notifyNameChanged(String name) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((BpfDataListener) e.nextElement()).nameChanged(name);
    }

    public Enumeration intersectionSearch(float start, float end, BpfAdapter adapter) {
        return new Intersection(start, end, adapter);
    }

    /**
* an utility class to implement the intersection with a range */
    class Intersection implements Enumeration {

        Intersection(float start, float end, BpfAdapter ad) {
            endTime = end;
            startTime = start;
            adapter = ad;
            index = 0;
        }

        public boolean hasMoreElements() {
            nextObject = findNext();
            return nextObject != null;
        }

        public Object nextElement() {
            return nextObject;
        }

        private Object findNext() {
            if (length() == 0) return null;
            BpfPoint p;
            while (index < length() && ((BpfPoint) points.elementAt(index)).getTime() <= endTime) {
                p = (BpfPoint) points.elementAt(index++);
                if (p.getTime() >= startTime || p.getTime() + adapter.getInvLenght(p) >= startTime) return p;
            }
            return null;
        }

        float endTime;

        float startTime;

        int index;

        Object nextObject = null;

        BpfAdapter adapter;
    }

    float maxValue = 1;

    float minValue = 0;

    public float getRange() {
        return (maxValue - minValue);
    }

    public float getMaximumValue() {
        return maxValue;
    }

    public float getMinimumValue() {
        return minValue;
    }

    public void setMaximumValue(float max) {
        maxValue = max;
    }

    public void setMinimumValue(float min) {
        minValue = min;
    }

    private Vector points = new Vector();

    MaxVector listeners = new MaxVector();

    protected FtsArgs args = new FtsArgs();
}
