package org.unicore.ajo;

import org.unicore.sets.ResourceSet;

/**
 * Change the permissions on a file or directory.
 * <p>
 * The permissions are changed for the owner of the file
 * only (group and world permissions are left unchanged).
 * The authorisation process must result in an incarnation of the user
 * that allows these changes to be made.
 * <p>
 * The location of the file is determined by the resources
 * as described in {@link FileAction}.
 *
 * @see org.unicore.outcome.ChangePermissions_Outcome
 *
 * @author S. van den Berghe (fecit)
 *
 * @since AJO 3
 *
 * @version $Id: ChangePermissions.java,v 1.3 2005/01/27 13:37:41 svenvdb Exp $  
 * 
 **/
public final class ChangePermissions extends FileAction {

    static final long serialVersionUID = -6770829373675073908L;

    /**
     * Create a new ChangePermissions.
     * 
     * @param name The name of the ChangePermissions
     * @param extra_information Client supplied extra information about the ChangePermissions
     * @param resources Resources needed to execute the ChangePermissions
     * @param target The name of the file to change
     * @param read Can the Xlogin read the target file following the ChangePermissions
     * @param write Can the Xlogin write the target file following the ChangePermissions
     * @param execute Can the Xlogin execute the target file following the ChangePermissions
     *
     **/
    public ChangePermissions(String name, byte[] extra_information, ResourceSet resources, String target, boolean read, boolean write, boolean execute) {
        super(name, extra_information, resources);
        this.target = target;
        this.read = read;
        this.write = write;
        this.execute = execute;
    }

    /**
     * Create a new ChangePermissions.
     * <p>
     * @param name The name of the ChangePermissions
     * @param resources Resources needed to execute the ChangePermissions
     * @param target The name of the file to change
     * @param read Can the Xlogin read the target file following the ChangePermissions
     * @param write Can the Xlogin write the target file following the ChangePermissions
     * @param execute Can the Xlogin execute the target file following the ChangePermissions
     *
     **/
    public ChangePermissions(String name, ResourceSet resources, String target, boolean read, boolean write, boolean execute) {
        this(name, (byte[]) null, resources, target, read, write, execute);
    }

    /**
     * Create a new ChangePermissions.
     * <p>
     * Sets read, write and execute to true.
     *
     * @param name The name of the ChangePermissions
     *
     **/
    public ChangePermissions(String name) {
        this(name, (byte[]) null, (ResourceSet) null, (String) null, true, true, true);
    }

    public ChangePermissions() {
        this("");
    }

    private String target;

    /**
     * Return the file for which the permissions will be changed.
     *
     **/
    public String getTarget() {
        return target;
    }

    /**
     * Set the file for which the permissions will be changed.
     *
     **/
    public void setTarget(String target) {
        this.target = target;
    }

    private boolean read;

    /**
     * The value of the Xlogin's read permission on the file after
     * the execution of this task.
     *
     **/
    public boolean canRead() {
        return read;
    }

    /**
     * Set the value of the Xlogin's read permission on the file after
     * the execution of this task.
     *
     **/
    public void setRead(boolean read) {
        this.read = read;
    }

    private boolean write;

    /**
     * The value of the Xlogin's write permission on the file after
     * the execution of this task.
     *
     **/
    public boolean canWrite() {
        return write;
    }

    /**
     * Set the value of the Xlogin's write permission on the file after
     * the execution of this task.
     *
     **/
    public void setWrite(boolean write) {
        this.write = write;
    }

    private boolean execute;

    /**
     * The value of the Xlogin's execute permission on the file after
     * the execution of this task.
     *
     **/
    public boolean canExecute() {
        return execute;
    }

    /**
     * Set the value of the Xlogin's execute permission on the file after
     * the execution of this task.
     *
     **/
    public void setExecute(boolean execute) {
        this.execute = execute;
    }
}
