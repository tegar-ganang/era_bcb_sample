package jdos.cpu.core_dynrec;

import jdos.cpu.Paging;
import jdos.cpu.Core_dynrec;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.hardware.Memory;
import jdos.util.Ptr;

public final class CodePageHandlerDynRec extends Paging.PageHandler {

    public CodePageHandlerDynRec() {
        invalidation_map = null;
    }

    public void SetupAt(int _phys_page, Paging.PageHandler _old_pagehandler) {
        phys_page = _phys_page;
        old_pagehandler = _old_pagehandler;
        flags = old_pagehandler.flags | Paging.PFLAG_HASCODE;
        flags &= ~Paging.PFLAG_WRITEABLE;
        active_blocks = 0;
        active_count = 16;
        invalidation_map = null;
    }

    boolean InvalidateRange(int start, int end) {
        int index = 1 + (end >> Core_dynrec.DYN_HASH_SHIFT);
        boolean is_current_block = false;
        long ip_point = CPU.Segs_CSphys + CPU_Regs.reg_eip;
        ip_point = (Paging.PAGING_GetPhysicalPage(ip_point) - (phys_page << 12)) + (ip_point & 0xfff);
        while (index >= 0) {
            int map = 0;
            for (int count = start; count <= end; count++) map += write_map.p[count];
            if (map == 0) return is_current_block;
            CacheBlockDynRec block = hash_map[index];
            while (block != null) {
                CacheBlockDynRec nextblock = block.hash.next;
                if (start <= block.page.end && end >= block.page.start) {
                    if (ip_point <= block.page.end && ip_point >= block.page.start) is_current_block = true;
                    block.Clear();
                }
                block = nextblock;
            }
            index--;
        }
        return is_current_block;
    }

