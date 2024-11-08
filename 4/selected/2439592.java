package net.sf.cclearly.entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.swing.ImageIcon;
import net.sf.cclearly.prefs.Prefs;
import net.sf.cclearly.resources.Icons;
import net.sf.cclearly.resources.Messages;
import net.sf.cclearly.util.FileUtil;
import za.dats.net.TransportableObject;
import za.dats.util.time.TimeSpan;

/**
 * The Task entity.
 * 
 * @author Dats
 */
@Entity
public class Task implements TransportableObject, Cloneable, Serializable {

    public static final EnumSet<TaskStatus> STATUS_ARCHIVED = EnumSet.of(TaskStatus.CANT_COMPLETE, TaskStatus.COMPLETED);

    public enum TaskStatus {

        UNSENT("Not Sent", Icons.TASK_UNSENT), UNREAD("Not Read", Icons.TASK_UNREAD), READ("Read", Icons.TASK_READ), PLANNED("Planned", Icons.TASK_PLANNED), IN_PROGRESS("In Progress", Icons.TASK_INPROGRESS), CANT_COMPLETE("Can't Complete", Icons.TASK_CANTCOMPLETE), COMPLETED("Completed", Icons.TASK_COMPLETED), CANCELLED("Cancelled", Icons.TASK_CANTCOMPLETE), SENT("Sent", Icons.TASK_SENT);

        private final String name;

        private ImageIcon icon;

        TaskStatus(String name, ImageIcon icon) {
            this.name = name;
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public ImageIcon getIcon() {
            return icon;
        }
    }

    public enum Priority {

        HIGH(Messages.getString("Task.Priority.High"), 8, Icons.PRIORITY_HIGH), NORMAL(Messages.getString("Task.Priority.Normal"), 5, Icons.PRIORITY_NORMAL), LOW(Messages.getString("Task.Priority.Low"), 3, Icons.PRIORITY_LOW), WISHLIST(Messages.getString("Task.Priority.Wishlist"), 1, Icons.PRIORITY_WISHLIST);

        private int value;

        private String name;

        private ImageIcon icon;

