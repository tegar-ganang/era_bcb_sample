package com.gochromium.nes.client.emulator;

import java.io.InputStream;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

/** 
 *
 * Class for the CPU Controller required by the NESCafe NES Emulator.
 *
 * @author   David de Niese
 * @version  0.56f
 * @final    TRUE
 *
 */
public final class CPU {

    /**
     *
     * <P>The current NES Machine.</P>
     *
     */
    private final NES nes;

    private final TVController tvController;

    /**
     *
     * <P>Whether this CPU Controller can request Screen Drawing.</P>
     *
     */
    private boolean allowDrawScreen = true;

    /**
     *
     * <P>True if the CPU is active.</P>
     *
     */
    protected boolean cpuActive = false;

    /**
     *
     * <P>True if the CPU is paused.</P>
     *
     */
    protected boolean cpuPaused = false;

    /**
     *
     * <P>True if the CPU is running instructions.</P>
     *
     *
     */
    protected boolean cpuRunning = false;

    /**
     *
     * <P>The current Cartridge ROM Image.</P>
     *
     */
    protected NESCart currentCart;

    /**
     *
     * 6502 Register : The Accumulator (8 bit)
     *
     */
    private int A;

    /**
     *
     * 6502 Register : X (8 bits)
     *
     */
    private int X;

    /**
     *
     * 6502 Register : Y (8 bits)
     *
     */
    private int Y;

    /**
     *
     * 6502 Register : The Processor Status Register (8 bits)
     *
     */
    private int P;

    /**
     *
     * 6502 Register : The Stack Index Register (8 bits)
     *
     */
    private int S;

    /**
     *
     * 6502 Register : The Program Counter Register (16 bits)
     *
     */
    private int PC;

    /**
     *
     * <P>The number of CPU Cycles until the next Horizontal Blank.</P>
     *
     */
    private float cyclesPending;

    /**
     *
     * <P>The number of CPU Cycles between Horizontal Blanks.</P>
     *
     */
    public float CYCLES_PER_LINE = 116.0f;

    /**
     *
     * <P>True if a stop request has been issued.</P>
     *
     */
    private boolean stopRequest = false;

    /**
     *
     * <P>Halt Instruction has been fetched.</P>
     *
     */
    private boolean halted = false;

    /**
     *
     * Debug Mode
     *
     */
    public boolean debug = false;

    /**
     *
     * <P>Create a new NES CPU Controller.</P>
     *
     * @param Nes The current NES Machine.
     * @param Gui The current Graphical User Interface.
     *
     */
    public CPU(NES nes, TVController tvController) {
        this.nes = nes;
        this.tvController = tvController;
    }

    Timer timer = new Timer() {

        @Override
        public void run() {
            if (everyY > everyX) {
                timeFrameStop = System.currentTimeMillis() + waitPeriod;
                everyY = 1;
            }
            emulateFrame();
            if (counter != 0) {
                if (++counter > 32) {
                    counter = 0;
                }
            }
            everyY++;
        }
    };

    final int everyX = 5;

    final int idealFrameRate = 60;

    final int waitPeriod = (1000 / idealFrameRate) * everyX;

    long timeFrameStop = System.currentTimeMillis() + waitPeriod;

    int everyY = 1;

    int counter = 1;

    /**
     *
     * <P>Method to Eat up CPU Cycles.</P>
     *
     */
    public final void eatCycles(int cycles) {
        cyclesPending -= cycles;
    }

