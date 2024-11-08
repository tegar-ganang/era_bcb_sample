import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

@SuppressWarnings("serial")
public class ScheduleItem implements Serializable, Comparable {

    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private StringBuffer log = new StringBuffer();

    private Vector<String> warnings = new Vector<String>();

    private HashMap<Date, SignalStatistic> stats = new HashMap<Date, SignalStatistic>();

    private Date startTime = null;

    private Date stopTime = null;

    private int duration = 0;

    private String channel = "";

    private int state = 0;

    private String status = "";

    private boolean abort = false;

    private String fileName = "";

    private int type = 0;

    private int capType = 0;

    long id = 0;

    private boolean autoDeleteable = false;

    private String fileNamePattern = "";

    private int capturePathIndex = -1;

    private int keepFor = 30;

    private String postTask = "";

    private GuideItem createdFrom = null;

    private Vector<String> logFiles = new Vector<String>();

    public static int WAITING = 0;

    public static int SKIPPED = 1;

    public static int RUNNING = 2;

    public static int FINISHED = 3;

    public static int ABORTED = 4;

    public static int ERROR = 5;

    public static int RESTART = 6;

    public static int ONCE = 0;

    public static int DAILY = 1;

    public static int WEEKLY = 2;

    public static int MONTHLY = 3;

    public static int WEEKDAY = 4;

    public static int EPG = 5;

    public ScheduleItem(GuideItem item, String chan, int captureType, long itemID, boolean autoDel) throws Exception {
        id = itemID;
        this.setCreatedFrom(item);
        startTime = item.getStart();
        stopTime = item.getStop();
        duration = item.getDuration();
        channel = chan;
        state = WAITING;
        type = EPG;
        capType = captureType;
        status = "Waiting";
        autoDeleteable = autoDel;
        fileName = checkFileName(item.getName());
    }

    public ScheduleItem(long itemID) {
        id = itemID;
    }

    public void addLogFileName(String fileName) {
        logFiles.add(fileName);
    }

    public Vector<String> getLogFileNames() {
        return logFiles;
    }

