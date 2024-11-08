package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.IoHandler;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.IntRef;

public class Bios_keyboard {

    private static int call_int16, call_irq1, call_irq6;

    private static final int none = 0;

    private static class Scan {

        public Scan(int normal, int shift, int control, int alt) {
            this.normal = normal;
            this.shift = shift;
            this.control = control;
            this.alt = alt;
        }

        int normal;

        int shift;

        int control;

        int alt;
    }

    private static Scan[] scan_to_scanascii = { new Scan(none, none, none, none), new Scan(0x011b, 0x011b, 0x011b, 0x01f0), new Scan(0x0231, 0x0221, none, 0x7800), new Scan(0x0332, 0x0340, 0x0300, 0x7900), new Scan(0x0433, 0x0423, none, 0x7a00), new Scan(0x0534, 0x0524, none, 0x7b00), new Scan(0x0635, 0x0625, none, 0x7c00), new Scan(0x0736, 0x075e, 0x071e, 0x7d00), new Scan(0x0837, 0x0826, none, 0x7e00), new Scan(0x0938, 0x092a, none, 0x7f00), new Scan(0x0a39, 0x0a28, none, 0x8000), new Scan(0x0b30, 0x0b29, none, 0x8100), new Scan(0x0c2d, 0x0c5f, 0x0c1f, 0x8200), new Scan(0x0d3d, 0x0d2b, none, 0x8300), new Scan(0x0e08, 0x0e08, 0x0e7f, 0x0ef0), new Scan(0x0f09, 0x0f00, 0x9400, none), new Scan(0x1071, 0x1051, 0x1011, 0x1000), new Scan(0x1177, 0x1157, 0x1117, 0x1100), new Scan(0x1265, 0x1245, 0x1205, 0x1200), new Scan(0x1372, 0x1352, 0x1312, 0x1300), new Scan(0x1474, 0x1454, 0x1414, 0x1400), new Scan(0x1579, 0x1559, 0x1519, 0x1500), new Scan(0x1675, 0x1655, 0x1615, 0x1600), new Scan(0x1769, 0x1749, 0x1709, 0x1700), new Scan(0x186f, 0x184f, 0x180f, 0x1800), new Scan(0x1970, 0x1950, 0x1910, 0x1900), new Scan(0x1a5b, 0x1a7b, 0x1a1b, 0x1af0), new Scan(0x1b5d, 0x1b7d, 0x1b1d, 0x1bf0), new Scan(0x1c0d, 0x1c0d, 0x1c0a, none), new Scan(none, none, none, none), new Scan(0x1e61, 0x1e41, 0x1e01, 0x1e00), new Scan(0x1f73, 0x1f53, 0x1f13, 0x1f00), new Scan(0x2064, 0x2044, 0x2004, 0x2000), new Scan(0x2166, 0x2146, 0x2106, 0x2100), new Scan(0x2267, 0x2247, 0x2207, 0x2200), new Scan(0x2368, 0x2348, 0x2308, 0x2300), new Scan(0x246a, 0x244a, 0x240a, 0x2400), new Scan(0x256b, 0x254b, 0x250b, 0x2500), new Scan(0x266c, 0x264c, 0x260c, 0x2600), new Scan(0x273b, 0x273a, none, 0x27f0), new Scan(0x2827, 0x2822, none, 0x28f0), new Scan(0x2960, 0x297e, none, 0x29f0), new Scan(none, none, none, none), new Scan(0x2b5c, 0x2b7c, 0x2b1c, 0x2bf0), new Scan(0x2c7a, 0x2c5a, 0x2c1a, 0x2c00), new Scan(0x2d78, 0x2d58, 0x2d18, 0x2d00), new Scan(0x2e63, 0x2e43, 0x2e03, 0x2e00), new Scan(0x2f76, 0x2f56, 0x2f16, 0x2f00), new Scan(0x3062, 0x3042, 0x3002, 0x3000), new Scan(0x316e, 0x314e, 0x310e, 0x3100), new Scan(0x326d, 0x324d, 0x320d, 0x3200), new Scan(0x332c, 0x333c, none, 0x33f0), new Scan(0x342e, 0x343e, none, 0x34f0), new Scan(0x352f, 0x353f, none, 0x35f0), new Scan(none, none, none, none), new Scan(0x372a, 0x372a, 0x9600, 0x37f0), new Scan(none, none, none, none), new Scan(0x3920, 0x3920, 0x3920, 0x3920), new Scan(none, none, none, none), new Scan(0x3b00, 0x5400, 0x5e00, 0x6800), new Scan(0x3c00, 0x5500, 0x5f00, 0x6900), new Scan(0x3d00, 0x5600, 0x6000, 0x6a00), new Scan(0x3e00, 0x5700, 0x6100, 0x6b00), new Scan(0x3f00, 0x5800, 0x6200, 0x6c00), new Scan(0x4000, 0x5900, 0x6300, 0x6d00), new Scan(0x4100, 0x5a00, 0x6400, 0x6e00), new Scan(0x4200, 0x5b00, 0x6500, 0x6f00), new Scan(0x4300, 0x5c00, 0x6600, 0x7000), new Scan(0x4400, 0x5d00, 0x6700, 0x7100), new Scan(none, none, none, none), new Scan(none, none, none, none), new Scan(0x4700, 0x4737, 0x7700, 0x0007), new Scan(0x4800, 0x4838, 0x8d00, 0x0008), new Scan(0x4900, 0x4939, 0x8400, 0x0009), new Scan(0x4a2d, 0x4a2d, 0x8e00, 0x4af0), new Scan(0x4b00, 0x4b34, 0x7300, 0x0004), new Scan(0x4cf0, 0x4c35, 0x8f00, 0x0005), new Scan(0x4d00, 0x4d36, 0x7400, 0x0006), new Scan(0x4e2b, 0x4e2b, 0x9000, 0x4ef0), new Scan(0x4f00, 0x4f31, 0x7500, 0x0001), new Scan(0x5000, 0x5032, 0x9100, 0x0002), new Scan(0x5100, 0x5133, 0x7600, 0x0003), new Scan(0x5200, 0x5230, 0x9200, 0x0000), new Scan(0x5300, 0x532e, 0x9300, none), new Scan(none, none, none, none), new Scan(none, none, none, none), new Scan(0x565c, 0x567c, none, none), new Scan(0x8500, 0x8700, 0x8900, 0x8b00), new Scan(0x8600, 0x8800, 0x8a00, 0x8c00) };

