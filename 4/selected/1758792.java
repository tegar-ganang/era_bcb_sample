package org.nightlabs.io.pcx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * PCX writer Service provider interface.
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 * @author Daniel.Mazurek <at> NightLabs <dot> de
 */
public class PCXImageWriterSPI extends ImageWriterSpi {

    public PCXImageWriterSPI() {
        super();
        init();
    }

    @SuppressWarnings("unchecked")
    public PCXImageWriterSPI(String vendorName, String version, String[] names, String[] suffixes, String[] MIMETypes, String writerClassName, Class[] outputTypes, String[] readerSpiNames, boolean supportsStandardStreamMetadataFormat, String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName, String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames, boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, outputTypes, readerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return true;
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new PCXImageWriter(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PCX Format Writer";
    }

    protected void init() {
        outputTypes = new Class[] { OutputStream.class, ImageOutputStream.class };
        suffixes = PCXReaderWriterConstants.suffixes;
        names = PCXReaderWriterConstants.names;
        MIMETypes = PCXReaderWriterConstants.mimeTypes;
    }
}
