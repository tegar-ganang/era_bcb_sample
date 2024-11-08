package jacg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stellt Methoden f�r die Dateiverarbeitung zur Verf�gung.
 * 
 * @author                       Carsten Spr�ner
 */
public class FileUtil {

    /**
     * Konstante f�r die Trennzeichen innerhalb Verzeichnisnamen. Default: /
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator", "/");

    /**
     * Zeilenendezeichen Default: \n
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    /**
     * Schreibt den Inhlat des InputStream in ein byte[]
     * 
     * @param is
     *            InputStream
     * @return byte[]
     * @throws IOException
     */
    public static byte[] streamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        is.close();
        return baos.toByteArray();
    }

    /**
     * Schreibt den Inhalt eines byte[] in ein OutputStream
     * 
     * @param data
     *            byte[]
     * @param os
     *            OutputStream
     * @throws IOException
     */
    public static void byteArrayToStream(byte[] data, OutputStream os) throws IOException {
        os.write(data);
        os.close();
    }
}
