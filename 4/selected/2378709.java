package com.webmotix.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.derby.iapi.services.io.ArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mullassery.imaging.Imaging;
import com.mullassery.imaging.ImagingFactory;
import com.mullassery.imaging.util.Util;
import com.webmotix.exception.ImageIOException;

public class MotixFileItem {

    /**
	 * Logger.
	 */
    private static final Logger log = LoggerFactory.getLogger(MotixFileItem.class);

    /**
	 * Extens�es de nomes de arquivos considerados como imagem permitidas no
	 * sistema.
	 */
    public static String[] EXTENSION_IMAGES = { "jpg", "jpeg", "png", "gif", "bmp", "JPG", "JPEG", "PNG", "GIF", "BMP" };

    private String name = null;

    private String extension = null;

    private float quality = -1f;

    private String contentType = null;

    /**
	 * Default max file upload size (200 MB).
	 */
    private int index = -1;

    private BufferedImage bufferedImage = null;

    private BufferedImage[] arrayBufferedImage = null;

    private BufferedImage bufferedImageThumb = null;

    private InputStream inputStream = null;

    private boolean resized = false;

    private boolean cropped = false;

    private boolean isImage = false;

    final Imaging imaging = ImagingFactory.createImagingInstance();

    public MotixFileItem(final FileItem fileItem, final int index) throws IOException {
        this(fileItem, fileItem.getName(), fileItem.getContentType(), index);
    }

    public MotixFileItem(final FileItem fileItem, final String name, final String contentType, final int index) throws IOException {
        this.name = name;
        this.contentType = contentType;
        this.index = index;
        this.extension = FilenameUtils.getExtension(this.name);
        this.inputStream = fileItem.getInputStream();
        this.isImage = ImageUtils.isImage(name);
        Object input = fileItem.getInputStream();
        ImageInputStream stream = ImageIO.createImageInputStream(input);
        if (this.extension.equals("gif")) {
            Iterator readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new RuntimeException("no image reader found");
            ImageReader reader = (ImageReader) readers.next();
            reader.setInput(stream);
            int n = reader.getNumImages(true);
            this.arrayBufferedImage = new BufferedImage[n];
            for (int i = 0; i < n; i++) {
                this.arrayBufferedImage[i] = reader.read(i);
            }
        } else {
            if (this.isImage) {
                this.bufferedImage = imaging.read(fileItem.getInputStream());
            }
        }
    }

