package mo1;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import viewer.com.sun.pdfview.PagePanel;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.JTree;
import viewer.com.sun.pdfview.PagePanel;

public class AppWin3 extends JFrame {

    static PagePanel panel = new PagePanel();

    public JTree sp0;

    public AppWin3() {
        sp0 = new JTree();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        SpringLayout layout = new SpringLayout();
        setLayout(layout);
        setPreferredSize(new Dimension(1500, 800));
        sp0.setPreferredSize(new Dimension(250, 250));
        add(sp0);
        add(panel);
        layout.putConstraint(SpringLayout.NORTH, panel, 10, SpringLayout.SOUTH, sp0);
        pack();
        setVisible(true);
    }

    public static void setup() throws IOException {
        File file = new File("output.pdf");
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(0);
        panel.showPage(page);
    }

    public static void main(final String[] args) {
        new AppWin3();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    setup();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
