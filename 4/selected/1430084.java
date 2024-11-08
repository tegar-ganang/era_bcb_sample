package net.sf.openforge.lim.memory;

import java.util.*;
import net.sf.openforge.lim.*;
import net.sf.openforge.lim.op.*;
import net.sf.openforge.util.naming.*;

/**
 * MemoryReferee is a {@link Module} which controls access to a {@link
 * MemoryPort}.
 *
 * when multiple {@link Task tasks} attempt to access the port, the go(s)
 * from the task (local) side are propagated to the MemoryPort (global) side
 * (and the global to local done) as follows:
 * <p>
 * <img src="../doc-files/MemoryReferee-GlobalMux.png">
 * <p>
 * where the task information is registered according to:
 * <p> <img src="../doc-files/MemoryReferee-TaskCapture.png">
 * <p>
 * and the state is updated according to:
 * <p> <img src="../doc-files/MemoryReferee-StateMachine.png"> <p>
 *
 * @author Jim Jensen based on design by Ian Miller and Jonathan Harris
 *
 * @version $Id: MemoryReferee.java 280 2006-08-11 17:00:32Z imiller $
 */
public class MemoryReferee extends Referee {

    private static final String _RCS_ = "$Rev: 280 $";

    private List taskSlots = new ArrayList();

    private GlobalSlot globalSlot;

    /** A Set of Components which represent the points of feedback
     * in this MemoryReferee, populated from the sets contained in
     * TaskCapture and StateMachine */
    Set feedbackPoints = new HashSet(11);

    Constant zeroConstant;

    Constant zeroDataConstant;

    Constant oneConstant;

    private int dataWidth = -1;

    protected MemoryReferee(Arbitratable resource) {
        super();
        this.dataWidth = resource.getDataPathWidth();
        Exit mainExit = makeExit(0, Exit.DONE);
        this.globalSlot = new GlobalSlot(this);
    }

