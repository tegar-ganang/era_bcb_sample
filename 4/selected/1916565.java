package jdos.cpu.core_dynamic;

import jdos.cpu.CPU_Regs;
import jdos.cpu.Flags;
import jdos.cpu.Instructions;
import jdos.cpu.core_share.Constants;
import jdos.hardware.Memory;

public class Grp3 extends Helper {

    public static class Testb_reg extends Op {

        short val;

        CPU_Regs.Reg earb;

        public Testb_reg(int rm) {
            this.val = decode_fetchb();
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.TESTB(val, earb.get8());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class Testb_mem extends Op {

        short val;

        EaaBase get_eaa;

        public Testb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val = decode_fetchb();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTB(val, Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEb_reg extends Op {

        CPU_Regs.Reg earb;

        public NotEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            earb.set8((byte) ~earb.get8());
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEb_mem extends Op {

        EaaBase get_eaa;

        public NotEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writeb(eaa, ~Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEb_reg extends Op {

        CPU_Regs.Reg earb;

        public NegEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Flags.lflags.type = Flags.t_NEGb;
            Flags.lf_var1b(earb.get8());
            Flags.lf_resb(0 - Flags.lf_var1b());
            earb.set8(Flags.lf_resb());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEb_mem extends Op {

        EaaBase get_eaa;

        public NegEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            Flags.lflags.type = Flags.t_NEGb;
            int eaa = get_eaa.call();
            Flags.lf_var1b(Memory.mem_readb(eaa));
            Flags.lf_resb(0 - Flags.lf_var1b());
            Memory.mem_writeb(eaa, Flags.lf_resb());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAlEb_reg extends Op {

        CPU_Regs.Reg earb;

        public MulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.MULB(earb.get8());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAlEb_mem extends Op {

        EaaBase get_eaa;

        public MulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULB(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAlEb_reg extends Op {

        CPU_Regs.Reg earb;

        public IMulAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            Instructions.IMULB(earb.get8());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAlEb_mem extends Op {

        EaaBase get_eaa;

        public IMulAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULB(Memory.mem_readb(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAlEb_reg extends Op {

        CPU_Regs.Reg earb;

        public DivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            return Instructions.DIVBr(this, earb.get8());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAlEb_mem extends Op {

        EaaBase get_eaa;

        public DivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.DIVBr(this, Memory.mem_readb(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAlEb_reg extends Op {

        CPU_Regs.Reg earb;

        public IDivAlEb_reg(int rm) {
            earb = Mod.eb(rm);
        }

        public int call() {
            return Instructions.IDIVBr(this, earb.get8());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAlEb_mem extends Op {

        EaaBase get_eaa;

        public IDivAlEb_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.IDIVBr(this, Memory.mem_readb(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class Testw_reg extends Op {

        int val;

        CPU_Regs.Reg earw;

        public Testw_reg(int rm) {
            this.val = decode_fetchw();
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.TESTW(val, earw.word());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class Testw_mem extends Op {

        int val;

        EaaBase get_eaa;

        public Testw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val = decode_fetchw();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTW(val, Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEw_reg extends Op {

        CPU_Regs.Reg earw;

        public NotEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            earw.word(~earw.word());
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEw_mem extends Op {

        EaaBase get_eaa;

        public NotEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writew(eaa, ~Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEw_reg extends Op {

        CPU_Regs.Reg earw;

        public NegEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Flags.lflags.type = Flags.t_NEGw;
            Flags.lf_var1w(earw.word());
            Flags.lf_resw(0 - Flags.lf_var1w());
            earw.word(Flags.lf_resw());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEw_mem extends Op {

        EaaBase get_eaa;

        public NegEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            Flags.lflags.type = Flags.t_NEGw;
            int eaa = get_eaa.call();
            Flags.lf_var1w(Memory.mem_readw(eaa));
            Flags.lf_resw(0 - Flags.lf_var1w());
            Memory.mem_writew(eaa, Flags.lf_resw());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAxEw_reg extends Op {

        CPU_Regs.Reg earw;

        public MulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.MULW(earw.word());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAxEw_mem extends Op {

        EaaBase get_eaa;

        public MulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULW(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAxEw_reg extends Op {

        CPU_Regs.Reg earw;

        public IMulAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            Instructions.IMULW(earw.word());
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAxEw_mem extends Op {

        EaaBase get_eaa;

        public IMulAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULW(Memory.mem_readw(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAxEw_reg extends Op {

        CPU_Regs.Reg earw;

        public DivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            return Instructions.DIVWr(this, earw.word());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAxEw_mem extends Op {

        EaaBase get_eaa;

        public DivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.DIVWr(this, Memory.mem_readw(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAxEw_reg extends Op {

        CPU_Regs.Reg earw;

        public IDivAxEw_reg(int rm) {
            earw = Mod.ew(rm);
        }

        public int call() {
            return Instructions.IDIVWr(this, earw.word());
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAxEw_mem extends Op {

        EaaBase get_eaa;

        public IDivAxEw_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.IDIVWr(this, Memory.mem_readw(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class Testd_reg extends Op {

        int val;

        CPU_Regs.Reg eard;

        public Testd_reg(int rm) {
            val = decode_fetchd();
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.TESTD(val, eard.dword);
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class Testd_mem extends Op {

        int val;

        EaaBase get_eaa;

        public Testd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
            this.val = decode_fetchd();
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.TESTD(val, Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEd_reg extends Op {

        CPU_Regs.Reg eard;

        public NotEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword = ~eard.dword;
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NotEd_mem extends Op {

        EaaBase get_eaa;

        public NotEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, ~Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEd_reg extends Op {

        CPU_Regs.Reg eard;

        public NegEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            eard.dword = Instructions.Negd(eard.dword);
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class NegEd_mem extends Op {

        EaaBase get_eaa;

        public NegEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Memory.mem_writed(eaa, Instructions.Negd(Memory.mem_readd(eaa)));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.AF | CPU_Regs.ZF | CPU_Regs.SF | CPU_Regs.OF | CPU_Regs.PF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAxEd_reg extends Op {

        CPU_Regs.Reg eard;

        public MulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.MULD(eard.dword);
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class MulAxEd_mem extends Op {

        EaaBase get_eaa;

        public MulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.MULD(Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAxEd_reg extends Op {

        CPU_Regs.Reg eard;

        public IMulAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            Instructions.IMULD(eard.dword);
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IMulAxEd_mem extends Op {

        EaaBase get_eaa;

        public IMulAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            Instructions.IMULD(Memory.mem_readd(eaa));
            return Constants.BR_Normal;
        }

        public int sets() {
            return CPU_Regs.CF | CPU_Regs.OF;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAxEd_reg extends Op {

        CPU_Regs.Reg eard;

        public DivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            return Instructions.DIVDr(this, eard.dword);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class DivAxEd_mem extends Op {

        EaaBase get_eaa;

        public DivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.DIVDr(this, Memory.mem_readd(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAxEd_reg extends Op {

        CPU_Regs.Reg eard;

        public IDivAxEd_reg(int rm) {
            eard = Mod.ed(rm);
        }

        public int call() {
            return Instructions.IDIVDr(this, eard.dword);
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }

    public static class IDivAxEd_mem extends Op {

        EaaBase get_eaa;

        public IDivAxEd_mem(int rm) {
            this.get_eaa = Mod.getEaa(rm);
        }

        public int call() {
            int eaa = get_eaa.call();
            return Instructions.IDIVDr(this, Memory.mem_readd(eaa));
        }

        public int sets() {
            return 0;
        }

        public int gets() {
            return 0;
        }
    }
}
