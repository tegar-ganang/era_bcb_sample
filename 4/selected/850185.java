package com.enjoyxstudy.hip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author onozaty
 */
public class LogViewerServlet extends HttpServlet {

    /** serialVersionUID */
    private static final long serialVersionUID = -7042544333443334503L;

    /** config */
    private Config config;

    /** buffer size */
    private static final int BUF_SIZE = 1024;

    /** log view template */
    private static final String TEMPLATE = "logview_template.html";

    /** charset */
    private static final String CHARSET = "UTF-8";

    /** template string */
    private String template;

    /**
     * @param config
     * @throws IOException
     */
    public LogViewerServlet(Config config) throws IOException {
        super();
        this.config = config;
        InputStream inputStream = LogViewerServlet.class.getResourceAsStream(TEMPLATE);
        try {
            template = getStringByStream(inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String date = request.getQueryString();
        if (date == null || date.equals("")) {
            outputResponce(response, createIndexHTML());
        } else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}")) {
            outputResponce(response, createLogHTML(date));
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
    }

    /**
     * @param response
     * @param html
     * @throws IOException
     */
    private void outputResponce(HttpServletResponse response, String html) throws IOException {
        response.setContentType("text/html; charset=" + CHARSET);
        Writer writer = response.getWriter();
        try {
            writer.write(html);
        } finally {
            writer.close();
        }
    }

    /**
     * @return index html
     */
    private String createIndexHTML() {
        String[] logDates = getLogDates();
        StringBuffer contents = new StringBuffer();
        contents.append("<ul>");
        for (int i = 0; i < logDates.length; i++) {
            contents.append("<li><a href=\"?");
            contents.append(logDates[i]);
            contents.append("\">");
            contents.append(logDates[i]);
            contents.append("</a></li>");
        }
        contents.append("</ul>");
        return template.replaceAll("\\$\\{nick\\}", config.getNick()).replaceAll("\\$\\{channel\\}", config.getChannel()).replaceAll("\\$\\{server\\}", config.getServerName()).replaceAll("\\$\\{contents\\}", contents.toString());
    }

    /**
     * @param date
     * @return html
     * @throws IOException
     */
    private String createLogHTML(String date) throws IOException {
        StringBuffer contents = new StringBuffer();
        contents.append("<p><a href=\"./\">Index</a></p>");
        contents.append("<h2>IRC Log for ");
        contents.append(date);
        contents.append("</h2>");
        FileInputStream inputStream = new FileInputStream(new File(config.getOutputDir(), date + ".log"));
        try {
            contents.append(getStringByStream(inputStream));
        } finally {
            inputStream.close();
        }
        return template.replaceAll("\\$\\{nick\\}", config.getNick()).replaceAll("\\$\\{channel\\}", config.getChannel()).replaceAll("\\$\\{server\\}", config.getServerName()).replaceAll("\\$\\{contents\\}", contents.toString());
    }

    /**
     * @return log date
     */
    private String[] getLogDates() {
        File[] logFiles = config.getOutputDir().listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.matches("\\d{4}\\-\\d{2}\\-\\d{2}\\.log");
            }
        });
        String[] logDates = new String[logFiles.length];
        for (int i = 0; i < logDates.length; i++) {
            logDates[i] = logFiles[i].getName();
            logDates[i] = logDates[i].substring(0, logDates[i].indexOf("."));
        }
        Arrays.sort(logDates, new Comparator() {

            public int compare(Object o1, Object o2) {
                return ((String) o1).compareTo(o2) * -1;
            }
        });
        return logDates;
    }

    /**
     * @param inputStream
     * @return string
     * @throws IOException
     */
    private String getStringByStream(InputStream inputStream) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET));
        char[] buf = new char[BUF_SIZE];
        int bufSize = 0;
        while ((bufSize = reader.read(buf, 0, BUF_SIZE)) != -1) {
            buffer.append(buf, 0, bufSize);
        }
        return buffer.toString();
    }
}