    /**
     *
     * <P>Load a Cartridge ROM for the CPU.</P>
     *
     * @param fileName The filename of the ROM image.
     *
     */
    public final void cpuLoadRom(InputStream is) {
        currentCart = new NESCart();
        boolean fail = currentCart.loadRom(is);
        if (fail) {
            String errString = "Error Reading ROM";
            switch(currentCart.getErrorCode()) {
                case NESCart.ERROR_IO:
                    errString = "Cannot Read ROM";
                    break;
                case NESCart.ERROR_FILE_FORMAT:
                    errString = "Invalid ROM";
                    break;
                case NESCart.ERROR_UNSUPPORTED_MAPPER:
                    errString = "Unsupported Mapper";
                    break;
            }
            currentCart = null;
            throw new RuntimeException(errString);
        }
        cpuStop();
        nes.memory.init(currentCart);
        nes.mapper = currentCart.mapper;
        if (nes.mapper == null) {
            currentCart = null;
            String errString = "The Hardware could not be located for the Cartridge.";
            throw new RuntimeException(errString);
        }
        nes.ppu.latchMapper = false;
        nes.mapper.init(nes.memory);
        nes.mapper.setCRC(currentCart.crc32);
        if (currentCart.getMapperNumber() > 0) {
        } else {
        }
    }

    /**
     *
     * <P>Run the CPU.</P>
     *
     */
    public final void cpuRun() {
        cpuActive = true;
        cpuRunning = true;
        intReset();
        stopRequest = false;
        timer.scheduleRepeating(1);
    }

    /**
     *
     * <P>Stop the current CPU.</P>
     *
     */
    public final void cpuStop() {
        if (!cpuRunning) {
            return;
        }
        stopProcessing();
        cpuRunning = false;
    }

    /**
     *
     * Halt the CPU
     *
     */
    public final boolean isCPUHalted() {
        return halted;
    }

    /**
     *
     * <P>Request that the Processor performs a NMI
     *
     */
    public final void cpuNMI() {
        NMI();
    }

    /**
     *
     * <P>Ask the TV Controller to draw the Screen.</P>
     *
     * @param force True to force a draw.
     *
     */
    public final synchronized void drawScreen(boolean force) {
        if (!allowDrawScreen) {
            return;
        }
        tvController.drawScreen(force);
    }

    /**
     *
     * Clears the Display.
     *
     */
    private final void deleteScreen() {
    }

    /**
     *
     * <P>Emulate a Frame.</P>
     *
     */
    private final void emulateFrame() {
        PPU localPPU = nes.ppu;
        Mapper localMapper = nes.mapper;
        localPPU.startFrame();
        for (int i = 0; i < 240; i++) {
            emulateCPUCycles(CYCLES_PER_LINE);
            if (localMapper.syncH(i) != 0) {
                IRQ();
            }
            localPPU.drawScanLine();
        }
        if ((nes.frameIRQEnabled & 0xC0) == 0) {
            IRQ();
        }
        for (int i = 240; i < 262; i++) {
            if (i == 261) {
                localPPU.endVBlank();
            }
            if (i == 241) {
                localPPU.startVBlank();
                localMapper.syncV();
                emulateCPUCycles(1);
                if (localPPU.nmiEnabled()) {
                    NMI();
                }
                emulateCPUCycles(CYCLES_PER_LINE - 1);
                if (localMapper.syncH(i) != 0) {
                    IRQ();
                }
            }
            emulateCPUCycles(CYCLES_PER_LINE);
            if (localMapper.syncH(i) != 0) {
                IRQ();
            }
        }
        drawScreen(false);
    }

    private final void emulateFrame2() {
        nes.ppu.startFrame();
        for (int i = 0; i < 240; i++) {
            emulateCPUCycles(CYCLES_PER_LINE);
            if (nes.mapper.syncH(i) != 0) {
                IRQ();
            }
            nes.ppu.drawScanLine();
        }
        if ((nes.frameIRQEnabled & 0xC0) == 0) {
            IRQ();
        }
        for (int i = 240; i <= 261; i++) {
            if (i == 261) {
                nes.ppu.endVBlank();
            }
            if (i == 241) {
                nes.ppu.startVBlank();
                nes.mapper.syncV();
                emulateCPUCycles(1);
                if (nes.ppu.nmiEnabled()) {
                    NMI();
                }
                emulateCPUCycles(CYCLES_PER_LINE - 1);
                if (nes.mapper.syncH(i) != 0) {
                    IRQ();
                }
            }
            emulateCPUCycles(CYCLES_PER_LINE);
            if (nes.mapper.syncH(i) != 0) {
                IRQ();
            }
        }
        drawScreen(false);
    }

