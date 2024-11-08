package org.eclipse.osgi.storagemanager;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.*;
import org.eclipse.osgi.framework.internal.reliablefile.*;
import org.eclipse.osgi.framework.util.SecureAction;

public final class StorageManager {

    private static final int FILETYPE_STANDARD = 0;

    private static final int FILETYPE_RELIABLEFILE = 1;

    private static final SecureAction secure = new SecureAction();

    private static boolean tempCleanup = Boolean.valueOf(secure.getProperty("osgi.embedded.cleanTempFiles")).booleanValue();

    private static boolean openCleanup = Boolean.valueOf(secure.getProperty("osgi.embedded.cleanupOnOpen")).booleanValue();

    private static final String MANAGER_FOLDER = ".manager";

    private static final String TABLE_FILE = ".fileTable";

    private static final String LOCK_FILE = ".fileTableLock";

    private static final int MAX_LOCK_WAIT = 5000;

    private class Entry {

        int readId;

        int writeId;

        int fileType;

        Entry(int readId, int writeId, int type) {
            this.readId = readId;
            this.writeId = writeId;
            this.fileType = type;
        }

        int getReadId() {
            return readId;
        }

        int getWriteId() {
            return writeId;
        }

        int getFileType() {
            return fileType;
        }

        void setReadId(int value) {
            readId = value;
        }

        void setWriteId(int value) {
            writeId = value;
        }

        void setFileType(int type) {
            fileType = type;
        }
    }

    private File base;

    private File managerRoot;

    private String lockMode = null;

    private File tableFile = null;

    private File lockFile;

    private Locker locker;

    private File instanceFile = null;

    private Locker instanceLocker = null;

    private boolean readOnly;

    private boolean open;

    private int tableStamp = -1;

    private Properties table = new Properties();

    private boolean useReliableFiles = Boolean.valueOf(secure.getProperty("osgi.useReliableFiles")).booleanValue();

    /**
	 * Returns a new storage manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the storage manager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 */
    public StorageManager(File base, String lockMode) {
        this(base, lockMode, false);
    }

    /**
	 * Returns a new storage manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the storage manager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 * @param readOnly true if the managed files are read-only
	 */
    public StorageManager(File base, String lockMode, boolean readOnly) {
        this.base = base;
        this.lockMode = lockMode;
        this.managerRoot = new File(base, MANAGER_FOLDER);
        if (!readOnly) this.managerRoot.mkdirs();
        this.tableFile = new File(managerRoot, TABLE_FILE);
        this.lockFile = new File(managerRoot, LOCK_FILE);
        this.readOnly = readOnly;
        open = false;
    }

    private void initializeInstanceFile() throws IOException {
        if (instanceFile != null || readOnly) return;
        this.instanceFile = File.createTempFile(".tmp", ".instance", managerRoot);
        this.instanceFile.deleteOnExit();
        instanceLocker = BasicLocation.createLocker(instanceFile, lockMode);
        instanceLocker.lock();
    }

    private String getAbsolutePath(String file) {
        return new File(base, file).getAbsolutePath();
    }

    /**
	 * Add the given managed file name to the list of files managed by this manager.
	 * 
	 * @param managedFile name of the file to manage
	 * @throws IOException if there are any problems adding the given file name to the manager
	 */
    public void add(String managedFile) throws IOException {
        add(managedFile, FILETYPE_STANDARD);
    }

