package com.indragunawan.restobiz.app;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import oracle.toplink.essentials.config.TopLinkProperties;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jdesktop.application.LocalStorage;

/**
 *
 * @author igoens
 */
public class GeneralConfig {

    private EntityManager configEntity;

    private Query configQuery;

    private String server, port, catalog, user, password, url;

    private static String DB_CONFIG = "restobiz.configuration";

    private static String RP_CONFIG = "restobiz.reports.location";

    private static String SS_LOGUSR = "login.session";

    private static String DB_DRIVER = "com.mysql.jdbc.Driver";

    private static String ENC_PASSW = "rq0e1psa9t2lob3j6i2z";

    private SimpleDateFormat dateSQL = new SimpleDateFormat("yyyy-MM-dd");

    private SimpleDateFormat dateDisplay = new SimpleDateFormat("dd/MM/yyyy");

    private NumberFormat invoiceDisplay = new DecimalFormat("0000");

    /** Create new GeneralConfig */
    public GeneralConfig() {
        if (isDbConnected()) {
            configEntity = Persistence.createEntityManagerFactory("RestobizPU", getPersistanceDbProperties()).createEntityManager();
        }
    }

    public Boolean confirmRestricted(Integer Module) {
        Boolean isRestricted = false;
        isRestricted = isRestricted(Module);
        if (isRestricted) {
            JOptionPane.showMessageDialog(null, "Anda tidak memiliki akses ke menu ini");
        }
        return isRestricted;
    }

    public Boolean isRestricted(Integer Module) {
        List skemaList;
        Boolean result = false;
        if (isDbConnected()) {
            configQuery = configEntity.createNativeQuery("SELECT * FROM skema s WHERE s.modul = #module AND s.akses = #group").setParameter("module", Module).setParameter("group", getUserGroup());
            skemaList = configQuery.getResultList();
            result = !skemaList.isEmpty();
        }
        return result;
    }

