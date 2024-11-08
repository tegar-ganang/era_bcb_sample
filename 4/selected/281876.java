package org.middleheaven.io.repository;

import java.io.IOException;
import java.util.Iterator;
import org.middleheaven.io.IOUtils;
import org.middleheaven.io.ManagedIOException;
import org.middleheaven.io.repository.watch.WatchEvent.Kind;
import org.middleheaven.io.repository.watch.WatchEventChannel;
import org.middleheaven.io.repository.watch.WatchService;
import org.middleheaven.util.classification.Classifier;
import org.middleheaven.util.collections.AbstractEnumerableAdapter;
import org.middleheaven.util.collections.EnhancedCollection;
import org.middleheaven.util.collections.EnhancedMap;
import org.middleheaven.util.collections.Enumerable;
import org.middleheaven.util.collections.Walker;

/**
 * Default implementation of a {@link ManagedFile} usefull for extention.
 */
public abstract class AbstractManagedFile implements ManagedFile {

    private ManagedFileRepository repository;

    protected AbstractManagedFile(ManagedFileRepository repository) {
        this.repository = repository;
    }

    public ManagedFileRepository getRepository() {
        return repository;
    }

    @Override
    public ManagedFile getParent() {
        return this.getRepository().retrive(this.getPath().getParent());
    }

    /**
	 * 
	 * {@inheritDoc}
	 */
    public ManagedFile retrive(String path) throws ManagedIOException {
        switch(this.getType()) {
            case FOLDER:
            case FILEFOLDER:
                return doRetriveFromFolder(path);
            case VIRTUAL:
                return this;
            case FILE:
            default:
                return null;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public ManagedFile retrive(ManagedFilePath path) throws ManagedIOException {
        return null;
    }

    /**
	 * @param path
	 * @return
	 */
    protected abstract ManagedFile doRetriveFromFolder(String path);

    @Override
    public void copyTo(ManagedFile other) throws ManagedIOException {
        try {
            if (other.getType() == ManagedFileType.FILE) {
                IOUtils.copy(this.getContent().getInputStream(), other.getContent().getOutputStream());
            } else {
                ManagedFile newFile = other.retrive(this.getPath());
                newFile.createFile();
                IOUtils.copy(this.getContent().getInputStream(), newFile.getContent().getOutputStream());
            }
        } catch (IOException ioe) {
            throw ManagedIOException.manage(ioe);
        }
    }

    @Override
    public boolean canRenameTo(String newName) {
        ManagedFile p = this.getParent();
        if (p == null) {
            return false;
        }
        return !p.retrive(p.getPath().resolve(newName)).exists();
    }

    @Override
    public void renameTo(String newName) {
        if (canRenameTo(newName)) {
            doRenameAndChangePath(this.getPath().resolveSibling(newName));
        }
    }

    /**
	 * @param resolveSibling
	 */
    protected abstract void doRenameAndChangePath(ManagedFilePath path);

    @Override
    public final ManagedFile createFile() {
        switch(this.getType()) {
            case FILE:
            case FILEFOLDER:
                return this;
            case VIRTUAL:
                return doCreateFile();
            default:
                throw new UnsupportedOperationException("Cannot create file of type " + this.getType());
        }
    }

    /**
	 * 
	 * {@inheritDoc}
	 */
    public final boolean contains(ManagedFile other) {
        switch(this.getType()) {
            case FILEFOLDER:
            case FOLDER:
                return doContains(other);
            default:
                return false;
        }
    }

    protected abstract boolean doContains(ManagedFile other);

    /**
	 * {@inheritDoc}
	 */
    @Override
    public final WatchEventChannel register(WatchService watchService, Kind... events) {
        if (this.isWatchable()) {
            return watchService.watch(this, events);
        }
        throw new UnsupportedOperationException("This file is not watchable");
    }

    @Override
    public final ManagedFile createFolder() {
        switch(this.getType()) {
            case FOLDER:
            case FILEFOLDER:
                return this;
            case VIRTUAL:
                return doCreateFolder();
            default:
                throw new UnsupportedOperationException("Cannot create folder of type " + this.getType());
        }
    }

    protected abstract ManagedFile doCreateFile();

    protected abstract ManagedFile doCreateFolder();

    /**
	 * @return
	 */
    public Enumerable<ManagedFile> children() {
        return new ManagedFileEnumerable();
    }

    @Override
    public void eachParent(Walker<ManagedFile> walker) {
        if (this.getParent() != null) {
            walker.doWith(this.getParent());
            this.getParent().eachParent(walker);
        }
    }

    @Override
    public void eachRecursive(Walker<ManagedFile> walker) {
        for (ManagedFile file : this.childrenIterable()) {
            walker.doWith(file);
            file.eachRecursive(walker);
        }
    }

    @Override
    public void each(Walker<ManagedFile> walker) {
        for (ManagedFile file : this.childrenIterable()) {
            walker.doWith(file);
        }
    }

    /**
	 * @return
	 */
    protected abstract Iterable<ManagedFile> childrenIterable();

    protected abstract int childrenCount();

    private class ManagedFileEnumerable extends AbstractEnumerableAdapter<ManagedFile> {

        /**
		 * {@inheritDoc}
		 */
        @Override
        public int size() {
            return childrenCount();
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public Iterator<ManagedFile> iterator() {
            return childrenIterable().iterator();
        }
    }
}
