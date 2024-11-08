package machine;

import static arcadeflex.libc.*;
import static arcadeflex.osdepend.*;
import static mame.cpuintrf.*;
import static mame.driverH.*;
import static mame.mame.*;
import static mame.inptport.*;
import static Z80.Z80H.*;
import static Z80.Z80.*;
import static vidhrdw.generic.*;
import static mame.osdependH.*;

public class digdug {

    public static CharPtr digdug_sharedram = new CharPtr();

    static int interrupt_enable_1;

    static int interrupt_enable_2;

    static int interrupt_enable_3;

    public static int digdug_hiscoreloaded;

    public static InitMachinePtr digdig_init_machine = new InitMachinePtr() {

        public void handler() {
            cpu_halt(1, 0);
            cpu_halt(2, 0);
        }
    };

    public static ReadHandlerPtr digdug_reset_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            digdug_hiscoreloaded = 0;
            return RAM[offset];
        }
    };

    public static ReadHandlerPtr digdug_sharedram_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            return digdug_sharedram.read(offset);
        }
    };

    public static WriteHandlerPtr digdug_sharedram_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            if (offset < 0x400) dirtybuffer[offset] = 1;
            if (offset == 0x1b3d && data == 0 && cpu_getpc() == 0x1df1 && Z80_ICount > 50) Z80_ICount = 50;
            if (offset == 0x1b3c && data == 0 && cpu_getpc() == 0x0316 && Z80_ICount > 50) Z80_ICount = 50;
            digdug_sharedram.write(offset, data);
        }
    };

    /***************************************************************************

         Emulate the custom IO chip.

         In the real digdug machine, the chip would cause an NMI on CPU #1 to ask
         for data to be transferred. We don't bother causing the NMI, we just look
         into the CPU register to see where the data has to be read/written to, and
         emulate the behaviour of the NMI interrupt.

        ***************************************************************************/
    static int mode;

    static int credits;

    static int coin;

    static int start1;

    static int start2;

    static int fire;

    public static WriteHandlerPtr digdug_customio_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            Z80_Regs regs = new Z80_Regs();
            switch(data) {
                case 0x10:
                    return;
                case 0x71:
                    {
                        int in;
                        if (osd_key_pressed(OSD_KEY_3)) {
                            if (coin == 0 && credits < 99) credits++;
                            coin = 1;
                        } else coin = 0;
                        if (osd_key_pressed(OSD_KEY_1)) {
                            if (start1 == 0 && credits >= 1) credits--;
                            start1 = 1;
                        } else start1 = 0;
                        if (osd_key_pressed(OSD_KEY_2)) {
                            if (start2 == 0 && credits >= 2) credits -= 2;
                            start2 = 1;
                        } else start2 = 0;
                        in = readinputport(2);
                        if ((in & 0x20) == 0) {
                            if (fire == 0) {
                                in &= ~0x10;
                                fire = 1;
                            }
                        } else fire = 0;
                        if ((in & 0x01) == 0) in = (in & ~0x0f) | 0x00; else if ((in & 0x02) == 0) in = (in & ~0x0f) | 0x02; else if ((in & 0x04) == 0) in = (in & ~0x0f) | 0x04; else if ((in & 0x08) == 0) in = (in & ~0x0f) | 0x06; else in = (in & ~0x0f) | 0x08;
                        if (mode != 0) cpu_writemem(0x7000, 0x80); else cpu_writemem(0x7000, (credits / 10) * 16 + credits % 10);
                        cpu_writemem(0x7000 + 1, in);
                        cpu_writemem(0x7000 + 2, 0xff);
                    }
                    break;
                case 0xb1:
                    credits = 0;
                    cpu_writemem(0x7000, 0);
                    cpu_writemem(0x7000 + 1, 0);
                    cpu_writemem(0x7000 + 2, 0);
                    break;
                case 0xa1:
                    mode = 1;
                    break;
                case 0xc1:
                case 0xe1:
                    mode = 0;
                    break;
                case 0xd2:
                    cpu_writemem(0x7000, readinputport(0));
                    cpu_writemem(0x7001, readinputport(1));
                    break;
                default:
                    if (errorlog != null) fprintf(errorlog, "%04x: warning: unknown custom IO command %02x\n", new Object[] { Integer.valueOf(cpu_getpc()), Integer.valueOf(data) });
                    break;
            }
            Z80_GetRegs(regs);
            while (regs.BC2 > 1) {
                cpu_writemem(regs.DE2, cpu_readmem(regs.HL2));
                regs.DE2 = (regs.DE2 + 1 & 0xFFFF);
                regs.HL2 = (regs.HL2 + 1 & 0xFFFF);
                regs.BC2 = (regs.BC2 - 1 & 0xFFFF);
            }
            regs.SP = (regs.SP - 2 & 0xFFFF);
            RAM[regs.SP] = (char) (regs.PC & 0xFF);
            RAM[(regs.SP + 1 & 0xFFFF)] = (char) (regs.PC >> 8 & 0xFF);
            regs.PC = 0x0066;
            Z80_SetRegs(regs);
        }
    };

    public static ReadHandlerPtr digdug_customio_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            return 0x10;
        }
    };

    public static WriteHandlerPtr digdug_halt_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            cpu_halt(1, data);
            cpu_halt(2, data);
        }
    };

    public static WriteHandlerPtr digdug_interrupt_enable_1_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_1 = data;
        }
    };

    public static InterruptPtr digdug_interrupt_1 = new InterruptPtr() {

        public int handler() {
            if (interrupt_enable_1 != 0) return 0xff; else return Z80_IGNORE_INT;
        }
    };

    public static WriteHandlerPtr digdug_interrupt_enable_2_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_2 = data;
        }
    };

    public static InterruptPtr digdug_interrupt_2 = new InterruptPtr() {

        public int handler() {
            if (interrupt_enable_2 != 0) return 0xff; else return Z80_IGNORE_INT;
        }
    };

    public static WriteHandlerPtr digdug_interrupt_enable_3_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_3 = data;
        }
    };

    public static InterruptPtr digdug_interrupt_3 = new InterruptPtr() {

        public int handler() {
            if (interrupt_enable_3 != 0) return Z80_IGNORE_INT; else return Z80_NMI_INT;
        }
    };
}
