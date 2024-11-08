package edu.isi.misd.image.gateway.conversion.loci;

import java.io.File;
import loci.common.services.ServiceFactory;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import org.apache.log4j.Logger;
import edu.isi.misd.image.gateway.conversion.ConvertImage;
import edu.isi.misd.image.gateway.conversion.ImageConversion;

/**
 * Class that is responsible for converting between two LOCI Bio-formats
 * supported types.
 * 
 * @author David Smith
 * 
 */
public class LociConversionImplementation implements ImageConversion {

    private static final Logger LOG = Logger.getLogger(ConvertImage.class);

    private final String sourceFile;

    private final String destinationFile;

    private long maximumImageSize = 0;

    private final long minimumImageSize = 0;

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: " + ConvertImage.class.getName() + " <source file> <destination file>");
        }
        final LociConversionImplementation convert = new LociConversionImplementation(args[0], args[1]);
        try {
            convert.run();
        } catch (final Exception e) {
            LOG.error("Error converting the image.", e);
        }
    }

    /**
     * Constructor
     * 
     * @param source
     *            the source image filename
     * @param destination
     *            the destination filename
     */
    public LociConversionImplementation(final String source, final String destination) {
        if (source == null || source.length() == 0 || destination == null || destination.length() == 0) {
            throw new IllegalArgumentException("Source and destination must be specified.");
        }
        final File f = new File(source);
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Source " + source + " doesn't exist or is not readable.");
        }
        sourceFile = source;
        destinationFile = destination;
    }

    @Override
    public void run() throws Exception {
        final BufferedImageReader reader = new BufferedImageReader();
        final ServiceFactory factory = new ServiceFactory();
        final OMEXMLService service = factory.getInstance(OMEXMLService.class);
        final IMetadata omexml = service.createOMEXMLMetadata();
        reader.setMetadataStore(omexml);
        reader.setId(sourceFile);
        int series = -1;
        long currentPixelSize = 0;
        for (int i = 0; i < reader.getSeriesCount(); i++) {
            reader.setSeries(i);
            final long imagePixelSize = reader.getSizeX() * reader.getSizeY() * reader.getSizeC();
            if (maximumImageSize == 0 || (imagePixelSize < maximumImageSize && imagePixelSize > minimumImageSize)) {
                if (imagePixelSize > currentPixelSize) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Image pixel size " + imagePixelSize + " in series " + i + " fits within bounds and is the largest series so far.");
                    }
                    series = i;
                    currentPixelSize = imagePixelSize;
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Image pixel size " + imagePixelSize + " in series " + i + " fits within bounds but is not the largest series.");
                }
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Image pixel size " + imagePixelSize + " in series " + i + " doesn't fit within bounds.");
            }
        }
        if (series < 0) {
            throw new Exception("Cound not find an image series that fits within bounds.");
        }
        reader.setSeries(series);
        final LociImageSource source = new LociImageSource(reader, omexml, 0);
        final LociImageDestination destination = new LociImageDestination(destinationFile, (int) source.getWidth(), (int) source.getHeight(), omexml);
        long remainingWidth = source.getWidth();
        long remainingHeight = source.getHeight();
        long readWidth, readHeight;
        long x = 0, y = 0;
        final long bufferWidth = source.getWidth();
        final long bufferHeight = source.getHeight();
        boolean finished = false;
        while (!finished) {
            if (remainingWidth > bufferWidth) {
                readWidth = bufferWidth;
            } else {
                readWidth = remainingWidth;
            }
            if (remainingHeight > bufferHeight) {
                readHeight = bufferHeight;
            } else {
                readHeight = remainingHeight;
            }
            final byte[] bytes = source.readBytes(x, y, readWidth, readHeight);
            destination.writeBytes(bytes, x, y, readWidth, readHeight);
            x += readWidth;
            remainingWidth -= readWidth;
            if (remainingWidth == 0) {
                y += readHeight;
                remainingHeight -= readHeight;
                if (remainingHeight == 0) {
                    finished = true;
                } else {
                    x = 0;
                    remainingWidth = source.getWidth();
                }
            }
        }
        destination.close();
        source.close();
    }

    @Override
    public String getSourceFilename() {
        return sourceFile;
    }

    @Override
    public String getDestinationFilename() {
        return destinationFile;
    }

    @Override
    public void setMaximumImageSize(final long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Maximum image size must be >= 0");
        }
        maximumImageSize = size;
    }

    @Override
    public long getMaximumImageSize() {
        return maximumImageSize;
    }
}
