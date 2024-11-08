package ircam.jmax.editors.sequence;

import ircam.fts.client.*;
import ircam.jmax.editors.sequence.renderers.*;
import ircam.jmax.editors.sequence.track.*;
import ircam.jmax.*;
import ircam.jmax.fts.*;
import ircam.jmax.toolkit.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import javax.swing.undo.*;
import javax.swing.*;

/**
* A general-purpose TrackDataModel, this class
 * offers an implementation based on a variable-length
 * array.
 * @see ircam.jmax.editors.sequence.track.TrackDataModel*/
public class FtsTrackObject extends FtsObjectWithEditor implements TrackDataModel, ClipableData, ClipboardOwner, TrackListener {

    static {
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("addEvent"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).addEventFromServer((TrackEvent) args.getObject(0));
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("addEvents"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).addEvents(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("removeEvents"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).removeEvents(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("clear"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).clear();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("moveEvents"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).moveEvents(args.getLength(), args.getAtoms(), true);
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("moveEventsFromServer"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).moveEvents(args.getLength(), args.getAtoms(), false);
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("lock"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).lock();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("unlock"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).unlock();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("active"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).active(args.getInt(0) == 1);
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("highlightEvents"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).highlightEvents(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("highlightEventsAndTime"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).highlightEventsAndTime(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("highlightReset"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).highlightReset();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("type"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).setType(args.getSymbol(0).toString());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("start_upload"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).startUpload(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("end_upload"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).endUpload();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("endPaste"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).endPaste();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("endUpdate"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).endUpdate();
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("properties"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).setTrackProperties(args.getLength(), args.getAtoms());
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("editor"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).setFtsTrackEditorObject(args.getInt(0));
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("save_editor"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).setSaveEditor(args.getInt(0) == 1);
            }
        });
        FtsObject.registerMessageHandler(FtsTrackObject.class, FtsSymbol.get("markers"), new FtsMessageHandler() {

            public void invoke(FtsObject obj, FtsArgs args) {
                ((FtsTrackObject) obj).setMarkersTrack(args.getLength(), args.getAtoms());
            }
        });
    }

    /**
* Create an AbstractSequence and initialize the type vector
 * with the given type.
 */
    public FtsTrackObject(FtsServer server, FtsObject parent, int objId, String className, FtsAtom args[], int offset, int length) {
        super(server, parent, objId, className, args, offset, length);
        ValueInfoTable.init();
        SequenceImages.init();
        if (args[offset].symbolValue != null) this.info = ValueInfoTable.getValueInfo(args[offset].symbolValue.toString()); else this.info = AnythingValue.info;
        listeners = new MaxVector();
        hhListeners = new MaxVector();
        stateListeners = new MaxVector();
        propertyNames = new MaxVector();
        propertyTypes = new MaxVector();
        propertyClasses = new MaxVector();
        if (flavors == null) flavors = new DataFlavor[1];
        flavors[0] = sequenceFlavor;
    }

    public boolean isInSequence() {
        return (getParent() instanceof FtsSequenceObject);
    }

    void setType(String type) {
        this.info = ValueInfoTable.getValueInfo(type);
    }

    public void setTrackProperties(int nArgs, FtsAtom args[]) {
        String type, prop;
        propertyNames.removeAllElements();
        propertyTypes.removeAllElements();
        propertyClasses.removeAllElements();
        for (int i = 0; i < nArgs - 1; i += 2) {
            prop = args[i].symbolValue.toString();
            type = args[i + 1].symbolValue.toString();
            if (type.equals("enum")) {
                int num = args[i + 2].intValue;
                eventTypesEnum.removeAllElements();
                for (int j = 1; j <= num; j++) eventTypesEnum.addElement(args[i + 2 + j].symbolValue.toString());
                i += num + 1;
            }
            propertyNames.addElement(prop);
            setPropertyType(type);
        }
    }

    void setPropertyType(String type) {
        Class typeClass;
        propertyTypes.addElement(type);
        if (type.equals("int")) typeClass = Integer.class; else if (type.equals("float")) typeClass = Float.class; else if (type.equals("double")) typeClass = Double.class; else if (type.equals("object")) typeClass = FtsGraphicObject.class; else typeClass = String.class;
        propertyClasses.addElement(typeClass);
    }

    public Enumeration getPropertyNames() {
        return propertyNames.elements();
    }

    public int getPropertyCount() {
        return propertyNames.size();
    }

    public Class getPropertyType(int i) {
        return (Class) propertyClasses.elementAt(i);
    }

    public Vector getEventTypes() {
        return eventTypesEnum;
    }

    public void beginUpdate(String type) {
        if (isMarkersTrack()) ((FtsUndoableObject) getParent()).beginUpdate(type); else super.beginUpdate(type);
    }

    public void beginUpdate() {
        if (isMarkersTrack()) ((FtsUndoableObject) getParent()).beginUpdate(); else super.beginUpdate();
    }

    public void endUpdate(String type) {
        if (isMarkersTrack()) ((FtsUndoableObject) getParent()).endUpdate(type); else super.endUpdate(type);
    }

    public void endUpdate() {
        if (isMarkersTrack()) ((FtsUndoableObject) getParent()).endUpdate(); else super.endUpdate();
    }

    public boolean isInGroup() {
        if (isMarkersTrack()) return ((FtsUndoableObject) getParent()).isInGroup(); else return super.isInGroup();
    }

    public void postEdit(UndoableEdit e) {
        if (isMarkersTrack()) ((FtsUndoableObject) getParent()).postEdit(e); else super.postEdit(e);
    }

    /**
* Fts callback: add a TrackEvent(first arg) in a track (second arg).
 *
 */
    public void addEventFromServer(TrackEvent evt) {
        addEvent(evt);
        endUpdate("addEvent");
    }

    public void addEvents(int nArgs, FtsAtom args[]) {
        addEvent(new TrackEvent(getServer(), this, args[0].intValue, "event", args, 1, nArgs));
        if (!isMarkersTrack() || (isMarkersTrack() && length() > 1)) endUpdate("addEvent");
    }

    public void removeEvents(int nArgs, FtsAtom args[]) {
        int removeIndex;
        TrackEvent event = null;
        for (int i = 0; i < nArgs; i++) {
            event = (TrackEvent) (args[i].objectValue);
            removeIndex = indexOf(event);
            deleteEventAt(removeIndex);
        }
        endUpdate("removeEvents");
    }

    public void clear() {
        while (events_fill_p != 0) {
            if (isInGroup()) postEdit(new UndoableDelete(events[0]));
            deleteRoomAt(0);
        }
        if (markersTrack != null) markersTrack.clear();
        notifyTrackCleared();
        endUpdate();
    }

    public void moveEvents(int nArgs, FtsAtom args[], boolean fromClient) {
        TrackEvent evt;
        int oldIndex, newIndex;
        double time;
        double maxTime = 0.0;
        TrackEvent maxEvent = null;
        int maxOldIndex = 0;
        int maxNewIndex = 0;
        for (int i = 0; i < nArgs; i += 2) {
            evt = (TrackEvent) (args[i].objectValue);
            oldIndex = indexOf(evt);
            deleteRoomAt(oldIndex);
            time = args[i + 1].doubleValue;
            if (isInGroup()) postEdit(new UndoableMove(evt, time));
            evt.setTime(time);
            newIndex = getIndexAfter(time);
            if (newIndex == EMPTY_COLLECTION) newIndex = 0; else if (newIndex == NO_SUCH_EVENT) newIndex = events_fill_p;
            makeRoomAt(newIndex);
            events[newIndex] = evt;
            if (time > maxTime) {
                maxTime = time;
                maxEvent = evt;
                maxOldIndex = oldIndex;
                maxNewIndex = newIndex;
            }
            notifyObjectMoved(evt, oldIndex, newIndex, fromClient);
        }
        if (nArgs > 0) notifyLastObjectMoved(maxEvent, maxOldIndex, maxNewIndex, fromClient);
        endUpdate();
    }

    public void nameChanged(String name) {
        super.nameChanged(name);
        notifyFtsNameChanged(name);
    }

    public void lock() {
        locked = true;
        notifyLock(true);
    }

    public void unlock() {
        locked = false;
        notifyLock(false);
    }

    public void active(boolean active) {
        notifyActive(active);
    }

    public void highlightEvents(int nArgs, FtsAtom args[]) {
        TrackEvent event = null;
        MaxVector events = new MaxVector();
        for (int i = 0; i < nArgs; i++) events.addElement((TrackEvent) (args[i].objectValue));
        double time = ((TrackEvent) args[0].objectValue).getTime();
        if (getEditorFrame() != null && !uploading) notifyHighlighting(events, time);
    }

    public void highlightEventsAndTime(int nArgs, FtsAtom args[]) {
        MaxVector events = new MaxVector();
        double time = args[0].doubleValue;
        if (nArgs > 1) for (int i = 1; i < nArgs; i++) events.addElement((TrackEvent) (args[i].objectValue));
        if (getEditorFrame() != null && !uploading) notifyHighlighting(events, time);
    }

    public void highlightReset() {
        if (getEditorFrame() != null && !uploading) notifyHighlightReset();
    }

    public void requestClearTrack() {
        try {
            send(FtsSymbol.get("clear"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending clear Message!");
            e.printStackTrace();
        }
    }

    public void requestNotifyGuiListeners(double time, TrackEvent evt) {
        args.clear();
        if (evt == null) args.addDouble(time); else args.addDouble(evt.getTime());
        try {
            send(FtsSymbol.get("notify_gui_listeners"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending notify_gui_listeners Message!");
            e.printStackTrace();
        }
    }

    public void export() {
        try {
            send(FtsSymbol.get("export"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending export_midi_dialog Message!");
            e.printStackTrace();
        }
    }

    public void importMidiFile() {
        try {
            send(FtsSymbol.get("import"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending import_midifile Message!");
            e.printStackTrace();
        }
    }

    public void requestSetActive(boolean active) {
        args.clear();
        args.addInt((active) ? 1 : 0);
        try {
            send(FtsSymbol.get("active"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending active Message!");
            e.printStackTrace();
        }
    }

    public void requestUpload() {
        try {
            send(FtsSymbol.get("upload"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending upload Message!");
            e.printStackTrace();
        }
    }

    public void requestEndPaste() {
        try {
            send(FtsSymbol.get("endPaste"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending endPaste Message!");
            e.printStackTrace();
        }
    }

    public void requestNotifyEndUpdate() {
        try {
            send(FtsSymbol.get("endUpdate"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending endUpdate Message!");
            e.printStackTrace();
        }
    }

    public void requestEventCreation(float time, String type, int nArgs, Object arguments[]) {
        args.clear();
        args.addDouble((double) time);
        args.addSymbol(FtsSymbol.get(type));
        for (int i = 0; i < nArgs; i++) {
            if (arguments[i] instanceof Double) args.addDouble(((Double) arguments[i]).floatValue()); else if (arguments[i] instanceof String) args.addSymbol(FtsSymbol.get((String) arguments[i])); else args.add(arguments[i]);
        }
        try {
            send(FtsSymbol.get("addEvent"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending addEvent Message!");
            e.printStackTrace();
        }
    }

    public void requestEventCreationWithoutUpload(float time, String type, int nArgs, Object arguments[]) {
        args.clear();
        args.addDouble((double) time);
        args.addSymbol(FtsSymbol.get(type));
        for (int i = 0; i < nArgs; i++) if (arguments[i] instanceof Double) args.addDouble(((Double) arguments[i]).doubleValue()); else if (arguments[i] instanceof String) args.addSymbol(FtsSymbol.get((String) arguments[i])); else args.add(arguments[i]);
        try {
            send(FtsSymbol.get("makeEvent"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending makeEvent Message!");
            e.printStackTrace();
        }
    }

    public void requestEventMove(TrackEvent evt, double newTime) {
        if (evt.getValue().isMovable()) {
            args.clear();
            args.addObject(evt);
            args.addDouble(newTime);
            try {
                send(FtsSymbol.get("moveEvents"), args);
            } catch (IOException e) {
                System.err.println("FtsTrackObject: I/O Error sending moveEvents Message!");
                e.printStackTrace();
            }
        }
    }

    public void requestEventsMove(Enumeration events, int deltaX, Adapter a) {
        TrackEvent aEvent;
        args.clear();
        for (Enumeration e = events; e.hasMoreElements(); ) {
            aEvent = (TrackEvent) e.nextElement();
            if (aEvent.getValue().isMovable()) {
                args.addObject(aEvent);
                args.addDouble((double) a.getInvX(a.getX(aEvent) + deltaX));
            }
        }
        try {
            send(FtsSymbol.get("moveEvents"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending moveEvents Message!");
            e.printStackTrace();
        }
    }

    public void requestSetSaveEditor(boolean save) {
        if (saveEditor != save) {
            try {
                args.clear();
                args.addInt((save) ? 1 : 0);
                send(FtsSymbol.get("save_editor"), args);
            } catch (IOException e) {
                System.err.println("FtsTrackObject: I/O Error sending save_editor Message!");
                e.printStackTrace();
            }
        }
        if (save && editorObject != null) editorObject.requestSetEditorState(isInSequence() ? null : getEditorFrame().getBounds());
    }

    public void requestInsertMarker(double time) {
        try {
            args.clear();
            args.addDouble(time);
            args.addSymbol(FtsSymbol.get("marker"));
            send(FtsSymbol.get("insert_marker"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending insert_marker Message!");
            e.printStackTrace();
        }
    }

    public void makeTrillFromSelection(Enumeration events) {
        beginUpdate("removeEvents");
        try {
            args.clear();
            for (Enumeration e = events; e.hasMoreElements(); ) args.addObject((TrackEvent) e.nextElement());
            send(FtsSymbol.get("make_trill"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending make_trill Message!");
            e.printStackTrace();
        }
    }

    public void appendBar(TrackEvent evt) {
        beginUpdate("addEvent");
        try {
            args.clear();
            if (evt != null) args.addObject(evt);
            send(FtsSymbol.get("append_bar"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending append_bar Message!");
            e.printStackTrace();
        }
    }

    public void collapseMarkers(Enumeration events) {
        try {
            args.clear();
            for (Enumeration e = events; e.hasMoreElements(); ) args.addObject((TrackEvent) e.nextElement());
            markersTrack.send(FtsSymbol.get("collapse_markers"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending collapse_markers Message!");
            e.printStackTrace();
        }
    }

    /**
* how many events in the database?
 */
    public int length() {
        return events_fill_p;
    }

    public double getMaximumTime() {
        double time;
        double max = 0;
        for (int i = 0; i < events_fill_p; i++) {
            time = events[i].getTime() + ((Double) events[i].getProperty("duration")).intValue();
            if (time > max) max = time;
        }
        return max;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
* returns an enumeration of all the events
 */
    public Enumeration getEvents() {
        return new SequenceEnumeration();
    }

    public Enumeration getEvents(int startIndex, int endIndex) {
        return new SequenceEnumeration(startIndex, endIndex);
    }

    /**
* returns a given event
 */
    public TrackEvent getEventAt(int index) {
        return events[index];
    }

    public TrackEvent getNextEvent(Event evt) {
        int index;
        if (evt instanceof UtilTrackEvent) index = getFirstEventAfter(evt.getTime() + 0.001); else index = indexOf(evt) + 1;
        if ((index != EMPTY_COLLECTION) && (index < events_fill_p) && (index >= 0)) return events[index]; else return null;
    }

    public TrackEvent getPreviousEvent(double time) {
        int index = getFirstEventBefore(time);
        if ((index != EMPTY_COLLECTION) && (index < events_fill_p) && (index >= 0)) return events[index]; else return null;
    }

    public TrackEvent getPreviousEvent(Event evt) {
        int index = getFirstEventBefore(evt.getTime());
        if ((index != EMPTY_COLLECTION) && (index < events_fill_p) && (index >= 0)) return events[index]; else return null;
    }

    public TrackEvent getLastEvent() {
        if (events_fill_p > 0) return events[events_fill_p - 1]; else return null;
    }

    /**
* return the index of the given event, if it exists, or the error constants
 * NO_SUCH_EVENT, EMPTY_COLLECTION */
    public int indexOf(Event event) {
        int index = getFirstEventAt(event.getTime());
        if (index == NO_SUCH_EVENT || index == EMPTY_COLLECTION) {
            return index;
        }
        for (; getEventAt(index) != event; index++) {
            if (index >= events_fill_p) return NO_SUCH_EVENT;
        }
        return index;
    }

    public Enumeration intersectionSearch(double start, double end) {
        return new Intersection(start, end);
    }

    public Enumeration inclusionSearch(double start, double end) {
        return new Inclusion(start, end);
    }

    public int getFirstEventAt(double time) {
        if (events_fill_p == 0) return EMPTY_COLLECTION; else if (events[events_fill_p - 1].getTime() < time) return NO_SUCH_EVENT;
        int min = 0;
        int max = events_fill_p - 1;
        int med = 0;
        while (max > min + 1) {
            med = (max + min) / 2;
            if (events[med].getTime() >= time) max = med; else min = med;
        }
        if (events[min].getTime() == time) return min; else if (events[max].getTime() == time) return max; else return NO_SUCH_EVENT;
    }

    public int getFirstEventAfter(double time) {
        if (events_fill_p == 0) return EMPTY_COLLECTION; else if (events[events_fill_p - 1].getTime() <= time) return NO_SUCH_EVENT;
        if (events[0].getTime() >= time) return 0;
        int min = 0;
        int max = events_fill_p - 1;
        int med = 0;
        while (max > min + 1) {
            med = (max + min) / 2;
            if (events[med].getTime() >= time) max = med; else min = med;
        }
        return max;
    }

    public int getFirstEventBefore(double time) {
        if (events_fill_p == 0) return EMPTY_COLLECTION; else if (events[0].getTime() >= time) return NO_SUCH_EVENT; else if (events[events_fill_p - 1].getTime() < time) return events_fill_p - 1;
        int min = 0;
        int max = events_fill_p - 1;
        int med = 0;
        while (max > min + 1) {
            med = (max + min) / 2;
            if (events[med].getTime() >= time) max = med; else min = med;
        }
        return min;
    }

    /**
* adds an event in the database
 */
    public void addEvent(TrackEvent event) {
        int index;
        event.setDataModel(this);
        index = getIndexAfter(event.getTime());
        if (index == EMPTY_COLLECTION) index = 0; else if (index == NO_SUCH_EVENT) index = events_fill_p;
        makeRoomAt(index);
        events[index] = event;
        notifyObjectAdded(event, index);
        if (isInGroup()) postEdit(new UndoableAdd(event));
    }

    /**
* generic change of an event in the database.
 * Call this function to signal the parameters changing of the event, except
 * the initial time and the duration parameters. Use moveEvent and resizeEvent for that.
 */
    public void changeEvent(TrackEvent event, String propName, Object propValue) {
        int index;
        index = indexOf(event);
        if (index == NO_SUCH_EVENT || index == EMPTY_COLLECTION) return;
        notifyObjectChanged(event, index, propName, propValue);
    }

    /**
* move an event in the database
 */
    public void moveEvent(TrackEvent event, double newTime) {
        int index = indexOf(event);
        int newIndex = getIndexAfter(newTime);
        if (newIndex == NO_SUCH_EVENT) newIndex = events_fill_p - 1; else if (event.getTime() <= newTime) newIndex -= 1;
        if (index == NO_SUCH_EVENT) {
            System.err.println("no such event error");
            return;
        }
        if (index == EMPTY_COLLECTION) index = 0;
        event.setTime(newTime);
        if (index < newIndex) {
            for (int i = index; i < newIndex; i++) {
                events[i] = events[i + 1];
            }
        } else {
            for (int i = index; i > newIndex; i--) {
                events[i] = events[i - 1];
            }
            events[newIndex] = event;
        }
        events[newIndex] = event;
        notifyObjectMoved(event, index, newIndex, true);
    }

    /**
* deletes an event from the database
 */
    public void deleteEvent(TrackEvent event) {
        args.clear();
        args.add(event);
        try {
            send(FtsSymbol.get("removeEvents"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending removeEvents Message!");
            e.printStackTrace();
        }
    }

    public void deleteEvents(Enumeration events) {
        args.clear();
        for (Enumeration e = events; e.hasMoreElements(); ) args.add((TrackEvent) e.nextElement());
        try {
            send(FtsSymbol.get("removeEvents"), args);
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending removeEvents Message!");
            e.printStackTrace();
        }
    }

    public void deleteAllEvents() {
        while (events_fill_p != 0) deleteEvent(events[0]);
    }

    private void deleteEventAt(int removeIndex) {
        TrackEvent event = getEventAt(removeIndex);
        if (removeIndex == NO_SUCH_EVENT || removeIndex == EMPTY_COLLECTION) return;
        if (isInGroup()) postEdit(new UndoableDelete(event));
        deleteRoomAt(removeIndex);
        notifyObjectDeleted(event, removeIndex);
    }

    /**
* utility to notify the data base change to all the listeners
 */
    private void notifyObjectAdded(Object spec, int index) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).objectAdded(spec, index);
    }

    private void notifyObjectsAdded(int maxTime) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).objectsAdded(maxTime);
    }

    private void notifyObjectDeleted(Object spec, int oldIndex) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).objectDeleted(spec, oldIndex);
    }

    private void notifyObjectChanged(Object spec, int index, String propName, Object propValue) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).objectChanged(spec, index, propName, propValue);
    }

    void notifyFtsNameChanged(String name) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).ftsNameChanged(name);
    }

    private void notifyTrackCleared() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).trackCleared();
    }

    private void notifyUploadStart(int size) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).startTrackUpload(this, size);
    }

    private void notifyUploadEnd() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).endTrackUpload(this);
    }

    private void notifyStartPaste() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).startPaste();
    }

    private void notifyEndPaste() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).endPaste();
    }

    private void notifyStartUndoRedo() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).startUndoRedo();
    }

    private void notifyEndUndoRedo() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).endUndoRedo();
    }

    private void notifyObjectMoved(Object spec, int oldIndex, int newIndex, boolean fromClient) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).objectMoved(spec, oldIndex, newIndex, fromClient);
    }

    private void notifyLastObjectMoved(Object spec, int oldIndex, int newIndex, boolean fromClient) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((TrackDataListener) e.nextElement()).lastObjectMoved(spec, oldIndex, newIndex, fromClient);
    }

    private void notifyHighlighting(MaxVector hhobj, double time) {
        for (Enumeration e = hhListeners.elements(); e.hasMoreElements(); ) ((HighlightListener) e.nextElement()).highlight(hhobj.elements(), time);
    }

    private void notifyHighlightReset() {
        for (Enumeration e = hhListeners.elements(); e.hasMoreElements(); ) ((HighlightListener) e.nextElement()).highlightReset();
    }

    private void notifyLock(boolean lock) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).lock(lock);
    }

    private void notifyActive(boolean active) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).active(active);
    }

    private void notifyRestoreEditorState(FtsTrackEditorObject editorObject) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).restoreEditorState(editorObject);
    }

    private void notifyMarkers(FtsTrackObject markers, SequenceSelection markersSelection) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).hasMarkers(markers, markersSelection);
    }

