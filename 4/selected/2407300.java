package nl.headspring.photoz.imagecollection.fs;

import nl.headspring.commons.xml.XML;
import nl.headspring.commons.xml.XMLBean;
import nl.headspring.photoz.common.Configuration;
import nl.headspring.photoz.common.Convert;
import nl.headspring.photoz.common.StoredChecksum;
import nl.headspring.photoz.common.Utils;
import nl.headspring.photoz.common.eventbus.EventBus;
import nl.headspring.photoz.imagecollection.Annotation;
import nl.headspring.photoz.imagecollection.Annotations;
import nl.headspring.photoz.imagecollection.BaseAnnotation;
import nl.headspring.photoz.imagecollection.BaseExifMetadata;
import nl.headspring.photoz.imagecollection.ExifMetadata;
import nl.headspring.photoz.imagecollection.ImageCollectionException;
import nl.headspring.photoz.imagecollection.Property;
import nl.headspring.photoz.imagecollection.fs.metadata.ExifMetadataReader;
import nl.headspring.photoz.imagecollection.fs.metadata.ExifMetadataWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.Checksum;
import static nl.headspring.photoz.common.Constants.APP_NAME;
import static nl.headspring.photoz.common.Utils.xmlClean;

/**
 * Class ShutdownHook.
 * <p/>
 * Stores the annotations as xml files alongside the thumbnails.
 *
 * @author Eelco Sommer
 * @since Sep 21, 2010
 */
public class FileSystemAnnotations implements Annotations {

    private static final Log LOG = LogFactory.getLog(FileSystemAnnotations.class);

    private static final String STORAGE_DATE_FORMAT = "dd-MM-yyyy kk:mm:ss";

    private final String LOCK_PATH = System.getProperty("java.io.tmpdir") + File.separator + "~" + APP_NAME + ".lock";

    private final Configuration configuration;

    private final ExifMetadataReader metaDataReader;

    private final ExifMetadataWriter exifMetadataWriter;

    private final EventBus eventBus;

    private final FsUtils fsUtils;

    private final File db;

    private final List<String> unreadables = new ArrayList<String>();

    private final List<String> unwriteables = new ArrayList<String>();

    private final int[] writeLock = new int[] {};

    public FileSystemAnnotations(Configuration configuration, ExifMetadataReader metadataReader, ExifMetadataWriter metadataWriter, EventBus eventBus, FsUtils fsUtils) {
        this.configuration = configuration;
        this.metaDataReader = metadataReader;
        this.exifMetadataWriter = metadataWriter;
        this.eventBus = eventBus;
        this.fsUtils = fsUtils;
        this.db = new File(configuration.get("db.home"), "db");
        if (!db.exists()) {
            if (!db.mkdirs()) {
                throw new ImageCollectionException("Could not create image annotation directory: " + db);
            }
        }
        Utils.readAll(new File(db, "UNREADABLE"), unreadables);
        Utils.readAll(new File(db, "UNWRITEABLE"), unwriteables);
    }

    public String getImageUniqueId(File imageFile) {
        return metaDataReader.getImageUniqueId(imageFile);
    }

    public String createUniqueImageId() {
        return "{" + UUID.randomUUID().toString() + "}";
    }

    public void create(String uniqueImageId, ExifMetadata metadata, String path, String localPath, Date creationTime, Checksum checksum) {
        if (exists(uniqueImageId)) {
            LOG.warn("Not overwriting annotation for " + uniqueImageId + ", it already exists");
            return;
        }
        LOG.debug("Storing annotation for " + uniqueImageId);
        File storagePath = new File(path);
        File localStoragePath = new File(localPath);
        store(new BaseAnnotation(uniqueImageId, Utils.stripExtension(storagePath.getName()), creationTime, storagePath.getParent(), storagePath.getName(), localStoragePath.getParent(), localStoragePath.getName(), metadata.get("UserComment"), checksum, metadata));
    }

    public boolean exists(String uniqueImageId) {
        return StringUtils.isNotEmpty(uniqueImageId) && new File(fsUtils.getAbsoluteHashedDataFolder(uniqueImageId), "annotations.xml").exists();
    }

    public Annotation get(final File imageFile) {
        synchronized (writeLock) {
            if (unreadables.contains(imageFile.getAbsolutePath()) || unwriteables.contains(imageFile.getAbsolutePath())) {
                return new BlacklistedAnnotation(imageFile);
            }
            return null;
        }
    }

    public Annotation get(String uniqueImageId) {
        return read(uniqueImageId);
    }

    public Iterator<Annotation> iterator() {
        LOG.warn("iterator() not implemented");
        return Collections.<Annotation>emptyList().iterator();
    }

    public Annotation read(String uniqueImageId) {
        synchronized (writeLock) {
            final File annotationFile = new File(fsUtils.getAbsoluteHashedDataFolder(uniqueImageId), "annotations.xml");
            if (!annotationFile.exists()) {
                return null;
            } else {
                FileReader fileReader = null;
                try {
                    fileReader = Utils.open(annotationFile);
                    Reader reader = new BufferedReader(fileReader);
                    XML xml = XML.read(annotationFile);
                    XMLBean root = xml.get("annotation");
                    return new BaseAnnotation(getText(root.get("unique-image-id")), getText(root.get("name")), getDate(root.get("creation-time")), getText(root.get("folder")), getText(root.get("resource")), getText(root.get("local-folder")), getText(root.get("local-resource")), getText(root.get("description")), getChecksum(root.get("checksum")), readMetadata(root.get("exif-meta-data")));
                } finally {
                    Utils.close(fileReader);
                }
            }
        }
    }

    private Checksum getChecksum(XMLBean bean) {
        final StoredChecksum c = bean == null ? null : new StoredChecksum(Convert.toLong(bean.getText(), -1));
        return c;
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
   * {@inheritDoc}
   * <p/>
   * The annotation is stored as XML in the hashed storage path.
   */
    public void store(Annotation annotation) {
        if (annotation == null || annotation.getUniqueImageId() == null) {
            LOG.error("Invalid annotation");
            return;
        }
        final File storageDir = fsUtils.getAbsoluteHashedDataFolder(annotation.getUniqueImageId());
        if (storageDir == null) {
            LOG.error("Could not determine storage path for " + annotation);
            return;
        }
        final File xmlFile = new File(storageDir, "annotations.xml");
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new ImageCollectionException("Could not create storage directory " + storageDir);
        }
        XML xml = new XML("<annotation/>");
        xml.add("unique-image-id", annotation.getUniqueImageId());
        xml.add("name", annotation.getName());
        xml.add("creation-time", createDateFormat().format(annotation.getCreationTime()));
        xml.add("folder", annotation.getFolder());
        xml.add("resource", annotation.getResource());
        xml.add("local-folder", annotation.getLocalFolder());
        xml.add("local-resource", annotation.getLocalResource());
        xml.add("description", xmlClean(annotation.getDescription()));
        xml.add("checksum", String.valueOf(annotation.getChecksum().getValue()));
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
}
