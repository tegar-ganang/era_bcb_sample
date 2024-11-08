import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class Main5 extends JFrame {

    private final int DEFAULT_PDF_DPI = 72;

    private final double PIXEL_PER_POINT = 1.25;

    static Image image;

    public Main5(String title) {
        super(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel panel = new JPanel(null) {

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(image, 0, 0, null);
                for (Component c : getComponents()) {
                    c.repaint();
                }
            }
        };
        panel.add(this.createTextField());
        JCheckBox cb1 = createCheckBox(500.4, 607.897, 511.2, 617.977);
        JCheckBox cb2 = createCheckBox(558, 607.897, 568.8, 617.977);
        ButtonGroup group = new ButtonGroup();
        group.add(cb1);
        group.add(cb2);
        panel.add(cb1);
        panel.add(cb2);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        panel.setPreferredSize(new Dimension(w, h));
        JScrollPane scrollPane = new JScrollPane(panel);
        getContentPane().add(scrollPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        Insets insets = this.getInsets();
        System.out.println("left:" + insets.left);
        System.out.println("top:" + insets.bottom);
    }

    public static void main(final String[] args) throws Exception {
        int pagenum = 1;
        System.out.println(ClassLoader.getSystemResource("test.pdf").toURI().toString());
        File file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();
        ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        PDFFile pdfFile = new PDFFile(buf);
        int numpages = pdfFile.getNumPages();
        System.out.println("Number of pages = " + numpages);
        if (pagenum > numpages) {
            pagenum = numpages;
        }
        PDFPage page = pdfFile.getPage(pagenum);
        Rectangle2D r2d = page.getBBox();
        double width = r2d.getWidth();
        double height = r2d.getHeight();
        width /= 72.0;
        height /= 72.0;
        int res = Toolkit.getDefaultToolkit().getScreenResolution();
        width *= res;
        height *= res;
        image = page.getImage((int) width, (int) height, r2d, null, true, true);
        Runnable r = new Runnable() {

            @Override
            public void run() {
                new Main5("PDF Viewer: ");
            }
        };
        EventQueue.invokeLater(r);
    }

    private Point2D transformPoint(Point2D ptSrc) {
        Point2D ptDst = new Point2D.Double();
        AffineTransform at = new AffineTransform();
        at.setTransform(1.25, 0, 0, -1.25, 0, 990);
        at.transform(ptSrc, ptDst);
        return ptDst;
    }

    private Point2D transformPoint(double x1, double y1) {
        Point2D ptSrc = new Point2D.Double(x1, y1);
        return transformPoint(ptSrc);
    }

    private JCheckBox createCheckBox(double x1, double y1, double x2, double y2) {
        int res = Toolkit.getDefaultToolkit().getScreenResolution();
        Point2D pt;
        pt = transformPoint(x1, y1);
        x1 = pt.getX();
        y1 = pt.getY();
        pt = transformPoint(x2, y2);
        x2 = pt.getX();
        y2 = pt.getY();
        double w;
        double h;
        double stroke_width = 0.5;
        w = Math.abs((x2 - x1) * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT);
        h = Math.abs((y2 - y1) * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT);
        x1 = x1 * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT;
        y1 = y1 * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT;
        x1 = x1 + stroke_width * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT;
        y1 = y1 - h + 2 * stroke_width * res / DEFAULT_PDF_DPI / PIXEL_PER_POINT;
        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(true);
        checkBox.setSelected(true);
        checkBox.setBounds((int) x1, (int) y1, (int) w, (int) h);
        checkBox.setIcon(new ImageIcon());
        checkBox.setSelectedIcon(createSelectedIcon((int) w, (int) h));
        return checkBox;
    }

    private Icon createSelectedIcon(final int w, final int h) {
        return new Icon() {

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(new BasicStroke(3));
                int width = Math.min(w, h);
                int offset = 3;
                int x1 = offset;
                int y1 = offset;
                int x2 = width - offset;
                int y2 = width - offset;
                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x1, y2, x2, y1);
                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return w;
            }

            @Override
            public int getIconHeight() {
                return h;
            }
        };
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField("The quick brown fox jumps over the lazy dog.");
        int x;
        int y;
        int w;
        int h = 12;
        x = 48;
        y = 342 - h;
        w = 186;
        textField.setBounds(x, y, w, h);
        x = 48;
        y = 215 - h;
        w = 368;
        textField.setBounds(x, y, w, h);
        textField.setBackground(Color.YELLOW);
        textField.setOpaque(true);
        textField.setBorder(null);
        return textField;
    }
}
