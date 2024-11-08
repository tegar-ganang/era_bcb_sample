class MyCollection {

    int[] values;

    int[] getValues() {
        return values;
    }

    int[] sort() {
        if (values.length > 100) {
            return quickSort();
        } else {
            return slowSort();
        }
    }

    int[] quickSort() {
        quicksort(values, 0, values.length - 1);
        return values;
    }

    void quicksort(int[] A, int left, int right) {
        if (right > left) {
            int pivotIndex = left;
            int pivotNewIndex = partition(A, left, right, pivotIndex);
            quicksort(A, left, pivotNewIndex - 1);
            quicksort(A, pivotNewIndex + 1, right);
        }
    }

    int partition(int[] A, int left, int right, int pivotIndex) {
        int pivotValue = A[pivotIndex];
        swap(A, pivotIndex, right);
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (A[i] < pivotValue) {
                swap(A, storeIndex, i);
                storeIndex += 1;
            }
        }
        swap(A, right, storeIndex);
        return storeIndex;
    }

    void swap(int[] A, int x, int y) {
        int tmp = A[x];
        A[x] = A[y];
        A[y] = tmp;
    }

    int[] slowSort() {
        int[] values = getValues();
        int n = values.length;
        for (int pass = 1; pass < n; pass++) {
            for (int i = 0; i < n - pass; i++) {
                if (values[i] > values[i + 1]) {
                    int temp = values[i];
                    values[i] = values[i + 1];
                    values[i + 1] = temp;
                }
            }
        }
        return values;
    }

    int[] reverse() {
        int[] values = getValues();
        int tmp;
        for (int i = 0; i < values.length / 2; i++) {
            tmp = values[i];
            values[i] = values[values.length - i - 1];
            values[values.length - i - 1] = tmp;
        }
        return values;
    }

    int max() {
        int[] sorted_values = sort();
        return sorted_values[sorted_values.length - 1];
    }
}
