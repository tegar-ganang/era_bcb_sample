package se.sics.mspsim.core;

import java.io.PrintStream;
import java.util.ArrayList;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.MapTable;
import se.sics.mspsim.util.Utils;

/**
 * The CPU of the MSP430
 */
public class MSP430Core extends Chip implements MSP430Constants {

    public static final int RETURN = 0x4130;

    public static final boolean DEBUG = false;

    public static final boolean debugInterrupts = false;

    public static final boolean EXCEPTION_ON_BAD_OPERATION = true;

    public static final int MAX_MEM = 64 * 1024;

    public static final int MAX_MEM_IO = 0x200;

    public static final int PORTS = 6;

    public int[] reg = new int[16];

    public CPUMonitor globalMonitor;

    public CPUMonitor[] regWriteMonitors = new CPUMonitor[16];

    public CPUMonitor[] regReadMonitors = new CPUMonitor[16];

    public CPUMonitor[] breakPoints = new CPUMonitor[MAX_MEM];

    boolean breakpointActive = true;

    public int memory[] = new int[MAX_MEM];

    public long cycles = 0;

    public long cpuCycles = 0;

    MapTable map;

    public MSP430Config config;

    public IOUnit[] memOut = new IOUnit[MAX_MEM_IO];

    public IOUnit[] memIn = new IOUnit[MAX_MEM_IO];

    private IOUnit[] ioUnits;

    private SFR sfr;

    private Watchdog watchdog;

    private InterruptHandler interruptSource[] = new InterruptHandler[64];

    private final int MAX_INTERRUPT;

    protected int interruptMax = -1;

    private int op;

    public int instruction;

    int servicedInterrupt = -1;

    InterruptHandler servicedInterruptUnit = null;

    protected boolean interruptsEnabled = false;

    protected boolean cpuOff = false;

    protected int dcoFrq = 2500000;

    int aclkFrq = 32768;

    int smclkFrq = dcoFrq;

    long lastCyclesTime = 0;

    long lastVTime = 0;

    long currentTime = 0;

    long lastMicrosDelta;

    double currentDCOFactor = 1.0;

    long nextEventCycles;

    private EventQueue vTimeEventQueue = new EventQueue();

    private long nextVTimeEventCycles;

    private EventQueue cycleEventQueue = new EventQueue();

    private long nextCycleEventCycles;

    private BasicClockModule bcs;

    private ArrayList<Chip> chips = new ArrayList<Chip>();

    ComponentRegistry registry;

    Profiler profiler;

    private Flash flash;

    boolean isFlashBusy;

