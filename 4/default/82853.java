import java.io.*;

public class PipedReaderWriterTest {

    public static void main(String[] argv) throws InterruptedException {
        String prop = System.getProperty("gnu.java.io.pipe_size");
        try {
            System.out.println("Started test of PipedReader and PipedWriter");
            System.out.println("Test 1: Basic pipe test");
            PipedTestWriter ptw = new PipedTestWriter();
            String str = ptw.getStr();
            PipedWriter pw = ptw.getWriter();
            PipedReader pr = new PipedReader();
            pr.connect(pw);
            new Thread(ptw).start();
            char[] buf = new char[12];
            int chars_read, total_read = 0;
            while ((chars_read = pr.read(buf)) != -1) {
                System.out.print(new String(buf, 0, chars_read));
                System.out.flush();
                Thread.sleep(10);
                total_read += chars_read;
            }
            if (total_read == str.length()) System.out.println("PASSED: Basic pipe test"); else System.out.println("FAILED: Basic pipe test");
        } catch (IOException e) {
            System.out.println("FAILED: Basic pipe test: " + e);
        }
    }
}

class PipedTestWriter implements Runnable {

    String str;

    StringReader sbr;

    PipedWriter out;

    public PipedTestWriter() {
        str = "In college, there was a tradition going for a while that people\n" + "would get together and hang out at Showalter Fountain - in the center\n" + "of Indiana University's campus - around midnight.  It was mostly folks\n" + "from the computer lab and just people who liked to use the Forum\n" + "bbs system on the VAX.  IU pulled the plug on the Forum after I left\n" + "despite its huge popularity.  Now they claim they are just giving\n" + "students what they want by cutting deals to make the campus all\n" + "Microsoft.\n";
        sbr = new StringReader(str);
        out = new PipedWriter();
    }

    public PipedWriter getWriter() {
        return (out);
    }

    public String getStr() {
        return (str);
    }

    public void run() {
        char[] buf = new char[32];
        int chars_read;
        try {
            int b = sbr.read();
            out.write(b);
            while ((chars_read = sbr.read(buf)) != -1) out.write(buf, 0, chars_read);
            out.close();
        } catch (IOException e) {
            System.out.println("FAILED: Basic pipe test: " + e);
        }
    }
}
