package org.opencube.oms;

import java.util.Date;

/**
 * omsCube database rigts representation
 * Instances of this class should be composed by OMSElements the rights are set for
 * @author <a href="mailto:robert@fingo.pl">FINGO - Robert Marek</a>
 */
public class OMSRights extends OMSNode {

    private OMSElement grantedFor = null;

    private String grantedToId;

    private boolean read = false;

    private boolean write = false;

    private boolean compose = false;

    private boolean associate = false;

    private boolean delete = false;

    private boolean root = false;

    /**
	 * OMSRights constructor
	 * @param grantedFor 
	 */
    public OMSRights(OMSElement grantedFor, String grantedToId) {
        super(OMSStructure.generateUniqueString(), null, null, null, null, null);
        this.grantedToId = grantedToId;
        this.grantedFor = grantedFor;
    }

    /**
	 * OMSRights constructor
	 * @param grantedTo OMSElement - the having the rights
	 * @param read boolean - true if read rihgts are granted
	 * @param write boolean  - true if write rihgts are granted
	 * @param compose boolean - true if compose rihgts are granted
	 * @param associate boolean - true if associate rihgts are granted
	 * @param delete boolean - true if delete rihgts are granted
	 */
    public OMSRights(OMSElement grantedFor, String grantedToId, boolean read, boolean write, boolean compose, boolean associate, boolean delete) {
        super(OMSStructure.generateUniqueString(), null, null, null, null, null);
        this.grantedToId = grantedToId;
        this.read = read;
        this.write = write;
        this.compose = compose;
        this.associate = associate;
        this.delete = delete;
        this.grantedFor = grantedFor;
    }

    /**
	 * OMSRights constructor
	 * @param grantedTo OMSElement - the having the rights
	 * @param read boolean - true if read rihgts are granted
	 * @param write boolean  - true if write rihgts are granted
	 * @param compose boolean - true if compose rihgts are granted
	 * @param associate boolean - true if associate rihgts are granted
	 * @param delete boolean - true if delete rihgts are granted
	 * @param id - the id of the rights (the id of root for inherited rights) 
	 * @param creatingDate - creation date
	 * @param creater - rights creater id
	 * @param modifyingDate - modification date
	 * @param modifier - rights modifier id 
	 */
    public OMSRights(OMSElement grantedFor, String grantedToId, boolean read, boolean write, boolean compose, boolean associate, boolean delete, String id, Date creatingDate, String creater, Date modifyingDate, String modifier) {
        super(id, null, creatingDate, creater, modifyingDate, modifier);
        this.grantedFor = grantedFor;
        this.grantedToId = grantedToId;
        this.read = read;
        this.write = write;
        this.compose = compose;
        this.associate = associate;
        this.delete = delete;
    }

    /**
	 * @return OMSElement - returns the element the rights are granted for
	 */
    public OMSElement getGrantedFor() {
        return grantedFor;
    }

    /**
	 * @return OMSElement - returns the element having rights
	 */
    public OMSElement getGrantedTo() {
        return grantedFor.getOMSStructure().getElementById(this.grantedToId);
    }

    /**
	 * @return boolean - returns the read rights
	 */
    public boolean canRead() {
        return read;
    }

    /**
	 * @param read boolean - the read rights to set
	 */
    public void setRead(boolean read) {
        if (this.read != read) {
            update();
        }
        this.read = read;
    }

    /**
	 * @return boolean - returns the write rights
	 */
    public boolean canWrite() {
        return write;
    }

    /**
	 * @param write boolean - the write rights to set
	 */
    public void setWrite(boolean write) {
        if (this.write != write) {
            update();
        }
        this.write = write;
    }

    /**
	 * @return boolean - returns the compose.
	 */
    public boolean canCompose() {
        return compose;
    }

    /**
	 * @param compose boolean - the compose rights to set
	 */
    public void setCompose(boolean compose) {
        if (this.compose != compose) {
            update();
        }
        this.compose = compose;
    }

    /**
	 * @return boolean - returns the associate rights
	 */
    public boolean canAssociate() {
        return associate;
    }

    private void update() {
        if (!isRoot()) {
            if (!isNew()) {
                setId(OMSStructure.generateUniqueString());
                setState(OMSNode.STATE_NEW);
                setRoot(true);
            }
        } else if (getState() == OMSNode.STATE_NORMAL) {
            setState(OMSNode.STATE_UPDATED);
        }
    }

    /**
	 * @param associate boolean - the associate rights to set
	 */
    public void setAssociate(boolean associate) {
        if (this.associate != associate) {
            update();
        }
        this.associate = associate;
    }

    /**
	 * @return boolean - returns the delete rights
	 */
    public boolean canDelete() {
        return delete;
    }

    /**
	 * @param delete boolean - the delete rights to set
	 */
    public void setDelete(boolean delete) {
        if (this.delete != delete) {
            update();
        }
        this.delete = delete;
    }

    /**
	 * @return boolean - returns true for elements with rigts set directly, false for inherited 
	 */
    public boolean isRoot() {
        return root;
    }

    /**
	 * @param root boolean - set true for elements with rigts set directly, false for inherited
	 */
    void setRoot(boolean root) {
        this.root = root;
    }

    /**
	 * @return OMSRights - new intance with the same content
	 */
    public String getGrantedToId() {
        return this.grantedToId;
    }

    public OMSRights cloneRights() {
        return new OMSRights(this.grantedFor, this.grantedToId, this.read, this.write, this.compose, this.associate, this.delete);
    }
}
