package scap.check.content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.xmlbeans.XmlException;

public class URLSourceContent implements SourceContent, SourceContext {

    public static final String TEMP_PREFIX = "tmpfile-";

    public static final String TEMP_SUFFIX = ".tmp";

    private final HrefResolver resolver;

    private final URI uri;

    private File localFile;

    public URLSourceContent(URI url) {
        this(url, new DefaultHrefResolver());
    }

    public URLSourceContent(URI url, HrefResolver resolver) {
        this.uri = url;
        this.resolver = resolver;
    }

    public URI getUri() {
        return uri;
    }

    /**
	 * @return the resolver
	 */
    protected HrefResolver getResolver() {
        return resolver;
    }

    public String getId() {
        return getUri().toASCIIString();
    }

    public File getLocalFile() throws IOException {
        if (localFile == null) {
            URI uri = getUri();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                localFile = new File(uri);
            } else {
                localFile = generateTempFile();
                writeContent(localFile);
            }
        }
        return localFile;
    }

    public SourceContent getSourceContent() {
        return this;
    }

    public XmlBeansInstance getXmlBeansInstance() throws XmlException, IOException {
        return new XmlBeansInstance(getUri().toURL());
    }

    public SourceContext resolveRelative(String href) throws IOException {
        return getResolver().resolveRelative(getUri(), href, this);
    }

    protected File generateTempFile() throws IOException {
        File retval = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        retval.deleteOnExit();
        return retval;
    }

    protected void writeContent(File file) throws IOException {
        InputStream in = getUri().toURL().openStream();
        FileOutputStream out = new FileOutputStream(file);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (this == other) {
            result = true;
        } else if (other instanceof URLSourceContent) {
            URLSourceContent that = (URLSourceContent) other;
            result = (that.canEqual(this) && this.getUri().equals(that.getUri()) && this.getResolver().equals(that.getResolver()));
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 15;
        hash = hash * 31 + getUri().hashCode();
        hash = hash * 31 + getResolver().hashCode();
        return hash;
    }

    public boolean canEqual(Object other) {
        return (other instanceof URLSourceContent);
    }
}
