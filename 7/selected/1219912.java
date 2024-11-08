package exam15;

import java.util.Random;

/**
 * 1.get out the bubble sorting by arrays. 2.modify the bubble algorithm,and
 * scanning in the forward and backward way by turns that is��the first time when
 * scanning get the max number in the bottom the second time get the min number
 * in the top and repeatedly until sorting the sequence
 * 
 * @author Fantasy
 * 
 */
public class Problem15 {

    private int[] array;

    public int[] getRandomNumber(int m) {
        array = new int[m];
        for (int i = 0; i < array.length; i++) {
            int temp = new Random().nextInt(100);
            array[i] = temp;
        }
        return array;
    }

    public void sortingByBubble(int[] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length - 1 - i; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }

    public void modifyBubble(int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (i % 2 != 0) {
                for (int j = array.length - i / 2 - 2; j >= i / 2; j--) {
                    if (array[j] >= array[j + 1]) {
                        int temp = array[j];
                        array[j] = array[j + 1];
                        array[j + 1] = temp;
                    }
                }
            } else {
                for (int j = i / 2; j < array.length - 1 - i / 2; j++) {
                    if (array[j] >= array[j + 1]) {
                        int temp = array[j];
                        array[j] = array[j + 1];
                        array[j + 1] = temp;
                    }
                }
            }
        }
    }

    public void test() {
        int a = 0;
        int b = 2;
        int c = a % b;
        System.out.println("a%2=" + c);
    }

    public static void main(String[] args) {
        Problem15 app = new Problem15();
        int[] array = new int[20];
        array = app.getRandomNumber(20);
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println();
        app.modifyBubble(array);
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
    }
}
