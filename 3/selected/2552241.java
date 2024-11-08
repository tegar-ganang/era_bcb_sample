package com.mindbright.security.prng;

import java.util.Random;
import java.math.BigInteger;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.SecureRandomSpi;
import com.mindbright.util.RandomSeed;

public final class BlumBlumShub extends SecureRandomSpi {

    private static final BigInteger one = BigInteger.valueOf(1);

    private static final BigInteger two = BigInteger.valueOf(2);

    private BigInteger n;

    private BigInteger xi;

    private int bitsTotal;

    private int bitsLeft;

    private int entropyBits;

    public static final byte[] N = { (byte) 0x78, (byte) 0xd0, (byte) 0xc8, (byte) 0xac, (byte) 0xe2, (byte) 0x35, (byte) 0x77, (byte) 0x31, (byte) 0x3d, (byte) 0x33, (byte) 0x25, (byte) 0x58, (byte) 0x49, (byte) 0x1c, (byte) 0x0c, (byte) 0x88, (byte) 0xbe, (byte) 0xf2, (byte) 0xc2, (byte) 0xf0, (byte) 0x26, (byte) 0x14, (byte) 0x20, (byte) 0x02, (byte) 0x0a, (byte) 0x75, (byte) 0xdf, (byte) 0xd4, (byte) 0x11, (byte) 0x39, (byte) 0xbf, (byte) 0x64, (byte) 0x72, (byte) 0x8d, (byte) 0x85, (byte) 0xdd, (byte) 0xe0, (byte) 0xb6, (byte) 0x7d, (byte) 0x78, (byte) 0x24, (byte) 0xb6, (byte) 0x49, (byte) 0x0b, (byte) 0x23, (byte) 0x34, (byte) 0x94, (byte) 0x8b, (byte) 0x06, (byte) 0x9f, (byte) 0x6f, (byte) 0x5a, (byte) 0x46, (byte) 0xfb, (byte) 0x52, (byte) 0x76, (byte) 0x81, (byte) 0x05, (byte) 0x34, (byte) 0x23, (byte) 0xd5, (byte) 0xfd, (byte) 0xe9, (byte) 0x32, (byte) 0xc0, (byte) 0xb0, (byte) 0x7f, (byte) 0x16, (byte) 0x7e, (byte) 0xd3, (byte) 0xc1, (byte) 0x91, (byte) 0xda, (byte) 0x5e, (byte) 0xd1, (byte) 0xb2, (byte) 0x71, (byte) 0x13, (byte) 0x4e, (byte) 0x4e, (byte) 0x59, (byte) 0x32, (byte) 0x30, (byte) 0x59, (byte) 0xba, (byte) 0x5b, (byte) 0x34, (byte) 0xd3, (byte) 0x28, (byte) 0xfa, (byte) 0x9c, (byte) 0x03, (byte) 0xde, (byte) 0x96, (byte) 0xbf, (byte) 0xea, (byte) 0x84, (byte) 0x25, (byte) 0x63, (byte) 0x7e, (byte) 0x41, (byte) 0xde, (byte) 0x29, (byte) 0xf0, (byte) 0xd0, (byte) 0x7c, (byte) 0xbe, (byte) 0x45, (byte) 0xe4, (byte) 0x0f, (byte) 0x77, (byte) 0x5e, (byte) 0xc8, (byte) 0xf6, (byte) 0xdd, (byte) 0x5d, (byte) 0x83, (byte) 0xc6, (byte) 0x5c, (byte) 0xe7, (byte) 0x7f, (byte) 0x7e, (byte) 0xd7, (byte) 0x74, (byte) 0x2a, (byte) 0xc5, (byte) 0xab, (byte) 0xb5 };

    public BlumBlumShub(BigInteger n, byte[] seed) {
        this.n = n;
        if (seed != null) {
            engineSetSeed(seed);
        }
    }

    public BlumBlumShub(byte[] seed) {
        this(new BigInteger(N), seed);
    }

    public BlumBlumShub() {
        this(null);
    }

    private int log2(int n) {
        int ln = 0;
        while (n != 0) {
            n >>>= 1;
            ln += 1;
        }
        return ln;
    }

    private void nextXi() {
        if (xi == null) {
            RandomSeed seed = new RandomSeed();
            engineSetSeed(seed.getBytes(30));
        }
        xi = xi.modPow(two, n);
        bitsLeft = bitsTotal;
        byte[] xib = xi.toByteArray();
        entropyBits = 0;
        for (int i = 0; i < bitsLeft; i += 8) {
            entropyBits <<= i;
            entropyBits |= (xib[(xib.length - 4) + i] & 0xff);
        }
        entropyBits <<= (32 - bitsTotal);
        entropyBits >>>= (32 - bitsTotal);
    }

    public synchronized int nextBits(int bits) {
        int v = 0;
        while (bits >= bitsLeft) {
            v <<= bitsLeft;
            v |= entropyBits;
            bits -= bitsLeft;
            nextXi();
        }
        if (bits > 0) {
            int e = entropyBits;
            v <<= bits;
            e <<= (32 - bits);
            e >>>= (32 - bits);
            v |= e;
            entropyBits >>>= bits;
            bitsLeft -= bits;
        }
        return v;
    }

    public static BigInteger getBlumInteger(int bits, Random rand) {
        BigInteger a;
        BigInteger[] p = new BigInteger[2];
        BigInteger four = BigInteger.valueOf(4);
        BigInteger three = BigInteger.valueOf(3);
        int found = 0;
        do {
            a = new BigInteger(bits / 2, 128, rand);
            if ((a.mod(four)).equals(three)) {
                p[found++] = a;
            }
        } while (found < 2);
        return p[0].multiply(p[1]);
    }

    protected byte[] engineGenerateSeed(int numBytes) {
        byte[] seed = new byte[numBytes];
        engineNextBytes(seed);
        return seed;
    }

    protected void engineNextBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            int v = nextBits(8);
            bytes[i] = (byte) (v & 0xff);
        }
    }

    protected void engineSetSeed(byte[] seed) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            if (xi == null) {
                int seedBits = 8 * seed.length;
                if (seedBits > 160) seedBits = 160;
                bitsTotal = log2(seedBits);
            } else {
                sha1.update(xi.toByteArray());
            }
            sha1.update(seed);
            xi = new BigInteger(1, sha1.digest());
            while (!(xi.gcd(n)).equals(one)) {
                xi = xi.add(one);
            }
            nextXi();
        } catch (Exception e) {
            throw new Error("Error in BlumBlumShub engine: " + e);
        }
    }
}
