package com.gestioni.adoc.apsadmin.protocollo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.gestioni.adoc.aps.system.AdocSystemConstants;
import com.gestioni.adoc.aps.system.services.aoo.Aoo;
import com.gestioni.adoc.aps.system.services.documento.IDocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.inoltro.IInoltroDocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.model.Documento;
import com.gestioni.adoc.aps.system.services.documento.personale.DocumentoPersonale;
import com.gestioni.adoc.aps.system.services.ente.Ente;
import com.gestioni.adoc.aps.system.services.pec.PecManagerIN;
import com.gestioni.adoc.aps.system.services.pec.mail.models.MessageAttachment;
import com.gestioni.adoc.aps.system.services.pec.mail.models.MessageIN;
import com.gestioni.adoc.aps.system.services.pec.mail.models.SmallMessageIN;
import com.gestioni.adoc.aps.system.services.pec.utils.FileUtil;
import com.gestioni.adoc.aps.system.services.profilo.IProfiloManager;
import com.gestioni.adoc.aps.system.services.profilo.Profilo;
import com.gestioni.adoc.aps.system.services.protocollo.IProtocolloIngressoManager;
import com.gestioni.adoc.aps.system.services.protocollo.model.Protocollo;
import com.gestioni.adoc.aps.system.services.protocollo.model.SmallContatto;
import com.gestioni.adoc.aps.system.services.uo.UnitaOrganizzativa;
import com.gestioni.adoc.apsadmin.AdocApsAdminSystemConstants;
import com.opensymphony.xwork2.Action;

public class ProtocolloAction extends AbstractProtocolloAction implements IProtocolloAction {

    private void disableAttributes(Protocollo protocollo) {
    }

    @Override
    public String newProtocollo() {
        this.setStrutsAction(ApsAdminSystemConstants.ADD);
        String out = super.newProtocollo();
        if (out.equals(SUCCESS)) if (this.getMainDocumentOption() != null && this.getMainDocumentOption().equals(MAIN_DOCUMENT_MAIL_OPTION)) {
            out = "mail";
        }
        return out;
    }

