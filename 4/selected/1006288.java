package org.jpedal.io;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import javax.imageio.ImageIO;

/**
 * set of methods to save/load objects to keep memory 
 * usage to a minimum by spooling images to disk 
 * Also includes ancillary method to store a filename - 
 * LogWriter is my logging class - 
 * Several methods are very similar and I should recode 
 * my code to use a common method for the RGB conversion 
 * 
 * Converted to avoid threading issues, if this causes any problems, 
 * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php - 
 */
public class ObjectStore {

    /**debug page cache*/
    private static final boolean debugCache = false;

    /**correct separator for platform program running on*/
    private static final String separator = System.getProperty("file.separator");

    /**file being decoded at present -used byOXbjects and other classes*/
    private String currentFilename = "";

    /**temp storage for the images so they are not held in memory */
    public static String temp_dir = "";

    /**temp storage for raw CMYK images*/
    private String cmyk_dir = temp_dir + "cmyk" + separator;

    /**key added to each file to make sure unique to pdf being handled*/
    private String key = "jpedal" + Math.random() + "_";

    /** track whether image saved as tif or jpg*/
    private Map image_type = new Hashtable();

    /**
	 * map to hold file names
	 */
    private Map tempFileNames = new HashMap();

    /**
	 * Converted to avoid threading issues, if this causes any problems, 
	 * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public String fullFileName;

    private static Map pagesOnDisk = new HashMap(), pagesOnDiskAsBytes = new HashMap();

    /**
	 * ObjectStore -
	 * Converted for Threading purposes - 
	 * To fix any errors please try replacing
	 * <b>ObjectStore</b> with 
	 * <b>{your instance of PdfDecoder}.getObjectStore()</b> - 
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
	 * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public ObjectStore() {
        try {
            if (temp_dir.length() == 0) temp_dir = System.getProperty("java.io.tmpdir") + separator + "jpedal" + separator;
            File f = new File(temp_dir);
            if (f.exists() == false) f.mkdirs();
        } catch (Exception e) {
            LogWriter.writeLog("Unable to create temp dir at " + temp_dir);
        }
    }

    /**
	 * 
	 * get the file name - we use this as a get in our file repository - 
	 * 
	 * 
	 * <b>Note </b> this method is not part of the API and is not guaranteed to
	 * be in future versions of JPedal - 
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
	 * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public String getCurrentFilename() {
        return currentFilename;
    }

    /**
	  * store filename as a key we can use to differentiate images,etc - 
	  *  <b>Note</b> this method is not part of the API and
	  * is not guaranteed to be in future versions of JPedal - 
 	  * 
 	  * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final void storeFileName(String name) {
        fullFileName = name;
        int temp_pointer = name.indexOf("\\");
        if (temp_pointer == -1) temp_pointer = name.indexOf("/");
        while (temp_pointer != -1) {
            name = name.substring(temp_pointer + 1);
            temp_pointer = name.indexOf("\\");
            if (temp_pointer == -1) temp_pointer = name.indexOf("/");
        }
        int pointer = name.lastIndexOf(".");
        if (pointer != -1) name = name.substring(0, pointer);
        name = Strip.stripAllSpaces(name);
        currentFilename = name.toLowerCase();
    }

    /**
	 * save raw CMYK data in CMYK directory - We extract the DCT encoded image 
	 * stream and save as a file with a .jpeg ending so we have the raw image - 
	 * This works for DeviceCMYK - 
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public boolean saveRawCMYKImage(byte[] image_data, String name) {
        boolean isSuccessful = true;
        File cmyk_d = new File(cmyk_dir);
        if (cmyk_d.exists() == false) cmyk_d.mkdirs();
        try {
            FileOutputStream a = new FileOutputStream(cmyk_dir + name + ".jpg");
            tempFileNames.put(cmyk_dir + name + ".jpg", "#");
            a.write(image_data);
            a.flush();
            a.close();
        } catch (Exception e) {
            LogWriter.writeLog("Unable to save CMYK jpeg " + name);
            isSuccessful = false;
        }
        return isSuccessful;
    }

    /**
	 * save buffered image as JPEG or tif
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final synchronized boolean saveStoredImage(String current_image, BufferedImage image, boolean file_name_is_path, boolean save_unclipped, String type) {
        boolean was_error = false;
        int type_id = image.getType();
        File checkDir = new File(temp_dir);
        if (checkDir.exists() == false) checkDir.mkdirs();
        if (type.indexOf("tif") != -1) {
            if (((type_id == 1) | (type_id == 2)) && (current_image.indexOf("HIRES_") == -1)) image = ColorSpaceConvertor.convertColorspace(image, BufferedImage.TYPE_3BYTE_BGR);
            if (file_name_is_path == false) image_type.put(current_image, "tif");
            was_error = saveStoredImage("TIFF", ".tif", ".tiff", current_image, image, file_name_is_path, save_unclipped);
        } else if (type.indexOf("jpg") != -1) {
            if (file_name_is_path == false) image_type.put(current_image, "jpg");
            was_error = saveStoredJPEGImage(current_image, image, file_name_is_path, save_unclipped);
        } else if (type.indexOf("png") != -1) {
            if (file_name_is_path == false) image_type.put(current_image, "png");
            was_error = saveStoredImage("PNG", ".png", ".png", current_image, image, file_name_is_path, save_unclipped);
        }
        image = null;
        return was_error;
    }

    /**
	 * get type of image used to store graphic
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final String getImageType(String current_image) {
        return (String) image_type.get(current_image);
    }

    /**
	 * init method to pass in values for temp directory, unique key,
	 * etc so program knows where to store files
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final void init(String current_key) {
        key = current_key + System.currentTimeMillis();
        File f = new File(temp_dir);
        if (f.exists() == false) f.mkdirs();
    }

    /**
	 * load a image when required and remove from store
	 *
	 * Converted to avoid threading issues, if this causes any problems,
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final synchronized BufferedImage loadStoredImage(String current_image) {
        String flag = (String) image_type.get(current_image);
        BufferedImage image = null;
        if (flag == null) return null; else if (flag.equals("tif")) image = loadStoredImage(current_image, ".tif"); else if (flag.equals("jpg")) image = loadStoredJPEGImage(current_image); else if (flag.equals("png")) image = loadStoredImage(current_image, ".png");
        return image;
    }

    /**
	 * see if image already saved to disk (ie multiple pages)
	 */
    public final synchronized boolean isImageCached(String current_image) {
        String flag = (String) image_type.get(current_image);
        BufferedImage image = null;
        if (flag == null) return false; else {
            String file_name = temp_dir + key + current_image + "." + flag;
            File imgFile = new File(file_name);
            return imgFile.exists();
        }
    }

