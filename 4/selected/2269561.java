package org.pagger.data.picture.iptc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.formats.jpeg.iptc.IPTCConstants;
import org.apache.sanselan.formats.jpeg.iptc.IPTCRecord;
import org.apache.sanselan.formats.jpeg.iptc.IPTCType;
import org.apache.sanselan.formats.jpeg.iptc.JpegIptcRewriter;
import org.apache.sanselan.formats.jpeg.iptc.PhotoshopApp13Data;
import org.pagger.util.Validator;

/**
 * @author Gerd Saurer
 */
public class IptcUtil {

    private IptcUtil() {
    }

    public static void write(final File location, final IptcRawMetadata metadata) throws IOException {
        Validator.notNull(location, "Image location");
        Validator.notNull(metadata, "Metadata");
        PhotoshopApp13Data outputSet = metadata.getFinalOutputSet();
        try {
            final String fileName = location.getName();
            final File tempLocation = File.createTempFile("pagger-" + System.currentTimeMillis(), "." + fileName.substring(fileName.lastIndexOf(".") + 1));
            OutputStream out = null;
            try {
                final JpegIptcRewriter writer = new JpegIptcRewriter();
                out = new FileOutputStream(tempLocation);
                writer.writeIPTC(location, out, outputSet);
            } finally {
                IOUtils.closeQuietly(out);
            }
            FileUtils.copyFile(tempLocation, location);
        } catch (ImageReadException ex) {
            throw new IOException(ex);
        } catch (ImageWriteException ex) {
            throw new IOException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getValues(IptcRawMetadata metadata, IPTCType type) {
        Validator.notNull(metadata, "Metadata");
        Validator.notNull(type, "Type");
        final List<String> retVal = new ArrayList<String>();
        final PhotoshopApp13Data photoshopApp13Data = metadata.getFinalOutputSet();
        final List<IPTCRecord> oldRecords = photoshopApp13Data.getRecords();
        for (IPTCRecord record : oldRecords) {
            if (record.iptcType.type == type.type) {
                retVal.add(record.value);
            }
        }
        return retVal;
    }

    public static String getValue(IptcRawMetadata metadata, IPTCType type) {
        final List<String> values = getValues(metadata, type);
        if (values.isEmpty()) {
            return null;
        } else {
            if (values.size() > 1) {
            }
            return values.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    public static void setValues(final IptcRawMetadata metadata, final IPTCType type, final List<String> values) {
        Validator.notNull(metadata, "Metadata");
        Validator.notNull(type, "Type");
        Validator.notNull(values, "Values");
        final PhotoshopApp13Data photoshopApp13Data = metadata.getFinalOutputSet();
        final List<IPTCRecord> records = photoshopApp13Data.getRecords();
        final List<IPTCRecord> toRemove = new ArrayList<IPTCRecord>();
        for (IPTCRecord record : records) {
            if (record.iptcType.type == type.type) {
                toRemove.add(record);
            }
        }
        for (IPTCRecord record : toRemove) {
            photoshopApp13Data.getRecords().remove(record);
        }
        for (String value : values) {
            if (value != null) {
                final IPTCRecord record = new IPTCRecord(type, value);
                photoshopApp13Data.getRecords().add(record);
            }
        }
        throw new UnsupportedOperationException();
    }

    public static void setValue(final IptcRawMetadata metadata, final IPTCType type, final String value) {
        setValues(metadata, type, Collections.singletonList(value));
    }

    public static void print(final IptcRawMetadata metadata) {
        Validator.notNull(metadata, "Metadata");
        final PhotoshopApp13Data photoshopApp13Data = metadata.getRawMetadata().photoshopApp13Data;
        List<?> oldRecords = photoshopApp13Data.getRecords();
        System.out.println();
        for (int j = 0; j < oldRecords.size(); j++) {
            IPTCRecord record = (IPTCRecord) oldRecords.get(j);
            if (record.iptcType.type != IPTCConstants.IPTC_TYPE_CITY.type) System.out.println("Key: " + record.iptcType.name + " (0x" + Integer.toHexString(record.iptcType.type) + "), value: " + record.value);
        }
        System.out.println();
    }
}
