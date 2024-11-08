package ch.arpage.collaboweb.services.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.ServletRequestUtils;
import ch.arpage.collaboweb.model.BinaryAttribute;
import ch.arpage.collaboweb.model.User;
import ch.arpage.collaboweb.services.ResourceManager;

/**
 * Open a file
 *
 * @author <a href="mailto:patrick@arpage.ch">Patrick Herber</a>
 */
public class FileOpenAction implements Action {

    private ResourceManager resourceManager;

    /**
	 * Set the resourceManager.
	 * @param resourceManager the resourceManager to set
	 */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public String execute(HttpServletRequest request, HttpServletResponse response, User user, String parameter) throws Exception {
        long resourceId = ServletRequestUtils.getLongParameter(request, "resourceId", 0L);
        BinaryAttribute binaryAttribute = resourceManager.readAttribute(resourceId, parameter, user);
        response.addHeader("Content-Disposition", "attachment; filename=\"" + binaryAttribute.getName() + '"');
        String contentType = binaryAttribute.getContentType();
        if (contentType != null) {
            if ("application/x-zip-compressed".equalsIgnoreCase(contentType)) {
                response.setContentType("application/octet-stream");
            } else {
                response.setContentType(contentType);
            }
        } else {
            response.setContentType("application/octet-stream");
        }
        IOUtils.copy(binaryAttribute.getInputStream(), response.getOutputStream());
        return null;
    }
}
