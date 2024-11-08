package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;
import edu.kds.misc.BinaryNumber;

public class AndInstruction extends MIPSInstruction {

    private final int reg1, reg2, reg3;

    public AndInstruction(int reg1, int reg2, int reg3) {
        super("and");
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.reg3 = reg3;
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) & owner.registers.readRegister(reg3);
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public long getMachineCode() {
        String op = "000000";
        String rt = BinaryNumber.toBinString(reg3, 5);
        String rs = BinaryNumber.toBinString(reg2, 5);
        String rd = BinaryNumber.toBinString(reg1, 5);
        String shamt = "00000";
        String funct = "100100";
        try {
            return Integer.parseInt(op + rs + rt + rd + shamt + funct, 2);
        } catch (NumberFormatException exc) {
            return 0;
        }
    }

    public MIPSInstruction clone() {
        return new AndInstruction(reg1, reg2, reg3);
    }

    public String toString() {
        return name + " " + MIPSRegisters.registerName(reg1) + ", " + MIPSRegisters.registerName(reg2) + ", " + MIPSRegisters.registerName(reg3);
    }
}
