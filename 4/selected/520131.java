package Bruker;

import java.util.*;
import ij.*;
import ij.gui.GenericDialog;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.process.*;

/**
 * 
* @author (C) 2010 Dimiter Prodanov
* 		   IMEC
* 
* @contents This plugin reshapes images based on their spatial calibration.
* 
* @license This library is free software; you can redistribute it and/or
*      modify it under the terms of the GNU Lesser General Public
*      License as published by the Free Software Foundation; either
*      version 2.1 of the License, or (at your option) any later version.
*
*      This library is distributed in the hope that it will be useful,
*      but WITHOUT ANY WARRANTY; without even the implied warranty of
*      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*       Lesser General Public License for more details.
*
*      You should have received a copy of the GNU Lesser General Public
*      License along with this library; if not, write to the Free Software
*      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
public class Resizer implements PlugInFilter {

    private ImagePlus outimp = null;

    ImagePlus inimp = null;

    boolean isStack = false;

    private static final String AX = "AX", AY = "AY", AZ = "AZ", RCHOICE = "R_choice", DOZ = "DO_Z";

    private static float ax = (float) Prefs.getDouble(AX, 1.0);

    private static float ay = (float) Prefs.getDouble(AY, 1.0);

    private static float az = (float) Prefs.getDouble(AZ, 1.0);

    private static int choice = (int) Prefs.getInt(RCHOICE, 1);

    private static boolean calZ = (boolean) Prefs.getBoolean(DOZ, false);

    private static String[] items = { "X", "Y" };

    private String what = "";

    private String title = "";

    private Calibration cal = null;

    double magnification = 1.0;

    int height = -1;

    int width = -1;

    int depth = -1;

    int nframes = -1;

    /**
	 * 
	 */
    public Resizer() {
    }

    public Resizer(ImagePlus imp, int direction, boolean calibrateZ) {
        setInputs(imp);
        if (direction < 0) direction = 0;
        if (direction > 1) direction = 1;
        choice = direction;
        calZ = calibrateZ;
        calibrate(inimp, calZ);
    }

    @Override
    public void run(ImageProcessor ip) {
        calibrate(inimp, calZ);
        if (!isUniform) {
            outimp = doResize();
            if (outimp != null) {
                outimp.setTitle(title + "_reshaped" + what);
                outimp.show();
            }
        }
    }

    private boolean isUniform = false;

    public boolean isUniform() {
        return isUniform;
    }

    private void calibrate(ImagePlus imp, boolean doZ) {
        Calibration cal2 = imp.getCalibration();
        height = imp.getHeight();
        width = imp.getWidth();
        isUniform = (cal2.pixelWidth == cal2.pixelHeight);
        if (cal2 != null) {
            double calx = cal2.pixelWidth;
            double caly = cal2.pixelHeight;
            double calz = cal2.pixelDepth;
            switch(choice) {
                case 1:
                    ax = 1.0f;
                    ay = (float) (caly / calx);
                    az = (float) (calz / calx);
                    break;
                case 0:
                    ax = (float) (calx / caly);
                    ay = 1.0f;
                    az = (float) (calz / caly);
                    break;
            }
            Log("ax: " + ax + " ay: " + ay + " az: " + az);
            cal = cal2.copy();
            cal.pixelWidth /= ax;
            cal.pixelHeight /= ay;
            if (doZ) cal.pixelDepth /= az;
        }
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        setInputs(imp);
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        if (IJ.versionLessThan("1.41)") || !showDialog(imp)) {
            return DONE;
        } else {
            return DOES_ALL + NO_CHANGES;
        }
    }

    /**
	 * @param imp
	 */
    public void setInputs(ImagePlus imp) {
        inimp = imp;
        title = inimp.getTitle();
        int i = title.indexOf(".");
        if (i > -1) title = title.substring(0, i);
        depth = inimp.getNSlices();
        nframes = inimp.getNFrames();
        if (depth > 0) {
            isStack = true;
        }
    }

    public ImagePlus doResize() {
        ImagePlus aimp = null;
        int nwidth = this.width;
        int nheight = this.height;
        nwidth = (int) Math.round(nwidth * ax);
        nheight = (int) Math.round(nheight * ay);
        int size = depth * nframes;
        if (isStack) {
            ImageStack iso = new ImageStack(nwidth, nheight, size);
            IJ.log("stack width " + nwidth + " stack height " + nheight + " stack depth " + depth + " frames " + nframes);
            int cnt = 1;
            for (int f = 1; f <= nframes; f++) {
                for (int s = 1; s <= depth; s++) {
                    inimp.setPositionWithoutUpdate(1, s, f);
                    final ImageProcessor ip2 = inimp.getChannelProcessor().duplicate();
                    ip2.setInterpolate(true);
                    ImageProcessor ip3 = ip2.resize(nwidth, nheight);
                    iso.setPixels(ip3.getPixels(), cnt);
                    cnt++;
                }
            }
            iso.setColorModel(inimp.getChannelProcessor().getColorModel());
            aimp = new ImagePlus("resized stack", iso);
            aimp.setDimensions(1, depth, nframes);
            aimp.setOpenAsHyperStack(inimp.isHyperStack());
            inimp.setPositionWithoutUpdate(1, 1, 1);
        } else {
            ImageProcessor ip2 = inimp.getProcessor().duplicate();
            ip2.setInterpolate(true);
            ip2.scale(nwidth, nheight);
            ImageProcessor ip3 = ip2.resize(nwidth, nheight);
            ip3.setColorModel(ip2.getColorModel());
            aimp = new ImagePlus("resized", ip3);
        }
        aimp.resetDisplayRange();
        aimp.setCalibration(cal);
        return aimp;
    }

    boolean showDialog(ImagePlus imp) {
        if (imp == null) return true;
        GenericDialog gd = new GenericDialog("Parameters");
        gd.addChoice("Resize by", items, items[choice]);
        gd.addCheckbox("Recalibrate Z: ", calZ);
        gd.showDialog();
        choice = gd.getNextChoiceIndex();
        calZ = gd.getNextBoolean();
        what = "_" + items[choice];
        if (gd.wasCanceled()) return false;
        return true;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            System.setProperty("plugins.dir", args[0]);
            new ImageJ();
        } catch (Exception ex) {
            IJ.log("plugins.dir misspecified");
        }
    }

    /**
     * 
     */
    void showAbout() {
        IJ.showMessage("About ImageReshape...", "This plugin reshapes images and stacks");
    }

    private static boolean debug = true;

    public static void Log(String astr) {
        if (debug) IJ.log(astr);
    }

    private static void savePreferences(Properties prefs) {
        prefs.put(AX, Double.toString(ax));
        prefs.put(AY, Double.toString(ay));
        prefs.put(AZ, Double.toString(az));
        prefs.put(RCHOICE, Double.toString(choice));
        prefs.put(DOZ, Boolean.toString(calZ));
    }
}
