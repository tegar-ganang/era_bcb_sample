package com.microfly.job.html;

import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.image.codec.jpeg.JPEGCodec;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * ImageHelper
 *   �����ͼ�ε�ת��
 *
 * Description: a new publishing system
 * Copyright (c) 2007
 *
 * @author jialin
 * @version 1.0
 */
public class ImageHelper {

    private File image = null;

    public ImageHelper(File img) {
        this.image = img;
    }

    public void ScaleTo(File dest, int width, int height) throws IOException {
        if ((width == -1) && (height == -1)) {
            CopyTo(dest);
            return;
        }
        String suffix = GetSuffix();
        if (!(suffix.equalsIgnoreCase(".JPG") || suffix.equalsIgnoreCase(".JPEG") || suffix.equalsIgnoreCase(".BMP") || suffix.equalsIgnoreCase(".GIF") || suffix.equalsIgnoreCase(".PNG") || suffix.equalsIgnoreCase(".TIF"))) {
            CopyTo(dest);
            return;
        }
        java.io.FileOutputStream out_newimg = null;
        try {
            BufferedImage src = javax.imageio.ImageIO.read(image);
            int i_old_w = src.getWidth(null);
            int i_old_h = src.getHeight(null);
            float f_old_w = (new Integer(i_old_w)).floatValue();
            float f_old_h = (new Integer(i_old_h)).floatValue();
            float fRatio = -1;
            if (width == -1) {
                fRatio = (new Integer(height)).floatValue() / f_old_h;
            } else if (height == -1) {
                fRatio = (new Integer(width)).floatValue() / f_old_w;
            }
            int new_w = width;
            int new_h = height;
            if (fRatio != -1) {
                new_w = Math.round(i_old_w * fRatio);
                new_h = Math.round(i_old_h * fRatio);
            }
            BufferedImage newimg = new BufferedImage(new_w, new_h, BufferedImage.TYPE_INT_RGB);
            newimg.getGraphics().drawImage(src, 0, 0, new_w, new_h, null);
            out_newimg = new java.io.FileOutputStream(dest);
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out_newimg);
            encoder.encode(newimg);
        } finally {
            if (out_newimg != null) try {
                out_newimg.close();
            } catch (Exception e) {
            }
        }
    }

    private String GetSuffix() {
        String filename = image.getName();
        int pos = filename.lastIndexOf(".");
        if (pos == -1) return "";
        return filename.substring(pos);
    }

    private void CopyTo(File dest) throws IOException {
        FileReader in = null;
        FileWriter out = null;
        int c;
        try {
            in = new FileReader(image);
            out = new FileWriter(dest);
            while ((c = in.read()) != -1) out.write(c);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
            if (out != null) try {
                out.close();
            } catch (Exception e) {
            }
        }
    }
}
