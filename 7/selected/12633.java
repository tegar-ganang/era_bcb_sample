package net.sf.linuxorg.pcal.engine;

import java.beans.PropertyChangeListener;
import java.util.Vector;
import net.sf.linuxorg.pcal.messages.Messages;

/**
 * This is container for the sympthoms list with the possible values which
 * are monitored in addition to the BBT values.
 * This container works as an integrap part of the Engine. It automatically sets the Engine to the modified state if applicable setters are called.
 * It is a limit of having no more than 4 sympthoms.
 */
public class BBTSympthomsSetDefinition {

    private static final String[] DEFAULT_SYMPTHOMS_LIST = { Messages.getString("BBTSympthomsSetDefinition.0"), null, null, null };

    private static final String[][] DEFAULT_SYMPTHOMS_LIST_LEVELS = { { Messages.getString("BBTSympthomsSetDefinition.1"), Messages.getString("BBTSympthomsSetDefinition.2"), Messages.getString("BBTSympthomsSetDefinition.3"), Messages.getString("BBTSympthomsSetDefinition.4") } };

    public static final int BBT_SYMPTHOMS_COUNT = 4;

    private String[] names = new String[BBT_SYMPTHOMS_COUNT];

    private Vector<Vector<String>> values = new Vector<Vector<String>>();

    private Vector<PropertyChangeListener> modifiedListenersList = new Vector<PropertyChangeListener>();

    /**
	 * @return the ordered list of the sympthoms being monitored. The array may contain null which
	 * means the sympthom is not used.
	 */
    public String[] getSympthoms() {
        return names;
    }

    /**
	 * @param sympthomIndex a sympthom index. If out of range an empty string is returned.
	 * @return a sympthom name
	 */
    public String getSympthomName(int sympthomIndex) {
        if ((sympthomIndex >= 0) && (sympthomIndex < BBT_SYMPTHOMS_COUNT)) {
            return names[sympthomIndex];
        } else {
            return "";
        }
    }

    /**
	 * Changes the sympthom name
	 * @param index the sympthom index
	 * @param newName the new name to be assigned
	 */
    public void changeSympthomName(int index, String newName) {
        names[index] = newName;
        fireModifiedListeners();
    }

