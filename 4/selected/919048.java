package org.p2s.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class Connect {

    public static void connect(InputStream in, OutputStream out, int bufsize) throws IOException {
        byte buf[] = new byte[bufsize];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.flush();
        in.close();
    }

    public static void connect(InputStream in, OutputStream out, int bufsize, int maxkbps) throws Exception {
        byte buf[] = new byte[bufsize];
        int read;
        Date date1 = new Date();
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
            Date date2 = new Date();
            int minmilsecperloop = read / maxkbps;
            int milseconds = (int) (date2.getTime() - date1.getTime());
            if (milseconds < minmilsecperloop) {
                Thread.sleep(minmilsecperloop - milseconds);
            }
            date1 = date2;
        }
        out.flush();
        in.close();
    }
}
