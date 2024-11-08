package mipt.math.fuzzy.op.impl.ns;

import mipt.math.fuzzy.*;
import mipt.math.fuzzy.op.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class DMinus extends Minus {

    public FuzzyNumber calc(FuzzyNumber af, FuzzyNumber bf) {
        double b[] = bf.data(), a[] = af.data();
        double c[] = new double[b.length];
        double atmp[] = new double[2], btmp[] = new double[2];
        for (int i = 0; i < b.length; i += 3) {
            atmp[0] = a[i];
            atmp[1] = a[i + 1];
            btmp[0] = b[i];
            btmp[1] = b[i + 1];
            double res[] = minus(atmp, btmp);
            c[i] = res[0];
            c[i + 1] = res[1];
            c[i + 2] = a[i + 2];
        }
        return new DiscreteNumber(c);
    }

    public double[] minus(double a[], double b[]) {
        double l = a[0] - b[0], r = a[1] - b[1];
        double c[] = { Math.min(l, r), Math.max(l, r) };
        return c;
    }
}
