package jpcsp.HLE.kernel.types;

import jpcsp.Memory;

public class pspUtilityDialogCommon extends pspAbstractMemoryMappedStructureVariableLength {

    public int language;

    public int buttonSwap;

    public static final int BUTTON_ACCEPT_CIRCLE = 0;

    public static final int BUTTON_ACCEPT_CROSS = 1;

    public int graphicsThread;

    public int accessThread;

    public int fontThread;

    public int soundThread;

    public int result;

    @Override
    protected void read() {
        super.read();
        language = read32();
        buttonSwap = read32();
        graphicsThread = read32();
        accessThread = read32();
        fontThread = read32();
        soundThread = read32();
        result = read32();
        readUnknown(16);
    }

    @Override
    protected void write() {
        super.write();
        write32(language);
        write32(buttonSwap);
        write32(graphicsThread);
        write32(accessThread);
        write32(fontThread);
        write32(soundThread);
        write32(result);
        writeUnknown(16);
    }

    public void writeResult(Memory mem) {
        writeResult(mem, getBaseAddress());
    }

    public void writeResult(Memory mem, int address) {
        mem.write32(address + 28, result);
    }

    @Override
    public int sizeof() {
        return Math.min(12 * 4, super.sizeof());
    }

    public int totalSizeof() {
        return super.sizeof();
    }
}
