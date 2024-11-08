package jmemento.service.photo.impl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import jmemento.api.domain.photo.IPhoto;
import jmemento.api.service.photo.IResizer;
import jmemento.api.service.storage.IStorageService;
import jmemento.domain.photo.impl.ImageSize;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rusty Wright
 * 
 */
public final class Resizer implements IResizer {

    private final transient Logger log = LoggerFactory.getLogger(getClass());

    private final PhotoSizeValues photoSizeValues;

    private final IStorageService storageService;

    /**
     * @param _photoSizeValues
     * @param _storageService
     */
    public Resizer(final PhotoSizeValues _photoSizeValues, final IStorageService _storageService) {
        if (_photoSizeValues == null) {
            throw (new IllegalArgumentException("photoSizeValues can't be null"));
        }
        photoSizeValues = _photoSizeValues;
        if (_storageService == null) {
            throw (new IllegalArgumentException("storageService can't be null"));
        }
        storageService = _storageService;
    }

    /**
     * @param photo
     */
    public void createImageFiles(final IPhoto photo) {
        final BufferedImage bufferedImage = readImageData(photo);
        final ImageSize origSize = new ImageSize(bufferedImage.getWidth(), bufferedImage.getHeight());
        photo.setImageSize(origSize);
        for (final Map.Entry<String, Integer> size : photoSizeValues.getBoundsMap().entrySet()) {
            if (size.getValue() == 0) {
                log.debug("skipping ORIGINAL");
                continue;
            }
            final BufferedImage scaledBI = scaleImage(bufferedImage, size.getValue());
            if (scaledBI == null) copyPhoto(photo, size); else {
                writeImageData(storageService.getPhotoPath(photo, size.getKey()), scaledBI);
            }
        }
    }

    private void copyPhoto(final IPhoto photo, final Map.Entry<String, Integer> size) {
        final File fileIn = new File(storageService.getPhotoPath(photo, storageService.getOriginalDir()));
        final File fileOut = new File(storageService.getPhotoPath(photo, size.getKey()));
        InputStream fileInputStream;
        OutputStream fileOutputStream;
        try {
            fileInputStream = new FileInputStream(fileIn);
            fileOutputStream = new FileOutputStream(fileOut);
            IOUtils.copy(fileInputStream, fileOutputStream);
            fileInputStream.close();
            fileOutputStream.close();
        } catch (final IOException e) {
            log.error("file io exception", e);
            return;
        }
    }

    private BufferedImage scaleImage(final BufferedImage bufferedImage, final int size) {
        final double boundSize = size;
        final int origWidth = bufferedImage.getWidth();
        final int origHeight = bufferedImage.getHeight();
        double scale;
        if (origHeight > origWidth) scale = boundSize / origHeight; else scale = boundSize / origWidth;
        log.debug("scale: {}", scale);
        if (scale > 1.0) return (null);
        final int scaledWidth = (int) (scale * origWidth);
        final int scaledHeight = (int) (scale * origHeight);
        final Image scaledImage = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        final BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = scaledBI.createGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();
        return (scaledBI);
    }

    private void writeImageData(final String photoPathName, final BufferedImage bufferedImage) {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw (new ResizeException("no image writers for jpeg format"));
        final ImageWriter writer = writers.next();
        final File fileOut = new File(photoPathName);
        ImageOutputStream ios;
        try {
            ios = ImageIO.createImageOutputStream(fileOut);
        } catch (final IOException e) {
            log.error("createImageOutputStream", e);
            throw (new ResizeException(e));
        }
        log.debug("ios: {}", ios);
        writer.setOutput(ios);
        try {
            writer.write(bufferedImage);
        } catch (final IOException e) {
            log.error("write", e);
            throw (new ResizeException(e));
        }
    }

    private BufferedImage readImageData(final IPhoto photo) {
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("jpeg");
        final ImageReader imageReader = imageReaders.next();
        final File fileIn = new File(storageService.getPhotoPath(photo, storageService.getOriginalDir()));
        if (!fileIn.isFile()) throw (new ResizeException("bad or missing photo file"));
        final ImageReadParam imageReadParam = new ImageReadParam();
        int subSampling = 1;
        BufferedImage bufferedImage = null;
        boolean outOfMemoryError;
        do {
            try {
                final ImageInputStream iis = ImageIO.createImageInputStream(fileIn);
                imageReader.reset();
                imageReader.setInput(iis, true);
                imageReadParam.setSourceSubsampling(subSampling, subSampling, 0, 0);
                bufferedImage = imageReader.read(0, imageReadParam);
                outOfMemoryError = false;
            } catch (final IOException e) {
                log.error("read", e);
                throw (new ResizeException(e));
            } catch (final OutOfMemoryError e) {
                log.error("read", e);
                subSampling++;
                outOfMemoryError = true;
            }
        } while (outOfMemoryError);
        return bufferedImage;
    }

    private BufferedImage readImageData2(final IPhoto photo) {
        final File fileIn = new File(storageService.getPhotoPath(photo, storageService.getOriginalDir()));
        if (!fileIn.isFile()) throw (new ResizeException("bad or missing photo file"));
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(fileIn);
        } catch (final IOException e) {
            log.error("createImageInputStream", e);
            throw (new ResizeException(e));
        }
        return (bufferedImage);
    }
}
