package gov.sns.apps.mpsinputtest;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.swing.*;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Provides the main window for the application.
 */
public class MPSFrame extends JeriInternalFrame {

    /**
   * Holds the properties of the application.
   */
    private Properties applicationProperties;

    private String propertyFileName;

    public boolean propertyFileInDatabase;

    private ArrayList roles;

    private static OracleDataSource dataSource;

    private MPSFrame mainWindow;

    public static CachingDatabaseAdaptor databaseAdaptor;

    /**
   * Holds the query used to retrieve the data contained in the model.
   */
    private PreparedStatement dataQuery;

    /**
   * Holds the query used to retrieve the number of rows contained in the model.
   */
    private PreparedStatement rowCountQuery;

    /**
   * Holds the query used to find the primary and foreign keys for the table 
   * being displayed.
   */
    private PreparedStatement mpsQuery;

    /**
   * Holds the names of the columns in the database table being displayed. This 
   * is populated when the data is retrieved.
   */
    private ArrayList columnNames = new ArrayList();

    /**
   * Holds the name of the primary keys of the database table being displayed. 
   * This is set when the key data is retrieved. The primary key is not 
   * editable.
   */
    private ArrayList primaryKeys = new ArrayList();

    /**
   * Holds the names of the foreign keys of the database table being displayed. 
   * This is set when the key data is retrieved. The primary key is editable on 
   * inserted rows if isn't is a foreign key.
   */
    private ArrayList foreignKeys = new ArrayList();

    public static OracleDataSource activeConnectionPool;

    public static DataSource connectionPool;

    public static Connection OracleConnection;

    public static LoginDialog login;

    private String[] headings = new String[] { "Signal ID", "Mode Mask" };

    public static char[][] CCL_BSmm = new char[500][35];

    public static String[] CCL_BSSignalID = new String[500];

    public static char[][] MEBT_BSmm = new char[500][35];

    public static String[] MEBT_BSSignalID = new String[500];

    public static char[][] LDmpmm = new char[500][35];

    public static String[] LDmpSignalID = new String[500];

    public static char[][] IDmpmm = new char[500][35];

    public static String[] IDmpSignalID = new String[500];

    public static char[][] EDmpmm = new char[500][35];

    public static String[] EDmpSignalID = new String[500];

    public static char[][] Ringmm = new char[500][35];

    public static String[] RingSignalID = new String[500];

    public static char[][] Tgtmm = new char[500][35];

    public static String[] TgtSignalID = new String[500];

    public static Map IOCdevIDS_Map, deviceIDS_Map, subsystemIDS_Map, channelNoIDS_Map;

    public static Map isTested_Map, PFstatus_Map;

