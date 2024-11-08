package dk.nullesoft.Airlog.DBinstantdb;

import dk.nullesoft.Airlog.*;
import dk.nullesoft.AirlogExceptions.*;
import java.sql.*;

public class PilotDataDB implements dk.nullesoft.Airlog.PilotDataDB {

    private JdbcConnection jdbc;

    public PilotDataDB(JdbcConnection jdbc) {
        this.jdbc = jdbc;
    }

    public PilotData getPilot(int id) {
        try {
            PreparedStatement psta = jdbc.prepareStatement("SELECT id, name, address1, address2, " + "zip, city, state, country, birthdate, pft_theory, pft, medical, passenger, " + "instructor, loc_language, loc_country, loc_variant, username, password " + "FROM pilot " + "WHERE id = ?");
            psta.setInt(1, id);
            ResultSet resl = psta.executeQuery();
            PilotData pilotData = new PilotData();
            if (resl.next()) {
                pilotData.id = resl.getInt(1);
                pilotData.name = resl.getString(2);
                pilotData.address1 = resl.getString(3);
                pilotData.address2 = resl.getString(4);
                pilotData.zip = resl.getString(5);
                pilotData.city = resl.getString(6);
                pilotData.state = resl.getString(7);
                pilotData.country = resl.getString(8);
                if (resl.getString(9) != null) pilotData.birthdate = new java.util.Date(resl.getLong(9)); else pilotData.birthdate = new java.util.Date(0);
                if (resl.getString(10) != null) pilotData.pft_theory = new java.util.Date(resl.getLong(10)); else pilotData.pft_theory = new java.util.Date(0);
                if (resl.getString(11) != null) pilotData.pft = new java.util.Date(resl.getLong(11)); else pilotData.pft = new java.util.Date(0);
                if (resl.getString(12) != null) pilotData.medical = new java.util.Date(resl.getLong(12)); else pilotData.medical = new java.util.Date(0);
                if (resl.getString(13).equals("Y")) pilotData.passenger = true; else pilotData.passenger = false;
                if (resl.getString(14).equals("Y")) pilotData.instructor = true; else pilotData.instructor = false;
                pilotData.loc_language = resl.getString(15);
                pilotData.loc_country = resl.getString(16);
                pilotData.loc_variant = resl.getString(17);
                pilotData.username = resl.getString(18);
                pilotData.password = resl.getString(19);
            }
            return pilotData;
        } catch (SQLException sql) {
            sql.printStackTrace();
        }
        return null;
    }

    public void setPilot(PilotData pilotData) throws UsernameNotValidException {
        try {
            if (pilotData.username.trim().equals("") || pilotData.password.trim().equals("")) throw new UsernameNotValidException(1, "Username or password missing");
            PreparedStatement psta;
            if (pilotData.id == 0) {
                psta = jdbc.prepareStatement("INSERT INTO pilot " + "(name, address1, address2, zip, city, state, country, birthdate, " + "pft_theory, pft, medical, passenger, instructor, loc_language, " + "loc_country, loc_variant, username, password, id) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                pilotData.id = Sequence.nextVal("pilot_id", jdbc);
            } else {
                psta = jdbc.prepareStatement("UPDATE pilot SET " + "name = ?, address1 = ?, address2 = ?, " + "zip = ?, city = ?, state = ?, country = ?, birthdate = ?, pft_theory = ?," + "pft = ?, medical = ?, passenger = ?, instructor = ?, loc_language = ?, " + "loc_country = ?, loc_variant = ?, username = ?, password = ? " + "WHERE id = ?");
            }
            psta.setString(1, pilotData.name);
            psta.setString(2, pilotData.address1);
            psta.setString(3, pilotData.address2);
            psta.setString(4, pilotData.zip);
            psta.setString(5, pilotData.city);
            psta.setString(6, pilotData.state);
            psta.setString(7, pilotData.country);
            if (pilotData.birthdate != null) psta.setLong(8, pilotData.birthdate.getTime()); else psta.setNull(8, java.sql.Types.INTEGER);
            if (pilotData.pft_theory != null) psta.setLong(9, pilotData.pft_theory.getTime()); else psta.setNull(9, java.sql.Types.INTEGER);
            if (pilotData.pft != null) psta.setLong(10, pilotData.pft.getTime()); else psta.setNull(10, java.sql.Types.INTEGER);
            if (pilotData.medical != null) psta.setLong(11, pilotData.medical.getTime()); else psta.setNull(11, java.sql.Types.INTEGER);
            if (pilotData.passenger) psta.setString(12, "Y"); else psta.setString(12, "N");
            if (pilotData.instructor) psta.setString(13, "Y"); else psta.setString(13, "N");
            psta.setString(14, pilotData.loc_language);
            psta.setString(15, pilotData.loc_country);
            psta.setString(16, pilotData.loc_variant);
            psta.setString(17, pilotData.username);
            psta.setString(18, pilotData.password);
            psta.setInt(19, pilotData.id);
            psta.executeUpdate();
            jdbc.commit();
        } catch (SQLException sql) {
            jdbc.rollback();
            sql.printStackTrace();
            throw new UsernameNotValidException(2, "Username allready exist");
        }
    }
}
