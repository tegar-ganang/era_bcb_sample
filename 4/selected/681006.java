package org.suli.kozosprojekt.brt.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.web.context.WebApplicationContext;
import org.suli.kozosprojekt.brt.ICommonValues;
import org.suli.kozosprojekt.brt.db.Bug;
import org.suli.kozosprojekt.brt.db.BugFile;
import org.suli.kozosprojekt.brt.db.BugFileHome;
import org.suli.kozosprojekt.brt.db.BugHistory;
import org.suli.kozosprojekt.brt.db.BugHistoryHome;
import org.suli.kozosprojekt.brt.db.BugHistoryTypeHome;
import org.suli.kozosprojekt.brt.db.BugHome;
import org.suli.kozosprojekt.brt.db.BugStatus;
import org.suli.kozosprojekt.brt.db.BugStatusHome;
import org.suli.kozosprojekt.brt.db.CategoryHome;
import org.suli.kozosprojekt.brt.db.PortalPreferences;
import org.suli.kozosprojekt.brt.db.PortalPreferencesHome;
import org.suli.kozosprojekt.brt.db.Project;
import org.suli.kozosprojekt.brt.db.ProjectHome;
import org.suli.kozosprojekt.brt.db.SentMail;
import org.suli.kozosprojekt.brt.db.SentMailHome;
import org.suli.kozosprojekt.brt.db.User;
import org.suli.kozosprojekt.brt.db.UserHome;
import org.suli.kozosprojekt.brt.db.UserProjectPreference;
import org.suli.kozosprojekt.brt.db.UserProjectPreferenceHome;
import org.suli.kozosprojekt.brt.mail.Envelope;
import org.suli.kozosprojekt.brt.mail.Message;
import org.suli.kozosprojekt.brt.mail.SMTPConnection;
import eu.medsea.mimeutil.MimeUtil2;

public class DAOUtil {

    private static final Logger LOG = Logger.getLogger(DAOUtil.class);

    /** The bug dao. */
    private BugHome bugDAO;

    /** The bug status dao. */
    private BugStatusHome bugStatusDAO;

    /** The bug file dao. */
    private BugFileHome bugFileDAO;

    /** The category dao. */
    private CategoryHome categoryDAO;

    /** The project dao. */
    private ProjectHome projectDAO;

    /** The portal preferences dao. */
    private PortalPreferencesHome portalPreferencesDAO;

    /** The user dao. */
    private UserHome userDAO;

    private BugHistoryHome bugHistoryDAO;

    private BugHistoryTypeHome bugHistoryTypeDAO;

    private UserProjectPreferenceHome userProjectPreferenceDAO;

    private SentMailHome sentMailDAO;

    private final WebApplicationContext spring;

