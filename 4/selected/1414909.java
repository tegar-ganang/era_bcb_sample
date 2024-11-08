package jpcsp.memory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;

public class FastMemory extends Memory {

    private int[] all;

    private static final boolean traceRead = false;

    private static final boolean traceWrite = false;

    @Override
    public boolean allocate() {
        all = null;
        int allSize = (MemoryMap.END_RAM + 1) / 4;
        try {
            all = new int[allSize];
        } catch (OutOfMemoryError e) {
            Memory.log.warn("Cannot allocate FastMemory: add the option '-Xmx256m' to the Java Virtual Machine startup command to improve Performance");
            Memory.log.info("The current Java Virtual Machine has been started using '-Xmx" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "m'");
            return false;
        }
        return super.allocate();
    }

    @Override
    public void Initialise() {
        Arrays.fill(all, 0);
    }

    @Override
    public int read8(int address) {
        try {
            address &= addressMask;
            int data = all[address >> 2];
            switch(address & 0x03) {
                case 1:
                    data >>= 8;
                    break;
                case 2:
                    data >>= 16;
                    break;
                case 3:
                    data >>= 24;
                    break;
            }
            if (traceRead) {
                if (log.isTraceEnabled()) {
                    log.trace("read8(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(data & 0xFF).toUpperCase());
                }
            }
            return data & 0xFF;
        } catch (Exception e) {
            invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public int read16(int address) {
        try {
            address &= addressMask;
            int data = all[address >> 2];
            if ((address & 0x02) != 0) {
                data >>= 16;
            }
            if (traceRead) {
                if (log.isTraceEnabled()) {
                    log.trace("read16(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(data & 0xFFFF).toUpperCase());
                }
            }
            return data & 0xFFFF;
        } catch (Exception e) {
            invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public int read32(int address) {
        try {
            address &= addressMask;
            if (traceRead) {
                if (log.isTraceEnabled()) {
                    log.trace("read32(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(all[address / 4]).toUpperCase() + " (" + Float.intBitsToFloat(all[address / 4]) + ")");
                }
            }
            return all[address >> 2];
        } catch (Exception e) {
            if (read32AllowedInvalidAddress(address)) {
                return 0;
            }
            invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public long read64(int address) {
        try {
            address &= addressMask;
            long data = (((long) all[address / 4 + 1]) << 32) | (((long) all[address / 4]) & 0xFFFFFFFFL);
            if (traceRead) {
                if (log.isTraceEnabled()) {
                    log.trace("read64(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Long.toHexString(data).toUpperCase());
                }
            }
            return data;
        } catch (Exception e) {
            invalidMemoryAddress(address, "read64", Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    @Override
    public void write8(int address, byte data) {
        try {
            address &= addressMask;
            int memData = all[address >> 2];
            switch(address & 0x03) {
                case 0:
                    memData = (memData & 0xFFFFFF00) | ((data & 0xFF));
                    break;
                case 1:
                    memData = (memData & 0xFFFF00FF) | ((data & 0xFF) << 8);
                    break;
                case 2:
                    memData = (memData & 0xFF00FFFF) | ((data & 0xFF) << 16);
                    break;
                case 3:
                    memData = (memData & 0x00FFFFFF) | ((data & 0xFF) << 24);
                    break;
            }
            if (traceWrite) {
                if (log.isTraceEnabled()) {
                    log.trace("write8(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data & 0xFF).toUpperCase() + ")");
                }
            }
            all[address >> 2] = memData;
            Modules.sceDisplayModule.write8(address);
        } catch (Exception e) {
            invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write16(int address, short data) {
        try {
            address &= addressMask;
            int memData = all[address >> 2];
            if ((address & 0x02) == 0) {
                memData = (memData & 0xFFFF0000) | (data & 0xFFFF);
            } else {
                memData = (memData & 0x0000FFFF) | ((data & 0xFFFF) << 16);
            }
            if (traceWrite) {
                if (log.isTraceEnabled()) {
                    log.trace("write16(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data & 0xFFFF).toUpperCase() + ")");
                }
            }
            all[address >> 2] = memData;
            Modules.sceDisplayModule.write16(address);
        } catch (Exception e) {
            invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write32(int address, int data) {
        try {
            address &= addressMask;
            all[address >> 2] = data;
            if (traceWrite) {
                if (log.isTraceEnabled()) {
                    log.trace("write32(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data).toUpperCase() + " (" + Float.intBitsToFloat(data) + "))");
                }
            }
            Modules.sceDisplayModule.write32(address);
        } catch (Exception e) {
            invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public void write64(int address, long data) {
        try {
            address &= addressMask;
            if (traceWrite) {
                if (log.isTraceEnabled()) {
                    log.trace("write64(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Long.toHexString(data).toUpperCase() + ")");
                }
            }
            all[address / 4] = (int) data;
            all[address / 4 + 1] = (int) (data >> 32);
        } catch (Exception e) {
            invalidMemoryAddress(address, "write64", Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    @Override
    public IntBuffer getMainMemoryByteBuffer() {
        return IntBuffer.wrap(all, MemoryMap.START_RAM / 4, MemoryMap.SIZE_RAM / 4);
    }

    @Override
    public IntBuffer getBuffer(int address, int length) {
        address = normalizeAddress(address);
        IntBuffer buffer = getMainMemoryByteBuffer();
        buffer.position(address / 4);
        buffer.limit((address + length + 3) / 4);
        return buffer.slice();
    }

    private boolean isIntAligned(int n) {
        return (n & 0x03) == 0;
    }

    @Override
    public void memset(int address, byte data, int length) {
        address = normalizeAddress(address);
        for (; !isIntAligned(address) && length > 0; address++, length--) {
            write8(address, data);
        }
        int count4 = length / 4;
        if (count4 > 0) {
            int data1 = data & 0xFF;
            int data4 = (data1 << 24) | (data1 << 16) | (data1 << 8) | data1;
            Arrays.fill(all, address / 4, address / 4 + count4, data4);
            address += count4 * 4;
            length -= count4 * 4;
        }
        for (; length > 0; address++, length--) {
            write8(address, data);
        }
    }

    @Override
    public void copyToMemory(int address, ByteBuffer source, int length) {
        while (!isIntAligned(address) && length > 0 && source.hasRemaining()) {
            byte b = source.get();
            write8(address, b);
            address++;
            length--;
        }
        int countInt = Math.min(length, source.remaining()) >> 2;
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, countInt << 2, 4);
        for (int i = 0; i < countInt; i++) {
            int data1 = source.get() & 0xFF;
            int data2 = source.get() & 0xFF;
            int data3 = source.get() & 0xFF;
            int data4 = source.get() & 0xFF;
            int data = (data4 << 24) | (data3 << 16) | (data2 << 8) | data1;
            memoryWriter.writeNext(data);
        }
        memoryWriter.flush();
        int copyLength = countInt << 2;
        length -= copyLength;
        address += copyLength;
        while (length > 0 && source.hasRemaining()) {
            byte b = source.get();
            write8(address, b);
            address++;
            length--;
        }
    }

    public int[] getAll() {
        return all;
    }

    private void memcpyAligned4(int destination, int source, int length, boolean checkOverlap) {
        if (checkOverlap || !areOverlapping(destination, source, length)) {
            System.arraycopy(all, source >> 2, all, destination >> 2, length >> 2);
        } else {
            IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 4);
            IMemoryWriter destinationWriter = MemoryWriter.getMemoryWriter(destination, length, 4);
            for (int i = 0; i < length; i += 4) {
                destinationWriter.writeNext(sourceReader.readNext());
            }
            destinationWriter.flush();
        }
    }

    @Override
    protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
        if (length <= 0) {
            return;
        }
        destination = normalizeAddress(destination);
        source = normalizeAddress(source);
        if (isIntAligned(source) && isIntAligned(destination) && isIntAligned(length)) {
            memcpyAligned4(destination, source, length, checkOverlap);
        } else if ((source & 0x03) == (destination & 0x03) && (!checkOverlap || !areOverlapping(destination, source, length))) {
            while (!isIntAligned(source) && length > 0) {
                write8(destination, (byte) read8(source));
                source++;
                destination++;
                length--;
            }
            int length4 = length & ~0x03;
            if (length4 > 0) {
                memcpyAligned4(destination, source, length4, checkOverlap);
                source += length4;
                destination += length4;
                length -= length4;
            }
            while (length > 0) {
                write8(destination, (byte) read8(source));
                destination++;
                source++;
                length--;
            }
        } else {
            if (!checkOverlap || source >= destination || !areOverlapping(destination, source, length)) {
                if (areOverlapping(destination, source, 4)) {
                    for (int i = 0; i < length; i++) {
                        write8(destination + i, (byte) read8(source + i));
                    }
                } else {
                    IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 1);
                    for (int i = 0; i < length; i++) {
                        write8(destination + i, (byte) sourceReader.readNext());
                    }
                }
            } else {
                for (int i = length - 1; i >= 0; i--) {
                    write8(destination + i, (byte) read8(source + i));
                }
            }
        }
    }
}
