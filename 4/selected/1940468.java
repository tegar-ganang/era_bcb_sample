package net.sf.fileexchange.api;

import static net.sf.fileexchange.api.FileUtil.deleteFileTree;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import net.sf.fileexchange.api.snapshot.ModelSnapshot;

public class Profile implements Closeable {

    private static final String MODEL_FILE_NAME = "model.xml";

    private static final String APPLICATION_NAME_PROPERTY = "application.name";

    private static Version VERSION = new Version(0, 6, Build.RELEASE);

    private static final String VERSION_PROPERTY = "version";

    private static final String PROPERTIES_FILE_NAME = "profile.properties";

    private static final String UPLOADS_DIRECTORY_NAME = "uploads";

    /**
	 * The version of the profile. This variable is never null for public
	 * methods, but is null for a short time while {@link #create(File)} is
	 * running.
	 */
    private Version version;

    private final File directory;

    private final File modelFile;

    private JAXBContext modelContext;

    private Unmarshaller modelUnmarschaller;

    private final Marshaller modelMarshaller;

    private final RandomAccessFile propertiesFile;

    private final File uploadsDirectory;

    private Profile(Version version, File directory, RandomAccessFile propertiesFile) {
        this.version = version;
        this.directory = directory;
        this.modelFile = new File(directory, MODEL_FILE_NAME);
        try {
            this.modelContext = JAXBContext.newInstance(ModelSnapshot.class);
            this.modelUnmarschaller = modelContext.createUnmarshaller();
            this.modelMarshaller = modelContext.createMarshaller();
            this.modelMarshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        this.propertiesFile = propertiesFile;
        this.uploadsDirectory = new File(directory, UPLOADS_DIRECTORY_NAME);
    }

    private static final class UnknownFilesFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.equals(PROPERTIES_FILE_NAME)) return false;
            if (name.equals(MODEL_FILE_NAME)) return false;
            if (name.equals(UPLOADS_DIRECTORY_NAME)) return false;
            return true;
        }
    }

    public static class CorruptProfileException extends Exception {

        private static final long serialVersionUID = 1L;

        public CorruptProfileException() {
        }

        public CorruptProfileException(Throwable cause) {
            super(cause);
        }
    }

    public static class ProfileInUseException extends Exception {

        private static final long serialVersionUID = 1L;

        private final File profileDirectory;

        public File getProfileDiretory() {
            return profileDirectory;
        }

        public ProfileInUseException(File profileDirectory) {
            super();
            this.profileDirectory = profileDirectory;
        }
    }

    public static Profile load(final File profileDirectory) throws CorruptProfileException, IOException, ProfileInUseException {
        if (!profileDirectory.exists()) {
            return null;
        }
        if (!profileDirectory.isDirectory()) throw new CorruptProfileException();
        final RandomAccessFile propertiesFileAccess = openPropertiesFile(profileDirectory);
        boolean success = false;
        try {
            FileLock lock = propertiesFileAccess.getChannel().tryLock();
            if (lock == null) throw new ProfileInUseException(profileDirectory);
            Properties properties = new Properties();
            try {
                properties.load(Channels.newInputStream(propertiesFileAccess.getChannel()));
            } catch (FileNotFoundException e) {
                throw new CorruptProfileException();
            }
            if (!ApplicationConstants.NAME.equals(properties.getProperty(APPLICATION_NAME_PROPERTY))) {
                throw new CorruptProfileException();
            }
            final Version version = readRequiredVersionProperty(properties, VERSION_PROPERTY);
            if (version.getBuild() != Build.RELEASE) throw new CorruptProfileException();
            final Profile profile = new Profile(version, profileDirectory, propertiesFileAccess);
            if (version.compareTo(VERSION) < 0) profile.saveModelSnapshot(profile.loadModelSnapshot());
            final File uploadsDirectory = new File(profileDirectory, UPLOADS_DIRECTORY_NAME);
            if (!uploadsDirectory.isDirectory()) throw new CorruptProfileException();
            success = true;
            return profile;
        } finally {
            if (!success) propertiesFileAccess.close();
        }
    }

    private static RandomAccessFile openPropertiesFile(final File profileDirectory) throws FileNotFoundException {
        final File propertiesFile = new File(profileDirectory, PROPERTIES_FILE_NAME);
        final RandomAccessFile propertiesFileAccess = new RandomAccessFile(propertiesFile, "rw");
        return propertiesFileAccess;
    }

    public static Profile create(File profileDirectory) throws IOException {
        profileDirectory.mkdir();
        final RandomAccessFile propertiesFileAccess = openPropertiesFile(profileDirectory);
        boolean success = false;
        try {
            FileLock lock = propertiesFileAccess.getChannel().tryLock();
            if (lock == null) throw new IOException("Unable to obtain log in newly created profile");
            final Profile profile = new Profile(null, profileDirectory, propertiesFileAccess);
            profile.saveModelSnapshot(new ModelSnapshot());
            success = true;
            return profile;
        } finally {
            if (!success) propertiesFileAccess.close();
        }
    }

    private void writePropertiesFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(APPLICATION_NAME_PROPERTY, ApplicationConstants.NAME);
        properties.setProperty(VERSION_PROPERTY, version.toString());
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        properties.store(buffer, null);
        propertiesFile.seek(0);
        propertiesFile.write(buffer.toByteArray());
    }

    private static Version readRequiredVersionProperty(Properties properties, String property) throws CorruptProfileException {
        final String propertyValue = properties.getProperty(property);
        if (propertyValue == null) throw new CorruptProfileException();
        try {
            return Version.valueOf(propertyValue);
        } catch (IllegalArgumentException e) {
            throw new CorruptProfileException();
        }
    }

    /**
	 * 
	 * @return true, if the application should be able to read and use some
	 *         information from the profile folder. If this method returns false
	 *         the application must not try to read the profile folder.
	 */
    public boolean isPartiallyReadable() {
        return VERSION.getMajor() == version.getMajor();
    }

    /**
	 * 
	 * @return true, if the application should be able to use all the
	 *         information which is available in the profile folder.
	 */
    public boolean isFullyReadable() {
        if (VERSION.getMajor() != version.getMajor()) return false;
        return VERSION.getMinor() >= version.getMinor();
    }

    /**
	 * 
	 * @return the directory where the profile data is located.
	 */
    public File getDirectory() {
        return directory;
    }

    public synchronized ModelSnapshot loadModelSnapshot() throws CorruptProfileException {
        try {
            return (ModelSnapshot) modelUnmarschaller.unmarshal(modelFile);
        } catch (JAXBException e) {
            throw new CorruptProfileException(e);
        }
    }

    public synchronized void saveModelSnapshot(ModelSnapshot snapshot) throws IOException {
        if (version == null || version.compareTo(VERSION) != 0) {
            for (File file : directory.listFiles(new UnknownFilesFilter())) {
                deleteFileTree(file);
            }
            this.version = VERSION;
            writePropertiesFile();
            if (!uploadsDirectory.exists()) {
                if (!uploadsDirectory.mkdir()) throw new IOException("Failed to create uploads directory.");
            }
        }
        try {
            modelMarshaller.marshal(snapshot, modelFile);
        } catch (JAXBException e) {
            IOException ioException = new IOException();
            ioException.initCause(e);
            throw ioException;
        }
    }

    /**
	 * Closing the profile releases the profile.properties file lock.
	 */
    @Override
    public void close() throws IOException {
        propertiesFile.close();
    }

    public File getUploadsDirectory() {
        return uploadsDirectory;
    }
}
