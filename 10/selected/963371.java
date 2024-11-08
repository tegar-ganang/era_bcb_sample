package com.jmarket.database;

import javax.naming.*;
import javax.sql.*;
import java.sql.*;

/**
 * Performs the required JNDI operations in order to get a connection
 * from an existing connection pool. The goal is to hide JNDI details from 
 * the several classes that need database access.
 * *
 * @author George
 */
public class DBConnection {

    Connection con = null;

    Statement stmt = null;

    ResultSet rs = null;

    /** Creates a new instance of DBConnection */
    public DBConnection() {
        try {
            Context initial = new InitialContext();
            Context context = (Context) initial.lookup("java:comp/env");
            DataSource dataSource = (DataSource) context.lookup("jdbc/jcommerce");
            con = dataSource.getConnection();
            stmt = con.createStatement();
        } catch (NamingException ne) {
            close();
            System.out.println(ne);
        } catch (SQLException sqle) {
            close();
            System.out.println(sqle);
        }
    }

    public Connection getConnection() {
        return con;
    }

    public ResultSet executeQuery(String query) {
        try {
            rs = stmt.executeQuery(query);
        } catch (Exception e) {
            rs = null;
        }
        return rs;
    }

    public int executeUpdate(String query) {
        int done;
        try {
            done = stmt.executeUpdate(query);
        } catch (Exception e) {
            done = -1;
        }
        return done;
    }

    public int executeTransaction(String[] queries) {
        int done;
        try {
            con.setAutoCommit(false);
            for (int i = 0; i < queries.length; i++) {
                done = stmt.executeUpdate(queries[i]);
            }
            con.commit();
            con.setAutoCommit(true);
            done = 1;
        } catch (Exception e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (Exception ex) {
            }
            done = -1;
        }
        return done;
    }

    public void close() {
        try {
            rs.close();
            stmt.close();
            con.close();
        } catch (SQLException sqle) {
        } finally {
            rs = null;
            stmt = null;
            con = null;
        }
    }
}
