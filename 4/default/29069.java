import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;

class LongValueComparator implements Comparator<Map.Entry<Method, Long>> {

    public int compare(final Map.Entry<Method, Long> o1, final Map.Entry<Method, Long> o2) {
        final long d = o1.getValue() - o2.getValue();
        return d < 0 ? -1 : d > 0 ? +2 : o1.getKey().getName().compareTo(o2.getKey().getName());
    }

    private LongValueComparator() {
    }

    private static LongValueComparator instance = new LongValueComparator();

    public static Comparator<Map.Entry<Method, Long>> getInstance() {
        return instance;
    }
}

public class PerfTestMapDecode {

    public static final int NUM_OF_TEST_REPETITIONS = 200;

    private static String testFileName;

    public static int sum;

    private static Pattern lineSplit = Pattern.compile("\n");

    private static Pattern wordSplit = Pattern.compile("\\s+");

    public static void main(final String... args) throws Throwable {
        testFileName = args[0];
        final List<Method> performanceTestMethods = new ArrayList<Method>();
        for (final Method method : PerfTestMapDecode.class.getMethods()) {
            if (method.getAnnotation(PerfTest.class) != null) {
                performanceTestMethods.add(method);
            }
        }
        Collections.shuffle(performanceTestMethods);
        final Map<Method, Long> results = new HashMap<Method, Long>();
        final List<Integer> sums = new ArrayList<Integer>();
        for (final Method performanceTestMethod : performanceTestMethods) {
            sum = 0;
            performanceTestMethod.invoke(null);
            final long start = System.currentTimeMillis();
            for (int i = 0; i < NUM_OF_TEST_REPETITIONS; i++) {
                performanceTestMethod.invoke(null);
            }
            sums.add(sum);
            final long end = System.currentTimeMillis();
            final long time = end - start;
            results.put(performanceTestMethod, time);
        }
        final SortedSet<Map.Entry<Method, Long>> sortedResults = new TreeSet<Map.Entry<Method, Long>>(LongValueComparator.getInstance());
        sortedResults.addAll(results.entrySet());
        for (final Map.Entry<Method, Long> result : sortedResults) {
            final Method performanceTestMethod = result.getKey();
            final long time = result.getValue();
            System.out.printf("%24s %05dms %s%n", performanceTestMethod.getName(), time, performanceTestMethod.getAnnotation(PerfTest.class).value());
        }
    }

