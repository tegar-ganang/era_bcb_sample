package verisoft.Algoritmos;

import java.awt.image.BufferedImage;

/**
 *
 * @author petra
 */
public class Thinner {

    private static final int CONSTANT_BLACK = 0xff000000;

    private static final int CONSTANT_WHITE = 0xffffffff;

    public static void thin(BufferedImage image) {
        int h = image.getHeight();
        int w = image.getWidth();
        int imgval[][] = new int[w][h];
        int mark[][] = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                imgval[x][y] = (image.getRGB(x, y) == CONSTANT_BLACK) ? 1 : 0;
            }
        }
        boolean hasdelete = true;
        while (hasdelete) {
            hasdelete = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (imgval[x][y] == 1) {
                        int nb[] = getNeighbors(imgval, x, y, w, h);
                        int a = 0;
                        for (int i = 2; i < 9; i++) {
                            if ((nb[i] == 0) && (nb[i + 1] == 1)) {
                                a++;
                            }
                        }
                        if ((nb[9] == 0) && (nb[2] == 1)) {
                            a++;
                        }
                        int b = nb[2] + nb[3] + nb[4] + nb[5] + nb[6] + nb[7] + nb[8] + nb[9];
                        int p1 = nb[2] * nb[4] * nb[6];
                        int p2 = nb[4] * nb[6] * nb[8];
                        if ((a == 1) && ((b >= 2) && (b <= 6)) && (p1 == 0) && (p2 == 0)) {
                            mark[x][y] = 0;
                            hasdelete = true;
                        } else {
                            mark[x][y] = 1;
                        }
                    } else {
                        mark[x][y] = 0;
                    }
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    imgval[x][y] = mark[x][y];
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (imgval[x][y] == 1) {
                        int nb[] = getNeighbors(imgval, x, y, w, h);
                        int a = 0;
                        for (int i = 2; i < 9; i++) {
                            if ((nb[i] == 0) && (nb[i + 1] == 1)) {
                                a++;
                            }
                        }
                        if ((nb[9] == 0) && (nb[2] == 1)) {
                            a++;
                        }
                        int b = nb[2] + nb[3] + nb[4] + nb[5] + nb[6] + nb[7] + nb[8] + nb[9];
                        int p1 = nb[2] * nb[4] * nb[8];
                        int p2 = nb[2] * nb[6] * nb[8];
                        if ((a == 1) && ((b >= 2) && (b <= 6)) && (p1 == 0) && (p2 == 0)) {
                            mark[x][y] = 0;
                            hasdelete = true;
                        } else {
                            mark[x][y] = 1;
                        }
                    } else {
                        mark[x][y] = 0;
                    }
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    imgval[x][y] = mark[x][y];
                }
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (imgval[x][y] == 1) {
                    image.setRGB(x, y, CONSTANT_BLACK);
                } else {
                    image.setRGB(x, y, CONSTANT_WHITE);
                }
            }
        }
    }

    private static int[] getNeighbors(int imgval[][], int x, int y, int w, int h) {
        int a[] = new int[10];
        for (int n = 1; n < 10; n++) {
            a[n] = 0;
        }
        if (y - 1 >= 0) {
            a[2] = imgval[x][y - 1];
            if (x + 1 < w) {
                a[3] = imgval[x + 1][y - 1];
            }
            if (x - 1 >= 0) {
                a[9] = imgval[x - 1][y - 1];
            }
        }
        if (y + 1 < h) {
            a[6] = imgval[x][y + 1];
            if (x + 1 < w) {
                a[5] = imgval[x + 1][y + 1];
            }
            if (x - 1 >= 0) {
                a[7] = imgval[x - 1][y + 1];
            }
        }
        if (x + 1 < w) {
            a[4] = imgval[x + 1][y];
        }
        if (x - 1 >= 0) {
            a[8] = imgval[x - 1][y];
        }
        return a;
    }
}
