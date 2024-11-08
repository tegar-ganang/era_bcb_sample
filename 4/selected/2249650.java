package com.hrzafer.javanta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Dosya işlemleri sınıfı
 * Yazar: Harun Reşit Zafer - hrzafer@gmail.com hrzafer.com
 *
 */
public class IO {

    private static final int BUFFER = 8192;

    /** Unicode karakter kodu - "UTF-8"*/
    public static final String UTF_8 = "UTF-8";

    /** ISO Türkçe karakter kodu - "ISO-8859-9"*/
    public static final String ISO_TR = "ISO-8859-9";

    /**
     * Bir dosyanın belirtilen adreste olup olmadığını döndürür
     * Ör: IO.exists("dosyalarım/dosyam.txt");     
     */
    public static boolean exists(String file) {
        boolean exists = (new File(file)).exists();
        return exists;
    }

    /**
     * Bir dosyayı okuyup bütünüyle string olarak döndürür. 
     * Dosya Unicode biçiminde ise sorunsuz çalışır. Aksi halde Türkçe karakterlerde 
     * sorun yaşamamak için (Dosya Türkçe karakterler içeriyorsa ve Unicode biçiminde değilse):
     * read("beni_oku.txt", "ISO-8859-9") şeklinde kullanılmalıdır
     * <br><br>Not: Yine de Türkçe karakter içeren dosyalarınızı her zaman unicode biçiminde kaydetmeniz tavsiye edilir.
     */
    public static String read(String file) {
        return read(file, UTF_8);
    }

    /**
     * Bir dosyayı okuyup bütünüyle string olarak döndürür.
     * Türkçe karakter içeren ANSI biçiminde dosyalar için: read("beni_oku.txt", "ISO-8859-9") şeklinde çağırılması tavsiye edilir
     * Kaynak: http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file/326440#326440
     */
    public static String read(String file, String encoding) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            Reader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName(encoding)));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[BUFFER];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(stream);
        }
    }

    /**
     * Kaynak dosyayı (from) hedef dosyaya (to) kopyalar. Klasörler için kullanılmaz!
     * Ör: copy("beni_kopyala.txt", "bana_kopyala.txt");
     */
    public static void copy(String from, String to) {
        File source = new File(from);
        File target = new File(to);
        copy(source, target);
    }

    /**
     *Kaynak(source) dosyayı hedef(target) dosyaya kopyalar. Klasörler için kullanılmaz!
     * Ör: copy(new File("beni_kopyala.txt"), new File("bana_kopyala.txt"));
     */
    public static void copy(File source, File target) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(target).getChannel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
            while (in.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    out.write(buffer);
                }
                buffer.clear();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(in);
            close(out);
        }
    }

    /**
     * Bir String'i bütünüyle dosyaya yazar.
     * Ör: write("bana_yaz.txt", "beni dosyaya yaz");
     */
    public static void write(String filePath, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(writer);
        }
    }

    /**
     * Bir dosyadaki kelimeleri liste (ArrayList) olarak döndürür.
     */
    public static List<String> readWords(String filePath) {
        return readWords(filePath, UTF_8);
    }

    /**
     * Bir dosyadaki kelimeleri liste (ArrayList) olarak döndürür.
     */
    public static List<String> readWords(String filePath, String encoding) {
        Scanner scanner = null;
        List<String> words = new ArrayList<String>();
        scanner = new Scanner(read(filePath, encoding));
        while (scanner.hasNext()) {
            words.add(scanner.next());
        }
        scanner.close();
        return words;
    }

    /**
     * Bir dosyadaki satırları liste (ArrayList) olarak döndürür.
     */
    public static List<String> readLines(String filePath) {
        return readLines(filePath, UTF_8);
    }

    /**
     * Bir dosyadaki satırları liste (ArrayList) olarak döndürür.
     */
    public static List<String> readLines(String filePath, String encoding) {
        Scanner scanner = null;
        List<String> lines = new ArrayList<String>();
        scanner = new Scanner(read(filePath, encoding));
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        scanner.close();
        return lines;
    }

    /**
     * Bir listedeki (ArrayList) satırları dosyaya yazar.
     */
    public static void writeLines(String filePath, ArrayList<String> lines) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath));
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(writer);
        }
    }

    /**
     * String'i dosya gibi okuyabilmek için kullanılabilecek bir metod.
     */
    public static InputStream toInputStream(String source) {
        InputStream stream;
        try {
            stream = new ByteArrayInputStream(source.getBytes(UTF_8));
            return stream;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Dosyayı exception handling ile uğraşmadan kapatmak için.
     */
    private static void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /***
     * Projeye resource olarak eklenmiş bir metin dosyasını okuyup string olarak döndürür.
     */
    public static String readResource(String resourcePath) {
        InputStream stream = null;
        try {
            stream = IO.class.getResourceAsStream(resourcePath);
            Reader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName(UTF_8)));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[BUFFER];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(stream);
        }
    }

    /**
     * Projedeki bir properties dosyasını okur ve Properties olarak döndürür<br>
     <br>Örnek: IO.readProperties("/com/hrzafer/javanta/resources/test.properties");
     */
    public static Properties readProperties(String filepath) {
        InputStream in = null;
        try {
            Properties properties = new Properties();
            in = IO.class.getResourceAsStream(filepath);
            properties.load(in);
            in.close();
            return properties;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Properties file (" + filepath + ") can not be read!!!");
        }
    }
}
