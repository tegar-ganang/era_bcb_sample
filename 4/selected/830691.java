package gnu.javax.imageio.bmp;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class BMPImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "GNU";

    static final String version = "0.1";

    static final String readerClassName = "gnu.javax.imageio.bmp.BMPImageReader";

    static final String[] names = { "Microsoft Windows BMP" };

    static final String[] suffixes = { ".bmp", ".bm" };

    static final String[] MIMETypes = { "image/bmp", "image/x-windows-bmp" };

    static final String[] writerSpiNames = null;

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = null;

    static final String nativeStreamMetadataFormatClassName = null;

    static final String[] extraStreamMetadataFormatNames = null;

    static final String[] extraStreamMetadataFormatClassNames = null;

    static final boolean supportsStandardImageMetadataFormat = false;

    static final String nativeImageMetadataFormatName = null;

    static final String nativeImageMetadataFormatClassName = null;

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    public BMPImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public String getDescription(Locale locale) {
        return "Microsoft BMP v3";
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) return false;
        ImageInputStream in = (ImageInputStream) input;
        boolean retval;
        in.mark();
        try {
            new BMPFileHeader(in);
            retval = true;
        } catch (BMPException e) {
            retval = false;
        }
        in.reset();
        return retval;
    }

    public ImageReader createReaderInstance(Object extension) {
        return new BMPImageReader(this);
    }
}
