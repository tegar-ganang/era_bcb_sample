package com.kwoksys.biz.files;

import com.kwoksys.action.files.FileUploadForm;
import com.kwoksys.biz.files.dao.FileDao;
import com.kwoksys.biz.files.dto.File;
import com.kwoksys.framework.configs.ConfigManager;
import com.kwoksys.framework.connection.database.QueryBits;
import com.kwoksys.framework.exception.DatabaseException;
import com.kwoksys.framework.exception.ObjectNotFoundException;
import com.kwoksys.framework.system.RequestContext;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.FormFile;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FileServiceImpl
 */
public class FileServiceImpl implements FileService {

    private static final Logger logger = Logger.getLogger(FileServiceImpl.class.getName());

    private static final FileServiceImpl INSTANCE = new FileServiceImpl();

    private FileServiceImpl() {
    }

    public static FileServiceImpl getInstance() {
        return INSTANCE;
    }

    public boolean isDirectoryExist(String directoryPath) {
        java.io.File file = new java.io.File(directoryPath);
        return file.isDirectory();
    }

    public boolean isFileExist(String filePath) {
        java.io.File file = new java.io.File(filePath);
        return file.isFile();
    }

    public List getFiles(QueryBits query, Integer objectTypeId, Integer objectId) throws DatabaseException {
        FileDao fileDao = new FileDao();
        return fileDao.getList(query, objectTypeId, objectId);
    }

    public File getFile(Integer objectTypeId, Integer objectId, Integer fileId) throws DatabaseException, ObjectNotFoundException {
        FileDao fileDao = new FileDao();
        return fileDao.getFile(objectTypeId, objectId, fileId);
    }

    /**
     * Will be moved to FileService class
     * @param actionForm
     * @return
     * @throws DatabaseException
     */
    private ActionMessages upload(File file, FileUploadForm actionForm) throws DatabaseException {
        ActionMessages errors = new ActionMessages();
        FormFile file0 = actionForm.getFile0();
        if (file0 == null) {
            errors.add("fileUpload", new ActionMessage("files.error.fileUpload"));
            return errors;
        }
        file.setLogicalName(file0.getFileName().trim());
        file.setSize(file0.getFileSize());
        file.setMimeType(file0.getContentType());
        if (file.getLogicalName().isEmpty()) {
            errors.add("emptyFilePath", new ActionMessage("files.error.emptyFilePath"));
            return errors;
        }
        if (file.getSize() > ConfigManager.file.getMaxFileUploadSize() || file.getSize() < 0) {
            errors.add("fileMaxSize", new ActionMessage("files.error.fileMaxSize"));
            return errors;
        }
        InputStream input = null;
        FileOutputStream output = null;
        FileDao fileDao = new FileDao();
        try {
            input = file0.getInputStream();
            errors = fileDao.add(file);
            if (!errors.isEmpty()) {
                return errors;
            }
            java.io.File uploadFile = new java.io.File(file.getConfigRepositoryPath(), file.getConfigUploadedFilePrefix() + file.getId());
            if (uploadFile.exists()) {
                throw new Exception("File already exists");
            }
            output = new FileOutputStream(uploadFile);
            byte[] bytes = new byte[1024 * 1024];
            while (true) {
                int b = input.read(bytes);
                if (b < 0) {
                    break;
                }
                output.write(bytes, 0, b);
                output.flush();
            }
        } catch (Throwable t) {
            fileDao.deleteNew(file.getId());
            errors.add("fileUpload", new ActionMessage("files.error.fileUpload"));
            logger.log(Level.SEVERE, "Problem writing file to repository. " + "Original file name: " + file.getLogicalName() + ". Physical file name: " + file.getConfigUploadedFilePrefix() + file.getId() + ". File repository: " + file.getConfigRepositoryPath(), t);
        } finally {
            close(input);
            close(output);
        }
        return errors;
    }

    /**
     * Will be moved to FileService class
     * @param request
     * @param actionForm
     * @return
     * @throws DatabaseException
     */
    public ActionMessages addFile(RequestContext requestContext, File file, FileUploadForm actionForm) throws DatabaseException {
        ActionMessages errors = upload(file, actionForm);
        if (!errors.isEmpty()) {
            return errors;
        }
        file.setTitle(requestContext.getRequest().getParameter("fileName0"));
        file.setFileuploadedFileName(file.getConfigUploadedFilePrefix() + file.getId());
        FileDao fileDao = new FileDao();
        errors = fileDao.update(file);
        if (!errors.isEmpty()) {
            return errors;
        }
        return errors;
    }

    /**
     * Instead of deleting the file, we'll rename it.
     * @param repositoryPath
     * @param uploadedFileName
     */
    private void deletePhysicalFile(String repositoryPath, String uploadedFileName) {
        java.io.File delfile = new java.io.File(repositoryPath, uploadedFileName);
        delfile.renameTo(new java.io.File(repositoryPath, ConfigManager.file.getDeleteFilePrefix() + uploadedFileName));
    }

    /**
     * First gather objectTypeId, objectId, fileId.
     * Query the database to see if there is such record.
     * If it cannot find any record, bail out and don't do anything, cuz I really don't know what to do.
     * If it can find a record, great, delete the database record first.
     * Then, delete the actual file according to the file repository path, and uploaded file name.
     *
     * @return ..
     */
    public ActionMessages deleteFile(File file) throws DatabaseException {
        FileDao fileDao = new FileDao();
        ActionMessages errors = fileDao.delete(file);
        if (!errors.isEmpty()) {
            return errors;
        }
        deletePhysicalFile(file.getConfigRepositoryPath(), file.getPhysicalName());
        return errors;
    }

    /**
     * This would accept a list of files to delete.
     */
    public void bulkDelete(String configRepositoryPath, List<File> fileList) {
        for (File file : fileList) {
            deletePhysicalFile(configRepositoryPath, file.getPhysicalName());
        }
    }

    public void download(HttpServletResponse response, File file) throws IOException {
        response.setContentType(file.getMimeType());
        response.setContentLength(file.getSize());
        response.setHeader("Content-Disposition", "filename=\"" + file.getLogicalName() + "\"");
        try {
            java.io.File downloadFile = new java.io.File(file.getConfigRepositoryPath(), file.getPhysicalName());
            FileInputStream input = new FileInputStream(downloadFile);
            ServletOutputStream output = response.getOutputStream();
            byte[] buffer = new byte[10240];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            close(input);
            close(output);
        } catch (FileNotFoundException e) {
            logger.warning("Problem downloading a file: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
            }
        }
    }
}
