package com.orientechnologies.odbms.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.orientechnologies.jdo.oConstants;

/**
 * BACKUP UTILITY FOR ORIENT ODBMS DATABASES
 * Copyright (c) 2001-2002
 * Orient Technologies (www.orientechnologies.com)
 *
 * @author Orient Staff (staff@orientechnologies.com)
 * @version 1.0
 */
public class DbBackup extends GenericTool {

    /**
     * Constructor for interactive execution
     */
    public DbBackup() {
        parameters.put("force", "true");
    }

    public void start(String[] iArgs) throws Exception {
        loadArgs(iArgs);
        translate();
    }

    public void makeBackup(String iDatabase, String iArchiveName, MonitorableTool iMonitor) throws DatabaseNotFoundException, DbBackupException {
        monitor = iMonitor;
        String dbDirectory = DbUtils.checkDatabase(iDatabase);
        File f = new File(dbDirectory + "/" + SYSTEM_NAME);
        if (!f.exists()) throw new DatabaseNotFoundException(iDatabase);
        if (monitor != null) monitor.notifyStatus("Starting database backup...", 1);
        if (iArchiveName == null || iArchiveName.length() == 0) iArchiveName = iDatabase;
        if (!iArchiveName.substring(iArchiveName.length() - DbUtils.ARCHIVE_EXT.length()).equals(DbUtils.ARCHIVE_EXT)) iArchiveName += DbUtils.ARCHIVE_EXT;
        createArchive(dbDirectory, iArchiveName);
        if (monitor != null) monitor.notifyStatus("Backup completed.", 100);
    }

    private void translate() throws DbBackupException {
        String database = (String) parameters.get("database");
        String archive = (String) parameters.get("archive");
        try {
            System.out.print("Creating backup of database <" + database + ">");
            if (archive != null) System.out.print(" in archive <" + archive + ">");
            System.out.print("...");
            makeBackup(database, archive, null);
            System.out.println("ok.");
        } catch (DatabaseNotFoundException e) {
            throw new DbBackupException("ERROR! Database not found.");
        }
    }

    private void createArchive(String iDatabaseDir, String iArchiveName) throws DbBackupException {
        try {
            File archiveFile = new File(iArchiveName);
            String force = (String) parameters.get("force");
            if (force == null && archiveFile.exists()) {
                char response = getUserAdvisor().askToUser(System.out, "    Archive already exist, overwrite ?", "#Yes,#No");
                if (response != 'y' && response != 'Y') {
                    System.out.println();
                    throw new DbBackupException("Backup aborted by user.");
                }
                System.out.print("    Overwriting...");
            }
            ZipOutputStream archive = new ZipOutputStream(new FileOutputStream(archiveFile));
            archive.setComment("Orient ODBMS backup archive \r\n" + "Created with odbbackup tool version " + oConstants.PRODUCT_VERSION + ".\r\n" + oConstants.PRODUCT_COPYRIGHTS + "\r\n\r\n" + "WARNING: MODIFING THIS ARCHIVE THE DATABASE CAN BE INCONSISTENT !!!");
            String[] files = new File(iDatabaseDir).list();
            String filePath;
            File inFile;
            FileInputStream inStream;
            byte[] buffer;
            for (int i = 0; i < files.length; ++i) {
                filePath = files[i];
                inFile = new File(iDatabaseDir + "/" + filePath);
                inStream = new FileInputStream(iDatabaseDir + "/" + filePath);
                buffer = new byte[(int) inFile.length()];
                inStream.read(buffer);
                if (monitor != null) monitor.notifyStatus("Archiving segment: " + filePath, i * 100 / files.length);
                archive.putNextEntry(new ZipEntry(filePath));
                archive.write(buffer);
            }
            archive.close();
        } catch (Exception e) {
            throw new DbBackupException("ERROR! Cannot backup the database.");
        }
    }

    private void loadArgs(String[] iArgs) {
        if (iArgs.length < 1 || iArgs[0].startsWith("-")) syntaxError("Missed <database> parameter");
        parameters.put("database", iArgs[0]);
        for (int i = 1; i < iArgs.length; ++i) {
            if (iArgs[i].startsWith("-a")) parameters.put("archive", iArgs[i].substring(2)); else if (iArgs[i].startsWith("-f")) parameters.put("force", "true");
        }
    }

    protected void printTitle() {
        System.out.println("Orient ODBMS oDbBackup v. " + oConstants.PRODUCT_VERSION + " - " + oConstants.PRODUCT_COPYRIGHTS + " (" + oConstants.PRODUCT_WWW + ")\n");
    }

    protected void printFormat() {
        System.out.println("Format: oDbBackup <database> [-a<archive>] [-f]");
        System.out.println(" where: <database> is the database alias");
        System.out.println("        -a         Specify archive name path. Default value is");
        System.out.println("                    the alias of database in current directory (optional)");
        System.err.println("        -f         Force overwrite of files (optional)");
    }

    private MonitorableTool monitor = null;

    private static final String SYSTEM_NAME = "System.erd";

    public static class DbBackupException extends Exception {

        public DbBackupException(String iMsg) {
            super(iMsg);
        }
    }
}