        Priority(String name, int value, ImageIcon icon) {
            this.name = name;
            this.value = value;
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public ImageIcon getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum ReminderInterval {

        NEVER(Messages.getString("Task.Reminder.Never"), -1), DAILY(Messages.getString("Task.Reminder.Daily"), 24 * 60 * 60), HOURLY(Messages.getString("Task.Reminder.Hourly"), 60 * 60), MINUTES_5(Messages.getString("Task.Reminder.5Minutes"), 5 * 60), ONCE(Messages.getString("Task.Reminder.Once"), 0);

        private String name;

        private int reminderTime;

        ReminderInterval(String name, int reminderTime) {
            this.name = name;
            this.reminderTime = reminderTime;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int getReminderTime() {
            return reminderTime;
        }
    }

    @Id
    @GeneratedValue
    protected long id;

    protected String taskNumber;

    @ManyToMany
    protected List<Folder> folders = new LinkedList<Folder>();

    /**
     * This is purely attachment metadata - Attachments in the task attachment
     * folder can still exist without having attached metadata, but not the
     * other way around. This provides a way to describe the attached files.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    protected List<Attachment> attachmentMetadata = new LinkedList<Attachment>();

    @ManyToOne
    protected Folder folder;

    private String requestorFolder;

    private Date dateAdded;

    private Date dateArchived;

    private Date targetDate;

    private String title;

    @Lob
    private String comments = "";

    private TaskStatus status = TaskStatus.UNSENT;

    private Integer priority = Priority.NORMAL.getValue();

    private ReminderInterval reminderInterval;

    private Long reminderDateFrom;

    private Long lastNotification;

    private Integer nextReminderLength;

    private Boolean deleted;

    private Date dateDeleted;

    private Boolean signedOff;

    private Long estimatedTime;

    @ManyToOne
    private User requestorUser;

    @ManyToOne
    private User actionerUser;

    @ManyToOne
    private User capturedBy;

    @Transient
    private File attachFolder;

    @Transient
    private File[] newTempFiles;

    @Basic
    private HashMap<MachineID, Long> idMap = new HashMap<MachineID, Long>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    protected List<TaskNote> notes = new LinkedList<TaskNote>();

    private Boolean unreadNotes;

    private Boolean userNotes;

    /**
     * Used to calculate the time value.
     */
    @Transient
    private long topEstimatedTime = 0L;

    public TimeSpan getEstimatedTime() {
        if (estimatedTime == null) {
            return new TimeSpan(0);
        }
        return new TimeSpan(estimatedTime.intValue());
    }

    public void setEstimatedTime(TimeSpan estimatedTime) {
        if (estimatedTime != null) {
            this.estimatedTime = Long.valueOf(estimatedTime.getTimeStamp());
        } else {
            this.estimatedTime = 0L;
        }
    }

    public boolean isDeleted() {
        if (deleted == null) {
            return false;
        }
        return deleted;
    }

    public boolean setDeleted(boolean deleted, User forUser) {
        if (deleted) {
            if (canDelete(forUser)) {
                this.deleted = deleted;
                dateDeleted = new Date();
                return true;
            } else {
                return false;
            }
        } else {
            this.deleted = deleted;
            return true;
        }
    }

    public long getLastNotification() {
        if (lastNotification == null) {
            return 0;
        }
        return lastNotification;
    }

    public void setLastNotification(long lastNotification) {
        this.lastNotification = lastNotification;
    }

    public long getReminderDateFrom() {
        if (reminderDateFrom == null) {
            return 0;
        }
        return reminderDateFrom;
    }

    public void setReminderDateFrom(long reminderDateFrom) {
        this.reminderDateFrom = reminderDateFrom;
    }

    public ReminderInterval getReminderInterval() {
        if (reminderInterval == null) {
            return ReminderInterval.NEVER;
        }
        return reminderInterval;
    }

    public void setReminderInterval(ReminderInterval reminderInterval) {
        if (reminderInterval == null) {
            this.reminderInterval = ReminderInterval.NEVER;
        } else {
            this.reminderInterval = reminderInterval;
        }
        nextReminderLength = this.reminderInterval.getReminderTime();
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Priority getPriority() {
        for (Priority priority : Priority.values()) {
            if (priority.getValue() == this.priority) {
                return priority;
            }
        }
        return Priority.NORMAL;
    }

    public void setPriority(Priority priority) {
        this.priority = priority.getValue();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Task other = (Task) obj;
        if (id == other.id) {
            return true;
        }
        return false;
    }

    /**
     * @return The folder where this task should keep its attachments
     * 
     * Note however that this folder may not yet exist.
     */
    public File getAttachFolder() {
        if (attachFolder != null) {
            return attachFolder;
        }
        if (id == 0) {
            return null;
        }
        File result = new File(Prefs.getAttachmentFolder(), "task_" + id);
        attachFolder = result;
        return result;
    }

    public int getNextReminderLength() {
        if (nextReminderLength == null) {
            return 0;
        }
        return nextReminderLength;
    }

    public int getMaxVersion() {
        return 6;
    }

    public void writeObject(DataOutputStream out, int version) throws IOException {
        out.writeLong(dateAdded == null ? -1 : dateAdded.getTime());
        out.writeInt(status == null ? -1 : status.ordinal());
        out.writeInt(priority != null ? priority : 0);
        out.writeInt(reminderInterval == null ? -1 : reminderInterval.ordinal());
        out.writeLong(reminderDateFrom == null ? -1 : reminderDateFrom);
        out.writeLong(lastNotification == null ? -1 : lastNotification);
        out.writeInt(nextReminderLength == null ? -1 : nextReminderLength);
        out.writeLong(estimatedTime == null ? -1 : estimatedTime);
        out.writeUTF(title);
        out.writeUTF(comments);
        getRequestor().writeObject(out, 1);
        getActioner().writeObject(out, 1);
        if (idMap == null) {
            out.writeInt(0);
        } else {
            out.writeInt(idMap.size());
            for (MachineID machineID : idMap.keySet()) {
                machineID.writeObject(out, 1);
                out.writeLong(idMap.get(machineID));
            }
        }
        int attachCount = 0;
        File[] attachedFiles = getAttachedFiles();
        if (attachedFiles != null) {
            attachCount += attachedFiles.length;
        }
        if (newTempFiles != null) {
            attachCount += newTempFiles.length;
        }
        if (attachCount == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(attachCount);
            if (attachedFiles != null) {
                for (File file : attachedFiles) {
                    writeFile(out, file);
                }
            }
            if (newTempFiles != null) {
                for (File file : newTempFiles) {
                    writeFile(out, file);
                }
            }
        }
        if (version >= 2) {
            if (targetDate == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeLong(targetDate.getTime());
            }
        }
        if (version >= 3) {
            if (dateArchived == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeLong(dateArchived.getTime());
            }
        }
        if (version >= 4) {
            if (attachmentMetadata == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeInt(attachmentMetadata.size());
                for (Attachment metadata : attachmentMetadata) {
                    metadata.writeObject(out, 2);
                }
            }
            if (requestorFolder == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeUTF(requestorFolder);
            }
            if (notes == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeInt(notes.size());
                for (TaskNote note : notes) {
                    note.writeObject(out, 2);
                }
            }
        }
        if (version >= 5) {
            if (taskNumber == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeUTF(taskNumber);
            }
        }
        if (version >= 6) {
            if (capturedBy == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                capturedBy.writeObject(out, 1);
            }
        }
    }

    public void readObject(DataInputStream in, int version) throws IOException {
        long dtAdded = in.readLong();
        dateAdded = dtAdded == -1 ? null : new Date(dtAdded);
        int statusOrdinal = in.readInt();
        if (statusOrdinal == -1) {
            status = null;
        } else {
            TaskStatus[] statusList = TaskStatus.values();
            status = statusList[statusOrdinal];
        }
        priority = in.readInt();
        int intervalOrdinal = in.readInt();
        if (intervalOrdinal == -1) {
            reminderInterval = null;
        } else {
            ReminderInterval[] intervals = ReminderInterval.values();
            reminderInterval = intervals[intervalOrdinal];
        }
        long dtReminderFrom = in.readLong();
        reminderDateFrom = dtReminderFrom == -1 ? null : dtReminderFrom;
        long dtLastNotification = in.readLong();
        lastNotification = dtLastNotification == -1 ? null : dtLastNotification;
        int nextRmLength = in.readInt();
        nextReminderLength = nextRmLength == -1 ? null : nextRmLength;
        long estTime = in.readLong();
        estimatedTime = estTime == -1 ? null : estTime;
        title = in.readUTF();
        comments = in.readUTF();
        User req = new User();
        req.readObject(in, 1);
        setRequestor(req);
        User act = new User();
        act.readObject(in, 1);
        setActioner(act);
        idMap.clear();
        int idCount = in.readInt();
        for (int i = 0; i < idCount; i++) {
            MachineID readMachineID = new MachineID();
            readMachineID.readObject(in, 1);
            long taskID = in.readLong();
            idMap.put(readMachineID, taskID);
        }
        File tmpFolder = File.createTempFile("attach_", ".cclearly");
        tmpFolder.delete();
        tmpFolder.mkdir();
        tmpFolder.deleteOnExit();
        int attachCount = in.readInt();
        newTempFiles = new File[attachCount];
        for (int i = 0; i < attachCount; i++) {
            String fileName = in.readUTF();
            File tmpFile = new File(tmpFolder, fileName);
            tmpFile.deleteOnExit();
            newTempFiles[i] = tmpFile;
            FileOutputStream out = new FileOutputStream(tmpFile, false);
            long fileSize = in.readLong();
            long fileRead = 0;
            byte[] readFile = new byte[4096];
            while (fileRead != fileSize) {
                int readAmount = (int) Math.min(4000, fileSize - fileRead);
                int realRead = in.read(readFile, 0, readAmount);
                out.write(readFile, 0, realRead);
                fileRead += realRead;
            }
            out.close();
        }
        if (version >= 2) {
            boolean hasTargetDate = in.readBoolean();
            if (hasTargetDate) {
                targetDate = new Date(in.readLong());
            }
        }
        if (version >= 3) {
            boolean hasDateArchived = in.readBoolean();
            if (hasDateArchived) {
                dateArchived = new Date(in.readLong());
            }
        }
        if (version >= 4) {
            boolean hasAttachmentMetadata = in.readBoolean();
            attachmentMetadata = new LinkedList<Attachment>();
            if (hasAttachmentMetadata) {
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    Attachment attachment = new Attachment();
                    attachment.readObject(in, 2);
                    attachmentMetadata.add(attachment);
                }
            }
            boolean hasRequestorFolder = in.readBoolean();
            if (hasRequestorFolder) {
                requestorFolder = in.readUTF();
            }
            boolean hasNotes = in.readBoolean();
            if (hasNotes) {
                int noteCount = in.readInt();
                for (int i = 0; i < noteCount; i++) {
                    TaskNote note = new TaskNote();
                    note.readObject(in, 2);
                    addNote(note);
                }
            }
        }
        if (version >= 5) {
            boolean hasTaskNumber = in.readBoolean();
            if (hasTaskNumber) {
                taskNumber = in.readUTF();
            }
        }
        if (version >= 6) {
            boolean hasCapturedBy = in.readBoolean();
            if (hasCapturedBy) {
                capturedBy = new User();
                capturedBy.readObject(in, 1);
            }
        }
    }

    private void writeFile(DataOutputStream out, File file) throws IOException, FileNotFoundException {
        out.writeUTF(file.getName());
        out.writeLong(file.length());
        FileInputStream in = new FileInputStream(file);
        byte[] data = new byte[4096];
        int readLen = 0;
        while ((readLen = in.read(data)) != -1) {
            out.write(data, 0, readLen);
        }
    }

    public long getAttachmentCount() {
        return getAttachedFiles().length;
    }

    public File[] getAttachedFiles() {
        if (id == 0) {
            return new File[0];
        }
        File taskAttachmentFolder = getAttachFolder();
        if (!taskAttachmentFolder.exists()) {
            return new File[0];
        }
        File[] files = taskAttachmentFolder.listFiles();
        List<File> resultFiles = new LinkedList<File>();
        for (File file : files) {
            if (file.isFile()) {
                resultFiles.add(file);
            }
        }
        File[] result = new File[0];
        result = resultFiles.toArray(result);
        return result;
    }

    public boolean hasAttachments() {
        return getAttachFolder().exists();
    }

    /**
     * @param attachment
     * @return The metadata to this attachment - May be null if none exists.
     */
    public Attachment getAttachmentMetadata(File attachment) {
        if (attachment == null) {
            return null;
        }
        if (attachmentMetadata == null) {
            return null;
        }
        for (Attachment metadata : attachmentMetadata) {
            if (metadata == null) {
                continue;
            }
            if (metadata.getFilename() == null) {
                continue;
            }
            if (metadata.getFilename().equals(attachment.getName())) {
                return metadata;
            }
        }
        return null;
    }

    public List<Attachment> getAttachmentMetadata() {
        return attachmentMetadata;
    }

    public void checkForNewAttachments(Task task) {
        if (task.newTempFiles == null) {
            return;
        }
        synchronized (task.newTempFiles) {
            for (File file : task.newTempFiles) {
                attachFile(file, true);
            }
        }
    }

    public void addAttachMetadata(Attachment metadata) {
        if (attachmentMetadata.contains(metadata)) {
            attachmentMetadata.remove(metadata);
        }
        attachmentMetadata.add(metadata);
    }

    public void removeMetadata(File file) {
        Attachment metadata = getAttachmentMetadata(file);
        if (metadata != null) {
            attachmentMetadata.remove(metadata);
        }
    }

    public void attachFile(File file, boolean move) {
        File attachFolder = getAttachFolder();
        if (!attachFolder.exists()) {
            attachFolder.mkdir();
        }
        if (move) {
            FileUtil.moveFile(file, attachFolder);
        } else {
            FileUtil.copyFile(file, attachFolder, null);
        }
    }

    public void detachFile(File attachment) {
        removeMetadata(attachment);
        if (attachment.exists()) {
            attachment.delete();
        }
        if ((getAttachFolder() != null) && ((getAttachFolder().listFiles() == null) || (getAttachFolder().listFiles().length == 0))) {
            getAttachFolder().delete();
        }
    }

    public List<Folder> getFolders() {
        return new LinkedList<Folder>(folders);
    }

    public int getFolderCount() {
        if (folders == null) {
            return 0;
        }
        return folders.size();
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }

    public boolean isSignedOff() {
        return (signedOff == null ? false : signedOff);
    }

    /**
     * Use this to conveniently check for permission to sign off the task.
     * 
     * @param signedOff
     * @param forUser
     * @return
     */
    public boolean setSignedOff(Boolean signedOff, User forUser) {
        if (canSignOff(forUser)) {
            this.signedOff = signedOff;
            return true;
        }
        return false;
    }

    /**
     * If you need to check for permission before signing off a task, use the
     * setSignedOff(Boolean, User) method instead
     * 
     * @param signedOff
     */
    public void setSignedOff(boolean signedOff) {
        this.signedOff = signedOff;
    }

    public void setIdMap(MachineID machineID) {
        idMap.put(machineID, id);
    }

    public void setIdMap(HashMap<MachineID, Long> idMap) {
        this.idMap = idMap;
    }

    public User getActioner() {
        return actionerUser;
    }

    public void setActioner(User actioner) {
        actionerUser = actioner;
    }

    public User getRequestor() {
        return requestorUser;
    }

    public void setRequestor(User requestor) {
        requestorUser = requestor;
    }

    public boolean setIdForMachine(MachineID machineID) {
        if (idMap.containsKey(machineID)) {
            id = idMap.get(machineID);
            return true;
        } else {
            return false;
        }
    }

    public HashMap<MachineID, Long> getIdMap() {
        return idMap == null ? new HashMap<MachineID, Long>() : idMap;
    }

    public boolean canDelete(User forUser) {
        if (forUser.equals(getRequestor())) {
            return true;
        } else if (isSignedOff()) {
            return true;
        } else if (TaskStatus.CANCELLED.equals(status)) {
            return true;
        } else if (requestorUser == null) {
            return true;
        }
        return false;
    }

    public boolean canSignOff(User forUser) {
        if ((requestorUser == null) || (getRequestor().equals(forUser))) {
            return true;
        }
        return false;
    }

    @Override
    public Task clone() {
        Task result;
        try {
            result = (Task) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        if (folders != null) {
            result.folders = new LinkedList<Folder>(folders);
        }
        if (attachmentMetadata != null) {
            result.attachmentMetadata = new LinkedList<Attachment>(attachmentMetadata);
        }
        if (notes != null) {
            result.notes = new LinkedList<TaskNote>(notes);
        }
        if (idMap != null) {
            result.idMap = new HashMap<MachineID, Long>(idMap);
        }
        return result;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    public Date getDateArchived() {
        return dateArchived;
    }

    public void setDateArchived(Date dateArchived) {
        this.dateArchived = dateArchived;
    }

    public void markArchived(TaskStatus status) {
        dateArchived = new Date();
        setStatus(status);
    }

    public String getFolderString() {
        if (folder == null) {
            return "Inbox";
        }
        return folder.toFullNameString();
    }

    @Override
    public String toString() {
        return title;
    }

    public Task getTask() {
        return this;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder newFolder) {
        folder = newFolder;
    }

    public String getRequestorFolder() {
        return requestorFolder;
    }

    public void setRequestorFolder(String requestorFolder) {
        this.requestorFolder = requestorFolder;
    }

    public List<TaskNote> getNotes() {
        return notes == null ? new LinkedList<TaskNote>() : notes;
    }

    public void addNote(TaskNote note) {
        notes.add(note);
        if (!note.isSystemNote()) {
            userNotes = true;
        }
    }

    public TimeSpan getTopEstimatedTime() {
        return new TimeSpan((int) topEstimatedTime);
    }

    public void setTopEstimatedTime(TimeSpan topEstimatedTime) {
        this.topEstimatedTime = topEstimatedTime.getValue();
    }

    public double getPriorityEstTimeValue() {
        if (topEstimatedTime == 0) {
            return priority;
        }
        if ((estimatedTime == null) || (estimatedTime == 0)) {
            return priority;
        }
        return (double) priority + 8 - (((double) estimatedTime / (double) topEstimatedTime) * 8);
    }

    public boolean hasUserNotes() {
        return userNotes == null ? false : true;
    }

    public boolean hasUnreadNotes() {
        return unreadNotes == null ? false : unreadNotes;
    }

    public void setUnreadNotes(boolean unreadNotes) {
        this.unreadNotes = unreadNotes;
    }

    public boolean needsAttention(User forUser) {
        if (hasUnreadNotes()) {
            return true;
        }
        if ((actionerUser != null) && (actionerUser.equals(forUser))) {
            if (TaskStatus.UNREAD.equals(getStatus()) || TaskStatus.UNSENT.equals(getStatus())) {
                return true;
            }
        } else if ((requestorUser != null) && (requestorUser.equals(forUser))) {
            if (!isSignedOff()) {
                if (TaskStatus.COMPLETED.equals(getStatus()) || TaskStatus.CANT_COMPLETE.equals(getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    /**
     * This is only for internal use, a task number should never normally be
     * changed.
     * 
     * @return
     */
    public void setTaskNumber(String newTaskNumber) {
        taskNumber = newTaskNumber;
    }

    /**
     * Generates a task number with the given prefix.
     * 
     * This should only be called once a newly captured task has been persisted.
     * 
     * This also does not override the task number if it has already been set.
     * 
     * @param prefix
     */
    public void generateTaskNumber(User currentUser) {
        if (taskNumber == null) {
            taskNumber = String.format("%1$s%2$04d", currentUser.getTaskPrefix(), id);
        }
    }

    public Date getDateDeleted() {
        return dateDeleted;
    }

    public User getCapturedBy() {
        return capturedBy;
    }

    public void setCapturedBy(User capturedBy) {
        this.capturedBy = capturedBy;
    }

    public String getHtmlNoteString() {
        User lastUser = null;
        boolean firstNote = true;
        boolean firstNoteForUser = true;
        StringBuilder noteText = new StringBuilder();
        for (TaskNote note : new TreeSet<TaskNote>(getNotes())) {
            if (note.getAddedBy() != lastUser) {
                if (!firstNote) {
                    noteText.append("<br><hr size=1 color=black style='height=1'>");
                    firstNoteForUser = true;
                } else {
                    firstNote = false;
                }
                noteText.append("<b>");
                noteText.append(note.getAddedBy().getName());
                noteText.append("</b><br>");
                lastUser = note.getAddedBy();
            }
            if (note.isSystemNote()) {
                noteText.append("<i><font color=gray>");
            } else {
                if (!firstNoteForUser) {
                    noteText.append("<br>");
                }
            }
            firstNoteForUser = false;
            String noteString = note.getNote().replaceAll("(\\r\\n)|\\r|\\n", "<br>&nbsp;");
            noteString = noteString.replaceAll("(http://)?((www.)[^\\s<]*)", "<a href='http://$2'>$2</a>");
            noteText.append("&nbsp;" + noteString);
            if (note.isSystemNote()) {
                noteText.append(" </font></i>");
            }
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            noteText.append("<font color=gray> ");
            noteText.append(("[ " + df.format(note.getDateAdded()) + " ]").replace(" ", "&nbsp;"));
            noteText.append("</font>");
            noteText.append("<br>");
        }
        return noteText.toString();
    }
}
