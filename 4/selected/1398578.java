package rubbish.db.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import rubbish.db.exception.IORuntimeException;

/**
 * ��o�̓��[�e�B���e�B
 *
 * @author $Author: winebarrel $
 * @version $Revision: 1.2 $
 */
public class IOUtils {

    public static void pipe(InputStream in, OutputStream out) {
        byte[] buf = new byte[1024];
        int len = 0;
        try {
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void pipe(Reader reader, Writer writer) {
        char[] buf = new char[256];
        int len = 0;
        try {
            while ((len = reader.read(buf)) > 0) writer.write(buf, 0, len);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static String readFile(String filename) {
        Writer writer = new StringWriter();
        Reader reader = null;
        try {
            reader = new FileReader(filename);
            try {
                pipe(reader, writer);
            } finally {
                if (reader != null) reader.close();
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return writer.toString();
    }
}
