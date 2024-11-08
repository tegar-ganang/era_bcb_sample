package net.sf.howabout.plugin;

import java.util.GregorianCalendar;

/**
 * Provides the POJO class that represents a TV event.
 * @author Paulo Roberto Massa Cereda
 * @version 1.0
 * @since 1.0
 */
public class Event {

    private GregorianCalendar date;

    private String name;

    private String description;

    private String genre;

    private String channel;

    /**
     * Empty constructor method.
     */
    public Event() {
    }

    /**
     * Constructor method. It basically sets all class attributes.
     * @param date The event date.
     * @param name The event name.
     * @param description The event description.
     * @param genre The event genre.
     * @param channel The event channel.
     */
    public Event(GregorianCalendar date, String name, String description, String genre, String channel) {
        this.date = date;
        this.name = name;
        this.description = description;
        this.genre = genre;
        this.channel = channel;
    }

    /**
     * Getter method for channel.
     * @return A string containing the event channel.
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
     * Getter method for description.
     * @return A string containing the event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter method for description.
     * @param description The event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter method for date.
     * @return A calendar object containing the event date.
     */
    public GregorianCalendar getDate() {
        return date;
    }

    /**
     * Setter method for date.
     * @param date The event date.
     */
    public void setDate(GregorianCalendar date) {
        this.date = date;
    }

    /**
     * Getter method for genre.
     * @return A string containing the event genre.
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
     * Getter method for name.
     * @return A string containing the event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Setter method for name.
     * @param name The event name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
