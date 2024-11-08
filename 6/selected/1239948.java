package com.duroty.task;

import com.duroty.hibernate.MailPreferences;
import com.duroty.hibernate.Users;
import com.duroty.jmx.mbean.ApplicationConstants;
import com.duroty.jmx.mbean.Constants;
import com.duroty.lucene.mail.indexer.MailIndexerConstants;
import com.duroty.lucene.utils.FileUtilities;
import com.duroty.service.Mailet;
import com.duroty.service.Servible;
import com.duroty.service.analyzer.BayesianAnalysis;
import com.duroty.utils.GeneralOperations;
import com.duroty.utils.log.DLog;
import com.duroty.utils.mail.MimeMessageInputStreamSource;
import com.duroty.utils.mail.MimeMessageSource;
import com.duroty.utils.mail.MimeMessageWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.jboss.varia.scheduler.Schedulable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author durot
 *
 */
public class POP3ServiceTask implements Schedulable, Servible, MailIndexerConstants {

    /**
    * DOCUMENT ME!
    */
    private static final List pool = new ArrayList(10);

    /**
     * DOCUMENT ME!
     */
    private Context ctx = null;

    /**
     * DOCUMENT ME!
     */
    private boolean init = false;

    /**
     * DOCUMENT ME!
     */
    private int poolSize = 10;

    /**
     * DOCUMENT ME!
     */
    private String durotyMailFactory;

    /**
     * DOCUMENT ME!
     */
    private String hibernateSessionFactory;

    /**
     * DOCUMENT ME!
     */
    private String defaultMessagesPath;

    /**
     * @throws NamingException
     * @throws IOException
     *
     */
    public POP3ServiceTask(int poolSize) throws NamingException, IOException {
        super();
        this.poolSize = poolSize;
        Map options = ApplicationConstants.options;
        try {
            ctx = new InitialContext();
            HashMap mail = (HashMap) ctx.lookup((String) options.get(Constants.MAIL_CONFIG));
            this.durotyMailFactory = (String) mail.get(Constants.DUROTY_MAIL_FACTOTY);
            this.hibernateSessionFactory = (String) mail.get(Constants.HIBERNATE_SESSION_FACTORY);
            this.defaultMessagesPath = (String) mail.get(Constants.MAIL_MESSAGES_PATH);
            String tempDir = System.getProperty("java.io.tmpdir");
            if (!tempDir.endsWith(File.separator)) {
                tempDir = tempDir + File.separator;
            }
            FileUtilities.deleteMotLocks(new File(tempDir));
            FileUtilities.deleteLuceneLocks(new File(tempDir));
        } finally {
        }
    }

    public void perform(Date arg0, long arg1) {
        if (isInit()) {
            DLog.log(DLog.DEBUG, this.getClass(), "POP3ServiceTask is running and wait.");
            return;
        }
        flush();
    }