    void notifyUpdateMarkers(FtsTrackObject markers, SequenceSelection markersSelection) {
        for (Enumeration e = stateListeners.elements(); e.hasMoreElements(); ) ((TrackStateListener) e.nextElement()).updateMarkers(markers, markersSelection);
    }

    /**
* requires to be notified when the database changes
 */
    public void addListener(TrackDataListener theListener) {
        listeners.addElement(theListener);
    }

    /**
* requires to be notified at events highlight
 */
    public void addHighlightListener(HighlightListener listener) {
        hhListeners.addElement(listener);
    }

    public void addTrackStateListener(TrackStateListener listener) {
        stateListeners.addElement(listener);
    }

    /**
* removes the listener
 */
    public void removeListener(TrackDataListener theListener) {
        listeners.removeElement(theListener);
    }

    /**
* removes the listener
 */
    public void removeHighlightListener(HighlightListener theListener) {
        hhListeners.removeElement(theListener);
    }

    public void removeTrackStateListener(TrackStateListener theListener) {
        stateListeners.removeElement(theListener);
    }

    public void cut() {
        if (SequenceSelection.getCurrent().getModel() != this) return;
        copy();
        beginUpdate();
        SequenceSelection.getCurrent().deleteAll();
    }

    public void copy() {
        if (SequenceSelection.getCurrent().getModel() != this) return;
        SequenceSelection.getCurrent().prepareACopy();
        SequenceClipboard.getClipboard().setContents(SequenceSelection.getCurrent(), this);
    }

