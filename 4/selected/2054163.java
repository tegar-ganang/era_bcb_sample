package nl.headspring.photoz.imagecollection.fs;

import nl.headspring.commons.xml.XML;
import nl.headspring.commons.xml.XMLBean;
import nl.headspring.photoz.common.Chrono;
import nl.headspring.photoz.common.Configuration;
import nl.headspring.photoz.common.Constants;
import nl.headspring.photoz.common.Utils;
import nl.headspring.photoz.common.eventbus.EventBus;
import nl.headspring.photoz.common.task.SerialTaskRunner;
import nl.headspring.photoz.common.task.Task;
import nl.headspring.photoz.imagecollection.Annotation;
import nl.headspring.photoz.imagecollection.Annotations;
import nl.headspring.photoz.imagecollection.BaseAnnotation;
import nl.headspring.photoz.imagecollection.BaseExifMetadata;
import nl.headspring.photoz.imagecollection.ExifMetadata;
import nl.headspring.photoz.imagecollection.ImageCollectionException;
import nl.headspring.photoz.imagecollection.PlaceholderAnnotation;
import nl.headspring.photoz.imagecollection.Property;
import nl.headspring.photoz.imagecollection.fs.events.AnnotationUpdatedBusEvent;
import nl.headspring.photoz.imagecollection.fs.events.FolderUpdatedBusEvent;
import nl.headspring.photoz.imagecollection.fs.metadata.ExifMetadataReader;
import nl.headspring.photoz.imagecollection.fs.metadata.ExifMetadataWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Checksum;
import static nl.headspring.photoz.common.Constants.APP_NAME;
import static nl.headspring.photoz.common.Utils.hash256;
import static nl.headspring.photoz.common.Utils.xmlClean;

/**
 * Class ShutdownHook.
 * <p/>
 * Stores the annotations as xml files alongside the thumbnails.
 *
 * @author Eelco Sommer
 * @since Sep 21, 2010
 */
public class AsynchronousFileSystemAnnotations implements Annotations {

    private static final Log LOG = LogFactory.getLog(AsynchronousFileSystemAnnotations.class);

    private static final String STORAGE_DATE_FORMAT = "dd-MM-yyyy kk:mm:ss";

    private final String LOCK_PATH = System.getProperty("java.io.tmpdir") + File.separator + "~" + APP_NAME + ".lock";

    private final Configuration configuration;

    private final ExifMetadataReader metaDataReader;

    private final ExifMetadataWriter exifMetadataWriter;

    private final SerialTaskRunner taskRunner;

    private final EventBus eventBus;

    private final File db;

    private final List<String> unreadables = new ArrayList<String>();

    private final List<String> unwriteables = new ArrayList<String>();

    private final Map<File, String> generatingA = new HashMap<File, String>();

    private final int[] writeLock = new int[] {};

    public AsynchronousFileSystemAnnotations(Configuration configuration, ExifMetadataReader metaDataReader, ExifMetadataWriter exifMetadataWriter, SerialTaskRunner taskRunner, EventBus eventBus) {
        this.configuration = configuration;
        this.metaDataReader = metaDataReader;
        this.exifMetadataWriter = exifMetadataWriter;
        this.taskRunner = taskRunner;
        this.eventBus = eventBus;
        lock();
        this.db = new File(configuration.get("db.home"), "an");
        if (!db.exists()) {
            if (!db.mkdirs()) {
                throw new ImageCollectionException("Could not create image annotation directory: " + db);
            }
        }
        Utils.readAll(new File(db, "UNREADABLE"), unreadables);
        Utils.readAll(new File(db, "UNWRITEABLE"), unwriteables);
    }

    private void lock() {
        try {
            LOG.info("Locking " + Constants.APP_NICE_NAME);
            if (new File(LOCK_PATH).exists()) {
                throw new RuntimeException("Database " + Constants.APP_NICE_NAME + " locked");
            }
            final FileWriter writer = new FileWriter(LOCK_PATH);
            writer.write("*lock*");
            writer.flush();
            writer.close();
            ShutdownHook shutdownHook = new ShutdownHook(LOCK_PATH);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IOException e) {
            LOG.error("Error locking " + Constants.APP_NICE_NAME);
        }
    }

    public File getHashedStoragePath(String uniqueImageId) {
        return StringUtils.isEmpty(uniqueImageId) ? null : new File(new File(db, hash256(uniqueImageId)), uniqueImageId);
    }

    public File getHashedStoragePath(File imageFile) {
        return getHashedStoragePath(getImageUniqueId(imageFile));
    }

