package org.nightlabs.io.pcx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * PCX reader Service provider interface.
 * @author Daniel.Mazurek <at> NightLabs <dot> de
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 */
public class PCXImageReaderSPI extends ImageReaderSpi {

    public PCXImageReaderSPI() {
        super();
        init();
    }

    @SuppressWarnings("unchecked")
    public PCXImageReaderSPI(String vendorName, String version, String[] names, String[] suffixes, String[] MIMETypes, String readerClassName, Class[] inputTypes, String[] writerSpiNames, boolean supportsStandardStreamMetadataFormat, String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName, String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames, boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, inputTypes, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    protected void init() {
        inputTypes = new Class[] { InputStream.class, ImageInputStream.class };
        suffixes = PCXReaderWriterConstants.suffixes;
        names = PCXReaderWriterConstants.names;
        MIMETypes = PCXReaderWriterConstants.mimeTypes;
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        if (source != null) {
            if (source instanceof InputStream) {
                return checkHeader((InputStream) source);
            } else if (source instanceof ImageInputStream) {
                ImageInputStream imageStream = (ImageInputStream) source;
                org.nightlabs.io.pcx.ImageInputStream iis = new org.nightlabs.io.pcx.ImageInputStream(imageStream);
                return checkHeader(iis);
            }
        }
        return false;
    }

    protected boolean checkHeader(InputStream in) {
        return PCXImageReader.getHeader(in) != null;
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new PCXImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PCX Format Reader";
    }
}
