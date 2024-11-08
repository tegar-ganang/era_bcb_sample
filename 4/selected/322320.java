package si.cit.eprojekti.edocs.controller.documentFilesStates;

import si.cit.eprojekti.edocs.DocsSchema;
import si.cit.eprojekti.edocs.dbobj.Document;
import si.cit.eprojekti.edocs.dbobj.DocumentFile;
import si.cit.eprojekti.edocs.util.ComponentSecurityManager;
import com.jcorporate.expresso.services.dbobj.MimeTypes;
import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.Input;
import com.jcorporate.expresso.core.controller.NonHandleableException;
import com.jcorporate.expresso.core.controller.Output;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.controller.Transition;
import com.jcorporate.expresso.services.dbobj.Setup;
import si.cit.eprojekti.edocs.controller.DocumentController;
import si.cit.eprojekti.edocs.controller.documentStates.ErrorState;
import javax.servlet.ServletRequest;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.i18n.Messages;
import org.apache.log4j.Priority;
import com.jcorporate.expresso.core.controller.ServletControllerRequest;
import java.util.*;
import java.io.*;

/**
 *	
 *	Upload Files State - external state for Document Files Controller
 *
 * 	@author taks
 *	@version 1.0
 *
 */
public class UploadFilesState extends State {

    private static final long serialVersionUID = -8043837085219426824L;

    private static org.apache.log4j.Category standardLog = org.apache.log4j.Category.getInstance("pvn.standard.edocs");

    private static org.apache.log4j.Category debugLog = org.apache.log4j.Category.getInstance("pvn.debug.edocs");

    private static org.apache.log4j.Category observerLog = org.apache.log4j.Category.getInstance("pvn.observer.edocs");

    private String fileName = "";

    /**
	 * 	Constructor
	 */
    public UploadFilesState() {
        super();
    }

    /** 
	 * Constructor
	 * @param stateName
	 * @param descrip
	 */
    public UploadFilesState(String stateName, String descrip) {
        super(stateName, descrip);
    }

    /** 
	 * Convert in string characters '\' to characters '/'
	 * 
	 * @param  a
	 * @return	String
	 */
    private String backSlashToSlash(String a) {
        int l = a.length();
        java.lang.StringBuffer _ret = new java.lang.StringBuffer();
        for (int i = 0; i < l; i++) if (a.charAt(i) == '\\') _ret.append('/'); else _ret.append(a.charAt(i));
        return _ret.toString();
    }