    /**
     *
     * <P>Perform a Non Maskable Interrupt.</P>
     *
     */
    public final void NMI() {
        pushWord(PC);
        push(P & 0xEF);
        PC = readWord(0xFFFA);
        cyclesPending += 7;
    }

    /**
     *
     * <P>Perform a IRQ/BRK Interrupt.</P>
     *
     */
    public final void IRQ() {
        if ((P & 0x4) == 0x00) {
            pushWord(PC);
            push(P & 0xEF);
            PC = readWord(0xFFFE);
            P |= 0x04;
            cyclesPending += 7;
        }
    }

    /**
     *
     * <P>Emulate until the next Horizontal Blank is encountered.</P>
     *
     */
    public final void emulateCPUCycles(float cycles) {
        cyclesPending += cycles;
        while (cyclesPending > 0) {
            if (!halted) {
                instructionFetchExecute();
            } else {
                cyclesPending--;
            }
        }
    }

    /**
     *
     * Halt the CPU
     *
     */
    public final void haltCPU() {
        halted = true;
    }

    /**
    *
    * <P>Fetch and Execute the next Instruction.</P>
    *
    */
    private final void instructionFetchExecute() {
        MemoryManager memory = nes.memory;
        int instCode = memory.read(PC++);
        int address;
        int writeVal;
        switch(instCode) {
            case 0x00:
                address = PC + 1;
                pushWord(address);
                push(P | 0x10);
                PC = readWord(0xFFFE);
                P |= 0x04;
                P |= 0x10;
                break;
            case 0xA9:
                A = byImmediate();
                setStatusFlags(A);
                break;
            case 0xA5:
                A = memory.read(byZeroPage());
                setStatusFlags(A);
                break;
            case 0xB5:
                A = memory.read(byZeroPageX());
                setStatusFlags(A);
                break;
            case 0xAD:
                A = memory.read(byAbsolute());
                setStatusFlags(A);
                break;
            case 0xBD:
                A = memory.read(byAbsoluteX());
                setStatusFlags(A);
                break;
            case 0xB9:
                A = memory.read(byAbsoluteY());
                setStatusFlags(A);
                break;
            case 0xA1:
                A = memory.read(byIndirectX());
                setStatusFlags(A);
                break;
            case 0xB1:
                A = memory.read(byIndirectY());
                setStatusFlags(A);
                break;
            case 0xA2:
                X = byImmediate();
                setStatusFlags(X);
                break;
            case 0xA6:
                X = memory.read(byZeroPage());
                setStatusFlags(X);
                break;
            case 0xB6:
                X = memory.read(byZeroPageY());
                setStatusFlags(X);
                break;
            case 0xAE:
                X = memory.read(byAbsolute());
                setStatusFlags(X);
                break;
            case 0xBE:
                X = memory.read(byAbsoluteY());
                setStatusFlags(X);
                break;
            case 0xA0:
                Y = byImmediate();
                setStatusFlags(Y);
                break;
            case 0xA4:
                Y = memory.read(byZeroPage());
                setStatusFlags(Y);
                break;
            case 0xB4:
                Y = memory.read(byZeroPageX());
                setStatusFlags(Y);
                break;
            case 0xAC:
                Y = memory.read(byAbsolute());
                setStatusFlags(Y);
                break;
            case 0xBC:
                Y = memory.read(byAbsoluteX());
                setStatusFlags(Y);
                break;
            case 0x85:
                address = byZeroPage();
                write(address, A);
                break;
            case 0x95:
                address = byZeroPageX();
                write(address, A);
                break;
            case 0x8D:
                address = byAbsolute();
                write(address, A);
                break;
            case 0x9D:
                address = byAbsoluteX();
                write(address, A);
                break;
            case 0x99:
                address = byAbsoluteY();
                write(address, A);
                break;
            case 0x81:
                address = byIndirectX();
                write(address, A);
                break;
            case 0x91:
                address = byIndirectY();
                write(address, A);
                break;
            case 0x86:
                address = byZeroPage();
                write(address, X);
                break;
            case 0x96:
                address = byZeroPageY();
                write(address, X);
                break;
            case 0x8E:
                address = byAbsolute();
                write(address, X);
                break;
            case 0x84:
                address = byZeroPage();
                write(address, Y);
                break;
            case 0x94:
                address = byZeroPageX();
                write(address, Y);
                break;
            case 0x8C:
                address = byAbsolute();
                write(address, Y);
                break;
            case 0xAA:
                X = A;
                setStatusFlags(X);
                break;
            case 0xA8:
                Y = A;
                setStatusFlags(Y);
                break;
            case 0xBA:
                X = S & 0xFF;
                setStatusFlags(X);
                break;
            case 0x8A:
                A = X;
                setStatusFlags(A);
                break;
            case 0x9A:
                S = X & 0XFF;
                break;
            case 0x98:
                A = Y;
                setStatusFlags(A);
                break;
            case 0x09:
                A |= byImmediate();
                setStatusFlags(A);
                break;
            case 0x05:
                address = byZeroPage();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x15:
                address = byZeroPageX();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x0D:
                address = byAbsolute();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x1D:
                address = byAbsoluteX();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x19:
                address = byAbsoluteY();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x01:
                address = byIndirectX();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x11:
                address = byIndirectY();
                A |= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x29:
                A &= byImmediate();
                setStatusFlags(A);
                break;
            case 0x25:
                address = byZeroPage();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x35:
                address = byZeroPageX();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x2D:
                address = byAbsolute();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x3D:
                address = byAbsoluteX();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x39:
                address = byAbsoluteY();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x21:
                address = byIndirectX();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x31:
                address = byIndirectY();
                A &= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x49:
                A ^= byImmediate();
                setStatusFlags(A);
                break;
            case 0x45:
                address = byZeroPage();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x55:
                address = byZeroPageX();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x4D:
                address = byAbsolute();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x5D:
                address = byAbsoluteX();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x59:
                address = byAbsoluteY();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x41:
                address = byIndirectX();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x51:
                address = byIndirectY();
                A ^= memory.read(address);
                setStatusFlags(A);
                break;
            case 0x24:
                operateBit(read(byZeroPage()));
                break;
            case 0x2C:
                operateBit(read(byAbsolute()));
                break;
            case 0x0A:
                A = ASL(A);
                break;
            case 0x06:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ASL(writeVal);
                write(address, writeVal);
                break;
            case 0x16:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ASL(writeVal);
                write(address, writeVal);
                break;
            case 0x0E:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ASL(writeVal);
                write(address, writeVal);
                break;
            case 0x1E:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ASL(writeVal);
                write(address, writeVal);
                break;
            case 0x4A:
                A = LSR(A);
                break;
            case 0x46:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = LSR(writeVal);
                write(address, writeVal);
                break;
            case 0x56:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = LSR(writeVal);
                write(address, writeVal);
                break;
            case 0x4E:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = LSR(writeVal);
                write(address, writeVal);
                break;
            case 0x5E:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = LSR(writeVal);
                write(address, writeVal);
                break;
            case 0x2A:
                A = ROL(A);
                break;
            case 0x26:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROL(writeVal);
                write(address, writeVal);
                break;
            case 0x36:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROL(writeVal);
                write(address, writeVal);
                break;
            case 0x2E:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROL(writeVal);
                write(address, writeVal);
                break;
            case 0x3E:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROL(writeVal);
                write(address, writeVal);
                break;
            case 0x6A:
                A = ROR(A);
                break;
            case 0x66:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROR(writeVal);
                write(address, writeVal);
                break;
            case 0x76:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROR(writeVal);
                write(address, writeVal);
                break;
            case 0x6E:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROR(writeVal);
                write(address, writeVal);
                break;
            case 0x7E:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = ROR(writeVal);
                write(address, writeVal);
                break;
            case 0x90:
                branch(0x01, false);
                break;
            case 0xB0:
                branch(0x01, true);
                break;
            case 0xD0:
                branch(0x02, false);
                break;
            case 0xF0:
                branch(0x02, true);
                break;
            case 0x10:
                branch(0x80, false);
                break;
            case 0x30:
                branch(0x80, true);
                break;
            case 0x50:
                branch(0x40, false);
                break;
            case 0x70:
                branch(0x40, true);
                break;
            case 0x4C:
                PC = byAbsolute();
                break;
            case 0x6C:
                address = byAbsolute();
                if ((address & 0x00FF) == 0xFF) {
                    PC = (read(address & 0xFF00) << 8) | read(address);
                } else {
                    PC = readWord(address);
                }
                break;
            case 0x20:
                address = PC + 1;
                pushWord(address);
                PC = byAbsolute();
                break;
            case 0x60:
                PC = popWord() + 1;
                break;
            case 0x40:
                P = pop();
                PC = popWord();
                break;
            case 0x48:
                push(A);
                break;
            case 0x08:
                push(P | 0x10);
                break;
            case 0x68:
                A = pop();
                setStatusFlags(A);
                break;
            case 0x28:
                P = pop();
                break;
            case 0x18:
                P &= 0xfe;
                break;
            case 0xD8:
                P &= 0xf7;
                break;
            case 0x58:
                P &= 0xfb;
                break;
            case 0xB8:
                P &= 0xbf;
                break;
            case 0x38:
                P |= 0x1;
                break;
            case 0xF8:
                P |= 0x8;
                break;
            case 0x78:
                P |= 0x4;
                break;
            case 0xE6:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = increment(writeVal);
                write(address, writeVal);
                break;
            case 0xF6:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = increment(read(address));
                write(address, writeVal);
                break;
            case 0xEE:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = increment(read(address));
                write(address, writeVal);
                break;
            case 0xFE:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = increment(read(address));
                write(address, writeVal);
                break;
            case 0xE8:
                X++;
                X &= 0xff;
                setStatusFlags(X);
                break;
            case 0xC8:
                Y++;
                Y &= 0xff;
                setStatusFlags(Y);
                break;
            case 0xC6:
                address = byZeroPage();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = decrement(read(address));
                write(address, writeVal);
                break;
            case 0xD6:
                address = byZeroPageX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = decrement(read(address));
                write(address, writeVal);
                break;
            case 0xCE:
                address = byAbsolute();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = decrement(read(address));
                write(address, writeVal);
                break;
            case 0xDE:
                address = byAbsoluteX();
                writeVal = memory.read(address);
                write(address, writeVal);
                writeVal = decrement(read(address));
                write(address, writeVal);
                break;
            case 0xCA:
                X--;
                X &= 0xff;
                setStatusFlags(X);
                break;
            case 0x88:
                Y--;
                Y &= 0xff;
                setStatusFlags(Y);
                break;
            case 0x69:
                operateAdd(byImmediate());
                break;
            case 0x65:
                operateAdd(read(byZeroPage()));
                break;
            case 0x75:
                operateAdd(read(byZeroPageX()));
                break;
            case 0x6D:
                operateAdd(read(byAbsolute()));
                break;
            case 0x7D:
                operateAdd(read(byAbsoluteX()));
                break;
            case 0x79:
                operateAdd(read(byAbsoluteY()));
                break;
            case 0x61:
                operateAdd(read(byIndirectX()));
                break;
            case 0x71:
                operateAdd(read(byIndirectY()));
                break;
            case 0xEB:
            case 0xE9:
                operateSub(byImmediate());
                break;
            case 0xE5:
                operateSub(read(byZeroPage()));
                break;
            case 0xF5:
                operateSub(read(byZeroPageX()));
                break;
            case 0xED:
                operateSub(read(byAbsolute()));
                break;
            case 0xFD:
                operateSub(read(byAbsoluteX()));
                break;
            case 0xF9:
                operateSub(read(byAbsoluteY()));
                break;
            case 0xE1:
                operateSub(read(byIndirectX()));
                break;
            case 0xF1:
                operateSub(read(byIndirectY()));
                break;
            case 0xC9:
                operateCmp(A, byImmediate());
                break;
            case 0xC5:
                operateCmp(A, read(byZeroPage()));
                break;
            case 0xD5:
                operateCmp(A, read(byZeroPageX()));
                break;
            case 0xCD:
                operateCmp(A, read(byAbsolute()));
                break;
            case 0xDD:
                operateCmp(A, read(byAbsoluteX()));
                break;
            case 0xD9:
                operateCmp(A, read(byAbsoluteY()));
                break;
            case 0xC1:
                operateCmp(A, read(byIndirectX()));
                break;
            case 0xD1:
                operateCmp(A, read(byIndirectY()));
                break;
            case 0xE0:
                operateCmp(X, byImmediate());
                break;
            case 0xE4:
                operateCmp(X, read(byZeroPage()));
                break;
            case 0xEC:
                operateCmp(X, read(byAbsolute()));
                break;
            case 0xC0:
                operateCmp(Y, byImmediate());
                break;
            case 0xC4:
                operateCmp(Y, read(byZeroPage()));
                break;
            case 0xCC:
                operateCmp(Y, read(byAbsolute()));
                break;
            case 0x1A:
            case 0x3A:
            case 0x5A:
            case 0x7A:
            case 0xDA:
            case 0xEA:
            case 0xFA:
                break;
            default:
                halted = true;
                PC--;
                break;
        }
        cyclesPending -= cycles[instCode];
    }

