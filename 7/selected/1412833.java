package jitt64;

/**
 * A track representation
 * A track is a list of patterns to play in sequence.
 * 
 * @author ice
 */
public class Track {

    /** max track dimension */
    public static final int DIMENSION = 256;

    /** End mark for restart pattern position */
    public static final int PATTERN_RESTART = 255;

    /** Position for not repeat the tune */
    public static final int PATTERN_RST_END = 255;

    /** End mark for repeting a pattern */
    public static final int PATTERN_REP = 223;

    /** List of patten commands */
    protected int[] list;

    /** size of the list */
    protected int size;

    /**
   * Construct an empty track with initial pattern 0 and no repeat command
   */
    public Track() {
        clear();
    }

    /**
   * Construct a special empty track with initial given pattern,
   * follow by pattern 0 and repeat command to pattern 0
   *
   * @param pattern the pattern to insert
   */
    public Track(int pattern) {
        list = new int[DIMENSION];
        list[0] = pattern;
        list[1] = 0;
        list[2] = PATTERN_RESTART;
        list[3] = 1;
        size = 4;
    }

    /**
   * Get the string representation of value at given index
   * 
   * @param index the index of the element
   * @return the string representation
   */
    public String getStringValueAt(int index) {
        int value = list[index];
        if (index >= size) return "";
        if (index >= 1 && list[index - 1] == PATTERN_REP) return (value >> 4) + "|" + (value & 0x0f);
        if (value == PATTERN_REP) return "REP";
        if (index >= 1 && list[index - 1] == PATTERN_RESTART) {
            if (!(index >= 2 && list[index - 2] == PATTERN_REP)) {
                if (value == PATTERN_RST_END) return "NO"; else return "P" + list[index];
            }
        }
        if (value == PATTERN_RESTART) return "RST";
        if (value >= 0xE0 && value <= 0xFE) {
            switch(list[index]) {
                case 0xE0:
                    return "TR-15";
                case 0xE1:
                    return "TR-14";
                case 0xE2:
                    return "TR-13";
                case 0xE3:
                    return "TR-12";
                case 0xE4:
                    return "TR-11";
                case 0xE5:
                    return "TR-10";
                case 0xE6:
                    return "TR-9";
                case 0xE7:
                    return "TR-8";
                case 0xE8:
                    return "TR-7";
                case 0xE9:
                    return "TR-6";
                case 0xEA:
                    return "TR-5";
                case 0xEB:
                    return "TR-4";
                case 0xEC:
                    return "TR-3";
                case 0xED:
                    return "TR-2";
                case 0xEE:
                    return "TR-1";
                case 0xEF:
                    return "TR+0";
                case 0xF0:
                    return "TR+1";
                case 0xF1:
                    return "TR+2";
                case 0xF2:
                    return "TR+3";
                case 0xF3:
                    return "TR+4";
                case 0xF4:
                    return "TR+5";
                case 0xF5:
                    return "TR+6";
                case 0xF6:
                    return "TR+7";
                case 0xF7:
                    return "TR+8";
                case 0xF8:
                    return "TR+9";
                case 0xF9:
                    return "TR+10";
                case 0xFA:
                    return "TR+11";
                case 0xFB:
                    return "TR+12";
                case 0xFC:
                    return "TR+13";
                case 0xFD:
                    return "TR+14";
                case 0xFE:
                    return "TR+15";
            }
        }
        return "" + value;
    }

    /**
   * Get the value at the given index
   * 
   * @param index the index to use
   * @return the value in list
   */
    public int getValueAt(int index) {
        return list[index];
    }

    /**
   * Set the value at the given index
   * 
   * @param index the index of element
   * @param value the value to put
   */
    public void setValueAt(int index, int value) {
        list[index] = value;
    }

    /**
   * Get actual size of elements in list
   * 
   * @return the actual size
   */
    public int getSize() {
        return size;
    }

    /**
   * Set actual size of elements in list
   * 
   * @param size the actual size
   */
    public void setSize(int size) {
        this.size = size;
    }

    /**
   * Insert the given value at given position, shifting all the others
   * 
   * @param index the index for element
   * @param value the value to insert
   * @return false if the is no more space
   */
    public boolean insertValueAt(int index, int value) {
        if (size >= list.length) return false;
        for (int i = list.length - 2; i >= index; i--) {
            list[i + 1] = list[i];
        }
        list[index] = value;
        size++;
        return true;
    }

    /**
   * Remove the value at the given index, shifting all the others
   * 
   * @param index the index for element to remove
   * @return false if element cannot be removed
   */
    public boolean removeValueAt(int index) {
        if (index >= size) return false;
        for (int i = index; i < size; i++) {
            list[i] = list[i + 1];
        }
        size--;
        return true;
    }

    /**
   * Set the repeat position
   * 
   * @param value the repeat position
   */
    public void setRepeatPos(int value) {
        list[size - 1] = value;
    }

    /**
   * Clear all the track
   */
    public void clear() {
        list = new int[DIMENSION];
        list[0] = 0;
        list[1] = PATTERN_RESTART;
        list[2] = PATTERN_RST_END;
        size = 3;
    }
}
