package org.palettelabs.dbcodesvnsync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.CRC32;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SyncObject {

    public String name = "";

    public DataSource dataSource = null;

    public String workingCopy = "";

    public String repositoryPath = "";

    public String userName = "";

    public String password = "";

    public String query = "";

    public String nameColumn = "";

    public String typeColumn = "";

    public String ownerColumn = "";

    public String dateColumn = "";

    private Logger logger = null;

    private SVNClientManager clientManager = null;

    private Connection sourceSQLConnection = null;

    private PreparedStatement sourceSQLStatement = null;

    public SyncObject() {
        logger = Logger.getLogger("org.palettelabs.dbcodesvnsync.basic");
    }

    public void sync() {
        logger.info("start");
        try {
            logger.info("checking working copy...");
            File wcdir = new File(workingCopy);
            String[] filenames = wcdir.list();
            if ((filenames != null) && (filenames.length > 0)) {
                logger.info("updating working copy...");
                createManager();
                SVNUpdateClient updateClient = clientManager.getUpdateClient();
                updateClient.setIgnoreExternals(false);
                long rev = updateClient.doUpdate(wcdir, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
                logger.info("at revision " + rev);
            } else {
                logger.info("checking out working copy...");
                createManager();
                SVNUpdateClient updateClient = clientManager.getUpdateClient();
                updateClient.setIgnoreExternals(false);
                SVNURL url = SVNURL.parseURIEncoded(repositoryPath);
                long rev = updateClient.doCheckout(url, wcdir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
                logger.info("at revision " + rev);
            }
            DataFetcher fetcher = new DataFetcher(dataSource.getConnection());
            int resultsFetched = 0;
            Vector<HashMap<String, String>> resultsData = new Vector<HashMap<String, String>>();
            fetcher.open(query);
            ArrayList<File> commitFiles = new ArrayList<File>();
            ArrayList<File> addFiles = new ArrayList<File>();
            prepeareSourceSQL();
            SVNWCClient wcClient = clientManager.getWCClient();
            do {
                resultsData = fetcher.fetch(500);
                resultsFetched = resultsData.size();
                for (HashMap<String, String> m : resultsData) {
                    String name = m.get(nameColumn);
                    String type = m.get(typeColumn);
                    String owner = m.get(ownerColumn);
                    String date = m.get(dateColumn);
                    DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    long datemills = formatter.parse(date).getTime();
                    File wcfile = getWCFile(type, name);
                    boolean mustBeCommited = false;
                    if (!wcfile.exists() || (wcfile.lastModified() < datemills)) {
                        boolean isAdd = !wcfile.exists();
                        String source = getSource(owner, type, name);
                        if (!isAdd) {
                            byte[] s1 = source.getBytes();
                            byte[] s2 = getStoredBytes(wcfile);
                            CRC32 c1 = new CRC32();
                            c1.update(s1);
                            CRC32 c2 = new CRC32();
                            c2.update(s2);
                            long cs1 = c1.getValue();
                            long cs2 = c2.getValue();
                            if (cs1 != cs2) {
                                storeSource(wcfile, source);
                                commitFiles.add(wcfile);
                                mustBeCommited = true;
                                logger.info("updated file \"" + wcfile.getName() + "\" (" + type + ")");
                            } else logger.info("file \"" + wcfile.getName() + "\" (" + type + ") is out of date, but it wasn't changed");
                        } else {
                            storeSource(wcfile, source);
                            addFiles.add(wcfile);
                            commitFiles.add(wcfile);
                            mustBeCommited = true;
                            logger.info("created file \"" + wcfile.getName() + "\" (" + type + ")");
                        }
                        String plist = "";
                        for (String field : m.keySet()) {
                            try {
                                if (field.toLowerCase().startsWith("p$")) {
                                    String propertyName = field.substring(2);
                                    String propertyValue = m.get(field);
                                    SVNPropertyData pdata = wcClient.doGetProperty(wcfile, propertyName, SVNRevision.HEAD, SVNRevision.HEAD);
                                    String storedPropertyValue = SVNPropertyValue.getPropertyAsString(pdata.getValue());
                                    if (!storedPropertyValue.equals(propertyValue)) {
                                        wcClient.doSetProperty(wcfile, propertyName, SVNPropertyValue.create(propertyValue), false, SVNDepth.INFINITY, null, null);
                                        plist += propertyName + "=" + propertyValue + ", ";
                                    }
                                }
                            } catch (SVNException se) {
                                logger.error("property processing error for file \"" + wcfile.getName() + "\" (" + type + "), class: " + se.getClass().getName() + ", message:" + se.getMessage());
                            }
                        }
                        if (!plist.isEmpty()) {
                            plist = plist.substring(0, plist.length() - 2);
                            if (!mustBeCommited) commitFiles.add(wcfile);
                            logger.info("property processing for file \"" + wcfile.getName() + "\" (" + type + ") (" + plist + ")");
                        }
                    }
                }
            } while (resultsFetched >= 500);
            File[] commitFilesArray = new File[commitFiles.size()];
            commitFilesArray = commitFiles.toArray(commitFilesArray);
            File[] addFilesArray = new File[addFiles.size()];
            addFilesArray = addFiles.toArray(addFilesArray);
            logger.info("adding new files...");
            wcClient.doAdd(addFilesArray, false, false, true, SVNDepth.INFINITY, false, false, true);
            logger.info("combining commit packet...");
            SVNCommitClient commitClient = clientManager.getCommitClient();
            SVNCommitPacket commitPacket = commitClient.doCollectCommitItems(commitFilesArray, false, false, SVNDepth.INFINITY, null);
            logger.info("commiting...");
            commitClient.doCommit(commitPacket, false, "dbcode-svn-sync bot revision, sync-object \"" + name + "\"");
            logger.info("commited (" + commitFilesArray.length + " file(s) total, " + addFilesArray.length + " new file(s))");
        } catch (Exception e) {
            logger.fatal("sync exception, class: " + e.getClass().getName() + ", message: " + e.getMessage());
        } finally {
            closeSourceSQL();
        }
        logger.info("end");
    }

    private void createManager() {
        DAVRepositoryFactory.setup();
        DefaultSVNOptions options = new DefaultSVNOptions();
        clientManager = SVNClientManager.newInstance(options, userName, password);
    }

    private File getWCFile(String type, String name) {
        String dirName = "";
        if (type.equalsIgnoreCase("FUNCTION")) dirName = "functions"; else if (type.equalsIgnoreCase("PROCEDURE")) dirName = "procedures"; else if (type.equalsIgnoreCase("PACKAGE")) dirName = "packages"; else if (type.equalsIgnoreCase("TRIGGER")) dirName = "triggers";
        String fsep = System.getProperty("file.separator");
        File file = new File(workingCopy + fsep + dirName + fsep + name + ".sql");
        return file;
    }

    private String getSource(String owner, String type, String name) throws Exception {
        sourceSQLStatement.setString(1, owner);
        sourceSQLStatement.setString(2, type);
        if (type.equalsIgnoreCase("PACKAGE")) sourceSQLStatement.setString(3, "PACKAGE BODY"); else if (type.equalsIgnoreCase("PACKAGE BODY")) sourceSQLStatement.setString(3, "PACKAGE"); else sourceSQLStatement.setString(3, type);
        sourceSQLStatement.setString(4, name);
        ResultSet resultSet = sourceSQLStatement.executeQuery();
        String text = "create or replace ";
        String selectedType0 = "";
        while (resultSet.next()) {
            String selectedType = resultSet.getString(1);
            if (selectedType0.isEmpty()) selectedType0 = selectedType;
            if (type.equalsIgnoreCase("PACKAGE") && !selectedType.equals(selectedType0)) text += "/\ncreate or replace ";
            text += resultSet.getString(2);
            selectedType0 = selectedType;
        }
        text += "/";
        resultSet.close();
        sourceSQLConnection.commit();
        return text;
    }

    private void prepeareSourceSQL() throws Exception {
        sourceSQLConnection = dataSource.getConnection();
        sourceSQLStatement = sourceSQLConnection.prepareStatement("select type, text from dba_source where owner = ? and type in (?, ?) and name = ? order by type, line asc");
    }

    private void closeSourceSQL() {
        try {
            sourceSQLStatement.close();
        } catch (Exception e) {
        }
    }

    private void storeSource(File file, String source) throws Exception {
        PrintWriter writer = new PrintWriter(file);
        writer.print(source);
        writer.flush();
        writer.close();
    }

    private byte[] getStoredBytes(File file) throws Exception {
        FileInputStream stream = new FileInputStream(file);
        ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
        int len = 0;
        byte[] buf = new byte[1024];
        while ((len = stream.read(buf)) > -1) arrayStream.write(buf, 0, len);
        stream.close();
        buf = arrayStream.toByteArray();
        arrayStream.close();
        return buf;
    }
}
