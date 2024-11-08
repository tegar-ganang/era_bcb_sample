import java.util.*;
import java.io.*;
import java.text.*;

class CaptureTask {

    private ScheduleItem item = null;

    private DllWrapper utils = null;

    private DataStore store = null;

    private Calendar now = Calendar.getInstance();

    private Calendar start = Calendar.getInstance();

    private Calendar stop = Calendar.getInstance();

    private String capFileName = null;

    private StreamProducerProcess producer = null;

    private StreamConsumerProcess consumer = null;

    private int minSpaceHard = 200;

    private DllWrapper wrapper = new DllWrapper();

    public CaptureTask(ScheduleItem it) throws Exception {
        store = DataStore.getInstance();
        item = it;
        try {
            minSpaceHard = Integer.parseInt(store.getProperty("capture.minspacehard"));
        } catch (Exception e) {
        }
    }

    public ScheduleItem getScheduleItem() {
        return item;
    }

    public int startCapture() {
        utils = new DllWrapper();
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        capFileName = getFullFileName();
        start.setTime(item.getStart());
        stop.setTime(item.getStop());
        File capFileParent = new File(capFileName).getParentFile();
        System.out.println(this + " : " + capFileParent.getAbsolutePath());
        if (capFileParent.exists() == false || capFileParent.isDirectory() == false) {
            item.setState(ScheduleItem.ERROR);
            item.setStatus("Error");
            item.log("The capture directory (" + capFileParent.getAbsolutePath() + ") does not exist, can not capture");
            System.out.println(this + " : The capture directory (" + capFileParent.getAbsolutePath() + ") does not exist, can not capture");
            item.addWarning("Directory does not exist");
            return -1;
        }
        Channel ch = store.getChannel(item.getChannel());
        if (ch == null) {
            item.setState(ScheduleItem.ERROR);
            item.setStatus("Error");
            item.log("Channel object was not found in Channel List");
            System.out.println(this + " : Channel object was not found in ScheduleItem");
            item.addWarning("Channel not found");
            return -2;
        }
        item.log("Channel data loaded from Item : " + ch.getName());
        int captureType = item.getCapType();
        if (captureType == -1) {
            captureType = ch.getCaptureType();
        }
        if (captureType == -1) {
            try {
                captureType = Integer.parseInt(store.getProperty("capture.deftype"));
            } catch (Exception e) {
                item.log("Error getting global capture type (" + store.getProperty("capture.deftype") + ")");
                System.out.println(this + " : Error getting global capture type (" + store.getProperty("capture.deftype") + ")");
                e.printStackTrace();
            }
        }
        System.out.println(this + " : About to start capture");
        item.log("File Name : " + new File(capFileName).getName());
        item.log("File Path : " + new File(capFileName).getParent());
        if (item.getCreatedFrom() != null) {
            item.log("EPG Data...");
            item.log("Title : " + item.getCreatedFrom().getName());
            item.log("Sub Title : " + item.getCreatedFrom().getSubName());
            item.log("Start : " + item.getCreatedFrom().getStart().toString());
            item.log("Duration : " + item.getCreatedFrom().getDuration());
        }
        item.log("Frequency : " + ch.getFrequency());
        item.log("Bandwidth : " + ch.getBandWidth());
        item.log("Prog Pid  : " + ch.getProgramID());
        item.log("Video Pid : " + ch.getVideoPid());
        item.log("Audio Pid : " + ch.getAudioPid());
        item.log("Cap Type  : " + captureType);
        producer = null;
        producer = devList.getProducer(ch.getFrequency(), ch.getBandWidth());
        if (producer != null) {
            System.out.println(this + " : Using existing producer, device " + producer.getDeviceIndex() + ":" + producer.getCaptureDevice().getName());
            item.log("Using existing producer, device " + producer.getDeviceIndex() + ":" + producer.getCaptureDevice().getName());
        } else {
            if (devList.getFreeDevice() == -1) {
                item.setState(ScheduleItem.WAITING);
                item.addWarning("No free device");
                System.out.println(this + " : No free device to start producer on");
                return -3;
            }
            StringBuffer producerLog = new StringBuffer();
            int startCode = -1;
            for (int x = 0; x < devList.getDeviceCount(); x++) {
                CaptureDevice cap = devList.getDevice(x);
                if (cap.isInUse() == false) {
                    runPreTask(x, cap.getID());
                    System.out.println(this + " : Starting producer, device " + x + ":" + cap.getName());
                    item.log("Starting producer, device " + x + ":" + cap.getName());
                    producer = null;
                    producer = new StreamProducerProcess(cap, x);
                    startCode = producer.startProducer(ch.getFrequency(), ch.getBandWidth(), producerLog, item.getLogFileNames());
                    if (startCode == 0) {
                        cap.setInUse(true);
                        break;
                    }
                    System.out.println(this + " : Producer start failed with code: " + startCode);
                    item.log("Producer start failed with code: " + startCode);
                    item.addWarning("Producer start failed");
                    runStartErrorTask(x, cap.getID());
                }
            }
            if (startCode != 0) {
                item.setStatus("Waiting for retry");
                item.log("No free capture device, Waiting for retry");
                System.out.println(this + " : No free capture device, Waiting for retry");
                item.setState(ScheduleItem.WAITING);
                item.addWarning("No free device");
                return -3;
            }
            System.out.println(this + " : Producer Started.");
            item.log("Producer Started.");
            item.log("Producer Start Log:\n\n" + producerLog.toString());
        }
        System.out.println(this + " : Starting consumer process");
        item.log("Starting consumer process");
        consumer = null;
        consumer = new StreamConsumerProcess();
        StringBuffer consumerLog = new StringBuffer();
        int consumerStartCode = -1;
        consumerStartCode = consumer.startConsumer(producer.getMemoryShareName(), ch.getProgramID(), ch.getVideoPid(), ch.getAudioPid(), ch.getAudioType(), captureType, capFileName, consumerLog, item.getLogFileNames());
        if (consumerStartCode == 0) {
            producer.addUsageCount();
            devList.addProducer(producer, ch.getFrequency(), ch.getBandWidth());
            System.out.println(this + " : Capture started.");
            item.log("Capture started.");
            item.log("Start Log:\n\n" + consumerLog.toString());
            new CaptureDetails().writeCaptureDetails(this);
            return producer.getDeviceIndex();
        } else {
            item.log("Start Log:\n\n" + consumerLog.toString());
            item.log("Consumer process start failed with code : " + consumerStartCode);
            item.setStatus("Waiting for retry");
            item.setState(ScheduleItem.WAITING);
            item.addWarning("Consumer did not start");
            deleteCapture(120000000);
            if (consumerStartCode == -104) {
                item.log("Consumer build graph failed, defaulting to TS-Mux capture type");
                item.addWarning("Capture Type Reset (TS-Mux)");
                item.setCapType(2);
            }
            if (consumerStartCode == -106) {
                item.log("PID's not found, defaulting to Full TS capture trye");
                item.addWarning("Capture Type Reset (Full-TS)");
                item.setCapType(0);
            }
            int usageCount = producer.getUsageCount();
            if (usageCount == 0) {
                if (producer.isProducerRunning()) {
                    int stopCode = producer.stopProducer();
                    Integer exitCode = producer.getExitCode();
                    item.log("Producer Stopped(" + stopCode + ") with exit code:" + exitCode);
                    System.out.println(this + " : Producer Stopped(" + stopCode + ") with exit code:" + exitCode);
                } else {
                    Integer exitCode = producer.getExitCode();
                    item.log("Producer already not running, it exited with:" + exitCode);
                    System.out.println(this + " : Producer already not running, it exited with:" + exitCode);
                }
                producer.getCaptureDevice().setInUse(false);
                devList.remProducer(producer.getKey());
                producer = null;
            } else {
                item.log("Producer not stopped, inUseCount(" + usageCount + ")");
                System.out.println(this + " : Producer not stopped, inUseCount(" + usageCount + ")");
            }
            return -4;
        }
    }

