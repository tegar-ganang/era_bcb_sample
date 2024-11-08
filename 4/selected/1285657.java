package listo.utils;

import org.apache.commons.lang.SystemUtils;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Random;

public class FileUtils2 {

    public static String readAllTextFromResource(String resource) {
        return readAllText(ClassLoader.getSystemClassLoader().getResourceAsStream(resource));
    }

    public static String readAllText(String filename) {
        return readAllText(new File(filename));
    }

    public static String readAllText(String filename, Charset charset) {
        return readAllText(new File(filename), charset);
    }

    public static String readAllText(File file) {
        return readAllText(file, Charset.defaultCharset());
    }

    public static String readAllText(File file, Charset charset) {
        try {
            return readAllText(new FileInputStream(file), charset);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readAllText(InputStream stream) {
        return readAllText(stream, Charset.defaultCharset());
    }

    public static String readAllText(InputStream stream, Charset charset) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
        StringWriter writer = new StringWriter();
        copyAll(reader, writer);
        return writer.toString();
    }

    public static byte[] readAllBytesFromResource(String resource) {
        return readAllBytes(ClassLoader.getSystemClassLoader().getResourceAsStream(resource));
    }

    public static byte[] readAllBytes(String filename) {
        return readAllBytes(new File(filename));
    }

    public static byte[] readAllBytes(File file) {
        try {
            return readAllBytes(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readAllBytes(InputStream stream) {
        BufferedInputStream in = new BufferedInputStream(stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copyAll(in, out);
        return out.toByteArray();
    }

    public static void writeAllText(String text, String filename) {
        writeAllText(text, new File(filename));
    }

    public static void writeAllText(String text, String filename, Charset charset) {
        writeAllText(text, new File(filename), charset);
    }

    public static void writeAllText(String text, File file) {
        try {
            writeAllText(text, new FileOutputStream(file), Charset.defaultCharset());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllText(String text, File file, Charset charset) {
        try {
            writeAllText(text, new FileOutputStream(file), charset);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllText(String text, OutputStream stream) {
        writeAllText(text, stream, Charset.defaultCharset());
    }

    public static void writeAllText(String text, OutputStream stream, Charset charset) {
        StringReader reader = new StringReader(text);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, charset));
        copyAll(reader, writer);
    }

    public static void writeAllBytes(byte[] data, String filename) {
        writeAllBytes(data, new File(filename));
    }

    public static void writeAllBytes(byte[] data, File file) {
        try {
            writeAllBytes(data, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAllBytes(byte[] data, OutputStream stream) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        BufferedOutputStream out = new BufferedOutputStream(stream);
        copyAll(in, out);
    }

    public static void copyAll(Reader reader, Writer writer) {
        try {
            char[] data = new char[4096];
            int count;
            while ((count = reader.read(data)) >= 0) writer.write(data, 0, count);
            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyAll(InputStream in, OutputStream out) {
        try {
            byte[] data = new byte[4096];
            int count;
            while ((count = in.read(data)) >= 0) {
                out.write(data, 0, count);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getTempFile() {
        File file;
        do {
            file = getTempFile("temp" + new Random().nextInt() + ".tmp");
        } while (file.exists());
        return file;
    }

    public static String getTempFileName() {
        try {
            return getTempFile().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTempFileName(String filename) {
        try {
            return SystemUtils.getJavaIoTmpDir().getCanonicalPath() + File.separatorChar + filename;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getTempFile(String filename) {
        return new File(getTempFileName(filename));
    }
}
