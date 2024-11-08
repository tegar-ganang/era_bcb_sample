package magictool.cluster;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import magictool.Cancelable;
import magictool.DidNotFinishException;
import magictool.Executable;
import magictool.ProcessTimer;
import magictool.ProgressFrame;
import magictool.Project;

/**
 * AbstractCluster is an abstract class with two methods - writeClusterFile and getOutFile which
 * need to be overridden by specific cluster classes. The specific classes should define in the
 * writeClusterFile method how to cluster the elements and should write them to a file. This class
 * provides the ability to cancel itself however the implementation of this is required in the
 * individual cluster classes so as to avoid problems with stopping threads. AbstractCluster
 * provides methods to read Dissimilarity files created by the magictool package as well as a standard
 * method to write the information header for a cluster file. This class extends the thread class as clustering
 * can take a large amount of computer processing strength.
 *
 */
public abstract class AbstractCluster extends Thread implements Cancelable, Executable {

    /**number of genes in dissimilarity file*/
    protected int numGenes = 0;

    /**total number of dissimilarities listed in dissimilarity file*/
    protected int numPairs = 0;

    /**dissimilarity matrix from dissimilarity file*/
    protected float dys[][] = null;

    /**labels for the genes*/
    protected ArrayList labels = null;

    /**file path for the original expression file*/
    protected String expFilePath = "";

    /**parameters for the dissimilarity file*/
    protected String dissimParams = "";

    /**dissimilarity method*/
    protected int dissimMethod = -1;

    /**whether clustering has successfully completed - to be set by cluster classes*/
    protected boolean completed = false;

    /**whether clustering process has finished*/
    protected boolean over = false;

    /**project associated with cluster*/
    protected Project project = null;

    /**whether clustering has been canceled*/
    protected boolean cancel = false;

    /**default constructor - cannot be called as class is abstract*/
    public AbstractCluster() {
    }

    /**
   * sets the project associated with the cluster
   * @param p project
   */
    public void setProject(Project p) {
        project = p;
    }

    /**
   * returns the name of the new cluster file - abstract and to be set by the individual
   * cluster classes
   * @return new cluster file name
   */
    public abstract String getOutFile();

