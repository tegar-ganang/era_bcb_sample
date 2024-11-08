package org.slizardo.beobachter.gui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.slizardo.beobachter.resources.languages.Translator;

public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    public static void isReadable(File file) throws Exception {
        if (!file.exists()) {
            throw new Exception(Translator.t("The_file_doesnt_exists"));
        }
        if (file.isDirectory()) {
            throw new Exception(Translator.t("Cannot_open_a_directory"));
        }
        if (!file.canRead()) {
            throw new Exception(Translator.t("You_dont_have_permission_to_read_this_file"));
        }
    }

    public static boolean copy(InputStream is, File file) {
        try {
            IOUtils.copy(is, new FileOutputStream(file));
            return true;
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return false;
        }
    }
}
