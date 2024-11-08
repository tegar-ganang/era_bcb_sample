package org.jazzteam.SummArrayLessNumber;

public class Array {

    private int[] array;

    private int number;

    public Array(int array[], int m) {
        this.number = m;
        this.array = array;
    }

    public void sortArray() {
        int a;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length - 1; j++) {
                if (array[j] < array[j + 1]) {
                    a = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = a;
                }
            }
        }
    }

    public String show() {
        String show = new String();
        for (int i = 0; i < array.length; i++) {
            show += array[i];
        }
        return show;
    }

    public int searchSummArrayLessNumber() {
        int summ = 0;
        int ymnog1 = 1;
        int ymnog2 = 1;
        for (int i = 0; i < array.length; i++) {
            ymnog2 = ymnog1;
            ymnog1 *= array[i];
            if (ymnog1 < number) {
                summ += array[i];
            } else ymnog1 = ymnog2;
        }
        return summ;
    }
}
