package org.pixory.pxmodel;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pixory.pxfoundation.PXDateUtility;
import org.pixory.pximage.PXImage;
import org.pixory.pximage.PXImageException;
import org.pixory.pximage.PXImageManager;
import org.pixory.pximage.PXImageManagerException;
import org.pixory.pximage.PXImageRotator;
import org.pixory.pximage.PXThumbnailParam;
import org.pixory.pxmodel.types.ImageRotation;

public class PXAlbumFace extends PXPersistentObject implements PXAlbum, PXAuditable, Serializable {

    private static final Log LOG = LogFactory.getLog(PXAlbumFace.class);

    private static final Boolean DEFAULT_IS_PUBLIC = Boolean.FALSE;

    private static final PXShareMethod DEFAULT_SHARE_METHOD = PXShareMethod.URL;

    private String _id;

    private String _name;

    private String _path;

    private int _photoCount;

    private Date _startDate;

    private Date _endDate;

    private Boolean _isPublic = DEFAULT_IS_PUBLIC;

    private String _coverPhotoName;

    private String _coverPhotoRotationString;

    private String _coverText;

    private Date _creationDate;

    private Date _lastUpdateDate;

    private Set _invitations;

    private PXShareMethod _shareMethod;

    private Set _shares;

    private File _albumPath;

    private PXImageManager _imageManager;

    public PXAlbumFace() {
        this(null);
    }

    public PXAlbumFace(String id) {
        super(id);
    }

    /**
	 * ensures that the album argument has an up-to-date face in the datastore;
	 * This method *FORCES* a store update, even if no data changes are implied
	 * by the 'album' argument.
	 * 
	 * @return the face that corresponds to album arg
	 */
    public static PXAlbumFace updateFaceForAlbum(PXAlbumContent album) {
        PXAlbumFace updateFace = null;
        if (album != null) {
            String anAlbumId = album.getId();
            if (anAlbumId != null) {
                Transaction aTransaction = null;
                try {
                    Session aSession = PXObjectStore.getInstance().getThreadSession();
                    updateFace = (PXAlbumFace) PXSessionUtility.tryLoad(aSession, PXAlbumFace.class, anAlbumId);
                    aTransaction = aSession.beginTransaction();
                    if (updateFace == null) {
                        File aPath = album.getAlbumPath();
                        if (aPath != null) {
                            PXAlbumFace anInvalidFace = PXAlbumFace.getFaceForPath(aPath.getAbsolutePath());
                            if (anInvalidFace != null) {
                                PXAlbumFace.removeFaceFromStore(anInvalidFace);
                            }
                        }
                        updateFace = new PXAlbumFace(anAlbumId);
                        updateFace.takeValuesFromAlbum(album);
                        aSession.save(updateFace);
                    } else {
                        updateFace.takeValuesFromAlbum(album);
                        updateFace.setLastUpdateDate(new Date());
                    }
                    aTransaction.commit();
                } catch (Exception anException) {
                    if (aTransaction != null) {
                        try {
                            aTransaction.rollback();
                        } catch (Exception eS) {
                        }
                    }
                    LOG.warn(null, anException);
                }
            }
        }
        return updateFace;
    }

    /**
	 * performs a Transaction in the ThreadSession
	 */
    public static void removeFaceFromStore(PXAlbumFace album) {
        if (album != null) {
            String anAlbumId = album.getId();
            if (anAlbumId != null) {
                Transaction aTransaction = null;
                try {
                    Session aSession = PXObjectStore.getInstance().getThreadSession();
                    aTransaction = aSession.beginTransaction();
                    aSession.delete(album);
                    aTransaction.commit();
                } catch (Exception anException) {
                    if (aTransaction != null) {
                        try {
                            aTransaction.rollback();
                        } catch (Exception eS) {
                        }
                    }
                    LOG.warn(null, anException);
                }
            }
        }
    }

