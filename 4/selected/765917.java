package org.horrabin.horrorss;

import java.util.List;

/**
* The RssFeed class maps the elements of an RSS feed 
* (channel, image and a list of items) 
*  
* @author Fernando Fornieles 
*/
public class RssFeed {

    private RssChannelBean channel;

    private RssImageBean image;

    private List<RssItemBean> items;

    /**
	 * Returns the feed channel element
	 * @return the feed channel element
	 */
    public RssChannelBean getChannel() {
        return channel;
    }

    /**
	 * Sets the feed channel element
	 * @param channel The feed channel element
	 */
    public void setChannel(RssChannelBean channel) {
        this.channel = channel;
    }

    /**
	 * Returns the feed image element
	 * @return the feed image element
	 */
    public RssImageBean getImage() {
        return image;
    }

    /**
	 * Sets the feed image element
	 * @param image The feed image element
	 */
    public void setImage(RssImageBean image) {
        this.image = image;
    }

    /**
	 * Returns the feed items list
	 * @return List of RssItemBean
	 */
    public List<RssItemBean> getItems() {
        return items;
    }

    /**
	 * Sets the feed items list
	 * @param items The feed item list
	 */
    public void setItems(List<RssItemBean> items) {
        this.items = items;
    }
}
