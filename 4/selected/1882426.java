package org.remus.infomngmnt.pdf.renderer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.remus.infomngmnt.pdf.extension.IPdf2ImageRenderer;
import org.remus.infomngmnt.pdf.extension.ImageInformation;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

/**
 * @author Tom Seidel <tom.seidel@remus-software.org>
 */
public class PdfRenderer implements IPdf2ImageRenderer {

    public List<ImageInformation> convert(final IFolder outputFolder, final IFile pdfFile, final IProgressMonitor monitor) {
        File file = pdfFile.getLocation().toFile();
        List<ImageInformation> returnValue = new ArrayList<ImageInformation>();
        PDFFile pdffile;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            pdffile = new PDFFile(buf);
            for (int i = 1, n = pdffile.getNumPages(); i <= n; i++) {
                PDFPage page = pdffile.getPage(i);
                Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
                Image img = page.getImage(rect.width, rect.height, rect, null, true, true);
                BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics g = bi.createGraphics();
                g.drawImage(img, 0, 0, null);
                try {
                    String name = "__" + i + ".png";
                    ImageIO.write(bi, "png", outputFolder.getFile(name).getLocation().toFile());
                    ImageInformation item = new ImageInformation();
                    item.setFileName(name);
                    item.setHeight(rect.height);
                    item.setWidth(rect.width);
                    returnValue.add(item);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return returnValue;
    }

    public Dimension firstSlid(final IFile pdfFile) {
        File file = pdfFile.getLocation().toFile();
        List<ImageInformation> returnValue = new ArrayList<ImageInformation>();
        PDFFile pdffile;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            pdffile = new PDFFile(buf);
            if (pdffile.getNumPages() == 0) {
                return new Dimension(0, 0);
            }
            PDFPage page = pdffile.getPage(0);
            Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
            return new Dimension(rect.width, rect.height);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return new Dimension(0, 0);
    }
}
