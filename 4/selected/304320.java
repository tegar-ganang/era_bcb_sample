package gnu.mail.handler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import javax.activation.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

/**
 * A JAF data content handler for the application/* family of MIME content
 * types.
 * This provides the basic behaviour for any number of byte-array-handling
 * subtypes which simply need to override their default constructor to provide
 * the correct MIME content-type and description.
 */
public abstract class Application implements DataContentHandler {

    /**
   * Our favorite data flavor.
   */
    protected DataFlavor flavor;

    /**
   * Constructor specifying the data flavor.
   * @param mimeType the MIME content type
   * @param description the description of the content type
   */
    protected Application(String mimeType, String description) {
        flavor = new ActivationDataFlavor(byte[].class, mimeType, description);
    }

    /**
   * Returns an array of DataFlavor objects indicating the flavors the data
   * can be provided in.
   * @return the DataFlavors
   */
    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor[] flavors = new DataFlavor[1];
        flavors[0] = flavor;
        return flavors;
    }

    /**
   * Returns an object which represents the data to be transferred.
   * The class of the object returned is defined by the representation class
   * of the flavor.
   * @param flavor the data flavor representing the requested type
   * @param source the data source representing the data to be converted
   * @return the constructed object
   */
    public Object getTransferData(DataFlavor flavor, DataSource source) throws UnsupportedFlavorException, IOException {
        if (this.flavor.equals(flavor)) return getContent(source);
        return null;
    }

    /**
   * Return an object representing the data in its most preferred form.
   * Generally this will be the form described by the first data flavor
   * returned by the <code>getTransferDataFlavors</code> method.
   * @param source the data source representing the data to be converted
   * @return a byte array
   */
    public Object getContent(DataSource source) throws IOException {
        InputStream in = source.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (true) {
            int len = in.read(buf);
            if (len > -1) out.write(buf, 0, len); else break;
        }
        return out.toByteArray();
    }

    /**
   * Convert the object to a byte stream of the specified MIME type and
   * write it to the output stream.
   * @param object the object to be converted
   * @param mimeType the requested MIME content type to write as
   * @param out the output stream into which to write the converted object
   */
    public void writeTo(Object object, String mimeType, OutputStream out) throws IOException {
        if (object instanceof byte[]) {
            byte[] bytes = (byte[]) object;
            out.write(bytes);
            out.flush();
        } else if (object instanceof InputStream) {
            InputStream in = (InputStream) object;
            byte[] bytes = new byte[4096];
            for (int len = in.read(bytes); len != -1; len = in.read(bytes)) out.write(bytes, 0, len);
            out.flush();
        } else throw new IOException("Unsupported data type: " + object.getClass().getName());
    }
}