    public boolean isFinished() {
        long totalTime = stop.getTimeInMillis() - start.getTimeInMillis();
        long perDone = (long) (((double) (now.getTimeInMillis() - start.getTimeInMillis()) / (double) totalTime) * 100.00);
        int leftToGo = (int) ((stop.getTimeInMillis() - now.getTimeInMillis()) / (1000 * 60));
        Vector<String> producerResponceData = producer.getResponceData();
        while (producerResponceData.size() > 0) {
            String data = producerResponceData.remove(0);
            if (data.startsWith("LOG_FILE:")) {
                String logName = data.substring("LOG_FILE:".length()).trim();
                System.out.println("Producer Log File " + logName);
                Vector<String> logFilesNames = item.getLogFileNames();
                logFilesNames.add(logName);
            }
        }
        Vector<String> consumerResponceData = consumer.getResponceData();
        while (consumerResponceData.size() > 0) {
            String data = consumerResponceData.remove(0);
            if (data.startsWith("LOG_FILE:")) {
                String logName = data.substring("LOG_FILE:".length()).trim();
                System.out.println("Consumer Log File " + logName);
                Vector<String> logFilesNames = item.getLogFileNames();
                logFilesNames.add(logName);
            }
            if (data.startsWith("LOG:")) {
                String logString = data.substring("LOG:".length()).trim();
                item.log(logString);
            }
            if (data.startsWith("WARNING:")) {
                String warningString = data.substring("WARNING:".length()).trim();
                item.log(warningString);
                item.addWarning(warningString);
            }
            if (data.startsWith("SIGNAL_DATA:")) {
                item.addSignalStatistic(new SignalStatistic(data));
            }
        }
        if (producer.getNeedsRestart() == true) {
            System.out.println(this + " : The producer for this consumer needs to be restarted producer exit code : " + producer.getExitCode());
            item.setState(ScheduleItem.RESTART);
            item.setStatus("Restarting");
            item.log("Producer needs restarting.");
            if (producer.getExitCode().intValue() == -7) {
                item.log("Producer (No Data Flowing)");
                item.addWarning("Producer (No Data Flowing)");
            } else {
                item.log("Producer (Exited:" + producer.getExitCode() + ")");
                item.addWarning("Producer (Exited:" + producer.getExitCode() + ")");
            }
            return true;
        }
        if (consumer.isConsumerRunning() == false) {
            System.out.println(this + " : Consumer not running, will restart, exit code: " + consumer.getExitCode());
            item.setState(ScheduleItem.RESTART);
            item.setStatus("Restarting");
            item.log("Consumer not running, will restart, exit code: " + consumer.getExitCode());
            Integer exitCode = consumer.getExitCode();
            if (exitCode != null && exitCode.intValue() == -7) {
                item.log("No Data Flowing Detected");
                item.addWarning("No data flowing");
            } else item.addWarning("Consumer not running");
            return true;
        }
        File capFile = new File(capFileName);
        long freeSpace = wrapper.getFreeSpace(capFile.getParent()) / (1024 * 1024);
        if (freeSpace < minSpaceHard) {
            item.setState(ScheduleItem.RESTART);
            item.setStatus("Restarting");
            item.log("Minimum free space (hard) limit hit free:" + freeSpace + " required:" + minSpaceHard + " on:" + capFileName);
            System.out.println(this + " : Minimum free space (hard) limit hit free:" + freeSpace + " required:" + minSpaceHard + " on:" + capFileName);
            item.addWarning("Minimum free space limit hit");
            return true;
        }
        now.setTime(new Date());
        start.setTime(item.getStart());
        stop.setTime(item.getStop());
        if (item.isAborted()) {
            item.log("Action Aborted By User");
            return true;
        }
        if (now.after(stop)) {
            item.log("Action Finished");
            return true;
        }
        item.setStatus("Running " + perDone + "% (" + leftToGo + "min)");
        return false;
    }

