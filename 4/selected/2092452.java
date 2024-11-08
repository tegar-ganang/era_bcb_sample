package codejam;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

public abstract class CodeJamProblem {

    static final String DEFAULT_ENCODING = System.getProperty("file.encoding");

    public static Iterable<Integer> from(final int start, final int end, final int inc) {
        ArrayList<Integer> chars = new ArrayList<Integer>();
        for (int i = start; i <= end; i += inc) chars.add(new Integer(i));
        return chars;
    }

    public static Iterable<Integer> from(final int start, final int end) {
        return from(start, end, 1);
    }

    public static Iterable<Integer> fromlt(final int start, final int end) {
        return from(start, end - 1, 1);
    }

    public static Iterable<Integer> fromlt(final int start, final int end, int inc) {
        return from(start, end - 1, inc);
    }

    public static Iterable<Character> chars(String d) {
        ArrayList<Character> chars = new ArrayList<Character>();
        int l = d.length();
        for (int i = 0; i < l; i++) chars.add(new Character(d.charAt(i)));
        return chars;
    }

    ArrayList<String[]> _tests = new ArrayList<String[]>();

    public static enum TestSize {

        SMALL, LARGE
    }

    public abstract void solve(TestSize testSize, CodeJamInput in, CodeJamOutput out) throws Exception;

    public void addTest(String[] in, String[] out) {
        String[] test = new String[] { "", "" };
        for (String s : in) test[0] += s;
        for (String s : out) test[1] += s;
        _tests.add(test);
    }

    public void run() {
        System.out.println("Running problem " + getClass().getName());
        try {
            for (TestSize testSize : TestSize.values()) {
                runTests(testSize);
                produceOutput(testSize);
                produceUpload(testSize);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void produceUpload(TestSize testSize) {
    }

    private void produceOutput(TestSize testSize) throws Exception {
        Class<?> klass = getClass();
        ClassLoader classLoader = klass.getClassLoader();
        String inName = klass.getName().replaceAll("\\.", "/") + "_" + testSize.toString() + ".in";
        InputStream in = classLoader.getResourceAsStream(inName);
        if (in == null) {
            inName = inName.replaceAll("_", "-");
            in = classLoader.getResourceAsStream(inName);
        }
        if (in == null) {
            System.out.println("No output produced for " + testSize + " test size, file " + inName + " not found.");
            return;
        }
        try {
            CodeJamOutput jamOutput = new CodeJamOutput();
            solve(testSize, new CodeJamInput(in), jamOutput);
            URL outputURL = classLoader.getResource(inName);
            String outName = URLDecoder.decode(outputURL.getFile(), DEFAULT_ENCODING);
            outName = outName.substring(0, outName.length() - 2) + "out";
            File outFile = new File(outName);
            FileOutputStream outputStream = new FileOutputStream(outFile);
            outputStream.write(jamOutput.toByteArray());
            outputStream.flush();
            outputStream.close();
            System.out.println("Solution file produced: " + outFile);
        } finally {
            in.close();
        }
    }

    private void runTests(TestSize testSize) throws Exception {
        Class<?> klass = getClass();
        ClassLoader classLoader = klass.getClassLoader();
        for (int i = 1; ; i++) {
            String inName = klass.getName().replaceAll("\\.", "/") + "-test-" + i + ".in";
            InputStream in = classLoader.getResourceAsStream(inName);
            if (in == null) {
                if (i == 1) {
                    System.err.println("WARNING: NO TESTS FOUND");
                    return;
                }
                break;
            }
            String outName = klass.getName().replaceAll("\\.", "/") + "-test-" + i + ".out";
            InputStream out = classLoader.getResourceAsStream(outName);
            if (out == null) throw new CodeJamException("Missing .out file for " + inName);
            try {
                System.out.println("Running test " + inName);
                CodeJamOutput jamOutput = new CodeJamOutput();
                solve(testSize, new CodeJamInput(in), jamOutput);
                if (!checkTestSuccess(inName, readFully(out), jamOutput.toByteArray())) return;
            } finally {
                in.close();
                out.close();
            }
        }
        System.out.println("ALL TESTS SUCCEEDED");
    }

    private byte[] readFully(InputStream in) throws IOException {
        in = new BufferedInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) out.write(c);
        return out.toByteArray();
    }

    private boolean checkTestSuccess(String testName, byte[] testOutput, byte[] solvedOutput) {
        try {
            boolean success = true;
            DataInput inTest = new DataInputStream(new ByteArrayInputStream(testOutput));
            DataInput outTest = new DataInputStream(new ByteArrayInputStream(solvedOutput));
            String lastIn = "";
            String lastOut = "";
            int line = 1;
            for (; success; line++) {
                if ((lastIn = inTest.readLine()) == null) {
                    if ((lastOut = outTest.readLine()) != null) success = false;
                    break;
                }
                if ((lastOut = outTest.readLine()) == null) {
                    success = false;
                    break;
                }
                if (!lastIn.equals(lastOut)) {
                    success = false;
                    break;
                }
            }
            if (!success) {
                System.err.println();
                System.err.println("Test " + testName + " failed at line " + line);
                System.err.println(" Expected>" + lastIn);
                System.err.println("Generated>" + lastOut);
                System.err.println();
                System.err.println("Expected Test Results:");
                inTest = new DataInputStream(new ByteArrayInputStream(testOutput));
                outTest = new DataInputStream(new ByteArrayInputStream(solvedOutput));
                String s;
                int i = 1;
                while ((s = inTest.readLine()) != null) {
                    String num = "0000000" + i++;
                    num = num.substring(num.length() - 3);
                    System.err.println(num + ">" + s);
                }
                System.err.println();
                System.err.println("Generated Test Results:");
                i = 1;
                while ((s = outTest.readLine()) != null) {
                    String num = "0000000" + i++;
                    num = num.substring(num.length() - 3);
                    System.err.println(num + ">" + s);
                }
            }
            return success;
        } catch (IOException x) {
            throw new RuntimeException("Unexpected Error", x);
        }
    }
}
