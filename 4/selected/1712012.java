package de.juwimm.cms.remote;

import java.io.*;
import java.sql.Blob;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import de.juwimm.cms.authorization.model.UserHbm;
import de.juwimm.cms.common.Constants;
import de.juwimm.cms.exceptions.AlreadyCheckedOutException;
import de.juwimm.cms.exceptions.UserException;
import de.juwimm.cms.messaging.MessageConstants;
import de.juwimm.cms.messaging.bean.MessagingHubInvoker;
import de.juwimm.cms.model.*;
import de.juwimm.cms.remote.helper.AuthenticationHelper;
import de.juwimm.cms.util.SequenceHelper;
import de.juwimm.cms.vo.*;
import de.juwimm.util.DateConverter;
import de.juwimm.util.XercesHelper;

/**
 * @see de.juwimm.cms.remote.ContentServiceSpring
 * @author <a href="mailto:carsten.schalm@juwimm.com">Carsten Schalm</a> company Juwi|MacMillan Group Gmbh, Walsrode, Germany
 * @version $Id: ContentServiceSpringImpl.java 33 2009-02-20 08:48:27Z skulawik $
 */
public class ContentServiceSpringImpl extends ContentServiceSpringBase {

    private static Log log = LogFactory.getLog(ContentServiceSpringImpl.class);

    public static final byte MAX_NO_OF_CONTENT_VERSIONS_PER_PAGE = 10;

    private MessagingHubInvoker messagingHubInvoker;

    public MessagingHubInvoker getMessagingHubInvoker() {
        return messagingHubInvoker;
    }

    @Autowired
    public void setMessagingHubInvoker(MessagingHubInvoker messagingHubInvoker) {
        this.messagingHubInvoker = messagingHubInvoker;
    }

    public class DocumentCountWrapper {

        private HashMap<Integer, Integer> deltaDocuments = null;

        public DocumentCountWrapper(HashMap<Integer, Integer> startMap) {
            this.deltaDocuments = startMap;
        }

        public void addDocument(Integer docId, Integer docDelta) {
            Integer docCount = this.deltaDocuments.get(docId);
            if (docCount == null) docCount = new Integer(0);
            docCount = new Integer(docCount.intValue() + docDelta.intValue());
            this.deltaDocuments.put(docId, docCount);
        }

        public HashMap<Integer, Integer> getDeltaDocuments() {
            return deltaDocuments;
        }
    }

    private HashMap<Integer, Integer> getDeltaDocumentCounts(String oldContentText, String newContentText) {
        HashMap<Integer, Integer> deltaMap = new HashMap<Integer, Integer>();
        if (oldContentText != null && !"".equalsIgnoreCase(oldContentText)) {
            deltaMap = this.getDocumentCountWrapper(deltaMap, oldContentText, true).getDeltaDocuments();
        }
        if (newContentText != null && !"".equalsIgnoreCase(newContentText)) {
            deltaMap = this.getDocumentCountWrapper(deltaMap, newContentText, false).getDeltaDocuments();
        }
        return deltaMap;
    }

