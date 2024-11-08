package ch.arpage.collaboweb.struts.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import ch.arpage.collaboweb.model.BinaryAttribute;
import ch.arpage.collaboweb.model.User;

/**
 * Struts action used to open the binary content of an attribute value.
 *
 * @author <a href="mailto:patrick@arpage.ch">Patrick Herber</a>
 */
public class AttributeOpenAction extends AbstractResourceAction {

    @Override
    protected ActionForward executeAction(ActionMapping mapping, ActionForm form, User user, HttpServletRequest request, HttpServletResponse response) throws Exception {
        long resourceId = ServletRequestUtils.getLongParameter(request, "resourceId", 0L);
        String attributeIdentifier = request.getParameter("identifier");
        if (resourceId != 0L && StringUtils.hasText(attributeIdentifier)) {
            try {
                BinaryAttribute binaryAttribute = resourceManager.readAttribute(resourceId, attributeIdentifier, user);
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
            } catch (DataRetrievalFailureException e) {
                addGlobalError(request, "errors.notFound");
            } catch (Exception e) {
                addGlobalError(request, e);
            }
        }
        return mapping.getInputForward();
    }
}
