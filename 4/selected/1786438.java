package com.atolsystems.atolutilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

public class AFileUtilities {

    protected static String currentDirectory;

    public static final String platformPathSeparator;

    static {
        currentDirectory = System.getProperty("user.dir");
        platformPathSeparator = System.getProperty("file.separator");
    }

    public static synchronized String getCurrentDirectory() {
        return currentDirectory;
    }

    public static synchronized void setCurrentDirectory(String currentDirectory) {
        AFileUtilities.currentDirectory = currentDirectory;
    }

    public static File createTempDirectory(String pre, String post) throws IOException {
        File dummyFile = File.createTempFile(pre, "");
        File out = new File(dummyFile.getCanonicalPath() + post);
        dummyFile.delete();
        if (!out.mkdir()) throw new RuntimeException("Could not create directory \"" + out.getCanonicalPath() + "\"");
        return out;
    }

    public static void fixEol(File fin) throws IOException {
        File fout = File.createTempFile(fin.getName(), ".fixEol", fin.getParentFile());
        FileChannel in = new FileInputStream(fin).getChannel();
        if (0 != in.size()) {
            FileChannel out = new FileOutputStream(fout).getChannel();
            byte[] eol = AStringUtilities.systemNewLine.getBytes();
            ByteBuffer bufOut = ByteBuffer.allocateDirect(1024 * eol.length);
            boolean previousIsCr = false;
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            while (in.read(buf) > 0) {
                buf.limit(buf.position());
                buf.position(0);
                while (buf.remaining() > 0) {
                    byte b = buf.get();
                    if (b == '\r') {
                        previousIsCr = true;
                        bufOut.put(eol);
                    } else {
                        if (b == '\n') {
                            if (!previousIsCr) bufOut.put(eol);
                        } else bufOut.put(b);
                        previousIsCr = false;
                    }
                }
                bufOut.limit(bufOut.position());
                bufOut.position(0);
                out.write(bufOut);
                bufOut.clear();
                buf.clear();
            }
            out.close();
        }
        in.close();
        fin.delete();
        fout.renameTo(fin);
    }

    public static String adaptPathSeparator(String path) {
        if (!"/".equals(AFileUtilities.platformPathSeparator)) path = path.replace("/", AFileUtilities.platformPathSeparator);
        if (!"\\".equals(AFileUtilities.platformPathSeparator)) path = path.replace("\\", AFileUtilities.platformPathSeparator);
        return path;
    }

    public static synchronized File newFile(String name) {
        name = AFileUtilities.adaptPathSeparator(name);
        File out = new File(name);
        if (false == out.isAbsolute()) out = new File(currentDirectory, name);
        return out;
    }

    public static File newFile(String baseDirectory, String name) {
        name = AFileUtilities.adaptPathSeparator(name);
        File out = new File(name);
        if (false == out.isAbsolute()) out = new File(baseDirectory, name);
        return out;
    }

    public static File newFile(File baseDirectory, String name) {
        return newFile(baseDirectory.getAbsolutePath(), name);
    }

