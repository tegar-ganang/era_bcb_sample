package log2gantt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;

/**
 * Parses a <code>auth.log</code> file and generates a gantt chart visualizing
 * the login sessions of each user.
 *
 * @author Michael Blume
 * @author Max Gensthaler (added: cmd args)
 */
public class AuthLog2Gantt {

    private static final File DEFAULT_LOG_FILE = new File("auth.log");

    private static final File DEFAULT_IMAGE_FILE = new File("auth_log_gantt.png");

    private static final int DEFAULT_IMAGE_WIDTH = 600;

    private static final int DEFAULT_IMAGE_HEIGHT = 0;

    private static final int DEFAULT_ROW_HEIGHT = 20;

    private static final String DEFAULT_IMAGE_TITLE = null;

    private static final boolean DEFAULT_FORCE = false;

    public static void main(String[] args) {
        File logFile = DEFAULT_LOG_FILE;
        File imageFile = DEFAULT_IMAGE_FILE;
        int imageWidth = DEFAULT_IMAGE_WIDTH;
        int imageHeight = DEFAULT_IMAGE_HEIGHT;
        String imageTitle = DEFAULT_IMAGE_TITLE;
        boolean force = DEFAULT_FORCE;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                printUsageAndExit(false);
            } else if (args[i].equals("-i")) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("The '-i' option is missing a filename.");
                }
                logFile = new File(args[++i]);
            } else if (args[i].equals("-o")) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("The '-o' option is missing a filename.");
                }
                imageFile = new File(args[++i]);
            } else if (args[i].equals("-w")) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("The '-w' option is missing the number of pixels.");
                }
                try {
                    imageWidth = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("Cannot parse width.");
                }
            } else if (args[i].equals("-h")) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("The '-h' option is missing the number of pixels.");
                }
                try {
                    imageHeight = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println();
                    printUsageAndExit("Cannot parse height.");
                }
            } else if (args[i].equals("-t")) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("The '-t' option is missing the title string.");
                }
                imageTitle = args[++i];
            } else if (args[i].equals("-f")) {
                force = true;
            } else {
                printUsageAndExit("Unknown argument: " + args[i]);
            }
        }
        if (!logFile.isFile()) {
            System.err.println("The logfile '" + logFile.getPath() + "' does not exist.");
            System.exit(1);
        }
        String imageFilename = imageFile.getName().toLowerCase();
        if (!imageFilename.endsWith(".png") && !imageFilename.endsWith(".jpg") && !imageFilename.endsWith(".jpeg")) {
            System.err.println("The output image filename has to end with '.png', '.jpg' or '.jpeg'");
            System.exit(1);
        }
        if (imageFile.exists() && force == false) {
            System.err.println("The output image file already exist. Set the force (-f) option to overwrite.");
            System.exit(1);
        }
        if (imageWidth <= 0) {
            System.err.println("Width must be > 0");
        }
        if (imageHeight < 0) {
            System.err.println("Height must be > 0");
        }
        new AuthLog2Gantt(logFile, imageFile, force, imageWidth, imageHeight, imageTitle);
    }

    private static void printUsageAndExit(String errMsg) {
        if (errMsg != null) {
            System.err.println("Error: " + errMsg);
            System.err.flush();
            System.out.println();
        }
        printUsageAndExit(errMsg != null);
    }

    private static void printUsageAndExit(boolean hasErr) {
        System.out.println("usage: java " + AuthLog2Gantt.class.getName() + " [options]");
        System.out.println("where options include:");
        System.out.println(" -i logfile        the input logfile to parse (default: " + DEFAULT_LOG_FILE.getName() + ")");
        System.out.println(" -o imagefile      the output image file to write (default: " + DEFAULT_IMAGE_FILE.getName() + ")");
        System.out.println(" -w width          the width of the output image (default: " + DEFAULT_IMAGE_WIDTH + ") [pixels]");
        System.out.println(" -h height         the height of the output image (default: " + DEFAULT_IMAGE_HEIGHT + ") [pixels]");
        System.out.println(" -t title          the title in the output image (default: " + DEFAULT_IMAGE_TITLE + ")");
        System.out.println(" -f                to force overwriting the output file (default: " + DEFAULT_FORCE + ")");
        System.out.println();
        System.exit(hasErr ? 1 : 0);
    }

    public AuthLog2Gantt(File inputFile, File outputFile, boolean forceOverwrite, int width, int height, String title) {
        TaskSeriesCollection dataset = createDataset(inputFile);
        JFreeChart chart = createChart(title, dataset);
        if (height == 0) {
            if (title != null) {
                height += 23;
            }
            height += 41;
            height += dataset.getSeries(0).getItemCount() * DEFAULT_ROW_HEIGHT;
            height += 4;
        }
        writeOutputImage(chart, outputFile, width, height);
    }

    /**
	 * Creates a sample dataset for a gantt chart.
	 * @param logfile
	 * @return the dataset
	 */
    private TaskSeriesCollection createDataset(File logfile) {
        LogFileParser parser = new LogFileParser(logfile);
        List<Integer> processIds = new ArrayList<Integer>();
        processIds.addAll(parser.getProcessIds());
        Collections.sort(processIds);
        Date startDate = new Date();
        Date endDate = null;
        for (Iterator<Integer> it = processIds.iterator(); it.hasNext(); ) {
            AuthFileEntry entry = parser.getEntry(it.next());
            if (entry.getLoginTime().before(startDate)) {
                startDate = entry.getLoginTime();
                if (endDate == null) {
                    endDate = startDate;
                }
            }
            if (entry.getLogoffTime().after(endDate)) {
                endDate = entry.getLogoffTime();
            }
        }
        TreeMap<String, Task> userTasks = new TreeMap<String, Task>();
        for (Iterator<Integer> it = processIds.iterator(); it.hasNext(); ) {
            AuthFileEntry entry = parser.getEntry(it.next());
            String username = entry.getUsername();
            Task task = userTasks.get(username);
            if (task == null) {
                task = new Task(username, startDate, endDate);
                userTasks.put(username, task);
            }
            Task subTask = new Task(entry.getProcessId() + "", entry.getLoginTime(), entry.getLogoffTime());
            task.addSubtask(subTask);
        }
        TaskSeries series = new TaskSeries("authlog");
        for (Iterator<Task> it = userTasks.values().iterator(); it.hasNext(); ) {
            series.add(it.next());
        }
        TaskSeriesCollection collection = new TaskSeriesCollection();
        collection.add(series);
        return collection;
    }

    private JFreeChart createChart(String title, IntervalCategoryDataset dataset) {
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
        BarRenderer.setDefaultShadowsVisible(false);
        JFreeChart chart = ChartFactory.createGanttChart(title, "User", "Date/Time", dataset, false, false, false);
        chart.setAntiAlias(true);
        return chart;
    }

    /**
     * Write output image file based on provided JFreeChart.
     *
     * @param chart
     *            JFreeChart
     * @param outputFile
     *            file to which JFreeChart will be written
     * @param width
     *            width of image
     * @param height
     *            height of image
     */
    private void writeOutputImage(JFreeChart chart, File outputFile, int width, int height) {
        try {
            String outputFilename = outputFile.getName().toLowerCase();
            if (outputFilename.endsWith(".png")) {
                ChartUtilities.writeChartAsPNG(new FileOutputStream(outputFile), chart, width, height);
            } else if (outputFilename.endsWith(".jpg") || outputFilename.endsWith(".jpeg")) {
                ChartUtilities.writeChartAsJPEG(new FileOutputStream(outputFile), chart, width, height);
            } else {
                throw new IllegalArgumentException("outputFile's name must end with '.png', '.jpg' or '.jpeg'.");
            }
        } catch (IOException ioEx) {
            System.err.println("Error writing output image file " + outputFile);
        }
    }
}
