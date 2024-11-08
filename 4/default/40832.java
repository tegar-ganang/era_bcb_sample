import java.util.*;
import java.text.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

class GuideStore {

    private HashMap<String, HashMap<String, GuideItem>> progList = new HashMap<String, HashMap<String, GuideItem>>();

    private Date maxEntry = new Date();

    private Date minEntry = new Date();

    private Vector<String[]> channelMap = new Vector<String[]>();

    private Vector<String> categoryList = new Vector<String>();

    private Vector<AddConflictDetails> conflictList = new Vector<AddConflictDetails>();

    private static GuideStore instance = null;

    private GuideStore() {
        loadChannelMap();
        loadEpg();
        loadCategories();
    }

    public static GuideStore getInstance() {
        synchronized (DataStore.class) {
            if (instance == null) {
                instance = new GuideStore();
                return instance;
            } else {
                return instance;
            }
        }
    }

    public String toString() {
        return "Programs Loaded:" + progList.size() + " Channel Maps Loads:" + channelMap.size() + " Categories Loaded:" + categoryList.size();
    }

    public Date getMaxEntry() {
        return maxEntry;
    }

    public Date getMinEntry() {
        return minEntry;
    }

    public Vector<String> getCategoryMap() {
        return categoryList;
    }

    public void setCategoryMap(Vector<String> cats) {
        categoryList = cats;
    }

    public int numberOfCategories() {
        return categoryList.size();
    }

    public HashMap<String, HashMap<String, GuideItem>> getProgramList() {
        return progList;
    }

    public void setProgramList(HashMap<String, HashMap<String, GuideItem>> progs) {
        progList = progs;
    }

    public Date getEPGmaxDate() {
        return getMaxEntry();
    }

    public Date getEPGminDate() {
        return getMinEntry();
    }

    public Vector<AddConflictDetails> getAutoAddConflicts() {
        return getConflictList();
    }

