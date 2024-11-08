package jdos.hardware;

import jdos.Dosbox;
import jdos.cpu.Paging;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.types.SVGACards;
import jdos.util.Ptr;

public class VGA_memory {

    public static boolean VGA_KEEP_CHANGES = false;

    private static long RasterOp(long input, int mask) {
        switch(VGA.vga.config.raster_op) {
            case 0x00:
                return (input & mask) | (VGA.vga.latch.d & ~mask);
            case 0x01:
                return (input | ~mask) & VGA.vga.latch.d;
            case 0x02:
                return (input & mask) | VGA.vga.latch.d;
            case 0x03:
                return (input & mask) ^ VGA.vga.latch.d;
        }
        return 0;
    }

    private static long ModeOperation(int val) {
        long full;
        switch(VGA.vga.config.write_mode) {
            case 0x00:
                val = ((val >> VGA.vga.config.data_rotate) | (val << (8 - VGA.vga.config.data_rotate))) & 0xFF;
                full = VGA.ExpandTable[val];
                full = (full & VGA.vga.config.full_not_enable_set_reset) | VGA.vga.config.full_enable_and_set_reset;
                full = RasterOp(full, VGA.vga.config.full_bit_mask);
                break;
            case 0x01:
                full = VGA.vga.latch.d;
                break;
            case 0x02:
                full = RasterOp(VGA.FillTable[val & 0xF], VGA.vga.config.full_bit_mask);
                break;
            case 0x03:
                val = ((val >> VGA.vga.config.data_rotate) | (val << (8 - VGA.vga.config.data_rotate))) & 0xFF;
                full = RasterOp(VGA.vga.config.full_set_reset, VGA.ExpandTable[val] & VGA.vga.config.full_bit_mask);
                break;
            default:
                if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:Unsupported write mode " + VGA.vga.config.write_mode);
                full = 0;
                break;
        }
        return full;
    }

    private static final int VGA_PAGES = (128 / 4);

    private static final int VGA_PAGE_A0 = (0xA0000 / 4096);

    private static final int VGA_PAGE_B0 = (0xB0000 / 4096);

    private static final int VGA_PAGE_B8 = (0xB8000 / 4096);

    private static class VGAPages {

        int base, mask;
    }

    private static VGAPages vgapages = new VGAPages();

    private static class VGA_UnchainedRead_Handler extends Paging.PageHandler {

        public int readHandler(int start) {
            VGA.vga.latch.d = Memory.host_readd(VGA.vga.mem.linear + start * 4);
            switch(VGA.vga.config.read_mode) {
                case 0:
                    return (VGA.vga.latch.b(VGA.vga.config.read_map_select));
                case 1:
                    VGA.VGA_Latch templatch = new VGA.VGA_Latch();
                    templatch.d = (VGA.vga.latch.d & VGA.FillTable[VGA.vga.config.color_dont_care]) ^ VGA.FillTable[VGA.vga.config.color_compare & VGA.vga.config.color_dont_care];
                    return (short) ~(templatch.b(0) | templatch.b(1) | templatch.b(2) | templatch.b(3));
            }
            return 0;
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return readHandler(addr);
        }

        public int readw(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8);
        }

