package net.sf.openforge.lim;

import java.util.*;
import java.math.BigInteger;
import net.sf.openforge.app.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.optimize.constant.TwoPassPartialConstant;
import net.sf.openforge.util.naming.ID;

/**
 * A single register memory.  The Register object is an atomic element
 * accessed by {@link RegisterRead} and {@link RegisterWrite} lim
 * components, and <b>is always considered to be unsigned</b>.  It is
 * up to the accessing context to correctly cast the read/write values
 * from/to unsigned as needed.  To facilitate this convertion the
 * methods {@link createWriteAccess} and {@link createReadAccess}
 * have been made available.  After scheduling, and during
 * GlobalConnection, the Register is implemented in a Physical module
 * by instantiating a Reg object with the correct attributes.
 *
 * @author  Stephen Edwards
 * @version $Id: Register.java 538 2007-11-21 06:22:39Z imiller $
 */
public class Register extends Storage implements StateHolder, Arbitratable {

    private static final String rcs_id = "RCS_REVISION: $Rev: 538 $";

    /** True if this is a volatile register */
    private boolean isVolatile = false;

    /** Collection of RegisterRead */
    private Collection reads = new HashSet(11);

    /** Collection of RegisterWrite */
    private Collection writes = new HashSet(11);

    /** This Component (either a plain Register.Physical or a RegisterReferee)
     * maintains physical connections for the Register. */
    private Module registerComponent = null;

    /** The width to which this Register was constructed. */
    private final int initWidth;

    /** The initial value of this Register. */
    private LogicalValue initialValue = null;

    /** The endianness swapper modules for Big Endian, comes in a pair */
    private EndianSwapper inputSwapper = null;

    private EndianSwapper outputSwapper = null;

    /**
     * Constructs a new Register with the specified width
     *
     * @param init, the {@link LogicalValue} which specifies the
     * initial value to be used for this Register.  May be null.
     * @param width the bit width of the register
     * @param isVolatile true if this is a volatile register
     * @throws IllegalArgumentException if width < 1.
     */
    public Register(LogicalValue init, int width, boolean isVolatile) {
        super();
        this.initialValue = init;
        this.isVolatile = isVolatile;
        if (width < 1) {
            throw new IllegalArgumentException("Illegal initial width specified for register: " + width);
        }
        this.initWidth = width;
    }

    public void addEndianSwappers(EndianSwapper front, EndianSwapper back) {
        this.inputSwapper = front;
        this.outputSwapper = back;
    }

    public EndianSwapper getInputSwapper() {
        return this.inputSwapper;
    }

    public EndianSwapper getOutputSwapper() {
        return this.outputSwapper;
    }

    /**
     * Returns the LogicalValue which specifies the initial value of
     * this Register, or null if none has been specified.
     *
     * @return a {@link LogicalValue} or null.
     */
    public LogicalValue getInitialValue() {
        return this.initialValue;
    }

    /**
     * Returns false, the implementation of the Register is ALWAYS
     * unsigned, but every read and write access contains a CastOp to
     * convert from the unsigned backing to the correct type Value for
     * the access.  {@see RegisterAccessBlock}.  This is a static
     * method because it holds true for all Register objects.
     *
     * @return a false
     */
    public static boolean isSigned() {
        return false;
    }

    /**
     * NOT IMPLEMENTED <strike>Returns true if the initial value of this register is a
     * primitive value and is floating point</strike>
     */
    public boolean isFloat() {
        return false;
    }

    /**
     * Returns the width of this Register that was specified at the
     * time of its construction.
     *
     * @return a non-negative 'int'
     */
    public int getInitWidth() {
        return this.initWidth;
    }

    /**
     * Tests the referencer types for compatibility and then returns 0
     * always. 
     *
     * @param from the prior accessor in source 'document' order.
     * @param to the latter accessor in source 'document' order.
     */
    public int getSpacing(Referencer from, Referencer to) {
        if (from instanceof RegisterWrite) return 1;
        return 0;
    }

    /**
     * Returns -1 indicating that the referencers must be scheduled
     * using the default DONE to GO spacing.
     */
    public int getGoSpacing(Referencer from, Referencer to) {
        return -1;
    }

    public String cpDebug(boolean verbose) {
        String ret = "initial width: " + getInitWidth();
        return ret;
    }

    /**
     * Constructs the physial implementation of this Register,
     * including the backing register (flops) and any write-side
     * arbitration logic that is needed.  The returned
     * {@link Component} is of type Register.Physical
     *
     * @param readers a list of the readers of this register, null
     * indicates a 'slot' which is not a reader (but may line up with
     * a write access in the writers list).
     * @param writers a list of the (arbitratable) writers of this
     * register, null indicates a 'slot' which is not a writer but may
     * be a reader.
     */
    public Component makePhysicalComponent(List readers, List writers) {
        String logicalId = showIDLogical();
        this.registerComponent = new Physical(readers, writers, logicalId);
        registerComponent.setIDLogical(logicalId);
        return registerComponent;
    }

