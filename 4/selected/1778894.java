package mo1;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import viewer.com.sun.pdfview.PagePanel;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.*;

public class AppWin2 extends JFrame {

    public JTree t0;

    public JScrollPane sp0;

    public JButton bt0;

    public PagePanel pn0;

    AppWin2() throws IOException {
        t0 = new JTree();
        sp0 = new JScrollPane();
        bt0 = new JButton();
        pn0 = new PagePanel();
        sp0.setViewportView(t0);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        SpringLayout layout = new SpringLayout();
        contentPane.setLayout(layout);
        contentPane.add(sp0);
        contentPane.add(bt0);
        contentPane.add(pn0);
        setPreferredSize(new Dimension(1500, 800));
        bt0.setPreferredSize(new Dimension(150, 150));
        sp0.setPreferredSize(new Dimension(250, 250));
        layout.putConstraint(SpringLayout.NORTH, bt0, 10, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, bt0, 10, SpringLayout.EAST, sp0);
        layout.putConstraint(SpringLayout.NORTH, pn0, 15, SpringLayout.SOUTH, sp0);
        layout.putConstraint(SpringLayout.WEST, pn0, 10, SpringLayout.EAST, sp0);
        File file = new File("output.pdf");
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(0);
        pn0.showPage(page);
        pack();
        setVisible(true);
    }

    public void openError(String message) {
        JOptionPane.showMessageDialog(new javax.swing.JSplitPane(), message, "Error opening file", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    new AppWin2();
                } catch (IOException ioexc) {
                    ioexc.printStackTrace();
                }
            }
        });
    }
}
