package main;

import core.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.File;
import java.awt.*;
import gui.*;
import utility.*;

public class Maincolor {

    public static void main(String argv[]) {
        ShowFrame frameRed = new ShowFrame();
        ShowFrame frameGreen = new ShowFrame();
        ShowFrame frameBlue = new ShowFrame();
        BufferedImage inputImage = null;
        BufferedImage outputImage = null;
        String inputImagePath = "images/leopard.jpg";
        int width = 64;
        int height = 64;
        int intorno = 8;
        outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        frameRed.setVisible(true);
        frameGreen.setVisible(true);
        frameBlue.setVisible(true);
        int r = 0;
        Color auxColor;
        for (int i = 0; i < width; i++) for (int j = 0; j < height; j++) {
            r = (int) (Math.random() * 256);
            auxColor = new Color(r, r, r);
            outputImage.setRGB(i, j, auxColor.getRGB());
        }
        try {
            inputImage = ImageIO.read(new File(inputImagePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        BufferedImage imgR = Utility.getChannel(Utility.RED_CH, inputImage);
        BufferedImage imgG = Utility.getChannel(Utility.GREEN_CH, inputImage);
        BufferedImage imgB = Utility.getChannel(Utility.BLUE_CH, inputImage);
        frameRed.setImage(imgR);
        frameGreen.setImage(imgG);
        frameBlue.setImage(imgB);
        BufferedImage rImg = runSynthetizer(Utility.RED_CH, imgR, outputImage);
        BufferedImage gImg = runSynthetizer(Utility.GREEN_CH, imgG, outputImage);
        BufferedImage bImg = runSynthetizer(Utility.BLUE_CH, imgB, outputImage);
        try {
            Utility.writeImageToFile(Utility.mergeChannel(rImg, gImg, bImg), "images/", "output", "jpg");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static BufferedImage runSynthetizer(int channel, BufferedImage inputImage, BufferedImage outputImage) {
        TextureSynthetizer ts = null;
        int intorno = 8;
        ts = new TextureSynthetizer(3, intorno, inputImage, outputImage);
        ts.synthetize();
        outputImage = getScaledInstance(outputImage, 128, 128, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, false);
        intorno = 16;
        for (int i = 0; i < 2; i++) {
            ts = new TextureSynthetizer(3, intorno, inputImage, outputImage);
            ts.synthetize();
            intorno /= 2;
            if (intorno % 2 == 0) intorno++;
            outputImage = ts.getOutputImage();
            ts.dispose();
        }
        outputImage = getScaledInstance(outputImage, 256, 256, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, false);
        intorno = 32;
        for (int i = 0; i < 3; i++) {
            ts = new TextureSynthetizer(3, intorno, inputImage, outputImage);
            ts.synthetize();
            intorno /= 2;
            if (intorno % 2 == 0) intorno++;
            outputImage = ts.getOutputImage();
            ts.dispose();
        }
        return ts.getOutputImage();
    }

    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }
            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while (w != targetWidth || h != targetHeight);
        return ret;
    }
}
