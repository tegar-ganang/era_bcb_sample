package org.sulweb.infustore.config;

import org.sulweb.commandline.Options;
import org.sulweb.infumon.common.DBConnector;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.util.logging.*;
import javax.swing.*;
import org.sulweb.infumon.common.DBSchema;
import org.sulweb.infumon.common.Hospital;
import org.sulweb.infumon.common.Hospitals;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class DBInitializer {

    private DBConnector connector;

    private DBSchema schema;

    public DBInitializer(Options opt, JFrame owner) throws Exception {
        connector = new DBConnector(opt);
        connector.connect();
        schema = connector.interactiveReinitDB(null, owner);
        if (schema == null) throw new IllegalStateException("Database not updated. Exiting on user's request.");
    }

    public DBInitializer(DBConnector dbc, DBSchema schema) {
        this.connector = dbc;
        this.schema = schema;
    }

    public Hospital getLocalHospital() throws Exception {
        Hospital result = null;
        Hospitals dbhosps = schema.getHospitals();
        Hospital first = null;
        Iterator iter = dbhosps.getRecords().iterator();
        while (iter.hasNext() && result == null) {
            Hospital temp = (Hospital) iter.next();
            if (first == null) first = temp;
            if (temp.isLocal()) result = temp;
        }
        if (result == null) result = first;
        return result;
    }

    public void doImport(File f, boolean checkHosp) throws Exception {
        connector.getConnection().setAutoCommit(false);
        File base = f.getParentFile();
        ZipInputStream in = new ZipInputStream(new FileInputStream(f));
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            String outFileName = entry.getName();
            File outFile = new File(base, outFileName);
            OutputStream out = new FileOutputStream(outFile, false);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
        }
        in.close();
        importDirectory(base, checkHosp);
        connector.getConnection().commit();
    }

    private void importDirectory(File dir, boolean checkHosp) throws Exception {
        FilenameFilter ff = new ExporterFileNameFilter();
        String[] filesToImport = dir.list(ff);
        Arrays.sort(filesToImport);
        for (int i = 0; i < filesToImport.length; i++) {
            String fileType = getFileType(filesToImport[i]);
            File f = new File(dir, filesToImport[i]);
            if (fileType.equals("SQL")) execSQLFile(f); else importDataFile(f, fileType, checkHosp);
            f.delete();
        }
    }

    private void importDataFile(File f, String table, boolean checkHosp) throws Exception {
        if (checkHosp && table.equals(Hospitals.name)) {
            Hospital h = getLocalHospital();
            if (h != null) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = br.readLine();
                br.close();
                if (line != null && line.toLowerCase().indexOf("" + h.getId()) < 0) throw new InvalidObjectException("Non posso importare pi� di un ospedale");
            }
        }
        String sql = "LOAD DATA CONCURRENT LOCAL INFILE '" + f.getAbsolutePath().replace('\\', '/') + "' REPLACE INTO TABLE " + table;
        PreparedStatement ps = connector.getConnection().prepareStatement(sql);
        ps.executeUpdate();
        ps.close();
        connector.getConnection().commit();
    }

    private String getFileType(String filename) {
        return filename.substring(ExporterFileNameFilter.strFormat.length()).toUpperCase();
    }

    private void execSQLFile(File f) throws Exception {
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null) {
            PreparedStatement ps = connector.getConnection().prepareStatement(line);
            ps.execute();
            ps.close();
        }
        connector.getConnection().commit();
    }
}
