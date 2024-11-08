package de.rentoudu.chat.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Id;
import com.googlecode.objectify.Key;

/**
 * The persistence message model.
 * 
 * @author Florian Sauter
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 4471865979430645955L;

    @Id
    private Long id;

    private Key<Channel> channel;

    private String message;

    private String author;

    private Date timestamp;

    public Message() {
    }

    public Message(String message, String author, Date timestamp) {
        super();
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Key<Channel> getChannel() {
        return channel;
    }

    public void setChannel(Key<Channel> channel) {
        this.channel = channel;
    }
}
