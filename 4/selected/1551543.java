package org.damour.base.client.objects;

import java.io.Serializable;

public class TagMembership implements Serializable, IHibernateFriendly {

    public Long id;

    public Tag tag;

    public PermissibleObject permissibleObject;

    public TagMembership() {
    }

    public String getCachePolicy() {
        return "nonstrict-read-write";
    }

    public boolean isLazy() {
        return false;
    }

    public boolean isFieldUnique(String fieldName) {
        return false;
    }

    public boolean isFieldKey(String fieldName) {
        return false;
    }

    public String getSqlUpdate() {
        return null;
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

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public PermissibleObject getPermissibleObject() {
        return permissibleObject;
    }

    public void setPermissibleObject(PermissibleObject permissibleObject) {
        this.permissibleObject = permissibleObject;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
