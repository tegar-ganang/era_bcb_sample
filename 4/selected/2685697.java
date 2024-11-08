package hadit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import nutils.ArrayUtils;
import nutils.IOUtils;

public class CopyNumberFileConcat {

    public static final String ConcatString = "All";

    public static void concatFiles(String pathPrefix, String pathSuffix, ArrayList<Integer> chromRangeDesired, ArrayList<Integer> cancerTypesDesired, ArrayList<Integer> windowLengths) {
        for (int cancerIndex = 0; cancerIndex < cancerTypesDesired.size(); cancerIndex++) {
            String cancerTypeStr = CopyNumberCancerMap.CancerTypes[cancerTypesDesired.get(cancerIndex).intValue()];
            for (int windowIndex = 0; windowIndex < windowLengths.size(); windowIndex++) {
                int windowLength = windowLengths.get(windowIndex).intValue();
                String fullPrefix = pathPrefix + cancerTypeStr + ".Win." + windowLength + ".Chr_";
                concatFilesSingle(fullPrefix, pathSuffix, chromRangeDesired);
            }
        }
    }

    public static void concatFilesSingle(String pathPrefix, String pathSuffix, ArrayList<Integer> chromRangeDesired) {
        String outFilename = (new File(pathPrefix)).getParent() + File.separatorChar + (new File(pathPrefix)).getName() + ConcatString + pathSuffix;
        char[] cbuf = new char[4194304];
        try {
            BufferedWriter out = IOUtils.getBufferedWriter(outFilename);
            for (int i = 0; i < chromRangeDesired.size(); i++) {
                String inFilename = pathPrefix + chromRangeDesired.get(i).intValue() + pathSuffix;
                BufferedReader in = IOUtils.getBufferedReader(inFilename);
                int numBytesRead = 0;
                while (numBytesRead != -1) {
                    out.write(cbuf, 0, numBytesRead);
                    numBytesRead = in.read(cbuf);
                }
                in.close();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /** Returns if file copying succeeded or not. */
    public static boolean fileCopy(String fromFileName, String toFileName) {
        boolean success = false;
        try {
            copy(fromFileName, toFileName);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return success;
    }

    private static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
            System.out.print("Overwrite existing file " + toFile.getName() + "? (Y/N): ");
            System.out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String response = in.readLine();
            if (!response.equals("Y") && !response.equals("y")) throw new IOException("FileCopy: " + "existing file was not overwritten.");
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    private static void PrintArgumentHelp() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("CopyNumberFileConcat Usage:\n");
        sb.append("---------------------------\n");
        sb.append("1)\t-batch   path_prefix path_suffix chrom_start chrom_end {Cancer_Types_Desired_(no_spaces)} {Window_Sizes_Desired_(no_spaces)}\n");
        sb.append("2)\t-single  path_prefix path_suffix chrom_start chrom_end");
        System.out.println(sb.toString());
    }

    private static void ParseArguments(String[] args) {
        if (args[0].equals("-batch")) {
            if (args.length < 5) {
                PrintArgumentHelp();
            } else {
                CopyNumberFileConcat.concatFiles(args[1], args[2], ArrayUtils.getIntListFromStringForm(args[3]), ArrayUtils.getIntListFromStringForm(args[4]), ArrayUtils.getIntListFromStringForm(args[5]));
            }
        } else if (args[0].equals("-single")) {
            if (args.length < 4) {
                PrintArgumentHelp();
            } else {
                CopyNumberFileConcat.concatFilesSingle(args[1], args[2], ArrayUtils.getIntListFromStringForm(args[3]));
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        ParseArguments(args);
    }
}
