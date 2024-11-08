package edu.csula.coolstatela.action;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.opensymphony.xwork2.ActionSupport;
import edu.csula.coolstatela.model.Entry;
import edu.csula.coolstatela.model.User;
import edu.csula.coolstatela.search.fts.Indexer;
import edu.csula.coolstatela.service.EntryService;

public class AddEntryAction extends ActionSupport {

    private static final Logger logger = Logger.getLogger(AddEntryAction.class);

    private EntryService entryService;

    private Indexer indexer;

    private String name;

    private String author;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private static final String UPLOAD_FOLDER = "c:/upload/";

    public String save() throws Exception {
        User user = new User();
        user.setId(new Integer(1));
        Entry entry = new Entry(name, author, user);
        File destFile = new File(getDestFile());
        try {
            FileUtils.copyFile(upload, destFile);
            edu.csula.coolstatela.model.File fileSpecs = new edu.csula.coolstatela.model.File();
            fileSpecs.setName(uploadFileName);
            fileSpecs.setType(uploadContentType);
            fileSpecs.setOwner(user);
            fileSpecs.setSize(upload.length());
            entry.setOwner(user);
            entry.setFile(fileSpecs);
            entryService.saveEntry(entry);
            indexer.index(entry, uploadContentType);
            logger.info("Success!");
        } catch (IOException ioe) {
            logger.error("IOException copyFile() ", ioe);
            return INPUT;
        } catch (Exception e) {
            logger.error("Exception e ", e);
            return INPUT;
        }
        return SUCCESS;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public void setIndexer(Indexer indexer) {
        this.indexer = indexer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getDestFile() {
        return UPLOAD_FOLDER + uploadFileName;
    }

    public void setEntryService(EntryService entryService) {
        this.entryService = entryService;
    }
}
