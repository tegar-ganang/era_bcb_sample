package edu.whitman.halfway.jigs;

import edu.whitman.halfway.exif.ExifConstants;
import edu.whitman.halfway.exif.ExifHashMap;
import edu.whitman.halfway.exif.ExifReader;
import java.io.File;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class PictureDescriptionInfo extends AlbumObjectDescriptionInfo {

    protected static Logger log = Logger.getLogger(PictureDescriptionInfo.class.getName());

    protected File imageFile;

    protected boolean autoFill;

    protected boolean overwrite;

    public PictureDescriptionInfo(PictureData pic) {
        this(pic, false, false);
    }

    public PictureDescriptionInfo(PictureData pic, boolean autoFill, boolean overwrite) {
        super(getDescriptionFile(pic.getFile()));
        imageFile = pic.getFile();
        this.autoFill = autoFill;
        this.overwrite = overwrite;
        if (autoFill) {
            readExifData(overwrite);
            calcOriginalDim(pic);
        }
    }

    public int getOrigWidth() {
        Integer w = (Integer) getData(JIGS_ORIGINAL_WIDTH);
        if (w != null) {
            return w.intValue();
        } else {
            log.error("Error, no width set in " + this + ", returning -1");
            return -1;
        }
    }

    public int getOrigHeight() {
        Integer h = (Integer) getData(JIGS_ORIGINAL_HEIGHT);
        if (h != null) {
            return h.intValue();
        } else {
            log.error("Error, no height set in " + this + ", returning -1");
            return -1;
        }
    }

    protected void calcOriginalDim(PictureData pic) {
        if (getOrigWidth() > 0 || getOrigHeight() > 0) {
            return;
        }
    }

    /** Given a file representing a valid image, returns the
        description file for the image.  If an old-style desc file
        exists, that file is returned.  Otherwise, the new preferred(?)
        name is returned, whether or not the file exists.. */
    public static File getDescriptionFile(File file) {
        String s = file.getAbsolutePath();
        File name;
        File oldName = new File(s.substring(0, s.lastIndexOf(".")) + ".txt");
        if (oldName.exists() && oldName.isFile()) name = oldName; else name = new File(s + ".txt");
        return name;
    }

    public void readExifData() {
        readExifData(overwrite);
    }

    public void readExifData(boolean overwrite) {
        HashMap tempMap = new HashMap();
        if ((imageFile != null) && ExifReader.isExif(imageFile)) {
            log.debug("Reading EXIF for " + imageFile + " " + overwrite);
            ExifHashMap hashMap = new ExifHashMap(ExifReader.decode(imageFile));
            tempMap.put("exif.cameramake", hashMap.getCameraMake());
            tempMap.put("exif.cameramodel", hashMap.getCameraModel());
            tempMap.put("exif.shutterspeed", hashMap.getShutterSpeed());
            tempMap.put("exif.fstop", hashMap.getFStop());
            tempMap.put("exif.exposureprogram", hashMap.getExposureProgram());
            tempMap.put("exif.flash", hashMap.getFlash());
            tempMap.put("exif.iso", "" + hashMap.getISO());
            tempMap.put("exif.lightsource", hashMap.getLightSource());
            tempMap.put("exif.focallength", "" + hashMap.getRational(ExifConstants.FOCAL_LENGTH));
            String s = hashMap.getString(ExifConstants.CREATION_DATE);
            if (!s.equals("")) tempMap.put(JIGS_DATE, s);
        }
        if (overwrite) {
            descMap.putAll(tempMap);
        } else {
            tempMap.putAll(descMap);
            descMap = tempMap;
        }
        convertTypes();
    }
}
