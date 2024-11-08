package oxygen.test;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.Socket;
import oxygen.util.Closeable;
import oxygen.util.OxygenIntRange;
import oxygen.util.OxygenProxy;
import oxygen.util.OxygenUtils;
import oxygen.util.ProcessHandler;
import oxygen.util.StringUtils;

public class OxygenTest {

    public static void main(String[] args) throws Exception {
        testProcHandler();
    }

    private static interface MyIntf extends Closeable {

        boolean b();

        int i();

        long l();

        char c();

        String s();

        String[] sa();

        char[] ca();

        void n();
    }

    private static class MyClass implements MyIntf, Serializable {

        public boolean b() {
            return true;
        }

        public int i() {
            return 1;
        }

        public long l() {
            return Long.MAX_VALUE;
        }

        public char c() {
            return 'a';
        }

        public String s() {
            return "s";
        }

        public void n() {
            System.out.println("Calling n");
        }

        public void close() {
            System.out.println("Calling close");
        }

        public String[] sa() {
            return new String[] { "hi", "there" };
        }

        public char[] ca() {
            return new char[] { 'h', 'i' };
        }
    }

    private static void main0(OxygenProxy op) throws Exception {
        MyIntf opmi = (MyIntf) op.getProxy();
        System.out.println("opmi.i(): " + opmi.i());
        System.out.println("opmi.l(): " + opmi.l());
        System.out.println("opmi.b(): " + opmi.b());
        System.out.println("opmi.c(): " + opmi.c());
        System.out.println("opmi.s(): " + opmi.s());
        System.out.println("opmi.ca(): " + String.valueOf(opmi.ca()));
        System.out.println("opmi.sa(): " + opmi.sa());
        opmi.n();
        Closeable opmic = (Closeable) opmi;
        opmic.close();
    }

    private static void testProxy() throws Exception {
        main0(new OxygenProxy(null, new Class[] { MyIntf.class, Serializable.class }));
        System.out.println("-----------------------");
        main0(new OxygenProxy(new MyClass()));
    }

    private static void testURL() throws Exception {
        String h = "localhost";
        int p = 8080;
        String path = "/oxywiki/p/sectiou8ns/builtin";
        Socket s = new Socket(h, p);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        pw.print("GET " + path + " HTTP/1.0");
        pw.print("\r\n\r\n");
        pw.flush();
        InputStreamReader isr = new InputStreamReader(s.getInputStream());
        System.out.println(" - - - - - - - ");
        System.out.println(OxygenUtils.getTextContents(isr, true));
        System.out.println(" - - - - - - - ");
        s.close();
    }

    private static void testIntRange() throws Exception {
        int x = Integer.MAX_VALUE - 5;
        String[] s = new String[] { "1-5,7,9-15,20", "1-5," + x + "-" };
        for (int i = 0; i < s.length; i++) {
            System.out.println("-----------------------------");
            OxygenIntRange oir = new OxygenIntRange(s[i]);
            while (oir.hasNext()) {
                System.out.println(oir.next());
            }
        }
    }

    private static void testProcHandler() throws Exception {
        String s = "c:/software/rcs/rlog -zLT c:/tmp/rcstest/MainUgoD/PAGE.TXT";
        Process p = Runtime.getRuntime().exec(s);
        StringWriter err = new StringWriter();
        ProcessHandler ph = new ProcessHandler(p).drainStdOut((Writer) null).drainStdErr(err).passStdIn(null).waitTillDone().check(0, err.toString());
    }

    private static void testReplaceRBStyle() throws Exception {
        String s = "ugorji {0} hi {1} thanks {2} {2} whatever {1} {1";
        String[] r = new String[] { "jason0", "john1" };
        String s2 = StringUtils.replaceRBStyle(s, r);
        System.out.println(s2);
    }

    static void main1(String[] args) throws Exception {
        String methodArg = args[0];
        String[] args1 = new String[args.length - 1];
        for (int i = 0; i < args1.length; i++) {
            args1[i] = args[i + 1];
        }
        Method method = OxygenTest.class.getMethod(methodArg, new Class[] { String[].class });
        if (method == null) {
            method = Try.class.getMethod(methodArg, new Class[] { String[].class });
        }
        method.invoke(null, new Object[] { args1 });
    }
}
