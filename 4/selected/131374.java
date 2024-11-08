package org.palettelabs.dbcodesvnsync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.palettelabs.libs.support.UsageStatisticsManager;
import org.palettelabs.libs.xml.XMLKeeper;

public class Starter {

    private static Vector<SyncObject> syncObjects = new Vector<SyncObject>();

    private static boolean submitUsageStat = true;

    public static void main(String[] args) {
        PropertyConfigurator.configure(ClassLoader.getSystemResource("org/palettelabs/dbcodesvnsync/log4j.properties"));
        Logger logger = Logger.getLogger("org.palettelabs.dbcodesvnsync.basic");
        if (args.length < 1) {
            System.out.println("please specify a configuration file");
            logger.fatal("please specify a configuration file");
        } else {
            String filename = args[0];
            try {
                loadConfiguration(filename);
                if (submitUsageStat) {
                    if (UsageStatisticsManager.submitNow("dbcode-svn-sync", "main()", "", "0.1")) {
                        logger.info("usage statistic data of this software was succesfully submited " + "(data collecter is located at palettelabs.org, to turn off this feature add " + "\"<configuration>...<usage-stat>off</usage-stat>...</configuration>\")");
                    } else {
                        logger.info("usage statistic data of this software wasn't succesfully submited " + "(data collecter is located at palettelabs.org, to turn off this feature add " + "\"<configuration>...<usage-stat>off</usage-stat>...</configuration>\"). " + "Or check your network settings (proxy, firewall, etc.) in case of you would like to enable it.");
                    }
                }
                startSyncThreads();
            } catch (Exception e) {
                logger.fatal("configuration exception, class: " + e.getClass().getName() + ", message: " + e.getMessage());
            }
        }
    }

    private static void loadConfiguration(String filename) throws Exception {
        File file = new File(filename);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            int b = 0;
            while ((b = fileInputStream.read()) > 0) outputStream.write(b);
        } finally {
            fileInputStream.close();
        }
        XMLKeeper c = new XMLKeeper(outputStream.toByteArray());
        String stat = c.getNodeValueByPath("configuration.usage-stat");
        if ((stat == null) || stat.isEmpty() || stat.equalsIgnoreCase("false") || stat.equalsIgnoreCase("off")) submitUsageStat = false;
        String syncObjectsPath = "configuration.sync-objects.sync";
        int syncObjectsCount = c.getNodesCountByPath(syncObjectsPath) + (c.contains(syncObjectsPath) ? 1 : 0);
        for (int f = 0; f < syncObjectsCount; f++) {
            String syncObjectPath = syncObjectsPath + (f > 0 ? f : "");
            SyncObject syncObject = new SyncObject();
            syncObject.name = c.getNodeValueByPath(syncObjectPath + ".name");
            String dataSourcePath = syncObjectPath + ".datasource";
            DataSource dataSource = new DataSource();
            String name = c.getNodeValueByPath(dataSourcePath + ".name");
            String driver = c.getNodeValueByPath(dataSourcePath + ".driver");
            String url = c.getNodeValueByPath(dataSourcePath + ".url");
            dataSource.setName(name);
            dataSource.setDriver(driver);
            dataSource.setUrl(url);
            if (c.contains(dataSourcePath + ".properties")) {
                Properties properties = new Properties();
                String propertiesPath = dataSourcePath + ".properties.property";
                int pcount = c.getNodesCountByPath(propertiesPath) + (c.contains(propertiesPath) ? 1 : 0);
                for (int n = 0; n < pcount; n++) {
                    String properyPath = propertiesPath + (n > 0 ? n : "");
                    String pname = c.getNodeValueByPath(properyPath + ".name");
                    String pvalue = c.getNodeValueByPath(properyPath + ".value");
                    properties.put(pname, pvalue);
                }
                dataSource.setProperties(properties);
            }
            syncObject.dataSource = dataSource;
            syncObject.workingCopy = c.getNodeValueByPath(syncObjectPath + ".working-copy");
            syncObject.repositoryPath = c.getNodeValueByPath(syncObjectPath + ".repository-path");
            syncObject.userName = c.getNodeValueByPath(syncObjectPath + ".auth.name");
            syncObject.password = c.getNodeValueByPath(syncObjectPath + ".auth.password");
            syncObject.query = c.getNodeValueByPath(syncObjectPath + ".datasource.query.<cdata>");
            syncObject.nameColumn = c.getNodeValueByPath(syncObjectPath + ".datasource.name-column-name");
            syncObject.typeColumn = c.getNodeValueByPath(syncObjectPath + ".datasource.type-column-name");
            syncObject.ownerColumn = c.getNodeValueByPath(syncObjectPath + ".datasource.owner-column-name");
            syncObject.dateColumn = c.getNodeValueByPath(syncObjectPath + ".datasource.date-column-name");
            syncObjects.add(syncObject);
        }
    }

    private static void startSyncThreads() {
        Vector<Thread> threads = new Vector<Thread>();
        for (SyncObject so : syncObjects) {
            SyncThread r = new SyncThread(so);
            Thread t = new Thread(r, "sync_thread[" + so.name + "]");
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) try {
            t.join();
        } catch (InterruptedException e) {
        }
    }
}
