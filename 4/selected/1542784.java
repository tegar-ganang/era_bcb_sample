package shu.util;

import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.datatransfer.*;
import shu.math.array.*;
import shu.util.log.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2001</p>
 *
 * <p>Company: </p>
 *
 * @author cms.shu.edu.tw
 * @version 1.0
 */
public final class Utils {

    public static final int execAndWaitFor(String cmd) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(cmd);
        return p.waitFor();
    }

    public static final String toString(int[][] array) {
        StringBuilder buf = new StringBuilder();
        for (int[] i : array) {
            buf.append(Arrays.toString(i));
            buf.append('\n');
        }
        return buf.toString();
    }

    public static String toString(int[] array) {
        StringBuilder buf = new StringBuilder();
        for (int x = 0; x < array.length; x++) {
            buf.append(array[x]);
            buf.append(' ');
        }
        return buf.toString();
    }

    public static final String readInputStreamAsString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder buf = new StringBuilder();
        while (reader.ready()) {
            String line = reader.readLine();
            if (line != null) {
                buf.append(line + "\n");
            } else {
                break;
            }
        }
        return buf.toString();
    }

    public static final double[] concatArray(double[] array1, double[] array2) {
        int size = array1.length + array2.length;
        double[] result = new double[size];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static final Object[] concatArray(Object[] array1, Object[] array2) {
        int size = array1.length + array2.length;
        Object[] result = new Object[size];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static final int[] concatArray(int[] array1, int[] array2) {
        int size = array1.length + array2.length;
        int[] result = new int[size];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static void main(String[] args) {
        double[] center = new double[] { 0, 0, 0 };
        double[] l = new double[] { 10, 10, 10 };
        double[][] cube = getDoubleCubeCoordinate(center, l);
        for (double[] c : cube) {
            System.out.println(Arrays.toString(c));
        }
    }

    public static final Object[][] transpose(Object[][] a) {
        int am = a.length;
        int an = a[0].length;
        Object[][] result = new Object[an][am];
        for (int i = 0; i < am; i++) {
            for (int j = 0; j < an; j++) {
                result[j][i] = a[i][j];
            }
        }
        return result;
    }

    public static void copyFile(File in, File out) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(in));
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(out));
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = is.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        is.close();
        fos.close();
    }

    /**
   * ��newIO�i��ƻs
   * @param in File
   * @param out File
   * @throws IOException
   */
    public static void copyFileByNIO(File in, File out) throws IOException {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static double[] list2DoubleArray(List<Double> list) {
        return DoubleArray.list2DoubleArray(list);
    }

    public static int[] list2IntArray(List<Integer> list) {
        int size = list.size();
        int[] result = new int[size];
        for (int x = 0; x < size; x++) {
            result[x] = list.get(x);
        }
        return result;
    }

    public static void setClipboard(Image image) {
        ImageSelection imgSel = new ImageSelection(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
    }

    public static class ImageSelection implements Transferable {

        private Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }

    public static final String fmt(double v) {
        return df.format(v);
    }

    public static final String fmt(double v, String format) {
        DecimalFormat df = new DecimalFormat(format);
        return df.format(v);
    }

    public static final String fmt(double[] v) {
        return DoubleArray.toString(df, v);
    }

    public static final String fmt(double[] v, String format) {
        DecimalFormat df = new DecimalFormat(format);
        return DoubleArray.toString(df, v);
    }

    public static final DecimalFormat df = new DecimalFormat("##.###");

    /**
   * �Hcenter�������Ilength�����,�ҭp��X�Ӫ���(�|)����y�� (26�I: 8+9x2 , �����I���h)
   * @param center double[]
   * @param length double[]
   * @return double[][]
   */
    public static final double[][] getCubeCoordinate(double[] center, double[] length) {
        if (center.length != 3 || length.length != 3) {
            throw new IllegalArgumentException("center.length != 3 || length.length != 3");
        }
        double[][] cube = new double[26][];
        double[] halfLength = DoubleArray.times(length, .5);
        int index = 0;
        double xlimit = center[0] + halfLength[0];
        double ylimit = center[1] + halfLength[1];
        double zlimit = center[2] + halfLength[2];
        xlimit = (xlimit == center[0]) ? xlimit + Double.MIN_NORMAL : xlimit;
        ylimit = (ylimit == center[0]) ? ylimit + Double.MIN_NORMAL : ylimit;
        zlimit = (zlimit == center[0]) ? zlimit + Double.MIN_NORMAL : zlimit;
        for (double x = center[0] - halfLength[0]; x <= xlimit; x += halfLength[0]) {
            for (double y = center[1] - halfLength[1]; y <= ylimit; y += halfLength[1]) {
                for (double z = center[2] - halfLength[2]; z <= zlimit; z += halfLength[2]) {
                    if (x == center[0] && y == center[1] && z == center[2]) {
                        continue;
                    }
                    if (index >= 26) {
                        return cube;
                    }
                    cube[index++] = new double[] { x, y, z };
                }
            }
        }
        return cube;
    }

    /**
   * �Hcenter�������Ilength�����,�ҭp��X�Ӫ���(�|)����y�� (124�I)
   * (��ߤ���y��, �j�]�p���ߤ���: 26+16x5+9x2 )
   * @param center double[]
   * @param length double[]
   * @return double[][]
   */
    public static final double[][] getDoubleCubeCoordinate(double[] center, double[] length) {
        if (center.length != 3 || length.length != 3) {
            throw new IllegalArgumentException("center.length != 3 || length.length != 3");
        }
        double[][] cube = new double[124][];
        double[] halfLength = DoubleArray.times(length, .5);
        int index = 0;
        double xlimit = center[0] + halfLength[0] * 2;
        double ylimit = center[1] + halfLength[1] * 2;
        double zlimit = center[2] + halfLength[2] * 2;
        xlimit = (xlimit == center[0]) ? xlimit + Double.MIN_NORMAL : xlimit;
        ylimit = (ylimit == center[0]) ? ylimit + Double.MIN_NORMAL : ylimit;
        zlimit = (zlimit == center[0]) ? zlimit + Double.MIN_NORMAL : zlimit;
        for (double x = center[0] - halfLength[0] * 2; x <= xlimit; x += halfLength[0]) {
            for (double y = center[1] - halfLength[1] * 2; y <= ylimit; y += halfLength[1]) {
                for (double z = center[2] - halfLength[2] * 2; z <= zlimit; z += halfLength[2]) {
                    if (x == center[0] && y == center[1] && z == center[2]) {
                        continue;
                    }
                    if (index >= 124) {
                        return cube;
                    }
                    cube[index++] = new double[] { x, y, z };
                }
            }
        }
        return cube;
    }

    public static final List<String> filterNameList(List nameIFList) {
        List<String> nameList = new ArrayList<String>(nameIFList.size());
        for (Object o : nameIFList) {
            NameIF n = (NameIF) o;
            nameList.add(n.getName());
        }
        return nameList;
    }

    public static final String getAcronym(String str) {
        StringBuilder buf = new StringBuilder();
        int size = str.length();
        for (int x = 0; x < size; x++) {
            char c = str.charAt(x);
            if (Character.isUpperCase(c)) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static final byte[] toByteArray(short s) {
        byte[] b = new byte[2];
        b[1] = new Short(s).byteValue();
        s = (short) (s >> 8);
        b[0] = new Short(s).byteValue();
        return b;
    }

    public static final void pause(boolean showMessage) {
        try {
            if (showMessage) {
                System.out.println("Press \"Enter\" to Continue...");
            }
            System.in.read();
        } catch (IOException ex) {
            Logger.log.error("", ex);
        }
    }

    public static final void pause() {
        pause(false);
    }

    public static final boolean or(boolean[] boolArray) {
        int size = boolArray.length;
        boolean b = boolArray[0];
        for (int x = 1; x < size; x++) {
            b = b || boolArray[x];
        }
        return b;
    }

    public static final int count(final boolean[] boolArray, boolean count) {
        int times = 0;
        for (boolean b : boolArray) {
            if (b == count) {
                times++;
            }
        }
        return times;
    }

    public static final boolean and(final boolean[] boolArray) {
        int size = boolArray.length;
        boolean b = boolArray[0];
        for (int x = 1; x < size; x++) {
            b = b && boolArray[x];
        }
        return b;
    }
}
