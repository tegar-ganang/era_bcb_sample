package net.sf.jcgm.imageio.plugins.cgm;

import static net.sf.jcgm.core.MIMETypes.CGM_MIME_Types;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Provides information about the plugin.
 * @author xphc (Philippe Cadé)
 * @version $Id: CGMImageReaderSpi.java 31 2010-09-07 08:05:47Z phica $
 */
public class CGMImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "Swiss AviationSoftware Ltd.";

    static final String version = "1";

    static final String readerClassName = "net.sf.jcgm.imageio.plugins.cgm.CGMImageReader";

    static final String[] names = { "CGM" };

    static final String[] suffixes = { "cgm" };

    static final String[] writerSpiNames = null;

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = null;

    static final String nativeStreamMetadataFormatClassName = null;

    static final String[] extraStreamMetadataFormatNames = null;

    static final String[] extraStreamMetadataFormatClassNames = null;

    static final boolean supportsStandardImageMetadataFormat = false;

    static final String nativeImageMetadataFormatName = "net.sf.jcgm.imageio.plugins.cgm.CGMMetadata_1.0";

    static final String nativeImageMetadataFormatClassName = "net.sf.jcgm.imageio.plugins.cgm.CGMMetadata";

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    public CGMImageReaderSpi() {
        super(vendorName, version, names, suffixes, CGM_MIME_Types, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    @Override
    public boolean canDecodeInput(Object input) {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }
        ImageInputStream stream = (ImageInputStream) input;
        byte[] b = new byte[2];
        try {
            stream.mark();
            stream.readFully(b);
            stream.reset();
        } catch (IOException e) {
            return false;
        }
        return b[0] == 0x00 && (b[1] & 0xE0) == 0x20;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new CGMImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "CGM (Computer Graphics Metafile) Image Reader";
    }
}
