package jdos.hardware;

import jdos.ints.Int10_modes;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class VGA_paradise {

    private static class SVGA_PVGA1A_DATA {

        int PR0A;

        int PR0B;

        int PR1;

        int PR2;

        int PR3;

        int PR4;

        int PR5;

        boolean locked() {
            return (PR5 & 7) != 5;
        }

        int[] clockFreq = new int[4];

        int biosMode;
    }

    static SVGA_PVGA1A_DATA pvga1a = new SVGA_PVGA1A_DATA();

    private static void bank_setup_pvga1a() {
        if ((pvga1a.PR1 & 0x08) != 0) {
        } else {
            VGA.vga.svga.bank_read = VGA.vga.svga.bank_write = (short) pvga1a.PR0A;
            VGA.vga.svga.bank_size = 4 * 1024;
            VGA_memory.VGA_SetupHandlers();
        }
    }

    private static VGA.tWritePort write_p3cf_pvga1a = new VGA.tWritePort() {

        public void call(int reg, int val, int iolen) {
            if (pvga1a.locked() && reg >= 0x09 && reg <= 0x0e) return;
            switch(reg) {
                case 0x09:
                    pvga1a.PR0A = val;
                    bank_setup_pvga1a();
                    break;
                case 0x0a:
                    pvga1a.PR0B = val;
                    bank_setup_pvga1a();
                    break;
                case 0x0b:
                    pvga1a.PR1 = (pvga1a.PR1 & ~0x08) | (val & 0x08);
                    bank_setup_pvga1a();
                    break;
                case 0x0c:
                    pvga1a.PR2 = val;
                    break;
                case 0x0d:
                    pvga1a.PR3 = val;
                    VGA.vga.config.display_start = (VGA.vga.config.display_start & 0xffff) | ((val & 0x18) << 13);
                    VGA.vga.config.cursor_start = (VGA.vga.config.cursor_start & 0xffff) | ((val & 0x18) << 13);
                    break;
                case 0x0e:
                    pvga1a.PR4 = val;
                    break;
                case 0x0f:
                    pvga1a.PR5 = val;
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:GFX:PVGA1A:Write to illegal index " + Integer.toString(reg, 16));
                    break;
            }
        }
    };

    private static VGA.tReadPort read_p3cf_pvga1a = new VGA.tReadPort() {

        public int call(int reg, int iolen) {
            if (pvga1a.locked() && reg >= 0x09 && reg <= 0x0e) return 0x0;
            switch(reg) {
                case 0x09:
                    return pvga1a.PR0A;
                case 0x0a:
                    return pvga1a.PR0B;
                case 0x0b:
                    return pvga1a.PR1;
                case 0x0c:
                    return pvga1a.PR2;
                case 0x0d:
                    return pvga1a.PR3;
                case 0x0e:
                    return pvga1a.PR4;
                case 0x0f:
                    return pvga1a.PR5;
                default:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_VGAMISC, LogSeverities.LOG_NORMAL, "VGA:GFX:PVGA1A:Read from illegal index " + Integer.toString(reg, 16));
                    break;
            }
            return 0x0;
        }
    };

    private static VGA.tFinishSetMode FinishSetMode_PVGA1A = new VGA.tFinishSetMode() {

        public void call(int crtc_base, VGA.VGA_ModeExtraData modeData) {
            pvga1a.biosMode = modeData.modeNo;
            IoHandler.IO_Write(0x3ce, 0x0f);
            int oldlock = IoHandler.IO_Read(0x3cf);
            IoHandler.IO_Write(0x3cf, 0x05);
            IoHandler.IO_Write(0x3ce, 0x09);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0a);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0b);
            short val = IoHandler.IO_Read(0x3cf);
            IoHandler.IO_Write(0x3cf, val & ~0x08);
            IoHandler.IO_Write(0x3ce, 0x0c);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0d);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0e);
            IoHandler.IO_Write(0x3cf, 0x00);
            IoHandler.IO_Write(0x3ce, 0x0f);
            IoHandler.IO_Write(0x3cf, oldlock);
            if (VGA.svga.determine_mode != null) VGA.svga.determine_mode.call();
            if (VGA.vga.mode != VGA.M_VGA) {
                VGA.vga.config.compatible_chain4 = false;
                VGA.vga.vmemwrap = VGA.vga.vmemsize;
            } else {
                VGA.vga.config.compatible_chain4 = true;
                VGA.vga.vmemwrap = 256 * 1024;
            }
            VGA_memory.VGA_SetupHandlers();
        }
    };

    private static VGA.tDetermineMode DetermineMode_PVGA1A = new VGA.tDetermineMode() {

        public void call() {
            if ((VGA.vga.attr.mode_control & 1) != 0) {
                if ((VGA.vga.gfx.mode & 0x40) != 0) VGA.VGA_SetMode((pvga1a.biosMode <= 0x13) ? VGA.M_VGA : VGA.M_LIN8); else if ((VGA.vga.gfx.mode & 0x20) != 0) VGA.VGA_SetMode(VGA.M_CGA4); else if ((VGA.vga.gfx.miscellaneous & 0x0c) == 0x0c) VGA.VGA_SetMode(VGA.M_CGA2); else VGA.VGA_SetMode((pvga1a.biosMode <= 0x13) ? VGA.M_EGA : VGA.M_LIN4);
            } else {
                VGA.VGA_SetMode(VGA.M_TEXT);
            }
        }
    };

    private static VGA.tSetClock SetClock_PVGA1A = new VGA.tSetClock() {

        public void call(int which, int target) {
            if (which < 4) {
                pvga1a.clockFreq[which] = 1000 * target;
                VGA.VGA_StartResize();
            }
        }
    };

    private static VGA.tGetClock GetClock_PVGA1A = new VGA.tGetClock() {

        public int call() {
            return pvga1a.clockFreq[(VGA.vga.misc_output >> 2) & 3];
        }
    };

    private static VGA.tAcceptsMode AcceptsMode_PVGA1A = new VGA.tAcceptsMode() {

        public boolean call(int modeNo) {
            return Int10_modes.VideoModeMemSize(modeNo) < VGA.vga.vmemsize;
        }
    };

    public static void SVGA_Setup_ParadisePVGA1A() {
        VGA.svga.write_p3cf = write_p3cf_pvga1a;
        VGA.svga.read_p3cf = read_p3cf_pvga1a;
        VGA.svga.set_video_mode = FinishSetMode_PVGA1A;
        VGA.svga.determine_mode = DetermineMode_PVGA1A;
        VGA.svga.set_clock = SetClock_PVGA1A;
        VGA.svga.get_clock = GetClock_PVGA1A;
        VGA.svga.accepts_mode = AcceptsMode_PVGA1A;
        VGA.VGA_SetClock(0, VGA.CLK_25);
        VGA.VGA_SetClock(1, VGA.CLK_28);
        VGA.VGA_SetClock(2, 32400);
        VGA.VGA_SetClock(3, 35900);
        if (VGA.vga.vmemsize == 0) VGA.vga.vmemsize = 512 * 1024;
        if (VGA.vga.vmemsize < 512 * 1024) {
            VGA.vga.vmemsize = 256 * 1024;
            pvga1a.PR1 = 1 << 6;
        } else if (VGA.vga.vmemsize > 512 * 1024) {
            VGA.vga.vmemsize = 1024 * 1024;
            pvga1a.PR1 = 3 << 6;
        } else {
            pvga1a.PR1 = 2 << 6;
        }
        int rom_base = (int) Memory.PhysMake(0xc000, 0);
        Memory.phys_writeb(rom_base + 0x007d, 'V');
        Memory.phys_writeb(rom_base + 0x007e, 'G');
        Memory.phys_writeb(rom_base + 0x007f, 'A');
        Memory.phys_writeb(rom_base + 0x0080, '=');
        IoHandler.IO_Write(0x3cf, 0x05);
    }
}
