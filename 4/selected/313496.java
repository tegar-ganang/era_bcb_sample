package adamb.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import java.io.*;

public class FileInsert {

    private byte[] chunk;

    public FileInsert(int readChunkSize) {
        assert readChunkSize > 0;
        chunk = new byte[readChunkSize];
    }

    /**
	 Insert, append, overwrite, or delete an interval of the given file.
	 
	 The simplest way to view this operation is:
	 1) the specified interval is deleted from the file (leaving no holes)
	 2) the new data is inserted at the start of the interval (extending the file length as needed)
	 The algorithm used is more efficient, however.
	 
	 All bytes on the interval [from, to) will be lost (unless from == to in which case no data will be lost).
	 
	 @param file an open file.  The file will <b>not</b> be closed.
	 @param from the file byte position to start insertion at.  Must be <= to the file length. (Equal implies an append)
	 @param to file position of the last byte to overwrite (exclusive).  If from == to then the operation will be purely an insert (no deleted data).  Must not be greater than the file size.
	 @param newData byte array containing the new data
	 @param offset the offset within newData to begin reading from
	 @param len the number of bytes to read from newData.  Pass 0 to delete the file interval
	 */
    public void insert(RandomAccessFile file, long from, long to, byte[] newData, int offset, int len) throws IOException {
        long fileLen = file.length();
        if (from < 0 || from > to || from > fileLen || to > fileLen) throw new IllegalArgumentException("Invalid insertion interval!: from=" + from + " to=" + to + " (file size=" + fileLen + ")");
        if (len > 0) {
            byte b = newData[offset];
            b = newData[offset + len - 1];
        }
        long intervalSize = to - from;
        long excess = len - intervalSize;
        if (excess > 0) {
            int nRead;
            boolean stop = false;
            int chunkSize = chunk.length;
            long lastPos = fileLen;
            while (!stop) {
                lastPos -= chunkSize;
                if (lastPos <= to) {
                    chunkSize = (int) ((lastPos + chunkSize) - to);
                    lastPos = to;
                    stop = true;
                }
                file.seek(lastPos);
                nRead = readCompletely(file, chunk, chunkSize);
                if (nRead != chunkSize) throw new IOException("Unexpected read shortage: " + nRead + " bytes instead of " + chunkSize + '!');
                file.seek(lastPos + excess);
                file.write(chunk, 0, chunkSize);
            }
        } else if (excess < 0) {
            long lastPos = to;
            int nRead;
            while (true) {
                file.seek(lastPos);
                nRead = file.read(chunk);
                if (nRead == -1) break;
                file.seek(lastPos + excess);
                file.write(chunk, 0, nRead);
                lastPos += nRead;
            }
            file.setLength(fileLen + excess);
        }
        file.seek(from);
        file.write(newData, offset, len);
    }

    /**block until the given byte array can be filled from the stream
	 @return the number bytes actually read.  This will only be less than the length
	 of the byte array if the end of the stream is reached.
	 */
    private int readCompletely(RandomAccessFile is, byte[] bytes, int numToRead) throws IOException {
        int nRead = is.read(bytes, 0, numToRead);
        while (nRead < numToRead) {
            if (nRead == -1) return 0; else nRead += is.read(bytes, nRead, numToRead - nRead);
        }
        return nRead;
    }

    public static class Tester {

        private final File tstFile = new File("delete_this_file.bin");

        /**tests for bad argument exceptions*/
        @Test
        public void testArgExceptions() throws IOException {
            FileInsert fi = new FileInsert(1);
            makeTestFile(tstFile, 10);
            RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
            byte[] data = new byte[5];
            try {
                fi.insert(raf, -1, 1, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 2, 1, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 0, -1, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 11, 5, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 5, 11, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 11, 11, data, 0, data.length);
                assertTrue(false);
            } catch (IllegalArgumentException ia) {
                assertTrue(true);
            }
            try {
                fi.insert(raf, 1, 2, data, -1, data.length);
                assertTrue(false);
            } catch (ArrayIndexOutOfBoundsException e) {
                assertTrue(isTestFileGood(tstFile, "", 0, 0));
            }
            try {
                fi.insert(raf, 1, 2, data, 0, data.length + 1);
                assertTrue(false);
            } catch (ArrayIndexOutOfBoundsException e) {
                assertTrue(isTestFileGood(tstFile, "", 0, 0));
            }
            try {
                fi.insert(raf, 1, 2, data, 1, data.length);
                assertTrue(false);
            } catch (ArrayIndexOutOfBoundsException e) {
                assertTrue(isTestFileGood(tstFile, "", 0, 0));
            }
            raf.close();
        }

