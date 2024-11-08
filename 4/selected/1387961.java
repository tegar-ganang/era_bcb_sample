package gnu.javax.imageio.gif;

import gnu.javax.imageio.IIOInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class GIFImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "GNU";

    static final String version = "0.1";

    static final String readerClassName = "gnu.javax.imageio.gif.GIFImageReader";

    static final String[] names = { "Compuserve GIF" };

    static final String[] suffixes = { ".gif" };

    static final String[] MIMETypes = { "image/gif", "image/x-gif" };

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

    public GIFImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, new Class[] { ImageInputStream.class, InputStream.class }, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public String getDescription(Locale locale) {
        return "Compuserve GIF";
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (input == null) throw new IllegalArgumentException("Input object cannot be null.");
        if (!(input instanceof ImageInputStream) && !(input instanceof InputStream)) return false;
        boolean retval;
        InputStream in;
        if (input instanceof ImageInputStream) in = new IIOInputStream((ImageInputStream) input); else in = (InputStream) input;
        in.mark(10);
        retval = GIFFile.readSignature(in);
        in.reset();
        return retval;
    }

    public ImageReader createReaderInstance(Object extension) {
        return new GIFImageReader(this);
    }
}
