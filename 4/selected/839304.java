package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.Dos_programs;
import jdos.dos.Dos_tables;
import jdos.hardware.*;
import jdos.misc.Log;
import jdos.misc.setup.Config;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;

public class Bios extends Module_base {

    public static final int BIOS_BASE_ADDRESS_COM1 = 0x400;

    public static final int BIOS_BASE_ADDRESS_COM2 = 0x402;

    public static final int BIOS_BASE_ADDRESS_COM3 = 0x404;

    public static final int BIOS_BASE_ADDRESS_COM4 = 0x406;

    public static final int BIOS_ADDRESS_LPT1 = 0x408;

    public static final int BIOS_ADDRESS_LPT2 = 0x40a;

    public static final int BIOS_ADDRESS_LPT3 = 0x40c;

    public static final int BIOS_CONFIGURATION = 0x410;

    public static final int BIOS_MEMORY_SIZE = 0x413;

    public static final int BIOS_TRUE_MEMORY_SIZE = 0x415;

    public static final int BIOS_KEYBOARD_STATE = 0x417;

    public static final int BIOS_KEYBOARD_FLAGS1 = BIOS_KEYBOARD_STATE;

    public static final int BIOS_KEYBOARD_FLAGS2 = 0x418;

    public static final int BIOS_KEYBOARD_TOKEN = 0x419;

    public static final int BIOS_KEYBOARD_BUFFER_HEAD = 0x41a;

    public static final int BIOS_KEYBOARD_BUFFER_TAIL = 0x41c;

    public static final int BIOS_KEYBOARD_BUFFER = 0x41e;

    public static final int BIOS_DRIVE_ACTIVE = 0x43e;

    public static final int BIOS_DRIVE_RUNNING = 0x43f;

    public static final int BIOS_DISK_MOTOR_TIMEOUT = 0x440;

    public static final int BIOS_DISK_STATUS = 0x441;

    public static final int BIOS_VIDEO_MODE = 0x449;

    public static final int BIOS_SCREEN_COLUMNS = 0x44a;

    public static final int BIOS_VIDEO_MEMORY_USED = 0x44c;

    public static final int BIOS_VIDEO_MEMORY_ADDRESS = 0x44e;

    public static final int BIOS_VIDEO_CURSOR_POS = 0x450;

    public static final int BIOS_CURSOR_SHAPE = 0x460;

    public static final int BIOS_CURSOR_LAST_LINE = 0x460;

    public static final int BIOS_CURSOR_FIRST_LINE = 0x461;

    public static final int BIOS_CURRENT_SCREEN_PAGE = 0x462;

    public static final int BIOS_VIDEO_PORT = 0x463;

    public static final int BIOS_VDU_CONTROL = 0x465;

    public static final int BIOS_VDU_COLOR_REGISTER = 0x466;

    public static final int BIOS_TIMER = 0x46c;

    public static final int BIOS_24_HOURS_FLAG = 0x470;

    public static final int BIOS_KEYBOARD_FLAGS = 0x471;

    public static final int BIOS_CTRL_ALT_DEL_FLAG = 0x472;

    public static final int BIOS_HARDDISK_COUNT = 0x475;

    public static final int BIOS_LPT1_TIMEOUT = 0x478;

    public static final int BIOS_LPT2_TIMEOUT = 0x479;

    public static final int BIOS_LPT3_TIMEOUT = 0x47a;

    public static final int BIOS_COM1_TIMEOUT = 0x47c;

    public static final int BIOS_COM2_TIMEOUT = 0x47d;

    public static final int BIOS_COM3_TIMEOUT = 0x47e;

    public static final int BIOS_COM4_TIMEOUT = 0x47f;

    public static final int BIOS_KEYBOARD_BUFFER_START = 0x480;

    public static final int BIOS_KEYBOARD_BUFFER_END = 0x482;

    public static final int BIOS_ROWS_ON_SCREEN_MINUS_1 = 0x484;

    public static final int BIOS_FONT_HEIGHT = 0x485;

    public static final int BIOS_VIDEO_INFO_0 = 0x487;

    public static final int BIOS_VIDEO_INFO_1 = 0x488;

    public static final int BIOS_VIDEO_INFO_2 = 0x489;

    public static final int BIOS_VIDEO_COMBO = 0x48a;

    public static final int BIOS_KEYBOARD_FLAGS3 = 0x496;

    public static final int BIOS_KEYBOARD_LEDS = 0x497;

    public static final int BIOS_WAIT_FLAG_POINTER = 0x498;

    public static final int BIOS_WAIT_FLAG_COUNT = 0x49c;

    public static final int BIOS_WAIT_FLAG_ACTIVE = 0x4a0;

    public static final int BIOS_WAIT_FLAG_TEMP = 0x4a1;

    public static final int BIOS_PRINT_SCREEN_FLAG = 0x500;

    public static final int BIOS_VIDEO_SAVEPTR = 0x4a8;

    public static int BIOS_DEFAULT_HANDLER_LOCATION() {
        return Memory.RealMake(0xf000, 0xff53);
    }

    public static int BIOS_DEFAULT_IRQ0_LOCATION() {
        return Memory.RealMake(0xf000, 0xfea5);
    }

    public static int BIOS_DEFAULT_IRQ1_LOCATION() {
        return Memory.RealMake(0xf000, 0xe987);
    }

    public static int BIOS_DEFAULT_IRQ2_LOCATION() {
        return Memory.RealMake(0xf000, 0xff55);
    }

    public static final int MAX_SCAN_CODE = 0x58;

    static int size_extended;

    static int other_memsystems = 0;

