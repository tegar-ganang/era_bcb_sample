package au.edu.monash.merc.kashgar.image;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sindhu Emilda
 * @version v2.0
 */
public class Exiftool {

    public static final String LOG_PREFIX = "[exiftool] ";

    /**
	 * The absolute path to the Exiftool executable on the host system running
	 * this class as defined by the "<code>exiftool.path</code>" system
	 * property.
	 * <p/>
	 * If Exiftool is on your system path and running the command "exiftool"
	 * successfully executes it, leaving this value unchanged will work fine on
	 * any platform. If the Exiftool executable is named something else or not
	 * in the system path, then this property will need to be set to point at it
	 * before using this class.
	 * <p/>
	 * This system property can be set on startup with:<br/>
	 * <code>
	 * -Dexiftool.path=/path/to/exiftool
	 * </code> or by calling {@link System#setProperty(String, String)} before
	 * this class is loaded.
	 * <p/>
	 * On Windows be sure to double-escape the path to the tool, for example:
	 * <code>
	 * -Dexiftool.path=C:\\Tools\\exiftool.exe
	 * </code>
	 * <p/>
	 * Default value is "<code>exiftool</code>".
	 * <h3>Relative Paths</h3>
	 * Relative path values (e.g. "bin/tools/exiftool") are executed with
	 * relation to the base directory the VM process was started in. Essentially
	 * the directory that <code>new File(".").getAbsolutePath()</code> points at
	 * during runtime.
	 */
    public static final String EXIF_TOOL_PATH = System.getProperty("exiftool.path", "exiftool");

    /**
	 * Simple class used to house the read/write streams used to communicate
	 * with an external Exiftool process as well as the logic used to safely
	 * close the streams when no longer needed.
	 * <p/>
	 * This class is just a convenient way to group and manage the read/write
	 * streams as opposed to making them dangling member variables off of
	 * Exiftool directly.
	 * 
	 */
    private static class IOStream {

        BufferedReader reader;

        OutputStreamWriter writer;

        public IOStream(BufferedReader reader, OutputStreamWriter writer) {
            this.reader = reader;
            this.writer = writer;
        }

        public void close() {
            try {
                reader.close();
                UtilFn.log(LOG_PREFIX, "\tClosing Read stream...");
            } catch (Exception e) {
            }
            try {
                writer.flush();
                writer.close();
                UtilFn.log(LOG_PREFIX, "\tClosing Write stream...");
            } catch (Exception e) {
            }
            reader = null;
            writer = null;
            UtilFn.log(LOG_PREFIX, "\tRead/Write streams successfully closed.");
        }
    }

    private IOStream streams;

    private List<String> args;

    public Exiftool() {
        args = new ArrayList<String>(64);
    }

    protected static IOStream startExiftoolProcess(List<String> args) throws RuntimeException {
        Process proc = null;
        IOStream streams = null;
        UtilFn.log(LOG_PREFIX, "\tAttempting to start external Exiftool process using args: %s", args);
        try {
            proc = new ProcessBuilder(args).start();
            UtilFn.log(LOG_PREFIX, "\t\tSuccessful");
        } catch (Exception e) {
            String message = "Unable to start external Exiftool process using the execution arguments: " + args + ". Ensure Exiftool is installed correctly and runs using the command path '" + EXIF_TOOL_PATH + "' as specified by the 'exiftool.path' system property.";
            UtilFn.log(LOG_PREFIX, message);
            throw new RuntimeException(message, e);
        }
        UtilFn.log(LOG_PREFIX, "\tSetting up Read/Write streams to the external Exiftool process...");
        streams = new IOStream(new BufferedReader(new InputStreamReader(proc.getInputStream())), new OutputStreamWriter(proc.getOutputStream()));
        UtilFn.log(LOG_PREFIX, "\t\tSuccessful, returning streams to caller.");
        return streams;
    }

    public void writeXMPFile(String in, String out) throws IOException {
        UtilFn.log(LOG_PREFIX, "Extracting tags from image: %s", in);
        long exifToolCallElapsedTime = 0;
        args.clear();
        File outFile = new File(out);
        if (outFile.exists()) {
            outFile.delete();
        }
        args.add(EXIF_TOOL_PATH);
        args.add("-Tagsfromfile\n");
        args.add(in);
        args.add(out);
        streams = startExiftoolProcess(args);
        exifToolCallElapsedTime = System.currentTimeMillis();
        String line = null;
        while ((line = streams.reader.readLine()) != null) {
            UtilFn.log(LOG_PREFIX, "\t\tReading response: %s", line);
        }
        streams.close();
        UtilFn.log(LOG_PREFIX, "\tFinished reading Exiftool response in %d ms.", (System.currentTimeMillis() - exifToolCallElapsedTime));
    }

    public void writeTxtFiles(String img_path, String txt_fpath) throws IOException {
        UtilFn.log(LOG_PREFIX, "Write text file, %s for the image %s", txt_fpath, img_path);
        long exifToolCallElapsedTime = System.currentTimeMillis();
        args.clear();
        args.add(EXIF_TOOL_PATH);
        args.add("-g\n");
        args.add(img_path);
        streams = startExiftoolProcess(args);
        exifToolCallElapsedTime = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream(txt_fpath);
        Writer out = new OutputStreamWriter(fos, "UTF8");
        String line = null;
        while ((line = streams.reader.readLine()) != null) {
            out.write(line + "\n");
        }
        out.flush();
        out.close();
        streams.close();
        UtilFn.log(LOG_PREFIX, "\tFnished reading Exiftool response in %d ms.", (System.currentTimeMillis() - exifToolCallElapsedTime));
    }
}
