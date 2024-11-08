import java.io.*;
import java.sql.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.text.NumberFormat;

public class POSController implements POSEventListener, POSStudentListener {

    private POSGUI gui;

    private DBSettings settings;

    private DBPOSSettings posExSet;

    private POSRegistration regServ;

    private DBManager dbMan;

    private Order order;

    private Order lastOrder;

    private PSSplashScreen splash;

    private PSCashDrawer cashDrawer;

    private Student student;

    private int intMode;

    private MoneyBuffer mBuf;

    private PSOrderSummary summary;

    private NumberFormat money;

    private ThreadedImageManager tim;

    private StudentInformationSystem sis;

    private File settingsFile;

    private String strPOSPrefix;

    private POSCreditGUI posCredGUI;

    private Boolean blDepositCredit;

    public static final int MODE_LOGIN = 0;

    public static final int MODE_ITEMS = 1;

    public static final int MODE_CHECKOUT = 2;

    public static final String VERSION = "2.0.0";

    public POSController(POSGUI gui, String settings) {
        this.gui = gui;
        gui.addPOSEventListener(this);
        mBuf = new MoneyBuffer();
        this.settingsFile = new File(settings);
        buildController();
    }

    private double round2Places(double d) {
        return Math.round(d * 100d) / 100d;
    }

