package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Paging;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;
import jdos.util.Ptr;
import jdos.util.StringHelper;
import java.util.Arrays;

public class Memory extends Module_base {

    public static final int MEM_PAGESIZE = 4096;

    public static int MEM_SIZE = 0;

    static final int EXTRA_MEM = 8196;

    static int highwaterMark;

    static Ptr host_memory;

    public static int[] direct;

    public static int allocate(int size) {
        int result = highwaterMark;
        highwaterMark += size;
        return result;
    }

    public static byte host_readbs(int address) {
        return (byte) (direct[(address >> 2)] >>> ((address & 0x3) << 3));
    }

    public static short host_readb(int address) {
        return (short) ((direct[(address >> 2)] >>> ((address & 0x3) << 3)) & 0xFF);
    }

    public static int host_readw(int address) {
        int rem = address & 0x3;
        int[] local = direct;
        int index = (address >>> 2);
        int val = local[index] >>> (rem << 3);
        if (rem == 3) {
            val |= local[index + 1] << 8;
        }
        return val & 0xFFFF;
    }

    public static int host_readd(int address) {
        int rem = (address & 0x3);
        if (rem == 0) {
            return direct[address >>> 2];
        }
        int off = rem << 3;
        int[] local = direct;
        int index = (address >>> 2);
        return local[index] >>> off | local[index + 1] << (32 - off);
    }

    public static void host_writeb(int address, short value) {
        int off = (address & 0x3) << 3;
        int[] local = direct;
        int mask = ~(0xFF << off);
        int index = (address >>> 2);
        int val = local[index] & mask | (value & 0xFF) << off;
        local[index] = val;
    }

    public static void host_writebs(int address, byte value) {
        int off = (address & 0x3) << 3;
        int[] local = direct;
        int mask = ~(0xFF << off);
        int index = (address >>> 2);
        int val = local[index] & mask | (value & 0xFF) << off;
        local[index] = val;
    }

    public static void host_writew(int address, int value) {
        int rem = (address & 0x3);
        int[] local = direct;
        int index = (address >>> 2);
        value &= 0xFFFF;
        if (rem == 3) {
            local[index] = (local[index] & 0xFFFFFF | value << 24);
            index++;
            local[index] = (local[index] & 0xFFFFFF00 | value >>> 8);
        } else {
            int off = rem << 3;
            int mask = ~(0xFFFF << off);
            local[index] = (local[index] & mask | value << off);
        }
    }

    public static void host_writed(int address, int val) {
        int rem = (address & 0x3);
        if (rem == 0) {
            direct[address >>> 2] = val;
        } else {
            int index = (address >>> 2);
            int[] local = direct;
            int off = rem << 3;
            int mask = -1 << off;
            local[index] = (local[index] & ~mask) | (val << off);
            index++;
            local[index] = (local[index] & mask) | (val >>> (32 - off));
        }
    }

    public static void host_memcpy(int dest, byte[] src, int srcOffset, int size) {
        for (int i = 0; i < size; i++) host_writeb(dest++, src[srcOffset++]);
    }

    public static void host_memcpy(byte[] dest, int dest_offset, int src, int size) {
        int begin = src & 3;
        int end = size & ~3;
        for (int i = 0; i < begin && i < size; i++) dest[i + dest_offset] = host_readbs(src + i);
        int off = dest_offset + begin;
        int index = (src + begin) >> 2;
        for (int i = begin; i < end && i + 3 < size; i += 4) {
            int v = direct[index++];
            dest[off++] = (byte) v;
            dest[off++] = (byte) (v >> 8);
            dest[off++] = (byte) (v >> 16);
            dest[off++] = (byte) (v >> 24);
        }
        for (int i = end; i < size; i++) dest[i + dest_offset] = host_readbs(src + i);
    }

    public static void host_memcpy(int[] dest, int dest_offset, int src, int size) {
        System.arraycopy(direct, src >> 2, dest, dest_offset >> 2, size >> 2);
    }