    public Vector<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        if (warnings.contains(warning) == false) warnings.add(warning);
    }

    public void addSignalStatistic(SignalStatistic stat) {
        stats.put(new Date(), stat);
    }

    public HashMap<Date, SignalStatistic> getSignalStatistics() {
        return stats;
    }

    public int getCapturePathIndex() {
        return capturePathIndex;
    }

    public void setCapturePathIndex(int pathIndex) throws Exception {
        capturePathIndex = pathIndex;
    }

    public String getPostTask() {
        return postTask;
    }

    public void setPostTask(String task) throws Exception {
        postTask = task;
    }

    public int getCapType() {
        return capType;
    }

    public void setCapType(int type) {
        capType = type;
    }

    public boolean equals(Object item) {
        ScheduleItem compTo = (ScheduleItem) item;
        if (compTo.getStart().getTime() == this.getStart().getTime() && compTo.getDuration() == this.getDuration() && compTo.getChannel().equalsIgnoreCase(this.getChannel())) {
            return true;
        }
        return false;
    }

    public int compareTo(Object comTo) {
        if (comTo == null) return -1;
        long comStart = ((ScheduleItem) comTo).getStart().getTime();
        long thisStart = getStart().getTime();
        int result = 0;
        if (comStart > thisStart) result = -1; else if (comStart < thisStart) result = +1;
        return result;
    }

    public void setType(int ty) throws Exception {
        type = ty;
    }

    public int getType() {
        return type;
    }

    public boolean skipToNext() throws Exception {
        if (this.getState() != ScheduleItem.WAITING) return false;
        Calendar start = Calendar.getInstance();
        start.setTime(this.getStart());
        if (type == DAILY) {
            start.add(Calendar.DATE, 1);
        } else if (type == WEEKLY) {
            start.add(Calendar.DATE, 7);
        } else if (type == MONTHLY) {
            start.add(Calendar.MONTH, 1);
        } else if (type == WEEKDAY) {
            start.add(Calendar.DATE, 1);
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            while (dayOfWeek < Calendar.MONDAY || dayOfWeek > Calendar.FRIDAY) {
                start.add(Calendar.DATE, 1);
                dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            }
        }
        this.setStart(start);
        this.setState(ScheduleItem.WAITING);
        this.setStatus("Waiting");
        this.resetAbort();
        this.log("Skipping Item forward to " + start.getTime().toString());
        System.out.println("Skipping schedule Item forward to " + start.getTime().toString());
        return true;
    }

    public ScheduleItem createNextInstance(Calendar now, Random rand) throws Exception {
        Calendar start = Calendar.getInstance();
        start.setTime(this.getStart());
        if (type == DAILY) {
            while (start.before(now)) {
                start.add(Calendar.DATE, 1);
            }
        } else if (type == WEEKLY) {
            while (start.before(now)) {
                start.add(Calendar.DATE, 7);
            }
        } else if (type == MONTHLY) {
            while (start.before(now)) {
                start.add(Calendar.MONTH, 1);
            }
        } else if (type == WEEKDAY) {
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            while (start.before(now) || dayOfWeek < Calendar.MONDAY || dayOfWeek > Calendar.FRIDAY) {
                start.add(Calendar.DATE, 1);
                dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
            }
        }
        ScheduleItem newInstance = new ScheduleItem(rand.nextLong());
        this.copyToSchedule(newInstance);
        newInstance.setStart(start);
        newInstance.setState(ScheduleItem.WAITING);
        newInstance.setStatus("Waiting");
        newInstance.resetAbort();
        newInstance.log("Repeating schedule item re-created (" + newInstance.getType() + ") : " + start.getTime().toString());
        System.out.println("Repeating schedule item re-created (" + newInstance.getType() + ") : " + start.getTime().toString());
        this.setType(ScheduleItem.ONCE);
        return newInstance;
    }

    public void setName(String name) throws Exception {
        fileName = checkFileName(name);
    }

    public void setCreatedFrom(GuideItem item) throws Exception {
        if (item != null) createdFrom = item.clone(); else createdFrom = null;
    }

    public GuideItem getCreatedFrom() {
        return createdFrom;
    }

    public String getName() {
        return fileName;
    }

    public String getFileName() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.getStart());
        String fileName = this.getFilePattern();
        fileName = fileName.replaceAll("%y", addZero(cal.get(Calendar.YEAR)));
        fileName = fileName.replaceAll("%m", addZero((cal.get(Calendar.MONTH) + 1)));
        fileName = fileName.replaceAll("%d", addZero(cal.get(Calendar.DATE)));
        fileName = fileName.replaceAll("%h", addZero(cal.get(Calendar.HOUR_OF_DAY)));
        fileName = fileName.replaceAll("%M", addZero(cal.get(Calendar.MINUTE)));
        String dayOfWeek = "";
        try {
            dayOfWeek = (String) DataStore.getInstance().dayName.get(new Integer(cal.get(Calendar.DAY_OF_WEEK)));
        } catch (Exception e) {
        }
        fileName = fileName.replaceAll("%D", dayOfWeek);
        if (this.getName().length() > 0) fileName = fileName.replaceAll("%n", checkFileName(this.getName())); else fileName = fileName.replaceAll("%n", "");
        if (createdFrom != null) {
            fileName = fileName.replaceAll("%s", checkFileName(createdFrom.getSubName()));
            String allCats = "";
            for (int x = 0; x < createdFrom.getCategory().size(); x++) {
                allCats += createdFrom.getCategory().get(x);
                if (x < createdFrom.getCategory().size() - 1) allCats += "-";
            }
            fileName = fileName.replaceAll("%t", checkFileName(allCats));
        } else {
            fileName = fileName.replaceAll("%s", "");
            fileName = fileName.replaceAll("%t", "");
        }
        fileName = fileName.replaceAll("%c", checkFileName(this.getChannel()));
        fileName = fileName.replaceAll("(\\ )+", " ");
        String finalName = fileName.trim();
        return finalName;
    }

    public void abort() {
        abort = true;
    }

    public boolean isAborted() {
        return abort;
    }

    public void resetAbort() {
        abort = false;
    }

    public void setDuration(int dur) throws Exception {
        duration = dur;
        if (startTime != null) {
            Calendar stop = Calendar.getInstance();
            stop.setTime(startTime);
            stop.add(Calendar.MINUTE, duration);
            stopTime = stop.getTime();
        }
    }

    public int getDuration() {
        return duration;
    }

    public Date getStop() {
        return stopTime;
    }

    public void setChannel(String ch) throws Exception {
        channel = ch;
    }

    public String getChannel() {
        return channel;
    }

    public void setState(int st) {
        state = st;
    }

    public int getState() {
        return state;
    }

    public void setStatus(String s) {
        status = s;
    }

    public String getStatus() {
        return status;
    }

    public void setStart(Calendar cal) throws Exception {
        startTime = cal.getTime();
        if (duration > 0) {
            Calendar stop = Calendar.getInstance();
            stop.setTime(startTime);
            stop.add(Calendar.MINUTE, duration);
            stopTime = stop.getTime();
        }
    }

    public Date getStart() {
        return startTime;
    }

    public String toString() {
        return new Long(id).toString();
    }

    public void log(String mes) {
        log.append(df.format(new Date()) + " : " + mes + "\n");
    }

    public String getLog() {
        return log.toString();
    }

    private String addZero(int input) {
        if (input < 10) return (String) ("0" + input); else return (new Integer(input)).toString();
    }

    private String checkFileName(String name) {
        name = name.trim();
        StringBuffer finalName = null;
        try {
            finalName = new StringBuffer(256);
            for (int x = 0; x < name.length(); x++) {
                char charAt = name.charAt(x);
                if (charAt >= 'a' && charAt <= 'z' || charAt >= 'A' && charAt <= 'Z' || charAt >= '0' && charAt <= '9' || charAt == ' ') finalName.append(charAt); else finalName.append('-');
            }
        } catch (Exception e) {
            name = "error";
        }
        return finalName.toString();
    }

    public boolean isOverlapping(ScheduleItem checkItem) {
        if (checkItem.getStart().getTime() >= this.getStart().getTime() && checkItem.getStart().getTime() < this.getStop().getTime()) return true;
        if (checkItem.getStop().getTime() > this.getStart().getTime() && checkItem.getStop().getTime() <= this.getStop().getTime()) return true;
        if (checkItem.getStart().getTime() <= this.getStart().getTime() && checkItem.getStop().getTime() >= this.getStop().getTime()) return true;
        return false;
    }

    public boolean isAutoDeletable() {
        return autoDeleteable;
    }

    public void setAutoDeletable(boolean del) throws Exception {
        autoDeleteable = del;
    }

    public String getFilePattern() {
        return fileNamePattern;
    }

    public void setFilePattern(String pat) throws Exception {
        fileNamePattern = pat;
    }

    public void setKeepFor(int keep) throws Exception {
        keepFor = keep;
    }

    public int getKeepFor() {
        return keepFor;
    }

    public void copyToSchedule(ScheduleItem to) throws Exception {
        Calendar start = Calendar.getInstance();
        start.setTime(this.getStart());
        to.setStart(start);
        to.setDuration(this.getDuration());
        to.setCapType(this.getCapType());
        to.setType(this.getType());
        to.setPostTask(this.getPostTask());
        to.setChannel(this.getChannel());
        to.setFilePattern(this.getFilePattern());
        to.setName(this.getName());
        to.setCreatedFrom(this.getCreatedFrom());
        to.setKeepFor(this.getKeepFor());
        to.setAutoDeletable(this.isAutoDeletable());
        to.setState(this.getState());
        to.setStatus(this.getStatus());
        to.setCapturePathIndex(this.getCapturePathIndex());
        if (this.isAborted()) to.abort();
    }
}
