package si.cit.eprojekti.eprocess.controller.managestates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletRequest;
import org.apache.log4j.Priority;
import si.cit.eprojekti.eprocess.ProcessSchema;
import si.cit.eprojekti.eprocess.controller.ManageController;
import si.cit.eprojekti.eprocess.controller.browsestate.ErrorState;
import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.NonHandleableException;
import com.jcorporate.expresso.core.controller.Output;
import com.jcorporate.expresso.core.controller.ServletControllerRequest;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.controller.Transition;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.services.dbobj.Setup;

/**
 * @author Luka Pavli� (luka.pavlic@cit.si)
 *
 * PromptAddFileState description:
 *	Upload file to /expresso/components folder; works on Windows and Unix - example upload state 
 */
public class ProcessAddFileState extends State {

    private static final long serialVersionUID = 23134546488887987L;

    /**
	 * Constructor
	 */
    public ProcessAddFileState() {
        super("ProcessAddFileState", "ProcessAddFileStateDescription");
    }

    private String backSlashToSlash(String a) {
        int l = a.length();
        java.lang.StringBuffer _ret = new java.lang.StringBuffer();
        for (int i = 0; i < l; i++) if (a.charAt(i) == '\\') _ret.append('/'); else _ret.append(a.charAt(i));
        return _ret.toString();
    }

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
	 * @see com.jcorporate.expresso.core.controller.State#run(com.jcorporate.expresso.core.controller.ControllerRequest, com.jcorporate.expresso.core.controller.ControllerResponse)
	 */
    public void run(ControllerRequest request, ControllerResponse response) throws ControllerException, NonHandleableException {
        super.run(request, response);
        Transition errorTrans = new Transition();
        errorTrans.setControllerObject(ProcessSchema.class);
        errorTrans.setState(ErrorState.STATE_NAME);
        errorTrans.setName("errorTrans");
        setErrorTransition(errorTrans);
        response.setFormCache();
        String origName = response.getFormCache("inFile");
        String backedSlashedName = backSlashToSlash(origName);
        int k = backedSlashedName.lastIndexOf('/');
        if (k == -1) backedSlashedName.lastIndexOf('\\');
        int typ = backedSlashedName.lastIndexOf('.');
        String fileType = backedSlashedName.substring(typ + 1).toLowerCase();
        String newName = backedSlashedName.substring(k + 1, typ);
        newName = newName + "_myNumber_." + fileType;
        String fn = request.getFileName("inFile");
        Output out = new Output("outStatus", "OK");
        out.setLabel("Status");
        response.add(out);
        out = new Output("outOriginalFileName", origName);
        out.setLabel("Original Name");
        response.add(out);
        out = new Output("outUniversalFileName", backedSlashedName);
        out.setLabel("Universal Name");
        response.add(out);
        out = new Output("outFileType", fileType);
        out.setLabel("Type");
        response.add(out);
        out = new Output("outNewName", newName);
        out.setLabel("My Newly Constructed Name");
        response.add(out);
        out = new Output("outNameRreq", fn);
        out.setLabel("Name On Server");
        response.add(out);
        try {
            String fileDirectory = Setup.getValue("", "BaseDir") + "testupload/luka/";
            File isDir = new File(fileDirectory);
            if (!isDir.isDirectory()) isDir.mkdirs();
            out = new Output("outBaseDir", fileDirectory);
            out.setLabel("Base Directory On Server");
            response.add(out);
            String nfn = fileDirectory + newName;
            File ff1 = new File(fn);
            File ff2 = new File(nfn);
            copyFile(ff1, ff2);
        } catch (Exception e) {
            if (e instanceof DBException) addError("errors.DBException"); else if (e.getMessage().equals("errors.accessDeniedOccured")) addError("errors.accessDeniedOccured"); else addError("errors.Exception");
            if (ProcessSchema.standardLog.isEnabledFor(Priority.WARN)) ProcessSchema.standardLog.warn(" :: Exception in \"" + this.getName() + "\" : " + e.toString());
            if (ProcessSchema.debugLog.isDebugEnabled()) ProcessSchema.debugLog.debug(" :: Exception in \"" + this.getName() + "\" : " + e.toString(), e.fillInStackTrace());
        } finally {
            if (ProcessSchema.observerLog.isInfoEnabled()) {
                ServletRequest servletRq = ((ServletControllerRequest) request).getServletRequest();
                ProcessSchema.observerLog.info(" :: Location= " + this.getClass().getName() + " :: UID= " + request.getUid() + " :: User= " + request.getUser() + " :: IP= " + servletRq.getRemoteAddr());
            }
        }
        Transition uploadButton = new Transition("UploadTransition", "Upload again!", ManageController.class, "PromptAddFileState");
        response.add(uploadButton);
    }
}
