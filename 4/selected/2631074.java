package com.flipdf.viewer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class PDFViewer {

    public PDFViewer() {
    }

    public PDFPage openFile(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = null;
        try {
            pdffile = new PDFFile(buf);
            PDFPage page = pdffile.getPage(0);
            System.err.println(page.getBBox());
            return page;
        } catch (IOException ioe) {
            return null;
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ScrollDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PDFViewer viewer = new PDFViewer();
        PDFPage page = null;
        File f = null;
        try {
            f = new File("c:/test.pdf");
            page = viewer.openFile(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PagePanel canvas = new PagePanel(f);
        canvas.showPage(page);
        JComponent newContentPane = new PageScrollPanel(canvas, frame);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                createAndShowGUI();
            }
        });
    }
}
