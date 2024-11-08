package cn.evilelf.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import cn.evilelf.util.exceptions.InvalidOptionException;

public class TextCutter {

    private static final int charMode = 0;

    private static final int lineMode = 1;

    private static final int htmlMode = 2;

    private static int cutMode = htmlMode;

    private static int charPerFile = 5000;

    private static int linePerFile = 150;

    private static int pPerFile = 150;

    private static String inputFileName = null;

    private static String inputCharSet = "GBK";

    private static String outputCharSet = "UTF-8";

    private static final String usage = "tc [-c[:character number per file]]" + "|[-l[:line number per file]] " + "|[-h[:<p> number per file] [-t:<title>]]" + "<-f:input file name> " + "[-i:input character set] " + "[-o:out file character set]";

    private static String htmlTitle = "";

    public static void main(String[] args) {
        try {
            Options options = new Options(args);
            if (options.containsOption("c")) {
                cutMode = 0;
                String value = options.getOptionValue("c");
                if (null != value) {
                    charPerFile = Integer.parseInt(value);
                }
            }
            if (options.containsOption("l")) {
                cutMode = 1;
                String value = options.getOptionValue("l");
                if (null != value) {
                    linePerFile = Integer.parseInt(value);
                }
            }
            if (options.containsOption("h")) {
                cutMode = 2;
                String value = options.getOptionValue("h");
                if (null != value) {
                    pPerFile = Integer.parseInt(value);
                }
                if (options.containsOption("t")) {
                    htmlTitle = options.getOptionValue("t");
                }
            }
            if (options.containsOption("f")) {
                String value = options.getOptionValue("f");
                if (null != value) {
                    inputFileName = value;
                } else {
                    throw new InvalidOptionException();
                }
            } else {
                throw new InvalidOptionException();
            }
            if (options.containsOption("i")) {
                String value = options.getOptionValue("i");
                if (null != value) {
                    inputCharSet = value;
                } else {
                    throw new InvalidOptionException();
                }
            }
            if (options.containsOption("o")) {
                String value = options.getOptionValue("o");
                if (null != value) {
                    outputCharSet = value;
                } else {
                    throw new InvalidOptionException();
                }
            }
        } catch (Exception e) {
            System.err.println("Invalid options, usage:");
            System.err.println(usage);
            System.err.flush();
            System.exit(1);
        }
        cut();
    }

    private static void cut() {
        File inputFile = new File(inputFileName);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), inputCharSet));
        } catch (FileNotFoundException e) {
            System.err.print("Invalid File Name!");
            System.err.flush();
            System.exit(1);
        } catch (UnsupportedEncodingException e) {
            System.err.print("Invalid Char Set Name!");
            System.err.flush();
            System.exit(1);
        }
        switch(cutMode) {
            case charMode:
                {
                    int outputFileIndex = 1;
                    char[] readBuf = new char[charPerFile];
                    while (true) {
                        int readCount = 0;
                        try {
                            readCount = in.read(readBuf);
                        } catch (IOException e) {
                            System.err.println("Read IO Error!");
                            System.err.flush();
                            System.exit(1);
                        }
                        if (-1 == readCount) break; else {
                            try {
                                int ppos = inputFileName.lastIndexOf(".");
                                String prefixInputFileName = inputFileName.substring(0, ppos);
                                String postfixInputFileName = "html";
                                DecimalFormat outputFileIndexFormat = new DecimalFormat("0000");
                                File outputFile = new File(prefixInputFileName + "-" + outputFileIndexFormat.format(outputFileIndex) + "." + postfixInputFileName);
                                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), outputCharSet));
                                out.write(readBuf, 0, readCount);
                                out.flush();
                                out.close();
                                outputFileIndex++;
                            } catch (IOException e) {
                                System.err.println("Write IO Error!");
                                System.err.flush();
                                System.exit(1);
                            }
                        }
                    }
                    break;
                }
            case lineMode:
                {
                    boolean isFileEnd = false;
                    int outputFileIndex = 1;
                    while (!isFileEnd) {
                        try {
                            int ppos = inputFileName.lastIndexOf(".");
                            String prefixInputFileName = inputFileName.substring(0, ppos);
                            String postfixInputFileName = inputFileName.substring(ppos + 1);
                            DecimalFormat outputFileIndexFormat = new DecimalFormat("0000");
                            File outputFile = new File(prefixInputFileName + outputFileIndexFormat.format(outputFileIndex) + "." + postfixInputFileName);
                            PrintStream out = new PrintStream(new FileOutputStream(outputFile), false, outputCharSet);
                            int p = 0;
                            while (p < linePerFile) {
                                String line = in.readLine();
                                if (null == line) {
                                    isFileEnd = true;
                                    break;
                                }
                                out.println(line);
                                ++p;
                            }
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            System.err.println("Write IO Error!");
                            System.err.flush();
                            System.exit(1);
                        }
                        ++outputFileIndex;
                    }
                    break;
                }
            case htmlMode:
                {
                    boolean isFileEnd = false;
                    int outputFileIndex = 1;
                    int ppos = inputFileName.lastIndexOf(".");
                    String prefixInputFileName = inputFileName.substring(0, ppos);
                    String postfixInputFileName = "html";
                    DecimalFormat df = new DecimalFormat("0000");
                    while (!isFileEnd) {
                        try {
                            File outputFile = new File(prefixInputFileName + "-" + df.format(outputFileIndex) + "." + postfixInputFileName);
                            PrintStream out = new PrintStream(new FileOutputStream(outputFile), false, outputCharSet);
                            out.println("<html><head><title>" + prefixInputFileName + "-" + df.format(outputFileIndex) + "</title>" + "<meta http-equiv=\"Content-Type\"" + " content=\"text/html; " + "charset=" + outputCharSet + "\" />" + "<link rel =\"stylesheet\" " + "type=\"text/css\" " + "href=\"stylesheet.css\" />" + "</head><body><div id=\"content\">");
                            int p = 0;
                            while (p < pPerFile) {
                                String line = in.readLine();
                                if (null == line) {
                                    isFileEnd = true;
                                    break;
                                }
                                if (line.length() > 0) out.println("<p>" + line + "</p>");
                                ++p;
                            }
                            out.println("</div><a href=\"" + prefixInputFileName + "-" + df.format(outputFileIndex + 1) + "." + postfixInputFileName + "\">NEXT</a></body></html>");
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            System.err.println("Write IO Error!");
                            System.err.flush();
                            System.exit(1);
                        }
                        ++outputFileIndex;
                    }
                    try {
                        File indexFile = new File("index.html");
                        PrintStream out = new PrintStream(new FileOutputStream(indexFile), false, outputCharSet);
                        out.println("<html><head><title>" + "Index" + "</title>" + "<meta http-equiv=\"Content-Type\"" + " content=\"text/html; " + "charset=" + outputCharSet + "\" />" + "<link rel =\"stylesheet\" " + "type=\"text/css\" " + "href=\"stylesheet.css\" />" + "</head><body><h2>" + htmlTitle + "</h2><div id=\"content\"><ul>");
                        for (int i = 1; i < outputFileIndex; i++) {
                            out.println("<li><a href=\"" + prefixInputFileName + "-" + df.format(i) + "." + postfixInputFileName + "\">" + df.format(i) + "</a></li>");
                        }
                        out.println("</ul></body></html>");
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        System.err.println("Write IO Error!");
                        System.err.flush();
                        System.exit(1);
                    }
                    break;
                }
        }
    }
}
