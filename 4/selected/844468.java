package net.rootnode.loomchild.util.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.jar.Manifest;
import net.rootnode.loomchild.util.exceptions.IORuntimeException;
import net.rootnode.loomchild.util.exceptions.ResourceNotFoundException;

/**
 * Static utility general puropose methods.
 * 
 * @author loomchild
 */
public class Util {

    public static final int READ_BUFFER_SIZE = 1024;

    public static BufferedReader getReader(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            return reader;
        } catch (UnsupportedEncodingException e) {
            throw new IORuntimeException(e);
        }
    }

    public static PrintWriter getWriter(OutputStream outputStream) {
        try {
            return new PrintWriter(new OutputStreamWriter((outputStream), "utf-8"), true);
        } catch (UnsupportedEncodingException e) {
            throw new IORuntimeException(e);
        }
    }

    public static FileInputStream getFileInputStream(String fileName) {
        try {
            return new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            throw new IORuntimeException(e);
        }
    }

    public static FileOutputStream getFileOutputStream(String fileName) {
        try {
            return new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * Znajduje zasób i zwraca go w postaci strumienia wejściowego. Do szukania
	 * zasobu używa systemowego Classloader-a.
	 * 
	 * @param name
	 *            Nazwa zasobu.
	 * @return Zwraca strumień wejściowy zasobu.
	 * @throws ResourceNotFoundException
	 *             Zgłaszany gdy nie udało się odnaleźć zasobu.
	 */
    public static InputStream getResourceStream(String name) {
        InputStream inputStream = Util.class.getClassLoader().getResourceAsStream(name);
        if (inputStream == null) {
            throw new ResourceNotFoundException(name);
        }
        return inputStream;
    }

    /**
	 * Znajduje ścieżkę do zasobu. Trzeba ograniczyć używanie ponieważ nie
	 * działa poprawnie gdy zasób znajduje się w archiwum JAR.
	 * 
	 * @param name
	 *            Nazwa zasobu.
	 * @return Zwraca ścieżkę do zasobu.
	 * @throws ResourceNotFoundException
	 *             Zgłaszany gdy nie udało się odnaleźć zasobu.
	 */
    public static String getResourcePath(String name) {
        URL url = Util.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new ResourceNotFoundException(name);
        }
        return url.getPath();
    }

    public static String read(Reader reader, int count) {
        try {
            char[] readBuffer = new char[count];
            reader.read(readBuffer);
            return new String(readBuffer);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * Wczytuje całą zawartość strumienia wejściowego do napisu. W razie
	 * niepowodzenia zgłasza wyjątek.
	 * 
	 * @param reader
	 *            Strumień wejściowy.
	 * @return Zwraca napis zawierający odczytany strumień.
	 * @throws IORuntimeException
	 *             gdy wystąpi błąd IO.
	 */
    public static String readAll(Reader reader) {
        StringWriter writer = new StringWriter();
        copyAll(reader, writer);
        return writer.toString();
    }

    public static void copyAll(Reader reader, Writer writer) {
        try {
            char[] readBuffer = new char[READ_BUFFER_SIZE];
            int count;
            while ((count = reader.read(readBuffer)) != -1) {
                writer.write(readBuffer, 0, count);
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static String getFileExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
        if (dotPosition == -1) {
            return "";
        } else {
            return fileName.substring(dotPosition);
        }
    }

    public static final String MANIFEST_PATH = "/META-INF/MANIFEST.MF";

    /**
	 * Returns Manifest of a jar containing given class. If class is not
	 * in a jar, throws {@link ResourceNotFoundException}.
	 * @param klass Class.
	 * @return Manifest.
	 * @throws ResourceNotFoundException Thrown if manifest was not found.
	 */
    public static Manifest getJarManifest(Class<?> klass) {
        URL classUrl = klass.getResource(klass.getSimpleName() + ".class");
        if (classUrl == null) {
            throw new IllegalArgumentException("Class not found: " + klass.getName() + ".");
        }
        String classPath = classUrl.toString();
        int jarIndex = classPath.indexOf('!');
        if (jarIndex != -1) {
            String manifestPath = classPath.substring(0, jarIndex + 1) + MANIFEST_PATH;
            try {
                URL manifestUrl = new URL(manifestPath);
                InputStream manifestStream = manifestUrl.openStream();
                Manifest manifest = new Manifest(manifestStream);
                return manifest;
            } catch (IOException e) {
                throw new ResourceNotFoundException("IO Error retrieving manifest.", e);
            }
        } else {
            throw new ResourceNotFoundException("Class is not in a JAR archive " + klass.getName() + ".");
        }
    }
}
