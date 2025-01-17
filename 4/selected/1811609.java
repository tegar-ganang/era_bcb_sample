package ws.system;

import java.util.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.*;

public class DataStore {

    private Date lastCheched = new Date();

    private HashMap<String, Date> sessionIDs = new HashMap<String, Date>();

    private String version = "N/A";

    private static DataStore instance = null;

    private HashMap<String, String> agentToThemeMap = new HashMap<String, String>();

    private HashMap<String, ScheduleItem> times = null;

    private Date lastDataChange = new Date();

    private HashMap<String, Channel> channels = null;

    private Properties serverProp = new Properties();

    public Random rand = new Random(new Date().getTime());

    public HashMap<Integer, String> monthNameFull = new HashMap<Integer, String>();

    public HashMap<Integer, String> monthNameShort = new HashMap<Integer, String>();

    public HashMap<Integer, String> dayName = new HashMap<Integer, String>();

    public HashMap<Integer, String> dayNameFull = new HashMap<Integer, String>();

    public HashMap<Integer, String> ampm = new HashMap<Integer, String>();

    private HashMap<String, KeepForDetails> autoDelList = null;

    private StringBuffer autoDelLog = new StringBuffer(2048);

    private Vector<EpgMatch> epgMatchList = new Vector<EpgMatch>();

    private HashMap<String, TaskCommand> tasks = new HashMap<String, TaskCommand>();

    public HashMap<String, TaskItemThread> runningTaskList = new HashMap<String, TaskItemThread>();

    public int activeTaskCount = 0;

    private HashMap<String, String> mimeTypes = new HashMap<String, String>();

    public int timerStatus = 0;

    public int adminStatus = 0;

    public String timerThreadErrorStack = "";

    public String adminThreadErrorStack = "";

    private void initMaps() {
        monthNameFull.put(new Integer(Calendar.JANUARY), "January");
        monthNameFull.put(new Integer(Calendar.FEBRUARY), "February");
        monthNameFull.put(new Integer(Calendar.MARCH), "March");
        monthNameFull.put(new Integer(Calendar.APRIL), "April");
        monthNameFull.put(new Integer(Calendar.MAY), "May");
        monthNameFull.put(new Integer(Calendar.JUNE), "June");
        monthNameFull.put(new Integer(Calendar.JULY), "July");
        monthNameFull.put(new Integer(Calendar.AUGUST), "August");
        monthNameFull.put(new Integer(Calendar.SEPTEMBER), "September");
        monthNameFull.put(new Integer(Calendar.OCTOBER), "October");
        monthNameFull.put(new Integer(Calendar.NOVEMBER), "November");
        monthNameFull.put(new Integer(Calendar.DECEMBER), "December");
        monthNameShort.put(new Integer(Calendar.JANUARY), "Jan");
        monthNameShort.put(new Integer(Calendar.FEBRUARY), "Feb");
        monthNameShort.put(new Integer(Calendar.MARCH), "Mar");
        monthNameShort.put(new Integer(Calendar.APRIL), "Apr");
        monthNameShort.put(new Integer(Calendar.MAY), "May");
        monthNameShort.put(new Integer(Calendar.JUNE), "Jun");
        monthNameShort.put(new Integer(Calendar.JULY), "Jul");
        monthNameShort.put(new Integer(Calendar.AUGUST), "Aug");
        monthNameShort.put(new Integer(Calendar.SEPTEMBER), "Sep");
        monthNameShort.put(new Integer(Calendar.OCTOBER), "Oct");
        monthNameShort.put(new Integer(Calendar.NOVEMBER), "Nov");
        monthNameShort.put(new Integer(Calendar.DECEMBER), "Dec");
        dayName.put(new Integer(Calendar.MONDAY), "Mon");
        dayName.put(new Integer(Calendar.TUESDAY), "Tue");
        dayName.put(new Integer(Calendar.WEDNESDAY), "Wed");
        dayName.put(new Integer(Calendar.THURSDAY), "Thu");
        dayName.put(new Integer(Calendar.FRIDAY), "Fri");
        dayName.put(new Integer(Calendar.SATURDAY), "Sat");
        dayName.put(new Integer(Calendar.SUNDAY), "Sun");
        dayNameFull.put(new Integer(Calendar.MONDAY), "Monday");
        dayNameFull.put(new Integer(Calendar.TUESDAY), "Tuesday");
        dayNameFull.put(new Integer(Calendar.WEDNESDAY), "Wednesday");
        dayNameFull.put(new Integer(Calendar.THURSDAY), "Thursday");
        dayNameFull.put(new Integer(Calendar.FRIDAY), "Friday");
        dayNameFull.put(new Integer(Calendar.SATURDAY), "Saturday");
        dayNameFull.put(new Integer(Calendar.SUNDAY), "Sunday");
        ampm.put(new Integer(Calendar.AM), "AM");
        ampm.put(new Integer(Calendar.PM), "PM");
    }