    private DocumentCountWrapper getDocumentCountWrapper(HashMap<Integer, Integer> startMap, String contentText, boolean isOld) {
        DocumentCountWrapper dcw = new DocumentCountWrapper(startMap);
        org.w3c.dom.Document contentNode = null;
        try {
            contentNode = XercesHelper.string2Dom(contentText);
        } catch (Exception e) {
            if (log.isDebugEnabled()) log.debug("Content is not valid XML!");
        }
        if (contentNode != null) {
            Iterator it = XercesHelper.findNodes(contentNode, "//document");
            while (it.hasNext()) {
                Element doc = (Element) it.next();
                Integer docId = null;
                try {
                    docId = Integer.valueOf(doc.getAttribute("src"));
                } catch (NumberFormatException nfe) {
                    if (log.isDebugEnabled()) log.debug(doc.getAttribute("src") + " is not a valid Number (DocumentId)!");
                }
                if (docId != null) {
                    dcw.addDocument(docId, Integer.valueOf(isOld ? -1 : +1));
                }
            }
        }
        return dcw;
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#addPicture2Unit(java.lang.Integer, byte[], byte[], java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
    protected Integer handleAddPicture2Unit(Integer unitId, byte[] thumbnail, byte[] picture, String mimeType, String altText, String pictureName) throws Exception {
        PictureHbm pictureHbm = PictureHbm.Factory.newInstance(thumbnail, picture, null, mimeType, null, altText, pictureName, null, null, null);
        UnitHbm unit = super.getUnitHbmDao().load(unitId);
        pictureHbm.setUnit(unit);
        pictureHbm = super.getPictureHbmDao().create(pictureHbm);
        unit = super.getUnitHbmDao().load(unitId);
        unit.addPicture(pictureHbm);
        return pictureHbm.getPictureId();
    }

    @Override
    protected Integer handleAddPictureWithPreview2Unit(Integer unitId, byte[] thumbnail, byte[] picture, byte[] preview, String mimeType, String altText, String pictureName) throws Exception {
        UnitHbm unit = super.getUnitHbmDao().load(unitId);
        PictureHbm pictureHbm = PictureHbm.Factory.newInstance(thumbnail, picture, preview, mimeType, null, altText, pictureName, null, null, null);
        pictureHbm.setPictureId(SequenceHelper.getSequenceSession().getNextSequenceNumber("picture.picture_id"));
        pictureHbm = super.getPictureHbmDao().create(pictureHbm);
        unit.addPicture(pictureHbm);
        return pictureHbm.getPictureId();
    }

    /**
	 * Save content and create new contentVersion if heading or content is different from saved state
	 * @see de.juwimm.cms.remote.ContentServiceSpring#checkIn(de.juwimm.cms.vo.ContentValue)
	 */
    @Override
    protected void handleCheckIn(ContentValue contentValue) throws Exception {
        if (log.isDebugEnabled()) log.debug("begin checkIn ContentValue");
        try {
            UserHbm user = super.getUserHbmDao().load(AuthenticationHelper.getUserName());
            ContentHbm content = super.getContentHbmDao().load(contentValue.getContentId());
            ContentVersionHbm latest = content.getLastContentVersion();
            boolean headingEqual = latest.getHeading() != null && latest.getHeading().equals(contentValue.getHeading());
            boolean textEqual = latest.getText() != null && latest.getText().equals(contentValue.getContentText());
            if (headingEqual && textEqual) {
                contentValue.setCreateNewVersion(false);
                this.updateDocumentUseCountLastVersion(latest.getText(), contentValue.getContentText());
            }
            contentValue.setVersion(latest.getVersion());
            this.checkIn(contentValue, content, latest, user);
            this.removeSpareContentVersions(contentValue.getContentId());
        } catch (Exception e) {
            log.error("Error checking in: " + e.getMessage(), e);
            throw new UserException("Error checking in: " + e);
        }
        if (log.isDebugEnabled()) log.debug("end checkIn ContentValue");
    }

    private void checkIn(ContentValue contentValue, ContentHbm content, ContentVersionHbm contentVersion, UserHbm user) throws UserException {
        if (log.isDebugEnabled()) log.debug("begin checkIn()");
        LockHbm lock = contentVersion.getLock();
        if (lock != null) {
            if (user.getUserId().equalsIgnoreCase(lock.getOwner().getUserId())) {
                if (contentValue.isCreateNewVersion()) {
                    try {
                        ContentVersionHbm newContentVersion = ContentVersionHbm.Factory.newInstance();
                        newContentVersion.setCreateDate(System.currentTimeMillis());
                        newContentVersion.setVersionComment(contentValue.getComment());
                        newContentVersion.setText(contentValue.getContentText());
                        newContentVersion.setHeading(contentValue.getHeading());
                        newContentVersion.setCreator(user.getUserId());
                        newContentVersion.setVersion(this.createNewVersionNr(contentVersion.getVersion()));
                        newContentVersion = super.getContentVersionHbmDao().create(newContentVersion);
                        content.getContentVersions().add(newContentVersion);
                    } catch (Exception exe) {
                        throw new UserException(exe.getMessage());
                    }
                } else {
                    contentVersion.setVersionComment(contentValue.getComment());
                    contentVersion.setText(contentValue.getContentText());
                    contentVersion.setHeading(contentValue.getHeading());
                    contentVersion.setCreateDate(System.currentTimeMillis());
                    contentVersion.setCreator(user.getUserId());
                }
                if (contentValue.getContentText() != null) {
                    Properties prop = new Properties();
                    prop.setProperty(MessageConstants.MSG_PROP_CONTENT_ID, content.getContentId().toString());
                    getMessagingHubInvoker().invokeQueue(MessageConstants.QUEUE_NAME_SEARCHENGINE, MessageConstants.MESSAGE_TYPE_CONTENT_SAVED, prop);
                }
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing Lock");
                    }
                    super.getLockHbmDao().remove(lock);
                } catch (Exception exe) {
                    throw new UserException(exe.getMessage());
                }
            } else {
                throw new UserException("checked out by another User: " + lock.getOwner().getUserId());
            }
        }
        if (log.isDebugEnabled()) log.debug("end checkIn()");
    }

    /**
	 * Checkin only without creating a specialized version <br/>
	 * This is used for checking in all remaining pages while exiting the app<br/>
	 * Just remove any lock
	 * 
	 * @throws UserException
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#checkIn4ContentId(java.lang.Integer)
	 */
    @Override
    protected void handleCheckIn4ContentId(Integer contentId) throws Exception {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            ContentVersionHbm latest = content.getLastContentVersion();
            LockHbm lock = latest.getLock();
            if (lock != null) {
                latest.setLock(null);
                super.getLockHbmDao().remove(lock);
            }
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * Lock this content-page exclusively for the current user
	 * @see de.juwimm.cms.remote.ContentServiceSpring#checkOut(java.lang.Integer, boolean)
	 */
    @Override
    protected ContentValue handleCheckOut(Integer contentId, boolean force) throws Exception {
        try {
            String caller = AuthenticationHelper.getUserName();
            ContentHbm content = super.getContentHbmDao().load(contentId);
            LockHbm lock = content.getLastContentVersion().getLock();
            UserHbm user = super.getUserHbmDao().load(caller);
            if (lock == null || force || lock.getOwner() == null) {
                this.checkOut(content, force, user);
                return content.getDao();
            }
            String lockOwner = lock.getOwner().getUserId();
            if (caller.equals(lockOwner)) {
                if (log.isDebugEnabled()) log.debug("AlreadyCheckedOut by mysqlf - " + caller);
                throw new AlreadyCheckedOutException("");
            }
            if (log.isDebugEnabled()) {
                log.debug("AlreadyCheckedOut by - " + lockOwner);
            }
            throw new AlreadyCheckedOutException(lockOwner);
        } catch (AlreadyCheckedOutException acoe) {
            log.warn("Content with id " + contentId + " already checked out.", acoe);
            throw new AlreadyCheckedOutException(acoe.getMessage());
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage(), e);
            throw new UserException(e.getMessage());
        }
    }

    private void checkOut(ContentHbm content, boolean force, UserHbm user) throws UserException, AlreadyCheckedOutException {
        if (log.isDebugEnabled()) log.debug("begin checkOut()");
        ContentVersionHbm latest = content.getLastContentVersion();
        LockHbm lock = latest.getLock();
        if (lock != null && lock.getOwner() != null) {
            if (!force) {
                if (user.getUserId().equals(lock.getOwner().getUserId())) {
                    if (log.isDebugEnabled()) {
                        log.debug("AlreadyCheckedOut by mysqlf - " + user.getUserId());
                    }
                    throw new AlreadyCheckedOutException("");
                }
                if (log.isDebugEnabled()) {
                    log.debug("AlreadyCheckedOut by - " + lock.getOwner().getUserId());
                }
                throw new AlreadyCheckedOutException(lock.getOwner().getUserId());
            }
            try {
                super.getLockHbmDao().remove(lock);
                LockHbm newLock = LockHbm.Factory.newInstance();
                newLock.setOwner(user);
                newLock.setCreateDate(System.currentTimeMillis());
                newLock = super.getLockHbmDao().create(newLock);
                latest.setLock(newLock);
            } catch (Exception exe) {
                log.error("Error occured", exe);
            }
        } else {
            try {
                LockHbm newLock = LockHbm.Factory.newInstance();
                newLock.setOwner(user);
                newLock.setCreateDate(System.currentTimeMillis());
                newLock = super.getLockHbmDao().create(newLock);
                latest.setLock(newLock);
                if (log.isDebugEnabled()) {
                    log.debug("Setting lock for checkout " + user.getUserId());
                }
            } catch (Exception exe) {
                log.error("Error occured", exe);
                throw new UserException(exe.getMessage());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("end checkOut()");
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#createContent(de.juwimm.cms.vo.ContentValue)
	 */
    @Override
    protected ContentValue handleCreateContent(ContentValue contentValue) throws Exception {
        ContentHbm contentHbm = super.getContentHbmDao().create(contentValue, getPrincipal().getName());
        if (contentValue.getContentText() != null) {
            Properties prop = new Properties();
            prop.setProperty(MessageConstants.MSG_PROP_CONTENT_ID, contentHbm.getContentId().toString());
            getMessagingHubInvoker().invokeQueue(MessageConstants.QUEUE_NAME_SEARCHENGINE, MessageConstants.MESSAGE_TYPE_CONTENT_SAVED, prop);
        }
        return contentHbm.getDao();
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#createEdition(java.lang.String, java.lang.Integer, boolean, boolean)
	 */
    @Override
    protected void handleCreateEdition(String commentText, Integer rootViewComponentId, boolean deploy, boolean succMessage) throws Exception {
        log.info("Enqueue createEdition-Event " + AuthenticationHelper.getUserName() + " rootVCID " + rootViewComponentId);
        SiteHbm site = null;
        ViewComponentHbm rootVc = null;
        try {
            site = super.getUserHbmDao().load(AuthenticationHelper.getUserName()).getActiveSite();
            rootVc = super.getViewComponentHbmDao().load(rootViewComponentId);
        } catch (Exception exe) {
            log.error("Havent found either site or viewcomponent: (rootVc)" + rootViewComponentId + " " + exe.getMessage());
        }
        if (rootVc != null && site != null && rootVc.getViewDocument().getSite().equals(site)) {
            try {
                log.info("Enqueue createEdition-Event for " + rootViewComponentId + ": " + rootVc.getAssignedUnit().getName().trim());
            } catch (Exception e) {
            }
            try {
                Properties prop = new Properties();
                prop.setProperty("userName", AuthenticationHelper.getUserName());
                prop.setProperty("comment", commentText);
                prop.setProperty("rootViewComponentId", rootViewComponentId.toString());
                prop.setProperty("siteId", site.getSiteId().toString());
                prop.setProperty("deploy", Boolean.toString(deploy));
                prop.setProperty("showMessage", Boolean.toString(succMessage));
                getMessagingHubInvoker().invokeQueue(MessageConstants.QUEUE_NAME_DEPLOY, MessageConstants.MESSAGE_TYPE_LIVE_DEPLOY, prop);
                if (log.isDebugEnabled()) log.debug("Finished createEdtion Task on Queue");
            } catch (Exception e) {
                throw new UserException(e.getMessage());
            }
        } else {
            throw new UserException("User was not loggedin to the site he is willing to deploy. Deploy has been CANCELED.");
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#createPicture(byte[], byte[], java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
    protected Integer handleCreatePicture(byte[] thumbnail, byte[] picture, String mimeType, String altText, String pictureName) throws Exception {
        PictureHbm pictureHbm = PictureHbm.Factory.newInstance(thumbnail, picture, null, mimeType, null, altText, pictureName, null, null, null);
        pictureHbm = super.getPictureHbmDao().create(pictureHbm);
        return pictureHbm.getPictureId();
    }

    /**
	 * Creates a new FULL-Edition for the active site and returns it as SOAP-Attachment.
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#exportEditionFull()
	 */
    @Override
    protected InputStream handleExportEditionFull() throws Exception {
        try {
            log.info("createEditionForExport " + AuthenticationHelper.getUserName());
            File fle = File.createTempFile("edition_full_export", ".xml.gz");
            FileOutputStream fout = new FileOutputStream(fle);
            GZIPOutputStream gzoudt = new GZIPOutputStream(fout);
            PrintStream out = new PrintStream(gzoudt, true, "UTF-8");
            UserHbm invoker = super.getUserHbmDao().load(AuthenticationHelper.getUserName());
            SiteHbm site = invoker.getActiveSite();
            if (log.isDebugEnabled()) log.debug("Invoker is: " + invoker.getUserId() + " within Site " + site.getName());
            EditionHbm edition = super.getEditionHbmDao().create("INTERIMEDITION", null, null, true);
            if (log.isDebugEnabled()) log.debug("Dummy-Editon create");
            out.println("<edition>");
            if (log.isDebugEnabled()) log.debug("picturesToXmlRecursive");
            getEditionHbmDao().picturesToXmlRecursive(null, site.getSiteId(), out, edition);
            System.gc();
            if (log.isDebugEnabled()) log.debug("documentsToXmlRecursive");
            getEditionHbmDao().documentsToXmlRecursive(null, site.getSiteId(), out, true, edition);
            System.gc();
            if (log.isDebugEnabled()) log.debug("unitsToXmlRecursive");
            getEditionHbmDao().unitsToXmlRecursive(site.getSiteId(), out, edition);
            System.gc();
            if (log.isDebugEnabled()) log.debug("hostsToXmlRecursive");
            getEditionHbmDao().hostsToXmlRecursive(site.getSiteId(), out, edition);
            if (log.isDebugEnabled()) log.debug("viewdocumentsToXmlRecursive");
            getEditionHbmDao().viewdocumentsToXmlRecursive(site.getSiteId(), out, edition);
            if (log.isDebugEnabled()) log.debug("realmsToXmlRecursive");
            getEditionHbmDao().realmsToXmlRecursive(site.getSiteId(), out, edition);
            System.gc();
            if (log.isDebugEnabled()) log.debug("Creating ViewComponent Data");
            Iterator vdIt = getViewDocumentHbmDao().findAll(site.getSiteId()).iterator();
            while (vdIt.hasNext()) {
                ViewDocumentHbm vdl = (ViewDocumentHbm) vdIt.next();
                super.getViewComponentHbmDao().toXml(vdl.getViewComponent(), new Integer(0), true, false, 1, false, false, out);
            }
            if (log.isDebugEnabled()) log.debug("Finished creating ViewComponent Data");
            out.println("</edition>");
            super.getEditionHbmDao().remove(edition);
            out.flush();
            out.close();
            out = null;
            return new FileInputStream(fle);
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * Creates a new Unit-Edition for the active site and returns it as SOAP-Attachment.
	 * 
	 * @throws UserException
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#exportEditionUnit(java.lang.Integer)
	 */
    @Override
    protected InputStream handleExportEditionUnit(Integer rootViewComponentId) throws Exception {
        try {
            File fle = File.createTempFile("edition_unit_export", ".xml.gz");
            FileOutputStream fout = new FileOutputStream(fle);
            GZIPOutputStream gzoudt = new GZIPOutputStream(fout);
            PrintStream out = new PrintStream(gzoudt, true, "UTF-8");
            EditionHbm edition = super.getEditionHbmDao().create("RETURNEDITION", rootViewComponentId, out, true);
            super.getEditionHbmDao().remove(edition);
            out.flush();
            out.close();
            out = null;
            return new FileInputStream(fle);
        } catch (Exception e) {
            log.error("Could not export edition unit", e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllContentVersions(java.lang.Integer)
	 */
    @Override
    protected ContentVersionValue[] handleGetAllContentVersions(Integer contentId) throws UserException {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            Collection<ContentVersionHbm> coll = content.getContentVersions();
            Iterator<ContentVersionHbm> it = coll.iterator();
            TreeMap<Integer, ContentVersionHbm> tm = new TreeMap<Integer, ContentVersionHbm>();
            while (it.hasNext()) {
                ContentVersionHbm cvd = it.next();
                if (!cvd.getVersion().equals("PUBLS")) {
                    tm.put(new Integer(cvd.getVersion()), cvd);
                }
            }
            it = tm.values().iterator();
            int siz = tm.values().size();
            ContentVersionValue[] arr = new ContentVersionValue[siz];
            for (int i = 0; i < siz; i++) {
                arr[i] = it.next().getDao();
                if (i == (siz - 1)) {
                    arr[i].setVersionComment(arr[i].getCreator() + " (" + DateConverter.getSql2String(new Date(arr[i].getCreateDate())) + ")");
                } else {
                    arr[i].setVersionComment(arr[i].getVersion() + " - " + arr[i].getCreator() + " (" + DateConverter.getSql2String(new Date(arr[i].getCreateDate())) + ")");
                }
                arr[i].setText(null);
            }
            return arr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllContentVersionsId(java.lang.Integer)
	 */
    @Override
    protected Integer[] handleGetAllContentVersionsId(Integer contentId) throws Exception {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            Collection coll = content.getContentVersions();
            Iterator it = coll.iterator();
            TreeMap<Integer, ContentVersionHbm> tm = new TreeMap<Integer, ContentVersionHbm>();
            while (it.hasNext()) {
                ContentVersionHbm cvd = (ContentVersionHbm) it.next();
                if (!cvd.getVersion().equals("PUBLS")) {
                    tm.put(new Integer(cvd.getVersion()), cvd);
                }
            }
            it = tm.values().iterator();
            int siz = tm.values().size();
            Integer[] arr = new Integer[siz];
            for (int i = 0; i < siz; i++) {
                arr[i] = ((ContentVersionHbm) it.next()).getContentVersionId();
            }
            return arr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllDocuments4Unit(java.lang.Integer)
	 */
    @Override
    protected Integer[] handleGetAllDocuments4Unit(Integer unitId) throws Exception {
        try {
            Collection coll = super.getDocumentHbmDao().findAll(unitId);
            Iterator it = coll.iterator();
            int siz = coll.size();
            Integer[] itarr = new Integer[siz];
            for (int i = 0; i < siz; i++) {
                itarr[i] = ((de.juwimm.cms.model.DocumentHbm) it.next()).getDocumentId();
            }
            return itarr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllPictures4Unit(java.lang.Integer)
	 */
    @Override
    protected Integer[] handleGetAllPictures4Unit(Integer unitId) throws Exception {
        try {
            Collection coll = super.getPictureHbmDao().findAll(unitId);
            int siz = coll.size();
            Iterator it = coll.iterator();
            Integer[] itarr = new Integer[siz];
            for (int i = 0; i < siz; i++) {
                itarr[i] = ((PictureHbm) it.next()).getPictureId();
            }
            return itarr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllSlimDocuments4Unit(java.lang.Integer)
	 */
    @Override
    protected DocumentSlimValue[] handleGetAllSlimDocuments4Unit(Integer unitId) throws Exception {
        DocumentSlimValue[] dvArr = null;
        try {
            Collection<DocumentHbm> coll = super.getDocumentHbmDao().findAll(unitId);
            dvArr = new DocumentSlimValue[coll.size()];
            int i = 0;
            for (DocumentHbm doc : coll) {
                dvArr[i++] = doc.getSlimValue();
            }
            return dvArr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAllSlimPictures4Unit(java.lang.Integer)
	 */
    @Override
    protected PictureSlimstValue[] handleGetAllSlimPictures4Unit(Integer unitId) throws Exception {
        PictureSlimstValue[] pvArr = null;
        try {
            Collection coll = super.getPictureHbmDao().findAll(unitId);
            Iterator it = coll.iterator();
            int siz = coll.size();
            pvArr = new PictureSlimstValue[siz];
            for (int i = 0; i < siz; i++) {
                PictureHbm pic = (PictureHbm) it.next();
                pvArr[i] = pic.getSlimstValue();
            }
            return pvArr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getAnchors(java.lang.Integer)
	 */
    @Override
    protected String[] handleGetAnchors(Integer contentId) throws Exception {
        try {
            String[] anchArr = new String[0];
            ContentHbm content = super.getContentHbmDao().load(contentId);
            ContentVersionHbm cvl = content.getLastContentVersion();
            String contentText = cvl.getText();
            org.w3c.dom.Document doc = XercesHelper.string2Dom(contentText);
            Iterator nit = XercesHelper.findNodes(doc, "//anchor/a");
            Vector<String> vec = new Vector<String>();
            while (nit.hasNext()) {
                Element elm = (Element) nit.next();
                try {
                    String anchor = elm.getAttribute("name");
                    vec.add(anchor);
                } catch (Exception exe) {
                }
            }
            anchArr = vec.toArray(new String[0]);
            return anchArr;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getContent(java.lang.Integer)
	 */
    @Override
    protected ContentValue handleGetContent(Integer contentId) throws Exception {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            return content.getDao();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getContentTemplateName(java.lang.Integer)
	 */
    @Override
    protected String handleGetContentTemplateName(Integer viewComponentId) throws Exception {
        ContentHbm content = null;
        ViewComponentHbm view = null;
        try {
            view = super.getViewComponentHbmDao().load(viewComponentId);
            if (view.getViewType() == Constants.VIEW_TYPE_INTERNAL_LINK || view.getViewType() == Constants.VIEW_TYPE_SYMLINK) {
                view = super.getViewComponentHbmDao().load(new Integer(view.getReference()));
                content = super.getContentHbmDao().load(new Integer(view.getReference()));
            } else {
                content = super.getContentHbmDao().load(new Integer(view.getReference()));
            }
            if (log.isDebugEnabled()) {
                log.debug("content: " + content + " vcId: " + viewComponentId);
                if (content != null) {
                    log.debug("content: " + content.getTemplate());
                }
            }
            return content.getTemplate();
        } catch (Exception e) {
            throw new UserException("Could not find referenced ContentVersion with Id: " + view.getReference() + " vcid:" + viewComponentId + "\n" + e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getContentVersion(java.lang.Integer)
	 */
    @Override
    protected ContentVersionValue handleGetContentVersion(Integer contentVersionId) throws Exception {
        try {
            ContentVersionHbm contentVersion = super.getContentVersionHbmDao().load(contentVersionId);
            return contentVersion.getDao();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getContentWithPUBLSVersion(java.lang.Integer)
	 */
    @Override
    protected ContentValue handleGetContentWithPUBLSVersion(Integer contentId) throws Exception {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            return content.getDaoWithPUBLSVersion();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getDocument(java.lang.Integer)
	 */
    @Override
    protected byte[] handleGetDocument(Integer documentId) throws Exception {
        byte[] retArr = null;
        try {
            log.debug("LOOKING FOR DOCUMENT");
            DocumentHbm document = super.getDocumentHbmDao().load(documentId);
            retArr = IOUtils.toByteArray(document.getDocument().getBinaryStream());
            if (log.isDebugEnabled()) {
                try {
                    log.debug("GOT THE DOCUMENT");
                    log.debug("DOCUMENT SIZE " + retArr.length);
                } catch (Exception inew) {
                    log.debug(inew.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("CANNOT GET DOCUMENT " + e.getMessage());
            throw new UserException(e.getMessage());
        }
        return retArr;
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getDocumentName(java.lang.Integer)
	 */
    @Override
    protected String handleGetDocumentName(Integer documentId) throws Exception {
        try {
            DocumentHbm document = super.getDocumentHbmDao().load(documentId);
            return document.getDocumentName();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getEchoDAO(java.lang.String)
	 */
    @Override
    protected ContentValue handleGetEchoDAO(String echoString) throws Exception {
        try {
            System.out.println("GOT AN ECHO REQUEST " + echoString);
            ContentValue cdao = new ContentValue();
            cdao.setHeading(echoString);
            return cdao;
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getEchoString(java.lang.String)
	 */
    @Override
    protected String handleGetEchoString(String echoString) throws Exception {
        try {
            ContentVersionHbm cvl = super.getContentVersionHbmDao().load(new Integer(999999));
            System.out.println("GOT AN ECHO REQUEST cvl.getHeading() " + cvl.getHeading());
            return cvl.getHeading();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getEditions(java.lang.Integer, java.lang.Integer)
	 */
    @Override
    protected EditionValue[] handleGetEditions(Integer unitId, Integer viewDocumentId) throws Exception {
        try {
            Collection<EditionHbm> editions = super.getEditionHbmDao().findByUnitAndViewDocument(unitId, viewDocumentId);
            EditionValue[] editionValues = new EditionValue[editions.size()];
            int i = 0;
            for (EditionHbm edition : editions) {
                editionValues[i++] = edition.getDao();
            }
            return editionValues;
        } catch (Exception e) {
            log.error("Could not get edition values", e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getMimeType4Document(java.lang.Integer)
	 */
    @Override
    protected String handleGetMimeType4Document(Integer pictureId) throws Exception {
        DocumentHbm document = super.getDocumentHbmDao().load(pictureId);
        try {
            return document.getMimeType();
        } catch (Exception e) {
            log.error("Could not get mime type for document with id: " + pictureId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * Get mime type for document
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getMimeType4Picture(java.lang.Integer)
	 */
    @Override
    protected String handleGetMimeType4Picture(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        try {
            return picture.getMimeType();
        } catch (Exception e) {
            log.error("Could not get mime type for picture with id: " + pictureId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getNotReferencedUnits(de.juwimm.cms.vo.ViewDocumentValue)
	 */
    @Override
    protected UnitValue[] handleGetNotReferencedUnits(ViewDocumentValue viewDocument) throws Exception {
        log.info("starting getNotReferencedUnits for Site " + viewDocument.getSiteId() + " and Language " + viewDocument.getLanguage());
        try {
            Collection coll = super.getViewComponentHbmDao().findAllWithUnit(viewDocument.getViewDocumentId());
            Collection u = super.getUnitHbmDao().findAll(viewDocument.getSiteId());
            Vector<Integer> units = new Vector<Integer>();
            Iterator itUnits = u.iterator();
            while (itUnits.hasNext()) {
                UnitHbm unit = (UnitHbm) itUnits.next();
                units.add(unit.getUnitId());
            }
            Iterator it = coll.iterator();
            while (it.hasNext()) {
                ViewComponentHbm vcl = (ViewComponentHbm) it.next();
                try {
                    units.remove(vcl.getAssignedUnit().getUnitId());
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not remove assigned unit: " + e.getMessage());
                    }
                }
            }
            UnitValue[] unitdaos = new UnitValue[units.size()];
            it = units.iterator();
            int i = 0;
            while (it.hasNext()) {
                Integer unitId = (Integer) it.next();
                UnitHbm ul = super.getUnitHbmDao().load(unitId);
                unitdaos[i++] = ul.getDao();
            }
            if (log.isDebugEnabled()) log.debug("end getNotReferencedUnits for Site " + viewDocument.getSiteId() + " and Language " + viewDocument.getLanguage());
            return unitdaos;
        } catch (Exception e) {
            log.warn("Error getNotReferencedUnits for Site " + viewDocument.getSiteId() + " and Language " + viewDocument.getLanguage() + ": " + e.getMessage(), e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getPicture(java.lang.Integer)
	 */
    @Override
    protected PictureSlimValue handleGetPicture(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        return picture.getSlimValue();
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getPictureAltText(java.lang.Integer)
	 */
    @Override
    protected String handleGetPictureAltText(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        return picture.getAltText();
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getPictureData(java.lang.Integer)
	 */
    @Override
    protected byte[] handleGetPictureData(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        try {
            return picture.getPicture();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not get data for picture with id: " + pictureId, e);
            }
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getPictureFileName(java.lang.Integer)
	 */
    @Override
    protected String handleGetPictureFileName(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        return picture.getPictureName();
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getThumbnail(java.lang.Integer)
	 */
    @Override
    protected byte[] handleGetThumbnail(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        if (picture != null) {
            return picture.getThumbnail();
        }
        return new byte[0];
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getTimestamp4Document(java.lang.Integer)
	 */
    @Override
    protected Long handleGetTimestamp4Document(Integer pictureId) throws Exception {
        DocumentHbm document = super.getDocumentHbmDao().load(pictureId);
        try {
            return document.getTimeStamp();
        } catch (Exception e) {
            log.error("Could not get timestamp for document with id: " + pictureId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#getTimestamp4Picture(java.lang.Integer)
	 */
    @Override
    protected Long handleGetTimestamp4Picture(Integer pictureId) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        try {
            return picture.getTimeStamp();
        } catch (Exception e) {
            log.error("Could not get timestamp for picture with id: " + pictureId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#importDocument(java.lang.Integer, java.lang.String, java.lang.String, java.io.InputStream)
	 */
    @Override
    protected Integer handleImportDocument(Integer unitId, String documentName, String mimeType, InputStream documentData, int documentSize) throws Exception {
        try {
            if (log.isDebugEnabled()) log.debug("importDocument for user " + AuthenticationHelper.getUserName());
            if (unitId != null && unitId.intValue() <= 0) unitId = null;
            UnitHbm unit = getUnitHbmDao().load(unitId);
            DocumentHbm doc = DocumentHbm.Factory.newInstance();
            doc.setDocumentName(documentName);
            doc.setMimeType(mimeType);
            doc.setTimeStamp(System.currentTimeMillis());
            doc.setUnit(unit);
            doc = getDocumentHbmDao().create(doc);
            Blob b = Hibernate.createBlob(documentData, documentSize);
            doc.setDocument(b);
            unit.addDocument(doc);
            return doc.getDocumentId();
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * Imports an Edition though an <b>.xml.gz </b> Upload. <br>
	 * It will be expected as a SOAP Attachment.
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#importEdition(java.lang.Integer, java.io.InputStream)
	 */
    @Override
    protected void handleImportEdition(Integer rootVcId, InputStream in) throws Exception {
        try {
            log.info("importEdition " + AuthenticationHelper.getUserName());
            if (rootVcId != null && rootVcId.intValue() <= 0) rootVcId = null;
            String tmpFileName = "";
            try {
                tmpFileName = this.storeEditionFile(in);
            } catch (IOException e) {
                log.warn("Unable to copy received inputstream: " + e.getMessage(), e);
            }
            if (log.isDebugEnabled()) log.debug("tmpFile " + tmpFileName);
            System.gc();
            log.info("Finished writing Edition to File, starting to throw Message into JMS Queue");
            Properties prop = new Properties();
            prop.setProperty("userName", AuthenticationHelper.getUserName());
            if (rootVcId != null) {
                prop.setProperty("rootVcId", rootVcId.toString());
            }
            prop.setProperty("siteId", super.getUserHbmDao().load(AuthenticationHelper.getUserName()).getActiveSite().getSiteId().toString());
            prop.setProperty("editionFileName", tmpFileName);
            getMessagingHubInvoker().invokeQueue(MessageConstants.QUEUE_NAME_DEPLOY, MessageConstants.MESSAGE_TYPE_FULL_IMPORT, prop);
            log.info("end importEdition");
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#removeAllOldContentVersions(java.lang.Integer)
	 */
    @Override
    protected void handleRemoveAllOldContentVersions(Integer contentId) throws Exception {
        try {
            Integer[] allContentVersionIds = getAllContentVersionsId(contentId);
            ContentHbm content = super.getContentHbmDao().load(contentId);
            ContentVersionHbm lastContentVersion = content.getLastContentVersion();
            Integer lastContentVersionId = lastContentVersion.getContentVersionId();
            for (int i = 0; i < allContentVersionIds.length; i++) {
                if (!lastContentVersionId.equals(allContentVersionIds[i])) {
                    removeContentVersion(allContentVersionIds[i]);
                }
            }
            lastContentVersion.setVersion("1");
        } catch (Exception e) {
            log.error("Could not remove all old content versions from content with id " + contentId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#removeContentVersion(java.lang.Integer)
	 */
    @Override
    protected void handleRemoveContentVersion(Integer contentVersionId) throws Exception {
        super.getContentVersionHbmDao().remove(contentVersionId);
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#removeDocument(java.lang.Integer)
	 */
    @Override
    protected void handleRemoveDocument(Integer documentId) throws Exception {
        super.getDocumentHbmDao().remove(documentId);
    }

    /**
	 * Deletes an Edition
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#removeEdition(java.lang.Integer)
	 */
    @Override
    protected void handleRemoveEdition(Integer editionId) throws Exception {
        try {
            super.getEditionHbmDao().remove(editionId);
        } catch (Exception e) {
            log.error("Could not remove edition with id: " + editionId, e);
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#removePicture(java.lang.Integer)
	 */
    @Override
    protected void handleRemovePicture(Integer pictureId) throws Exception {
        super.getPictureHbmDao().remove(pictureId);
    }

    /**
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#restoreEdition(java.lang.Integer)
	 */
    @Override
    protected void handleRestoreEdition(Integer editionId) throws Exception {
        throw new UnsupportedOperationException("Method importEdition(Integer editionId) not yet implemented.");
    }

    /**
	 * Saves a content directly. ONLY for migration use! <br/>
	 * Is currently used to implement the XML-Lasche.
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#saveContent(java.lang.Integer, java.lang.String)
	 */
    @Override
    protected void handleSaveContent(Integer contentId, String contentText) throws Exception {
        try {
            ContentHbm content = super.getContentHbmDao().load(contentId);
            content.setContent(contentText);
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * Sets the ActiveEdition and deploy it to liveServer
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#setActiveEdition(java.lang.Integer, boolean)
	 */
    @Override
    protected void handleSetActiveEdition(Integer editionId, boolean deploy) throws Exception {
        try {
            EditionHbm edac = super.getEditionHbmDao().load(editionId);
            Collection coll = super.getEditionHbmDao().findByUnitAndViewDocument(new Integer(edac.getUnitId()), new Integer(edac.getViewDocumentId()));
            Iterator it = coll.iterator();
            EditionHbm edition = null;
            while (it.hasNext()) {
                edition = (EditionHbm) it.next();
                if (edition.getEditionId().equals(editionId)) {
                    if (edition.getStatus() == 0) {
                        if (log.isDebugEnabled()) log.debug("CALLING importEdition AND/OR publishToLiveserver");
                        if (deploy) {
                            edition = super.getEditionHbmDao().create(edition);
                            super.getEditionServiceSpring().publishEditionToLiveserver(edition.getEditionId());
                        }
                        edition.setStatus((byte) 1);
                    }
                } else {
                    edition.setStatus((byte) 0);
                }
            }
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#setPicture4Unit(java.lang.Integer, java.lang.Integer)
	 */
    @Override
    protected void handleSetPicture4Unit(Integer unitId, Integer pictureId) throws Exception {
        UnitHbm unit = super.getUnitHbmDao().load(unitId);
        unit.setImageId(pictureId);
    }

    /**
	 * @see de.juwimm.cms.remote.ContentServiceSpring#updatePictureAltText(java.lang.Integer, java.lang.String)
	 */
    @Override
    protected void handleUpdatePictureAltText(Integer pictureId, String altText) throws Exception {
        PictureHbm picture = super.getPictureHbmDao().load(pictureId);
        picture.setAltText(altText);
    }

    /**
	 * Changes the TemplateName. This should be used carefully by the siteRoot, because it could corrupt the content after a second "save". <br>
	 * In general this should be used only to change templates for pages, that can't be deleted - for example the root-page.
	 * 
	 * @param viewComponentId
	 *            The ViewComponent to which the templateName should be updated in the content bean
	 * @param templateName
	 *            The template Name
	 * 
	 * @see de.juwimm.cms.remote.ContentServiceSpring#updateTemplate(java.lang.Integer, java.lang.String)
	 */
    @Override
    protected void handleUpdateTemplate(Integer viewComponentId, String templateName) throws Exception {
        ViewComponentHbm viewComponent = super.getViewComponentHbmDao().load(viewComponentId);
        ContentHbm content = super.getContentHbmDao().load(new Integer(viewComponent.getReference()));
        content.setTemplate(templateName);
    }

    /**
	 * If this page has more than ContentServiceSpringImpl.MAX_NO_OF_CONTENT_VERSIONS_PER_PAGE contentVersions, the oldest ones are deleted, the rest gets renumbered. An existing PUBLS-Version is
	 * conserved.
	 */
    private void removeSpareContentVersions(Integer contentId) {
        try {
            Collection allContentVersions = super.getContentHbmDao().load(contentId).getContentVersions();
            if (allContentVersions != null && allContentVersions.size() > ContentServiceSpringImpl.MAX_NO_OF_CONTENT_VERSIONS_PER_PAGE) {
                Iterator<ContentVersionHbm> it = allContentVersions.iterator();
                TreeMap<Integer, ContentVersionHbm> tm = new TreeMap<Integer, ContentVersionHbm>();
                while (it.hasNext()) {
                    ContentVersionHbm cvd = it.next();
                    if (!cvd.getVersion().equals("PUBLS")) {
                        tm.put(new Integer(cvd.getVersion()), cvd);
                    }
                }
                List<Integer> cvList2Delete = new ArrayList<Integer>();
                List<ContentVersionHbm> cvList = new ArrayList<ContentVersionHbm>();
                it = tm.values().iterator();
                while (it.hasNext()) {
                    cvList.add(it.next());
                }
                int firstCoolIndex = tm.values().size() - ContentServiceSpringImpl.MAX_NO_OF_CONTENT_VERSIONS_PER_PAGE;
                int currentIndex = 1;
                for (int i = 0; i < cvList.size(); i++) {
                    ContentVersionHbm current = cvList.get(i);
                    if (i < firstCoolIndex) {
                        cvList2Delete.add(current.getContentVersionId());
                    } else {
                        current.setVersion(Integer.toString(currentIndex++));
                    }
                }
                Iterator<Integer> delIt = cvList2Delete.iterator();
                while (delIt.hasNext()) {
                    Integer currContentVersionId = delIt.next();
                    if (log.isDebugEnabled()) log.debug("Content: " + contentId + " Contentversion to delete: " + currContentVersionId);
                    super.getContentVersionHbmDao().remove(currContentVersionId);
                }
            }
        } catch (Exception e) {
            log.error("Error removing spare ContentVersions: " + e.getMessage(), e);
        }
    }

    private String storeEditionFile(InputStream in) throws IOException {
        String datadir = getCqPropertiesBeanSpring().getDatadir() + File.separator + "attachments" + File.separator;
        File attachmentsDir = new File(datadir);
        attachmentsDir.mkdirs();
        File storedEditionFile = File.createTempFile("edition_import_", ".tmp", attachmentsDir);
        FileOutputStream out = new FileOutputStream(storedEditionFile);
        IOUtils.copyLarge(in, out);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(in);
        return storedEditionFile.getAbsolutePath();
    }

    /**
	 * @param oldContentText
	 * @param newContentText
	 */
    private void updateDocumentUseCountLastVersion(String oldContentText, String newContentText) {
    }

    private void updateDocumentUseCountPublishVersion(String oldContentText, String newContentText) {
    }

    private void updateDocumentUseCounts(HashMap<Integer, Integer> deltaMap, boolean isLastVersion) {
        Iterator<Integer> it = deltaMap.keySet().iterator();
        while (it.hasNext()) {
            Integer docId = it.next();
            Integer docCountDelta = deltaMap.get(docId);
            if (docCountDelta != null && docCountDelta.intValue() != 0) {
                try {
                    DocumentHbm doc = super.getDocumentHbmDao().load(docId);
                    if (isLastVersion) {
                        int newCount = doc.getUseCountLastVersion() + docCountDelta.intValue();
                        if (newCount < 0) newCount = 0;
                        doc.setUseCountLastVersion(newCount);
                    } else {
                        int newCount = doc.getUseCountPublishVersion() + docCountDelta.intValue();
                        if (newCount < 0) newCount = 0;
                        doc.setUseCountPublishVersion(newCount);
                    }
                } catch (Exception e) {
                    log.warn("Error updating documentCount for document " + docId + ": " + e.getMessage());
                }
            }
        }
    }

    private String createNewVersionNr(String versionId) {
        if (versionId == null) {
            return "1";
        }
        int i = new Integer(versionId).intValue();
        return new String(new Integer(i + 1).toString());
    }
}
