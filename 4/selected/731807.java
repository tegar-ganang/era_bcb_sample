package mujmail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;
import mujmail.debug.DebugConsole;
import mujmail.tasks.Progress;
import mujmail.tasks.StoppableBackgroundTask;
import mujmail.tasks.StoppableProgress;
import mujmail.threading.Algorithm;
import mujmail.ui.OKCancelDialog;
import mujmail.util.Callback;
import mujmail.util.Functions;
import mujmail.util.StartupModes;
import mujmail.util.PersistentValueReminder.PersistentIntValueReminder;

/**
 * Provides functions for storing mail headers and low level functions
 * for storing fragments of body parts of mails used in class {@link RMSStorage}.
 * 
 * TODO: synchronize access to MailDB. Note that synchronizing with
 * the storage of owner is not possible now because the storage can
 * be null and storage does not hold all messages. 
 */
public class MailDB {

    /** The name of this source file */
    private static final String SOURCE_FILE = "MailDB";

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    /** True if headers of boxes should be loaded on startup. */
    private static final boolean LOAD_HEADERS_ON_START = false;

    /** The number of headers to delete from headers database if database is full. */
    private static final int NUM_HEADERS_TO_DELETE_IF_DB_FULL = 5;

    /** The size of free space in header database when start to delete headers or notice user that the space is left. */
    private static final int FREE_SPACE_IN_HEADER_DB_WHEN_DELETE_HEADERS = 3000;

    /** The number of headers in database. -1 if we don't know. */
    private int numHeadersInDB = -1;

    private static final byte RUNMODE_LOAD = 1;

    private static final byte RUNMODE_DELETE_ALL_MAILS = 2;

    private static final byte RUNMODE_DELETE_MAIL = 3;

    public static final String safeModeDBFile = "safemodeStore";

    /** The box that will use this database. */
    private final PersistentBox owner;

    /** Database file name where mail are stored */
    private String dbName;

    /** The dbLoadingTask that loads the database. */
    private StoppableBackgroundTask dbLoadingTask = null;

    private boolean busy = false;

    private Object notifier = new Object();

    /**
     * Creates the instance of MailDB.
     * 
     * @param owner the box that will use this database
     * @param dbName the name of the database file
     */
    public MailDB(PersistentBox owner, String dbName) {
        this.owner = owner;
        busy = false;
        this.dbName = dbName;
    }

    /**
     * Loads initial interval of headers of the box that owns this 
     * database. 
     * Does block. 
     * If MailDB.LOAD_HEADERS_ON_START == false, do nothing.
     */
    void loadInitialPageOfHeadersOnStartup() {
        if (!LOAD_HEADERS_ON_START) {
            busy = false;
            return;
        }
        if (DEBUG) {
            System.out.println("MailDB.loadInitialPageOfHeaders - " + dbName);
        }
        dbLoadingTask = new MailDBTask(RUNMODE_LOAD, 0, PersistentBox.getMaxNumHeadersInStorage() - 1);
        busy = true;
        dbLoadingTask.disableDisplayingProgress();
        dbLoadingTask.disableDisplayingUserActionRunnerUI();
        dbLoadingTask.start(owner, true);
    }

    /**
     * Loads given interval of headers from database to storage of
     * associated persistent box.
     * 
     * @param from number of first loaded header. 
     * @param to number of last loaded header.
     * @param block true if the operation should block. That means that
     *  the thread that called this method will wait until the headers
     *  will be loaded.
     * @param showProgress if true, the progress will be shown.
     */
    void loadHeadersInNewTask(int from, int to, boolean block, boolean showProgress) {
        if (DEBUG) {
            System.out.println("MailDB.loadHeadersInNewTask - " + dbName);
        }
        dbLoadingTask = new MailDBTask(RUNMODE_LOAD, from, to);
        if (!showProgress) {
            dbLoadingTask.disableDisplayingProgress();
        }
        busy = true;
        dbLoadingTask.start(owner, block);
    }

