package adamb.util;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Util {

    public static int calcHashCapacity(int estimatedMaxSize) {
        float size = estimatedMaxSize + 32;
        return (int) (size / 0.75f);
    }

    public static String getLeadingDigits(String str) {
        if (str.length() > 0) {
            int len = str.length();
            int i = 0;
            while (i < len) {
                if (Character.isDigit(str.charAt(i))) i++; else break;
            }
            return str.substring(0, i);
        } else return new String();
    }

    public static String getTrailingDigits(String str) {
        int i = str.length() - 1;
        while (i >= 0) {
            if (Character.isDigit(str.charAt(i))) i--; else break;
        }
        i++;
        if (i < str.length()) return str.substring(i); else return new String();
    }

    public static Integer getLeadingInteger(String s) {
        String digits = getLeadingDigits(s);
        if (digits.length() > 0) return new Integer(digits); else return null;
    }

    public static Integer getTrailingInteger(String s) {
        String digits = getTrailingDigits(s);
        if (digits.length() > 0) return new Integer(digits); else return null;
    }

    public static String getDelimitedIDs(Collection<Integer> ids, int avgIDLength, char delimiter) {
        StringBuilder sb = new StringBuilder(ids.size() * (avgIDLength + 1));
        boolean first = true;
        for (Integer id : ids) {
            if (!first) sb.append(delimiter); else first = false;
            sb.append(id.intValue());
        }
        return sb.toString();
    }

    public static String wrapText(String txt, char breakChar, String lineDelim, int numChars) {
        StringTokenizer tok = new StringTokenizer(txt, "" + breakChar);
        String piece;
        int lineLen = 0;
        StringBuilder sb = new StringBuilder(txt.length() + 16);
        while (tok.hasMoreTokens()) {
            piece = tok.nextToken();
            if (lineLen + piece.length() <= numChars) lineLen += piece.length(); else {
                sb.append(lineDelim);
                lineLen = 0;
            }
            sb.append(piece);
            if (tok.hasMoreTokens()) sb.append(breakChar);
        }
        return sb.toString();
    }

    public static int arrayIndexOf(int value, int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return -1;
    }

    /**Find a sub sequence within an array
	 @return -1 if not found*/
    public static int arrayIndexOf(byte[] array, byte[] subsequence, int fromIndex) {
        if (subsequence.length == 0) return 0; else {
            int j;
            int stop = (array.length - subsequence.length) + 1;
            while (fromIndex < stop) {
                if (array[fromIndex] == subsequence[0]) {
                    j = 1;
                    while (j < subsequence.length) {
                        if (array[fromIndex + j] == subsequence[j]) j++; else break;
                    }
                    if (j == subsequence.length) return fromIndex;
                }
                fromIndex++;
            }
            return -1;
        }
    }

    /**Find a single value within an array
	 @return -1 if not found*/
    public static int arrayIndexOf(byte[] array, byte value, int fromIndex) {
        while (fromIndex < array.length) {
            if (array[fromIndex] == value) return fromIndex; else fromIndex++;
        }
        return -1;
    }

    /**
	 shift List elements left or right.
	 As a result of this call indicies will be sorted in ascending order.
	 */
    public static void shiftIndicies(List list, int[] indicies, int shiftVector) {
        if (indicies.length > 0) {
            Arrays.sort(indicies);
            int listSize = list.size();
            if (shiftVector > 0) {
                while (shiftVector > 0) {
                    shiftVector--;
                    if (indicies[indicies.length - 1] == (listSize - 1)) break;
                    for (int i = indicies.length - 1; i >= 0; i--) {
                        swapListElements(list, indicies[i], indicies[i] + 1);
                        indicies[i]++;
                    }
                }
            } else if (shiftVector < 0) {
                while (shiftVector < 0) {
                    shiftVector++;
                    if (indicies[0] == 0) break;
                    for (int i = 0; i < indicies.length; i++) {
                        swapListElements(list, indicies[i], indicies[i] - 1);
                        indicies[i]--;
                    }
                }
            }
        }
    }

    public static void swapListElements(List list, int idx1, int idx2) {
        Object tmp = list.get(idx1);
        list.set(idx1, list.get(idx2));
        list.set(idx2, tmp);
    }

    public static String trimLastPart(String s, char partDelimChar) {
        int idx = s.lastIndexOf(partDelimChar);
        if (idx != -1) return s.substring(0, idx); else return s;
    }

    /**
	 Compare possibly null objects.
	 The comparison is made with Object.equals.  If both
	 objects are null they are considered equal.  If one is
	 null but the other is not they are considered different.
	 */
    public static boolean safeEquals(Object o1, Object o2) {
        return (o1 == null && o2 == null) || (o1 != null && o2 != null && o1.equals(o2));
    }

    public static <T> void buildDisjoint(Collection<T> a, Collection<T> b, Collection<T> aNotB, Collection<T> bNotA, Collection<T> aAndB) {
        if (aAndB == null) {
            int intersectionSize = Math.min(a.size(), b.size());
            aAndB = new ArrayList<T>(intersectionSize);
        } else aAndB.clear();
        aNotB.clear();
        bNotA.clear();
        for (T obj : a) {
            if (b.contains(obj)) aAndB.add(obj); else aNotB.add(obj);
        }
        for (T obj : b) {
            if (!aAndB.contains(obj)) bNotA.add(obj);
        }
    }

    public static int ubyte(byte b) {
        return b & 0xFF;
    }

    public static byte ubyte(int i) {
        assert i > -1 && i < 256;
        return (byte) i;
    }

    public static boolean startsWith(byte[] a, byte[] a2) {
        return intervalEquals(a, 0, a2);
    }

    public static boolean intervalEquals(byte[] a, int offset, byte[] a2) {
        if ((a.length - offset) >= a2.length) {
            for (int i = 0; i < a2.length; i++) {
                if (a[i + offset] != a2[i]) return false;
            }
            return true;
        } else return false;
    }

    /**Expand the given path to an absolute path and find the first truly existing
	 parent directory.
	 @return null if none of the parents exist in the given path*/
    public static File findFirstExistingParentDirectory(File fileOrDir) throws IOException {
        fileOrDir = fileOrDir.getCanonicalFile();
        do {
            if (fileOrDir.isDirectory()) return fileOrDir; else fileOrDir = fileOrDir.getParentFile();
        } while (fileOrDir != null);
        return null;
    }

    /**block until the given byte array can be filled from the stream
	 @return the number bytes actually read.  This will only be less than the length
	 of the byte array if the end of the stream is reached.
	 */
    public static int readCompletely(InputStream is, byte[] bytes) throws IOException {
        int totalRead = 0;
        do {
            int nRead = is.read(bytes, totalRead, bytes.length - totalRead);
            if (nRead == -1) break; else totalRead += nRead;
        } while (totalRead < bytes.length);
        return totalRead;
    }

    public static String printBytes(byte[] bytes) {
        return printBytes(bytes, 0, bytes.length);
    }

    public static String printBytes(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder(length * 4 + 32);
        for (int i = 0; i < length; i++) {
            String s = Integer.toHexString(ubyte(bytes[i + offset]));
            if (s.length() == 1) sb.append('0');
            sb.append(s.toUpperCase());
            sb.append(' ');
        }
        return sb.toString();
    }

    public static byte[] concat(byte[] a, byte[] a2, byte[] a3) {
        byte[] all = new byte[a.length + a2.length + a3.length];
        System.arraycopy(a, 0, all, 0, a.length);
        System.arraycopy(a2, 0, all, a.length, a2.length);
        System.arraycopy(a3, 0, all, a.length + a2.length, a3.length);
        return all;
    }

    public static int asIntBE(byte[] b, int offset, int length) {
        int t = 0;
        for (int i = 0; i < length; i++) t = (t << 8) + (b[i + offset] & 0xFF);
        return t;
    }

    public static int asIntLE(byte[] b, int offset, int length) {
        assert length > 0 && length < 5;
        int t = 0;
        for (int i = length; i-- > 0; ) t = (t << 8) + (b[i + offset] & 0xFF);
        return t;
    }

    public static byte[] intLE(int value) {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
        return bytes;
    }

    public static byte[] longLE(long value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(value);
        return bytes;
    }

    public static String asUTF8(byte[] bytes, int offset, int length) {
        try {
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            return decoder.decode(ByteBuffer.wrap(bytes, offset, length)).toString();
        } catch (MalformedInputException ex) {
            ex.printStackTrace();
        } catch (UnmappableCharacterException uc) {
            uc.printStackTrace();
        } catch (CharacterCodingException cc) {
            cc.printStackTrace();
        }
        return null;
    }

    public static long asLongLE(byte[] b, int offset, int length) {
        assert length > 0 && length < 9;
        long t = 0;
        for (int i = length; i-- > 0; ) t = (t << 8) + (b[i + offset] & 0xFF);
        return t;
    }

    public static int lowNibble(byte b) {
        return b & 0xF;
    }

    public static int highNibble(byte b) {
        return Util.ubyte(b) >>> 4;
    }

    /**
	 Find the specified byte pattern in the input stream.
	 If the pattern is found the input stream will be positioned
	 to read the byte directly after the pattern.  If the stream does not
	 contain the pattern it will be consumed completely.
	 
	 
	 @param is the stream to search.  Mark and reset does NOT need to be supported
	 @param pattern the byte pattern to search for.  Must be at least 1 byte.
	 @return the offset from the initial stream position to pattern (-1 if the pattern was not found).  For example if you call streamFind when the pattern is the very next part of the stream, 0 will be returned (found immediately).  If the pattern is 5 bytes a way when you call streamFind, 5 will be returned.
	 @todo: 1) test this and 2) it would be handy if this function could optionally mark the stream
	 at the start of the pattern.  However this might be inefficient.
	 */
    public static int streamFind(InputStream is, byte[] pattern) throws IOException {
        byte[] windowBuffer = new byte[pattern.length];
        if (readCompletely(is, windowBuffer) == windowBuffer.length) {
            int distance = 0;
            if (pattern.length > 0) {
                CircularByteQueue window = new CircularByteQueue(windowBuffer, true);
                int b;
                while (!window.equals(pattern)) {
                    b = is.read();
                    distance++;
                    if (b != -1) window.put((byte) b); else return -1;
                }
            }
            return distance;
        } else return -1;
    }

    public static String readTextStream(InputStream is, Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 4);
        redirectInput(is, baos, new byte[1024 * 2], -1);
        byte[] bytes = baos.toByteArray();
        baos = null;
        return new String(bytes, charset);
    }

    /**
	 @param flushEveryNBytes flush after writing at least this many bytes to the output stream.  pass -1 to never flush.  pass 1 to flush with every non empty read.
	 */
    public static int redirectInput(InputStream from, OutputStream to, byte[] readBuffer, int flushEveryNBytes) throws IOException {
        int nRead = 0;
        int totalRead = 0;
        int unflushed = 0;
        while (nRead != -1) {
            if (nRead > 0) {
                to.write(readBuffer, 0, nRead);
                totalRead += nRead;
                unflushed += nRead;
                if (unflushed >= flushEveryNBytes) {
                    unflushed = 0;
                    to.flush();
                }
            }
            nRead = from.read(readBuffer);
        }
        return totalRead;
    }

    public static void serializeObjectToFile(Object obj, File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 128);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();
        } finally {
            fos.close();
        }
    }

    public static Object deserializeObjectFromFile(File f) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(f);
        try {
            BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 128);
            ObjectInputStream ois = new ObjectInputStream(bis);
            return ois.readObject();
        } finally {
            fis.close();
        }
    }

    public static String getStackTraceText(Throwable t) {
        StringWriter sw = new StringWriter(1024);
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void copyFile(File source, File destination) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destination);
            FileChannel sourceChannel = fis.getChannel();
            FileChannel destinationChannel = fos.getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            destinationChannel.close();
            sourceChannel.close();
        } finally {
            if (fos != null) fos.close();
            fis.close();
        }
    }

    /**
	 Return true if the number is even
	 */
    public static boolean isEven(int number) {
        return (number & 1) == 0;
    }

    public static class Tester {

        File root = new File("deleteme (fileSetTest)");

        private void makeFileSetTestFiles() {
            try {
                if (!root.exists()) {
                    new File(root, "d1.html/d4").mkdirs();
                    new File(root, "d2.txt/d3").mkdirs();
                    new File(root, "f1.txt").createNewFile();
                    new File(root, "f2.html").createNewFile();
                    new File(root, "d1.html/f3.txt").createNewFile();
                    new File(root, "d1.html/f4.html").createNewFile();
                    new File(root, "d2.txt/f5.txt").createNewFile();
                    new File(root, "d2.txt/f6.html").createNewFile();
                    new File(root, "d2.txt/d3/f7.txt").createNewFile();
                    new File(root, "d2.txt/d3/f8.html").createNewFile();
                    new File(root, "d2.txt/d3/f9.txt").createNewFile();
                }
            } catch (IOException ioe) {
                fail(ioe.toString());
            }
        }

        @Test
        public void streamFindTest() throws IOException {
            {
                ByteArrayInputStream is;
                {
                    is = new ByteArrayInputStream(new byte[0]);
                    assertTrue(Util.streamFind(is, new byte[0]) == 0);
                }
                {
                    is = new ByteArrayInputStream(new byte[0]);
                    assertTrue(Util.streamFind(is, new byte[1]) == -1);
                }
                {
                    is = new ByteArrayInputStream(new byte[1]);
                    assertTrue(Util.streamFind(is, new byte[0]) == 0);
                }
                {
                    is = new ByteArrayInputStream(new byte[] { Byte.MIN_VALUE });
                    assertTrue(Util.streamFind(is, new byte[] { Byte.MIN_VALUE }) == 0);
                    assertTrue(is.read() == -1);
                }
                {
                    is = new ByteArrayInputStream(new byte[] { 126, Byte.MAX_VALUE });
                    assertTrue(Util.streamFind(is, new byte[] { Byte.MAX_VALUE }) == 1);
                    assertTrue(is.read() == -1);
                }
                {
                    is = new ByteArrayInputStream(new byte[] { 126, 127 });
                    assertTrue(Util.streamFind(is, new byte[] { 126 }) == 0);
                    assertTrue(is.read() == 127);
                }
                {
                    is = new ByteArrayInputStream(new byte[] { 1, 2 });
                    assertTrue(Util.streamFind(is, new byte[] { 1, 2, 3 }) == -1);
                    assertTrue(is.read() == -1);
                }
                {
                    is = new ByteArrayInputStream("HelloJelloFellow".getBytes());
                    assertTrue(Util.streamFind(is, "Hello".getBytes()) == 0);
                    assertTrue(is.read() == 'J');
                    assertTrue(Util.streamFind(is, "Hello".getBytes()) == -1);
                    assertTrue(is.read() == -1);
                }
                {
                    is = new ByteArrayInputStream("HelloJelloFellow".getBytes());
                    assertTrue(Util.streamFind(is, "ello".getBytes()) == 1);
                    assertTrue(is.read() == 'J');
                    assertTrue(Util.streamFind(is, "ello".getBytes()) == 0);
                    assertTrue(is.read() == 'F');
                    assertTrue(Util.streamFind(is, "ello".getBytes()) == 0);
                    assertTrue(is.read() == 'w');
                    assertTrue(Util.streamFind(is, "ello".getBytes()) == -1);
                    assertTrue(is.read() == -1);
                }
                {
                    is = new ByteArrayInputStream("HelloJelloFellow".getBytes());
                    assertTrue(Util.streamFind(is, "Fell".getBytes()) == 10);
                    assertTrue(Util.streamFind(is, "w".getBytes()) == 1);
                    assertTrue(is.read() == -1);
                }
            }
        }

        @Test(groups = "cur")
        public void indexOfTest() {
            assertTrue(arrayIndexOf(new byte[0], new byte[0], 0) == 0);
            assertTrue(arrayIndexOf(new byte[] { 1 }, new byte[0], 0) == 0);
            assertTrue(arrayIndexOf(new byte[] { 1 }, new byte[] { 1 }, 0) == 0);
            assertTrue(arrayIndexOf(new byte[] {}, new byte[] { 1 }, 0) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2 }, new byte[] { 1, 2 }, 0) == 0);
            assertTrue(arrayIndexOf(new byte[] { 1, 2 }, new byte[] { 2, 1 }, 0) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2 }, new byte[] { 2 }, 1) == 1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2 }, new byte[] { 2 }, 2) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2 }, new byte[] { 2 }, 99) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3, 4 }, 0) == 0);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3, 4 }, 1) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3, 4, 5 }, 0) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 4 }, 0) == 3);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 4 }, 1) == 3);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 4 }, 2) == 3);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 4 }, 3) == 3);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 4 }, 4) == -1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 2, 3 }, 0) == 1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 2, 3 }, 1) == 1);
            assertTrue(arrayIndexOf(new byte[] { 1, 2, 3, 4 }, new byte[] { 2, 3 }, 2) == -1);
        }
    }
}