    /**
	 * routine to remove all objects from temp store
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final void flush() {
        if (temp_dir.length() > 2) {
            Iterator filesTodelete = tempFileNames.keySet().iterator();
            while (filesTodelete.hasNext()) {
                String file = ((String) filesTodelete.next());
                if (file.indexOf(key) != -1) {
                    File delete_file = new File(file);
                    delete_file.delete();
                }
            }
        }
        File cmyk_d = new File(cmyk_dir);
        if (cmyk_d.exists()) {
            cmyk_d.delete();
            cmyk_d = null;
        }
    }

    /**
	 * copies cmyk raw data from cmyk temp dir to target directory
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public void copyCMYKimages(String target_dir) {
        File cmyk_d = new File(cmyk_dir);
        if (cmyk_d.exists()) {
            String[] file_list = cmyk_d.list();
            if (file_list.length > 0) {
                if (target_dir.endsWith(separator) == false) target_dir = target_dir + separator;
                File test_d = new File(target_dir);
                if (test_d.exists() == false) test_d.mkdirs();
                test_d = null;
            }
            for (int ii = 0; ii < file_list.length; ii++) {
                File source = new File(cmyk_dir + file_list[ii]);
                File dest = new File(target_dir + file_list[ii]);
                source.renameTo(dest);
            }
        }
        cmyk_d = null;
    }

    /**
	 * save buffered image as JPEG
	 */
    private final synchronized boolean saveStoredJPEGImage(String current_image, BufferedImage image, boolean file_name_is_path, boolean save_unclipped) {
        boolean was_error = false;
        boolean image_written = false;
        String file_name = current_image;
        String unclipped_file_name = "";
        if (file_name_is_path == false) {
            file_name = temp_dir + key + current_image;
            unclipped_file_name = temp_dir + key + "R" + current_image;
            image_type.put("R" + current_image, image_type.get(current_image));
        }
        if ((file_name.toLowerCase().endsWith(".jpg") == false) & (file_name.toLowerCase().endsWith(".jpeg") == false)) {
            file_name = file_name + ".jpg";
            unclipped_file_name = unclipped_file_name + ".jpg";
        }
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(file_name)));
            JPEGImageEncoder as_jpeg = JPEGCodec.createJPEGEncoder(out);
            as_jpeg.encode(image);
            tempFileNames.put(file_name, "#");
            out.close();
            image_written = true;
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " writing image " + image + " as " + file_name);
        }
        if (image_written == false) {
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(file_name)));
                JPEGImageEncoder as_jpeg = JPEGCodec.createJPEGEncoder(out);
                as_jpeg.encode(image);
                tempFileNames.put(file_name, "#");
                out.close();
            } catch (Exception e) {
                was_error = true;
                LogWriter.writeLog("Exception " + e + " writing image " + image);
            }
        }
        if (save_unclipped == true) {
            saveCopy(file_name, unclipped_file_name);
            tempFileNames.put(unclipped_file_name, "#");
        }
        return was_error;
    }

    /**
	 * load a image when required and remove from store
	 */
    private final synchronized BufferedImage loadStoredImage(String current_image, String ending) {
        String file_name = temp_dir + key + current_image + ending;
        BufferedImage image = null;
        if (JAIHelper.isJAIused()) {
            try {
                JAIHelper.confirmJAIOnClasspath();
                image = javax.media.jai.JAI.create("fileload", file_name).getAsBufferedImage();
            } catch (Exception e) {
            }
        } else {
            try {
                image = ImageIO.read(new File(file_name));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }

    /**
	 * save copy
	 */
    private final void saveCopy(String source, String destination) {
        BufferedInputStream from = null;
        BufferedOutputStream to = null;
        try {
            from = new BufferedInputStream(new FileInputStream(source));
            to = new BufferedOutputStream(new FileOutputStream(destination));
            byte[] buffer = new byte[65535];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " copying file");
        }
        try {
            to.close();
            from.close();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " closing files");
        }
        to = null;
        from = null;
    }

    /**
	 * save copy
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public final void saveAsCopy(String current_image, String destination) {
        BufferedInputStream from = null;
        BufferedOutputStream to = null;
        String source = temp_dir + key + current_image;
        try {
            from = new BufferedInputStream(new FileInputStream(source));
            to = new BufferedOutputStream(new FileOutputStream(destination));
            byte[] buffer = new byte[65535];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " copying file");
        }
        try {
            to.close();
            from.close();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " closing files");
        }
    }

    /**
	 * save copy
	 * 
	 * Converted to avoid threading issues, if this causes any problems, 
 	  * please raise them in our forums, http://www.jpedal.org/phpBB2/index.php
	 */
    public static final void copy(String source, String destination) {
        BufferedInputStream from = null;
        BufferedOutputStream to = null;
        try {
            from = new BufferedInputStream(new FileInputStream(source));
            to = new BufferedOutputStream(new FileOutputStream(destination));
            byte[] buffer = new byte[65535];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " copying file");
        }
        try {
            to.close();
            from.close();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " closing files");
        }
    }

    /**
	 * load a image when required and remove from store
	 */
    private final synchronized BufferedImage loadStoredJPEGImage(String current_image) {
        String file_name = temp_dir + key + current_image + ".jpg";
        BufferedImage image = null;
        File a = new File(file_name);
        if (a.exists()) {
            try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file_name));
                JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
                image = decoder.decodeAsBufferedImage();
                in.close();
            } catch (Exception e) {
                LogWriter.writeLog("Exception " + e + " loading " + current_image);
            }
        } else image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        return image;
    }

    /**
	 * save buffered image
	 */
    private final synchronized boolean saveStoredImage(String format, String ending1, String ending2, String current_image, BufferedImage image, boolean file_name_is_path, boolean save_unclipped) {
        boolean was_error = false;
        String file_name = current_image;
        String unclipped_file_name = "";
        if (file_name_is_path == false) {
            file_name = temp_dir + key + current_image;
            unclipped_file_name = temp_dir + key + "R" + current_image;
            image_type.put("R" + current_image, image_type.get(current_image));
        }
        if ((file_name.toLowerCase().endsWith(ending1) == false) & (file_name.toLowerCase().endsWith(ending2) == false)) {
            file_name = file_name + ending1;
            unclipped_file_name = unclipped_file_name + ending1;
        }
        try {
            if (JAIHelper.isJAIused()) {
                JAIHelper.confirmJAIOnClasspath();
                javax.media.jai.JAI.create("filestore", image, file_name, format);
            } else {
                ImageIO.setUseCache(false);
                FileOutputStream fos = new FileOutputStream(file_name);
                if (format.equals("TIFF")) ImageIO.write(image, "png", fos); else ImageIO.write(image, format, fos);
                fos.flush();
                fos.close();
            }
            tempFileNames.put(file_name, "#");
        } catch (Exception e) {
            e.printStackTrace();
            LogWriter.writeLog(" Exception " + e + " writing image " + image + " with type " + image.getType());
            was_error = true;
        } catch (Error ee) {
            LogWriter.writeLog("Error " + ee + " writing image " + image + " with type " + image.getType());
            was_error = true;
            image = null;
            System.gc();
        }
        if (save_unclipped == true) {
            saveCopy(file_name, unclipped_file_name);
            tempFileNames.put(unclipped_file_name, "#");
        }
        return was_error;
    }

    /**
	 * delete all cached pages
	 */
    public static void flushPages() {
        Iterator filesTodelete = pagesOnDisk.keySet().iterator();
        while (filesTodelete.hasNext()) {
            Object file = filesTodelete.next();
            if (file != null) {
                File delete_file = new File((String) pagesOnDisk.get(file));
                delete_file.delete();
            }
        }
        pagesOnDisk.clear();
        filesTodelete = pagesOnDiskAsBytes.keySet().iterator();
        while (filesTodelete.hasNext()) {
            Object file = filesTodelete.next();
            if (file != null) {
                File delete_file = new File((String) pagesOnDiskAsBytes.get(file));
                delete_file.delete();
            }
        }
        pagesOnDiskAsBytes.clear();
        if (debugCache) System.out.println("Flush cache ");
    }

    public static DynamicVectorRenderer getCachedPage(Integer key) {
        DynamicVectorRenderer currentDisplay = null;
        Object cachedFile = pagesOnDisk.get(key);
        if (debugCache) System.out.println("read from cache " + currentDisplay);
        if (cachedFile != null) {
            BufferedInputStream from = null;
            try {
                File fis = new File((String) cachedFile);
                from = new BufferedInputStream(new FileInputStream(fis));
                byte[] data = new byte[(int) fis.length()];
                from.read(data);
                from.close();
                currentDisplay = new DynamicVectorRenderer(data, new HashMap());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return currentDisplay;
    }

    public static void cachePage(Integer key, DynamicVectorRenderer currentDisplay) {
        try {
            File ff = File.createTempFile("page", ".bin");
            BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(ff));
            to.write(currentDisplay.serializeToByteArray(null));
            to.flush();
            to.close();
            pagesOnDisk.put(key, ff.getAbsolutePath());
            if (debugCache) System.out.println("save to cache " + key + " " + ff.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getCachedPageAsBytes(String key) {
        byte[] data = null;
        Object cachedFile = pagesOnDiskAsBytes.get(key);
        if (cachedFile != null) {
            BufferedInputStream from = null;
            try {
                File fis = new File((String) cachedFile);
                from = new BufferedInputStream(new FileInputStream(fis));
                data = new byte[(int) fis.length()];
                from.read(data);
                from.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public static void cachePageAsBytes(String key, byte[] bytes) {
        try {
            File ff = File.createTempFile("bytes", ".bin");
            BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(ff));
            to.write(bytes);
            to.flush();
            to.close();
            pagesOnDiskAsBytes.put(key, ff.getAbsolutePath());
            if (debugCache) System.out.println("save to cache " + key + " " + ff.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e);
        }
    }
}
