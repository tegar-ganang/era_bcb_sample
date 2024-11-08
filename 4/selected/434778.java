package test.com.gestioni.adoc.aps.system.services.repository.documento;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import org.apache.jackrabbit.core.lock.SessionLockManager;
import test.com.gestioni.adoc.aps.AdocBaseTestCase;
import test.com.gestioni.adoc.aps.JcrRepositoryBaseTestCase;
import test.com.gestioni.adoc.aps.system.services.UtilsHelperTest;
import test.com.gestioni.adoc.aps.system.services.documento.DocumentoTestHelper;
import test.com.gestioni.adoc.aps.system.services.repository.JcrCredentials;
import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import com.gestioni.adoc.aps.system.AdocSystemConstants;
import com.gestioni.adoc.aps.system.services.documento.IDocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.model.Documento;
import com.gestioni.adoc.aps.system.services.documento.model.DocumentoRecord;
import com.gestioni.adoc.aps.system.services.documento.model.IDocumento;
import com.gestioni.adoc.aps.system.services.documento.personale.DocumentoPersonale;
import com.gestioni.adoc.aps.system.services.documento.stato.Stato;
import com.gestioni.adoc.aps.system.services.fascicolo.IFascicoloManager;
import com.gestioni.adoc.aps.system.services.profilo.IProfiloManager;
import com.gestioni.adoc.aps.system.services.profilo.Profilo;
import com.gestioni.adoc.aps.system.services.repository.IRepositoryConnection;
import com.gestioni.adoc.aps.system.services.repository.documento.IRepositoryDocumentoManager;
import com.gestioni.adoc.aps.system.services.repository.documento.personale.IRepositoryDocumentoPersonaleManager;
import com.gestioni.adoc.aps.system.services.repository.documento.personale.RepositoryDocumentoPersonaleManager;

