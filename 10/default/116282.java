import java.io.*;
import java.sql.*;
import java.util.*;

public class StockTrackerDB {

    private Connection con = null;

    public StockTrackerDB() throws ClassNotFoundException, SQLException {
        if (con == null) {
            String url = "jdbc:odbc:StockTracker";
            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            } catch (ClassNotFoundException ex) {
                throw new ClassNotFoundException(ex.getMessage() + "\nCannot locate sun.jdbc.odbc.JdbcOdbcDriver");
            }
            try {
                con = DriverManager.getConnection(url);
            } catch (SQLException ex) {
                throw new SQLException(ex.getMessage() + "\nCannot open database connection for " + url);
            }
        }
    }

    public void close() throws SQLException, IOException, ClassNotFoundException {
        con.close();
        con = null;
    }

    public void addStock(String stockSymbol, String stockDesc) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate("INSERT INTO Stocks VALUES ('" + stockSymbol + "'" + ",'" + stockDesc + "')");
        stmt.close();
    }

    public boolean addUser(User user) throws SQLException, IOException, ClassNotFoundException {
        boolean result = false;
        String dbUserID;
        String dbLastName;
        String dbFirstName;
        Password dbPswd;
        boolean isAdmin;
        dbUserID = user.getUserID();
        if (getUser(dbUserID) == null) {
            dbLastName = user.getLastName();
            dbFirstName = user.getFirstName();
            Password pswd = user.getPassword();
            isAdmin = user.isAdmin();
            PreparedStatement pStmt = con.prepareStatement("INSERT INTO Users VALUES (?,?,?,?,?)");
            pStmt.setString(1, dbUserID);
            pStmt.setString(2, dbLastName);
            pStmt.setString(3, dbFirstName);
            pStmt.setBytes(4, ObjUtil.serializeObj(pswd));
            pStmt.setBoolean(5, isAdmin);
            pStmt.executeUpdate();
            pStmt.close();
            result = true;
        } else throw new IOException("User exists - cannot add.");
        return result;
    }

    public void addUserStocks(String userID, String stockSymbol) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate("INSERT INTO UserStocks VALUES ('" + userID + "'" + ",'" + stockSymbol + "')");
        stmt.close();
    }

    public boolean updUser(User user) throws SQLException, IOException, ClassNotFoundException {
        boolean result = false;
        String dbUserID;
        String dbLastName;
        String dbFirstName;
        Password dbPswd;
        boolean isAdmin;
        dbUserID = user.getUserID();
        if (getUser(dbUserID) != null) {
            dbLastName = user.getLastName();
            dbFirstName = user.getFirstName();
            Password pswd = user.getPassword();
            isAdmin = user.isAdmin();
            PreparedStatement pStmt = con.prepareStatement("UPDATE Users SET lastName = ?," + " firstName = ?, pswd = ?, admin = ? WHERE userID = ?");
            pStmt.setString(1, dbLastName);
            pStmt.setString(2, dbFirstName);
            pStmt.setBytes(3, ObjUtil.serializeObj(pswd));
            pStmt.setBoolean(4, isAdmin);
            pStmt.setString(5, dbUserID);
            pStmt.executeUpdate();
            pStmt.close();
            result = true;
        } else throw new IOException("User does not exist - cannot update.");
        return result;
    }

    private void delStock(String stockSymbol) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        stmt.executeUpdate("DELETE FROM Stocks WHERE " + "symbol = '" + stockSymbol + "'");
        stmt.close();
    }

    public void delUser(User user) throws SQLException, IOException, ClassNotFoundException {
        String dbUserID;
        String stockSymbol;
        Statement stmt = con.createStatement();
        try {
            con.setAutoCommit(false);
            dbUserID = user.getUserID();
            if (getUser(dbUserID) != null) {
                ResultSet rs1 = stmt.executeQuery("SELECT userID, symbol " + "FROM UserStocks WHERE userID = '" + dbUserID + "'");
                while (rs1.next()) {
                    try {
                        stockSymbol = rs1.getString("symbol");
                        delUserStocks(dbUserID, stockSymbol);
                    } catch (SQLException ex) {
                        throw new SQLException("Deletion of user stock holding failed: " + ex.getMessage());
                    }
                }
                try {
                    stmt.executeUpdate("DELETE FROM Users WHERE " + "userID = '" + dbUserID + "'");
                } catch (SQLException ex) {
                    throw new SQLException("User deletion failed: " + ex.getMessage());
                }
            } else throw new IOException("User not found in database - cannot delete.");
            try {
                con.commit();
            } catch (SQLException ex) {
                throw new SQLException("Transaction commit failed: " + ex.getMessage());
            }
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException sqx) {
                throw new SQLException("Transaction failed then rollback failed: " + sqx.getMessage());
            }
            throw new SQLException("Transaction failed; was rolled back: " + ex.getMessage());
        }
        stmt.close();
    }

    public void delUserStocks(String userID, String stockSymbol) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        ResultSet rs;
        stmt.executeUpdate("DELETE FROM UserStocks WHERE " + "userID = '" + userID + "'" + "AND symbol = '" + stockSymbol + "'");
        rs = stmt.executeQuery("SELECT symbol FROM UserStocks " + "WHERE symbol = '" + stockSymbol + "'");
        if (!rs.next()) delStock(stockSymbol);
        stmt.close();
    }

    public String getStockDesc(String stockSymbol) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        String stockDesc = null;
        ResultSet rs = stmt.executeQuery("SELECT symbol, name FROM Stocks " + "WHERE symbol = '" + stockSymbol + "'");
        if (rs.next()) stockDesc = rs.getString("name");
        rs.close();
        stmt.close();
        return stockDesc;
    }

    public User getUser(String userID) throws SQLException, IOException, ClassNotFoundException {
        Statement stmt = con.createStatement();
        String dbUserID;
        String dbLastName;
        String dbFirstName;
        Password dbPswd;
        boolean isAdmin;
        byte[] buf = null;
        User user = null;
        ResultSet rs = stmt.executeQuery("SELECT * FROM Users WHERE userID = '" + userID + "'");
        if (rs.next()) {
            dbUserID = rs.getString("userID");
            dbLastName = rs.getString("lastName");
            dbFirstName = rs.getString("firstName");
            buf = rs.getBytes("pswd");
            dbPswd = (Password) ObjUtil.deserializeObj(buf);
            isAdmin = rs.getBoolean("admin");
            user = new User(dbUserID, dbFirstName, dbLastName, dbPswd, isAdmin);
        }
        rs.close();
        stmt.close();
        return user;
    }

    public ArrayList listUsers() throws SQLException, IOException, ClassNotFoundException {
        ArrayList aList = new ArrayList();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT userID, firstName, lastName, admin " + "FROM Users");
        while (rs.next()) {
            aList.add(rs.getString("userID"));
            aList.add(rs.getString("firstName"));
            aList.add(rs.getString("lastName"));
            aList.add(new Boolean(rs.getBoolean("admin")));
        }
        rs.close();
        stmt.close();
        return aList;
    }

    public ArrayList listUserStocks(String userID) throws SQLException, IOException, ClassNotFoundException {
        ArrayList aList = new ArrayList();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM UserStocks " + "WHERE userID = '" + userID + "' ORDER BY symbol");
        while (rs.next()) aList.add(rs.getString("symbol"));
        rs.close();
        stmt.close();
        return aList;
    }
}
