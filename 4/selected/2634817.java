package edu.csula.coolstatela.action;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;
import edu.csula.coolstatela.model.Entry;
import edu.csula.coolstatela.model.User;
import edu.csula.coolstatela.search.fts.Indexer;
import edu.csula.coolstatela.service.EntryService;
import edu.csula.coolstatela.service.UserService;

public class EditEntryAction extends ActionSupport {

    private EntryService entryService;

    private UserService userService;

    private Indexer indexer;

    private Integer id;

    private String name;

    private String author;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private String fileName;

    private Entry entry = new Entry();

    private static final String UPLOAD_FOLDER = "c:/upload/";

    private static final String PLAIN_TEXT = "TXT";

    private static final String TEXT_HTML = "TXT_HTML";

    private static final String PDF = "PDF";

    private static final String MSWORD = "MSWORD";

    private static final String RTF = "RTF";

    private static Logger logger = Logger.getLogger(EditEntryAction.class);

    public String showEntry() {
        logger.info("showEntry() id " + id);
        entry = entryService.getEntryById(id);
        fileName = entry.getFile().getName();
        return SUCCESS;
    }

    public String edit() {
        User user = new User();
        user.setId(new Integer(1));
        boolean reindex = false;
        logger.info("edit() id " + id);
        entry = entryService.getEntryById(id);
        if (!entry.getName().equals(name) || !entry.getAuthor().equals(author)) {
            entry.setName(name);
            entry.setAuthor(author);
            try {
                if (upload != null) {
                    File destFile = new File(getDestFile());
                    FileUtils.copyFile(upload, destFile);
                    edu.csula.coolstatela.model.File fileSpecs = new edu.csula.coolstatela.model.File();
                    fileSpecs.setName(uploadFileName);
                    fileSpecs.setType(uploadContentType);
                    fileSpecs.setOwner(user);
                    fileSpecs.setSize(upload.length());
                    entry.setOwner(user);
                    entry.setFile(fileSpecs);
                }
                entryService.saveEntry(entry);
                reindex = true;
            } catch (IOException ioe) {
                logger.error("EditEntryAction, editEntry ", ioe);
            }
            reindex = true;
        }
        if (reindex) {
            indexer.delete(entry);
            logger.info("Finish deleting index");
            if (uploadContentType != null && uploadContentType.length() > 0) {
                indexer.index(entry, uploadContentType);
            } else {
                indexer.index(entry, entry.getFile().getType());
            }
            logger.info("Finish reindexing");
        }
        return Action.SUCCESS;
    }

    public String getDestFile() {
        return UPLOAD_FOLDER + uploadFileName;
    }

    public void setEntryService(EntryService entryService) {
        this.entryService = entryService;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setIndexer(Indexer indexer) {
        this.indexer = indexer;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
