package org.openscience.nmrshiftdb.daemons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletConfig;
import org.apache.jetspeed.daemon.Daemon;
import org.apache.turbine.om.NumberKey;
import org.apache.turbine.services.db.TurbineDB;
import org.apache.turbine.util.Log;
import org.apache.turbine.util.db.pool.DBConnection;
import org.openscience.nmrshiftdb.modules.actions.portlets.OrderAction;
import org.openscience.nmrshiftdb.om.DBRawFile;
import org.openscience.nmrshiftdb.om.DBSample;
import org.openscience.nmrshiftdb.om.DBSamplePeer;
import org.openscience.nmrshiftdb.util.GeneralUtils;

public class AssignRobotDaemon extends AbstractDaemonWithServletConfig {

    public void run(ServletConfig servcon) throws Exception {
        this.setResult(Daemon.RESULT_PROCESSING);
        Log.info("running robot stuff");
        DBConnection dbconn = null;
        String query = "select distinct SAMPLE.SAMPLE_ID, USERS_ID from SAMPLE left join RAW_FILE using (SAMPLE_ID) where URL is null and FINISHED=\"false\" and (PROCESS=\"" + OrderAction.ROBOT + "\" or PROCESS=\"" + OrderAction.SELF + "\")";
        try {
            dbconn = TurbineDB.getConnection();
            Statement stmt = dbconn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String sampleid = rs.getString(1);
                String usersid = rs.getString(2);
                File zipfile = new File(GeneralUtils.getNmrshiftdbProperty("compressedfiledirectory", servcon) + "/" + usersid + ".zip");
                FileOutputStream baos = new FileOutputStream(zipfile);
                ZipOutputStream zipout = new ZipOutputStream(baos);
                File rawfiledir = new File(GeneralUtils.getNmrshiftdbProperty("rawfiledirectoryrobot", servcon));
                StringBuffer attachedfiles = new StringBuffer();
                searchfiles(rawfiledir, usersid, zipout, attachedfiles, new File(GeneralUtils.getNmrshiftdbProperty("compressedfiledirectory", servcon)));
                if (!attachedfiles.toString().equals("")) {
                    zipout.close();
                    DBRawFile rawfile = new DBRawFile();
                    rawfile.setSampleId(sampleid);
                    rawfile.setUrl(GeneralUtils.getNmrshiftdbProperty("urlcompressedfiles", servcon) + "/" + usersid + ".zip");
                    rawfile.setContainedFiles(attachedfiles.toString());
                    rawfile.setAssigned("false");
                    rawfile.save();
                    DBSample sample = DBSamplePeer.retrieveByPK(new NumberKey(sampleid));
                    sample.setFinished("true");
                    sample.save();
                }
            }
            dbconn.close();
            this.setResult(Daemon.RESULT_SUCCESS);
        } catch (Exception ex) {
            dbconn.close();
            GeneralUtils.logError(ex, "robot daemon", null, true);
        }
    }

    private void searchfiles(File tolookin, String usersid, ZipOutputStream zipoutputstream, StringBuffer attachedfiles, File dirtoignore) throws IOException {
        File[] filesindir = tolookin.listFiles();
        if (filesindir != null) {
            for (int i = 0; i < filesindir.length; i++) {
                if (filesindir[i].isDirectory() && filesindir[i].getName().indexOf(usersid) != 0 && !filesindir[i].getName().equals(".") && !filesindir[i].getName().equals("..") && !filesindir[i].equals(dirtoignore)) {
                    searchfiles(filesindir[i], usersid, zipoutputstream, attachedfiles, dirtoignore);
                } else if (filesindir[i].isDirectory() && filesindir[i].getName().indexOf(usersid) == 0) {
                    if ((System.currentTimeMillis() - filesindir[i].lastModified()) > 300000) addfiles(filesindir[i], zipoutputstream, attachedfiles);
                } else {
                    if (filesindir[i].getName().indexOf(usersid) == 0) {
                        zipoutputstream.putNextEntry(new ZipEntry(filesindir[i].getName()));
                        FileInputStream fis = new FileInputStream(filesindir[i]);
                        int read = 0;
                        while ((read = fis.read()) != -1) {
                            zipoutputstream.write(read);
                        }
                        attachedfiles.append(filesindir[i] + "; ");
                        zipoutputstream.closeEntry();
                    }
                }
            }
        }
    }

    private void addfiles(File toadd, ZipOutputStream zipoutputstream, StringBuffer attachedfiles) throws IOException {
        File[] filesindir = toadd.listFiles();
        if (filesindir != null) {
            for (int i = 0; i < filesindir.length; i++) {
                if (filesindir[i].isDirectory() && !filesindir[i].getName().equals(".") && !filesindir[i].getName().equals("..")) {
                    addfiles(filesindir[i], zipoutputstream, attachedfiles);
                } else if (filesindir[i].isFile()) {
                    zipoutputstream.putNextEntry(new ZipEntry(toadd + "/" + filesindir[i].getName()));
                    FileInputStream fis = new FileInputStream(filesindir[i]);
                    int read = 0;
                    while ((read = fis.read()) != -1) {
                        zipoutputstream.write(read);
                    }
                    attachedfiles.append(filesindir[i] + "; ");
                    zipoutputstream.closeEntry();
                }
            }
        }
    }
}
