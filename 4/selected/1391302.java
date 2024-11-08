package com.seph.tga;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public class TGAImageWriterSPI extends ImageWriterSpi {

    static final String vendorName = "Seph";

    static final String version = "1.0";

    static final String writerClassName = "com.seph.tga.TGAImageWriter";

    static final String[] names = { "tga", "targa" };

    static final String[] suffixes = { "tga" };

    static final String[] MIMETypes = { "image/targa" };

    static final String[] readerSpiNames = {};

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = null;

    static final String nativeStreamMetadataFormatClassName = null;

    static final String[] extraStreamMetadataFormatNames = null;

    static final String[] extraStreamMetadataFormatClassNames = null;

    static final boolean supportsStandardImageMetadataFormat = true;

    static final String nativeImageMetadataFormatName = null;

    static final String nativeImageMetadataFormatClassName = null;

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    public TGAImageWriterSPI() {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, STANDARD_OUTPUT_TYPE, readerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public boolean canEncodeImage(ImageTypeSpecifier imageType) {
        int bands = imageType.getNumBands();
        return bands == 4;
    }

    public String getDescription(Locale locale) {
        return "Seph TGA Writer";
    }

    public ImageWriter createWriterInstance(Object extension) {
        return new TGAImageWriter(this);
    }
}