    public static void host_memcpy(int dst, int src, int amount) {
        int src_align = src & 0x3;
        int dst_align = dst & 0x3;
        if (src_align == dst_align) {
            while ((src & 0x3) > 0 && amount > 0) {
                host_writeb(dst++, host_readb(src++));
                amount--;
            }
            int len = (amount >>> 2);
            if (len > 0) System.arraycopy(direct, src >>> 2, direct, dst >>> 2, len);
            len = len << 2;
            if (len == amount) return;
            dst += len;
            src += len;
            amount -= len;
        }
        for (int i = 0; i < amount; i++) {
            host_writeb(dst++, host_readb(src++));
        }
    }

    public static void host_zeroset(int dest, int size) {
        if ((dest & 0x3) == 0) {
            int index = (dest >>> 2);
            int len = (size >>> 2);
            Arrays.fill(direct, index, index + len, 0);
            size = (size & 0x3) << 3;
            dest += len << 2;
        }
        byte b = (byte) 0;
        for (int i = 0; i < size; i++) host_writeb(dest++, b);
    }

    public static void host_memset(int dest, int size, int value) {
        if ((dest & 0x3) == 0) {
            int index = (dest >>> 2);
            int len = (size >>> 2);
            value &= 0xFF;
            Arrays.fill(direct, index, index + len, (value << 24) | (value << 16) | (value << 8) | value);
            size = (size & 0x3) << 3;
            dest += len << 2;
        }
        byte b = (byte) value;
        for (int i = 0; i < size; i++) host_writeb(dest++, b);
    }

    public static void phys_writes(int addr, String s) {
        int i;
        byte[] b = s.getBytes();
        for (i = 0; i < s.length(); i++) host_writeb(addr + i, b[i]);
        host_writeb(addr + i, (byte) 0);
    }

    public static void phys_writeb(int addr, int val) {
        host_writeb(addr, (short) val);
    }

    public static void phys_writew(int addr, int val) {
        host_writew(addr, val);
    }

    public static void phys_writed(int addr, int val) {
        host_writed(addr, val);
    }

    public static short phys_readb(int addr) {
        return host_readb(addr);
    }

    public static int phys_readw(int addr) {
        return host_readw(addr);
    }

    public static int phys_readd(int addr) {
        return host_readd(addr);
    }

    public static short real_readb(int seg, int off) {
        return mem_readb((seg << 4) + off);
    }

    public static int real_readw(int seg, int off) {
        return mem_readw((seg << 4) + off);
    }

    public static int real_readd(int seg, int off) {
        return mem_readd((seg << 4) + off);
    }

    public static void real_writeb(int seg, int off, int val) {
        mem_writeb(((seg << 4) + off), val);
    }

    public static void real_writew(int seg, int off, int val) {
        mem_writew(((seg << 4) + off), val);
    }

    public static void real_writed(int seg, int off, int val) {
        mem_writed(((seg << 4) + off), val);
    }

    public static int RealSeg(int pt) {
        return (int) ((pt >>> 16) & 0xFFFF);
    }

    public static int RealOff(int pt) {
        return (int) (pt & 0xffff);
    }

    public static int Real2Phys(int pt) {
        return (RealSeg(pt) << 4) + RealOff(pt);
    }

    public static int PhysMake(int seg, int off) {
        return (seg << 4) + off;
    }

    public static int RealMake(int seg, int off) {
        return (seg << 16) + off;
    }

    public static void RealSetVec(int vec, int pt) {
        mem_writed(vec << 2, pt);
    }

    public static void RealSetVec(int vec, int pt, IntRef old) {
        old.value = mem_readd(vec << 2);
        mem_writed(vec << 2, pt);
    }

    public static int RealSetVec2(int vec, int pt) {
        int ret = mem_readd(vec << 2);
        mem_writed(vec << 2, pt);
        return ret;
    }

    public static int RealGetVec(int vec) {
        return mem_readd(vec << 2);
    }

    private static final int PAGES_IN_BLOCK = ((1024 * 1024) / Paging.MEM_PAGE_SIZE);

    private static final int SAFE_MEMORY = 32;

    private static final int MAX_MEMORY = 64;

    private static final int MAX_PAGE_ENTRIES = (MAX_MEMORY * 1024 * 1024 / 4096);

    private static final int LFB_PAGES = 512;

    private static final int MAX_LINKS = ((MAX_MEMORY * 1024 / 4) + 4096);

