package com.ericdaugherty.mail.gui;

import java.io.*;
import java.nio.channels.FileChannel;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateCrtKey;

/**
 *
 * @author mfg8876
 */
public class Tools {

    public static void createTruststoreWithCACertificate(String certificatePathname, String provider, String type, String alias, String truststorePathname) {
        try {
            createTruststoreWithCACertificate0(certificatePathname, provider, type, alias, truststorePathname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createTruststoreWithCACertificate0(String certificatePathname, String provider, String type, String alias, String truststorePathname) throws Exception {
        CertificateFactory cf = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        Certificate serverCert = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
            bis = new BufferedInputStream(new FileInputStream(new File(certificatePathname)));
            fos = new FileOutputStream(new File(truststorePathname));
            serverCert = (X509Certificate) cf.generateCertificate(bis);
            KeyStore ks = KeyStore.getInstance(type, provider);
            ks.load(null, null);
            ks.setCertificateEntry(alias, serverCert);
            ks.store(fos, "password".toCharArray());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void createKeystoreWithPrivateKey(String pkcs12Alias, char[] pkcs12Password, String pkcs12Pathname, String provider, String type, String alias, char[] password, String keystorePathname) {
        try {
            createKeystoreWithPrivateKey0(pkcs12Alias, pkcs12Password, pkcs12Pathname, provider, type, alias, password, keystorePathname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createKeystoreWithPrivateKey0(String pkcs12Alias, char[] pkcs12Password, String pkcs12Pathname, String provider, String type, String alias, char[] password, String keystorePathname) throws Exception {
        FileOutputStream fos = null;
        try {
            KeyStore tempks = KeyStore.getInstance("pkcs12", "SunJSSE");
            File pkcs12 = new File(pkcs12Pathname);
            tempks.load(new FileInputStream(pkcs12), pkcs12Password);
            Key key = tempks.getKey(pkcs12Alias, pkcs12Password);
            if (key == null) {
                throw new RuntimeException("Got null key from keystore!");
            }
            RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) key;
            Certificate[] clientCerts = tempks.getCertificateChain(pkcs12Alias);
            if (clientCerts == null) {
                throw new RuntimeException("Got null cert chain from keystore!");
            }
            KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry(privKey, clientCerts);
            KeyStore.ProtectionParameter kspp = new KeyStore.PasswordProtection(password);
            fos = new FileOutputStream(keystorePathname);
            KeyStore ks = KeyStore.getInstance(type, provider);
            ks.load(null, password);
            ks.setEntry(alias, pke, kspp);
            ks.store(fos, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            if (System.getProperty("os.name").toUpperCase().indexOf("WIN") != -1) {
                int maxCount = (64 * 1024 * 1024) - (32 * 1024);
                long size = inChannel.size();
                long position = 0;
                while (position < size) {
                    position += inChannel.transferTo(position, maxCount, outChannel);
                }
            } else {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        } finally {
            if (inChannel != null) try {
                inChannel.close();
            } catch (Exception e) {
            }
            ;
            if (outChannel != null) try {
                outChannel.close();
            } catch (Exception e) {
            }
            ;
        }
    }
}
