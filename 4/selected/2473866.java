package edu.mit.osidutil.contrivance;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *  <p>
 *  Performs lookups on a group-like formatted file. 
 *  </p><p>
 *  The group file format is expected to be in the form of:
 *  <code>
 *  group:*:gid:user
 *  </code>
 *  </p><p>  
 *  CVS $Id: GroupFile.java,v 1.2 2005/09/19 14:59:30 tom Exp $
 *  </p>
 *  @author  Tom Coppeto
 *  @version $Revision: 1.2 $
 *  @see java.lang.Object
 */
public class GroupFile extends Object {

    private String filename;

    private RandomAccessFile file;

    private FileChannel channel;

    private FileLock lock = null;

    /**
     *  Constructor. Opens the specified file for reading.
     *
     *  @param file the pathname to the group file to be parsed
     *  @throws GroupFileException if there was an error opening the
     *          group file
     */
    public GroupFile(final String file) throws GroupFileException {
        if (file == null) {
            throw new GroupFileException("no file specified");
        }
        this.filename = file;
        open();
        return;
    }

    /**
     *  Rewind the file and retrieve the first entry in the groupfile.
     *
     *  @return the first entry in the group file
     *  @throws GroupFileException if there was an error parsing the file
     *          group file
     *  @see GroupEntry
     */
    public synchronized GroupEntry firstEntry() throws GroupFileException {
        if (this.file == null) {
            throw new GroupFileException(file + " is closed");
        }
        try {
            this.file.seek(0);
        } catch (IOException ie) {
            throw new GroupFileException("unable to parse file " + this.filename, ie);
        }
        return (nextEntry());
    }

    /**
     *  Retrieve the next entry from the group file relative to the current
     *  file pointer.
     *
     *  @return the next group entry
     *  @throws GroupFileException if there was an error parsing the
     *          group file
     *  @see GroupEntry
     */
    public synchronized GroupEntry nextEntry() throws GroupFileException {
        if (this.file == null) {
            throw new GroupFileException(file + " is closed");
        }
        try {
            String line = this.file.readLine();
            if (line == null) {
                return (null);
            }
            return (new GroupEntry(line));
        } catch (IOException ie) {
            throw new GroupFileException("unable to parse file " + this.filename, ie);
        } catch (GroupFileException pe) {
            return (nextEntry());
        }
    }

    /**
     *  Looks up the line in the group file corresponding to the
     *  specified user.
     *
     *  @param group the group to be looked up
     *  @return the GroupEntry corresponding to the group, or null
     *          if the group is not found.
     *  @throws GroupFileException if there was an error parsing the
     *          group file
     *  @throws GroupFileGroupNotFoundException if the specified group
     *          is not found in the group file
     *  @see GroupEntry
     */
    public synchronized GroupEntry lookupEntry(final String group) throws GroupFileException {
        GroupEntry entry = firstEntry();
        do {
            if (entry.getGroup().equals(group)) {
                return (entry);
            }
            entry = nextEntry();
        } while (entry != null);
        throw new GroupFileGroupNotFoundException("group " + group + " not found");
    }

    /**
     *  Looks up the line in the group file corresponding to the
     *  specified gid.
     *
     *  @param gid the gid to be looked up
     *  @return the GroupEntry corresponding to the gid, or null
     *          if the user is not found.
     *  @throws GroupFileException if there was an error parsing the
     *          group file
     *  @throws GroupFileGroupNotFoundException if the specified gid
     *          is not found in the group file
     *  @see GroupEntry
     */
    public synchronized GroupEntry lookupEntry(final int gid) throws GroupFileException {
        GroupEntry entry = firstEntry();
        do {
            if (entry.getGID() == gid) {
                return (entry);
            }
            entry = nextEntry();
        } while (entry != null);
        throw new GroupFileGroupNotFoundException("gid " + gid + " not found");
    }

