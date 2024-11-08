package org.phoneid.keepassj2me;

import org.phoneid.*;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import java.lang.RuntimeException;
import java.io.UnsupportedEncodingException;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import org.bouncycastle1.util.encoders.Hex;
import org.bouncycastle1.crypto.*;
import org.bouncycastle1.crypto.generators.*;
import org.bouncycastle1.crypto.digests.*;
import org.bouncycastle1.crypto.params.*;
import org.bouncycastle1.crypto.paddings.*;
import org.bouncycastle1.crypto.modes.*;
import org.bouncycastle1.crypto.engines.*;
import org.bouncycastle1.util.*;
import org.bouncycastle1.util.encoders.*;

/**
 * Load a v3 database file.
 *
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 */
public class ImporterV3 {

    Form mForm;

    public ImporterV3() {
        super();
        mForm = null;
    }

    /**
     * Constructor which takes form for debugging
     */
    public ImporterV3(Form form) {
        super();
        mForm = form;
    }

    /**
   * Load a v3 database file, return contents in a new PwManager.
   * 
   * @param infile  Existing file to load.
   * @param password Pass phrase for infile.
   * @param pRepair (unused)
   * @return new PwManager container.
   * 
   * @throws IOException on any file error.
   * @throws InvalidKeyException on a decryption error, or possible internal bug.
   * @throws IllegalBlockSizeException on a decryption error, or possible internal bug.
   * @throws BadPaddingException on a decryption error, or possible internal bug.
   * @throws NoSuchAlgorithmException on a decryption error, or possible internal bug.
   * @throws NoSuchPaddingException on a decryption error, or possible internal bug.
   * @throws InvalidAlgorithmParameterException if error decrypting main file body. 
   * @throws ShortBufferException if error decrypting main file body.
   */
    public PwManager openDatabase(InputStream inStream, String password) throws IOException, PhoneIDException, InvalidCipherTextException {
        PwManager newManager;
        SHA256Digest md;
        byte[] transformedMasterKey;
        byte[] finalKey;
        if (mForm != null) mForm.append("openDatabase()\r\n");
        byte[] filebuf = new byte[(int) inStream.available()];
        inStream.read(filebuf, 0, (int) inStream.available());
        inStream.close();
        if (filebuf.length < PwDbHeader.BUF_SIZE) throw new IOException("File too short for header");
        PwDbHeader hdr = new PwDbHeader(filebuf, 0);
        if ((hdr.signature1 != PwManager.PWM_DBSIG_1) || (hdr.signature2 != PwManager.PWM_DBSIG_2)) {
            KeePassMIDlet.logS("Bad database file signature");
            throw new IOException("Bad database file signature");
        }
        if (hdr.version != PwManager.PWM_DBVER_DW) {
            KeePassMIDlet.logS("Bad database file version");
            throw new IOException("Bad database file version");
        }
        newManager = new PwManager();
        newManager.setMasterKey(password);
        if ((hdr.flags & PwManager.PWM_FLAG_RIJNDAEL) != 0) {
            KeePassMIDlet.logS("Algorithm AES");
            newManager.algorithm = PwManager.ALGO_AES;
        } else if ((hdr.flags & PwManager.PWM_FLAG_TWOFISH) != 0) {
            KeePassMIDlet.logS("Algorithm TWOFISH");
            newManager.algorithm = PwManager.ALGO_TWOFISH;
        } else {
            throw new IOException("Unknown algorithm.");
        }
        if (newManager.algorithm == PwManager.ALGO_TWOFISH) throw new IOException("TwoFish algorithm is not supported");
        newManager.numKeyEncRounds = hdr.numKeyEncRounds;
        if (mForm != null) mForm.append("rounds = " + newManager.numKeyEncRounds + "\r\n");
        newManager.name = "KeePass Password Manager";
        transformedMasterKey = transformMasterKey(hdr.masterSeed2, newManager.masterKey, newManager.numKeyEncRounds);
        md = new SHA256Digest();
        md.update(hdr.masterSeed, 0, hdr.masterSeed.length);
        md.update(transformedMasterKey, 0, transformedMasterKey.length);
        finalKey = new byte[md.getDigestSize()];
        md.doFinal(finalKey, 0);
        BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        cipher.init(false, new ParametersWithIV(new KeyParameter(finalKey), hdr.encryptionIV));
        int paddedEncryptedPartSize = cipher.processBytes(filebuf, PwDbHeader.BUF_SIZE, filebuf.length - PwDbHeader.BUF_SIZE, filebuf, PwDbHeader.BUF_SIZE);
        int encryptedPartSize = 0;
        PKCS7Padding padding = new PKCS7Padding();
        encryptedPartSize = paddedEncryptedPartSize - padding.padCount(filebuf);
        byte[] plainContent = new byte[encryptedPartSize];
        System.arraycopy(filebuf, PwDbHeader.BUF_SIZE, plainContent, 0, encryptedPartSize);
        if (mForm != null) mForm.append("filebuf length: " + filebuf.length + "\r\n");
        md = new SHA256Digest();
        md.update(filebuf, PwDbHeader.BUF_SIZE, encryptedPartSize);
        md.doFinal(finalKey, 0);
        if (PhoneIDUtil.compare(finalKey, hdr.contentsHash) == false) {
            System.out.println("Database file did not decrypt correctly. (checksum code is broken)");
            if (mForm != null) mForm.append("Database file did not decrypt correctly. (checksum code is broken)\r\n");
            throw new PhoneIDException("Wrong Password, or Database File Corrupted (database file did not decrypt correctly)");
        }
        if (mForm != null) mForm.append("Import all groups\r\n");
        int pos = PwDbHeader.BUF_SIZE;
        PwGroup newGrp = new PwGroup();
        for (int i = 0; i < hdr.numGroups; ) {
            int fieldType = Types.readShort(filebuf, pos);
            pos += 2;
            int fieldSize = Types.readInt(filebuf, pos);
            pos += 4;
            if (fieldType == 0xFFFF) {
                KeePassMIDlet.logS(newGrp.level + " " + newGrp.name);
                newManager.addGroup(newGrp);
                newGrp = new PwGroup();
                i++;
            } else {
                readGroupField(newGrp, fieldType, filebuf, pos);
            }
            pos += fieldSize;
        }
        if (mForm != null) mForm.append("Import all entries\r\n");
        PwEntry newEnt = new PwEntry();
        for (int i = 0; i < hdr.numEntries; ) {
            int fieldType = Types.readShort(filebuf, pos);
            int fieldSize = Types.readInt(filebuf, pos + 2);
            if (fieldType == 0xFFFF) {
                newManager.addEntry(newEnt);
                KeePassMIDlet.logS(newEnt.title);
                newEnt = new PwEntry();
                i++;
            } else {
                readEntryField(newEnt, filebuf, pos);
            }
            pos += 2 + 4 + fieldSize;
        }
        if (mForm != null) mForm.append("Keep the Meta-Info entry separate\r\n");
        for (int i = 0; i < newManager.entries.size(); i++) {
            PwEntry ent = (PwEntry) newManager.entries.elementAt(i);
            if (ent.title.equals("Meta-Info") && ent.url.equals("$") && ent.username.equals("SYSTEM")) {
                newManager.metaInfo = ent;
                newManager.entries.removeElementAt(i);
            }
        }
        if (mForm != null) mForm.append("Return newManager: " + newManager + "\r\n");
        return newManager;
    }