    public void paste() {
        if (SequenceSelection.getCurrent().getModel() != this) return;
        Transferable clipboardContent = SequenceClipboard.getClipboard().getContents(this);
        Enumeration objectsToPaste = null;
        if (clipboardContent != null && areMyDataFlavorsSupported(clipboardContent)) {
            try {
                objectsToPaste = (Enumeration) clipboardContent.getTransferData(SequenceDataFlavor.getInstance());
            } catch (UnsupportedFlavorException ufe) {
                System.err.println("Clipboard error in paste: content does not support " + SequenceDataFlavor.getInstance().getHumanPresentableName());
            } catch (IOException ioe) {
                System.err.println("Clipboard error in paste: content is no more an " + SequenceDataFlavor.getInstance().getHumanPresentableName());
            }
        }
        if (objectsToPaste != null) {
            Event event;
            SequenceSelection.getCurrent().deselectAll();
            if (objectsToPaste.hasMoreElements()) {
                event = (Event) objectsToPaste.nextElement();
                if (event.getValue().getValueInfo() != getType()) {
                    System.err.println("Clipboard error in paste: attempt to copy <" + event.getValue().getValueInfo().getPublicName() + "> events in <" + getType().getPublicName() + "> track!");
                    return;
                }
                try {
                    startPaste();
                    beginUpdate();
                    requestEventCreation((float) event.getTime(), event.getValue().getValueInfo().getName(), event.getValue().getDefinedPropertyCount() * 2, event.getValue().getDefinedPropertyNamesAndValues());
                    while (objectsToPaste.hasMoreElements()) {
                        event = (Event) objectsToPaste.nextElement();
                        requestEventCreation((float) event.getTime(), event.getValue().getValueInfo().getName(), event.getValue().getDefinedPropertyCount() * 2, event.getValue().getDefinedPropertyNamesAndValues());
                    }
                    requestEndPaste();
                } catch (Exception e) {
                    System.err.println("FtsTrackObject: error in paste " + e);
                }
            }
        }
    }

