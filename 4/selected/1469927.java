package drjava.smyle.core;

import org.artsProject.mcop.*;
import drjava.smyle.*;

public class Compression {

    static final byte NOCOMPRESSION = 0, ZEROS = 1, DOUBLEZEROS = 2, XOR5 = 3, HIGHNIBBLE026 = 4, HIGHNIBBLE067 = 5, HIGHNIBBLE267 = 6;

    public static int estimateXOR(byte[] chunk, int m) {
        int bits = 0, last = 0;
        for (int i = 1; i < chunk.length; i++) {
            int d = chunk[i] ^ last;
            last = chunk[i];
            bits += (d == (d & ((1 << m) - 1))) ? 1 + m : 1 + 8;
        }
        return (bits + 7) / 8;
    }

    static void compressXOR(Buffer src, BitBuffer dest, int m) {
        int mask = (1 << m) - 1;
        byte last = 0;
        while (src.remaining() != 0) {
            byte s = src.readByte();
            byte d = (byte) (last ^ s);
            last = s;
            if (d == (d & mask)) {
                dest.writeBits(((s & mask) << 1) | 1, m + 1);
            } else {
                dest.writeBits(0, 1);
                dest.writeOffstreamByte(s);
            }
        }
        dest.close();
    }

    static void decompressXOR(BitBuffer src, Buffer dest, int m) {
        int mask = (1 << m) - 1;
        int last = 0;
        while (true) {
            int tag = src.readBits(1);
            if (tag < 0) return;
            if (tag != 0) {
                dest.writeByte((byte) (last = (last & ~mask) | src.readBits(m)));
            } else {
                last = src.readOffstreamByte();
                if (last < 0) return;
                dest.writeByte((byte) last);
            }
        }
    }

    /** stores one bit per byte: 0 = zero; 1 = not zero */
    public static int estimateZeros(byte[] chunk) {
        int bits = 0;
        for (int i = 0; i < chunk.length; i++) {
            bits += chunk[i] != 0 ? 1 + 8 : 1;
        }
        return (bits + 7) / 8;
    }

    static void compressZeros(Buffer src, BitBuffer dest) {
        while (src.remaining() != 0) {
            byte s = src.readByte();
            if (s == 0) dest.writeBits(1, 1); else {
                dest.writeBits(0, 1);
                dest.writeOffstreamByte(s);
            }
        }
        dest.close();
    }

    static void decompressZeros(BitBuffer src, Buffer dest) {
        while (true) {
            int tag = src.readBits(1);
            if (tag < 0) return;
            if (tag != 0) dest.writeByte((byte) 0); else {
                int s = src.readOffstreamByte();
                if (s < 0) return;
                dest.writeByte((byte) s);
            }
        }
    }

    /** stores every 0000 in one bit */
    public static int estimateDoubleZeros(byte[] chunk) {
        int bits = 0, i;
        for (i = 0; i + 1 < chunk.length; i += 2) {
            bits += chunk[i] != 0 || chunk[i + 1] != 0 ? 1 + 16 : 1;
        }
        bits += (chunk.length - i) * 8;
        return (bits + 7) / 8;
    }

    static void compressDoubleZeros(Buffer src, BitBuffer dest) {
        while (src.remaining() >= 2) {
            byte s = src.readByte(), s2 = src.readByte();
            if (s == 0 && s2 == 0) dest.writeBits(1, 1); else {
                dest.writeBits(0, 1);
                dest.writeOffstreamByte(s);
                dest.writeOffstreamByte(s2);
            }
        }
        if (src.remaining() != 0) {
            dest.writeBits(0, 1);
            dest.writeOffstreamByte(src.readByte());
        }
        dest.close();
    }

    static void decompressDoubleZeros(BitBuffer src, Buffer dest) {
        while (true) {
            int tag = src.readBits(1);
            if (tag < 0) return;
            if (tag != 0) {
                dest.writeByte((byte) 0);
                dest.writeByte((byte) 0);
            } else {
                int s = src.readOffstreamByte();
                if (s < 0) return;
                dest.writeByte((byte) s);
                s = src.readOffstreamByte();
                if (s < 0) return;
                dest.writeByte((byte) s);
            }
        }
    }

