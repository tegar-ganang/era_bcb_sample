package servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import beans.DBconn;
import beans.PasswordService;

/**
 * Servlet implementation class for Servlet: AdminStaffAccount
 *
 */
public class AdminStaffAccount extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    public AdminStaffAccount() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uid = request.getParameter("id");
        String pwd = request.getParameter("pwd");
        String email = request.getParameter("email");
        String sid = request.getParameter("sid");
        System.out.println(uid);
        System.out.println(pwd);
        System.out.println(email);
        System.out.println(sid);
        DBconn dbc = new DBconn();
        Connection conn;
        PreparedStatement pst;
        Statement st;
        conn = dbc.getConnection();
        PasswordService ps = PasswordService.getInstance();
        String hash = "";
        try {
            hash = ps.encrypt(pwd);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        String sql = "insert into HP_ADMINISTRATOR.STAFF_REGISTRATION values (?,?,?,?)";
        try {
            pst = conn.prepareStatement(sql);
            pst.setString(1, uid);
            pst.setString(2, hash);
            pst.setString(3, email);
            pst.setString(4, sid);
            System.out.println(pst.toString());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        response.sendRedirect("AdminControlStaff.jsp");
    }
}
