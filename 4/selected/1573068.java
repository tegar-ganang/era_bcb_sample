package test.com.gestioni.adoc.apsadmin.documento.personale;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import test.com.gestioni.adoc.aps.system.services.documento.personale.helper.DocumentoPersonaleTestHelper;
import test.com.gestioni.adoc.apsadmin.JcrRepositoryAdocApsAdminBaseTestCase;
import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.gestioni.adoc.aps.system.AdocSystemConstants;
import com.gestioni.adoc.aps.system.services.documento.DocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.IDocumentoManager;
import com.gestioni.adoc.aps.system.services.documento.personale.DocumentoPersonale;
import com.gestioni.adoc.aps.system.services.documento.personale.IDocumentoPersonaleManager;
import com.gestioni.adoc.aps.system.services.profilo.IProfiloManager;
import com.gestioni.adoc.aps.system.services.profilo.Profilo;
import com.gestioni.adoc.aps.system.services.protocollo.ProtocolloManager;
import com.gestioni.adoc.aps.system.services.protocollo.model.Protocollo;
import com.gestioni.adoc.aps.system.utils.AdocLogger;
import com.gestioni.adoc.apsadmin.documento.personale.DocumentoPersonaleAction;
import com.opensymphony.xwork2.Action;

public class TestDocumentoPersonaleAction extends JcrRepositoryAdocApsAdminBaseTestCase {

