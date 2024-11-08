package org.unicore.outcome;

import org.unicore.Unicore;
import java.util.Date;

/**
 * Information about a file.
 * 
 * @see ListDirectory_Outcome
 * @see FileCheck_Outcome
 *
 * @author S. van den Berghe (fecit)
 *
 * @since AJO 3
 *
 * @version $Id: XFile.java,v 1.2 2004/05/26 16:31:44 svenvdb Exp $
 * 
 **/
public class XFile extends Unicore {

    static final long serialVersionUID = -4200793665133520644L;

    private String name;

    private long size;

    private Date modify_date;

    private boolean owns;

    private boolean can_write;

    private boolean can_read;

    private boolean can_execute;

    private String extra_information;

    /**
     * Create a new XFile.
     *
     * @param name              The name of the file (full path to)
     * @param size              The size of the file in bytes.
     * @param modify_date       The time of last changes to the file.
     * @param owns              true if the Xlogin owns the file
     * @param can_write         true if the Xlogin can write to the file
     * @param can_read          true if the Xlogin can read the file
     * @param can_execute       true if the Xlogin can  execute the file
     * @param extra_information Any further information about the file returned by the target OS.
     *
     **/
    public XFile(String name, long size, Date modify_date, boolean owns, boolean can_read, boolean can_write, boolean can_execute, String extra_information) {
        setName(name);
        setSize(size);
        setModifyDate(modify_date);
        setIsOwner(owns);
        setCanWrite(can_write);
        setCanRead(can_read);
        setCanExecute(can_execute);
        setExtraInformation(extra_information);
    }

    /**
     * Create a new XFile.
     *
     **/
    public XFile() {
        name = "";
    }

    /**
     * Return the name of the file (full path to file)
     *
     **/
    public String getName() {
        return name;
    }

    /**
     * Set the name of the file.
     *
     **/
    public void setName(String s) {
        name = s;
    }

    /**
     * Return the size of the file (in bytes)
     *
     **/
    public long getSize() {
        return size;
    }

    /**
     * Set the size of the file
     *
     **/
    public void setSize(long i) {
        size = i;
    }

    /**
     * Return the time of last changes to the file.
     *
     **/
    public Date getModifyDate() {
        return modify_date;
    }

    /**
     * Set the time of last changes to the file.
     *
     **/
    public void setModifyDate(Date d) {
        modify_date = d;
    }

    /**
     * Returns true if the Xlogin owns the file
     *
     **/
    public boolean isOwner() {
        return owns;
    }

    /**
     * Set Xlogin ownership of the file
     *
     **/
    public void setIsOwner(boolean b) {
        owns = b;
    }

    /**
     * Returns true if the Xlogin can read the file
     *
     **/
    public boolean canRead() {
        return can_read;
    }

    /**
     * Set Xlogin ability to read the file
     *
     **/
    public void setCanRead(boolean b) {
        can_read = b;
    }

    /**
     * Returns true if the Xlogin can write to the file
     *
     **/
    public boolean canWrite() {
        return can_write;
    }

    /**
     * Set Xlogin ability to write to the file
     *
     **/
    public void setCanWrite(boolean b) {
        can_write = b;
    }

    /**
     * Returns true if the Xlogin can  execute the file
     *
     **/
    public boolean canExecute() {
        return can_execute;
    }

    /**
     * Set Xlogin ability to execute the file
     *
     **/
    public void setCanExecute(boolean b) {
        can_execute = b;
    }

    /**
     * Returns extra information about the file as provided
     * by the target system. 
     * <p>
     * An implementation of an NJS can
     * choose to supply some extra information about 
     * the file in this field. This information will be
     * seamful.
     *
     **/
    public String getExtraInformation() {
        return extra_information;
    }

    /**
     * Set file extra information.
     *
     **/
    public void setExtraInformation(String extra_information) {
        this.extra_information = extra_information;
    }
}
