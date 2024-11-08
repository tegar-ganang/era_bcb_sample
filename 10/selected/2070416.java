package coyousoft.javaee._05_jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pstmt {

    private static final String LS = System.getProperty("line.separator");

    public boolean insert(Persons person) {
        int affectedRows = 0;
        StringBuilder sb = new StringBuilder(500);
        sb.append("insert into PERSONS  ").append(LS);
        sb.append("    (PERSON_NAME, PERSON_AGE, PERSON_CITY, PERSON_UDATE) ").append(LS);
        sb.append("values               ").append(LS);
        sb.append("    (?, ?, ?, NOW()) ").append(LS);
        if (true) {
            System.out.println(sb.toString());
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            pstmt = conn.prepareStatement(sb.toString());
            pstmt.setString(1, person.getPersonName());
            pstmt.setLong(2, person.getPersonAge());
            pstmt.setString(3, person.getPersonCity());
            affectedRows = pstmt.executeUpdate();
            System.out.printf("affectedRows = %d%n%n", affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, null);
        }
        return affectedRows > 0;
    }

    public boolean delete(Long personId) {
        int affectedRows = 0;
        String sql = "delete from PERSONS where PERSON_ID = ?";
        if (true) {
            System.out.println(sql);
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, personId);
            affectedRows = pstmt.executeUpdate();
            System.out.printf("affectedRows = %d%n%n", affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, null);
        }
        return affectedRows > 0;
    }

    public boolean update(String personName, Long personAge, String personCity, Long personId) {
        int affectedRows = 0;
        StringBuilder sb = new StringBuilder(500);
        sb.append("update PERSONS          ").append(LS);
        sb.append("   set PERSON_NAME  = ? ").append(LS);
        sb.append("      ,PERSON_AGE   = ? ").append(LS);
        sb.append("      ,PERSON_CITY  = ? ").append(LS);
        sb.append("      ,PERSON_UDATE = NOW() ").append(LS);
        sb.append(" where PERSON_ID = ?    ").append(LS);
        if (true) {
            System.out.printf("%s%s, %d, %s, %d%n", sb.toString(), personName, personAge, personCity, personId);
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            pstmt = conn.prepareStatement(sb.toString());
            pstmt.setString(1, personName);
            pstmt.setLong(2, personAge);
            pstmt.setString(3, personCity);
            pstmt.setLong(4, personId);
            affectedRows = pstmt.executeUpdate();
            System.out.printf("affectedRows = %d%n%n", affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, null);
        }
        return affectedRows > 0;
    }

    public List<Persons> getPersonsList(Long personId, String keyword) {
        Map<Long, Persons> personMap = new LinkedHashMap<Long, Persons>();
        StringBuilder sb = new StringBuilder(500);
        sb.append("select T1.PERSON_ID, T1.PERSON_NAME, T1.PERSON_CDATE, T2.PET_NAME, T2.PET_TYPE").append(LS);
        sb.append("  from PERSONS T1 ").append(LS);
        sb.append("  left join PETS T2 on T2.PERSON_ID = T1.PERSON_ID ").append(LS);
        sb.append(" where T1.PERSON_ID >= ?      ").append(LS);
        sb.append("   and T1.PERSON_NAME like ?  ").append(LS);
        sb.append("   and T1.PERSON_CDATE < ?  ").append(LS);
        sb.append(" order by T1.PERSON_NAME desc ").append(LS);
        if (false) {
            System.out.println(sb.toString());
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            pstmt = conn.prepareStatement(sb.toString());
            pstmt.setLong(1, personId);
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
            rs = pstmt.executeQuery();
            Persons person = null;
            Pets pet = null;
            while (rs.next()) {
                person = (Persons) personMap.get(rs.getLong("PERSON_ID"));
                if (person == null) {
                    person = new Persons();
                    person.setPersonId(rs.getLong("PERSON_ID"));
                    person.setPersonName(rs.getString("PERSON_NAME"));
                    person.setPersonCdate(rs.getTimestamp("PERSON_CDATE"));
                    personMap.put(person.getPersonId(), person);
                }
                if (rs.getString("PET_NAME") != null) {
                    pet = new Pets();
                    pet.setPetName(rs.getString("PET_NAME"));
                    pet.setPetType(rs.getString("PET_TYPE"));
                    person.getPetList().add(pet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, pstmt, rs);
        }
        return new ArrayList<Persons>(personMap.values());
    }

    public boolean transaction(Long personId) {
        boolean result = true;
        String delPets = "delete from PETS where PERSON_ID = ?";
        String delPersons = "delete from PERSONS where PERSON_ID = ?";
        if (true) {
            System.out.println(delPets);
            System.out.println(delPersons);
        }
        Connection conn = null;
        PreparedStatement pstmtDelPets = null;
        PreparedStatement pstmtDelPersons = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            conn.setAutoCommit(false);
            pstmtDelPets = conn.prepareStatement(delPets);
            pstmtDelPets.setLong(1, personId);
            int affectedRows = pstmtDelPets.executeUpdate();
            pstmtDelPets.close();
            System.out.println("affectedRows = " + affectedRows);
            if (true) {
                throw new SQLException("fasfdsaf");
            }
            pstmtDelPersons = conn.prepareStatement(delPersons);
            pstmtDelPersons.setLong(1, personId);
            affectedRows = pstmtDelPersons.executeUpdate();
            System.out.println("affectedRows = " + affectedRows);
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e.printStackTrace(System.out);
            }
            e.printStackTrace(System.out);
            result = false;
        } finally {
            ConnHelper.close(conn, pstmtDelPersons, null);
        }
        return result;
    }
}
