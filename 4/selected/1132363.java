package org.damour.base.client.objects;

import java.io.Serializable;

public class Permission implements Serializable, IHibernateFriendly {

    public static enum PERM {

        READ, WRITE, EXECUTE, CREATE_CHILD
    }

    ;

    public PermissibleObject permissibleObject;

    public SecurityPrincipal securityPrincipal;

    public Boolean readPerm = false;

    public Boolean writePerm = false;

    public Boolean executePerm = false;

    public Boolean createChildPerm = false;

    public Permission() {
    }

    public boolean isFieldKey(String fieldName) {
        if (fieldName.equals("permissibleObject")) {
            return true;
        } else if (fieldName.equals("securityPrincipal")) {
            return true;
        }
        return false;
    }

    public boolean isFieldUnique(String fieldName) {
        return false;
    }

    public Boolean isCreateChildPerm() {
        if (createChildPerm == null) {
            createChildPerm = false;
        }
        return createChildPerm;
    }

    public void setCreateChildPerm(Boolean createChildPerm) {
        if (createChildPerm == null) {
            createChildPerm = false;
        }
        this.createChildPerm = createChildPerm;
    }

    public boolean isReadPerm() {
        return readPerm;
    }

    public void setReadPerm(boolean readPerm) {
        this.readPerm = readPerm;
    }

    public boolean isWritePerm() {
        return writePerm;
    }

    public void setWritePerm(boolean writePerm) {
        this.writePerm = writePerm;
    }

    public boolean isExecutePerm() {
        return executePerm;
    }

    public void setExecutePerm(boolean executePerm) {
        this.executePerm = executePerm;
    }

    public PermissibleObject getPermissibleObject() {
        return permissibleObject;
    }

    public void setPermissibleObject(PermissibleObject permissibleObject) {
        this.permissibleObject = permissibleObject;
    }

    public SecurityPrincipal getSecurityPrincipal() {
        return securityPrincipal;
    }

    public void setSecurityPrincipal(SecurityPrincipal permissionRecipient) {
        this.securityPrincipal = permissionRecipient;
    }

    public String getSqlUpdate() {
        return null;
    }

    public String getCachePolicy() {
        return "nonstrict-read-write";
    }

    public boolean isLazy() {
        return false;
    }

    public boolean isFieldMapped(String fieldName) {
        return true;
    }

    public String getFieldType(String fieldName) {
        return null;
    }

    public int getFieldLength(String fieldName) {
        return -1;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((permissibleObject == null) ? 0 : permissibleObject.hashCode());
        result = prime * result + ((securityPrincipal == null) ? 0 : securityPrincipal.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Permission other = (Permission) obj;
        if (permissibleObject == null) {
            if (other.permissibleObject != null) return false;
        } else if (!permissibleObject.equals(other.permissibleObject)) return false;
        if (securityPrincipal == null) {
            if (other.securityPrincipal != null) return false;
        } else if (!securityPrincipal.equals(other.securityPrincipal)) return false;
        return true;
    }
}
