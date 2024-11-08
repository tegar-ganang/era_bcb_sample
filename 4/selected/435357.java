package barde.log.view;

import java.io.IOException;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeSet;
import barde.log.LogReader;
import barde.log.Message;
import cbonar.memento.Memento;
import cbonar.memento.Operation;

/**
 * This class provides an efficient (hope so) implementation of {@link LogView}.<br>
 * It has a tree-based structure to be faster to find messages by their channel or source.<br>
 * Although its internal structure is tree-like, its interface is a flat perspective of its list of {@link Message}s,
 * which enable search for direct message and date.<br>
 * <i>Currently being refactored...</i><br>
 * TODO : memento dp is not correctly implemented (multiple memento for a same add operation,
 * going back to a previous state generates new memento, ...)
 * @author cbonar
 */
public class TreeLogView extends AbstractList implements LogView {

    /** the content of this {@link AbstractList} */
    protected LinkedList flatlist;

    /** contains the channels, ordered */
    protected TreeSet treelist;

    public TreeLogView() {
        this.flatlist = new LinkedList();
        this.treelist = new TreeSet();
    }

    public TreeLogView(LogReader reader) {
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
        for (Iterator cit = this.treelist.iterator(); cit.hasNext(); ) {
            ChannelRef chan = (ChannelRef) cit.next();
            System.out.println(chan.getName());
            for (Iterator avit = chan.avatars(); avit.hasNext(); ) {
                AvatarRef avat = (AvatarRef) avit.next();
                System.out.println("\t" + avat.getName());
                for (Iterator mit = avat.messages(); mit.hasNext(); ) System.out.println("\t\t" + ((MessageRef) mit.next()).getContent());
            }
        }
    }

    public TreeLogView(Collection c) {
        this();
        addAll(c);
    }

    public Object get(int index) {
        return this.flatlist.get(index);
    }

    public int size() {
        return this.flatlist.size();
    }

    public Object remove(int index) {
        MessageRef mref = (MessageRef) this.flatlist.remove(index);
        AvatarRef aref = mref.avatar;
        aref.remove(mref);
        if (aref.size() == 0) {
            ChannelRef cref = mref.channel;
            cref.remove(aref);
            if (cref.size() == 0) this.treelist.remove(cref);
        }
        return mref;
    }

    /**
	 * Adds a new {@link Message} to this view.
	 * Use only {@link #add(Object)} to add an element to this <tt>AbstractList</tt>.
	 * This method should only be used for internal purpose.
	 * @throws ClassCastException if <tt>element</tt> is not a <tt>Message</tt>
	 * @see java.util.List#add(int, java.lang.Object)
	 */
    public void add(int index, Object element) {
        Message message = (Message) element;
        ChannelRef channel = new ChannelRef(message.getChannel());
        for (Iterator cit = this.treelist.iterator(); cit.hasNext(); ) {
            ChannelRef cref = (ChannelRef) cit.next();
            if (cref.getName().equals(message.getChannel())) {
                channel = cref;
                break;
            }
        }
        this.treelist.add(channel);
        AvatarRef avatar = (AvatarRef) channel.get(message.getAvatar());
        if (avatar == null) {
            avatar = new AvatarRef(message.getAvatar());
            channel.add(avatar);
        }
        MessageRef mref = new MessageRef(channel, avatar, message);
        if (!avatar.contains(mref)) {
            avatar.add(mref);
            this.flatlist.add(index, mref);
        } else System.out.println("duplicate:" + mref);
    }

    /**
	 * Adds a new {@link Message} to this view.
	 * Should be the only entry point to be used to add an element to this <tt>AbstractList</tt>, since it takes cares of their relative order.
	 * @throws ClassCastException if <tt>o</tt> is not a <tt>Message</tt>
	 */
    public boolean add(Object o) throws ClassCastException {
        Message message = (Message) o;
        for (ListIterator mit = this.flatlist.listIterator(this.flatlist.size()); mit.hasPrevious(); ) {
            int i = mit.previousIndex();
            MessageRef mref = (MessageRef) mit.previous();
            int compared = message.compareTo(mref);
            if (compared > 0) {
                add(i + 1, message);
                return true;
            } else if (compared == 0) {
                return false;
            }
        }
        add(0, message);
        return true;
    }

    public LogView removeChannels(String regex, boolean remove) {
        System.out.println("TreeLogView.removeChannels(" + regex + "," + remove + ")");
        for (Iterator cit = this.treelist.iterator(); cit.hasNext(); ) {
            ChannelRef channel = (ChannelRef) cit.next();
            System.out.println(channel.getName() + ".matches(" + regex + ")=" + channel.getName().matches(regex));
            if (channel.getName().matches(regex) == remove) {
                System.out.println("remove(" + channel.getName() + ")");
                HashSet toremove = new HashSet();
                for (Iterator avit = channel.avatars(); avit.hasNext(); ) toremove.addAll((AvatarRef) avit.next());
                this.flatlist.removeAll(toremove);
                cit.remove();
            }
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
        for (Iterator cit = this.treelist.iterator(); cit.hasNext(); ) {
            ChannelRef channel = (ChannelRef) cit.next();
            if (channel.getName().matches(channels) == remove) {
                for (Iterator avit = channel.avatars(); avit.hasNext(); ) {
                    AvatarRef avatar = (AvatarRef) avit.next();
                    if (avatar.getName().matches(regex) == remove) {
                        this.flatlist.removeAll(avatar);
                        avit.remove();
                    }
                }
            }
        }
        return this;
    }

    public LogView removeMessages(String channels, String avatars, String regex, boolean remove) {
        for (Iterator cit = this.treelist.iterator(); cit.hasNext(); ) {
            ChannelRef channel = (ChannelRef) cit.next();
            if (channel.getName().matches(channels) == remove) {
                for (Iterator avit = channel.avatars(); avit.hasNext(); ) {
                    AvatarRef avatar = (AvatarRef) avit.next();
                    if (avatar.getName().matches(avatars) == remove) {
                        for (Iterator mit = avatar.messages(); mit.hasNext(); ) {
                            MessageRef message = (MessageRef) mit.next();
                            if (message.getContent().matches(regex) == remove) {
                                mit.remove();
                                this.flatlist.remove(message);
                            }
                        }
                    }
                }
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
	 * @see barde.log.view.LogView#removeAvatars(String, String)
	 * TODO : channels not to match + avatars to match ?
	 */
    public LogView removeAvatars(String channels, String regex) {
        return removeAvatars(channels, regex, true);
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