    private void buildController() {
        money = NumberFormat.getCurrencyInstance();
        blDepositCredit = false;
        if (settingsFile == null) settingsFile = new File("etc" + File.separator + "settings.dbp");
        if (settingsFile.exists() && settingsFile.canWrite()) {
            DBSettingsWriter writ = new DBSettingsWriter();
            settings = (DBSettings) writ.loadSettingsDB(settingsFile);
        } else System.err.println(settingsFile.getPath() + " not found!");
        sis = new StudentInformationSystem(settings);
        strPOSPrefix = settings.get(DBSettings.POS_TABLEPREFIX);
        if (strPOSPrefix == null) strPOSPrefix = "";
        if (checkCriticalSettings()) {
            try {
                ImagePackage ip = new ImagePackage();
                ip.setImage(ImagePackage.IMG_BACKGROUND, "images/" + settings.get(DBSettings.IMAGES_MAINBG));
                gui.loadGUI(ip);
                dbMan = new DBManager();
                gui.setStatus("Attempting to connect to SIS - " + sis.getSISName());
                if (sis.isComplete()) {
                    Updater update = new Updater(settings.get(DBSettings.UPDATEURL));
                    update.start();
                    gui.setStatus("Connected to SIS - " + sis.getSISName());
                    gui.setStatus("Preparing login screen...please wait...");
                    setGlobalMode(POSController.MODE_LOGIN);
                    student = new SchoolStudent(Student.NOSTUDENT, sis);
                    resetStudent();
                    if (settings.get(DBSettings.DRAWER_ENABLED).compareTo("1") == 0) {
                        try {
                            cashDrawer = CashDrawerManager.getDrawer(settings.get(DBSettings.DRAWER_CLASS));
                            cashDrawer.setDrawerName(settings.get(DBSettings.DRAWER_COMMONNAME));
                            cashDrawer.addPOSEventListener(this);
                            if (!cashDrawer.prepareDrawer()) {
                                cashDrawer = null;
                            }
                        } catch (Exception exCD) {
                            cashDrawer = null;
                        }
                        gui.setStatus("Cash drawer is active.");
                    } else {
                        gui.setStatus("Cash drawer is disabled.");
                        cashDrawer = null;
                    }
                    if (update.checkComplete()) checkLastestVersion(update.getVersion());
                } else {
                    gui.setStatus("Connection to sis Failed...System Halted, please set debug to 1 in the settings file to debug the problem.", true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkLastestVersion(String serverVersion) {
        if (serverVersion.trim().compareTo(VERSION.trim()) != 0) gui.setStatus("A new version of PSCafePOS has been released!\n\nCurrent Version: " + VERSION.trim() + "\nNew Version: " + serverVersion.trim() + "\n\nIt is highly recommended that you update to the latest version, please download the updated version from \n\nhttp://www.sf.net/project/pscafe", true); else gui.setStatus("PSCafePOS is currently up to date! Thanks for using this product!");
    }

    private String getHostName() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException e) {
            System.err.println("unable to determine local address and hostname");
            return null;
        }
    }

    private void setGlobalMode(int m) {
        intMode = m;
        blDepositCredit = false;
        gui.setMode(m);
    }

    private OrderItem getItemByID(int itemid) {
        OrderItem item = null;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbMan.getPOSConnection();
            stmt = conn.createStatement();
            if (stmt.execute("select * from " + strPOSPrefix + "items where item_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and item_visible = '1' and item_id = " + itemid)) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    int id = rs.getInt("item_id");
                    String name = rs.getString("item_name");
                    String desc = rs.getString("item_description");
                    String category = rs.getString("item_category");
                    String build = rs.getString("item_building");
                    double price = rs.getDouble("item_price");
                    double redPrice = rs.getDouble("item_reducedprice");
                    boolean free = (rs.getString("item_allowfree").compareTo("1") == 0);
                    boolean reduced = (rs.getString("item_allowreduced").compareTo("1") == 0);
                    boolean typeA = (rs.getString("item_istypea").compareTo("1") == 0);
                    int freeBL = rs.getInt("item_fr_bl");
                    item = new OrderItem(id, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                    if (item.completeItem()) return item; else return null;
                }
            }
        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
            System.err.println("SQLState: " + sqlEx.getSQLState());
            System.err.println("VendorError: " + sqlEx.getErrorCode());
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.err.println(e);
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                    rs = null;
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                        stmt = null;
                    }
                }
            }
        }
        return item;
    }

    private Vector getItems(String cat) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Vector items = new Vector();
        try {
            conn = dbMan.getPOSConnection();
            stmt = conn.createStatement();
            if (stmt.execute("select * from " + strPOSPrefix + "items where item_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and item_visible = '1' and item_category = '" + cat + "' order by item_name")) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    OrderItem single;
                    int id = rs.getInt("item_id");
                    String name = rs.getString("item_name");
                    String desc = rs.getString("item_description");
                    String category = rs.getString("item_category");
                    String build = rs.getString("item_building");
                    double price = rs.getDouble("item_price");
                    double redPrice = rs.getDouble("item_reducedprice");
                    boolean free = (rs.getString("item_allowfree").compareTo("1") == 0);
                    boolean reduced = (rs.getString("item_allowreduced").compareTo("1") == 0);
                    boolean typeA = (rs.getString("item_istypea").compareTo("1") == 0);
                    int freeBL = rs.getInt("item_fr_bl");
                    single = new OrderItem(id, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                    if (single.completeItem()) items.add(single);
                }
            }
            stmt = null;
            rs = null;
            stmt = conn.createStatement();
            if (stmt.execute("select " + strPOSPrefix + "batch_master.mb_name, " + strPOSPrefix + "batch_master.mb_id from " + strPOSPrefix + "batch_master where " + strPOSPrefix + "batch_master.mb_active = '1' and " + strPOSPrefix + "batch_master.mb_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and " + strPOSPrefix + "batch_master.mb_category = '" + cat + "' order by " + strPOSPrefix + "batch_master.mb_name")) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    String batchName = rs.getString("mb_name");
                    int batchID = rs.getInt("mb_id");
                    if (batchName != null && batchID >= 0) {
                        PSBatchItem batch = new PSBatchItem(batchName);
                        Statement stmtItem = null;
                        ResultSet rsItem = null;
                        stmtItem = conn.createStatement();
                        if (stmtItem.execute("select " + strPOSPrefix + "items.* from (SELECT * FROM " + strPOSPrefix + "batch_items WHERE ib_batchid = '" + batchID + "') " + strPOSPrefix + "batch_items inner join (SELECT * FROM " + strPOSPrefix + "items WHERE item_visible = '1' and item_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "') " + strPOSPrefix + "items on ( " + strPOSPrefix + "batch_items.ib_itemid = " + strPOSPrefix + "items.item_id ) order by item_name")) {
                            rsItem = stmtItem.getResultSet();
                            while (rsItem.next()) {
                                OrderItem single;
                                int id = rsItem.getInt("item_id");
                                String name = rsItem.getString("item_name");
                                String desc = rsItem.getString("item_description");
                                String category = rsItem.getString("item_category");
                                String build = rsItem.getString("item_building");
                                double price = rsItem.getDouble("item_price");
                                double redPrice = rsItem.getDouble("item_reducedprice");
                                boolean free = (rsItem.getString("item_allowfree").compareTo("1") == 0);
                                boolean reduced = (rsItem.getString("item_allowreduced").compareTo("1") == 0);
                                boolean typeA = (rsItem.getString("item_istypea").compareTo("1") == 0);
                                int freeBL = rsItem.getInt("item_fr_bl");
                                single = new OrderItem(id, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                                if (single.completeItem()) batch.addItem(single);
                            }
                            if (batch.getItems().size() > 0) items.add(batch);
                        }
                    }
                }
            }
        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
            System.err.println("SQLState: " + sqlEx.getSQLState());
            System.err.println("VendorError: " + sqlEx.getErrorCode());
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.err.println(e);
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                    rs = null;
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                        stmt = null;
                    }
                }
            }
        }
        return items;
    }

    private String[] getCategories() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Vector vCats = new Vector();
        try {
            conn = dbMan.getPOSConnection();
            stmt = conn.createStatement();
            if (stmt.execute("select item_category from " + strPOSPrefix + "items where item_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and item_visible = '1' group by item_category order by item_category")) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    vCats.add(new String(rs.getString("item_category")));
                }
            }
            stmt = null;
            rs = null;
            stmt = conn.createStatement();
            if (stmt.execute("select " + strPOSPrefix + "batch_master.mb_category from ( SELECT * FROM " + strPOSPrefix + "batch_master WHERE " + strPOSPrefix + "batch_master.mb_active = '1' and " + strPOSPrefix + "batch_master.mb_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "') " + strPOSPrefix + "batch_master inner join " + strPOSPrefix + "batch_items on ( " + strPOSPrefix + "batch_master.mb_id = " + strPOSPrefix + "batch_items.ib_batchid ) group by " + strPOSPrefix + "batch_master.mb_category")) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    String curCat = rs.getString("mb_category");
                    if (curCat != null) {
                        if (vCats.size() > 0) {
                            boolean exists = false;
                            for (int z = 0; z < vCats.size(); z++) {
                                String tmp = (String) vCats.get(z);
                                if (curCat.compareToIgnoreCase(tmp) == 0) exists = true;
                            }
                            if (!exists) vCats.add(new String(curCat));
                        } else {
                            vCats.add(new String(curCat));
                        }
                    }
                }
            }
            if (vCats.size() > 0) {
                String[] cats = new String[vCats.size()];
                vCats.toArray(cats);
                return cats;
            }
        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
            System.err.println("SQLState: " + sqlEx.getSQLState());
            System.err.println("VendorError: " + sqlEx.getErrorCode());
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.err.println(e);
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                    rs = null;
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                        stmt = null;
                    }
                }
            }
        }
        return null;
    }

    private Vector getHotBarItems() {
        Vector items = new Vector();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbMan.getPOSConnection();
            stmt = conn.createStatement();
            if (stmt.execute("select * from (SELECT * FROM " + strPOSPrefix + "hotbar WHERE hb_register = '" + getHostName() + "' AND hb_cashier = '" + dbMan.getPOSUser() + "' and hb_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' ) " + strPOSPrefix + "hotbar inner join (SELECT * FROM " + strPOSPrefix + "items WHERE item_visible = '1') " + strPOSPrefix + "items on ( " + strPOSPrefix + "hotbar.hb_itemid = " + strPOSPrefix + "items.item_id ) order by hb_count desc ")) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    OrderItem single;
                    int id = rs.getInt("item_id");
                    String name = rs.getString("item_name");
                    String desc = rs.getString("item_description");
                    String category = rs.getString("item_category");
                    String build = rs.getString("item_building");
                    double price = rs.getDouble("item_price");
                    double redPrice = rs.getDouble("item_reducedprice");
                    boolean free = (rs.getString("item_allowfree").compareTo("1") == 0);
                    boolean reduced = (rs.getString("item_allowreduced").compareTo("1") == 0);
                    boolean typeA = (rs.getString("item_istypea").compareTo("1") == 0);
                    int freeBL = rs.getInt("item_fr_bl");
                    single = new OrderItem(id, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                    if (single.completeItem()) items.add(single);
                }
            }
        } catch (SQLException sqlEx) {
            System.err.println("SQLException: " + sqlEx.getMessage());
            System.err.println("SQLState: " + sqlEx.getSQLState());
            System.err.println("VendorError: " + sqlEx.getErrorCode());
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.err.println(e);
            return null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                    rs = null;
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                        stmt = null;
                    }
                }
            }
        }
        return items;
    }

    private void loadAutoAddItems() {
        if (intMode == MODE_ITEMS && posExSet != null) {
            if (order != null) {
                int intRegID = regServ.getPOSRegID();
                Connection conn = null;
                Statement stmt = null;
                ResultSet rs = null;
                try {
                    conn = dbMan.getPOSConnection();
                    stmt = conn.createStatement();
                    if (stmt.execute("select paa_itemid from " + strPOSPrefix + "pos_autoadditems where paa_posid = '" + intRegID + "'")) {
                        Vector vctItems = new Vector();
                        rs = stmt.getResultSet();
                        while (rs.next()) {
                            vctItems.add(new Integer(rs.getInt("paa_itemid")));
                        }
                        if (vctItems.size() > 0) {
                            for (int i = 0; i < vctItems.size(); i++) {
                                int intCurItemId = (Integer) vctItems.get(i);
                                if (intCurItemId >= 0) {
                                    Statement stmt_items = conn.createStatement();
                                    ResultSet rs_items = null;
                                    if (stmt_items.execute("select * from " + strPOSPrefix + "items where item_id = '" + intCurItemId + "' and item_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and item_visible = '1'")) {
                                        rs_items = stmt_items.getResultSet();
                                        while (rs_items.next()) {
                                            int iid = rs_items.getInt("item_id");
                                            String name = rs_items.getString("item_name");
                                            String desc = rs_items.getString("item_description");
                                            String category = rs_items.getString("item_category");
                                            String build = rs_items.getString("item_building");
                                            double price = rs_items.getDouble("item_price");
                                            double redPrice = rs_items.getDouble("item_reducedprice");
                                            boolean free = (rs_items.getString("item_allowfree").compareTo("1") == 0);
                                            boolean reduced = (rs_items.getString("item_allowreduced").compareTo("1") == 0);
                                            boolean typeA = (rs_items.getString("item_istypea").compareTo("1") == 0);
                                            int freeBL = rs_items.getInt("item_fr_bl");
                                            OrderItem single = new OrderItem(iid, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                                            if (single.completeItem()) order.addItem(single);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException sqlEx) {
                    System.err.println("SQLException: " + sqlEx.getMessage());
                    System.err.println("SQLState: " + sqlEx.getSQLState());
                    System.err.println("VendorError: " + sqlEx.getErrorCode());
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                    System.err.println(e);
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException sqlEx) {
                            rs = null;
                        }
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (SQLException sqlEx) {
                                stmt = null;
                            }
                        }
                    }
                }
            }
        }
    }

    private void pushLastOrder() {
        String showLast = null;
        if (posExSet != null) showLast = posExSet.getGeneralSettings("displayLastOrder");
        boolean blGo = true;
        String id = "";
        if (student != null && student.getStudentNumber().compareTo(Student.NOSTUDENT) != 0) {
            id = student.getStudentNumber();
        } else {
            blGo = false;
        }
        if (showLast != null) {
            if (showLast.compareTo("0") == 0) blGo = false;
        }
        if (blGo) {
            lastOrder = new Order("Student's Last Order");
            if (intMode == MODE_ITEMS) {
                if (id != null) {
                    if (id.compareTo(Student.NOSTUDENT) != 0) {
                        gui.setStatus("Searching for student's last order...");
                        id = id.replaceAll("[/']", "");
                        Connection conn = null;
                        Statement stmt = null;
                        ResultSet rs = null;
                        try {
                            conn = dbMan.getPOSConnection();
                            stmt = conn.createStatement();
                            if (stmt.execute("select tm_id from " + strPOSPrefix + "trans_master where tm_register = '" + getHostName() + "' and  tm_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and tm_studentid = '" + id + "' order by tm_id desc")) {
                                int intMtId = -1;
                                rs = stmt.getResultSet();
                                while (rs.next()) {
                                    intMtId = rs.getInt("tm_id");
                                }
                                if (intMtId > -1) {
                                    Statement stmt_items = conn.createStatement();
                                    ResultSet rs_items = null;
                                    if (stmt_items.execute("select * from (SELECT * FROM " + strPOSPrefix + "trans_item WHERE ti_tmid = '" + intMtId + "') " + strPOSPrefix + "trans_item inner join (SELECT * FROM " + strPOSPrefix + "items WHERE item_visible = '1') " + strPOSPrefix + "items on ( " + strPOSPrefix + "trans_item.ti_itemid = " + strPOSPrefix + "items.item_id ) ")) {
                                        rs_items = stmt_items.getResultSet();
                                        while (rs_items.next()) {
                                            int iid = rs_items.getInt("item_id");
                                            String name = rs_items.getString("item_name");
                                            String desc = rs_items.getString("item_description");
                                            String category = rs_items.getString("item_category");
                                            String build = rs_items.getString("item_building");
                                            double price = rs_items.getDouble("item_price");
                                            double redPrice = rs_items.getDouble("item_reducedprice");
                                            boolean free = (rs_items.getString("item_allowfree").compareTo("1") == 0);
                                            boolean reduced = (rs_items.getString("item_allowreduced").compareTo("1") == 0);
                                            boolean typeA = (rs_items.getString("item_istypea").compareTo("1") == 0);
                                            int freeBL = rs_items.getInt("item_fr_bl");
                                            OrderItem single = new OrderItem(iid, name, desc, category, build, price, redPrice, free, reduced, typeA, freeBL);
                                            if (single.completeItem()) lastOrder.addItem(single);
                                        }
                                        if (lastOrder.getNumberOfItems() > 0) {
                                            gui.loadStudentsLastOrder(lastOrder);
                                        }
                                    }
                                }
                            }
                        } catch (SQLException sqlEx) {
                            System.err.println("SQLException: " + sqlEx.getMessage());
                            System.err.println("SQLState: " + sqlEx.getSQLState());
                            System.err.println("VendorError: " + sqlEx.getErrorCode());
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            System.err.println(e);
                        } finally {
                            if (rs != null) {
                                try {
                                    rs.close();
                                } catch (SQLException sqlEx) {
                                    rs = null;
                                }
                                if (stmt != null) {
                                    try {
                                        stmt.close();
                                    } catch (SQLException sqlEx) {
                                        stmt = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void inputKeyPressed(char k, String curBuffer) {
        if (intMode == MODE_ITEMS) {
            if (curBuffer != null) gui.setStatus("Current Input: " + curBuffer);
        }
    }

    public void processStudentScan(String id) {
        if (intMode == MODE_ITEMS) {
            boolean blProcessAsItem = true;
            blDepositCredit = false;
            gui.setEnabled(false);
            if (id != null && sis.useSIS()) {
                if (id.length() > 0) {
                    if (!id.contains(" ") || sis.allowSpacesInID()) {
                        if (!sis.idFixedWidth() || (sis.idFixedWidth() && (id.length() == sis.idFixedWidthLength() || id == Student.NOSTUDENT))) {
                            gui.setStatus("Processing Scan...");
                            id = id.replaceAll("[/']", "");
                            gui.setStatus("Checking for a prefix to remove from front of string...");
                            String prefix = posExSet.getGeneralSettings("rmPrefix");
                            if (prefix != null) {
                                if (id.toLowerCase().startsWith(prefix.toLowerCase())) {
                                    StringBuffer buf = new StringBuffer(id);
                                    buf.delete(0, prefix.length());
                                    id = buf.toString();
                                }
                            }
                            SchoolStudent tmpStu = new SchoolStudent(id, sis);
                            if (tmpStu.isAnonStudent() || tmpStu.getExistsInDB()) {
                                blProcessAsItem = false;
                                student = tmpStu;
                                pushStudentObjToGUI();
                                pushLastOrder();
                                String imagePath = sis.getImagePath();
                                gui.setStatus("Student: " + id + " found!");
                                if (imagePath != null && imagePath.contains("{studentid}")) {
                                    try {
                                        imagePath = imagePath.replace("{studentid}", id);
                                        if (imagePath != null) {
                                            if (tim != null) {
                                                if (tim.isAlive()) {
                                                    tim.interrupt();
                                                }
                                            }
                                            tim = new ThreadedImageManager(imagePath, 150);
                                            tim.setPOSEventListener(this);
                                            tim.start();
                                        }
                                    } catch (Exception e) {
                                        System.err.println(e.getMessage());
                                    }
                                }
                            } else {
                                gui.setStatus("Student: " + id + " was not found, checking if it is an item's barcode");
                            }
                        } else {
                            gui.setStatus("Invalid Student ID, Fixed width of " + sis.idFixedWidthLength() + " required", true);
                        }
                    } else {
                        gui.setStatus("Spaces in ID numbers are not allowed as configured in the settings file.", false);
                    }
                }
            }
            if (blProcessAsItem) {
                addItemByBarcode(id);
            }
            updateOrderPricing();
            gui.setEnabled(true);
        }
    }

    private void addItemByBarcode(String bc) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        if (bc != null) {
            try {
                conn = dbMan.getPOSConnection();
                stmt = conn.createStatement();
                if (stmt.execute("select ibItemID, ibType from " + strPOSPrefix + "item_barcodes where ibBarcode = '" + bc + "'")) {
                    rs = stmt.getResultSet();
                    while (rs.next()) {
                        if (rs.getString("ibType").compareTo("1") == 0) {
                            OrderItem item = getItemByID(rs.getInt("ibItemID"));
                            if (item != null) {
                                addItemToOrder(item);
                            }
                        } else if (rs.getString("ibType").compareTo("2") == 0) {
                            System.out.println("Adding Batch items via barcode scan is not yet supported!");
                        }
                    }
                }
            } catch (SQLException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
                System.err.println("SQLState: " + sqlEx.getSQLState());
                System.err.println("VendorError: " + sqlEx.getErrorCode());
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println(e);
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                        rs = null;
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) {
                            stmt = null;
                        }
                    }
                }
            }
        }
    }

    private void pushStudentObjToGUI() {
        gui.removeLastOrderScreen();
        gui.loadStudent(student, sis.useFreeReduced() && !sis.hideFreeReduced(), !canGetFRMealToday(OrderItem.COUNTS_AS_FREE_REDUCED_BREAKFAST), !canGetFRMealToday(OrderItem.COUNTS_AS_FREE_REDUCED_LUNCH), getStudentCredit());
        pushLastOrder();
    }

    private void pushStudentImage(ImageIcon i) {
        if (i != null) {
            if (intMode == MODE_ITEMS) {
                gui.loadStudentImage(i);
            }
        }
    }

    private boolean checkCriticalSettings() {
        if (settings != null) {
            if (settings.get(DBSettings.MAIN_BUILDING) == null || settings.get(DBSettings.MAIN_BUILDING).length() <= 0) {
                gui.setCriticalMessage("The Building value is not set in the config file.");
                return false;
            }
            if (settings.get(DBSettings.POS_HOSTNAME) == null || settings.get(DBSettings.POS_HOSTNAME).length() <= 0) {
                gui.setCriticalMessage("The POS Database Setting: [hostname] is not set in the config file.");
                return false;
            }
            if (settings.get(DBSettings.POS_DATABASE) == null || settings.get(DBSettings.POS_DATABASE).length() <= 0) {
                gui.setCriticalMessage("The POS Database Setting: [database] is not set in the config file.");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void loadItems(boolean resetOrder) {
        setGlobalMode(MODE_ITEMS);
        if (resetOrder || order == null) {
            order = new Order("Current Order");
            loadAutoAddItems();
        }
        gui.refreshOrder(order);
        gui.addHotbarItems(getHotBarItems());
        resetStudent();
    }

    private void resetStudent() {
        gui.loadStudent(student, false, false, false, 0d);
        pushLastOrder();
        updateOrderPricing();
    }

    private void checkForMessages() {
        if (posExSet != null) {
            if (posExSet.hasMessage()) {
                String msg = posExSet.getMessage();
                if (msg != null) gui.setStatus(msg, true);
            }
        }
    }

    private void attemptLogin() {
        gui.setStatus("Attempting Login...");
        if (dbMan.setUpPOSConnection(settings.get(DBSettings.POS_JDBCDRIVER), settings.get(DBSettings.POS_HOSTNAME), settings.get(DBSettings.POS_DATABASE), gui.getLoginUserName(), gui.getLoginPassword(), settings.get(DBSettings.POS_JDBCDBTYPE), settings.get(DBSettings.POS_JDBCPORT), settings.get(DBSettings.POS_SSLENABLED).compareTo("1") == 0)) {
            regServ = new POSRegistration(dbMan, settings.get(DBSettings.MAIN_BUILDING), strPOSPrefix);
            if (regServ.register()) {
                gui.setStatus("POS registration/update completed!");
            } else {
                gui.setStatus("POS Registration failed! Unable to register or update POS information with server.  You will still be allowed to use this POS, but some settings and server communication may not be functional!", true);
            }
            gui.setStatus("Attempting to create extended settings object...");
            posExSet = new DBPOSSettings(dbMan, regServ.getPOSRegID(), strPOSPrefix);
            checkForMessages();
            gui.setStatus("Login Successful!");
            gui.loadKeyManager(this);
            loadItems(true);
        } else {
            gui.setStatus("Login Failed, please try again.", true);
        }
    }

    private void loadCatsToGUI() {
        gui.loadCatgories(getCategories());
    }

    private void loadItemsToGUI(String cat) {
        gui.loadItems(getItems(cat));
    }

    private void addItemToOrder(OrderItem item) {
        if (intMode == MODE_ITEMS) {
            if (item != null) {
                gui.setStatus("Adding item: " + item.getName() + " please wait...");
                order.addItem((OrderItem) item.clone());
                gui.refreshOrder(order);
                updateOrderPricing();
                gui.setStatus("Added item: " + item.getName());
            } else {
                gui.setStatus("Unable to add item because object reference is null.", true);
            }
        }
    }

    private void drawerOpened() {
        gui.disableUI();
    }

    private void drawerClosed() {
        if (splash != null) splash.kill();
        if (summary != null) {
            summary.kill();
            resetOrder();
        }
        gui.enableUI();
    }

    private void processNoSale() {
        if (intMode == MODE_ITEMS) {
            if (order == null || order.getNumberOfItems() == 0) {
                if (cashDrawer != null) {
                    gui.setStatus("Processing no sale.  ");
                    cashDrawer.openDrawer();
                } else {
                    gui.setStatus("No cash drawer is currently loaded.", true);
                }
            } else gui.setStatus("No sale can only be done when there are no items in the current order.", true);
        } else gui.setStatus("No sale can only be done in item mode.", true);
    }

    private void updateOrderPricing() {
        if (intMode == MODE_ITEMS || intMode == MODE_CHECKOUT) {
            if (order != null && order.getNumberOfItems() > 0) {
                OrderItem[] items = order.getOrderItems();
                if (student != null) {
                    if (student.isFree() || student.isReduced()) {
                        boolean gotSpecialBreakfast = !canGetFRMealToday(OrderItem.COUNTS_AS_FREE_REDUCED_BREAKFAST);
                        boolean gotSpecialLunch = !canGetFRMealToday(OrderItem.COUNTS_AS_FREE_REDUCED_LUNCH);
                        if (student.isFree()) {
                            for (int i = 0; i < items.length; i++) {
                                if (items[i].isFree()) {
                                    if (items[i].countAsBreakOrLunch() == OrderItem.COUNTS_AS_FREE_REDUCED_BREAKFAST) {
                                        if (gotSpecialBreakfast) {
                                            items[i].sellAsFree(false);
                                        } else {
                                            items[i].sellAsFree(true);
                                            gotSpecialBreakfast = true;
                                        }
                                    } else if (items[i].countAsBreakOrLunch() == OrderItem.COUNTS_AS_FREE_REDUCED_LUNCH) {
                                        if (gotSpecialLunch) {
                                            items[i].sellAsFree(false);
                                        } else {
                                            items[i].sellAsFree(true);
                                            gotSpecialLunch = true;
                                        }
                                    }
                                }
                            }
                        } else if (student.isReduced()) {
                            for (int i = 0; i < items.length; i++) {
                                if (items[i].isReduced()) {
                                    if (items[i].countAsBreakOrLunch() == OrderItem.COUNTS_AS_FREE_REDUCED_BREAKFAST) {
                                        if (gotSpecialBreakfast) {
                                            items[i].sellAsReduced(false);
                                        } else {
                                            items[i].sellAsReduced(true);
                                            gotSpecialBreakfast = true;
                                        }
                                    } else if (items[i].countAsBreakOrLunch() == OrderItem.COUNTS_AS_FREE_REDUCED_LUNCH) {
                                        if (gotSpecialLunch) {
                                            items[i].sellAsReduced(false);
                                        } else {
                                            items[i].sellAsReduced(true);
                                            gotSpecialLunch = true;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < items.length; i++) {
                            items[i].sellAsFree(false);
                            items[i].sellAsReduced(false);
                        }
                    }
                } else {
                    for (int i = 0; i < items.length; i++) {
                        items[i].sellAsFree(false);
                        items[i].sellAsReduced(false);
                    }
                }
            }
            gui.refreshOrder(order);
        }
    }

    private void voidLastItem() {
        if (intMode == MODE_ITEMS) {
            if (order != null && order.getNumberOfItems() != 0) {
                order.voidLast();
                gui.refreshOrder(order);
                updateOrderPricing();
            }
        }
    }

    private void voidItem(OrderItem item) {
        if (intMode == MODE_ITEMS) {
            if (order != null && order.getNumberOfItems() != 0) {
                if (item != null) {
                    if (order.voidItem(item)) gui.setStatus("Voided Item: " + item.getName());
                    gui.refreshOrder(order);
                    updateOrderPricing();
                }
            }
        }
    }

    private void voidOrder() {
        if (intMode == MODE_ITEMS) {
            if (order != null && order.getNumberOfItems() != 0) {
                order.voidOrder();
                gui.refreshOrder(order);
            }
        }
    }

    private void resetAnonStudent() {
        gui.setStatus("Reseting Student to an anonymous Student.");
        processStudentScan(Student.NOSTUDENT);
        updateOrderPricing();
        gui.setStatus("Student reset.");
    }

    private void loadCheckOutScreen() {
        if (order != null && order.getNumberOfItems() != 0) {
            if (settings.get(DBSettings.MAIN_ALLOWANONTRANS).compareTo("0") == 0) {
                if (!student.isStudentSet()) {
                    gui.setStatus("Anonymous transactions are currently not allowed.", true);
                    return;
                }
            }
            if (!sis.allowOnlyActiveStudents() || (sis.allowOnlyActiveStudents() && student.getIsActiveStudent())) {
                if (!sis.allowOnlyStudentsThatExist() || (sis.allowOnlyStudentsThatExist() && student.getExistsInDB())) {
                    gui.setStatus("Loading Checkout Screen...");
                    setGlobalMode(MODE_CHECKOUT);
                    gui.removeLastOrderScreen();
                    gui.loadCheckOutInfo(order);
                    mBuf.flushAll();
                    gui.setStatus("Checkout screen ready");
                } else {
                    gui.setStatus("POS is set to only process students that exist in your sis, " + sis.getSISName(), true);
                }
            } else {
                gui.setStatus("POS is set to only process students that are active in your sis, " + sis.getSISName(), true);
            }
        } else {
            gui.setStatus("Can't load checkout screen because there are no items in the current order.", true);
        }
    }

    private void updateCashBuffer(double amt) {
        if (intMode == MODE_CHECKOUT) {
            if (mBuf != null) {
                if (amt > 0d) mBuf.addCash(amt); else if (amt == 0d) {
                    mBuf.setCash(order.getOrderTotal() - mBuf.getCredit());
                }
                gui.updateBuffer(mBuf, order);
                updateChangeCreditDeposit();
            }
        }
    }

    private void updateCreditBuffer(double amt) {
        if (intMode == MODE_CHECKOUT) {
            if (mBuf != null) {
                if (amt > 0d) mBuf.addCredit(amt); else if (amt == 0d) {
                    mBuf.setCredit(order.getOrderTotal() - mBuf.getCash());
                }
                gui.updateBuffer(mBuf, order);
                updateChangeCreditDeposit();
            }
        }
    }

    private void updateChangeCreditDeposit() {
        String strChange = "N/A";
        if (mBuf.getBufferTotal() > order.getOrderTotal()) {
            Double dblElig = order.getOrderTotal() - mBuf.getCredit();
            if (dblElig < 0) dblElig = 0d;
            Double dblDiff = mBuf.getCash() - dblElig;
            if (dblDiff >= 0d) {
                strChange = money.format(dblDiff);
            }
        }
        if (blDepositCredit) {
            gui.setDepositChange(strChange);
        } else {
            gui.setNoDepositChange(strChange);
        }
    }

    private void clearCash() {
        if (intMode == MODE_CHECKOUT) {
            if (mBuf != null) {
                mBuf.flushCash();
                gui.updateBuffer(mBuf, order);
                updateChangeCreditDeposit();
            }
        }
    }

    private void clearCredit() {
        if (intMode == MODE_CHECKOUT) {
            if (mBuf != null) {
                mBuf.flushCredit();
                gui.updateBuffer(mBuf, order);
                updateChangeCreditDeposit();
            }
        }
    }

    private void returnToItemsScreen() {
        if (intMode == MODE_CHECKOUT) {
            gui.setStatus("Returning to items screen...");
            setGlobalMode(MODE_ITEMS);
            processStudentScan(student.getStudentNumber());
            updateOrderPricing();
            gui.addHotbarItems(getHotBarItems());
            gui.setStatus("Item Screen Loaded.");
        }
    }

    private boolean canGetFRMealToday(int mealType) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        if (student.isStudentSet()) {
            try {
                conn = dbMan.getPOSConnection();
                stmt = conn.createStatement();
                if (mealType == OrderItem.COUNTS_AS_FREE_REDUCED_BREAKFAST || mealType == OrderItem.COUNTS_AS_FREE_REDUCED_LUNCH) {
                    String fld = "";
                    if (student.isFree()) fld = "ti_isFree"; else if (student.isReduced()) fld = "ti_isReduced"; else return false;
                    String sql = "select count(*) from (SELECT * FROM " + strPOSPrefix + "trans_item WHERE ti_studentid = '" + student.getStudentNumber() + "' AND extract(day from ti_datetime) = extract(day from current_date)  and extract(month from ti_datetime) = extract(month from current_date) and extract(year from ti_datetime) = extract(year from current_date) AND " + fld + " = '1' ) " + strPOSPrefix + "trans_item inner join ( SELECT * FROM " + strPOSPrefix + "items WHERE item_fr_bl = '" + mealType + "') " + strPOSPrefix + "items on (" + strPOSPrefix + "trans_item.ti_itemid = " + strPOSPrefix + "items.item_id)";
                    if (stmt.execute(sql)) {
                        rs = stmt.getResultSet();
                        while (rs.next()) {
                            if (rs.getInt(1) == 0) return true;
                        }
                    }
                }
            } catch (SQLException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
                System.err.println("SQLState: " + sqlEx.getSQLState());
                System.err.println("VendorError: " + sqlEx.getErrorCode());
                return false;
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println(e);
                return false;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                        rs = null;
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) {
                            stmt = null;
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            System.err.println(e);
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasStudentCredit() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean blHas = false;
        if (student.isStudentSet()) {
            try {
                conn = dbMan.getPOSConnection();
                stmt = conn.createStatement();
                if (stmt.execute("select count(*) as num from " + strPOSPrefix + "studentcredit where credit_studentid = '" + student.getStudentNumber() + "' and credit_active = '1'")) {
                    rs = stmt.getResultSet();
                    int cnt = 0;
                    int intNum = 0;
                    while (rs.next()) {
                        intNum = rs.getInt("num");
                        cnt++;
                    }
                    if (cnt == 1) {
                        if (intNum == 1) return true; else return blHas;
                    }
                }
            } catch (SQLException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
                System.err.println("SQLState: " + sqlEx.getSQLState());
                System.err.println("VendorError: " + sqlEx.getErrorCode());
                return blHas;
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println(e);
                return blHas;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                        rs = null;
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) {
                            stmt = null;
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            System.err.println(e);
                        }
                    }
                }
            }
        }
        return blHas;
    }

    private double getStudentCredit() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        double credit = 0d;
        if (student.isStudentSet()) {
            try {
                conn = dbMan.getPOSConnection();
                stmt = conn.createStatement();
                if (stmt.execute("select * from " + strPOSPrefix + "studentcredit where credit_studentid = '" + student.getStudentNumber() + "' and credit_active = '1'")) {
                    rs = stmt.getResultSet();
                    int cnt = 0;
                    while (rs.next()) {
                        credit = rs.getDouble("credit_amount");
                        cnt++;
                    }
                    if (cnt == 1) {
                        return credit;
                    }
                }
            } catch (SQLException sqlEx) {
                System.err.println("SQLException: " + sqlEx.getMessage());
                System.err.println("SQLState: " + sqlEx.getSQLState());
                System.err.println("VendorError: " + sqlEx.getErrorCode());
                return credit;
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println(e);
                return credit;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                        rs = null;
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) {
                            stmt = null;
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            System.err.println(e);
                        }
                    }
                }
            }
        }
        return credit;
    }

    private void processOrder() {
        double neg = 0d;
        if (intMode == MODE_CHECKOUT) {
            if (round2Places(mBuf.getBufferTotal()) >= round2Places(order.getOrderTotal())) {
                double cash, credit, allowedCredit = 0d;
                allowedCredit = getStudentCredit();
                if (settings.get(DBSettings.MAIN_ALLOWNEGBALANCES).compareTo("1") == 0) {
                    try {
                        neg = Double.parseDouble(settings.get(DBSettings.MAIN_MAXNEGBALANCE));
                    } catch (NumberFormatException ex) {
                        System.err.println("NumberFormatException::Potential problem with setting MAIN_MAXNEGBALANCE");
                        System.err.println("     * Note: If you enable negative balances, please don't leave this");
                        System.err.println("             blank.  At least set it to 0.  For right now we are setting ");
                        System.err.println("             the max negative balance to $0.00");
                        System.err.println("");
                        System.err.println("Exception Message:" + ex.getMessage());
                    }
                    if (neg < 0) neg *= -1;
                    allowedCredit += neg;
                }
                if (round2Places(mBuf.getCredit()) <= round2Places(allowedCredit)) {
                    if (round2Places(mBuf.getCredit()) > round2Places(getStudentCredit()) && !student.isStudentSet()) {
                        gui.setStatus("Can't allow negative balance on an anonymous student!", true);
                    } else {
                        if (round2Places(mBuf.getCredit()) > round2Places(order.getOrderTotal())) {
                            credit = round2Places(order.getOrderTotal());
                        } else {
                            credit = round2Places(mBuf.getCredit());
                        }
                        if ((mBuf.getCash() + credit) >= order.getOrderTotal()) {
                            cash = round2Places(order.getOrderTotal() - credit);
                            double change = round2Places(mBuf.getCash() - cash);
                            if (round2Places(cash + credit) == round2Places(order.getOrderTotal())) {
                                Connection conn = null;
                                Statement stmt = null;
                                ResultSet rs = null;
                                try {
                                    conn = dbMan.getPOSConnection();
                                    conn.setAutoCommit(false);
                                    stmt = conn.createStatement();
                                    String host = getHostName();
                                    String stuId = student.getStudentNumber();
                                    String building = settings.get(DBSettings.MAIN_BUILDING);
                                    String cashier = dbMan.getPOSUser();
                                    String strSql = "insert into " + strPOSPrefix + "trans_master ( tm_studentid, tm_total, tm_cashtotal, tm_credittotal, tm_building, tm_register, tm_cashier, tm_datetime, tm_change ) values( '" + stuId + "', '" + round2Places(order.getOrderTotal()) + "', '" + round2Places(cash) + "', '" + round2Places(credit) + "', '" + building + "', '" + host + "', '" + cashier + "', NOW(), '" + round2Places(change) + "')";
                                    int intSqlReturnVal = -1;
                                    int masterID = -1;
                                    try {
                                        intSqlReturnVal = stmt.executeUpdate(strSql, Statement.RETURN_GENERATED_KEYS);
                                        ResultSet keys = stmt.getGeneratedKeys();
                                        keys.next();
                                        masterID = keys.getInt(1);
                                        keys.close();
                                        stmt.close();
                                    } catch (Exception exRetKeys) {
                                        System.err.println(exRetKeys.getMessage() + " (but pscafepos is attempting a work around)");
                                        intSqlReturnVal = stmt.executeUpdate(strSql);
                                        masterID = dbMan.getLastInsertIDWorkAround(stmt, strPOSPrefix + "trans_master_tm_id_seq");
                                        if (masterID == -1) System.err.println("It looks like the work around failed, please submit a bug report!"); else System.err.println("work around was successful!");
                                    }
                                    if (intSqlReturnVal == 1) {
                                        if (masterID >= 0) {
                                            OrderItem[] itms = order.getOrderItems();
                                            if (itms != null && itms.length > 0) {
                                                for (int i = 0; i < itms.length; i++) {
                                                    if (itms[i] != null) {
                                                        stmt = conn.createStatement();
                                                        int itemid = itms[i].getDBID();
                                                        double itemprice = round2Places(itms[i].getEffectivePrice());
                                                        int f, r, a;
                                                        String strItemName, strItemBuilding, strItemCat;
                                                        f = 0;
                                                        r = 0;
                                                        a = 0;
                                                        if (itms[i].isSoldAsFree()) {
                                                            f = 1;
                                                        }
                                                        if (itms[i].isSoldAsReduced()) {
                                                            r = 1;
                                                        }
                                                        if (itms[i].isTypeA()) {
                                                            a = 1;
                                                        }
                                                        strItemName = itms[i].getName();
                                                        strItemBuilding = (String) itms[i].getBuilding();
                                                        strItemCat = itms[i].getCategory();
                                                        if (stmt.executeUpdate("insert into " + strPOSPrefix + "trans_item ( ti_itemid, ti_tmid, ti_pricesold, ti_registerid, ti_cashier, ti_studentid, ti_isfree, ti_isreduced, ti_datetime, ti_istypea, ti_itemname, ti_itembuilding, ti_itemcat  ) values('" + itemid + "', '" + masterID + "', '" + round2Places(itemprice) + "', '" + host + "', '" + cashier + "', '" + stuId + "', '" + f + "', '" + r + "', NOW(), '" + a + "', '" + strItemName + "', '" + strItemBuilding + "', '" + strItemCat + "')") != 1) {
                                                            gui.setCriticalMessage("Item insert failed");
                                                            conn.rollback();
                                                        }
                                                        stmt.close();
                                                        stmt = conn.createStatement();
                                                        String sqlInv = "SELECT inv_id from " + strPOSPrefix + "inventory where inv_menuid = " + itemid + "";
                                                        if (stmt.execute(sqlInv)) {
                                                            ResultSet rsInv = stmt.getResultSet();
                                                            int delId = -1;
                                                            if (rsInv.next()) {
                                                                delId = rsInv.getInt("inv_id");
                                                            }
                                                            if (delId != -1) {
                                                                stmt.executeUpdate("delete from " + strPOSPrefix + "inventory where inv_id = " + delId);
                                                            }
                                                            stmt.close();
                                                        }
                                                    } else {
                                                        gui.setCriticalMessage("Null Item");
                                                        conn.rollback();
                                                    }
                                                }
                                                boolean blOk = true;
                                                if (round2Places(credit) > 0d) {
                                                    if (round2Places(allowedCredit) >= round2Places(credit)) {
                                                        if (hasStudentCredit()) {
                                                            stmt = conn.createStatement();
                                                            if (stmt.executeUpdate("update " + strPOSPrefix + "studentcredit set credit_amount = credit_amount - " + round2Places(credit) + " where credit_active = '1' and credit_studentid = '" + stuId + "'") == 1) {
                                                                stmt.close();
                                                                stmt = conn.createStatement();
                                                                if (stmt.executeUpdate("update " + strPOSPrefix + "studentcredit set credit_lastused = NOW() where credit_active = '1' and credit_studentid = '" + stuId + "'") == 1) {
                                                                    stmt.close();
                                                                    stmt = conn.createStatement();
                                                                    if (stmt.executeUpdate("insert into " + strPOSPrefix + "studentcredit_log ( scl_studentid, scl_action, scl_transid, scl_datetime ) values( '" + stuId + "', '" + round2Places((-1) * credit) + "', '" + masterID + "', NOW() )") == 1) {
                                                                        stmt.close();
                                                                        blOk = true;
                                                                    } else {
                                                                        gui.setCriticalMessage("Unable to update student credit log.");
                                                                        blOk = false;
                                                                    }
                                                                } else {
                                                                    gui.setCriticalMessage("Unable to update student credit account.");
                                                                    blOk = false;
                                                                }
                                                            } else {
                                                                gui.setCriticalMessage("Unable to update student credit account.");
                                                                blOk = false;
                                                            }
                                                        } else {
                                                            stmt = conn.createStatement();
                                                            if (stmt.executeUpdate("insert into " + strPOSPrefix + "studentcredit (credit_amount,credit_active,credit_studentid,credit_lastused) values('" + round2Places((-1) * credit) + "','1','" + stuId + "', NOW())") == 1) {
                                                                stmt.close();
                                                                stmt = conn.createStatement();
                                                                if (stmt.executeUpdate("insert into " + strPOSPrefix + "studentcredit_log ( scl_studentid, scl_action, scl_transid, scl_datetime ) values( '" + stuId + "', '" + round2Places((-1) * credit) + "', '" + masterID + "', NOW() )") == 1) {
                                                                    stmt.close();
                                                                    blOk = true;
                                                                } else {
                                                                    gui.setCriticalMessage("Unable to update student credit log.");
                                                                    blOk = false;
                                                                }
                                                            } else {
                                                                gui.setCriticalMessage("Unable to create new student credit account.");
                                                                blOk = false;
                                                            }
                                                        }
                                                    } else {
                                                        gui.setCriticalMessage("Student doesn't have enought credit.");
                                                        blOk = false;
                                                    }
                                                }
                                                if (blOk) {
                                                    if (blDepositCredit && change > 0d) {
                                                        try {
                                                            if (doStudentCreditUpdate(change, stuId)) {
                                                                change = 0d;
                                                            } else blOk = false;
                                                        } catch (Exception cExp) {
                                                            blOk = false;
                                                        }
                                                    }
                                                }
                                                if (blOk) {
                                                    boolean blHBOK = true;
                                                    if (itms != null && itms.length > 0) {
                                                        for (int i = 0; i < itms.length; i++) {
                                                            stmt = conn.createStatement();
                                                            if (stmt.execute("select count(*) from " + strPOSPrefix + "hotbar where hb_itemid = '" + itms[i].getDBID() + "' and hb_building = '" + building + "' and hb_register = '" + host + "' and hb_cashier = '" + cashier + "'")) {
                                                                rs = stmt.getResultSet();
                                                                rs.next();
                                                                int num = rs.getInt(1);
                                                                stmt.close();
                                                                if (num == 1) {
                                                                    stmt = conn.createStatement();
                                                                    if (stmt.executeUpdate("update " + strPOSPrefix + "hotbar set hb_count = hb_count + 1 where hb_itemid = '" + itms[i].getDBID() + "' and hb_building = '" + building + "' and hb_register = '" + host + "' and hb_cashier = '" + cashier + "'") != 1) blHBOK = false;
                                                                } else {
                                                                    stmt = conn.createStatement();
                                                                    if (stmt.executeUpdate("insert into " + strPOSPrefix + "hotbar ( hb_itemid, hb_building, hb_register, hb_cashier, hb_count ) values( '" + itms[i].getDBID() + "', '" + building + "', '" + host + "', '" + cashier + "', '1' )") != 1) blHBOK = false;
                                                                }
                                                                stmt.close();
                                                            }
                                                        }
                                                    } else blHBOK = false;
                                                    if (blHBOK) {
                                                        conn.commit();
                                                        gui.setStatus("Order Complete.");
                                                        gui.disableUI();
                                                        summary = new PSOrderSummary(gui);
                                                        if (cashDrawer != null) cashDrawer.openDrawer(); else summary.setPOSEventListener(this);
                                                        summary.display(money.format(order.getOrderTotal()), money.format(mBuf.getCash()), money.format(credit), money.format(change), money.format(getStudentCredit()));
                                                    } else {
                                                        conn.rollback();
                                                        gui.setStatus("Failure during Hotbar update.  Transaction has been rolled back.", true);
                                                    }
                                                } else {
                                                    conn.rollback();
                                                }
                                            } else {
                                                gui.setCriticalMessage("Unable to fetch items.");
                                                conn.rollback();
                                            }
                                        } else {
                                            gui.setCriticalMessage("Unable to retrieve autoid");
                                            conn.rollback();
                                        }
                                    } else {
                                        gui.setCriticalMessage("Error During Writting of Transaction Master Record.");
                                        conn.rollback();
                                    }
                                } catch (SQLException sqlEx) {
                                    System.err.println("SQLException: " + sqlEx.getMessage());
                                    System.err.println("SQLState: " + sqlEx.getSQLState());
                                    System.err.println("VendorError: " + sqlEx.getErrorCode());
                                    try {
                                        conn.rollback();
                                    } catch (SQLException sqlEx2) {
                                        System.err.println("Rollback failed: " + sqlEx2.getMessage());
                                    }
                                } catch (Exception e) {
                                    System.err.println("Exception: " + e.getMessage());
                                    System.err.println(e);
                                    try {
                                        conn.rollback();
                                    } catch (SQLException sqlEx2) {
                                        System.err.println("Rollback failed: " + sqlEx2.getMessage());
                                    }
                                } finally {
                                    if (rs != null) {
                                        try {
                                            rs.close();
                                        } catch (SQLException sqlEx) {
                                            rs = null;
                                        }
                                        if (stmt != null) {
                                            try {
                                                stmt.close();
                                            } catch (SQLException sqlEx) {
                                                stmt = null;
                                            } catch (Exception e) {
                                                System.err.println("Exception: " + e.getMessage());
                                                System.err.println(e);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            gui.setStatus("Credit total + Cash total is less then the order total! ", true);
                        }
                    }
                } else {
                    if (settings.get(DBSettings.MAIN_ALLOWNEGBALANCES).compareTo("1") == 0) {
                        gui.setStatus("Sorry, maximum negative balance is " + money.format(neg) + "!", true);
                    } else gui.setStatus("Student does not have enough credit to process this order.", true);
                }
            } else {
                gui.setStatus("Buffer total is less then the order total.", true);
            }
        }
    }

    private void resetOrder() {
        gui.setStatus("Order Processed, preparing POS.");
        mBuf.flushAll();
        order.voidOrder();
        setGlobalMode(MODE_ITEMS);
        processStudentScan(Student.NOSTUDENT);
        gui.addHotbarItems(getHotBarItems());
        loadAutoAddItems();
        updateOrderPricing();
        gui.setStatus("Order complete");
        gui.refreshOrder(order);
        gui.requestFocus();
        checkForMessages();
    }

    private void clearHotBar() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbMan.getPOSConnection();
            stmt = conn.createStatement();
            if (stmt.executeUpdate("delete from " + strPOSPrefix + "hotbar where hb_building = '" + settings.get(DBSettings.MAIN_BUILDING) + "' and hb_register = '" + getHostName() + "' and hb_cashier = '" + dbMan.getPOSUser() + "'") > 0) {
                gui.setStatus("Hotbar items purged");
            }
        } catch (SQLException sqlEx) {
            System.err.println(sqlEx);
            System.err.println(sqlEx.getMessage());
        } catch (Exception e) {
            System.err.println(e);
            System.err.println(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                    stmt = null;
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                    System.err.println(e);
                }
            }
        }
    }

    private void exitSystem() {
        if (intMode == MODE_ITEMS) {
            if (order.getNumberOfItems() == 0) {
                gui.setStatus("Exiting System by user's request.");
                clearHotBar();
                System.exit(0);
            } else gui.setStatus("Can not exit while there is an active order.", true);
        } else if (intMode == MODE_LOGIN) {
            gui.setStatus("Exiting System");
            System.exit(0);
        } else gui.setStatus("You must be in Item Mode to exit this POS.", true);
    }

    private void loadKeyPad() {
        gui.loadKeyPad(this);
    }

    private void processItemAdd(Object item) {
        if (item != null) {
            try {
                if (Class.forName("PSBatchItem").isInstance(item)) {
                    PSBatchItem b = (PSBatchItem) item;
                    if (b != null) {
                        Vector vItems = b.getItems();
                        for (int i = 0; i < vItems.size(); i++) {
                            OrderItem singleItem = (OrderItem) vItems.get(i);
                            if (singleItem != null) addItemToOrder((OrderItem) (singleItem.clone()));
                        }
                        gui.setStatus("Batch add: " + vItems.size() + " items added.");
                    }
                } else if (Class.forName("OrderItem").isInstance(item)) {
                    OrderItem i = (OrderItem) item;
                    addItemToOrder((OrderItem) (i.clone()));
                }
            } catch (ClassNotFoundException cnfe) {
                System.err.println(cnfe.getMessage());
            }
        }
    }

    public void activateLastStudentOrder() {
        gui.setStatus("Activating order...");
        gui.removeLastOrderScreen();
        order = lastOrder;
        updateOrderPricing();
        gui.setStatus("Order is now active.");
    }

    public void killAddCreditScreen() {
        posCredGUI.kill();
        gui.loadKeyManager(this);
        gui.enableUI();
        gui.setStatus("Student Credit Screen closed.");
    }

    private boolean doStudentCreditUpdate(Double dblCAmnt, String stuID) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Boolean blOk = false;
        String strMessage = "";
        try {
            conn = dbMan.getPOSConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            String host = getHostName();
            String stuId = student.getStudentNumber();
            String building = settings.get(DBSettings.MAIN_BUILDING);
            String cashier = dbMan.getPOSUser();
            if (hasStudentCredit()) {
                stmt = conn.createStatement();
                if (stmt.executeUpdate("UPDATE " + strPOSPrefix + "studentcredit set credit_amount = credit_amount + " + round2Places(dblCAmnt) + " WHERE credit_active = '1' and credit_studentid = '" + stuId + "'") == 1) {
                    stmt.close();
                    stmt = conn.createStatement();
                    if (stmt.executeUpdate("UPDATE " + strPOSPrefix + "studentcredit set credit_lastused = NOW() where credit_active = '1' and credit_studentid = '" + stuId + "'") == 1) {
                        stmt.close();
                        stmt = conn.createStatement();
                        if (stmt.executeUpdate("INSERT into " + strPOSPrefix + "studentcredit_log ( scl_studentid, scl_action, scl_datetime ) values( '" + stuId + "', '" + round2Places(dblCAmnt) + "', NOW() )") == 1) {
                            stmt.close();
                            blOk = true;
                        } else {
                            strMessage = "Unable to update student credit log.";
                            blOk = false;
                        }
                    } else {
                        strMessage = "Unable to update student credit account.";
                        blOk = false;
                    }
                } else {
                    strMessage = "Unable to update student credit account.";
                    blOk = false;
                }
            } else {
                stmt = conn.createStatement();
                if (stmt.executeUpdate("insert into " + strPOSPrefix + "studentcredit (credit_amount,credit_active,credit_studentid,credit_lastused) values('" + round2Places(dblCAmnt) + "','1','" + stuId + "', NOW())") == 1) {
                    stmt.close();
                    stmt = conn.createStatement();
                    if (stmt.executeUpdate("insert into " + strPOSPrefix + "studentcredit_log ( scl_studentid, scl_action, scl_datetime ) values( '" + stuId + "', '" + round2Places(dblCAmnt) + "', NOW() )") == 1) {
                        stmt.close();
                        blOk = true;
                    } else {
                        strMessage = "Unable to update student credit log.";
                        blOk = false;
                    }
                } else {
                    strMessage = "Unable to create new student credit account.";
                    blOk = false;
                }
            }
            if (blOk) {
                stmt = conn.createStatement();
                if (stmt.executeUpdate("insert into " + strPOSPrefix + "creditTrans ( ctStudentNumber, ctCreditAction, ctBuilding, ctRegister, ctUser, ctDateTime ) values( '" + stuId + "', '" + round2Places(dblCAmnt) + "', '" + building + "', '" + host + "', '" + cashier + "', NOW() )") == 1) {
                    stmt.close();
                    blOk = true;
                } else blOk = false;
            }
            if (blOk) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                throw new Exception("Error detected during credit adjustment!  " + strMessage);
            }
        } catch (Exception exp) {
            try {
                conn.rollback();
            } catch (SQLException sqlEx2) {
                System.err.println("Rollback failed: " + sqlEx2.getMessage());
                return false;
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                        rs = null;
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) {
                            stmt = null;
                        } catch (Exception e) {
                            System.err.println("Exception: " + e.getMessage());
                            System.err.println(e);
                        }
                    }
                }
            }
            exp.printStackTrace();
            throw new Exception("Error detected during credit adjustment: " + exp.getMessage());
        }
    }

    public void processStudentCreditAdd(String strAmount) {
        killAddCreditScreen();
        try {
            if (strAmount != null && strAmount.length() > 0) {
                double dblCAmnt = Double.parseDouble(strAmount);
                if (dblCAmnt > 0d) {
                    if (dblCAmnt <= 1000d) {
                        String stuId = student.getStudentNumber();
                        try {
                            if (doStudentCreditUpdate(dblCAmnt, stuId)) {
                                gui.setStatus("Order Complete.");
                                gui.disableUI();
                                summary = new PSOrderSummary(gui);
                                if (cashDrawer != null) cashDrawer.openDrawer(); else summary.setPOSEventListener(this);
                                summary.display(money.format(round2Places(dblCAmnt)), money.format(round2Places(dblCAmnt)), money.format(0d), money.format(0d), money.format(getStudentCredit()));
                            } else {
                                gui.setStatus("Failed to update student credit account!", true);
                            }
                        } catch (Exception e) {
                            gui.setStatus("Error: " + e.getMessage(), true);
                        }
                    } else gui.setStatus(dblCAmnt + " is not allowed.  Credit adjustments are limited to $1,000.00.", true);
                } else gui.setStatus(dblCAmnt + " is not valid.  Credit amount must be greater than zero.", true);
            }
        } catch (NumberFormatException nfe) {
            gui.setStatus(strAmount + " is not a valid credit amount!  No credit adjustments were made.", true);
        }
    }

    public void loadAddCreditScreen() {
        gui.setStatus("Loading Student Credit Screen...");
        if (intMode == MODE_ITEMS) {
            if (student != null) {
                if (!student.isAnonStudent()) {
                    if (student.getIsActiveStudent()) {
                        gui.removeKeyManager();
                        gui.disableUI();
                        posCredGUI = new POSCreditGUI(gui, student.getStudentNumber(), student.getStudentName());
                        posCredGUI.addPOSEventListener(this);
                        posCredGUI.display();
                    } else gui.setStatus("You can only add credit to an active student.", true);
                } else gui.setStatus("Credit can NOT be applied to an anonymous student account.", true);
            } else gui.setStatus("A student must be loaded to add student credit to their account.", true);
        } else gui.setStatus("This can only be done in item mode.", true);
    }

    public void POSEventOccurred(POSEvent posEvent) {
        if (posEvent.getEventType() == POSEvent.SYSTEM_LOGIN) {
            attemptLogin();
        } else if (posEvent.getEventType() == POSEvent.SYSTEM_EXIT) {
            exitSystem();
        } else if (posEvent.getEventType() == POSEvent.ITEMS_LOADCAT) {
            gui.setStatus("Searching for categories...");
            loadCatsToGUI();
            gui.setStatus("Catgories loaded.");
        } else if (posEvent.getEventType() == POSEvent.ITEMS_LOADITEMS) {
            POSButton b = (POSButton) posEvent.getSource();
            gui.setStatus("Searching for items...");
            loadItemsToGUI((String) b.getObjectRef());
            gui.setStatus("items loaded.");
        } else if (posEvent.getEventType() == POSEvent.ITEMS_ADDITEM) {
            POSButton b = (POSButton) posEvent.getSource();
            processItemAdd(b.getObjectRef());
        } else if (posEvent.getEventType() == POSEvent.DRAWER_OPENED) {
            drawerOpened();
        } else if (posEvent.getEventType() == POSEvent.DRAWER_CLOSED) {
            drawerClosed();
        } else if (posEvent.getEventType() == POSEvent.NO_SALE) {
            if (intMode == MODE_ITEMS) {
                if (order != null || order.getNumberOfItems() == 0) {
                    processNoSale();
                } else {
                    gui.setStatus("Can not open drawer while items exist in the order.", true);
                }
            } else {
                gui.setStatus("No sale can only be done in Item Mode.", true);
            }
        } else if (posEvent.getEventType() == POSEvent.ORDER_VOIDLAST) {
            if (intMode == MODE_ITEMS) voidLastItem(); else if (intMode == MODE_CHECKOUT) {
                returnToItemsScreen();
                gui.toggleButtonText();
                voidLastItem();
            } else gui.setStatus("Voiding can't be done in this mode.", true);
        } else if (posEvent.getEventType() == POSEvent.ORDER_VOIDORDER) {
            if (intMode == MODE_ITEMS) voidOrder(); else if (intMode == MODE_CHECKOUT) {
                returnToItemsScreen();
                gui.toggleButtonText();
                voidOrder();
            } else gui.setStatus("Voiding can't be done in this mode.", true);
        } else if (posEvent.getEventType() == POSEvent.STUDENT_RESET) {
            if (intMode == MODE_ITEMS) {
                resetAnonStudent();
            } else gui.setStatus("Changing students can only be done in Item Mode", true);
        } else if (posEvent.getEventType() == POSEvent.ITEMS_TOGGLECHECKOUT) {
            if (intMode == MODE_ITEMS) loadCheckOutScreen(); else if (intMode == MODE_CHECKOUT) returnToItemsScreen();
            gui.toggleButtonText();
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_ADDCASH) {
            try {
                if (posEvent.getSource() != null) {
                    POSKeyPad key = (POSKeyPad) posEvent.getSource();
                    updateCashBuffer(key.getValueDouble());
                    key.reset();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_CLEARCASH) {
            clearCash();
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_CLEARCREDIT) {
            clearCredit();
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_ADDCREDIT) {
            try {
                if (posEvent.getSource() != null) {
                    POSKeyPad key = (POSKeyPad) posEvent.getSource();
                    updateCreditBuffer(key.getValueDouble());
                    key.reset();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_PROCESS) {
            processOrder();
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_SUMMARYSCREENCLOSED) {
            drawerClosed();
        } else if (posEvent.getEventType() == POSEvent.IMAGE_LOADED) {
            if (posEvent.getSource() != null) pushStudentImage((ImageIcon) posEvent.getSource());
        } else if (posEvent.getEventType() == POSEvent.IMAGE_LOAD_FAILED) {
            System.err.println("Image load failed!");
        } else if (posEvent.getEventType() == POSEvent.ITEMS_MANUALENTRY) {
            if (intMode == MODE_ITEMS) loadKeyPad(); else gui.setStatus("Changing students can only be done in Item Mode", true);
        } else if (posEvent.getEventType() == POSEvent.ITEMS_ACTIVATELASTORDER) {
            if (intMode == MODE_ITEMS) activateLastStudentOrder(); else gui.setStatus("Activating a previous order can only be done in Item Mode", true);
        } else if (posEvent.getEventType() == POSEvent.ITEMS_MANUALENTRYSUBMIT) {
            try {
                if (posEvent.getSource() != null) {
                    PSEntryPad key = (PSEntryPad) posEvent.getSource();
                    processStudentScan(key.getKeyPad().getValueString());
                    key.kill();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        } else if (posEvent.getEventType() == POSEvent.ORDER_VOIDITEM) {
            try {
                if (intMode == MODE_ITEMS) {
                    if (posEvent.getSource() != null) {
                        OrderItemPanel itemPnl = (OrderItemPanel) posEvent.getSource();
                        if (itemPnl != null) {
                            if (itemPnl.isTapped()) {
                                OrderItem item = (OrderItem) (itemPnl.getObjectRef());
                                voidItem(item);
                            } else {
                                itemPnl.tap();
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        } else if (posEvent.getEventType() == POSEvent.ITEMS_ADDCREDIT) {
            loadAddCreditScreen();
        } else if (posEvent.getEventType() == POSEvent.CREDIT_CANCEL) {
            killAddCreditScreen();
        } else if (posEvent.getEventType() == POSEvent.CREDIT_PROCESS) {
            try {
                if (posEvent.getSource() != null && posEvent.getSource() instanceof POSCreditGUI) {
                    POSCreditGUI cGUI = (POSCreditGUI) posEvent.getSource();
                    processStudentCreditAdd(cGUI.getAmount());
                }
            } catch (Exception exp) {
                System.err.println(exp.getMessage());
            }
        } else if (posEvent.getEventType() == POSEvent.CHECKOUT_CHANGEDEPOSITTOGGLE) {
            if (intMode == MODE_CHECKOUT) {
                if (student != null) {
                    if (student.getIsActiveStudent()) {
                        blDepositCredit = !blDepositCredit;
                        updateChangeCreditDeposit();
                    } else gui.setStatus("Credit adjustments can not be made for inactive students.", true);
                }
            }
        }
    }
}
