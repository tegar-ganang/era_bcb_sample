package civquest.gameChange;

import civquest.io.Messages;
import civquest.nation.RestrictedToNation;

/**
 *
 */
public abstract class AbstractGameChange implements GameChange {

    private RestrictedToNation resToNation = null;

    private Long[] affectedObjects = new Long[0];

    public AbstractGameChange() {
        resToNation = new RestrictedToNation();
    }

    public AbstractGameChange(Long... ids) {
        resToNation = new RestrictedToNation();
        affectedObjects = ids;
    }

    public AbstractGameChange(Long[]... ids) {
        resToNation = new RestrictedToNation();
        int sumLength = 0;
        for (Long[] currArray : ids) {
            sumLength += currArray.length;
        }
        affectedObjects = new Long[sumLength];
        int currIndex = 0;
        for (Long[] currArray : ids) {
            for (Long currID : currArray) {
                affectedObjects[currIndex] = currID;
                currIndex++;
            }
        }
    }

    public AbstractGameChange(RestrictedToNation resToNation) {
        this.resToNation = resToNation;
    }

    public AbstractGameChange(RestrictedToNation resToNation, Long... ids) {
        this.resToNation = resToNation;
        affectedObjects = ids;
    }

    public void execute() throws Exception {
        System.out.println("Executing " + this.getClass().toString());
    }

    public void notifyBefore() {
        GameChangeManager.getGameChangeManager().notifyBefore(this);
    }

    public void notifyBefore(RestrictedToNation resToNation) {
        GameChangeManager.getGameChangeManager().notifyBefore(this, resToNation);
    }

    public void notifyAfter() {
        GameChangeManager.getGameChangeManager().notifyAfter(this);
    }

    public void notifyAfter(RestrictedToNation resToNation) {
        GameChangeManager.getGameChangeManager().notifyAfter(this, resToNation);
    }

    public void notify(boolean before, boolean after) {
        if (before) {
            notifyBefore();
        }
        if (after) {
            notifyAfter();
        }
    }

    public void notify(RestrictedToNation resToNation, boolean before, boolean after) {
        if (before) {
            notifyBefore(resToNation);
        }
        if (after) {
            notifyAfter(resToNation);
        }
    }

    public void setResToNation(RestrictedToNation resToNation) {
        this.resToNation = resToNation;
    }

    public RestrictedToNation getResToNation() {
        return resToNation;
    }

    public Long[] getAllAffectedObjects() {
        return affectedObjects;
    }

    protected void addToAffectedObjects(Long id) {
        affectedObjects = addToIDArray(affectedObjects, id);
    }

    /** 
	 */
    protected static Long[] addToIDArray(Long[] array, Long el) {
        Long[] retArray = new Long[array.length + 1];
        for (int n = 0; n < array.length; n++) {
            retArray[n] = array[n];
        }
        retArray[retArray.length - 1] = el;
        return retArray;
    }

    protected void removeFromAffectedObjects(Long id) {
        affectedObjects = removeFromIDArray(affectedObjects, id);
    }

    protected static Long[] removeFromIDArray(Long[] array, Long el) {
        int index = -1;
        for (int n = 0; n < array.length; n++) {
            if (array[n] == el) {
                index = n;
                break;
            }
        }
        assert index != -1;
        Long[] retArray = new Long[array.length - 1];
        for (int n = 0; n < retArray.length; n++) {
            if (n < index) {
                retArray[n] = array[n];
            } else {
                retArray[n] = array[n + 1];
            }
        }
        return retArray;
    }
}
