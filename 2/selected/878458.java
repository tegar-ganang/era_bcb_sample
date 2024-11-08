package test.loopstart;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import junit.framework.Assert;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

/**
 * SDLoaderの開始と停止を繰り返します。
 * 
 * @author c9katayama
 */
public class SDLoaderLoopStartTest extends TestCase {

    public void testLoop() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            startstop(8080, false);
        }
        System.out.println("end testLoop");
    }

    public void testMultiThread() throws InterruptedException {
        List<Thread> tlist = new ArrayList<Thread>(10);
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    startstop(8080, true);
                }
            };
            t.start();
            tlist.add(t);
        }
        for (Thread t : tlist) {
            t.join();
        }
        System.out.println("end testMultiThread");
    }

    protected void startstop(int port, boolean useAutoPortDetect) {
        final SDLoader server = new SDLoader(port);
        server.setAutoPortDetect(useAutoPortDetect);
        final WebAppContext ap = new WebAppContext("/loop", "src/test/java/test/loopstart");
        server.addWebAppContext(ap);
        LogManager.getLogManager().reset();
        server.start();
        try {
            List<Thread> tlist = new ArrayList<Thread>();
            for (int i = 0; i < 10; i++) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        long time = System.currentTimeMillis();
                        String url = "http://localhost:" + server.getPort() + ap.getContextPath() + "/loop?loop=" + time + "&filter=" + time;
                        try {
                            String result = SDLoaderLoopStartTest.getRequestContent(url, "GET");
                            Assert.assertEquals(time + "" + time, result);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                    }
                };
                tlist.add(t);
                t.start();
                Thread.sleep(100);
            }
            for (Thread t : tlist) {
                t.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            server.stop();
        }
    }

    public static String getRequestContent(String urlText, String method) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setUseCaches(false);
        urlcon.setDoInput(true);
        urlcon.setDoOutput(true);
        urlcon.setRequestMethod(method);
        BufferedReader reader = null;
        String line = null;
        try {
            urlcon.connect();
            reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
            line = reader.readLine();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
            try {
                urlcon.disconnect();
            } catch (Exception e) {
            }
        }
        return line;
    }
}
