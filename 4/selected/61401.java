package test.com.gestioni.adoc.aps.system.services.repository.documento;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.commons.collections.ListUtils;
import org.apache.jackrabbit.core.lock.SessionLockManager;
import test.com.gestioni.adoc.aps.AdocBaseTestCase;
import test.com.gestioni.adoc.aps.JcrRepositoryBaseTestCase;
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
import com.gestioni.adoc.aps.system.services.fascicolo.Fascicolo;
import com.gestioni.adoc.aps.system.services.fascicolo.IFascicoloManager;
import com.gestioni.adoc.aps.system.services.profilo.IProfiloManager;
import com.gestioni.adoc.aps.system.services.profilo.Profilo;
import com.gestioni.adoc.aps.system.services.repository.AbstractJcrRepositoryDAO;
import com.gestioni.adoc.aps.system.services.repository.IRepositoryConnection;
import com.gestioni.adoc.aps.system.services.repository.documento.IRepositoryDocumentoDAO;
import com.gestioni.adoc.aps.system.services.repository.documento.IRepositoryDocumentoManager;
import com.gestioni.adoc.aps.system.services.repository.documento.RepositoryDocumentoDAO;
import com.gestioni.adoc.aps.system.services.repository.fascicolo.IRepositoryFascicoloManager;

