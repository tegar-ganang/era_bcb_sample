import org.okip.service.filing.api.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.okip.service.filing.impl.rfs.*;

public class CabCat {

    public static void main(String args[]) throws FilingException {
        ArrayList words = CabUtil.parseCommandLine(args);
        Iterator i = words.iterator();
        while (i.hasNext()) {
            String name = (String) i.next();
            ByteStore bs = CabUtil.getByteStore(name);
            OkiInputStream in = bs.getOkiInputStream();
            printStream(in);
            in.close();
        }
    }

    public static void printStream(OkiInputStream in) throws FilingException {
        byte buf[] = new byte[128];
        int len;
        while ((len = in.read(buf)) != -1) System.out.write(buf, 0, len);
    }

    public static void dumpStream(OkiInputStream in) throws FilingException {
        in.skip(11);
        in.skip(-10);
        long avail = in.available();
        System.err.println("=available=" + avail);
        int b;
        int c = 0;
        while ((b = in.read()) != -1) {
            System.out.println("'" + (char) b + "'");
            in.skip(2);
            System.out.println("available=" + in.available());
            c++;
            if (c % 1000 == 0) System.err.println("processed=" + c);
        }
        System.err.println("handled " + c + " bytes");
    }

    public static void copyStream(OkiInputStream in, OkiOutputStream out) throws FilingException {
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.close();
    }
}
