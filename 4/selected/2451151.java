package org.commonlibrary.lcms.web.springmvc.account;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlibrary.lcms.content.dao.ContentDao;
import org.commonlibrary.lcms.model.Content;
import org.commonlibrary.lcms.model.User;
import org.commonlibrary.lcms.security.service.UserService;
import org.commonlibrary.lcms.support.spring.beans.Property;
import org.commonlibrary.lcms.userProfile.service.UserProfileService;
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

@org.springframework.stereotype.Controller
public class AccountController implements Controller {

    protected final transient Log logger = LogFactory.getLog(getClass());

    @Property("clv2.deployment.host")
    private String hostDomain;

    @Autowired
    @Qualifier("userService")
    UserService userService;

    @Autowired
    @Qualifier("userProfileService")
    UserProfileService userProfileService;

    @Autowired
    @Qualifier("svnContentDao")
    private ContentDao contentDao;

    @PostConstruct
    public void init() {
        String msg = "%s property must be set";
        if (this.userService == null) {
            throw new IllegalStateException(String.format(msg, "learningObjectService"));
        }
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the Content assocciated to the given <code>contentReferenceId</code>
     * @param request
     * @param response
     * @return the content associated with the contentReferenceId
     */
    @RequestMapping("/accountContentByReferenceId.spr")
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentByReferenceIdController");
        ModelAndView mvc = null;
        String contentReferenceId = request.getParameter("contentReferenceId");
        String strVersion = request.getParameter("version");
        long version = -1;
        if (strVersion != null) version = Long.parseLong(strVersion);
        String username = request.getParameter("username");
        Content content = null;
        if (contentReferenceId != null) {
            content = contentDao.findByReferenceId(contentReferenceId);
        } else if (strVersion != null) {
            User user = userService.findUserByUsername(username);
            content = userProfileService.getVersion(user.getUserProfile(), version);
        }
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            mvc = new ModelAndView();
            mvc.addObject("hostDomain", this.hostDomain);
            mvc.setViewName("account/defaultMyPage");
            return mvc;
        }
    }

    @RequestMapping("/accountContentByVersion.spr")
    public ModelAndView getVersionContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (logger.isInfoEnabled()) logger.info("Enter ContentByReferenceIdController");
        ModelAndView mvc = null;
        long version = Long.parseLong(request.getParameter("version"));
        String username = request.getParameter("username");
        User user = userService.findUserByUsername(username);
        Content content = userProfileService.getVersion(user.getUserProfile(), version);
        if (content != null) {
            String mimeType = content.getMimeType();
            response.setContentType(mimeType);
            writeContent(content.getInputStream(), response.getOutputStream());
            return null;
        } else {
            mvc = new ModelAndView();
            mvc.addObject("hostDomain", this.hostDomain);
            mvc.setViewName("account/defaultMyPage");
            return mvc;
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
