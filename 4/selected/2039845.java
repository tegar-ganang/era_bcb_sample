package org.damour.base.client.objects;

import java.io.Serializable;

@SuppressWarnings({ "serial", "rawtypes" })
public class PermissibleObject implements Serializable, IHibernateFriendly, Comparable {

    public Long id;

    public User owner;

    public String name;

    public String description;

    public String keywords;

    public PermissibleObject parent;

    public Long creationDate = System.currentTimeMillis();

    public Long lastModifiedDate = System.currentTimeMillis();

    public float averageRating = 0f;

    public long numRatingVotes = 0;

    public float averageAdvisory = 0f;

    public long numAdvisoryVotes = 0;

    public long numUpVotes = 0;

    public long numDownVotes = 0;

    public long numComments = 0;

    public boolean allowComments = true;

    public boolean moderateComments = false;

    public Long customCounter1 = new Long(0);

    public Long customCounter2 = new Long(0);

    public Long customCounter3 = new Long(0);

    public boolean hidden = false;

    public boolean globalRead = true;

    public boolean globalWrite = false;

    public boolean globalExecute = false;

    public Boolean globalCreateChild = false;

    public PermissibleObject() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    public boolean isGlobalRead() {
        return globalRead;
    }

    public void setGlobalRead(boolean globalRead) {
        this.globalRead = globalRead;
    }

    public boolean isGlobalWrite() {
        return globalWrite;
    }

    public void setGlobalWrite(boolean globalWrite) {
        this.globalWrite = globalWrite;
    }

    public boolean isGlobalExecute() {
        return globalExecute;
    }

    public void setGlobalExecute(boolean globalExecute) {
        this.globalExecute = globalExecute;
    }

    public boolean isGlobalCreateChild() {
        if (globalCreateChild == null) {
            globalCreateChild = false;
        }
        return globalCreateChild;
    }

    public void setGlobalCreateChild(Boolean globalCreateChild) {
        if (globalCreateChild == null) {
            globalCreateChild = false;
        }
        this.globalCreateChild = globalCreateChild;
    }

    public String getCachePolicy() {
        return "nonstrict-read-write";
    }

    public boolean isLazy() {
        return false;
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
        PermissibleObject other = (PermissibleObject) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public PermissibleObject getParent() {
        return parent;
    }

    public void setParent(PermissibleObject parent) {
        this.parent = parent;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public Long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
   * @return the averageRating
   */
    public float getAverageRating() {
        return averageRating;
    }

    /**
   * @param averageRating
   *          the averageRating to set
   */
    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    /**
   * @return the numRatingVotes
   */
    public long getNumRatingVotes() {
        return numRatingVotes;
    }

    /**
   * @param numRatingVotes
   *          the numRatingVotes to set
   */
    public void setNumRatingVotes(long numRatingVotes) {
        this.numRatingVotes = numRatingVotes;
    }

    /**
   * @return the averageAdvisory
   */
    public float getAverageAdvisory() {
        return averageAdvisory;
    }

    /**
   * @param averageAdvisory
   *          the averageAdvisory to set
   */
    public void setAverageAdvisory(float averageAdvisory) {
        this.averageAdvisory = averageAdvisory;
    }

    /**
   * @return the numAdvisoryVotes
   */
    public long getNumAdvisoryVotes() {
        return numAdvisoryVotes;
    }

    /**
   * @param numAdvisoryVotes
   *          the numAdvisoryVotes to set
   */
    public void setNumAdvisoryVotes(long numAdvisoryVotes) {
        this.numAdvisoryVotes = numAdvisoryVotes;
    }

    public long getNumUpVotes() {
        return numUpVotes;
    }

    public void setNumUpVotes(long numUpVotes) {
        this.numUpVotes = numUpVotes;
    }

    public long getNumDownVotes() {
        return numDownVotes;
    }

    public void setNumDownVotes(long numDownVotes) {
        this.numDownVotes = numDownVotes;
    }

    public long getNumComments() {
        return numComments;
    }

    public void setNumComments(long numComments) {
        this.numComments = numComments;
    }

    /**
   * @return the allowComments
   */
    public boolean isAllowComments() {
        return allowComments;
    }

    /**
   * @param allowComments
   *          the allowComments to set
   */
    public void setAllowComments(boolean allowComments) {
        this.allowComments = allowComments;
    }

    /**
   * @return the moderateComments
   */
    public boolean isModerateComments() {
        return moderateComments;
    }

    /**
   * @param moderateComments
   *          the moderateComments to set
   */
    public void setModerateComments(boolean moderateComments) {
        this.moderateComments = moderateComments;
    }

    public Long getCustomCounter1() {
        if (customCounter1 == null) {
            customCounter1 = new Long(0);
        }
        return customCounter1;
    }

    public void setCustomCounter1(Long customCounter1) {
        this.customCounter1 = customCounter1;
    }

    public Long getCustomCounter2() {
        if (customCounter2 == null) {
            customCounter2 = new Long(0);
        }
        return customCounter2;
    }

    public void setCustomCounter2(Long customCounter2) {
        this.customCounter2 = customCounter2;
    }

    public Long getCustomCounter3() {
        if (customCounter3 == null) {
            customCounter3 = new Long(0);
        }
        return customCounter3;
    }

    public void setCustomCounter3(Long customCounter3) {
        this.customCounter3 = customCounter3;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isFieldMapped(String fieldName) {
        return true;
    }

    public String getFieldType(String fieldName) {
        return null;
    }

    public int compareTo(Object o) {
        if (o instanceof PermissibleObject == false) {
            return 0;
        }
        PermissibleObject other = (PermissibleObject) o;
        return this.id.compareTo(other.getId());
    }

    public void mergeInto(PermissibleObject target) {
        PermissibleObject source = this;
        target.id = source.id;
        target.owner = source.owner;
        target.name = source.name;
        target.description = source.description;
        target.keywords = source.keywords;
        target.parent = source.parent;
        target.creationDate = source.creationDate;
        target.lastModifiedDate = source.lastModifiedDate;
        target.averageRating = source.averageRating;
        target.numRatingVotes = source.numRatingVotes;
        target.averageAdvisory = source.averageAdvisory;
        target.numAdvisoryVotes = source.numAdvisoryVotes;
        target.numUpVotes = source.numUpVotes;
        target.numDownVotes = source.numDownVotes;
        target.numComments = source.numComments;
        target.allowComments = source.allowComments;
        target.moderateComments = source.moderateComments;
        target.customCounter1 = source.customCounter1;
        target.customCounter2 = source.customCounter2;
        target.customCounter3 = source.customCounter3;
        target.hidden = source.hidden;
        target.globalRead = source.globalRead;
        target.globalWrite = source.globalWrite;
        target.globalExecute = source.globalExecute;
    }

    public String toString() {
        String toString = "id:" + getId() + " parentId:" + (getParent() != null ? getParent().getId() : "-") + " class:" + getClass().getName().substring(getClass().getName().lastIndexOf(".") + 1) + " name:" + getName() + " owner:" + getOwner().getUsername();
        return toString;
    }

    public int getFieldLength(String fieldName) {
        return -1;
    }
}
