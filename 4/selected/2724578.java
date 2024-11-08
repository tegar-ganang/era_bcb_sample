package com.ununbium.Scripting.reports;

import com.ununbium.LoadGen.Request;
import com.ununbium.LoadGen.ScriptStatus;
import java.io.*;
import java.net.URL;
import java.util.Vector;
import org.apache.commons.httpclient.*;
import org.apache.log4j.Logger;

public class HTMLTestReport implements TestReport {

    private String scriptName = "ScriptName";

    private String directory = ".";

    private String startTime = "";

    private String duration = "";

    private String sleepTime = "";

    private String durationMinusSleep = "";

    private int totFailures = 0;

    private int totFinished = 0;

    private Vector stats = null;

    private String logoFile = "wpg-logo.html";

    private URL logoURL = this.getClass().getResource("com/ununbium/wpg-logo.gif");

    private String resultFile = "result.html";

    private StringBuffer indexSB = new StringBuffer();

    private String indexFile = "index.html";

    private StringBuffer leftMenuSB = new StringBuffer();

    private String leftMenuFile = "leftmenu.html";

    private StringBuffer logFileSB = new StringBuffer();

    private String logFile = "";

    public File getReportFile() {
        return new File(directory + File.separator + indexFile);
    }

    public HTMLTestReport(String scriptName, String directory, Vector stats) {
        this.scriptName = scriptName;
        this.directory = directory;
        this.stats = stats;
    }

    private void createFile(String fileName, StringBuffer fileContent) {
        try {
            BufferedWriter os = new BufferedWriter(new FileWriter(directory + "/" + fileName));
            os.write(fileContent.toString(), 0, fileContent.length());
            os.close();
        } catch (Exception e) {
            System.out.println("Exception in TestReport::createFile: FileName: " + fileName + " Exception: " + e);
            e.printStackTrace();
        }
        System.out.println("Created report output file: " + fileName + " Size: " + fileContent.length());
    }

    private String colorLine(String line) {
        if (line.contains(" WARN ")) {
            line = "<font size=+1 color=\"blue\">" + line + "</font><br>";
        } else if (line.contains(" ERROR ") || line.startsWith("Error: ")) {
            line = "<font size=+2 color=\"red\">" + line + "</font><br>";
        } else if (line.contains(" FATAL ")) {
            line = "<font size=+4 color=\"red\">" + line + "</font><br>";
        } else {
            line = "<font color=\"black\">" + line + "</font><br>";
        }
        return line;
    }

    private void processRequest(String requestFile, Request ro, String tranName, int ind) {
        StringBuffer sb = new StringBuffer("<html><head><title>Test Report for " + scriptName + " Transaction " + tranName + " Request: " + ind + "</title></head>");
        sb.append(ro.toPrint() + "</html>");
        createFile(requestFile, sb);
    }

    private void processStatObject(ScriptStatus sso) {
        StringBuffer sb = new StringBuffer("<html><head><title>Test Report for " + scriptName + " Transaction " + sso.tranName + "</title></head>");
        sb.append("<H1>Script: " + scriptName + "</H1>");
        sb.append("<H1>Transaction: " + sso.tranName + "</H1>");
        sb.append("Status: " + sso.getStatusText() + "<br>");
        if (sso.status == 2) {
            totFinished++;
        } else {
            totFailures++;
        }
        sb.append("Start Time: " + sso.printDate() + "<br>");
        sb.append("Duration: " + sso.printDuration() + "<br>");
        if (sso.tranName.equals("_RunTime_")) {
            startTime = sso.printDate();
            duration = sso.printDuration();
            sleepTime = sso.printSleep();
            durationMinusSleep = sso.printDurationMinusSleep();
        }
        sb.append("<h2>Web Requests:</h2><Table border=\"1\" width=\"100%\"><tr>" + "<th><b>Details</b></th>" + "<th><b>Method</b></th>" + "<th><b>URL</b></th>" + "<th><b>Response Code</b></th>" + "<th><b>Length</b></th></tr>");
        for (int i = 0; i < sso.requests.size(); i++) {
            Request ro = (Request) sso.requests.get(i);
            String requestFile = "trans-" + sso.tranName + "-request" + i + ".html";
            sb.append("<tr><td><a href=\"" + requestFile + "\" target=\"_blank\">Details</a></td>" + "<td>" + ro.method + "</td>" + "<td>" + ro.url + "</td>" + "<td>" + ro.respCode + " - " + ro.respCodeText + "</td>" + "<td>" + ro.respText.length() + "</td></tr>");
            processRequest(requestFile, ro, sso.tranName, i);
        }
        sb.append("</table><p><H2>Log Entries:</H2><ul>");
        for (int i = 0; i < sso.logs.size(); i++) {
            String logLine = (String) sso.logs.get(i);
            sb.append("<li>" + colorLine(logLine) + "</li>");
        }
        sb.append("</ul></html>");
        createFile("trans-" + sso.tranName + ".html", sb);
    }

