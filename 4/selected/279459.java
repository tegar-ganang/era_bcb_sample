package net.sourceforge.recman.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.Duration;
import net.sourceforge.recman.backend.manager.RecordingDao;
import net.sourceforge.recman.backend.manager.exception.DaoException;
import net.sourceforge.recman.backend.manager.pojo.Recording;
import net.sourceforge.recman.web.util.UrlGenerator;
import org.jboss.resteasy.plugins.providers.atom.Content;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.plugins.providers.atom.Link;
import org.jboss.resteasy.plugins.providers.atom.Person;

/**
 * Atom Feeds for new recordings
 * 
 * @author Marcus Kessel
 * 
 */
@Path("recman/1.0")
public class AtomResource extends AbstractResource {

    private static final int WIDTH = 320;

    private static final int HEIGTH = 240;

    private static final String IMAGE_STRING = "\" width=\"" + WIDTH + "\" height=\"" + HEIGTH + "\" alt=\"Preview\" title=\"Preview\" />";

    private RecordingDao recordingDao;

    public AtomResource(RecordingDao recordingDao) {
        this.recordingDao = recordingDao;
    }

    /**
     * Get feed
     * 
     * @param req
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Path("/atom/feed")
    @Produces("application/atom+xml")
    public Feed getFeed(@Context HttpServletRequest req) throws URISyntaxException {
        List<Recording> recList = Collections.emptyList();
        try {
            recList = this.recordingDao.loadAll(null, null, "desc", this.getStreamUrl(req)).getRecordings();
        } catch (DaoException e) {
            e.printStackTrace();
        }
        Feed feed = new Feed();
        feed.setTitle("Recman - VDR Recordings");
        feed.setSubtitle("New VDR Recordings");
        feed.setUpdated(new GregorianCalendar(TimeZone.getTimeZone("CET"), Locale.GERMANY).getTime());
        feed.getAuthors().add(new Person("recman web-manager"));
        for (Recording recording : recList) {
            Date date = recording.getStartDate().toGregorianCalendar().getTime();
            URI streamURI = new URI(recording.getStreamUrl());
            Entry entry = new Entry();
            entry.setTitle(recording.getTitle());
            Content content = new Content();
            content.setType(MediaType.TEXT_HTML_TYPE);
            content.setText(this.generateHtml(recording, req));
            entry.setPublished(date);
            entry.setContent(content);
            entry.setUpdated(date);
            entry.setId(streamURI);
            entry.getLinks().add(new Link("Stream URL", streamURI));
            feed.getEntries().add(entry);
        }
        return feed;
    }

    /**
     * Generate HTML for feed entry content
     * 
     * @param recording
     * @return
     */
    private String generateHtml(Recording recording, HttpServletRequest req) {
        StringBuffer sb = new StringBuffer();
        String thumbnailSrc = UrlGenerator.generateImgBaseUrl(req) + recording.getId() + "/1.png";
        sb.append("<img src=\"");
        sb.append(thumbnailSrc);
        sb.append(IMAGE_STRING);
        this.addInfo(sb, recording);
        this.newLine(sb, recording.getShortText());
        this.newLine(sb, recording.getDescription());
        this.newLine(sb, recording.getStreamUrl());
        return sb.toString();
    }

    /**
     * Make new HTML "
     * <p>
     * " tag with content
     * 
     * @param sb
     * @param content
     */
    private void newLine(StringBuffer sb, String content) {
        if (content == null || content.equals("")) {
            return;
        }
        sb.append("<p>");
        sb.append(content);
        sb.append("</p>");
    }

    /**
     * Add extra info
     * 
     * @param sb
     * @param recording
     */
    private void addInfo(StringBuffer sb, Recording recording) {
        Duration duration = recording.getDuration();
        Duration totalDuration = recording.getTotalDuration();
        String info = recording.getChannelName() + ", " + duration.getHours() + ":" + fillWithSpace(duration.getMinutes()) + " (" + totalDuration.getHours() + ":" + fillWithSpace(totalDuration.getMinutes()) + ")";
        this.newLine(sb, info);
    }

    /**
     * Put a space in front of a one digit number.
     * 
     * @param number
     * @return
     */
    private String fillWithSpace(int number) {
        String temp = Integer.toString(number);
        if (temp.length() == 1) {
            return "0" + temp;
        }
        return temp;
    }
}
