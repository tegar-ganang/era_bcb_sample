package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *  Format:        LW rt, offset(base)
 *  Description:   rt = memory[base+offset]
 *                 To load a word from memory as a signed value
  * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class LW extends Loading {

    final String OPCODE_VALUE = "100011";

    public LW() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "LW";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        long address = TR[OFFSET_PLUS_BASE].getValue();
        Dinero din = Dinero.getInstance();
        din.Load(Converter.binToHex(Converter.positiveIntToBin(64, address)), 4);
        MemoryElement memEl = memory.getCell((int) address);
        try {
            TR[LMD_REGISTER].writeWord(memEl.readWord((int) (address % 8)));
            if (enableForwarding) {
                doWB();
            }
        } catch (NotAlingException er) {
            throw new AddressErrorException();
        }
    }
}
