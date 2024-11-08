package net.sf.sageplugins.webserver;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.SystemMessageApi;
import viecili.jrss.generator.RSSFeedGeneratorFactory;
import viecili.jrss.generator.elem.Channel;
import viecili.jrss.generator.elem.Item;
import viecili.jrss.generator.elem.RSS;

public class RssServlet extends SageServlet {

    private static final long serialVersionUID = -4340435187492421102L;

    protected void doServletGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String port = ":" + req.getServerPort();
        if (((req.getScheme().toLowerCase().equals("http")) && (req.getServerPort() == 80)) || ((req.getScheme().toLowerCase().equals("https")) && (req.getServerPort() == 443))) {
            port = "";
        }
        String baseURL = req.getScheme() + "://" + req.getServerName() + port + "/sage";
        RSS rss = new RSS();
        try {
            if (req.getPathInfo() != null) {
                if (req.getPathInfo().startsWith("/RecordingSchedule")) {
                    Channel channel = new Channel("Sage Recording Schedule", baseURL + "/RecordingSchedule", "The programs to be recorded by Sage in the next few days");
                    rss.addChannel(channel);
                    Object filelist = SageApi.Api("GetScheduledRecordings");
                    AddShowsToChannel(baseURL, channel, filelist, req, resp);
                } else if (req.getPathInfo().startsWith("/IRSuggestions")) {
                    Channel channel = new Channel("Sage Intelligent Suggestions", baseURL + "/IRSuggestions", "Programs Sage thinks that you might be interested in recording over the next few days");
                    rss.addChannel(channel);
                    Object filelist = SageApi.Api("GetSuggestedIntelligentRecordings");
                    AddShowsToChannel(baseURL, channel, filelist, req, resp);
                } else if (req.getPathInfo().equals("/Conflicts")) {
                    Channel channel = new Channel("Sage Recording Conflicts", baseURL + "/Conflicts", "Programs that will not be recorded because they conflict with other recordings");
                    rss.addChannel(channel);
                    AddConflictsToChannel(baseURL, channel, req, resp);
                } else if (req.getPathInfo().equals("/Search")) {
                    Search search = new Search();
                    search.setProperties(req);
                    Object searchResults = search.doSearch();
                    String searchName = req.getParameter("title");
                    if (searchName == null) searchName = search.getSearchName();
                    Channel channel = new Channel(searchName, baseURL + "/Search?" + req.getQueryString(), "Search results from Sage Database");
                    rss.addChannel(channel);
                    AddShowsToChannel(baseURL, channel, searchResults, req, resp);
                } else if (req.getPathInfo().startsWith("/SystemMessages")) {
                    Channel channel = new Channel("Sage System Messages", baseURL + "/SystemMessages", "System messages reported by SageTV");
                    rss.addChannel(channel);
                    AddSystemMessagesToChannel(baseURL, channel, req, resp);
                } else {
                    throw new IllegalArgumentException("No such feed: " + req.getPathInfo());
                }
            } else {
                throw new IllegalArgumentException("No feed name passed");
            }
        } catch (Exception e) {
            log("Exception while processing servlet " + this.getClass().getName(), e);
            System.out.println(e.toString());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println();
            out.println();
            out.println("<body><pre>");
            out.println("Exception while generating RSS:\r\n" + e.toString());
            e.printStackTrace(out);
            out.println("</pre>");
            out.close();
            return;
        }
        ReturnFeed(rss, req, resp);
    }

    private void AddShowsToChannel(String baseURL, Channel channel, Object airings, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        channel.setPubDate(new Date());
        channel.setTtl(SageApi.GetIntProperty("nielm/webserver/rss_ttl_mins", 360));
        channel.setLastBuildDate(new Date());
        for (int i = 0; i < SageApi.Size(airings); i++) {
            Airing airing = new Airing(SageApi.GetElement(airings, i));
            String title = "\"" + airing.getTitle();
            String episode = airing.getEpisode();
            if (episode != null && episode.length() > 0) title = title + " - " + episode;
            title = title + "\"";
            String link = baseURL + "/DetailedInfo?" + airing.getIdArg();
            Date startDate = airing.getStartDate();
            String dateStr = DateFormat.getDateInstance().format(startDate) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(startDate);
            title = title + " at " + dateStr + " on " + airing.getChannel();
            String description;
            String showdesc = (String) SageApi.Api("GetShowDescription", airing.sageAiring);
            if (showdesc != null && showdesc.length() > 0) description = showdesc; else description = "";
            String categ = (String) SageApi.Api("GetShowCategory", airing.sageAiring);
            if (categ != null && categ.length() > 0) {
                description = description + " (Category: " + categ;
                String subcateg = (String) SageApi.Api("GetShowSubCategory", airing.sageAiring);
                if (subcateg != null && subcateg.length() > 0) description = description + "/" + subcateg;
                description = description + ")";
            }
            Item item = new Item(title, link, description);
            item.setGuid(item.new Guid(link, true));
            item.setPubDate(startDate);
            if (airing.idType == Airing.ID_TYPE_MEDIAFILE) {
                File file = (File) SageApi.Api("GetFileForSegment", new Object[] { airing.sageAiring, new Integer(0) });
                if (file != null && file.canRead() && file.length() > 0) {
                    String url = baseURL + "public/MediaFile/" + file.getName() + "?MediaFileId=" + airing.id + "&amp;Segment=0";
                    String mimeType = this.getServletContext().getMimeType(file.getName());
                    if (mimeType == null) {
                        mimeType = "text/plain";
                    }
                    Item.Enclosure enc = item.new Enclosure(url, file.length(), mimeType);
                    item.setEnclosure(enc);
                }
            }
            channel.addItem(item);
        }
    }

    private void AddConflictsToChannel(String baseURL, Channel channel, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        channel.setPubDate(new Date());
        channel.setTtl(SageApi.GetIntProperty("nielm/webserver/rss_ttl_mins", 360));
        channel.setLastBuildDate(new Date());
        Object UnresolvedConflicts = SageApi.Api("GetAiringsThatWontBeRecorded", new Object[] { Boolean.TRUE });
        Object airings = SageApi.Api("GetAiringsThatWontBeRecorded", new Object[] { Boolean.FALSE });
        airings = SageApi.Api("Sort", new Object[] { airings, Boolean.FALSE, "GetAiringStartTime" });
        for (int i = 0; i < SageApi.Size(airings); i++) {
            Object SageAiring = SageApi.GetElement(airings, i);
            Airing airing = new Airing(SageAiring);
            String title;
            if (SageApi.Size(SageApi.Api("DataIntersection", new Object[] { UnresolvedConflicts, SageAiring })) > 0) {
                title = "UNRESOLVED: ";
            } else {
                title = "RESOLVED: ";
            }
            title = title + "\"" + airing.getTitle();
            String episode = airing.getEpisode();
            if (episode != null && episode.length() > 0) title = title + " - " + episode;
            title = title + "\"";
            String link = baseURL + "/Conflicts#AiringId" + Integer.toString(airing.id);
            Date startDate = airing.getStartDate();
            String dateStr = DateFormat.getDateInstance().format(startDate) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(startDate);
            title = title + " at " + dateStr + " on " + airing.getChannel();
            String description;
            String showdesc = (String) SageApi.Api("GetShowDescription", airing.sageAiring);
            if (showdesc != null && showdesc.length() > 0) description = showdesc; else description = "";
            String categ = (String) SageApi.Api("GetShowCategory", airing.sageAiring);
            if (categ != null && categ.length() > 0) {
                description = description + " (Category: " + categ;
                String subcateg = (String) SageApi.Api("GetShowSubCategory", airing.sageAiring);
                if (subcateg != null && subcateg.length() > 0) description = description + "/" + subcateg;
                description = description + ")";
            }
            Item item = new Item(title, link, description);
            item.setGuid(item.new Guid(link));
            item.setPubDate(startDate);
            channel.addItem(item);
        }
    }

    private void AddSystemMessagesToChannel(String baseURL, Channel channel, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        channel.setPubDate(new Date());
        channel.setTtl(SageApi.GetIntProperty("nielm/webserver/rss_ttl_mins", 360));
        channel.setLastBuildDate(new Date());
        Object messageList = SageApi.Api("GetSystemMessages");
        for (int i = 0; i < SageApi.Size(messageList); i++) {
            Object message = SageApi.GetElement(messageList, i);
            int level = SageApi.IntApi("GetSystemMessageLevel", new Object[] { message });
            String messageString = SageApi.StringApi("GetSystemMessageString", new Object[] { message });
            int typeCode = SageApi.IntApi("GetSystemMessageTypeCode", new Object[] { message });
            String typeName = SageApi.StringApi("GetSystemMessageTypeName", new Object[] { message });
            int count = SageApi.IntApi("GetSystemMessageRepeatCount", new Object[] { message });
            count = Math.max(1, count);
            Long time = (Long) SageApi.Api("GetSystemMessageTime", new Object[] { message });
            Long endTime = (Long) SageApi.Api("GetSystemMessageEndTime", new Object[] { message });
            String messageKey = SystemMessageApi.getKey(message);
            String messageUid = SystemMessageApi.getUid(message);
            String title = SystemMessageApi.ALERT_LEVEL_TEXT_PREFIX[level] + " - " + typeName + " - " + count + ((count == 1) ? " Message" : " Messages");
            String link = baseURL + "/SystemMessages?" + messageUid + "#" + messageKey;
            String description = messageString;
            Item item = new Item(title, link, description);
            item.setGuid(item.new Guid(link));
            item.setPubDate(new Date(endTime));
            channel.addItem(item);
        }
    }

    private void ReturnFeed(RSS rss, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setContentType("application/rss+xml; charset=" + charset);
        noCacheHeaders(resp);
        resp.setBufferSize(8192);
        OutputStream outs = getGzippedOutputStream(req, resp);
        try {
            RSSFeedGeneratorFactory.getDefault().generateToStream(rss, outs);
            outs.close();
        } catch (Throwable e) {
            log("Exception while processing servlet " + this.getClass().getName(), e);
            System.out.println(e.toString());
            e.printStackTrace();
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/html");
                PrintWriter out = new PrintWriter(outs);
                out.println();
                out.println();
                out.println("<body><pre>");
                out.println("Exception while processing servlet:\r\n" + e.toString());
                e.printStackTrace(out);
                out.println("</pre>");
                out.close();
                outs.close();
            }
        }
    }
}
