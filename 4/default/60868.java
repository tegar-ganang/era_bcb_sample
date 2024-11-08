import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;
import java.net.*;

class SystemDataRes extends HTTPResponse {

    private DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);

    public SystemDataRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream, HashMap<String, String> headers) throws Exception {
        if ("01".equals(urlData.getParameter("action"))) {
            outStream.write(getLastChangeString());
            return;
        } else if ("02".equals(urlData.getParameter("action"))) {
            outStream.write(showServerProperties(urlData));
            return;
        } else if ("03".equals(urlData.getParameter("action"))) {
            outStream.write(setServerProperty(urlData));
            return;
        } else if ("04".equals(urlData.getParameter("action"))) {
            outStream.write(getTunerList(urlData));
            return;
        } else if ("06".equals(urlData.getParameter("action"))) {
            outStream.write(showAutoDelItems(urlData));
            return;
        } else if ("07".equals(urlData.getParameter("action"))) {
            outStream.write(remAutoDelItem(urlData));
            return;
        } else if ("08".equals(urlData.getParameter("action"))) {
            outStream.write(showTasks(urlData));
            return;
        } else if ("09".equals(urlData.getParameter("action"))) {
            outStream.write(addTask(urlData, headers));
            return;
        } else if ("10".equals(urlData.getParameter("action"))) {
            outStream.write(remTask(urlData, headers));
            return;
        } else if ("11".equals(urlData.getParameter("action"))) {
            outStream.write(showJavaEnviroment(urlData, headers));
            return;
        } else if ("12".equals(urlData.getParameter("action"))) {
            outStream.write(setEpgTask(urlData));
            return;
        } else if ("13".equals(urlData.getParameter("action"))) {
            outStream.write(addTunerToList(urlData));
            return;
        } else if ("14".equals(urlData.getParameter("action"))) {
            outStream.write(remTunerFromList(urlData));
            return;
        } else if ("15".equals(urlData.getParameter("action"))) {
            outStream.write(moveTunerUp(urlData));
            return;
        } else if ("16".equals(urlData.getParameter("action"))) {
            outStream.write(moveTunerDown(urlData));
            return;
        } else if ("17".equals(urlData.getParameter("action"))) {
            outStream.write(enableTask(urlData, headers));
            return;
        } else if ("18".equals(urlData.getParameter("action"))) {
            outStream.write(exportTaskList(urlData));
            return;
        } else if ("19".equals(urlData.getParameter("action"))) {
            outStream.write(showAvailableThemes(urlData, headers));
            return;
        } else if ("20".equals(urlData.getParameter("action"))) {
            outStream.write(applyThemes(urlData));
            return;
        } else if ("22".equals(urlData.getParameter("action"))) {
            outStream.write(editTaskPage(urlData));
            return;
        } else if ("23".equals(urlData.getParameter("action"))) {
            outStream.write(updateTask(urlData, headers));
            return;
        } else if ("25".equals(urlData.getParameter("action"))) {
            outStream.write(showTaskImportForm(urlData));
            return;
        } else if ("26".equals(urlData.getParameter("action"))) {
            outStream.write(importTaskListData(urlData, headers));
            return;
        } else if ("27".equals(urlData.getParameter("action"))) {
            outStream.write(showCapPathPage(urlData));
            return;
        } else if ("28".equals(urlData.getParameter("action"))) {
            outStream.write(deleteNamePattern(urlData));
            return;
        } else if ("29".equals(urlData.getParameter("action"))) {
            outStream.write(addNamePattern(urlData));
            return;
        } else if ("30".equals(urlData.getParameter("action"))) {
            outStream.write(moveNamePattern(urlData));
            return;
        } else if ("31".equals(urlData.getParameter("action"))) {
            outStream.write(showAvailablePaths(urlData));
            return;
        } else if ("32".equals(urlData.getParameter("action"))) {
            outStream.write(addCapturePath(urlData));
            return;
        } else if ("33".equals(urlData.getParameter("action"))) {
            outStream.write(deleteCapturePath(urlData));
            return;
        } else if ("34".equals(urlData.getParameter("action"))) {
            outStream.write(moveCapturePath(urlData));
            return;
        } else if ("35".equals(urlData.getParameter("action"))) {
            outStream.write(updatePathSettings(urlData));
            return;
        } else if ("36".equals(urlData.getParameter("action"))) {
            outStream.write(addAgentToThemeMap(urlData));
            return;
        } else if ("37".equals(urlData.getParameter("action"))) {
            outStream.write(remAgentToThemeMap(urlData));
            return;
        } else if ("38".equals(urlData.getParameter("action"))) {
            exportAllSettings(urlData, outStream);
            return;
        } else if ("39".equals(urlData.getParameter("action"))) {
            outStream.write(importAllSettings(urlData, headers));
            return;
        } else if ("40".equals(urlData.getParameter("action"))) {
            outStream.write(showRunningActions(urlData, headers));
            return;
        }
        outStream.write(showSystemInfo(urlData));
    }

    private byte[] getLastChangeString() {
        StringBuffer resData = new StringBuffer();
        resData.append("HTTP/1.0 200 OK\n");
        resData.append("Content-Type: text/plain\n");
        resData.append("Pragma: no-cache\n");
        resData.append("Cache-Control: no-cache\n\n");
        resData.append(store.getLastDataChange());
        return resData.toString().getBytes();
    }

    private byte[] importAllSettings(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "SettingsLoad.html");
        StringBuffer buff = new StringBuffer();
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        if (devList.getActiveDeviceCount() > 0) {
            buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Can not load settings while a capture is in progress.</td></tr>");
            template.replaceAll("$result", buff.toString());
            return template.getPageBytes();
        }
        byte[] securityData = urlData.getMultiPart().getPart("sessionID");
        if (securityData == null || store.checkSessionID(new String(securityData)) == false) {
            buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Security ID does not match.</td></tr>");
            template.replaceAll("$result", buff.toString());
            return template.getPageBytes();
        }
        boolean matchList = false;
        boolean autoAdd = false;
        boolean channelMapping = false;
        boolean deviceSelection = false;
        boolean agentMapping = false;
        boolean channels = false;
        boolean tasks = false;
        boolean systemProp = false;
        boolean schedules = false;
        matchList = urlData.getMultiPart().getPart("MatchList") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("MatchList")));
        autoAdd = urlData.getMultiPart().getPart("AutoAdd") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("AutoAdd")));
        channelMapping = urlData.getMultiPart().getPart("ChannelMapping") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("ChannelMapping")));
        deviceSelection = urlData.getMultiPart().getPart("DeviceSelection") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("DeviceSelection")));
        agentMapping = urlData.getMultiPart().getPart("AgentMapping") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("AgentMapping")));
        channels = urlData.getMultiPart().getPart("Channels") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("Channels")));
        tasks = urlData.getMultiPart().getPart("Tasks") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("Tasks")));
        systemProp = urlData.getMultiPart().getPart("SystemProp") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("SystemProp")));
        schedules = urlData.getMultiPart().getPart("Schedules") != null && "true".equalsIgnoreCase(new String(urlData.getMultiPart().getPart("Schedules")));
        boolean matchListDone = false;
        boolean autoAddDone = false;
        boolean channelMappingDone = false;
        boolean deviceSelectionDone = false;
        boolean agentMappingDone = false;
        boolean channelsDone = false;
        boolean tasksDone = false;
        boolean systemPropDone = false;
        boolean schedulesDone = false;
        byte[] partData = urlData.getMultiPart().getPart("file");
        if (partData != null) {
            ByteArrayInputStream partBytes = new ByteArrayInputStream(partData);
            ByteArrayOutputStream zipFileBytes = null;
            byte[] buffer = new byte[512];
            int in;
            ZipInputStream zipIn = new ZipInputStream(partBytes);
            ZipEntry fileEntry = zipIn.getNextEntry();
            if (fileEntry == null) {
                buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Not a vaild zip file.</td></tr>");
            }
            while (fileEntry != null) {
                if (fileEntry.isDirectory()) {
                    fileEntry = zipIn.getNextEntry();
                    continue;
                }
                zipFileBytes = new ByteArrayOutputStream();
                while (true) {
                    in = zipIn.read(buffer, 0, 512);
                    if (in == -1) break;
                    zipFileBytes.write(buffer, 0, in);
                }
                zipFileBytes.close();
                if (fileEntry.getName().equalsIgnoreCase("Channels.xml") && channels) {
                    try {
                        store.importChannels(zipFileBytes.toString("UTF-8"), false);
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Channels data loaded successfully</td></tr>");
                        channelsDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Channels data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("MatchList.xml") && matchList) {
                    try {
                        store.importMatchList(zipFileBytes.toString("UTF-8"), false);
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Match List data loaded successfully</td></tr>");
                        matchListDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Match List data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("EpgAutoAdd.xml") && autoAdd) {
                    try {
                        store.importEpgAutoList(zipFileBytes.toString("UTF-8"), false);
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Auto-Add data loaded successfully</td></tr>");
                        autoAddDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Auto-Add data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("Tasks.xml") && tasks) {
                    try {
                        store.importTaskList(zipFileBytes.toString("UTF-8"), false);
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Tasks data loaded successfully</td></tr>");
                        tasksDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Tasks data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("CaptureDevices.sof") && deviceSelection) {
                    try {
                        devList.importDeviceList(zipFileBytes.toByteArray());
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Capture Device data loaded successfully</td></tr>");
                        deviceSelectionDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Capture Device data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("ChannelMap.sof") && channelMapping) {
                    try {
                        GuideStore guideStore = GuideStore.getInstance();
                        guideStore.importChannelMap(zipFileBytes.toByteArray());
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Channel Map data loaded successfully</td></tr>");
                        channelMappingDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Channel Map data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("AgentMap.sof") && agentMapping) {
                    try {
                        store.importAgentToThemeMap(zipFileBytes.toByteArray());
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Agent to Theme Map data loaded successfully</td></tr>");
                        agentMappingDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Agent to Theme Map data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("Times.sof") && schedules) {
                    try {
                        store.importSchedule(zipFileBytes.toByteArray());
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Schedule data loaded successfully</td></tr>");
                        schedulesDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Schedule data load failed</td></tr>");
                        e.printStackTrace();
                    }
                } else if (fileEntry.getName().equalsIgnoreCase("ServerProperties.sof") && systemProp) {
                    try {
                        ByteArrayInputStream mapBytes = new ByteArrayInputStream(zipFileBytes.toByteArray());
                        ObjectInputStream ois = new ObjectInputStream(mapBytes);
                        @SuppressWarnings("unchecked") HashMap<String, String> serverprop = (HashMap<String, String>) ois.readObject();
                        ois.close();
                        String[] keys = serverprop.keySet().toArray(new String[0]);
                        for (int x = 0; x < keys.length; x++) {
                            store.setServerProperty(keys[x], serverprop.get(keys[x]));
                        }
                        buff.append("<tr><td><img border=0 src='/images/tick.png' align='absmiddle' width='24' height='24'></td><td>Server Settings data loaded successfully</td></tr>");
                        systemPropDone = true;
                    } catch (Exception e) {
                        buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Server Settings data load failed</td></tr>");
                        e.printStackTrace();
                    }
                }
                fileEntry = zipIn.getNextEntry();
            }
        } else {
            buff = new StringBuffer();
            buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Posted file not found.</td></tr>");
        }
        if (channels != channelsDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Channels data not found in settings file</td></tr>");
        if (matchList != matchListDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Match List data not found in settings file</td></tr>");
        if (autoAdd != autoAddDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Auto-Add data not found in settings file</td></tr>");
        if (channelMapping != channelMappingDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Channel Map data not found in settings file</td></tr>");
        if (deviceSelection != deviceSelectionDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Capture Device data not found in settings file</td></tr>");
        if (agentMapping != agentMappingDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Agent to Theme Map data not found in settings file</td></tr>");
        if (tasks != tasksDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Tasks data not found in settings file</td></tr>");
        if (systemProp != systemPropDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Server Settings data not found in settings file</td></tr>");
        if (schedules != schedulesDone) buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Schedule data not found in settings file</td></tr>");
        template.replaceAll("$result", buff.toString());
        return template.getPageBytes();
    }

    private void exportAllSettings(HTTPurl urlData, OutputStream outStream) throws Exception {
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        if (devList.getActiveDeviceCount() > 0) {
            PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "SettingsLoad.html");
            StringBuffer buff = new StringBuffer();
            buff.append("<tr><td><img border=0 src='/images/stop.png' align='absmiddle' width='24' height='24'></td><td>Can not save settings while a capture is in progress.</td></tr>");
            template.replaceAll("$result", buff.toString());
            outStream.write(template.getPageBytes());
            return;
        }
        boolean matchList = "true".equalsIgnoreCase(urlData.getParameter("MatchList"));
        boolean autoAdd = "true".equalsIgnoreCase(urlData.getParameter("AutoAdd"));
        boolean channelMapping = "true".equalsIgnoreCase(urlData.getParameter("ChannelMapping"));
        boolean deviceSelection = "true".equalsIgnoreCase(urlData.getParameter("DeviceSelection"));
        boolean agentMapping = "true".equalsIgnoreCase(urlData.getParameter("AgentMapping"));
        boolean channels = "true".equalsIgnoreCase(urlData.getParameter("Channels"));
        boolean tasks = "true".equalsIgnoreCase(urlData.getParameter("Tasks"));
        boolean systemProp = "true".equalsIgnoreCase(urlData.getParameter("SystemProp"));
        boolean schedules = "true".equalsIgnoreCase(urlData.getParameter("Schedules"));
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesOut);
        out.setComment("TV Scheduler Pro Settings file (Version: 1.0)");
        if (channels) {
            out.putNextEntry(new ZipEntry("Channels.xml"));
            StringBuffer channelData = new StringBuffer();
            store.saveChannels(channelData);
            byte[] channelBytes = channelData.toString().getBytes("UTF-8");
            out.write(channelBytes);
            out.closeEntry();
        }
        if (matchList) {
            out.putNextEntry(new ZipEntry("MatchList.xml"));
            StringBuffer matchData = new StringBuffer();
            store.saveMatchList(matchData);
            byte[] matchBytes = matchData.toString().getBytes("UTF-8");
            out.write(matchBytes);
            out.closeEntry();
        }
        if (autoAdd) {
            out.putNextEntry(new ZipEntry("EpgAutoAdd.xml"));
            StringBuffer addData = new StringBuffer();
            store.saveEpgAutoList(addData);
            byte[] addBytes = addData.toString().getBytes("UTF-8");
            out.write(addBytes);
            out.closeEntry();
        }
        if (tasks) {
            out.putNextEntry(new ZipEntry("Tasks.xml"));
            StringBuffer taskData = new StringBuffer();
            store.saveTaskList(taskData);
            byte[] taskBytes = taskData.toString().getBytes("UTF-8");
            out.write(taskBytes);
            out.closeEntry();
        }
        if (channelMapping) {
            GuideStore guideStore = GuideStore.getInstance();
            out.putNextEntry(new ZipEntry("ChannelMap.sof"));
            ByteArrayOutputStream chanMapBytes = new ByteArrayOutputStream();
            guideStore.saveChannelMap(chanMapBytes);
            out.write(chanMapBytes.toByteArray());
            out.closeEntry();
        }
        if (deviceSelection) {
            out.putNextEntry(new ZipEntry("CaptureDevices.sof"));
            ByteArrayOutputStream deviceBytes = new ByteArrayOutputStream();
            devList.saveDeviceList(deviceBytes);
            out.write(deviceBytes.toByteArray());
            out.closeEntry();
        }
        if (agentMapping) {
            out.putNextEntry(new ZipEntry("AgentMap.sof"));
            ByteArrayOutputStream agentMapBytes = new ByteArrayOutputStream();
            store.saveAgentToThemeMap(agentMapBytes);
            out.write(agentMapBytes.toByteArray());
            out.closeEntry();
        }
        if (schedules) {
            out.putNextEntry(new ZipEntry("Times.sof"));
            ByteArrayOutputStream timesBytes = new ByteArrayOutputStream();
            store.saveSchedule(timesBytes);
            out.write(timesBytes.toByteArray());
            out.closeEntry();
        }
        if (systemProp) {
            HashMap<String, String> serverProp = new HashMap<String, String>();
            serverProp.put("Capture.path", store.getProperty("Capture.path"));
            serverProp.put("Capture.AverageDataRate", store.getProperty("Capture.AverageDataRate"));
            serverProp.put("Capture.AutoSelectMethod", store.getProperty("Capture.AutoSelectMethod"));
            serverProp.put("Capture.minSpace", store.getProperty("Capture.minSpace"));
            serverProp.put("Capture.IncludeCalculatedUsage", store.getProperty("Capture.IncludeCalculatedUsage"));
            serverProp.put("Capture.deftype", store.getProperty("Capture.deftype"));
            serverProp.put("Capture.filename.patterns", store.getProperty("Capture.filename.patterns"));
            serverProp.put("Capture.path.details", store.getProperty("Capture.path.details"));
            serverProp.put("Capture.CaptureFailedTimeout", store.getProperty("Capture.CaptureFailedTimeout"));
            serverProp.put("Schedule.buffer.start", store.getProperty("Schedule.buffer.start"));
            serverProp.put("Schedule.buffer.end", store.getProperty("Schedule.buffer.end"));
            serverProp.put("Schedule.buffer.end.epg", store.getProperty("Schedule.buffer.end.epg"));
            serverProp.put("Schedule.wake.system", store.getProperty("Schedule.wake.system"));
            serverProp.put("sch.autodel.action", store.getProperty("sch.autodel.action"));
            serverProp.put("sch.autodel.time", store.getProperty("sch.autodel.time"));
            serverProp.put("guide.source.http.pwd", store.getProperty("guide.source.http.pwd"));
            serverProp.put("guide.source.xml.channelList", store.getProperty("guide.source.xml.channelList"));
            serverProp.put("guide.source.type", store.getProperty("guide.source.type"));
            serverProp.put("guide.source.http", store.getProperty("guide.source.http"));
            serverProp.put("guide.source.file", store.getProperty("guide.source.file"));
            serverProp.put("guide.action.name", store.getProperty("guide.action.name"));
            serverProp.put("guide.source.http.usr", store.getProperty("guide.source.http.usr"));
            serverProp.put("guide.source.schedule", store.getProperty("guide.source.schedule"));
            serverProp.put("guide.warn.overlap", store.getProperty("guide.warn.overlap"));
            serverProp.put("proxy.server", store.getProperty("proxy.server"));
            serverProp.put("proxy.port", store.getProperty("proxy.port"));
            serverProp.put("proxy.server.usr", store.getProperty("proxy.server.usr"));
            serverProp.put("proxy.server.pwd", store.getProperty("proxy.server.pwd"));
            serverProp.put("email.server", store.getProperty("email.server"));
            serverProp.put("email.from.name", store.getProperty("email.from.name"));
            serverProp.put("email.to", store.getProperty("email.to"));
            serverProp.put("email.from", store.getProperty("email.from"));
            serverProp.put("Tasks.DefTask", store.getProperty("Tasks.DefTask"));
            serverProp.put("Tasks.PreTask", store.getProperty("Tasks.PreTask"));
            serverProp.put("Tasks.NoDataErrorTask", store.getProperty("Tasks.NoDataErrorTask"));
            serverProp.put("Tasks.StartErrorTask", store.getProperty("Tasks.StartErrorTask"));
            serverProp.put("filebrowser.DirsAtTop", store.getProperty("filebrowser.DirsAtTop"));
            serverProp.put("filebrowser.masks", store.getProperty("filebrowser.masks"));
            serverProp.put("server.kbLED", store.getProperty("server.kbLED"));
            ByteArrayOutputStream serverpropBytes = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(serverpropBytes);
            oos.writeObject(serverProp);
            oos.close();
            out.putNextEntry(new ZipEntry("ServerProperties.sof"));
            out.write(serverpropBytes.toByteArray());
            out.closeEntry();
        }
        out.flush();
        out.close();
        StringBuffer header = new StringBuffer();
        header.append("HTTP/1.1 200 OK\n");
        header.append("Content-Type: application/zip\n");
        header.append("Content-Length: " + bytesOut.size() + "\n");
        header.append("Content-Disposition: attachment; filename=\"TV Scheduler Pro Settings.zip\"\n");
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

    private byte[] remAgentToThemeMap(HTTPurl urlData) throws Exception {
        store.removeAgentToThemeMap(urlData.getParameter("agent"));
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=19\n\n");
        return buff.toString().getBytes();
    }

    private byte[] addAgentToThemeMap(HTTPurl urlData) throws Exception {
        String agent = urlData.getParameter("agent");
        String theme = urlData.getParameter("theme");
        store.addAgentToThemeMap(agent, theme);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=19\n\n");
        return buff.toString().getBytes();
    }

    private byte[] updatePathSettings(HTTPurl urlData) throws Exception {
        int minSpace = 500;
        try {
            minSpace = Integer.parseInt(urlData.getParameter("minSpace").trim());
        } catch (Exception e) {
        }
        store.setServerProperty("Capture.minSpace", new Integer(minSpace).toString());
        store.setServerProperty("Capture.AutoSelectMethod", urlData.getParameter("AutoSelectType").trim());
        String include = urlData.getParameter("IncludeThisCapture");
        if ("true".equalsIgnoreCase(include)) {
            store.setServerProperty("Capture.IncludeCalculatedUsage", "1");
        } else {
            store.setServerProperty("Capture.IncludeCalculatedUsage", "0");
        }
        int avgData = 7000000;
        try {
            avgData = Integer.parseInt(urlData.getParameter("AverageDataRate").trim());
        } catch (Exception e) {
        }
        store.setServerProperty("Capture.AverageDataRate", new Integer(avgData).toString());
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] moveCapturePath(HTTPurl urlData) throws Exception {
        int index = Integer.parseInt(urlData.getParameter("id"));
        int amount = Integer.parseInt(urlData.getParameter("amount"));
        store.moveCapturePath(index, amount);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] deleteCapturePath(HTTPurl urlData) throws Exception {
        int index = Integer.parseInt(urlData.getParameter("id"));
        store.deleteCapturePath(index);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] addCapturePath(HTTPurl urlData) throws Exception {
        store.addCapturePath(urlData.getParameter("path"));
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showAvailablePaths(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "CapturePathsAvailable.html");
        StringBuffer buff = new StringBuffer();
        template.replaceAll("$title", "Available capture paths");
        String path = urlData.getParameter("path");
        File[] files = null;
        String parent = "";
        if (path == null || path.length() == 0) {
            files = File.listRoots();
            template.replaceAll("$currentPath", "");
        } else {
            File thisPath = new File(path);
            files = thisPath.listFiles();
            if (thisPath.getParentFile() != null) parent = thisPath.getParentFile().getAbsolutePath();
            String addLink = "";
            if (thisPath.exists()) {
                addLink = " <a href='#' onClick=\"addPath('/servlet/SystemDataRes?action=32&path=" + URLEncoder.encode(thisPath.getAbsolutePath(), "UTF-8") + "');\">" + "<img alt='Add Path' border=0 src='/images/add.png' align='absmiddle' width='24' height='24'></a> " + thisPath.getAbsolutePath();
            }
            template.replaceAll("$currentPath", addLink);
            if (thisPath.getParentFile() != null && thisPath.getParentFile().exists() == true) {
                buff.append("<tr><td nowrap>");
                buff.append("<a href='/servlet/SystemDataRes?action=31&path=" + URLEncoder.encode(parent, "UTF-8") + "' class='noUnder'>");
                buff.append("<img alt='parent' border=0 src='/images/prev.png' align='absmiddle' width='24' height='24'> ");
                buff.append("(parent)");
                buff.append("</a>");
                buff.append("</td></tr>");
            } else {
                buff.append("<tr><td nowrap>");
                buff.append("<a href='/servlet/SystemDataRes?action=31&path=' class='noUnder'>");
                buff.append("<img alt='parent' border=0 src='/images/prev.png' align='absmiddle' width='24' height='24'> ");
                buff.append("(root)");
                buff.append("</a>");
                buff.append("</td></tr>");
            }
        }
        if (files == null) {
            files = new File[0];
        }
        int numberItems = 0;
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                buff.append("<tr><td nowrap>");
                buff.append("<a href='/servlet/SystemDataRes?action=31&path=" + URLEncoder.encode(files[x].getCanonicalPath(), "UTF-8") + "' class='noUnder'>");
                buff.append("<img alt='path' border=0 src='/images/showchildren.png' align='absmiddle' width='24' height='24'> ");
                buff.append(files[x].getCanonicalPath());
                buff.append("</a>");
                buff.append("</td></tr>");
                numberItems++;
            }
        }
        if (numberItems == 0) {
            buff.append("<tr><td nowrap>No items to show</td></tr>");
        }
        template.replaceAll("$availablePaths", buff.toString());
        return template.getPageBytes();
    }

    private byte[] moveNamePattern(HTTPurl urlData) throws Exception {
        int index = Integer.parseInt(urlData.getParameter("id"));
        int amount = Integer.parseInt(urlData.getParameter("amount"));
        store.moveNamePattern(index, amount);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] deleteNamePattern(HTTPurl urlData) throws Exception {
        int index = Integer.parseInt(urlData.getParameter("id"));
        store.deleteNamePattern(index);
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] addNamePattern(HTTPurl urlData) throws Exception {
        store.addNamePattern(urlData.getParameter("pattern"));
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=27\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showCapPathPage(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "CapturePaths.html");
        template.replaceAll("$title", "Capture path and file name setup");
        StringBuffer buff = new StringBuffer();
        String[] paths = store.getCapturePaths();
        DllWrapper wrapper = new DllWrapper();
        NumberFormat nf = NumberFormat.getInstance();
        for (int x = 0; x < paths.length; x++) {
            buff.append("<tr>");
            File capPath = new File(paths[x]);
            if (capPath.exists() == false) {
                buff.append("<td nowrap>");
                buff.append("<img border='0' alt='Does Not Exist' src='/images/exclaim24.png' align='absmiddle' width='22' height='24'> ");
                buff.append(paths[x] + " </td>");
                buff.append("<td nowrap> (No details available) </td>");
            } else {
                buff.append("<td nowrap>" + capPath.getCanonicalPath() + " </td>");
                long freeSpace = wrapper.getFreeSpace(capPath.getCanonicalPath());
                freeSpace /= (1024 * 1024);
                buff.append("<td nowrap> Free: " + nf.format(freeSpace) + " MB</td>");
            }
            buff.append("<td nowrap width='50px'> ");
            if (paths.length > 1) {
                buff.append(" <a href='/servlet/SystemDataRes?action=33&id=" + x + "'><img border='0' alt='DEL' src='/images/delete.png' align='absmiddle' width='24' height='24'></a> ");
                if (x > 0) buff.append("<a href='/servlet/SystemDataRes?action=34&id=" + x + "&amount=-1'><img border='0' alt='Up' src='/images/up01.png' align='absmiddle' width='7' height='7'></a> "); else buff.append("<img border='0' alt='' src='/images/blank.gif' align='absmiddle' width='7' height='7'> ");
                if (x < paths.length - 1) buff.append("<a href='/servlet/SystemDataRes?action=34&id=" + x + "&amount=1'><img border='0' alt='Down' src='/images/down01.png' align='absmiddle' width='7' height='7'></a>"); else buff.append("<img border='0' alt='' src='/images/blank.gif' align='absmiddle' width='7' height='7'> ");
            }
            buff.append(" </td>\n");
            buff.append("</tr>");
        }
        template.replaceAll("$capturePaths", buff.toString());
        template.replaceAll("$minSpace", store.getProperty("Capture.minSpace"));
        String autoType = store.getProperty("Capture.AutoSelectMethod");
        buff = new StringBuffer();
        if (autoType.equals("0")) buff.append("<option value='0' selected>Most Free Space</option>"); else buff.append("<option value='0'>Most Free Space</option>");
        if (autoType.equals("1")) buff.append("<option value='1' selected>First With Enough Space</option>"); else buff.append("<option value='1'>First With Enough Space</option>");
        template.replaceAll("$AutoSelectType", buff.toString());
        String includeThis = store.getProperty("Capture.IncludeCalculatedUsage");
        if ("1".equals(includeThis)) {
            template.replaceAll("$IncludeThisCapture", "checked");
        } else {
            template.replaceAll("$IncludeThisCapture", "");
        }
        String avDataRate = store.getProperty("Capture.AverageDataRate").trim();
        template.replaceAll("$AverageDataRate", avDataRate);
        String[] patterns = store.getNamePatterns();
        buff = new StringBuffer();
        for (int x = 0; x < patterns.length; x++) {
            buff.append("<tr>");
            buff.append("<td>" + patterns[x] + " </td>");
            buff.append("<td> " + testPattern(patterns[x]) + " </td>");
            buff.append("<td nowrap width='50px'> ");
            buff.append(" <a href='/servlet/SystemDataRes?action=28&id=" + x + "'><img border='0' alt='DEL' src='/images/delete.png' align='absmiddle' width='24' height='24'></a> ");
            buff.append("<a href='/servlet/SystemDataRes?action=30&id=" + x + "&amount=-1'><img border='0' alt='Up' src='/images/up01.png' align='absmiddle' width='7' height='7'></a> ");
            buff.append("<a href='/servlet/SystemDataRes?action=30&id=" + x + "&amount=1'><img border='0' alt='Down' src='/images/down01.png' align='absmiddle' width='7' height='7'></a>");
            buff.append(" </td>\n");
            buff.append("</tr>");
        }
        template.replaceAll("$fileNamePatterns", buff.toString());
        return template.getPageBytes();
    }

    private String testPattern(String pattern) {
        Calendar cal = Calendar.getInstance();
        pattern = pattern.replaceAll("%y", addZero(cal.get(Calendar.YEAR)));
        pattern = pattern.replaceAll("%m", addZero((cal.get(Calendar.MONTH) + 1)));
        pattern = pattern.replaceAll("%d", addZero(cal.get(Calendar.DATE)));
        pattern = pattern.replaceAll("%h", addZero(cal.get(Calendar.HOUR_OF_DAY)));
        pattern = pattern.replaceAll("%M", addZero(cal.get(Calendar.MINUTE)));
        String dayOfWeek = "";
        try {
            dayOfWeek = (String) DataStore.getInstance().dayName.get(new Integer(cal.get(Calendar.DAY_OF_WEEK)));
        } catch (Exception e) {
        }
        pattern = pattern.replaceAll("%D", dayOfWeek);
        pattern = pattern.replaceAll("%n", "My Program");
        pattern = pattern.replaceAll("%N", "Program 01, Program 02, Program 30");
        pattern = pattern.replaceAll("%s", "Sub Name");
        pattern = pattern.replaceAll("%t", "Category");
        pattern = pattern.replaceAll("%c", "Chan10");
        pattern = pattern.replaceAll("(\\ )+", " ");
        return pattern.trim();
    }

    private String addZero(int input) {
        if (input < 10) return (String) ("0" + input); else return (new Integer(input)).toString();
    }

    private byte[] importTaskListData(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        if ("false".equalsIgnoreCase((String) headers.get("LoopbackAddress"))) {
            String out = "Security Warning:\n\n";
            out += "This action is not permitted from remote addresses, you can only perform\n";
            out += "this action from the machine that TV Scheduler Pro is running on.\n\n";
            out += "Your current address is " + (String) headers.get("RemoteAddress");
            return out.getBytes();
        }
        String sessionID = urlData.getParameter("sessionID");
        if (!store.checkSessionID(sessionID)) {
            return "Security Warning: The Security Session ID you entered is not correct.".getBytes();
        }
        boolean append = "append".equalsIgnoreCase(urlData.getParameter("data_action"));
        String data = urlData.getParameter("data");
        if (data != null && data.length() > 0) {
            store.importTaskList(data.trim(), append);
        }
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 302 Moved Temporarily\n");
        buff.append("Location: /servlet/SystemDataRes?action=08\n\n");
        return buff.toString().getBytes();
    }

    private byte[] showTaskImportForm(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ImportForm.html");
        template.replaceAll("$title", "Task List Data Import");
        template.replaceAll("$action", "/servlet/SystemDataRes?action=26");
        return template.getPageBytes();
    }

    private byte[] applyThemes(HTTPurl urlData) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/SystemDataRes?action=19\n\n";
        String theme = urlData.getParameter("theme");
        store.setServerProperty("path.theme", theme);
        String epg_theme = urlData.getParameter("epg_theme");
        store.setServerProperty("path.theme.epg", epg_theme);
        return out.getBytes();
    }

    private byte[] showAvailableThemes(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        StringBuffer out = new StringBuffer();
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ShowThemes.html");
        String httpDir = store.getProperty("path.httproot");
        String themeDir = store.getProperty("path.theme");
        File themeDirs = new File(httpDir + File.separator + "themes");
        int count = 0;
        if (themeDirs.exists()) {
            File[] dirs = themeDirs.listFiles();
            for (int x = 0; x < dirs.length; x++) {
                if (dirs[x].isDirectory() && dirs[x].isHidden() == false) {
                    count++;
                    out.append("<option value=\"" + dirs[x].getName() + "\"");
                    if (dirs[x].getName().equalsIgnoreCase(themeDir)) out.append(" SELECTED ");
                    out.append(">" + dirs[x].getName() + "</option>\n");
                }
            }
        }
        if (count == 0) {
            out.append("<option value=\"none\">none available</option>\n");
        }
        template.replaceAll("$themeList", out.toString());
        String currentEPGTheme = store.getProperty("path.theme.epg");
        out = new StringBuffer();
        String xslDir = store.getProperty("path.xsl");
        count = 0;
        File xslDirs = new File(xslDir);
        if (xslDirs.exists()) {
            File[] xslFiles = xslDirs.listFiles();
            for (int x = 0; x < xslFiles.length; x++) {
                if (xslFiles[x].isDirectory() == false) {
                    if (xslFiles[x].getName().matches("epg-.*.xsl")) {
                        count++;
                        out.append("<option value=\"" + xslFiles[x].getName() + "\"");
                        if (xslFiles[x].getName().equalsIgnoreCase(currentEPGTheme)) out.append(" SELECTED ");
                        String name = xslFiles[x].getName().substring(4, xslFiles[x].getName().length() - 4);
                        out.append(">" + name + "</option>\n");
                    }
                }
            }
        }
        if (count == 0) {
            out.append("<option value=\"none\">none available</option>\n");
        }
        template.replaceAll("$epg_themeList", out.toString());
        out = new StringBuffer();
        String[] agentList = store.getAgentMappingList();
        for (int x = 0; x < agentList.length; x++) {
            String themeForAgent = store.getThemeForAgent(agentList[x]);
            out.append("<tr>");
            out.append("<td>" + agentList[x] + "</td>");
            out.append("<td>" + themeForAgent + "</td>");
            out.append("<td><a href='/servlet/SystemDataRes?action=37&agent=" + URLEncoder.encode(agentList[x], "UTF-8") + "'><img src='/images/delete.png' alt='Delete Mapping' align='absmiddle' border='0' height='24' width='24'></a></td>");
            out.append("</tr>\n");
        }
        template.replaceAll("$themeMappings", out.toString());
        template.replaceAll("$agentString", headers.get("User-Agent"));
        return template.getPageBytes();
    }

    private byte[] showRunningActions(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        StringBuffer buff = new StringBuffer(2048);
        buff.append("HTTP/1.0 200 OK\n");
        buff.append("Content-Type: text/html\n");
        buff.append("Pragma: no-cache\n");
        buff.append("Cache-Control: no-cache\n\n");
        buff.append("<html>\n");
        buff.append("<table align='center' border='1'>\n");
        buff.append("<tr>\n");
        buff.append("<td nowrap>Device Index</td>\n");
        buff.append("<td nowrap>Device Name</td>\n");
        buff.append("<td nowrap>HashKey</td>\n");
        buff.append("<td nowrap>Share Name</td>\n");
        buff.append("<td nowrap>Usage Count</td>\n");
        buff.append("<td nowrap>Is Running</td>\n");
        buff.append("<td nowrap>Exit Code</td>\n");
        buff.append("<td nowrap>Needs Reastart</td>\n");
        buff.append("</tr>\n");
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        StreamProducerProcess[] producers = devList.getProducers();
        for (int index = 0; index < producers.length; index++) {
            StreamProducerProcess producer = producers[index];
            buff.append("<tr>\n");
            buff.append("<td nowrap>" + producer.getDeviceIndex() + "</td>\n");
            buff.append("<td nowrap>" + producer.getCaptureDevice().getName() + "</td>\n");
            buff.append("<td nowrap>" + producer.getKey() + "</td>\n");
            buff.append("<td nowrap>" + producer.getMemoryShareName() + "</td>\n");
            buff.append("<td nowrap>" + producer.getUsageCount() + "</td>\n");
            buff.append("<td nowrap>" + producer.isProducerRunning() + "</td>\n");
            buff.append("<td nowrap>" + producer.getExitCode() + "</td>\n");
            buff.append("<td nowrap>" + producer.getNeedsRestart() + "</td>\n");
            buff.append("</tr>\n");
        }
        buff.append("</table>\n");
        buff.append("<br><br><br>\n");
        buff.append("</html>");
        buff.append("\n");
        return buff.toString().getBytes();
    }

    private byte[] showJavaEnviroment(HTTPurl urlData, HashMap<String, String> headers) throws Exception {
        StringBuffer biff = new StringBuffer(2048);
        NumberFormat nf = NumberFormat.getInstance();
        Runtime r = Runtime.getRuntime();
        long total = r.totalMemory();
        long free = r.freeMemory();
        long max = r.maxMemory();
        biff.append("HTTP/1.0 200 OK\n");
        biff.append("Content-Type: text/plain\n");
        biff.append("Pragma: no-cache\n");
        biff.append("Cache-Control: no-cache\n\n");
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("System Info\n");
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("java.home: " + System.getProperty("java.home") + "\n");
        biff.append("java.class.path: " + "java.home" + System.getProperty("java.class.path") + "\n");
        biff.append("java.specification.version: " + System.getProperty("java.specification.version") + "\n");
        biff.append("java.specification.vendor: " + System.getProperty("java.specification.vendor") + "\n");
        biff.append("java.specification.name: " + System.getProperty("java.specification.name") + "\n");
        biff.append("java.version: " + System.getProperty("java.version") + "\n");
        biff.append("java.vendor: " + System.getProperty("java.vendor") + "\n");
        biff.append("java.vendor.url: " + System.getProperty("java.vendor.url") + "\n");
        biff.append("java.vm.specification.version: " + System.getProperty("java.vm.specification.version") + "\n");
        biff.append("java.vm.specification.vendor: " + System.getProperty("java.vm.specification.vendor") + "\n");
        biff.append("java.vm.specification.name: " + System.getProperty("java.vm.specification.name") + "\n");
        biff.append("java.vm.version: " + System.getProperty("java.vm.version") + "\n");
        biff.append("java.vm.vendor: " + System.getProperty("java.vm.vendor") + "\n");
        biff.append("java.vm.name: " + System.getProperty("java.vm.name") + "\n");
        biff.append("java.class.version: " + System.getProperty("java.class.version") + "\n");
        biff.append("os.home: " + System.getProperty("os.name") + "\n");
        biff.append("os.arch: " + System.getProperty("os.arch" + "\n"));
        biff.append("os.version: " + System.getProperty("os.version") + "\n");
        biff.append("user.name: " + System.getProperty("user.name") + "\n");
        biff.append("user.home: " + System.getProperty("user.home") + "\n");
        biff.append("user.dir: " + System.getProperty("user.dir") + "\n");
        biff.append("Runtime.maxMemory()   : " + nf.format(max) + "\n");
        biff.append("Runtime.totalMemory() : " + nf.format(total) + "\n");
        biff.append("Runtime.freeMemory() : " + nf.format(free) + "\n");
        biff.append("Time Zone ID     : " + Calendar.getInstance().getTimeZone().getID() + "\n");
        biff.append("Time Zone Name   : " + Calendar.getInstance().getTimeZone().getDisplayName() + "\n");
        biff.append("Time Zone DST    : " + Calendar.getInstance().getTimeZone().getDSTSavings() + "\n");
        biff.append("Time Zone Offset : " + Calendar.getInstance().getTimeZone().getRawOffset() + "\n");
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("HTTP Headers\n--------------------------------------------------------------------------\n");
        String[] headName = (String[]) headers.keySet().toArray(new String[0]);
        for (int x = 0; x < headName.length; x++) {
            biff.append(headName[x] + " : " + (String) headers.get(headName[x]) + "\n");
        }
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("Timer Thread Status = " + store.timerStatus + "\n");
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("Threads\n--------------------------------------------------------------------------\n");
        ThreadGroup top = Thread.currentThread().getThreadGroup();
        StringBuffer buff = new StringBuffer();
        while (true) {
            if (top.getParent() != null) top = top.getParent(); else break;
        }
        Thread[] theThreads = new Thread[top.activeCount()];
        top.enumerate(theThreads);
        for (int i = 0; i < theThreads.length; i++) {
            biff.append("Thread " + i + " : " + theThreads[i].getName() + " : " + theThreads[i].toString() + " : " + theThreads[i].getState() + "\n");
            StackTraceElement[] stack = theThreads[i].getStackTrace();
            buff.append("Thread " + i + " : " + theThreads[i].getName() + " : " + theThreads[i].toString() + " : " + theThreads[i].getState() + "\n");
            for (int q = 0; q < stack.length; q++) {
                buff.append(stack[q].toString() + "\n");
            }
            buff.append("\n");
        }
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("Thread StackTrace\n--------------------------------------------------------------------------\n");
        biff.append(buff.toString());
        biff.append("--------------------------------------------------------------------------\n");
        biff.append("ThreadLock Details\n--------------------------------------------------------------------------\n");
        biff.append("Is Locked : " + ThreadLock.getInstance().isLocked() + "\n");
        biff.append("Has Queued Threads :  " + ThreadLock.getInstance().hasQueuedThreads() + "\n");
        biff.append("Queue Length : " + ThreadLock.getInstance().getQueueLength() + "\n");
        biff.append("--------------------------------------------------------------------------\n");
        return biff.toString().getBytes();
    }

    private byte[] remTask(HTTPurl urlData, HashMap headers) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
        if ("false".equalsIgnoreCase((String) headers.get("LoopbackAddress"))) {
            out = "Security Warning:\n\n";
            out += "This action is not permitted from remote addresses, you can only perform\n";
            out += "this action from the machine that TV Scheduler Pro is running on.\n\n";
            out += "Tour current address is " + (String) headers.get("RemoteAddress");
            return out.getBytes();
        }
        HashMap tasks = store.getTaskList();
        String name = urlData.getParameter("name");
        tasks.remove(name);
        store.saveTaskList(null);
        return out.getBytes();
    }

    private byte[] addTask(HTTPurl urlData, HashMap headers) throws Exception {
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String name = urlData.getParameter("name");
        name = checkName(name);
        if (name != null && name.trim().length() > 0) {
            TaskCommand taskCommand = new TaskCommand(name.trim());
            tasks.put(taskCommand.getName(), taskCommand);
            store.saveTaskList(null);
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=22&name=" + URLEncoder.encode(name.trim(), "UTF-8") + "\n\n";
            return out.getBytes();
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
        return out.getBytes();
    }

    private String checkName(String name) {
        StringBuffer finalName = null;
        try {
            finalName = new StringBuffer(256);
            for (int x = 0; x < name.length(); x++) {
                char charAt = name.charAt(x);
                if ((charAt >= 'a' && charAt <= 'z') || (charAt >= 'A' && charAt <= 'Z') || (charAt >= '0' && charAt <= '9') || charAt == ' ') finalName.append(charAt); else finalName.append('-');
            }
        } catch (Exception e) {
            name = "error";
        }
        return finalName.toString();
    }

    private byte[] exportTaskList(HTTPurl urlData) throws Exception {
        StringBuffer buff = new StringBuffer();
        buff.append("HTTP/1.0 200 OK\nContent-Type: text/xml\n");
        buff.append("Content-Disposition: attachment; filename=\"Tasks.xml\"\n");
        buff.append("Pragma: no-cache\n");
        buff.append("Cache-Control: no-cache\n");
        buff.append("\n");
        store.saveTaskList(buff);
        return buff.toString().getBytes();
    }

    private byte[] setEpgTask(HTTPurl urlData) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
        String taskDef = urlData.getParameter("Tasks.DefTask");
        if (taskDef == null || "none".equals(taskDef)) taskDef = "";
        store.setServerProperty("Tasks.DefTask", taskDef);
        String taskPre = urlData.getParameter("Tasks.PreTask");
        if (taskPre == null || "none".equals(taskPre)) taskPre = "";
        store.setServerProperty("Tasks.PreTask", taskPre);
        String taskStartError = urlData.getParameter("Tasks.StartErrorTask");
        if (taskStartError == null || "none".equals(taskStartError)) taskStartError = "";
        store.setServerProperty("Tasks.StartErrorTask", taskStartError);
        String taskNoDataError = urlData.getParameter("Tasks.NoDataErrorTask");
        if (taskNoDataError == null || "none".equals(taskNoDataError)) taskNoDataError = "";
        store.setServerProperty("Tasks.NoDataErrorTask", taskNoDataError);
        return out.getBytes();
    }

    private byte[] updateTask(HTTPurl urlData, HashMap headers) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
        if ("false".equalsIgnoreCase((String) headers.get("LoopbackAddress"))) {
            out = "Security Warning:\n\n";
            out += "This action is not permitted from remote addresses, you can only perform\n";
            out += "this action from the machine that TV Scheduler Pro is running on.\n\n";
            out += "Tour current address is " + (String) headers.get("RemoteAddress");
            return out.getBytes();
        }
        String sessionID = urlData.getParameter("sessionID");
        if (!store.checkSessionID(sessionID)) {
            out = "Security Warning: The Security Session ID you entered is not correct.";
            return out.getBytes();
        }
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String name = urlData.getParameter("task_name");
        TaskCommand task = (TaskCommand) tasks.get(name);
        if (task != null) {
            String command = urlData.getParameter("command");
            String autoRem = urlData.getParameter("autoRemove");
            String delay = urlData.getParameter("delay");
            String concurrentTasks = urlData.getParameter("concurrentTasks");
            String timeToNextSchedule = urlData.getParameter("timeToNextSchedule");
            String whenNotCapturing = urlData.getParameter("whenNotCapturing");
            Boolean notCap = new Boolean(false);
            if ("true".equalsIgnoreCase(whenNotCapturing)) {
                notCap = new Boolean(true);
            }
            task.setWhenNotCapturing(notCap.booleanValue());
            int timeToNext = 0;
            try {
                timeToNext = Integer.parseInt(timeToNextSchedule);
            } catch (Exception e) {
            }
            task.setTimeToNextSchedule(timeToNext);
            int conTasks = 0;
            try {
                conTasks = Integer.parseInt(concurrentTasks);
            } catch (Exception e) {
            }
            task.setConcurrent(conTasks);
            Boolean autoRemove = new Boolean(false);
            if ("true".equalsIgnoreCase(autoRem)) {
                autoRemove = new Boolean(true);
            }
            task.setAutoRemove(autoRemove.booleanValue());
            int delayValue = 0;
            try {
                delayValue = Integer.parseInt(delay);
            } catch (Exception e) {
            }
            task.setDelay(delayValue);
            task.setCommand(command.trim());
            store.saveTaskList(null);
        }
        return out.getBytes();
    }

    private byte[] editTaskPage(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "TaskEdit.html");
        String taskName = urlData.getParameter("name");
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        TaskCommand task = (TaskCommand) tasks.get(taskName);
        if (task != null) {
            template.replaceAll("$taskName", taskName);
            String command = task.getCommand();
            command = command.replaceAll("\"", "&#34;");
            command = command.replaceAll("<", "&lt;");
            command = command.replaceAll(">", "&gt;");
            template.replaceAll("$taskCommand", command);
            template.replaceAll("$taskDelayFor", new Integer(task.getDelay()).toString());
            if (task.getAutoRemove()) template.replaceAll("$autoRemove", "checked"); else template.replaceAll("$autoRemove", "");
            template.replaceAll("$concurrentTasks", new Integer(task.getConcurrent()).toString());
            template.replaceAll("$timeToNextSchedule", new Integer(task.getTimeToNextSchedule()).toString());
            if (task.getWhenNotCapturing()) template.replaceAll("$whenNotCapturing", "checked"); else template.replaceAll("$whenNotCapturing", "");
        } else {
            String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
            return out.getBytes();
        }
        return template.getPageBytes();
    }

    private byte[] enableTask(HTTPurl urlData, HashMap headers) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=08\n\n";
        if ("false".equalsIgnoreCase((String) headers.get("LoopbackAddress"))) {
            out = "Security Warning:\n\n";
            out += "This action is not permitted from remote addresses, you can only perform\n";
            out += "this action from the machine that TV Scheduler Pro is running on.\n\n";
            out += "Tour current address is " + (String) headers.get("RemoteAddress");
            return out.getBytes();
        }
        HashMap tasks = store.getTaskList();
        String enabled = urlData.getParameter("enabled");
        String name = urlData.getParameter("name");
        TaskCommand taskCommand = (TaskCommand) tasks.get(name);
        if (taskCommand != null) {
            if ("true".equals(enabled)) taskCommand.setEnabled(true); else taskCommand.setEnabled(false);
            store.saveTaskList(null);
        }
        return out.getBytes();
    }

    private byte[] showTasks(HTTPurl urlData) throws Exception {
        StringBuffer out = new StringBuffer(2048);
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "TaskList.html");
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String[] key = (String[]) tasks.keySet().toArray(new String[0]);
        Arrays.sort(key);
        for (int x = 0; x < key.length; x++) {
            TaskCommand taskCommand = (TaskCommand) tasks.get(key[x]);
            out.append("<tr>\n");
            if (taskCommand.getEnabled()) {
                out.append("<td align='center'><a href='/servlet/" + urlData.getServletClass() + "?action=17&name=" + URLEncoder.encode(key[x], "UTF-8") + "&enabled=false'><img border='0' alt='Yes' src='/images/tick.png' width='24' height='24'></a></td>");
            } else {
                out.append("<td align='center'><a href='/servlet/" + urlData.getServletClass() + "?action=17&name=" + URLEncoder.encode(key[x], "UTF-8") + "&enabled=true'><img border='0' alt='No' src='/images/stop.png' width='24' height='24'></a></td>");
            }
            out.append("<td nowrap>" + key[x] + "</td>");
            out.append("<td nowrap>" + new Boolean(taskCommand.getAutoRemove()).toString() + "</td>");
            out.append("<td nowrap>" + new Integer(taskCommand.getDelay()).toString() + "</td>");
            out.append("<td>" + taskCommand.getCommand() + "</td>");
            out.append("<td align='center' nowrap>");
            out.append("<a class='noUnder' " + "href='/servlet/" + urlData.getServletClass() + "?action=22&name=" + URLEncoder.encode(key[x], "UTF-8") + "'>");
            out.append("<img src='/images/edit.png' border='0' alt='Edit' title='Edit' width='24' height='24'></a> ");
            out.append("<a class='noUnder' onClick='return confirmAction(\"Delete\");' " + "href='/servlet/" + urlData.getServletClass() + "?action=10&name=" + URLEncoder.encode(key[x], "UTF-8") + "'>");
            out.append("<img src='/images/delete.png' border='0' alt='Delete' title='Delete' width='24' height='24'></a> ");
            out.append("</td>");
            out.append("</tr>\n");
        }
        template.replaceAll("$taskList", out.toString());
        template.replaceAll("$defEpgTaskSelect", getTaskSelect("Tasks.DefTask"));
        template.replaceAll("$preTaskSelect", getTaskSelect("Tasks.PreTask"));
        template.replaceAll("$startErrorSelect", getTaskSelect("Tasks.StartErrorTask"));
        template.replaceAll("$noDataErrorSelect", getTaskSelect("Tasks.NoDataErrorTask"));
        return template.getPageBytes();
    }

    private String getTaskSelect(String selected) {
        String selectedTask = store.getProperty(selected);
        StringBuffer buff = new StringBuffer(1024);
        HashMap<String, TaskCommand> tasks = store.getTaskList();
        String[] keys = (String[]) tasks.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        buff.append("<select name='" + selected + "'>\n");
        if (selectedTask.length() == 0) buff.append("<option value='' selected>none</option>\n"); else buff.append("<option value='' >none</option>\n");
        for (int x = 0; x < keys.length; x++) {
            if (selectedTask.equalsIgnoreCase(keys[x])) buff.append("<option value='" + keys[x] + "' selected>" + keys[x] + "</option>\n"); else buff.append("<option value='" + keys[x] + "'>" + keys[x] + "</option>\n");
        }
        buff.append("</select>\n");
        return buff.toString();
    }

    private byte[] remAutoDelItem(HTTPurl urlData) throws Exception {
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: " + "/servlet/" + urlData.getServletClass() + "?action=06\n\n";
        String id = urlData.getParameter("id");
        HashMap items = store.getAutoDelList();
        items.remove(id);
        return out.getBytes();
    }

    private byte[] showAutoDelItems(HTTPurl urlData) throws Exception {
        StringBuffer out = new StringBuffer(2048);
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "AutoDelItems.html");
        HashMap<String, KeepForDetails> items = store.getAutoDelList();
        String[] key = (String[]) items.keySet().toArray(new String[0]);
        for (int x = 0; x < key.length; x++) {
            KeepForDetails item = (KeepForDetails) items.get(key[x]);
            out.append("<tr>\n");
            out.append("<td>" + item.getCreated().toString() + "</td>");
            out.append("<td>" + item.getFileName() + "</td>");
            out.append("<td>" + item.getKeepFor() + "</td>");
            out.append("<td><a href='/servlet/" + urlData.getServletClass() + "?action=07&id=" + key[x] + "'>remove</a></td>");
            out.append("</tr>\n");
        }
        template.replaceAll("$itemList", out.toString());
        template.replaceAll("$autoDelLog", store.getAutoDelLog());
        return template.getPageBytes();
    }

    private byte[] getTunerList(HTTPurl urlData) throws Exception {
        boolean showID = "true".equalsIgnoreCase(urlData.getParameter("showid"));
        showID = showID | "true".equalsIgnoreCase(urlData.getCookie("showDeviceID"));
        if ("false".equalsIgnoreCase(urlData.getParameter("showid"))) showID = false;
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "CardSetup.html");
        if (showID == true) template.addCookie("showDeviceID", "true"); else template.addCookie("showDeviceID", "false");
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        int activeDevices = devList.getActiveDeviceCount();
        String scanCommand = "device.exe";
        System.out.println("Running device scan command: " + scanCommand);
        Runtime runner = Runtime.getRuntime();
        Process scan = runner.exec(scanCommand);
        TunerScanResult tuners = new TunerScanResult();
        tuners.readInput(scan.getInputStream());
        tuners.parseXML();
        StringBuffer out = new StringBuffer();
        Vector<CaptureDevice> tunersList = tuners.getResult();
        out.append("<tr><td colspan='3' style='border: 1px solid #FFFFFF;'>");
        out.append("<table width='100%' border='0' cellpadding='0' cellspacing='0'><tr><td><strong>Currently Selected Devices</strong></td><td align='right'>");
        if (showID) out.append("<a style='text-decoration: none; color: #FFFFFF; font-size: 12px;' href='/servlet/SystemDataRes?action=04&showid=false'>Hide IDs</a>"); else out.append("<a style='text-decoration: none; color: #FFFFFF; font-size: 12px;' href='/servlet/SystemDataRes?action=04&showid=true'>Show IDs</a>");
        out.append("</td></tr></table></td></tr>\n");
        for (int x = 0; x < devList.getDeviceCount(); x++) {
            CaptureDevice cd = (CaptureDevice) devList.getDevice(x);
            out.append("<tr>");
            out.append("<td nowrap>" + x + "</td>");
            out.append("<td nowrap>: ");
            out.append(cd.getName());
            if (cd.isInUse() == true) out.append(" (Active)");
            boolean isAvailable = false;
            for (int y = 0; y < tunersList.size(); y++) {
                CaptureDevice cd2 = (CaptureDevice) tunersList.get(y);
                if (cd.getID().equals(cd2.getID())) {
                    isAvailable = true;
                    break;
                }
            }
            if (isAvailable == false) out.append(" <img border='0' alt='Not Available' title='Device Not Available' src='/images/exclaim24.png' align='absmiddle' width='22' height='24'> ");
            if (showID) out.append("(" + cd.getID() + ")");
            out.append("</td>\n");
            out.append("<td nowrap width='50px'> ");
            out.append(" <a href='/servlet/SystemDataRes?action=14&tunerID=" + x + "'><img border='0' alt='DEL' src='/images/delete.png' align='absmiddle' width='24' height='24'></a> ");
            out.append("<a href='/servlet/SystemDataRes?action=15&tunerID=" + x + "'><img border='0' alt='Up' src='/images/up01.png' align='absmiddle' width='7' height='7'></a> ");
            out.append("<a href='/servlet/SystemDataRes?action=16&tunerID=" + x + "'><img border='0' alt='Down' src='/images/down01.png' align='absmiddle' width='7' height='7'></a>");
            out.append("</td>\n");
            out.append("</tr>\n");
        }
        if (devList.getDeviceCount() == 0) {
            out.append("<tr><td colspan ='3'>No devices selected</td></tr>");
        }
        int numCards = 0;
        out.append("<tr><td colspan='3'><strong>&nbsp;</strong></td></tr>");
        out.append("<tr><td colspan='3' style='border: 1px solid #FFFFFF;'><strong>Devices Available But Not Selected</strong></td></tr>");
        for (int x = 0; x < tunersList.size(); x++) {
            CaptureDevice dev = (CaptureDevice) tunersList.get(x);
            boolean found = false;
            for (int y = 0; y < devList.getDeviceCount(); y++) {
                CaptureDevice cd = (CaptureDevice) devList.getDevice(y);
                if (cd.getID().equals(dev.getID())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                out.append("<tr>");
                out.append("<td>&nbsp;</td>");
                out.append("<td nowrap>" + dev.getName() + "</td>");
                out.append("<td width='50px'><a href='/servlet/SystemDataRes?action=13&tunerID=" + URLEncoder.encode(dev.getID(), "UTF-8"));
                out.append("&tunerName=" + URLEncoder.encode(dev.getName(), "UTF-8") + "'>");
                out.append("<img border='0' alt='ADD' src='/images/add.png' align='absmiddle' width='24' height='24'></a></td>\n");
                out.append("</tr>\n");
                numCards++;
            }
        }
        if (numCards == 0) {
            out.append("<tr><td colspan ='3'>No devices available</td></tr>");
        }
        numCards = 0;
        template.replaceAll("$cardList", out.toString());
        template.replaceAll("$cardCount", new Integer(activeDevices).toString());
        return template.getPageBytes();
    }

    private byte[] moveTunerUp(HTTPurl urlData) throws Exception {
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        int tunerIndex = -1;
        try {
            tunerIndex = Integer.parseInt(urlData.getParameter("tunerID"));
            if (devList.getActiveDeviceCount() == 0) {
                if (tunerIndex > 0 && tunerIndex < devList.getDeviceCount()) {
                    CaptureDevice cap = devList.remDevice(tunerIndex);
                    devList.addDeviceAt(tunerIndex - 1, cap);
                    devList.saveDeviceList(null);
                }
            }
        } catch (Exception e) {
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: /servlet/SystemDataRes?action=04\n\n";
        return out.getBytes();
    }

    private byte[] moveTunerDown(HTTPurl urlData) throws Exception {
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        int tunerIndex = -1;
        try {
            tunerIndex = Integer.parseInt(urlData.getParameter("tunerID"));
            if (devList.getActiveDeviceCount() == 0) {
                if (tunerIndex >= 0 && tunerIndex < devList.getDeviceCount() - 1) {
                    CaptureDevice cap = devList.remDevice(tunerIndex);
                    devList.addDeviceAt(tunerIndex + 1, cap);
                    devList.saveDeviceList(null);
                }
            }
        } catch (Exception e) {
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: /servlet/SystemDataRes?action=04\n\n";
        return out.getBytes();
    }

    private byte[] addTunerToList(HTTPurl urlData) throws Exception {
        String tunerID = "";
        String name = "";
        try {
            tunerID = urlData.getParameter("tunerID");
            name = urlData.getParameter("tunerName");
            boolean alreadyAdded = false;
            CaptureDeviceList devList = CaptureDeviceList.getInstance();
            if (tunerID.length() > 0) {
                for (int x = 0; x < devList.getDeviceCount(); x++) {
                    CaptureDevice cap = (CaptureDevice) devList.getDevice(x);
                    if (cap.getID() == tunerID) {
                        alreadyAdded = true;
                    }
                }
            }
            if (alreadyAdded == false && tunerID.length() > 0 && devList.getActiveDeviceCount() == 0) {
                CaptureDevice cap = new CaptureDevice(name, tunerID);
                devList.addDevice(cap);
                devList.saveDeviceList(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: /servlet/SystemDataRes?action=04\n\n";
        return out.getBytes();
    }

    private byte[] remTunerFromList(HTTPurl urlData) throws Exception {
        int tunerIndex = -1;
        try {
            CaptureDeviceList devList = CaptureDeviceList.getInstance();
            tunerIndex = Integer.parseInt(urlData.getParameter("tunerID"));
            if (devList.getActiveDeviceCount() == 0) {
                if (tunerIndex >= 0 && tunerIndex < devList.getDeviceCount()) {
                    devList.remDevice(tunerIndex);
                    devList.saveDeviceList(null);
                }
            }
        } catch (Exception e) {
        }
        String out = "HTTP/1.0 302 Moved Temporarily\nLocation: /servlet/SystemDataRes?action=04\n\n";
        return out.getBytes();
    }

    private byte[] setServerProperty(HTTPurl urlData) throws Exception {
        String out = "";
        String sessionID = urlData.getParameter("sessionID");
        if (!store.checkSessionID(sessionID)) {
            out = "Security Warning: The Security Session ID you entered is not correct.";
            return out.getBytes();
        }
        out = "HTTP/1.0 302 Moved Temporarily\nLocation: /settings.html\n\n";
        String[] parameter = urlData.getParameterList();
        for (int x = 0; x < parameter.length; x++) {
            if (!parameter[x].equals("action") && !parameter[x].equals("sessionID")) {
                String value = urlData.getParameter(parameter[x]);
                if (value != null) {
                    store.setServerProperty(parameter[x], value);
                }
            }
        }
        return out.getBytes();
    }

    private byte[] showServerProperties(HTTPurl urlData) throws Exception {
        StringBuffer out = new StringBuffer(1024);
        String value = "";
        HashMap<String, String> options = null;
        out.append("<tr><td colspan='3' align='left' style='border: 1px solid rgb(255, 255, 255);'>");
        out.append("<span class='areaTitle'>Capture Settings</span>\n");
        out.append("</td></tr>\n");
        value = store.getProperty("Capture.deftype");
        options = new HashMap<String, String>();
        Vector<CaptureCapability> capabilities = CaptureCapabilities.getInstance().getCapabilities();
        for (int index = 0; index < capabilities.size(); index++) {
            CaptureCapability capability = capabilities.get(index);
            options.put(new Integer(capability.getTypeID()).toString(), capability.getName());
        }
        out.append("<tr><td align='left'>Default Capture Type</td><td>");
        out.append(htmlDropMenu(options, "Capture.deftype", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Capture.deftype');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("Capture.path.details");
        out.append("<tr><td align='left'>Capture Details Path</td><td>\n");
        out.append("<input type='text' name='Capture.path.details' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Capture.path.details');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("Capture.CaptureFailedTimeout");
        options = new HashMap<String, String>();
        options.put("003", "15 Seconds");
        options.put("006", "30 Seconds");
        options.put("012", "60 Seconds");
        options.put("024", "128 Seconds");
        out.append("<tr><td align='left'>Failed Capture Timeout</td><td>");
        out.append(htmlDropMenu(options, "Capture.CaptureFailedTimeout", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Capture.CaptureFailedTimeout');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        out.append("<tr><td colspan='3' align='left' style='border: 1px solid rgb(255, 255, 255);'>");
        out.append("<span class='areaTitle'>Schedule Settings</span>\n");
        out.append("</td></tr>\n");
        value = store.getProperty("Schedule.buffer.end");
        out.append("<tr><td align='left'>End Buffer Time</td><td>\n");
        out.append("<input type='text' name='Schedule.buffer.end' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Schedule.buffer.end');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("Schedule.buffer.start");
        out.append("<tr><td align='left'>Start Buffer Time</td><td>\n");
        out.append("<input type='text' name='Schedule.buffer.start' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Schedule.buffer.start');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("Schedule.buffer.end.epg");
        out.append("<tr><td align='left'>End Buffer EPG Addition</td><td>\n");
        out.append("<input type='text' name='Schedule.buffer.end.epg' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Schedule.buffer.end.epg');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        out.append("<tr><td colspan='3' align='left' style='border: 1px solid rgb(255, 255, 255);'>");
        out.append("<span class='areaTitle'>Email Settings</span>\n");
        out.append("</td></tr>\n");
        value = store.getProperty("email.server");
        out.append("<tr><td align='left'>Email Server</td><td>\n");
        out.append("<input type='text' name='email.server' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('email.server');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("email.from");
        out.append("<tr><td align='left'>Email From Address</td><td>\n");
        out.append("<input type='text' name='email.from' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('email.from');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("email.from.name");
        out.append("<tr><td align='left'>Email From Name</td><td>\n");
        out.append("<input type='text' name='email.from.name' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('email.from.name');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("email.to");
        out.append("<tr><td align='left'>Email To</td><td>\n");
        out.append("<input type='text' name='email.to' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('email.to');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        out.append("<tr><td colspan='3' align='left' style='border: 1px solid rgb(255, 255, 255);'>");
        out.append("<span class='areaTitle'>File Browser Settings</span>\n");
        out.append("</td></tr>\n");
        value = store.getProperty("filebrowser.DirsAtTop");
        options = new HashMap<String, String>();
        options.put("0", "Bottom");
        options.put("1", "Top");
        out.append("<tr><td align='left'>Directories Shown At</td><td>");
        out.append(htmlDropMenu(options, "filebrowser.DirsAtTop", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('filebrowser.DirsAtTop');\" width='24' height='24'></td></tr>\n");
        value = store.getProperty("filebrowser.masks");
        out.append("<tr><td align='left'>Show Extensions</td><td>\n");
        out.append("<input type='text' name='filebrowser.masks' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('filebrowser.masks');\" width='24' height='24'></tr>\n");
        value = store.getProperty("filebrowser.ShowWsPlay");
        options = new HashMap<String, String>();
        options.put("0", "False");
        options.put("1", "True");
        out.append("<tr><td align='left'>Show Play Now Link</td><td>");
        out.append(htmlDropMenu(options, "filebrowser.ShowWsPlay", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('filebrowser.ShowWsPlay');\" width='24' height='24'></td></tr>\n");
        out.append("<tr><td colspan='3' align='left' style='border: 1px solid rgb(255, 255, 255);'>");
        out.append("<span class='areaTitle'>Server Settings</span>\n");
        out.append("</td></tr>\n");
        value = store.getProperty("server.kbLED");
        options = new HashMap<String, String>();
        options.put("0", "Disabled");
        options.put("1", "Enabled");
        out.append("<tr><td align='left'>Keyboard LED Control</td><td>");
        out.append(htmlDropMenu(options, "server.kbLED", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('server.kbLED');\" width='24' height='24'>");
        out.append("</td></tr>\n");
        value = store.getProperty("Schedule.wake.system");
        out.append("<tr><td align='left'>Seconds for system wake up</td><td>\n");
        out.append("<input type='text' name='Schedule.wake.system' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('Schedule.wake.system');\" width='24' height='24'></tr>\n");
        value = store.getProperty("AutoDel.KeepFor");
        out.append("<tr><td align='left'>Default keep for</td><td>\n");
        out.append("<input type='text' name='AutoDel.KeepFor' value='" + value + "' size='50'>\n");
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('AutoDel.KeepFor');\" width='24' height='24'></tr>\n");
        value = store.getProperty("EPG.ShowUnlinked");
        options = new HashMap<String, String>();
        options.put("0", "False");
        options.put("1", "True");
        out.append("<tr><td align='left'>Show Unlinked Schedules</td><td>");
        out.append(htmlDropMenu(options, "EPG.ShowUnlinked", value));
        out.append("</td><td><img style='cursor:hand;cursor:pointer;' border=0 src='/images/help24.png' alt='help' align='absmiddle' onClick=\"showHelp('EPG.ShowUnlinked');\" width='24' height='24'></tr>\n");
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "serverpropertie.html");
        template.replaceAll("$properties", out.toString());
        return template.getPageBytes();
    }

    private String htmlDropMenu(HashMap<String, String> values, String name, String selected) {
        String data = "<SELECT NAME='" + name + "'>\n";
        String[] keys = (String[]) values.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (int x = 0; x < keys.length; x++) {
            String marker = "";
            if (keys[x].equals(selected)) marker = " selected";
            String value = (String) values.get(keys[x]);
            data += "<OPTION VALUE='" + keys[x] + "'" + marker + ">" + value + "</OPTION>\n";
        }
        data += "</SELECT>\n";
        return data;
    }

    private byte[] showSystemInfo(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "systeminfo.html");
        template.replaceAll("$sysinfo", getSystemInfo(urlData));
        return template.getPageBytes();
    }

    private String getSystemInfo(HTTPurl urlData) {
        StringBuffer content = new StringBuffer();
        content.append("<table class='systemtable'>");
        content.append("<tr><td colspan='2' class='systemheading'>System Info</td></tr>");
        content.append("<tr><td class='systemkey'>Current Time</td><td class='systemdata'>" + dtf.format(new Date()) + "</td></tr>\n");
        Runtime r = Runtime.getRuntime();
        long total = r.totalMemory();
        long free = r.freeMemory();
        long freePercentage = (long) (((double) free / (double) total) * 100);
        content.append("<tr><td class='systemkey'>Memory</td><td class='systemdata'>" + freePercentage + "% Free</td></tr>\n");
        DllWrapper capEng = new DllWrapper();
        NumberFormat nf = NumberFormat.getNumberInstance();
        content.append("<tr><td class='systemkey'>Capture Paths</td><td class='systemdata' nowrap>");
        String[] paths = store.getCapturePaths();
        for (int x = 0; x < paths.length; x++) {
            String fullPath = new File(paths[x]).getAbsolutePath();
            long freeSpace = capEng.getFreeSpace(fullPath);
            content.append(fullPath);
            if (freeSpace == 0) {
                content.append(" (N/A)");
            } else {
                content.append(" (" + nf.format((freeSpace / (1024 * 1024))) + " MB Free)");
            }
            if (x != paths.length - 1) content.append("<br>");
        }
        content.append("</td></tr>\n");
        content.append("<tr><td class='systemkey'>Channels Loaded</td><td class='systemdata'>" + store.numberOfChannels());
        content.append("</td></tr>\n");
        File cap = new File(store.getProperty("path.httproot"));
        String fullPath = cap.getAbsolutePath();
        content.append("<tr><td class='systemkey'>Httpd Path</td><td class='systemdata'>" + fullPath + "</td></tr>\n");
        cap = new File(store.getProperty("path.data"));
        fullPath = cap.getAbsolutePath();
        content.append("<tr><td class='systemkey'>Data Path</td><td class='systemdata'>" + fullPath + "</td></tr>\n");
        content.append("<tr><td class='systemkey'>HTTP Server Version</td><td class='systemdata'>" + store.getVersion() + "</td></tr>\n");
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        content.append("<tr><td class='systemkey'>Number of Devices Selected</td><td class='systemdata'>" + devList.getDeviceCount() + "</td></tr>\n");
        content.append("</table>");
        return content.toString();
    }
}
