package jpcsp.HLE.kernel.types;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelCallbackInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int threadId;

    public final int callback_addr;

    public final int callback_arg_addr;

    public int notifyCount;

    public int notifyArg;

    public final int uid;

    public SceKernelCallbackInfo(String name, int threadId, int callback_addr, int callback_arg_addr) {
        this.name = name;
        this.threadId = threadId;
        this.callback_addr = callback_addr;
        this.callback_arg_addr = callback_arg_addr;
        notifyCount = 0;
        notifyArg = 0;
        uid = SceUidManager.getNewUid("ThreadMan-callback");
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(threadId);
        write32(callback_addr);
        write32(callback_arg_addr);
        write32(notifyCount);
        write32(notifyArg);
    }

    /** Call this to switch in the callback, in a given thread context.
     */
    public void startContext(SceKernelThreadInfo thread, IAction afterAction) {
        int registerA0 = notifyCount;
        int registerA1 = notifyArg;
        int registerA2 = callback_arg_addr;
        notifyCount = 0;
        notifyArg = 0;
        Modules.ThreadManForUserModule.executeCallback(thread, callback_addr, afterAction, true, registerA0, registerA1, registerA2);
    }

    @Override
    public String toString() {
        return String.format("name:'%s', thread:'%s', PC:%08X, $a0:%08X, $a1: %08X, $a2: %08X", name, Modules.ThreadManForUserModule.getThreadName(threadId), callback_addr, notifyCount, notifyArg, callback_arg_addr);
    }
}
