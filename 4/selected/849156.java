package filemanager.vfs.impl;

import filemanager.vfs.PermissionIfc;
import filemanager.vfs.ReadableFileIfc;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 *
 * @author sahaqiel
 */
public class JavaPermission implements PermissionIfc {

    private ReadableFileIfc fileIfc;

    private boolean isDir = false;

    private boolean read = false;

    private boolean write = false;

    private boolean exec = false;

    public JavaPermission(ReadableFileIfc fileIfc) {
        this.fileIfc = fileIfc;
        if (fileIfc.getBackingObject() instanceof File) {
            File f = (File) fileIfc.getBackingObject();
            isDir = f.isDirectory();
            read = f.canRead();
            write = f.canWrite();
            exec = f.canExecute();
        } else if (fileIfc.getBackingObject() instanceof FileObject) {
            FileObject fo = (FileObject) fileIfc.getBackingObject();
            try {
                isDir = false;
                read = fo.isReadable();
                write = fo.isWriteable();
                exec = false;
            } catch (FileSystemException ex) {
                ex.printStackTrace();
            }
        }
    }

    private JavaPermission(boolean read, boolean write, boolean exec) {
        this.read = read;
        this.write = write;
        this.exec = exec;
    }

    public static PermissionIfc getReadablePermission() {
        return new JavaPermission(true, false, false);
    }

    public static PermissionIfc getWritablePermission() {
        return new JavaPermission(true, true, false);
    }

    @Override
    public String getGroupName() {
        return "Unknown";
    }

    @Override
    public int getGroupPermissions() {
        return 0;
    }

    @Override
    public int getOtherPermissions() {
        return 0;
    }

    @Override
    public String getOwnerName() {
        return "Unknown";
    }

    @Override
    public int[] getPermissions() {
        int[] perm = { 0, 0, 0, 0 };
        if (isDir) {
            perm[0] = 1;
        }
        perm[1] = getUserPermissions();
        perm[2] = getGroupPermissions();
        perm[3] = getOtherPermissions();
        return perm;
    }

    @Override
    public int getUserPermissions() {
        int userPermisson = 0;
        if (read) {
            userPermisson = 1;
        }
        if (write) {
            userPermisson += 2;
        }
        if (exec) {
            userPermisson += 4;
        }
        return userPermisson;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        int[] perm = getPermissions();
        if (perm[0] == 1) {
            bldr.append('d');
        } else {
            bldr.append('-');
        }
        for (int i = 1; i < 4; i++) {
            bldr.append(toChar(perm[0]));
        }
        return bldr.toString();
    }

    private String toChar(int permission) {
        switch(permission) {
            case 1:
                return "r--";
            case 3:
                return "rw-";
            case 4:
                return "--x";
            case 5:
                return "r-x";
            case 7:
                return "rwx";
            default:
                return "---";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JavaPermission other = (JavaPermission) obj;
        if (this.read != other.read) {
            return false;
        }
        if (this.write != other.write) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.read ? 1 : 0);
        hash = 41 * hash + (this.write ? 1 : 0);
        return hash;
    }
}