    /**
     *
     * <P>Reset the Processor</P>
     *
     */
    private final void reset() {
        nes.memory.init(currentCart);
        nes.mapper.init(nes.memory);
        intReset();
    }

    /**
     *
     * <P>Correct the CPU Cycles for a Couple of Odd Games.</P>
     *
     */
    public void correctCPUCycles() {
        if (nes.mapper == null) {
            CYCLES_PER_LINE = 116.0f;
            return;
        }
        long crc = currentCart.crc32;
        System.out.println(crc);
        switch(nes.mapper.getMapperNumber()) {
            case 0x04:
                {
                    if (crc == 0xA0B0B742l) {
                        CYCLES_PER_LINE = 144.0f;
                        return;
                    }
                }
            case 0x07:
                {
                    if (crc == 0x279710DCl) {
                        CYCLES_PER_LINE = 112.0f;
                        return;
                    }
                }
            default:
                {
                    CYCLES_PER_LINE = 116.0f;
                    return;
                }
        }
    }

    /**
     *
     * <P>Reset the internal CPU registers.</P>
     *
     */
    private final void intReset() {
        correctCPUCycles();
        A = 0x00;
        X = 0x00;
        Y = 0x00;
        P = 0x04;
        S = 0xFF;
        halted = false;
        PC = readWord(0xFFFC);
    }

