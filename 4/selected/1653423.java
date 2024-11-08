package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *       Syntax: SW rt, offset(base)
 *  Description: Stores in memory a byte from memory i.e rt = memory[base+offset]
 *               adding the signed offset to base to form the final address.
 * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class SW extends Storing {

    final String OPCODE_VALUE = "101011";

    public SW() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "SW";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException, IrregularWriteOperationException {
        try {
            long address = TR[OFFSET_PLUS_BASE].getValue();
            Dinero din = Dinero.getInstance();
            din.Store(Converter.binToHex(Converter.positiveIntToBin(64, address)), 4);
            MemoryElement memEl = memory.getCell((int) address);
            memEl.writeWord(TR[RT_FIELD].readWord(0), (int) (address % 8));
        } catch (NotAlingException er) {
            throw new AddressErrorException();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void EX() throws IrregularStringOfBitsException, IntegerOverflowException {
    }

    public void WB() throws IrregularStringOfBitsException {
    }
}
