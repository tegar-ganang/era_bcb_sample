package gov.nasa.jpf.tools;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.StaticFieldInstruction;
import gov.nasa.jpf.search.Search;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Simple field access race detector example
 *
 * This implementation so far doesn't deal with synchronization via signals, it
 * only checks if the lockset intersections of reads and writes from different
 * threads get empty.
 *
 * To rule out false positives, we have to verify that there are at least
 * two paths leading to the conflict with a different order of the read/write
 * instructions
 *
 *  See also the PreciseRaceDetector, which requires less work and avoids
 *  these false positives
 */
public class RaceDetector extends PropertyListenerAdapter {

    /*** helper classes ***************************************************************/
    static class FieldAccess {

        ThreadInfo ti;

        Object[] locksHeld;

        Object[] lockCandidates;

        FieldInstruction finsn;

        FieldAccess prev;

        FieldAccess(ThreadInfo ti, FieldInstruction finsn) {
            this.ti = ti;
            this.finsn = finsn;
            LinkedList<ElementInfo> lockSet = ti.getLockedObjects();
            locksHeld = new Object[lockSet.size()];
            if (locksHeld.length > 0) {
                Iterator<ElementInfo> it = lockSet.iterator();
                for (int i = 0; it.hasNext(); i++) {
                    locksHeld[i] = it.next().toString();
                }
            }
        }

        <T> T[] intersect(T[] a, T[] b) {
            ArrayList<T> list = new ArrayList<T>(a.length);
            for (int i = 0; i < a.length; i++) {
                for (int j = 0; j < b.length; j++) {
                    if (a[i].equals(b[j])) {
                        list.add(a[i]);
                        break;
                    }
                }
            }
            return (list.size() == a.length) ? a : list.toArray(a.clone());
        }

        void updateLockCandidates() {
            if (prev == null) {
                lockCandidates = locksHeld;
            } else {
                lockCandidates = intersect(locksHeld, prev.lockCandidates);
            }
        }

        boolean hasLockCandidates() {
            return (lockCandidates.length > 0);
        }

        boolean isWriteAccess() {
            return ((finsn instanceof PUTFIELD) || (finsn instanceof PUTSTATIC));
        }

        FieldAccess getConflict() {
            boolean isWrite = isWriteAccess();
            for (FieldAccess c = prev; c != null; c = c.prev) {
                if ((c.ti != ti) && (isWrite != c.isWriteAccess())) {
                    return c;
                }
            }
            return null;
        }

        public boolean equals(Object other) {
            if (other instanceof FieldAccess) {
                FieldAccess that = (FieldAccess) other;
                if (this.ti != that.ti) return false;
                if (this.finsn != that.finsn) return false;
                return true;
            } else {
                return false;
            }
        }

        public int hashCode() {
            assert false : "hashCode not designed";
            return 42;
        }

        String describe() {
            String s = isWriteAccess() ? "write" : "read";
            s += " from thread: \"";
            s += ti.getName();
            s += "\", holding locks {";
            for (int i = 0; i < locksHeld.length; i++) {
                if (i > 0) s += ',';
                s += locksHeld[i];
            }
            s += "} in ";
            s += finsn.getSourceLocation();
            return s;
        }
    }

    static class FieldAccessSequence {

        String id;

        FieldAccess lastAccess;

        FieldAccessSequence(String id) {
            this.id = id;
        }

        void addAccess(FieldAccess fa) {
            fa.prev = lastAccess;
            lastAccess = fa;
            fa.updateLockCandidates();
        }

        void purgeLastAccess() {
            lastAccess = lastAccess.prev;
        }
    }

    /*** private fields and methods ****************************************/
    HashMap<String, FieldAccessSequence> fields = new HashMap<String, FieldAccessSequence>();

    Stack<ArrayList<FieldAccessSequence>> transitions = new Stack<ArrayList<FieldAccessSequence>>();

