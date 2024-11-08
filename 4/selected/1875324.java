package com.galileoschoice.admin.core;

import java.io.*;
import java.util.*;
import java.sql.*;
import javax.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class BlobUploadServ extends HttpServlet {

    private static String dbUserName = "";

    private static String dbPassword = "";

    private static final char CR = 13;

    private static final char LF = 10;

    private Statement stmt;

    private Connection con;

    protected String boundary = null;

    protected Hashtable params = new Hashtable();

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        ServletInputStream in = request.getInputStream();
        BufferedInputStream bin = new BufferedInputStream(in);
        boundary = getBoundary(request.getHeader("content-type"));
        out.println("<html><body><pre>");
        out.println("boundary =\n" + boundary);
        out.println();
        byte[] bytes = new byte[128];
        in.readLine(bytes, 0, bytes.length);
        String line = new String(bytes);
        Hashtable header = null;
        while (in.readLine(bytes, 0, bytes.length) >= 0) {
            line = new String(bytes);
            if (line.startsWith("Content-Disposition:")) {
                out.println(line);
                header = parseHeader(line);
                updateParams(header);
            } else if (line.startsWith("Content-Type:")) {
                params.put("Content-Type", line.substring("Content-Type:".length()).trim());
            } else {
                if (header != null && bytes[0] == 13) {
                    if (header.containsKey("filename")) {
                        displayParams(out);
                        out.println(" ...saving payload");
                        savePayload(params, bin);
                        header = null;
                    } else {
                        String name = (String) header.get("name");
                        String value = getParameter(in).trim();
                        params.put(name, value);
                    }
                }
                if (line.indexOf(boundary) >= 0) out.println(line);
            }
            bytes = new byte[128];
        }
        out.println("</pre></body></html>");
        out.close();
    }

    private void displayParams(ServletOutputStream out) throws java.io.IOException {
        for (Enumeration e = params.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            out.println(" " + key + " = " + params.get(key));
        }
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

    private void savePayload(Hashtable params, BufferedInputStream is) throws java.io.IOException {
        int c;
        PushbackInputStream input = new PushbackInputStream(is, 128);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((c = read(input, boundary)) >= 0) out.write(c);
        int id = Integer.parseInt((String) params.get("ID"));
        saveBlob(id, (String) params.get("filename"), out.toByteArray());
        out.close();
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

    public void saveBlob(int id, String description, byte[] out) {
        String cmd = "INSERT INTO Photos (Id,Description,Image) VALUES(?,?,?)";
        System.out.println(cmd);
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            con = DriverManager.getConnection("jdbcdbc:test", "", "");
            stmt = con.createStatement();
            stmt.executeUpdate("INSERT INTO Photo VALUES('" + id + "','" + description + "','" + out + "')");
            con.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
