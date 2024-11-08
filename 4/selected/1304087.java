package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_tseng {

    private static class SVGA_ET4K_DATA {

        public boolean extensionsEnabled = true;

        public int store_3d4_31;

        public int store_3d4_32;

        public int store_3d4_33;

        public int store_3d4_34;

        public int store_3d4_35;

        public int store_3d4_36;

        public int store_3d4_37;

        public int store_3d4_3f;

        public int store_3c0_16;

        public int store_3c0_17;

        public int store_3c4_06;

        public int store_3c4_07;

        public int[] clockFreq = new int[16];

        public int biosMode;
    }

    static SVGA_ET4K_DATA et4k = new SVGA_ET4K_DATA();

    private static VGA.tWritePort write_p3d5_et4k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            if (!et4k.extensionsEnabled && reg != 0x33) return;
            switch(reg) {
                case 0x31:
                    et4k.store_3d4_31 = val;
                    break;
                case 0x32:
                    et4k.store_3d4_32 = val;
                    break;
                case 0x33:
                    et4k.store_3d4_33 = val;
                    VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x03) << 16);
                    VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x0c) << 14);
                    break;
                case 0x34:
                    et4k.store_3d4_34 = val;
                    break;
                case 0x35:
                    et4k.store_3d4_35 = val;
                    VGA.vga.config.line_compare = (VGA.vga.config.line_compare & 0x3ff) | ((val & 0x10) << 6);
                    {
                        int s3val = ((val & 0x01) << 2) | ((val & 0x02) >> 1) | ((val & 0x04) >> 1) | ((val & 0x08) << 1) | ((val & 0x10) << 2);
                        if (((s3val ^ VGA.vga.s3.ex_ver_overflow) & 0x3) != 0) {
                            VGA.vga.s3.ex_ver_overflow = (short) s3val;
                            VGA.VGA_StartResize();
                        } else VGA.vga.s3.ex_ver_overflow = (short) s3val;
                    }
                    break;
                case 0x36:
                    et4k.store_3d4_34 = val;
                    break;
                case 0x37:
                    if (val != et4k.store_3d4_37) {
                        et4k.store_3d4_37 = val;
                        VGA.vga.vmemwrap = ((64 * 1024) << ((val & 8) >> 2)) << ((val & 3) - 1);
                        VGA_memory.VGA_SetupHandlers();
                    }
                    break;
                case 0x3f:
                    et4k.store_3d4_3f = val;
                    if (((val ^ VGA.vga.s3.ex_hor_overflow) & 3) != 0) {
                        VGA.vga.s3.ex_hor_overflow = (short) (val & 0x15);
                        VGA.VGA_StartResize();
                    } else VGA.vga.s3.ex_hor_overflow = (short) (val & 0x15);
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:CRTC:ET4K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3d5_et4k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            if (!et4k.extensionsEnabled && reg != 0x33) return 0x0;
            switch(reg) {
                case 0x31:
                    return et4k.store_3d4_31;
                case 0x32:
                    return et4k.store_3d4_32;
                case 0x33:
                    return et4k.store_3d4_33;
                case 0x34:
                    return et4k.store_3d4_34;
                case 0x35:
                    return et4k.store_3d4_35;
                case 0x36:
                    return et4k.store_3d4_36;
                case 0x37:
                    return et4k.store_3d4_37;
                case 0x3f:
                    return et4k.store_3d4_3f;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:CRTC:ET4K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static VGA.tWritePort write_p3c5_et4k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x06:
                    et4k.store_3c4_06 = val;
                    break;
                case 0x07:
                    et4k.store_3c4_07 = val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:SEQ:ET4K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3c5_et4k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x06:
                    return et4k.store_3c4_06;
                case 0x07:
                    return et4k.store_3c4_07;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:SEQ:ET4K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static IoHandler.IO_WriteHandler io_write_p3cd_et4k = new IoHandler.IO_WriteHandler() {

        public void call(int port, int val, int iolen) {
            write_p3cd_et4k.call(port, val, iolen);
        }
    };

    private static VGA.tWritePort write_p3cd_et4k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            VGA.vga.svga.bank_write = (short) (val & 0x0f);
            VGA.vga.svga.bank_read = (short) ((val >> 4) & 0x0f);
            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static IoHandler.IO_ReadHandler read_p3cd_et4k = new IoHandler.IO_ReadHandler() {

        public int call(int port, int iolen) {
            return (VGA.vga.svga.bank_read << 4) | VGA.vga.svga.bank_write;
        }
    };

    private static VGA.tWritePort write_p3c0_et4k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x16:
                    et4k.store_3c0_16 = val;
                    break;
                case 0x17:
                    et4k.store_3c0_17 = val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:ATTR:ET4K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3c1_et4k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x16:
                    return et4k.store_3c0_16;
                case 0x17:
                    return et4k.store_3c0_17;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:ATTR:ET4K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static int get_clock_index_et4k() {
        return ((VGA.vga.misc_output >> 2) & 3) | ((et4k.store_3d4_34 << 1) & 4) | ((et4k.store_3d4_31 >> 3) & 8);
    }

    private static void set_clock_index_et4k(int index) {
        IoHandler.IO_Write(0x3c2, (VGA.vga.misc_output & ~0x0c) | ((index & 3) << 2));
        et4k.store_3d4_34 = (et4k.store_3d4_34 & ~0x02) | ((index & 4) >> 1);
        et4k.store_3d4_31 = (et4k.store_3d4_31 & ~0xc0) | ((index & 8) << 3);
    }

    private static VGA.tFinishSetMode FinishSetMode_ET4K = new VGA.tFinishSetMode() {

        public void call(int crtc_base, VGA.VGA_ModeExtraData modeData) {
            et4k.biosMode = modeData.modeNo;
            IoHandler.IO_Write(0x3cd, 0x00);
            int et4k_hor_overflow = (modeData.hor_overflow & 0x01) | (modeData.hor_overflow & 0x04) | (modeData.hor_overflow & 0x10);
            IoHandler.IO_Write(crtc_base, 0x3f);
            IoHandler.IO_Write(crtc_base + 1, et4k_hor_overflow);
            int et4k_ver_overflow = ((modeData.ver_overflow & 0x01) << 1) | ((modeData.ver_overflow & 0x02) << 1) | ((modeData.ver_overflow & 0x04) >> 2) | ((modeData.ver_overflow & 0x10) >> 1) | ((modeData.ver_overflow & 0x40) >> 2);
            IoHandler.IO_Write(crtc_base, 0x35);
            IoHandler.IO_Write(crtc_base + 1, et4k_ver_overflow);
            IoHandler.IO_Write(crtc_base, 0x31);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x32);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x33);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x34);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x36);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x37);
            IoHandler.IO_Write(crtc_base + 1, 0x0c | (VGA.vga.vmemsize == 1024 * 1024 ? 3 : VGA.vga.vmemsize == 512 * 1024 ? 2 : 1));
            IoHandler.IO_Write(0x3c4, 0x06);
            IoHandler.IO_Write(0x3c5, 0);
            IoHandler.IO_Write(0x3c4, 0x07);
            IoHandler.IO_Write(0x3c5, 0);
            IoHandler.IO_Write(0x3c0, 0x16);
            IoHandler.IO_Write(0x3c0, 0);
            IoHandler.IO_Write(0x3c0, 0x17);
            IoHandler.IO_Write(0x3c0, 0);
            if (modeData.modeNo > 0x13) {
                int target = modeData.vtotal * 8 * modeData.htotal * 60;
                int best = 1;
                int dist = 100000000;
                for (int i = 0; i < 16; i++) {
                    int cdiff = Math.abs((int) (target - et4k.clockFreq[i]));
                    if (cdiff < dist) {
                        best = i;
                        dist = cdiff;
                    }
                }
                set_clock_index_et4k(best);
            }
            if (VGA.svga.determine_mode != null) VGA.svga.determine_mode.call();
            VGA.vga.config.compatible_chain4 = false;
            VGA.vga.vmemwrap = VGA.vga.vmemsize;
            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static VGA.tDetermineMode DetermineMode_ET4K = new VGA.tDetermineMode() {

        public void call() {
            if ((VGA.vga.attr.mode_control & 1) != 0) {
                if ((VGA.vga.gfx.mode & 0x40) != 0) VGA.VGA_SetMode((et4k.biosMode <= 0x13) ? VGA.M_VGA : VGA.M_LIN8); else if ((VGA.vga.gfx.mode & 0x20) != 0) VGA.VGA_SetMode(VGA.M_CGA4); else if ((VGA.vga.gfx.miscellaneous & 0x0c) == 0x0c) VGA.VGA_SetMode(VGA.M_CGA2); else VGA.VGA_SetMode((et4k.biosMode <= 0x13) ? VGA.M_EGA : VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    private static VGA.tSetClock SetClock_ET4K = new VGA.tSetClock() {

        public void call(int which, int target) {
            et4k.clockFreq[which] = 1000 * target;
            VGA.VGA_StartResize();
        }
    };

    private static VGA.tGetClock GetClock_ET4K = new VGA.tGetClock() {

        public int call() {
            return et4k.clockFreq[get_clock_index_et4k()];
        }
    };

    private static VGA.tAcceptsMode AcceptsMode_ET4K = new VGA.tAcceptsMode() {

        public boolean call(int modeNo) {
            return Int10_modes.VideoModeMemSize(modeNo) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_TsengET4K() {
        VGA.svga.write_p3d5 = write_p3d5_et4k;
        VGA.svga.read_p3d5 = read_p3d5_et4k;
        VGA.svga.write_p3c5 = write_p3c5_et4k;
        VGA.svga.read_p3c5 = read_p3c5_et4k;
        VGA.svga.write_p3c0 = write_p3c0_et4k;
        VGA.svga.read_p3c1 = read_p3c1_et4k;
        VGA.svga.set_video_mode = FinishSetMode_ET4K;
        VGA.svga.determine_mode = DetermineMode_ET4K;
        VGA.svga.set_clock = SetClock_ET4K;
        VGA.svga.get_clock = GetClock_ET4K;
        VGA.svga.accepts_mode = AcceptsMode_ET4K;
        VGA.VGA_SetClock(0, VGA.CLK_25);
        VGA.VGA_SetClock(1, VGA.CLK_28);
        VGA.VGA_SetClock(2, 32400);
        VGA.VGA_SetClock(3, 35900);
        VGA.VGA_SetClock(4, 39900);
        VGA.VGA_SetClock(5, 44700);
        VGA.VGA_SetClock(6, 31400);
        VGA.VGA_SetClock(7, 37500);
        VGA.VGA_SetClock(8, 50000);
        VGA.VGA_SetClock(9, 56500);
        VGA.VGA_SetClock(10, 64900);
        VGA.VGA_SetClock(11, 71900);
        VGA.VGA_SetClock(12, 79900);
        VGA.VGA_SetClock(13, 89600);
        VGA.VGA_SetClock(14, 62800);
        VGA.VGA_SetClock(15, 74800);
        IoHandler.IO_RegisterReadHandler(0x3cd, read_p3cd_et4k, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3cd, io_write_p3cd_et4k, IoHandler.IO_MB);
        if (VGA.vga.vmemsize == 0) VGA.vga.vmemsize = 1024 * 1024;
        if (VGA.vga.vmemsize < 512 * 1024) VGA.vga.vmemsize = 256 * 1024; else if (VGA.vga.vmemsize < 1024 * 1024) VGA.vga.vmemsize = 512 * 1024; else VGA.vga.vmemsize = 1024 * 1024;
        int rom_base = (int) Memory.PhysMake(0xc000, 0);
        Memory.phys_writeb(rom_base + 0x0075, ' ');
        Memory.phys_writeb(rom_base + 0x0076, 'T');
        Memory.phys_writeb(rom_base + 0x0077, 's');
        Memory.phys_writeb(rom_base + 0x0078, 'e');
        Memory.phys_writeb(rom_base + 0x0079, 'n');
        Memory.phys_writeb(rom_base + 0x007a, 'g');
        Memory.phys_writeb(rom_base + 0x007b, ' ');
    }

    private static class SVGA_ET3K_DATA {

        public int store_3d4_1b;

        public int store_3d4_1c;

        public int store_3d4_1d;

        public int store_3d4_1e;

        public int store_3d4_1f;

        public int store_3d4_20;

        public int store_3d4_21;

        public int store_3d4_23;

        public int store_3d4_24;

        public int store_3d4_25;

        public int store_3c0_16;

        public int store_3c0_17;

        public int store_3c4_06;

        public int store_3c4_07;

        public int[] clockFreq = new int[8];

        public int biosMode;
    }

    private static SVGA_ET3K_DATA et3k = new SVGA_ET3K_DATA();

    private static VGA.tWritePort write_p3d5_et3k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x1b:
                    et3k.store_3d4_1b = val;
                    break;
                case 0x1c:
                    et3k.store_3d4_1c = val;
                    break;
                case 0x1d:
                    et3k.store_3d4_1d = val;
                    break;
                case 0x1e:
                    et3k.store_3d4_1e = val;
                    break;
                case 0x1f:
                    et3k.store_3d4_1f = val;
                    break;
                case 0x20:
                    et3k.store_3d4_20 = val;
                    break;
                case 0x21:
                    et3k.store_3d4_21 = val;
                    break;
                case 0x23:
                    et3k.store_3d4_23 = val;
                    VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x02) << 15);
                    VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x01) << 16);
                    break;
                case 0x24:
                    et3k.store_3d4_24 = val;
                    break;
                case 0x25:
                    et3k.store_3d4_25 = val;
                    VGA.vga.config.line_compare = (VGA.vga.config.line_compare & 0x3ff) | ((val & 0x10) << 6);
                    {
                        int s3val = ((val & 0x01) << 2) | ((val & 0x02) >> 1) | ((val & 0x04) >> 1) | ((val & 0x08) << 1) | ((val & 0x10) << 2);
                        if (((s3val ^ VGA.vga.s3.ex_ver_overflow) & 0x3) != 0) {
                            VGA.vga.s3.ex_ver_overflow = (short) s3val;
                            VGA.VGA_StartResize();
                        } else VGA.vga.s3.ex_ver_overflow = (short) s3val;
                    }
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:CRTC:ET3K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3d5_et3k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x1b:
                    return et3k.store_3d4_1b;
                case 0x1c:
                    return et3k.store_3d4_1c;
                case 0x1d:
                    return et3k.store_3d4_1d;
                case 0x1e:
                    return et3k.store_3d4_1e;
                case 0x1f:
                    return et3k.store_3d4_1f;
                case 0x20:
                    return et3k.store_3d4_20;
                case 0x21:
                    return et3k.store_3d4_21;
                case 0x23:
                    return et3k.store_3d4_23;
                case 0x24:
                    return et3k.store_3d4_24;
                case 0x25:
                    return et3k.store_3d4_25;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:CRTC:ET3K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static VGA.tWritePort write_p3c5_et3k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x06:
                    et3k.store_3c4_06 = val;
                    break;
                case 0x07:
                    et3k.store_3c4_07 = val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:SEQ:ET3K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3c5_et3k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x06:
                    return et3k.store_3c4_06;
                case 0x07:
                    return et3k.store_3c4_07;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:SEQ:ET3K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static IoHandler.IO_WriteHandler write_p3cd_et3k = new IoHandler.IO_WriteHandler() {

        public void call(int port, int val, int iolen) {
            VGA.vga.svga.bank_write = (short) (val & 0x07);
            VGA.vga.svga.bank_read = (short) ((val >> 3) & 0x07);
            VGA.vga.svga.bank_size = (val & 0x40) != 0 ? 64 * 1024 : 128 * 1024;
            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static IoHandler.IO_ReadHandler read_p3cd_et3k = new IoHandler.IO_ReadHandler() {

        public int call(int port, int iolen) {
            return (VGA.vga.svga.bank_read << 3) | VGA.vga.svga.bank_write | ((VGA.vga.svga.bank_size == 128 * 1024) ? 0 : 0x40);
        }
    };

    private static VGA.tWritePort write_p3c0_et3k = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            switch(reg) {
                case 0x16:
                    et3k.store_3c0_16 = val;
                    break;
                case 0x17:
                    et3k.store_3c0_17 = val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:ATTR:ET3K:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3c1_et3k = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            switch(reg) {
                case 0x16:
                    return et3k.store_3c0_16;
                case 0x17:
                    return et3k.store_3c0_17;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:ATTR:ET3K:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static int get_clock_index_et3k() {
        return ((VGA.vga.misc_output >> 2) & 3) | ((et3k.store_3d4_24 << 1) & 4);
    }

    private static void set_clock_index_et3k(int index) {
        IoHandler.IO_Write(0x3c2, (VGA.vga.misc_output & ~0x0c) | ((index & 3) << 2));
        et3k.store_3d4_24 = (et3k.store_3d4_24 & ~0x02) | ((index & 4) >> 1);
    }

    private static VGA.tFinishSetMode FinishSetMode_ET3K = new VGA.tFinishSetMode() {

        public void call(int crtc_base, VGA.VGA_ModeExtraData modeData) {
            et3k.biosMode = modeData.modeNo;
            IoHandler.IO_Write(0x3cd, 0x40);
            int et4k_ver_overflow = ((modeData.ver_overflow & 0x01) << 1) | ((modeData.ver_overflow & 0x02) << 1) | ((modeData.ver_overflow & 0x04) >> 2) | ((modeData.ver_overflow & 0x10) >> 1) | ((modeData.ver_overflow & 0x40) >> 2);
            IoHandler.IO_Write(crtc_base, 0x25);
            IoHandler.IO_Write(crtc_base + 1, et4k_ver_overflow);
            for (int i = 0x16; i <= 0x21; i++) IoHandler.IO_Write(crtc_base, i);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x23);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(crtc_base, 0x24);
            IoHandler.IO_Write(crtc_base + 1, 0);
            IoHandler.IO_Write(0x3c4, 0x06);
            IoHandler.IO_Write(0x3c5, 0);
            IoHandler.IO_Write(0x3c4, 0x07);
            IoHandler.IO_Write(0x3c5, 0x40);
            IoHandler.IO_Write(0x3c0, 0x16);
            IoHandler.IO_Write(0x3c0, 0);
            IoHandler.IO_Write(0x3c0, 0x17);
            IoHandler.IO_Write(0x3c0, 0);
            if (modeData.modeNo > 0x13) {
                int target = modeData.vtotal * 8 * modeData.htotal * 60;
                int best = 1;
                int dist = 100000000;
                for (int i = 0; i < 8; i++) {
                    int cdiff = Math.abs((int) (target - et3k.clockFreq[i]));
                    if (cdiff < dist) {
                        best = i;
                        dist = cdiff;
                    }
                }
                set_clock_index_et3k(best);
            }
            if (VGA.svga.determine_mode != null) VGA.svga.determine_mode.call();
            VGA.vga.config.compatible_chain4 = false;
            VGA.vga.vmemwrap = VGA.vga.vmemsize;
            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static VGA.tDetermineMode DetermineMode_ET3K = new VGA.tDetermineMode() {

        public void call() {
            if ((VGA.vga.attr.mode_control & 1) != 0) {
                if ((VGA.vga.gfx.mode & 0x40) != 0) VGA.VGA_SetMode((et3k.biosMode <= 0x13) ? VGA.M_VGA : VGA.M_LIN8); else if ((VGA.vga.gfx.mode & 0x20) != 0) VGA.VGA_SetMode(VGA.M_CGA4); else if ((VGA.vga.gfx.miscellaneous & 0x0c) == 0x0c) VGA.VGA_SetMode(VGA.M_CGA2); else VGA.VGA_SetMode((et3k.biosMode <= 0x13) ? VGA.M_EGA : VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    private static VGA.tSetClock SetClock_ET3K = new VGA.tSetClock() {

        public void call(int which, int target) {
            et3k.clockFreq[which] = 1000 * target;
            VGA.VGA_StartResize();
        }
    };

    private static VGA.tGetClock GetClock_ET3K = new VGA.tGetClock() {

        public int call() {
            return et3k.clockFreq[get_clock_index_et3k()];
        }
    };

    private static VGA.tAcceptsMode AcceptsMode_ET3K = new VGA.tAcceptsMode() {

        public boolean call(int mode) {
            return mode <= 0x37 && mode != 0x2f && Int10_modes.VideoModeMemSize(mode) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_TsengET3K() {
        VGA.svga.write_p3d5 = write_p3d5_et3k;
        VGA.svga.read_p3d5 = read_p3d5_et3k;
        VGA.svga.write_p3c5 = write_p3c5_et3k;
        VGA.svga.read_p3c5 = read_p3c5_et3k;
        VGA.svga.write_p3c0 = write_p3c0_et3k;
        VGA.svga.read_p3c1 = read_p3c1_et3k;
        VGA.svga.set_video_mode = FinishSetMode_ET3K;
        VGA.svga.determine_mode = DetermineMode_ET3K;
        VGA.svga.set_clock = SetClock_ET3K;
        VGA.svga.get_clock = GetClock_ET3K;
        VGA.svga.accepts_mode = AcceptsMode_ET3K;
        VGA.VGA_SetClock(0, VGA.CLK_25);
        VGA.VGA_SetClock(1, VGA.CLK_28);
        VGA.VGA_SetClock(2, 32400);
        VGA.VGA_SetClock(3, 35900);
        VGA.VGA_SetClock(4, 39900);
        VGA.VGA_SetClock(5, 44700);
        VGA.VGA_SetClock(6, 31400);
        VGA.VGA_SetClock(7, 37500);
        IoHandler.IO_RegisterReadHandler(0x3cd, read_p3cd_et3k, IoHandler.IO_MB);
        IoHandler.IO_RegisterWriteHandler(0x3cd, write_p3cd_et3k, IoHandler.IO_MB);
        VGA.vga.vmemsize = 512 * 1024;
        int rom_base = (int) Memory.PhysMake(0xc000, 0);
        Memory.phys_writeb(rom_base + 0x0075, ' ');
        Memory.phys_writeb(rom_base + 0x0076, 'T');
        Memory.phys_writeb(rom_base + 0x0077, 's');
        Memory.phys_writeb(rom_base + 0x0078, 'e');
        Memory.phys_writeb(rom_base + 0x0079, 'n');
        Memory.phys_writeb(rom_base + 0x007a, 'g');
        Memory.phys_writeb(rom_base + 0x007b, ' ');
    }
}