    /**
     *
     * <P>Request that the current CPU stops Processing.</P>
     *
     */
    public final void stopProcessing() {
        stopRequest = true;
        try {
        } catch (Exception e) {
        }
    }

    /**
     *
     * <P>Wait while CPU is not Active.</P>
     *
     */
    private final void waitWhileNotActive() {
        while (!cpuActive) {
            deleteScreen();
            if (stopRequest) {
                return;
            }
            try {
            } catch (Exception controlPausedException) {
            }
        }
    }

    /**
     *
     * <P>Wait while CPU is paused.</P>
     *
     */
    private final void waitWhilePaused() {
        if (true) return;
        while (cpuPaused) {
            drawScreen(true);
            if (stopRequest) {
                return;
            }
            try {
            } catch (Exception controlPausedException) {
            }
        }
    }

    /**
     *
     * <P>Read Byte from memory.</P>
     *
     * @param  address Address in memory to read from.
     * @return value at the specified address.
     *
     */
    private final int read(int addr) {
        return nes.memory.read(addr);
    }

    /**
     *
     * <P>Read Word from memory.</P>
     *
     * @param  address Address in memory to read from.
     * @return value at the specified address.
     *
     */
    private final int readWord(int address) {
        return nes.memory.readWord(address);
    }

    /**
     *
     * <P>Write Byte to memory.</P>
     *
     * @param  address Address in memory to write to.
     * @param  value   value to write.
     *
     */
    private final void write(int address, int value) {
        nes.memory.write(address, value);
    }

