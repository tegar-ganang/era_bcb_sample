import java.io.*;

public class PipedStreamTest {

    public static void main(String[] argv) throws InterruptedException {
        String prop = System.getProperty("gnu.java.io.pipe_size");
        try {
            System.out.println("Started test of PipedInputStream and " + "PipedOutputStream");
            System.out.println("Test 1: Basic piped stream test");
            PipedStreamTestWriter pstw = new PipedStreamTestWriter();
            String str = pstw.getStr();
            PipedOutputStream pos = pstw.getStream();
            PipedInputStream pis = new PipedInputStream();
            pis.connect(pos);
            new Thread(pstw).start();
            byte[] buf = new byte[12];
            int bytes_read, total_read = 0;
            while ((bytes_read = pis.read(buf)) != -1) {
                System.out.print(new String(buf, 0, bytes_read));
                System.out.flush();
                Thread.sleep(10);
                total_read += bytes_read;
            }
            if (total_read == str.length()) System.out.println("PASSED: Basic piped stream test"); else System.out.println("FAILED: Basic piped stream test");
        } catch (IOException e) {
            System.out.println("FAILED: Basic piped stream test: " + e);
        }
    }
}

class PipedStreamTestWriter implements Runnable {

    String str;

    StringBufferInputStream sbis;

    PipedOutputStream out;

    public PipedStreamTestWriter() {
        str = "I went to work for Andersen Consulting after I graduated\n" + "from college.  They sent me to their training facility in St. Charles,\n" + "Illinois and tried to teach me COBOL.  I didn't want to learn it.\n" + "The instructors said I had a bad attitude and I got a green sheet\n" + "which is a nasty note in your file saying what a jerk you are.\n";
        sbis = new StringBufferInputStream(str);
        out = new PipedOutputStream();
    }

    public PipedOutputStream getStream() {
        return (out);
    }

    public String getStr() {
        return (str);
    }

    public void run() {
        byte[] buf = new byte[32];
        int bytes_read;
        try {
            int b = sbis.read();
            out.write(b);
            while ((bytes_read = sbis.read(buf)) != -1) out.write(buf, 0, bytes_read);
            out.close();
        } catch (IOException e) {
            System.out.println("FAILED: Basic piped stream test: " + e);
        }
    }
}
