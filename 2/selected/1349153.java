package reports.utility.database;

import java.net.URL;

/**
 *
 * @author Administrator
 */
public class PostgresConnectionPool implements reports.utility.database.ConnectionPool {

    /** Creates a new instance of PostgresConnectionPool */
    private PostgresConnectionPool() {
        source = new org.postgresql.ds.PGPoolingDataSource();
        source.setDataSourceName("NewGenLibPostgresDataSource");
        String hostname = "";
        String databaseName = "";
        String username = "";
        String password = "";
        String portNumber = "";
        try {
            String serverIp = java.util.prefs.Preferences.systemRoot().get("serverurl", "localhost");
            String port = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
            String sysfilepath = reports.utility.NewGenLibDesktopRoot.getInstance().getURLRoot() + "/SystemFiles/postgres-ds.xml";
            System.out.println("SYS FILE PATH" + sysfilepath);
            java.net.URL url1 = new java.net.URL(sysfilepath);
            org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
            org.jdom.Document doc = sb.build(url1.openStream());
            org.jdom.Element rootEle = doc.getRootElement();
            org.jdom.Element sf = rootEle.getChild("local-tx-datasource");
            String url = "";
            url = sf.getChildTextTrim("connection-url");
            System.out.println("url = " + url);
            url = url.replaceFirst("localhost", serverIp);
            int start = url.indexOf("//");
            int end = url.lastIndexOf(":");
            hostname = url.substring(start + 2, end);
            start = url.lastIndexOf("/");
            portNumber = url.substring(end + 1, start);
            databaseName = url.substring(start + 1);
            System.out.println("Port number: " + portNumber);
            username = sf.getChildTextTrim("user-name");
            System.out.println("user name = " + username);
            password = sf.getChildTextTrim("password");
            System.out.println("password = " + password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        source.setServerName(hostname);
        source.setDatabaseName(databaseName);
        source.setUser(username);
        source.setPassword(password);
        source.setPortNumber(Integer.parseInt(portNumber));
    }

    public static PostgresConnectionPool getInstance() {
        if (thisClass == null) thisClass = new PostgresConnectionPool();
        return thisClass;
    }

    public java.sql.Connection getConnection() {
        java.sql.Connection conret = null;
        try {
            System.out.println("get connection" + source);
            System.out.println(source.getServerName());
            System.out.println(source.getDatabaseName());
            System.out.println(source.getPortNumber());
            conret = source.getConnection();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return conret;
    }

    private org.postgresql.ds.PGPoolingDataSource source;

    private static PostgresConnectionPool thisClass;
}