    public File getHashedStoragePath(Annotation annotation) {
        return getHashedStoragePath(annotation.getUniqueImageId());
    }

    public String getImageUniqueId(File imageFile) {
        return metaDataReader.getImageUniqueId(imageFile);
    }

    public String createUniqueImageId() {
        return "{" + UUID.randomUUID().toString() + "}";
    }

    public void create(String uniqueImageId, ExifMetadata metadata, String path, String localPath, Date creationTime, Checksum checksum) {
        throw new UnsupportedOperationException();
    }

    public boolean exists(String uniqueImageId) {
        throw new UnsupportedOperationException();
    }

    public Annotation get(final File imageFile) {
        synchronized (writeLock) {
            if (unreadables.contains(imageFile.getAbsolutePath()) || unwriteables.contains(imageFile.getAbsolutePath())) {
                return new BlacklistedAnnotation(imageFile);
            }
            if (alreadyA(imageFile)) {
                return new PlaceholderAnnotation(imageFile, getA(imageFile));
            }
            String uniqueImageId = null;
            try {
                uniqueImageId = getImageUniqueId(imageFile);
            } catch (Throwable t) {
                LOG.error("Blacklisting '" + imageFile + "' for unreadable exif data", t);
                unreadables.add(imageFile.getAbsolutePath());
                return new BlacklistedAnnotation(imageFile);
            }
            if (annotationExists(uniqueImageId)) {
                return new PlaceholderAnnotation(imageFile, uniqueImageId);
            }
            final ExifMetadata metadata = metaDataReader.read(imageFile);
            if (StringUtils.isEmpty(uniqueImageId)) {
                final String newUniqueImageId = createUniqueImageId();
                addA(imageFile, newUniqueImageId);
                taskRunner.add(new Task() {

                    public String getName() {
                        return imageFile.getName();
                    }

                    public void execute() {
                        try {
                            exifMetadataWriter.setImageUniqueId(imageFile, newUniqueImageId);
                            LOG.info("Image unique id " + newUniqueImageId + " injected into " + imageFile);
                            removeA(imageFile);
                            eventBus.publish(new AnnotationUpdatedBusEvent(read(newUniqueImageId)));
                        } catch (Throwable t) {
                            LOG.error("Blacklisting '" + imageFile + "' for unwriteable exif data", t);
                            unwriteables.add(imageFile.getAbsolutePath());
                            eventBus.publish(new FolderUpdatedBusEvent(new Folder(imageFile.getParentFile())));
                        }
                    }
                }, true);
                return new PlaceholderAnnotation(imageFile, newUniqueImageId);
            } else {
                final String finalUniqueImageId = uniqueImageId;
                addA(imageFile, uniqueImageId);
                taskRunner.add(new Task() {

                    public String getName() {
                        return imageFile.getName();
                    }

                    public void execute() {
                        String userComment = metadata.get("UserComment");
                        if (StringUtils.isEmpty(userComment)) {
                            userComment = Utils.stripExtension(imageFile.getName());
                        }
                        Annotation newAnnotation = new BaseAnnotation(finalUniqueImageId, Utils.stripExtension(imageFile.getName()), new Date(), imageFile.getParent(), imageFile.getName(), null, null, userComment, null, metadata);
                        store(newAnnotation);
                        removeA(imageFile);
                        eventBus.publish(new FolderUpdatedBusEvent(new Folder(imageFile.getParentFile())));
                    }
                }, true);
                return new PlaceholderAnnotation(imageFile, finalUniqueImageId);
            }
        }
    }

    public Annotation get(String uniqueImageId) {
        return read(uniqueImageId);
    }

    public Iterator<Annotation> iterator() {
        LOG.warn("iterator() not implemented");
        return Collections.<Annotation>emptyList().iterator();
    }

    private boolean annotationExists(String uniqueImageId) {
        return StringUtils.isNotEmpty(uniqueImageId) && new File(getHashedStoragePath(uniqueImageId), uniqueImageId + ".xml").exists();
    }

    public Annotation read(String uniqueImageId) {
        synchronized (writeLock) {
            final File annotationFile = new File(getHashedStoragePath(uniqueImageId), uniqueImageId + ".xml");
            if (!annotationFile.exists()) {
                return null;
            } else {
                XML xml = XML.read(annotationFile);
                XMLBean root = xml.get("annotation");
                return new BaseAnnotation(getText(root.get("unique-image-id")), getText(root.get("name")), getDate(root.get("createion-time")), getText(root.get("folder")), getText(root.get("resource")), getText(root.get("local-folder")), getText(root.get("local-resource")), getText(root.get("description")), null, readMetadata(root.get("exif-meta-data")));
            }
        }
    }

