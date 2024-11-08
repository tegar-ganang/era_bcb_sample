package part2.chapter06;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import part1.chapter03.MovieTemplates;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;

public class MemoryInfo {

    /** The resulting PDF file. */
    public static final String RESULT = "results/part2/chapter06/memory_info.txt";

    /**
     * Main method.
     * @param args no arguments needed
     * @throws DocumentException 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, DocumentException {
        MovieTemplates.main(args);
        PrintWriter writer = new PrintWriter(new FileOutputStream(RESULT));
        fullRead(writer, MovieTemplates.RESULT);
        partialRead(writer, MovieTemplates.RESULT);
        writer.close();
    }

    /**
     * Do a full read of a PDF file
     * @param writer a writer to a report file
     * @param filename the file to read
     * @throws IOException
     */
    public static void fullRead(PrintWriter writer, String filename) throws IOException {
        long before = getMemoryUse();
        PdfReader reader = new PdfReader(filename);
        reader.getNumberOfPages();
        writer.println(String.format("Memory used by full read: %d", getMemoryUse() - before));
        writer.flush();
    }

    /**
     * Do a partial read of a PDF file
     * @param writer a writer to a report file
     * @param filename the file to read
     * @throws IOException
     */
    public static void partialRead(PrintWriter writer, String filename) throws IOException {
        long before = getMemoryUse();
        PdfReader reader = new PdfReader(new RandomAccessFileOrArray(filename), null);
        reader.getNumberOfPages();
        writer.println(String.format("Memory used by partial read: %d", getMemoryUse() - before));
        writer.flush();
    }

    /**
     * Returns the current memory use.
     * 
     * @return the current memory use
     */
    public static long getMemoryUse() {
        garbageCollect();
        garbageCollect();
        garbageCollect();
        garbageCollect();
        long totalMemory = Runtime.getRuntime().totalMemory();
        garbageCollect();
        garbageCollect();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return (totalMemory - freeMemory);
    }

    /**
     * Makes sure all garbage is cleared from the memory.
     */
    public static void garbageCollect() {
        try {
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
