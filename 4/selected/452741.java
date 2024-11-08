package com.duroty.application.mail.actions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import com.duroty.application.mail.interfaces.Preferences;
import com.duroty.application.mail.interfaces.Send;
import com.duroty.application.mail.utils.MailDefaultAction;
import com.duroty.application.mail.utils.MailPartObj;
import com.duroty.constants.Constants;
import com.duroty.constants.ExceptionCode;
import com.duroty.utils.exceptions.ExceptionUtilities;
import com.duroty.utils.log.DLog;
import com.duroty.utils.log.DMessage;

/**
 * @author durot
 *
 */
public class SaveDraftAction extends MailDefaultAction {

    /**
     * Creates a new SaveDraftAction object.
     */
    public SaveDraftAction() {
        super();
    }

    protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ActionMessages errors = new ActionMessages();
        try {
            boolean isMultipart = FileUpload.isMultipartContent(request);
            if (isMultipart) {
                Map fields = new HashMap();
                Vector attachments = new Vector();
                List items = diskFileUpload.parseRequest(request);
                Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    FileItem item = (FileItem) iter.next();
                    if (item.isFormField()) {
                        fields.put(item.getFieldName(), item.getString());
                    } else {
                        if (!StringUtils.isBlank(item.getName())) {
                            ByteArrayOutputStream baos = null;
                            try {
                                baos = new ByteArrayOutputStream();
                                IOUtils.copy(item.getInputStream(), baos);
                                MailPartObj part = new MailPartObj();
                                part.setAttachent(baos.toByteArray());
                                part.setContentType(item.getContentType());
                                part.setName(item.getName());
                                part.setSize(item.getSize());
                                attachments.addElement(part);
                            } catch (Exception ex) {
                            } finally {
                                IOUtils.closeQuietly(baos);
                            }
                        }
                    }
                }
                String body = "";
                if (fields.get("taBody") != null) {
                    body = (String) fields.get("taBody");
                } else if (fields.get("taReplyBody") != null) {
                    body = (String) fields.get("taReplyBody");
                }
                Preferences preferencesInstance = getPreferencesInstance(request);
                Send sendInstance = getSendInstance(request);
                String mid = (String) fields.get("mid");
                sendInstance.saveDraft(mid, Integer.parseInt((String) fields.get("identity")), (String) fields.get("to"), (String) fields.get("cc"), (String) fields.get("bcc"), (String) fields.get("subject"), body, attachments, preferencesInstance.getPreferences().isHtmlMessage(), Charset.defaultCharset().displayName(), (String) fields.get("priority"));
            } else {
                errors.add("general", new ActionMessage(ExceptionCode.ERROR_MESSAGES_PREFIX + "mail.send", "The form is null"));
                request.setAttribute("exception", "The form is null");
                request.setAttribute("newLocation", null);
                doTrace(request, DLog.ERROR, getClass(), "The form is null");
            }
        } catch (Exception ex) {
            String errorMessage = ExceptionUtilities.parseMessage(ex);
            if (errorMessage == null) {
                errorMessage = "NullPointerException";
            }
            errors.add("general", new ActionMessage(ExceptionCode.ERROR_MESSAGES_PREFIX + "general", errorMessage));
            request.setAttribute("exception", errorMessage);
            doTrace(request, DLog.ERROR, getClass(), errorMessage);
        } finally {
        }
        if (errors.isEmpty()) {
            doTrace(request, DLog.INFO, getClass(), "OK");
            return mapping.findForward(Constants.ACTION_SUCCESS_FORWARD);
        } else {
            saveErrors(request, errors);
            return mapping.findForward(Constants.ACTION_FAIL_FORWARD);
        }
    }

    protected void doTrace(HttpServletRequest request, int level, Class classe, String message) throws Exception {
        DLog.log(level, classe, DMessage.toString(request, message));
    }
}