    public static DataStore getInstance() {
        synchronized (DataStore.class) {
            if (instance == null) {
                instance = new DataStore();
                return instance;
            } else {
                return instance;
            }
        }
    }

    private DataStore() {
        DllWrapper wrapper = new DllWrapper();
        String dataPath = wrapper.getAllUserPath();
        getVersionDetails();
        System.out.println("New DataStore Object created (" + version + ")");
        try {
            File servProp = new File(dataPath + "server.prop");
            FileInputStream in = new FileInputStream(servProp);
            serverProp.load(new FileInputStream(servProp));
            in.close();
        } catch (Exception e) {
            System.out.println("Error loading server.prop (" + e.getMessage() + ")");
        }
        serverProp.setProperty("path.data", dataPath + "data");
        channels = new HashMap<String, Channel>();
        loadAutoDelList();
        loadChannels();
        loadSchedule();
        initMaps();
        loadEpgAutoList();
        loadTaskList();
        loadAgentToThemeMap();
        loadMineTypes();
        StringBuffer buff = new StringBuffer();
        buff.append("TV Scheduler Pro was started (" + new Date().toString() + ")\n");
        buff.append("Following is a summary of the startup details:\n\n");
        buff.append("Channel Count           : " + numberOfChannels() + "\n");
        buff.append("Server Version          : " + getVersion() + "\n");
        buff.append("Number of Schedules     : " + times.size() + "\n");
        sendEmailServerStarted(buff.toString());
    }

    public String toString() {
        return "DataStore Properties Loaded:" + serverProp.size();
    }

    private void getVersionDetails() {
        try {
            Properties verProp = new Properties();
            FileInputStream in = new FileInputStream("version.txt");
            verProp.load(in);
            in.close();
            version = verProp.getProperty("version", "N/A");
        } catch (Exception e) {
            System.out.println("ERROR!: could not load version info");
        }
    }

    public String createSessionID() {
        List<Map.Entry<String, Date>> entrylist = new ArrayList<Map.Entry<String, Date>>(sessionIDs.entrySet());
        Collections.sort(entrylist, new SecurityPinSorter());
        sessionIDs.clear();
        for (int x = 0; x < entrylist.size() && x < 4; x++) {
            Map.Entry<String, Date> entry = entrylist.get(x);
            sessionIDs.put((String) entry.getKey(), (Date) entry.getValue());
        }
        String[] ids = (String[]) sessionIDs.keySet().toArray(new String[0]);
        Date now = new Date();
        for (int x = 0; x < ids.length; x++) {
            Date createDate = (Date) sessionIDs.get(ids[x]);
            if ((createDate.getTime() + (1000 * 60 * 3)) < now.getTime()) {
                sessionIDs.remove(ids[x]);
                System.out.println("Removed Old Session ID : " + ids[x] + " : " + ((now.getTime() - createDate.getTime()) / (1000)));
            }
        }
        Random ran = new Random();
        String ranNum = new Integer(ran.nextInt(999999)).toString();
        if (ranNum.length() == 1) ranNum = "00000" + ranNum;
        if (ranNum.length() == 2) ranNum = "0000" + ranNum;
        if (ranNum.length() == 3) ranNum = "000" + ranNum;
        if (ranNum.length() == 4) ranNum = "00" + ranNum;
        if (ranNum.length() == 5) ranNum = "0" + ranNum;
        sessionIDs.put(ranNum, new Date());
        entrylist = new ArrayList<Map.Entry<String, Date>>(sessionIDs.entrySet());
        Collections.sort(entrylist, new SecurityPinSorter());
        for (int x = 0; x < entrylist.size() && x < 5; x++) {
            Map.Entry<String, Date> entry = entrylist.get(x);
            System.out.println("Session ID : " + x + " " + (String) entry.getKey() + " " + ((Date) entry.getValue()).toString());
        }
        return ranNum;
    }

    public boolean checkSessionID(String id) {
        String captcha = getProperty("security.captcha");
        if ("0".equals(captcha)) return true;
        synchronized (this) {
            long sleepFor = (new Date()).getTime() - lastCheched.getTime();
            if (sleepFor < 10000) {
                try {
                    Thread.sleep(10000 - sleepFor);
                } catch (Exception e) {
                }
            }
            lastCheched = new Date();
            String[] ids = (String[]) sessionIDs.keySet().toArray(new String[0]);
            Date now = new Date();
            for (int x = 0; x < ids.length; x++) {
                Date createDate = (Date) sessionIDs.get(ids[x]);
                if ((createDate.getTime() + (1000 * 60 * 3)) < now.getTime()) {
                    System.out.println("Removed Old Session ID : " + ids[x] + " - " + createDate);
                    sessionIDs.remove(ids[x]);
                }
            }
            Date testDate = (Date) sessionIDs.get(id);
            if (testDate != null) {
                sessionIDs.remove(id);
                System.out.println("Removed Used Session ID : " + id + " - " + testDate);
                return true;
            } else return false;
        }
    }