    /**
   * KeePass's custom pad style.
   * 
   * @param data buffer to pad.
   * @return addtional bytes to append to data[] to make
   *    a properly padded array.
   */
    public static byte[] makePad(byte[] data) {
        int thisblk = 32 - data.length % 32;
        int nextblk = 0;
        if (thisblk < 9) {
            nextblk = 32;
        }
        byte[] pad = new byte[thisblk + nextblk];
        pad[0] = (byte) 0x80;
        int ix = thisblk + nextblk - 8;
        Types.writeInt(data.length >> 29, pad, ix);
        bsw32(pad, ix);
        ix += 4;
        Types.writeInt(data.length << 3, pad, ix);
        bsw32(pad, ix);
        return pad;
    }

    public static void bsw32(byte[] ary, int offset) {
        byte t = ary[offset];
        ary[offset] = ary[offset + 3];
        ary[offset + 3] = t;
        t = ary[offset + 1];
        ary[offset + 1] = ary[offset + 2];
        ary[offset + 2] = t;
    }

    /**
   * Encrypt the master key a few times to make brute-force key-search harder
   * @throws NoSuchPaddingException 
   * @throws NoSuchAlgorithmException 
   * @throws ShortBufferException
   */
    static byte[] transformMasterKey(byte[] pKeySeed, byte[] pKey, int rounds) {
        KeePassMIDlet.logS("transformMasterKey, rounds=" + rounds);
        KeePassMIDlet.logS("transformMasterKey, pkey=" + new String(Hex.encode(pKey)));
        byte[] newKey = new byte[pKey.length];
        int i;
        BufferedBlockCipher cipher = new BufferedBlockCipher(new AESEngine());
        cipher.init(true, new KeyParameter(pKeySeed));
        newKey = pKey;
        for (i = 0; i < rounds; i++) cipher.processBytes(newKey, 0, newKey.length, newKey, 0);
        SHA256Digest md = new SHA256Digest();
        md.update(newKey, 0, newKey.length);
        md.doFinal(newKey, 0);
        return newKey;
    }