    ArrayList<FieldAccessSequence> pendingChanges;

    FieldAccessSequence raceField;

    ArrayList<FieldAccess> raceAccess1 = new ArrayList<FieldAccess>();

    ArrayList<FieldAccess> raceAccess2 = new ArrayList<FieldAccess>();

    String[] watchFields;

    boolean terminate;

    boolean verifyCycle;

    public RaceDetector(Config config) {
        watchFields = config.getStringArray("race.fields");
        terminate = config.getBoolean("race.terminate", true);
        verifyCycle = config.getBoolean("race.verify_cycle", false);
    }

    public void reset() {
        raceField = null;
    }

    boolean isWatchedField(FieldInstruction finsn) {
        if (watchFields == null) {
            return true;
        }
        String fname = finsn.getVariableId();
        for (int i = 0; i < watchFields.length; i++) {
            if (fname.matches(watchFields[i])) {
                return true;
            }
        }
        return false;
    }

    /*** GenericProperty **************************************************/
    public boolean check(Search search, JVM vm) {
        return (raceField == null);
    }

    public String getErrorMessage() {
        return ("potential field race: " + raceField.id);
    }

    /*** SearchListener ****************************************************/
    public void stateAdvanced(Search search) {
        transitions.push(pendingChanges);
        pendingChanges = null;
    }

    public void stateBacktracked(Search search) {
        ArrayList<FieldAccessSequence> fops = transitions.pop();
        if (fops != null) {
            for (FieldAccessSequence fs : fops) {
                fs.purgeLastAccess();
            }
        }
    }

    /*** VMListener *******************************************************/
    public void instructionExecuted(JVM jvm) {
        Instruction insn = jvm.getLastInstruction();
        if (insn instanceof FieldInstruction) {
            ThreadInfo ti = jvm.getLastThreadInfo();
            FieldInstruction finsn = (FieldInstruction) insn;
            String id = null;
            if (raceField != null) {
                return;
            }
            if (ti.hasOtherRunnables() && isWatchedField(finsn)) {
                if (finsn instanceof StaticFieldInstruction) {
                    if (finsn.getMethodInfo().isClinit(finsn.getFieldInfo().getClassInfo())) {
                        return;
                    }
                    id = finsn.getVariableId();
                } else {
                    ElementInfo ei = ((InstanceFieldInstruction) insn).getLastElementInfo();
                    if ((ei != null) && ei.isShared()) {
                        id = finsn.getId(ei);
                    }
                }
                if (id != null) {
                    FieldAccessSequence fs = fields.get(id);
                    if (fs == null) {
                        fs = new FieldAccessSequence(id);
                        fields.put(id, fs);
                    }
                    FieldAccess fa = new FieldAccess(ti, finsn);
                    fs.addAccess(fa);
                    if (pendingChanges == null) {
                        pendingChanges = new ArrayList<FieldAccessSequence>(5);
                    }
                    pendingChanges.add(fs);
                    if (!fa.hasLockCandidates()) {
                        FieldAccess conflict = fa.getConflict();
                        if (conflict != null) {
                            if (verifyCycle) {
                                int idx = raceAccess1.indexOf(conflict);
                                if ((idx >= 0) && (fa.equals(raceAccess2.get(idx)))) {
                                    if (terminate) {
                                        raceField = fs;
                                    }
                                    System.err.println("race detected (access occurred in both orders): " + fs.id);
                                    System.err.println("\t" + fa.describe());
                                    System.err.println("\t" + conflict.describe());
                                } else {
                                    raceAccess1.add(fa);
                                    raceAccess2.add(conflict);
                                }
                            } else {
                                if (terminate) {
                                    raceField = fs;
                                }
                                System.err.println("potential race detected: " + fs.id);
                                System.err.println("\t" + fa.describe());
                                System.err.println("\t" + conflict.describe());
                            }
                        }
                    }
                }
            }
        }
    }
}
