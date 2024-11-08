package targetanalyzer.core.data.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import org.jfree.data.time.Day;
import targetanalyzer.core.data.Gun;
import targetanalyzer.core.data.Target;

/**
 * This class is intended for storing / archiving files.
 * 
 * @author Ruediger Gad
 * 
 */
public class Archiver {

    public static String baseDir = System.getProperty("osgi.instance.area").substring(5);

    public String archiveTargetImage(String oldFilename, Gun gun, Date date, long dateId) {
        String imageName = new Day(date) + "_" + dateId + ".jpg";
        String newDirname = this.getGunDirName(gun);
        String newFilename = newDirname + imageName;
        File newDir = new File(newDirname);
        newDir.mkdirs();
        File newFile = new File(newFilename);
        File oldFile = new File(oldFilename);
        try {
            this.copy(oldFile, newFile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return newFilename;
    }

    private void copy(File source, File target) throws IOException {
        FileChannel in = (new FileInputStream(source)).getChannel();
        FileChannel out = (new FileOutputStream(target)).getChannel();
        in.transferTo(0, source.length(), out);
        in.close();
        out.close();
    }

    public void deleteTargetImage(Target t) throws FileNotFoundException {
        String filename = t.getImageFilename();
        this.delete(filename);
    }

    /**
	 * Delete directory of a gun.<br>
	 * It is assumed that the gun doesnt have any target entries left.
	 * 
	 * @param g
	 * @return
	 */
    public void deleteGunDir(Gun g) throws FileNotFoundException {
        String dirname = this.getGunDirName(g);
        this.delete(dirname);
    }

    /**
	 * Get directory name for a gun.
	 * 
	 * @param g
	 * @return
	 */
    private String getGunDirName(Gun g) {
        String imageDir = Archiver.baseDir + "image_data";
        String gunSubdir = g.getManufacturer() + "_" + g.getModelName();
        return imageDir + File.separatorChar + gunSubdir + File.separatorChar;
    }

    /**
	 * Delete a file or an empty(!) directory.
	 * 
	 * @param name
	 * @throws FileNotFoundException
	 */
    private void delete(String name) throws FileNotFoundException {
        File file = new File(name);
        boolean success = false;
        if (file.canWrite()) {
            success = file.delete();
        } else if (file.exists()) {
            throw new FileNotFoundException("File " + name + " not writable!");
        }
        if (!success && file.exists()) {
            throw new FileNotFoundException("Errors occured during deletion process!");
        }
    }
}
