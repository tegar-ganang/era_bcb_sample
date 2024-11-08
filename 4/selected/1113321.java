package se.sics.mspsim.chip;

import java.io.IOException;
import se.sics.mspsim.core.*;
import se.sics.mspsim.util.Utils;

public abstract class M25P80 extends Chip implements USARTListener, PortListener, Memory {

    public static final int WRITE_STATUS = 0x01;

    public static final int PAGE_PROGRAM = 0x02;

    public static final int READ_DATA = 0x03;

    public static final int WRITE_DISABLE = 0x04;

    public static final int READ_STATUS = 0x05;

    public static final int WRITE_ENABLE = 0x06;

    public static final int READ_DATA_FAST = 0x0b;

    public static final int READ_IDENT = 0x9f;

    public static final int SECTOR_ERASE = 0xd8;

    public static final int BULK_ERASE = 0xc7;

    public static final int DEEP_POWER_DOWN = 0xb9;

    public static final int WAKE_UP = 0xab;

    public static final int STATUS_MASK = 0x9C;

    public static final int MEMORY_SIZE = 1024 * 1024;

    public static final int CHIP_SELECT = 0x10;

    private static final double PROGRAM_PAGE_MILLIS = 1.0;

    private static final double SECTOR_ERASE_MILLIS = 800;

    private int state = 0;

    private boolean chipSelect;

    private int pos;

    private int status = 0;

    private boolean writeEnable = false;

    private boolean writing = false;

