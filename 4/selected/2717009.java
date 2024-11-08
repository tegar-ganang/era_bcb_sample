package nl.kbna.dioscuri.module.cpu32;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import nl.kbna.dioscuri.exception.ModuleException;
import nl.kbna.dioscuri.exception.ModuleUnknownPort;
import nl.kbna.dioscuri.exception.ModuleWriteOnlyPortException;
import nl.kbna.dioscuri.module.Module;
import nl.kbna.dioscuri.module.ModuleDevice;

public class DMAController extends ModuleDevice implements IOPortCapable, HardwareComponent {

    private static final int pagePortList0 = 0x1;

    private static final int pagePortList1 = 0x2;

    private static final int pagePortList2 = 0x3;

    private static final int pagePortList3 = 0x7;

    private static final int[] pagePortList = new int[] { pagePortList0, pagePortList1, pagePortList2, pagePortList3 };

    private static final int CMD_MEMORY_TO_MEMORY = 0x01;

    private static final int CMD_FIXED_ADDRESS = 0x02;

    private static final int CMD_BLOCK_CONTROLLER = 0x04;

    private static final int CMD_COMPRESSED_TIME = 0x08;

    private static final int CMD_CYCLIC_PRIORITY = 0x10;

    private static final int CMD_EXTENDED_WRITE = 0x20;

    private static final int CMD_LOW_DREQ = 0x40;

    private static final int CMD_LOW_DACK = 0x80;

    private static final int CMD_NOT_SUPPORTED = CMD_MEMORY_TO_MEMORY | CMD_FIXED_ADDRESS | CMD_COMPRESSED_TIME | CMD_CYCLIC_PRIORITY | CMD_EXTENDED_WRITE | CMD_LOW_DREQ | CMD_LOW_DACK;

    private int status;

    private int command;

    private int mask;

    private boolean flipFlop;

    private int dShift;

    private int iobase, pageBase, pageHBase;

    private int controllerNumber;

    private PhysicalAddressSpace memory;

    static class DMARegister {

        public static final int ADDRESS = 0;

        public static final int COUNT = 1;

        public int nowAddress;

        public int nowCount;

        public short baseAddress;

        public short baseCount;

        public int mode;

        public byte page, pageh, dack, eop;

        public DMATransferCapable transferDevice;

        public DMARegister() {
        }

        public void dumpState(DataOutput output) throws IOException {
            output.writeInt(nowAddress);
            output.writeInt(nowCount);
            output.writeShort(baseAddress);
            output.writeShort(baseCount);
            output.writeInt(mode);
            output.writeByte(page);
            output.writeByte(pageh);
            output.writeByte(dack);
            output.writeByte(eop);
        }

        public void loadState(DataInput input) throws IOException {
            nowAddress = input.readInt();
            nowCount = input.readInt();
            baseAddress = input.readShort();
            baseCount = input.readShort();
            mode = input.readInt();
            page = input.readByte();
            pageh = input.readByte();
            dack = input.readByte();
            eop = input.readByte();
        }

        public void reset() {
            transferDevice = null;
            nowAddress = nowCount = mode = 0;
            baseAddress = baseCount = 0;
            page = pageh = dack = eop = 0;
        }
    }

    private DMARegister[] dmaRegs;

    public DMAController(boolean highPageEnable, boolean zeroth) {
        ioportRegistered = false;
        this.dShift = zeroth ? 0 : 1;
        this.iobase = zeroth ? 0x00 : 0xc0;
        this.pageBase = zeroth ? 0x80 : 0x88;
        this.pageHBase = highPageEnable ? (zeroth ? 0x480 : 0x488) : -1;
        this.controllerNumber = zeroth ? 0 : 1;
        dmaRegs = new DMARegister[4];
        for (int i = 0; i < 4; i++) dmaRegs[i] = new DMARegister();
        this.reset();
    }

    public void dumpState(DataOutput output) throws IOException {
        output.writeInt(status);
        output.writeInt(command);
        output.writeInt(mask);
        output.writeBoolean(flipFlop);
        output.writeInt(dShift);
        output.writeInt(iobase);
        output.writeInt(pageBase);
        output.writeInt(pageHBase);
        output.writeInt(controllerNumber);
        output.writeInt(dmaRegs.length);
        for (int i = 0; i < dmaRegs.length; i++) dmaRegs[i].dumpState(output);
    }

