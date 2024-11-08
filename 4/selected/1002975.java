package reports.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import reports.util.IOUtils;
import analyzer.Analyzer;

public class MakeReportServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) {
        try {
            doProcess(request, response);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println(1);
            sendError(response);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(2);
            sendError(response);
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.out.println(3);
            sendError(response);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.out.println(4);
            sendError(response);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println(5);
            sendError(response);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(6);
            sendError(response);
        }
    }

    private void sendError(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doProcess(HttpServletRequest request, HttpServletResponse resp) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        Analyzer analyzer = new Analyzer();
        ServletContext context = getServletContext();
        String xml = context.getRealPath("data\\log.xml");
        String xsd = context.getRealPath("data\\log.xsd");
        String grs = context.getRealPath("reports\\" + request.getParameter("type") + ".grs");
        String pdf = context.getRealPath("html\\report.pdf");
        System.out.println("omg: " + request.getParameter("type"));
        System.out.println("omg: " + request.getParameter("pc"));
        int pcount = Integer.parseInt(request.getParameter("pc"));
        String[] params = new String[pcount];
        for (int i = 0; i < pcount; i++) {
            params[i] = request.getParameter("p" + i);
        }
        try {
            analyzer.generateReport(xml, xsd, grs, pdf, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(pdf);
        byte[] bs = tryLoadFile(pdf);
        if (bs == null) throw new NullPointerException();
        resp.setHeader("Content-Disposition", " filename=\"" + file.getName() + "\";");
        resp.setContentLength(bs.length);
        InputStream is = new ByteArrayInputStream(bs);
        IOUtils.copy(is, resp.getOutputStream());
    }

    private static byte[] tryLoadFile(String path) throws IOException {
        InputStream in = new FileInputStream(path);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        in.close();
        out.close();
        return out.toByteArray();
    }
}
