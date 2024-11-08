package de.mud.ssh;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Marcus Meissner
 * @version $Id: SshPacket2.java 499 2005-09-29 08:24:54Z leo $
 */
public class SshPacket2 extends SshPacket {

    private static final boolean debug = true;

    private byte[] packet_length_array = new byte[5];

    private int packet_length = 0;

    private int padlen = 0;

    private byte[] crc_array = new byte[4];

    private int position = 0;

    private int phase_packet = 0;

    private final int PHASE_packet_length = 0;

    private final int PHASE_block = 1;

    private SshCrypto crypto = null;

    public SshPacket2(SshCrypto _crypto) {
        position = 0;
        phase_packet = PHASE_packet_length;
        crypto = _crypto;
    }

    public SshPacket2(byte newType) {
        setType(newType);
    }

    /**
   * Return the mp-int at the position offset in the data
   * First 4 bytes are the number of bytes in the integer, msb first
   * (for example, the value 0x00012345 would have 17 bits).  The
   * value zero has zero bits.  It is permissible that the number of
   * bits be larger than the real number of bits.
   * The number of bits is followed by (bits + 7) / 8 bytes of binary
   * data, msb first, giving the value of the integer.
   */
    public BigInteger getMpInt() {
        return new BigInteger(1, getBytes(getInt32()));
    }

    public void putMpInt(BigInteger bi) {
        byte[] mpbytes = bi.toByteArray(), xbytes;
        int i;
        for (i = 0; (i < mpbytes.length) && (mpbytes[i] == 0); i++) ;
        xbytes = new byte[mpbytes.length - i];
        System.arraycopy(mpbytes, i, xbytes, 0, mpbytes.length - i);
        putInt32(mpbytes.length - i);
        putBytes(xbytes);
    }

    public byte[] getPayLoad(SshCrypto xcrypt, long seqnr) throws IOException {
        byte[] data = getData();
        int blocksize = 8;
        packet_length = 4 + 1 + 1;
        if (data != null) packet_length += data.length;
        int padlen = blocksize - (packet_length % blocksize);
        if (padlen < 4) padlen += blocksize;
        byte[] padding = new byte[padlen];
        System.out.println("packet length is " + packet_length + ", padlen is " + padlen);
        if (xcrypt == null) for (int i = 0; i < padlen; i++) padding[i] = 0; else for (int i = 0; i < padlen; i++) padding[i] = SshMisc.getNotZeroRandomByte();
        byte[] block = new byte[packet_length + padlen];
        int xlen = padlen + packet_length - 4;
        block[3] = (byte) (xlen & 0xff);
        block[2] = (byte) ((xlen >> 8) & 0xff);
        block[1] = (byte) ((xlen >> 16) & 0xff);
        block[0] = (byte) ((xlen >> 24) & 0xff);
        block[4] = (byte) padlen;
        block[5] = getType();
        System.arraycopy(data, 0, block, 6, data.length);
        System.arraycopy(padding, 0, block, 6 + data.length, padlen);
        byte[] md5sum;
        if (xcrypt != null) {
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                System.err.println("SshPacket2: unable to load message digest algorithm: " + e);
            }
            byte[] seqint = new byte[4];
            seqint[0] = (byte) ((seqnr >> 24) & 0xff);
            seqint[1] = (byte) ((seqnr >> 16) & 0xff);
            seqint[2] = (byte) ((seqnr >> 8) & 0xff);
            seqint[3] = (byte) ((seqnr) & 0xff);
            md5.update(seqint, 0, 4);
            md5.update(block, 0, block.length);
            md5sum = md5.digest();
        } else {
            md5sum = new byte[0];
        }
        if (xcrypt != null) block = xcrypt.encrypt(block);
        byte[] sendblock = new byte[block.length + md5sum.length];
        System.arraycopy(block, 0, sendblock, 0, block.length);
        System.arraycopy(md5sum, 0, sendblock, block.length, md5sum.length);
        return sendblock;
    }

    ;

    private byte block[];

    public byte[] addPayload(byte buff[]) {
        int boffset = 0;
        byte b;
        byte[] newbuf = null;
        int hmaclen = 0;
        if (crypto != null) hmaclen = 16;
        System.out.println("addPayload2 " + buff.length);
        while (boffset < buff.length) {
            switch(phase_packet) {
                case PHASE_packet_length:
                    packet_length_array[position++] = buff[boffset++];
                    if (position == 5) {
                        packet_length = (packet_length_array[3] & 0xff) + ((packet_length_array[2] & 0xff) << 8) + ((packet_length_array[1] & 0xff) << 16) + ((packet_length_array[0] & 0xff) << 24);
                        padlen = packet_length_array[4];
                        position = 0;
                        System.out.println("SSH2: packet length " + packet_length);
                        System.out.println("SSH2: padlen " + padlen);
                        packet_length += hmaclen;
                        block = new byte[packet_length - 1];
                        phase_packet++;
                    }
                    break;
                case PHASE_block:
                    if (position < block.length) {
                        int amount = buff.length - boffset;
                        if (amount > 0) {
                            if (amount > block.length - position) amount = block.length - position;
                            System.arraycopy(buff, boffset, block, position, amount);
                            boffset += amount;
                            position += amount;
                        }
                    }
                    if (position == block.length) {
                        if (buff.length > boffset) {
                            newbuf = new byte[buff.length - boffset];
                            System.arraycopy(buff, boffset, newbuf, 0, buff.length - boffset);
                        }
                        byte[] decryptedBlock = new byte[block.length - hmaclen];
                        byte[] data;
                        packet_length -= hmaclen;
                        System.arraycopy(block, 0, decryptedBlock, 0, block.length - hmaclen);
                        if (crypto != null) decryptedBlock = crypto.decrypt(decryptedBlock);
                        for (int i = 0; i < decryptedBlock.length; i++) System.out.print(" " + decryptedBlock[i]);
                        System.out.println("");
                        setType(decryptedBlock[0]);
                        System.err.println("Packet type: " + getType());
                        System.err.println("Packet len: " + packet_length);
                        if (packet_length > padlen + 1 + 1) {
                            data = new byte[packet_length - 1 - padlen - 1];
                            System.arraycopy(decryptedBlock, 1, data, 0, data.length);
                            putData(data);
                        } else {
                            putData(null);
                        }
                        return newbuf;
                    }
                    break;
            }
        }
        return null;
    }

    ;
}
