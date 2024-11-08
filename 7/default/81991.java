import eprog.*;
import java.lang.Math.*;

public class Rgb extends EprogIO {

    public static boolean input_err = false;

    public static void main(String args[]) {
        int red = 0, green = 0, blue = 0;
        try {
            red = readInt();
            green = readInt();
            blue = readInt();
        } catch (EprogException e) {
            input_err = true;
        }
        if (input_err) println("?"); else {
            calc_corners(red, green, blue);
        }
    }

    public static void calc_corners(int red, int green, int blue) {
        int i = 0;
        short j = 1;
        short[] RG = new short[8];
        short[] B = new short[4];
        short i_red, i_green, i_blue;
        boolean quader = false;
        if (!(0 <= red && red <= 255 && 0 <= green && green <= 255 && 0 <= blue && blue <= 255)) {
            println("FALSCHE EINGABE");
        } else {
            for (i = 0; i <= 7; i++) {
                RG[i] = (short) (java.lang.Math.round((float) (i * 255) / 7));
            }
            for (i = 0; i <= 3; i++) B[i] = (short) (i * 255 / 3);
            while (quader == false) {
                if (red <= RG[j]) {
                    quader = true;
                } else j++;
            }
            i_red = j;
            j = 1;
            quader = false;
            while (quader == false) {
                if (green <= RG[j]) {
                    quader = true;
                } else j++;
            }
            i_green = j;
            j = 1;
            quader = false;
            while (quader == false) {
                if (blue <= B[j]) {
                    quader = true;
                } else j++;
            }
            i_blue = j;
            short corner_r = RG[i_red];
            short corner_g = RG[i_green];
            short corner_b = B[i_blue];
            calc_dist(corner_r, corner_g, corner_b, red, green, blue);
        }
    }

    public static void calc_dist(short corner_r, short corner_g, short corner_b, int red, int green, int blue) {
        double x = 0;
        double y = 0;
        int i = 0;
        float[] pkt = new float[8];
        float[] norm_abst = new float[8];
        x = Math.sqrt(Math.pow((corner_r - red), 2) + Math.pow((corner_g - green), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((corner_b - blue), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((corner_r - red), 2) + Math.pow((green - (corner_g - 36)), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((corner_b - blue), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((red - (corner_r - 36)), 2) + Math.pow((green - (corner_g - 36)), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((corner_b - blue), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((red - (corner_r - 36)), 2) + Math.pow((corner_g - green), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((corner_b - blue), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((corner_r - red), 2) + Math.pow((corner_g - green), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((blue - (corner_b - 85)), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((corner_r - red), 2) + Math.pow((green - (corner_g - 36)), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((blue - (corner_b - 85)), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((red - (corner_r - 36)), 2) + Math.pow((green - (corner_g - 36)), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((blue - (corner_b - 85)), 2));
        pkt[i] = (float) y;
        i++;
        x = Math.sqrt(Math.pow((red - (corner_r - 36)), 2) + Math.pow((corner_g - green), 2));
        y = Math.sqrt(Math.pow(x, 2) + Math.pow((blue - (corner_b - 85)), 2));
        pkt[i] = (float) y;
        float summe = (pkt[0] + pkt[1] + pkt[2] + pkt[3] + pkt[4] + pkt[5] + pkt[6] + pkt[7]);
        for (int j = 0; j < 8; j++) {
            norm_abst[j] = (pkt[j] / summe);
        }
        sort(norm_abst);
    }

    public static void sort(float norm_abst[]) {
        float temp;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (norm_abst[j] > norm_abst[j + 1]) {
                    temp = norm_abst[j];
                    norm_abst[j] = norm_abst[j + 1];
                    norm_abst[j + 1] = temp;
                }
            }
        }
        printFixed(norm_abst[0]);
        print(" ");
        printFixed(norm_abst[1]);
        print(" ");
        printFixed(norm_abst[2]);
        print(" ");
        printFixed(norm_abst[3]);
        print(" ");
        printFixed(norm_abst[4]);
        print(" ");
        printFixed(norm_abst[5]);
        print(" ");
        printFixed(norm_abst[6]);
        print(" ");
        printFixed(norm_abst[7]);
        print("\n");
    }
}
