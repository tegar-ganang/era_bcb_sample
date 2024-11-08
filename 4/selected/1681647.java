package org.j2eebuilder.license;

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * @(#)LicenseGenerator.java	1.350 01/12/03
 * LicenseGeneratorBean is a java bean with the main function of
 * signature and key-pair generation
 * the license
 * @version 1.3
 * Note: implements PropertyChangeListener, Session is required because
 * beans introspector does not support inheritance of interfaces. See
 * sun bug report ID 4140856
 */
public class LicenseGenerator {

    private String organizationName;

    private Integer numberOfOrganizations;

    private Integer numberOfUsers;

    private String licenseID;

    private Date expirationDate;

    public LicenseGenerator() {
    }

    public LicenseGenerator(String message, String privateKeyFile, String signatureFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        if (message == null) throw new SignatureException("Message is null. Nothing to sign.");
        this.signMessage(message.getBytes(), privateKeyFile, signatureFile);
    }

    public LicenseGenerator(byte[] messageBytes, String privateKeyFile, String signatureFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        if (messageBytes == null || messageBytes.length == 0) throw new SignatureException("Message is null. Nothing to sign.");
        this.signMessage(messageBytes, privateKeyFile, signatureFile);
    }

    private void signMessage(byte[] messageBytes, String privateKeyFile, String signatureFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
        PrivateKey privKey = readPrivateKey(privateKeyFile);
        dsa.initSign(privKey);
        dsa.update(messageBytes);
        byte[] realSig = dsa.sign();
        this.writeSignature(realSig, signatureFile);
    }

    private void writeSignature(byte[] signature, String signatureFile) throws IOException {
        FileOutputStream sigfos = new FileOutputStream(signatureFile);
        sigfos.write(signature);
        sigfos.close();
    }

    public static void generateKeyPair(String publicKeyFile, String privateKeyFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("DSA", "SUN");
        keygen.initialize(512, new SecureRandom());
        KeyPair keys = keygen.generateKeyPair();
        PublicKey publicKey = keys.getPublic();
        PrivateKey privateKey = keys.getPrivate();
        byte[] bPublicKey = publicKey.getEncoded();
        FileOutputStream pubkeyfos = new FileOutputStream(publicKeyFile);
        pubkeyfos.write(bPublicKey);
        pubkeyfos.close();
        byte[] bPrivateKey = privateKey.getEncoded();
        FileOutputStream privkeyfos = new FileOutputStream(privateKeyFile);
        privkeyfos.write(bPrivateKey);
        privkeyfos.close();
    }

    private PrivateKey readPrivateKey(String privateKeyFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        FileInputStream keyfis = new FileInputStream(privateKeyFile);
        byte[] encKey = new byte[keyfis.available()];
        keyfis.read(encKey);
        keyfis.close();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
        PrivateKey privkey = keyFactory.generatePrivate(privKeySpec);
        return privkey;
    }

    private PublicKey readPublicKey(String publicKeyFile) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        FileInputStream keyfis = new FileInputStream(publicKeyFile);
        byte[] encKey = new byte[keyfis.available()];
        keyfis.read(encKey);
        keyfis.close();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
        PublicKey pubkey = keyFactory.generatePublic(pubKeySpec);
        return pubkey;
    }

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter Licensee Name[Ohioedge]: ");
            String licenseeName = br.readLine();
            if (licenseeName == null || licenseeName.trim().equals("")) {
                licenseeName = "Ohioedge";
            }
            System.out.println("You entered licenseeName:" + licenseeName);
            java.sql.Date expirationDate = null;
            System.out.print("Enter License Expiration Date[2003-12-31]: ");
            String strExpirationDate = br.readLine();
            if (strExpirationDate == null || strExpirationDate.trim().equals("")) {
                expirationDate = java.sql.Date.valueOf("2003-12-31");
            } else {
                expirationDate = java.sql.Date.valueOf(strExpirationDate);
            }
            System.out.println("You entered expirationDate:" + expirationDate);
            System.out.print("Enter number of organizations[1]: ");
            String numberOfOrganizations = br.readLine();
            if (numberOfOrganizations == null || numberOfOrganizations.trim().equals("")) {
                numberOfOrganizations = "1";
            }
            System.out.println("You entered numberOfOrganizations:" + numberOfOrganizations);
            System.out.print("Enter number of users[1000]: ");
            String numberOfUsers = br.readLine();
            if (numberOfUsers == null || numberOfUsers.trim().equals("")) {
                numberOfUsers = "1000";
            }
            System.out.println("You entered numberOfUsers:" + numberOfUsers);
            File licenseDirectory = null;
            System.out.print("Enter the directory location where the license files will be created. You need to have read/write privilege on this directory. [C:\\ohioedge\\crm\\license]:");
            String strLicenseDirectory = br.readLine();
            if (strLicenseDirectory == null || strLicenseDirectory.trim().equals("")) {
                strLicenseDirectory = "C:\\ohioedge\\crm\\license";
            }
            licenseDirectory = new File(strLicenseDirectory);
            licenseDirectory.mkdirs();
            File publicKeyFile = new File(licenseDirectory, "public.key");
            publicKeyFile.createNewFile();
            File privateKeyFile = new File(licenseDirectory, "private.key");
            privateKeyFile.createNewFile();
            File msgSignatureFile = new File(licenseDirectory, "msg.signature");
            msgSignatureFile.createNewFile();
            System.out.println("You entered directory:" + licenseDirectory);
            System.out.print("1. Generating message...");
            String message = licenseeName + expirationDate.toString() + numberOfOrganizations + numberOfUsers;
            System.out.println("Complete.");
            System.out.print("2. Creating license files...");
            LicenseGenerator.generateKeyPair(publicKeyFile.toString(), privateKeyFile.toString());
            LicenseGenerator l = new LicenseGenerator(message, privateKeyFile.toString(), msgSignatureFile.toString());
            System.out.println("Complete.\npublic.key, private.key and msg.signature files are created in " + strLicenseDirectory + " directory.");
        } catch (Exception e) {
            System.out.println("Exception while generating license:" + e.toString());
        }
    }
}
