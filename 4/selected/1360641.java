package net.sf.andpdf.awtpdfrenderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class AwtPdfRenderer {

    static final int maxX = 200;

    static final int maxY = 200;

    private static final String PDFFILENAME = "U:/Android/PDFs/Synopse_der_Antworten_auf_Wahlpruefsteine.pdf";

    private static final int PAGENUMBER = 1;

    public static void main(String[] args) {
        String pdfName = PDFFILENAME;
        if (args.length > 0) pdfName = args[0];
        AwtPdfRenderer pdfv = new AwtPdfRenderer();
        pdfv.showPDF(pdfName);
    }

    private void showPDF(String filename) {
        try {
            File f = new File(filename);
            long len = f.length();
            if (len == 0) {
                log("file '" + filename + "' not found");
            } else {
                log("file '" + filename + "' has " + len + " bytes");
                openFile(f);
                System.out.println("waiting");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("Exception: " + e.getMessage());
        }
    }

    private void log(String txt) {
        System.out.println(txt);
    }

    /**
	 * <p>
	 * Open a specific pdf file. Creates a DocumentInfo from the file, and opens
	 * that.
	 * </p>
	 * 
	 * <p>
	 * <b>Note:</b> Mapping the file locks the file until the PDFFile is closed.
	 * </p>
	 * 
	 * @param file
	 *            the file to open
	 * @throws IOException
	 * @throws InterruptedException 
	 */
    public void openFile(File file) throws IOException, InterruptedException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdfFile = new PDFFile(buf);
        int page = PAGENUMBER;
        int numPages = pdfFile.getNumPages();
        log("Seite: " + page + "/" + numPages);
        PDFPage pdfPage = pdfFile.getPage(page);
        log("\n" + pdfPage.getPageNumber() + ":" + pdfPage.getWidth() + "x" + pdfPage.getHeight());
        float zoom = 1.00f;
        final Image pdfPageImg = pdfPage.getImage((int) (zoom * pdfPage.getWidth()), (int) (zoom * pdfPage.getHeight()), null, null, true, true);
        BufferedImage pdfBI = (BufferedImage) pdfPageImg;
        JFrame frame = new JFrame("AWT PDF Renderer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MyJPanel panel = new MyJPanel();
        panel.setImage(pdfBI);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setSize(maxX, maxY);
        frame.setVisible(true);
    }

    static class MyJPanel extends JPanel implements ImageObserver {

        Image currImg;

        public MyJPanel() {
            super();
        }

        public void setImage(Image img) {
            currImg = img;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.black);
            g.drawString("huhu", getWidth() / 2 - 30, getHeight() / 2);
            if (currImg != null) {
                g.drawImage(currImg, 0, 0, null);
            }
        }

        /**
		 * Handles notification of the fact that some part of the image changed.
		 * Repaints that portion.
		 * 
		 * @return true if more updates are desired.
		 */
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            System.out.println("Image update: " + (infoflags & ALLBITS));
            if ((infoflags & (SOMEBITS | ALLBITS)) != 0) {
                repaint(x, y, width, height);
            }
            if ((infoflags & (ALLBITS | ERROR | ABORT)) != 0) {
                System.out.println("   flag set");
                return false;
            } else {
                return true;
            }
        }
    }
}
