package org.gnu.textutils.cat;

import java.io.*;
import java.text.*;
import java.util.*;
import org.gnu.common.*;

/** Concatenate files and print on the standard output.
	<p>
	The original says, "Always unbuffered, -u is ignored."
	In the Java version,
	we ignore -u also, but it is not clear what they mean
	by always unbuffered.  They use a char[] to transfer
	data from read calls to write calls.  That looks like
	buffering to me.  We use a char[] for simple cat, and
	for cat-with-options, we use the char[] that's internal
	to BufferedReader.  This approach is very similar to
	the original.  Are we always unbuffered too?
	<p>
	When displaying multiple files, should line numbering
	start over for each new file?  In Solaris it starts over,
	but as I read the GNU code, it does not start over.  I
	may be misreading it.  Does anybody know what POSIX says?
	Please send any info to kevinr@gjt.org.
	<p>
	I am not sure if the behavior I have in place now for line 
	numbering when concatenating a file after a file that does 
	not end with a line terminator is entirely correct.
	<p>
	By tege@sics.se, Torbjorn Granlund,<br>
	Advised by rms, Richard Stallman.
	<p>
	Ported to Java by Kevin Raulerson<br>
	http://www.gjt.org/~kevinr/
	
	@version 1.0
	@since 1.0
 */
public class Cat extends AbstractCommand {

    /** Same as Cat(), but lets you set the program name.
	 */
    public Cat(String programName) {
        setProgramName(programName);
    }

    /** Creates a Cat.  Afterwards you can make any desired adjustments 
		to properties and then optionally call start with a String[].
	 */
    public Cat() {
    }

    public static void main(String[] args) {
        new Cat().start(args);
    }

    public GetOpt start(String[] args) {
        GetOpt opts = super.start(args);
        int argind = opts.getOptInd();
        int argc = args.length;
        String infileName = "-";
        Reader input;
        Writer output = new OutputStreamWriter(getOut());
        int lineNumber = 0;
        do {
            if (argind < argc) infileName = args[argind];
            if (infileName.equals("-")) {
                input = new InputStreamReader(getIn());
            } else {
                try {
                    File infile = new File(infileName);
                    if (!infile.isAbsolute()) {
                        String cwd = System.getProperty(getUserDirKey());
                        infile = new File(cwd, infile.getPath());
                    }
                    input = new FileReader(infile);
                } catch (FileNotFoundException ex) {
                    getError().error(GNUError.OK, getString("fileNotFound") + ex.getMessage());
                    setExitStatus(GNUError.FAILURE);
                    continue;
                }
            }
            if (isNumbers() || isNumbersAtEmptyLines() || isMarkLineEnds() || isQuote() || isSqueezeEmptyLines() || isOutputTabs()) {
                try {
                    lineNumber = cat(input, infileName, output, lineNumber);
                } catch (IOException ex) {
                    getError().error(GNUError.OK, infileName);
                    setExitStatus(GNUError.FAILURE);
                }
            } else {
                simpleCat(input, infileName, output);
            }
            if (!infileName.equals("-")) {
                try {
                    input.close();
                } catch (IOException ex) {
                    getError().error(GNUError.OK, getString("errorClosingFile") + infileName);
                    setExitStatus(GNUError.FAILURE);
                }
            }
        } while (++argind < argc);
        System.exit(getExitStatus());
        return opts;
    }

    /** Switches on c.  Returns true if an option was selected.
	 */
    protected void decodeSwitch(int c, GetOpt opts) {
        switch(c) {
            case 'b':
                setNumbers(true);
                setNumbersAtEmptyLines(false);
                break;
            case 'e':
                setMarkLineEnds(true);
                setQuote(true);
                break;
            case 'n':
                setNumbers(true);
                break;
            case 's':
                setSqueezeEmptyLines(true);
                break;
            case 't':
                setOutputTabs(false);
                setQuote(true);
                break;
            case 'u':
                break;
            case 'v':
                setQuote(true);
                break;
            case 'A':
                setQuote(true);
                setMarkLineEnds(true);
                setOutputTabs(false);
                break;
            case 'E':
                setMarkLineEnds(true);
                break;
            case 'T':
                setOutputTabs(false);
                break;
            default:
                usage(GNUError.FAILURE);
        }
    }

