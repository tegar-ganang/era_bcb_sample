package de.lichtflut.infra.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import de.lichtflut.infra.logging.Log;

/**
 * Encoding util.
 * 
 * Created: 02.12.2008
 *
 * @author Oliver Tigges
 */
public class Encoder {

    public static final String UTF_8 = "UTF-8";

    public static final String UUML_UC = "Ü";

    public static final String UUML_LC = "ü";

    public static final String AUML_UC = "Ä";

    public static final String AUML_LC = "ä";

    public static final String OUML_UC = "Ö";

    public static final String OUML_LC = "ö";

    public static Reader createFileReader(String filename, String encoding) throws IOException {
        return createFileReader(new File(filename), encoding);
    }

    public static Reader createFileReader(File file, String encoding) throws IOException {
        InputStream in = new FileInputStream(file);
        return new InputStreamReader(in, encoding);
    }

    public void convert(File file, String fromEncoding, String toEncoding) throws IOException {
        InputStream in = new FileInputStream(file);
        StringWriter cache = new StringWriter();
        Reader reader = new InputStreamReader(in, fromEncoding);
        char[] buffer = new char[128];
        int read;
        while ((read = reader.read(buffer)) > -1) {
            cache.write(buffer, 0, read);
        }
        reader.close();
        in.close();
        Log.warn(this, "read from file " + file + " (" + fromEncoding + "):" + cache);
        OutputStream out = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(out, toEncoding);
        writer.write(cache.toString());
        cache.close();
        writer.close();
        out.close();
    }
}
