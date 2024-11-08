package com.orientechnologies.odbms.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.orientechnologies.jdo.oConstants;

/**
 * RESTORE UTILITY FOR ORIENT ODBMS DATABASES
 * Copyright (c) 2001-2002
 * Orient Technologies (www.orientechnologies.com)
 *
 * @author Orient Staff (staff@orientechnologies.com)
 * @version 1.0
 */
public class DbRestore extends GenericTool {

    /**
     * Constructor for interactive execution
     */
    public DbRestore() {
    }

    /**
     * Constructor for API call.
     */
    public DbRestore(String iBatch) {
        parameters.put("force", "true");
    }

    public void start(String[] iArgs) throws Exception {
        loadArgs(iArgs);
        translate();
    }

    public void makeRestore(String iDatabase, String iArchiveName, String iOutputPath, MonitorableTool iMonitor) throws DatabaseNotFoundException, DbRestoreException {
        monitor = iMonitor;
        if (monitor != null) monitor.notifyStatus("Starting restore of database...", 1);
        String dbDirectory;
        if (iOutputPath == null || iOutputPath.length() == 0) dbDirectory = DbUtils.checkDatabase(iDatabase); else dbDirectory = iOutputPath;
        if (iArchiveName == null || iArchiveName.length() == 0) iArchiveName = iDatabase;
        if (!iArchiveName.substring(iArchiveName.length() - DbUtils.ARCHIVE_EXT.length()).equals(DbUtils.ARCHIVE_EXT)) iArchiveName += DbUtils.ARCHIVE_EXT;
        File archFile = loadArchive(iArchiveName);
        restoreDatabase(dbDirectory, archFile);
        if (monitor != null) monitor.notifyStatus("Restore database completed.", 100);
    }

    private void translate() throws DbRestoreException {
        String database = (String) parameters.get("database");
        String archive = (String) parameters.get("archive");
        String output = (String) parameters.get("output");
        try {
            System.out.print("Restoring the database <" + database + ">");
            if (archive != null) System.out.print(" from archive <" + archive + ">");
            if (output != null) System.out.print(" in directory <" + output + ">");
            System.out.print("...");
            makeRestore(database, archive, output, null);
            System.out.println("ok.");
        } catch (DatabaseNotFoundException e) {
            throw new DbRestoreException("ERROR! Database not found.");
        }
    }

    private void restoreDatabase(String iDbDirectory, File iArchFile) throws DbRestoreException {
        try {
            ZipFile archive = new ZipFile(iArchFile);
            File outFile;
            FileOutputStream outStream;
            byte[] buffer;
            ZipEntry entry;
            InputStream inStream;
            outFile = new File(iDbDirectory);
            outFile.mkdir();
            String force = (String) parameters.get("force");
            Enumeration entries = archive.entries();
            for (int fileIndex = 0; entries.hasMoreElements(); ++fileIndex) {
                entry = (ZipEntry) entries.nextElement();
                inStream = archive.getInputStream(entry);
                outFile = new File(iDbDirectory + "/" + entry.getName());
                if (monitor != null) monitor.notifyStatus("Restoring segment: " + entry.getName(), fileIndex * 100 / archive.size());
                if (force == null && outFile.exists()) {
                    System.out.println();
                    char response = getUserAdvisor().askToUser(System.out, "    Database already exist, overwrite ?", "#No,#Yes");
                    if (response != 'y' && response != 'Y') {
                        System.out.println();
                        throw new DbRestoreException("Restore aborted by user.");
                    }
                    System.out.print("    Overwriting...");
                    force = "true";
                }
                outStream = new FileOutputStream(outFile);
                int n;
                int nread = 0;
                int len = (int) entry.getSize();
                buffer = new byte[(int) len];
                while (nread < len) {
                    if ((n = inStream.read(buffer, nread, len - nread)) == -1) {
                        throw new DbRestoreException("ERROR! Cannot restore the database: error on reading archive file");
                    }
                    nread += n;
                }
                inStream.close();
                outStream.write(buffer);
                outStream.flush();
                outStream.close();
            }
        } catch (Exception e) {
            throw new DbRestoreException("ERROR! Cannot restore the database (" + e + ")");
        }
    }

    private File loadArchive(String iArchive) throws DbRestoreException {
        File archFile = new File(iArchive);
        if (!archFile.exists()) {
            throw new DbRestoreException("ERROR! Backup archive not found.");
        }
        return archFile;
    }

    private void loadArgs(String[] iArgs) {
        if (iArgs.length < 2 || iArgs[1].startsWith("-")) syntaxError("Missed <database> parameter");
        parameters.put("database", iArgs[1]);
        for (int i = 2; i < iArgs.length; ++i) {
            if (iArgs[i].startsWith("-a")) parameters.put("archive", iArgs[i].substring(2)); else if (iArgs[i].startsWith("-o")) parameters.put("output", iArgs[i].substring(2)); else if (iArgs[i].startsWith("-f")) parameters.put("force", "true");
        }
    }

    protected void printTitle() {
        System.out.println("Orient ODBMS oDbRestore v. " + oConstants.PRODUCT_VERSION + " - " + oConstants.PRODUCT_COPYRIGHTS + " (" + oConstants.PRODUCT_WWW + ")\n");
    }

    protected void printFormat() {
        System.out.println("Format: oDbRestore <database> [-a<archive>] [-o<output>] [-f]");
        System.out.println(" where: <database> is the database alias");
        System.out.println("        -a         Specify archive name path. Default value is");
        System.out.println("                    the alias of database in current directory (optional)");
        System.out.println("        -o         Specify the output directory for database. Default value");
        System.out.println("                    is the url configured for the database alias (optional)");
        System.err.println("        -f         Force overwrite of files (optional)");
    }

    private MonitorableTool monitor = null;

    public static class DbRestoreException extends Exception {

        public DbRestoreException(String iMsg) {
            super(iMsg);
        }
    }
}
