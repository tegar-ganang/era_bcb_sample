package org.matthew.bork;

import java.util.Random;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Bork encrypts/decrypts files using a stream cipher - currently only
 * RC4 is implemented. Obfuscating filenames is also supported by
 * SHA-1 hashing: to enable, set borkFilenames=true.
 * <p>
 * The main () method of this class provides a simple command-line
 * interface for batch-mode Bork'ing files. You can also use the class
 * from other programs if required.
 * <p>
 * The BORK file format:
 * <p>
 * 
 * <pre>
 * 
 *   Types
 *   ----------------------------
 *   uint8  = 8-bit unsigned integer: [byte (bits 0..7)] 
 *   uint16 = 16-bit unsigned integer: [byte (bits 8..15), byte (bits 0..7)]
 *   uint32 = 32-bit unsigned integer: [23..31 , 16..23, 8..15, 0..7]
 *   string = length-delimited UTF-8 string: [uint8 (n), byte 1, ..., byte n]
 * 
 *   Bork file header (cleartext)
 *   ----------------------------
 *   [6 bytes  ] : Magic number for bork files ['b', 'o', 'r', 'k', 04, 02]
 *   [uint16   ] : Version number: major * 1000 + minor (eg 1000 = v1.0)
 *   [string   ] : Cipher type (must be "RC4" for version 1.0)
 * 
 *   Bork file body (ciphertext)
 *   ----------------------------
 *   [uint32   ] : CRC32 for the following data (after decryption). 
 *   [string   ] : Name of original file
 *   [remaining] : Encrypted contents of original file
 * </pre>
 *
 * @author Matthew Phillips
 */
public class Bork {

    /** Magic number used to identify BORK files */
    private static final byte[] MAGIC = new byte[] { 'b', 'o', 'r', 'k', '\4', '\2' };

    /** The format version: major * 1000 + minor. */
    private static final short VERSION = 1000;

    /** Default cipher name. */
    private static final String CIPHER_NAME = "RC4";

    private static final int ENCRYPT = 0;

    private static final int DECRYPT = 1;

    public static final int BUFFER_SIZE = 4096;

    private static boolean atLineStart = true;

    public String password;

    public File infile;

    public File outfile;

    public File outputDir;

    public boolean skipped;

    public boolean borkFilenames;

    /**
   * Command line interface for batch-processing files. The arguments
   * are treated as file names: if ending in ".bork" a decrypt is
   * attempted, otherwise the file is encrypted. The encryption
   * password must be set using "-Dbork.password=[password]".
   * <p>
   * 
   * Optional properties:
   * <p>
   * 
   * <pre>
   *  -Dbork.nuke=(true|false)    : Default: false. Destroy input files
   *                                after encrypt/decrypt. Cleartext files
   *                                are overwritten several times with
   *                                random crap before deletion, .bork
   *                                files are simply deleted.
   *  -Dbork.names=(true|false)   : Default: false. If true, scramble names
   *                                of .bork files with SHA-1.
   *  -Dbork.enbork=(true|false)  : Default: true. If false, do not
   *                                encrypt/decrypt any files. This is
   *                                useful in conjunction with
   *                                bork.nuke=true for destroying decrypted
   *                                plaintext.
   *  -Dbork.outputDir=directory  : Override the location for output files.
   *                                Default is same directory as the source
   *                                file.
   * </pre>
   * 
   * Exit codes:
   * 
   * <pre>
   *  0: success
   *  1: no password
   *  2: IO error on one or more files (some files may have been processed).
   *  3: IO error during a nuke (some files may have been processed).
   *  4: Output directory not valid.
   * </pre>
   * 
   * @param args The command line arguments (a set of input files).
   */
    public static void main(String[] args) {
        String password = System.getProperty("bork.password");
        File outputDir = getFile("bork.outputDir");
        boolean nuke = getBoolean("bork.nuke", false);
        boolean borkFilenames = getBoolean("bork.names", false);
        boolean enbork = getBoolean("bork.enbork", true);
        int errorCode = 0;
        if (enbork && password == null) {
            logError("Need a password in bork.password property");
            errorCode = 1;
        }
        if (outputDir != null) {
            if (!outputDir.exists()) {
                logError("Output directory does not exist: " + outputDir);
                errorCode = 4;
            } else if (!outputDir.isDirectory()) {
                logError("A file with the same name as the specified output directory " + "already exists: " + outputDir);
                errorCode = 4;
            }
        }
        if (errorCode != 0) System.exit(errorCode);
        for (int i = 0; i < args.length; i++) {
            String file = args[i];
            int mode = file.toLowerCase().endsWith(".bork") ? DECRYPT : ENCRYPT;
            try {
                if (enbork) {
                    logInfoNr("Borking " + file + " ... ");
                    Bork bork = new Bork(file, password);
                    bork.borkFilenames = borkFilenames;
                    if (outputDir != null) bork.outputDir = outputDir;
                    if (mode == ENCRYPT) bork.encrypt(); else bork.decrypt();
                    String outname = bork.outfile.getName();
                    if (outputDir != null) outname = outputDir.getName() + File.separatorChar + outname;
                    if (bork.skipped) {
                        logInfo("skipped: original file \"" + outname + "\" already exists");
                    } else {
                        logInfoNr(mode == ENCRYPT ? "borked" : "de-borked");
                        logInfo(" as \"" + outname + "\"");
                    }
                }
                if (nuke) {
                    File infile = new File(file);
                    try {
                        if (mode == ENCRYPT) {
                            logInfoNr("Nuking " + infile.getName() + " ... ");
                            nuke(infile);
                        } else {
                            logInfoNr("Deleting " + infile.getName() + " ... ");
                            delete(infile);
                        }
                        logInfo("done");
                    } catch (IOException ex) {
                        logError("Failed on error: " + ex.getMessage());
                        errorCode = 3;
                    }
                }
            } catch (IOException ex) {
                logError("Failed on error: " + ex.getMessage());
                errorCode = 2;
            }
        }
        System.exit(errorCode);
    }

