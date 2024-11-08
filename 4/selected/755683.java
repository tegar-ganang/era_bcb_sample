package net.sf.cryptoluggage.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.ProgressMonitorInputStream;

/**
 * This class offers a high level API to cryptographically transform files.
 * FileTransformer is intended to be used to insert files into the luggage or
 * to extract them back to plain files, but doesn't use any Luggage metadata
 * so it could be used for any other purposes.
 * 
 * The way instances of this class actually transform the files is precisely
 * defined in the ParameterSet instance required in the constructor.
 *
 * @author Miguel Hernández <mhernandez314@gmail.com>
 */
public class FileTransformer {

    protected ParameterSet parameterSet;

    private final int blocksInBuffer = 128;

    /**
     * Get a new instance ready to start transforming files
     * 
     * @param parameterSet the set of parameters to be used in the
     * transformation
     */
    public FileTransformer(ParameterSet parameterSet) {
        this.parameterSet = parameterSet;
    }

    /**
     * Encrypt input to output using the given secret key and the specified IV
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param input the input file
     * @param output the output file. Will be overwriten if existed before.
     * @param key the key used in the encryption
     * @param progressMessage Progress message to show if encryption takes long enough.
     * If it's null, no message will be shown
     * @param iv IV to be used in the cipher creation
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void encryptFile(File input, File output, SecretKey key, String progressMessage, IvParameterSpec iv) throws IOException {
        CipherGenerator cipherGenerator = new CipherGenerator(parameterSet);
        Cipher cipher = cipherGenerator.generateCipher(key, Cipher.ENCRYPT_MODE, iv);
        transformFile(input, output, cipher, true, progressMessage);
    }

    /**
     * Encrypt input to output using the given secret key and the key generation
     * salt as IV.
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param input the input file
     * @param output the output file. Will be overwriten if existed before.
     * @param key the key used in the encryption
     * @param progressMessage Progress message to show if encryption takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void encryptFile(File input, File output, SecretKey key, String progressMessage) throws IOException {
        encryptFile(input, output, key, progressMessage, parameterSet.getDefaultIV());
    }

    /**
     * Encrypt input to output using the given secret key, null progress message
     * and default IV.
     */
    public void encryptFile(File input, File output, SecretKey key) throws IOException {
        encryptFile(input, output, key, (String) null);
    }

    /**
     * Encrypt input to output using the given secret key, null progress message
     * but custom IV
     */
    public void encryptFile(File input, File output, SecretKey key, IvParameterSpec iv) throws IOException {
        encryptFile(input, output, key, null, iv);
    }

    /**
     * Encrypt input to output using the give secret key and the default IV
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param inputPath the input file path
     * @param output the output file path. Will be overwriten if existed before.
     * @param key the key used in the encryption
     * @param progressMessage Progress message to show if encryption takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void encryptFile(String inputPath, String outputPath, SecretKey key, String progressMessage) throws IOException {
        File input = new File(inputPath);
        File output = new File(outputPath);
        encryptFile(input, output, key, progressMessage);
    }

    public void encryptFile(String inputPath, String outputPath, SecretKey key, String progressMessage, IvParameterSpec iv) throws IOException {
        File input = new File(inputPath);
        File output = new File(outputPath);
        encryptFile(input, output, key, progressMessage, iv);
    }

    /**
     * Call encryptFile with null message and default IV
     */
    public void encryptFile(String inputPath, String outputPath, SecretKey key) throws IOException {
        encryptFile(inputPath, outputPath, key, (String) null);
    }

    /**
     * Call encryptFile with null message but custom IV
     */
    public void encryptFile(String inputPath, String outputPath, SecretKey key, IvParameterSpec iv) throws IOException {
        encryptFile(inputPath, outputPath, key, (String) null, iv);
    }