    private void add(String managedFile, int fileType) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            Entry entry = (Entry) table.get(managedFile);
            if (entry == null) {
                entry = new Entry(0, 1, fileType);
                table.put(managedFile, entry);
                int oldestGeneration = findOldestGeneration(managedFile);
                if (oldestGeneration != 0) entry.setWriteId(oldestGeneration + 1);
                save();
            } else {
                if (entry.getFileType() != fileType) {
                    entry.setFileType(fileType);
                    updateTable();
                    save();
                }
            }
        } finally {
            release();
        }
    }

    private int findOldestGeneration(String managedFile) {
        String[] files = base.list();
        int oldestGeneration = 0;
        if (files != null) {
            String name = managedFile + '.';
            int len = name.length();
            for (int i = 0; i < files.length; i++) {
                if (!files[i].startsWith(name)) continue;
                try {
                    int generation = Integer.parseInt(files[i].substring(len));
                    if (generation > oldestGeneration) oldestGeneration = generation;
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return oldestGeneration;
    }

    /**
	 * Update the given managed files with the content in the given source files.
	 * The managedFiles is a list of managed file names which are currently managed. 
	 * If a managed file name is not currently managed it will be added as a 
	 * managed file for this storage manager.
	 * The sources are absolute (or relative to the current working directory) 
	 * file paths containing the new content for the corresponding managed files.
	 * 
	 * @param managedFiles the managed files to update
	 * @param sources the new content for the managed files
	 * @throws IOException if there are any problems updating the given managed files
	 */
    public void update(String[] managedFiles, String[] sources) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            int[] originalReadIDs = new int[managedFiles.length];
            boolean error = false;
            for (int i = 0; i < managedFiles.length; i++) {
                originalReadIDs[i] = getId(managedFiles[i]);
                if (!update(managedFiles[i], sources[i])) error = true;
            }
            if (error) {
                for (int i = 0; i < managedFiles.length; i++) {
                    Entry entry = (Entry) table.get(managedFiles[i]);
                    entry.setReadId(originalReadIDs[i]);
                }
                throw new IOException(EclipseAdaptorMsg.fileManager_updateFailed);
            }
            save();
        } finally {
            release();
        }
    }

    /**
	 * Returns a list of all the managed files currently being managed.
	 * 
	 * @return the names of the managed files
	 */
    public String[] getManagedFiles() {
        if (!open) return null;
        Set set = table.keySet();
        String[] keys = (String[]) set.toArray(new String[set.size()]);
        String[] result = new String[keys.length];
        for (int i = 0; i < keys.length; i++) result[i] = new String(keys[i]);
        return result;
    }

    /**
	 * Returns the directory containing the files being managed by this storage
	 * manager.
	 * 
	 * @return the directory containing the managed files
	 */
    public File getBase() {
        return base;
    }

    /**
	 * Returns the current numeric id (appendage) of the given managed file.
	 * <code>managedFile + "." + getId(target)</code>. A value of -1 is returned 
	 * if the given name is not managed.
	 * 
	 * @param managedFile the name of the managed file
	 * @return the id of the managed file
	 */
    public int getId(String managedFile) {
        if (!open) return -1;
        Entry entry = (Entry) table.get(managedFile);
        if (entry == null) return -1;
        return entry.getReadId();
    }

    /**
	 * Returns if readOnly state this storage manager is using.
	 * 
	 * @return if this storage manager update state is read-only.
	 */
    public boolean isReadOnly() {
        return readOnly;
    }

    private boolean lock(boolean wait) throws IOException {
        if (readOnly) return false;
        if (locker == null) {
            locker = BasicLocation.createLocker(lockFile, lockMode);
            if (locker == null) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        }
        boolean locked = locker.lock();
        if (locked || !wait) return locked;
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            locked = locker.lock();
            if (locked) return true;
            long time = System.currentTimeMillis() - start;
            if (time > MAX_LOCK_WAIT) return false;
        }
    }

    /**
	 * Returns the actual file location to use when reading the given managed file. 
	 * A value of <code>null</code> can be returned if the given managed file name is not 
	 * managed and add is set to false.  
	 * <p>
	 * The returned file should be considered read-only.  Any updates to the content of this
	 * file should be done using {@link #update(String[], String[])}.
	 * 
	 * @param managedFile the managed file to lookup
	 * @param add indicate whether the managed file name should be added to the manager if 
	 * it is not already managed.
	 * @throws IOException if the add flag is set to true and the addition of the managed file failed
	 * @return the absolute file location to use for the given managed file or
	 *               <code>null</code> if the given managed file is not managed
	 */
    public File lookup(String managedFile, boolean add) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        Entry entry = (Entry) table.get(managedFile);
        if (entry == null) {
            if (add) {
                add(managedFile);
                entry = (Entry) table.get(managedFile);
            } else {
                return null;
            }
        }
        return new File(getAbsolutePath(managedFile + '.' + entry.getReadId()));
    }

    private boolean move(String source, String managedFile) {
        File original = new File(source);
        File targetFile = new File(managedFile);
        if (!original.exists() || targetFile.exists()) return false;
        return original.renameTo(targetFile);
    }

    /**
	 * Saves the state of the storage manager and releases any locks held.
	 */
    private void release() {
        if (locker == null) return;
        locker.release();
    }

    /**
	 * Removes the given managed file from management by this storage manager.
	 * 
	 * @param managedFile the managed file to remove
	 * @throws IOException if an error occured removing the managed file
	 */
    public void remove(String managedFile) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            table.remove(managedFile);
            save();
        } finally {
            release();
        }
    }

    private void updateTable() throws IOException {
        int stamp;
        stamp = ReliableFile.lastModifiedVersion(tableFile);
        if (stamp == tableStamp || stamp == -1) return;
        Properties diskTable = new Properties();
        try {
            InputStream input;
            input = new ReliableFileInputStream(tableFile);
            try {
                diskTable.load(input);
            } finally {
                input.close();
            }
        } catch (IOException e) {
            throw e;
        }
        tableStamp = stamp;
        for (Enumeration e = diskTable.keys(); e.hasMoreElements(); ) {
            String file = (String) e.nextElement();
            String value = diskTable.getProperty(file);
            if (value != null) {
                Entry entry = (Entry) table.get(file);
                int id;
                int fileType;
                int idx = value.indexOf(',');
                if (idx != -1) {
                    id = Integer.parseInt(value.substring(0, idx));
                    fileType = Integer.parseInt(value.substring(idx + 1));
                } else {
                    id = Integer.parseInt(value);
                    fileType = FILETYPE_STANDARD;
                }
                if (entry == null) {
                    table.put(file, new Entry(id, id + 1, fileType));
                } else {
                    entry.setWriteId(id + 1);
                }
            }
        }
    }

    private void save() throws IOException {
        if (readOnly) return;
        updateTable();
        Properties props = new Properties();
        for (Enumeration e = table.keys(); e.hasMoreElements(); ) {
            String file = (String) e.nextElement();
            Entry entry = (Entry) table.get(file);
            String value;
            if (entry.getFileType() != FILETYPE_STANDARD) {
                value = Integer.toString(entry.getWriteId() - 1) + ',' + Integer.toString(entry.getFileType());
            } else {
                value = Integer.toString(entry.getWriteId() - 1);
            }
            props.put(file, value);
        }
        ReliableFileOutputStream fileStream = new ReliableFileOutputStream(tableFile);
        try {
            boolean error = true;
            try {
                props.store(fileStream, "safe table");
                fileStream.close();
                error = false;
            } finally {
                if (error) fileStream.abort();
            }
        } catch (IOException e) {
            throw new IOException(EclipseAdaptorMsg.fileManager_couldNotSave);
        }
        tableStamp = ReliableFile.lastModifiedVersion(tableFile);
    }

    private boolean update(String managedFile, String source) throws IOException {
        Entry entry = (Entry) table.get(managedFile);
        if (entry == null) add(managedFile);
        int newId = entry.getWriteId();
        boolean success = move(getAbsolutePath(source), getAbsolutePath(managedFile) + '.' + newId);
        if (!success) {
            newId = findOldestGeneration(managedFile) + 1;
            success = move(getAbsolutePath(source), getAbsolutePath(managedFile) + '.' + newId);
        }
        if (!success) return false;
        entry.setReadId(newId);
        entry.setWriteId(newId + 1);
        return true;
    }

    /**
	 * This methods remove all the temporary files that have been created by the storage manager.
	 * This removal is only done if the instance of eclipse calling this method is the last instance using this storage manager.
	 * @throws IOException
	 */
    private void cleanup() throws IOException {
        if (readOnly) return;
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            String[] files = managerRoot.list();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].endsWith(".instance") && instanceFile != null && !files[i].equalsIgnoreCase(instanceFile.getName())) {
                        Locker tmpLocker = BasicLocation.createLocker(new File(managerRoot, files[i]), lockMode);
                        if (tmpLocker.lock()) {
                            tmpLocker.release();
                            new File(managerRoot, files[i]).delete();
                        } else {
                            tmpLocker.release();
                            return;
                        }
                    }
                }
            }
            updateTable();
            Collection managedFiles = table.entrySet();
            for (Iterator iter = managedFiles.iterator(); iter.hasNext(); ) {
                Map.Entry fileEntry = (Map.Entry) iter.next();
                String fileName = (String) fileEntry.getKey();
                Entry info = (Entry) fileEntry.getValue();
                if (info.getFileType() == FILETYPE_RELIABLEFILE) {
                    ReliableFile.cleanupGenerations(new File(base, fileName));
                } else {
                    String readId = Integer.toString(info.getWriteId() - 1);
                    deleteCopies(fileName, readId);
                }
            }
            if (tempCleanup) {
                files = base.list();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].endsWith(ReliableFile.tmpExt)) {
                            new File(base, files[i]).delete();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            release();
        }
    }

    private void deleteCopies(String fileName, String exceptionNumber) {
        String notToDelete = fileName + '.' + exceptionNumber;
        String[] files = base.list();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            if (files[i].startsWith(fileName + '.') && !files[i].equals(notToDelete)) new File(base, files[i]).delete();
        }
    }

    /**
	 * This method declares the storage manager as closed. From thereon, the instance can no longer be used.
	 * It is important to close the manager as it also cleans up old copies of the managed files.
	 */
    public void close() {
        if (!open) return;
        open = false;
        if (readOnly) return;
        try {
            cleanup();
        } catch (IOException e) {
        }
        if (instanceLocker != null) instanceLocker.release();
        if (instanceFile != null) instanceFile.delete();
    }

    /**
	 * This methods opens the storage manager. 
	 * This method must be called before any operation on the storage manager.
	 * @param wait indicates if the open operation must wait in case of contention on the lock file.
	 * @throws IOException if an error occured opening the storage manager
	 */
    public void open(boolean wait) throws IOException {
        if (openCleanup) cleanup();
        if (!readOnly) {
            boolean locked = lock(wait);
            if (!locked && wait) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        }
        try {
            initializeInstanceFile();
            updateTable();
            open = true;
        } finally {
            release();
        }
    }

    /**
	 * Creates a new unique empty temporary-file in the storage manager base directory. The file name
	 * must be at least 3 characters. This file can later be used to update a managed file.
	 * 
	 * @param file the file name to create temporary file from.
	 * @return the newly-created empty file.
	 * @throws IOException if the file can not be created.
	 * @see #update(String[], String[])
	 */
    public File createTempFile(String file) throws IOException {
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        File tmpFile = File.createTempFile(file, ReliableFile.tmpExt, base);
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    /**
	 * Returns a managed <code>InputStream</code> for a managed file. 
	 * <code>null</code> can be returned if the given name is not managed. 
	 * 
	 * @param managedFile the name of the managed file to open.
	 * @return an input stream to the managed file or 
	 * <code>null</code> if the given name is not managed.
	 * @throws IOException if the content is missing, corrupt or an error occurs.
	 */
    public InputStream getInputStream(String managedFile) throws IOException {
        return getInputStream(managedFile, ReliableFile.OPEN_BEST_AVAILABLE);
    }

    /**
	 * Returns a managed input stream set for the managed file names. 
	 * Elements of the returned set may be <code>null</code> if a given name is not managed.
	 * This method should be used for managed file sets which use the output streams returned 
	 * by the {@link #getOutputStreamSet(String[])} to save data.
	 * 
	 * @param managedFiles the names of the managed files to open.
	 * @return a set input streams to the given managed files.
	 * @throws IOException if the content of one of the managed files is missing, corrupt or an error occurs.
	 */
    public InputStream[] getInputStreamSet(String[] managedFiles) throws IOException {
        InputStream[] streams = new InputStream[managedFiles.length];
        for (int i = 0; i < streams.length; i++) streams[i] = getInputStream(managedFiles[i], ReliableFile.OPEN_FAIL_ON_PRIMARY);
        return streams;
    }

    private InputStream getInputStream(String managedFiles, int openMask) throws IOException {
        if (useReliableFiles) {
            int id = getId(managedFiles);
            if (id == -1) return null;
            return new ReliableFileInputStream(new File(getBase(), managedFiles), id, openMask);
        }
        File lookup = lookup(managedFiles, false);
        if (lookup == null) return null;
        return new FileInputStream(lookup);
    }

    /**
	 * Returns a <code>ManagedOutputStream</code> for a managed file.  
	 * Closing the ouput stream will update the storage manager with the 
	 * new content of the managed file.
	 * 
	 * @param managedFile the name of the managed file to write.
	 * @return a managed output stream for the managed file.
	 * @throws IOException if an error occurs opening the managed file.
	 */
    public ManagedOutputStream getOutputStream(String managedFile) throws IOException {
        if (useReliableFiles) {
            ReliableFileOutputStream out = new ReliableFileOutputStream(new File(getBase(), managedFile));
            return new ManagedOutputStream(out, this, managedFile, null);
        }
        File tmpFile = createTempFile(managedFile);
        return new ManagedOutputStream(new FileOutputStream(tmpFile), this, managedFile, tmpFile);
    }

    /**
	 * Returns an array of <code>ManagedOutputStream</code> for a set of managed files.
	 * When all managed output streams in the set have been closed, the storage manager
	 * will be updated with the new content of the managed files. 
	 * Aborting any one of the streams will cause the entire content of the set to abort 
	 * and be discarded.
	 * 
	 * @param managedFiles list of names of the managed file to write.
	 * @return an array of managed output streams respectively of managed files.
	 * @throws IOException if an error occurs opening the managed files.
	 */
    public ManagedOutputStream[] getOutputStreamSet(String[] managedFiles) throws IOException {
        int count = managedFiles.length;
        ManagedOutputStream[] streams = new ManagedOutputStream[count];
        int idx = 0;
        try {
            for (; idx < count; idx++) {
                ManagedOutputStream newStream = getOutputStream(managedFiles[idx]);
                newStream.setStreamSet(streams);
                streams[idx] = newStream;
            }
        } catch (IOException e) {
            for (int jdx = 0; jdx < idx; jdx++) streams[jdx].abort();
            throw e;
        }
        return streams;
    }

    void abortOutputStream(ManagedOutputStream out) {
        ManagedOutputStream[] set = out.getStreamSet();
        if (set == null) {
            set = new ManagedOutputStream[] { out };
        }
        synchronized (set) {
            for (int idx = 0; idx < set.length; idx++) {
                out = set[idx];
                if (out.getOutputFile() == null) {
                    ReliableFileOutputStream rfos = (ReliableFileOutputStream) out.getOutputStream();
                    rfos.abort();
                } else {
                    if (out.getState() == ManagedOutputStream.ST_OPEN) {
                        try {
                            out.getOutputStream().close();
                        } catch (IOException e) {
                        }
                    }
                    out.getOutputFile().delete();
                }
                out.setState(ManagedOutputStream.ST_CLOSED);
            }
        }
    }

    void closeOutputStream(ManagedOutputStream smos) throws IOException {
        if (smos.getState() != ManagedOutputStream.ST_OPEN) return;
        ManagedOutputStream[] streamSet = smos.getStreamSet();
        if (smos.getOutputFile() == null) {
            ReliableFileOutputStream rfos = (ReliableFileOutputStream) smos.getOutputStream();
            File file = rfos.closeIntermediateFile();
            smos.setState(ManagedOutputStream.ST_CLOSED);
            String target = smos.getTarget();
            if (streamSet == null) {
                add(target, StorageManager.FILETYPE_RELIABLEFILE);
                update(new String[] { smos.getTarget() }, new String[] { file.getName() });
                ReliableFile.fileUpdated(new File(getBase(), smos.getTarget()));
            }
        } else {
            OutputStream out = smos.getOutputStream();
            out.flush();
            try {
                ((FileOutputStream) out).getFD().sync();
            } catch (SyncFailedException e) {
            }
            out.close();
            smos.setState(ManagedOutputStream.ST_CLOSED);
            String target = smos.getTarget();
            if (streamSet == null) {
                add(target, StorageManager.FILETYPE_STANDARD);
                update(new String[] { target }, new String[] { smos.getOutputFile().getName() });
            }
        }
        if (streamSet != null) {
            synchronized (streamSet) {
                for (int idx = 0; idx < streamSet.length; idx++) {
                    if (streamSet[idx].getState() == ManagedOutputStream.ST_OPEN) return;
                }
                String[] targets = new String[streamSet.length];
                String[] sources = new String[streamSet.length];
                for (int idx = 0; idx < streamSet.length; idx++) {
                    smos = streamSet[idx];
                    targets[idx] = smos.getTarget();
                    File outputFile = smos.getOutputFile();
                    if (outputFile == null) {
                        add(smos.getTarget(), StorageManager.FILETYPE_RELIABLEFILE);
                        ReliableFileOutputStream rfos = (ReliableFileOutputStream) smos.getOutputStream();
                        File file = rfos.closeIntermediateFile();
                        sources[idx] = file.getName();
                        ReliableFile.fileUpdated(new File(getBase(), smos.getTarget()));
                    } else {
                        add(smos.getTarget(), StorageManager.FILETYPE_STANDARD);
                        sources[idx] = outputFile.getName();
                    }
                }
                update(targets, sources);
            }
        }
    }
}
