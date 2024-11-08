package jpcsp.HLE.kernel.types;

import jpcsp.Memory;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelLwMutexInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public final int lwMutexUid;

    public final int lwMutexOpaqueWorkAreaAddr;

    public final int initCount;

    public int lockedCount;

    public int numWaitThreads;

    public final int uid;

    public int threadid;

    public SceKernelLwMutexInfo(int workArea, String name, int count, int attr) {
        Memory mem = Memory.getInstance();
        this.lwMutexUid = 0;
        this.lwMutexOpaqueWorkAreaAddr = workArea;
        this.name = name;
        this.attr = attr;
        initCount = count;
        lockedCount = count;
        numWaitThreads = 0;
        uid = SceUidManager.getNewUid("ThreadMan-LwMutex");
        mem.write32(lwMutexOpaqueWorkAreaAddr, uid);
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(lwMutexUid);
        write32(lwMutexOpaqueWorkAreaAddr);
        write32(initCount);
        write32(lockedCount);
        write32(numWaitThreads);
    }

    @Override
    public String toString() {
        return String.format("SceKernelLwMutexInfo(uid=%x, name=%s, mutexUid=%x, lwMutexOpaqueWorkAreaAddr=0x%X, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X, threadid=0x%X)", uid, name, lwMutexUid, lwMutexOpaqueWorkAreaAddr, initCount, lockedCount, numWaitThreads, attr, threadid);
    }
}
