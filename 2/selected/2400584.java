package updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    interface UrlLineHandler {

        boolean process(String line);
    }

    public static boolean processUrl(String urlPath, UrlLineHandler handler) {
        boolean ret = true;
        URL url;
        InputStream in = null;
        BufferedReader bin = null;
        try {
            url = new URL(urlPath);
            in = url.openStream();
            bin = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = bin.readLine()) != null) {
                if (!handler.process(line)) break;
            }
        } catch (IOException e) {
            ret = false;
        } finally {
            safelyClose(bin, in);
        }
        return ret;
    }

    public static String extractSingleOccurrencePattern(String urlPath, final String pattern) {
        class PatternExtractor implements UrlLineHandler {

            private Pattern p = Pattern.compile(pattern);

            String s = null;

            public boolean process(String line) {
                Matcher m = p.matcher(line);
                if (!m.find()) return true;
                s = m.group(1);
                return false;
            }
        }
        PatternExtractor extractor = new PatternExtractor();
        if (processUrl(urlPath, extractor)) return extractor.s;
        return null;
    }

    public static Vector<String> extractMultiOccurrencePattern(String urlPath, final String pattern) {
        class PatternExtractor implements UrlLineHandler {

            private Pattern p = Pattern.compile(pattern);

            Vector<String> lines = new Vector<String>();

            public boolean process(String line) {
                Matcher m = p.matcher(line);
                if (m.find()) lines.add(m.group(1));
                return true;
            }
        }
        PatternExtractor extractor = new PatternExtractor();
        if (processUrl(urlPath, extractor)) return extractor.lines;
        return null;
    }

    public interface ProgressHandler {

        void setSize(int size);

        void bytesRead(int numBytes);

        boolean isAborted();
    }

    private static final int BUFFER = 2048;

    public static File downloadFile(String urlString, String downloadTo, ProgressHandler progress) {
        File targetFile = null;
        URL url;
        InputStream in = null;
        BufferedInputStream bin = null;
        FileOutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            url = new URL(urlString);
            URLConnection conn = url.openConnection();
            progress.setSize(conn.getContentLength());
            in = conn.getInputStream();
            bin = new BufferedInputStream(in);
            targetFile = new File(downloadTo);
            out = new FileOutputStream(targetFile);
            bout = new BufferedOutputStream(out);
            byte[] buffer = new byte[BUFFER];
            int bytesRead;
            int totalBytes = 0;
            long time = System.currentTimeMillis();
            while ((bytesRead = bin.read(buffer)) > 0) {
                totalBytes += bytesRead;
                bout.write(buffer, 0, bytesRead);
                long time1 = System.currentTimeMillis();
                if (time1 - time >= 1000) {
                    if (progress.isAborted()) return null;
                    time = time1;
                    progress.bytesRead(totalBytes);
                }
            }
        } catch (IOException e) {
            targetFile = null;
        } finally {
            safelyClose(bin, in);
            safelyClose(bout, out);
        }
        return targetFile;
    }

    public static void safelyClose(BufferedOutputStream bout, OutputStream out) {
        if (bout != null) {
            try {
                bout.close();
                out = null;
            } catch (IOException e) {
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    public static void safelyClose(Object bin, InputStream in) {
        if (bin != null) {
            try {
                if (bin instanceof Reader) ((Reader) bin).close(); else if (bin instanceof InputStream) ((InputStream) bin).close();
                in = null;
            } catch (IOException e) {
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }
}
