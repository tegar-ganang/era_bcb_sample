package org.nakedobjects.utility;

import org.nakedobjects.system.AboutNakedObjects;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

public class RemoteLogger {

    private static final Logger LOG = Logger.getLogger(RemoteLogger.class);

    private static final String URL_SPEC = "http://development.nakedobjects.net/errors/log.php";

    /**
     * Submits an error log to the development server.
     */
    public static void submitLog(String message, String detail, String proxyAddress, int proxyPort) {
        String user = System.getProperty("user.name");
        String system = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") " + System.getProperty("os.version");
        String java = System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version");
        String version = AboutNakedObjects.getVersion() + " " + AboutNakedObjects.getBuildId();
        try {
            URL url = proxyAddress == null ? new URL(URL_SPEC) : new URL("http", proxyAddress, proxyPort, URL_SPEC);
            LOG.info("connect to " + url);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            HttpQueryWriter out = new HttpQueryWriter(connection.getOutputStream());
            out.addParameter("user", user);
            out.addParameter("system", system);
            out.addParameter("vm", java);
            out.addParameter("error", message);
            out.addParameter("trace", detail);
            out.addParameter("version", version);
            out.close();
            InputStream in = connection.getInputStream();
            int c;
            StringBuffer result = new StringBuffer();
            while ((c = in.read()) != -1) {
                result.append((char) c);
            }
            LOG.info(result);
            in.close();
        } catch (UnknownHostException e) {
            LOG.info("could not find host (unknown host) to submit log to");
        } catch (IOException e) {
            LOG.debug("i/o problem submitting log", e);
        }
    }

    private static class HttpQueryWriter extends OutputStreamWriter {

        private int parameter = 1;

        public HttpQueryWriter(OutputStream outputStream) throws UnsupportedEncodingException {
            super(outputStream, "ASCII");
        }

        public void addParameter(String name, String value) throws IOException {
            if (name == null || value == null) {
                return;
            }
            if (parameter > 1) {
                write("&");
            }
            parameter++;
            write(URLEncoder.encode(name));
            write("=");
            write(URLEncoder.encode(value));
        }

        public void close() throws IOException {
            write("\r\n");
            flush();
            super.close();
        }
    }
}
