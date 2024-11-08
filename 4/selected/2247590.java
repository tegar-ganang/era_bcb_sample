package edu.mit.osidutil.contrivance;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *  <p>
 *  Performs lookups on a passwd-like formatted file. 
 *  </p><p>
 *  The passwd file format is expected to be in the form of:
 *  <code>
 *  user:passwd:uid:gid:Full Name, Office,Work Phone,Home Phone:directory:shell
 *  </code>
 *  or
 *  <code>
 *  user:passwd:uid:gid:Full Name,Nickname,Office,Work Phone,Home Phone:directory:shell
 *  </code>
 *  </p><p>  
 *  CVS $Id: PasswdFile.java,v 1.3 2005/09/19 15:07:34 tom Exp $
 *  </p>
 *  @author  Tom Coppeto
 *  @version $Revision: 1.3 $
 *  @see java.lang.Object
 */
public class PasswdFile extends Object {

    private String filename;

    private RandomAccessFile file;

    private FileChannel channel;

    private FileLock lock = null;

    /**
     *  Constructor. Opens the specified file for reading.
     *
     *  @param file the pathname to the passwd file to be parsed
     *  @throws PasswdFileException if there was an error opening the
     *          passwd file
     */
    public PasswdFile(final String file) throws PasswdFileException {
        if (file == null) {
            throw new PasswdFileException("no file specified");
        }
        this.filename = file;
        open();
        return;
    }

    /**
     *  Rewinds the file and returns the first entry
     *
     *  @return the first entry
     *  @throws PasswdFileException if there was an error parsing the
     *          passwd file
     *  @see PasswdEntry
     */
    public synchronized PasswdEntry firstEntry() throws PasswdFileException {
        if (this.file == null) {
            throw new PasswdFileException(file + " is closed");
        }
        try {
            this.file.seek(0);
        } catch (IOException ie) {
            throw new PasswdFileException("unable to parse file " + this.filename, ie);
        }
        return (nextEntry());
    }

    /** 
     *  Returns the next passwd file entry relative to the current file
     *  pointer.
     *
     *  @return the next passwd entry
     *  @throws PasswdFileException if there was an error parsing the
     *          passwd file
     *  @see PasswdEntry
     */
    public synchronized PasswdEntry nextEntry() throws PasswdFileException {
        if (this.file == null) {
            throw new PasswdFileException(file + " is closed");
        }
        try {
            while (true) {
                String line = this.file.readLine();
                if (line == null) {
                    return (null);
                }
                if (line.trim().startsWith("#") == true) {
                    continue;
                }
                return (new PasswdEntry(line));
            }
        } catch (IOException ie) {
            throw new PasswdFileException("unable to parse file " + this.filename, ie);
        } catch (PasswdFileException pe) {
            return (nextEntry());
        }
    }

    /**
     *  Looks up the line in the passwd file corresponding to the
     *  specified user.
     *
     *  @param user the user to be looked up
     *  @return the PasswdEntry corresponding to the user, or null
     *          if the user is not found.
     *  @throws PasswdFileException if there was an error parsing the
     *          passwd file
     *  @throws PasswdFileUserNotFoundException if the specified user
     *          is not found in the passwd file
     *  @see PasswdEntry
     */
    public synchronized PasswdEntry lookupEntry(final String user) throws PasswdFileException {
        PasswdEntry entry = firstEntry();
        do {
            if (entry.getUser().equals(user)) {
                return (entry);
            }
            entry = nextEntry();
        } while (entry != null);
        throw new PasswdFileUserNotFoundException("user " + user + " not found");
    }

    /**
     *  Looks up the line in the passwd file corresponding to the
     *  specified uid.
     *
     *  @param uid the uid to be looked up
     *  @return the PasswdEntry corresponding to the user, or null
     *          if the user is not found.
     *  @throws PasswdFileException if there was an error parsing the
     *          passwd file
     *  @throws PasswdFileUserNotFoundException if the specified user
     *          is not found in the passwd file
     *  @see PasswdEntry
     */
    public synchronized PasswdEntry lookupEntry(final int uid) throws PasswdFileException {
        PasswdEntry entry = firstEntry();
        do {
            if (entry.getUID() == uid) {
                return (entry);
            }
            entry = nextEntry();
        } while (entry != null);
        throw new PasswdFileUserNotFoundException("uid " + uid + " not found");
    }