    public MemoryReferee(Arbitratable resource, List readList, List writeList) {
        this(resource);
        assert readList.size() == writeList.size() : "readList must match writeList size";
        int numTaskSlots = readList.size();
        final int memoryWidth = resource.getDataPathWidth();
        final int addressWidth = resource.getAddrPathWidth();
        final boolean isAddressable = resource.isAddressable();
        final boolean combinationalMemoryReads = resource.allowsCombinationalReads();
        this.globalSlot.setSizes(memoryWidth, addressWidth, LogicalMemory.SIZE_WIDTH);
        getClockPort().setUsed(true);
        getResetPort().setUsed(true);
        zeroConstant = new SimpleConstant(0, 1);
        zeroConstant.setIDLogical(this, "zeroConstant");
        addComponent(zeroConstant);
        zeroDataConstant = new SimpleConstant(0, memoryWidth);
        zeroDataConstant.setIDLogical(this, "zeroDataConstant");
        addComponent(zeroDataConstant);
        oneConstant = new SimpleConstant(1, 1);
        oneConstant.setIDLogical(this, "oneConstant");
        addComponent(oneConstant);
        int stateWidth = 0;
        for (int i = numTaskSlots; i > 0; i = i >> 1) {
            stateWidth++;
        }
        for (int i = 0; i < numTaskSlots; i++) {
            boolean doesRead = readList.get(i) != null;
            boolean doesWrite = writeList.get(i) != null;
            TaskSlot ts = new TaskSlot(this, memoryWidth, addressWidth, doesRead, doesWrite);
            addTaskSlot(ts);
        }
        final int taskCount = getTaskSlots().size();
        if (taskCount == 1) {
            TaskSlot ts = (TaskSlot) getTaskSlots().get(0);
            boolean readUsed = ts.getGoRPort() != null;
            boolean writeUsed = ts.getGoWPort() != null;
            Or goOr = null;
            if (readUsed && writeUsed) {
                goOr = new Or(2);
                addComponent(goOr);
                goOr.setIDLogical(this, "goOr");
                ((Port) goOr.getDataPorts().get(0)).setBus(ts.getGoRPort().getPeer());
                ((Port) goOr.getDataPorts().get(1)).setBus(ts.getGoWPort().getPeer());
                globalSlot.getGoBus().getPeer().setBus(goOr.getResultBus());
                globalSlot.getWriteEnableBus().getPeer().setBus(ts.getGoWPort().getPeer());
            } else if (readUsed) {
                globalSlot.getGoBus().getPeer().setBus(ts.getGoRPort().getPeer());
                globalSlot.getWriteEnableBus().getPeer().setBus(getZeroConstant().getValueBus());
            } else {
                globalSlot.getGoBus().getPeer().setBus(ts.getGoWPort().getPeer());
                globalSlot.getWriteEnableBus().getPeer().setBus(ts.getGoWPort().getPeer());
            }
            globalSlot.getAddressBus().getPeer().setBus(ts.getAddressPort().getPeer());
            globalSlot.getSizeBus().getPeer().setBus(ts.getSizePort().getPeer());
            if (writeUsed) {
                globalSlot.getWriteDataBus().getPeer().setBus(ts.getDataInPort().getPeer());
            } else {
                globalSlot.getWriteDataBus().getPeer().setBus(zeroDataConstant.getValueBus());
            }
            if (readUsed) {
                ts.getDataOutBus().getPeer().setBus(globalSlot.getReadDataPort().getPeer());
            }
            ts.getDoneBus().getPeer().setBus(globalSlot.getDonePort().getPeer());
            return;
        }
        EncodedMux goMux = new EncodedMux(taskCount);
        addComponent(goMux);
        goMux.setIDLogical(this, "goMux");
        EncodedMux addrMux = null;
        if (isAddressable) {
            addrMux = new EncodedMux(taskCount);
            addComponent(addrMux);
            addrMux.setIDLogical(this, "addrMux");
        }
        EncodedMux sizeMux = new EncodedMux(taskCount);
        addComponent(sizeMux);
        sizeMux.setIDLogical(this, "sizeMux");
        EncodedMux dataInMux = new EncodedMux(taskCount);
        addComponent(dataInMux);
        dataInMux.setIDLogical(this, "dataInMux");
        EncodedMux writeEnableMux = new EncodedMux(taskCount);
        addComponent(writeEnableMux);
        writeEnableMux.setIDLogical(this, "writeEnableMux");
        Or advanceOr = new Or(taskCount);
        addComponent(advanceOr);
        advanceOr.setIDLogical(this, "advanceOr");
        StateMachine stateMachine = new StateMachine(getClockPort().getPeer(), getResetPort().getPeer(), advanceOr.getResultBus(), stateWidth);
        goMux.getSelectPort().setBus(stateMachine.getResultBus());
        if (addrMux != null) {
            addrMux.getSelectPort().setBus(stateMachine.getResultBus());
        }
        sizeMux.getSelectPort().setBus(stateMachine.getResultBus());
        dataInMux.getSelectPort().setBus(stateMachine.getResultBus());
        writeEnableMux.getSelectPort().setBus(stateMachine.getResultBus());
        List taskCaptures = new ArrayList();
        for (int i = 0; i < taskCount; i++) {
            DoneFilter df = new DoneFilter(globalSlot.getDonePort().getPeer(), stateMachine.getDelayStateBus(), i, stateWidth);
            TaskCapture tc = new TaskCapture(stateWidth, addressWidth, memoryWidth, getResetPort().getPeer(), df.getResultBus(), globalSlot.getReadDataPort().getPeer(), (TaskSlot) getTaskSlots().get(i), i, combinationalMemoryReads);
            taskCaptures.add(tc);
            this.feedbackPoints.addAll(tc.getFeedbackPoints());
            goMux.getDataPort(i).setBus(tc.getTaskMemGoBus());
            ((Port) advanceOr.getDataPorts().get(i)).setBus(tc.getTaskMemGoBus());
            if (addrMux != null) {
                addrMux.getDataPort(i).setBus(tc.getTaskMemAddrBus());
            }
            sizeMux.getDataPort(i).setBus(tc.getTaskMemSizeBus());
            final CastOp castOp = new CastOp(memoryWidth, false);
            addComponent(castOp);
            castOp.getDataPort().setBus(tc.getTaskMemDataInBus());
            dataInMux.getDataPort(i).setBus(castOp.getResultBus());
            writeEnableMux.getDataPort(i).setBus(tc.getTaskMemWrBus());
        }
        stateMachine.setTaskCaptures(taskCaptures);
        this.feedbackPoints.addAll(stateMachine.getFeedbackPoints());
        globalSlot.getGoBus().getPeer().setBus(goMux.getResultBus());
        if (addrMux != null) {
            globalSlot.getAddressBus().getPeer().setBus(addrMux.getResultBus());
        } else {
            Constant addrConst = new SimpleConstant(0, addressWidth);
            globalSlot.getAddressBus().getPeer().setBus(addrConst.getValueBus());
        }
        globalSlot.getSizeBus().getPeer().setBus(sizeMux.getResultBus());
        globalSlot.getWriteDataBus().getPeer().setBus(dataInMux.getResultBus());
        globalSlot.getWriteEnableBus().getPeer().setBus(writeEnableMux.getResultBus());
    }

    private Constant getZeroConstant() {
        return zeroConstant;
    }

    private Constant getOneConstant() {
        return oneConstant;
    }

