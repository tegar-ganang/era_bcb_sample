package metadata;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

/**
 * @author Fernando Moreno Ruiz
 * @version 0.01
 * <b>This class is pretending to read and write Jpg file metadata.<b>
 * */
public class JpegFileMetadata {

    static final int TAG_DATE = 36868;

    static DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    public static DateFormat formatterTimeDate = new SimpleDateFormat("yyyy-MM-dd-HHmmss");

    public static DateFormat formatterOnlyDate = new SimpleDateFormat("yyyy-MM-dd");

    public static DateFormat formatterOnlyTime = new SimpleDateFormat("HH.mm.ss");

    Date date;

    /**
	 * @author Fernando Moreno Ruiz
	 * @version 0.01
	 * <b>get date from jpg file.<b>
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws ImageReadException 
	 * */
    public JpegFileMetadata(String path) throws ImageReadException, IOException, ParseException {
        date = getDate(path);
    }

    public static Date getDate(String path) throws ImageReadException, IOException, ParseException {
        IImageMetadata jpgMetadata;
        TiffImageMetadata tiffMetadata;
        TiffField date;
        jpgMetadata = Sanselan.getMetadata(new File(path));
        if (jpgMetadata instanceof JpegImageMetadata) {
            tiffMetadata = ((JpegImageMetadata) jpgMetadata).getExif();
            date = tiffMetadata.findField(TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            return formatter.parse(date.getStringValue());
        } else {
            throw new ImageReadException("Image doesn't contain jpg metadata information.");
        }
    }

    /**
	 * @author Fernando Moreno Ruiz
	 * @version 0.01
	 * <b>set date from jpg file.<b>
	 * */
    public static void setDate(String path, Date date) throws ImageReadException, IOException, ClassCastException, ImageWriteException {
        File source = new File(path);
        File temp = null;
        OutputStream os = null;
        String stringDate = formatter.format(date);
        TiffOutputSet propiertiesSet = new TiffOutputSet();
        TiffOutputDirectory exifDirectory;
        propiertiesSet = ((JpegImageMetadata) Sanselan.getMetadata(source)).getExif().getOutputSet();
        propiertiesSet.removeField(TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        TiffOutputField newfieldDate = new TiffOutputField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, TiffFieldTypeConstants.FIELD_TYPE_ASCII, stringDate.length(), stringDate.getBytes());
        exifDirectory = propiertiesSet.getOrCreateExifDirectory();
        exifDirectory.add(newfieldDate);
        temp = File.createTempFile("temp-" + System.currentTimeMillis(), ".jpg");
        os = new FileOutputStream(temp);
        os = new BufferedOutputStream(os);
        ExifRewriter rewrite = new ExifRewriter();
        rewrite.updateExifMetadataLossless(source, os, propiertiesSet);
        os.close();
        FileUtils.copyFile(temp, source);
    }

    public String getTimeDate() {
        return formatterTimeDate.format(date);
    }

    public String getOnlyDate() {
        return formatterOnlyDate.format(date);
    }

    public String getOnlyTime() {
        return formatterOnlyTime.format(date);
    }
}
