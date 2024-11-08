package org.jazzteam.weektwodayfour;

import java.util.Random;

/**
 * ������ ������� X[5][6]. ������������ ������ Y[5] �� ������������� ���������
 * ������� �, �����, ����� ��� ���������� � ������ ������� � ��������.
 * 
 * @author �������� ������ ��������
 */
public class Matrix {

    private int[][] initialMatrix = { { -11, 12, -13, -14, -11 }, { -21, -22, -23, 24, -11 }, { -31, 32, -33, -34, -11 }, { 41, -22, -13, -45, -11 }, { 31, -62, -43, -33, -11 }, { -31, -44, 43, -54, -11 } };

    private int[] finalMatrix = new int[5];

    public String elementsSearch() {
        int index = 0;
        for (int i1 = 0; i1 < 6; i1++) {
            for (int i2 = 0; i2 < 5; i2++) {
                if (index < 5) {
                    if (initialMatrix[i1][i2] > 0) {
                        finalMatrix[index] = initialMatrix[i1][i2];
                        index++;
                    }
                } else break;
            }
        }
        int temp;
        for (int i = 0; i < finalMatrix.length; i++) {
            for (int j = 0; j < finalMatrix.length - 1; j++) {
                if (finalMatrix[j] < finalMatrix[j + 1]) {
                    temp = finalMatrix[j];
                    finalMatrix[j] = finalMatrix[j + 1];
                    finalMatrix[j + 1] = temp;
                }
            }
        }
        String result = "";
        for (int k : finalMatrix) result += k + " ";
        return result;
    }
}
