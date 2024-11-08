package net.sf.cclearly.conn.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import net.sf.cclearly.entities.Attachment;
import net.sf.cclearly.entities.MachineID;
import net.sf.cclearly.entities.Task;
import net.sf.cclearly.entities.TaskNote;
import net.sf.cclearly.entities.User;
import net.sf.cclearly.entities.Task.Priority;
import net.sf.cclearly.entities.Task.TaskStatus;
import za.dats.net.TransportableObject;
import za.dats.util.I18nProvider;
import za.dats.util.time.TimeSpan;

public class TaskUpdateMessage implements TransportableObject {

    private boolean changedActioner;

    private User actioner;

    private boolean changedRequestor;

    private User requestor;

    private boolean changedComments;

    private String comments;

    private boolean changedEstTime;

    private TimeSpan estimatedTime;

    private boolean changedIdMap;

    private HashMap<MachineID, Long> idMap;

    private boolean changedPriority;

    private Priority priority;

    private boolean changedStatus;

    private TaskStatus status;

    private boolean changedTargetDate;

    private Date targetDate;

    private boolean changedDateArchived;

    private Date dateArchived;

    private boolean changedTitle;

    private String title;

    private boolean changedSignoff;

    private boolean signedOff;

    private boolean attachmentsChanged;

    private List<Attachment> changedAttachments = new LinkedList<Attachment>();

    private HashMap<Attachment, byte[]> changedFiles = new HashMap<Attachment, byte[]>();

    private boolean attachmentsDeleted;

    private List<Attachment> deletedAttachments = new LinkedList<Attachment>();

    private boolean changedRequestorFolder;

    private String requestorFolder;

    private boolean changedNotes;

    private List<TaskNote> notes = new LinkedList<TaskNote>();

    private boolean changedTaskNumber;

    private String taskNumber;

    public TaskUpdateMessage() {
    }

    public TaskUpdateMessage(Task oldTask, Task newTask, MachineID currentUser) {
        setDiff(oldTask, newTask);
    }

    private boolean objectChanged(Object first, Object second) {
        if (first == null) {
            if (second != null) {
                return true;
            }
        } else if (second == null) {
            return true;
        } else if (!first.equals(second)) {
            return true;
        }
        return false;
    }

