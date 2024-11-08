package dk.i2m.converge.plugins.actions.pdfpreview;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.imageio.ImageIO;

/**
 * Extracts a preview image of a PDF document.
 *
 * @author Allan Lykke Christensen
 */
public class PdfPreviewExtractor {

    private String filename;

    private int page;

    private int width = 0;

    private int height = 0;

    private boolean extracted = false;

    private byte[] preview = null;

    public PdfPreviewExtractor(String filename) {
        this(filename, 1);
    }

    public PdfPreviewExtractor(String filename, int page) {
        this.filename = filename;
        this.page = page;
    }

    public byte[] extract() throws IOException {
        this.extracted = false;
        this.preview = null;
        File file = new File(this.filename);
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdffile = new PDFFile(buf);
        PDFPage page = pdffile.getPage(this.page - 1);
        Rectangle r = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        this.width = r.width;
        this.height = r.height;
        Image img = page.getImage(this.width, this.height, r, null, true, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOException throwEx = null;
        try {
            ImageIO.write((RenderedImage) img, "png", baos);
            baos.flush();
            this.preview = baos.toByteArray();
            this.extracted = true;
        } catch (IOException ex) {
            this.preview = null;
            this.extracted = false;
            throwEx = ex;
        } finally {
            baos.close();
            if (throwEx != null) {
                throw throwEx;
            }
        }
        return this.preview;
    }

    public boolean isExtracted() {
        return extracted;
    }

    public void setExtracted(boolean extracted) {
        this.extracted = extracted;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public byte[] getPreview() {
        return preview;
    }

    public void setPreview(byte[] preview) {
        this.preview = preview;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
