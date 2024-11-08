package sorting;

/**
 *
 * @author BMLAB1
 */
public class Sorting {

    public int[] dizi = new int[10];

    public void selectionSort() {
        for (int i = 0; i < dizi.length - 1; i++) {
            int enk = i;
            for (int j = i + 1; j < dizi.length; j++) {
                if (dizi[enk] > dizi[j]) {
                    enk = j;
                }
            }
            int temp = dizi[i];
            dizi[i] = dizi[enk];
            dizi[enk] = temp;
        }
    }

    void insertionsort2() {
        int kars = 0, tas = 0;
        for (int i = 1; i < dizi.length; i++) {
            for (int j = 0; j < i; j++) {
                kars++;
                if (dizi[i] < dizi[j]) {
                    int temp = dizi[i];
                    for (int k = i; k > j; k--) {
                        dizi[k] = dizi[k - 1];
                        tas++;
                    }
                    tas++;
                    dizi[j] = temp;
                }
            }
        }
        System.out.println(kars + " " + tas);
    }

    void insertionSort() {
        for (int i = 1; i < dizi.length; i++) {
            if (dizi[i] < 0) {
                continue;
            }
            int tmp = dizi[i];
            int j;
            for (j = i; j > 0 && tmp < dizi[j - 1]; j--) {
                dizi[j] = dizi[j - 1];
            }
            dizi[j] = tmp;
        }
    }

    public void bubble() {
        boolean test = false;
        int kars = 0, tas = 0;
        while (true) {
            for (int j = 0; j < dizi.length - 1; j++) {
                kars++;
                if (dizi[j] > dizi[j + 1]) {
                    int temp = dizi[j];
                    dizi[j] = dizi[j + 1];
                    dizi[j + 1] = temp;
                    test = true;
                    tas++;
                }
            }
            if (!test) {
                break;
            } else {
                test = false;
            }
        }
        System.out.print(kars + " " + tas);
    }

    public void swap(int i, int j) {
        int temp = dizi[i];
        dizi[i] = dizi[j];
        dizi[j] = temp;
    }

    void bubbleSort(int n) {
        boolean sorted = false;
        int last = n - 1;
        for (int i = 0; (i < last) && !sorted; i++) {
            sorted = true;
            for (int j = last; j > i; j--) {
                if (dizi[j - 1] > dizi[j]) {
                    swap(j, j - 1);
                    sorted = false;
                }
            }
        }
    }

    public void listele() {
        for (int i = 0; i < 10; i++) {
            System.out.print(dizi[i] + " ");
        }
    }

    public void merge(int i, int j) {
        if (i == j) {
            return;
        }
        int orta = (i + j) / 2;
        int sayac = 0;
        int[] tempdizi = new int[j - i + 1];
        merge(i, orta);
        merge(orta + 1, j);
        int k = i, m = orta + 1;
        for (k = i; k <= orta; k++) {
            for (m = orta + 1; m <= j; m++) {
                if (dizi[k] > dizi[m]) {
                    tempdizi[sayac] = dizi[m];
                    dizi[m] = -1;
                } else {
                    tempdizi[sayac] = dizi[k];
                    dizi[k] = -1;
                    k++;
                    sayac++;
                    if (k > orta) {
                        break;
                    }
                }
            }
        }
        for (int s = i; s <= j; s++) {
            if (dizi[s] != -1) {
                tempdizi[sayac] = dizi[s];
                sayac++;
            }
        }
        sayac = 0;
        for (int s = i; s <= j; s++) {
            dizi[s] = tempdizi[sayac];
            sayac++;
        }
    }
}
