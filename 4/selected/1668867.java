package jgnash.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.StoredObject;

/**
 * Message object
 * 
 * @author Craig Cavanaugh
 * @version $Id: Message.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class Message implements Serializable, Cloneable {

    private static final long serialVersionUID = 2351895771349706585L;

    private ChannelEvent event;

    private MessageChannel channel;

    private String source;

    private String message = "";

    private transient EnumMap<MessageProperty, StoredObject> properties = new EnumMap<MessageProperty, StoredObject>(MessageProperty.class);

    /**
     * Used to flag message sent remotely
     */
    private transient boolean remote;

    /**
     * No argument constructor for reflection purposes.<br>
     * <b>Do not use to create new instances</b>
     * 
     * @deprecated
     */
    @Deprecated
    public Message() {
    }

    public Message(final MessageChannel channel, final ChannelEvent event, final Engine source) {
        this(channel, event, source.getUuid());
    }

    private Message(final MessageChannel channel, final ChannelEvent event, final String source) {
        if (source == null || event == null || channel == null) {
            throw new IllegalArgumentException();
        }
        this.source = source;
        this.event = event;
        this.channel = channel;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public ChannelEvent getEvent() {
        return event;
    }

    public void setMessage(final String message) {
        if (message == null) {
            throw new IllegalArgumentException("data may not be null");
        }
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Sets a message property. The value must be reachable by the engine or and exception will be thrown.
     * 
     * @param key property key
     * @param value message value
     * @throws IllegalArgumentException throws an exception if value is null     
     */
    public void setObject(final MessageProperty key, final StoredObject value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }
        properties.put(key, value);
    }

    public Object getObject(final MessageProperty key) {
        return properties.get(key);
    }

    public String getSource() {
        return source;
    }

    public void setRemote(final boolean remote) {
        this.remote = remote;
    }

    public boolean isRemote() {
        return remote;
    }

    /**
     * Write message out to ObjectOutputStream.
     * 
     * @param s stream
     * @throws IOException io exception
     * @serialData Write serializable fields, if any exist. Write out the integer count of properties. Write out key and
     *             value of each property
     */
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeUTF(message);
        s.writeInt(properties.size());
        StoredObject[] values = properties.values().toArray(new StoredObject[properties.size()]);
        MessageProperty[] keys = properties.keySet().toArray(new MessageProperty[properties.size()]);
        for (int i = 0; i < properties.size(); i++) {
            s.writeObject(keys[i]);
            s.writeUTF(values[i].getUuid());
        }
    }

    /**
     * Read a Message from an ObjectInputStream
     * 
     * @param s input stream
     * @throws java.io.IOException io exception
     * @throws ClassNotFoundException thrown is class is not found
     * @serialData Read serializable fields, if any exist. Read the integer count of properties. Read the key and value
     *             of each property
     */
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        message = s.readUTF();
        properties = new EnumMap<MessageProperty, StoredObject>(MessageProperty.class);
        int size = s.readInt();
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        for (int i = 0; i < size; i++) {
            MessageProperty key = (MessageProperty) s.readObject();
            StoredObject value = engine.getStoredObjectByUuid(s.readUTF());
            properties.put(key, value);
        }
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
        Message m = null;
        try {
            m = (Message) super.clone();
            m.message = message;
            m.properties = properties.clone();
        } catch (CloneNotSupportedException e) {
            Logger.getLogger(Message.class.getName()).log(Level.SEVERE, e.toString(), e);
        }
        return m;
    }

    /**
     * Returns the event and channel for this message
     * 
     * @see java.lang.Object#toString()
     * @return event and channel information
     */
    @Override
    public String toString() {
        return String.format("Message [event=%s, channel=%s, source=%s \n\tmessage=%s]", event, channel, source, message);
    }
}