    /**
     * Tell if two files have the some content or not
     * @param a
     * @param b
     * @return return true if files have identical content
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean compare(File a, File b) throws FileNotFoundException, IOException {
        if (a.length() != b.length()) return false;
        InputStream as = new FileInputStream(a);
        InputStream bs = new FileInputStream(b);
        int fromA = 0;
        int fromB = 0;
        while (fromA != -1) {
            fromA = as.read();
            fromB = bs.read();
            if (fromA != fromB) return false;
        }
        return true;
    }

    /**
     * Generate a binary difference file: out = a^b
     * @param a
     * @param b
     * @return return true if files have identical content
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean writeBinDiff(File a, File b, File out) throws FileNotFoundException, IOException {
        InputStream as = new FileInputStream(a);
        InputStream bs = new FileInputStream(b);
        FileOutputStream fos = new FileOutputStream(out);
        int fromA = 0;
        int fromB = 0;
        boolean endA = false;
        boolean endB = false;
        boolean identical = true;
        while (true) {
            fromA = as.read();
            if (-1 == fromA) {
                endA = true;
                fromA = 0;
            }
            fromB = bs.read();
            if (-1 == fromB) {
                endB = true;
                fromB = 0;
            }
            if (endA & endB) break;
            int diff = fromA ^ fromB;
            if (0 != diff) identical = false;
            fos.write(diff);
        }
        fos.close();
        return identical;
    }

    /**
     * Return a hard copy of the input File object
     * Here "File" means <code>java.lang.Object.File</code>, not the content of a file.
     * To copy the content of a file, see <code>copyFile</code>
     * @param file File object to copy
     * @return the copy object
     */
    public static File hardCopy(File file) {
        File trial = null;
        try {
            trial = new File(file.getCanonicalPath());
        } catch (Throwable ex) {
        }
        return trial;
    }

    /**
     * Copy content of file src in file dst
     * @param dst output file
     * @param src input file
     * @param append if true, then src will be written to the end of dst rather than the beginning
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void copyFile(File dst, File src, boolean append) throws FileNotFoundException, IOException {
        dst.createNewFile();
        FileChannel in = new FileInputStream(src).getChannel();
        FileChannel out = new FileOutputStream(dst).getChannel();
        long startAt = 0;
        if (append) startAt = out.size();
        in.transferTo(startAt, in.size(), out);
        out.close();
        in.close();
    }

    /**
     * Copy content of file src in file dst with base64 encoding/decoding
     * @param dst output file
     * @param src input file
     * @param append if true, then src will be written to the end of dst rather than the beginning
     * @param enc64 if true, src bytes are encoded into base64 before copying to dst (dst is base64 encoded)
     * @param dec64 if true, src bytes are decoded from base64 to binary before copying to dst (src is base64 encoded)
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void copyFile(File dst, File src, boolean append, boolean enc64, boolean dec64) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(dst, append);
        AStreamUtilities.appendBinFile(out, src, enc64, dec64);
        out.close();
    }

    /**
     * Copy content of file src in file dst, with AES encryption/decryption
     * @param dst
     * @param src
     * @param append if true, then src will be written to the end of dst rather than the beginning
     * @param key a valid AES key (128, 192 or 256 bits)
     * @param encrypt if true, perform encryption, otherwise decryption
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidKeyException
     */
    public static void copyFile(File dst, File src, boolean append, byte[] key, boolean encrypt) throws FileNotFoundException, IOException, InvalidKeyException {
        FileOutputStream out = new FileOutputStream(dst, append);
        AStreamUtilities.appendBinFile(out, src, key, encrypt);
        out.close();
    }

    /**
     * Copy content of file src in file dst
     * @param dst
     * @param src
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void copyFile(File dst, File src) throws FileNotFoundException, IOException {
        copyFile(dst, src, false);
    }

    /**
     * Append content of file src to file dst
     * @param dst
     * @param src
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void appendFile(File dst, File src) throws FileNotFoundException, IOException {
        copyFile(dst, src, true);
    }

    public static String generateValidFileName(String fileName) {
        fileName = fileName.replace(':', '_');
        fileName = fileName.replace('\\', '_');
        fileName = fileName.replace('/', '_');
        fileName = fileName.replace('*', '_');
        return fileName;
    }

    public static String appendToFileName(File file, String toInsert) throws IOException {
        String path = file.getParentFile().getCanonicalPath();
        String baseName = file.getName();
        String ext = AFileUtilities.extractFileExtension(baseName);
        if (false == ext.isEmpty()) {
            baseName = baseName.substring(0, baseName.length() - ext.length() - 1);
        }
        String pathSeparator = System.getProperty("file.separator");
        StringBuilder name = new StringBuilder();
        name.append(path);
        name.append(pathSeparator);
        name.append(baseName);
        name.append(toInsert);
        name.append(".");
        name.append(ext);
        return name.toString();
    }

    /**
     * Retrieve the code base necessary to make <code>theClass</code> available to a rmi server
     * To be able to locate the class, this method needs a resource in the same package as the target class
     * If there is no resource in the package, a dummy empty file can do.
     * @param theClass
     * @param aResourceInTheSamePackage
     * @return the
     */
    public static String getRmiCodeBase(Class theClass, String aResourceInTheSamePackage) {
        String location;
        location = getPackageUrl(theClass, aResourceInTheSamePackage);
        if (null != location) location = location.substring(0, location.length() - theClass.getPackage().getName().length() - 1);
        return location;
    }

