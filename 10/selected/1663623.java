package com.rpgeeframework.maps;

import com.rpgeeframework.GameObject;
import com.rpgeeframework.data.DataAccessObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.Date;

/**
 * @author allen
 *
 */
public class Room extends GameObject implements Directions, Descriptions, DataAccessObject {

    private int exits = 0;

    private String roomMap = null;

    private String roomKey = "";

    int x = 0;

    int y = 0;

    public String getRoomMap() {
        return this.roomMap;
    }

    /**
	 * @return the rendered room map, with optional border
	 */
    public String getRenderedRoomMap(String borderCharacter) {
        String borderLeft = borderCharacter.length() > 0 ? borderCharacter + " " : "";
        String borderRight = borderCharacter.length() > 0 ? " " + borderCharacter : "";
        StringBuffer output = new StringBuffer();
        String[] lines;
        if (borderCharacter.length() > 0) {
            lines = getRoomMap().split("\n");
            for (int count = 0; count < lines.length + 4; count++) {
                output.append(borderCharacter);
            }
            output.append("\n");
            output.append(borderLeft);
            for (int count = 0; count < lines.length; count++) {
                output.append(" ");
            }
            output.append(borderRight);
            output.append("\n");
            for (int yCount = 0; yCount < lines.length; yCount++) {
                output.append(borderLeft);
                output.append(lines[yCount]);
                output.append(borderRight);
                output.append("\n");
            }
            output.append(borderLeft);
            for (int count = 0; count < lines.length; count++) {
                output.append(" ");
            }
            output.append(borderRight + "\n");
            for (int count = 0; count < lines.length + 4; count++) {
                output.append(borderCharacter);
            }
            output.append("\n");
        } else {
            output.append(getRoomMap());
        }
        return output.toString();
    }

    /**
	 * @param roomMap the roomMap to set
	 */
    public void setRoomMap(String roomMap) {
        this.roomMap = roomMap;
    }

    /**
	 * @return the exits
	 */
    public int getExits() {
        return exits;
    }

    public Room() {
        init();
    }

    public Room(String roomMap, int exits, int x, int y) {
        init();
        setMap(roomMap);
        setExits(exits);
        setX(x);
        setY(y);
        setProperty("CREATED", (new Date()).toString());
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    private void setMap(String roomMap) {
        this.roomMap = roomMap;
    }

    public void setExits(int exits) {
        this.exits = exits;
    }

    private void init() {
    }

    public String toString() {
        StringBuffer output = new StringBuffer();
        output.append("ROOM [" + getCoordinatesString() + "]\n\nMap\n" + getRenderedRoomMap("=") + getExitsString() + "  (" + getExitsString(SHORT) + ")");
        return output.toString();
    }

    public String getExitsString() {
        return getExitsString(LONG);
    }

    public String getExitsStringShort() {
        return getExitsString(SHORT);
    }

    public String getExitsString(int format) {
        StringBuffer exitString = new StringBuffer();
        BitSet bitset = new BitSet(8);
        int exits = getExits();
        for (int i = 0; i < 8; i++) {
            if ((exits & (1 << i)) > 0) {
                bitset.set(i);
            }
        }
        if (bitset.get(NORTH)) {
            switch(format) {
                case SHORT:
                    exitString.append("N");
                    break;
                case LONG:
                    exitString.append("North");
                    break;
            }
        }
        if (bitset.get(SOUTH)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("S");
                    break;
                case LONG:
                    exitString.append("South");
                    break;
            }
        }
        if (bitset.get(EAST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("E");
                    break;
                case LONG:
                    exitString.append("East");
                    break;
            }
        }
        if (bitset.get(WEST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("W");
                    break;
                case LONG:
                    exitString.append("West");
                    break;
            }
        }
        if (bitset.get(NORTHEAST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("NE");
                    break;
                case LONG:
                    exitString.append("Northeast");
                    break;
            }
        }
        if (bitset.get(NORTHWEST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("NW");
                    break;
                case LONG:
                    exitString.append("Northwest");
                    break;
            }
        }
        if (bitset.get(SOUTHEAST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("SE");
                    break;
                case LONG:
                    exitString.append("Southeast");
                    break;
            }
        }
        if (bitset.get(SOUTHWEST)) {
            if (exitString.length() > 0) {
                exitString.append(",");
            }
            switch(format) {
                case SHORT:
                    exitString.append("SW");
                    break;
                case LONG:
                    exitString.append("Southwest");
                    break;
            }
        }
        return exitString.toString();
    }

    public String getCoordinatesString() {
        return "(" + getX() + "," + getY() + ")";
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    /**
	 * @return the roomKey
	 */
    public String getRoomKey() {
        return roomKey;
    }

    /**
	 * @param roomKey the roomKey to set
	 */
    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    @Override
    public void insert(Connection conn) throws SQLException {
        PreparedStatement objectInsert = null;
        String sqlString = null;
        int newID = 0;
        try {
            conn.setAutoCommit(false);
            sqlString = "SELECT NEXTVAL(OBJ_SEQ) AS NEXTVAL";
            objectInsert = conn.prepareStatement(sqlString);
            ResultSet r = objectInsert.executeQuery(sqlString);
            newID = r.getInt("NEXTVAL");
            sqlString = "INSERT INTO OBJECTS" + "(" + "OBJ_ID," + "OBJ_NAME," + "OBTY_CDE" + ")" + "VALUES" + "(" + "?," + "?," + "?" + ")" + "";
            objectInsert = conn.prepareStatement(sqlString);
            objectInsert.setInt(1, newID);
            objectInsert.setString(2, getRoomKey());
            objectInsert.setString(3, "ROOM");
            objectInsert.executeUpdate();
            sqlString = "INSERT INTO ROOMS" + "(" + "";
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    conn.rollback();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
            }
        } finally {
            if (objectInsert != null) {
                objectInsert.close();
            }
            conn.setAutoCommit(true);
        }
    }

    @Override
    public void update(Connection conn) {
    }

    @Override
    public void delete(Connection conn) {
    }
}
