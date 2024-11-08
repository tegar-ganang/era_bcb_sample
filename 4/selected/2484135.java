package org.happy.commons.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Arrays;
import javax.activation.DataHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.google.common.base.Preconditions;

/**
 * FileUtil class to work with files
 * @author Andreas Hollmann
 */
public class FileUtils_1x0 {

    /**
	 * writes content of the DataHandler to file
	 * 
	 * @param handler data handler, which has content
	 * @param file file which should be used store content
	 * @param append if true then the content will be appended to the end of the file
	 * @throws IOException
	 */
    public static void writeToFile(DataHandler handler, File file, boolean append) throws IOException {
        Preconditions.checkNotNull(handler, "data handler can't be null");
        Preconditions.checkNotNull(file, "file which should be used to store data can't be null!");
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IllegalArgumentException("file \"" + file.getCanonicalPath() + "\" could not be created!");
            }
        }
        FileOutputStream outStream = new FileOutputStream(file, append);
        try {
            handler.writeTo(outStream);
            outStream.flush();
        } finally {
            outStream.close();
        }
    }

    /**
	 * Closes InputStream and/or OutputStream. It makes sure that both streams
	 * tried to be closed, even first throws an exception.
	 * 
	 * @throw IOException if stream (is not null and) cannot be closed.
	 * 
	 */
    protected static void close(InputStream iStream, OutputStream oStream) throws IOException {
        try {
            if (iStream != null) {
                iStream.close();
            }
        } finally {
            if (oStream != null) {
                oStream.close();
            }
        }
    }

    private static final int BUFFSIZE = 1024;

    private static byte buff1[] = new byte[BUFFSIZE];

    private static byte buff2[] = new byte[BUFFSIZE];

    /**
	 * compares two input streams
	 * I used as reference the code posted by snoopyjc here: 
	 * http://www.velocityreviews.com/forums/t123770-re-java-code-for-determining-binary-file-equality.html
	 * @param is1 first input stream
	 * @param is2 second stream
	 * @return true if the binary content of both input streams is the same
	 */
    public static boolean isInputStreamContentEquals(InputStream is1, InputStream is2) {
        if (is1 == is2) return true;
        if (is1 == null && is2 == null) return true;
        if (is1 == null || is2 == null) return false;
        try {
            int read1 = -1;
            int read2 = -1;
            do {
                int offset1 = 0;
                while (offset1 < BUFFSIZE && (read1 = is1.read(buff1, offset1, BUFFSIZE - offset1)) >= 0) {
                    offset1 += read1;
                }
                int offset2 = 0;
                while (offset2 < BUFFSIZE && (read2 = is2.read(buff2, offset2, BUFFSIZE - offset2)) >= 0) {
                    offset2 += read2;
                }
                if (offset1 != offset2) return false;
                if (offset1 != BUFFSIZE) {
                    Arrays.fill(buff1, offset1, BUFFSIZE, (byte) 0);
                    Arrays.fill(buff2, offset2, BUFFSIZE, (byte) 0);
                }
                if (!Arrays.equals(buff1, buff2)) return false;
            } while (read1 >= 0 && read2 >= 0);
            if (read1 < 0 && read2 < 0) return true;
            return false;
        } catch (Exception ei) {
            return false;
        }
    }

    /**
	 * checks is the content of two files the same
	 * @param file01 
	 * @param file02
	 * @return true if the binary content of two files is equal
	 */
    public static boolean isFileContentsEquals(File file01, File file02) {
        InputStream is1 = null;
        InputStream is2 = null;
        if (file01.length() != file02.length()) return false;
        try {
            is1 = new FileInputStream(file01);
            is2 = new FileInputStream(file02);
            return isInputStreamContentEquals(is1, is2);
        } catch (Exception ei) {
            return false;
        } finally {
            try {
                if (is1 != null) is1.close();
                if (is2 != null) is2.close();
            } catch (Exception ei2) {
            }
        }
    }

    /**
	 *  checks is the content of two files the same
	 * @param file01URL path of the file1
	 * @param file02URL path of the file2
	 * @return true if the binary content of two files is equal
	 */
    public static boolean isFileContentsEquals(String file01URL, String file02URL) {
        return isFileContentsEquals(new File(file01URL), new File(file02URL));
    }

    /**
	 * creates or gets the folder with url
	 * if folder already exists then it will be deleted
	 * @return new created or getted folder
	 * @throws IOException 
	 */
    public static File getOrCreateDir(String url) throws IOException {
        File folder = new File(url);
        if (folder.exists()) {
            folder.delete();
        }
        FileUtils.forceMkdir(folder);
        return folder;
    }

    /**
	 * creates or gets the file with url
	 * if file already exists then it will be deleted
	 * @return new created or getted file
	 * @throws IOException 
	 */
    public static File getOrCreateFile(String url) throws IOException {
        File file = new File(url);
        if (!file.exists()) {
            String dirUrl = FilenameUtils.getFullPath(url);
            FileUtils.forceMkdir(new File(dirUrl));
            file.createNewFile();
        }
        Preconditions.checkArgument(!file.isDirectory());
        return file;
    }

    private static final long K = 1024;

    private static final long M = K * K;

    private static final long G = M * K;

    private static final long T = G * K;

    /**
	 * converts number of bytes to human readable form
	 * http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	 * @param value
	 * @return
	 */
    public static String convertBytesNumberToHumanReadableForm(final long value) {
        final long[] dividers = new long[] { T, G, M, K, 1 };
        final String[] units = new String[] { "TB", "GB", "MB", "KB", "B" };
        if (value < 1) throw new IllegalArgumentException("Invalid file size: " + value);
        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value, final long divider, final String unit) {
        final double result = divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }

    /**
	 * creates a child file in the directory, if the file is Created, then the file will be not created again, but the pointer to existing file will be returned
	 * @param dir directory which should be used to create a file
	 * @param fileName the name of the file
	 * @return
	 */
    public static File createChildFile(File dir, String fileName) {
        Preconditions.checkArgument(dir.exists(), "dir dowsn't exist");
        Preconditions.checkArgument(dir.isDirectory(), "dir shold be a directory");
        return new File(dir.getAbsolutePath() + File.separator + fileName);
    }

    /**
	 * copy the data from fin to fout by appending it to the fout
	 * used code from this example http://www.torsten-horn.de/techdocs/java-io.htm
	 * @param fin file with input-data
	 * @param fout file with output-data
	 * @param append if true then appends the data at the end
	 * @throws IOException
	 */
    public static void writeFileToFile(File fin, File fout, boolean append) throws IOException {
        FileChannel inChannel = new FileInputStream(fin).getChannel();
        FileChannel outChannel = new FileOutputStream(fout, append).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) try {
                inChannel.close();
            } catch (IOException ex) {
            }
            if (outChannel != null) try {
                outChannel.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
	 * writes data to the file
	 * @param bytes bytes which should be written to the file
	 * @param fout file to which the bytes will be written
	 * @param append if true then the bytes will be appended at the end of the file
	 * @throws IOException
	 */
    public static void writeBytesToFile(byte[] bytes, File fout, boolean append) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        FileOutputStream os = new FileOutputStream(fout, append);
        try {
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            os.flush();
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }
}