    /**
     * Gets the {@link Module} that maintains this Register connections.
     * May either be a {@link RegisterReferee} or a {@link Register.Physical}.
     */
    public Module getPhysicalComponent() {
        return this.registerComponent;
    }

    /**
     * Returns a fixed latency of ZERO because all accesses are
     * guaranteed to succeed and the resource dependencies will ensure
     * that multiple accesses are seperated by a cycle.
     */
    public Latency getLatency(Exit exit) {
        return Latency.ZERO;
    }

    public Collection getReferences() {
        Collection list = new ArrayList(reads.size() + writes.size());
        list.addAll(reads);
        list.addAll(writes);
        return list;
    }

    /**
     * Makes a new {@link RegisterAccessBlock#RegisterReadBlock} that
     * can be used to correctly access this Register and preserves the
     * signedness of the access through the register.  This method is
     * the preferred method of generating accesses to this Register.
     *
     * @param signed a 'boolean', true if the access is a signed access
     * @return a value of type 'RegisterAccessBlock.RegisterReadBlock'
     */
    public RegisterAccessBlock.RegisterReadBlock createReadAccess(boolean signed) {
        return new RegisterAccessBlock.RegisterReadBlock(makeReadAccess(signed));
    }

    /**
     * Makes a new {@link RegisterAccessBlock#RegisterWriteBlock} that
     * can be used to correctly access this Register and preserves the
     * signedness of the access through the register.  This method is
     * the preferred method of generating accesses to this Register.
     *
     * @param signed a 'boolean', true if the access is a signed access
     * @return a value of type 'RegisterAccessBlock.RegisterWriteBlock'
     */
    public RegisterAccessBlock.RegisterWriteBlock createWriteAccess(boolean signed) {
        return new RegisterAccessBlock.RegisterWriteBlock(makeWriteAccess(signed));
    }

    /**
     * makes a new atomic register read to access this Register, this
     * method should not be generally used {@see createReadAccess}
     *
     * @param isSigned a boolean, true if this read is a signed access.
     * @return a value of type 'RegisterRead'
     */
    RegisterRead makeReadAccess(boolean isSigned) {
        RegisterRead read = new RegisterRead(this, isSigned);
        reads.add(read);
        return read;
    }

    /**
     * makes a new atomic register write to access this Register, this
     * method should not be generally used {@see createWriteAccess}
     *
     * @param isSigned a boolean, true if this write is a signed access.
     * @return a value of type 'RegisterWrite'
     */
    RegisterWrite makeWriteAccess(boolean isSigned) {
        RegisterWrite write = new RegisterWrite(this, isSigned);
        writes.add(write);
        return write;
    }

    public Collection getReadAccesses() {
        return Collections.unmodifiableCollection(reads);
    }

    public Collection getWriteAccesses() {
        return Collections.unmodifiableCollection(writes);
    }

    public void removeReference(Reference ref) {
        if (!(reads.remove(ref) || writes.remove(ref))) {
            throw new IllegalArgumentException("unknown access");
        }
    }

    public boolean isVolatile() {
        return isVolatile;
    }

    /**
     * Clones this register, clearing out the sets of stored reads and
     * writes as well as cloning the underlying register component.
     *
     * @return a Register object.
     * @exception CloneNotSupportedException if an error occurs
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public String toString() {
        String ret = super.toString();
        ret = ret.replaceAll("net.sf.openforge.lim.", "");
        return ret + "=<" + getInitialValue() + ">";
    }

    public int getDataPathWidth() {
        return getInitWidth();
    }

    public int getAddrPathWidth() {
        return 32;
    }

    public boolean isAddressable() {
        return false;
    }

    public boolean allowsCombinationalReads() {
        return true;
    }

    /**
     * Encapsulates the physical implementation of the Register.  This
     * class is a Module that contains a {@link Reg} object,
     * initialized to the correct value and maps the ports/bus of the
     * {@link Reg} to the connections necessary for connecting to the
     * entry function.
     */
    public class Physical extends PhysicalImplementationModule {

        Port enable;

        Port data;

        Bus registerOutput;

