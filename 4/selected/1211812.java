package org.kablink.teaming.ssfs.server.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.FileTypeMap;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.kablink.teaming.InternalException;
import org.kablink.teaming.ObjectKeys;
import org.kablink.teaming.dao.CoreDao;
import org.kablink.teaming.domain.Binder;
import org.kablink.teaming.domain.CustomAttribute;
import org.kablink.teaming.domain.Entry;
import org.kablink.teaming.domain.FileAttachment;
import org.kablink.teaming.domain.Folder;
import org.kablink.teaming.domain.NoBinderByTheIdException;
import org.kablink.teaming.domain.NoFolderByTheIdException;
import org.kablink.teaming.domain.ReservedByAnotherUserException;
import org.kablink.teaming.domain.EntityIdentifier.EntityType;
import org.kablink.teaming.module.binder.impl.WriteEntryDataException;
import org.kablink.teaming.module.file.LockIdMismatchException;
import org.kablink.teaming.module.file.LockedByAnotherUserException;
import org.kablink.teaming.module.file.WriteFilesException;
import org.kablink.teaming.module.profile.index.ProfileIndexUtils;
import org.kablink.teaming.module.shared.AccessUtils;
import org.kablink.teaming.module.shared.EmptyInputData;
import org.kablink.teaming.module.shared.InputDataAccessor;
import org.kablink.teaming.module.shared.MapInputData;
import org.kablink.teaming.security.AccessControlException;
import org.kablink.teaming.ssfs.AlreadyExistsException;
import org.kablink.teaming.ssfs.CrossContextConstants;
import org.kablink.teaming.ssfs.LockException;
import org.kablink.teaming.ssfs.NoAccessException;
import org.kablink.teaming.ssfs.NoSuchObjectException;
import org.kablink.teaming.ssfs.TypeMismatchException;
import org.kablink.teaming.ssfs.server.KablinkFileSystem;
import org.kablink.teaming.ssfs.server.KablinkFileSystemException;
import org.kablink.teaming.util.AllModulesInjected;
import org.kablink.teaming.util.DatedMultipartFile;
import org.kablink.util.Validator;
import org.kablink.util.search.Constants;

public class KablinkFileSystemInternal implements KablinkFileSystem {

    private static final String BINDER = "b";

    private static final String ENTRY = "e";

    private static final String DEFINITION = "d";

    private static final String FILE_ATTACHMENT = "fa";

    private static final String ELEMENT_NAME = "en";

    private AllModulesInjected bs;

    private FileTypeMap mimeTypes;

    private CoreDao coreDao;

    KablinkFileSystemInternal(AllModulesInjected bs) {
        this.bs = bs;
    }

    protected FileTypeMap getMimeTypes() {
        return this.mimeTypes;
    }

    public void setMimeTypes(FileTypeMap mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    protected CoreDao getCoreDao() {
        return coreDao;
    }

    public void setCoreDao(CoreDao coreDao) {
        this.coreDao = coreDao;
    }

    public void createResource(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        Map objMap = new HashMap();
        if (objectExists(uri, objMap)) throw new AlreadyExistsException("The resource already exists");
        writeResourceInternal(uri, objMap, new ByteArrayInputStream(new byte[0]));
    }

    public void setResource(Map uri, InputStream content) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        writeResourceInternal(uri, objMap, content);
    }

