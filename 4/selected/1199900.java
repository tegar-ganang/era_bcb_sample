package net.cygeek.tech.client.main;

import net.cygeek.tech.client.ConfigDB;
import net.cygeek.tech.client.HsHrGeninfoPeer;
import net.cygeek.tech.client.jobs.JobManager;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Thilina Hasantha
 * @date: Sep 23, 2008
 * @time: 10:37:20 AM
 */
public class Configurator extends HttpServlet {

    public static String APP_HOME = "";

    public static boolean APP_HOME_CORRECT = false;

    public static boolean APP_INSTALLED = false;

    public static boolean TORQUE_WRITE = false;

    public static boolean OHRM_WRITE = false;

    public static boolean DB_NAME = false;

    public static String mysqlDB = "";

    public static String mysqlUser = "";

    public static String mysqlPassword = "";

    public static String mysqlhost = "";

    public static String mysqlPort = "";

    public void init(ServletConfig config) throws ServletException {
        APP_HOME = config.getInitParameter("app_home");
        System.out.println("===================================");
        System.out.println("App home:" + APP_HOME);
        System.out.println("DB Name:" + config.getInitParameter("db_name"));
        System.out.println("===================================");
        mysqlDB = config.getInitParameter("db_name");
        APP_HOME_CORRECT = checkAppHomeCorrect();
        if (APP_HOME_CORRECT) {
            try {
                String readFile = readFile(APP_HOME + "/config/app.conf");
                System.out.println("ohrm value " + readFile);
                if (readFile.trim().equals("1")) {
                    System.out.println("Application Installed");
                    APP_INSTALLED = true;
                    configureTorque();
                    startJobManager();
                } else {
                    System.out.println("Application not Installed");
                    APP_INSTALLED = false;
                    TORQUE_WRITE = isTorqueConfWrite();
                    System.out.println("Torque file writable :" + TORQUE_WRITE);
                    OHRM_WRITE = isOhrmWrite();
                    System.out.println("Ohrm writable :" + OHRM_WRITE);
                    if (mysqlDB == null || mysqlDB.trim() == "") {
                        System.out.println("Db name not specified");
                        System.out.println("No DB Name Specified,System stoped. Please correct app_home and restart tomcat");
                        DB_NAME = false;
                    } else {
                        DB_NAME = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(" app_home parameter is incorrect. System stoped. Please correct app_home" + "and restart tomcat");
        }
    }

    public static String getServerStstus() {
        TORQUE_WRITE = isTorqueConfWrite();
        OHRM_WRITE = isOhrmWrite();
        String s = "";
        s += (APP_HOME_CORRECT) ? "1" : "0";
        s += ",";
        s += (DB_NAME) ? "1" : "0";
        s += ",";
        s += (APP_INSTALLED) ? "1" : "0";
        s += ",";
        s += (TORQUE_WRITE) ? "1" : "0";
        s += ",";
        s += (OHRM_WRITE) ? "1" : "0";
        s += ",";
        s += mysqlDB;
        System.out.println("Server Status : " + s);
        return s;
    }

    public static boolean testDBConnection(String host, String port, String db, String user, String password) {
        try {
            mysqlDB = db.trim();
            mysqlhost = host.trim();
            mysqlPassword = password.trim();
            mysqlUser = user.trim();
            mysqlPort = port.trim();
            System.out.println("Server:" + host);
            System.out.println("Port:" + port);
            System.out.println("DB:" + db);
            System.out.println("User:" + user);
            System.out.println("Password:" + password);
            String readFile = readFile(APP_HOME + "/config/SampleTorque.properties");
            readFile = readFile.replace("#host#", host);
            readFile = readFile.replace("#db#", db);
            readFile = readFile.replace("#port#", port);
            readFile = readFile.replace("#user#", user);
            readFile = readFile.replace("#password#", password);
            writeFile(APP_HOME + "/config/Torque.properties", readFile);
            try {
                if (Torque.isInit()) {
                    Torque.shutdown();
                }
            } catch (TorqueException e) {
                e.printStackTrace();
            }
            return configureTorque();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean configureTorque() {
        try {
            System.out.println("Configuring Torque");
            ConfigDB.setDB(mysqlDB);
            Torque.init(APP_HOME + "/config/Torque.properties");
            try {
                HsHrGeninfoPeer.executeQuery("Select 1");
                System.out.println("Conf Torque Successful");
                return true;
            } catch (TorqueException e) {
                e.printStackTrace();
                return false;
            }
        } catch (TorqueException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean insertData(boolean insertSampleData) {
        System.out.println("Inserting Data");
        try {
            DriverManager.registerDriver((java.sql.Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
            Connection conn = DriverManager.getConnection("jdbc:mysql://" + mysqlhost + ":" + mysqlPort + "/" + mysqlDB, mysqlUser, mysqlPassword);
            System.out.println("DB Connection successful");
            SC runer = new SC(conn, false, false);
            runer.runScript(new BufferedReader(new FileReader(APP_HOME + "/config/schema/schema.sql")));
            System.out.println("Tables Created");
            runer.runScript(new BufferedReader(new FileReader(APP_HOME + "/config/schema/master-data.sql")));
            System.out.println("Master data Inserted");
            if (insertSampleData) {
                runer.runScript(new BufferedReader(new FileReader(APP_HOME + "/config/schema/sample-data.sql")));
                System.out.println("Sample Data Inserted");
            }
            APP_INSTALLED = true;
            writeFile(APP_HOME + "/config/app.conf", "1");
            configureTorque();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void startJobManager() {
        System.out.println("Starting ... JOB Manager");
        JobManager jm = new JobManager();
        jm.start();
    }

    public static boolean checkAppHomeCorrect() {
        File f = new File(APP_HOME + "/config/app.conf");
        return f.exists();
    }

    public static boolean isTorqueConfWrite() {
        File f = new File(APP_HOME + "/config/Torque.properties");
        if (f.exists()) {
            return f.canWrite();
        }
        return false;
    }

    public static boolean isWebInfWrite() {
        File f = new File(APP_HOME + "/WEB-INF/web.xml");
        if (f.exists()) {
            return f.canWrite();
        }
        return false;
    }

    public static boolean isOhrmWrite() {
        File f = new File(APP_HOME + "/config/app.conf");
        if (f.exists()) {
            return f.canWrite();
        }
        return false;
    }

    public static String readFile(String file) throws IOException {
        FileReader input = new FileReader(file);
        BufferedReader bufRead = new BufferedReader(input);
        StringBuilder sb = new StringBuilder("");
        String line = null;
        while ((line = bufRead.readLine()) != null) {
            if (!sb.toString().equals("")) {
                sb.append("\r\n");
            }
            sb.append(line);
        }
        bufRead.close();
        return sb.toString();
    }

    public static void writeFile(String file, String data) throws IOException {
        FileWriter fw = new FileWriter(file, false);
        fw.write(data);
        fw.flush();
        fw.close();
    }
}
