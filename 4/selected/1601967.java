package test.com.gestioni.adoc.aps.system.services.repository.documento;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import test.com.gestioni.adoc.aps.AdocBaseTestCase;
import test.com.gestioni.adoc.aps.JcrRepositoryBaseTestCase;
import test.com.gestioni.adoc.aps.system.services.UtilsHelperTest;
import test.com.gestioni.adoc.aps.system.services.documento.DocumentoTestHelper;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import com.gestioni.adoc.aps.system.services.documento.DocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.model.Documento;
import com.gestioni.adoc.aps.system.services.profilo.Profilo;
import com.gestioni.adoc.aps.system.services.protocollo.ProtocolloManager;
import com.gestioni.adoc.aps.system.services.protocollo.model.Protocollo;
import com.gestioni.adoc.aps.system.services.repository.IRepositoryConnection;
import com.gestioni.adoc.aps.system.services.repository.documento.IRepositoryDocumentoDAO;
import com.gestioni.adoc.aps.system.services.repository.documento.RepositoryDocumentoDAO;
import com.gestioni.adoc.aps.system.services.repository.documento.versioning.DocumentVersion;
import com.gestioni.adoc.aps.system.utils.AdocLogger;

public class TestRepositoryDocumentoDAO extends JcrRepositoryBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
    }

    /**
	 * Effettua la creazione e fascicolazione di un documento, ne effettua 2 aggiornamenti in cui mi aspetto
	 *  la generazione delle versioni. Controlla la riuscita dell recupero del file della versione intermedia
	 * @throws Throwable
	 */
    public void testInsertDocumentIntoFascicoloWithCheckVersioning() throws Throwable {
        String uuid = null;
        File out = null;
        Documento documento = null;
        try {
            String titolo = "DELETE_" + this.getClass().getSimpleName();
            createAndInsertDocument(titolo, FASCICOLO, false, ADD_GIALLI_FORMAZ, FILE_DOCUMENT_TEMP_PATHNAME);
            documento = loadDocByTitolo(titolo);
            File file = new File(FILE_DOCUMENT_PATHNAME_1);
            String uuidFascicolo = _fascicoloManager.getFascicolo(FASCICOLO).getPuntatoreJackRabbit();
            List<String> fascicoliList = new ArrayList<String>();
            fascicoliList.add(uuidFascicolo);
            IRepositoryConnection repConnection = _repositoryDocumentoDAO.add("USERNAME", documento, fascicoliList, file, file.getName());
            repConnection.saveTransaction();
            _repositoryDocumentoDAO.createNewDocumentVersionById(repConnection, documento.getId());
            uuid = documento.getPuntatoreJackRabbit();
            System.out.println("nome: " + _repositoryDocumentoDAO.getFileName("USERNAME", uuid));
            assertNotNull(uuid);
            out = getFileFromDocumento(uuid);
            assertEquals(file.length(), out.length());
            out.delete();
            List<DocumentVersion> versioni = _repositoryDocumentoDAO.getListaVersioni("USERNAME", documento.getId());
            assertEquals(1, versioni.size());
            DocumentVersion docVersioned = new DocumentVersion();
            InputStream versionStream = _repositoryDocumentoDAO.getDocumentFileVersioned("USERNAME", docVersioned, uuid, versioni.get(0).getVersion());
            InputStream fileIn = new FileInputStream(file);
            assertTrue(UtilsHelperTest.compareInputStreams(versionStream, fileIn));
        } finally {
            if (null != uuid) {
                _repositoryDocumentoDAO.removeByUUID("userIdentifier", uuid);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    /**
	 * Effettua la creazione e fascicolazione di un documento, ne effettua 2 aggiornamenti in cui mi aspetto
	 *  la generazione delle versioni. Controlla la riuscita dell recupero del file della versione intermedia
	 * @throws Throwable
	 */
    public void _testInsertDocumentIntoFascicoloAnUpdateFileWithCheckVersioning() throws Throwable {
        String uuid = null;
        File out = null;
        try {
            String[] fascicoli = new String[] { "1.01.01-F02-02-02" };
            String[] vociTitolario = new String[] {};
            String[] docCollegati = new String[] {};
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("titolo", "META TITOLO DI TEST");
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put("Titolo", "ATTRIBUTO TITOLO DI TEST");
            Documento documento = (Documento) _helper.createTestDocument(AdocBaseTestCase.DOCUMENT_TYPE_TES, this._profilo, metadata, attributes, vociTitolario, fascicoli, docCollegati);
            documento.setId("" + _keyManager.getUniqueKeyCurrentValue());
            File file = new File(FILE_DOCUMENT_PATHNAME_1);
            String uuidFascicolo = _fascicoloManager.getFascicolo("1.01.01-F02-02-01").getPuntatoreJackRabbit();
            List<String> fascicoliList = new ArrayList<String>();
            fascicoliList.add(uuidFascicolo);
            IRepositoryConnection repConnection = _repositoryDocumentoDAO.add("USERNAME", documento, fascicoliList, file, file.getName());
            repConnection.saveAndCloseConnection();
            uuid = documento.getPuntatoreJackRabbit();
            System.out.println("nome: " + _repositoryDocumentoDAO.getFileName("USERNAME", uuid));
            assertNotNull(uuid);
            out = getFileFromDocumento(uuid);
            assertEquals(file.length(), out.length());
            out.delete();
            File fileNewVersion2 = new File(FILE_DOCUMENT_PATHNAME_2);
            System.out.println("nome: " + _repositoryDocumentoDAO.getFileName("USERNAME", uuid));
            assertNotNull(uuid);
            out = getFileFromDocumento(uuid);
            assertEquals(fileNewVersion2.length(), out.length());
            out.delete();
            File fileNewVersion3 = new File(FILE_DOCUMENT_PATHNAME_3);
            uuid = documento.getPuntatoreJackRabbit();
            System.out.println("nome: " + _repositoryDocumentoDAO.getFileName("USERNAME", uuid));
            assertNotNull(uuid);
            out = getFileFromDocumento(uuid);
            assertEquals(fileNewVersion3.length(), out.length());
            out.delete();
            InputStream versionStream = _repositoryDocumentoDAO.getDocumentFileVersioned("USERNAME", null, uuid, "1.0");
            InputStream fileIn = new FileInputStream(fileNewVersion2);
            assertTrue(UtilsHelperTest.compareInputStreams(versionStream, fileIn));
        } finally {
            if (null != uuid) {
                _repositoryDocumentoDAO.removeByUUID("userIdentifier", uuid);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    private File getFileFromDocumento(String uuid) throws Throwable {
        File out = null;
        InputStream in = ((RepositoryDocumentoDAO) _repositoryDocumentoDAO).loadFileByUUIDDocument("userIdentifier", uuid);
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
        _repositoryDocumentoDAO = (IRepositoryDocumentoDAO) this.getApplicationContext().getBean("repositoryDocumentoDAO");
        _keyManager = (IKeyGeneratorManager) this.getService(SystemConstants.KEY_GENERATOR_MANAGER);
        _helper = new DocumentoTestHelper(_documentoManager);
        _profilo = this._profiloManager.getProfilo(3);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            List<String> protocolli = searchProtocolliByOggetto("DELETE_", true);
            AdocLogger.logDebug("Trovati " + protocolli.size() + " protocolli da cancellare", this);
            for (int i = 0; i < protocolli.size(); i++) {
                String protocolloId = protocolli.get(i);
                Protocollo prot = this._protocolloManager.getProtocollo(protocolloId);
                ((ProtocolloManager) this._protocolloManager).deleteProtocollo(prot, ADD_GIALLI_FORMAZ);
                AdocLogger.logDebug("RIMOSSO PROTOCOLLO " + prot.getOggetto(), this);
            }
            List<String> documenti = this.searchDocumentByTitolo("DELETE_", true, null);
            AdocLogger.logDebug("Trovati " + documenti.size() + " documenti da cancellare", this);
            for (int i = 0; i < documenti.size(); i++) {
                String documentiId = documenti.get(i);
                ((DocumentoManager) this._documentoManager).deleteDocument(documentiId, null);
                AdocLogger.logDebug("RIMOSSO DOCUMENTO " + documentiId, this);
            }
        } catch (Exception e) {
        } finally {
        }
        super.tearDown();
    }

    private IRepositoryDocumentoDAO _repositoryDocumentoDAO;

    private Profilo _profilo;

    private IKeyGeneratorManager _keyManager;

    private DocumentoTestHelper _helper;

    private static final String FILE_DOCUMENT_TEMP_PATHNAME = "./admin/test/documentsFile/temp.txt";

    private static final String FILE_DOCUMENT_PATHNAME_1 = "./admin/test/documentsFile/1.jpg";

    private static final String FILE_DOCUMENT_PATHNAME_2 = "./admin/test/documentsFile/2.jpg";

    private static final String FILE_DOCUMENT_PATHNAME_3 = "./admin/test/documentsFile/3.jpg";

    private static final String FASCICOLO = "2.03.02-F03-01-07";

    private static final String TITOLARIO = "2.03.01";
}
