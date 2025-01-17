package jpcsp.memory;

import static jpcsp.MemoryMap.SIZE_RAM;
import static jpcsp.MemoryMap.SIZE_SCRATCHPAD;
import static jpcsp.MemoryMap.SIZE_VRAM;
import static jpcsp.MemoryMap.START_RAM;
import static jpcsp.MemoryMap.START_SCRATCHPAD;
import static jpcsp.MemoryMap.START_VRAM;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;

public class StandardMemory extends Memory {

    private static final int PAGE_COUNT = 0x00100000;

    private static final int PAGE_MASK = 0x00000FFF;

    private static final int PAGE_SHIFT = 12;

    private static final int INDEX_SCRATCHPAD = 0;

    private static final int INDEX_VRAM = SIZE_SCRATCHPAD >>> PAGE_SHIFT;

    private static final int INDEX_RAM = INDEX_VRAM + (SIZE_VRAM >>> PAGE_SHIFT);

    private static int SIZE_ALLMEM;

    private byte[] all;

    private static int[] map;

    private ByteBuffer buf;

    private ByteBuffer scratchpad;

    private ByteBuffer videoram;

    private ByteBuffer mainmemory;

    @Override
    public boolean allocate() {
        SIZE_ALLMEM = SIZE_SCRATCHPAD + SIZE_VRAM + SIZE_RAM;
        all = null;
        map = null;
        try {
            all = new byte[SIZE_ALLMEM];
            map = new int[PAGE_COUNT];
            buf = ByteBuffer.wrap(all);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            scratchpad = ByteBuffer.wrap(all, 0, SIZE_SCRATCHPAD).slice();
            scratchpad.order(ByteOrder.LITTLE_ENDIAN);
            videoram = ByteBuffer.wrap(all, SIZE_SCRATCHPAD, SIZE_VRAM).slice();
            videoram.order(ByteOrder.LITTLE_ENDIAN);
            mainmemory = ByteBuffer.wrap(all, SIZE_SCRATCHPAD + SIZE_VRAM, SIZE_RAM).slice();
            mainmemory.order(ByteOrder.LITTLE_ENDIAN);
            buildMap();
        } catch (OutOfMemoryError e) {
            Memory.log.error("Cannot allocate StandardMemory: add the option '-Xmx64m' to the Java Virtual Machine startup command to improve Performance");
            return false;
        }
        return super.allocate();
    }

    @Override
    public void Initialise() {
        Arrays.fill(all, (byte) 0);
    }

    public StandardMemory() {
    }

    private void buildMap() {
        int i;
        int page;
        Arrays.fill(map, -1);
        page = START_SCRATCHPAD >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_SCRATCHPAD >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
        }
        page = START_VRAM >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_VRAM >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x00000 + 0x200 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x40000 + 0x200 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x80000 + 0x200 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
        }
        page = START_RAM >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_RAM >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
        }
    }

    private static int indexFromAddr(int address) throws Exception {
        int index = map[address >>> PAGE_SHIFT];
        if (index == -1) {
            throw new Exception("Invalid memory address : " + Integer.toHexString(address) + " PC=" + Integer.toHexString(Emulator.getProcessor().cpu.pc));
        }
        return index;
    }

    @Override
    public int read8(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.get(page + (address & PAGE_MASK)) & 0xFF;
        } catch (Exception e) {
            Memory.log.error("read8 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public int read16(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getShort(page + (address & PAGE_MASK)) & 0xFFFF;
        } catch (Exception e) {
            Memory.log.error("read16 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public int read32(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getInt(page + (address & PAGE_MASK));
        } catch (Exception e) {
            if (read32AllowedInvalidAddress(address)) {
                return 0;
            }
            Memory.log.error("read32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public long read64(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getLong(page + (address & PAGE_MASK));
        } catch (Exception e) {
            Memory.log.error("read64 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public void write8(int address, byte data) {
        try {
            int page = indexFromAddr(address);
            buf.put(page + (address & PAGE_MASK), data);
            Modules.sceDisplayModule.write8(address & addressMask);
        } catch (Exception e) {
            Memory.log.error("write8 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write16(int address, short data) {
        try {
            int page = indexFromAddr(address);
            buf.putShort(page + (address & PAGE_MASK), data);
            Modules.sceDisplayModule.write16(address & addressMask);
        } catch (Exception e) {
            Memory.log.error("write16 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write32(int address, int data) {
        try {
            int page = indexFromAddr(address);
            buf.putInt(page + (address & PAGE_MASK), data);
            Modules.sceDisplayModule.write32(address & addressMask);
        } catch (Exception e) {
            Memory.log.error("write32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write64(int address, long data) {
        try {
            int page = indexFromAddr(address);
            buf.putLong(page + (address & PAGE_MASK), data);
        } catch (Exception e) {
            Memory.log.error("write64 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public ByteBuffer getMainMemoryByteBuffer() {
        return mainmemory;
    }

    @Override
    public ByteBuffer getBuffer(int address, int length) {
        address = normalizeAddress(address);
        int endAddress = address + length - 1;
        if (address >= MemoryMap.START_RAM && endAddress <= MemoryMap.END_RAM) {
            return ByteBuffer.wrap(mainmemory.array(), mainmemory.arrayOffset() + address - MemoryMap.START_RAM, length).slice().order(ByteOrder.LITTLE_ENDIAN);
        } else if (address >= MemoryMap.START_VRAM && endAddress <= MemoryMap.END_VRAM) {
            return ByteBuffer.wrap(videoram.array(), videoram.arrayOffset() + address - MemoryMap.START_VRAM, length).slice().order(ByteOrder.LITTLE_ENDIAN);
        } else if (address >= MemoryMap.START_SCRATCHPAD && endAddress <= MemoryMap.END_SCRATCHPAD) {
            return ByteBuffer.wrap(scratchpad.array(), scratchpad.arrayOffset() + address - MemoryMap.START_SCRATCHPAD, length).slice().order(ByteOrder.LITTLE_ENDIAN);
        }
        return null;
    }

    @Override
    public void memset(int address, byte data, int length) {
        ByteBuffer buffer = getBuffer(address, length);
        Arrays.fill(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + length, data);
    }

    @Override
    public void copyToMemory(int address, ByteBuffer source, int length) {
        byte[] data = new byte[length];
        source.get(data);
        ByteBuffer destination = getBuffer(address, length);
        destination.put(data);
    }

    @Override
    protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
        destination = normalizeAddress(destination);
        source = normalizeAddress(source);
        if (checkOverlap || !areOverlapping(destination, source, length)) {
            ByteBuffer destinationBuffer = getBuffer(destination, length);
            ByteBuffer sourceBuffer = getBuffer(source, length);
            destinationBuffer.put(sourceBuffer);
        } else {
            IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 1);
            for (int i = 0; i < length; i++) {
                write8(destination + i, (byte) sourceReader.readNext());
            }
        }
    }
}