    /**
   * Creates a new <CODE>MainFrame</CODE>.
   */
    public MPSFrame() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Component initialization.
   *
   * @throws java.lang.Exception Thrown on error.
   */
    private void jbInit() throws Exception {
        connectionPool = getDataSource();
        this.setDataSource(connectionPool);
        this.addComponentListener(new java.awt.event.ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                this_componentResized(e);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                this_componentMoved(e);
            }
        });
    }

    /**
   * Called when the application is closed by clicking on the X button in the 
   * title bar. This method calls <CODE>exitApplication</CODE>.
   * 
   * @param e The <CODE>WindowEvent</CODE> that caused the invocation of this method.
   */
    void this_windowClosing(WindowEvent e) {
        exitApplication();
    }

    /**
   * Called when the window is moved. This method records the new location in 
   * the application settings, which are saved when the application exits.
   *
   * @param e The <CODE>ComponentEvent</CODE> that caused the invocation of this method.
   */
    void this_componentMoved(ComponentEvent e) {
        Point newLocation = getLocation();
        Properties settings = getApplicationProperties();
        if (settings != null) {
            settings.setProperty("mainWindow.x", String.valueOf(newLocation.x));
            settings.setProperty("mainWindow.y", String.valueOf(newLocation.y));
        }
    }

    /**
   * Exits the application. This method stores all of the applications settings 
   * in the property file and exits.
   */
    public void exitApplication() {
        System.exit(0);
    }

    /**
   * Called when the window is resized. This method records the new size in the 
   * application settings, which are saved when the application exits.
   *
   * @param e The <CODE>ComponentEvent</CODE> that caused the invocation of this method.
   */
    @Override
    void this_componentResized(ComponentEvent e) {
        Dimension newSize = getSize();
        Properties settings = getApplicationProperties();
        if (settings != null) {
            settings.setProperty("mainWindow.width", String.valueOf(newSize.width));
            settings.setProperty("mainWindow.height", String.valueOf(newSize.height));
        }
    }

    /**
   * Gets the properties stored in the applications properties file.
   *
   * @return The settings for the application.
   */
    @Override
    public Properties getApplicationProperties() {
        return applicationProperties;
    }

    public Map retrieveMM(String MachMode, Connection oracleConnection) throws java.sql.SQLException {
        Map signalIDS_mmMap = new HashMap();
        final String sql = "{ ? = call epics.epics_mps_pkg.mps_signals_to_audit(?) }";
        final CallableStatement procedure = oracleConnection.prepareCall(sql);
        procedure.registerOutParameter(1, Types.ARRAY, "EPICS.MPS_MODE_MASK_TAB");
        procedure.setString(2, MachMode);
        procedure.execute();
        Object[] array = (Object[]) procedure.getArray(1).getArray();
        int sid = 0, mmID = 0;
        for (int index = 0; index < array.length; index++) {
            final Struct element = (Struct) array[index];
            final Object[] attributes = element.getAttributes();
            if (attributes.length > 1) {
                Object signalID = attributes[0];
                Object signalMM = attributes[1];
                if (signalID != null && signalMM != null) {
                    try {
                        String SignalID = signalID.toString();
                        String SignalMM = signalMM.toString();
                        signalIDS_mmMap.put(SignalID, SignalMM);
                        if (MachMode.equals("CCL_BS")) {
                            CCL_BSSignalID[sid] = SignalID;
                            CCL_BSmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("MEBT_BS")) {
                            MEBT_BSSignalID[sid] = SignalID;
                            MEBT_BSmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("LDmp")) {
                            LDmpSignalID[sid] = SignalID;
                            LDmpmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("Ring")) {
                            RingSignalID[sid] = SignalID;
                            Ringmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("Tgt")) {
                            TgtSignalID[sid] = SignalID;
                            Tgtmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("EDmp")) {
                            EDmpSignalID[sid] = SignalID;
                            EDmpmm[sid] = SignalMM.toCharArray();
                        } else if (MachMode.equals("IDmp")) {
                            IDmpSignalID[sid] = SignalID;
                            IDmpmm[sid] = SignalMM.toCharArray();
                        }
                        sid++;
                    } catch (Exception exception) {
                        final String message = index + " Exception generating input info for MPS signal: " + signalID.toString() + " " + signalMM.toString();
                        exception.printStackTrace();
                    }
                }
            }
        }
        procedure.close();
        return signalIDS_mmMap;
    }

    public Map reloadIOC() {
        return IOCdevIDS_Map;
    }

    public Map getDeviceMap() {
        return deviceIDS_Map;
    }

    public Map getSubSystemMap() {
        return subsystemIDS_Map;
    }

    public Map getChannelNoMap() {
        return channelNoIDS_Map;
    }

    public Map getIsTested_Map() {
        return isTested_Map;
    }

    public Map getPFstatus_Map() {
        return PFstatus_Map;
    }

    public void putPFstatus_Map(Map pfStatusMap) {
        PFstatus_Map = pfStatusMap;
    }

    public void putIsTested_Map(Map isTestedMap) {
        isTested_Map = isTestedMap;
    }

    public Map loadDefaults(Connection oracleConnection, String MachMode, int isNew) throws java.sql.SQLException {
        if (isNew == 1) {
            subsystemIDS_Map = new HashMap();
            IOCdevIDS_Map = new HashMap();
            deviceIDS_Map = new HashMap();
            channelNoIDS_Map = new HashMap();
            isTested_Map = new HashMap();
            PFstatus_Map = new HashMap();
        }
        StringBuffer sql = new StringBuffer("SELECT MACHINE_MODE.MPS_DVC_ID, MACHINE_MODE.MPS_DVC_ID||':'||");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM,DVC.DVC_TYPE_ID, DVC.SUBSYS_ID, DVC.SYS_ID, ");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM, MPS_SGNL_PARAM.DVC_ID, ");
        sql.append("MPS_SGNL_PARAM.IOC_DVC_ID, MACHINE_MODE.CHANNEL_NBR, MPS_SGNL_PARAM.MPS_CHAIN_ID ");
        sql.append("FROM EPICS.DVC, EPICS.MACHINE_MODE, EPICS.MPS_SGNL_PARAM, EPICS.MACHINE_MODE_DEF_MASK ");
        sql.append("WHERE ((MPS_SGNL_PARAM.DVC_ID =  MACHINE_MODE.DVC_ID) AND (");
        sql.append("MPS_SGNL_PARAM.APPR_DTE = MACHINE_MODE.APPR_DTE) AND (MACHINE_MODE.MPS_DVC_ID = ");
        if (MachMode.equals("All Chains")) {
            sql.append("DVC.DVC_ID)");
            sql.append(") ORDER BY DVC.DVC_TYPE_ID");
        } else {
            sql.append("DVC.DVC_ID) AND MPS_SGNL_PARAM.MPS_CHAIN_ID = '");
            sql.append(MachMode);
            sql.append("') ORDER BY DVC.DVC_TYPE_ID");
        }
        PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
        ResultSet result = null;
        try {
            result = query.executeQuery();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (result != null) {
            try {
                while (result.next()) {
                    subsystemIDS_Map.put(result.getObject(1), result.getObject(4));
                    deviceIDS_Map.put(result.getObject(1), result.getObject(3));
                    IOCdevIDS_Map.put(result.getObject(1), result.getObject(8));
                    channelNoIDS_Map.put(result.getObject(1), result.getObject(9));
                    isTested_Map.put(result.getObject(1), "");
                    PFstatus_Map.put(result.getObject(1), "");
                }
            } finally {
                result.close();
                query.close();
            }
        }
        return subsystemIDS_Map;
    }

    public Map loadIsTested(Connection oracleConnection, Map pfStatusMap) throws java.sql.SQLException {
        if (pfStatusMap == null) pfStatusMap = getPFstatus_Map();
        if (pfStatusMap.size() < isTested_Map.size()) {
            Iterator keyValue = isTested_Map.keySet().iterator();
            Object k1;
            while (keyValue.hasNext()) {
                k1 = keyValue.next();
                if (!pfStatusMap.containsKey(k1)) pfStatusMap.put(k1, "");
            }
            putPFstatus_Map(pfStatusMap);
        }
        Iterator m1 = isTested_Map.entrySet().iterator();
        Iterator m2 = pfStatusMap.entrySet().iterator();
        Map.Entry p1, p2;
        while (m1.hasNext()) {
            p1 = (Map.Entry) m1.next();
            p2 = (Map.Entry) m2.next();
            StringBuffer sql = new StringBuffer("SELECT to_date(AUDIT_DTE,'dd-mon-yy'), PASS_IND ");
            sql.append("FROM epics.MPS_CHAN_AUDIT ");
            sql.append("WHERE (MPS_DVC_ID = '" + p1.getKey() + "')");
            String p1Key = (String) p1.getKey();
            if (p1Key.indexOf("_Mag:PS_DC") == -1 && p1Key.indexOf("_DCH") == -1 && p1Key.indexOf("_Mag:DCH") == -1) {
                PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
                ResultSet result = null;
                try {
                    result = query.executeQuery();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
                if (result != null) {
                    try {
                        if (result.next()) {
                            p1.setValue(result.getObject(1));
                            p2.setValue(result.getObject(2));
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, sql.toString(), "Not Found.", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        result.close();
                        query.close();
                    }
                }
            }
        }
        putPFstatus_Map(pfStatusMap);
        return isTested_Map;
    }

    public Map loadDevices(Connection oracleConnection, String MachMode, String DeviceSel, int isNew) throws java.sql.SQLException {
        if (isNew == 1) {
            subsystemIDS_Map = new HashMap();
            IOCdevIDS_Map = new HashMap();
            deviceIDS_Map = new HashMap();
            channelNoIDS_Map = new HashMap();
            isTested_Map = new HashMap();
            PFstatus_Map = new HashMap();
        }
        StringBuffer sql = new StringBuffer("SELECT MACHINE_MODE.MPS_DVC_ID, MACHINE_MODE.MPS_DVC_ID||':'||");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM,DVC.DVC_TYPE_ID, DVC.SUBSYS_ID, DVC.SYS_ID, ");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM, MPS_SGNL_PARAM.DVC_ID, ");
        sql.append("MPS_SGNL_PARAM.IOC_DVC_ID, MACHINE_MODE.CHANNEL_NBR, MPS_SGNL_PARAM.MPS_CHAIN_ID ");
        sql.append("FROM EPICS.DVC, EPICS.MACHINE_MODE, EPICS.MPS_SGNL_PARAM, EPICS.MACHINE_MODE_DEF_MASK ");
        sql.append("WHERE ((MPS_SGNL_PARAM.DVC_ID =  MACHINE_MODE.DVC_ID) AND (");
        sql.append("MPS_SGNL_PARAM.APPR_DTE = MACHINE_MODE.APPR_DTE) AND (MACHINE_MODE.MPS_DVC_ID = ");
        if (MachMode.equals("All Chains")) {
            sql.append("DVC.DVC_ID) AND DVC.DVC_TYPE_ID = '");
            sql.append(DeviceSel);
            sql.append("') ORDER BY DVC.DVC_TYPE_ID");
        } else {
            sql.append("DVC.DVC_ID) AND MPS_SGNL_PARAM.MPS_CHAIN_ID = '");
            sql.append(MachMode);
            sql.append("' AND DVC.DVC_TYPE_ID = '");
            sql.append(DeviceSel);
            sql.append("') ORDER BY DVC.DVC_TYPE_ID");
        }
        PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
        ResultSet result = null;
        try {
            result = query.executeQuery();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (result != null) {
            try {
                while (result.next()) {
                    subsystemIDS_Map.put(result.getObject(1), result.getObject(4));
                    deviceIDS_Map.put(result.getObject(1), result.getObject(3));
                    IOCdevIDS_Map.put(result.getObject(1), result.getObject(8));
                    channelNoIDS_Map.put(result.getObject(1), result.getObject(9));
                    isTested_Map.put(result.getObject(1), "");
                    PFstatus_Map.put(result.getObject(1), "");
                }
            } finally {
                result.close();
                query.close();
            }
        }
        return subsystemIDS_Map;
    }

    public Map loadSubSystems(Connection oracleConnection, String MachMode, String SubSysSel, int isNew) throws java.sql.SQLException {
        if (isNew == 1) {
            subsystemIDS_Map = new HashMap();
            IOCdevIDS_Map = new HashMap();
            deviceIDS_Map = new HashMap();
            channelNoIDS_Map = new HashMap();
            isTested_Map = new HashMap();
            PFstatus_Map = new HashMap();
        }
        StringBuffer sql = new StringBuffer("SELECT MACHINE_MODE.MPS_DVC_ID, MACHINE_MODE.MPS_DVC_ID||':'||");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM,DVC.DVC_TYPE_ID, DVC.SUBSYS_ID, DVC.SYS_ID, ");
        sql.append("MACHINE_MODE_DEF_MASK.DVC_TYPE_SGNL_NM, MPS_SGNL_PARAM.DVC_ID, ");
        sql.append("MPS_SGNL_PARAM.IOC_DVC_ID, MACHINE_MODE.CHANNEL_NBR, MPS_SGNL_PARAM.MPS_CHAIN_ID ");
        sql.append("FROM EPICS.DVC, EPICS.MACHINE_MODE, EPICS.MPS_SGNL_PARAM, EPICS.MACHINE_MODE_DEF_MASK ");
        sql.append("WHERE ((MPS_SGNL_PARAM.DVC_ID =  MACHINE_MODE.DVC_ID) AND (");
        sql.append("MPS_SGNL_PARAM.APPR_DTE = MACHINE_MODE.APPR_DTE) AND (MACHINE_MODE.MPS_DVC_ID = ");
        if (MachMode.equals("All Chains")) {
            sql.append("DVC.DVC_ID) AND DVC.SUBSYS_ID = '");
            sql.append(SubSysSel);
            sql.append("') ORDER BY DVC.DVC_TYPE_ID");
        } else {
            sql.append("DVC.DVC_ID) AND (MPS_SGNL_PARAM.MPS_CHAIN_ID = '");
            sql.append(MachMode);
            sql.append("') AND DVC.SUBSYS_ID = '");
            sql.append(SubSysSel);
            sql.append("') ORDER BY DVC.DVC_TYPE_ID");
        }
        PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
        ResultSet result = null;
        try {
            result = query.executeQuery();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (result != null) {
            try {
                while (result.next()) {
                    subsystemIDS_Map.put(result.getObject(1), result.getObject(4));
                    deviceIDS_Map.put(result.getObject(1), result.getObject(3));
                    IOCdevIDS_Map.put(result.getObject(1), result.getObject(8));
                    channelNoIDS_Map.put(result.getObject(1), result.getObject(9));
                    isTested_Map.put(result.getObject(1), "");
                    PFstatus_Map.put(result.getObject(1), "");
                }
            } finally {
                result.close();
                query.close();
            }
        }
        return subsystemIDS_Map;
    }

    /**
   * Sets the properties stored in the applications properties file. These are 
   * read in before the user logs in by the main application class and passed to 
   * this class as an instance of <CODE>Properties</CODE>.
   * 
   * @param applicationProperties The applicationProperties application settings.
   */
    @Override
    public void setApplicationProperties(Properties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
   * Sets the name of the file the application settings are to be saved in. When
   * the application exits, the application settings are stored in this file.
   *
   * @param propertyFileName The name of the file to save the applications settings in.
   */
    public void setPropertyFileName(String propertyFileName) {
        this.propertyFileName = propertyFileName;
    }

    /**
   * Gets the name of the file that the application settings will be saved in.
   * The settings are saved by this class when the application exits.
   *
   * @return The name of the file that the application will save it's settings in.
   */
    public String getPropertyFileName() {
        return propertyFileName;
    }

    /**
   * Gets the value of the property file in database flag. This flag determines
   * the type of statement used to save the property file.
   *
   * @return The name of the file that the application will save it's settings in.
   */
    public boolean PropertyFileInDatabase() {
        return propertyFileInDatabase;
    }

    /**
   * Sets the value of the property file in database flag. This flag determines
   * the type of statement used to save the property file.
   * 
   * @param propertyFileInDatabase Pass as <CODE>true</CODE> if the property file is in the database, <CODE>false</CODE> if not.
   */
    public void setPropertyFileInDatabase(boolean propertyFileInDatabase) {
        this.propertyFileInDatabase = propertyFileInDatabase;
    }

    /**
   * Sets the database roles for the user.
   * 
   * @param roles The roles the user has in the database.
   */
    public void setRoles(ArrayList roles) {
        this.roles = roles;
    }

    public void setConnection(Connection oracleConnection, OracleDataSource dataSource) throws java.sql.SQLException {
        OracleConnection = oracleConnection;
        setDataSource(dataSource);
    }

    public Connection getConnection() {
        return OracleConnection;
    }

    public DataSource getConnectionPool() {
        return connectionPool;
    }

    /**
   * Allows the user to interact directly with the JDBC data source.
   * 
   * @return The <CODE>DataSource</CODE> for the guven URL.
   */
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(OracleDataSource connectionPool) {
        dataSource = connectionPool;
    }

    public void setLogin(LoginDialog mainLogin) {
        login = mainLogin;
    }

    public static LoginDialog getLogin() {
        return login;
    }

    public void setDatabaseAdaptor(CachingDatabaseAdaptor CacheDataAdaptor) {
        databaseAdaptor = CacheDataAdaptor;
    }

    public static CachingDatabaseAdaptor getDatabaseAdaptor() {
        return databaseAdaptor;
    }

    /**
   * Runs the given query and returns the integer value returned by it. The
   * query needs to return one integer as a result.
   * 
   * @param countQuery The query that gets a record count.
   * @return The integer returned by the query.
   * @throws java.sql.SQLException Thrown on SQL error.
   */
    protected int runCountQuery(PreparedStatement countQuery) throws java.sql.SQLException {
        ResultSet result = null;
        try {
            result = countQuery.executeQuery();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (result != null) {
            try {
                result.next();
                return result.getInt(1);
            } finally {
                result.close();
            }
        }
        return -1;
    }
}