public class TestRepositoryDocumentoManager extends JcrRepositoryBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
    }

    public void testCheckOutDocument() throws Throwable {
        String uuidDoc = null;
        File out = null;
        String uuidDocPersonale = null;
        try {
            File file = new File(FILE_DOCUMENT_PATHNAME);
            IDocumento documento = this.createDocument("DELETE_titolo", false, ADD_GIALLI_FORMAZ);
            IRepositoryConnection repConnection = _repositoryDocumentoManager.add(USERNAME, documento, file, file.getName());
            repConnection.saveAndCloseConnection();
            uuidDoc = documento.getPuntatoreJackRabbit();
            assertNotNull(uuidDoc);
            out = getFileFromDocumento(uuidDoc);
            assertEquals(file.length(), out.length());
            out.delete();
            DocumentoPersonale documentoPersonale = new DocumentoPersonale((Documento) documento, _profilo);
            int key = this._keyManager.getUniqueKeyCurrentValue();
            documentoPersonale.setId(String.valueOf(key));
            repConnection = _repositoryDocumentoManager.checkout(JcrCredentials.getUserIdentifier(_profilo), documento, documentoPersonale);
            repConnection.saveAndCloseConnection();
            uuidDocPersonale = documentoPersonale.getPuntatoreJackRabbit();
            DocumentoRecord docLetto = _repositoryDocumentoManager.getDocument(JcrCredentials.getUserIdentifier(_profilo), uuidDoc);
            assertEquals(Stato.CHECKOUT, docLetto.getStato());
            DocumentoPersonale docPersonaleLetto = _repositoryDocumentoPersonaleManager.getDocumentoPersonale(JcrCredentials.getUserIdentifier(_profilo), uuidDocPersonale);
            this.compareDocumento((Documento) documento, docLetto, file);
            this.compareDocumentoPersonale(documentoPersonale, docPersonaleLetto, file);
        } finally {
            if (null != uuidDoc) {
                this._repositoryDocumentoManager.removeById(USERNAME, uuidDoc);
                this._repositoryDocumentoManager.removeById(USERNAME, uuidDocPersonale);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    public void testCheckInDocument() throws Throwable {
        String uuidDoc = null;
        File out = null;
        String uuidDocPersonale = null;
        try {
            File file = new File(FILE_DOCUMENT_PATHNAME);
            System.out.println("exist " + file.exists());
            IDocumento documento = this.createDocument("DELETE_TITOLO", false, ADD_GIALLI_FORMAZ);
            documento.setDataCreazione(new Timestamp(System.currentTimeMillis()));
            String uuidFascicolo = _fascicoloManager.getFascicolo(documento.getFascicoli().get(0)).getPuntatoreJackRabbit();
            IRepositoryConnection repConnection = _repositoryDocumentoManager.add(USERNAME, documento, file, file.getName());
            repConnection.saveAndCloseConnection();
            uuidDoc = documento.getPuntatoreJackRabbit();
            assertNotNull(uuidDoc);
            out = getFileFromDocumento(uuidDoc);
            assertEquals(file.length(), out.length());
            out.delete();
            DocumentoPersonale documentoPersonale = new DocumentoPersonale((Documento) documento, _profilo);
            int key = this._keyManager.getUniqueKeyCurrentValue();
            documentoPersonale.setId(String.valueOf(key));
            repConnection = _repositoryDocumentoManager.checkout(JcrCredentials.getUserIdentifier(_profilo), documento, documentoPersonale);
            repConnection.saveAndCloseConnection();
            uuidDocPersonale = documentoPersonale.getPuntatoreJackRabbit();
            DocumentoRecord docLetto = _repositoryDocumentoManager.getDocument(JcrCredentials.getUserIdentifier(_profilo), uuidDoc);
            assertEquals(Stato.CHECKOUT, docLetto.getStato());
            DocumentoPersonale docPersonaleLetto = _repositoryDocumentoPersonaleManager.getDocumentoPersonale(JcrCredentials.getUserIdentifier(_profilo), uuidDocPersonale);
            this.compareDocumento((Documento) documento, docLetto, file);
            this.compareDocumentoPersonale(documentoPersonale, docPersonaleLetto, file);
            File newFile = new File(FILE_DOCUMENT_JPG_PATHNAME);
            ((RepositoryDocumentoPersonaleManager) _repositoryDocumentoPersonaleManager).update(JcrCredentials.getUserIdentifier(_profilo), documentoPersonale, newFile, newFile.getName());
            DocumentoPersonale docPersonaleLettoPostUpdate = _repositoryDocumentoPersonaleManager.getDocumentoPersonale(JcrCredentials.getUserIdentifier(_profilo), uuidDocPersonale);
            this.compareDocumentoPersonale(documentoPersonale, docPersonaleLettoPostUpdate, newFile);
            IRepositoryConnection repConnection1 = _repositoryDocumentoManager.checkin(JcrCredentials.getUserIdentifier(_profilo), documento, newFile, newFile.getName(), docPersonaleLettoPostUpdate);
            repConnection1.saveAndCloseConnection();
            DocumentoRecord docLettoPostCheckIn = _repositoryDocumentoManager.getDocument(JcrCredentials.getUserIdentifier(_profilo), uuidDoc);
            this.compareDocumento((Documento) documento, docLettoPostCheckIn, newFile);
        } finally {
            if (null != uuidDoc) {
                this._repositoryDocumentoManager.removeById(USERNAME, uuidDoc);
                this._repositoryDocumentoManager.removeById(USERNAME, uuidDocPersonale);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    protected void closeDaoResources(Session session) {
        try {
            if (session != null) {
                session.logout();
                SessionLockManager lockManager = (SessionLockManager) session.getWorkspace().getLockManager();
                String[] locks = lockManager.getLockTokens();
                if (locks.length > 0) {
                    for (int i = 0; i < locks.length; i++) {
                        System.out.println("locks: " + locks[i]);
                        lockManager.removeLockToken(locks[i]);
                    }
                }
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "closeDaoStatement", "Error closing jcr connection");
        }
    }

    private void compareDocumentoPersonale(DocumentoPersonale expected, DocumentoPersonale docDaComparare, File file) throws Throwable {
        assertEquals(expected.getId(), docDaComparare.getId());
        assertEquals(expected.getProfilo(), docDaComparare.getProfilo());
        assertEquals(expected.getAutore(), docDaComparare.getAutore());
        assertEquals(expected.getDataCreazione(), docDaComparare.getDataCreazione());
        assertEquals(expected.getVersione(), docDaComparare.getVersione());
        assertEquals(expected.getPuntatoreJackRabbit(), docDaComparare.getPuntatoreJackRabbit());
        assertEquals(expected.getTitolo(), docDaComparare.getTitolo());
        assertEquals(expected.getSunto(), docDaComparare.getSunto());
        assertEquals(expected.getDettaglio(), docDaComparare.getDettaglio());
        assertEquals(expected.getTags(), docDaComparare.getTags());
        assertEquals(expected.getRefDocumentale(), docDaComparare.getRefDocumentale());
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        InputStream input = _repositoryDocumentoManager.loadFileByDocumentUUID(JcrCredentials.getUserIdentifier(_profilo), docDaComparare.getPuntatoreJackRabbit());
        InputStream fileIn = new FileInputStream(file);
        assertTrue(UtilsHelperTest.compareInputStreams(input, fileIn));
    }

    private void compareDocumento(Documento expected, DocumentoRecord docDaComparare, File file) throws Throwable {
        assertEquals(expected.getId(), docDaComparare.getId());
        assertEquals(expected.getProfilo(), docDaComparare.getProfilo());
        assertEquals(expected.getAutore(), docDaComparare.getAutore());
        assertEquals(expected.getDataCreazione(), docDaComparare.getDataCreazione());
        assertEquals(expected.getVersione(), docDaComparare.getVersione());
        assertEquals(expected.getPuntatoreJackRabbit(), docDaComparare.getPuntatoreJackRabbit());
        assertEquals(expected.getTitolo(), docDaComparare.getTitolo());
        assertEquals(expected.getSunto(), docDaComparare.getSunto());
        assertEquals(expected.getDettaglio(), docDaComparare.getDettaglio());
        assertEquals(expected.getTags(), docDaComparare.getTags());
        assertEquals(expected.getTypeCode(), docDaComparare.getTypeCode());
        assertEquals(expected.getXML(), docDaComparare.getXml());
        assertEquals(expected.isRiservato(), docDaComparare.isRiservato());
        assertEquals(expected.getStartDate(), docDaComparare.getStartDate());
        assertEquals(expected.getEndDate(), docDaComparare.getEndDate());
        assertEquals(expected.getStato(), docDaComparare.getStato());
        assertEquals(expected.getSunto(), docDaComparare.getSunto());
        assertEquals(expected.getDettaglio(), docDaComparare.getDettaglio());
        assertEquals(expected.getTags(), docDaComparare.getTags());
        assertEquals(expected.getDataPassaggioStorico(), docDaComparare.getDataPassaggioStorico());
        assertEquals(expected.getDataPassaggioDeposito(), docDaComparare.getDataPassaggioDeposito());
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        InputStream input = _repositoryDocumentoManager.loadFileByDocumentUUID(JcrCredentials.getUserIdentifier(_profilo), docDaComparare.getPuntatoreJackRabbit());
        InputStream fileIn = new FileInputStream(file);
        assertTrue(UtilsHelperTest.compareInputStreams(input, fileIn));
    }

    private File getFileFromDocumento(String uuid) throws Throwable {
        File out = null;
        InputStream in = _repositoryDocumentoManager.loadFileByDocumentUUID(USERNAME, uuid);
        out = new File(FILE_DOCUMENT_TEMP_PATHNAME);
        OutputStream outStream = new FileOutputStream(out);
        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) outStream.write(buf, 0, len);
        outStream.close();
        in.close();
        assertNotNull(out.length());
        return out;
    }

    private void init() throws ApsSystemException {
        _documentoManager = (IDocumentoManager) this.getService(AdocSystemConstants.DOCUMENTO_MANAGER);
        _repositoryDocumentoManager = (IRepositoryDocumentoManager) this.getService(AdocSystemConstants.REPOSITORY_DOCUMENTO_MANAGER);
        _repositoryDocumentoPersonaleManager = (IRepositoryDocumentoPersonaleManager) this.getService(AdocSystemConstants.REPOSITORY_DOCUMENTO_PERSONALE_MANAGER);
        _fascicoloManager = (IFascicoloManager) this.getService(AdocSystemConstants.FASCICOLO_MANAGER);
        _keyManager = (IKeyGeneratorManager) this.getService(SystemConstants.KEY_GENERATOR_MANAGER);
        _helper = new DocumentoTestHelper(_documentoManager);
        _profiloManager = (IProfiloManager) this.getService(AdocSystemConstants.PROFILO_MANAGER);
        _profilo = this._profiloManager.getProfilo(3);
    }

    private IRepositoryDocumentoManager _repositoryDocumentoManager;

    private Profilo _profilo;

    private IKeyGeneratorManager _keyManager;

    private IFascicoloManager _fascicoloManager;

    private IDocumentoManager _documentoManager;

    private IRepositoryDocumentoPersonaleManager _repositoryDocumentoPersonaleManager;

    private DocumentoTestHelper _helper;

    private IProfiloManager _profiloManager;

    private static final String FILE_DOCUMENT_PATHNAME = "./admin/test/documentsFile/test.txt";

    private static final String FILE_DOCUMENT_TEMP_PATHNAME = "./admin/test/documentsFile/temp.txt";

    private static final String FILE_DOCUMENT_JPG_PATHNAME = "./admin/test/documentsFile/test.jpg";

    private static final String USERNAME = "test";
}
