package test.com.gestioni.adoc.aps.system.services.registro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import test.com.gestioni.adoc.aps.JcrRepositoryBaseTestCase;
import test.com.gestioni.adoc.aps.system.services.UtilsHelperTest;
import com.gestioni.adoc.aps.system.AdocSystemConstants;
import com.gestioni.adoc.aps.system.services.documento.DocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.inoltro.InoltroDocumento;
import com.gestioni.adoc.aps.system.services.documento.model.Documento;
import com.gestioni.adoc.aps.system.services.protocollo.ProtocolloManager;
import com.gestioni.adoc.aps.system.services.protocollo.model.Protocollo;
import com.gestioni.adoc.aps.system.services.protocollo.registro.IRegistroProtocolloManager;
import com.gestioni.adoc.aps.system.utils.AdocLogger;

public class TestRegistroProtocolloManager extends JcrRepositoryBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
        assertNotNull(_registroProtocolloManager);
    }

    /**
	 * Inserisce due protocolli aventi dall'inizio il documento, e crea il registro protocollo in pdf ....
	 * @throws Throwable
	 */
    public void testPrint() throws Throwable {
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        String fascicolo = "2.03.02-F03-01-07";
        assertTrue(this.createAndInsertDocument(titolo, fascicolo, false, ADD_GIALLI_FORMAZ, null));
        Documento doc = this.loadDocByTitolo(titolo);
        this.inoltraDocument(doc.getId(), ADD_GIALLI_FORMAZ, AdocSystemConstants.INOLTRO_TIPOLOGIA_COMPETENZA, "diretto", ADD_091_PROTOCOLLO);
        List<Integer> inoltriPerProtocollo = this.loadDocumentiLavorazione(ADD_091_PROTOCOLLO);
        assertEquals(1, inoltriPerProtocollo.size());
        InoltroDocumento inoltro = this._inoltroDocumentoManager.getInoltro(inoltriPerProtocollo.get(0));
        assertNotNull(inoltro);
        assertTrue(this.createAndInsertProtocolloUscita(titolo, false, doc, ADD_091_PROTOCOLLO, MODALITA_SPEDIZIONE_OTHER));
        Protocollo protocollo = this.loadProtByOggetto(titolo);
        assertNotNull(protocollo);
        assertEquals(doc.getXML(), protocollo.getDocumento().getXML());
        File fileSaved = this.getFileFromDocumento(doc.getPuntatoreJackRabbit());
        File originalFile = new File(FILE_DOCUMENT_PATHNAME);
        InputStream input = new FileInputStream(fileSaved);
        InputStream fileIn = new FileInputStream(originalFile);
        assertTrue("Ckeck inputstream:", UtilsHelperTest.compareInputStreams(input, fileIn));
        String fileName = this._registroProtocolloManager.print();
        File f = new File(this._registroProtocolloManager.getRegProtocolDiskFolder() + fileName);
        assertTrue(f.exists());
        if (f.exists()) {
            f.delete();
        }
    }

    private File getFileFromDocumento(String uuid) throws Throwable {
        File out = null;
        String user = uuid;
        InputStream in = _documentoManager.getDocumentFile(user, uuid);
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

    private void init() throws Exception {
        this._registroProtocolloManager = (IRegistroProtocolloManager) this.getService(AdocSystemConstants.REGISTRO_PROTOCOLLO_MANAGER);
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
            e.printStackTrace();
        } finally {
            super.tearDown();
        }
    }

    private IRegistroProtocolloManager _registroProtocolloManager;

    private static final String FILE_DOCUMENT_PATHNAME = "./admin/test/documentsFile/test.txt";

    private static final String FILE_DOCUMENT_TEMP_PATHNAME = "./admin/test/documentsFile/temp.txt";
}
