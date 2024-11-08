package com.orklz.jgeotagger.model;

import com.orklz.jgeotagger.util.ImageHelper;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.stream.FileImageInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.geotools.gpx2.gpxentities.SimpleWaypoint;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * This class implements ...
 *
 * @author Naxos Software Solutions GmbH, Oliver Wilkening
 * @version $Id$
 */
public class JpegBean {

    private File image;

    private BufferedImage thumbnail;

    private JpegImageMetadata metadata = null;

    private static final int THB_WIDTH = 150;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    private GeoPosition tmpGeoPosition;

    /**
    * The default constructor to create an object of a JpegBean
    */
    public JpegBean() {
    }

    public JpegBean(File image) throws Exception {
        this.image = image;
        try {
            setThumbnail(genThumbnail(image));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        getTimestamp();
    }

    public File getImage() {
        return image;
    }

    public BufferedImage getBufferedImage() {
        try {
            return Sanselan.getBufferedImage(image);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setImage(File image) {
        this.image = image;
    }

    public BufferedImage getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(BufferedImage thumbnail) {
        this.thumbnail = thumbnail;
    }

    public JpegImageMetadata getMetadata() throws ImageReadException, IOException {
        if (image != null) {
            metadata = (JpegImageMetadata) Sanselan.getMetadata(image);
        } else {
            throw new NullPointerException("No image available!");
        }
        return metadata;
    }

    public GeoPosition getTmpGeoPosition() {
        return tmpGeoPosition;
    }

    public void setTmpGeoPosition(GeoPosition tmpGeoPosition) {
        this.tmpGeoPosition = tmpGeoPosition;
    }

    public void setTmpGeoPosition(SimpleWaypoint point) {
        setTmpGeoPosition(new GeoPosition(point.getLatitude(), point.getLongitude()));
    }

    public void writeGeoPosition(GeoPosition pos) throws ImageReadException, IOException, ImageWriteException {
        JpegImageMetadata jpegMetadata = getMetadata();
        TiffOutputSet os = getMetadata().getExif().getOutputSet();
        double lng = pos.getLongitude();
        double lat = pos.getLatitude();
        os.setGPSInDegrees(lng, lat);
        File dst = File.createTempFile("temp-" + System.currentTimeMillis(), ".jpeg");
        OutputStream out = new FileOutputStream(dst);
        out = new BufferedOutputStream(out);
        new ExifRewriter().updateExifMetadataLossless(image, out, os);
        out.close();
        FileUtils.copyFile(dst, image);
    }

    public void writeGeoPosition(SimpleWaypoint point) throws ImageReadException, IOException, ImageWriteException {
        writeGeoPosition(new GeoPosition(point.getLatitude(), point.getLongitude()));
    }

    public GeoPosition getGeoPosition() throws ImageReadException, IOException {
        GeoPosition result = null;
        if (image != null) {
            TiffImageMetadata.GPSInfo gpsInfo = getMetadata().getExif().getGPS();
            if (gpsInfo != null) {
                result = new GeoPosition(gpsInfo.getLatitudeAsDegreesNorth(), gpsInfo.getLongitudeAsDegreesEast());
            }
        } else {
            throw new NullPointerException("No image available!");
        }
        return result;
    }

    public String getExifTag(TagInfo tag) throws ImageReadException, IOException {
        String result = null;
        JpegImageMetadata jpegMetadata = getMetadata();
        if (jpegMetadata != null) {
            result = printTagValue(jpegMetadata, tag);
        }
        return result;
    }

    public long getTimestamp() {
        long ts = -1;
        String tmpTimestamp = null;
        try {
            tmpTimestamp = printTagValue(getMetadata(), TiffConstants.TIFF_TAG_DATE_TIME);
            if (tmpTimestamp != null) {
                tmpTimestamp = tmpTimestamp.replaceAll("'", "");
                Date tmpDate = dateFormat.parse(tmpTimestamp);
                ts = tmpDate.getTime();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (tmpTimestamp == null) {
            ts = image.lastModified();
        }
        return (ts);
    }

    private String printTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        String result = null;
        if (field != null) {
            result = field.getValueDescription();
        }
        return result;
    }

    private BufferedImage genThumbnail(File srcFile) throws FileNotFoundException, IOException {
        FileImageInputStream in = new FileImageInputStream(srcFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int n;
        byte[] buf = new byte[1000];
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        byte[] ba = bos.toByteArray();
        return ImageHelper.resize(ba, "jpg", THB_WIDTH);
    }
}
