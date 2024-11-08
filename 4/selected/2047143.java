package purej.util.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

/**
 * Utility methods once in TextUtils that require the mail.jar library.
 * 
 * @author $Author: hani $
 * @version $Revision: 1.1.1.1 $
 */
public class MailUtils {

    /**
     * Decode binary data from String using base64.
     * 
     * @see #encodeBytes(byte[])
     */
    public static final byte[] decodeBytes(String str) throws IOException {
        try {
            ByteArrayInputStream encodedStringStream = new ByteArrayInputStream(str.getBytes());
            InputStream decoder = MimeUtility.decode(encodedStringStream, "base64");
            ByteArrayOutputStream decodedByteStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            while (true) {
                int read = decoder.read(buffer);
                if (read == -1) {
                    break;
                }
                decodedByteStream.write(buffer, 0, read);
            }
            decodedByteStream.flush();
            return decodedByteStream.toByteArray();
        } catch (MessagingException me) {
            throw new IOException("Cannot decode data.");
        }
    }

    /**
     * Encode binary data into String using base64.
     * 
     * @see #decodeBytes(java.lang.String)
     */
    public static final String encodeBytes(byte[] data) throws IOException {
        try {
            ByteArrayOutputStream encodedByteStream = new ByteArrayOutputStream();
            OutputStream encoder = MimeUtility.encode(encodedByteStream, "base64");
            encoder.write(data);
            encoder.flush();
            return new String(encodedByteStream.toByteArray());
        } catch (MessagingException me) {
            throw new IOException("Cannot encode data.");
        }
    }

    /**
     * Verify that the given string is a valid email address. "Validity" in this
     * context only means that the address conforms to the correct syntax (not
     * if the address actually exists).
     * 
     * @param email
     *                The email address to verify.
     * @return a boolean indicating whether the email address is correctly
     *         formatted.
     */
    public static final boolean verifyEmail(String email) {
        if (email == null) {
            return false;
        }
        if (email.indexOf('@') < 1) {
            return false;
        }
        try {
            new InternetAddress(email);
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
