package jcontrol.eib.extended.ai_layer;

import java.io.Serializable;

/**
 * A simple container to hold a property description like defined in the EIB
 * specification.
 *
 * @author Bjoern Hornburg
 * @version 0.2.0.000
 * @since 0.2.0
 */
public class PropertyDescr implements Serializable {

    /**
     * Physical address of the EIB device containing the associated property.
     */
    public int physAddr;

    /**
     * Index of the EIB object containing the associated property.
     */
    public int objectIdx;

    /**
     * Identification of the associated property.
     */
    public int propertyID;

    /**
     * Index of the associated property.
     */
    public int propertyIdx;

    /**
     * Type of the associated property.
     */
    public int type;

    /**
     * Maximum number of elements the associated property may hold.
     */
    public int maxNoElems;

    /**
     * Required access level to read the associated property.
     */
    public int readLevel;

    /**
     * Required access level to write the associated property.
     */
    public int writeLevel;

    public PropertyDescr(int da, int objIdx, int propID, int propIdx) {
        physAddr = da;
        objectIdx = objIdx;
        propertyID = propID;
        propertyIdx = propIdx;
        type = -1;
        maxNoElems = -1;
        readLevel = -1;
        writeLevel = -1;
    }

    public PropertyDescr(int da, int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel) {
        physAddr = da;
        objectIdx = objIdx;
        propertyID = propID;
        propertyIdx = propIdx;
        this.type = type;
        this.maxNoElems = maxNoElems;
        this.readLevel = readLevel;
        this.writeLevel = writeLevel;
    }

    public boolean equals(Object o) {
        if (o instanceof PropertyDescr) {
            PropertyDescr p = (PropertyDescr) o;
            return ((p.physAddr == physAddr) && (p.objectIdx == objectIdx) && ((p.propertyID == propertyID) || (propertyID == 0)) && ((p.propertyIdx == propertyIdx) || (propertyIdx == 0)));
        } else {
            return false;
        }
    }
}