    private ExifMetadata readMetadata(XMLBean bean) {
        ExifMetadata metadata = new BaseExifMetadata();
        for (XMLBean property : bean.children("property")) {
            String key = getText(property.get("key"));
            if (StringUtils.isNotEmpty(key)) {
                metadata.set(key, getText(property.get("value")));
            }
        }
        return metadata;
    }

    private String getText(XMLBean bean) {
        return bean == null ? "" : bean.getText();
    }

    private Date getDate(XMLBean bean) {
        try {
            return bean == null ? null : createDateFormat().parse(bean.getText());
        } catch (ParseException e) {
            LOG.error("Invalid date in " + bean.getTag());
            return null;
        }
    }

    private SimpleDateFormat createDateFormat() {
        return new SimpleDateFormat(STORAGE_DATE_FORMAT);
    }

    /**
   * Todo: optimize this!
   *
   * @param folder
   * @return
   */
    public List<Annotation> find(Folder folder) {
        Chrono c = new Chrono();
        List<Annotation> annotations = new ArrayList<Annotation>();
        for (Resource r : folder.getResources()) {
            if (r.getName().toLowerCase().endsWith(".jpg") || r.getName().toLowerCase().endsWith(".jpeg")) {
                final Annotation annotation = get(new File(r.getAbsolutePath()));
                if (annotation != null) {
                    annotations.add(annotation);
                }
            }
        }
        return annotations;
    }

    /**
   * {@inheritDoc}
   * <p/>
   * The annotation is stored as XML in the hashed storage path.
   */
    public void store(Annotation annotation) {
        final File storageDir = getHashedStoragePath(annotation);
        if (annotation == null || annotation.getUniqueImageId() == null || storageDir == null) {
            LOG.error("Could not determine storage path for " + annotation);
            return;
        }
        final File xmlFile = new File(storageDir, annotation.getUniqueImageId() + ".xml");
        LOG.debug("Saving " + xmlFile);
        XML xml = new XML("<annotation/>");
        xml.add("unique-image-id", annotation.getUniqueImageId());
        xml.add("name", annotation.getName());
        xml.add("folder", annotation.getFolder());
        xml.add("resource", annotation.getResource());
        xml.add("description", xmlClean(annotation.getDescription()));
        XMLBean exifMetaBean = xml.add("exif-meta-data");
        for (Property property : annotation.getMetaData()) {
            XMLBean p = exifMetaBean.add("property");
            XMLBean keyBean = p.add("key", xmlClean(property.getKey()));
            XMLBean valueBean = p.add("value", xmlClean(property.getValue()));
        }
        try {
            Document document = xml.getDocument();
            OutputFormat outputFormat = new OutputFormat(document, "UTF-8", true);
            outputFormat.setIndent(2);
            outputFormat.setLineWidth(120);
            outputFormat.setOmitXMLDeclaration(false);
            synchronized (writeLock) {
                xmlFile.getParentFile().mkdirs();
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(xmlFile)));
                XMLSerializer xmlSerializer = new XMLSerializer(writer, outputFormat);
                xmlSerializer.serialize(document);
                writer.flush();
                writer.close();
            }
        } catch (FileNotFoundException e) {
            LOG.error("Error creating xml file " + xmlFile, e);
        } catch (IOException e) {
            LOG.error("Error serializing xml to file " + xmlFile, e);
        }
    }

    public void close() {
        LOG.info("Flushing annotation blacklists");
        Utils.writeAll(new File(db, "UNREADABLE"), unreadables);
        Utils.writeAll(new File(db, "UNWRITEABLE"), unwriteables);
    }

    private boolean alreadyA(File imageFile) {
        synchronized (generatingA) {
            return generatingA.keySet().contains(imageFile);
        }
    }

    private void addA(File imageFile, String newUniqueImageId) {
        synchronized (generatingA) {
            generatingA.put(imageFile, newUniqueImageId);
        }
    }

    private void removeA(File imageFile) {
        synchronized (generatingA) {
            generatingA.remove(imageFile);
        }
    }

    /**
   * Returns the new unique image associated with the file.
   *
   * @param imageFile
   * @return
   */
    private String getA(File imageFile) {
        synchronized (generatingA) {
            return generatingA.get(imageFile);
        }
    }
}