    public static String getPackageUrl(Class theClass, String aResourceInTheSamePackage) {
        String location;
        try {
            String packageName = theClass.getPackage().getName();
            String modifiedPackageName = packageName.replace('.', '/');
            modifiedPackageName += "/";
            URL url = theClass.getClassLoader().getResource(modifiedPackageName + aResourceInTheSamePackage);
            location = url.toString();
            location = location.substring(0, location.length() - aResourceInTheSamePackage.length());
        } catch (Exception ex) {
            location = null;
        }
        return location;
    }

    public static String getPackagePath(Class theClass, String aResourceInTheSamePackage) {
        String location;
        location = getPackageUrl(theClass, aResourceInTheSamePackage);
        if (null != location) {
            location = location.replace("%20", " ");
            if (location.startsWith("file:/")) location = location.substring(6); else {
                String separator = "/";
                String start = "jar:file:" + separator;
                if (location.startsWith(start)) {
                    location = location.substring(10);
                    int jarNamePos = location.lastIndexOf(".jar!" + separator);
                    jarNamePos = location.lastIndexOf(separator, jarNamePos);
                    location = location.substring(0, jarNamePos + 1);
                }
            }
        }
        return location;
    }

    /**
     * Add a subdirectory or a filename to a root path, detecting if a file separator
     * (eg. '\' on windows and '/' on unix) needs to be inserted or not
     * @param root the root path
     * @param toAdd a subdirectory name or a file name
     * @return a String with root and toAdd properly appended
     */
    public static String appendToPath(String root, String toAdd) {
        String separator = System.getProperty("file.separator");
        if (root.endsWith(separator)) return root + toAdd; else return root + separator + toAdd;
    }

    /**
     * Extract the extension out of a file name
     * The extension is defined here as the part of the file name after the last
     * occurence of the "." character. if there is no "." character in the file name,
     * then the extension is an empty String.
     *
     * @param fileName the string to process
     *
     * @return the extension found in this file name.
     */
    public static String extractFileExtension(String fileName) {
        String out = "";
        int i = fileName.lastIndexOf('.');
        if ((i != -1) && (i + 1 < fileName.length())) {
            out = fileName.substring(i + 1);
        }
        return out;
    }

