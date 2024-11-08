package com.bluebrim.image.impl.shared;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * @author G�ran St�ck 2003-04-26
 *
 */
public class CoEpsImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "BlueBrim";

    static final String version = "1.0";

    static final String readerClassName = "com.bluebrim.image.impl.shared.CoEpsImageReader";

    static final String[] names = { "Encapsulated PostScript" };

    static final String[] suffixes = { "eps" };

    static final String[] MIMETypes = { "application/x-postscript" };

    static final String[] writerSpiNames = {};

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

    public CoEpsImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public String getDescription(Locale locale) {
        return "Description goes here";
    }

    public boolean canDecodeInput(Object input) {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }
        ImageInputStream stream = (ImageInputStream) input;
        byte[] b = new byte[8];
        try {
            stream.mark();
            stream.readFully(b);
            stream.reset();
        } catch (IOException e) {
            return false;
        }
        return (((b[0] & 0xff) == 0xc5) && ((b[1] & 0xff) == 0xd0) && ((b[2] & 0xff) == 0xd3) && ((b[3] & 0xff) == 0xc6)) || ((b[0] == '%') && (b[1] == '!'));
    }

    public ImageReader createReaderInstance(Object extension) {
        return new CoEpsImageReader(this);
    }
}