    private void processResultFile() {
        StringBuffer sb = new StringBuffer("<html><head><title>Test Report for " + scriptName + "</title></head>");
        sb.append("<H1>Script: " + scriptName + "</H1>");
        sb.append("Start Time: " + startTime + "<br>");
        sb.append("Duration: " + duration + "<br>");
        sb.append("Sleep Time: " + sleepTime + "<br>");
        sb.append("System Duration: " + durationMinusSleep + "<br>");
        sb.append("Successes: " + totFinished + "<br>");
        sb.append("Failures: " + totFailures + "<br>");
        sb.append("<a href=\"" + logFile + "\" target=\"_blank\">View Output Log</a><br>");
        sb.append("<h2>Transactions:</h2><Table border=\"1\" width=\"100%\"><tr>" + "<th><b>Name</b></th>" + "<th><b>Duration</b></th>" + "<th><b>Requests</b></th>" + "<th><b>Status</b></th></tr>");
        for (int i = 0; i < stats.size(); i++) {
            ScriptStatus sso = (ScriptStatus) stats.get(i);
            sb.append("<tr><td><a href=\"trans-" + sso.tranName + ".html\" target=\"main\">" + sso.tranName + "</a></td>");
            sb.append("<td>" + sso.printDuration() + "</td>");
            sb.append("<td>" + sso.requests.size() + "</td>");
            sb.append("<td>" + sso.getStatusText() + "</td></tr>");
        }
        sb.append("</table></html>");
        createFile(resultFile, sb);
    }

    public void generate() {
        try {
            createFile(logoFile, new StringBuffer("<html><a href=\"http://www.webperformancegroup.com\" target=\"_blank\"><img src=\"wpg-logo.gif\" alt=\"Web Performance Group\" align=\"center\" border=\"0\" /></a></html>"));
            DataInputStream di = new DataInputStream(this.getClass().getResourceAsStream("/com.ununbium/wpg-logo.gif"));
            FileOutputStream fo = new FileOutputStream(directory + "/" + "wpg-logo.gif");
            byte[] b = new byte[1];
            while (di.read(b, 0, 1) != -1) fo.write(b, 0, 1);
            di.close();
            fo.close();
            indexSB.append("<html><head><title>Test Report for " + scriptName + "</title></head>");
            indexSB.append("<frameset cols=\"20%,80%\">" + "<frameset rows=\"20%,80%\">" + "<frame src=\"" + logoFile + "\" noresize>" + "<frame src=\"" + leftMenuFile + "\" name=\"leftmenu\" noresize>" + "</frameset>" + "<frame src=\"" + resultFile + "\" name=\"main\" scrolling=\"yes\">" + "</frameset> <noframes>" + "<h1>Your Browser Does Not Support Frames.</h1>" + "</noframes></html>");
            createFile(indexFile, indexSB);
            leftMenuSB.append("<html><body><a href=\"" + resultFile + "\" target=\"main\">Script Run Results</a><br>" + "<a href=\"" + logFile + "\" target=\"_blank\">View Output Log</a><br>" + "<p><h2>Transactions:</h2> <ul>");
            for (int i = 0; i < stats.size(); i++) {
                ScriptStatus sso = (ScriptStatus) stats.get(i);
                leftMenuSB.append("<li> <a href=\"trans-" + sso.tranName + ".html\" target=\"main\">" + sso.tranName + "</a> </li>");
                processStatObject(sso);
            }
            leftMenuSB.append("</ul>");
            leftMenuSB.append("</body></html>");
            createFile(leftMenuFile, leftMenuSB);
            processResultFile();
        } catch (Exception e) {
            System.out.println("Exception in TestReport::generate: " + e);
            e.printStackTrace();
        }
    }
}
