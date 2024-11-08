package flames2d.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.logging.Logger;
import flames2d.core.ProcessingResult;
import flames2d.core.Project;
import flames2d.io.ConcurrentBufferedImageWriter;
import flames2d.io.FileHandling;
import flames2d.io.OutputWriter;
import flames2d.math.Function;

/**
 * This class produces the output of this application.
 * Therefore a thread will be started and a queue will be used to
 * setup the results. All results will be written to disk in into files.
 * Concurrent Image Writers ensure that the saving process doesn't hesitate
 * the calculation of the output.
 * At least for each ProcessingResult an image shows the calculation. In addition
 * other files show the results of all calculations.
 * These files have the form:
 * out_"input image name".png as output image
 * "input image name".pnt as file of x/y values which have been used for the regression
 * "input image name".fnc as file of coefficients
 * "input image name".log as generic log which shows the whole process
 * @author AnjoVahldiek
 *
 */
public class ProcessOutput implements Runnable {

    /**
	 * Data structure which holds the tasks. It is used
	 * in a way like the consumer-producer problem known in parallel
	 * programming. The producer in this case is the Process.java and this
	 * class is the consumer.
	 */
    BlockingDeque<ProcessingResult> mTaskQueue;

    /**
	 * Holds the writing threads. All these threads have to be finished before
	 * the application is terminated. Otherwise the images on disk may not be stored.
	 */
    List<Thread> mWriterThreads;

    /**
	 * Variable which specifies a finished computation. Has to be set from an external
	 * Thread. Therefore getters and setters are synchronized to ensure a thread-safe
	 * access!
	 */
    private boolean mFinished = false;

    /**
	 * Should be used to terminate the number of processing results. At maximum this number
	 * of processing results will be calculated. It is possible to set the finished status
	 * before the Capacity isn't reached! In this case all elements which are in the Queue
	 * will be calculated.
	 */
    private int mCapacity = 0;

    private Project mProject;

    /**
	 * Counts the number of PRs to form better output files
	 */
    private int count = 0;

    private static final int POINT_GAP = 4;

    /**
	 * Constructor creating the process out object
	 * @param deque the queue to block
	 * @param project	the actual project
	 * @param capacity the capacity of this threads
	 */
    public ProcessOutput(BlockingDeque<ProcessingResult> deque, Project project, int capacity) {
        this.mTaskQueue = deque;
        this.mWriterThreads = new ArrayList<Thread>(capacity);
        this.mCapacity = capacity;
        this.mProject = project;
    }

    /**
	 * Starts the processing of output images in another thread.
	 * @return the thread in which the output images will be processed (important to join)
	 */
    public Thread beginProcessing() {
        Thread t = new Thread(this, "Processing Output - Capacity=" + this.mCapacity);
        t.start();
        return t;
    }

