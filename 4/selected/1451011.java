package moller.util.io;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import moller.util.string.StringUtil;

public class FileUtil {

    private static final String LS = System.getProperty("line.separator");

    /**
	 * This method copies a file.
	 *
	 * @param sourceFile is the file that shall be copied
	 * @param destinationFile is to which folder and name
	 *        the the sourceFile will be copied.
	 *
	 * @return a boolean value indication whether the file
	 *         copy was successful or not (true == success,
	 *         false == failure).
	 */
    public static boolean copyFile(File sourceFile, File destinationFile) {
        boolean copySuccessfull = false;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destinationFile).getChannel();
            long transferedBytes = destination.transferFrom(source, 0, source.size());
            copySuccessfull = transferedBytes == source.size() ? true : false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return copySuccessfull;
    }

    public static boolean copyFile(byte[] data, File destinationFile) {
        boolean copySuccessfull = false;
        FileChannel destination = null;
        try {
            destination = new FileOutputStream(destinationFile).getChannel();
            long transferedBytes = destination.write(ByteBuffer.wrap(data));
            copySuccessfull = transferedBytes == data.length ? true : false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return copySuccessfull;
    }

    /**
	 * This method copies the content of a InputStream to a file.
	 *
	 * @param source is the InputStream that has the content that shall be
	 *        copied.
	 * @param destinationFile is to which folder and name
	 *        the the source InputStream will be copied.
	 *
	 * @return a boolean value indicating whether the file
	 *         copy was successful or not (true == success,
	 *         false == failure).
	 *
	 * @throws IOException is thrown if it is not possible to read from the
	 *         BufferedInputStream.
	 */
    public static boolean copy(InputStream source, File destinationFile) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(source);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] buffer = new byte[8096];
        int result = bis.read(buffer);
        while (result != -1) {
            buf.write(buffer, 0, result);
            result = bis.read(buffer);
        }
        return copyFile(buf.toByteArray(), destinationFile);
    }

    /**
	 * This method calculates the size a file will occupy on disk when stored.
	 *
	 * @param file is the object that will have it�s actual size on disk
	 *        calculated.

	 * @param clusterSize is the smallest size used to store data by the file
	 *        system for the executing machine.
	 *
	 * @return the size in bytes that a file will actually occupy on disk when
	 *         stored.
	 */
    public static long getFileSizeOnDisk(File file, long clusterSize) {
        return getFileSizeOnDisk(file.length(), clusterSize);
    }

    /**
	 * This method calculates the size a file will occupy on disk when stored.
	 *
	 * @param fileSize is the size in bytes for a file.
	 *
	 * @param clusterSize is the smallest size used to store data by the file
	 *        system for the executing machine.
	 *
	 * @return the size in bytes that a file will actually occupy on disk when
	 *         stored.
	 */
    public static long getFileSizeOnDisk(long fileSize, long clusterSize) {
        long clustersNeeded = fileSize / clusterSize;
        if (fileSize % clusterSize != 0) {
            clustersNeeded += 1;
        }
        return clustersNeeded * clusterSize;
    }

    /**
	 * This method creates empty files on the file system, according the paths
	 * in each file object in the java.io.File Collection sent to this method.
	 *
	 * @param files is a Collection of java.io.File objects.
	 *
	 * @return whether the creation of all files was successful. True means
	 *         success, false means failure.
	 */
    public static boolean createFiles(Collection<File> files) {
        for (File file : files) {
            if (!createFile(file)) {
                return false;
            }
        }
        return true;
    }

    /**
	 * This method create an empty file on the file system according to the
	 * path in the input File object.
	 *
	 * @param file is the java.io.File object that shall be created on the
	 *        file system according to the path in the File object.
	 *
	 * @return whether the creation of the file was successful. True means
	 *         success, false means failure.
	 */
    public static boolean createFile(File file) {
        try {
            if (!file.createNewFile()) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
	 * This method writes a string to a file either by appending the file
	 * content or by writing the string from the beginning of the file.
	 *
	 * @param f is the file object to write to.
	 * @param text contains the string that will be written to the file f
	 *        object
	 * @param append decides whether the string in the text object will be
	 *        appended to the file object f or put to the start of the file.
	 *
	 * @throws IOException if the file does not exist or is a directory or by
	 *         some other reason is impossible to write to.
	 */
    public static void writeToFile(File f, String text, boolean append) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, append));
        bw.write(text);
        bw.close();
    }

    /**
	 * This method writes a set of strings which are transformed into a string
	 * with line separator characters added between each string to a file,
	 * either by appending the file content or by writing the string from the
	 * beginning of the file.
	 *
	 * @param f is the file object to write to.
	 * @param texts contains the strings that will be written to the file f
	 *        object
	 * @param append decides whether the strings in the texts object will be
	 *        appended to the file object f or put to the start of the file.
	 *
	 * @throws IOException if the file does not exist or is a directory or by
	 *         some other reason is impossible to write to.
	 */
    public static void writeToFile(File f, Set<String> texts, boolean append) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String text : texts) {
            sb.append(text + LS);
        }
        writeToFile(f, sb.toString(), append);
    }

    /**
	 * This method writes a string to a file either by appending the file
	 * content or by writing the string from the beginning of the file.
	 *
	 * @param f is the file object to write to.
	 * @param texts contains the strings that will be written to the file f
	 *        object
	 * @param append decides whether the string in the text object will be
	 *        appended to the file object f or put to the start of the file.
	 *
	 * @throws IOException if the file does not exist or is a directory or by
	 *         some other reason is impossible to write to.
	 */
    public static void writeToFile(File f, List<String> texts, boolean append) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String text : texts) {
            sb.append(text + LS);
        }
        writeToFile(f, sb.toString(), append);
    }

    /**
	 * This method writes a set of strings which are transformed into a string
	 * with line separator characters added between each string to a file,
	 * either by appending the file content or by writing the string from the
	 * beginning of the file.
	 *
	 * @param f is the file object to write to.
	 * @param texts contains the strings that will be written to the file f
	 *        object
	 * @param append decides whether the strings in the texts object will be
	 *        appended to the file object f or put to the start of the file.
	 *
	 * @throws IOException if the file does not exist or is a directory or by
	 *         some other reason is impossible to write to.
	 */
    public static void writeToFile(File f, Iterator<Object> texts, boolean append) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (texts.hasNext()) {
            sb.append(texts.next() + LS);
        }
        writeToFile(f, sb.toString(), append);
    }

    /**
	 * This method writes a set of strings which are transformed into a string
	 * with line separator characters added between each string to a file,
	 * either by appending the file content or by writing the string from the
	 * beginning of the file.
	 *
	 * @param f is the file object to write to.
	 *
	 * @param texts contains the strings that will be written to the file f
	 *        object
	 *
	 * @param append decides whether the strings in the texts object will be
	 *        appended to the file object f or put to the start of the file.
	 *
	 * @param splitTextToken defines a token that will be used to split the
	 *        strings contained in the texts object.
	 *
	 * @param useFirstPart defines whether the first or second part of the
	 *        strings in the texts object will be used.
	 *
	 * @throws IOException if the file does not exist or is a directory or by
	 *         some other reason is impossible to write to.
	 */
    public static void writeToFile(File f, Iterator<Object> texts, boolean append, String splitTextToken, boolean useFirstPart) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (texts.hasNext()) {
            String text = texts.next().toString();
            text = StringUtil.reverse(text);
            sb.append(StringUtil.reverse(text.split(splitTextToken, 2)[useFirstPart ? 1 : 0]));
            if (texts.hasNext()) {
                sb.append(LS);
            }
        }
        writeToFile(f, sb.toString(), append);
    }

    /**
	 * This method downloads a file from an URL and saves it to a File object
	 *
	 * @param source is the URL from which the file shall be downloaded.
	 * @param destination is to which end point the file in the source
	 *        parameter shall be downloaded.
	 * @param fileSize is the size in bytes for the file that shall be
	 *        downloaded. This size will be used to set the max value of the
	 *        progressbar.
	 * @param bufferSize is the size in kilobytes that will be used as buffer
	 *        when the file is downloaded.
	 * @param centerPoint defines the center point that the progressbar shall
	 *        have.
	 *
	 * @throws IOException is thrown when the destination file is not possible
	 *         to create, when it is not possible to open a stream to the
	 *         source, when it is not possible to read from the source stream
	 *         or when it is not possible to write to the destination file.
	 */
    public static void downloadAndSaveFile(URL source, File destination, int fileSize, int bufferSize, Point centerPoint) throws IOException {
        OutputStream destinationFile = null;
        InputStream fileStream = null;
        try {
            destinationFile = new FileOutputStream(destination);
            fileStream = source.openStream();
            byte[] buf = new byte[bufferSize * 1024];
            int bytesRead = 0;
            while ((bytesRead = fileStream.read(buf)) != -1) {
                destinationFile.write(buf, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            StreamUtil.closeStream(fileStream);
            StreamUtil.closeStream(destinationFile);
        }
    }

    /**
	 * This method tests whether a file is of the of a specific type.
	 *
	 * @param extension holds a String representation of a file type, for
	 *        instance: "jpg", "jpeg" and so on.
	 *
	 * @param file is the object that shall be tested regarding the file type.
	 *
	 * @return a boolean value indicating whether the parameter "file" has the
	 *         same extension as the parameter "extension" has. If that is
	 *         true, then true is returned, otherwise false is returned.
	 */
    public static boolean isOfType(String extension, File file) {
        String fileName = file.getName();
        String fileSuffix = "";
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return false;
        } else {
            fileSuffix = fileName.substring(index, fileName.length());
        }
        return fileSuffix.equalsIgnoreCase(extension);
    }

    /**
	 * This method reads the content of a text file and returns the content as
	 * a List of Strings
	 *
	 * @param file is the source to read from.
	 * @return a List consisting of the contents found in the file specified by
	 *         the file parameter.
	 * @throws IOException
	 */
    public static List<String> readFromFile(File file) throws IOException {
        List<String> content = new ArrayList<String>(32);
        String line = null;
        BufferedReader input = new BufferedReader(new FileReader(file));
        try {
            while ((line = input.readLine()) != null) {
                content.add(line);
            }
        } finally {
            input.close();
        }
        return content;
    }

    /**
	 * @param file
	 * @param simpleDateFormatString
	 * @return
	 */
    public static String getLatestModified(File file, String simpleDateFormatString) {
        SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormatString);
        Date date = new Date(file.lastModified());
        return sdf.format(date);
    }

    /**
	 * @param file
	 * @return
	 */
    public static String getLatestModifiedDate(File file) {
        return getLatestModified(file, "yyyy-MM-dd");
    }

    /**
	 * @param file
	 * @return
	 */
    public static String getLatestModifiedTime(File file) {
        return getLatestModified(file, "HH-mm-ss");
    }

    /**
	 * @param theFileToZip
	 * @throws IOException
	 */
    public static void zipTheFile(File theFileToZip) throws IOException {
        byte[] buf = new byte[8096];
        File zipFile = new File(theFileToZip.getParent(), theFileToZip.getName() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        FileInputStream fis = new FileInputStream(theFileToZip);
        zos.putNextEntry(new ZipEntry(theFileToZip.getName()));
        int lenght;
        while ((lenght = fis.read(buf)) > 0) {
            zos.write(buf, 0, lenght);
        }
        zos.closeEntry();
        fis.close();
        zos.close();
    }

    public static boolean testWriteAccess(File directory) {
        File test = new File(directory, System.currentTimeMillis() + "javapeg-test.txt");
        if (FileUtil.createFile(test)) {
            return test.delete();
        } else {
            return false;
        }
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = null;
        byte[] bytes;
        try {
            is = new FileInputStream(file);
            bytes = new byte[(int) file.length()];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return bytes;
    }
}
