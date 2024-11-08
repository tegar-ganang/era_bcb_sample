package de.cinek.rssview;

import java.util.Date;
import org.apache.commons.lang.builder.EqualsBuilder;

public class RssArticle implements Article {

    private String title;

    private String link;

    private String description;

    private boolean read;

    private Date publishingDate;

    private int id;

    private Channel channel;

    public RssArticle(String title, String link, String description, Date pubdate) {
        this.title = (title == null) ? null : title.trim();
        this.link = (link == null) ? null : link.trim();
        this.description = (description == null) ? null : description.trim();
        this.read = false;
        this.publishingDate = (pubdate == null ? new Date() : pubdate);
    }

    public RssArticle() {
        this(null, null, null, null);
    }

    public boolean isDescriptionSet() {
        return (description != null && description.length() > 0);
    }

    public boolean isTitleSet() {
        return (title != null && title.length() > 0);
    }

    /**
	 * Overrides equal to compare link and title of article.
	 * @param obj to compare to
	 * @return true if link and title are equal else false
	 */
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Article) {
            RssArticle other = ((RssArticle) obj);
            return new EqualsBuilder().append(link, other.link).append(title, other.title).isEquals();
        }
        return false;
    }

    public String toString() {
        return "<article>" + title + "</article>";
    }

    /**
	 * Getter for property pubdate.
	 * @return Value of property pubdate.
	 */
    public Date getPublishingDate() {
        return publishingDate;
    }

    /**
	 * Setter for property pubdate.
	 * @param pubdate New value of property pubdate.
	 */
    public void setPublishingDate(Date pubdate) {
        this.publishingDate = pubdate;
    }

    public String getTitle() {
        return title;
    }

    /**
	 * Setter for property title.
	 * @param title New value of property title.
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    /**
	 * Setter for property link.
	 * @param link New value of property link.
	 */
    public void setLink(String link) {
        this.link = link;
    }

    /**
	 * Getter for property description.
	 * @return Value of property description.
	 */
    public String getDescription() {
        return description;
    }

    /**
	 * Setter for property description.
	 * @param description New value of property description.
	 */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
	 * Getter for property read.
	 * @return Value of property read.
	 */
    public boolean isRead() {
        return read;
    }

    /**
	 * Setter for property read.
	 * @param read New value of property read.
	 */
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
	 * @return Returns the id.
	 */
    public int getId() {
        return id;
    }

    /**
	 * @param id The id to set.
	 */
    public void setId(int id) {
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
