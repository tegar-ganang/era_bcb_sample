package com.generatescape.baseobjects;

import java.io.Serializable;
import java.util.Date;
import org.apache.log4j.Logger;

/*******************************************************************************
 * Copyright (c) 2005, 2007 GenerateScape and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the GNU General Public License which accompanies this distribution, and is
 * available at http://www.gnu.org/copyleft/gpl.html
 * 
 * @author kentgibson : http://www.bigblogzoo.com
 * 
 ******************************************************************************/
public class ArticleObject implements Serializable {

    private static final long serialVersionUID = 3257562910472549682L;

    static Logger log = Logger.getLogger(ArticleObject.class.getName());

    private String unmatchedquery = "";

    private String image = "";

    private String title = "";

    private String description = "";

    private String url = "";

    private String query = "";

    private String caption = "";

    private String ftpAddress = "";

    private String channelURL = "";

    private Date dateFound;

    private String related = "";

    ;

    private String channelTitle = "";

    /**
  * @return
  */
    public String getRelated() {
        return related;
    }

    /**
  * @param related
  */
    public void setRelated(String related) {
        this.related = related;
    }

    /**
  * @param arg0
  * @return
  */
    public int compareTo(Object arg0) {
        ArticleObject ao = (ArticleObject) arg0;
        return ao.dateFound.compareTo(dateFound);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ArticleObject) {
            ArticleObject ao = (ArticleObject) obj;
            return title.equals(ao.title);
        }
        return false;
    }

    /**
  * 
  */
    public ArticleObject() {
        title = "";
        description = "";
        url = "";
        query = "";
        caption = "";
        ftpAddress = "";
    }

    /**
  * @return Returns the name.
  */
    public String getTitle() {
        return title;
    }

    /**
  * @param name
  *         The name to set.
  */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
  * @return Returns the url.
  */
    public String getUrl() {
        return url;
    }

    /**
  * @param url
  *         The url to set.
  */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
  * @return Returns the description.
  */
    public String getDescription() {
        return description;
    }

    /**
  * @param description
  *         The description to set.
  */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
  * @return Returns the query.
  */
    public String getQuery() {
        return query;
    }

    /**
  * @param query
  *         The query to set.
  */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
  * FOR THE MINUTE THE NORMAL SEARCHER USES THESE
  * 
  * @return Returns the caption.
  */
    public String getCaption() {
        return caption;
    }

    /**
  * @param caption
  *         The caption to set.
  */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
  * @return Returns the ftpAddress.
  */
    public String getFtpAddress() {
        return ftpAddress;
    }

    /**
  * @param ftpAddress
  *         The ftpAddress to set.
  */
    public void setFtpAddress(String ftpAddress) {
        this.ftpAddress = ftpAddress;
    }

    /**
  * @return Returns the channelURL.
  */
    public String getChannelURL() {
        return channelURL;
    }

    /**
  * @param channelURL
  *         The channelURL to set.
  */
    public void setChannelURL(String channelURL) {
        this.channelURL = channelURL;
    }

    /**
  * @return
  */
    public Date getDateFound() {
        return dateFound;
    }

    /**
  * @param dateFound
  */
    public void setDateFound(Date dateFound) {
        this.dateFound = dateFound;
    }

    /**
  * @return
  */
    public String getImage() {
        return image;
    }

    /**
  * @param image
  */
    public void setImage(String image) {
        this.image = image;
    }

    /**
  * @return
  */
    public String getUnmatchedquery() {
        return unmatchedquery;
    }

    /**
  * @param unmatchedquery
  */
    public void setUnmatchedquery(String unmatchedquery) {
        this.unmatchedquery = unmatchedquery;
    }

    /**
  * @return
  */
    public String getChannelTitle() {
        return channelTitle;
    }

    /**
  * @param channelTitle
  */
    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }
}
