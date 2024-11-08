package net.sf.howabout.plugin;

/**
 * Provides query features for HowAbout classes. The query object is heavily
 * used by all plugins, as they rely on the information a query holds.
 * @author Paulo Roberto Massa Cereda
 * @version 1.0
 * @since 1.0
 */
public class Query {

    private String channel;

    private String genre;

    private Day day;

    /**
     * Getter method for channel.
     * @return The event channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Setter method for channel.
     * @param channel The event channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Getter method for day.
     * @return The event day.
     */
    public Day getDay() {
        return day;
    }

    /**
     * Setter method for day.
     * @param day The event day.
     */
    public void setDay(Day day) {
        this.day = day;
    }

    /**
     * Getter method for genre.
     * @return The event genre.
     */
    public String getGenre() {
        return genre;
    }

    /**
     * Setter method for genre.
     * @param genre The event genre.
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Empty constructor method.
     */
    public Query() {
    }

    /**
     * Constructor method which sets all attributes.
     * @param channel The event channel.
     * @param genre The event genre.
     * @param day The event day.
     */
    public Query(String channel, String genre, Day day) {
        this.channel = channel;
        this.genre = genre;
        this.day = day;
    }
}
