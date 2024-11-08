package barde.t4c;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import barde.log.Message;

/**
 * @author cbonar@free.fr
 */
public class T4CMessage extends Message {

    public static DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    protected Date date;

    protected String channel;

    protected String avatar;

    protected String content;

    public T4CMessage(Date date, String channel, String avatar, String message) {
        Logger.getAnonymousLogger().entering(getClass().getName(), "new T4CMessage(" + date + "," + channel + "," + avatar + "," + message + ")");
        this.date = date;
        this.channel = channel;
        this.avatar = avatar;
        this.content = message;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public DateFormat getDateFormat() {
        return T4CMessage.dateFormat;
    }

    public Date getDate() {
        return this.date;
    }

    public String getContent() {
        return content;
    }

    /**
	 * Provides a way to quickly print the content of this message.<br>
	 * 
	 * @return "&lt;date|{unknown date}&gt; &lt;channel&gt; &lt;character&gt; &lt;message&gt;"
	 */
    public String toString() {
        String dateString = getDate() == null ? "{unknown date}" : getDateFormat().format(getDate());
        return dateString + " | " + this.channel + " | " + this.avatar + " | " + this.content;
    }
}
