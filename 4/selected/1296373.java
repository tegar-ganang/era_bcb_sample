package at.fhjoanneum.aim.sdi.project.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import at.fhjoanneum.aim.sdi.project.utilities.GlobalProperties;

public class BackupService {

    public static void backupAccessFile() throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date myDate = new Date();
        String dateext = "_" + df.format(myDate);
        String path = GlobalProperties.getFILEPATH();
        String ext = path.contains(".") ? path.substring(path.lastIndexOf("."), path.length()) : "";
        String split = path.contains("\\") ? "\\" : "/";
        String bck = GlobalProperties.getBackupPath() + path.substring(path.lastIndexOf(split), path.contains(".") ? path.indexOf(".") : path.length());
        FileUtils.copyFile(new File(path), new File(bck + dateext + ext));
    }
}