    /**
   * Nuke a file by overwriting it several times with random crap and then
   * deleting it. This uses the JDK 1.4 NIO API to force disk sync at each
   * pass.
   * 
   * @param file The file to nuke.
   * 
   * @throws IOException if an error overwriting or deleting the file occurs.
   */
    public static void nuke(File file) throws IOException {
        long fileSize = file.length();
        byte[] crap = new byte[BUFFER_SIZE];
        Random random = new Random(crap.hashCode());
        FileOutputStream output = null;
        for (int c = 0; c < 3; c++) {
            try {
                output = new FileOutputStream(file);
                for (long written = 0; written < fileSize; written += crap.length) {
                    random.nextBytes(crap);
                    output.write(crap);
                }
            } finally {
                if (output != null) output.getChannel().force(false);
                close(output);
            }
        }
        if (!file.delete()) throw new IOException("Failed to delete nuked " + file);
    }

    public Bork(String filename, String password) {
        this(new File(filename), password);
    }

    public Bork(File file, String password) {
        this.infile = file;
        this.outputDir = infile.getAbsoluteFile().getParentFile();
        this.password = password;
    }

    public void decrypt() throws IOException {
        if (!infile.exists()) throw new FileNotFoundException(infile + " does not exist");
        try {
            doDecrypt(true);
        } catch (IOException ex) {
            errorCleanup();
            throw ex;
        } catch (RuntimeException ex) {
            errorCleanup();
            throw ex;
        }
    }

    public void encrypt() throws IOException {
        if (!infile.exists()) throw new FileNotFoundException(infile + " does not exist");
        try {
            doEncrypt();
        } catch (IOException ex) {
            errorCleanup();
            throw ex;
        } catch (RuntimeException ex) {
            errorCleanup();
            throw ex;
        }
    }

