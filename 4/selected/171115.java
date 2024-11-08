package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class OrIInstruction extends ITypeInstruction {

    public OrIInstruction(int reg1, int reg2, int constant) {
        super("ori", reg1, reg2, constant);
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) | constant;
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public MIPSInstruction clone() {
        return new OrIInstruction(reg1, reg2, constant);
    }

    protected String getOpCode() {
        return "001101";
    }
}
