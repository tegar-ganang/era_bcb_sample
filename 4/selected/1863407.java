package com.ma_la.myRunning;

import com.ma_la.myRunning.domain.RunnerEvent;
import com.ma_la.myRunning.domain.Starter;
import com.ma_la.myRunning.domain.Runner;
import com.ma_la.myRunning.domain.News;
import com.ma_la.myRunning.domain.EventRoute;
import com.ma_la.myRunning.domain.Event;
import com.ma_la.myRunning.domain.Admin;
import com.ma_la.util.Mail;
import com.ma_la.util.Constants;
import com.ma_la.util.MailQueueBean;
import com.ma_la.HibernateUtil;
import com.ma_la.myRunning.manager.EmailManager;
import com.ma_la.myRunning.manager.BenchmarkManager;
import com.ma_la.myRunning.manager.EventManager;
import com.ma_la.myRunning.manager.NewsManager;
import com.ma_la.myRunning.domain.EmailType;
import com.ma_la.myRunning.domain.EmailTypeRunner;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.apache.log4j.Logger;

/**
 * steuert die taegliche Task-Ausfuehrung
 *
 * @author <a href="mailto:mail@myrunning.de">Martin Lang</a>
 */
public class ReminderTask extends HttpServlet {

    private static final long serialVersionUID = -5307774322197986991L;

    Timer timer = null;

    private static Logger log = Logger.getLogger(ReminderTask.class);

    @Override
    public void init() throws ServletException {
        log.info("Start des ReminderTask-Servlet");
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new DailyReportGenerator(getServletContext()), 5000, 1000 * 60 * 60 * 24);
        }
    }

    @Override
    public void destroy() {
        log.info("Ende des ReminderTask-Servlet");
        timer.cancel();
    }
}

/**
 * prueft, ob und welche Reports erzeugt und versendet werden muessen
 *
 * @author <a href="mailto:mail@myrunning.de">Martin Lang</a>
 */
class DailyReportGenerator extends TimerTask {

    private RunningMasterBean runningMasterBean;

    private MailQueueBean mailQueueBean;

    private SessionFactory sf;

    private static Logger log = Logger.getLogger(DailyReportGenerator.class);

    private LogBean logBean;

    int counterMyCalendarEntries = 0;

    int counterAdminReport = 0;

    int counterNewCalewndarEntries = 0;

    public DailyReportGenerator(ServletContext context) {
        runningMasterBean = new RunningMasterBean();
        runningMasterBean.setServletContext(context);
        logBean = new LogBean();
        RunningSystemBean runningSystemBean = new RunningSystemBean();
        runningSystemBean.setRunningMasterBean(runningMasterBean);
        runningMasterBean.setRunningSystemBean(runningSystemBean);
        mailQueueBean = MailQueueBean.getInstance();
        this.logBean.initLogFile(context.getRealPath("log") + File.separatorChar + "TaskLog", Integer.parseInt(context.getInitParameter(Constants.InitParameter.LOGLEVEL)));
        runningMasterBean.setLogFile(logBean);
    }

    public void run() {
        log.info("Start ReportGenerator");
        sf = HibernateUtil.getSessionFactory();
        counterMyCalendarEntries = 0;
        counterAdminReport = 0;
        counterNewCalewndarEntries = 0;
        checkMyCalendarEntries();
        checkCalendarEntries();
        createAdminReport();
        sf.getCurrentSession().beginTransaction();
        Mail mail = new Mail();
        mail.setSender(RunningSystemBean.getNewsletterSenderMyRunning());
        mail.setReceiver(RunningSystemBean.getNewsletterSenderMyRunning());
        mail.setSubject("myRunning-Task-Auswertung!");
        mail.setMessage("<br/><h2>Auswertung Reminder-Task fuer den " + RunningSystemBean.formatToDateGerman(new Date()) + "</h2>" + "MyCalendarEntries: " + counterMyCalendarEntries + "<br/>" + "counterNewCalendarEntries: " + counterNewCalewndarEntries + "<br/>" + "counterAdminReport: " + counterAdminReport + "<br/>");
        mail.setSmtpServer(RunningSystemBean.getSMTPHostMyRunning());
        mail.setPopServer(RunningSystemBean.getPopHostMyRunning());
        mail.setSmtpUser(RunningSystemBean.getSMTPUserMyRunning());
        mail.setSmtpPassword(RunningSystemBean.getSMTPPasswordMyRunning());
        mail.setEmailType(EmailManager.getInstance().getEmailTypeById(Constants.Values.EmailTypes.REMINDER_TASK));
        mailQueueBean.addMail(mail);
        RunningSystemBean.writeLog(logBean, "Send Mails", LogBean.MESSAGE, "started!");
        log.info("Send Mails");
        mailQueueBean.sendMails();
        sf.getCurrentSession().getTransaction().commit();
        sf.getCurrentSession().close();
        log.info("End Run ReminderTask");
    }

