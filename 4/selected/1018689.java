package org.dcm4cheri.imageio.plugins;

import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.spi.*;

/**
 *
 * @author   Thomas Hacklaender
 * @version  2002.6.16
 */
public class DcmImageWriterSpi extends ImageWriterSpi {

    private static final String vendorName = "IFTM GmbH";

    private static final String version = "1.0";

    private static final String[] names = { "DICOM" };

    private static final String[] suffixes = { "dcm" };

    private static final String[] MIMETypes = { "Application/dicom" };

    private static final String writerClassName = "org.dcm4cheri.imageio.plugins.DcmImageWriter";

    private static final Class[] outputTypes = { ImageOutputStream.class };

    private static final String[] readerSpiNames = null;

    private static final boolean supportsStandardStreamMetadataFormat = false;

    private static final String nativeStreamMetadataFormatName = org.dcm4che.imageio.plugins.DcmMetadata.nativeMetadataFormatName;

    private static final String nativeStreamMetadataFormatClassName = "org.dcm4che.imageio.plugins.DcmMetadataFormat";

    private static final String[] extraStreamMetadataFormatNames = null;

    private static final String[] extraStreamMetadataFormatClassNames = null;

    private static final boolean supportsStandardImageMetadataFormat = false;

    private static final String nativeImageMetadataFormatName = null;

    private static final String nativeImageMetadataFormatClassName = null;

    private static final String[] extraImageMetadataFormatNames = null;

    private static final String[] extraImageMetadataFormatClassNames = null;

    /**
   * Constructs a blank ImageWriterSpi.
   */
    public DcmImageWriterSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, outputTypes, readerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    /**
   * Returns true if the ImageWriter implementation associated with this service 
   * provider is able to encode an image with the given layout. The layout (i.e., 
   * the image's SampleModel and ColorModel) is described by an ImageTypeSpecifier 
   * object.<br>
   * A return value of true is not an absolute guarantee of successful encoding; 
   * the encoding process may still produce errors due to factors such as I/O 
   * errors, inconsistent or malformed data structures, etc. The intent is that 
   * a reasonable inspection of the basic structure of the image be performed in 
   * order to determine if it is within the scope of the encoding format.<br>
   * @param type an ImageTypeSpecifier specifying the layout of the image to be 
   *             written.
   * @return allways true.
   * @throws IllegalArgumentException if type is null.
   */
    public boolean canEncodeImage(ImageTypeSpecifier type) throws IllegalArgumentException {
        return true;
    }

    /**
   * Returns an instance of the ImageWriter implementation associated with this 
   * service provider. The returned object will initially be in an initial state 
   * as if its reset method had been called.<br>
   * @param extension a plug-in specific extension object, which may be null. This
   *                  implementation does not support any extensions.
   * @return an ImageWriter instance.
   * @throws IOException if the attempt to instantiate the writer fails.
   * @throws IllegalArgumentException if the ImageWriter's constructor throws an 
   *         IllegalArgumentException to indicate that the extension object is 
   *         unsuitable.
   */
    public ImageWriter createWriterInstance(Object extension) throws IOException {
        return new DcmImageWriter(this);
    }

    /**
   * Returns the Locale associated with this writer.
   * @return the Locale.
   */
    public String getDescription(Locale locale) {
        return "DICOM image writer";
    }
}
