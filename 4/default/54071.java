import java.util.Hashtable;

public class Program {

    private String startTime;

    private Hashtable titles;

    private Hashtable descriptions;

    private Credits credits;

    private String creationDate;

    private String country;

    private String episodeNumber;

    private Video video;

    private String rating;

    private String stars;

    private Duration duration;

    private String channel;

    private Hashtable categories;

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(startTime);
        sb.append(": ");
        sb.append(titles.get("en"));
        sb.append(" (");
        sb.append(descriptions.get("en"));
        sb.append(")");
        return sb.toString();
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Hashtable getTitles() {
        return titles;
    }

    public void setTitles(Hashtable titles) {
        this.titles = titles;
    }

    public Hashtable getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Hashtable descriptions) {
        this.descriptions = descriptions;
    }

    public Credits getCredits() {
        return credits;
    }

    public void setCredits(Credits credits) {
        this.credits = credits;
    }

    public Credit[] getCreditsAsArray() {
        if (credits == null) {
            return null;
        } else {
            Credit[] creditsArray = credits.getCredits();
            if (creditsArray.length == 0) {
                return null;
            } else {
                return credits.getCredits();
            }
        }
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(String episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getStars() {
        return stars;
    }

    public void setStars(String stars) {
        this.stars = stars;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Hashtable getCategories() {
        return categories;
    }

    public void setCategories(Hashtable categories) {
        this.categories = categories;
    }
}
