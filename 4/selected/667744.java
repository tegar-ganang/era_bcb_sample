package jamm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZipCreator is a facade type class to add in the creation of a zip file.
 * It is generally better to pass the full filename to the add method.
 * Example code:
 * <pre>
 *      ZipCreator zc = new ZipCreator("/tmp/dude.test.zip");
 *      zc.open();
 *      zc.add("./build.xml");
 *      zc.add("NEWS");
 *      zc.add("README");
 *      zc.add("/usr/src/linux-2.4.19.tar.bz2");
 *      zc.close();
 * </pre>
 * If you use setBaseDirectory, if a file being added matches the base
 * directory, when the file is stored in the zip file, the base
 * directory will be stripped off the filename in the zip file.
 */
public class ZipCreator {

    /**
     * Creates a new <code>ZipCreator</code> instance.
     *
     * @param filename The file name of the new zip file.
     */
    public ZipCreator(String filename) {
        this(new File(filename));
    }

    /**
     * Creates a new <code>ZipCreator</code> instance.
     *
     * @param file a File object for the new zip file.
     */
    public ZipCreator(File file) {
        mFile = file;
    }

    /**
     * Open the new zip file for writing.
     *
     * @exception FileNotFoundException if an error occurs
     */
    public void open() throws FileNotFoundException {
        OutputStream os = new FileOutputStream(mFile);
        mZos = new ZipOutputStream(os);
    }

    /**
     * Add a file or directory to the zip file.  Note: directory adds
     * will be recursive.
     *
     * @param name The name of the file or directory to be added.
     * @exception IOException if an error occurs
     */
    public void add(String name) throws IOException {
        add(new File(name));
    }

    /**
     * Add a file or directory to the zip file.  Note: directory adds
     * will be recursive.
     *
     * @param file the File object representing the file or directory to add.
     * @exception IOException if an error occurs
     */
    public void add(File file) throws IOException {
        if (file.isDirectory()) {
            addDirectory(file);
        } else {
            addFile(file);
        }
    }

    /**
     * Helper method to get a filename in Zip that is clean and sane.
     *
     * @param file the File object to make a ZipName for.
     * @return a String containing the name.
     */
    private String getZipName(File file) {
        String parent = file.getParent();
        String zipName;
        if (parent == null || parent.equals("null") || parent.equals(".")) {
            zipName = file.getName();
        } else {
            zipName = parent + File.separator + file.getName();
        }
        if (mBaseDirectory != null) {
            if (zipName.startsWith(mBaseDirectory)) {
                zipName = zipName.substring(mBaseDirectory.length());
            }
        }
        if (zipName.charAt(0) == '/') {
            zipName = zipName.substring(1);
        }
        if (file.isDirectory()) {
            zipName = zipName + '/';
        }
        return zipName;
    }

    /**
     * Adds a file to the zip file.
     *
     * @param aFile the file to add to the zip file.
     * @exception IOException if an error occurs
     */
    private void addFile(File aFile) throws IOException {
        if (aFile.isDirectory()) {
            throw new IOException(aFile.getName() + " is a directory.");
        }
        String zipName = getZipName(aFile);
        ZipEntry ze = new ZipEntry(zipName);
        ze.setTime(aFile.lastModified());
        if (aFile.length() < 51) {
            mZos.setMethod(ZipOutputStream.STORED);
            ze.setSize(aFile.length());
            ze.setCompressedSize(aFile.length());
            ze.setCrc(calcCRC32(aFile));
        } else {
            mZos.setMethod(ZipOutputStream.DEFLATED);
        }
        mZos.putNextEntry(ze);
        byte[] buf = new byte[2048];
        int read;
        InputStream is = new FileInputStream(aFile);
        while ((read = is.read(buf, 0, buf.length)) > -1) {
            mZos.write(buf, 0, read);
        }
        is.close();
    }

    /**
     * Helper method to calculate the crc32 on a file
     *
     * @param file the file to get the crc on
     * @return a long of the crc result
     * @exception IOException if an error occurs
     */
    private long calcCRC32(File file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buf = new byte[2048];
        int read;
        InputStream is = new FileInputStream(file);
        while ((read = is.read(buf, 0, buf.length)) > -1) {
            crc.update(buf, 0, read);
        }
        is.close();
        return crc.getValue();
    }

    /**
     * This function is recursive.  It will add the entire directory tree
     * to the zip file.
     *
     * @param dir the directory to add to the zip file
     * @exception IOException if an error occurs
     */
    private void addDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(dir.getName() + " is not a directory.");
        }
        String zipName = getZipName(dir);
        ZipEntry ze = new ZipEntry(zipName);
        ze.setTime(dir.lastModified());
        ze.setSize(0);
        ze.setCompressedSize(0);
        ze.setCrc(0);
        mZos.setMethod(ZipOutputStream.STORED);
        mZos.putNextEntry(ze);
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isDirectory()) {
                    addDirectory(contents[i]);
                } else {
                    addFile(contents[i]);
                }
            }
        }
    }

    /**
     * Close the zip file.
     *
     * @exception IOException if an error occurs
     */
    public void close() throws IOException {
        mZos.close();
    }

    /**
     * Sets the base directory to be stripped off when putting into a
     * zip file.
     *
     * @param baseDirectory a string with the base dir
     */
    public void setBaseDirectory(String baseDirectory) {
        mBaseDirectory = baseDirectory;
    }

    /**
     * Get the base directory.
     *
     * @return a string with the base dir.
     */
    public String getBaseDirectory() {
        return mBaseDirectory;
    }

    /** A File object representing the zip file. */
    private File mFile;

    /** the zip output stream we're writing to. */
    private ZipOutputStream mZos;

    /** A base directory to be ignored. */
    private String mBaseDirectory;
}