public class TestRepositorySharedDocumentoManager extends JcrRepositoryBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
    }

    public void testInsertExchangeDocumentIntoFascicoli() throws Throwable {
        String documentoId = null;
        File out = null;
        IRepositoryConnection repConnection = null;
        try {
            Documento documento = this.createDefaultDocument();
            documentoId = documento.getId();
            File file = new File(FILE_DOCUMENT_PATHNAME);
            repConnection = _repositoryDocumentoManager.add(USERNAME, documento, file, file.getName());
            repConnection.saveTransaction();
            DocumentoRecord docLetto = _repositoryDocumentoManager.getDocument(JcrCredentials.getUserIdentifier(_profilo), documento.getPuntatoreJackRabbit());
            this.compareDocumento(documento, docLetto, file);
            Fascicolo proxFatherShared = _fascicoloManager.getFascicolo("3.01.02-F01-04");
            documento.getFascicoli().add("3.01.02-F01-04");
            _repositoryDocumentoManager.updateClassify(repConnection, documento);
            Node documentoLettoNewPadre = ((RepositoryDocumentoDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), documento.getPuntatoreJackRabbit());
            List<String> allNodeFatherToChecks = ListUtils.sum(documento.getVociTitolario(), documento.getFascicoli());
            List<String> temp = getListName(documentoLettoNewPadre.getSharedSet());
            assertEquals(2, temp.size());
            Fascicolo fascicoloPadrePrimario = _fascicoloManager.getFascicolo("1.01.01-F02-02-01");
            Node proxFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared.getPuntatoreJackRabbit());
            Node origFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), fascicoloPadrePrimario.getPuntatoreJackRabbit());
            temp = getListName(documentoLettoNewPadre.getSharedSet());
            assertTrue(ListUtils.isEqualList(allNodeFatherToChecks, temp));
            String uuidDocByfather = this.checkMyDocumentIntoChildren(origFatherNode, documento.getId());
            String uuidDocByfatherShared = this.checkMyDocumentIntoChildren(proxFatherNode, documento.getId());
            assertEquals(uuidDocByfather, uuidDocByfatherShared);
            repConnection.saveAndCloseConnection();
        } finally {
            if (null != documentoId) {
                this._repositoryDocumentoManager.removeById(USERNAME, documentoId);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    public void _testInsertDocumentIntoFascicolo() throws Throwable {
        String uuid = null;
        String documentoId = null;
        File out = null;
        IRepositoryConnection repConnection = null;
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        try {
            Documento documento = this.createDefaultDocument();
            documentoId = documento.getId();
            File file = new File(FILE_DOCUMENT_PATHNAME);
            Fascicolo fascicoloPadrePrimario = _fascicoloManager.getFascicolo("1.01.01-F02-02-01");
            String uuidFascicoloPadre = fascicoloPadrePrimario.getPuntatoreJackRabbit();
            documento.getFascicoli().add("1.01.01-F02-02-01");
            repConnection = _repositoryDocumentoManager.add(USERNAME, documento, file, file.getName());
            repConnection.saveTransaction();
            uuid = documento.getPuntatoreJackRabbit();
            DocumentoRecord docLetto = _repositoryDocumentoManager.getDocument(JcrCredentials.getUserIdentifier(_profilo), documento.getPuntatoreJackRabbit());
            this.compareDocumento(documento, docLetto, file);
            Fascicolo proxFatherShared = _fascicoloManager.getFascicolo("3.01.02-F01-04");
            documento.getFascicoli().add("3.01.02-F01-04");
            _repositoryDocumentoManager.updateClassify(repConnection, documento);
            Node documentoLetto2Padri = ((RepositoryDocumentoDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), documento.getPuntatoreJackRabbit());
            List<String> allNodeFatherToChecks = ListUtils.sum(documento.getVociTitolario(), documento.getFascicoli());
            Node proxFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared.getPuntatoreJackRabbit());
            Node origFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), fascicoloPadrePrimario.getPuntatoreJackRabbit());
            List<String> temp = getListName(documentoLetto2Padri.getSharedSet());
            assertTrue(ListUtils.isEqualList(allNodeFatherToChecks, temp));
            repConnection.saveAndCloseConnection();
            String uuidDocByfather = this.checkMyDocumentIntoChildren(origFatherNode, documento.getId());
            String uuidDocByfatherShared = this.checkMyDocumentIntoChildren(proxFatherNode, documento.getId());
            assertEquals(uuidDocByfather, uuidDocByfatherShared);
            documento.setAutore("123ProvaAutore");
            _repositoryDocumentoManager.updateDocument(((SimpleCredentials) cred).getUserID(), documento);
            uuidDocByfather = this.checkMyDocumentIntoChildren(origFatherNode, documento.getId());
            uuidDocByfatherShared = this.checkMyDocumentIntoChildren(proxFatherNode, documento.getId());
            assertNotNull(uuidDocByfatherShared);
            assertEquals(uuidDocByfather, uuidDocByfather);
            DocumentoRecord docRilettoDopoAggiornato = _repositoryDocumentoManager.getDocument(((SimpleCredentials) cred).getUserID(), uuidDocByfather);
            this.compareDocumento(documento, docRilettoDopoAggiornato, file);
            repConnection.saveTransaction();
            Fascicolo proxFatherShared2 = _fascicoloManager.getFascicolo("3.01.02-F01-03");
            documento.getFascicoli().add("3.01.02-F01-03");
            _repositoryDocumentoManager.updateClassify(repConnection, documento);
            Node documentoLetto3Padri = ((RepositoryDocumentoDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), documento.getPuntatoreJackRabbit());
            allNodeFatherToChecks = ListUtils.sum(documento.getVociTitolario(), documento.getFascicoli());
            assertTrue(ListUtils.isEqualList(allNodeFatherToChecks, getListName(documentoLetto3Padri.getSharedSet())));
            System.out.println("nodo doc path: " + documentoLetto3Padri.getPath());
            System.out.println("nodo padre: " + documentoLetto3Padri.getParent().getName());
            origFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), fascicoloPadrePrimario.getPuntatoreJackRabbit());
            proxFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared.getPuntatoreJackRabbit());
            Node proxFather2Node = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared2.getPuntatoreJackRabbit());
            repConnection.saveTransaction();
            uuidDocByfather = this.checkMyDocumentIntoChildren(origFatherNode, documento.getId());
            uuidDocByfatherShared = this.checkMyDocumentIntoChildren(proxFatherNode, documento.getId());
            String uuidDocByfather2Shared = this.checkMyDocumentIntoChildren(proxFather2Node, documento.getId());
            assertNotNull(uuidDocByfatherShared);
            assertEquals(uuidDocByfather, uuidDocByfatherShared);
            assertEquals(uuidDocByfather, uuidDocByfather2Shared);
            documento.getFascicoli().remove(fascicoloPadrePrimario.getCode());
            this._repositoryDocumentoManager.updateClassify(repConnection, documento);
            repConnection.saveAndCloseConnection();
            Node documentoLettoSolo2PadriShared = ((RepositoryDocumentoDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), documento.getPuntatoreJackRabbit());
            allNodeFatherToChecks = ListUtils.sum(documento.getVociTitolario(), documento.getFascicoli());
            assertTrue(ListUtils.isEqualList(allNodeFatherToChecks, getListName(documentoLettoSolo2PadriShared.getSharedSet())));
            origFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), fascicoloPadrePrimario.getPuntatoreJackRabbit());
            proxFatherNode = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared.getPuntatoreJackRabbit());
            proxFather2Node = ((AbstractJcrRepositoryDAO) _repositoryDocumentoDAO).getNodeByUUID((Session) repConnection.getRepositoryConnection(), proxFatherShared2.getPuntatoreJackRabbit());
            uuidDocByfather = this.checkMyDocumentIntoChildren(origFatherNode, documento.getId());
            uuidDocByfatherShared = this.checkMyDocumentIntoChildren(proxFatherNode, documento.getId());
            uuidDocByfather2Shared = this.checkMyDocumentIntoChildren(proxFather2Node, documento.getId());
            assertNull(uuidDocByfather);
            assertNotNull(uuidDocByfatherShared);
            assertEquals(uuidDocByfatherShared, uuidDocByfather2Shared);
        } finally {
            if (null != uuid) {
                this._repositoryDocumentoManager.removeById(USERNAME, documentoId);
            }
            if (null != out) {
                out.delete();
            }
        }
    }

    /**
	 * Verifica se tra i figli del nodo in ingresso vi è un nodo che possiede l'id uguale a quello richiesto
	 * @param node Il nodo in cuio ricercare il figlio ddesiderato
	 * @param idDocument L'id del documento da ricercare
	 * @throws RepositoryException
	 */
    private String checkMyDocumentIntoChildren(Node node, String idDocument) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        String uuid = null;
        boolean found = false;
        while (nodes.hasNext() && !found) {
            Node nodeToCheck = nodes.nextNode();
            found = nodeToCheck.getName().equals(idDocument);
            if (found) {
                uuid = nodeToCheck.getIdentifier();
            }
        }
        return uuid;
    }

    /**
	 * Verifica se l'iserimento del documento è avvenuto correttamente.
	 * @param uuid
	 * @param file
	 * @throws ApsSystemException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private Documento createDefaultDocument() throws ApsSystemException {
        String[] fascicoli = new String[] { "1.01.01-F02-02-01" };
        String[] vociTitolario = new String[] {};
        String[] docCollegati = new String[] { DOCUMENT_TYPE_TES + "2", DOCUMENT_TYPE_TES + "4", DOCUMENT_TYPE_TES + "6" };
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("titolo", "META TITOLO DI TEST");
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("Titolo", "ATTRIBUTO TITOLO DI TEST");
        Documento documento = (Documento) _helper.createTestDocument(AdocBaseTestCase.DOCUMENT_TYPE_TES, this._profilo, metadata, attributes, vociTitolario, fascicoli, docCollegati);
        documento.setDataCreazione(new Timestamp(System.currentTimeMillis()));
        documento.setId(String.valueOf(_keyManager.getUniqueKeyCurrentValue()));
        return documento;
    }

    private static List<String> getListName(NodeIterator iter) throws RepositoryException {
        ArrayList<String> list = new ArrayList<String>();
        while (iter.hasNext()) {
            list.add(iter.nextNode().getParent().getName());
        }
        return list;
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

    private void compareDocumento(Documento expected, DocumentoRecord docDaComparare, File file) throws Throwable {
        assertNotNull(docDaComparare.getPuntatoreJackRabbit());
        File out = getFileFromDocumento(docDaComparare.getPuntatoreJackRabbit());
        assertEquals(file.length(), out.length());
        out.delete();
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
        checkEqualsList(expected.getVociTitolario(), docDaComparare.getVociTitolario());
        checkEqualsList(expected.getFascicoli(), docDaComparare.getFascicoli());
        checkEqualsList(expected.getDocumentiCollegati(), docDaComparare.getDocumentiCollegati());
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        InputStream input = _repositoryDocumentoManager.loadFileByDocumentUUID(JcrCredentials.getUserIdentifier(_profilo), docDaComparare.getPuntatoreJackRabbit());
        InputStream fileIn = new FileInputStream(file);
        assertTrue(isSame(input, fileIn));
    }

    private void checkEqualsList(List<String> orinalList, List<String> expectedList) {
        assertEquals(orinalList.size(), expectedList.size());
        for (int i = 0; i < orinalList.size(); i++) {
            assertTrue(expectedList.contains(orinalList.get(i)));
        }
    }

    public boolean isSame(InputStream input1, InputStream input2) throws Throwable {
        boolean error = false;
        try {
            byte[] buffer1 = new byte[1024];
            byte[] buffer2 = new byte[1024];
            try {
                int numRead1 = 0;
                int numRead2 = 0;
                while (true) {
                    numRead1 = input1.read(buffer1);
                    numRead2 = input2.read(buffer2);
                    if (numRead1 > -1) {
                        if (numRead2 != numRead1) return false;
                        if (!Arrays.equals(buffer1, buffer2)) return false;
                    } else {
                        return numRead2 < 0;
                    }
                }
            } finally {
                input1.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            error = true;
            throw e;
        } finally {
            try {
                input2.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
    }

    private File getFileFromDocumento(String uuid) throws ApsSystemException, FileNotFoundException, IOException {
        File out;
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
        _helper = new DocumentoTestHelper(_documentoManager);
        _profilo = this._profiloManager.getProfilo(ADD_GIALLI_FORMAZ);
    }

    private IRepositoryDocumentoManager _repositoryDocumentoManager;

    private Profilo _profilo;

    private IKeyGeneratorManager _keyManager;

    private IFascicoloManager _fascicoloManager;

    private IDocumentoManager _documentoManager;

    private DocumentoTestHelper _helper;

    private IProfiloManager _profiloManager;

    private static final String FILE_DOCUMENT_PATHNAME = "./admin/test/documentsFile/test.txt";

    private static final String FILE_DOCUMENT_TEMP_PATHNAME = "./admin/test/documentsFile/temp.txt";

    private static final String USERNAME = "test";

    private IRepositoryDocumentoDAO _repositoryDocumentoDAO;
}
