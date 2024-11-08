package gphoto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

public class SanselanDemo {

    public void readMetaData(File file) {
        IImageMetadata metadata = null;
        try {
            metadata = Sanselan.getMetadata(file);
        } catch (ImageReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            System.out.println("\nFile: " + file.getPath());
            printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION);
            printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE);
            printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE);
            TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (exifMetadata != null) {
                try {
                    TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
                    if (null != gpsInfo) {
                        double longitude = gpsInfo.getLongitudeAsDegreesEast();
                        double latitude = gpsInfo.getLatitudeAsDegreesNorth();
                        System.out.println("    " + "GPS Description: " + gpsInfo);
                        System.out.println("    " + "GPS Longitude (Degrees East): " + longitude);
                        System.out.println("    " + "GPS Latitude (Degrees North): " + latitude);
                    }
                } catch (ImageReadException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("EXIF items -");
            ArrayList<?> items = jpegMetadata.getItems();
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                System.out.println("    " + "item: " + item);
            }
            System.out.println();
        }
    }

    private static void printTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field == null) {
            System.out.println(tagInfo.name + ": " + "Not Found.");
        } else {
            System.out.println(tagInfo.name + ": " + field.getValueDescription());
        }
    }

    public void addImageHistoryTag(File file) {
        File dst = null;
        IImageMetadata metadata = null;
        JpegImageMetadata jpegMetadata = null;
        TiffImageMetadata exif = null;
        OutputStream os = null;
        TiffOutputSet outputSet = new TiffOutputSet();
        try {
            metadata = Sanselan.getMetadata(file);
        } catch (ImageReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (metadata != null) {
            jpegMetadata = (JpegImageMetadata) metadata;
        }
        if (jpegMetadata != null) {
            exif = jpegMetadata.getExif();
        }
        if (exif != null) {
            try {
                outputSet = exif.getOutputSet();
            } catch (ImageWriteException e) {
                e.printStackTrace();
            }
        }
        if (outputSet != null) {
            TiffOutputField imageHistoryPre = outputSet.findField(TiffConstants.EXIF_TAG_IMAGE_HISTORY);
            if (imageHistoryPre != null) {
                outputSet.removeField(TiffConstants.EXIF_TAG_IMAGE_HISTORY);
            }
            try {
                String fieldData = "ImageHistory-" + System.currentTimeMillis();
                TiffOutputField imageHistory = new TiffOutputField(ExifTagConstants.EXIF_TAG_IMAGE_HISTORY, TiffFieldTypeConstants.FIELD_TYPE_ASCII, fieldData.length(), fieldData.getBytes());
                TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                exifDirectory.add(imageHistory);
            } catch (ImageWriteException e) {
                e.printStackTrace();
            }
        }
        try {
            dst = File.createTempFile("temp-" + System.currentTimeMillis(), ".jpeg");
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            new ExifRewriter().updateExifMetadataLossless(file, os, outputSet);
        } catch (ImageReadException e) {
            e.printStackTrace();
        } catch (ImageWriteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        try {
            FileUtils.copyFile(dst, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File bikeFile = new File("data/bike.jpg");
        SanselanDemo demo = new SanselanDemo();
        System.out.println("BEFORE update");
        demo.readMetaData(bikeFile);
        demo.addImageHistoryTag(bikeFile);
        System.out.println("\nAFTER update");
        demo.readMetaData(bikeFile);
    }
}
