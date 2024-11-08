package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.internal.adaptor.BasicLocation;
import org.eclipse.core.runtime.internal.adaptor.Locker;
import org.eclipse.osgi.framework.internal.reliablefile.*;

/**
 * File managers provide a facility for tracking the state of files being used and updated by several
 * systems at the same time. The typical usecase is in shared configuration data areas.
 * <p>
 * The general principle is to maintain a table which maps user-level file name
 * onto actual disk file. The filename is actually never used, and the file is always stored under the
 * given filename suffixed by an integer. If a file needs to be modified, it is written into a new file whose name suffix 
 * is incremented.
 * Once the instance has been created, open() must be called before performing any other operation.
 * On open the fileManager starts by reading the current table and
 * thereby obtaining a snapshot of the current directory state. If another
 * entity updates the directory, the file manager is able to detect the change.
 * Given that the file is unique, if another entity used the file manager mechanism, the file manager can
 * still access the state of the file as it was when the file manager first started.
 * <p>
 * The facilities provided here are cooperative. That is, all participants must
 * agree to the conventions and to calling the given API. There is no capacity
 * to enforce these conventions or prohibit corruption.
 * </p>
 * <p>
 * Clients may not extend this class.
 * </p>
 * @since 3.1
 */
public class FileManager {

    static final int FILETYPE_STANDARD = 0;

    static final int FILETYPE_RELIABLEFILE = 1;

    private static boolean tempCleanup = Boolean.valueOf(System.getProperty("osgi.embedded.cleanTempFiles")).booleanValue();

    private static boolean openCleanup = Boolean.valueOf(System.getProperty("osgi.embedded.cleanupOnOpen")).booleanValue();

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

    private static final String MANAGER_FOLDER = ".manager";

    private static final String TABLE_FILE = ".fileTable";

    private static final String LOCK_FILE = ".fileTableLock";

    private static final int MAX_LOCK_WAIT = 5000;

    /**
	 * Returns a new file manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the given filemanager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 */
    public FileManager(File base, String lockMode) {
        this(base, lockMode, false);
    }

