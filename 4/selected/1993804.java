package org.hexahedron.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

public class TerragenSkyboxDownfiller {

    int skipUp = 3;

    public TerragenSkyboxDownfiller(int skipUp) {
        super();
        this.skipUp = skipUp;
    }

    public void downfill(BufferedImage image) {
        BufferedImage bi = image;
        for (int x = 0; x < bi.getWidth(); x++) {
            int firstBlankY = -1;
            for (int y = 0; y < bi.getHeight(); y++) {
                int p = bi.getRGB(x, y);
                Color c = new Color(p);
                if ((c.getRed() == 0) && (c.getGreen() == 0) && (c.getBlue() == 0) && (firstBlankY == -1)) {
                    firstBlankY = y;
                }
            }
            if (firstBlankY >= 0) {
                int xc = x;
                if (xc == 0) xc = 1;
                if (xc == bi.getWidth() - 1) xc = bi.getWidth() - 2;
                int lastGoodColour = bi.getRGB(xc, firstBlankY - skipUp);
                for (int y = firstBlankY - skipUp; y < bi.getHeight(); y++) {
                    bi.setRGB(x, y, lastGoodColour);
                }
            }
        }
    }

    public static int averageColours(int[] colours) {
        float r = 0;
        float g = 0;
        float b = 0;
        for (int i = 0; i < colours.length; i++) {
            Color c = new Color(colours[i]);
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
        }
        float count = (float) (colours.length);
        r /= count;
        g /= count;
        b /= count;
        r = clip(r);
        g = clip(g);
        b = clip(b);
        return new Color(r, g, b).getRGB();
    }

    public static float clip(float v) {
        if (v < 0) {
            return 0;
        } else if (v > 1) {
            return 1;
        } else {
            return v;
        }
    }

    public static final void main(String[] args) {
        TerragenSkyboxDownfiller filler = new TerragenSkyboxDownfiller(4);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose images to fill");
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                File outFile = new File(f.getParent(), "d_" + f.getName());
                try {
                    BufferedImage read = ImageIO.read(f);
                    filler.downfill(read);
                    System.out.println(ImageIO.write(read, "bmp", outFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