    public void loadState(DataInput input) throws IOException {
        ioportRegistered = false;
        status = input.readInt();
        command = input.readInt();
        mask = input.readInt();
        flipFlop = input.readBoolean();
        dShift = input.readInt();
        iobase = input.readInt();
        pageBase = input.readInt();
        pageHBase = input.readInt();
        controllerNumber = input.readInt();
        int len = input.readInt();
        dmaRegs = new DMARegister[len];
        for (int i = 0; i < dmaRegs.length; i++) {
            dmaRegs[i] = new DMARegister();
            dmaRegs[i].loadState(input);
        }
    }

    public boolean isFirst() {
        return (this.dShift == 0);
    }

    public boolean reset() {
        for (int i = 0; i < dmaRegs.length; i++) dmaRegs[i].reset();
        this.writeController(0x0d << this.dShift, 0);
        memory = null;
        ioportRegistered = false;
        return true;
    }

    private void writeChannel(int portNumber, int data) {
        int port = (portNumber >>> dShift) & 0x0f;
        int channelNumber = port >>> 1;
        DMARegister r = dmaRegs[channelNumber];
        if (getFlipFlop()) {
            if ((port & 1) == DMARegister.ADDRESS) r.baseAddress = (short) ((r.baseAddress & 0xff) | ((data << 8) & 0xff00)); else r.baseCount = (short) ((r.baseCount & 0xff) | ((data << 8) & 0xff00));
            initChannel(channelNumber);
        } else {
            if ((port & 1) == DMARegister.ADDRESS) r.baseAddress = (short) ((r.baseAddress & 0xff00) | (data & 0xff)); else r.baseCount = (short) ((r.baseCount & 0xff00) | (data & 0xff));
        }
    }

    private void writeController(int portNumber, int data) {
        int port = (portNumber >>> this.dShift) & 0x0f;
        switch(port) {
            case 0x08:
                if ((data != 0) && ((data & CMD_NOT_SUPPORTED) != 0)) break;
                command = data;
                break;
            case 0x09:
                int channelNumber = data & 3;
                if ((data & 4) != 0) {
                    status |= 1 << (channelNumber + 4);
                } else {
                    status &= ~(1 << (channelNumber + 4));
                }
                status &= ~(1 << channelNumber);
                runTransfers();
                break;
            case 0x0a:
                if ((data & 0x4) != 0) {
                    mask |= 1 << (data & 3);
                } else {
                    mask &= ~(1 << (data & 3));
                    runTransfers();
                }
                break;
            case 0x0b:
                channelNumber = data & 3;
                dmaRegs[channelNumber].mode = data;
                break;
            case 0x0c:
                flipFlop = false;
                break;
            case 0x0d:
                flipFlop = false;
                mask = ~0;
                status = 0;
                command = 0;
                break;
            case 0x0e:
                mask = 0;
                runTransfers();
                break;
            case 0x0f:
                mask = data;
                runTransfers();
                break;
            default:
                break;
        }
    }

    private static final int[] channels = new int[] { -1, 2, 3, 1, -1, -1, -1, 0 };

