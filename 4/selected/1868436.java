package net.sf.fileexchange.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import net.sf.fileexchange.api.snapshot.DirectoryLinkSnapshot;
import net.sf.fileexchange.api.snapshot.ResourceTreeSnapshot;
import net.sf.fileexchange.api.snapshot.VirtualFolderSnapshot;
import net.sf.fileexchange.api.snapshot.events.AddDirectoryLinkChildEvent;
import net.sf.fileexchange.api.snapshot.events.AddFileLinkChildEvent;
import net.sf.fileexchange.api.snapshot.events.AddVirtualFolderChildEvent;
import net.sf.fileexchange.api.snapshot.events.CollapseVirtualFolderChildEvent;
import net.sf.fileexchange.api.snapshot.events.DeleteVirtualFolderChildEvent;
import net.sf.fileexchange.api.snapshot.events.RenameVirtualFolderChildEvent;
import net.sf.fileexchange.api.snapshot.events.SetUploadEnabledStateOfVirtualFolder;
import net.sf.fileexchange.api.snapshot.events.SetVirtualFolderSourceEvent;
import net.sf.fileexchange.api.snapshot.events.SetVisibilityOfChildEvent;
import net.sf.fileexchange.api.snapshot.events.StorageEvent;
import net.sf.fileexchange.api.snapshot.events.StorageEventListener;
import net.sf.fileexchange.api.snapshot.events.StorageEventListeners;
import net.sf.fileexchange.api.snapshot.events.UpdateVirtualFolderChildEvent;
import net.sf.fileexchange.util.http.Method;
import net.sf.fileexchange.util.http.MovedPermanentlyException;
import net.sf.fileexchange.util.http.MultiPartFormDataReader;
import net.sf.fileexchange.util.http.RequestHeader;
import net.sf.fileexchange.util.http.Resource;
import net.sf.fileexchange.util.http.MultiPartFormDataReader.Part;

public class VirtualFolder implements ResourceTree, Iterable<VirtualFolder.Child> {

    private final List<Child> childs;

    private final List<Child> protectedChilds;

    private final Map<String, Child> nameToChildMap;

    private boolean uploadEnabled;

    private final List<Listener> listeners = new ArrayList<Listener>();

    private final ChangeListeners uploadEnabledListeners;

    private final FilesUploadedByOthers filesUploadedByOthers;

    private final StorageEventListeners<ResourceTreeSnapshot> storageEventListeners = new StorageEventListeners<ResourceTreeSnapshot>();

    private int numberOfVisibleChilds = 0;

    private File lastSourceDirectory;

    public interface Listener {

        void childGotAdded(int index, Child child);

        void childGotRemoved(int index, Child child);
    }

    private void fireChildGotAdded(int index, Child child) {
        for (Listener listener : listeners) {
            listener.childGotAdded(index, child);
        }
    }

    private void fireChildGotRemoved(int index, Child child) {
        for (Listener listener : listeners) {
            listener.childGotRemoved(index, child);
        }
    }

    public static class Child {

        private final VirtualFolder parent;

        private String name;

        private final ResourceTreeOwner<VirtualFolderSnapshot.Child> resourceTreeOwner;

        private boolean collapsed;

        private boolean visible;

        private final ChangeListeners nameChangeListener = new ChangeListeners();

        private final ChangeListeners collapsedChangeListeners = new ChangeListeners();

        private final ChangeListeners visiblityChangeListeners = new ChangeListeners();

        private Child(final VirtualFolder parent, String name, ResourceTree tree, boolean visible) {
            this.parent = parent;
            this.name = name;
            this.collapsed = true;
            this.visible = visible;
            this.resourceTreeOwner = new ResourceTreeOwner<VirtualFolderSnapshot.Child>(tree);
            registerListeners(parent);
        }

        private void fireStorageEventToParent(StorageEvent<VirtualFolderSnapshot.Child> event) {
            final int index = parent.childs.indexOf(this);
            if (index != -1) {
                parent.storageEventListeners.fireEvent(new UpdateVirtualFolderChildEvent(index, event));
            }
        }

