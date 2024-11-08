package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *       Syntax: SH rt, offset(base)
 *  Description: Stores the halfword in rt to memory
 *   		 i.e: memory[base+offset] = rt
 *               
 *  
 * </pre>
 * @author IS Group
 */
class SH extends Storing {

    final String OPCODE_VALUE = "101001";

    public SH() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "SH";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException {
        try {
            long address = TR[OFFSET_PLUS_BASE].getValue();
            Dinero din = Dinero.getInstance();
            din.Store(Converter.binToHex(Converter.positiveIntToBin(64, address)), 2);
            MemoryElement memEl = memory.getCell((int) address);
            memEl.writeHalf(TR[RT_FIELD].readHalf(0), (int) (address % 8));
            if (enableForwarding) {
                WB();
            }
        } catch (NotAlingException er) {
            throw new AddressErrorException();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
