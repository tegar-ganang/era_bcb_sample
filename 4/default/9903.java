import java.sql.*;
import java.util.*;
import java.io.*;
import java.security.*;
import borknet_services.core.*;

/**
 * The database communication class of the Q IRC C.
 * @author Ozafy - ozafy@borknet.org - http://www.borknet.org
 */
public class DBControl {

    /** Database connection */
    private Connection con;

    /** Main bot */
    private Core C;

    private CoreDBControl dbc;

    private X Bot;

    private int maxUsers;

    private int maxOpers;

    private int maxServers;

    private int maxChannels;

    /**
	 * Constructs a Database connection.
	 * @param server		Database server
	 * @param user			Database user
	 * @param pass			Database password
	 * @param db			Database
	 * @param debug			Are we debugging?
	 * @param B				Main bot
	 */
    public DBControl(Core C, X Bot) {
        try {
            this.C = C;
            this.Bot = Bot;
            this.dbc = C.get_dbc();
            this.con = dbc.getCon();
            load();
        } catch (Exception e) {
            C.printDebug("Database error!");
            System.exit(0);
        }
    }

    private void load() {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT * FROM x_stats");
            ResultSet rs = pstmt.executeQuery();
            rs.first();
            maxUsers = rs.getInt("maxusers");
            maxOpers = rs.getInt("maxopers");
            maxServers = rs.getInt("maxservers");
            maxChannels = rs.getInt("maxchannels");
        } catch (Exception e) {
            System.out.println("Error executing sql statement");
            C.debug(e);
            System.exit(0);
        }
    }

    public void save() {
        try {
            getOperCount();
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("UPDATE x_stats SET maxusers = ?, maxopers = ?,maxservers = ?,maxchannels = ?");
            pstmt.setInt(1, maxUsers);
            pstmt.setInt(2, maxOpers);
            pstmt.setInt(3, maxServers);
            pstmt.setInt(4, maxChannels);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error executing sql statement");
            C.debug(e);
            System.exit(0);
        }
    }

    public int getUserCount() {
        return dbc.getUsers().size();
    }

    public int getMaxUserCount() {
        if (dbc.getUsers().size() > maxUsers) {
            maxUsers = dbc.getUsers().size();
        }
        return maxUsers;
    }

    public int getOperCount() {
        int opers = 0;
        ArrayList<String> numerics = new ArrayList<String>(dbc.getUsers().keySet());
        for (String n : numerics) {
            User u = dbc.getUsers().get(n);
            if (u.getModes().contains("o")) {
                opers++;
            }
        }
        if (opers > maxOpers) {
            maxOpers = opers;
        }
        return opers;
    }

    public int getMaxOperCount() {
        return maxOpers;
    }

    public int getServerCount() {
        return dbc.getServerCount();
    }

    public int getMaxServerCount() {
        if (dbc.getServerCount() > maxServers) {
            maxServers = dbc.getServerCount();
        }
        return maxServers;
    }

    public String[][] getServerTable() {
        return dbc.getServerTable();
    }

    public int getChannelCount() {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT SQL_CALC_FOUND_ROWS * FROM userchans GROUP BY channel");
            ResultSet rs = pstmt.executeQuery();
            pstmt = con.prepareStatement("SELECT FOUND_ROWS();");
            rs = pstmt.executeQuery();
            rs.first();
            int channels = rs.getInt(1);
            rs.close();
            if (channels > maxChannels) {
                maxChannels = channels;
            }
            return channels;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getMaxChannelCount() {
        return maxChannels;
    }

    public String[][] getChannelTable() {
        try {
            PreparedStatement pstmt;
            pstmt = con.prepareStatement("SELECT Count(user) as count,channel FROM userchans GROUP BY channel ORDER BY count DESC LIMIT 10;");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String> a = new ArrayList<String>();
            ArrayList<String> b = new ArrayList<String>();
            while (rs.next()) {
                a.add(rs.getString("channel").toLowerCase());
                b.add(rs.getString("count"));
            }
            String[][] r = new String[a.size()][2];
            if (a.size() > 0) {
                for (int n = 0; n < r.length; n++) {
                    r[n][0] = a.get(n);
                    r[n][1] = b.get(n);
                }
                return r;
            } else {
                return new String[][] { { "0", "0" }, { "0", "0" } };
            }
        } catch (Exception e) {
            return new String[][] { { "0", "0" }, { "0", "0" } };
        }
    }
}
