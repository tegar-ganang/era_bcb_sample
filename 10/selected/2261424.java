package edu.ecnu.struts.addressbook.model;

import java.sql.*;
import java.util.*;
import edu.ecnu.struts.addressbook.Constants;

public class AddressBookBean {

    String name;

    String phone;

    String address;

    public AddressBookBean() {
    }

    public AddressBookBean(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void insert() throws Exception {
        Connection con = DbUtil.connectToDb();
        PreparedStatement pStmt = null;
        try {
            pStmt = con.prepareStatement("INSERT INTO " + Constants.TABLENAME + " (name,phone,address)" + " values(?,?,?)");
            con.setAutoCommit(false);
            pStmt.setString(1, name);
            pStmt.setString(2, phone);
            pStmt.setString(3, address);
            int j = pStmt.executeUpdate();
            con.commit();
        } catch (Exception ex) {
            try {
                con.rollback();
            } catch (SQLException sqlex) {
                sqlex.printStackTrace(System.out);
            }
            throw ex;
        } finally {
            try {
                pStmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Vector search(String strSql) throws Exception {
        Vector addressbookBeans = new Vector();
        Connection con = DbUtil.connectToDb();
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        try {
            pStmt = con.prepareStatement(strSql);
            rs = pStmt.executeQuery();
            while (rs.next()) addressbookBeans.add(new AddressBookBean(rs.getString("NAME"), rs.getString("PHONE"), rs.getString("ADDRESS")));
            return addressbookBeans;
        } finally {
            try {
                rs.close();
                pStmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
