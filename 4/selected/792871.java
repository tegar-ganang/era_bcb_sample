package jmemento.impl.service.photo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
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
import jmemento.impl.domain.photo.ImageSize;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Rusty Wright
 * 
 */
@Service
public final class Resizer implements IResizer {

    private final transient Logger log = LoggerFactory.getLogger(getClass());

    private final IStorageService storageService;

    private final PhotoSizeValues photoSizeValues;

    private final Map<String, Integer> boundsMap;

    @Autowired
    public Resizer(final IStorageService _storageService, final PhotoSizeValues _photoSizeValues) {
        if (_storageService == null) throw (new IllegalArgumentException("storageService can't be null"));
        this.storageService = _storageService;
        if (_photoSizeValues == null) throw (new IllegalArgumentException("photoSizeValues can't be null"));
        this.photoSizeValues = _photoSizeValues;
        boundsMap = photoSizeValues.getBoundsMap();
    }

    /**
     * @param photo
     */
    @Override
    public void createImageFiles(final IPhoto photo) {
        log.debug("photo: {}", photo);
        final BufferedImage bufferedImage = readImageData(photo);
        final ImageSize origSize = new ImageSize(bufferedImage.getWidth(), bufferedImage.getHeight());
        photo.setImageSize(origSize);
        for (final Map.Entry<String, Integer> size : boundsMap.entrySet()) {
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
        if (!imageReaders.hasNext()) throw (new ResizeException("no image readers for jpeg format"));
        final ImageReader imageReader = imageReaders.next();
        final File fileIn = new File(storageService.getPhotoPath(photo, storageService.getOriginalDir()));
        if (!fileIn.isFile()) throw (new ResizeException("bad or missing photo file: " + fileIn));
        final ImageReadParam imageReadParam = new ImageReadParam();
        int subSampling = 1;
        BufferedImage bufferedImage = null;
        boolean outOfMemoryError;
        do {
            try {
                final ImageInputStream imageInput = ImageIO.createImageInputStream(fileIn);
                imageReader.reset();
                imageReader.setInput(imageInput, true);
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

    @Override
    public void initPhotoSizeStrings(final IPhoto photo) {
        final Map<String, ImageSize> photoSizes = new HashMap<String, ImageSize>();
        final int origWidth = photo.getImageSize().getWidth();
        final int origHeight = photo.getImageSize().getHeight();
        for (final Map.Entry<String, Integer> photoSize : boundsMap.entrySet()) {
            final float size = photoSize.getValue();
            if (size == 0) {
                final ImageSize imageSize = new ImageSize(origWidth, origHeight);
                photoSizes.put(photoSize.getKey(), imageSize);
                continue;
            }
            float scale;
            if (origHeight > origWidth) scale = size / origHeight; else scale = size / origWidth;
            ImageSize imageSize;
            if (scale > 1.0) {
                imageSize = new ImageSize(origWidth, origHeight);
            } else {
                final int scaledWidth = (int) (scale * origWidth);
                final int scaledHeight = (int) (scale * origHeight);
                imageSize = new ImageSize(scaledWidth, scaledHeight);
            }
            photoSizes.put(photoSize.getKey(), imageSize);
        }
        photo.setPhotoSizes(photoSizes);
    }
}
