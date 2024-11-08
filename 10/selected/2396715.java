package dk.nullesoft.Airlog.DBinstantdb;

import dk.nullesoft.Airlog.*;
import java.sql.*;
import java.util.*;

public class MiscDB implements dk.nullesoft.Airlog.MiscDB {

    private JdbcConnection conn;

    private org.log4j.Category log = org.log4j.Category.getInstance("Log.MiscDB");

    public MiscDB(JdbcConnection conn) {
        this.conn = conn;
    }

    public void close() {
        try {
            conn.rollback();
            Statement stmt = conn.getStatement();
            stmt.executeUpdate("shutdown");
            stmt.close();
            log.debug("Airlog database closed");
        } catch (Exception e) {
            log.debug(e.toString());
        }
    }

    /**
   Method returns flights and minutes within the last year
*/
    public Hashtable getBarometerData(int currentPilot) throws SQLException {
        Hashtable result = new Hashtable();
        Calendar aYearAgo = new GregorianCalendar();
        aYearAgo.set(Calendar.YEAR, aYearAgo.get(Calendar.YEAR) - 1);
        long then = aYearAgo.getTime().getTime();
        int flights = 0;
        int minutes = 0;
        Statement stmt = conn.getStatement();
        ResultSet rs = stmt.executeQuery("select motortid, slaebetid, svaevetid from flyvning where " + "flyvning.dato > " + then + " and " + "flyvning.kaptajn = " + "'Y' and " + "pilot_id = " + currentPilot);
        try {
            while (rs.next()) {
                flights++;
                minutes = minutes + rs.getInt(1) + rs.getInt(2) + rs.getInt(3);
                if (false) log.debug("MiscDB.getBarometerData(): flights = " + flights + "\nminutes = " + minutes);
            }
        } catch (SQLException sqle) {
            log.debug(sqle);
            throw sqle;
        }
        result.put("flights", new Integer(flights));
        result.put("minutes", new Integer(minutes));
        return result;
    }
}
