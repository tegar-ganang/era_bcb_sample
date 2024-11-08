package eduburner.web.controller.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import eduburner.entity.user.UserData;
import eduburner.enumerations.Message;
import eduburner.web.controller.BaseController;

@Controller
public class UserSettingsController extends BaseController {

    private static final String SETTINGS_VIEW = "settings";

    @RequestMapping(value = "/account/settings", method = RequestMethod.GET)
    public String show(Model model) {
        UserData ud = getRemoteUserDataObj();
        model.addAttribute("user", ud);
        return SETTINGS_VIEW;
    }

    @RequestMapping(value = "/account/settings", method = RequestMethod.PUT)
    public String update(@ModelAttribute("user") UserData user, BindingResult br, Model model) {
        return null;
    }

    @RequestMapping(value = "/account/profilepicture", method = RequestMethod.POST)
    public String uploadProfilePicture(HttpServletRequest request, Model model) throws Exception {
        logger.debug("entering uploadProfilePicture method...");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        CommonsMultipartFile file = (CommonsMultipartFile) multipartRequest.getFile("Filedata");
        String username = getRemoteUser();
        String origionalFileName = file.getOriginalFilename();
        String extension = origionalFileName.substring(origionalFileName.lastIndexOf(".") + 1);
        String newFileName = "profile." + extension;
        String uploadDir = request.getSession().getServletContext().getRealPath("/") + "/static/profiles/" + username + "/";
        logger.debug("upload dir is: " + uploadDir);
        handleUpload(file, newFileName, uploadDir);
        String profilePicture = request.getServletPath() + "/static/profiles/" + username + "/" + newFileName;
        userManager.uploadUserProfilePicture(username, profilePicture);
        setReturnMsg(model, Message.OK);
        return JSON_VIEW;
    }

    private void handleUpload(CommonsMultipartFile file, String newFileName, String uploadDir) throws IOException, FileNotFoundException {
        File dirPath = new File(uploadDir);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        InputStream stream = file.getInputStream();
        OutputStream bos = new FileOutputStream(uploadDir + newFileName);
        IOUtils.copy(stream, bos);
    }
}
