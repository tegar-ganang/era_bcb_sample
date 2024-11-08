package com.germinus.xpression.cms.directory;

import com.germinus.util.UniTuple;
import com.germinus.xpression.cms.CMSRuntimeException;
import com.germinus.xpression.cms.CmsConfig;
import com.germinus.xpression.cms.Location;
import com.germinus.xpression.cms.cache.NeedsRefreshException;
import com.germinus.xpression.cms.cache.ScribeCache;
import com.germinus.xpression.cms.contents.Content;
import com.germinus.xpression.cms.contents.ContentManager;
import com.germinus.xpression.cms.contents.ContentNotFoundException;
import com.germinus.xpression.cms.contents.ContentsUtil;
import com.germinus.xpression.cms.contents.MalformedContentException;
import com.germinus.xpression.cms.contents.binary.BinaryDataReference;
import com.germinus.xpression.cms.jcr.DirectoryFileNode;
import com.germinus.xpression.cms.jcr.DirectoryFolderNode;
import com.germinus.xpression.cms.jcr.DirectoryItemNode;
import com.germinus.xpression.cms.jcr.JCRLocation;
import com.germinus.xpression.cms.jcr.JCRManagerRegistry;
import com.germinus.xpression.cms.jcr.JCRUtil;
import com.germinus.xpression.cms.util.ManagerRegistry;
import com.germinus.xpression.cms.util.Performance;
import com.germinus.xpression.image.ImageDimensions;
import com.germinus.xpression.image.ImageUtils;
import com.germinus.xpression.image.NotSupportedException;
import java.util.Arrays;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.activation.MimetypesFileTypeMap;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

/**
 * Class to create and manage Elements in the Directory Workspace
 * Version 1.0
 * @author <a href="mailto:gruiz@germinus.com">Gonzalo Ruiz</a>
 *         Date: 5-april-2006
 */
public class JCRDirectoryPersister implements DirectoryPersister {

    private static final String NAME_ATTRIBUTE = "name";

    private static final MessageFormat descendantNameQueryMessageFormat = new MessageFormat("{0}/*/" + JCRUtil.JCR_CONTENT_PREFIX + "[@" + NAME_ATTRIBUTE + "=''{1}'']");

    private static final MessageFormat nodePathForQueryMessageFormat = new MessageFormat("//*[@jcr:uuid = ''{0}'']");

    private static enum ResizeType {

        thumbnail, medium;

        public String getJCRPrefix() {
            if (this == thumbnail) return JCRUtil.JCR_THUMBNAIL_PREFIX; else return JCRUtil.JCR_MEDIUM_SIZE_PREFIX;
        }
    }

    static Log log = LogFactory.getLog(JCRDirectoryPersister.class);

    private ScribeCache cacheAdmin;

    private FolderCachedSearcherStrategy folderCachedSearcherStrategy;

    private SearchByURLPathAttribtueStrategy searchByURLPathAttribtueStrategy;

    private SimpleSearcherStrategy simpleSearcherStrategy;

    /**
     * Add in a ubiquitous way, a rootFolder below a Node.
     * RootFolder and tree below this Node don´t depend of nothing
     * above this RootFolderNode
     *
     * @param location
     * @param itemFolder
     * @return
     * @throws CMSRuntimeException
     */
    public DirectoryFolder addRootFolder(Location location, DirectoryFolder itemFolder) throws CMSRuntimeException {
        return addFolder(((JCRLocation) location).getNode(), itemFolder, JCRUtil.DIRECTORY_ROOT_PREFIX, null);
    }

    public void updateRecursiveFolderSize(DirectoryFolder folder, long incSize) throws CMSRuntimeException, ItemNotFoundException, AccessDeniedException, RepositoryException {
        Node folderNode = getNodeFromItem(folder);
        updateFolderSize(folder, incSize);
        Node parentNode;
        while (!(parentNode = folderNode.getParent()).getPath().equals("/")) {
            if (parentNode.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                try {
                    updateFolderSize((DirectoryFolder) getItemFromNode(parentNode), incSize);
                } catch (MalformedDirectoryItemException e) {
                    throw new CMSRuntimeException(e);
                }
            }
            folderNode = parentNode;
        }
    }

    public DirectoryFolder addRootFolder(Location location, DirectoryFolder itemFolder, String prefix) throws CMSRuntimeException {
        return addFolder(((JCRLocation) location).getNode(), itemFolder, prefix, null);
    }

    public DirectoryFolder addFolder(DirectoryFolder parentFolder, DirectoryFolder childFolder) throws CMSRuntimeException {
        return addFolder(getNodeFromItem(parentFolder), childFolder, JCRUtil.FOLDER_PREFIX, parentFolder.getRootFolderId());
    }

