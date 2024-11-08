package jp.go.aist.six.oval.model.unix;

import jp.go.aist.six.oval.model.Component;
import jp.go.aist.six.oval.model.Family;
import jp.go.aist.six.oval.model.definitions.EntityStateBoolType;
import jp.go.aist.six.oval.model.definitions.EntityStateIntType;
import jp.go.aist.six.oval.model.definitions.EntityStateStringType;
import jp.go.aist.six.oval.model.definitions.StateType;

/**
 * The file state defines the different metadata associate with a UNIX file.
 *
 * @author  Akihito Nakamura, AIST
 * @version $Id: FileState.java 2279 2012-04-04 01:15:16Z nakamura5akihito@gmail.com $
 * @see <a href="http://oval.mitre.org/language/">OVAL Language</a>
 */
public class FileState extends StateType {

    private EntityStateStringType filepath;

    private EntityStateStringType path;

    private EntityStateStringType filename;

    private EntityStateStringType type;

    private EntityStateIntType group_id;

    private EntityStateIntType user_id;

    private EntityStateIntType a_time;

    private EntityStateIntType c_time;

    private EntityStateIntType m_time;

    private EntityStateIntType size;

    private EntityStateBoolType suid;

    private EntityStateBoolType sgid;

    private EntityStateBoolType sticky;

    private EntityStateBoolType uread;

    private EntityStateBoolType uwrite;

    private EntityStateBoolType uexec;

    private EntityStateBoolType gread;

    private EntityStateBoolType gwrite;

    private EntityStateBoolType gexec;

    private EntityStateBoolType oread;

    private EntityStateBoolType owrite;

    private EntityStateBoolType oexec;

    private EntityStateBoolType has_extended_acl;

    /**
     * Constructor.
     */
    public FileState() {
        this(null, 0);
    }

    public FileState(final String id, final int version) {
        this(id, version, null);
    }

    public FileState(final String id, final int version, final String comment) {
        super(id, version, comment);
        _oval_family = Family.UNIX;
        _oval_component = Component.FILE;
    }

    /**
     */
    public void setFilepath(final EntityStateStringType filepath) {
        this.filepath = filepath;
    }

    public EntityStateStringType getFilepath() {
        return filepath;
    }

    public FileState filepath(final EntityStateStringType filepath) {
        setFilepath(filepath);
        return this;
    }

    /**
     */
    public void setPath(final EntityStateStringType path) {
        this.path = path;
    }

    public EntityStateStringType getPath() {
        return path;
    }

    public FileState path(final EntityStateStringType path) {
        setPath(path);
        return this;
    }

    /**
     */
    public void setFilename(final EntityStateStringType filename) {
        this.filename = filename;
    }

    public EntityStateStringType getFilename() {
        return filename;
    }

    public FileState filename(final EntityStateStringType filename) {
        setFilename(filename);
        return this;
    }

    /**
     */
    public void setType(final EntityStateStringType type) {
        this.type = type;
    }

    public EntityStateStringType getType() {
        return type;
    }

    /**
     */
    public void setGroupID(final EntityStateIntType group_id) {
        this.group_id = group_id;
    }

    public EntityStateIntType getGroupID() {
        return group_id;
    }

    public FileState groupID(final EntityStateIntType group_id) {
        setGroupID(group_id);
        return this;
    }

    /**
     */
    public void setUserID(final EntityStateIntType user_id) {
        this.user_id = user_id;
    }

    public EntityStateIntType getUserID() {
        return user_id;
    }

    public FileState userID(final EntityStateIntType user_id) {
        setUserID(user_id);
        return this;
    }

    /**
     */
    public void setATime(final EntityStateIntType a_time) {
        this.a_time = a_time;
    }

    public EntityStateIntType getATime() {
        return a_time;
    }

    /**
     */
    public void setCTime(final EntityStateIntType c_time) {
        this.c_time = c_time;
    }

    public EntityStateIntType getCTime() {
        return c_time;
    }

    /**
     */
    public void setMTime(final EntityStateIntType m_time) {
        this.m_time = m_time;
    }

    public EntityStateIntType getMTime() {
        return m_time;
    }

    /**
     */
    public void setSize(final EntityStateIntType size) {
        this.size = size;
    }

    public EntityStateIntType getSize() {
        return size;
    }

    /**
     */
    public void setSuID(final EntityStateBoolType suid) {
        this.suid = suid;
    }

    public EntityStateBoolType getSuID() {
        return suid;
    }

    /**
     */
    public void setSgID(final EntityStateBoolType sgid) {
        this.sgid = sgid;
    }

    public EntityStateBoolType getSgID() {
        return sgid;
    }

    /**
     */
    public void setSticky(final EntityStateBoolType sticky) {
        this.sticky = sticky;
    }

    public EntityStateBoolType getSticky() {
        return sticky;
    }

    /**
     */
    public void setURead(final EntityStateBoolType uread) {
        this.uread = uread;
    }

    public EntityStateBoolType getURead() {
        return uread;
    }

    /**
     */
    public void setUWrite(final EntityStateBoolType uwrite) {
        this.uwrite = uwrite;
    }

    public EntityStateBoolType getUWrite() {
        return uwrite;
    }

    /**
     */
    public void setUExec(final EntityStateBoolType uexec) {
        this.uexec = uexec;
    }

    public EntityStateBoolType getUExec() {
        return uexec;
    }

    /**
     */
    public void setGRead(final EntityStateBoolType gread) {
        this.gread = gread;
    }

    public EntityStateBoolType getGRead() {
        return gread;
    }

    /**
     */
    public void setGWrite(final EntityStateBoolType gwrite) {
        uwrite = gwrite;
    }

    public EntityStateBoolType getGWrite() {
        return gwrite;
    }

    /**
     */
    public void setGExec(final EntityStateBoolType gexec) {
        this.gexec = gexec;
    }

    public EntityStateBoolType getGExec() {
        return gexec;
    }

    /**
     */
    public void setORead(final EntityStateBoolType oread) {
        this.oread = oread;
    }

    public EntityStateBoolType getORead() {
        return oread;
    }

    /**
     */
    public void setOWrite(final EntityStateBoolType owrite) {
        this.owrite = owrite;
    }

    public EntityStateBoolType getOWrite() {
        return owrite;
    }

    /**
     */
    public void setOExec(final EntityStateBoolType oexec) {
        this.oexec = oexec;
    }

    public EntityStateBoolType getOExec() {
        return oexec;
    }

    /**
     */
    public void setHasExtendedAcl(final EntityStateBoolType has_extended_acl) {
        this.has_extended_acl = has_extended_acl;
    }

    public EntityStateBoolType getHasExtendedAcl() {
        return has_extended_acl;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FileState)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "file_state[" + super.toString() + ", filepath=" + getFilepath() + ", path=" + getPath() + ", filename=" + getFilename() + ", type=" + getType() + ", group_id=" + getGroupID() + ", user_id=" + getUserID() + ", a_time=" + getATime() + ", c_time=" + getCTime() + ", m_time=" + getMTime() + ", size=" + getSize() + ", suid=" + getSuID() + ", sgid=" + getSgID() + ", sticky=" + getSticky() + ", uread=" + getURead() + ", uwrite=" + getUWrite() + ", uexec=" + getUExec() + ", gread=" + getGRead() + ", gwrite=" + getGWrite() + ", gexec=" + getGExec() + ", oread=" + getORead() + ", owrite=" + getOWrite() + ", oexec=" + getOExec() + ", has_extended_acl=" + getHasExtendedAcl() + "]";
    }
}
