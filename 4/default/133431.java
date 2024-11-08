import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.*;

public class SystemStatusData {

    public SystemStatusData() {
    }

    public byte[] getStatusXML(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "sys-info", null);
        Element root = doc.getDocumentElement();
        Element item = null;
        DataStore store = DataStore.getInstance();
        Calendar start = Calendar.getInstance();
        start.setTime(new Date());
        item = doc.createElement("time");
        int hour = start.get(Calendar.HOUR_OF_DAY);
        if (hour > 12) hour = hour - 12; else if (hour == 0) hour = 12;
        item.setAttribute("hour_12", intToXchar(hour, 2));
        item.setAttribute("hour_24", intToXchar(start.get(Calendar.HOUR_OF_DAY), 2));
        item.setAttribute("minute", intToXchar(start.get(Calendar.MINUTE), 2));
        if (start.get(Calendar.AM_PM) == Calendar.AM) item.setAttribute("am_pm", "am"); else item.setAttribute("am_pm", "pm");
        root.appendChild(item);
        ScheduleItem[] itemsArray = store.getScheduleArray();
        Arrays.sort(itemsArray);
        Vector<ScheduleItem> nextItems = new Vector<ScheduleItem>();
        ScheduleItem firstNext = null;
        item = doc.createElement("now_running");
        for (int x = 0; x < itemsArray.length; x++) {
            if (nextItems.size() == 0 && itemsArray[x].getState() == ScheduleItem.WAITING && itemsArray[x].getStop().getTime() > new Date().getTime()) {
                firstNext = itemsArray[x];
                nextItems.add(itemsArray[x]);
            } else if (firstNext != null && itemsArray[x].getState() == ScheduleItem.WAITING && itemsArray[x].getStart().getTime() < firstNext.getStop().getTime()) {
                nextItems.add(itemsArray[x]);
            }
            if (itemsArray[x].getState() == ScheduleItem.RUNNING) {
                addNowRunningItem(itemsArray[x], item, doc);
            }
        }
        root.appendChild(item);
        item = doc.createElement("next");
        for (int x = 0; x < nextItems.size(); x++) {
            ScheduleItem next = nextItems.get(x);
            addNextItem(next, item, doc);
        }
        root.appendChild(item);
        item = doc.createElement("now_and_next");
        addNOWandNEXT(store, item, doc);
        root.appendChild(item);
        NumberFormat nf = NumberFormat.getInstance();
        item = doc.createElement("freeSpace");
        String[] paths = store.getCapturePaths();
        for (int x = 0; x < paths.length; x++) {
            File capPath = new File(paths[x]);
            Element drive = doc.createElement("drive");
            drive.setAttribute("path", paths[x]);
            if (capPath.exists() == false) {
                drive.setAttribute("free", "Path not found");
            } else {
                DllWrapper wrapper = new DllWrapper();
                long freeSpace = wrapper.getFreeSpace(capPath.getCanonicalPath());
                freeSpace /= (1024 * 1024);
                drive.setAttribute("free", nf.format(freeSpace) + " MB");
            }
            item.appendChild(drive);
        }
        root.appendChild(item);
        XSL transformer = new XSL(doc, "status.xsl", urlData, headers);
        transformer.addCookie("backURL", "/");
        return transformer.doTransform(false);
    }

    private void addNOWandNEXT(DataStore store, Element item, Document doc) {
        GuideStore guide = GuideStore.getInstance();
        Vector<String[]> chanMap = guide.getChannelMap();
        Set<String> wsChannels = store.getChannels().keySet();
        Date now = new Date();
        Calendar startTime = Calendar.getInstance();
        for (int y = 0; y < chanMap.size(); y++) {
            String[] map = (String[]) chanMap.get(y);
            if (wsChannels.contains(map[0])) {
                Element channel = doc.createElement("channel");
                channel.setAttribute("epg_channel", map[1]);
                channel.setAttribute("ws_channel", map[0]);
                GuideItem[] items = guide.getProgramsForChannel(map[1]);
                for (int x = 0; x < items.length; x++) {
                    GuideItem gitem = items[x];
                    startTime.setTime(gitem.getStart());
                    if (gitem.getStart().before(now) && gitem.getStop().after(now)) {
                        Element elmNow = doc.createElement("now");
                        addGuideItem(items[x], elmNow, doc);
                        channel.appendChild(elmNow);
                        if (x + 1 < items.length) {
                            Element elmNext = doc.createElement("next");
                            addGuideItem(items[x + 1], elmNext, doc);
                            channel.appendChild(elmNext);
                        }
                        break;
                    }
                    if (gitem.getStart().after(now)) {
                        Element elmNext = doc.createElement("next");
                        addGuideItem(gitem, elmNext, doc);
                        channel.appendChild(elmNext);
                        break;
                    }
                }
                item.appendChild(channel);
            }
        }
    }

    private void addGuideItem(GuideItem gItem, Element parent, Document doc) {
        Element name = null;
        Text text = null;
        name = doc.createElement("name");
        name.setAttribute("sub_name", gItem.getSubName());
        text = doc.createTextNode(gItem.getName());
        name.appendChild(text);
        parent.appendChild(name);
        Calendar start = Calendar.getInstance();
        start.setTime(gItem.getStart());
        name = doc.createElement("start");
        int hour = start.get(Calendar.HOUR_OF_DAY);
        if (hour > 12) hour = hour - 12; else if (hour == 0) hour = 12;
        name.setAttribute("hour_12", intToXchar(hour, 2));
        name.setAttribute("hour_24", intToXchar(start.get(Calendar.HOUR_OF_DAY), 2));
        name.setAttribute("minute", intToXchar(start.get(Calendar.MINUTE), 2));
        if (start.get(Calendar.AM_PM) == Calendar.AM) name.setAttribute("am_pm", "am"); else name.setAttribute("am_pm", "pm");
        parent.appendChild(name);
        name = doc.createElement("duration");
        text = doc.createTextNode(new Integer(gItem.getDuration()).toString());
        name.appendChild(text);
        parent.appendChild(name);
        name = doc.createElement("id");
        text = doc.createTextNode(gItem.toString());
        name.appendChild(text);
        parent.appendChild(name);
    }

    private void addNextItem(ScheduleItem schItem, Element item, Document doc) {
        Element nextItem = doc.createElement("item");
        Element name = null;
        Text text = null;
        name = doc.createElement("name");
        text = doc.createTextNode(schItem.getName());
        name.appendChild(text);
        nextItem.appendChild(name);
        Calendar start = Calendar.getInstance();
        start.setTime(schItem.getStart());
        name = doc.createElement("start");
        int hour = start.get(Calendar.HOUR_OF_DAY);
        if (hour > 12) hour = hour - 12; else if (hour == 0) hour = 12;
        name.setAttribute("hour_12", intToXchar(hour, 2));
        name.setAttribute("hour_24", intToXchar(start.get(Calendar.HOUR_OF_DAY), 2));
        name.setAttribute("minute", intToXchar(start.get(Calendar.MINUTE), 2));
        if (start.get(Calendar.AM_PM) == Calendar.AM) name.setAttribute("am_pm", "am"); else name.setAttribute("am_pm", "pm");
        nextItem.appendChild(name);
        name = doc.createElement("id");
        text = doc.createTextNode(schItem.toString());
        name.appendChild(text);
        nextItem.appendChild(name);
        name = doc.createElement("duration");
        text = doc.createTextNode(new Integer(schItem.getDuration()).toString());
        name.appendChild(text);
        nextItem.appendChild(name);
        name = doc.createElement("channel");
        text = doc.createTextNode(schItem.getChannel());
        name.appendChild(text);
        nextItem.appendChild(name);
        name = doc.createElement("status");
        text = doc.createTextNode(schItem.getStatus());
        name.appendChild(text);
        nextItem.appendChild(name);
        Date now = new Date();
        long timeLeft = schItem.getStart().getTime() - now.getTime();
        long days = timeLeft / (1000 * 60 * 60 * 24);
        long hours = (timeLeft - (days * 1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long min = (timeLeft - (days * 1000 * 60 * 60 * 24) - (hours * 1000 * 60 * 60)) / (1000 * 60);
        long seconds = (timeLeft - (days * 1000 * 60 * 60 * 24) - (hours * 1000 * 60 * 60) - (min * 1000 * 60)) / 1000;
        name = doc.createElement("time_to_action");
        name.setAttribute("days", new Long(days).toString());
        name.setAttribute("hours", new Long(hours).toString());
        name.setAttribute("minutes", new Long(min).toString());
        name.setAttribute("seconds", new Long(seconds).toString());
        nextItem.appendChild(name);
        item.appendChild(nextItem);
    }

    private void addNowRunningItem(ScheduleItem schItem, Element item, Document doc) {
        Element runnintItem = null;
        Element name = null;
        Text text = null;
        runnintItem = doc.createElement("item");
        name = doc.createElement("name");
        text = doc.createTextNode(schItem.getName());
        name.appendChild(text);
        runnintItem.appendChild(name);
        Calendar start = Calendar.getInstance();
        start.setTime(schItem.getStart());
        name = doc.createElement("start");
        int hour = start.get(Calendar.HOUR_OF_DAY);
        if (hour > 12) hour = hour - 12; else if (hour == 0) hour = 12;
        name.setAttribute("hour_12", intToXchar(hour, 2));
        name.setAttribute("hour_24", intToXchar(start.get(Calendar.HOUR_OF_DAY), 2));
        name.setAttribute("minute", intToXchar(start.get(Calendar.MINUTE), 2));
        if (start.get(Calendar.AM_PM) == Calendar.AM) name.setAttribute("am_pm", "am"); else name.setAttribute("am_pm", "pm");
        runnintItem.appendChild(name);
        name = doc.createElement("id");
        text = doc.createTextNode(schItem.toString());
        name.appendChild(text);
        runnintItem.appendChild(name);
        name = doc.createElement("channel");
        text = doc.createTextNode(schItem.getChannel());
        name.appendChild(text);
        runnintItem.appendChild(name);
        name = doc.createElement("duration");
        text = doc.createTextNode(new Integer(schItem.getDuration()).toString());
        name.appendChild(text);
        runnintItem.appendChild(name);
        name = doc.createElement("status");
        text = doc.createTextNode(schItem.getStatus());
        name.appendChild(text);
        runnintItem.appendChild(name);
        item.appendChild(runnintItem);
    }

    private String intToXchar(int val, int len) {
        String finalString = "";
        String rawInt = new Integer(val).toString();
        int toAdd = len - rawInt.length();
        for (int x = 0; x < toAdd; x++) {
            finalString += "0";
        }
        finalString += rawInt;
        return finalString;
    }
}
