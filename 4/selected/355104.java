package com.gestioni.adoc.aps.system.services.firmaDigitale;

import java.io.File;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.AbstractService;
import com.agiletec.aps.util.DateConverter;

public class FirmaDigitaleManager extends AbstractService implements IFirmaDigitaleManager {

    @Override
    public void init() throws Exception {
        this.checkAndMakeFolder(this.getSignaturesFolderPath());
        this.checkAndMakeFolder(this.getFirmaDigitaleTempDiskFolder());
        ApsSystemUtils.getLogger().info(this.getClass().getName() + " ready");
    }

    /**
	 * Verifica la presenza di una directory.<br>
	 * Se non esiste la crea.
	 * @throws Exception 
	 */
    private void checkAndMakeFolder(String dirPath) throws Exception {
        try {
            File theDir = new File(dirPath);
            if (!theDir.exists()) {
                FileUtils.forceMkdir(theDir);
                ApsSystemUtils.getLogger().info("Directory " + dirPath + " creata con successo");
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "checkSignatureFolder");
            throw new Exception("Errore in creazione directory " + dirPath);
        }
    }

    public File signDocument(String username, File file, String extensionLowerCase, FirmaDigitale firmaDigitale) throws Throwable {
        try {
            if (this.getDigitalSignatureServices().containsKey(extensionLowerCase)) {
                return this.getDigitalSignatureServices().get(extensionLowerCase).nonSelfSignedMode(username, file, firmaDigitale);
            } else {
                ApsSystemUtils.getLogger().severe(extensionLowerCase + "non e' un'estensione valida ");
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "SignDoc");
            throw new ApsSystemException("Errore nell'apposizione della firma digitale " + username);
        }
        return null;
    }

