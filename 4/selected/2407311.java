package org.photovault.imginfo;

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFTIFFTagSet;
import com.sun.org.apache.xerces.internal.impl.dtd.models.DFAContentModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.*;
import com.sun.media.jai.codec.*;
import java.awt.Transparency;
import java.awt.image.*;
import java.awt.geom.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.imaging.jpeg.*;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.odmg.*;
import org.odmg.*;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.folder.*;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ImageIOImage;
import org.photovault.image.PhotovaultImage;
import org.photovault.image.PhotovaultImageFactory;

/**
 PhotoInfo represents information about a single photograph
 TODO: write a decent doc!
 */
public class PhotoInfo {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PhotoInfo.class.getName());

    /** Max length of camera field */
    static final int CAMERA_LENGTH = 30;

    /** Max length of shooting place field */
    static final int SHOOTING_PLACE_LENGTH = 30;

    /** Max length of photographer field */
    static final int PHOTOGRAPHER_LENGTH = 30;

    /** Max length of lens field */
    static final int LENS_LENGTH = 30;

    /** Max length of film field */
    static final int FILM_LENGTH = 30;

    /** Max length of origFname field */
    static final int ORIG_FNAME_LENGTH = 30;

    public PhotoInfo() {
        changeListeners = new HashSet();
        instances = new Vector();
    }

    /**
     Static method to load photo info from database by photo id.
     @param photoId ID of the photo to be retrieved
     */
    public static PhotoInfo retrievePhotoInfo(int photoId) throws PhotoNotFoundException {
        log.debug("Fetching PhotoInfo with ID " + photoId);
        String oql = "select photos from " + PhotoInfo.class.getName() + " where uid=" + photoId;
        List photos = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            photos = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            log.warn("Error fetching record: " + e.getMessage());
            txw.abort();
            throw new PhotoNotFoundException();
        }
        if (photos.size() == 0) {
            throw new PhotoNotFoundException();
        }
        PhotoInfo photo = (PhotoInfo) photos.get(0);
        if (photo.getUid() != photoId) {
            log.warn("Found photo with ID = " + photo.getUid() + " while looking for ID " + photoId);
            throw new PhotoNotFoundException();
        }
        return photo;
    }

    /**
     Static method to load photo info from database by its globally unique id
     @param uuid UUID of the photo to be retrieved 
     @return PhotoInfo onject with the given uuid or <code>null<code> if 
     such object is not found.
     */
    public static PhotoInfo retrievePhotoInfo(UUID uuid) throws PhotoNotFoundException {
        log.debug("Fetching PhotoInfo with UUID " + uuid);
        String oql = "select photos from " + PhotoInfo.class.getName() + " where uuid=\"" + uuid.toString() + "\"";
        List photos = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            photos = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            log.warn("Error fetching record: " + e.getMessage());
            txw.abort();
            throw new PhotoNotFoundException();
        }
        PhotoInfo photo = null;
        if (photos.size() > 0) {
            if (photos.size() > 1) {
                log.warn("" + photos.size() + " photos with UUID " + uuid);
            }
            photo = (PhotoInfo) photos.get(0);
        }
        return photo;
    }

    /**
     Retrieves the PhotoInfo objects whose original instance has a specific hash code
     @param hash The hash code we are looking for
     @return An array of matching PhotoInfo objects or <code>null</code>
     if none found.
     */
    public static PhotoInfo[] retrieveByOrigHash(byte[] hash) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        Transaction tx = odmg.currentTransaction();
        PhotoInfo photos[] = null;
        try {
            PersistenceBroker broker = ((HasBroker) tx).getBroker();
            Criteria crit = new Criteria();
            crit.addEqualTo("origInstanceHash", hash);
            QueryByCriteria q = new QueryByCriteria(PhotoInfo.class, crit);
            Collection result = broker.getCollectionByQuery(q);
            if (result.size() > 0) {
                photos = (PhotoInfo[]) result.toArray(new PhotoInfo[result.size()]);
            }
            txw.commit();
        } catch (Exception e) {
            log.warn("Error executing query: " + e.getMessage());
            e.printStackTrace(System.out);
            txw.abort();
        }
        return photos;
    }

    /**
     Creates a new persistent PhotoInfo object and stores it in database
     (just a dummy one with no meaningful field values)
     @return A new PhotoInfo object
     */
    public static PhotoInfo create() {
        PhotoInfo photo = new PhotoInfo();
        photo.uuid = UUID.randomUUID();
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        db.makePersistent(photo);
        txw.lock(photo, Transaction.WRITE);
        txw.commit();
        return photo;
    }

    /**
     Creates a new PhotoInfo object with a given UUID
     @param uuid UUID for the new object
     @return A new PhotoInfo object
     */
    public static PhotoInfo create(UUID uuid) {
        PhotoInfo photo = new PhotoInfo();
        photo.uuid = uuid;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        db.makePersistent(photo);
        txw.lock(photo, Transaction.WRITE);
        txw.commit();
        return photo;
    }

    /**
     Add a new image to the database. Unless the image resides in an external
     volume this method first copies a given image file to the default database
     volume. It then extracts the information it can from the image file and
     stores a corresponding entry in DB.
     
     @param imgFile File object that describes the image file that is to be added
     to the database
     @return The PhotoInfo object describing the new file.
     @throws PhotoNotFoundException if the file given as imgFile argument does
     not exist or is unaccessible. This includes a case in which imgFile is part
     if normal Volume.
     */
    public static PhotoInfo addToDB(File imgFile) throws PhotoNotFoundException {
        VolumeBase vol = null;
        try {
            vol = VolumeBase.getVolumeOfFile(imgFile);
        } catch (IOException ex) {
            throw new PhotoNotFoundException();
        }
        File instanceFile = null;
        if (vol == null) {
            vol = VolumeBase.getDefaultVolume();
            instanceFile = vol.getFilingFname(imgFile);
            try {
                FileUtils.copyFile(imgFile, instanceFile);
            } catch (IOException ex) {
                log.warn("Error copying file: " + ex.getMessage());
                throw new PhotoNotFoundException();
            }
        } else if (vol instanceof ExternalVolume) {
            instanceFile = imgFile;
        } else if (vol instanceof Volume) {
            throw new PhotoNotFoundException();
        } else {
            throw new java.lang.Error("Unknown subclass of VolumeBase: " + vol.getClass().getName());
        }
        ODMGXAWrapper txw = new ODMGXAWrapper();
        PhotoInfo photo = PhotoInfo.create();
        txw.lock(photo, Transaction.WRITE);
        photo.addInstance(vol, instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL);
        photo.setCropBounds(new Rectangle2D.Float(0.0F, 0.0F, 1.0F, 1.0F));
        photo.updateFromOriginalFile();
        txw.commit();
        return photo;
    }

    UUID uuid = null;

    /**
     Get the globally unique ID for this photo;
     */
    public UUID getUUID() {
        if (uuid == null) {
            setUUID(UUID.randomUUID());
        }
        return uuid;
    }

    public void setUUID(UUID uuid) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.uuid = uuid;
        modified();
        txw.commit();
    }

    /**
     Reads field values from original file EXIF values
     @return true if successfull, false otherwise
     */
    public boolean updateFromOriginalFile() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance instance = null;
        for (int n = 0; n < instances.size(); n++) {
            ImageInstance candidate = (ImageInstance) instances.get(n);
            if (candidate.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL) {
                instance = candidate;
                File f = instance.getImageFile();
                boolean success = false;
                if (f != null) {
                    String suffix = "";
                    int suffixStart = f.getName().lastIndexOf(".");
                    if (suffixStart >= 0 && suffixStart < f.getName().length() - 1) {
                        suffix = f.getName().substring(suffixStart + 1);
                    }
                    Iterator readers = ImageIO.getImageReadersBySuffix(suffix);
                    if (readers.hasNext()) {
                        updateFromFileMetadata(f);
                        success = true;
                    } else {
                        success = updateFromRawFileMetadata(f);
                    }
                    txw.commit();
                    return success;
                }
            }
        }
        txw.commit();
        return false;
    }

    /**
     This method reads the metadata from image file and updates the PhotoInfo record from it
     @param f The file to read
     */
    void updateFromFileMetadata(File f) {
        ImageIOImage img = ImageIOImage.getImage(f, false, true);
        if (img == null) {
            return;
        }
        setShootTime(img.getTimestamp());
        setFStop(img.getAperture());
        setShutterSpeed(img.getShutterSpeed());
        setFocalLength(img.getFocalLength());
        setFilmSpeed(img.getFilmSpeed());
        String camera = img.getCamera();
        if (camera.length() > CAMERA_LENGTH) {
            camera = camera.substring(0, CAMERA_LENGTH);
        }
        setCamera(camera);
    }

    /**
     Reads metadata from raw camera file (using dcraw) and updates PhotoInfo
     fields based on that
     @param f The raw file to read
     @return <code>true</code> if meta data was succesfully read, <code>false</code> 
     otherwise (e.g. if f was not a raw image file.
     */
    private boolean updateFromRawFileMetadata(File f) {
        RawImage ri;
        try {
            ri = new RawImage(f);
        } catch (PhotovaultException ex) {
            return false;
        }
        if (!ri.isValidRawFile()) {
            return false;
        }
        setShootTime(ri.getTimestamp());
        setFStop(ri.getAperture());
        setShutterSpeed(ri.getShutterSpeed());
        setFilmSpeed(ri.getFilmSpeed());
        String camera = ri.getCamera();
        if (camera.length() > CAMERA_LENGTH) {
            camera = camera.substring(0, CAMERA_LENGTH);
        }
        setCamera(camera);
        setFilm("Digital");
        setFocalLength(ri.getFocalLength());
        ri.autoExpose();
        setRawSettings(ri.getRawSettings());
        return true;
    }

    /**
     Deletes the PhotoInfo and all related instances from database. 
     
     @deprecated This method does not do any error checking whether the instances
     are actually deleted. This is sometimes useful for e.g. cleaning up a test
     environment but production code should use 
     {@link #delete( boolean deleteExternalInstances )} instead.
     */
    public void delete() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        for (int i = 0; i < instances.size(); i++) {
            ImageInstance f = (ImageInstance) instances.get(i);
            f.delete();
        }
        if (folders != null) {
            Object[] foldersArray = folders.toArray();
            for (int n = 0; n < foldersArray.length; n++) {
                ((PhotoFolder) foldersArray[n]).removePhoto(this);
            }
        }
        db.deletePersistent(this);
        txw.commit();
    }

    /**
     Tries to delete a this photo, including all of its instances. If some
     instances cannot be deleted, other instances are deleted anyway but the actual
     PhotoInfo and its associations to folders are preserved.
     
     @param deleteExternalInstances Tries to delete also instances on external 
     volumes
     
     @throws PhotovaultException if some instances of the photo cannot be deleted
     */
    public void delete(boolean deleteExternalInstances) throws PhotovaultException {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        Vector deletedInstances = new Vector();
        Vector notDeletedInstances = new Vector();
        for (int i = 0; i < instances.size(); i++) {
            ImageInstance f = (ImageInstance) instances.get(i);
            if (f.delete(deleteExternalInstances)) {
                deletedInstances.add(f);
            } else {
                notDeletedInstances.add(f);
            }
        }
        for (int i = 0; i < deletedInstances.size(); i++) {
            instances.remove(deletedInstances.elementAt(i));
        }
        if (notDeletedInstances.size() > 0) {
            txw.commit();
            throw new PhotovaultException("Unable to delete some instances of the photo");
        }
        if (folders != null) {
            Object[] foldersArray = folders.toArray();
            for (int n = 0; n < foldersArray.length; n++) {
                ((PhotoFolder) foldersArray[n]).removePhoto(this);
            }
        }
        db.deletePersistent(this);
        txw.commit();
    }

    /**
     Adds a new listener to the list that will be notified of modifications to this object
     @param l reference to the listener
     */
    public void addChangeListener(PhotoInfoChangeListener l) {
        changeListeners.add(l);
    }

    /**
     Removes a listenre
     */
    public void removeChangeListener(PhotoInfoChangeListener l) {
        changeListeners.remove(l);
    }

    private void notifyListeners(PhotoInfoChangeEvent e) {
        Iterator iter = changeListeners.iterator();
        while (iter.hasNext()) {
            PhotoInfoChangeListener l = (PhotoInfoChangeListener) iter.next();
            l.photoInfoChanged(e);
        }
    }

    protected void modified() {
        lastModified = new java.util.Date();
        notifyListeners(new PhotoInfoChangeEvent(this));
    }

    /**
     set of the listeners that should be notified of any changes to this object
     */
    HashSet changeListeners = null;

    private int uid;

    /**
     * Describe timeAccuracy here.
     */
    private double timeAccuracy;

    /**
     * Describe quality here.
     */
    private int quality;

    /**
     * Describe lastModified here.
     */
    private java.util.Date lastModified;

    /**
     * Describe techNotes here.
     */
    private String techNotes;

    /**
     * Describe origFname here.
     */
    private String origFname;

    /**
     Returns the uid of the object
     */
    public int getUid() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return uid;
    }

    /**
     Adds a new image instance for this photo
     @param i The new instance
     */
    public void addInstance(ImageInstance i) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        txw.lock(i, Transaction.WRITE);
        instances.add(i);
        i.setPhotoUid(uid);
        if (i.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL) {
            origInstanceHash = i.getHash();
        }
        txw.commit();
    }

    /**
     Adds a new instance of the photo into the database.
     @param volume Volume in which the instance is stored
     @param instanceFile File name of the instance
     @param instanceType Type of the instance - original, modified or thumbnail.
     @return The new instance
     @see ImageInstance class documentation for details.
     */
    public ImageInstance addInstance(VolumeBase volume, File instanceFile, int instanceType) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        ImageInstance instance = ImageInstance.create(volume, instanceFile, this, instanceType);
        instances.add(instance);
        if (instances.size() == 1 || instanceType == ImageInstance.INSTANCE_TYPE_ORIGINAL) {
            invalidateThumbnail();
        }
        if (instanceType == ImageInstance.INSTANCE_TYPE_ORIGINAL) {
            origInstanceHash = instance.getHash();
            modified();
        }
        txw.commit();
        return instance;
    }

    public void removeInstance(int instanceNum) throws IndexOutOfBoundsException {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance instance = null;
        try {
            instance = (ImageInstance) getInstances().get(instanceNum);
        } catch (IndexOutOfBoundsException e) {
            txw.abort();
            throw e;
        }
        txw.lock(this, Transaction.WRITE);
        txw.lock(instance, Transaction.WRITE);
        instances.remove(instance);
        instance.delete();
        txw.commit();
    }

    /**
     Returns the number of instances of this photo that are stored in database
     */
    public int getNumInstances() {
        return instances.size();
    }

    /**
     Returns the arrayList that contains all instances of this file.
     */
    public Vector getInstances() {
        return instances;
    }

    Vector instances = null;

    /**
     Return a single image instance based on its order number
     @param instanceNum Number of the instance to return
     @throws IndexOutOfBoundsException if instanceNum is < 0 or >= the number of instances
     */
    public ImageInstance getInstance(int instanceNum) throws IndexOutOfBoundsException {
        ImageInstance instance = (ImageInstance) getInstances().get(instanceNum);
        return instance;
    }

    /**
     Find instance that is preferred for using in particular situatin. This function 
     seeks for an isntance that has at least a given resolution and has certain
     operations already applied.
     */
    public ImageInstance getPreferredInstance(EnumSet<ImageOperations> requiredOpers, EnumSet<ImageOperations> allowedOpers, int minWidth, int minHeight) {
        ImageInstance preferred = null;
        EnumSet<ImageOperations> appliedPreferred = null;
        Vector instances = getInstances();
        for (Object o : instances) {
            ImageInstance i = (ImageInstance) o;
            File f = i.getImageFile();
            if (f != null && f.exists() && i.getWidth() >= minWidth && i.getHeight() >= minHeight) {
                EnumSet<ImageOperations> applied = i.getAppliedOperations();
                if (applied.containsAll(requiredOpers) && allowedOpers.containsAll(applied) && isConsistentWithCurrentSettings(i)) {
                    if (preferred == null) {
                        preferred = i;
                        appliedPreferred = applied;
                    } else if (!appliedPreferred.containsAll(applied)) {
                        preferred = i;
                        appliedPreferred = applied;
                    }
                }
            }
        }
        return preferred;
    }

    /**
     Returns a thumbnail of this image. If no thumbnail instance is yetavailable, creates a
     new instance on the default volume. Otherwise loads an existing thumbnail instance. <p>
     
     If thumbnail creation fails of if there is no image instances available at all, returns
     a default thumbnail image.
     @return Thumbnail of this photo or default thumbnail if no photo instances available
     */
    public Thumbnail getThumbnail() {
        log.debug("getThumbnail: entry, Finding thumbnail for " + uid);
        if (thumbnail == null) {
            thumbnail = getExistingThumbnail();
            if (thumbnail == null) {
                log.debug("No thumbnail found, creating");
                createThumbnail();
            }
        }
        if (thumbnail == null) {
            thumbnail = Thumbnail.getErrorThumbnail();
            oldThumbnail = null;
        }
        log.debug("getThumbnail: exit");
        return thumbnail;
    }

    /**
     Returns an existing thumbnail for this photo but do not try to contruct a new
     one if there is no thumbnail already created.
     @return Thumbnail for this photo or null if none is found.
     */
    public Thumbnail getExistingThumbnail() {
        if (thumbnail == null) {
            log.debug("Finding thumbnail from database");
            ImageInstance original = null;
            for (int n = 0; n < instances.size(); n++) {
                ImageInstance instance = (ImageInstance) instances.get(n);
                if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL && matchesCurrentSettings(instance)) {
                    File f = instance.getImageFile();
                    if (f != null) {
                        log.debug("Found thumbnail from database");
                        thumbnail = Thumbnail.createThumbnail(this, instance.getImageFile());
                        oldThumbnail = null;
                        break;
                    }
                }
            }
        }
        return thumbnail;
    }

    /**
     Returns true if the photo has a Thumbnail already created,
     false otherwise
     */
    public boolean hasThumbnail() {
        log.debug("hasThumbnail: entry, Finding thumbnail for " + uid);
        if (thumbnail == null) {
            thumbnail = getExistingThumbnail();
        }
        log.debug("hasThumbnail: exit");
        return (thumbnail != null && thumbnail != Thumbnail.getDefaultThumbnail());
    }

    Thumbnail thumbnail = null;

    /**
     Reference to an outdated thumbnail image while a new thumbnail in being created
     */
    Thumbnail oldThumbnail = null;

    public Thumbnail getOldThumbnail() {
        return oldThumbnail;
    }

    /**
     Invalidates the current thumbnail:
     <ul>
     <li>Set thumbnail to null</li>
     <li>Set oldThumbnail to the previous thumbnail</li>
     </ul>
     */
    private void invalidateThumbnail() {
        if (thumbnail != null) {
            oldThumbnail = thumbnail;
            thumbnail = null;
        }
    }

    /**
     Delete all thumbnail/copy instance that do not match to the image settings.
     */
    private void purgeInvalidInstances() {
        log.debug("entry: purgeInvalidInstances");
        Vector purgeList = new Vector();
        for (int n = 0; n < instances.size(); n++) {
            ImageInstance instance = (ImageInstance) instances.get(n);
            if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL && !matchesCurrentSettings(instance)) {
                purgeList.add(instance);
            } else if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_MODIFIED && !isConsistentWithCurrentSettings(instance)) {
                purgeList.add(instance);
            }
        }
        log.debug("Deleting " + purgeList.size() + " instances");
        Iterator iter = purgeList.iterator();
        while (iter.hasNext()) {
            ImageInstance i = (ImageInstance) iter.next();
            ODMGXAWrapper txw = new ODMGXAWrapper();
            txw.lock(this, Transaction.WRITE);
            txw.lock(i, Transaction.WRITE);
            instances.remove(i);
            i.delete();
            txw.commit();
        }
        log.debug("exit: purgeInvalidInstances");
    }

    /**
     Helper function to calculate aspect ratio of an image
     @param width width of the image
     @param height height of the image
     @param pixelAspect Aspect ratio of a single pixel (width/height)
     @return aspect ratio (width/height)
     */
    private double getAspect(int width, int height, double pixelAspect) {
        return height > 0 ? pixelAspect * (((double) width) / ((double) height)) : -1.0;
    }

    /**
     Helper method to check if a image is ok for thumbnail creation, i.e. that
     it is large enough and that its aspect ration is same as the original has
     @param width width of the image to test
     @param height Height of the image to test
     @param minWidth Minimun width needed for creating a thumbnail
     @param minHeight Minimum height needed for creating a thumbnail
     @param origAspect Aspect ratio of the original image
     */
    private boolean isOkForThumbCreation(int width, int height, int minWidth, int minHeight, double origAspect, double aspectAccuracy) {
        if (width < minWidth) return false;
        if (height < minHeight) return false;
        double aspect = getAspect(width, height, 1.0);
        if (Math.abs(aspect - origAspect) / origAspect > aspectAccuracy) {
            return false;
        }
        return true;
    }

    private boolean matchesCurrentSettings(ImageInstance instance) {
        return Math.abs(instance.getRotated() - prefRotation) < 0.0001 && instance.getCropBounds().equals(getCropBounds()) && (channelMap == null || channelMap.equals(instance.getColorChannelMapping())) && (rawSettings == null || rawSettings.equals(instance.getRawSettings()));
    }

    private boolean isConsistentWithCurrentSettings(ImageInstance instance) {
        EnumSet<ImageOperations> applied = instance.getAppliedOperations();
        if (applied.contains(ImageOperations.CROP) && !(Math.abs(instance.getRotated() - prefRotation) < 0.0001 && instance.getCropBounds().equals(getCropBounds()))) {
            return false;
        }
        if (applied.contains(ImageOperations.COLOR_MAP) && !(channelMap == null || channelMap.equals(instance.getColorChannelMapping()))) {
            return false;
        }
        if (applied.contains(ImageOperations.RAW_CONVERSION) && !(rawSettings == null || rawSettings.equals(instance.getRawSettings()))) {
            return false;
        }
        return true;
    }

    /**
     Creates thumbnail & preview instances in given volume. The preview instance 
     is created only if one does not exist currently or it is out of date.
     TODO: Thiuis chould be refactored into a more generic and configurable 
     framework for creating needed instances.
     */
    protected void createThumbnail(VolumeBase volume) {
        boolean recreatePreview = true;
        EnumSet<ImageOperations> previewOps = EnumSet.of(ImageOperations.COLOR_MAP, ImageOperations.RAW_CONVERSION);
        ImageInstance previewInstance = this.getPreferredInstance(EnumSet.noneOf(ImageOperations.class), previewOps, 1024, 1024);
        if (previewInstance != null && previewInstance.getInstanceType() == ImageInstance.INSTANCE_TYPE_MODIFIED) {
            recreatePreview = false;
        }
        createThumbnail(volume, recreatePreview);
    }

    /** Creates new thumbnail and preview instances for this image on specific volume
     @param volume The volume in which the instance is to be created
     @param createpreview, if <code>true</code> create also a preview image.
     */
    protected void createThumbnail(VolumeBase volume, boolean createPreview) {
        log.debug("Creating thumbnail for " + uid);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        int maxThumbWidth = 100;
        int maxThumbHeight = 100;
        checkCropBounds();
        double cropWidth = cropMaxX - cropMinX;
        cropWidth = (cropWidth > 0.000001) ? cropWidth : 1.0;
        double cropHeight = cropMaxY - cropMinY;
        cropHeight = (cropHeight > 0.000001) ? cropHeight : 1.0;
        int minInstanceWidth = (int) (((double) maxThumbWidth) / cropWidth);
        int minInstanceHeight = (int) (((double) maxThumbHeight) / cropHeight);
        int minInstanceSide = Math.max(minInstanceWidth, minInstanceHeight);
        EnumSet<ImageOperations> allowedOps = EnumSet.allOf(ImageOperations.class);
        if (createPreview) {
            allowedOps = EnumSet.noneOf(ImageOperations.class);
            minInstanceWidth = 1024;
            minInstanceHeight = 1024;
        }
        ImageInstance original = this.getPreferredInstance(EnumSet.noneOf(ImageOperations.class), allowedOps, minInstanceWidth, minInstanceHeight);
        if (original == null) {
            log.warn("Error - no original image was found!!!");
            txw.commit();
            return;
        }
        txw.lock(original, Transaction.READ);
        log.debug("Found original, reading it...");
        double origAspect = this.getAspect(original.getWidth(), original.getHeight(), 1.0);
        double aspectAccuracy = 0.01;
        RenderedImage origImage = null;
        RenderedImage thumbImage = null;
        RenderedImage previewImage = null;
        try {
            File imageFile = original.getImageFile();
            PhotovaultImageFactory imgFactory = new PhotovaultImageFactory();
            PhotovaultImage img = imgFactory.create(imageFile, false, false);
            if (channelMap != null) {
                img.setColorAdjustment(channelMap);
            }
            if (img instanceof RawImage) {
                RawImage ri = (RawImage) img;
                ri.setRawSettings(rawSettings);
            }
            if (createPreview) {
                int previewWidth = img.getWidth();
                int previewHeight = img.getHeight();
                while (previewWidth > 2048 || previewHeight > 2048) {
                    previewWidth >>= 1;
                    previewHeight >>= 1;
                }
                previewImage = img.getRenderedImage(previewWidth, previewHeight, false);
            }
            img.setCropBounds(this.getCropBounds());
            img.setRotation(prefRotation - original.getRotated());
            thumbImage = img.getRenderedImage(maxThumbWidth, maxThumbHeight, true);
        } catch (Exception e) {
            log.warn("Error reading image: " + e.getMessage());
            txw.commit();
            return;
        }
        log.debug("Done, finding name");
        File thumbnailFile = volume.getInstanceName(this, "jpg");
        log.debug("name = " + thumbnailFile.getName());
        try {
            saveInstance(thumbnailFile, thumbImage);
            if (thumbImage instanceof PlanarImage) {
                ((PlanarImage) thumbImage).dispose();
                System.gc();
            }
        } catch (PhotovaultException ex) {
            log.error("error writing thumbnail for " + original.getImageFile().getAbsolutePath() + ": " + ex.getMessage());
            txw.commit();
            return;
        }
        ImageInstance thumbInstance = addInstance(volume, thumbnailFile, ImageInstance.INSTANCE_TYPE_THUMBNAIL);
        thumbInstance.setRotated(prefRotation - original.getRotated());
        thumbInstance.setCropBounds(getCropBounds());
        thumbInstance.setColorChannelMapping(channelMap);
        thumbInstance.setRawSettings(rawSettings);
        log.debug("Loading thumbnail...");
        thumbnail = Thumbnail.createThumbnail(this, thumbnailFile);
        oldThumbnail = null;
        log.debug("Thumbnail loaded");
        if (createPreview) {
            File previewFile = volume.getInstanceName(this, "jpg");
            try {
                saveInstance(previewFile, previewImage);
                if (previewImage instanceof PlanarImage) {
                    ((PlanarImage) previewImage).dispose();
                    System.gc();
                }
            } catch (PhotovaultException ex) {
                log.error("error writing preview for " + original.getImageFile().getAbsolutePath() + ": " + ex.getMessage());
                txw.commit();
                return;
            }
            ImageInstance previewInstance = addInstance(volume, previewFile, ImageInstance.INSTANCE_TYPE_MODIFIED);
            previewInstance.setColorChannelMapping(channelMap);
            previewInstance.setRawSettings(rawSettings);
        }
        txw.commit();
    }

    /**
     Helper function to save a rendered image to file
     @param instanceFile The file into which the image will be saved
     @param img Image that willb e saved
     @throws PhotovaultException if saving does not succeed
     */
    protected void saveInstance(File instanceFile, RenderedImage img) throws PhotovaultException {
        OutputStream out = null;
        log.debug("Entry: saveInstance, file = " + instanceFile.getAbsolutePath());
        try {
            out = new FileOutputStream(instanceFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing thumbnail: " + e.getMessage());
            throw new PhotovaultException(e.getMessage());
        }
        if (img.getSampleModel().getSampleSize(0) == 16) {
            log.debug("16 bit image, converting to 8 bits");
            double[] subtract = new double[1];
            subtract[0] = 0;
            double[] divide = new double[1];
            divide[0] = 1. / 256.;
            ParameterBlock pbRescale = new ParameterBlock();
            pbRescale.add(divide);
            pbRescale.add(subtract);
            pbRescale.addSource(img);
            PlanarImage outputImage = (PlanarImage) JAI.create("rescale", pbRescale, null);
            ParameterBlock pbConvert = new ParameterBlock();
            pbConvert.addSource(outputImage);
            pbConvert.add(DataBuffer.TYPE_BYTE);
            img = JAI.create("format", pbConvert);
        }
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, encodeParam);
        try {
            log.debug("starting JPEG enconde");
            encoder.encode(img);
            log.debug("done JPEG encode");
            out.close();
        } catch (Exception e) {
            log.warn("Exception while encoding" + e.getMessage());
            throw new PhotovaultException("Error writing instance " + instanceFile.getAbsolutePath() + ": " + e.getMessage());
        }
        log.debug("Exit: saveInstance");
    }

    /**
     Attemps to read a thumbnail from EXIF headers
     @return The thumbnail image or null if none available
     */
    private BufferedImage readExifThumbnail(File f) {
        BufferedImage bi = null;
        Metadata metadata = null;
        try {
            metadata = JpegMetadataReader.readMetadata(f);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        ExifDirectory exif = null;
        if (metadata != null && metadata.containsDirectory(ExifDirectory.class)) {
            try {
                exif = (ExifDirectory) metadata.getDirectory(ExifDirectory.class);
                byte[] thumbData = exif.getThumbnailData();
                if (thumbData != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(thumbData);
                    try {
                        bi = ImageIO.read(bis);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException ex) {
                            log.error("Cannot close image instance after creating thumbnail.");
                        }
                    }
                }
            } catch (MetadataException e) {
            }
        }
        return bi;
    }

    /** Creates a new thumbnail on the default volume
     */
    protected void createThumbnail() {
        VolumeBase vol = VolumeBase.getDefaultVolume();
        createThumbnail(vol);
    }

    /**
     Exports an image from database to a specified file with given resolution.
     The image aspect ratio is preserved and the image is scaled so that it fits
     to the given maximum resolution.
     @param file File in which the image will be saved
     @param width Width of the exported image in pixels. If negative the image is
     exported in its "natural" resolution (i.e. not scaled)
     @param height Height of the exported image in pixels
     @throws PhotovaultException if exporting the photo fails for some reason.
     */
    public void exportPhoto(File file, int width, int height) throws PhotovaultException {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        ImageInstance original = null;
        for (int n = 0; n < instances.size(); n++) {
            ImageInstance instance = (ImageInstance) instances.get(n);
            if (instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL && instance.getImageFile() != null && instance.getImageFile().exists()) {
                original = instance;
                txw.lock(original, Transaction.READ);
                break;
            }
        }
        if (original == null) {
            log.warn("Error - no original image was found!!!");
            txw.commit();
            throw new PhotovaultException("No image file found to export photo");
        }
        RenderedImage exportImage = null;
        try {
            File imageFile = original.getImageFile();
            String fname = imageFile.getName();
            int lastDotPos = fname.lastIndexOf(".");
            if (lastDotPos <= 0 || lastDotPos >= fname.length() - 1) {
                throw new IOException("Cannot determine file type extension of " + imageFile.getAbsolutePath());
            }
            PhotovaultImageFactory imageFactory = new PhotovaultImageFactory();
            PhotovaultImage img = null;
            try {
                img = imageFactory.create(imageFile, false, false);
            } catch (PhotovaultException ex) {
                log.error(ex.getMessage());
            }
            img.setCropBounds(this.getCropBounds());
            img.setRotation(prefRotation - original.getRotated());
            if (channelMap != null) {
                img.setColorAdjustment(channelMap);
            }
            if (img instanceof RawImage) {
                RawImage ri = (RawImage) img;
                if (rawSettings != null) {
                    ri.setRawSettings(rawSettings);
                } else if (rawSettings == null) {
                    rawSettings = ri.getRawSettings();
                    txw.lock(rawSettings, Transaction.WRITE);
                }
            }
            if (width > 0) {
                exportImage = img.getRenderedImage(width, height, false);
            } else {
                exportImage = img.getRenderedImage(1.0, false);
            }
        } catch (Exception e) {
            log.warn("Error reading image: " + e.getMessage());
            txw.abort();
            throw new PhotovaultException("Error reading image: " + e.getMessage(), e);
        }
        if (exportImage.getSampleModel().getSampleSize(0) == 16) {
            double[] subtract = new double[1];
            subtract[0] = 0;
            double[] divide = new double[1];
            divide[0] = 1. / 256.;
            ParameterBlock pbRescale = new ParameterBlock();
            pbRescale.add(divide);
            pbRescale.add(subtract);
            pbRescale.addSource(exportImage);
            PlanarImage outputImage = (PlanarImage) JAI.create("rescale", pbRescale, null);
            ParameterBlock pbConvert = new ParameterBlock();
            pbConvert.addSource(outputImage);
            pbConvert.add(DataBuffer.TYPE_BYTE);
            exportImage = JAI.create("format", pbConvert);
        }
        String ftype = "jpg";
        String imageFname = file.getName();
        int extIndex = imageFname.lastIndexOf(".") + 1;
        if (extIndex > 0) {
            ftype = imageFname.substring(extIndex);
        }
        try {
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName(ftype);
            if (iter.hasNext()) writer = (ImageWriter) iter.next();
            if (writer != null) {
                ImageOutputStream ios = null;
                try {
                    ios = ImageIO.createImageOutputStream(file);
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    writer.write(null, new IIOImage(exportImage, null, null), param);
                    ios.flush();
                } finally {
                    if (ios != null) ios.close();
                    writer.dispose();
                    if (exportImage != null & exportImage instanceof PlanarImage) {
                        ((PlanarImage) exportImage).dispose();
                        System.gc();
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error writing exported image: " + e.getMessage());
            txw.abort();
            throw new PhotovaultException("Error writing exported image: " + e.getMessage(), e);
        }
        txw.commit();
    }

    /**
     MD5 hash code of the original instance of this PhotoInfo. It must is stored also
     as part of PhotoInfo object since the original instance might be deleted from the
     database (or we might synchronize just metadata without originals into other database!).
     With the hash code we are still able to detect that an image file is actually the
     original.
     */
    byte origInstanceHash[] = null;

    public byte[] getOrigInstanceHash() {
        return (origInstanceHash != null) ? ((byte[]) origInstanceHash.clone()) : null;
    }

    /**
     Sets the original instance hash. This is intended for only internal use
     @param hash MD5 hash value for original instance
     */
    protected void setOrigInstanceHash(byte[] hash) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        origInstanceHash = (byte[]) hash.clone();
        txw.commit();
    }

    java.util.Date shootTime;

    /**
     * Get the value of shootTime. Note that shoot time can also be
     null (to mean that the time is unspecified)1
     @return value of shootTime.
     */
    public java.util.Date getShootTime() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return shootTime != null ? (java.util.Date) shootTime.clone() : null;
    }

    /**
     * Set the value of shootTime.
     * @param v  Value to assign to shootTime.
     */
    public void setShootTime(java.util.Date v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.shootTime = (v != null) ? (java.util.Date) v.clone() : null;
        modified();
        txw.commit();
    }

    /**
     Set both shooting time & accuracy directly using a FuzzyTime object
     @param v FuzzyTime containing new values.
     */
    public void setFuzzyShootTime(FuzzyDate v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.shootTime = (java.util.Date) v.getDate().clone();
        this.timeAccuracy = v.getAccuracy();
        modified();
        txw.commit();
    }

    /**
     
     @return The timeAccuracty value
     */
    public final double getTimeAccuracy() {
        return timeAccuracy;
    }

    /**
     
     Set the shooting time accuracy. The value is a +/- range from shootingTime
     parameter (i.e. shootingTime April 15 2000, timeAccuracy 15 means that the
     photo is taken in April 2000.
     
     * @param newTimeAccuracy The new TimeAccuracy value.
     */
    public final void setTimeAccuracy(final double newTimeAccuracy) {
        this.timeAccuracy = newTimeAccuracy;
    }

    String desc;

    /**
     * Get the value of desc.
     * @return value of desc.
     */
    public String getDesc() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return desc;
    }

    /**
     * Set the value of desc.
     * @param v  Value to assign to desc.
     */
    public void setDesc(String v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.desc = v;
        modified();
        txw.commit();
    }

    double FStop;

    /**
     * Get the value of FStop.
     * @return value of FStop.
     */
    public double getFStop() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return FStop;
    }

    /**
     * Set the value of FStop.
     * @param v  Value to assign to FStop.
     */
    public void setFStop(double v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.FStop = v;
        modified();
        txw.commit();
    }

    double focalLength;

    /**
     * Get the value of focalLength.
     * @return value of focalLength.
     */
    public double getFocalLength() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return focalLength;
    }

    /**
     * Set the value of focalLength.
     * @param v  Value to assign to focalLength.
     */
    public void setFocalLength(double v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.focalLength = v;
        modified();
        txw.commit();
    }

    String shootingPlace;

    /**
     * Get the value of shootingPlace.
     * @return value of shootingPlace.
     */
    public String getShootingPlace() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return shootingPlace;
    }

    /**
     * Set the value of shootingPlace.
     * @param v  Value to assign to shootingPlace.
     */
    public void setShootingPlace(String v) {
        checkStringProperty("Shooting place", v, SHOOTING_PLACE_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.shootingPlace = v;
        modified();
        txw.commit();
    }

    String photographer;

    /**
     * Get the value of photographer.
     * @return value of photographer.
     */
    public String getPhotographer() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return photographer;
    }

    /**
     * Set the value of photographer.
     * @param v  Value to assign to photographer.
     */
    public void setPhotographer(String v) {
        checkStringProperty("Photographer", v, this.PHOTOGRAPHER_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.photographer = v;
        modified();
        txw.commit();
    }

    double shutterSpeed;

    /**
     * Get the value of shutterSpeed.
     * @return value of shutterSpeed.
     */
    public double getShutterSpeed() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return shutterSpeed;
    }

    /**
     * Set the value of shutterSpeed.
     * @param v  Value to assign to shutterSpeed.
     */
    public void setShutterSpeed(double v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.shutterSpeed = v;
        modified();
        txw.commit();
    }

    String camera;

    /**
     * Get the value of camera.
     * @return value of camera.
     */
    public String getCamera() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return camera;
    }

    /**
     * Set the value of camera.
     * @param v  Value to assign to camera.
     */
    public void setCamera(String v) {
        checkStringProperty("Camera", v, CAMERA_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.camera = v;
        modified();
        txw.commit();
    }

    String lens;

    /**
     * Get the value of lens.
     * @return value of lens.
     */
    public String getLens() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return lens;
    }

    /**
     * Set the value of lens.
     * @param v  Value to assign to lens.
     */
    public void setLens(String v) {
        checkStringProperty("Lens", v, LENS_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.lens = v;
        modified();
        txw.commit();
    }

    String film;

    /**
     * Get the value of film.
     * @return value of film.
     */
    public String getFilm() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return film;
    }

    /**
     * Set the value of film.
     * @param v  Value to assign to film.
     */
    public void setFilm(String v) {
        checkStringProperty("Film", v, FILM_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.film = v;
        modified();
        txw.commit();
    }

    int filmSpeed;

    /**
     * Get the value of filmSpeed.
     * @return value of filmSpeed.
     */
    public int getFilmSpeed() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return filmSpeed;
    }

    /**
     * Set the value of filmSpeed.
     * @param v  Value to assign to filmSpeed.
     */
    public void setFilmSpeed(int v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.filmSpeed = v;
        modified();
        txw.commit();
    }

    double prefRotation;

    /**
     Get the preferred rotation for this image in degrees. Positive values 
     indicate that the image should be rotated clockwise.
     @return value of prefRotation.
     */
    public double getPrefRotation() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return prefRotation;
    }

    /**
     Set the value of prefRotation.
     @param v  New preferred rotation in degrees. The value should be in range 
     0.0 <= v < 360, otherwise v is normalized to be between these values.
     */
    public void setPrefRotation(double v) {
        while (v < 0.0) {
            v += 360.0;
        }
        while (v >= 360.0) {
            v -= 360.0;
        }
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        if (v != prefRotation) {
            invalidateThumbnail();
            purgeInvalidInstances();
        }
        this.prefRotation = v;
        modified();
        txw.commit();
    }

    /**
     Check that the e crop bounds are defined in consistent manner. This is needed
     since in old installations the max parameters can be larger than min ones.
     */
    private void checkCropBounds() {
        cropMinX = Math.min(1.0, Math.max(0.0, cropMinX));
        cropMinY = Math.min(1.0, Math.max(0.0, cropMinY));
        cropMaxX = Math.min(1.0, Math.max(0.0, cropMaxX));
        cropMaxY = Math.min(1.0, Math.max(0.0, cropMaxY));
        if (cropMaxX - cropMinX <= 0.0) {
            cropMaxX = 1.0 - cropMinX;
        }
        if (cropMaxY - cropMinY <= 0.0) {
            cropMaxY = 1.0 - cropMinY;
        }
    }

    /**
     Get the preferred crop bounds of the original image
     */
    public Rectangle2D getCropBounds() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        checkCropBounds();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return new Rectangle2D.Double(cropMinX, cropMinY, cropMaxX - cropMinX, cropMaxY - cropMinY);
    }

    /**
     Set the preferred cropping operation
     @param cropBounds New crop bounds
     */
    public void setCropBounds(Rectangle2D cropBounds) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        if (!cropBounds.equals(getCropBounds())) {
            invalidateThumbnail();
            purgeInvalidInstances();
        }
        cropMinX = cropBounds.getMinX();
        cropMinY = cropBounds.getMinY();
        cropMaxX = cropBounds.getMaxX();
        cropMaxY = cropBounds.getMaxY();
        checkCropBounds();
        modified();
        txw.commit();
    }

    /**
     CropBounds describes the desired crop rectange from original image. It is
     defined as proportional coordinates that are applied after rotating the
     original image so that top left corner is (0.0, 0.0) and bottom right
     (1.0, 1.0)
     */
    double cropMinX;

    double cropMaxX;

    double cropMinY;

    double cropMaxY;

    /**
     Mapping of the original color channels to preferred ones.
     */
    ChannelMapOperation channelMap = null;

    /**
     Set the preferred color channel mapping
     @param cm the new color channel mapping
     */
    public void setColorChannelMapping(ChannelMapOperation cm) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        if (cm != null) {
            if (!cm.equals(channelMap)) {
                invalidateThumbnail();
                purgeInvalidInstances();
            }
        }
        channelMap = cm;
        modified();
        txw.commit();
    }

    /**
     Get currently preferred color channe?l mapping.
     @return The current color channel mapping
     */
    public ChannelMapOperation getColorChannelMapping() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return channelMap;
    }

    /**
     Raw conversion settings or <code>null</code> if no raw image is available
     */
    RawConversionSettings rawSettings = null;

    /**
     OJB database identified for the raw settings
     */
    int rawSettingsId;

    /**
     Set the raw conversion settings for this photo
     @param s The new raw conversion settings to use. The method makes a clone of 
     the object.     
     */
    public void setRawSettings(RawConversionSettings s) {
        log.debug("entry: setRawSettings()");
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        RawConversionSettings settings = null;
        if (s != null) {
            if (!s.equals(rawSettings)) {
                invalidateThumbnail();
                purgeInvalidInstances();
            }
            settings = s.clone();
            Database db = ODMG.getODMGDatabase();
            db.makePersistent(settings);
            RawConversionSettings oldSettings = rawSettings;
            txw.lock(settings, Transaction.WRITE);
            if (oldSettings != null) {
                txw.lock(oldSettings, Transaction.WRITE);
                db.deletePersistent(oldSettings);
            }
        } else {
            if (rawSettings != null) {
                log.error("Setting raw conversion settings of an raw image to null!!!");
                invalidateThumbnail();
                purgeInvalidInstances();
            }
        }
        rawSettings = settings;
        modified();
        txw.commit();
        log.debug("exit: setRawSettings()");
    }

    /**
     Get the current raw conversion settings.
     @return Current settings or <code>null</code> if this is not a raw image.     
     */
    public RawConversionSettings getRawSettings() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return rawSettings;
    }

    public int getRawSettingsId() {
        return rawSettingsId;
    }

    String description;

    /**
     * Get the value of description.
     * @return value of description.
     */
    public String getDescription() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return description;
    }

    /**
     * Set the value of description.
     * @param v  Value to assign to description.
     */
    public void setDescription(String v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.description = v;
        modified();
        txw.commit();
    }

    public static final int QUALITY_UNDEFINED = 0;

    public static final int QUALITY_TOP = 1;

    public static final int QUALITY_GOOD = 2;

    public static final int QUALITY_FAIR = 3;

    public static final int QUALITY_POOR = 4;

    public static final int QUALITY_UNUSABLE = 5;

    /**
     * Get the value of value attribute.
     *
     * @return an <code>int</code> value
     */
    public final int getQuality() {
        return quality;
    }

    /**
     * Set the "value attribute for the photo which tries to describe
     How good the pohot is. Possible values:
     <ul>
     <li>QUALITY_UNDEFINED - value of the photo has not been evaluated</li>
     <li>QUALITY_TOP - This frame is a top quality photo</li>
     <li>QUALITY_GOOD - This frame is good, one of the best available from the session</li>
     <li>QUALITY_FAIR - This frame is OK but probably not the 1st choice for use</li>
     <li>QUALITY_POOR - Unsuccesful picture</li>
     <li>QUALITY_UNUSABLE - Technical failure</li>
     </ul>
     
     *
     * @param newQuality The new Quality value.
     */
    public final void setQuality(final int newQuality) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.quality = newQuality;
        modified();
        txw.commit();
    }

    /**
     Returns the time when this photo (=metadata of it) was last modified
     * @return a <code>Date</code> value
     */
    public final java.util.Date getLastModified() {
        return lastModified != null ? (java.util.Date) lastModified.clone() : null;
    }

    public void setLastModified(final java.util.Date newDate) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.lastModified = (newDate != null) ? (java.util.Date) newDate.clone() : null;
        modified();
        txw.commit();
    }

    /**
     * Get the <code>TechNotes</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getTechNotes() {
        return techNotes;
    }

    /**
     * Set the <code>TechNotes</code> value.
     *
     * @param newTechNotes The new TechNotes value.
     */
    public final void setTechNotes(final String newTechNotes) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.techNotes = newTechNotes;
        modified();
        txw.commit();
    }

    /**
     Get the original file name of this photo
     
     * @return a <code>String</code> value
     */
    public final String getOrigFname() {
        return origFname;
    }

    /**
     Set the original file name of this photo. This is set also by addToDB which is the
     preferred way of creating a new photo into the DB.
     @param newFname The original file name
     @throws IllegalArgumentException if the given file name is longer than
     {@link #ORIG_FNAME_LENGTH}
     */
    public final void setOrigFname(final String newFname) {
        checkStringProperty("OrigFname", newFname, ORIG_FNAME_LENGTH);
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.origFname = newFname;
        modified();
        txw.commit();
    }

    /**
     List of folders this photo belongs to
     */
    Collection folders = null;

    /**
     This is called by PhotoFolder when the photo is added to a folder
     */
    public void addedToFolder(PhotoFolder folder) {
        if (folders == null) {
            folders = new Vector();
        }
        folders.add(folder);
    }

    /**
     This is called by PhotoFolder when the photo is removed from a folder
     */
    public void removedFromFolder(PhotoFolder folder) {
        if (folders == null) {
            folders = new Vector();
        }
        folders.remove(folder);
    }

    /**
     Returns a collection that contains all folders the photo belongs to
     */
    public Collection getFolders() {
        Vector foldersCopy = new Vector();
        if (folders != null) {
            foldersCopy = new Vector(folders);
        }
        return foldersCopy;
    }

    private static boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return true;
            } else {
                return false;
            }
        }
        return o1.equals(o2);
    }

    /**
     Checks that a string is no longer tha tmaximum length allowed for it
     @param propertyName The porperty name used in error message
     @param value the new value
     @param maxLength Maximum length for the string
     @throws IllegalArgumentException if value is longer than maxLength
     */
    void checkStringProperty(String propertyName, String value, int maxLength) throws IllegalArgumentException {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(propertyName + " cannot be longer than " + maxLength + " characters");
        }
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        PhotoInfo p = (PhotoInfo) obj;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.lock(p, Transaction.READ);
        txw.commit();
        return (isEqual(p.photographer, this.photographer) && isEqual(p.shootingPlace, this.shootingPlace) && isEqual(p.shootTime, this.shootTime) && isEqual(p.description, this.description) && isEqual(p.camera, this.camera) && isEqual(p.lens, this.lens) && isEqual(p.film, this.film) && isEqual(p.techNotes, this.techNotes) && isEqual(p.origFname, this.origFname) && isEqual(p.channelMap, this.channelMap) && p.shutterSpeed == this.shutterSpeed && p.filmSpeed == this.filmSpeed && p.focalLength == this.focalLength && p.FStop == this.FStop && p.uid == this.uid && p.quality == this.quality && ((p.rawSettings == null && this.rawSettings == null) || (p.rawSettings != null && p.rawSettings.equals(this.rawSettings))));
    }

    public int hashCode() {
        return uid;
    }
}
