package randres.kindle.reader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import randres.kindle.previewer.MobiDecoder;
import randres.kindle.previewer.huffmandec.DHDecompiler;

public class MyReader {

    private int section;

    private MobiDecoder bookDecoded;

    private StringBuffer pageContent;

    public MyReader(String filename) throws Exception {
        bookDecoded = new MobiDecoder(filename);
        section = 1;
    }

    public String getNext() {
        String next = bookDecoded.getSection(section);
        section++;
        return next;
    }

    public boolean hasNext() {
        return section < (bookDecoded.getMaxTextSection() - 2);
    }

    public String getEncoding() {
        return bookDecoded.getEncoding();
    }

    public static void main(String[] args) {
        String filename = "/home/randres/Escritorio/kindle/books/Rice, Anne - La voz del diablo.prc";
        String html = "/home/randres/Escritorio/fich.html";
        try {
            MyReader reader = new MyReader(filename);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(html), reader.getEncoding()));
            while (reader.hasNext()) {
                String next = reader.getNext();
                writer.write(next);
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCurrentSection() {
        return section;
    }
}
