package com.webstersmalley.jtv;

import java.util.Set;

/**
 * @author Matthew Smalley
 * Houses the fields / accessors of a Program
 */
public class AbstractProgram {

    protected String channelId;

    protected String start;

    protected String subTitle;

    protected String title;

    protected String description;

    protected Credits credits;

    protected String date;

    protected String country;

    protected String episodeNumber;

    protected Video video;

    protected String rating;

    protected String stars;

    protected Categories categories;

    protected String language;

    protected String originalLanguage;

    protected String length;

    protected String icon;

    protected String url;

    protected String audio;

    protected String previouslyShown;

    protected String premiere;

    protected String lastChance;

    protected String newflag;

    protected String subtitles;

    protected String checkString(String input) {
        if (input == null) {
            return null;
        }
        String output = input.trim();
        if (output.equals("")) {
            return null;
        } else {
            return output;
        }
    }

    /**
     * @return Returns the country.
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country The country to set.
     */
    public void setCountry(String country) {
        this.country = checkString(country);
    }

    /**
     * @return Returns the credits.
     */
    public Credits getCredits() {
        return credits;
    }

    /**
     * @param credits The credits to set.
     */
    public void setCredits(Credits credits) {
        this.credits = credits;
    }

    public void addCredit(String role, String name) {
        name = checkString(name);
        if (name != null) {
            if (credits == null) {
                credits = new Credits();
            }
            Credit credit = new Credit(role, name);
            credits.addCredit(credit);
        }
    }

    /**
     * @return Returns the date.
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date The date to set.
     */
    public void setDate(String date) {
        this.date = checkString(date);
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = checkString(description);
    }

    /**
     * @return Returns the episodeNumber.
     */
    public String getEpisodeNumber() {
        return episodeNumber;
    }

    /**
     * @param episodeNumber The episodeNumber to set.
     */
    public void setEpisodeNumber(String episodeNumber) {
        this.episodeNumber = checkString(episodeNumber);
    }

    /**
     * @return Returns the rating.
     */
    public String getRating() {
        return rating;
    }

    /**
     * @param rating The rating to set.
     */
    public void setRating(String rating) {
        this.rating = checkString(rating);
    }

    /**
     * @return Returns the stars.
     */
    public String getStars() {
        return stars;
    }

    /**
     * @param stars The stars to set.
     */
    public void setStars(String stars) {
        this.stars = checkString(stars);
    }

    /**
     * @return Returns the start.
     */
    public String getStart() {
        return start;
    }

    /**
     * @param start The start to set.
     */
    public void setStart(String start) {
        this.start = checkString(start);
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = checkString(title);
    }

    /**
     * @return Returns the video.
     */
    public Video getVideo() {
        return video;
    }

    /**
     * @param video The video to set.
     */
    public void setVideo(Video video) {
        this.video = video;
    }

    public void addVideo(String videoType) {
        if (video == null) {
            video = new Video();
        }
        this.video.addAspect(videoType);
    }

    /**
     * @return Returns the subTitle.
     */
    public String getSubTitle() {
        return subTitle;
    }

    /**
     * @param subTitle The subTitle to set.
     */
    public void setSubTitle(String subTitle) {
        this.subTitle = checkString(subTitle);
    }

    /**
     * @return Returns the channelId.
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * @param channelId The channelId to set.
     */
    public void setChannelId(String channelId) {
        this.channelId = checkString(channelId);
    }

    /**
     * @return Returns the audio.
     */
    public String getAudio() {
        return audio;
    }

    /**
     * @param audio The audio to set.
     */
    public void setAudio(String audio) {
        this.audio = audio;
    }

    /**
     * @return Returns the category.
     */
    public Categories getCategories() {
        return categories;
    }

    /**
     * @param category The category to set.
     */
    public void setCategory(Categories categories) {
        this.categories = categories;
    }

    public void addCategory(String category) {
        if (categories == null) {
            categories = new Categories();
        }
        categories.addCategory(category);
    }

    /**
     * @return Returns the icon.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon The icon to set.
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return Returns the language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language The language to set.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return Returns the lastChance.
     */
    public String getLastChance() {
        return lastChance;
    }

    /**
     * @param lastChance The lastChance to set.
     */
    public void setLastChance(String lastChance) {
        this.lastChance = lastChance;
    }

    /**
     * @return Returns the length.
     */
    public String getLength() {
        return length;
    }

    /**
     * @param length The length to set.
     */
    public void setLength(String length) {
        this.length = length;
    }

    /**
     * @return Returns the newflag.
     */
    public String getNewflag() {
        return newflag;
    }

    /**
     * @param newflag The newflag to set.
     */
    public void setNewflag(String newflag) {
        this.newflag = newflag;
    }

    /**
     * @return Returns the originalLanguage.
     */
    public String getOriginalLanguage() {
        return originalLanguage;
    }

    /**
     * @param originalLanguage The originalLanguage to set.
     */
    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    /**
     * @return Returns the premiere.
     */
    public String getPremiere() {
        return premiere;
    }

    /**
     * @param premiere The premiere to set.
     */
    public void setPremiere(String premiere) {
        this.premiere = premiere;
    }

    /**
     * @return Returns the previouslyShown.
     */
    public String getPreviouslyShown() {
        return previouslyShown;
    }

    /**
     * @param previouslyShown The previouslyShown to set.
     */
    public void setPreviouslyShown(String previouslyShown) {
        this.previouslyShown = previouslyShown;
    }

    /**
     * @return Returns the subtitles.
     */
    public String getSubtitles() {
        return subtitles;
    }

    /**
     * @param subtitles The subtitles to set.
     */
    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    /**
     * @return Returns the url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
