package sorting;

import takatuka.drivers.DriversFactory;

public class BubbleSort {

    private static final boolean WANT_PROGRESS = true;

    private static int progressCounter;

    private static void printProgress() {
        System.out.println("I'm still there: " + progressCounter++ + "!");
    }

    private static void printIntro() {
        System.out.println("Start\n");
        if (WANT_PROGRESS) {
            System.out.println("Progress printing enabled " + "(see source to disable)...\n");
        } else {
            System.out.println("Progress printing disabled " + "(see source to enable)...\n");
        }
    }

    public static void main(String args[]) {
        int i, j, l;
        short NUMNUMBERS = 100;
        short numbers[] = new short[NUMNUMBERS];
        printIntro();
        int time = (int) System.currentTimeMillis();
        for (i = 0; i < NUMNUMBERS; i++) {
            numbers[i] = (short) (NUMNUMBERS - 1 - i);
        }
        for (i = 0; i < NUMNUMBERS; i++) {
            for (j = 0; j < NUMNUMBERS - i - 1; j++) {
                if (numbers[j] > numbers[j + 1]) {
                    short temp = numbers[j];
                    numbers[j] = numbers[j + 1];
                    numbers[j + 1] = temp;
                }
            }
            if (WANT_PROGRESS) {
                printProgress();
            }
        }
        time = (int) System.currentTimeMillis() - time;
        System.out.print(time);
        System.out.print("End\n");
    }
}
