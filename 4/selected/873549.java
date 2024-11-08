package org.xrn.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/***
 * A helper static class, that enables, for instance to copy resources from the
 * classpath.
 *
 * @author Edouard Mercier
 * @version 1.0 : 2002.11.27
 */
public abstract class JarHelper {

    private static final int BUFFER_SIZE = 1024;

    /***
     * Copies the content of the file that should be present in the jar, into
     * the provided local file. If the file could not be found, it is searched
     * for in the provided alternative directory.<br/>
     * Note that the original file is not supposed to be a text file.<br/>
     * If the directory path to the new file does not exist, it is created.
     *
     * @param fileNameInsideJar the resource file name of the file inside the
     * jar
     * @param alternativeDirectoryPath the directory path used if the file
     * cannot be found within the jar; if set to null, this argument is ignored
     * @param copyFile the file that will be the copy of the original one
     * @return <code>true</code> if the copy was OK
     * @see #copyInputStreamIntoFile
     */
    public static boolean copyFileFromJar(final String fileNameInsideJar, final String alternativeDirectoryPath, final File copyFile) {
        ClassLoader classLoader = JarHelper.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileNameInsideJar);
        if ((inputStream == null) && (alternativeDirectoryPath != null)) {
            File alternativeDirectory = new File(alternativeDirectoryPath);
            try {
                inputStream = new FileInputStream(new File(alternativeDirectory, fileNameInsideJar));
            } catch (FileNotFoundException fileNotFoundException) {
                return false;
            }
        }
        return copyInputStreamIntoFile(inputStream, copyFile);
    }

    /***
     * Copies the content of the input stream into the given file. If the
     * directory path to the new file does not exist, it is created.
     *
     * @param inputStream the input stream, from which the data are copied;
     * <b>note that the input stream will be closed if this method returns
     * <code>true</code></b>. The special cas when it is null is also handled
     * @param copyFile the file in which the content of the input stream should
     * be copied
     * @return <code>true</code> if no problem during the copy occured
     */
    public static boolean copyInputStreamIntoFile(InputStream inputStream, File copyFile) {
        if (inputStream == null) {
            return false;
        }
        try {
            int bufferSize = BUFFER_SIZE;
            byte[] byteArray = new byte[bufferSize];
            int readByteNumber = -1;
            File parentDirectory = copyFile.getParentFile();
            if (parentDirectory.isDirectory() == false) {
                if (parentDirectory.mkdirs() == true) {
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream(copyFile);
            while ((readByteNumber = inputStream.read(byteArray)) != -1) {
                fileOutputStream.write(byteArray, 0, readByteNumber);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
        } catch (IOException ioException) {
            return false;
        }
        return true;
    }
}