    /** ClipboardOwner interface */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    boolean areMyDataFlavorsSupported(Transferable clipboardContent) {
        boolean supported = true;
        for (int i = 0; i < flavors.length; i++) supported = supported && clipboardContent.isDataFlavorSupported(flavors[i]);
        return supported;
    }

    final int getIndexAfter(double time) {
        if (events_fill_p == 0) return EMPTY_COLLECTION; else if (events[events_fill_p - 1].getTime() <= time) return NO_SUCH_EVENT;
        int min = 0;
        int max = events_fill_p - 1;
        while (max > min + 1) {
            int med = (max + min) / 2;
            if (events[med].getTime() <= time) min = med; else max = med;
        }
        if (time < events[min].getTime()) return min; else if (time > events[max].getTime()) return max + 1; else return max;
    }

    /**
* utility function to make the event vector bigger
 */
    protected final void reallocateEvents() {
        int new_size;
        TrackEvent temp_events[];
        new_size = (events_size * 3) / 2;
        temp_events = new TrackEvent[new_size];
        for (int i = 0; i < events_size; i++) temp_events[i] = events[i];
        events = temp_events;
        events_size = new_size;
    }

    /**
* utility function to create a new place at the given index
 */
    private final void makeRoomAt(int index) {
        if (events_fill_p >= events_size) reallocateEvents();
        for (int i = events_fill_p; i > index; i--) events[i] = events[i - 1];
        events_fill_p++;
    }

