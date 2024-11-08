package fluid.version;

import java.io.IOException;
import java.util.Enumeration;
import fluid.ir.IRCompoundType;
import fluid.ir.IRInput;
import fluid.ir.IRLocation;
import fluid.ir.IROutput;
import fluid.ir.IRSequence;
import fluid.ir.IRSequenceWrapper;
import fluid.ir.InsertionPoint;

class VersionedSequence extends IRSequenceWrapper {

    protected VersionedStructure structure;

    VersionedSequence(IRSequence base) {
        super(base);
        structure = VersionedStructureFactory.getVS();
    }

    public void setVS(VersionedStructure vs) {
        structure = vs;
    }

    protected boolean checkVS() {
        if (structure != null) {
            Version v = Version.getVersionLocal();
            return structure.isDefined(v);
        }
        return true;
    }

    protected void assertVS(boolean isWrite) {
        while (!checkVS()) {
            Version v = Version.getVersionLocal();
            new SlotUnknownVersionException(isWrite ? "not loaded for write" : "not loaded for read", null, v).handle();
        }
    }

    protected void informVS() {
        if (structure != null) {
            structure.noteChange(Version.getVersionLocal());
        }
    }

    public boolean validAt(IRLocation loc) {
        return checkVS() && super.validAt(loc);
    }

    public Object elementAt(IRLocation loc) {
        assertVS(false);
        return super.elementAt(loc);
    }

    public Enumeration elements() {
        assertVS(false);
        return super.elements();
    }

    public void setElementAt(Object e, IRLocation loc) {
        assertVS(true);
        super.setElementAt(e, loc);
        informVS();
    }

    public IRLocation insertElementAt(Object e, InsertionPoint ip) {
        assertVS(true);
        IRLocation loc = super.insertElementAt(e, ip);
        informVS();
        return loc;
    }

    public void removeElementAt(IRLocation loc) {
        assertVS(true);
        super.removeElementAt(loc);
        informVS();
    }

    public void writeValue(IROutput out) throws IOException {
        if (structure == null) structure = VersionedStructureFactory.getVS();
        sequence.writeValue(out);
    }

    public void writeContents(IRCompoundType t, IROutput out) throws IOException {
        if (structure == null) structure = VersionedStructureFactory.getVS();
        sequence.writeContents(t, out);
    }

    public void readContents(IRCompoundType t, IRInput in) throws IOException {
        if (structure == null) structure = VersionedStructureFactory.getVS();
        sequence.readContents(t, in);
    }

    public boolean isChanged() {
        return sequence.isChanged();
    }

    public void writeChangedContents(IRCompoundType t, IROutput out) throws IOException {
        sequence.writeChangedContents(t, out);
    }

    public void readChangedContents(IRCompoundType t, IRInput in) throws IOException {
        sequence.readChangedContents(t, in);
    }
}
