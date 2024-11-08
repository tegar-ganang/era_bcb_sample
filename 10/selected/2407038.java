package dk.highflier.airlog.dataaccess.dbinstantdb;

import dk.highflier.airlog.*;
import dk.highflier.airlog.common.*;
import dk.highflier.airlog.utility.*;
import dk.highflier.airlog.entity.*;
import dk.highflier.airlog.dataaccess.dbinstantdb.common.*;
import java.sql.*;
import java.text.*;
import java.util.*;

public class FlightDAImpl implements dk.highflier.airlog.dataaccess.FlightDA {

    private JdbcConnection conn;

    private org.log4j.Category log = org.log4j.Category.getInstance("Log.FlightDB");

    private PreparedStatement psta_insert = null;

    public FlightDAImpl(JdbcConnection conn) throws SQLException {
        try {
            this.conn = conn;
            psta_insert = conn.prepareStatement("insert into flyvning values (" + Sequence.nextVal("flyvning", conn) + ",?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        } catch (SQLException sqle) {
            log.error(sqle);
            throw sqle;
        }
    }

    /**
     * Method will insert a new record with _flight data for current pilot
     *
     * @param _Flight[]	
     *
     * @exception None
     *
     * @return int[] values of seq_flyvning_id
     */
    public int[] insertFlight(Flight _flight[], int currentPilot) throws SQLException {
        int[] result = new int[_flight.length];
        try {
            for (int x = 0; x < _flight.length; x++) {
                psta_insert.setInt(1, currentPilot);
                psta_insert.setInt(2, _flight[x].nr);
                psta_insert.setLong(3, _flight[x].dato.getTime());
                psta_insert.setInt(4, _flight[x].flytype_id);
                psta_insert.setString(5, _flight[x].startart);
                psta_insert.setInt(6, _flight[x].slaebetid);
                psta_insert.setInt(7, _flight[x].motortid);
                psta_insert.setInt(8, _flight[x].svaevetid);
                psta_insert.setInt(9, _flight[x].distance);
                psta_insert.setInt(10, _flight[x].startsted_id);
                psta_insert.setInt(11, _flight[x].landingssted_id);
                psta_insert.setString(12, _flight[x].note);
                psta_insert.setString(13, convertFromBoolean(_flight[x].straek));
                psta_insert.setString(14, convertFromBoolean(_flight[x].udelanding));
                psta_insert.setString(15, convertFromBoolean(_flight[x].kaptajn));
                psta_insert.setString(16, convertFromBoolean(_flight[x].instruktoer));
                psta_insert.setString(17, convertFromBoolean(_flight[x].forsaede));
                psta_insert.setString(18, convertFromBoolean(_flight[x].passager));
                psta_insert.setString(19, convertFromBoolean(_flight[x].afbrudtstart));
                psta_insert.executeUpdate();
                conn.commit();
                result[x] = getCurrval("flyvning");
            }
        } catch (SQLException sqle) {
            conn.rollback();
            log.debug(sqle);
            throw sqle;
        }
        return result;
    }

    /**
     * Method will delete a record with _flight data
     *
     * @param id	
     * @param currentPilot
     * @exception SQLEXception
     *
     * @return void
     */
    public void deleteFlight(int id, int currentPilot) throws SQLException {
        String sSQL = new String();
        try {
            sSQL = "delete from flyvning where pilot_id = " + currentPilot + " AND id = " + id;
            log.debug(sSQL);
            conn.execute(sSQL);
            conn.commit();
        } catch (SQLException sqle) {
            conn.rollback();
            log.error(sqle);
            throw sqle;
        }
    }

    /**
     * Method will update a record with _flight data for current pilot
     *
     * @param _flight filled out _Flight object
     *
     * @exception SQLException
     */
    public void update(Flight _flight, int currentPilot) throws SQLException {
        String sSQL = new String();
        try {
            sSQL = "update flyvning " + "set pilot_id = " + currentPilot + ", " + "nr = " + _flight.nr + ", " + "dato = " + _flight.dato.getTime() + ", " + "flytype_id = " + _flight.flytype_id + ", " + "startart = '" + _flight.startart + "', " + "slaebetid = " + _flight.slaebetid + ", " + "motortid = " + _flight.motortid + ", " + "svaevetid = " + _flight.svaevetid + ", " + "distance = " + _flight.distance + ", " + "startsted_id = " + _flight.startsted_id + ", " + "landingssted_id = " + _flight.landingssted_id + ", " + "note = '" + _flight.note + "', " + "straek = '" + convertFromBoolean(_flight.straek) + "', " + "udelanding = '" + convertFromBoolean(_flight.udelanding) + "', " + "kaptajn = '" + convertFromBoolean(_flight.kaptajn) + "', " + "instruktoer = '" + convertFromBoolean(_flight.instruktoer) + "', " + "forsaede = '" + convertFromBoolean(_flight.forsaede) + "', " + "passager = '" + convertFromBoolean(_flight.passager) + "', " + "afbrudtstart = '" + convertFromBoolean(_flight.afbrudtstart) + "' where id = " + _flight.id;
            log.debug(sSQL);
            conn.execute(sSQL);
            conn.commit();
            log.debug("update succeeded");
        } catch (SQLException sqle) {
            conn.rollback();
            log.debug(sqle);
            throw sqle;
        }
    }

    /**
     * Method will fetch a record with the given flight number for current pilot
     *
     * @param nr flight number
     *
     * @exception Exception
     * @exception SQLException
     *
     * @return	_Flight if record is found, otherwise null
     */
    public Flight get(int id, int currentPilot) throws Exception, SQLException {
        ResultSet rs = null;
        Flight _flight = new Flight();
        try {
            Statement stmt = conn.getStatement();
            rs = stmt.executeQuery("select id, nr, dato, flytype_id, startart, slaebetid, " + "motortid, svaevetid, distance, " + "startsted_id, landingssted_id, " + "note, straek, udelanding, kaptajn, instruktoer, " + "forsaede, passager, afbrudtstart " + "from flyvning " + "where pilot_id = " + currentPilot + " " + "and id = " + id);
            if (rs.next()) {
                _flight.id = rs.getInt(1);
                _flight.nr = rs.getInt(2);
                _flight.dato = new java.util.Date(rs.getLong(3));
                _flight.flytype_id = rs.getInt(4);
                _flight.startart = rs.getString(5);
                _flight.slaebetid = rs.getInt(6);
                _flight.motortid = rs.getInt(7);
                _flight.svaevetid = rs.getInt(8);
                _flight.distance = rs.getInt(9);
                _flight.startsted_id = rs.getInt(10);
                _flight.landingssted_id = rs.getInt(11);
                _flight.note = rs.getString(12);
                _flight.straek = convertFromString(rs.getString(13));
                _flight.udelanding = convertFromString(rs.getString(14));
                _flight.kaptajn = convertFromString(rs.getString(15));
                _flight.instruktoer = convertFromString(rs.getString(16));
                _flight.forsaede = convertFromString(rs.getString(17));
                _flight.passager = convertFromString(rs.getString(18));
                _flight.afbrudtstart = convertFromString(rs.getString(19));
            } else _flight = null;
            stmt.close();
        } catch (SQLException sqle) {
            log.error(sqle);
            _flight = null;
            throw sqle;
        } catch (Exception e) {
            log.error(e);
            _flight = null;
            throw e;
        } finally {
            rs = null;
        }
        return _flight;
    }

    /**
     * Method returns Object[] containing  flight data for current pilot<BR>
     * Intended use is for tabledata on the flight dialog form <br>
     *
     * Returns id, nr, dato, flytype.navn, svaevetid, distance, startsted, landingssted
     *
     * @param none
     *
     * @exception SQLException
     *
     * @return Vector of vectors, null if no records are found
     */
    public Vector getFlightDialogTableData(int currentPilot) throws SQLException {
        Object data[] = null;
        Statement stmt;
        ResultSet rs = null;
        int columnCount;
        int y = 0;
        String s = "";
        Vector rows = null, row = null;
        try {
            String sQuery = "select nr, dato, flytype.navn, " + "svaevetid, distance, airfield1.name, airfield2.name, flyvning.id " + "from flyvning, flytype, airfield as airfield1, airfield as airfield2  " + "where flytype.id = flytype_id " + "and pilot_id = " + currentPilot + " " + "and airfield1.id = startsted_id " + "and airfield2.id = landingssted_id " + "order by nr";
            stmt = conn.getStatement();
            rs = stmt.executeQuery(sQuery);
            columnCount = rs.getMetaData().getColumnCount();
            rows = new Vector(200);
            if (!rs.next()) {
                log.info("Oops, no rows selected in flyvning, creating fake");
                row = new Vector(8);
                row.addElement(new Integer(0));
                row.addElement(Calendar.getInstance().getTime());
                row.addElement(s);
                row.addElement(new Integer(0));
                row.addElement(new Integer(0));
                row.addElement(s);
                row.addElement(s);
                row.addElement(new Integer(0));
                rows.addElement(row);
            } else {
                do {
                    row = new Vector(columnCount);
                    for (y = 0; y < columnCount; y++) {
                        row.addElement(rs.getObject(y + 1));
                        if (row.get(y) == null) {
                            row.set(y, s);
                        }
                    }
                    row.set(1, new java.util.Date(rs.getLong(2)));
                    rows.addElement(row);
                } while (rs.next());
            }
        } catch (SQLException sqle) {
            log.debug(sqle);
            throw sqle;
        }
        if (rs != null) rs.close();
        if (stmt != null) stmt.close();
        return rows;
    }

    /**
     * Method returns next max(nr)+1 for current pilot 
     *
     * @param	None
     *
     * @exception Exception
     , @exception SQLException
     *
     * @return	Next flight number to enter
     */
    public int getNextFlightNr(int currentPilot) throws Exception, SQLException {
        int result = 0;
        try {
            Statement stmt = conn.getStatement();
            ResultSet rs = stmt.executeQuery("select max(nr) from flyvning " + " where pilot_id = " + currentPilot);
            if (rs.next()) result = rs.getInt(1) + 1; else result = 1;
            stmt.close();
        } catch (SQLException sqle) {
            log.debug(sqle);
            throw sqle;
        }
        return result;
    }

    /**
     * Method will return number of rows in table flyvning for current pilot
     *
     * @param None
     *
     * @exception None
     *
     * @return	Number of rows in table flyvning
     */
    public int getNumberOfFlights(int currentPilot) {
        int x = 0;
        try {
            Statement stmt = conn.getStatement();
            ResultSet rs = stmt.executeQuery("select count(id) from flyvning " + " where pilot_id = " + currentPilot);
            if (rs.next()) x = rs.getInt(1); else x = 0;
            stmt.close();
        } catch (SQLException sqle) {
            log.debug(sqle);
        } catch (Exception e) {
            log.debug(e);
        }
        return x;
    }

    /**
     * Returns current value of a sequence
     *
     * @param sequence name of sequence
     *
     * @exception SQLException
     *
     * @return int value of sequence
     */
    public int getCurrval(String sequence) throws SQLException {
        log.debug("sequence: " + sequence);
        log.debug("conn: " + conn);
        return Sequence.currVal(sequence, conn);
    }

    /**
     * Method will convert a boolean to Y or N
     *
     * @param bol
     *
     * @exception None
     *
     * @return	Y for true, N for false
     */
    private static String convertFromBoolean(boolean bol) {
        String Result;
        if (bol) Result = "Y"; else Result = "N";
        return Result;
    }

    /**
     * Method will convert a char to boolean 
     *
     * @param ch 
     *
     * @exception None
     *
     * @return	true for Y, false for anything else
     */
    private static boolean convertFromString(String ch) {
        boolean Result;
        if (ch.equals("Y")) Result = true; else Result = false;
        return Result;
    }

    /**
     * Method will set current pilot
     *
     * @param pilot_id
     *
     * @exception PilotNotFound
     *
     * @return none
     */
    public void setCurrentPilot(int pilot_id) throws SQLException, PilotNotFoundException {
        try {
            Statement stmt = conn.getStatement();
            ResultSet rs = stmt.executeQuery("select id from pilot where id = " + pilot_id);
            if (!rs.next()) {
                stmt.close();
                throw new PilotNotFoundException();
            }
            stmt.close();
        } catch (SQLException sqle) {
            log.debug(sqle);
            throw sqle;
        }
    }
}