    /**
	 * Getter for the sympthom values list
	 * @param sympthomIndex the sympthom number to be queried
	 * @return ordered sympthom values list as an array of strings
	 */
    public String[] getSympthomValues(int sympthomIndex) {
        try {
            Vector<String> sympthomValues = values.get(sympthomIndex);
            if (sympthomValues == null) {
                return null;
            } else {
                String[] result = new String[sympthomValues.size()];
                sympthomValues.copyInto(result);
                return result;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
	 * Getter for the sympthom values list in a form of vector
	 * @param sympthomIndex the sympthom number to be queried
	 * @return ordered sympthom values list as an array of strings
	 */
    public Vector<String> getSympthomValuesVector(int sympthomIndex) {
        return values.get(sympthomIndex);
    }

    /**
	 * Getter for a particular sympthom value
	 * @param sympthomIndex sympthom #
	 * @param valueIndex value #
	 * @return the sympthom value or "" if something wrong
	 */
    public String getSympthomValue(int sympthomIndex, int valueIndex) {
        String emptyString = "";
        if ((sympthomIndex < 0) || sympthomIndex >= values.size()) return emptyString;
        Vector<String> sympthomValues = values.get(sympthomIndex);
        if (sympthomValues == null) return emptyString;
        if ((valueIndex < 0) || valueIndex >= sympthomValues.size()) return emptyString;
        String result = sympthomValues.get(valueIndex);
        if (result == null) {
            return emptyString;
        } else {
            return result;
        }
    }

    /**
	 * @param sympthomIndex the sympthom # to be queried
	 * @return the number of the sympthom values. 0 if sympthom does not exists
	 */
    public int getSympthomValuesCount(int sympthomIndex) {
        Vector<String> valuesList = values.get(sympthomIndex);
        if (valuesList == null) {
            return 0;
        } else {
            return valuesList.size();
        }
    }

    /**
	 * Clears the internal data
	 */
    public void clear() {
        values.clear();
        for (int i = 0; i < names.length; i++) {
            names[i] = null;
            values.add(null);
        }
    }

    /**
	 * Sets the default values to the sympthoms list
	 */
    public void setDefaults() {
        values.clear();
        for (int i = 0; i < DEFAULT_SYMPTHOMS_LIST.length; i++) {
            names[i] = DEFAULT_SYMPTHOMS_LIST[i];
            Vector<String> sympthomValues = new Vector<String>();
            if (i < DEFAULT_SYMPTHOMS_LIST_LEVELS.length) {
                for (int j = 0; j < DEFAULT_SYMPTHOMS_LIST_LEVELS[i].length; j++) {
                    sympthomValues.add(DEFAULT_SYMPTHOMS_LIST_LEVELS[i][j]);
                }
            }
            values.add(sympthomValues);
        }
    }

    /**
	 * Set the sympthom name and values
	 * @param position - the position of the sympthom to be set
	 * @param sympthomName - sympthom name
	 * @param sympthomValues - list of the sympthom values
	 */
    public void setSypmthomNameAndValues(int position, String sympthomName, Vector<String> sympthomValues) {
        names[position] = sympthomName;
        values.set(position, sympthomValues);
        fireModifiedListeners();
    }

    /**
	 * Inserts a new value into the sympthom's values list
	 * @param sympthomIndex the index of the sympthom to be updated. If beyond the sympthoms list - this method will do nothing
	 * @param valueIndex the desired position for the new value. If beyond the current list or -1, the value will be appended
	 * @param value the string value to be inserted
	 * @return the actual placement location or -1 on error
	 */
    public int insertSypmthomValue(int sympthomIndex, int valueIndex, String value) {
        if ((sympthomIndex < 0) || (sympthomIndex >= names.length)) return -1;
        Vector<String> sympthomValues = values.get(sympthomIndex);
        if ((valueIndex < 0) || (valueIndex > sympthomValues.size())) {
            valueIndex = sympthomValues.size();
        }
        sympthomValues.insertElementAt(value, valueIndex);
        fireModifiedListeners();
        return valueIndex;
    }

    /**
	 * Change the sympthom value
	 * @param sympthomIndex - if beyond the sympthoms list - this method will do nothing
	 * @param valueIndex - if beyond the sympthoms list a new item will be added to the end 
	 * of the list
	 * @param sympthomValue - the value to be set. If the value is empty string - the sympthom value will be removed
	 */
    public void setSypmthomValue(int sympthomIndex, int valueIndex, String sympthomValue) {
        if ((sympthomIndex < 0) || (sympthomIndex >= names.length)) return;
        if ((valueIndex >= 0) && (valueIndex < values.get(sympthomIndex).size())) {
            values.get(sympthomIndex).set(valueIndex, sympthomValue);
        } else {
            values.get(sympthomIndex).add(sympthomValue);
        }
        fireModifiedListeners();
    }

    /**
	 * Determines the value index in the specified sympthom values list
	 * @param sympthomIndex the index of the sympthom to be queried
	 * @param sympthomValue the sympthom value to be looked for
	 * @return the value index or -1 if the value not found
	 */
    public int getSympthomValueIndex(int sympthomIndex, String sympthomValue) {
        Vector<String> sympthomValues = values.get(sympthomIndex);
        return sympthomValues.indexOf(sympthomValue);
    }

    /**
	 * Delete the sympthom specified. All further sypmthoms are moved upwards. The last one becomes empty.
	 * @param sympthomIndex
	 */
    public void deleteSympthom(int sympthomIndex) {
        for (int i = sympthomIndex; i < names.length - 1; i++) {
            names[i] = names[i + 1];
        }
        names[names.length - 1] = "";
        values.remove(sympthomIndex);
        values.add(new Vector<String>());
        fireModifiedListeners();
    }

    /**
	 * Adds a listener which is called once anything is changed (except setDefaults() and clear()) 
	 * @param listener
	 */
    public void addModifiedListener(PropertyChangeListener listener) {
        modifiedListenersList.add(listener);
    }

    private void fireModifiedListeners() {
        for (PropertyChangeListener listener : modifiedListenersList) {
            listener.propertyChange(null);
        }
    }
}
