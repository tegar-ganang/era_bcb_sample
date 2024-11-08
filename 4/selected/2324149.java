package net.sf.jpkgmk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import net.sf.jpkgmk.util.StreamUtil;
import net.sf.jpkgmk.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of {@link FileHandler}.
 * @author gommma (gommma AT users.sourceforge.net)
 * @author Last changed by: $Author: gommma $
 * @version $Revision: 2 $ $Date: 2008-08-20 15:14:19 -0400 (Wed, 20 Aug 2008) $
 * @since 1.0
 */
public class DefaultFileHandler implements FileHandler {

    private File file;

    private Log log = LogFactory.getLog(DefaultFileHandler.class);

    /**
	 * @param basedir The directory where the file should be created.
	 * @param filename
	 */
    public DefaultFileHandler(File basedir, String filename) {
        initialize(basedir, filename);
    }

    private void initialize(File basedir, String filename) {
        if (basedir == null) {
            throw new NullPointerException("The parameter 'basedir' must not be null");
        }
        if (!basedir.isDirectory()) {
            throw new IllegalArgumentException("The basedir '" + basedir + "' must be a directory");
        }
        if (StringUtil.isNullOrEmpty(filename)) {
            throw new NullPointerException("The parameter 'filename' must not be null");
        }
        File targetFile = new File(basedir, filename);
        this.file = targetFile;
    }

    public File getBasedir() {
        return file.getParentFile();
    }

    public File getFile() {
        return file;
    }

    public void create(ContentWriter contentWriter) throws IOException {
        File targetFile = this.getFile();
        if (targetFile.exists()) {
            String message = "The file '" + targetFile.getAbsolutePath() + "' already exists. Will not overwrite it.";
            throw new PackageException(message);
        }
        OutputStream outputStream = new FileOutputStream(targetFile);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            contentWriter.writeContent(writer);
            writer.flush();
        } finally {
            StreamUtil.tryCloseStream(outputStream);
        }
        log.info("Wrote file '" + targetFile.getAbsolutePath() + "'");
    }

    /**
	 * @see net.sf.jpkgmk.FileHandler#clean()
	 */
    public void clean() {
        File file = getFile();
        deleteFile(file);
    }

    /**
	 * delete configuration files for pkgs. If deletion did not succeed, a {@link PackageException} is thrown.
	 * @param file The file to be deleted
	 * @throws PackageException if the file could not be deleted
	 */
    private void deleteFile(File file) {
        log.info("Deleting file " + file);
        if (!file.exists()) {
            log.debug("File '" + file.getAbsolutePath() + "' does not exist. Nothing to delete");
            return;
        }
        boolean success = file.delete();
        if (!success) {
            throw new PackageException("Could not delete file '" + file + "'.");
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + "file=" + this.file + "]";
    }
}
