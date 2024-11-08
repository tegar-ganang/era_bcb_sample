package org.jcryptool.visual.he.algo;

import java.math.BigInteger;

/**
 * Encryption of the fully homomorphic encryption scheme by Gentry & Halevi
 * Based on the C code of Gentry & Halevi, see https://researcher.ibm.com/researcher/view_project.php?id=1579
 * @author Coen Ramaekers
 *
 */
public class GHEncrypt {

    /**
	 * Encrypts the plain bit b
	 * @param fheparams the scheme parameters
	 * @param key the key
	 * @param b the plain bit
	 * @return Encryption of b
	 */
    public static BigInteger encrypt(FHEParams fheparams, GHKeyGen key, int b) {
        int i, num = 1;
        int[] bit = new int[1];
        bit[0] = b;
        long n = 1 << (fheparams.logn);
        double p = ((double) fheparams.noise) / n;
        if (p > 0.5) p = 0.5;
        BigInteger[] vals = evalRandPoly(num, n, p, key.root, key.det);
        BigInteger[] out = new BigInteger[num];
        for (i = 0; i < num; i++) {
            out[i] = vals[i + 1];
        }
        for (i = 0; i < num; i++) {
            out[i] = out[i].shiftLeft(1);
            out[i] = out[i].add(new BigInteger(Integer.toString(bit[i])));
            if (out[i].compareTo(key.det) >= 0) out[i] = out[i].subtract(key.det);
        }
        return out[0];
    }

    /**
	 * Encrypts a bit array
	 * @param fheparams the scheme parameters
	 * @param key the key
	 * @param b the bit array
	 * @return An array containing the elementwise encryption of b
	 */
    public static BigInteger[] encrypt(FHEParams fheparams, GHKeyGen key, int[] b) {
        int i, num = b.length;
        long n = 1 << (fheparams.logn);
        double p = ((double) fheparams.noise) / n;
        if (p > 0.5) p = 0.5;
        BigInteger[] vals = evalRandPoly(num, n, p, key.root, key.det);
        BigInteger[] out = new BigInteger[num];
        for (i = 0; i < num; i++) {
            out[i] = vals[i + 1];
        }
        for (i = 0; i < num; i++) {
            out[i] = out[i].shiftLeft(1);
            out[i] = out[i].add(new BigInteger(Integer.toString(b[i])));
            if (out[i].compareTo(key.det) >= 0) out[i] = out[i].subtract(key.det);
        }
        return out;
    }

    /**
	 * Encrypts the plain bit b
	 * @param fheparams the scheme parameters
	 * @param key the keypair
	 * @param b the plain bit
	 * @return Encryption of b
	 */
    public static BigInteger encrypt(FHEParams fheparams, GHKeyPair key, int b) {
        int i, num = 1;
        int[] bit = new int[1];
        bit[0] = b;
        long n = 1 << (fheparams.logn);
        double p = ((double) fheparams.noise) / n;
        if (p > 0.5) p = 0.5;
        BigInteger[] vals = evalRandPoly(num, n, p, key.root, key.det);
        BigInteger[] out = new BigInteger[num];
        for (i = 0; i < num; i++) {
            out[i] = vals[i + 1];
        }
        for (i = 0; i < num; i++) {
            out[i] = out[i].shiftLeft(1);
            out[i] = out[i].add(new BigInteger(Integer.toString(bit[i])));
            if (out[i].compareTo(key.det) >= 0) out[i] = out[i].subtract(key.det);
        }
        return out[0];
    }

    /**
	 * Encrypts a bit array
	 * @param fheparams the scheme parameters
	 * @param key the keypair
	 * @param b the bit array
	 * @return An array containing the elementwise encryption of b
	 */
    public static BigInteger[] encrypt(FHEParams fheparams, GHKeyPair key, int[] b) {
        int i, num = b.length;
        long n = 1 << (fheparams.logn);
        double p = ((double) fheparams.noise) / n;
        if (p > 0.5) p = 0.5;
        BigInteger[] vals = evalRandPoly(num, n, p, key.root, key.det);
        BigInteger[] out = new BigInteger[num];
        for (i = 0; i < num; i++) {
            out[i] = vals[i + 1];
        }
        for (i = 0; i < num; i++) {
            out[i] = out[i].shiftLeft(1);
            out[i] = out[i].add(new BigInteger(Integer.toString(b[i])));
            if (out[i].compareTo(key.det) >= 0) out[i] = out[i].subtract(key.det);
        }
        return out;
    }

