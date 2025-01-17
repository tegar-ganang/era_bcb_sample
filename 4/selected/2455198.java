package ra.lajolla.utilities.evaluation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class CathFileListParserAndCopier {

    static String CathDir = "/media/truecrypt1/benchmarking/datasets/CathDomainPdb.S35.v3.2.0/";

    static String completeFileWithDirToCathFileList = "/media/truecrypt1/benchmarking/datasets/CathDomainList.S35";

    /** Where to copy the filtered stuff: */
    static String outputDir = "/media/truecrypt1/benchmarking/datasets/CathDomainPdb.S35.v3.2.0_filtered_by_H/";

    public static void main(String[] a) {
        ArrayList<String> allFilesToBeCopied = new ArrayList<String>();
        new File(outputDir).mkdirs();
        try {
            FileReader fis = new FileReader(completeFileWithDirToCathFileList);
            BufferedReader bis = new BufferedReader(fis);
            String line = "";
            String currentCombo = "";
            while ((line = bis.readLine()) != null) {
                String[] allEntries = line.split("\\s+");
                String fileName = allEntries[0];
                String thisCombo = allEntries[1] + allEntries[2] + allEntries[3] + allEntries[4];
                if (currentCombo.equals(thisCombo)) {
                } else {
                    System.out.println("merke: " + fileName);
                    allFilesToBeCopied.add(fileName);
                    currentCombo = thisCombo;
                }
            }
            System.out.println(allFilesToBeCopied.size());
            for (String file : allFilesToBeCopied) {
                try {
                    FileChannel srcChannel = new FileInputStream(CathDir + file).getChannel();
                    FileChannel dstChannel = new FileOutputStream(outputDir + file).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class CATHEntry {

        String filename;

        String C;

        String A;

        String T;

        String H;

        CATHEntry(String filename, String C, String A, String T, String H) {
            this.filename = filename;
            this.C = C;
            this.A = A;
            this.T = T;
            this.H = H;
        }

        String getFilename() {
            return filename;
        }

        String getC() {
            return C;
        }

        String getA() {
            return A;
        }

        String getT() {
            return T;
        }

        String getH() {
            return H;
        }
    }
}
