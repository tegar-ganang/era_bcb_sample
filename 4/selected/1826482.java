package net.sf.wwusmart.algorithms;

import net.sf.wwusmart.algorithms.framework.*;
import net.sf.wwusmart.helper.FileSystemOps;
import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * This is a plugin that was once written as a wrapper for a shape matching tool
 * that came as a standalone command line application.
 *
 * Plugin was written in the early days of SMART and might be slightly outdated
 * in some aspects, at some points it uses a quick-and-dirty solution.
 * But it may roughly serve as an example how one could integrate standalone
 * applications as plugins to SMART.
 *
 * @author thilo
 */
public class SegMatchExeWrapper implements JavaAlgorithmImplementation {

    File pluginDirectory = null;

    File matchExecutable = null;

    File segmentExecutable = null;

    File matchParameters = null;

    File tmpDir = null;

    Parameter<Double> query_bbr = new Parameter<Double>(Double.class, "Parameter bbr for Query Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 0.2);

    Parameter<Integer> query_thif = new Parameter<Integer>(Integer.class, "Parameter thif for Query Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 2);

    Parameter<Integer> query_thff = new Parameter<Integer>(Integer.class, "Parameter thff for Query Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 12);

    Parameter<Double> query_mr = new Parameter<Double>(Double.class, "Merge Ratio for Query Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 1.2);

    Parameter<Double> reference_bbr = new Parameter<Double>(Double.class, "Parameter bbr for Reference Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 0.3);

    Parameter<Integer> reference_thif = new Parameter<Integer>(Integer.class, "Parameter thif for Reference Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 5);

    Parameter<Integer> reference_thff = new Parameter<Integer>(Integer.class, "Parameter thff for Reference Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 10);

    Parameter<Double> reference_mr = new Parameter<Double>(Double.class, "Merge Ratio for Reference Object", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 1.1);

    Parameter<Double> seg_bbr = new Parameter<Double>(Double.class, "Parameter bbr for Volume Segmentation", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 0.25);

    Parameter<Integer> seg_thif = new Parameter<Integer>(Integer.class, "Parameter thif for Volume Segmentation", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 5);

    Parameter<Integer> seg_thff = new Parameter<Integer>(Integer.class, "Parameter thff for Volume Segmentation", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 10);

    Parameter<Double> seg_mr = new Parameter<Double>(Double.class, "Merge Ratio for Volume Segmentation", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 1.3);

    Parameter<Integer> seg_opc = new Parameter<Integer>(Integer.class, "Parameter opc for Volume Segmentation", "See paper `Shape Segmentation and Matching with Flow Discretization', Dey, Giesen, Goswami", 20);

    List<Parameter> parameterList;

    public void initialize() {
        parameterList = new Vector<Parameter>(13);
        parameterList.add(query_bbr);
        parameterList.add(query_thif);
        parameterList.add(query_thff);
        parameterList.add(query_mr);
        parameterList.add(reference_bbr);
        parameterList.add(reference_thif);
        parameterList.add(reference_thff);
        parameterList.add(reference_mr);
        parameterList.add(seg_bbr);
        parameterList.add(seg_thif);
        parameterList.add(seg_thff);
        parameterList.add(seg_mr);
        parameterList.add(seg_opc);
        pluginDirectory = new File("plugins");
        String matchParametersFilename;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            matchExecutable = new File(pluginDirectory.getPath() + "\\seg-match\\windows\\rmp-win.exe");
            segmentExecutable = new File(pluginDirectory.getPath() + "\\seg-match\\windows\\rvol_seg-win.exe");
            matchParametersFilename = "param-list.txt";
        } else {
            matchExecutable = new File(pluginDirectory.getPath() + "/seg-match/linux/rmp-linux");
            segmentExecutable = new File(pluginDirectory.getPath() + "/seg-match/linux/rvol_seg-linux");
            matchParametersFilename = "param-list";
        }
        if (!matchExecutable.canRead()) {
            throw new RuntimeException("Cannot read `" + matchExecutable.getPath() + "', no appropriate permissions");
        }
        if (!segmentExecutable.canRead()) {
            throw new RuntimeException("Cannot read `" + segmentExecutable.getPath() + "', no appropriate permissions");
        }
        if (Float.parseFloat(System.getProperty("java.specification.version")) >= 1.6f) {
            if (!FileSystemOps.canExecute(matchExecutable)) {
                throw new RuntimeException("Cannot execute `" + matchExecutable.getPath() + "', no appropriate permissions");
            }
            if (!FileSystemOps.canExecute(segmentExecutable)) {
                throw new RuntimeException("Cannot execute `" + segmentExecutable.getPath() + "', no appropriate permissions");
            }
        }
        try {
            do {
                tmpDir = File.createTempFile("smart_seg-match_plugin", "");
                tmpDir.delete();
            } while (!tmpDir.mkdir());
            tmpDir.deleteOnExit();
            matchParameters = new File(tmpDir.getPath() + File.separator + matchParametersFilename);
            writeParamsFile();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getName() {
        return "Shape Segmentation and Matching with Flow Discretization";
    }

    public String getDescription() {
        return "This is a wrapper for the Windows and Linux executables " + "implementing an algorithm for Shape Segmentation and Matching with Flow Discretization" + "developed at the Deprtament of Computer and Information Science " + "of The Ohio State University. " + "Executable binaries should reside in eihter the `plugins\\seg-match\\windows' " + "or the `plugins/seg-match/linux' subdirectory.";
    }

    public String getVersion() {
        return "1.0";
    }

    public List<Parameter> getParameters() {
        return parameterList;
    }

    private void writeParamsFile() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(matchParameters);
            fos.write(String.format("query\n" + "bbr %f thif %d thff %d\n" + "mr %f\n" + "\n" + "reference\n" + "bbr %f thif %d thff %d\n" + "mr %f\n", query_bbr.getValue(), query_thif.getValue(), query_thff.getValue(), query_mr.getValue(), reference_bbr.getValue(), reference_thif.getValue(), reference_thff.getValue(), reference_mr.getValue()).getBytes());
        } catch (IOException ex) {
            Logger.getLogger(SegMatchExeWrapper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(SegMatchExeWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public byte[] computeDescriptor(byte[] shape) {
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            File inFile = new File(tmpDir.getAbsolutePath() + File.separator + "din.off");
            fos = new FileOutputStream(inFile);
            fos.write(shape);
            fos.close();
            String outPrefix = tmpDir.getAbsolutePath() + File.separator + "dout";
            String cmd = String.format("%s -bbr %f -thif %d -thff %d -mr %f -opc %d %s %s", segmentExecutable.getAbsolutePath(), seg_bbr.getValue(), seg_thif.getValue(), seg_thff.getValue(), seg_mr.getValue(), seg_opc.getValue(), inFile.getPath(), outPrefix);
            Process proc = Runtime.getRuntime().exec(cmd, null, tmpDir.getAbsoluteFile());
            proc.waitFor();
            File resultFile = new File(outPrefix + "_seg.off");
            if (!resultFile.exists()) {
                System.err.println("Executable has not generated any output file!");
                return null;
            }
            fis = new FileInputStream(resultFile);
            long resultSize = fis.getChannel().size();
            byte[] result = new byte[(int) resultSize];
            fis.read(result);
            fis.close();
            return result;
        } catch (InterruptedException ex) {
            System.err.println("Could not compute Descriptor: Error when running executable.");
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println("Could not compute Descriptor: IO error.");
            ex.printStackTrace(System.err);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(SegMatchExeWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public double match(byte[] d1, byte[] d2) {
        FileOutputStream fos = null;
        Process proc = null;
        try {
            File inFile1 = new File(tmpDir.getAbsolutePath() + File.separator + "din1.off");
            fos = new FileOutputStream(inFile1);
            fos.write(d1);
            fos.close();
            File inFile2 = new File(tmpDir.getAbsolutePath() + File.separator + "din2.off");
            fos = new FileOutputStream(inFile2);
            fos.write(d2);
            fos.close();
            String cmd = String.format("%s %s %s %s", matchExecutable.getAbsolutePath(), inFile1.getPath(), inFile2.getPath(), matchParameters.getPath());
            proc = Runtime.getRuntime().exec(cmd, null, tmpDir.getAbsoluteFile());
            byte[] buffer = new byte[1024];
            proc.getInputStream().read(buffer);
            proc.waitFor();
            proc.getInputStream().close();
            String resultStr = new String(buffer);
            float result;
            result = Float.parseFloat(resultStr);
            return result;
        } catch (InterruptedException ex) {
            System.err.println("Could not match descriptors: Error when running executable.");
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println("Could not match descriptors: IO error.");
            ex.printStackTrace(System.err);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (proc != null) {
                    proc.getInputStream().close();
                }
            } catch (IOException ex) {
                Logger.getLogger(SegMatchExeWrapper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return Float.NaN;
    }

    public Set<ShapeType> getApplicableTypes() {
        return EnumSet.of(ShapeType.OFF_3D);
    }

    public String getAuthors() {
        return "SMART Team";
    }

    public boolean isQueryShapeMandatory() {
        return true;
    }

    public void processNewParameters() throws InvalidParameterValuesException {
        return;
    }

    public void renderDescriptor(byte[] shapeData, byte[] descData, DescriptorRenderer renderer) {
        return;
    }
}
