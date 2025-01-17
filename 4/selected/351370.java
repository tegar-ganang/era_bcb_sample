package org.apache.commons.vfs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.vfs.Capability;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileContentInfoFactory;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileUtil;
import org.apache.commons.vfs.NameScope;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.operations.DefaultFileOperations;
import org.apache.commons.vfs.operations.FileOperations;
import org.apache.commons.vfs.util.FileObjectUtils;
import org.apache.commons.vfs.util.RandomAccessMode;

/**
 * A partial file object implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @author Gary D. Gregory
 * @version $Revision: 537944 $ $Date: 2007-05-14 11:41:51 -0700 (Mon, 14 May 2007) $
 * @todo Chop this class up - move all the protected methods to several
 * interfaces, so that structure and content can be separately overridden.
 * @todo Check caps in methods like getChildren(), etc, and give better error messages
 * (eg 'this file type does not support listing children', vs 'this is not a folder')
 */
public abstract class AbstractFileObject implements FileObject {

    private static final FileName[] EMPTY_FILE_ARRAY = {};

    private final AbstractFileName name;

    private final AbstractFileSystem fs;

    private FileContent content;

    private boolean attached;

    private FileType type;

    private FileObject parent;

    private FileName[] children;

    private List objects;

    /**
     * FileServices instance.
     */
    private FileOperations operations;

    protected AbstractFileObject(final FileName name, final AbstractFileSystem fs) {
        this.name = (AbstractFileName) name;
        this.fs = fs;
        fs.fileObjectHanded(this);
    }

    /**
     * Attaches this file object to its file resource.  This method is called
     * before any of the doBlah() or onBlah() methods.  Sub-classes can use
     * this method to perform lazy initialisation.
     * <p/>
     * This implementation does nothing.
     */
    protected void doAttach() throws Exception {
    }

    /**
     * Detaches this file object from its file resource.
     * <p/>
     * <p>Called when this file is closed.  Note that the file object may be
     * reused later, so should be able to be reattached.
     * <p/>
     * This implementation does nothing.
     */
    protected void doDetach() throws Exception {
    }

    /**
     * Determines the type of this file.  Must not return null.  The return
     * value of this method is cached, so the implementation can be expensive.
     */
    protected abstract FileType doGetType() throws Exception;

    /**
     * Determines if this file is hidden.  Is only called if {@link #doGetType}
     * does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation always returns false.
     */
    protected boolean doIsHidden() throws Exception {
        return false;
    }

    /**
     * Determines if this file can be read.  Is only called if {@link #doGetType}
     * does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation always returns true.
     */
    protected boolean doIsReadable() throws Exception {
        return true;
    }

    /**
     * Determines if this file can be written to.  Is only called if
     * {@link #doGetType} does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation always returns true.
     */
    protected boolean doIsWriteable() throws Exception {
        return true;
    }

    /**
     * Lists the children of this file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.
     */
    protected abstract String[] doListChildren() throws Exception;

    /**
     * Lists the children of this file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.  The return value of this method
     * is cached, so the implementation can be expensive.<br>
     * Other than <code>doListChildren</code> you could return FileObject's to e.g. reinitialize the type of the file.<br>
     * (Introduced for Webdav: "permission denied on resource" during getType())
     */
    protected FileObject[] doListChildrenResolved() throws Exception {
        return null;
    }

    /**
     * Deletes the file.  Is only called when:
     * <ul>
     * <li>{@link #doGetType} does not return {@link FileType#IMAGINARY}.
     * <li>{@link #doIsWriteable} returns true.
     * <li>This file has no children, if a folder.
     * </ul>
     * <p/>
     * This implementation throws an exception.
     */
    protected void doDelete() throws Exception {
        throw new FileSystemException("vfs.provider/delete-not-supported.error");
    }

    /**
     * Renames the file.  Is only called when:
     * <ul>
     * <li>{@link #doIsWriteable} returns true.
     * </ul>
     * <p/>
     * This implementation throws an exception.
     */
    protected void doRename(FileObject newfile) throws Exception {
        throw new FileSystemException("vfs.provider/rename-not-supported.error");
    }