    /**
     * Gets number of headers stored in this database.
     * @return the number of headers stored in this database.
     */
    synchronized int getNumHeadersInDB() {
        if (numHeadersInDB != -1) return numHeadersInDB;
        int numRecords = -1;
        RecordStore headerRS = null;
        try {
            if (DEBUG) {
                System.out.println("DEBUG MailDB.getNumHeadersInDB() - start - " + dbName);
            }
            headerRS = Functions.openRecordStore(dbName + "_H", true);
            if (DEBUG) {
                System.out.println("DEBUG MailDB.getNumHeadersInDB() - Record box opened");
            }
            RecordEnumeration enumeration = headerRS.enumerateRecords(null, null, false);
            numRecords = enumeration.numRecords();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (headerRS != null) Functions.closeRecordStore(headerRS);
        }
        if (DEBUG) {
            System.out.println("DEBUG MailDB.getNumHeadersInDB() - end - " + dbName);
        }
        numHeadersInDB = numRecords;
        return numRecords;
    }

    public StoppableBackgroundTask getDBLoadingTask() {
        return dbLoadingTask;
    }

    /**
     * Delete all mails marked as to markAsDeleted.
     */
    public void deleteMails() {
        dbLoadingTask = new MailDBTask(RUNMODE_DELETE_ALL_MAILS);
        busy = true;
        dbLoadingTask.disableDisplayingProgress();
        dbLoadingTask.start(owner, false);
    }

    /**
     * Deletes mail from database. Does not update the vector where the mail
     * is stored in TheBox.
     * @param header
     */
    public void deleteMail(MessageHeader header) {
        if (this != header.getMailDB()) {
            throw new RuntimeException("Called on bad database");
        }
        MailDBTask task = new MailDBTask(RUNMODE_DELETE_MAIL);
        task.setMessageHeaderToDelete(header);
        task.disableDisplayingProgress();
        task.start();
    }

    public String getDBName() {
        return dbName;
    }

    public boolean isBusy() {
        return busy;
    }

    /**
     * This method clears all records in the database. 
     * By default removes all message bodies.
     * @param headers If set remove message headers too.
     */
    public void clearDb(boolean headers) throws MyException {
        boolean exception = false;
        numHeadersInDB = -1;
        exception = clearRecordStore(dbName);
        if (headers) {
            exception = clearRecordStore(dbName + "_H");
        }
        if (exception) {
            throw new MyException(MyException.DB_CANNOT_CLEAR);
        }
    }

    private boolean clearRecordStore(String dbName) {
        boolean exception = false;
        {
            try {
                RecordStore.deleteRecordStore(dbName);
            } catch (Exception ex) {
                exception = true;
                ex.printStackTrace();
                if (DEBUG) {
                    System.out.println("DEBUG MailDB.clearDB - removing mail body problem from DB: " + dbName);
                    System.out.println(ex);
                }
            }
        }
        return exception;
    }

