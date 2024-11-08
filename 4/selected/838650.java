package gnu.mcvox.io;

import java.io.*;

/** Class for more simpler working with files */
public class FileUtils {

    public static byte[] loadFileAsBytes(String fileName) throws IOException {
        return loadFileAsBytes(new File(fileName));
    }

    public static byte[] loadFileAsBytes(File file) throws IOException {
        byte[] result = new byte[(int) file.length()];
        loadFileAsBytes(file, result);
        return result;
    }

    public static void loadFileAsBytes(File file, byte[] buf) throws IOException {
        loadFileAsBytes(file, buf, 0, buf.length);
    }

    public static void loadFileAsBytes(File file, byte[] buf, int off, int len) throws IOException {
        FileInputStream f = new FileInputStream(file);
        try {
            f.read(buf, off, len);
        } finally {
            try {
                f.close();
            } catch (Exception e) {
            }
            ;
        }
    }

    public static void saveFileFromBytes(String fileName, byte[] buf) throws IOException {
        saveFileFromBytes(new File(fileName), buf);
    }

    public static void saveFileFromBytes(File file, byte[] buf) throws IOException {
        saveFileFromBytes(file, buf, 0, buf.length);
    }

    public static void saveFileFromBytes(File file, byte[] buf, int off, int len) throws IOException {
        FileOutputStream f = new FileOutputStream(file);
        try {
            f.write(buf, off, len);
        } catch (IOException e) {
            try {
                f.close();
            } catch (Exception e1) {
            }
            ;
            return;
        }
        f.close();
    }

    public static void copyFile(String source, String target) throws IOException {
        copyFile(new File(source), new File(target));
    }

    public static void copyFile(File source, File target) throws IOException {
        RandomAccessFile input = new RandomAccessFile(source, "r");
        RandomAccessFile output = new RandomAccessFile(target, "rw");
        try {
            byte[] buf = new byte[65536];
            long len = input.length();
            output.setLength(len);
            int bytesRead;
            while ((bytesRead = input.read(buf, 0, buf.length)) > 0) output.write(buf, 0, bytesRead);
        } catch (IOException e) {
            try {
                input.close();
            } catch (Exception e1) {
            }
            ;
            try {
                output.close();
            } catch (Exception e1) {
            }
            ;
            return;
        }
        try {
            input.close();
        } catch (Exception e) {
        }
        ;
        output.close();
    }

    public static String loadFileAsString(File file, String encoding) throws IOException {
        InputStreamReader f = (encoding == null) ? new FileReader(file) : new InputStreamReader(new FileInputStream(file), encoding);
        StringBuffer sb = new StringBuffer();
        try {
            char[] buf = new char[32768];
            int len;
            while ((len = f.read(buf, 0, buf.length)) >= 0) sb.append(buf, 0, len);
            return sb.toString();
        } finally {
            try {
                f.close();
            } catch (Exception e) {
            }
            ;
        }
    }

    public static String loadFileAsString(String fileName, String encoding) throws IOException {
        return loadFileAsString(new File(fileName), encoding);
    }

    public static char[] loadFileAsChars(String fileName, String encoding) throws IOException {
        return loadFileAsChars(new File(fileName), encoding);
    }

    public static char[] loadFileAsChars(File file, String encoding) throws IOException {
        String buf = loadFileAsString(file, encoding);
        char[] result = new char[buf.length()];
        buf.getChars(0, result.length, result, 0);
        return result;
    }

    public static void loadFileAsChars(File file, String encoding, char[] buf) throws IOException {
        loadFileAsChars(file, encoding, buf, 0, buf.length);
    }

    public static void loadFileAsChars(File file, String encoding, char[] buf, int off, int len) throws IOException {
        InputStreamReader f = (encoding == null) ? new FileReader(file) : new InputStreamReader(new FileInputStream(file), encoding);
        try {
            f.read(buf, off, len);
        } finally {
            try {
                f.close();
            } catch (Exception e) {
            }
            ;
        }
    }

    public static void saveFileFromString(String fileName, String encoding, String written) throws IOException {
        saveFileFromString(new File(fileName), encoding, written);
    }

    public static void saveFileFromString(File file, String encoding, String written) throws IOException {
        if (written == null) {
            file.delete();
            return;
        }
        char[] buf = new char[written.length()];
        written.getChars(0, buf.length, buf, 0);
        saveFileFromChars(file, encoding, buf);
    }

    public static void saveFileFromChars(String fileName, String encoding, char[] buf) throws IOException {
        saveFileFromChars(new File(fileName), encoding, buf);
    }

    public static void saveFileFromChars(File file, String encoding, char[] buf) throws IOException {
        if (buf == null) {
            file.delete();
            return;
        }
        saveFileFromChars(file, encoding, buf, 0, buf.length);
    }

    public static void saveFileFromChars(File file, String encoding, char[] buf, int off, int len) throws IOException {
        if (buf == null) {
            file.delete();
            return;
        }
        OutputStreamWriter f = (encoding == null) ? new FileWriter(file) : new OutputStreamWriter(new FileOutputStream(file), encoding);
        try {
            f.write(buf, off, len);
        } catch (IOException e) {
            try {
                f.close();
            } catch (Exception e1) {
            }
            ;
            return;
        }
        f.close();
    }
}
