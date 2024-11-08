package org.thadeus.rssreader.views;

import java.net.URL;
import java.util.Date;
import org.thadeus.rssreader.ItemMetaData;
import de.nava.informa.core.ItemIF;

public class MetaItem {

    private final ItemIF item;

    private float score;

    private ItemMetaData metaData;

    public MetaItem(ItemIF item, ItemMetaData metaData) {
        this.item = item;
        this.metaData = metaData;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public ItemIF getItem() {
        return item;
    }

    public boolean isRead() {
        return metaData.isRead();
    }

    public void setRead(boolean read) {
        metaData.setRead(read);
    }

    public Boolean getVote() {
        return metaData.getVote();
    }

    public void setVote(Boolean vote) {
        metaData.setVote(vote);
    }

    public static String getKey(ItemIF item) {
        Date date = item.getDate();
        URL location = item.getChannel().getLocation();
        String title = item.getTitle();
        return (location == null ? "" : location.toString()) + "-" + (date == null ? 0 : date.getTime()) + "-" + (title == null ? 0 : title.hashCode());
    }
}
