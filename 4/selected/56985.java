package common;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Iterator;
import java.util.logging.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import org.dbunit.*;

public class Util {

    public static final int GROUP_ITEM_VISIBLE = 1;

    public static final int SLIDER_VAL_POSITION = 1;

    public static final int TEXT_VAL_POSITION = 1;

    public static final int TEXT_COL_WHEN_NOT_NULL = 2;

    public static final int TEXT_BORDER_COL_WHEN_NOT_NULL = 3;

    public static final int TEXT_FILL_COL_WHEN_NOT_NULL = 4;

    public static final int OVAL_FILL_COL_WHEN_NOT_NULL = 1;

    public static final int OVAL_FILL_COL_WHEN_NULL = 2;

    public static final int OVAL_BORDER_COL_WHEN_NOT_NULL = 3;

    public static final int OVAL_BORDER_COL_WHEN_NULL = 4;

    public static final int OVAL_VISIBLE = 5;

    public static final int OVAL_BLINK = 6;

    public static final int ICON_BLINK = 1;

    public static final int ICON_VISIBLE = 2;

    public static final int ICON_BORDER_COL_WHEN_NOT_NULL = 3;

    public static final int ICON_BORDER_COL_WHEN_NULL = 4;

    public static final int ARROWLINE_FILL_COL_WHEN_NOT_NULL = 1;

    public static final int LINE_BORDER_COL_WHEN_NOT_NULL = 1;

    public static final int POLYGON_FILL_COL_WHEN_NOT_NULL = 1;

    public static final Dimension buttonIconDim = new Dimension(27, 24);

    public static String[] intParNames = { "SHAPECOLOR", "BORDERCOLOR", "GRIDCOLOR", "FILLCOLOR", "TEXTCOLOR", "SELECTEDTEXTCOLOR", "SELECTEDBACKCOLOR", "FOCUSTEXTCOLOR", "FOCUSBACKCOLOR", "TEXTHALIGNMENT", "TEXTVALIGNMENT", "TEXTWIDTH", "TEXTHEIGHT", "FONTSTYLE", "FONTSIZE", "FONTCOLOR", "COLUMNWIDTH", "BORDERWIDTH", "MAXFRACDIGIT", "MINFRACDIGIT", "AUTORESIZEMODE", "OVERLAY", "HISTORYCOUNT", "POLLINGMSEC", "BCKMSEC", "ARROWIDTH", "ARROWLENGTH", "LINETHICK", "CHARTTITLEFONTSTYLE", "CHARTTITLEFONTSIZE", "CHARTTITLECOLOR", "NTICKS", "ROWHEIGHT", "ROWMARGIN", "ISPW", "ISPH", "BODDTABROWRULE", "BEVENTABROWRULE", "FODDTABROWRULE", "FEVENTABROWRULE", "PLOTCOLOR", "XGRCOLOR", "YGRCOLOR" };

    public static String[] intArrayParNames = { "TABBGCOLCOLOR", "TABFGCOLCOLOR", "TABCOLALIGNMENTS", "CHARTCURVECOLOR" };

    public static String[] stringArrayParNames = { "CHARTCURVENAME", "CHARTCURVESTYLE", "CHARTCURVESHAPE" };

    public static String[] floatArrayParNames = { "CHARTCURVEWIDTH" };

    public static String[] boolParNames = { "DISPLAYHEADER", "DISPLAYROWNUM", "DISPLAYSHAPES", "BLINKING", "OPAQUE", "SHOWHGRID", "SHOWVGRID", "BACKGROUND", "SHOWBORDER", "SHOWSCALEUP", "SHOWSUBSCALEUP", "SHOWVALUP", "SHOWSCALEDOWN", "SHOWSUBSCALEDOWN", "SHOWVALDOWN", "ENABLED", "POLLING", "DRAWARROW", "FILLARROW", "SPECIFYTIMESTAMP", "AUTOSCALE", "3D", "SHOWLEGEND", "VORIENTATION", "XGRID", "YGRID" };

    public static String[] stringParNames = { "FONTNAME", "SQLQUERY", "VALUE", "DEFAULTIMAGEPATH", "CHARTBCKIMG", "FORMATTER", "CHARTTITLE", "YRLABEL", "XRLABEL", "CHARTTITLEFONTNAME", "LINESTYLE", "SLIDERTYPE" };