    /**
* deletes the place at the given index
 */
    private final void deleteRoomAt(int index) {
        for (int i = index; i < events_fill_p - 1; i++) events[i] = events[i + 1];
        events_fill_p--;
    }

    /**
* an utility class to efficiently implement the getEvents()
 * call
 */
    private class SequenceEnumeration implements Enumeration {

        int p;

        int start, end;

        public SequenceEnumeration() {
            start = 0;
            end = events_fill_p;
            p = start;
        }

        public SequenceEnumeration(int startIndex, int endIndex) {
            if (startIndex < 0) start = 0; else start = startIndex;
            if (endIndex > events_fill_p) end = events_fill_p; else end = endIndex + 1;
            p = start;
        }

        public boolean hasMoreElements() {
            return p < end;
        }

        public Object nextElement() {
            return events[p++];
        }
    }

    /**
* an utility class to implement the intersection with a range */
    class Intersection implements Enumeration {

        Intersection(double start, double end) {
            endTime = end;
            startTime = start;
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
            TrackEvent e;
            while (index < length() && events[index].getTime() <= endTime) {
                e = events[index++];
                if (e.getTime() >= startTime || e.getTime() + ((Double) e.getProperty("duration")).intValue() >= startTime) {
                    return e;
                }
            }
            return null;
        }