    @Override
    public String entrySchedaProtocollo() {
        try {
            if (this.getMainDocumentOption().equals(MAIN_DOCUMENT_MAIL_OPTION)) {
                this.setStrutsAction(ApsAdminSystemConstants.ADD);
            }
            if (this.getFromScanner() == true) {
                File f = new File(this.getTempDir().concat(this.getMainDocFileName()));
                if (f.exists()) {
                    this.setMainDoc(f);
                } else {
                    this.addActionError(this.getText("Error.documentoFromScanner.null"));
                    return INPUT;
                }
            }
            if (this.getStrutsAction() == ApsAdminSystemConstants.ADD) {
            } else if (this.getStrutsAction() == ApsAdminSystemConstants.EDIT) {
                Protocollo protocollo = (Protocollo) this.getApsEntity();
                if (protocollo.getDocumento() != null) {
                    ApsSystemUtils.getLogger().info("inserimento documento allegato per " + protocollo.getId());
                }
            }
            String checkMainDocument = this.checkMainDocument();
            if (null != checkMainDocument) {
                return INPUT;
            }
            this.saveTempMainDocument();
            this.disableAttributes((Protocollo) this.getApsEntity());
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "entrySchedaProtocollo");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String saveSchedaProtocollo() {
        String result = null;
        try {
            Protocollo protocollo = (Protocollo) this.getApsEntity();
            if (this.getStrutsAction() == ApsAdminSystemConstants.ADD) {
                if (protocollo == null) {
                    ApsSystemUtils.getLogger().severe("saveSchedaProtocollo: protocollo null!");
                    return FAILURE;
                }
                SmallContatto mittente = protocollo.getMittente();
                if (null == mittente) {
                    this.addActionError(this.getText("Error.mittente.empty"));
                    return INPUT;
                } else {
                    if ((null == mittente.getRagioneSociale() || mittente.getRagioneSociale().trim().length() == 0) && (null == mittente.getCognome() || mittente.getCognome().trim().length() == 0)) {
                        this.addActionError(this.getText("Error.mittente.RagionesocialeOrCognome.required"));
                        return INPUT;
                    }
                }
                result = Action.SUCCESS;
                if (this.getMainDocumentOption().equals(MAIN_DOCUMENT_NONE_OPTION)) {
                    result = "protProvisorio";
                }
            } else if (this.getStrutsAction() == ApsAdminSystemConstants.EDIT) {
                this.getProtocolloManager().updateProtocollo(protocollo, this.getProfilo().getId());
                this.addActionMessage(this.getText("Message.protocollo.updated"));
                this.getRequest().getSession().removeAttribute(ProtocolloAction.SESSION_PARAM_NAME_CURRENT_PROTOCOLLO);
                result = "successUpdate";
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "saveSchedaProtocollo");
            return FAILURE;
        }
        return result;
    }

    @Override
    public String edit() {
        try {
            this.clearSessionProtocolloAndDocumento();
            Protocollo protocollo = (Protocollo) this.getProtocolloManager().getProtocollo(this.getId());
            if (null == protocollo) {
                this.addActionError(this.getText("Message.protocollo.null"));
                return INPUT;
            }
            this.getRequest().getSession().setAttribute(IProtocolloAction.SESSION_PARAM_NAME_CURRENT_PROTOCOLLO, protocollo);
            if (null == protocollo.getDocumento()) {
                ApsSystemUtils.getLogger().info("Il protocollo " + protocollo.getId() + " non ha un documento.");
            }
            this.setStrutsAction(ApsAdminSystemConstants.EDIT);
            this.disableAttributes(protocollo);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "edit");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String registraProvvisorio() {
        try {
            Protocollo protocollo = (Protocollo) this.getProtocolloManager().getProtocollo(this.getId());
            if (null == protocollo) {
                this.addActionError(this.getText("Message.protocollo.null"));
                return INPUT;
            }
            if (null != protocollo.getDocumento()) {
                this.addActionError(this.getText("Message.protocollo.hadDocument"));
                ApsSystemUtils.getLogger().info("Il protocollo " + protocollo.getId() + " ha già un documento.");
                return INPUT;
            }
            this.getRequest().getSession().setAttribute(IProtocolloAction.SESSION_PARAM_NAME_CURRENT_PROTOCOLLO, protocollo);
            this.setStrutsAction(ApsAdminSystemConstants.EDIT);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "registraProvvisorio");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String entrySegnatura() {
        try {
            Protocollo protocollo = (Protocollo) this.getApsEntity();
            if (protocollo == null) {
                ApsSystemUtils.getLogger().severe("entrySegnatura: nessun protocollo in sessione");
                return FAILURE;
            }
            Documento documento = this.getDocumento();
            if (documento == null && !this.getMainDocumentOption().equals(MAIN_DOCUMENT_NONE_OPTION)) {
                ApsSystemUtils.getLogger().severe("entrySegnatura: nessun documento in sessione");
                return FAILURE;
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "entrySegnatura");
            return FAILURE;
        }
        return SUCCESS;
    }

    @Override
    public String saveProtocollo() {
        File file;
        String result = SUCCESS;
        boolean isInUpdateDocuments = false;
        try {
            Protocollo protocollo = (Protocollo) this.getApsEntity();
            Documento documento = this.getDocumento();
            if (protocollo == null) {
                ApsSystemUtils.getLogger().severe("saveProtocollo: nessun protocollo in sessione");
                return FAILURE;
            }
            if (this.getStrutsAction() == ApsAdminSystemConstants.ADD) {
                protocollo.setIdProtocollatore(this.getIdProfilo());
                if (!this.getMainDocumentOption().equals(MAIN_DOCUMENT_NONE_OPTION)) {
                    if (documento == null) {
                        ApsSystemUtils.getLogger().severe("saveProtocollo: nessun documento in sessione");
                        return FAILURE;
                    }
                    file = new File(this.getTempDir().concat(this.getMainDocumentTempFilePath()));
                    if (!file.exists()) {
                        ApsSystemUtils.getLogger().severe("aveProtocollo: !file.exists()");
                        return FAILURE;
                    }
                    protocollo.setDocumento(documento);
                    ((IProtocolloIngressoManager) this.getProtocolloManager()).addProtocollo(protocollo, file, this.getMainDocFileName(), this.getProfilo().getId(), false);
                    if (file.exists()) {
                        FileUtils.forceDelete(file);
                    }
                } else {
                    ((IProtocolloIngressoManager) this.getProtocolloManager()).addProtocollo(protocollo, this.getProfilo().getId());
                }
                this.addActionMessage(this.getText("Message.protocollo.added"));
            } else if (this.getStrutsAction() == ApsAdminSystemConstants.EDIT) {
                if (!this.getMainDocumentOption().equals(MAIN_DOCUMENT_NONE_OPTION)) {
                    isInUpdateDocuments = true;
                    if (null == documento) {
                        this.addActionError(this.getText("Error.documento.null"));
                        return INPUT;
                    }
                    file = new File(this.getTempDir().concat(this.getMainDocumentTempFilePath()));
                    if (!file.exists()) {
                        ApsSystemUtils.getLogger().severe("saveProtocollo: !file.exists()");
                        return FAILURE;
                    }
                    ((IProtocolloIngressoManager) this.getProtocolloManager()).addDocumentoToProtocollo(protocollo, documento, file, this.getMainDocFileName(), this.getProfilo().getId(), false);
                    if (file.exists()) {
                        FileUtils.forceDelete(file);
                    }
                }
            }
            if (this.getInoltra()) {
                result = "inoltra";
            } else if (!isInUpdateDocuments) {
                result = "viewDetails";
            } else {
                result = "edit";
                this.addActionMessage(this.getText("Message.allegato.saved"));
            }
            this.setId(protocollo.getId());
            String userOption = this.getMainDocumentOption();
            if (userOption.equals(MAIN_DOCUMENT_MAIL_OPTION)) {
                this.getPecManagerIN().updateStatusPecMailIN(this.getMailId(), protocollo.getId(), MessageIN.STATUS_LETTA_E_PROTOCOLLATA);
            }
            this.clearSessionProtocolloAndDocumento();
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "save");
            return FAILURE;
        }
        return result;
    }

    @Override
    public String entryDocumento() {
        try {
            Protocollo protocollo = (Protocollo) this.getApsEntity();
            if (null == protocollo) {
                ApsSystemUtils.getLogger().severe("Si sta cercando di inserire un allegato ad un protocollo non in sessione");
                return INPUT;
            }
            if (protocollo.getStatoProtocollo() == Protocollo.STATO_ANNULLATO) {
                this.addActionError(this.getText("Error.protocollo.anullato"));
                return INPUT;
            }
            boolean inoltrato = this.getProtocolloManager().isInoltrato(protocollo.getId());
            if (inoltrato) {
                this.addActionError(this.getText("Error.protocollo.inoltrato"));
                return INPUT;
            }
            this.setStrutsAction(ApsAdminSystemConstants.EDIT);
            this.setAnno(protocollo.getAnno());
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "entryDocumento");
            return FAILURE;
        }
        return SUCCESS;
    }

    public boolean isInoltrato(Object protocolloParam) {
        boolean inoltrato = false;
        try {
            if (protocolloParam != null) {
                inoltrato = this.getProtocolloManager().isInoltrato(protocolloParam);
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "isInoltrato");
            throw new RuntimeException("errore in verifica protocollo inoltrato");
        }
        return inoltrato;
    }

    /**
	 * Restituisce una mappa contenente gli inoltri per il protocollo in sessione
	 * La chiave della mappa è costituita dalla uo, i valori sono la lista degli profili ai quali è stato inoltrato
	 * @return
	 */
    public Map<UnitaOrganizzativa, List<Profilo>> getInfoInoltri() {
        Map<UnitaOrganizzativa, List<Profilo>> info = new HashMap<UnitaOrganizzativa, List<Profilo>>();
        try {
            Protocollo protocollo = (Protocollo) this.getApsEntity();
            List<Integer> recipients = this.getInoltroDocumentoManager().getRecipientsByDocument(protocollo.getDocumento().getId());
            if (null != recipients && recipients.size() > 0) {
                Iterator<Integer> it = recipients.iterator();
                while (it.hasNext()) {
                    int recipient = it.next();
                    Profilo profilo = this.getProfiloManager().getProfilo(recipient);
                    UnitaOrganizzativa uo = profilo.getUnitaOrganizzativa();
                    if (!info.containsKey(uo)) {
                        info.put(uo, new ArrayList<Profilo>());
                    }
                    info.get(uo).add(profilo);
                }
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "getInfoInoltri");
            throw new RuntimeException("errore recupero inoltri per il protocollo " + this.getApsEntity().getId());
        }
        return info;
    }

    public Documento getDocumento() {
        return (Documento) this.getRequest().getSession().getAttribute(IProtocolloAction.SESSION_PARAM_NAME_CURRENT_DOC_PROTOCOLLO);
    }

    public Ente getCurrentEnte() {
        return this.getEnteManager().getCurrentEnte();
    }

    public Aoo getCurrentAoo() {
        return this.getAooManager().getCurrentAoo();
    }

    public String printDocWithSignature() {
        try {
            if (null == this.getSelectedProtocol() || this.getSelectedProtocol().trim().length() == 0) {
                this.addActionError(this.getText("Message.protocollo.null"));
                return INPUT;
            }
            Profilo profilo = (Profilo) this.getRequest().getSession().getAttribute(AdocApsAdminSystemConstants.SESSION_PARAM_CURRENT_PROFILO);
            String extension = this.getProtocolloManager().getExtensionDocumentoProtocollo(profilo.getUsername(), this.getSelectedProtocol());
            if (extension == null) {
                this.addActionError(this.getText("Error.print.doc.protocol.extension"));
                return INPUT;
            }
            if (this.getDocumentoManager().isValidExtension(extension) == false) {
                this.addActionError(this.getText("Error.print.doc.protocol") + ": " + extension + " " + this.getText("Error.print.doc.protocol.extension.not.valid"));
                return INPUT;
            }
            InputStream baseInputStream = this.getProtocolloManager().getInputStreamDocumentoProtocollo(profilo.getUsername(), this.getSelectedProtocol());
            if (baseInputStream == null) {
                this.addActionError(this.getText("Error.print.doc.protocol.input.stream"));
                return INPUT;
            }
            InputStream in = this.getProtocolloManager().printDocumentoProtocollo(baseInputStream, extension, this.getSelectedProtocol());
            if (in == null) {
                this.addActionError(this.getText("Error.print.doc.protocol.openofficeserver"));
                return INPUT;
            }
            this.setInputStream(in);
            this.setDocFileName(this.getSelectedProtocol() + "." + AdocSystemConstants.FILE_EXTENSION_PDF);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "printDocWithSignature");
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
	 * Controlla la validata dei dati relativi al documento principale.
	 */
    private String checkMainDocument() {
        String check = null;
        String userOption = this.getMainDocumentOption();
        if (null == userOption || userOption.trim().length() == 0) {
            this.addActionError(this.getText("Error.mainDocument.option.null"));
            check = INPUT;
        } else if (userOption.equals(MAIN_DOCUMENT_NOTES_OPTION)) {
            if (null == this.getNote() || this.getNote().trim().length() == 0) {
                this.addActionError(this.getText("Error.mainDocument.insertNote"));
                check = INPUT;
            }
        } else if (userOption.equals(MAIN_DOCUMENT_NONE_OPTION)) {
        } else if (userOption.equals(MAIN_DOCUMENT_FILE_OPTION)) {
            if (null == this.getMainDoc()) {
                this.addActionError(this.getText("Error.mainDocument.null"));
                return INPUT;
            }
        } else if (userOption.equals(MAIN_DOCUMENT_MAIL_OPTION)) {
        } else {
            this.addActionError(this.getText("Error.mainDocument.checkMainDocument.error"));
            check = INPUT;
        }
        return check;
    }

    private void saveTempMainDocument() {
        try {
            String userOption = this.getMainDocumentOption();
            String tempFileName = this.createMainDocumentTempFileName();
            if (userOption.equals(MAIN_DOCUMENT_FILE_OPTION)) {
                if (this.getMainDoc().getAbsolutePath().compareTo(this.getTempDir().concat(tempFileName)) != 0) {
                    FileUtils.copyFile(new File(this.getMainDoc().getAbsolutePath()), new File(this.getTempDir().concat(tempFileName)));
                }
            } else if (userOption.equals(MAIN_DOCUMENT_NOTES_OPTION)) {
                String fullPath = this.getTempDir().concat(tempFileName);
                FileWriter fstream = new FileWriter(fullPath);
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(this.getNote());
                out.close();
                this.setMainDocFileName(tempFileName);
            } else if (userOption.equals(MAIN_DOCUMENT_MAIL_OPTION)) {
                MessageAttachment attach = getPecManagerIN().extractMainDocument(this.getMailId());
                if (attach != null) {
                    String fullPath = this.getTempDir().concat(attach.getFileName());
                    FileUtil.writeFile(attach.getInputStream(), fullPath);
                    this.setMainDocFileName(attach.getFileName());
                    tempFileName = attach.getFileName();
                } else {
                    SmallMessageIN messageIn = getPecManagerIN().getEmailIN(this.getMailId());
                    String fullPath = this.getTempDir().concat(tempFileName);
                    FileWriter fstream = new FileWriter(fullPath);
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(messageIn.getSubject());
                    out.close();
                    this.setMainDocFileName(tempFileName);
                }
            }
            this.setMainDocumentTempFilePath(tempFileName);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "saveMainDocument");
            throw new RuntimeException("Errore in salvataggio file principale del protocollo su cartella temporanea");
        }
    }

    /**
	 * Restituice il nome del cocumento temporaneo
	 * Se il documento è un file...
	 * Se il documento è campo note....
	 * @return path del file
	 */
    private String createMainDocumentTempFileName() {
        String pathOutFile = null;
        String userOption = this.getMainDocumentOption();
        if (userOption.equals(MAIN_DOCUMENT_FILE_OPTION)) {
            pathOutFile = this.getMainDoc().getName();
        } else if (userOption.equals(MAIN_DOCUMENT_NOTES_OPTION) || userOption.equals(MAIN_DOCUMENT_MAIL_OPTION)) {
            String noteFileName = "prot@" + DateConverter.getFormattedDate(new Date(), "dd-MM-yyyy_HH.mm.ss") + ".txt";
            pathOutFile = noteFileName;
        }
        return pathOutFile;
    }

    public Map<Integer, String> getEmailInByProtId(String idProtocollo) {
        Map<Integer, String> email = new HashMap<Integer, String>();
        try {
            email = this.getProtocolloManager().getEmailInByProtId(idProtocollo);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "getEmailInByProtId");
            throw new RuntimeException("Errore recupero email in input per il protocollo con id " + idProtocollo);
        }
        return email;
    }