    /**
     * Tests whether this component is opaque.  If true, then this
     * component is to be treated as a self-contained entity.  This
     * means that its internal definition can make no direct references
     * to external entitities.  In particular, external {@link Bit Bits}
     * are not pushed into this component during constant propagation,
     * nor are any of its internal {@link Bit Bits} propagated to its
     * external {@link Bus Buses}.
     * <P>
     * Typically this implies that the translator will either generate
     * a primitive definition or an instantiatable module for this
     * component.
     *
     * @return true if this component is opaque, false otherwise
     */
    public boolean isOpaque() {
        return true;
    }

    protected void addTaskSlot(TaskSlot slot) {
        this.taskSlots.add(slot);
    }

    public List getTaskSlots() {
        return Collections.unmodifiableList(this.taskSlots);
    }

    protected GlobalSlot getGlobalSlot() {
        return this.globalSlot;
    }

    public void connectImplementation(StructuralMemory.StructuralMemoryPort port) {
        GlobalSlot globalSlot = getGlobalSlot();
        port.getAddressPort().setBus(globalSlot.getAddressBus());
        if (port.isWrite()) {
            port.getDataInPort().setBus(globalSlot.getWriteDataBus());
            port.getWriteEnablePort().setBus(globalSlot.getWriteEnableBus());
        }
        if (port.isRead()) {
            port.getEnablePort().setBus(globalSlot.getGoBus());
            globalSlot.getReadDataPort().setBus(port.getDataOutBus());
        } else {
            Constant zeroCon = new SimpleConstant(0, this.dataWidth);
            if (this.getOwner() != null) this.getOwner().addComponent(zeroCon);
            globalSlot.getReadDataPort().setBus(zeroCon.getValueBus());
        }
        port.getSizePort().setBus(globalSlot.getSizeBus());
        globalSlot.getDonePort().setBus(port.getDoneBus());
    }

    /**
     * Returns a Set of {@link Component Components} that represent
     * the feedback points in this Module.  This set is populated from
     * the components created by the TaskCapture and StateMachine
     * classes.
     *
     * @return a 'Set' of {@link Component Components}
     */
    public Set getFeedbackPoints() {
        Set feedback = new HashSet();
        feedback.addAll(super.getFeedbackPoints());
        feedback.addAll(feedbackPoints);
        return Collections.unmodifiableSet(feedback);
    }

    /**
     * Accept method for the Visitor interface
     */
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public boolean removeDataBus(Bus bus) {
        assert false : "remove data bus not supported on " + this;
        return false;
    }

    public boolean removeDataPort(Port port) {
        assert false : "remove data port not supported on " + this;
        return false;
    }

    public String show() {
        String ret = toString();
        for (Iterator iter = getPorts().iterator(); iter.hasNext(); ) {
            Port port = (Port) iter.next();
            if (port == getGoPort()) ret = ret + " go:" + port + "/" + port.getPeer(); else if (port == getClockPort()) ret = ret + " ck:" + port + "/" + port.getPeer(); else if (port == getResetPort()) ret = ret + " rs:" + port + "/" + port.getPeer() + "\n"; else if (port == globalSlot.getDonePort()) ret = ret + " GL_done:" + port + "/" + port.getPeer(); else if (port == globalSlot.getReadDataPort()) ret = ret + " GL_read:" + port + "/" + port.getPeer() + "\n"; else {
                String id = null;
                for (Iterator pIter = this.taskSlots.iterator(); pIter.hasNext(); ) {
                    TaskSlot ts = (TaskSlot) pIter.next();
                    if (port == ts.getGoRPort()) {
                        id = " task go R: " + port + "/" + port.getPeer();
                    } else if (port == ts.getGoWPort()) {
                        id = " task go W: " + port + "/" + port.getPeer();
                    } else if (port == ts.getAddressPort()) {
                        id = " task ad: " + port + "/" + port.getPeer();
                    } else if (port == ts.getDataInPort()) {
                        id = " task data in: " + port + "/" + port.getPeer() + "\n";
                    } else if (port == ts.getSizePort()) {
                        id = " size in: " + port + "/" + port.getPeer() + "\n";
                    }
                }
                if (id == null) id = " p:" + port + "/" + port.getPeer();
                ret += id;
            }
        }
        for (Iterator bIter = getExits().iterator(); bIter.hasNext(); ) {
            Exit exit = (Exit) bIter.next();
            for (Iterator busIter = exit.getBuses().iterator(); busIter.hasNext(); ) {
                Bus bus = (Bus) busIter.next();
                if (bus == exit.getDoneBus()) ret = ret + " done:" + bus + "/" + bus.getPeer(); else if (bus == globalSlot.getWriteDataBus()) ret = ret + " GS_wd_bus:" + bus + "/" + bus.getPeer(); else if (bus == globalSlot.getAddressBus()) ret = ret + " GS_ad_bus:" + bus + "/" + bus.getPeer(); else if (bus == globalSlot.getWriteEnableBus()) ret = ret + " GS_we_bus:" + bus + "/" + bus.getPeer(); else if (bus == globalSlot.getGoBus()) ret = ret + " GS_go_bus:" + bus + "/" + bus.getPeer() + "\n"; else if (bus == globalSlot.getSizeBus()) ret = ret + " GS_size_bus:" + bus + "/" + bus.getPeer() + "\n"; else {
                    String id = null;
                    for (Iterator slotIter = this.taskSlots.iterator(); slotIter.hasNext(); ) {
                        TaskSlot ts = (TaskSlot) slotIter.next();
                        if (bus == ts.getDataOutBus()) {
                            id = " task data out bus:" + bus + "/" + bus.getPeer();
                        } else if (bus == ts.getDoneBus()) {
                            id = " task done bus: " + bus + "/" + bus.getPeer();
                        }
                    }
                    if (id == null) id = " data:" + bus;
                    ret += id;
                }
            }
        }
        return ret;
    }

