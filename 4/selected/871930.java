package com.cokemi.utils.tree;

import java.io.*;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * ����Tree�����Դ����ȡ���
 * @author Jammy Zhou
 *
 */
public class TreeResourceServlet extends TreeServlet {

    private static final String resourceDir = "/com/cokemi/utils/tree/resources/";

    private static Hashtable<String, StringBuffer> resourcePool = new Hashtable<String, StringBuffer>();

    private static TreeResourceServlet instance = new TreeResourceServlet();

    public static TreeResourceServlet getInstance() {
        return instance;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            String resourceName = super.getRequestResourceName(request);
            if (resourceName != null) {
                String suffix = resourceName.substring(resourceName.lastIndexOf(".") + 1);
                if (suffix.equalsIgnoreCase("js") || suffix.equalsIgnoreCase("css")) {
                    response.setContentType("text/html; charset=GBK");
                    outputTextFile(resourceDir + resourceName, response);
                } else if (suffix.equalsIgnoreCase("gif") || suffix.equalsIgnoreCase("jpg") || suffix.equalsIgnoreCase("png")) {
                    response.setContentType("image/" + suffix);
                    outputStreamFile(resourceDir + resourceName, response);
                }
            }
        } catch (Exception e) {
            try {
                response.getWriter().println("[ERROR] : <pre>");
                e.printStackTrace(response.getWriter());
                response.getWriter().println("</pre>");
            } catch (Exception ex) {
            }
        }
    }

    private void outputStreamFile(String filePath, HttpServletResponse response) throws Exception {
        OutputStream outs = response.getOutputStream();
        if (resourcePool.containsKey(filePath)) {
            String str = resourcePool.get(filePath).toString();
            byte[] bytes = str.getBytes("ISO8859-1");
            outs.write(bytes, 0, str.length());
        } else {
            InputStream inputStream = getClass().getResourceAsStream(filePath);
            byte[] bytes = new byte[1024];
            byte[] tempBytes = null;
            StringBuffer str = new StringBuffer();
            int read = 0;
            while ((read = inputStream.read(bytes)) >= 0) {
                outs.write(bytes, 0, read);
                tempBytes = new byte[read];
                for (int i = 0; i < read; i++) {
                    tempBytes[i] = bytes[i];
                }
                str.append(new String(tempBytes, "ISO8859-1"));
            }
            inputStream.close();
            resourcePool.put(filePath, str);
        }
        outs.flush();
        outs.close();
    }

    private void outputTextFile(String filePath, HttpServletResponse response) throws Exception {
        StringBuffer content = null;
        if (resourcePool.containsKey(filePath)) {
            content = resourcePool.get(filePath);
        } else {
            InputStream stream = getClass().getResourceAsStream(filePath);
            InputStreamReader isr = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(isr);
            content = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line + "\n");
            }
            reader.close();
            isr.close();
            stream.close();
            resourcePool.put(filePath, content);
        }
        response.getWriter().println(content.toString());
    }
}
