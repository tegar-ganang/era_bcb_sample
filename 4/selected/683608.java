package jpcsp.HLE.kernel.types;

import static jpcsp.HLE.kernel.managers.VplManager.PSP_VPL_ATTR_ADDR_HIGH;
import java.util.HashMap;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.VplManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.util.Utilities;

public class SceKernelVplInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public final int poolSize;

    public int freeSize;

    public int numWaitThreads;

    public static final int vplHeaderSize = 32;

    public static final int vplBlockHeaderSize = 8;

    public static final int vplAddrAlignment = 7;

    private final SysMemInfo sysMemInfo;

    public final int uid;

    public final int partitionid;

    private final int allocAddress;

    private HashMap<Integer, Integer> dataBlockMap;

    private MemoryChunkList freeMemoryChunks;

    private SceKernelVplInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;
        poolSize = size - vplHeaderSize;
        freeSize = poolSize;
        numWaitThreads = 0;
        dataBlockMap = new HashMap<Integer, Integer>();
        uid = SceUidManager.getNewUid("ThreadMan-Vpl");
        this.partitionid = partitionid;
        int totalVplSize = Utilities.alignUp(size, vplAddrAlignment);
        sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, String.format("ThreadMan-Vpl-0x%x-%s", uid, name), memType, totalVplSize, 0);
        if (sysMemInfo == null) throw new RuntimeException("SceKernelVplInfo: not enough free mem");
        int addr = sysMemInfo.addr;
        Memory mem = Memory.getInstance();
        mem.write32(addr, addr - 1);
        mem.write32(addr + 4, size - 8);
        mem.write32(addr + 8, 0);
        mem.write32(addr + 12, addr + size - 16);
        mem.write32(addr + 16, 0);
        mem.write32(addr + 20, 0);
        allocAddress = addr;
        MemoryChunk initialMemoryChunk = new MemoryChunk(addr + vplHeaderSize, totalVplSize - vplHeaderSize);
        freeMemoryChunks = new MemoryChunkList(initialMemoryChunk);
    }

    public static SceKernelVplInfo tryCreateVpl(String name, int partitionid, int attr, int size, int memType) {
        SceKernelVplInfo info = null;
        int totalVplSize = Utilities.alignUp(size, vplAddrAlignment);
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();
        if (totalVplSize <= maxFreeSize) {
            info = new SceKernelVplInfo(name, partitionid, attr, totalVplSize, memType);
        } else {
            VplManager.log.warn(String.format("tryCreateVpl not enough free mem (want=%d ,free=%d, diff=%d)", totalVplSize, maxFreeSize, totalVplSize - maxFreeSize));
        }
        return info;
    }

    public void delete() {
        Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(poolSize);
        write32(freeSize);
        write32(numWaitThreads);
    }

    /** @return true on success */
    public boolean free(int addr) {
        if (!dataBlockMap.containsKey(addr)) {
            if (VplManager.log.isDebugEnabled()) {
                VplManager.log.debug(String.format("Free VPL 0x%08X address not allocated", addr));
            }
            return false;
        }
        Memory mem = Memory.getInstance();
        int top = mem.read32(addr - vplBlockHeaderSize);
        if (top != allocAddress) {
            VplManager.log.warn(String.format("Free VPL 0x%08X corrupted header", addr));
            return false;
        }
        int deallocSize = dataBlockMap.remove(addr);
        freeSize += deallocSize;
        MemoryChunk memoryChunk = new MemoryChunk(addr - vplBlockHeaderSize, deallocSize);
        freeMemoryChunks.add(memoryChunk);
        if (VplManager.log.isDebugEnabled()) {
            VplManager.log.debug(String.format("Free VPL: Block 0x%08X with size=%d freed", addr, deallocSize));
        }
        return true;
    }

    public int alloc(int size) {
        int addr = 0;
        int allocSize = Utilities.alignUp(size, vplAddrAlignment) + vplBlockHeaderSize;
        if (allocSize <= freeSize) {
            if ((attr & PSP_VPL_ATTR_ADDR_HIGH) == PSP_VPL_ATTR_ADDR_HIGH) {
                addr = freeMemoryChunks.allocHigh(allocSize, vplAddrAlignment);
            } else {
                addr = freeMemoryChunks.allocLow(allocSize, vplAddrAlignment);
            }
            if (addr != 0) {
                Memory mem = Memory.getInstance();
                mem.write32(addr, allocAddress);
                mem.write32(addr + 4, 0);
                addr += vplBlockHeaderSize;
                freeSize -= allocSize;
                dataBlockMap.put(addr, allocSize);
            }
        }
        return addr;
    }
}
