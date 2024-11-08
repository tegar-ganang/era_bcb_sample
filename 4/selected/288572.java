package net.sf.jvdr.data.ejb;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class VdrRecordInfo implements Serializable {

    public static final long serialVersionUID = 1;

    public static enum Key {

        id, rawVdrAvailable, hashInfoVdr
    }

    private int id;

    private String channel, title, description;

    private String relativePath, hashInfoVdr;

    private Date record, recAdded;

    private boolean rawVdrAvailable;

    private VdrRecordEpg recordEpg;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getRecord() {
        return record;
    }

    public void setRecord(Date record) {
        this.record = record;
    }

    public Date getRecAdded() {
        return recAdded;
    }

    public void setRecAdded(Date recAdded) {
        this.recAdded = recAdded;
    }

    @OneToOne
    public VdrRecordEpg getRecordEpg() {
        return recordEpg;
    }

    public void setRecordEpg(VdrRecordEpg recordEpg) {
        this.recordEpg = recordEpg;
    }

    public boolean isRawVdrAvailable() {
        return rawVdrAvailable;
    }

    public void setRawVdrAvailable(boolean rawVdrAvailable) {
        this.rawVdrAvailable = rawVdrAvailable;
    }

    public String getHashInfoVdr() {
        return hashInfoVdr;
    }

    public void setHashInfoVdr(String hashInfoVdr) {
        this.hashInfoVdr = hashInfoVdr;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(id);
        sb.append(" raw?" + rawVdrAvailable);
        sb.append(" " + relativePath);
        sb.append(" " + record);
        sb.append(" " + channel);
        sb.append(" (" + title + ")");
        sb.append(" (" + description + ")");
        return sb.toString();
    }
}
