package ws.webservice;

import java.io.*;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import ws.system.*;

public class WebServiceEpg {

    DataStore store = null;

    public WebServiceEpg() {
        store = DataStore.getInstance();
    }

    public void setIgnore(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String id = urlData.getParameter("id");
        String channel = urlData.getParameter("channel");
        String ignore = urlData.getParameter("ignore");
        GuideStore guide = GuideStore.getInstance();
        String epgChan = guide.getEpgChannelFromMap(channel);
        GuideItem item = guide.getProgram(epgChan, id);
        XmlDoc xmlDoc = new XmlDoc("epg_ignore");
        String message = "";
        if (item != null) {
            if ("1".equals(ignore) && item.getIgnored() == false) {
                item.setIgnored(true);
                GuideStore.getInstance().saveEpg();
                message = "EPG item is now being ignored";
            } else if ("0".equals(ignore) && item.getIgnored() == true) {
                item.setIgnored(false);
                GuideStore.getInstance().saveEpg();
                message = "EPG item is not NOT being ignored";
            }
            Element mess = xmlDoc.createTextElement("message", message);
            xmlDoc.getRoot().appendChild(mess);
        } else {
            Element error = xmlDoc.createTextElement("error", "Error, epg item not found");
            xmlDoc.getRoot().appendChild(error);
        }
        Element category = xmlDoc.createTextElement("message", message);
        xmlDoc.getRoot().appendChild(category);
        out.write(xmlDoc.getDocBytes());
    }

