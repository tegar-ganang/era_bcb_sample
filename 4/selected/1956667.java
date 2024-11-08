package jpcsp.HLE.kernel.types;

public class SceKernelSystemStatus extends pspAbstractMemoryMappedStructureVariableLength {

    /** The status ? */
    public int status;

    /** The number of cpu clocks in the idle thread */
    public long idleClocks;

    /** Number of times we resumed from idle */
    public int comesOutOfIdleCount;

    /** Number of thread context switches */
    public int threadSwitchCount;

    /** Number of vfpu switches ? */
    public int vfpuSwitchCount;

    @Override
    protected void read() {
        super.read();
        status = read32();
        idleClocks = read64();
        comesOutOfIdleCount = read32();
        threadSwitchCount = read32();
        vfpuSwitchCount = read32();
    }

    @Override
    protected void write() {
        super.write();
        write32(status);
        write64(idleClocks);
        write32(comesOutOfIdleCount);
        write32(threadSwitchCount);
        write32(vfpuSwitchCount);
    }
}
