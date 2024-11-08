package conversion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import util.BitReader;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;

public class PBMToBDDConverter {

    public static final byte CODING_1_DIM = 0;

    public static final byte CODING_2_DIM_X_Y = 1;

    public static final byte CODING_2_DIM_X_Y_ALTERNATE = 2;

    boolean[] ReadPBM(String filename) throws IOException {
        BufferedReader fr = new BufferedReader(new FileReader(filename));
        System.out.println("ReadPBM : " + filename);
        String magicNumber = fr.readLine();
        if (!magicNumber.equalsIgnoreCase("P4")) {
            throw new IOException("Something wrong with PBM file : magic number");
        }
        String dimString = fr.readLine();
        StringTokenizer st = new StringTokenizer(dimString);
        if (st.countTokens() != 2) {
            throw new IOException("Something wrong with PBM file : Second line doesnt have 2 tokens");
        }
        width = Integer.parseInt(st.nextToken());
        height = Integer.parseInt(st.nextToken());
        System.out.println("PBM image " + width + " x " + height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int byteCount = 0;
        while (fr.ready()) {
            out.write(fr.read());
            byteCount++;
        }
        if (byteCount != width * height / 8) {
            throw new IOException("Read incorrect number of bytes!");
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BitReader br = new BitReader(in);
        boolean[] bits = new boolean[byteCount * 8];
        for (int i = 0; i < bits.length; i++) {
            if (i % width == 0) {
                if (width < 50) System.out.println("");
            }
            int bit = br.readBit();
            if (bit < 0 || bit > 1) throw new IOException("Failure reading from in memory buffer"); else if (bit == 1) {
                if (width < 50) System.out.print(1);
                bits[i] = true;
            } else {
                if (width < 50) System.out.print(0);
                bits[i] = false;
            }
        }
        if (width < 50) System.out.println("");
        return bits;
    }

    void BDDToPBM(BDD imageBDD, String fname, byte coding) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(fname));
    }

    int width, height;

    BDD Convert(String filename, byte coding, boolean useGray) throws IOException {
        boolean[] bits = ReadPBM(filename);
        if (coding == CODING_1_DIM) {
            return FileToBDD.BitStringToBDD(bits);
        } else if (coding == CODING_2_DIM_X_Y) {
            return CodeXY(bits, useGray);
        } else if (coding == CODING_2_DIM_X_Y_ALTERNATE) {
            return null;
        } else return null;
    }

    BDD CodeXY(boolean[] bits, boolean useGray) {
        assert (bits.length == width * height);
        BDDFactory f = JFactory.init(2000000, 1000);
        int xbits = util.Util.log2Ceil(width);
        int ybits = util.Util.log2Ceil(height);
        System.out.println("Using " + xbits + " x vars " + ybits + " y vars");
        if (useGray) System.out.println("Using Gray encoding");
        f.setVarNum(xbits + ybits);
        int[] xvars = new int[xbits];
        int[] yvars = new int[ybits];
        int[] allvars = new int[xbits + ybits];
        for (int i = 0; i < xbits; i++) xvars[i] = i + ybits;
        for (int i = 0; i < ybits; i++) yvars[i] = i;
        for (int i = 0; i < ybits + xbits; i++) allvars[i] = i;
        BDD bdd = f.zero();
        for (int y = 0; y < height; y++) {
            BDD temp = f.one();
            if (useGray) {
                temp.andWith(buildVector(f, inverseGray(y), yvars, true));
            } else {
                temp.andWith(buildVector(f, y, yvars, true));
            }
            for (int x = 0; x < width; x++) {
                if (bits[y * width + x]) {
                    System.out.print("(" + x + " , " + y + ") " + (y * width + x) + " \n");
                    BDD temp2 = temp.id();
                    if (useGray) {
                        temp2.andWith(buildVector(f, inverseGray(x), xvars, true));
                    } else {
                        temp2.andWith(buildVector(f, x, xvars, true));
                    }
                    bdd.orWith(temp2);
                }
            }
        }
        System.out.println("Coded " + bits.length + " bits with " + bdd.nodeCount() + " nodes");
        return bdd;
    }

    public static int invertBits(int val, int len) {
        int nval = 0;
        for (int i = 0; i < len; i++) {
            if (((val >> i) & 1) == 1) {
                nval |= (1 << (len - 1 - i));
            }
        }
        return nval;
    }

    public static BDD buildVector(BDDFactory f, long bits, int[] vars, boolean bigEndian) {
        assert (vars.length < 2 || vars[0] < vars[vars.length - 1]);
        BDD temp2 = f.one();
        for (int j = vars.length - 1; j >= 0; j--) {
            int var = vars[j];
            long bit = 0;
            if (bigEndian) {
                bit = (bits >> (vars.length - j - 1)) & 1;
            } else {
                bit = (bits >> j) & 1;
            }
            if (bit == 0) {
                temp2.andWith(f.nithVar(var));
            } else {
                temp2.andWith(f.ithVar(var));
            }
        }
        return temp2;
    }

    public static long inverseGray(long n) {
        long ish, ans, idiv;
        ish = 1;
        ans = n;
        while (true) {
            idiv = ans >> ish;
            ans ^= idiv;
            if (idiv <= 1 || ish == 32) return ans;
            ish <<= 1;
        }
    }

    public static void main(String[] args) throws IOException {
        for (int i = 1; i < 9; i++) {
            BDD citt = new PBMToBDDConverter().Convert("C:\\citt\\ccitt" + i + ".pbm", CODING_2_DIM_X_Y, false);
            citt.getFactory().save("C:\\bdds\\citt" + i + "-2DXY.bdd", citt);
            citt.getFactory().done();
        }
    }
}