    public void nowAndNext(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("epg_now_next");
        GuideStore guide = GuideStore.getInstance();
        Vector<String[]> chanMap = guide.getChannelMap();
        Set<String> wsChannels = store.getChannels().keySet();
        Date now = new Date();
        Calendar startTime = Calendar.getInstance();
        for (int y = 0; y < chanMap.size(); y++) {
            String[] map = (String[]) chanMap.get(y);
            if (wsChannels.contains(map[0])) {
                Element channel = xmlDoc.createElement("channel");
                channel.setAttribute("epg_channel", map[1]);
                channel.setAttribute("ws_channel", map[0]);
                GuideItem[] items = guide.getProgramsForChannel(map[1]);
                for (int x = 0; x < items.length; x++) {
                    GuideItem gitem = items[x];
                    startTime.setTime(gitem.getStart());
                    if (gitem.getStart().before(now) && gitem.getStop().after(now)) {
                        Element elmNow = xmlDoc.createElement("now");
                        addGuideItem(items[x], elmNow, xmlDoc.getDoc());
                        channel.appendChild(elmNow);
                        if (x + 1 < items.length) {
                            Element elmNext = xmlDoc.createElement("next");
                            addGuideItem(items[x + 1], elmNext, xmlDoc.getDoc());
                            channel.appendChild(elmNext);
                        }
                        break;
                    }
                    if (gitem.getStart().after(now)) {
                        Element elmNext = xmlDoc.createElement("next");
                        addGuideItem(gitem, elmNext, xmlDoc.getDoc());
                        channel.appendChild(elmNext);
                        break;
                    }
                }
                xmlDoc.getRoot().appendChild(channel);
            }
        }
        out.write(xmlDoc.getDocBytes());
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

    private String removeChars(String data) {
        data = data.replaceAll("'", "`");
        data = data.replaceAll("\"", "`");
        return data;
    }

    public void searchResults(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        String name = urlData.getParameter("q");
        if (name == null || name.length() == 0) name = "";
        String type = urlData.getParameter("type");
        if (type == null || type.length() == 0) type = "Title";
        int searchType = 0;
        if ("title".equalsIgnoreCase(type)) searchType = 1; else if ("description".equalsIgnoreCase(type)) searchType = 2; else searchType = 0;
        String cat = urlData.getParameter("cat");
        if (cat == null || cat.length() == 0) cat = "any";
        String chan = urlData.getParameter("chan");
        if (chan == null || chan.length() == 0) chan = "any";
        GuideStore guide = GuideStore.getInstance();
        HashMap<String, Vector<GuideItem>> results = new HashMap<String, Vector<GuideItem>>();
        guide.simpleEpgSearch(name, searchType, cat, chan, 0, null, results);
        String[] keys = (String[]) results.keySet().toArray(new String[0]);
        Vector<String[]> channelMap = guide.getChannelMap();
        XmlDoc xmlDoc = new XmlDoc("epg_search");
        for (int y = 0; y < keys.length; y++) {
            Vector<GuideItem> result = results.get(keys[y]);
            if (result != null && result.size() > 0) {
                for (int x = 0; x < result.size(); x++) {
                    GuideItem item = (GuideItem) result.get(x);
                    String epgChannel = "not_mapped";
                    for (int q = 0; q < channelMap.size(); q++) {
                        if (channelMap.get(q)[0].equals(keys[y])) {
                            epgChannel = channelMap.get(q)[1];
                            break;
                        }
                    }
                    Element program = xmlDoc.createElement("epg_item");
                    Element idElement = xmlDoc.createTextElement("id", item.toString());
                    program.appendChild(idElement);
                    Element titleElement = xmlDoc.createTextElement("title", removeChars(item.getName()));
                    program.appendChild(titleElement);
                    Element subTitleElement = xmlDoc.createTextElement("sub-title", item.getSubName());
                    program.appendChild(subTitleElement);
                    for (int index = 0; index < item.getCategory().size(); index++) {
                        Element catElement = xmlDoc.createTextElement("category", item.getCategory().get(index));
                        program.appendChild(catElement);
                    }
                    Element descElement = xmlDoc.createTextElement("desc", removeChars(item.getDescription()));
                    program.appendChild(descElement);
                    Element programLengthElement = xmlDoc.createTextElement("duration-length", new Long(item.getDuration()).toString());
                    program.appendChild(programLengthElement);
                    Element fullTimes = xmlDoc.createElement("times");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(item.getStart());
                    Element time = xmlDoc.createElement("start");
                    time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
                    time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
                    time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
                    time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
                    time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
                    fullTimes.appendChild(time);
                    cal.setTime(item.getStop());
                    time = xmlDoc.createElement("stop");
                    time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
                    time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
                    time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
                    time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
                    time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
                    fullTimes.appendChild(time);
                    program.appendChild(fullTimes);
                    Element channels = xmlDoc.createElement("program_channel");
                    channels.setAttribute("chan-display-name", keys[y]);
                    channels.setAttribute("chan-epgdata-name", epgChannel);
                    program.appendChild(channels);
                    xmlDoc.getRoot().appendChild(program);
                }
            }
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void getCategoryList(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("category_list");
        GuideStore guide = GuideStore.getInstance();
        String[] cats = guide.getCategoryStrings();
        Arrays.sort(cats, String.CASE_INSENSITIVE_ORDER);
        for (int z = 0; z < cats.length; z++) {
            Element category = xmlDoc.createTextElement("category", cats[z]);
            xmlDoc.getRoot().appendChild(category);
        }
        out.write(xmlDoc.getDocBytes());
    }

    public void createAutoAddFromItem(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            createAutoAddFromItemInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void createAutoAddFromItemInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("auto_add_action");
        GuideStore guide = GuideStore.getInstance();
        String itemID = urlData.getParameter("id");
        String wsChan = urlData.getParameter("channel");
        String epgChan = guide.getEpgChannelFromMap(wsChan);
        GuideItem item = guide.getProgram(epgChan, itemID);
        if (item == null) {
            Element error = xmlDoc.createTextElement("error", "Guide Item Not Found!");
            xmlDoc.getRoot().appendChild(error);
            out.write(xmlDoc.getDocBytes());
            return;
        }
        String name = item.getName();
        EpgMatchList newMatchList = new EpgMatchList();
        Vector<EpgMatchListItem> items = newMatchList.getMatchList();
        EpgMatchListItem newItemTitle = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        newItemTitle.setTextSearchData(name, EpgMatchListItem.FIELD_TITLE, true, EpgMatchListItem.FLAG_CASEINSENSATIVE);
        items.add(newItemTitle);
        EpgMatchListItem newItemChan = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        newItemChan.setTextSearchData(wsChan, EpgMatchListItem.FIELD_CHANNEL, true, EpgMatchListItem.FLAG_NONE);
        items.add(newItemChan);
        EpgMatch epgMatch = new EpgMatch(name);
        epgMatch.addMatchList(newMatchList);
        int keepFor = 30;
        try {
            keepFor = Integer.parseInt(store.getProperty("autodel.keepfor"));
        } catch (Exception e) {
        }
        epgMatch.setKeepFor(keepFor);
        epgMatch.setAutoDel(false);
        int startBuff = 0;
        int endBuffer = 0;
        try {
            startBuff = Integer.parseInt(store.getProperty("schedule.buffer.start"));
            endBuffer = Integer.parseInt(store.getProperty("schedule.buffer.end"));
        } catch (Exception e) {
        }
        epgMatch.setStartBuffer(startBuff);
        epgMatch.setEndBuffer(endBuffer);
        epgMatch.setPostTask(store.getProperty("tasks.deftask"));
        String[] namePatterns = store.getNamePatterns();
        if (namePatterns.length > 0) {
            epgMatch.setFileNamePattern(namePatterns[0]);
        } else {
            epgMatch.setFileNamePattern("(%y-%m-%d %h-%M) %n %c");
        }
        epgMatch.setCaptureType(-1);
        store.addEpgMatch(epgMatch, 0);
        Element message = xmlDoc.createTextElement("message", "Auto Add item added!");
        xmlDoc.getRoot().appendChild(message);
        out.write(xmlDoc.getDocBytes());
    }

    public void addSchFromItem(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        ThreadLock.getInstance().getLock();
        try {
            addSchFromItemInternal(urlData, headers, out);
        } finally {
            ThreadLock.getInstance().releaseLock();
        }
    }

    private void addSchFromItemInternal(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("auto_add_action");
        String channel = urlData.getParameter("channel");
        String id = urlData.getParameter("id");
        GuideStore guide = GuideStore.getInstance();
        String epgChan = guide.getEpgChannelFromMap(channel);
        GuideItem guideItem = guide.getProgram(epgChan, id);
        if (epgChan == null || epgChan.length() == 0) {
            Element error = xmlDoc.createTextElement("error", "WS Channel Not Found!");
            xmlDoc.getRoot().appendChild(error);
            out.write(xmlDoc.getDocBytes());
            return;
        }
        int captype = -1;
        ScheduleItem schItem = new ScheduleItem(guideItem, channel, captype, store.rand.nextLong(), false);
        String task = store.getProperty("tasks.deftask");
        schItem.setPostTask(task);
        int startBuff = 0;
        int endBuff = 0;
        int endBuffEpg = 0;
        try {
            startBuff = Integer.parseInt(store.getProperty("schedule.buffer.start"));
            endBuff = Integer.parseInt(store.getProperty("schedule.buffer.end"));
            endBuffEpg = Integer.parseInt(store.getProperty("schedule.buffer.end.epg"));
        } catch (Exception e) {
        }
        String[] patterns = store.getNamePatterns();
        schItem.setFilePattern(patterns[0]);
        String keepFor = store.getProperty("autodel.keepfor");
        int keepInt = 30;
        try {
            keepInt = Integer.parseInt(keepFor);
        } catch (Exception e) {
        }
        schItem.setKeepFor(keepInt);
        schItem.setCapType(captype);
        Calendar cal = Calendar.getInstance();
        cal.setTime(schItem.getStart());
        cal.add(Calendar.MINUTE, (startBuff * -1));
        if (endBuffEpg > 0) {
            endBuffEpg = endBuffEpg * (int) (guideItem.getDuration() / 60);
        }
        schItem.setDuration(guideItem.getDuration() + startBuff + endBuff + endBuffEpg);
        schItem.setStart(cal);
        schItem.setType(ScheduleItem.ONCE);
        schItem.log("New Schedule added/edited");
        boolean isAlreadyInList = guide.isAlreadyInList(schItem, 1);
        String actionResult = "";
        if (!isAlreadyInList) {
            store.addScheduleItem(schItem);
            actionResult = "New Schedule Added";
        } else {
            actionResult = "Schedule NOT Added, Alreay in list!";
        }
        Element message = xmlDoc.createTextElement("message", actionResult);
        xmlDoc.getRoot().appendChild(message);
        out.write(xmlDoc.getDocBytes());
    }

    public void getEpgItem(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        XmlDoc xmlDoc = new XmlDoc("epg_item");
        String ws_channel = urlData.getParameter("ws_channel");
        String epg_channel = urlData.getParameter("epg_channel");
        String id = urlData.getParameter("id");
        GuideStore epgStore = GuideStore.getInstance();
        GuideItem item = epgStore.getProgram(epg_channel, id);
        if (item == null) {
            Element error = xmlDoc.createTextElement("error", "Epg item not found");
            xmlDoc.getRoot().appendChild(error);
            out.write(xmlDoc.getDocBytes());
            return;
        }
        ScheduleItem[] schItems = store.getScheduleArray();
        ScheduleItem programSchedule = null;
        for (int schIndex = 0; schIndex < schItems.length; schIndex++) {
            ScheduleItem sch = schItems[schIndex];
            GuideItem createdFrom = sch.getCreatedFrom();
            if (createdFrom != null) {
                if (createdFrom.matches(item)) {
                    programSchedule = sch;
                    break;
                }
            }
        }
        Element program = xmlDoc.createElement("epg_item");
        Element idElement = xmlDoc.createTextElement("id", item.toString());
        program.appendChild(idElement);
        Element titleElement = xmlDoc.createTextElement("title", removeChars(item.getName()));
        program.appendChild(titleElement);
        Element subTitleElement = xmlDoc.createTextElement("sub-title", item.getSubName());
        program.appendChild(subTitleElement);
        Element ignoredElement = xmlDoc.createTextElement("ignored", new Boolean(item.getIgnored()).toString());
        program.appendChild(ignoredElement);
        for (int index = 0; index < item.getCategory().size(); index++) {
            Element catElement = xmlDoc.createTextElement("category", item.getCategory().get(index));
            program.appendChild(catElement);
        }
        Element descElement = xmlDoc.createTextElement("desc", removeChars(item.getDescription()));
        program.appendChild(descElement);
        Element programLengthElement = xmlDoc.createTextElement("duration-length", new Long(item.getDuration()).toString());
        program.appendChild(programLengthElement);
        Element fullTimes = xmlDoc.createElement("times");
        Calendar cal = Calendar.getInstance();
        cal.setTime(item.getStart());
        Element time = xmlDoc.createElement("start");
        time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
        time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
        time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
        time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        fullTimes.appendChild(time);
        cal.setTime(item.getStop());
        time = xmlDoc.createElement("stop");
        time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
        time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
        time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
        time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        fullTimes.appendChild(time);
        program.appendChild(fullTimes);
        Element channels = xmlDoc.createElement("program_channel");
        channels.setAttribute("chan-display-name", ws_channel);
        channels.setAttribute("chan-epgdata-name", epg_channel);
        program.appendChild(channels);
        Element schElement = xmlDoc.createElement("scheduled");
        if (programSchedule == null) {
            schElement.setAttribute("state", "-1");
            schElement.setAttribute("id", "-1");
        } else {
            schElement.setAttribute("state", new Integer(programSchedule.getState()).toString());
            schElement.setAttribute("id", programSchedule.toString());
        }
        program.appendChild(schElement);
        xmlDoc.getRoot().appendChild(program);
        out.write(xmlDoc.getDocBytes());
    }

    public void getEpgData(HTTPurl urlData, HashMap<String, String> headers, OutputStream out) throws Exception {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        int year = -1;
        try {
            year = Integer.parseInt(urlData.getParameter("year"));
        } catch (Exception e) {
        }
        if (year == -1) year = now.get(Calendar.YEAR);
        int month = -1;
        try {
            month = Integer.parseInt(urlData.getParameter("month"));
        } catch (Exception e) {
        }
        if (month == -1) month = now.get(Calendar.MONTH) + 1;
        int day = -1;
        try {
            day = Integer.parseInt(urlData.getParameter("day"));
        } catch (Exception e) {
        }
        if (day == -1) day = now.get(Calendar.DATE);
        int startHour = -1;
        try {
            startHour = Integer.parseInt(urlData.getParameter("start"));
        } catch (Exception e) {
        }
        if (startHour == -1) startHour = now.get(Calendar.HOUR_OF_DAY);
        int timeSpan = 3;
        try {
            timeSpan = Integer.parseInt(urlData.getParameter("span"));
        } catch (Exception e) {
        }
        XmlDoc xmlDoc = new XmlDoc("epg");
        xmlDoc.getRoot().setAttribute("year", new Integer(year).toString());
        xmlDoc.getRoot().setAttribute("month", new Integer(month).toString());
        xmlDoc.getRoot().setAttribute("day", new Integer(day).toString());
        xmlDoc.getRoot().setAttribute("hour", new Integer(startHour).toString());
        xmlDoc.getRoot().setAttribute("span", new Integer(timeSpan).toString());
        Calendar startPointer = Calendar.getInstance();
        startPointer.set(Calendar.SECOND, 0);
        startPointer.set(Calendar.MINUTE, 0);
        startPointer.set(Calendar.MILLISECOND, 0);
        startPointer.set(Calendar.YEAR, year);
        startPointer.set(Calendar.MONTH, month - 1);
        startPointer.set(Calendar.DATE, day);
        startPointer.set(Calendar.HOUR_OF_DAY, startHour);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, year);
        start.set(Calendar.MONTH, month - 1);
        start.set(Calendar.DATE, day);
        start.set(Calendar.HOUR_OF_DAY, startHour);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.add(Calendar.SECOND, -1);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = Calendar.getInstance();
        end.setTime(start.getTime());
        end.add(Calendar.HOUR_OF_DAY, timeSpan);
        Calendar startTime = Calendar.getInstance();
        startTime.set(Calendar.MILLISECOND, 0);
        GuideStore epgStore = GuideStore.getInstance();
        Vector<String[]> channelMap = epgStore.getChannelMap();
        Set<String> wsChannels = store.getChannels().keySet();
        for (int x = 0; x < channelMap.size(); x++) {
            Element channel = xmlDoc.createElement("channel");
            Element programsElem = xmlDoc.createElement("programs");
            String[] map = (String[]) channelMap.get(x);
            String channelName = map[0];
            if (channelName != null && wsChannels.contains(map[0])) {
                channel.setAttribute("display-name", map[0]);
                channel.setAttribute("epgdata-name", map[1]);
                GuideItem[] programs = epgStore.getProgramsInc(start.getTime(), end.getTime(), map[1]);
                Vector<ScheduleItem> schItems = new Vector<ScheduleItem>();
                store.getSchedulesWhenInc(start.getTime(), end.getTime(), channelName, schItems);
                int colCount = 0;
                for (int y = 0; y < programs.length; y++) {
                    GuideItem item = programs[y];
                    start.add(Calendar.SECOND, 1);
                    startTime.setTime(item.getStart());
                    long pastStart = startTime.getTime().getTime() - start.getTime().getTime();
                    if (y == 0 && pastStart > 0) {
                        Element program_PH = xmlDoc.createElement("program");
                        Element idElement = xmlDoc.createTextElement("id", "-1");
                        program_PH.appendChild(idElement);
                        Element titleElement = xmlDoc.createTextElement("title", "EMPTY");
                        program_PH.appendChild(titleElement);
                        Element subTitleElement = xmlDoc.createTextElement("sub-title", "EMPTY");
                        program_PH.appendChild(subTitleElement);
                        Element descElement = xmlDoc.createTextElement("desc", "EMPTY");
                        program_PH.appendChild(descElement);
                        String lengthText = new Long(pastStart / (1000 * 60)).toString();
                        Element lengthElement = xmlDoc.createTextElement("length", lengthText);
                        lengthElement.setAttribute("units", "minutes");
                        program_PH.appendChild(lengthElement);
                        String plString = new Long(pastStart / (1000 * 60)).toString();
                        Element programLengthElement = xmlDoc.createTextElement("programLength", plString);
                        programLengthElement.setAttribute("units", "minutes");
                        program_PH.appendChild(programLengthElement);
                        programsElem.appendChild(program_PH);
                        colCount += (int) (pastStart / (1000 * 60));
                    }
                    start.add(Calendar.SECOND, -1);
                    if (y > 0) {
                        long skip = item.getStart().getTime() - (programs[y - 1].getStart().getTime() + (programs[y - 1].getDuration() * 1000 * 60));
                        if (skip > 0) {
                            System.out.println("Skipping : " + skip);
                            Element program_PH = xmlDoc.createElement("program");
                            Element idElement = xmlDoc.createTextElement("id", "-1");
                            program_PH.appendChild(idElement);
                            Element titleElement = xmlDoc.createTextElement("title", "EMPTY");
                            program_PH.appendChild(titleElement);
                            Element subTitleElement = xmlDoc.createTextElement("sub-title", "EMPTY");
                            program_PH.appendChild(subTitleElement);
                            Element descElement = xmlDoc.createTextElement("desc", "EMPTY");
                            program_PH.appendChild(descElement);
                            String lengthText = new Long(skip / (1000 * 60)).toString();
                            Element lengthElement = xmlDoc.createTextElement("length", lengthText);
                            lengthElement.setAttribute("units", "minutes");
                            program_PH.appendChild(lengthElement);
                            String programLengthText = new Long(skip / (1000 * 60)).toString();
                            Element programLengthElement = xmlDoc.createTextElement("programLength", programLengthText);
                            programLengthElement.setAttribute("units", "minutes");
                            program_PH.appendChild(programLengthElement);
                            programsElem.appendChild(program_PH);
                            colCount += (int) (skip / (1000 * 60));
                        }
                    }
                    ScheduleItem programSchedule = null;
                    for (int schIndex = 0; schIndex < schItems.size(); schIndex++) {
                        ScheduleItem sch = schItems.get(schIndex);
                        GuideItem createdFrom = sch.getCreatedFrom();
                        if (createdFrom != null) {
                            if (createdFrom.matches(item)) {
                                schItems.remove(schIndex);
                                programSchedule = sch;
                                break;
                            }
                        }
                    }
                    Element program = xmlDoc.createElement("program");
                    Element idElement = xmlDoc.createTextElement("id", item.toString());
                    program.appendChild(idElement);
                    Element titleElement = xmlDoc.createTextElement("title", removeChars(item.getName()));
                    program.appendChild(titleElement);
                    Element subTitleElement = xmlDoc.createTextElement("sub-title", item.getSubName());
                    program.appendChild(subTitleElement);
                    for (int index = 0; index < item.getCategory().size(); index++) {
                        Element catElement = xmlDoc.createTextElement("category", item.getCategory().get(index));
                        program.appendChild(catElement);
                    }
                    Element descElement = xmlDoc.createTextElement("desc", removeChars(item.getDescription()));
                    program.appendChild(descElement);
                    int fits = 0;
                    int colSpan = item.getDuration();
                    if (item.getStart().getTime() < start.getTime().getTime() && item.getStop().getTime() > end.getTime().getTime()) {
                        fits = 1;
                        colSpan = (timeSpan * 60);
                    } else if (y == 0 && start.getTime().getTime() > item.getStart().getTime()) {
                        fits = 2;
                        colSpan -= ((start.getTime().getTime() - item.getStart().getTime()) / (1000 * 60)) + 1;
                    } else if (y == programs.length - 1 && (item.getStop().getTime() - 5000) > end.getTime().getTime()) {
                        fits = 3;
                        colSpan = (timeSpan * 60) - colCount;
                    }
                    colCount += colSpan;
                    Element lengthElement = xmlDoc.createTextElement("display-length", new Integer(colSpan).toString());
                    lengthElement.setAttribute("fits", new Integer(fits).toString());
                    program.appendChild(lengthElement);
                    Element programLengthElement = xmlDoc.createTextElement("duration-length", new Long(item.getDuration()).toString());
                    program.appendChild(programLengthElement);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(item.getStart());
                    Element fullTimes = xmlDoc.createElement("times");
                    Element time = xmlDoc.createElement("start");
                    time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
                    time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
                    time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
                    time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
                    time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
                    fullTimes.appendChild(time);
                    cal.setTime(item.getStop());
                    time = xmlDoc.createElement("stop");
                    time.setAttribute("year", new Integer(cal.get(Calendar.YEAR)).toString());
                    time.setAttribute("month", new Integer(cal.get(Calendar.MONTH) + 1).toString());
                    time.setAttribute("day", new Integer(cal.get(Calendar.DATE)).toString());
                    time.setAttribute("hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
                    time.setAttribute("minute", new Integer(cal.get(Calendar.MINUTE)).toString());
                    fullTimes.appendChild(time);
                    program.appendChild(fullTimes);
                    Element channels = xmlDoc.createElement("program_channel");
                    channels.setAttribute("chan-display-name", map[0]);
                    channels.setAttribute("chan-epgdata-name", map[1]);
                    program.appendChild(channels);
                    Element schElement = xmlDoc.createElement("scheduled");
                    if (programSchedule == null) {
                        schElement.setAttribute("state", "-1");
                        schElement.setAttribute("id", "-1");
                    } else {
                        schElement.setAttribute("state", new Integer(programSchedule.getState()).toString());
                        schElement.setAttribute("id", programSchedule.toString());
                    }
                    program.appendChild(schElement);
                    programsElem.appendChild(program);
                }
                channel.appendChild(programsElem);
            }
            xmlDoc.getRoot().appendChild(channel);
        }
        out.write(xmlDoc.getDocBytes());
    }
}