    public MSP430Core(int type, ComponentRegistry registry, MSP430Config config) {
        super("MSP430", "MSP430 Core", null);
        MAX_INTERRUPT = config.maxInterruptVector;
        this.registry = registry;
        this.config = config;
        addChip(this);
        setModeNames(MODE_NAMES);
        int passIO = 0;
        ioUnits = new IOUnit[PORTS + 9];
        Timer ta = new Timer(this, memory, config.timerConfig[0]);
        Timer tb = new Timer(this, memory, config.timerConfig[1]);
        for (int i = 0, n = 0x20; i < n; i++) {
            memOut[config.timerConfig[0].offset + i] = ta;
            memIn[config.timerConfig[0].offset + i] = ta;
            memOut[config.timerConfig[1].offset + i] = tb;
            memIn[config.timerConfig[1].offset + i] = tb;
        }
        flash = new Flash(this, memory, new FlashRange(0x4000, 0x10000, 512, 64), new FlashRange(0x1000, 0x01100, 128, 64));
        for (int i = 0x128; i < 0x12e; i++) {
            memOut[i] = flash;
            memIn[i] = flash;
        }
        sfr = new SFR(this, memory);
        for (int i = 0, n = 0x10; i < n; i++) {
            memOut[i] = sfr;
            memIn[i] = sfr;
        }
        watchdog = new Watchdog(this);
        memOut[0x120] = watchdog;
        memIn[0x120] = watchdog;
        memIn[Timer.TAIV] = ta;
        memOut[Timer.TAIV] = ta;
        memIn[Timer.TBIV] = tb;
        memOut[Timer.TBIV] = tb;
        bcs = new BasicClockModule(this, memory, 0, new Timer[] { ta, tb });
        for (int i = 0x56, n = 0x59; i < n; i++) {
            memOut[i] = bcs;
        }
        Multiplier mp = new Multiplier(this, memory, 0);
        for (int i = 0x130, n = 0x13f; i < n; i++) {
            memOut[i] = mp;
            memIn[i] = mp;
        }
        USART usart0 = new USART(this, 0, memory, 0x70);
        USART usart1 = new USART(this, 1, memory, 0x78);
        for (int i = 0, n = 8; i < n; i++) {
            memOut[0x70 + i] = usart0;
            memIn[0x70 + i] = usart0;
            memOut[0x78 + i] = usart1;
            memIn[0x78 + i] = usart1;
        }
        ioUnits[0] = new IOPort(this, 1, 4, memory, 0x20);
        ioUnits[1] = new IOPort(this, 2, 1, memory, 0x28);
        for (int i = 0, n = 8; i < n; i++) {
            memOut[0x20 + i] = ioUnits[0];
            memOut[0x28 + i] = ioUnits[1];
        }
        for (int i = 0, n = 2; i < n; i++) {
            ioUnits[i + 2] = new IOPort(this, (3 + i), 0, memory, 0x18 + i * 4);
            memOut[0x18 + i * 4] = ioUnits[i + 2];
            memOut[0x19 + i * 4] = ioUnits[i + 2];
            memOut[0x1a + i * 4] = ioUnits[i + 2];
            memOut[0x1b + i * 4] = ioUnits[i + 2];
            ioUnits[i + 4] = new IOPort(this, (5 + i), 0, memory, 0x30 + i * 4);
            memOut[0x30 + i * 4] = ioUnits[i + 4];
            memOut[0x31 + i * 4] = ioUnits[i + 4];
            memOut[0x32 + i * 4] = ioUnits[i + 4];
            memOut[0x33 + i * 4] = ioUnits[i + 4];
        }
        passIO = 6;
        ioUnits[passIO++] = sfr;
        ioUnits[passIO++] = bcs;
        ioUnits[passIO++] = usart0;
        ioUnits[passIO++] = usart1;
        ioUnits[passIO++] = ta;
        ioUnits[passIO++] = tb;
        ADC12 adc12 = new ADC12(this);
        ioUnits[passIO++] = adc12;
        ioUnits[passIO++] = watchdog;
        for (int i = 0, n = 16; i < n; i++) {
            memOut[0x80 + i] = adc12;
            memIn[0x80 + i] = adc12;
            memOut[0x140 + i] = adc12;
            memIn[0x140 + i] = adc12;
            memOut[0x150 + i] = adc12;
            memIn[0x150 + i] = adc12;
        }
        for (int i = 0, n = 8; i < n; i++) {
            memOut[0x1A0 + i] = adc12;
            memIn[0x1A0 + i] = adc12;
        }
        DMA dma = new DMA("dma", memory, 0, this);
        for (int i = 0, n = 24; i < n; i++) {
            memOut[0x1E0 + i] = dma;
            memIn[0x1E0 + i] = dma;
        }
        memOut[0x122] = dma;
        memIn[0x124] = dma;
        dma.setDMATrigger(DMA.URXIFG0, usart0, 0);
        dma.setDMATrigger(DMA.UTXIFG0, usart0, 1);
        dma.setDMATrigger(DMA.URXIFG1, usart1, 0);
        dma.setDMATrigger(DMA.UTXIFG1, usart1, 1);
        dma.setInterruptMultiplexer(new InterruptMultiplexer(this, 0));
        ioUnits[passIO++] = dma;
        if (DEBUG) System.out.println("Number of passive: " + passIO);
    }

    public Profiler getProfiler() {
        return profiler;
    }

    public void setProfiler(Profiler prof) {
        registry.registerComponent("profiler", prof);
        profiler = prof;
        profiler.setCPU(this);
    }

    public void setGlobalMonitor(CPUMonitor mon) {
        globalMonitor = mon;
    }

    public ComponentRegistry getRegistry() {
        return registry;
    }

    public IOPort getIOPort(int portID) {
        if (portID > 0 && portID < 7) {
            return (IOPort) ioUnits[portID - 1];
        }
        return null;
    }

    public SFR getSFR() {
        return sfr;
    }

    public void addChip(Chip chip) {
        chips.add(chip);
        chip.setEmulationLogger(logger);
    }

    public Chip getChip(String name) {
        for (Chip chip : chips) {
            if (name.equalsIgnoreCase(chip.getID()) || name.equalsIgnoreCase(chip.getName())) {
                return chip;
            }
        }
        return null;
    }

    public Chip getChip(Class<? extends Chip> type) {
        for (Chip chip : chips) {
            if (type.isInstance(chip)) {
                return chip;
            }
        }
        return null;
    }

    public Loggable[] getLoggables() {
        Loggable[] ls = new Loggable[ioUnits.length + chips.size()];
        for (int i = 0; i < ioUnits.length; i++) {
            ls[i] = ioUnits[i];
        }
        for (int i = 0; i < chips.size(); i++) {
            ls[i + ioUnits.length] = chips.get(i);
        }
        return ls;
    }

    public Loggable getLoggable(String name) {
        Loggable l = getChip(name);
        if (l == null) {
            l = getIOUnit(name);
        }
        return l;
    }

    public Chip[] getChips() {
        return chips.toArray(new Chip[chips.size()]);
    }

    public void setBreakPoint(int address, CPUMonitor mon) {
        breakPoints[address] = mon;
    }

