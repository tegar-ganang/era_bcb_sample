package com.atolsystems.atolutilities;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64InputStream;

public class AStreamUtilities {

    public static void appendBinFile(OutputStream out, File inputFile) throws FileNotFoundException, IOException {
        BufferedOutputStream bout = new BufferedOutputStream(out);
        FileInputStream in = new FileInputStream(inputFile);
        byte buf[] = new byte[4096];
        int read = in.read(buf);
        while (read != -1) {
            bout.write(buf, 0, read);
            read = in.read(buf);
        }
        bout.close();
    }

    public static void appendBinFile(OutputStream out, File inputFile, boolean enc64, boolean dec64) throws FileNotFoundException, IOException {
        BufferedOutputStream bout = new BufferedOutputStream(out);
        FileInputStream in = new FileInputStream(inputFile);
        Base64InputStream in64 = new Base64InputStream(in, enc64);
        InputStream input = (enc64 ^ dec64) ? in64 : in;
        byte buf[] = new byte[4096];
        byte outBuf[] = buf;
        int read = input.read(buf);
        while (read != -1) {
            bout.write(outBuf, 0, read);
            read = input.read(buf);
        }
        bout.close();
    }

    public static void appendBinFile(OutputStream out, File inputFile, byte[] key, boolean encrypt) throws FileNotFoundException, IOException, InvalidKeyException {
        try {
            BufferedOutputStream bout = new BufferedOutputStream(out);
            FileInputStream in = new FileInputStream(inputFile);
            byte[] buffer = new byte[4096];
            int size;
            final String CipherAlgorithmName = "AES";
            final String CipherAlgorithmMode = "CBC";
            final String CipherAlgorithmPadding = "PKCS5Padding";
            final String CryptoTransformation = CipherAlgorithmName + "/" + CipherAlgorithmMode + "/" + CipherAlgorithmPadding;
            SecretKeySpec ky = new SecretKeySpec(key, CipherAlgorithmName);
            byte[] iv = new byte[16];
            AlgorithmParameterSpec aps = new IvParameterSpec(iv);
            Cipher cf = Cipher.getInstance(CryptoTransformation);
            if (encrypt) cf.init(Cipher.ENCRYPT_MODE, ky, aps); else cf.init(Cipher.DECRYPT_MODE, ky, aps);
            while ((size = in.read(buffer)) != -1) {
                byte[] bufPost;
                if (size != buffer.length) {
                    try {
                        bufPost = cf.doFinal(buffer, 0, size);
                    } catch (IllegalBlockSizeException ex) {
                        throw new RuntimeException(ex);
                    } catch (BadPaddingException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    bufPost = cf.update(buffer, 0, size);
                }
                bout.write(bufPost);
            }
            bout.close();
        } catch (InvalidAlgorithmParameterException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void appendFile(OutputStream out, File inputFile) throws FileNotFoundException, IOException {
        appendFile(out, Charset.defaultCharset(), inputFile, Charset.defaultCharset());
    }

    public static void appendFile(OutputStream out, Charset outCharset, File inputFile, Charset inCharset) throws FileNotFoundException, IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, Charset.defaultCharset());
        appendFile(writer, inputFile, Charset.defaultCharset());
        writer.close();
    }

    public static void appendFile(OutputStreamWriter out, File inputFile, Charset inCharset) throws FileNotFoundException, IOException {
        BufferedWriter writer = new BufferedWriter(out);
        appendFile(writer, inputFile, Charset.defaultCharset());
        writer.close();
    }

    public static void appendFile(BufferedWriter out, File inputFile, Charset inCharset) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        appendStream(out, in, inCharset);
        in.close();
    }

    public static void appendStream(BufferedWriter out, InputStream in, Charset inCharset) throws FileNotFoundException, IOException {
        BufferedReader isr = new BufferedReader(new InputStreamReader(in, inCharset));
        appendStream(out, isr);
        isr.close();
    }

    public static void appendStream(BufferedWriter out, BufferedReader in) throws FileNotFoundException, IOException {
        int n;
        char cbuf[] = new char[4096];
        n = in.read(cbuf);
        while (n > 0) {
            StringBuilder sb = new StringBuilder(4096);
            sb.append(cbuf, 0, n);
            out.write(sb.toString());
            n = in.read(cbuf);
        }
    }

    public static void appendFile(BufferedWriter out, File inputFile, Charset inCharset, Map<String, String> replaceMap) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        appendStream(out, in, inCharset, replaceMap);
        in.close();
    }

    public static void appendStream(BufferedWriter out, InputStream in, Charset inCharset, Map<String, String> replaceMap) throws IOException {
        BufferedReader isr = new BufferedReader(new InputStreamReader(in, inCharset));
        appendStream(out, isr, replaceMap);
        isr.close();
    }

    public static void appendStream(BufferedWriter out, BufferedReader in, Map<String, String> replaceMap) throws IOException {
        int n;
        int bufferSize = 4096;
        char cbuf[] = new char[bufferSize];
        n = in.read(cbuf);
        String toWrite = "";
        while (n > 0) {
            StringBuilder sb = new StringBuilder(4096);
            sb.append(cbuf, 0, n);
            toWrite += sb.toString();
            n = in.read(cbuf);
        }
        toWrite = AStringUtilities.replace(toWrite, replaceMap);
        out.write(toWrite);
    }

    public static CharSequence stream2CharSequence(InputStream in, Charset inCharset) throws IOException {
        BufferedReader isr = new BufferedReader(new InputStreamReader(in, inCharset));
        CharSequence out = stream2CharSequence(isr);
        isr.close();
        return out;
    }

    public static CharSequence stream2CharSequence(BufferedReader in) throws IOException {
        int n;
        char cbuf[] = new char[4096];
        n = in.read(cbuf);
        StringBuilder sb = new StringBuilder(4096);
        while (n > 0) {
            sb.append(cbuf, 0, n);
            n = in.read(cbuf);
        }
        return sb.subSequence(0, sb.length());
    }

    static byte[] hexStream2Bytes(InputStream in, Charset inCharset) throws IOException {
        BufferedReader isr = new BufferedReader(new InputStreamReader(in, inCharset));
        byte[] out = hexStream2Bytes(isr);
        isr.close();
        return out;
    }

    static byte[] hexStream2Bytes(BufferedReader in) throws IOException {
        int n;
        int lastEvaluated = 0;
        char cbuf[] = new char[4096];
        n = in.read(cbuf);
        List<Byte> lb = new ArrayList<Byte>();
        while (n > 1) {
            for (int i = 0; i < n - 1; i++) {
                String test = "";
                test += cbuf[i];
                test += cbuf[i + 1];
                byte b = 0;
                try {
                    b = AStringUtilities.hexToByte(test);
                    lb.add(b);
                    i++;
                } catch (Exception ex) {
                }
                lastEvaluated = i;
            }
            if (lastEvaluated == (n - 2)) {
                cbuf[0] = cbuf[n - 1];
                n = in.read(cbuf, 1, 1) + 1;
            } else n = in.read(cbuf);
        }
        byte out[] = new byte[lb.size()];
        for (int i = 0; i < lb.size(); i++) {
            out[i] = lb.get(i);
        }
        return out;
    }
}
