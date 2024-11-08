package be.roam.drest.service.youtube;

import java.util.Date;
import java.util.List;

public class YouTubeVideo {

    private String id;

    private String author;

    private String title;

    private double ratingAverage;

    private int ratingCount;

    private String tags;

    private String description;

    private Date timeUpdated;

    private Date timeUploaded;

    private int nrViews;

    private int lengthInSeconds;

    private String recordingDate;

    private String recordingLocation;

    private String recordingCountry;

    private List<YouTubeComment> commentList;

    private List<String> channelList;

    private String thumbnailUrl;

    private int nrComments;

    private boolean embedable = true;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the channelList
     */
    public List<String> getChannelList() {
        return channelList;
    }

    /**
     * @param channelList the channelList to set
     */
    public void setChannelList(List<String> channelList) {
        this.channelList = channelList;
    }

    /**
     * @return the commentList
     */
    public List<YouTubeComment> getCommentList() {
        return commentList;
    }

    /**
     * @param commentList the commentList to set
     */
    public void setCommentList(List<YouTubeComment> commentList) {
        this.commentList = commentList;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the lengthInSeconds
     */
    public int getLengthInSeconds() {
        return lengthInSeconds;
    }

    /**
     * @param lengthInSeconds the lengthInSeconds to set
     */
    public void setLengthInSeconds(int lengthInSeconds) {
        this.lengthInSeconds = lengthInSeconds;
    }

    /**
     * @return the nrViews
     */
    public int getNrViews() {
        return nrViews;
    }

    /**
     * @param nrViews the nrViews to set
     */
    public void setNrViews(int nrViews) {
        this.nrViews = nrViews;
    }

    /**
     * @return the ratingAverage
     */
    public double getRatingAverage() {
        return ratingAverage;
    }

    /**
     * @param ratingAverage the ratingAverage to set
     */
    public void setRatingAverage(double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    /**
     * @return the ratingCount
     */
    public int getRatingCount() {
        return ratingCount;
    }

    /**
     * @param ratingCount the ratingCount to set
     */
    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    /**
     * @return the recordingCountry
     */
    public String getRecordingCountry() {
        return recordingCountry;
    }

    /**
     * @param recordingCountry the recordingCountry to set
     */
    public void setRecordingCountry(String recordingCountry) {
        this.recordingCountry = recordingCountry;
    }

    /**
     * @return the recordingDate
     */
    public String getRecordingDate() {
        return recordingDate;
    }

    /**
     * @param recordingDate the recordingDate to set
     */
    public void setRecordingDate(String recordingDate) {
        this.recordingDate = recordingDate;
    }

    /**
     * @return the recordingLocation
     */
    public String getRecordingLocation() {
        return recordingLocation;
    }

    /**
     * @param recordingLocation the recordingLocation to set
     */
    public void setRecordingLocation(String recordingLocation) {
        this.recordingLocation = recordingLocation;
    }

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * @return the thumbnailUrl
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    /**
     * @param thumbnailUrl the thumbnailUrl to set
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * @return the timeUpdated
     */
    public Date getTimeUpdated() {
        return timeUpdated;
    }

    /**
     * @param timeUpdated the timeUpdated to set
     */
    public void setTimeUpdated(Date timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    /**
     * @return the timeUploaded
     */
    public Date getTimeUploaded() {
        return timeUploaded;
    }

    /**
     * @param timeUploaded the timeUploaded to set
     */
    public void setTimeUploaded(Date timeUploaded) {
        this.timeUploaded = timeUploaded;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the number of comments on this video.
     * <p>
     * <strong>This does not necessarily match the size of the comments list</strong>.
     * </p>
     * 
     * @return the nrComments
     */
    public int getNrComments() {
        if (nrComments == 0) {
            if (commentList != null) {
                return commentList.size();
            }
        }
        return nrComments;
    }

    /**
     * @param nrComments the nrComments to set
     */
    public void setNrComments(int nrComments) {
        this.nrComments = nrComments;
    }

    public boolean isEmbedable() {
        return embedable;
    }

    public void setEmbedable(boolean embedable) {
        this.embedable = embedable;
    }
}
