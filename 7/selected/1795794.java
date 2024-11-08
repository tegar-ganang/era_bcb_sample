package com.z8888q.zlottery.algorithmic;

import java.util.ArrayList;
import java.util.Random;
import com.z8888q.zlottery.Zlottery;

public class Algorithmic {

    Zlottery zl;

    Random rdm = new Random();

    String[] number = { "", "", "", "", "", "", "" };

    public Algorithmic(Zlottery zl) {
        this.zl = zl;
    }

    public String[] selectnumber() {
        switch(zl.center_show_mark) {
            case Zlottery.MARK_DALETO:
                return daleto();
            case Zlottery.MARK_F15X5:
                return f15x5();
            case Zlottery.MARK_F3D:
                return f3d();
            case Zlottery.MARK_F7LECAI:
                return f7lecai();
            case Zlottery.MARK_PAILIE3:
                return pailie3();
            case Zlottery.MARK_PAILIE5:
                return pailie5();
            case Zlottery.MARK_SHUANG:
                return shuang();
            case Zlottery.MARK_T22X5:
                return t22x5();
            case Zlottery.MARK_T7XCAI:
                return t7xcai();
            default:
                return number;
        }
    }

    public int[] Xuan(int n, int m) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        int[] returnValue = new int[n];
        for (int i = 0; i < n; i++) {
            while (true) {
                int temp = rdm.nextInt(m);
                if (!result.contains(temp)) {
                    result.add(temp);
                    break;
                }
            }
            returnValue[i] = result.get(result.size() - 1);
        }
        return returnValue;
    }

    public void clear_number() {
        for (int i = 7; i < 7; i++) number[i] = "";
    }

    public String[] shuang() {
        int[] six = new int[6];
        six = Xuan(6, 33);
        six = bubbleSort(six);
        int[] one = new int[1];
        one = Xuan(1, 16);
        for (int i = 0; i < 7; i++) {
            if (i < 6) number[i] = (six[i] + 1) + ""; else number[i] = (one[i - 6] + 1) + "";
        }
        return to_standardize(number);
    }

    public String[] daleto() {
        int[] wu = new int[5];
        wu = Xuan(5, 35);
        wu = bubbleSort(wu);
        int[] er = new int[2];
        er = Xuan(2, 12);
        er = bubbleSort(er);
        for (int i = 0; i < 7; i++) {
            if (i < 5) number[i] = (wu[i] + 1) + ""; else number[i] = (er[i - 5] + 1) + "";
        }
        return to_standardize(number);
    }

    public String[] f3d() {
        clear_number();
        number[1] = rdm.nextInt(10) + "";
        number[3] = rdm.nextInt(10) + "";
        number[5] = rdm.nextInt(10) + "";
        return number;
    }

    public String[] pailie3() {
        clear_number();
        number[1] = rdm.nextInt(10) + "";
        number[3] = rdm.nextInt(10) + "";
        number[5] = rdm.nextInt(10) + "";
        return number;
    }

    public String[] pailie5() {
        clear_number();
        for (int i = 0; i < 5; i++) number[i + 1] = rdm.nextInt(10) + "";
        return number;
    }

    public String[] t7xcai() {
        for (int i = 0; i < 7; i++) number[i] = rdm.nextInt(10) + "";
        return number;
    }

    public String[] f7lecai() {
        int[] qi = new int[5];
        qi = Xuan(7, 30);
        qi = bubbleSort(qi);
        for (int i = 0; i < 7; i++) {
            number[i] = (qi[i] + 1) + "";
        }
        return to_standardize(number);
    }

    public String[] t22x5() {
        clear_number();
        int[] wu = new int[5];
        wu = Xuan(5, 22);
        wu = bubbleSort(wu);
        for (int i = 0; i < 5; i++) {
            number[i + 1] = (wu[i] + 1) + "";
        }
        return to_standardize(number);
    }

    public String[] f15x5() {
        clear_number();
        int[] wu = new int[5];
        wu = Xuan(5, 15);
        wu = bubbleSort(wu);
        for (int i = 0; i < 5; i++) {
            number[i + 1] = (wu[i] + 1) + "";
        }
        return to_standardize(number);
    }

    public int[] bubbleSort(int[] data) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data.length - i - 1; j++) {
                if (data[j] > data[j + 1]) {
                    int temp = data[j];
                    data[j] = data[j + 1];
                    data[j + 1] = temp;
                }
            }
        }
        return data;
    }

    public String[] to_standardize(String[] number) {
        for (int i = 0; i < number.length; i++) {
            if (number[i].length() == 1) number[i] = "0" + number[i];
        }
        return number;
    }
}
