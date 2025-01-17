package ws.system;

import java.io.*;
import java.util.*;
import java.net.*;

class ChannelDataRes extends HTTPResponse {

    public ChannelDataRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream) throws Exception {
        if ("01".equals(urlData.getParameter("action"))) {
            outStream.write(showCountryList(urlData));
            return;
        } else if ("02".equals(urlData.getParameter("action"))) {
            outStream.write(showRegionList(urlData));
            return;
        } else if ("03".equals(urlData.getParameter("action"))) {
            outStream.write(showStationList(urlData));
            return;
        } else if ("04".equals(urlData.getParameter("action"))) {
            outStream.write(showScanResult(urlData));
            return;
        } else if ("05".equals(urlData.getParameter("action"))) {
            outStream.write(addSelectedPrograms(urlData));
            return;
        } else if ("06".equals(urlData.getParameter("action"))) {
            outStream.write(deleteChannel(urlData));
            return;
        } else if ("09".equals(urlData.getParameter("action"))) {
            outStream.write(showAddEditForm(urlData));
            return;
        } else if ("11".equals(urlData.getParameter("action"))) {
            outStream.write(updateChannel(urlData));
            return;
        } else if ("12".equals(urlData.getParameter("action"))) {
            outStream.write(showChannelImportForm(urlData));
            return;
        } else {
            outStream.write(showChannels(urlData));
            return;
        }
    }

    private byte[] showChannelImportForm(HTTPurl urlData) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ImportForm.html");
        String captcha = store.getProperty("security.captcha");
        if ("1".equals(captcha)) {
            template.replaceAll("$usingCAPTCHA$", "true");
        } else {
            template.replaceAll("$usingCAPTCHA$", "false");
        }
        template.replaceAll("$title", "Channel Data Import");
        template.replaceAll("$action", "/servlet/ChannelDataRes?action=13");
        return template.getPageBytes();
    }

    private byte[] showAddEditForm(HTTPurl urlData) throws Exception {
        String name = urlData.getParameter("ID");
        HashMap<String, Channel> channels = store.getChannels();
        Channel chan = null;
        if (name != null) {
            chan = (Channel) channels.get(name);
        }
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channel-details.html");
        if (chan != null) {
            template.replaceAll("$name", chan.getName());
            template.replaceAll("$chanOldName", chan.getName());
            template.replaceAll("$frequency", new Integer(chan.getFrequency()).toString());
            template.replaceAll("$bandwidth", new Integer(chan.getBandWidth()).toString());
            template.replaceAll("$programid", new Integer(chan.getProgramID()).toString());
            template.replaceAll("$videopid", new Integer(chan.getVideoPid()).toString());
            template.replaceAll("$audiopid", new Integer(chan.getAudioPid()).toString());
            String audioType = "";
            if (chan.getAudioType() == Channel.TYPE_AUDIO_MPG) {
                audioType = "<option value=\"1\" selected>MPG</option>\n" + "<option value=\"2\">AC3</option>\n";
            } else {
                audioType = "<option value=\"1\">MPG</option>\n" + "<option value=\"2\" selected>AC3</option>\n";
            }
            template.replaceAll("$audioType", audioType);
            template.replaceAll("$captureType", getCapTypeList(chan.getCaptureType()));
        } else {
            template.replaceAll("$name", "");
            template.replaceAll("$frequency", "");
            template.replaceAll("$bandwidth", "");
            template.replaceAll("$programid", "");
            template.replaceAll("$videopid", "");
            template.replaceAll("$audiopid", "");
            template.replaceAll("$chanOldName", "");
            String audioType = "<option value=\"1\" selected>MPG</option>\n" + "<option value=\"2\">AC3</option>\n";
            template.replaceAll("$audioType", audioType);
        }
        return template.getPageBytes();
    }

    private String getCapTypeList(int type) {
        StringBuffer buff = new StringBuffer(1024);
        Vector<CaptureCapability> capabilities = CaptureCapabilities.getInstance().getCapabilities();
        if (type == -1) buff.append("<option value='-1' selected>AutoSelect</option>\n"); else buff.append("<option value='-1'>AutoSelect</option>\n");
        for (int x = 0; x < capabilities.size(); x++) {
            CaptureCapability capability = capabilities.get(x);
            buff.append("<option value='" + capability.getTypeID() + "' ");
            if (type == capability.getTypeID()) buff.append("selected");
            buff.append(">" + capability.getName() + "</option>");
        }
        return buff.toString();
    }

    private byte[] deleteChannel(HTTPurl urlData) throws Exception {
        GuideStore guide = GuideStore.getInstance();
        String name = urlData.getParameter("ID");
        if (name != null) {
            store.removeChannel(name);
            store.saveChannels(null);
            boolean save = false;
            Vector<String[]> chanMap = guide.getChannelMap();
            for (int x = 0; x < chanMap.size(); x++) {
                String[] map = (String[]) chanMap.get(x);
                if (map[0].equals(name)) {
                    chanMap.remove(x);
                    x--;
                    save = true;
                }
            }
            if (save) guide.saveChannelMap(null);
        }
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] showChannels(HTTPurl urlData) throws Exception {
        StringBuffer out = new StringBuffer(4096);
        HashMap<String, Channel> channels = store.getChannels();
        String[] keys = (String[]) channels.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        Channel ch = null;
        for (int x = 0; x < keys.length; x++) {
            ch = (Channel) channels.get(keys[x]);
            String channelName = "";
            try {
                channelName = URLEncoder.encode(keys[x], "UTF-8");
            } catch (Exception e) {
            }
            out.append("<tr>");
            out.append("<td nowrap><span class='channelName'>" + keys[x] + "</span></td>");
            out.append("<td class='channelInfo'>" + ch.getFrequency() + "</td>\n");
            out.append("<td class='channelInfo'>" + ch.getBandWidth() + "</td>\n");
            out.append("<td class='channelInfo'>" + ch.getProgramID() + "</td>\n");
            out.append("<td class='channelInfo'>" + ch.getVideoPid() + "</td>\n");
            out.append("<td class='channelInfo'>" + ch.getAudioPid());
            if (ch.getAudioType() == Channel.TYPE_AUDIO_AC3) out.append("-AC3"); else out.append("-MPG");
            out.append("</td>\n");
            if (ch.getCaptureType() == -1) {
                out.append("<td class='channelInfo'>AutoSelect</td>\n");
            } else {
                CaptureCapabilities caps = CaptureCapabilities.getInstance();
                CaptureCapability cap = caps.getCapabiltyWithID(ch.getCaptureType());
                String capName = "ERROR";
                if (cap != null) capName = caps.getCapabiltyWithID(ch.getCaptureType()).getName();
                out.append("<td class='channelInfo'>" + capName + "</td>\n");
            }
            out.append("<td class='channelInfo'>");
            out.append(" <a onClick='return confirmAction(\"Delete\");' href='/servlet/" + urlData.getServletClass() + "?action=06&ID=" + channelName + "'>");
            out.append("<img border=0 src='/images/delete.png' alt='Delete Channel' align='absmiddle' width='24' height='24'>");
            out.append("</a>");
            out.append(" <a href='/servlet/" + urlData.getServletClass() + "?action=09&ID=" + channelName + "'>");
            out.append("<img border=0 src='/images/edit.png' alt='Edit Channel' align='absmiddle' width='24' height='24'>");
            out.append("</a> ");
            out.append("</td>\n");
            out.append("<tr>");
        }
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channels.html");
        template.replaceAll("$channels", out.toString());
        return template.getPageBytes();
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

    private byte[] addSelectedPrograms(HTTPurl urlData) throws Exception {
        int freq = 0;
        int band = 0;
        int progID = -1;
        int videoid = -1;
        int audioid = -1;
        int audioType = -1;
        String add = "";
        String name = "";
        int numAdded = 0;
        for (int x = 0; x < 1000; x++) {
            add = urlData.getParameter("add" + x);
            if (add != null) {
                name = urlData.getParameter("name" + x);
                if (name == null) name = "none";
                name = name.trim();
                name = checkName(name);
                progID = -1;
                try {
                    freq = Integer.parseInt(urlData.getParameter("freq" + x).trim());
                    band = Integer.parseInt(urlData.getParameter("band" + x).trim());
                    progID = Integer.parseInt(urlData.getParameter("programid" + x).trim());
                    videoid = Integer.parseInt(urlData.getParameter("videoid" + x).trim());
                    String audioData = urlData.getParameter("audioid" + x).trim();
                    String[] adSplit = audioData.split(":");
                    audioid = Integer.parseInt(adSplit[0]);
                    audioType = Integer.parseInt(adSplit[1]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (progID > -1) {
                    Channel chan = new Channel(name, freq, band, progID, videoid, audioid);
                    chan.setAudioType(audioType);
                    store.addChannel(chan);
                    numAdded++;
                }
            }
        }
        if (numAdded > 0) store.saveChannels(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] updateChannel(HTTPurl urlData) throws Exception {
        int freq = 0;
        int band = 0;
        int progID = -1;
        int videoid = -1;
        int audioid = -1;
        int audioType = -1;
        int capType = -1;
        String name = "";
        String oldName = urlData.getParameter("oldName");
        name = urlData.getParameter("name");
        if (name == null) name = "none";
        name = name.trim();
        name = checkName(name);
        GuideStore guide = GuideStore.getInstance();
        try {
            freq = Integer.parseInt(urlData.getParameter("freq").trim());
            band = Integer.parseInt(urlData.getParameter("band").trim());
            progID = Integer.parseInt(urlData.getParameter("programid").trim());
            videoid = Integer.parseInt(urlData.getParameter("videoid").trim());
            audioid = Integer.parseInt(urlData.getParameter("audioid").trim());
            audioType = Integer.parseInt(urlData.getParameter("audioType").trim());
            capType = Integer.parseInt(urlData.getParameter("captureType").trim());
        } catch (Exception e) {
            e.printStackTrace();
            Exception nEx = new Exception("Invalue Input");
            nEx.initCause(e);
            throw nEx;
        }
        Channel chan = new Channel(name, freq, band, progID, videoid, audioid);
        chan.setAudioType(audioType);
        chan.setCaptureType(capType);
        store.removeChannel(oldName);
        store.addChannel(chan);
        boolean save = false;
        Vector<String[]> chanMap = guide.getChannelMap();
        for (int x = 0; x < chanMap.size(); x++) {
            String[] map = (String[]) chanMap.get(x);
            if (map[0].equals(oldName)) {
                map[0] = name;
                save = true;
            }
        }
        if (save) {
            guide.saveChannelMap(null);
        }
        ScheduleItem[] schedules = store.getScheduleArray();
        save = false;
        for (int x = 0; x < schedules.length; x++) {
            ScheduleItem item = schedules[x];
            if (item.getState() == ScheduleItem.WAITING && item.getChannel().equals(oldName)) {
                item.setChannel(name);
                save = true;
            }
        }
        if (save) {
            store.saveSchedule(null);
        }
        store.saveChannels(null);
        StringBuffer out = new StringBuffer(256);
        out.append("HTTP/1.0 302 Moved Temporarily\n");
        out.append("Location: /servlet/" + urlData.getServletClass() + "\n\n");
        return out.toString().getBytes();
    }

    private byte[] showScanResult(HTTPurl urlData) throws Exception {
        int freq = 0;
        int band = 0;
        try {
            freq = Integer.parseInt(urlData.getParameter("freq"));
            band = Integer.parseInt(urlData.getParameter("band"));
        } catch (Exception e) {
            throw new Exception("Freq or Band not valid: " + e.toString());
        }
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channel-scanresult.html");
        CaptureDeviceList devList = CaptureDeviceList.getInstance();
        if (devList.getActiveDeviceCount() > 0) {
            template.replaceAll("$scanresult", "Can not scan channels while captures are active!");
            return template.getPageBytes();
        }
        if (devList.getDeviceCount() == 0) {
            template.replaceAll("$scanresult", "No Devices Available!");
            return template.getPageBytes();
        }
        String name = urlData.getParameter("name");
        StringBuffer buff = new StringBuffer(1024);
        buff.append("<form action='/servlet/" + urlData.getServletClass() + "' method='POST' accept-charset=\"UTF-8\">\n");
        buff.append("<input type='hidden' name='action' value='05'>\n");
        buff.append("<table class='channelScanResult'>\n");
        buff.append("<tr class='scanChannelHeading'>");
        buff.append("<td class='scanChannelHeadingDataName'>" + name + "</td>");
        buff.append("<td class='scanChannelHeadingData'>Program</td>");
        buff.append("<td class='scanChannelHeadingData'>Video</td>");
        buff.append("<td class='scanChannelHeadingData'>Audio</td>");
        buff.append("<td class='scanChannelHeadingData'>Add</td>");
        buff.append("</tr>\n");
        boolean testMode = "1".equals(store.getProperty("tools.testmode"));
        CaptureDevice cap = (CaptureDevice) devList.getDevice(0);
        Runtime runner = Runtime.getRuntime();
        String[] com = null;
        if (testMode == true) {
            String scanCommand = "win32/scan.exe -test";
            System.out.println("Running channel scan command: " + scanCommand);
            com = new String[2];
            com[0] = "win32/scan.exe";
            com[1] = "-test";
        } else {
            String scanCommand = "win32/scan.exe " + freq + " " + band + " \"" + cap.getID() + "\"";
            System.out.println("Running channel scan command: " + scanCommand);
            com = new String[4];
            com[0] = "win32/scan.exe";
            com[1] = new Integer(freq).toString();
            com[2] = new Integer(band).toString();
            com[3] = "\"" + cap.getID() + "\"";
        }
        Process scan = runner.exec(com);
        ScanResult result = new ScanResult(freq, band);
        result.readInput(scan.getInputStream());
        result.parseXML();
        int channelCount = 0;
        if (result.getResult().size() > 0) {
            Vector<Channel> scanResult = result.getResult();
            Channel chData = null;
            for (int x = 0; x < scanResult.size(); x++) {
                chData = (Channel) scanResult.get(x);
                if (chData != null) {
                    buff.append("<tr class='scanChannelResult'>");
                    buff.append("<td>\n<input type='text' name='name" + channelCount + "' value='" + checkName(chData.getName()) + "'>\n");
                    buff.append("<input type='hidden' name='freq" + channelCount + "' value='" + chData.getFrequency() + "'>\n");
                    buff.append("<input type='hidden' name='band" + channelCount + "' value='" + chData.getBandWidth() + "'>\n");
                    buff.append("</td>\n\n");
                    buff.append("<td align='center'>" + chData.getProgramID());
                    buff.append("<input type='hidden' name='programid" + channelCount + "' value='" + chData.getProgramID() + "'>\n");
                    buff.append("</td>\n");
                    buff.append("<td align='center'>");
                    int count = getStreamTypeCount(chData, Channel.TYPE_VIDEO);
                    if (count == 0) {
                        buff.append("-1");
                        buff.append("<input type='hidden' name='videoid" + channelCount + "' value='-1'>\n");
                    }
                    if (count == 1) {
                        int[] data = getFirstOfType(chData, Channel.TYPE_VIDEO);
                        buff.append(data[0]);
                        buff.append("<input type='hidden' name='videoid" + channelCount + "' value='" + data[0] + "'>\n");
                    } else if (count > 1) {
                        buff.append("<SELECT NAME='videoid" + channelCount + "'>\n");
                        for (int st = 0; st < chData.getStreams().size(); st++) {
                            int[] streamData = (int[]) chData.getStreams().get(st);
                            if (streamData[1] == Channel.TYPE_VIDEO) {
                                buff.append("<OPTION value=\"" + streamData[0] + "\">");
                                buff.append(streamData[0]);
                                buff.append("</OPTION>\n");
                            }
                        }
                        buff.append("</SELECT>");
                    }
                    buff.append("</td>\n");
                    buff.append("<td align='center'>");
                    count = getStreamTypeCount(chData, Channel.TYPE_AUDIO_AC3);
                    count += getStreamTypeCount(chData, Channel.TYPE_AUDIO_MPG);
                    if (count == 0) {
                        buff.append("-1");
                        buff.append("<input type='hidden' name='audioid" + channelCount + "' value='-1'>\n");
                    }
                    if (count == 1) {
                        int[] data = getFirstOfType(chData, Channel.TYPE_AUDIO_AC3);
                        if (data == null) data = getFirstOfType(chData, Channel.TYPE_AUDIO_MPG);
                        buff.append(data[0]);
                        if (data[1] == Channel.TYPE_AUDIO_AC3) buff.append(" AC3"); else buff.append(" MPG");
                        buff.append("<input type='hidden' name='audioid" + channelCount + "' value='" + data[0] + ":" + data[1] + "'>\n");
                    } else if (count > 1) {
                        buff.append("<SELECT NAME='audioid" + channelCount + "'>\n");
                        for (int st = 0; st < chData.getStreams().size(); st++) {
                            int[] streamData = (int[]) chData.getStreams().get(st);
                            if (streamData[1] == Channel.TYPE_AUDIO_MPG || streamData[1] == Channel.TYPE_AUDIO_AC3) {
                                buff.append("<OPTION value=\"" + streamData[0] + ":" + streamData[1] + "\">");
                                buff.append(streamData[0]);
                                if (streamData[1] == Channel.TYPE_AUDIO_AC3) buff.append(" AC3"); else buff.append(" MPG");
                                buff.append("</OPTION>\n");
                            }
                        }
                        buff.append("</SELECT>");
                    }
                    buff.append("</td>\n");
                    buff.append("<td align='center'><input type='checkbox' name='add" + channelCount + "' value='add'></td>\n");
                    buff.append("</tr>\n\n");
                    channelCount++;
                } else {
                    break;
                }
            }
        } else {
            buff.append("<tr class='scanChannelResult'>");
            buff.append("<td>No Programs Found</td>\n");
            buff.append("<td align='center'>N/A</td>\n");
            buff.append("<td align='center'>N/A</td>\n");
            buff.append("<td align='center'>N/A</td>\n");
            buff.append("<td align='center'>N/A</td>\n");
            buff.append("</tr>\n\n");
        }
        buff.append("</table><br>\n");
        if (channelCount > 0) buff.append("<input type='submit' value='Add Selected'>\n");
        buff.append("</form>\n");
        template.replaceAll("$scanresult", buff.toString());
        return template.getPageBytes();
    }

    private int getStreamTypeCount(Channel chan, int type) {
        Vector<int[]> streams = chan.getStreams();
        int count = 0;
        for (int x = 0; x < streams.size(); x++) {
            int[] streamData = (int[]) streams.get(x);
            if (streamData[1] == type) count++;
        }
        return count;
    }

    private int[] getFirstOfType(Channel chan, int type) {
        Vector<int[]> streams = chan.getStreams();
        for (int x = 0; x < streams.size(); x++) {
            int[] streamData = (int[]) streams.get(x);
            if (streamData[1] == type) return streamData;
        }
        return null;
    }

    private byte[] showStationList(HTTPurl urlData) throws Exception {
        int country = 0;
        int region = 0;
        try {
            country = Integer.parseInt(urlData.getParameter("country"));
            region = Integer.parseInt(urlData.getParameter("region"));
        } catch (Exception e) {
            throw new Exception("country or region code not valid: " + e.toString());
        }
        ChannelList list = new ChannelList(store.getProperty("path.data") + File.separator + "stationdata.list");
        StringBuffer buff = new StringBuffer(1024);
        Channel[] channelList = list.getStations(country, region);
        list.close();
        buff.append("<table><tr><td><ul>\n");
        for (int x = 0; x < channelList.length; x++) {
            if (channelList[x] != null) {
                buff.append("<li>\n");
                buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=04&freq=" + channelList[x].getFrequency());
                buff.append("&band=" + channelList[x].getBandWidth() + "&name=" + channelList[x].getName() + "' class='noUnder'>");
                buff.append(channelList[x].getName() + "</a>\n");
                buff.append("</li>\n");
            }
        }
        buff.append("</ul></td></tr></table>\n");
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channel-stationlist.html");
        template.replaceAll("$stations", buff.toString());
        return template.getPageBytes();
    }

    private byte[] showRegionList(HTTPurl urlData) throws Exception {
        int country = 0;
        try {
            country = Integer.parseInt(urlData.getParameter("country"));
        } catch (Exception e) {
            throw new Exception("country code not valid: " + e.toString());
        }
        ChannelList list = new ChannelList(store.getProperty("path.data") + File.separator + "stationdata.list");
        StringBuffer buff = new StringBuffer(1024);
        String[] regionList = list.getRegions(country);
        list.close();
        buff.append("<table><tr><td><ul>\n");
        for (int x = 0; x < regionList.length; x++) {
            if (regionList[x] != null) {
                buff.append("<li>\n");
                buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=03&country=" + country + "&region=" + x + "' class='noUnder'>" + regionList[x] + "</a>\n");
                buff.append("</li>\n");
            }
        }
        buff.append("</ul></td></tr></table>\n");
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channel-regionlist.html");
        template.replaceAll("$regions", buff.toString());
        return template.getPageBytes();
    }

    private byte[] showCountryList(HTTPurl urlData) throws Exception {
        ChannelList list = new ChannelList(store.getProperty("path.data") + File.separator + "stationdata.list");
        StringBuffer buff = new StringBuffer(1024);
        String[] countList = list.getCountries();
        list.close();
        buff.append("<table><tr><td><ul>\n");
        for (int x = 0; x < countList.length; x++) {
            if (countList[x] != null) {
                buff.append("<li>\n");
                buff.append("<a href='/servlet/" + urlData.getServletClass() + "?action=02&country=" + x + "' class='noUnder'>" + countList[x] + "</a>\n");
                buff.append("</li>\n");
            }
        }
        buff.append("</ul></td></tr></table>\n");
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "channel-countrylist.html");
        template.replaceAll("$countries", buff.toString());
        return template.getPageBytes();
    }
}
