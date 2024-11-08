package com.duroty.application.mail.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import com.duroty.application.mail.exceptions.MailException;
import com.duroty.application.mail.utils.MailPartObj;
import com.duroty.hibernate.Identity;
import com.duroty.hibernate.MailPreferences;
import com.duroty.hibernate.Message;
import com.duroty.hibernate.Users;
import com.duroty.jmx.mbean.Constants;
import com.duroty.service.Messageable;
import com.duroty.utils.GeneralOperations;
import com.duroty.utils.NumberUtils;
import com.duroty.utils.mail.MessageUtilities;
import com.duroty.utils.mail.RFC2822Headers;

/**
 * @author Jordi Marquès
 * @version 1.0
*/
public class SendManager {

    /**
     * DOCUMENT ME!
     */
    private Messageable messageable;

    /**
     * Creates a new SendManager object.
     */
    public SendManager(HashMap properties) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
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
    public void send(org.hibernate.Session hsession, Session session, String repositoryName, int ideIdint, String to, String cc, String bcc, String subject, String body, Vector attachments, boolean isHtml, String charset, InternetHeaders headers, String priority) throws MailException {
        try {
            if (charset == null) {
                charset = MimeUtility.javaCharset(Charset.defaultCharset().displayName());
            }
            if ((body == null) || body.trim().equals("")) {
                body = " ";
            }
            Email email = null;
            if (isHtml) {
                email = new HtmlEmail();
            } else {
                email = new MultiPartEmail();
            }
            email.setCharset(charset);
            Users user = getUser(hsession, repositoryName);
            Identity identity = getIdentity(hsession, ideIdint, user);
            InternetAddress _returnPath = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _from = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _replyTo = new InternetAddress(identity.getIdeReplyTo(), identity.getIdeName());
            InternetAddress[] _to = MessageUtilities.encodeAddresses(to, null);
            InternetAddress[] _cc = MessageUtilities.encodeAddresses(cc, null);
            InternetAddress[] _bcc = MessageUtilities.encodeAddresses(bcc, null);
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
            if ((_to != null) && (_to.length > 0)) {
                HashSet aux = new HashSet(_to.length);
                Collections.addAll(aux, _to);
                email.setTo(aux);
            }
            if ((_cc != null) && (_cc.length > 0)) {
                HashSet aux = new HashSet(_cc.length);
                Collections.addAll(aux, _cc);
                email.setCc(aux);
            }
            if ((_bcc != null) && (_bcc.length > 0)) {
                HashSet aux = new HashSet(_bcc.length);
                Collections.addAll(aux, _bcc);
                email.setBcc(aux);
            }
            email.setSubject(subject);
            Date now = new Date();
            email.setSentDate(now);
            File dir = new File(System.getProperty("user.home") + File.separator + "tmp");
            if (!dir.exists()) {
                dir.mkdir();
            }
            if ((attachments != null) && (attachments.size() > 0)) {
                for (int i = 0; i < attachments.size(); i++) {
                    ByteArrayInputStream bais = null;
                    FileOutputStream fos = null;
                    try {
                        MailPartObj obj = (MailPartObj) attachments.get(i);
                        File file = new File(dir, obj.getName());
                        bais = new ByteArrayInputStream(obj.getAttachent());
                        fos = new FileOutputStream(file);
                        IOUtils.copy(bais, fos);
                        EmailAttachment attachment = new EmailAttachment();
                        attachment.setPath(file.getPath());
                        attachment.setDisposition(EmailAttachment.ATTACHMENT);
                        attachment.setDescription("File Attachment: " + file.getName());
                        attachment.setName(file.getName());
                        if (email instanceof MultiPartEmail) {
                            ((MultiPartEmail) email).attach(attachment);
                        }
                    } catch (Exception ex) {
                    } finally {
                        IOUtils.closeQuietly(bais);
                        IOUtils.closeQuietly(fos);
                    }
                }
            }
            String mid = getId();
            if (headers != null) {
                Header xheader;
                Enumeration xe = headers.getAllHeaders();
                for (; xe.hasMoreElements(); ) {
                    xheader = (Header) xe.nextElement();
                    if (xheader.getName().equals(RFC2822Headers.IN_REPLY_TO)) {
                        email.addHeader(xheader.getName(), xheader.getValue());
                    } else if (xheader.getName().equals(RFC2822Headers.REFERENCES)) {
                        email.addHeader(xheader.getName(), xheader.getValue());
                    }
                }
            } else {
                email.addHeader(RFC2822Headers.IN_REPLY_TO, "<" + mid + ".JavaMail.duroty@duroty" + ">");
                email.addHeader(RFC2822Headers.REFERENCES, "<" + mid + ".JavaMail.duroty@duroty" + ">");
            }
            if (priority != null) {
                if (priority.equals("high")) {
                    email.addHeader("Importance", priority);
                    email.addHeader("X-priority", "1");
                } else if (priority.equals("low")) {
                    email.addHeader("Importance", priority);
                    email.addHeader("X-priority", "5");
                }
            }
            if (email instanceof HtmlEmail) {
                ((HtmlEmail) email).setHtmlMsg(body);
            } else {
                email.setMsg(body);
            }
            email.setMailSession(session);
            email.buildMimeMessage();
            MimeMessage mime = email.getMimeMessage();
            int size = MessageUtilities.getMessageSize(mime);
            if (!controlQuota(hsession, user, size)) {
                throw new MailException("ErrorMessages.mail.quota.exceded");
            }
            messageable.saveSentMessage(mid, mime, user);
            Thread thread = new Thread(new SendMessageThread(email));
            thread.start();
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailException(e);
        } catch (java.lang.OutOfMemoryError ex) {
            System.gc();
            throw new MailException(ex);
        } catch (Throwable e) {
            throw new MailException(e);
        } finally {
            GeneralOperations.closeHibernateSession(hsession);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param hsession DOCUMENT ME!
     * @param session DOCUMENT ME!
     * @param repositoryName DOCUMENT ME!
     * @param ideIdint DOCUMENT ME!
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
    public void saveDraft(org.hibernate.Session hsession, Session session, String repositoryName, int ideIdint, String to, String cc, String bcc, String subject, String body, Vector attachments, boolean isHtml, String charset, InternetHeaders headers, String priority) throws MailException {
        try {
            if (charset == null) {
                charset = MimeUtility.javaCharset(Charset.defaultCharset().displayName());
            }
            if ((body == null) || body.trim().equals("")) {
                body = " ";
            }
            Email email = null;
            if (isHtml) {
                email = new HtmlEmail();
            } else {
                email = new MultiPartEmail();
            }
            email.setCharset(charset);
            Users user = getUser(hsession, repositoryName);
            Identity identity = getIdentity(hsession, ideIdint, user);
            InternetAddress _returnPath = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _from = new InternetAddress(identity.getIdeEmail(), identity.getIdeName());
            InternetAddress _replyTo = new InternetAddress(identity.getIdeReplyTo(), identity.getIdeName());
            InternetAddress[] _to = MessageUtilities.encodeAddresses(to, null);
            InternetAddress[] _cc = MessageUtilities.encodeAddresses(cc, null);
            InternetAddress[] _bcc = MessageUtilities.encodeAddresses(bcc, null);
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
            if ((_to != null) && (_to.length > 0)) {
                HashSet aux = new HashSet(_to.length);
                Collections.addAll(aux, _to);
                email.setTo(aux);
            }
            if ((_cc != null) && (_cc.length > 0)) {
                HashSet aux = new HashSet(_cc.length);
                Collections.addAll(aux, _cc);
                email.setCc(aux);
            }
            if ((_bcc != null) && (_bcc.length > 0)) {
                HashSet aux = new HashSet(_bcc.length);
                Collections.addAll(aux, _bcc);
                email.setBcc(aux);
            }
            email.setSubject(subject);
            Date now = new Date();
            email.setSentDate(now);
            File dir = new File(System.getProperty("user.home") + File.separator + "tmp");
            if (!dir.exists()) {
                dir.mkdir();
            }
            if ((attachments != null) && (attachments.size() > 0)) {
                for (int i = 0; i < attachments.size(); i++) {
                    ByteArrayInputStream bais = null;
                    FileOutputStream fos = null;
                    try {
                        MailPartObj obj = (MailPartObj) attachments.get(i);
                        File file = new File(dir, obj.getName());
                        bais = new ByteArrayInputStream(obj.getAttachent());
                        fos = new FileOutputStream(file);
                        IOUtils.copy(bais, fos);
                        EmailAttachment attachment = new EmailAttachment();
                        attachment.setPath(file.getPath());
                        attachment.setDisposition(EmailAttachment.ATTACHMENT);
                        attachment.setDescription("File Attachment: " + file.getName());
                        attachment.setName(file.getName());
                        if (email instanceof MultiPartEmail) {
                            ((MultiPartEmail) email).attach(attachment);
                        }
                    } catch (Exception ex) {
                    } finally {
                        IOUtils.closeQuietly(bais);
                        IOUtils.closeQuietly(fos);
                    }
                }
            }
            if (headers != null) {
                Header xheader;
                Enumeration xe = headers.getAllHeaders();
                for (; xe.hasMoreElements(); ) {
                    xheader = (Header) xe.nextElement();
                    if (xheader.getName().equals(RFC2822Headers.IN_REPLY_TO)) {
                        email.addHeader(xheader.getName(), xheader.getValue());
                    } else if (xheader.getName().equals(RFC2822Headers.REFERENCES)) {
                        email.addHeader(xheader.getName(), xheader.getValue());
                    }
                }
            }
            if (priority != null) {
                if (priority.equals("high")) {
                    email.addHeader("Importance", priority);
                    email.addHeader("X-priority", "1");
                } else if (priority.equals("low")) {
                    email.addHeader("Importance", priority);
                    email.addHeader("X-priority", "5");
                }
            }
            if (email instanceof HtmlEmail) {
                ((HtmlEmail) email).setHtmlMsg(body);
            } else {
                email.setMsg(body);
            }
            email.setMailSession(session);
            email.buildMimeMessage();
            MimeMessage mime = email.getMimeMessage();
            int size = MessageUtilities.getMessageSize(mime);
            if (!controlQuota(hsession, user, size)) {
                throw new MailException("ErrorMessages.mail.quota.exceded");
            }
            messageable.storeDraftMessage(getId(), mime, user);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            throw new MailException(e);
        } catch (java.lang.OutOfMemoryError ex) {
            System.gc();
            throw new MailException(ex);
        } catch (Throwable e) {
            throw new MailException(e);
        } finally {
            GeneralOperations.closeHibernateSession(hsession);
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
    protected Identity getIdentity(org.hibernate.Session hsession, int ideIdint, Users users) throws Exception {
        try {
            Criteria criteria = hsession.createCriteria(Identity.class);
            criteria.add(Restrictions.eq("ideIdint", ideIdint));
            criteria.add(Restrictions.eq("users", users));
            return (Identity) criteria.uniqueResult();
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

    /**
     * DOCUMENT ME!
     *
     * @param mid DOCUMENT ME!
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws OutOfMemoryError DOCUMENT ME!
     * @throws Exception DOCUMENT ME!
     * @throws Throwable DOCUMENT ME!
     */
    public InternetHeaders getHeaders(org.hibernate.Session hsession, String repositoryName, String mid) {
        InternetHeaders xheaders = null;
        try {
            Criteria crit = hsession.createCriteria(Message.class);
            crit.add(Restrictions.eq("users", getUser(hsession, repositoryName)));
            crit.add(Restrictions.eq("mesName", mid));
            Message message = (Message) crit.uniqueResult();
            if (message != null) {
                String headers = message.getMesHeaders();
                xheaders = new InternetHeaders(IOUtils.toInputStream(headers));
            }
            return xheaders;
        } catch (OutOfMemoryError e) {
            return null;
        } catch (Exception e) {
            return null;
        } catch (Throwable e) {
            return null;
        } finally {
            GeneralOperations.closeHibernateSession(hsession);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param session DOCUMENT ME!
     * @param repositoryName DOCUMENT ME!
     * @param from DOCUMENT ME!
     * @param to DOCUMENT ME!
     * @param subject DOCUMENT ME!
     * @param body DOCUMENT ME!
     *
     * @throws MailException DOCUMENT ME!
     */
    public void sendIdentity(Session session, String repositoryName, String from, String to, String subject, String body) throws MailException {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setMailSession(session);
            email.setFrom(from);
            email.addTo(to);
            email.setSubject(subject);
            email.setHtmlMsg(body);
            email.setCharset(Charset.defaultCharset().displayName());
            email.send();
        } catch (Exception e) {
            throw new MailException(e);
        } finally {
        }
    }
}
