package org.encog.workbench.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.zip.GZIPInputStream;
import javax.swing.JFileChooser;
import org.encog.app.analyst.AnalystError;
import org.encog.app.quant.QuantError;
import org.encog.app.quant.loader.yahoo.YahooDownload;
import org.encog.bot.BotUtil;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.util.Format;
import org.encog.util.benchmark.RandomTrainingFactory;
import org.encog.util.csv.CSVFormat;
import org.encog.util.file.FileUtil;
import org.encog.util.simple.EncogUtility;
import org.encog.workbench.EncogWorkBench;
import org.encog.workbench.dialogs.trainingdata.CreateMarketTrainingDialog;
import org.encog.workbench.dialogs.trainingdata.RandomTrainingDataDialog;
import org.encog.workbench.frames.document.EncogDocumentFrame;
import org.encog.workbench.util.TemporalXOR;

public class CreateTrainingData {

    /**
	 * The update time for a download.
	 */
    public static final int UPDATE_TIME = 10;

    public static void downloadMarketData(String name) {
        CreateMarketTrainingDialog dialog = new CreateMarketTrainingDialog(EncogWorkBench.getInstance().getMainWindow());
        dialog.getFromDay().setValue(1);
        dialog.getFromMonth().setValue(1);
        dialog.getFromYear().setValue(1995);
        dialog.getToDay().setValue(31);
        dialog.getToMonth().setValue(12);
        dialog.getToYear().setValue(2005);
        if (dialog.process()) {
            String ticker = dialog.getTicker().getValue();
            int fromDay = dialog.getFromDay().getValue();
            int fromMonth = dialog.getFromMonth().getValue();
            int fromYear = dialog.getFromYear().getValue();
            int toDay = dialog.getToDay().getValue();
            int toMonth = dialog.getToMonth().getValue();
            int toYear = dialog.getToYear().getValue();
            Calendar begin = new GregorianCalendar(fromYear, fromMonth - 1, fromDay);
            Calendar end = new GregorianCalendar(toYear, toMonth - 1, toDay);
            try {
                final YahooDownload loader = new YahooDownload();
                if (end.getTimeInMillis() < begin.getTimeInMillis()) {
                    EncogWorkBench.displayError("Dates", "Ending date should not be before begin date.");
                    return;
                }
                File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
                EncogWorkBench.getInstance().getMainWindow().beginWait();
                loader.loadAllData(dialog.getTicker().getValue(), targetFile, CSVFormat.ENGLISH, begin.getTime(), end.getTime());
            } catch (QuantError e) {
                EncogWorkBench.displayError("Ticker Symbol", "Invalid ticker symbol, or cannot connect.");
            } finally {
                EncogWorkBench.getInstance().getMainWindow().endWait();
            }
        }
    }

    public static void generateXORTemp(String name) {
        String str = EncogWorkBench.displayInput("How many training elements in the XOR temporal data set?");
        if (str != null) {
            int count = 0;
            try {
                count = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                EncogWorkBench.displayError("Error", "Must enter a valid number.");
            }
            TemporalXOR temp = new TemporalXOR();
            BasicMLDataSet trainingData = (BasicMLDataSet) temp.generate(count);
            File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
            EncogUtility.saveCSV(targetFile, CSVFormat.ENGLISH, trainingData);
        }
    }

    public static void generateRandom(String name) throws IOException {
        RandomTrainingDataDialog dialog = new RandomTrainingDataDialog(EncogWorkBench.getInstance().getMainWindow());
        dialog.getHigh().setValue(1);
        dialog.getLow().setValue(-1);
        if (dialog.process()) {
            double high = dialog.getHigh().getValue();
            double low = dialog.getLow().getValue();
            int elements = dialog.getElements().getValue();
            int input = dialog.getColumns().getValue();
            File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
            MLDataSet trainingData = RandomTrainingFactory.generate(System.currentTimeMillis(), elements, input, 0, low, high);
            EncogUtility.saveCSV(targetFile, CSVFormat.ENGLISH, trainingData);
        }
    }