    private void doEncrypt() throws IOException {
        String cipherBaseFilename;
        if (borkFilenames) cipherBaseFilename = hexify(sha1(infile.getName())); else cipherBaseFilename = basename(infile);
        outfile = new File(outputDir, cipherBaseFilename + ".bork");
        if (outfile.exists()) {
            if (sameEncryptedFile(infile, outfile)) {
                skipped = true;
                return;
            } else {
                String outfileName = outfile.getName();
                outfile = null;
                throw new IOException("Encrypted output clash: plaintext file " + infile + " maps to encrypted file " + outfileName + " which is from another source");
            }
        }
        FileInputStream input = null;
        FileOutputStream cleartextOutput = null;
        try {
            input = new FileInputStream(infile);
            cleartextOutput = new FileOutputStream(outfile);
            cleartextOutput.write(MAGIC);
            writeShort(cleartextOutput, VERSION);
            writeString(cleartextOutput, CIPHER_NAME);
            Cipher cipher = createCipher(CIPHER_NAME, createSessionKey(password, cipherBaseFilename));
            int crcPos = (int) cleartextOutput.getChannel().position();
            cleartextOutput.getChannel().position(crcPos + 4);
            cipher.skip(4);
            CipherOutputStream encryptedOutput = new CipherOutputStream(cleartextOutput, cipher);
            writeString(encryptedOutput, infile.getName());
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) encryptedOutput.write(buffer, 0, bytesRead);
            cleartextOutput.getChannel().position(crcPos);
            cipher.reset();
            writeInt(encryptedOutput, (int) encryptedOutput.getCRC());
            close(cleartextOutput);
            outfile.setLastModified(infile.lastModified());
        } finally {
            close(input);
            close(cleartextOutput);
        }
    }

    /**
   * Do a decryption cycle.
   * 
   * @param createOutput Set to false to skip decrypting the file body and
   * only read the headers.
   */
    private void doDecrypt(boolean createOutput) throws IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        File tempOutput = null;
        try {
            input = new FileInputStream(infile);
            String cipherBaseFilename = basename(infile);
            byte[] magic = new byte[MAGIC.length];
            input.read(magic);
            for (int i = 0; i < MAGIC.length; i++) {
                if (MAGIC[i] != magic[i]) throw new IOException("Not a BORK file (bad magic number)");
            }
            short version = readShort(input);
            if (version / 1000 > VERSION / 1000) throw new IOException("File created by an incompatible future version: " + version + " > " + VERSION);
            String cipherName = readString(input);
            Cipher cipher = createCipher(cipherName, createSessionKey(password, cipherBaseFilename));
            CipherInputStream decryptedInput = new CipherInputStream(input, cipher);
            long headerCrc = Unsigned.promote(readInt(decryptedInput));
            decryptedInput.resetCRC();
            outfile = new File(outputDir, readString(decryptedInput));
            if (!createOutput || outfile.exists()) {
                skipped = true;
                return;
            }
            tempOutput = File.createTempFile("bork", null, outputDir);
            tempOutput.deleteOnExit();
            byte[] buffer = new byte[BUFFER_SIZE];
            output = new FileOutputStream(tempOutput);
            int bytesRead;
            while ((bytesRead = decryptedInput.read(buffer)) != -1) output.write(buffer, 0, bytesRead);
            output.close();
            output = null;
            if (headerCrc != decryptedInput.getCRC()) {
                outfile = null;
                throw new IOException("CRC mismatch: password is probably incorrect");
            }
            if (!tempOutput.renameTo(outfile)) throw new IOException("Failed to rename temp output file " + tempOutput + " to " + outfile);
            outfile.setLastModified(infile.lastModified());
        } finally {
            close(input);
            close(output);
            if (tempOutput != null) tempOutput.delete();
        }
    }

    /**
   * Test if an encrypted file is from a given source file by comparing the
   * embedded name. Only the names are compared, the paths are ignored.
   * 
   * todo check size as well
   * 
   * @param plaintextFile The plaintext file.
   * @param encryptedFile The encrypted file.
   * @return True if encryptedFile was created from a file with the same name
   * as plaintextFile. If the password is wrong or any error occurs reading the
   * encrypted file's header, this defaults to false.
   */
    private boolean sameEncryptedFile(File plaintextFile, File encryptedFile) {
        Bork bork = new Bork(encryptedFile.getPath(), password);
        try {
            bork.doDecrypt(false);
            if (bork.outfile.getName().equals(plaintextFile.getName())) return true;
        } catch (IOException ex) {
        }
        return false;
    }

    /**
   * Create the session key from a password plus the base ciphertext
   * output filename (minus extension), i.e. filename is used as an
   * initialisation vector (IV) to the password. This avoids using the
   * same key for every file borked with the same password, which
   * would open the encryption to a differential analysis attack.
   * 
   * @see #createCipher(String, byte[])
   */
    private static byte[] createSessionKey(String password, String cipherBaseFilename) throws UnsupportedEncodingException {
        return (password + cipherBaseFilename).getBytes("UTF-8");
    }

    /**
   * Factory method for creating cipher.
   * 
   * @see #createSessionKey(String, String)
   */
    private static Cipher createCipher(String name, byte[] key) {
        if (name.equals("RC4")) return new RC4(key); else throw new IllegalArgumentException("No cipher named " + name);
    }

    private void errorCleanup() {
        if (outfile != null && !outfile.delete()) logWarn("Failed to delete input file " + outfile);
    }

    /**
   * Generate the SHA-1 hash of a string.
   * 
   * @throws UnsupportedEncodingException if UTF-8 encoding is not
   * supported (since UTF-8 is a standard encoding for Java, this
   * will only happen in alternate universes).
   */
    private static byte[] sha1(String string) throws UnsupportedEncodingException {
        SHA1 sha1 = new SHA1();
        byte[] bytes = string.getBytes("UTF-8");
        sha1.engineUpdate(bytes, 0, bytes.length);
        return sha1.engineDigest();
    }

    /**
   * Turn a byte array into a hex string.
   */
    private static String hexify(byte[] bytes) {
        StringBuffer hex = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            hex.append(hexDigit((b >>> 4) & 0x0F));
            hex.append(hexDigit((b >>> 0) & 0x0F));
        }
        return hex.toString();
    }

    /**
   * Return the hex digit (0-9, a-f) for a decimal digit (0-15). 
   */
    private static char hexDigit(int digit) {
        if (digit < 0 || digit > 15) throw new IllegalArgumentException("Must be in range 0..15: " + digit);
        if (digit < 10) return (char) (digit + '0'); else return (char) (digit - 10 + 'a');
    }

    private static void writeShort(OutputStream output, int value) throws IOException {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) throw new IllegalArgumentException("Number not in range: " + value);
        output.write((value >>> 8) & 0xFF);
        output.write((value >>> 0) & 0xFF);
    }

    private static short readShort(InputStream input) throws IOException {
        int ch1 = input.read();
        int ch2 = input.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    private static int readInt(InputStream input) throws IOException {
        int ch1 = input.read();
        int ch2 = input.read();
        int ch3 = input.read();
        int ch4 = input.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 0) & 0xFF);
    }

    /**
   * Write a short (255 chars or less) string to the output stream.
   */
    private static void writeString(OutputStream output, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        if (bytes.length > 255) throw new IOException("String too long: " + bytes.length);
        output.write(bytes.length);
        output.write(bytes);
    }

    /**
   * Read a short (255 chars or less) string from an input stream.
   */
    private static String readString(InputStream input) throws IOException {
        int length = input.read();
        byte[] buffer = new byte[length];
        int bytesRead = input.read(buffer);
        if (bytesRead != length) {
            throw new IOException("Truncated string (should be " + length + " characters): \"" + new String(buffer, "UTF-8") + "\"");
        }
        return new String(buffer, "UTF-8");
    }

    private static void delete(File file) throws IOException {
        if (!file.delete()) throw new IOException("Cannot delete " + file);
    }

    /**
   * Return the base filename of a file, minus path and extension (if any).
   */
    private static String basename(File file) {
        String basename = file.getName();
        int extIndex = basename.lastIndexOf('.');
        if (extIndex != -1) return basename.substring(0, extIndex); else return basename;
    }

    /**
   * Get a boolean system property.
   * 
   * @param name The property name.
   * @param defaultValue The default value if not defined or invalid.
   */
    private static boolean getBoolean(String name, boolean defaultValue) {
        boolean value = defaultValue;
        String str = System.getProperty(name);
        if (str != null) {
            if (str.equalsIgnoreCase("true")) value = true; else if (str.equalsIgnoreCase("false")) value = false;
        }
        return value;
    }

    /**
   * Get a file from a system property.
   * 
   * @param name The property name.
   * @return A file wrapping the system property if defined, null
   *         otherwise.
   */
    private static File getFile(String name) {
        String filename = System.getProperty(name);
        return (filename == null) ? null : new File(filename);
    }

    /**
   * Close an input stream. Do nothing for null stream and ignore an error
   * while closing the stream.
   */
    private static void close(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
   * Close an output stream. Do nothing for null stream and ignore an
   * error while closing the stream.
   */
    private static void close(OutputStream output) {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ex) {
            }
        }
    }

    private static void logInfo(String message) {
        log(System.out, "Info", message, true);
    }

    private static void logInfoNr(String message) {
        log(System.out, "Info", message, false);
    }

    private static void logError(String message) {
        log(System.err, "Error", message, true);
    }

    private static void logWarn(String message) {
        log(System.err, "Warning", message, true);
    }

    private static void log(PrintStream out, String type, String message, boolean newline) {
        if (atLineStart) {
            out.print("Bork: " + type + ": ");
            atLineStart = false;
        }
        out.print(message);
        if (newline) {
            out.println();
            atLineStart = true;
        }
    }
}
