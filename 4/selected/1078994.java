package magictool.dissim;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import javax.swing.JDesktopPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import magictool.Cancelable;
import magictool.DidNotFinishException;
import magictool.Executable;
import magictool.ExpFile;
import magictool.ProcessTimer;
import magictool.ProgressFrame;
import magictool.Project;
import java.util.Date;

/**
 * Dissimilarity creates a dissimilarity file with one of three methods - Correlation, Jacknife, Weighted LP.
 * The process is cancelable and can be executed as a task.
 */
public class Dissimilarity extends Thread implements Cancelable, Executable {

    /**expression file to create dissimlarity file from*/
    protected ExpFile expfile;

    private String outfile;

    /**dissimlarity method*/
    protected int disType;

    /**other parameters*/
    protected String modifiers;

    private ProgressFrame progress;

    private JDesktopPane desktop;

    /**whether process successfully completed*/
    protected boolean completed = false;

    /**whether process is over*/
    protected boolean over = false;

    /**project to place dissimlarity file in*/
    protected Project project = null;

    /**whether process has been canceled*/
    protected boolean cancel = false;

    /**1-Correlation Coefficent method*/
    public static final int COR = 0;

    /**Weighted LP method*/
    public static final int LP = 1;

    /**Kacknife Correlation method*/
    public static final int JACK = 2;

    /**whether or not to show error messages to the user*/
    protected boolean showMessages = true;

    /**
     * constructs the dissimilarity based on the method and files but does not start the creation of a file
     *
     * @param exp expression file to create dissimlarity file from
     * @param outfile dissimilarity filename
     * @param disType dissimilarity method
     * @param modifiers other parameters
     * @param desktop desktoppane to draw progress bar on
     */
    public Dissimilarity(String exp, String outfile, int disType, String modifiers, JDesktopPane desktop) {
        this.expfile = new ExpFile(new File(exp));
        this.outfile = outfile;
        this.disType = disType;
        this.modifiers = modifiers;
        this.desktop = desktop;
    }

    /**
     * sets the project associated with the dissimilarity file
     * @param p project associated with the dissimilarity file
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /**
     * Overrides the run method of the thread to create the dissimilarity file.
     */
    public void run() {
        progress = new ProgressFrame("Calculating a Matrix of Dissimilarity scores /n\nfrom " + expfile.getExpFile().getName(), true, this);
        desktop.add(progress);
        progress.show();
        progress.setMaximum(((expfile.numGenes() - 1) * (expfile.numGenes())) / 2);
        if (disType == COR) {
            writeCorrelation();
        } else if (disType == LP) {
            writeLP();
        } else if (disType == JACK) {
            writeJacknife();
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progress.dispose();
            }
        });
        if (completed == true && !cancel) {
            if (project != null) project.addFile(expfile.getName() + File.separator + outfile.substring(outfile.lastIndexOf(File.separator) + 1));
        } else if (completed == false && !cancel) {
            if (showMessages) JOptionPane.showMessageDialog(null, "Error Writing Dissimilarity File");
        }
        over = true;
    }

    private void printHeader(DataOutputStream stream, ExpFile expfile, int disType, String modifiers) throws Exception {
        stream.writeInt(expfile.numGenes());
        stream.writeBoolean(true);
        stream.writeUTF(expfile.getName() + (expfile.getName().endsWith(".exp") ? "" : ".exp"));
        stream.writeInt(disType);
        if (disType == 1) stream.writeUTF(modifiers);
        expfile.getAllGenes(stream);
    }

    /**
     * starts the thread and the creation of the dissimilarity file
     */
    public void start() {
        cancel = false;
        completed = false;
        over = false;
        super.start();
    }

    /**
     * cancels the process
     */
    public void cancel() {
        cancel = true;
        File f = new File(outfile);
        if (f.exists()) {
            while (f.exists()) {
                f.delete();
            }
            String name = f.getAbsolutePath();
            int current = -1;
            int slast = -1, last = -1;
            while ((current = name.indexOf(File.separator, last + 1)) != -1) {
                slast = last;
                last = current;
            }
            if (project != null && project.fileExists(name)) project.removeFile(name);
        }
    }

    /**
     * writes the dissimilarity file using the 1-Correlation Coefficient method
     */
    protected void writeCorrelation() {
        try {
            ProcessTimer corrWriteTimer = new ProcessTimer("Dissimilarity.writeCorrelation()");
            FileChannel disOutChannel = new RandomAccessFile(outfile, "rw").getChannel();
            DataOutputStream stream = new DataOutputStream(Channels.newOutputStream(disOutChannel));
            printHeader(stream, expfile, disType, null);
            MappedByteBuffer disMappedByteBuffer = disOutChannel.map(FileChannel.MapMode.READ_WRITE, disOutChannel.position(), (expfile.numGenes() * (expfile.numGenes() - 1) * 2));
            for (int row = 1; row < expfile.numGenes() && !cancel; row++) {
                for (int column = 0; column < row; column++) {
                    disMappedByteBuffer.putFloat(expfile.correlation(row, column));
                    progress.addValue(1);
                }
            }
            stream.close();
            corrWriteTimer.finish();
            if (!cancel) completed = true;
        } catch (Exception e2) {
        }
    }

    /**
     * writes the dissimilarity file using the Weighted LP method
     */
    protected void writeLP() {
        try {
            DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));
            printHeader(stream, expfile, disType, modifiers);
            for (int row = 1; row < expfile.numGenes(); row++) {
                for (int column = 0; column < row && !cancel; column++) {
                    float lp = 0;
                    if (modifiers.toLowerCase().equals("i")) {
                        lp = expfile.weightedlp(row, column);
                    } else {
                        int p = Integer.parseInt(modifiers);
                        lp = expfile.weightedlp(row, column, p);
                    }
                    stream.writeFloat(lp);
                }
                progress.addValue(expfile.numGenes() - (row + 1));
            }
            stream.close();
            if (!cancel) completed = true;
        } catch (Exception e2) {
        }
    }

    /**
     * writes the dissimilarity file using the Jacknife Correlation method
     */
    protected void writeJacknife() {
        try {
            DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));
            printHeader(stream, expfile, disType, null);
            for (int row = 1; row < expfile.numGenes(); row++) {
                for (int column = 0; column < row && !cancel; column++) {
                    stream.writeFloat(expfile.jackknife(row, column));
                }
                progress.addValue(expfile.numGenes() - (row + 1));
            }
            stream.close();
            if (!cancel) completed = true;
        } catch (Exception e2) {
        }
    }

    /**
     * executes the task of creating a dissimilarity file
     * @throws DidNotFinishException if the process failed
     */
    public void execute() throws DidNotFinishException {
        start();
        while (!over) {
        }
        if (cancel || !completed) throw new DidNotFinishException();
    }

    /**
     * whether the process is finished
     * @return whether the process is finished
     */
    public boolean isFinished() {
        return over;
    }
}
