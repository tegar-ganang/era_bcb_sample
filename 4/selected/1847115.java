package org.commonlibrary.lcms.web.springmvc.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlibrary.lcms.content.dao.ContentDao;
import org.commonlibrary.lcms.learningobject.service.LearningObjectService;
import org.commonlibrary.lcms.model.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author alonso
 *         Date: Dec 8, 2008
 *         Time: 1:59:01 PM
 *         <p/>
 */
@org.springframework.stereotype.Controller
@RequestMapping("/contentByReferenceId.spr")
public class ContentByReferenceIdController implements Controller {

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    @Qualifier("learningObjectService")
    private LearningObjectService learningObjectService;

    @Autowired
    @Qualifier("svnContentDao")
    private ContentDao contentDao;

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
     * Retrieves the Content assocciated to the given <code>contentReferenceId</code>
     * @param request
     * @param response
     * @return the content associated with the contentReferenceId
     */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentByReferenceIdController");
        String contentReferenceId = request.getParameter("contentReferenceId");
        Content content = contentDao.findByReferenceId(contentReferenceId);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No content was found for content reference id " + contentReferenceId);
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
}