        public int readd(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8) | (readHandler(addr + 2) << 16) | (readHandler(addr + 3) << 24);
        }
    }

    private static class VGA_ChainedEGA_Handler extends Paging.PageHandler {

        public int readHandler(int addr) {
            return Memory.host_readb(VGA.vga.mem.linear + addr);
        }

        public void writeHandler(int s, int val) {
            int start = (int) s;
            ModeOperation(val);
            VGA.VGA_Latch pixels = new VGA.VGA_Latch();
            Memory.host_writeb(VGA.vga.mem.linear + start, (short) val);
            start >>= 2;
            pixels.d = Memory.host_readd(VGA.vga.mem.linear + start * 4);
            int colors0_3, colors4_7;
            VGA.VGA_Latch temp = new VGA.VGA_Latch();
            temp.d = (pixels.d >> 4) & 0x0f0f0f0f;
            colors0_3 = VGA.Expand16Table[0][temp.b(0)] | VGA.Expand16Table[1][temp.b(1)] | VGA.Expand16Table[2][temp.b(2)] | VGA.Expand16Table[3][temp.b(3)];
            Memory.host_writed(VGA.vga.fastmem + start << 3, colors0_3);
            temp.d = pixels.d & 0x0f0f0f0f;
            colors4_7 = VGA.Expand16Table[0][temp.b(0)] | VGA.Expand16Table[1][temp.b(1)] | VGA.Expand16Table[2][temp.b(2)] | VGA.Expand16Table[3][temp.b(3)];
            Memory.host_writed(VGA.vga.fastmem + (start << 3) + 4, colors4_7);
        }

        public VGA_ChainedEGA_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
        }

        public void writew(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
        }

        public void writed(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
            writeHandler(addr + 2, (short) (val >> 16));
            writeHandler(addr + 3, (short) (val >> 24));
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return readHandler(addr);
        }

        public int readw(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8);
        }

        public int readd(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8) | (readHandler(addr + 2) << 16) | (readHandler(addr + 3) << 24);
        }
    }

    private static class VGA_UnchainedEGA_Handler extends VGA_UnchainedRead_Handler {

        public void writeHandler(int start, short val) {
            long data = ModeOperation(val);
            VGA.VGA_Latch pixels = new VGA.VGA_Latch();
            pixels.d = Memory.host_readd(VGA.vga.mem.linear + start * 4);
            pixels.d &= VGA.vga.config.full_not_map_mask;
            pixels.d |= (data & VGA.vga.config.full_map_mask);
            Memory.host_writed(VGA.vga.mem.linear + start * 4, pixels.d);
            int colors0_3, colors4_7;
            VGA.VGA_Latch temp = new VGA.VGA_Latch();
            temp.d = (pixels.d >> 4) & 0x0f0f0f0f;
            colors0_3 = VGA.Expand16Table[0][temp.b(0)] | VGA.Expand16Table[1][temp.b(1)] | VGA.Expand16Table[2][temp.b(2)] | VGA.Expand16Table[3][temp.b(3)];
            Memory.host_writed(VGA.vga.fastmem + (start << 3), colors0_3);
            temp.d = pixels.d & 0x0f0f0f0f;
            colors4_7 = VGA.Expand16Table[0][temp.b(0)] | VGA.Expand16Table[1][temp.b(1)] | VGA.Expand16Table[2][temp.b(2)] | VGA.Expand16Table[3][temp.b(3)];
            Memory.host_writed(VGA.vga.fastmem + (start << 3) + 4, colors4_7);
        }

        public VGA_UnchainedEGA_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
        }

        public void writew(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
        }

        public void writed(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
            writeHandler(addr + 2, (short) (val >> 16));
            writeHandler(addr + 3, (short) (val >> 24));
        }
    }

    private static class VGA_ChainedVGA_Handler extends Paging.PageHandler {

        VGA_ChainedVGA_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return Memory.host_readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }

        public int readw(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            if ((addr & 1) != 0) {
                int a = Memory.host_readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
                int b = Memory.host_readb(VGA.vga.mem.linear + (((addr + 1) & ~3) << 2) + ((addr + 1) & 3));
                return a | (b << 8);
            }
            return Memory.host_readw(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }

        public int readd(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            if ((addr & 3) != 0) {
                int a = Memory.host_readb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
                int b = Memory.host_readb(VGA.vga.mem.linear + (((addr + 1) & ~3) << 2) + ((addr + 1) & 3));
                int c = Memory.host_readb(VGA.vga.mem.linear + (((addr + 2) & ~3) << 2) + ((addr + 2) & 3));
                int d = Memory.host_readb(VGA.vga.mem.linear + (((addr + 3) & ~3) << 2) + ((addr + 3) & 3));
                return a | (b << 8) | (c << 16) | (d << 24);
            }
            return Memory.host_readd(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3));
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            Memory.host_writeb(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3), (short) val);
            Memory.host_writeb(VGA.vga.fastmem + addr, (short) val);
            if (addr < 320) Memory.host_writeb(VGA.vga.fastmem + addr + 64 * 1024, (short) val);
        }

        public void writew(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            if ((addr & 1) != 0) {
                Memory.host_writebs(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3), (byte) val);
                Memory.host_writebs(VGA.vga.mem.linear + (((addr + 1) & ~3) << 2) + ((addr + 1) & 3), (byte) (val >> 8));
            } else {
                Memory.host_writew(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3), val);
            }
            Memory.host_writew(VGA.vga.fastmem + addr, val);
            if (addr < 320) Memory.host_writew(VGA.vga.fastmem + addr + 64 * 1024, val);
        }

        public void writed(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            if ((addr & 3) != 0) {
                Memory.host_writebs(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3), (byte) val);
                Memory.host_writebs(VGA.vga.mem.linear + (((addr + 1) & ~3) << 2) + ((addr + 1) & 3), (byte) (val >> 8));
                Memory.host_writebs(VGA.vga.mem.linear + (((addr + 2) & ~3) << 2) + ((addr + 2) & 3), (byte) (val >> 16));
                Memory.host_writebs(VGA.vga.mem.linear + (((addr + 3) & ~3) << 2) + ((addr + 3) & 3), (byte) (val >> 24));
            } else {
                Memory.host_writed(VGA.vga.mem.linear + ((addr & ~3) << 2) + (addr & 3), val);
            }
            Memory.host_writed(VGA.vga.fastmem + addr, val);
            if (addr < 320) Memory.host_writed(VGA.vga.fastmem + addr + 64 * 1024, val);
        }
    }

    private static class VGA_UnchainedVGA_Handler extends VGA_UnchainedRead_Handler {

        public void writeHandler(int addr, int val) {
            addr <<= 2;
            long data = ModeOperation(val);
            int d = Memory.host_readd(VGA.vga.mem.linear + addr);
            d &= VGA.vga.config.full_not_map_mask;
            d |= (data & VGA.vga.config.full_map_mask);
            Memory.host_writed(VGA.vga.mem.linear + addr, d);
        }

        public VGA_UnchainedVGA_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public void writeb(int addr, int val) {
            int a = (int) (addr & vgapages.mask);
            a += VGA.vga.svga.bank_write_full;
            writeHandler(a, val);
        }

        public void writew(int addr, int val) {
            int a = (int) (Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask);
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(a, val);
            writeHandler(a + 1, val >> 8);
        }

        public void writed(int addr, int val) {
            int a = (int) (Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask);
            addr += VGA.vga.svga.bank_write_full;
            writeHandler(a, val);
            writeHandler(a + 1, val >> 8);
            writeHandler(a + 2, val >> 16);
            writeHandler(a + 3, val >>> 24);
        }
    }

    private static class VGA_TEXT_PageHandler extends Paging.PageHandler {

        public VGA_TEXT_PageHandler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            return VGA.vga.draw.font[(int) addr];
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            if ((VGA.vga.seq.map_mask & 0x4) != 0) {
                VGA.vga.draw.font[(int) addr] = (byte) val;
            }
        }
    }

    private static class VGA_Map_Handler extends Paging.PageHandler {

        public VGA_Map_Handler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public int GetHostReadPt(int phys_page) {
            phys_page -= vgapages.base;
            return VGA.vga.mem.linear + ((VGA.vga.svga.bank_read_full + phys_page * 4096) & (VGA.vga.vmemwrap - 1));
        }

        public int GetHostWritePt(int phys_page) {
            phys_page -= vgapages.base;
            return VGA.vga.mem.linear + ((VGA.vga.svga.bank_write_full + phys_page * 4096) & (VGA.vga.vmemwrap - 1));
        }
    }

    private static class VGA_Changes_Handler extends Paging.PageHandler {

        public VGA_Changes_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return Memory.host_readb(VGA.vga.mem.linear + addr);
        }

        public int readw(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return Memory.host_readw(VGA.vga.mem.linear + addr);
        }

        public int readd(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_read_full;
            return Memory.host_readd(VGA.vga.mem.linear + addr);
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            Memory.host_writeb(VGA.vga.mem.linear + addr, (short) val);
        }

        public void writew(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            Memory.host_writew(VGA.vga.mem.linear + addr, val);
        }

        public void writed(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) & vgapages.mask;
            addr += VGA.vga.svga.bank_write_full;
            Memory.host_writed(VGA.vga.mem.linear + addr, val);
        }
    }

    private static class VGA_LIN4_Handler extends VGA_UnchainedEGA_Handler {

        public VGA_LIN4_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public void writeb(int addr, int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            writeHandler(addr + 0, (short) (val >> 0));
        }

        public void writew(int addr, int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
        }

        public void writed(int addr, int val) {
            addr = VGA.vga.svga.bank_write_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            writeHandler(addr + 0, (short) (val >> 0));
            writeHandler(addr + 1, (short) (val >> 8));
            writeHandler(addr + 2, (short) (val >> 16));
            writeHandler(addr + 3, (short) (val >> 24));
        }

        public int readb(int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            return readHandler(addr);
        }

        public int readw(int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8);
        }

        public int readd(int addr) {
            addr = VGA.vga.svga.bank_read_full + (Paging.PAGING_GetPhysicalAddress(addr) & 0xffff);
            addr &= (VGA.vga.vmemwrap >> 2) - 1;
            return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8) | (readHandler(addr + 2) << 16) | (readHandler(addr + 3) << 24);
        }
    }

    private static class VGA_LFBChanges_Handler extends Paging.PageHandler {

        public VGA_LFBChanges_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public int readb(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            return Memory.host_readb(VGA.vga.mem.linear + addr);
        }

        public int readw(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            return Memory.host_readw(VGA.vga.mem.linear + addr);
        }

        public int readd(int addr) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            return Memory.host_readd(VGA.vga.mem.linear + addr);
        }

        public void writeb(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            Memory.host_writeb(VGA.vga.mem.linear + addr, (short) val);
        }

        public void writew(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            Memory.host_writew(VGA.vga.mem.linear + addr, val);
        }

        public void writed(int addr, int val) {
            addr = Paging.PAGING_GetPhysicalAddress(addr) - VGA.vga.lfb.addr;
            Memory.host_writed(VGA.vga.mem.linear + addr, val);
        }
    }

    private static class VGA_LFB_Handler extends Paging.PageHandler {

        public VGA_LFB_Handler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
        }

        public int GetHostReadPt(int phys_page) {
            phys_page -= VGA.vga.lfb.page;
            return VGA.vga.mem.linear + ((phys_page * 4096) & (VGA.vga.vmemwrap - 1));
        }

        public int GetHostWritePt(int phys_page) {
            return GetHostReadPt(phys_page);
        }
    }

    private static class VGA_MMIO_Handler extends Paging.PageHandler {

        public VGA_MMIO_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public void writeb(int addr, int val) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 1);
        }

        public void writew(int addr, int val) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 2);
        }

        public void writed(int addr, int val) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            VGA_xga.XGA_Write.call(port, val, 4);
        }

        public int readb(int addr) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 1);
        }

        public int readw(int addr) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 2);
        }

        public int readd(int addr) {
            int port = (int) Paging.PAGING_GetPhysicalAddress(addr) & 0xffff;
            return VGA_xga.XGA_Read.call(port, 4);
        }
    }

    private static class VGA_TANDY_PageHandler extends Paging.PageHandler {

        public VGA_TANDY_PageHandler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
        }

        public int GetHostReadPt(int phys_page) {
            if ((VGA.vga.tandy.mem_bank & 1) != 0) phys_page &= 0x03; else phys_page &= 0x07;
            return VGA.vga.tandy.mem_base + (phys_page * 4096);
        }

        public int GetHostWritePt(int phys_page) {
            return GetHostReadPt(phys_page);
        }
    }

    private static class VGA_PCJR_Handler extends Paging.PageHandler {

        public VGA_PCJR_Handler() {
            flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
        }

        public int GetHostReadPt(int phys_page) {
            phys_page -= 0xb8;
            if ((VGA.vga.tandy.mem_bank & 1) != 0) phys_page &= 0x03;
            return VGA.vga.tandy.mem_base + (phys_page * 4096);
        }

        public int GetHostWritePt(int phys_page) {
            return GetHostReadPt(phys_page);
        }
    }

    private static class VGA_Empty_Handler extends Paging.PageHandler {

        public VGA_Empty_Handler() {
            flags = Paging.PFLAG_NOCODE;
        }

        public int readb(int addr) {
            return 0xff;
        }

        public void writeb(int addr, int val) {
        }
    }

    private static class vg {

        public VGA_Map_Handler map = new VGA_Map_Handler();

        public VGA_Changes_Handler changes = new VGA_Changes_Handler();

        public VGA_TEXT_PageHandler text = new VGA_TEXT_PageHandler();

        public VGA_TANDY_PageHandler tandy = new VGA_TANDY_PageHandler();

        public VGA_ChainedEGA_Handler cega = new VGA_ChainedEGA_Handler();

        public VGA_ChainedVGA_Handler cvga = new VGA_ChainedVGA_Handler();

        public VGA_UnchainedEGA_Handler uega = new VGA_UnchainedEGA_Handler();

        public VGA_UnchainedVGA_Handler uvga = new VGA_UnchainedVGA_Handler();

        public VGA_PCJR_Handler pcjr = new VGA_PCJR_Handler();

        public VGA_LIN4_Handler lin4 = new VGA_LIN4_Handler();

        public VGA_LFB_Handler lfb = new VGA_LFB_Handler();

        public VGA_LFBChanges_Handler lfbchanges = new VGA_LFBChanges_Handler();

        public VGA_MMIO_Handler mmio = new VGA_MMIO_Handler();

        public VGA_Empty_Handler empty = new VGA_Empty_Handler();
    }

    private static vg vgaph = new vg();

    private static void VGA_ChangedBank() {
        if (!VGA.VGA_LFB_MAPPED) {
            if (VGA.vga.mode >= VGA.M_LIN4 && VGA.vga.mode <= VGA.M_LIN32) {
                return;
            }
        }
        VGA_SetupHandlers();
    }

    private static void rangeDone() {
        Paging.PAGING_ClearTLB();
    }

    public static void VGA_SetupHandlers() {
        VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_read * VGA.vga.svga.bank_size;
        VGA.vga.svga.bank_write_full = VGA.vga.svga.bank_write * VGA.vga.svga.bank_size;
        Paging.PageHandler newHandler;
        switch(Dosbox.machine) {
            case MachineType.MCH_CGA:
            case MachineType.MCH_PCJR:
                Memory.MEM_SetPageHandler(VGA_PAGE_B8, 8, vgaph.pcjr);
                rangeDone();
                return;
            case MachineType.MCH_HERC:
                vgapages.base = VGA_PAGE_B0;
                if ((VGA.vga.herc.enable_bits & 0x2) != 0) {
                    vgapages.mask = 0xffff;
                    Memory.MEM_SetPageHandler(VGA_PAGE_B0, 16, vgaph.map);
                } else {
                    vgapages.mask = 0x7fff;
                    Memory.MEM_SetPageHandler(VGA_PAGE_B0, 8, vgaph.map);
                    Memory.MEM_SetPageHandler(VGA_PAGE_B8, 8, vgaph.empty);
                }
                rangeDone();
                return;
            case MachineType.MCH_TANDY:
                vgapages.base = VGA_PAGE_A0;
                vgapages.mask = 0x1ffff;
                Memory.MEM_SetPageHandler(VGA_PAGE_A0, 32, vgaph.map);
                if ((VGA.vga.tandy.extended_ram & 1) != 0) {
                    VGA.vga.tandy.draw_base = VGA.vga.mem.linear;
                    VGA.vga.tandy.mem_base = VGA.vga.mem.linear;
                } else {
                    VGA.vga.tandy.draw_base = 0x80000 + VGA.vga.tandy.draw_bank * 16 * 1024;
                    VGA.vga.tandy.mem_base = 0x80000 + VGA.vga.tandy.mem_bank * 16 * 1024;
                    Memory.MEM_SetPageHandler(0xb8, 8, vgaph.tandy);
                }
                rangeDone();
                return;
            case MachineType.MCH_EGA:
            case MachineType.MCH_VGA:
                break;
            default:
                Log.log_msg("Illegal machine type " + Dosbox.machine);
                return;
        }
        switch(VGA.vga.mode) {
            case VGA.M_ERROR:
            default:
                return;
            case VGA.M_LIN4:
                newHandler = vgaph.lin4;
                break;
            case VGA.M_LIN15:
            case VGA.M_LIN16:
            case VGA.M_LIN32:
                if (VGA.VGA_LFB_MAPPED) newHandler = vgaph.map; else newHandler = vgaph.changes;
                break;
            case VGA.M_LIN8:
            case VGA.M_VGA:
                if (VGA.vga.config.chained) {
                    if (VGA.vga.config.compatible_chain4) newHandler = vgaph.cvga; else if (VGA.VGA_LFB_MAPPED) newHandler = vgaph.map; else newHandler = vgaph.changes;
                } else {
                    newHandler = vgaph.uvga;
                }
                break;
            case VGA.M_EGA:
                if (VGA.vga.config.chained) newHandler = vgaph.cega; else newHandler = vgaph.uega;
                break;
            case VGA.M_TEXT:
                if ((VGA.vga.gfx.miscellaneous & 0x2) != 0) newHandler = vgaph.map; else newHandler = vgaph.text;
                break;
            case VGA.M_CGA4:
            case VGA.M_CGA2:
                newHandler = vgaph.map;
                break;
        }
        switch((VGA.vga.gfx.miscellaneous >> 2) & 3) {
            case 0:
                vgapages.base = VGA_PAGE_A0;
                switch(Dosbox.svgaCard) {
                    case SVGACards.SVGA_TsengET3K:
                    case SVGACards.SVGA_TsengET4K:
                        vgapages.mask = 0xffff;
                        break;
                    case SVGACards.SVGA_S3Trio:
                    default:
                        vgapages.mask = 0x1ffff;
                        break;
                }
                Memory.MEM_SetPageHandler(VGA_PAGE_A0, 32, newHandler);
                break;
            case 1:
                vgapages.base = VGA_PAGE_A0;
                vgapages.mask = 0xffff;
                Memory.MEM_SetPageHandler(VGA_PAGE_A0, 16, newHandler);
                Memory.MEM_ResetPageHandler(VGA_PAGE_B0, 16);
                break;
            case 2:
                vgapages.base = VGA_PAGE_B0;
                vgapages.mask = 0x7fff;
                Memory.MEM_SetPageHandler(VGA_PAGE_B0, 8, newHandler);
                Memory.MEM_ResetPageHandler(VGA_PAGE_A0, 16);
                Memory.MEM_ResetPageHandler(VGA_PAGE_B8, 8);
                break;
            case 3:
                vgapages.base = VGA_PAGE_B8;
                vgapages.mask = 0x7fff;
                Memory.MEM_SetPageHandler(VGA_PAGE_B8, 8, newHandler);
                Memory.MEM_ResetPageHandler(VGA_PAGE_A0, 16);
                Memory.MEM_ResetPageHandler(VGA_PAGE_B0, 8);
                break;
        }
        if (Dosbox.svgaCard == SVGACards.SVGA_S3Trio && (VGA.vga.s3.ext_mem_ctrl & 0x10) != 0) Memory.MEM_SetPageHandler(VGA_PAGE_A0, 16, vgaph.mmio);
        rangeDone();
    }

    public static void VGA_StartUpdateLFB() {
        VGA.vga.lfb.page = VGA.vga.s3.la_window << 4;
        VGA.vga.lfb.addr = VGA.vga.s3.la_window << 16;
        if (VGA.VGA_LFB_MAPPED) VGA.vga.lfb.handler = vgaph.lfb; else VGA.vga.lfb.handler = vgaph.lfbchanges;
        Memory.MEM_SetLFB(VGA.vga.s3.la_window << 4, (int) (VGA.vga.vmemsize / 4096), VGA.vga.lfb.handler, vgaph.mmio);
    }

    public static Section.SectionFunction VGA_Memory_ShutDown = new Section.SectionFunction() {

        public void call(Section section) {
            VGA.vga.mem.linear_orgptr = 0;
            VGA.vga.mem.linear = 0;
            VGA.vga.fastmem_orgptr = 0;
            VGA.vga.fastmem = 0;
        }
    };

    public static Section.SectionFunction VGA_SetupMemory = new Section.SectionFunction() {

        public void call(Section sec) {
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = 0;
            VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_write_full = 0;
            int vga_allocsize = VGA.vga.vmemsize;
            if (vga_allocsize < 512 * 1024) vga_allocsize = 512 * 1024;
            vga_allocsize += 2048;
            VGA.vga.mem.linear_orgptr = Memory.allocate(vga_allocsize + 16);
            VGA.vga.mem.linear = VGA.vga.mem.linear_orgptr;
            VGA.vga.fastmem_orgptr = Memory.allocate((VGA.vga.vmemsize << 1) + 4096 + 16);
            VGA.vga.fastmem = VGA.vga.fastmem_orgptr;
            VGA_draw.TempLine = Memory.allocate(VGA_draw.TEMPLINE_SIZE);
            VGA.vga.vmemwrap = VGA.vga.vmemsize;
            if (VGA_KEEP_CHANGES) {
                VGA.vga.changes = new VGA.VGA_Changes();
                int changesMapSize = (VGA.vga.vmemsize >> VGA.VGA_CHANGE_SHIFT) + 32;
                VGA.vga.changes.map = new Ptr(changesMapSize);
            }
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = 0;
            VGA.vga.svga.bank_read_full = VGA.vga.svga.bank_write_full = 0;
            VGA.vga.svga.bank_size = 0x10000;
            sec.AddDestroyFunction(VGA_Memory_ShutDown);
            if (Dosbox.machine == MachineType.MCH_PCJR) {
            }
        }
    };
}
