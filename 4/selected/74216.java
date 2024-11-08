package barde.log.view;

import java.io.IOException;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import barde.log.LogReader;
import barde.log.Message;
import cbonar.memento.Memento;
import cbonar.memento.Operation;

/**
 * This class provides a basic implementation of {@link LogView}.<br>
 *
 * It's very simple, but it's also very slow, and uses probably much memory.<br>
 * @author cbonar
 * @deprecated I don't see a reason to keep this class ; or maybe as an example for the dev. docs... Use {@link TreeLogView} instead
 */
public class SimpleLogView extends AbstractList implements LogView {

    /** the content of this {@link AbstractList} */
    protected LinkedList messages;

    public SimpleLogView() {
        this.messages = new LinkedList();
    }

    public SimpleLogView(LogReader reader) {
        this();
        try {
            while (true) {
                try {
                    Message message = reader.read();
                    if (message == null) break; else add(message);
                } catch (ParseException pe) {
                    pe.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public SimpleLogView(Collection c) {
        this();
        addAll(c);
    }

    public Object get(int index) {
        return this.messages.get(index);
    }

    public int size() {
        return this.messages.size();
    }

    public Object remove(int index) {
        Message message = (Message) this.messages.remove(index);
        return message;
    }

    public void add(int index, Object element) {
        Message message = (Message) element;
        this.messages.add(index, message);
    }

    /**
	 * This must be the only external entry point for adding an element,
	 * because add(int,Object) doesn't check for unicity of the element,
	 * neither it checks for its type
	 */
    public boolean add(Object o) throws ClassCastException {
        Message message = (Message) o;
        for (ListIterator lit = this.messages.listIterator(this.messages.size()); lit.hasPrevious(); ) {
            Message prev = (Message) lit.previous();
            int compared = prev.compareTo(message);
            if (compared < 0) {
                lit.add(message);
                return true;
            } else if (compared == 0) return false;
        }
        this.messages.add(0, message);
        return true;
    }

    public LogView removeChannels(String regex, boolean remove) {
        for (Iterator it = iterator(); it.hasNext(); ) {
            Message next = (Message) it.next();
            if (next.getChannel().matches(regex) == remove) remove(next);
        }
        return this;
    }

    /**
	 * @see barde.log.view.LogView#removeChannels(java.lang.String)
	 */
    public LogView removeChannels(String regex) {
        return removeChannels(regex, true);
    }

    /**
	 * @see barde.log.view.LogView#retainChannels(java.lang.String)
	 */
    public LogView retainChannels(String regex) {
        return removeChannels(regex, false);
    }

    public LogView removeAvatars(String channels, String regex, boolean remove) {
        for (Iterator it = iterator(); it.hasNext(); ) {
            Message next = (Message) it.next();
            if (next.getChannel().matches(regex)) {
                if (next.getAvatar().matches(regex) == remove) remove(next);
            }
        }
        return this;
    }

    /**
	 * @see barde.log.view.LogView#retainAvatars(String, String)
	 * TODO : channels to match + avatars not to match ?
	 */
    public LogView retainAvatars(String channels, String regex) {
        return removeAvatars(channels, regex, false);
    }

    /**
	 * Removes matching avatars, and their messages
	 * @see barde.log.view.LogView#removeAvatars(java.lang.String,String)
	 * TODO : channels not to match + avatars to match ?
	 */
    public LogView removeAvatars(String channels, String regex) {
        return removeAvatars(channels, regex, true);
    }

    public LogView removeMessages(String channels, String avatars, String regex, boolean remove) {
        for (Iterator it = iterator(); it.hasNext(); ) {
            Message next = (Message) it.next();
            if (next.getChannel().matches(regex)) {
                if (next.getAvatar().matches(regex)) {
                    if (next.getContent().matches(regex) == remove) remove(next);
                }
            }
        }
        return this;
    }

    /**
	 * @see barde.log.view.LogView#removeMessages(String, String, String)
	 */
    public LogView removeMessages(String channels, String avatars, String regex) {
        return removeMessages(channels, avatars, regex, true);
    }

    /**
	 * @see barde.log.view.LogView#retainMessages(java.lang.String, java.lang.String, java.lang.String)
	 */
    public LogView retainMessages(String channels, String avatars, String regex) {
        return removeMessages(channels, avatars, regex, false);
    }
}
