package si.cit.eprojekti.edocs.controller.documentStates;

import java.util.Vector;
import java.io.*;
import si.cit.eprojekti.edocs.DocsSchema;
import si.cit.eprojekti.edocs.controller.DocumentController;
import si.cit.eprojekti.edocs.dbobj.Activity;
import si.cit.eprojekti.edocs.dbobj.Document;
import si.cit.eprojekti.edocs.dbobj.DocumentFile;
import si.cit.eprojekti.edocs.dbobj.DocumentHistory;
import si.cit.eprojekti.edocs.util.ComponentSecurityManager;
import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.NonHandleableException;
import com.jcorporate.expresso.core.controller.Output;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.controller.Transition;
import com.jcorporate.expresso.services.dbobj.Setup;
import javax.servlet.ServletRequest;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.i18n.Messages;
import org.apache.log4j.Priority;
import com.jcorporate.expresso.core.controller.ServletControllerRequest;

/**
 *	
 *	Check In - external state for Document Controller
 *
 * 	@author taks
 *	@version 1.0
 *
 */
public class CheckIn extends State {

    private static final long serialVersionUID = -8043837085219426824L;

    private static org.apache.log4j.Category standardLog = org.apache.log4j.Category.getInstance("pvn.standard.edocs");

    private static org.apache.log4j.Category debugLog = org.apache.log4j.Category.getInstance("pvn.debug.edocs");

    private static org.apache.log4j.Category observerLog = org.apache.log4j.Category.getInstance("pvn.observer.edocs");

    /**
	 * 	Constructor
	 */
    public CheckIn() {
        super();
    }

