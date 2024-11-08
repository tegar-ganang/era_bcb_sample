package org.openuss.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * @author Ingo Dueppe
 * @see org.openuss.repository.RepositoryService
 */
public class RepositoryServiceImpl extends RepositoryServiceBase {

    private static final Logger logger = Logger.getLogger(RepositoryServiceImpl.class);

    private String path;

    public RepositoryServiceImpl() {
        String tmpPath = System.getProperty("java.io.tmpdir", "./");
        setRepositoryLocation(tmpPath + "/plexus/");
    }

    @Override
    protected InputStream handleLoadContent(Long fileId) throws Exception {
        Validate.notNull(fileId, "Parameter fileId must not be null.");
        RepositoryFile file = getRepositoryFileDao().load(fileId);
        if (file == null) {
            logger.error("File with id " + fileId + " not found.");
            throw new RepositoryServiceException("File with id " + fileId + " not found.");
        }
        getRepositoryFileDao().update(file);
        return fetchInputStream(file);
    }

    private InputStream fetchInputStream(RepositoryFile file) throws FileNotFoundException, IOException {
        long fileId = file.getId();
        File cachedFile = cachedFile(fileId);
        if (!cachedFile.exists()) {
            refreshCacheFile(file, cachedFile);
        } else {
            if (isCachedFileInvalide(file, cachedFile)) {
                if (cachedFile.delete()) {
                    refreshCacheFile(file, cachedFile);
                } else {
                    logger.warn("Could not delete cached file " + cachedFile.getName());
                    return file.getInputStream();
                }
            }
        }
        return new FileInputStream(cachedFile);
    }

    private boolean isCachedFileInvalide(RepositoryFile file, File cachedFile) {
        try {
            return !FileUtils.isFileNewer(cachedFile, file.getModified()) || file.getContent().length() == cachedFile.length();
        } catch (SQLException e) {
            logger.warn(e);
            return true;
        }
    }

    @Override
    protected void handleRemoveContent(Long fileId) throws Exception {
        Validate.notNull(fileId, "Parameter fileId must not be null.");
        getRepositoryFileDao().remove(fileId);
        File cachedFile = new File(toFileName(fileId));
        if (cachedFile.exists()) {
            if (!cachedFile.delete()) {
                cachedFile.deleteOnExit();
            }
        }
    }

    private File cachedFile(long fileId) {
        return new File(toFileName(fileId));
    }

    private void refreshCacheFile(RepositoryFile file, File cacheFile) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(cacheFile);
        InputStream is = file.getInputStream();
        int count = IOUtils.copy(is, fos);
        logger.debug("===========================================================> wrote bytes to cache " + count);
        fos.flush();
        IOUtils.closeQuietly(fos);
        IOUtils.closeQuietly(file.getInputStream());
    }

    @Override
    protected void handleSaveContent(Long fileId, InputStream content) throws Exception {
        Validate.notNull(fileId, "Parameter fileId must not be null.");
        Validate.notNull(content, "Parameter content must not be null.");
        RepositoryFile file = getRepositoryFileDao().load(fileId);
        if (file == null) {
            persistNewFile(fileId, content);
        } else {
            persistFile(file, content);
        }
    }

    private void persistFile(RepositoryFile file, InputStream content) {
        file.setModified(new Date());
        file.setInputStream(content);
        getRepositoryFileDao().update(file);
    }

    private void persistNewFile(Long fileId, InputStream content) {
        RepositoryFile file;
        file = new RepositoryFileImpl();
        file.setId(fileId);
        file.setModified(new Date());
        file.setInputStream(content);
        getRepositoryFileDao().create(file);
    }

    @Override
    protected void handleSetRepositoryLocation(String path) throws Exception {
        Validate.notEmpty(path, "RepositoryLocation is not configured!");
        logger.info("set repository path to " + path);
        this.path = path;
        File dirPath = new File(path);
        if (!dirPath.exists()) {
            if (!dirPath.mkdirs()) {
                throw new RepositoryServiceException("Cannot create repository path " + dirPath.getAbsolutePath());
            }
        }
    }

    @Override
    protected String handleGetRepositoryLocation() throws Exception {
        return path;
    }

    private String toFileName(Long fileId) {
        return path + "/_filecontent_" + fileId + ".tmp";
    }
}
