package org.xebra.dcm.io;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.RegisterableService;
import javax.imageio.stream.ImageInputStream;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * Service provider interface for the {@link org.xebra.dcm.io.DCMImageReader} JAI IIO plugin.
 *
 * @author Kenny Pearce
 * @version $Revision: 1.2 $
 */
public class DCMImageReaderSpi extends ImageReaderSpi implements RegisterableService {

    private static final String vendorName = "Hx Technologies";

    private static final String version = "1.0";

    private static final String names[] = new String[] { "DICOM", "dicom" };

    private static final String suffixes[] = new String[] { "dcm", "dcmdir" };

    private static final String MIMETypes[] = new String[] { "application/dicom" };

    private static final String readerClassName = "org.xebra.dcm.io.DCMImageReader";

    private static final Class[] inputTypes = new Class[] { ImageInputStream.class, File.class, DicomObject.class };

    private static final String writerSpiNames[] = null;

    private static final boolean supportsStandardStreamMetadataFormat = false;

    private static final String nativeStreamMetadataFormatName = null;

    private static final String nativeStreamMetadataFormatClassName = null;

    private static final String extraStreamMetadataFormatNames[] = null;

    private static final String extraStreamMetadataFormatClassNames[] = null;

    private static final boolean supportsStandardImageMetadataFormat = false;

    private static final String nativeImageMetadataFormatName = null;

    private static final String nativeImageMetadataFormatClassName = null;

    private static final String extraImageMetadataFormatNames[] = null;

    private static final String extraImageMetadataFormatClassNames[] = null;

    public DCMImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, inputTypes, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public boolean canDecodeInput(Object source) throws IOException {
        if (source instanceof ImageInputStream) {
            return isDicom((ImageInputStream) source);
        } else if (source instanceof DicomObject) {
            DicomObject a = (DicomObject) source;
            return a.containsValue(Tag.PixelData);
        }
        return false;
    }

    private boolean isDicom(ImageInputStream iis) {
        iis.mark();
        byte[] b = new byte[132];
        try {
            iis.readFully(b);
        } catch (IOException exc) {
            return false;
        } finally {
            try {
                iis.reset();
            } catch (IOException exc) {
                return false;
            }
        }
        try {
            iis.readFully(b);
            iis.seek(128);
            if (b[128] == 'D' && b[129] == 'I' && b[130] == 'C' && b[131] == 'M') {
                return true;
            }
            int len = b[0];
            if (len != 0) {
                if (b[1] != 0) return false;
                int glen = ((b[6] & 0xff) | ((b[7] & 0xff) << 8)) + 8;
                if (len == b[glen]) return true;
                glen = ((b[4] & 0xff) | ((b[5] & 0xff) << 8)) + 8;
                return len == glen;
            } else {
                return b[1] != 0;
            }
        } catch (Throwable t) {
        }
        return false;
    }

    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new DCMImageReader(this);
    }

    public String getDescription(Locale locale) {
        return "Xebra DICOM IIO Plugin. Reads DICOM images into 16bit grayscale or " + "24bit RGB java BufferedImage objects.";
    }

    public ImageReader createReaderInstance() throws IOException {
        return new DCMImageReader(this);
    }

    public Class[] getInputTypes() {
        return inputTypes;
    }

    public boolean isOwnReader(ImageReader reader) {
        return reader instanceof DCMImageReader;
    }
}