    public void createAndSetResource(Map uri, InputStream content) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        Map objMap = new HashMap();
        if (objectExists(uri, objMap)) throw new AlreadyExistsException("The resource already exists");
        writeResourceInternal(uri, objMap, content);
    }

    public void createDirectory(Map uri) throws NoAccessException, AlreadyExistsException, TypeMismatchException {
        throw new UnsupportedOperationException("(createDirectory)");
    }

    public InputStream getResource(Map uri) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        FileAttachment fa = (FileAttachment) objMap.get(FILE_ATTACHMENT);
        return bs.getFileModule().readFile((Binder) objMap.get(BINDER), (Entry) objMap.get(ENTRY), fa);
    }

    public void removeObject(Map uri) throws NoAccessException, NoSuchObjectException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        removeResourceInternal(uri, objMap);
    }

    public String[] getChildrenNames(Map uri) throws NoAccessException, NoSuchObjectException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        return getChildrenNamesInternal(uri, objMap);
    }

    public Map getProperties(Map uri) throws NoAccessException, NoSuchObjectException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        FileAttachment fa = (FileAttachment) objMap.get(FILE_ATTACHMENT);
        Map<String, Object> props = new HashMap<String, Object>();
        if (fa != null) {
            props.put(CrossContextConstants.OBJECT_INFO, CrossContextConstants.OBJECT_INFO_FILE);
            props.put(CrossContextConstants.DAV_PROPERTIES_CREATION_DATE, fa.getCreation().getDate());
            props.put(CrossContextConstants.DAV_PROPERTIES_GET_LAST_MODIFIED, fa.getModification().getDate());
            props.put(CrossContextConstants.DAV_PROPERTIES_GET_CONTENT_LENGTH, fa.getFileItem().getLength());
            props.put(CrossContextConstants.DAV_PROPERTIES_GET_CONTENT_TYPE, getMimeTypes().getContentType(fa.getFileItem().getName()));
            FileAttachment.FileLock lock = fa.getFileLock();
            if (lock != null) {
                props.put(CrossContextConstants.LOCK_PROPERTIES_ID, lock.getId());
                props.put(CrossContextConstants.LOCK_PROPERTIES_SUBJECT, lock.getSubject());
                props.put(CrossContextConstants.LOCK_PROPERTIES_EXPIRATION_DATE, lock.getExpirationDate());
                props.put(CrossContextConstants.LOCK_PROPERTIES_OWNER_INFO, lock.getOwnerInfo());
            }
            return props;
        } else {
            props.put(CrossContextConstants.OBJECT_INFO, CrossContextConstants.OBJECT_INFO_DIRECTORY);
        }
        Entry entry = (Entry) objMap.get(ENTRY);
        if (entry != null) {
            props.put(CrossContextConstants.DAV_PROPERTIES_CREATION_DATE, entry.getCreation().getDate());
            props.put(CrossContextConstants.DAV_PROPERTIES_GET_LAST_MODIFIED, entry.getModification().getDate());
            return props;
        }
        Binder binder = (Binder) objMap.get(BINDER);
        if (binder != null) {
            props.put(CrossContextConstants.DAV_PROPERTIES_CREATION_DATE, binder.getCreation().getDate());
            props.put(CrossContextConstants.DAV_PROPERTIES_GET_LAST_MODIFIED, binder.getModification().getDate());
            return props;
        }
        props.put(CrossContextConstants.DAV_PROPERTIES_CREATION_DATE, new Date(0));
        props.put(CrossContextConstants.DAV_PROPERTIES_GET_LAST_MODIFIED, new Date());
        return props;
    }

    public void lockResource(Map uri, String lockId, String lockSubject, Date lockExpirationDate, String lockOwnerInfo) throws NoAccessException, NoSuchObjectException, LockException, TypeMismatchException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        Entry entry = (Entry) objMap.get(ENTRY);
        try {
            AccessUtils.modifyCheck(entry);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        }
        try {
            bs.getFileModule().lock(((Binder) objMap.get(BINDER)), entry, ((FileAttachment) objMap.get(FILE_ATTACHMENT)), lockId, lockSubject, lockExpirationDate, lockOwnerInfo);
        } catch (ReservedByAnotherUserException e) {
            throw new LockException(e.getLocalizedMessage());
        } catch (LockedByAnotherUserException e) {
            throw new LockException(e.getLocalizedMessage());
        } catch (LockIdMismatchException e) {
            throw new LockException(e.getLocalizedMessage());
        }
    }

    public void unlockResource(Map uri, String lockId) throws NoAccessException, NoSuchObjectException, TypeMismatchException {
        Map objMap = new HashMap();
        if (!objectExists(uri, objMap)) throw new NoSuchObjectException("The resource does not exist");
        Entry entry = (Entry) objMap.get(ENTRY);
        bs.getFileModule().unlock(((Binder) objMap.get(BINDER)), entry, ((FileAttachment) objMap.get(FILE_ATTACHMENT)), lockId);
    }

    public void copyObject(Map sourceUri, Map targetUri, boolean overwrite, boolean recursive) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        throw new UnsupportedOperationException("(copyObject) " + getOriginal(sourceUri) + " to " + getOriginal(targetUri));
    }

    public void moveObject(Map sourceUri, Map targetUri, boolean overwrite) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        throw new UnsupportedOperationException("(moveObject) " + getOriginal(sourceUri) + " to " + getOriginal(targetUri));
    }

    private void removeResourceInternal(Map uri, Map objMap) throws NoAccessException {
        FileAttachment fa = (FileAttachment) objMap.get(FILE_ATTACHMENT);
        List faId = new ArrayList();
        faId.add(fa.getId());
        try {
            bs.getFolderModule().modifyEntry(getBinderId(uri), getEntryId(uri), new EmptyInputData(), null, faId, null, null);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        } catch (WriteFilesException e) {
            throw new KablinkFileSystemException(e.getLocalizedMessage());
        } catch (WriteEntryDataException e) {
            throw new KablinkFileSystemException(e.getLocalizedMessage());
        }
    }

    private String[] getChildrenNamesInternal(Map uri, Map objMap) throws NoAccessException, NoSuchObjectException {
        Binder binder = (Binder) objMap.get(BINDER);
        if (binder == null) {
            Map options = new HashMap();
            options.put(ObjectKeys.SEARCH_MAX_HITS, Integer.MAX_VALUE - 1);
            Document searchFilter = DocumentHelper.createDocument();
            Element field = searchFilter.addElement(Constants.FIELD_ELEMENT);
            field.addAttribute(Constants.FIELD_NAME_ATTRIBUTE, Constants.DOC_TYPE_FIELD);
            Element child = field.addElement(Constants.FIELD_TERMS_ELEMENT);
            child.setText(Constants.DOC_TYPE_BINDER);
            options.put(ObjectKeys.SEARCH_FILTER_AND, searchFilter);
            Map searchResults = bs.getBinderModule().executeSearchQuery(null, options);
            List<Map> groups = (List) searchResults.get(ObjectKeys.SEARCH_ENTRIES);
            List<String> folderIds = new ArrayList();
            for (Map groupMap : groups) {
                String fId = (String) groupMap.get(Constants.DOCID_FIELD);
                if (Validator.isNotNull(fId)) folderIds.add(fId);
            }
            return folderIds.toArray(new String[folderIds.size()]);
        }
        Set<String> children = new HashSet<String>();
        Entry entry = (Entry) objMap.get(ENTRY);
        if (entry == null) {
            if (EntityType.folder.equals(binder.getEntityType())) {
                Map options = new HashMap();
                options.put(ObjectKeys.SEARCH_MAX_HITS, Integer.MAX_VALUE);
                Map folderEntries = bs.getFolderModule().getEntries(binder.getId(), options);
                List entries = (ArrayList) folderEntries.get(ObjectKeys.SEARCH_ENTRIES);
                for (int i = 0; i < entries.size(); i++) {
                    Map ent = (Map) entries.get(i);
                    String entryIdString = (String) ent.get(Constants.DOCID_FIELD);
                    if (Validator.isNotNull(entryIdString)) children.add(entryIdString);
                }
            }
            return children.toArray(new String[children.size()]);
        }
        String itemType = getItemType(uri);
        if (itemType == null) {
            Document def = entry.getEntryDefDoc();
            if (def != null) {
                Element root = def.getRootElement();
                if (root.selectNodes("//item[@name='fileEntryTitle' and @type='data']").size() > 0) children.add(CrossContextConstants.URI_ITEM_TYPE_LIBRARY);
                if (root.selectNodes("//item[@name='file' and @type='data']").size() > 0) children.add(CrossContextConstants.URI_ITEM_TYPE_FILE);
                if (root.selectNodes("//item[@name='graphic' and @type='data']").size() > 0) children.add(CrossContextConstants.URI_ITEM_TYPE_GRAPHIC);
                if (root.selectNodes("//item[@name='attachFiles' and @type='data']").size() > 0) children.add(CrossContextConstants.URI_ITEM_TYPE_ATTACH);
            }
            return children.toArray(new String[children.size()]);
        }
        if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_LIBRARY)) {
            if (getFilePath(uri) == null) {
                return new String[] { entry.getTitle() };
            } else {
                return null;
            }
        } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_FILE) || itemType.equals(CrossContextConstants.URI_ITEM_TYPE_GRAPHIC)) {
            if (getElemName(uri) == null) {
                Document def = (Document) objMap.get(DEFINITION);
                Element root = def.getRootElement();
                List items = root.selectNodes("//item[@name='" + toDefItemType(itemType) + "' and @type='data']");
                for (int i = 0; i < items.size(); i++) {
                    Element item = (Element) items.get(i);
                    Element nameProperty = (Element) item.selectSingleNode("./properties/property[@name='name']");
                    if (nameProperty != null) {
                        String nameValue = nameProperty.attributeValue("value");
                        if (nameValue != null && !nameValue.equals("")) {
                            children.add(nameValue);
                        }
                    }
                }
                return children.toArray(new String[children.size()]);
            }
            if (getFilePath(uri) == null) {
                CustomAttribute ca = entry.getCustomAttribute((String) objMap.get(ELEMENT_NAME));
                if (ca != null) {
                    Iterator it = ((Set) ca.getValueSet()).iterator();
                    while (it.hasNext()) {
                        FileAttachment fa = (FileAttachment) it.next();
                        children.add(fa.getFileItem().getName());
                    }
                    return children.toArray(new String[children.size()]);
                } else {
                    return new String[0];
                }
            } else {
                return null;
            }
        } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_ATTACH)) {
            if (getReposName(uri) == null) {
                Iterator it = entry.getFileAttachments().iterator();
                while (it.hasNext()) {
                    FileAttachment fa = (FileAttachment) it.next();
                    children.add(fa.getRepositoryName());
                }
                return children.toArray(new String[children.size()]);
            }
            if (getFilePath(uri) == null) {
                Iterator it = entry.getFileAttachments(getReposName(uri)).iterator();
                while (it.hasNext()) {
                    FileAttachment fa = (FileAttachment) it.next();
                    children.add(fa.getFileItem().getName());
                }
                return children.toArray(new String[children.size()]);
            } else {
                return null;
            }
        } else {
            throw new InternalException();
        }
    }

    private String getOriginal(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_ORIGINAL);
    }

    private Long getBinderId(Map uri) {
        return (Long) uri.get(CrossContextConstants.URI_BINDER_ID);
    }

    private String getFilePath(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_FILEPATH);
    }

    private Long getEntryId(Map uri) {
        return (Long) uri.get(CrossContextConstants.URI_ENTRY_ID);
    }

    private String getItemType(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_ITEM_TYPE);
    }

    private String getElemName(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_ELEMNAME);
    }

    private String getReposName(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_REPOS_NAME);
    }

    private String toDefItemType(String itemType) {
        if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_LIBRARY)) {
            return "fileEntryTitle";
        } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_FILE)) {
            return "file";
        } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_GRAPHIC)) {
            return "graphic";
        } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_ATTACH)) {
            return "attachFiles";
        } else {
            return null;
        }
    }

    /**
	 * Write the file. If the file already exists, this will create a new 
	 * version of the file. Otherwise it will create a new file with initial
	 * version.  
	 * 
	 * @param uri
	 * @param objMap
	 * @param in
	 */
    private void writeResourceInternal(Map uri, Map objMap, InputStream in) throws NoAccessException {
        DatedMultipartFile mf = new DatedMultipartFile(getFilePath(uri), in);
        Map fileItems = new HashMap();
        InputDataAccessor inputData;
        if (getItemType(uri).equals(CrossContextConstants.URI_ITEM_TYPE_ATTACH)) {
            fileItems.put((String) objMap.get(ELEMENT_NAME) + "1", mf);
            Map source = new HashMap();
            source.put((String) objMap.get(ELEMENT_NAME) + "_repos1", getReposName(uri));
            inputData = new MapInputData(source);
        } else {
            fileItems.put(objMap.get(ELEMENT_NAME), mf);
            inputData = new EmptyInputData();
        }
        Entry entry = (Entry) objMap.get(ENTRY);
        try {
            AccessUtils.modifyCheck(entry);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        }
        try {
            bs.getFolderModule().modifyEntry(getBinderId(uri), getEntryId(uri), inputData, fileItems, null, null, null);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        } catch (WriteFilesException e) {
            throw new KablinkFileSystemException(e.getLocalizedMessage(), true);
        } catch (WriteEntryDataException e) {
            throw new KablinkFileSystemException(e.getLocalizedMessage(), true);
        }
    }

    private boolean objectExists(Map uri, Map objMap) throws NoAccessException {
        try {
            Long binderId = getBinderId(uri);
            if (binderId == null) return true;
            Binder binder = bs.getBinderModule().getBinder(binderId);
            objMap.put(BINDER, binder);
            Long entryId = getEntryId(uri);
            if (entryId == null) return true;
            Entry entry = null;
            if (binder instanceof Folder) entry = bs.getFolderModule().getEntry(binderId, entryId); else entry = bs.getProfileModule().getEntry(entryId);
            objMap.put(ENTRY, entry);
            String itemType = getItemType(uri);
            if (itemType == null) return true;
            Document def = entry.getEntryDefDoc();
            if (def == null) return false;
            objMap.put(DEFINITION, def);
            String defItemType = toDefItemType(itemType);
            if (defItemType == null) return false;
            Element root = def.getRootElement();
            List items = root.selectNodes("//item[@name='" + defItemType + "' and @type='data']");
            if (items.size() == 0) return false;
            String elementName = null;
            String reposName = null;
            if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_FILE) || itemType.equals(CrossContextConstants.URI_ITEM_TYPE_GRAPHIC)) {
                elementName = getElemName(uri);
                if (elementName == null) return true;
                boolean matchFound = false;
                Iterator itItems = items.listIterator();
                while (itItems.hasNext()) {
                    Element item = (Element) itItems.next();
                    Element nameProperty = (Element) item.selectSingleNode("./properties/property[@name='name']");
                    if (nameProperty != null) {
                        String nameValue = nameProperty.attributeValue("value");
                        if (nameValue != null && nameValue.equals(elementName)) {
                            matchFound = true;
                            objMap.put(ELEMENT_NAME, elementName);
                            break;
                        }
                    }
                }
                if (!matchFound) return false;
            } else if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_ATTACH)) {
                reposName = getReposName(uri);
                if (reposName == null) return true;
                Element attachFilesItem = (Element) items.get(0);
                Element nameProperty = (Element) attachFilesItem.selectSingleNode("./properties/property[@name='name']");
                elementName = nameProperty.attributeValue("value");
                objMap.put(ELEMENT_NAME, elementName);
            } else {
                Element primaryItem = (Element) items.get(0);
                Element nameProperty = (Element) primaryItem.selectSingleNode("./properties/property[@name='name']");
                elementName = nameProperty.attributeValue("value");
                objMap.put(ELEMENT_NAME, elementName);
            }
            String filePath = getFilePath(uri);
            if (filePath == null) return true;
            if (itemType.equals(CrossContextConstants.URI_ITEM_TYPE_ATTACH)) {
                FileAttachment fa = entry.getFileAttachment(filePath);
                if (fa == null) return false; else {
                    objMap.put(FILE_ATTACHMENT, fa);
                    return true;
                }
            } else {
                CustomAttribute ca = entry.getCustomAttribute(elementName);
                if (ca == null) {
                    return false;
                } else {
                    Iterator it = ((Set) ca.getValueSet()).iterator();
                    while (it.hasNext()) {
                        FileAttachment fa = (FileAttachment) it.next();
                        if (fa.getFileItem().getName().equals(filePath)) {
                            objMap.put(FILE_ATTACHMENT, fa);
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (NoBinderByTheIdException e) {
            return false;
        } catch (NoFolderByTheIdException e) {
            return false;
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        }
    }
}
