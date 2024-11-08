package jsslib.pdf;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PagePanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import jsslib.shell.Exec;

/**
 *
 * @author robert schuster
 */
public class PageDisplay extends PagePanel {

    PDFPage page;

    File pdffile;

    /**
     *
     * @param pdffile   The PDF-File to Display
     */
    public PageDisplay(File pdffile) {
        super();
        this.pdffile = pdffile;
        this.setLayout(new GridLayout(1, 1));
        this.setPreferredSize(new Dimension(-1, -1));
        this.setBackground(Color.white);
        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                resize();
            }
        });
        Update();
    }

    /**
     * if the size of the panel changes, then the clipping area needs
     * a new calculation
     */
    private void resize() {
        if (page != null && getWidth() > 0 && getHeight() > 0) {
            Rectangle2D pagesize = page.getBBox();
            Dimension actualsize = page.getUnstretchedSize(getWidth(), getHeight(), null);
            double factor = actualsize.width / pagesize.getWidth();
            Rectangle2D clip = new Rectangle2D.Double(factor * getBBoxFromGS().getX() - 1, factor * getBBoxFromGS().getY() - 1, factor * getBBoxFromGS().getWidth() + 10, factor * getBBoxFromGS().getHeight() + 10);
            showPage(null);
            showPage(page);
            waitForCurrentPage();
            setClip(clip);
        }
    }

    /**
     * Update the domain-Display
     */
    public void Update() {
        new Runnable() {

            public void run() {
                doUpdate();
            }
        }.run();
    }

    private void doUpdate() {
        if (pdffile == null) return;
        try {
            String gs_out = Exec.runToString("gs -sDEVICE=bbox -dNOPAUSE -dBATCH " + pdffile.getName(), pdffile.getParent());
            int bbox_pos = gs_out.indexOf("%%HiResBoundingBox:");
            String bbox = gs_out.substring(bbox_pos + 20);
            String[] bbox_split = bbox.trim().split("\\s+");
            double x = Double.parseDouble(bbox_split[0]);
            double y = Double.parseDouble(bbox_split[1]);
            double w = Double.parseDouble(bbox_split[2]) - x;
            double h = Double.parseDouble(bbox_split[3]) - y;
            setBBoxFromGS(new Rectangle2D.Double(x, y, w, h));
            RandomAccessFile raf = new RandomAccessFile(pdffile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile_ = new PDFFile(buf);
            page = pdffile_.getPage(0);
            resize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    Rectangle2D BBoxFromGS = null;

    private void setBBoxFromGS(Rectangle2D bbox) {
        BBoxFromGS = bbox;
    }

    private Rectangle2D getBBoxFromGS() {
        return BBoxFromGS;
    }

    public void setPDFfile(File file) {
        pdffile = file;
        Update();
    }
}
