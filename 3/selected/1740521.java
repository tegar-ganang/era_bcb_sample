package gnu.java.security.key.dss;

import gnu.java.security.hash.Sha160;
import gnu.java.security.util.PRNG;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * An implementation of the DSA parameters generation as described in FIPS-186.
 * <p>
 * References:
 * <p>
 * <a href="http://www.itl.nist.gov/fipspubs/fip186.htm">Digital Signature
 * Standard (DSS)</a>, Federal Information Processing Standards Publication
 * 186. National Institute of Standards and Technology.
 */
public class FIPS186 {

    public static final int DSA_PARAMS_SEED = 0;

    public static final int DSA_PARAMS_COUNTER = 1;

    public static final int DSA_PARAMS_Q = 2;

    public static final int DSA_PARAMS_P = 3;

    public static final int DSA_PARAMS_E = 4;

    public static final int DSA_PARAMS_G = 5;

    /** The BigInteger constant 2. */
    private static final BigInteger TWO = BigInteger.valueOf(2L);

    private static final BigInteger TWO_POW_160 = TWO.pow(160);

    /** The SHA instance to use. */
    private Sha160 sha = new Sha160();

    /** The length of the modulus of DSS keys generated by this instance. */
    private int L;

    /** The optional {@link SecureRandom} instance to use. */
    private SecureRandom rnd = null;

    /** Our default source of randomness. */
    private PRNG prng = null;

    public FIPS186(int L, SecureRandom rnd) {
        super();
        this.L = L;
        this.rnd = rnd;
    }

    /**
   * This method generates the DSS <code>p</code>, <code>q</code>, and
   * <code>g</code> parameters only when <code>L</code> (the modulus length)
   * is not one of the following: <code>512</code>, <code>768</code> and
   * <code>1024</code>. For those values of <code>L</code>, this
   * implementation uses pre-computed values of <code>p</code>,
   * <code>q</code>, and <code>g</code> given in the document <i>CryptoSpec</i>
   * included in the security guide documentation of the standard JDK
   * distribution.
   * <p>
   * The DSS requires two primes , <code>p</code> and <code>q</code>,
   * satisfying the following three conditions:
   * <ul>
   * <li><code>2<sup>159</sup> &lt; q &lt; 2<sup>160</sup></code></li>
   * <li><code>2<sup>L-1</sup> &lt; p &lt; 2<sup>L</sup></code> for a
   * specified <code>L</code>, where <code>L = 512 + 64j</code> for some
   * <code>0 &lt;= j &lt;= 8</code></li>
   * <li>q divides p - 1.</li>
   * </ul>
   * The algorithm used to find these primes is as described in FIPS-186,
   * section 2.2: GENERATION OF PRIMES. This prime generation scheme starts by
   * using the {@link Sha160} and a user supplied <i>SEED</i> to construct a
   * prime, <code>q</code>, in the range 2<sup>159</sup> &lt; q &lt; 2<sup>160</sup>.
   * Once this is accomplished, the same <i>SEED</i> value is used to construct
   * an <code>X</code> in the range <code>2<sup>L-1
   * </sup> &lt; X &lt; 2<sup>L</sup>. The prime, <code>p</code>, is then
   * formed by rounding <code>X</code> to a number congruent to <code>1 mod
   * 2q</code>. In this implementation we use the same <i>SEED</i> value given
   * in FIPS-186, Appendix 5.
   */
    public BigInteger[] generateParameters() {
        int counter, offset;
        BigInteger SEED, alpha, U, q, OFFSET, SEED_PLUS_OFFSET, W, X, p, c, g;
        byte[] a, u;
        byte[] kb = new byte[20];
        int b = (L - 1) % 160;
        int n = (L - 1 - b) / 160;
        BigInteger[] V = new BigInteger[n + 1];
        algorithm: while (true) {
            step1: while (true) {
                nextRandomBytes(kb);
                SEED = new BigInteger(1, kb).setBit(159).setBit(0);
                alpha = SEED.add(BigInteger.ONE).mod(TWO_POW_160);
                synchronized (sha) {
                    a = SEED.toByteArray();
                    sha.update(a, 0, a.length);
                    a = sha.digest();
                    u = alpha.toByteArray();
                    sha.update(u, 0, u.length);
                    u = sha.digest();
                }
                for (int i = 0; i < a.length; i++) a[i] ^= u[i];
                U = new BigInteger(1, a);
                q = U.setBit(159).setBit(0);
                if (q.isProbablePrime(80)) break step1;
            }
            counter = 0;
            offset = 2;
            step7: while (true) {
                OFFSET = BigInteger.valueOf(offset & 0xFFFFFFFFL);
                SEED_PLUS_OFFSET = SEED.add(OFFSET);
                synchronized (sha) {
                    for (int k = 0; k <= n; k++) {
                        a = SEED_PLUS_OFFSET.add(BigInteger.valueOf(k & 0xFFFFFFFFL)).mod(TWO_POW_160).toByteArray();
                        sha.update(a, 0, a.length);
                        V[k] = new BigInteger(1, sha.digest());
                    }
                }
                W = V[0];
                for (int k = 1; k < n; k++) W = W.add(V[k].multiply(TWO.pow(k * 160)));
                W = W.add(V[n].mod(TWO.pow(b)).multiply(TWO.pow(n * 160)));
                X = W.add(TWO.pow(L - 1));
                c = X.mod(TWO.multiply(q));
                p = X.subtract(c.subtract(BigInteger.ONE));
                if (p.compareTo(TWO.pow(L - 1)) >= 0) {
                    if (p.isProbablePrime(80)) break algorithm;
                }
                counter++;
                offset += n + 1;
                if (counter >= 4096) continue algorithm;
            }
        }
        BigInteger e = p.subtract(BigInteger.ONE).divide(q);
        BigInteger h = TWO;
        BigInteger p_minus_1 = p.subtract(BigInteger.ONE);
        g = TWO;
        for (; h.compareTo(p_minus_1) < 0; h = h.add(BigInteger.ONE)) {
            g = h.modPow(e, p);
            if (!g.equals(BigInteger.ONE)) break;
        }
        return new BigInteger[] { SEED, BigInteger.valueOf(counter), q, p, e, g };
    }

    /**
   * Fills the designated byte array with random data.
   * 
   * @param buffer the byte array to fill with random data.
   */
    private void nextRandomBytes(byte[] buffer) {
        if (rnd != null) rnd.nextBytes(buffer); else getDefaultPRNG().nextBytes(buffer);
    }

    private PRNG getDefaultPRNG() {
        if (prng == null) prng = PRNG.getInstance();
        return prng;
    }
}
