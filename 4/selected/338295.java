package ArianneUtil;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.awt.*;
import java.awt.geom.*;
import com.borland.dx.sql.dataset.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/***
 * Guide Viewer
 * Copyright (C) 2006 Itaco S.r.l.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 ***/
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

    public static final Dimension buttonIconDim = new Dimension(27, 24);

    public static String[] intParNames = { "SHAPECOLOR", "BORDERCOLOR", "GRIDCOLOR", "FILLCOLOR", "TEXTCOLOR", "SELECTEDTEXTCOLOR", "SELECTEDBACKCOLOR", "FOCUSTEXTCOLOR", "FOCUSBACKCOLOR", "TEXTHALIGNMENT", "TEXTVALIGNMENT", "TEXTWIDTH", "TEXTHEIGHT", "FONTSTYLE", "FONTSIZE", "FONTCOLOR", "COLUMNWIDTH", "BORDERWIDTH", "MAXFRACDIGIT", "MINFRACDIGIT", "AUTORESIZEMODE", "OVERLAY", "HISTORYCOUNT", "POLLINGMSEC", "BCKMSEC", "ARROWIDTH", "ARROWLENGTH", "LINETHICK", "CHARTTITLEFONTSTYLE", "CHARTTITLEFONTSIZE", "CHARTTITLECOLOR", "NTICKS", "ROWHEIGHT", "ROWMARGIN", "ISPW", "ISPH", "BODDTABROWRULE", "BEVENTABROWRULE", "FODDTABROWRULE", "FEVENTABROWRULE" };

    public static String[] intArrayParNames = { "TABBGCOLCOLOR", "TABFGCOLCOLOR", "TABCOLALIGNMENTS" };

    public static String[] stringArrayParNames = { "CHARTCURVENAME" };

    public static String[] boolParNames = { "DISPLAYHEADER", "DISPLAYROWNUM", "DISPLAYSHAPES", "BLINKING", "OPAQUE", "SHOWHGRID", "SHOWVGRID", "BACKGROUND", "SHOWBORDER", "SHOWSCALEUP", "SHOWSUBSCALEUP", "SHOWVALUP", "SHOWSCALEDOWN", "SHOWSUBSCALEDOWN", "SHOWVALDOWN", "ENABLED", "POLLING", "DRAWARROW", "FILLARROW", "SPECIFYTIMESTAMP", "AUTOSCALE", "3D", "SHOWLEGEND", "VORIENTATION" };

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
        dropMemDb(memDb);
        createMemDb(memDb);
        new Transfer(path, selectedValue, memDb.conn);
    }

    /**
   * Elimina il DB in memoria
   */
    public static void dropMemDb(MemDb memDb) {
        try {
            memDb.update("DROP TABLE DBCONNECTION IF EXISTS");
            memDb.update("DROP TABLE IMAGE IF EXISTS");
            memDb.update("DROP TABLE LINK_SHAPES IF EXISTS");
            memDb.update("DROP TABLE POINT IF EXISTS");
            memDb.update("DROP TABLE SHAPE IF EXISTS");
            memDb.update("DROP TABLE SHAPE_PARAMETERS IF EXISTS ");
            memDb.update("DROP TABLE LINK_SHAPEQUERY IF EXISTS");
            memDb.update("DROP TABLE SHAPEQUERY IF EXISTS");
            memDb.update("DROP TABLE SYSCALL IF EXISTS");
            memDb.conn.commit();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Crea il DB in memoria
   */
    public static void createMemDb(MemDb memDb) {
        try {
            memDb.update("CREATE TABLE DBCONNECTION (ID INT  NOT NULL, HOST VARCHAR(255), PORT INT , DBNAME VARCHAR(255),USERNAME VARCHAR(255) , PASSWORD VARCHAR(255) , NAME VARCHAR(255),IMAGE_NAME VARCHAR(255) NOT NULL, PRIMARY KEY (IMAGE_NAME,ID) )");
            memDb.update("CREATE TABLE IMAGE (NAME VARCHAR(255)  NOT NULL, WIDTH INT  NOT NULL, HEIGHT INT  NOT NULL, BACKGROUND VARCHAR(255), BCKIMG VARCHAR(512), RELOADTIME INT DEFAULT 0 NOT NULL, PRIMARY KEY (NAME) )");
            memDb.update("CREATE TABLE LINK_SHAPES (ID_SH2 INT  NOT NULL, IMAGE_NAME_SH2 VARCHAR(255)  NOT NULL, ID_SH1 INT  NOT NULL, IMAGE_NAME_SH1 VARCHAR(255)  NOT NULL, LINE_NUMBER INT DEFAULT 1 NOT NULL , LINK_ACTION VARCHAR(255) )");
            memDb.update("CREATE TABLE POINT (IMAGE_NAME VARCHAR(255) , SHAPE_ID INT  NOT NULL, POINT_ID INT , X DOUBLE  NOT NULL, Y DOUBLE  NOT NULL, PRIMARY KEY (IMAGE_NAME, SHAPE_ID, POINT_ID))");
            memDb.update("CREATE TABLE SHAPE (IMAGE_NAME VARCHAR(255)  NOT NULL, ID INT  NOT NULL, TYPE VARCHAR(255) , FATHER_SHAPE_ID INT , NUMVERTEX INT , REFRESHPERIOD INT , DBCONNECTION_ID INT, SEQ INT, PRIMARY KEY (IMAGE_NAME,ID) )");
            memDb.update("CREATE TABLE SHAPE_PARAMETERS (IMAGE_NAME VARCHAR(255)  NOT NULL, SHAPE_ID INT  NOT NULL, PARAMETER VARCHAR(255)  NOT NULL, VALUE VARCHAR(255), PRIMARY KEY (IMAGE_NAME,SHAPE_ID,PARAMETER) )");
            memDb.update("CREATE TABLE LINK_SHAPEQUERY (SHAPE_ID INT  NOT NULL, IMAGE_NAME VARCHAR(255)  NOT NULL, QUERY_ID INT  NOT NULL, PRIMARY KEY (IMAGE_NAME,SHAPE_ID,QUERY_ID) )");
            memDb.update("CREATE TABLE SHAPEQUERY (ID INT  NOT NULL, IMAGE_NAME VARCHAR(255)  NOT NULL,  SQLQUERY VARCHAR(2048), PRIMARY KEY (ID,IMAGE_NAME) )");
            memDb.update("CREATE TABLE SYSCALL (SYS_CALL_NAME VARCHAR(255) , ID INT  NOT NULL, IMAGE_NAME VARCHAR(255) , DESCR VARCHAR(255) , FK_SHAPE_ID INT )");
            memDb.update("CREATE INDEX DBCONNECTION_NAME_IDX ON DBCONNECTION (NAME)");
            memDb.update("CREATE INDEX POINT_IMAGE_NAME_IDX ON POINT (IMAGE_NAME)");
            memDb.update("CREATE INDEX POINT_ID_IDX ON POINT (POINT_ID)");
            memDb.update("CREATE INDEX POINT_SHAPE_ID_IDX ON POINT (SHAPE_ID)");
            memDb.update("CREATE INDEX LINK_SHAPEQUERY_QUERY_ID_IDX ON LINK_SHAPEQUERY (QUERY_ID)");
            memDb.update("CREATE INDEX LINK_SHAPEQUERY_IMAGE_NAME_IDX ON LINK_SHAPEQUERY (IMAGE_NAME)");
            memDb.update("CREATE INDEX LINK_SHAPEQUERY_SHAPE_ID_IDX ON LINK_SHAPEQUERY (SHAPE_ID)");
            memDb.update("CREATE INDEX SHAPEQUERY_ID_IDX ON SHAPEQUERY (ID)");
            memDb.update("CREATE INDEX SHAPEQUERY_IMAGE_NAME_IDX ON SHAPEQUERY (IMAGE_NAME)");
            memDb.update("CREATE INDEX SHAPEQUERY_SQLQUERY_IDX ON SHAPEQUERY (SQLQUERY)");
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Inizializza i link al DB
   */
    public static Database initLink(MemDb memDb) {
        ArianneUtil.Util.createMemDb(memDb);
        try {
            memDb.update("CREATE USER root PASSWORD manager ADMIN");
            memDb.connect("root", "manager");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Database(memDb.getConnection());
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

    /**
   * Stampa in output il contenuto della tabella SYSCALL
   */
    public static int dumpTableSYSCALL(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM SYSCALL");
            System.out.println("SYSCALL:");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "SYS_CALL_NAME") + "," + getInt(rs, "ID") + "," + getString(rs, "IMAGE_NAME") + "," + getString(rs, "DESCR") + "," + getInt(rs, "FK_SHAPE_ID"));
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
   * Stampa in output il contenuto della tabella LINK_SHAPEQUERY
   */
    public static int dumpTableLINK_SHAPEQUERY(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM LINK_SHAPEQUERY");
            System.out.println("LINK_SHAPEQUERY:");
            while (rs.next()) {
                nRows++;
                System.out.println(getInt(rs, "SHAPE_ID") + "," + getString(rs, "IMAGE_NAME") + "," + getInt(rs, "QUERY_ID"));
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
   * Stampa in output il contenuto della tabella SHAPEQUERY
   */
    public static int dumpTableSHAPEQUERY(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM SHAPEQUERY");
            System.out.println("SHAPEQUERY:");
            while (rs.next()) {
                nRows++;
                System.out.println(getInt(rs, "ID") + "," + getString(rs, "IMAGE_NAME") + "," + getString(rs, "SQLQUERY"));
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
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "IMAGE_NAME") + "," + getInt(rs, "ID") + "," + getString(rs, "TYPE") + "," + getInt(rs, "FATHER_SHAPE_ID") + "," + getInt(rs, "NUMVERTEX") + "," + getInt(rs, "REFRESHPERIOD") + "," + getInt(rs, "DBCONNECTION_ID"));
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
   * Stampa in output il contenuto della tabella SHAPE_PARAMETERS
   */
    public static int dumpTableSHAPE_PARAMETERS(MemDb memDb) {
        int nRows = 0;
        try {
            Statement st = null;
            ResultSet rs = null;
            st = memDb.getConnection().createStatement();
            rs = st.executeQuery("SELECT * FROM SHAPE_PARAMETERS");
            System.out.println("SHAPE_PARAMETERS:");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "IMAGE_NAME") + "," + getInt(rs, "SHAPE_ID") + "," + getString(rs, "PARAMETER") + "," + getString(rs, "VALUE"));
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
            System.out.println("DBCONNECTION:");
            while (rs.next()) {
                nRows++;
                System.out.println(getString(rs, "IMAGE_NAME") + " - " + rs.getInt("ID") + " - " + getInt(rs, "PORT") + " - " + getString(rs, "HOST") + " - " + getString(rs, "DBNAME") + " - " + getString(rs, "USERNAME") + " - " + getString(rs, "PASSWORD") + " - " + getString(rs, "NAME"));
            }
            rs.close();
            st.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nRows;
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

    /**
   * Carica il DB in memoria
   */
    public static void loadMemDb(MemDb memDb, Database localDb) {
        ArianneUtil.Util.createMemDb(memDb);
        loadTableDBCONNECTION(memDb, localDb);
        loadTableIMAGE(memDb, localDb);
        loadTableLINK_SHAPES(memDb, localDb);
        loadTablePOINT(memDb, localDb);
        loadTableSHAPE(memDb, localDb);
        loadTableSHAPE_PARAMETERS(memDb, localDb);
        loadTableLINK_SHAPEQUERY(memDb, localDb);
        loadTableSHAPEQUERY(memDb, localDb);
        loadTableSYSCALL(memDb, localDb);
    }

    /**
   * Carica la tabella DBCONNECTION
   */
    public static void loadTableDBCONNECTION(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM DBCONNECTION");
            while (appR.next()) {
                memDb.update("INSERT INTO DBCONNECTION ( IMAGE_NAME,ID,PORT,HOST,DBNAME,USERNAME,PASSWORD,NAME) " + "VALUES (" + getString(appR, "IMAGE_NAME") + "," + getInt(appR, "ID") + "," + getInt(appR, "PORT") + "," + getString(appR, "HOST") + "," + getString(appR, "DBNAME") + "," + getString(appR, "USERNAME") + "," + getString(appR, "PASSWORD") + "," + getString(appR, "NAME") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella IMAGE
   */
    public static void loadTableIMAGE(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM IMAGE");
            while (appR.next()) {
                memDb.update("INSERT INTO IMAGE (NAME,WIDTH,HEIGHT,BACKGROUND,BCKIMG,RELOADTIME) " + "VALUES ('" + getString(appR, "NAME") + "'," + getInt(appR, "WIDTH") + "," + getInt(appR, "HEIGHT") + "," + getString(appR, "BACKGROUND") + "," + getString(appR, "BCKIMG") + "," + getInt(appR, "RELOADTIME") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella LINK_SHAPES
   */
    public static void loadTableLINK_SHAPES(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM LINK_SHAPES");
            while (appR.next()) {
                String app = "INSERT INTO LINK_SHAPES (ID_SH2,IMAGE_NAME_SH2,ID_SH1,IMAGE_NAME_SH1,LINE_NUMBER,LINK_ACTION) " + "VALUES (" + getInt(appR, "ID_SH2") + "," + getString(appR, "IMAGE_NAME_SH2") + "," + getInt(appR, "ID_SH1") + "," + getString(appR, "IMAGE_NAME_SH1") + "," + getInt(appR, "LINE_NUMBER") + "," + getString(appR, "LINK_ACTION") + ")";
                memDb.update("INSERT INTO LINK_SHAPES (ID_SH2,IMAGE_NAME_SH2,ID_SH1,IMAGE_NAME_SH1,LINE_NUMBER,LINK_ACTION) " + "VALUES (" + getInt(appR, "ID_SH2") + "," + getString(appR, "IMAGE_NAME_SH2") + "," + getInt(appR, "ID_SH1") + "," + getString(appR, "IMAGE_NAME_SH1") + "," + getInt(appR, "LINE_NUMBER") + "," + getString(appR, "LINK_ACTION") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella POINT
   */
    public static void loadTablePOINT(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM POINT");
            while (appR.next()) {
                memDb.update("INSERT INTO POINT (IMAGE_NAME,SHAPE_ID,POINT_ID,X,Y) " + "VALUES (" + getString(appR, "IMAGE_NAME") + "," + getInt(appR, "SHAPE_ID") + "," + getInt(appR, "POINT_ID") + "," + getDouble(appR, "X") + "," + getDouble(appR, "Y") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella SHAPE
   */
    public static void loadTableSHAPE(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM SHAPE");
            while (appR.next()) {
                memDb.update("INSERT INTO SHAPE (IMAGE_NAME,ID,TYPE,FATHER_SHAPE_ID,NUMVERTEX,REFRESHPERIOD,DBCONNECTION_ID) " + "VALUES (" + getString(appR, "IMAGE_NAME") + "," + getInt(appR, "ID") + "," + getString(appR, "TYPE") + "," + getInt(appR, "FATHER_SHAPE_ID") + "," + getInt(appR, "NUMVERTEX") + "," + getInt(appR, "REFRESHPERIOD") + "," + getInt(appR, "DBCONNECTION_ID") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella SHAPE_PARAMETERS
   */
    public static void loadTableSHAPE_PARAMETERS(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM SHAPE_PARAMETERS");
            while (appR.next()) {
                memDb.update("INSERT INTO SHAPE_PARAMETERS (IMAGE_NAME,SHAPE_ID,PARAMETER,VALUE) " + "VALUES (" + getString(appR, "IMAGE_NAME") + "," + getInt(appR, "SHAPE_ID") + "," + getString(appR, "PARAMETER") + "," + getString(appR, "VALUE") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella SHAPEQUERY
   */
    public static void loadTableSHAPEQUERY(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM SHAPEQUERY");
            while (appR.next()) {
                String sqlInsert = "INSERT INTO SHAPEQUERY (IMAGE_NAME,ID,SQLQUERY) " + "VALUES (" + getInt(appR, "ID") + "," + getString(appR, "IMAGE_NAME") + "," + getString(appR, "SQLQUERY") + ")";
                memDb.update(sqlInsert);
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
   * Carica la tabella LINK_SHAPEQUERY
   */
    public static void loadTableLINK_SHAPEQUERY(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM LINK_SHAPEQUERY");
            while (appR.next()) {
                String sqlInsert = "INSERT INTO LINK_SHAPEQUERY (SHAPE_ID,IMAGE_NAME,QUERY_ID) " + "VALUES (" + getInt(appR, "SHAPE_ID") + "," + getString(appR, "IMAGE_NAME") + "," + getInt(appR, "QUERY_ID") + ")";
                memDb.update(sqlInsert);
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    /**
     * Carica la tabella SYSCALL
     */
    public static void loadTableSYSCALL(MemDb memDb, Database localDb) {
        try {
            Statement appS = localDb.createStatement();
            ResultSet appR = appS.executeQuery("SELECT * FROM SYSCALL");
            while (appR.next()) {
                memDb.update("INSERT INTO SYSCALL (SYS_CALL_NAME,ID,IMAGE_NAME,DESCR,FK_SHAPE_ID) " + "VALUES (" + getString(appR, "SYS_CALL_NAME") + "," + getInt(appR, "ID") + "," + getString(appR, "IMAGE_NAME") + "," + getString(appR, "DESCR") + "," + getInt(appR, "FK_SHAPE_ID") + ")");
            }
            appR.close();
            appS.close();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
            LogHandler.log(sqlex.getMessage(), Level.INFO, "ERR_MSG", isLoggingEnabled());
        }
    }

    public static Vector loadShapePoints(int id, String imgName, String type, int nVertex, Database localDb) {
        Vector res = new Vector();
        double[] xPnt = new double[nVertex];
        double[] yPnt = new double[nVertex];
        try {
            String qp = "SELECT * " + "FROM POINT " + "WHERE SHAPE_ID=" + id + " AND " + "IMAGE_NAME='" + imgName + "'";
            Statement sp = localDb.createStatement();
            ResultSet rp = sp.executeQuery(qp);
            int k = 0;
            while (rp.next()) {
                xPnt[k] = rp.getDouble("X");
                yPnt[k] = rp.getDouble("Y");
                k++;
            }
            rp.close();
            sp.close();
        } catch (java.sql.SQLException sqlex) {
            LogHandler.log("Errato Caricamento oggetto " + type + " " + id + "(file " + imgName + ")", Level.INFO, "ERR_MSG", isLoggingEnabled());
            sqlex.printStackTrace();
        }
        res.addElement(xPnt);
        res.addElement(yPnt);
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

    public static Hashtable getParameters(Database localDb, String selectedValue, int shapeId) {
        Hashtable parameters = new Hashtable();
        String qp = "SELECT PARAMETER, VALUE " + "FROM SHAPE_PARAMETERS " + "WHERE IMAGE_NAME='" + selectedValue + "' AND " + "SHAPE_ID=" + shapeId;
        try {
            Statement s = localDb.createStatement();
            ResultSet r = s.executeQuery(qp);
            while (r.next()) {
                for (int i = 0; i < stringArrayParNames.length; i++) {
                    if (r.getString("PARAMETER").toUpperCase().startsWith(stringArrayParNames[i])) {
                        int idx = Integer.parseInt(r.getString("PARAMETER").substring(stringArrayParNames[i].length(), r.getString("PARAMETER").length()));
                        parameters.put(stringArrayParNames[i] + idx, (r.getString("VALUE") == null ? "" : r.getString("VALUE")));
                    }
                }
                for (int i = 0; i < intArrayParNames.length; i++) {
                    if (r.getString("PARAMETER").toUpperCase().startsWith(intArrayParNames[i])) {
                        int idx = Integer.parseInt(r.getString("PARAMETER").substring(intArrayParNames[i].length(), r.getString("PARAMETER").length()));
                        if (r.getString("VALUE") != null) parameters.put(intArrayParNames[i] + idx, new Integer(r.getString("VALUE")));
                    }
                }
                for (int i = 0; i < intParNames.length; i++) {
                    if (r.getString("PARAMETER").equalsIgnoreCase(intParNames[i])) {
                        if (r.getString("VALUE") != null) parameters.put(intParNames[i], new Integer(r.getString("VALUE")));
                    }
                }
                for (int i = 0; i < boolParNames.length; i++) {
                    if (r.getString("PARAMETER").equalsIgnoreCase(boolParNames[i])) {
                        if (r.getString("VALUE") != null) parameters.put(boolParNames[i], new Boolean(r.getString("VALUE")));
                    }
                }
                for (int i = 0; i < stringParNames.length; i++) {
                    if (r.getString("PARAMETER").equalsIgnoreCase(stringParNames[i])) {
                        String value = (r.getString("VALUE") == null ? "" : r.getString("VALUE"));
                        parameters.put(stringParNames[i], coalesceBlanks(value));
                    }
                }
                for (int i = 0; i < doubleParNames.length; i++) {
                    if (r.getString("PARAMETER").equalsIgnoreCase(doubleParNames[i])) {
                        parameters.put(doubleParNames[i], new Double(r.getString("VALUE")));
                    }
                }
                for (int i = 0; i < floatParNames.length; i++) {
                    if (r.getString("PARAMETER").equalsIgnoreCase(floatParNames[i])) {
                        parameters.put(floatParNames[i], new Float(r.getString("VALUE")));
                    }
                }
            }
            r.close();
            s.close();
        } catch (Exception ex) {
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
}
