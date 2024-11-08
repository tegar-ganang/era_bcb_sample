package edu.ohiou.lev_neiman.jung.volume_render.ui.control.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import com.sun.opengl.util.BufferUtil;

/**
 * <p>Title: Scientific Volume Rendering</p>
 *
 * <p>Description: Lev Neiman's Summer Job</p>
 *
 * <p>Copyright: Copyright (c) 2008, Lev A. Neiman</p>
 *
 * <p>Company: Dr. Peter Jung</p>
 *
 * @author Lev A. Neiman
 * @version 1.0
 */
public class DataReader {

    public DataReader() {
    }

    /**
     * find min and max values of data and normalize it to be between values [0,1]
     * @param data FloatBuffer
     */
    public static void normalizeBuffer(FloatBuffer data) {
        float min, max;
        min = Float.MAX_VALUE;
        max = min * -1f;
        for (int i = 0; i < data.capacity(); ++i) {
            float f = data.get(i);
            if (max < f) {
                max = f;
            }
            if (min > f) {
                min = f;
            }
        }
        System.out.println("Max value found = " + Float.toString(max));
        System.out.println("Min value found = " + Float.toString(min));
        float alpha = -1f / (min - max);
        float beta = 1f - alpha * max;
        for (int i = 0; i < data.capacity(); ++i) {
            data.put(i, alpha * data.get(i) + beta);
        }
    }

    public static float[] getMinMax(File file) {
        float[] ret = { Float.MAX_VALUE, Float.MAX_VALUE * -1f };
        try {
            long s = file.length();
            FileChannel in = new FileInputStream(file).getChannel();
            ByteBuffer buffa = BufferUtil.newByteBuffer((int) s);
            in.read(buffa);
            buffa.rewind();
            FloatBuffer data = buffa.asFloatBuffer();
            for (int i = 0; i < data.capacity(); ++i) {
                float f = data.get(i);
                if (ret[0] > f) {
                    ret[0] = f;
                }
                if (ret[1] < f) {
                    ret[1] = f;
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return ret;
    }

    public static FloatBuffer readFileIntoBuffer(File file) throws FileNotFoundException, IOException {
        long s = file.length();
        System.out.println("Volume file size = " + Long.toString(s));
        FileChannel in = new FileInputStream(file).getChannel();
        ByteBuffer buffa = BufferUtil.newByteBuffer((int) s);
        in.read(buffa);
        buffa.rewind();
        FloatBuffer ret = buffa.asFloatBuffer();
        normalizeBuffer(ret);
        return ret;
    }

    public static float[] computeMeanDev(FloatBuffer f) {
        float[] ret = { 0f, 0f };
        int n = f.capacity();
        double sum = 0;
        double dev = 0f;
        for (int i = 0; i < n; ++i) {
            sum += f.get(i);
        }
        double mean = sum / (double) n;
        for (int i = 0; i < n; ++i) {
            dev += Math.pow(f.get(i) - mean, 2);
        }
        dev = Math.sqrt(dev / (double) sum);
        ret[0] = (float) mean;
        ret[1] = (float) dev;
        return ret;
    }

    public static void main(String[] args) {
        float a = .1f;
        a *= 255;
        byte b = (byte) a;
        System.out.println(b);
        System.out.println(BufferUtil.SIZEOF_BYTE);
        System.out.println(BufferUtil.SIZEOF_FLOAT);
    }
}