    /**
     * DOCUMENT ME!
     */
    private void flush() {
        setInit(true);
        SessionFactory hfactory = null;
        Session hsession = null;
        javax.mail.Session msession = null;
        try {
            hfactory = (SessionFactory) ctx.lookup(hibernateSessionFactory);
            hsession = hfactory.openSession();
            msession = (javax.mail.Session) ctx.lookup(durotyMailFactory);
            String pop3Host = msession.getProperty("mail.pop3.host");
            int port = 0;
            try {
                port = Integer.parseInt(msession.getProperty("mail.pop3.port"));
            } catch (Exception ex) {
                port = 0;
            }
            Query query = hsession.getNamedQuery("users-mail");
            query.setBoolean("active", true);
            query.setString("role", "mail");
            ScrollableResults scroll = query.scroll();
            while (scroll.next()) {
                POP3Client client = new POP3Client();
                try {
                    if (port > 0) {
                        client.connect(pop3Host, port);
                    } else {
                        client.connect(pop3Host);
                    }
                    client.setState(POP3Client.AUTHORIZATION_STATE);
                    Users user = (Users) scroll.get(0);
                    String repositoryName = user.getUseUsername();
                    if (client.login(repositoryName, user.getUsePassword())) {
                        POP3MessageInfo[] info = client.listUniqueIdentifiers();
                        if ((info != null) && (info.length > 0)) {
                            for (int i = 0; i < info.length; i++) {
                                if (pool.size() >= poolSize) {
                                    break;
                                }
                                Reader reader = client.retrieveMessage(info[i].number);
                                boolean existMessage = existMessageName(hfactory.openSession(), user, info[i].identifier);
                                String key = info[i].identifier + "--" + repositoryName;
                                if (existMessage) {
                                    client.deleteMessage(info[i].number);
                                } else {
                                    if (!poolContains(key)) {
                                        addPool(key);
                                        MimeMessage mime = buildMimeMessage(info[i].identifier, reader, user);
                                        if (!isSpam(user, mime)) {
                                            client.deleteMessage(info[i].number);
                                            Mailet mailet = new Mailet(this, info[i].identifier, repositoryName, mime);
                                            Thread thread = new Thread(mailet, key);
                                            thread.start();
                                        } else {
                                            client.deleteMessage(info[i].number);
                                        }
                                    }
                                }
                                Thread.sleep(100);
                            }
                        }
                    } else {
                    }
                } catch (Exception e) {
                } finally {
                    System.gc();
                    try {
                        client.logout();
                        client.disconnect();
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            System.gc();
            pool.clear();
            DLog.log(DLog.ERROR, this.getClass(), e.getMessage());
        } catch (OutOfMemoryError e) {
            System.gc();
            pool.clear();
            DLog.log(DLog.ERROR, this.getClass(), e.getMessage());
        } catch (Throwable e) {
            System.gc();
            pool.clear();
            DLog.log(DLog.ERROR, this.getClass(), e.getMessage());
        } finally {
            System.gc();
            GeneralOperations.closeHibernateSession(hsession);
            setInit(false);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param user DOCUMENT ME!
     * @param mime DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private boolean isSpam(Users user, MimeMessage mime) {
        boolean control = false;
        double probability = 0.0;
        try {
            BayesianAnalysis bayesianAnalysis = new BayesianAnalysis();
            bayesianAnalysis.init(null);
            bayesianAnalysis.service(user.getUseUsername(), null, mime);
            String[] aux = mime.getHeader(BayesianAnalysis.messageIsSpamProbability);
            if ((aux != null) && (aux.length > 0)) {
                for (int i = 0; i < aux.length; i++) {
                    probability = Double.parseDouble(aux[i].trim());
                    break;
                }
            }
            Iterator it = user.getMailPreferenceses().iterator();
            MailPreferences mailPreferences = (MailPreferences) it.next();
            if (mailPreferences.getMaprSpamTolerance() == -1) {
                return false;
            }
            double tolerance = ((double) mailPreferences.getMaprSpamTolerance()) / 100;
            if ((probability > 0.0) && (tolerance < 1.0) && (probability >= tolerance)) {
                control = true;
            }
            return control;
        } catch (NamingException e) {
            return false;
        } catch (Exception ex) {
            return false;
        } catch (java.lang.OutOfMemoryError ex) {
            System.gc();
            return false;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param mid DOCUMENT ME!
     * @param reader DOCUMENT ME!
     * @param user DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws MessagingException DOCUMENT ME!
     */
    private MimeMessage buildMimeMessage(String mid, Reader reader, Users user) throws IOException, MessagingException {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(IOUtils.toByteArray(reader));
            String userMessagesPath = null;
            if (!defaultMessagesPath.endsWith(File.separator)) {
                userMessagesPath = defaultMessagesPath + File.separator + user.getUseUsername() + File.separator + MESSAGES;
            } else {
                userMessagesPath = defaultMessagesPath + user.getUseUsername() + File.separator + MESSAGES;
            }
            MimeMessageSource source = new MimeMessageInputStreamSource(userMessagesPath, mid, bais, false);
            return new MimeMessageWrapper(source);
        } finally {
            IOUtils.closeQuietly(bais);
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param key DOCUMENT ME!
     */
    public void addPool(String key) {
        pool.add(key);
    }

    /**
     * DOCUMENT ME!
     *
     * @param key DOCUMENT ME!
     */
    public void removePool(String key) {
        pool.remove(key);
    }

    /**
     * DOCUMENT ME!
     *
     * @param key DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean poolContains(String key) {
        return pool.contains(key);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public synchronized boolean isInit() {
        synchronized (POP3ServiceTask.class) {
            return this.init;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param init DOCUMENT ME!
     */
    public synchronized void setInit(boolean init) {
        synchronized (POP3ServiceTask.class) {
            this.init = init;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param hsession DOCUMENT ME!
     * @param user DOCUMENT ME!
     * @param messageName DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private boolean existMessageName(Session hsession, Users user, String messageName) {
        try {
            Criteria crit = hsession.createCriteria(com.duroty.hibernate.Message.class);
            crit.add(Restrictions.eq("users", user));
            crit.add(Restrictions.eq("mesName", messageName));
            com.duroty.hibernate.Message message = (com.duroty.hibernate.Message) crit.uniqueResult();
            if (message != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        } finally {
            GeneralOperations.closeHibernateSession(hsession);
        }
    }
}
