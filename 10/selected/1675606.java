package com.googlecode.project.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;

/**
 * Servlet implementation class for Servlet: DataDeletionServlet
 * 
 */
public class DataDeletionServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    public DataDeletionServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        PrintWriter out = res.getWriter();
        String requestNumber = req.getParameter("reqno");
        int parseNumber = Integer.parseInt(requestNumber);
        Connection con = null;
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            con = DriverManager.getConnection("jdbc:derby:/DerbyDB/AssetDB");
            con.setAutoCommit(false);
            String inet = req.getRemoteAddr();
            Statement stmt = con.createStatement();
            String sql = "UPDATE REQUEST_DETAILS SET viewed = '1', checked_by = '" + inet + "' WHERE QUERY = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, parseNumber);
            pst.executeUpdate();
            con.commit();
            String nextJSP = "/queryRemoved.jsp";
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(nextJSP);
            dispatcher.forward(req, res);
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }
            out.println("Failed");
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
