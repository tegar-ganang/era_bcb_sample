package com.reserveamerica.jirarmi.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.web.util.AttachmentException;
import com.reserveamerica.commons.Token;
import com.reserveamerica.jirarmi.AttachmentService;
import com.reserveamerica.jirarmi.Utils;
import com.reserveamerica.jirarmi.beans.issue.AttachmentRemote;
import com.reserveamerica.jirarmi.exceptions.AttachmentNotFoundException;
import com.reserveamerica.jirarmi.exceptions.JiraException;
import com.reserveamerica.jirarmi.transformers.issue.AttachmentTransformer;

/**
 * @author BStasyszyn
 */
public class AttachmentServiceImpl implements AttachmentService {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AttachmentServiceImpl.class);

    public AttachmentRemote getAttachment(Token token, Long id) throws JiraException {
        return AttachmentTransformer.get().transform(JiraUtils.getAttachmentObject(id));
    }

    public List<AttachmentRemote> getAttachments(Token token, Set<Long> ids) throws JiraException {
        List<AttachmentRemote> values = new ArrayList<AttachmentRemote>(ids.size());
        for (Long id : ids) {
            try {
                values.add(AttachmentTransformer.get().transform(JiraUtils.getAttachmentObject(id)));
            } catch (AttachmentNotFoundException ex) {
                log.warn("getAttachments(" + token + ") - Attachment [" + id + "] not found. Ignoring.");
            }
        }
        return values;
    }

    public byte[] getAttachmentContents(Token token, Long id) throws JiraException {
        return Utils.getContents(AttachmentUtils.getAttachmentFile(JiraUtils.getAttachmentObject(id)));
    }

    public boolean attachmentsEnabled(Token token) throws JiraException {
        return JiraManagers.attachmentManager.attachmentsEnabled();
    }

    public AttachmentRemote createAttachment(Token token, String filename, String contentType, Long issueId, byte[] contents, Map<String, Object> attachmentProperties, Date createdTime) throws JiraException {
        GenericValue issue = JiraUtils.getIssue(issueId);
        File tempFile = createFile(contents);
        try {
            ChangeItemBean changeItem = JiraManagers.attachmentManager.createAttachment(tempFile, filename, contentType, RequestContext.getUser(), issue, attachmentProperties, Utils.getTimestamp(createdTime, true));
            return AttachmentTransformer.get().transform(JiraUtils.getAttachmentObject(Long.parseLong(changeItem.getTo())));
        } catch (AttachmentException ex) {
            log.error("createAttachment(" + token + ") - Unable to create attachment", ex);
            throw new JiraException("Unable to create attachment. Details: " + ex.getMessage());
        } catch (GenericEntityException ex) {
            log.error("createAttachment(" + token + ") - Unable to create attachment", ex);
            throw new JiraException("Unable to create attachment. Details: " + ex.getMessage());
        } finally {
            if (!tempFile.delete()) {
                log.warn("createAttachment(" + token + ") - Unable to delete temporary attachement file [" + tempFile.getAbsolutePath() + "].");
            }
        }
    }

    public void deleteAttachment(Token token, Long id) throws JiraException {
        try {
            JiraManagers.attachmentManager.deleteAttachment(JiraUtils.getAttachmentObject(id));
        } catch (RemoveException ex) {
            log.error("deleteAttachment - Unable to delete attachment", ex);
            throw new JiraException("Unable to delete attachment. Details: " + ex.getMessage());
        }
    }

    public Collection<AttachmentRemote> getAttachmentsForIssue(Token token, Long issueId) throws JiraException {
        return AttachmentTransformer.get().transform(JiraManagers.attachmentManager.getAttachments(JiraUtils.getIssueObject(issueId)));
    }

    public boolean isScreenshotAppletEnabled(Token token) throws JiraException {
        return JiraManagers.attachmentManager.isScreenshotAppletEnabled();
    }

    public boolean isScreenshotAppletSupportedByOS(Token token) throws JiraException {
        return JiraManagers.attachmentManager.isScreenshotAppletSupportedByOS();
    }

    private File createFile(byte[] contents) throws JiraException {
        File tempFile;
        try {
            tempFile = File.createTempFile("_ra", "att");
        } catch (IOException ex) {
            log.error("createFile - ", ex);
            throw new JiraException("Unable to create temporary attachement file.");
        }
        FileOutputStream os = null;
        InputStream is = null;
        try {
            os = new FileOutputStream(tempFile);
            is = new ByteArrayInputStream(contents);
            byte[] b = new byte[4096];
            while (true) {
                int read = is.read(b);
                if (read <= 0) {
                    break;
                }
                os.write(b, 0, read);
            }
            return tempFile;
        } catch (IOException ex) {
            log.error("createFile - ", ex);
            throw new JiraException("Unable to create temporary attachement file [" + tempFile.getAbsolutePath() + "].");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.warn("createFile - ", ex);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    log.warn("createFile - ", ex);
                }
            }
        }
    }
}
