package hambo.remindserver;

import java.io.*;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Date;
import java.math.BigDecimal;
import com.lutris.appserver.server.Enhydra;
import com.lutris.util.Config;
import com.lutris.util.ConfigFile;
import com.lutris.appserver.server.StandardApplication;
import com.lutris.logging.StandardLogger;
import com.room33.osp.eventmonitor.shared.jms.io.Event;
import com.room33.osp.eventmonitor.EventMonitorFactory;
import hambo.messaging.*;
import hambo.svc.database.*;
import hambo.svc.log.Log;
import hambo.svc.log.LogServiceManager;
import hambo.svc.*;
import hambo.util.OID;

/**
 * Server thread responsible for starting threads to send reminders/notifications
 * via SMS and email messages.
 *
 */
public class RemindServer {

    private static MyApplication app = null;

    private EmailThread[] et = null;

    private SmsThread[] st = null;

    private Log log = null;

    private Properties prop = null;

    private int activeThreads = 0;

    private int sleeptime;

    private int search_interval;

    private int maxno_SMSThreads;

    private int maxno_EMAILThreads;

    private List smsList;

    private List emailList;

    private String evtmntPath;

    protected String remind_daemon_name = null;

    protected String remind_daemon_adress = null;

    protected String sms_trailer = null;

    private boolean isEventMonitorEnabled = false;

    public static void main(String[] args) {
        RemindServer r = new RemindServer();
        r.run();
    }

    public RemindServer() {
        smsList = Collections.synchronizedList(new ArrayList());
        emailList = Collections.synchronizedList(new ArrayList());
        String configPath = System.getProperty("osp.config.dir");
        if (configPath == null) {
            System.err.println("No system property defining the path to the remind.prop file.");
            System.exit(0);
        }
        prop = new Properties();
        try {
            prop.load(new FileInputStream(new File(configPath + "/OSP.conf")));
        } catch (FileNotFoundException ex) {
            System.err.println("Prop file not found in RemindServer");
            ex.printStackTrace();
            System.exit(0);
        } catch (IOException ex) {
            System.err.println("IOException in RemindServer");
            ex.printStackTrace();
            System.exit(0);
        }
        try {
            registerWithEnhydra(prop);
        } catch (Exception ex) {
            System.err.println("Exception in RemindServer when trying to register the application to Enhydra.");
            ex.printStackTrace();
            System.exit(0);
        }
        try {
            ServiceManagerLoader sml = new ServiceManagerLoader(prop);
            sml.loadServices();
            log = LogServiceManager.getLog("RemindServer");
            Messaging.init(prop);
        } catch (ServiceManagerException ex) {
            System.err.println("ServiceManagerException in RemindServer when trying to start logging");
            ex.printStackTrace();
            System.exit(0);
        } catch (Exception ex) {
            System.err.println("Exception in RemindServer when trying to start logging");
            ex.printStackTrace();
            System.exit(0);
        }
        maxno_SMSThreads = Integer.parseInt(prop.getProperty("maxno_SMSThreads"));
        maxno_EMAILThreads = Integer.parseInt(prop.getProperty("maxno_EMAILThreads"));
        isEventMonitorEnabled = (prop.getProperty("isEventMonitorEnabled", "")).equals("true");
        evtmntPath = prop.getProperty("eventMonitorPath");
        sleeptime = Integer.parseInt(prop.getProperty("wait_for"));
        search_interval = Integer.parseInt(prop.getProperty("search_interval"));
        remind_daemon_name = prop.getProperty("remind_daemon_name");
        remind_daemon_adress = prop.getProperty("remind_daemon_adress");
        sms_trailer = prop.getProperty("sms_trailer");
        et = new EmailThread[maxno_EMAILThreads];
        st = new SmsThread[maxno_SMSThreads];
    }

    public void log(int level, String msg) {
        log.println(level, msg);
    }

    public void log(int level, String msg, Throwable t) {
        log.println(level, msg, t);
    }

    public Log getLog() {
        return log;
    }

    public Properties getProperties() {
        return prop;
    }

    public synchronized Hashtable getSmsHT() {
        if (!smsList.isEmpty()) return (Hashtable) smsList.remove(0);
        return null;
    }

    public synchronized Hashtable getEmailHT() {
        if (!emailList.isEmpty()) return (Hashtable) emailList.remove(0);
        return null;
    }

    public synchronized void alterActiveThreads(int amount) {
        activeThreads += amount;
    }

    /**
     * <p>Fires a user event with the given event name and parameters. Event
     * parameters are provided using a 2-dimensional <code>String</code> 
     * array with name/value pairs.The event is recieved and processed by
     * the event monitor.
     * <p>
     * <pre>
     * logEvent("info", new String[][] {
     *     {"param1", "value1"},
     *     {"param2", "value2"},
     *     {"param3", "value3"}
     * }); 
     * </pre>
     *
     * @param eventName  the events name
     * @param params     the events parameters
     */
    public void logEvent(String eventName, String[][] params) {
        if (isEventMonitorEnabled) {
            Event e = EventMonitorFactory.createEvent(1, eventName);
            if (params != null) {
                if (params[0].length != 2) {
                    throw new IllegalArgumentException("parameter array must have the dimension String[*][2]");
                }
                for (int i = 0; i < params.length; i++) {
                    e.setParameter(params[i][0], params[i][1] != null ? params[i][1] : "null");
                }
            }
            EventMonitorFactory.getEventProducer(evtmntPath).sendEvent(e);
        }
    }

