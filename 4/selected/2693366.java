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
public class Sync4jCrypto {

    TripleDESKey key;

    private byte[] cred;

    public Sync4jCrypto(byte[] cred) {
        this.cred = cred;
        MD5Digest md = new MD5Digest();
        md.update(cred);
        byte[] keyBytes = md.getDigest(true);
        key = new TripleDESKey(keyBytes);
    }

    /**
    *@param String: data
    *encrypts  the data  
    *
    **/
    public byte[] encryptData(String data) {
        try {
            TripleDESEncryptorEngine encryptionEngine = new TripleDESEncryptorEngine(key);
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
            TripleDESDecryptorEngine decryptorEngine = new TripleDESDecryptorEngine(key);
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
            return decryptedData.toString();
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