    public void setTempDir(String tempDir) {
        this._tempDir = tempDir;
    }

    public String getTempDir() {
        return _tempDir;
    }

    public void setStrutsAction(int strutsAction) {
        this._strutsAction = strutsAction;
    }

    public int getStrutsAction() {
        return _strutsAction;
    }

    /**
	 * Campo note relativo al passo 1
	 * @param note
	 */
    public void setNote(String note) {
        this._note = note;
    }

    public String getNote() {
        return _note;
    }

    /**
	 * Il metodo di inserimento del documento principale del protocollo. vedere le costanti
	 * @param mainDocumentOption
	 */
    public void setMainDocumentOption(String mainDocumentOption) {
        System.out.println("oooooooo " + mainDocumentOption);
        this._mainDocumentOption = mainDocumentOption;
    }

    public String getMainDocumentOption() {
        return _mainDocumentOption;
    }

    /**
	 * Il documento principale deifinito la passo 1
	 * @return
	 */
    public File getMainDoc() {
        return _mainDoc;
    }

    public void setMainDoc(File mainDoc) {
        this._mainDoc = mainDoc;
    }

    public String getMainDocFileName() {
        return _mainDocFileName;
    }

    public void setMainDocFileName(String mainDocFileName) {
        this._mainDocFileName = mainDocFileName;
    }

