package com.gestioni.adoc.apsadmin.documento.personale;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.gestioni.adoc.aps.system.services.documento.personale.DocumentoPersonale;
import com.gestioni.adoc.aps.system.services.documento.personale.IDocumentoPersonaleManager;
import com.gestioni.adoc.aps.system.services.pec.utils.ContentTypes;
import com.gestioni.adoc.apsadmin.scanner.ScannerAction;

public class DocumentoPersonaleAction extends ScannerAction implements IDocumentoPersonalAction {

    @Override
    public String edit() {
        try {
            if (null == this.getSelectedDocument() || this.getSelectedDocument().trim().length() == 0) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
                return INPUT;
            }
            DocumentoPersonale documentoPersonale = this.getDocumentoPersonaleManager().getDocument(this.getSelectedDocument());
            if (null == documentoPersonale || !documentoPersonale.getAutore().equals(this.getCurrentUser().getUsername())) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
                return INPUT;
            }
            this.setDocumentoPersonale(documentoPersonale);
            this.setStrutsAction(ApsAdminSystemConstants.EDIT);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "edit");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String newDocument() {
        try {
            this.setStrutsAction(ApsAdminSystemConstants.ADD);
            this.getRequest().getSession().removeAttribute(IDocumentoPersonalAction.SESSION_NEWDOCUMENT_MAP);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "newDocument");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String newDocument_2() {
        try {
            if (null == this.getDoc()) {
                this.addActionError(this.getText("Error.documentoPersonale.nullFile"));
                return INPUT;
            } else {
                String pathOutFile = this.getTempDir().concat(this.getDoc().getName());
                FileUtils.copyFile(new File(this.getDoc().getAbsolutePath()), new File(pathOutFile));
                this.setDocTempFilePath(pathOutFile);
                this.getDocumentoPersonale().setStartDate(new Date());
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "newDocument_2");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String newDocumentFromScanner_2() {
        try {
            this.setStrutsAction(ApsAdminSystemConstants.ADD);
            File f = new File(this.getDocTempFilePath() + this.getDocFileName());
            this.setDoc(f);
            this.setTempDir(this.getDocTempFilePath());
            String pathOutFile = this.getTempDir().concat(this.getDoc().getName());
            this.setDocTempFilePath(pathOutFile);
            if (null == this.getDoc()) {
                this.addActionError(this.getText("Error.documentoPersonale.nullFile"));
                return INPUT;
            }
            this.getDocumentoPersonale().setStartDate(new Date());
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "newDocumentFromScanner_2");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String save() {
        try {
            DocumentoPersonale documento = this.getDocumentoPersonale();
            if (this.getStrutsAction() == ApsAdminSystemConstants.ADD) {
                File file = new File(this.getDocTempFilePath());
                this.setDoc(file);
                documento.setAutore(this.getCurrentUser().getUsername());
                documento.setProfilo(this.getIdProfilo());
                this.getDocumentoPersonaleManager().addDocument(this.getProfilo().getId(), documento, this.getDoc(), this.getDocFileName());
                this.addActionMessage(this.getText("Message.documentoPersonale.saved"));
                file.delete();
            } else if (this.getStrutsAction() == ApsAdminSystemConstants.EDIT) {
                this.getDocumentoPersonaleManager().updateDocumentFile(String.valueOf(this.getProfilo().getId()), documento, this.getDoc(), this.getDocFileName());
                this.addActionMessage(this.getText("Message.documentoPersonale.updated"));
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "save");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String fillDocumement() {
        try {
            DocumentoPersonale documento = this.getDocumentoPersonale();
            if (this.getStrutsAction() == ApsAdminSystemConstants.ADD) {
                documento.setAutore(this.getCurrentUser().getUsername());
                documento.setProfilo(this.getIdProfilo());
                Map<String, Object> newDocumentSessionParam = new HashMap<String, Object>();
                newDocumentSessionParam.put(IDocumentoPersonalAction.SESSION_NEWDOCUMENT_METADATA, documento);
                newDocumentSessionParam.put(IDocumentoPersonalAction.SESSION_NEWDOCUMENT_TEMP_FILEPATH_NAME, this.getDocTempFilePath());
                newDocumentSessionParam.put(IDocumentoPersonalAction.SESSION_NEWDOCUMENT_FILENAME, this.getDocFileName());
                this.getRequest().getSession().setAttribute(IDocumentoPersonalAction.SESSION_NEWDOCUMENT_MAP, newDocumentSessionParam);
            } else {
                this.addActionError(this.getText("Error.saveDocumentale.addAction.notFound"));
                return INPUT;
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "save");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String trash() {
        try {
            if (null == this.getSelectedDocument() || this.getSelectedDocument().trim().length() == 0) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
                return INPUT;
            }
            DocumentoPersonale documentoPersonale = this.getDocumentoPersonaleManager().getDocument(this.getSelectedDocument());
            if (null == documentoPersonale || !documentoPersonale.getAutore().equals(this.getCurrentUser().getUsername())) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
                return INPUT;
            }
            this.setDocumentoPersonale(documentoPersonale);
            this.setStrutsAction(ApsAdminSystemConstants.DELETE);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "trash");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String delete() {
        try {
            if (null == this.getSelectedDocument() || this.getSelectedDocument().trim().length() == 0) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
            }
            DocumentoPersonale documentoPersonale = this.getDocumentoPersonaleManager().getDocument(this.getSelectedDocument());
            if (null == documentoPersonale || !documentoPersonale.getAutore().equals(this.getCurrentUser().getUsername())) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
            }
            if (this.getStrutsAction() == ApsAdminSystemConstants.DELETE) {
                this.getDocumentoPersonaleManager().deleteDocument(String.valueOf(this.getProfilo().getId()), this.getSelectedDocument());
                this.addActionMessage(this.getText("Message.documentoPersonale.deleted"));
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "delete");
            return FAILURE;
        }
        return SUCCESS;
    }

    public String download() {
        try {
            if (null == this.getSelectedDocument() || this.getSelectedDocument().trim().length() == 0) {
                this.addActionError(this.getText("Error.documentoPersonale.null"));
            }
            DocumentoPersonale documentoPersonale = this.getDocumentoPersonaleManager().getDocument(this.getSelectedDocument());
            InputStream in = this.getDocumentoPersonaleManager().getDocumentFile(String.valueOf(this.getProfilo().getId()), documentoPersonale.getPuntatoreJackRabbit());
            this.setInputStream(in);
            this.setDocFileName(this.getDocumentoPersonaleManager().getDocumentFileName(String.valueOf(this.getProfilo().getId()), documentoPersonale.getPuntatoreJackRabbit()));
            this.setDocContentType(ContentTypes.getContentTypeByFileName(this.getDocFileName()));
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "download");
            return FAILURE;
        }
        return SUCCESS;
    }

    public String getDocFileName() throws ApsSystemException {
        if (_docFileName == null) {
            DocumentoPersonale documentoPersonale = this.getDocumentoPersonaleManager().getDocument(this.getSelectedDocument());
            this.setDocFileName(this.getDocumentoPersonaleManager().getDocumentFileName(String.valueOf(this.getProfilo().getId()), documentoPersonale.getPuntatoreJackRabbit()));
        }
        return _docFileName;
    }

    public void setDocFileName(String docFileName) {
        this._docFileName = docFileName;
    }

    public void setSelectedDocument(String selectedDocument) {
        this._selectedDocument = selectedDocument;
    }

    public String getSelectedDocument() {
        return _selectedDocument;
    }

    public void setDocumentoPersonale(DocumentoPersonale documentoPersonale) {
        this._documentoPersonale = documentoPersonale;
    }

    public DocumentoPersonale getDocumentoPersonale() {
        return _documentoPersonale;
    }

    public void setStrutsAction(int strutsAction) {
        this._strutsAction = strutsAction;
    }

    public int getStrutsAction() {
        return _strutsAction;
    }

    public void setDoc(File doc) {
        this._doc = doc;
    }

    public File getDoc() {
        return _doc;
    }

    public void setDocContentType(String docContentType) {
        this._docContentType = docContentType;
    }

    public String getDocContentType() {
        return _docContentType;
    }

    public void setDocumentoPersonaleManager(IDocumentoPersonaleManager documentoPersonaleManager) {
        this._documentoPersonaleManager = documentoPersonaleManager;
    }

    protected IDocumentoPersonaleManager getDocumentoPersonaleManager() {
        return _documentoPersonaleManager;
    }

    public InputStream getInputStream() {
        return _inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this._inputStream = inputStream;
    }

    protected String getTempDir() {
        return _tempDir;
    }

    public void setTempDir(String tempDir) {
        this._tempDir = tempDir;
    }

    public String getDocTempFilePath() {
        return _docTempFilePath;
    }

    public void setDocTempFilePath(String docTempFilePath) {
        this._docTempFilePath = docTempFilePath;
    }

    private String _docTempFilePath;

    private InputStream _inputStream;

    private File _doc;

    private String _docFileName;

    private String _docContentType;

    private int _strutsAction;

    private String _tempDir;

    private DocumentoPersonale _documentoPersonale = new DocumentoPersonale();

    private IDocumentoPersonaleManager _documentoPersonaleManager;

    private String _selectedDocument;
}
