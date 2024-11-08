package org.noranj.formak.server.utils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.noranj.formak.shared.dto.SystemUserDTO;
import org.noranj.formak.shared.type.ActivityType;
import org.noranj.formak.shared.type.PartyRoleType;
import com.google.appengine.api.taskqueue.QueueFailureException;

/**  
 *  
 * @author
 * @since 0.3.2012MAR06
 * @version 0.3.2012MAR06
 * @change
 */
public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Serialize an object into a byte array.
     * 
     * @param obj An object to be serialized.
     * @return A byte array containing the serialized object
     * @throws QueueFailureException If an I/O error occurs during the
     * serialization process.
     */
    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(bytesOut));
            objectOut.writeObject(obj);
            objectOut.close();
            return encodeBase64(bytesOut.toByteArray());
        } catch (IOException e) {
            throw new QueueFailureException(e);
        }
    }

    /**
     * Deserialize an object from an HttpServletRequest input stream. Does not
     * throw any exceptions; instead, exceptions are logged and null is returned.
     * 
     * @param req An HttpServletRequest that contains a serialized object.
     * @return An object instance, or null if an exception occurred.
     */
    public static Object deserialize(HttpServletRequest req) {
        if (req.getContentLength() == 0) {
            logger.severe("request content length is 0");
            return null;
        }
        try {
            byte[] bytesIn = new byte[req.getContentLength()];
            req.getInputStream().readLine(bytesIn, 0, bytesIn.length);
            return deserialize(bytesIn);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error deserializing task", e);
            return null;
        }
    }

    /**
     * Deserialize an object from a byte array. Does not throw any exceptions;
     * instead, exceptions are logged and null is returned.
     * 
     * @param bytesIn A byte array containing a previously serialized object.
     * @return An object instance, or null if an exception occurred.
     */
    public static Object deserialize(byte[] bytesIn) {
        ObjectInputStream objectIn = null;
        try {
            bytesIn = decodeBase64(bytesIn);
            objectIn = new ObjectInputStream(new BufferedInputStream(new ByteArrayInputStream(bytesIn)));
            return objectIn.readObject();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deserializing task", e);
            return null;
        } finally {
            try {
                if (objectIn != null) {
                    objectIn.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * 
     * @param e
     * @return
     */
    public static String stackTraceToString(Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) pw.close();
                if (sw != null) sw.close();
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }

    /**
     * It reads the whole content of an input stream and return it as byte[].
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int read = 0;
        do {
            read = in.read(buf);
            out.write(buf, 0, read);
        } while (read > 0);
        out.flush();
        out.close();
        return (out.toByteArray());
    }

    /**
     * NOTE: Do not move this method to 'shared' package. 
     * java.util.Properties is not supported by GWT.
     * @param mapContent
     * @return
     */
    public static Map<String, String> buildMap(String mapContent) {
        Properties prop = new Properties();
        try {
            prop.load(new ByteArrayInputStream(mapContent.getBytes()));
        } catch (IOException ioex) {
            logger.severe("Failed to extract the map content from [" + mapContent + "]");
        }
        return ((Map) prop);
    }
}
