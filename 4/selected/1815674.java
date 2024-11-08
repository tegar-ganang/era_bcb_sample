package si.cit.eprojekti.enews.controller.newsFilesStates;

import si.cit.eprojekti.enews.NewsSchema;
import si.cit.eprojekti.enews.controller.NewsController;
import si.cit.eprojekti.enews.controller.newsStates.ErrorState;
import si.cit.eprojekti.enews.dbobj.News;
import si.cit.eprojekti.enews.dbobj.NewsFiles;
import si.cit.eprojekti.enews.util.ComponentSecurityManager;
import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.Input;
import com.jcorporate.expresso.core.controller.NonHandleableException;
import com.jcorporate.expresso.core.controller.Output;
import com.jcorporate.expresso.core.controller.ServletControllerRequest;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.controller.Transition;
import com.jcorporate.expresso.services.dbobj.Setup;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.dbobj.ValidValue;
import com.jcorporate.expresso.core.i18n.Messages;
import org.apache.log4j.Priority;
import java.util.*;
import java.io.*;
import javax.servlet.ServletRequest;

/**
 *	
 *	Upload Files State - external state for News Files Controller
 *
 * 	@author taks
 *	@version 1.0
 *
 */
public class UploadFilesState extends State {

    private static final long serialVersionUID = -717729524307195364L;

    private static org.apache.log4j.Category standardLog = org.apache.log4j.Category.getInstance("pvn.standard.enews");

    private static org.apache.log4j.Category debugLog = org.apache.log4j.Category.getInstance("pvn.debug.enews");

