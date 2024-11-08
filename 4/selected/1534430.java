package ssg.tools.common.fragmentedFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Reference implementation for processing FragmentedFileInfo using related actual
 * set of data sources.
 * 
 * IMPORTANT: Exposes method to build FragmentedFileInfo based on EditOperations: buildFFI
 *
 * This tool purpose is to enable optimized processing of large files.
 * Though it may be used for small ones as well.
 *
 * The core of tool is EditOperation abstract that is used to plan and
 * then perform file editing actions.
 *
 * EditOperation is characterized by data source and source offset/size and target offset/size.
 * Editing operations are classified as:
 * - atomic (those to which any other operation is developed)
 * - edit (most used in normal editing processes)
 * - extended (e.g. informational or user-defind macro operations which
 *   finally should be decomposed to atomic ones)
 *
 * List of EditOperation(s) is a taskfor file editing.
 * File may be edited
 * - inplace (just overwriting existing file content) - NOT IMPLEMENTED YET
 * - by copying (i.e.  streaming input file(s) in to output and applying needed changes on the fly)
 *
 * Actual file processing depends on selected mode and decompose list of atomic operations.
 * If atomic operations result in change of file size then only copying may be used.
 *
 * @author ssg
 */
public class FileCmdEditor implements Serializable {

    /**
     * @return the inputFileName
     */
    public String getInputFileName() {
        return inputFileName;
    }

    /**
     * @param inputFileName the inputFileName to set
     */
    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    /**
     * @return the outputFileName
     */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
     * @param outputFileName the outputFileName to set
     */
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    /**
     * @return the operations
     */
    public List<EditOperation> getOperations() {
        return operations;
    }

    /**
     * @param operations the operations to set
     */
    public void setOperations(List<EditOperation> operations) {
        this.setOperations(operations);
    }

    /**
     * @return the lastProcessingFragment
     */
    public synchronized ProcessingStatus getLastProcessingFragment() {
        return lastProcessingFragment;
    }

    public enum OPERATION_MODE {

        auto, stream, direct, any
    }

    private String inputFileName;

    private String outputFileName;

    private List<EditOperation> operations = new LinkedList<EditOperation>();

    private volatile ProcessingStatus lastProcessingFragment = null;

    public FileCmdEditor() {
    }

    public FileCmdEditor(String inputFileName, String outputFileName) {
        setInputFileName(inputFileName);
        setOutputFileName(outputFileName);
    }

    public List<EditOperation> processOperations() throws FileNotFoundException, IOException {
        return processOperations(getOperations());
    }

    public List<EditOperation> processOperations(List<EditOperation> operations) throws FileNotFoundException, IOException {
        if (getInputFileName() == null) {
            return null;
        }
        if (operations == null || operations.size() == 0) {
            return operations;
        }
        Map<String, StreamDataSource> dss = new HashMap<String, StreamDataSource>();
        for (EditOperation eop : operations) {
            String source = eop.getSource();
            if (!dss.containsKey(source)) {
                File dsf = null;
                if (source.equals(EditOperation.SOURCE_IS_ORIGINAL)) {
                    dsf = new File(getInputFileName());
                } else {
                    dsf = new File(source);
                }
                dss.put(source, new StreamDataSource(source, dsf));
            }
        }
        return processOperations(operations, dss, true);
    }

    public static FragmentedFileInfo buildFFI(List<EditOperation> operations, Map<String, StreamDataSource> dss) throws IOException {
        FragmentedFileInfo ffi = new FragmentedFileInfo();
        return buildFFI(ffi, operations, dss);
    }

    public static FragmentedFileInfo buildFFI(FragmentedFileInfo ffi, List<EditOperation> operations, Map<String, StreamDataSource> dss) throws IOException {
        for (EditOperation eop : operations) {
            long sourceLength = EditOperation.POSITION_PARSE_ERROR;
            if (eop.getSourceOffset() == EditOperation.POSITION_SAME || eop.getSourceSize() == EditOperation.POSITION_SAME || eop.getSourceSize() == EditOperation.POSITION_END) {
                sourceLength = dss.get(eop.getSource()).getLength();
            }
            List<Fragment> fs = eop.getFragments();
            for (Fragment f : fs) {
                boolean fIsUndefined = EditOperation.isUndefined(f.size) || EditOperation.isUndefined(f.offset) || f.eop != null && f.eop.valuesEvaluationIsNeeded();
                if (fIsUndefined || f.getStart() < 0 || f.getEnd() < f.getStart()) {
                    long targetLength = ffi.getLength();
                    if (f.sourceOffset == EditOperation.POSITION_SAME) {
                        f.sourceOffset = f.offset;
                    }
                    if (f.size == EditOperation.POSITION_END) {
                        f.size = sourceLength - f.sourceOffset;
                    }
                    if (f.offset == EditOperation.POSITION_END) {
                        f.offset = targetLength;
                    }
                    if (f.size == EditOperation.POSITION_SAME) {
                        f.size = f.eop.getSourceSize();
                    }
                }
                long targetSize = f.eop.getTargetSize();
                if (eop.isUndefined(targetSize)) {
                    if (eop.getTargetSize() == eop.POSITION_SAME || eop.getTargetSize() == eop.POSITION_END) {
                        targetSize = sourceLength;
                    }
                }
                boolean sizeChanged = targetSize != f.size;
                if (f.eop != null && sizeChanged) {
                    ffi.addFragment(f, targetSize);
                } else {
                    ffi.addFragment(f);
                }
                if (1 == 0) {
                    System.out.println("\nBuilding fragments, temporary info:");
                    for (Fragment ff : ffi.getFragments()) {
                        System.out.println(ff.toString());
                    }
                    try {
                        System.out.println("File length: " + ffi.getLength());
                    } catch (IOException ioex) {
                    }
                }
            }
        }
        return ffi;
    }

