package crypto.prng;

/**
 * The Mother of all RNG's
 * 
 * The recursion relation is x_n = 2111111111 x_{n-4} + 1492 x_{n-3} + 1776
 * x_{n-2} + 5115 x_{n-1} + carry mod b = 2^32
 * 
 * @author v-moyang
 * 
 */
public class MWCGenerator implements RandomGenerator {

    private static final long a[] = { 2111111111, 1492, 1776, 5115 };

    private static final long b = (long) 0xffffffff;

    private static final int shiftRight = 32;

    private long[] x = new long[4];

    private long carry;

    public MWCGenerator() {
        for (int i = 0; i < 4; ++i) x[i] = 0;
        carry = 0;
    }

    @Override
    public void addSeedMaterial(byte[] seed) {
        int pos = 0;
        for (int itr = 0; itr < seed.length; ++itr) {
            if (pos == 0) {
                for (int i = 0; i < 3; ++i) x[i] = x[i + 1];
                x[3] = seed[itr] & 0xff;
                pos = 1;
            } else {
                x[3] = (x[3] << 8) | (seed[itr] & 0xff);
                pos = (pos + 1 == 4 ? 0 : pos + 1);
            }
        }
    }

    @Override
    public void addSeedMaterial(long seed) {
        x[0] = x[2];
        x[1] = x[3];
        x[2] = seed & b;
        x[3] = seed >> shiftRight;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        nextBytes(bytes, 0, bytes.length);
    }

    private byte[] buf = new byte[4];

    @Override
    public void nextBytes(byte[] bytes, int start, int len) {
        int firstLen = len & 0xfffffffc;
        int val = 0;
        for (int pos = start; pos < start + firstLen; pos += 4) {
            val = next();
            bytes[pos] = (byte) (val & 0xff);
            bytes[pos + 1] = (byte) ((val >> 8) & 0xff);
            bytes[pos + 2] = (byte) ((val >> 16) & 0xff);
            bytes[pos + 3] = (byte) ((val >> 24) & 0xff);
        }
        if (firstLen < len) {
            val = next();
            buf[0] = (byte) (val & 0xff);
            buf[1] = (byte) ((val >> 8) & 0xff);
            buf[2] = (byte) ((val >> 16) & 0xff);
            buf[3] = (byte) ((val >> 24) & 0xff);
            System.arraycopy(buf, 0, bytes, start + firstLen, len - firstLen);
        }
    }

    private int next() {
        long val = 0;
        for (int i = 0; i < 4; ++i) val += a[i] * x[i];
        val += carry;
        carry = val >> shiftRight;
        for (int i = 0; i < 3; ++i) x[i] = x[i + 1];
        x[3] = val & b;
        return (int) x[3];
    }
}