    /** compresses the most common higher-nibble of characters */
    public static int estimateHighNibble(byte[] chunk, int a, int b, int c) {
        int bits = 0;
        for (int i = 0; i < chunk.length; i++) {
            int nib = (chunk[i] >> 4) & 15;
            bits += (nib == a || nib == b || nib == c ? 2 : 2 + 4) + 4;
        }
        return (bits + 7) / 8;
    }

    static void compressHighNibble(Buffer src, BitBuffer dest, int a, int b, int c) {
        while (src.remaining() != 0) {
            int s = src.readByte(), nib = (s >> 4) & 15;
            if (nib == a) dest.writeBits(((s & 15) << 2) | 1, 6); else if (nib == b) dest.writeBits(((s & 15) << 2) | 2, 6); else if (nib == c) dest.writeBits(((s & 15) << 2) | 3, 6); else {
                dest.writeBits(0, 2);
                dest.writeOffstreamByte((byte) s);
            }
        }
        dest.close();
    }

    static void decompressHighNibble(BitBuffer src, Buffer dest, int a, int b, int c) {
        while (true) {
            int tag = src.readBits(2);
            switch(tag) {
                case 1:
                    dest.writeByte((byte) ((a << 4) | src.readBits(4)));
                    break;
                case 2:
                    dest.writeByte((byte) ((b << 4) | src.readBits(4)));
                    break;
                case 3:
                    dest.writeByte((byte) ((c << 4) | src.readBits(4)));
                    break;
                default:
                    int s = src.readOffstreamByte();
                    if (s < 0) return;
                    dest.writeByte((byte) s);
            }
        }
    }

    public static void compress(Buffer src, Buffer dest) {
        byte[] data = src.toByteArray();
        int[] est = new int[7];
        est[NOCOMPRESSION] = data.length;
        est[ZEROS] = estimateZeros(data);
        est[DOUBLEZEROS] = estimateDoubleZeros(data);
        est[XOR5] = estimateXOR(data, 5);
        est[HIGHNIBBLE026] = estimateHighNibble(data, 0, 2, 6);
        est[HIGHNIBBLE067] = estimateHighNibble(data, 0, 6, 7);
        est[HIGHNIBBLE267] = estimateHighNibble(data, 2, 6, 7);
        int method = 0;
        for (int i = 1; i < est.length; i++) if (est[i] < est[method]) method = i;
        dest.writeByte((byte) method);
        switch(method) {
            case NOCOMPRESSION:
                dest.writeBytes(data);
                break;
            case ZEROS:
                compressZeros(src, new BitBuffer(dest));
                break;
            case DOUBLEZEROS:
                compressDoubleZeros(src, new BitBuffer(dest));
                break;
            case XOR5:
                compressXOR(src, new BitBuffer(dest), 5);
                break;
            case HIGHNIBBLE026:
                compressHighNibble(src, new BitBuffer(dest), 0, 2, 6);
                break;
            case HIGHNIBBLE067:
                compressHighNibble(src, new BitBuffer(dest), 0, 6, 7);
                break;
            case HIGHNIBBLE267:
                compressHighNibble(src, new BitBuffer(dest), 2, 6, 7);
                break;
        }
    }

    public static void decompress(Buffer src, Buffer dest) {
        byte method = src.readByte();
        switch(method) {
            case NOCOMPRESSION:
                dest.writeBuffer(src);
                break;
            case ZEROS:
                decompressZeros(new BitBuffer(src), dest);
                break;
            case DOUBLEZEROS:
                decompressDoubleZeros(new BitBuffer(src), dest);
                break;
            case XOR5:
                decompressXOR(new BitBuffer(src), dest, 5);
                break;
            case HIGHNIBBLE026:
                decompressHighNibble(new BitBuffer(src), dest, 0, 2, 6);
                break;
            case HIGHNIBBLE067:
                decompressHighNibble(new BitBuffer(src), dest, 0, 6, 7);
                break;
            case HIGHNIBBLE267:
                decompressHighNibble(new BitBuffer(src), dest, 2, 6, 7);
                break;
            default:
                throw new InternalSmyleError("Unknown compression method " + method);
        }
    }
}