    /**
     *  Appends a new passwd entry in the passwd file
     *
     *  @param entry the new passwd entry to append
     *  @throws PasswdFileException if there was an error adding thios entry
     *  @see PasswdEntry
     */
    public synchronized void addEntry(final PasswdEntry entry) throws PasswdFileException {
        if (entry == null) {
            throw new PasswdFileException("entry is null");
        }
        if (this.file == null) {
            throw new PasswdFileException(file + " is closed");
        }
        long pos = -1;
        try {
            exLock();
            pos = this.file.length();
            PasswdEntry pe = firstEntry();
            if (pe != null) {
                this.file.writeBytes(entry.formatLine(pe.getNickname() == null ? 0 : 1));
            } else {
                this.file.writeBytes(entry.formatLine(0));
            }
        } catch (Exception e) {
            try {
                if (pos >= 0) {
                    this.file.setLength(pos);
                }
            } catch (Exception r2d2) {
            }
            throw new PasswdFileException("unable to write entry for " + entry.getUser(), e);
        } finally {
            try {
                unlock();
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
     *  Updates the passwd entries matching the entry's username. This
     *  is performed by copying the passwd file to a temporary file.
     *
     *  @param update the PassswdEntry to update
     *  @throws PasswdFileException if there was an error updating this entry
     */
    public synchronized void updateEntry(final PasswdEntry update) throws PasswdFileException {
        if (update == null) {
            throw new PasswdFileException("entry is null");
        }
        if (this.file == null) {
            throw new PasswdFileException(file + " is closed");
        }
        exLock();
        File temp;
        RandomAccessFile raf;
        try {
            temp = File.createTempFile("passwd", ".tmp");
            raf = new RandomAccessFile(temp, "rw");
        } catch (IOException ie) {
            throw new PasswdFileException("unable to open temp file", ie);
        }
        try {
            PasswdEntry entry = firstEntry();
            int version = 0;
            if (entry != null) {
                if (entry.getNickname() != null) {
                    version = 1;
                }
            }
            do {
                if (!entry.getUser().equals(update.getUser())) {
                    raf.writeBytes(entry.formatLine(version));
                } else {
                    raf.writeBytes(update.formatLine(version));
                }
                entry = nextEntry();
            } while (entry != null);
        } catch (Exception e) {
            unlock();
            temp.delete();
            throw new PasswdFileException("unable to remove entry " + update.getUser(), e);
        }
        try {
            raf.close();
            temp.renameTo(new File(this.filename));
            open();
        } catch (IOException ie) {
            throw new PasswdFileException("unable to close temp passwd file", ie);
        }
        return;
    }

    /**
     *  Deletes the passwd entries matching the specified username. This
     *  is performed by copying the passwd file to a temporary file.
     *
     *  @param name the username to delete
     *  @throws PasswdFileException if there was an error deleting this user
     */
    public synchronized void deleteEntry(final String name) throws PasswdFileException {
        if (this.file == null) {
            throw new PasswdFileException(file + " is closed");
        }
        exLock();
        File temp;
        RandomAccessFile raf;
        try {
            temp = File.createTempFile("passwd", ".tmp");
            raf = new RandomAccessFile(temp, "rw");
        } catch (IOException ie) {
            throw new PasswdFileException("unable to open temp file", ie);
        }
        try {
            PasswdEntry entry = firstEntry();
            int version = 0;
            if (entry != null) {
                if (entry.getNickname() != null) {
                    version = 1;
                }
            }
            do {
                if (!entry.getUser().equals(name)) {
                    raf.writeBytes(entry.formatLine(version));
                }
                entry = nextEntry();
            } while (entry != null);
        } catch (Exception e) {
            unlock();
            temp.delete();
            throw new PasswdFileException("unable to remove entry " + name, e);
        }
        try {
            raf.close();
            temp.renameTo(new File(this.filename));
            open();
        } catch (IOException ie) {
            throw new PasswdFileException("unable to close temp passwd file", ie);
        }
        return;
    }

    /**
     *  Closes the passwd file. This class would need to be reinstantiated
     *  to re-open the passwd file.
     */
    public synchronized void close() throws PasswdFileException {
        try {
            this.channel.close();
            this.file.close();
        } catch (IOException ie) {
            throw new PasswdFileException("unable to close passwd file: " + this.filename, ie);
        }
        this.file = null;
        return;
    }

    private void open() throws PasswdFileException {
        try {
            this.file = new RandomAccessFile(this.filename, "rw");
        } catch (FileNotFoundException fe) {
            throw new PasswdFileException(file + " not found", fe);
        }
        this.channel = this.file.getChannel();
        return;
    }

    private void exLock() throws PasswdFileException {
        if (this.lock != null) {
            throw new PasswdFileException(filename + " already locked");
        }
        try {
            this.lock = channel.lock();
        } catch (IOException ie) {
            throw new PasswdFileException("unable to lock " + this.filename, ie);
        }
        return;
    }

    private void unlock() throws PasswdFileException {
        if (this.lock == null) {
            return;
        }
        try {
            this.lock.release();
            this.lock = null;
        } catch (IOException ie) {
            throw new PasswdFileException("unable to unlock " + this.filename, ie);
        }
        return;
    }
}
