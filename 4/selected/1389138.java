package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class AndIInstruction extends ITypeInstruction {

    public AndIInstruction(int reg1, int reg2, int constant) {
        super("andi", reg1, reg2, constant);
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) & constant;
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new AndIInstruction(reg1, reg2, constant);
    }

    public String getOpCode() {
        return "001100";
    }
}
