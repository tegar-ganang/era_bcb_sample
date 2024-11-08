package imagesJAI;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.ImageIcon;
import com.sun.media.jai.codec.SeekableStream;

public class ImprovedImageImpl implements ImprovedImage {

    private static final long serialVersionUID = -6271834616099069085L;

    private ImageIcon image;

    private transient BufferedImage bufferedImage;

    private transient boolean bufferHasChanged = true;

    public ImprovedImageImpl(String src) throws WrongImageFormatException {
        ImageFormat imageFormat = getExtension(src);
        if (imageFormat == null) throw new WrongImageFormatException();
        try {
            InputStream inputImage;
            inputImage = new FileInputStream(src);
            SeekableStream stream = SeekableStream.wrapInputStream(inputImage, false);
            ParameterBlock pb = new ParameterBlock();
            pb.add(stream);
            RenderedOp renderedOp = JAI.create(imageFormat.toString(), pb);
            bufferedImage = renderedOp.getAsBufferedImage();
            bufferHasChanged = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ImprovedImageImpl(BufferedImage image) {
        bufferedImage = image;
        bufferHasChanged = true;
    }

    public ImprovedImageImpl(ImageIcon image) {
        RenderedOp renderedOp = JAI.create("AWTImage", image.getImage());
        bufferedImage = renderedOp.getAsBufferedImage();
        bufferHasChanged = true;
    }

    public int readData(int x, int y) {
        return bufferedImage.getRGB(x, y);
    }

    public int readRed(int x, int y) {
        return (readData(x, y) & 0X00FF0000) >> 16;
    }

    public int readGreen(int x, int y) {
        return (readData(x, y) & 0X0000FF00) >> 8;
    }

    public int readBlue(int x, int y) {
        return (readData(x, y) & 0X000000FF);
    }

    public int readGray(int x, int y) {
        return (readRed(x, y) + readGreen(x, y) + readBlue(x, y)) / 3;
    }

    public void writeData(int x, int y, int newValue) {
        bufferedImage.setRGB(x, y, newValue);
        bufferHasChanged = true;
    }

    public void writeGrayData(int x, int y, int grayValue) {
        writeData(x, y, grayValue | (grayValue << 8) | (grayValue << 16));
    }

    public void updateDatas() {
        if (bufferHasChanged) {
            image = new ImageIcon(bufferedImage);
            bufferHasChanged = false;
        }
    }

    public void saveAs(String dest, ImageFormat imageFormat) {
        String extention = imageFormat.toString();
        updateDatas();
        JAI.create("filestore", bufferedImage, dest + "." + extention, extention);
    }

    public void rotate(float radAngle) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(bufferedImage);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(radAngle);
        pb.add(new InterpolationBilinear());
        RenderedOp renderedOp = JAI.create("Rotate", pb);
        pb.add(new double[] { 255., 255., 255. });
        renderedOp = JAI.create("Rotate", pb);
        bufferedImage = renderedOp.getAsBufferedImage();
        bufferHasChanged = true;
    }

    public void binarize(int threshold) {
        int nbthread = Runtime.getRuntime().availableProcessors();
        int[] xMin = new int[nbthread], xMax = new int[nbthread];
        int stepX = (int) Math.ceil((double) getWidth() / (double) nbthread);
        Thread[] threads = new Thread[nbthread];
        for (int i = 0; i < nbthread; i++) {
            xMin[i] = Math.min(stepX * i + 1, getWidth() - stepX);
            if (i == 0) xMin[0]--;
            xMax[i] = Math.min(stepX * (i + 1), getWidth());
            threads[i] = new Thread(new BinarizeThreadPart(xMin[i], xMax[i], threshold));
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) try {
            threads[i].join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class BinarizeThreadPart implements Runnable {

        private int xMin;

        private int xMax;

        private int threshold;

        private BinarizeThreadPart(int xMin, int xMax, int threshold) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.threshold = threshold;
        }

        public void run() {
            for (int y = 0; y < getHeight(); y++) for (int x = xMin; x < xMax; x++) writeGrayData(x, y, readGray(x, y) > threshold ? 255 : 0);
        }
    }

    public ImageIcon getScaledImage(Dimension dimension) {
        updateDatas();
        return new ImageIcon(image.getImage().getScaledInstance((int) dimension.getWidth(), (int) dimension.getHeight(), Image.SCALE_DEFAULT));
    }

    public Dimension getDimension() {
        updateDatas();
        return new Dimension(image.getIconWidth(), image.getIconHeight());
    }

    public int getHeight() {
        updateDatas();
        return image.getIconHeight();
    }

    public int getWidth() {
        updateDatas();
        return image.getIconWidth();
    }

    public Image getImageAWT() {
        updateDatas();
        return image.getImage();
    }

    public ImageIcon getImageIcon() {
        updateDatas();
        return image;
    }

    private static ImageFormat getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        ext = (ext.compareTo("pgm") == 0) ? "pnm" : ext;
        try {
            return ImageFormat.valueOf(ext);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public BufferedImage getImage() {
        return bufferedImage;
    }

    public void setImage(BufferedImage newImage) {
        bufferedImage = newImage;
        bufferHasChanged = true;
    }

    public void canvasResize(int newWidth, int newHeight) {
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        int x1 = Math.round((newWidth - getWidth()) / 2);
        int y1 = Math.round((newHeight - getHeight()) / 2);
        int x2 = x1 + getWidth();
        int y2 = y1 + getHeight();
        for (int y = 0; y < newHeight; y++) for (int x = 0; x < newWidth; x++) if (x < x1 || x >= x2 - 1 || y < y1 || y >= y2 - 1) resizedImage.setRGB(x, y, 255 | (255 << 8) | (255 << 16)); else resizedImage.setRGB(x, y, readData(x - x1, y - y1));
        bufferedImage = resizedImage;
        bufferHasChanged = true;
    }

    public void replaceImage(ImprovedImage image, Point position) throws WrongPositionException {
        mergedImage(image, position, 0, 1);
    }

    public void mergedImage(ImprovedImage image, Point position, int poderation, int poderationOtherImage) throws WrongPositionException {
        if (getWidth() <= (image.getWidth() + position.x) || getHeight() <= (image.getHeight() + position.y)) throw new WrongPositionException();
        for (int y = position.y, yy = 0; yy < image.getHeight(); y++, yy++) for (int x = position.x, xx = 0; xx < image.getWidth(); x++, xx++) {
            int value = (readGray(x, y) * poderation + image.readGray(xx, yy) * poderationOtherImage) / (poderation + poderationOtherImage);
            writeGrayData(x, y, value);
        }
        bufferHasChanged = true;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        updateDatas();
        image = (ImageIcon) in.readObject();
        RenderedOp renderedOp = JAI.create("AWTImage", image.getImage());
        bufferedImage = renderedOp.getAsBufferedImage();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        updateDatas();
        out.writeObject(image);
    }
}
