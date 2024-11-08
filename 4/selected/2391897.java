package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *         Syntax: LB rt, offset(base)
 *    Description: rt = memory[base+offset]
 *                 To load a byte from memory as a signed value
 * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class LB extends Loading {

    final String OPCODE_VALUE = "100000";

    public LB() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "LB";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        long address = TR[OFFSET_PLUS_BASE].getValue();
        Dinero din = Dinero.getInstance();
        din.Load(Converter.binToHex(Converter.positiveIntToBin(64, address)), 1);
        MemoryElement memEl = memory.getCell((int) address);
        TR[LMD_REGISTER].writeByte(memEl.readByte((int) (address % 8)));
        if (enableForwarding) {
            doWB();
        }
    }
}
