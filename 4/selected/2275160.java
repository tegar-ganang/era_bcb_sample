package org.charvolant.tmsnet.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.charvolant.tmsnet.TMSNetPreferences;

/**
 * A factory for resources.
 * <p>
 * Resources have to be versioned, so that the factory can determine whether a newer
 * version of the resource is available.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public abstract class ResourceFactory<Resource extends VersionedResource> {

    /** The logger for the factory */
    private static final Logger logger = Logger.getLogger(ResourceFactory.class.getName());

    /** The context for loading resources */
    private JAXBContext context;

    /** The save directory */
    private File save;

    /** The base URL */
    private URL base;

    /**
   * Construct a network map factory.
   */
    protected ResourceFactory() {
        super();
        try {
            this.context = JAXBContext.newInstance(this.getClass().getPackage().getName() + ":" + AbstractIdentifiable.class.getPackage().getName());
            this.save = TMSNetPreferences.getInstance().getSave();
            this.base = TMSNetPreferences.getInstance().getUpdate();
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, "Unable to create context", ex);
        }
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
   * Get the name of the subdirectory used to store these items.
   * <p>
   * In the save of saved items, it is the saved subdirectory.
   * In the case of update URLs it is the first part of the path after the base URL.
   * 
   * @return The home directory
   */
    protected abstract String getHomeDirectory();

    /**
   * Get the name of a resource fitting the locale.
   * 
   * @return The localised resource name
   */
    protected abstract String getLocalisedResourceName();

    /**
   * Get the name of a default resource
   * 
   * @return The default resource name
   * 
   */
    protected abstract String getDefaultResourceName();

    /**
   * Get the location of default instance of the resource.
   * <p>
   * If there is a localised version of the network map, then use that.
   * Otherwise, just go for the complete default.
   * <p>
   * These URLs are based on the factory class and don't use
   * the {@link #getHomeDirectory()}.
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
        String dir = this.getHomeDirectory() + "/";
        String name = this.getLocalisedResourceName();
        try {
            resourceBase = this.getBase();
            resourceBase = new URL(resourceBase, dir);
            return new URL(resourceBase, name);
        } catch (Exception ex) {
            this.logger.info("Can't access resource from base " + resourceBase + ": " + ex.getMessage());
        }
        return null;
    }

    /**
   * Get the current local resource.
   * 
   * @return The local copy of the network map or null if not present
   */
    @SuppressWarnings("unchecked")
    protected Resource getLocalMap() {
        File base, local;
        Unmarshaller unmarshaller;
        base = new File(this.save, this.getHomeDirectory());
        local = new File(base, this.getLocalisedResourceName());
        if (!local.exists()) local = new File(base, this.getDefaultResourceName());
        if (!local.exists()) return null;
        try {
            unmarshaller = this.context.createUnmarshaller();
            return (Resource) unmarshaller.unmarshal(local);
        } catch (JAXBException ex) {
            this.logger.log(Level.SEVERE, "Unable to get local resource from " + local);
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
    @SuppressWarnings("unchecked")
    protected Resource check(Resource local, URL location) {
        Resource update;
        try {
            Unmarshaller unmarshaller = this.context.createUnmarshaller();
            update = (Resource) unmarshaller.unmarshal(location);
            if (local == null || (update != null && update.getVersion().compareTo(local.getVersion()) > 0)) {
                this.logger.fine("Updating from " + (local == null ? null : local.getVersion()) + " to " + (update == null ? null : update.getVersion()) + " from " + location);
                try {
                    File copy = this.copyMap(location);
                    update = (Resource) unmarshaller.unmarshal(copy);
                    return update;
                } catch (Exception ex) {
                    this.logger.log(Level.SEVERE, "Unable to update resource", ex);
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
    protected Resource checkDefault() {
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
        File saveDir = new File(this.save, this.getHomeDirectory());
        File saveFile = new File(saveDir, name);
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
    @SuppressWarnings("unchecked")
    protected File copyMap(URL original) throws IOException, JAXBException, IllegalArgumentException, URISyntaxException {
        File name = new File(original.getPath());
        File destination = new File(this.save, this.getHomeDirectory());
        URL base = new URL(original, ".");
        Unmarshaller unmarshaller;
        Marshaller marshaller;
        Resource resource;
        Map<URI, URI> copied = new HashMap<URI, URI>(128);
        this.logger.fine("Copy map from " + original + " with base " + base);
        if (destination.exists() && !destination.isDirectory()) throw new IllegalArgumentException("Expect " + destination + " to be a directory");
        if (!destination.exists()) if (!destination.mkdirs()) throw new IOException("Unable to make " + destination);
        name = new File(destination, name.getName());
        unmarshaller = this.context.createUnmarshaller();
        resource = (Resource) unmarshaller.unmarshal(original);
        resource.copyResources(this, base, copied);
        marshaller = this.context.createMarshaller();
        marshaller.marshal(resource, name);
        return name;
    }

    /**
   * Get the resource.
   * <p>
   * If there is a local version, then get that.
   * Otherwise, try to make a local copy of the resource from the stored default.
   * 
   * @return The network map.
   */
    public Resource getResource() {
        return this.checkDefault();
    }

    /**
   * Check to see if the current resource needs updating.
   * 
   * @return The current version of the resource.
   */
    public Resource checkCurrent() {
        return this.check(this.getLocalMap(), this.getCurrentLocalisedResourceURL());
    }
}
