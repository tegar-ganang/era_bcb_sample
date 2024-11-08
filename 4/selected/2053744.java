package net.sf.openforge.lim.memory;

import java.util.*;
import net.sf.openforge.app.*;
import net.sf.openforge.lim.*;

/**
 * LogicalMemoryPort is an access point for a {@link LogicalMemory}, which may
 * have one or more instances of it.  Each LogicalMemoryPort supports one
 * concurrent access to the {@link LogicalMemory} which owns it.
 * <P>
 * Every {@link LValue} is associated with exactly one LogicalMemoryPort.
 * This association is established with {@link #addAccess(LValue,Location};
 * all further accesses for that {@link LValue} must be made with the
 * same LogicalMemoryPort, until either the LogicalMemoryPort or the
 * {@link LValue} is removed from the {@link LogicalMemory}.
 * <P>
 * <i>This class temporarily extends MemoryPort until the Memory class is</i>
 * <i>no longer used.</i>
 *
 * @version $Id: LogicalMemoryPort.java 538 2007-11-21 06:22:39Z imiller $
 */
public class LogicalMemoryPort extends Storage implements Arbitratable {

    public static final String WRITE_FIRST = "WRITE_FIRST";

    public static final String READ_FIRST = "READ_FIRST";

    public static final String NO_CHANGE = "NO_CHANGE";

    /** Revision */
    private static final String _RCS_ = "$Rev: 538 $";

    /** The owner of this port */
    private LogicalMemory logicalMemory;

    /** Set to true if this memory port is arbitrated. */
    private boolean arbitrated = false;

    /** the MemoryReferee that referees accesses to this LogicalMemoryPort */
    private MemoryReferee referee;

    private boolean doSimpleArbiter = false;

