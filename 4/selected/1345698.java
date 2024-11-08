package ecosim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *  Writes a bare-bones narrative file for review.
 *
 *  @author Andrew Warner
 */
public class NarrWriter {

    /**
     * Creates a NarrWriter object.
     *
     * @param output The file to write the narrative to.
     */
    public NarrWriter(File output) {
        try {
            this.output = new BufferedWriter(new FileWriter(output));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes a string to the narrative file.
     *
     *  @param s The String to be written.
     */
    public void print(String s) {
        try {
            output.write(s);
            allText.append(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes a line to the narrative file.
     *
     *  @param s The String to be written.
     */
    public void println(String s) {
        try {
            output.write(s);
            output.newLine();
            allText.append(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Overloaded the println method to print a blank line.
     */
    public void println() {
        try {
            output.newLine();
            allText.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Closes the narrator file.
     */
    public void close() {
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Takes a fred method / hillclimbing input or output file and writes it to
     *  the narrative file via the narrator.
     *
     *  @param inputFile The file to write to the narrator.
     */
    public void writeInput(File inputFile) {
        BufferedReader input;
        try {
            input = new BufferedReader(new FileReader(inputFile));
            String nextLine = input.readLine();
            while (nextLine != null) {
                println(nextLine);
                nextLine = input.readLine();
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Return all text stored in this Narrator.
     *
     *  @return String containing the text.
     */
    public String getText() {
        return allText.toString();
    }

    /**
     *  The output writer.
     */
    private BufferedWriter output;

    /**
     *  The text from the narrator.
     */
    private StringBuffer allText = new StringBuffer();
}
