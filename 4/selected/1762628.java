package org.red5.server.so;

import static org.red5.server.api.so.ISharedObject.TYPE;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.so.ISharedObjectEvent.Type;

public class SharedObject implements IPersistable, Constants {

    protected static Log log = LogFactory.getLog(SharedObject.class.getName());

    protected String name = "";

    protected String path = "";

    protected boolean persistent = false;

    protected boolean persistentSO = false;

    protected IPersistenceStore storage = null;

    protected int version = 1;

    protected Map<String, Object> data = null;

    protected Map<String, Integer> hashes = new HashMap<String, Integer>();

    protected int updateCounter = 0;

    protected boolean modified = false;

    protected long lastModified = -1;

    private SharedObjectMessage ownerMessage;

    private LinkedList<ISharedObjectEvent> syncEvents = new LinkedList<ISharedObjectEvent>();

    protected HashSet<IEventListener> listeners = new HashSet<IEventListener>();

    private IEventListener source = null;

    public SharedObject() {
        data = new HashMap<String, Object>();
        ownerMessage = new SharedObjectMessage(null, null, -1, false);
    }

    public SharedObject(Input input) throws IOException {
        this();
        deserialize(input);
    }

    public SharedObject(Map<String, Object> data, String name, String path, boolean persistent) {
        this.data = data;
        this.name = name;
        this.path = path;
        this.persistentSO = persistent;
        ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
    }

    public SharedObject(Map<String, Object> data, String name, String path, boolean persistent, IPersistenceStore storage) {
        this.data = data;
        this.name = name;
        this.path = path;
        this.persistentSO = persistent;
        setStore(storage);
        ownerMessage = new SharedObjectMessage(null, name, 0, persistent);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return TYPE;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isPersistentObject() {
        return persistentSO;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    private void sendUpdates() {
        if (!ownerMessage.getEvents().isEmpty()) {
            SharedObjectMessage syncOwner = new SharedObjectMessage(null, name, version, isPersistentObject());
            syncOwner.addEvents(ownerMessage.getEvents());
            if (source != null) {
                Channel channel = ((RTMPConnection) source).getChannel((byte) 3);
                if (channel != null) {
                    channel.write(syncOwner);
                    log.debug("Owner: " + channel);
                } else {
                    log.warn("No channel found for owner changes!?");
                }
            }
            ownerMessage.getEvents().clear();
        }
        if (!syncEvents.isEmpty()) {
            for (IEventListener listener : listeners) {
                if (listener == source) {
                    log.debug("Skipped " + source);
                    continue;
                }
                if (!(listener instanceof RTMPConnection)) {
                    log.warn("Can't send sync message to unknown connection " + listener);
                    continue;
                }
                SharedObjectMessage syncMessage = new SharedObjectMessage(null, name, version, isPersistentObject());
                syncMessage.addEvents(syncEvents);
                Channel c = ((RTMPConnection) listener).getChannel((byte) 3);
                log.debug("Send to " + c);
                c.write(syncMessage);
            }
            syncEvents.clear();
        }
    }

    private void updateHashes() {
        hashes.clear();
        for (String name : data.keySet()) {
            Object value = data.get(name);
            hashes.put(name, value != null ? value.hashCode() : 0);
        }
    }

    private void notifyModified() {
        if (updateCounter > 0) {
            return;
        }
        if (modified) {
            updateVersion();
            lastModified = System.currentTimeMillis();
        }
        if (modified && storage != null) {
            if (!storage.save(this)) {
                log.error("Could not store shared object.");
            }
        }
        sendUpdates();
        updateHashes();
    }

    public boolean hasAttribute(String name) {
        return data.containsKey(name);
    }

    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public Object getAttribute(String name) {
        return data.get(name);
    }

    public boolean setAttribute(String name, Object value) {
        ownerMessage.addEvent(Type.CLIENT_UPDATE_ATTRIBUTE, name, null);
        Object old = data.get(name);
        Integer oldHash = (value != null ? value.hashCode() : 0);
        if (old == null || !old.equals(value) || !oldHash.equals(hashes.get(name))) {
            modified = true;
            data.put(name, value);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, name, value));
            notifyModified();
            return true;
        } else {
            notifyModified();
            return false;
        }
    }

