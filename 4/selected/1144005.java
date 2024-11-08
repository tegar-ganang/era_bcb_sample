package org.systemsbiology.PIPE2.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.systemsbiology.PIPE2.client.GOTableService;
import org.systemsbiology.PIPE2.domain.Namelist;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class GOTableServiceImpl extends RemoteServiceServlet implements GOTableService {

    public static final String PIPE2_TOMCAT_GO_DIRECTORY = "PIPEletResourceDir/GOTableEnrichment/";

    public static final String PIPE2_PBS_GO_DIR = "/serum/analysis/PIPE2/GOEnrichment/";

    private static final String TOMCAT_BASE_DIR = "/usr/local/apache-tomcat-6.0.29/webapps/PIPE2/";

    private static final String GO_SCRIPT_NAME = "GOEnrichmentScript.R";

    private static final String WEBSTART_FILES_DIR = "webstartFiles";

    /**
     * initial submission of a new GO enrichment job.  Creates the params.txt file
     * and the submitJobToCluster.csh files in a new directory
     *
     * @param namelist
     * @param enrichmentType
     * @return
     * @throws Exception
    */
    public String SubmitGOEnrichment(Namelist namelist, String enrichmentType) throws Exception {
        if (!goEnrichmentServiceAvailable()) throw new Exception("GO Enrichment Service temporarily unavailable.  If proplem persists, please contact administrators.");
        String directoryName = "";
        String outputFilename = namelist.getName().replaceAll(" ", "_") + "_" + enrichmentType.replaceAll(" ", "_") + ".xls";
        try {
            directoryName = makeNewDirectory(enrichmentType, namelist.getSpecies());
            File pbsDirectory = new File(PIPE2_PBS_GO_DIR + directoryName);
            File finalLocationDirectory = new File(TOMCAT_BASE_DIR + PIPE2_TOMCAT_GO_DIRECTORY);
            System.out.println(PIPE2_PBS_GO_DIR + directoryName);
            pbsDirectory.mkdir();
            writeParamFile(namelist, enrichmentType, pbsDirectory, outputFilename, "params.txt");
            writeResultsFileHeading(namelist, enrichmentType, pbsDirectory, outputFilename);
            writeClusterSubmitionScript(pbsDirectory, "submitJobToCluster.csh");
            copy_file(finalLocationDirectory.toString() + File.separator + GO_SCRIPT_NAME, pbsDirectory.toString() + File.separator + GO_SCRIPT_NAME);
            copyDirectory(new File(finalLocationDirectory.toString() + File.separator + WEBSTART_FILES_DIR), new File(pbsDirectory.toString() + File.separator + WEBSTART_FILES_DIR));
            submitJobToCluster(pbsDirectory);
        } catch (IOException e) {
            throw new Exception("GO Enrichment Service temporarily unavailable.  If problem persists, please contact administrators.  Error id: 41380 - " + e.toString());
        }
        return directoryName + "-" + estimateTimeToCompletion(namelist.getNames().length, namelist.getSpecies(), enrichmentType) + "-" + outputFilename;
    }

    /**
     * If R script has completed, send results, if not, send "nope"
     *
     * @param refid the id of the GO enrichment job to check
     * @return
     */
    public String[][] checkGOJobStatus(String refid, String resultsFilename) throws Exception {
        File completeIndicatorFile = new File(PIPE2_PBS_GO_DIR + refid + File.separator, "COMPLETE");
        File errorFile = new File(PIPE2_PBS_GO_DIR + refid + File.separator, "ERROR");
        File resultsFile;
        if (completeIndicatorFile.exists()) {
            copyDirectory(new File(PIPE2_PBS_GO_DIR + refid), new File(TOMCAT_BASE_DIR + PIPE2_TOMCAT_GO_DIRECTORY + refid));
            resultsFile = new File(TOMCAT_BASE_DIR + PIPE2_TOMCAT_GO_DIRECTORY + refid + File.separator, resultsFilename);
            deleteDir(new File(PIPE2_PBS_GO_DIR + refid));
            deleteDir(new File(TOMCAT_BASE_DIR + PIPE2_TOMCAT_GO_DIRECTORY + refid + WEBSTART_FILES_DIR));
            return parseResultsFile(resultsFile);
        } else if (errorFile.exists()) {
            throw new Exception(parseErrorFile(errorFile));
        }
        return new String[][] { { "nope" } };
    }

    /**
     * go enrichment completed.  lets parse the results
     *
     * @param resultsFile
     * @return
     * @throws Exception
     */
    private String[][] parseResultsFile(File resultsFile) throws Exception {
        ArrayList<String[]> retVal = new ArrayList<String[]>();
        BufferedReader input = null;
        int i = 0;
        try {
            input = new BufferedReader(new FileReader(resultsFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().equals("")) {
                    if (i++ > 0) retVal.add(line.split("\t"));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new Exception("Results file could not be found: " + ex.toString());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new Exception("Results file could not be found: " + ex.toString());
            }
        }
        return retVal.toArray(new String[retVal.size()][]);
    }

    /**
     * There was an error in the go enrichment.  lets parse the error file
     *
     * @param errorFile
     * @return
     */
    private String parseErrorFile(File errorFile) throws Exception {
        StringBuffer sb = new StringBuffer();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(errorFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new Exception("Results file could not be found: " + ex.toString());
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new Exception("Results file could not be found: " + ex.toString());
            }
        }
        return sb.toString();
    }

    public static void copy_file(String fromFileName, String toFileName) throws IOException {
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

    public void copyDirectory(File srcPath, File dstPath) throws IOException {
        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {
            if (!srcPath.exists()) {
                System.out.println("File or directory does not exist.");
                System.exit(0);
            } else {
                InputStream in = new FileInputStream(srcPath);
                OutputStream out = new FileOutputStream(dstPath);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
        System.out.println("Directory copied.");
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private void writeResultsFileHeading(Namelist namelist, String enrichmentType, File directory, String outputFilename) throws IOException {
        File outputFile = new File(directory, outputFilename);
        FileWriter writer = new FileWriter(outputFile);
        writer.write("#List Name: " + namelist.getName());
        writer.write("\n#Length: " + namelist.getNames().length);
        writer.write("\n#Organism: " + namelist.getSpecies());
        writer.write("\n#GO Enrichment Type: " + enrichmentType + "\n\n");
        StringBuffer genes = new StringBuffer();
        genes.append("#Genes: ");
        for (int g = 0; g < namelist.getNames().length; g++) {
            if (g > 0) genes.append("; ");
            genes.append(namelist.getNames()[g].trim());
        }
        writer.write(genes.toString());
        writer.write("\n\n");
        writer.close();
        System.out.println("wrote " + outputFilename);
    }

    private void submitJobToCluster(File directory) throws IOException {
        try {
            String qsub_str;
            Process qsub_proc = Runtime.getRuntime().exec("ssh -x -i ~/.ssh/id_dsa corra@moog.systemsbiology.net qsub " + directory.toString() + File.separator + "submitJobToCluster.csh");
            DataInputStream ls_in = new DataInputStream(qsub_proc.getInputStream());
            try {
                while ((qsub_str = ls_in.readLine()) != null) {
                    System.out.println(qsub_str);
                }
            } catch (IOException e) {
                System.exit(0);
            }
        } catch (IOException e1) {
            System.err.println(e1);
        }
    }

    /**
     * writes the script to be used when the job is submitted to the cluster
     *
     * @param directory
     * @param finalLocationDirectory
     * @param pbsScriptFilename      @throws java.io.IOException
     */
    private void writeClusterSubmitionScript(File pbsDirectory, String pbsScriptFilename) throws IOException {
        File pbsFile = new File(pbsDirectory, pbsScriptFilename);
        FileWriter writer = new FileWriter(pbsFile);
        String processName = pbsDirectory.toString().substring(pbsDirectory.toString().lastIndexOf(File.separator) + 1, pbsDirectory.toString().length());
        writer.write("#!/bin/csh\n\n#PBS -N " + processName + "\n");
        writer.write("#PBS -M hramos@systemsbiology.org\n#PBS -m abe\n");
        writer.write("#PBS -o " + PIPE2_PBS_GO_DIR + "PBSoutput/PIPE2.oe\n");
        writer.write("#PBS -l vmem=4294967296\n\n");
        writer.write("cd " + pbsDirectory.toString() + "\n");
        writer.write("R --no-save < GOEnrichmentScript.R\n");
        writer.close();
    }

    /**
     * Given the type of enrichment and the organism, create a unique identifier with a timestamp
     *
     * @param enrichmentType
     * @param organism
     * @return
     */
    private String makeNewDirectory(String enrichmentType, String organism) {
        String fixedOrganism = "organismUnknown";
        if (organism.equalsIgnoreCase("Homo sapiens") || organism.equalsIgnoreCase("human")) fixedOrganism = "human"; else if (organism.equalsIgnoreCase("mus musculus") || organism.equalsIgnoreCase("mouse")) fixedOrganism = "mouse"; else if (organism.equalsIgnoreCase("Saccharomyces cerevisiae") || organism.equalsIgnoreCase("yeast")) fixedOrganism = "yeast"; else if (organism.equalsIgnoreCase("Rattus norvegicus") || organism.equalsIgnoreCase("rat")) fixedOrganism = "rat";
        long time = new Date().getTime();
        String timeString = (new Long(time)).toString();
        String result = fixedOrganism + "_" + enrichmentType + "_" + timeString;
        return (result);
    }

    /**
     * writes the parameters file that the R process will read and with it launch the process
     *
     * @param namelist
     * @param enrichmentType
     * @param directory
     * @param filename
     * @throws Exception
     */
    private void writeParamFile(Namelist namelist, String enrichmentType, File directory, String outFilename, String filename) throws Exception {
        File paramFile = new File(directory, filename);
        FileWriter writer = new FileWriter(paramFile);
        writer.write(namelist.getSpecies() + "\n" + enrichmentType + "\n" + outFilename + "\n");
        for (int g = 0; g < namelist.getNames().length; g++) writer.write(namelist.getNames()[g] + "\n");
        writer.close();
    }

    /**
     * check to see if GO Enrichment service is currently available
     *
     * @return true if service is available, exception if not
     */
    public boolean goEnrichmentServiceAvailable() {
        File statusFile = new File(TOMCAT_BASE_DIR + PIPE2_TOMCAT_GO_DIRECTORY, "GO_ENRICHMENT_UNAVAILABLE");
        if (statusFile.exists()) return false; else return true;
    }

    /**
     * given a GOEnrichmentOperationMetaData object will all the relevant data fields filled, this will
     * fill the estimatedTimeToCompletion field.  These values are based off manually gathered statistics
     *
     * @param numGenes
     * @param organism
     * @param ontologyType
     */
    private int estimateTimeToCompletion(int numGenes, String organism, String ontologyType) {
        if (ontologyType == null || organism == null || numGenes == 0) return 1;
        if (organism.equals("Homo sapiens")) {
            if (numGenes < 76) {
                if (ontologyType.equals("BP")) {
                    return 85;
                } else if (ontologyType.equals("MF")) {
                    return 60;
                } else if (ontologyType.equals("CC")) {
                    return 35;
                }
            } else if (numGenes < 151) {
                if (ontologyType.equals("BP")) {
                    return 100;
                } else if (ontologyType.equals("MF")) {
                    return 65;
                } else if (ontologyType.equals("CC")) {
                    return 45;
                }
            } else if (numGenes < 306) {
                if (ontologyType.equals("BP")) {
                    return 125;
                } else if (ontologyType.equals("MF")) {
                    return 65;
                } else if (ontologyType.equals("CC")) {
                    return 45;
                }
            } else if (numGenes < 611) {
                if (ontologyType.equals("BP")) {
                    return 160;
                } else if (ontologyType.equals("MF")) {
                    return 85;
                } else if (ontologyType.equals("CC")) {
                    return 55;
                }
            } else if (numGenes < 1222) {
                if (ontologyType.equals("BP")) {
                    return 250;
                } else if (ontologyType.equals("MF")) {
                    return 125;
                } else if (ontologyType.equals("CC")) {
                    return 80;
                }
            } else {
                if (ontologyType.equals("BP")) {
                    return 290;
                } else if (ontologyType.equals("MF")) {
                    return 165;
                } else if (ontologyType.equals("CC")) {
                    return 120;
                }
            }
        } else if (organism.equals("Mus musculus")) {
            if (numGenes < 76) {
                if (ontologyType.equals("BP")) {
                    return 175;
                } else if (ontologyType.equals("MF")) {
                    return 107;
                } else if (ontologyType.equals("CC")) {
                    return 65;
                }
            } else if (numGenes < 151) {
                if (ontologyType.equals("BP")) {
                    return 175;
                } else if (ontologyType.equals("MF")) {
                    return 107;
                } else if (ontologyType.equals("CC")) {
                    return 65;
                }
            } else if (numGenes < 306) {
                if (ontologyType.equals("BP")) {
                    return 171;
                } else if (ontologyType.equals("MF")) {
                    return 116;
                } else if (ontologyType.equals("CC")) {
                    return 62;
                }
            } else if (numGenes < 611) {
                if (ontologyType.equals("BP")) {
                    return 184;
                } else if (ontologyType.equals("MF")) {
                    return 120;
                } else if (ontologyType.equals("CC")) {
                    return 55;
                }
            } else if (numGenes < 1222) {
                if (ontologyType.equals("BP")) {
                    return 255;
                } else if (ontologyType.equals("MF")) {
                    return 145;
                } else if (ontologyType.equals("CC")) {
                    return 85;
                }
            } else {
                if (ontologyType.equals("BP")) {
                    return 400;
                } else if (ontologyType.equals("MF")) {
                    return 300;
                } else if (ontologyType.equals("CC")) {
                    return 150;
                }
            }
        } else if (organism.equals("Rattus norvegicus") || organism.equals("Saccharomyces cerevisiae")) {
            if (numGenes < 76) {
                if (ontologyType.equals("BP")) {
                    return 130;
                } else if (ontologyType.equals("MF")) {
                    return 108;
                } else if (ontologyType.equals("CC")) {
                    return 68;
                }
            } else if (numGenes < 151) {
                if (ontologyType.equals("BP")) {
                    return 155;
                } else if (ontologyType.equals("MF")) {
                    return 108;
                } else if (ontologyType.equals("CC")) {
                    return 65;
                }
            } else if (numGenes < 306) {
                if (ontologyType.equals("BP")) {
                    return 175;
                } else if (ontologyType.equals("MF")) {
                    return 115;
                } else if (ontologyType.equals("CC")) {
                    return 70;
                }
            } else if (numGenes < 611) {
                if (ontologyType.equals("BP")) {
                    return 195;
                } else if (ontologyType.equals("MF")) {
                    return 120;
                } else if (ontologyType.equals("CC")) {
                    return 75;
                }
            } else if (numGenes < 1222) {
                if (ontologyType.equals("BP")) {
                    return 330;
                } else if (ontologyType.equals("MF")) {
                    return 220;
                } else if (ontologyType.equals("CC")) {
                    return 100;
                }
            } else {
                if (ontologyType.equals("BP")) {
                    return 350;
                } else if (ontologyType.equals("MF")) {
                    return 290;
                } else if (ontologyType.equals("CC")) {
                    return 150;
                }
            }
        }
        return 120;
    }
}
