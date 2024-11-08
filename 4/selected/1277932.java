package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *  Syntax:        LH rt, offset(base)
 *  Description:   rt = memory[base+offset]
 *  Purpose:       To load a halfword from memory as a signed value
 * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class LH extends Loading {

    final String OPCODE_VALUE = "100001";

    public LH() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "LH";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        long address = TR[OFFSET_PLUS_BASE].getValue();
        Dinero din = Dinero.getInstance();
        din.Load(Converter.binToHex(Converter.positiveIntToBin(64, address)), 2);
        MemoryElement memEl = memory.getCell((int) address);
        try {
            TR[LMD_REGISTER].writeHalf(memEl.readHalf((int) (address % 8)));
            if (enableForwarding) {
                doWB();
            }
        } catch (NotAlingException er) {
            throw new AddressErrorException();
        }
    }
}
