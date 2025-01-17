package org.p2s.core;

import java.net.HttpURLConnection;
import java.net.URL;

public class Log {

    public static void log(String s) throws Exception {
        System.out.println("logging " + s);
        String url = "http://p2s.sourceforge.net/log.php?s=" + s + "&v=" + Config.getVersion();
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        conn.getInputStream().close();
        System.out.println("end logging");
    }
}
