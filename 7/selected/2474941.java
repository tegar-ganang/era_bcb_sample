package fs;

import java.util.Vector;
import utilities.IOFile;

public class Preprocessing {

    public static final int skipvalue = -999;

    private static float[] minNeg = null;

    private static float[] maxPos = null;

    private static float[] means = null;

    private static float[] stds = null;

    public static void MaxMinColumn(float[][] M, float[] mm, int col) {
        mm[0] = M[0][col];
        mm[1] = M[0][col];
        for (int i = 0; i < M.length; i++) {
            if (M[i][col] > mm[0]) {
                mm[0] = M[i][col];
            }
            if (M[i][col] < mm[1]) {
                mm[1] = M[i][col];
            }
        }
    }

    public static void MaxMinRow(float[][] M, float[] mm, int row, int label) {
        mm[0] = M[row][0];
        mm[1] = M[row][0];
        for (int j = 0; j < M[0].length - label; j++) {
            if (M[row][j] > mm[0]) {
                mm[0] = M[row][j];
            }
            if (M[row][j] < mm[1]) {
                mm[1] = M[row][j];
            }
        }
    }

    public static void MaxMin(float[][] M, float[] mm) {
        mm[0] = M[0][0];
        mm[1] = M[0][0];
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[0].length; j++) {
                if (M[i][j] > mm[0]) {
                    mm[0] = M[i][j];
                }
                if (M[i][j] < mm[1]) {
                    mm[1] = M[i][j];
                }
            }
        }
    }

    public static void MaxMin(float[] M, float[] mm) {
        mm[0] = M[0];
        mm[1] = M[0];
        for (int i = 1; i < M.length; i++) {
            if ((int) M[i] != skipvalue) {
                if (M[i] > mm[0]) {
                    mm[0] = M[i];
                }
                if (M[i] < mm[1]) {
                    mm[1] = M[i];
                }
            }
        }
    }

    public static float[][] FilterMA(float[][] expressiondata, Vector[] geneids, Vector remaingenes, Vector removedgenes) {
        for (int lin = 0; lin < expressiondata.length; lin++) {
            int contz = 0;
            for (int col = 0; col < expressiondata[0].length; col++) {
                if (expressiondata[lin][col] == 0) {
                    contz++;
                }
            }
            if (contz < 1) {
                remaingenes.add(lin);
            } else {
                removedgenes.add(geneids[0].get(lin));
                System.out.println("Gene " + (String) geneids[0].get(lin) + " was removed by filter.");
            }
        }
        System.out.println(removedgenes.size() + " removed genes.");
        float[][] filtereddata = new float[remaingenes.size()][expressiondata[0].length];
        for (int i = 0; i < remaingenes.size(); i++) {
            int lin = (Integer) remaingenes.get(i);
            for (int col = 0; col < expressiondata[0].length; col++) {
                filtereddata[i][col] = expressiondata[lin][col];
            }
        }
        return (filtereddata);
    }

    public static float[][] ApplyLog2(float[][] expressiondata) {
        float[][] filtereddata = new float[expressiondata.length][expressiondata[0].length];
        for (int row = 0; row < expressiondata.length; row++) {
            for (int col = 0; col < expressiondata[0].length; col++) {
                if (expressiondata[row][col] != skipvalue) {
                    filtereddata[row][col] = ((Double) (Math.log(expressiondata[row][col]) / Math.log(2))).floatValue();
                } else {
                    filtereddata[row][col] = expressiondata[row][col];
                }
            }
        }
        return (filtereddata);
    }

    public static void QuickSort(float[][] M, int inicio, int fim) {
        int i, j;
        float[] aux = new float[2];
        float pivo = M[inicio][0];
        i = inicio;
        j = fim;
        while (i < j) {
            while (M[i][0] <= pivo && i < fim) {
                i++;
            }
            while (M[j][0] >= pivo && j > inicio) {
                j--;
            }
            if (i < j) {
                aux[0] = M[i][0];
                aux[1] = M[i][1];
                M[i][0] = M[j][0];
                M[i][1] = M[j][1];
                M[j][0] = aux[0];
                M[j][1] = aux[1];
            }
        }
        if (inicio != j) {
            aux[0] = M[inicio][0];
            aux[1] = M[inicio][1];
            M[inicio][0] = M[j][0];
            M[inicio][1] = M[j][1];
            M[j][0] = aux[0];
            M[j][1] = aux[1];
        }
        if (inicio < j - 1) {
            QuickSort(M, inicio, j - 1);
        }
        if (fim > j + 1) {
            QuickSort(M, j + 1, fim);
        }
    }

    public static void QuickSort(float[] M, int inicio, int fim) {
        int i, j;
        float aux;
        float pivo = M[inicio];
        i = inicio;
        j = fim;
        while (i < j) {
            while (M[i] <= pivo && i < fim) {
                i++;
            }
            while (M[j] >= pivo && j > inicio) {
                j--;
            }
            if (i < j) {
                aux = M[i];
                M[i] = M[j];
                M[j] = aux;
            }
        }
        if (inicio != j) {
            aux = M[inicio];
            M[inicio] = M[j];
            M[j] = aux;
        }
        if (inicio < j - 1) {
            QuickSort(M, inicio, j - 1);
        }
        if (fim > j + 1) {
            QuickSort(M, j + 1, fim);
        }
    }

    public static void QuickSortASC(float[][] M, int inicio, int fim, int[] index) {
        int i, j, k;
        float[] aux = new float[M[0].length];
        float[] pivo = new float[index.length];
        for (i = 0; i < index.length; i++) {
            pivo[i] = M[inicio][index[i]];
        }
        i = inicio;
        j = fim;
        while (i < j) {
            while (M[i][index[1]] <= pivo[1] && i < fim) {
                if (M[i][index[1]] == pivo[1]) {
                    if (M[i][index[0]] <= pivo[0]) {
                        i++;
                    } else {
                        break;
                    }
                } else {
                    i++;
                }
            }
            while (M[j][index[1]] >= pivo[1] && j > inicio) {
                if (M[j][index[1]] == pivo[1]) {
                    if (M[j][index[0]] >= pivo[0]) {
                        j--;
                    } else {
                        break;
                    }
                } else {
                    j--;
                }
            }
            if (i < j) {
                for (k = 0; k < M[0].length; k++) {
                    aux[k] = M[i][k];
                }
                for (k = 0; k < M[0].length; k++) {
                    M[i][k] = M[j][k];
                }
                for (k = 0; k < M[0].length; k++) {
                    M[j][k] = aux[k];
                }
            }
        }
        if (inicio != j) {
            for (k = 0; k < M[0].length; k++) {
                aux[k] = M[inicio][k];
            }
            for (k = 0; k < M[0].length; k++) {
                M[inicio][k] = M[j][k];
            }
            for (k = 0; k < M[0].length; k++) {
                M[j][k] = aux[k];
            }
        }
        if (inicio < j - 1) {
            QuickSortASC(M, inicio, j - 1, index);
        }
        if (fim > j + 1) {
            QuickSortASC(M, j + 1, fim, index);
        }
    }

    public static void QuickSortDSC(Vector M, int inicio, int fim, int[] index) {
        int i, j;
        Vector aux;
        double[] pivo = new double[index.length];
        for (i = 0; i < index.length; i++) {
            pivo[i] = ((double[]) ((Vector) M.get(inicio)).get(0))[index[i]];
        }
        i = inicio;
        j = fim;
        while (i < j) {
            while (((double[]) ((Vector) M.get(i)).get(0))[index[0]] <= pivo[0] && i < fim) {
                if (((double[]) ((Vector) M.get(i)).get(0))[index[0]] == pivo[0]) {
                    if (((double[]) ((Vector) M.get(i)).get(0))[index[1]] >= pivo[1]) {
                        i++;
                    } else {
                        break;
                    }
                } else {
                    i++;
                }
            }
            while (((double[]) ((Vector) M.get(j)).get(0))[index[0]] >= pivo[0] && j > inicio) {
                if (((double[]) ((Vector) M.get(j)).get(0))[index[0]] == pivo[0]) {
                    if (((double[]) ((Vector) M.get(j)).get(0))[index[1]] <= pivo[1]) {
                        j--;
                    } else {
                        break;
                    }
                } else {
                    j--;
                }
            }
            if (i < j) {
                aux = (Vector) M.get(i);
                M.set(i, M.get(j));
                M.set(j, aux);
            }
        }
        if (inicio != j) {
            aux = (Vector) M.get(inicio);
            M.set(inicio, M.get(j));
            M.set(j, aux);
        }
        if (inicio < j - 1) {
            QuickSortDSC(M, inicio, j - 1, index);
        }
        if (fim > j + 1) {
            QuickSortDSC(M, j + 1, fim, index);
        }
    }

    public static void SelectionSort(Vector<Vector> M) {
        Vector aux;
        float minvalue = 0;
        int minposition = 0;
        for (int j = 0; j < M.size() - 1; j++) {
            minvalue = (Float) M.get(j).get(0);
            minposition = j;
            for (int i = j + 1; i < M.size(); i++) {
                if ((Float) M.get(i).get(0) < minvalue) {
                    minvalue = (Float) M.get(i).get(0);
                    minposition = i;
                }
            }
            if (minposition != j) {
                aux = M.get(j);
                M.set(j, M.get(minposition));
                M.set(minposition, aux);
            }
        }
    }

    public static void SelectionSort(Vector<Vector> M, int index) {
        Vector aux;
        int minvalue = 0;
        int minposition = 0;
        for (int j = 0; j < M.size() - 1; j++) {
            minvalue = (Integer) M.get(j).get(index);
            minposition = j;
            for (int i = j + 1; i < M.size(); i++) {
                if ((Integer) M.get(i).get(index) < minvalue) {
                    minvalue = (Integer) M.get(i).get(index);
                    minposition = i;
                }
            }
            if (minposition != j) {
                aux = M.get(j);
                M.set(j, M.get(minposition));
                M.set(minposition, aux);
            }
        }
    }

    public static void BubbleSort(Vector<Vector> M) {
        Vector aux;
        boolean change = true;
        while (change) {
            change = false;
            for (int i = 0; i < M.size() - 1; i++) {
                if ((Double) M.get(i).get(0) > (Double) M.get(i + 1).get(0)) {
                    aux = M.get(i);
                    M.set(i, M.get(i + 1));
                    M.set(i + 1, aux);
                    change = true;
                }
            }
        }
    }

    public static int[] BubbleSortDEC(int[] values) {
        boolean change = true;
        int aux;
        int[] indexes = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            indexes[i] = i;
        }
        while (change) {
            change = false;
            for (int i = 0; i < values.length - 1; i++) {
                if (values[i] < values[i + 1]) {
                    aux = values[i];
                    values[i] = values[i + 1];
                    values[i + 1] = aux;
                    aux = indexes[i];
                    indexes[i] = indexes[i + 1];
                    indexes[i + 1] = aux;
                    change = true;
                }
            }
        }
        return (indexes);
    }

    public static void QuickSort(Vector<Vector> M, int inicio, int fim) {
        int i, j;
        Vector aux;
        float pivo = (Float) M.get(inicio).get(0);
        i = inicio;
        j = fim;
        while (i < j) {
            while ((Float) M.get(i).get(0) <= pivo && i < fim) {
                i++;
            }
            while ((Float) M.get(j).get(0) >= pivo && j > inicio) {
                j--;
            }
            if (i < j) {
                aux = M.get(i);
                M.set(i, M.get(j));
                M.set(j, aux);
            }
        }
        if (inicio != j) {
            aux = M.get(inicio);
            M.set(inicio, M.get(j));
            M.set(j, aux);
        }
        if (inicio < j - 1) {
            QuickSort(M, inicio, j - 1);
        }
        if (fim > j + 1) {
            QuickSort(M, j + 1, fim);
        }
    }

    public static void QuickSortASC(Vector M, int inicio, int fim) {
        int i, j;
        double aux;
        double pivo = (Double) M.get(inicio);
        i = inicio;
        j = fim;
        while (i < j) {
            while ((Double) M.get(i) <= pivo && i < fim) {
                i++;
            }
            while ((Double) M.get(j) >= pivo && j > inicio) {
                j--;
            }
            if (i < j) {
                aux = (Double) M.get(i);
                M.set(i, M.get(j));
                M.set(j, aux);
            }
        }
        if (inicio != j) {
            aux = (Double) M.get(inicio);
            M.set(inicio, M.get(j));
            M.set(j, aux);
        }
        if (inicio < j - 1) {
            QuickSortASC(M, inicio, j - 1);
        }
        if (fim > j + 1) {
            QuickSortASC(M, j + 1, fim);
        }
    }

    public static StringBuffer MakeResultList(float[][] R, Vector ordenado) {
        int[] I = new int[2];
        I[0] = 0;
        I[1] = 2;
        QuickSortASC(R, 0, R.length - 1, I);
        int i = 0;
        int maxc = 0;
        while (i < R.length) {
            Vector preditos = new Vector();
            preditos.add((int) R[i][1]);
            int preditor = (int) R[i][0];
            float entropia = (float) R[i][2];
            int count = 1;
            i++;
            while (i < R.length && R[i][0] == preditor && Math.abs(R[i][2] - entropia) < 0.001) {
                preditos.add((int) R[i][1]);
                count++;
                i++;
            }
            if (preditos.size() > maxc) {
                maxc = preditos.size();
            }
            Vector item = new Vector();
            item.add(new double[] { preditor, entropia, count });
            item.add(preditos);
            ordenado.add(item);
        }
        I[0] = 1;
        I[1] = 2;
        QuickSortDSC(ordenado, 0, ordenado.size() - 1, I);
        StringBuffer res = new StringBuffer("predictor\tentropy \tfrequency \n");
        for (i = 0; i < ordenado.size(); i++) {
            Vector item = (Vector) ordenado.get(i);
            double[] item1 = (double[]) item.get(0);
            int preditor = (int) item1[0];
            float entropia = (float) item1[1];
            int count = (int) item1[2];
            res.append(preditor + "\t\t" + entropia + "\t\t" + count + "\n");
        }
        return (res);
    }

    public static double[][] transpose(double[][] M) {
        int lines = M.length;
        int columns = M[0].length;
        double[][] Mtrans = new double[columns][lines];
        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < columns; j++) {
                Mtrans[j][i] = M[i][j];
            }
        }
        return Mtrans;
    }

    public static float[][] InvertColumns(float[][] M) {
        int lines = M.length;
        int columns = M[0].length;
        float[][] MInv = new float[lines][columns];
        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < columns; j++) {
                MInv[i][j] = M[i][columns - j - 1];
            }
        }
        return MInv;
    }

    public static float[][] copyMatrix(float[][] M) {
        int lines = M.length;
        int columns = M[0].length;
        float[][] cM = new float[lines][columns];
        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < columns; j++) {
                cM[i][j] = M[i][j];
            }
        }
        return cM;
    }

    public static void normalTransformcolumns(float[][] M, boolean extreme_values, int label) {
        int lines = M.length;
        int columns = M[0].length;
        if (extreme_values) {
            means = new float[columns - label];
            stds = new float[columns - label];
        } else if (minNeg == null) {
            new fs.FSException("Error on applying normal transform.", false);
        }
        for (int j = 0; j < columns - label; j++) {
            if (M[0][j] != skipvalue) {
                if (extreme_values) {
                    float sum = 0;
                    for (int i = 0; i < lines; i++) {
                        sum += M[i][j];
                    }
                    means[j] = sum / lines;
                    stds[j] = 0f;
                    for (int i = 0; i < lines; i++) {
                        stds[j] += (M[i][j] - means[j]) * (M[i][j] - means[j]);
                    }
                    stds[j] /= (lines - 1);
                    stds[j] = ((Double) Math.sqrt(stds[j])).floatValue();
                }
                if (stds[j] > 0) {
                    for (int i = 0; i < lines; i++) {
                        M[i][j] -= means[j];
                        M[i][j] /= stds[j];
                    }
                } else {
                    for (int i = 0; i < lines; i++) {
                        M[i][j] = 0;
                    }
                }
            }
        }
    }

    public static void normalTransformlines(float[][] M, boolean extreme_values, int label) {
        int lines = M.length;
        int columns = M[0].length;
        if (extreme_values) {
            means = new float[lines];
            stds = new float[lines];
        } else if (minNeg == null) {
            new fs.FSException("Error on applying normal transform.", false);
        }
        for (int j = 0; j < lines; j++) {
            if (extreme_values) {
                double sum = 0;
                for (int i = 0; i < columns - label; i++) {
                    sum += M[j][i];
                }
                means[j] = ((Double) (sum / (columns - label))).floatValue();
                stds[j] = 0;
                for (int i = 0; i < columns - label; i++) {
                    stds[j] += (M[j][i] - means[j]) * (M[j][i] - means[j]);
                }
                stds[j] /= (columns - label - 1);
                stds[j] = ((Double) Math.sqrt(((Float) stds[j]).doubleValue())).floatValue();
            }
            if (stds[j] > 0) {
                for (int i = 0; i < columns - label; i++) {
                    M[j][i] -= means[j];
                    M[j][i] /= stds[j];
                }
            } else {
                for (int i = 0; i < columns - label; i++) {
                    M[j][i] = 0;
                }
            }
        }
    }

    public static void ScaleColumn(float[][] M, int maxvalue, int label) {
        float[] maxmin = new float[2];
        float normalizedvalue = 0;
        for (int col = 0; col < M[0].length - label; col++) {
            MaxMinColumn(M, maxmin, col);
            for (int lin = 0; lin < M.length; lin++) {
                normalizedvalue = (maxvalue - 1) * (M[lin][col] - maxmin[1]) / (maxmin[0] - maxmin[1]);
                M[lin][col] = normalizedvalue;
            }
        }
    }

    public static void ScaleRow(float[][] M, int maxvalue, int label) {
        float[] maxmin = new float[2];
        float normalizedvalue = 0;
        for (int row = 0; row < M.length; row++) {
            MaxMinRow(M, maxmin, row, label);
            for (int col = 0; col < M[0].length - label; col++) {
                normalizedvalue = (maxvalue - 1) * (M[row][col] - maxmin[1]) / (maxmin[0] - maxmin[1]);
                M[row][col] = normalizedvalue;
            }
        }
    }

    public static void normalize(float[][] M, int qd, int label) {
        float[] threshold = new float[qd - 1];
        float[] maxmin = new float[2];
        float normalizedvalue = 0;
        float increment = (qd - 1) / ((float) qd);
        for (int k = 0; k < (qd - 1); k++) {
            threshold[k] = increment;
            increment += increment;
        }
        for (int lin = 0; lin < M.length; lin++) {
            MaxMin(M[lin], maxmin);
            for (int col = 0; col < M[lin].length - label; col++) {
                normalizedvalue = (qd - 1) * (M[lin][col] - maxmin[1]) / (maxmin[0] - maxmin[1]);
                int k = 0;
                for (k = 0; k < (qd - 1); k++) {
                    if (threshold[k] >= normalizedvalue) {
                        break;
                    }
                }
                M[lin][col] = k;
            }
        }
    }

    public static void quantizerows(float[][] M, int qd, boolean extreme_values, int label) {
        int lines = M.length;
        int columns = M[0].length;
        normalTransformlines(M, extreme_values, label);
        for (int j = 0; j < lines; j++) {
            if (M[0][j] != skipvalue) {
                Vector negatives = new Vector();
                Vector positives = new Vector();
                float meanneg = 0;
                float meanpos = 0;
                for (int i = 0; i < columns - label; i++) {
                    if (M[j][i] < 0) {
                        negatives.add(M[j][i]);
                        meanneg += M[j][i];
                    } else {
                        positives.add(M[j][i]);
                        meanpos += M[j][i];
                    }
                }
                meanneg /= negatives.size();
                meanpos /= positives.size();
                int indThreshold = 0;
                double increment = -meanneg / ((double) qd / 2);
                double[] threshold = new double[qd - 1];
                for (double i = meanneg + increment; i < 0; i += increment, indThreshold++) {
                    threshold[indThreshold] = i;
                }
                increment = meanpos / ((double) qd / 2);
                indThreshold = qd - 2;
                for (double i = meanpos - increment; i > 0; i -= increment, indThreshold--) {
                    threshold[indThreshold] = i;
                }
                for (int i = 0; i < columns - label; i++) {
                    int k;
                    for (k = 0; k < qd - 1; k++) {
                        if (threshold[k] >= M[j][i]) {
                            break;
                        }
                    }
                    M[j][i] = k;
                }
            }
        }
    }

    public static void quantizecolumns(float[][] M, int qd, boolean extreme_values, int label) {
        int lines = M.length;
        int columns = M[0].length;
        normalTransformcolumns(M, extreme_values, label);
        for (int j = 0; j < columns - label; j++) {
            if (M[0][j] != skipvalue) {
                Vector negatives = new Vector();
                Vector positives = new Vector();
                float meanneg = 0;
                float meanpos = 0;
                for (int i = 0; i < lines; i++) {
                    if (M[i][j] < 0) {
                        negatives.add(M[i][j]);
                        meanneg += M[i][j];
                    } else {
                        positives.add(M[i][j]);
                        meanpos += M[i][j];
                    }
                }
                meanneg /= negatives.size();
                meanpos /= positives.size();
                int indThreshold = 0;
                double increment = -meanneg / ((double) qd / 2);
                double[] threshold = new double[qd - 1];
                for (double i = meanneg + increment; i < 0; i += increment, indThreshold++) {
                    threshold[indThreshold] = i;
                }
                increment = meanpos / ((double) qd / 2);
                indThreshold = qd - 2;
                for (double i = meanpos - increment; i > 0; i -= increment, indThreshold--) {
                    threshold[indThreshold] = i;
                }
                for (int i = 0; i < lines; i++) {
                    int k;
                    for (k = 0; k < qd - 1; k++) {
                        if (threshold[k] >= M[i][j]) {
                            break;
                        }
                    }
                    M[i][j] = k;
                }
            }
        }
    }

    public static float[][] quantizecolumnsMAnormal(float[][] M, int[][] quantizeddata, int qd, float[] mean, float[] std, float[] lowthreshold, float[] hithreshold) {
        int totalrows = M.length;
        int totalcols = M[0].length;
        float[][] auxM = Preprocessing.copyMatrix(M);
        normalTransformcolumns(auxM, true, 0);
        minNeg = new float[totalcols];
        maxPos = new float[totalcols];
        for (int col = 0; col < totalcols; col++) {
            if (M[0][col] != skipvalue) {
                float sum = 0;
                float[] colvalues = new float[totalrows];
                for (int row = 0; row < totalrows; row++) {
                    sum += M[row][col];
                    colvalues[row] = auxM[row][col];
                }
                mean[col] = sum / totalrows;
                std[col] = 0;
                for (int row = 0; row < totalrows; row++) {
                    std[col] += (M[row][col] - mean[col]) * (M[row][col] - mean[col]);
                }
                std[col] /= (totalrows - 1);
                std[col] = ((Double) Math.sqrt(std[col])).floatValue();
                IOFile.PrintArray(colvalues);
                QuickSort(colvalues, 0, totalrows - 1);
                IOFile.PrintArray(colvalues);
                Vector negatives = new Vector();
                Vector positives = new Vector();
                float meanpos = 0;
                float meanneg = 0;
                float meantotal = 0;
                for (int i = 0; i < totalrows; i++) {
                    if (auxM[i][col] < 0) {
                        negatives.add(auxM[i][col]);
                        meanneg += auxM[i][col];
                    } else {
                        positives.add(auxM[i][col]);
                        meanpos += auxM[i][col];
                    }
                    meantotal += auxM[i][col];
                }
                if (std[col] == 0) {
                    continue;
                }
                if (negatives.isEmpty() || positives.isEmpty()) {
                    continue;
                }
                meanneg /= negatives.size();
                meanpos /= positives.size();
                meantotal /= totalrows;
                double[] threshold = new double[qd - 1];
                if (qd == 2) {
                    int index3rdq = Math.round(0.75f * (totalrows + 1));
                    threshold[0] = colvalues[index3rdq];
                    lowthreshold[col] = (float) threshold[0];
                    hithreshold[col] = (float) threshold[0];
                } else if (qd == 3) {
                    threshold[0] = meanneg;
                    threshold[1] = meanpos;
                    lowthreshold[col] = (float) threshold[0];
                    hithreshold[col] = (float) threshold[1];
                } else {
                    return null;
                }
                int count0 = 0;
                int count1 = 0;
                for (int i = 0; i < totalrows; i++) {
                    int k;
                    for (k = 0; k < qd - 1; k++) {
                        if (auxM[i][col] <= threshold[k]) {
                            break;
                        }
                    }
                    if (qd == 3) {
                        if (k == 2 || k == 0) {
                            k = 1;
                        } else if (k == 1) {
                            k = 0;
                        }
                    }
                    if (k == 0) {
                        count0++;
                    } else {
                        count1++;
                    }
                    quantizeddata[i][col] = k;
                }
                System.out.println("Totais quantizados na coluna " + col + ": " + (count0 + count1));
                System.out.println("zeros = " + count0);
                System.out.println("ums = " + count1);
                System.out.println();
            } else {
                for (int i = 0; i < totalrows; i++) {
                    quantizeddata[i][col] = (int) M[i][col];
                }
            }
        }
        return (auxM);
    }
}