    private void sendEmailServerStarted(String body) {
        String sendServerStarted = getProperty("email.send.serverstarted");
        if ("1".equals(sendServerStarted) == false) return;
        EmailSender sender = new EmailSender();
        sender.setSubject("TV Scheduler Pro Started");
        sender.setBody(body);
        try {
            Thread mailThread = new Thread(Thread.currentThread().getThreadGroup(), sender, sender.getClass().getName());
            mailThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getThemeForAgent(String agent) {
        return agentToThemeMap.get(agent);
    }

    public String[] getAgentMappingList() {
        return (String[]) agentToThemeMap.keySet().toArray(new String[0]);
    }

    public void addAgentToThemeMap(String agent, String theme) {
        agentToThemeMap.put(agent, theme);
        saveAgentToThemeMap(null);
    }

    public void removeAgentToThemeMap(String agent) {
        agentToThemeMap.remove(agent);
        saveAgentToThemeMap(null);
    }

    public int saveAgentToThemeMap(ByteArrayOutputStream agentMapBytes) {
        try {
            ObjectOutputStream oos = null;
            if (agentMapBytes == null) {
                FileOutputStream fos = new FileOutputStream(this.getProperty("path.data") + File.separator + "AgentMap.sof");
                oos = new ObjectOutputStream(fos);
            } else {
                oos = new ObjectOutputStream(agentMapBytes);
            }
            oos.writeObject(agentToThemeMap);
            oos.close();
            if (agentMapBytes == null) System.out.println("AgentMap.sof saved.");
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public void importAgentToThemeMap(byte[] agentMapBytes) throws Exception {
        ByteArrayInputStream mapBytes = new ByteArrayInputStream(agentMapBytes);
        ObjectInputStream ois = new ObjectInputStream(mapBytes);
        agentToThemeMap = (HashMap<String, String>) ois.readObject();
        ois.close();
        System.out.println("Agent to Theme Map imported");
        this.saveAgentToThemeMap(null);
    }

    @SuppressWarnings("unchecked")
    private void loadAgentToThemeMap() {
        try {
            FileInputStream fis = new FileInputStream(this.getProperty("path.data") + File.separator + "AgentMap.sof");
            ObjectInputStream ois = new ObjectInputStream(fis);
            agentToThemeMap = (HashMap<String, String>) ois.readObject();
            ois.close();
            System.out.println("AgentMap.sof found and loaded");
            refreshWakeupTime();
        } catch (Exception e) {
            agentToThemeMap = new HashMap<String, String>();
            System.out.println("Error loading AgentMap.sof, starting with blank map");
        }
    }

    private void loadTaskList() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(this.getProperty("path.data") + File.separator + "Tasks.xml"));
            NodeList tasksNodes = doc.getElementsByTagName("task");
            tasks = new HashMap<String, TaskCommand>();
            for (int x = 0; x < tasksNodes.getLength(); x++) {
                Node item = tasksNodes.item(x);
                TaskCommand taskCommand = new TaskCommand(item);
                tasks.put(taskCommand.getName(), taskCommand);
            }
            System.out.println("Tasks.xml found and loaded (" + tasksNodes.getLength() + ")");
        } catch (Exception e) {
            tasks = new HashMap<String, TaskCommand>();
            System.out.println("Error loading Tasks.xml, starting with no tasks.");
        }
    }

    public void importTaskList(String data, boolean append) throws Exception {
        HashMap<String, TaskCommand> importedTasks = new HashMap<String, TaskCommand>();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream reader = new ByteArrayInputStream(data.toString().getBytes());
        Document doc = docBuilder.parse(reader);
        NodeList tasksNodes = doc.getElementsByTagName("task");
        for (int x = 0; x < tasksNodes.getLength(); x++) {
            Node item = tasksNodes.item(x);
            TaskCommand taskCommand = new TaskCommand(item);
            importedTasks.put(taskCommand.getName(), taskCommand);
        }
        if (append) {
            if (tasks == null) tasks = new HashMap<String, TaskCommand>();
            tasks.putAll(importedTasks);
        } else {
            tasks = importedTasks;
        }
        saveTaskList(null);
    }

    public void saveTaskList(StringBuffer output) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "tasks", null);
        Element root = doc.getDocumentElement();
        String[] keys = (String[]) tasks.keySet().toArray(new String[0]);
        for (int x = 0; x < keys.length; x++) {
            TaskCommand taskData = (TaskCommand) tasks.get(keys[x]);
            taskData.addXML(doc, root);
        }
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Source source = new DOMSource(doc);
        Result result = new StreamResult(buff);
        transformer.transform(source, result);
        if (output != null) {
            output.append(buff.toString());
        } else {
            FileWriter out = new FileWriter(this.getProperty("path.data") + File.separator + "Tasks.xml");
            out.write(buff.toString());
            out.close();
            System.out.println("Tasks.xml saved.");
        }
    }

    public HashMap<String, TaskCommand> getTaskList() {
        return tasks;
    }

    private void loadEpgAutoList() {
        epgMatchList = new Vector<EpgMatch>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(this.getProperty("path.data") + File.separator + "EpgAutoAdd.xml"));
            NodeList items = doc.getElementsByTagName("item");
            for (int x = 0; x < items.getLength(); x++) {
                Node item = items.item(x);
                EpgMatch matcher = new EpgMatch(item);
                epgMatchList.add(matcher);
            }
            System.out.println("EpgAutoAdd.xml found and loaded (" + items.getLength() + ")");
        } catch (Exception e) {
            epgMatchList = new Vector<EpgMatch>();
            System.out.println("Error loading EpgAutoAdd.xml, starting with no AutoAdds");
        }
    }

    public void importEpgAutoList(String data, boolean append) throws Exception {
        Vector<EpgMatch> importedData = new Vector<EpgMatch>();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream reader = new ByteArrayInputStream(data.toString().getBytes());
        Document doc = docBuilder.parse(reader);
        NodeList items = doc.getElementsByTagName("item");
        for (int x = 0; x < items.getLength(); x++) {
            Node item = items.item(x);
            EpgMatch matcher = new EpgMatch(item);
            importedData.add(matcher);
        }
        if (append) {
            epgMatchList.addAll(importedData);
        } else {
            epgMatchList = importedData;
        }
        saveEpgAutoList(null);
    }

    public void saveEpgAutoList(StringBuffer output) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "auto-add", null);
        Element root = doc.getDocumentElement();
        for (int x = 0; x < epgMatchList.size(); x++) {
            EpgMatch item = (EpgMatch) epgMatchList.get(x);
            item.getXML(doc, root);
        }
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Source source = new DOMSource(doc);
        Result result = new StreamResult(buff);
        transformer.transform(source, result);
        if (output != null) {
            output.append(buff.toString());
        } else {
            FileWriter out = new FileWriter(this.getProperty("path.data") + File.separator + "EpgAutoAdd.xml");
            out.write(buff.toString());
            out.close();
            System.out.println("EpgAutoAdd.xml saved.");
        }
    }

    public Vector<EpgMatch> getEpgMatchList() {
        return epgMatchList;
    }

    public void addEpgMatch(EpgMatch match) throws Exception {
        epgMatchList.add(match);
        saveEpgAutoList(null);
    }

    public void addEpgMatch(EpgMatch match, int index) throws Exception {
        epgMatchList.add(index, match);
        saveEpgAutoList(null);
    }

    public void remEpgMatch(int id) throws Exception {
        epgMatchList.remove(id);
        saveEpgAutoList(null);
    }

    public void moveEpgItem(int id, boolean direction) throws Exception {
        int dest = 0;
        if (direction) dest = id + 1; else dest = id - 1;
        if (dest >= 0 && dest < epgMatchList.size()) {
            EpgMatch obj = (EpgMatch) epgMatchList.remove(id);
            if (direction) epgMatchList.add(id + 1, obj); else epgMatchList.add(id - 1, obj);
            saveEpgAutoList(null);
        }
    }

    public int removeEPGitems(StringBuffer buff, int format) {
        int num = 0;
        ScheduleItem item = null;
        String[] ids = (String[]) (times.keySet()).toArray(new String[0]);
        for (int x = 0; x < ids.length; x++) {
            item = (ScheduleItem) times.get(ids[x]);
            if (item.getType() == ScheduleItem.EPG && item.getState() == ScheduleItem.WAITING) {
                times.remove(ids[x]);
                num++;
            }
        }
        if (format == 1) buff.append("Removed " + num + " item of type EPG from the schedule list.<br><br>\n\n"); else buff.append("Removed " + num + " item of type EPG from the schedule list.\n\n");
        return num;
    }

    public ScheduleItem[] getSchedulesWhen(Date start, Date end, String channel) {
        Vector<ScheduleItem> items = new Vector<ScheduleItem>();
        Iterator<ScheduleItem> it = times.values().iterator();
        ScheduleItem item = null;
        while (it.hasNext()) {
            item = (ScheduleItem) it.next();
            if (start.getTime() < item.getStart().getTime() && end.getTime() > item.getStart().getTime()) {
                if (channel.equals(item.getChannel())) {
                    items.add(item);
                }
            }
        }
        ScheduleItem[] itemList = (ScheduleItem[]) items.toArray(new ScheduleItem[0]);
        Arrays.sort(itemList);
        return itemList;
    }

    public void getSchedulesWhenInc(Date start, Date end, String channel, Vector<ScheduleItem> items) {
        ScheduleItem[] schedules = (ScheduleItem[]) times.values().toArray(new ScheduleItem[0]);
        Arrays.sort(schedules);
        for (int y = 0; y < schedules.length; y++) {
            ScheduleItem item = schedules[y];
            if (item.getStop().getTime() > (start.getTime() + 5000) && item.getStart().getTime() < end.getTime()) {
                if (channel.equals(item.getChannel())) {
                    items.add(item);
                }
            }
        }
    }

    public ScheduleItem getNextSchedule() {
        Date now = new Date();
        ScheduleItem[] itemsArray = (ScheduleItem[]) times.values().toArray(new ScheduleItem[0]);
        Arrays.sort(itemsArray);
        for (int x = 0; x < itemsArray.length; x++) {
            ScheduleItem item = itemsArray[x];
            if (item.getStop().getTime() > now.getTime() && item.getState() == ScheduleItem.WAITING) {
                return item;
            }
        }
        return null;
    }

    public String[] getScheduleKeys() {
        return (String[]) times.keySet().toArray(new String[0]);
    }

    public ScheduleItem[] getScheduleArray() {
        return (ScheduleItem[]) times.values().toArray(new ScheduleItem[0]);
    }

    public int getScheduleCount() {
        return times.size();
    }

    public ScheduleItem getScheduleItem(String id) {
        return (ScheduleItem) times.get(id);
    }

    public int addScheduleItem(ScheduleItem item) {
        times.put(item.toString(), item);
        saveSchedule(null);
        return 1;
    }

    public ScheduleItem removeScheduleItem(String id) {
        ScheduleItem removedItem = times.remove(id);
        return removedItem;
    }

    public void addChannel(Channel chan) {
        channels.put(chan.getName(), chan);
    }

    public Channel getChannel(String chID) {
        return (Channel) channels.get(chID);
    }

    public int removeChannel(String name) {
        channels.remove(name);
        return 0;
    }

    public HashMap<String, Channel> getChannels() {
        return channels;
    }

    private void loadChannels() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(this.getProperty("path.data") + File.separator + "Channels.xml"));
            NodeList items = doc.getElementsByTagName("channel");
            channels = new HashMap<String, Channel>();
            for (int x = 0; x < items.getLength(); x++) {
                Node item = items.item(x);
                Channel chan = new Channel(item);
                channels.put(chan.getName(), chan);
            }
            System.out.println("Channels.xml found and loaded (" + items.getLength() + ")");
        } catch (Exception e) {
            channels = new HashMap<String, Channel>();
            System.out.println("Error loading Channels.xml, starting with blank channel list.");
        }
    }

    public void importChannels(String data, boolean append) throws Exception {
        HashMap<String, Channel> importedChannels = new HashMap<String, Channel>();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream reader = new ByteArrayInputStream(data.toString().getBytes());
        Document doc = docBuilder.parse(reader);
        NodeList items = doc.getElementsByTagName("channel");
        for (int x = 0; x < items.getLength(); x++) {
            Node item = items.item(x);
            Channel chan = new Channel(item);
            importedChannels.put(chan.getName(), chan);
        }
        if (append) {
            if (channels == null) channels = new HashMap<String, Channel>();
            channels.putAll(importedChannels);
        } else {
            channels = importedChannels;
        }
        saveChannels(null);
    }

    public void saveChannels(StringBuffer output) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation di = db.getDOMImplementation();
        Document doc = di.createDocument("", "channels", null);
        Element root = doc.getDocumentElement();
        String[] keys = (String[]) channels.keySet().toArray(new String[0]);
        for (int x = 0; x < keys.length; x++) {
            Channel chan = (Channel) channels.get(keys[x]);
            root.appendChild(chan.getXML(doc));
        }
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        Source source = new DOMSource(doc);
        Result result = new StreamResult(buff);
        transformer.transform(source, result);
        if (output != null) {
            output.append(buff.toString());
        } else {
            FileWriter out = new FileWriter(this.getProperty("path.data") + File.separator + "Channels.xml");
            out.write(buff.toString());
            out.close();
            System.out.println("Channels.xml saved.");
        }
    }

    public int numberOfChannels() {
        return channels.size();
    }

    public void setServerProperty(String pKey, String value) throws Exception {
        pKey = pKey.toLowerCase();
        serverProp.setProperty(pKey, value);
        DllWrapper wrapper = new DllWrapper();
        String dataPath = wrapper.getAllUserPath();
        File servProp = new File(dataPath + "server.prop");
        FileOutputStream out = new FileOutputStream(servProp);
        serverProp.store(out, "TV Scheduler Pro Server Properties");
        out.close();
    }

    public String getProperty(String pKey) {
        pKey = pKey.toLowerCase();
        String prop = serverProp.getProperty(pKey, null);
        if (prop != null) return prop;
        String def = getDefProp(pKey);
        try {
            setServerProperty(pKey, def);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    private String getDefProp(String key) {
        HashMap<String, String> defprop = new HashMap<String, String>();
        defprop.put("tools.testmode", "0");
        defprop.put("capture.path", "capture");
        defprop.put("capture.minspacesoft", "1200");
        defprop.put("capture.minspacehard", "200");
        defprop.put("capture.deletetofreespace", "0");
        defprop.put("capture.path.details", "none");
        defprop.put("schedule.wake.system", "45");
        defprop.put("guide.source.schedule", "0:0:0");
        defprop.put("capture.filename.patterns", "(%y-%m-%d %h-%M) %n %c;%n %c;%n;%D %n;%n\\%y-%m-%d %h-%M");
        defprop.put("path.httproot", "http");
        defprop.put("path.theme", "default");
        defprop.put("path.template", "data/templates");
        defprop.put("capture.merged.separate", "1");
        defprop.put("path.data", "data");
        defprop.put("schedule.buffer.start", "5");
        defprop.put("schedule.buffer.end", "10");
        defprop.put("schedule.buffer.end.epg", "0");
        defprop.put("schedule.overlap", "0");
        defprop.put("autodel.keepfor", "30");
        defprop.put("capture.deftype", "2");
        defprop.put("filebrowser.masks", ".log,.mpg,.mpeg,.bin,.mpv,.mpa,.ts,.tp,.dvr-ms,.rec,.pva,.avi");
        defprop.put("guide.action.name", "0");
        defprop.put("guide.source.type", "0");
        defprop.put("filebrowser.dirsattop", "1");
        defprop.put("filebrowser.showwsplay", "1");
        defprop.put("guide.source.file", "data/xmltv");
        defprop.put("sch.autodel.time", "0");
        defprop.put("sch.autodel.action", "0");
        defprop.put("server.kbled", "0");
        defprop.put("proxy.port", "8080");
        defprop.put("proxy.server", "");
        defprop.put("guide.warn.overlap", "1");
        defprop.put("guide.search.url", "http://www.google.com.au/search?q=$TITLE $SUB $CAT");
        defprop.put("capture.averagedatarate", "7000000");
        defprop.put("capture.includecalculatedUsage", "1");
        defprop.put("capture.autoselectmethod", "0");
        defprop.put("capture.capturefailedtimeout", "060");
        defprop.put("epg.showunlinked", "0");
        defprop.put("email.server.address", "");
        defprop.put("email.server.port", "25");
        defprop.put("email.from", "");
        defprop.put("email.to", "");
        defprop.put("email.auth.enabled", "0");
        defprop.put("email.auth.user", "");
        defprop.put("email.auth.password", "");
        defprop.put("email.security", "0");
        defprop.put("email.send.weeklyreport", "0");
        defprop.put("email.send.capfinished", "0");
        defprop.put("email.send.epgloaded", "0");
        defprop.put("email.send.onwarning", "0");
        defprop.put("email.send.freespacelow", "0");
        defprop.put("email.send.serverstarted", "0");
        defprop.put("security.captcha", "1");
        defprop.put("security.highsecurity", "0");
        defprop.put("security.accesslog", "0");
        defprop.put("security.authentication", "0");
        defprop.put("security.username", "user");
        defprop.put("security.password", "password");
        defprop.put("security.protocol", "http");
        defprop.put("security.port", "8420");
        String defValue = defprop.get(key);
        if (defValue == null) {
            System.out.println("Server property (" + key + ") does not have a default value in DataStore.getDefProp()!");
            return "";
        } else {
            return defValue;
        }
    }

    public String getLastDataChange() {
        return lastDataChange.toString();
    }

    public void saveSchedule(ByteArrayOutputStream timesBytes) {
        try {
            ObjectOutputStream oos = null;
            if (timesBytes == null) {
                FileOutputStream fos = new FileOutputStream(this.getProperty("path.data") + File.separator + "Times.sof");
                oos = new ObjectOutputStream(fos);
            } else {
                oos = new ObjectOutputStream(timesBytes);
            }
            oos.writeObject(times);
            oos.close();
            refreshWakeupTime();
            lastDataChange = new Date();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void importSchedule(byte[] timesBytes) throws Exception {
        ByteArrayInputStream timeBytes = new ByteArrayInputStream(timesBytes);
        ObjectInputStream ois = new ObjectInputStream(timeBytes);
        times = (HashMap<String, ScheduleItem>) ois.readObject();
        ois.close();
        System.out.println("Times imported (" + times.size() + ")");
        refreshWakeupTime();
        saveSchedule(null);
    }

    @SuppressWarnings("unchecked")
    private void loadSchedule() {
        try {
            FileInputStream fis = new FileInputStream(this.getProperty("path.data") + File.separator + "Times.sof");
            ObjectInputStream ois = new ObjectInputStream(fis);
            times = (HashMap<String, ScheduleItem>) ois.readObject();
            ois.close();
            System.out.println("Times.sof found and loaded (" + times.size() + ")");
            refreshWakeupTime();
        } catch (Exception e) {
            times = new HashMap<String, ScheduleItem>();
            System.out.println("Error loading Times.sof, starting with blank schedule");
        }
    }

    public int refreshWakeupTime() {
        int allow = 0;
        try {
            allow = Integer.parseInt(getProperty("schedule.wake.system").trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (allow == 0) {
            System.out.println("Not Using wakeup STUFF");
            return -2;
        }
        DllWrapper capEng = new DllWrapper();
        Calendar wakeAt = getClosestStart();
        int sec = 0;
        int min = 0;
        int hour = 0;
        int day = 0;
        int month = 0;
        int year = 0;
        if (wakeAt != null) {
            sec = wakeAt.get(Calendar.SECOND);
            min = wakeAt.get(Calendar.MINUTE);
            hour = wakeAt.get(Calendar.HOUR_OF_DAY);
            day = wakeAt.get(Calendar.DATE);
            month = wakeAt.get(Calendar.MONTH) + 1;
            year = wakeAt.get(Calendar.YEAR);
            capEng.setNextScheduleTime(year, month, day, hour, min, sec);
        }
        String scheduleOptions = getProperty("guide.source.schedule");
        String[] schOptsArray = scheduleOptions.split(":");
        if (schOptsArray.length == 3) {
            if ("1".equals(schOptsArray[0])) {
                int data_hour = 0;
                try {
                    data_hour = Integer.parseInt(schOptsArray[1]);
                } catch (Exception e) {
                }
                int data_min = 0;
                try {
                    data_min = Integer.parseInt(schOptsArray[2]);
                } catch (Exception e) {
                }
                Calendar guideSch = Calendar.getInstance();
                guideSch.set(Calendar.HOUR_OF_DAY, data_hour);
                guideSch.set(Calendar.MINUTE, data_min);
                guideSch.set(Calendar.MILLISECOND, 0);
                guideSch.set(Calendar.SECOND, 0);
                if (guideSch.before(Calendar.getInstance())) guideSch.add(Calendar.DATE, 1);
                if (wakeAt == null || guideSch.before(wakeAt)) wakeAt = guideSch;
            }
        }
        if (wakeAt == null) {
            System.out.println("refreshWakeupTime() Failed! could not work out the next wake time.");
            return -1;
        }
        wakeAt.add(Calendar.SECOND, (0 - allow));
        sec = wakeAt.get(Calendar.SECOND);
        min = wakeAt.get(Calendar.MINUTE);
        hour = wakeAt.get(Calendar.HOUR_OF_DAY);
        day = wakeAt.get(Calendar.DATE);
        month = wakeAt.get(Calendar.MONTH) + 1;
        year = wakeAt.get(Calendar.YEAR);
        System.out.println("About to set wakeup time " + day + "/" + month + "/" + year + " " + hour + ":" + min + ":" + sec + " -" + allow);
        int result = capEng.setWakeUpTime(year, month, day, hour, min, sec);
        if (result != 0) System.out.println("SetWaitableTimer FAILED! (" + result + ")"); else System.out.println("SetWaitableTimer Succeeded (" + result + ")");
        return result;
    }

    private Calendar getClosestStart() {
        Calendar nearest = Calendar.getInstance();
        nearest.add(Calendar.YEAR, 10);
        Calendar start = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        String[] keys = (String[]) times.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (int x = 0; x < keys.length; x++) {
            ScheduleItem item = (ScheduleItem) times.get(keys[x]);
            if (item == null) {
                System.out.println("ERROR: for some reason one of your schedule items in the MAP is null : " + keys[x]);
                break;
            }
            start.setTime(item.getStart());
            if (start.after(now)) {
                if (start.before(nearest)) nearest.setTime(start.getTime());
            }
        }
        return nearest;
    }

    public HashMap<String, KeepForDetails> getAutoDelList() {
        return autoDelList;
    }

    public void saveAutoDelList() {
        try {
            FileOutputStream fos = new FileOutputStream(this.getProperty("path.data") + File.separator + "AutoDel.sof");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(autoDelList);
            oos.close();
            System.out.println("AutoDel.sof saved.");
        } catch (Exception e) {
            System.out.println("Error loading AutoDel.sof, will start with no Auto Delete list.");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadAutoDelList() {
        try {
            FileInputStream fis = new FileInputStream(this.getProperty("path.data") + File.separator + "AutoDel.sof");
            ObjectInputStream ois = new ObjectInputStream(fis);
            autoDelList = (HashMap<String, KeepForDetails>) ois.readObject();
            ois.close();
            System.out.println("AutoDel.sof loaded");
        } catch (Exception e) {
            autoDelList = new HashMap<String, KeepForDetails>();
            System.out.println("Error loading AutoDel.sof, starting with empty list.");
        }
    }

    public void addAutoDeleteItem(String fileName, int keepFor) {
        if (fileName == null) return;
        String[] key = (String[]) autoDelList.keySet().toArray(new String[0]);
        for (int x = 0; x < key.length; x++) {
            KeepForDetails item = (KeepForDetails) autoDelList.get(key[x]);
            if (fileName.equalsIgnoreCase(item.getFileName())) autoDelList.remove(key[x]);
        }
        KeepForDetails kpd = new KeepForDetails(fileName, keepFor);
        String key02 = new Long(new Date().getTime()).toString() + "-" + new Long(rand.nextLong()).toString();
        autoDelList.put(key02, kpd);
        saveAutoDelList();
    }

    public void resetAutoDelLog() {
        autoDelLog = new StringBuffer(2048);
    }

    public void autoDelLogAdd(String log) {
        autoDelLog.append(log + "<br>\n");
    }

    public String getAutoDelLog() {
        return autoDelLog.toString();
    }

    public String getVersion() {
        return version;
    }

    public String intToStr(int num) {
        if (num > -1 && num < 10) return ("0" + (new Integer(num).toString())); else return (new Integer(num).toString());
    }

    public String[] getCapturePaths() {
        String patterns = getProperty("capture.path");
        String[] line = patterns.split(";");
        for (int x = 0; x < line.length; x++) {
            line[x] = line[x].trim();
        }
        return line;
    }

    public void addCapturePath(String newPath) throws Exception {
        String patterns = getProperty("capture.path");
        patterns += ";" + newPath;
        this.setServerProperty("capture.path", patterns);
    }

    public void deleteCapturePath(int index) throws Exception {
        String patterns = getProperty("capture.path");
        String[] line = patterns.split(";");
        String newNames = "";
        for (int x = 0; x < line.length; x++) {
            if (x != index) {
                newNames += line[x].trim() + ";";
            }
        }
        newNames = newNames.substring(0, newNames.length() - 1);
        this.setServerProperty("capture.path", newNames);
    }

    public void moveCapturePath(int index, int amount) throws Exception {
        String patterns = getProperty("capture.path");
        String[] line = patterns.split(";");
        int newIndex = index + amount;
        if (newIndex < 0 || newIndex > line.length - 1) return;
        String temp = line[newIndex];
        line[newIndex] = line[index];
        line[index] = temp;
        String newNames = "";
        for (int x = 0; x < line.length; x++) {
            newNames += line[x].trim() + ";";
        }
        newNames = newNames.substring(0, newNames.length() - 1);
        this.setServerProperty("capture.path", newNames);
    }

    public String[] getNamePatterns() {
        String patterns = getProperty("capture.filename.patterns");
        String[] line = patterns.split(";");
        for (int x = 0; x < line.length; x++) {
            line[x] = line[x].trim();
        }
        return line;
    }

    public void deleteNamePattern(int index) throws Exception {
        String patterns = getProperty("capture.filename.patterns");
        String[] line = patterns.split(";");
        String newNames = "";
        for (int x = 0; x < line.length; x++) {
            if (x != index) {
                newNames += line[x].trim() + ";";
            }
        }
        newNames = newNames.substring(0, newNames.length() - 1);
        this.setServerProperty("capture.filename.patterns", newNames);
    }

    public void addNamePattern(String newPattern) throws Exception {
        String patterns = getProperty("capture.filename.patterns");
        patterns += ";" + newPattern;
        this.setServerProperty("capture.filename.patterns", patterns);
    }

    public void moveNamePattern(int index, int amount) throws Exception {
        String patterns = getProperty("capture.filename.patterns");
        String[] line = patterns.split(";");
        int newIndex = index + amount;
        if (newIndex < 0 || newIndex > line.length - 1) return;
        String temp = line[newIndex];
        line[newIndex] = line[index];
        line[index] = temp;
        String newNames = "";
        for (int x = 0; x < line.length; x++) {
            newNames += line[x].trim() + ";";
        }
        newNames = newNames.substring(0, newNames.length() - 1);
        this.setServerProperty("capture.filename.patterns", newNames);
    }

    public HashMap<String, String> getMimeTypes() {
        return mimeTypes;
    }

    private void loadMineTypes() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(this.getProperty("path.data") + File.separator + "mime-types.prop"));
            String line = in.readLine();
            while (line != null) {
                String[] bits = line.split("=");
                if (bits.length == 2) {
                    mimeTypes.put(bits[0], bits[1]);
                }
                line = in.readLine();
            }
        } catch (Exception e) {
            System.out.println("Error loading mime types!");
            e.printStackTrace();
        }
        System.out.println("Mime-Types loaded (" + mimeTypes.size() + ")");
    }
}
