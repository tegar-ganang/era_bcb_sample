package server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import common.CampaignData;
import server.campaign.CampaignMain;

public class AutomaticBackup extends Thread {

    String dateTime = "";

    String factionZipFileName = "";

    String planetZipFileName = "";

    String playerZipFileName = "";

    String dataZipFileName = "";

    String dateTimeFormat = "yyyy.MM.dd.HH.mm";

    FileOutputStream out;

    ZipOutputStream zipFile;

    long time;

    public AutomaticBackup(long time) {
        super("Automatic Backup");
        this.time = time;
    }

    public void run() {
        if (this.time < 1) return;
        long backupHours = Long.parseLong(CampaignMain.cm.getConfig("AutomaticBackupHours")) * 3600000;
        long lastBackup = Long.parseLong(CampaignMain.cm.getConfig("LastAutomatedBackup"));
        if (lastBackup > time - backupHours) return;
        CampaignData.mwlog.mainLog("Archiving Started at " + time);
        CampaignMain.cm.setArchiving(true);
        if (CampaignMain.cm.isUsingMySQL()) {
            CampaignMain.cm.MySQL.backupDB(time);
        }
        SimpleDateFormat sDF = new SimpleDateFormat(dateTimeFormat);
        Date date = new Date(time);
        File folder = new File("./campaign/backup");
        if (!folder.exists()) folder.mkdir();
        dateTime = sDF.format(date);
        factionZipFileName = "./campaign/backup/factions" + dateTime + ".zip";
        planetZipFileName = "./campaign/backup/planets" + dateTime + ".zip";
        playerZipFileName = "./campaign/backup/players" + dateTime + ".zip";
        dataZipFileName = "./campaign/backup/data" + dateTime + ".zip";
        if (!CampaignMain.cm.isUsingMySQL()) {
            try {
                out = new FileOutputStream(factionZipFileName);
                zipFile = new ZipOutputStream(out);
                zipBackupFactions();
                zipFile.close();
            } catch (Exception ex) {
                CampaignData.mwlog.errLog("Unable to create factions zip file");
                CampaignData.mwlog.errLog(ex);
            }
            try {
                out = new FileOutputStream(planetZipFileName);
                zipFile = new ZipOutputStream(out);
                zipBackupPlanets();
                zipFile.close();
            } catch (Exception ex) {
                CampaignData.mwlog.errLog("Unable to create planets zip file");
                CampaignData.mwlog.errLog(ex);
            }
            try {
                out = new FileOutputStream(playerZipFileName);
                zipFile = new ZipOutputStream(out);
                zipBackupPlayers();
                zipFile.close();
            } catch (Exception ex) {
                CampaignData.mwlog.errLog("Unable to create player zip file");
                CampaignData.mwlog.errLog(ex);
            }
        }
        try {
            out = new FileOutputStream(dataZipFileName);
            zipFile = new ZipOutputStream(out);
            zipBackupData();
            zipFile.close();
        } catch (Exception ex) {
            CampaignData.mwlog.errLog("Unable to create data zip file");
            CampaignData.mwlog.errLog(ex);
        }
        CampaignMain.cm.getConfig().setProperty("LastAutomatedBackup", Long.toString(time));
        CampaignMain.dso.createConfig();
        CampaignMain.cm.setArchiving(false);
        CampaignData.mwlog.mainLog("Archiving Ended.");
    }

    /**
     * @author Torren (Jason Tighe)
     *
     * Backup the filename into a nice zip file.
     * 
     */
    public void zipBackupFactions() {
        File folder = new File("./campaign/factions");
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            try {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getName());
                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            } catch (FileNotFoundException fnfe) {
                CampaignData.mwlog.errLog("Unable to backup faction file: " + files[i].getName());
            } catch (Exception ex) {
                CampaignData.mwlog.errLog("Unable to backup faction files");
                CampaignData.mwlog.errLog(ex);
            }
        }
    }

    public void zipBackupPlanets() {
        File folder = new File("./campaign/planets");
        File[] files = folder.listFiles();
        try {
            for (int i = 0; i < files.length; i++) {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getName());
                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            CampaignData.mwlog.errLog("Unable to backup planet files");
            CampaignData.mwlog.errLog(ex);
        }
    }

    public void zipBackupPlayers() {
        File folder = new File("./campaign/players");
        File[] files = folder.listFiles();
        try {
            for (int i = 0; i < files.length; i++) {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getName());
                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            CampaignData.mwlog.errLog("Unable to backup player files");
            CampaignData.mwlog.errLog(ex);
        }
    }

    public void zipBackupData() {
        zipBackupData("./data");
    }

    public void zipBackupData(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        ZipEntry entry;
        try {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    entry = new ZipEntry(files[i].getPath() + "/");
                    zipFile.putNextEntry(entry);
                    zipBackupData(files[i].getPath());
                    continue;
                }
                FileInputStream in = new FileInputStream(files[i]);
                entry = new ZipEntry(path + "/" + files[i].getName());
                zipFile.putNextEntry(entry);
                int c;
                while ((c = in.read()) != -1) zipFile.write(c);
                zipFile.closeEntry();
                in.close();
            }
        } catch (Exception ex) {
            CampaignData.mwlog.errLog("Unable to backup server data files: " + path);
            CampaignData.mwlog.errLog(ex);
        }
    }
}
