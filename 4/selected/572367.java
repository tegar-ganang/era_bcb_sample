package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *  Syntax:       LHU rt, offset(base)
 *  Description:  rt = memory[base+offset]    
 *                To load a halfword from memory as an unsigned value  
 * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class LHU extends Loading {

    final String OPCODE_VALUE = "100101";

    public LHU() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "LHU";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        long address = TR[OFFSET_PLUS_BASE].getValue();
        Dinero din = Dinero.getInstance();
        din.Load(Converter.binToHex(Converter.positiveIntToBin(64, address)), 2);
        MemoryElement memEl = memory.getCell((int) address);
        try {
            TR[LMD_REGISTER].writeHalfUnsigned(memEl.readHalfUnsigned((int) (address % 8)));
            if (enableForwarding) {
                doWB();
            }
        } catch (NotAlingException er) {
            throw new AddressErrorException();
        }
    }
}
