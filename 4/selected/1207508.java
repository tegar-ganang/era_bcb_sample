package org.wportal.rss.parse;

import org.wportal.search.Searchable;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import java.io.Serializable;
import java.util.Date;

public class RSSItem implements Serializable, Searchable {

    private String id = null;

    private String title;

    private String content;

    private String link;

    private Date pubDate;

    private Date fetchDate;

    private RSSChannel channel;

    public Document getDocument() {
        Document doc = new Document();
        if (content != null) {
            doc.add(Field.Text("content", content));
        }
        if (title != null) {
            doc.add(Field.UnIndexed("title", title));
        }
        if (fetchDate != null) doc.add(Field.Keyword("createdTime", fetchDate));
        doc.add(Field.Text("version", "New"));
        doc.add(Field.Keyword("type", "RSS"));
        return doc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public Date getFetchDate() {
        return fetchDate;
    }

    public void setFetchDate(Date fetchDate) {
        this.fetchDate = fetchDate;
    }

    public RSSChannel getChannel() {
        return channel;
    }

    public void setChannel(RSSChannel channel) {
        this.channel = channel;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof RSSItem)) return false;
        if (link == null) return false;
        return link.equals(((RSSItem) o).getLink());
    }
}