    /**
     * This method saves a bodypart of a message as a new record in a different RecordStore which name is determined TheBox.name
     * @param body - a String that is supposed to be save. Whole bodypart is stored as one String. The other information
     * about the bodypart are hold in a <code>Vector</code> and stored separatly.
     * Then we return an index which will be stored in bodyPart.recordID.
     * Synchronization is ensured by the rms system.
     * 
     * @see RMSStorage
     */
    int saveFragmentBodypartContent(String body, boolean safeMode) throws MyException {
        if (DEBUG) {
            System.out.println("MailDB.saveBodypartContent: " + body);
        }
        int index = -1;
        if (body.length() == 0) {
            body = "<no content>";
        }
        if (DEBUG) {
            System.out.println("Saving body part content");
        }
        RecordStore bodyRS = Functions.openRecordStore(safeMode ? safeModeDBFile : dbName, true);
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(byteStream);
            outputStream.writeUTF(body);
            outputStream.flush();
            index = bodyRS.addRecord(byteStream.toByteArray(), 0, byteStream.size());
            outputStream.close();
            byteStream.close();
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_SAVE_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }
        if (DEBUG) {
            System.out.println("Body part content saved");
        }
        return index;
    }

    /**
     * this method is for saving binary data
     * @param body
     * @param safeMode
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    int saveFragmentOfBodypartContent(byte[] body, boolean safeMode) throws MyException {
        int index = -1;
        if (DEBUG) {
            System.out.println("Saving body part content raw");
        }
        RecordStore bodyRS = Functions.openRecordStore(safeMode ? safeModeDBFile : dbName, true);
        try {
            index = bodyRS.addRecord(body, 0, body.length);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_SAVE_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }
        if (DEBUG) {
            System.out.println("Body part content raw saved");
        }
        return index;
    }

    /**
     * By this method we get the real content of a body part in byte[]. Can be used by a class that displays mails
     * @param dbFileName
     * @param recordID
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    static byte[] loadFragmentBodypartContentRaw(String dbFileName, int recordID) throws MyException {
        byte[] body = null;
        if (DEBUG) {
            System.out.println("Loading body part content");
        }
        RecordStore bodyRS = Functions.openRecordStore(dbFileName, true);
        try {
            body = bodyRS.getRecord(recordID);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }
        if (DEBUG) {
            System.out.println("body part content loaded");
        }
        return body;
    }

    /**
     * By this method we get the real content a body part. Can be used by a class that displays mails
     * @param dbFileName
     * @param recordID
     * @return
     * @throws MyException
     * 
     * @see RMSStorage
     */
    static String loadFragmentOfBodypartContent(String dbFileName, int recordID) throws MyException {
        String body = null;
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - Loading body part content from database " + dbFileName);
        }
        RecordStore bodyRS = Functions.openRecordStore(dbFileName, true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - Database opened");
        }
        try {
            byte[] data = new byte[bodyRS.getRecordSize(recordID)];
            bodyRS.getRecord(recordID, data, 0);
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
            body = inputStream.readUTF();
            if (DEBUG) {
                System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - loadBodypartContent body='" + body + "'");
            }
            inputStream.close();
            data = null;
        } catch (NullPointerException npex) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - null pointer exception");
            body = "";
        } catch (Exception ex) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - exception ");
            ex.printStackTrace();
            throw new MyException(MyException.DB_CANNOT_LOAD_BODY);
        } finally {
            Functions.closeRecordStore(bodyRS);
        }
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadBodypartContent(String, int) - body part content loaded");
        }
        return body;
    }

    private static final String NULL_STRING = "\\";

    /**
     * This method serves as translator for string values.
     * If value of string is null, it is replaced with "\\" (one backslash).
     * If value is not null and first character is backslash this backslash
     * is doubled otherwise original string is returned.
     * 
     * @param str string to be translated 
     * @return escaped string
     */
    public static String saveNullable(final String str) {
        if (str == null) {
            return NULL_STRING;
        }
        final int length = str.length();
        if (length == 0) {
            return "";
        } else {
            if (str.charAt(0) == '\\') {
                return "\\" + str;
            } else {
                return str;
            }
        }
    }

    /**
     * Oposite for {@link #saveNullable(String)} method.
     * 
     */
    public static String loadNullable(final String str) {
        if (NULL_STRING.equals(str)) {
            return null;
        }
        final int length = str.length();
        if (length == 0) {
            return "";
        } else {
            final char c1 = str.charAt(0);
            if (c1 == '\\') {
                return str.substring(1);
            } else {
                return str;
            }
        }
    }

    /**
     * Handles the situation when message header cannot be saved.
     *
     * @throws MyException if the header was not saved and cannot be saved now
     * @throws Exception if there was exception while saving the header
     */
    private RecordStore handleProblemWithSavingHeader(final MessageHeader header, RecordStore headerRS) throws Exception {
        if (Settings.deleteMailsWhenHeaderDBIsFull) {
            headerRS.closeRecordStore();
            header.getBox().deleteOldestMails(NUM_HEADERS_TO_DELETE_IF_DB_FULL);
            throw new MyException(MyException.DB_CANNOT_SAVE_HEADER);
        } else {
            DeleteOldMails deleteOldMailsAndSaveHeader = new DeleteOldMails(headerRS, header);
            OKCancelDialog dialog = new OKCancelDialog("Not enough space in database", "There is not enough space to store header of this mail. Do you want to delete " + NUM_HEADERS_TO_DELETE_IF_DB_FULL + " oldest mails?", deleteOldMailsAndSaveHeader);
            dialog.showScreen(StartupModes.IN_NEW_THREAD);
            throw new MyException(MyException.DB_CANNOT_SAVE_HEADER);
        }
    }

    private class DeleteOldMails implements Callback {

        private final MessageHeader messageHeader;

        public DeleteOldMails(RecordStore headerRS, MessageHeader messageHeader) {
            this.messageHeader = messageHeader;
        }

        public void callback(Object called, Object message) {
            messageHeader.getBox().deleteOldestMails(NUM_HEADERS_TO_DELETE_IF_DB_FULL);
        }
    }

    /**
     * Saves the header of the message and header of all bodyparts to the RMS database.
     * Does not save the content of the message.
     * If the status of the message is header.DBStatus == MessageHeader.STORED
     * saves the header to existing record in the database (just updates it)
     * @param header the header of the message which will be saved
     * @return the record ID under which the header is saved
     * @throws mujmail.MyException
     */
    public int saveHeader(final MessageHeader header) throws MyException {
        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - saving header: " + header);
        }
        RecordStore headerRS = Functions.openRecordStore(dbName + "_H", true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - to database: " + this.dbName);
        }
        try {
            saveHeader(headerRS, header);
            if (0 != 0) {
                if (DEBUG) {
                    DebugConsole.println("Not enough space in the database.");
                }
                ;
                headerRS = handleProblemWithSavingHeader(header, headerRS);
            } else {
                saveHeader(headerRS, header);
            }
        } catch (MyException myex) {
            myex.printStackTrace();
            if (DEBUG) {
                DebugConsole.println("MyException");
            }
            ;
            throw myex;
        } catch (Exception ex) {
            if (DEBUG) {
                DebugConsole.println(ex.toString());
            }
            ;
            ex.printStackTrace();
            try {
                headerRS = handleProblemWithSavingHeader(header, headerRS);
            } catch (Exception ex1) {
                if (DEBUG) {
                    DebugConsole.println(ex1.toString());
                }
                ;
                ex1.printStackTrace();
            }
        } finally {
            try {
                if (DEBUG) System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - Record store size = " + headerRS.getRecordSize(header.getRecordID()));
            } catch (RecordStoreNotOpenException ex) {
                ex.printStackTrace();
            } catch (InvalidRecordIDException ex) {
                ex.printStackTrace();
            } catch (RecordStoreException ex) {
                ex.printStackTrace();
            }
            Functions.closeRecordStore(headerRS);
        }
        if (DEBUG) {
            System.out.println("DEBUG MailDB.saveHeader(MessageHeader) - header saved");
        }
        return header.getRecordID();
    }

    public static void deleteStorageContent(String dbFileName, int recordID) throws MyException {
        if (DEBUG) {
            System.out.println("Deleting body part");
        }
        RecordStore bodyRecordStore = Functions.openRecordStore(dbFileName, true);
        try {
            bodyRecordStore.deleteRecord(recordID);
        } catch (Exception ex) {
            throw new MyException(MyException.DB_CANNOT_DEL_BODY);
        } finally {
            Functions.closeRecordStore(bodyRecordStore);
        }
        if (DEBUG) {
            System.out.println("Body part deleted");
        }
    }

    public static int bodyRecordSize(String dbFileName, int recordID) {
        RecordStore store = null;
        int size = -1;
        try {
            store = RecordStore.openRecordStore(dbFileName, true);
            size = store.getRecordSize(recordID);
        } catch (Exception ex) {
        }
        Functions.closeRecordStore(store);
        return size;
    }

    /**
     * Get space in bytes that database take place in persistent storage.
     * @return Size of database.
     */
    public int getOccupiedSpace() {
        RecordStore db = null;
        int size = 0;
        try {
            db = RecordStore.openRecordStore(dbName + "_H", true);
            size += db.getSize();
            db.closeRecordStore();
            db = RecordStore.openRecordStore(dbName, true);
            size += db.getSize();
            db.closeRecordStore();
        } catch (Exception ex) {
        }
        return size;
    }

    private void loadHeaders(int from, int to, StoppableProgress progress) throws MyException {
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadHeaders() - start - " + dbName);
            System.out.println("DEBUG MailDB.loadHeaders() - from: " + from + "; to = " + to);
        }
        RecordStore headerRS = Functions.openRecordStore(dbName + "_H", true);
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadHeaders() - Record box opened");
        }
        try {
            if (DEBUG) {
                System.out.println("DEBUG MailDB.loadHeaders() - number of records: " + headerRS.getNumRecords());
            }
            if (headerRS.getNumRecords() > 0) {
                RecordEnumeration enumeration = headerRS.enumerateRecords(null, null, false);
                byte[] data = new byte[250];
                if (DEBUG) {
                    System.out.println("DEBUG MailDB.loadHeaders() - opening input stream");
                }
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
                if (DEBUG) {
                    System.out.println("DEBUG MailDB.loadHeaders() - input stream opened");
                }
                byte bodyPartsCount;
                int id, sizeOfRecord;
                progress.setTitle(Lang.get(Lang.ALRT_LOADING) + owner.getName());
                progress.updateProgress(to - from + 1, 0);
                final Vector storedMessages = new Vector(to - from + 1);
                int numHeader = 0;
                while (enumeration.hasNextElement()) {
                    if (DEBUG) {
                        System.out.println("DEBUG MailDB.loadHeaders() - loading next header");
                    }
                    try {
                        id = enumeration.nextRecordId();
                        if (numHeader < from) {
                            numHeader++;
                            continue;
                        }
                        if (numHeader > to) break;
                        sizeOfRecord = headerRS.getRecordSize(id);
                        if (sizeOfRecord > data.length) {
                            data = new byte[sizeOfRecord + 100];
                            inputStream = new DataInputStream(new ByteArrayInputStream(data));
                        }
                        headerRS.getRecord(id, data, 0);
                        inputStream.reset();
                        MessageHeader header = new MessageHeader(owner);
                        header.setRecordID(id);
                        header.setOrgLocation(inputStream.readChar());
                        header.setFrom(inputStream.readUTF());
                        header.setRecipients(inputStream.readUTF());
                        header.setSubject(inputStream.readUTF());
                        header.setBoundary(inputStream.readUTF());
                        header.setMessageID(inputStream.readUTF());
                        header.setIMAPFolder(inputStream.readUTF());
                        header.setAccountID(inputStream.readUTF());
                        header.messageFormat = inputStream.readByte();
                        header.readStatus = inputStream.readByte();
                        header.flagged = inputStream.readBoolean();
                        header.DBStatus = inputStream.readByte();
                        header.sendStatus = inputStream.readByte();
                        header.setSize(inputStream.readInt());
                        header.setTime(inputStream.readLong());
                        header.setThreadingMessageID(loadNullable(inputStream.readUTF()));
                        header.setParentID(loadNullable(inputStream.readUTF()));
                        int parents = inputStream.readInt();
                        Vector parentIDs = new Vector();
                        for (int i = 0; i < parents; ++i) {
                            parentIDs.addElement(inputStream.readUTF());
                        }
                        header.setParentIDs(parentIDs);
                        bodyPartsCount = inputStream.readByte();
                        for (byte k = 0; k < bodyPartsCount; k++) {
                            BodyPart bp = BodyPart.createUnintializedBodyPart(header);
                            bp.loadBodyPart(inputStream);
                            header.addBodyPart(bp);
                        }
                        if (header.readStatus == MessageHeader.NOT_READ) {
                        }
                        storedMessages.addElement(header);
                        progress.incActual(1);
                        if (progress.stopped()) {
                            break;
                        }
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                    numHeader++;
                }
                if (Algorithm.DEBUG) System.out.println("DEBUG MailDB.loadHeaders(MailDBTask) -  box name: " + owner.getName());
                owner.setStorage(Algorithm.getAlgorithm().invoke(storedMessages));
                if (inputStream != null) {
                    inputStream.close();
                }
                data = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MyException(MyException.DB_CANNOT_LOAD_HEADERS);
        } catch (Error er) {
            er.printStackTrace();
            throw new MyException(MyException.SYS_OUT_OF_MEMORY);
        } finally {
            Functions.closeRecordStore(headerRS);
        }
        if (DEBUG) {
            System.out.println("DEBUG MailDB.loadHeaders() - end - " + dbName);
        }
    }

    private void _deleteMail(MailDBTask progress, MessageHeader header) throws MyException {
        numHeadersInDB = -1;
        for (byte j = (byte) (header.getBodyPartCount() - 1); j >= 0; --j) {
            BodyPart bp = (BodyPart) header.getBodyPart(j);
            bp.getStorage().deleteContent();
        }
        RecordStore headerRS = null;
        try {
            headerRS = RecordStore.openRecordStore(dbName + "_H", true);
        } catch (Exception ex) {
            throw new MyException(0);
        }
        if (headerRS != null) {
            try {
                headerRS.deleteRecord(header.getRecordID());
            } catch (Exception ex) {
                owner.report("+" + Lang.get(Lang.ALRT_DELETING) + header.getSubject() + Lang.get(Lang.FAILED) + ": " + ex, SOURCE_FILE);
                throw new MyException(0);
            }
        }
        Functions.closeRecordStore(headerRS);
    }

    private void _deleteMails(MailDBTask progress) throws MyException {
        numHeadersInDB = -1;
        owner.report(Lang.get(Lang.ALRT_DELETING) + owner.getName(), SOURCE_FILE);
        final int size = owner.getMessageCount();
        Vector toDel = new Vector();
        for (int i = 0; i < size; ++i) {
            MessageHeader header = owner.getMessageHeaderAt(i, false);
            if (header == null) break;
            if (header.deleted) {
                toDel.addElement(header);
            }
        }
        for (Enumeration e = toDel.elements(); e.hasMoreElements(); ) {
            MessageHeader mh = (MessageHeader) e.nextElement();
            MujMail.mujmail.getTrash().storeToTrash(mh, Trash.TrashModes.CONDITIONALLY_MOVE_TO_TRASH);
            _deleteMail(progress, mh);
            --owner.deleted;
        }
        owner.setStorage(null);
        MujMail.mujmail.getTrash().setStorage(null);
    }

    private void saveHeader(RecordStore headerRS, final MessageHeader header) throws RecordStoreNotOpenException, RecordStoreException, IOException, Exception {
        numHeadersInDB = -1;
        if (DEBUG) {
            DebugConsole.println("MailDB.saveHeader - start");
        }
        ;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteStream);
        boolean update = header.DBStatus == MessageHeader.STORED;
        if (header.getOrgLocation() == 'X') {
            header.setOrgLocation(dbName.charAt(0));
        }
        outputStream.writeChar(header.getOrgLocation());
        outputStream.writeUTF(header.getFrom());
        outputStream.writeUTF(header.getRecipients());
        outputStream.writeUTF(header.getSubject());
        if (header.getBoundary() == null) {
            header.setBoundary(header.getMessageID());
        }
        outputStream.writeUTF(header.getBoundary());
        outputStream.writeUTF(header.getMessageID());
        outputStream.writeUTF(header.getIMAPFolder());
        outputStream.writeUTF(header.getAccountID());
        outputStream.writeByte(header.messageFormat);
        outputStream.writeByte(header.readStatus);
        outputStream.writeBoolean(header.flagged);
        header.DBStatus = MessageHeader.STORED;
        outputStream.writeByte(header.DBStatus);
        outputStream.writeByte(header.sendStatus);
        outputStream.writeInt(header.getSize());
        outputStream.writeLong(header.getTime());
        if (DEBUG) {
            DebugConsole.println("MailDB.saveHeader - header fields saved");
        }
        ;
        outputStream.writeUTF(saveNullable(header.getThreadingMessageID()));
        outputStream.writeUTF(saveNullable(header.getParentID()));
        Vector parentIDs = header.getParentIDs();
        int parentsCount = parentIDs.size();
        outputStream.writeInt(parentsCount);
        for (int i = 0; i < parentsCount; ++i) {
            outputStream.writeUTF(parentIDs.elementAt(i).toString());
        }
        if (DEBUG) {
            DebugConsole.println("MailDB.saveHeader - fields for threading saved");
        }
        ;
        byte size = header.getBodyPartCount();
        if (DEBUG) System.out.println("DEBUG - MailDB.saveHeader() - number of bodyparts " + size);
        outputStream.writeByte(size);
        for (byte j = 0; j < size; j++) {
            header.getBodyPart(j).saveBodyPart(outputStream);
        }
        if (DEBUG) {
            DebugConsole.println("MailDB.saveHeader - bodypart headers saved");
        }
        ;
        outputStream.flush();
        int oldID = header.getRecordID();
        header.setRecordID(headerRS.addRecord(byteStream.toByteArray(), 0, byteStream.size()));
        if (DEBUG) {
            DebugConsole.println("MailDB.saveHeader - the record added");
        }
        ;
        if (update) {
            try {
                if (DEBUG) {
                    DebugConsole.println("MailDB.saveHeader - the record deleted (update)");
                }
                ;
                headerRS.deleteRecord(oldID);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        outputStream.close();
        byteStream.close();
        if (DEBUG) {
            DebugConsole.printlnPersistent("MailDB.saveHeader - end");
        }
        ;
    }

    /**
     * Performs stoppable actions.
     */
    private class MailDBTask extends StoppableBackgroundTask {

        /** Specific action to do */
        private byte runMode;

        private int from = -1;

        private int to = -1;

        /** Select mail to delete (in case single mail delete) */
        private MessageHeader messageHeaderToDelete;

        public MailDBTask(byte runMode) {
            super("Database task " + owner.getDBFileName() + " MailDBTask " + runMode);
            this.runMode = runMode;
        }

        public MailDBTask(byte runMode, int from, int to) {
            this(runMode);
            this.from = from;
            this.to = to;
        }

        void setMessageHeaderToDelete(MessageHeader messageHeaderToDelete) {
            this.messageHeaderToDelete = messageHeaderToDelete;
        }

        public void doWork() {
            if (MailDB.DEBUG) System.out.println("Starting MailDBTask");
            busy = true;
            switch(runMode) {
                case RUNMODE_LOAD:
                    try {
                        loadHeaders(from, to, this);
                        if (MailDB.DEBUG) System.out.println("MailDBTask.doWork - loadHeaders finnished");
                        owner.report(Lang.get(Lang.ALRT_LOADING) + owner.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                    } catch (MyException ex) {
                        ex.printStackTrace();
                        owner.report(Lang.get(Lang.ALRT_LOADING) + owner.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    MailDBManager.getMailDBManager().loadedDB(MailDB.this);
                    if (MailDB.DEBUG) System.out.println("MailDBTask.doWork - loadHeaders after loadedDB");
                    break;
                case RUNMODE_DELETE_ALL_MAILS:
                    if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - starting deleting all mails");
                    try {
                        _deleteMails(this);
                        if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - end deleting all mails");
                        owner.report(Lang.get(Lang.ALRT_DELETING) + owner.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                        if (MailDB.DEBUG) System.out.println("DEBUG MailDBTask.doWork() - success reported");
                    } catch (MyException ex) {
                        owner.report(Lang.get(Lang.ALRT_DELETING) + owner.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    break;
                case RUNMODE_DELETE_MAIL:
                    try {
                        System.out.println("MailDB.doWork - starting to delete mail");
                        _deleteMail(this, messageHeaderToDelete);
                        System.out.println("MailDB.doWork - mail succesfully deleted");
                        owner.report(Lang.get(Lang.ALRT_DELETING) + owner.getName() + " " + Lang.get(Lang.SUCCESS), SOURCE_FILE);
                    } catch (MyException ex) {
                        owner.report(Lang.get(Lang.ALRT_DELETING) + owner.getName() + " " + Lang.get(Lang.FAILED) + ": " + ex.getDetails(), SOURCE_FILE);
                    }
                    break;
            }
            busy = false;
            owner.repaint();
            if (MailDB.DEBUG) System.out.println("Ending MailDBTask");
        }
    }
}
