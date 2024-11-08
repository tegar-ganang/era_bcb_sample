package org.systemsbiology.apps.corragui.server.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.systemsbiology.apps.corragui.client.constants.OutputFileConstants;
import org.systemsbiology.apps.corragui.domain.User;
import org.systemsbiology.apps.corragui.server.CorraWebappConstants;
import org.systemsbiology.apps.corragui.server.executor.corrastats.CorraStatsConstants;
import org.systemsbiology.apps.corragui.server.executor.targetFeatureAnnotation.TargetFeatureAnnotationConstants;
import org.systemsbiology.apps.corragui.server.manager.InclusionListManager;
import org.systemsbiology.apps.corragui.server.manager.ProjectManager;
import org.systemsbiology.apps.corragui.server.provider.ProviderFactory;
import org.systemsbiology.apps.corragui.server.provider.location.ILocationInfoProvider;
import org.systemsbiology.apps.corragui.server.provider.user.IUserInfoProvider;

public class AnalysisResultsServlet extends HttpServlet {

    private static final long serialVersionUID = -7086915523748524524L;

    private static Logger log = Logger.getLogger(AnalysisResultsServlet.class.getName());

    public static final Pattern randSuffixPattern = Pattern.compile("\\.\\d+$");

    public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        log.debug(req.getQueryString());
        StringBuilder errorMsg = new StringBuilder();
        User user = this.authenticateRequest(req, errorMsg);
        if (user == null) {
            response.setContentType("text/html");
            response.getWriter().println("User not logged in!\n" + errorMsg.toString());
            return;
        }
        String projName = req.getParameter("projName");
        String fileType = req.getParameter("file");
        log.debug("projName is: " + projName + "\nfileName is: " + fileType + "\nuser is: " + user.getLoginName());
        fileType = this.removeRandomNumber(fileType);
        String fileToSend = getFilePath(user, projName, fileType);
        if (fileToSend == null) {
            response.setContentType("text/html");
            response.getWriter().println("Invalid file type requested: " + fileType);
            log.error("Invalid file type requested: " + fileType);
            return;
        }
        if (!(new File(fileToSend).exists())) {
            response.setContentType("text/html");
            response.getWriter().println("File does not exist: " + (new File(fileToSend)).getName());
            log.error("File does not exist: " + fileToSend);
            return;
        }
        log.debug("Sending file: " + fileToSend);
        if (fileType.endsWith("PNG")) response.setContentType("image/png"); else if (fileType.endsWith("PDF")) response.setContentType("application/pdf"); else if (fileType.endsWith("TSV")) response.setContentType("text/tab-separated-values"); else if (fileType.endsWith("APML")) response.setContentType("text/xml"); else response.setContentType("text/html");
        response.setHeader("Content-disposition", "attachment; filename=" + new File(fileToSend).getName());
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            ServletOutputStream outStr = response.getOutputStream();
            in = new BufferedInputStream(new FileInputStream(fileToSend));
            out = new BufferedOutputStream(outStr);
            byte[] buf = new byte[2048];
            int readBytes = 0;
            while ((readBytes = in.read(buf, 0, buf.length)) != -1) {
                out.write(buf, 0, readBytes);
            }
        } catch (IOException e) {
            log.error("Error opening file: " + fileToSend, e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private String getFilePath(User user, String projName, String fileType) {
        String projDir = locationInfoProvider().getUserProjectLocation(user, projName).pathForWebServer();
        ProjectManager manager = ProjectManager.instance();
        InclusionListManager inclManager = InclusionListManager.instance();
        if (fileType.equals(OutputFileConstants.FCOUNT_DISTR_PDF)) {
            return manager.getFeatureCountFile(user, projName, CorraStatsConstants.Output.FC_PDF);
        } else if (fileType.equals(OutputFileConstants.FCOUNT_DISTR_PNG)) {
            return manager.getFeatureCountFile(user, projName, CorraStatsConstants.Output.FC_PNG);
        } else if (fileType.equals(OutputFileConstants.ALIGNED_ORIG)) {
            return manager.getAlignedFeaturesOrig(user, projName, fileType);
        } else if (fileType.equals(OutputFileConstants.ALIGNED_APML)) {
            return manager.getAlignedFeaturesApml(user, projName, fileType);
        } else if (fileType.equals(OutputFileConstants.CLUST_PDF)) {
            return manager.getClusteringFile(user, projName, CorraStatsConstants.Output.HC_PDF);
        } else if (fileType.equals(OutputFileConstants.CLUST_PNG)) {
            return manager.getClusteringFile(user, projName, CorraStatsConstants.Output.HC_PNG);
        } else if (fileType.contains(OutputFileConstants.VOLCANO_PDF)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.VOLCANO_PDF);
            return manager.getVolcanoFile(user, projName, conditionPair, CorraStatsConstants.Output.VOLCANO_PDF);
        } else if (fileType.contains(OutputFileConstants.VOLCANO_PNG)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.VOLCANO_PNG);
            return manager.getVolcanoFile(user, projName, conditionPair, CorraStatsConstants.Output.VOLCANO_PNG);
        } else if (fileType.endsWith(OutputFileConstants.VOLCANO_TSV)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.VOLCANO_TSV);
            return manager.getVolcanoFile(user, projName, conditionPair, CorraStatsConstants.Output.VOLCANO_TSV);
        } else if (fileType.contains(OutputFileConstants.SEGMENTS_TSV)) {
            String listName = extractPrefix(fileType, OutputFileConstants.SEGMENTS_TSV);
            return inclManager.getSegmentsFilePath(listName, inclManager.getInclListDirPath(user, projName));
        } else if (fileType.contains(OutputFileConstants.SEGMENTS_EXPORT_TSV)) {
            String listName = extractPrefix(fileType, OutputFileConstants.SEGMENTS_EXPORT_TSV);
            return inclManager.getExportedSegmentsFilePath(listName, inclManager.getInclListDirPath(user, projName));
        } else if (fileType.contains(OutputFileConstants.INCL_LIST_TSV)) {
            String listName = extractPrefix(fileType, OutputFileConstants.INCL_LIST_TSV);
            return inclManager.getInclListFilePath(listName, inclManager.getInclListDirPath(user, projName));
        } else if (fileType.endsWith(OutputFileConstants.ANNO_TSV)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.ANNO_TSV);
            return manager.getTargetFeatureAnnotationFile(user, projName, conditionPair, TargetFeatureAnnotationConstants.FileSuffix.VOLCANO_ANNO_TSV);
        } else if (fileType.endsWith(OutputFileConstants.IPI_TSV)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.IPI_TSV);
            return manager.getTargetFeatureAnnotationFile(user, projName, conditionPair, TargetFeatureAnnotationConstants.FileSuffix.IPI_TSV);
        } else if (fileType.endsWith(OutputFileConstants.MSSTATS_TOPPROTEINS_TSV)) {
            String conditionPair = extractPrefix(fileType, OutputFileConstants.MSSTATS_TOPPROTEINS_TSV);
            return manager.getTargetFeatureAnnotationFile(user, projName, conditionPair, TargetFeatureAnnotationConstants.FileSuffix.MSSTATS_TOPPROTEINS_TSV);
        } else return null;
    }

    private String extractPrefix(String fullStr, String suffix) {
        int idx = fullStr.indexOf(suffix);
        if (idx != -1) return fullStr.substring(0, idx);
        return fullStr;
    }

    public String removeRandomNumber(String original) {
        Matcher m = randSuffixPattern.matcher(original);
        int idx = -1;
        if (m.find()) {
            idx = m.start();
        }
        if (idx == -1) return original; else return original.substring(0, idx);
    }

    public void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        doGet(arg0, arg1);
    }

    protected User authenticateRequest(HttpServletRequest request, StringBuilder errorMsg) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("Session is null");
            errorMsg.append("Session is null\n");
            return null;
        }
        String user = (String) session.getAttribute(CorraWebappConstants.USER);
        if (user == null) {
            log.debug("User is null");
            errorMsg.append("User in session is null\n");
            return null;
        }
        String reqUser = request.getParameter("user");
        if (reqUser == null) {
            errorMsg.append("User in request is null\n");
            log.debug("User in request is null");
            return null;
        }
        if (!(user.equals(reqUser))) {
            log.debug("User in request (" + reqUser + ") and session (" + user + ") are not the same");
            errorMsg.append("Invalid user\n");
            return null;
        }
        User userObj = userInfoProvider().getUser(reqUser);
        if (userObj == null) {
            log.error("No user found for login name: " + reqUser);
            errorMsg.append("Invalid user\n");
        }
        return userObj;
    }

    private ILocationInfoProvider locationInfoProvider() {
        return ProviderFactory.instance().getLocationInfoProvider();
    }

    private IUserInfoProvider userInfoProvider() {
        return ProviderFactory.instance().getUserInfoProvider();
    }
}
