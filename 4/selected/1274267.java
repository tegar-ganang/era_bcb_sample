package jpcsp.HLE.kernel.types;

import java.util.LinkedList;
import java.util.List;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.format.DeferredStub;
import jpcsp.format.PSF;
import jpcsp.format.PSPModuleInfo;
import jpcsp.util.Utilities;

public class SceModule {

    public int next;

    public short attribute;

    public byte[] version = new byte[2];

    public String modname;

    public final byte terminal = (byte) 0;

    public int status;

    public int unk1;

    public int modid;

    public int usermod_thid;

    public int memid;

    public int mpidtext;

    public int mpiddata;

    public int ent_top;

    public int ent_size;

    public int stub_top;

    public int stub_size;

    public int module_start_func;

    public int module_stop_func;

    public int module_bootstart_func;

    public int module_reboot_before_func;

    public int module_reboot_phase_func;

    public int entry_addr;

    public int gp_value;

    public int text_addr;

    public int text_size;

    public int data_size;

    public int bss_size;

    private List<SysMemInfo> allocatedMemory;

    public int nsegment;

    public int[] segmentaddr = new int[4];

    public int[] segmentsize = new int[4];

    public int module_start_thread_priority;

    public int module_start_thread_stacksize;

    public int module_start_thread_attr;

    public int module_stop_thread_priority;

    public int module_stop_thread_stacksize;

    public int module_stop_thread_attr;

    public int module_reboot_before_thread_priority;

    public int module_reboot_before_thread_stacksize;

    public int module_reboot_before_thread_attr;

    public static final int size = 196;

    public final int address;

    public final boolean isFlashModule;

    public boolean isLoaded;

    public boolean isStarted;

    public boolean isStopped;

    private static SceModule previousModule;

    public int fileFormat;

    public String pspfilename;

    public PSF psf;

    public int loadAddressLow, loadAddressHigh;

    public int baseAddress;

    public int[] initsection;

    public int[] finisection;

    public int[] stubtextsection;

    public List<DeferredStub> unresolvedImports;

    public int importFixupAttempts;

    private static int sceModuleAddressOffset = 0x08400000;

    public static void ResetAllocator() {
        sceModuleAddressOffset = 0x08400000;
    }

    public SceModule(boolean isFlashModule) {
        this.isFlashModule = isFlashModule;
        modid = SceUidManager.getNewUid("SceModule");
        sceModuleAddressOffset -= (size + 256) & ~255;
        address = sceModuleAddressOffset;
        if (previousModule != null) previousModule.next = address;
        previousModule = this;
        fileFormat = Loader.FORMAT_UNKNOWN;
        initsection = new int[2];
        finisection = new int[2];
        stubtextsection = new int[2];
        unresolvedImports = new LinkedList<DeferredStub>();
        importFixupAttempts = 0;
        allocatedMemory = new LinkedList<SysMemInfo>();
    }

    public void load() {
        isLoaded = true;
    }

    public void unload() {
        isLoaded = false;
        free();
    }

    public void start() {
        isStarted = true;
    }

    public void stop() {
        isStopped = true;
    }

    public boolean isModuleLoaded() {
        return isLoaded;
    }

    public boolean isModuleStarted() {
        return isStarted;
    }

    public boolean isModuleStopped() {
        return isStopped;
    }

    /** For use when unloading modules. */
    public void free() {
        for (SysMemInfo sysMemInfo : allocatedMemory) {
            Modules.SysMemUserForUserModule.free(sysMemInfo);
        }
        allocatedMemory.clear();
    }

    public void addAllocatedMemory(SysMemInfo sysMemInfo) {
        if (sysMemInfo != null) {
            allocatedMemory.add(sysMemInfo);
        }
    }