    public static void copyCSV(String name) {
        final JFileChooser fc = new JFileChooser();
        if (EncogWorkBench.getInstance().getProjectDirectory() != null) fc.setCurrentDirectory(EncogWorkBench.getInstance().getProjectDirectory());
        fc.addChoosableFileFilter(EncogDocumentFrame.CSV_FILTER);
        final int result = fc.showOpenDialog(EncogWorkBench.getInstance().getMainWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            String file = fc.getSelectedFile().getAbsolutePath();
            File sourceFile = new File(file);
            File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
            FileUtil.copy(sourceFile, targetFile);
        }
    }

    public static void copyXOR(String name) {
        File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
        FileUtil.copyResource("org/encog/workbench/data/xor.csv", targetFile);
    }

    public static void copyIris(String name) {
        File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
        FileUtil.copyResource("org/encog/workbench/data/iris.csv", targetFile);
    }

    public static void downloadSunspots(String name) {
        try {
            EncogWorkBench.getInstance().getMainWindow().beginWait();
            File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
            BotUtil.downloadPage(new URL("http://solarscience.msfc.nasa.gov/greenwch/spot_num.txt"), targetFile);
        } catch (IOException ex) {
            EncogWorkBench.displayError("Error Downloading Data", ex);
        } finally {
            EncogWorkBench.getInstance().getMainWindow().endWait();
        }
    }

    public static void copyDigits(String name) {
        File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
        FileUtil.copyResource("org/encog/workbench/data/digits.csv", targetFile);
    }

    public static void copyPatterns1(String name) {
        File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
        FileUtil.copyResource("org/encog/workbench/data/pattern1.csv", targetFile);
    }

    public static void copyPatterns2(String name) {
        File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
        FileUtil.copyResource("org/encog/workbench/data/pattern2.csv", targetFile);
    }

    /**
	 * Down load a file from the specified URL, uncompress if needed.
	 * @param url THe URL.
	 * @param file The file to down load into.
	 */
    private static void downloadPage(final URL url, final File file) {
        try {
            long size = 0;
            final byte[] buffer = new byte[BotUtil.BUFFER_SIZE];
            final File tempFile = new File(file.getParentFile(), "temp.tmp");
            int length;
            int lastUpdate = 0;
            FileOutputStream fos = new FileOutputStream(tempFile);
            final InputStream is = url.openStream();
            do {
                length = is.read(buffer);
                if (length >= 0) {
                    fos.write(buffer, 0, length);
                    size += length;
                }
                if (lastUpdate > UPDATE_TIME) {
                    EncogWorkBench.getInstance().outputLine("Downloading..." + Format.formatMemory(size));
                    lastUpdate = 0;
                }
                lastUpdate++;
            } while (length >= 0);
            fos.close();
            if (url.toString().toLowerCase().endsWith(".gz")) {
                final FileInputStream fis = new FileInputStream(tempFile);
                final GZIPInputStream gis = new GZIPInputStream(fis);
                fos = new FileOutputStream(file);
                size = 0;
                lastUpdate = 0;
                do {
                    length = gis.read(buffer);
                    if (length >= 0) {
                        fos.write(buffer, 0, length);
                        size += length;
                    }
                    if (lastUpdate > UPDATE_TIME) {
                        EncogWorkBench.getInstance().outputLine("Downloading..." + Format.formatMemory(size));
                        lastUpdate = 0;
                    }
                    lastUpdate++;
                } while (length >= 0);
                fos.close();
                fis.close();
                gis.close();
                tempFile.delete();
            } else {
                file.delete();
                tempFile.renameTo(file);
            }
        } catch (final IOException e) {
            throw new AnalystError(e);
        }
    }

    public static void downloadURL(String name) {
        String url = EncogWorkBench.displayInput("Enter a URL to download to a CSV.");
        if (url != null) {
            try {
                File targetFile = new File(EncogWorkBench.getInstance().getProjectDirectory(), name);
                downloadPage(new URL(url), targetFile);
                EncogWorkBench.getInstance().refresh();
            } catch (MalformedURLException e) {
                EncogWorkBench.displayError("Invalid URL", url);
            }
        }
    }
}
