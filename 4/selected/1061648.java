package org.jaxlib.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import jaxlib.buffer.CharBuffers;
import org.jaxlib.junit.ObjectTestCase;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: ReaderTestCase.java 2730 2009-04-21 01:12:29Z joerg_wassmer $
 */
public abstract class ReaderTestCase extends ObjectTestCase {

    static final Charset[] charsets = { Charset.forName("Big5"), Charset.forName("ISO-8859-1"), Charset.forName("ISO-8859-2"), Charset.forName("ISO-8859-3"), Charset.forName("ISO-8859-4"), Charset.forName("ISO-8859-5"), Charset.forName("ISO-8859-6"), Charset.forName("ISO-8859-7"), Charset.forName("ISO-8859-8"), Charset.forName("ISO-8859-9"), Charset.forName("ISO-8859-11"), Charset.forName("ISO-8859-13"), Charset.forName("ISO-8859-15"), Charset.forName("US-ASCII"), Charset.forName("UTF-8"), Charset.forName("UTF-16"), Charset.forName("x-Johab") };

    private static SoftReference<TestInput> sharedTestInput;

    public static TestInput createTestInput(Charset cs) throws IOException {
        TestInput in = (sharedTestInput == null) ? null : sharedTestInput.get();
        if (in == null) {
            in = new TestInput();
            sharedTestInput = new SoftReference<TestInput>(in);
        }
        in.setCharset(cs);
        return in;
    }

    protected ReaderTestCase(String name) {
        super(name);
    }

    protected abstract Reader createReader(TestInput in) throws IOException;

    public Charset[] getCharsetsToTest() {
        if (isTestingMultipleCharsets()) return charsets.clone(); else return new Charset[] { Charset.defaultCharset() };
    }

    void isClosedTestImpl(Reader in) throws IOException {
    }

    void isOpenTestImpl(Reader in) throws IOException {
    }

    public boolean isTestingMultipleCharsets() {
        return false;
    }