    public void setAttributes(Map values) {
        if (values == null) {
            return;
        }
        beginUpdate();
        Iterator it = values.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            setAttribute(name, values.get(name));
        }
        endUpdate();
    }

    public void setAttributes(IAttributeStore values) {
        if (values == null) {
            return;
        }
        beginUpdate();
        Iterator it = values.getAttributeNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            setAttribute(name, values.getAttribute(name));
        }
        endUpdate();
    }

    public boolean removeAttribute(String name) {
        boolean result = data.containsKey(name);
        if (result) {
            data.remove(name);
        }
        ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, name, null);
        if (result) {
            modified = true;
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, name, null));
        }
        notifyModified();
        return result;
    }

    public void sendMessage(String handler, List arguments) {
        ownerMessage.addEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments);
        syncEvents.add(new SharedObjectEvent(Type.CLIENT_SEND_MESSAGE, handler, arguments));
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public int getVersion() {
        return version;
    }

    private void updateVersion() {
        version += 1;
    }

    public void removeAttributes() {
        Iterator keys = data.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            ownerMessage.addEvent(Type.CLIENT_DELETE_DATA, key, null);
            syncEvents.add(new SharedObjectEvent(Type.CLIENT_DELETE_DATA, key, null));
        }
        data.clear();
        modified = true;
        notifyModified();
    }

    public void register(IEventListener listener) {
        listeners.add(listener);
        ownerMessage.addEvent(Type.CLIENT_INITIAL_DATA, null, null);
        if (!isPersistentObject()) {
            ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, null, null);
        }
        if (!data.isEmpty()) {
            ownerMessage.addEvent(new SharedObjectEvent(Type.CLIENT_UPDATE_DATA, null, getData()));
        }
        notifyModified();
    }

    public void unregister(IEventListener listener) {
        listeners.remove(listener);
        if (!isPersistentObject() && listeners.isEmpty()) {
            log.info("Deleting shared object " + name + " because all clients disconnected.");
            data.clear();
            if (storage != null) {
                if (!storage.remove(this)) {
                    log.error("Could not remove shared object.");
                }
            }
        }
    }

    public HashSet getListeners() {
        return listeners;
    }

    public void beginUpdate() {
        beginUpdate(source);
    }

    public void beginUpdate(IEventListener listener) {
        source = listener;
        updateCounter += 1;
    }

    public void endUpdate() {
        updateCounter -= 1;
        if (updateCounter == 0) {
            notifyModified();
            source = null;
        }
    }

    public void serialize(Output output) throws IOException {
        Serializer ser = new Serializer();
        ser.serialize(output, getName());
        ser.serialize(output, data);
    }

    public void deserialize(Input input) throws IOException {
        Deserializer deserializer = new Deserializer();
        name = (String) deserializer.deserialize(input);
        persistentSO = persistent = true;
        data.clear();
        data.putAll((Map<String, Object>) deserializer.deserialize(input));
        updateHashes();
        ownerMessage.setName(name);
        ownerMessage.setIsPersistent(true);
    }

    public void setStore(IPersistenceStore store) {
        this.storage = store;
    }

    public IPersistenceStore getStore() {
        return storage;
    }

    /**
	 * Deletes all the attributes and sends a clear event to all listeners. The
	 * persistent data object is also removed from a persistent shared object.
	 * 
	 * @return true if successful; false otherwise
	 */
    public boolean clear() {
        data.clear();
        ownerMessage.addEvent(Type.CLIENT_CLEAR_DATA, name, null);
        return data.isEmpty();
    }

    /**
	 * Detaches a reference from this shared object, this will destroy the
	 * reference immediately. This is useful when you don't want to proxy a
	 * shared object any longer.
	 */
    public void close() {
        data.clear();
        listeners.clear();
        hashes.clear();
        syncEvents.clear();
        data = null;
        listeners = null;
        hashes = null;
        ownerMessage = null;
        source = null;
        syncEvents = null;
        storage = null;
    }
}
