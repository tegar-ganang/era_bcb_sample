package com.ecyrd.jspwiki.rss;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.ServletContext;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.ecyrd.jspwiki.Release;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Represents an RSS 2.0 feed (with enclosures).  This feed provides no
 *  fizz-bang features.
 *
 *  @since 2.2.27
 */
public class RSS20Feed extends Feed {

    /**
     *  Creates an RSS 2.0 feed for the specified Context.
     *  
     *  @param context The WikiContext.
     */
    public RSS20Feed(WikiContext context) {
        super(context);
    }

    private List getItems() {
        ArrayList<Element> list = new ArrayList<Element>();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        WikiEngine engine = m_wikiContext.getEngine();
        ServletContext servletContext = null;
        if (m_wikiContext.getHttpRequest() != null) servletContext = m_wikiContext.getHttpRequest().getSession().getServletContext();
        for (Iterator i = m_entries.iterator(); i.hasNext(); ) {
            Entry e = (Entry) i.next();
            WikiPage p = e.getPage();
            String url = e.getURL();
            Element item = new Element("item");
            item.addContent(new Element("link").setText(url));
            item.addContent(new Element("title").setText(e.getTitle()));
            item.addContent(new Element("description").setText(e.getContent()));
            if (engine.getAttachmentManager().hasAttachments(p) && servletContext != null) {
                try {
                    Collection c = engine.getAttachmentManager().listAttachments(p);
                    for (Iterator a = c.iterator(); a.hasNext(); ) {
                        Attachment att = (Attachment) a.next();
                        Element attEl = new Element("enclosure");
                        attEl.setAttribute("url", engine.getURL(WikiContext.ATTACH, att.getName(), null, true));
                        attEl.setAttribute("length", Long.toString(att.getSize()));
                        attEl.setAttribute("type", getMimeType(servletContext, att.getFileName()));
                        item.addContent(attEl);
                    }
                } catch (ProviderException ex) {
                }
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(p.getLastModified());
            cal.add(Calendar.MILLISECOND, -(cal.get(Calendar.ZONE_OFFSET) + (cal.getTimeZone().inDaylightTime(p.getLastModified()) ? cal.get(Calendar.DST_OFFSET) : 0)));
            item.addContent(new Element("pubDate").setText(fmt.format(cal.getTime())));
            list.add(item);
        }
        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getString() {
        WikiEngine engine = m_wikiContext.getEngine();
        Element root = new Element("rss");
        root.setAttribute("version", "2.0");
        Element channel = new Element("channel");
        root.addContent(channel);
        channel.addContent(new Element("title").setText(getChannelTitle()));
        channel.addContent(new Element("link").setText(engine.getBaseURL()));
        channel.addContent(new Element("description").setText(getChannelDescription()));
        channel.addContent(new Element("language").setText(getChannelLanguage()));
        channel.addContent(new Element("generator").setText("JSPWiki " + Release.VERSTR));
        String mail = engine.getVariable(m_wikiContext, RSSGenerator.PROP_RSS_AUTHOREMAIL);
        if (mail != null) {
            String editor = engine.getVariable(m_wikiContext, RSSGenerator.PROP_RSS_AUTHOR);
            if (editor != null) mail = mail + " (" + editor + ")";
            channel.addContent(new Element("managingEditor").setText(mail));
        }
        channel.addContent(getItems());
        XMLOutputter output = new XMLOutputter();
        output.setFormat(Format.getPrettyFormat());
        try {
            StringWriter res = new StringWriter();
            output.output(root, res);
            return res.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
