import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

class KBAutoAddRes extends HTTPResponse {

    public KBAutoAddRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream, HashMap<String, String> headers) throws Exception {
        if ("01".equals(urlData.getParameter("action"))) {
            outStream.write(showAutoEpgAddForm(urlData, headers));
            return;
        }
        if ("02".equals(urlData.getParameter("action"))) {
            outStream.write(showAutoEpgOptions(urlData, headers));
            return;
        }
        if ("03".equals(urlData.getParameter("action"))) {
            outStream.write(runAutoAddTest(urlData, headers));
            return;
        }
        if ("04".equals(urlData.getParameter("action"))) {
            outStream.write(moveAutoAddItem(urlData, headers));
            return;
        }
        if ("05".equals(urlData.getParameter("action"))) {
            outStream.write(remAutoAddItem(urlData, headers));
            return;
        }
        if ("06".equals(urlData.getParameter("action"))) {
            outStream.write(addAutoEpgString(urlData));
            return;
        } else if ("07".equals(urlData.getParameter("action"))) {
            outStream.write(enableEpgMatchItem(urlData));
            return;
        } else if ("08".equals(urlData.getParameter("action"))) {
            outStream.write(showMatchListMenu(urlData, headers));
            return;
        } else if ("09".equals(urlData.getParameter("action"))) {
            outStream.write(showAddMatchList(urlData, headers));
            return;
        } else if ("10".equals(urlData.getParameter("action"))) {
            outStream.write(showDelMatchList(urlData, headers));
            return;
        } else if ("11".equals(urlData.getParameter("action"))) {
            outStream.write(delMatchList(urlData, headers));
            return;
        } else if ("12".equals(urlData.getParameter("action"))) {
            outStream.write(addMatchList(urlData, headers));
            return;
        } else if ("13".equals(urlData.getParameter("action"))) {
            outStream.write(createAutoAddFromItem(urlData, headers));
            return;
        } else {
            outStream.write(getAutoAddTable(urlData, headers));
            return;
        }
    }

    private byte[] createAutoAddFromItem(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        String backURL = urlData.getCookie("backURL");
        try {
            backURL = URLDecoder.decode(backURL, "UTF-8");
        } catch (Exception e) {
        }
        HashMap<String, EpgMatchList> matchLists = store.getMatchLists();
        String itemID = urlData.getParameter("itemID");
        String wsChan = urlData.getParameter("chan");
        GuideStore guide = GuideStore.getInstance();
        String epgChan = guide.getEpgChannelFromMap(wsChan);
        GuideItem item = guide.getProgram(epgChan, itemID);
        if (item == null) {
            StringBuffer out = new StringBuffer(256);
            out.append("HTTP/1.0 302 Moved Temporarily\n");
            out.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
            return out.toString().getBytes();
        }
        String name = item.getName();
        int nextIndex = 0;
        boolean useInt = false;
        String matchListName = name + " (" + wsChan + ")";
        if (matchLists.containsKey(matchListName)) {
            useInt = true;
            while (matchLists.containsKey(matchListName + "_" + nextIndex)) {
                nextIndex++;
            }
        }
        if (useInt) matchListName = matchListName + "_" + nextIndex;
        EpgMatchList newMatchList = new EpgMatchList();
        Vector<EpgMatchListItem> items = newMatchList.getMatchList();
        EpgMatchListItem newItemTitle = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        newItemTitle.setTextSearchData(name, EpgMatchListItem.FIELD_TITLE, true, EpgMatchListItem.FLAG_CASEINSENSATIVE);
        items.add(newItemTitle);
        EpgMatchListItem newItemChan = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        newItemChan.setTextSearchData(wsChan, EpgMatchListItem.FIELD_CHANNEL, true, EpgMatchListItem.FLAG_NONE);
        items.add(newItemChan);
        matchLists.put(matchListName, newMatchList);
        store.saveMatchList(null);
        EpgMatch epgMatch = new EpgMatch();
        epgMatch.getMatchListNames().add(matchListName);
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
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("back", "/servlet/KBAutoAddRes");
        root.setAttribute("title", "The Auto-Add item was created and saved. You should probably run the Auto-Add scan now to add any programs that match your new Auto-Add item to the schedule list.");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("button");
        button.setAttribute("name", "Run Auto-Add Scan Now");
        elm = doc.createElement("url");
        text = doc.createTextNode("/servlet/KBEpgDataRes?action=04");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        button = doc.createElement("button");
        button.setAttribute("name", "Return to the EPG");
        elm = doc.createElement("url");
        text = doc.createTextNode(backURL);
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        XSL transformer = new XSL(doc, "kb-buttons.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] addMatchList(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch item = (EpgMatch) store.getEpgMatchList().get(index);
        if (item == null) {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
            return out.getBytes();
        }
        String name = urlData.getParameter("name");
        if (name != null) {
            if (!item.getMatchListNames().contains(name)) {
                item.getMatchListNames().add(name);
                store.saveMatchList(null);
            }
        }
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return buff.toString().getBytes();
    }

    private byte[] delMatchList(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch item = (EpgMatch) store.getEpgMatchList().get(index);
        if (item == null) {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
            return out.getBytes();
        }
        String name = urlData.getParameter("name");
        if (name != null) {
            item.getMatchListNames().remove(name);
            store.saveMatchList(null);
        }
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showDelMatchList(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch item = (EpgMatch) store.getEpgMatchList().get(index);
        if (item == null) {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
            return out.getBytes();
        }
        String start = urlData.getParameter("start");
        if (start == null || start.length() == 0) start = "0";
        String show = urlData.getParameter("show");
        if (show == null || show.length() == 0) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("back", "/servlet/" + urlData.getServletClass() + "?action=08&index=" + index);
        root.setAttribute("title", "Select a Match List to Delete it");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/" + urlData.getServletClass() + "?action=10&index=" + index + "&");
        button.appendChild(text);
        root.appendChild(button);
        String[] keys = (String[]) item.getMatchListNames().toArray(new String[0]);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        int total = 0;
        for (int x = 0; x < keys.length; x++) {
            String action = "/servlet/KBAutoAddRes?action=11&index=" + index + "&name=" + URLEncoder.encode(keys[x], "UTF-8");
            button = doc.createElement("button");
            button.setAttribute("name", keys[x]);
            elm = doc.createElement("url");
            text = doc.createTextNode(action);
            elm.appendChild(text);
            button.appendChild(elm);
            elm = doc.createElement("confirm");
            text = doc.createTextNode("true");
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
            total++;
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showAddMatchList(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch item = (EpgMatch) store.getEpgMatchList().get(index);
        if (item == null) {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
            return out.getBytes();
        }
        String start = urlData.getParameter("start");
        if (start == null || start.length() == 0) start = "0";
        String show = urlData.getParameter("show");
        if (show == null || show.length() == 0) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("back", "/servlet/" + urlData.getServletClass() + "?action=08&index=" + index);
        root.setAttribute("title", "Select a Match List to Add it");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/" + urlData.getServletClass() + "?action=09&index=" + index + "&");
        button.appendChild(text);
        root.appendChild(button);
        HashMap<String, EpgMatchList> matches = store.getMatchLists();
        String[] keys = (String[]) matches.keySet().toArray(new String[0]);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        int total = 0;
        for (int x = 0; x < keys.length; x++) {
            String action = "/servlet/KBAutoAddRes?action=12&index=" + index + "&name=" + URLEncoder.encode(keys[x], "UTF-8");
            button = doc.createElement("button");
            button.setAttribute("name", keys[x]);
            elm = doc.createElement("url");
            text = doc.createTextNode(action);
            elm.appendChild(text);
            button.appendChild(elm);
            elm = doc.createElement("confirm");
            text = doc.createTextNode("true");
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
            total++;
        }
        root.setAttribute("total", new Integer(total).toString());
        XSL transformer = new XSL(doc, "kb-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showMatchListMenu(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch item = (EpgMatch) store.getEpgMatchList().get(index);
        if (item == null) {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
            return out.getBytes();
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("back", "/servlet/" + urlData.getServletClass() + "?action=02&index=" + index);
        root.setAttribute("title", "Auto-Add Match List Menu");
        Element button = null;
        Element elm = null;
        Text text = null;
        String actionURL = "";
        button = doc.createElement("button");
        button.setAttribute("name", "Back");
        elm = doc.createElement("url");
        actionURL = "/servlet/" + urlData.getServletClass() + "?action=02&index=" + index;
        text = doc.createTextNode(actionURL);
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        if (item.getMatchListNames().size() > 0) {
            button = doc.createElement("button");
            button.setAttribute("name", "Show Current");
            elm = doc.createElement("url");
            actionURL = "/servlet/" + urlData.getServletClass() + "?action=10&index=" + index;
            text = doc.createTextNode(actionURL);
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
        }
        if (store.getMatchLists().size() > 0) {
            button = doc.createElement("button");
            button.setAttribute("name", "Add");
            elm = doc.createElement("url");
            actionURL = "/servlet/" + urlData.getServletClass() + "?action=09&index=" + index;
            text = doc.createTextNode(actionURL);
            elm.appendChild(text);
            button.appendChild(elm);
            root.appendChild(button);
        }
        XSL transformer = new XSL(doc, "kb-buttons.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] enableEpgMatchItem(HTTPurl urlData) throws Exception {
        int indexOf = Integer.parseInt(urlData.getParameter("index"));
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = (EpgMatch) list.get(indexOf);
        if (epgMatcher != null) {
            if ("true".equals(urlData.getParameter("enabled"))) epgMatcher.setEnabled(true); else epgMatcher.setEnabled(false);
            store.saveEpgAutoList(null);
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "\n\n";
        return out.getBytes();
    }

    private byte[] addAutoEpgString(HTTPurl urlData) throws Exception {
        String ad = urlData.getParameter("autoDel");
        boolean autoDel = false;
        if (ad != null && ad.equalsIgnoreCase("true")) autoDel = true;
        int keepFor = 30;
        try {
            keepFor = Integer.parseInt(urlData.getParameter("keepFor"));
        } catch (Exception e) {
        }
        int startBuffer = 0;
        int endBuffer = 0;
        try {
            startBuffer = Integer.parseInt(urlData.getParameter("startbuffer"));
            endBuffer = Integer.parseInt(urlData.getParameter("endbuffer"));
        } catch (Exception e) {
        }
        String postTask = urlData.getParameter("task");
        if (postTask == null || postTask.equalsIgnoreCase("none")) postTask = "";
        String namePattern = urlData.getParameter("filenamePatterns");
        if (namePattern == null || namePattern.length() == 0) namePattern = "(%y-%m-%d %h-%M) %n %c";
        int capType = 0;
        try {
            capType = Integer.parseInt(urlData.getParameter("captype"));
        } catch (Exception e) {
        }
        int capPathIndex = -1;
        try {
            capPathIndex = Integer.parseInt(urlData.getParameter("capPath"));
        } catch (Exception e) {
        }
        EpgMatch epgMatch = null;
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        if (index > -1) {
            epgMatch = (EpgMatch) store.getEpgMatchList().get(index);
        } else {
            epgMatch = new EpgMatch();
            store.addEpgMatch(epgMatch, 0);
        }
        epgMatch.setKeepFor(keepFor);
        epgMatch.setAutoDel(autoDel);
        epgMatch.setStartBuffer(startBuffer);
        epgMatch.setEndBuffer(endBuffer);
        epgMatch.setPostTask(postTask);
        epgMatch.setFileNamePattern(namePattern);
        epgMatch.setCaptureType(capType);
        epgMatch.setCapturePathIndex(capPathIndex);
        store.saveEpgAutoList(null);
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return buff.toString().getBytes();
    }

    private byte[] moveAutoAddItem(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int id = -1;
        try {
            id = Integer.parseInt(urlData.getParameter("id"));
        } catch (Exception e) {
        }
        int dir = 1;
        try {
            dir = Integer.parseInt(urlData.getParameter("dir"));
        } catch (Exception e) {
        }
        boolean direction = false;
        if (dir == 1) direction = true;
        if (id != -1) store.moveEpgItem(id, direction);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/KBAutoAddRes\n\n");
        return buff.toString().getBytes();
    }

    private byte[] runAutoAddTest(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = Integer.parseInt(urlData.getParameter("index"));
        EpgMatch epgMatcher = (EpgMatch) store.getEpgMatchList().get(index);
        HashMap<String, Vector<GuideItem>> results = new HashMap<String, Vector<GuideItem>>();
        Vector<String> matchNames = epgMatcher.getMatchListNames();
        HashMap<String, EpgMatchList> matchLists = store.getMatchLists();
        GuideStore guide = GuideStore.getInstance();
        EpgMatchList matcher = null;
        for (int nameIndex = 0; nameIndex < matchNames.size(); nameIndex++) {
            String matchListName = (String) matchNames.get(nameIndex);
            matcher = (EpgMatchList) matchLists.get(matchListName);
            if (matcher != null) {
                guide.searchEPG(matcher, results);
            }
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("id", "");
        root.setAttribute("url", "/servlet/KBAutoAddRes?action=02&index=" + index);
        root.setAttribute("filter", "");
        Element logitem = null;
        Element elm = null;
        Text text = null;
        Vector<String[]> channelMap = guide.getChannelMap();
        Set<String> wsChannels = store.getChannels().keySet();
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM d h:mm aa");
        int count = 0;
        for (int y = 0; y < channelMap.size(); y++) {
            String[] map = (String[]) channelMap.get(y);
            Vector<GuideItem> result = results.get(map[0]);
            if (result.size() > 0 && wsChannels.contains(map[0])) {
                logitem = doc.createElement("logitem");
                logitem.setAttribute("type", "1");
                elm = doc.createElement("line");
                text = doc.createTextNode(map[0]);
                elm.appendChild(text);
                logitem.appendChild(elm);
                root.appendChild(logitem);
                for (int x = 0; x < result.size(); x++) {
                    GuideItem item = (GuideItem) result.get(x);
                    logitem = doc.createElement("logitem");
                    logitem.setAttribute("type", "0");
                    elm = doc.createElement("line");
                    String matchText = item.getName();
                    matchText += " (" + df.format(item.getStart(), new StringBuffer(), new FieldPosition(0)).toString() + ")";
                    text = doc.createTextNode(matchText);
                    elm.appendChild(text);
                    logitem.appendChild(elm);
                    root.appendChild(logitem);
                    count++;
                }
            }
        }
        if (count == 0) {
            logitem = doc.createElement("logitem");
            logitem.setAttribute("type", "0");
            elm = doc.createElement("line");
            text = doc.createTextNode("No Hits");
            elm.appendChild(text);
            logitem.appendChild(elm);
            root.appendChild(logitem);
        }
        XSL transformer = new XSL(doc, "kb-searchtest.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] showAutoEpgOptions(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        int index = -1;
        try {
            index = Integer.parseInt(urlData.getParameter("index"));
        } catch (Exception e) {
        }
        EpgMatch epgMatcher = (EpgMatch) (store.getEpgMatchList().get(index));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "buttons", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("back", "/servlet/KBAutoAddRes");
        Element button = null;
        Element elm = null;
        Text text = null;
        button = doc.createElement("button");
        button.setAttribute("name", "Back");
        elm = doc.createElement("url");
        text = doc.createTextNode("/servlet/KBAutoAddRes");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        button = doc.createElement("button");
        String action = "/servlet/KBAutoAddRes?action=07&index=" + index;
        if (epgMatcher.isEnabled()) {
            button.setAttribute("name", "Disable");
            action += "&enabled=false";
        } else {
            button.setAttribute("name", "Enable");
            action += "&enabled=true";
        }
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        elm = doc.createElement("confirm");
        text = doc.createTextNode("false");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/" + urlData.getServletClass() + "?action=08&index=" + index;
        button = doc.createElement("button");
        button.setAttribute("name", "Match Lists");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/" + urlData.getServletClass() + "?action=01&index=" + index;
        button = doc.createElement("button");
        button.setAttribute("name", "Edit Options");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/" + urlData.getServletClass() + "?action=03&index=" + index;
        button = doc.createElement("button");
        button.setAttribute("name", "Show Matches");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/" + urlData.getServletClass() + "?action=04&id=" + index + "&dir=0";
        button = doc.createElement("button");
        button.setAttribute("name", "Move Up");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        elm = doc.createElement("confirm");
        text = doc.createTextNode("false");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/" + urlData.getServletClass() + "?action=04&id=" + index + "&dir=1";
        button = doc.createElement("button");
        button.setAttribute("name", "Move Down");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        elm = doc.createElement("confirm");
        text = doc.createTextNode("false");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        action = "/servlet/KBAutoAddRes?action=05&id=" + index;
        button = doc.createElement("button");
        button.setAttribute("name", "Delete");
        elm = doc.createElement("url");
        text = doc.createTextNode(action);
        elm.appendChild(text);
        button.appendChild(elm);
        elm = doc.createElement("confirm");
        text = doc.createTextNode("true");
        elm.appendChild(text);
        button.appendChild(elm);
        root.appendChild(button);
        XSL transformer = new XSL(doc, "kb-buttons.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] remAutoAddItem(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        String idString = urlData.getParameter("id");
        int id = -1;
        if (idString != null) {
            try {
                id = Integer.parseInt(idString);
            } catch (Exception e) {
            }
        }
        String all = urlData.getParameter("all");
        if (id > -1) {
            EpgMatch item = (EpgMatch) store.getEpgMatchList().get(id);
            if (item != null) {
                String[] unUsed = noSharedMatchLists(item);
                if (unUsed.length == 0 || (unUsed.length > 0 && "0".equalsIgnoreCase(all))) {
                    store.remEpgMatch(id);
                } else if (unUsed.length > 0 && "1".equalsIgnoreCase(all)) {
                    store.remEpgMatch(id);
                    for (int x = 0; x < unUsed.length; x++) {
                        store.getMatchLists().remove(unUsed[x]);
                    }
                    store.saveMatchList(null);
                } else {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    DOMImplementation di = db.getDOMImplementation();
                    Document doc = di.createDocument("", "buttons", null);
                    Element root = doc.getDocumentElement();
                    root.setAttribute("back", "/servlet/" + urlData.getServletClass());
                    root.setAttribute("title", "Delete any unused Match Lists as Well?");
                    Element button = null;
                    Element elm = null;
                    Text text = null;
                    String actionURL = "";
                    button = doc.createElement("button");
                    button.setAttribute("name", "Yes");
                    elm = doc.createElement("url");
                    actionURL = "/servlet/" + urlData.getServletClass() + "?action=05&id=" + id + "&all=1";
                    text = doc.createTextNode(actionURL);
                    elm.appendChild(text);
                    button.appendChild(elm);
                    root.appendChild(button);
                    button = doc.createElement("button");
                    button.setAttribute("name", "No");
                    elm = doc.createElement("url");
                    actionURL = "/servlet/" + urlData.getServletClass() + "?action=05&id=" + id + "&all=0";
                    text = doc.createTextNode(actionURL);
                    elm.appendChild(text);
                    button.appendChild(elm);
                    root.appendChild(button);
                    XSL transformer = new XSL(doc, "kb-buttons.xsl", urlData, headers);
                    return transformer.doTransform();
                }
            }
        }
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/KBAutoAddRes\n\n");
        return buff.toString().getBytes();
    }

    private String[] noSharedMatchLists(EpgMatch item) {
        Vector<String> unUsed = new Vector<String>();
        EpgMatch[] items = (EpgMatch[]) store.getEpgMatchList().toArray(new EpgMatch[0]);
        for (int x = 0; x < item.getMatchListNames().size(); x++) {
            boolean used = false;
            String name = (String) item.getMatchListNames().get(x);
            for (int y = 0; y < items.length; y++) {
                if (item != items[y] && items[y].getMatchListNames().contains(name)) used = true;
            }
            if (used == false) unUsed.add(name);
        }
        return (String[]) unUsed.toArray(new String[0]);
    }

    private byte[] showAutoEpgAddForm(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = null;
        String index = urlData.getParameter("index");
        if (index == null) index = "";
        int indexOf = -1;
        try {
            indexOf = Integer.parseInt(index);
        } catch (Exception e) {
        }
        if (indexOf > -1 && indexOf < list.size()) {
            epgMatcher = (EpgMatch) list.get(indexOf);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "item_form", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("index", new Integer(indexOf).toString());
        root.setAttribute("delete", "No");
        Element formEl = null;
        formEl = doc.createElement("startBuffer");
        formEl.setAttribute("Name", "Start");
        formEl.setAttribute("max", "59");
        formEl.setAttribute("min", "0");
        formEl.setAttribute("amount", "1");
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", new Integer(epgMatcher.getStartBuffer()).toString()); else formEl.setAttribute("value", "5");
        root.appendChild(formEl);
        formEl = doc.createElement("endBuffer");
        formEl.setAttribute("Name", "End");
        formEl.setAttribute("max", "400");
        formEl.setAttribute("min", "0");
        formEl.setAttribute("amount", "5");
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", new Integer(epgMatcher.getEndBuffer()).toString()); else formEl.setAttribute("value", "10");
        root.appendChild(formEl);
        formEl = doc.createElement("referer");
        Text text = doc.createTextNode("/servlet/KBAutoAddRes");
        formEl.appendChild(text);
        root.appendChild(formEl);
        formEl = doc.createElement("captureType");
        getCaptureTypes(doc, formEl);
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", new Integer(epgMatcher.getCaptureType()).toString()); else formEl.setAttribute("value", store.getProperty("capture.deftype"));
        root.appendChild(formEl);
        formEl = doc.createElement("autoDel");
        formEl.setAttribute("Name", "Auto Delete");
        if (indexOf > -1 && indexOf < list.size()) if (epgMatcher.getAutoDel()) formEl.setAttribute("value", "True"); else formEl.setAttribute("value", "False"); else formEl.setAttribute("value", "False");
        root.appendChild(formEl);
        formEl = doc.createElement("keepfor");
        formEl.setAttribute("Name", "keep For");
        formEl.setAttribute("max", "120");
        formEl.setAttribute("min", "1");
        formEl.setAttribute("amount", "1");
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", new Integer(epgMatcher.getKeepFor()).toString()); else {
            String keep = store.getProperty("autoDel.keepfor");
            formEl.setAttribute("value", keep);
        }
        root.appendChild(formEl);
        formEl = doc.createElement("posttask");
        getTaskList(doc, formEl);
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", epgMatcher.getPostTask()); else formEl.setAttribute("value", "");
        root.appendChild(formEl);
        formEl = doc.createElement("filenamePatterns");
        getNamePatterns(doc, formEl);
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", epgMatcher.GetFileNamePattern()); else formEl.setAttribute("value", "");
        root.appendChild(formEl);
        formEl = doc.createElement("capturePaths");
        getCapturePaths(doc, formEl);
        if (indexOf > -1 && indexOf < list.size()) formEl.setAttribute("value", new Integer(epgMatcher.getCapturePathIndex()).toString()); else formEl.setAttribute("value", "-1");
        root.appendChild(formEl);
        XSL transformer = new XSL(doc, "kb-aa-details.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private byte[] getAutoAddTable(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = null;
        String start = urlData.getParameter("start");
        if (start == null) start = "0";
        String show = urlData.getParameter("show");
        if (show == null) show = "10";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "schedules", null);
        Element root = doc.getDocumentElement();
        root.setAttribute("start", start);
        root.setAttribute("show", show);
        root.setAttribute("total", new Integer(list.size()).toString());
        Element aa = null;
        Element elm = null;
        Text text = null;
        aa = doc.createElement("mainurl");
        text = doc.createTextNode("/servlet/KBAutoAddRes?");
        aa.appendChild(text);
        root.appendChild(aa);
        for (int x = 0; x < list.size(); x++) {
            epgMatcher = (EpgMatch) list.get(x);
            aa = doc.createElement("aaitem");
            aa.setAttribute("index", new Integer(x).toString());
            Vector<String> items = epgMatcher.getMatchListNames();
            String names = "";
            for (int q = 0; q < items.size(); q++) {
                String name = (String) items.get(q);
                if (q == items.size() - 1) {
                    names += name;
                } else names += name + ", ";
            }
            if (names.length() == 0) names = "No Match List Associated";
            elm = doc.createElement("title");
            text = doc.createTextNode(names);
            elm.appendChild(text);
            aa.appendChild(elm);
            elm = doc.createElement("enabled");
            text = doc.createTextNode(new Boolean(epgMatcher.isEnabled()).toString());
            elm.appendChild(text);
            aa.appendChild(elm);
            elm = doc.createElement("startbuffer");
            text = doc.createTextNode(new Integer(epgMatcher.getStartBuffer()).toString());
            elm.appendChild(text);
            aa.appendChild(elm);
            elm = doc.createElement("endbuffer");
            text = doc.createTextNode(new Integer(epgMatcher.getEndBuffer()).toString());
            elm.appendChild(text);
            aa.appendChild(elm);
            elm = doc.createElement("capturetype");
            text = doc.createTextNode(new Integer(epgMatcher.getCaptureType()).toString());
            elm.appendChild(text);
            aa.appendChild(elm);
            String action = "/servlet/" + urlData.getServletClass() + "?action=02&index=" + URLEncoder.encode(new Integer(x).toString(), "UTF-8");
            elm = doc.createElement("action");
            text = doc.createTextNode(action);
            elm.appendChild(text);
            aa.appendChild(elm);
            root.appendChild(aa);
        }
        XSL transformer = new XSL(doc, "kb-aa-list.xsl", urlData, headers);
        return transformer.doTransform();
    }

    private String getCapturePaths(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        StringBuffer buff = new StringBuffer(1024);
        String[] capturePaths = store.getCapturePaths();
        option = doc.createElement("option");
        option.setAttribute("value", "-1");
        text = doc.createTextNode("AutoSelect");
        option.appendChild(text);
        formEl.appendChild(option);
        for (int x = 0; x < capturePaths.length; x++) {
            option = doc.createElement("option");
            option.setAttribute("value", new Integer(x).toString());
            text = doc.createTextNode(capturePaths[x]);
            option.appendChild(text);
            formEl.appendChild(option);
        }
        return buff.toString();
    }

    private String getNamePatterns(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        StringBuffer buff = new StringBuffer(1024);
        String[] namePatterns = store.getNamePatterns();
        for (int x = 0; x < namePatterns.length; x++) {
            option = doc.createElement("option");
            text = doc.createTextNode(namePatterns[x]);
            option.appendChild(text);
            formEl.appendChild(option);
        }
        return buff.toString();
    }

    private String getCaptureTypes(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        StringBuffer buff = new StringBuffer(1024);
        option = doc.createElement("option");
        option.setAttribute("value", "-1");
        text = doc.createTextNode("AutoSelect");
        option.appendChild(text);
        formEl.appendChild(option);
        Vector<CaptureCapability> capabilities = CaptureCapabilities.getInstance().getCapabilities();
        for (int x = 0; x < capabilities.size(); x++) {
            CaptureCapability capability = capabilities.get(x);
            option = doc.createElement("option");
            option.setAttribute("value", new Integer(capability.getTypeID()).toString());
            text = doc.createTextNode(capability.getName());
            option.appendChild(text);
            formEl.appendChild(option);
        }
        return buff.toString();
    }

    private String getTaskList(Document doc, Element formEl) {
        Element option = null;
        Text text = null;
        StringBuffer buff = new StringBuffer(1024);
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String[] keys = (String[]) tasks.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        option = doc.createElement("option");
        text = doc.createTextNode("none");
        option.appendChild(text);
        formEl.appendChild(option);
        for (int x = 0; x < keys.length; x++) {
            option = doc.createElement("option");
            text = doc.createTextNode(keys[x]);
            option.appendChild(text);
            formEl.appendChild(option);
        }
        return buff.toString();
    }
}
