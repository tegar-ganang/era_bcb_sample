import java.io.*;

/**
 * Writes a bare-bones narrative file for review
 *
 * @author Andrew Warner
 * @version 1.0
 */
public class NarrWriter {

    private BufferedWriter output;

    /**
     * The text from the narrator
     */
    private StringBuffer allText = new StringBuffer();

    /**
     * Creates a NarrWriter object
     * @param output the file to write the narrative to
     */
    public NarrWriter(File output) {
        try {
            this.output = new BufferedWriter(new FileWriter(output));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a string to the narrative file
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
     * Writes a line to the narrative file
     * @param s the line to be written
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
     * Overloaded println method to print a blank line
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
     * Closes the output file
     */
    public void close() {
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes a fred method / hillclimbing input or output file and writes it to
     * the narrative file via the narrator
     * @param inputFile the file to write to the narrator
     * @param narr the narrator class
     *
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

    public String getText() {
        return allText.toString();
    }
}
