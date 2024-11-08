package library.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public class TextFile extends ArrayList<String> {

    public static String read(String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            FileChannel fc = new RandomAccessFile(fileName, "r").getChannel();
            try {
                sb.append(Charset.forName(System.getProperty("file.encoding")).decode(fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())));
            } finally {
                fc.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public static void write(String fileName, String text) {
        try {
            FileChannel fc = new RandomAccessFile(new File(fileName).getAbsoluteFile(), "rw").getChannel();
            try {
                fc.map(FileChannel.MapMode.READ_WRITE, 0, text.length()).put(text.getBytes());
            } finally {
                fc.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TextFile(String fileName, String splitter) {
        super(Arrays.asList(read(fileName).split(splitter)));
        if (get(0).equals("")) remove(0);
    }

    public TextFile(String fileName) {
        this(fileName, "\n");
    }

    public void write(String fileName) {
        try {
            PrintWriter out = new PrintWriter(new File(fileName).getAbsoluteFile());
            try {
                for (String item : this) out.println(item);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
