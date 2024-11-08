package jpcsp.HLE.kernel.types.interrupts;

import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;

public class VTimerInterruptHandler extends AbstractAllegrexInterruptHandler {

    private SceKernelVTimerInfo sceKernelVTimerInfo;

    public VTimerInterruptHandler(SceKernelVTimerInfo sceKernelVTimerInfo) {
        super(sceKernelVTimerInfo.handlerAddress);
        this.sceKernelVTimerInfo = sceKernelVTimerInfo;
        setArgument(0, sceKernelVTimerInfo.uid);
        setArgument(3, sceKernelVTimerInfo.handlerArgument);
    }

    @Override
    public void copyArgumentsToCpu(CpuState cpu) {
        Memory mem = Memory.getInstance();
        int internalMemory = sceKernelVTimerInfo.getInternalMemory();
        if (internalMemory != 0) {
            mem.write64(internalMemory, sceKernelVTimerInfo.schedule);
            mem.write64(internalMemory + 8, Modules.ThreadManForUserModule.getVTimerTime(sceKernelVTimerInfo));
            setArgument(1, internalMemory);
            setArgument(2, internalMemory + 8);
        } else {
            setArgument(1, 0);
            setArgument(2, 0);
        }
        super.copyArgumentsToCpu(cpu);
    }
}