    protected final Reader createObject() {
        try {
            Charset cs = Charset.defaultCharset();
            return createReader(createTestInput(cs));
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void testReadArrayComplete() throws IOException {
        for (Charset cs : getCharsetsToTest()) {
            String csName = cs.name();
            TestInput testInput = createTestInput(cs);
            Reader expectIn = testInput.asReader();
            Reader actualIn = createReader(testInput);
            char[] a = new char[100];
            while (true) {
                Arrays.fill(a, (char) 666);
                int step = actualIn.read(a);
                if (step == -1) {
                    assertEquals(csName, -1, expectIn.read());
                    for (char c : a) assertEquals(666, c);
                    break;
                } else if (step <= 0) {
                    fail("read(char[]) returned illegal value: " + step);
                } else {
                    assertBetween(1, a.length, step);
                    for (int i = 0; i < step; i++) assertEquals(csName, expectIn.read(), a[i]);
                    for (int i = a.length; --i >= step; ) assertEquals(666, a[i]);
                }
            }
            assertEquals(-1, actualIn.read(a));
            expectIn.close();
            actualIn.close();
            isClosedTestImpl(actualIn);
        }
    }

    public void testReadArrayPartial() throws IOException {
        for (Charset cs : getCharsetsToTest()) {
            String csName = cs.name();
            TestInput testInput = createTestInput(cs);
            Reader expectIn = testInput.asReader();
            Reader actualIn = createReader(testInput);
            char[] a = new char[100];
            int x = 100;
            while (true) {
                Arrays.fill(a, (char) 666);
                int maxStep = ++x % 100;
                int step = actualIn.read(a, 0, maxStep);
                if (step == -1) {
                    assertEquals(csName, -1, expectIn.read());
                    for (int i = a.length; --i >= 0; ) assertEquals(666, a[i]);
                    break;
                } else {
                    assertBetween((maxStep == 0) ? 0 : 1, maxStep, step);
                    for (int i = 0; i < step; i++) assertEquals(csName, expectIn.read(), a[i]);
                    for (int i = a.length; --i >= step; ) assertEquals(666, a[i]);
                }
            }
            assertEquals(-1, actualIn.read(a));
            expectIn.close();
            actualIn.close();
            isClosedTestImpl(actualIn);
        }
    }

    public void testReadCharBuffer() throws IOException {
        for (Charset cs : getCharsetsToTest()) {
            String csName = cs.name();
            TestInput testInput = createTestInput(cs);
            Reader expectIn = testInput.asReader();
            Reader actualIn = createReader(testInput);
            CharBuffer a = CharBuffer.allocate(100);
            while (true) {
                a.clear();
                CharBuffers.fill(a, (char) 666);
                a.clear();
                int step = actualIn.read(a);
                if (step == -1) {
                    assertEquals(csName, 0, a.position());
                    assertEquals(csName, a.capacity(), a.limit());
                    assertEquals(csName, -1, expectIn.read());
                    for (int i = a.capacity(); --i >= 0; ) assertEquals(csName, 666, a.get(i));
                    break;
                } else if (step <= 0) {
                    fail("read(CharBuffer) returned illegal value: " + step);
                } else {
                    assertEquals(csName, step, a.position());
                    assertEquals(csName, a.capacity(), a.limit());
                    for (int i = 0; i < step; i++) assertEquals(csName, expectIn.read(), a.get(i));
                    for (int i = a.capacity(); --i >= step; ) assertEquals(csName, 666, a.get(i));
                }
            }
            assertEquals(csName, -1, actualIn.read(a));
            expectIn.close();
            actualIn.close();
            isClosedTestImpl(actualIn);
        }
    }

    public void testReadSingleChar() throws IOException {
        for (Charset cs : getCharsetsToTest()) {
            String csName = cs.name();
            TestInput testInput = createTestInput(cs);
            Reader expectIn = testInput.asReader();
            Reader actualIn = createReader(testInput);
            while (true) {
                int exp = expectIn.read();
                assertEquals(csName, exp, actualIn.read());
                if (exp < 0) break;
            }
            assertEquals(-1, actualIn.read());
            expectIn.close();
            actualIn.close();
            isClosedTestImpl(actualIn);
        }
    }

    public static final class TestInput extends Object {

        private static int writeSourceBytes(ByteArrayOutputStream bout, Charset cs) throws IOException {
            final int maxChar = cs.name().startsWith("UTF-") ? Character.MAX_VALUE - 2 : Character.MAX_VALUE;
            bout.reset();
            OutputStreamWriter out = new OutputStreamWriter(bout, cs.name());
            CharsetEncoder enc = cs.newEncoder();
            for (int i = 0; i <= maxChar; i++) {
                if (enc.canEncode((char) i)) out.write(i);
                if ((i % 79) == 0) out.write('\n');
            }
            out.close();
            return maxChar;
        }

        private ByteBuffer bytes;

        private CharBuffer chars;

        private Charset charset;

        private MyByteArrayOutputStream out;

        TestInput() {
            super();
        }

        public ByteBuffer getBytes() {
            return this.bytes.asReadOnlyBuffer();
        }

        public CharBuffer getChars() {
            if (this.chars != null) return this.chars;
            CharArrayWriter out = new CharArrayWriter(this.bytes.capacity());
            Reader in = asReader();
            try {
                for (int c; (c = in.read()) >= 0; ) out.write(c);
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
            this.chars = CharBuffer.wrap(out.toCharArray(), 0, out.size());
            return this.chars.asReadOnlyBuffer();
        }

        public ReadableByteChannel asByteChannel() {
            return Channels.newChannel(asInputStream());
        }

        public InputStream asInputStream() {
            return new ByteArrayInputStream(this.bytes.array(), 0, this.bytes.limit());
        }

        public Reader asReader() {
            return new BufferedReader(new InputStreamReader(asInputStream(), this.charset), 256);
        }

        public Charset getCharset() {
            return this.charset;
        }

        void setCharset(Charset cs) throws IOException {
            if (cs.equals(this.charset)) return;
            this.charset = cs;
            this.bytes = null;
            this.chars = null;
            MyByteArrayOutputStream out = this.out;
            if (out == null) this.out = out = new MyByteArrayOutputStream();
            out.reset();
            writeSourceBytes(out, cs);
            this.bytes = ByteBuffer.wrap(out.getArray(), 0, out.size());
        }
    }

    private static final class MyByteArrayOutputStream extends ByteArrayOutputStream {

        Charset actualCharset;

        MyByteArrayOutputStream() {
            super(Character.MAX_VALUE + 8192);
        }

        byte[] getArray() {
            return this.buf;
        }
    }
}
