package ircam.jmax.editors.explode;

import javax.swing.*;
import javax.swing.event.*;
import ircam.jmax.toolkit.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;

/**
 * A panel that edits the fields of a ScrEvent.
 * It uses the NumericPropertyEditor class.
 * It is used in the Explode statusBar */
class ScrEventWidget extends Box implements ListSelectionListener, ExplodeDataListener, ActionListener {

    /**
   * Constructor. It builds up the single widgets corresponding to
   * the properties of the object to edit. */
    public ScrEventWidget(ExplodeGraphicContext theGc) {
        super(BoxLayout.X_AXIS);
        gc = theGc;
        timeEditor = new NumericalWidget("time", 6, NumericalWidget.EDITABLE_FIELD);
        timeEditor.addActionListener(this);
        add(timeEditor);
        totalWidth += timeEditor.getSize().width;
        pitchEditor = new NumericalWidget("pitch", 3, NumericalWidget.EDITABLE_FIELD);
        pitchEditor.addActionListener(this);
        add(pitchEditor);
        totalWidth += pitchEditor.getSize().width;
        durationEditor = new NumericalWidget("dur.", 6, NumericalWidget.EDITABLE_FIELD);
        durationEditor.addActionListener(this);
        add(durationEditor);
        totalWidth += durationEditor.getSize().width;
        velocityEditor = new NumericalWidget("vel.", 3, NumericalWidget.EDITABLE_FIELD);
        velocityEditor.addActionListener(this);
        add(velocityEditor);
        totalWidth += velocityEditor.getSize().width;
        channelEditor = new NumericalWidget("ch.", 2, NumericalWidget.EDITABLE_FIELD);
        channelEditor.addActionListener(this);
        add(channelEditor);
        totalWidth += channelEditor.getSize().width;
        dim.setSize(totalWidth, HEIGHT);
        setSize(dim.width, dim.height);
        gc.getSelection().addListSelectionListener(this);
        gc.getDataModel().addListener(this);
    }

    /**
   * List selection listener interface */
    public void valueChanged(ListSelectionEvent e) {
        if (gc.getSelection().isSelectionEmpty()) setTarget(null); else setTarget(identifyTarget(e));
    }

    private ScrEvent identifyTarget(ListSelectionEvent e) {
        int count = 0;
        for (int i = gc.getSelection().getMinSelectionIndex(); i <= gc.getSelection().getMaxSelectionIndex(); i++) if (gc.getSelection().isSelectedIndex(i)) count += 1;
        if (count == 1) return (gc.getDataModel().getEventAt(gc.getSelection().getMinSelectionIndex())); else return null;
    }

    /**
   * ExplodeDataListener interface */
    public void objectDeleted(Object whichObject, int oldIndex) {
        if (target == whichObject) setTarget(null);
    }

    public void objectAdded(Object whichObject, int index) {
    }

    public void objectChanged(Object whichObject) {
        if (target == whichObject) refresh();
    }

    public void objectMoved(Object whichObject, int oldIndex, int newIndex) {
        objectChanged(whichObject);
    }

    /** set the target ScrEvent to edit.
   * null means no objects */
    public void setTarget(ScrEvent e) {
        target = e;
        if (e != null) {
            timeEditor.setValue(e.getTime());
            pitchEditor.setValue(e.getPitch());
            durationEditor.setValue(e.getDuration());
            velocityEditor.setValue(e.getVelocity());
            channelEditor.setValue(e.getChannel());
        } else {
            timeEditor.setValue("");
            pitchEditor.setValue("");
            durationEditor.setValue("");
            velocityEditor.setValue("");
            channelEditor.setValue("");
        }
    }

    private void refresh() {
        setTarget(target);
    }

    /** Action listener interface. 
   * This class is a listener for the edit fields of its NumericalWidgets.
   * The information is used here to set the same value for 
   * all the events in a selection*/
    public void actionPerformed(ActionEvent e) {
        if (gc.getSelection().isSelectionEmpty()) return;
        int value;
        ScrEvent temp;
        try {
            value = Integer.parseInt(((JTextField) e.getSource()).getText());
        } catch (Exception ex) {
            return;
        }
        ((UndoableData) gc.getDataModel()).beginUpdate();
        Enumeration en;
        if (e.getSource() == timeEditor.getCustomComponent()) {
            for (en = gc.getSelection().getSelected(); en.hasMoreElements(); ) {
                temp = (ScrEvent) en.nextElement();
                temp.move(value);
            }
        } else if (e.getSource() == pitchEditor.getCustomComponent()) {
            for (en = gc.getSelection().getSelected(); en.hasMoreElements(); ) {
                temp = (ScrEvent) en.nextElement();
                temp.setPitch(value);
            }
        } else if (e.getSource() == durationEditor.getCustomComponent()) {
            for (en = gc.getSelection().getSelected(); en.hasMoreElements(); ) {
                temp = (ScrEvent) en.nextElement();
                temp.setDuration(value);
            }
        } else if (e.getSource() == velocityEditor.getCustomComponent()) {
            for (en = gc.getSelection().getSelected(); en.hasMoreElements(); ) {
                temp = (ScrEvent) en.nextElement();
                temp.setVelocity(value);
            }
        } else if (e.getSource() == channelEditor.getCustomComponent()) {
            for (en = gc.getSelection().getSelected(); en.hasMoreElements(); ) {
                temp = (ScrEvent) en.nextElement();
                temp.setChannel(value);
            }
        }
        ((UndoableData) gc.getDataModel()).endUpdate();
    }

    public Dimension getPreferredSize() {
        return dim;
    }

    public Dimension getMinimumSize() {
        return dim;
    }

    ExplodeGraphicContext gc;

    Dimension dim = new Dimension();

    int totalWidth = 0;

    ScrEvent target;

    NumericalWidget timeEditor;

    NumericalWidget pitchEditor;

    NumericalWidget durationEditor;

    NumericalWidget velocityEditor;

    NumericalWidget channelEditor;

    public static final int HEIGHT = 20;
}
