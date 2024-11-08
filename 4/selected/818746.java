package com.laoer.bbscs.comm;

import java.awt.*;
import java.io.*;
import java.awt.image.*;
import com.sun.image.codec.jpeg.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.io.*;

/**
 * <p>
 * Title: Tianyi BBS
 * </p>
 *
 * <p>
 * Description: BBSCS
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 *
 * <p>
 * Company: Laoer.com
 * </p>
 *
 * @author Gong Tianyi
 * @version 7.0
 */
public class ImgUtil {

    private static final Log logger = LogFactory.getLog(ImgUtil.class);

    public ImgUtil() {
    }

    public static void reduceImg(String imgsrc, String imgdist, int widthdist, int heightdist, int benchmark) {
        try {
            File srcfile = new File(imgsrc);
            if (!srcfile.exists()) {
                return;
            }
            Image src = javax.imageio.ImageIO.read(srcfile);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            if (width <= widthdist && height <= heightdist) {
                FileUtils.copyFile(new File(imgsrc), new File(imgdist));
                return;
            }
            float wh = (float) width / (float) height;
            if (benchmark == 0) {
                if (wh > 1) {
                    float tmp_heigth = (float) widthdist / wh;
                    BufferedImage tag = new BufferedImage(widthdist, (int) tmp_heigth, BufferedImage.TYPE_INT_RGB);
                    tag.getGraphics().drawImage(src, 0, 0, widthdist, (int) tmp_heigth, null);
                    FileOutputStream out = new FileOutputStream(imgdist);
                    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                    encoder.encode(tag);
                    out.close();
                } else {
                    float tmp_width = (float) heightdist * wh;
                    BufferedImage tag = new BufferedImage((int) tmp_width, heightdist, BufferedImage.TYPE_INT_RGB);
                    tag.getGraphics().drawImage(src, 0, 0, (int) tmp_width, heightdist, null);
                    FileOutputStream out = new FileOutputStream(imgdist);
                    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                    encoder.encode(tag);
                    out.close();
                }
            }
            if (benchmark == 1) {
                float tmp_heigth = (float) widthdist / wh;
                BufferedImage tag = new BufferedImage(widthdist, (int) tmp_heigth, BufferedImage.TYPE_INT_RGB);
                tag.getGraphics().drawImage(src, 0, 0, widthdist, (int) tmp_heigth, null);
                FileOutputStream out = new FileOutputStream(imgdist);
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                encoder.encode(tag);
                out.close();
            }
            if (benchmark == 2) {
                float tmp_width = (float) heightdist * wh;
                BufferedImage tag = new BufferedImage((int) tmp_width, heightdist, BufferedImage.TYPE_INT_RGB);
                tag.getGraphics().drawImage(src, 0, 0, (int) tmp_width, heightdist, null);
                FileOutputStream out = new FileOutputStream(imgdist);
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                encoder.encode(tag);
                out.close();
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
}
