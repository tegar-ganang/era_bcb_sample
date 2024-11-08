package org.jnerve.persistence;

import org.jnerve.*;
import org.jnerve.util.*;
import java.io.*;
import java.util.*;

/** A flat-file implementation of a UserPersistenceStore.
  * This should be compatible with the backend used by opennap 0.22.
  * 
  * 3-24-00 Not the most efficient code for insertUser() and updateUser(), but it's safe
  * and seems to work fairly well.
  */
public class FileUserPersistenceStore implements UserPersistenceStore {

    private static final String PROPERTY_USERS_FILE_PATH = "users.file.path";

    private String fileName;

    private Object fileWriteLock = new Object();

    private Hashtable users = new Hashtable();

    private Vector fileContents = new Vector();

    private WriteMonitorThread writeMonitorThread = null;

    public void init(Properties properties) throws PersistenceException {
        fileName = properties.getProperty(PROPERTY_USERS_FILE_PATH, "users");
        Reader r = null;
        try {
            r = new FileReader(fileName);
        } catch (FileNotFoundException fnfe) {
            Logger.getInstance().log(Logger.SEVERE, "Users file [" + fileName + "] not found. " + fnfe.toString());
            throw new PersistenceException(fnfe.toString());
        }
        BufferedReader br = new BufferedReader(r);
        try {
            String line = null;
            do {
                line = br.readLine();
                if (line != null) {
                    FileLineRecord record = new FileLineRecord(line);
                    fileContents.addElement(record);
                    if (record.isUserRecord()) {
                        users.put(record.getUser().getNickname(), record.getUser());
                    }
                }
            } while (line != null);
        } catch (IOException ioe) {
            Logger.getInstance().log(Logger.SEVERE, "Error reading from file [" + fileName + "]" + ioe.toString());
            throw new PersistenceException(ioe.toString());
        }
        try {
            br.close();
            r.close();
        } catch (IOException ioe) {
            Logger.getInstance().log(Logger.WARNING, "Error closing file [" + fileName + "]" + ioe.toString());
        }
        writeMonitorThread = new WriteMonitorThread(fileName, fileContents);
        writeMonitorThread.setDaemon(true);
        writeMonitorThread.start();
    }

    public User retrieveUser(String nickname) throws PersistenceException {
        return (User) users.get(nickname);
    }

    public void insertUser(User user) throws PersistenceException {
        FileLineRecord record = new FileLineRecord(user);
        fileContents.addElement(record);
        users.put(user.getNickname(), user);
        writeMonitorThread.scheduleWriteAction();
    }

    /** Despite UserPersistenceStore's interface for updating a particular user,
      * this method updates the entire db.
	  */
    public void updateUser(User user) throws PersistenceException {
        writeMonitorThread.scheduleWriteAction();
    }

    public void deleteUser(User user) throws PersistenceException {
        boolean found = false;
        int numUsers = fileContents.size();
        for (int x = 0; x < numUsers && !found; x++) {
            FileLineRecord flr = (FileLineRecord) fileContents.elementAt(x);
            if (flr.isUserRecord()) {
                if (flr.getUser().getNickname().equals(user.getNickname())) {
                    fileContents.removeElementAt(x);
                    found = true;
                }
            }
        }
        if (found) {
            writeMonitorThread.scheduleWriteAction();
        }
    }

    public class FileLineRecord {

        private String rawLine;

        private User user = null;

        public FileLineRecord(String rawLine) {
            this.rawLine = rawLine;
            parse(rawLine);
        }

        public FileLineRecord(User user) {
            this.user = user;
        }

        public boolean isUserRecord() {
            return user != null;
        }

        public User getUser() {
            return user;
        }

        private void parse(String line) {
            StringTokenizer st = new StringTokenizer(line, " ");
            if (line.trim().indexOf("#") != 0 && line.trim().length() > 0) {
                if (st.countTokens() == 6) {
                    User u = new User();
                    u.setNickname(st.nextToken());
                    u.setPassword(st.nextToken());
                    u.setEmail(st.nextToken());
                    u.setLevel(st.nextToken());
                    u.setCreated(new Long(st.nextToken()).longValue());
                    u.setLastseen(new Long(st.nextToken()).longValue());
                    this.user = u;
                } else {
                    Logger.getInstance().log(Logger.WARNING, "Invalid line in users file, ignoring record: " + line);
                }
            }
        }

        public String toFileString() {
            if (isUserRecord()) {
                StringBuffer buf = new StringBuffer(80);
                buf.append(user.getNickname());
                buf.append(" ");
                buf.append(user.getPassword());
                buf.append(" ");
                buf.append(user.getEmail());
                buf.append(" ");
                buf.append(user.getLevel());
                buf.append(" ");
                buf.append(user.getCreated());
                buf.append(" ");
                buf.append(user.getLastseen());
                return buf.toString();
            }
            return rawLine;
        }
    }

    /** Reponsible for writing out the user db file in its own thread */
    public class WriteMonitorThread extends Thread {

        private boolean inWritingProcess = false;

        private boolean hitWhileInWritingProcess = false;

        private boolean keepRunning = true;

        private String filename;

        private Vector recordVector;

        public WriteMonitorThread(String filename, Vector recordVector) {
            this.filename = filename;
            this.recordVector = recordVector;
        }

        public void scheduleWriteAction() {
            synchronized (this) {
                if (inWritingProcess) {
                    hitWhileInWritingProcess = true;
                } else {
                    notify();
                }
            }
        }

        public void safeStop() {
            keepRunning = false;
        }

        public void run() {
            while (keepRunning) {
                try {
                    if (hitWhileInWritingProcess) {
                        hitWhileInWritingProcess = false;
                    } else {
                        synchronized (this) {
                            wait();
                        }
                    }
                } catch (InterruptedException ie) {
                }
                synchronized (this) {
                    inWritingProcess = true;
                }
                doWrite();
                synchronized (this) {
                    inWritingProcess = false;
                }
            }
        }

        private void doWrite() {
            Vector vectorCopy = (Vector) recordVector.clone();
            FileWriter writer = null;
            String tempFileName = fileName + ".TMP";
            try {
                writer = new FileWriter(tempFileName);
                int numRecords = vectorCopy.size();
                for (int x = 0; x < numRecords; x++) {
                    FileLineRecord record = (FileLineRecord) vectorCopy.elementAt(x);
                    writer.write(record.toFileString());
                    writer.write("\n");
                }
                writer.flush();
                writer.close();
                File original = new File(fileName);
                File updated = new File(tempFileName);
                original.delete();
                updated.renameTo(original);
            } catch (IOException ioe) {
                try {
                    writer.close();
                } catch (IOException ioe2) {
                }
                Logger.getInstance().log(Logger.SEVERE, "Error in WriteMonitorThread for FileUserPersistenceStore:" + ioe.toString());
            }
        }
    }
}
