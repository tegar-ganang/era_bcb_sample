package edu.rice.cs.cunit.record;

import com.sun.jdi.*;
import edu.rice.cs.cunit.SyncPointBuffer;
import edu.rice.cs.cunit.record.compactGraph.CompactThreadInfo;
import edu.rice.cs.cunit.record.compactGraph.CompactWaitGraph;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Processing for compact events with debug information.
 * @author Mathias Ricken
 */
public class CompactDebugProcessor implements ISyncPointProcessor {

    /**
     * Writer for output.
     */
    private PrintWriter _writer;

    /**
     * Record instance.
     */
    private Record _record;

    /**
     * Hashmap of threads, key is the thread ID.
     */
    private HashMap<Long, CompactThreadInfo> _threads;

    /**
     * Wait graph.
     */
    private CompactWaitGraph _waitGraph;

    /**
     * Method database, class index/method index pairs as keys.
     */
    private HashMap<String, String> _methodDatabase;

    /**
     * Create a new processor for verbose object events.
     * @param writer writer for output
     * @param record Record instance
     * @param methodDatabase method database
     */
    public CompactDebugProcessor(PrintWriter writer, Record record, HashMap<String, String> methodDatabase) {
        _writer = writer;
        _record = record;
        _threads = new HashMap<Long, CompactThreadInfo>();
        _methodDatabase = methodDatabase;
    }

    /**
     * Processes the compact synchronization points since the last request and then clears the list in the
     * debugged VM.
     * @param force true if processing should be forced, even if debugged VM is in the process of changing the list
     * @param recorderClass class of the recorder
     * @param recorderClassObject class object of the recorder
     * @param methodDatabase method database (key=class index/method index pair in "%08x %04x" format)
     * @return true if update was successful, false if it was delayed
     */
    public boolean process(boolean force, ClassType recorderClass, ClassObjectReference recorderClassObject, HashMap<String, String> methodDatabase) {
        Field compactIndexField = recorderClass.fieldByName("_compactIndex");
        if (null == compactIndexField) {
            throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._compactIndex field.");
        }
        Value compactIndexValue = recorderClass.getValue(compactIndexField);
        if (!(compactIndexValue instanceof IntegerValue)) {
            throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._compactIndex.");
        }
        IntegerValue compactIndex = (IntegerValue) compactIndexValue;
        int syncPointsInCompactList = compactIndex.intValue() / SyncPointBuffer.COMPACT_DEBUG_RECORD_SIZE;
        _record.setProperty("numSyncPointsInCompactList", syncPointsInCompactList);
        _record.setProperty("numTotalCompactSyncPoints", _record.getTotalNumCompactSyncPoints() + syncPointsInCompactList);
        _writer.println("// Total number of compact sync points so far: " + _record.getProperty("numTotalCompactSyncPoints"));
        try {
            if ((!force) && (null != recorderClassObject.owningThread())) {
                return false;
            } else {
                Field compactArrayField = recorderClass.fieldByName("_compactSyncPoints");
                if (null == compactArrayField) {
                    throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._compactSyncPoints field.");
                }
                Value compactArrayValue = recorderClass.getValue(compactArrayField);
                if (!(compactArrayValue instanceof ArrayReference)) {
                    throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._compactSyncPoints.");
                }
                ArrayReference compactArray = (ArrayReference) compactArrayValue;
                List<Value> compactValues = compactArray.getValues();
                for (int i = 0; i < compactIndex.intValue(); i += SyncPointBuffer.COMPACT_DEBUG_RECORD_SIZE) {
                    long tid = ((LongValue) compactValues.get(i)).longValue();
                    long code = ((LongValue) compactValues.get(i + 1)).longValue();
                    long classIndex = ((LongValue) compactValues.get(i + 2)).longValue();
                    long methodAndPC = ((LongValue) compactValues.get(i + 3)).longValue();
                    long oid = ((LongValue) compactValues.get(i + 4)).longValue();
                    processSyncPoint(tid, code, classIndex, methodAndPC, oid, methodDatabase);
                }
                _writer.flush();
            }
        } catch (IncompatibleThreadStateException e) {
            throw new Error("Could not access edu.rice.cs.cunit.SyncPointBuffer class lock.", e);
        }
        try {
            recorderClass.setValue(compactIndexField, _record.getVM().mirrorOf(0));
        } catch (InvalidTypeException e) {
            throw new Error("Could not set edu.rice.cs.cunit._compactIndex to 0.", e);
        } catch (ClassNotLoadedException e) {
            throw new Error("Could not set edu.rice.cs.cunit._compactIndex to 0.", e);
        }
        for (CompactThreadInfo cti : _threads.values()) {
            _writer.print("// Thread ");
            _writer.println(cti.toString());
        }
        _waitGraph = new CompactWaitGraph(_threads, _writer);
        _writer.flush();
        return true;
    }