    public String getMainDocContentType() {
        return _mainDocContentType;
    }

    public void setMainDocContentType(String mainDocContentType) {
        this._mainDocContentType = mainDocContentType;
    }

    public void setMainDocumentTempFilePath(String mainDocumentTempFilePath) {
        this._mainDocumentTempFilePath = mainDocumentTempFilePath;
    }

    public String getMainDocumentTempFilePath() {
        return _mainDocumentTempFilePath;
    }

    public void setDocumentoManager(IDocumentoManager documentoManager) {
        this._documentoManager = documentoManager;
    }

    public IDocumentoManager getDocumentoManager() {
        return _documentoManager;
    }

    public void setDocumentoPersonale(DocumentoPersonale documentoPersonale) {
        this._documentoPersonale = documentoPersonale;
    }

    public DocumentoPersonale getDocumentoPersonale() {
        return _documentoPersonale;
    }

    public void setInoltra(boolean inoltra) {
        this._inoltra = inoltra;
    }

    public boolean getInoltra() {
        return _inoltra;
    }

    public void setInoltroDocumentoManager(IInoltroDocumentoManager inoltroDocumentoManager) {
        this._inoltroDocumentoManager = inoltroDocumentoManager;
    }

    protected IInoltroDocumentoManager getInoltroDocumentoManager() {
        return _inoltroDocumentoManager;
    }

