package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class OrInstruction extends RTypeInstruction {

    public OrInstruction(int reg1, int reg2, int reg3) {
        super("or", reg1, reg2, reg3);
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) | owner.registers.readRegister(reg3);
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new OrInstruction(reg1, reg2, reg3);
    }

    protected String getFunctField() {
        return "100101";
    }
}