    /**
     * a DoneFilter encapsulates the hardware to filter the global done signal
     * to the correct TaskCapture
     */
    class DoneFilter {

        And and;

        /**
         * build the hardware
         * @param globalDoneBus
         * @param delayStateBus
         * @param stage int describing the current stage (0...(N-1))
         * @param stateWidth how many bits to represent state
         */
        DoneFilter(Bus globalDoneBus, Bus delayStateBus, int stage, int stateWidth) {
            Constant constant = new SimpleConstant(stage, stateWidth, false);
            addComponent(constant);
            constant.setIDLogical(this, "DFconst_" + stage);
            EqualsOp equalsOp = new EqualsOp();
            addComponent(equalsOp);
            equalsOp.setIDLogical(this, "DFequalsOp+" + stage);
            and = new And(2);
            addComponent(and);
            and.setIDLogical(this, "DFand_" + stage);
            ((Port) equalsOp.getDataPorts().get(0)).setBus(constant.getValueBus());
            ((Port) equalsOp.getDataPorts().get(1)).setBus(delayStateBus);
            ((Port) and.getDataPorts().get(0)).setBus(equalsOp.getResultBus());
            ((Port) and.getDataPorts().get(1)).setBus(globalDoneBus);
        }

        Bus getResultBus() {
            return and.getResultBus();
        }
    }

    /**
     * TaskCapture encapsulates the hardware associated with a task that
     * is part of an arbitrated referee (single task referee does not require
     * this hardware
     */
    class TaskCapture {

        Or readOr;

        Or writeOr;

        EncodedMux addrRegMux;

        EncodedMux sizeRegMux;

        EncodedMux dinMux;

        /** A Set of Components which represent the points of feedback
         * in this taskCapture */
        Set feedbackPoints = new HashSet(7);