    /**
     * Creates this file as a folder.  Is only called when:
     * <ul>
     * <li>{@link #doGetType} returns {@link FileType#IMAGINARY}.
     * <li>The parent folder exists and is writeable, or this file is the
     * root of the file system.
     * </ul>
     * <p/>
     * This implementation throws an exception.
     */
    protected void doCreateFolder() throws Exception {
        throw new FileSystemException("vfs.provider/create-folder-not-supported.error");
    }

    /**
     * Called when the children of this file change.  Allows subclasses to
     * refresh any cached information about the children of this file.
     * <p/>
     * This implementation does nothing.
     */
    protected void onChildrenChanged(FileName child, FileType newType) throws Exception {
    }

    /**
     * Called when the type or content of this file changes.
     * <p/>
     * This implementation does nothing.
     */
    protected void onChange() throws Exception {
    }

    /**
     * Returns the last modified time of this file.  Is only called if
     * {@link #doGetType} does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation throws an exception.
     */
    protected long doGetLastModifiedTime() throws Exception {
        throw new FileSystemException("vfs.provider/get-last-modified-not-supported.error");
    }

    /**
	 * Sets the last modified time of this file.  Is only called if
	 * {@link #doGetType} does not return {@link FileType#IMAGINARY}.
	 * <p/>
	 * This implementation throws an exception.
	 *
	 * @return false if it was not possible to change the time
	 */
    protected boolean doSetLastModTime(final long modtime) throws Exception {
        doSetLastModifiedTime(modtime);
        return true;
    }

    /**
     * Sets the last modified time of this file.  Is only called if
     * {@link #doGetType} does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation throws an exception.
	 *
	 * @deprecated use {@link #doSetLastModTime}
     */
    protected void doSetLastModifiedTime(final long modtime) throws Exception {
        throw new FileSystemException("vfs.provider/set-last-modified-not-supported.error");
    }

    /**
     * Returns the attributes of this file.  Is only called if {@link #doGetType}
     * does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation always returns an empty map.
     */
    protected Map doGetAttributes() throws Exception {
        return Collections.EMPTY_MAP;
    }

    /**
     * Sets an attribute of this file.  Is only called if {@link #doGetType}
     * does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation throws an exception.
     */
    protected void doSetAttribute(final String atttrName, final Object value) throws Exception {
        throw new FileSystemException("vfs.provider/set-attribute-not-supported.error");
    }

    /**
     * Removes an attribute of this file.  Is only called if {@link #doGetType}
     * does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation throws an exception.
	 * @returns true if removing the attribute succeed. In this case we remove the attribute from
	 * our cache
     */
    protected void doRemoveAttribute(final String atttrName) throws Exception {
        throw new FileSystemException("vfs.provider/remove-attribute-not-supported.error");
    }

    /**
     * Returns the certificates used to sign this file.  Is only called if
     * {@link #doGetType} does not return {@link FileType#IMAGINARY}.
     * <p/>
     * This implementation always returns null.
     */
    protected Certificate[] doGetCertificates() throws Exception {
        return null;
    }

    /**
     * Returns the size of the file content (in bytes).  Is only called if
     * {@link #doGetType} returns {@link FileType#FILE}.
     */
    protected abstract long doGetContentSize() throws Exception;

    /**
     * Creates an input stream to read the file content from.  Is only called
     * if {@link #doGetType} returns {@link FileType#FILE}.
     * <p/>
     * <p>It is guaranteed that there are no open output streams for this file
     * when this method is called.
     * <p/>
     * <p>The returned stream does not have to be buffered.
     */
    protected abstract InputStream doGetInputStream() throws Exception;

    /**
     * Creates access to the file for random i/o.  Is only called
     * if {@link #doGetType} returns {@link FileType#FILE}.
     * <p/>
     * <p>It is guaranteed that there are no open output streams for this file
     * when this method is called.
     * <p/>
     */
    protected RandomAccessContent doGetRandomAccessContent(final RandomAccessMode mode) throws Exception {
        throw new FileSystemException("vfs.provider/random-access-not-supported.error");
    }