    private static class LinkBlock {

        public int used;

        public long[] pages = new long[MAX_LINKS];
    }

    private static class MemoryBlock {

        public int pages;

        Paging.PageHandler[] phandlers;

        int[] mhandles;

        LinkBlock links = new LinkBlock();

        public static class Lfb {

            int start_page;

            int end_page;

            int pages;

            Paging.PageHandler handler;

            Paging.PageHandler mmiohandler;
        }

        public Lfb lfb = new Lfb();

        public static class A20 {

            boolean enabled;

            short controlport;
        }

        A20 a20 = new A20();
    }

    private static MemoryBlock memory = new MemoryBlock();

    static Ptr MemBase;

    private static class IllegalPageHandler extends Paging.PageHandler {

        public IllegalPageHandler() {
            flags = Paging.PFLAG_INIT | Paging.PFLAG_NOCODE;
        }

        static int r_lcount = 0;

        public int readb(int addr) {
            if (Config.C_DEBUG) Log.log_msg(StringHelper.sprintf("Illegal read from %x, CS:IP %8x:%8x", new Object[] { new Integer(addr), new Integer(CPU.Segs_CSval), new Integer(CPU_Regs.reg_eip) })); else {
                if (r_lcount < 1000) {
                    r_lcount++;
                    Log.log_msg(StringHelper.sprintf("Illegal read from %x, CS:IP %8x:%8x", new Object[] { new Integer(addr), new Integer(CPU.Segs_CSval), new Integer(CPU_Regs.reg_eip) }));
                }
            }
            return 0;
        }

        static int w_lcount = 0;

        public void writeb(int addr, int val) {
            if (Config.C_DEBUG) Log.log_msg(StringHelper.sprintf("Illegal write to %x, CS:IP %8x:%8x", new Object[] { new Integer(addr), new Integer(CPU.Segs_CSval), new Integer(CPU_Regs.reg_eip) })); else {
                if (w_lcount < 1000) {
                    w_lcount++;
                    Log.log_msg(StringHelper.sprintf("Illegal write to %x, CS:IP %8x:%8x", new Object[] { new Integer(addr), new Integer(CPU.Segs_CSval), new Integer(CPU_Regs.reg_eip) }));
                }
            }
        }
    }

    private static class RAMPageHandler extends Paging.PageHandler {

        public RAMPageHandler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
        }

        public int GetHostReadPt(int phys_page) {
            return phys_page * MEM_PAGESIZE;
        }

