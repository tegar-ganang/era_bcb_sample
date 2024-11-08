package org.ekstrabilet.stadium.logic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import org.ekstrabilet.database.connection.ConnectionManager;
import org.ekstrabilet.database.statements.Statements;
import org.ekstrabilet.logic.exceptions.StadiumException;
import org.ekstrabilet.stadium.beans.Stadium;
import org.ekstrabilet.stadium.beans.Tribune;

/**
 * Singleton, provides logic for Stadium object, is a connector
 * between database and Stadium object, implements methods responsible
 * for manipulating that object in database
 * @author Marcin
 *
 */
public class StadiumLogic {

    private static StadiumLogic stadiumLogic;

    private StadiumLogic() {
    }

    /**
	 * creates new entity in database based on Stadium object, that operation
	 * is transacion if one of steps fail its being rolled back
	 * @param stadium 
	 * @throws StadiumException
	 */
    public void addStadium(Stadium stadium) throws StadiumException {
        Connection conn = ConnectionManager.getManager().getConnection();
        if (findStadiumBy_N_C(stadium.getName(), stadium.getCity()) != -1) throw new StadiumException("Stadium already exists");
        try {
            PreparedStatement stm = conn.prepareStatement(Statements.INSERT_STADIUM);
            conn.setAutoCommit(false);
            stm.setString(1, stadium.getName());
            stm.setString(2, stadium.getCity());
            stm.executeUpdate();
            int id = getMaxId();
            TribuneLogic logic = TribuneLogic.getInstance();
            for (Tribune trib : stadium.getTribunes()) {
                int tribuneId = logic.addTribune(trib);
                if (tribuneId != -1) {
                    stm = conn.prepareStatement(Statements.INSERT_STAD_TRIBUNE);
                    stm.setInt(1, id);
                    stm.setInt(2, tribuneId);
                    stm.executeUpdate();
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new StadiumException("Adding stadium failed", e);
        }
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * removes stadium from database, that operation is a transaction,
	 * if single step fail its being rolled back
	 * @param name name of stadium	
	 * @param city stadium city
	 * @throws StadiumException
	 */
    public void removeStadium(String name, String city) throws StadiumException {
        Connection conn = ConnectionManager.getManager().getConnection();
        int id = findStadiumBy_N_C(name, city);
        if (id == -1) throw new StadiumException("No such stadium");
        try {
            conn.setAutoCommit(false);
            PreparedStatement stm = conn.prepareStatement(Statements.SELECT_STAD_TRIBUNE);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            TribuneLogic logic = TribuneLogic.getInstance();
            while (rs.next()) {
                logic.removeTribune(rs.getInt("tribuneID"));
            }
            stm = conn.prepareStatement(Statements.DELETE_STADIUM);
            stm.setInt(1, id);
            stm.executeUpdate();
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new StadiumException("Removing stadium failed", e);
        }
        try {
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * gets stadium from database and returns it as Stadium object
	 * @param name name of stadium
	 * @param city stadium city
	 * @return Stadium object
	 * @throws StadiumException
	 */
    public Stadium getStadium(String name, String city) throws StadiumException {
        Connection conn = ConnectionManager.getManager().getConnection();
        int id = findStadiumBy_N_C(name, city);
        if (id == -1) throw new StadiumException("No such stadium");
        Stadium stadium = new Stadium();
        TribuneLogic logic = TribuneLogic.getInstance();
        Tribune[] tribunes = new Tribune[4];
        PreparedStatement stm;
        try {
            stm = conn.prepareStatement(Statements.SELECT_STAD_TRIBUNE);
            stm.setInt(1, id);
            ResultSet rs = stm.executeQuery();
            int i = 0;
            while (rs.next()) {
                int tribuneId = rs.getInt("tribuneID");
                tribunes[i] = logic.getTribuneById(tribuneId);
                i++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int capacity = 0;
        for (Tribune trib : tribunes) {
            capacity += trib.getCapacity();
        }
        stadium.setName(name);
        stadium.setCity(city);
        stadium.setTribunes(tribunes);
        stadium.setCapacity(capacity);
        return stadium;
    }

    /**
	 * finds stadium in database by stadium name and city
	 * @param name name of stadium
	 * @param city stadium city
	 * @return id of found stadium or -1 if stadium doesn't exist
	 */
    public int findStadiumBy_N_C(String name, String city) {
        Connection conn = ConnectionManager.getManager().getConnection();
        int id = -1;
        try {
            PreparedStatement stm = conn.prepareStatement(Statements.SELECT_STADIUM);
            stm.setString(1, name);
            stm.setString(2, city);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                id = rs.getInt("stadiumID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
	 * finds stadium sector by stadium name, city, tribune number and sector token
	 * @param stadiumName name of stadium
	 * @param stadiumCity stadium city
	 * @param tribuneNum tribune number
	 * @param sectorToken sector token
	 * @return id of found sector or -1 if sector doesn't exist
	 */
    public int findSectorIdBy_SN_SC_TN_ST(String stadiumName, String stadiumCity, int tribuneNum, String sectorToken) {
        Connection conn = ConnectionManager.getManager().getConnection();
        int id = -1;
        int stadiumId = findStadiumBy_N_C(stadiumName, stadiumCity);
        if (stadiumId != -1) {
            try {
                PreparedStatement stm = conn.prepareStatement(Statements.SELECT_STAD_SECTOR);
                stm.setString(1, sectorToken);
                stm.setInt(2, tribuneNum);
                stm.setInt(3, stadiumId);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    id = rs.getInt("sectorID");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    private int getMaxId() throws SQLException {
        Connection conn = ConnectionManager.getManager().getConnection();
        try {
            int maxId;
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(Statements.MAX_STADIUM_ID);
            while (rs.next()) {
                maxId = rs.getInt("maxID");
                return maxId;
            }
        } catch (SQLException e) {
            throw e;
        }
        return -1;
    }

    /**
	 * finds stadium names by city name
	 * @param cityName 
	 * @return list of stadium names
	 */
    public LinkedList<String> getStadiumNames(String cityName) {
        String query = "Select name from stadium where city like '%" + cityName + "%' order by name";
        LinkedList<String> sList = new LinkedList<String>();
        Connection conn = ConnectionManager.getManager().getConnection();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                sList.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sList;
    }

    /**
	 * finds city names by stadium name
	 * @param sName stadium name
	 * @return list of city names
	 */
    public LinkedList<String> getCitiesNames(String sName) {
        String query = "Select city from stadium where name like '%" + sName + "%' order by name";
        LinkedList<String> cList = new LinkedList<String>();
        Connection conn = ConnectionManager.getManager().getConnection();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                cList.add(rs.getString("city"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cList;
    }

    /**
	 * finds stadium names by city name
	 * @param cName city name	
	 * @return array of stadium names
	 */
    public String[] getStadiumsArray(String cName) {
        LinkedList<String> sList = getStadiumNames(cName);
        sList.addFirst("");
        String[] stadiumNames = (String[]) sList.toArray(new String[0]);
        return stadiumNames;
    }

    /**
	 * finds city names by stadium name
	 * @param sName stadium name
	 * @return array of city names
	 */
    public String[] getCitiesArray(String sName) {
        LinkedList<String> cList = getCitiesNames(sName);
        cList.addFirst("");
        String[] cityNames = (String[]) cList.toArray(new String[0]);
        return cityNames;
    }

    /**
	 * 
	 * @return instance of StadiumLogic
	 */
    public static StadiumLogic getInstance() {
        if (stadiumLogic == null) {
            stadiumLogic = new StadiumLogic();
        }
        return stadiumLogic;
    }
}
