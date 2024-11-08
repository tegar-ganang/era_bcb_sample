package edu.rice.cs.cunit.record;

import com.sun.jdi.*;
import edu.rice.cs.cunit.record.graph.LockInfo;
import edu.rice.cs.cunit.record.graph.ThreadInfo;
import edu.rice.cs.cunit.record.graph.WaitGraph;
import edu.rice.cs.cunit.record.syncPoints.ISyncPoint;
import edu.rice.cs.cunit.record.syncPoints.object.ObjectEnterWaitSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.object.ObjectLeaveWaitSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.object.ObjectNotifyAllSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.object.ObjectNotifySyncPoint;
import edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedEnterBlockSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedLeaveBlockSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedTryEnterBlockSyncPoint;
import edu.rice.cs.cunit.record.syncPoints.thread.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processing for verbose object events.
 * @author Mathias Ricken
 */
public class ObjectProcessor implements ISyncPointProcessor {

    /**
     * List with thread information.
     */
    private HashMap<Long, ThreadInfo> _threads;

    /**
     * List with lock information.
     */
    private HashMap<Long, LockInfo> _locks;

    /**
     * Writer for output.
     */
    private PrintWriter _writer;

    /**
     * Wait graph.
     */
    WaitGraph _waitGraph;

    /**
     * Thread monitor instance.
     */
    private Record _record;

    /**
     * Create a new processor for verbose object events.
     * @param writer writer for output
     * @param record Record model
     */
    public ObjectProcessor(PrintWriter writer, Record record) {
        _threads = new HashMap<Long, ThreadInfo>();
        _locks = new HashMap<Long, LockInfo>();
        _writer = writer;
        _record = record;
    }

    /**
     * Processes the verbose synchronization points since the last request and then clears the list in the
     * debugged VM.
     * @param force true if processing should be forced, even if debugged VM is in the process of changing the list
     * @param recorderClass class of the recorder
     * @param recorderClassObject class object of the recorder
     * @param methodDatabase method database (key=class index/method index pair in "%08x %08x" format)
     * @return true if update was successful, false if it was delayed
     */
    public boolean process(boolean force, ClassType recorderClass, ClassObjectReference recorderClassObject, HashMap<String, String> methodDatabase) {
        Field indexField = recorderClass.fieldByName("_index");
        if (null == indexField) {
            throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._index field.");
        }
        Value indexValue = recorderClass.getValue(indexField);
        if (!(indexValue instanceof IntegerValue)) {
            throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._index.");
        }
        IntegerValue index = (IntegerValue) indexValue;
        _record.setProperty("numSyncPointsInList", index.intValue());
        _record.setProperty("numTotalSyncPoints", _record.getTotalNumSyncPoints() + index.intValue());
        _writer.println("// Total number of object sync points so far: " + _record.getProperty("numTotalSyncPoints"));
        try {
            if ((!force) && (null != recorderClassObject.owningThread())) {
                return false;
            } else {
                Field arrayField = recorderClass.fieldByName("_syncPoints");
                if (null == arrayField) {
                    throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._syncPoints field.");
                }
                Value arrayValue = recorderClass.getValue(arrayField);
                if (!(arrayValue instanceof ArrayReference)) {
                    throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._syncPoints.");
                }
                ArrayReference array = (ArrayReference) arrayValue;
                List<Value> values = array.getValues();
                for (int i = 0; i < index.intValue(); ++i) {
                    processSyncPointListEntry(values.get(i));
                }
            }
        } catch (IncompatibleThreadStateException e) {
            throw new Error("Could not access edu.rice.cs.cunit.SyncPointBuffer class lock.", e);
        }
        try {
            recorderClass.setValue(indexField, _record.getVM().mirrorOf(0));
        } catch (InvalidTypeException e) {
            throw new Error("Could not set edu.rice.cs.cunit._index to 0.", e);
        } catch (ClassNotLoadedException e) {
            throw new Error("Could not set edu.rice.cs.cunit._index to 0.", e);
        }
        _waitGraph = new WaitGraph(_threads, _writer);
        return true;
    }