    public boolean hasBreakPoint(int address) {
        return breakPoints[address] != null;
    }

    public void clearBreakPoint(int address) {
        breakPoints[address] = null;
    }

    public void setRegisterWriteMonitor(int r, CPUMonitor mon) {
        regWriteMonitors[r] = mon;
    }

    public void setRegisterReadMonitor(int r, CPUMonitor mon) {
        regReadMonitors[r] = mon;
    }

    public int[] getMemory() {
        return memory;
    }

    public void writeRegister(int r, int value) {
        if (regWriteMonitors[r] != null) {
            regWriteMonitors[r].cpuAction(CPUMonitor.REGISTER_WRITE, r, value);
        }
        reg[r] = value;
        if (r == SR) {
            boolean oldCpuOff = cpuOff;
            boolean oldIE = interruptsEnabled;
            interruptsEnabled = ((value & GIE) == GIE);
            if (oldIE == false && interruptsEnabled && servicedInterrupt >= 0) {
                handlePendingInterrupts();
            }
            cpuOff = ((value & CPUOFF) == CPUOFF);
            if (cpuOff != oldCpuOff) {
            }
            if (cpuOff) {
                boolean scg0 = (value & SCG0) == SCG0;
                boolean scg1 = (value & SCG1) == SCG1;
                boolean oscoff = (value & OSCOFF) == OSCOFF;
                if (oscoff && scg1 && scg0) {
                    setMode(MODE_LPM4);
                } else if (scg1 && scg0) {
                    setMode(MODE_LPM3);
                } else if (scg1) {
                    setMode(MODE_LPM2);
                } else if (scg0) {
                    setMode(MODE_LPM1);
                } else {
                    setMode(MODE_LPM0);
                }
            } else {
                setMode(MODE_ACTIVE);
            }
        }
    }

    public int readRegister(int r) {
        if (regReadMonitors[r] != null) {
            regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
        }
        return reg[r];
    }

    public int readRegisterCG(int r, int m) {
        if ((r == CG1 && m != 0) || r == CG2) {
            return CREG_VALUES[r - 2][m];
        }
        if (regReadMonitors[r] != null) {
            regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
        }
        return reg[r];
    }

    public int incRegister(int r, int value) {
        if (regReadMonitors[r] != null) {
            regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
        }
        if (regWriteMonitors[r] != null) {
            regWriteMonitors[r].cpuAction(CPUMonitor.REGISTER_WRITE, r, reg[r] + value);
        }
        reg[r] += value;
        return reg[r];
    }

    public void setACLKFrq(int frequency) {
        aclkFrq = frequency;
    }

    public void setDCOFrq(int frequency, int smclkFrq) {
        dcoFrq = frequency;
        this.smclkFrq = smclkFrq;
        lastVTime = getTime();
        lastCyclesTime = cycles;
        lastMicrosDelta = 0;
        currentDCOFactor = 1.0 * BasicClockModule.MAX_DCO_FRQ / frequency;
        if (DEBUG) System.out.println("Set smclkFrq: " + smclkFrq);
        dcoReset();
    }

    protected void dcoReset() {
    }

    public long getTime() {
        long diff = cycles - lastCyclesTime;
        return lastVTime + (long) (diff * currentDCOFactor);
    }

    private long convertVTime(long vTime) {
        long tmpTime = lastCyclesTime + (long) ((vTime - lastVTime) / currentDCOFactor);
        return tmpTime;
    }

    public double getTimeMillis() {
        return 1000.0 * getTime() / BasicClockModule.MAX_DCO_FRQ;
    }

    private void executeEvents() {
        if (cycles >= nextVTimeEventCycles) {
            if (vTimeEventQueue.eventCount == 0) {
                nextVTimeEventCycles = cycles + 10000;
            } else {
                TimeEvent te = vTimeEventQueue.popFirst();
                long now = getTime();
                te.execute(now);
                if (vTimeEventQueue.eventCount > 0) {
                    nextVTimeEventCycles = convertVTime(vTimeEventQueue.nextTime);
                } else {
                    nextVTimeEventCycles = cycles + 10000;
                }
            }
        }
        if (cycles >= nextCycleEventCycles) {
            if (cycleEventQueue.eventCount == 0) {
                nextCycleEventCycles = cycles + 10000;
            } else {
                TimeEvent te = cycleEventQueue.popFirst();
                te.execute(cycles);
                if (cycleEventQueue.eventCount > 0) {
                    nextCycleEventCycles = cycleEventQueue.nextTime;
                } else {
                    nextCycleEventCycles = cycles + 10000;
                }
            }
        }
        nextEventCycles = nextCycleEventCycles < nextVTimeEventCycles ? nextCycleEventCycles : nextVTimeEventCycles;
    }