    /**
	 * Evaluates n random polynomials at root mod det, coefficients are 1, -1 with respective probability p/2
	 * and 0 with probability 1-p. Splits up the evaluation (into two parts) if the number of polynomials
	 * is large enough with respect to the degree, i.e. n+1+(m&1) < m/2.
	 * @param n the number of polynomials
	 * @param m the degree
	 * @param p the coefficient probability
	 * @param root the lattice root
	 * @param det the lattice determinant
	 * @return An array containing the n evaluations.
	 */
    public static BigInteger[] evalRandPoly(int n, long m, double p, BigInteger root, BigInteger det) {
        BigInteger[] vals;
        if ((n + 1 + (m & 1) < m / 2)) {
            double q;
            vals = evalRandPoly(2 * n, m / 2, p, root, det);
            for (int i = 1; i <= n; i++) {
                if (((m & 1) == 1) && ((q = Math.random()) < p)) {
                    vals[i + n] = ((q < p / 2) ? vals[i + n].add(vals[0]).mod(det) : vals[i + n].subtract(vals[0]).mod(det));
                }
                BigInteger tmp = vals[i + n].multiply(vals[0]).mod(det);
                vals[i] = vals[i].add(tmp);
            }
            vals[0] = vals[0].modPow(new BigInteger("2"), det);
            if ((m & 1) == 1) vals[0] = vals[0].multiply(root).mod(det);
        } else {
            vals = basicRandPoly(n, m, p, root, det);
        }
        return vals;
    }

    /**
	 * Evaluates n random polynomials at root mod det, coefficients are 1, -1 with respective probability p/2
	 * and 0 with probability 1-p.
	 * @param n the number of polynomials
	 * @param m the degree
	 * @param p the coefficient probability
	 * @param root the lattice root
	 * @param det the lattice determinant
	 * @return An array containing the n evaluations.
	 */
    public static BigInteger[] basicRandPoly(int n, long m, double p, BigInteger root, BigInteger det) {
        BigInteger[] vals = new BigInteger[n + 1];
        ;
        int i, j, k;
        if (m <= 0) {
            vals[0] = new BigInteger("1");
            return vals;
        }
        double q;
        for (i = 1; i <= n; i++) vals[i] = new BigInteger(Integer.toString((((q = Math.random()) < p) ? ((q < p / 2) ? -1 : 1) : 0)));
        if (m == 1) {
            vals[0] = root;
            return vals;
        }
        BigInteger rSqr = root.modPow(new BigInteger("2"), det);
        BigInteger rPowm;
        for (i = 1; i <= n; i++) {
            if ((q = Math.random()) < p) {
                vals[i] = (q < p / 2) ? vals[i].add(root) : vals[i].subtract(root).mod(det);
            }
            if (m > 2 && ((q = Math.random()) < p)) {
                vals[i] = (q < p / 2) ? vals[i].add(rSqr).mod(det) : vals[i].subtract(rSqr).mod(det);
            }
        }
        if (m > 4) {
            rPowm = rSqr;
            for (j = 4; j < m; j *= 2) {
                rPowm = rPowm.modPow(new BigInteger("2"), det);
                for (i = 1; i <= n; i++) {
                    if ((q = Math.random()) < p) {
                        vals[i] = (q < p / 2) ? vals[i].add(rPowm).mod(det) : vals[i].subtract(rPowm).mod(det);
                    }
                }
            }
        } else if (m < 4) {
            vals[0] = ((m == 2) ? rSqr : rSqr.multiply(root).mod(det));
            return vals;
        }
        BigInteger rOddPow = root;
        for (j = 3; j < m; j += 2) {
            rOddPow = rOddPow.multiply(rSqr).mod(det);
            rPowm = rOddPow;
            k = j;
            while (true) {
                for (i = 1; i <= n; i++) if ((q = Math.random()) < p) {
                    vals[i] = (q < p / 2) ? vals[i].add(rPowm).mod(det) : vals[i].subtract(rPowm).mod(det);
                }
                k *= 2;
                if (k >= m) break;
                rPowm = rPowm.modPow(new BigInteger("2"), det);
            }
        }
        vals[0] = (((m & 1) == 1) ? rOddPow.multiply(rSqr).mod(det) : rOddPow.multiply(root).mod(det));
        return vals;
    }
}
