package edu.kds.asm.asmProgram;

import edu.kds.asm.asmProgram.trace.TraceAction;

public class AddIInstruction extends ITypeInstruction {

    public AddIInstruction(int reg1, int reg2, int constant) {
        super("addi", reg1, reg2, constant);
    }

    TraceAction perform() throws MIPSInstructionException {
        long writeValue = owner.registers.readRegister(reg2) + constant;
        TraceAction a = owner.registers.writeRegister(reg1, writeValue);
        owner.incPC();
        return a;
    }

    public String getOpCode() {
        return "001000";
    }

    public MIPSInstruction clone() {
        return new AddIInstruction(reg1, reg2, constant);
    }

    public static void main(String[] args) {
        AddIInstruction i = new AddIInstruction(1, 2, 3);
        System.out.println("Machine-code: " + i.getMachineCode());
        try {
            String expected = "00100000010000010000000000000011";
            System.out.println("Expecting:    " + Integer.parseInt(expected, 2));
        } catch (NumberFormatException exc) {
        }
    }
}