        public int GetHostWritePt(int phys_page) {
            return phys_page * MEM_PAGESIZE;
        }
    }

    private static class ROMPageHandler extends RAMPageHandler {

        public ROMPageHandler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_HASROM;
        }

        public void writeb(int addr, int val) {
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR, "Write " + Integer.toString(val, 16) + " to rom at " + Integer.toString(addr, 16));
        }

        public void writew(int addr, int val) {
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR, "Write " + Integer.toString(val, 16) + " to rom at " + Integer.toString(addr, 16));
        }

        public void writed(int addr, int val) {
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_CPU, LogSeverities.LOG_ERROR, "Write " + Integer.toString(val, 16) + " to rom at " + Integer.toString(addr, 16));
        }
    }

    private static IllegalPageHandler illegal_page_handler = new IllegalPageHandler();

    private static RAMPageHandler ram_page_handler = new RAMPageHandler();

    private static ROMPageHandler rom_page_handler = new ROMPageHandler();

    public static void MEM_SetLFB(int page, int pages, Paging.PageHandler handler, Paging.PageHandler mmiohandler) {
        memory.lfb.handler = handler;
        memory.lfb.mmiohandler = mmiohandler;
        memory.lfb.start_page = page;
        memory.lfb.end_page = page + pages;
        memory.lfb.pages = pages;
        Paging.PAGING_ClearTLB();
    }

    public static Paging.PageHandler MEM_GetPageHandler(int phys_page) {
        if (phys_page < memory.pages) {
            return memory.phandlers[phys_page];
        } else if ((phys_page >= memory.lfb.start_page) && (phys_page < memory.lfb.end_page)) {
            return memory.lfb.handler;
        } else if ((phys_page >= memory.lfb.start_page + 0x01000000 / 4096) && (phys_page < memory.lfb.start_page + 0x01000000 / 4096 + 16)) {
            return memory.lfb.mmiohandler;
        }
        return illegal_page_handler;
    }

    public static void MEM_SetPageHandler(int phys_page, int pages, Paging.PageHandler handler) {
        for (; pages > 0; pages--) {
            memory.phandlers[phys_page] = handler;
            phys_page++;
        }
    }

    public static void MEM_ResetPageHandler(int phys_page, int pages) {
        for (; pages > 0; pages--) {
            memory.phandlers[phys_page] = ram_page_handler;
            phys_page++;
        }
    }

    public static int mem_strlen(int pt) {
        int x = 0;
        while (x < 1024) {
            if (Paging.mem_readb_inline(pt + x) == 0) return x;
            x++;
        }
        return 0;
    }

    private static void mem_strcpy(int dest, int src) {
        short r;
        while ((r = mem_readb(src++)) != 0) Paging.mem_writeb_inline(dest++, r);
        Paging.mem_writeb_inline(dest, (short) 0);
    }

    public static void mem_memmove(int dest, int src, int size) {
        while (size-- != 0) Paging.mem_writeb_inline(dest++, Paging.mem_readb_inline(src++));
    }

    public static void mem_memcpy(int dest, int src, int size) {
        while (size-- != 0) Paging.mem_writeb_inline(dest++, Paging.mem_readb_inline(src++));
    }

    public static void mem_memcpy(byte[] dest, int destOffset, int src, int size) {
        while (size-- != 0) dest[destOffset++] = (byte) Paging.mem_readb_inline(src++);
    }

    public static void mem_memcpy(int dest, byte[] src, int srcOffset, int size) {
        while (size-- != 0) Paging.mem_writeb_inline(dest++, src[srcOffset++]);
    }

    public static void mem_zero(int dest, int len) {
        while (len-- != 0) Paging.mem_writeb_inline(dest++, (short) 0);
    }

    public static void phys_zero(int dest, int len) {
        while (len-- != 0) host_writeb(dest++, (short) 0);
    }

    public static void phys_memcpy(int dest, byte[] buffer, int offset, int len) {
        while (len-- != 0) host_writeb(dest++, (short) buffer[offset++]);
    }

    public static void MEM_BlockRead(int pt, short[] data, int offset, int size) {
        for (int i = 0; i < size; i++) {
            short v1 = Paging.mem_readb_inline(pt++);
            short v2 = Paging.mem_readb_inline(pt++);
            data[i + offset] = (short) ((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }

    public static void MEM_BlockRead16u(int pt, int[] data, int offset, int size) {
        for (int i = 0; i < size; i++) {
            short v1 = Paging.mem_readb_inline(pt++);
            short v2 = Paging.mem_readb_inline(pt++);
            data[i + offset] = ((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }

    public static void MEM_BlockRead(int pt, short[] data, int size) {
        for (int i = 0; i < size; i++) {
            short v1 = Paging.mem_readb_inline(pt++);
            short v2 = Paging.mem_readb_inline(pt++);
            data[i] = (short) ((v1 & 0xFF) | ((v2 & 0xFF) << 16));
        }
    }

    public static String MEM_BlockRead(int pt, int size) {
        byte[] b = new byte[size];
        MEM_BlockRead(pt, b, size);
        return new String(b, 0, StringHelper.strlen(b));
    }

    public static void MEM_BlockRead(int pt, byte[] data, int size) {
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (Paging.mem_readb_inline(pt++) & 0xFF);
        }
    }

    public static void MEM_BlockRead(int pt, byte[] data, int offset, int size) {
        for (int i = 0; i < size; i++) {
            data[i + offset] = (byte) (Paging.mem_readb_inline(pt++) & 0xFF);
        }
    }

    public static void MEM_BlockWrite(int pt, byte[] read, int size) {
        int i;
        for (i = 0; i < size && i < read.length; i++) {
            Paging.mem_writeb_inline(pt++, read[i]);
        }
        for (; i < size; i++) {
            Paging.mem_writeb_inline(pt++, (byte) 0);
        }
    }

    public static void MEM_BlockWrite(int pt, byte[] read, int offset, int size) {
        int i;
        for (i = 0; i < size && i < read.length; i++) {
            Paging.mem_writeb_inline(pt++, read[i + offset]);
        }
        for (; i < size; i++) {
            Paging.mem_writeb_inline(pt++, (byte) 0);
        }
    }

    public static void MEM_BlockWrite(int pt, String data, int size) {
        byte[] read = data.getBytes();
        MEM_BlockWrite(pt, read, size);
    }

    public static void MEM_BlockCopy(int dest, int src, int size) {
        mem_memcpy(dest, src, size);
    }

    public static String MEM_StrCopy(int pt, int size) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < size; i++) {
            short r = Paging.mem_readb_inline(pt++);
            if (r == 0) break;
            buf.append((char) r);
        }
        return buf.toString();
    }

    public static int MEM_TotalPages() {
        return memory.pages;
    }

    public static int MEM_ExtraPages() {
        return direct.length * 4 / 4096 - memory.pages;
    }

    public static int MEM_FreeLargest() {
        int size = 0;
        int largest = 0;
        int index = Paging.XMS_START;
        while (index < memory.pages) {
            if (memory.mhandles[index] == 0) {
                size++;
            } else {
                if (size > largest) largest = size;
                size = 0;
            }
            index++;
        }
        if (size > largest) largest = size;
        return largest;
    }

    public static int MEM_FreeTotal() {
        int free = 0;
        int index = Paging.XMS_START;
        while (index < memory.pages) {
            if (memory.mhandles[index] == 0) free++;
            index++;
        }
        return free;
    }

    private static int MEM_AllocatedPages(int handle) {
        int pages = 0;
        while (handle > 0) {
            pages++;
            handle = memory.mhandles[handle];
        }
        return pages;
    }

    private static int BestMatch(int size) {
        int index = Paging.XMS_START;
        int first = 0;
        int best = 0xfffffff;
        int best_first = 0;
        while (index < memory.pages) {
            if (first == 0) {
                if (memory.mhandles[index] == 0) {
                    first = index;
                }
            } else {
                if (memory.mhandles[index] != 0) {
                    int pages = index - first;
                    if (pages == size) {
                        return first;
                    } else if (pages > size) {
                        if (pages < best) {
                            best = pages;
                            best_first = first;
                        }
                    }
                    first = 0;
                }
            }
            index++;
        }
        if (first != 0 && (index - first >= size) && (index - first < best)) {
            return first;
        }
        return best_first;
    }

    public static int MEM_AllocatePages(int pages, boolean sequence) {
        int ret = -1;
        if (pages == 0) return 0;
        if (sequence) {
            int index = BestMatch(pages);
            if (index == 0) return 0;
            while (pages != 0) {
                if (ret == -1) ret = index; else memory.mhandles[index - 1] = index;
                index++;
                pages--;
            }
            memory.mhandles[index - 1] = -1;
        } else {
            if (MEM_FreeTotal() < pages) return 0;
            int lastIndex = -1;
            while (pages != 0) {
                int index = BestMatch(1);
                if (index == 0) Log.exit("MEM:corruption during allocate");
                while (pages != 0 && (memory.mhandles[index] == 0)) {
                    if (ret == -1) ret = index; else memory.mhandles[lastIndex] = index;
                    lastIndex = index;
                    index++;
                    pages--;
                }
                memory.mhandles[lastIndex] = -1;
            }
        }
        return ret;
    }

    public static int MEM_GetNextFreePage() {
        return BestMatch(1);
    }

    public static void MEM_ReleasePages(int handle) {
        while (handle > 0) {
            int next = memory.mhandles[handle];
            memory.mhandles[handle] = 0;
            handle = next;
        }
    }

    public static boolean MEM_ReAllocatePages(IntRef handle, int pages, boolean sequence) {
        if (handle.value <= 0) {
            if (pages == 0) return true;
            handle.value = MEM_AllocatePages(pages, sequence);
            return (handle.value > 0);
        }
        if (pages == 0) {
            MEM_ReleasePages(handle.value);
            handle.value = -1;
            return true;
        }
        int index = handle.value;
        int last = 0;
        int old_pages = 0;
        while (index > 0) {
            old_pages++;
            last = index;
            index = memory.mhandles[index];
        }
        if (old_pages == pages) return true;
        if (old_pages > pages) {
            pages--;
            index = handle.value;
            old_pages--;
            while (pages != 0) {
                index = memory.mhandles[index];
                pages--;
                old_pages--;
            }
            int next = memory.mhandles[index];
            memory.mhandles[index] = -1;
            index = next;
            while (old_pages != 0) {
                next = memory.mhandles[index];
                memory.mhandles[index] = 0;
                index = next;
                old_pages--;
            }
            return true;
        } else {
            int need = pages - old_pages;
            if (sequence) {
                index = last + 1;
                int free = 0;
                while (index < memory.pages && memory.mhandles[index] == 0) {
                    index++;
                    free++;
                }
                if (free >= need) {
                    index = last;
                    while (need != 0) {
                        memory.mhandles[index] = index + 1;
                        need--;
                        index++;
                    }
                    memory.mhandles[index] = -1;
                    return true;
                } else {
                    int newhandle = MEM_AllocatePages(pages, true);
                    if (newhandle == 0) return false;
                    MEM_BlockCopy(newhandle * 4096, handle.value * 4096, old_pages * 4096);
                    MEM_ReleasePages(handle.value);
                    handle.value = newhandle;
                    return true;
                }
            } else {
                int rem = MEM_AllocatePages(need, false);
                if (rem == 0) return false;
                memory.mhandles[last] = rem;
                return true;
            }
        }
    }

    public static int MEM_NextHandle(int handle) {
        return memory.mhandles[handle];
    }

    public static int MEM_NextHandleAt(int handle, int where) {
        while (where != 0) {
            where--;
            handle = memory.mhandles[handle];
        }
        return handle;
    }

    public static boolean MEM_A20_Enabled() {
        return memory.a20.enabled;
    }

    public static void MEM_A20_Enable(boolean enabled) {
        int phys_base = enabled ? (1024 / 4) : 0;
        for (int i = 0; i < 16; i++) Paging.PAGING_MapPage((1024 / 4) + i, phys_base + i);
        memory.a20.enabled = enabled;
    }

    public static int mem_unalignedreadw(int address) {
        return Paging.mem_readb_inline(address) | Paging.mem_readb_inline(address + 1) << 8;
    }

    public static int mem_unalignedreadd(int address) {
        return Paging.mem_readb_inline(address) | (Paging.mem_readb_inline(address + 1) << 8) | (Paging.mem_readb_inline(address + 2) << 16) | (Paging.mem_readb_inline(address + 3) << 24);
    }

    public static void mem_unalignedwritew(int address, int val) {
        Paging.mem_writeb_inline(address, (short) (val & 0xFF));
        val >>= 8;
        Paging.mem_writeb_inline(address + 1, (short) (val & 0xFF));
    }

    public static void mem_unalignedwrited(int address, int val) {
        Paging.mem_writeb_inline(address, (short) (val & 0xFF));
        val >>= 8;
        Paging.mem_writeb_inline(address + 1, (short) (val & 0xFF));
        val >>= 8;
        Paging.mem_writeb_inline(address + 2, (short) (val & 0xFF));
        val >>= 8;
        Paging.mem_writeb_inline(address + 3, (short) (val & 0xFF));
    }

    public static short mem_readb(int address) {
        return Paging.mem_readb_inline(address);
    }

    public static int mem_readw(int address) {
        return Paging.mem_readw_inline(address);
    }

    public static int mem_readd(int address) {
        return Paging.mem_readd_inline(address);
    }

    public static void mem_writeb(int address, int val) {
        Paging.mem_writeb_inline(address, (short) val);
    }

    public static void mem_writew(int address, int val) {
        Paging.mem_writew_inline(address, val);
    }

    public static void mem_writed(int address, int val) {
        Paging.mem_writed_inline(address, val);
    }

    private static IoHandler.IO_WriteHandler write_p92 = new IoHandler.IO_WriteHandler() {

        public void call(int port, int val, int iolen) {
            if ((val & 1) != 0) Log.exit("XMS: CPU reset via port 0x92 not supported.");
            memory.a20.controlport = (short) (val & ~2);
            MEM_A20_Enable((val & 2) > 0);
        }
    };

    private static IoHandler.IO_ReadHandler read_p92 = new IoHandler.IO_ReadHandler() {

        public int call(int port, int iolen) {
            return memory.a20.controlport | (memory.a20.enabled ? 0x02 : 0);
        }
    };

    public static void RemoveEMSPageFrame() {
        for (int ct = 0xe0; ct < 0xf0; ct++) {
            memory.phandlers[ct] = rom_page_handler;
        }
    }

    public static void PreparePCJRCartRom() {
        for (int ct = 0xd0; ct < 0xe0; ct++) {
            memory.phandlers[ct] = rom_page_handler;
        }
    }

    private IoHandler.IO_ReadHandleObject ReadHandler = new IoHandler.IO_ReadHandleObject();

    private IoHandler.IO_WriteHandleObject WriteHandler = new IoHandler.IO_WriteHandleObject();

    public Memory(Section configuration) {
        super(configuration);
        int i;
        Section_prop section = (Section_prop) configuration;
        int memsize = section.Get_int("memsize");
        if (memsize < 1) memsize = 1;
        if (memsize > MAX_MEMORY - 1) {
            Log.log_msg("Maximum memory size is " + (MAX_MEMORY - 1) + " MB");
            memsize = MAX_MEMORY - 1;
        }
        if (memsize > SAFE_MEMORY - 1) {
            Log.log_msg("Memory sizes above " + (SAFE_MEMORY - 1) + " MB are NOT recommended.");
            Log.log_msg("Stick with the default values unless you are absolutely certain.");
        }
        MEM_SIZE = memsize;
        try {
            Runtime.getRuntime().gc();
            highwaterMark = memsize * 1024 * 1024;
            int videosize = section.Get_int("vmemsize");
            if (videosize == 0) videosize = 2;
            videosize *= 3 * 1024 * 1024;
            System.out.println("About to allocate memory " + String.valueOf((highwaterMark + EXTRA_MEM + VGA_draw.TEMPLINE_SIZE + videosize) / 1024) + "kb: " + String.valueOf(Runtime.getRuntime().freeMemory() / 1024) + "kb free");
            direct = new int[(highwaterMark + EXTRA_MEM + videosize + VGA_draw.TEMPLINE_SIZE + 3) >> 2];
        } catch (java.lang.OutOfMemoryError e) {
            Log.exit("Can't allocate main memory of " + memsize + " MB");
        }
        memory.pages = (memsize * 1024 * 1024) / 4096;
        memory.phandlers = new Paging.PageHandler[memory.pages];
        memory.mhandles = new int[memory.pages];
        for (i = 0; i < memory.pages; i++) {
            memory.phandlers[i] = ram_page_handler;
            memory.mhandles[i] = 0;
        }
        for (i = 0xc0; i < 0xc8; i++) {
            memory.phandlers[i] = rom_page_handler;
        }
        for (i = 0xf0; i < 0x100; i++) {
            memory.phandlers[i] = rom_page_handler;
        }
        if (Dosbox.machine == MachineType.MCH_PCJR) {
            for (i = 0xe0; i < 0xf0; i++) {
                memory.phandlers[i] = rom_page_handler;
            }
        }
        memory.links.used = 0;
        WriteHandler.Install(0x92, write_p92, IoHandler.IO_MB);
        ReadHandler.Install(0x92, read_p92, IoHandler.IO_MB);
        MEM_A20_Enable(false);
    }

    public static void clear() {
        for (int i = 0; i < memory.pages; i++) {
            memory.phandlers[i] = ram_page_handler;
            memory.mhandles[i] = 0;
        }
        memory.links.used = 0;
    }

    static Memory test;

    public static Section.SectionFunction MEM_ShutDown = new Section.SectionFunction() {

        public void call(Section section) {
            MemBase = null;
            host_memory = null;
            direct = null;
            test = null;
        }
    };

    public static Section.SectionFunction MEM_Init = new Section.SectionFunction() {

        public void call(Section section) {
            test = new Memory(section);
            section.AddDestroyFunction(MEM_ShutDown);
        }
    };
}