    /**
   * Schedules a new Time event using the cycles counter
   * @param event
   * @param time
   */
    public void scheduleCycleEvent(TimeEvent event, long cycles) {
        long currentNext = cycleEventQueue.nextTime;
        cycleEventQueue.addEvent(event, cycles);
        if (currentNext != cycleEventQueue.nextTime) {
            nextCycleEventCycles = cycleEventQueue.nextTime;
            if (nextEventCycles > nextCycleEventCycles) {
                nextEventCycles = nextCycleEventCycles;
            }
        }
    }

    /**
   * Schedules a new Time event using the virtual time clock
   * @param event
   * @param time
   */
    public void scheduleTimeEvent(TimeEvent event, long time) {
        long currentNext = vTimeEventQueue.nextTime;
        vTimeEventQueue.addEvent(event, time);
        if (currentNext != vTimeEventQueue.nextTime) {
            nextVTimeEventCycles = convertVTime(vTimeEventQueue.nextTime);
            if (nextEventCycles > nextVTimeEventCycles) {
                nextEventCycles = nextVTimeEventCycles;
            }
            if (cycles > nextVTimeEventCycles) {
                logger.warning(this, "Scheduling time event backwards in time!!!");
                throw new IllegalStateException("Cycles are passed desired future time...");
            }
        }
    }

    /**
   * Schedules a new Time event msec milliseconds in the future
   * @param event
   * @param time
   */
    public long scheduleTimeEventMillis(TimeEvent event, double msec) {
        long time = (long) (getTime() + msec / 1000 * BasicClockModule.MAX_DCO_FRQ);
        scheduleTimeEvent(event, time);
        return time;
    }

    public void printEventQueues(PrintStream out) {
        out.println("Current cycles: " + cycles + "  virtual time:" + getTime());
        out.println("Cycle event queue: (next time: " + nextCycleEventCycles + ")");
        cycleEventQueue.print(out);
        out.println("Virtual time event queue: (next time: " + nextVTimeEventCycles + ")");
        vTimeEventQueue.print(out);
    }

    public IOUnit getIOUnit(String name) {
        for (int i = 0, n = ioUnits.length; i < n; i++) {
            if (name.equalsIgnoreCase(ioUnits[i].getID()) || name.equalsIgnoreCase(ioUnits[i].getName())) {
                return ioUnits[i];
            }
        }
        return null;
    }

    private void resetIOUnits() {
        for (int i = 0, n = ioUnits.length; i < n; i++) {
            ioUnits[i].reset(RESET_POR);
        }
    }

    private void internalReset() {
        for (int i = 0, n = 16; i < n; i++) {
            interruptSource[i] = null;
        }
        servicedInterruptUnit = null;
        servicedInterrupt = -1;
        interruptMax = -1;
        writeRegister(SR, 0);
        cycleEventQueue.removeAll();
        vTimeEventQueue.removeAll();
        bcs.reset();
        for (Chip chip : chips) {
            chip.notifyReset();
        }
        resetIOUnits();
        if (profiler != null) {
            profiler.resetProfile();
        }
    }

    public void setWarningMode(EmulationLogger.WarningMode mode) {
        if (logger != null) {
            logger.setWarningMode(mode);
        }
    }

    public void reset() {
        flagInterrupt(MAX_INTERRUPT, null, true);
    }

    public void flagInterrupt(int interrupt, InterruptHandler source, boolean triggerIR) {
        if (triggerIR) {
            interruptSource[interrupt] = source;
            if (debugInterrupts) {
                if (source != null) {
                    System.out.println("### Interrupt " + interrupt + " flagged ON by " + source.getName() + " prio: " + interrupt);
                } else {
                    System.out.println("### Interrupt " + interrupt + " flagged ON by <null>");
                }
            }
            if (interrupt > interruptMax) {
                interruptMax = interrupt;
                if (interruptMax == MAX_INTERRUPT) {
                    interruptsEnabled = true;
                }
            }
        } else {
            if (interruptSource[interrupt] == source) {
                if (debugInterrupts) {
                    System.out.println("### Interrupt flagged OFF by " + source.getName() + " prio: " + interrupt);
                }
                interruptSource[interrupt] = null;
                reevaluateInterrupts();
            }
        }
    }

    private void reevaluateInterrupts() {
        interruptMax = -1;
        for (int i = 0; i < interruptSource.length; i++) {
            if (interruptSource[i] != null) interruptMax = i;
        }
    }

    public int getServicedInterrupt() {
        return servicedInterrupt;
    }

    public void handlePendingInterrupts() {
        reevaluateInterrupts();
        servicedInterrupt = -1;
        servicedInterruptUnit = null;
    }