    public static PXAlbumFace getFaceForPath(String path) {
        PXAlbumFace getFaceForPath = null;
        if (path != null) {
            try {
                Session aSession = PXObjectStore.getInstance().getThreadSession();
                if (aSession != null) {
                    String queryString = "from org.pixory.pxmodel.PXAlbumFace album where album.path = :path";
                    List someAlbums = aSession.createQuery(queryString).setString("path", path).list();
                    if ((someAlbums != null) && (someAlbums.size() > 0)) {
                        if (someAlbums.size() == 1) {
                            getFaceForPath = (PXAlbumFace) someAlbums.get(0);
                        } else {
                            String aMessage = "More than one album face found for path: " + path;
                            LOG.warn(aMessage);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn(null, e);
            }
        }
        return getFaceForPath;
    }

    /**
	 * remove all PXAlbumFaces where lastUpdateDate <
	 */
    public static void removeFacesUpdatedBefore(Date lastUpdateDate) {
        Transaction aTransaction = null;
        try {
            Session aSession = PXObjectStore.getInstance().getThreadSession();
            aTransaction = aSession.beginTransaction();
            String queryString = "delete org.pixory.pxmodel.PXAlbumFace album where album.lastUpdateDate < :lastUpdateDate";
            aSession.createQuery(queryString).setTimestamp("lastUpdateDate", lastUpdateDate).executeUpdate();
            aTransaction.commit();
        } catch (Exception anException) {
            if (aTransaction != null) {
                try {
                    aTransaction.rollback();
                } catch (Exception e) {
                }
            }
            LOG.warn(null, anException);
        }
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getPath() {
        if (_path == null) {
            if (_albumPath != null) {
                _path = _albumPath.getAbsolutePath();
            }
        }
        return _path;
    }

    public void setPath(String path) {
        _path = path;
        _albumPath = null;
    }

    public int getPhotoCount() {
        return _photoCount;
    }

    public void setPhotoCount(int photoCount) {
        _photoCount = photoCount;
    }

    public Date getStartDate() {
        return _startDate;
    }

    public void setStartDate(Date startDate) {
        _startDate = startDate;
    }

    public Date getEndDate() {
        return _endDate;
    }

    public void setEndDate(Date endDate) {
        _endDate = endDate;
    }

    public Boolean getIsPublic() {
        return _isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        _isPublic = isPublic;
    }

    public String getCoverPhotoName() {
        return _coverPhotoName;
    }

    public void setCoverPhotoName(String coverPhotoName) {
        _coverPhotoName = coverPhotoName;
    }

    public String getCoverText() {
        return _coverText;
    }

    public void setCoverText(String coverText) {
        _coverText = coverText;
    }

    public Date getCreationDate() {
        return _creationDate;
    }

    public void setCreationDate(Date creationDate) {
        _creationDate = creationDate;
    }

    public Date getLastUpdateDate() {
        return _lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        _lastUpdateDate = lastUpdateDate;
    }

    /**
	 * holy heinous hax batman! We need this awkward construct because of the
	 * decision to collapse the rotation property of an image into the PXPhoto
	 * for the image. Instead, it belongs as a property of the image itself,
	 * independent of any PXAlbum/PXPhoto. This will get fixed when the model
	 * gets rationalized
	 */
    public String getCoverPhotoRotationString() {
        return _coverPhotoRotationString;
    }

    public void setCoverPhotoRotationString(String coverPhotoRotationString) {
        _coverPhotoRotationString = coverPhotoRotationString;
    }

    public Set getInvitations() {
        return _invitations;
    }

    void setInvitations(Set invitations) {
        _invitations = invitations;
    }

    public PXShareMethod getShareMethod() {
        if (_shareMethod == null) {
            Boolean anIsPublic = this.getIsPublic();
            if ((anIsPublic != null) && (anIsPublic.booleanValue())) {
                _shareMethod = PXShareMethod.PUBLIC;
            } else {
                _shareMethod = DEFAULT_SHARE_METHOD;
            }
        }
        return _shareMethod;
    }

    public void setShareMethod(PXShareMethod shareMethod) {
        _shareMethod = shareMethod;
    }

    public Set getShares() {
        return _shares;
    }

    public void setShares(Set shares) {
        _shares = shares;
    }

    private void addShare(PXShare share) {
        if (share != null) {
            if (_shares == null) {
                _shares = new HashSet();
            }
            _shares.add(share);
        }
    }

    private void removeShare(PXShare share) {
        if ((share != null) && (_shares != null)) {
            _shares.remove(share);
        }
    }

    /**
	 * looks up names in the contact list of sharer; then invokes
	 * setShares(Set,PXPerson)
	 */
    public void setSharesFromContactNames(Set names, PXPerson sharer) {
        if ((names != null) && (sharer != null)) {
            Set someSharees = new HashSet(names.size());
            Iterator aNameIterator = names.iterator();
            while (aNameIterator.hasNext()) {
                String aName = (String) aNameIterator.next();
                PXIdentity aSharee = sharer.getContactForName(aName);
                if (aSharee != null) {
                    someSharees.add(aSharee);
                }
            }
            this.setShares(someSharees, sharer);
        }
    }

    /**
	 * high level convenience that adds and subtracts shares to ensure that the
	 * receivers shares precisely match the provided list; any added shares use
	 * the invocation time as the shareDate The entire operation is performed in
	 * a single Transaction against the currrent threadSession.
	 * However if a transaction is already running it will use that transaction.
	 * 
	 * @param sharees
	 *           Set of PXIdentity
	 */
    public void setShares(Set sharees, PXPerson sharer) {
        if ((sharees != null) && (sharer != null)) {
            HashSet someToAdd = new HashSet();
            HashSet someToRemove = this.getAllSharees();
            if (someToRemove == null) {
                someToRemove = new HashSet();
            }
            Iterator aShareeIterator = sharees.iterator();
            while (aShareeIterator.hasNext()) {
                PXIdentity aSharee = (PXIdentity) aShareeIterator.next();
                if (someToRemove.contains(aSharee)) {
                    someToRemove.remove(aSharee);
                } else {
                    someToAdd.add(aSharee);
                }
            }
            if ((someToAdd.size() > 0) || (someToRemove.size() > 0)) {
                Transaction aTransaction = null;
                boolean commitTransaction = false;
                try {
                    Session aSession = PXObjectStore.getInstance().getThreadSession();
                    aTransaction = aSession.getTransaction();
                    if (!aTransaction.isActive()) {
                        aTransaction = aSession.beginTransaction();
                        commitTransaction = true;
                    }
                    Iterator anIterator = someToAdd.iterator();
                    while (anIterator.hasNext()) {
                        PXIdentity aSharee = (PXIdentity) anIterator.next();
                        this.addShare(aSharee, sharer);
                    }
                    anIterator = someToRemove.iterator();
                    while (anIterator.hasNext()) {
                        PXIdentity aSharee = (PXIdentity) anIterator.next();
                        this.removeShare(aSharee, aSession);
                    }
                    if (commitTransaction) {
                        aTransaction.commit();
                    }
                } catch (Exception e) {
                    LOG.warn(null, e);
                    if (aTransaction != null) {
                        try {
                            aTransaction.rollback();
                        } catch (Exception f) {
                            LOG.warn(f);
                        }
                    }
                }
            }
        }
    }

    /**
	 * @return all the sharees from all the shares; the HashSet contains
	 *         PXIdentity
	 */
    public HashSet getAllSharees() {
        HashSet getAllSharees = null;
        Set someShares = this.getShares();
        if (someShares != null) {
            getAllSharees = new HashSet(someShares.size());
            Iterator aShareIterator = someShares.iterator();
            while (aShareIterator.hasNext()) {
                PXShare aShare = (PXShare) aShareIterator.next();
                PXIdentity aSharee = aShare.getSharee();
                if (aSharee != null) {
                    getAllSharees.add(aSharee);
                }
            }
        }
        return getAllSharees;
    }

    /**
	 * if the sharee and sharer match an existing share, then nothing happens
	 * 
	 * @return the added share
	 */
    public PXShare addShare(PXIdentity sharee, PXPerson sharer) {
        PXShare addShare = null;
        if ((sharee != null) && (sharer != null)) {
            addShare = new PXShare();
            addShare.setShareDate(new Date());
            addShare.setSharer(sharer);
            addShare.setSharee(sharee);
            addShare.setAlbum(this);
            this.addShare(addShare);
        }
        return addShare;
    }

    /**
	 * add sharees_ and commit in a single transaction using the ThreadSession
	 * 
	 * @param sharees_ Set of PXIdentity
	 */
    public void addShares(Set sharees_, PXPerson sharer_) {
        if ((sharees_ != null) && (sharees_.size() > 0) && (sharer_ != null)) {
            Transaction transaction = null;
            try {
                Session session = PXObjectStore.getInstance().getThreadSession();
                Iterator shareeIterator = sharees_.iterator();
                transaction = session.beginTransaction();
                while (shareeIterator.hasNext()) {
                    PXIdentity sharee = (PXIdentity) shareeIterator.next();
                    this.addShare(sharee, sharer_);
                }
                transaction.commit();
            } catch (Exception e) {
                LOG.debug(null, e);
                if (transaction != null) {
                    try {
                        transaction.rollback();
                    } catch (Exception f) {
                        LOG.warn(f);
                    }
                }
            }
        }
    }

    /**
	 * removes the share corresponding to the provided sharee. Does nothing if
	 * there is no match; deletes share row from the db
	 */
    public void removeShare(PXIdentity sharee, Session session) throws PXObjectStoreException {
        if (sharee != null) {
            PXShare aShare = this.getShareForSharee(sharee);
            if (aShare != null) {
                try {
                    this.removeShare(aShare);
                    if (session != null) {
                        session.delete(aShare);
                    }
                } catch (Exception e) {
                    LOG.warn(null, e);
                    if (!(e instanceof PXObjectStoreException)) {
                        e = new PXObjectStoreException(e);
                    }
                    throw (PXObjectStoreException) e;
                }
            }
        }
    }

    PXShare getShareForSharee(PXIdentity sharee) {
        PXShare getShare = null;
        if (sharee != null) {
            Set someShares = this.getShares();
            if ((someShares != null) && (someShares.size() > 0)) {
                Iterator anIterator = someShares.iterator();
                while (anIterator.hasNext()) {
                    PXShare aShare = (PXShare) anIterator.next();
                    PXIdentity aSharee = aShare.getSharee();
                    if ((aSharee != null) && (aSharee.equals(sharee))) {
                        getShare = aShare;
                        break;
                    }
                }
            }
        }
        return getShare;
    }

    public boolean isAccessibleTo(PXIdentity identity) {
        boolean isAccessibleTo = false;
        PXShareMethod aShareMethod = this.getShareMethod();
        LOG.debug("user: " + identity);
        LOG.debug("aShareMethod: " + aShareMethod);
        if (aShareMethod.equals(PXShareMethod.URL)) {
            isAccessibleTo = true;
        } else if (aShareMethod.equals(PXShareMethod.PUBLIC)) {
            isAccessibleTo = true;
        } else if (identity != null) {
            if ((identity instanceof PXPerson) && (((PXPerson) identity).isAdmin())) {
                isAccessibleTo = true;
            } else if (aShareMethod.equals(PXShareMethod.LIST)) {
                Set someSharedAlbums = identity.getToSharedAlbums();
                if ((someSharedAlbums != null) && (someSharedAlbums.contains(this))) {
                    isAccessibleTo = true;
                }
            }
        }
        LOG.debug("isAccessibleTo: " + isAccessibleTo);
        return isAccessibleTo;
    }

    public String getDisplayName() {
        return this.getName();
    }

    public void setDisplayName(String displayName) {
        this.setName(displayName);
    }

    public File getAlbumPath() {
        if (_albumPath == null) {
            if (_path != null) {
                _albumPath = new File(_path);
            }
        }
        return _albumPath;
    }

    public void setAlbumPath(File albumPath) {
        _albumPath = albumPath;
        _path = null;
    }

    public Integer getStartYear() {
        Integer getStartYear = null;
        Date aStartDate = this.getStartDate();
        if (aStartDate != null) {
            getStartYear = PXDateUtility.getYear(aStartDate);
        }
        return getStartYear;
    }

    public PXImageRotator.RotateType getCoverRotateType() {
        PXImageRotator.RotateType getCoverRotate = null;
        String aRotateString = this.getCoverPhotoRotationString();
        if (aRotateString != null) {
            getCoverRotate = PXImageRotator.RotateType.fromString(aRotateString);
        }
        return getCoverRotate;
    }

    public PXImage getCoverThumbnail(int size) {
        PXImage getCoverThumbnail = null;
        String aCoverPhotoName = this.getCoverPhotoName();
        PXImageRotator.RotateType aRotateType = this.getCoverRotateType();
        PXImageManager anImageManager = this.getImageManager();
        if ((anImageManager != null) && (aCoverPhotoName != null)) {
            PXThumbnailParam aThumbnailParam = new PXThumbnailParam(size, aRotateType);
            try {
                getCoverThumbnail = anImageManager.getThumbnailForImageNamed(aCoverPhotoName, aThumbnailParam);
            } catch (PXImageException e) {
                LOG.warn(null, e);
            }
        }
        return getCoverThumbnail;
    }

    private PXImageManager getImageManager() {
        if (_imageManager == null) {
            File anAlbumDirectory = this.getAlbumPath();
            if (anAlbumDirectory != null) {
                File anAlbumMetaDirectory = new File(anAlbumDirectory, PXAlbum.ALBUM_META_DIRECTORY);
                try {
                    _imageManager = new PXImageManager(anAlbumDirectory, anAlbumMetaDirectory);
                } catch (PXImageManagerException e) {
                    LOG.warn(null, e);
                }
            }
        }
        return _imageManager;
    }

    public void takeValuesFromAlbum(PXAlbumContent album) {
        if (album != null) {
            this.setId(album.getId());
            this.setDisplayName(album.getDisplayName());
            this.setAlbumPath(album.getAlbumPath());
            this.setPhotoCount(album.getPhotoCount());
            this.setIsPublic(album.getIsPublic());
            this.setShareMethod(PXShareMethod.fromString(album.getShareMethod()));
            this.setStartDate(album.getStartDate());
            this.setEndDate(album.getEndDate());
            this.setCoverText(album.getCoverText());
            PXPhoto aCoverPhoto = album.getCoverPhoto();
            if (aCoverPhoto != null) {
                this.setCoverPhotoName(aCoverPhoto.getName());
                ImageRotation aRotation = aCoverPhoto.getImageRotation();
                if (aRotation != null) {
                    PXImageRotator.RotateType aRotateType = PXImageRotationUtility.getRotateTypeForImageRotation(aRotation);
                    this.setCoverPhotoRotationString(aRotateType.toString());
                }
            }
        }
    }

    /**
	 * Equals method compares not on object identity, but on equality of id
	 */
    public boolean equals(Object other) {
        if (!(other instanceof PXAlbumFace)) return false;
        PXAlbumFace castOther = (PXAlbumFace) other;
        return new EqualsBuilder().append(this.getAlbumPath() + this.getName(), castOther.getAlbumPath() + castOther.getName()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getAlbumPath() + this.getName()).toHashCode();
    }
}