    /**
     * Processes a synchronization point list entry.
     * @param entryValue entry value
     * @return translated synchronization point
     */
    public ISyncPoint.Translated processSyncPointListEntry(Value entryValue) {
        if (!(entryValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._syncPoints.elementData entry: " + entryValue);
        }
        ObjectReference entryObj = (ObjectReference) entryValue;
        ReferenceType entryObjType = entryObj.referenceType();
        if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadStartSyncPoint")) {
            return processThreadStartSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadExitSyncPoint")) {
            return processThreadExitSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadDestroySyncPoint")) {
            return processThreadDestroySyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadInterruptSyncPoint")) {
            return processThreadInterruptSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadEnterJoinSyncPoint")) {
            return processThreadEnterJoinSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadLeaveJoinSyncPoint")) {
            return processThreadLeaveJoinSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadSetPrioritySyncPoint")) {
            return processThreadSetPrioritySyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadEnterSleepSyncPoint")) {
            return processThreadEnterSleepSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadLeaveSleepSyncPoint")) {
            return processThreadLeaveSleepSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadStopSyncPoint")) {
            return processThreadStopSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadResumeSyncPoint")) {
            return processThreadResumeSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadSuspendSyncPoint")) {
            return processThreadSuspendSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadEnterYieldSyncPoint")) {
            return processThreadEnterYieldSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.thread.ThreadLeaveYieldSyncPoint")) {
            return processThreadLeaveYieldSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.object.ObjectEnterWaitSyncPoint")) {
            return processObjectEnterWaitSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.object.ObjectLeaveWaitSyncPoint")) {
            return processObjectLeaveWaitSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.object.ObjectNotifySyncPoint")) {
            return processObjectNotifySyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.object.ObjectNotifyAllSyncPoint")) {
            return processObjectNotifyAllSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedTryEnterBlockSyncPoint")) {
            return processSynchronizedTryEnterBlockSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedEnterBlockSyncPoint")) {
            return processSynchronizedEnterBlockSyncPoint(entryObjType, entryObj);
        } else if (entryObjType.name().equals("edu.rice.cs.cunit.record.syncPoints.sync.SynchronizedLeaveBlockSyncPoint")) {
            return processSynchronizedLeaveBlockSyncPoint(entryObjType, entryObj);
        } else {
            throw new RuntimeException("Unknown synchronization point");
        }
    }

