package ws.system;

import java.io.*;
import java.util.*;
import java.net.*;

class EpgAutoAddDataRes extends HTTPResponse {

    public EpgAutoAddDataRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream, HashMap<String, String> headers) throws Exception {
        if ("01".equals(urlData.getParameter("action"))) {
            outStream.write(showAutoAddList(urlData));
            return;
        } else if ("02".equals(urlData.getParameter("action"))) {
            outStream.write(remAutoAddItem(urlData));
            return;
        } else if ("03".equals(urlData.getParameter("action"))) {
            outStream.write(showAutoEpgAddForm(urlData));
            return;
        } else if ("04".equals(urlData.getParameter("action"))) {
            outStream.write(updateAutoAdd(urlData));
            return;
        } else if ("05".equals(urlData.getParameter("action"))) {
            outStream.write(addAutoAdd(urlData));
            return;
        } else if ("06".equals(urlData.getParameter("action"))) {
            outStream.write(addMatchList(urlData));
            return;
        } else if ("07".equals(urlData.getParameter("action"))) {
            outStream.write(deleteMatchList(urlData));
            return;
        } else if ("08".equals(urlData.getParameter("action"))) {
            outStream.write(editMatchList(urlData));
            return;
        } else if ("10".equals(urlData.getParameter("action"))) {
            outStream.write(addTextMatch(urlData));
            return;
        } else if ("11".equals(urlData.getParameter("action"))) {
            outStream.write(addSpanMatch(urlData));
            return;
        } else if ("12".equals(urlData.getParameter("action"))) {
            outStream.write(addDayMatch(urlData));
            return;
        } else if ("13".equals(urlData.getParameter("action"))) {
            outStream.write(deleteMatchItem(urlData));
            return;
        } else if ("14".equals(urlData.getParameter("action"))) {
            outStream.write(createAutoAddFromItem(urlData));
            return;
        } else if ("15".equals(urlData.getParameter("action"))) {
            ThreadLock.getInstance().getLock();
            try {
                outStream.write(rescanXMLTVdata(urlData));
            } finally {
                ThreadLock.getInstance().releaseLock();
            }
            return;
        } else if ("16".equals(urlData.getParameter("action"))) {
            outStream.write(addMatchList(urlData));
            return;
        } else if ("19".equals(urlData.getParameter("action"))) {
            outStream.write(textInserterPage(urlData));
            return;
        } else if ("20".equals(urlData.getParameter("action"))) {
            outStream.write(moveEpgMatchTo(urlData));
            return;
        } else if ("23".equals(urlData.getParameter("action"))) {
            outStream.write(moveAutoAddItem(urlData));
            return;
        } else if ("25".equals(urlData.getParameter("action"))) {
            outStream.write(showConflicts(urlData));
            return;
        } else if ("26".equals(urlData.getParameter("action"))) {
            outStream.write(exportAutoEpgList(urlData));
            return;
        } else if ("27".equals(urlData.getParameter("action"))) {
            outStream.write(enableEpgMatchItem(urlData));
            return;
        } else if ("28".equals(urlData.getParameter("action"))) {
            outStream.write(showImportAutoAddForm(urlData));
            return;
        } else if ("29".equals(urlData.getParameter("action"))) {
            outStream.write(importAutoAddData(urlData));
            return;
        } else if ("30".equals(urlData.getParameter("action"))) {
            outStream.write(showMatchListImportForm(urlData));
            return;
        } else if ("32".equals(urlData.getParameter("action"))) {
            outStream.write(addFlagMatch(urlData));
            return;
        } else if ("38".equals(urlData.getParameter("action"))) {
            outStream.write(addExcludeToMatchItem(urlData));
            return;
        } else if ("39".equals(urlData.getParameter("action"))) {
            outStream.write(showEditTextPage(urlData));
            return;
        } else if ("40".equals(urlData.getParameter("action"))) {
            outStream.write(updateTextPage(urlData));
            return;
        } else {
            outStream.write("Action Not Supported".getBytes());
            return;
        }
    }

    private byte[] updateTextPage(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        int item_id = Integer.parseInt(urlData.getParameter("item_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        EpgMatchListItem item = list.getMatchList().get(item_id);
        if (match == null || list == null || item == null) {
            throw new Exception("Items not found!");
        }
        if (item.getType() != EpgMatchListItem.TYPE_TEXT) {
            throw new Exception("Invalid Item Type");
        }
        String textData = urlData.getParameter("searchText");
        int field = 0;
        try {
            field = Integer.parseInt(urlData.getParameter("field"));
        } catch (Exception e) {
        }
        boolean exists = true;
        if ("false".equalsIgnoreCase(urlData.getParameter("exists")) == true) exists = false;
        int flags = 0;
        try {
            flags = Integer.parseInt(urlData.getParameter("flags"));
        } catch (Exception e) {
        }
        item.setTextSearchData(textData, field, exists, flags);
        store.saveEpgAutoList(null);
        String pageData = "HTTP/1.0 200 OK\n" + "Content-Type: text/html; charset=utf-8\n" + "Pragma: no-cache\n" + "Cache-Control: no-cache\n\n" + "<html><body onLoad=\"" + "window.opener.location.href = '/servlet/EpgAutoAddDataRes?action=08&match_id=" + match_id + "&list_id=" + list_id + "';" + "window.close();\"></body></html>\n";
        return pageData.getBytes();
    }

    private byte[] showEditTextPage(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-matchListEditText.html");
        template.replaceAll("$title", "Edit Match List Text Item");
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        int item_id = Integer.parseInt(urlData.getParameter("item_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        EpgMatchListItem item = list.getMatchList().get(item_id);
        if (match == null || list == null || item == null) {
            throw new Exception("Items not found!");
        }
        template.replaceAll("$match_id$", new Integer(match_id).toString());
        template.replaceAll("$list_id$", new Integer(match_id).toString());
        template.replaceAll("$item_id$", new Integer(item_id).toString());
        if (item.getType() != EpgMatchListItem.TYPE_TEXT) {
            throw new Exception("Invalid Item Type");
        }
        template.replaceAll("$textValue", item.getTextSearch());
        StringBuffer buff = new StringBuffer();
        buff.append("<OPTION VALUE=\"0\"");
        if (item.getField() == EpgMatchListItem.FIELD_TITLE) buff.append(" selected");
        buff.append(">Title</OPTION>\n");
        buff.append("<OPTION VALUE=\"1\"");
        if (item.getField() == EpgMatchListItem.FIELD_DESCRIPTION) buff.append(" selected");
        buff.append(">Description</OPTION>\n");
        buff.append("<OPTION VALUE=\"2\"");
        if (item.getField() == EpgMatchListItem.FIELD_CHANNEL) buff.append(" selected");
        buff.append(">Channel</OPTION>\n");
        buff.append("<OPTION VALUE=\"3\"");
        if (item.getField() == EpgMatchListItem.FIELD_CATEGORY) buff.append(" selected");
        buff.append(">Category</OPTION>\n");
        template.replaceAll("$fieldOptionData", buff.toString());
        buff = new StringBuffer();
        buff.append("<OPTION VALUE=\"true\"");
        if (item.getExists() == true) buff.append(" selected");
        buff.append(">Exists</OPTION>\n");
        buff.append("<OPTION VALUE=\"false\"");
        if (item.getExists() == false) buff.append(" selected");
        buff.append(">Not Exists</OPTION>\n");
        template.replaceAll("$existsOptionData", buff.toString());
        buff = new StringBuffer();
        buff.append("<OPTION VALUE=\"0\"");
        if (item.getFlags() == EpgMatchListItem.FLAG_NONE) buff.append(" selected");
        buff.append(">Case Sensitive</OPTION>\n");
        buff.append("<OPTION VALUE=\"1\"");
        if (item.getFlags() == EpgMatchListItem.FLAG_CASEINSENSATIVE) buff.append(" selected");
        buff.append(">Case Insensitive</OPTION>\n");
        buff.append("<OPTION VALUE=\"2\"");
        if (item.getFlags() == EpgMatchListItem.FLAG_REGEX) buff.append(" selected");
        buff.append(">RegEx</OPTION>\n");
        template.replaceAll("$flagOtpionData", buff.toString());
        return template.getPageBytes();
    }

    private byte[] addExcludeToMatchItem(HTTPurl urlData) throws Exception {
        String wsChan = urlData.getParameter("chan");
        GuideStore guide = GuideStore.getInstance();
        String epgChan = guide.getEpgChannelFromMap(wsChan);
        String itemID = urlData.getParameter("itemID");
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        GuideItem guide_item = guide.getProgram(epgChan, itemID);
        if (match == null || list == null) {
            throw new Exception("Items not found!");
        }
        String newUrl = "/servlet/EpgAutoAddDataRes?action=37&match_id=" + match_id + "&list_id=" + list_id + "&chan=" + URLEncoder.encode(wsChan, "UTF-8");
        EpgMatchListItem newItem = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        newItem.setTextSearchData(guide_item.getName(), EpgMatchListItem.FIELD_TITLE, false, EpgMatchListItem.FLAG_CASEINSENSATIVE);
        list.add(newItem);
        store.saveEpgAutoList(null);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: " + newUrl + "\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showMatchListImportForm(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ImportForm.html");
        String captcha = store.getProperty("security.captcha");
        if ("1".equals(captcha)) {
            template.replaceAll("$usingCAPTCHA$", "true");
        } else {
            template.replaceAll("$usingCAPTCHA$", "false");
        }
        template.replaceAll("$title", "Match List Data Import");
        template.replaceAll("$action", "/servlet/EpgAutoAddDataRes?action=31");
        return template.getPageBytes();
    }

    private byte[] importAutoAddData(HTTPurl urlData) throws Exception {
        String sessionID = urlData.getParameter("sessionID");
        if (!store.checkSessionID(sessionID)) {
            return "Security Warning: The Security Session ID you entered is not correct.".getBytes();
        }
        boolean append = "append".equalsIgnoreCase(urlData.getParameter("data_action"));
        String data = urlData.getParameter("data");
        if (data != null && data.length() > 0) {
            store.importEpgAutoList(data.trim(), append);
        }
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/EpgAutoAddDataRes?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showImportAutoAddForm(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ImportForm.html");
        String captcha = store.getProperty("security.captcha");
        if ("1".equals(captcha)) {
            template.replaceAll("$usingCAPTCHA$", "true");
        } else {
            template.replaceAll("$usingCAPTCHA$", "false");
        }
        template.replaceAll("$title", "Auto-Add Data Import");
        template.replaceAll("$action", "/servlet/EpgAutoAddDataRes?action=29");
        return template.getPageBytes();
    }

    private byte[] moveEpgMatchTo(HTTPurl urlData) throws Exception {
        int id = -1;
        try {
            id = Integer.parseInt(urlData.getParameter("id"));
        } catch (Exception e) {
        }
        int toIndex = -1;
        try {
            toIndex = Integer.parseInt(urlData.getParameter("move"));
        } catch (Exception e) {
        }
        Vector<EpgMatch> mList = store.getEpgMatchList();
        if (id > -1 && toIndex > -1 && toIndex < mList.size()) {
            EpgMatch obj = (EpgMatch) mList.remove(id);
            if (obj != null) {
                mList.add(toIndex, obj);
                store.saveEpgAutoList(null);
            }
        }
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] textInserterPage(HTTPurl urlData) throws Exception {
        StringBuffer buff = new StringBuffer();
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-textInsert.html");
        GuideStore guide = GuideStore.getInstance();
        HashMap<String, Channel> channels = store.getChannels();
        String[] keys = (String[]) channels.keySet().toArray(new String[0]);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        for (int x = 0; x < keys.length; x++) {
            buff.append("<a href='#' onClick=\"addText('" + keys[x] + "');\">" + keys[x] + "</a>");
            if (x < keys.length - 1) buff.append(", ");
        }
        if (buff.length() == 0) buff.append("none");
        template.replaceAll("$channels", buff.toString());
        buff = new StringBuffer();
        String[] cats = guide.getCategoryStrings();
        Arrays.sort(cats, String.CASE_INSENSITIVE_ORDER);
        for (int x = 0; x < cats.length; x++) {
            buff.append("<a href='#' onClick=\"addText('" + cats[x] + "');\">" + cats[x] + "</a>");
            if (x < cats.length - 1) buff.append(", ");
        }
        if (buff.length() == 0) buff.append("none");
        template.replaceAll("$categories", buff.toString());
        buff = new StringBuffer();
        Vector<String> progNames = new Vector<String>();
        HashMap<String, HashMap<String, GuideItem>> progs = guide.getProgramList();
        keys = (String[]) progs.keySet().toArray(new String[0]);
        for (int x = 0; x < keys.length; x++) {
            HashMap<String, GuideItem> channelProgs = (HashMap<String, GuideItem>) progs.get(keys[x]);
            GuideItem[] items = (GuideItem[]) channelProgs.values().toArray(new GuideItem[0]);
            for (int y = 0; y < items.length; y++) {
                if (!progNames.contains(items[y].getName())) progNames.add(items[y].getName());
            }
        }
        String[] names = (String[]) progNames.toArray(new String[0]);
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        for (int x = 0; x < names.length; x++) {
            String htmlText = HTMLEncoder.encode(names[x]);
            buff.append("<a href='#' onClick=\"addText('" + URLEncoder.encode(names[x], "UTF-8") + "');\">" + htmlText + "</a>");
            if (x < names.length - 1) buff.append(", \n");
        }
        if (buff.length() == 0) buff.append("none");
        template.replaceAll("$programs", buff.toString());
        return template.getPageBytes();
    }

    private byte[] rescanXMLTVdata(HTTPurl urlData) throws Exception {
        GuideStore guide = GuideStore.getInstance();
        StringBuffer buff = new StringBuffer();
        store.removeEPGitems(buff, 1);
        guide.addEPGmatches(buff, 1);
        store.saveSchedule(null);
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-rescan.html");
        template.replaceAll("$result", buff.toString());
        return template.getPageBytes();
    }

    private byte[] showConflicts(HTTPurl urlData) throws Exception {
        GuideStore guide = GuideStore.getInstance();
        Vector<AddConflictDetails> conflicts = guide.getAutoAddConflicts();
        StringBuffer buff = new StringBuffer();
        buff.append("<table border='0' cellspacing='5' cellpadding='5'>");
        buff.append("<tr>\n");
        buff.append("<td nowrap class='itemheading'><strong>Name</strong></td>\n");
        buff.append("<td nowrap class='itemheading'><strong>Reason</strong></td>\n");
        buff.append("<td nowrap class='itemheading'><strong>Conflicts</strong></td>\n");
        buff.append("</tr>\n");
        AddConflictDetails conf = null;
        for (int x = 0; x < conflicts.size(); x++) {
            conf = (AddConflictDetails) conflicts.get(x);
            buff.append("<tr>\n");
            buff.append("<td nowrap valign='top'>" + conf.getName() + "</td>");
            if (conf.getReason() == AddConflictDetails.REASON_WARNING) {
                buff.append("<td nowrap valign='top'><font color='#FFFF00'>" + conf.getDescription() + "</font></td>");
            } else if (conf.getReason() == AddConflictDetails.REASON_ERROR) {
                buff.append("<td nowrap valign='top'><font color='#FF0000'>" + conf.getDescription() + "</font></td>");
            } else {
                buff.append("<td nowrap valign='top'>" + conf.getDescription() + "</td>");
            }
            buff.append("<td nowrap valign='top'>" + conf.getConflict() + "</td>");
            buff.append("</tr>\n");
        }
        buff.append("</table>");
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-conflicts.html");
        template.replaceAll("$title", "EPG Auto-Add conflicts from last run");
        template.replaceAll("$list", buff.toString());
        return template.getPageBytes();
    }

    private byte[] addAutoAdd(HTTPurl urlData) throws Exception {
        EpgMatch epgMatch = new EpgMatch("New Auto Add");
        store.addEpgMatch(epgMatch, 0);
        store.saveEpgAutoList(null);
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] updateAutoAdd(HTTPurl urlData) throws Exception {
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
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String postTask = urlData.getParameter("task");
        if (postTask == null || postTask.equalsIgnoreCase("none") || !tasks.containsKey(postTask)) postTask = "";
        String namePattern = urlData.getParameter("namePattern");
        String[] namePatterns = store.getNamePatterns();
        boolean found = false;
        for (int x = 0; x < namePatterns.length; x++) {
            if (namePatterns[x].equals(namePattern)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new Exception("Name Pattern Not Found!");
        }
        int capType = 0;
        try {
            capType = Integer.parseInt(urlData.getParameter("captype"));
        } catch (Exception e) {
        }
        int capPathIndex = -1;
        try {
            capPathIndex = Integer.parseInt(urlData.getParameter("capturePathIndex"));
        } catch (Exception e) {
        }
        int existsCheckType = 1;
        try {
            existsCheckType = Integer.parseInt(urlData.getParameter("existsCheckType"));
        } catch (Exception e) {
        }
        String item_name = urlData.getParameter("item_name");
        int index = Integer.parseInt(urlData.getParameter("index"));
        EpgMatch epgMatch = (EpgMatch) store.getEpgMatchList().get(index);
        epgMatch.setName(item_name);
        epgMatch.setKeepFor(keepFor);
        epgMatch.setAutoDel(autoDel);
        epgMatch.setStartBuffer(startBuffer);
        epgMatch.setEndBuffer(endBuffer);
        epgMatch.setPostTask(postTask);
        epgMatch.setFileNamePattern(namePattern);
        epgMatch.setCaptureType(capType);
        epgMatch.setCapturePathIndex(capPathIndex);
        epgMatch.setExistingCheckType(existsCheckType);
        store.saveEpgAutoList(null);
        StringBuffer buff = new StringBuffer(256);
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] moveAutoAddItem(HTTPurl urlData) throws Exception {
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
        buff.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showAutoEpgAddForm(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-auto-addform.html");
        String index = urlData.getParameter("index");
        if (index == null) index = "";
        int indexOf = -1;
        try {
            indexOf = Integer.parseInt(index);
        } catch (Exception e) {
        }
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = (EpgMatch) list.get(indexOf);
        Vector<EpgMatchList> matchItems = epgMatcher.getMatchLists();
        StringBuffer buff = new StringBuffer();
        buff.append("<tr><td colspan=\"2\" style=\"border: 1px solid #FFFFFF;\">\n");
        buff.append("<table width='100%'>\n");
        for (int x = 0; x < matchItems.size(); x++) {
            EpgMatchList ml_item = matchItems.get(x);
            Vector<EpgMatchListItem> ml_items = ml_item.getMatchList();
            String ml_name = "";
            for (int y = 0; y < ml_items.size(); y++) {
                EpgMatchListItem listItems = ml_items.get(y);
                if (listItems.getType() == EpgMatchListItem.TYPE_TEXT) {
                    ml_name += "(" + listItems.getTextSearch() + ") ";
                }
                if (listItems.getType() == EpgMatchListItem.TYPE_DAYS) {
                    ml_name += "(";
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_SAT)) {
                        ml_name += "sat,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_SUN)) {
                        ml_name += "sun,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_MON)) {
                        ml_name += "mon,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_TUE)) {
                        ml_name += "tue,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_WED)) {
                        ml_name += "wed,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_THUR)) {
                        ml_name += "thur,";
                    }
                    if (listItems.getDayValue(EpgMatchListItem.DAYS_FRI)) {
                        ml_name += "fri,";
                    }
                    ml_name += ") ";
                }
                if (listItems.getType() == EpgMatchListItem.TYPE_SPAN) {
                    ml_name += "(" + listItems.getSpanFromHour() + ":" + listItems.getSpanFromMin() + "-" + listItems.getSpanToHour() + ":" + listItems.getSpanToMin() + ") ";
                }
                if (listItems.getType() == EpgMatchListItem.TYPE_FLAG) {
                    ml_name += "(";
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_CC) {
                        ml_name += "cc";
                    }
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_HD) {
                        ml_name += "hd";
                    }
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_LIVE) {
                        ml_name += "live";
                    }
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_PREMIERE) {
                        ml_name += "premiere";
                    }
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_REPEAT) {
                        ml_name += "repeat";
                    }
                    if (listItems.getFlagID() == EpgMatchListItem.ITEM_FLAG_WS) {
                        ml_name += "ws";
                    }
                    ml_name += ") ";
                }
            }
            buff.append("<tr><td>" + ml_name + "</td><td align='right'>");
            buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=07&match_id=" + index + "&list_id=" + x + "' onClick='return confirmAction(\"Delete\");'><img src='/images/delete.png' border='0' alt='Delete' width='24' height='24'></a> ");
            buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=08&match_id=" + index + "&list_id=" + x + "'><img src='/images/edit.png' border='0' alt='Edit' width='24' height='24'></a>");
            buff.append("</td>");
            buff.append("</tr>");
        }
        if (matchItems.size() == 0) {
            buff.append("<tr><td><b> No Match Items </b></td></tr>");
        }
        buff.append("</table>\n");
        buff.append("</td></tr>");
        template.replaceAll("$matchList", buff.toString());
        template.replaceAll("$match_id$", new Integer(indexOf).toString());
        template.replaceAll("$startBuff", new Integer(epgMatcher.getStartBuffer()).toString());
        template.replaceAll("$endBuff", new Integer(epgMatcher.getEndBuffer()).toString());
        if (epgMatcher.getAutoDel()) template.replaceAll("$AutoDelete", "checked"); else template.replaceAll("$AutoDelete", "");
        template.replaceAll("$keepFor", new Integer(epgMatcher.getKeepFor()).toString());
        template.replaceAll("$index", index);
        template.replaceAll("$captureTypes", getCaptureTypes(epgMatcher.getCaptureType()));
        template.replaceAll("$fileNamePatterns", getNamePatternList(epgMatcher));
        template.replaceAll("$CapturePath", getCapturePathList(epgMatcher.getCapturePathIndex()));
        template.replaceAll("$tasks", getTaskList(epgMatcher.getPostTask()));
        template.replaceAll("$exists", getExistsCheckTypeList(epgMatcher.getExistingCheckType()));
        template.replaceAll("$item_name$", epgMatcher.getName());
        return template.getPageBytes();
    }

    private String getExistsCheckTypeList(int type) {
        StringBuffer buff = new StringBuffer(1024);
        buff.append("<select name='existsCheckType'>\n");
        buff.append("<option value='0'");
        if (type == 0) buff.append(" selected");
        buff.append(">No Check</option>\n");
        buff.append("<option value='1'");
        if (type == 1) buff.append(" selected");
        buff.append(">Same Channel</option>\n");
        buff.append("<option value='2'");
        if (type == 2) buff.append(" selected");
        buff.append(">Any Channel</option>\n");
        buff.append("</select>\n");
        return buff.toString();
    }

    private String getNamePatternList(EpgMatch item) {
        StringBuffer buff = new StringBuffer(1024);
        String[] namePatterns = store.getNamePatterns();
        buff.append("<select name='namePattern'>\n");
        for (int x = 0; x < namePatterns.length; x++) {
            if ((item == null && x == 0) || (item != null && namePatterns[x].equals(item.GetFileNamePattern()))) buff.append("<option value='" + namePatterns[x] + "' selected>" + namePatterns[x] + "</option>\n"); else buff.append("<option value='" + namePatterns[x] + "'>" + namePatterns[x] + "</option>\n");
        }
        buff.append("</select>");
        return buff.toString();
    }

    private String getCapturePathList(int index) throws Exception {
        StringBuffer buff = new StringBuffer(1024);
        String[] capturePaths = store.getCapturePaths();
        buff.append("<select name='capturePathIndex'>\n");
        if (index == -1) buff.append("<option value='-1' selected>AutoSelect</option>\n"); else buff.append("<option value='-1'>AutoSelect</option>\n");
        for (int x = 0; x < capturePaths.length; x++) {
            String capPath = new File(capturePaths[x]).getCanonicalPath();
            if (x == index) buff.append("<option value='" + x + "' selected>" + capPath + "</option>\n"); else buff.append("<option value='" + x + "'>" + capPath + "</option>\n");
        }
        buff.append("</select>");
        return buff.toString();
    }

    private byte[] remAutoAddItem(HTTPurl urlData) throws Exception {
        int id = Integer.parseInt(urlData.getParameter("id"));
        store.remEpgMatch(id);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/" + urlData.getServletClass() + "?action=01\n\n");
        return buff.toString().getBytes();
    }

    private byte[] exportAutoEpgList(HTTPurl urlData) throws Exception {
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 200 OK\nContent-Type: text/xml\n");
        buff.append("Content-Disposition: attachment; filename=\"EpgAutoAdd.xml\"\n");
        buff.append("Pragma: no-cache\n");
        buff.append("Cache-Control: no-cache\n");
        buff.append("\n");
        store.saveEpgAutoList(buff);
        return buff.toString().getBytes();
    }

    private byte[] enableEpgMatchItem(HTTPurl urlData) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=01\n\n";
        int indexOf = Integer.parseInt(urlData.getParameter("index"));
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = (EpgMatch) list.get(indexOf);
        if (epgMatcher != null) {
            if ("true".equals(urlData.getParameter("enabled"))) epgMatcher.setEnabled(true); else epgMatcher.setEnabled(false);
            store.saveEpgAutoList(null);
        }
        return out.getBytes();
    }

    private byte[] showAutoAddList(HTTPurl urlData) throws Exception {
        Vector<EpgMatch> list = store.getEpgMatchList();
        EpgMatch epgMatcher = null;
        StringBuffer buff = new StringBuffer();
        buff.append("<tr>\n");
        buff.append("<td nowrap class='itemheading'><strong>Enabled</strong></td>\n");
        buff.append("<td nowrap class='itemheading'><strong>Name</strong></td>\n");
        buff.append("<td nowrap class='itemheading' align='center'><strong>Action</strong></td>");
        buff.append("</tr>\n");
        if (list.size() == 0) {
            buff.append("<tr><td colspan='3'><center>No Auto-Add items</center></td></tr>\n");
        }
        for (int x = 0; x < list.size(); x++) {
            epgMatcher = (EpgMatch) list.get(x);
            buff.append("<form action='/servlet/" + urlData.getServletClass() + "' method='POST'>");
            buff.append("<tr>\n");
            if (epgMatcher.isEnabled()) {
                buff.append("<td nowrap align='center'><a href='/servlet/" + urlData.getServletClass() + "?action=27&index=" + x + "&enabled=false'>" + "<img border='0' alt='Yes' src='/images/tick.png' width='24' height='24'></a></td>");
            } else {
                buff.append("<td nowrap align='center'><a href='/servlet/" + urlData.getServletClass() + "?action=27&index=" + x + "&enabled=true'>" + "<img border='0' alt='No' src='/images/stop.png' width='24' height='24'></a></td>");
            }
            buff.append("<td nowrap width='250'>" + epgMatcher.getName() + "</td>");
            buff.append("<td nowrap align='center'>");
            buff.append("<a onClick='return confirmAction(\"Delete\");' href='/servlet/" + urlData.getServletClass() + "?action=02&id=" + x + "'><img align='absmiddle' src='/images/delete.png' border='0' alt='Delete' width='24' height='24'></a> ");
            buff.append(" <a href='/servlet/" + urlData.getServletClass() + "?action=03");
            buff.append("&index=" + x + "'><img align='absmiddle' src='/images/edit.png' border='0' alt='Edit' width='24' height='24'></a> ");
            buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=23&id=" + x + "&dir=0'><img border='0' alt='Up' src='/images/up01.png' width='7' height='7'></a>");
            buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=23&id=" + x + "&dir=1'><img border='0' alt='Down' src='/images/down01.png' width='7' height='7'></a>");
            buff.append("<input type='hidden' name='action' value='20'>");
            buff.append("<input type='hidden' name='id' value='" + x + "'>");
            buff.append(" <select name='move' onChange='submit();'");
            buff.append("style='background-color: #ffffff; color: #000000; border: 1px solid #ffffff; font-size: 10px;'>");
            for (int y = 0; y < list.size(); y++) {
                buff.append("<option value='" + y + "'");
                if (x == y) buff.append(" selected");
                buff.append(">" + y + "</option>");
            }
            buff.append("</select>");
            buff.append("</td></tr>\n");
            buff.append("</form>");
        }
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-auto-add.html");
        template.replaceAll("$list", buff.toString());
        return template.getPageBytes();
    }

    private byte[] createAutoAddFromItem(HTTPurl urlData) throws Exception {
        String backURL = urlData.getCookie("backURL");
        try {
            backURL = URLDecoder.decode(backURL, "UTF-8");
        } catch (Exception e) {
        }
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-autoAddAddConfirmation.html");
        GuideStore guide = GuideStore.getInstance();
        String itemID = urlData.getParameter("itemID");
        String wsChan = urlData.getParameter("chan");
        String epgChan = guide.getEpgChannelFromMap(wsChan);
        GuideItem item = guide.getProgram(epgChan, itemID);
        if (item == null) {
            template.replaceAll("$title", "Auto-Add Error");
            template.replaceAll("$message", "Error adding the Auto-Add item.");
            template.replaceAll("$link01", "<a href='" + backURL + "'>Back to EPG</a>");
            template.replaceAll("$link02", "<a href='/'>Home</a>");
            return template.getPageBytes();
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
        template.replaceAll("$title", "Auto-Add Item Created");
        template.replaceAll("$message", "The Auto-Add item was created and saved.<br>You should probably run the Auto-Add scan now to add any programs<br>that match your new Auto-Add item to the schedule list.");
        template.replaceAll("$link01", " <a class='noUnder' href='/servlet/EpgAutoAddDataRes?action=15'> <strong>Run Auto-Add Scan Now</strong> </a> ");
        template.replaceAll("$link02", " <a class='noUnder' href='" + backURL + "'> <strong>Return to the EPG</strong> </a> ");
        return template.getPageBytes();
    }

    private byte[] addDayMatch(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        if (match == null || list == null) {
            throw new Exception("Items not found");
        }
        EpgMatchListItem newItem = new EpgMatchListItem(EpgMatchListItem.TYPE_DAYS);
        String sun = urlData.getParameter("sun");
        if (sun != null && "1".equals(sun)) newItem.setDayData(EpgMatchListItem.DAYS_SUN, true); else newItem.setDayData(EpgMatchListItem.DAYS_SUN, false);
        String mon = urlData.getParameter("mon");
        if (mon != null && "1".equals(mon)) newItem.setDayData(EpgMatchListItem.DAYS_MON, true); else newItem.setDayData(EpgMatchListItem.DAYS_MON, false);
        String tue = urlData.getParameter("tue");
        if (tue != null && "1".equals(tue)) newItem.setDayData(EpgMatchListItem.DAYS_TUE, true); else newItem.setDayData(EpgMatchListItem.DAYS_TUE, false);
        String wed = urlData.getParameter("wed");
        if (wed != null && "1".equals(wed)) newItem.setDayData(EpgMatchListItem.DAYS_WED, true); else newItem.setDayData(EpgMatchListItem.DAYS_WED, false);
        String thur = urlData.getParameter("thur");
        if (thur != null && "1".equals(thur)) newItem.setDayData(EpgMatchListItem.DAYS_THUR, true); else newItem.setDayData(EpgMatchListItem.DAYS_THUR, false);
        String fri = urlData.getParameter("fri");
        if (fri != null && "1".equals(fri)) newItem.setDayData(EpgMatchListItem.DAYS_FRI, true); else newItem.setDayData(EpgMatchListItem.DAYS_FRI, false);
        String sat = urlData.getParameter("sat");
        if (sat != null && "1".equals(sat)) newItem.setDayData(EpgMatchListItem.DAYS_SAT, true); else newItem.setDayData(EpgMatchListItem.DAYS_SAT, false);
        list.add(newItem);
        store.saveEpgAutoList(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=08&match_id=" + match_id + "&list_id=" + list_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] addSpanMatch(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        if (match == null || list == null) {
            throw new Exception("Items not found");
        }
        EpgMatchListItem newItem = new EpgMatchListItem(EpgMatchListItem.TYPE_SPAN);
        int spanFromHour = -1;
        try {
            spanFromHour = Integer.parseInt(urlData.getParameter("span_from_hour"));
        } catch (Exception e) {
        }
        int spanFromMin = -1;
        try {
            spanFromMin = Integer.parseInt(urlData.getParameter("span_from_min"));
        } catch (Exception e) {
        }
        int spanToHour = -1;
        try {
            spanToHour = Integer.parseInt(urlData.getParameter("span_to_hour"));
        } catch (Exception e) {
        }
        int spanToMin = -1;
        try {
            spanToMin = Integer.parseInt(urlData.getParameter("span_to_min"));
        } catch (Exception e) {
        }
        if (spanFromHour > -1 && spanFromMin > -1 && spanToHour > -1 && spanToMin > -1) {
            newItem.setSpanData(spanFromHour, spanFromMin, spanToHour, spanToMin);
            list.add(newItem);
            store.saveEpgAutoList(null);
        }
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=08&match_id=" + match_id + "&list_id=" + list_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] addFlagMatch(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        if (match == null || list == null) {
            throw new Exception("Items not found");
        }
        EpgMatchListItem newItem = new EpgMatchListItem(EpgMatchListItem.TYPE_FLAG);
        int flagID = -1;
        try {
            flagID = Integer.parseInt(urlData.getParameter("flagID"));
        } catch (Exception e) {
        }
        int flagValue = -1;
        try {
            flagValue = Integer.parseInt(urlData.getParameter("flagValue"));
        } catch (Exception e) {
        }
        if (flagID > -1 && flagValue > -1) {
            boolean flagBoolVal = false;
            if (flagValue == 1) flagBoolVal = true;
            newItem.setFlagData(flagID, flagBoolVal);
            list.add(newItem);
            store.saveEpgAutoList(null);
        }
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=08&match_id=" + match_id + "&list_id=" + list_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] deleteMatchItem(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        int item_id = Integer.parseInt(urlData.getParameter("item_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        if (match == null || list == null) {
            throw new Exception("Items not found");
        }
        list.getMatchList().remove(item_id);
        store.saveEpgAutoList(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=08&match_id=" + match_id + "&list_id=" + list_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] addTextMatch(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList list = match.getMatchLists().get(list_id);
        if (match == null || list == null) {
            throw new Exception("Items not found");
        }
        EpgMatchListItem newItem = new EpgMatchListItem(EpgMatchListItem.TYPE_TEXT);
        String textSearch = urlData.getParameter("searchText");
        int field = -1;
        try {
            field = Integer.parseInt(urlData.getParameter("field"));
        } catch (Exception e) {
        }
        boolean exists = true;
        if ("false".equalsIgnoreCase(urlData.getParameter("exists"))) exists = false;
        int flags = -1;
        try {
            flags = Integer.parseInt(urlData.getParameter("flags"));
        } catch (Exception e) {
        }
        if (textSearch != null && textSearch.length() > 0 && field > -1 && flags > -1) {
            newItem.setTextSearchData(textSearch, field, exists, flags);
            list.add(newItem);
            store.saveEpgAutoList(null);
        }
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=08&match_id=" + match_id + "&list_id=" + list_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] addMatchList(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        match.addMatchList(new EpgMatchList());
        store.saveEpgAutoList(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=03&index=" + match_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] deleteMatchList(HTTPurl urlData) throws Exception {
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        match.getMatchLists().remove(list_id);
        store.saveEpgAutoList(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "?action=03&index=" + match_id + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] editMatchList(HTTPurl urlData) throws Exception {
        GuideStore guide = GuideStore.getInstance();
        int match_id = Integer.parseInt(urlData.getParameter("match_id"));
        int list_id = Integer.parseInt(urlData.getParameter("list_id"));
        EpgMatch match = store.getEpgMatchList().get(match_id);
        EpgMatchList matchList = match.getMatchLists().get(list_id);
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "epg-matchListEdit.html");
        template.replaceAll("$title", "Edit Epg Auto-Add Match List");
        template.replaceAll("$match_id$", new Integer(match_id).toString());
        template.replaceAll("$list_id$", new Integer(list_id).toString());
        Vector<EpgMatchListItem> items = matchList.getMatchList();
        StringBuffer buff = new StringBuffer();
        for (int x = 0; x < items.size(); x++) {
            EpgMatchListItem item = (EpgMatchListItem) items.get(x);
            buff.append("<tr>");
            if (item.getType() == EpgMatchListItem.TYPE_TEXT) {
                String searchText = HTMLEncoder.encode(item.getTextSearch());
                buff.append("<td>TEXT : (" + searchText + ") in the ");
                if (item.getField() == EpgMatchListItem.FIELD_TITLE) {
                    buff.append("title");
                } else if (item.getField() == EpgMatchListItem.FIELD_DESCRIPTION) {
                    buff.append("description");
                } else if (item.getField() == EpgMatchListItem.FIELD_CHANNEL) {
                    buff.append("channel");
                } else if (item.getField() == EpgMatchListItem.FIELD_CATEGORY) {
                    buff.append("category");
                }
                if (item.getExists()) {
                    buff.append(" must exist");
                } else {
                    buff.append(" must NOT exist");
                }
                if (item.getFlags() == EpgMatchListItem.FLAG_NONE) buff.append(" (Case)"); else if (item.getFlags() == EpgMatchListItem.FLAG_CASEINSENSATIVE) buff.append(" (No Case)"); else if (item.getFlags() == EpgMatchListItem.FLAG_REGEX) buff.append(" (RegEx)");
                buff.append("</td>");
            } else if (item.getType() == EpgMatchListItem.TYPE_SPAN) {
                buff.append("<td>SPAN : From <b>" + store.intToStr(item.getSpanFromHour()) + ":" + store.intToStr(item.getSpanFromMin()));
                buff.append("</b> to <b>" + store.intToStr(item.getSpanToHour()) + ":" + store.intToStr(item.getSpanToMin()) + "</b>");
                buff.append("</td>");
            } else if (item.getType() == EpgMatchListItem.TYPE_DAYS) {
                buff.append("<td>DAYS : ");
                if (item.getDayValue(EpgMatchListItem.DAYS_SUN)) buff.append(" Sun ");
                if (item.getDayValue(EpgMatchListItem.DAYS_MON)) buff.append(" Mon ");
                if (item.getDayValue(EpgMatchListItem.DAYS_TUE)) buff.append(" Tue ");
                if (item.getDayValue(EpgMatchListItem.DAYS_WED)) buff.append(" Wed ");
                if (item.getDayValue(EpgMatchListItem.DAYS_THUR)) buff.append(" Thur ");
                if (item.getDayValue(EpgMatchListItem.DAYS_FRI)) buff.append(" Fri ");
                if (item.getDayValue(EpgMatchListItem.DAYS_SAT)) buff.append(" Sat ");
                buff.append("</td>");
            } else if (item.getType() == EpgMatchListItem.TYPE_FLAG) {
                buff.append("<td>FLAG : ");
                int flagID = item.getFlagID();
                if (flagID == EpgMatchListItem.ITEM_FLAG_REPEAT) buff.append(" Repeat "); else if (flagID == EpgMatchListItem.ITEM_FLAG_LIVE) buff.append(" Live "); else if (flagID == EpgMatchListItem.ITEM_FLAG_HD) buff.append(" High Definition "); else if (flagID == EpgMatchListItem.ITEM_FLAG_WS) buff.append(" Wide Screen "); else if (flagID == EpgMatchListItem.ITEM_FLAG_CC) buff.append(" Closed Captions "); else if (flagID == EpgMatchListItem.ITEM_FLAG_PREMIERE) buff.append(" Premiere ");
                if (item.getFlagValue() == true) buff.append("= TRUE"); else buff.append("= FALSE");
                buff.append("</td>");
            } else {
                buff.append("<td>Unknown Type</td>");
            }
            buff.append("<td align='left'>");
            buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=13&match_id=" + match_id + "&list_id=" + list_id + "&item_id=" + x + "' onClick='return confirmAction(\"Delete\");'><img src='/images/delete.png' border='0' alt='' title='Delete' width='24' height='24'></a> ");
            if (item.getType() == EpgMatchListItem.TYPE_TEXT) {
                buff.append("<a href='#' onclick='editTextType(" + match_id + ", " + list_id + ", " + x + ");'>" + "<img src='/images/edit.png' border='0' alt='' title='Edit' width='24' height='24'></a>");
            }
            buff.append("</td>");
            buff.append("</tr>");
        }
        if (items.size() == 0) {
            buff.append("<tr><td colspan='2'>No Match Items</td></tr>");
        }
        template.replaceAll("$matchItems", buff.toString());
        if ("1".equals(urlData.getParameter("show"))) {
            buff = new StringBuffer();
            HashMap<String, Vector<GuideItem>> results = new HashMap<String, Vector<GuideItem>>();
            int num = guide.searchEPG(matchList, results);
            Calendar startTime = Calendar.getInstance();
            buff.append("<table class='epgSearchResults'>\n");
            String[] keys = (String[]) results.keySet().toArray(new String[0]);
            for (int y = 0; y < keys.length; y++) {
                Vector<GuideItem> result = results.get(keys[y]);
                if (result.size() > 0) {
                    buff.append("<tr><td colspan='4'>\n");
                    buff.append("<span class='areaTitle'>" + keys[y] + "</span><br>\n");
                    buff.append("</td></tr\n");
                    for (int x = 0; x < result.size(); x++) {
                        GuideItem item = (GuideItem) result.get(x);
                        startTime.setTime(item.getStart());
                        int hour = startTime.get(Calendar.HOUR);
                        if (hour == 0) hour = 12;
                        String startString = store.intToStr(hour) + ":" + store.intToStr(startTime.get(Calendar.MINUTE)) + " " + store.ampm.get(new Integer(startTime.get(Calendar.AM_PM)));
                        String dateString = store.dayName.get(new Integer(startTime.get(Calendar.DAY_OF_WEEK))) + ", " + startTime.get(Calendar.DATE) + " " + store.monthNameShort.get(new Integer(startTime.get(Calendar.MONTH)));
                        buff.append("<tr>\n");
                        buff.append("<td class='epgSearchResults'>" + item.getName() + "</td>");
                        buff.append("<td class='epgSearchResults'>" + dateString + " at " + startString + "</td>");
                        buff.append("<td class='epgSearchResults'>" + item.getDuration() + "</td>");
                        buff.append("<td class='epgSearchResults'>");
                        String infoUrl = "/servlet/EpgDataRes?action=06&id=" + item.toString() + "&channel=" + URLEncoder.encode(keys[y], "UTF-8");
                        buff.append("<span class='programName' onClick=\"openDetails('" + infoUrl + "')\">");
                        buff.append("Details");
                        buff.append("</span> | \n");
                        String epgUrl = "/servlet/EpgDataRes?action=12" + "&year=" + startTime.get(Calendar.YEAR) + "&month=" + (startTime.get(Calendar.MONTH) + 1) + "&day=" + startTime.get(Calendar.DATE) + "&scrollto=" + startTime.get(Calendar.HOUR_OF_DAY);
                        buff.append("<a class='infoNav' href='" + epgUrl + "'>EPG</a>");
                        buff.append("</td>");
                        buff.append("</tr>\n");
                    }
                }
            }
            if (num == 0) {
                buff.append("<tr><td colspan='4'>No Matches</td></tr>\n");
            }
            buff.append("</table><p>\n");
            template.replaceAll("$searchResult", buff.toString());
        } else {
            template.replaceAll("$searchResult", "");
        }
        return template.getPageBytes();
    }

    private String getTaskList(String current) {
        StringBuffer buff = new StringBuffer(1024);
        String selectedTask = store.getProperty("tasks.deftask");
        if (current != null) {
            selectedTask = current;
        }
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String[] keys = (String[]) tasks.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        buff.append("<select name='task'>\n");
        if (selectedTask.length() == 0) buff.append("<option value='none' selected>none</option>\n"); else buff.append("<option value='none'>none</option>\n");
        for (int x = 0; x < keys.length; x++) {
            if (keys[x].equals(selectedTask)) buff.append("<option value='" + keys[x] + "' selected>" + keys[x] + "</option>\n"); else buff.append("<option value='" + keys[x] + "'>" + keys[x] + "</option>\n");
        }
        buff.append("</select>\n");
        return buff.toString();
    }

    private String getCaptureTypes(int current) {
        StringBuffer buff = new StringBuffer(1024);
        int capType = -1;
        if (current > -1) capType = current;
        buff.append("<select name='captype'>\n");
        if (capType == -1) buff.append("<option value='-1' selected>AutoSelect</option>\n"); else buff.append("<option value='-1'>AutoSelect</option>\n");
        Vector<CaptureCapability> capabilities = CaptureCapabilities.getInstance().getCapabilities();
        for (int x = 0; x < capabilities.size(); x++) {
            CaptureCapability capability = capabilities.get(x);
            buff.append("<option value='" + capability.getTypeID() + "'");
            if (capType == capability.getTypeID()) buff.append(" selected");
            buff.append(">" + capability.getName() + "</option>\n");
        }
        buff.append("</select>\n");
        return buff.toString();
    }
}