    public void testNew() throws Throwable {
        String result = this.executeNewDocument(USER);
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.ADD, action.getStrutsAction());
    }

    public void testNewDocumentStep2_NullFile() throws Throwable {
        String result = this.executeNewDocumentStep2(USER, null);
        assertEquals(Action.INPUT, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        List<String> errors = (List<String>) action.getActionErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.contains(action.getText("Error.documentoPersonale.nullFile")));
    }

    public void testNewDocumentStep2() throws Throwable {
        File file = getCloneFile();
        System.out.println("file " + file.exists());
        String result = this.executeNewDocumentStep2(USER, file);
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.ADD, action.getStrutsAction());
        assertNotNull(action.getDoc());
    }

    public void testNewDocumentFromScannerStep2() throws Throwable {
        String fileName = "testScanner.pdf";
        String filePath = "FILE_DOCUMENT_PATHNAME_FROM_SCANNER";
        File f = new File(filePath + fileName);
        f.createNewFile();
        String result = this.executeNewDocumentFromScannerStep2(USER, fileName, filePath);
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.ADD, action.getStrutsAction());
        assertNotNull(action.getDoc());
        if (f.exists()) {
            f.delete();
        }
    }

    public void testSaveWithFieldErrors() throws Throwable {
        File file = getCloneFile();
        Map<String, Object> params = new HashMap<String, Object>();
        String result = this.executeSave(USER, file, params, ApsAdminSystemConstants.ADD);
        assertEquals(Action.INPUT, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        Map<String, List<String>> filedErrors = action.getFieldErrors();
        assertEquals(4, filedErrors.size());
        assertTrue(filedErrors.containsKey("documentoPersonale.titolo"));
        assertTrue(filedErrors.containsKey("documentoPersonale.sunto"));
        assertTrue(filedErrors.containsKey("documentoPersonale.dettaglio"));
        assertTrue(filedErrors.containsKey("documentoPersonale.startDate"));
    }

    public void testSaveOk() throws Throwable {
        String documentId = null;
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        File file = getCloneFile();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("documentoPersonale.titolo", titolo);
        params.put("documentoPersonale.sunto", "Sunto testSaveOk");
        params.put("documentoPersonale.dettaglio", "Dettaglio testSaveOk");
        params.put("documentoPersonale.tags", "test, ciao");
        params.put("documentoPersonale.startDate", "23/09/2010");
        String result = this.executeSave(USER, file, params, ApsAdminSystemConstants.ADD);
        assertEquals(Action.SUCCESS, result);
        EntitySearchFilter profiloFilter = new EntitySearchFilter(IDocumentoManager.PROFILO_FILTER_KEY, false, ((Integer) ADD_GIALLI_FORMAZ).toString(), false);
        EntitySearchFilter titoloFilter = new EntitySearchFilter(IDocumentoManager.TITOLO_FILTER_KEY, false, titolo, false);
        EntitySearchFilter[] filters = { profiloFilter, titoloFilter };
        List<String> docsId = this._documentoPersonaleManager.loadDocumentiId(filters);
        assertEquals(1, docsId.size());
    }

    public void testTrashInexistent() throws Throwable {
        String result = this.executeTrash(USER, "1000");
        assertEquals(Action.INPUT, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        List<String> actionErrors = (List<String>) action.getActionErrors();
        assertEquals(1, actionErrors.size());
        assertTrue(actionErrors.contains(action.getText("Error.documentoPersonale.null")));
    }

    public void testTrashNotAutore() throws Throwable {
        String result = this.executeTrash(USER, "1000");
        assertEquals(Action.INPUT, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        List<String> actionErrors = (List<String>) action.getActionErrors();
        assertEquals(1, actionErrors.size());
        assertTrue(actionErrors.contains(action.getText("Error.documentoPersonale.null")));
    }

    public void testTrashOk() throws Throwable {
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        assertTrue(this.createAndInsertDocumentoPersonale(USER, titolo));
        DocumentoPersonale doc = this.loadDocumentoPersonaleByTitolo(titolo);
        assertNotNull(doc);
        String result = this.executeTrash(USER, doc.getId());
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.DELETE, action.getStrutsAction());
    }

    public void testEditOk() throws Throwable {
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        assertTrue(this.createAndInsertDocumentoPersonale(USER, titolo));
        DocumentoPersonale doc = this.loadDocumentoPersonaleByTitolo(titolo);
        assertNotNull(doc);
        String result = this.executeEdit(USER, doc.getId());
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.EDIT, action.getStrutsAction());
    }

    public void testEditNotAutore() throws Throwable {
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        assertTrue(this.createAndInsertDocumentoPersonale(ADD_VIOLA_GEST_FORN, titolo));
        DocumentoPersonale doc = this.loadDocumentoPersonaleByTitolo(titolo);
        assertNotNull(doc);
        String result = this.executeEdit(USER, doc.getId());
        assertEquals(Action.INPUT, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(1, action.getActionErrors().size());
    }

    public void testDelete() throws Throwable {
        String titolo = "DELETE_" + this.getClass().getSimpleName();
        assertTrue(this.createAndInsertDocumentoPersonale(USER, titolo));
        DocumentoPersonale doc = this.loadDocumentoPersonaleByTitolo(titolo);
        assertNotNull(doc);
        DocumentoPersonale gialli = this._documentoPersonaleManager.getDocument(doc.getId());
        String result = this.executeDelete(USER, gialli.getId(), ApsAdminSystemConstants.DELETE);
        assertEquals(Action.SUCCESS, result);
        doc = this.loadDocumentoPersonaleByTitolo(titolo);
        assertNull(doc);
    }

    public void testNewDocumentFromScanner_2() throws Throwable {
        String result;
        String fileName = "testFromScanner.jpg";
        String filePath = FILE_DOCUMENT_PATHNAME_FROM_SCANNER;
        File file = createFileTest(FILE_DOCUMENT_PATHNAME_FROM_SCANNER, "testFromScanner.jpg");
        int l = (int) file.length();
        result = executeNewDocumentFromScannerStep2(USER, fileName, filePath);
        assertEquals(Action.SUCCESS, result);
        DocumentoPersonaleAction action = (DocumentoPersonaleAction) this.getAction();
        assertEquals(ApsAdminSystemConstants.ADD, action.getStrutsAction());
        assertNotNull(action.getDoc());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("documentoPersonale.titolo", "DELETE_" + this.getClass().getSimpleName());
        params.put("documentoPersonale.sunto", "Sunto testSaveOk");
        params.put("documentoPersonale.dettaglio", "Dettaglio testSaveOk");
        params.put("documentoPersonale.tags", "test, ciao");
        params.put("documentoPersonale.startDate", "23/09/2010");
        result = this.executeSave(USER, file, params, ApsAdminSystemConstants.ADD);
        assertEquals(Action.SUCCESS, result);
        EntitySearchFilter profiloFilter = new EntitySearchFilter(IDocumentoManager.PROFILO_FILTER_KEY, false, ((Integer) ADD_GIALLI_FORMAZ).toString(), false);
        EntitySearchFilter titoloFilter = new EntitySearchFilter(IDocumentoManager.TITOLO_FILTER_KEY, false, "DELETE_" + this.getClass().getSimpleName(), false);
        EntitySearchFilter[] filters = { profiloFilter, titoloFilter };
        List<String> docsId = this._documentoPersonaleManager.loadDocumentiId(filters);
        System.out.println("id" + docsId.get(0));
        assertEquals(1, docsId.size());
        String id = docsId.get(0);
        DocumentoPersonale documentoPersonale = this._documentoPersonaleManager.getDocument(id);
        InputStream in = this._documentoPersonaleManager.getDocumentFile(String.valueOf(ADD_GIALLI_FORMAZ), documentoPersonale.getPuntatoreJackRabbit());
        File docFile = this.createFromIS(in);
        assertEquals(docFile.length(), l);
        assertTrue(docFile.exists());
        if (docFile.exists()) {
            docFile.delete();
        }
    }

    private File createFromIS(InputStream inputStream) throws Throwable {
        File f = new File(FILE_DOCUMENT_PATHNAME_FROM_SCANNER + "outFile.jpg");
        OutputStream out = new FileOutputStream(f);
        byte buf[] = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        out.close();
        inputStream.close();
        return f;
    }

    private File createFileTest(String path, String fileName) throws ApsSystemException {
        File testImg = null;
        try {
            BufferedImage bufferedImage = new BufferedImage(900, 900, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(30, 20, 900, 900);
            g2d.setColor(Color.black);
            g2d.dispose();
            testImg = new File(path + fileName);
            RenderedImage rendImage = bufferedImage;
            ImageIO.write(rendImage, "jpg", testImg);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "createFileTest");
            throw new ApsSystemException("Errore in creazione file per scanner");
        }
        return testImg;
    }

    private String executeNewDocument(int idProfilo) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "new");
        return this.executeAction();
    }

    private String executeNewDocumentStep2(int idProfilo, File file) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "newDocument_2");
        this.addParameter("strutsAction", ApsAdminSystemConstants.ADD);
        if (null != file) {
            this.addParameter("doc", file);
            this.addParameter("docFileName", file.getName());
        }
        return this.executeAction();
    }

    private String executeNewDocumentFromScannerStep2(int idProfilo, String fileName, String filePath) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "newDocumentFromScanner_2");
        this.addParameter("docFileName", fileName);
        this.addParameter("docTempFilePath", filePath);
        return this.executeAction();
    }

    private String executeSave(int idProfilo, File file, Map<String, Object> params, int strutsAction) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "save");
        this.addParameter("strutsAction", strutsAction);
        this.addParameters(params);
        if (null != file) {
            this.addParameter("doc", file);
            this.addParameter("docFileName", file.getName());
            this.addParameter("docTempFilePath", file.getPath());
        }
        return this.executeAction();
    }

    private String executeTrash(int idProfilo, String docId) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "trash");
        this.addParameter("selectedDocument", docId);
        return this.executeAction();
    }

    private String executeDelete(int idProfilo, String docId, int strutsAction) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "delete");
        this.addParameter("selectedDocument", docId);
        this.addParameter("strutsAction", strutsAction);
        return this.executeAction();
    }

    private String executeEdit(int idProfilo, String docId) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "edit");
        this.addParameter("selectedDocument", docId);
        return this.executeAction();
    }

    private String executeNewDownload(int idProfilo) throws Throwable {
        this.setUserOnSession(this._profiloManager.getProfilo(idProfilo).getUsername(), idProfilo);
        this.initAction(NS, "download");
        return this.executeAction();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            List<String> docPersonali = this.searchDocumentiPersonaliByTitolo("DELETE_", true);
            AdocLogger.logDebug("Trovati " + docPersonali.size() + " docPersonali da cancellare", this);
            for (int i = 0; i < docPersonali.size(); i++) {
                String docPersId = docPersonali.get(i);
                this._documentoPersonaleManager.deleteDocument(USER.toString(), docPersId);
                AdocLogger.logDebug("RIMOSSO DOCUMENTO PERSONALE " + docPersId, this);
            }
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

    public File getCloneFile() throws Throwable {
        File f1 = new File(FILE_DOCUMENT_PATHNAME);
        File f2 = new File(FILE_DOCUMENT_TEMP_PATHNAME);
        InputStream in = new FileInputStream(f1);
        OutputStream out = new FileOutputStream(f2);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        return f2;
    }

    private static final String FILE_DOCUMENT_PATHNAME_FROM_SCANNER = "./admin/test/";

    private static final String FILE_DOCUMENT_TEMP_PATHNAME = "./build/test/test.txt";

    private static final String NS = "/do/Documento/Personale";

    private static final Integer USER = ADD_GIALLI_FORMAZ;
}
