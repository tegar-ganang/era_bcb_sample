package org.mandarax.zkb;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.mandarax.util.xmlser.LogExceptionListener;

/**
 * Object persistency service based on XML serialization.
 * <strong>Warning:</strong>Requires JDK 1.4 or better!
 * @author <A href="http://www-ist.massey.ac.nz/JBDietrich" target="_top">Jens Dietrich</A>
 * @version 3.4 <7 March 05>
 * @since 2.2
 */
public class XMLSerializationOPS extends AbstractOPS {

    /**
	 * Export objects.
	 * @param out an output stream
	 * @exception an io exception
	 */
    public void exportObjects(OutputStream out) throws IOException {
        XMLEncoder encoder = new XMLEncoder(out);
        encoder.setExceptionListener(new LogExceptionListener());
        encoder.writeObject(objectsByUri);
        encoder.close();
    }

    /**
	 * Import objects.
	 * @param in an input stream
	 * @exception an io exception
	 */
    public void importObjects(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte b = -1;
        while ((b = (byte) in.read()) > -1) buf.write(b);
        InputStream bin = new ByteArrayInputStream(buf.toByteArray());
        XMLDecoder decoder = new XMLDecoder(bin);
        decoder.setExceptionListener(new LogExceptionListener());
        reset();
        objectsByUri = (Map) decoder.readObject();
        reverseMap(objectsByUri, urisByObject);
        decoder.close();
    }

    /**
	 * Get name.
	 * @return the name of the service
	 */
    public String getName() {
        return "XML serialization";
    }

    /**
	 * Get a brief description.
	 * @return a brief description of the service
	 */
    public String getDescription() {
        return "Object persistency service based on JDK XML serialization, requires JDK 1.4 or better";
    }

    /**
	 * Get the extension of the associated files.
	 * @return a file extension
	 */
    public String getExtension() {
        return "xml";
    }
}
