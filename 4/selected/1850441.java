package jpcsp.HLE.kernel.types;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

public class SceKernelMppInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public final int bufSize;

    public int freeSize;

    public int numSendWaitThreads;

    public int numReceiveWaitThreads;

    private final SysMemInfo sysMemInfo;

    public final int uid;

    public final int partitionid;

    public final int address;

    private int head;

    private int tail;

    private SceKernelMppInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;
        bufSize = size;
        freeSize = size;
        numSendWaitThreads = 0;
        numReceiveWaitThreads = 0;
        sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, "ThreadMan-MsgPipe", memType, size, 0);
        if (sysMemInfo == null) {
            throw new RuntimeException("SceKernelFplInfo: not enough free mem");
        }
        address = sysMemInfo.addr;
        uid = SceUidManager.getNewUid("ThreadMan-MsgPipe");
        this.partitionid = partitionid;
        head = 0;
        tail = 0;
    }

    public static SceKernelMppInfo tryCreateMpp(String name, int partitionid, int attr, int size, int memType) {
        SceKernelMppInfo info = null;
        int alignedSize = (size + 0xFF) & ~0xFF;
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();
        if (size <= 0) {
            Modules.log.warn("tryCreateMpp invalid size " + size);
        } else if (alignedSize > maxFreeSize) {
            Modules.log.warn("tryCreateMpp not enough free mem (want=" + alignedSize + ",free=" + maxFreeSize + ",diff=" + (alignedSize - maxFreeSize) + ")");
        } else {
            info = new SceKernelMppInfo(name, partitionid, attr, size, memType);
        }
        return info;
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(bufSize);
        write32(freeSize);
        write32(numSendWaitThreads);
        write32(numReceiveWaitThreads);
    }

    public int availableReadSize() {
        return bufSize - freeSize;
    }

    public int availableWriteSize() {
        return freeSize;
    }

    public void deleteSysMemInfo() {
        Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    public void append(Memory mem, int src, int size) {
        int copySize;
        freeSize -= size;
        while (size > 0) {
            copySize = Math.min(bufSize - tail, size);
            mem.memcpy(address + tail, src, copySize);
            src += copySize;
            size -= copySize;
            tail = (tail + copySize) % bufSize;
        }
    }

    public void consume(Memory mem, int dst, int size) {
        int copySize;
        freeSize += size;
        while (size > 0) {
            copySize = Math.min(bufSize - head, size);
            mem.memcpy(dst, address + head, copySize);
            dst += copySize;
            size -= copySize;
            head = (head + copySize) % bufSize;
        }
    }
}
