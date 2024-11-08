package org.fulworx.core.rest.restlet.representation;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fulworx.core.rest.EntityRepresentation;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Allow access of File as a restful resource.  Using the Apache FileUpload utility, retrieve the
 * DiskFileItems representing the temporarily stored file parts.
 *
 * @version $Id: $
 */
public class FileUploadEntityRepresentation extends FileRepresentation implements EntityRepresentation {

    private static final Log LOG = LogFactory.getLog(FileUploadEntityRepresentation.class);

    private RestletFileUpload restletFileUpload;

    private Representation inputRepresentation;

    private List<File> files;

    public FileUploadEntityRepresentation(RestletFileUpload restletFileUpload, Representation inputRepresentation) {
        super("", MediaType.MULTIPART_ALL, 0);
        this.restletFileUpload = restletFileUpload;
        this.inputRepresentation = inputRepresentation;
        files = new ArrayList<File>();
    }

    /**
     * Leverage the RestletFileUpload (a decoration of apache FileUpload) to save multipart
     * request file items off to the disk in a temporary fashion.
     *
     * @return an internal entity (File)
     */
    public Object getElement() {
        File retFile = null;
        if (inputRepresentation != null) {
            try {
                readFiles();
            } catch (FileUploadException e) {
                LOG.error("Unable to parse incoming multipart file request", e);
            }
            if (files.size() > 0) {
                retFile = files.get(0);
            } else {
                LOG.warn("No files parsed during file upload invocation");
            }
        }
        return retFile;
    }

    private void readFiles() throws FileUploadException {
        List<FileItem> fileItems;
        fileItems = restletFileUpload.parseRepresentation(inputRepresentation);
        for (FileItem fileItem : fileItems) {
            if (fileItem instanceof DiskFileItem) {
                DiskFileItem diskFileItem = (DiskFileItem) fileItem;
                files.add(diskFileItem.getStoreLocation());
            }
        }
    }

    @Override
    public FileChannel getChannel() throws IOException {
        if (inputRepresentation instanceof FileRepresentation) {
            return (FileChannel) inputRepresentation.getChannel();
        }
        return null;
    }

    @Override
    public long getSize() {
        return inputRepresentation.getSize();
    }

    @Override
    public FileInputStream getStream() throws IOException {
        if (inputRepresentation instanceof FileRepresentation) {
            return (FileInputStream) inputRepresentation.getStream();
        }
        return null;
    }

    @Override
    public String getText() throws IOException {
        return inputRepresentation.getText();
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        inputRepresentation.write(outputStream);
    }

    @Override
    public void write(WritableByteChannel writableChannel) throws IOException {
        inputRepresentation.write(writableChannel);
    }
}
