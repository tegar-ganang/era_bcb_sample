package javacream.resource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

/**
 * ImageResourceHandler
 * 
 * @author Glenn Powell
 *
 */
public class ImageResourceHandler implements ResourceHandler<BufferedImage> {

    public BufferedImage read(InputStream input) throws ResourceException {
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    public void write(BufferedImage resource, OutputStream output) throws ResourceException {
        try {
            String[] formatNames = ImageIO.getWriterFormatNames();
            for (Iterator<ImageReader> readers = ImageIO.getImageReaders(resource); readers.hasNext(); ) {
                ImageWriter writer = ImageIO.getImageWriter(readers.next());
                if (writer != null) {
                    for (String formatName : formatNames) {
                        for (Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName); writers.hasNext(); ) {
                            if (writer.equals(writers.next())) {
                                ImageIO.write(resource, formatName, output);
                                return;
                            }
                        }
                    }
                }
            }
            throw new ResourceException("Unable to find suitable ImageWriter");
        } catch (ClassCastException e) {
            throw new ResourceException(e);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }
}