    /** 
	 * Constructor
	 * @param stateName
	 * @param descrip
	 */
    public CheckIn(String stateName, String descrip) {
        super(stateName, descrip);
    }

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    /** 
	 *  Run this state
	 */
    public void run(ControllerRequest request, ControllerResponse response) throws ControllerException, NonHandleableException {
        super.run(request, response);
        boolean checkIN = false;
        String projectId = "";
        try {
            Transition errorTrans = new Transition();
            errorTrans.setControllerObject(DocumentController.class);
            errorTrans.setState(ErrorState.STATE_NAME);
            errorTrans.setName("errorTrans");
            setErrorTransition(errorTrans);
            short accessLevel = ComponentSecurityManager.checkSecurityForProject(request.getParameter("projectId"), request.getParameter("category"), request.getUid());
            if (accessLevel < ComponentSecurityManager.ACCESS_MEMBER) throw ComponentSecurityManager.accessDeniedOccured(response);
            projectId = request.getParameter("projectId");
            String checkInType = request.getParameter("type");
            String coment = request.getParameter("coment");
            Document doc = new Document();
            doc.setField("DocumentId", request.getParameter("key"));
            doc.retrieve();
            doc.setLocale(request.getLocale());
            if (doc.getField("CheckedOutBy").equals("0")) {
                Output resultDoc = new Output();
                resultDoc.setName("DocumentResult");
                resultDoc.setDisplayLength(10);
                resultDoc.setContent("The document have already been CHECK-IN");
                response.addOutput(resultDoc);
                Transition goToDetail = new Transition();
                goToDetail.setName("GoToDetailDocument");
                goToDetail.setLabel(Messages.getString(DocsSchema.class.getName(), request.getLocale(), "checkInBackToDetailsLabel"));
                goToDetail.setControllerObject(this.getController().getClass());
                goToDetail.addParam("key", request.getParameter("key"));
                goToDetail.addParam("projectId", projectId);
                goToDetail.setState("detailDocumentState");
                response.add(goToDetail);
            } else {
                if (doc.getField("CheckedOutBy").equals(String.valueOf(request.getUid()))) {
                    String categoryId = doc.getField("CategoryId");
                    String documentTitle = doc.getField("DocumentTitle");
                    String documentBody = doc.getField("DocumentBody");
                    String documentSource = doc.getField("DocumentSource");
                    String authorId = doc.getField("AuthorId");
                    String language = doc.getField("DocumentLanguage");
                    String documentVisible = doc.getField("DocumentVisible");
                    String version = doc.getField("DocumentVersion");
                    int version_int = Integer.parseInt(version) + 1;
                    String tempId = doc.getField("TempVersionDoc");
                    if (checkInType.equals("minor")) {
                        String fileDirectory = Setup.getValue("", "BaseDir") + Setup.getValue("", "si.cit.eprojekti.edocs.DocsSchema", "docsFileDir");
                        fileDirectory += projectId + "/" + doc.getField("DocumentId") + "/";
                        File isDir = new File(fileDirectory + version_int);
                        if (!isDir.isDirectory()) isDir.mkdirs();
                        DocumentFile file = new DocumentFile();
                        file.setLocale(request.getLocale());
                        Vector fileV = new Vector();
                        fileV = file.getFilesWithNoHistory(doc.getFieldInt("DocumentId"));
                        for (int i = 0; i < fileV.size(); i++) {
                            file = new DocumentFile();
                            file.setField("FileId", String.valueOf(fileV.elementAt(i)));
                            file.retrieve();
                            String fn = fileDirectory + version + "/" + file.getField("FileName");
                            String nfn = fileDirectory + version_int + "/" + file.getField("FileName");
                            File ff1 = new File(fn);
                            File ff2 = new File(nfn);
                            if (ff1.canRead()) copyFile(ff1, ff2);
                            File f = new File(fn);
                            if (f.canRead()) f.delete();
                        }
                        Document tmpVerDoc = new Document();
                        tmpVerDoc.setField("DocumentId", tempId);
                        tmpVerDoc.retrieve();
                        tmpVerDoc.setLocale(request.getLocale());
                        String fileDirectoryFrom = Setup.getValue("", "BaseDir") + Setup.getValue("", "si.cit.eprojekti.edocs.DocsSchema", "docsFileDir");
                        fileDirectoryFrom += projectId + "/" + tempId + "/" + tmpVerDoc.getField("DocumentVersion");
                        fileV = file.getFilesWithNoHistory(tmpVerDoc.getFieldInt("DocumentId"));
                        for (int ii = 0; ii < fileV.size(); ii++) {
                            file = new DocumentFile();
                            file.setField("FileId", String.valueOf(fileV.elementAt(ii)));
                            file.retrieve();
                            String fn = fileDirectoryFrom + "/" + file.getField("FileName");
                            String nfn = fileDirectory + version_int + "/" + file.getField("FileName");
                            File ff1 = new File(fn);
                            File ff2 = new File(nfn);
                            if (ff1.canRead()) copyFile(ff1, ff2);
                            File f = new File(fn);
                            if (f.canRead()) f.delete();
                            DocumentFile fileUpdate = new DocumentFile();
                            fileUpdate.setField("FileId", String.valueOf(fileV.elementAt(ii)));
                            fileUpdate.setField("DocumentId", doc.getFieldInt("DocumentId"));
                            fileUpdate.setRequestingUid(request.getUid());
                            fileUpdate.update(true);
                        }
                        Document oldDoc = new Document();
                        oldDoc.setField("DocumentId", tempId);
                        oldDoc.retrieve();
                        oldDoc.setLocale(request.getLocale());
                        doc.setField("CategoryId", oldDoc.getField("CategoryId"));
                        doc.setField("DocumentTitle", oldDoc.getField("DocumentTitle"));
                        doc.setField("DocumentBody", oldDoc.getField("DocumentBody"));
                        doc.setField("DocumentSource", oldDoc.getField("DocumentSource"));
                        doc.setField("AuthorId", oldDoc.getField("AuthorId"));
                        doc.setField("DocumentLanguage", oldDoc.getField("DocumentLanguage"));
                        doc.setField("DocumentVisible", oldDoc.getField("DocumentVisible"));
                        doc.setField("DocumentVersion", String.valueOf(version_int));
                        doc.setField("CheckedOutBy", "0");
                        doc.setField("TempVersionDoc", "0");
                        doc.updateCheckOut();
                        doc.setField("UpdatedUid", request.getUid());
                        doc.updateJustCheckedOutBy();
                        oldDoc.setField("CategoryId", categoryId);
                        oldDoc.setField("DocumentTitle", documentTitle);
                        oldDoc.setField("DocumentBody", documentBody);
                        oldDoc.setField("DocumentSource", documentSource);
                        oldDoc.setField("AuthorId", authorId);
                        oldDoc.setField("DocumentLanguage", language);
                        oldDoc.setField("DocumentVisible", documentVisible);
                        oldDoc.updateCheckOut();
                        oldDoc.setField("CheckedOutBy", "-1");
                        oldDoc.setField("TempVersionDoc", "0");
                        oldDoc.setField("DocumentVersion", version);
                        oldDoc.updateJustCheckedOutBy();
                    } else {
                        Document linkDoc = new Document();
                        linkDoc.setField("DocumentId", doc.getField("TempVersionDoc"));
                        linkDoc.retrieve();
                        linkDoc.setField("CheckedOutBy", "0");
                        linkDoc.setField("TempVersionDoc", "0");
                        linkDoc.setField("CreatedUid", request.getUid());
                        linkDoc.setField("UpdatedUid", request.getUid());
                        linkDoc.updateJustCheckedOutBy();
                        doc.setField("CheckedOutBy", "-1");
                        doc.setField("TempVersionDoc", "0");
                        doc.updateJustCheckedOutBy();
                    }
                    Activity activity = new Activity();
                    activity.setField("DocumentId", request.getParameter("key"));
                    activity.setField("CreatedUid", request.getUid());
                    activity.setField("ActivityType", "2");
                    activity.add();
                    DocumentHistory history = new DocumentHistory();
                    if (checkInType.equals("minor")) {
                        history.setField("DocumentIdOld", request.getParameter("key"));
                        history.setField("DocumentIdNew", tempId);
                    } else {
                        history.setField("DocumentIdOld", tempId);
                        history.setField("DocumentIdNew", request.getParameter("key"));
                    }
                    history.setField("CreatedUid", request.getUid());
                    history.setField("Coment", coment);
                    history.add();
                    checkIN = true;
                } else {
                    Output documentId = new Output();
                    documentId.setName("DocumentID");
                    documentId.setLabel("You don't have permission to CHECK-IN document with ID:");
                    documentId.setDisplayLength(10);
                    documentId.setContent(request.getParameter("key"));
                    response.addOutput(documentId);
                    Transition goToDetail = new Transition();
                    goToDetail.setName("GoToDetailDocument");
                    goToDetail.setLabel(Messages.getString(DocsSchema.class.getName(), request.getLocale(), "checkInBackToDetailsLabel"));
                    goToDetail.setControllerObject(this.getController().getClass());
                    goToDetail.addParam("key", request.getParameter("key"));
                    goToDetail.addParam("projectId", projectId);
                    goToDetail.setState("detailDocumentState");
                    response.add(goToDetail);
                }
            }
            if (checkIN) {
                Transition detail = new Transition();
                detail.setName("Detail");
                detail.setLabel("detail");
                detail.setControllerObject(this.getController().getClass());
                detail.setState("detailDocumentState");
                detail.addParam("key", request.getParameter("key"));
                detail.addParam("projectId", projectId);
                setSuccessTransition(detail);
            }
        } catch (Exception e) {
            if (e instanceof DBException) addError("errors.DBException"); else if (e.getMessage().equals("errors.accessDeniedOccured")) addError("errors.accessDeniedOccured"); else addError("errors.Exception");
            if (standardLog.isEnabledFor(Priority.WARN)) standardLog.warn(" :: Exception in \"" + this.getName() + "\" : " + e.toString());
            if (debugLog.isDebugEnabled()) debugLog.debug(" :: Exception in \"" + this.getName() + "\" : " + e.toString(), e.fillInStackTrace());
        } finally {
            if (observerLog.isInfoEnabled()) {
                ServletRequest servletRq = ((ServletControllerRequest) request).getServletRequest();
                observerLog.info(" :: Location= " + this.getClass().getName() + " :: UID= " + request.getUid() + " :: User= " + request.getUser() + " :: IP= " + servletRq.getRemoteAddr());
            }
        }
    }
}
