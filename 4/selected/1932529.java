package jgnash.xml;

import java.io.*;
import javax.crypto.*;

/** XMLFileOutputStream that is used to write XMLObjects out as a xml file
 *
 * @author Craig Cavanaugh
 *
 * $Id: XMLFileOutputStream.java 675 2008-06-17 01:36:01Z ccavanaugh $
 */
public class XMLFileOutputStream extends XMLOutputStream {

    /** Constructor for write to a file without encryption
     * @param fileName name of the file to write
     * @throws IOException
     */
    public XMLFileOutputStream(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
        ((FileOutputStream) stream).getChannel().lock();
    }

    /** Constructor for write to a file with encryption
     * @param fileName name of the file to write
     * @param password the password for the file
     * @throws IOException
     */
    public XMLFileOutputStream(String fileName, char[] password) throws IOException {
        super(new CipherOutputStream(new FileOutputStream(fileName), generateCipher(Cipher.ENCRYPT_MODE, password)));
    }

    public void close() {
        writer.close();
    }
}
