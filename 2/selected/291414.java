package javax.media.ding3d;

import java.net.URL;
import java.io.InputStream;

/**
 * The MediaContainerRetained object defines all rendering state that can
 * be set as a component object of a retained Soundscape node.
 */
class MediaContainerRetained extends NodeComponentRetained {

    /**
      *  Gain Scale Factor applied to source with this attribute
      */
    boolean cached = true;

    /**
      *  URL string that references the sound data
      */
    URL url = null;

    String urlString = null;

    InputStream inputStream = null;

    /**
     * Set Cached flag
     * @param state flag denoting sound data is cached by app within node
     */
    void setCacheEnable(boolean state) {
        this.cached = state;
    }

    /**
     * Retrieve Attrribute Gain (amplitude)
     * @return gain amplitude scale factor
     */
    boolean getCacheEnable() {
        return this.cached;
    }

    /**
     * Set URL object that references the sound data
     * @param url URL object that references the sound data
     */
    void setURLObject(URL url) {
        setURLObject(url, true);
    }

    /**
     * Set URL object that references the sound data
     * @param url URL object that references the sound data
     * @param forceLoad ensures that message about change is sent to scheduler
     */
    void setURLObject(URL url, boolean forceLoad) {
        if (url != null) {
            if (urlString != null || inputStream != null) throw new IllegalArgumentException(Ding3dI18N.getString("MediaContainer5"));
            try {
                InputStream stream;
                stream = url.openStream();
                stream.close();
            } catch (Exception e) {
                throw new SoundException(javax.media.ding3d.Ding3dI18N.getString("MediaContainer0"));
            }
        }
        this.url = url;
        if (forceLoad) dispatchMessage();
    }

    /**
     * Set URL path that references the sound data
     * @param path string of URL that references the sound data
     */
    void setURLString(String path) {
        setURLString(path, true);
    }

    /**
     * Set URL path that references the sound data
     * @param path string of URL that references the sound data
     * @param forceLoad ensures that message about change is sent to scheduler
     */
    void setURLString(String path, boolean forceLoad) {
        if (path != null) {
            if (this.url != null || inputStream != null) throw new IllegalArgumentException(Ding3dI18N.getString("MediaContainer5"));
            try {
                URL url = new URL(path);
                InputStream stream;
                stream = url.openStream();
                stream.close();
            } catch (Exception e) {
                throw new SoundException(javax.media.ding3d.Ding3dI18N.getString("MediaContainer0"));
            }
        }
        this.urlString = path;
        if (forceLoad) dispatchMessage();
    }

    /**
     * Set input stream reference to sound data
     * @param stream InputStream that references the sound data
     * @param forceLoad ensures that message about change is sent to scheduler
     */
    void setInputStream(InputStream stream) {
        setInputStream(stream, true);
    }

    /**
     * Set input stream reference to sound data
     * @param stream InputStream that references the sound data
     */
    void setInputStream(InputStream stream, boolean forceLoad) {
        if (stream != null) {
            if (url != null || urlString != null) throw new IllegalArgumentException(Ding3dI18N.getString("MediaContainer5"));
        }
        this.inputStream = stream;
        if (forceLoad) dispatchMessage();
    }

    /**
     * Retrieve URL String
     * @return URL string that references the sound data
     */
    String getURLString() {
        return this.urlString;
    }

    /**
     * Retrieve URL objects
     * @return URL object that references the sound data
     */
    URL getURLObject() {
        return this.url;
    }

    /**
     * Retrieve InputData
     * @return InputString that references the sound data
     */
    InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * Dispatch a message about a media container change
     */
    void dispatchMessage() {
        Ding3dMessage createMessage = new Ding3dMessage();
        createMessage.threads = Ding3dThread.SOUND_SCHEDULER;
        createMessage.type = Ding3dMessage.MEDIA_CONTAINER_CHANGED;
        createMessage.universe = null;
        createMessage.args[0] = this;
        createMessage.args[1] = new Integer(SoundRetained.SOUND_DATA_DIRTY_BIT);
        createMessage.args[2] = new Integer(users.size());
        createMessage.args[3] = users;
        VirtualUniverse.mc.processMessage(createMessage);
    }
}