    private int[] identity = new int[] { 0x20, 0x20, 0x14, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    private int readAddress;

    private int loadedAddress = -1;

    private int blockWriteAddress;

    private byte[] readMemory = new byte[256];

    private byte[] buffer = new byte[256];

    private TimeEvent writeEvent = new TimeEvent(0, "M25P80 Writing") {

        public void execute(long t) {
            writing = false;
        }
    };

    public M25P80(MSP430Core cpu) {
        super("M25P80", "External Flash", cpu);
    }

    @Override
    public void notifyReset() {
        writing = false;
    }

    public int getStatus() {
        return status | (writeEnable ? 0x02 : 0x00) | (writing ? 0x01 : 0x00);
    }

    public void stateChanged(int state) {
    }

    public void dataReceived(USART source, int data) {
        if (chipSelect) {
            if (DEBUG) {
                log("byte received: " + data);
            }
            switch(state) {
                case READ_STATUS:
                    if (DEBUG) {
                        log("Read status => " + getStatus() + " from $" + Utils.hex16(cpu.getPC()));
                    }
                    source.byteReceived(getStatus());
                    return;
                case READ_IDENT:
                    source.byteReceived(identity[pos]);
                    pos++;
                    if (pos >= identity.length) {
                        pos = 0;
                    }
                    return;
                case WRITE_STATUS:
                    status = data & STATUS_MASK;
                    source.byteReceived(0);
                    return;
                case READ_DATA:
                    if (pos < 3) {
                        readAddress = (readAddress << 8) + data;
                        source.byteReceived(0);
                        pos++;
                        if (DEBUG && pos == 3) {
                            log("reading from $" + Integer.toHexString(readAddress));
                        }
                    } else {
                        source.byteReceived(readMemory(readAddress++));
                        if (readAddress > 0xfffff) {
                            readAddress = 0;
                        }
                    }
                    return;
                case SECTOR_ERASE:
                    if (pos < 3) {
                        readAddress = (readAddress << 8) + data;
                        source.byteReceived(0);
                        pos++;
                        if (pos == 3) {
                            sectorErase(readAddress);
                        }
                    }
                    return;
                case PAGE_PROGRAM:
                    if (pos < 3) {
                        readAddress = (readAddress << 8) + data;
                        source.byteReceived(0);
                        pos++;
                        if (pos == 3) {
                            for (int i = 0; i < buffer.length; i++) {
                                buffer[i] = (byte) 0xff;
                            }
                            blockWriteAddress = readAddress & 0xfff00;
                            if (DEBUG) {
                                log("programming at $" + Integer.toHexString(readAddress));
                            }
                        }
                    } else {
                        source.byteReceived(0);
                        writeBuffer((readAddress++) & 0xff, data);
                    }
                    return;
            }
            if (DEBUG) {
                log("new command: " + data);
            }
            switch(data) {
                case WRITE_ENABLE:
                    if (DEBUG) {
                        log("Write Enable");
                    }
                    writeEnable = true;
                    break;
                case WRITE_DISABLE:
                    if (DEBUG) {
                        log("Write Disable");
                    }
                    writeEnable = false;
                    break;
                case READ_IDENT:
                    if (DEBUG) {
                        log("Read ident.");
                    }
                    state = READ_IDENT;
                    pos = 0;
                    source.byteReceived(0);
                    return;
                case READ_STATUS:
                    state = READ_STATUS;
                    source.byteReceived(0);
                    return;
                case WRITE_STATUS:
                    if (DEBUG) {
                        log("Write status");
                    }
                    state = WRITE_STATUS;
                    break;
                case READ_DATA:
                    if (DEBUG) {
                        log("Read Data");
                    }
                    state = READ_DATA;
                    pos = readAddress = 0;
                    break;
                case PAGE_PROGRAM:
                    if (DEBUG) {
                        log("Page Program");
                    }
                    state = PAGE_PROGRAM;
                    pos = readAddress = 0;
                    break;
                case SECTOR_ERASE:
                    if (DEBUG) {
                        log("Sector Erase");
                    }
                    state = SECTOR_ERASE;
                    pos = 0;
                    break;
                case BULK_ERASE:
                    log("Bulk Erase");
                    break;
            }
            source.byteReceived(0);
        }
    }

    /***********************************************
   * Memory interface methods
   ***********************************************/
    public int readByte(int address) {
        byte[] data = new byte[256];
        try {
            loadMemory(address, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ~data[address & 0xff];
    }

    public void writeByte(int address, int data) {
        byte[] mem = new byte[256];
        try {
            loadMemory(address, mem);
            mem[address & 0xff] = (byte) ~data;
            writeBack(address, mem);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (loadedAddress >= 0 && ((loadedAddress & 0xfff00) == (address & 0xfff00))) {
            loadedAddress = -1;
        }
    }

    public int getSize() {
        return MEMORY_SIZE;
    }

    private int readMemory(int address) {
        if (DEBUG) {
            log("Reading memory address: " + Integer.toHexString(address));
        }
        ensureLoaded(address);
        return readMemory[address & 0xff];
    }

    private void writeBuffer(int address, int data) {
        buffer[address] = (byte) data;
    }

    private void ensureLoaded(int address) {
        if (loadedAddress < 0 || ((loadedAddress & 0xfff00) != (address & 0xfff00))) {
            try {
                if (DEBUG) {
                    log("Loading memory: " + (address & 0xfff00));
                }
                loadMemory(address, readMemory);
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadedAddress = address & 0xfff00;
        }
    }

    private void loadMemory(int address, byte[] readMemory) throws IOException {
        seek(address & 0xfff00);
        readFully(readMemory);
        for (int i = 0; i < readMemory.length; i++) {
            readMemory[i] = (byte) (~readMemory[i] & 0xff);
        }
    }

    public boolean getChipSelect() {
        return chipSelect;
    }

    public void portWrite(IOPort source, int data) {
        if (chipSelect && (data & CHIP_SELECT) != 0) {
            switch(state) {
                case PAGE_PROGRAM:
                    programPage();
                    break;
            }
        }
        chipSelect = (data & CHIP_SELECT) == 0;
        state = 0;
    }

    private void writeStatus(double time) {
        writing = true;
        cpu.scheduleTimeEventMillis(writeEvent, time);
    }

    private void programPage() {
        if (writing) logw("Can not set program page while already writing... from $" + Utils.hex16(cpu.getPC()));
        writeStatus(PROGRAM_PAGE_MILLIS);
        ensureLoaded(blockWriteAddress);
        for (int i = 0; i < readMemory.length; i++) {
            readMemory[i] &= buffer[i];
        }
        writeBack(blockWriteAddress, readMemory);
    }

    private void sectorErase(int address) {
        writeStatus(SECTOR_ERASE_MILLIS);
        int sectorAddress = address & 0xf0000;
        loadedAddress = -1;
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0xff;
        }
        blockWriteAddress = sectorAddress;
        for (int i = 0; i < 0x100; i++) {
            if (DEBUG) {
                log("erasing at $" + Integer.toHexString(blockWriteAddress));
            }
            writeBack(blockWriteAddress, buffer);
            blockWriteAddress += 0x100;
        }
    }

    private void writeBack(int address, byte[] data) {
        try {
            byte[] tmp = new byte[data.length];
            if (DEBUG) {
                log("Writing data to disk at $" + Integer.toHexString(address));
            }
            seek(address & 0xfff00);
            for (int i = 0; i < data.length; i++) {
                tmp[i] = (byte) (~data[i] & 0xff);
            }
            write(tmp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getModeMax() {
        return 0;
    }

    public abstract void seek(long pos) throws IOException;

    public abstract int readFully(byte[] b) throws IOException;

    public abstract void write(byte[] b) throws IOException;

    public int getConfiguration(int param) {
        return 0;
    }

    public String info() {
        return "  Status: " + getStatus() + "  Write Enabled: " + writeEnable + "  Write in Progress: " + writing + '\n' + "  Chip Select: " + chipSelect;
    }
}
