package siena.remote.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import junit.framework.TestCase;
import siena.SienaException;
import siena.remote.URLConnector;

public class URLConnectorTest extends TestCase {

    public void testURLConnector() throws Exception {
        URLConnector connector = new URLConnector();
        Properties properties = new Properties();
        properties.setProperty("backend", "http://example.com/");
        connector.configure(properties);
        connector.connect();
        OutputStream out = connector.getOutputStream();
        out.close();
        InputStream in = connector.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int read;
        do {
            read = in.read(buff);
            if (read == -1) break;
            baos.write(buff, 0, read);
        } while (true);
        String s = new String(baos.toByteArray());
        assertTrue(s.indexOf("http://www.rfc-editor.org/rfc/rfc2606.txt") > 0);
        connector.close();
    }

    public void testMalformedURL() {
        URLConnector connector = new URLConnector();
        Properties properties = new Properties();
        properties.setProperty("backend", "hello");
        try {
            connector.configure(properties);
        } catch (SienaException e) {
            return;
        }
        fail("configure() should have been failed due to a MalformedURLException");
    }
}
