package mo1;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import viewer.com.sun.pdfview.PagePanel;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.*;
import viewer.com.sun.pdfview.PagePanel;

/**
 * An example of using the PagePanel class to show PDFs. For more advanced
 * usage including navigation and zooming, look ad the 
 * com.sun.pdfview.PDFViewer class.
 *
 * @author joshua.marinacci@sun.com
 */
public class JMain {

    public static void setup() throws IOException {
        JFrame frame = new JFrame("PDF Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PagePanel panel = new PagePanel();
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        File file = new File("output.pdf");
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(2);
        panel.showPage(page);
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    JMain.setup();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