    public static String[] doubleParNames = { "MAXSLIDER", "MINSLIDER", "MINYRANGE", "MAXYRANGE", "ROTATIONANGLE" };

    public static String[] floatParNames = { "ALPHA" };

    private static boolean logEnabled = false;

    public Util() {
    }

    public static boolean isLoggingEnabled() {
        return logEnabled;
    }

    public static void preLoadImage(String path, String selectedValue, MemDb memDb) {
        dropMemDb();
        createMemDb();
        try {
            MemDb.getInstance().importFromXml(path, MemDb.getInstance().getConnection());
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (DatabaseUnitException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Elimina il DB in memoria
   */
    public static void dropMemDb() {
        try {
            MemDb.getInstance().update("DROP TABLE DBCONNECTION IF EXISTS");
            MemDb.getInstance().update("DROP TABLE IMAGE IF EXISTS");
            MemDb.getInstance().update("DROP TABLE LINK_SHAPES IF EXISTS");
            MemDb.getInstance().update("DROP TABLE POINT IF EXISTS");
            MemDb.getInstance().update("DROP TABLE SHAPE IF EXISTS");
            MemDb.getInstance().update("DROP TABLE SHAPE_PARAMETERS IF EXISTS ");
            MemDb.getInstance().update("DROP TABLE LINK_SHAPEQUERY IF EXISTS");
            MemDb.getInstance().update("DROP TABLE SHAPEQUERY IF EXISTS");
            MemDb.getInstance().update("DROP TABLE SYSCALL IF EXISTS");
            MemDb.getInstance().conn.commit();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Crea il DB in memoria
   */
    public static void createMemDb() {
        try {
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_DBCONN);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_IMG);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_LNKSHAPES);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_POINT);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_SHAPE);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_SHAPE_PARAMETERS);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_LNK_SHQ);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_SHQUERY);
            MemDb.getInstance().update(IMemDb.DDL_CREATE_TAB_SYSCALL);
            MemDb.getInstance().update("CREATE INDEX DBCONNECTION_NAME_IDX ON DBCONNECTION (NAME)");
            MemDb.getInstance().update("CREATE INDEX POINT_IMAGE_NAME_IDX ON POINT (IMAGE_NAME)");
            MemDb.getInstance().update("CREATE INDEX POINT_ID_IDX ON POINT (POINT_ID)");
            MemDb.getInstance().update("CREATE INDEX POINT_SHAPE_ID_IDX ON POINT (SHAPE_ID)");
            MemDb.getInstance().update("CREATE INDEX LINK_SHAPEQUERY_QUERY_ID_IDX ON LINK_SHAPEQUERY (QUERY_ID)");
            MemDb.getInstance().update("CREATE INDEX LINK_SHAPEQUERY_IMAGE_NAME_IDX ON LINK_SHAPEQUERY (IMAGE_NAME)");
            MemDb.getInstance().update("CREATE INDEX LINK_SHAPEQUERY_SHAPE_ID_IDX ON LINK_SHAPEQUERY (SHAPE_ID)");
            MemDb.getInstance().update("CREATE INDEX SHAPEQUERY_ID_IDX ON SHAPEQUERY (ID)");
            MemDb.getInstance().update("CREATE INDEX SHAPEQUERY_IMAGE_NAME_IDX ON SHAPEQUERY (IMAGE_NAME)");
            MemDb.getInstance().update("CREATE INDEX SHAPEQUERY_SQLQUERY_IDX ON SHAPEQUERY (SQLQUERY)");
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    public static void initLink() {
        common.Util.createMemDb();
        try {
            MemDb.getInstance().update("CREATE USER root PASSWORD manager ADMIN");
            MemDb.getInstance().connect("root", "manager");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
   * Restituisce il valore della colonna data in input contenuta nel recordset dato in input
   * @param r il ResultSet in input
   * @param col il nome della colonna
   * @return l'oggetto Int risultante
   */
    public static String getInt(ResultSet r, String col) {
        try {
            String res = "" + r.getInt(col);
            return (r.wasNull() ? "NULL" : res);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
            return "NULL";
        }
    }

    /**
   * Restituisce il valore double della colonna data in input contenuta nel recordset dato in input
   * @param r il ResultSet in input
   * @param col il nome della colonna
   * @return l'oggetto Double risultante
   */
    public static String getDouble(ResultSet r, String col) {
        try {
            String res = "" + r.getDouble(col);
            return (r.wasNull() ? "NULL" : res);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
            return "NULL";
        }
    }

    /**
   * Restituisce il valore della colonna data in input contenuta nel recordset dato in input
   * @param r il ResultSet in input
   * @param col il nome della colonna
   * @return l'oggetto String risultante
   */
    public static String getString(ResultSet r, String col) {
        try {
            String res = r.getString(col);
            return (r.wasNull() ? "NULL" : "'" + res.replaceAll("'", "''") + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
            return "NULL";
        }
    }

    public static boolean existsTable(String tableName, MemDb memDb) {
        boolean res = false;
        String q = "SELECT count(*) FROM INFORMATION_SCHEMA.SYSTEM_TABLES " + "WHERE TABLE_NAME = '" + tableName + "'";
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery(q);
            while (rs.next()) {
                res = (rs.getInt(1) > 0 ? true : false);
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return res;
    }

    /**
   * Stampa in output il contenuto della tabella IMAGE
   */
    public static int dumpTableIMAGE(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM IMAGE");
            System.out.println("IMAGE:");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "NAME") + " - " + getInt(rs, "WIDTH") + " - " + getInt(rs, "HEIGHT") + " - " + getString(rs, "BACKGROUND") + " - " + getString(rs, "BCKIMG"));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return nRows;
    }

    /**
   * Stampa in output il contenuto della tabella DBCONNECTION
   */
    public static int dumpTableDBCONNECTION(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM DBCONNECTION");
            System.out.println("DBCONNECTION: ");
            System.out.println("ID  HOST  PORT  DBNAME  USERNAME  PASSWORD  NAME  IMAGE_NAME  POLL_MSEC");
            System.out.println("-----------------------------------------------------------------------");
            while (rs.next()) {
                nRows++;
                System.out.println(getInt(rs, IMemDb.FLD_DBCONN_ID) + " | " + getString(rs, IMemDb.FLD_DBCONN_HOST) + " | " + getInt(rs, IMemDb.FLD_DBCONN_PORT) + " | " + getString(rs, IMemDb.FLD_DBCONN_DBNAME) + " | " + getString(rs, IMemDb.FLD_DBCONN_USER) + " | " + getString(rs, IMemDb.FLD_DBCONN_PASSWD) + " | " + getString(rs, IMemDb.FLD_DBCONN_NAME) + " | " + getString(rs, IMemDb.FLD_DBCONN_IMG_NAME) + " | " + getInt(rs, IMemDb.FLD_DBCONN_PMSEC));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return nRows;
    }

    /**
   * Stampa in output il contenuto della tabella LINK_SHAPES
   */
    public static int dumpTableLINK_SHAPES(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM LINK_SHAPES");
            System.out.println("LINK_SHAPES:");
            System.out.println("ID_SH2 IMAGE_NAME_SH2 ID_SH1 IMAGE_NAME_SH1 LINE_NUMBER LINK_ACTION");
            System.out.println("-------------------------------------------------------------------");
            while (rs.next()) {
                nRows++;
                System.out.println(rs.getInt("ID_SH2") + "," + getString(rs, "IMAGE_NAME_SH2") + "," + getInt(rs, "ID_SH1") + "," + getString(rs, "IMAGE_NAME_SH1") + "," + getInt(rs, "LINE_NUMBER") + "," + getString(rs, "LINK_ACTION"));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return nRows;
    }

    /**
   * Stampa in output il contenuto della tabella POINT
   */
    public static int dumpTablePOINT(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM POINT");
            System.out.println("LINK_SHAPES:");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "IMAGE_NAME") + "," + getInt(rs, "SHAPE_ID") + "," + getInt(rs, "POINT_ID") + "," + getDouble(rs, "X") + "," + getDouble(rs, "Y"));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return nRows;
    }

    String FLD_SHAPE_IMG_NAME = "IMAGE_NAME";

    String FLD_SHAPE_ID = "ID";

    String FLD_SHAPE_TYPE = "TYPE";

    String FLD_SHAPE_FATHER_ID = "FATHER_SHAPE_ID";

    String FLD_SHAPE_NUMVERTEX = "NUMVERTEX";

    String FLD_SHAPE_REFRESHP = "REFRESHPERIOD";

    String FLD_SHAPE_DBCONN_ID = "DBCONNECTION_ID";

    String FLD_SHAPE_SEQ = "SEQ";

    /**
   * Stampa in output il contenuto della tabella SHAPE
   */
    public static int dumpTableSHAPE(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM SHAPE");
            System.out.println("SHAPE:");
            System.out.println(IMemDb.FLD_SHAPE_IMG_NAME + "   " + IMemDb.FLD_SHAPE_ID + "   " + IMemDb.FLD_SHAPE_TYPE + "   " + IMemDb.FLD_SHAPE_FATHER_ID + "   " + IMemDb.FLD_SHAPE_NUMVERTEX + "   " + IMemDb.FLD_SHAPE_REFRESHP + "   " + IMemDb.FLD_SHAPE_DBCONN_ID + "   " + IMemDb.FLD_SHAPE_SEQ);
            System.out.println("-------------------------------------------------------------------");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, IMemDb.FLD_SHAPE_IMG_NAME) + " | " + getInt(rs, IMemDb.FLD_SHAPE_ID) + " | " + getString(rs, IMemDb.FLD_SHAPE_TYPE) + " | " + getInt(rs, IMemDb.FLD_SHAPE_FATHER_ID) + " | " + getInt(rs, IMemDb.FLD_SHAPE_NUMVERTEX) + " | " + getInt(rs, IMemDb.FLD_SHAPE_REFRESHP) + " | " + getInt(rs, IMemDb.FLD_SHAPE_DBCONN_ID) + " | " + getInt(rs, IMemDb.FLD_SHAPE_SEQ));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
        return nRows;
    }

    public static Vector loadShapePoints(int id, String imgName, String type, int nVertex) {
        Vector res = new Vector();
        double[] xPnt = new double[nVertex];
        double[] yPnt = new double[nVertex];
        try {
            TreeMap<String, CPointsRecord> rows = CPointsTabDAO.getInstance().findByImgNameShId(imgName, id);
            Iterator<CPointsRecord> it = rows.values().iterator();
            int k = 0;
            while (it.hasNext()) {
                CPointsRecord r = it.next();
                xPnt[k] = r.getX();
                yPnt[k] = r.getY();
                k++;
            }
            res.addElement(xPnt);
            res.addElement(yPnt);
        } catch (CTabDAOException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public static void handleScale(Graphics2D g, int[] xcoord, int[] ycoord, double minVal, double maxVal, int nTicks, Font scaleFont, Color scaleColor, boolean showScaleUp, boolean showSubScaleUp, boolean showValuesUp, boolean showScaleDown, boolean showSubScaleDown, boolean showValuesDown, int addxup, int addyup, int addxdown, int addydown, boolean toDraw) {
        if (toDraw && (showScaleUp || showScaleDown)) {
            int xc = 0, xc2 = 0;
            int yc = 0, yc2 = 0;
            int d = 15;
            double dxu = (xcoord[1] - xcoord[0]);
            double dyu = (ycoord[1] - ycoord[0]);
            double dxd = (xcoord[2] - xcoord[3]);
            double dyd = (ycoord[2] - ycoord[3]);
            double alfa = Math.atan(dyu / dxu);
            double alfa2 = Math.atan(dyd / dxd) + Math.PI;
            double incxu = dxu / nTicks;
            double incyu = dyu / nTicks;
            double incxd = dxd / nTicks;
            double incyd = dyd / nTicks;
            double incVal;
            int signxu = (int) (incxu == 0 ? 1 : Math.signum(incxu));
            int signxd = (int) (incxd == 0 ? 1 : Math.signum(incxd));
            for (int k = 0; k < nTicks; k++) {
                incxu = dxu / nTicks;
                incyu = dyu / nTicks;
                incxd = dxd / nTicks;
                incyd = dyd / nTicks;
                incVal = (maxVal - minVal) / nTicks;
                xc = (int) Math.rint(xcoord[0] + k * incxu);
                yc = (int) Math.rint(ycoord[0] + k * incyu);
                xc2 = (int) Math.rint(xcoord[3] + k * incxd);
                yc2 = (int) Math.rint(ycoord[3] + k * incyd);
                if (showValuesUp || showValuesDown) {
                    String txt = "" + (incVal * k + minVal);
                    Font oldFont = g.getFont();
                    Color oldColor = g.getColor();
                    g.setFont(scaleFont);
                    g.setColor(scaleColor);
                    Rectangle2D txt2D = g.getFontMetrics().getStringBounds(txt, g);
                    if (showValuesUp) {
                        g.drawString(txt, xc + addxup + (int) Math.round(d * Math.sin(alfa) * signxu - (1 - Math.sin(alfa)) * txt2D.getWidth() / 2), yc + addyup + (int) Math.round(-d * Math.cos(alfa) * signxu + (1 - signxu * Math.cos(alfa)) * txt2D.getHeight() / 2));
                    }
                    if (showValuesDown) {
                        g.drawString(txt, xc2 + addxdown + (int) Math.round(d * Math.sin(alfa2) * signxd - (1 - Math.sin(alfa2)) * txt2D.getWidth() / 2), yc2 + addydown + (int) Math.round(-(d - signxd * txt2D.getHeight() / 2) * Math.cos(alfa2) * signxd + (1 - signxd * Math.cos(alfa2)) * txt2D.getHeight() / 2));
                    }
                    g.setFont(oldFont);
                    g.setColor(oldColor);
                }
                if (showScaleUp) {
                    g.drawLine(xc + (int) Math.round(d * Math.sin(alfa) * signxu), yc - (int) Math.round(d * Math.cos(alfa) * signxu), xc, yc);
                }
                xc = (int) Math.rint(xcoord[3] + k * incxd);
                yc = (int) Math.rint(ycoord[3] + k * incyd);
                if (showScaleDown) {
                    g.drawLine(xc - (int) Math.round(d * Math.sin(alfa) * signxd), yc + (int) Math.round(d * Math.cos(alfa) * signxd), xc, yc);
                }
                incxu = incxu / nTicks;
                incyu = incyu / nTicks;
                for (int j = 1; showSubScaleUp && j < nTicks; j++) {
                    xc = (int) Math.rint(xcoord[0] + j * incxu + k * nTicks * incxu);
                    yc = (int) Math.rint(ycoord[0] + j * incyu + k * nTicks * incyu);
                    g.drawLine(xc + (int) Math.round(d / 2 * Math.sin(alfa) * signxu), yc - (int) Math.round(d / 2 * Math.cos(alfa) * signxu), xc, yc);
                }
                incxd = incxd / nTicks;
                incyd = incyd / nTicks;
                for (int j = 1; showSubScaleDown && j < nTicks; j++) {
                    xc = (int) Math.rint(xcoord[3] + j * incxd + k * nTicks * incxd);
                    yc = (int) Math.rint(ycoord[3] + j * incyd + k * nTicks * incyd);
                    g.drawLine(xc - (int) Math.round(d / 2 * Math.sin(alfa) * signxd), yc + (int) Math.round(d / 2 * Math.cos(alfa) * signxd), xc, yc);
                }
            }
            if (showValuesUp || showValuesDown) {
                Font oldFont = g.getFont();
                Color oldColor = g.getColor();
                g.setFont(scaleFont);
                g.setColor(scaleColor);
                String txt = "" + maxVal;
                Rectangle2D txt2D = g.getFontMetrics().getStringBounds(txt, g);
                if (showValuesUp) {
                    g.drawString("" + maxVal, xcoord[1] + addxup + (int) Math.round(d * Math.sin(alfa) * signxu - (1 - Math.sin(alfa)) * txt2D.getWidth() / 2), ycoord[1] + addyup + (int) Math.round(-d * Math.cos(alfa) * signxu + (1 - signxu * Math.cos(alfa)) * txt2D.getHeight() / 2));
                }
                if (showValuesDown) {
                    g.drawString(txt, xcoord[2] + addxdown + (int) Math.round(d * Math.sin(alfa2) * signxd - (1 - Math.sin(alfa2)) * txt2D.getWidth() / 2), ycoord[2] + addydown + (int) Math.round(-(d - signxd * txt2D.getHeight() / 2) * Math.cos(alfa2) * signxd + (1 - signxd * Math.cos(alfa2)) * txt2D.getHeight() / 2));
                }
                g.setFont(oldFont);
                g.setColor(oldColor);
            }
            if (showScaleUp) {
                g.drawLine(xcoord[1] + (int) Math.round(d * Math.sin(alfa) * signxu), ycoord[1] - (int) Math.round(d * Math.cos(alfa) * signxu), xcoord[1], ycoord[1]);
            }
            if (showScaleDown) {
                g.drawLine(xcoord[2] - (int) Math.round(d * Math.sin(alfa) * signxd), ycoord[2] + (int) Math.round(d * Math.cos(alfa) * signxd), xcoord[2], ycoord[2]);
            }
        }
    }

    public static Hashtable getParameters(String selectedValue, int shapeId) {
        Hashtable parameters = new Hashtable();
        try {
            TreeMap<String, CShapeParamsRecord> rows = CShapeParamsTabDAO.getInstance().findImgNameShId(selectedValue, shapeId);
            Iterator<CShapeParamsRecord> it = rows.values().iterator();
            while (it.hasNext()) {
                CShapeParamsRecord r = it.next();
                String p = r.getPk().getParameter();
                String v = r.getValue();
                for (int i = 0; i < stringArrayParNames.length; i++) {
                    if (p.toUpperCase().startsWith(stringArrayParNames[i])) {
                        int idx = Integer.parseInt(p.substring(stringArrayParNames[i].length(), p.length()));
                        parameters.put(stringArrayParNames[i] + idx, (v == null ? "" : v));
                    }
                }
                for (int i = 0; i < intArrayParNames.length; i++) {
                    if (p.toUpperCase().startsWith(intArrayParNames[i])) {
                        int idx = Integer.parseInt(p.substring(intArrayParNames[i].length(), p.length()));
                        if (v != null) {
                            parameters.put(intArrayParNames[i] + idx, new Integer(v));
                        }
                    }
                }
                for (int i = 0; i < floatArrayParNames.length; i++) {
                    if (p.toUpperCase().startsWith(floatArrayParNames[i])) {
                        int idx = Integer.parseInt(p.substring(floatArrayParNames[i].length(), p.length()));
                        if (v != null) {
                            parameters.put(floatArrayParNames[i] + idx, new Float(v));
                        }
                    }
                }
                for (int i = 0; i < intParNames.length; i++) {
                    if (p.equalsIgnoreCase(intParNames[i])) {
                        if (v != null) {
                            parameters.put(intParNames[i], new Integer(v));
                        }
                    }
                }
                for (int i = 0; i < boolParNames.length; i++) {
                    if (p.equalsIgnoreCase(boolParNames[i])) {
                        if (v != null) {
                            parameters.put(boolParNames[i], new Boolean(v));
                        }
                    }
                }
                for (int i = 0; i < stringParNames.length; i++) {
                    if (p.equalsIgnoreCase(stringParNames[i])) {
                        String value = (v == null ? "" : v);
                        parameters.put(stringParNames[i], coalesceBlanks(value));
                    }
                }
                for (int i = 0; i < doubleParNames.length; i++) {
                    if (p.equalsIgnoreCase(doubleParNames[i])) {
                        parameters.put(doubleParNames[i], new Double(v));
                    }
                }
                for (int i = 0; i < floatParNames.length; i++) {
                    if (p.equalsIgnoreCase(floatParNames[i])) {
                        parameters.put(floatParNames[i], new Float(v));
                    }
                }
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        } catch (CTabDAOException ex) {
            ex.printStackTrace();
        }
        return parameters;
    }

    public static void copyFile(String fromPath, String toPath) {
        try {
            File inputFile = new File(fromPath);
            String dirImg = (new File(toPath)).getParent();
            File tmp = new File(dirImg);
            if (!tmp.exists()) {
                tmp.mkdir();
            }
            File outputFile = new File(toPath);
            if (!inputFile.getCanonicalPath().equals(outputFile.getCanonicalPath())) {
                FileInputStream in = new FileInputStream(inputFile);
                FileOutputStream out = new FileOutputStream(outputFile);
                int c;
                while ((c = in.read()) != -1) out.write(c);
                in.close();
                out.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LogHandler.log(ex.getMessage(), Level.INFO, "LOG_MSG", isLoggingEnabled());
        }
    }

    /**
   * Compatta i caratteri spazio consecutivi
   * @param s la stringa di input
   * @return la stringa in cui gli spazi consecutivi sono sostituiti da un
   * unico spazio
   */
    public static String coalesceBlanks(String val) {
        String[] s = val.split("\\s");
        String[] res = new String[s.length];
        String returnVal = "" + s[0];
        int j = 0;
        for (int i = 1; i < s.length; i++) {
            if (!s[i].equals("") && !s[i].equals(" ")) {
                returnVal += (" " + s[i]);
                j++;
            }
        }
        return returnVal;
    }

    public void showMessageDialog(String msg) {
        MessageDialog md = new MessageDialog(null, "Attenzione", false, 2000);
        md.setSize(900, 100);
        md.setLocation(100, 300);
        if (!md.isShowing()) md.setVisible(true);
        md.setText(msg);
    }

    public static void rotate(double alfa, Point p, Point c) {
        double cosAlfa = Math.cos(alfa);
        double sinAlfa = Math.sin(alfa);
        double X = p.x - c.x;
        double Y = p.y - c.y;
        p.x = (int) Math.round(X * cosAlfa - Y * sinAlfa + c.x);
        p.y = (int) Math.round(X * sinAlfa + Y * cosAlfa + c.y);
    }

    public static void fillPolygon(Graphics2D g, int x[], int y[], int v, boolean toDraw, boolean isInOverlay) {
        if (toDraw && isInOverlay) {
            g.fillPolygon(x, y, v);
        }
    }

    public static void fillShape(JPanel p, Graphics2D g, Color c, int xt[], int yt[], int[] xc, int[] yc, int nV, boolean toDraw, boolean is3d, boolean isInOvl) {
        if (is3d) {
            double a = (xt[1] - xt[0]) == 0 ? ((yt[1] - yt[0]) > 0 ? Math.PI / 2 : (yt[1] - yt[0]) < 0 ? -Math.PI / 2 : 0.0) : Math.atan((double) (yt[1] - yt[0]) / (double) (xt[1] - xt[0]));
            double cosAlfa = Math.cos(a);
            double sinAlfa = Math.sin(a);
            double ds = Math.sqrt(Math.pow(xt[1] - xt[0], 2) + Math.pow(yt[1] - yt[0], 2));
            double dk = Math.sqrt(Math.pow(xt[3] - xt[0], 2) + Math.pow(yt[3] - yt[0], 2));
            Point ps = new Point((int) Math.round(ds / 2 * cosAlfa) + xt[0], (int) Math.round(ds / 2 * sinAlfa + yt[0]));
            Point pe = new Point((int) Math.round(ps.x - dk / 2 * sinAlfa), (int) Math.round(ps.y + dk / 2 * cosAlfa));
            Color baseColor = p.getBackground();
            Color gColor = new Color((baseColor.getRed() + 150) % 255, (baseColor.getGreen() + 150) % 255, (baseColor.getBlue() + 150) % 255);
            g.setPaint(new GradientPaint(ps.x, ps.y, gColor, pe.x, pe.y, baseColor, true));
            fillPolygon(g, xt, yt, nV, toDraw, isInOvl);
            g.setPaint(new GradientPaint(ps.x, ps.y, c, pe.x, pe.y, baseColor, true));
            fillPolygon(g, xc, yc, nV, toDraw, isInOvl);
        } else fillPolygon(g, xc, yc, nV, toDraw, isInOvl);
    }

    public String getShapeType(Shapes s) {
        if (s instanceof IChartShape || s instanceof IChartShape) {
            return "Chart";
        }
        if (s instanceof IButtonShape) {
            return "Button";
        }
        if (s instanceof ITabularShape) {
            return "Tabular";
        }
        if (s instanceof IPolygonShape) {
            return s.getElemType();
        }
        return "Unrecognized Shape Type";
    }

    public static CShapePoints getShapePoints(String imgName, int id) {
        double[] x = null;
        double[] y = null;
        try {
            TreeMap<String, CPointsRecord> res = CPointsTabDAO.getInstance().findByImgNameShId(imgName, id);
            x = new double[res.size()];
            y = new double[res.size()];
            Iterator<CPointsRecord> r = res.values().iterator();
            int k = 0;
            while (r.hasNext()) {
                CPointsRecord rp = r.next();
                x[k] = rp.getX();
                y[k] = rp.getY();
                k++;
            }
        } catch (CTabDAOException ex) {
            ex.printStackTrace();
        }
        return new CShapePoints(x, y);
    }
}