    public int read(int address, boolean word) throws EmulationException {
        int val = 0;
        if (address < 0x1ff && memIn[address] != null) {
            val = memIn[address].read(address, word, cycles);
        } else {
            address &= 0xffff;
            if (isFlashBusy && flash.addressInFlash(address)) {
                flash.notifyRead(address);
            }
            val = memory[address] & 0xff;
            if (word) {
                val |= (memory[(address + 1) & 0xffff] << 8);
                if ((address & 1) != 0) {
                    printWarning(MISALIGNED_READ, address);
                }
            }
        }
        if (breakPoints[address] != null) {
            breakPoints[address].cpuAction(CPUMonitor.MEMORY_READ, address, val);
        }
        if (globalMonitor != null) {
            globalMonitor.cpuAction(CPUMonitor.MEMORY_READ, address, val);
        }
        return val;
    }

    public void write(int dstAddress, int dst, boolean word) throws EmulationException {
        if (breakPoints[dstAddress] != null) {
            breakPoints[dstAddress].cpuAction(CPUMonitor.MEMORY_WRITE, dstAddress, dst);
        }
        if (dstAddress < 0x1ff && memOut[dstAddress] != null) {
            if (!word) dst &= 0xff;
            memOut[dstAddress].write(dstAddress, dst, word, cycles);
        } else if (flash.addressInFlash(dstAddress)) {
            flash.flashWrite(dstAddress, dst, word);
        } else {
            memory[dstAddress] = dst & 0xff;
            if (word) {
                memory[dstAddress + 1] = (dst >> 8) & 0xff;
                if ((dstAddress & 1) != 0) {
                    printWarning(MISALIGNED_WRITE, dstAddress);
                }
            }
        }
        if (globalMonitor != null) {
            globalMonitor.cpuAction(CPUMonitor.MEMORY_WRITE, dstAddress, dst);
        }
    }

    void printWarning(int type, int address) throws EmulationException {
        String message = null;
        switch(type) {
            case MISALIGNED_READ:
                message = "**** Illegal read - misaligned word from $" + Utils.hex16(address) + " at $" + Utils.hex16(reg[PC]);
                break;
            case MISALIGNED_WRITE:
                message = "**** Illegal write - misaligned word to $" + Utils.hex16(address) + " at $" + Utils.hex16(reg[PC]);
                break;
        }
        if (logger != null && message != null) {
            logger.warning(this, message);
        }
    }

    public void generateTrace(PrintStream out) {
    }

    private int serviceInterrupt(int pc) {
        int pcBefore = pc;
        int spBefore = readRegister(SP);
        int sp = spBefore;
        int sr = readRegister(SR);
        if (profiler != null) {
            profiler.profileInterrupt(interruptMax, cycles);
        }
        if (flash.blocksCPU()) {
            throw new IllegalStateException("Got interrupt while flash controller blocks CPU. CPU CRASHED.");
        }
        if (interruptMax < MAX_INTERRUPT) {
            writeRegister(SP, sp = spBefore - 2);
            write(sp, pc, true);
            writeRegister(SP, sp = sp - 2);
            write(sp, sr, true);
        }
        writeRegister(SR, 0);
        writeRegister(PC, pc = read(0xfffe - (MAX_INTERRUPT - interruptMax) * 2, true));
        servicedInterrupt = interruptMax;
        servicedInterruptUnit = interruptSource[servicedInterrupt];
        reevaluateInterrupts();
        if (servicedInterrupt == MAX_INTERRUPT) {
            if (debugInterrupts) System.out.println("**** Servicing RESET! => " + Utils.hex16(pc));
            internalReset();
        }
        cycles += 6;
        if (debugInterrupts) {
            System.out.println("### Executing interrupt: " + servicedInterrupt + " at " + pcBefore + " to " + pc + " SP before: " + spBefore + " Vector: " + Utils.hex16(0xfffe - (MAX_INTERRUPT - servicedInterrupt) * 2));
        }
        if (servicedInterruptUnit != null) {
            if (debugInterrupts) {
                System.out.println("### Calling serviced interrupt on: " + servicedInterruptUnit.getName());
            }
            servicedInterruptUnit.interruptServiced(servicedInterrupt);
        }
        return pc;
    }