        private void registerListeners(final VirtualFolder parent) {
            this.collapsedChangeListeners.registerListener(new ChangeListener() {

                @Override
                public void stateChanged() {
                    fireStorageEventToParent(new CollapseVirtualFolderChildEvent(isCollapsed()));
                }
            });
            this.nameChangeListener.registerListener(new ChangeListener() {

                @Override
                public void stateChanged() {
                    fireStorageEventToParent(new RenameVirtualFolderChildEvent(getName()));
                }
            });
            resourceTreeOwner.registerStorageListener(new StorageEventListener<VirtualFolderSnapshot.Child>() {

                @Override
                public void handleEvent(StorageEvent<VirtualFolderSnapshot.Child> event) {
                    fireStorageEventToParent(event);
                }
            });
            visiblityChangeListeners.registerListener(new ChangeListener() {

                @Override
                public void stateChanged() {
                    fireStorageEventToParent(new SetVisibilityOfChildEvent(isVisible()));
                    if (isVisible()) {
                        parent.numberOfVisibleChilds++;
                    } else {
                        parent.numberOfVisibleChilds--;
                    }
                }
            });
        }

        public String getName() {
            return name;
        }

        /**
		 * @return the Virtual Folder component.
		 * 
		 */
        public VirtualFolder getParent() {
            return parent;
        }

        public void remove() {
            getParent().remove(this);
        }

        public void renameTo(String newName) {
            if (this.name.equals(newName)) return;
            newName = getParent().getUnusedName(newName);
            if (this.name.equals(newName)) return;
            getParent().nameToChildMap.remove(this.name);
            getParent().nameToChildMap.put(newName, this);
            this.name = newName;
            nameChangeListener.fireChangeEvent();
        }

        public void registerNameChangeListener(ChangeListener listener) {
            nameChangeListener.registerListener(listener);
        }

        public void unregisterNameChangeListener(ChangeListener listener) {
            nameChangeListener.unregisterListener(listener);
        }

        public void toggleCollapsed() {
            this.collapsed = !collapsed;
            collapsedChangeListeners.fireChangeEvent();
        }

        public boolean isCollapsed() {
            return collapsed;
        }

        public void registerCollapsedChangeListener(ChangeListener listener) {
            collapsedChangeListeners.registerListener(listener);
        }

        public void unregisterCollapsedChangeListener(ChangeListener listener) {
            collapsedChangeListeners.unregisterListener(listener);
        }

        /**
		 * Sets the collapsed state of the child.
		 * 
		 * @param the
		 *            value to which the collapsed state should be set.
		 * 
		 * @see {@link #toggleCollapsed()} and {@link #isCollapsed()}.
		 */
        public void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            collapsedChangeListeners.fireChangeEvent();
        }

        public ResourceTreeOwner<?> getResourceTreeOwner() {
            return resourceTreeOwner;
        }

        public boolean isVisible() {
            return visible;
        }

        public void toggleVisiblity() {
            this.visible = !visible;
            visiblityChangeListeners.fireChangeEvent();
        }

        public void setVisible(boolean newValue) {
            if (this.visible != newValue) {
                this.visible = newValue;
                visiblityChangeListeners.fireChangeEvent();
            }
        }

        public void registerVisibilityChangeListener(ChangeListener listener) {
            visiblityChangeListeners.registerListener(listener);
        }

