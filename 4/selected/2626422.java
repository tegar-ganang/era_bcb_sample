package com.tomgibara.imageio.impl.tiff;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import com.tomgibara.imageio.impl.common.PackageUtil;

public class TIFFImageWriterSpi extends ImageWriterSpi {

    private static final String[] names = { "tif", "TIF", "tiff", "TIFF" };

    private static final String[] suffixes = { "tif", "tiff" };

    private static final String[] MIMETypes = { "image/tiff" };

    private static final String writerClassName = "com.tomgibara.imageio.impl.tiff.TIFFImageWriter";

    private static final String[] readerSpiNames = { "com.tomgibara.imageio.impl.tiff.TIFFImageReaderSpi" };

    private boolean registered = false;

    public TIFFImageWriterSpi() {
        super(PackageUtil.getVendor(), PackageUtil.getVersion(), names, suffixes, MIMETypes, writerClassName, STANDARD_OUTPUT_TYPE, readerSpiNames, false, TIFFStreamMetadata.nativeMetadataFormatName, "com.tomgibara.imageio.impl.tiff.TIFFStreamMetadataFormat", null, null, false, TIFFImageMetadata.nativeMetadataFormatName, "com.tomgibara.imageio.impl.tiff.TIFFImageMetadataFormat", null, null);
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        return true;
    }

    public String getDescription(Locale locale) {
        String desc = PackageUtil.getSpecificationTitle() + " TIFF Image Writer";
        return desc;
    }

    public ImageWriter createWriterInstance(Object extension) {
        return new TIFFImageWriter(this);
    }

    public void onRegistration(ServiceRegistry registry, Class category) {
        if (registered) {
            return;
        }
        registered = true;
    }
}
