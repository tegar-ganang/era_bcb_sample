import java.util.*;
import java.io.*;
import java.text.*;

public class TimerThread implements Runnable {

    private DataStore store = null;

    private Calendar now = Calendar.getInstance();

    private Calendar start = Calendar.getInstance();

    private Calendar stop = Calendar.getInstance();

    private HashMap<String, CaptureTask> captureTasks = new HashMap<String, CaptureTask>();

    public TimerThread() throws Exception {
        System.out.println("Timer Thread: Created");
        store = DataStore.getInstance();
    }

    public void run() {
        System.out.println("Timer Thread: Started");
        ThreadLock locker = ThreadLock.getInstance();
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        try {
            while (true) {
                store.timerStatus = 1;
                locker.getLock();
                try {
                    StreamProducerProcess[] producers = devList.getProducers();
                    for (int index = 0; index < producers.length; index++) {
                        StreamProducerProcess producer = producers[index];
                        if (producer.isProducerRunning() == false) {
                            System.out.println("Producer with KEY=" + producer.getKey() + " has been marked for needs restart");
                            producer.setNeedsRestart(true);
                        }
                    }
                    store.timerStatus = 2;
                    String[] keys = (String[]) captureTasks.keySet().toArray(new String[0]);
                    for (int index = 0; index < keys.length; index++) {
                        CaptureTask task = (CaptureTask) captureTasks.get(keys[index]);
                        boolean fin = task.isFinished();
                        if (fin) {
                            store.timerStatus = 3;
                            task.stopCapture();
                            store.timerStatus = 4;
                            captureTasks.remove(keys[index]);
                            NowRunningInfo update = new NowRunningInfo(captureTasks);
                            update.writeNowRunning();
                            new DllWrapper().setActiveCount(captureTasks.size());
                            System.gc();
                        }
                    }
                    now.setTime(new Date());
                    long autoDelSchedTime = 0;
                    try {
                        autoDelSchedTime = 3600000 * Integer.parseInt(store.getProperty("sch.autodel.time"));
                    } catch (Exception exp) {
                    }
                    keys = store.getScheduleKeys();
                    Arrays.sort(keys);
                    for (int x = 0; x < keys.length; x++) {
                        ScheduleItem item = store.getScheduleItem(keys[x]);
                        if (item == null) {
                            System.out.println("ERROR: for some reason one of your schedule items in the MAP is null : " + keys[x]);
                            break;
                        }
                        if (captureTasks.keySet().contains(item.toString()) == false) {
                            start.setTime(item.getStart());
                            stop.setTime(item.getStop());
                            if (item.getState() == ScheduleItem.WAITING && now.after(stop)) {
                                item.setState(ScheduleItem.SKIPPED);
                                item.setStatus("skipped");
                                item.log("Marked as skipped");
                                store.saveSchedule(null);
                            } else if (autoDelSchedTime > 0 && item.getState() == ScheduleItem.FINISHED && (now.getTimeInMillis() - stop.getTimeInMillis()) > autoDelSchedTime) {
                                store.autoDelLogAdd("Auto-Deleting Finished Item: (" + item.getName() + ") (" + item.getStart() + ") (" + item.getDuration() + ")");
                                System.out.println("Removing Old Finished Item: " + keys[x]);
                                ScheduleItem removedItem = store.removeScheduleItem(keys[x]);
                                store.saveSchedule(null);
                                int autoDelSchedAction = 0;
                                try {
                                    autoDelSchedAction = Integer.parseInt(store.getProperty("sch.autodel.action"));
                                } catch (Exception e) {
                                }
                                if (autoDelSchedAction == 0 && removedItem != null) {
                                    archiveOldItem(removedItem);
                                    System.out.println("Item Archived");
                                }
                            } else if ((item.getType() == ScheduleItem.DAILY || item.getType() == ScheduleItem.WEEKLY || item.getType() == ScheduleItem.WEEKDAY || item.getType() == ScheduleItem.MONTHLY) && (item.getState() == ScheduleItem.FINISHED || item.getState() == ScheduleItem.SKIPPED)) {
                                Vector<ScheduleItem> nextInstances = new Vector<ScheduleItem>();
                                nextInstances.add(item.createNextInstance(now, store.rand));
                                for (int z = 0; z < nextInstances.size(); z++) {
                                    ScheduleItem nextInstance = (ScheduleItem) nextInstances.get(z);
                                    store.addScheduleItem(nextInstance);
                                }
                            } else if (item.getState() == ScheduleItem.RUNNING) {
                                item.setState(ScheduleItem.WAITING);
                                item.setStatus("Reset");
                                item.log("Running Item marked as Waiting, this should not happen, WS probably crashed.");
                                store.saveSchedule(null);
                            } else if (item.getState() == ScheduleItem.ABORTED) {
                                item.setState(ScheduleItem.FINISHED);
                                item.setStatus("Reset");
                                item.log("Aborted Item marked as Finished, this should not happen.");
                                store.saveSchedule(null);
                            } else if (item.getState() == ScheduleItem.RESTART) {
                                item.setState(ScheduleItem.WAITING);
                                item.setStatus("Restarted");
                                item.log("Item Restarted and Set to WAITING by Timer Thread, this should not happen.");
                                store.saveSchedule(null);
                            } else if (item.getState() == ScheduleItem.WAITING && now.after(start) && now.before(stop) && item.isDealyed() == false) {
                                try {
                                    System.gc();
                                    store.refreshWakeupTime();
                                    System.out.println("Number of cards available (" + devList.getDeviceCount() + ") number in use (" + devList.getActiveDeviceCount() + ")");
                                    CaptureTask capTask = new CaptureTask(item);
                                    item.setState(ScheduleItem.RUNNING);
                                    item.setStatus("Starting");
                                    store.timerStatus = 7;
                                    int startCode = capTask.startCapture();
                                    store.timerStatus = 8;
                                    if (startCode < 0) {
                                        int delayFor = 60;
                                        try {
                                            delayFor = Integer.parseInt(store.getProperty("capture.capturefailedtimeout"));
                                            if (delayFor < 60) delayFor = 60;
                                            if (delayFor > 240) delayFor = 240;
                                        } catch (Exception e) {
                                        }
                                        item.delayFor(delayFor);
                                        item.setStatus("Error Delay");
                                        item.log("Schedule failed to start, it will be delayed for " + delayFor + " seconds and then retried.");
                                        if (startCode == -3) {
                                            System.out.println("No cards available, Will retry this schedule in " + delayFor + " seconds.");
                                        } else {
                                            System.out.println("Error starting capture : (" + startCode + ") " + "Will retry this schedule in " + delayFor + " seconds.");
                                        }
                                    } else {
                                        item.setState(ScheduleItem.RUNNING);
                                        item.setStatus("Running");
                                        item.log("Capture Started");
                                        captureTasks.put(item.toString(), capTask);
                                        NowRunningInfo update = new NowRunningInfo(captureTasks);
                                        update.writeNowRunning();
                                        new DllWrapper().setActiveCount(captureTasks.size());
                                    }
                                    store.saveSchedule(null);
                                } catch (Exception e) {
                                    System.out.println("Error running the action task: " + e);
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                } finally {
                    locker.releaseLock();
                }
                if ("1".equals(store.getProperty("server.kbled"))) {
                    new DllWrapper().setKbLEDs(captureTasks.size());
                }
                store.timerStatus = 0;
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            System.out.println("The main Timer Thread has crashed!");
            e.printStackTrace();
            System.out.println("This is really bad!!!!!");
            store.timerStatus = -1;
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            PrintWriter err = new PrintWriter(ba);
            e.printStackTrace(err);
            err.flush();
            store.timerThreadErrorStack = ba.toString();
        }
    }

    private void archiveOldItem(ScheduleItem removedItem) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd@HHmmssS");
            String archiveName = new DllWrapper().getAllUserPath() + "archive\\Schedule-" + df.format(removedItem.getStart()) + " (" + removedItem.getChannel() + ") (" + removedItem.getName() + ").sof";
            File outFile = new File(archiveName);
            outFile = outFile.getCanonicalFile();
            File parent = outFile.getParentFile();
            if (parent.exists() == false) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(outFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(removedItem);
            oos.close();
        } catch (Exception e) {
            System.out.println("Error trying to archive old Schedule Item:");
            e.printStackTrace();
        }
    }
}
