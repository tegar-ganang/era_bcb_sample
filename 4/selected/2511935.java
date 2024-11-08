package jniosemu.instruction.emulator;

import jniosemu.emulator.Emulator;
import jniosemu.emulator.EmulatorException;

public class SraInstruction extends RTypeInstruction {

    public SraInstruction(int opCode) {
        super(opCode);
    }

    public void run(Emulator em) throws EmulatorException {
        int vB = em.readRegister(this.rB) & 0xF;
        em.writeRegister(this.rC, em.readRegister(this.rA) >> vB);
    }
}