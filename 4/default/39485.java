import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

class LoaderTread implements Runnable {

    private URL webPage = null;

    private ByteArrayOutputStream pageBytes = new ByteArrayOutputStream();

    private boolean finished = false;

    private Date lastRead = new Date();

    private long urlLength = 0;

    private int returnCode = -1;

    public LoaderTread(URL pageUrl) {
        webPage = pageUrl;
        try {
            DataStore store = DataStore.getInstance();
            String proxy = store.getProperty("proxy.server");
            proxy = proxy.trim();
            if (proxy.length() > 0) {
                System.out.println("Using Proxy : " + proxy + " : " + store.getProperty("proxy.port").trim());
                System.getProperties().put("proxySet", "true");
                System.getProperties().put("proxyHost", proxy);
                System.getProperties().put("proxyPort", store.getProperty("proxy.port").trim());
            } else {
                System.getProperties().put("proxySet", "false");
                System.getProperties().put("proxyHost", "");
                System.getProperties().put("proxyPort", "");
            }
            Authenticator.setDefault(new HTTPAuthenticator());
        } catch (Exception e) {
            System.out.println("Error setting HTTP proxy");
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance();
            lastRead = new Date();
            HttpURLConnection con = null;
            InputStream is = null;
            try {
                String verString = DataStore.getInstance().getVersion();
                con = (HttpURLConnection) webPage.openConnection();
                con.setRequestProperty("User-Agent", "TVSchedulerPro(" + verString + ")");
                is = con.getInputStream();
            } catch (Exception e) {
                System.out.println("ERROR: Url Exception (" + e.toString() + ")");
                finished = true;
                return;
            }
            returnCode = con.getResponseCode();
            int colCount = 0;
            lastRead = new Date();
            byte[] buff = new byte[128];
            int read = is.read(buff);
            while (read > -1) {
                if (colCount == 80) {
                    System.out.println("Downloaded: " + nf.format(pageBytes.size()));
                    colCount = 0;
                }
                colCount++;
                lastRead = new Date();
                pageBytes.write(buff, 0, read);
                read = is.read(buff);
            }
            System.out.println("Downloaded: " + nf.format(pageBytes.size()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        finished = true;
    }

    public void kill() {
        returnCode = -1;
    }

    public int getResponceCode() {
        return returnCode;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getDataString() {
        return pageBytes.toString();
    }

    public byte[] getDataBytes() {
        return pageBytes.toByteArray();
    }

    public boolean isTimedOut(int sec) {
        Date now = new Date();
        long timeOut = sec * 1000;
        long lifeTime = now.getTime() - lastRead.getTime();
        if (lifeTime > timeOut) return true; else return false;
    }

    public long getLength() {
        return urlLength;
    }
}
