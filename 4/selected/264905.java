package net.sourceforge.thinfeeder.vo;

import java.awt.Image;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Favicon implements FaviconIF {

    private long id;

    private long channelId;

    private Image icon;

    /**
	 * @return Returns the channelId.
	 */
    public long getChannelId() {
        return channelId;
    }

    /**
	 * @param channelId The channelId to set.
	 */
    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    /**
	 * @return Returns the icon.
	 */
    public Image getIcon() {
        return icon;
    }

    /**
	 * @param icon The icon to set.
	 */
    public void setIcon(Image icon) {
        this.icon = icon;
    }

    /**
	 * @return Returns the id.
	 */
    public long getId() {
        return id;
    }

    /**
	 * @param id The id to set.
	 */
    public void setId(long id) {
        this.id = id;
    }
}