    public void write(Memory mem, int address) {
        mem.write32(address, next);
        mem.write16(address + 4, attribute);
        mem.write8(address + 6, version[0]);
        mem.write8(address + 7, version[1]);
        Utilities.writeStringNZ(mem, address + 8, 28, modname);
        mem.write32(address + 36, status);
        mem.write32(address + 40, unk1);
        mem.write32(address + 44, modid);
        mem.write32(address + 48, usermod_thid);
        mem.write32(address + 52, memid);
        mem.write32(address + 56, mpidtext);
        mem.write32(address + 60, mpiddata);
        mem.write32(address + 64, ent_top);
        mem.write32(address + 68, ent_size);
        mem.write32(address + 72, stub_top);
        mem.write32(address + 76, stub_size);
        mem.write32(address + 80, module_start_func);
        mem.write32(address + 84, module_stop_func);
        mem.write32(address + 88, module_bootstart_func);
        mem.write32(address + 92, module_reboot_before_func);
        mem.write32(address + 96, module_reboot_phase_func);
        mem.write32(address + 100, entry_addr);
        mem.write32(address + 104, gp_value);
        mem.write32(address + 108, text_addr);
        mem.write32(address + 112, text_size);
        mem.write32(address + 116, data_size);
        mem.write32(address + 120, bss_size);
        mem.write32(address + 124, nsegment);
        mem.write32(address + 128, segmentaddr[0]);
        mem.write32(address + 132, segmentaddr[1]);
        mem.write32(address + 136, segmentaddr[2]);
        mem.write32(address + 140, segmentaddr[3]);
        mem.write32(address + 144, segmentsize[0]);
        mem.write32(address + 148, segmentsize[1]);
        mem.write32(address + 152, segmentsize[2]);
        mem.write32(address + 156, segmentsize[3]);
        mem.write32(address + 160, module_start_thread_priority);
        mem.write32(address + 164, module_start_thread_stacksize);
        mem.write32(address + 168, module_start_thread_attr);
        mem.write32(address + 172, module_stop_thread_priority);
        mem.write32(address + 176, module_stop_thread_stacksize);
        mem.write32(address + 180, module_stop_thread_attr);
        mem.write32(address + 184, module_reboot_before_thread_priority);
        mem.write32(address + 188, module_reboot_before_thread_stacksize);
        mem.write32(address + 192, module_reboot_before_thread_attr);
    }

    public void read(Memory mem, int address) {
        next = mem.read32(address);
        attribute = (short) mem.read16(address + 4);
        version[0] = (byte) mem.read8(address + 6);
        version[1] = (byte) mem.read8(address + 7);
        modname = Utilities.readStringNZ(mem, address + 8, 28);
        status = mem.read32(address + 36);
        unk1 = mem.read32(address + 40);
        modid = mem.read32(address + 44);
        usermod_thid = mem.read32(address + 48);
        memid = mem.read32(address + 52);
        mpidtext = mem.read32(address + 56);
        mpiddata = mem.read32(address + 60);
        ent_top = mem.read32(address + 64);
        ent_size = mem.read32(address + 68);
        stub_top = mem.read32(address + 72);
        stub_size = mem.read32(address + 76);
        module_start_func = mem.read32(address + 80);
        module_stop_func = mem.read32(address + 84);
        module_bootstart_func = mem.read32(address + 88);
        module_reboot_before_func = mem.read32(address + 92);
        module_reboot_phase_func = mem.read32(address + 96);
        entry_addr = mem.read32(address + 100);
        gp_value = mem.read32(address + 104);
        text_addr = mem.read32(address + 108);
        text_size = mem.read32(address + 112);
        data_size = mem.read32(address + 116);
        bss_size = mem.read32(address + 120);
        nsegment = mem.read32(address + 124);
        segmentaddr[0] = mem.read32(address + 128);
        segmentaddr[1] = mem.read32(address + 132);
        segmentaddr[2] = mem.read32(address + 136);
        segmentaddr[3] = mem.read32(address + 140);
        segmentsize[0] = mem.read32(address + 144);
        segmentsize[1] = mem.read32(address + 148);
        segmentsize[2] = mem.read32(address + 152);
        segmentsize[3] = mem.read32(address + 156);
        module_start_thread_priority = mem.read32(address + 160);
        module_start_thread_stacksize = mem.read32(address + 164);
        module_start_thread_attr = mem.read32(address + 168);
        module_stop_thread_priority = mem.read32(address + 172);
        module_stop_thread_stacksize = mem.read32(address + 176);
        module_stop_thread_attr = mem.read32(address + 180);
        module_reboot_before_thread_priority = mem.read32(address + 184);
        module_reboot_before_thread_stacksize = mem.read32(address + 188);
        module_reboot_before_thread_attr = mem.read32(address + 192);
    }

    /** initialise ourself from a PSPModuleInfo object.
     * PSPModuleInfo object comes from the loader/ELF. */
    public void copy(PSPModuleInfo moduleInfo) {
        attribute = (short) (moduleInfo.getM_attr() & 0xFFFF);
        version[0] = (byte) (moduleInfo.getM_version() & 0xFF);
        version[1] = (byte) ((moduleInfo.getM_version() >> 8) & 0xFF);
        modname = moduleInfo.getM_namez();
        gp_value = (int) (moduleInfo.getM_gp() & 0xFFFFFFFFL);
        ent_top = (int) moduleInfo.getM_exports();
        ent_size = (int) moduleInfo.getM_exp_end() - ent_top;
        stub_top = (int) moduleInfo.getM_imports();
        stub_size = (int) moduleInfo.getM_imp_end() - stub_top;
    }
}
