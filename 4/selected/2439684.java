package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class SltIInstruction extends ITypeInstruction {

    public SltIInstruction(int reg1, int reg2, int constant) {
        super("slti", reg1, reg2, constant);
    }

    TraceAction perform() throws MIPSInstructionException {
        int writeValue = owner.registers.readRegister(reg2) < constant ? 1 : 0;
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new SltIInstruction(reg1, reg2, constant);
    }

    protected String getOpCode() {
        return "001010";
    }
}
