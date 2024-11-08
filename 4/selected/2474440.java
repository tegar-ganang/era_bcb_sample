package fr.jussieu.gla.wasa.monitor.xml.junit;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import com.lc.util.CascadingRuntimeException;
import fr.jussieu.gla.wasa.monitor.xml.BreakingInputStream;

/**
 * Test of {@link BreakingInputStream}.
 * @author Laurent Caillette
 * @version $Revision: 1.3 $ $Date: 2002/03/21 00:59:18 $
 */
public class BreakingInputStreamTest extends TestCase {

    private static final int READ_BUFFER_SIZE = 5;

    private ByteArrayOutputStream bufferedRead(InputStream is) {
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead > 0) {
                    baos.write(buffer, 0, bytesRead);
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            throw new CascadingRuntimeException(ex);
        }
        return baos;
    }

    private ByteArrayOutputStream bigBufferedRead(InputStream is) {
        byte[] buffer = new byte[READ_BUFFER_SIZE * 3];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (true) {
                int bytesRead = is.read(buffer, READ_BUFFER_SIZE, READ_BUFFER_SIZE);
                if (bytesRead > 0) {
                    baos.write(buffer, READ_BUFFER_SIZE, bytesRead);
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            throw new CascadingRuntimeException(ex);
        }
        return baos;
    }

    private ByteArrayOutputStream bufferedClosingRead(InputStream is) {
        byte[] buffer = new byte[READ_BUFFER_SIZE * 3];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (true) {
                int bytesRead = is.read(buffer, READ_BUFFER_SIZE, READ_BUFFER_SIZE);
                if (bytesRead > 0) {
                    baos.write(buffer, READ_BUFFER_SIZE, bytesRead);
                } else {
                    break;
                }
            }
            is.close();
        } catch (IOException ex) {
            throw new CascadingRuntimeException(ex);
        }
        return baos;
    }

    public void test3BreaksBufferedClosingReadSparse() {
        String breakA = "stop";
        String breakB = "end";
        String breakC = "finished";
        String content1 = "abc def" + breakA;
        String content2 = "ghi  jkl" + breakB;
        String content3 = "mnop q  rst" + breakC;
        String content4 = "uv" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + "   " + content2 + content3 + "  " + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        String s;
        s = bufferedClosingRead(bis).toString();
        assertEquals(content1, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content2, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content3, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content4, s);
    }

    private ByteArrayOutputStream simpleRead(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (true) {
                int theByte = is.read();
                if (theByte != -1) {
                    baos.write(theByte);
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            throw new CascadingRuntimeException(ex);
        }
        return baos;
    }

    private ByteArrayOutputStream availableRead(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            while (is.available() > 0) {
                baos.write(is.read());
            }
        } catch (IOException ex) {
            throw new CascadingRuntimeException(ex);
        }
        return baos;
    }

    public void test3BreaksBufferedClosingRead() {
        String breakA = "end";
        String breakB = "stop";
        String breakC = "finish";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        String s;
        s = bufferedClosingRead(bis).toString();
        assertEquals(content1, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content2, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content3, s);
        bis.continueReading();
        s = bufferedClosingRead(bis).toString();
        assertEquals(content4, s);
    }

    public void test1BreakSimpleRead() {
        String content1 = "<start>something</start>";
        String content2 = "<start>somethingelse</start>";
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2).getBytes()), new byte[][] { "</start>".getBytes() });
        assertEquals(content1, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, simpleRead(bis).toString());
    }

    public void test3BreaksSimpleRead() {
        String breakA = "end";
        String breakB = "stop";
        String breakC = "finish";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        assertEquals(content1, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content3, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content4, simpleRead(bis).toString());
    }

    public void testCollidingBreaksSimpleRead() {
        String breakA = "end1";
        String breakB = "end2";
        String breakC = "endend";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        assertEquals(content1, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content3, simpleRead(bis).toString());
        bis.continueReading();
        assertEquals(content4, simpleRead(bis).toString());
    }

    public void test3BreaksBufferedRead() {
        String breakA = "end";
        String breakB = "stop";
        String breakC = "finish";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        assertEquals(content1, bufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, bufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content3, bufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content4, bufferedRead(bis).toString());
    }

    public void test3BreaksBigBufferedRead() {
        String breakA = "end";
        String breakB = "stop";
        String breakC = "finish";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        assertEquals(content1, bigBufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, bigBufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content3, bigBufferedRead(bis).toString());
        bis.continueReading();
        assertEquals(content4, bigBufferedRead(bis).toString());
    }

    public void test2BreaksAvailableRead() {
        String breakA = "end";
        String breakB = "stop";
        String breakC = "finish";
        String content1 = "azer" + breakA;
        String content2 = "xyz" + breakB;
        String content3 = "012345" + breakC;
        String content4 = "qwerty" + breakA;
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream((content1 + content2 + content3 + content4).getBytes()), new byte[][] { breakA.getBytes(), breakB.getBytes(), breakC.getBytes() });
        assertEquals(content1, availableRead(bis).toString());
        bis.continueReading();
        assertEquals(content2, availableRead(bis).toString());
        bis.continueReading();
        assertEquals(content3, availableRead(bis).toString());
        bis.continueReading();
        assertEquals(content4, availableRead(bis).toString());
    }

    public void test0BreakSimpleRead() {
        String content = "<start>something</start>";
        BreakingInputStream bis = new BreakingInputStream(new ByteArrayInputStream(content.getBytes()), new byte[][] { "</start>".getBytes() });
        assertEquals(content, simpleRead(bis).toString());
    }

    public BreakingInputStreamTest(String s) {
        super(s);
    }

    public static Test suite() {
        TestSuite theSuite = new TestSuite(BreakingInputStreamTest.class);
        return theSuite;
    }

    public static void main(String[] args) {
        TestRunner.run(BreakingInputStreamTest.class);
    }
}
