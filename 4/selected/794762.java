package net.sf.buildbox.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

/**
 * Unsorted utilities.
 *
 * @author Attila Pal
 * @author Jaroslav Sovicka
 * @author Petr Kozelka
 * @author Roman Dolejsi
 * @author Vojtech Habarta
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Retrieves serialVersionUID.
     *
     * @param type the examined class
     * @return serialVersionUID
     */
    public static long getClassSerial(Class type) {
        ObjectStreamClass osc = ObjectStreamClass.lookup(type);
        return (osc == null) ? 0 : osc.getSerialVersionUID();
    }

    /**
     * Serializes an object to array of bytes.
     *
     * @param object object to be serialized
     * @return null or array of bytes
     * @throws IOException whenever serialization fails
     */
    public static byte[] objectToBytes(Serializable object) throws IOException {
        if (object != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
            return baos.toByteArray();
        } else {
            return null;
        }
    }

    /**
     * Deserializes array of bytes.
     *
     * @param bytes array to be deserialized
     * @return the object created
     * @throws IOException            whenever serialization fails
     * @throws ClassNotFoundException when deserializing non-existent class
     */
    public static Serializable bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        if ((bytes != null) && bytes.length > 0) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Serializable) ois.readObject();
        } else {
            return null;
        }
    }

    /**
     * This method reads the contents of the stream into the byte array.
     *
     * @param in   the inputstream
     * @param size initial size of buffer in bytes
     * @return returns a byte array read from the stream
     * @throws java.io.IOException if some I/O error occurs
     * @throws java.lang.IllegalArgumentException
     *                             if size is negative
     */
    public static byte[] readBytes(InputStream in, int size) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            baos.write(buffer, 0, len);
        }
        in.close();
        return baos.toByteArray();
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        return readBytes(in, 1024);
    }

    public static String readString(Reader from) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = from.read(buffer)) >= 0) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    public static void sleepTill(Date date) throws InterruptedException {
        long till = date.getTime();
        long diff;
        while ((diff = till - System.currentTimeMillis()) > 0) {
            Thread.sleep(diff);
        }
    }

    /**
     * Converts URI to URL.
     * If scheme is "res" URI is treated as java resource otherwise URI.toURL() is used.
     *
     * @param uri the uri to be converted
     * @return url corresponding to the uri
     * @throws MalformedURLException if URI with 'res' scheme doesn't start with '/' or if URI.toURL() throws it
     */
    public static URL uriToUrl(URI uri) throws MalformedURLException {
        if ("res".equals(uri.getScheme())) {
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            if (!schemeSpecificPart.startsWith("/")) {
                throw new MalformedURLException("Scheme specific part of 'res' URI doesn't start with '/': " + uri);
            }
            URL resource = Utils.class.getResource(schemeSpecificPart);
            if (resource == null) {
                throw new MalformedURLException("URI not found: " + uri);
            }
            return resource;
        } else {
            if (uri.isAbsolute()) {
                return uri.toURL();
            } else {
                throw new MalformedURLException("URI is not absolute: " + uri);
            }
        }
    }

    /**
     * Resolves relative uri against base uri. This method can handle also base uri with schema "jar".
     *
     * @param base base uri (absolute)
     * @param uri  the examined uri
     * @return absolute uri
     * @throws URISyntaxException if base uri is "jar" uri and resource is not found
     */
    public static URI resolve(URI base, URI uri) throws URISyntaxException {
        if (base.getScheme() == null) {
            throw new IllegalArgumentException("Base URI is not absolute: " + base);
        }
        if (base.getScheme().equals("jar")) {
            String schemeSpecificPart = base.getSchemeSpecificPart();
            int index = schemeSpecificPart.indexOf('!');
            String resourceName = schemeSpecificPart.substring(index + 1);
            URI resourceUri = new URI("res", resourceName, null).resolve(uri);
            URL resource = Utils.class.getResource(resourceUri.getSchemeSpecificPart());
            if (resource == null) {
                throw new URISyntaxException(resourceUri.toString(), "Resource not found");
            }
            return new URI(resource.toString());
        } else {
            return base.resolve(uri);
        }
    }

    public static File resolve(File base, String relPath) {
        final File f = new File(relPath);
        if (f.isAbsolute()) return f;
        return new File(base, relPath);
    }

    /**
     * Copies an <code>InputStream</code> to an <code>OutputStream</code>.
     *
     * @param in  stream to copy from
     * @param out stream to copy to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs (may result in partially done work)
     */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] copyBuffer = new byte[8192];
        long bytesCopied = 0;
        int read;
        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
            out.write(copyBuffer, 0, read);
            bytesCopied += read;
        }
        return bytesCopied;
    }

    public static String getBinaryName(Class type) {
        if (type.getDeclaringClass() != null) {
            int i = type.getName().lastIndexOf('.');
            String shortName = i != -1 ? type.getName().substring(i + 1) : type.getName();
            return getBinaryName(type.getDeclaringClass()) + "$" + shortName;
        } else {
            return type.getName();
        }
    }
}