    /**
     *
     * <P>Write Word to memory.</P>
     *
     * @param  address Address in memory to write to.
     * @param  value   Value to write.
     *
     */
    private final void writeWord(int address, int value) {
        nes.memory.writeWord(address, value);
    }

    /**
     *
     * <P>Get value by Immediate Mode Addressing - #$00</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byImmediate() {
        int i = read(PC++);
        return i;
    }

    /**
     *
     * <P>Get value by Absolute Mode Addressing - $aaaa</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byAbsolute() {
        int address = readWord(PC);
        PC += 2;
        return address;
    }

    /**
     *
     * <P>Get value by Absolute Y Mode Addressing - $aaaa,Y</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byAbsoluteY() {
        int i = byAbsolute();
        int j = i + Y;
        checkPageBoundaryCrossing(i, j);
        return j;
    }

    /**
     *
     * <P>Get value by Absolute X Mode Addressing - $aaaa,X</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byAbsoluteX() {
        int i = byAbsolute();
        int j = i + X;
        checkPageBoundaryCrossing(i, j);
        return j;
    }

    /**
     *
     * <P>Get value by Zero Page Mode Addressing - $aa</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byZeroPage() {
        int address = read(PC++);
        return address;
    }

    /**
     *
     * <P>Get value by Zero Page X Mode Addressing - $aa,X</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byZeroPageX() {
        int address = read(PC++);
        return (address + X) & 0xff;
    }

    /**
     *
     * <P>Get value by Zero Page Y Mode Addressing - $aa,Y</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byZeroPageY() {
        int address = read(PC++);
        return address + Y & 0xff;
    }

    /**
     *
     * <P>Get value by Indirect X Mode Addressing - ($aa,X)</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byIndirectX() {
        int address = read(PC++);
        address += X;
        address &= 0xFF;
        return readWord(address);
    }

    /**
     *
     * <P>Get value by Indirect Y Mode Addressing - ($aa),Y</P>
     *
     * @return The value by the specified addressing mode in relation to the current PC.
     *
     */
    private final int byIndirectY() {
        int address = read(PC++);
        address = readWord(address);
        checkPageBoundaryCrossing(address, address + Y);
        return address + Y;
    }