    /** Plain cat.  Copies the file behind `input' to
		the file behind `output'.
	 */
    protected void simpleCat(Reader input, String infileName, Writer output) {
        char[] buf = new char[16384];
        int nRead;
        for (; ; ) {
            try {
                nRead = input.read(buf);
            } catch (IOException ex) {
                getError().error(GNUError.OK, infileName);
                setExitStatus(GNUError.FAILURE);
                return;
            }
            if (nRead == -1) break;
            try {
                output.write(buf, 0, nRead);
                output.flush();
            } catch (IOException ex) {
                getError().error(GNUError.FAILURE, getString("writeError"));
            }
        }
    }

    /** Cat the file behind inputDesc to the file behind outputDesc.
		Called if any option more than -u was specified.
		<p>
		A newline character is always put at the end of the buffer, to make
		an explicit test for buffer end unnecessary.
		@return The last line number of this file (for use in the
			next file, if any).
     */
    protected int cat(Reader plainInput, String infileName, Writer output, int lineNumber) throws IOException {
        LineNumberReader input = new LineNumberReader(new BufferedReader(plainInput));
        output = new BufferedWriter(output);
        boolean outputTabs = isOutputTabs();
        boolean numbersAtEmptyLines = isNumbersAtEmptyLines();
        boolean markLineEnds = isMarkLineEnds();
        boolean squeezeEmptyLines = isSqueezeEmptyLines();
        boolean number = isNumbers();
        boolean quote = isQuote();
        int newlines = getNewlines();
        input.setLineNumber(lineNumber);
        int ch = input.read();
        boolean startOfFile = true;
        while (ch != -1) {
            if (numbers && ch != '\n') {
                output.write(nextLineNum(lineNumber, output));
                output.write('\t');
            }
            if (quote && ch != '\n') {
                while (ch != -1) {
                    if (ch >= 32) {
                        if (ch < 127) {
                            output.write(ch);
                        } else if (ch == 127) {
                            output.write("^?");
                        } else {
                            if (ch >= 128 + 32) {
                            } else {
                                output.write('^');
                                output.write(ch - 128 + 64);
                            }
                        }
                    } else if (ch == '\t' && outputTabs) {
                        output.write('\t');
                    } else if (ch == '\n') {
                        if (!startOfFile) newlines = 0;
                        break;
                    } else {
                        output.write('^');
                        output.write(ch + 64);
                    }
                    ch = input.read();
                    if (startOfFile) startOfFile = false;
                }
            } else if (ch != '\n') {
                while (ch != -1) {
                    if (ch == '\t' && !outputTabs) {
                        output.write("^I");
                    } else if (ch == '\n') {
                        if (!startOfFile) newlines = 0;
                        break;
                    } else {
                        output.write(ch);
                    }
                    ch = input.read();
                    if (startOfFile) startOfFile = false;
                }
            }
            while (ch == '\n') {
                if (++newlines > 1) {
                    if (squeezeEmptyLines && (newlines > 2)) {
                        ch = input.read();
                        input.setLineNumber(input.getLineNumber() - 1);
                        continue;
                    }
                    if (numbers && numbersAtEmptyLines) {
                        output.write(nextLineNum(lineNumber, output));
                        output.write('\t');
                    }
                    if (!numbersAtEmptyLines) input.setLineNumber(input.getLineNumber() - 1);
                }
                if (markLineEnds) output.write('$');
                output.write('\n');
                lineNumber = input.getLineNumber();
                ch = input.read();
            }
        }
        output.flush();
        setNewlines(newlines);
        return lineNumber;
    }

