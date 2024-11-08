package ircam.jmax.editors.table;

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
public class FtsTableObject extends FtsUndoableObject implements TableDataModel {

    static {
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("size"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).setSize(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("setVisibles"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).setVisibles(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("appendVisibles"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).appendVisibles(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("addVisibles"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).addVisibles(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("startEdit"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).startEdit();
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("endEdit"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).endEdit();
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("setPixels"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).setPixels(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("appendPixels"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).appendPixels(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("addPixels"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).addPixels(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("resetEditor"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).resetEditor();
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("range"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).setRange(args.getFloat(0), args.getFloat(1));
            }
        });
        FtsObject.registerMessageHandler(FtsTableObject.class, FtsSymbol.get("reference"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTableObject) obj).setReference(args.getLength(), args.getAtoms());
            }
        });
    }

    /**
* constructor.
 */
    public FtsTableObject(FtsServer server, FtsObject parent, int id) {
        super(server, parent, id);
        vector = parent;
        listeners = new MaxVector();
        type = (parent instanceof FtsIvecObject);
    }

    public boolean isIvec() {
        return type;
    }

    public void setSize(int newSize) {
        if (size != newSize) {
            int oldSize = size;
            size = newSize;
            notifySizeChanged(size, oldSize);
        }
    }

    public void resetEditor() {
        firstTime = false;
        firstVisIndex = 0;
        lastVisIndex = 0;
    }

    void extendVisiblesVector(int newSize) {
        if (newSize > visibleSize) {
            double[] temp = new double[newSize + 10];
            int start = 0;
            if (visibles != null) {
                for (int i = 0; i < visibleSize; i++) temp[i] = visibles[i];
                start = visibleSize;
            }
            for (int i = start; i < temp.length; i++) temp[i] = 0.0;
            visibleSize = newSize;
            visibles = temp;
        }
    }

    public void setVisibles(int nArgs, FtsAtom args[]) {
        int i = 0;
        int oldSize = size;
        size = args[0].intValue;
        visibleSize = args[1].intValue;
        firstVisIndex = args[2].intValue;
        visibles = new double[visibleSize + 10];
        if (isIvec()) {
            if (isInGroup()) for (i = 0; i < nArgs - 3; i++) {
                postEdit(new UndoableValueSet(this, i, visibles[i]));
                visibles[i] = (double) args[i + 3].intValue;
            } else for (i = 0; i < nArgs - 3; i++) visibles[i] = (double) args[i + 3].intValue;
        } else {
            if (isInGroup()) for (i = 0; i < nArgs - 3 && i < visibles.length; i++) {
                postEdit(new UndoableValueSet(this, i, visibles[i]));
                visibles[i] = args[i + 3].doubleValue;
            } else for (i = 0; i < nArgs - 3 && i < visibles.length; i++) visibles[i] = args[i + 3].doubleValue;
        }
        if (size != oldSize) notifySizeChanged(size, oldSize);
        notifySet();
    }

    public void appendVisibles(int nArgs, FtsAtom args[]) {
        int startIndex = args[0].intValue;
        int i = 0;
        if (isIvec()) {
            if (isInGroup()) for (i = 0; ((i < nArgs - 1) && (startIndex + i < size)); i++) {
                postEdit(new UndoableValueSet(this, startIndex + i, visibles[startIndex + i - firstVisIndex]));
                visibles[startIndex + i - firstVisIndex] = (double) args[i + 1].intValue;
            } else for (i = 0; ((i < nArgs - 1) && (startIndex + i < size)); i++) visibles[startIndex + i - firstVisIndex] = (double) args[i + 1].intValue;
        } else {
            if (startIndex - firstVisIndex < 0) System.err.println("appendVisibles: startIndex " + visibleSize + " firstVisIndex " + firstVisIndex);
            if (isInGroup()) for (i = 0; (i < nArgs - 1) && (startIndex + i < size); i++) {
                postEdit(new UndoableValueSet(this, startIndex + i, visibles[startIndex + i - firstVisIndex]));
                visibles[startIndex + i - firstVisIndex] = args[i + 1].doubleValue;
            } else for (i = 0; (i < nArgs - 1) && (startIndex + i < size); i++) visibles[startIndex + i - firstVisIndex] = args[i + 1].doubleValue;
        }
        notifyValueChanged(startIndex, startIndex + i - 1, fromScroll);
    }

    public void addVisibles(int nArgs, FtsAtom args[]) {
        int startIndex = args[0].intValue;
        int direction = args[1].intValue;
        int i = 0;
        int newp = nArgs - 2;
        double[] vis_temp = new double[visibles.length];
        if (direction == 1) {
            if (isIvec()) for (i = 0; i < newp; i++) vis_temp[i] = (double) args[i + 2].intValue; else for (i = 0; i < newp; i++) vis_temp[i] = args[i + 2].doubleValue;
            for (i = 0; newp + i < vis_temp.length; i++) vis_temp[newp + i] = visibles[i];
        } else {
            for (i = 0; (i < visibleSize - newp); i++) vis_temp[i] = visibles[i + newp];
            if (visibleSize - newp < 0) System.err.println("visibleSize " + visibleSize + " newp " + newp);
            if (isIvec()) for (i = 0; i < nArgs - 2; i++) vis_temp[visibleSize - newp + i] = (double) args[i + 2].intValue; else for (i = 0; i < nArgs - 2; i++) vis_temp[visibleSize - newp + i] = args[i + 2].doubleValue;
        }
        visibles = vis_temp;
        notifySet();
    }

    public void startEdit() {
        beginUpdate();
    }

    public void endEdit() {
        endUpdate();
    }

    void printVisibles() {
        System.err.println("printvisibles ");
        for (int i = 0; i < visibleSize - 9; i += 10) {
            System.err.println(" " + visibles[i] + " " + visibles[i + 1] + " " + visibles[i + 2] + " " + visibles[i + 3] + " " + visibles[i + 4] + " " + visibles[i + 5] + " " + visibles[i + 6] + " " + visibles[i + 7] + " " + visibles[i + 8] + " " + visibles[i + 9]);
        }
    }

    public void setPixels(int nArgs, FtsAtom args[]) {
        int i = 0;
        int j = 0;
        pixelsSize = args[0].intValue;
        int oldSize = size;
        size = args[1].intValue;
        t_pixels = new double[pixelsSize + 10];
        b_pixels = new double[pixelsSize + 10];
        boolean notify = false;
        if (isIvec()) for (i = 0; i < nArgs - 3; i += 2) {
            t_pixels[j] = (double) args[i + 2].intValue;
            b_pixels[j] = (double) args[i + 3].intValue;
            j++;
        } else for (i = 0; i < nArgs - 3; i += 2) {
            t_pixels[j] = args[i + 2].doubleValue;
            b_pixels[j] = args[i + 3].doubleValue;
            j++;
        }
        if (oldSize != size) notifyTableUpdated(); else if (pixelsSize <= nArgs - 2) notifySet();
    }

    public void appendPixels(int nArgs, FtsAtom args[]) {
        int startIndex = args[0].intValue;
        size = args[1].intValue;
        int i = 0;
        int j = 0;
        if (isIvec()) for (i = 0; (i < nArgs - 3) && (startIndex + j < t_pixels.length); i += 2) {
            t_pixels[startIndex + j] = (double) args[i + 2].intValue;
            b_pixels[startIndex + j] = (double) args[i + 3].intValue;
            j++;
        } else for (i = 0; (i < nArgs - 3) && (startIndex + j < t_pixels.length); i += 2) {
            t_pixels[startIndex + j] = args[i + 2].doubleValue;
            b_pixels[startIndex + j] = args[i + 3].doubleValue;
            j++;
        }
        notifyPixelsChanged(startIndex, startIndex + j - 2);
    }

    public void addPixels(int nArgs, FtsAtom args[]) {
        int startIndex = args[0].intValue;
        int i = 0;
        int j = 0;
        int newp = (int) (nArgs - 1) / 2;
        double[] t_temp = new double[pixelsSize + newp];
        double[] b_temp = new double[pixelsSize + newp];
        if (startIndex == 0) {
            if (isIvec()) {
                for (i = 0; i < (nArgs - 1); i += 2) {
                    t_temp[j] = (double) args[i + 1].intValue;
                    b_temp[j] = (double) args[i + 2].intValue;
                    j++;
                }
            } else {
                for (i = 0; i < (nArgs - 1); i += 2) {
                    t_temp[j] = args[i + 1].doubleValue;
                    b_temp[j] = args[i + 2].doubleValue;
                    j++;
                }
            }
            for (i = newp; i < pixelsSize; i++) {
                t_temp[i] = t_pixels[i - newp];
                b_temp[i] = b_pixels[i - newp];
            }
        } else {
            for (i = 0; (i < pixelsSize - newp) && (i + newp < t_temp.length); i++) {
                t_temp[i] = t_pixels[i + newp];
                b_temp[i] = b_pixels[i + newp];
            }
            j = 1;
            if (isIvec()) for (i = 1; i <= (nArgs - 1); i += 2) {
                t_temp[pixelsSize - newp - 1 + j] = (double) args[i].intValue;
                b_temp[pixelsSize - newp - 1 + j] = (double) args[i + 1].intValue;
                j++;
            } else for (i = 1; (i <= (nArgs - 2)) && (pixelsSize - newp - 1 + j < t_temp.length); i += 2) {
                t_temp[pixelsSize - newp - 1 + j] = args[i].doubleValue;
                b_temp[pixelsSize - newp - 1 + j] = args[i + 1].doubleValue;
                j++;
            }
        }
        t_pixels = t_temp;
        b_pixels = b_temp;
        notifySet();
    }

    public void setRange(float min_val, float max_val) {
        this.min_val = min_val;
        this.max_val = max_val;
        notifyRange(min_val, max_val);
    }

    int nRowsRef, nColsRef, indexRef, onsetRef, sizeRef;

    String typeRef;

    boolean hasReference = false;

    public void setReference(int nArgs, FtsAtom args[]) {
        nRowsRef = args[0].intValue;
        nColsRef = args[1].intValue;
        typeRef = args[2].symbolValue.toString();
        indexRef = args[3].intValue;
        onsetRef = args[4].intValue;
        sizeRef = args[5].intValue;
        hasReference = true;
        notifyReference(nRowsRef, nColsRef, typeRef, indexRef, onsetRef, sizeRef);
    }

    public boolean hasReference() {
        return hasReference;
    }

    public void requestSetValue(int index, double value) {
        args.clear();
        args.addInt(index);
        if (isIvec()) args.addInt((int) value); else args.addDouble(value);
        try {
            send(FtsSymbol.get("set_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending set_from_client Message!");
            e.printStackTrace();
        }
    }

    public void requestSetValues(double[] values, int startIndex, int size) {
        args.clear();
        args.addInt(startIndex);
        if (isIvec()) {
            for (int i = 0; i < size; i++) args.addInt((int) values[i]);
        } else for (int i = 0; i < size; i++) args.addDouble(values[i]);
        try {
            send(FtsSymbol.get("set_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending set_from_client Message!");
            e.printStackTrace();
        }
    }

    int firstVisIndex = 0;

    int lastVisIndex = 0;

    public void requestSetVisibleWindow(int vsize, int startIndex, int windowSize, double zoom, int sizePixels) {
        this.firstVisIndex = startIndex;
        if (vsize > visibleSize) extendVisiblesVector(vsize);
        visibleSize = vsize;
        lastVisIndex = firstVisIndex + visibleSize;
        if (lastVisIndex > size) lastVisIndex = size;
        args.clear();
        args.addInt(vsize + 10);
        args.addInt(startIndex);
        args.addInt(windowSize);
        args.addDouble(zoom);
        args.addInt(sizePixels);
        try {
            send(FtsSymbol.get("set_visible_window"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending set_visible_window Message!");
            e.printStackTrace();
        }
    }

    public void requestEndEdit() {
        try {
            send(FtsSymbol.get("end_edit"));
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending end_edit Message!");
            e.printStackTrace();
        }
    }

    private boolean firstTime = false;

    public void requestGetValues() {
        try {
            send(FtsSymbol.get("get_from_client"));
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending get_from_client Message!");
            e.printStackTrace();
        }
        firstTime = true;
    }

    private boolean fromScroll = false;

    public void requestGetValues(int first, int last, boolean fromScroll) {
        this.fromScroll = fromScroll;
        if (!firstTime) requestGetValues(); else {
            args.clear();
            args.addInt(first);
            args.addInt(last);
            System.err.println("requestGetValues first " + first + " last " + last);
            try {
                send(FtsSymbol.get("get_from_client"), args);
            } catch (IOException e) {
                System.err.println("FtsTableObject: I/O Error sending get_from_client Message!");
                e.printStackTrace();
            }
        }
    }

    public void requestGetPixels(int deltax, int deltap) {
        if (deltax == 0) try {
            send(FtsSymbol.get("get_pixels_from_client"));
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending get_pixels_from_client Message!");
            e.printStackTrace();
        } else {
            args.clear();
            args.addInt(deltax);
            args.addInt(deltap);
            try {
                send(FtsSymbol.get("get_pixels_from_client"), args);
            } catch (IOException e) {
                System.err.println("FtsTableObject: I/O Error sending get_pixels_from_client Message!");
                e.printStackTrace();
            }
        }
        firstTime = false;
    }

    boolean thereIsAcopy = false;

    public boolean thereIsACopy() {
        return thereIsAcopy;
    }

    public void requestCopy(int startIndex, int size) {
        args.clear();
        args.addInt(startIndex);
        args.addInt(size);
        try {
            send(FtsSymbol.get("copy_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending copy_from_client Message!");
            e.printStackTrace();
        }
        thereIsAcopy = true;
    }

    public void requestCut(int startIndex, int size, int vsize, int pixsize) {
        args.clear();
        args.addInt(vsize);
        args.addInt(pixsize);
        args.addInt(startIndex);
        args.addInt(size);
        try {
            send(FtsSymbol.get("cut_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending cut_from_client Message!");
            e.printStackTrace();
        }
        thereIsAcopy = true;
        clearAllUndoRedo();
    }

    public void requestPaste(int startIndex, int size) {
        args.clear();
        args.addInt(startIndex);
        args.addInt(size);
        try {
            send(FtsSymbol.get("paste_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending paste_from_client Message!");
            e.printStackTrace();
        }
        clearAllUndoRedo();
    }

    public void requestInsert(int startIndex, int vsize, int pixsize) {
        args.clear();
        args.addInt(vsize);
        args.addInt(pixsize);
        args.addInt(startIndex);
        try {
            send(FtsSymbol.get("insert_from_client"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending insert_from_client Message!");
            e.printStackTrace();
        }
        clearAllUndoRedo();
    }

    public void requestSetRange(float min_value, float max_value) {
        args.clear();
        args.addFloat(min_value);
        args.addFloat(max_value);
        try {
            send(FtsSymbol.get("change_range"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending range Message!");
            e.printStackTrace();
        }
    }

    public void requestChangeReference(String type_ref, int idx_ref, int onset_ref, int size_ref) {
        args.clear();
        args.addSymbol(FtsSymbol.get(type_ref));
        args.addInt(idx_ref);
        args.addInt(onset_ref);
        args.addInt(size_ref);
        try {
            send(FtsSymbol.get("reference"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending reference Message!");
            e.printStackTrace();
        }
    }

    private double[] visibles;

    private int visibleSize = 0;

    private double[] t_pixels;

    private double[] b_pixels;

    private int pixelsSize = 0;

    public int getVisibleSize() {
        return visibleSize;
    }

    public double getVisibleValue(int index) {
        return visibles[index - firstVisIndex];
    }

    public int getPixelsSize() {
        return pixelsSize;
    }

    public double getTopPixel(int index) {
        if (index >= pixelsSize) return 0; else return t_pixels[index];
    }

    public double getBottomPixel(int index) {
        if (index >= pixelsSize) return 0; else return b_pixels[index];
    }

    public int getSize() {
        return size;
    }

    public int getFirstVisibleIndex() {
        return firstVisIndex;
    }

    public int getLastVisibleIndex() {
        return lastVisIndex;
    }

    private int[] values;

    public int getVerticalSize() {
        return Math.abs(max() - min());
    }

    public int max() {
        if (values == null) return 0;
        int max = values[0];
        for (int i = 0; i < getSize(); i++) {
            if (values[i] > max) max = values[i];
        }
        return max;
    }

    public int min() {
        if (values == null) return 0;
        int min = values[0];
        for (int i = 0; i < getSize(); i++) {
            if (values[i] < min) min = values[i];
        }
        return min;
    }

    public int[] getValues() {
        return values;
    }

    public int getValue(int index) {
        return values[index];
    }

    public void interpolate(int start, int end, double startValue, double endValue) {
        if (end >= getSize()) end = getSize() - 1;
        args.clear();
        args.addInt(start);
        args.addInt(end);
        if (isIvec()) {
            args.addInt((int) startValue);
            args.addInt((int) endValue);
        } else {
            args.addDouble(startValue);
            args.addDouble(endValue);
        }
        try {
            send(FtsSymbol.get("interpolate"), args);
        } catch (IOException e) {
            System.err.println("FtsTableObject: I/O Error sending interpolate Message!");
            e.printStackTrace();
        }
    }

    double cutToBounds(double y, double max, double min) {
        if (y > max) return max; else if (y < min) return min;
        return y;
    }

    public String getType() {
        return (isIvec()) ? "ivec" : "fvec";
    }

    public String getName() {
        return ((FtsGraphicObject) vector).getVariableName();
    }

    public void nameChanged(String name) {
        super.nameChanged(name);
        notifyNameChanged(name);
    }

    /**
* Utility private function to allocate a buffer used during the interpolate operations.
 * The computation is done in a private vector that is stored in one shot. */
    static double buffer[];

    private static void prepareBuffer(int lenght) {
        if (buffer == null || buffer.length < lenght) buffer = new double[lenght];
    }

    /**
* Require to be notified when database change
 */
    public void addListener(TableDataListener theListener) {
        listeners.addElement(theListener);
    }

    /**
* Remove the listener
 */
    public void removeListener(TableDataListener theListener) {
        listeners.removeElement(theListener);
    }

    /**
* utility to notify the data base change to all the listeners
 */
    private void notifySizeChanged(int size, int oldSize) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).sizeChanged(size, oldSize);
    }

    private void notifySet() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableSetted();
    }

    private void notifyValueChanged(int start, int end, boolean fromScroll) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).valueChanged(start, end, fromScroll);
    }

    private void notifyPixelsChanged(int start, int end) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).pixelsChanged(start, end);
    }

    private void notifyClear() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableCleared();
    }

    private void notifyTableUpdated() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableUpdated();
    }

    private void notifyRange(float min_val, float max_val) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableRange(min_val, max_val);
    }

    private void notifyReference(int nRowsRef, int nColsRef, String typeRef, int indexRef, int onsetRef, int sizeRef) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableReference(nRowsRef, nColsRef, typeRef, indexRef, onsetRef, sizeRef);
    }

    private void notifyNameChanged(String name) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TableDataListener) e.nextElement()).tableNameChanged(name);
    }

    private Vector points = new Vector();

    MaxVector listeners = new MaxVector();

    private int size = 0;

    private FtsObject vector;

    private boolean type;

    float min_val = -1;

    float max_val = 1;
}