    /** 
	 * Copy file from input destination to output destination
	 * @param  in
	 * @param  out 
	 */
    void copyFile(File src, File dst) throws IOException {
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
        response.setFormCache();
        String documentId = "";
        String projectId = "";
        String fileId = "";
        String addAs = "";
        String fromCheckOut = "";
        boolean goToAddAdvanced = false;
        boolean goToDetailFiles = false;
        String types = "(";
        try {
            Transition errorTrans = new Transition();
            errorTrans.setControllerObject(DocumentController.class);
            errorTrans.setState(ErrorState.STATE_NAME);
            errorTrans.setName("errorTrans");
            setErrorTransition(errorTrans);
            short accessLevel = ComponentSecurityManager.checkSecurityForProject(request.getParameter("projectId"), request.getParameter("category"), request.getUid());
            if (accessLevel < ComponentSecurityManager.ACCESS_MEMBER) throw ComponentSecurityManager.accessDeniedOccured(response);
            projectId = request.getParameter("projectId");
            if (request.getParameter("addAs") != null) addAs = request.getParameter("addAs");
            if (request.getParameter("fromCheckOut") != null) fromCheckOut = request.getParameter("fromCheckOut");
            if ((request.getParameter("status") != null) && (request.getParameter("status").equals("upload"))) {
                String docId = request.getParameter("documentId");
                Document docUser = new Document();
                docUser.setField("DocumentId", docId);
                docUser.retrieve();
                docUser.setLocale(request.getLocale());
                if (!docUser.getField("CheckedOutBy").equals("0")) {
                    String name = response.getFormCache("File");
                    name = backSlashToSlash(name);
                    int k = name.lastIndexOf('/');
                    int typ = name.lastIndexOf('.');
                    String type = name.substring(typ + 1).toLowerCase();
                    int flag = 0;
                    MimeTypes docT = new MimeTypes();
                    ArrayList arrayMimes = docT.searchAndRetrieveList();
                    Iterator allRecordIterMime = arrayMimes.iterator();
                    types = "-";
                    String docID = "";
                    while (allRecordIterMime.hasNext()) {
                        docT = (MimeTypes) allRecordIterMime.next();
                        StringTokenizer st = new StringTokenizer(docT.getField("MimeFileExtensions"));
                        while (st.hasMoreTokens()) {
                            String typeEnd = st.nextToken();
                            if (type.toUpperCase().equals(typeEnd.toUpperCase())) {
                                flag = 1;
                                docID = docT.getField("MimeNumber");
                            }
                        }
                        types += docT.getField("MimeFileExtensions") + " ";
                    }
                    types += "-";
                    if (flag == 1) {
                        DocumentFile fid = new DocumentFile();
                        int idd = 1;
                        try {
                            idd = Integer.parseInt(fid.getMax("FileId")) + 1;
                        } catch (Exception e) {
                        }
                        DocumentFile f = new DocumentFile();
                        name = name.substring(k + 1, typ);
                        name = name + "_id_" + idd + "." + type;
                        String fn = request.getFileName("File");
                        Document doc = new Document();
                        doc.setField("DocumentId", docId);
                        doc.retrieve();
                        String fileDirectory = Setup.getValue("", "BaseDir") + Setup.getValue("", "si.cit.eprojekti.edocs.DocsSchema", "docsFileDir");
                        fileDirectory += projectId + "/" + docId + "/" + doc.getField("DocumentVersion") + "/";
                        File isDir = new File(fileDirectory);
                        if (!isDir.isDirectory()) isDir.mkdirs();
                        String nfn = fileDirectory + name;
                        File ff1 = new File(fn);
                        File ff2 = new File(nfn);
                        copyFile(ff1, ff2);
                        fileName = name;
                        if ((request.getParameter("fromDocument") != null) && (request.getParameter("fromDocument").equals("no"))) f.setField("DocumentId", Integer.parseInt(docId));
                        f.setField("ProjectId", projectId);
                        f.setField("FileTitle", request.getParameter("title"));
                        f.setField("FileDesc", request.getParameter("desc"));
                        f.setField("FileName", name);
                        f.setField("TypeId", docID);
                        f.setField("FileVisible", request.getParameter("vis"));
                        f.setField("FileURL", request.getParameter("fUrl"));
                        f.setField("CreatedUid", request.getUid());
                        f.setField("UpdatedUid", request.getUid());
                        f.add();
                        fileId = f.getField("FileId");
                    } else {
                        request.setParameter("status", "notUpload");
                    }
                } else {
                    addError("errors.ExceptionNotLockedDocument");
                }
                if (request.getParameter("fromDocument").equals("no")) goToDetailFiles = true; else goToAddAdvanced = true;
            }
            if ((request.getParameter("status") != null) && (request.getParameter("status").equals("upload"))) {
            } else {
                if ((request.getParameter("status") != null) && (request.getParameter("status").equals("notUpload"))) {
                    String allowedFormats = Messages.getString(DocsSchema.class.getName(), request.getLocale(), "uploadFilesStateAllowedFormats") + ": " + types;
                    response.add(new Output("ErrorFormatType", allowedFormats));
                    goToDetailFiles = false;
                    goToAddAdvanced = false;
                }
                if (request.getParameter("documentId") != null) {
                    documentId = request.getParameter("documentId");
                    Output docIdOutput = new Output();
                    docIdOutput.setName("documentIdHidden");
                    response.add(docIdOutput);
                    Input docId = new Input("documentId");
                    docId.setDefaultValue(request.getParameter("documentId"));
                    docId.setAttribute(Input.ATTRIBUTE_HIDDEN, "true");
                    response.addInput(docId);
                } else {
                    Output docIdOutput = new Output();
                    docIdOutput.setName("documentIdText");
                    response.add(docIdOutput);
                    Input docId = new Input("documentId");
                    docId.setLabel("Document ID");
                    docId.setType("text");
                    response.add(docId);
                }
                Input filePrompt = new Input("File");
                filePrompt.setLabel("Select file");
                filePrompt.setAttribute("file", "");
                filePrompt.setType("file");
                response.add(filePrompt);
                Input fileT = new Input("title");
                fileT.setLabel("Title");
                fileT.setType("text");
                fileT.setDefaultValue(request.getParameter("title"));
                response.addInput(fileT);
                Input fileD = new Input("desc");
                fileD.setLabel("Description");
                fileD.setType("text");
                fileD.setDefaultValue(request.getParameter("desc"));
                response.addInput(fileD);
                DocumentFile docF = new DocumentFile();
                docF.setLocale(request.getLocale());
                Input fileVis = new Input("vis");
                fileVis.setLabel("File visibility");
                fileVis.setValidValues(docF.getValidValues("FileVisible"));
                fileVis.setDefaultValue("V");
                if (request.getParameter("vis") != null) fileVis.setDefaultValue(request.getParameter("vis"));
                fileVis.setType(Input.ATTRIBUTE_CHECKBOX);
                response.addInput(fileVis);
                docF = null;
                Input fileURL = new Input("fUrl");
                fileURL.setLabel("URL address");
                fileURL.setType("text");
                fileURL.setDefaultValue(request.getParameter("fUrl"));
                response.addInput(fileURL);
                Transition uploadButton = new Transition();
                uploadButton.setName("FileUpload");
                uploadButton.setLabel(Messages.getString(DocsSchema.class.getName(), request.getLocale(), "uploadFilesStateUploadFileLabel"));
                uploadButton.setControllerObject(this.getController().getClass());
                uploadButton.setState("uploadFilesState");
                uploadButton.addParam("projectId", projectId);
                uploadButton.addParam("status", "upload");
                uploadButton.addParam("addAs", addAs);
                uploadButton.addParam("fromCheckOut", fromCheckOut);
                if (request.getParameter("documentId") == null) uploadButton.addParam("fromDocument", "no"); else {
                    uploadButton.addParam("fromDocument", "Yes");
                    Transition goBackToAdvanced = new Transition();
                    goBackToAdvanced.setName("GoBackToAdvanced");
                    goBackToAdvanced.setControllerObject(si.cit.eprojekti.edocs.controller.DocumentAddController.class);
                    goBackToAdvanced.setLabel(Messages.getString(DocsSchema.class.getName(), request.getLocale(), "uploadFilesStateBackToAdvancedLabel"));
                    goBackToAdvanced.setState("addAdvancedState");
                    goBackToAdvanced.addParam("documentId", documentId);
                    goBackToAdvanced.addParam("projectId", projectId);
                    goBackToAdvanced.addParam("addAs", addAs);
                    goBackToAdvanced.addParam("fromCheckOut", fromCheckOut);
                    response.add(goBackToAdvanced);
                }
                response.add(uploadButton);
            }
            if (goToAddAdvanced) {
                documentId = request.getParameter("documentId");
                Transition goToAdvanced = new Transition();
                goToAdvanced.setName("GoToAdvenced");
                goToAdvanced.setControllerObject(si.cit.eprojekti.edocs.controller.DocumentAddController.class);
                goToAdvanced.setState("addAdvancedState");
                goToAdvanced.addParam("documentId", documentId);
                goToAdvanced.addParam("projectId", projectId);
                goToAdvanced.addParam("addAs", addAs);
                goToAdvanced.addParam("fromCheckOut", fromCheckOut);
                setSuccessTransition(goToAdvanced);
            }
            if (goToDetailFiles) {
                Transition goToDetail = new Transition();
                goToDetail.setName("GoToDetailFiles");
                goToDetail.setControllerObject(this.getController().getClass());
                goToDetail.setState("detailFilesState");
                goToDetail.addParam("fileId", fileId);
                goToDetail.addParam("projectId", projectId);
                setSuccessTransition(goToDetail);
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
