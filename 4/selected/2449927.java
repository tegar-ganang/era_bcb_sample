package GlobeGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Dale R. Tourtelotte
 */
public class RezConfigGenerator {

    private File sourceDirectory;

    private File destinationDirectory;

    private File masterConfigFile;

    private int cols;

    private int rows;

    private File[][] fileArray;

    /**
     * @param args the command line arguments in this specific order:
     * rows, cols, sourceDirectory, destinationDirectory, masterConfigFile, and
     * the files in geographic order from north west to south east
     */
    public static void main(String[] args) {
        int argIndex = 0;
        int rows = Integer.parseInt(args[argIndex]);
        int cols = Integer.parseInt(args[++argIndex]);
        File sourceDirectory = new File(args[++argIndex]);
        File destinationDirectory = new File(args[++argIndex]);
        File[][] fileArray = new File[rows][cols];
        File masterConfigFile = new File(args[++argIndex]);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                fileArray[i][j] = new File(args[++argIndex]);
            }
        }
        new RezConfigGenerator(sourceDirectory, destinationDirectory, cols, rows, masterConfigFile, fileArray);
    }

    private String configFile;

    private File configFileFolder;

    public RezConfigGenerator(File sourceDirectory, File destinationDirectory, int cols, int rows, File masterConfigFile, File[][] fileArray) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.cols = cols;
        this.rows = rows;
        this.masterConfigFile = masterConfigFile;
        this.fileArray = fileArray;
        this.configFileFolder = new File(destinationDirectory.toString() + File.separator + "ConfigFileFolder");
        configFileFolder.mkdir();
        writeConfigurationFile();
    }

    public RezConfigGenerator(File sourceDirectory, File destinationDirectory, int cols, int rows, File masterConfigFile, File file) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
        this.cols = cols;
        this.rows = rows;
        this.masterConfigFile = masterConfigFile;
        this.fileArray = new File[1][1];
        this.configFileFolder = new File(destinationDirectory.toString() + File.separator + "ConfigFileFolder");
        configFileFolder.mkdir();
        this.fileArray[0][0] = file;
        writeConfigurationFile();
    }

    private void writeConfigurationFile() {
        String nextLine = "ERROR: FILE INPUT";
        File firstElement = fileArray[0][0];
        String filename = firstElement.getParentFile().getName() + firstElement.getName().substring(0, firstElement.getName().lastIndexOf('.'));
        configFile = configFileFolder + File.separator + filename + ".txt";
        FileReader fileReader = null;
        PrintWriter configWriter = null;
        try {
            fileReader = new FileReader(masterConfigFile);
            configWriter = new PrintWriter(configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader bufReader = new BufferedReader(fileReader);
        while (true) {
            try {
                nextLine = bufReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nextLine == null) {
                break;
            }
            if (nextLine.contains("sourceDirectory ./")) {
                nextLine = "sourceDirectory " + sourceDirectory.toString().replace('\\', '/') + "/";
            } else if (nextLine.contains("destinationDirectory ./")) {
                nextLine = "destinationDirectory " + destinationDirectory.toString().replace('\\', '/') + "/";
            } else if (nextLine.contains("TileArrays")) {
                nextLine = "TileArrays " + rows * cols;
            } else if (nextLine.contains("TileXDim")) {
                nextLine = "TileXDim " + cols;
            } else if (nextLine.contains("TileYDim")) {
                nextLine = "TileYDim " + rows;
            }
            configWriter.println(nextLine);
        }
        try {
            bufReader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (int i = 0; i < rows; ++i) {
            configWriter.print("TileList ");
            for (int j = 0; j < cols; ++j) {
                configWriter.print(fileArray[i][j].getParentFile().getName() + File.separator + fileArray[i][j].getName() + " ");
            }
            configWriter.println();
        }
        configWriter.close();
    }

    /**
     * @return the outFile
     */
    public String getConfigFile() {
        return configFile;
    }
}
