package barde.log;

import java.text.DateFormat;
import java.util.Date;

/**
 * This is the base unit of a log : a sentence, somehow.<br>
 * Each message is :
 * <ul>
 * <li>spoken by a character (the source, also known as the avatar)
 * <li>timestamped
 * <li>stored in a channel
 * </ul>
 * This is actually a very general definition of a message, in order to allow
 * the existence of different implementations.<br>
 * TODO : return AvatarRef and ChannelRef, String wrappers, rather than a String : permit low memory as those were duplicated
 * 
 * @see barde.log.view.MessageRef , which is designed as an efficient wrapper for this class.<br>
 * @author cbonar@free.fr
 */
public abstract class Message implements Comparable {

    /** @return the date when the message was spoken */
    public abstract Date getDate();

    public abstract DateFormat getDateFormat();

    /** @return the channel where it was spoken */
    public abstract String getChannel();

    /** @return the character who said this message */
    public abstract String getAvatar();

    /** the content of this message */
    public abstract String getContent();

    public void setDate(Date date) {
        throw new UnsupportedOperationException();
    }

    ;

    public void setChannel(String channel) {
        throw new UnsupportedOperationException();
    }

    ;

    public void setAvatar(String avatar) {
        throw new UnsupportedOperationException();
    }

    ;

    public void setContent(String content) {
        throw new UnsupportedOperationException();
    }

    ;

    /**
	 * A Message is compared to another one using its date
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    public int compareTo(Object o) throws ClassCastException {
        Message anotherMessage = (Message) o;
        int dateCompare = getDate().compareTo(anotherMessage.getDate());
        if (dateCompare == 0) {
            int chanCompare = getChannel().compareTo(anotherMessage.getChannel());
            if (chanCompare == 0) {
                int avCompare = getAvatar().compareTo(anotherMessage.getAvatar());
                if (avCompare == 0) return getContent().compareTo(anotherMessage.getContent()); else return avCompare;
            } else return chanCompare;
        } else return dateCompare;
    }

    public boolean equals(Object obj) {
        return compareTo(obj) == 0;
    }

    public int hashCode() {
        return 0;
    }
}
