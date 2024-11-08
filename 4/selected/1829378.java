package net.sf.lpr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.lpr.exception.IORuntimeException;

/**
 * Utility class for doing some of the dumb work that is needed in more than one place but makes little sense to have in a parent class.
 * @author x_sid
 *
 */
public class LPRUtils {

    private static AtomicInteger jobId = new AtomicInteger(0);

    public static final byte ZERO[] = new byte[] { 0, 0 };

    /**
    * This static method is used for clarity in code to provide a way to throw an exception if the response from the server does not match the expected two bytes of zero
    * @param response byte array of response from the server to be compared.
    * @param request {@link Class} that is making a call to this method.  It is used for the purpose of filling out the exception that can be thrown if the the response from the server does not match.
    * @throws IORuntimeException if the response does not match the two bytes of zero
    */
    public static void checkResponse(byte response[], Class<?> request) throws IORuntimeException {
        if (Arrays.equals(ZERO, response) == false) throw new IORuntimeException("Server returned invalid response while performing action: " + request.getSimpleName() + " response: " + response[0] + "" + response[1]);
    }

    /**
    * Copies the contents of the {@link InputStream} to the {@link OutputStream}.  A call to this method will create a buffer of 4096 bytes to do the copying.  If something like this is needed it would be advisable to use Apache commons IO
    * @param from stream to copy from
    * @param to the stream to copy to.
    * @throws IORuntimeException
    */
    public static void copy(InputStream from, OutputStream to) throws IORuntimeException {
        byte buffer[] = new byte[4096];
        int read;
        try {
            while ((read = from.read(buffer)) != -1) to.write(buffer, 0, read);
            to.flush();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
    * This method is used to in the high level API classes to generate job ID's that are required by the LPR protocol
    * @return int that is the generated ID between 1 and 999
    */
    public static int generateJobId() {
        int toReturn = jobId.incrementAndGet();
        if (toReturn > 999) {
            jobId.set(0);
            toReturn = jobId.incrementAndGet();
        }
        return jobId.incrementAndGet();
    }

    /**
    * Does it's best to return a host name for the machine that it is running on.  Since I first implemented this library on Windows XP the way that I get the host name is by 
    * using the COMPUTERNAME environment variable.  If this returns null then it tries using the HOSTNAME variable that my Fedora 9 box uses to store the host name of the machine.
    * If that fails it then falls back to the {@link InetAddress} class and calls the getLocalHost() method and then the getHostName() method.  If the final attempt fails because
    * of an exception being thrown then the empty string "" is returned.  A patch from someone who knows more about this would be much appreciated.
    * @return String that is the host name of the machine or "" if it was unable to be retrieved.
    */
    public static String getHostname() {
        String toReturn = "";
        toReturn = System.getenv("COMPUTERNAME");
        if (toReturn == null) {
            toReturn = System.getenv("HOSTNAME");
            if (toReturn == null) {
                try {
                    toReturn = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    toReturn = "";
                }
            }
        }
        return toReturn;
    }
}