    public void stopCapture() {
        if (consumer.isConsumerRunning()) {
            int stopCode = consumer.stopConsumer();
            Integer exitCode = consumer.getExitCode();
            item.log("Consumer Stopped(" + stopCode + ") with exit code:" + exitCode);
            System.out.println(this + " : Consumer Stopped (" + stopCode + ") with exit code:" + exitCode);
        } else {
            Integer exitCode = consumer.getExitCode();
            item.log("Consumer already not running, it exited with:" + exitCode);
            System.out.println(this + " : Consumer already not running, it exited with:" + exitCode);
        }
        producer.decUsageCount();
        int deviceID = producer.getDeviceIndex();
        String deviceString = producer.getCaptureDevice().getID();
        int usageCount = producer.getUsageCount();
        if (usageCount == 0) {
            if (producer.isProducerRunning()) {
                int stopCode = producer.stopProducer();
                Integer exitCode = producer.getExitCode();
                item.log("Producer Stopped(" + stopCode + ") with exit code:" + exitCode);
                System.out.println(this + " : Producer Stopped(" + stopCode + ") with exit code:" + exitCode);
            } else {
                Integer exitCode = producer.getExitCode();
                item.log("Producer already not running, it exited with:" + exitCode);
                System.out.println(this + " : Producer already not running, it exited with:" + exitCode);
                if (exitCode.intValue() == -7) {
                    item.log("Producer return code was for No Data Flowing.");
                    System.out.println(this + " : Return code was for No Data Flowing.");
                    runNoDataErrorTask(producer.getDeviceIndex(), producer.getCaptureDevice().getID());
                }
            }
            producer.getCaptureDevice().setInUse(false);
            CaptureDeviceList devList = CaptureDeviceList.getInstance();
            devList.remProducer(producer.getKey());
            producer = null;
        } else {
            item.log("Producer not stopped, inUseCount(" + usageCount + ")");
            System.out.println(this + " : Producer not stopped, inUseCount(" + usageCount + ")");
        }
        if (item.getState() == ScheduleItem.RESTART) {
            item.setState(ScheduleItem.WAITING);
            item.setStatus("Waiting");
            item.log("Schedule Item Restarted and Set to WAITING");
            store.saveSchedule(null);
            boolean deleted = deleteCapture(10000);
            if (deleted == false) item.addCaptureFile(new File(capFileName));
            return;
        }
        item.setState(ScheduleItem.FINISHED);
        item.setStatus("Finished");
        try {
            HashMap<Date, SignalStatistic> stats = item.getSignalStatistics();
            Date[] keys = stats.keySet().toArray(new Date[0]);
            Arrays.sort(keys);
            int strengthMIN = -1;
            int strengthAVG = 0;
            int strengthMAX = -1;
            int qualityMIN = -1;
            int qualityAVG = 0;
            int qualityMAX = -1;
            for (int x = 0; x < keys.length; x++) {
                SignalStatistic value = stats.get(keys[x]);
                if (strengthMIN == -1 || value.getStrength() < strengthMIN) strengthMIN = value.getStrength();
                if (strengthMAX == -1 || value.getStrength() > strengthMAX) strengthMAX = value.getStrength();
                if (qualityMIN == -1 || value.getQuality() < qualityMIN) qualityMIN = value.getQuality();
                if (qualityMAX == -1 || value.getQuality() > qualityMAX) qualityMAX = value.getQuality();
                strengthAVG += value.getStrength();
                qualityAVG += value.getQuality();
            }
            if (keys.length > 0) {
                strengthAVG /= keys.length;
                qualityAVG /= keys.length;
            }
            item.log("Signal Strength (" + strengthMIN + ", " + strengthAVG + ", " + strengthMAX + ")");
            item.log("Signal Quality (" + qualityMIN + ", " + qualityAVG + ", " + qualityMAX + ")");
        } catch (Exception e) {
            item.log("Error getting signal stats!");
        }
        NumberFormat nf = NumberFormat.getInstance();
        File capFile = new File(capFileName);
        item.log("Capture : " + capFile.getName() + " (" + nf.format(capFile.length() / (1024 * 1024)) + " MB)");
        long freeSpace = utils.getFreeSpace(capFile.getAbsoluteFile().getParent());
        item.log("Free Space : (" + nf.format(freeSpace / (1024 * 1024)) + " MB)");
        boolean deleted = deleteCapture(0);
        if (deleted == false) item.addCaptureFile(new File(capFileName));
        if (item.isAutoDeletable()) {
            store.addAutoDeleteItem(capFileName, item.getKeepFor());
            store.saveAutoDelList();
        }
        store.saveSchedule(null);
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        TaskCommand taskCommand = (TaskCommand) tasks.get(item.getPostTask());
        if (taskCommand != null) {
            if (item.getPostTaskEnabled()) {
                File taskCapFile = new File(capFileName);
                runTask(taskCommand, false, deviceID, deviceString, taskCapFile);
            } else {
                item.log("Skipping Post Capture Task.");
            }
        }
        item.log("Capture Finished");
        sendEmail();
    }

