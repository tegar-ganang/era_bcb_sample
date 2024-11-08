package at.fhjoanneum.aim.sdi.project.svnconfig;

import at.fhjoanneum.aim.sdi.project.utilities.GlobalProperties;

public class Permission {

    private boolean writeable;

    private boolean readable;

    public Permission() {
    }

    public Permission(boolean readAccess, boolean writeAccess) {
        this.readable = readAccess;
        this.writeable = (readAccess == true && writeAccess == true ? true : false);
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    @Override
    public String toString() {
        String ret = "";
        if (isReadable() == true) {
            ret += "r";
        }
        if (isWriteable() == true) {
            ret += "w";
        }
        return ret;
    }
}