    public MotixFileItem(final InputStream is, final String name, final String contentType, final int index) throws IOException {
        this.name = name;
        this.contentType = contentType;
        this.index = index;
        this.extension = FilenameUtils.getExtension(this.name);
        this.isImage = ImageUtils.isImage(name);
        ArrayInputStream isAux = null;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, out);
            isAux = new ArrayInputStream(out.toByteArray());
            if (this.isImage) {
                this.bufferedImage = imaging.read(isAux);
            }
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(isAux);
        }
        this.inputStream = new ArrayInputStream(out.toByteArray());
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(final BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public InputStream getInputStream() {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (this.bufferedImage != null) {
                if (this.isImage && ((this.quality != -1 && this.quality != 1.0) || this.cropped || this.resized)) {
                    imaging.write(this.bufferedImage, out, this.extension, this.quality);
                    return new ArrayInputStream(out.toByteArray());
                }
            } else if (this.extension.equals("gif")) {
                ImageWriter iw = ImageIO.getImageWritersByFormatName(this.extension).next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                iw.setOutput(ios);
                iw.prepareWriteSequence(null);
                for (int i = 0; i < this.arrayBufferedImage.length; i++) {
                    this.bufferedImage = this.arrayBufferedImage[i];
                    iw.writeToSequence(new IIOImage(this.bufferedImage, null, null), iw.getDefaultWriteParam());
                }
                iw.endWriteSequence();
                ios.close();
                return this.inputStream;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.inputStream;
    }

    public int getHeight() {
        if (bufferedImage != null) {
            return bufferedImage.getHeight();
        } else {
            return -1;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getWidth() {
        if (bufferedImage != null) {
            return bufferedImage.getWidth();
        } else {
            return -1;
        }
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(final boolean isImage) {
        this.isImage = isImage;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    /**
	 * Corta a imagem no centro, caso seja uma imagem.
	 * @param width
	 * @param height
	 * @throws ImageIOException
	 */
    public void crop(int width, int height) throws ImageIOException {
        try {
            if (this.isImage) {
                final int imageHeight = this.bufferedImage.getHeight();
                final int imageWidth = this.bufferedImage.getWidth();
                if ((imageWidth > width || imageHeight > height)) {
                    if (width == -1) {
                        width = height;
                    }
                    if (height == -1) {
                        height = width;
                    }
                    int x = (imageWidth - width) / 2;
                    int y = (imageHeight - height) / 2;
                    if (x < 0 || (x + width) > imageWidth) {
                        x = 0;
                    }
                    if (y < 0 || (y + height) > imageHeight) {
                        y = 0;
                    }
                    if (width > imageWidth) {
                        width = imageWidth;
                    }
                    if (height > imageHeight) {
                        height = imageHeight;
                    }
                    this.bufferedImage = imaging.crop(this.bufferedImage, x, y, width, height);
                }
            }
        } catch (final Exception e) {
            throw new ImageIOException("Falha ao cortar a imagem: " + e.getMessage());
        } finally {
            this.cropped = true;
        }
    }

    /**
	 * Redimensiona o arquivo, caso seja uma imagem.
	 * @param width
	 * @param height
	 * @param proportion
	 * @throws ImageIOException
	 */
    public void resize(int width, int height, final boolean proportion) throws ImageIOException {
        try {
            if (this.isImage) {
                if (proportion) {
                    height = height > 0 ? height : (width * this.getHeight() / this.getWidth());
                    width = width > 0 ? width : (height * this.getWidth() / this.getHeight());
                } else {
                    if (width == -1) {
                        if (height > this.getHeight()) {
                            width = this.getHeight();
                        } else {
                            width = height;
                        }
                    }
                    if (height == -1) {
                        if (width > this.getWidth()) {
                            height = this.getWidth();
                        } else {
                            height = width;
                        }
                    }
                }
                final Image scaled = this.bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                this.bufferedImage = Util.createBufferedImage(scaled, this.extension);
            }
        } catch (final Exception e) {
            throw new ImageIOException("Falha ao redimensionar a imagem: " + e.getMessage());
        } finally {
            this.resized = true;
        }
    }

    /**
	 * Define a qualidade que a imagem deve ser tratada
	 * @param quality valor de 0 a 100, representando a porcentagem.
	 * @throws ImageIOException
	 */
    public void setQuality(final int quality) {
        this.quality = (quality / 100f);
    }

    /**
	 * Cria um icone para o arquivo, caso seja uma imagem.
	 * @param width
	 * @param height
	 * @return
	 * @throws ImageIOException
	 */
    public InputStream createThumb(final int width, final int height) throws ImageIOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (this.getWidth() > ImageUtils.THUMB_WIDTH || this.getHeight() > ImageUtils.THUMB_HEIGHT) {
                this.bufferedImageThumb = imaging.resize(this.bufferedImage, ImageUtils.THUMB_WIDTH, ImageUtils.THUMB_HEIGHT, true);
            } else {
                this.bufferedImageThumb = this.bufferedImage;
            }
            imaging.write(this.bufferedImageThumb, out, this.extension, ImageUtils.COMPRESSION_QUALITY_VERY_HIGH);
            return new ArrayInputStream(out.toByteArray());
        } catch (final Exception e) {
            log.error("Falha ao criar thumb de arquivo: " + e.getMessage(), e);
            return null;
        }
    }

    public BufferedImage getBufferedImageThumb() {
        return bufferedImageThumb;
    }
}
