package name.huzhenbo.java.patterns.strategy;

class DoubleSorterHandler implements SorterHandler {

    private double[] doubleArray;

    public boolean outOfOrder(int i) {
        return doubleArray[i] > doubleArray[i + 1];
    }

    public void swap(int i) {
        double temp = doubleArray[i];
        doubleArray[i] = doubleArray[i + 1];
        doubleArray[i + 1] = temp;
    }

    public void setArray(Object array) {
        doubleArray = (double[]) array;
    }

    public int length() {
        return doubleArray.length;
    }
}