    public static final Object lock = new Object();

    public static boolean BIOS_AddKeyToBuffer(int code) {
        synchronized (Bios_keyboard.lock) {
            if ((Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS2) & 8) != 0) return true;
            int start, end, head, tail, ttail;
            if (Dosbox.machine == MachineType.MCH_PCJR) {
                start = 0x1e;
                end = 0x3e;
            } else {
                start = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_START);
                end = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_END);
            }
            head = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_HEAD);
            tail = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_TAIL);
            ttail = tail + 2;
            if (ttail >= end) {
                ttail = start;
            }
            if (ttail == head) return false;
            Memory.real_writew(0x40, tail, code);
            Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_TAIL, ttail);
        }
        return true;
    }

    private static void add_key(int code) {
        if (code != 0) BIOS_AddKeyToBuffer(code);
    }

    private static boolean get_key(IntRef code) {
        synchronized (Bios_keyboard.lock) {
            int start, end, head, tail, thead;
            if (Dosbox.machine == MachineType.MCH_PCJR) {
                start = 0x1e;
                end = 0x3e;
            } else {
                start = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_START);
                end = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_END);
            }
            head = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_HEAD);
            tail = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_TAIL);
            if (head == tail) return false;
            thead = head + 2;
            if (thead >= end) thead = start;
            Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_HEAD, thead);
            code.value = Memory.real_readw(0x40, head);
        }
        return true;
    }

    static boolean check_key(IntRef code) {
        int head, tail;
        synchronized (Bios_keyboard.lock) {
            head = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_HEAD);
            tail = Memory.mem_readw(Bios.BIOS_KEYBOARD_BUFFER_TAIL);
            if (head == tail) return false;
            code.value = Memory.real_readw(0x40, head);
        }
        return true;
    }

    private static Callback.Handler IRQ1_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios_keyboard.IRQ1_Handler";
        }

        public int call() {
            int scancode = CPU_Regs.reg_eax.low();
            short flags1, flags2, flags3, leds;
            flags1 = Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS1);
            flags2 = Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS2);
            flags3 = Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS3);
            leds = Memory.mem_readb(Bios.BIOS_KEYBOARD_LEDS);
            switch(scancode) {
                case 0xfa:
                    break;
                case 0xe1:
                    flags3 |= 0x01;
                    break;
                case 0xe0:
                    flags3 |= 0x02;
                    break;
                case 0x1d:
                    if ((flags3 & 0x01) == 0) {
                        flags1 |= 0x04;
                        if ((flags3 & 0x02) != 0) flags3 |= 0x04; else flags2 |= 0x01;
                    }
                    break;
                case 0x9d:
                    if ((flags3 & 0x01) == 0) {
                        if ((flags3 & 0x02) != 0) flags3 &= ~0x04; else flags2 &= ~0x01;
                        if (!((flags3 & 0x04) != 0 || (flags2 & 0x01) != 0)) flags1 &= ~0x04;
                    }
                    break;
                case 0x2a:
                    flags1 |= 0x02;
                    break;
                case 0xaa:
                    flags1 &= ~0x02;
                    break;
                case 0x36:
                    flags1 |= 0x01;
                    break;
                case 0xb6:
                    flags1 &= ~0x01;
                    break;
                case 0x38:
                    flags1 |= 0x08;
                    if ((flags3 & 0x02) != 0) flags3 |= 0x08; else flags2 |= 0x02;
                    break;
                case 0xb8:
                    if ((flags3 & 0x02) != 0) flags3 &= ~0x08; else flags2 &= ~0x02;
                    if (!((flags3 & 0x08) != 0 || (flags2 & 0x02) != 0)) {
                        flags1 &= ~0x08;
                        int token = Memory.mem_readb(Bios.BIOS_KEYBOARD_TOKEN);
                        if (token != 0) {
                            add_key(token);
                            Memory.mem_writeb(Bios.BIOS_KEYBOARD_TOKEN, 0);
                        }
                    }
                    break;
                case 0x3a:
                    flags2 |= 0x40;
                    break;
                case 0xba:
                    flags1 ^= 0x40;
                    flags2 &= ~0x40;
                    leds ^= 0x04;
                    break;
                case 0x45:
                    if ((flags3 & 0x01) != 0) {
                        flags3 &= ~0x01;
                        Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS3, flags3);
                        if ((flags2 & 1) != 0) {
                        } else if ((flags2 & 8) == 0) {
                            Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS2, flags2 | 8);
                            IoHandler.IO_Write(0x20, 0x20);
                            while ((Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS2) & 8) != 0) Callback.CALLBACK_Idle();
                            CPU_Regs.reg_ip(CPU_Regs.reg_ip() + 5);
                            return Callback.CBRET_NONE;
                        }
                    } else {
                        flags2 |= 0x20;
                    }
                    break;
                case 0xc5:
                    if ((flags3 & 0x01) != 0) {
                        flags3 &= ~0x01;
                    } else {
                        flags1 ^= 0x20;
                        leds ^= 0x02;
                        flags2 &= ~0x20;
                    }
                    break;
                case 0x46:
                    flags2 |= 0x10;
                    break;
                case 0xc6:
                    flags1 ^= 0x10;
                    flags2 &= ~0x10;
                    leds ^= 0x01;
                    break;
                case 0xd2:
                    if ((flags3 & 0x02) != 0) {
                        flags1 ^= 0x80;
                        flags2 &= ~0x80;
                        break;
                    } else {
                        break;
                    }
                case 0x47:
                case 0x48:
                case 0x49:
                case 0x4b:
                case 0x4c:
                case 0x4d:
                case 0x4f:
                case 0x50:
                case 0x51:
                case 0x52:
                case 0x53:
                    if ((flags3 & 0x02) != 0) {
                        if (scancode == 0x52) flags2 |= 0x80;
                        if ((flags1 & 0x08) != 0) {
                            add_key(scan_to_scanascii[scancode].normal + 0x5000);
                        } else if ((flags1 & 0x04) != 0) {
                            add_key((scan_to_scanascii[scancode].control & 0xff00) | 0xe0);
                        } else if (((flags1 & 0x3) != 0) || ((flags1 & 0x20) != 0)) {
                            add_key((scan_to_scanascii[scancode].shift & 0xff00) | 0xe0);
                        } else add_key((scan_to_scanascii[scancode].normal & 0xff00) | 0xe0);
                        break;
                    }
                    if ((flags1 & 0x08) != 0) {
                        short token = Memory.mem_readb(Bios.BIOS_KEYBOARD_TOKEN);
                        token = (short) (token * 10 + scan_to_scanascii[scancode].alt & 0xff);
                        Memory.mem_writeb(Bios.BIOS_KEYBOARD_TOKEN, token);
                    } else if ((flags1 & 0x04) != 0) {
                        add_key(scan_to_scanascii[scancode].control);
                    } else if (((flags1 & 0x3) != 0) || ((flags1 & 0x20) != 0)) {
                        add_key(scan_to_scanascii[scancode].shift);
                    } else add_key(scan_to_scanascii[scancode].normal);
                    break;
                default:
                    int asciiscan;
                    if ((scancode & 0x80) != 0) break;
                    if (scancode > Bios.MAX_SCAN_CODE) break;
                    if ((flags1 & 0x08) != 0) {
                        asciiscan = scan_to_scanascii[scancode].alt;
                    } else if ((flags1 & 0x04) != 0) {
                        asciiscan = scan_to_scanascii[scancode].control;
                    } else if ((flags1 & 0x03) != 0) {
                        asciiscan = scan_to_scanascii[scancode].shift;
                    } else {
                        asciiscan = scan_to_scanascii[scancode].normal;
                    }
                    if ((flags1 & 64) != 0) {
                        if ((flags1 & 3) != 0) {
                            if (((asciiscan & 0x00ff) > 0x40) && ((asciiscan & 0x00ff) < 0x5b)) asciiscan = scan_to_scanascii[scancode].normal;
                        } else {
                            if (((asciiscan & 0x00ff) > 0x60) && ((asciiscan & 0x00ff) < 0x7b)) asciiscan = scan_to_scanascii[scancode].shift;
                        }
                    }
                    if ((flags3 & 0x02) != 0) {
                        if (scancode == 0x1c) {
                            if ((flags1 & 0x08) != 0) asciiscan = 0xa600; else asciiscan = (asciiscan & 0xff) | 0xe000;
                        } else if (scancode == 0x35) {
                            if ((flags1 & 0x08) != 0) asciiscan = 0xa400; else if ((flags1 & 0x04) != 0) asciiscan = 0x9500; else asciiscan = 0xe02f;
                        }
                    }
                    add_key(asciiscan);
                    break;
            }
            if (scancode != 0xe0) flags3 &= ~0x02;
            Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS1, flags1);
            if ((scancode & 0x80) == 0) flags2 &= 0xf7;
            Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS2, flags2);
            Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS3, flags3);
            Memory.mem_writeb(Bios.BIOS_KEYBOARD_LEDS, leds);
            if (false) {
                short old61 = IoHandler.IO_Read(0x61);
                IoHandler.IO_Write(0x61, old61 | 128);
                IoHandler.IO_Write(0x64, 0xae);
            }
            return Callback.CBRET_NONE;
        }
    };

    private static boolean IsEnhancedKey(IntRef key) {
        if ((key.value >> 8) == 0xe0) {
            if (((key.value & 0xff) == 0x0a) || ((key.value & 0xff) == 0x0d)) {
                key.value = (key.value & 0xff) | 0x1c00;
            } else {
                key.value = (key.value & 0xff) | 0x3500;
            }
            return false;
        } else if (((key.value >> 8) > 0x84) || (((key.value & 0xff) == 0xf0) && (key.value >> 8) != 0)) {
            return true;
        }
        if ((key.value >> 8) != 0 && ((key.value & 0xff) == 0xe0)) {
            key.value &= 0xff00;
        }
        return false;
    }

    private static Callback.Handler INT16_Handler = new Callback.Handler() {

        public String getName() {
            return "Bios_keyboard.INT16_Handler";
        }

        public int call() {
            IntRef temp = new IntRef(0);
            switch(CPU_Regs.reg_eax.high()) {
                case 0x00:
                    if ((get_key(temp)) && (!IsEnhancedKey(temp))) {
                        CPU_Regs.reg_eax.word(temp.value);
                    } else {
                        CPU_Regs.reg_ip(CPU_Regs.reg_ip() + 1);
                    }
                    break;
                case 0x10:
                    if (get_key(temp)) {
                        if (((temp.value & 0xff) == 0xf0) && (temp.value >> 8) != 0) {
                            temp.value &= 0xff00;
                        }
                        CPU_Regs.reg_eax.word(temp.value);
                    } else {
                        CPU_Regs.reg_ip(CPU_Regs.reg_ip() + 1);
                    }
                    break;
                case 0x01:
                    Memory.mem_writew(CPU.Segs_SSphys + CPU_Regs.reg_esp.word() + 4, (Memory.mem_readw(CPU.Segs_SSphys + CPU_Regs.reg_esp.word() + 4) | CPU_Regs.IF));
                    for (; ; ) {
                        if (check_key(temp)) {
                            if (!IsEnhancedKey(temp)) {
                                Callback.CALLBACK_SZF(false);
                                CPU_Regs.reg_eax.word(temp.value);
                                break;
                            } else {
                                get_key(temp);
                            }
                        } else {
                            Callback.CALLBACK_SZF(true);
                            break;
                        }
                    }
                    break;
                case 0x11:
                    if (!check_key(temp)) {
                        Callback.CALLBACK_SZF(true);
                    } else {
                        Callback.CALLBACK_SZF(false);
                        if (((temp.value & 0xff) == 0xf0) && (temp.value >> 8) != 0) {
                            temp.value &= 0xff00;
                        }
                        CPU_Regs.reg_eax.word(temp.value);
                    }
                    break;
                case 0x02:
                    CPU_Regs.reg_eax.low(Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS1));
                    break;
                case 0x03:
                    if (CPU_Regs.reg_eax.low() == 0x00) {
                        IoHandler.IO_Write(0x60, 0xf3);
                        IoHandler.IO_Write(0x60, 0x20);
                    } else if (CPU_Regs.reg_eax.low() == 0x05) {
                        IoHandler.IO_Write(0x60, 0xf3);
                        IoHandler.IO_Write(0x60, (CPU_Regs.reg_ebx.high() & 3) << 5 | (CPU_Regs.reg_ebx.low() & 0x1f));
                    } else {
                        if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT16:Unhandled Typematic Rate Call " + Integer.toString(CPU_Regs.reg_eax.low(), 16) + " BX=" + Integer.toString(CPU_Regs.reg_ebx.word(), 16));
                    }
                    break;
                case 0x05:
                    if (BIOS_AddKeyToBuffer(CPU_Regs.reg_ecx.word())) CPU_Regs.reg_eax.low(0); else CPU_Regs.reg_eax.low(1);
                    break;
                case 0x12:
                    CPU_Regs.reg_eax.low(Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS1));
                    CPU_Regs.reg_eax.high(Memory.mem_readb(Bios.BIOS_KEYBOARD_FLAGS2));
                    break;
                case 0x55:
                    Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_NORMAL, "INT16:55:Word TSR compatible call");
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_BIOS, LogSeverities.LOG_ERROR, "INT16:Unhandled call " + Integer.toString(CPU_Regs.reg_eax.high(), 16));
                    break;
            }
            return Callback.CBRET_NONE;
        }
    };

    private static void InitBiosSegment() {
        Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_START, 0x1e);
        Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_END, 0x3e);
        Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_HEAD, 0x1e);
        Memory.mem_writew(Bios.BIOS_KEYBOARD_BUFFER_TAIL, 0x1e);
        short flag1 = 0;
        short leds = 16;
        Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS1, flag1);
        Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS2, 0);
        Memory.mem_writeb(Bios.BIOS_KEYBOARD_FLAGS3, 16);
        Memory.mem_writeb(Bios.BIOS_KEYBOARD_TOKEN, 0);
        Memory.mem_writeb(Bios.BIOS_KEYBOARD_LEDS, leds);
    }

    public static void BIOS_SetupKeyboard() {
        InitBiosSegment();
        call_int16 = Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_int16, INT16_Handler, Callback.CB_INT16, "Keyboard");
        Memory.RealSetVec(0x16, Callback.CALLBACK_RealPointer(call_int16));
        call_irq1 = Callback.CALLBACK_Allocate();
        Callback.CALLBACK_Setup(call_irq1, IRQ1_Handler, Callback.CB_IRQ1, Memory.Real2Phys(Bios.BIOS_DEFAULT_IRQ1_LOCATION()), "IRQ 1 Keyboard");
        Memory.RealSetVec(0x09, Bios.BIOS_DEFAULT_IRQ1_LOCATION());
        if (Dosbox.machine == MachineType.MCH_PCJR) {
            call_irq6 = Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_irq6, null, Callback.CB_IRQ6_PCJR, "PCJr kb irq");
            Memory.RealSetVec(0x0e, Callback.CALLBACK_RealPointer(call_irq6));
        }
    }
}