    public Vector<String[]> getEPGlinks(Calendar hiLight) {
        DataStore store = DataStore.getInstance();
        Date end = getMaxEntry();
        Date start = getMinEntry();
        if (start == null || end == null) return new Vector<String[]>();
        Vector<String[]> data = new Vector<String[]>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        Calendar now = Calendar.getInstance();
        while (cal.getTime().getTime() < end.getTime()) {
            String[] link = new String[2];
            link[0] = "year=" + cal.get(Calendar.YEAR) + "&month=" + (cal.get(Calendar.MONTH) + 1) + "&day=" + cal.get(Calendar.DATE);
            String dayName = (String) store.dayName.get(new Integer(cal.get(Calendar.DAY_OF_WEEK)));
            if (now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && now.get(Calendar.DATE) == cal.get(Calendar.DATE)) {
                dayName = "Today";
            }
            if (hiLight != null && hiLight.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && hiLight.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && hiLight.get(Calendar.DATE) == cal.get(Calendar.DATE)) {
                link[1] = "(" + dayName + ")";
            } else {
                link[1] = dayName;
            }
            data.add(link);
            cal.add(Calendar.DATE, 1);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void loadCategories() {
        DataStore store = DataStore.getInstance();
        try {
            FileInputStream fis = new FileInputStream(store.getProperty("path.data") + File.separator + "Categories.sof");
            ObjectInputStream ois = new ObjectInputStream(fis);
            Vector<String> cats = (Vector<String>) ois.readObject();
            ois.close();
            fis.close();
            setCategoryMap(cats);
            System.out.println("Categories.sof loaded (" + cats.size() + ")");
        } catch (Exception e) {
            setCategoryMap(new Vector<String>());
            System.out.println("Error loading Categories.sof, starting with blank category list");
        }
    }

    public void saveCategories() {
        DataStore store = DataStore.getInstance();
        try {
            FileOutputStream fos = new FileOutputStream(store.getProperty("path.data") + File.separator + "Categories.sof");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(getCategoryMap());
            oos.close();
            fos.close();
            System.out.println("Categories.sof saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runPostLoadActions(StringBuffer buff, int format) {
        if (format == 1) buff.append("Running post load action<br>\n"); else buff.append("Running post load action\n");
        DataStore store = DataStore.getInstance();
        boolean mergeSameName = false;
        try {
            mergeSameName = "1".equals(store.getProperty("guide.action.name").trim());
        } catch (Exception e) {
        }
        if (mergeSameName) {
            if (format == 1) buff.append("Merging Programs with the same name<br>\n"); else buff.append("Merging Programs with the same name\n");
            String[] epgChannels = (String[]) progList.keySet().toArray(new String[0]);
            for (int x = 0; x < epgChannels.length; x++) {
                HashMap<String, GuideItem> items = (HashMap<String, GuideItem>) progList.get(epgChannels[x]);
                String[] itemKeys = (String[]) items.keySet().toArray(new String[0]);
                Arrays.sort(itemKeys);
                GuideItem prev = null;
                for (int y = 0; y < itemKeys.length; y++) {
                    GuideItem item = (GuideItem) items.get(itemKeys[y]);
                    if (item != null && prev != null && item.getName().equalsIgnoreCase(prev.getName())) {
                        items.remove(prev.toString());
                        items.remove(item.toString());
                        GuideItem newItem = new GuideItem();
                        newItem.setStart(prev.getStart());
                        newItem.setStop(item.getStop());
                        newItem.setName(prev.getName());
                        newItem.setSubName(prev.getSubName() + "-" + item.getSubName());
                        newItem.setDescription(prev.getDescription() + "\n<br>\n" + item.getDescription());
                        newItem.setCategory(prev.getCategory());
                        newItem.setLanguage(prev.getLanguage());
                        newItem.setURL(prev.getURL());
                        items.put(newItem.toString(), newItem);
                        if (format == 1) buff.append("Merging same name : " + newItem.getStart() + " (" + newItem.getDuration() + ") - " + newItem.getName() + "<br>\n"); else buff.append("Merging same name : " + newItem.getStart() + " (" + newItem.getDuration() + ") - " + newItem.getName() + "\n");
                        prev = newItem;
                    } else prev = item;
                }
            }
        }
        if (format == 1) buff.append("Post load actions finished<br>\n"); else buff.append("Post load actions finished\n");
    }

    public void loadXMLTV(StringBuffer buff, int format) {
        DataStore store = DataStore.getInstance();
        try {
            int type = 0;
            try {
                type = Integer.parseInt(store.getProperty("guide.source.type"));
            } catch (Exception e) {
            }
            String location = "";
            if (type == 0) location = store.getProperty("guide.source.file"); else if (type == 1) location = store.getProperty("guide.source.http"); else location = "error, unknown type";
            if (format == 1) buff.append("About to load XMLTV Data from:<br>" + location + "<br><br>\n");
            GuideDataLoader loader = new GuideDataLoader(type);
            Vector dataFiles = null;
            if (type == 0) dataFiles = loader.getDataFromFiles(location, buff, format); else if (type == 1) dataFiles = loader.getDataFromURL(location); else {
                throw new Exception("Unknown type:" + type);
            }
            if (dataFiles.size() == 0) {
                if (format == 1) buff.append("No Data Found in source, exiting reload.<br><br>\n"); else buff.append("No Data Found in source, exiting reload.\n\n");
                return;
            }
            HashMap<String, HashMap<String, GuideItem>> ignoreList = clearGuideData();
            for (int x = 0; x < dataFiles.size(); x++) {
                byte[] fileBytes = (byte[]) dataFiles.get(x);
                loadXMLdata(fileBytes);
            }
            int ignoreCount = setIgnored(ignoreList);
            buff.append("<br>Ignored Programs : " + ignoreCount + "<br>\n");
        } catch (Exception e) {
            System.out.println("Error Loading EPG DATA!");
            if (format == 1) buff.append("<br><strong>");
            buff.append("There was an error loading the XMLTV Data!");
            if (format == 1) buff.append("</strong><br>");
            buff.append("\n");
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            PrintWriter err = new PrintWriter(ba);
            e.printStackTrace(err);
            err.flush();
            if (format == 1) buff.append("<pre>");
            buff.append(ba.toString());
            if (format == 1) buff.append("</pre>");
            if (format == 1) buff.append("<br><br>");
            buff.append("\n");
            return;
        }
        StringBuffer actionBuff = new StringBuffer();
        runPostLoadActions(actionBuff, format);
        detectDateRange();
        if (getMinEntry() != null && getMaxEntry() != null) {
            if (format == 1) {
                buff.append("<br>Earliest Entry : " + getMinEntry().toString() + "<br>\n");
                buff.append("Latest Entry : " + getMaxEntry().toString() + "<br><br>\n");
            } else {
                buff.append("\nEarliest Entry : " + getMinEntry().toString() + "\n");
                buff.append("Latest Entry : " + getMaxEntry().toString() + "\n\n");
            }
        }
        Vector cats = getCategoryMap();
        if (cats.size() > 0) {
            if (format == 1) buff.append("Number of Categories Found : " + String.valueOf(cats.size()) + "<br><br>\n"); else buff.append("Number of Categories Found : " + String.valueOf(cats.size()) + "\n\n");
        }
        HashMap<String, HashMap<String, GuideItem>> progList = getProgramList();
        String[] channels = (String[]) progList.keySet().toArray(new String[0]);
        Arrays.sort(channels);
        if (format == 1) {
            buff.append("Current EPG Data Set<br>\n");
            for (int x = 0; x < channels.length; x++) {
                HashMap progs = (HashMap) progList.get(channels[x]);
                buff.append(channels[x] + " : " + progs.size() + "<br>\n");
            }
            buff.append("<br>Saving loaded EPG data<br><br>\n");
        } else {
            buff.append("Current EPG Data Set\n");
            for (int x = 0; x < channels.length; x++) {
                HashMap progs = (HashMap) progList.get(channels[x]);
                buff.append(channels[x] + " : " + progs.size() + "\n");
            }
            buff.append("\nSaving loaded EPG data\n\n");
        }
        buff.append(actionBuff);
        saveEpg();
        saveCategories();
        detectDateRange();
    }

    @SuppressWarnings("unchecked")
    public void loadEpg() {
        DataStore store = DataStore.getInstance();
        String loadFrom = store.getProperty("path.data") + File.separator + "Epg.sof";
        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<String, HashMap<String, GuideItem>> progList = (HashMap<String, HashMap<String, GuideItem>>) ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Epg.sof loaded.");
            setProgramList(progList);
            detectDateRange();
        } catch (Exception e) {
            System.out.println("Error loading Epg.sof, starting with empty EPG data set.");
        }
    }

    public void saveEpg() {
        DataStore store = DataStore.getInstance();
        String saveTo = store.getProperty("path.data") + File.separator + "Epg.sof";
        try {
            FileOutputStream fos = new FileOutputStream(saveTo);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(getProgramList());
            oos.close();
            fos.close();
            System.out.println("Epg.sof saved.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error saving Epg.sof");
        }
    }

    @SuppressWarnings("unchecked")
    public void importChannelMap(byte[] mapData) throws Exception {
        ByteArrayInputStream mapBytes = new ByteArrayInputStream(mapData);
        ObjectInputStream ois = new ObjectInputStream(mapBytes);
        Vector<String[]> channelMap = (Vector<String[]>) ois.readObject();
        ois.close();
        System.out.println("Channel Map imported");
        setChannelMap(channelMap);
        saveChannelMap(null);
    }

    @SuppressWarnings("unchecked")
    public void loadChannelMap() {
        DataStore store = DataStore.getInstance();
        String loadFrom = store.getProperty("path.data") + File.separator + "ChannelMap.sof";
        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Vector<String[]> channelMap = (Vector<String[]>) ois.readObject();
            ois.close();
            fis.close();
            System.out.println("ChannelMap.sof loaded.");
            setChannelMap(channelMap);
        } catch (Exception e) {
            System.out.println("Error loading ChannelMap.sof, starting with no channel mapping.");
        }
    }

    public void saveChannelMap(ByteArrayOutputStream chanMapBytes) {
        try {
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            if (chanMapBytes == null) {
                DataStore store = DataStore.getInstance();
                String saveTo = store.getProperty("path.data") + File.separator + "ChannelMap.sof";
                fos = new FileOutputStream(saveTo);
                oos = new ObjectOutputStream(fos);
            } else {
                oos = new ObjectOutputStream(chanMapBytes);
            }
            oos.writeObject(getChannelMap());
            oos.close();
            if (fos == null) fos.close();
            System.out.println("ChannelMap.sof saved.");
        } catch (Exception e) {
            System.out.println("Problem saving ChannelMap.sof");
            e.printStackTrace();
        }
    }

    public String getWsChannelFromMap(String epgChannel) {
        if (epgChannel == null) return null;
        for (int x = 0; x < channelMap.size(); x++) {
            String[] map = (String[]) channelMap.get(x);
            if (map[1].equals(epgChannel)) return map[0];
        }
        return null;
    }

    public String getEpgChannelFromMap(String wsChannel) {
        if (wsChannel == null) return null;
        for (int x = 0; x < channelMap.size(); x++) {
            String[] map = (String[]) channelMap.get(x);
            if (map[0].equals(wsChannel)) return map[1];
        }
        return null;
    }

    public String[] getCategoryStrings() {
        return (String[]) categoryList.toArray(new String[0]);
    }

    public void detectDateRange() {
        maxEntry = null;
        minEntry = null;
        String[] channels = (String[]) progList.keySet().toArray(new String[0]);
        for (int x = 0; x < channels.length; x++) {
            HashMap<String, GuideItem> progs = (HashMap<String, GuideItem>) progList.get(channels[x]);
            if (progs != null) {
                GuideItem[] items = (GuideItem[]) progs.values().toArray(new GuideItem[0]);
                Arrays.sort(items);
                if (items.length > 0) {
                    if (minEntry == null || items[0].getStart().getTime() < minEntry.getTime()) minEntry = items[0].getStart();
                    if (maxEntry == null || items[items.length - 1].getStart().getTime() > maxEntry.getTime()) maxEntry = items[items.length - 1].getStart();
                }
            }
        }
    }

    public int loadXMLdata(byte[] xmlFile) throws Exception {
        int found = 0;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream reader = new ByteArrayInputStream(xmlFile);
        Document doc = docBuilder.parse(reader);
        NodeList root = null;
        root = doc.getElementsByTagName("tv");
        if (root != null) {
            Node firstNode = root.item(0);
            if (firstNode == null) {
                System.out.println("ERROR precessing XML data, first node of <tv> not found.");
                return found;
            }
            root = firstNode.getChildNodes();
            if (root == null) {
                System.out.println("ERROR precessing XML data, no children of first node.");
                return found;
            }
            HashMap channels = loadChannels(root);
            found = recurseXMLData(root, channels);
        }
        return found;
    }

    private HashMap<String, String> loadChannels(NodeList root) {
        HashMap<String, String> channels = new HashMap<String, String>();
        Node thisNode = null;
        for (int x = 0; x < root.getLength(); x++) {
            thisNode = root.item(x);
            if (thisNode == null) break;
            if (thisNode.getNodeName().equals("channel")) parseChannel(thisNode, channels);
        }
        return channels;
    }

    public String getNodeString(Node prog, String nodename, String defaultValue) {
        String out = defaultValue;
        try {
            Node tmp = getChildByType(prog, nodename);
            if (tmp != null && tmp.getFirstChild() != null) {
                out = tmp.getFirstChild().getNodeValue();
            }
            tmp = null;
        } catch (Exception e) {
        }
        return out;
    }

    public Vector<String> getNodeStrings(Node node, String nodename) {
        Vector<String> values = new Vector<String>();
        try {
            Vector<Node> nodeList = getChildrenByType(node, nodename);
            for (int x = 0; x < nodeList.size(); x++) {
                Node tmp = nodeList.get(x);
                if (tmp.getFirstChild() != null) {
                    values.add(tmp.getFirstChild().getNodeValue());
                }
                tmp = null;
            }
        } catch (Exception e) {
        }
        return values;
    }

    public boolean getNodeExists(Node prog, String name) {
        Node tmp = null;
        try {
            tmp = getChildByType(prog, name);
            if (tmp != null) return true;
        } catch (Exception e) {
        }
        return false;
    }

    public HashMap<String, HashMap<String, GuideItem>> clearGuideData() {
        HashMap<String, HashMap<String, GuideItem>> ignoreList = new HashMap<String, HashMap<String, GuideItem>>();
        String[] channels = (String[]) progList.keySet().toArray(new String[0]);
        for (int x = 0; x < channels.length; x++) {
            HashMap<String, GuideItem> guideItems = progList.get(channels[x]);
            String[] ids = (String[]) guideItems.keySet().toArray(new String[0]);
            for (int y = 0; y < ids.length; y++) {
                GuideItem item = guideItems.get(ids[y]);
                if (item.getIgnored() == true) {
                    HashMap<String, GuideItem> guideItems2 = (HashMap<String, GuideItem>) ignoreList.get(channels[x]);
                    if (guideItems2 == null) {
                        guideItems2 = new HashMap<String, GuideItem>();
                        ignoreList.put(channels[x], guideItems2);
                    }
                    guideItems2.put(item.toString(), item);
                }
            }
        }
        progList = new HashMap<String, HashMap<String, GuideItem>>();
        categoryList = new Vector<String>();
        return ignoreList;
    }

    private int setIgnored(HashMap<String, HashMap<String, GuideItem>> ignoredList) {
        int count = 0;
        String[] channels = (String[]) ignoredList.keySet().toArray(new String[0]);
        for (int x = 0; x < channels.length; x++) {
            HashMap<String, GuideItem> guideItems = ignoredList.get(channels[x]);
            String[] ids = (String[]) guideItems.keySet().toArray(new String[0]);
            for (int y = 0; y < ids.length; y++) {
                GuideItem item = guideItems.get(ids[y]);
                HashMap<String, GuideItem> guideItems2 = progList.get(channels[x]);
                if (guideItems2 != null) {
                    GuideItem[] items2 = (GuideItem[]) guideItems2.values().toArray(new GuideItem[0]);
                    for (int q = 0; q < items2.length; q++) {
                        if (items2[q].matches(item)) {
                            count++;
                            System.out.println("Setting EPG item to ignored : " + items2[q]);
                            items2[q].setIgnored(true);
                        }
                    }
                }
            }
        }
        return count;
    }

    public void addProgram(GuideItem item, String channel) {
        HashMap<String, GuideItem> programs = (HashMap<String, GuideItem>) progList.get(channel);
        if (programs == null) {
            programs = new HashMap<String, GuideItem>();
            progList.put(channel, programs);
        }
        if (item.getCategory().size() > 0) {
            for (int index = 0; index < item.getCategory().size(); index++) {
                String itemCat = item.getCategory().get(index);
                if (categoryList.contains(itemCat) == false) {
                    categoryList.add(itemCat);
                }
            }
        }
        programs.put(item.toString(), item);
    }

    public GuideItem[] getProgramsForChannel(String channel) {
        HashMap fullList = (HashMap) progList.get(channel);
        if (fullList == null) {
            return new GuideItem[0];
        }
        Iterator it = fullList.values().iterator();
        Vector<GuideItem> subList = new Vector<GuideItem>();
        GuideItem item = null;
        while (it.hasNext()) {
            item = (GuideItem) it.next();
            subList.add(item);
        }
        GuideItem[] items = (GuideItem[]) subList.toArray(new GuideItem[0]);
        Arrays.sort(items);
        return items;
    }

    public GuideItem[] getPrograms(Date start, Date end, String channel) {
        HashMap fullList = (HashMap) progList.get(channel);
        if (fullList == null) {
            return new GuideItem[0];
        }
        Iterator it = fullList.values().iterator();
        Vector<GuideItem> subList = new Vector<GuideItem>();
        GuideItem item = null;
        while (it.hasNext()) {
            item = (GuideItem) it.next();
            if (start.getTime() < item.getStart().getTime() && end.getTime() > item.getStart().getTime()) {
                subList.add(item);
            }
        }
        GuideItem[] items = (GuideItem[]) subList.toArray(new GuideItem[0]);
        Arrays.sort(items);
        return items;
    }

    public GuideItem[] getProgramsInc(Date start, Date end, String channel) {
        HashMap fullList = (HashMap) progList.get(channel);
        if (fullList == null) {
            return new GuideItem[0];
        }
        Iterator it = fullList.values().iterator();
        Vector<GuideItem> subList = new Vector<GuideItem>();
        GuideItem item = null;
        while (it.hasNext()) {
            item = (GuideItem) it.next();
            if (item.getStop().getTime() > (start.getTime() + 5000) && item.getStart().getTime() < end.getTime()) {
                subList.add(item);
            }
        }
        GuideItem[] items = (GuideItem[]) subList.toArray(new GuideItem[0]);
        Arrays.sort(items);
        return items;
    }

    public GuideItem getProgram(String channel, String id) {
        HashMap fullList = (HashMap) progList.get(channel);
        if (fullList == null) return null;
        return (GuideItem) fullList.get(id);
    }

    public int recurseXMLData(NodeList nl, HashMap channels) {
        int found = 0;
        Node thisNode = null;
        for (int x = 0; x < nl.getLength(); x++) {
            thisNode = nl.item(x);
            if (thisNode == null) break;
            if (thisNode.getNodeName().equals("programme")) {
                if (parseProgram(thisNode, channels)) found++;
            }
        }
        return found;
    }

    public void moveChannel(int id, boolean dir) {
        int dest = 0;
        if (dir) dest = id + 1; else dest = id - 1;
        if (dest >= 0 && dest < channelMap.size()) {
            String[] obj = (String[]) channelMap.remove(id);
            if (dir) channelMap.add(id + 1, obj); else channelMap.add(id - 1, obj);
            try {
                saveChannelMap(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String[] getChannelList() {
        return (String[]) progList.keySet().toArray(new String[0]);
    }

    public Vector getChannelMap() {
        return channelMap;
    }

    public void setChannelMap(Vector<String[]> chanMap) {
        channelMap = chanMap;
    }

    public void addChannelToMap(String channel, String egpChannel) {
        String[] map = new String[2];
        map[0] = channel;
        map[1] = egpChannel;
        channelMap.add(map);
    }

    public Date parseDate(String newDate) throws NumberFormatException {
        String dateFormat = "";
        if (newDate.indexOf(" +") > -1 || newDate.indexOf(" -") > -1) {
            dateFormat = "yyyyMMddHHmmss Z";
        } else {
            dateFormat = "yyyyMMddHHmmss";
        }
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);
        Date parsedDate = null;
        try {
            parsedDate = df.parse(newDate);
        } catch (Exception e) {
            e.printStackTrace();
            parsedDate = new Date(0);
        }
        Calendar parsedCal = Calendar.getInstance();
        parsedCal.setTime(parsedDate);
        parsedCal.set(Calendar.MILLISECOND, 0);
        parsedCal.add(Calendar.SECOND, 30);
        parsedCal.set(Calendar.SECOND, 0);
        return parsedCal.getTime();
    }

    private void parseChannel(Node chan, HashMap<String, String> channels) {
        NamedNodeMap attMap = chan.getAttributes();
        Node idNode = attMap.getNamedItem("id");
        String id = idNode.getNodeValue();
        Node nameNode = getChildByType(chan, "display-name");
        String name = nameNode.getFirstChild().getNodeValue();
        if (id != null && id.length() > 0 && name != null && name.length() > 0) {
            channels.put(id, name);
        }
    }

    public boolean parseProgram(Node prog, HashMap channels) {
        GuideItem item = new GuideItem();
        String temp01 = "";
        String temp02 = "";
        try {
            NamedNodeMap attMap = prog.getAttributes();
            Node start_time = attMap.getNamedItem("start");
            temp01 = start_time.getNodeValue();
            Date start = parseDate(start_time.getNodeValue());
            item.setStart(start);
            Node stop_time = attMap.getNamedItem("stop");
            temp02 = stop_time.getNodeValue();
            Date stop = parseDate(stop_time.getNodeValue());
            long duration = (stop.getTime() - start.getTime());
            duration = duration / (1000 * 60);
            if (duration < 0) duration *= -1;
            item.setDuration((int) duration);
            Node chan = attMap.getNamedItem("channel");
            Node title = getChildByType(prog, "title");
            item.setName(title.getFirstChild().getNodeValue());
            Node subTitle = getChildByType(prog, "sub-title");
            if (subTitle != null && subTitle.getFirstChild() != null) item.setSubName(subTitle.getFirstChild().getNodeValue());
            Node description = getChildByType(prog, "desc");
            if (description != null && description.getFirstChild() != null) item.setDescription(description.getFirstChild().getNodeValue());
            Node url = getChildByType(prog, "url");
            if (url != null && url.getFirstChild() != null) item.setURL(url.getFirstChild().getNodeValue());
            if (getNodeExists(prog, "rating")) {
                Node tmp = getChildByType(prog, "rating");
                Node value = getChildByType(tmp, "value");
                Node ratingNode = value.getFirstChild();
                if (ratingNode != null) item.setRatings(ratingNode.getNodeValue());
            }
            item.setLanguage(getNodeString(prog, "language", item.getLanguage()));
            item.setCategory(getNodeStrings(prog, "category"));
            if (getNodeExists(prog, "credits")) {
                Node tmp = getChildByType(prog, "credits");
                item.setActors(getNodeStrings(tmp, "actor"));
                item.setDirectors(getNodeStrings(tmp, "director"));
            }
            item.setLastChance(getNodeExists(prog, "last-chance"));
            item.setRepeat(getNodeExists(prog, "previously-shown"));
            item.setPremiere(getNodeExists(prog, "premiere"));
            item.setCaptions(getNodeExists(prog, "subtitles"));
            if (getNodeExists(prog, "audio")) {
                Node tmp = getChildByType(prog, "audio");
                String str = getNodeString(tmp, "stereo", "").toLowerCase();
                if (str.startsWith("surround")) item.setSurround(true);
                if (str.startsWith("ac3")) item.setAC3(true);
                tmp = null;
                str = "";
            }
            if (getNodeExists(prog, "video")) {
                Node tmp = getChildByType(prog, "video");
                String str = getNodeString(tmp, "aspect", "");
                if (str.toLowerCase().startsWith("16:9")) item.setWidescreen(true);
                str = getNodeString(tmp, "quality", "");
                if (str.toLowerCase().startsWith("hd")) item.setHighDef(true);
                tmp = null;
                str = "";
            }
            String channel = (String) channels.get(chan.getNodeValue());
            if (channel == null) channel = chan.getNodeValue();
            channel = channel.replaceAll("'", "`");
            channel = channel.replaceAll("\"", "`");
            channel = channel.replaceAll("<", "(");
            channel = channel.replaceAll(">", ")");
            addProgram(item, channel);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Temp 01:" + temp01);
            System.out.println("Temp 02:" + temp02);
            System.out.println("Available Item Information:");
            System.out.println("Name:" + item.getName());
            System.out.println("Sub Name:" + item.getSubName());
            System.out.println("StartTime:" + item.getStart());
            System.out.println("Duration:" + item.getDuration());
            System.out.println("Description:" + item.getDescription());
            return false;
        }
        return true;
    }

    public Node getChildByType(Node node, String name) {
        if (!node.hasChildNodes()) return null;
        Node found = null;
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            found = nl.item(x);
            if (found.getNodeName().equals(name)) return found;
        }
        return null;
    }

    public Vector<Node> getChildrenByType(Node node, String name) {
        Vector<Node> children = new Vector<Node>();
        if (!node.hasChildNodes()) return children;
        Node found = null;
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            found = nl.item(x);
            if (found.getNodeName().equals(name)) {
                children.add(found);
            }
        }
        return children;
    }

    String[] getFirstLetters() {
        Vector<String> firsts = new Vector<String>();
        for (int x = 0; x < channelMap.size(); x++) {
            String[] map = (String[]) channelMap.get(x);
            HashMap<String, GuideItem> progs = (HashMap<String, GuideItem>) progList.get(map[1]);
            if (progs != null) {
                GuideItem[] items = (GuideItem[]) progs.values().toArray(new GuideItem[0]);
                for (int y = 0; y < items.length; y++) {
                    if (items[y].getName() != null && items[y].getName().length() > 1) {
                        String firstLetter = items[y].getName().substring(0, 1).toUpperCase();
                        if (!firsts.contains(firstLetter)) firsts.add(firstLetter);
                    }
                }
            }
        }
        String[] result = (String[]) firsts.toArray(new String[0]);
        Arrays.sort(result);
        return result;
    }

    String[] getNamesStartingWith(String starting) {
        Vector<String> starts = new Vector<String>();
        for (int x = 0; x < channelMap.size(); x++) {
            String[] map = (String[]) channelMap.get(x);
            HashMap<String, GuideItem> progs = (HashMap<String, GuideItem>) progList.get(map[1]);
            if (progs != null) {
                GuideItem[] items = (GuideItem[]) progs.values().toArray(new GuideItem[0]);
                for (int y = 0; y < items.length; y++) {
                    if (items[y].getName() != null && items[y].getName().length() > 1) {
                        if (items[y].getName().toUpperCase().startsWith(starting.toUpperCase())) {
                            if (!starts.contains(items[y].getName())) {
                                starts.add(items[y].getName());
                            }
                        }
                    }
                }
            }
        }
        String[] result = (String[]) starts.toArray(new String[0]);
        Arrays.sort(result);
        return result;
    }

    GuideItem[] getItems(String name, String channel) {
        Vector<GuideItem> matchingItems = new Vector<GuideItem>();
        HashMap<String, GuideItem> progs = (HashMap<String, GuideItem>) progList.get(channel);
        if (progs != null) {
            GuideItem[] items = (GuideItem[]) progs.values().toArray(new GuideItem[0]);
            for (int y = 0; y < items.length; y++) {
                if (name.equals(items[y].getName())) {
                    matchingItems.add(items[y]);
                }
            }
        }
        GuideItem[] result = (GuideItem[]) matchingItems.toArray(new GuideItem[0]);
        Arrays.sort(result);
        return result;
    }

    public int searchEPG(EpgMatchList epgMatchList, HashMap<String, Vector<GuideItem>> result) {
        DataStore store = DataStore.getInstance();
        int num = 0;
        GuideItem[] progs = null;
        Vector channelMap = getChannelMap();
        if (channelMap == null || channelMap.size() == 0) {
            return 0;
        }
        Set wsChannels = store.getChannels().keySet();
        for (int x = 0; x < channelMap.size(); x++) {
            String[] map = (String[]) channelMap.get(x);
            if (wsChannels.contains(map[0])) {
                progs = getProgramsForChannel(map[1]);
                if (progs.length > 0) {
                    Vector<GuideItem> results = null;
                    if (result.containsKey(map[0])) {
                        results = (Vector<GuideItem>) result.get(map[0]);
                    } else {
                        results = new Vector<GuideItem>();
                        result.put(map[0], results);
                    }
                    for (int y = 0; y < progs.length; y++) {
                        if (epgMatchList.isMatch(progs[y], map[0])) {
                            if (!results.contains(progs[y])) {
                                results.add(progs[y]);
                                num++;
                            }
                        }
                    }
                }
            }
        }
        return num;
    }

    public int addEPGmatches(StringBuffer buff, int format) throws Exception {
        DataStore store = DataStore.getInstance();
        int num = 0;
        SimpleDateFormat df = new SimpleDateFormat("EE d HH:mm");
        int type = 0;
        try {
            type = Integer.parseInt(store.getProperty("Capture.deftype"));
        } catch (Exception e) {
        }
        conflictList.clear();
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        EpgMatch epgMatch = null;
        Calendar cal = Calendar.getInstance();
        Vector channelMap = getChannelMap();
        Set wsChannels = store.getChannels().keySet();
        if (channelMap == null || channelMap.size() == 0) {
            if (format == 1) buff.append("Channel map not set so could not run Auto-Add system.<br>\n"); else buff.append("Channel map not set so could not run Auto-Add system.\n");
            System.out.println("Channel map not set so could not run Auto-Add system.");
            return 0;
        }
        if (format == 1) buff.append("About to run Auto-Add system.<br><br>\n"); else buff.append("About to run Auto-Add system.\n\n");
        GuideItem[] progs = null;
        GuideItem guidItem = null;
        ScheduleItem schItem = null;
        int conflictNum = 0;
        Vector epgMatchList = store.getEpgMatchList();
        System.out.println("");
        for (int x = 0; x < epgMatchList.size(); x++) {
            epgMatch = (EpgMatch) epgMatchList.get(x);
            if (epgMatch.isEnabled()) {
                for (int z = 0; z < channelMap.size(); z++) {
                    String[] chanMap = (String[]) channelMap.get(z);
                    if (wsChannels.contains(chanMap[0])) {
                        progs = getProgramsForChannel(chanMap[1]);
                        for (int y = 0; y < progs.length; y++) {
                            guidItem = progs[y];
                            boolean foundMatch = false;
                            Vector matchNames = epgMatch.getMatchListNames();
                            HashMap matchLists = store.getMatchLists();
                            EpgMatchList matcher = null;
                            for (int nameIndex = 0; nameIndex < matchNames.size(); nameIndex++) {
                                String matchListName = (String) matchNames.get(nameIndex);
                                matcher = (EpgMatchList) matchLists.get(matchListName);
                                if (matcher != null) {
                                    if (matcher.isMatch(guidItem, chanMap[0])) {
                                        foundMatch = true;
                                        break;
                                    }
                                }
                            }
                            if (foundMatch && guidItem.getIgnored() == true) {
                                String itemAction = "IGNORED";
                                String itemResult = "Item Ignored";
                                String itemDetails = guidItem.getName() + " " + df.format(guidItem.getStart());
                                if (format == 1) {
                                    buff.append("<b><font color='#FF0000'>" + itemAction + "</font></b> - " + itemDetails + " (" + itemResult + ")<br>\n");
                                } else buff.append(itemAction + " - " + itemDetails + " (" + itemResult + ")\n");
                            } else if (foundMatch) {
                                schItem = new ScheduleItem(guidItem, chanMap[0], type, store.rand.nextLong(), epgMatch.getAutoDel());
                                String[] patterns = store.getNamePatterns();
                                schItem.setFilePattern(patterns[0]);
                                schItem.setKeepFor(epgMatch.getKeepFor());
                                schItem.setPostTask(epgMatch.getPostTask());
                                schItem.setFilePattern(epgMatch.GetFileNamePattern());
                                schItem.setCapType(epgMatch.getCaptureType());
                                schItem.setCapturePathIndex(epgMatch.getCapturePathIndex());
                                cal.setTime(schItem.getStart());
                                cal.add(Calendar.MINUTE, (epgMatch.getStartBuffer() * -1));
                                schItem.setDuration(guidItem.getDuration() + epgMatch.getStartBuffer() + epgMatch.getEndBuffer());
                                schItem.setStart(cal);
                                String itemDetails = schItem.getName() + " (" + df.format(schItem.getStart(), new StringBuffer(), new FieldPosition(0)) + " " + schItem.getDuration() + " " + schItem.getChannel() + ") ";
                                System.out.println("Matched Item = " + schItem.getName() + " " + schItem.getStart() + " " + schItem);
                                String itemAction = "";
                                StringBuffer conflictResult = new StringBuffer();
                                String itemResult = "";
                                boolean alreadyInList = isAlreadyInList(schItem, epgMatch.getExistingCheckType());
                                if (alreadyInList) {
                                    itemAction = "ERROR";
                                    itemResult = "Already Exists";
                                    AddConflictDetails acd = new AddConflictDetails(guidItem.getName());
                                    acd.setConflict("A schedule for this EPG item already Exists.");
                                    acd.setReason("Already Exists");
                                    conflictList.add(acd);
                                } else if (cal.after(Calendar.getInstance())) {
                                    int overlaps = numOverlaps(schItem, conflictResult);
                                    if (overlaps <= devList.getDeviceCount()) {
                                        store.addScheduleItem(schItem);
                                        itemAction = "ADDED";
                                        num++;
                                    } else {
                                        itemAction = "ERROR";
                                        itemResult = "Item overlapped";
                                        AddConflictDetails acd = new AddConflictDetails(itemDetails);
                                        acd.setConflict(conflictResult.toString());
                                        acd.setReason(itemResult);
                                        conflictList.add(acd);
                                        conflictNum++;
                                    }
                                } else {
                                    itemAction = "ERROR";
                                    itemResult = "Before now";
                                }
                                if (format == 1) {
                                    if (itemAction.equalsIgnoreCase("ERROR")) buff.append("<b><font color='#FF0000'>" + itemAction + "</font></b> - " + itemDetails + " (" + itemResult + ")<br>\n"); else buff.append("<b><font color='#00FF00'>" + itemAction + "</font></b> - " + itemDetails + " (" + itemResult + ")<br>\n");
                                } else buff.append(itemAction + " - " + itemDetails + " (" + itemResult + ")\n");
                            }
                        }
                    }
                }
            } else {
                Vector items = epgMatch.getMatchListNames();
                String names = "";
                for (int q = 0; q < items.size(); q++) {
                    String name = (String) items.get(q);
                    if (format == 1) {
                        name = name.replaceAll("<", "&lt;");
                        name = name.replaceAll(">", "&gt;");
                    }
                    if (q == items.size() - 1) {
                        names += name;
                    } else names += name + ", ";
                }
                if (names.length() == 0) names = "No Match List Assigned";
                if (format == 1) {
                    buff.append("Auto-Add (" + names + ") is disabled.<br>\n");
                } else {
                    buff.append("Auto-Add (" + names + ") is disabled.\n");
                }
            }
        }
        if (format == 1) buff.append("<br>Matched and added " + num + " items from the EPG data.<br>\n"); else buff.append("\nMatched and added " + num + " items from the EPG data.\n");
        if (conflictNum > 0 && format == 1) {
            buff.append("<br>There were " + conflictNum + " conflicts, check the <a href='/servlet/EpgAutoAddDataRes?action=25'>CONFLICT REPORT PAGE</a> for more info.<br>\n");
        }
        return num;
    }

    public boolean isAlreadyInList(ScheduleItem schItem, int checkType) {
        if (checkType == 0) return false;
        DataStore store = DataStore.getInstance();
        ScheduleItem[] itemsArray = store.getScheduleArray();
        for (int x = 0; x < itemsArray.length; x++) {
            ScheduleItem item = itemsArray[x];
            if (schItem.equals(item)) return true;
            if (checkType == 2 || schItem.getChannel().equals(item.getChannel())) {
                if (schItem.getCreatedFrom() != null && item.getCreatedFrom() != null) {
                    if (schItem.getCreatedFrom().matches(item.getCreatedFrom())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int numOverlaps(ScheduleItem schItem, StringBuffer buff) {
        DataStore store = DataStore.getInstance();
        Vector<ScheduleItem> overlapping = new Vector<ScheduleItem>();
        SimpleDateFormat df = new SimpleDateFormat("EE d HH:mm");
        ScheduleItem[] itemsArray = store.getScheduleArray();
        for (int x = 0; x < itemsArray.length; x++) {
            ScheduleItem item = itemsArray[x];
            if (item.isOverlapping(schItem)) {
                overlapping.add(item);
                buff.append(item.getName() + " (" + df.format(item.getStart()));
                buff.append(" " + item.getDuration() + " ");
                buff.append(item.getChannel() + ")<br>\n");
            }
        }
        if (overlapping.size() == 0) return 1;
        int overlapCount = getOverlapCount(schItem, overlapping);
        return overlapCount;
    }

    private int getOverlapCount(ScheduleItem item, Vector<ScheduleItem> overlapping) {
        Date start = item.getStart();
        Date end = item.getStop();
        DataStore store = DataStore.getInstance();
        HashMap<String, Channel> channels = store.getChannels();
        Channel schChannel = channels.get(item.getChannel());
        String muxString = schChannel.getFrequency() + "-" + schChannel.getBandWidth();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);
        int maxOverlap = 0;
        while (startCal.before(endCal)) {
            Calendar span = Calendar.getInstance();
            span.setTime(startCal.getTime());
            span.add(Calendar.MINUTE, 1);
            HashMap<String, Integer> muxOverlapCount = new HashMap<String, Integer>();
            muxOverlapCount.put(muxString, new Integer(1));
            getOverlapsForMin(startCal.getTime(), span.getTime(), muxOverlapCount, overlapping, channels);
            Integer[] count = (Integer[]) muxOverlapCount.values().toArray(new Integer[0]);
            if (maxOverlap < count.length) maxOverlap = count.length;
            startCal.add(Calendar.MINUTE, 1);
        }
        return maxOverlap;
    }

    private void getOverlapsForMin(Date start, Date end, HashMap<String, Integer> muxOverlapCount, Vector<ScheduleItem> overlapping, HashMap<String, Channel> channels) {
        for (int x = 0; x < overlapping.size(); x++) {
            ScheduleItem item = overlapping.get(x);
            Channel schChannel = channels.get(item.getChannel());
            String muxString = schChannel.getFrequency() + "-" + schChannel.getBandWidth();
            boolean overlap = false;
            if (item.getStart().getTime() >= start.getTime() && item.getStart().getTime() < end.getTime()) overlap = true;
            if (item.getStop().getTime() > start.getTime() && item.getStop().getTime() <= end.getTime()) overlap = true;
            if (item.getStart().getTime() <= start.getTime() && item.getStop().getTime() >= end.getTime()) overlap = true;
            if (overlap) {
                Integer muxCount = muxOverlapCount.get(muxString);
                if (muxCount == null) {
                    muxOverlapCount.put(muxString, new Integer(1));
                } else {
                    muxOverlapCount.put(muxString, new Integer(muxCount.intValue() + 1));
                }
            }
        }
    }

    public Vector<AddConflictDetails> getConflictList() {
        return conflictList;
    }

    public int simpleEpgSearch(String lookFor, int type, String cat, String chan, int ignored, int[] times, HashMap<String, Vector<GuideItem>> result) {
        int num = 0;
        GuideItem[] progs = null;
        GuideItem guidItem = null;
        DataStore store = DataStore.getInstance();
        Vector channelMap = getChannelMap();
        Set wsChannels = store.getChannels().keySet();
        if (channelMap == null || channelMap.size() == 0) {
            System.out.println("Channel map not set so could not do search.");
            return 0;
        }
        int startHH = 0;
        int startMM = 0;
        int endHH = 23;
        int endMM = 59;
        if (times != null && times.length == 4) {
            startHH = times[0];
            startMM = times[1];
            endHH = times[2];
            endMM = times[3];
        }
        for (int x = 0; x < channelMap.size(); x++) {
            String[] chanMap = (String[]) channelMap.get(x);
            if (wsChannels.contains(chanMap[0])) {
                progs = getProgramsForChannel(chanMap[1]);
                if (progs.length > 0) {
                    Vector<GuideItem> results = new Vector<GuideItem>();
                    for (int y = 0; y < progs.length; y++) {
                        guidItem = progs[y];
                        boolean nameMatches = false;
                        boolean descriptionMatches = false;
                        boolean textMatch = false;
                        boolean catMatches = false;
                        boolean chanMatches = false;
                        boolean ignoreMatches = false;
                        boolean timeSpanMatch = false;
                        if (guidItem.getName().toUpperCase().indexOf(lookFor.toUpperCase()) > -1) nameMatches = true;
                        if (guidItem.getDescription().toUpperCase().indexOf(lookFor.toUpperCase()) > -1) descriptionMatches = true;
                        if (type == 0) textMatch = nameMatches | descriptionMatches; else if (type == 1) textMatch = nameMatches; else if (type == 2) textMatch = descriptionMatches;
                        if (cat.equalsIgnoreCase("any")) {
                            catMatches = true;
                        } else {
                            for (int index = 0; index < guidItem.getCategory().size(); index++) {
                                String itemCat = guidItem.getCategory().get(index);
                                if (itemCat.equalsIgnoreCase(cat)) {
                                    catMatches = true;
                                    break;
                                }
                            }
                        }
                        if (chan.equalsIgnoreCase("any")) chanMatches = true;
                        if (chanMap[0].toUpperCase().indexOf(chan.toUpperCase()) > -1) chanMatches = true;
                        if (ignored == 2) ignoreMatches = true; else if (guidItem.getIgnored() == true && ignored == 1) ignoreMatches = true; else if (guidItem.getIgnored() == false && ignored == 0) ignoreMatches = true;
                        if (times != null && times.length == 4) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(guidItem.getStart());
                            int startMinInDay = (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE);
                            int startMinInDaySPAN = (startHH * 60) + startMM;
                            int endMinInDatSPAN = (endHH * 60) + endMM;
                            if (startMinInDaySPAN < endMinInDatSPAN) {
                                if (startMinInDay >= startMinInDaySPAN && startMinInDay <= endMinInDatSPAN) timeSpanMatch = true;
                            } else {
                                if (startMinInDay >= startMinInDaySPAN || startMinInDay <= endMinInDatSPAN) timeSpanMatch = true;
                            }
                        } else {
                            timeSpanMatch = true;
                        }
                        if (catMatches && textMatch && chanMatches && ignoreMatches && timeSpanMatch) {
                            results.add(guidItem);
                            num++;
                        }
                    }
                    if (results.size() > 0) result.put(chanMap[0], results);
                }
            }
        }
        return num;
    }
}