    private static org.apache.log4j.Category observerLog = org.apache.log4j.Category.getInstance("pvn.observer.enews");

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
        String newsId = "";
        String fileId = "";
        String projectId = "";
        boolean goToAddAdvanced = false;
        boolean goToDetailFiles = false;
        String errorName = "";
        long pictureSizeLong = 0;
        String types = "";
        try {
            int flag = 0;
            Transition errorTrans = new Transition();
            errorTrans.setControllerObject(NewsController.class);
            errorTrans.setState(ErrorState.STATE_NAME);
            setErrorTransition(errorTrans);
            short accessLevel = ComponentSecurityManager.checkSecurityForProject(request.getParameter("projectId"), request.getParameter("category"), request.getUid());
            if (accessLevel < ComponentSecurityManager.ACCESS_MEMBER) throw ComponentSecurityManager.accessDeniedOccured(response);
            newsId = request.getParameter("newsId");
            projectId = request.getParameter("projectId");
            String pictureSize = Setup.getValue("", "si.cit.eprojekti.enews.NewsSchema", "newsMaxPictureSize");
            if (pictureSize == null) pictureSize = "100000";
            pictureSizeLong = Long.parseLong(pictureSize);
            long maxSize = pictureSizeLong / 1000;
            String maxSizeString = Messages.getString(NewsSchema.class.getName(), request.getLocale(), "uploadFilesStatePictureSizeMax") + maxSize + " kB!";
            response.add(new Output("PictureSizeMax", maxSizeString));
            if ((request.getParameter("status") != null) && (request.getParameter("status").equals("upload"))) {
                String name = response.getFormCache("File");
                name = backSlashToSlash(name);
                int k = name.lastIndexOf('/');
                int typ = name.lastIndexOf('.');
                String type = name.substring(typ + 1).toLowerCase();
                types = "(";
                NewsFiles newsF = new NewsFiles();
                Vector value = new Vector();
                value = newsF.getValidValues("FileType");
                for (int i = 0; i < value.size(); i++) {
                    ValidValue vv = (ValidValue) value.get(i);
                    if (i == value.size() - 1) types += vv.getValue() + ")"; else types += vv.getValue() + ", ";
                    if (vv.getValue().toUpperCase().equals(type.toUpperCase())) flag = 1;
                }
                newsF = null;
                String fnSize = request.getFileName("File");
                File fileSize = new File(fnSize);
                if (fileSize.length() > pictureSizeLong) {
                    errorName = "errorPictureSize";
                    flag = 0;
                }
                if (flag == 1) {
                    NewsFiles fid = new NewsFiles();
                    int idd = 1;
                    try {
                        idd = Integer.parseInt(fid.getMax("FileId")) + 1;
                    } catch (Exception e) {
                    }
                    NewsFiles f = new NewsFiles();
                    name = name.substring(k + 1, typ);
                    name = name + "_id_" + idd + "." + type;
                    String fn = request.getFileName("File");
                    String fileDirectory = Setup.getValue("", "BaseDir") + Setup.getValue("", "si.cit.eprojekti.enews.NewsSchema", "newsFileDir");
                    fileDirectory += projectId + "/";
                    File isDir = new File(fileDirectory);
                    if (!isDir.isDirectory()) isDir.mkdirs();
                    String nfn = fileDirectory + name;
                    File ff1 = new File(fn);
                    File ff2 = new File(nfn);
                    copyFile(ff1, ff2);
                    fileName = name;
                    f.setField("ProjectId", projectId);
                    f.setField("FileTitle", request.getParameter("title"));
                    f.setField("FileDescription", request.getParameter("desc"));
                    f.setField("FileName", name);
                    f.setField("FileType", type);
                    f.setField("FileVisible", request.getParameter("vis"));
                    f.setField("PictureFormat", request.getParameter("format"));
                    f.setField("CreatedUid", request.getUid());
                    f.setField("UpdatedUid", request.getUid());
                    f.add();
                    if (request.getParameter("fromNews").equals("no")) {
                        fileId = f.getField("FileId");
                        goToDetailFiles = true;
                    } else {
                        News news = new News();
                        news.setField("NewsId", newsId);
                        news.setField("FileId", f.getField("FileId"));
                        news.update(true);
                        goToAddAdvanced = true;
                    }
                } else {
                    request.setParameter("status", "notUpload");
                    if (!errorName.equals("errorPictureSize")) errorName = "errorType";
                }
            }
            if ((request.getParameter("status") != null) && (request.getParameter("status").equals("upload"))) {
            } else {
                if ((request.getParameter("status") != null) && (request.getParameter("status").equals("notUpload"))) {
                    if (errorName.equals("errorType")) {
                        String allowedFormats = Messages.getString(NewsSchema.class.getName(), request.getLocale(), "uploadFilesStateAllowedFormats") + ": " + types;
                        response.add(new Output("ErrorFormatType", allowedFormats));
                    }
                    if (errorName.equals("errorPictureSize")) {
                        String errorFormats = Messages.getString(NewsSchema.class.getName(), request.getLocale(), "uploadFilesStatePictureSize");
                        response.add(new Output("ErrorPictureSize", errorFormats));
                    }
                }
                Input filePrompt = new Input("File");
                filePrompt.setLabel("Select file");
                filePrompt.setAttribute("file", "");
                filePrompt.setType("file");
                response.add(filePrompt);
                Input fileT = new Input("title");
                fileT.setLabel("Title of file uploaded");
                fileT.setType("text");
                fileT.setDefaultValue(request.getParameter("title"));
                response.addInput(fileT);
                Input fileD = new Input("desc");
                fileD.setLabel("Description of file");
                fileD.setType("text");
                fileD.setDefaultValue(request.getParameter("desc"));
                response.addInput(fileD);
                NewsFiles newsF = new NewsFiles();
                newsF.setLocale(request.getLocale());
                Input fileVis = new Input("vis");
                fileVis.setLabel("File visibility");
                fileVis.setValidValues(newsF.getValidValues("FileVisible"));
                fileVis.setDefaultValue("T");
                if (request.getParameter("vis") != null) fileVis.setDefaultValue(request.getParameter("vis"));
                fileVis.setType(Input.ATTRIBUTE_CHECKBOX);
                response.addInput(fileVis);
                Input filePF = new Input("format");
                filePF.setLabel("Picture format");
                filePF.setValidValues(newsF.getValidValues("PictureFormat"));
                filePF.setDefaultValue("200");
                if (request.getParameter("format") != null) filePF.setDefaultValue(request.getParameter("format"));
                response.addInput(filePF);
                Transition uploadButton = new Transition();
                uploadButton.setName("FileUpload");
                uploadButton.setLabel(Messages.getString(NewsSchema.class.getName(), request.getLocale(), "uploadFilesStateUploadFileLabel"));
                uploadButton.setControllerObject(this.getController().getClass());
                uploadButton.setState("uploadFilesState");
                uploadButton.addParam("status", "upload");
                uploadButton.addParam("projectId", projectId);
                if (request.getParameter("newsId") == null) uploadButton.addParam("fromNews", "no"); else {
                    uploadButton.addParam("fromNews", "Yes");
                    uploadButton.addParam("newsId", newsId);
                    Transition goBackToAdvanced = new Transition();
                    goBackToAdvanced.setName("GoBackToAdvanced");
                    goBackToAdvanced.setLabel(Messages.getString(NewsSchema.class.getName(), request.getLocale(), "uploadFilesStateBackToAdvancedLabel"));
                    goBackToAdvanced.setControllerObject(si.cit.eprojekti.enews.controller.NewsAddController.class);
                    goBackToAdvanced.setState("addAdvancedState");
                    goBackToAdvanced.addParam("newsId", newsId);
                    goBackToAdvanced.addParam("projectId", projectId);
                    response.add(goBackToAdvanced);
                }
                response.add(uploadButton);
            }
            if (goToAddAdvanced) {
                Transition goToAdvanced = new Transition();
                goToAdvanced.setName("GoToAdvenced");
                goToAdvanced.setControllerObject(si.cit.eprojekti.enews.controller.NewsAddController.class);
                goToAdvanced.setState("addAdvancedState");
                goToAdvanced.addParam("newsId", newsId);
                goToAdvanced.addParam("projectId", projectId);
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
