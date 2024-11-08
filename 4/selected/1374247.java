package org.apache.sanselan.sampleUsage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.apache.sanselan.util.IOUtils;

public class WriteExifMetadataExample {

    public void removeExifMetadata(File jpegImageFile, File dst) throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
            new ExifRewriter().removeExifMetadata(jpegImageFile, os);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This example illustrates how to add/update EXIF metadata in a JPEG file.
     *
     * @param jpegImageFile
     *            A source image file.
     * @param dst
     *            The output file.
     * @throws IOException
     * @throws ImageReadException
     * @throws ImageWriteException
     */
    public void changeExifMetadata(File jpegImageFile, File dst) throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        try {
            TiffOutputSet outputSet = null;
            IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
            if (null == outputSet) outputSet = new TiffOutputSet();
            {
                TiffOutputField aperture = TiffOutputField.create(TiffConstants.EXIF_TAG_APERTURE_VALUE, outputSet.byteOrder, new Double(0.3));
                TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                exifDirectory.removeField(TiffConstants.EXIF_TAG_APERTURE_VALUE);
                exifDirectory.add(aperture);
            }
            {
                double longitude = -74.0;
                double latitude = 40 + 43 / 60.0;
                outputSet.setGPSInDegrees(longitude, latitude);
            }
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
            os.close();
            os = null;
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This example illustrates how to remove a tag (if present) from EXIF
     * metadata in a JPEG file.
     *
     * In this case, we remove the "aperture" tag from the EXIF metadata if
     * present.
     *
     * @param jpegImageFile
     *            A source image file.
     * @param dst
     *            The output file.
     * @throws IOException
     * @throws ImageReadException
     * @throws ImageWriteException
     */
    public void removeExifTag(File jpegImageFile, File dst) throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        try {
            TiffOutputSet outputSet = null;
            IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
            if (null == outputSet) {
                IOUtils.copyFileNio(jpegImageFile, dst);
                return;
            }
            {
                outputSet.removeField(TiffConstants.EXIF_TAG_APERTURE_VALUE);
                TiffOutputDirectory exifDirectory = outputSet.getExifDirectory();
                if (null != exifDirectory) exifDirectory.removeField(TiffConstants.EXIF_TAG_APERTURE_VALUE);
            }
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
            os.close();
            os = null;
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This example illustrates how to set the GPS values in JPEG EXIF metadata.
     *
     * @param jpegImageFile
     *            A source image file.
     * @param dst
     *            The output file.
     * @throws IOException
     * @throws ImageReadException
     * @throws ImageWriteException
     */
    public void setExifGPSTag(File jpegImageFile, File dst) throws IOException, ImageReadException, ImageWriteException {
        OutputStream os = null;
        try {
            TiffOutputSet outputSet = null;
            IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
            if (null == outputSet) outputSet = new TiffOutputSet();
            {
                double longitude = -74.0;
                double latitude = 40 + 43 / 60.0;
                outputSet.setGPSInDegrees(longitude, latitude);
            }
            os = new FileOutputStream(dst);
            os = new BufferedOutputStream(os);
            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
            os.close();
            os = null;
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }
}
