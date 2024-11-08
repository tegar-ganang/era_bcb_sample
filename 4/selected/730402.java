package org.jwebsocket.plugins.channels;

import java.util.Date;
import org.jwebsocket.api.WebSocketConnector;

/**
 * Represents the single publisher connected the the particular channel
 * 
 * @author puran
 * @version $Id: Publisher.java 1120 2010-10-24 06:03:08Z mailtopuran@gmail.com$
 */
public final class Publisher {

    private String id;

    private String login;

    private String channel;

    private WebSocketConnector connector;

    private Date authorizedDate;

    private Date lastPublishedDate;

    private boolean authorized;

    public Publisher(WebSocketConnector connector, String login, String channel, Date authorizedDate, Date lastPublishedDate, boolean authorized) {
        this.id = connector.getSession().getSessionId();
        this.login = login;
        this.channel = channel;
        this.connector = connector;
        this.authorizedDate = authorizedDate;
        this.lastPublishedDate = lastPublishedDate;
        this.authorized = authorized;
    }

    public Publisher(String id, String login, String channel, Date authorizedDate, Date lastPublishedDate, boolean authorized) {
        this.id = id;
        this.login = login;
        this.channel = channel;
        this.connector = null;
        this.authorizedDate = authorizedDate;
        this.lastPublishedDate = lastPublishedDate;
        this.authorized = authorized;
    }

    /**
	 * @return the id
	 */
    public String getId() {
        return id;
    }

    /**
	 * @return the login name
	 */
    public String getLogin() {
        return login;
    }

    /**
	 * @return the channel
	 */
    public String getChannel() {
        return channel;
    }

    /**
	 * @return the connector
	 */
    public WebSocketConnector getConnector() {
        return connector;
    }

    /**
	 * @return the authorizedDate
	 */
    public Date getAuthorizedDate() {
        return authorizedDate;
    }

    /**
	 * @return the authorized
	 */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
	 * @return the lastPublishedDate
	 */
    public Date getLastPublishedDate() {
        return lastPublishedDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Publisher other = (Publisher) obj;
        if (channel == null) {
            if (other.channel != null) {
                return false;
            }
        } else if (!channel.equals(other.channel)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
