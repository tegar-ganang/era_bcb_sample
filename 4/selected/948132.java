package jdos.ints;

import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.hardware.VGA;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

public class Int10_misc {

    public static void INT10_GetFuncStateInformation(int save) {
        Memory.mem_writed(save, Int10.int10.rom.static_state);
        int i;
        for (i = 0; i < 0x1e; i++) {
            Memory.mem_writeb(save + 0x4 + i, Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CURRENT_MODE + i));
        }
        Memory.mem_writeb(save + 0x22, Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_ROWS) + 1);
        for (i = 1; i < 3; i++) {
            Memory.mem_writeb(save + 0x22 + i, Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_NB_ROWS + i));
        }
        for (i = 0x25; i < 0x40; i++) Memory.mem_writeb(save + i, 0);
        short dccode = 0x00;
        int vsavept = Memory.real_readd(Int10.BIOSMEM_SEG, Int10.BIOSMEM_VS_POINTER);
        int svstable = Memory.real_readd(Memory.RealSeg(vsavept), Memory.RealOff(vsavept) + 0x10);
        if (svstable != 0) {
            int dcctable = Memory.real_readd(Memory.RealSeg(svstable), Memory.RealOff(svstable) + 0x02);
            short entries = Memory.real_readb(Memory.RealSeg(dcctable), Memory.RealOff(dcctable) + 0x00);
            short idx = Memory.real_readb(Int10.BIOSMEM_SEG, Int10.BIOSMEM_DCC_INDEX);
            if (idx < entries) {
                int dccentry = Memory.real_readw(Memory.RealSeg(dcctable), Memory.RealOff(dcctable) + 0x04 + idx * 2);
                if ((dccentry & 0xff) == 0) dccode = (short) ((dccentry >> 8) & 0xff); else dccode = (short) (dccentry & 0xff);
            }
        }
        Memory.mem_writeb(save + 0x25, dccode);
        int col_count = 0;
        switch(Int10_modes.CurMode.type) {
            case VGA.M_TEXT:
                if (Int10_modes.CurMode.mode == 0x7) col_count = 1; else col_count = 16;
                break;
            case VGA.M_CGA2:
                col_count = 2;
                break;
            case VGA.M_CGA4:
                col_count = 4;
                break;
            case VGA.M_EGA:
                if (Int10_modes.CurMode.mode == 0x11 || Int10_modes.CurMode.mode == 0x0f) col_count = 2; else col_count = 16;
                break;
            case VGA.M_VGA:
                col_count = 256;
                break;
            default:
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "Get Func State illegal mode type " + Int10_modes.CurMode.type);
        }
        Memory.mem_writew(save + 0x27, col_count);
        Memory.mem_writeb(save + 0x29, Int10_modes.CurMode.ptotal);
        switch(Int10_modes.CurMode.sheight) {
            case 200:
                Memory.mem_writeb(save + 0x2a, 0);
                break;
            case 350:
                Memory.mem_writeb(save + 0x2a, 1);
                break;
            case 400:
                Memory.mem_writeb(save + 0x2a, 2);
                break;
            case 480:
                Memory.mem_writeb(save + 0x2a, 3);
                break;
        }
        if (Int10_modes.CurMode.type == VGA.M_TEXT) Memory.mem_writeb(save + 0x2d, 0x21); else Memory.mem_writeb(save + 0x2d, 0x01);
        Memory.mem_writeb(save + 0x31, 3);
    }

    public static int INT10_EGA_RIL_GetVersionPt() {
        return Memory.RealMake(0xc000, 0x30);
    }

    private static void EGA_RIL(int dx, IntRef port, IntRef regs) {
        port.value = 0;
        regs.value = 0;
        switch(dx) {
            case 0x00:
                port.value = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS);
                regs.value = 25;
                break;
            case 0x08:
                port.value = 0x3C4;
                regs.value = 5;
                break;
            case 0x10:
                port.value = 0x3CE;
                regs.value = 9;
                break;
            case 0x18:
                port.value = 0x3c0;
                regs.value = 20;
                break;
            case 0x20:
                port.value = 0x3C2;
                break;
            case 0x28:
                port.value = Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6;
                break;
            case 0x30:
                port.value = 0x3CC;
                break;
            case 0x38:
                port.value = 0x3CA;
                break;
            default:
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "unknown RIL port selection " + Integer.toString(dx, 16));
                break;
        }
    }

    public static short INT10_EGA_RIL_ReadRegister(short bl, int dx) {
        IntRef port = new IntRef(0);
        IntRef regs = new IntRef(0);
        EGA_RIL(dx, port, regs);
        if (regs.value == 0) {
            if (port.value != 0) bl = IoHandler.IO_Read(port.value);
        } else {
            if (port.value == 0x3c0) IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
            IoHandler.IO_Write(port.value, bl);
            bl = IoHandler.IO_Read(port.value + 1);
            if (port.value == 0x3c0) IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
            Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_NORMAL, "EGA RIL read used with multi-reg");
        }
        return bl;
    }

    public static short INT10_EGA_RIL_WriteRegister(short bl, short bh, int dx) {
        IntRef port = new IntRef(0);
        IntRef regs = new IntRef(0);
        EGA_RIL(dx, port, regs);
        if (regs.value == 0) {
            if (port.value != 0) IoHandler.IO_Write(port.value, bl);
        } else {
            if (port.value == 0x3c0) {
                IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
                IoHandler.IO_Write(port.value, bl);
                IoHandler.IO_Write(port.value, bh);
            } else {
                IoHandler.IO_Write(port.value, bl);
                IoHandler.IO_Write(port.value + 1, bh);
            }
            bl = bh;
            Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_NORMAL, "EGA RIL write used with multi-reg");
        }
        return bl;
    }

    public static void INT10_EGA_RIL_ReadRegisterRange(short ch, short cl, int dx, int dst) {
        IntRef port = new IntRef(0);
        IntRef regs = new IntRef(0);
        EGA_RIL(dx, port, regs);
        if (regs.value == 0) {
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "EGA RIL range read with port " + Integer.toString(port.value, 16) + " called");
        } else {
            if (ch < regs.value) {
                if (ch + cl > regs.value) cl = (short) (regs.value - ch);
                for (int i = 0; i < cl; i++) {
                    if (port.value == 0x3c0) IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
                    IoHandler.IO_Write(port.value, (ch + i));
                    Memory.mem_writeb(dst++, IoHandler.IO_Read(port.value + 1));
                }
                if (port.value == 0x3c0) IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
            } else {
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "EGA RIL range read from " + Integer.toString(port.value, 16) + " for invalid register " + Integer.toString(ch, 16));
            }
        }
    }

    public static void INT10_EGA_RIL_WriteRegisterRange(short ch, short cl, int dx, int src) {
        IntRef port = new IntRef(0);
        IntRef regs = new IntRef(0);
        EGA_RIL(dx, port, regs);
        if (regs.value == 0) {
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "EGA RIL range write called with port " + Integer.toString(port.value, 16));
        } else {
            if (ch < regs.value) {
                if (ch + cl > regs.value) cl = (short) (regs.value - ch);
                if (port.value == 0x3c0) {
                    IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
                    for (int i = 0; i < cl; i++) {
                        IoHandler.IO_Write(port.value, (ch + i));
                        IoHandler.IO_Write(port.value, Memory.mem_readb(src++));
                    }
                } else {
                    for (int i = 0; i < cl; i++) {
                        IoHandler.IO_Write(port.value, (ch + i));
                        IoHandler.IO_Write(port.value + 1, Memory.mem_readb(src++));
                    }
                }
            } else {
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_INT10, LogSeverities.LOG_ERROR, "EGA RIL range write to " + Integer.toString(port.value, 16) + " with invalid register " + Integer.toString(ch, 16));
            }
        }
    }

    public static void INT10_EGA_RIL_ReadRegisterSet(int cx, int tbl) {
        for (int i = 0; i < cx; i++) {
            short vl = Memory.mem_readb(tbl + 2);
            INT10_EGA_RIL_ReadRegister(vl, Memory.mem_readw(tbl));
            Memory.mem_writeb(tbl + 3, vl);
            tbl += 4;
        }
    }

    public static void INT10_EGA_RIL_WriteRegisterSet(int cx, int tbl) {
        IntRef port = new IntRef(0);
        IntRef regs = new IntRef(0);
        for (int i = 0; i < cx; i++) {
            EGA_RIL(Memory.mem_readw(tbl), port, regs);
            short vl = Memory.mem_readb(tbl + 3);
            if (regs.value == 0) {
                if (port.value != 0) IoHandler.IO_Write(port.value, vl);
            } else {
                short idx = Memory.mem_readb(tbl + 2);
                if (port.value == 0x3c0) {
                    IoHandler.IO_Read(Memory.real_readw(Int10.BIOSMEM_SEG, Int10.BIOSMEM_CRTC_ADDRESS) + 6);
                    IoHandler.IO_Write(port.value, idx);
                    IoHandler.IO_Write(port.value, vl);
                } else {
                    IoHandler.IO_Write(port.value, idx);
                    IoHandler.IO_Write(port.value + 1, vl);
                }
            }
            tbl += 4;
        }
    }
}
