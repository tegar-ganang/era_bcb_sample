package machine;

import static arcadeflex.libc.*;
import static arcadeflex.osdepend.*;
import static mame.driverH.*;
import static mame.common.*;
import static mame.commonH.*;
import static mame.mame.*;
import static mame.osdependH.*;
import static mame.cpuintrf.*;
import static vidhrdw.generic.*;
import static Z80.Z80H.*;
import static Z80.Z80.*;
import static sndhrdw.bosco.*;
import static mame.inptport.*;

public class bosco {

    public static CharPtr bosco_sharedram = new CharPtr();

    static int interrupt_enable_1;

    static int interrupt_enable_2;

    static int interrupt_enable_3;

    static int do_nmi;

    public static char bosco_hiscoreloaded;

    public static int HiScore;

    public static InitMachinePtr bosco_init_machine = new InitMachinePtr() {

        public void handler() {
            cpu_halt(1, 0);
            cpu_halt(2, 0);
            Machine.memory_region[0][0x8c00] = 1;
            Machine.memory_region[0][0x8c01] = 1;
        }
    };

    public static ReadHandlerPtr bosco_reset_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            bosco_hiscoreloaded = 0;
            return RAM[offset];
        }
    };

    public static ReadHandlerPtr bosco_sharedram_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            return bosco_sharedram.read(offset);
        }
    };

    public static WriteHandlerPtr bosco_sharedram_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            bosco_sharedram.write(offset, data);
        }
    };

    public static ReadHandlerPtr bosco_dsw_r = new ReadHandlerPtr() {

        public int handler(int offset) {
            int bit0, bit1;
            bit0 = (input_port_0_r.handler(0) >> offset) & 1;
            bit1 = (input_port_1_r.handler(0) >> offset) & 1;
            return bit0 | (bit1 << 1);
        }
    };

    /***************************************************************************

         Emulate the custom IO chip.

         In the real bosco machine, the chip would cause an NMI on CPU #1 to ask
         for data to be transferred. We don't bother causing the NMI, we just look
         into the CPU register to see where the data has to be read/written to, and
         emulate the behaviour of the NMI interrupt.

        ***************************************************************************/
    static int mode, credits;

    static int Score, Score1, Score2;

    static int coin, start1, start2, fire;

    public static WriteHandlerPtr bosco_customio_w_1 = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            Z80_Regs regs = new Z80_Regs();
            switch(data) {
                case 0x10:
                    return;
                case 0x48:
                    Z80_GetRegs(regs);
                    switch(regs.HL2) {
                        case 0x16F0:
                            if (Machine.samples != null) {
                                if (Machine.samples.sample[0] != null) {
                                    osd_play_sample(7, Machine.samples.sample[0].data, Machine.samples.sample[0].length, Machine.samples.sample[0].smpfreq, Machine.samples.sample[0].volume, 0);
                                }
                            }
                            break;
                        case 0x16F2:
                            if (Machine.samples != null) {
                                if (Machine.samples.sample[1] != null) {
                                    osd_play_sample(6, Machine.samples.sample[1].data, Machine.samples.sample[1].length, Machine.samples.sample[1].smpfreq, Machine.samples.sample[1].volume, 0);
                                }
                            }
                            break;
                        case 0x16F4:
                            if (Machine.samples != null) {
                                if (Machine.samples.sample[2] != null) {
                                    osd_play_sample(5, Machine.samples.sample[2].data, Machine.samples.sample[2].length, Machine.samples.sample[2].smpfreq, Machine.samples.sample[2].volume, 0);
                                }
                            }
                            break;
                    }
                    break;
                case 0x64:
                    Z80_GetRegs(regs);
                    switch(cpu_readmem(regs.HL2)) {
                        case 0x01:
                            break;
                        case 0x10:
                            break;
                        case 0x40:
                            break;
                        case 0x60:
                            Score2 = Score;
                            Score = Score1;
                            break;
                        case 0x68:
                            Score1 = Score;
                            Score = Score2;
                            break;
                        case 0x80:
                            break;
                        case 0x81:
                            Score += 10;
                            break;
                        case 0x83:
                            Score += 20;
                            break;
                        case 0x87:
                            Score += 50;
                            break;
                        case 0x88:
                            Score += 60;
                            break;
                        case 0x8D:
                            Score += 200;
                            break;
                        case 0x95:
                            Score += 300;
                            break;
                        case 0x96:
                            Score += 400;
                            break;
                        case 0xA2:
                            Score += 1500;
                            break;
                        case 0xB7:
                            Score += 500;
                            break;
                        case 0xC3:
                            break;
                        default:
                            if (errorlog != null) fprintf(errorlog, "unknown score: %02x\n", new Object[] { Integer.valueOf(cpu_readmem(regs.HL2)) });
                            break;
                    }
                    break;
                case 0x71:
                    {
                        int in, dir;
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
                        dir = 8;
                        if ((in & 0x01) == 0) {
                            if ((in & 0x02) == 0) dir = 1; else if ((in & 0x08) == 0) dir = 7; else dir = 0;
                        } else if ((in & 0x04) == 0) {
                            if ((in & 0x02) == 0) dir = 3; else if ((in & 0x08) == 0) dir = 5; else dir = 4;
                        } else if ((in & 0x02) == 0) dir = 2; else if ((in & 0x08) == 0) dir = 6;
                        dir |= 0x10;
                        if ((in & 0x10) == 0) {
                            if (fire == 0) dir &= ~0x10; else fire = 1;
                        } else {
                            fire = 0;
                        }
                        if (mode != 0) cpu_writemem(0x7000, 0x80); else cpu_writemem(0x7000, (credits / 10) * 16 + credits % 10);
                        cpu_writemem(0x7000 + 1, dir);
                        cpu_writemem(0x7000 + 2, dir);
                    }
                    break;
                case 0x61:
                    mode = 1;
                    break;
                case 0x94:
                    {
                        int tmp0, tmp1, tmp2;
                        tmp0 = Score;
                        tmp1 = tmp0 / 10000000;
                        tmp0 -= tmp1 * 10000000;
                        tmp2 = tmp0 / 1000000;
                        tmp0 -= tmp2 * 1000000;
                        if (Score >= HiScore) {
                            cpu_writemem(0x7000, ((tmp1 * 16) + tmp2) | 0x80);
                            HiScore = Score;
                        } else {
                            cpu_writemem(0x7000, (tmp1 * 16) + tmp2);
                        }
                        tmp1 = tmp0 / 100000;
                        tmp0 -= tmp1 * 100000;
                        tmp2 = tmp0 / 10000;
                        tmp0 -= tmp2 * 10000;
                        cpu_writemem(0x7000 + 1, (tmp1 * 16) + tmp2);
                        tmp1 = tmp0 / 1000;
                        tmp0 -= tmp1 * 1000;
                        tmp2 = tmp0 / 100;
                        tmp0 -= tmp2 * 100;
                        cpu_writemem(0x7000 + 2, (tmp1 * 16) + tmp2);
                        tmp1 = tmp0 / 10;
                        tmp0 -= tmp1 * 10;
                        tmp2 = tmp0;
                        cpu_writemem(0x7000 + 3, (tmp1 * 16) + tmp2);
                    }
                    break;
                case 0xC1:
                    Score = 0;
                    Score1 = 0;
                    Score2 = 0;
                    break;
                case 0xC8:
                    break;
                case 0x84:
                    break;
                case 0x91:
                    cpu_writemem(0x7000, 0);
                    cpu_writemem(0x7000 + 1, 0);
                    cpu_writemem(0x7000 + 2, 0);
                    mode = 0;
                    break;
                case 0xa1:
                    mode = 1;
                    break;
                default:
                    if (errorlog != null) fprintf(errorlog, "%04x: warning: unknown custom IO command %02x\n", new Object[] { Integer.valueOf(cpu_getpc()), Integer.valueOf(data) });
                    break;
            }
            Z80_GetRegs(regs);
            while (regs.BC2 > 0) {
                cpu_writemem(regs.DE2, cpu_readmem(regs.HL2));
                regs.DE2 = (regs.DE2 + 1 & 0xFFFF);
                regs.HL2 = (regs.HL2 + 1 & 0xFFFF);
                regs.BC2 = (regs.BC2 - 1 & 0xFFFF);
            }
            Z80_SetRegs(regs);
        }
    };

    public static WriteHandlerPtr bosco_customio_w_2 = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            Z80_Regs regs = new Z80_Regs();
            switch(data) {
                case 0x10:
                    return;
                case 0x34:
                    break;
                case 0x81:
                    break;
                case 0x82:
                    Z80_GetRegs(regs);
                    switch(regs.HL2) {
                        case 0x1BEE:
                            bosco_sample_play(0x0020 * 2, 0x08D7 * 2);
                            break;
                        case 0x1BF1:
                            bosco_sample_play(0x8F7 * 2, 0x0906 * 2);
                            break;
                        case 0x1BF4:
                            bosco_sample_play(0x11FD * 2, 0x07DD * 2);
                            break;
                        case 0x1BF7:
                            bosco_sample_play(0x19DA * 2, 0x07DE * 2);
                            break;
                        case 0x1BFA:
                            bosco_sample_play(0x21B8 * 2, 0x079F * 2);
                            break;
                    }
                    break;
                case 0x91:
                    cpu_writemem(0x9000, 0);
                    cpu_writemem(0x9000 + 1, 0);
                    cpu_writemem(0x9000 + 2, 0);
                    cpu_writemem(0x9000 + 3, 0);
                    break;
                case 0xA1:
                    break;
            }
            Z80_GetRegs(regs);
            while (regs.BC2 > 0) {
                cpu_writemem(regs.DE2, cpu_readmem(regs.HL2));
                regs.DE2 = (regs.DE2 + 1 & 0xFFFF);
                regs.HL2 = (regs.HL2 + 1 & 0xFFFF);
                regs.BC2 = (regs.BC2 - 1 & 0xFFFF);
            }
            Z80_SetRegs(regs);
        }
    };

    public static ReadHandlerPtr bosco_customio_r_1 = new ReadHandlerPtr() {

        public int handler(int offset) {
            return 0x10;
        }
    };

    public static ReadHandlerPtr bosco_customio_r_2 = new ReadHandlerPtr() {

        public int handler(int offset) {
            return 0x10;
        }
    };

    public static WriteHandlerPtr bosco_halt_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            cpu_halt(1, data);
            cpu_halt(2, data);
        }
    };

    public static WriteHandlerPtr bosco_interrupt_enable_1_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_1 = data;
        }
    };

    public static InterruptPtr bosco_interrupt_1 = new InterruptPtr() {

        public int handler() {
            if (do_nmi != 0) {
                do_nmi = 0;
                return Z80_NMI_INT;
            }
            if (interrupt_enable_1 != 0) return 0xff; else return Z80_IGNORE_INT;
        }
    };

    public static WriteHandlerPtr bosco_interrupt_enable_2_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_2 = data;
        }
    };

    public static InterruptPtr bosco_interrupt_2 = new InterruptPtr() {

        public int handler() {
            if (interrupt_enable_2 != 0) return 0xff; else return Z80_IGNORE_INT;
        }
    };

    public static WriteHandlerPtr bosco_interrupt_enable_3_w = new WriteHandlerPtr() {

        public void handler(int offset, int data) {
            interrupt_enable_3 = data;
        }
    };

    public static InterruptPtr bosco_interrupt_3 = new InterruptPtr() {

        public int handler() {
            if (interrupt_enable_3 != 0) return Z80_IGNORE_INT; else return Z80_NMI_INT;
        }
    };
}
