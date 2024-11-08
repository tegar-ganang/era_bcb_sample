package io;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * ImageJ plugin to read a file in Gordon Kindlmann's NRRD 
 * or 'nearly raw raster data' format, a simple format which handles
 * coordinate systems and data types in a very general way.
 * See <A HREF="http://teem.sourceforge.net/nrrd">http://teem.sourceforge.net/nrrd</A>
 * and <A HREF="http://flybrain.stanford.edu/nrrd">http://flybrain.stanford.edu/nrrd</A>
 */
public class Nrrd_Reader extends ImagePlus implements PlugIn {

    public final String uint8Types = "uchar, unsigned char, uint8, uint8_t";

    public final String int16Types = "short, short int, signed short, signed short int, int16, int16_t";

    public final String uint16Types = "ushort, unsigned short, unsigned short int, uint16, uint16_t";

    public final String int32Types = "int, signed int, int32, int32_t";

    public final String uint32Types = "uint, unsigned int, uint32, uint32_t";

    private String notes = "";

    private boolean detachedHeader = false;

    public String headerPath = null;

    public String imagePath = null;

    public String imageName = null;

    public void run(String arg) {
        String directory = "", name = arg;
        if ((arg == null) || (arg == "")) {
            OpenDialog od = new OpenDialog("Load Nrrd (or .nhdr) File...", arg);
            name = od.getFileName();
            if (name == null) return;
            directory = od.getDirectory();
        } else {
            File dest = new File(arg);
            directory = dest.getParent();
            name = dest.getName();
        }
        ImagePlus imp = load(directory, name);
        if (imp == null) return;
        if (imageName != null) {
            setStack(imageName, imp.getStack());
        } else {
            setStack(name, imp.getStack());
        }
        if (!notes.equals("")) setProperty("Info", notes);
        copyScale(imp);
        if (arg.equals("")) show();
    }

    public ImagePlus load(String directory, String fileName) {
        if (!directory.endsWith(File.separator)) directory += File.separator;
        if ((fileName == null) || (fileName == "")) return null;
        NrrdFileInfo fi;
        try {
            fi = getHeaderInfo(directory, fileName);
        } catch (IOException e) {
            IJ.write("readHeader: " + e.getMessage());
            return null;
        }
        if (IJ.debugMode) IJ.log("fi:" + fi);
        IJ.showStatus("Loading Nrrd File: " + directory + fileName);
        ImagePlus imp;
        FlexibleFileOpener gzfo;
        if (fi.encoding.equals("gzip") && detachedHeader) {
            gzfo = new FlexibleFileOpener(fi, FlexibleFileOpener.GZIP);
            imp = gzfo.open(false);
        } else if (fi.encoding.equals("gzip")) {
            long preOffset = fi.longOffset > 0 ? fi.longOffset : fi.offset;
            fi.offset = 0;
            fi.longOffset = 0;
            gzfo = new FlexibleFileOpener(fi, FlexibleFileOpener.GZIP, preOffset);
            if (IJ.debugMode) IJ.log("gzfo:" + gzfo);
            imp = gzfo.open(false);
        } else {
            FileOpener fo = new FileOpener(fi);
            imp = fo.open(false);
        }
        if (imp == null) return null;
        Calibration cal = imp.getCalibration();
        Calibration spatialCal = this.getCalibration();
        cal.pixelWidth = spatialCal.pixelWidth;
        cal.pixelHeight = spatialCal.pixelHeight;
        cal.pixelDepth = spatialCal.pixelDepth;
        cal.setUnit(spatialCal.getUnit());
        cal.xOrigin = spatialCal.xOrigin;
        cal.yOrigin = spatialCal.yOrigin;
        cal.zOrigin = spatialCal.zOrigin;
        imp.setCalibration(cal);
        return imp;
    }

