package annone.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import annone.database.Database;
import annone.database.LocalConnection;
import annone.engine.Version;
import annone.util.AnnoneException;
import annone.util.Const;
import annone.util.Nullable;
import annone.util.Safe;
import annone.util.Text;
import annone.util.Tools;

public class Workspace {

    private final File location;

    private OutputStream lock;

    private Database metadata;

    public Workspace(File location) {
        this.location = location;
    }

    public File getLocation() {
        return location;
    }

    public File getVersionFile() {
        return new File(location, ".version");
    }

    public File getLockFile() {
        return new File(location, ".lock");
    }

    public boolean isLocked() {
        return (lock != null);
    }

    public synchronized boolean lock() {
        checkLocation(location);
        try {
            lock = new FileOutputStream(getLockFile());
            return true;
        } catch (FileNotFoundException xp) {
            return false;
        }
    }

    public synchronized void unlock() {
        if (lock != null) {
            Safe.close(lock);
            lock = null;
        }
    }

    public File getBinariesDirectory() {
        return new File(location, "bin");
    }

    public File getConfigurationDirectory() {
        return new File(location, "conf");
    }

    public File getLibrariesDirectory() {
        return new File(location, "lib");
    }

    public File getComponentsDirectory() {
        return new File(location, "comp");
    }

    public File getSourcesDirectory() {
        return new File(location, "src");
    }

    public File getDocumentsDirectory() {
        return new File(location, "doc");
    }

    public File getTempDirectory() {
        return new File(location, "temp");
    }

    public File getUpdatesDirectory() {
        return new File(location, "updates");
    }

    public File getChannelsDirectory() {
        return new File(location, "channels");
    }

    public Version getVersion() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(getVersionFile()), Const.UTF_8));
            try {
                String versionString = r.readLine();
                Version version = Version.parseVersion(versionString);
                return version;
            } catch (IOException xp) {
                throw new AnnoneException(Text.get("Can''t retrieve workspace version."));
            } finally {
                Safe.close(r);
            }
        } catch (FileNotFoundException xp) {
            return Version.UNSPECIFIED;
        }
    }

    public Database getMetadata() {
        if (metadata == null) {
            File metadataFile = new File(getBinariesDirectory(), "metadata.sqlite");
            boolean exists = metadataFile.exists();
            metadata = new Database("jdbc:sqlite:" + metadataFile.getAbsolutePath(), null, null);
            if (!exists) {
                LocalConnection connection = metadata.newLocalConnection();
                try {
                    connection.script(Tools.readReader(new FileReader(new File(getBinariesDirectory(), "metadata.sql"))));
                } catch (Throwable xp) {
                    throw new AnnoneException(Text.get("Can''t create metadata database."), xp);
                } finally {
                    connection.close();
                }
            }
        }
        return metadata;
    }

    public void create(@Nullable URL template) {
        checkLocation(location);
        if (template != null) {
            String protocol = template.getProtocol();
            if ("file".equalsIgnoreCase(protocol)) try {
                createFromDirectory(new File(template.toURI()));
            } catch (URISyntaxException xp) {
                throw new AnnoneException(Text.get("Invalid template location."), xp);
            }
        }
    }

    private void createFromDirectory(File template) {
        if (!template.isDirectory()) throw new AnnoneException(Text.get("''{0}'' must be a directory.", template.getAbsolutePath()));
        Tools.directoryCopy(template, location, null, null);
    }

    private void checkLocation(File location) {
        if (!location.mkdirs()) if (!location.exists()) throw new AnnoneException(Text.get("Can''t create location ''{0}''.", location.getAbsolutePath()));
        if (!location.isDirectory()) throw new AnnoneException(Text.get("''{0}'' must be a directory.", location.getAbsolutePath()));
    }
}
