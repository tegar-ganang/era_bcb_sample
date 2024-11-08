package com.sitescape.team.ssfs.server.impl;

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
import org.dom4j.Element;
import com.sitescape.team.InternalException;
import com.sitescape.team.ObjectKeys;
import com.sitescape.team.domain.Binder;
import com.sitescape.team.domain.CustomAttribute;
import com.sitescape.team.domain.Entry;
import com.sitescape.team.domain.FileAttachment;
import com.sitescape.team.domain.Folder;
import com.sitescape.team.domain.NoBinderByTheIdException;
import com.sitescape.team.domain.NoFolderByTheIdException;
import com.sitescape.team.domain.ReservedByAnotherUserException;
import com.sitescape.team.module.binder.AccessUtils;
import com.sitescape.team.module.file.LockIdMismatchException;
import com.sitescape.team.module.file.LockedByAnotherUserException;
import com.sitescape.team.module.file.WriteFilesException;
import com.sitescape.team.module.shared.EmptyInputData;
import com.sitescape.team.module.shared.EntityIndexUtils;
import com.sitescape.team.module.shared.InputDataAccessor;
import com.sitescape.team.module.shared.MapInputData;
import com.sitescape.team.security.AccessControlException;
import com.sitescape.team.ssfs.AlreadyExistsException;
import com.sitescape.team.ssfs.CrossContextConstants;
import com.sitescape.team.ssfs.LockException;
import com.sitescape.team.ssfs.NoAccessException;
import com.sitescape.team.ssfs.NoSuchObjectException;
import com.sitescape.team.ssfs.TypeMismatchException;
import com.sitescape.team.ssfs.server.SiteScapeFileSystem;
import com.sitescape.team.ssfs.server.SiteScapeFileSystemException;
import com.sitescape.team.util.AllBusinessServicesInjected;
import com.sitescape.team.util.DatedMultipartFile;

public class SiteScapeFileSystemInternal implements SiteScapeFileSystem {

    private static final String BINDER = "b";

    private static final String ENTRY = "e";

    private static final String DEFINITION = "d";

    private static final String FILE_ATTACHMENT = "fa";

    private static final String ELEMENT_NAME = "en";

    private AllBusinessServicesInjected bs;

    private FileTypeMap mimeTypes;

    SiteScapeFileSystemInternal(AllBusinessServicesInjected bs) {
        this.bs = bs;
    }

    protected FileTypeMap getMimeTypes() {
        return this.mimeTypes;
    }

    public void setMimeTypes(FileTypeMap mimeTypes) {
        this.mimeTypes = mimeTypes;
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
        throw new UnsupportedOperationException();
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
        try {
            AccessUtils.modifyCheck(entry);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        }
        bs.getFileModule().unlock(((Binder) objMap.get(BINDER)), entry, ((FileAttachment) objMap.get(FILE_ATTACHMENT)), lockId);
    }

    public void copyObject(Map sourceUri, Map targetUri, boolean overwrite, boolean recursive) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        throw new UnsupportedOperationException();
    }

    public void moveObject(Map sourceUri, Map targetUri, boolean overwrite) throws NoAccessException, NoSuchObjectException, AlreadyExistsException, TypeMismatchException {
        throw new UnsupportedOperationException();
    }

    private void removeResourceInternal(Map uri, Map objMap) throws NoAccessException {
        FileAttachment fa = (FileAttachment) objMap.get(FILE_ATTACHMENT);
        List faId = new ArrayList();
        faId.add(fa.getId());
        try {
            bs.getFolderModule().modifyEntry(getBinderId(uri), getEntryId(uri), new EmptyInputData(), null, faId, null);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        } catch (WriteFilesException e) {
            throw new SiteScapeFileSystemException(e.getMessage());
        }
    }

    private String[] getChildrenNamesInternal(Map uri, Map objMap) throws NoAccessException, NoSuchObjectException {
        Binder binder = (Binder) objMap.get(BINDER);
        if (binder == null) {
            List<String> folderIds = bs.getFolderModule().getFolderIds(null);
            return folderIds.toArray(new String[folderIds.size()]);
        }
        Set<String> children = new HashSet<String>();
        Entry entry = (Entry) objMap.get(ENTRY);
        if (entry == null) {
            Map options = new HashMap();
            options.put(ObjectKeys.SEARCH_MAX_HITS, Integer.MAX_VALUE);
            Map folderEntries = bs.getFolderModule().getEntries(binder.getId(), options);
            List entries = (ArrayList) folderEntries.get(ObjectKeys.SEARCH_ENTRIES);
            for (int i = 0; i < entries.size(); i++) {
                Map ent = (Map) entries.get(i);
                String entryIdString = (String) ent.get(EntityIndexUtils.DOCID_FIELD);
                if (entryIdString != null && !entryIdString.equals("")) children.add(entryIdString);
            }
            return children.toArray(new String[children.size()]);
        }
        String itemType = getItemType(uri);
        if (itemType == null) {
            Document def = entry.getEntryDef().getDefinition();
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

    private String getZoneName(Map uri) {
        return (String) uri.get(CrossContextConstants.URI_ZONENAME);
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
        try {
            bs.getFolderModule().modifyEntry(getBinderId(uri), getEntryId(uri), inputData, fileItems, null, null);
        } catch (AccessControlException e) {
            throw new NoAccessException(e.getLocalizedMessage());
        } catch (WriteFilesException e) {
            throw new SiteScapeFileSystemException(e.getMessage());
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
            if (binder instanceof Folder) entry = bs.getFolderModule().getEntry(binderId, entryId); else entry = bs.getProfileModule().getEntry(binderId, entryId);
            objMap.put(ENTRY, entry);
            String itemType = getItemType(uri);
            if (itemType == null) return true;
            Document def = entry.getEntryDef().getDefinition();
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
