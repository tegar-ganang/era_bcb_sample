package com.volantis.mcs.build;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This class
 */
public class VBMManager {

    /**
     * end of comment
     */
    static final String TERMINATOR = "*/";

    /**
     * Dashes before end of comment
     */
    static final String DASHES = "---------------------------" + "--------------------------";

    /**
     * The VBM string.
     */
    private final String VBM = "VBM:";

    /**
     * Appended after the number eg VBM:2003010101 -
     */
    private final String AFTER_VBM = " - ";

    /**
     * Whitespace to insert after date to align the engineer name.
     */
    private final String DATE_TO_NAME = "    ";

    /**
     * Maximum amount of whitespace from the engineer name to the position
     * where the comment is aligned.  A substring based on the engineer name
     * length will be used.
     */
    private final String NAME_TO_TEXT = "                ";

    /**
     * The maximum width of a comment.
     */
    private final int MAX_CHARS = 47;

    /**
     * String to append between date elements.  eg.  01-Jan-03
     */
    private final char DATE_SEPARATOR = '-';

    public static void main(String[] args) {
        PrintStream out = System.out;
        InputStream in = System.in;
        String infile = "System.in";
        VBMInfo vbmInfo = new VBMInfo();
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }
        try {
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals("-i")) {
                    infile = args[++i];
                    in = new FileInputStream(infile);
                    continue;
                }
                if (args[i].equals("-o")) {
                    out = new PrintStream(new FileOutputStream(args[++i]));
                    continue;
                }
                if (args[i].equals("-w")) {
                    vbmInfo.setWho(args[++i]);
                    continue;
                }
                if (args[i].equals("-t")) {
                    vbmInfo.setText(args[++i]);
                    continue;
                }
                if (args[i].equals("-n")) {
                    vbmInfo.setVBM(args[++i]);
                    continue;
                }
                if (args[i].equals("-d")) {
                    vbmInfo.setDate(args[++i]);
                    continue;
                }
            }
            new VBMManager().addHistory(vbmInfo, in, out);
        } catch (Exception e) {
            System.err.println("In " + infile + "--->");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * 
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("    -i infile optional");
        System.out.println("    -o outfile optional");
        System.out.println("    -d datestring optional");
        System.out.println("    -n VBMNumber required");
        System.out.println("    -t \"text\" required");
        System.out.println("    -w \"who\" required");
    }

    public void addHistory(VBMInfo info, InputStream input, PrintStream output) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {
            processHistory(reader, output);
        } catch (IOException e) {
            throw new Exception("Unable to process the history: " + e.toString());
        }
        try {
            addVBMInfo(info, output);
        } catch (IOException e) {
            throw new Exception("Unable to add VBM info: " + e.toString());
        }
        try {
            finishInput(reader, output);
        } catch (IOException e) {
            throw new Exception("Unable to complete history addition: " + e.toString());
        }
    }

    /**
     * @param input
     * @param output
     */
    private void finishInput(BufferedReader input, PrintStream output) throws IOException {
        int read;
        while ((read = input.read()) != -1) {
            output.write(read);
        }
    }

    /**
     * @param info
     * @param output
     */
    private void addVBMInfo(VBMInfo info, PrintStream output) throws IOException {
        StringBuffer content = new StringBuffer();
        content.append(" * ").append(info.getDate()).append(DATE_TO_NAME).append(info.getWho()).append(NAME_TO_TEXT.substring(info.getWho().length()));
        StringBuffer descBuffer = new StringBuffer(VBM);
        descBuffer.append(info.getVBM()).append(AFTER_VBM).append(info.getText());
        StringTokenizer tokenizer = new StringTokenizer(descBuffer.toString(), " ");
        int width = 0;
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            width = width + token.length();
            if (width > MAX_CHARS) {
                content.append("\n").append(" ").append("*").append("                              ");
                width = token.length();
            }
            content.append(token);
            if (width < MAX_CHARS) {
                content.append(' ');
                width = width + 1;
            }
        }
        output.println(content.toString());
    }

    /**
     * This method processes from the input to the output stream till
     * we reach a line of dashes followed by a * /. We rewind the stream
     * to before the dashes and return. Both stream are primed to the 
     * correct locations
     * @param input
     * @param output
     */
    private void processHistory(BufferedReader input, PrintStream output) throws IOException {
        for (; ; ) {
            input.mark(200);
            String inputLine = input.readLine();
            if (inputLine == null) {
                throw new IOException("Failed to locate VBM header in file");
            }
            if (inputLine.indexOf(DASHES) > -1) {
                String maybeTerm = input.readLine();
                if (maybeTerm == null) {
                    throw new IOException("Failed to locate end of VBM header in file");
                }
                if (maybeTerm.trim().equals(TERMINATOR)) {
                    input.reset();
                    break;
                } else {
                    output.println(inputLine);
                    output.println(maybeTerm);
                }
            } else {
                output.println(inputLine);
            }
        }
    }
}

class VBMInfo {

    private String who;

    private String text;

    private String vbm;

    private String date;

    /**
     * @return String
     */
    public String getDate() {
        if (date == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
            Date now = new Date();
            date = sdf.format(now);
        }
        return date;
    }

    /**
     * @return String
     */
    public String getVBM() {
        return vbm;
    }

    /**
     * @return String
     */
    public String getText() {
        return text;
    }

    /**
     * @return String
     */
    public String getWho() {
        return who;
    }

    /**
     * Sets the date.
     * @param date The date to set
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Sets the number.
     * @param number The number to set
     */
    public void setVBM(String number) {
        this.vbm = number;
    }

    /**
     * Sets the text.
     * @param text The text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the who.
     * @param who The who to set
     */
    public void setWho(String who) {
        this.who = who;
    }
}