    private void sendEmail() {
        String sendServerStarted = store.getProperty("email.send.capfinished");
        if ("1".equals(sendServerStarted) == false) return;
        StringBuffer buff = new StringBuffer(2048);
        buff.append("Schedule Item Log:\n" + item.getLog());
        EmailSender sender = new EmailSender();
        sender.setSubject("TV Scheduler Pro Capture Finished");
        sender.setBody("Following are the results of your TV Scheduler Pro capture:\n\n" + buff.toString());
        try {
            Thread mailThread = new Thread(Thread.currentThread().getThreadGroup(), sender, sender.getClass().getName());
            mailThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runNoDataErrorTask(int deviceID, String deviceString) {
        String preTaskName = store.getProperty("tasks.nodataerrortask");
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        TaskCommand taskCommand = (TaskCommand) tasks.get(preTaskName);
        if (taskCommand == null) {
            return;
        }
        runTask(taskCommand, true, deviceID, deviceString, null);
    }

    private void runStartErrorTask(int deviceID, String deviceString) {
        String preTaskName = store.getProperty("tasks.starterrortask");
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        TaskCommand taskCommand = (TaskCommand) tasks.get(preTaskName);
        if (taskCommand == null) {
            return;
        }
        runTask(taskCommand, true, deviceID, deviceString, null);
    }

    private void runPreTask(int deviceID, String deviceString) {
        String preTaskName = store.getProperty("tasks.pretask");
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        TaskCommand taskCommand = (TaskCommand) tasks.get(preTaskName);
        if (taskCommand == null) {
            return;
        }
        runTask(taskCommand, true, deviceID, deviceString, null);
    }

    private void runTask(TaskCommand taskCommand, boolean waitForExit, int deviceID, String deviceString, File capFile) {
        String fileName = "";
        if (capFile != null) fileName = capFile.getName();
        item.log("Running task (" + taskCommand.getName() + ") on file (" + fileName + ")");
        System.out.println(this + " : Running task (" + taskCommand.getName() + ") on file (" + fileName + ")");
        if (taskCommand.getEnabled() == false) {
            item.log("Task (" + taskCommand.getName() + ") is disabled.");
            System.out.println(this + " : Task (" + taskCommand.getName() + ") is disabled.");
            return;
        }
        String command = taskCommand.getCommand();
        if (command != null && command.length() > 1) {
            int index = command.indexOf("$deviceID");
            if (index > -1) {
                StringBuffer buff = new StringBuffer(command);
                buff.replace(index, index + "$deviceID".length(), new Integer(deviceID).toString());
                command = buff.toString();
            }
            command.replaceAll("$deviceString", deviceString);
            index = command.indexOf("$deviceString");
            if (index > -1) {
                StringBuffer buff = new StringBuffer(command);
                buff.replace(index, index + "$deviceString".length(), deviceString);
                command = buff.toString();
            }
            index = command.indexOf("$filename");
            if (index > -1 && capFile != null) {
                String fullPath = capFile.getPath();
                try {
                    fullPath = capFile.getCanonicalPath();
                } catch (Exception e) {
                }
                StringBuffer buff = new StringBuffer(command);
                buff.replace(index, index + "$filename".length(), fullPath);
                command = buff.toString();
            }
            index = command.indexOf("$capType");
            if (index > -1) {
                int captureType = item.getCapType();
                if (captureType == -1) {
                    Channel ch = store.getChannel(item.getChannel());
                    if (ch != null) captureType = ch.getCaptureType();
                }
                if (captureType == -1) {
                    try {
                        captureType = Integer.parseInt(store.getProperty("capture.deftype"));
                    } catch (Exception e) {
                    }
                }
                String capType = new Integer(captureType).toString();
                StringBuffer buff = new StringBuffer(command);
                buff.replace(index, index + "$capType".length(), capType);
                command = buff.toString();
            }
            try {
                runCommand(taskCommand, command, item, waitForExit, capFile);
            } catch (Exception e) {
                item.log("There was an Exception thrown when trying to run the task.");
                e.printStackTrace();
            }
        }
    }

    private int runCommand(TaskCommand taskCommand, String command, ScheduleItem item, boolean wait, File target) throws Exception {
        long timeout = 20;
        if (taskCommand.getDelay() > 0) {
            item.log("The command has a delayed start of " + taskCommand.getDelay() + " seconds");
            System.out.println(this + " : The command has a delayed start of " + taskCommand.getDelay() + " seconds");
        }
        TaskItemThread taskItem = new TaskItemThread(taskCommand, new CommandWaitThread(command), target);
        Thread taskThread = new Thread(Thread.currentThread().getThreadGroup(), taskItem, taskItem.getClass().getName());
        taskThread.start();
        if (wait == false) {
            return 1;
        }
        item.log("Will wait for " + timeout + " seconds for it to complete");
        System.out.println(this + " : Will wait for " + timeout + " seconds for it to complete");
        long started = new Date().getTime();
        while (true) {
            if (taskItem.isFinished()) {
                item.log("Command finished normally");
                System.out.println(this + " : Command finished normally");
                break;
            } else if (((new Date().getTime()) - started) > (1000 * timeout)) {
                item.log("Command timed out, killing it now");
                System.out.println(this + " : Command timed out, killing it now");
                taskItem.stop();
                break;
            } else if (item.isAborted()) {
                item.log("Action Aborted so Killing off Command");
                System.out.println(this + " : Action Aborted so Killing off Command");
                taskItem.stop();
                break;
            }
            System.out.println(this + " : Waiting for command to finish.....");
            Thread.sleep(1000);
        }
        item.log("Task finished, Output of task follows:");
        item.log("*****************************");
        System.out.println(this + " : Pre Task finished, Output of task follows");
        System.out.println(this + " : *****************************");
        item.log("Standard Out:\n" + taskItem.getOutput());
        System.out.println(this + " : Standard Out:\n" + taskItem.getOutput());
        item.log("Standard Error:\n" + taskItem.getError());
        System.out.println(this + " : Standard Error:\n" + taskItem.getError());
        item.log("*****************************");
        System.out.println(this + " : *****************************");
        item.log("Task Command Finished");
        return 0;
    }

    public String getFullFileName() {
        String extension = ".bin";
        int captureType = item.getCapType();
        if (captureType == -1) {
            Channel ch = store.getChannel(item.getChannel());
            if (ch != null) captureType = ch.getCaptureType();
        }
        if (captureType == -1) {
            try {
                captureType = Integer.parseInt(store.getProperty("capture.deftype"));
            } catch (Exception e) {
            }
        }
        CaptureCapability capability = CaptureCapabilities.getInstance().getCapabiltyWithID(captureType);
        if (capability != null) extension = "." + capability.getFileExt();
        long avDataRateSec = 7000000;
        try {
            avDataRateSec = Long.parseLong(store.getProperty("capture.averagedatarate").trim());
        } catch (Exception e) {
        }
        avDataRateSec = (long) (((double) (avDataRateSec * 60) / (double) 8) / (double) (1024 * 1024));
        boolean calculateUsage = "1".equals(store.getProperty("capture.includecalculatedusage").trim());
        if (calculateUsage) {
            item.log("Using Calculated Usage (" + avDataRateSec + " MB Minute)");
            System.out.println(this + " : Using Calculated Usage (" + avDataRateSec + " MB Minute)");
        } else {
            avDataRateSec = 0;
        }
        long estimatedUsage = item.getDuration() * avDataRateSec;
        String capturePath = determinCapturePath(item.getCapturePathIndex(), estimatedUsage) + File.separator;
        String fileName = item.getFileName();
        File dir = new File(capturePath + fileName + extension);
        int count = 1;
        while (dir.exists()) {
            String tempName = fileName + "-" + count++;
            dir = new File(capturePath + tempName + extension);
        }
        File parentPath = dir.getParentFile();
        if (!parentPath.exists()) {
            parentPath.mkdirs();
        }
        System.out.println(this + " : Capture File Name : " + dir.getAbsolutePath());
        return dir.getAbsolutePath();
    }

    private String determinCapturePath(int pathIndex, long sizeEstimation) {
        DllWrapper wrapper = new DllWrapper();
        String[] paths = store.getCapturePaths();
        int minSpaceSoft = 1200;
        try {
            minSpaceSoft = Integer.parseInt(store.getProperty("capture.minspacesoft"));
        } catch (Exception e) {
        }
        int autoSelectMethod = 0;
        try {
            autoSelectMethod = Integer.parseInt(store.getProperty("capture.autoselectmethod"));
        } catch (Exception e) {
        }
        try {
            if (pathIndex > -1 && pathIndex < paths.length) {
                String capPath = new File(paths[pathIndex]).getCanonicalPath();
                long freeSpace = wrapper.getFreeSpace(capPath) / (1024 * 1024);
                if (sizeEstimation > 0) item.log("Size Estimation (" + sizeEstimation + ")");
                if (freeSpace != 0 && ((freeSpace - sizeEstimation) > minSpaceSoft)) {
                    item.log("Using Path(" + pathIndex + ") " + capPath + " " + freeSpace);
                    System.out.println(this + " : Using Path(" + pathIndex + ") " + capPath + " " + freeSpace);
                    return capPath;
                } else {
                    item.log("Not enough free space(" + pathIndex + ") " + capPath + " free:" + freeSpace + " required:" + (minSpaceSoft + sizeEstimation));
                    System.out.println(this + " : Not enough free space(" + pathIndex + ") " + capPath + " free:" + freeSpace + " required:" + (minSpaceSoft + sizeEstimation));
                }
            }
            item.log("Doing path AutoSelect of type : " + autoSelectMethod);
            System.out.println(this + " : Doing path AutoSelect of type : " + autoSelectMethod);
            if (autoSelectMethod == 0) {
                int maxFreeID = -1;
                long prevMaxFree = -1;
                String capPath = null;
                for (int x = 0; x < paths.length; x++) {
                    capPath = new File(paths[x]).getCanonicalPath();
                    long freeSpace = wrapper.getFreeSpace(capPath) / (1024 * 1024);
                    if (freeSpace > prevMaxFree) {
                        prevMaxFree = freeSpace;
                        maxFreeID = x;
                    }
                }
                capPath = new File(paths[maxFreeID]).getCanonicalPath();
                item.log("Using Path(" + maxFreeID + ") " + capPath + " " + prevMaxFree);
                System.out.println(this + " : Using Path(" + maxFreeID + ") " + capPath + " " + prevMaxFree);
                return capPath;
            } else {
                for (int x = 0; x < paths.length; x++) {
                    String capPath = new File(paths[x]).getCanonicalPath();
                    long freeSpace = wrapper.getFreeSpace(capPath) / (1024 * 1024);
                    System.out.println(this + " : Size Estimations (" + sizeEstimation + ")");
                    if (freeSpace != 0 && ((freeSpace - sizeEstimation) > minSpaceSoft)) {
                        item.log("Using Path(" + x + ") " + capPath + " " + freeSpace);
                        System.out.println(this + " : Using Path(" + x + ") " + capPath + " " + freeSpace);
                        return capPath;
                    } else {
                        item.log("Not enough space(" + x + ") " + capPath + " " + freeSpace);
                        System.out.println(this + " : Not enough space(" + x + ") " + capPath + " " + freeSpace);
                    }
                }
                item.log("No path found with enough free space (" + minSpaceSoft + ")");
                System.out.println(this + " : No path found with enough free space (" + minSpaceSoft + ")");
            }
        } catch (Exception e) {
            item.log("Error determining capture path.");
            System.out.println(this + " : Error determining capture path.");
            e.printStackTrace();
        }
        if (paths.length > 0) {
            item.log("Using first capture path.");
            System.out.println(this + " : Using first capture path.");
            return paths[0];
        } else {
            item.log("No capture paths found, using hard coded.");
            System.out.println(this + " : No capture paths found, using hard coded.");
            return "capture";
        }
    }

    public boolean deleteCapture(long minSize) {
        File file = new File(capFileName);
        if (file.length() <= minSize) {
            item.log("Deleting failed capture file : " + file.getName());
            file.delete();
            return true;
        }
        return false;
    }

    public String getCurrentFileName() {
        return capFileName;
    }

    public String getDeviceID() {
        return producer.getCaptureDevice().getID();
    }

    public int getDeviceIndex() {
        return producer.getDeviceIndex();
    }
}