    /**
     *
     * <P>Decrement the number of cycles pending if over a page boundary.</P>
     *
     * @param address1 The first address.
     * @param address2 The second address.
     *
     */
    private final void checkPageBoundaryCrossing(int address1, int address2) {
        if (((address2 ^ address1) & 0x100) != 0) {
            cyclesPending--;
        }
    }

    /**
     *
     * <P>Set the Zero and Negative Status Flags.</P>
     *
     * @param value The value used to determine the Status Flags.
     *
     */
    private final void setStatusFlags(int value) {
        P &= 0x7D;
        P |= znTable[value];
    }

    /**
     *
     * <P>Perform Arithmetic Shift Left.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int ASL(int i) {
        P &= 0x7C;
        P |= i >> 7;
        i <<= 1;
        i &= 0xFF;
        P |= znTable[i];
        return i;
    }

    /**
     *
     * <P>Perform Logical Shift Right.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int LSR(int i) {
        P &= 0x7C;
        P |= i & 0x1;
        i >>= 1;
        P |= znTable[i];
        return i;
    }

    /**
     *
     * <P>Perform Rotate Left.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int ROL(int i) {
        i <<= 1;
        i |= P & 0x1;
        P &= 0x7C;
        P |= i >> 8;
        i &= 0xFF;
        P |= znTable[i];
        return i;
    }

    /**
     *
     * <P>Perform Rotate Right.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int ROR(int i) {
        int j = P & 0x1;
        P &= 0x7C;
        P |= i & 0x1;
        i >>= 1;
        i |= j << 7;
        P |= znTable[i];
        return i;
    }

    /**
     *
     * <P>Perform Incrementation.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int increment(int i) {
        i = ++i & 0xff;
        setStatusFlags(i);
        return i;
    }

    /**
     *
     * <P>Perform Decrementation.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final int decrement(int i) {
        i = --i & 0xff;
        setStatusFlags(i);
        return i;
    }

    /**
     *
     * <P>Perform Add with Carry (no decimal mode on NES).</P>
     *
     * @param i The value used by the function.
     *
     */
    private final void operateAdd(int i) {
        int k = P & 0x1;
        int j = A + i + k;
        P &= 0x3C;
        P |= (~(A ^ i) & (A ^ i) & 0x80) == 0 ? 0 : 0x40;
        P |= j <= 255 ? 0 : 0x1;
        A = j & 0xFF;
        P |= znTable[A];
    }

