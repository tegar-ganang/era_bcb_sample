package openjirc;

import java.util.*;

/**
 * This class represents an IRC Channel as it is.<br>
 * <br>
 * OJIRCChannel klass esindab yht irc kanalit koos liikmete<br>
 * ja aktiivsustega.<br>
 */
public class OJIRCChannel {

    private String channel_name = "#java";

    private Vector members = new Vector();

    private boolean active = false;

    /**
	 * The Constructor.
	 * Konstruktor.
	 * @param the_channel_name kanali nimi (ntx "#java") / kanali nimi
	 */
    public OJIRCChannel(String the_channel_name) {
        super();
        channel_name = the_channel_name;
    }

    /**
	 * Sets the channel name to the_channel_name.<br>
	 * S'a'testab uue kanali nime.<br>
	 * @param the_channel_name uus kanali nimi / new channel name
	 */
    public void setChannelName(String the_channel_name) {
        channel_name = the_channel_name;
    }

    /**
	 * Gets the channel name.<br>
	 * Tagastab kanali nime.<br>
	 * @return kanali nimi / channel name
	 */
    public String getChannelName() {
        return channel_name;
    }

    /**
	 * Gets the channel name.<br>
	 * Tagastab kanali nime.<br>
	 * 
	 * @return kanali nimi / channel name
	 */
    public String getName() {
        return getChannelName();
    }

    /**
	 * Adds a member to the channel.<br>
	 * Lisab kanalile uue liikme.<br>
	 * @param mem uus liige / new member
	 */
    public void addMember(OJIRCMember mem) {
        members.addElement(mem);
    }

    /**
	 * Returns the count of the members on channel.<br>
	 * Tagastab kanalil olevate inimeste arvu.<br>
	 * 
	 * @return inimeste arv / channel members 
	 */
    public int getMembersCount() {
        return members.size();
    }

    /**
	 * Returns the nicknames of members on the channel.<br>
	 * Tagastab kanali liikmete hyydnimed.<br>
	 * 
	 * @return liikmete hyydnimed / members nicknames
	 */
    public String[] getMembersNickNames() {
        String ret[] = new String[getMembersCount()];
        for (int i = 0; i < getMembersCount(); i++) {
            ret[i] = ((OJIRCMember) members.elementAt(i)).getNick();
        }
        return ret;
    }

    /**
	 * Removes a member from the channel.<br>
	 * Eemaldab liikme kanalilt.<br>
	 *
	 * @param mem eemaldatav / member to be removed
	 */
    public void removeMember(OJIRCMember mem) {
        members.removeElement(mem);
    }

    /**
	 * Sets the state of the channel.<br>
	 * S'a'testab kanali staatuse.<br>
	 * 
	 * @param isactive on(true) v�i ei ole aktiivne(false) / active(true) or non active(false)
	 */
    public void setActivate(boolean isactive) {
        active = isactive;
    }

    /**
	 * Returns the current state of the channel.<br>
	 * Tagastab kanali hetke staatuse.
	 *
	 * @return aktiive / is active
	 * @see #setActivate(boolean)
	 */
    public boolean isActive() {
        return active;
    }

    /**
	 * Removes all members from tha channel, then destroys it (to avid memory leaks).<br>
	 * Eemaldab kanalilt k�ik liikmed , seej'a'rel h'a'vitab kanali.
	 */
    public void finalize() throws Throwable {
        members.removeAllElements();
        super.finalize();
    }
}
