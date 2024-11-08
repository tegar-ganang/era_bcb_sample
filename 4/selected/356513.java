package org.charvolant.tmsnet.networks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.charvolant.tmsnet.TMSNetPreferences;

/**
 * How to get a network map.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class NetworkMapFactory {

    /** The logger for the factory */
    private static final Logger logger = Logger.getLogger(NetworkMapFactory.class.getName());

    /** The location for the country-specific default map, {0} is the country code. */
    private static final String DEFAULT_COUNTRY_URL = "network-map-{0}.xml";

    /** The location for the default map */
    private static final String DEFAULT_URL = "network-map-default.xml";

    /** The singleton instance */
    private static final NetworkMapFactory INSTANCE = new NetworkMapFactory();

    /** The context for loading maps */
    private JAXBContext context;

    /** The save directory */
    private File save;

    /** The base URL */
    private URL base;

    /**
   * Construct a network map factory.
   */
    protected NetworkMapFactory() {
        super();
        try {
            this.context = JAXBContext.newInstance(this.getClass().getPackage().getName());
            this.save = TMSNetPreferences.getInstance().getSave();
            this.base = TMSNetPreferences.getInstance().getUpdate();
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, "Unable to create context", ex);
        }
    }

    /**
   * Get the network map factory instance.
   * 
   * @return The common network map factory
   */
    public static NetworkMapFactory getInstance() {
        return INSTANCE;
    }

    /**
   * Get the save directory.
   *
   * @return the save
   */
    public File getSave() {
        return this.save;
    }

    /**
   * Set the save directory.
   *
   * @param save the save to set
   */
    protected void setSave(File save) {
        this.save = save;
    }

    /**
   * Get the base.
   *
   * @return the base
   */
    public URL getBase() {
        return this.base;
    }

    /**
   * Set the base URL.
   *
   * @param base the base to set
   */
    protected void setBase(URL base) {
        this.base = base;
    }

    /**
   * Get the name of a resource fitting the locale.
   * 
   * @return The localised resource name
   */
    protected String getLocalisedResourceName() {
        return MessageFormat.format(this.DEFAULT_COUNTRY_URL, Locale.getDefault().getCountry());
    }

    /**
   * Get the name of a default resource
   * 
   * @return The default resource name
   * 
   */
    protected String getDefaultResourceName() {
        return this.DEFAULT_URL;
    }

    /**
   * Get the location of default instance of the network map.
   * <p>
   * If there is a country-specific version of the network map, then use that.
   * Otherwise, just go for the complete default.
   * 
   * @return The default resource
   */
    protected URL getDefaultResourceURL() {
        URL resource;
        resource = this.getClass().getResource(this.getLocalisedResourceName());
        if (resource == null) resource = this.getClass().getResource(this.getDefaultResourceName());
        return resource;
    }

    /**
   * Get the location of the currently updated instance of the resource map.
   * 
   * @return The location of the current networks resource, or null if not available
   */
    protected URL getCurrentLocalisedResourceURL() {
        URL resourceBase = null;
        String name = this.getLocalisedResourceName();
        try {
            resourceBase = this.getBase();
            return new URL(resourceBase, name);
        } catch (Exception ex) {
            this.logger.info("Can't access resource from base " + resourceBase + ": " + ex.getMessage());
        }
        return null;
    }

    /**
   * Get the current local map.
   * 
   * @return The local copy of the network map or null if not present
   */
    protected NetworkMap getLocalMap() {
        File local;
        Unmarshaller unmarshaller;
        local = new File(this.save, this.getLocalisedResourceName());
        if (!local.exists()) local = new File(this.save, this.getDefaultResourceName());
        if (!local.exists()) return null;
        try {
            unmarshaller = this.context.createUnmarshaller();
            return (NetworkMap) unmarshaller.unmarshal(local);
        } catch (JAXBException ex) {
            this.logger.log(Level.SEVERE, "Unable to get local network map from " + local);
        }
        return null;
    }

    /**
   * Check to see whether we should update the local copy with a current
   * copy on the web.
   * 
   * @param local The local network map
   * @param update The updated (maybe) network map
   * 
   * @return The network map used
   */
    protected NetworkMap check(NetworkMap local, URL location) {
        NetworkMap update;
        try {
            Unmarshaller unmarshaller = this.context.createUnmarshaller();
            update = (NetworkMap) unmarshaller.unmarshal(location);
            if (local == null || (update != null && update.getVersion().compareTo(local.getVersion()) > 0)) {
                this.logger.fine("Updating from " + (local == null ? null : local.getVersion()) + " to " + (update == null ? null : update.getVersion()) + " from " + location);
                try {
                    File copy = this.copyMap(location);
                    update = (NetworkMap) unmarshaller.unmarshal(copy);
                    return update;
                } catch (Exception ex) {
                    this.logger.log(Level.SEVERE, "Unable to update network map", ex);
                }
            } else {
                this.logger.fine("No update from " + local.getVersion() + " to " + update.getVersion() + " from " + location);
            }
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, "Unable to update from " + location, ex);
        }
        return local;
    }

    /**
   * Check to see whether we should update the local copy with a current
   * copy on the web.
   * 
   * @return True if the local network map was updated
   */
    protected NetworkMap checkDefault() {
        return this.check(this.getLocalMap(), this.getDefaultResourceURL());
    }

    /**
   * Copy a file from an original into a destination location
   * 
   * @param original The original
   * @param destination The destination
   * @param copied The uris already copied
   * 
   * @return The local copy
   * 
   * @throws IOException if unable to make the copy
   * @throws URISyntaxException if the original is not a URI
   */
    protected URI copyFile(URL base, String name, Map<URI, URI> copied) throws IOException, URISyntaxException {
        URL original = new URL(base, name);
        URI originalURI = original.toURI();
        File saveFile = new File(this.save, name);
        URI saveURI = saveFile.toURI();
        InputStream is = original.openStream();
        OutputStream os;
        byte[] buffer = new byte[1024];
        int count;
        if (copied.containsKey(originalURI)) return copied.get(originalURI);
        this.logger.fine("Copy from " + original + " to " + saveFile);
        if (!saveFile.getParentFile().exists()) if (!saveFile.getParentFile().mkdirs()) throw new IOException("Can't create " + saveFile.getParentFile());
        os = new FileOutputStream(saveFile);
        while ((count = is.read(buffer)) > 0) os.write(buffer, 0, count);
        os.close();
        is.close();
        copied.put(originalURI, saveURI);
        copied.put(saveURI, saveURI);
        return saveURI;
    }

    /**
   * Copy a map and all the resources it references into a save directory.
   * 
   * @param original Where to get the original map from
   * 
   * @return The copied file
   * 
   * @throws IOException if unable to make the copy
   * @throws JAXBException if the map is invalid
   * @throws IllegalArgumentException if the destination is not a makable directory
   * @throws URISyntaxException if there is a URI mapping problem
   */
    protected File copyMap(URL original) throws IOException, JAXBException, IllegalArgumentException, URISyntaxException {
        File name = new File(original.getPath());
        URL base = new URL(original, ".");
        Unmarshaller unmarshaller;
        Marshaller marshaller;
        NetworkMap map;
        Map<URI, URI> copied = new HashMap<URI, URI>(128);
        this.logger.fine("Copy map from " + original + " with base " + base);
        if (this.save.exists() && !this.save.isDirectory()) throw new IllegalArgumentException("Expect " + this.save + " to be a directory");
        if (!this.save.exists()) if (!this.save.mkdirs()) throw new IOException("Unable to make " + this.save);
        name = new File(this.save, name.getName());
        unmarshaller = this.context.createUnmarshaller();
        map = (NetworkMap) unmarshaller.unmarshal(original);
        map.copyResources(this, base, copied);
        marshaller = this.context.createMarshaller();
        marshaller.marshal(map, name);
        return name;
    }

    /**
   * Get the network map.
   * <p>
   * If there is a local version, then get that.
   * Otherwise, try to make a local copy of the map from the stored default.
   * 
   * @return The network map.
   */
    public NetworkMap getNetworkMap() {
        return this.checkDefault();
    }

    /**
   * Check to see if the current map needs updating.
   * 
   * @return The current version of the network map.
   */
    public NetworkMap checkCurrent() {
        return this.check(this.getLocalMap(), this.getCurrentLocalisedResourceURL());
    }
}
