import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Markus Plessing
 */
public class BinaryFile {

    /**
	 * 
	 * @param con Connection to the Database, which should be used
	 * @param type String which documenttype to get
	 * @param savePath String Path to save the File to got from BLOB
	 * @param id int the database id of the document
	 * @return boolean success?
	 */
    public static final boolean dbFileSaveTo(Connection con, String savePath, String type, int id) {
        String SQLQuery = "select " + type + " from documents where documentid= " + id + "";
        boolean success = true;
        InputStream is;
        FileOutputStream fos;
        byte[] buff;
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(SQLQuery);
            if (rs.next()) {
                is = rs.getBinaryStream(type);
                fos = new FileOutputStream(savePath);
                buff = new byte[8192];
                int len;
                while (0 < (len = is.read(buff))) fos.write(buff, 0, len);
                fos.close();
                is.close();
            }
            rs.close();
            stmt.close();
            con.close();
            fos = null;
            buff = null;
        } catch (java.lang.Exception ex) {
            success = false;
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
        return success;
    }

    /**
	 * 
	 * @param con Connection to the Database, which should be used
	 * @param SQLQuery String to send to DB as the query
	 * @param filePath String Path to save the File to got from BLOB
	 * @param type document ending
	 */
    public static final void getFileFromDB(Connection con, String SQLQuery, String filePath, String type) {
        InputStream is;
        FileOutputStream fos;
        byte[] buff;
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(SQLQuery);
            if (rs.next()) {
                is = rs.getBinaryStream(type);
                fos = new FileOutputStream(filePath);
                buff = new byte[8192];
                int len;
                while (0 < (len = is.read(buff))) fos.write(buff, 0, len);
                fos.close();
                is.close();
            }
            rs.close();
            stmt.close();
            con.close();
            fos = null;
            buff = null;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
    }

    /**
	  * 
	  * @param SQLCommand String SQLCommand to perform in DB 
	  * @param filePath String Path to the File to Save to BLOB
	  * @return boolean success true/false
	  */
    public static final boolean putFiletoDB(String SQLCommand, String filePath) {
        ExecuteSQL con = new ExecuteSQL();
        File file1 = new File(filePath + ".sxw");
        File file2 = new File(filePath + ".pdf");
        try {
            con.beginTransaction();
            java.sql.PreparedStatement pstmt = ConnectDB.con.prepareStatement(SQLCommand);
            pstmt.setBinaryStream(1, new FileInputStream(filePath + ".sxw"), (int) file1.length());
            pstmt.setBinaryStream(2, new FileInputStream(filePath + ".pdf"), (int) file2.length());
            pstmt.executeUpdate();
            con.endTransaction();
            con = null;
            file1 = null;
            file2 = null;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
            return false;
        }
        return true;
    }

    /**
	 * 
	 * @param con Connection to the Database, which should be used
	 * @param orderid
	 * @return Hashtable
	 */
    public static final MyHashtable getLabelsFromDB(Connection con, int orderid) {
        MyHashtable response = new MyHashtable();
        InputStream is;
        FileOutputStream fos;
        byte[] buff;
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select * from ups_labels where orderid=" + orderid);
            while (rs.next()) {
                String fileName = "label" + orderid + "_" + System.currentTimeMillis() + ".gif";
                String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
                is = rs.getBinaryStream("label");
                fos = new FileOutputStream(filePath);
                buff = new byte[8192];
                int len;
                while (0 < (len = is.read(buff))) fos.write(buff, 0, len);
                fos.close();
                is.close();
                response.put(fileName, filePath);
            }
            rs.close();
            stmt.close();
            con.close();
            fos = null;
            buff = null;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
        return response;
    }

    /**
	  * 
	  * @param SQLCommand String SQLCommand to perform in DB 
	  * @param filePath String Path to the File to Save to BLOB
	  * @return boolean success true/false
	  */
    public static final boolean putLabeltoDB(String SQLCommand, String filePath) {
        ExecuteSQL con = new ExecuteSQL();
        File file = new File(filePath);
        try {
            con.beginTransaction();
            java.sql.PreparedStatement pstmt = ConnectDB.con.prepareStatement(SQLCommand);
            pstmt.setBinaryStream(1, new FileInputStream(filePath), (int) file.length());
            pstmt.executeUpdate();
            con.endTransaction();
            con = null;
            file = null;
        } catch (SQLException ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
            return false;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
            return false;
        }
        return true;
    }
}
