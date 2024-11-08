package fido.servlets;

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import fido.util.FidoDataSource;

public class ClearData extends HttpServlet {

    public static void clearTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("delete from VerbTransactions");
        stmt.executeUpdate("delete from VerbConstraints");
        stmt.executeUpdate("delete from Transactions");
        stmt.executeUpdate("delete from TransactionSlots");
        stmt.executeUpdate("delete from TransactionPreconditions");
        stmt.executeUpdate("delete from Verbs");
        stmt.executeUpdate("delete from AdverbPrepositions");
        stmt.executeUpdate("delete from InstructionGroups");
        stmt.executeUpdate("delete from Instructions");
        stmt.executeUpdate("delete from FrameSlots");
        stmt.executeUpdate("delete from GrammarLinks");
        stmt.executeUpdate("delete from Articles");
        stmt.executeUpdate("delete from LanguageMorphologies");
        stmt.executeUpdate("delete from Pronouns");
        stmt.executeUpdate("delete from VerbConstraints");
        stmt.executeUpdate("delete from Dictionary");
        stmt.executeUpdate("delete from WordClassifications");
        stmt.executeUpdate("delete from AdjectivePrepositions");
        stmt.executeUpdate("delete from ObjectLinks");
        stmt.executeUpdate("delete from ObjectAttributes");
        stmt.executeUpdate("delete from Objects where ObjectId != 1");
        stmt.executeUpdate("delete from QuestionWords");
        stmt.executeUpdate("update Objects set Description = 'Object' where ObjectId = 1");
        stmt.executeUpdate("delete from ClassLinkTypes where LinkType = 2");
        stmt.executeUpdate("delete from Attributes");
        stmt.executeUpdate("delete from AttributeCategories");
        stmt.executeUpdate("delete from ProperNouns");
    }

    private void load() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = FidoDataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            clearTables(stmt);
            stmt.executeQuery("select setval('objects_objectid_seq', 1000)");
            stmt.executeQuery("select setval('instructions_instructionid_seq', 1)");
            stmt.executeQuery("select setval('transactions_transactionid_seq', 1)");
            stmt.executeQuery("select setval('verbtransactions_verbid_seq', 1)");
            stmt.executeUpdate("update SystemProperties set value = 'Minimal Data' where name = 'DB Data Version'");
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String context = request.getContextPath();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<TITLE>Clear Data</TITLE>");
        out.println("<LINK REL=stylesheet HREF=" + context + "/Borderless.css>");
        out.println("</HEAD>");
        out.println("<BODY>");
        String[][] path = { { "Home", context + "/index" }, { "Clear Data", null } };
        FidoServlet.header(out, path, null, context);
        try {
            load();
            out.println("Everything loaded OK<P>");
        } catch (Exception e) {
            out.println("<PRE>");
            e.printStackTrace(out);
            out.println("</PRE>");
        }
        FidoServlet.footer(out);
        out.println("</BODY>");
        out.println("</HTML>");
    }
}