        double endTime;

        double startTime;

        int index;

        Object nextObject = null;
    }

    /**
* AN utility class to return an enumeration of all the events
 * in a temporal range */
    class Inclusion implements Enumeration {

        Inclusion(double start, double end) {
            this.endTime = end;
            index = getIndexAfter(start);
            endIndex = getIndexAfter(end);
        }

        public boolean hasMoreElements() {
            nextObject = findNext();
            return nextObject != null;
        }

        public Object nextElement() {
            return nextObject;
        }

        private Object findNext() {
            TrackEvent e;
            while (index < endIndex) {
                e = events[index++];
                if (e.getTime() + ((Double) e.getProperty("duration")).intValue() <= endTime) {
                    return e;
                }
            }
            return null;
        }

        double endTime;

        int index;

        int endIndex;

        Object nextObject = null;
    }

    /**
* Move all the events of the given model in this model. Merging is allowed only between
 * tracks of the same type. Merging is not undoable.
 */
    public void mergeModel(TrackDataModel model) {
        Event event;
        beginUpdate();
        try {
            for (Enumeration e = model.getEvents(); e.hasMoreElements(); ) {
                event = (Event) e.nextElement();
                requestEventCreationWithoutUpload((float) event.getTime(), event.getValue().getValueInfo().getName(), event.getValue().getDefinedPropertyCount() * 2, event.getValue().getDefinedPropertyNamesAndValues());
            }
            try {
                send(FtsSymbol.get("upload"));
            } catch (IOException e) {
                System.err.println("FtsTrackObject: I/O Error sending upload Message!");
                e.printStackTrace();
            }
        } catch (Exception e) {
        }
    }

