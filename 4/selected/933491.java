package jmax.editors.explode;

import jmax.toolkit.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.ImageIcon;
import javax.swing.undo.*;

/**
 * the tool used to perform the different operations associated
 * with the "arrow" tool, that is:
 * selection, area_selection, deselection, moving.
 */
public class ArrowTool extends SelecterTool implements DirectionListener, DragListener {

    /**
   * Constructor. 
   */
    public ArrowTool(ImageIcon theImageIcon) {
        super("arrow", theImageIcon);
        itsDirectionChooser = new DirectionChooser(this);
        itsSelectionMover = new ExplodeSelectionMover(this, SelectionMover.HORIZONTAL_MOVEMENT);
    }

    /**
   * called when this tool is unmounted
   */
    public void deactivate() {
    }

    /**
   * a single object has been selected, in coordinates x, y.
   * Starts a move or a clone operation (if ALT is pressed).
   * overrides the abstract SelecterTool.singleObjectSelected */
    void singleObjectSelected(int x, int y, int modifiers) {
        if ((modifiers & InputEvent.ALT_DOWN_MASK) == 0) itsMoveMode = SIMPLE; else itsMoveMode = CLONE;
        mountIModule(itsDirectionChooser, x, y);
    }

    /** 
   * a group of objects was selected 
   *overrides the abstract SelecterTool.multipleObjectSelected */
    void multipleObjectSelected() {
    }

    /**
   * called by the DirectionChooser UI module
   */
    public void directionChoosen(int theDirection) {
        itsSelectionMover.setDirection(theDirection);
        mountIModule(itsSelectionMover, startingPoint.x, startingPoint.y);
    }

    /**
   * called by the DirectionChooser UI module
   */
    public void directionAbort() {
        mountIModule(itsSelecter);
    }

    /**
   * drag listener called by the SelectionMover UI module,
   * at the end of its interaction.
   * Moves all the selected elements
   */
    public void dragEnd(int x, int y) {
        ScrEvent newEvent;
        ExplodeGraphicContext egc = (ExplodeGraphicContext) gc;
        int deltaY = y - startingPoint.y;
        int deltaX = x - startingPoint.x;
        if (itsMoveMode == CLONE) {
            ((UndoableData) egc.getDataModel()).beginUpdate();
            for (ScrEvent aEvent : egc.getSelection().getSelected()) {
                newEvent = new ScrEvent(aEvent.getDataModel(), aEvent.getTime(), aEvent.getPitch(), aEvent.getVelocity(), aEvent.getDuration(), aEvent.getChannel());
                aEvent.getDataModel().addEvent(newEvent);
                egc.getAdapter().setX(newEvent, egc.getAdapter().getX(aEvent) + deltaX);
                egc.getAdapter().setY(newEvent, egc.getAdapter().getY(aEvent) + deltaY);
            }
            ((UndoableData) egc.getDataModel()).endUpdate();
        } else {
            ((UndoableData) egc.getDataModel()).beginUpdate();
            for (ScrEvent aEvent : egc.getSelection().getSelected()) {
                if (deltaX != 0) egc.getAdapter().setX(aEvent, egc.getAdapter().getX(aEvent) + deltaX);
                if (deltaY != 0) egc.getAdapter().setY(aEvent, egc.getAdapter().getY(aEvent) + deltaY);
            }
            ((UndoableData) egc.getDataModel()).endUpdate();
        }
        mountIModule(itsSelecter);
        gc.getGraphicDestination().repaint();
    }

    DirectionChooser itsDirectionChooser;

    SelectionMover itsSelectionMover;

    static final int SIMPLE = 0;

    static final int CLONE = 1;

    int itsMoveMode;
}