    public void run() {
        try {
            DBConnection con = null;
            while (true) {
                try {
                    long now = System.currentTimeMillis();
                    con = DBServiceManager.allocateConnection();
                    PreparedStatement ps = con.prepareStatement("select cal_Event_Remind.sms,cal_Event_Remind.email," + "cal_Event_Remind.rtime,cal_Event_Remind.subject,user_UserAccount.userid,user_UserAccount.OId as useroid," + "user_UserAccount.timezn,fwk_CountryPreferences.pref_country_code,user_UserAccount.language from " + "cal_Event_Remind,user_UserAccount,cal_Event,fwk_CountryPreferences where cal_Event_Remind.rtime<=? " + "and cal_Event_Remind.rtime>? and cal_Event.oid=cal_Event_Remind.event " + "and fwk_CountryPreferences.pref_country_name=user_UserAccount.country and cal_Event.owner=user_UserAccount.oid");
                    ps.setBigDecimal(1, new BigDecimal(now));
                    ps.setBigDecimal(2, new BigDecimal(now - search_interval));
                    log(Log.INFO, "Searching between long " + now + " and " + (now - search_interval));
                    if (!emailList.isEmpty() || !smsList.isEmpty()) log(Log.INFO, "WARNING Lists not empty - Email Count = " + emailList.size() + " and SMS Count = " + smsList.size());
                    ResultSet rs = ps.executeQuery();
                    int rowCount = 0;
                    while (rs.next()) {
                        rowCount++;
                        List tempList = emailList;
                        Hashtable ht = new Hashtable();
                        String key = "email";
                        String value = rs.getString("email");
                        if (value == null || value.equals("")) {
                            key = "sms";
                            value = rs.getString("sms");
                            tempList = smsList;
                        }
                        ht.put(key, value);
                        ht.put("user_id", rs.getString("userid"));
                        ht.put("user_oid", new OID(rs.getBigDecimal("useroid")));
                        ht.put("rtime", rs.getBigDecimal("rtime"));
                        ht.put("subject", rs.getString("subject"));
                        ht.put("timezn", rs.getString("timezn"));
                        ht.put("countrycode", rs.getString("pref_country_code"));
                        ht.put("language", rs.getString("language"));
                        tempList.add(ht);
                    }
                    for (int i = 0; i < maxno_EMAILThreads && i < emailList.size(); i++) {
                        if (et[i] == null || !et[i].isAlive()) {
                            et[i] = new EmailThread(this);
                            et[i].start();
                        }
                    }
                    for (int i = 0; i < maxno_SMSThreads && i < smsList.size(); i++) {
                        if (st[i] == null || !st[i].isAlive()) {
                            st[i] = new SmsThread(this);
                            st[i].start();
                        }
                    }
                    ps = con.prepareStatement("delete from cal_Event_Remind where rtime<=?");
                    ps.setBigDecimal(1, new BigDecimal(now));
                    int deletedRows = ps.executeUpdate();
                    log(Log.INFO, "There is " + rowCount + " reminders");
                    if (deletedRows > rowCount) log(Log.INFO, "WARNING Did not send " + (deletedRows - rowCount) + " old reminders");
                } catch (SQLException ex) {
                    log(Log.ERROR, "SQLException in remindserver", ex);
                    throw ex;
                } finally {
                    if (con != null) {
                        con.release();
                    }
                }
                try {
                    int activetime = 0;
                    while (activeThreads > 0) {
                        Thread.sleep(100);
                        activetime += 100;
                    }
                    log(Log.INFO, "No active threads, killing is allowed");
                    Thread.sleep(sleeptime - activetime > 0 ? sleeptime - activetime : 0);
                    log(Log.INFO, "WARNING Work in progress. DONT SHUT DOWN!!!");
                } catch (InterruptedException ex) {
                    log(Log.ERROR, "InterruptedException in RemindServer, exiting", ex);
                    System.exit(0);
                }
            }
        } catch (Exception ex) {
            log(Log.ERROR, "Exception  in remindserver, exiting", ex);
            System.exit(1);
        } finally {
            unregister();
        }
    }

    /**
     * Register a new application with enhydra and load OSP services.  This
     * has to be done before doing anything that requires database of log
     * info.
     */
    private void registerWithEnhydra(Properties prop) throws IOException, ServiceManagerException, com.lutris.util.ConfigException, com.lutris.appserver.server.ApplicationException {
        if (app == null) {
            app = new MyApplication();
            String configLocation = (String) prop.get("enhydra.config");
            app.setConfig((new ConfigFile(new File(configLocation))).getConfig());
            String logLocation = (String) prop.get("enhydra.logfile");
            File logFile = new File(logLocation);
            String[] levels = { "ERROR", "OSP_ERROR", "OSP_INFO", "OSP_DEBUG1", "OSP_DEBUG2", "OSP_DEBUG3" };
            StandardLogger logger = new StandardLogger(true);
            logger.configure(logFile, new String[] {}, levels);
            app.setLogChannel(logger.getChannel(""));
            app.startup(app.getConfig());
            Enhydra.register(app);
        }
    }

    /**
      * Register the current thread with the application of this service. This
      * <em>must</em> be called before each thread uses the service system.
      */
    public static void register() {
        Enhydra.register(app);
    }

    /**
      * Register the current thread with the application of this service.  This
      * <em>must</em> be called before a registered thread terminates.
      */
    public static void unregister() {
        Enhydra.unRegister();
    }

    class MyApplication extends StandardApplication {

        public void setConfig(Config config) {
            this.config = config;
        }
    }
}