    public void writeb(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readb(hostmem + addr) == (val & 0xFF)) return;
        Memory.host_writeb(hostmem + addr, (short) val);
        if (write_map.readb(addr) == 0) {
            if (active_blocks != 0) return;
            active_count--;
            if (active_count == 0) Release();
            return;
        } else if (invalidation_map == null) {
            invalidation_map = new Ptr(4096);
        }
        invalidation_map.p[addr]++;
        InvalidateRange(addr, addr);
    }

    public void writew(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readw(hostmem + addr) == (val & 0xFFFF)) return;
        Memory.host_writew(hostmem + addr, val);
        if (write_map.readw(addr) == 0) {
            if (active_blocks != 0) return;
            active_count--;
            if (active_count == 0) Release();
            return;
        } else if (invalidation_map == null) {
            invalidation_map = new Ptr(4096);
        }
        invalidation_map.writew(addr, invalidation_map.readw(addr) + 0x101);
        InvalidateRange(addr, addr + 1);
    }

    public void writed(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readd(hostmem + addr) == (val & 0xFFFFFFFFl)) return;
        Memory.host_writed(hostmem + addr, val);
        if (write_map.readd(addr) == 0) {
            if (active_blocks != 0) return;
            active_count--;
            if (active_count == 0) Release();
            return;
        } else if (invalidation_map == null) {
            invalidation_map = new Ptr(4096);
        }
        invalidation_map.writed(addr, invalidation_map.readd(addr) + 0x1010101);
        InvalidateRange(addr, addr + 3);
    }

    public boolean writeb_checked(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readb(hostmem + addr) == (val & 0xFF)) return false;
        if (write_map.readb(addr) == 0) {
            if (active_blocks == 0) {
                active_count--;
                if (active_count == 0) Release();
            }
        } else {
            if (invalidation_map == null) {
                invalidation_map = new Ptr(4096);
            }
            invalidation_map.p[addr]++;
            if (InvalidateRange(addr, addr)) {
                CPU.cpu.exception.which = Core_dynrec.SMC_CURRENT_BLOCK;
                return true;
            }
        }
        Memory.host_writeb(hostmem + addr, (short) val);
        return false;
    }

    public boolean writew_checked(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readw(hostmem + addr) == (val & 0xFFFF)) return false;
        if (write_map.readw(addr) == 0) {
            if (active_blocks == 0) {
                active_count--;
                if (active_count == 0) Release();
            }
        } else {
            if (invalidation_map == null) {
                invalidation_map = new Ptr(4096);
            }
            invalidation_map.writew(addr, invalidation_map.readw(addr) + 0x101);
            if (InvalidateRange(addr, addr + 1)) {
                CPU.cpu.exception.which = Core_dynrec.SMC_CURRENT_BLOCK;
                return true;
            }
        }
        Memory.host_writew(hostmem + addr, val);
        return false;
    }

    public boolean writed_checked(long address, int val) {
        int addr = (int) (address & 4095);
        if (Memory.host_readd(hostmem + addr) == (val & 0xFFFFFFFFl)) return false;
        if (write_map.readd(addr) == 0) {
            if (active_blocks == 0) {
                active_count--;
                if (active_count == 0) Release();
            }
        } else {
            if (invalidation_map == null) {
                invalidation_map = new Ptr(4096);
            }
            invalidation_map.writed(addr, invalidation_map.readd(addr) + 0x1010101);
            if (InvalidateRange(addr, addr + 3)) {
                CPU.cpu.exception.which = Core_dynrec.SMC_CURRENT_BLOCK;
                return true;
            }
        }
        Memory.host_writed(hostmem + addr, val);
        return false;
    }

    void AddCacheBlock(CacheBlockDynRec block) {
        int index = 1 + (block.page.start >> Core_dynrec.DYN_HASH_SHIFT);
        block.hash.next = hash_map[index];
        block.hash.index = index;
        hash_map[index] = block;
        block.page.handler = this;
        active_blocks++;
    }

    void AddCrossBlock(CacheBlockDynRec block) {
        block.hash.next = hash_map[0];
        block.hash.index = 0;
        hash_map[0] = block;
        block.page.handler = this;
        active_blocks++;
    }

    void DelCacheBlock(CacheBlockDynRec block) {
        active_blocks--;
        active_count = 16;
        if (hash_map[block.hash.index] == block) {
            hash_map[block.hash.index] = block.hash.next;
        } else {
            CacheBlockDynRec parent = hash_map[block.hash.index];
            CacheBlockDynRec bwhere = parent.hash.next;
            while (bwhere != block) {
                parent = bwhere;
                bwhere = parent.hash.next;
            }
            parent.hash.next = block.hash.next;
        }
        if (block.cache.wmapmask != null) {
            for (int i = block.page.start; i < block.cache.maskstart; i++) {
                if (write_map.p[i] != 0) write_map.p[i]--;
            }
            int maskct = 0;
            for (int i = block.cache.maskstart; i <= block.page.end; i++, maskct++) {
                if (write_map.p[i] != 0) {
                    if ((maskct >= block.cache.masklen) || (block.cache.wmapmask[maskct] == 0)) write_map.p[i]--;
                }
            }
            block.cache.wmapmask = null;
        } else {
            for (int i = block.page.start; i <= block.page.end; i++) {
                if (write_map.p[i] != 0) write_map.p[i]--;
            }
        }
    }

    public void Release() {
        Memory.MEM_SetPageHandler(phys_page, 1, old_pagehandler);
        Paging.PAGING_ClearTLB();
        if (prev != null) prev.next = next; else Cache.cache.used_pages = next;
        if (next != null) next.prev = prev; else Cache.cache.last_page = prev;
        next = Cache.cache.free_pages;
        Cache.cache.free_pages = this;
        prev = null;
    }

    public void ClearRelease() {
        for (int index = 0; index < (1 + Core_dynrec.DYN_PAGE_HASH); index++) {
            CacheBlockDynRec block = hash_map[index];
            while (block != null) {
                CacheBlockDynRec nextblock = block.hash.next;
                block.page.handler = null;
                block.Clear();
                block = nextblock;
            }
        }
        Release();
    }

    public CacheBlockDynRec FindCacheBlock(int start) {
        CacheBlockDynRec block = hash_map[1 + (start >> Core_dynrec.DYN_HASH_SHIFT)];
        while (block != null) {
            if (block.page.start == start) return block;
            block = block.hash.next;
        }
        return null;
    }

    public long GetHostReadPt(int phys_page) {
        hostmem = (int) old_pagehandler.GetHostReadPt(phys_page);
        return hostmem;
    }

    public long GetHostWritePt(int phys_page) {
        return GetHostReadPt(phys_page);
    }

    public Ptr write_map = new Ptr(4096);

    public Ptr invalidation_map;

    CodePageHandlerDynRec next, prev;

    private Paging.PageHandler old_pagehandler;

    private CacheBlockDynRec[] hash_map = new CacheBlockDynRec[1 + Core_dynrec.DYN_PAGE_HASH];

    private int active_blocks;

    private int active_count;

    private int hostmem;

    private int phys_page;
}
