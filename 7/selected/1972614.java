package name.huzhenbo.java.patterns.templatemethod;

class DoubleBubbleSorter extends AbstractBubbleSorter {

    private double[] doubleArray;

    public double[] sort(double[] doubleArray) {
        this.length = doubleArray.length;
        this.doubleArray = doubleArray;
        doSort();
        return this.doubleArray;
    }

    @Override
    public boolean outOfOrder(int i) {
        return doubleArray[i] > doubleArray[i + 1];
    }

    @Override
    public void swap(int i) {
        double temp = doubleArray[i];
        doubleArray[i] = doubleArray[i + 1];
        doubleArray[i + 1] = temp;
    }
}