    public List<EditOperation> processOperations(List<EditOperation> operations, Map<String, StreamDataSource> dss, boolean closeDSSs) throws FileNotFoundException, IOException {
        if (getOutputFileName() == null) {
            return null;
        }
        if (operations == null || operations.size() == 0) {
            return operations;
        }
        FragmentedFileInfo ffi = buildFFI(operations, dss);
        try {
            System.out.println("\nBuilding fragments, final info [" + ffi.getLength() + "]:");
            for (Fragment f : ffi.getFragments()) {
                System.out.println(f.toString());
            }
            ffi.defragment();
            System.out.println("\nBuilding fragments, final info (defragmented) [" + ffi.getLength() + "]:");
            for (Fragment f : ffi.getFragments()) {
                System.out.println(f.toString());
            }
        } catch (IOException ioex) {
        }
        OutputStream fos = null;
        File f = new File(getOutputFileName());
        if (f.exists()) {
            f.delete();
        }
        fos = new BufferedOutputStream(new FileOutputStream(getOutputFileName()), 1024 * 30);
        for (Fragment fr : ffi.getFragments()) {
            lastProcessingFragment = new ProcessingStatus();
            lastProcessingFragment.fr = fr;
            if (fr.eop != null) {
                fr.eop.setStatus(EditOperation.OPERATION_STATUS.executing);
            }
            if (fr.size > 0) {
                InputStream frIs = (fr.eop != null) ? dss.get(fr.eop.getSource()).getInputStream(fr.sourceOffset) : new ByteArrayInputStream(fr.data);
                byte[] buf = new byte[1024 * 4];
                long toReadSize = fr.size;
                while (toReadSize > 0) {
                    int readCount = frIs.read(buf, 0, (int) Math.min(buf.length, toReadSize));
                    toReadSize -= readCount;
                    fos.write(buf, 0, readCount);
                    lastProcessingFragment.length = fr.offset + (fr.size - toReadSize);
                }
            } else {
            }
            if (fr.eop != null) {
                fr.eop.setStatus(EditOperation.OPERATION_STATUS.OK);
            }
        }
        fos.close();
        if (closeDSSs) {
            for (String source : dss.keySet()) {
                try {
                    dss.get(source).close();
                } catch (IOException ioex) {
                }
            }
        }
        return operations;
    }

    public static void dumpOperation(EditOperation eop) {
        System.out.println("Operation [" + eop.getSource() + "]: " + eop.getSourceOffset() + "/" + eop.getSourceSize() + " -> " + eop.getTargetOffset() + "/" + eop.getTargetSize());
    }

    public static void main(String[] args) throws Exception {
        File folder = new File("C:/Work/Personal/Projects/_my_tests/graph/java/branches/baseGraph/_graph_/ssgTools");
        File fIn = new File(folder, "_target.rar.000");
        File fOut = new File(folder, "_target.rar.000.out");
        FileCmdEditor fce = new FileCmdEditor(fIn.getAbsolutePath(), fOut.getAbsolutePath());
        List<EditOperation> ops = new LinkedList<EditOperation>();
        ops.add(EditOperation.createCopyAllOperation(EditOperation.SOURCE_IS_ORIGINAL, 0));
        ops.add(EditOperation.createCutOperation(3, 2));
        ops.add(EditOperation.createReplaceOperation(EditOperation.SOURCE_IS_ORIGINAL, 2, 6, 2, 1));
        List<EditOperation> result = fce.processOperations(ops);
        System.out.println("\nReturned resultant operations:");
        for (EditOperation eop : result) {
            dumpOperation(eop);
        }
    }

    public static class ProcessingStatus {

        public Fragment fr;

        public long length;
    }
}