    /**
     *
     * <P>Perform Subtract with Carry (no decimal mode on NES).</P>
     *
     * @param i The value used by the function.
     *
     */
    private final void operateSub(int i) {
        int k = ~P & 0x1;
        int j = A - i - k;
        P &= 0x3C;
        P |= (~(A ^ i) & (A ^ i) & 0x80) == 0 ? 0 : 0x40;
        P |= j < 0 ? 0 : 0x1;
        A = j & 0xFF;
        P |= znTable[A];
    }

    /**
     *
     * <P>Perform Compare Function.</P>
     *
     * @param i The first value.
     * @param j The second value.
     *
     */
    private final void operateCmp(int i, int j) {
        int k = i - j;
        P &= 0x7C;
        P |= k < 0 ? 0 : 0x1;
        P |= znTable[k & 0xff];
    }

    /**
     *
     * <P>Perform Bit Function.</P>
     *
     * @param i The value used by the function.
     *
     */
    private final void operateBit(int i) {
        P &= 0x3D;
        P |= i & 0xc0;
        P |= (A & i) != 0 ? 0 : 0x2;
    }

    /**
     *
     * <P>Function for Handling Branches</P>
     *
     * @param flagNum The byte value to compare.
     * @param flagVal The expected truth value for a branch.
     *
     */
    private final void branch(int flagNum, boolean flagVal) {
        int offset = (byte) read(PC++);
        if (((P & flagNum) != 0) == flagVal) {
            checkPageBoundaryCrossing(PC + offset, PC);
            PC = PC + offset;
            cyclesPending--;
        }
    }

    /**
     *
     * <P>Push a value onto the Stack.</P>
     *
     * @param stackVal The value to push.
     *
     */
    private final void push(int stackVal) {
        write(S + 256, stackVal);
        S--;
        S &= 0xff;
    }

    /**
     *
     * <P>Pop a value from the Stack.</P>
     *
     * @return The value on top of the Stack.
     *
     */
    private final int pop() {
        S++;
        S &= 0xff;
        return read(S + 256);
    }

    /**
     *
     * <P>Push a Word onto the Stack.</P>
     *
     * @param stackVal The 16 bit word to push.
     *
     */
    private final void pushWord(int stackVal) {
        push((stackVal >> 8) & 0xFF);
        push(stackVal & 0xFF);
    }

    /**
     *
     * <P>Pop a Word from the Stack.</P>
     *
     * @return The 16 bit word on top of the Stack.
     *
     */
    private final int popWord() {
        return pop() + pop() * 256;
    }

    public int getPC() {
        return PC;
    }

    /**
     *
     * Array of CPU Cycles for each Machine Code Instruction
     *
     */
    private final int cycles[] = { 7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7, 6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7, 6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7, 4, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6, 4, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7, 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4, 2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5, 2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4, 2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7, 2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6, 2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 5, 5, 7, 7 };

    /**
     *
     * Array of Zero and Negative Flags for Speedy Lookup
     *
     */
    private final int znTable[] = { 002, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 000, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128 };
}