        /**
         * build the hardware for a task capture
         * @param stateWidth number of bits required to represent the state
         * @param depth number of bits required to represent the address
         * @param width number of bits required to represent the data
         * @param resetBus global reset
         * @param doneBus bus providing the done (from done filter)
         * @param dataOutBus bus providing data (used by read)
         * @param taskSlot TaskSlot that corresponds to this task
         * @param index unique int for naming
         * @param combinationalRead true if memory read takes 0
         * clocks, therefore adjust the logic to break the
         * combinational feedback loop from go to done of the memory.
         */
        TaskCapture(int stateWidth, int depth, int width, Bus resetBus, Bus doneBus, Bus dataOutBus, TaskSlot ts, int index, boolean combinationalRead) {
            boolean readUsed = ts.getGoRPort() != null;
            boolean writeUsed = ts.getGoWPort() != null;
            Bus goRBus = readUsed ? ts.getGoRPort().getPeer() : null;
            Bus goWBus = writeUsed ? ts.getGoWPort().getPeer() : null;
            Bus addrBus = ts.getAddressPort().getPeer();
            Bus sizeBus = ts.getSizePort().getPeer();
            Bus dataInBus = writeUsed ? ts.getDataInPort().getPeer() : null;
            Bus clockBus = getClockPort().getPeer();
            Bus oneBus = getOneConstant().getValueBus();
            Reg readGoReg = null;
            And readAnd = null;
            Not readNot = null;
            EncodedMux readMux = null;
            if (readUsed) {
                readGoReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "TCreadGoReg_" + index);
                addComponent(readGoReg);
                readGoReg.getInternalResetPort().setBus(resetBus);
                readGoReg.getClockPort().setBus(clockBus);
                feedbackPoints.add(readGoReg);
                readGoReg.getResultBus().setSize(1, false);
                readAnd = new And(2);
                addComponent(readAnd);
                readAnd.setIDLogical(this, "TCreadAnd_" + index);
                readNot = new Not();
                addComponent(readNot);
                readNot.setIDLogical(this, "TCreadNot_" + index);
                readMux = new EncodedMux(2);
                addComponent(readMux);
                readMux.setIDLogical(this, "TCreadMux_" + index);
            }
            if (!readUsed || !writeUsed) {
                readOr = new Or(2);
            } else {
                readOr = new Or(4);
            }
            addComponent(readOr);
            readOr.setIDLogical(this, "TCreadOr_" + index);
            Reg writeGoReg = null;
            And writeAnd = null;
            Not writeNot = null;
            EncodedMux writeMux = null;
            if (writeUsed) {
                writeGoReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "TCwriteGoReg_" + index);
                addComponent(writeGoReg);
                writeGoReg.getInternalResetPort().setBus(resetBus);
                writeGoReg.getClockPort().setBus(clockBus);
                feedbackPoints.add(writeGoReg);
                writeGoReg.getResultBus().setSize(1, false);
                writeAnd = new And(2);
                addComponent(writeAnd);
                writeAnd.setIDLogical(this, "TCwriteAnd_" + index);
                writeNot = new Not();
                addComponent(writeNot);
                writeNot.setIDLogical(this, "TCwriteNot_" + index);
                writeMux = new EncodedMux(2);
                addComponent(writeMux);
                writeMux.setIDLogical(this, "TCwriteMux_" + index);
                writeOr = new Or(2);
                addComponent(writeOr);
                writeOr.setIDLogical(this, "TCwriteOr_" + index);
            }
            Or addrOr = null;
            if (readUsed && writeUsed) {
                addrOr = new Or(2);
                addComponent(addrOr);
                addrOr.setIDLogical(this, "TCaddrOr_" + index);
            }
            Reg addrReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "TCaddrReg_" + index);
            addComponent(addrReg);
            addrReg.getInternalResetPort().setBus(resetBus);
            addrReg.getClockPort().setBus(clockBus);
            feedbackPoints.add(addrReg);
            addrReg.getResultBus().setSize(depth, false);
            addrRegMux = new EncodedMux(2);
            addComponent(addrRegMux);
            addrRegMux.setIDLogical(this, "TCaddrRegMux_" + index);
            Reg sizeReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "TCsizeReg_" + index);
            addComponent(sizeReg);
            sizeReg.getInternalResetPort().setBus(resetBus);
            sizeReg.getClockPort().setBus(clockBus);
            feedbackPoints.add(sizeReg);
            sizeReg.getResultBus().setSize(LogicalMemory.SIZE_WIDTH, false);
            sizeRegMux = new EncodedMux(2);
            addComponent(sizeRegMux);
            sizeRegMux.setIDLogical(this, "TCsizeRegMux_" + index);
            Reg dinReg = null;
            dinMux = null;
            if (writeUsed) {
                dinReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "TCdinReg_" + index);
                addComponent(dinReg);
                dinReg.getInternalResetPort().setBus(resetBus);
                dinReg.getClockPort().setBus(clockBus);
                dinReg.getResultBus().setSize(width, false);
                feedbackPoints.add(dinReg);
                dinMux = new EncodedMux(2);
                addComponent(dinMux);
                dinMux.setIDLogical(this, "TCdinMux_" + index);
            }
            ts.getDoneBus().getPeer().setBus(doneBus);
            if (readUsed) {
                readMux.getSelectPort().setBus(goRBus);
                readMux.getDataPort(1).setBus(oneBus);
                if (combinationalRead) {
                    readMux.getDataPort(0).setBus(readGoReg.getResultBus());
                    readGoReg.getDataPort().setBus(readAnd.getResultBus());
                    ((Port) readAnd.getDataPorts().get(0)).setBus(readMux.getResultBus());
                } else {
                    readMux.getDataPort(0).setBus(readAnd.getResultBus());
                    readGoReg.getDataPort().setBus(readMux.getResultBus());
                    ((Port) readAnd.getDataPorts().get(0)).setBus(readGoReg.getResultBus());
                }
                ((Port) readAnd.getDataPorts().get(1)).setBus(readNot.getResultBus());
                readNot.getDataPort().setBus(doneBus);
            }
            if (writeUsed) {
                writeMux.getSelectPort().setBus(goWBus);
                writeMux.getDataPort(1).setBus(oneBus);
                writeMux.getDataPort(0).setBus(writeAnd.getResultBus());
                writeGoReg.getDataPort().setBus(writeMux.getResultBus());
                ((Port) writeAnd.getDataPorts().get(0)).setBus(writeGoReg.getResultBus());
                ((Port) writeAnd.getDataPorts().get(1)).setBus(writeNot.getResultBus());
                writeNot.getDataPort().setBus(doneBus);
            }
            if (readUsed && writeUsed) {
                if (combinationalRead) {
                    ((Port) readOr.getDataPorts().get(0)).setBus(readGoReg.getResultBus());
                } else {
                    ((Port) readOr.getDataPorts().get(0)).setBus(readAnd.getResultBus());
                }
                ((Port) readOr.getDataPorts().get(1)).setBus(goRBus);
                ((Port) readOr.getDataPorts().get(2)).setBus(writeAnd.getResultBus());
                ((Port) readOr.getDataPorts().get(3)).setBus(goWBus);
                ((Port) writeOr.getDataPorts().get(0)).setBus(writeAnd.getResultBus());
                ((Port) writeOr.getDataPorts().get(1)).setBus(goWBus);
            } else if (readUsed) {
                if (combinationalRead) {
                    ((Port) readOr.getDataPorts().get(0)).setBus(readGoReg.getResultBus());
                } else {
                    ((Port) readOr.getDataPorts().get(0)).setBus(readAnd.getResultBus());
                }
                ((Port) readOr.getDataPorts().get(1)).setBus(goRBus);
            } else {
                ((Port) readOr.getDataPorts().get(0)).setBus(writeAnd.getResultBus());
                ((Port) readOr.getDataPorts().get(1)).setBus(goWBus);
                ((Port) writeOr.getDataPorts().get(0)).setBus(writeAnd.getResultBus());
                ((Port) writeOr.getDataPorts().get(1)).setBus(goWBus);
            }
            Bus addrSelectBus;
            if (readUsed && writeUsed) {
                ((Port) addrOr.getDataPorts().get(0)).setBus(goRBus);
                ((Port) addrOr.getDataPorts().get(1)).setBus(goWBus);
                addrSelectBus = addrOr.getResultBus();
            } else if (readUsed) {
                addrSelectBus = goRBus;
            } else {
                addrSelectBus = goWBus;
            }
            addrRegMux.getSelectPort().setBus(addrSelectBus);
            addrRegMux.getDataPort(0).setBus(addrReg.getResultBus());
            addrRegMux.getDataPort(1).setBus(addrBus);
            addrReg.getDataPort().setBus(addrRegMux.getResultBus());
            sizeRegMux.getSelectPort().setBus(addrSelectBus);
            sizeRegMux.getDataPort(0).setBus(sizeReg.getResultBus());
            sizeRegMux.getDataPort(1).setBus(sizeBus);
            sizeReg.getDataPort().setBus(sizeRegMux.getResultBus());
            if (writeUsed) {
                dinMux.getSelectPort().setBus(goWBus);
                dinMux.getDataPort(0).setBus(dinReg.getResultBus());
                dinMux.getDataPort(1).setBus(dataInBus);
                dinReg.getDataPort().setBus(dinMux.getResultBus());
            }
            if (readUsed) {
                ts.getDataOutBus().getPeer().setBus(dataOutBus);
            }
        }

        Bus getTaskMemGoBus() {
            return readOr.getResultBus();
        }

        Bus getTaskMemWrBus() {
            if (writeOr == null) {
                return getZeroConstant().getValueBus();
            }
            return writeOr.getResultBus();
        }

        Bus getTaskMemAddrBus() {
            return addrRegMux.getResultBus();
        }

        Bus getTaskMemSizeBus() {
            return sizeRegMux.getResultBus();
        }

        Bus getTaskMemDataInBus() {
            if (dinMux == null) {
                return MemoryReferee.this.zeroDataConstant.getValueBus();
            }
            return dinMux.getResultBus();
        }

        public Set getFeedbackPoints() {
            return this.feedbackPoints;
        }
    }

    /** StateMachine encapsulates the selection of the appropriate go signal to
     * propagate to the global resource, in a fair (round-robbin) priority
     * scheme.  It is implemented as follows:
     * <img src="doc-files/MemoryReferee-StateMachine.png">
     * where gcX is defined as the oputput of the Ors in the go capture module
     * {@link MemoryReferee shown} here.
     */
    class StateMachine {

        Or stateOr;

        AndOp stateAndOp;

        And stateAnd;

        EncodedMux stateMux;

        Reg stateReg;

        List stateMuxList;

        Bus resetBus;

        Bus advanceStateBus;

        Not stateNot;

        Reg delayStateReg;

        int stateWidth;

        /** A Set of Components which represent the points of feedback
         * in this statemachine */
        Set feedbackPoints = new HashSet(7);

        /** 
         * create the state machine
         * @param reset bus is the global reset
         * @param advanceStateBus is the advance state signal
         */
        StateMachine(Bus clockBus, Bus resetBus, Bus advanceStateBus, int stateWidth) {
            assert (resetBus != null);
            assert (advanceStateBus != null);
            assert stateWidth > 0;
            this.stateWidth = stateWidth;
            stateReg = Reg.getConfigurableReg(Reg.REGRE, ID.showLogical(this) + "stateReg");
            stateReg.getClockPort().setBus(getClockPort().getPeer());
            stateReg.getInternalResetPort().setBus(getResetPort().getPeer());
            feedbackPoints.add(stateReg);
            stateReg.getResultBus().setSize(stateWidth, false);
            delayStateReg = Reg.getConfigurableReg(Reg.REGR, ID.showLogical(this) + "delayStateReg");
            delayStateReg.getClockPort().setBus(getClockPort().getPeer());
            delayStateReg.getInternalResetPort().setBus(getResetPort().getPeer());
            addComponent(stateReg);
            addComponent(delayStateReg);
            this.resetBus = resetBus;
            this.advanceStateBus = advanceStateBus;
        }

        Bus getResultBus() {
            return stateReg.getResultBus();
        }

        Bus getDelayStateBus() {
            return delayStateReg.getResultBus();
        }

        /**
         * instantiate its components
         * and wire it up as shown above
         * @param taskCaptures is the list of inputs to the state machine
         */
        void setTaskCaptures(List taskCaptures) {
            int size = taskCaptures.size();
            stateNot = new Not();
            addComponent(stateNot);
            stateNot.setIDLogical(this, "stateNot");
            stateOr = new Or(size);
            addComponent(stateOr);
            stateOr.setIDLogical(this, "stateOr");
            stateAnd = new And(2);
            addComponent(stateAnd);
            stateAnd.setIDLogical(this, "stateAnd");
            stateAndOp = new AndOp();
            addComponent(stateAndOp);
            stateAndOp.setIDLogical(this, "stateAndOp");
            stateAndOp.getResultBus().setIDLogical("stateAndOp");
            stateMux = new EncodedMux(size);
            addComponent(stateMux);
            stateMux.setIDLogical(this, "stateMux");
            stateMuxList = new ArrayList(size);
            stateNot.getDataPort().setBus(resetBus);
            ((Port) stateAnd.getDataPorts().get(0)).setBus(stateNot.getResultBus());
            ((Port) stateAnd.getDataPorts().get(1)).setBus(stateOr.getResultBus());
            final CastOp stateAndSignChange = new CastOp(1, true);
            stateAndSignChange.getDataPort().setBus(stateAnd.getResultBus());
            addComponent(stateAndSignChange);
            final CastOp stateAndCast = new CastOp(stateWidth, false);
            stateAndCast.getDataPort().setBus(stateAndSignChange.getResultBus());
            addComponent(stateAndCast);
            ((Port) stateAndOp.getDataPorts().get(0)).setBus(stateAndCast.getResultBus());
            Bus mux0Input = stateAndOp.getResultBus();
            for (int i = 0; i < size; i++) {
                EncodedMux m = new EncodedMux(2);
                addComponent(m);
                m.setIDLogical(this, "stateMux_" + i);
                stateMuxList.add(m);
                TaskCapture tc = (TaskCapture) taskCaptures.get(i);
                Bus tcResultBus = tc.getTaskMemGoBus();
                Port port = m.getDataPort(0);
                port.setBus(mux0Input);
                Constant stateValue = new SimpleConstant(i, stateWidth, false);
                addComponent(stateValue);
                stateValue.setIDLogical(this, "stateValue_" + i);
                port = m.getDataPort(1);
                port.setBus(stateValue.getValueBus());
                port = m.getSelectPort();
                port.setBus(tcResultBus);
                mux0Input = m.getResultBus();
                if (i < (size - 1)) {
                    port = stateMux.getDataPort(i + 1);
                    port.setBus(mux0Input);
                } else {
                    port = stateMux.getDataPort(0);
                    port.setBus(mux0Input);
                    port = ((Port) stateAndOp.getDataPorts().get(1));
                    port.setBus(mux0Input);
                    feedbackPoints.add(m);
                    m.getResultBus().setSize(stateWidth, false);
                }
                port = ((Port) stateOr.getDataPorts().get(i));
                port.setBus(tcResultBus);
            }
            Port port = stateReg.getDataPort();
            port.setBus(stateMux.getResultBus());
            port = stateReg.getEnablePort();
            port.setBus(advanceStateBus);
            port = stateMux.getSelectPort();
            port.setBus(stateReg.getResultBus());
            delayStateReg.getDataPort().setBus(stateReg.getResultBus());
        }

        public Set getFeedbackPoints() {
            return this.feedbackPoints;
        }
    }

    /**
     * a collection of ports and buses to which a task can connect.
     * encapsulates both reading and writing.  Note that this contains no 
     * hardware, it is just an interface.  the TaskCapture contains the 
     * hardware.
     */
    public class TaskSlot {

        /** read go */
        private Port goR = null;

        /** write go */
        private Port goW = null;

        /** address */
        private Port address;

        /** data in */
        private Port dataIn = null;

        /** size in */
        private Port size = null;

        /** data out */
        private Bus dataOut = null;

        /** done */
        private Bus done;

        private boolean readUsed;

        private boolean writeUsed;

        TaskSlot(MemoryReferee parent, int dataWidth, int addrWidth, boolean readUsed, boolean writeUsed) {
            this.readUsed = readUsed;
            this.writeUsed = writeUsed;
            Exit exit = parent.getExit(Exit.DONE);
            if (readUsed) {
                goR = parent.makeDataPort();
                dataOut = exit.makeDataBus();
            }
            if (writeUsed) {
                goW = parent.makeDataPort();
                dataIn = parent.makeDataPort();
            }
            address = parent.makeDataPort();
            size = parent.makeDataPort();
            done = exit.makeDataBus();
            setSizes(dataWidth, addrWidth, LogicalMemory.SIZE_WIDTH);
        }

        private void setSizes(int dataWidth, int addrWidth, int sizeWidth) {
            if (goR != null) {
                goR.setSize(1, false);
                dataOut.setSize(dataWidth, false);
            }
            if (goW != null) {
                goW.setSize(1, false);
                dataIn.setSize(dataWidth, false);
            }
            address.setSize(addrWidth, false);
            size.setSize(sizeWidth, false);
            done.setSize(1, false);
        }

        public Port getGoRPort() {
            return goR;
        }

        public Port getGoWPort() {
            return goW;
        }

        public Port getAddressPort() {
            return address;
        }

        public Port getDataInPort() {
            return dataIn;
        }

        public Port getSizePort() {
            return size;
        }

        public Bus getDataOutBus() {
            return dataOut;
        }

        public Bus getDoneBus() {
            return done;
        }

        public String toString() {
            String ret = super.toString();
            ret += " erp: " + getGoRPort() + "/" + (getGoRPort() == null ? "null" : getGoRPort().getPeer().toString());
            ret += " ewp: " + getGoWPort() + "/" + (getGoWPort() == null ? "null" : getGoWPort().getPeer().toString());
            ret += " ap: " + getAddressPort() + "/" + (getAddressPort() == null ? "null" : getAddressPort().getPeer().toString());
            ret += " dp: " + getDataInPort() + "/" + (getDataInPort() == null ? "null" : getDataInPort().getPeer().toString());
            ret += " sp: " + getSizePort() + "/" + (getSizePort() == null ? "null" : getSizePort().getPeer().toString());
            ret += " db: " + getDataOutBus() + "/" + (getDataOutBus() == null ? "null" : getDataOutBus().getPeer().toString());
            ret += " doneb: " + getDoneBus() + "/" + (getDoneBus() == null ? "null" : getDoneBus().getPeer().toString());
            return ret;
        }
    }

    public class GlobalSlot {

        private Port done;

        private Port readData;

        private Bus writeData;

        private Bus address;

        private Bus writeEnable;

        private Bus go;

        private Bus size;

        public GlobalSlot(MemoryReferee parent) {
            done = parent.makeDataPort();
            readData = parent.makeDataPort();
            Exit exit = parent.getExit(Exit.DONE);
            writeData = exit.makeDataBus();
            address = exit.makeDataBus();
            writeEnable = exit.makeDataBus();
            go = exit.makeDataBus();
            size = exit.makeDataBus();
        }

        public void setSizes(int dataWidth, int addrWidth, int sizeWidth) {
            done.setSize(1, false);
            readData.setSize(dataWidth, false);
            writeData.setSize(dataWidth, false);
            address.setSize(addrWidth, false);
            writeEnable.setSize(1, false);
            go.setSize(1, false);
            size.setSize(sizeWidth, false);
        }

        public Port getDonePort() {
            return this.done;
        }

        public Port getReadDataPort() {
            return this.readData;
        }

        public Bus getWriteDataBus() {
            return this.writeData;
        }

        public Bus getAddressBus() {
            return this.address;
        }

        public Bus getWriteEnableBus() {
            return this.writeEnable;
        }

        public Bus getGoBus() {
            return this.go;
        }

        public Bus getSizeBus() {
            return this.size;
        }

        public String toString() {
            String ret = super.toString();
            ret += " donep: " + getDonePort() + "/" + getDonePort().getPeer();
            ret += " rdp: " + getReadDataPort() + "/" + getReadDataPort().getPeer();
            ret += " wdb: " + getWriteDataBus() + "/" + getWriteDataBus().getPeer();
            ret += " addr: " + getAddressBus() + "/" + getAddressBus().getPeer();
            ret += " wen: " + getWriteEnableBus() + "/" + getWriteEnableBus().getPeer();
            ret += " go: " + getGoBus() + "/" + getGoBus().getPeer();
            ret += " size: " + getSizeBus() + "/" + getSizeBus().getPeer();
            return ret;
        }
    }
}
