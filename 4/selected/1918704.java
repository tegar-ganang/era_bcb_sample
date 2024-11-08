package org.damour.base.client.objects;

import java.io.Serializable;

public class UserThumb implements Serializable, IHibernateFriendly {

    public Long id;

    public User voter;

    public PermissibleObject permissibleObject;

    public String voterGUID;

    public boolean likeThumb = true;

    public long ratingDate = System.currentTimeMillis();

    public UserThumb() {
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

    public String getCachePolicy() {
        return "nonstrict-read-write";
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

    public User getVoter() {
        return voter;
    }

    public void setVoter(User voter) {
        this.voter = voter;
    }

    public PermissibleObject getPermissibleObject() {
        return permissibleObject;
    }

    public void setPermissibleObject(PermissibleObject permissibleObject) {
        this.permissibleObject = permissibleObject;
    }

    public long getRatingDate() {
        return ratingDate;
    }

    public void setRatingDate(long ratingDate) {
        this.ratingDate = ratingDate;
    }

    public boolean isLikeThumb() {
        return likeThumb;
    }

    public void setLikeThumb(boolean likeThumb) {
        this.likeThumb = likeThumb;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        UserThumb other = (UserThumb) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    /**
   * @return the voterGUID
   */
    public String getVoterGUID() {
        return voterGUID;
    }

    /**
   * @param voterGUID
   *          the voterGUID to set
   */
    public void setVoterGUID(String voterGUID) {
        this.voterGUID = voterGUID;
    }

    /**
   * @return the id
   */
    public Long getId() {
        return id;
    }

    /**
   * @param id
   *          the id to set
   */
    public void setId(Long id) {
        this.id = id;
    }
}