    public Boolean isVATItem(Integer Menu) {
        Boolean Nilai = true;
        configQuery = configEntity.createNativeQuery("SELECT m.pajak FROM menu m WHERE m.menu = #menu").setParameter("menu", invoiceDisplay.format(Menu));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Boolean.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getSubTotal(int Nomor, Date Tanggal) {
        Double Nilai = 0.0;
        configQuery = configEntity.createNativeQuery("SELECT SUM(CASE WHEN (dt.berat>0) THEN (dt.harga*dt.berat) ELSE (dt.harga*dt.jumlah) END) AS total FROM detiltransaksi dt WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getTotalBayar(int Nomor, Date Tanggal) {
        Double Nilai = 0.0;
        configQuery = configEntity.createNativeQuery("SELECT SUM(p.jumlah) AS TOTAL FROM pembayaran p WHERE p.nomor = #nomor AND p.tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getTotalDiscount(int Nomor, Date Tanggal) {
        Double Nilai = 0.0;
        configQuery = configEntity.createNativeQuery("SELECT SUM(CASE WHEN (dt.berat>0) THEN (dt.diskon*dt.berat) ELSE (dt.diskon*dt.jumlah) END) AS total FROM detiltransaksi dt WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getTotalPajak(int Nomor, Date Tanggal) {
        Double Nilai = 0.0;
        configQuery = configEntity.createNativeQuery("SELECT SUM(CASE WHEN (dt.berat>0) THEN (dt.pajak*dt.berat) ELSE (dt.pajak*dt.jumlah) END) AS total FROM detiltransaksi dt WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getTotalTransaksi(int Nomor, Date Tanggal) {
        Double Nilai = 0.0;
        configQuery = configEntity.createNativeQuery("SELECT SUM(CASE WHEN (dt.berat>0) THEN ((dt.harga-dt.diskon+dt.pajak)*dt.berat) ELSE ((dt.harga-dt.diskon+dt.pajak)*dt.jumlah) END) AS total FROM detiltransaksi dt WHERE dt.nomor = #nomor AND dt.tanggal = #tanggal").setParameter("nomor", invoiceDisplay.format(Nomor)).setParameter("tanggal", dateSQL.format(Tanggal));
        Vector result = (Vector) configQuery.getSingleResult();
        if (result.get(0) != null) {
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getTotalSisa(int Nomor, Date Tanggal) {
        Double Nilai;
        Nilai = getTotalTransaksi(Nomor, Tanggal) - getTotalBayar(Nomor, Tanggal);
        return Nilai;
    }

    @SuppressWarnings("unchecked")
    public Map getCompanyReportHeader() {
        Map mp = new HashMap();
        mp.put("SUBREPORT_DIR", getReportLocation());
        mp.put("RESTOBIZ_BUSINESS", getJenisUsaha());
        mp.put("RESTOBIZ_COMPANY", getNamaPerusahaan());
        mp.put("RESTOBIZ_ADDRESS", getAlamatUsaha());
        mp.put("RESTOBIZ_PHONE", getTelepon());
        return mp;
    }

    public Boolean isReportExists(String Report) {
        Boolean result = false;
        String reportFile = getReportLocation() + Report;
        File fileReport = new File(reportFile);
        if (!fileReport.exists()) {
            JOptionPane.showMessageDialog(null, "Report tidak ditemukan atau directory belum diset");
        } else {
            result = fileReport.exists();
        }
        return result;
    }

    public Boolean previewReport(Dialog Owner, String Report, Map Parameters, String Title, Boolean Modal) {
        Boolean result = false;
        if (isReportExists(Report)) {
            try {
                JasperPrint jp = JasperFillManager.fillReport(getReportLocation() + Report, Parameters, getJDBCConnection());
                JRViewer jrv = new JRViewer(jp);
                JDialog printPreviewDialog = new JDialog(Owner, Title, Modal);
                printPreviewDialog.getContentPane().add(jrv);
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                printPreviewDialog.setSize(screenSize.width, screenSize.height);
                printPreviewDialog.setLocation(0, 0);
                printPreviewDialog.setVisible(true);
                result = true;
            } catch (JRException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    public Boolean isFirstTime() {
        Boolean result = true;
        if (isDbConnected()) {
            Connection con = getJDBCConnection();
            ResultSet rs;
            try {
                String[] schemeType = { "TABLE" };
                rs = con.getMetaData().getTables(catalog, null, null, schemeType);
                rs.last();
                if (rs.getRow() >= 1) {
                    result = false;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            result = false;
        }
        return result;
    }

    public Boolean addRestrictedPolicy(Integer Module, String Group) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("INSERT INTO skema VALUES (#module, #group, false)").setParameter("module", Module).setParameter("group", Group);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean deleteRestrictedPolicy(Integer Module, String Group) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("DELETE FROM skema WHERE modul = #module AND akses = #group").setParameter("module", Module).setParameter("group", Group);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean addJenisBayar(String Jenis, Boolean Negasi) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("INSERT INTO jenisbayar VALUES (#jenis, #negasi)").setParameter("jenis", Jenis).setParameter("negasi", Negasi);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean deleteJenisBayar(String Jenis) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("DELETE FROM jenisbayar WHERE jenis = #jenis").setParameter("jenis", Jenis);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean addOperator(String NamaUser, String Password, String NamaAsli, String Akses) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("INSERT INTO operator VALUES (#namauser, MD5(#password), #namaasli, #akses)").setParameter("namauser", NamaUser).setParameter("password", Password).setParameter("namaasli", NamaAsli).setParameter("akses", Akses);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean addUserGroup(String Group) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("INSERT INTO hakakses VALUES (#akses, false)").setParameter("akses", Group);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean deleteUserGroup(String Group) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("DELETE FROM hakakses WHERE akses = #akses").setParameter("akses", Group);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Boolean deleteOperator(String NamaUser) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("DELETE FROM operator WHERE namauser = #namauser").setParameter("namauser", NamaUser);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public List getSystemGroupList() {
        List groupList = null;
        if (isDbConnected()) {
            groupList = configEntity.createQuery("SELECT h.akses FROM HakAkses h WHERE h.sistem = true ORDER BY h.akses").getResultList();
        }
        return groupList;
    }

    public String getUserGroup() {
        String groupUser = "";
        if (isDbConnected()) {
            groupUser = configEntity.createQuery("SELECT h.akses FROM Operator o JOIN o.akses h WHERE o.namauser = :user").setParameter("user", getLoggedUser()).getSingleResult().toString();
        }
        return groupUser;
    }

    public List getUserGroupList() {
        List userGroupList = null;
        if (isDbConnected()) {
            userGroupList = configEntity.createQuery("SELECT h.akses FROM HakAkses h WHERE h.sistem = false ORDER BY h.akses").getResultList();
        }
        return userGroupList;
    }

    public List getOperatorList() {
        List operatorList = null;
        if (isDbConnected()) {
            operatorList = configEntity.createQuery("SELECT o.namauser, o.namaasli, h.akses FROM Operator o JOIN o.akses h").getResultList();
        }
        return operatorList;
    }

    public List getAksesList() {
        List hakAksesList = null;
        if (isDbConnected()) {
            hakAksesList = configEntity.createQuery("SELECT h.akses FROM HakAkses h").getResultList();
        }
        return hakAksesList;
    }

    public List getSkemaList() {
        List skemaList = null;
        if (isDbConnected()) {
            skemaList = configEntity.createQuery("SELECT s.skemaPK.modul, s.skemaPK.akses, s.negasi FROM Skema s ORDER BY s.skemaPK.modul").getResultList();
        }
        return skemaList;
    }

    public Double getJumlahItem(Date Tanggal, Integer Nomor, Integer Menu) {
        Double Nilai = 0.0;
        if (isDbConnected()) {
            configQuery = configEntity.createNativeQuery("SELECT dt.jumlah FROM detiltransaksi dt WHERE dt.tanggal = #tanggal AND dt.nomor = #nomor AND dt.menu = #menu").setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("nomor", Nomor).setParameter("menu", Menu);
            Vector result = (Vector) configQuery.getSingleResult();
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public Double getBeratItem(Date Tanggal, Integer Nomor, Integer Menu) {
        Double Nilai = 0.0;
        if (isDbConnected()) {
            configQuery = configEntity.createNativeQuery("SELECT dt.berat FROM detiltransaksi dt WHERE dt.tanggal = #tanggal AND dt.nomor = #nomor AND dt.menu = #menu").setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("nomor", Nomor).setParameter("menu", Menu);
            Vector result = (Vector) configQuery.getSingleResult();
            Nilai = Double.valueOf(String.valueOf(result.get(0)));
        }
        return Nilai;
    }

    public String getLoggedUser() {
        String loggedUser = null;
        Properties prop = new Properties();
        LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
        File configFile = new File(ls.getDirectory().getPath() + File.separatorChar + SS_LOGUSR);
        if (configFile.exists()) {
            try {
                prop.load(ls.openInputFile(SS_LOGUSR));
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        loggedUser = prop.getProperty("User", "");
        loggedUser = textDecrypt(loggedUser);
        return loggedUser;
    }

    public Integer getDatabaseVersion() {
        Integer Version = 0;
        String DBVersion = getConfigValue("DBVersion");
        if (!DBVersion.isEmpty()) {
            Version = Integer.valueOf(DBVersion);
        }
        return Version;
    }

    public void setDatabaseVersion(Integer Version) {
        setConfigValue("DBVersion", String.valueOf(Version));
    }

    public String getNamaPerusahaan() {
        return getConfigValue("Company");
    }

    public void setNamaPerusahaan(String Nama) {
        setConfigValue("Company", Nama);
    }

    public String getJenisUsaha() {
        return getConfigValue("Business");
    }

    public void setJenisUsaha(String Jenis) {
        setConfigValue("Business", Jenis);
    }

    public String getAlamatUsaha() {
        return getConfigValue("Address");
    }

    public void setAlamatUsaha(String Alamat) {
        setConfigValue("Address", Alamat);
    }

    public String getTelepon() {
        return getConfigValue("Phone");
    }

    public void setTelepon(String Telepon) {
        setConfigValue("Phone", Telepon);
    }

    public String getPesanPromosi() {
        return getConfigValue("Promo");
    }

    public void setPesanPromosi(String Pesan) {
        setConfigValue("Promo", Pesan);
    }

    public Boolean setUserPassword(String NamaUser, String Password) {
        Boolean result = false;
        try {
            configEntity.getTransaction().begin();
            configQuery = configEntity.createNativeQuery("UPDATE operator SET password = MD5(#password) WHERE namauser = #namauser").setParameter("password", Password).setParameter("namauser", NamaUser);
            configQuery.executeUpdate();
            configEntity.getTransaction().commit();
            result = true;
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public String getLoggedUserName() {
        String loggedUser = null;
        Properties prop = new Properties();
        LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
        File configFile = new File(ls.getDirectory().getPath() + File.separatorChar + SS_LOGUSR);
        if (configFile.exists()) {
            try {
                prop.load(ls.openInputFile(SS_LOGUSR));
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        loggedUser = prop.getProperty("Nama", "");
        loggedUser = textDecrypt(loggedUser);
        return loggedUser;
    }

    public void setLoggedUser(String User) {
        Properties prop = new Properties();
        String Nama = "";
        if (!User.equals("")) {
            configQuery = configEntity.createNativeQuery("SELECT o.namaasli FROM operator o WHERE o.namauser = #namauser").setParameter("namauser", User);
            Vector result = (Vector) configQuery.getSingleResult();
            Nama = String.valueOf(result.get(0));
        }
        User = textEncrypt(User);
        Nama = textEncrypt(Nama);
        prop.setProperty("User", User);
        prop.setProperty("Nama", Nama);
        try {
            LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
            prop.store(ls.openOutputFile(SS_LOGUSR), "Restobiz Login Session\nDo not modify this file manually");
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clearLoggedUser() {
        setLoggedUser("");
    }

    public Boolean isUserLogged() {
        Boolean result = false;
        if (getLoggedUser().length() > 0) {
            result = true;
        }
        return result;
    }

    public String textEncrypt(String Text) {
        String result = "";
        try {
            BasicTextEncryptor bte = new BasicTextEncryptor();
            bte.setPassword(ENC_PASSW);
            result = bte.encrypt(Text);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public String textDecrypt(String Text) {
        String result = "";
        try {
            BasicTextEncryptor bte = new BasicTextEncryptor();
            bte.setPassword(ENC_PASSW);
            result = bte.decrypt(Text);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Date getLastTransaksi() throws ParseException {
        String lastDate;
        Date tanggal = new Date();
        lastDate = getConfigValue("LastTrans");
        if (lastDate.isEmpty()) {
            setLastTransaksi(tanggal);
            return tanggal;
        } else {
            return dateDisplay.parse(getConfigValue("LastTrans"));
        }
    }

    public void setLastTransaksi(Date LastDate) {
        setConfigValue("LastTrans", dateDisplay.format(LastDate));
    }

    public Date getLastCash() throws ParseException {
        String lastDate;
        Date tanggal = new Date();
        lastDate = getConfigValue("LastCash");
        if (lastDate.isEmpty()) {
            setLastCash(tanggal);
            return tanggal;
        } else {
            return dateDisplay.parse(getConfigValue("LastCash"));
        }
    }

    public void setLastCash(Date LastDate) {
        setConfigValue("LastCash", dateDisplay.format(LastDate));
    }

    public Date getLastReceipt() throws ParseException {
        String lastDate;
        Date tanggal = new Date();
        lastDate = getConfigValue("LastReceipt");
        if (lastDate.isEmpty()) {
            setLastReceipt(tanggal);
            return tanggal;
        } else {
            return dateDisplay.parse(getConfigValue("LastReceipt"));
        }
    }

    public void setLastReceipt(Date LastDate) {
        setConfigValue("LastReceipt", dateDisplay.format(LastDate));
    }

    public int getNomorTransaksi() {
        String nomor = "";
        String Akhir = "";
        String Tanggal = "";
        try {
            Akhir = dateDisplay.format(getLastTransaksi());
            Tanggal = dateDisplay.format(new Date());
            if (!Akhir.equals(Tanggal)) {
                setLastTransaksi(dateDisplay.parse(Tanggal));
                setConfigValue("Invoice", "1");
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        nomor = getConfigValue("Invoice");
        if (nomor.isEmpty()) {
            return 1;
        } else {
            return Integer.valueOf(nomor);
        }
    }

    public void incNomorTransaksi() {
        try {
            int n;
            n = getNomorTransaksi() + 1;
            setConfigValue("Invoice", String.valueOf(n));
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getNomorKas() {
        String nomor = "";
        String Akhir = "";
        String Tanggal = "";
        try {
            Akhir = dateDisplay.format(getLastCash());
            Tanggal = dateDisplay.format(new Date());
            if (!Akhir.equals(Tanggal)) {
                setLastCash(dateDisplay.parse(Tanggal));
                setConfigValue("Cash", "1");
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        nomor = getConfigValue("Cash");
        if (nomor.isEmpty()) {
            return 1;
        } else {
            return Integer.valueOf(nomor);
        }
    }

    public void incNomorKas() {
        try {
            int n;
            n = getNomorKas() + 1;
            setConfigValue("Cash", String.valueOf(n));
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getCurrentShift() {
        String nomor;
        nomor = getConfigValue("Shift");
        if (nomor.isEmpty()) {
            return 1;
        } else {
            return Integer.valueOf(nomor);
        }
    }

    public void incCurrentShift() {
        try {
            int n = getCurrentShift();
            if (n < getMaxShift()) {
                n = n + 1;
            } else {
                n = 1;
            }
            setConfigValue("Shift", String.valueOf(n));
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getMaxShift() {
        String maxShift;
        int defaultMax = 3;
        maxShift = getConfigValue("MaxShift");
        if (maxShift.isEmpty()) {
            setConfigValue("MaxShift", String.valueOf(defaultMax));
            return defaultMax;
        } else {
            return Integer.valueOf(maxShift);
        }
    }

    public void setMaxShift(Integer maxShift) {
        setConfigValue("MaxShift", String.valueOf(maxShift));
    }

    public void resetNoTransaksi() {
        setConfigValue("Invoice", "1");
    }

    public int getNomorKuitansi() {
        String nomor = "";
        String Akhir = "";
        String Tanggal = "";
        try {
            Akhir = dateDisplay.format(getLastReceipt());
            Tanggal = dateDisplay.format(new Date());
            if (!Akhir.equals(Tanggal)) {
                setLastReceipt(dateDisplay.parse(Tanggal));
                setConfigValue("Receipt", "1");
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        nomor = getConfigValue("Receipt");
        if (nomor.isEmpty()) {
            return 1;
        } else {
            return Integer.valueOf(nomor);
        }
    }

    public void incNomorKuitansi() {
        try {
            int n;
            n = getNomorKuitansi() + 1;
            setConfigValue("Receipt", String.valueOf(n));
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void resetNoKuitansi() {
        setConfigValue("Receipt", "1");
    }

    public List getJenisBayar() {
        List bayarList = null;
        if (isDbConnected()) {
            bayarList = configEntity.createNativeQuery("SELECT b.jenis FROM jenisbayar b ORDER BY b.negasi").getResultList();
        }
        return bayarList;
    }

    public List getKelompokKas() {
        List kasList = null;
        if (isDbConnected()) {
            kasList = configEntity.createNativeQuery("SELECT k.kelompok FROM kas k GROUP BY k.kelompok ORDER BY k.kelompok").getResultList();
        }
        return kasList;
    }

    public List getJenisBayarList() {
        List bayarList = null;
        if (isDbConnected()) {
            bayarList = configEntity.createQuery("SELECT b.jenis, b.negasi FROM JenisBayar b ORDER BY b.jenis").getResultList();
        }
        return bayarList;
    }

    public Boolean isNegativePay(String Jenis) {
        String negatifPay = null;
        if (isDbConnected()) {
            Vector result = (Vector) configEntity.createNativeQuery("SELECT j.negasi FROM jenisbayar j WHERE j.jenis = #jenis").setParameter("jenis", Jenis).getSingleResult();
            negatifPay = String.valueOf(result.get(0));
        }
        return Boolean.parseBoolean(negatifPay);
    }

    public List getKelompokMenu() {
        List kelompokList = null;
        if (isDbConnected()) {
            kelompokList = configEntity.createQuery("SELECT m.kelompok FROM Menu m GROUP BY m.kelompok ORDER BY m.kelompok").getResultList();
        }
        return kelompokList;
    }

    public List getMenu() {
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.nama FROM Menu m GROUP BY m.nama ORDER BY m.nama").getResultList();
        }
        return menuList;
    }

    public List getMenuList() {
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.menu, m.kelompok, m.nama, m.harga, m.satuan, m.pajak, m.aktif FROM Menu m ORDER BY m.kelompok, m.nama").getResultList();
        }
        return menuList;
    }

    public List getMenuNameList() {
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.nama FROM Menu m WHERE m.aktif = true ORDER BY m.nama").getResultList();
        }
        return menuList;
    }

    public List getMenuByKelompok(String Kelompok) {
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.nama FROM Menu m WHERE m.kelompok = :kelompok AND m.aktif = true GROUP BY m.nama ORDER BY m.nama").setParameter("kelompok", Kelompok).getResultList();
        }
        return menuList;
    }

    public Boolean isMenuNameExists(String Nama) {
        Boolean result = false;
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m FROM Menu m WHERE m.nama = :nama AND m.aktif = true").setParameter("nama", Nama).getResultList();
            if (menuList.size() == 1) {
                result = true;
            }
        }
        return result;
    }

    public Object getMenuByNama(String Nama) {
        Object menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.menu FROM Menu m WHERE m.nama = :nama").setParameter("nama", Nama).getSingleResult();
        }
        return menuList;
    }

    public Object getHargaByNama(String Nama) {
        Object menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.harga FROM Menu m WHERE m.nama = :nama").setParameter("nama", Nama).getSingleResult();
        }
        return menuList;
    }

    public List getKelompokMenuByKeyword(String Keyword) {
        List menuList = null;
        if (isDbConnected()) {
            menuList = configEntity.createQuery("SELECT m.kelompok FROM Menu m WHERE m.nama LIKE :keyword GROUP BY m.kelompok ORDER BY m.kelompok").setParameter("keyword", "'%" + Keyword + "%'").getResultList();
        }
        return menuList;
    }

    public Double getGlobalDiscount() {
        String Discount;
        Discount = getConfigValue("Discount");
        if (Discount.isEmpty()) {
            return 0.0;
        } else {
            return Double.valueOf(Discount);
        }
    }

    public void setGlobalDiscount(Double Discount) {
        String StoredDiscount;
        StoredDiscount = String.valueOf(Discount);
        setConfigValue("Discount", StoredDiscount);
    }

    public List getCustomer() {
        List customerList = null;
        if (isDbConnected()) {
            customerList = configEntity.createQuery("SELECT t.customer FROM Transaksi t GROUP BY t.customer ORDER BY t.customer").getResultList();
        }
        return customerList;
    }

    public String getDbConfigFile() {
        return GeneralConfig.DB_CONFIG;
    }

    public String getConfigValue(String Nama) {
        String configList = "";
        configQuery = configEntity.createQuery("SELECT k.nilai FROM Konfigurasi k WHERE k.nama = :nama").setParameter("nama", Nama);
        if (!configQuery.getResultList().isEmpty()) {
            configList = configQuery.getSingleResult().toString();
        }
        return configList;
    }

    public void setConfigValue(String Nama, String Nilai) {
        configQuery = configEntity.createQuery("SELECT k FROM Konfigurasi k WHERE k.nama = :nama").setParameter("nama", Nama);
        if (configQuery.getResultList().isEmpty()) {
            try {
                configEntity.getTransaction().begin();
                configQuery = configEntity.createNativeQuery("INSERT INTO konfigurasi VALUES (#nama, #nilai)").setParameter("nama", Nama).setParameter("nilai", Nilai);
                configQuery.executeUpdate();
                configEntity.getTransaction().commit();
            } catch (Exception ex) {
                configEntity.getTransaction().rollback();
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                configEntity.getTransaction().begin();
                configQuery = configEntity.createNativeQuery("UPDATE konfigurasi k SET k.nilai = #nilai WHERE k.nama = #nama").setParameter("nama", Nama).setParameter("nilai", Nilai);
                configQuery.executeUpdate();
                configEntity.getTransaction().commit();
            } catch (Exception ex) {
                configEntity.getTransaction().rollback();
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Double getVatValue() {
        String Pajak;
        Pajak = getConfigValue("VAT");
        if (Pajak.isEmpty()) {
            return 0.0;
        } else {
            return Double.valueOf(Pajak);
        }
    }

    public void setVatValue(Double VAT) {
        String StoredVAT;
        StoredVAT = String.valueOf(VAT);
        setConfigValue("VAT", StoredVAT);
    }

    @SuppressWarnings("unchecked")
    public Map getPersistanceDbProperties() {
        Map mp = new HashMap();
        try {
            mp.put(TopLinkProperties.JDBC_DRIVER, DB_DRIVER);
            mp.put(TopLinkProperties.JDBC_URL, url);
            mp.put(TopLinkProperties.JDBC_USER, user);
            mp.put(TopLinkProperties.JDBC_PASSWORD, password);
        } catch (Exception ex) {
            configEntity.getTransaction().rollback();
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mp;
    }

    public Properties getLocalDbProperties() {
        Properties p = new Properties();
        LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
        File configFile = new File(ls.getDirectory().getPath() + File.separatorChar + DB_CONFIG);
        if (configFile.exists()) {
            try {
                p.load(ls.openInputFile(DB_CONFIG));
                p.setProperty("Password", textDecrypt(p.getProperty("Password")));
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            p.setProperty("Server", "localhost");
            p.setProperty("Port", "3306");
            p.setProperty("Catalog", "");
            p.setProperty("User", "root");
            p.setProperty("Password", "");
        }
        server = p.getProperty("Server");
        port = p.getProperty("Port");
        catalog = p.getProperty("Catalog");
        user = p.getProperty("User");
        password = p.getProperty("Password");
        url = "jdbc:mysql://" + server + ":" + port + "/" + catalog;
        return p;
    }

    public Boolean setLocalDbProperties(String Server, Integer Port, String Catalog, String User, String Password) {
        Properties p = new Properties();
        Boolean result = false;
        p.setProperty("Server", Server);
        p.setProperty("Port", String.valueOf(Port));
        p.setProperty("Catalog", Catalog);
        Password = textEncrypt(Password);
        p.setProperty("User", User);
        p.setProperty("Password", Password);
        try {
            LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
            p.store(ls.openOutputFile(DB_CONFIG), "Restobiz Configuration\nDo not modify this file manually");
            result = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        return result;
    }

    public String getReportLocation() {
        String location = null;
        Properties prop = new Properties();
        LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
        File f = new File(ls.getDirectory().getPath() + File.separatorChar + RP_CONFIG);
        if (f.exists()) {
            try {
                prop.load(ls.openInputFile(RP_CONFIG));
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        location = prop.getProperty("Location", "");
        return location;
    }

    public void setReportLocation(String Location) {
        Properties prop = new Properties();
        prop.setProperty("Location", Location);
        try {
            LocalStorage ls = MainApp.getInstance(MainApp.class).getContext().getLocalStorage();
            prop.store(ls.openOutputFile(RP_CONFIG), "Restobiz Report Location\nDo not modify this file manually");
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean isValidUser(String User, String Password) {
        List userList;
        Boolean result = false;
        configQuery = configEntity.createNativeQuery("SELECT * FROM operator o WHERE o.namauser = #namauser AND o.password = MD5(#password)").setParameter("namauser", User).setParameter("password", Password);
        userList = configQuery.getResultList();
        result = !userList.isEmpty();
        return result;
    }

    public Boolean confirmReopenTransaction(Date Tanggal, String Customer) {
        List transaksi;
        Boolean result = false;
        configQuery = configEntity.createNativeQuery("SELECT * FROM transaksi t WHERE t.tanggal = #tanggal AND t.customer = #customer AND t.posted = false").setParameter("tanggal", dateSQL.format(Tanggal)).setParameter("customer", Customer);
        transaksi = configQuery.getResultList();
        if (transaksi.isEmpty()) {
            result = true;
        } else {
            if (JOptionPane.showConfirmDialog(null, "Masih ada transaksi " + Customer + " hari ini yang aktif, anda yakin akan membuka transaksi baru?", "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                result = true;
            }
        }
        return result;
    }

    public Boolean isStandardLogin() {
        List userList;
        Boolean result = false;
        if (isDbConnected()) {
            configQuery = configEntity.createNativeQuery("SELECT * FROM operator o WHERE o.namauser = #namauser AND o.password = MD5(#password)").setParameter("namauser", "admin").setParameter("password", "admin");
            userList = configQuery.getResultList();
            result = !userList.isEmpty();
        }
        return result;
    }

    public Boolean isDbConnected() {
        Connection conn = null;
        Boolean result = false;
        getLocalDbProperties();
        try {
            Class.forName(DB_DRIVER);
            conn = DriverManager.getConnection(url, user, password);
            result = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return result;
    }

    public Boolean confirmConnected() {
        Boolean result = false;
        if (isDbConnected()) {
            result = true;
        } else {
            JOptionPane.showMessageDialog(null, "Koneksi database program belum terhubung");
        }
        return result;
    }

    public Boolean confirmUserLogged() {
        Boolean result = false;
        if (isUserLogged()) {
            result = true;
        } else {
            JOptionPane.showMessageDialog(null, "Anda belum login, silahkan login terlebih dahulu");
        }
        return result;
    }

    public Boolean confirmMenuExists() {
        Boolean result = false;
        List menuList = getMenu();
        if (!menuList.isEmpty()) {
            result = true;
        } else {
            JOptionPane.showMessageDialog(null, "Silahkan isi daftar menu terlebih dahulu");
        }
        return result;
    }

    public Connection getJDBCConnection() {
        Connection conn = null;
        if (isDbConnected()) {
            getLocalDbProperties();
            try {
                Class.forName(DB_DRIVER);
                conn = DriverManager.getConnection(url, user, password);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return conn;
    }

    public Boolean createDatabaseTable(String FileName) {
        Boolean result = false;
        try {
            ExecuteSqlScript sqlExec = new ExecuteSqlScript(FileName, getJDBCConnection());
            sqlExec.loadScript();
            sqlExec.execute();
            result = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public void printMenuList() {
        if (confirmConnected()) {
            try {
                Map parameters = new HashMap();
                parameters = getCompanyReportHeader();
                previewReport(null, "menulist.jasper", parameters, "Daftar Menu", true);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(GeneralConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
