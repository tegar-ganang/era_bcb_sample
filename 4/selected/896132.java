package jpcsp.HLE.kernel.types;

import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelMutexInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public final int initCount;

    public int lockedCount;

    public int numWaitThreads;

    public final int uid;

    public int threadid;

    public SceKernelMutexInfo(String name, int count, int attr) {
        this.name = name;
        this.attr = attr;
        initCount = count;
        lockedCount = count;
        numWaitThreads = 0;
        uid = SceUidManager.getNewUid("ThreadMan-Mutex");
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(initCount);
        write32(lockedCount);
        write32(numWaitThreads);
    }

    @Override
    public String toString() {
        return String.format("SceKernelMutexInfo(uid=%x, name=%s, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X)", uid, name, initCount, lockedCount, numWaitThreads, attr);
    }
}