    public void setProfiloManager(IProfiloManager profiloManager) {
        this._profiloManager = profiloManager;
    }

    protected IProfiloManager getProfiloManager() {
        return _profiloManager;
    }

    public void setFromScanner(boolean fromScanner) {
        this._fromScanner = fromScanner;
    }

    public boolean getFromScanner() {
        return this._fromScanner;
    }

    public void setSelectedProtocol(String selectedProtocol) {
        this._selectedProtocol = selectedProtocol;
    }

    public String getSelectedProtocol() {
        return _selectedProtocol;
    }

    public InputStream getInputStream() {
        return _inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this._inputStream = inputStream;
    }

    public String getDocFileName() {
        return _docFileName;
    }

    public void setDocFileName(String docFileName) {
        this._docFileName = docFileName;
    }

    public void setMailId(int mailId) {
        this._mailId = mailId;
    }

    public int getMailId() {
        return _mailId;
    }

    public void setPecManagerIN(PecManagerIN pecManagerIN) {
        this._pecManagerIN = pecManagerIN;
    }

    public PecManagerIN getPecManagerIN() {
        return _pecManagerIN;
    }

    private int _strutsAction;

    private String _note;

    private File _mainDoc;

    private String _mainDocFileName;

    private String _mainDocContentType;

    private String _tempDir;

    private String _mainDocumentTempFilePath;

    private DocumentoPersonale _documentoPersonale;

    private boolean _inoltra;

    private boolean _fromScanner;

    private String _mainDocumentOption;

    public static final String PROTOCOLLO_DISABLING_CODE = "offEdit";

    private IDocumentoManager _documentoManager;

    private IInoltroDocumentoManager _inoltroDocumentoManager;

    private IProfiloManager _profiloManager;

    private String _selectedProtocol;

    private InputStream _inputStream;

    private int _mailId;

    private String _docFileName;

    private PecManagerIN _pecManagerIN;
}