    /**
     * Processes a thread start synchronization point
     * @param entryObjType entry
     * @return processed start synchronization point
     * @param entryObj
     */
    private ThreadStartSyncPoint.Translated processThreadStartSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Thread.start%s", ti.getThreadID(), System.getProperty("line.separator"));
        _writer.flush();
        _threads.put(ti.getThreadID(), ti);
        return new ThreadStartSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread exit synchronization point
     * @return processed exit synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadExitSyncPoint.Translated processThreadExitSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Thread.exit%s", ti.getThreadID(), System.getProperty("line.separator"));
        _writer.flush();
        _threads.remove(ti.getThreadID());
        return new ThreadExitSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread destroy synchronization point
     * @return processed destroy synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadDestroySyncPoint.Translated processThreadDestroySyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        return new ThreadDestroySyncPoint.Translated(ti);
    }

    /**
     * Processes a thread interrupt synchronization point
     * @return processed interrupt synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadInterruptSyncPoint.Translated processThreadInterruptSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        return new ThreadInterruptSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread join synchronization point
     * @return processed join synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadEnterJoinSyncPoint.Translated processThreadEnterJoinSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        Field timeOutField = entryObjType.fieldByName("_timeOut");
        if (null == timeOutField) {
            throw new Error("Could not find _timeOut field.");
        }
        Value timeOutValue = entryObj.getValue(timeOutField);
        if (!(timeOutValue instanceof LongValue)) {
            throw new Error("Unexpected type for _timeOut.");
        }
        return new ThreadEnterJoinSyncPoint.Translated(ti, ((LongValue) timeOutValue).longValue());
    }

    /**
     * Processes a thread join end synchronization point
     * @return processed join end synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadLeaveJoinSyncPoint.Translated processThreadLeaveJoinSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        Field timeOutField = entryObjType.fieldByName("_timeOut");
        if (null == timeOutField) {
            throw new Error("Could not find _timeOut field.");
        }
        Value timeOutValue = entryObj.getValue(timeOutField);
        if (!(timeOutValue instanceof LongValue)) {
            throw new Error("Unexpected type for _timeOut.");
        }
        return new ThreadLeaveJoinSyncPoint.Translated(ti, ((LongValue) timeOutValue).longValue());
    }

    /**
     * Processes a thread setPriority synchronization point
     * @return processed sleep synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadSetPrioritySyncPoint.Translated processThreadSetPrioritySyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        Field prioField = entryObjType.fieldByName("_newPriority");
        if (null == prioField) {
            throw new Error("Could not find _newPriority field.");
        }
        Value prioValue = entryObj.getValue(prioField);
        if (!(prioValue instanceof IntegerValue)) {
            throw new Error("Unexpected type for _newPriority.");
        }
        return new ThreadSetPrioritySyncPoint.Translated(ti, ((IntegerValue) prioValue).intValue());
    }

    /**
     * Processes a thread sleep synchronization point
     * @return processed sleep synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadEnterSleepSyncPoint.Translated processThreadEnterSleepSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        Field durField = entryObjType.fieldByName("_duration");
        if (null == durField) {
            throw new Error("Could not find _duration field.");
        }
        Value durValue = entryObj.getValue(durField);
        if (!(durValue instanceof LongValue)) {
            throw new Error("Unexpected type for _duration.");
        }
        _writer.printf("%5d: Thread.sleep(%d)%s", ti.getThreadID(), ((LongValue) durValue).longValue(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.setStatus(ThreadReference.THREAD_STATUS_SLEEPING);
        return new ThreadEnterSleepSyncPoint.Translated(ti, ((LongValue) durValue).longValue());
    }

    /**
     * Processes a thread sleep end synchronization point
     * @return processed sleep end synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadLeaveSleepSyncPoint.Translated processThreadLeaveSleepSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        Field durField = entryObjType.fieldByName("_duration");
        if (null == durField) {
            throw new Error("Could not find _duration field.");
        }
        Value durValue = entryObj.getValue(durField);
        if (!(durValue instanceof LongValue)) {
            throw new Error("Unexpected type for _duration.");
        }
        _writer.printf("%5d: Thread.sleep(%d) ended%s", ti.getThreadID(), ((LongValue) durValue).longValue(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.setStatus(ThreadReference.THREAD_STATUS_RUNNING);
        return new ThreadLeaveSleepSyncPoint.Translated(ti, ((LongValue) durValue).longValue());
    }

    /**
     * Processes a thread stop synchronization point
     *
     * @param entryObjType entry
     * @param entryObj
     *
     * @return processed stop synchronization point
     */
    private ThreadStopSyncPoint.Translated processThreadStopSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        return new ThreadStopSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread suspend synchronization point
     *
     * @param entryObjType entry
     * @param entryObj
     *
     * @return processed suspend synchronization point
     */
    private ThreadSuspendSyncPoint.Translated processThreadSuspendSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        return new ThreadSuspendSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread resume synchronization point
     *
     * @param entryObjType entry
     * @param entryObj
     *
     * @return processed resume synchronization point
     */
    private ThreadResumeSyncPoint.Translated processThreadResumeSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Thread.resume%s", ti.getThreadID(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.setStatus(ThreadReference.THREAD_STATUS_RUNNING);
        return new ThreadResumeSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread yield synchronization point
     * @return processed yield synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadEnterYieldSyncPoint.Translated processThreadEnterYieldSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Thread.yield%s", ti.getThreadID(), System.getProperty("line.separator"));
        _writer.flush();
        return new ThreadEnterYieldSyncPoint.Translated(ti);
    }

    /**
     * Processes a thread yield end synchronization point
     * @return processed yield end synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ThreadLeaveYieldSyncPoint.Translated processThreadLeaveYieldSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Thread.yield ended%s", ti.getThreadID(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.setStatus(ThreadReference.THREAD_STATUS_RUNNING);
        return new ThreadLeaveYieldSyncPoint.Translated(ti);
    }

    /**
     * Processes an object enter wait synchronization point
     * @return processed object enter wait synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ObjectEnterWaitSyncPoint.Translated processObjectEnterWaitSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        Field timeOutField = entryObjType.fieldByName("_timeOut");
        if (null == timeOutField) {
            throw new Error("Could not find _timeOut field.");
        }
        Value timeOutValue = entryObj.getValue(timeOutField);
        if (!(timeOutValue instanceof LongValue)) {
            throw new Error("Unexpected type for _timeOut.");
        }
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        LockInfo li = new LockInfo(objectRef);
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: Object.wait, lock = %d%s", ti.getThreadID(), li.getUniqueId(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null != tiOld) {
            tiOld.setStatus(ThreadReference.THREAD_STATUS_WAIT);
        }
        return new ObjectEnterWaitSyncPoint.Translated(li, ((LongValue) timeOutValue).longValue(), ti);
    }

    /**
     * Processes an object leave  wait synchronization point
     * @return processed object leave  wait synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ObjectLeaveWaitSyncPoint.Translated processObjectLeaveWaitSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        Field timeOutField = entryObjType.fieldByName("_timeOut");
        if (null == timeOutField) {
            throw new Error("Could not find _timeOut field.");
        }
        Value timeOutValue = entryObj.getValue(timeOutField);
        if (!(timeOutValue instanceof LongValue)) {
            throw new Error("Unexpected type for _timeOut.");
        }
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        LockInfo li = new LockInfo(objectRef);
        _writer.printf("%5d: Object.wait ended, lock = %d%s", ti.getThreadID(), li.getUniqueId(), System.getProperty("line.separator"));
        _writer.flush();
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null != tiOld) {
            tiOld.setStatus(ThreadReference.THREAD_STATUS_RUNNING);
        }
        return new ObjectLeaveWaitSyncPoint.Translated(li, ((LongValue) timeOutValue).longValue(), ti);
    }

    /**
     * Processes an object notify synchronization point
     * @return processed object notify synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ObjectNotifySyncPoint.Translated processObjectNotifySyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        return new ObjectNotifySyncPoint.Translated(new LockInfo(objectRef), new ThreadInfo(threadRef));
    }

    /**
     * Processes an object notify all synchronization point
     * @return processed object notify all synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private ObjectNotifyAllSyncPoint.Translated processObjectNotifyAllSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        return new ObjectNotifyAllSyncPoint.Translated(new LockInfo(objectRef), new ThreadInfo(threadRef));
    }

    /**
     * Processes a try enter block synchronization point
     * @return processedtry try enter block synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private SynchronizedTryEnterBlockSyncPoint.Translated processSynchronizedTryEnterBlockSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        LockInfo li = new LockInfo(objectRef);
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: try enter block, lock = %d%s", ti.getThreadID(), li.getUniqueId(), System.getProperty("line.separator"));
        _writer.flush();
        LockInfo liOld = _locks.get(li.getUniqueId());
        if (null == liOld) {
            _locks.put(li.getUniqueId(), li);
            liOld = li;
        }
        liOld.getWaitingThreadIds().add(ti.getThreadID());
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.setContendedLockId(li.getUniqueId());
        tiOld.setStatus(ThreadReference.THREAD_STATUS_MONITOR);
        return new SynchronizedTryEnterBlockSyncPoint.Translated(li, ti);
    }

    /**
     * Processes an enter block synchronization point
     * @return processedtry enter block synchronization point
     * @param entryObjType entry
     * @param entryObj
     */
    private SynchronizedEnterBlockSyncPoint.Translated processSynchronizedEnterBlockSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        LockInfo li = new LockInfo(objectRef);
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: enter block, lock = %d%s", ti.getThreadID(), li.getUniqueId(), System.getProperty("line.separator"));
        _writer.flush();
        LockInfo liOld = _locks.get(li.getUniqueId());
        if (null == liOld) {
            _locks.put(li.getUniqueId(), li);
            liOld = li;
        }
        liOld.getWaitingThreadIds().remove(ti.getThreadID());
        liOld.setOwningThreadId(ti.getThreadID());
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.getOwnedLockIDs().add(tiOld.getContendedLockID());
        tiOld.setContendedLockId(null);
        tiOld.setStatus(ThreadReference.THREAD_STATUS_RUNNING);
        return new SynchronizedEnterBlockSyncPoint.Translated(li, ti);
    }

    /**
     * Processes a leave block synchronization point
     * @return processedtry leave block synchronization point
     * @param entryObjType entry object type
     * @param entryObj
     */
    private SynchronizedLeaveBlockSyncPoint.Translated processSynchronizedLeaveBlockSyncPoint(ReferenceType entryObjType, ObjectReference entryObj) {
        Field objectField = entryObjType.fieldByName("_object");
        if (null == objectField) {
            throw new Error("Could not find _object field.");
        }
        Value objectValue = entryObj.getValue(objectField);
        if (!(objectValue instanceof ObjectReference)) {
            throw new Error("Unexpected type for _object: " + objectValue);
        }
        ObjectReference objectRef = (ObjectReference) objectValue;
        LockInfo li = new LockInfo(objectRef);
        Field threadField = entryObjType.fieldByName("_thread");
        if (null == threadField) {
            throw new Error("Could not find _thread field.");
        }
        Value threadValue = entryObj.getValue(threadField);
        if ((threadValue != null) && !(threadValue instanceof ThreadReference)) {
            throw new Error("Unexpected type for _thread: " + threadValue);
        }
        ThreadReference threadRef = (ThreadReference) threadValue;
        ThreadInfo ti = new ThreadInfo(threadRef);
        _writer.printf("%5d: leave block, lock = %d%s", ti.getThreadID(), li.getUniqueId(), System.getProperty("line.separator"));
        _writer.flush();
        LockInfo liOld = _locks.get(li.getUniqueId());
        if (null == liOld) {
            _locks.put(li.getUniqueId(), li);
            liOld = li;
        }
        liOld.setOwningThreadId(null);
        ThreadInfo tiOld = _threads.get(ti.getThreadID());
        if (null == tiOld) {
            _threads.put(ti.getThreadID(), ti);
            tiOld = ti;
        }
        tiOld.getOwnedLockIDs().remove(liOld.getUniqueId());
        return new SynchronizedLeaveBlockSyncPoint.Translated(li, ti);
    }

    /**
     * Processes the current synchronization point immediately.
     *
     * @param force
     * @return true if update was successful, false if it was delayed
     * @param recorderClass       class of the recorder
     * @param recorderClassObject class object of the recorder
     * @param methodDatabase      method database (key=class index/method index pair in "%08x %08x" format)
     *
     */
    public boolean processImmediately(boolean force, ClassType recorderClass, ClassObjectReference recorderClassObject, HashMap<String, String> methodDatabase) {
        return true;
    }

    /**
     * Return a map of threads. Key is the unique ID.
     * @return map of threads
     */
    public Map<Long, IThreadInfo> getThreads() {
        Map<Long, IThreadInfo> threads = new HashMap<Long, IThreadInfo>();
        for (Long key : _threads.keySet()) {
            ThreadInfo value = _threads.get(key);
            threads.put(key, value);
        }
        return threads;
    }

    /**
     * Returns list of list with cycles.
     * @return list of list with cycles
     */
    public List<List<IThreadInfo>> getCycles() {
        List<List<ThreadInfo>> wgloc = _waitGraph.getCycles();
        ArrayList<List<IThreadInfo>> listOfCycles = new ArrayList<List<IThreadInfo>>(wgloc.size());
        for (List<ThreadInfo> wgc : wgloc) {
            ArrayList<IThreadInfo> cycle = new ArrayList<IThreadInfo>(wgc);
            listOfCycles.add(cycle);
        }
        return listOfCycles;
    }
}
