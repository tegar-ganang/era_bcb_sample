package org.sulweb.infustore.config;

import java.sql.*;
import java.io.*;
import java.text.*;
import java.util.zip.*;
import org.sulweb.infumon.common.AutoIncrementID;
import org.sulweb.infumon.common.Bindings;
import org.sulweb.infumon.common.BindingsLog;
import org.sulweb.infumon.common.Commands;
import org.sulweb.infumon.common.Converters;
import org.sulweb.infumon.common.DBSchema;
import org.sulweb.infumon.common.DBVersions;
import org.sulweb.infumon.common.Drugs;
import org.sulweb.infumon.common.Hospital;
import org.sulweb.infumon.common.Hospitals;
import org.sulweb.infumon.common.Infusions;
import org.sulweb.infumon.common.InfusionsPumpsDrugs;
import org.sulweb.infumon.common.Medicians;
import org.sulweb.infumon.common.MediciansInfusions;
import org.sulweb.infumon.common.Models;
import org.sulweb.infumon.common.Patients;
import org.sulweb.infumon.common.Pumps;
import org.sulweb.infumon.common.Samples;
import org.sulweb.infumon.common.Vendors;

public class HospitalExporter {

    private DBSchema schema;

    private Connection conn;

    public HospitalExporter(DBSchema schema, Connection conn) {
        this.schema = schema;
        this.conn = conn;
    }

    public void doExport(File f, Hospital h, boolean setLocal) throws IOException, SQLException {
        String whereID = " WHERE ID=" + h.getId();
        String whereHospidalID = " WHERE HospitalID=" + h.getId();
        String[] vendors = { Vendors.name, "" };
        String[] models = { Models.name, "" };
        String[] commands = { Commands.name, "" };
        String[] hospitals = { Hospitals.name, whereID };
        String[] converters = { Converters.name, whereHospidalID };
        String[] pumps = { Pumps.name, whereHospidalID };
        String[][] tables = { hospitals, vendors, models, commands, converters, pumps };
        String sqlFileName = getFileName(f, 0, "SQL");
        File sqlFile = new File(sqlFileName);
        if (!sqlFile.createNewFile()) throw new IOException("Impossibile scrivere nella cartella specificata");
        boolean wasLocal = h.isLocal();
        try {
            if (!wasLocal && setLocal) {
                h.setLocal(true);
                schema.getHospitals().update(conn, h);
            }
            for (int i = 0; i < tables.length; i++) {
                String sql = "SELECT * INTO OUTFILE '" + getFileNameNormalizedForDB(f, i + 1, tables[i][0]) + "' FROM " + tables[i][0] + tables[i][1];
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.execute();
                ps.close();
            }
        } finally {
            if (!wasLocal && setLocal) {
                h.setLocal(false);
                schema.getHospitals().update(conn, h);
            }
        }
        conn.commit();
        zipdir(f, h);
    }

    public void fullBackup(File basedir) throws IOException, SQLException {
        String[] tables = { AutoIncrementID.tableName.toString(), Hospitals.name, DBVersions.name, Vendors.name, Models.name, Commands.name, Converters.name, Pumps.name, Bindings.name, BindingsLog.name, Drugs.name, Infusions.name, InfusionsPumpsDrugs.name, Medicians.name, MediciansInfusions.name, Patients.name, Samples.name };
        for (int i = 0; i < tables.length; i++) {
            String sql = "SELECT * INTO OUTFILE '" + getFileNameNormalizedForDB(basedir, i + 1, tables[i]) + "' FROM " + tables[i];
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.execute();
            ps.close();
        }
        conn.commit();
        java.util.Date now = new java.util.Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        zipdir(basedir, "fullbackup-" + sdf.format(now));
    }

    private String getFileName(File base, int index, String tableName) {
        return new File(base, ExporterFileNameFilter.zeroPadded.format(index) + tableName).getAbsolutePath();
    }

    private String getFileNameNormalizedForDB(File base, int index, String tableName) {
        return getFileName(base, index, tableName).replace('\\', '/');
    }

    private void zipdir(File base, Hospital h) throws IOException {
        zipdir(base, h.getName());
    }

    private void zipdir(File base, String zipname) throws IOException {
        FilenameFilter ff = new ExporterFileNameFilter();
        String[] files = base.list(ff);
        File zipfile = new File(base, zipname + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipfile));
        byte[] buf = new byte[10240];
        for (int i = 0; i < files.length; i++) {
            File f = new File(base, files[i]);
            FileInputStream fis = new FileInputStream(f);
            zos.putNextEntry(new ZipEntry(f.getName()));
            int len;
            while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
            zos.closeEntry();
            fis.close();
            f.delete();
        }
        zos.close();
    }
}
