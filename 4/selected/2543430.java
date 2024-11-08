package org.pagger.data.picture.exif;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.common.RationalNumber;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.fieldtypes.FieldType;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.pagger.util.Validator;

/**
 * @author Gerd Saurer
 */
public class ExifUtil {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private ExifUtil() {
    }

    public static FieldType getFieldType(Class<?> clazz) {
        FieldType fieldType = null;
        if (String.class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_ASCII;
        } else if (Double.class.equals(clazz) || double.class.equals(clazz) || Double[].class.equals(clazz) || double[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_DOUBLE;
        } else if (Float.class.equals(clazz) || float.class.equals(clazz) || Float[].class.equals(clazz) || float[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_LONG;
        } else if (Long.class.equals(clazz) || long.class.equals(clazz) || Long[].class.equals(clazz) || long[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_DOUBLE;
        } else if (Short.class.equals(clazz) || short.class.equals(clazz) || Short[].class.equals(clazz) || short[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_SHORT;
        } else if (RationalNumber.class.equals(clazz) || RationalNumber[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_RATIONAL;
        } else if (Integer.class.equals(clazz) || int.class.equals(clazz) || Integer[].class.equals(clazz) || int[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_RATIONAL;
        } else if (Boolean.class.equals(clazz) || boolean.class.equals(clazz) || Boolean[].class.equals(clazz) || boolean[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_BYTE;
        } else if (Byte.class.equals(clazz) || byte.class.equals(clazz) || Byte[].class.equals(clazz) || byte[].class.equals(clazz)) {
            fieldType = FieldType.FIELD_TYPE_BYTE;
        }
        return fieldType;
    }

    public static Date toDate(final String date) throws ParseException {
        Validator.notEmpty(date, "Date");
        return DATE_FORMATTER.parse(date);
    }

    public static String toString(final Date date) {
        Validator.notNull(date, "Date");
        return DATE_FORMATTER.format(date);
    }

    public static Object getValue(final ExifRawMetadata metadata, final TagInfo tagInfo) throws ImageReadException {
        Validator.notNull(metadata, "Metadata");
        Validator.notNull(tagInfo, "Tag Info");
        final TiffField field = metadata.getRawMetadata().findField(tagInfo);
        Object retVal = null;
        if (field != null) {
            retVal = tagInfo.getValue(field);
            if (retVal instanceof String) {
                String temp = (String) retVal;
                char toCheck = temp.charAt(temp.length() - 1);
                if (Character.isDigit(toCheck) == false && Character.isLetter(toCheck) == false) {
                    retVal = temp.substring(0, temp.length() - 1);
                }
            }
        }
        return retVal;
    }

    public static void setValue(final ExifRawMetadata metadata, final TagInfo tagInfo, final FieldType type, final Object value) throws ImageWriteException {
        Validator.notNull(metadata, "Metadata");
        Validator.notNull(tagInfo, "Tag Info");
        Validator.notNull(type, "Field Type");
        final TiffOutputSet outputSet = metadata.getFinalOutputSet();
        final TiffOutputField field = outputSet.findField(tagInfo);
        if (field != null) {
            outputSet.removeField(tagInfo);
        }
        if (value != null) {
            byte[] data = type.writeData(value, outputSet.byteOrder);
            int length = 1;
            if (value instanceof String) {
                length = data.length;
            } else if (value.getClass().isArray()) {
                length = Array.getLength(value);
            }
            TiffOutputDirectory directory = outputSet.getOrCreateExifDirectory();
            directory.add(new TiffOutputField(tagInfo, type, length, data));
        }
    }

    public static void print(final ExifRawMetadata metadata) {
        Validator.notNull(metadata, "Metadata");
        TagInfo[] allTags = TiffField.ALL_TAGS;
        Map<String, String> temp = new HashMap<String, String>();
        for (int i = 0; i < allTags.length; i++) {
            TagInfo tagInfo = allTags[i];
            try {
                if (tagInfo != null && temp.containsKey(tagInfo.name) == false) {
                    temp.put(tagInfo.name, tagInfo.name + "\t" + ((tagInfo.dataTypes != null && tagInfo.dataTypes.length > 0) ? tagInfo.dataTypes[0].name : "NULL") + ": \t" + convert(ExifUtil.getValue(metadata, tagInfo)));
                }
            } catch (ImageReadException e) {
                e.printStackTrace();
            }
        }
        List<String> keys = new ArrayList<String>();
        keys.addAll(temp.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            System.out.println(temp.get(key));
        }
    }

    private static String convert(Object value) {
        String retVal = null;
        if (value != null) {
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                retVal = "";
                for (int i = 0; i < length; i++) {
                    Object object = Array.get(value, i);
                    retVal += object.toString() + " § ";
                }
            } else {
                retVal = value.toString().trim();
            }
        }
        return retVal;
    }

    public static void write(final File location, final ExifRawMetadata metadata) throws IOException {
        Validator.notNull(location, "Image location");
        Validator.notNull(metadata, "Metadata");
        TiffOutputSet outputSet = metadata.getFinalOutputSet();
        try {
            final String fileName = location.getName();
            final File tempLocation = File.createTempFile("pagger-" + System.currentTimeMillis(), "." + fileName.substring(fileName.lastIndexOf(".") + 1));
            OutputStream out = null;
            try {
                final ExifRewriter writer = new ExifRewriter();
                out = new FileOutputStream(tempLocation);
                writer.updateExifMetadataLossy(location, out, outputSet);
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
}
