package com.oreditions.csvconverter.controller;

import com.oreditions.csvconverter.operation.*;
import com.oreditions.csvconverter.exception.MappingException;
import com.oreditions.csvconverter.util.CSVLine;
import java.io.*;
import java.util.*;

/**
 *
 * @author HO.OREY
 */
public class OperationProcessor implements Process {

    protected BufferedReader inputfilereader = null;

    protected BufferedWriter outputfilewriter = null;

    protected BufferedWriter errorfilewriter = null;

    protected BufferedWriter rejectedlineswriter = null;

    protected Vector<Operation> ops = null;

    protected int sourcecolnb = 0;

    protected int targetcolnb = 0;

    protected boolean verbose = false;

    /**
     * 
     * @param inputfilereader
     * @param outputfilewriter
     * @param errorfilewriter
     * @param rejectedlineswriter
     * @param ops
     */
    public OperationProcessor(BufferedReader inputfilereader, BufferedWriter outputfilewriter, BufferedWriter errorfilewriter, BufferedWriter rejectedlineswriter, Vector<Operation> ops, int sourcecolnb, int targetcolnb, boolean verbose) {
        this.inputfilereader = inputfilereader;
        this.outputfilewriter = outputfilewriter;
        this.errorfilewriter = errorfilewriter;
        this.rejectedlineswriter = rejectedlineswriter;
        this.sourcecolnb = sourcecolnb;
        this.targetcolnb = targetcolnb;
        this.ops = ops;
        this.verbose = verbose;
    }

    public int process() {
        String line = null;
        int nblinetreated = 0;
        int nblineerror = 0;
        long begin = System.currentTimeMillis();
        try {
            while ((line = inputfilereader.readLine()) != null) {
                Operation mem = null;
                if (verbose) System.out.println("Operation processor: Treating line : " + line);
                CSVLine input = new CSVLine(line), output = new CSVLine(targetcolnb);
                try {
                    for (Operation op : ops) {
                        mem = op;
                        op.process(input, output);
                    }
                    outputfilewriter.write(output.serializeCSVLine());
                    outputfilewriter.newLine();
                    outputfilewriter.flush();
                    if (verbose) System.out.println("Operation processor: Processed line : " + output.serializeCSVLine());
                    nblinetreated++;
                } catch (MappingException e) {
                    errorfilewriter.write("- One line has not been mapped : " + input.serializeCSVLine());
                    errorfilewriter.newLine();
                    errorfilewriter.write("Reason : " + mem.serialize());
                    errorfilewriter.newLine();
                    errorfilewriter.flush();
                    rejectedlineswriter.write(e.getNonMappedLine().serializeCSVLine());
                    rejectedlineswriter.newLine();
                    rejectedlineswriter.flush();
                    if (verbose) System.out.println("Operation processor: Line could not " + "be processed (see error file)");
                    nblineerror++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error in processing");
            e.printStackTrace();
            return 1;
        }
        if (verbose) {
            long end = System.currentTimeMillis() - begin;
            System.out.println("Operation processor: report :");
            System.out.println("- number of lines treated: " + nblinetreated);
            System.out.println("- number of lines in error: " + nblineerror);
            System.out.println("- processing done in " + end + " ms");
        }
        return 0;
    }
}