    /**
     * Constructs a new LogicalMemoryPort.  This method is normally called
     * only from {@link LogicalMemory}.
     *
     * @param logicalMemory the memory to which this port belongs
     * @throws NullPointerException if <code>logicalMemory</code> is null
     */
    LogicalMemoryPort(LogicalMemory logicalMemory) {
        super();
        if (logicalMemory == null) {
            throw new NullPointerException("null logicalMemory arg");
        }
        this.logicalMemory = logicalMemory;
        this.doSimpleArbiter = EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.SIMPLE_STATE_ARBITRATION);
    }

    /**
     * Gets the {@link LogicalMemory}.
     *
     * @return the memory which owns this port
     */
    public LogicalMemory getLogicalMemory() {
        return logicalMemory;
    }

    /**
     * Tests the referencer types for compatibility and then returns 1
     * always.
     *
     * @param from the prior accessor in source document order.
     * @param to the latter accessor in source document order.
     */
    public int getSpacing(Referencer from, Referencer to) {
        if ((from instanceof MemoryRead) || (from instanceof MemoryWrite)) {
            if (((MemoryAccess) from).getLatency().getMinClocks() == 0) {
                return 1;
            } else {
                return 0;
            }
        } else throw new IllegalArgumentException("Source access to " + this + " is of unknown type " + from.getClass());
    }

    /**
     * Returns -1 indicating that the referencers must be scheduled
     * using the default DONE to GO spacing.
     */
    public int getGoSpacing(Referencer from, Referencer to) {
        return -1;
    }

    /**
     * Returns true if this memory port is going to be arbitrated and
     * consequently have indeterminate timing in the final
     * implementation. 
     */
    public boolean isArbitrated() {
        return this.arbitrated;
    }

    /**
     * Returns the write mode for this port.  Always returns
     * READ_FIRST.  LUT based memories are always read first, only
     * block rams have the capability of selection of mode.  Thus, if
     * this method returns anything other than READ_FIRST, then the
     * code allocating dual ports, and the resource sequencer must
     * account for the difference between LUT and block rams (and the
     * decision on which this memory is must be made much earlier).
     *
     * @return {@see #READ_FIRST}
     */
    public String getWriteMode() {
        return this.READ_FIRST;
    }

    /**
     * Sets whether this memory port is to be arbitrated.
     *
     * @param value true if this memory port needs to be arbitrated.
     */
    public void setArbitrated(boolean value) {
        this.arbitrated = value;
    }

    /**
     * Gets the MemoryReferee for this port (created with makePhysicalComponent).
     */
    public MemoryReferee getReferee() {
        return referee;
    }

    /**
     * Makes the MemoryReferee which provides the physical implementation
     * for access to this MemoryPort.
     *
     * @param readList list with MemoryReadConnections for each task.
     * @param writeList list with MemoryReadConnections for each task. 
     * null reads or writes mean there is no read or write for that task
     */
    public MemoryReferee makePhysicalComponent(List readList, List writeList) {
        if (this.doSimpleArbiter) {
            this.referee = new SimpleMemoryReferee(this, readList, writeList);
        } else {
            this.referee = new MemoryReferee(this, readList, writeList);
        }
        return referee;
    }

    /**
     * Records an access through this port.  An access consists of an
     * {@link LValue} and the {@link Location} it references.  A
     * given {@link Location} will only be recorded once for an {@link LValue}.
     * An {@link LValue} may only have accesses to one LogicalMemoryPort at
     * a time.
     *
     * @param lvalue the {@link LValue} that denotes the access
     * @param location the {@link Location} accessed by <code>lvalue</code>
     *
     * @throws NullPointerException if <code>lvalue</code> or <code>location</code>
     *           is null
     * @throws IllegalArgumentException if <code>location</code> does not refer
     *           to this port's {@link LogicalMemory}, or if <code>lvalue</code>
     *           already accesses the memory through a different LogicalMemoryPort
     */
    public void addAccess(LValue lvalue, Location location) {
        getLogicalMemory().addAccess(lvalue, location, this);
        lvalue.setLogicalMemoryPort(this);
    }

    public void addAccess(LValue lvalue) {
        getLogicalMemory().addAccess(lvalue, this);
        lvalue.setLogicalMemoryPort(this);
    }

    /**
     * Gets the accesses which read from this port.
     *
     * @return a collection of {@link LValue LValues} which read from this port
     */
    public List getReadAccesses() {
        return getLogicalMemory().getReadLValues(this);
    }

    /**
     * Gets the accesses which write to this port.
     *
     * @return a collection of {@link LValue LValues} which write to port
     */
    public List getWriteAccesses() {
        return getLogicalMemory().getWriteLValues(this);
    }

    /**
     * Tests whether there are any write accesses to this port.
     *
     * @return false if there are write accesses to this port, true otherwise
     */
    public boolean isReadOnly() {
        return getWriteAccesses().isEmpty();
    }

    /**
     * Tests whether there are any read accesses to this port.
     *
     * @return false if there are read accesses to this port, true otherwise
     */
    public boolean isWriteOnly() {
        return getReadAccesses().isEmpty();
    }

    /**
     * Gets the latency of this memory port which is dependent on
     * whether the port is arbitrated or not.  If it is arbitrated the
     * latency is an open one, otherwise it is a fixed one cycle.
     *
     * @param exit is an {@link Exit} which is used to find the type
     * of the access by getting the owner of the exit.
     */
    public Latency getLatency(Exit exit) {
        Component owner = exit.getOwner();
        if (owner instanceof MemoryAccessBlock) {
            return getLatency(((MemoryAccessBlock) owner).getMemoryAccess());
        } else {
            return getLatency((MemoryAccess) exit.getOwner());
        }
    }

    /**
     * Gets the maximum possible number of bits in an address for
     * this port's {@link LogicalMemory}.
     *
     * @return the nonzero number of bits
     */
    public int getMaxAddressWidth() {
        return getLogicalMemory().getMaxAddressWidth();
    }

    private Latency getLatency(MemoryAccess access) {
        Latency lat = null;
        final MemoryImplementation impl = getLogicalMemory().getImplementation();
        if (access.isReadAccess()) {
            lat = (impl != null) ? impl.getReadLatency() : null;
        } else if (access.isWriteAccess()) {
            lat = (impl != null) ? impl.getWriteLatency() : null;
        } else {
            throw new IllegalArgumentException("Unknown access type to memory port " + access);
        }
        if ((lat != null) && isArbitrated() && !doSimpleArbiter) {
            lat = lat.open(access.getExit(Exit.DONE));
        }
        return lat;
    }

    public StructuralMemory getStructuralMemory() {
        return getLogicalMemory().getStructuralMemory();
    }

    /**
     * Removes a reference from this memory port.
     * ABKTODO: This used to be removeReference(Reference), but
     * read/write accesses are LValues though not necessarily References,
     * and are added as LValues (not References), so this has been
     * changed to removeAccess(LValue) to be more consistent. 
     *
     * @param lvalue the reference to be removed; should be an {@link LValue}
     *          that accesses this port
     * @throws NullPointerException if <code>lvalue</code> is null
     * @throws IllegalArgumentException if <code>lvalue</code> is not known
     *           to this memory port
     */
    public void removeAccess(LValue lvalue) {
        if (lvalue == null) {
            throw new NullPointerException("null lvalue arg");
        }
        if (getLogicalMemory().getLogicalMemoryPort(lvalue) != this) {
            throw new IllegalArgumentException("unknown lvalue arg");
        }
        getLogicalMemory().removeAccessor(lvalue);
    }

    public int getDataPathWidth() {
        return getStructuralMemory().getDataWidth();
    }

    public int getAddrPathWidth() {
        return getStructuralMemory().getAddrWidth();
    }

    public boolean isAddressable() {
        return getStructuralMemory().getAddressableLocations() > 1;
    }

    public boolean allowsCombinationalReads() {
        return getStructuralMemory().allowsCombinationalReads();
    }
}
