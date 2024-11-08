package com.icteam.fiji.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.icteam.fiji.defaults.ServerDefaults;

public class FileManager {

    public static final Log logger = LogFactory.getLog(FileManager.class);

    private static FileManager s_instance;

    private File m_rootDir = ServerDefaults.getInstance().getDefaultUploadRoot();

    public static FileManager getInstance() {
        if (s_instance == null) s_instance = new FileManager();
        return s_instance;
    }

    private FileManager() {
    }

    public File createFile(String p_file) {
        logger.debug("Creating file under " + m_rootDir.getAbsolutePath() + " with name " + p_file);
        return new File(m_rootDir, p_file);
    }

    public File createFile(String p_fileName, InputStream p_is, boolean overwrite) {
        logger.debug("Creating file under " + m_rootDir.getAbsolutePath());
        File storedFile = new File(m_rootDir, p_fileName);
        return createFile(storedFile, p_is, overwrite);
    }

    private File createFile(File p_storedFile, InputStream p_is, boolean overwrite) {
        OutputStream fileStream = null;
        try {
            logger.debug("Filename to create=" + p_storedFile.getAbsolutePath());
            if (!p_storedFile.createNewFile() && !overwrite) {
                logger.info("Unable to create file " + p_storedFile.toString());
                return null;
            }
            fileStream = new FileOutputStream(p_storedFile, false);
            byte[] buff = new byte[2048];
            int read;
            while ((read = p_is.read(buff)) > 0) {
                fileStream.write(buff, 0, read);
            }
            fileStream.flush();
            return p_storedFile;
        } catch (FileNotFoundException e) {
            logger.debug(e);
            deleteFile(p_storedFile);
        } catch (IOException e) {
            logger.debug(e);
            deleteFile(p_storedFile);
        } finally {
            try {
                if (fileStream != null) fileStream.close();
            } catch (IOException e) {
                logger.debug(e);
            }
        }
        return null;
    }

    public InputStream getFileContent(String p_uri) throws URISyntaxException, FileNotFoundException {
        File file = new File(new URI(p_uri));
        return new FileInputStream(file);
    }

    public void deleteFile(String p_uri) throws URISyntaxException {
        deleteFile(new File(new URI(p_uri)));
    }

    public InputStream getFileContent(URI p_uri) throws FileNotFoundException {
        File file = new File(p_uri);
        return new FileInputStream(file);
    }

    public void deleteFile(URI p_uri) {
        deleteFile(new File(p_uri));
    }

    public void deleteFile(File p_file) {
        if (p_file != null && p_file.exists()) {
            boolean deleted = p_file.delete();
            if (!deleted) logger.info("Unable to delete file " + p_file.toString());
        }
    }

    public File moveFile(File p_file, String p_destDirRelativeToRoot) throws IOException {
        return moveFile(m_rootDir, p_file, p_destDirRelativeToRoot);
    }

    public File moveFile(File p_src, File p_destDir) throws IOException {
        return copyFile(p_src, p_destDir, true);
    }

    private File moveFile(File rootDir, File p_file, String p_destDirRelativeToRoot) throws IOException {
        if (p_file == null) return null;
        File newDir = new File(rootDir, p_destDirRelativeToRoot);
        if (!newDir.exists()) {
            if (!newDir.mkdir()) throw new IOException("Unable to create directory " + newDir.toString());
        }
        return copyFile(p_file, new File(rootDir, p_destDirRelativeToRoot), true);
    }

    private File copyFile(File p_src, File p_destDir, boolean overwrite) throws FileNotFoundException {
        FileInputStream is = null;
        File f = null;
        try {
            is = new FileInputStream(p_src);
            f = createFile(new File(p_destDir, p_src.getName()), is, overwrite);
        } catch (IOException e) {
            logger.debug(e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                logger.debug(e);
            }
        }
        deleteFile(p_src);
        return f;
    }
}
