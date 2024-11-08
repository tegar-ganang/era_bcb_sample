package gnu.javax.imageio.bmp;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public class BMPImageWriterSpi extends ImageWriterSpi {

    static final String vendorName = "GNU";

    static final String version = "0.1";

    static final String writerClassName = "gnu.javax.imageio.bmp.BMPImageWriter";

    static final String[] names = { "Microsoft Windows BMP" };

    static final String[] suffixes = { ".bmp", ".bm" };

    static final String[] MIMETypes = { "image/bmp", "image/x-windows-bmp" };

    static final String[] readerSpiNames = { "gnu.javax.imageio.bmp.BMPImageReaderSpi" };

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

    private BMPImageWriter writerInstance;

    public BMPImageWriterSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, STANDARD_OUTPUT_TYPE, readerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    /**
   * Returns true if the image can be encoded.
   * 
   * @param type - the image type specifier.
   * @return true if image can be encoded, otherwise false.
   */
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        if (type == null) return false;
        BMPInfoHeader ih = writerInstance.infoHeader;
        if (ih != null) {
            int compressionType = ih.getCompression();
            int bytes = type.getColorModel().getPixelSize();
            if ((compressionType == BMPInfoHeader.BI_RLE4 && (bytes != 4 || bytes != 8)) || (compressionType == BMPInfoHeader.BI_RGB && ((bytes != 1 || bytes != 4 || bytes != 8 || bytes != 16 || bytes != 24 || bytes != 32)))) return false;
        }
        return true;
    }

    /**
   * Creates an instance of ImageWriter using the given extension.
   * 
   * @param extension - the provider that is constructing this image writer, or
   *          null
   */
    public ImageWriter createWriterInstance(Object extension) {
        if (extension != null && extension instanceof ImageWriterSpi) writerInstance = new BMPImageWriter((ImageWriterSpi) extension); else writerInstance = new BMPImageWriter(this);
        return writerInstance;
    }

    /**
   * Gets the instance of ImageWriter, if already created.
   */
    public BMPImageWriter getWriterInstance() {
        if (writerInstance != null) return writerInstance;
        return (BMPImageWriter) createWriterInstance(null);
    }

    /**
   * Returns a short description of this service provider that can be
   * presented to a human user.
   *
   * @param locale - the locale for which the description string should
   * be localized.
   */
    public String getDescription(Locale locale) {
        return "Microsoft BMP v3";
    }
}