        /**
         * Builds the physical implementation of a Register.  NOTE:
         * If/when sized accesses are allowed to Registers we need to
         * create logic similar to the logic in StructuralMemory for
         * managing the input data (write data).  READ LOGIC must be
         * implemented at each accessor, NOT the global level, so that
         * we can still allow multiple parallel reads.
         */
        private Physical(List readers, List writers, String logicalId) {
            super(0);
            final boolean isLittleEndian = EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.LITTLE_ENDIAN);
            final boolean doSimpleMerge = EngineThread.getGenericJob().getUnscopedBooleanOptionValue(OptionRegistry.SIMPLE_STATE_ARBITRATION);
            setIDLogical(logicalId);
            getResetPort().setUsed(true);
            setConsumesReset(true);
            getClockPort().setUsed(true);
            setConsumesClock(true);
            for (Iterator iter = writers.iterator(); iter.hasNext(); ) {
                if (iter.next() != null) {
                    Port enablePort = makeDataPort();
                    enablePort.setUsed(true);
                    enablePort.getPeer().setSize(1, false);
                    Port dataPort = makeDataPort();
                    dataPort.setUsed(true);
                    dataPort.getPeer().setSize(Register.this.getInitWidth(), Register.this.isSigned());
                }
            }
            BigInteger initValue = AddressableUnit.getCompositeValue(getInitialValue().getRep(), getInitialValue().getAddressStridePolicy());
            Constant initConstant = new SimpleConstant(initValue, Register.this.getInitWidth(), Register.this.isSigned());
            Bus dataSource = null;
            EndianSwapper inSwapper = null;
            EndianSwapper outSwapper = null;
            if (writers.size() > 0) {
                Reg reg = Reg.getConfigurableReg(Reg.REGRE, logicalId);
                reg.getResultBus().setSize(getInitWidth(), false);
                Value init = initConstant.getValueBus().getValue();
                reg.setInitialValue(init);
                reg.getClockPort().setBus(getClockPort().getPeer());
                reg.getResetPort().setBus(getResetPort().getPeer());
                reg.getInternalResetPort().setBus(getResetPort().getPeer());
                if (writers.size() == 1) {
                    assert this.getDataPorts().size() == 2;
                    reg.getEnablePort().setBus(((Port) getDataPorts().get(0)).getPeer());
                    reg.getDataPort().setBus(((Port) getDataPorts().get(1)).getPeer());
                } else {
                    if (doSimpleMerge) {
                        Mux mux = new Mux(writers.size());
                        Or or = new Or(writers.size());
                        Iterator dataMuxPorts = mux.getGoPorts().iterator();
                        Iterator writeOrPorts = or.getDataPorts().iterator();
                        Iterator physicalPorts = getDataPorts().iterator();
                        for (int i = 0; i < writers.size(); i++) {
                            Bus enable = ((Port) physicalPorts.next()).getPeer();
                            Bus data = ((Port) physicalPorts.next()).getPeer();
                            Port dataMuxGoPort = (Port) dataMuxPorts.next();
                            dataMuxGoPort.setBus(enable);
                            Port dataMuxDataPort = mux.getDataPort(dataMuxGoPort);
                            dataMuxDataPort.setBus(data);
                            ((Port) writeOrPorts.next()).setBus(enable);
                        }
                        addComponent(mux);
                        addComponent(or);
                        reg.getDataPort().setBus(mux.getResultBus());
                        reg.getEnablePort().setBus(or.getResultBus());
                    } else {
                        assert false : "Not supporting multiple writers in arbitrated register case.  See comment";
                        RegisterReferee referee = new RegisterReferee(Register.this, readers, writers);
                        addComponent(referee);
                        referee.connectImplementation(reg, this.getDataPorts());
                        addFeedbackPoint(reg);
                        addFeedbackPoint(referee);
                    }
                }
                if (!isLittleEndian && (getInitWidth() > 8)) {
                    inSwapper = new EndianSwapper(getInitWidth(), getInitialValue().getAddressStridePolicy().getStride());
                    inSwapper.getInputPort().setBus(reg.getDataPort().getBus());
                    reg.getDataPort().setBus(inSwapper.getOutputBus());
                    addComponent(inSwapper);
                }
                addComponent(reg);
                dataSource = reg.getResultBus();
            } else {
                addComponent(initConstant);
                dataSource = initConstant.getValueBus();
            }
            if (!isLittleEndian && (getInitWidth() > 8)) {
                outSwapper = new EndianSwapper(getInitWidth(), getInitialValue().getAddressStridePolicy().getStride());
                outSwapper.getInputPort().setBus(dataSource);
                dataSource = outSwapper.getOutputBus();
                addComponent(outSwapper);
            }
            this.registerOutput = makeExit(0).makeDataBus();
            this.registerOutput.setSize(getInitWidth(), isSigned());
            this.registerOutput.getPeer().setBus(dataSource);
        }

        public String toString() {
            String busWidth = (registerOutput == null ? "null" : Integer.toString(registerOutput.getWidth()));
            String r = "Register.Physical " + Integer.toHexString(hashCode()) + " result bus width: " + busWidth;
            return r;
        }

        public Bus getRegisterOutput() {
            return this.registerOutput;
        }

        public boolean isOpaque() {
            return true;
        }

        public void accept(Visitor v) {
            throw new UnexpectedVisitationException();
        }

        public boolean removeDataBus(Bus bus) {
            assert false : "remove data bus not supported on " + this;
            return false;
        }

        public boolean removeDataPort(Port port) {
            assert false : "remove data port not supported on " + this;
            return false;
        }
    }
}
