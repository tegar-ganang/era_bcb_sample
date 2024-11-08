package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_s3 {

    private static VGA.tWritePort SVGA_S3_WriteCRTC = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x31:
                    VGA.vga.s3.reg_31 = (short) val;
                    VGA.vga.config.compatible_chain4 = (val & 0x08) == 0;
                    if (VGA.vga.config.compatible_chain4) VGA.vga.vmemwrap = 256 * 1024; else VGA.vga.vmemwrap = VGA.vga.vmemsize;
                    VGA.vga.config.display_start = (VGA.vga.config.display_start & ~0x30000) | ((val & 0x30) << 12);
                    VGA.VGA_DetermineMode();
                    VGA_memory.VGA_SetupHandlers();
                    break;
                case 0x35:
                    if (VGA.vga.s3.reg_lock1 != 0x48) return;
                    VGA.vga.s3.reg_35 = (short) (val & 0xf0);
                    if (((VGA.vga.svga.bank_read & 0xf) ^ (val & 0xf)) != 0) {
                        VGA.vga.svga.bank_read &= 0xf0;
                        VGA.vga.svga.bank_read |= val & 0xf;
                        VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                        VGA_memory.VGA_SetupHandlers();
                    }
                    break;
                case 0x38:
                    VGA.vga.s3.reg_lock1 = (short) val;
                    break;
                case 0x39:
                    VGA.vga.s3.reg_lock2 = (short) val;
                    break;
                case 0x3a:
                    VGA.vga.s3.reg_3a = (short) val;
                    break;
                case 0x40:
                    VGA.vga.s3.reg_40 = (short) val;
                    break;
                case 0x41:
                    VGA.vga.s3.reg_41 = (short) val;
                    break;
                case 0x43:
                    VGA.vga.s3.reg_43 = (short) (val & ~0x4);
                    if ((((val & 0x4) ^ (VGA.vga.config.scan_len >> 6)) & 0x4) != 0) {
                        VGA.vga.config.scan_len &= 0x2ff;
                        VGA.vga.config.scan_len |= (val & 0x4) << 6;
                        VGA_draw.VGA_CheckScanLength();
                    }
                    break;
                case 0x45:
                    VGA.vga.s3.hgc.curmode = (short) val;
                    VGA_draw.VGA_ActivateHardwareCursor();
                    break;
                case 0x46:
                    VGA.vga.s3.hgc.originx = (VGA.vga.s3.hgc.originx & 0x00ff) | (val << 8);
                    break;
                case 0x47:
                    VGA.vga.s3.hgc.originx = (VGA.vga.s3.hgc.originx & 0xff00) | val;
                    break;
                case 0x48:
                    VGA.vga.s3.hgc.originy = (VGA.vga.s3.hgc.originy & 0x00ff) | (val << 8);
                    break;
                case 0x49:
                    VGA.vga.s3.hgc.originy = (VGA.vga.s3.hgc.originy & 0xff00) | val;
                    break;
                case 0x4A:
                    if (VGA.vga.s3.hgc.fstackpos > 2) VGA.vga.s3.hgc.fstackpos = 0;
                    VGA.vga.s3.hgc.forestack.set(VGA.vga.s3.hgc.fstackpos, val);
                    VGA.vga.s3.hgc.fstackpos++;
                    break;
                case 0x4B:
                    if (VGA.vga.s3.hgc.bstackpos > 2) VGA.vga.s3.hgc.bstackpos = 0;
                    VGA.vga.s3.hgc.backstack.set(VGA.vga.s3.hgc.bstackpos, val);
                    VGA.vga.s3.hgc.bstackpos++;
                    break;
                case 0x4c:
                    VGA.vga.s3.hgc.startaddr &= 0xff;
                    VGA.vga.s3.hgc.startaddr |= ((val & 0xf) << 8);
                    if ((((int) VGA.vga.s3.hgc.startaddr) << 10) + ((64 * 64 * 2) / 8) > VGA.vga.vmemsize) {
                        VGA.vga.s3.hgc.startaddr &= 0xff;
                        Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:S3:CRTC: HGC pattern address beyond video memory");
                    }
                    break;
                case 0x4d:
                    VGA.vga.s3.hgc.startaddr &= 0xff00;
                    VGA.vga.s3.hgc.startaddr |= (val & 0xff);
                    break;
                case 0x4e:
                    VGA.vga.s3.hgc.posx = (short) (val & 0x3f);
                    break;
                case 0x4f:
                    VGA.vga.s3.hgc.posy = (short) (val & 0x3f);
                    break;
                case 0x50:
                    VGA.vga.s3.reg_50 = (short) val;
                    switch(val & VGA.S3_XGA_CMASK) {
                        case VGA.S3_XGA_32BPP:
                            VGA.vga.s3.xga_color_mode = VGA.M_LIN32;
                            break;
                        case VGA.S3_XGA_16BPP:
                            VGA.vga.s3.xga_color_mode = VGA.M_LIN16;
                            break;
                        case VGA.S3_XGA_8BPP:
                            VGA.vga.s3.xga_color_mode = VGA.M_LIN8;
                            break;
                    }
                    switch(val & VGA.S3_XGA_WMASK) {
                        case VGA.S3_XGA_1024:
                            VGA.vga.s3.xga_screen_width = 1024;
                            break;
                        case VGA.S3_XGA_1152:
                            VGA.vga.s3.xga_screen_width = 1152;
                            break;
                        case VGA.S3_XGA_640:
                            VGA.vga.s3.xga_screen_width = 640;
                            break;
                        case VGA.S3_XGA_800:
                            VGA.vga.s3.xga_screen_width = 800;
                            break;
                        case VGA.S3_XGA_1280:
                            VGA.vga.s3.xga_screen_width = 1280;
                            break;
                        default:
                            VGA.vga.s3.xga_screen_width = 1024;
                            break;
                    }
                    break;
                case 0x51:
                    VGA.vga.s3.reg_51 = (short) (val & 0xc0);
                    VGA.vga.config.display_start &= 0xF3FFFF;
                    VGA.vga.config.display_start |= (val & 3) << 18;
                    if (((VGA.vga.svga.bank_read & 0x30) ^ ((val & 0xc) << 2)) != 0) {
                        VGA.vga.svga.bank_read &= 0xcf;
                        VGA.vga.svga.bank_read |= (val & 0xc) << 2;
                        VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                        VGA_memory.VGA_SetupHandlers();
                    }
                    if ((((val & 0x30) ^ (VGA.vga.config.scan_len >> 4)) & 0x30) != 0) {
                        VGA.vga.config.scan_len &= 0xff;
                        VGA.vga.config.scan_len |= (val & 0x30) << 4;
                        VGA_draw.VGA_CheckScanLength();
                    }
                    break;
                case 0x52:
                    VGA.vga.s3.reg_52 = (short) val;
                    break;
                case 0x53:
                    if (VGA.vga.s3.ext_mem_ctrl != val) {
                        VGA.vga.s3.ext_mem_ctrl = (short) val;
                        VGA_memory.VGA_SetupHandlers();
                    }
                    break;
                case 0x55:
                    VGA.vga.s3.reg_55 = (short) val;
                    break;
                case 0x58:
                    VGA.vga.s3.reg_58 = (short) val;
                    break;
                case 0x59:
                    if (((VGA.vga.s3.la_window & 0xff00) ^ (val << 8)) != 0) {
                        VGA.vga.s3.la_window = (VGA.vga.s3.la_window & 0x00ff) | (val << 8);
                        VGA_memory.VGA_StartUpdateLFB();
                    }
                    break;
                case 0x5a:
                    if (((VGA.vga.s3.la_window & 0x00ff) ^ val) != 0) {
                        VGA.vga.s3.la_window = (VGA.vga.s3.la_window & 0xff00) | val;
                        VGA_memory.VGA_StartUpdateLFB();
                    }
                    break;
                case 0x5D:
                    if (((val ^ VGA.vga.s3.ex_hor_overflow) & 3) != 0) {
                        VGA.vga.s3.ex_hor_overflow = (short) val;
                        VGA.VGA_StartResize();
                    } else VGA.vga.s3.ex_hor_overflow = (short) val;
                    break;
                case 0x5e:
                    VGA.vga.config.line_compare = (VGA.vga.config.line_compare & 0x3ff) | (val & 0x40) << 4;
                    if (((val ^ VGA.vga.s3.ex_ver_overflow) & 0x3) != 0) {
                        VGA.vga.s3.ex_ver_overflow = (short) val;
                        VGA.VGA_StartResize();
                    } else VGA.vga.s3.ex_ver_overflow = (short) val;
                    break;
                case 0x67:
                    VGA.vga.s3.misc_control_2 = (short) val;
                    VGA.VGA_DetermineMode();
                    break;
                case 0x69:
                    if ((((VGA.vga.config.display_start & 0x1f0000) >> 16) ^ (val & 0x1f)) != 0) {
                        VGA.vga.config.display_start &= 0xffff;
                        VGA.vga.config.display_start |= (val & 0x1f) << 16;
                    }
                    break;
                case 0x6a:
                    VGA.vga.svga.bank_read = (short) (val & 0x7f);
                    VGA.vga.svga.bank_write = VGA.vga.svga.bank_read;
                    VGA_memory.VGA_SetupHandlers();
                    break;
                case 0x6b:
                    VGA.vga.s3.reg_6b = (short) val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:S3:CRTC:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort SVGA_S3_ReadCRTC = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x24:
                case 0x26:
                    return ((VGA.vga.attr.disabled & 1) != 0 ? 0x00 : 0x20) | (VGA.vga.attr.index & 0x1f);
                case 0x2d:
                    return 0x88;
                case 0x2e:
                    return 0x11;
                case 0x2f:
                    return 0x00;
                case 0x30:
                    return 0xe1;
                case 0x31:
                    return VGA.vga.s3.reg_31;
                case 0x35:
                    return VGA.vga.s3.reg_35 | (VGA.vga.svga.bank_read & 0xf);
                case 0x36:
                    return VGA.vga.s3.reg_36;
                case 0x37:
                    return 0x2b;
                case 0x38:
                    return VGA.vga.s3.reg_lock1;
                case 0x39:
                    return VGA.vga.s3.reg_lock2;
                case 0x3a:
                    return VGA.vga.s3.reg_3a;
                case 0x40:
                    return VGA.vga.s3.reg_40;
                case 0x41:
                    return VGA.vga.s3.reg_41;
                case 0x42:
                    return 0x0d;
                case 0x43:
                    return VGA.vga.s3.reg_43 | ((VGA.vga.config.scan_len >> 6) & 0x4);
                case 0x45:
                    VGA.vga.s3.hgc.bstackpos = 0;
                    VGA.vga.s3.hgc.fstackpos = 0;
                    return VGA.vga.s3.hgc.curmode | 0xa0;
                case 0x46:
                    return VGA.vga.s3.hgc.originx >> 8;
                case 0x47:
                    return VGA.vga.s3.hgc.originx & 0xff;
                case 0x48:
                    return VGA.vga.s3.hgc.originy >> 8;
                case 0x49:
                    return VGA.vga.s3.hgc.originy & 0xff;
                case 0x4A:
                    return VGA.vga.s3.hgc.forestack.get(VGA.vga.s3.hgc.fstackpos);
                case 0x4B:
                    return VGA.vga.s3.hgc.backstack.get(VGA.vga.s3.hgc.bstackpos);
                case 0x50:
                    return VGA.vga.s3.reg_50;
                case 0x51:
                    return ((VGA.vga.config.display_start >> 16) & 3) | ((VGA.vga.svga.bank_read & 0x30) >> 2) | ((VGA.vga.config.scan_len & 0x300) >> 4) | VGA.vga.s3.reg_51;
                case 0x52:
                    return VGA.vga.s3.reg_52;
                case 0x53:
                    return VGA.vga.s3.ext_mem_ctrl;
                case 0x55:
                    return VGA.vga.s3.reg_55;
                case 0x58:
                    return VGA.vga.s3.reg_58;
                case 0x59:
                    return (VGA.vga.s3.la_window >> 8);
                case 0x5a:
                    return (VGA.vga.s3.la_window & 0xff);
                case 0x5D:
                    return VGA.vga.s3.ex_hor_overflow;
                case 0x5e:
                    return VGA.vga.s3.ex_ver_overflow;
                case 0x67:
                    return VGA.vga.s3.misc_control_2;
                case 0x69:
                    return ((VGA.vga.config.display_start & 0x1f0000) >> 16);
                case 0x6a:
                    return (VGA.vga.svga.bank_read & 0x7f);
                case 0x6b:
                    return VGA.vga.s3.reg_6b;
                default:
                    return 0x00;
            }
        }
    };

    private static VGA.tWritePort SVGA_S3_WriteSEQ = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            if (reg > 0x8 && VGA.vga.s3.pll.lock != 0x6) return;
            switch(reg) {
                case 0x08:
                    VGA.vga.s3.pll.lock = (short) val;
                    break;
                case 0x10:
                    VGA.vga.s3.mclk.n = (short) (val & 0x1f);
                    VGA.vga.s3.mclk.r = (short) (val >> 5);
                    break;
                case 0x11:
                    VGA.vga.s3.mclk.m = (short) (val & 0x7f);
                    break;
                case 0x12:
                    VGA.vga.s3.clk[3].n = (short) (val & 0x1f);
                    VGA.vga.s3.clk[3].r = (short) (val >> 5);
                    break;
                case 0x13:
                    VGA.vga.s3.clk[3].m = (short) (val & 0x7f);
                    break;
                case 0x15:
                    VGA.vga.s3.pll.cmd = (short) val;
                    VGA.VGA_StartResize();
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:S3:SEQ:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort SVGA_S3_ReadSEQ = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            if (reg > 0x8 && VGA.vga.s3.pll.lock != 0x6) {
                if (reg < 0x1b) return 0; else return reg;
            }
            switch(reg) {
                case 0x08:
                    return VGA.vga.s3.pll.lock;
                case 0x10:
                    return (VGA.vga.s3.mclk.n != 0 || (VGA.vga.s3.mclk.r << 5) != 0) ? 1 : 0;
                case 0x11:
                    return VGA.vga.s3.mclk.m;
                case 0x12:
                    return (VGA.vga.s3.clk[3].n != 0 || (VGA.vga.s3.clk[3].r << 5) != 0) ? 1 : 0;
                case 0x13:
                    return VGA.vga.s3.clk[3].m;
                case 0x15:
                    return VGA.vga.s3.pll.cmd;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:S3:SEQ:Read from illegal index " + Integer.toString(reg, 16));
                    return 0;
            }
        }
    };

    private static VGA.tGetClock SVGA_S3_GetClock = new VGA.tGetClock() {

        public int call() {
            int clock = (VGA.vga.misc_output >> 2) & 3;
            if (clock == 0) clock = 25175000; else if (clock == 1) clock = 28322000; else clock = 1000 * VGA.S3_CLOCK(VGA.vga.s3.clk[clock].m, VGA.vga.s3.clk[clock].n, VGA.vga.s3.clk[clock].r);
            if ((VGA.vga.s3.pll.cmd & 0x10) != 0) clock /= 2;
            return clock;
        }
    };

    private static VGA.tHWCursorActive SVGA_S3_HWCursorActive = new VGA.tHWCursorActive() {

        public boolean call() {
            return (VGA.vga.s3.hgc.curmode & 0x1) != 0;
        }
    };

    private static VGA.tAcceptsMode SVGA_S3_AcceptsMode = new VGA.tAcceptsMode() {

        public boolean call(int mode) {
            return Int10_modes.VideoModeMemSize(mode) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_S3Trio() {
        VGA.svga.write_p3d5 = SVGA_S3_WriteCRTC;
        VGA.svga.read_p3d5 = SVGA_S3_ReadCRTC;
        VGA.svga.write_p3c5 = SVGA_S3_WriteSEQ;
        VGA.svga.read_p3c5 = SVGA_S3_ReadSEQ;
        VGA.svga.write_p3c0 = null;
        VGA.svga.read_p3c1 = null;
        VGA.svga.set_video_mode = null;
        VGA.svga.determine_mode = null;
        VGA.svga.set_clock = null;
        VGA.svga.get_clock = SVGA_S3_GetClock;
        VGA.svga.hardware_cursor_active = SVGA_S3_HWCursorActive;
        VGA.svga.accepts_mode = SVGA_S3_AcceptsMode;
        if (VGA.vga.vmemsize == 0) VGA.vga.vmemsize = 2 * 1024 * 1024;
        if (VGA.vga.vmemsize < 1024 * 1024) {
            VGA.vga.vmemsize = 512 * 1024;
            VGA.vga.s3.reg_36 = 0xfa;
        } else if (VGA.vga.vmemsize < 2048 * 1024) {
            VGA.vga.vmemsize = 1024 * 1024;
            VGA.vga.s3.reg_36 = 0xda;
        } else if (VGA.vga.vmemsize < 3072 * 1024) {
            VGA.vga.vmemsize = 2048 * 1024;
            VGA.vga.s3.reg_36 = 0x9a;
        } else if (VGA.vga.vmemsize < 4096 * 1024) {
            VGA.vga.vmemsize = 3072 * 1024;
            VGA.vga.s3.reg_36 = 0x5a;
        } else if (VGA.vga.vmemsize < 8192 * 1024) {
            VGA.vga.vmemsize = 4096 * 1024;
            VGA.vga.s3.reg_36 = 0x1a;
        } else {
            VGA.vga.vmemsize = 8192 * 1024;
            VGA.vga.s3.reg_36 = 0x7a;
        }
        int rom_base = Memory.PhysMake(0xc000, 0);
        Memory.phys_writeb(rom_base + 0x003f, 'S');
        Memory.phys_writeb(rom_base + 0x0040, '3');
        Memory.phys_writeb(rom_base + 0x0041, ' ');
        Memory.phys_writeb(rom_base + 0x0042, '8');
        Memory.phys_writeb(rom_base + 0x0043, '6');
        Memory.phys_writeb(rom_base + 0x0044, 'C');
        Memory.phys_writeb(rom_base + 0x0045, '7');
        Memory.phys_writeb(rom_base + 0x0046, '6');
        Memory.phys_writeb(rom_base + 0x0047, '4');
    }
}