    /**
* Returns the ValueInfo contained in this model
 */
    public ValueInfo getType() {
        return info;
    }

    public String getFtsName() {
        return super.getVariableName();
    }

    public DataFlavor[] getDataFlavors() {
        return flavors;
    }

    public TrackEvent getEventLikeThis(Event e) {
        TrackEvent evt;
        TrackEvent retEvt = null;
        int index = getFirstEventAt((float) e.getTime());
        if ((index != EMPTY_COLLECTION) && (index < events_fill_p) && (index >= 0)) {
            evt = events[index];
            if (e.getValue().samePropertyValues(evt.getValue().getDefinedPropertyCount() * 2, evt.getValue().getDefinedPropertyNamesAndValues())) retEvt = evt; else {
                evt = getNextEvent(evt);
                while ((evt != null) && (evt.getTime() == e.getTime())) {
                    if (e.getValue().samePropertyValues(evt.getValue().getDefinedPropertyCount() * 2, evt.getValue().getDefinedPropertyNamesAndValues())) {
                        retEvt = evt;
                        break;
                    } else evt = getNextEvent(evt);
                }
            }
        }
        return retEvt;
    }

    /** utility function */
    protected void addFlavor(DataFlavor flavor) {
        int dim = flavors.length;
        DataFlavor temp[] = new DataFlavor[dim + 1];
        for (int i = 0; i < dim; i++) {
            temp[i] = flavors[i];
        }
        temp[dim] = flavor;
        flavors = temp;
    }

    /********************************************************
*  others FtsObjects
********************************************************/
    public void setFtsTrackEditorObject(int id) {
        if (editorObject == null || (editorObject == null && editorObject.getID() != id)) editorObject = new FtsTrackEditorObject(JMaxApplication.getFtsServer(), this, id);
    }

    public FtsTrackEditorObject getFtsTrackEditorObject() {
        return editorObject;
    }

    public void setMarkersTrack(int nArgs, FtsAtom args[]) {
        markersTrack = new FtsTrackObject(JMaxApplication.getFtsServer(), this, args[0].intValue, "track", args, 1, nArgs);
        markersTrack.setAsMarkersTrack();
        markersSelection = new SequenceSelection(markersTrack);
        if (!isInSequence() || (isInSequence() && ((FtsSequenceObject) getParent()).getTrackIndex(this) == 0)) notifyMarkers(markersTrack, markersSelection);
    }

