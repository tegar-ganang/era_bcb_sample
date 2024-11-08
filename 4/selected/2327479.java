package org.anuta.xmltv.beans;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Program {

    private String url;

    private String title;

    private String longTitle;

    private Date startDate;

    private Date endDate;

    private boolean fullyLoaded = false;

    private String description;

    private String type;

    private String ganre;

    private String premiere;

    private String channelId;

    private String subTitle;

    private String presentors;

    private String actors;

    private String directors;

    private String specials;

    private String clumpIdx;

    private List<Rating> rating = new ArrayList<Rating>();

    public String getLongTitle() {
        return longTitle;
    }

    public void setLongTitle(String longTitle) {
        this.longTitle = longTitle;
    }

    public List<Rating> getRating() {
        return rating;
    }

    public void setRating(List<Rating> rating) {
        this.rating = rating;
    }

    public String getSpecials() {
        return specials;
    }

    public void setSpecials(String specials) {
        this.specials = specials;
    }

    public String getPresentors() {
        return presentors;
    }

    public void setPresentors(String presentors) {
        this.presentors = presentors;
    }

    public String getDirectors() {
        return directors;
    }

    public void setDirectors(String directors) {
        this.directors = directors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getGanre() {
        return ganre;
    }

    public void setGanre(String ganre) {
        this.ganre = ganre;
    }

    public String getPremiere() {
        return premiere;
    }

    public void setPremiere(String premiere) {
        this.premiere = premiere;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() throws NoSuchAlgorithmException {
        String hash = getHash().toLowerCase();
        String allowed = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (allowed.indexOf(c) != -1) sb.append(c);
            if (sb.length() > 100) break;
        }
        return sb.toString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isFullyLoaded() {
        return fullyLoaded;
    }

    public void setFullyLoaded(boolean fullyLoaded) {
        this.fullyLoaded = fullyLoaded;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        if (startDate != null) {
            sb.append(sdf.format(getStartDate()));
        } else {
            sb.append("null");
        }
        sb.append(" - ");
        if (endDate != null) {
            sb.append(sdf.format(getEndDate()));
        } else {
            sb.append("null");
        }
        sb.append(":");
        sb.append("title:").append(title).append("\n");
        sb.append(url).append("\n");
        sb.append("longTitle:").append(longTitle).append("\n");
        sb.append("description:").append(description).append("\n");
        sb.append("type:").append(type).append("\n");
        sb.append("ganre:").append(ganre).append("\n");
        sb.append("premiere:").append(premiere).append("\n");
        sb.append("channelId:").append(channelId).append("\n");
        sb.append("subTitle:").append(subTitle).append("\n");
        sb.append("presentors:").append(presentors).append("\n");
        sb.append("actors:").append(actors).append("\n");
        sb.append("directors:").append(directors).append("\n");
        sb.append("specials:").append(specials).append("\n");
        sb.append("clumpIdx:").append(clumpIdx).append("\n");
        return sb.toString();
    }

    public String getHash() {
        StringBuffer sb = new StringBuffer();
        sb.append(getStartDate()).append("-").append(getEndDate()).append("-").append(getChannelId()).append("-").append(getTitle());
        return sb.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActors() {
        return actors;
    }

    public String getClumpIdx() {
        return clumpIdx;
    }

    public void setClumpIdx(String clumpIdx) {
        this.clumpIdx = clumpIdx;
    }
}
