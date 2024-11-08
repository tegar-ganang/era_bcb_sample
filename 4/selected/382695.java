package org.maveryx.report.reader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * @author Alfonso Nocella
 */
@SuppressWarnings("unchecked")
public class TestMonitor {

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH.mm.ss";

    private Timer timer_;

    private HashMap files_;

    private static HashMap<String, Integer> resultTable = new HashMap<String, Integer>();

    private static DataSummary summary = DataSummary.getInstance();

    private TestHeader header = TestHeader.getInstance();

    private String destinationFilePath;

    private boolean ended = false;

    private long pollingInterval;

    private String reportDate;

    private static TestMonitor instance;

    public static TestMonitor getInstance(long polling, String destinationPath) {
        if (instance == null) {
            instance = new TestMonitor(polling, destinationPath);
        }
        return instance;
    }

    private TestMonitor(long polling, String destinationPath) {
        pollingInterval = polling;
        this.destinationFilePath = destinationPath;
        files_ = new HashMap();
        timer_ = new Timer(true);
    }

    private void now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        setReportDate(sdf.format(cal.getTime()));
    }

    public void setReportDate(String date) {
        reportDate = date;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void stopPolling() {
        timer_.cancel();
    }

    public void addFile(File file) {
        if (!files_.containsKey(file)) {
            long modifiedTime = file.exists() ? file.lastModified() : -1;
            files_.put(file, new Long(modifiedTime));
        }
    }

    public void removeFile(File file) {
        files_.remove(file);
    }

    public void addListener(XMLListener listener) {
        MyListener.getInstance().setListener(listener);
        timer_.schedule(new FileMonitorNotifier(), 0, pollingInterval);
    }

    public HashMap<String, Integer> getResultTable() {
        return resultTable;
    }

    public TestHeader getTestHeader() {
        return header;
    }

    public int getNumberOfTestClassesToRun() {
        return summary.getTestClassesToRun();
    }

    public int getNumberOfExecutedClasses() {
        return summary.getExecutedTestClass();
    }

    public int getNumberOfExecutedTests() {
        return summary.getExecutedTests();
    }

    public int getPassed() {
        return summary.getPassed();
    }

    public int getFailed() {
        return summary.getFailed();
    }

    public int getIgnored() {
        return summary.getIgnored();
    }

    public Klass[] getClasses() {
        return summary.getClasses();
    }

    public DataSummary getSummary() {
        return summary;
    }

    public boolean isEnded() {
        return ended;
    }

    private class FileMonitorNotifier extends TimerTask {

        public void run() {
            Collection files = new ArrayList(files_.keySet());
            for (Iterator i = files.iterator(); i.hasNext(); ) {
                File file = (File) i.next();
                long lastModifiedTime = ((Long) files_.get(file)).longValue();
                long newModifiedTime = file.exists() ? file.lastModified() : -1;
                if (newModifiedTime != lastModifiedTime) {
                    files_.put(file, new Long(newModifiedTime));
                    XMLListener listener = MyListener.getInstance().getListener();
                    if (listener != null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            listener.fileChanged(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public class XMLListener {

        private int tmp = 0;

        protected void fileChanged(File file) throws Exception {
            now();
            System.out.println("Test report data refreshed \n");
            if (file.exists()) {
                File _report = new File(file.getPath().replace(".xml", "-") + getReportDate() + ".xml");
                copy(file, _report);
                updateDataSummary(_report);
                tmp = resultTable.get("Ignored");
                _report.delete();
                if (summary.getTestClassesToRun() == summary.getExecutedTestClass()) {
                    summary.clearClassesSummary();
                    tmp = 0;
                    String path = destinationFilePath + header.getTestName();
                    File dir = new File(path);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    File copied = new File(path + "/testReport.xml");
                    copy(file, copied);
                    file.delete();
                    ended = true;
                }
            }
        }

        private void setHeader(File xml) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document document = builder.build(xml);
                Element root = document.getRootElement();
                Element head = root.getChild("HEADER");
                List headerInfo = head.getChildren("INFO");
                Iterator infoIterator = headerInfo.iterator();
                while (infoIterator.hasNext()) {
                    Element info = (Element) infoIterator.next();
                    if (info != null) {
                        String name = info.getAttributeValue("name");
                        String value = info.getAttributeValue("value");
                        if (!name.isEmpty() && !value.isEmpty()) {
                            if (name.contentEquals("Report name")) {
                                header.setTestName(value);
                            }
                            if (name.contentEquals("Test run ID")) {
                                header.setTestID(value);
                            }
                            if (name.contentEquals("Start time")) {
                                header.setStartTime(value);
                            }
                            if (name.contentEquals("Run by")) {
                                header.setOwner(value);
                            }
                            if (name.contentEquals("Maveryx version")) {
                                @SuppressWarnings("unused") String version = value;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setSummary(File xml) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document document = builder.build(xml);
                Element root = document.getRootElement();
                Element summary = root.getChild("SUMMARY");
                List summaryChildren = summary.getChildren("DATA");
                Iterator summaryIterator = summaryChildren.iterator();
                while (summaryIterator.hasNext()) {
                    Element item = (Element) summaryIterator.next();
                    if (item != null) {
                        String name = item.getAttributeValue("name");
                        String value = item.getAttributeValue("value");
                        DecimalFormat df = new DecimalFormat("#,###");
                        if (!name.isEmpty() && !value.isEmpty()) {
                            if (name.contentEquals("Test Classes")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setNumberOfTestClassesToRun(resultTable.get("Test Classes"));
                            }
                            if (name.contentEquals("Executed Test Classes")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setNumberOfExecutedClasses(resultTable.get("Executed Test Classes"));
                            }
                            if (name.contentEquals("Executed Tests")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setNumberOfExecutedTests(resultTable.get("Executed Tests"));
                            }
                            if (name.contentEquals("Passed")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setPassed(resultTable.get("Passed"));
                            }
                            if (name.contentEquals("Failed")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setFailed(resultTable.get("Failed"));
                            }
                            if (name.contentEquals("Ignored")) {
                                resultTable.put(name, df.parse(value).intValue());
                                setIgnored(resultTable.get("Ignored"));
                                setClassIgnored(resultTable.get("Ignored") - tmp);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setClasses(File xml) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document document = builder.build(xml);
                Element root = document.getRootElement();
                Element testClasses = root.getChild("TESTCLASSES");
                List classi = testClasses.getChildren("CLASS");
                Iterator classiIterator = classi.iterator();
                while (classiIterator.hasNext()) {
                    Element klass = (Element) classiIterator.next();
                    if (klass != null) {
                        String className = klass.getAttributeValue("class");
                        if (!className.isEmpty()) {
                            List metodi = klass.getChildren("METHOD");
                            Iterator methodIterator = metodi.iterator();
                            Map methods = new HashMap<String, String>();
                            while (methodIterator.hasNext()) {
                                Element method = (Element) methodIterator.next();
                                if (method != null) {
                                    String methodName = method.getAttributeValue("method");
                                    String methodStatus = method.getAttributeValue("status");
                                    methods.put(methodName, methodStatus);
                                }
                            }
                            Klass classe = new Klass(className, methods, summary.getClassIgnored());
                            summary.addClass(classe);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void updateDataSummary(File XmlFile) throws Exception {
            setHeader(XmlFile);
            setSummary(XmlFile);
            setClasses(XmlFile);
            summary.changed();
        }

        private void copy(File inputFile, File outputFile) throws Exception {
            FileReader in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        }

        @SuppressWarnings("unused")
        private int getNumberOfTestClassesToRun() {
            return summary.getTestClassesToRun();
        }

        private void setNumberOfTestClassesToRun(int numOfTestClassesToRun) {
            summary.setTestClassesToRun(numOfTestClassesToRun);
        }

        @SuppressWarnings("unused")
        private int getNumberOfExecutedClasses() {
            return summary.getExecutedTestClass();
        }

        private void setNumberOfExecutedClasses(int numOfClasses) {
            summary.setExecutedTestClass(numOfClasses);
        }

        @SuppressWarnings("unused")
        private int getNumberOfExecutedTests() {
            return summary.getExecutedTests();
        }

        private void setNumberOfExecutedTests(int numOfTests) {
            summary.setExecutedTests(numOfTests);
        }

        @SuppressWarnings("unused")
        private int getPassed() {
            return summary.getPassed();
        }

        private void setPassed(int successed) {
            summary.setPassed(successed);
        }

        @SuppressWarnings("unused")
        private int getFailed() {
            return summary.getFailed();
        }

        private void setFailed(int fails) {
            summary.setFailed(fails);
        }

        @SuppressWarnings("unused")
        private int getIgnored() {
            return summary.getIgnored();
        }

        private void setIgnored(int ignored) {
            summary.setIgnored(ignored);
        }

        private void setClassIgnored(int ignored) {
            summary.setClassIgnored(ignored);
        }
    }
}
