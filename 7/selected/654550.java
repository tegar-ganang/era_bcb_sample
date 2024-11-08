package jpatch.control;

import java.util.*;
import javax.swing.event.*;
import jpatch.control.edit2.JPatchUndoableEdit;

public final class JPatchUndoManager {

    private static final int DEPTH = 50;

    private static final JPatchUndoableEdit[] ARRAY = new JPatchUndoableEdit[0];

    private static final Map<Object, JPatchUndoManager> undoManagerMap = new HashMap<Object, JPatchUndoManager>();

    private JPatchUndoableEdit[][] edits = new JPatchUndoableEdit[DEPTH][];

    private EditType[] editTypes = new EditType[DEPTH];

    private int currentEdit = -1;

    private int lastEdit = -1;

    private final ChangeEvent changeEvent = new ChangeEvent(this);

    private final EventListenerList listenerList = new EventListenerList();

    public static JPatchUndoManager getUndoManagerFor(Object object) {
        JPatchUndoManager undoManager = undoManagerMap.get(object);
        if (undoManager == null) {
            undoManager = new JPatchUndoManager();
            undoManagerMap.put(object, undoManager);
        }
        return undoManager;
    }

    private JPatchUndoManager() {
    }

    public void dispose() {
        for (Object key : undoManagerMap.keySet()) {
            if (undoManagerMap.get(key) == this) {
                undoManagerMap.remove(key);
                break;
            }
        }
    }

    public void addEdit(EditType type, JPatchUndoableEdit edit) {
        addEdit(type);
        edits[currentEdit] = new JPatchUndoableEdit[] { edit };
        fireStateChanged();
    }

    public void addEdit(EditType type, List<JPatchUndoableEdit> list) {
        addEdit(type);
        edits[currentEdit] = list.toArray(ARRAY);
        fireStateChanged();
    }

    public void appendEdit(EditType newType, JPatchUndoableEdit edit) {
        JPatchUndoableEdit[] tmp = new JPatchUndoableEdit[edits[currentEdit].length + 1];
        System.arraycopy(edits[currentEdit], 0, tmp, 0, edits[currentEdit].length);
        tmp[tmp.length - 1] = edit;
        edits[currentEdit] = tmp;
        editTypes[currentEdit] = newType;
        fireStateChanged();
    }

    public void appendEdit(EditType newType, List<JPatchUndoableEdit> editList) {
        JPatchUndoableEdit[] tmp = new JPatchUndoableEdit[edits[currentEdit].length + editList.size()];
        System.arraycopy(edits[currentEdit], 0, tmp, 0, edits[currentEdit].length);
        System.arraycopy(editList.toArray(ARRAY), 0, tmp, edits[currentEdit].length, editList.size());
        edits[currentEdit] = tmp;
        editTypes[currentEdit] = newType;
        fireStateChanged();
    }

    public void undo() {
        assert canUndo() : "Can't undo: currentEdit = " + currentEdit;
        for (int i = edits[currentEdit].length - 1; i >= 0; i--) {
            edits[currentEdit][i].undo();
        }
        currentEdit--;
        fireStateChanged();
    }

    public void redo() {
        assert canRedo() : "Can't redo: currentEdit = " + currentEdit + ", lastEdit = " + lastEdit;
        for (int i = 0; i < edits[currentEdit].length; i++) {
            edits[currentEdit][i].redo();
        }
        currentEdit++;
        fireStateChanged();
    }

    public boolean canUndo() {
        return currentEdit >= 0;
    }

    public boolean canRedo() {
        return currentEdit < lastEdit;
    }

    public String getUndoName() {
        return canUndo() ? editTypes[currentEdit].toString() : "";
    }

    public String getRedoName() {
        return canRedo() ? editTypes[currentEdit + 1].toString() : "";
    }

    public EditType getCurrentEditType() {
        return canUndo() ? editTypes[currentEdit] : null;
    }

    private void addEdit(EditType type) {
        currentEdit++;
        if (currentEdit >= DEPTH) {
            cycle();
        }
        lastEdit = currentEdit;
        editTypes[currentEdit] = type;
    }

    /**
     * Adds a <code>ChangeListener</code> to this JPatchUndoManager.
     *
     * @param l the listener to add
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a <code>ChangeListener</code> from this JPatchUndoManager.
     *
     * @param l the listener to remove
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    private void cycle() {
        for (int i = 0; i < DEPTH - 1; i++) {
            edits[i] = edits[i + 1];
            editTypes[i] = editTypes[i + 1];
        }
        currentEdit = DEPTH - 1;
    }

    private void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }
}
