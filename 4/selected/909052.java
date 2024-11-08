package ws.system;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.text.*;

public class PageTemplate {

    private SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy H:mm:ss");

    private StringBuffer page = null;

    private String templateLocation = "";

    private Vector<String> cookies = new Vector<String>();

    public PageTemplate(String template) throws Exception {
        templateLocation = template;
        loadTemplate();
    }

    public void addCookie(String n, String d) throws Exception {
        String name = URLEncoder.encode(n, "UTF-8");
        String data = URLEncoder.encode(d, "UTF-8");
        cookies.add(name + "=" + data + "; PATH=/");
    }

    private void loadTemplate() throws Exception {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        FileInputStream fi = new FileInputStream(templateLocation);
        int read = 0;
        byte[] bytes = new byte[1024];
        read = fi.read(bytes);
        while (read > -1) {
            ba.write(bytes, 0, read);
            read = fi.read(bytes);
        }
        fi.close();
        page = new StringBuffer();
        page.append(ba.toString());
    }

    public void replaceAll(String placeHolder, String replace) throws Exception {
        if (page == null) new Exception("Template not loaded or failed load");
        int phLength = placeHolder.length();
        int replaceLength = replace.length();
        int phIndex = page.indexOf(placeHolder);
        while (phIndex != -1) {
            page.replace(phIndex, phIndex + phLength, replace);
            phIndex = page.indexOf(placeHolder, phIndex + replaceLength);
        }
    }

    public byte[] getPageBytes() throws Exception {
        if (page == null) new Exception("Template not loaded or filed load");
        StringBuffer returnData = new StringBuffer(buildResponce());
        returnData.append(page);
        return returnData.toString().getBytes("UTF-8");
    }

    private String buildResponce() {
        StringBuffer header = new StringBuffer();
        header.append("HTTP/1.0 200 OK\n");
        header.append("Content-Type: text/html; charset=utf-8\n");
        header.append("Pragma: no-cache\n");
        header.append("Cache-Control: no-cache\n");
        String timeStamp = df.format(new Date());
        timeStamp += " GMT";
        header.append("Last-Modified: " + timeStamp + "\n");
        header.append("Date: " + timeStamp + "\n");
        header.append("Expires: " + timeStamp + "\n");
        for (int x = 0; x < cookies.size(); x++) {
            String cook = (String) cookies.get(x);
            header.append("Set-Cookie: " + cook + "\n");
        }
        header.append("\n");
        return header.toString();
    }
}
