import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PagePanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author Peter Wu <peterwu@hotmail.com>
 */
public class Main2 {

    public void setup() throws IOException {
        JFrame frame = new JFrame("PDF Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        PagePanel pagePanel = new PagePanel() {

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                for (Component c : getComponents()) {
                    c.repaint();
                }
            }
        };
        pagePanel.setLayout(null);
        createTextField(pagePanel);
        frame.add(pagePanel);
        int dpi = frame.getToolkit().getScreenResolution();
        System.out.println(dpi);
        frame.setSize(765, 990);
        frame.setVisible(true);
        File file = new File("test.pdf");
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(1, true);
        pagePanel.showPage(page);
    }

    private void createTextField(JPanel panel) {
        JTextField textField = new JTextField("Hello, World!");
        textField.setBounds(52, 298, 170, 12);
        textField.setBackground(Color.YELLOW);
        textField.setOpaque(true);
        textField.setBorder(null);
        panel.add(textField);
    }

    private void test() {
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    Main2 m2 = new Main2();
                    m2.setup();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
