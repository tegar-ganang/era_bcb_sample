package org.systemsbiology.apps.gui.server.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.systemsbiology.apps.gui.domain.ATAQSProject;
import org.systemsbiology.apps.gui.domain.CandidateTransition;
import org.systemsbiology.apps.gui.domain.TransitionListGeneratorSetup;
import org.systemsbiology.apps.gui.domain.User;
import org.systemsbiology.apps.gui.server.MediumBlobFile;
import org.systemsbiology.apps.gui.server.MediumClobFile;
import org.systemsbiology.apps.gui.server.WebappConstants;
import org.systemsbiology.apps.gui.server.executor.TransitionListGenerator.TransitionListGeneratorPreparer;
import org.systemsbiology.apps.gui.server.provider.ProviderFactory;
import org.systemsbiology.apps.gui.server.provider.lob.ILobProvider;
import org.systemsbiology.apps.gui.server.provider.project.IProjectInfoProvider;
import org.systemsbiology.apps.gui.server.provider.project.ProjectInfoProvider;
import org.systemsbiology.apps.gui.server.provider.user.IUserInfoProvider;

/**
 * File download servlet
 * 
 * @author Mark Christiansen
 * @author Chris Kwok
 */
public class FileDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1414135753952065445L;

    private static Logger log = Logger.getLogger(FileDownloadServlet.class.getName());

    /**
     * File names in image file requests will end in digits, e.g. XXXX_TYPE.123445
     * This is done to avoid caching of images on the client.
     */
    public static final Pattern randSuffixPattern = Pattern.compile("\\.\\d+$");

    private InputStream inputStream = null;

    public void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        log.debug(req.getQueryString());
        StringBuilder errorMsg = new StringBuilder();
        User user = this.authenticateRequest(req, errorMsg);
        if (user == null) {
            response.setContentType("text/html");
            response.getWriter().println("User not logged in!\n" + errorMsg.toString());
            return;
        }
        String fileType = req.getParameter("fileType");
        fileType = this.removeRandomNumber(fileType);
        log.debug("\nfileType is: " + fileType + "\nuser is: " + user.getUserName());
        initInputStream(req, response, fileType);
        if (this.inputStream == null) {
            response.setContentType("text/html");
            response.getWriter().println("Invalid file type requested: " + fileType);
            log.error("Invalid file type requested: " + fileType);
            return;
        }
        if (fileType.endsWith("PNG")) response.setContentType("image/png"); else if (fileType.endsWith("PDF")) response.setContentType("application/pdf"); else if (fileType.endsWith("TSV")) response.setContentType("text/tab-separated-values"); else if (fileType.endsWith("CSV")) response.setContentType("text/comma-separated-values"); else if (fileType.endsWith("TraML")) response.setContentType("text/xml"); else if (fileType.endsWith("APML")) response.setContentType("text/xml"); else response.setContentType("text/html");
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            ServletOutputStream outStr = response.getOutputStream();
            in = new BufferedInputStream(this.inputStream);
            out = new BufferedOutputStream(outStr);
            byte[] buf = new byte[2048];
            int readBytes = 0;
            while ((readBytes = in.read(buf, 0, buf.length)) != -1) {
                out.write(buf, 0, readBytes);
            }
        } catch (IOException e) {
            log.error("Error opening file:", e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private void initInputStream(HttpServletRequest req, HttpServletResponse response, String fileType) {
        if (req.getParameter("fileId") != null) {
            String fileId = req.getParameter("fileId");
            log.debug("fileId is: " + fileId);
            if (fileId == null) return;
            if (fileType.endsWith("TraML")) {
                MediumBlobFile blobFile = getMediumBlobFile(fileId);
                if (blobFile == null) return;
                response.setHeader("Content-disposition", "attachment; filename=" + blobFile.getFileName());
                this.inputStream = getGzipStream(blobFile);
            } else {
                MediumBlobFile blobFile = getMediumBlobFile(fileId);
                response.setHeader("Content-disposition", "attachment; filename=" + blobFile.getFileName());
                this.inputStream = getBinaryStream(blobFile);
            }
        } else if (req.getParameter("projectId") != null) {
            String projectid = req.getParameter("projectId");
            String user = req.getParameter("user");
            ATAQSProject project = this.getProject(projectid);
            int pipelineStep = Integer.parseInt(req.getParameter("pipelineStep"));
            if (pipelineStep == 3) {
                String outputFileName = user + "_" + project.getTitle() + "_generator.csv";
                response.setHeader("Content-disposition", "attachment; filename=" + outputFileName);
                TransitionListGeneratorSetup tlgSetup = this.projectInfoProvider().getTransitionListGeneratorSetup(project);
                this.inputStream = getCandidateTransitionStream(tlgSetup);
            }
        }
    }

    private MediumBlobFile getMediumBlobFile(String fileId) {
        Long id = Long.parseLong(fileId);
        MediumBlobFile blobFile = lobProvider().getMediumBlobFile(id);
        return blobFile;
    }

    private MediumClobFile getMediumClobFile(String fileId) {
        Long id = Long.parseLong(fileId);
        MediumClobFile clobFile = lobProvider().getMediumClobFile(id);
        return clobFile;
    }

    private InputStream getBinaryStream(MediumBlobFile blobFile) {
        InputStream is = null;
        Blob blob = blobFile.getData();
        try {
            is = blob.getBinaryStream();
        } catch (SQLException e) {
            log.error(e);
        }
        return is;
    }

    private InputStream getAsciiStream(MediumClobFile clobFile) {
        InputStream is = null;
        Clob clob = clobFile.getData();
        try {
            is = clob.getAsciiStream();
        } catch (SQLException e) {
            log.error(e);
        }
        return is;
    }

    private InputStream getGzipStream(MediumBlobFile blobFile) {
        InputStream is = null;
        GZIPInputStream gzStream = null;
        Blob blob = blobFile.getData();
        try {
            is = blob.getBinaryStream();
            gzStream = new GZIPInputStream(is);
        } catch (SQLException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
        }
        return gzStream;
    }

    private ATAQSProject getProject(String projectId) {
        Long id = Long.parseLong(projectId);
        ATAQSProject project = ProjectInfoProvider.instance().getUserProject(id);
        return project;
    }

    private InputStream getCandidateTransitionStream(TransitionListGeneratorSetup tlgSetup) {
        Set<CandidateTransition> candidateTransitions = tlgSetup.getCandidateTransitions();
        File fileName;
        FileInputStream fis = null;
        try {
            fileName = File.createTempFile(FileDownloadServlet.class.getSimpleName(), ".csv");
            TransitionListGeneratorPreparer.writeToFile(new ArrayList<CandidateTransition>(candidateTransitions), fileName.getAbsolutePath());
            fis = new FileInputStream(fileName);
        } catch (IOException e) {
            log.error(e);
        }
        return fis;
    }

    /**
     * Remove randomly assigned number from string
     * @param original original string
     * @return string without random number 
     */
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
        String user = (String) session.getAttribute(WebappConstants.USER);
        if (user == null) {
            log.debug("AnalysisResultsServelet: User is null");
            errorMsg.append("AnalysisResultsServelet: User in session is null\n");
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

    private IUserInfoProvider userInfoProvider() {
        return ProviderFactory.instance().getUserInfoProvider();
    }

    private ILobProvider lobProvider() {
        return ProviderFactory.instance().getLobProvider();
    }

    private IProjectInfoProvider projectInfoProvider() {
        return ProviderFactory.instance().getProjectInfoProvider();
    }
}
