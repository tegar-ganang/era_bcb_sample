package ws.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ws.system.Channel;
import ws.system.DataStore;
import ws.system.GuideItem;
import ws.system.GuideStore;
import ws.system.HTTPurl;
import ws.system.ScheduleItem;
import ws.system.SignalStatistic;
import ws.system.TaskCommand;
import ws.system.ThreadLock;

public class WebServiceSchedules {

    DataStore store = null;

    public WebServiceSchedules() {
        store = DataStore.getInstance();
    }

    public void getRunning(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("schedule_running");
        ScheduleItem[] itemsArray = store.getScheduleArray();
        Arrays.sort(itemsArray);
        for (int x = 0; x < itemsArray.length; x++) {
            if (itemsArray[x].getState() == ScheduleItem.RUNNING) {
                ScheduleItem next = itemsArray[x];
                Element sch = xmlDoc.createElement("schedule");
                addSchedule2Doc(xmlDoc, sch, next, false);
                xmlDoc.getRoot().appendChild(sch);
            }
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void getNext(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("schedule_next");
        ScheduleItem[] itemsArray = store.getScheduleArray();
        Arrays.sort(itemsArray);
        Vector<ScheduleItem> nextItems = new Vector<ScheduleItem>();
        ScheduleItem firstNext = null;
        for (int x = 0; x < itemsArray.length; x++) {
            if (nextItems.size() == 0 && itemsArray[x].getState() == ScheduleItem.WAITING && itemsArray[x].getStop().getTime() > new Date().getTime()) {
                firstNext = itemsArray[x];
                nextItems.add(itemsArray[x]);
            } else if (firstNext != null && itemsArray[x].getState() == ScheduleItem.WAITING && itemsArray[x].getStart().getTime() < firstNext.getStop().getTime()) {
                nextItems.add(itemsArray[x]);
            }
        }
        for (int x = 0; x < nextItems.size(); x++) {
            ScheduleItem next = nextItems.get(x);
            Element sch = xmlDoc.createElement("schedule");
            addSchedule2Doc(xmlDoc, sch, next, false);
            xmlDoc.getRoot().appendChild(sch);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void export(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            exportInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void exportInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream outStream) throws Exception {
        String id = urlData.getParameter("id");
        ScheduleItem item = store.getScheduleItem(id);
        if (item == null) {
            outStream.write("ID not found".getBytes());
            return;
        }
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesOut);
        Vector<String> logFiles = item.getLogFileNames();
        for (int x = 0; x < logFiles.size(); x++) {
            File log = new File(logFiles.get(x));
            if (log.exists()) {
                out.putNextEntry(new ZipEntry(log.getName()));
                byte[] data = new byte[1024];
                FileInputStream is = new FileInputStream(log);
                int read = is.read(data);
                while (read > -1) {
                    out.write(data, 0, read);
                    read = is.read(data);
                }
                out.closeEntry();
            }
        }
        out.putNextEntry(new ZipEntry("ItemLog.txt"));
        out.write(item.getLog().getBytes("UTF-8"));
        out.closeEntry();
        StringBuffer buff = new StringBuffer();
        buff.append("Name              : " + item.getName() + "\r\n");
        buff.append("Start             : " + item.getStart().toString() + "\r\n");
        buff.append("Stop              : " + item.getStop().toString() + "\r\n");
        buff.append("Duration          : " + item.getDuration() + "\r\n");
        buff.append("Channel           : " + item.getChannel() + "\r\n");
        buff.append("Path Index        : " + item.getCapturePathIndex() + "\r\n");
        buff.append("Capture Type      : " + item.getCapType() + "\r\n");
        buff.append("Filename          : " + item.getFileName() + "\r\n");
        buff.append("File Pattern      : " + item.getFilePattern() + "\r\n");
        buff.append("Keep For          : " + item.getKeepFor() + "\r\n");
        buff.append("Post Task         : " + item.getPostTask() + "\r\n");
        buff.append("Post Task Enabled : " + item.getPostTaskEnabled() + "\r\n");
        buff.append("State             : " + item.getState() + "\r\n");
        buff.append("Status            : " + item.getStatus() + "\r\n");
        buff.append("Type              : " + item.getType() + "\r\n");
        buff.append("\r\nWarnings:\r\n");
        Vector<String> warns = item.getWarnings();
        for (int x = 0; x < warns.size(); x++) {
            buff.append(warns.get(x) + "\r\n");
        }
        buff.append("\r\n");
        buff.append("Log Files:\r\n");
        Vector<String> logs = item.getLogFileNames();
        for (int x = 0; x < logs.size(); x++) {
            buff.append(logs.get(x) + "\r\n");
        }
        buff.append("\r\n");
        GuideItem guide_item = item.getCreatedFrom();
        if (guide_item != null) {
            buff.append("Created From:\r\n");
            buff.append("Name     : " + guide_item.getName() + "\r\n");
            buff.append("Start    : " + guide_item.getStart().toString() + "\r\n");
            buff.append("Stop     : " + guide_item.getStop().toString() + "\r\n");
            buff.append("Duration : " + guide_item.getDuration() + "\r\n");
            buff.append("\r\n");
        }
        HashMap<Date, SignalStatistic> signal = item.getSignalStatistics();
        if (signal.size() > 0) {
            buff.append("Signal Statistics: (Locked, Strength, Quality)\r\n");
            Date[] keys = signal.keySet().toArray(new Date[0]);
            for (int x = 0; x < signal.size(); x++) {
                SignalStatistic stat = signal.get(keys[x]);
                buff.append(keys[x].toString() + " - " + stat.getLocked() + ", " + stat.getStrength() + ", " + stat.getQuality() + "\r\n");
            }
            buff.append("\r\n");
        }
        out.putNextEntry(new ZipEntry("ItemDetails.txt"));
        out.write(buff.toString().getBytes("UTF-8"));
        out.closeEntry();
        out.flush();
        out.close();
        StringBuffer header = new StringBuffer();
        header.append("HTTP/1.1 200 OK\n");
        header.append("Content-Type: application/zip\n");
        header.append("Content-Length: " + bytesOut.size() + "\n");
        header.append("Content-Disposition: attachment; filename=\"ScheduleErrorReport.zip\"\n");
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss 'GMT'", new Locale("En", "Us", "Unix"));
        header.append("Last-Modified: " + df.format(new Date()) + "\n");
        header.append("\n");
        outStream.write(header.toString().getBytes());
        ByteArrayInputStream zipStream = new ByteArrayInputStream(bytesOut.toByteArray());
        byte[] bytes = new byte[4096];
        int read = zipStream.read(bytes);
        while (read > -1) {
            outStream.write(bytes, 0, read);
            outStream.flush();
            read = zipStream.read(bytes);
        }
    }

    public void stop(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            stopInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void stopInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        ScheduleItem item = store.getScheduleItem(id);
        String errorString = null;
        if (item != null && item.getState() == ScheduleItem.RUNNING) {
            item.abort();
            item.setStatus("Aborting");
            item.setState(ScheduleItem.ABORTED);
            item.log("Item marked for abort");
            int counts = 20;
            while (item.getState() == ScheduleItem.ABORTED && counts > 0) {
                counts--;
                Thread.sleep(1000);
            }
        } else {
            errorString = "Schedule was not found or it was not in the correct state";
        }
        XmlDoc xmlDoc = new XmlDoc("schedule_action");
        if (errorString != null) {
            Element error = xmlDoc.createTextElement("error", errorString);
            xmlDoc.getRoot().appendChild(error);
        } else {
            Element message = xmlDoc.createTextElement("message", "Schedule Stopped");
            xmlDoc.getRoot().appendChild(message);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void skip(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            skipInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void skipInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        ScheduleItem item = store.getScheduleItem(id);
        String errorString = null;
        if (id != null && id.length() > 0 && item != null && item.getState() == ScheduleItem.WAITING) {
            item.skipToNext();
            store.saveSchedule(null);
        } else {
            errorString = "Schedule in the wrong state";
        }
        XmlDoc xmlDoc = new XmlDoc("schedule_action");
        if (errorString != null) {
            Element error = xmlDoc.createTextElement("error", errorString);
            xmlDoc.getRoot().appendChild(error);
        } else {
            Element message = xmlDoc.createTextElement("message", "Schedule Skipped Forward");
            xmlDoc.getRoot().appendChild(message);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void addPadding(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            addPaddingInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void addPaddingInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        ScheduleItem item = store.getScheduleItem(id);
        String errorString = null;
        if (item != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(item.getStart());
            int startBuff = 0;
            int endBuff = 0;
            try {
                startBuff = Integer.parseInt(store.getProperty("schedule.buffer.start"));
                endBuff = Integer.parseInt(store.getProperty("schedule.buffer.end"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (item.getState() != ScheduleItem.RUNNING) {
                System.out.println("Not RUNNING");
                cal.add(Calendar.MINUTE, (startBuff * -1));
                item.setDuration(item.getDuration() + startBuff + endBuff);
                item.setStart(cal);
            } else {
                System.out.println("RUNNING");
                item.setDuration(item.getDuration() + endBuff);
            }
            store.saveSchedule(null);
        } else {
            errorString = "Schedule not found";
        }
        XmlDoc xmlDoc = new XmlDoc("schedule_action");
        if (errorString != null) {
            Element error = xmlDoc.createTextElement("error", errorString);
            xmlDoc.getRoot().appendChild(error);
        } else {
            Element message = xmlDoc.createTextElement("message", "Schedule Padding Added");
            xmlDoc.getRoot().appendChild(message);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void delete(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            deleteInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void deleteInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        XmlDoc xmlDoc = new XmlDoc("schedule_action");
        ScheduleItem item = store.getScheduleItem(id);
        if (item == null) {
            System.out.println("Schedule Item Not Found (" + id + ")");
            Element error = xmlDoc.createTextElement("error", "Schedule Item Not Found (" + id + ")");
            xmlDoc.getRoot().appendChild(error);
            return;
        }
        String errorString = null;
        if (item != null && (item.getState() == ScheduleItem.WAITING || item.getState() == ScheduleItem.FINISHED || item.getState() == ScheduleItem.SKIPPED || item.getState() == ScheduleItem.ERROR)) {
            store.removeScheduleItem(id);
            store.saveSchedule(null);
        } else {
            errorString = "Can not delete schedule (Wrong State:" + item.getState() + ") ";
        }
        if (errorString != null) {
            Element error = xmlDoc.createTextElement("error", errorString);
            xmlDoc.getRoot().appendChild(error);
        } else {
            Element message = xmlDoc.createTextElement("message", "Schedule Deleted");
            xmlDoc.getRoot().appendChild(message);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void set(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            setInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void setInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String data = urlData.getParameter("data");
        String errorString = null;
        try {
            getSchFromXML(data);
        } catch (Exception e) {
            e.printStackTrace();
            errorString = e.toString();
        }
        XmlDoc xmlDoc = new XmlDoc("schedule_action");
        if (errorString != null) {
            Element error = xmlDoc.createTextElement("error", errorString);
            xmlDoc.getRoot().appendChild(error);
        } else {
            Element message = xmlDoc.createTextElement("message", "Schedule Saved");
            xmlDoc.getRoot().appendChild(message);
        }
        out.write(xmlDoc.getDocBytes());
    }

    private ScheduleItem getSchFromXML(String data) throws Exception {
        if (data == null || data.length() == 0) throw new Exception("Post data is missing");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(data.getBytes()));
        NodeList schedule = document.getElementsByTagName("schedule");
        if (schedule.getLength() == 0) throw new Exception("No schedule entry in the xml");
        String id = null;
        String name = null;
        String duration = null;
        String channel = null;
        String type = null;
        String path = null;
        String filename_pattern = null;
        String capture_type = null;
        String post_task = null;
        String auto_delete = null;
        String keep_for = null;
        String startYear = null;
        String startMonth = null;
        String startDay = null;
        String startHour = null;
        String startMinute = null;
        try {
            id = schedule.item(0).getAttributes().getNamedItem("id").getNodeValue();
            name = getNodeString(schedule.item(0), "name");
            if (name == null) name = "";
            duration = getNodeString(schedule.item(0), "duration");
            channel = getNodeString(schedule.item(0), "channel");
            type = getNodeString(schedule.item(0), "type");
            path = getNodeString(schedule.item(0), "path");
            filename_pattern = getNodeString(schedule.item(0), "filename_pattern");
            capture_type = getNodeString(schedule.item(0), "capture_type");
            post_task = getNodeString(schedule.item(0), "post_task");
            if (post_task == null) post_task = "";
            auto_delete = getNodeString(schedule.item(0), "auto_delete");
            keep_for = getNodeString(schedule.item(0), "keep_for");
            Node times = getChildByTag(schedule.item(0), "times");
            Node start = getChildByTag(times, "start");
            startYear = start.getAttributes().getNamedItem("year").getNodeValue();
            startMonth = start.getAttributes().getNamedItem("month").getNodeValue();
            startDay = start.getAttributes().getNamedItem("day").getNodeValue();
            startHour = start.getAttributes().getNamedItem("hour").getNodeValue();
            startMinute = start.getAttributes().getNamedItem("minute").getNodeValue();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error parsing XML (" + e.toString() + ")");
        }
        HashMap<String, Channel> channels = store.getChannels();
        if (!channels.containsKey(channel)) {
            throw new Exception("Channel Not Found");
        }
        String[] namePatterns = store.getNamePatterns();
        boolean patternFound = false;
        for (int x = 0; x < namePatterns.length; x++) {
            if (namePatterns[x].equals(filename_pattern)) {
                patternFound = true;
                break;
            }
        }
        if (patternFound == false) {
            throw new Exception("Filename pattern not found!");
        }
        int iDuration = -1;
        try {
            iDuration = Integer.parseInt(duration);
        } catch (Exception e) {
            throw new Exception("Duration not valid!");
        }
        int iType = -1;
        try {
            iType = Integer.parseInt(type);
        } catch (Exception e) {
            throw new Exception("Schedule Type not valid!");
        }
        int iCapType = -1;
        try {
            iCapType = Integer.parseInt(capture_type);
        } catch (Exception e) {
            throw new Exception("Capture Type not valid!");
        }
        int iPathIndex = -1;
        try {
            iPathIndex = Integer.parseInt(path);
        } catch (Exception e01) {
            throw new Exception("Path not valid!");
        }
        int iKeepFor = 30;
        try {
            iKeepFor = Integer.parseInt(keep_for);
        } catch (Exception e) {
            throw new Exception("Keep for not valid!");
        }
        if (post_task.length() > 0) {
            HashMap<String, TaskCommand> tasks = store.getTaskList();
            if (tasks.containsKey(post_task) == false) {
                throw new Exception("Post Task not valid!");
            }
        }
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.MILLISECOND, 0);
        try {
            startTime.set(Integer.parseInt(startYear), Integer.parseInt(startMonth) - 1, Integer.parseInt(startDay), Integer.parseInt(startHour), Integer.parseInt(startMinute), 0);
        } catch (Exception e) {
            throw new Exception("Start Time/Date not valid!");
        }
        boolean replace = false;
        ScheduleItem item = null;
        if (id.length() > 0) {
            item = store.getScheduleItem(id);
            replace = true;
            if (item == null) {
                throw new Exception("Schedule ID (" + id + ") not found");
            }
            if (item.getState() != ScheduleItem.WAITING && item.getState() != ScheduleItem.FINISHED && item.getState() != ScheduleItem.SKIPPED && item.getState() != ScheduleItem.ERROR) {
                throw new Exception("Can only edit schedules in the the (WAITING|FINISHED|SKIPPED|ERROR) states");
            }
        } else {
            item = new ScheduleItem(store.rand.nextLong());
        }
        item.setCreatedFrom(null);
        item.setCapType(iCapType);
        item.setType(iType);
        item.setName(name);
        item.setState(ScheduleItem.WAITING);
        item.setStatus("Waiting");
        item.resetAbort();
        item.setStart(startTime);
        item.setDuration(iDuration);
        item.setCapturePathIndex(iPathIndex);
        item.setChannel(channel);
        item.setFilePattern(filename_pattern);
        if ("1".equals(auto_delete)) item.setAutoDeletable(true); else item.setAutoDeletable(false);
        item.setKeepFor(iKeepFor);
        item.setPostTask(post_task);
        item.log("New Schedule added/edited");
        if (replace == true) {
            store.removeScheduleItem(item.toString());
        }
        GuideStore guide = GuideStore.getInstance();
        boolean isAlreadyInLIst = guide.isAlreadyInList(item, 1);
        if (isAlreadyInLIst) {
            throw new Exception("A schedule with matching details already exists");
        } else {
            store.addScheduleItem(item);
        }
        return item;
    }

    private Node getChildByTag(Node node, String name) throws Exception {
        if (!node.hasChildNodes()) return null;
        Node found = null;
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            found = nl.item(x);
            if (found.getNodeName().equals(name)) return found;
        }
        return null;
    }

    private String getNodeString(Node prog, String nodename) throws Exception {
        String out = null;
        Node tmp = getChildByTag(prog, nodename);
        if (tmp != null && tmp.getFirstChild() != null) {
            out = tmp.getFirstChild().getNodeValue();
        }
        tmp = null;
        return out;
    }

    public void getList(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ScheduleItem[] itemsArray = store.getScheduleArray();
        Arrays.sort(itemsArray);
        XmlDoc xmlDoc = new XmlDoc("schedules");
        for (int x = 0; x < itemsArray.length; x++) {
            ScheduleItem item = itemsArray[x];
            Element sch = xmlDoc.createElement("schedule");
            addSchedule2Doc(xmlDoc, sch, item, false);
            xmlDoc.getRoot().appendChild(sch);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void get(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        ScheduleItem item = store.getScheduleItem(id);
        XmlDoc xmlDoc = new XmlDoc("schedule");
        if (item != null) {
            addSchedule2Doc(xmlDoc, xmlDoc.getRoot(), item, true);
        } else {
            Element error = xmlDoc.createTextElement("error", "Item with ID " + id + " was not found!");
            xmlDoc.getRoot().appendChild(error);
        }
        out.write(xmlDoc.getDocBytes());
    }

    private void addSchedule2Doc(XmlDoc doc, Element parent, ScheduleItem item, boolean addLog) throws Exception {
        Element elm = null;
        parent.setAttribute("id", item.toString());
        Calendar cal = Calendar.getInstance();
        cal.setTime(item.getStart());
        elm = doc.createElement("times");
        Element time = doc.createElement("start");
        time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
        time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
        time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
        time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        elm.appendChild(time);
        cal.setTime(item.getStop());
        time = doc.createElement("stop");
        time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
        time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
        time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
        time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        elm.appendChild(time);
        parent.appendChild(elm);
        elm = doc.createTextElement("name", item.getName());
        parent.appendChild(elm);
        elm = doc.createTextElement("duration", new Integer(item.getDuration()).toString());
        parent.appendChild(elm);
        elm = doc.createTextElement("channel", item.getChannel());
        parent.appendChild(elm);
        elm = doc.createTextElement("state", new Integer(item.getState()).toString());
        parent.appendChild(elm);
        elm = doc.createTextElement("status", item.getStatus());
        parent.appendChild(elm);
        elm = doc.createTextElement("type", new Integer(item.getType()).toString());
        parent.appendChild(elm);
        elm = doc.createTextElement("path", new Integer(item.getCapturePathIndex()).toString());
        parent.appendChild(elm);
        elm = doc.createTextElement("filename_pattern", item.getFilePattern());
        parent.appendChild(elm);
        elm = doc.createTextElement("capture_type", new Integer(item.getCapType()).toString());
        parent.appendChild(elm);
        elm = doc.createTextElement("post_task", item.getPostTask());
        parent.appendChild(elm);
        if (item.isAutoDeletable()) elm = doc.createTextElement("auto_delete", "1"); else elm = doc.createTextElement("auto_delete", "0");
        parent.appendChild(elm);
        elm = doc.createTextElement("keep_for", new Integer(item.getKeepFor()).toString());
        parent.appendChild(elm);
        elm = doc.createElement("warnings");
        Vector<String> warnings = item.getWarnings();
        for (int x = 0; x < warnings.size(); x++) {
            Element warningElm = doc.createTextElement("warning", warnings.get(x));
            elm.appendChild(warningElm);
        }
        parent.appendChild(elm);
        if (addLog) {
            elm = doc.createTextElement("log_data", item.getLog());
            parent.appendChild(elm);
        }
    }

    public void getSchTypes(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("schedule_types");
        Element typeElm = null;
        typeElm = xmlDoc.createTextElement("type", "Once");
        typeElm.setAttribute("id", new Integer(ScheduleItem.ONCE).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        typeElm = xmlDoc.createTextElement("type", "Daily");
        typeElm.setAttribute("id", new Integer(ScheduleItem.DAILY).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        typeElm = xmlDoc.createTextElement("type", "Weekly");
        typeElm.setAttribute("id", new Integer(ScheduleItem.WEEKLY).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        typeElm = xmlDoc.createTextElement("type", "Monthly");
        typeElm.setAttribute("id", new Integer(ScheduleItem.MONTHLY).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        typeElm = xmlDoc.createTextElement("type", "Weekday");
        typeElm.setAttribute("id", new Integer(ScheduleItem.WEEKDAY).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        typeElm = xmlDoc.createTextElement("type", "EPG");
        typeElm.setAttribute("id", new Integer(ScheduleItem.EPG).toString());
        xmlDoc.getRoot().appendChild(typeElm);
        out.write(xmlDoc.getDocBytes());
    }
}
