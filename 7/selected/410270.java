package org.ghutchis.xtgcore;

import java.math.BigInteger;

/**
 * Galois-field multiplier generator
 * 
 * Creates a multi-cycle Galois field multiplier based on a provided
 * reduction polynomial.  Eliminates redudant terms and creates a
 * balanced-tree input as part of the construction process.
 * 
 * @author hutch
 *
 */
public class TBGFMult extends TBComponent {

    private int partial, mWidth;

    BigInteger poly;

    /**
	 * Generator constructor for 128-bit multiplier.
	 * 
	 * @param name Module name of generator
	 * @param par_width Partial multiply width, must be natural power of 2 and smaller than 128
	 */
    TBGFMult(String name, int par_width) {
        super(name, 5);
        setPort(0, "v_in", 128, 'i');
        setPort(1, "z_in", 128, 'i');
        setPort(2, "b_in", par_width, 'i');
        setPort(3, "v_out", 128, 'o');
        setPort(4, "z_out", 128, 'o');
        partial = par_width;
        mWidth = 128;
        poly = new BigInteger("E1000000000000000000000000000000", 16);
    }

    /**
	 * Generator constructor for arbitrary-width multiplier.
	 * 
	 * @param name Module name of generator
	 * @param par_width Partial multiply width, must be natural power of 2 and smaller than mult_size
	 * @param mult_size Multiplier size, must be natural power of 2
	 */
    public TBGFMult(String name, int par_width, int mult_size) {
        super(name, 5);
        setPort(0, "v_in", mult_size, 'i');
        setPort(1, "z_in", mult_size, 'i');
        setPort(2, "b_in", par_width, 'i');
        setPort(3, "v_out", mult_size, 'o');
        setPort(4, "z_out", mult_size, 'o');
        partial = par_width;
        mWidth = mult_size;
        poly = new BigInteger("E1000000000000000000000000000000", 16);
    }

    public void setPoly(String hexpoly) {
        poly = new BigInteger(hexpoly, 16);
    }

    public void build() {
        TBSymbol v[], z[], b_in[];
        v = getPort(0).getAllBits();
        z = getPort(1).getAllBits();
        b_in = getPort(2).getAllBits();
        for (int r = (partial - 1); r >= 0; r--) loop_mul(mWidth, v, z, b_in[r], poly);
        for (int b = 0; b < mWidth; b++) {
            getPort(3).setBit(b, v[b]);
            getPort(4).setBit(b, z[b]);
        }
    }

    void loop_mul(int mwidth, TBSymbol v[], TBSymbol z[], TBSymbol b_in, BigInteger poly) {
        TBSymbol tmp;
        TBSymbol newv[];
        newv = new TBSymbol[mwidth];
        for (int i = 0; i < mwidth; i = i + 1) {
            tmp = new TBSymOperator(z[i], new TBSymOperator(v[i], b_in, '&'), '^');
            z[i] = tmp;
        }
        for (int i = 0; i < (mwidth - 1); i++) {
            if (poly.testBit(i)) newv[i] = new TBSymOperator(v[0], v[i + 1], '^'); else newv[i] = v[i + 1];
        }
        newv[mwidth - 1] = v[0];
        for (int i = 0; i < mwidth; i++) v[i] = newv[i];
    }

    String getBody() {
        TBSymOperator tmp;
        StringBuffer sb = new StringBuffer();
        for (int b = 0; b < mWidth; b++) {
            tmp = (TBSymOperator) getPort(4).getBit(b);
            tmp.rebalance();
            sb.append("assign " + getPort(3).getName() + "[" + b + "] = " + getPort(3).getBit(b).toString() + ";\n");
            sb.append("assign " + getPort(4).getName() + "[" + b + "] = " + getPort(4).getBit(b).toString() + ";\n");
        }
        return sb.toString();
    }
}