    /** Compute the next line number.
	 */
    protected String nextLineNum(int lineNumber, Writer output) throws IOException {
        FieldPosition fp = new FieldPosition(NumberFormat.INTEGER_FIELD);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        StringBuffer temp = new StringBuffer();
        nf.format(lineNumber + 1, temp, fp);
        for (int i = 0; i < getMinimumSizeSize() - fp.getEndIndex(); i++) {
            output.write(' ');
        }
        return temp.toString();
    }

    protected String getOptString() {
        return super.getOptString() + "benstuvAET";
    }

    protected InputStream getIn() {
        return System.in;
    }

    private boolean numbersAtEmptyLines = true;

    protected boolean isNumbersAtEmptyLines() {
        return this.numbersAtEmptyLines;
    }

    protected void setNumbersAtEmptyLines(boolean numbersAtEmptyLines) {
        this.numbersAtEmptyLines = numbersAtEmptyLines;
    }

    private boolean numbers;

    protected boolean isNumbers() {
        return this.numbers;
    }

    protected void setNumbers(boolean numbers) {
        this.numbers = numbers;
    }

    private boolean markLineEnds;

    protected boolean isMarkLineEnds() {
        return this.markLineEnds;
    }

    protected void setMarkLineEnds(boolean markLineEnds) {
        this.markLineEnds = markLineEnds;
    }

    private boolean outputTabs = true;

    protected boolean isOutputTabs() {
        return this.outputTabs;
    }

    protected void setOutputTabs(boolean outputTabs) {
        this.outputTabs = outputTabs;
    }

    private boolean squeezeEmptyLines;

    protected boolean isSqueezeEmptyLines() {
        return this.squeezeEmptyLines;
    }

    protected void setSqueezeEmptyLines(boolean squeezeEmptyLines) {
        this.squeezeEmptyLines = squeezeEmptyLines;
    }

    private boolean quote;

    protected boolean isQuote() {
        return this.quote;
    }

    protected void setQuote(boolean quote) {
        this.quote = quote;
    }

    private int newlines = 1;

    /** Preserves the `cat' function's local `newlines' 
		between invocations.  The initial value is one,
		to prime the mechanism the first time through.
	 */
    protected int getNewlines() {
        return this.newlines;
    }

    /** See getNewlines().
	 */
    protected void setNewlines(int newlines) {
        this.newlines = newlines;
    }

    /** Count of non-fatal error conditions.
	 */
    private int exitStatus = GNUError.OK;

    protected int getExitStatus() {
        return this.exitStatus;
    }

    protected void setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

    private final GetOptConstants.Option[] longOptions = { new GetOptConstants.Option("number-nonblank", NO_ARGUMENT, null, 'b'), new GetOptConstants.Option("number", NO_ARGUMENT, null, 'n'), new GetOptConstants.Option("squeeze-blank", NO_ARGUMENT, null, 's'), new GetOptConstants.Option("show-nonprinting", NO_ARGUMENT, null, 'v'), new GetOptConstants.Option("show-ends", NO_ARGUMENT, null, 'E'), new GetOptConstants.Option("show-tabs", NO_ARGUMENT, null, 'T'), new GetOptConstants.Option("show-all", NO_ARGUMENT, null, 'A') };

    protected GetOptConstants.Option[] getLongOptions() {
        return longOptions;
    }

    protected int getMinimumSizeSize() {
        return 6;
    }

    protected String getUserDirKey() {
        return "user.dir";
    }

    protected String[] getOptionKeys() {
        return this.optionKeys == null ? this.optionKeys = new String[] { "-A, --show-all", "-b, --number-nonblank", "-e", "-E, --show-ends", "-n, --number", "-s, --squeeze-blank", "-t", "-T, --show-tabs", "-u", "-v, --show-nonprinting" } : this.optionKeys;
    }
}