    private static Callback.Handler INT70_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT70_Handler";
        }

        public int call() {
            IoHandler.IO_Write(0x70, 0xc);
            IoHandler.IO_Read(0x71);
            if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE) != 0) {
                long count = Memory.mem_readd(BIOS_WAIT_FLAG_COUNT) & 0xFFFFFFFFl;
                if (count > 997) {
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT, (int) count - 997);
                } else {
                    Memory.mem_writed(BIOS_WAIT_FLAG_COUNT, 0);
                    int where = Memory.Real2Phys(Memory.mem_readd(BIOS_WAIT_FLAG_POINTER));
                    Memory.mem_writeb(where, (short) (Memory.mem_readb(where) | 0x80));
                    Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE, 0);
                    Memory.mem_writed(BIOS_WAIT_FLAG_POINTER, Memory.RealMake(0, BIOS_WAIT_FLAG_TEMP));
                    IoHandler.IO_Write(0x70, 0xb);
                    IoHandler.IO_Write(0x71, (IoHandler.IO_Read(0x71) & ~0x40));
                }
            }
            IoHandler.IO_Write(0xa0, 0x20);
            IoHandler.IO_Write(0x20, 0x20);
            return 0;
        }
    };

    private static Callback[] tandy_DAC_callback = new Callback[2];

    private static class Tandy_sb {

        int port;

        short irq;

        short dma;
    }

    private static Tandy_sb tandy_sb;

    private static class Tandy_dac {

        int port;

        short irq;

        short dma;
    }

    private static Tandy_dac tandy_dac;

    private static boolean Tandy_InitializeSB() {
        IntRef sbport = new IntRef(0), sbirq = new IntRef(0), sbdma = new IntRef(0);
        if (SBlaster.SB_Get_Address(sbport, sbirq, sbdma)) {
            tandy_sb.port = (int) (sbport.value & 0xffff);
            tandy_sb.irq = (short) (sbirq.value & 0xff);
            tandy_sb.dma = (short) (sbdma.value & 0xff);
            return true;
        } else {
            tandy_sb.port = 0;
            return false;
        }
    }

    private static boolean Tandy_InitializeTS() {
        int tsport, tsirq, tsdma;
        tandy_dac.port = 0;
        return false;
    }

    private static boolean Tandy_TransferInProgress() {
        if (Memory.real_readw(0x40, 0xd0) != 0) return true;
        if (Memory.real_readb(0x40, 0xd4) == 0xff) return false;
        short tandy_dma = 1;
        if (tandy_sb.port != 0) tandy_dma = tandy_sb.dma; else if (tandy_dac.port != 0) tandy_dma = tandy_dac.dma;
        IoHandler.IO_Write(0x0c, 0x00);
        int datalen = (short) (IO.IO_ReadB(tandy_dma * 2 + 1) & 0xff);
        datalen |= (IO.IO_ReadB(tandy_dma * 2 + 1) << 8);
        if (datalen == 0xffff) return false; else if ((datalen < 0x10) && (Memory.real_readb(0x40, 0xd4) == 0x0f) && (Memory.real_readw(0x40, 0xd2) == 0x1c)) {
            return false;
        }
        return true;
    }

    private static void Tandy_SetupTransfer(int bufpt, boolean isplayback) {
        int length = Memory.real_readw(0x40, 0xd0);
        if (length == 0) return;
        if ((tandy_sb.port == 0) && (tandy_dac.port == 0)) return;
        short tandy_irq = 7;
        if (tandy_sb.port != 0) tandy_irq = tandy_sb.irq; else if (tandy_dac.port != 0) tandy_irq = tandy_dac.irq;
        short tandy_irq_vector = tandy_irq;
        if (tandy_irq_vector < 8) tandy_irq_vector += 8; else tandy_irq_vector += (0x70 - 8);
        int current_irq = Memory.RealGetVec(tandy_irq_vector);
        if (current_irq != tandy_DAC_callback[0].Get_RealPointer()) {
            Memory.real_writed(0x40, 0xd6, current_irq);
            Memory.RealSetVec(tandy_irq_vector, tandy_DAC_callback[0].Get_RealPointer());
        }
        short tandy_dma = 1;
        if (tandy_sb.port != 0) tandy_dma = tandy_sb.dma; else if (tandy_dac.port != 0) tandy_dma = tandy_dac.dma;
        if (tandy_sb.port != 0) {
            IoHandler.IO_Write(tandy_sb.port + 0xc, 0xd0);
            IoHandler.IO_Write(0x21, (IoHandler.IO_Read(0x21) & (~(1 << tandy_irq))));
            IoHandler.IO_Write(tandy_sb.port + 0xc, 0xd1);
        } else {
            IoHandler.IO_Write(tandy_dac.port, (IoHandler.IO_Read(tandy_dac.port) & 0x60));
            IoHandler.IO_Write(0x21, (IoHandler.IO_Read(0x21) & (~(1 << tandy_irq))));
        }
        IoHandler.IO_Write(0x0a, (0x04 | tandy_dma));
        IoHandler.IO_Write(0x0c, 0x00);
        if (isplayback) IoHandler.IO_Write(0x0b, (0x48 | tandy_dma)); else IoHandler.IO_Write(0x0b, 0x44 | tandy_dma);
        short bufpage = (short) ((bufpt >> 16) & 0xff);
        IoHandler.IO_Write(tandy_dma * 2, (short) (bufpt & 0xff));
        IoHandler.IO_Write(tandy_dma * 2, (short) ((bufpt >> 8) & 0xff));
        switch(tandy_dma) {
            case 0:
                IoHandler.IO_Write(0x87, bufpage);
                break;
            case 1:
                IoHandler.IO_Write(0x83, bufpage);
                break;
            case 2:
                IoHandler.IO_Write(0x81, bufpage);
                break;
            case 3:
                IoHandler.IO_Write(0x82, bufpage);
                break;
        }
        Memory.real_writeb(0x40, 0xd4, bufpage);
        long tlength = length;
        if (tlength + (bufpt & 0xffff) > 0x10000) tlength = 0x10000 - (bufpt & 0xffff);
        Memory.real_writew(0x40, 0xd0, (int) (length - tlength));
        tlength--;
        IoHandler.IO_Write(tandy_dma * 2 + 1, (short) (tlength & 0xff));
        IoHandler.IO_Write(tandy_dma * 2 + 1, (short) ((tlength >> 8) & 0xff));
        int delay = (int) (Memory.real_readw(0x40, 0xd2) & 0xfff);
        short amplitude = (short) ((Memory.real_readw(0x40, 0xd2) >> 13) & 0x7);
        if (tandy_sb.port != 0) {
            IoHandler.IO_Write(0x0a, tandy_dma);
            IoHandler.IO_Write(tandy_sb.port + 0xc, 0x40);
            IoHandler.IO_Write(tandy_sb.port + 0xc, 256 - delay * 100 / 358);
            if (isplayback) IoHandler.IO_Write(tandy_sb.port + 0xc, 0x14); else IoHandler.IO_Write(tandy_sb.port + 0xc, 0x24);
            IoHandler.IO_Write(tandy_sb.port + 0xc, (short) (tlength & 0xff));
            IoHandler.IO_Write(tandy_sb.port + 0xc, (short) ((tlength >> 8) & 0xff));
        } else {
            if (isplayback) IoHandler.IO_Write(tandy_dac.port, (IoHandler.IO_Read(tandy_dac.port) & 0x7c) | 0x03); else IoHandler.IO_Write(tandy_dac.port, (IoHandler.IO_Read(tandy_dac.port) & 0x7c) | 0x02);
            IoHandler.IO_Write(tandy_dac.port + 2, (short) (delay & 0xff));
            IoHandler.IO_Write(tandy_dac.port + 3, (short) (((delay >> 8) & 0xf) | (amplitude << 5)));
            if (isplayback) IoHandler.IO_Write(tandy_dac.port, (IoHandler.IO_Read(tandy_dac.port) & 0x7c) | 0x1f); else IoHandler.IO_Write(tandy_dac.port, (IoHandler.IO_Read(tandy_dac.port) & 0x7c) | 0x1e);
            IoHandler.IO_Write(0x0a, tandy_dma);
        }
        if (!isplayback) {
            Memory.real_writew(0x40, 0xd2, (int) (delay | 0x1000));
        }
    }

    private static Callback.Handler IRQ_TandyDAC = new Callback.Handler() {

        public String getName() {
            return "Bios.IRQ_TandyDAC";
        }

        public int call() {
            if (tandy_dac.port != 0) {
                IoHandler.IO_Read(tandy_dac.port);
            }
            if (Memory.real_readw(0x40, 0xd0) != 0) {
                IoHandler.IO_Write(0x20, 0x20);
                if (tandy_sb.port != 0) {
                    IoHandler.IO_Read(tandy_sb.port + 0xe);
                }
                short npage = (short) (Memory.real_readb(0x40, 0xd4) + 1);
                Memory.real_writeb(0x40, 0xd4, npage);
                int rb = Memory.real_readb(0x40, 0xd3);
                if ((rb & 0x10) != 0) {
                    Memory.real_writeb(0x40, 0xd3, rb & 0xef);
                    Tandy_SetupTransfer(npage << 16, false);
                } else {
                    Tandy_SetupTransfer(npage << 16, true);
                }
            } else {
                short tandy_irq = 7;
                if (tandy_sb.port != 0) tandy_irq = tandy_sb.irq; else if (tandy_dac.port != 0) tandy_irq = tandy_dac.irq;
                short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector < 8) tandy_irq_vector += 8; else tandy_irq_vector += (0x70 - 8);
                Memory.RealSetVec(tandy_irq_vector, Memory.real_readd(0x40, 0xd6));
                if (tandy_sb.port != 0) {
                    IoHandler.IO_Write(tandy_sb.port + 0xc, 0xd3);
                    IoHandler.IO_Read(tandy_sb.port + 0xe);
                }
                CPU_Regs.SegSet16CS(Memory.RealSeg(tandy_DAC_callback[1].Get_RealPointer()));
                CPU_Regs.reg_ip((short) Memory.RealOff(tandy_DAC_callback[1].Get_RealPointer()));
            }
            return Callback.CBRET_NONE;
        }
    };

    static void TandyDAC_Handler(short tfunction) {
        if ((tandy_sb.port == 0) && (tandy_dac.port == 0)) return;
        switch(tfunction) {
            case 0x81:
                if (tandy_dac.port != 0) {
                    CPU_Regs.reg_eax.word(tandy_dac.port);
                } else {
                    CPU_Regs.reg_eax.word(0xc4);
                }
                Callback.CALLBACK_SCF(Tandy_TransferInProgress());
                break;
            case 0x82:
            case 0x83:
                if (Tandy_TransferInProgress()) {
                    CPU_Regs.reg_eax.high(0x00);
                    Callback.CALLBACK_SCF(true);
                    break;
                }
                Memory.real_writew(0x40, 0xd0, CPU_Regs.reg_ecx.word());
                Memory.real_writew(0x40, 0xd2, (CPU_Regs.reg_edx.word() & 0xfff) | ((CPU_Regs.reg_eax.low() & 7) << 13));
                Tandy_SetupTransfer(Memory.PhysMake((int) CPU.Segs_ESval, CPU_Regs.reg_ebx.word()), CPU_Regs.reg_eax.high() == 0x83);
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
            case 0x84:
                CPU_Regs.reg_eax.high(0x00);
                Memory.real_writew(0x40, 0xd0, 0x0a);
                Memory.real_writew(0x40, 0xd2, 0x1c);
                Tandy_SetupTransfer(Memory.PhysMake(0xf000, 0xa084), true);
                Callback.CALLBACK_SCF(false);
                break;
            case 0x85:
                if (tandy_dac.port != 0) {
                    IoHandler.IO_Write(tandy_dac.port, (short) (IoHandler.IO_Read(tandy_dac.port) & 0xe0));
                }
                CPU_Regs.reg_eax.high(0x00);
                Callback.CALLBACK_SCF(false);
                break;
        }
    }

    private static Callback.Handler INT1A_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT1A_Handler";
        }

        public int call() {
            switch(CPU_Regs.reg_eax.high() & 0xFF) {
                case 0x00:
                    {
                        int ticks = Memory.mem_readd(BIOS_TIMER);
                        CPU_Regs.reg_eax.low(0);
                        CPU_Regs.reg_ecx.word(ticks >> 16);
                        CPU_Regs.reg_edx.word(ticks & 0xffff);
                        break;
                    }
                case 0x01:
                    Memory.mem_writed(BIOS_TIMER, (CPU_Regs.reg_ecx.word() << 16) | CPU_Regs.reg_edx.word());
                    break;
                case 0x02:
                    IoHandler.IO_Write(0x70, 0x04);
                    CPU_Regs.reg_ecx.high(IoHandler.IO_Read(0x71));
                    IoHandler.IO_Write(0x70, 0x02);
                    CPU_Regs.reg_ecx.low(IoHandler.IO_Read(0x71));
                    IoHandler.IO_Write(0x70, 0x00);
                    CPU_Regs.reg_edx.high(IoHandler.IO_Read(0x71));
                    CPU_Regs.reg_edx.low(0);
                    Callback.CALLBACK_SCF(false);
                    break;
                case 0x04:
                    IoHandler.IO_Write(0x70, 0x32);
                    CPU_Regs.reg_ecx.high(IoHandler.IO_Read(0x71));
                    IoHandler.IO_Write(0x70, 0x09);
                    CPU_Regs.reg_ecx.low(IoHandler.IO_Read(0x71));
                    IoHandler.IO_Write(0x70, 0x08);
                    CPU_Regs.reg_edx.high(IoHandler.IO_Read(0x71));
                    IoHandler.IO_Write(0x70, 0x07);
                    CPU_Regs.reg_edx.low(IoHandler.IO_Read(0x71));
                    Callback.CALLBACK_SCF(false);
                    break;
                case 0x80:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT1A:80:Setup tandy sound multiplexer to " + Integer.toString(CPU_Regs.reg_eax.low()));
                    break;
                case 0x81:
                case 0x82:
                case 0x83:
                case 0x84:
                case 0x85:
                    TandyDAC_Handler(CPU_Regs.reg_eax.high());
                    break;
                case 0xb1:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT1A:PCI bios call " + Integer.toString(CPU_Regs.reg_eax.low(), 16));
                    Callback.CALLBACK_SCF(true);
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT1A:Undefined call " + Integer.toString(CPU_Regs.reg_eax.high(), 16));
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT11_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT11_Handler";
        }

        public int call() {
            CPU_Regs.reg_eax.word(Memory.mem_readw(BIOS_CONFIGURATION));
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT8_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT8_Handler";
        }

        public int call() {
            int value = Memory.mem_readd(BIOS_TIMER) + 1;
            Memory.mem_writed(BIOS_TIMER, value);
            short val = Memory.mem_readb(BIOS_DISK_MOTOR_TIMEOUT);
            if (val != 0) Memory.mem_writeb(BIOS_DISK_MOTOR_TIMEOUT, (short) (val - 1));
            Memory.mem_writeb(BIOS_DRIVE_RUNNING, Memory.mem_readb(BIOS_DRIVE_RUNNING) & 0xF0);
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT1C_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT1C_Handler";
        }

        public int call() {
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT12_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT12_Handler";
        }

        public int call() {
            CPU_Regs.reg_eax.word(Memory.mem_readw(BIOS_MEMORY_SIZE));
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler INT17_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT17_Handler";
        }

        public int call() {
            if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "INT17:Function " + Integer.toString(CPU_Regs.reg_eax.high(), 16));
            switch(CPU_Regs.reg_eax.high()) {
                case 0x00:
                    CPU_Regs.reg_eax.high(1);
                    break;
                case 0x01:
                    break;
                case 0x02:
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0x20:
                    break;
                default:
                    Log.exit("Unhandled INT 17 call " + Integer.toString(CPU_Regs.reg_eax.high(), 16));
            }
            return Callback.CBRET_NONE;
        }
    };

    private static boolean INT14_Wait(int port, short mask, short timeout, IntRef retval) {
        double starttime = Pic.PIC_FullIndex();
        double timeout_f = timeout * 1000.0;
        while (((retval.value = IO.IO_ReadB(port)) & mask) != mask) {
            if (starttime < (Pic.PIC_FullIndex() - timeout_f)) {
                return false;
            }
            Callback.CALLBACK_Idle();
        }
        return true;
    }

    private static Callback.Handler INT14_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT14_Handler";
        }

        public int call() {
            if (CPU_Regs.reg_eax.high() > 0x3 || CPU_Regs.reg_edx.word() > 0x3) {
                Log.log_msg("BIOS INT14: Unhandled call AH=" + Integer.toString(CPU_Regs.reg_eax.high(), 16) + " DX=" + Integer.toString(CPU_Regs.reg_edx.word(), 16));
                return Callback.CBRET_NONE;
            }
            int port = Memory.real_readw(0x40, CPU_Regs.reg_edx.word() * 2);
            short timeout = Memory.mem_readb(BIOS_COM1_TIMEOUT + CPU_Regs.reg_edx.word());
            if (port == 0) {
                if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "BIOS INT14: port " + CPU_Regs.reg_edx.word() + " does not exist.");
                return Callback.CBRET_NONE;
            }
            switch(CPU_Regs.reg_eax.high()) {
                case 0x00:
                    {
                        int baudrate = 9600;
                        int baudresult;
                        int rawbaud = CPU_Regs.reg_eax.low() >> 5;
                        if (rawbaud == 0) {
                            baudrate = 110;
                        } else if (rawbaud == 1) {
                            baudrate = 150;
                        } else if (rawbaud == 2) {
                            baudrate = 300;
                        } else if (rawbaud == 3) {
                            baudrate = 600;
                        } else if (rawbaud == 4) {
                            baudrate = 1200;
                        } else if (rawbaud == 5) {
                            baudrate = 2400;
                        } else if (rawbaud == 6) {
                            baudrate = 4800;
                        } else if (rawbaud == 7) {
                            baudrate = 9600;
                        }
                        baudresult = (int) (115200 / baudrate);
                        IO.IO_WriteB(port + 3, 0x80);
                        IO.IO_WriteB(port, (short) baudresult & 0xff);
                        IO.IO_WriteB(port + 1, (short) (baudresult >> 8));
                        IO.IO_WriteB(port + 3, CPU_Regs.reg_eax.low() & 0x1F);
                        IO.IO_WriteB(port + 1, 0);
                        CPU_Regs.reg_eax.high(IO.IO_ReadB(port + 5) & 0xff);
                        CPU_Regs.reg_eax.low(IO.IO_ReadB(port + 6) & 0xff);
                        Callback.CALLBACK_SCF(false);
                        break;
                    }
                case 0x01:
                    {
                        IO.IO_WriteB(port + 4, 0x3);
                        IntRef result = new IntRef(CPU_Regs.reg_eax.high());
                        if (INT14_Wait(port + 6, (short) 0x30, timeout, result)) {
                            if (INT14_Wait(port + 5, (short) 0x20, timeout, result)) {
                                CPU_Regs.reg_eax.high(result.value);
                                IO.IO_WriteB(port, CPU_Regs.reg_eax.low());
                            } else {
                                CPU_Regs.reg_eax.high(result.value |= 0x80);
                            }
                        } else {
                            CPU_Regs.reg_eax.high(result.value |= 0x80);
                        }
                        Callback.CALLBACK_SCF(false);
                        break;
                    }
                case 0x02:
                    IO.IO_WriteB(port + 4, 0x1);
                    IntRef result = new IntRef(CPU_Regs.reg_eax.high());
                    if (INT14_Wait(port + 6, (short) 0x20, timeout, result)) {
                        if (INT14_Wait(port + 5, (short) 0x01, timeout, result)) {
                            CPU_Regs.reg_eax.high(result.value & 0x1E);
                            CPU_Regs.reg_eax.low(IO.IO_ReadB(port));
                        } else {
                            CPU_Regs.reg_eax.high(result.value |= 0x80);
                        }
                    } else {
                        CPU_Regs.reg_eax.high(result.value |= 0x80);
                    }
                    Callback.CALLBACK_SCF(false);
                    break;
                case 0x03:
                    CPU_Regs.reg_eax.high((IO.IO_ReadB(port + 5) & 0xff));
                    CPU_Regs.reg_eax.low((IO.IO_ReadB(port + 6) & 0xff));
                    Callback.CALLBACK_SCF(false);
                    break;
            }
            return Callback.CBRET_NONE;
        }
    };

    private static int biosConfigSeg = 0;

    private static boolean apm_realmode_connected = false;

    private static Callback.Handler INT15_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.INT15_Handler";
        }

        public int call() {
            switch(CPU_Regs.reg_eax.high() & 0xFF) {
                case 0x06:
                    Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "INT15 Unkown Function 6");
                    break;
                case 0xC0:
                    {
                        if (biosConfigSeg == 0) biosConfigSeg = Dos_tables.DOS_GetMemory(1);
                        int data = Memory.PhysMake(biosConfigSeg, 0);
                        Memory.mem_writew(data, 8);
                        if (Dosbox.IS_TANDY_ARCH()) {
                            if (Dosbox.machine == MachineType.MCH_TANDY) {
                                Memory.mem_writeb(data + 2, 0xFF);
                            } else {
                                Memory.mem_writeb(data + 2, 0xFD);
                            }
                            Memory.mem_writeb(data + 3, 0x0A);
                            Memory.mem_writeb(data + 4, 0x10);
                            Memory.mem_writeb(data + 5, (1 << 6) | (1 << 5) | (1 << 4));
                        } else {
                            Memory.mem_writeb(data + 2, 0xFC);
                            Memory.mem_writeb(data + 3, 0x00);
                            Memory.mem_writeb(data + 4, 0x01);
                            Memory.mem_writeb(data + 5, (1 << 6) | (1 << 5) | (1 << 4));
                        }
                        Memory.mem_writeb(data + 6, (1 << 6));
                        Memory.mem_writeb(data + 7, 0);
                        Memory.mem_writeb(data + 8, 0);
                        Memory.mem_writeb(data + 9, 0);
                        CPU.CPU_SetSegGeneralES(biosConfigSeg);
                        CPU_Regs.reg_ebx.word(0);
                        CPU_Regs.reg_eax.high(0);
                        Callback.CALLBACK_SCF(false);
                    }
                    break;
                case 0x4f:
                    Callback.CALLBACK_SCF(true);
                    break;
                case 0x83:
                    {
                        if (CPU_Regs.reg_eax.low() == 0x01) {
                            Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE, 0);
                            IoHandler.IO_Write(0x70, 0xb);
                            IoHandler.IO_Write(0x71, IoHandler.IO_Read(0x71) & ~0x40);
                            Callback.CALLBACK_SCF(false);
                            break;
                        }
                        if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE) != 0) {
                            CPU_Regs.reg_eax.high(0x80);
                            Callback.CALLBACK_SCF(true);
                            break;
                        }
                        long count = (CPU_Regs.reg_ecx.word() << 16) | CPU_Regs.reg_edx.word();
                        Memory.mem_writed(BIOS_WAIT_FLAG_POINTER, Memory.RealMake(CPU.Segs_ESval, CPU_Regs.reg_ebx.word()));
                        Memory.mem_writed(BIOS_WAIT_FLAG_COUNT, (int) count);
                        Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE, 1);
                        IoHandler.IO_Write(0x70, 0xb);
                        IoHandler.IO_Write(0x71, IoHandler.IO_Read(0x71) | 0x40);
                        Callback.CALLBACK_SCF(false);
                    }
                    break;
                case 0x84:
                    if (CPU_Regs.reg_edx.word() == 0x0000) {
                        if (Joystick.JOYSTICK_IsEnabled(0) || Joystick.JOYSTICK_IsEnabled(1)) {
                            CPU_Regs.reg_eax.low(IO.IO_ReadB(0x201) & 0xf0);
                            Callback.CALLBACK_SCF(false);
                        } else {
                            CPU_Regs.reg_eax.word(0x00f0);
                            CPU_Regs.reg_edx.word(0x0201);
                            Callback.CALLBACK_SCF(true);
                        }
                    } else if (CPU_Regs.reg_edx.word() == 0x0001) {
                        if (Joystick.JOYSTICK_IsEnabled(0)) {
                            CPU_Regs.reg_eax.word((int) (Joystick.JOYSTICK_GetMove_X(0) * 127 + 128));
                            CPU_Regs.reg_ebx.word((int) (Joystick.JOYSTICK_GetMove_Y(0) * 127 + 128));
                            if (Joystick.JOYSTICK_IsEnabled(1)) {
                                CPU_Regs.reg_ecx.word((int) (Joystick.JOYSTICK_GetMove_X(1) * 127 + 128));
                                CPU_Regs.reg_edx.word((int) (Joystick.JOYSTICK_GetMove_Y(1) * 127 + 128));
                            } else {
                                CPU_Regs.reg_ecx.word(0);
                                CPU_Regs.reg_edx.word(0);
                            }
                            Callback.CALLBACK_SCF(false);
                        } else if (Joystick.JOYSTICK_IsEnabled(1)) {
                            CPU_Regs.reg_eax.word(0);
                            CPU_Regs.reg_ebx.word(0);
                            CPU_Regs.reg_ecx.word((int) (Joystick.JOYSTICK_GetMove_X(1) * 127 + 128));
                            CPU_Regs.reg_edx.word((int) (Joystick.JOYSTICK_GetMove_Y(1) * 127 + 128));
                            Callback.CALLBACK_SCF(false);
                        } else {
                            CPU_Regs.reg_eax.word(0);
                            CPU_Regs.reg_ebx.word(0);
                            CPU_Regs.reg_ecx.word(0);
                            CPU_Regs.reg_edx.word(0);
                            Callback.CALLBACK_SCF(true);
                        }
                    } else {
                        Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT15:84:Unknown Bios Joystick functionality.");
                    }
                    break;
                case 0x86:
                    {
                        if (Memory.mem_readb(BIOS_WAIT_FLAG_ACTIVE) != 0) {
                            CPU_Regs.reg_eax.high(0x83);
                            Callback.CALLBACK_SCF(true);
                            break;
                        }
                        long count = (CPU_Regs.reg_ecx.word() << 16) | CPU_Regs.reg_edx.word();
                        Memory.mem_writed(BIOS_WAIT_FLAG_POINTER, Memory.RealMake(0, BIOS_WAIT_FLAG_TEMP));
                        Memory.mem_writed(BIOS_WAIT_FLAG_COUNT, (int) count);
                        Memory.mem_writeb(BIOS_WAIT_FLAG_ACTIVE, 1);
                        IoHandler.IO_Write(0x70, 0xb);
                        IoHandler.IO_Write(0x71, IoHandler.IO_Read(0x71) | 0x40);
                        while (Memory.mem_readd(BIOS_WAIT_FLAG_COUNT) != 0) {
                            Callback.CALLBACK_Idle();
                        }
                        Callback.CALLBACK_SCF(false);
                    }
                case 0x87:
                    {
                        boolean enabled = Memory.MEM_A20_Enabled();
                        Memory.MEM_A20_Enable(true);
                        int bytes = CPU_Regs.reg_ecx.word() * 2;
                        int data = CPU.Segs_ESphys + CPU_Regs.reg_esi.word();
                        int source = ((Memory.mem_readd(data + 0x12) & 0x00FFFFFF) + (Memory.mem_readb(data + 0x16) << 24));
                        int dest = ((Memory.mem_readd(data + 0x1A) & 0x00FFFFFF) + (Memory.mem_readb(data + 0x1E) << 24));
                        Memory.MEM_BlockCopy(dest, source, bytes);
                        CPU_Regs.reg_eax.word(0x00);
                        Memory.MEM_A20_Enable(enabled);
                        Callback.CALLBACK_SCF(false);
                        break;
                    }
                case 0x88:
                    CPU_Regs.reg_eax.word(other_memsystems != 0 ? 0 : size_extended);
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "INT15:Function 0x88 Remaining " + Integer.toString(CPU_Regs.reg_eax.word(), 16) + " kb");
                    Callback.CALLBACK_SCF(false);
                    break;
                case 0x89:
                    {
                        IoHandler.IO_Write(0x20, 0x10);
                        IoHandler.IO_Write(0x21, CPU_Regs.reg_ebx.high());
                        IoHandler.IO_Write(0x21, 0);
                        IoHandler.IO_Write(0xA0, 0x10);
                        IoHandler.IO_Write(0xA1, CPU_Regs.reg_ebx.low());
                        IoHandler.IO_Write(0xA1, 0);
                        Memory.MEM_A20_Enable(true);
                        int table = CPU.Segs_ESphys + CPU_Regs.reg_esi.word();
                        CPU.CPU_LGDT(Memory.mem_readw(table + 0x8), Memory.mem_readd(table + 0x8 + 0x2) & 0xFFFFFF);
                        CPU.CPU_LIDT(Memory.mem_readw(table + 0x10), Memory.mem_readd(table + 0x10 + 0x2) & 0xFFFFFF);
                        CPU.CPU_SET_CRX(0, CPU.CPU_GET_CRX(0) | 1);
                        CPU.CPU_SetSegGeneralDS(0x18);
                        CPU.CPU_SetSegGeneralES(0x20);
                        CPU.CPU_SetSegGeneralSS(0x28);
                        CPU_Regs.reg_esp.word(CPU_Regs.reg_esp.word() + 6);
                        CPU.CPU_SetFlags(0, CPU_Regs.FMASK_ALL);
                        CPU_Regs.reg_eax.word(0);
                        CPU.CPU_JMP(false, 0x30, CPU_Regs.reg_ecx.word(), 0);
                    }
                    break;
                case 0x90:
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0x91:
                    Callback.CALLBACK_SCF(false);
                    CPU_Regs.reg_eax.high(0);
                    break;
                case 0xc2:
                    switch(CPU_Regs.reg_eax.low()) {
                        case 0x00:
                            if (CPU_Regs.reg_ebx.high() == 0) {
                                Mouse.Mouse_SetPS2State(false);
                                CPU_Regs.reg_eax.high(0);
                                Callback.CALLBACK_SCF(false);
                            } else if (CPU_Regs.reg_ebx.high() == 0x01) {
                                if (!Mouse.Mouse_SetPS2State(true)) {
                                    CPU_Regs.reg_eax.high(5);
                                    Callback.CALLBACK_SCF(true);
                                    break;
                                }
                                CPU_Regs.reg_eax.high(0);
                                Callback.CALLBACK_SCF(false);
                            } else {
                                Callback.CALLBACK_SCF(true);
                                CPU_Regs.reg_eax.high(1);
                            }
                            break;
                        case 0x01:
                            CPU_Regs.reg_ebx.word(0x00aa);
                        case 0x05:
                            Mouse.Mouse_SetPS2State(false);
                            Callback.CALLBACK_SCF(false);
                            CPU_Regs.reg_eax.high(0);
                            break;
                        case 0x02:
                        case 0x03:
                            Callback.CALLBACK_SCF(false);
                            CPU_Regs.reg_eax.high(0);
                            break;
                        case 0x04:
                            CPU_Regs.reg_ebx.high(0);
                            Callback.CALLBACK_SCF(false);
                            CPU_Regs.reg_eax.high(0);
                            break;
                        case 0x06:
                            if ((CPU_Regs.reg_ebx.high() == 0x01) || (CPU_Regs.reg_ebx.high() == 0x02)) {
                                Callback.CALLBACK_SCF(false);
                                CPU_Regs.reg_eax.high(0);
                            } else {
                                Callback.CALLBACK_SCF(true);
                                CPU_Regs.reg_eax.high(1);
                            }
                            break;
                        case 0x07:
                            Mouse.Mouse_ChangePS2Callback((int) CPU.Segs_ESval, CPU_Regs.reg_ebx.word());
                            Callback.CALLBACK_SCF(false);
                            CPU_Regs.reg_eax.high(0);
                            break;
                        default:
                            Callback.CALLBACK_SCF(true);
                            CPU_Regs.reg_eax.high(1);
                            break;
                    }
                    break;
                case 0xc3:
                    CPU_Regs.reg_eax.high(0x86);
                    Callback.CALLBACK_SCF(true);
                    break;
                case 0xc4:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "INT15:Function " + Integer.toString(CPU_Regs.reg_eax.high(), 16) + " called, bios mouse not supported");
                    Callback.CALLBACK_SCF(true);
                    break;
                case 0x53:
                    switch(CPU_Regs.reg_eax.low()) {
                        case 0x00:
                            CPU_Regs.reg_eax.high(1);
                            CPU_Regs.reg_eax.low(2);
                            CPU_Regs.reg_ebx.word(0x504d);
                            CPU_Regs.reg_ecx.word(0);
                            break;
                        case 0x01:
                            if (CPU_Regs.reg_ebx.word() != 0x0) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (!apm_realmode_connected) {
                                Callback.CALLBACK_SCF(false);
                                apm_realmode_connected = true;
                            } else {
                                CPU_Regs.reg_eax.high(0x02);
                                Callback.CALLBACK_SCF(true);
                            }
                            break;
                        case 0x04:
                            if (CPU_Regs.reg_ebx.word() != 0x0) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (apm_realmode_connected) {
                                Callback.CALLBACK_SCF(false);
                                apm_realmode_connected = false;
                            } else {
                                CPU_Regs.reg_eax.high(0x03);
                                Callback.CALLBACK_SCF(true);
                            }
                            break;
                        case 0x07:
                            if (CPU_Regs.reg_ebx.word() != 0x1) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (!apm_realmode_connected) {
                                CPU_Regs.reg_eax.high(0x03);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            switch(CPU_Regs.reg_ecx.word()) {
                                case 0x3:
                                    Log.exit("Power Off");
                                    break;
                                default:
                                    CPU_Regs.reg_eax.high(0x0A);
                                    Callback.CALLBACK_SCF(true);
                                    break;
                            }
                            break;
                        case 0x08:
                            if (CPU_Regs.reg_ebx.word() != 0x0 && CPU_Regs.reg_ebx.word() != 0x1) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            } else if (!apm_realmode_connected) {
                                CPU_Regs.reg_eax.high(0x03);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (CPU_Regs.reg_ecx.word() == 0x0) Log.log_msg("disable APM for device " + Integer.toString(CPU_Regs.reg_ebx.word(), 16)); else if (CPU_Regs.reg_ecx.word() == 0x1) Log.log_msg("enable APM for device " + Integer.toString(CPU_Regs.reg_ebx.word(), 16)); else {
                                CPU_Regs.reg_eax.high(0x0A);
                                Callback.CALLBACK_SCF(true);
                            }
                            break;
                        case 0x0e:
                            if (CPU_Regs.reg_ebx.word() != 0x0) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            } else if (!apm_realmode_connected) {
                                CPU_Regs.reg_eax.high(0x03);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (CPU_Regs.reg_eax.high() < 1) CPU_Regs.reg_eax.high(1);
                            if (CPU_Regs.reg_eax.low() < 2) CPU_Regs.reg_eax.low(2);
                            Callback.CALLBACK_SCF(false);
                            break;
                        case 0x0f:
                            if (CPU_Regs.reg_ebx.word() != 0x0 && CPU_Regs.reg_ebx.word() != 0x1) {
                                CPU_Regs.reg_eax.high(0x09);
                                Callback.CALLBACK_SCF(true);
                                break;
                            } else if (!apm_realmode_connected) {
                                CPU_Regs.reg_eax.high(0x03);
                                Callback.CALLBACK_SCF(true);
                                break;
                            }
                            if (CPU_Regs.reg_ecx.word() == 0x0) Log.log_msg("disengage APM for device " + Integer.toString(CPU_Regs.reg_ebx.word(), 16)); else if (CPU_Regs.reg_ecx.word() == 0x1) Log.log_msg("engage APM for device " + Integer.toString(CPU_Regs.reg_ebx.word(), 16)); else {
                                CPU_Regs.reg_eax.high(0x0A);
                                Callback.CALLBACK_SCF(true);
                            }
                            break;
                        default:
                            if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "unknown APM BIOS call " + Integer.toString(CPU_Regs.reg_eax.word(), 16));
                            break;
                    }
                    Callback.CALLBACK_SCF(false);
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT15:Unknown call " + Integer.toString(CPU_Regs.reg_eax.word(), 16));
                    CPU_Regs.reg_eax.high(0x86);
                    Callback.CALLBACK_SCF(true);
                    if ((Dosbox.IS_EGAVGA_ARCH()) || (Dosbox.machine == MachineType.MCH_CGA)) {
                        Callback.CALLBACK_SZF(false);
                    }
            }
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler Reboot_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios.Reboot_Handler";
        }

        public int call() {
            byte[] text = "\n\n   Reboot requested, quitting now.".getBytes();
            CPU_Regs.reg_eax.word(0);
            Callback.CALLBACK_RunRealInt(0x10);
            CPU_Regs.reg_eax.high(0xe);
            CPU_Regs.reg_ebx.word(0);
            for (int i = 0; i < text.length; i++) {
                CPU_Regs.reg_eax.low(text[i]);
                Callback.CALLBACK_RunRealInt(0x10);
            }
            Log.log_msg(new String(text));
            double start = Pic.PIC_FullIndex();
            while ((Pic.PIC_FullIndex() - start) < 3000) Callback.CALLBACK_Idle();
            throw new Dos_programs.RebootException();
        }
    };

    public static void BIOS_ZeroExtendedSize(boolean in) {
        if (in) other_memsystems++; else other_memsystems--;
        if (other_memsystems < 0) other_memsystems = 0;
    }

    private Callback[] callback = new Callback[11];

    public Bios(Section configuration) {
        super(configuration);
        for (int i = 0; i < callback.length; i++) callback[i] = new Callback();
        boolean use_tandyDAC = (Memory.real_readb(0x40, 0xd4) == 0xff);
        for (int i = 0; i < 0x200; i++) Memory.real_writeb(0x40, i, 0);
        int call_irq0 = Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_irq0, INT8_Handler, Callback.CB_IRQ0, Memory.Real2Phys(BIOS_DEFAULT_IRQ0_LOCATION()), "IRQ 0 Clock");
        Memory.RealSetVec(0x08, BIOS_DEFAULT_IRQ0_LOCATION());
        Memory.mem_writed(BIOS_TIMER, 0);
        callback[1].Install(INT11_Handler, Callback.CB_IRET, "Int 11 Equipment");
        callback[1].Set_RealVec(0x11);
        callback[2].Install(INT12_Handler, Callback.CB_IRET, "Int 12 Memory");
        callback[2].Set_RealVec(0x12);
        if (Dosbox.IS_TANDY_ARCH()) {
            if (Dosbox.machine == MachineType.MCH_TANDY) Memory.mem_writew(BIOS_MEMORY_SIZE, 624); else Memory.mem_writew(BIOS_MEMORY_SIZE, 640);
            Memory.mem_writew(BIOS_TRUE_MEMORY_SIZE, 640);
        } else Memory.mem_writew(BIOS_MEMORY_SIZE, 640);
        Bios_disk.BIOS_SetupDisks();
        callback[3].Install(INT14_Handler, Callback.CB_IRET_STI, "Int 14 COM-port");
        callback[3].Set_RealVec(0x14);
        callback[4].Install(INT15_Handler, Callback.CB_IRET, "Int 15 Bios");
        callback[4].Set_RealVec(0x15);
        Bios_keyboard.BIOS_SetupKeyboard();
        callback[5].Install(INT17_Handler, Callback.CB_IRET_STI, "Int 17 Printer");
        callback[5].Set_RealVec(0x17);
        callback[6].Install(INT1A_Handler, Callback.CB_IRET_STI, "Int 1a Time");
        callback[6].Set_RealVec(0x1A);
        callback[7].Install(INT1C_Handler, Callback.CB_IRET, "Int 1c Timer");
        callback[7].Set_RealVec(0x1C);
        callback[8].Install(INT70_Handler, Callback.CB_IRET, "Int 70 RTC");
        callback[8].Set_RealVec(0x70);
        callback[9].Install(null, Callback.CB_IRQ9, "irq 9 bios");
        callback[9].Set_RealVec(0x71);
        callback[10].Install(Reboot_Handler, Callback.CB_IRET, "reboot");
        callback[10].Set_RealVec(0x18);
        int rptr = callback[10].Get_RealPointer();
        Memory.RealSetVec(0x19, rptr);
        Memory.phys_writeb(0xFFFF0, 0xEA);
        Memory.phys_writew(0xFFFF1, Memory.RealOff(rptr));
        Memory.phys_writew(0xFFFF3, Memory.RealSeg(rptr));
        int call_irq2 = Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_irq2, null, Callback.CB_IRET_EOI_PIC1, Memory.Real2Phys(BIOS_DEFAULT_IRQ2_LOCATION()), "irq 2 bios");
        Memory.RealSetVec(0x0a, BIOS_DEFAULT_IRQ2_LOCATION());
        Memory.phys_writeb((int) Memory.Real2Phys(BIOS_DEFAULT_HANDLER_LOCATION()), 0xcf);
        Memory.phys_writew((int) Memory.Real2Phys(Memory.RealGetVec(0x12)) + 0x12, 0x20);
        if (Dosbox.machine == MachineType.MCH_TANDY) Memory.phys_writeb(0xffffe, 0xff); else if (Dosbox.machine == MachineType.MCH_PCJR) Memory.phys_writeb(0xffffe, 0xfd); else Memory.phys_writeb(0xffffe, 0xfc);
        byte[] b_type = "IBM COMPATIBLE 486 BIOS COPYRIGHT The DOSBox Team.".getBytes();
        for (int i = 0; i < b_type.length; i++) Memory.phys_writeb(0xfe00e + i, b_type[i]);
        byte[] b_vers = "DOSBox FakeBIOS v1.0".getBytes();
        for (int i = 0; i < b_vers.length; i++) Memory.phys_writeb(0xfe061 + i, b_vers[i]);
        byte[] b_date = "01/01/92".getBytes();
        for (int i = 0; i < b_date.length; i++) Memory.phys_writeb(0xffff5 + i, b_date[i]);
        Memory.phys_writeb(0xfffff, 0x55);
        tandy_sb.port = 0;
        tandy_dac.port = 0;
        if (use_tandyDAC) {
            int tandy_dac_type = 0;
            if (Tandy_InitializeSB()) {
                tandy_dac_type = 1;
            } else if (Tandy_InitializeTS()) {
                tandy_dac_type = 2;
            }
            if (tandy_dac_type != 0) {
                Memory.real_writew(0x40, 0xd0, 0x0000);
                Memory.real_writew(0x40, 0xd2, 0x0000);
                Memory.real_writeb(0x40, 0xd4, 0xff);
                Memory.real_writed(0x40, 0xd6, 0x00000000);
                tandy_DAC_callback[0] = new Callback();
                tandy_DAC_callback[1] = new Callback();
                tandy_DAC_callback[0].Install(IRQ_TandyDAC, Callback.CB_IRET, "Tandy DAC IRQ");
                tandy_DAC_callback[1].Install(null, Callback.CB_TDE_IRET, "Tandy DAC end transfer");
                short tandy_irq = 7;
                if (tandy_dac_type == 1) tandy_irq = tandy_sb.irq; else if (tandy_dac_type == 2) tandy_irq = tandy_dac.irq;
                short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector < 8) tandy_irq_vector += 8; else tandy_irq_vector += (0x70 - 8);
                int current_irq = Memory.RealGetVec(tandy_irq_vector);
                Memory.real_writed(0x40, 0xd6, current_irq);
                for (int i = 0; i < 0x10; i++) Memory.phys_writeb((int) Memory.PhysMake(0xf000, 0xa084 + i), 0x80);
            } else Memory.real_writeb(0x40, 0xd4, 0x00);
        }
        Memory.mem_writeb(BIOS_LPT1_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_LPT2_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_LPT3_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_COM1_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_COM2_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_COM3_TIMEOUT, 1);
        Memory.mem_writeb(BIOS_COM4_TIMEOUT, 1);
        int ppindex = 0;
        if ((IoHandler.IO_Read(0x378) != 0xff) | (IoHandler.IO_Read(0x379) != 0xff)) {
            Memory.mem_writew(BIOS_ADDRESS_LPT1, 0x378);
            ppindex++;
            if ((IoHandler.IO_Read(0x278) != 0xff) | (IoHandler.IO_Read(0x279) != 0xff)) {
                Memory.mem_writew(BIOS_ADDRESS_LPT2, 0x278);
                ppindex++;
                if ((IoHandler.IO_Read(0x3bc) != 0xff) | (IoHandler.IO_Read(0x3be) != 0xff)) {
                    Memory.mem_writew(BIOS_ADDRESS_LPT3, 0x3bc);
                    ppindex++;
                }
            } else if ((IoHandler.IO_Read(0x3bc) != 0xff) | (IoHandler.IO_Read(0x3be) != 0xff)) {
                Memory.mem_writew(BIOS_ADDRESS_LPT2, 0x3bc);
                ppindex++;
            }
        } else if ((IoHandler.IO_Read(0x3bc) != 0xff) | (IoHandler.IO_Read(0x3be) != 0xff)) {
            Memory.mem_writew(BIOS_ADDRESS_LPT1, 0x3bc);
            ppindex++;
            if ((IoHandler.IO_Read(0x278) != 0xff) | (IoHandler.IO_Read(0x279) != 0xff)) {
                Memory.mem_writew(BIOS_ADDRESS_LPT2, 0x278);
                ppindex++;
            }
        } else if ((IoHandler.IO_Read(0x278) != 0xff) | (IoHandler.IO_Read(0x279) != 0xff)) {
            Memory.mem_writew(BIOS_ADDRESS_LPT1, 0x278);
            ppindex++;
        }
        int config = 0x0;
        if (ppindex == 2) config |= 0x4000; else config |= 0xc000;
        if (Config.C_FPU) {
            config |= 0x2;
        }
        switch(Dosbox.machine) {
            case MachineType.MCH_HERC:
                config |= 0x30;
                break;
            case MachineType.MCH_EGA:
            case MachineType.MCH_VGA:
            case MachineType.MCH_CGA:
            case MachineType.MCH_TANDY:
            case MachineType.MCH_PCJR:
                config |= 0x20;
                break;
            default:
                config |= 0;
                break;
        }
        config |= 0x04;
        config |= 0x1000;
        Memory.mem_writew(BIOS_CONFIGURATION, config);
        Cmos.CMOS_SetRegister(0x14, (short) (config & 0xff));
        IoHandler.IO_Write(0x70, 0x30);
        size_extended = IoHandler.IO_Read(0x71);
        IoHandler.IO_Write(0x70, 0x31);
        size_extended |= (IoHandler.IO_Read(0x71) << 8);
    }

    private void destroy() {
        if (tandy_sb.port != 0) {
            IoHandler.IO_Write(tandy_sb.port + 0xc, 0xd3);
            IoHandler.IO_Write(tandy_sb.port + 0xc, 0xd0);
        }
        Memory.real_writeb(0x40, 0xd4, 0x00);
        if (tandy_DAC_callback[0] != null) {
            long orig_vector = Memory.real_readd(0x40, 0xd6);
            if (orig_vector == tandy_DAC_callback[0].Get_RealPointer()) {
                short tandy_irq = 7;
                if (tandy_sb.port != 0) tandy_irq = tandy_sb.irq; else if (tandy_dac.port != 0) tandy_irq = tandy_dac.irq;
                short tandy_irq_vector = tandy_irq;
                if (tandy_irq_vector < 8) tandy_irq_vector += 8; else tandy_irq_vector += (0x70 - 8);
                Memory.RealSetVec(tandy_irq_vector, Memory.real_readd(0x40, 0xd6));
                Memory.real_writed(0x40, 0xd6, 0x00000000);
            }
            tandy_DAC_callback[0] = null;
            tandy_DAC_callback[1] = null;
        }
        Bios_disk.BIOS_CloseDisks();
    }

    public static void BIOS_SetComPorts(int baseaddr[]) {
        int portcount = 0;
        int equipmentword;
        for (int i = 0; i < 4; i++) {
            if (baseaddr[i] != 0) portcount++;
            if (i == 0) Memory.mem_writew(BIOS_BASE_ADDRESS_COM1, baseaddr[i]); else if (i == 1) Memory.mem_writew(BIOS_BASE_ADDRESS_COM2, baseaddr[i]); else if (i == 2) Memory.mem_writew(BIOS_BASE_ADDRESS_COM3, baseaddr[i]); else Memory.mem_writew(BIOS_BASE_ADDRESS_COM4, baseaddr[i]);
        }
        equipmentword = Memory.mem_readw(BIOS_CONFIGURATION);
        equipmentword &= (~0x0E00);
        equipmentword |= (portcount << 9);
        Memory.mem_writew(BIOS_CONFIGURATION, equipmentword);
        Cmos.CMOS_SetRegister(0x14, (short) (equipmentword & 0xff));
    }

    static Bios test;

    public static Section.SectionFunction BIOS_Destroy = new Section.SectionFunction() {

        public void call(Section section) {
            test.destroy();
            test = null;
            tandy_dac = null;
            tandy_sb = null;
        }
    };

    public static Section.SectionFunction BIOS_Init = new Section.SectionFunction() {

        public void call(Section section) {
            biosConfigSeg = 0;
            tandy_dac = new Tandy_dac();
            tandy_sb = new Tandy_sb();
            test = new Bios(section);
            section.AddDestroyFunction(BIOS_Destroy, false);
        }
    };
}
