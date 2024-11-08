package com.duroty.application.files.manager;

import com.duroty.application.files.exceptions.FilesException;
import com.duroty.application.mail.exceptions.MailException;
import com.duroty.application.mail.utils.MailPartObj;
import com.duroty.hibernate.Identity;
import com.duroty.hibernate.MailPreferences;
import com.duroty.hibernate.Users;
import com.duroty.jmx.mbean.Constants;
import com.duroty.service.Messageable;
import com.duroty.utils.GeneralOperations;
import com.duroty.utils.NumberUtils;
import com.duroty.utils.mail.MessageUtilities;
import com.duroty.utils.mail.RFC2822Headers;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.MultiPartEmail;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

/**
 * @author Jordi Marquès
 * @version 1.0
*/
public class StoreManager {

    /**
     * DOCUMENT ME!
     */
    private Messageable messageable;

    /**
     * Creates a new SendManager object.
     */
    public StoreManager(HashMap properties) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        super();
        String messageFactory = (String) properties.get(Constants.MESSAGES_FACTORY);
        if ((messageFactory != null) && !messageFactory.trim().equals("")) {
            Class clazz = null;
            clazz = Class.forName(messageFactory.trim());
            this.messageable = (Messageable) clazz.newInstance();
            this.messageable.setProperties(properties);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param hsession DOCUMENT ME!
     * @param session DOCUMENT ME!
     * @param repositoryName DOCUMENT ME!
     * @param identity DOCUMENT ME!
     * @param to DOCUMENT ME!
     * @param cc DOCUMENT ME!
     * @param bcc DOCUMENT ME!
     * @param subject DOCUMENT ME!
     * @param body DOCUMENT ME!
     * @param attachments DOCUMENT ME!
     * @param isHtml DOCUMENT ME!
     * @param charset DOCUMENT ME!
     * @param headers DOCUMENT ME!
     * @param priority DOCUMENT ME!
     *
     * @throws MailException DOCUMENT ME!
     */
    public void send(org.hibernate.Session hsession, Session session, String repositoryName, Vector files, int label, String charset) throws FilesException {
        ByteArrayInputStream bais = null;
        FileOutputStream fos = null;
        try {
            if ((files == null) || (files.size() <= 0)) {
                return;
            }
            if (charset == null) {
                charset = MimeUtility.javaCharset(Charset.defaultCharset().displayName());
            }
            Users user = getUser(hsession, repositoryName);
            Identity identity = getDefaultIdentity(hsession, user);
            InternetAddress _returnPath = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _from = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _replyTo = new InternetAddress(identity.getIdeReplyTo(), identity.getIdeName());
            InternetAddress _to = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            for (int i = 0; i < files.size(); i++) {
                MultiPartEmail email = email = new MultiPartEmail();
                email.setCharset(charset);
                if (_from != null) {
                    email.setFrom(_from.getAddress(), _from.getPersonal());
                }
                if (_returnPath != null) {
                    email.addHeader("Return-Path", _returnPath.getAddress());
                    email.addHeader("Errors-To", _returnPath.getAddress());
                    email.addHeader("X-Errors-To", _returnPath.getAddress());
                }
                if (_replyTo != null) {
                    email.addReplyTo(_replyTo.getAddress(), _replyTo.getPersonal());
                }
                if (_to != null) {
                    email.addTo(_to.getAddress(), _to.getPersonal());
                }
                MailPartObj obj = (MailPartObj) files.get(i);
                email.setSubject("Files-System " + obj.getName());
                Date now = new Date();
                email.setSentDate(now);
                File dir = new File(System.getProperty("user.home") + File.separator + "tmp");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File file = new File(dir, obj.getName());
                bais = new ByteArrayInputStream(obj.getAttachent());
                fos = new FileOutputStream(file);
                IOUtils.copy(bais, fos);
                IOUtils.closeQuietly(bais);
                IOUtils.closeQuietly(fos);
                EmailAttachment attachment = new EmailAttachment();
                attachment.setPath(file.getPath());
                attachment.setDisposition(EmailAttachment.ATTACHMENT);
                attachment.setDescription("File Attachment: " + file.getName());
                attachment.setName(file.getName());
                email.attach(attachment);
                String mid = getId();
                email.addHeader(RFC2822Headers.IN_REPLY_TO, "<" + mid + ".JavaMail.duroty@duroty" + ">");
                email.addHeader(RFC2822Headers.REFERENCES, "<" + mid + ".JavaMail.duroty@duroty" + ">");
                email.addHeader("X-DBox", "FILES");
                email.addHeader("X-DRecent", "false");
                email.setMailSession(session);
                email.buildMimeMessage();
                MimeMessage mime = email.getMimeMessage();
                int size = MessageUtilities.getMessageSize(mime);
                if (!controlQuota(hsession, user, size)) {
                    throw new MailException("ErrorMessages.mail.quota.exceded");
                }
                messageable.storeMessage(mid, mime, user);
            }
        } catch (FilesException e) {
            throw e;
        } catch (Exception e) {
            throw new FilesException(e);
        } catch (java.lang.OutOfMemoryError ex) {
            System.gc();
            throw new FilesException(ex);
        } catch (Throwable e) {
            throw new FilesException(e);
        } finally {
            GeneralOperations.closeHibernateSession(hsession);
            IOUtils.closeQuietly(bais);
            IOUtils.closeQuietly(fos);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param hsession DOCUMENT ME!
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws FilesException DOCUMENT ME!
     */
    protected Identity getDefaultIdentity(org.hibernate.Session hsession, Users user) throws FilesException {
        try {
            Criteria crit = hsession.createCriteria(Identity.class);
            crit.add(Restrictions.eq("users", user));
            crit.add(Restrictions.eq("ideActive", new Boolean(true)));
            crit.add(Restrictions.eq("ideDefault", new Boolean(true)));
            Identity identity = (Identity) crit.uniqueResult();
            return identity;
        } catch (Exception ex) {
            return null;
        } finally {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param hsession DOCUMENT ME!
     * @param username DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    protected Users getUser(org.hibernate.Session hsession, String username) throws Exception {
        try {
            Criteria criteria = hsession.createCriteria(Users.class);
            criteria.add(Restrictions.eq("useUsername", username));
            criteria.add(Restrictions.eq("useActive", new Boolean(true)));
            return (Users) criteria.uniqueResult();
        } finally {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param username DOCUMENT ME!
     * @param password DOCUMENT ME!
     * @param size DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    protected boolean controlQuota(org.hibernate.Session hsession, Users user, int size) throws Exception {
        int maxQuotaSize = getMaxQuotaSize(hsession, user);
        int usedQuotaSize = getUsedQuotaSize(hsession, user) + size;
        int count = maxQuotaSize - usedQuotaSize;
        if (count < 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param username DOCUMENT ME!
     * @param password DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws DMailException DOCUMENT ME!
     */
    protected int getMaxQuotaSize(org.hibernate.Session hsession, Users user) throws MailException {
        try {
            Criteria crit = hsession.createCriteria(MailPreferences.class);
            crit.add(Restrictions.eq("users", user));
            MailPreferences preferences = (MailPreferences) crit.uniqueResult();
            return preferences.getMaprQuotaSize();
        } catch (Exception ex) {
            return 0;
        } finally {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param username DOCUMENT ME!
     * @param password DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws DMailException DOCUMENT ME!
     */
    protected int getUsedQuotaSize(org.hibernate.Session hsession, Users user) throws MailException {
        try {
            Query query = hsession.getNamedQuery("used-quota-size");
            query.setInteger("user", new Integer(user.getUseIdint()));
            Integer value = (Integer) query.uniqueResult();
            return value.intValue();
        } catch (Exception ex) {
            return 0;
        } finally {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    protected String getId() {
        return NumberUtils.pad(System.currentTimeMillis());
    }
}
