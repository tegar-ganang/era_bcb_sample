package org.dcm4cheri.imageio.plugins;

import org.dcm4che.data.DcmParseException;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author  gunter.zeilinger@tiani.com
 * @version 1.0.0
 */
public class DcmImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "TIANI MEDGRAPH AG";

    static final String version = "1.0";

    static final String[] names = { "DICOM" };

    static final String[] suffixes = { "dcm" };

    static final String[] MIMETypes = { "Application/dicom" };

    static final String readerClassName = "org.dcm4cheri.imageio.plugins.DcmImageReader";

    private static final String[] writerSpiNames = null;

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = org.dcm4che.imageio.plugins.DcmMetadata.nativeMetadataFormatName;

    static final String nativeStreamMetadataFormatClassName = "org.dcm4che.imageio.plugins.DcmMetadataFormat";

    static final DcmImageReaderConf conf = DcmImageReaderConf.getInstance();

    static final boolean supportsStandardImageMetadataFormat = false;

    static final String nativeImageMetadataFormatName = null;

    static final String nativeImageMetadataFormatClassName = null;

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    public DcmImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, conf.getExtraStreamMetadataFormatNames(), conf.getExtraStreamMetadataFormatClassNames(), supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    }

    public String getDescription(Locale locale) {
        return "DICOM image reader";
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }
        DcmParser parser = DcmParserFactory.getInstance().newDcmParser((ImageInputStream) input);
        try {
            parser.detectFileFormat();
            return true;
        } catch (DcmParseException e) {
            return false;
        }
    }

    public ImageReader createReaderInstance(Object extension) {
        return new DcmImageReader(this);
    }
}