    /** 
     *  Adds a new entry to teh group file. The specified entry is appended
     *  to an existing file.
     *
     *  @param entry the Group entry to add
     *  @throws GroupFileException if there was an error adding the group
     *          entry
     *  @see GroupEntry
     */
    public synchronized void addEntry(final GroupEntry entry) throws GroupFileException {
        if (entry == null) {
            throw new GroupFileException("entry is null");
        }
        if (this.file == null) {
            throw new GroupFileException(file + " is closed");
        }
        long pos = -1;
        try {
            exLock();
            pos = this.file.length();
            this.file.seek(pos);
            this.file.writeBytes(entry.formatLine());
        } catch (Exception e) {
            if (pos >= 0) {
                try {
                    this.file.setLength(pos);
                } catch (Exception r2d2) {
                }
            }
            throw new GroupFileException("unable to write entry for " + entry.getGroup(), e);
        } finally {
            try {
                unlock();
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
     *  Updates the group entry.
     *  This is performed by copying the group database to a 
     *  temporary file.
     *
     *  @param update the entry to update
     *  @throws GroupFileException if there was an error updaring the group
     *  @see GroupEntry
     */
    public synchronized void updateEntry(final GroupEntry update) throws GroupFileException {
        if (update == null) {
            throw new GroupFileException("entry is null");
        }
        if (this.file == null) {
            throw new GroupFileException(file + " is closed");
        }
        exLock();
        File temp;
        RandomAccessFile raf;
        try {
            temp = File.createTempFile("group", ".tmp");
            raf = new RandomAccessFile(temp, "rw");
        } catch (IOException ie) {
            throw new GroupFileException("unable to open temp file", ie);
        }
        try {
            GroupEntry entry = firstEntry();
            do {
                if (!entry.getGroup().equals(update.getGroup())) {
                    raf.writeBytes(entry.formatLine());
                } else {
                    raf.writeBytes(update.formatLine());
                }
                entry = nextEntry();
            } while (entry != null);
        } catch (Exception e) {
            unlock();
            temp.delete();
            throw new GroupFileException("unable to remove entry " + update.getGroup(), e);
        }
        try {
            raf.close();
            temp.renameTo(new File(this.filename));
            open();
        } catch (IOException ie) {
            throw new GroupFileException("unable to close temp group file", ie);
        }
        return;
    }

    /**
     *  Removes the group entry(ies) whose group name matches the specified
     *  string. This is performed by copying the group database to a 
     *  temporary file.
     *
     *  @param name the name of the group(s) to remove
     *  @throws GroupFileException if there was an error removing the group
     *  @see GroupEntry
     */
    public synchronized void deleteEntry(final String name) throws GroupFileException {
        if (this.file == null) {
            throw new GroupFileException(file + " is closed");
        }
        exLock();
        File temp;
        RandomAccessFile raf;
        try {
            temp = File.createTempFile("group", ".tmp");
            raf = new RandomAccessFile(temp, "rw");
        } catch (IOException ie) {
            throw new GroupFileException("unable to open temp file", ie);
        }
        try {
            GroupEntry entry = firstEntry();
            do {
                if (!entry.getGroup().equals(name)) {
                    raf.writeBytes(entry.formatLine());
                }
                entry = nextEntry();
            } while (entry != null);
        } catch (Exception e) {
            unlock();
            temp.delete();
            throw new GroupFileException("unable to remove entry " + name, e);
        }
        try {
            raf.close();
            temp.renameTo(new File(this.filename));
            open();
        } catch (IOException ie) {
            throw new GroupFileException("unable to close temp group file", ie);
        }
        return;
    }

    /**
     *  Closes the group file. This class would need to be reinstantiated
     *  to re-open the group file.
     */
    public synchronized void close() throws GroupFileException {
        try {
            this.channel.close();
            this.file.close();
        } catch (IOException ie) {
            throw new GroupFileException("unable to close group file: " + this.filename, ie);
        }
        this.file = null;
        return;
    }

    private void open() throws GroupFileException {
        try {
            this.file = new RandomAccessFile(this.filename, "rw");
        } catch (FileNotFoundException fe) {
            throw new GroupFileException(file + " not found", fe);
        }
        this.channel = this.file.getChannel();
        this.lock = null;
        return;
    }

    private void exLock() throws GroupFileException {
        if (this.lock != null) {
            throw new GroupFileException(filename + " already locked");
        }
        try {
            this.lock = channel.lock();
        } catch (IOException ie) {
            throw new GroupFileException("unable to lock " + this.filename, ie);
        }
        return;
    }

    private void unlock() throws GroupFileException {
        if (this.lock == null) {
            return;
        }
        try {
            this.lock.release();
            this.lock = null;
        } catch (IOException ie) {
            throw new GroupFileException("unable to unlock " + this.filename, ie);
        }
        return;
    }
}
