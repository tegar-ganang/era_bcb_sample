package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *         Syntax: LBU rt, offset(base)
 *    Description: To load a byte from memory as an unsigned value
 *                 rt = memory[base+offset]
  * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class LBU extends Loading {

    final String OPCODE_VALUE = "100100";

    public LBU() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "LBU";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        long address = TR[OFFSET_PLUS_BASE].getValue();
        Dinero din = Dinero.getInstance();
        din.Load(Converter.binToHex(Converter.positiveIntToBin(64, address)), 1);
        MemoryElement memEl = memory.getCell((int) address);
        int read = memEl.readByteUnsigned((int) (address % 8));
        edumips64.Main.logger.debug("LBU: read from address " + address + " the value " + read);
        TR[LMD_REGISTER].writeByteUnsigned(read);
        if (enableForwarding) {
            doWB();
        }
    }
}
