package nl.ikarus.nxt.priv.imageio.icoreader.lib;

import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.spi.*;
import nl.ikarus.nxt.priv.imageio.icoreader.*;
import javax.imageio.stream.*;

public class ICOReaderSpi extends ImageReaderSpi {

    public ICOReaderSpi() {
        this("nXt webapps", VersionData.getVersion(), new String[] { "ico", "ICO" }, new String[] { "ico", "ICO" }, new String[] { "image/vnd.microsoft.icon", "image/x-ico" }, "nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReader", new Class[] { javax.imageio.stream.ImageInputStream.class }, null, false, null, null, null, null, false, ICOMetaDataFormat.NAME, "nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOMetaData", null, null);
    }

    /**
   * vendorName - the vendor name, as a non-null  String
   * version - a version identifier, as a non-null String
   *  names - a non-null array of Strings indicating the format names. At least one entry must be present.
   * suffixes - an array of Strings indicating the common file suffixes. If no suffixes are defined, null should be supplied. An array of length 0 will be normalized to null.
   * MIMETypes - an array of Strings indicating the format's MIME types. If no MIME types are defined, null should be supplied. An array of length 0 will be normalized to null.
   * readerClassName - the fully-qualified name of the associated ImageReader class, as a non-null String.
   * inputTypes - a non-null array of Class objects of length at least 1 indicating the legal input types.
   * writerSpiNames - an array Strings naming the classes of all associated ImageWriters, or null. An array of length 0 is normalized to null.
   * supportsStandardStreamMetadataFormat - a boolean that indicates whether a stream metadata object can use trees described by the standard metadata format.
   * nativeStreamMetadataFormatName - a String, or null, to be returned from getNativeStreamMetadataFormatName.
   * nativeStreamMetadataFormatClassName - a String, or null, to be used to instantiate a metadata format object to be returned from getNativeStreamMetadataFormat.
   * extraStreamMetadataFormatNames - an array of Strings, or null, to be returned from getExtraStreamMetadataFormatNames. An array of length 0 is normalized to null.
   * extraStreamMetadataFormatClassNames - an array of Strings, or null, to be used to instantiate a metadata format object to be returned from getStreamMetadataFormat. An array of length 0 is normalized to null.
   * supportsStandardImageMetadataFormat - a boolean that indicates whether an image metadata object can use trees described by the standard metadata format.
   * nativeImageMetadataFormatName - a String, or null, to be returned from getNativeImageMetadataFormatName.
   * nativeImageMetadataFormatClassName - a String, or null, to be used to instantiate a metadata format object to be returned from getNativeImageMetadataFormat.
   * extraImageMetadataFormatNames - an array of Strings to be returned from getExtraImageMetadataFormatNames. An array of length 0 is normalized to null.
   * extraImageMetadataFormatClassNames - an array of Strings, or null, to be used to instantiate a metadata format object to be returned from getImageMetadataFormat. An array of length 0 is normalized to null.
   *
   */
    public ICOReaderSpi(String vendorName, String version, String[] names, String[] suffixes, String[] MIMETypes, String readerClassName, Class[] inputTypes, String[] writerSpiNames, boolean supportsStandardStreamMetadataFormat, String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName, String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames, boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, inputTypes, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    /**
   * Returns <code>true</code> if the supplied source object appears to be of
   * the format supported by this reader.
   *
   * @param source the object (typically an <code>ImageInputStream</code>) to
   *   be decoded.
   * @return <code>true</code> if it is likely that this stream can be decoded.
   * @throws IOException if an I/O error occurs while reading the stream.
   * @todo Implement this javax.imageio.spi.ImageReaderSpi method
   */
    public boolean canDecodeInput(Object source) throws IOException {
        if (source instanceof ImageInputStream) {
            byte[] buff = new byte[4];
            int len;
            ImageInputStream in = ((ImageInputStream) source);
            in.mark();
            in.readFully(buff);
            in.reset();
            return (buff[0] == 0x00 && buff[1] == 0x00 && buff[2] == 0x01 && buff[3] == 0x00);
        }
        return true;
    }

    private static volatile Boolean isRegistered = Boolean.FALSE;

    static {
        registerIcoReader();
    }

    public static synchronized void registerIcoReader() {
        if (ICOReaderSpi.isRegistered.booleanValue()) return;
        ICOReaderSpi.isRegistered = Boolean.TRUE;
        try {
            Object registeredReader = IIORegistry.getDefaultInstance().getServiceProviderByClass(ICOReaderSpi.class);
            if (registeredReader == null) {
                Object reader = new ICOReaderSpi();
                IIORegistry.getDefaultInstance().registerServiceProvider(reader);
            }
        } finally {
            boolean DEBUG = Boolean.valueOf(System.getProperty(ICOReader.PROPERTY_NAME_PREFIX + "debug", Boolean.toString(false)));
            if (DEBUG) System.out.println(ICOReader.class.getName() + " loaded, version: " + VersionData.getVersion() + " build: " + VersionData.getBuild());
        }
    }

    /**
   * Returns an instance of the <code>ImageReader</code> implementation
   * associated with this service provider.
   *
   * @param extension a plug-in specific extension object, which may be
   *   <code>null</code>.
   * @return an <code>ImageReader</code> instance.
   * @throws IOException if the attempt to instantiate the reader fails.
   * @todo Implement this javax.imageio.spi.ImageReaderSpi method
   */
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new ICOReader(this);
    }

    /**
   * Returns a brief, human-readable description of this service provider and
   * its associated implementation.
   *
   * @param locale a <code>Locale</code> for which the return value should be
   *   localized.
   * @return a <code>String</code> containing a description of this service
   *   provider.
   * @todo Implement this javax.imageio.spi.IIOServiceProvider method
   */
    public String getDescription(Locale locale) {
        return "Microsoft Icon Format (ICO) Reader version: " + VersionData.getVersion() + " #" + VersionData.getBuild();
    }
}
