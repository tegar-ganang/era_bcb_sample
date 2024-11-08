package de.jassda.modules.trace.reflect;

import de.jassda.modules.trace.event.EventSet;
import de.jassda.csp.Context;
import de.jassda.csp.CspProcess;
import de.jassda.util.log.Log;
import de.jassda.modules.trace.parser.Node;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Class LocalChannelContainer
 *
 *
 * @author Mark Broerkens
 * @version %I%, %G%
 */
public class LocalChannelContainer extends Container {

    Vector processes = new Vector();

    Hashtable channels = new Hashtable();

    /** Field ALPHABET_SET           */
    public static final int ALPHABET_SET = 1;

    /** Field ALPHABET_UNION           */
    public static final int ALPHABET_UNION = 2;

    EventSet alphabet = null;

    int alphabetOperator = 0;

    /**
     * Constructor LocalChannelContainer
     *
     *
     * @param parent
     * @param node
     *
     */
    public LocalChannelContainer(Container parent, Node node) {
        super(parent, node);
    }

    /**
     * Method addProcess
     *
     *
     * @param process
     *
     */
    public void addProcess(CspProcess process) {
        processes.add(process);
    }

    /**
     * Method setChannels
     *
     *
     * @param channels
     *
     */
    public void setChannels(Hashtable channels) {
        this.channels = channels;
    }

    /**
     * Method addChannel
     *
     *
     * @param name
     * @param channel
     *
     * @return
     *
     */
    public EventSet addChannel(String name, EventSet channel) {
        return (EventSet) channels.put(name, channel);
    }

    /**
     * Method getChannel
     *
     *
     * @param name
     *
     * @return
     *
     */
    public EventSet getChannel(String name) {
        return (EventSet) channels.get(name);
    }

    /**
     * Method getChannels
     *
     *
     * @return
     *
     */
    public List getChannels() {
        Vector eventSets = new Vector();
        for (Enumeration enumeration = channels.elements(); enumeration.hasMoreElements(); ) {
            eventSets.add(enumeration.nextElement());
        }
        return eventSets;
    }

    /**
     * Method getChannelDefinitions
     *
     *
     * @return
     *
     */
    public Hashtable getChannelDefinitions() {
        Hashtable channelDefinitions = super.getChannelDefinitions();
        channelDefinitions.putAll(channels);
        return channelDefinitions;
    }

    /**
     * Method getEventSetName
     *
     *
     * @return
     *
     */
    public String getEventSetName() {
        return null;
    }

    /**
     * Method getAlphabet
     *
     *
     * @return
     *
     */
    public EventSet getAlphabet() {
        return alphabet;
    }

    /**
     * Method setAlphabet
     *
     *
     * @param alphabet
     *
     */
    public void setAlphabet(EventSet alphabet) {
        this.alphabet = alphabet;
    }

    /**
     * Method getAlphabetOperator
     *
     *
     * @return
     *
     */
    public int getAlphabetOperator() {
        return alphabetOperator;
    }

    /**
     * Method setAlphabetOperator
     *
     *
     * @param alphabetOperator
     *
     */
    public void setAlphabetOperator(int alphabetOperator) {
        this.alphabetOperator = alphabetOperator;
    }
}