    public DirectoryFolder addUniqueFolder(DirectoryFolder parentFolder, DirectoryFolder childFolder) throws CMSRuntimeException, DuplicatedFolderNameException {
        try {
            if (!folderExists(parentFolder, childFolder.getName())) {
                return addFolder(getNodeFromItem(parentFolder), childFolder, JCRUtil.FOLDER_PREFIX, parentFolder.getRootFolderId());
            } else {
                throw new DuplicatedFolderNameException("A folder with name " + childFolder.getName() + " already exists");
            }
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private DirectoryFolder addFolder(Node parentFolderNode, DirectoryFolder childFolder, String prefix, String rootFolderId) throws CMSRuntimeException {
        try {
            Node childFolderNode = parentFolderNode.addNode(prefix, JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER);
            childFolderNode.addMixin(JCRUtil.MIX_REFERENCEABLE);
            childFolderNode.addMixin(JCRUtil.MIX_LOCKABLE);
            if (rootFolderId == null) {
                childFolder.setRootFolderId(childFolderNode.getIdentifier());
            } else {
                childFolder.setRootFolderId(rootFolderId);
            }
            Node contentNode = childFolderNode.addNode(JCRUtil.JCR_CONTENT_PREFIX);
            JCRUtil.fillContentNode(childFolder, contentNode);
            DirectoryFolder itemFromNode;
            try {
                itemFromNode = (DirectoryFolder) getItemFromNode(childFolderNode);
                return itemFromNode;
            } catch (MalformedDirectoryItemException e) {
                throw new CMSRuntimeException("Folder has not been correctly created", e);
            }
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public void resizeTo(DirectoryFile file, long edgeWidth, long edgeHeight) throws CMSRuntimeException {
        InputStream is;
        InputStream resizeInputStream;
        Node fileNode;
        try {
            resizeInputStream = getInputStreamFromFile(file);
            is = ImageUtils.resizeImage(resizeInputStream, edgeWidth, edgeHeight);
            fileNode = getNodeFromItem(file).getNode(JCRUtil.JCR_FILE_PREFIX);
            JCRUtil.modifyFileContent(is, fileNode);
        } catch (MalformedDirectoryItemException e) {
            String errorMessage = "File has not been correctly readed: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (IOException e) {
            String errorMessage = "Error resizing the image: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (NotSupportedException e) {
            String errorMessage = "Error not supported by the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (PathNotFoundException e) {
            String errorMessage = "Error accessing the file, path not found: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public DirectoryFile addFile(DirectoryFolder parentFolder, DirectoryFile itemFile, InputStream inputStream, long size) throws CMSRuntimeException {
        try {
            Node parentNode = getNodeFromItem(parentFolder);
            Node itemNode = parentNode.addNode(JCRUtil.FILE_PREFIX, JCRUtil.NODETYPE_DIRECTORY_ITEM_FILE);
            itemNode.addMixin(JCRUtil.MIX_REFERENCEABLE);
            String encoding = itemFile.getEncoding();
            String mimeType = itemFile.getMimeType();
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTime(itemFile.getLastModificationDate());
            JCRUtil.addFileNode(JCRUtil.JCR_FILE_PREFIX, inputStream, itemNode, encoding, mimeType, lastModified);
            if (itemFile.isImage()) {
                try {
                    ImageDimensions dimensions = obtainDimensions(getInputStreamFromNode(itemNode));
                    itemFile.setDimensions(dimensions);
                } catch (NullPointerException e) {
                    log.error("Error obtainDimensions image dimensions", e);
                } catch (IOException e) {
                    log.error("Error setting image dimensions", e);
                }
                ImageDimensions thumbnailDimensions = saveResizedImage(getInputStreamFromNode(itemNode), ResizeType.thumbnail, itemFile.isPNGOrGIF(), itemNode);
                ImageDimensions mediumSizeDimensions = saveResizedImage(getInputStreamFromNode(itemNode), ResizeType.medium, itemFile.isPNGOrGIF(), itemNode);
                itemFile.setThumbnailDimensions(thumbnailDimensions);
                itemFile.setMediumDimensions(mediumSizeDimensions);
            }
            String rootFolderId = parentNode.getIdentifier();
            itemFile.setRootFolderId(rootFolderId);
            itemFile.setSize(size);
            Node contentNode = itemNode.addNode(JCRUtil.JCR_CONTENT_PREFIX);
            JCRUtil.fillContentNode(itemFile, contentNode);
            contentNode.setProperty("rootFolderId", rootFolderId);
            DirectoryFile itemFromNode = (DirectoryFile) getItemFromNode(itemNode);
            try {
                updateRecursiveFolderSize(parentFolder, size);
            } catch (Throwable e) {
                log.warn("Cannot update folder [" + parentFolder.getId() + "] size", e);
            }
            return itemFromNode;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            throw new CMSRuntimeException("File has not been correctly created", e);
        }
    }

    @Override
    public DirectoryFile addOrOverrideFile(DirectoryFolder parentFolder, DirectoryFile itemFile, InputStream inputStream, long size) throws CMSRuntimeException {
        if (fileNameExists(parentFolder, itemFile.getName())) {
            try {
                return overwriteFile(parentFolder, inputStream, itemFile.getName(), size);
            } catch (DirectoryItemNotFoundException e) {
                throw new CMSRuntimeException("item not found but file name exists");
            } catch (FileNotFoundException e) {
                throw new CMSRuntimeException("item not found but file name exists");
            }
        } else {
            return addFile(parentFolder, itemFile, inputStream, size);
        }
    }

    @Override
    public void optimizeImages(DirectoryFile directoryFile) {
        Node node = getNodeFromItem(directoryFile);
        try {
            if (directoryFile.isImage()) {
                if (!hasThumbnailProperty(node) && hasThumbnailNode(node)) {
                    doUpdateImage(directoryFile, node, ResizeType.thumbnail);
                }
                if (!hasMediumSizeImageProperty(node)) {
                    saveResizedImage(directoryFile, ResizeType.medium);
                }
                if (!hasHeightProperty(node)) {
                    updateFile(directoryFile);
                }
            }
        } catch (RepositoryException e) {
            log.error("Cannot update thumbnail data", e);
        } catch (MalformedDirectoryItemException e) {
            log.error("Cannot create mediumSize data", e);
        }
    }

    private boolean hasHeightProperty(Node node) {
        try {
            return node.getNode(JCRUtil.JCR_CONTENT_PREFIX).hasProperty("height");
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
    }

    private void doUpdateImage(DirectoryFile directoryFile, Node node, ResizeType resizeType) {
        try {
            byte[] resizedImageData = getFileDataFromNode(resizeType.getJCRPrefix(), node);
            saveResizedImage(new ByteArrayInputStream(resizedImageData), resizeType, directoryFile.isPNGOrGIF(), node);
            log.info("Updated thumbnail for: " + directoryFile.getName());
        } catch (IOException e) {
            log.error("Cannot update thumbnail data", e);
        } catch (MalformedDirectoryItemException e) {
            log.error("Cannot update thumbnail data", e);
        }
    }

    private ImageDimensions saveResizedImage(DirectoryFile itemFile, ResizeType resizeType) throws MalformedDirectoryItemException {
        InputStream savedInputStream = getInputStreamFromFile(itemFile);
        return saveResizedImage(itemFile, savedInputStream, resizeType);
    }

    private ImageDimensions saveResizedImage(DirectoryFile itemFile, InputStream savedInputStream, ResizeType resizeType) {
        Node itemNode = getNodeFromItem(itemFile);
        boolean isPNGOrGIF = itemFile.isPNGOrGIF();
        return saveResizedImage(savedInputStream, resizeType, isPNGOrGIF, itemNode);
    }

    private ImageDimensions saveResizedImage(InputStream savedInputStream, ResizeType resizeType, boolean isPNGOrGIF, Node itemNode) {
        try {
            InputStream inputResizedImage = scaleImage(savedInputStream, resizeType, isPNGOrGIF);
            if (log.isDebugEnabled()) log.debug("Scaled original");
            try {
                boolean isThumbnail = resizeType == ResizeType.thumbnail;
                String propertyValue = resizeType.getJCRPrefix();
                if ((isThumbnail && hasThumbnailNode(itemNode))) {
                    itemNode.getNode(propertyValue).remove();
                }
                JCRUtil.setBinaryProperty(itemNode, propertyValue, inputResizedImage);
                log.info((isThumbnail ? "Thumbnail" : "Medium") + " image saved.");
                ImageDimensions imageDimensions = obtainDimensions(JCRUtil.getBinaryProperty(itemNode, propertyValue));
                return imageDimensions;
            } catch (RepositoryException e) {
                log.error("Error saving the thumbnail image in repository: ", e);
                return ImageDimensions.NULL;
            }
        } catch (Exception e) {
            log.error("Error scaling the image, the resized image will not be saved ", e);
            return ImageDimensions.NULL;
        }
    }

    private ImageDimensions obtainDimensions(InputStream imageInputStream) throws MalformedDirectoryItemException, IOException {
        Image image = ImageUtils.buildImage(imageInputStream);
        ImageDimensions imageDimensions = new ImageDimensions(image);
        return imageDimensions;
    }

    private InputStream scaleImage(InputStream originalInputStream, ResizeType resizeType, boolean isPNGOrGIF) throws IOException, NotSupportedException {
        InputStream inputResizedImage = null;
        long edgeLength = resizeType == ResizeType.thumbnail ? CmsConfig.getEdgeLengthThumbnail() : CmsConfig.getMediumSizeEdgeLength();
        inputResizedImage = ImageUtils.scaleImage(originalInputStream, edgeLength, isPNGOrGIF);
        return inputResizedImage;
    }

    private void updateFolderSize(DirectoryFolder folder, long incSize) throws CMSRuntimeException {
        Node folderNode = getNodeFromItem(folder);
        try {
            Node parentPropertiesNode = folderNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            long newSize = folder.getSize() + incSize;
            if (newSize < 0) newSize = 0;
            parentPropertiesNode.setProperty("size", newSize);
            folder.setSize(newSize);
            flushItem(folder);
            putInCache(folder.getId(), folder);
        } catch (RepositoryException e) {
            String errorMessage = "Error updating Folder size property: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private void flushItem(DirectoryItem item) {
        getCacheAdmin().flushEntry(item.getId());
    }

    public long recalculateFolderSize(DirectoryFolder folder) {
        long size = 0;
        Iterator<DirectoryFile> filesIterator = listFiles(folder).iterator();
        Iterator<DirectoryFolder> foldersIterator = listFolders(folder).iterator();
        while (filesIterator.hasNext()) {
            DirectoryFile file = filesIterator.next();
            long fileSize = file.getSize();
            if (fileSize > 0) {
                size = size + file.getSize();
            } else {
                try {
                    size = size + recalculateFileSize(file);
                } catch (MalformedDirectoryItemException e) {
                    log.error("Cannot recalculate size of file" + file.getId() + " File is malformed");
                }
            }
        }
        while (foldersIterator.hasNext()) {
            DirectoryFolder childFolder = (DirectoryFolder) foldersIterator.next();
            size = size + recalculateFolderSize(childFolder);
        }
        return size;
    }

    private long recalculateFileSize(DirectoryFile file) throws MalformedDirectoryItemException {
        try {
            byte[] fileBytes = getFileDataFromFile(file);
            return fileBytes.length;
        } catch (IOException e) {
            String errorMessage = "Error recalculating File size property: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public Collection<DirectoryFolder> getDirectoriesByLocation(Location location) throws RepositoryException {
        Node locationNode = ((JCRLocation) location).getNode();
        NodeIterator directoriesNodes = locationNode.getNodes();
        Collection<DirectoryFolder> directoriesList = new ArrayList<DirectoryFolder>();
        while (directoriesNodes.hasNext()) {
            Node rootFolderNode;
            if ((rootFolderNode = directoriesNodes.nextNode()).getPrimaryNodeType().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                try {
                    DirectoryItem itemFromNode = getItemFromNode(rootFolderNode);
                    if (itemFromNode instanceof DirectoryFolder) {
                        DirectoryFolder directoryFolder = (DirectoryFolder) itemFromNode;
                        directoriesList.add(directoryFolder);
                    }
                } catch (MalformedDirectoryItemException e) {
                    log.error("Malformed directory: " + rootFolderNode.getIdentifier());
                }
            }
        }
        return directoriesList;
    }

    public List<DirectoryFolder> listFolders(DirectoryFolder parentFolder) throws CMSRuntimeException {
        try {
            Node parentNode = getNodeFromItem(parentFolder);
            NodeIterator nodeChildren = parentNode.getNodes();
            List<DirectoryFolder> items = new ArrayList<DirectoryFolder>();
            while (nodeChildren.hasNext()) {
                Node itemNode = nodeChildren.nextNode();
                if (itemNode.getPrimaryNodeType().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                    try {
                        items.add((DirectoryFolder) getItemFromNode(itemNode));
                    } catch (MalformedDirectoryItemException e) {
                        log.error("Malformed folder" + itemNode.getIdentifier());
                    } catch (CMSRuntimeException e) {
                        log.error("Error getting DirectoryItem from node..." + e);
                    }
                }
            }
            return items;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    /**
     *
     * @param parentFolder
     * @param sortingProperty
     * @param sortingOrder
     * @return
     */
    public List<DirectoryFolderNode> listFoldersNodes(DirectoryFolder parentFolder, String sortingProperty, String sortingOrder) {
        try {
            if (log.isDebugEnabled()) log.debug("Listing folders of :" + parentFolder.getURLPath());
            Node parentNode = JCRUtil.getNodeById(parentFolder.getId(), parentFolder.getWorkspace());
            NodeIterator nodeChildren = parentNode.getNodes(JCRUtil.FOLDER_PREFIX);
            List<DirectoryFolderNode> directoryFolderNodes = new ArrayList<DirectoryFolderNode>();
            while (nodeChildren.hasNext()) {
                Node itemNode = (Node) nodeChildren.next();
                directoryFolderNodes.add(new DirectoryFolderNode(itemNode));
            }
            return directoryFolderNodes;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public List<DirectoryFile> listFiles(DirectoryFolder parentFolder) throws CMSRuntimeException {
        return listFiles(parentFolder, null);
    }

    public List<DirectoryFile> listFiles(DirectoryFolder parentFolder, List<String> contentTypes) throws CMSRuntimeException {
        try {
            Node parentNode = getNodeFromItem(parentFolder);
            NodeIterator nodeChildren = parentNode.getNodes();
            List<DirectoryFile> items = new ArrayList<DirectoryFile>();
            while (nodeChildren.hasNext()) {
                Node itemNode = nodeChildren.nextNode();
                if (itemNode.getPrimaryNodeType().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FILE)) {
                    DirectoryFile file;
                    try {
                        file = (DirectoryFile) getItemFromNode(itemNode);
                        if ((contentTypes == null) || contentTypes.get(0) == null) items.add(file); else if (matchScribeContentType(contentTypes, file)) {
                            items.add(file);
                        }
                    } catch (MalformedDirectoryItemException e) {
                        log.error("Malformed file " + itemNode.getIdentifier());
                    }
                }
            }
            return items;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage + e);
        }
    }

    public boolean matchScribeContentType(List<String> contentTypes, DirectoryFile file) {
        String mimeType = file.getScribeMimeType();
        for (String contentType : contentTypes) {
            if (matchContentType(contentType, mimeType)) return true;
        }
        return false;
    }

    private boolean matchContentType(String contentType, String mimeType) {
        return ((mimeType != null) && (mimeType.contains(contentType)));
    }

    public List<DirectoryItem> listItems(DirectoryFolder parentFolder) throws CMSRuntimeException {
        try {
            if (log.isDebugEnabled()) log.debug("Listing child nodes in folder id: " + parentFolder.getId());
            Node parentNode = getNodeFromItem(parentFolder);
            NodeIterator nodeChildren = parentNode.getNodes();
            List<DirectoryItem> items = new ArrayList<DirectoryItem>();
            while (nodeChildren.hasNext()) {
                Node child = nodeChildren.nextNode();
                if (child.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM)) {
                    if (log.isDebugEnabled()) log.debug("Child node id: " + child.getIdentifier());
                    try {
                        items.add(getItemFromNode(child));
                    } catch (MalformedDirectoryItemException e) {
                        log.error("Malformed directory item " + child.getIdentifier());
                    }
                }
            }
            return items;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    @Deprecated
    public void deleteItem(String uuid) {
        UniTuple<Node> uniTuple = JCRUtil.findNodeIdInWorkspaces(uuid);
        if (uniTuple.isOk()) {
            try {
                deleteItem(getItemFromNode(uniTuple.getValue()));
            } catch (MalformedDirectoryItemException e) {
                log.error("MalformedDirectoryItemException when trying to delete node " + uuid);
                throw new CMSRuntimeException(e);
            }
        }
    }

    public void deleteItem(String uuid, String workspace) {
        if (log.isDebugEnabled()) log.debug("Removing the content by id: " + uuid);
        Session session = JCRUtil.currentSession(workspace);
        try {
            Node nodeToDelete = session.getNodeByIdentifier(uuid);
            Node parent = nodeToDelete.getParent();
            try {
                DirectoryFolder folder = (DirectoryFolder) getItemFromNode(parent);
                long nodeSize = getNodeSize(nodeToDelete);
                updateRecursiveFolderSize(folder, nodeSize * -1);
            } catch (Exception e) {
                log.warn("Error updating fodler size", e);
            }
            removeFromCache(nodeToDelete);
            nodeToDelete.remove();
            parent.getSession().save();
        } catch (RepositoryException e1) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e1);
            throw new CMSRuntimeException(errorMessage, e1);
        }
    }

    private long getNodeSize(Node node) {
        long nodeSize = 0;
        try {
            if (node.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM)) {
                try {
                    return ((DirectoryItem) getItemFromNode(node)).getSize();
                } catch (MalformedDirectoryItemException e) {
                    throw new CMSRuntimeException("Cannot calculate node size. Item [" + node.getIdentifier() + "] is malformed");
                }
            }
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
        return nodeSize;
    }

    @Deprecated
    public void deleteItemList(String[] UUIDs) throws DirectoryItemNotFoundException {
        if (log.isDebugEnabled()) log.debug("Removing the File List");
        for (int i = 0; i < UUIDs.length; i++) {
            String UUID = UUIDs[i];
            if (getFileRelatedContents(UUID).size() == 0) {
                deleteItem(UUID);
            }
        }
    }

    public void deleteItem(DirectoryItem item) throws CMSRuntimeException {
        deleteItem(item.getId(), item.getWorkspace());
    }

    public void deleteItemByURLPath(String urlPath) throws CMSRuntimeException {
        try {
            Node nodeByURLPath = getNodeByURLPath(urlPath);
            deleteItem(nodeByURLPath.getIdentifier(), nodeByURLPath.getSession().getWorkspace().getName());
        } catch (RepositoryException e1) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e1);
            throw new CMSRuntimeException(errorMessage, e1);
        } catch (DirectoryItemNotFoundException e) {
            String errorMessage = "Error deleting item. Not found.";
            log.error(errorMessage);
            throw new CMSRuntimeException(errorMessage);
        }
    }

    public DirectoryItem getItemFromNode(DirectoryItemNode directoryItemNode) throws MalformedDirectoryItemException {
        Node node = directoryItemNode.getNode();
        return getItemFromNode(node);
    }

    public DirectoryItem getItemFromNode(Node node) throws MalformedDirectoryItemException {
        DirectoryItem item = getItemFromCache(node);
        if (item == null) {
            final String UUID;
            try {
                UUID = node.getIdentifier();
            } catch (RepositoryException e) {
                String errorMessage = "Error accessing the repository.";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
            if (log.isDebugEnabled()) log.debug("Building Item with UUID : " + UUID + " because is not in cache");
            item = buildItemFromNode(node);
            putInCache(UUID, item);
        }
        return item;
    }

    private DirectoryItem buildItemFromNode(Node node) throws CMSRuntimeException, MalformedDirectoryItemException {
        try {
            DirectoryItem item;
            NodeType type = node.getPrimaryNodeType();
            if (!node.hasNode(JCRUtil.JCR_CONTENT_PREFIX)) {
                throw new MalformedDirectoryItemException(node.getIdentifier());
            }
            Node propertiesSubNode = node.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            Map<String, Object> propertyMap = JCRUtil.buildPropertyMapFromJCR(propertiesSubNode);
            if (type.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FILE)) {
                item = buildFileFromNode(node, propertyMap);
            } else if (type.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                item = buildFolderFromNode(node, propertyMap);
            } else {
                String errorMessage = "Error the type of the Node " + node.getPrimaryNodeType().getName() + " is not a Valid DirectoryItem node";
                log.error(errorMessage);
                throw new CMSRuntimeException(errorMessage);
            }
            return item;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private DirectoryFile buildFileFromNode(Node node, Map<String, Object> propertyMap) throws CMSRuntimeException, RepositoryException {
        DirectoryFile directoryFile = new DirectoryFile("", "");
        JCRUtil.populateObject(directoryFile, propertyMap);
        DirectoryFile file = directoryFile;
        file.setId(node.getIdentifier());
        String filePath = getURLPath(node);
        file.setURLPath(filePath);
        file.setWorkspace(node.getSession().getWorkspace().getName());
        return directoryFile;
    }

    private DirectoryFolder buildFolderFromNode(Node node, Map<String, Object> propertyMap) throws CMSRuntimeException, RepositoryException {
        DirectoryFolder directoryFolder = new DirectoryFolder("");
        JCRUtil.populateObject(directoryFolder, propertyMap);
        DirectoryFolder folder = directoryFolder;
        folder.setId(node.getIdentifier());
        String filePath = getURLPath(node);
        folder.setURLPath(filePath);
        folder.setWorkspace(node.getSession().getWorkspace().getName());
        return folder;
    }

    public Node getNodeFromItem(DirectoryItem item) throws CMSRuntimeException {
        try {
            String workspace = item.getWorkspace();
            Session session = JCRUtil.currentSession(workspace);
            return session.getNodeByIdentifier(item.getId());
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public byte[] getFileDataFromFile(DirectoryFile directoryFile) throws IOException, MalformedDirectoryItemException {
        return getFileDataFromFile(JCRUtil.JCR_FILE_PREFIX, directoryFile);
    }

    public DirectoryFolder getRootFolder(DirectoryFile file) {
        try {
            DirectoryFolder ownerFolder = getOwnerFolder(file);
            Node directoryItemNode = JCRUtil.currentSession(file.getWorkspace()).getNodeByIdentifier(ownerFolder.getId());
            log.debug("Getting the owner Folder of item with path : " + directoryItemNode.getPath());
            Node topFolderNode = directoryItemNode;
            while (!directoryItemNode.isNodeType(JCRUtil.NODETYPE_CONTENT)) {
                if (directoryItemNode.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                    topFolderNode = directoryItemNode;
                }
                directoryItemNode = directoryItemNode.getParent();
            }
            return (DirectoryFolder) getItemFromNode(topFolderNode);
        } catch (MalformedDirectoryItemException e) {
            log.debug("Erro getting root folder of file " + file.getName(), e);
        } catch (RepositoryException e) {
            log.debug("Erro getting root folder of file " + file.getName(), e);
        }
        return null;
    }

    public DirectoryFolder getRootFolder(DirectoryFolder currentFolder) throws MalformedDirectoryItemException, DirectoryItemNotFoundException {
        String rootFolderID = currentFolder.getRootFolderId();
        DirectoryFolder rootFolder = (DirectoryFolder) getItemByUUIDWorkspace(rootFolderID, currentFolder.getWorkspace());
        if (rootFolder == null) return currentFolder; else return rootFolder;
    }

    public byte[] getFileDataFromUUIDWorkspace(String uuid, String workspace) throws IOException, MalformedDirectoryItemException, DirectoryItemNotFoundException {
        return getFileDataFromFile(JCRUtil.JCR_FILE_PREFIX, (DirectoryFile) getItemByUUIDWorkspace(uuid, workspace));
    }

    public byte[] getThumbnailFileDataFromFile(DirectoryFile directoryFile) {
        Node node = getNodeFromItem(directoryFile);
        try {
            if (hasThumbnailProperty(node)) return getFileDataFromProperty(JCRUtil.JCR_THUMBNAIL_PREFIX, node); else if (hasThumbnailNode(node)) return getFileDataFromNode(JCRUtil.JCR_THUMBNAIL_PREFIX, node);
        } catch (Exception e) {
            log.error("The thumbnail is incorrect.");
            return null;
        }
        return null;
    }

    public byte[] generateThumbnailFileDataFromFile(DirectoryFile directoryFile) throws IOException {
        Node node = getNodeFromItem(directoryFile);
        try {
            if (directoryFile.isImage()) {
                InputStream savedInputStream = getInputStreamFromNode(node);
                long start = System.currentTimeMillis();
                saveResizedImage(directoryFile, savedInputStream, ResizeType.thumbnail);
                if (Performance.performanceLog.isDebugEnabled()) Performance.performanceLog.debug("Generating thumbnail, total time: " + (System.currentTimeMillis() - start));
                return getFileDataFromProperty(JCRUtil.JCR_THUMBNAIL_PREFIX, node);
            }
        } catch (MalformedDirectoryItemException e) {
            log.error("Cannot generate thumbnail for file " + directoryFile.getId() + ". File is malformed");
            return null;
        }
        return null;
    }

    public byte[] getMediumSizeImageDataFromFile(DirectoryFile directoryFile) {
        Node node = getNodeFromItem(directoryFile);
        try {
            if (hasMediumSizeImageProperty(node)) return getFileDataFromProperty(JCRUtil.JCR_MEDIUM_SIZE_PREFIX, node);
        } catch (Exception e) {
            log.error("The medium size image is incorrect.", e);
            return null;
        }
        return null;
    }

    public byte[] generateMediumSizeImageDataFromFile(DirectoryFile directoryFile) throws IOException {
        Node node = getNodeFromItem(directoryFile);
        try {
            if (directoryFile.isImage()) {
                InputStream savedInputStream = getInputStreamFromNode(node);
                saveResizedImage(directoryFile, savedInputStream, ResizeType.medium);
                return getFileDataFromProperty(JCRUtil.JCR_MEDIUM_SIZE_PREFIX, node);
            }
        } catch (MalformedDirectoryItemException e) {
            log.error("Cannot generate medium size image for file " + directoryFile.getId() + ". File is malformed");
            return null;
        }
        return null;
    }

    private boolean hasThumbnailProperty(Node node) {
        try {
            return node.hasProperty(JCRUtil.JCR_THUMBNAIL_PREFIX);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean hasMediumSizeImageProperty(Node node) {
        try {
            return node.hasProperty(JCRUtil.JCR_MEDIUM_SIZE_PREFIX);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean hasThumbnailNode(Node node) throws RepositoryException {
        return node.hasNode(JCRUtil.JCR_THUMBNAIL_PREFIX);
    }

    public byte[] getFileDataFromFile(String name, DirectoryFile directoryFile) throws IOException, MalformedDirectoryItemException {
        InputStream is = getInputStreamFromFile(name, directoryFile);
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public byte[] getFileDataFromProperty(String name, Node node) throws IOException, MalformedDirectoryItemException {
        Property dataProperty;
        try {
            dataProperty = node.getProperty(name);
            InputStream is = dataProperty.getValue().getBinary().getStream();
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } catch (RepositoryException e) {
            throw new MalformedDirectoryItemException("Cannot obtain binary property [" + name + "] from node " + node);
        }
    }

    public byte[] getFileDataFromNode(String name, Node node) throws IOException, MalformedDirectoryItemException {
        InputStream is = getInputStreamFromNode(name, node);
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public InputStream getInputStreamFromFile(DirectoryFile directoryFile) throws MalformedDirectoryItemException {
        return getInputStreamFromFile(JCRUtil.JCR_FILE_PREFIX, directoryFile);
    }

    private InputStream getInputStreamFromFile(String name, DirectoryFile directoryFile) throws MalformedDirectoryItemException {
        Node fileNode = getNodeFromItem(directoryFile);
        return getInputStreamFromNode(name, fileNode);
    }

    private InputStream getInputStreamFromNode(Node fileNode) {
        return getInputStreamFromNode(JCRUtil.JCR_FILE_PREFIX, fileNode);
    }

    private InputStream getInputStreamFromNode(String name, Node fileNode) {
        try {
            Node itemNode = fileNode.getNode(name).getNode(JCRUtil.JCR_CONTENT_PREFIX);
            Property dataProperty = itemNode.getProperty(JCRUtil.JCR_DATA_PREFIX);
            return dataProperty.getValue().getBinary().getStream();
        } catch (RepositoryException e) {
            String errorMessage = "Error getting the file from node...";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public DirectoryItem getItemFromPath(String urlPath) throws DirectoryItemNotFoundException {
        try {
            Node itemNode = getNodeByURLPath(urlPath);
            long start = System.currentTimeMillis();
            DirectoryItem itemFromNode = getItemFromNode(itemNode);
            if (Performance.performanceLog.isDebugEnabled()) Performance.performanceLog.debug("Item got by node. Total time: " + (System.currentTimeMillis() - start));
            return itemFromNode;
        } catch (ItemNotFoundException e) {
            throw new DirectoryItemNotFoundException("Item with path : " + urlPath + " Not found, error: " + e);
        } catch (RepositoryException e) {
            String errorMessage = "Error getting the item with path: " + urlPath + " error: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            String errorMessage = "Error getting the item with path: " + urlPath + " Item is malformed";
            throw new CMSRuntimeException(errorMessage);
        }
    }

    /**
     * @param urlPath
     * @return
     * @throws RepositoryException
     * @throws DirectoryItemNotFoundException 
     * @throws ItemNotFoundException
     */
    public Node getNodeByURLPath(String urlPath) throws RepositoryException, DirectoryItemNotFoundException {
        long start = System.currentTimeMillis();
        Node node = selectedSearchStrategy().searchByURLPath(urlPath);
        long end = System.currentTimeMillis();
        if (Performance.performanceLog.isDebugEnabled()) Performance.performanceLog.debug("Searching by URL path: " + (end - start));
        return node;
    }

    private SimpleSearcherStrategy selectedSearchStrategy() {
        return simpleSearchStrategy();
    }

    @SuppressWarnings("unused")
    private FolderCachedSearcherStrategy folderCachedSearcherStrategy() {
        if (folderCachedSearcherStrategy == null) folderCachedSearcherStrategy = new FolderCachedSearcherStrategy(getCacheAdmin());
        return folderCachedSearcherStrategy;
    }

    @SuppressWarnings("unused")
    private SearchByURLPathAttribtueStrategy searchByURLPathAttribtueStrategy() {
        if (searchByURLPathAttribtueStrategy == null) searchByURLPathAttribtueStrategy = new SearchByURLPathAttribtueStrategy();
        return searchByURLPathAttribtueStrategy;
    }

    private SimpleSearcherStrategy simpleSearchStrategy() {
        if (simpleSearcherStrategy == null) simpleSearcherStrategy = new SimpleSearcherStrategy();
        return simpleSearcherStrategy;
    }

    public boolean existFileInRootFolder(String urlPath, String rootFolderId, String workspace) {
        Node node = null;
        try {
            node = simpleSearchStrategy().doSearchByRootFolderBaseNodeRestPath(JCRUtil.getNodeById(rootFolderId, workspace), urlPath);
        } catch (DirectoryItemNotFoundException e) {
            if (log.isDebugEnabled()) log.debug("Url path " + urlPath + " not found in " + rootFolderId + "/" + workspace);
            return false;
        } catch (RepositoryException e) {
            log.error("Error accessing repository when looking for file: " + urlPath + " in workspace " + workspace);
            return false;
        }
        return (node != null);
    }

    private Node getItemNodeByName(Node parentNode, String itemName) throws DirectoryItemNotFoundException {
        String unescapeItemName = ContentsUtil.getInstance().unescapePathElement(itemName);
        String escapeApostrophesForXPath = JCRUtil.escapeApostrophesForXPath(unescapeItemName);
        final String nodePathForQuery = getNodePathForQuery(parentNode);
        Object[] queryArguments = new Object[] { nodePathForQuery, escapeApostrophesForXPath };
        String query = descendantNameQueryMessageFormat.format(queryArguments);
        Node n;
        try {
            n = JCRUtil.searchUniqueNodeByXPathQuery(query, parentNode.getSession());
        } catch (ItemNotFoundException e) {
            throw new DirectoryFolderNotFoundException("Item " + unescapeItemName + " under node: " + nodePathForQuery);
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing parent node of jcr:content: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
        try {
            return n.getParent();
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing parent node of jcr:content: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private Node getItemNodeByNameAlternative(Node parentNode, String itemName) throws DirectoryItemNotFoundException {
        final String nodePathForQuery;
        String unescapeItemName = ContentsUtil.getInstance().unescapePathElement(itemName);
        nodePathForQuery = getNodePathForQueryAlternative(parentNode);
        String query = descendantNameQueryMessageFormat.format(new String[] { nodePathForQuery, JCRUtil.escapeApostrophesForXPath(unescapeItemName) });
        Node n;
        try {
            long init = System.currentTimeMillis();
            n = JCRUtil.searchUniqueNodeByXPathQuery(query, parentNode.getSession());
            long end = System.currentTimeMillis();
            if (Performance.performanceLog.isDebugEnabled()) Performance.performanceLog.debug("Get DirectoryItem (alternative method) " + (end - init) + " ms");
        } catch (ItemNotFoundException e) {
            throw new DirectoryFolderNotFoundException("Item " + unescapeItemName + " under node: " + nodePathForQuery);
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing parent node of jcr:content: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
        try {
            return n.getParent();
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing parent node of jcr:content: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public String getURLPath(DirectoryItem item) {
        try {
            Node itemNode = getNodeFromItem(item);
            return getURLPath(itemNode);
        } catch (Exception e) {
            String errorMessage = "Error finding node for file [" + item.getId() + "]: ";
            log.error(errorMessage);
            throw new CMSRuntimeException(errorMessage + e);
        }
    }

    public String getURLPath(Node node) throws RepositoryException {
        if (node.getParent().getPrimaryNodeType().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER) || node.getParent().getPrimaryNodeType().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FILE)) {
            String name = node.getNode(JCRUtil.JCR_CONTENT_PREFIX).getProperty("name").getString();
            name = ContentsUtil.getInstance().escapePathElement(name);
            return getURLPath(node.getParent()) + "/" + name;
        } else {
            StringBuffer folderPrefix = new StringBuffer("");
            String nodeWorkspace = JCRUtil.nodeWorkspace(node);
            if (!nodeWorkspace.equals(JCRUtil.DEFAULT_WORKSPACE)) folderPrefix.append(nodeWorkspace).append("/");
            folderPrefix.append(node.getIdentifier());
            return folderPrefix.toString();
        }
    }

    public List<DirectoryItem> getPathList(DirectoryFolder currentFolder) {
        List<DirectoryItem> pathList = new ArrayList<DirectoryItem>();
        JCRDirectoryPersister jcrDirectoryPersister = JCRManagerRegistry.getJcrDirectoryPersister();
        try {
            Node currentNode = jcrDirectoryPersister.getNodeFromItem(currentFolder);
            pathList.add(0, jcrDirectoryPersister.getItemFromNode(currentNode));
            while (currentNode.getParent().isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                pathList.add(0, jcrDirectoryPersister.getItemFromNode(currentNode.getParent()));
                currentNode = currentNode.getParent();
            }
            return pathList;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            throw new CMSRuntimeException("One of the path elements is malformed", e);
        }
    }

    public DirectoryItem getItemByUUIDWorkspace(String uuid, String workspace) throws MalformedDirectoryItemException, DirectoryItemNotFoundException {
        String resolvedWorkspace = workspace;
        String resolvedUuid = uuid;
        if (uuid.contains(":")) {
            StringTokenizer uuidTokenizer = new StringTokenizer(uuid, ":");
            resolvedWorkspace = uuidTokenizer.nextToken();
            resolvedUuid = uuidTokenizer.nextToken();
        }
        try {
            Node itemNode = JCRUtil.getNodeById(resolvedUuid, resolvedWorkspace);
            DirectoryItem item = getItemFromNode(itemNode);
            return item;
        } catch (ItemNotFoundException e) {
            throw new DirectoryItemNotFoundException(resolvedWorkspace + ":" + resolvedUuid);
        } catch (RepositoryException e1) {
            String errorMessage = "Error accessing the repository.";
            log.error(errorMessage, e1);
            throw new CMSRuntimeException(errorMessage, e1);
        }
    }

    private DirectoryItem getItemFromCache(Node node) {
        String UUID;
        try {
            UUID = node.getIdentifier();
        } catch (RepositoryException e2) {
            String errorMessage = "Error getting UUID of node: ";
            log.error(errorMessage);
            throw new CMSRuntimeException(errorMessage + e2);
        }
        try {
            return (DirectoryItem) getCacheAdmin().getFromCache(UUID);
        } catch (NeedsRefreshException e) {
            try {
                DirectoryItem item = buildItemFromNode(node);
                putInCache(UUID, item);
                return item;
            } catch (MalformedDirectoryItemException me) {
                log.warn("Error refreshing Directory Item " + UUID + "Item is malformed");
                cacheAdmin.cancelUpdate(UUID);
                return null;
            } catch (Throwable e1) {
                log.warn("Error refreshing Directory Item " + UUID);
                cacheAdmin.cancelUpdate(UUID);
                return null;
            }
        }
    }

    private void putInCache(String id, DirectoryItem item) {
        getCacheAdmin().putInCache(id, item);
    }

    private void removeFromCache(Node node) throws RepositoryException {
        getCacheAdmin().flushEntry(node.getIdentifier());
        getCacheAdmin().removeEntry(node.getIdentifier());
    }

    public boolean fileNameExists(String query) {
        for (String workspace : JCRUtil.getAccesibleWorkspaceNames()) {
            try {
                JCRUtil.searchUniqueNodeByXPathQuery(query, JCRUtil.currentSession(workspace));
                return Boolean.TRUE;
            } catch (ItemNotFoundException e) {
                if (log.isDebugEnabled()) log.debug("Catch ItemNotFoundException: " + e);
            }
        }
        return Boolean.FALSE;
    }

    public Node getFirstNodeFound(String query) {
        for (String workspace : JCRUtil.getAccesibleWorkspaceNames()) {
            try {
                return JCRUtil.searchUniqueNodeByXPathQuery(query, JCRUtil.currentSession(workspace));
            } catch (ItemNotFoundException e) {
                if (log.isDebugEnabled()) log.debug("Catch ItemNotFoundException: " + e);
            }
        }
        return null;
    }

    private boolean fileNameExists(DirectoryFolder folder, String fileName) {
        return fileNameExists(fileName, folder.getURLPath());
    }

    public boolean fileNameExists(String fileName, String URLPath) {
        try {
            Node node = getNodeByURLPath(URLPath);
            String query = getNodePathForQuery(node) + "/*/" + JCRUtil.JCR_CONTENT_PREFIX + "[@" + NAME_ATTRIBUTE + " = '" + JCRUtil.escapeApostrophesForXPath(fileName) + "']";
            return fileNameExists(query);
        } catch (RepositoryException e) {
            return false;
        } catch (DirectoryItemNotFoundException e) {
            return false;
        }
    }

    public Node fileNameWithMIMETypeExists(String mimeType, String URLPath) {
        try {
            Node node = getNodeByURLPath(URLPath);
            String query = getNodePathForQuery(node) + "/*/" + JCRUtil.JCR_CONTENT_PREFIX + "[@mimeType = '" + mimeType + "']";
            return getFirstNodeFound(query);
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        } catch (DirectoryItemNotFoundException e) {
            return null;
        }
    }

    public DirectoryFolder getFolder(DirectoryFolder parentFolder, String folderName) throws DirectoryItemNotFoundException, MalformedDirectoryItemException {
        return (DirectoryFolder) getDirectoryItem(parentFolder, folderName);
    }

    @Override
    public DirectoryFile getFile(DirectoryFolder parentFolder, String fileName) throws MalformedDirectoryItemException, DirectoryItemNotFoundException {
        return (DirectoryFile) getDirectoryItem(parentFolder, fileName);
    }

    @Override
    public DirectoryItem getDirectoryItem(DirectoryFolder parentFolder, String fileName) throws MalformedDirectoryItemException, DirectoryItemNotFoundException {
        Node parentNode = getNodeFromItem(parentFolder);
        DirectoryItem directoryItem;
        Node itemNode = getItemNodeByName(parentNode, fileName);
        getItemNodeByNameAlternative(parentNode, fileName);
        directoryItem = getItemFromNode(itemNode);
        return directoryItem;
    }

    public DirectoryFile overwriteFile(DirectoryFolder parentFolder, InputStream inputStream, String fileName, long fileSize) throws CMSRuntimeException, DirectoryItemNotFoundException, FileNotFoundException {
        Node parentNode = getNodeFromItem(parentFolder);
        try {
            Node itemNode = getItemNodeByName(parentNode, fileName);
            long oldSize = getNodeSize(itemNode);
            ZipEntryInputStream zeis = new ZipEntryInputStream(inputStream);
            Node jcrContentNode = itemNode.getNode(JCRUtil.JCR_FILE_PREFIX).getNode(JCRUtil.JCR_CONTENT_PREFIX);
            jcrContentNode.setProperty(JCRUtil.JCR_DATA_PREFIX, JCRUtil.createBinaryValue(zeis, jcrContentNode));
            try {
                updateRecursiveFolderSize(parentFolder, fileSize - oldSize);
            } catch (Throwable e) {
                log.warn("Cannot update folder [" + parentFolder.getId() + "] size", e);
            }
            DirectoryFile itemFromNode = (DirectoryFile) getItemFromNode(new DirectoryFileNode(itemNode));
            itemFromNode.setSize(fileSize);
            return itemFromNode;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            log.error("Overwritten file [" + fileName + "] is malformed");
            throw new CMSRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public String addZipFile(DirectoryFolder folder, File file, String ownerId) throws CMSRuntimeException, ErrorUnzippingZipEntryException {
        if (log.isDebugEnabled()) log.debug("addZipFile: starting to add zip");
        ZipArchiveEntry theEntry = null;
        DirectoryFolder currentFolder = folder;
        Map<String, DirectoryFolder> importedFolders = new HashMap<String, DirectoryFolder>();
        try {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<ZipArchiveEntry> all = zipFile.getEntries();
            while (all.hasMoreElements()) {
                theEntry = (ZipArchiveEntry) all.nextElement();
                if (theEntry.isDirectory()) {
                    if (log.isDebugEnabled()) log.debug("addZipFile: Adding Folder :" + theEntry.getName());
                    currentFolder = makeDir(folder, theEntry.getName(), importedFolders, ownerId);
                    importedFolders.put(theEntry.getName(), currentFolder);
                } else if (!(StringUtils.contains(theEntry.getName(), ".zip"))) {
                    if (log.isDebugEnabled()) log.debug("addZipFile: Adding file in the folder: " + currentFolder);
                    String entryName = theEntry.getName();
                    String entryFolderPath = "";
                    int lastSeparatorIndex = theEntry.getName().lastIndexOf("/");
                    if (lastSeparatorIndex != -1) {
                        entryName = entryName.substring(lastSeparatorIndex + 1, theEntry.getName().length());
                        entryFolderPath = theEntry.getName().substring(0, lastSeparatorIndex);
                    }
                    if (log.isDebugEnabled()) log.debug("addZipFile: Processing File " + entryName);
                    if (folder == null || entryFolderPath == null) {
                        if (log.isDebugEnabled()) log.debug("addZipFile: folderpath is null");
                    }
                    DirectoryFolder folderToAddFile = makeDir(folder, entryFolderPath, importedFolders, ownerId);
                    if (ownerId != null) folderToAddFile.setOwnerId(ownerId);
                    importedFolders.put(entryFolderPath, folderToAddFile);
                    if (log.isDebugEnabled()) log.debug("addZipFile: Dir created  ");
                    long size = theEntry.getSize();
                    processFileZipEntry(folderToAddFile, entryName, zipFile.getInputStream(theEntry), size, ownerId);
                    if (log.isDebugEnabled()) log.debug("addZipFile: Added file in the folder: " + currentFolder);
                }
            }
        } catch (IllegalArgumentException ioe) {
            String errorMessage = "Error unzipping " + ((theEntry != null) ? "entry " + theEntry.getName() + " from " : "") + "zip file: " + file.getName();
            log.error(errorMessage, ioe);
            throw new ErrorUnzippingZipEntryException(errorMessage, ioe, theEntry.getName());
        } catch (Exception e) {
            String errorMessage = "Error processing zip entry named: " + theEntry;
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
        return DirectoryPersister.SUCCESS;
    }

    public DirectoryFolder makeDir(DirectoryFolder parentFolder, String entryName, Map<String, DirectoryFolder> importedFolders, String ownerId) throws DirectoryFileNotFoundException, DirectoryItemNotFoundException {
        String currentFolderKey = "";
        StringTokenizer tokenizer = new StringTokenizer(entryName, "/", false);
        DirectoryFolder currentFolder = parentFolder;
        while (tokenizer.hasMoreTokens()) {
            String currentTokenFolderName = tokenizer.nextToken();
            currentFolderKey += "/" + currentTokenFolderName;
            DirectoryFolderNode childFolderNode = childFolder(currentTokenFolderName, currentFolder);
            if (childFolderNode != null) {
                String newCurrentFolderPath = currentFolder.getURLPath() + "/" + currentTokenFolderName;
                if (log.isDebugEnabled()) log.debug("Path exists: " + newCurrentFolderPath);
                try {
                    currentFolder = (DirectoryFolder) buildItemFromNode(childFolderNode.getNode());
                } catch (MalformedDirectoryItemException e) {
                    throw new CMSRuntimeException(e);
                }
            } else {
                if (log.isDebugEnabled()) log.debug("Path not exists: " + currentFolder.getURLPath() + "/" + currentTokenFolderName);
                DirectoryFolder folder = new DirectoryFolder(currentTokenFolderName);
                if (ownerId != null) folder.setOwnerId(ownerId);
                currentFolder = addFolder(currentFolder, folder);
                importedFolders.put(currentFolderKey, currentFolder);
            }
        }
        return currentFolder;
    }

    private DirectoryFolderNode childFolder(String childFolderName, DirectoryFolder currentFolder) {
        Node folderNode = getNodeFromItem(currentFolder);
        try {
            NodeIterator nodes = folderNode.getNodes(JCRUtil.FOLDER_PREFIX);
            while (nodes.hasNext()) {
                Node childFolder = nodes.nextNode();
                DirectoryFolderNode directoryFolderNode = DirectoryFolderNode.createDirectoryFolderNode(childFolder);
                try {
                    if (childFolderName.equals(directoryFolderNode.getName())) return directoryFolderNode;
                } catch (MalformedContentException e) {
                    log.warn("Malformed folder node: " + directoryFolderNode.getUUID());
                } catch (PathNotFoundException e) {
                    log.warn("Malformed folder node: " + directoryFolderNode.getUUID());
                } catch (RepositoryException e) {
                    log.warn("Malformed folder node: " + directoryFolderNode.getUUID());
                }
            }
        } catch (RepositoryException e) {
            throw new CMSRuntimeException();
        }
        return null;
    }

    private void processFileZipEntry(DirectoryFolder currentFolder, String entryName, InputStream source, long size, String ownerId) throws Exception {
        DirectoryFile file = new DirectoryFile(entryName);
        if (ownerId != null) file.setOwnerId(ownerId);
        file.setSize(size);
        String mime = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(entryName);
        file.setMimeType(ScribeMimeTypes.getMimeType(entryName, mime));
        if (ScribeMimeTypes.isSupportedTextMimeType(mime)) {
            if ((file.getEncoding() == null) || (file.getEncoding().equals(""))) file.setEncoding("UTF-8");
        }
        try {
            if (fileNameExists(entryName, currentFolder.getURLPath())) {
                overwriteFile(currentFolder, source, entryName, size);
            } else {
                addFile(currentFolder, file, source, size);
            }
        } catch (FileNotFoundException e2) {
            String errorMessage = "Error in file inside the zip: " + entryName + " Error : ";
            log.error(errorMessage, e2);
            throw new CMSRuntimeException(errorMessage, e2);
        }
    }

    public ScribeCache getCacheAdmin() {
        return cacheAdmin;
    }

    public void setCacheAdmin(ScribeCache cacheAdmin) {
        this.cacheAdmin = cacheAdmin;
    }

    private String getNodePathForQuery(Node n) {
        try {
            return nodePathForQueryMessageFormat.format(new Object[] { n.getIdentifier() });
        } catch (RepositoryException e) {
            try {
                StringTokenizer st = new StringTokenizer(n.getPath(), "/", false);
                String path = "";
                while (st.hasMoreTokens()) {
                    String tempPath = st.nextToken();
                    if (!tempPath.endsWith("]")) {
                        tempPath += "[1]";
                    }
                    path = path + "/" + tempPath;
                }
                return "/" + path;
            } catch (RepositoryException e1) {
                String errorMessage = "Error getting NodePathForQuery: " + n + " Error : ";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e1);
            }
        }
    }

    private String getNodePathForQueryAlternative(Node n) {
        try {
            return n.getPath().substring(1);
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
    }

    public List<Content> getFileRelatedContents(String UUID) {
        String query = "//*" + "[@" + "fileID" + " = '" + UUID + "']";
        List<Content> contents = new ArrayList<Content>();
        for (String workspace : JCRUtil.getAccesibleWorkspaceNames()) {
            Session session = JCRUtil.currentSession(workspace);
            QueryResult qr = JCRUtil.searchByXPathQuery(query, session);
            NodeIterator ni;
            try {
                ni = qr.getNodes();
                while (ni.hasNext()) {
                    Node n = (Node) ni.next();
                    try {
                        Content content = getContentParentName(n);
                        if (!contents.contains(content)) {
                            contents.add(content);
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (RepositoryException e) {
                String errorMessage = "Error getting related contents of file: " + UUID + " Error : ";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
        }
        return contents;
    }

    private Content getContentParentName(Node node) throws RepositoryException {
        NodeType type = node.getPrimaryNodeType();
        String errorMessage = "Error getting content of node: " + node + " Error : ";
        if (type.isNodeType("nt:content")) {
            ContentManager contentManager = ManagerRegistry.getContentManager();
            Content relatedContent;
            try {
                relatedContent = contentManager.getContentById(node.getIdentifier(), JCRUtil.nodeWorkspace(node));
            } catch (ContentNotFoundException e) {
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            } catch (MalformedContentException e) {
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
            return relatedContent;
        }
        try {
            return (getContentParentName(node.getParent()));
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public List<DirectoryItem> getFilesWithRelatedContents(String[] filePaths) throws DirectoryItemNotFoundException {
        List<DirectoryItem> fileList = new ArrayList<DirectoryItem>();
        for (int i = 0; i < filePaths.length; i++) {
            String path = filePaths[i];
            if (getFileRelatedContents(path).size() != 0) {
                fileList.add(getItemFromPath(path));
            }
        }
        return fileList;
    }

    public DirectoryFile copyFile(DirectoryFile originalFile, DirectoryFolder targetFolder) {
        return copyFile(originalFile, targetFolder, originalFile.getName());
    }

    public DirectoryFile copyFile(DirectoryFile originalFile, DirectoryFolder targetFolder, String newFileName) {
        if (!fileNameExists(newFileName, targetFolder.getURLPath())) {
            return doCopyFile(originalFile, targetFolder, newFileName);
        }
        try {
            return getFile(targetFolder, originalFile.getName());
        } catch (MalformedDirectoryItemException e) {
            log.error("Destination file is malformed. Deleting and copying again");
            try {
                Node malformedItemNode = getNodeByURLPath(targetFolder.getURLPath() + "/" + originalFile.getName());
                deleteMalformedItemNode(malformedItemNode);
            } catch (RepositoryException e1) {
                log.error("Cannot delete malformed element", e);
            } catch (DirectoryItemNotFoundException e1) {
                log.error("Cannot find element to delete", e1);
            }
            return doCopyFile(originalFile, targetFolder, newFileName);
        } catch (DirectoryItemNotFoundException e1) {
            return doCopyFile(originalFile, targetFolder, newFileName);
        }
    }

    private void deleteMalformedItemNode(Node malformedItemNode) throws RepositoryException {
        malformedItemNode.remove();
    }

    private DirectoryFile doCopyFile(DirectoryFile originalFile, DirectoryFolder targetFolder, String newFileName) {
        if (log.isDebugEnabled()) log.debug("Copying the file with id: " + originalFile.getId() + ", to the destination Folder " + targetFolder.getId());
        Session session = JCRUtil.currentSession(originalFile.getWorkspace());
        try {
            Node originalFileNode;
            try {
                originalFileNode = session.getNodeByIdentifier(originalFile.getId());
                session.save();
            } catch (ItemNotFoundException e) {
                String errorMessage = "The file with id: " + originalFile.getId() + " is not found. ";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
            Node destinationFolderNode;
            try {
                destinationFolderNode = session.getNodeByIdentifier(targetFolder.getId());
            } catch (ItemNotFoundException e) {
                String errorMessage = "The destination world with id: " + targetFolder.getId() + " is not found. ";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
            String finishTargetPath = destinationFolderNode.getPath() + "/" + JCRUtil.FILE_PREFIX;
            String temporalTargetPath = destinationFolderNode.getPath() + "/" + originalFile.getId();
            String temporalRelativeTargetPath = temporalTargetPath.substring(1, temporalTargetPath.length());
            session.getWorkspace().copy(originalFileNode.getPath(), temporalTargetPath);
            Node temporalCopiedNode = session.getRootNode().getNode(temporalRelativeTargetPath);
            session.save();
            session.getWorkspace().move(temporalCopiedNode.getPath(), finishTargetPath);
            Node jcrContentNode = temporalCopiedNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            jcrContentNode.setProperty("name", newFileName);
            jcrContentNode.setProperty("rootFolderId", targetFolder.getId());
            session.save();
            DirectoryFile copiedFile = (DirectoryFile) getItemFromNode(temporalCopiedNode);
            try {
                updateRecursiveFolderSize(targetFolder, copiedFile.getSize());
            } catch (Throwable e) {
                log.warn("Cannot update folder [" + targetFolder.getId() + "] size", e);
            }
            return copiedFile;
        } catch (RepositoryException e) {
            String errorMessage = "Error copying node: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            throw new CMSRuntimeException("File has not been correctly copied", e);
        }
    }

    public boolean isEmpty(DirectoryFolder folder) throws ItemNotFoundException {
        try {
            Node parentNode = getNodeFromItem(folder);
            NodeIterator nodeChildren = parentNode.getNodes();
            while (nodeChildren.hasNext()) {
                Node child = nodeChildren.nextNode();
                if (child.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM)) {
                    return false;
                }
            }
            return true;
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
    }

    public boolean folderExists(DirectoryFolder parentFolder, String folderName) throws RepositoryException {
        if (folderName == null) throw new IllegalArgumentException("Illegal argument [folderName]. Value is null");
        Node parentNode = getNodeFromItem(parentFolder);
        NodeIterator nodes = parentNode.getNodes(JCRUtil.FOLDER_PREFIX);
        while (nodes.hasNext()) {
            Node folderNode = (Node) nodes.next();
            Node jcrContentNode = folderNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            if (jcrContentNode.hasProperty("name")) {
                String name = jcrContentNode.getProperty("name").getString();
                if (folderName.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if files itsn't referenced to remove it
     *
     * @param filesToRemove Collection<BinaryDataReference> files to be checked
     * @throws RepositoryException
     */
    public void removeUnlinkedFiles(Collection<BinaryDataReference> filesToRemove) throws RepositoryException {
        for (BinaryDataReference fileToRemove : filesToRemove) {
            try {
                PropertyIterator references = JCRUtil.getNodeById(fileToRemove.getFileID(), fileToRemove.getWorkspace()).getReferences();
                if (references.getSize() <= 1) {
                    deleteItem(fileToRemove.loadFile());
                    if (log.isDebugEnabled()) log.debug("The unliked file " + fileToRemove.getFileID() + " has been removed.");
                }
            } catch (ItemNotFoundException e) {
            } catch (RepositoryException e) {
                throw e;
            }
        }
    }

    public DirectoryFolder getOwnerFolder(DirectoryItem directoryItem) throws CMSRuntimeException, MalformedDirectoryItemException {
        try {
            Node directoryItemNode = JCRUtil.currentSession(directoryItem.getWorkspace()).getNodeByIdentifier(directoryItem.getId());
            log.debug("Getting the owner Folder of item with path : " + directoryItemNode.getPath());
            Node parentNode = directoryItemNode.getParent();
            if (parentNode.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                return (DirectoryFolder) getItemFromNode(parentNode);
            } else if (parentNode.isNodeType(JCRUtil.NODETYPE_CONTENT)) {
                if (directoryItemNode.isNodeType(JCRUtil.NODETYPE_DRAFT_CONTENT)) return (DirectoryFolder) getItemFromNode(parentNode.getParent()); else return null;
            }
            return null;
        } catch (ItemNotFoundException e) {
            String errorMessage = "The content with id " + directoryItem.getId() + " is not found";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing the repository: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    public DirectoryFolder getTopFolder(DirectoryFolder folder) throws MalformedDirectoryItemException {
        return goUpToFolder(folder);
    }

    private DirectoryFolder goUpToFolder(DirectoryFolder folder) throws MalformedDirectoryItemException {
        try {
            Node directoryItemNode = JCRUtil.currentSession(folder.getWorkspace()).getNodeByIdentifier(folder.getId());
            log.debug("Getting the owner Folder of item with path : " + directoryItemNode.getPath());
            Node topFolderNode = directoryItemNode;
            while (!(directoryItemNode = directoryItemNode.getParent()).getPath().equals("/")) {
                if (directoryItemNode.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
                    topFolderNode = directoryItemNode;
                }
            }
            return (DirectoryFolder) getItemFromNode(topFolderNode);
        } catch (ItemNotFoundException e) {
            throw new CMSRuntimeException("Error obtaining top folder node", e);
        } catch (AccessDeniedException e) {
            throw new CMSRuntimeException("Error obtaining top folder node", e);
        } catch (RepositoryException e) {
            throw new CMSRuntimeException("Error obtaining top folder node", e);
        }
    }

    public void dumpFS(DirectoryFolder folder) throws RepositoryException, MalformedContentException {
        dumpFSFromNode(getNodeFromItem(folder));
    }

    public void dumpFSFromNode(Node node) throws RepositoryException, MalformedContentException {
        PrintStream printStream = System.out;
        dumpFSFromNode(node, printStream);
    }

    public void dumpFSFromNode(Node node, PrintStream printStream) throws RepositoryException, PathNotFoundException, MalformedContentException {
        String accTab = "";
        dumpFSFromNode(node, printStream, accTab);
    }

    private void dumpFSFromNode(Node node, PrintStream printStream, String accTab) throws RepositoryException, PathNotFoundException, MalformedContentException {
        if (node.isNodeType(JCRUtil.NODETYPE_DIRECTORY_ITEM_FOLDER)) {
            String newAccTab = accTab + "\t";
            DirectoryFolderNode directoryFolderNode = new DirectoryFolderNode(node);
            printStream.println(accTab + "/" + directoryFolderNode.getName());
            dumpFilesFromNode(node, printStream, newAccTab);
            NodeIterator folderIterator = node.getNodes(JCRUtil.FOLDER_PREFIX);
            while (folderIterator.hasNext()) {
                Node subNode = folderIterator.nextNode();
                dumpFSFromNode(subNode, printStream, newAccTab);
            }
        } else throw new CMSRuntimeException("Cannot dump filesystem from a non-folder node");
    }

    private void dumpFilesFromNode(Node node, PrintStream printStream, String accTab) throws RepositoryException, PathNotFoundException, MalformedContentException {
        NodeIterator fileIterator = node.getNodes(JCRUtil.FILE_PREFIX);
        while (fileIterator.hasNext()) {
            Node fileNode = fileIterator.nextNode();
            DirectoryFileNode directoryFileNode = new DirectoryFileNode(fileNode);
            printStream.println(accTab + "-" + directoryFileNode.getName());
        }
    }

    public void renameFolder(DirectoryFolder currentFolder, String folderName) throws DuplicatedFolderNameException, RepositoryException, MalformedDirectoryItemException {
        String workspace = currentFolder.getWorkspace();
        String currentFolderId = currentFolder.getId();
        Node currentFolderNode = JCRUtil.getNodeById(currentFolderId, workspace);
        DirectoryFolder parentFolder = goUpToFolder(currentFolder);
        if (folderExists(parentFolder, folderName)) {
            String msg = "A folder with name " + folderName + " already exists";
            log.warn(msg);
            throw new DuplicatedFolderNameException(msg);
        } else {
            Node jcrContentNode = currentFolderNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            jcrContentNode.setProperty("name", folderName);
            currentFolder.setName(folderName);
            currentFolder.setURLPath(getURLPath(currentFolderNode));
            currentFolderNode.getSession().save();
            flushItem(currentFolder);
            putInCache(currentFolderId, currentFolder);
            Iterator<Content> iterator = currentFolder.recursiveContentIterator();
            while (iterator.hasNext()) {
                DirectoryItem currentItem = iterator.next();
                flushItem(currentItem);
            }
        }
    }

    public void moveFileToFolder(DirectoryFile fileToMove, DirectoryFolder destinationFolder) throws ContentNotFoundException, MalformedContentException {
        moveFileToFolder(fileToMove, destinationFolder, fileToMove.getName());
    }

    public void moveFileToFolder(DirectoryFile fileToMove, DirectoryFolder destinationFolder, String newFileName) throws ContentNotFoundException, MalformedContentException {
        if (!fileNameExists(newFileName, destinationFolder.getURLPath())) {
            Node fileNode;
            Node destinationFolderNode;
            try {
                fileNode = JCRUtil.getNodeById(fileToMove.getId(), fileToMove.getWorkspace());
                Node jcrContentNode = fileNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
                jcrContentNode.setProperty("name", newFileName);
                jcrContentNode.setProperty("rootFolderId", destinationFolder.getId());
                fileNode.getSession().save();
                final DirectoryFile fileMoved = (DirectoryFile) buildItemFromNode(fileNode);
                fileMoved.setSize(fileToMove.getSize());
                updateFile(fileMoved);
                destinationFolderNode = JCRUtil.getNodeById(destinationFolder.getId(), destinationFolder.getWorkspace());
            } catch (ItemNotFoundException e2) {
                throw new ContentNotFoundException(e2);
            } catch (RepositoryException e2) {
                throw new CMSRuntimeException("Error obtaining node for content [" + fileToMove.getId() + "] : " + e2 + " and/or for folder[" + destinationFolder.getId() + "]");
            } catch (MalformedDirectoryItemException e) {
                throw new CMSRuntimeException(e);
            }
            Session currentSession;
            try {
                currentSession = fileNode.getSession();
            } catch (RepositoryException e1) {
                throw new CMSRuntimeException("Error obtaining session from node: " + e1);
            }
            try {
                currentSession.move(fileNode.getPath(), destinationFolderNode.getPath() + "/" + JCRUtil.FILE_PREFIX);
                removeFromCache(fileNode);
            } catch (Exception e) {
                String errorMessage = "Unexpected error moving file to folder: ";
                log.error(errorMessage, e);
                throw new CMSRuntimeException(errorMessage, e);
            }
        }
    }

    @Override
    public DirectoryFolder moveFolder(DirectoryFolder currentFolder, DirectoryFolder newParentFolder) throws DuplicatedFolderNameException {
        return moveFolder(currentFolder, newParentFolder, currentFolder.getName());
    }

    @Override
    public DirectoryFolder moveFolder(DirectoryFolder currentFolder, DirectoryFolder newParentFolder, String newFolderName) throws DuplicatedFolderNameException {
        try {
            if (folderExists(newParentFolder, newFolderName)) {
                String msg = "A folder with name " + newFolderName + " already exists";
                log.warn(msg);
                throw new DuplicatedFolderNameException(msg);
            } else {
                Node folderNodeToMove = getNodeFromItem(currentFolder);
                Node targetFolder = getNodeFromItem(newParentFolder);
                Session currentSession = folderNodeToMove.getSession();
                currentSession.move(folderNodeToMove.getPath(), targetFolder.getPath() + "/" + JCRUtil.FOLDER_PREFIX);
                Node folderMovedNode = JCRUtil.getNodeById(currentFolder.getId(), currentFolder.getWorkspace());
                Node jcrContentNode = folderMovedNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
                jcrContentNode.setProperty("name", newFolderName);
                jcrContentNode.setProperty("rootFolderId", newParentFolder.getId());
                folderMovedNode.getSession().save();
                final DirectoryFolder folderMoved = (DirectoryFolder) buildItemFromNode(folderMovedNode);
                updateFolder(folderMoved);
                JCRUtil.runInFuture(new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws Exception {
                        folderMoved.refresh();
                        folderMoved.reindexAllContents(true);
                        return Boolean.TRUE;
                    }
                });
                return folderMoved;
            }
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        } catch (MalformedDirectoryItemException e) {
            throw new CMSRuntimeException(e);
        }
    }

    public List<DirectoryFile> getAllDescendantFilesOfFolderByFuzzyNameSearch(String path, String fileName, String contentType) throws DirectoryItemNotFoundException {
        try {
            NodeIterator nodeIterator = getAllDescendantNodesOfPathByFuzzyNameSearch(path, fileName);
            List<DirectoryFile> fileList = new ArrayList<DirectoryFile>();
            List<String> contentTypes = new ArrayList<String>();
            if (contentType.equals("image-names")) {
                contentTypes.add("image");
            } else if (contentType.equals("multimedia")) {
                contentTypes.addAll(Arrays.asList(ScribeMimeTypes.multimediaMimeTypes));
            } else {
                contentTypes.add(contentType);
            }
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.nextNode();
                DirectoryItem file = getItemFromNode(node.getParent());
                if (file instanceof DirectoryFile) {
                    if (StringUtils.isEmpty(contentType) || "ALL".equals(contentType)) {
                        fileList.add((DirectoryFile) file);
                    } else if (matchScribeContentType(contentTypes, (DirectoryFile) file)) {
                        fileList.add((DirectoryFile) file);
                    }
                }
            }
            return fileList;
        } catch (RepositoryException e) {
            String errorMessage = "Error accessing parent node of jcr:content: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            log.error("MalformedDirectoryItemException getting files of folder " + path, e);
            throw new CMSRuntimeException("MalformedDirectoryItemException getting files of folder " + path, e);
        }
    }

    @Override
    public DirectoryItem refresh(DirectoryItem directoryItem) throws MalformedDirectoryItemException {
        Node nodeFromItem = getNodeFromItem(directoryItem);
        try {
            removeFromCache(nodeFromItem);
        } catch (RepositoryException e) {
        }
        return buildItemFromNode(nodeFromItem);
    }

    private NodeIterator getAllDescendantNodesOfPathByFuzzyNameSearch(String path, String fileName) throws RepositoryException, DirectoryItemNotFoundException {
        Node folderNode = getNodeByURLPath(path);
        String nodePath = getNodePathForQuery(folderNode);
        String query = nodePath + "/*//" + JCRUtil.JCR_CONTENT_PREFIX + "[jcr:contains(@" + NAME_ATTRIBUTE + ", '*" + fileName.toLowerCase() + "*')]";
        return JCRUtil.searchNodesByXPathQuery(query, folderNode.getSession());
    }

    public void updateFile(DirectoryFile directoryFile) {
        updateItem(directoryFile);
    }

    public void updateFolder(DirectoryFolder directoryFolder) {
        updateItem(directoryFolder);
    }

    @Override
    public void updateItem(DirectoryItem directoryItem) {
        try {
            Node itemNode = getNodeFromItem(directoryItem);
            Node contentNode;
            try {
                contentNode = itemNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
            } catch (PathNotFoundException e) {
                contentNode = itemNode.addNode(JCRUtil.JCR_CONTENT_PREFIX);
            }
            JCRUtil.fillContentNode(directoryItem, contentNode);
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
    }

    @Override
    public boolean isFileFolder(DirectoryFolder directoryFolder) {
        try {
            Node folderNode = JCRUtil.getNodeById(directoryFolder.getId(), directoryFolder.getWorkspace());
            while (!folderNode.getParent().getName().equals("jcr:root")) {
                if (JCRUtil.GROUPWARE_FOLDERS_PREFIX.equals(folderNode.getParent().getName())) {
                    return true;
                } else if (folderNode.getParent().isNodeType(JCRUtil.NODETYPE_WORLD)) {
                    return false;
                } else {
                    folderNode = folderNode.getParent();
                }
            }
        } catch (RepositoryException e) {
            throw new CMSRuntimeException(e);
        }
        return false;
    }

    @Override
    public DirectoryFolder copyFolder(DirectoryFolder originaFolder, DirectoryFolder targetFolder, String newFolderName) throws DuplicatedFolderNameException {
        try {
            if (!folderExists(targetFolder, newFolderName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Copying the file with id: " + originaFolder.getId() + ", to the destination Folder " + targetFolder.getId());
                }
                Session session = JCRUtil.currentSession(originaFolder.getWorkspace());
                Node originalFolderNode;
                try {
                    originalFolderNode = session.getNodeByIdentifier(originaFolder.getId());
                } catch (ItemNotFoundException e) {
                    String errorMessage = "The file with id: " + originaFolder.getId() + " is not found. ";
                    log.error(errorMessage, e);
                    throw new CMSRuntimeException(errorMessage, e);
                }
                Node destinationFolderNode;
                try {
                    destinationFolderNode = session.getNodeByIdentifier(targetFolder.getId());
                } catch (ItemNotFoundException e) {
                    String errorMessage = "The destination world with id: " + targetFolder.getId() + " is not found. ";
                    log.error(errorMessage, e);
                    throw new CMSRuntimeException(errorMessage, e);
                }
                String finishTargetPath = destinationFolderNode.getPath() + "/" + JCRUtil.FOLDER_PREFIX;
                String temporalTargetPath = destinationFolderNode.getPath() + "/" + originaFolder.getId();
                String temporalRelativeTargetPath = temporalTargetPath.substring(1, temporalTargetPath.length());
                session.getWorkspace().copy(originalFolderNode.getPath(), temporalTargetPath);
                Node temporalCopiedNode = session.getRootNode().getNode(temporalRelativeTargetPath);
                session.save();
                session.getWorkspace().move(temporalCopiedNode.getPath(), finishTargetPath);
                Node jcrContentNode = temporalCopiedNode.getNode(JCRUtil.JCR_CONTENT_PREFIX);
                jcrContentNode.setProperty("name", newFolderName);
                jcrContentNode.setProperty("rootFolderId", targetFolder.getId());
                session.save();
                try {
                    updateFolderSize(targetFolder, originaFolder.getSize());
                } catch (Throwable e) {
                    log.warn("Cannot update folder [" + targetFolder.getId() + "] size", e);
                }
                return (DirectoryFolder) getItemFromNode(temporalCopiedNode);
            } else {
                throw new DuplicatedFolderNameException("A folder with name " + newFolderName + " already exists");
            }
        } catch (RepositoryException e) {
            String errorMessage = "Error copying node: ";
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        } catch (MalformedDirectoryItemException e) {
            throw new CMSRuntimeException("File has not been correctly copied", e);
        }
    }
}