    /**
   * cancels the clustering - actual implementation of canceling must be placed in a cluster class
   * so as to aviod problems with stopping threads
   */
    public void cancel() {
        cancel = true;
        File f = new File(getOutFile());
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
            if (project != null) project.removeFile(name);
        }
    }

    /**
   * returns if clustering has been canceled
   * @return if clustering has been canceled
   */
    public boolean isCancelled() {
        return cancel;
    }

    /**
   * reads the dissimilarity file and stores them in appropriate variables that can be accessed
   * by subclasses
   * @param filename name of dissimilarity file
   * @param progress frame to show progress of file reading
   * @throws IOException if dissimilarity file cannot be read
   */
    public void readDissimilarityFile(String filename, ProgressFrame progress) throws IOException {
        try {
            ProcessTimer disReadTimer = new ProcessTimer("AbstractCluster.readDissimilarityFile()");
            FileChannel dysChannel = new FileInputStream(filename).getChannel();
            DataInputStream dysHeaders = new DataInputStream(Channels.newInputStream(dysChannel));
            readDisHeaders(dysHeaders);
            labels = new ArrayList(numGenes);
            for (int count = 0; count < numGenes; count++) labels.add(dysHeaders.readUTF());
            MappedByteBuffer dysBuffer = dysChannel.map(FileChannel.MapMode.READ_ONLY, dysChannel.position(), dysChannel.size() - dysChannel.position());
            dysHeaders.close();
            dys = new float[numGenes - 1][];
            numPairs = (numGenes * (numGenes - 1)) / 2;
            if (progress != null) progress.setMaximum(numPairs);
            for (int row = 1; row < numGenes && !cancel; row++) {
                dys[row - 1] = new float[row];
                for (int column = 0; column < row; column++) {
                    dys[row - 1][column] = dysBuffer.getFloat();
                    if (progress != null) progress.addValue(1);
                }
            }
            disReadTimer.finish();
        } catch (Exception e) {
            throw new IOException();
        }
    }

    private void readDisHeaders(DataInputStream stream) throws Exception {
        this.numGenes = stream.readInt();
        if (stream.readBoolean() == true) this.expFilePath = stream.readUTF();
        this.dissimMethod = stream.readInt();
        if (dissimMethod == 1) this.dissimParams = stream.readUTF();
    }

    /**
   * Overrides the run method of the thread to create the cluster file
   */
    public void run() {
        writeClusterFile();
    }

    /**
   * starts the thread and begins the creation of the cluster file
   */
    public void start() {
        cancel = false;
        completed = false;
        over = false;
        super.start();
    }

    /**
   * Must be overriden by cluster classes to provide the implementation for creating and writing
   * a cluster file
   */
    public abstract void writeClusterFile();

    /**
   * returns the number of genes in dissimilarity file read
   * @return number of genes in dissimilarity file read
   */
    public int getNumberOfGenes() {
        return numGenes;
    }

    /**
   * returns the method used in dissimilarity file
   * @return method used in dissimilarity file
   */
    public int getDisMethod() {
        return dissimMethod;
    }

    /**
   * returns the number of dissimilarities that existed in dissimlarity file
   * @return number of dissimilarities that existed in dissimlarity file
   */
    public int getNumberOfPairs() {
        return numPairs;
    }

    /**
   * returns the parameters of dissimlarity file
   * @return parameters of dissimlarity file
   */
    public String getDisParams() {
        return dissimParams;
    }

    /**
   *returns the original expression file
   * @return original expression file
   */
    public String getExpressionFile() {
        return expFilePath;
    }

    /**
   * returns the array of label names for genes
   * @return array of label names for genes
   */
    public String[] getLabels() {
        Object o[] = labels.toArray();
        String s[] = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            s[i] = o[i].toString();
        }
        return s;
    }

    /**
   * returns an ArrayList of label names for genes
   * @return ArrayList of label names for genes
   */
    public ArrayList getListOfLabels() {
        return labels;
    }

    /**
   * returns the array of dissimilarity data
   * @return array of dissimilarity data
   */
    public float[][] getDisData() {
        return dys;
    }

    /**
   * returns a linked list of dissimilarity data
   * @return linked list of dissimilarity data
   */
    public LinkedList getDisDataInList() {
        LinkedList al = new LinkedList();
        for (int i = 0; i < dys.length; i++) {
            LinkedList innerList = new LinkedList();
            al.add(innerList);
            for (int j = 0; j < dys[i].length; j++) {
                innerList.add(new Float(dys[i][j]));
            }
        }
        return al;
    }

    /**
   * returns the string representation of the dissimilarity method
   * @param dis dissimilairty method
   * @return string representation of the dissimilarity method
   */
    protected String getStringDisMethod(int dis) {
        if (dis == 0) return new String("1-Correlation"); else if (dis == 1) return new String("lp"); else if (dis == 2) return new String("1-Jackknife"); else return null;
    }

    /**
     * returns whether or not the new file exists
     * @param filename dissimilarity filename
     * @return whether or not the new file exists
     */
    protected boolean testInFile(String filename) {
        File testFile = new File(filename);
        if (!testFile.exists()) return false; else return true;
    }

    /**
     * returns whether or not the new cluster filename is valid
     * @param writeOutFile new cluster filename
     * @return whether or not the new cluster filename is valid
     */
    protected boolean testWriteOutFile(String writeOutFile) {
        File testFile = new File(writeOutFile);
        if (testFile.isDirectory()) return false; else if (!testFile.isFile()) return false; else return true;
    }

    /**
     * writes a common header of information for a cluster file
     * @param outfile new cluster filename
     * @param genes number of genes
     * @param expPath original expression file
     * @param dismethod dissimilairty method
     * @param disparams dissimlarity parameters
     * @param disfile dissimlarity file
     * @param clusttype cluster type
     * @param clustparams cluster parameters
     * @param comments any extra comments
     * @throws Exception if header cannot be written to the file
     */
    public void writeHeaders(String outfile, int genes, String expPath, int dismethod, String disparams, String disfile, String clusttype, String clustparams, String comments) throws Exception {
        BufferedWriter stream = new BufferedWriter(new FileWriter(outfile));
        stream.write("/********************CLUSTERFILEHEADER******************\n");
        stream.write("*Numgenes:\t" + genes + "\n");
        if (expPath != null) {
            stream.write("*ExpFile:\t" + expPath + "\n");
        } else stream.write("*ExpFile:\tnull\n");
        stream.write("*DisMeth:\t" + getStringDisMethod(dismethod) + "\n");
        if (disparams != null) {
            stream.write("*Params:\t" + disparams + "\n");
        } else stream.write("*Params:\tnull\n");
        stream.write("*DisFile:\t" + disfile + "\n");
        stream.write("*ClustMeth:\t" + clusttype + "\n");
        stream.write("*Params:\t" + clustparams + "\n");
        stream.write("*Comments:\t" + comments + "\n");
        stream.write("*******************************************************/\n");
        stream.close();
    }

    /**
     * static method that can be called to read the header created using the common method provided
     *
     * <br><br> The array consists of:<br>
     * [1] number of genes<br>
     * [2] original expression file path<br>
     * [3] dissimilarity method <br>
     * [4] dissimlarity parameters <br>
     * [5] dissimlarity file <br>
     * [6] cluster method <br>
     * [7] cluster parameters <br>
     *
     * @param clustFile filename of a clusterfile
     * @return string array of information stored in a cluster file header
     * @throws Exception if cannot read the header of the file
     */
    public static String[] readHeaders(String clustFile) throws Exception {
        String info[] = new String[7];
        try {
            BufferedReader stream = new BufferedReader(new FileReader(clustFile));
            CharArrayWriter charwriter = new CharArrayWriter();
            for (int i = 0; i < 7; i++) {
                while (stream.read() != '\t') {
                }
                for (char ch = (char) stream.read(); ch != '\n'; ch = (char) stream.read()) {
                    charwriter.write(ch);
                }
                info[i] = charwriter.toString();
                charwriter.reset();
            }
        } catch (Exception e) {
            for (int i = 0; i < 7; i++) info[i] = "";
        }
        return info;
    }

    /**
     * starts the execution of the cluster - required for the executable interface
     * @throws DidNotFinishException if process failed
     */
    public void execute() throws DidNotFinishException {
        this.start();
        while (!over) {
        }
        if (cancel || !completed) throw new DidNotFinishException();
    }

    /**
     * returns whether or not cluster process has finished
     * @return whether or not cluster process has finished
     */
    public boolean isFinished() {
        return over;
    }
}
