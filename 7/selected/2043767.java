package data;

import error.*;

/**
 * A simple Array class that holds long values. Dynamically grows bigger
 * as more space is needed. 
 * 
 * TODO: Maybe make it shrink when we don't need as much space anymore?
 * 
 * @author jchoy
 *
 */
public class Array {

    private int numElements = 0;

    private static final int DEFAULT_ARRAY_SIZE = 1000;

    private int maxArraySize = DEFAULT_ARRAY_SIZE;

    protected long[] array = new long[maxArraySize];

    /**
     * Constructor
     */
    public Array() {
    }

    public static void main(String args[]) {
        try {
            Array arr = new Array();
            arr.addValue(00);
            arr.addValue(99);
            arr.addValue(33);
            arr.addValue(55);
            arr.addValue(74);
            arr.addValue(12);
            arr.addValue(78);
            arr.addValue(39);
            arr.addValue(81);
            arr.addValue(22);
            arr.addValue(43);
            System.out.println(arr.getString());
            int index = -1;
            try {
                index = arr.findValue(22);
            } catch (InvalidValueException e) {
                throw new ProgrammingErrorException("Arrray did not find" + " a value I inserted");
            }
            System.out.println("Found 22 at index " + Integer.toString(index) + ".");
            try {
                index = arr.findValue(11);
            } catch (InvalidValueException e) {
                System.out.println("Could not find 11 in array.");
            }
            try {
                System.out.println("Deleting 81 & 39.");
                arr.deleteValue(81);
                arr.deleteValue(39);
            } catch (InvalidValueException e) {
                throw new ProgrammingErrorException("Arrray did not find" + " a value I inserted");
            }
            System.out.println(arr.getString());
        } catch (ProgrammingErrorException e) {
            System.out.println(e.getMessage());
        } catch (OutOfMemoryException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @return The number of elements in the array
     */
    public int getNumElements() {
        return numElements;
    }

    /**
     * Increases the array size
     * 
     * @param newSize The new size we should increase to
     * @throws ProgrammingErrorException if newSize is <= currentSize
     */
    private void increaseArraySize(int newSize) throws ProgrammingErrorException {
        if (newSize <= maxArraySize) {
            throw new ProgrammingErrorException("Array.increaseArraySize " + "got an invalid newSize");
        }
        long[] newArray = new long[newSize];
        for (int i = 0; i < numElements; i++) {
            newArray[i] = array[i];
        }
        array = newArray;
        maxArraySize = newSize;
    }

    /**
     * Finds if the array includes searchValue
     * 
     * @param searchValue The value we are trying to find
     * @throws InvalidValueException if value not found
     */
    public int findValue(long searchValue) throws InvalidValueException {
        boolean isFound = false;
        int index = 0;
        for (index = 0; index < numElements; index++) {
            if (array[index] == searchValue) {
                isFound = true;
                break;
            }
        }
        if (isFound == false) {
            throw new InvalidValueException(Severity.NON_FATAL);
        }
        return index;
    }

    /**
     * Adds a value into the array
     * 
     * @param value The value to add
     * @throws ProgrammingErrorException
     * @throws OutOfMemoryException 
     */
    public void addValue(long value) throws ProgrammingErrorException, OutOfMemoryException {
        if (maxArraySize == Integer.MAX_VALUE) {
            throw new OutOfMemoryException("Array index = MAX_INT!");
        }
        if (numElements >= maxArraySize) {
            int newArraySize = maxArraySize + DEFAULT_ARRAY_SIZE;
            if ((Integer.MAX_VALUE - maxArraySize) < DEFAULT_ARRAY_SIZE) {
                newArraySize = Integer.MAX_VALUE;
            }
            increaseArraySize(newArraySize);
        }
        array[numElements++] = value;
    }

    /**
     * Deletes a value from the array
     * 
     * @param value The value to find a delete
     * @throws InvalidValueException If the value does not exist
     */
    public void deleteValue(long value) throws InvalidValueException {
        int index = findValue(value);
        for (int i = index; i < numElements - 1; i++) {
            array[i] = array[i + 1];
        }
        numElements--;
    }

    /**
     * Swaps the two values for the two indexes
     * 
     * @param a The index of the first value to swap
     * @param b The index of the seconds value to swap
     * @throws ProgrammingErrorException
     */
    protected void swapValue(int a, int b) throws ProgrammingErrorException {
        if (a > (numElements - 1) || b > (numElements - 1)) {
            throw new ProgrammingErrorException("Array.swapValue got Invalid" + "indexes");
        }
        long temp = array[a];
        array[a] = array[b];
        array[b] = temp;
    }

    /**
     * @return The array as a string
     */
    public String getString() {
        String retVal = new String();
        for (int i = 0; i < numElements; i++) {
            retVal += Long.toString(array[i]);
            retVal += " ";
        }
        return retVal;
    }

    /**
     * Fills the array with pseudorandom numbers
     * 
     * @param numValuesToFill Number of values to fill
     * @throws ProgrammingErrorException
     * @throws OutOfMemoryException 
     */
    public void fillWithRandomNumbers(int numValuesToFill) throws ProgrammingErrorException, OutOfMemoryException {
        for (int i = 0; i < numValuesToFill; i++) {
            long num = (long) (Math.random() * 99.0);
            this.addValue(num);
        }
    }

    /**
     * Cycles through the array and checks to see if the array is ordered
     * from smallest to biggest from smallestIndex to biggestIndex
     * @return
     */
    public boolean isOrdered() {
        boolean isOrdered = true;
        for (int i = 0; i < numElements - 1; i++) {
            if (array[i] > array[i + 1]) {
                isOrdered = false;
                break;
            }
        }
        return isOrdered;
    }
}