    /**
   * Parse and save one record from binary file.
   * @param buf
   * @param offset
   * @return If >0, 
   */
    void readGroupField(PwGroup grp, int fieldType, byte[] buf, int offset) {
        switch(fieldType) {
            case 0x0000:
                break;
            case 0x0001:
                grp.groupId = Types.readInt(buf, offset);
                break;
            case 0x0002:
                grp.name = new String(buf, offset, Types.strlen(buf, offset));
                break;
            case 0x0003:
                grp.tCreation = Types.readTime(buf, offset);
                break;
            case 0x0004:
                grp.tLastMod = Types.readTime(buf, offset);
                break;
            case 0x0005:
                grp.tLastAccess = Types.readTime(buf, offset);
                break;
            case 0x0006:
                grp.tExpire = Types.readTime(buf, offset);
                break;
            case 0x0007:
                grp.imageId = Types.readInt(buf, offset);
                break;
            case 0x0008:
                grp.level = Types.readShort(buf, offset);
                break;
            case 0x0009:
                grp.flags = Types.readInt(buf, offset);
                break;
        }
    }

    void readEntryField(PwEntry ent, byte[] buf, int offset) throws UnsupportedEncodingException {
        int fieldType = Types.readShort(buf, offset);
        offset += 2;
        int fieldSize = Types.readInt(buf, offset);
        offset += 4;
        switch(fieldType) {
            case 0x0000:
                break;
            case 0x0001:
                System.arraycopy(buf, offset, ent.uuid, 0, 16);
                break;
            case 0x0002:
                ent.groupId = Types.readInt(buf, offset);
                break;
            case 0x0003:
                ent.imageId = Types.readInt(buf, offset);
                break;
            case 0x0004:
                ent.title = new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
                break;
            case 0x0005:
                ent.url = new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
                break;
            case 0x0006:
                ent.username = new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
                break;
            case 0x0007:
                ent.setPassword(buf, offset, Types.strlen(buf, offset));
                break;
            case 0x0008:
                ent.additional = new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
                break;
            case 0x0009:
                ent.tCreation = Types.readTime(buf, offset);
                break;
            case 0x000A:
                ent.tLastMod = Types.readTime(buf, offset);
                break;
            case 0x000B:
                ent.tLastAccess = Types.readTime(buf, offset);
                break;
            case 0x000C:
                ent.tExpire = Types.readTime(buf, offset);
                break;
            case 0x000D:
                ent.binaryDesc = new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
                break;
            case 0x000E:
                ent.setBinaryData(buf, offset, fieldSize);
                break;
        }
    }

    /**
   * Test Sun's JCE.
   * Note you need the "unlimited security" policy files from Sun.
   * They're where you download the JDK, i.e.
   * <a href="http://java.sun.com/j2se/1.5.0/download.jsp"
   * >http://java.sun.com/j2se/1.5.0/download.jsp</a>
   * @throws NoSuchPaddingException 
   * @throws NoSuchAlgorithmException 
   */
    static void testRijndael_JCE() {
        byte[] aKey = new byte[32];
        byte[] aTest = new byte[16];
        byte[] aRef = new byte[16];
        int[] aRef_int = { 0x8e, 0xa2, 0xb7, 0xca, 0x51, 0x67, 0x45, 0xbf, 0xea, 0xfc, 0x49, 0x90, 0x4b, 0x49, 0x60, 0x89 };
        int i;
        for (i = 0; i < 32; i++) {
            aKey[i] = (byte) i;
        }
        for (i = 0; i < 16; i++) {
            aTest[i] = (byte) ((i << 4) | i);
            aRef[i] = (byte) aRef_int[i];
        }
        try {
            BufferedBlockCipher cipher = new BufferedBlockCipher(new AESEngine());
            cipher.init(true, new KeyParameter(aKey));
            cipher.processBytes(aTest, 0, aTest.length, aTest, 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("JCE failed test");
        }
        if (PhoneIDUtil.compare(aTest, aRef) == false) throw new RuntimeException("JCE failed test");
    }
}
