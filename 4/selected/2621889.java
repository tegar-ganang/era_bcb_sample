package com.funambol.syncclient.blackberry.email.impl;

import java.io.*;
import net.rim.device.api.crypto.*;
import net.rim.device.api.util.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.funambol.syncclient.util.StaticDataHelper;
import net.rim.device.api.crypto.MD5Digest;

/**
 * <code>Sync4jCrypto</code> Uses TripleDES Encryption & Decryption to encrypt & decrypt the data
 *
 */
public class Sync4jDesCrypto {

    DESKey key;

    private byte[] cred;

    public Sync4jDesCrypto(byte[] cred) {
        this.cred = cred;
        MD5Digest md = new MD5Digest();
        md.update(cred);
        byte[] keyBytes = md.getDigest(true);
        key = new DESKey(keyBytes);
        StaticDataHelper.log("" + key);
    }

    /**
    *@param String: data
    *encrypts  the data  
    *
    **/
    public byte[] encryptData(String data) {
        try {
            StaticDataHelper.log("" + key);
            DESEncryptorEngine encryptionEngine = new DESEncryptorEngine(key);
            PKCS5FormatterEngine formatterEngine = new PKCS5FormatterEngine(encryptionEngine);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BlockEncryptor encryptor = new BlockEncryptor(formatterEngine, outputStream);
            encryptor.write(data.getBytes());
            encryptor.close();
            return outputStream.toByteArray();
        } catch (CryptoTokenException e) {
            StaticDataHelper.log(e.toString());
        } catch (CryptoUnsupportedOperationException e) {
            StaticDataHelper.log(e.toString());
        } catch (IOException e) {
            StaticDataHelper.log(e.toString());
        }
        return null;
    }

    /**
    *@param byte [] : data
    *@return String : data 
    *decrypts  the data  
    *
    **/
    public String decryptData(byte[] data) {
        try {
            StaticDataHelper.log("" + key);
            DESDecryptorEngine decryptorEngine = new DESDecryptorEngine(key);
            PKCS5UnformatterEngine unformatterEngine = new PKCS5UnformatterEngine(decryptorEngine);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BlockDecryptor decryptor = new BlockDecryptor(unformatterEngine, inputStream);
            byte[] read = new byte[data.length];
            DataBuffer databuffer = new DataBuffer();
            int bytesRead = decryptor.read(read);
            if (bytesRead > 0) {
                databuffer.write(read, 0, bytesRead);
            }
            byte[] decryptedData = databuffer.toArray();
            return new String(decryptedData);
        } catch (CryptoTokenException e) {
            StaticDataHelper.log(e.toString());
        } catch (CryptoUnsupportedOperationException e) {
            StaticDataHelper.log(e.toString());
        } catch (IOException e) {
            StaticDataHelper.log(e.toString());
        }
        return null;
    }
}