    public FtsTrackObject getMarkersTrack() {
        return markersTrack;
    }

    boolean iAmMarkersTrack = false;

    public boolean isMarkersTrack() {
        return iAmMarkersTrack;
    }

    public void setAsMarkersTrack() {
        iAmMarkersTrack = true;
    }

    public SequenceSelection getMarkersSelection() {
        return markersSelection;
    }

    public void setSaveEditor(boolean save) {
        saveEditor = save;
    }

    public void createMarkers() {
        try {
            send(FtsSymbol.get("make_bars"));
        } catch (IOException e) {
            System.err.println("FtsTrackObject: I/O Error sending make_bars Message!");
            e.printStackTrace();
        }
    }

    /********************************************************
*  FtsObjectWithEditor
********************************************************/
    public boolean opening = false;

    public void createEditor() {
        if (getEditorFrame() == null) setEditorFrame(new TrackWindow(this));
    }

    public void reinitEditorFrame() {
        setEditorFrame(new TrackWindow((TrackWindow) getEditorFrame()));
    }

    public void destroyEditor() {
        disposeEditor();
        System.gc();
    }

    boolean editorRestored = false;

    public void restoreEditorState() {
        if (editorObject != null && editorObject.haveContent()) {
            if (!isInSequence() && getEditorFrame() != null) ((TrackWindow) getEditorFrame()).restoreWindowSize(editorObject.wx, editorObject.wy, editorObject.ww, editorObject.wh - 1);
            notifyRestoreEditorState(editorObject);
            editorRestored = true;
        }
    }

    /********************************************************/
    void startUpload(int size) {
        uploading = true;
        uploadingSize = size;
        notifyUploadStart(size);
    }

    void endUpload() {
        uploading = false;
        uploadingSize = 0;
        if (!editorRestored) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (saveEditor) restoreEditorState();
                    notifyUploadEnd();
                    editorRestored = false;
                }
            });
        } else editorRestored = false;
    }

    public boolean isUploading() {
        return uploading;
    }

    public int getUploadingSize() {
        return uploadingSize;
    }

    void startPaste() {
        pasting = true;
        notifyStartPaste();
    }

    void endPaste() {
        pasting = false;
        notifyEndPaste();
    }

    public void requestUndo() {
        startUndoRedo();
        try {
            ((UndoableData) this).undo();
        } catch (CannotUndoException e1) {
            System.out.println("Can't undo");
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                requestPing("undo");
            }
        });
    }

    public void requestRedo() {
        startUndoRedo();
        try {
            ((UndoableData) this).redo();
        } catch (CannotRedoException e1) {
            System.out.println("Can't redo");
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                requestPing("redo");
            }
        });
    }

    public boolean isUndoRedoing = false;

    public void startUndoRedo() {
        isUndoRedoing = true;
        notifyStartUndoRedo();
    }

    public void endUndoRedo() {
        isUndoRedoing = false;
        notifyEndUndoRedo();
    }

    public boolean getUndoRedoing() {
        return isUndoRedoing;
    }

    public void ping(String ping) {
        if (ping.equals("undo") || ping.equals("redo")) endUndoRedo();
    }

    public void trackAdded(Track track, boolean isUploading) {
    }

    public void tracksAdded(int maxTime) {
    }

    public void trackRemoved(Track track) {
    }

    public void trackChanged(Track track) {
    }

    public void trackMoved(Track track, int oldPosition, int newPosition) {
    }

    public void ftsNameChanged(String name) {
    }

    public void sequenceStartUpload() {
        sequenceUploading = true;
    }

    ;

    public void sequenceEndUpload() {
        sequenceUploading = false;
    }

    ;

    public void sequenceClear() {
    }

    ;

    boolean sequenceUploading = false;

    public boolean isSequenceUploading() {
        return sequenceUploading;
    }

    ValueInfo info;

    boolean pasting = false;

    boolean uploading = false;

    boolean locked = false;

    int uploadingSize = 0;

    public boolean saveEditor = false;

    int events_size = 256;

    int events_fill_p = 0;

    TrackEvent events[] = new TrackEvent[256];

    private transient MaxVector listeners;

    private transient MaxVector hhListeners;

    private transient MaxVector stateListeners;

    private transient MaxVector tempVector = new MaxVector();

    private MaxVector propertyTypes, propertyNames, propertyClasses;

    private Vector eventTypesEnum = new Vector();

    public transient DataFlavor flavors[];

    public FtsTrackEditorObject editorObject = null;

    public FtsTrackObject markersTrack = null;

    public SequenceSelection markersSelection = null;

    public static transient DataFlavor sequenceFlavor = new DataFlavor(ircam.jmax.editors.sequence.SequenceSelection.class, "SequenceSelection");

    protected transient FtsArgs args = new FtsArgs();
}
