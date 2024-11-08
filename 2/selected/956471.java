package net.simpleframework.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ImageUtils {

    public static final String code = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static Random random = new Random();

    public static Color getRandColor(int fc, int bc) {
        fc = Math.min(fc, 255);
        bc = Math.min(bc, 255);
        final int r = fc + random.nextInt(bc - fc);
        final int g = fc + random.nextInt(bc - fc);
        final int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }

    public static String genCode(final OutputStream outputStream) throws IOException {
        return genCode(90, 38, outputStream);
    }

    public static String genCode(final int width, final int height, final OutputStream outputStream) throws IOException {
        final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = bi.createGraphics();
        final GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, width, height, getRandColor(120, 200), false);
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < 50; i++) {
            g.setColor(getRandColor(210, 220));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        final Font font = new Font("comic sans ms", Font.BOLD, 24);
        g.setFont(font);
        final FontMetrics metrics = g.getFontMetrics();
        int stringWidth = 0;
        final char[] text = new char[4];
        final int[] textWidth = new int[4];
        for (int i = 0; i < 4; i++) {
            final int j = random.nextInt(code.length());
            text[i] = code.charAt(j);
            textWidth[i] = metrics.charWidth(text[i]);
            stringWidth += textWidth[i];
        }
        int posX = (width - stringWidth) / 2;
        final int posY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        for (int i = 0; i < text.length; i++) {
            g.setColor(getRandColor(50, 120));
            g.drawString(ConvertUtils.toString(text[i]), posX, posY);
            posX += textWidth[i];
        }
        ImageIO.write(bi, "png", outputStream);
        return ConvertUtils.toString(text);
    }

    public static void thumbnail(final InputStream inputStream, final int width, final int height, final OutputStream outputStream) throws IOException {
        thumbnail(inputStream, width, height, outputStream, "png");
    }

    public static void thumbnail(final InputStream inputStream, final int width, final int height, final OutputStream outputStream, final String filetype) throws IOException {
        thumbnail(inputStream, width, height, false, outputStream, filetype);
    }

    public static void thumbnail(final InputStream inputStream, final int width, final int height, final boolean stretch, final OutputStream outputStream, final String filetype) throws IOException {
        int w, h;
        try {
            if (width == 0 && height == 0) {
                IoUtils.copyStream(inputStream, outputStream);
                return;
            }
            final BufferedImage sbi = ImageIO.read(inputStream);
            if (sbi == null) {
                return;
            }
            if (width == 0 || height == 0) {
                w = sbi.getWidth();
                h = sbi.getHeight();
            } else {
                if (!stretch) {
                    final double d = (double) width / (double) height;
                    final double d0 = (double) sbi.getWidth() / (double) sbi.getHeight();
                    if (d < d0) {
                        w = width;
                        h = (int) (width / d0);
                    } else {
                        w = (int) (height * d0);
                        h = height;
                    }
                } else {
                    w = width;
                    h = height;
                }
            }
            final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = bi.createGraphics();
            if (w != width) {
                g.drawImage(sbi, Math.abs(w - width) / 2, 0, w, h, null);
            } else if (h != height) {
                g.drawImage(sbi, 0, Math.abs(h - height) / 2, w, h, null);
            } else {
                g.drawImage(sbi, 0, 0, w, h, null);
            }
            g.dispose();
            ImageIO.write(bi, filetype, outputStream);
        } finally {
            outputStream.close();
        }
    }

    public static boolean isImage(final URL url) throws IOException {
        final InputStream inputStream = url.openStream();
        try {
            return ImageIO.read(inputStream) != null;
        } finally {
            inputStream.close();
        }
    }
}