    /**
     * Decrypt input to output using the given secret key and the specified IV
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param input the input file
     * @param output the output file. Will be overwriten if existed before.
     * @param key the key used in the decryption
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * @param iv the IV to use in the cipher creation. If null, the default
     * IV will be used.
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void decryptFile(File input, File output, SecretKey key, String progressMessage, IvParameterSpec iv) throws IOException {
        if (iv == null) {
            iv = parameterSet.getDefaultIV();
        }
        CipherGenerator cipherGenerator = new CipherGenerator(parameterSet);
        Cipher cipher = cipherGenerator.generateCipher(key, Cipher.DECRYPT_MODE, iv);
        transformFile(input, output, cipher, false, progressMessage);
    }

    /**
     * Decrypt input to output using the given secret key and a default IV
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param input the input file
     * @param output the output file. Will be overwriten if existed before.
     * @param key the key used in the decryption
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void decryptFile(File input, File output, SecretKey key, String progressMessage) throws IOException {
        decryptFile(input, output, key, progressMessage, parameterSet.getDefaultIV());
    }

    /**
     * Call decryptFile with null message and default IV
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param input the input file
     * @param output the output file. Will be overwriten if existed before.
     * @param key the key used in the decryption
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void decryptFile(File input, File output, SecretKey key) throws IOException {
        decryptFile(input, output, key, null, parameterSet.getDefaultIV());
    }

    /**
     * Decrypt file using the given key and IV, with null progress message
     */
    public void decryptFile(File input, File output, SecretKey key, IvParameterSpec iv) throws IOException {
        decryptFile(input, output, key, null, iv);
    }

    /**
     * Decrypt input to output using the give secret key specifying a
     * progress message.
     *
     * @see org.sourceforge.cryptoluggage.crypto.EasyKeyGenerator
     * @param inputPath the input file path
     * @param output the output file path. Will be overwriten if existed before.
     * @param key the key used in the decryption
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException If any problem arises while processing data
     */
    public void decryptFile(String inputPath, String outputPath, SecretKey key, String progressMessage) throws IOException {
        decryptFile(inputPath, outputPath, key, parameterSet.getDefaultIV());
    }

    /**
     * Decrypt the file specified by inputPath with the given key and IV, and
     * custom progress message.
     */
    public void decryptFile(String inputPath, String outputPath, SecretKey key, String progressMessage, IvParameterSpec iv) throws IOException {
        File input = new File(inputPath);
        File output = new File(outputPath);
        decryptFile(input, output, key, progressMessage, iv);
    }

    /**
     * Call decryptFile with null progressMessage and default IV
     */
    public void decryptFile(String inputPath, String outputPath, SecretKey key) throws IOException {
        decryptFile(inputPath, outputPath, key, (String) null);
    }

    /**
     * Call decryptFile with the specified key and IV, but null progress message
     */
    public void decryptFile(String inputPath, String outputPath, SecretKey key, IvParameterSpec iv) throws IOException {
        decryptFile(inputPath, outputPath, key, null, iv);
    }

    /**
     * Apply the actual transformation to the file with an already instantiated
     * cipher
     *
     * @param input input file
     * @param output output file
     * @param cipher an already initialized Cipher instance, ready to
     * process data
     * @param compress if true, data will be compressed before aplying the cipher.
     * Else, it will be decompressed after aplying the cipher
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     *
     * @throws java.io.IOException If any problem arises while processing data
     */
    private void transformFile(File input, File output, Cipher cipher, boolean compress, String progressMessage) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(input);
        InputStream inputStream;
        if (progressMessage != null) {
            inputStream = new ProgressMonitorInputStream(null, progressMessage, fileInputStream);
        } else {
            inputStream = fileInputStream;
        }
        FilterInputStream is = new BufferedInputStream(inputStream);
        FilterOutputStream os = new BufferedOutputStream(new FileOutputStream(output));
        FilterInputStream fis;
        FilterOutputStream fos;
        if (compress) {
            fis = is;
            fos = new GZIPOutputStream(new CipherOutputStream(os, cipher));
        } else {
            fis = new GZIPInputStream(new CipherInputStream(is, cipher));
            fos = os;
        }
        byte[] buffer = new byte[cipher.getBlockSize() * blocksInBuffer];
        int readLength = fis.read(buffer);
        while (readLength != -1) {
            fos.write(buffer, 0, readLength);
            readLength = fis.read(buffer);
        }
        if (compress) {
            GZIPOutputStream gos = (GZIPOutputStream) fos;
            gos.finish();
        }
        fos.close();
        fis.close();
    }

    /**
     * Call transformFile with null progressMessage
     */
    private void transformFile(File input, File output, Cipher cipher, boolean compress) throws IOException {
        transformFile(input, output, cipher, compress, null);
    }
}