    public static CharSequence file2CharSequence(File inputFile) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        CharSequence out = AStreamUtilities.stream2CharSequence(in, Charset.defaultCharset());
        in.close();
        return out;
    }

    public static CharSequence file2CharSequence(File inputFile, Charset inCharset) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        CharSequence out = AStreamUtilities.stream2CharSequence(in, inCharset);
        in.close();
        return out;
    }

    public static byte[] file2Bytes(File inputFile) throws FileNotFoundException, IOException {
        if (inputFile.length() > Integer.MAX_VALUE) throw new RuntimeException();
        byte[] out = new byte[(int) inputFile.length()];
        FileInputStream in = new FileInputStream(inputFile);
        try {
            in.read(out);
        } finally {
            in.close();
        }
        return out;
    }

    public static void binFile2HexFile(File inputFile, File outputFile) throws FileNotFoundException, IOException {
        FileWriter writer = new FileWriter(outputFile);
        FileInputStream in = new FileInputStream(inputFile);
        int byteCnt = 0;
        String endl = AStringUtilities.systemNewLine;
        try {
            while (true) {
                int bin = in.read();
                if (-1 == bin) break;
                String hex = AStringUtilities.byteToHex(bin);
                writer.write(hex);
                byteCnt++;
                if (16 == byteCnt) {
                    byteCnt = 0;
                    writer.write(endl);
                }
            }
        } finally {
            writer.close();
            in.close();
        }
    }

    public static void binFile2base64File(File inputFile, File outputFile) throws FileNotFoundException, IOException {
        FileOutputStream binOut = new FileOutputStream(outputFile);
        FileInputStream in = new FileInputStream(inputFile);
        OutputStream out = new Base64OutputStream(binOut);
        try {
            int nRead;
            byte[] binBytes = new byte[1024];
            while (true) {
                nRead = in.read(binBytes);
                if (-1 == nRead) break;
                out.write(binBytes, 0, nRead);
            }
        } finally {
            out.close();
            in.close();
        }
    }

    static void base64File2HexFile(File inputFile, File outputFile) throws IOException {
        FileWriter writer = new FileWriter(outputFile);
        FileInputStream inBin = new FileInputStream(inputFile);
        InputStream in = new Base64InputStream(inBin);
        int byteCnt = 0;
        String endl = AStringUtilities.systemNewLine;
        try {
            while (true) {
                int bin = in.read();
                if (-1 == bin) break;
                String hex = AStringUtilities.byteToHex(bin);
                writer.write(hex);
                byteCnt++;
                if (16 == byteCnt) {
                    byteCnt = 0;
                    writer.write(endl);
                }
            }
        } finally {
            writer.close();
            in.close();
        }
    }

    public static byte[] hexFile2Bytes(File inputFile) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        byte[] out = null;
        try {
            out = AStreamUtilities.hexStream2Bytes(in, Charset.defaultCharset());
        } finally {
            in.close();
        }
        return out;
    }

    public static byte[] hexFile2Bytes(File inputFile, Charset inCharset) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(inputFile);
        byte[] out = AStreamUtilities.hexStream2Bytes(in, inCharset);
        in.close();
        return out;
    }

    /**
     * Remove all files and directory within the targetDirectory (recursive)
     * @param targetDirectory
     * @return number of deleted files
     */
    public static int removeAll(String targetDirectory) throws IOException {
        File targetDir = new File(targetDirectory);
        return removeAll(targetDir);
    }

    /**
     * Remove all files and directory within the targetDirectory (recursive)
     * @param targetDirectory
     * @return number of deleted files
     */
    public static int removeAll(File targetDirectory) throws IOException {
        if (!targetDirectory.isDirectory()) throw new RuntimeException(targetDirectory.getCanonicalPath() + " is not a directory");
        File[] toDelete = targetDirectory.listFiles();
        int deleteCnt = 0;
        for (int i = 0; i < toDelete.length; i++) {
            if (toDelete[i].isFile()) {
                toDelete[i].delete();
                deleteCnt++;
            } else {
                deleteCnt += removeAll(toDelete[i]);
                toDelete[i].delete();
            }
        }
        return deleteCnt;
    }

    public static void removeDirectory(File file) throws IOException {
        removeAll(file);
        file.delete();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
    }

    /**
     * copy a folder recursively, if source directory is empty, dst folder is not created
     * If source is not empty and dst does not exist, it is created, along with non existent parent directories.
     * @param dst destination folder
     * @param src source folder
     * @throws IOException 
     */
    public static void copyFolder(File dst, File src) throws IOException {
        File[] list = src.listFiles();
        if (0 == list.length) return;
        dst.mkdirs();
        for (File f : list) {
            File d = (new File(dst, f.getName())).getCanonicalFile();
            if (f.isDirectory()) {
                f.mkdir();
                copyFolder(d, f);
            } else {
                copyFile(d, f);
            }
        }
    }
}