    /**
	 * prueft, ob es neue Termine gibt, an die das Portal per E-Mail informieren muss
	 */
    private void checkCalendarEntries() {
        RunningSystemBean.writeLog(logBean, "checkCalendarEntries", LogBean.MESSAGE, "started!");
        log.info("checkCalendarEntries");
        sf.getCurrentSession().beginTransaction();
        try {
            Session session = HibernateUtil.currentSession();
            GregorianCalendar yesterdayDate = new GregorianCalendar();
            yesterdayDate.setTime(new Date());
            yesterdayDate.set(Calendar.HOUR_OF_DAY, 0);
            yesterdayDate.set(Calendar.SECOND, 0);
            yesterdayDate.set(Calendar.MINUTE, 0);
            yesterdayDate.set(Calendar.MILLISECOND, 0);
            yesterdayDate.add(Calendar.DATE, -1);
            GregorianCalendar todayDate = new GregorianCalendar();
            todayDate.setTime(new Date());
            todayDate.set(Calendar.HOUR_OF_DAY, 0);
            todayDate.set(Calendar.SECOND, 0);
            todayDate.set(Calendar.MINUTE, 0);
            todayDate.set(Calendar.MILLISECOND, 0);
            List<Event> newEvents = session.createQuery("From Event e" + " Where (e.creationDate >= :yesterday And e.creationDate < :today)" + " And e.checked is not null And e.checked = true And e.eventDate > :today" + " Order by e.eventDate").setParameter("yesterday", yesterdayDate.getTime()).setParameter("today", todayDate.getTime()).list();
            List<Event> changedEvents = session.createQuery("From Event e" + " Where e.lastModificationDate >= :yesterday And e.lastModificationDate < :today" + " And e.checked is not null And e.checked = true And e.eventDate > :today" + " Order by e.eventDate").setParameter("yesterday", yesterdayDate.getTime()).setParameter("today", todayDate.getTime()).list();
            changedEvents.removeAll(newEvents);
            if (newEvents.size() == 0 && changedEvents.size() == 0) return;
            StringBuffer newsHeader = new StringBuffer("");
            StringBuffer newsText = new StringBuffer("");
            String at_date = RunningSystemBean.getLocalizedText(Locale.GERMAN, "at_date");
            String in_town = RunningSystemBean.getLocalizedText(Locale.GERMAN, "in_town");
            if (newEvents.size() > 0) {
                if (newEvents.size() == 1) {
                    newsHeader.append(RunningSystemBean.getLocalizedText(Locale.GERMAN, "added_single_publicrun"));
                } else {
                    newsHeader.append(RunningSystemBean.getLocalizedTextStatic(Locale.GERMAN, "added_multiple_publicrun", new String[] { "" + newEvents.size() }));
                }
                newsText.append("<ul>");
                for (Event event : newEvents) {
                    newsText.append("<li>").append(RunningSystemBean.getLocalizedTextStatic(Locale.GERMAN, "news_name_date_town", new String[] { event.getName(), RunningSystemBean.formatToDateGerman(event.getEventDate()), event.getZipWithFallBack() + " " + event.getTownWithFallBack() })).append("</li>");
                }
                newsText.append("</ul>");
            }
            if (changedEvents.size() > 0) {
                if (changedEvents.size() == 1) {
                } else {
                }
                for (Event event : changedEvents) {
                    newsText.append("<li>").append(RunningSystemBean.getLocalizedTextStatic(Locale.GERMAN, "news_name_date_town", new String[] { event.getName(), RunningSystemBean.formatToDateGerman(event.getEventDate()), event.getZipWithFallBack() + " " + event.getTownWithFallBack() })).append("</li>");
                }
            }
            if (newsHeader.length() > 0 && NewsManager.getInstance().getNewsBySubjectAndDate(newsHeader.toString(), new Date()) == null) {
                News news = new News();
                news.setStartDate(new Date());
                news.setHeader(newsHeader.toString());
                news.setNewsType(NewsManager.getInstance().getNewsTypeById((long) Constants.Values.NewsTypes.PUBLIC_RUN_CREATED));
                news.setText(newsText.toString());
                NewsManager.getInstance().saveNews(news);
            }
            EmailType emailType = EmailManager.getInstance().getEmailTypeById(Constants.Values.EmailTypes.EVENT_REGISTERED);
            int runnersInformed = 0;
            Collection<EmailTypeRunner> emailTypeRunners = EmailManager.getInstance().getEmailTypeRunner(emailType);
            Runner runner;
            for (EmailTypeRunner emailTypeRunner : emailTypeRunners) {
                runner = emailTypeRunner.getRunner();
                String receiverAddress = runner.getEmail();
                if (!RunningSystemBean.checkEMail(receiverAddress)) continue;
                if (runner.hasUserAccount()) {
                    Locale locale = getLocale(runner);
                    if (runner.getZip() != null && runner.getDefaultRadius() != null) {
                        newEvents = EventManager.getInstance().filterEventsRadius(runner.getZip(), runner.getDefaultRadius().intValue(), newEvents);
                        changedEvents = EventManager.getInstance().filterEventsRadius(runner.getZip(), runner.getDefaultRadius().intValue(), changedEvents);
                    }
                    StringBuffer newEventsText = new StringBuffer("");
                    StringBuffer changedEventsText = new StringBuffer("");
                    if (newEvents.size() > 0) {
                        if (newEvents.size() == 1) {
                            newEventsText.append(RunningSystemBean.getLocalizedText(locale, "calendar_added_single_publicrun"));
                            newEventsText.append(":<p>");
                        } else {
                            newEventsText.append(RunningSystemBean.getLocalizedText(locale, "calendar_added_multiple_publicrun"));
                            newEventsText.append(":<p>");
                        }
                        newEventsText.append("<ul>");
                        for (Event event : newEvents) {
                            newEventsText.append("<li><b><a href=\"http://www.myrunning.de?p=40&veranstaltung=").append(event.getEventID()).append("\">").append(event.getName()).append("</a></b> ").append(RunningSystemBean.getLocalizedText(locale, "at_date")).append(" <b>").append(RunningSystemBean.formatToDateGerman(event.getEventDate())).append("</b> ").append(RunningSystemBean.getLocalizedText(locale, "in_town")).append(" <b>").append(event.getZipWithFallBack()).append(" ").append(event.getTownWithFallBack()).append("</b></li>");
                        }
                        newEventsText.append("</ul>");
                    }
                    changedEvents.removeAll(newEvents);
                    if (changedEvents.size() > 0) {
                        if (changedEvents.size() == 1) {
                            changedEventsText.append(RunningSystemBean.getLocalizedText(locale, "calendar_updated_single_publicrun"));
                            changedEventsText.append(":<p>");
                        } else {
                            changedEventsText.append(RunningSystemBean.getLocalizedText(locale, "calendar_updated_multiple_publicrun"));
                            changedEventsText.append(":<p>");
                        }
                        changedEventsText.append("<ul>");
                        for (Event event : changedEvents) {
                            changedEventsText.append("<li><b><a href=\"http://www.myrunning.de?p=40&veranstaltung=").append(event.getEventID()).append("\">").append(event.getName()).append("</a></b> ").append(RunningSystemBean.getLocalizedText(locale, "at_date")).append(" <b>").append(RunningSystemBean.formatToDateGerman(event.getEventDate())).append("</b> ").append(RunningSystemBean.getLocalizedText(locale, "in_town")).append(" <b>").append(event.getZipWithFallBack()).append(" ").append(event.getTownWithFallBack()).append("</b></li>");
                        }
                        changedEventsText.append("</ul>");
                    }
                    Mail mail = new Mail();
                    mail.setSender(RunningSystemBean.getSenderMyRunning());
                    mail.setReceiver(receiverAddress);
                    mail.setSmtpServer(RunningSystemBean.getSMTPHostMyRunning());
                    mail.setPopServer(RunningSystemBean.getPopHostMyRunning());
                    mail.setSmtpUser(RunningSystemBean.getSMTPUserMyRunning());
                    mail.setSmtpPassword(RunningSystemBean.getSMTPPasswordMyRunning());
                    mail.setSubject(RunningSystemBean.getLocalizedText(locale, "added_actualized_publicruns"));
                    String mailText = getSalutation(runner) + newEventsText.toString() + changedEventsText.toString();
                    mailText += runningMasterBean.getRunningSystemBean().getSalutationContactFooter(locale) + "<small><br/>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_subscribed_notifications", new String[] { receiverAddress }) + "</small><br/>" + runningMasterBean.getRunningSystemBean().getNewsletterDeActivationMailFooter(locale, emailTypeRunner) + "<p>" + runningMasterBean.getRunningSystemBean().getAutomaticMailFooter(locale, receiverAddress);
                    mail.setMessage(mailText);
                    mail.setEmailType(emailType);
                    if ((newEventsText.toString() + changedEventsText.toString()).length() > 0) {
                        mailQueueBean.addMail(mail);
                        runnersInformed++;
                        RunningSystemBean.writeLog(logBean, new Date() + " - Send Event-Notification im ReminderTask to " + receiverAddress, LogBean.MESSAGE, "");
                    }
                }
            }
            counterNewCalewndarEntries = runnersInformed;
        } catch (HibernateException e) {
            throw new RuntimeException(e.getMessage());
        }
        sf.getCurrentSession().getTransaction().commit();
        sf.getCurrentSession().close();
        RunningSystemBean.writeLog(logBean, "checkCalendarEntries", LogBean.MESSAGE, "ended!");
    }

    /**
	 * prueft, ob es heute Termine gibt, an die das Portal per E-Mail erinnern muss
	 */
    private void checkMyCalendarEntries() {
        RunningSystemBean.writeLog(logBean, "checkMyCalendarEntries", LogBean.MESSAGE, "started!");
        log.info("checkMyCalendarEntries");
        sf.getCurrentSession().beginTransaction();
        try {
            Session session = HibernateUtil.currentSession();
            List<RunnerEvent> unDoneNotifications = session.createQuery("From RunnerEvent re" + " Where (re.notified is Null Or re.notified = false)" + " And re.notificationPeriod is not null").list();
            Calendar compareDate = RunningSystemBean.getCalendarS();
            Calendar eventDate = RunningSystemBean.getCalendarS();
            String receiverAddress;
            for (RunnerEvent runnerEvent : unDoneNotifications) {
                Event event = runnerEvent.getEvent();
                if (event != null) eventDate.setTime(event.getEventDate()); else eventDate.setTime(runnerEvent.getEventDate());
                log.debug("runnerEvent.getNotificationPeriod().getDays(): " + runnerEvent.getNotificationPeriod().getDays());
                compareDate.setTime(new Date());
                compareDate.add(Calendar.DATE, runnerEvent.getNotificationPeriod().getDays());
                log.debug("compareDate: " + compareDate.getTime() + " - eventDate: " + eventDate.getTime());
                if (eventDate.get(Calendar.DATE) == compareDate.get(Calendar.DATE) && eventDate.get(Calendar.MONTH) == compareDate.get(Calendar.MONTH) && eventDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR)) {
                    Runner runner = runnerEvent.getRunner();
                    receiverAddress = runner.getEmail();
                    if (!RunningSystemBean.checkEMail(receiverAddress)) continue;
                    if (receiverAddress != null && receiverAddress.length() > 0) {
                        Locale locale = getLocale(runner);
                        String text = getSalutation(runner) + RunningSystemBean.getLocalizedText(locale, "reminder_myrunning_event_detail") + ":<p><b>" + runnerEvent.getDisplayName() + "</b> " + RunningSystemBean.getLocalizedText(locale, "at_date") + " <b>" + RunningSystemBean.formatToDateGerman(eventDate.getTime()) + "</b>";
                        if (runnerEvent.getEvent() != null) {
                            text += " " + RunningSystemBean.getLocalizedText(locale, "in_town") + " <b>" + runnerEvent.getEvent().getTown() + "</b>";
                        }
                        text += ".<p>";
                        if (event != null) {
                            text += RunningSystemBean.getLocalizedText(locale, "informations_more_at") + " <a href=\"http://www.myrunning.de?p=40&veranstaltung=" + event.getEventID() + "\">www.myrunning.de?p=40&veranstaltung=" + event.getEventID() + "</a><br/>";
                        }
                        text += "<br/>" + runningMasterBean.getRunningSystemBean().getSalutationContactFooter(locale) + "<hr><small>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_subscribed_reminder", new String[] { receiverAddress }) + "</small><p>" + runningMasterBean.getRunningSystemBean().getAutomaticMailFooter(locale, receiverAddress);
                        Mail mail = new Mail();
                        mail.setSender(RunningSystemBean.getNewsletterSenderMyRunning());
                        mail.setReceiver(receiverAddress);
                        mail.setSubject(RunningSystemBean.getLocalizedText(locale, "reminder_myrunning_event"));
                        mail.setMessage(text);
                        mail.setRunner(runner);
                        mail.setEmailType(EmailManager.getInstance().getEmailTypeById(Constants.Values.EmailTypes.EVENT_REMINDER));
                        mail.setSmtpServer(RunningSystemBean.getSMTPHostMyRunning());
                        mail.setPopServer(RunningSystemBean.getPopHostMyRunning());
                        mail.setSmtpUser(RunningSystemBean.getSMTPUserMyRunning());
                        mail.setSmtpPassword(RunningSystemBean.getSMTPPasswordMyRunning());
                        counterMyCalendarEntries++;
                        mailQueueBean.addMail(mail);
                        RunningSystemBean.writeLog(logBean, new Date() + " - Send Notification im ReminderTask to " + receiverAddress, LogBean.MESSAGE, "");
                        runnerEvent.setNotified(true);
                    }
                }
            }
        } catch (HibernateException e) {
            throw new RuntimeException(e.getMessage());
        }
        sf.getCurrentSession().getTransaction().commit();
        sf.getCurrentSession().close();
        RunningSystemBean.writeLog(logBean, "checkMyCalendarEntries", LogBean.MESSAGE, "ended!");
    }

    /**
	 * prueft, ob es Volkslaeufe gibt, fuer die eine Benachrichtigung ueber
	 * - wie oft wurde die Veranstaltung angesehen
	 * - wie oft wurde die Veranstaltung in den myCalendar getan
	 * - wie oft wurde der Link zu HP angeklickt
	 * - wie oft wurde wurde der Link zur Online-Anmeldung angeklickt
	 * - ob ein Ergebnis importiert wurde
	 */
    private void createAdminReport() {
        RunningSystemBean.writeLog(logBean, "createAdminReport", LogBean.MESSAGE, "started");
        log.info("createAdminReport");
        sf.getCurrentSession().beginTransaction();
        GregorianCalendar date = new GregorianCalendar();
        date.setTime(new Date());
        date.add(Calendar.MONTH, -1);
        Date dateFrom = date.getTime();
        date.add(Calendar.DATE, 1);
        Collection<Event> events = EventManager.getInstance().getEvents(Boolean.TRUE, null, dateFrom, date.getTime(), null, null, null, null, null);
        String receiverEMailAddress;
        for (Event event : events) {
            Locale locale = Locale.GERMAN;
            int homepageVistis = event.getHomepageVisits();
            int regVisits = event.getRegistrationVisits();
            int runnerViews = event.getViewed();
            int myCalendarEntries = event.getMyCalendarEntries();
            double bewertung = event.getBewertungAvg();
            if (runnerViews < 5 && myCalendarEntries < 3) {
                continue;
            }
            receiverEMailAddress = event.getContactEmail();
            String text = RunningSystemBean.getLocalizedText(Locale.GERMAN, "dear.informal");
            if (RunningSystemBean.getStringValue(event.getContactPerson()).length() > 0) {
                text += " " + RunningSystemBean.getStringValue(event.getContactPerson());
            }
            text += ",<p>";
            text += RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_intro", new String[] { event.getName() }) + ":<br/><ul>";
            text += "<li>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_viewed", new String[] { "" + runnerViews }) + "</li>";
            if (myCalendarEntries > 1) {
                text += "<li>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_reminder", new String[] { "" + myCalendarEntries }) + "</li>";
            }
            if (event.getHomepage() != null && event.getHomepage().length() > 0 && homepageVistis > 1) {
                text += "<li>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_homepage", new String[] { "" + homepageVistis }) + "</li>";
            }
            if (event.getOnlineRegistrationUrlFormatted() != null && event.getOnlineRegistrationUrlFormatted().length() > 0 && regVisits > 1) {
                text += "<li>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_registration_link", new String[] { "" + regVisits }) + "</li>";
            }
            int bewertungsCount = BenchmarkManager.getInstance().getBewertungsCount(event);
            if (bewertung > 0.0 && bewertungsCount > 1) {
                text += "<li>" + RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_benchmark", new String[] { "" + bewertung, "" + bewertungsCount }) + "</li>";
            }
            text += "</ul><br/>";
            Set<EventRoute> eventRoutes = event.getEventRoutes();
            Iterator iter = eventRoutes.iterator();
            Collection<EventRoute> resultRoutes = new ArrayList<EventRoute>();
            while (iter.hasNext()) {
                EventRoute eventRoute = (EventRoute) iter.next();
                if (eventRoute.getResultAvailable()) {
                    resultRoutes.add(eventRoute);
                }
            }
            if (resultRoutes.size() > 0) {
                if (resultRoutes.size() == 1) text += RunningSystemBean.getLocalizedText(locale, "result_imported_route"); else text += RunningSystemBean.getLocalizedText(locale, "results_imported_routes");
                text += ":" + "<ul>";
                for (EventRoute eventRoute : resultRoutes) {
                    text += "<li>" + eventRoute.getRoute().getName() + "</li>";
                }
                text += "</ul><p>";
            } else {
                text += RunningSystemBean.getLocalizedText(locale, "email_admin_eventstatistics_noresults") + "<p>";
            }
            Set<Admin> admins = event.getAdmins();
            for (Admin admin : admins) {
                text += RunningSystemBean.getLocalizedText(locale, "logindata_yours") + ":<br/>" + RunningSystemBean.getLocalizedText(locale, "web_adress") + ": http://admin.myrunning.de<br/>" + RunningSystemBean.getLocalizedText(locale, "runnerid_username") + ": " + admin.getRunner().getFrontendRunnerID() + "<br/>" + RunningSystemBean.getLocalizedText(locale, "password") + ": " + admin.getPassword() + "<p>";
            }
            Mail mail = new Mail();
            if (!RunningSystemBean.checkEMail(receiverEMailAddress)) {
                receiverEMailAddress = RunningSystemBean.getNewsletterSenderMyRunning();
            } else {
                mail.setBccReceiver(RunningSystemBean.getNewsletterSenderMyRunning());
            }
            text += RunningSystemBean.getLocalizedTextStatic(locale, "email_admin_eventstatistics_advanced", new String[] { event.getName() }) + "<p>" + runningMasterBean.getRunningSystemBean().getSalutationContactFooter(locale) + "<p>" + runningMasterBean.getRunningSystemBean().getAutomaticMailFooter(locale, receiverEMailAddress);
            mail.setSender(RunningSystemBean.getSenderMyRunning());
            mail.setReceiver(receiverEMailAddress);
            String subject = RunningSystemBean.getLocalizedTextStatic(locale, "analysis_mr_for", new String[] { event.getName() });
            mail.setSubject(subject);
            mail.setMessage(text);
            mail.setEmailType(EmailManager.getInstance().getEmailTypeById(Constants.Values.EmailTypes.EVENT_ANALYSIS));
            mail.setSmtpServer(RunningSystemBean.getSMTPHostMyRunning());
            mail.setPopServer(RunningSystemBean.getPopHostMyRunning());
            mail.setSmtpUser(RunningSystemBean.getSMTPUserMyRunning());
            mail.setSmtpPassword(RunningSystemBean.getSMTPPasswordMyRunning());
            if (!EmailManager.getInstance().isMailSent(mail)) {
                counterAdminReport++;
                mailQueueBean.addMail(mail);
                RunningSystemBean.writeLog(logBean, new Date() + " - Try to send Admin-Report in ReminderTask to " + receiverEMailAddress, LogBean.MESSAGE, "");
            } else {
                RunningSystemBean.writeLog(logBean, new Date() + " - Admin-Report for  >" + event.getName() + "< in ReminderTask already sent to " + receiverEMailAddress, LogBean.ERROR, "");
            }
        }
        sf.getCurrentSession().getTransaction().commit();
        sf.getCurrentSession().close();
        RunningSystemBean.writeLog(logBean, "createAdminReport", LogBean.MESSAGE, "ended!");
    }

    private String getSalutation(Runner runner) {
        String text = "";
        Locale locale = getLocale(runner);
        if (runner.getGender().getId().intValue() == 1) text += RunningSystemBean.getLocalizedText(locale, "dear.male"); else if (runner.getGender().getId().intValue() == 2) text += RunningSystemBean.getLocalizedText(locale, "dear.female");
        return text += " " + RunningSystemBean.getSalutation(locale, runner) + ",<p>";
    }

    private Locale getLocale(Runner runner) {
        return RunningSystemBean.getLocale(runner.getLanguageId());
    }
}
