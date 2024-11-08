package hdr_plugin.calibration.ZMatrix;

import hdr_plugin.Exceptions.TypeNotSupportedException;
import hdr_plugin.helper.ImageJTools;
import ij.ImagePlus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Heidrich
 */
public class RandomZMatrixBuilder implements ZMatrixBuilder {

    public static final Logger log = Logger.getLogger(RandomZMatrixBuilder.class.getName());

    private ImagePlus imp;

    private int noOfImagesP;

    private int noOfPixelsN;

    private int noOfChannels;

    private int imgWidth;

    private int imgHeight;

    private Random rnd = new Random();

    public RandomZMatrixBuilder(ImagePlus imp, int noOfPixelsN, int noOfImagesP) {
        this.imp = imp;
        this.noOfPixelsN = noOfPixelsN;
        this.noOfChannels = imp.getChannelProcessor().getNChannels();
        this.noOfImagesP = noOfImagesP;
        this.imgHeight = imp.getHeight();
        this.imgWidth = imp.getWidth();
    }

    public int[][][] getZ() throws TypeNotSupportedException {
        HashSet<Integer> pixtemp = new HashSet<Integer>();
        log.log(Level.FINE, "Selecting random pixels - Start");
        while (pixtemp.size() < (noOfPixelsN)) {
            pixtemp.add(getNextRandom());
        }
        log.log(Level.FINE, "Selecting random pixels - End");
        ArrayList<Integer> pixels = new ArrayList<Integer>(pixtemp);
        int[][][] Z = new int[noOfChannels][noOfPixelsN][noOfImagesP];
        log.log(Level.FINE, "Filling Z-Matrix - Start");
        for (int i = 0; i < Z.length; i++) {
            for (int j = 0; j < Z[i].length; j++) {
                for (int k = 0; k < Z[i][j].length; k++) {
                    Z[i][j][k] = ImageJTools.getPixelValue(imp.getImageStack().getPixels(k + 1), pixels.get(j), imp.getType(), i);
                }
            }
        }
        log.log(Level.FINE, "Filling Z-Matrix - End");
        return Z;
    }

    private int getNextRandom() {
        return rnd.nextInt(imgWidth * imgHeight);
    }
}
