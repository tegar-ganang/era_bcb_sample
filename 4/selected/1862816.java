package net.sf.openforge.verilog.testbench;

import java.io.*;
import net.sf.openforge.verilog.model.*;
import net.sf.openforge.verilog.pattern.CommaDelimitedStatement;

/**
 * ClockChecker maintains wires and logic for counting the number of
 * clock cycles between the GO's asserted to and DONE's received from
 * each task.  This is accomplished by instantiating a global timer
 * (counts from 0 starting at the beginning of the simulation).  For
 * each time a GO is asserted the current time is pushed into the
 * cycle count fifo.  Each time a done is received the cycle count is
 * calculated by subtracting from the current time (global timer
 * value) the 'old time' as read from the fifo.  This is guaranteed to
 * work so long as each task completes before the next task begin.
 * This scheme will fail if task A takes 5 cycles and task B takes 0
 * cycles and task B is told to GO before the last DONE of task A is
 * received.  We avoid this in our test bench by pausing the GO's
 * anytime we switch tasks until all the DONEs have been received from
 * the first task.  Otherwise, one fifo will be necessary for each
 * task.
 *
 * <p>Created: Wed Jan  8 15:43:29 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ClockChecker.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ClockChecker {

    private static final String _RCS_ = "$Rev: 2 $";

    /** The depth of the clock checker fifo, determines the maximum
     * number of simultaneous GOs that can be pending in a given
     * task */
    public static final int MEM_DEPTH = 1024;

    private Register writePointer;

    private Register readPointer;

    private Register globalTime;

    private Register memory;

    private MemoryDeclaration memoryDec;

    private Wire cycles;

    private SimFileHandle results;

    public ClockChecker(File cycleResults) {
        this.globalTime = new Register("ccGlobalTimer", 32);
        this.memory = new Register("ccTimeFifo", 32);
        final int topNum = (int) java.lang.Math.ceil(java.lang.Math.log(MEM_DEPTH) / java.lang.Math.log(2));
        this.writePointer = new Register("ccWritePointer", topNum);
        this.readPointer = new Register("ccReadPointer", topNum);
        this.memoryDec = new MemoryDeclaration(memory, MEM_DEPTH - 1, 0);
        this.cycles = new Wire("ccCycleCount", 32);
        this.results = new SimFileHandle(cycleResults, "clkCntFile");
    }

    /**
     * States initial values for the read/write pointers and the
     * global timer, and opens the results file
     */
    public void stateInits(InitialBlock ib) {
        ib.add(new Assign.NonBlocking(this.writePointer, new Constant(0, this.writePointer.getWidth())));
        ib.add(new Assign.NonBlocking(this.readPointer, new Constant(0, this.readPointer.getWidth())));
        ib.add(new Assign.NonBlocking(this.globalTime, new Constant(0, this.globalTime.getWidth())));
        results.stateInits(ib);
    }

    /**
     * Creates the fifo logic and calculates the number of cycles.
     */
    public void stateLogic(Module module, StateMachine mach) {
        module.declare(memoryDec);
        final Wire oldTime = new Wire("ccOldTime", 32);
        final Wire isPassThrough = new Wire("ccPassThrough", 1);
        final Compare.EQ ef = new Compare.EQ(this.readPointer, this.writePointer);
        final Logical.And simultaneous = new Logical.And(mach.getAllGoWire(), mach.getAllDoneWire());
        module.state(new Assign.Continuous(isPassThrough, new Logical.And(ef, new Group(simultaneous))));
        module.state(new Assign.Continuous(oldTime, new Conditional(new Group(isPassThrough), this.globalTime, new MemoryElement(this.memory, this.readPointer))));
        module.state(new Assign.Continuous(this.cycles, new net.sf.openforge.verilog.model.Math.Subtract(this.globalTime, oldTime)));
        stateGlobalTimer(module, mach);
        stateFifo(module, mach, isPassThrough);
        stateCapture(module, mach);
    }

    /**
     * Generates the global timer.
     */
    private void stateGlobalTimer(Module module, StateMachine mach) {
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(new EventControl(new EventExpression.PosEdge(mach.getClock())), new Assign.NonBlocking(this.globalTime, new net.sf.openforge.verilog.model.Math.Add(this.globalTime, new Constant(1, this.globalTime.getWidth()))));
        module.state(new Always(ptb));
    }

    /**
     * Creates the actual cycle count FIFO.
     *
     * @param module a value of type 'Module'
     * @param mach a value of type 'StateMachine'
     * @param isPassThrough a value of type 'Wire'
     */
    private void stateFifo(Module module, StateMachine mach, Wire isPassThrough) {
        final SequentialBlock ifBlock = new SequentialBlock();
        final SequentialBlock wpBlock = new SequentialBlock();
        wpBlock.add(new Assign.NonBlocking(this.writePointer, new net.sf.openforge.verilog.model.Math.Add(this.writePointer, new Constant(1, this.writePointer.getWidth()))));
        wpBlock.add(new Assign.NonBlocking(new MemoryElement(this.memory, this.writePointer), this.globalTime));
        ifBlock.add(new ConditionalStatement(mach.getAllGoWire(), wpBlock));
        ifBlock.add(new ConditionalStatement(mach.getAllDoneWire(), new Assign.NonBlocking(this.readPointer, new net.sf.openforge.verilog.model.Math.Add(this.readPointer, new Constant(1, this.readPointer.getWidth())))));
        final ConditionalStatement cond1 = new ConditionalStatement(new Unary.Not(isPassThrough), ifBlock);
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(new EventControl(new EventExpression.PosEdge(mach.getClock())), new SequentialBlock(cond1));
        module.state(new Always(ptb));
    }

    /**
     * States the logic for writing the cycle count to the results
     * file.
     * <pre>
     * always @(posedge clk) begin
     *   if (anyDone) $fwrite(cycleCaptureFile, "%d\n", cycles);
     * end
     * </pre>
     *
     * @param module a value of type 'Module'
     */
    private void stateCapture(Module module, StateMachine mach) {
        CommaDelimitedStatement cds = new CommaDelimitedStatement();
        cds.append(new StringStatement("%d\\n"));
        cds.append(this.cycles);
        ConditionalStatement condition = new ConditionalStatement(mach.getAllDoneWire(), new FStatement.FWrite(results.getHandle(), cds));
        ProceduralTimingBlock ptb = new ProceduralTimingBlock(new EventControl(new EventExpression.PosEdge(mach.getClock())), condition);
        module.state(new Always(ptb));
    }
}