    @PerfTest("BufferedReader, regionMatches (old)")
    public static void old() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                if (line.regionMatches(0, "arch ", 0, 5)) {
                    state += 1;
                } else if (line.regionMatches(0, "endmsg", 0, 6)) {
                    state += 2;
                } else if (line.regionMatches(0, "end", 0, 3)) {
                    state += 3;
                } else if (line.regionMatches(0, "msg", 0, 3)) {
                    state += 4;
                } else if (line.regionMatches(0, "x ", 0, 2)) {
                    state += 5;
                } else if (line.regionMatches(0, "y ", 0, 2)) {
                    state += 6;
                } else if (line.regionMatches(0, "type ", 0, 5)) {
                    state += 7;
                } else if (line.regionMatches(0, "direction ", 0, 10)) {
                    state += 8;
                } else if (line.regionMatches(0, "face ", 0, 5)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, startsWith (current)")
    public static void current() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                if (line.startsWith("arch ")) {
                    state += 1;
                } else if (line.startsWith("endmsg")) {
                    state += 2;
                } else if (line.startsWith("end")) {
                    state += 3;
                } else if (line.startsWith("msg")) {
                    state += 4;
                } else if (line.startsWith("x ")) {
                    state += 5;
                } else if (line.startsWith("y ")) {
                    state += 6;
                } else if (line.startsWith("type ")) {
                    state += 7;
                } else if (line.startsWith("direction ")) {
                    state += 8;
                } else if (line.startsWith("face ")) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, word by regex split of java.lang.String")
    public static void splitEqualsString() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final String word = line.split("\\s+")[0];
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, word by regex split of java.util.regex.Pattern")
    public static void splitEqualsPattern() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final String word = wordSplit.split(line)[0];
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, manual word split using String.indexOf(' ')")
    public static void manualEquals() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, manual word split using String.indexOf(' ') using hashCode")
    public static void manualEqualsHash() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                final int hash = word.hashCode();
                if ("arch".hashCode() == hash) {
                    state += 1;
                } else if ("endmsg".hashCode() == hash) {
                    state += 2;
                } else if ("end".hashCode() == hash) {
                    state += 3;
                } else if ("msg".hashCode() == hash) {
                    state += 4;
                } else if ("x".hashCode() == hash) {
                    state += 5;
                } else if ("y".hashCode() == hash) {
                    state += 6;
                } else if ("type".hashCode() == hash) {
                    state += 7;
                } else if ("direction".hashCode() == hash) {
                    state += 8;
                } else if ("face".hashCode() == hash) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    private static final int hashArch = "arch".hashCode();

    private static final int hashEndmsg = "endmsg".hashCode();

    private static final int hashEnd = "end".hashCode();

    private static final int hashMsg = "msg".hashCode();

    private static final int hashX = "x".hashCode();

    private static final int hashY = "y".hashCode();

    private static final int hashType = "type".hashCode();

    private static final int hashDirection = "direction".hashCode();

    private static final int hashFace = "face".hashCode();

    private static final int[] hashes = { hashArch, hashEndmsg, hashEnd, hashMsg, hashX, hashY, hashType, hashDirection, hashFace };

    @PerfTest("BufferedReader, manual word split using String.indexOf(' ') using a cached hashCode")
    public static void manualEqualsCashedHash() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                final int hash = word.hashCode();
                if (hash == hashArch) {
                    state += 1;
                } else if (hash == hashEndmsg) {
                    state += 2;
                } else if (hash == hashEnd) {
                    state += 3;
                } else if (hash == hashMsg) {
                    state += 4;
                } else if (hash == hashX) {
                    state += 5;
                } else if (hash == hashY) {
                    state += 6;
                } else if (hash == hashType) {
                    state += 7;
                } else if (hash == hashDirection) {
                    state += 8;
                } else if (hash == hashFace) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedReader, manual word split using String.indexOf(' ') using a cached hashCode via switch")
    public static void manualEqualsCashedHashSwitch() throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"));
        int state = 0;
        try {
            for (String line; (line = in.readLine()) != null; ) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                final int hash = word.hashCode();
                int hashIndex;
                for (hashIndex = 0; hashIndex < hashes.length; hashIndex++) {
                    if (hashes[hashIndex] == hash) {
                        break;
                    }
                }
                switch(hashIndex) {
                    case 0:
                        state += 1;
                        break;
                    case 1:
                        state += 2;
                        break;
                    case 2:
                        state += 3;
                        break;
                    case 3:
                        state += 4;
                        break;
                    case 4:
                        state += 5;
                        break;
                    case 5:
                        state += 6;
                        break;
                    case 6:
                        state += 7;
                        break;
                    case 7:
                        state += 8;
                        break;
                    case 8:
                        state += 9;
                        break;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("New File I/O nondirect -> String,  manual split of java.lang.String")
    public static void nioManual() throws IOException {
        int state = 0;
        final FileChannel fileChannel = new FileInputStream(testFileName).getChannel();
        final ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
        fileChannel.read(buffer);
        try {
            final String file = new String(buffer.array(), 0);
            for (String line : lineSplit.split(file)) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            fileChannel.close();
        }
        sum += state;
    }

    private static final CharsetDecoder decoder = Charset.forName("iso-8859-1").newDecoder();

    @PerfTest("Mapped File I/O nondirect -> charset, pattern split")
    public static void nioChar() throws IOException {
        int state = 0;
        final FileChannel fileChannel = new FileInputStream(testFileName).getChannel();
        final ByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        final CharBuffer charBuffer = decoder.decode(buffer);
        try {
            for (String line : lineSplit.split(charBuffer)) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            fileChannel.close();
        }
        sum += state;
    }

    @PerfTest("BufferdReader -> StringBuilder,  manual split of java.lang.String")
    public static void bufReadManBuf() throws IOException {
        int state = 0;
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(testFileName), "iso-8859-1"), 8192);
        try {
            final StringBuilder file = new StringBuilder();
            final char[] buf = new char[8192];
            for (int charsRead; (charsRead = in.read(buf)) != -1; ) {
                file.append(buf, 0, charsRead);
            }
            for (String line : lineSplit.split(file)) {
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("BufferedInputStream -> String, manual split")
    public static void bufReadAll() throws IOException {
        int state = 0;
        final int size = (int) new File(testFileName).length();
        final byte[] buf = new byte[size];
        final BufferedInputStream in = new BufferedInputStream(new FileInputStream(testFileName));
        try {
            for (int pos = 0, bytesRead; pos < size && (bytesRead = in.read(buf, pos, size - pos)) != -1; pos += bytesRead) ;
            final String file = new String(buf);
            final StringTokenizer tk = new StringTokenizer(file, "\n");
            while (tk.hasMoreTokens()) {
                String line = tk.nextToken();
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            in.close();
        }
        sum += state;
    }

    @PerfTest("Mapped File I/O nondirect -> String, StringTokenizer")
    public static void nioByte() throws IOException {
        int state = 0;
        final FileChannel fileChannel = new FileInputStream(testFileName).getChannel();
        final ByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        try {
            final byte[] buf = new byte[buffer.remaining()];
            buffer.get(buf);
            final String file = new String(buf);
            final StringTokenizer tk = new StringTokenizer(file, "\n");
            while (tk.hasMoreTokens()) {
                String line = tk.nextToken();
                line = line.trim();
                final int indexOfSpace = line.indexOf(' ');
                final String word = indexOfSpace > 0 ? line.substring(0, indexOfSpace) : line;
                if ("arch".equals(word)) {
                    state += 1;
                } else if ("endmsg".equals(word)) {
                    state += 2;
                } else if ("end".equals(word)) {
                    state += 3;
                } else if ("msg".equals(word)) {
                    state += 4;
                } else if ("x".equals(word)) {
                    state += 5;
                } else if ("y".equals(word)) {
                    state += 6;
                } else if ("type".equals(word)) {
                    state += 7;
                } else if ("direction".equals(word)) {
                    state += 8;
                } else if ("face".equals(word)) {
                    state += 9;
                }
            }
        } finally {
            fileChannel.close();
        }
        sum += state;
    }
}
