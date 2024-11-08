package org.commonlibrary.lcms.web.springmvc.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlibrary.lcms.learningobject.service.LearningObjectService;
import org.commonlibrary.lcms.model.Attachment;
import org.commonlibrary.lcms.model.Content;
import org.commonlibrary.lcms.model.LearningObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Andrea Ramirez
 * @author Rodrigo Bartels
 *         Date: Oct 10, 2008
 *         Time: 10:59:43 AM
 *         <p/>
 */
@org.springframework.stereotype.Controller
public class ContentController {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    @Qualifier("learningObjectService")
    private LearningObjectService learningObjectService;

    @PostConstruct
    public void init() {
        String msg = "%s property must be set";
        if (this.learningObjectService == null) {
            throw new IllegalStateException(String.format(msg, "learningObjectService"));
        }
    }

    public void setLearningObjectService(LearningObjectService learningObjectService) {
        this.learningObjectService = learningObjectService;
    }

    /**
     * Retrieves the Content assocciated to the given <code>learningObjectId</code>
     * @param request
     * @param response
     * @return the content associated with the learningObjectId
     */
    @RequestMapping("/content.spr")
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentController");
        String learningObjectId = request.getParameter("learningObjectId");
        Content content = learningObjectService.getContentByLearningObjectId(learningObjectId);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content was found for learning object id " + learningObjectId);
            return null;
        }
    }

    /**
     * Retrieves the Content assocciated to the given version of a Learning Object
     * @param request
     * @param response
     * @return the content associated with the learningObjectId
     */
    @RequestMapping("/contentbyVersion.spr")
    public ModelAndView getContentVersion(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentController by Version");
        String learningObjectId = request.getParameter("learningObjectId");
        long versionNumber = Long.parseLong(request.getParameter("versionNumber"));
        LearningObject learningObject = learningObjectService.findById(learningObjectId);
        Content content = learningObjectService.getVersion(learningObject, versionNumber);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content was found for learning object id " + learningObjectId);
            return null;
        }
    }

    /**
     * Retrieves the Content assocciated to the last version of a Learning Object
     * @param request
     * @param response
     * @return the content associated with the learningObjectId
     */
    @RequestMapping("/contentLastVersion.spr")
    public ModelAndView getContentOfLastVersion(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentController by Last Version");
        String learningObjectId = request.getParameter("learningObjectId");
        LearningObject learningObject = learningObjectService.findById(learningObjectId);
        long versionNumber = learningObjectService.getNumberOfVersions(learningObject);
        Content content = learningObjectService.getVersion(learningObject, versionNumber);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content was found for learning object id " + learningObjectId);
            return null;
        }
    }

    @RequestMapping("/draftContent.spr")
    public ModelAndView getDraftContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentController");
        String learningObjectId = request.getParameter("learningObjectId");
        Content content = learningObjectService.getDraftByLearningObjectId(learningObjectId);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content was found for learning object id " + learningObjectId);
            return null;
        }
    }

    /**
     * Write the InputStream's content in the OutputStream
     * @param inputStream
     * @return outputStream
     */
    protected void writeContent(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
    }

    @RequestMapping("/attachmentContent.spr")
    public ModelAndView getAttachmentContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentController by Attachment");
        String attachmentId = request.getParameter("attachmentId");
        String learningObjectId = request.getParameter("learningObjectId");
        LearningObject learningObject = learningObjectService.findById(learningObjectId);
        List<Attachment> attachments = learningObject.getAttachments();
        for (Attachment attachment : attachments) {
            if (attachmentId.equals(attachment.getId())) {
                Content content = learningObjectService.getAttachmentContent(attachment);
                if (content != null) {
                    String mimeType = content.getMimeType();
                    response.setContentType(mimeType);
                    writeContent(content.getInputStream(), response.getOutputStream());
                    return null;
                }
            }
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("No content was found for learningObjectId = %s and attachmentId = %s", learningObjectId, attachmentId));
        return null;
    }
}
