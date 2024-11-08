package ohealth.common.crypto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import javax.crypto.Cipher;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

public class TryEncryption {

    public static void main(String[] args) throws Exception {
        test2();
    }

    public static void test2() throws Exception {
        int keySize = 1024;
        int dBlockSize = keySize / 8;
        int eBlockSize = dBlockSize - 8 - 3;
        CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA", "MD5WithRSA");
        certAndKeyGen.generate(keySize);
        PublicKey publicKey = certAndKeyGen.getPublicKey();
        PrivateKey privateKey = certAndKeyGen.getPrivateKey();
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher1.init(Cipher.ENCRYPT_MODE, publicKey);
        String fileA = "C:/temp/a.txt";
        String fileB = "C:/temp/b.txt";
        String fileC = "C:/temp/c.txt";
        FileInputStream fis = new FileInputStream(fileA);
        FileOutputStream fos = new FileOutputStream(fileB, false);
        CipherOutputStream eos = new CipherOutputStream(fos, cipher1, eBlockSize);
        byte[] b = new byte[128];
        int i = fis.read(b);
        while (i != -1) {
            eos.write(b, 0, i);
            i = fis.read(b);
        }
        eos.flush();
        eos.close();
        fos.close();
        Cipher cipher2 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher2.init(Cipher.DECRYPT_MODE, privateKey);
        fis = new FileInputStream(fileB);
        CipherInputStream cis = new CipherInputStream(fis, cipher2, dBlockSize);
        FileOutputStream decodedFile = new FileOutputStream(fileC, false);
        int read = -1;
        while ((read = cis.read()) > -1) {
            decodedFile.write(read);
        }
        decodedFile.close();
        fis.close();
    }

    public static void test() throws Exception {
        int keySize = 512;
        int blockSize = keySize / 8;
        CertAndKeyGen certAndKeyGen = new CertAndKeyGen("RSA", "MD5WithRSA");
        certAndKeyGen.generate(keySize);
        PublicKey publicKey = certAndKeyGen.getPublicKey();
        PrivateKey privateKey = certAndKeyGen.getPrivateKey();
        String input = "Aabcdefgh12345678 " + "Babcdefgh12345678 " + "Cabcdefgh12345678 " + "Dabcdefgh12345678 " + "Eabcdefgh12345678 " + "Fabcdefgh12345678 " + "Gabcdefgh12345678 " + "Habcdefgh12345678 " + "Iabcdefgh12345678 " + "Jabcdefgh12345678 " + "Kabcdefgh12345678 " + "Labcdefgh12345678 " + "Mabcdefgh12345678";
        input = "This is to be encoded. Part1, Part2,Part3";
        byte[] binput = input.getBytes();
        System.out.println("blockSize = " + blockSize);
        System.out.println("len = " + binput.length);
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher1.init(Cipher.ENCRYPT_MODE, publicKey);
        System.out.println("binput.length = " + binput.length);
        byte[] boutput1B = cipher1.doFinal(binput, 0, binput.length);
        System.out.println("boutput1B.length = " + boutput1B.length);
        Cipher cipher2 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher2.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded1B = cipher2.doFinal(boutput1B);
        System.out.println("decoded1B.length = " + decoded1B.length);
        System.out.println(new String(decoded1B));
    }
}
