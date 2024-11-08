import codesounding.*;

public class Sorter {

    private int partition(int arr[], int left, int right) {
        BasicProcessor.getInstance().getStartBlock();
        BasicProcessor.getInstance().getVarDeclaration();
        int i = left, j = right;
        BasicProcessor.getInstance().getVarDeclaration();
        int tmp;
        BasicProcessor.getInstance().getVarDeclaration();
        int pivot = arr[(left + right) / 2];
        {
            BasicProcessor.getInstance().getWhileStatement();
            while (i <= j) {
                BasicProcessor.getInstance().getStartBlock();
                {
                    BasicProcessor.getInstance().getWhileStatement();
                    while (arr[i] < pivot) i++;
                }
                {
                    BasicProcessor.getInstance().getWhileStatement();
                    while (arr[j] > pivot) j--;
                }
                {
                    BasicProcessor.getInstance().getIfStatement();
                    if (i <= j) {
                        BasicProcessor.getInstance().getStartBlock();
                        tmp = arr[i];
                        arr[i] = arr[j];
                        arr[j] = tmp;
                        i++;
                        j--;
                        BasicProcessor.getInstance().getEndBlock();
                    }
                }
                BasicProcessor.getInstance().getEndBlock();
            }
        }
        {
            BasicProcessor.getInstance().getReturnStatement();
            return i;
        }
    }

    private void quickSort(int arr[], int left, int right) {
        BasicProcessor.getInstance().getStartBlock();
        BasicProcessor.getInstance().getVarDeclaration();
        int index = partition(arr, left, right);
        {
            BasicProcessor.getInstance().getIfStatement();
            if (left < index - 1) quickSort(arr, left, index - 1);
        }
        {
            BasicProcessor.getInstance().getIfStatement();
            if (index < right) quickSort(arr, index, right);
        }
        BasicProcessor.getInstance().getEndBlock();
    }

    public void quickSort(int arr[]) {
        BasicProcessor.getInstance().getStartBlock();
        quickSort(arr, 0, arr.length - 1);
        BasicProcessor.getInstance().getEndBlock();
    }

    public void insertionSort(int[] arr) {
        BasicProcessor.getInstance().getStartBlock();
        BasicProcessor.getInstance().getVarDeclaration();
        int i, j, newValue;
        {
            BasicProcessor.getInstance().getForStatement();
            for (i = 1; i < arr.length; i++) {
                BasicProcessor.getInstance().getStartBlock();
                newValue = arr[i];
                j = i;
                {
                    BasicProcessor.getInstance().getWhileStatement();
                    while (j > 0 && arr[j - 1] > newValue) {
                        BasicProcessor.getInstance().getStartBlock();
                        arr[j] = arr[j - 1];
                        j--;
                        BasicProcessor.getInstance().getEndBlock();
                    }
                }
                arr[j] = newValue;
                BasicProcessor.getInstance().getEndBlock();
            }
        }
        BasicProcessor.getInstance().getEndBlock();
    }

    public void selectionSort(int[] arr) {
        BasicProcessor.getInstance().getStartBlock();
        BasicProcessor.getInstance().getVarDeclaration();
        int i, j, minIndex, tmp;
        BasicProcessor.getInstance().getVarDeclaration();
        int n = arr.length;
        {
            BasicProcessor.getInstance().getForStatement();
            for (i = 0; i < n - 1; i++) {
                BasicProcessor.getInstance().getStartBlock();
                minIndex = i;
                {
                    BasicProcessor.getInstance().getForStatement();
                    for (j = i + 1; j < n; j++) {
                        BasicProcessor.getInstance().getIfStatement();
                        if (arr[j] < arr[minIndex]) minIndex = j;
                    }
                }
                {
                    BasicProcessor.getInstance().getIfStatement();
                    if (minIndex != i) {
                        BasicProcessor.getInstance().getStartBlock();
                        tmp = arr[i];
                        arr[i] = arr[minIndex];
                        arr[minIndex] = tmp;
                        BasicProcessor.getInstance().getEndBlock();
                    }
                }
                BasicProcessor.getInstance().getEndBlock();
            }
        }
        BasicProcessor.getInstance().getEndBlock();
    }

    public void bubbleSort(int[] arr) {
        BasicProcessor.getInstance().getStartBlock();
        BasicProcessor.getInstance().getVarDeclaration();
        boolean swapped = true;
        BasicProcessor.getInstance().getVarDeclaration();
        int j = 0;
        BasicProcessor.getInstance().getVarDeclaration();
        int tmp;
        {
            BasicProcessor.getInstance().getWhileStatement();
            while (swapped) {
                BasicProcessor.getInstance().getStartBlock();
                swapped = false;
                j++;
                {
                    BasicProcessor.getInstance().getForStatement();
                    for (int i = 0; i < arr.length - j; i++) {
                        BasicProcessor.getInstance().getStartBlock();
                        {
                            BasicProcessor.getInstance().getIfStatement();
                            if (arr[i] > arr[i + 1]) {
                                BasicProcessor.getInstance().getStartBlock();
                                tmp = arr[i];
                                arr[i] = arr[i + 1];
                                arr[i + 1] = tmp;
                                swapped = true;
                                BasicProcessor.getInstance().getEndBlock();
                            }
                        }
                        BasicProcessor.getInstance().getEndBlock();
                    }
                }
                BasicProcessor.getInstance().getEndBlock();
            }
        }
        BasicProcessor.getInstance().getEndBlock();
    }
}