    public NrrdFileInfo getHeaderInfo(String directory, String fileName) throws IOException {
        if (IJ.debugMode) IJ.log("Entering Nrrd_Reader.readHeader():");
        NrrdFileInfo fi = new NrrdFileInfo();
        fi.directory = directory;
        fi.fileName = fileName;
        Calibration spatialCal = this.getCalibration();
        RandomAccessFile input = new RandomAccessFile(fi.directory + fi.fileName, "r");
        String thisLine, noteType, noteValue, noteValuelc;
        fi.fileType = FileInfo.GRAY8;
        spatialCal.setUnit("micron");
        fi.fileFormat = FileInfo.RAW;
        fi.nImages = 1;
        while (true) {
            thisLine = input.readLine();
            if (thisLine == null || thisLine.equals("")) {
                if (!detachedHeader) fi.longOffset = input.getFilePointer();
                break;
            }
            notes += thisLine + "\n";
            if (thisLine.indexOf("#") == 0) continue;
            noteType = getFieldPart(thisLine, 0).toLowerCase();
            noteValue = getFieldPart(thisLine, 1);
            noteValuelc = noteValue.toLowerCase();
            String firstNoteValue = getSubField(thisLine, 0);
            if (IJ.debugMode) IJ.log("NoteType:" + noteType + ", noteValue:" + noteValue);
            if (noteType.equals("data file") || noteType.equals("datafile")) {
                if (firstNoteValue.equals("LIST")) {
                    throw new IOException("Nrrd_Reader: not yet able to handle datafile: LIST specifications");
                } else if (!getSubField(thisLine, 1).equals("")) {
                    throw new IOException("Nrrd_Reader: not yet able to handle datafile: sprintf file specifications");
                } else {
                    File imageFile;
                    if (noteValue.indexOf("/") == 0) {
                        imageFile = new File(noteValue);
                    } else {
                        imageFile = new File(fi.directory, noteValue);
                    }
                    if (imageFile.exists()) {
                        fi.directory = imageFile.getParent();
                        fi.fileName = imageFile.getName();
                        imagePath = imageFile.getPath();
                        detachedHeader = true;
                    } else {
                        throw new IOException("Unable to find image file =" + imageFile.getPath());
                    }
                }
            }
            if (noteType.equals("dimension")) {
                fi.dimension = Integer.valueOf(noteValue).intValue();
                if (fi.dimension > 3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
            }
            if (noteType.equals("sizes")) {
                fi.sizes = new int[fi.dimension];
                for (int i = 0; i < fi.dimension; i++) {
                    fi.sizes[i] = Integer.valueOf(getSubField(thisLine, i)).intValue();
                    if (i == 0) fi.width = fi.sizes[0];
                    if (i == 1) fi.height = fi.sizes[1];
                    if (i == 2) fi.nImages = fi.sizes[2];
                }
            }
            if (noteType.equals("units")) spatialCal.setUnit(firstNoteValue);
            if (noteType.equals("space units")) spatialCal.setUnit(firstNoteValue);
            if (noteType.equals("spacings")) {
                double[] spacings = new double[fi.dimension];
                for (int i = 0; i < fi.dimension; i++) {
                    spacings[i] = Double.valueOf(getSubField(thisLine, i)).doubleValue();
                    if (i == 0) spatialCal.pixelWidth = spacings[0];
                    if (i == 1) spatialCal.pixelHeight = spacings[1];
                    if (i == 2) spatialCal.pixelDepth = spacings[2];
                }
            }
            if (noteType.equals("space dimension")) {
                fi.spaceDims = Integer.valueOf(noteValue).intValue();
                if (fi.spaceDims != fi.dimension) throw new IOException("Nrrd_Reader: Don't yet know how to handle image dimension!=space dimension!");
            }
            if (noteType.equals("space")) {
                fi.setSpace(noteValue);
                if (fi.spaceDims > 3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
            }
            if (noteType.equals("space directions")) {
                double[][] spaceDirs = new double[fi.spaceDims][fi.spaceDims];
                for (int i = 0, dim = 0; i < fi.dimension; i++) {
                    double[] vec = getVector(noteValue, i);
                    if (vec == null) continue;
                    for (int j = 0; j < fi.spaceDims; j++) {
                        spaceDirs[dim][j] = vec[j];
                    }
                    dim++;
                }
                fi.setSpaceDirs(spaceDirs);
            }
            if (noteType.equals("space origin")) {
                fi.setSpaceOrigin(getVector(thisLine, 0));
            }
            if (noteType.equals("centers") || noteType.equals("centerings")) {
                fi.centers = new String[fi.dimension];
                for (int i = 0; i < fi.dimension; i++) {
                    fi.centers[i] = getSubField(thisLine, i);
                }
            }
            if (noteType.equals("axis mins") || noteType.equals("axismins")) {
                double[] axismins = new double[fi.dimension];
                for (int i = 0; i < fi.dimension; i++) {
                    axismins[i] = Double.valueOf(getSubField(thisLine, i)).doubleValue();
                    if (i == 0) spatialCal.xOrigin = axismins[0];
                    if (i == 1) spatialCal.yOrigin = axismins[1];
                    if (i == 2) spatialCal.zOrigin = axismins[2];
                }
            }
            if (noteType.equals("type")) {
                if (uint8Types.indexOf(noteValuelc) >= 0) {
                    fi.fileType = FileInfo.GRAY8;
                } else if (uint16Types.indexOf(noteValuelc) >= 0) {
                    fi.fileType = FileInfo.GRAY16_UNSIGNED;
                } else if (int16Types.indexOf(noteValuelc) >= 0) {
                    fi.fileType = FileInfo.GRAY16_SIGNED;
                } else if (uint32Types.indexOf(noteValuelc) >= 0) {
                    fi.fileType = FileInfo.GRAY32_UNSIGNED;
                } else if (int32Types.indexOf(noteValuelc) >= 0) {
                    fi.fileType = FileInfo.GRAY32_INT;
                } else if (noteValuelc.equals("float")) {
                    fi.fileType = FileInfo.GRAY32_FLOAT;
                } else if (noteValuelc.equals("double")) {
                    fi.fileType = FileInfo.GRAY64_FLOAT;
                } else {
                    throw new IOException("Unimplemented data type =" + noteValue);
                }
            }
            if (noteType.equals("byte skip") || noteType.equals("byteskip")) fi.longOffset = Long.valueOf(noteValue).longValue();
            if (noteType.equals("endian")) {
                if (noteValuelc.equals("little")) {
                    fi.intelByteOrder = true;
                } else {
                    fi.intelByteOrder = false;
                }
            }
            if (noteType.equals("encoding")) {
                if (noteValuelc.equals("gz")) noteValuelc = "gzip";
                fi.encoding = noteValuelc;
            }
        }
        if (fi.spaceDims > 0) {
            spatialCal = fi.updateCalibration(spatialCal);
        } else {
            if (spatialCal.pixelWidth != 0) spatialCal.xOrigin = spatialCal.xOrigin / spatialCal.pixelWidth;
            if (spatialCal.pixelHeight != 0) spatialCal.yOrigin = spatialCal.yOrigin / spatialCal.pixelHeight;
            if (spatialCal.pixelDepth != 0) spatialCal.zOrigin = spatialCal.zOrigin / spatialCal.pixelDepth;
            if (fi.centers != null) {
                if (fi.centers[0].equals("cell")) spatialCal.xOrigin -= spatialCal.pixelWidth / 2;
                if (fi.centers[1].equals("cell")) spatialCal.yOrigin -= spatialCal.pixelHeight / 2;
                if (fi.dimension > 2 && fi.centers[2].equals("cell")) spatialCal.zOrigin -= spatialCal.pixelDepth / 2;
            }
        }
        if (!detachedHeader) fi.longOffset = input.getFilePointer();
        input.close();
        this.setCalibration(spatialCal);
        return (fi);
    }

    String getFieldPart(String str, int fieldIndex) {
        str = str.trim();
        String[] fieldParts = str.split(":\\s+");
        if (fieldParts.length < 2) return (fieldParts[0]);
        if (fieldIndex == 0) return fieldParts[0]; else return fieldParts[1];
    }

    String getSubField(String str, int fieldIndex) {
        String fieldDescriptor = getFieldPart(str, 1);
        fieldDescriptor = fieldDescriptor.trim();
        if (IJ.debugMode) IJ.log("fieldDescriptor = " + fieldDescriptor + "; fieldIndex = " + fieldIndex);
        String[] fields_values = fieldDescriptor.split("\\s+");
        if (fieldIndex >= fields_values.length) {
            return "";
        } else {
            String rval = fields_values[fieldIndex];
            if (rval.startsWith("\"")) rval = rval.substring(1);
            if (rval.endsWith("\"")) rval = rval.substring(0, rval.length() - 1);
            return rval;
        }
    }

    double[] getVector(String str, int vecIndex) {
        String fieldDescriptor = getFieldPart(str, 1);
        fieldDescriptor = fieldDescriptor.trim();
        if (IJ.debugMode) IJ.log("fieldDescriptor = " + fieldDescriptor);
        fieldDescriptor.replace("none", "(none)");
        String[] fields_values = fieldDescriptor.split("\\)\\s*\\(");
        if (vecIndex >= fields_values.length) return null;
        String svec = fields_values[vecIndex];
        svec = svec.trim();
        svec = svec.replaceAll("[()]", "");
        if (svec.equals("") || svec.equals("none")) return null;
        String[] svals = svec.split("\\s*,\\s*");
        double[] rvals = new double[svals.length];
        for (int i = 0; i < svals.length; i++) {
            rvals[i] = (new Double(svals[i])).doubleValue();
        }
        return rvals;
    }
}
