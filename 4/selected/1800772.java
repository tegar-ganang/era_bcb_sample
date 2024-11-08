package ch.ethz.dcg.spamato.base.common.main;

import java.util.*;
import ch.ethz.dcg.plugin.*;
import ch.ethz.dcg.plugin.config.CommitableConfiguration;
import ch.ethz.dcg.spamato.factory.common.Mail;
import ch.ethz.dcg.spamato.factory.common.main.*;
import ch.ethz.dcg.spamato.webconfig.*;
import ch.ethz.dcg.thread.*;

public class SpamatoPageHandler extends AbstractPageHandler {

    SpamatoImpl spamato;

    CommitableConfiguration config = null;

    public SpamatoPageHandler(PluginContext context, SpamatoImpl spamato, Configuration config) {
        super(context);
        this.spamato = spamato;
        this.config = new CommitableConfiguration(config);
    }

    public String getTitle(String page) {
        if ("threads".equals(page)) {
            return String.format("%1$s - Threads", super.getTitle(page));
        } else if ("options".equals(page)) {
            return String.format("%1$s - Options", super.getTitle(page));
        }
        return super.getTitle(page);
    }

    public String getDescription(String page) {
        if ("threads".equals(page)) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Spamato uses several threads when checking a message.");
            buffer.append(" Each PreChecker and Filter runs in its own thread.");
            buffer.append(" This drastically increases the overall filtering speed, since some filters are---due to blocking I/O (network) operations---rather slow compared to others.");
            buffer.append(" When changing the thread numbers below, beware that they are related to each other.");
            buffer.append(" For instance, the number of PreChecker/Filter threads is always an upper bound for the number of active Process threads.");
            buffer.append("<br><br>");
            buffer.append("Also note that when no inactive thread is available, a new task is buffered until a thread becomes available.");
            return buffer.toString();
        } else if ("process_threads".equals(page)) {
            return "This page gives an overview of the emails currently being checked.";
        } else if ("filter_threads".equals(page)) {
            return "This page gives an overview of the emails currently being checked.";
        } else if ("options".equals(page)) {
            return "On this page, you can tweak some parameters of this plugin";
        }
        return super.getDescription(page);
    }

    public void renderPage(String page, Hashtable<String, String> parameters, HtmlWriter writer) {
        if ("threads".equals(page)) {
            renderThreadsPage(parameters, writer);
        } else if ("process_threads".equals(page)) {
            renderProcessThreadsPage(parameters, writer);
        } else if ("filter_threads".equals(page)) {
            renderFilterThreadsPage(parameters, writer);
        } else if ("options".equals(page)) {
            renderOptionsPage(parameters, writer);
        } else {
            renderMainPage(parameters, writer);
        }
    }

    private void renderMainPage(Hashtable<String, String> parameters, HtmlWriter writer) {
        writer.startPanel("General Information");
        writer.writeLnIndent("<p>");
        writer.writeLnIndent(String.format("This instance of Spamato has been started by <b>%1$s</b> (%2$s).", SpamatoUtils.getClientName(spamato.getClientIdentifier()), spamato.getClientIdentifier()));
        writer.writeLnIndent(String.format("All your personal settings (also referred to as your <i>Profile Directory</i>) is stored at: <b>%1$s</b>.", SpamatoFactory.getFactory().getProfileDirectory().getAbsolutePath()));
        writer.writeLnIndent("</p>");
        writer.writeLnIndent("<p>");
        writer.writeLnIndent("Spamato's homepage is available at <a href='http://www.spamato.net' target='_blank' style='font-weight:bold;'>http://www.spamato.net</a>.");
        writer.writeLnIndent("Spamato is being hosted on SourceForge: <a href='http://sf.net/projects/spamato' target='_blank' style='font-weight:bold;'>http://sf.net/projects/spamato</a>.");
        writer.writeLnIndent("</p>");
        writer.writeLnIndent("<p>");
        writer.writeLnIndent("If you have any questions regarding Spamato, please find our forums here: <a href='http://sourceforge.net/forum/?group_id=136033' target='_blank' style='font-weight:bold;'>http://sourceforge.net/forum/?group_id=136033</a>.");
        writer.writeLnIndent("You can also send us an email: <a href='mailto:info@spamato.net' style='font-weight:bold;'>info@spamato.net</a>.");
        writer.writeLnIndent("</p>");
        writer.endPanel();
    }

    private void renderThreadsPage(Hashtable<String, String> parameters, HtmlWriter writer) {
        writer.startPanel("WARNING");
        writer.writeLn("On this page, you can alter some key functions of the Spamato system.");
        writer.writeLn("Please be aware that you should really know what you are doing; otherwise, Spamato might not work properly anymore.");
        writer.writeLn("(If you know what you are doing, you might be able to optimize your Spamato system for your particular machine and get some info about what's going on.)");
        writer.addBr();
        writer.addBr();
        writer.writeLn("<b>You have been warned!</b>");
        writer.endPanel();
        writer.startPanel("Process Thread Pool");
        writer.writeLn("The Process Thread Pool limits the number of concurrent email checks.");
        writer.addBr();
        writer.addBr();
        writer.writeLn("You can change the initial ");
        writer.addTextField(writer.getPropertyName(SpamatoSettings.INITIAL_PROCESS_THREADS_PROPERTY_NAME), config.get(SpamatoSettings.INITIAL_PROCESS_THREADS_PROPERTY_NAME), "2");
        writer.writeLn(" and maximum ");
        writer.addTextField(writer.getPropertyName(SpamatoSettings.MAX_PROCESS_THREADS_PROPERTY_NAME), config.get(SpamatoSettings.MAX_PROCESS_THREADS_PROPERTY_NAME), "2");
        writer.writeLn(" number of threads.");
        writer.write("You can set the ");
        writer.addButton("default", "?action=defaultProcessThreads");
        writer.writeLn(" values here.");
        writer.writeLn("<b>Don't forget to click 'Save' to apply your changes.</b>");
        writer.addBr();
        writer.addBr();
        writer.writeLn("<b>Current state</b>");
        writer.addBr();
        writer.writeLn("All Threads: " + spamato.processThreadPool.getThreadNumber() + "<br>");
        writer.writeLn(String.format("Active Threads: %1d<br>", spamato.processThreadPool.getActiveThreadNumber()));
        writer.writeLn("Bored Threads: " + spamato.processThreadPool.getBoredThreadNumber() + "<br>");
        writer.writeLn("Buffered Tasks: " + spamato.processThreadPool.getBufferedTaskNumber() + "<br>");
        writer.addBr();
        writer.writeLnIndent(String.format("<a href='%1s' target='_top'>Click here to see more information about emails currently being checked.</a>", getComponentPath() + "process_threads"));
        writer.endPanel();
        writer.startPanel("PreChecker/Filter Thread Pool");
        writer.writeLn("The PreChecker/Filter Thread Pool limits the number of concurrently running PreChecker/Filters.");
        writer.writeLn("We handle this separately since one email check invokes several PreCheckers/Filters.");
        writer.addBr();
        writer.addBr();
        writer.writeLn("You can change the initial ");
        writer.addTextField(writer.getPropertyName(SpamatoSettings.INITIAL_FILTER_THREADS_PROPERTY_NAME), config.get(SpamatoSettings.INITIAL_FILTER_THREADS_PROPERTY_NAME), "2");
        writer.writeLn(" and maximum ");
        writer.addTextField(writer.getPropertyName(SpamatoSettings.MAX_FILTER_THREADS_PROPERTY_NAME), config.get(SpamatoSettings.MAX_FILTER_THREADS_PROPERTY_NAME), "2");
        writer.writeLn(" number of threads.");
        writer.write("You can set the ");
        writer.addButton("default", "?action=defaultFilterThreads");
        writer.writeLn(" values here.");
        writer.writeLn("<b>Don't forget to click 'Save' to apply your changes.</b>");
        writer.addBr();
        writer.addBr();
        writer.writeLn("<b>Current state</b>");
        writer.addBr();
        writer.writeLn("All Threads: " + spamato.filterThreadPool.getThreadNumber() + "<br>");
        writer.writeLn(String.format("Active Threads: %1d<br>", spamato.filterThreadPool.getActiveThreadNumber()));
        writer.writeLn("Bored Threads: " + spamato.filterThreadPool.getBoredThreadNumber() + "<br>");
        writer.writeLn("Buffered Tasks: " + spamato.filterThreadPool.getBufferedTaskNumber() + "<br>");
        writer.addBr();
        writer.writeLnIndent(String.format("<a href='%1s' target='_top'>Click here to see more information about emails currently being checked.</a>", getComponentPath() + "filter_threads"));
        writer.endPanel();
    }

    private void renderProcessThreadsPage(Hashtable<String, String> parameters, HtmlWriter writer) {
        writer.startPanel("Currently Checked Emails");
        Vector<TaskThread> threads = spamato.processThreadPool.getActiveThreads();
        synchronized (threads) {
            if (threads.size() > 0) {
                writer.writeLnIndent("<p>");
                writer.writeLnIndent(String.format("Currently, the following %1d emails are being checked:", threads.size()));
                writer.addBr();
                writer.writeLnIndent("</p>");
                boolean rowType = true;
                writer.writeLnIndent("<table class='list' cellspacing='0'><tr><th class='list' align='left'><b>Subject</b></th></tr>");
                for (TaskThread thread : threads) {
                    FilterProcess process = (FilterProcess) thread.getTask();
                    if (process != null) {
                        writer.writeLnIndent(String.format("<tr style='background-color:%1s;'>", rowType ? "#FFF4E4" : "#FFFFFF"));
                        rowType = !rowType;
                        writer.writeLnIndent("<td valign='top'>");
                        String subject = process.getMail().getSubject();
                        if (subject == null || subject.trim().equals("")) subject = "[no subject found by Spamato]";
                        writer.writeLnIndent(subject);
                        writer.writeLnIndent("</td></tr>");
                    }
                }
                writer.writeLnIndent("</table>");
            } else {
                writer.writeLnIndent("No emails are being checked.");
            }
        }
        writer.endPanel();
        writer.startPanel("Buffered Emails");
        Vector<Task> tasks = spamato.processThreadPool.getBufferedTasks();
        synchronized (tasks) {
            if (tasks.size() > 0) {
                writer.writeLnIndent("<p>");
                writer.writeLnIndent(String.format("Currently, the following %1d emails have been buffered:", tasks.size()));
                writer.writeLnIndent("</p>");
                boolean rowType = true;
                writer.writeLnIndent("<table class='list' cellspacing='0'><tr><th class='list' align='left'><b>Subject</b></th></tr>");
                for (Task task : tasks) {
                    FilterProcess process = (FilterProcess) task;
                    writer.writeLnIndent(String.format("<tr style='background-color:%1s;'>", rowType ? "#FFF4E4" : "#FFFFFF"));
                    rowType = !rowType;
                    writer.writeLnIndent("<td valign='top'>");
                    String subject = process.getMail().getSubject();
                    if (subject == null || subject.trim().equals("")) subject = "[no subject found by Spamato]";
                    writer.writeLnIndent(subject);
                    writer.writeLnIndent("</td></tr>");
                }
                writer.writeLnIndent("</table>");
            } else {
                writer.writeLnIndent("No emails are buffered.");
            }
        }
        writer.endPanel();
    }

    private void renderFilterThreadsPage(Hashtable<String, String> parameters, HtmlWriter writer) {
        writer.startPanel("Active PreCheckers &amp; Filters");
        Vector<TaskThread> threads = spamato.filterThreadPool.getActiveThreads();
        synchronized (threads) {
            int activePreCheckers = 0;
            int activeFilters = 0;
            if (threads.size() > 0) {
                Hashtable<Mail, Vector<String>> mails = new Hashtable<Mail, Vector<String>>();
                for (TaskThread thread : threads) {
                    String name;
                    Mail mail;
                    Task mainTask = thread.getTask();
                    if (mainTask != null) {
                        if (mainTask instanceof PreCheckerTask) {
                            PreCheckerTask task = (PreCheckerTask) mainTask;
                            mail = task.getFilterProcessResult().getMail();
                            name = task.getPreChecker().getPreCheckerName() + " (PreChecker)";
                            activePreCheckers++;
                        } else {
                            FilterTask task = (FilterTask) mainTask;
                            mail = task.getFilterProcessResult().getMail();
                            name = task.getFilter().getName();
                            activeFilters++;
                        }
                        Vector<String> names = mails.get(mail);
                        if (names == null) {
                            names = new Vector<String>();
                            mails.put(mail, names);
                        }
                        names.add(name);
                    }
                }
                writer.writeLnIndent("<p>");
                writer.writeLnIndent(String.format("Currently, %1d PreCheckers and %2d Filters are checking the following %3d emails:", activePreCheckers, activeFilters, mails.size()));
                writer.writeLnIndent("</p>");
                boolean rowType = true;
                writer.writeLnIndent("<table class='list' cellspacing='0'>");
                writer.writeLnIndent("<tr><th class='list' align='left'><b>Subject</b></th><th class='list' align='left'>&nbsp;</th><th class='list' align='left'><b>PreChecker / Filter</b></th></tr>");
                for (Mail mail : mails.keySet()) {
                    Vector<String> names = mails.get(mail);
                    writer.writeLnIndent(String.format("<tr style='background-color:%1s;'>", rowType ? "#FFF4E4" : "#FFFFFF"));
                    rowType = !rowType;
                    writer.writeLnIndent(String.format("<td valign='top'>%1s</td><td>&nbsp;</td>", mail.getSubject()));
                    writer.writeLnIndent("<td valign='top'>");
                    for (String name : names) {
                        writer.writeLnIndent(name);
                        writer.addBr();
                    }
                    writer.writeLnIndent("</td></tr>");
                }
                writer.writeLnIndent("</table>");
            } else {
                writer.writeLnIndent("No emails are being checked.");
            }
        }
        writer.endPanel();
        writer.startPanel("Buffered PreCheckers &amp; Filters");
        Vector<Task> tasks = spamato.filterThreadPool.getBufferedTasks();
        synchronized (tasks) {
            int bufferedPreCheckers = 0;
            int bufferedFilters = 0;
            if (tasks.size() > 0) {
                Hashtable<Mail, Vector<String>> mails = new Hashtable<Mail, Vector<String>>();
                for (Task task : tasks) {
                    String name;
                    Mail mail;
                    if (task instanceof PreCheckerTask) {
                        PreCheckerTask pcTask = (PreCheckerTask) task;
                        mail = pcTask.getFilterProcessResult().getMail();
                        name = pcTask.getPreChecker().getPreCheckerName() + " (PreChecker)";
                        bufferedPreCheckers++;
                    } else {
                        FilterTask fTask = (FilterTask) task;
                        mail = fTask.getFilterProcessResult().getMail();
                        name = fTask.getFilter().getName();
                        bufferedFilters++;
                    }
                    Vector<String> names = mails.get(mail);
                    if (names == null) {
                        names = new Vector<String>();
                        mails.put(mail, names);
                    }
                    names.add(name);
                }
                writer.writeLnIndent("<p>");
                writer.writeLnIndent(String.format("Currently, %1d PreCheckers and %2d Filters are buffered to check the following %3d emails:", bufferedPreCheckers, bufferedFilters, mails.size()));
                writer.writeLnIndent("</p>");
                boolean rowType = true;
                writer.writeLnIndent("<table class='list' cellspacing='0'>");
                writer.writeLnIndent("<tr><th class='list' align='left'><b>Subject</b></th><th class='list' align='left'>&nbsp;</th><th class='list' align='left'><b>PreChecker / Filter</b></th></tr>");
                for (Mail mail : mails.keySet()) {
                    Vector<String> names = mails.get(mail);
                    writer.writeLnIndent(String.format("<tr style='background-color:%1s;'>", rowType ? "#FFF4E4" : "#FFFFFF"));
                    rowType = !rowType;
                    writer.writeLnIndent(String.format("<td valign='top'>%1s</td><td>&nbsp;</td>", mail.getSubject()));
                    writer.writeLnIndent("<td valign='top'>");
                    for (String name : names) {
                        writer.writeLnIndent(name);
                        writer.addBr();
                    }
                    writer.writeLnIndent("</td></tr>");
                }
                writer.writeLnIndent("</table>");
            } else {
                writer.writeLnIndent("No emails are buffered.");
            }
        }
        writer.endPanel();
    }

    private void renderOptionsPage(Hashtable<String, String> parameters, HtmlWriter writer) {
        writer.startPanel("Options");
        writer.writeLnIndent("<table>");
        writer.writeLnIndent("<tr>");
        writer.writeLnIndent("<td colspan='2'>");
        writer.writeLnIndent("<b>Emails that are larger than ");
        writer.addTextField(writer.getPropertyName(SpamatoSettings.MAX_SPAM_SIZE_PROPERTY_NAME), config.get(SpamatoSettings.MAX_SPAM_SIZE_PROPERTY_NAME), "2");
        writer.writeLnIndent(" kB are not examined by Spamato.</b>");
        writer.writeLnIndent("</td>");
        writer.writeLnIndent("</tr>");
        writer.writeLnIndent("<tr>");
        writer.writeLnIndent("<td>&nbsp;</td>");
        writer.writeLnIndent("<td style='padding-left:24px;padding-bottom:16px;'>");
        writer.writeLnIndent("Emails that are larger than this size are considered legitimate by default.");
        writer.writeLnIndent("This prevents Spamato from checking emails of extreme size that spam usually does not reach.");
        writer.writeLnIndent("</td>");
        writer.writeLnIndent("</tr>");
        writer.writeLnIndent("</table>");
        writer.writeLnIndent("<p><b>");
        writer.writeLnIndent("Don't forget to click 'Save' to apply your changes.");
        writer.writeLnIndent("Please notice that you probably have to restart Spamato afterwards!");
        writer.writeLnIndent("</b></p>");
        writer.endPanel();
    }

    public String executeAction(String name, Hashtable<String, String> parameters) {
        if ("defaultProcessThreads".equals(name)) {
            config.set(SpamatoSettings.INITIAL_PROCESS_THREADS_PROPERTY_NAME, String.valueOf(SpamatoSettings.INITIAL_PROCESS_THREADS));
            config.set(SpamatoSettings.MAX_PROCESS_THREADS_PROPERTY_NAME, String.valueOf(SpamatoSettings.MAX_PROCESS_THREADS));
        } else if ("defaultFilterThreads".equals(name)) {
            config.set(SpamatoSettings.INITIAL_FILTER_THREADS_PROPERTY_NAME, String.valueOf(SpamatoSettings.INITIAL_FILTER_THREADS));
            config.set(SpamatoSettings.MAX_FILTER_THREADS_PROPERTY_NAME, String.valueOf(SpamatoSettings.MAX_FILTER_THREADS));
        }
        return null;
    }

    public void save() {
        int maxSpamSize = getFixedConfig(SpamatoSettings.MAX_SPAM_SIZE_PROPERTY_NAME);
        int initProcessThreads = getFixedConfig(SpamatoSettings.INITIAL_PROCESS_THREADS_PROPERTY_NAME);
        int maxProcessThreads = getFixedConfig(SpamatoSettings.MAX_PROCESS_THREADS_PROPERTY_NAME);
        int initFilterThreads = getFixedConfig(SpamatoSettings.INITIAL_FILTER_THREADS_PROPERTY_NAME);
        int maxFilterThreads = getFixedConfig(SpamatoSettings.MAX_FILTER_THREADS_PROPERTY_NAME);
        if (initProcessThreads > maxProcessThreads) {
            maxProcessThreads = initProcessThreads;
            config.set(SpamatoSettings.MAX_PROCESS_THREADS_PROPERTY_NAME, String.valueOf(initProcessThreads));
        }
        if (initFilterThreads > maxFilterThreads) {
            maxFilterThreads = initFilterThreads;
            config.set(SpamatoSettings.MAX_FILTER_THREADS_PROPERTY_NAME, String.valueOf(initFilterThreads));
        }
        config.commit();
        if (spamato.processThreadPool.getThreadNumber() < initProcessThreads) {
            spamato.processThreadPool.alterThreadNumber(initProcessThreads, false);
        }
        if (spamato.processThreadPool.getThreadNumber() > maxProcessThreads) {
            spamato.processThreadPool.alterThreadNumber(maxProcessThreads, false);
        }
        if (spamato.filterThreadPool.getThreadNumber() < initFilterThreads) {
            spamato.filterThreadPool.alterThreadNumber(initFilterThreads, false);
        }
        if (spamato.filterThreadPool.getThreadNumber() > maxFilterThreads) {
            spamato.filterThreadPool.alterThreadNumber(maxFilterThreads, false);
        }
    }

    private int getFixedConfig(String propertyName) {
        int value = 0;
        try {
            value = Integer.parseInt(config.get(propertyName));
            if (value < 0) {
                String s = config.getOriginal(propertyName);
                try {
                    value = Integer.parseInt(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                config.set(propertyName, String.valueOf(value));
            }
        } catch (NumberFormatException e) {
            value = Integer.parseInt(config.getOriginal(propertyName));
            config.set(propertyName, String.valueOf(value));
        }
        return value;
    }

    public void abort() {
        config.abort();
    }

    public Configuration getConfig(String name) {
        return config;
    }
}
