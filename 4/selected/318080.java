package patho.textmining.munich;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;
import patho.generic.*;
import patho.generic.ConnectDataBase;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

/**
 *
 * @author Administrator
 */
public class ImportIntoDb {

    private XMLReader xml_reader_ = null;

    private XMLNode xml_node_;

    private ConnectDataBase db_;

    private String spesimen_;

    public ImportIntoDb(String file, String spesimen) {
        xml_reader_ = new XMLReader(file);
        xml_node_ = new XMLNode(xml_reader_.getRootNode(), true);
        spesimen_ = spesimen;
        db_ = new ConnectDataBase();
        try {
            db_.connectPostgresPropertie();
        } catch (org.postgresql.util.PSQLException exloc) {
            System.err.println("ImportIntoDb::ImportIntoDb - PSQLException Error: " + exloc.getMessage());
            System.err.println("ImportIntoDb::ImportIntoDb - Exit Program, No connection to lockal update database available");
            System.exit(-1);
        }
        try {
            Statement st = db_.getNewStatement();
            st.execute("BEGIN");
        } catch (SQLException ex) {
            System.err.println("Transaction failed!");
            System.err.println("SQL Error: " + ex.getMessage());
        }
        HashSet<String> inserts = xml_node_.generateInsert(spesimen_);
        for (String query : inserts) {
            try {
                Statement st = db_.getNewStatement();
                st.execute(query);
            } catch (SQLException ex) {
                System.err.println("Transaction failed!");
                System.err.println("SQL Error: " + ex.getMessage());
            }
        }
        try {
            Statement st = db_.getNewStatement();
            st.execute("COMMIT");
        } catch (SQLException ex) {
            System.err.println("Transaction failed!");
            System.err.println("SQL Error: " + ex.getMessage());
        }
        copyFile(file);
    }

    private void copyFile(String file) {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            Date dt = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HHmmss ");
            File in = new File(file);
            String[] name = file.split("\\\\");
            File out = new File(".\\xml_archiv\\" + df.format(dt) + name[name.length - 1]);
            inChannel = new FileInputStream(in).getChannel();
            outChannel = new FileOutputStream(out).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            System.err.println("Copy error!");
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (inChannel != null) {
                try {
                    inChannel.close();
                } catch (IOException ex) {
                    Logger.getLogger(ImportIntoDb.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (outChannel != null) {
                try {
                    outChannel.close();
                } catch (IOException ex) {
                    Logger.getLogger(ImportIntoDb.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
