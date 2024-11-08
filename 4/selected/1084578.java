package ch.unibas.jmeter.snmp.reporters.resultwritertrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.visualizers.Visualizer;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import ch.unibas.debug.Debug;
import ch.unibas.jmeter.snmp.assertions.SnmpAssertionResult;
import ch.unibas.jmeter.snmp.reporters.trap.SnmpTrapReporter;

public class ResultWriterSnmpTrapReporter extends SnmpTrapReporter implements SampleListener, Visualizer, TestBean {

    private static final long serialVersionUID = 8028872783413494300L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private boolean writeResults;

    private String htmlBasePath;

    private String urlPath;

    public void sampleOccurred(SampleEvent e) {
        if (isWriteResults()) {
            AssertionResult[] assertionResults = e.getResult().getAssertionResults();
            String label = e.getResult().getSampleLabel(true);
            Date startTime = new Date(e.getResult().getStartTime());
            String resultFileName = null;
            String thread = e.getThreadGroup();
            StringBuffer messages = new StringBuffer();
            for (int j = 0; j < assertionResults.length; j++) {
                if (assertionResults[j] instanceof SnmpAssertionResult) {
                    SnmpAssertionResult result = (SnmpAssertionResult) assertionResults[j];
                    if (result.isFailure() || result.isError()) {
                        if (resultFileName == null) {
                            resultFileName = writeResult(result, thread, label, startTime);
                        }
                        result.setErrorUrl(getBaseUrl() + "/" + resultFileName);
                    }
                    messages.append(extractMessage(result));
                }
            }
            if (resultFileName != null) {
                writeSummary(thread, label, startTime, resultFileName, messages.toString());
            }
        }
        super.sampleOccurred(e);
    }

    private String writeResult(SnmpAssertionResult result, String thread, String label, Date startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmssSSS");
            String resultFileName = sdf.format(startTime) + "-" + label + ".html";
            resultFileName = resultFileName.replace(' ', '_').replace(':', '-');
            String htmlBasePath = getHtmlBasePath() + java.io.File.separator;
            String htmlResourcePath = htmlBasePath + thread + java.io.File.separator;
            (new File(htmlResourcePath)).mkdir();
            String fqResultFileName = htmlResourcePath + resultFileName;
            FileOutputStream resFos = new FileOutputStream(fqResultFileName);
            resFos.write(result.getResponseData());
            resFos.flush();
            resFos.close();
            Debug.debug("Wrote " + fqResultFileName);
            return resultFileName;
        } catch (Exception e) {
            log.error("Could open result file", e);
        }
        return null;
    }

    private StringBuffer extractMessage(SnmpAssertionResult result) {
        StringWriter messageWriter = new StringWriter();
        messageWriter.write("<tr><td>" + result.getName() + "</td><td>");
        messageWriter.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        String failureMessage = result.getFailureMessage();
        if (failureMessage == null) {
            return new StringBuffer("");
        }
        String[] text = failureMessage.split("\n");
        for (int i = 0; i < text.length; i++) {
            if (i > 0) {
                messageWriter.write("<br />");
            }
            messageWriter.write(text[i]);
        }
        messageWriter.write("</td></tr>");
        messageWriter.flush();
        return messageWriter.getBuffer();
    }

    private void writeSummary(String thread, String label, Date startTime, String resultFileName, String messages) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            String time = sdf.format(startTime);
            String htmlBasePath = getHtmlBasePath() + java.io.File.separator;
            String fqSummaryFileName = htmlBasePath + resultFileName;
            FileWriter summaryWriter = new FileWriter(fqSummaryFileName);
            summaryWriter.write("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'><html><head>");
            summaryWriter.write("<title>JMeter-Test: " + label + " at " + time + "</title>");
            summaryWriter.write("<meta name='GENERATOR' content='snmpJMeter'><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>");
            summaryWriter.write("<p>");
            summaryWriter.write("<table>");
            summaryWriter.write("<tr><td>Test:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><b>" + label + "</b></td></tr>");
            summaryWriter.write("<tr><td>Time:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><b>" + time + "</b></td></tr>");
            summaryWriter.write("</table>");
            summaryWriter.write("</p>");
            summaryWriter.write("<table><thead><tr><th>Assertion</th><th>Message</th></tr></thead><tbody>");
            summaryWriter.write(messages);
            summaryWriter.write("</tbody></table>");
            summaryWriter.write("<iframe height='700px' width='100%' src='" + thread + "/" + resultFileName + "'></iframe>");
            summaryWriter.write("</body></html>");
            summaryWriter.flush();
            summaryWriter.close();
            Debug.debug("Wrote " + fqSummaryFileName);
        } catch (Exception e) {
            log.error("Could open summary file", e);
        }
    }

    public void setWriteResults(boolean writeResults) {
        this.writeResults = writeResults;
    }

    public boolean isWriteResults() {
        return writeResults;
    }

    public void setHtmlBasePath(String htmlBasePath) {
        this.htmlBasePath = htmlBasePath;
    }

    public String getHtmlBasePath() {
        return htmlBasePath;
    }

    public String getBaseUrl() {
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
            Debug.debug("Found hostname " + hostname);
        } catch (UnknownHostException e) {
            log.error("Cannot determine the local hostname", e);
        }
        return "http://" + hostname + "/" + getUrlPath();
    }

    public void setUrlPath(String baseUrlPath) {
        this.urlPath = baseUrlPath;
    }

    public String getUrlPath() {
        return urlPath;
    }
}
