package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class SltInstruction extends RTypeInstruction {

    public SltInstruction(int reg1, int reg2, int reg3) {
        super("slt", reg1, reg2, reg3);
    }

    TraceAction perform() throws MIPSInstructionException {
        int writeValue = owner.registers.readRegister(reg2) < owner.registers.readRegister(reg3) ? 1 : 0;
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new SltInstruction(reg1, reg2, reg3);
    }

    protected String getFunctField() {
        return "101010";
    }
}
