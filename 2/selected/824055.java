package org.apache.myfaces.trinidad.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.myfaces.trinidad.resource.AggregatingResourceLoader;
import org.apache.myfaces.trinidad.resource.ResourceLoader;

public class AggregatingResourceLoaderTest extends ResourceLoaderTestCase {

    public AggregatingResourceLoaderTest(String testName) {
        super(testName);
    }

    public void testContentLength() throws IOException {
        ResourceLoader loader = new AggregatingResourceLoader("test.xml", new String[] { "test-1.xml", "test-2.xml" }, new LocalResourceLoader());
        doTestContentLength(loader.getResource("test.xml"));
    }

    public void testContentLengthWithException() throws IOException {
        try {
            AggregatingResourceLoader loader = new AggregatingResourceLoader("test.xml", new String[] { "test-1.xml", "test-exception.xml", "test-2.xml" }, new ThrowingResourceLoader());
            loader.setSeparator("\n");
            doTestContentLength(loader.getResource("test.xml"));
            assertTrue("Expected IOException was not thrown.", false);
        } catch (IOException e) {
            if (!"This test exception is expected".equals(e.getMessage())) {
                throw new IOException(e.getMessage());
            }
        }
    }

    public void testUnknownContentLength() throws IOException {
        AggregatingResourceLoader loader = new AggregatingResourceLoader("test.xml", new String[] { "test-1.xml", "unknown-length.xml", "test-2.xml" }, new UnknownLengthResourceLoader());
        loader.setSeparator("\n");
        doTestUnknownContentLength(loader.getResource("test.xml"));
    }

    private class ThrowingResourceLoader extends LocalResourceLoader {

        @Override
        protected URL findResource(String name) throws IOException {
            if ("test-exception.xml".equals(name)) throw new IOException("This test exception is expected");
            return super.findResource(name);
        }
    }

    private class UnknownLengthResourceLoader extends LocalResourceLoader {

        @Override
        protected URL findResource(String name) throws IOException {
            if ("unknown-length.xml".equals(name)) return new URL(super.findResource("test-1.xml"), name, new UnknownLengthStreamHandler());
            return super.findResource(name);
        }
    }

    private class UnknownLengthStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new UnknownLengthURLConnection(url);
        }
    }

    private class UnknownLengthURLConnection extends URLConnection {

        public UnknownLengthURLConnection(URL url) {
            super(url);
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
        }
    }
}