        public void unregisterVisibilityChangeListener(ChangeListener listener) {
            visiblityChangeListeners.unregisterListener(listener);
        }
    }

    public VirtualFolder(VirtualFolderSnapshot snapshot, final FilesUploadedByOthers files) {
        this.filesUploadedByOthers = files;
        this.childs = new ArrayList<Child>();
        this.protectedChilds = Collections.unmodifiableList(childs);
        this.nameToChildMap = new HashMap<String, Child>();
        this.lastSourceDirectory = snapshot.getLastSourceDirectory();
        this.uploadEnabled = snapshot.isUploadEnabled();
        this.uploadEnabledListeners = new ChangeListeners();
        addChilds(snapshot, files);
        this.uploadEnabledListeners.registerListener(new ChangeListener() {

            @Override
            public void stateChanged() {
                storageEventListeners.fireEvent(new SetUploadEnabledStateOfVirtualFolder(isUploadEnabled()));
            }
        });
    }

    private void addChilds(VirtualFolderSnapshot snapshot, FilesUploadedByOthers files) {
        int index = 0;
        final Iterator<VirtualFolderSnapshot.Child> iterator = snapshot.getChilds().iterator();
        while (iterator.hasNext()) {
            VirtualFolderSnapshot.Child childSnapshot = iterator.next();
            final ResourceTreeSnapshot childTreeSnapshot = childSnapshot.getResourceTreeSnapshot();
            if (childTreeSnapshot == null) {
                iterator.remove();
                continue;
            }
            final ResourceTree childTree = ResourceTreeFactory.createResourceTree(childTreeSnapshot, files);
            VirtualFolder.Child child = addChild(childSnapshot.getName(), childTree, childSnapshot.isVisible());
            if (!child.getName().equals(childSnapshot.getName())) {
                childSnapshot.setName(child.getName());
            }
            child.setCollapsed(childSnapshot.isCollapsed());
            index++;
        }
    }

    @Override
    public Resource getResource(List<String> path, RequestHeader header, InputStream inputStream) throws InterruptedException, IOException, MovedPermanentlyException {
        if (path.isEmpty()) {
            return parseAndCreateResource(header, inputStream);
        }
        String name = path.get(0);
        final boolean isDirectoryPath = name.endsWith("/");
        if (isDirectoryPath) {
            name = name.substring(0, name.length() - 1);
        }
        Child child = nameToChildMap.get(name);
        if (child == null) return null;
        ResourceTree tree = child.getResourceTreeOwner().getResourceTree();
        if (path.size() == 1) {
            if (tree.isDirectoryLike()) {
                if (!isDirectoryPath) {
                    URI old = header.getRequestURI();
                    throw MovedPermanentlyException.createIsDirectoryException(old);
                }
            } else {
                if (isDirectoryPath) {
                    URI old = header.getRequestURI();
                    throw MovedPermanentlyException.createIsNoDirectoryException(old);
                }
            }
        }
        return tree.getResource(path.subList(1, path.size()), header, inputStream);
    }

    void remove(Child child) {
        int index = childs.indexOf(child);
        if (index == -1) throw new IllegalArgumentException();
        childs.remove(index);
        storageEventListeners.fireEvent(new DeleteVirtualFolderChildEvent(index));
        nameToChildMap.remove(child.getName());
        if (child.isVisible()) numberOfVisibleChilds--;
        fireChildGotRemoved(index, child);
    }

    private Resource parseAndCreateResource(RequestHeader header, InputStream inputStream) throws IOException {
        String message = null;
        final boolean uploadEnabledSnapshot = isUploadEnabled();
        if (header.getMethod() == Method.POST) {
            if (uploadEnabledSnapshot) {
                String contentType = header.getField("Content-Type");
                String multiPartPrefix = "multipart/form-data; boundary=";
                if (contentType.startsWith(multiPartPrefix)) {
                    int fileCounter = 0;
                    String boundaryString = contentType.substring(multiPartPrefix.length());
                    byte[] boundary = boundaryString.getBytes("ASCII");
                    MultiPartFormDataReader multiPartReader = new MultiPartFormDataReader(inputStream, boundary);
                    Part part = multiPartReader.next();
                    byte[] buffer = new byte[1024];
                    while (part != null) {
                        fileCounter++;
                        String suggestedName = part.getFileName();
                        File file = filesUploadedByOthers.createFile(suggestedName);
                        FileOutputStream outputStream = new FileOutputStream(file);
                        InputStream partInputStream = part.getInputStream();
                        try {
                            int readedBytes = partInputStream.read(buffer);
                            while (readedBytes != -1) {
                                outputStream.write(buffer, 0, readedBytes);
                                readedBytes = partInputStream.read(buffer);
                            }
                        } finally {
                            outputStream.close();
                        }
                        synchronized (filesUploadedByOthers) {
                            filesUploadedByOthers.addEntry(suggestedName, file.getName());
                        }
                        part = multiPartReader.next();
                    }
                    if (fileCounter == 1) {
                        message = "Uploaded 1 file.";
                    } else {
                        message = String.format("Uploaded %d files.", fileCounter);
                    }
                }
            }
        }
        return createResource(header, message, uploadEnabledSnapshot);
    }

    private Resource createResource(RequestHeader header, String message, boolean uploadsEnabled) {
        final IndexPageBuilder builder = new IndexPageBuilder();
        if (uploadEnabled) {
            builder.setUploadEnabled(true);
        }
        builder.setMessage(message);
        for (Child child : childs) {
            if (child.isVisible()) {
                final String name = child.name;
                final ResourceTree childTree = child.getResourceTreeOwner().getResourceTree();
                final String metaData = childTree.getMetaData();
                final boolean directoryLike = childTree.isDirectoryLike();
                builder.addChild(name, metaData, directoryLike);
            }
        }
        return builder.build();
    }

    @Override
    public String getMetaData() {
        return String.format("%s childs", numberOfVisibleChilds);
    }

    public Child addFileLinkChild(String suggestedName, File target) {
        final Child child = addChild(suggestedName, new FileLink(target), true);
        storageEventListeners.fireEvent(new AddFileLinkChildEvent(child.name, target));
        return child;
    }

    public Child addDirectoryLinkChild(String suggestedName, File target) {
        final DirectoryLinkSnapshot snapshot = new DirectoryLinkSnapshot();
        snapshot.setTarget(target);
        final Child child = addChild(suggestedName, new DirectoryLink(snapshot), true);
        storageEventListeners.fireEvent(new AddDirectoryLinkChildEvent(child.name, target));
        return child;
    }

    public Child addVirtualFolderChild(String suggestedName) {
        final Child child = addChild(suggestedName, new VirtualFolder(new VirtualFolderSnapshot(), filesUploadedByOthers), true);
        storageEventListeners.fireEvent(new AddVirtualFolderChildEvent(child.name));
        return child;
    }

    /**
	 * Adds the specified {@link ResourceTree} with the specified name or a
	 * similar name, if the specified name is in use.
	 * 
	 * @param suggestedName
	 *            can be null in which case a name is choosen which indicates
	 *            that the file has no name.
	 * 
	 * @param the
	 *            {@link ResourceTree} to add.
	 */
    private Child addChild(String suggestedName, ResourceTree tree, boolean visible) {
        final Child child = new Child(this, getUnusedName(suggestedName), tree, visible);
        int index = childs.size();
        childs.add(child);
        nameToChildMap.put(child.name, child);
        if (child.isVisible()) numberOfVisibleChilds++;
        fireChildGotAdded(index, child);
        return child;
    }

    /**
	 * Synchronize to this object while calling this method.
	 */
    private static String suggestName(String bestName, int suggestionCounter) {
        if (bestName == null) bestName = "Nameless";
        if (suggestionCounter == 1) return bestName;
        int index = bestName.indexOf(".");
        if (index == -1) return bestName + "_" + suggestionCounter;
        return bestName.substring(0, index) + "_" + suggestionCounter + bestName.substring(index);
    }

    /**
	 * Synchronize to this object while calling this method.
	 */
    private boolean isNameInUse(String name) {
        return nameToChildMap.containsKey(name);
    }

    /**
	 * Synchronize to this object while calling this method.
	 */
    private String getUnusedName(String suggestedName) {
        String name;
        int suggestionCounter = 1;
        while (true) {
            name = suggestName(suggestedName, suggestionCounter);
            if (!isNameInUse(name)) return name;
            suggestionCounter++;
        }
    }

    @Override
    public boolean isDirectoryLike() {
        return true;
    }

    /**
	 * Do this method call and the iteration in a synchronized block which
	 * synchronize on this object.
	 * 
	 * @return an {@link Iterator} for the iteration over the children. The
	 *         {@link Iterator} behaves like the iterator described in
	 *         {@link ConcurrentSkipListMap#entrySet()}.
	 */
    @Override
    public Iterator<Child> iterator() {
        return protectedChilds.iterator();
    }

    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public void setUploadEnabled(boolean newValue) {
        if (newValue != uploadEnabled) {
            uploadEnabled = newValue;
            uploadEnabledListeners.fireChangeEvent();
        }
    }

    public void registerUploadEnabledListener(ChangeListener listener) {
        uploadEnabledListeners.registerListener(listener);
    }

    public void unregisterUploadEnabledListener(ChangeListener listener) {
        uploadEnabledListeners.unregisterListener(listener);
    }

    public void toggleUploadEnabled() {
        setUploadEnabled(!uploadEnabled);
    }

    @Override
    public void registerStorageEventListener(StorageEventListener<ResourceTreeSnapshot> listener) {
        storageEventListeners.registerListener(listener);
    }

    @Override
    public void unregisterStorageEventListener(StorageEventListener<ResourceTreeSnapshot> listener) {
        storageEventListeners.unregisterListener(listener);
    }

    public int getNumberOfVisibleChilds() {
        return numberOfVisibleChilds;
    }

    public File GetLastSourceDirectory() {
        return lastSourceDirectory;
    }

    public void setLastSourceDirectory(File lastSourceDirectory) {
        this.lastSourceDirectory = lastSourceDirectory;
        storageEventListeners.fireEvent(new SetVirtualFolderSourceEvent(lastSourceDirectory));
    }
}
