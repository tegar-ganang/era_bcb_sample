package utilidad.pdf;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;
import com.sun.pdfview.*;

public class Renderer {

    private Image image;

    private String fileName;

    private PDFFile pdfFile;

    private double height;

    private double width;

    private int numpages;

    private Image[] images;

    public Renderer(String file, int pagenum) {
        fileName = file;
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(fileName), "r");
            FileChannel fc = raf.getChannel();
            ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            pdfFile = new PDFFile(buf);
        } catch (IOException e) {
            System.out.println("the file wasn't found");
        }
        numpages = pdfFile.getNumPages();
        System.out.println("Number of pages = " + numpages);
        images = new Image[numpages];
        int counter = 1;
        for (int i = 0; i < images.length; i++) {
            PDFPage page = pdfFile.getPage(counter);
            Rectangle2D r2d = page.getBBox();
            width = r2d.getWidth();
            height = r2d.getHeight();
            width /= 72.0;
            height /= 72.0;
            int res = Toolkit.getDefaultToolkit().getScreenResolution();
            width *= res;
            height *= res;
            image = page.getImage((int) width, (int) height, r2d, null, true, true);
            images[i] = image;
            counter++;
        }
    }

    public Image[] getImagePDFArray() {
        return this.images;
    }

    public Image getPDFImage() {
        return this.image;
    }

    public double getWidth() {
        return this.width;
    }

    public int getNumOfPages() {
        return this.numpages;
    }

    public int getHeight() {
        return (int) this.height;
    }
}