    /**
     * Process a synchronization point.
     * @param tid thread ID
     * @param code code for the sync point
     * @param classIndex class index
     * @param methodAndPC method index and PC
     * @param oid object ID
     * @param methodDatabase method database to use
     */
    private void processSyncPoint(long tid, long code, long classIndex, long methodAndPC, long oid, HashMap<String, String> methodDatabase) {
        CompactThreadInfo cti = _threads.get(tid);
        if (cti == null) {
            cti = new CompactThreadInfo(tid, _methodDatabase);
        }
        cti.setLastClassIndex(classIndex);
        cti.setLastMethodIndexAndPC(methodAndPC);
        long methodIndex = (methodAndPC >>> 32);
        long methodPC = (methodAndPC & 0xFFFF);
        String methodAndPCString = "";
        if ((classIndex != 0) && (methodIndex != 0)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            pw.printf("%08x %04x", classIndex, methodIndex);
            pw.flush();
            String key = baos.toString();
            methodAndPCString = methodDatabase.get(key);
            if (methodAndPCString == null) {
                methodAndPCString = "";
            } else {
                methodAndPCString = " " + methodAndPCString + " PC=" + methodPC;
            }
        }
        String prefix = "";
        if ((((Boolean) _record.getProperty("mainEntered")) == false) || (((Integer) _record.getProperty("userThreads")) == 0)) {
            prefix = "// ";
        }
        _writer.printf("%s%d %d // %08x %04x %04x %10d%s%s", prefix, tid, code, classIndex, methodIndex, methodPC, oid, methodAndPCString, System.getProperty("line.separator"));
        if (code == SyncPointBuffer.SP.THREADSTART.intValue()) {
            int userThreads = ((Integer) _record.getProperty("userThreads")) + 1;
            _record.setProperty("userThreads", userThreads);
            _writer.println("// user thread with id " + tid + " started; " + userThreads + " remaining");
            cti.setStatus(CompactThreadInfo.STATUS.RUNNING);
        } else if (code == SyncPointBuffer.SP.THREADEXIT.intValue()) {
            int userThreads = ((Integer) _record.getProperty("userThreads")) - 1;
            _record.setProperty("userThreads", userThreads);
            _writer.println("// user thread with id " + tid + " exited; " + userThreads + " remaining");
            if (userThreads == 0) {
                _record.setAllUserThreadsDied();
            }
            cti.setStatus(CompactThreadInfo.STATUS.DEAD);
        } else if (code == SyncPointBuffer.SP.TRYMONITORENTER.intValue()) {
            if (oid != 0) {
                if (!cti.getOwnedLockIDs().keySet().contains(oid)) {
                    cti.setContendedLockID(oid);
                    cti.setStatus(CompactThreadInfo.STATUS.WAITING);
                }
            } else {
                _writer.println("// Warning: encountered TRYMONITORENTER with object ID = 0");
            }
        } else if (code == SyncPointBuffer.SP.MONITORENTER.intValue()) {
            if (oid != 0) {
                if (!cti.getOwnedLockIDs().keySet().contains(oid)) {
                    cti.getOwnedLockIDs().put(oid, 1L);
                    cti.setContendedLockID(null);
                    cti.setStatus(CompactThreadInfo.STATUS.RUNNING);
                } else {
                    long count = cti.getOwnedLockIDs().get(oid);
                    cti.getOwnedLockIDs().put(oid, count + 1);
                }
            } else {
                _writer.println("// Warning: encountered MONITORENTER with object ID = 0");
            }
        } else if (code == SyncPointBuffer.SP.MONITOREXIT.intValue()) {
            if (oid != 0) {
                long count = cti.getOwnedLockIDs().get(oid);
                if (count == 1) {
                    cti.getOwnedLockIDs().remove(oid);
                } else {
                    cti.getOwnedLockIDs().put(oid, count - 1);
                }
            } else {
                _writer.println("// Warning: encountered MONITOREXIT with object ID = 0");
            }
        }
        _threads.put(tid, cti);
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
        Field compactIndexField = recorderClass.fieldByName("_compactIndex");
        if (null == compactIndexField) {
            throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._compactIndex field.");
        }
        Value compactIndexValue = recorderClass.getValue(compactIndexField);
        if (!(compactIndexValue instanceof IntegerValue)) {
            throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._compactIndex.");
        }
        IntegerValue compactIndex = (IntegerValue) compactIndexValue;
        if (compactIndex.intValue() > 0) {
            _writer.println("Note: non-empty buffer while processing immediate sync point, transferring buffer first!");
            process(force, recorderClass, recorderClassObject, methodDatabase);
        }
        _record.setProperty("numSyncPointsInCompactList", 1);
        _record.setProperty("numTotalCompactSyncPoints", _record.getTotalNumCompactSyncPoints() + 1);
        Field immediateArrayField = recorderClass.fieldByName("_immediateTransfer");
        if (null == immediateArrayField) {
            throw new Error("Could not find edu.rice.cs.cunit.SyncPointBuffer._immediateTransfer field.");
        }
        Value immediateArrayValue = recorderClass.getValue(immediateArrayField);
        if (!(immediateArrayValue instanceof ArrayReference)) {
            throw new Error("Unexpected type for edu.rice.cs.cunit.SyncPointBuffer._immediateTransfer.");
        }
        ArrayReference immediateArray = (ArrayReference) immediateArrayValue;
        List<Value> compactValues = immediateArray.getValues();
        long tid = ((LongValue) compactValues.get(0)).longValue();
        long code = ((LongValue) compactValues.get(1)).longValue();
        long classIndex = ((LongValue) compactValues.get(2)).longValue();
        long methodAndPC = ((LongValue) compactValues.get(3)).longValue();
        long oid = ((LongValue) compactValues.get(4)).longValue();
        processSyncPoint(tid, code, classIndex, methodAndPC, oid, methodDatabase);
        _writer.flush();
        return true;
    }

    /**
     * Return a map of threads. Key is the unique ID.
     * @return map of threads
     */
    public Map<Long, IThreadInfo> getThreads() {
        Map<Long, IThreadInfo> threads = new HashMap<Long, IThreadInfo>();
        for (Long key : _threads.keySet()) {
            CompactThreadInfo value = _threads.get(key);
            threads.put(key, value);
        }
        return threads;
    }

    /**
     * Returns list of list with cycles.
     * @return list of list with cycles
     */
    public List<List<IThreadInfo>> getCycles() {
        List<List<CompactThreadInfo>> wgloc = _waitGraph.getCycles();
        ArrayList<List<IThreadInfo>> listOfCycles = new ArrayList<List<IThreadInfo>>(wgloc.size());
        for (List<CompactThreadInfo> wgc : wgloc) {
            ArrayList<IThreadInfo> cycle = new ArrayList<IThreadInfo>(wgc);
            listOfCycles.add(cycle);
        }
        return listOfCycles;
    }
}
