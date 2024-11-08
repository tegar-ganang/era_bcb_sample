package com.tomgibara.imageio.impl.tiff;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import com.tomgibara.imageio.impl.common.PackageUtil;

public class TIFFImageReaderSpi extends ImageReaderSpi {

    private static final String[] names = { "tif", "TIF", "tiff", "TIFF" };

    private static final String[] suffixes = { "tif", "tiff" };

    private static final String[] MIMETypes = { "image/tiff" };

    private static final String readerClassName = "com.tomgibara.imageio.impl.tiff.TIFFImageReader";

    private static final String[] writerSpiNames = { "com.tomgibara.imageio.impl.tiff.TIFFImageWriterSpi" };

    private boolean registered = false;

    public TIFFImageReaderSpi() {
        super(PackageUtil.getVendor(), PackageUtil.getVersion(), names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, false, TIFFStreamMetadata.nativeMetadataFormatName, "com.tomgibara.imageio.impl.tiff.TIFFStreamMetadataFormat", null, null, true, TIFFImageMetadata.nativeMetadataFormatName, "com.tomgibara.imageio.impl.tiff.TIFFImageMetadataFormat", null, null);
    }

    public String getDescription(Locale locale) {
        String desc = PackageUtil.getSpecificationTitle() + " TIFF Image Reader";
        return desc;
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) {
            return false;
        }
        ImageInputStream stream = (ImageInputStream) input;
        byte[] b = new byte[4];
        stream.mark();
        stream.readFully(b);
        stream.reset();
        return ((b[0] == (byte) 0x49 && b[1] == (byte) 0x49 && b[2] == (byte) 0x2a && b[3] == (byte) 0x00) || (b[0] == (byte) 0x4d && b[1] == (byte) 0x4d && b[2] == (byte) 0x00 && b[3] == (byte) 0x2a));
    }

    public ImageReader createReaderInstance(Object extension) {
        return new TIFFImageReader(this);
    }

    public void onRegistration(ServiceRegistry registry, Class category) {
        if (registered) {
            return;
        }
        registered = true;
    }
}
