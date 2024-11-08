package ircam.jmax.editors.explode;

import java.awt.*;
import java.util.*;
import javax.swing.undo.*;

/**
 * A simple UndoableEdit that can be used when  the action to undo 
 * is related to one (or more) parameters changing in an event. 
 * It takes as argument the event that is going to be modified. 
 * @see UndoableAdd */
public class UndoableEventTransformation extends AbstractUndoableEdit {

    ScrEvent itsEvent;

    int undoTime;

    int undoPitch;

    int undoVelocity;

    int undoDuration;

    int undoChannel;

    int redoTime;

    int redoPitch;

    int redoVelocity;

    int redoDuration;

    int redoChannel;

    /**
   * Constructor. theEvent is the event that is going to be modified. */
    public UndoableEventTransformation(ScrEvent theEvent) {
        itsEvent = theEvent;
        undoTime = theEvent.getTime();
        undoPitch = theEvent.getPitch();
        undoVelocity = theEvent.getVelocity();
        undoDuration = theEvent.getDuration();
        undoChannel = theEvent.getChannel();
    }

    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    public String getPresentationName() {
        return "simple event transformation";
    }

    /**
   * Undo the trasformation */
    public void undo() {
        redoTime = itsEvent.getTime();
        redoPitch = itsEvent.getPitch();
        redoVelocity = itsEvent.getVelocity();
        redoDuration = itsEvent.getDuration();
        redoChannel = itsEvent.getChannel();
        itsEvent.setTime(undoTime);
        itsEvent.setPitch(undoPitch);
        itsEvent.setVelocity(undoVelocity);
        itsEvent.setDuration(undoDuration);
        itsEvent.setChannel(undoChannel);
    }

    /**
   * redo the trasformation */
    public void redo() {
        undoTime = itsEvent.getTime();
        undoPitch = itsEvent.getPitch();
        undoVelocity = itsEvent.getVelocity();
        undoDuration = itsEvent.getDuration();
        undoChannel = itsEvent.getChannel();
        itsEvent.setTime(redoTime);
        itsEvent.setPitch(redoPitch);
        itsEvent.setVelocity(redoVelocity);
        itsEvent.setDuration(redoDuration);
        itsEvent.setChannel(redoChannel);
    }
}
