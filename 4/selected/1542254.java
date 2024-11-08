package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class AddInstruction extends RTypeInstruction {

    public AddInstruction(int reg1, int reg2, int reg3) {
        super("add", reg1, reg2, reg3);
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) + owner.registers.readRegister(reg3);
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new AddInstruction(reg1, reg2, reg3);
    }

    protected String getFunctField() {
        return "100000";
    }
}
