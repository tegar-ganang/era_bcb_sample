package coyousoft.javaee._05_jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class Stmt {

    private static final String LS = System.getProperty("line.separator");

    public void insert() {
        String personName = "七匹狼";
        int personAge = new Random().nextInt(100);
        String personCity = "中国南京";
        StringBuilder sb = new StringBuilder(200);
        sb.append("insert into PERSONS ").append(LS);
        sb.append("    (PERSON_NAME, PERSON_AGE, PERSON_CITY, PERSON_UDATE) ").append(LS);
        sb.append("values ").append(LS);
        sb.append("    ('").append(personName).append("', ").append(personAge).append(", '").append(personCity).append("', NOW())  ").append(LS);
        if (true) {
            System.out.println(sb.toString());
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            stmt = conn.createStatement();
            int affectedRows = stmt.executeUpdate(sb.toString());
            System.out.println("affectedRows = " + affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, stmt, rs);
        }
    }

    public void delete() {
        String sql = "delete from PERSONS where PERSON_ID = 7";
        if (true) {
            System.out.println(sql);
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            stmt = conn.createStatement();
            int affectedRows = stmt.executeUpdate(sql);
            System.out.println("affectedRows = " + affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, stmt, rs);
        }
    }

    public void update() {
        String personName = "七匹狼";
        int personAge = new Random().nextInt(100);
        String personCity = "中国南京";
        StringBuilder sb = new StringBuilder(200);
        sb.append("update PERSONS              ").append(LS);
        sb.append("   set PERSON_NAME  = '").append(personName).append("'").append(LS);
        sb.append("      ,PERSON_AGE   = ").append(personAge).append(LS);
        sb.append("      ,PERSON_CITY  = '").append(personCity).append("'").append(LS);
        sb.append("      ,PERSON_UDATE = NOW() ").append(LS);
        sb.append(" where PERSON_ID = 1").append(LS);
        if (true) {
            System.out.println(sb.toString());
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            stmt = conn.createStatement();
            int affectedRows = stmt.executeUpdate(sb.toString());
            System.out.println("affectedRows = " + affectedRows);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            ConnHelper.close(conn, stmt, rs);
        }
    }

    public void transaction() {
        String delPets = "delete from PETS where PERSON_ID = 1";
        String delPersons = "delete from PERSONS where PERSON_ID = 1";
        if (true) {
            System.out.println(delPets);
            System.out.println(delPersons);
        }
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = ConnHelper.getConnectionByDriverManager();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            int affectedRows = stmt.executeUpdate(delPets);
            System.out.println("affectedRows = " + affectedRows);
            if (true) {
                throw new SQLException("fasfdsaf");
            }
            affectedRows = stmt.executeUpdate(delPersons);
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
        } finally {
            ConnHelper.close(conn, stmt, null);
        }
    }

    public static void main(String[] args) {
        Stmt stmt = new Stmt();
        stmt.transaction();
    }
}
