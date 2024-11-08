package com.moko.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;

/**
 * @author li.li
 */
public class ImageUtils {

    private static final Logger log = Logger.getLogger(ImageUtils.class);

    private static final String LANG_OPTION = "-l";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String LANG = "eng";

    /** 图像二值化 */
    public static boolean binaryzation(String from) {
        try {
            FileInputStream fin = new FileInputStream(from);
            BufferedImage image = ImageIO.read(fin);
            int iw = image.getWidth();
            int ih = image.getHeight();
            int[] pixels = new int[iw * ih];
            PixelGrabber pg = new PixelGrabber(image.getSource(), 0, 0, iw, ih, pixels, 0, iw);
            try {
                pg.grabPixels();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            int grey = 100;
            ColorModel cm = ColorModel.getRGBdefault();
            for (int i = 0; i < iw * ih; i++) {
                int red, green, blue;
                int alpha = cm.getAlpha(pixels[i]);
                if (cm.getRed(pixels[i]) > grey) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (cm.getGreen(pixels[i]) > grey) {
                    green = 255;
                } else {
                    green = 0;
                }
                if (cm.getBlue(pixels[i]) > grey) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                pixels[i] = alpha << 24 | red << 16 | green << 8 | blue;
            }
            Image tempImg = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(iw, ih, pixels, 0, iw));
            image = new BufferedImage(tempImg.getWidth(null), tempImg.getHeight(null), BufferedImage.TYPE_INT_BGR);
            image.createGraphics().drawImage(tempImg, 0, 0, null);
            ImageIO.write(image, "jpg", new File(from));
            return true;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /** 转换为黑白灰度图 */
    public static void grayFilter(String image) {
        ImageOutputStream output = null;
        try {
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            output = new FileImageOutputStream(new File(image));
            ImageIO.write(op.filter(ImageIO.read(new File(image)), null), "jpg", output);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                if (output != null) {
                    output = null;
                }
            }
        }
    }

    /**
	 * 转JPEG
	 */
    public static boolean convertToJPEG(String src, String dest) {
        OutputStream os = null;
        try {
            RenderedOp srcOP = JAI.create("fileload", src);
            os = new FileOutputStream(dest);
            ImageEncoder ie = ImageCodec.createImageEncoder("JPEG", os, new JPEGEncodeParam());
            ie.encode(srcOP);
            return true;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                os = null;
            }
        }
    }

    public static String getImagePrefix(String image) {
        return StringUtils.substring(image, 0, image.lastIndexOf("."));
    }

    /**
	 * 处理验证码
	 */
    public static void handler(String captchaFilePath) throws Throwable {
        binaryzation(captchaFilePath);
        grayFilter(captchaFilePath);
    }

    /**
	 * 识别验证码
	 */
    public static String recognize(String captchaFilePath) throws Throwable {
        File captchaFile = new File(captchaFilePath);
        log.info(captchaFile.getAbsoluteFile());
        File outputFile = new File(captchaFile.getParentFile(), "output");
        StringBuffer strB = new StringBuffer();
        List<String> cmd = new ArrayList<String>();
        cmd.add("tesseract");
        cmd.add("");
        cmd.add(outputFile.getName());
        cmd.add(LANG_OPTION);
        cmd.add(LANG);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(captchaFile.getParentFile());
        cmd.set(1, captchaFile.getName());
        pb.command(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int w = process.waitFor();
        log.info("Exit value = " + w);
        if (w == 0) {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile.getAbsolutePath() + ".txt"), "UTF-8"));
            String str;
            while ((str = in.readLine()) != null) strB.append(str).append(LINE_SEPARATOR);
            in.close();
        } else {
            String msg;
            switch(w) {
                case 1:
                    msg = "Errors accessing files. There may be spaces in your image's filename.";
                    break;
                case 29:
                    msg = "Cannot recognize the image or its selected region.";
                    break;
                case 31:
                    msg = "Unsupported image format.";
                    break;
                default:
                    msg = "Errors occurred.";
            }
            captchaFile.delete();
            throw new RuntimeException(msg);
        }
        new File(outputFile.getAbsolutePath() + ".txt").delete();
        log.info("图像识别结果:" + strB);
        return strB.toString().trim();
    }

    /**
	 * 判断文件格式
	 */
    public static String getFormatName(Object object) throws Throwable {
        ImageInputStream iis = ImageIO.createImageInputStream(object);
        Iterator<ImageReader> iterator = ImageIO.getImageReaders(iis);
        while (iterator.hasNext()) {
            ImageReader reader = (ImageReader) iterator.next();
            return reader.getFormatName();
        }
        return null;
    }
}
