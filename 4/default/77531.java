import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

public class ArchiveDataRes extends HTTPResponse {

    public ArchiveDataRes() throws Exception {
        super();
    }

    public void getResponse(HTTPurl urlData, OutputStream outStream) throws Exception {
        String action = urlData.getParameter("action");
        Method m = this.getClass().getMethod(action, new Class[] { HTTPurl.class, OutputStream.class });
        Object ret = m.invoke(this, urlData, outStream);
        outStream.write((byte[]) ret);
    }

    @SuppressWarnings("unused")
    public byte[] showItemInfo(HTTPurl urlData, OutputStream outStream) throws Exception {
        String itemFile = urlData.getParameter("file");
        ScheduleItem item = null;
        FileInputStream fis = new FileInputStream(itemFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        item = (ScheduleItem) ois.readObject();
        ois.close();
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ArchiveItemInfo.html");
        template.replaceAll("$info", getScheduleInfo(item, itemFile));
        return template.getPageBytes();
    }

    private String getScheduleInfo(ScheduleItem item, String itemFile) throws Exception {
        DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
        StringBuffer data = new StringBuffer(2048);
        data.append("<p style='border: solid 1px #FFFFFF; padding: 3px; width: 100%;'>Schedule Details:</p>\n");
        data.append("Current State : " + item.getStatus() + " (" + item.getState() + ")<p>");
        String type = "? " + item.getType() + " ?";
        if (item.getType() == ScheduleItem.ONCE) type = "Once"; else if (item.getType() == ScheduleItem.DAILY) type = "Daily"; else if (item.getType() == ScheduleItem.WEEKLY) type = "Weekly"; else if (item.getType() == ScheduleItem.MONTHLY) type = "Monthly"; else if (item.getType() == ScheduleItem.WEEKDAY) type = "Week Day"; else if (item.getType() == ScheduleItem.EPG) type = "EPG";
        data.append("This Scheduled Item is set to trigger " + type + " <p>");
        data.append("<table>");
        data.append("<tr><td>Start</td><td>" + dtf.format(item.getStart()) + "</td></tr>");
        data.append("<tr><td>Stop</td><td>" + dtf.format(item.getStop()) + "</td></tr>");
        data.append("<tr><td>Duration</td><td>" + item.getDuration() + "</td></tr>");
        data.append("<tr><td>Channel</td><td>" + item.getChannel() + "</td></tr>");
        data.append("</table><p>");
        data.append("Name Pattern : " + item.getFilePattern() + "<br>");
        String capName = item.getFileName();
        data.append("File : " + capName + "<br>");
        data.append("<p>");
        if (item.getCreatedFrom() != null) {
            data.append("Created From :<pre>");
            data.append("Title      : " + item.getCreatedFrom().getName() + "\n");
            data.append("Sub Title  : " + item.getCreatedFrom().getSubName() + "\n");
            data.append("Start      : " + item.getCreatedFrom().getStart().toString() + "\n");
            data.append("Duration   : " + item.getCreatedFrom().getDuration() + "\n");
            data.append("</pre><p>");
        }
        Vector<CaptureCapability> capabilities = CaptureCapabilities.getInstance().getCapabilities();
        String capType = "ERROR";
        if (item.getCapType() == -1) {
            capType = "AutoSelect";
        } else {
            for (int x = 0; x < capabilities.size(); x++) {
                CaptureCapability capability = capabilities.get(x);
                if (capability.getTypeID() == item.getCapType()) capType = capability.getName();
            }
        }
        data.append("Capture Type : " + capType + "<p>");
        data.append("Is Auto Deletable : " + item.isAutoDeletable() + "<br>");
        data.append("Keep for : " + item.getKeepFor() + " days before auto deleting.<p>");
        data.append("Post Capture Task : " + item.getPostTask() + "<p>");
        data.append("<p style='border: solid 1px #FFFFFF; padding: 3px; width: 100%;'>Signal Statistics:</p>\n");
        data.append("<table cellpadding='2' cellspacing='2'>\n");
        data.append("<tr><td>&nbsp;</td>");
        data.append("<td>Strength</td>");
        data.append("<td>Quality</td></tr>\n");
        HashMap<Date, SignalStatistic> stats = item.getSignalStatistics();
        Date[] keys = stats.keySet().toArray(new Date[0]);
        Arrays.sort(keys);
        NumberFormat nf2Dec = NumberFormat.getInstance();
        nf2Dec.setMaximumFractionDigits(2);
        double strengthMIN = -1;
        double strengthAVG = 0;
        double strengthMAX = -1;
        double qualityMIN = -1;
        double qualityAVG = 0;
        double qualityMAX = -1;
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
        data.append("<tr><td align='left'>Minimum</td>");
        data.append("<td align='center'>" + nf2Dec.format(strengthMIN) + "</td>");
        data.append("<td align='center'>" + nf2Dec.format(qualityMIN) + "</td></tr>\n");
        data.append("<tr><td align='left'>Average</td>");
        data.append("<td align='center'>" + nf2Dec.format(strengthAVG) + "</td>");
        data.append("<td align='center'>" + nf2Dec.format(qualityAVG) + "</td></tr>\n");
        data.append("<tr><td align='left'>Maximum</td>");
        data.append("<td align='center'>" + nf2Dec.format(strengthMAX) + "</td>");
        data.append("<td align='center'>" + nf2Dec.format(qualityMAX) + "</td></tr>\n");
        data.append("</table>\n");
        if (keys.length > 0) {
            data.append("<ul>\n");
            data.append("<li><a class='nounder' href='/servlet/SignalStatisticsImageDataRes?action=01&file=" + URLEncoder.encode(itemFile, "UTF-8") + "&data=strength'>Show Signal Strength Graph</a></li>\n");
            data.append("<li><a class='nounder' href='/servlet/SignalStatisticsImageDataRes?action=01&file=" + URLEncoder.encode(itemFile, "UTF-8") + "&data=quality'>Show Signal Quality Graph</a></li>\n");
            data.append("</ul>\n");
        }
        data.append("<p style='border: solid 1px #FFFFFF; padding: 3px; width: 100%;'>Schedule Log:</p>\n");
        String log = item.getLog();
        data.append("<pre class='log'>" + log + "</pre>");
        return data.toString();
    }

    @SuppressWarnings("unused")
    public byte[] showArchive(HTTPurl urlData, OutputStream outStream) throws Exception {
        PageTemplate template = new PageTemplate(store.getProperty("path.template") + File.separator + "ArchiveList.html");
        StringBuffer buff = new StringBuffer();
        File outFile = new File(new DllWrapper().getAllUserPath() + "archive");
        if (outFile.exists() == false) outFile.mkdirs();
        File[] files = outFile.listFiles();
        Arrays.sort(files);
        for (int x = files.length - 1; files != null && x >= 0; x--) {
            File archiveFile = files[x];
            if (archiveFile.isDirectory() == false && archiveFile.getName().startsWith("Schedule-")) {
                buff.append("<tr>\n");
                buff.append("<td><a class='noUnder' href='/servlet/ArchiveDataRes?action=showItemInfo&file=" + URLEncoder.encode(archiveFile.getCanonicalPath(), "UTF-8") + "'>" + archiveFile.getName() + "</a></td>\n");
                buff.append("</tr>\n");
            }
        }
        template.replaceAll("$ArchiveList", buff.toString());
        return template.getPageBytes();
    }
}
