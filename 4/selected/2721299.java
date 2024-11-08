package edumips64.core.is;

import edumips64.core.*;
import edumips64.utils.*;

/** <pre>
 *       Syntax: SB rt, offset(base)
 *  Description: Stores the least-significant 8 bit of rt in memory 
 *  		 i.e: memory[base+offset] = rt
 * </pre>
 * @author Trubia Massimo, Russo Daniele
 */
class SB extends Storing {

    final String OPCODE_VALUE = "101000";

    public SB() {
        super.OPCODE_VALUE = OPCODE_VALUE;
        this.name = "SB";
    }

    public void MEM() throws IrregularStringOfBitsException, MemoryElementNotFoundException, AddressErrorException {
        try {
            long address = TR[OFFSET_PLUS_BASE].getValue();
            Dinero din = Dinero.getInstance();
            din.Store(Converter.binToHex(Converter.positiveIntToBin(64, address)), 1);
            MemoryElement memEl = memory.getCell((int) address);
            memEl.writeByte(TR[RT_FIELD].readByte(0), (int) (address % 8));
            if (enableForwarding) {
                WB();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void EX() throws IrregularStringOfBitsException, IntegerOverflowException {
    }

    public void WB() throws IrregularStringOfBitsException {
    }
}
