package org.vrforcad.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.vrforcad.controller.online.ZIPfile;

/**
 * This class perform the Mysql Connection. 
 * 
 * @version 1.2 
 * @author Daniel Cioi <dan.cioi@vrforcad.org>
 */
public class ConnectToMySQL {

    Connection conn = null;

    File tempFile = null;

    int id;

    String fileName = null;

    String fileNameWithExt = null;

    String userName = null;

    String description = null;

    /**
    * Connect to MySQL database.
    * @param fileFormat	file extension
    * @param typeQuery	select, dFile (download file), uFile (upload file)
    */
    public void connect(String fileFormat, String typeQuery) {
        try {
            String userName = "";
            String password = "";
            String dbURL = "";
            String dbName = "";
            String url = "jdbc:mysql://" + dbURL + "/" + dbName;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, userName, password);
            System.out.println("Database connection established");
            if (typeQuery == "select") {
                querySelect(fileFormat);
            }
            if (typeQuery == "dFile") {
                queryGetFile(fileFormat, id);
            }
            if (typeQuery == "uFile") {
                queryUploadFile(fileFormat);
            }
        } catch (Exception e) {
            System.err.println("Cannot connect to database server");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database connection terminated");
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * Show the files in a table.
	 * @param table which table must be query
	 */
    private void querySelect(String table) {
        Statement st;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            if (table == "stl") {
                rs = st.executeQuery("select id, file_name, user_create, user_modify, version, description from stl");
            }
            if (table == "dxf") {
                rs = st.executeQuery("select id, file_name, user_create, user_modify, version, description from dxf");
            }
            if (table == "x3d") {
                rs = st.executeQuery("select id, file_name, user_create, user_modify, version, description from x3d");
            }
            if (table == "vfc") {
                rs = st.executeQuery("select id, file_name, user_create, user_modify, version, description from vfc");
            }
            int row = 0;
            while (rs.next()) {
                row++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Get the file from id.
	 * @param id id number
	 */
    public void getFile(int id) {
        this.id = id;
        connect("stl", "dFile");
        File theFile = tempFile;
    }

    /**
    * Get the selected file and download it local in temp folder.
    * @param table
    * @param id
    */
    public void queryGetFile(String table, int id) {
        Statement st;
        ResultSet rs = null;
        String filename = null;
        try {
            st = conn.createStatement();
            if (table == "stl") {
                String stlQuery = "select file_name, file_content from stl where id=" + id;
                rs = st.executeQuery(stlQuery);
            }
            if (table == "dxf") {
                String dxfQuery = "select file_name, file_content from dxf where id=" + id;
                rs = st.executeQuery(dxfQuery);
            }
            if (table == "x3d") {
                String x3dQuery = "select file_name, file_content from x3d where id=" + id;
                rs = st.executeQuery(x3dQuery);
            }
            if (table == "vfc") {
                String vfcQuery = "select file_name, file_content from vfc where id=" + id;
                rs = st.executeQuery(vfcQuery);
            }
            while (rs.next()) {
                Blob blob = rs.getBlob("file_content");
                filename = rs.getString("file_name");
                OutputStream fwriter;
                try {
                    fwriter = new FileOutputStream("temp/" + filename + ".zip");
                    readFromBlob(blob, fwriter);
                    fwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        File zipFile = new File("temp/" + filename + ".zip");
        ZIPfile decompress = new ZIPfile(zipFile);
        File getTempFile = new File("temp/" + filename + "." + table);
    }

    /** Read from BLOB file type in database */
    public void readFromBlob(Blob blob, OutputStream out) throws SQLException, IOException {
        InputStream in = blob.getBinaryStream();
        int length = -1;
        long read = 0;
        byte[] buf = new byte[1024];
        while ((length = in.read(buf)) != -1) {
            out.write(buf, 0, length);
            read += length;
        }
        in.close();
    }

    /**
	 * Upload the file to database.
	 * @param fileFormat the file extension
	 * @param fileName the file name
	 * @param userName the user name who send the file tot database
	 * @param description few words about the file (description)
	 */
    public void writeToDatabase(String fileFormat, String fileName, String userName, String description) {
        this.fileName = fileName;
        this.fileNameWithExt = fileName + ".zip";
        this.userName = userName;
        this.description = description;
        connect(fileFormat, "uFile");
    }

    /**
        * Upload the file to database.
        * @param table in which table must upload the file 
        */
    public void queryUploadFile(String table) {
        try {
            PreparedStatement ps = null;
            String whoModify = "test";
            String fileVersion = "" + 11;
            if (table == "stl") {
                String stlQuery = "INSERT INTO stl (file_name, user_create, user_modify, version, file_content, description) VALUES ('" + fileName + "', '" + userName + "', '" + whoModify + "', '" + fileVersion + "', ?, '" + description + "')";
                ps = conn.prepareStatement(stlQuery);
            }
            if (table == "dxf") {
                String stlQuery = "INSERT INTO dxf (file_name, user_create, user_modify, version, file_content, description) VALUES ('" + fileName + "', '" + userName + "', '" + whoModify + "', '" + fileVersion + "', ?, '" + description + "')";
                ps = conn.prepareStatement(stlQuery);
            }
            if (table == "vfc") {
                String stlQuery = "INSERT INTO vfc (file_name, user_create, user_modify, version, file_content, description) VALUES ('" + fileName + "', '" + userName + "', '" + whoModify + "', '" + fileVersion + "', ?, '" + description + "')";
                ps = conn.prepareStatement(stlQuery);
            }
            File fileIn = new File("temp/" + fileNameWithExt);
            int fileLength = (int) fileIn.length();
            InputStream streamedFile;
            try {
                streamedFile = new FileInputStream(fileIn);
                ps.setBinaryStream(1, streamedFile, fileLength);
                ps.executeUpdate();
                ps.close();
                streamedFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
