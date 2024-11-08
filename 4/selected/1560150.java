package lebah.servlets;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class BlobUploadServlet extends HttpServlet {

    private String dbUrl = "jdbc:mysql://localhost/images";

    private String jdbcDriver = "com.mysql.jdbc.Driver";

    private static final char CR = 13;

    private static final char LF = 10;

    protected String boundary = null;

    protected Hashtable params = new Hashtable();

    protected ServletOutputStream out = null;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        out = response.getOutputStream();
        ServletInputStream in = request.getInputStream();
        BufferedInputStream bin = new BufferedInputStream(in);
        boundary = getBoundary(request.getHeader("content-type"));
        out.println("<html><body>");
        byte[] bytes = new byte[128];
        in.readLine(bytes, 0, bytes.length);
        String line = new String(bytes);
        Hashtable header = null;
        while (in.readLine(bytes, 0, bytes.length) >= 0) {
            line = new String(bytes);
            if (line.startsWith("Content-Disposition:")) {
                header = parseHeader(line);
                updateParams(header);
            } else if (line.startsWith("Content-Type:")) {
                params.put("Content-Type", line.substring("Content-Type:".length()).trim());
            } else {
                if (header != null && bytes[0] == 13) {
                    if (header.containsKey("filename")) {
                        String filename = (String) params.get("filename");
                        out.println("Saving " + filename);
                        savePayload(filename, bin);
                        header = null;
                    } else {
                        String name = (String) header.get("name");
                        String value = getParameter(in).trim();
                        params.put(name, value);
                    }
                }
            }
        }
        out.println("</body></html>");
        out.close();
    }

    private void updateParams(Hashtable header) {
        for (Enumeration e = header.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            params.put(key, header.get(key));
        }
    }

    private String getParameter(ServletInputStream in) throws java.io.IOException {
        byte[] bytes = new byte[128];
        in.readLine(bytes, 0, bytes.length);
        return new String(bytes);
    }

    private String getBoundary(String contentType) {
        int bStart = contentType.indexOf("boundary=") + "boundary=".length();
        return "" + CR + LF + "--" + contentType.substring(bStart);
    }

    private void savePayload(String filename, BufferedInputStream is) throws java.io.IOException {
        int c;
        PushbackInputStream input = new PushbackInputStream(is, 128);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        while ((c = read(input, boundary)) >= 0) blob.write(c);
        saveBlob(filename, blob.toByteArray());
        blob.close();
    }

    private int read(PushbackInputStream input, String boundary) throws IOException {
        StringBuffer buffer = new StringBuffer();
        int index = -1;
        int c;
        do {
            c = input.read();
            buffer.append((char) c);
            index++;
        } while ((buffer.length() < boundary.length()) && (c == boundary.charAt(index)));
        if (c == boundary.charAt(index)) {
            int type = -1;
            if (input.read() == '-') type = -2;
            while (input.read() != LF) ;
            return type;
        }
        while (index >= 0) {
            input.unread(buffer.charAt(index));
            index--;
        }
        return input.read();
    }

    private Hashtable parseHeader(String line) {
        Hashtable header = new Hashtable();
        String token = null;
        StringTokenizer st = new StringTokenizer(line, ";");
        while (st.hasMoreTokens()) {
            token = ((String) st.nextToken()).trim();
            String key = "";
            String val = "";
            int eq = token.indexOf("=");
            if (eq < 0) eq = token.indexOf(":");
            if (eq > 0) {
                key = token.substring(0, eq).trim();
                val = token.substring(eq + 1);
                val = val.replace('"', ' ');
                val = val.trim();
                header.put(key, val);
            }
        }
        return header;
    }

    private void saveBlob(String filename, byte[] out) {
        Connection con = null;
        PreparedStatement pstmt = null;
        String sqlCmd = "INSERT INTO BLOBS (FileName,BinaryData) VALUES(?,?)";
        try {
            Class.forName(jdbcDriver);
            con = DriverManager.getConnection(dbUrl);
            pstmt = con.prepareStatement(sqlCmd);
            pstmt.setString(1, filename);
            pstmt.setBytes(2, out);
            pstmt.executeUpdate();
            con.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
