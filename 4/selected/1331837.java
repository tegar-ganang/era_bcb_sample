package org.proteored.miapeapi.interfaces;

import java.util.regex.PatternSyntaxException;

/**
 * Defines the permissions of a MIAPE document or Project
 * 
 * @author Salva
 * 
 */
public class Permission {

    private static final String SHARE = "share";

    private static final String DELETE = "delete";

    private static final String WRITE = "write";

    private static final String READ = "read";

    private boolean read = false;

    private boolean write = false;

    private boolean delete = false;

    private boolean share = false;

    /**
	 * Create an instance of the class from a string like: read=value,
	 * write=value, delete=value, share=value
	 * 
	 * @param permissionsString
	 */
    public Permission(String permissionsString) {
        if (permissionsString == null) return;
        final String[] split = permissionsString.split(",");
        for (String tmp : split) {
            try {
                final String[] split2 = tmp.split("=");
                if (READ.equals(split2[0])) {
                    this.read = Boolean.valueOf(split2[1]);
                }
                if (WRITE.equals(split2[0])) {
                    this.write = Boolean.valueOf(split2[1]);
                }
                if (DELETE.equals(split2[0])) {
                    this.delete = Boolean.valueOf(split2[1]);
                }
                if (SHARE.equals(split2[0])) {
                    this.share = Boolean.valueOf(split2[1]);
                }
            } catch (PatternSyntaxException ex) {
            }
        }
    }

    public Permission() {
        this.removePermissions();
    }

    @Override
    public String toString() {
        return Permission.READ + "=" + read + "," + Permission.WRITE + "=" + write + "," + Permission.DELETE + "=" + delete + "," + Permission.SHARE + "=" + share;
    }

    public boolean canRead() {
        return read;
    }

    public void canRead(boolean read) {
        this.read = read;
    }

    public boolean canWrite() {
        return write;
    }

    public void canWrite(boolean write) {
        this.write = write;
    }

    public boolean canDelete() {
        return delete;
    }

    public void canDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean canShare() {
        return share;
    }

    public void canShare(boolean share) {
        this.share = share;
    }

    public Permission setFullPermissions() {
        this.canDelete(true);
        this.canRead(true);
        this.canWrite(true);
        this.canShare(true);
        return this;
    }

    public Permission removePermissions() {
        this.canDelete(false);
        this.canRead(false);
        this.canWrite(false);
        this.canShare(false);
        return this;
    }

    public boolean hasFullPermissions() {
        if (read && write && delete && share) return true;
        return false;
    }
}
