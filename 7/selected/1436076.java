package name.huzhenbo.java.patterns.templatemethod;

class IntBubbleSorter extends AbstractBubbleSorter {

    private int[] intArray;

    public int[] sort(int[] intArray) {
        this.length = intArray.length;
        this.intArray = intArray;
        doSort();
        return this.intArray;
    }

    @Override
    public boolean outOfOrder(int i) {
        return intArray[i] > intArray[i + 1];
    }

    @Override
    public void swap(int i) {
        int temp = intArray[i];
        intArray[i] = intArray[i + 1];
        intArray[i + 1] = temp;
    }
}
