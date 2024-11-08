package uk.ac.ed.rapid.symbol.impl;

import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileType;
import uk.ac.ed.rapid.data.RapidData;
import uk.ac.ed.rapid.data.filesystem.AbstractFileSystem;
import uk.ac.ed.rapid.data.filesystem.FileSystemConnector;
import uk.ac.ed.rapid.data.filesystem.FileSystemTable;
import uk.ac.ed.rapid.exception.RapidException;
import uk.ac.ed.rapid.jobdata.JobData;
import uk.ac.ed.rapid.jobdata.VariableAnalysis;
import uk.ac.ed.rapid.jobdata.VariableResolver;
import uk.ac.ed.rapid.value.Value;
import uk.ac.ed.rapid.value.impl.SingleValue;

/**
 *
 * @author jos
 */
public class FileUploadSymbol extends SymbolImpl {

    private String fileSystemName = "";

    private String path = "";

    private Value cache = new SingleValue("");

    public FileUploadSymbol() {
        this.setReadOnly(true);
    }

    public void setFileSystemName(String fileSystemName) {
        this.fileSystemName = fileSystemName;
    }

    public String getFileSystemName() {
        return fileSystemName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Value get(JobData jobData) {
        return cache;
    }

    @Override
    public void put(RapidData data, FileItem item) throws RapidException {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        FileObject fileObject = null;
        JobData jobData = data.getJobData();
        FileSystemTable fileSystemTable = data.getFileSystemTable();
        try {
            if (item.isFormField()) throw new RapidException("Item " + item.getFieldName() + " is not a File at symbol " + this.getVariableName());
            if (!"".equals(item.getName())) {
                if (!"".equals(this.getVariableName())) VariableResolver.getVariable(this.getVariableName(), jobData).put(item.getName());
                String resolvedFileSystem = VariableResolver.resolve(this.fileSystemName, jobData);
                AbstractFileSystem fileSystem = fileSystemTable.getFileSystem(resolvedFileSystem);
                fileObject = fileSystemTable.getFileSystemConnector().connect(fileSystem, jobData, 0);
                String resolvedPath = VariableResolver.resolve(this.path, jobData);
                fileObject = fileObject.resolveFile(resolvedPath);
                if (fileObject.exists() && fileObject.getType() == FileType.FOLDER) {
                    fileObject = fileObject.resolveFile(item.getName());
                    fileObject.createFile();
                } else if (fileObject.exists() && fileObject.getType() != FileType.FOLDER) {
                    fileObject.delete();
                    fileObject.createFile();
                } else {
                    fileObject.createFile();
                }
                outputStream = fileObject.getContent().getOutputStream();
                inputStream = item.getInputStream();
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, size);
            }
        } catch (Exception ex) {
            throw new RapidException("Unable to write file to " + this.fileSystemName + " : " + this.path + " cause: " + ex.getMessage());
        } finally {
            try {
                if (fileObject != null) fileObject.close();
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception ex) {
                throw new RapidException("Unable to close file " + this.fileSystemName + " : " + this.path + " cause: " + ex.getMessage());
            }
        }
    }

    public String toString() {
        return "\nFILE UPLOAD\nDestination \n" + this.fileSystemName + " : " + this.path + "\n" + super.toString();
    }
}