    private final MimeUtil2 mimeUtil;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DAOUtil(final WebApplicationContext context) {
        this.spring = context;
        this.loadDAOs();
        this.mimeUtil = new MimeUtil2();
        this.mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    /**
     * Load da os.
     */
    private void loadDAOs() {
        this.bugDAO = (BugHome) this.spring.getBean(ICommonValues.DAO_BUG);
        this.bugStatusDAO = (BugStatusHome) this.spring.getBean(ICommonValues.DAO_BUG_STATUS);
        this.bugFileDAO = (BugFileHome) this.spring.getBean(ICommonValues.DAO_BUG_FILE);
        this.bugHistoryDAO = (BugHistoryHome) this.spring.getBean(ICommonValues.DAO_BUG_HISTORY);
        this.bugHistoryTypeDAO = (BugHistoryTypeHome) this.spring.getBean(ICommonValues.DAO_BUG_HISTORY_TYPE);
        this.categoryDAO = (CategoryHome) this.spring.getBean(ICommonValues.DAO_CATEGORY);
        this.projectDAO = (ProjectHome) this.spring.getBean(ICommonValues.DAO_PROJECT);
        this.userDAO = (UserHome) this.spring.getBean(ICommonValues.DAO_USER);
        this.portalPreferencesDAO = (PortalPreferencesHome) this.spring.getBean(ICommonValues.DAO_PORTAL_PREFERENCES);
        this.userProjectPreferenceDAO = (UserProjectPreferenceHome) this.spring.getBean(ICommonValues.DAO_USER_PROJECT_PREFERENCE);
        this.sentMailDAO = (SentMailHome) this.spring.getBean(ICommonValues.DAO_SENT_MAIL);
    }

    /**
     * Gets the all users.
     * 
     * @return the all users
     */
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers(final User excludedUser, final boolean enabled, final String email, final String textPassword) {
        final List<User> users = new ArrayList<User>();
        final DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
        criteria.add(Restrictions.eq("enabled", enabled));
        criteria.add(Restrictions.in("superUser", new Object[] { true, false }));
        if (excludedUser != null) {
            criteria.add(Restrictions.ne("id", excludedUser.getId()));
        }
        if (email != null) {
            criteria.add(Restrictions.eq("email", email));
        }
        if (textPassword != null) {
            criteria.add(Restrictions.eq("password", ProjektUtil.getMd5Password(textPassword)));
        }
        users.addAll(this.userDAO.getHibernateTemplate().findByCriteria(criteria));
        return users;
    }

    public User getCopyOf(final User persistentUser, final String newName) {
        User myselfUser = null;
        try {
            myselfUser = new User();
            BeanUtils.copyProperties(myselfUser, persistentUser);
            myselfUser.setName(newName);
        } catch (final Exception e) {
            DAOUtil.LOG.error("Can't clone the logged in user!", e);
        }
        return myselfUser;
    }

    /**
     * Load dest dir from bug project.
     * 
     * @return the file
     */
    public File getDestDirFromBugProject(final Bug newBug) {
        File response = null;
        if (newBug != null) {
            final Project project = newBug.getProject();
            final PortalPreferences examplePref = new PortalPreferences();
            examplePref.setName(ICommonValues.PORTAL_FILES_SAVE_PATH);
            final List<PortalPreferences> prefs = this.portalPreferencesDAO.findByExample(examplePref);
            if (prefs.size() > 0) {
                final String portalSavePath = prefs.get(0).getValue();
                final String projectSavePath = project.getStorageFolder();
                final File uploadDir = new File(portalSavePath, projectSavePath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdir();
                }
                response = uploadDir;
            }
        }
        return response;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getMimeType(final File file, final String fileName) {
        String response = null;
        List<?> mimeList = new ArrayList(this.mimeUtil.getMimeTypes(file));
        if (mimeList.size() > 0) {
            response = mimeList.get(0).toString();
        }
        if (response == null) {
            mimeList = new ArrayList(this.mimeUtil.getMimeTypes(fileName));
            if (mimeList.size() > 0) {
                response = mimeList.get(0).toString();
            }
        }
        return response;
    }

    /**
     * Save attached files.
     * 
     * @param destDir
     *            the dest dir
     * @return true, if successful
     */
    public boolean saveAttachedFiles(final Bug newBug, final User userInAction, final File destDir, final List<File> newBugFile, final List<String> newBugFileFileName) {
        boolean wasError = false;
        BugFile bugFile;
        String randomName;
        File finalFile;
        File tempFile;
        for (int i = 0; i < newBugFile.size(); i++) {
            final File file = newBugFile.get(i);
            try {
                bugFile = new BugFile();
                bugFile.setBug(newBug);
                bugFile.setCreated(new Date());
                bugFile.setName(newBugFileFileName.get(i));
                randomName = UUID.randomUUID().toString();
                FileUtils.copyFileToDirectory(file, destDir);
                finalFile = new File(destDir, randomName);
                tempFile = new File(destDir, file.getName());
                tempFile.renameTo(finalFile);
                bugFile.setLocalFileName(randomName);
                bugFile.setUser(userInAction);
                this.bugFileDAO.persist(bugFile);
            } catch (final IOException e) {
                wasError = true;
            }
        }
        this.bugFileDAO.getHibernateTemplate().flush();
        return wasError;
    }

    public void addHistory(final Bug bug, final String description, final int historyType, final User userInAction) {
        final BugHistory history = new BugHistory();
        history.setBug(bug);
        history.setBugHistoryType(this.bugHistoryTypeDAO.findById(historyType));
        history.setDate(new Date());
        history.setUser(userInAction);
        history.setDescription(description);
        this.bugHistoryDAO.persist(history);
        this.bugHistoryDAO.getHibernateTemplate().flush();
    }

    @SuppressWarnings("unchecked")
    public void sendMail(final Bug newBug, final int bugStatusAssigned, final User referredUser, final String hostURL, final String portalEmail) {
        final DetachedCriteria criteria = DetachedCriteria.forClass(UserProjectPreference.class);
        criteria.add(Restrictions.eq("user", referredUser));
        this.fillCriteriaWithEmailPref(criteria, bugStatusAssigned);
        criteria.add(Restrictions.eq("project", newBug.getProject()));
        final List<UserProjectPreference> sendEmailPrefs = this.userProjectPreferenceDAO.getHibernateTemplate().findByCriteria(criteria);
        if (sendEmailPrefs.size() != 0) {
            try {
                final String subject = this.getSubject(newBug, bugStatusAssigned);
                final String body = this.getMailContent(newBug, bugStatusAssigned, referredUser, hostURL);
                final Message message = new Message(portalEmail, referredUser.getEmail(), subject, body);
                final Envelope email = new Envelope(message);
                final SMTPConnection connection = this.getSMTPConnection();
                connection.send(email);
                connection.close();
                this.saveSentMail(referredUser.getEmail(), subject, body);
            } catch (final IOException e) {
                DAOUtil.LOG.error("Error occured when sending mail to " + referredUser.getEmail());
            }
        }
    }

    private void fillCriteriaWithEmailPref(final DetachedCriteria criteria, final int bugStatus) {
        switch(bugStatus) {
            case ICommonValues.BUG_STATUS_ASSIGNED:
                {
                    criteria.add(Restrictions.eq("sendEmailWhenAssigned", Boolean.TRUE));
                    break;
                }
            case ICommonValues.BUG_STATUS_CLOSED:
                {
                    criteria.add(Restrictions.eq("sendEmailWhenAssigned", Boolean.TRUE));
                    break;
                }
            case ICommonValues.BUG_STATUS_FEEDBACK:
                {
                    criteria.add(Restrictions.eq("sendEmailWhenAssigned", Boolean.TRUE));
                    break;
                }
            case ICommonValues.BUG_STATUS_RESOLVED:
                {
                    criteria.add(Restrictions.eq("sendEmailWhenAssigned", Boolean.TRUE));
                    break;
                }
        }
    }

    private void saveSentMail(final String email, final String subject, final String body) {
        final SentMail sentMail = new SentMail();
        sentMail.setEmail(email);
        sentMail.setSubject(subject);
        sentMail.setBody(body);
        sentMail.setCreated(new Date());
        this.sentMailDAO.persist(sentMail);
    }

    private SMTPConnection getSMTPConnection() throws IOException {
        final PortalPreferences smtpExample = new PortalPreferences();
        smtpExample.setName("smtp_host");
        final PortalPreferences smtpPortExample = new PortalPreferences();
        smtpPortExample.setName("smtp_port");
        final List<PortalPreferences> smtpHostPref = this.portalPreferencesDAO.findByExample(smtpExample);
        final List<PortalPreferences> smtpPortPref = this.portalPreferencesDAO.findByExample(smtpPortExample);
        final String emailHostName = smtpHostPref.size() > 0 ? smtpHostPref.get(0).getValue() : "localhost";
        final int emailHostPort = smtpPortPref.size() > 0 ? Integer.parseInt(smtpPortPref.get(0).getValue()) : 25;
        return new SMTPConnection(emailHostName, emailHostPort);
    }

    private String getMailContent(final Bug newBug, final int bugStatusAssigned, final User referredUser, final String hostURL) {
        final BugStatus status = this.bugStatusDAO.findById(bugStatusAssigned);
        final StringBuffer sb = new StringBuffer("<html>");
        sb.append("User ").append(referredUser).append(" has changed bug ").append(this.getBugLink(newBug, hostURL));
        sb.append(" status to ").append(status.getDescription()).append("\n");
        this.loadHistoryEntries(sb, newBug);
        sb.append("</html>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void loadHistoryEntries(final StringBuffer sb, final Bug bug) {
        final DetachedCriteria criteria = DetachedCriteria.forClass(BugHistory.class);
        criteria.add(Restrictions.eq("bug", bug));
        final List<BugHistory> historyList = this.bugHistoryDAO.getHibernateTemplate().findByCriteria(criteria);
        if (historyList.size() > 0) {
            sb.append("\nBug History is:\n");
            sb.append("<table>");
            for (final BugHistory history : historyList) {
                sb.append("<tr>\n");
                sb.append("<td align='left' width='25%' bgcolor='#B5B5B5'>\n");
                sb.append("    &nbsp;").append(this.df.format(history.getDate())).append("\n");
                sb.append("</td>\n");
                sb.append("<td align='left' width='25%' bgcolor='#B5B5B5'>\n");
                sb.append("&nbsp;").append(history.getUser().getName()).append("\n");
                sb.append("</td>\n");
                sb.append("<td align='left' width='25%' bgcolor='#B5B5B5'>\n");
                sb.append("&nbsp;").append(history.getBugHistoryType().getDescription()).append("\n");
                sb.append("</td>\n");
                sb.append("<td align='left' width='25%' bgcolor='#B5B5B5'>\n");
                sb.append("&nbsp;").append(history.getDescription()).append("\n");
                sb.append("</td>\n");
                sb.append("</tr>\n");
            }
            sb.append("</table>");
        }
    }

    private String getBugLink(final Bug newBug, final String hostURL) {
        final StringBuffer sb = new StringBuffer();
        sb.append("<a href=\"").append(hostURL).append(this.spring.getServletContext().getContextPath());
        sb.append("/oneIssue?id=").append(newBug.getId()).append("\">").append(newBug.getId()).append("</a>");
        return sb.toString();
    }

    private String getSubject(final Bug newBug, final int bugStatusAssigned) {
        final BugStatus status = this.bugStatusDAO.findById(bugStatusAssigned);
        return status.getDescription() + " " + newBug.getId();
    }
}