    /**
	 * Returns a new file manager for the area identified by the given base
	 * directory.
	 * 
	 * @param base the directory holding the files to be managed
	 * @param lockMode the lockMode to use for the given filemanager. It can have one the 3 values: none, java.io, java.nio 
	 * and also supports null in which case the lock strategy will be the global one.  
	 */
    public FileManager(File base, String lockMode, boolean readOnly) {
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
	 * Add the given file name to the list of files managed by this location.
	 * 
	 * @param file path of the file to manage
	 * @throws IOException if there are any problems adding the given file to the manager
	 */
    public void add(String file) throws IOException {
        add(file, FILETYPE_STANDARD);
    }

    /**
	 * Add the given file name to the list of files managed by this location.
	 * 
	 * @param file path of the file to manage.
	 * @param fileType the file type. 
	 * @throws IOException if there are any problems adding the given file to the manager
	 * @see #FILETYPE_STANDARD
	 * @see #FILETYPE_STANDARD
	 */
    protected void add(String file, int fileType) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            Entry entry = (Entry) table.get(file);
            if (entry == null) {
                entry = new Entry(0, 1, fileType);
                table.put(file, entry);
                int oldestGeneration = findOldestGeneration(file);
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

    /**
	 * Find the oldest generation of a file still available on disk 
	 * @param file the file from which to obtain the oldest generation.
	 * @return the oldest generation of the file or 0 if the file does
	 * not exist. 
	 */
    private int findOldestGeneration(String file) {
        String[] files = base.list();
        int oldestGeneration = 0;
        if (files != null) {
            String name = file + '.';
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
	 * Update the given target files with the content in the given source files.
	 * The targets are file paths which are currently managed. The sources are
	 * absolute (or relative to the current working directory) file paths
	 * containing the new content for the corresponding target.
	 * 
	 * @param targets the target files to update
	 * @param sources the new content for the target files
	 * @throws IOException if there are any problems updating the given files
	 */
    public void update(String[] targets, String[] sources) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            int[] originalReadIDs = new int[targets.length];
            boolean error = false;
            for (int i = 0; i < targets.length; i++) {
                originalReadIDs[i] = getId(targets[i]);
                if (!update(targets[i], sources[i])) error = true;
            }
            if (error) {
                for (int i = 0; i < targets.length; i++) {
                    Entry entry = (Entry) table.get(targets[i]);
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
	 * Returns a list of all the file paths currently being managed.
	 * 
	 * @return the file paths being managed
	 */
    public String[] getFiles() {
        if (!open) return null;
        Set set = table.keySet();
        String[] keys = (String[]) set.toArray(new String[set.size()]);
        String[] result = new String[keys.length];
        for (int i = 0; i < keys.length; i++) result[i] = new String(keys[i]);
        return result;
    }

    /**
	 * Returns the directory containing the files being managed by this file
	 * manager.
	 * 
	 * @return the directory containing the managed files
	 */
    public File getBase() {
        return base;
    }

    /**
	 * Returns the current numeric id (appendage) of the given file.
	 * <code>file + "." + getId(file)</code>. -1 is returned if the given
	 * target file is not managed.
	 * 
	 * @param target
	 *                   the managed file to access
	 * @return the id of the file
	 */
    public int getId(String target) {
        if (!open) return -1;
        Entry entry = (Entry) table.get(target);
        if (entry == null) return -1;
        return entry.getReadId();
    }

    /**
	 * Returns the file type for the given file. If the file is not managed or
	 * the file manager is not open, -1 will be returned.
	 * 
	 * @param target the managed file to access.
	 * @return the current type this file is being managed as or -1 if an
	 * error occurs.
	 */
    protected int getFileType(String target) {
        if (open) {
            Entry entry = (Entry) table.get(target);
            if (entry != null) return entry.getFileType();
        }
        return -1;
    }

    /**
	 * Returns if readOnly state this file manager is using.
	 * 
	 * @return if this filemanage update state is read-only.
	 */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
	 * Attempts to lock the state of this manager and returns <code>true</code>
	 * if the lock could be acquired.
	 * <p>
	 * Locking a manager is advisory only. That is, it does not prevent other
	 * applications from modifying the files managed by this manager.
	 * </p>
	 * 
	 * @exception IOException
	 *                         if there was an unexpected problem while acquiring the
	 *                         lock.
	 */
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
	 * <code>null</code> can be returned if the given target is not managed and add is set to false.
	 * 
	 * @param target the managed file to lookup
	 * @param add indicate whether the file should be added to the manager if it is not managed.
	 * @throws IOException if the add flag is set to true and the addition of the file failed
	 * @return the absolute file location to use for the given file or
	 *               <code>null</code> if the given target is not managed
	 */
    public File lookup(String target, boolean add) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        Entry entry = (Entry) table.get(target);
        if (entry == null) {
            if (add) {
                add(target);
                entry = (Entry) table.get(target);
            } else {
                return null;
            }
        }
        return new File(getAbsolutePath(target + '.' + entry.getReadId()));
    }

    private boolean move(String source, String target) {
        File original = new File(source);
        File targetFile = new File(target);
        if (!original.exists() || targetFile.exists()) return false;
        return original.renameTo(targetFile);
    }

    /**
	 * Saves the state of the file manager and releases any locks held.
	 */
    private void release() {
        if (locker == null) return;
        locker.release();
    }

    /**
	 * Removes the given file from management by this file manager.
	 * 
	 * @param file the file to remove
	 */
    public void remove(String file) throws IOException {
        if (!open) throw new IOException(EclipseAdaptorMsg.fileManager_notOpen);
        if (readOnly) throw new IOException(EclipseAdaptorMsg.fileManager_illegalInReadOnlyMode);
        if (!lock(true)) throw new IOException(EclipseAdaptorMsg.fileManager_cannotLock);
        try {
            updateTable();
            table.remove(file);
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

    protected boolean update(String target, String source) {
        Entry entry = (Entry) table.get(target);
        int newId = entry.getWriteId();
        boolean success = move(getAbsolutePath(source), getAbsolutePath(target) + '.' + newId);
        if (!success) {
            newId = findOldestGeneration(target) + 1;
            success = move(getAbsolutePath(source), getAbsolutePath(target) + '.' + newId);
        }
        if (!success) return false;
        entry.setReadId(newId);
        entry.setWriteId(newId + 1);
        return true;
    }

    /**
	 * This methods remove all the temporary files that have been created by the fileManager.
	 * This removal is only done if the instance of eclipse calling this method is the last instance using this fileManager.
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
	 * This methods declare the fileManager as closed. From thereon, the instance can no longer be used.
	 * It is important to close the manager as it also cleanup old copies of the managed files.
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
	 * This methods opens the fileManager, which loads the table in memory. This method must be called before any operation on the filemanager.
	 * @param wait indicates if the open operation must wait in case of contention on the lock file.
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
	 * Creates a new unique empty temporary-file in the filemanager base direcotry. The file name
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
}