    @Override
    public void putKeyStore(FirmaDigitale firmaDigitale) throws ApsSystemException {
        String username = firmaDigitale.getUsername();
        try {
            String dir = this.getSignaturesFolderPath() + username;
            this.checkAndMakeFolder(dir);
            String filename = this.buildKeystoreFilename(username);
            String filePath = dir + System.getProperty("file.separator") + filename;
            File previous = new File(filePath);
            if (previous.exists()) {
                String backupValue = DateConverter.getFormattedDate(new Date(), "yyyyMMddhhmmss");
                File backup = new File(previous.getAbsolutePath() + "." + backupValue);
                FileUtils.copyFile(previous, backup);
                FileUtils.forceDelete(previous);
            }
            FileUtils.copyFile(firmaDigitale.getKeyStore(), previous);
            this.getFirmaDigitaleDAO().insert(firmaDigitale);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "putKeyStore");
            throw new ApsSystemException("Errore in salvataggio file keystore per utente " + username);
        }
    }

    @Override
    public void deleteKeyStore(String username) throws ApsSystemException {
        try {
            String dir = this.getSignaturesFolderPath() + username;
            this.checkAndMakeFolder(dir);
            String filename = this.buildKeystoreFilename(username);
            String filePath = dir + System.getProperty("file.separator") + filename;
            File previous = new File(filePath);
            if (previous.exists()) {
                String backupValue = DateConverter.getFormattedDate(new Date(), "yyyyMMddhhmmss");
                File backup = new File(previous.getAbsolutePath() + "." + backupValue);
                FileUtils.copyFile(previous, backup);
                FileUtils.forceDelete(previous);
            }
            this.getFirmaDigitaleDAO().delete(username);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "deleteKeyStore");
            throw new ApsSystemException("Errore in eliminazione file keystore per utente " + username);
        }
    }

    @Override
    public FirmaDigitale getFirmaDigitale(String username) throws ApsSystemException {
        FirmaDigitale firmaDigitale = null;
        try {
            firmaDigitale = this.getFirmaDigitaleDAO().getFirmaDigitale(username);
            if (null != firmaDigitale) {
                String dir = this.getSignaturesFolderPath() + username;
                String filename = this.buildKeystoreFilename(username);
                String filePath = dir + System.getProperty("file.separator") + filename;
                File previous = new File(filePath);
                if (previous.exists()) {
                    firmaDigitale.setKeyStore(previous);
                }
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "getKeyStore");
            throw new ApsSystemException("Errore in recupero file keystore per utente " + username);
        }
        return firmaDigitale;
    }

    public void deleteUserFolder(String username) throws ApsSystemException {
        try {
            String dir = this.getSignaturesFolderPath() + username;
            File userDir = new File(dir);
            if (userDir.exists() && userDir.isDirectory()) {
                File[] files = userDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
                FileUtils.forceDelete(userDir);
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "deleteUserFolder");
            throw new ApsSystemException("Errore in eliminazione directory utente per utente " + username);
        }
    }

    @Override
    public void putCert(String username, File file) throws ApsSystemException {
        try {
            String dir = this.getSignaturesFolderPath() + username;
            this.checkAndMakeFolder(dir);
            String filename = this.buildCertFilename(username);
            String filePath = dir + System.getProperty("file.separator") + filename;
            File previous = new File(filePath);
            if (previous.exists()) {
                String backupValue = DateConverter.getFormattedDate(new Date(), "yyyyMMddhhmmss");
                File backup = new File(previous.getAbsolutePath() + "." + backupValue);
                FileUtils.copyFile(previous, backup);
                FileUtils.forceDelete(previous);
            }
            FileUtils.copyFile(file, previous);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "putCert");
            throw new ApsSystemException("Errore in salvataggio file cert per utente " + username);
        }
    }

    @Override
    public File getCert(String username) throws ApsSystemException {
        File file = null;
        try {
            String dir = this.getSignaturesFolderPath() + username;
            String filename = this.buildCertFilename(username);
            String filePath = dir + System.getProperty("file.separator") + filename;
            File previous = new File(filePath);
            if (previous.exists()) {
                file = previous;
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "getCert");
            throw new ApsSystemException("Errore in recupero file cert per utente " + username);
        }
        return file;
    }

    @Override
    public void deleteCert(String username) throws ApsSystemException {
        try {
            String dir = this.getSignaturesFolderPath() + username;
            this.checkAndMakeFolder(dir);
            String filename = this.buildCertFilename(username);
            String filePath = dir + System.getProperty("file.separator") + filename;
            File previous = new File(filePath);
            if (previous.exists()) {
                String backupValue = DateConverter.getFormattedDate(new Date(), "yyyyMMddhhmmss");
                File backup = new File(previous.getAbsolutePath() + "." + backupValue);
                FileUtils.copyFile(previous, backup);
                FileUtils.forceDelete(previous);
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "deleteCert");
            throw new ApsSystemException("Errore in eliminazione file cert per utente " + username);
        }
    }

    private String buildKeystoreFilename(String username) {
        return ".keystore";
    }

    private String buildCertFilename(String username) {
        return username + ".cer";
    }

    public void setSignaturesFolderPath(String signaturesFolderPath) {
        this._signaturesFolderPath = signaturesFolderPath;
    }

    public String getSignaturesFolderPath() {
        return _signaturesFolderPath;
    }

    public void setFirmaDigitaleTempDiskFolder(String firmaDigitaleTempDiskFolder) {
        this._firmaDigitaleTempDiskFolder = firmaDigitaleTempDiskFolder;
    }

    public String getFirmaDigitaleTempDiskFolder() {
        return _firmaDigitaleTempDiskFolder;
    }

    public void setFirmaDigitaleDAO(IFirmaDigitaleDAO firmaDigitaleDAO) {
        this._firmaDigitaleDAO = firmaDigitaleDAO;
    }

    protected IFirmaDigitaleDAO getFirmaDigitaleDAO() {
        return _firmaDigitaleDAO;
    }

    public void setDigitalSignatureServices(Map<String, ISignDocManager> digitalSignatureServices) {
        this._digitalSignatureServices = digitalSignatureServices;
    }

    public Map<String, ISignDocManager> getDigitalSignatureServices() {
        return _digitalSignatureServices;
    }

    private String _signaturesFolderPath;

    private IFirmaDigitaleDAO _firmaDigitaleDAO;

    private String _firmaDigitaleTempDiskFolder;

    private Map<String, ISignDocManager> _digitalSignatureServices;
}