    private void setDiff(Task oldTask, Task newTask) {
        idMap = new HashMap<MachineID, Long>(newTask.getIdMap());
        taskNumber = newTask.getTaskNumber();
        if (objectChanged(oldTask.getActioner(), newTask.getActioner())) {
            changedActioner = true;
            actioner = newTask.getActioner().clone();
        }
        if (objectChanged(oldTask.getRequestor(), newTask.getRequestor())) {
            changedRequestor = true;
            requestor = newTask.getRequestor().clone();
        }
        if (objectChanged(oldTask.getComments(), newTask.getComments())) {
            changedComments = true;
            comments = newTask.getComments();
        }
        if (objectChanged(oldTask.getEstimatedTime(), newTask.getEstimatedTime())) {
            changedEstTime = true;
            estimatedTime = newTask.getEstimatedTime();
        }
        if (objectChanged(oldTask.getIdMap(), newTask.getIdMap())) {
            changedIdMap = true;
        }
        if (objectChanged(oldTask.getPriority(), newTask.getPriority())) {
            changedPriority = true;
            priority = newTask.getPriority();
        }
        if (objectChanged(oldTask.getStatus(), newTask.getStatus())) {
            changedStatus = true;
            status = newTask.getStatus();
        }
        if (objectChanged(oldTask.getTargetDate(), newTask.getTargetDate())) {
            changedTargetDate = true;
            targetDate = newTask.getTargetDate();
        }
        if (objectChanged(oldTask.getDateArchived(), newTask.getDateArchived())) {
            changedDateArchived = true;
            dateArchived = newTask.getDateArchived();
        }
        if (objectChanged(oldTask.getTitle(), newTask.getTitle())) {
            changedTitle = true;
            title = newTask.getTitle();
        }
        if (objectChanged(oldTask.isSignedOff(), newTask.isSignedOff())) {
            changedSignoff = true;
            signedOff = newTask.isSignedOff();
        }
        if (objectChanged(oldTask.getRequestorFolder(), newTask.getRequestorFolder())) {
            changedRequestorFolder = true;
            requestorFolder = newTask.getRequestorFolder();
        }
        for (Attachment oldAttachment : oldTask.getAttachmentMetadata()) {
            if (!newTask.getAttachmentMetadata().contains(oldAttachment)) {
                attachmentsDeleted = true;
                deletedAttachments.add(oldAttachment);
            }
        }
        for (Attachment newAttachment : newTask.getAttachmentMetadata()) {
            if (newAttachment == null) {
                continue;
            }
            if (!oldTask.getAttachmentMetadata().contains(newAttachment)) {
                attachmentsChanged = true;
                changedAttachments.add(newAttachment);
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(data);
                File attachment = new File(newTask.getAttachFolder(), newAttachment.getFilename());
                try {
                    writeFile(out, attachment);
                    out.close();
                    changedFiles.put(newAttachment, data.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Attachment oldAttachment = oldTask.getAttachmentMetadata().get(oldTask.getAttachmentMetadata().indexOf(newAttachment));
                if (oldAttachment == null) {
                    continue;
                }
                if (oldAttachment.getVersion() != newAttachment.getVersion()) {
                    attachmentsChanged = true;
                    changedAttachments.add(newAttachment);
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(data);
                    File attachment = new File(newTask.getAttachFolder(), newAttachment.getFilename());
                    try {
                        writeFile(out, attachment);
                        out.close();
                        changedFiles.put(newAttachment, data.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (((oldAttachment.getDescription() == null) && (newAttachment.getDescription() != null)) || ((oldAttachment.getDescription() != null) && (newAttachment.getDescription() == null)) || ((oldAttachment.getDescription() != null) && (newAttachment.getDescription() != null) && (!oldAttachment.getDescription().equals(newAttachment.getDescription())))) {
                    attachmentsChanged = true;
                    changedAttachments.add(newAttachment);
                }
            }
        }
        for (TaskNote note : newTask.getNotes()) {
            if (!oldTask.getNotes().contains(note)) {
                changedNotes = true;
                notes.add(note);
            }
        }
        if (objectChanged(oldTask.getTaskNumber(), newTask.getTaskNumber())) {
            changedTaskNumber = true;
        }
    }

    public boolean hasChanged() {
        return changedActioner || changedComments || changedEstTime || changedIdMap || changedIdMap || changedPriority || changedRequestor || changedSignoff || changedStatus || changedTargetDate || changedTitle || changedDateArchived || attachmentsChanged || attachmentsDeleted || changedRequestorFolder || changedNotes || changedTaskNumber;
    }

    public int getMaxVersion() {
        return 4;
    }

    public void readObject(DataInputStream in, int version) throws IOException {
        idMap = new HashMap<MachineID, Long>();
        int idCount = in.readInt();
        for (int i = 0; i < idCount; i++) {
            MachineID readMachineID = new MachineID();
            readMachineID.readObject(in, 1);
            long taskID = in.readLong();
            idMap.put(readMachineID, taskID);
        }
        changedActioner = in.readBoolean();
        if (changedActioner) {
            actioner = new User();
            actioner.readObject(in, 1);
        }
        changedComments = in.readBoolean();
        if (changedComments) {
            comments = in.readUTF();
        }
        changedEstTime = in.readBoolean();
        if (changedEstTime) {
            estimatedTime = new TimeSpan(in.readInt());
        }
        changedIdMap = in.readBoolean();
        changedPriority = in.readBoolean();
        if (changedPriority) {
            priority = Priority.valueOf(in.readUTF());
        }
        changedRequestor = in.readBoolean();
        if (changedRequestor) {
            requestor = new User();
            requestor.readObject(in, 1);
        }
        changedSignoff = in.readBoolean();
        if (changedSignoff) {
            signedOff = in.readBoolean();
        }
        changedStatus = in.readBoolean();
        if (changedStatus) {
            int statusOrdinal = in.readInt();
            if (statusOrdinal == -1) {
                status = null;
            } else {
                TaskStatus[] statusList = TaskStatus.values();
                status = statusList[statusOrdinal];
            }
        }
        changedTargetDate = in.readBoolean();
        if (changedTargetDate) {
            long dt = in.readLong();
            if (dt == 0) {
                targetDate = null;
            } else {
                targetDate = new Date(dt);
            }
        }
        changedTitle = in.readBoolean();
        if (changedTitle) {
            title = in.readUTF();
        }
        if (version >= 2) {
            changedDateArchived = in.readBoolean();
            if (changedDateArchived) {
                dateArchived = new Date(in.readLong());
            }
        }
        if (version >= 3) {
            changedRequestorFolder = in.readBoolean();
            if (changedRequestorFolder) {
                requestorFolder = in.readUTF();
            }
            attachmentsDeleted = in.readBoolean();
            if (attachmentsDeleted) {
                int deleteCount = in.readInt();
                for (int i = 0; i < deleteCount; i++) {
                    Attachment attachment = new Attachment();
                    attachment.readObject(in, 2);
                    deletedAttachments.add(attachment);
                }
            }
            attachmentsChanged = in.readBoolean();
            if (attachmentsChanged) {
                int changeCount = in.readInt();
                for (int i = 0; i < changeCount; i++) {
                    Attachment attachment = new Attachment();
                    attachment.readObject(in, 2);
                    changedAttachments.add(attachment);
                    long fileLength = in.readLong();
                    if (fileLength > 0) {
                        byte[] tmpFile = new byte[(int) fileLength];
                        in.readFully(tmpFile);
                        changedFiles.put(attachment, tmpFile);
                    }
                }
            }
            changedNotes = in.readBoolean();
            if (changedNotes) {
                int noteCount = in.readInt();
                for (int i = 0; i < noteCount; i++) {
                    TaskNote note = new TaskNote();
                    note.readObject(in, 2);
                    notes.add(note);
                }
            }
        }
        if (version >= 4) {
            changedTaskNumber = in.readBoolean();
            boolean hasTaskNumber = in.readBoolean();
            if (hasTaskNumber) {
                taskNumber = in.readUTF();
            }
        }
    }

    public void writeObject(DataOutputStream out, int version) throws IOException {
        out.writeInt(idMap.size());
        for (MachineID machineID : idMap.keySet()) {
            machineID.writeObject(out, 1);
            out.writeLong(idMap.get(machineID));
        }
        out.writeBoolean(changedActioner);
        if (changedActioner) {
            actioner.writeObject(out, 1);
        }
        out.writeBoolean(changedComments);
        if (changedComments) {
            out.writeUTF(comments);
        }
        out.writeBoolean(changedEstTime);
        if (changedEstTime) {
            out.writeInt(estimatedTime.getValue());
        }
        out.writeBoolean(changedIdMap);
        out.writeBoolean(changedPriority);
        if (changedPriority) {
            out.writeUTF(priority.name());
        }
        out.writeBoolean(changedRequestor);
        if (changedRequestor) {
            requestor.writeObject(out, 1);
        }
        out.writeBoolean(changedSignoff);
        if (changedSignoff) {
            out.writeBoolean(signedOff);
        }
        out.writeBoolean(changedStatus);
        if (changedStatus) {
            out.writeInt(status == null ? -1 : status.ordinal());
        }
        out.writeBoolean(changedTargetDate);
        if (changedTargetDate) {
            if (targetDate == null) {
                out.writeLong(0);
            } else {
                out.writeLong(targetDate.getTime());
            }
        }
        out.writeBoolean(changedTitle);
        if (changedTitle) {
            out.writeUTF(title);
        }
        if (version >= 2) {
            out.writeBoolean(changedDateArchived);
            if (changedDateArchived) {
                out.writeLong(dateArchived.getTime());
            }
        }
        if (version >= 3) {
            out.writeBoolean(changedRequestorFolder);
            if (changedRequestorFolder) {
                out.writeUTF(requestorFolder);
            }
            out.writeBoolean(attachmentsDeleted);
            if (attachmentsDeleted) {
                out.writeInt(deletedAttachments.size());
                for (Attachment attachment : deletedAttachments) {
                    attachment.writeObject(out, 2);
                }
            }
            out.writeBoolean(attachmentsChanged);
            if (attachmentsChanged) {
                out.writeInt(changedAttachments.size());
                for (Attachment attachment : changedAttachments) {
                    attachment.writeObject(out, 2);
                    if (changedFiles.containsKey(attachment)) {
                        out.writeLong(changedFiles.get(attachment).length);
                        out.write(changedFiles.get(attachment));
                    } else {
                        out.writeLong(0);
                    }
                }
            }
            out.writeBoolean(changedNotes);
            if (changedNotes) {
                out.writeInt(notes.size());
                for (TaskNote note : notes) {
                    note.writeObject(out, 2);
                }
            }
        }
        if (version >= 4) {
            out.writeBoolean(changedTaskNumber);
            out.writeBoolean(taskNumber != null);
            if (taskNumber != null) {
                out.writeUTF(taskNumber);
            }
        }
    }

    private void writeFile(DataOutputStream out, File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] data = new byte[4096];
        int readLen = 0;
        while ((readLen = in.read(data)) != -1) {
            out.write(data, 0, readLen);
        }
    }

    public void refreshTask(Task oldTask) {
        if (changedActioner) {
            oldTask.setActioner(actioner);
        }
        if (changedComments) {
            oldTask.setComments(comments);
        }
        if (changedEstTime) {
            oldTask.setEstimatedTime(estimatedTime);
        }
        if (changedIdMap) {
            oldTask.getIdMap().putAll(idMap);
        }
        if (changedPriority) {
            oldTask.setPriority(priority);
        }
        if (changedRequestor) {
            oldTask.setRequestor(requestor);
        }
        if (changedSignoff) {
            oldTask.setSignedOff(signedOff);
        }
        if (changedStatus) {
            oldTask.setStatus(status);
        }
        if (changedTargetDate) {
            oldTask.setTargetDate(targetDate);
        }
        if (changedTitle) {
            oldTask.setTitle(title);
        }
        if (changedDateArchived) {
            oldTask.setDateArchived(dateArchived);
        }
        if (changedRequestorFolder) {
            oldTask.setRequestorFolder(requestorFolder);
        }
        if ((changedTaskNumber) || ((taskNumber != null) && (oldTask.getTaskNumber() == null))) {
            oldTask.setTaskNumber(taskNumber);
        }
        if (attachmentsDeleted) {
            File attachFolder = oldTask.getAttachFolder();
            for (Attachment attachment : deletedAttachments) {
                oldTask.detachFile(new File(attachFolder, attachment.getFilename()));
            }
        }
        if (attachmentsChanged) {
            try {
                File tmpFolder = File.createTempFile("attach_", ".cclearly");
                tmpFolder.delete();
                tmpFolder.mkdir();
                tmpFolder.deleteOnExit();
                for (Attachment attachment : changedAttachments) {
                    if (!changedFiles.containsKey(attachment)) {
                        oldTask.addAttachMetadata(attachment);
                        continue;
                    }
                    try {
                        File tmpFile = new File(tmpFolder, attachment.getFilename());
                        FileOutputStream out = new FileOutputStream(tmpFile);
                        out.write(changedFiles.get(attachment));
                        out.close();
                        oldTask.attachFile(tmpFile, true);
                        oldTask.addAttachMetadata(attachment);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if (changedNotes) {
            for (TaskNote note : notes) {
                oldTask.addNote(note);
            }
        }
    }

    public static void addChangeNotes(User currentUser, Task oldTask, Task newTask) {
        TaskUpdateMessage msg = new TaskUpdateMessage(oldTask, newTask, currentUser.getMachineID());
        if (msg.changedActioner) {
            newTask.addNote(new TaskNote(currentUser, "Changed actioner from " + oldTask.getActioner() + " to " + newTask.getActioner(), true));
        }
        if (msg.changedComments) {
            newTask.addNote(new TaskNote(currentUser, "Changed description", true));
        }
        if (msg.changedEstTime) {
            if ((oldTask.getEstimatedTime() == null) || (oldTask.getEstimatedTime().getValue() == 0)) {
                newTask.addNote(new TaskNote(currentUser, "Set estimated time to " + newTask.getEstimatedTime(), true));
            } else {
                newTask.addNote(new TaskNote(currentUser, "Changed estimated time from " + oldTask.getEstimatedTime() + " to " + newTask.getEstimatedTime(), true));
            }
        }
        if (msg.changedPriority) {
            newTask.addNote(new TaskNote(currentUser, "Changed priority from " + oldTask.getPriority().getName() + " to " + newTask.getPriority().getName(), true));
        }
        if (msg.changedRequestor) {
            newTask.addNote(new TaskNote(currentUser, "Changed requestor from " + oldTask.getRequestor() + " to " + newTask.getRequestor(), true));
        }
        if (msg.changedSignoff) {
            newTask.addNote(new TaskNote(currentUser, "Signed off", true));
        }
        if (msg.changedStatus) {
            newTask.addNote(new TaskNote(currentUser, "Changed status from " + oldTask.getStatus().getName() + " to " + newTask.getStatus().getName(), true));
        }
        if (msg.changedTargetDate) {
            String toDate = I18nProvider.dateToMedium(newTask.getTargetDate());
            if (oldTask.getTargetDate() == null) {
                newTask.addNote(new TaskNote(currentUser, "Set target date to " + toDate, true));
            } else {
                String fromDate = I18nProvider.dateToMedium(oldTask.getTargetDate());
                newTask.addNote(new TaskNote(currentUser, "Changed target date from " + fromDate + " to " + toDate, true));
            }
        }
        if (msg.changedTitle) {
            newTask.addNote(new TaskNote(currentUser, "Changed title from \"" + oldTask.getTitle() + "\" to \"" + newTask.getTitle() + "\"", true));
        }
        if (msg.attachmentsDeleted) {
            for (Attachment attachment : msg.deletedAttachments) {
                newTask.addNote(new TaskNote(currentUser, "Removed attachment: " + attachment.getFilename(), true));
            }
        }
        if (msg.attachmentsChanged) {
            for (Attachment attachment : msg.changedAttachments) {
                newTask.addNote(new TaskNote(currentUser, "Added attachment: " + attachment.getFilename(), true));
            }
        }
    }

    public boolean containsIdFor(MachineID machineID) {
        return idMap.containsKey(machineID);
    }

    public long getIdFor(MachineID machineID) {
        return idMap.get(machineID);
    }

    public void setNewStatus(TaskStatus status) {
        changedStatus = true;
        this.status = status;
    }

    public HashMap<MachineID, Long> getIdMap() {
        return idMap;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public boolean hasChangedActioner() {
        return changedActioner;
    }

    public boolean hasChangedRequestor() {
        return changedRequestor;
    }

    public boolean isAttachmentsChanged() {
        return attachmentsChanged;
    }

    public boolean isAttachmentsDeleted() {
        return attachmentsDeleted;
    }

    public boolean isChangedActioner() {
        return changedActioner;
    }

    public List<Attachment> getChangedAttachments() {
        return changedAttachments;
    }

    public boolean isChangedComments() {
        return changedComments;
    }

    public boolean isChangedDateArchived() {
        return changedDateArchived;
    }

    public boolean isChangedEstTime() {
        return changedEstTime;
    }

    public HashMap<Attachment, byte[]> getChangedFiles() {
        return changedFiles;
    }

    public boolean isChangedIdMap() {
        return changedIdMap;
    }

    public boolean isChangedNotes() {
        return changedNotes;
    }

    public boolean isChangedPriority() {
        return changedPriority;
    }

    public boolean isChangedRequestor() {
        return changedRequestor;
    }

    public boolean isChangedRequestorFolder() {
        return changedRequestorFolder;
    }

    public boolean isChangedSignoff() {
        return changedSignoff;
    }

    public boolean isChangedStatus() {
        return changedStatus;
    }

    public boolean isChangedTargetDate() {
        return changedTargetDate;
    }

    public boolean isChangedTaskNumber() {
        return changedTaskNumber;
    }

    public boolean isChangedTitle() {
        return changedTitle;
    }

    public String getChangeString() {
        StringBuilder result = new StringBuilder();
        if (changedActioner) {
            result.append(", Actioner");
        }
        if (changedComments) {
            result.append(", Comments");
        }
        if (changedEstTime) {
            result.append(", Estimated Time");
        }
        if (changedIdMap) {
            result.append(", Id Mapping");
        }
        if (changedPriority) {
            result.append(", Priority");
        }
        if (changedRequestor) {
            result.append(", Requestor");
        }
        if (changedSignoff) {
            result.append(", Signed off");
        }
        if (changedStatus) {
            result.append(", Status");
        }
        if (changedTargetDate) {
            result.append(", Target Date");
        }
        if (changedTitle) {
            result.append(", Title");
        }
        if (changedDateArchived) {
            result.append(", Archived Date");
        }
        if (changedRequestorFolder) {
            result.append(", Requestor Folder");
        }
        if (changedTaskNumber) {
            result.append(", Task Number");
        }
        if (attachmentsDeleted) {
            result.append(", Deleted Attachments");
        }
        if (attachmentsChanged) {
            result.append(", Changed Attachments");
        }
        if (changedNotes) {
            result.append(", Notes");
        }
        return result.toString();
    }
}
