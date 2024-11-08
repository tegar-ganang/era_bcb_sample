package org.yjchun.hanghe.chart.bsb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;

/**
 * @author yjchun
 * 
 */
public class BSBImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "yjchun";

    static final String version = "1.0";

    static final String readerClassName = "org.yjchun.hanghe.frmt.bsb.BSBImageReader";

    static final String[] names = { "bsb" };

    static final String[] suffixes = { "kap", "bsb", "nos" };

    static final String[] MIMETypes = { "image/x-bsb" };

    static final String[] writerSpiNames = {};

    static final Class[] inputTypes = { File.class };

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = null;

    static final String nativeStreamMetadataFormatClassName = null;

    static final String[] extraStreamMetadataFormatNames = null;

    static final String[] extraStreamMetadataFormatClassNames = null;

    static final boolean supportsStandardImageMetadataFormat = false;

    static final String nativeImageMetadataFormatName = "com.mycompany.imageio.MyFormatMetadata_1.0";

    static final String nativeImageMetadataFormatClassName = "com.mycompany.imageio.MyFormatMetadata";

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    public BSBImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    @Override
    public String getDescription(Locale locale) {
        return "NOAA MAPTECH NDI SoftChart BSB Reader";
    }

    @Override
    public boolean canDecodeInput(Object input) {
        if (!(input instanceof File)) return false;
        File infile = (File) input;
        if (!infile.canRead()) return false;
        return true;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) {
        return new BSBImageReader(this);
    }
}