    /**
	 * the run method of the process out thread
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        int num = 0;
        while (((!isFinished()) && (num < this.mCapacity)) || !this.mTaskQueue.isEmpty()) {
            try {
                ProcessingResult pr = this.mTaskQueue.takeFirst();
                Logger log = pr.getLog();
                if (pr.getError() == null) {
                    Point vertex = pr.getVertex();
                    Point left = pr.getLeftCalibration();
                    Point right = pr.getRightCalibration();
                    File inputFile = pr.getFile();
                    BufferedImage inputImage = pr.getInputImage();
                    Function f = pr.getFunction();
                    log.finest("Berechne das Outputbild");
                    BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics g = outputImage.getGraphics();
                    g.drawImage(inputImage, 0, 0, null);
                    g.setColor(Color.red);
                    List<Point> detectedPoints = pr.getPoints();
                    String[][] pointValues = new String[detectedPoints.size() + 2][2];
                    g.setColor(new Color(104, 255, 32));
                    int i = 2;
                    int vertexX = vertex.getCor()[0];
                    int leftY = left.getCor()[1];
                    pointValues[0][0] = NumberFormat.getNumberInstance().format(left.getCor()[0] - vertexX);
                    pointValues[0][1] = NumberFormat.getNumberInstance().format(0);
                    pointValues[1][0] = NumberFormat.getNumberInstance().format(right.getCor()[0] - vertexX);
                    pointValues[1][1] = NumberFormat.getNumberInstance().format(0);
                    for (Point p : detectedPoints) {
                        int x2 = p.getCor()[0];
                        int y2 = p.getCor()[1];
                        g.fillRect(x2 - 1, y2 - 1, 3, 3);
                        pointValues[i][0] = NumberFormat.getNumberInstance().format(x2 - vertexX);
                        pointValues[i][1] = NumberFormat.getNumberInstance().format(Math.abs(leftY - y2));
                        i++;
                    }
                    g.setColor(Color.RED);
                    for (int x = pr.getMinXmaxY(); x < pr.getMaxXmaxY(); x += POINT_GAP) {
                        double y = f.function(x);
                        g.drawOval(x - 2, ((int) y) - 2, 4, 4);
                    }
                    int calibY = (left.getCor()[1] + right.getCor()[1]) / 2;
                    int leftBound = pr.getLeftBound();
                    int rightBound = pr.getRightBound();
                    g.fillRect(leftBound, calibY, rightBound - leftBound + 3, 3);
                    g.fillRect(leftBound, 0, 3, calibY);
                    g.fillRect(rightBound, 0, 3, calibY);
                    g.setColor(Color.white);
                    StringBuffer drawString = new StringBuffer("f(x) = ");
                    drawString.append(f.toString());
                    g.drawString(drawString.toString(), 100, 100);
                    g.drawString("Surface = " + pr.getSurface(), 100, 120);
                    g.drawString("Volume = " + pr.getVolume(), 100, 140);
                    g.setColor(Color.blue);
                    g.drawOval(vertex.getCor()[0] - 5, vertex.getCor()[1] - 5, 10, 10);
                    g.dispose();
                    log.finest("Berechnung abgeschlossen");
                    File output = FileHandling.formOutputImageFile(inputFile, mProject.getProjectDirectory(), count);
                    ConcurrentBufferedImageWriter cbiw = new ConcurrentBufferedImageWriter(output, "png");
                    log.finest("Speichere Bild");
                    this.mWriterThreads.add(cbiw.write(outputImage));
                    pr.setOutputImage(output);
                    double fac = pr.getUnitValue() / Math.sqrt((pr.getMeasurePoints()[1].getCor()[0] - pr.getMeasurePoints()[0].getCor()[0]) * (pr.getMeasurePoints()[1].getCor()[0] - pr.getMeasurePoints()[0].getCor()[0]) + (pr.getMeasurePoints()[1].getCor()[1] - pr.getMeasurePoints()[0].getCor()[1]) * (pr.getMeasurePoints()[1].getCor()[1] - pr.getMeasurePoints()[0].getCor()[1]));
                    String[] header = { Flames2D.formMeterOutput(fac, mProject.getUnit()) + "/px" };
                    OutputWriter pointList = new OutputWriter(header, pointValues);
                    File pointListOutput = new File(this.mProject.getProjectDirectory().getAbsolutePath() + File.separator + count + "_" + inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".")) + "_pt.txt");
                    log.finest("Speichere Punktmenge in Datei");
                    pointList.write(pointListOutput);
                    String[] fHeader = { "Function:", pr.getFunction().toString() };
                    double[] coeffs = pr.getFunction().getCoefficients();
                    String[][] fCoeffs = new String[coeffs.length][1];
                    i = 0;
                    for (String[] coeff : fCoeffs) {
                        coeff[0] = String.valueOf(coeffs[i]);
                        i++;
                    }
                    OutputWriter functionCoeffsWriter = new OutputWriter(fHeader, fCoeffs, String.valueOf(pr.getSurface()) + mProject.getUnit() + "²" + "\nVolume: " + pr.getVolume() + mProject.getUnit() + "³");
                    File functionCoeffsFile = new File(this.mProject.getProjectDirectory().getAbsolutePath() + File.separator + count + "_" + inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".")) + "_fct.txt");
                    log.finest("Speichere Funktion in Datei");
                    functionCoeffsWriter.write(functionCoeffsFile);
                    count++;
                } else {
                    log.warning("Ausgabedateien wurden nicht geschrieben!");
                }
            } catch (InterruptedException e) {
                LogManager.logWarning(this.getClass().getSimpleName(), e);
            } catch (IOException io) {
                LogManager.logWarning(this.getClass().getSimpleName(), io);
            } finally {
                num++;
            }
        }
        try {
            for (Thread t : this.mWriterThreads) {
                t.join();
            }
        } catch (InterruptedException e) {
            LogManager.logWarning(this.getClass().getSimpleName(), e);
        }
    }

    /**
	 * Set the process to produce output to true if there isn't
	 * anymore a result.
	 * @param fini determines whether the process should be finished or not
	 */
    public synchronized void setFinished(boolean fini) {
        this.mFinished = fini;
    }

    /**
	 * Ask whether the process was set to finished or not
	 * @return false, if process runs to produce output. Otherwise true.
	 */
    public synchronized boolean isFinished() {
        return this.mFinished;
    }
}
