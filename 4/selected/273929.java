package org.fudaa.ctulu.gui;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.fudaa.ctulu.gui.CtuluFileChooser;
import org.fudaa.ctulu.pdf.CtuluFramePdfCustomViewer;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFViewer;
import com.sun.pdfview.PagePanel;

/**
 * classe de test poru voir le fonctionnement du renderer de pdf pour plus d exemples :
 * https://pdf-renderer.dev.java.net/examples.html
 * 
 * @author Adrien Hadoux
 */
public class ExampleCtuluPdf {

    public static void setup() throws IOException {
        javax.swing.JFrame frame = new JFrame("PDF Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PagePanel panel = new PagePanel();
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        CtuluFileChooser chooser = new CtuluFileChooser(true);
        int reponse = chooser.showOpenDialog(frame);
        if (reponse != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(0);
        panel.showPage(page);
    }

    public static void afficheViewerCompletement() {
        CtuluFramePdfCustomViewer viewer = new CtuluFramePdfCustomViewer("Test viewer", true, null);
        viewer.setVisible(true);
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                afficheViewerCompletement();
            }
        });
    }
}
