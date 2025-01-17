package org.jarsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checksum generation methods.
 *
 * @version $Revision: 1.12 $
 */
public class Generator {

    /**
    * Our configuration. Contains such things as our rolling checksum
    * and message digest.
    */
    protected final Configuration config;

    public Generator(Configuration config) {
        this.config = config;
    }

    /**
    * Generate checksums over an entire byte array, with a base offset
    * of 0.
    *
    * @param buf The byte buffer to checksum.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
    public List generateSums(byte[] buf) {
        return generateSums(buf, 0, buf.length, 0);
    }

    /**
    * Generate checksums over a portion of a byte array, with a base
    * offset of 0.
    *
    * @param buf The byte array to checksum.
    * @param off The offset in <code>buf</code> to begin.
    * @param len The number of bytes to checksum.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
    public List generateSums(byte[] buf, int off, int len) {
        return generateSums(buf, off, len, 0);
    }

    /**
    * Generate checksums over an entire byte array, with a specified
    * base offset. This <code>baseOffset</code> is added to the offset
    * stored in each {@link ChecksumPair}.
    *
    * @param buf        The byte array to checksum.
    * @param baseOffset The offset from whence this byte array came.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the array.
    * @see #generateSums(byte[],int,int,long)
    */
    public List generateSums(byte[] buf, long baseOffset) {
        return generateSums(buf, 0, buf.length, baseOffset);
    }

    /**
    * Generate checksums over a portion of abyte array, with a specified
    * base offset. This <code>baseOffset</code> is added to the offset
    * stored in each {@link ChecksumPair}.
    *
    * @param buf        The byte array to checksum.
    * @param off        From whence in <code>buf</code> to start.
    * @param len        The number of bytes to check in
    *                   <code>buf</code>.
    * @param baseOffset The offset from whence this byte array came.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the array.
    */
    public List generateSums(byte[] buf, int off, int len, long baseOffset) {
        int count = (len + (config.blockLength - 1)) / config.blockLength;
        int remainder = len % config.blockLength;
        int offset = off;
        List sums = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            int n = Math.min(len, config.blockLength);
            ChecksumPair pair = generateSum(buf, offset, n, offset + baseOffset);
            pair.seq = i;
            sums.add(pair);
            len -= n;
            offset += n;
        }
        return sums;
    }

    /**
    * Generate checksums for an entire file.
    *
    * @param f The {@link java.io.File} to checksum.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the file.
    * @throws java.io.IOException if <code>f</code> cannot be read from.
    */
    public List generateSums(File f) throws IOException {
        long len = f.length();
        int count = (int) ((len + (config.blockLength + 1)) / config.blockLength);
        long offset = 0;
        FileInputStream fin = new FileInputStream(f);
        List sums = new ArrayList(count);
        int n = config.blockLength;
        byte[] buf = new byte[n];
        for (int i = 0; i < count; i++) {
            int l = fin.read(buf, 0, n);
            if (l == -1) {
                break;
            }
            if (n < config.blockLength) {
                Arrays.fill(buf, n, config.blockLength, (byte) 0);
            }
            if (l > 0) {
                ChecksumPair pair = generateSum(buf, 0, config.blockLength, offset);
                pair.seq = i;
                sums.add(pair);
                len -= n;
                offset += n;
                n = (int) Math.min(len, config.blockLength);
            }
        }
        fin.close();
        return sums;
    }

    /**
    * Generate checksums for an InputStream.
    *
    * @param in The {@link java.io.InputStream} to checksum.
    * @return A {@link java.util.List} of {@link ChecksumPair}s
    *    generated from the bytes read.
    * @throws java.io.IOException if reading fails.
    */
    public List generateSums(InputStream in) throws IOException {
        List sums = null;
        byte[] buf = new byte[config.blockLength * config.blockLength];
        long offset = 0;
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            if (sums == null) {
                sums = generateSums(buf, 0, len, offset);
            } else {
                sums.addAll(generateSums(buf, 0, len, offset));
            }
            offset += len;
        }
        return sums;
    }

    /**
    * Generate a sum pair for an entire byte array.
    *
    * @param buf The byte array to checksum.
    * @param fileOffset The offset in the original file from whence
    *    this block came.
    * @return A {@link ChecksumPair} for this byte array.
    */
    public ChecksumPair generateSum(byte[] buf, long fileOffset) {
        return generateSum(buf, 0, buf.length, fileOffset);
    }

    /**
    * Generate a sum pair for a portion of a byte array.
    *
    * @param buf The byte array to checksum.
    * @param off Where in <code>buf</code> to start.
    * @param len How many bytes to checksum.
    * @param fileOffset The original offset of this byte array.
    * @return A {@link ChecksumPair} for this byte array.
    */
    public ChecksumPair generateSum(byte[] buf, int off, int len, long fileOffset) {
        ChecksumPair p = new ChecksumPair();
        config.weakSum.check(buf, off, len);
        config.strongSum.update(buf, off, len);
        if (config.checksumSeed != null) {
            config.strongSum.update(config.checksumSeed, 0, config.checksumSeed.length);
        }
        p.weak = config.weakSum.getValue();
        p.strong = new byte[config.strongSumLength];
        System.arraycopy(config.strongSum.digest(), 0, p.strong, 0, p.strong.length);
        p.offset = fileOffset;
        p.length = len;
        return p;
    }

    public int generateWeakSum(byte[] buf, int offset) {
        config.weakSum.first(buf, offset, config.blockLength);
        int weakSum = config.weakSum.getValue();
        return weakSum;
    }

    public int generateRollSum(byte b) {
        config.weakSum.roll(b);
        int weakSum = config.weakSum.getValue();
        return weakSum;
    }

    public byte[] generateStrongSum(byte[] buf, int off, int len) {
        config.strongSum.update(buf, off, len);
        byte[] strongSum = new byte[config.strongSumLength];
        System.arraycopy(config.strongSum.digest(), 0, strongSum, 0, strongSum.length);
        return strongSum;
    }
}