    /**
     * Creates an output stream to write the file content to.  Is only
     * called if:
     * <ul>
     * <li>{@link #doIsWriteable} returns true.
     * <li>{@link #doGetType} returns {@link FileType#FILE}, or
     * {@link #doGetType} returns {@link FileType#IMAGINARY}, and the file's
     * parent exists and is a folder.
     * </ul>
     * <p/>
     * <p>It is guaranteed that there are no open stream (input or output) for
     * this file when this method is called.
     * <p/>
     * <p>The returned stream does not have to be buffered.
     * <p/>
     * This implementation throws an exception.
     */
    protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
        throw new FileSystemException("vfs.provider/write-not-supported.error");
    }

    /**
     * Returns the URI of the file.
     */
    public String toString() {
        return name.getURI();
    }

    /**
     * Returns the name of the file.
     */
    public FileName getName() {
        return name;
    }

    /**
     * Returns the file system this file belongs to.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns a URL representation of the file.
     */
    public URL getURL() throws FileSystemException {
        final StringBuffer buf = new StringBuffer();
        try {
            return (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws MalformedURLException {
                    return new URL(UriParser.extractScheme(name.getURI(), buf), "", -1, buf.toString(), new DefaultURLStreamHandler(fs.getContext(), fs.getFileSystemOptions()));
                }
            });
        } catch (final PrivilegedActionException e) {
            throw new FileSystemException("vfs.provider/get-url.error", name, e.getException());
        }
    }

    /**
     * Determines if the file exists.
     */
    public boolean exists() throws FileSystemException {
        return (getType() != FileType.IMAGINARY);
    }

    /**
     * Returns the file's type.
     */
    public FileType getType() throws FileSystemException {
        synchronized (fs) {
            attach();
            return type;
        }
    }

    /**
     * Determines if this file can be read.
     */
    public boolean isHidden() throws FileSystemException {
        try {
            if (exists()) {
                return doIsHidden();
            } else {
                return false;
            }
        } catch (final Exception exc) {
            throw new FileSystemException("vfs.provider/check-is-hidden.error", name, exc);
        }
    }

    /**
     * Determines if this file can be read.
     */
    public boolean isReadable() throws FileSystemException {
        try {
            if (exists()) {
                return doIsReadable();
            } else {
                return false;
            }
        } catch (final Exception exc) {
            throw new FileSystemException("vfs.provider/check-is-readable.error", name, exc);
        }
    }

    /**
     * Determines if this file can be written to.
     */
    public boolean isWriteable() throws FileSystemException {
        try {
            if (exists()) {
                return doIsWriteable();
            } else {
                final FileObject parent = getParent();
                if (parent != null) {
                    return parent.isWriteable();
                }
                return true;
            }
        } catch (final Exception exc) {
            throw new FileSystemException("vfs.provider/check-is-writeable.error", name, exc);
        }
    }

    /**
     * Returns the parent of the file.
     */
    public FileObject getParent() throws FileSystemException {
        if (this == fs.getRoot()) {
            if (fs.getParentLayer() != null) {
                return fs.getParentLayer().getParent();
            } else {
                return null;
            }
        }
        synchronized (fs) {
            if (parent == null) {
                FileName parentName = name.getParent();
                if (parentName != null) {
                    parent = (FileObject) fs.resolveFile(parentName);
                }
            }
        }
        return parent;
    }

    /**
     * Returns the children of the file.
     */
    public FileObject[] getChildren() throws FileSystemException {
        synchronized (fs) {
            if (!getType().hasChildren()) {
                throw new FileSystemException("vfs.provider/list-children-not-folder.error", name);
            }
            if (children != null) {
                return resolveFiles(children);
            }
            FileObject[] childrenObjects;
            try {
                childrenObjects = doListChildrenResolved();
                children = extractNames(childrenObjects);
            } catch (Exception exc) {
                throw new FileSystemException("vfs.provider/list-children.error", new Object[] { name }, exc);
            }
            if (childrenObjects != null) {
                return childrenObjects;
            }
            final String[] files;
            try {
                files = doListChildren();
            } catch (Exception exc) {
                throw new FileSystemException("vfs.provider/list-children.error", new Object[] { name }, exc);
            }
            if (files == null) {
                return null;
            } else if (files.length == 0) {
                children = EMPTY_FILE_ARRAY;
            } else {
                children = new FileName[files.length];
                for (int i = 0; i < files.length; i++) {
                    final String file = files[i];
                    children[i] = getFileSystem().getFileSystemManager().resolveName(name, file, NameScope.CHILD);
                }
            }
            return resolveFiles(children);
        }
    }

    private FileName[] extractNames(FileObject[] objects) {
        if (objects == null) {
            return null;
        }
        FileName[] names = new FileName[objects.length];
        for (int iterObjects = 0; iterObjects < objects.length; iterObjects++) {
            names[iterObjects] = objects[iterObjects].getName();
        }
        return names;
    }

    private FileObject[] resolveFiles(FileName[] children) throws FileSystemException {
        if (children == null) {
            return null;
        }
        FileObject[] objects = new FileObject[children.length];
        for (int iterChildren = 0; iterChildren < children.length; iterChildren++) {
            objects[iterChildren] = resolveFile(children[iterChildren]);
        }
        return objects;
    }

    private FileObject resolveFile(FileName child) throws FileSystemException {
        return fs.resolveFile(child);
    }

    /**
     * Returns a child of this file.
     */
    public FileObject getChild(final String name) throws FileSystemException {
        FileObject[] children = getChildren();
        for (int i = 0; i < children.length; i++) {
            final FileName child = children[i].getName();
            if (child.getBaseName().equals(name)) {
                return resolveFile(child);
            }
        }
        return null;
    }

    /**
     * Returns a child by name.
     */
    public FileObject resolveFile(final String name, final NameScope scope) throws FileSystemException {
        return fs.resolveFile(getFileSystem().getFileSystemManager().resolveName(this.name, name, scope));
    }

    /**
     * Finds a file, relative to this file.
     *
     * @param path The path of the file to locate.  Can either be a relative
     *             path, which is resolved relative to this file, or an
     *             absolute path, which is resolved relative to the file system
     *             that contains this file.
     */
    public FileObject resolveFile(final String path) throws FileSystemException {
        final FileName otherName = getFileSystem().getFileSystemManager().resolveName(name, path);
        return fs.resolveFile(otherName);
    }

    /**
     * Deletes this file, once all its children have been deleted
     *
     * @return true if this file has been deleted
     */
    private boolean deleteSelf() throws FileSystemException {
        synchronized (fs) {
            if (getType() == FileType.IMAGINARY) {
                return false;
            }
            try {
                doDelete();
                handleDelete();
            } catch (final RuntimeException re) {
                throw re;
            } catch (final Exception exc) {
                throw new FileSystemException("vfs.provider/delete.error", new Object[] { name }, exc);
            }
            return true;
        }
    }

    /**
     * Deletes this file.
     *
     * @return true if this object has been deleted
     * @todo This will not fail if this is a non-empty folder.
     */
    public boolean delete() throws FileSystemException {
        return delete(Selectors.SELECT_SELF) > 0;
    }

    /**
     * Deletes this file, and all children.
     *
     * @return the number of deleted files
     */
    public int delete(final FileSelector selector) throws FileSystemException {
        int nuofDeleted = 0;
        if (getType() == FileType.IMAGINARY) {
            return nuofDeleted;
        }
        ArrayList files = new ArrayList();
        findFiles(selector, true, files);
        final int count = files.size();
        for (int i = 0; i < count; i++) {
            final AbstractFileObject file = FileObjectUtils.getAbstractFileObject((FileObject) files.get(i));
            if (file.getType().hasChildren() && file.getChildren().length != 0) {
                continue;
            }
            boolean deleted = file.deleteSelf();
            if (deleted) {
                nuofDeleted++;
            }
        }
        return nuofDeleted;
    }

    /**
     * Creates this file, if it does not exist.
     */
    public void createFile() throws FileSystemException {
        synchronized (fs) {
            try {
                if (exists() && !FileType.FILE.equals(getType())) {
                    throw new FileSystemException("vfs.provider/create-file.error", name);
                }
                if (!exists()) {
                    getOutputStream().close();
                    endOutput();
                }
            } catch (final RuntimeException re) {
                throw re;
            } catch (final Exception e) {
                throw new FileSystemException("vfs.provider/create-file.error", name, e);
            }
        }
    }

    /**
     * Creates this folder, if it does not exist.  Also creates any ancestor
     * files which do not exist.
     */
    public void createFolder() throws FileSystemException {
        synchronized (fs) {
            if (getType().hasChildren()) {
                return;
            }
            if (getType() != FileType.IMAGINARY) {
                throw new FileSystemException("vfs.provider/create-folder-mismatched-type.error", name);
            }
            if (!isWriteable()) {
                throw new FileSystemException("vfs.provider/create-folder-read-only.error", name);
            }
            final FileObject parent = getParent();
            if (parent != null) {
                parent.createFolder();
            }
            try {
                doCreateFolder();
                handleCreate(FileType.FOLDER);
            } catch (final RuntimeException re) {
                throw re;
            } catch (final Exception exc) {
                throw new FileSystemException("vfs.provider/create-folder.error", name, exc);
            }
        }
    }

    /**
     * Copies another file to this file.
     */
    public void copyFrom(final FileObject file, final FileSelector selector) throws FileSystemException {
        if (!file.exists()) {
            throw new FileSystemException("vfs.provider/copy-missing-file.error", file);
        }
        if (!isWriteable()) {
            throw new FileSystemException("vfs.provider/copy-read-only.error", new Object[] { file.getType(), file.getName(), this }, null);
        }
        final ArrayList files = new ArrayList();
        file.findFiles(selector, false, files);
        final int count = files.size();
        for (int i = 0; i < count; i++) {
            final FileObject srcFile = (FileObject) files.get(i);
            final String relPath = file.getName().getRelativeName(srcFile.getName());
            final FileObject destFile = resolveFile(relPath, NameScope.DESCENDENT_OR_SELF);
            if (destFile.exists() && destFile.getType() != srcFile.getType()) {
                destFile.delete(Selectors.SELECT_ALL);
            }
            try {
                if (srcFile.getType().hasContent()) {
                    FileUtil.copyContent(srcFile, destFile);
                } else if (srcFile.getType().hasChildren()) {
                    destFile.createFolder();
                }
            } catch (final IOException e) {
                throw new FileSystemException("vfs.provider/copy-file.error", new Object[] { srcFile, destFile }, e);
            }
        }
    }

    /**
     * Moves (rename) the file to another one
     */
    public void moveTo(FileObject destFile) throws FileSystemException {
        if (canRenameTo(destFile)) {
            if (!getParent().isWriteable()) {
                throw new FileSystemException("vfs.provider/rename-parent-read-only.error", new FileName[] { getName(), getParent().getName() });
            }
        } else {
            if (!isWriteable()) {
                throw new FileSystemException("vfs.provider/rename-read-only.error", getName());
            }
        }
        if (destFile.exists() && !isSameFile(destFile)) {
            destFile.delete(Selectors.SELECT_ALL);
        }
        if (canRenameTo(destFile)) {
            try {
                attach();
                doRename(destFile);
                (FileObjectUtils.getAbstractFileObject(destFile)).handleCreate(getType());
                destFile.close();
                handleDelete();
            } catch (final RuntimeException re) {
                throw re;
            } catch (final Exception exc) {
                throw new FileSystemException("vfs.provider/rename.error", new Object[] { getName(), destFile.getName() }, exc);
            }
        } else {
            destFile.copyFrom(this, Selectors.SELECT_SELF);
            if (((destFile.getType().hasContent() && destFile.getFileSystem().hasCapability(Capability.SET_LAST_MODIFIED_FILE)) || (destFile.getType().hasChildren() && destFile.getFileSystem().hasCapability(Capability.SET_LAST_MODIFIED_FOLDER))) && getFileSystem().hasCapability(Capability.GET_LAST_MODIFIED)) {
                destFile.getContent().setLastModifiedTime(this.getContent().getLastModifiedTime());
            }
            deleteSelf();
        }
    }

    /**
     * Checks if this fileObject is the same file as <code>destFile</code> just with a different
     * name.<br />
     * E.g. for case insensitive filesystems like windows.
     */
    protected boolean isSameFile(FileObject destFile) throws FileSystemException {
        attach();
        return doIsSameFile(destFile);
    }

    /**
     * Checks if this fileObject is the same file as <code>destFile</code> just with a different
     * name.<br />
     * E.g. for case insensitive filesystems like windows.
     */
    protected boolean doIsSameFile(FileObject destFile) throws FileSystemException {
        return false;
    }

    /**
     * Queries the object if a simple rename to the filename of <code>newfile</code>
     * is possible.
     *
     * @param newfile the new filename
     * @return true if rename is possible
     */
    public boolean canRenameTo(FileObject newfile) {
        if (getFileSystem() == newfile.getFileSystem()) {
            return true;
        }
        return false;
    }

    /**
     * Finds the set of matching descendents of this file, in depthwise
     * order.
     *
     * @return list of files or null if the base file (this object) do not exist
     */
    public FileObject[] findFiles(final FileSelector selector) throws FileSystemException {
        if (!exists()) {
            return null;
        }
        final ArrayList list = new ArrayList();
        findFiles(selector, true, list);
        return (FileObject[]) list.toArray(new FileObject[list.size()]);
    }

    /**
     * Returns the file's content.
     */
    public FileContent getContent() throws FileSystemException {
        synchronized (fs) {
            attach();
            if (content == null) {
                content = doCreateFileContent();
            }
            return content;
        }
    }

    /**
	 * Create a FileContent implementation
	 */
    protected FileContent doCreateFileContent() throws FileSystemException {
        return new DefaultFileContent(this, getFileContentInfoFactory());
    }

    /**
     * This will prepare the fileObject to get resynchronized with the underlaying filesystem if required
     */
    public void refresh() throws FileSystemException {
        try {
            detach();
        } catch (final Exception e) {
            throw new FileSystemException("vfs.provider/resync.error", name, e);
        }
    }

    /**
     * Closes this file, and its content.
     */
    public void close() throws FileSystemException {
        FileSystemException exc = null;
        if (content != null) {
            try {
                content.close();
                content = null;
            } catch (FileSystemException e) {
                exc = e;
            }
        }
        try {
            detach();
        } catch (final Exception e) {
            exc = new FileSystemException("vfs.provider/close.error", name, e);
        }
        if (exc != null) {
            throw exc;
        }
    }

    /**
     * Returns an input stream to use to read the content of the file.
     */
    public InputStream getInputStream() throws FileSystemException {
        if (!getType().hasContent()) {
            throw new FileSystemException("vfs.provider/read-not-file.error", name);
        }
        if (!isReadable()) {
            throw new FileSystemException("vfs.provider/read-not-readable.error", name);
        }
        try {
            return doGetInputStream();
        } catch (final Exception exc) {
            throw new FileSystemException("vfs.provider/read.error", name, exc);
        }
    }

    /**
     * Returns an input/output stream to use to read and write the content of the file in and
     * random manner.
     */
    public RandomAccessContent getRandomAccessContent(final RandomAccessMode mode) throws FileSystemException {
        if (!getType().hasContent()) {
            throw new FileSystemException("vfs.provider/read-not-file.error", name);
        }
        if (mode.requestRead()) {
            if (!getFileSystem().hasCapability(Capability.RANDOM_ACCESS_READ)) {
                throw new FileSystemException("vfs.provider/random-access-read-not-supported.error");
            }
            if (!isReadable()) {
                throw new FileSystemException("vfs.provider/read-not-readable.error", name);
            }
        }
        if (mode.requestWrite()) {
            if (!getFileSystem().hasCapability(Capability.RANDOM_ACCESS_WRITE)) {
                throw new FileSystemException("vfs.provider/random-access-write-not-supported.error");
            }
            if (!isWriteable()) {
                throw new FileSystemException("vfs.provider/write-read-only.error", name);
            }
        }
        try {
            return doGetRandomAccessContent(mode);
        } catch (final Exception exc) {
            throw new FileSystemException("vfs.provider/random-access.error", name, exc);
        }
    }

    /**
     * Prepares this file for writing.  Makes sure it is either a file,
     * or its parent folder exists.  Returns an output stream to use to
     * write the content of the file to.
     */
    public OutputStream getOutputStream() throws FileSystemException {
        return getOutputStream(false);
    }

    /**
     * Prepares this file for writing.  Makes sure it is either a file,
     * or its parent folder exists.  Returns an output stream to use to
     * write the content of the file to.<br>
     *
     * @param bAppend true when append to the file.<br>
     *                Note: If the underlaying filesystem do not support this, it wont work.
     */
    public OutputStream getOutputStream(boolean bAppend) throws FileSystemException {
        if (getType() != FileType.IMAGINARY && !getType().hasContent()) {
            throw new FileSystemException("vfs.provider/write-not-file.error", name);
        }
        if (!isWriteable()) {
            throw new FileSystemException("vfs.provider/write-read-only.error", name);
        }
        if (bAppend && !getFileSystem().hasCapability(Capability.APPEND_CONTENT)) {
            throw new FileSystemException("vfs.provider/write-append-not-supported.error", name);
        }
        if (getType() == FileType.IMAGINARY) {
            FileObject parent = getParent();
            if (parent != null) {
                parent.createFolder();
            }
        }
        try {
            return doGetOutputStream(bAppend);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exc) {
            throw new FileSystemException("vfs.provider/write.error", new Object[] { name }, exc);
        }
    }

    /**
     * Detaches this file, invaliating all cached info.  This will force
     * a call to {@link #doAttach} next time this file is used.
     */
    private void detach() throws Exception {
        synchronized (fs) {
            if (attached) {
                try {
                    doDetach();
                } finally {
                    attached = false;
                    setFileType(null);
                    parent = null;
                    removeChildrenCache();
                }
            }
        }
    }

    private void removeChildrenCache() {
        children = null;
    }

    /**
     * Attaches to the file.
     */
    private void attach() throws FileSystemException {
        synchronized (fs) {
            if (attached) {
                return;
            }
            try {
                doAttach();
                attached = true;
                if (type == null) {
                    setFileType(doGetType());
                }
                if (type == null) {
                    setFileType(FileType.IMAGINARY);
                }
            } catch (Exception exc) {
                throw new FileSystemException("vfs.provider/get-type.error", new Object[] { name }, exc);
            }
        }
    }

    /**
     * Called when the ouput stream for this file is closed.
     */
    protected void endOutput() throws Exception {
        if (getType() == FileType.IMAGINARY) {
            handleCreate(FileType.FILE);
        } else {
            onChange();
        }
    }

    /**
     * Called when this file is created.  Updates cached info and notifies
     * the parent and file system.
     */
    protected void handleCreate(final FileType newType) throws Exception {
        synchronized (fs) {
            if (attached) {
                injectType(newType);
                removeChildrenCache();
                onChange();
            }
            notifyParent(this.getName(), newType);
            fs.fireFileCreated(this);
        }
    }

    /**
     * Called when this file is deleted.  Updates cached info and notifies
     * subclasses, parent and file system.
     */
    protected void handleDelete() throws Exception {
        synchronized (fs) {
            if (attached) {
                injectType(FileType.IMAGINARY);
                removeChildrenCache();
                onChange();
            }
            notifyParent(this.getName(), FileType.IMAGINARY);
            fs.fireFileDeleted(this);
        }
    }

    /**
     * Called when this file is changed.<br />
     * This will only happen if you monitor the file using {@link org.apache.commons.vfs.FileMonitor}.
     */
    protected void handleChanged() throws Exception {
        fs.fireFileChanged(this);
    }

    /**
     * Notifies the file that its children have changed.
     *
     * @deprecated use {@link #childrenChanged(FileName,FileType)}
     */
    protected void childrenChanged() throws Exception {
        childrenChanged(null, null);
    }

    /**
     * Notifies the file that its children have changed.
     */
    protected void childrenChanged(FileName childName, FileType newType) throws Exception {
        if (children != null) {
            if (childName != null && newType != null) {
                ArrayList list = new ArrayList(Arrays.asList(children));
                if (newType.equals(FileType.IMAGINARY)) {
                    list.remove(childName);
                } else {
                    list.add(childName);
                }
                children = new FileName[list.size()];
                list.toArray(children);
            }
        }
        onChildrenChanged(childName, newType);
    }

    /**
     * Notify the parent of a change to its children, when a child is created
     * or deleted.
     */
    private void notifyParent(FileName childName, FileType newType) throws Exception {
        if (parent == null) {
            FileName parentName = name.getParent();
            if (parentName != null) {
                parent = fs.getFileFromCache(parentName);
            }
        }
        if (parent != null) {
            FileObjectUtils.getAbstractFileObject(parent).childrenChanged(childName, newType);
        }
    }

    /**
     * Traverses the descendents of this file, and builds a list of selected
     * files.
     */
    public void findFiles(final FileSelector selector, final boolean depthwise, final List selected) throws FileSystemException {
        try {
            if (exists()) {
                final DefaultFileSelectorInfo info = new DefaultFileSelectorInfo();
                info.setBaseFolder(this);
                info.setDepth(0);
                info.setFile(this);
                traverse(info, selector, depthwise, selected);
            }
        } catch (final Exception e) {
            throw new FileSystemException("vfs.provider/find-files.error", name, e);
        }
    }

    /**
     * Traverses a file.
     */
    private static void traverse(final DefaultFileSelectorInfo fileInfo, final FileSelector selector, final boolean depthwise, final List selected) throws Exception {
        final FileObject file = fileInfo.getFile();
        final int index = selected.size();
        if (file.getType().hasChildren() && selector.traverseDescendents(fileInfo)) {
            final int curDepth = fileInfo.getDepth();
            fileInfo.setDepth(curDepth + 1);
            final FileObject[] children = file.getChildren();
            for (int i = 0; i < children.length; i++) {
                final FileObject child = children[i];
                fileInfo.setFile(child);
                traverse(fileInfo, selector, depthwise, selected);
            }
            fileInfo.setFile(file);
            fileInfo.setDepth(curDepth);
        }
        if (selector.includeFile(fileInfo)) {
            if (depthwise) {
                selected.add(file);
            } else {
                selected.add(index, file);
            }
        }
    }

    /**
     * Check if the content stream is open
     *
     * @return true if this is the case
     */
    public boolean isContentOpen() {
        if (content == null) {
            return false;
        }
        return content.isOpen();
    }

    /**
     * Check if the internal state is "attached"
     *
     * @return true if this is the case
     */
    public boolean isAttached() {
        return attached;
    }

    /**
     * create the filecontentinfo implementation
     */
    protected FileContentInfoFactory getFileContentInfoFactory() {
        return getFileSystem().getFileSystemManager().getFileContentInfoFactory();
    }

    protected void injectType(FileType fileType) {
        setFileType(fileType);
    }

    private void setFileType(FileType type) {
        if (type != null && type != FileType.IMAGINARY) {
            try {
                name.setType(type);
            } catch (FileSystemException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        this.type = type;
    }

    /**
     * This method is meant to add a object where this object holds a strong reference then.
     * E.g. a archive-filesystem creates a list of all childs and they shouldnt get
     * garbage collected until the container is garbage collected
     *
     * @param strongRef
     */
    public void holdObject(Object strongRef) {
        if (objects == null) {
            objects = new ArrayList(5);
        }
        objects.add(strongRef);
    }

    /**
     * will be called after this file-object closed all its streams.
     */
    protected void notifyAllStreamsClosed() {
    }

    /**
     * @return FileOperations interface that provides access to the operations
     *         API.
     * @throws FileSystemException
     */
    public FileOperations getFileOperations() throws FileSystemException {
        if (operations == null) {
            operations = new DefaultFileOperations(this);
        }
        return operations;
    }

    protected void finalize() throws Throwable {
        fs.fileObjectDestroyed(this);
        super.finalize();
    }
}
