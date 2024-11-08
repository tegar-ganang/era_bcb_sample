package applications;

import java.io.File;
import java.io.IOException;
import mmt_imagingapi.mmt_image.FileImageReader;
import mmt_imagingapi.mmt_image.FileImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ImageAPIConstants;

/**
 *
 * @author fhs33961
 */
public class Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        File srcDir = null;
        if (DEBUG) {
            srcDir = new File("D:\\SoftwareTests\\TestImages\\Test_Images\\Test_Images\\Enhancement");
        } else {
            if (args.length == 1) {
                srcDir = new File(args[0]);
            } else {
                exitWithError();
            }
        }
        if (!srcDir.isDirectory()) {
            exitWithError();
        }
        File[] files = srcDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            LOGGER.info("Processing File " + files[i].getName());
            try {
                FileImageWriter.write(FileImageReader.read(files[i].getAbsolutePath()), ((files[i].getAbsolutePath().split(ImageAPIConstants.DOT_CHARACTER_REGEX)[0]) + "_converted"));
            } catch (IOException ex) {
                LOGGER.error("Exception occurred", ex);
            }
        }
    }

    public static void exitWithError() {
        System.exit(-1);
    }

    public static void convert(File f) {
    }
}
