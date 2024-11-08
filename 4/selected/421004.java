package com.google.code.sagetvaddons.sagerss.server;

import gkusnick.sagetv.api.API;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viecili.jrss.generator.RSSFeedGenerator;
import viecili.jrss.generator.RSSFeedGeneratorFactory;
import viecili.jrss.generator.elem.Channel;
import viecili.jrss.generator.elem.Item;
import viecili.jrss.generator.elem.RSS;
import com.google.code.sagetvaddons.sagerss.client.SageShow;
import com.google.code.sagetvaddons.sagerss.client.ShowsQueryResponse;

public final class SearchShowsRss extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 741464088322185052L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/rss+xml");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Cache-Control", "no-cache, must-revalidate");
        resp.getWriter().write(generateRssFeed(req));
    }

    private String generateRssFeed(HttpServletRequest req) throws IOException {
        ShowsQueryResponse resp = new SearchShowsSearcher(req.getParameter("q")).getResult();
        RSS rss = new RSS();
        StringBuffer chURL = req.getRequestURL();
        chURL.append("?" + req.getQueryString());
        Channel chan = new Channel("SageRSS: SageTV Custom Search Results", chURL.toString(), "Results of a custom SageRSS search query");
        chan.setLanguage("en-us");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long today = c.getTimeInMillis();
        chan.setTtl(Integer.parseInt(Long.toString((today + 86400000 - System.currentTimeMillis()) / 60000)));
        chan.setPubDate(new Date(today));
        long lastEpgDload = API.apiNullUI.global.GetLastEPGDownloadTime();
        if (lastEpgDload < today) lastEpgDload = today;
        chan.setLastBuildDate(new Date(lastEpgDload));
        rss.addChannel(chan);
        for (SageShow s : resp.getResults()) {
            String iTitle = s.getTitle();
            String subtitle = s.getSubtitle();
            if (subtitle != null && subtitle.length() > 0) iTitle = iTitle.concat(": " + s.getSubtitle());
            String year = s.getYear();
            if (year != null && year.length() > 0) iTitle = iTitle.concat(" (" + year + ")");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            StringWriter iDesc = new StringWriter();
            iDesc.write("<div style=\"font-size:x-small;margin-top:1px;margin-bottom:5px;\"><b>Next Airing:</b> " + fmt.format(s.getStart()) + " on " + s.getChannel() + "</div>");
            iDesc.write("<div>" + s.getDescription() + "</div><div><b>" + s.getCategory());
            if (s.getSubcategory() != null && s.getSubcategory().length() > 0) iDesc.write("/" + s.getSubcategory());
            iDesc.write("</b></div>\n");
            Item i = new Item(iTitle, String.format(resp.getAiringUrl(), s.getAiringId()), iDesc.toString());
            iDesc.close();
            i.setGuid(i.new Guid(s.getId(), false));
            chan.addItem(i);
        }
        RSSFeedGenerator feed = RSSFeedGeneratorFactory.getDefault();
        return feed.generateAsString(rss);
    }
}