    private void writePage(int portNumber, int data) {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber) {
            return;
        }
        dmaRegs[channelNumber].page = (byte) data;
    }

    private void writePageH(int portNumber, int data) {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber) {
            return;
        }
        dmaRegs[channelNumber].pageh = (byte) data;
    }

    private int readChannel(int portNumber) {
        int port = (portNumber >>> dShift) & 0x0f;
        int channelNumber = port >>> 1;
        int registerNumber = port & 1;
        DMARegister r = dmaRegs[channelNumber];
        int direction = ((r.mode & 0x20) == 0) ? 1 : -1;
        boolean flipflop = getFlipFlop();
        int val;
        if (registerNumber != 0) {
            val = ((0xffff & r.baseCount) << dShift) - r.nowCount;
        } else {
            val = r.nowAddress + r.nowCount * direction;
        }
        return (val >>> (dShift + (flipflop ? 0x8 : 0x0))) & 0xff;
    }

    private int readController(int portNumber) {
        int val;
        int port = (portNumber >>> dShift) & 0x0f;
        switch(port) {
            case 0x08:
                val = status;
                status &= 0xf0;
                break;
            case 0x0f:
                val = mask;
                break;
            default:
                val = 0;
                break;
        }
        return val;
    }

    private int readPage(int portNumber) {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber) {
            return 0;
        }
        return 0xff & dmaRegs[channelNumber].page;
    }

    private int readPageH(int portNumber) {
        int channelNumber = channels[portNumber & 7];
        if (-1 == channelNumber) {
            return 0;
        }
        return 0xff & dmaRegs[channelNumber].pageh;
    }

    public void ioPortWriteByte(int address, int data) {
        switch((address - iobase) >>> dShift) {
            case 0x0:
            case 0x1:
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x6:
            case 0x7:
                writeChannel(address, data);
                return;
            case 0x8:
            case 0x9:
            case 0xa:
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
            case 0xf:
                writeController(address, data);
                return;
            default:
                break;
        }
        switch(address - pageBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                writePage(address, data);
                return;
            default:
                break;
        }
        switch(address - pageHBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                writePageH(address, data);
                return;
            default:
                break;
        }
    }

    public void ioPortWriteWord(int address, int data) {
        this.ioPortWriteByte(address, data);
        this.ioPortWriteByte(address + 1, data >>> 8);
    }

    public void ioPortWriteLong(int address, int data) {
        this.ioPortWriteWord(address, data);
        this.ioPortWriteWord(address + 2, data >>> 16);
    }

    public int ioPortReadByte(int address) {
        switch((address - iobase) >>> dShift) {
            case 0x0:
            case 0x1:
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x6:
            case 0x7:
                return readChannel(address);
            case 0x8:
            case 0x9:
            case 0xa:
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
            case 0xf:
                return readController(address);
            default:
                break;
        }
        switch(address - pageBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                return readPage(address);
            default:
                break;
        }
        switch(address - pageHBase) {
            case pagePortList0:
            case pagePortList1:
            case pagePortList2:
            case pagePortList3:
                return readPageH(address);
            default:
                break;
        }
        return 0xff;
    }

    public int ioPortReadWord(int address) {
        return (0xff & this.ioPortReadByte(address)) | ((this.ioPortReadByte(address) << 8) & 0xff);
    }

    public int ioPortReadLong(int address) {
        return (0xffff & this.ioPortReadByte(address)) | ((this.ioPortReadByte(address) << 16) & 0xffff);
    }

    public int[] ioPortsRequested() {
        int[] temp;
        if (pageHBase >= 0) {
            temp = new int[16 + (2 * pagePortList.length)];
        } else {
            temp = new int[16 + pagePortList.length];
        }
        int j = 0;
        for (int i = 0; i < 8; i++) {
            temp[j++] = iobase + (i << this.dShift);
        }
        for (int i = 0; i < pagePortList.length; i++) {
            temp[j++] = pageBase + pagePortList[i];
            if (pageHBase >= 0) {
                temp[j++] = pageHBase + pagePortList[i];
            }
        }
        for (int i = 0; i < 8; i++) {
            temp[j++] = iobase + ((i + 8) << this.dShift);
        }
        return temp;
    }

    private boolean getFlipFlop() {
        boolean ff = flipFlop;
        flipFlop = !ff;
        return ff;
    }

    private void initChannel(int channelNumber) {
        DMARegister r = dmaRegs[channelNumber];
        r.nowAddress = (0xffff & r.baseAddress) << dShift;
        r.nowCount = 0;
    }

    public void runTransfers() {
        int value = ~mask & (status >>> 4) & 0xf;
        if (value == 0) return;
        while (value != 0) {
            int channelNumber = Integer.numberOfTrailingZeros(value);
            if (channelNumber < 4) runChannel(channelNumber); else break;
            value &= ~(1 << channelNumber);
        }
    }

    private void runChannel(int channelNumber) {
        DMARegister r = dmaRegs[channelNumber];
        int n = r.transferDevice.transferHandler(channelNumber + (controllerNumber << 2), r.nowCount, (r.baseCount + 1) << controllerNumber);
        r.nowCount = n;
    }

    public int getChannelMode(int channelNumber) {
        return dmaRegs[channelNumber].mode;
    }

    public void holdDREQ(int channelNumber) {
        status |= 1 << (channelNumber + 4);
        runTransfers();
    }

    public void releaseDREQ(int channelNumber) {
        status &= ~(1 << (channelNumber + 4));
    }

    public void registerChannel(int channelNumber, DMATransferCapable device) {
        dmaRegs[channelNumber].transferDevice = device;
    }

    public int readMemory(int channelNumber, byte[] buffer, int bufferOffset, int position, int length) {
        DMARegister r = dmaRegs[channelNumber];
        long address = ((r.pageh & 0x7fl) << 24) | ((0xffl & r.page) << 16) | (0xffffffffl & r.nowAddress);
        if ((r.mode & 0x20) != 0) {
            System.err.println("DMA Read In Address Decrement Mode!");
            memory.copyContentsInto((int) (address - position - length), buffer, bufferOffset, length);
            for (int left = bufferOffset, right = bufferOffset + length - 1; left < right; left++, right--) {
                byte temp = buffer[left];
                buffer[left] = buffer[right];
                buffer[right] = temp;
            }
        } else memory.copyContentsInto((int) (address + position), buffer, bufferOffset, length);
        return length;
    }

    public int writeMemory(int channelNumber, byte[] buffer, int bufferOffset, int position, int length) {
        DMARegister r = dmaRegs[channelNumber];
        long address = ((0x7fl & r.pageh) << 24) | ((0xffl & r.page) << 16) | (0xffffffffl & r.nowAddress);
        if ((r.mode & 0x20) != 0) {
            System.err.println("DMA Write In Address Decrement Mode!");
            for (int left = bufferOffset, right = bufferOffset + length - 1; left < right; left++, right--) {
                byte temp = buffer[left];
                buffer[left] = buffer[right];
                buffer[right] = temp;
            }
            memory.copyContentsFrom((int) (address - position - length), buffer, bufferOffset, length);
        } else memory.copyContentsFrom((int) (address + position), buffer, bufferOffset, length);
        return length;
    }

    private boolean ioportRegistered;

    public boolean initialised() {
        return ((memory != null) && ioportRegistered);
    }

    public boolean updated() {
        return memory.updated() && ioportRegistered;
    }

    public void acceptComponent(HardwareComponent component) {
        if (component instanceof PhysicalAddressSpace) this.memory = (PhysicalAddressSpace) component;
        if (component instanceof IOPortHandler) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public void updateComponent(HardwareComponent component) {
        if (component instanceof IOPortHandler) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public void timerCallback() {
    }

    public String toString() {
        return "DMA Controller [element " + dShift + "]";
    }

    public byte getIOPortByte(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        int result = ioPortReadByte(portAddress);
        return (byte) result;
    }

    public byte[] getIOPortWord(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        int result = ioPortReadWord(portAddress);
        return new byte[] { ((byte) ((result >> 8) & 0xFF)), ((byte) (result & 0xFF)) };
    }

    public byte[] getIOPortDoubleWord(int portAddress) throws ModuleException, ModuleUnknownPort, ModuleWriteOnlyPortException {
        int result = ioPortReadLong(portAddress);
        return new byte[] { ((byte) ((result >> 24) & 0xFF)), ((byte) ((result >> 16) & 0xFF)), ((byte) ((result >> 8) & 0xFF)), ((byte) (result & 0xFF)) };
    }

    public void setIOPortByte(int portAddress, byte data) throws ModuleException, ModuleUnknownPort {
        ioPortWriteByte(portAddress, data);
    }

    public void setIOPortWord(int portAddress, byte[] dataWord) throws ModuleException, ModuleUnknownPort {
        ioPortWriteLong(portAddress, ((((int) dataWord[0]) & 0xFF) << 8) + (((int) dataWord[1]) & 0xFF));
    }

    public void setIOPortDoubleWord(int portAddress, byte[] dataDoubleWord) throws ModuleException, ModuleUnknownPort {
        ioPortWriteLong(portAddress, ((((int) dataDoubleWord[3]) & 0xFF) << 24) + ((((int) dataDoubleWord[2]) & 0xFF) << 16) + ((((int) dataDoubleWord[1]) & 0xFF) << 8) + (((int) dataDoubleWord[0]) & 0xFF));
    }

    public void notImplemented() {
        System.out.println("[DMAController]: ModuleDevice method not implemented");
    }

    @Override
    public int getUpdateInterval() {
        notImplemented();
        return 0;
    }

    @Override
    public void setUpdateInterval(int interval) {
        notImplemented();
    }

    @Override
    public void update() {
        notImplemented();
    }

    @Override
    public String[] getConnection() {
        notImplemented();
        return null;
    }

    @Override
    public byte[] getData(Module module) {
        notImplemented();
        return null;
    }

    @Override
    public boolean getDebugMode() {
        notImplemented();
        return false;
    }

    @Override
    public String getDump() {
        notImplemented();
        return null;
    }

    @Override
    public int getID() {
        notImplemented();
        return 0;
    }

    @Override
    public String getName() {
        notImplemented();
        return null;
    }

    @Override
    public String getType() {
        notImplemented();
        return null;
    }

    @Override
    public boolean isConnected() {
        notImplemented();
        return false;
    }

    @Override
    public boolean isObserved() {
        notImplemented();
        return false;
    }

    @Override
    public boolean setConnection(Module mod) {
        notImplemented();
        return false;
    }

    @Override
    public boolean setData(byte[] data, Module module) {
        notImplemented();
        return false;
    }

    @Override
    public boolean setData(String[] data, Module module) {
        notImplemented();
        return false;
    }

    @Override
    public void setDebugMode(boolean status) {
        notImplemented();
    }

    @Override
    public void setObserved(boolean status) {
        notImplemented();
    }

    @Override
    public void start() {
        notImplemented();
    }

    @Override
    public void stop() {
        notImplemented();
    }
}
