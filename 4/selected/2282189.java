package bwmorg.bouncycastle.crypto.tls;

import java.io.IOException;
import bwmorg.bouncycastle.crypto.*;
import bwmorg.bouncycastle.crypto.params.*;

/**
 * A generic TLS 1.0 block cipher suite. This can be used for AES or 3DES for
 * example.
 */
public class TlsBlockCipherCipherSuite extends TlsCipherSuite {

    private BlockCipher encryptCipher;

    private BlockCipher decryptCipher;

    private Digest writeDigest;

    private Digest readDigest;

    private int cipherKeySize;

    private short keyExchange;

    private TlsMac writeMac;

    private TlsMac readMac;

    protected TlsBlockCipherCipherSuite(BlockCipher encrypt, BlockCipher decrypt, Digest writeDigest, Digest readDigest, int cipherKeySize, short keyExchange) {
        this.encryptCipher = encrypt;
        this.decryptCipher = decrypt;
        this.writeDigest = writeDigest;
        this.readDigest = readDigest;
        this.cipherKeySize = cipherKeySize;
        this.keyExchange = keyExchange;
    }

    protected void init(byte[] ms, byte[] cr, byte[] sr) {
        int prfSize = (2 * cipherKeySize) + (2 * writeDigest.getDigestSize()) + (2 * encryptCipher.getBlockSize());
        byte[] key_block = new byte[prfSize];
        byte[] random = new byte[cr.length + sr.length];
        System.arraycopy(cr, 0, random, sr.length, cr.length);
        System.arraycopy(sr, 0, random, 0, sr.length);
        TlsUtils.PRF(ms, TlsUtils.toByteArray("key expansion"), random, key_block);
        int offset = 0;
        writeMac = new TlsMac(writeDigest, key_block, offset, writeDigest.getDigestSize());
        offset += writeDigest.getDigestSize();
        readMac = new TlsMac(readDigest, key_block, offset, readDigest.getDigestSize());
        offset += readDigest.getDigestSize();
        this.initCipher(true, encryptCipher, key_block, cipherKeySize, offset, offset + (cipherKeySize * 2));
        offset += cipherKeySize;
        this.initCipher(false, decryptCipher, key_block, cipherKeySize, offset, offset + cipherKeySize + decryptCipher.getBlockSize());
    }

    private void initCipher(boolean forEncryption, BlockCipher cipher, byte[] key_block, int key_size, int key_offset, int iv_offset) {
        KeyParameter key_parameter = new KeyParameter(key_block, key_offset, key_size);
        ParametersWithIV parameters_with_iv = new ParametersWithIV(key_parameter, key_block, iv_offset, cipher.getBlockSize());
        cipher.init(forEncryption, parameters_with_iv);
    }

    protected byte[] encodePlaintext(short type, byte[] plaintext, int offset, int len) {
        int blocksize = encryptCipher.getBlockSize();
        int paddingsize = blocksize - ((len + writeMac.getSize() + 1) % blocksize);
        int totalsize = len + writeMac.getSize() + paddingsize + 1;
        byte[] outbuf = new byte[totalsize];
        System.arraycopy(plaintext, offset, outbuf, 0, len);
        byte[] mac = writeMac.calculateMac(type, plaintext, offset, len);
        System.arraycopy(mac, 0, outbuf, len, mac.length);
        int paddoffset = len + mac.length;
        for (int i = 0; i <= paddingsize; i++) {
            outbuf[i + paddoffset] = (byte) paddingsize;
        }
        for (int i = 0; i < totalsize; i += blocksize) {
            encryptCipher.processBlock(outbuf, i, outbuf, i);
        }
        return outbuf;
    }

    protected byte[] decodeCiphertext(short type, byte[] ciphertext, int offset, int len, TlsProtocolHandler handler) throws IOException {
        int blocksize = decryptCipher.getBlockSize();
        boolean decrypterror = false;
        for (int i = 0; i < len; i += blocksize) {
            decryptCipher.processBlock(ciphertext, i + offset, ciphertext, i + offset);
        }
        int sPaddingsize = ciphertext[offset + len - 1];
        int uPaddingSize = sPaddingsize & 0xFF;
        if (offset + len - 1 - uPaddingSize < 0) {
            decrypterror = true;
            sPaddingsize = 0;
            uPaddingSize = 0;
        } else {
            for (int i = 0; i <= uPaddingSize; i++) {
                if (ciphertext[offset + len - 1 - i] != sPaddingsize) {
                    decrypterror = true;
                }
            }
        }
        int plaintextlength = len - readMac.getSize() - uPaddingSize - 1;
        byte[] calculatedMac = readMac.calculateMac(type, ciphertext, offset, plaintextlength);
        for (int i = 0; i < calculatedMac.length; i++) {
            if (ciphertext[offset + plaintextlength + i] != calculatedMac[i]) {
                decrypterror = true;
            }
        }
        if (decrypterror) {
            bwmorg.LOG.info("TlsBlockCipherCipherSuite: decodeCiphertext() - Error: unable to decrypt.");
            handler.failWithError(TlsProtocolHandler.AL_fatal, TlsProtocolHandler.AP_bad_record_mac);
        }
        byte[] plaintext = new byte[plaintextlength];
        System.arraycopy(ciphertext, offset, plaintext, 0, plaintextlength);
        return plaintext;
    }

    protected short getKeyExchangeAlgorithm() {
        return this.keyExchange;
    }
}