        @Test
        public void insertTest() {
            try {
                {
                    FileInsert fi = new FileInsert(1);
                    makeTestFile(tstFile, 0);
                    RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
                    fi.insert(raf, 0, 0, new byte[0], 0, 0);
                    raf.close();
                    assertTrue(getFileBytes(tstFile).length == 0);
                    makeTestFile(tstFile, 0);
                    raf = new RandomAccessFile(tstFile, "rw");
                    fi.insert(raf, 0, 0, new byte[] { Byte.MIN_VALUE }, 0, 1);
                    raf.close();
                    byte[] bytes = getFileBytes(tstFile);
                    assertTrue(bytes.length == 1 && bytes[0] == Byte.MIN_VALUE);
                }
                int[] chunkSizes = new int[] { 1, 2, 3, 10, 100, 1000, 4096 };
                int[] fileSizes = new int[] { 9, 10, 100, 1000, 4096, 10001 };
                System.out.println("Testing " + (chunkSizes.length * fileSizes.length * 3) + " combinations of FileInsert");
                for (int fileSize : fileSizes) {
                    for (int chunkSize : chunkSizes) {
                        FileInsert fi = new FileInsert(chunkSize);
                        for (FileSection section : java.util.EnumSet.allOf(FileSection.class)) {
                            {
                                helper(fi, fileSize, section, 3, "CB");
                                helper(fi, fileSize, section, fileSize / 3, "CBA");
                            }
                            {
                                helper(fi, fileSize, section, 2, "BA");
                                helper(fi, fileSize, section, 2, "CBA");
                            }
                            {
                                helper(fi, fileSize, section, 0, "CB");
                                helper(fi, fileSize, section, 1, "BA");
                                helper(fi, fileSize, section, 1, "CBA");
                                helper(fi, fileSize, section, 2, "CBA");
                            }
                        }
                        {
                            makeTestFile(tstFile, fileSize);
                            RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
                            fi.insert(raf, 0, fileSize, new byte[0], 0, 0);
                            raf.close();
                            assertTrue(tstFile.length() == 0);
                            makeTestFile(tstFile, fileSize);
                            raf = new RandomAccessFile(tstFile, "rw");
                            fi.insert(raf, 0, fileSize, new byte[] { 65 }, 0, 1);
                            raf.close();
                            byte[] bytes = getFileBytes(tstFile);
                            assertTrue(bytes.length == 1 && bytes[0] == 65);
                            makeTestFile(tstFile, fileSize);
                            raf = new RandomAccessFile(tstFile, "rw");
                            bytes = new byte[fileSize];
                            for (int i = 0; i < fileSize; i++) bytes[i] = Util.ubyte((255 - (i % 255)));
                            fi.insert(raf, 0, fileSize, bytes, 0, bytes.length);
                            raf.close();
                            bytes = getFileBytes(tstFile);
                            for (int i = 0; i < fileSize; i++) assertTrue(bytes[i] == Util.ubyte(255 - (i % 255)));
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        enum FileSection {

            BEG, MID, END
        }

        private void helper(FileInsert fi, int fileSize, FileSection section, int amountToOverwrite, String tokenSubstring) throws IOException {
            File f = tstFile;
            makeTestFile(f, fileSize);
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            int start;
            if (section == FileSection.BEG) start = 0; else if (section == FileSection.MID) start = fileSize / 3; else start = fileSize - amountToOverwrite;
            byte[] tokenData = "CBA".getBytes("UTF-8");
            int tokenOffset = "CBA".indexOf(tokenSubstring);
            fi.insert(raf, start, start + amountToOverwrite, tokenData, tokenOffset, tokenSubstring.length());
            raf.close();
            assertTrue(isTestFileGood(f, tokenSubstring, start, amountToOverwrite));
        }

        private boolean isTestFileGood(File file, String tokenStr, int expectedPosition, int numOverwritten) throws IOException {
            byte[] token = tokenStr.getBytes();
            int size = (int) file.length();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 2048);
            try {
                int start = Util.streamFind(bis, token);
                if (start != -1 && start == expectedPosition) {
                    bis.close();
                    bis = new BufferedInputStream(new FileInputStream(file), 2048);
                    int pos = 0;
                    for (int i = 0; i < start; i++) {
                        int val = bis.read();
                        int expectedVal = pos % 255;
                        if (val != expectedVal) return false; else pos++;
                    }
                    bis.skip(token.length);
                    pos += numOverwritten;
                    for (int i = start + token.length; i < size; i++) {
                        int val = bis.read();
                        int expectedVal = pos % 255;
                        if (val != expectedVal) return false; else pos++;
                    }
                    return true;
                } else return false;
            } finally {
                bis.close();
            }
        }

        private byte[] getFileBytes(File f) throws IOException {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f), 2048);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
            int b;
            while ((b = bis.read()) != -1) baos.write(b);
            bis.close();
            return baos.toByteArray();
        }

        private void makeTestFile(File file, int size) throws IOException {
            file.delete();
            file.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), 2048);
            for (int i = 0; i < size; i++) bos.write(i % 255);
            bos.close();
        }
    }
}
