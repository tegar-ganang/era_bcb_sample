package jp.go.aist.six.oval.model.unix;

import jp.go.aist.six.oval.model.Component;
import jp.go.aist.six.oval.model.Family;
import jp.go.aist.six.oval.model.sc.EntityItemBoolType;
import jp.go.aist.six.oval.model.sc.EntityItemIntType;
import jp.go.aist.six.oval.model.sc.EntityItemStringType;
import jp.go.aist.six.oval.model.sc.ItemType;
import jp.go.aist.six.oval.model.sc.StatusEnumeration;

/**
 * The file item holds information about the individual files found on a system.
 *
 * @author  Akihito Nakamura, AIST
 * @version $Id: FileItem.java 2279 2012-04-04 01:15:16Z nakamura5akihito@gmail.com $
 * @see <a href="http://oval.mitre.org/language/">OVAL Language</a>
 */
public class FileItem extends ItemType {

    private EntityItemStringType filepath;

    private EntityItemStringType path;

    private EntityItemStringType filename;

    private EntityItemStringType type;

    private EntityItemIntType group_id;

    private EntityItemIntType user_id;

    private EntityItemIntType a_time;

    private EntityItemIntType c_time;

    private EntityItemIntType m_time;

    private EntityItemIntType size;

    private EntityItemBoolType suid;

    private EntityItemBoolType sgid;

    private EntityItemBoolType sticky;

    private EntityItemBoolType uread;

    private EntityItemBoolType uwrite;

    private EntityItemBoolType uexec;

    private EntityItemBoolType gread;

    private EntityItemBoolType gwrite;

    private EntityItemBoolType gexec;

    private EntityItemBoolType oread;

    private EntityItemBoolType owrite;

    private EntityItemBoolType oexec;

    private EntityItemBoolType has_extended_acl;

    /**
     * Constructor.
     */
    public FileItem() {
        this(0);
    }

    public FileItem(final int id) {
        this(id, null);
    }

    public FileItem(final int id, final StatusEnumeration status) {
        super(id, status);
        _oval_family = Family.UNIX;
        _oval_component = Component.FILE;
    }

    /**
     */
    public void setFilepath(final EntityItemStringType filepath) {
        this.filepath = filepath;
    }

    public EntityItemStringType getFilepath() {
        return filepath;
    }

    /**
     */
    public void setPath(final EntityItemStringType path) {
        this.path = path;
    }

    public EntityItemStringType getPath() {
        return path;
    }

    /**
     */
    public void setFilename(final EntityItemStringType filename) {
        this.filename = filename;
    }

    public EntityItemStringType getFilename() {
        return filename;
    }

    /**
     */
    public void setType(final EntityItemStringType type) {
        this.type = type;
    }

    public EntityItemStringType getType() {
        return type;
    }

    /**
     */
    public void setGroupID(final EntityItemIntType group_id) {
        this.group_id = group_id;
    }

    public EntityItemIntType getGroupID() {
        return group_id;
    }

    public FileItem groupID(final EntityItemIntType group_id) {
        setGroupID(group_id);
        return this;
    }

    /**
     */
    public void setUserID(final EntityItemIntType user_id) {
        this.user_id = user_id;
    }

    public EntityItemIntType getUserID() {
        return user_id;
    }

    /**
     */
    public void setATime(final EntityItemIntType a_time) {
        this.a_time = a_time;
    }

    public EntityItemIntType getATime() {
        return a_time;
    }

    /**
     */
    public void setCTime(final EntityItemIntType c_time) {
        this.c_time = c_time;
    }

    public EntityItemIntType getCTime() {
        return c_time;
    }

    /**
     */
    public void setMTime(final EntityItemIntType m_time) {
        this.m_time = m_time;
    }

    public EntityItemIntType getMTime() {
        return m_time;
    }

    /**
     */
    public void setSize(final EntityItemIntType size) {
        this.size = size;
    }

    public EntityItemIntType getSize() {
        return size;
    }

    /**
     */
    public void setSuID(final EntityItemBoolType suid) {
        this.suid = suid;
    }

    public EntityItemBoolType getSuID() {
        return suid;
    }

    /**
     */
    public void setSgID(final EntityItemBoolType sgid) {
        this.sgid = sgid;
    }

    public EntityItemBoolType getSgID() {
        return sgid;
    }

    /**
     */
    public void setSticky(final EntityItemBoolType sticky) {
        this.sticky = sticky;
    }

    public EntityItemBoolType getSticky() {
        return sticky;
    }

    /**
     */
    public void setURead(final EntityItemBoolType uread) {
        this.uread = uread;
    }

    public EntityItemBoolType getURead() {
        return uread;
    }

    /**
     */
    public void setUWrite(final EntityItemBoolType uwrite) {
        this.uwrite = uwrite;
    }

    public EntityItemBoolType getUWrite() {
        return uwrite;
    }

    /**
     */
    public void setUExec(final EntityItemBoolType uexec) {
        this.uexec = uexec;
    }

    public EntityItemBoolType getUExec() {
        return uexec;
    }

    /**
     */
    public void setGRead(final EntityItemBoolType gread) {
        this.gread = gread;
    }

    public EntityItemBoolType getGRead() {
        return gread;
    }

    /**
     */
    public void setGWrite(final EntityItemBoolType gwrite) {
        uwrite = gwrite;
    }

    public EntityItemBoolType getGWrite() {
        return gwrite;
    }

    /**
     */
    public void setGExec(final EntityItemBoolType gexec) {
        this.gexec = gexec;
    }

    public EntityItemBoolType getGExec() {
        return gexec;
    }

    /**
     */
    public void setORead(final EntityItemBoolType oread) {
        this.oread = oread;
    }

    public EntityItemBoolType getORead() {
        return oread;
    }

    /**
     */
    public void setOWrite(final EntityItemBoolType owrite) {
        this.owrite = owrite;
    }

    public EntityItemBoolType getOWrite() {
        return owrite;
    }

    /**
     */
    public void setOExec(final EntityItemBoolType oexec) {
        this.oexec = oexec;
    }

    public EntityItemBoolType getOExec() {
        return oexec;
    }

    /**
     */
    public void setHasExtendedAcl(final EntityItemBoolType has_extended_acl) {
        this.has_extended_acl = has_extended_acl;
    }

    public EntityItemBoolType getHasExtendedAcl() {
        return has_extended_acl;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FileItem)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "file_item[" + super.toString() + ", filepath=" + getFilepath() + ", path=" + getPath() + ", filename=" + getFilename() + ", type=" + getType() + ", group_id=" + getGroupID() + ", user_id=" + getUserID() + ", a_time=" + getATime() + ", c_time=" + getCTime() + ", m_time=" + getMTime() + ", size=" + getSize() + ", suid=" + getSuID() + ", sgid=" + getSgID() + ", sticky=" + getSticky() + ", uread=" + getURead() + ", uwrite=" + getUWrite() + ", uexec=" + getUExec() + ", gread=" + getGRead() + ", gwrite=" + getGWrite() + ", gexec=" + getGExec() + ", oread=" + getORead() + ", owrite=" + getOWrite() + ", oexec=" + getOExec() + ", has_extended_acl=" + getHasExtendedAcl() + "]";
    }
}
