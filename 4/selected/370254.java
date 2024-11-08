package jpcsp.HLE.kernel.types;

import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelSemaInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public final int initCount;

    public int currentCount;

    public final int maxCount;

    public int numWaitThreads;

    public final int uid;

    public SceKernelSemaInfo(String name, int attr, int initCount, int maxCount) {
        this.name = name;
        this.attr = attr;
        this.initCount = initCount;
        this.currentCount = initCount;
        this.maxCount = maxCount;
        this.numWaitThreads = 0;
        uid = SceUidManager.getNewUid("ThreadMan-sema");
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(initCount);
        write32(currentCount);
        write32(maxCount);
        write32(numWaitThreads);
    }
}