    public boolean emulateOP(long maxCycles) throws EmulationException {
        int pc = readRegister(PC);
        long startCycles = cycles;
        if (interruptsEnabled && servicedInterrupt == -1 && interruptMax >= 0) {
            pc = serviceInterrupt(pc);
        }
        if (cpuOff || flash.blocksCPU()) {
            while (cycles >= nextEventCycles) {
                executeEvents();
            }
            if (interruptsEnabled && interruptMax > 0) {
                return false;
            }
            if (maxCycles >= 0 && maxCycles < nextEventCycles) {
                cycles = cycles < maxCycles ? maxCycles : cycles;
            } else {
                cycles = nextEventCycles;
            }
            return false;
        }
        if (breakPoints[pc] != null) {
            if (breakpointActive) {
                breakPoints[pc].cpuAction(CPUMonitor.EXECUTE, pc, 0);
                breakpointActive = false;
                return false;
            }
            breakpointActive = true;
        }
        if (globalMonitor != null) {
            globalMonitor.cpuAction(CPUMonitor.EXECUTE, pc, 0);
        }
        instruction = read(pc, true);
        op = instruction >> 12;
        int sp = 0;
        int sr = 0;
        boolean word = (instruction & 0x40) == 0;
        int dstRegister = 0;
        int dstAddress = -1;
        boolean dstRegMode = false;
        int dst = 0;
        boolean write = false;
        boolean updateStatus = true;
        pc += 2;
        writeRegister(PC, pc);
        switch(op) {
            case 0:
                op = instruction & 0xf0f0;
                System.out.println("Executing MSP430X instruction op:" + Utils.hex16(op) + " ins:" + Utils.hex16(instruction) + " PC = " + Utils.hex16(pc - 2));
                int src = 0;
                int srcData = (instruction & 0x0f00) >> 8;
                int dstData = (instruction & 0x000f);
                switch(op) {
                    case MOVA_IMM2REG:
                        src = read(pc, true);
                        writeRegister(PC, pc += 2);
                        dst = src + (srcData << 16);
                        System.out.println("*** Writing $" + Utils.hex16(dst) + " to reg: " + dstData);
                        writeRegister(dstData, dst);
                        break;
                    default:
                        System.out.println("MSP430X instructions not yet supported...");
                }
                break;
            case 1:
                {
                    dstRegister = instruction & 0xf;
                    int ad = (instruction >> 4) & 3;
                    int nxtCarry = 0;
                    op = instruction & 0xff80;
                    if (op == PUSH || op == CALL) {
                        sp = readRegister(SP) - 2;
                        writeRegister(SP, sp);
                    }
                    if ((dstRegister == CG1 && ad > AM_INDEX) || dstRegister == CG2) {
                        dstRegMode = true;
                        cycles++;
                    } else {
                        switch(ad) {
                            case AM_REG:
                                dstRegMode = true;
                                cycles++;
                                break;
                            case AM_INDEX:
                                dstAddress = readRegisterCG(dstRegister, ad) + read(pc, true);
                                pc += 2;
                                writeRegister(PC, pc);
                                cycles += 4;
                                break;
                            case AM_IND_REG:
                                dstAddress = readRegister(dstRegister);
                                cycles += 3;
                                break;
                            case AM_IND_AUTOINC:
                                if (dstRegister == PC) {
                                    dstAddress = readRegister(PC);
                                    pc += 2;
                                    writeRegister(PC, pc);
                                } else {
                                    dstAddress = readRegister(dstRegister);
                                    writeRegister(dstRegister, dstAddress + (word ? 2 : 1));
                                }
                                cycles += 3;
                                break;
                        }
                    }
                    if (dstRegMode) {
                        dst = readRegisterCG(dstRegister, ad);
                        if (!word) {
                            dst &= 0xff;
                        }
                    } else {
                        dst = read(dstAddress, word);
                    }
                    switch(op) {
                        case RRC:
                            nxtCarry = (dst & 1) > 0 ? CARRY : 0;
                            dst = dst >> 1;
                            if (word) {
                                dst |= (readRegister(SR) & CARRY) > 0 ? 0x8000 : 0;
                            } else {
                                dst |= (readRegister(SR) & CARRY) > 0 ? 0x80 : 0;
                            }
                            write = true;
                            writeRegister(SR, (readRegister(SR) & ~(CARRY | OVERFLOW)) | nxtCarry);
                            break;
                        case SWPB:
                            int tmp = dst;
                            dst = ((tmp >> 8) & 0xff) + ((tmp << 8) & 0xff00);
                            write = true;
                            break;
                        case RRA:
                            nxtCarry = (dst & 1) > 0 ? CARRY : 0;
                            if (word) {
                                dst = (dst & 0x8000) | (dst >> 1);
                            } else {
                                dst = (dst & 0x80) | (dst >> 1);
                            }
                            write = true;
                            writeRegister(SR, (readRegister(SR) & ~(CARRY | OVERFLOW)) | nxtCarry);
                            break;
                        case SXT:
                            sr = readRegister(SR);
                            dst = (dst & 0x80) > 0 ? dst | 0xff00 : dst & 0x7f;
                            write = true;
                            sr = sr & ~(CARRY | OVERFLOW);
                            if (dst != 0) {
                                sr |= CARRY;
                            }
                            writeRegister(SR, sr);
                            break;
                        case PUSH:
                            if (word) {
                                write(sp, dst, true);
                            } else {
                                write(sp, dst & 0xff, true);
                            }
                            cycles += (ad == AM_REG || ad == AM_IND_AUTOINC) ? 2 : 1;
                            write = false;
                            updateStatus = false;
                            break;
                        case CALL:
                            pc = readRegister(PC);
                            write(sp, pc, true);
                            writeRegister(PC, dst);
                            cycles += (ad == AM_REG) ? 3 : (ad == AM_IND_AUTOINC) ? 2 : 1;
                            if (profiler != null) {
                                MapEntry function = map.getEntry(dst);
                                if (function == null) {
                                    function = getFunction(map, dst);
                                }
                                profiler.profileCall(function, cpuCycles, pc);
                            }
                            write = false;
                            updateStatus = false;
                            break;
                        case RETI:
                            servicedInterrupt = -1;
                            sp = readRegister(SP);
                            writeRegister(SR, read(sp, true));
                            sp = sp + 2;
                            writeRegister(PC, read(sp, true));
                            sp = sp + 2;
                            writeRegister(SP, sp);
                            write = false;
                            updateStatus = false;
                            cycles += 4;
                            if (debugInterrupts) {
                                System.out.println("### RETI at " + pc + " => " + reg[PC] + " SP after: " + reg[SP]);
                            }
                            if (profiler != null) {
                                profiler.profileRETI(cycles);
                            }
                            handlePendingInterrupts();
                            break;
                        default:
                            System.out.println("Error: Not implemented instruction:" + instruction);
                    }
                }
                break;
            case 2:
            case 3:
                int jmpOffset = instruction & 0x3ff;
                jmpOffset = (jmpOffset & 0x200) == 0 ? 2 * jmpOffset : -(2 * (0x200 - (jmpOffset & 0x1ff)));
                boolean jump = false;
                cycles += 2;
                sr = readRegister(SR);
                switch(instruction & 0xfc00) {
                    case JNE:
                        jump = (sr & ZERO) == 0;
                        break;
                    case JEQ:
                        jump = (sr & ZERO) > 0;
                        break;
                    case JNC:
                        jump = (sr & CARRY) == 0;
                        break;
                    case JC:
                        jump = (sr & CARRY) > 0;
                        break;
                    case JN:
                        jump = (sr & NEGATIVE) > 0;
                        break;
                    case JGE:
                        jump = (sr & NEGATIVE) > 0 == (sr & OVERFLOW) > 0;
                        break;
                    case JL:
                        jump = (sr & NEGATIVE) > 0 != (sr & OVERFLOW) > 0;
                        break;
                    case JMP:
                        jump = true;
                        break;
                    default:
                        System.out.println("Not implemented instruction: " + Utils.binary16(instruction));
                }
                if (jump) {
                    writeRegister(PC, pc + jmpOffset);
                }
                updateStatus = false;
                break;
            default:
                dstRegister = instruction & 0xf;
                int srcRegister = (instruction >> 8) & 0xf;
                int as = (instruction >> 4) & 3;
                dstRegMode = ((instruction >> 7) & 1) == 0;
                dstAddress = -1;
                int srcAddress = -1;
                src = 0;
                if ((srcRegister == CG1 && as > AM_INDEX) || srcRegister == CG2) {
                    src = CREG_VALUES[srcRegister - 2][as];
                    if (!word) {
                        src &= 0xff;
                    }
                    cycles += dstRegMode ? 1 : 4;
                } else {
                    switch(as) {
                        case AM_REG:
                            src = readRegister(srcRegister);
                            if (!word) {
                                src &= 0xff;
                            }
                            cycles += dstRegMode ? 1 : 4;
                            if (dstRegister == PC) cycles++;
                            break;
                        case AM_INDEX:
                            srcAddress = readRegisterCG(srcRegister, as) + read(pc, true);
                            incRegister(PC, 2);
                            cycles += dstRegMode ? 3 : 6;
                            break;
                        case AM_IND_REG:
                            srcAddress = readRegister(srcRegister);
                            cycles += dstRegMode ? 2 : 5;
                            break;
                        case AM_IND_AUTOINC:
                            if (srcRegister == PC) {
                                srcAddress = readRegister(PC);
                                pc += 2;
                                incRegister(PC, 2);
                                cycles += dstRegMode ? 2 : 5;
                            } else {
                                srcAddress = readRegister(srcRegister);
                                incRegister(srcRegister, word ? 2 : 1);
                                cycles += dstRegMode ? 2 : 5;
                            }
                            if (dstRegister == PC) {
                                cycles++;
                            }
                            break;
                    }
                }
                if (dstRegMode) {
                    if (op != MOV) {
                        dst = readRegister(dstRegister);
                        if (!word) {
                            dst &= 0xff;
                        }
                    }
                } else {
                    pc = readRegister(PC);
                    if (dstRegister == 2) {
                        dstAddress = read(pc, true);
                    } else {
                        dstAddress = readRegister(dstRegister) + read(pc, true);
                    }
                    if (op != MOV) dst = read(dstAddress, word);
                    pc += 2;
                    incRegister(PC, 2);
                }
                if (srcAddress != -1) {
                    srcAddress = srcAddress & 0xffff;
                    src = read(srcAddress, word);
                }
                int tmp = 0;
                int tmpAdd = 0;
                switch(op) {
                    case MOV:
                        dst = src;
                        write = true;
                        updateStatus = false;
                        if (instruction == RETURN && profiler != null) {
                            profiler.profileReturn(cpuCycles);
                        }
                        break;
                    case SUB:
                        tmpAdd = 1;
                    case SUBC:
                        src = (src ^ 0xffff) & 0xffff;
                    case ADDC:
                        if (op == ADDC || op == SUBC) tmpAdd = ((readRegister(SR) & CARRY) > 0) ? 1 : 0;
                    case ADD:
                        sr = readRegister(SR);
                        sr &= ~(OVERFLOW | CARRY);
                        tmp = (src ^ dst) & (word ? 0x8000 : 0x80);
                        dst = dst + src + tmpAdd;
                        if (dst > (word ? 0xffff : 0xff)) {
                            sr |= CARRY;
                        }
                        if (tmp == 0 && ((src ^ dst) & (word ? 0x8000 : 0x80)) != 0) {
                            sr |= OVERFLOW;
                        }
                        writeRegister(SR, sr);
                        write = true;
                        break;
                    case CMP:
                        int b = word ? 0x8000 : 0x80;
                        sr = readRegister(SR);
                        sr = (sr & ~(CARRY | OVERFLOW)) | (dst >= src ? CARRY : 0);
                        tmp = (dst - src);
                        if (((src ^ tmp) & b) == 0 && (((src ^ dst) & b) != 0)) {
                            sr |= OVERFLOW;
                        }
                        writeRegister(SR, sr);
                        dst = tmp;
                        break;
                    case DADD:
                        if (DEBUG) System.out.println("DADD: Decimal add executed - result error!!!");
                        dst = dst + src + ((readRegister(SR) & CARRY) > 0 ? 1 : 0);
                        write = true;
                        break;
                    case BIT:
                        dst = src & dst;
                        sr = readRegister(SR);
                        sr = sr & ~(CARRY | OVERFLOW);
                        if (dst != 0) {
                            sr |= CARRY;
                        }
                        writeRegister(SR, sr);
                        break;
                    case BIC:
                        dst = (~src) & dst;
                        write = true;
                        updateStatus = false;
                        break;
                    case BIS:
                        dst = src | dst;
                        write = true;
                        updateStatus = false;
                        break;
                    case XOR:
                        sr = readRegister(SR);
                        sr = sr & ~(CARRY | OVERFLOW);
                        if ((src & (word ? 0x8000 : 0x80)) != 0 && (dst & (word ? 0x8000 : 0x80)) != 0) {
                            sr |= OVERFLOW;
                        }
                        dst = src ^ dst;
                        if (dst != 0) {
                            sr |= CARRY;
                        }
                        write = true;
                        writeRegister(SR, sr);
                        break;
                    case AND:
                        sr = readRegister(SR);
                        sr = sr & ~(CARRY | OVERFLOW);
                        dst = src & dst;
                        if (dst != 0) {
                            sr |= CARRY;
                        }
                        write = true;
                        writeRegister(SR, sr);
                        break;
                    default:
                        System.out.println("DoubleOperand not implemented: op = " + op + " at " + pc);
                        if (EXCEPTION_ON_BAD_OPERATION) {
                            EmulationException ex = new EmulationException("Bad operation: " + op + " at " + pc);
                            ex.initCause(new Throwable("" + pc));
                            throw ex;
                        }
                }
        }
        if (word) {
            dst &= 0xffff;
        } else {
            dst &= 0xff;
        }
        if (write) {
            if (dstRegMode) {
                writeRegister(dstRegister, dst);
            } else {
                dstAddress &= 0xffff;
                write(dstAddress, dst, word);
            }
        }
        if (updateStatus) {
            sr = readRegister(SR);
            sr = (sr & ~(ZERO | NEGATIVE)) | ((dst == 0) ? ZERO : 0) | (word ? ((dst & 0x8000) > 0 ? NEGATIVE : 0) : ((dst & 0x80) > 0 ? NEGATIVE : 0));
            writeRegister(SR, sr);
        }
        while (cycles >= nextEventCycles) {
            executeEvents();
        }
        cpuCycles += cycles - startCycles;
        return true;
    }

    public int getModeMax() {
        return MODE_MAX;
    }

    MapEntry getFunction(MapTable map, int address) {
        MapEntry function = new MapEntry(MapEntry.TYPE.function, address, 0, "fkn at $" + Utils.hex16(address), null, true);
        map.setEntry(function);
        return function;
    }

    public int getPC() {
        return reg[PC];
    }

    public int getConfiguration(int parameter) {
        return 0;
    }

    public String info() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 16; i++) {
            buf.append("Vector: at $" + Utils.hex16(0xfffe - i * 2) + " -> $" + Utils.hex16(read(0xfffe - i * 2, true)) + "\n");
        }
        return buf.toString();
    }
}
