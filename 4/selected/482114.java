package de.uniwue.tm.textruler.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import de.uniwue.tm.textmarker.engine.TextMarkerEngine;
import de.uniwue.tm.textruler.core.TextRulerToolkit;

/**
 * 
 * @author Tobias Hermann
 * 
 */
public class BatchRuleEvaluator {

    private static AnalysisEngine ae;

    private static String engineFile;

    private static String tempDir;

    private static String foldRootDirectory;

    private static int foldCount = 0;

    private static CAS sharedCAS;

    public static AnalysisEngine getAnalysisEngine() {
        if (ae == null) {
            String descriptorFile = engineFile;
            TextRulerToolkit.log("loading AE...");
            ae = TextRulerToolkit.loadAnalysisEngine(descriptorFile);
            ae.setConfigParameterValue(TextMarkerEngine.DEFAULT_FILTERED_MARKUPS, new String[0]);
            IPath path = new Path(tempDir + "/results.tm");
            ae.setConfigParameterValue(TextMarkerEngine.MAIN_SCRIPT, path.removeFileExtension().lastSegment());
            ae.setConfigParameterValue(TextMarkerEngine.SCRIPT_PATHS, new String[] { path.removeLastSegments(1).toPortableString() });
            try {
                ae.reconfigure();
            } catch (ResourceConfigurationException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ae;
    }

    private static File[] getXMIFileFromFolder(String folderName) {
        File folder = new File(folderName);
        File[] files = folder.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return (name.endsWith(".xmi"));
            }
        });
        return files;
    }

    /**
   * @param args
   */
    public static void main(String[] args) {
        tempDir = "/testinput/temp/";
        engineFile = "/Users/tobi/Documents/UniLaptop/Diplomarbeit/TestDataSets/withPosTags/Subset100/10fold/desc/lp2ergebnisrandomgiantEngine.xml";
        foldRootDirectory = "/Users/tobi/Documents/UniLaptop/Diplomarbeit/TestDataSets/withPosTags/9010_middle/";
        foldCount = 1;
        String slotNames[] = { "de.uniwue.ml.types.etime", "de.uniwue.ml.types.stime", "de.uniwue.ml.types.location", "de.uniwue.ml.types.speaker" };
        String algIDs[] = { "optimizedLP2" };
        for (int foldNumber = 0; foldNumber < foldCount; foldNumber++) {
            for (String slotName : slotNames) {
                for (String algID : algIDs) {
                    runRules(foldNumber, slotName, algID);
                }
            }
        }
    }

    public static void runRules(int foldNumber, String slotName, String algorithmID) {
        getAnalysisEngine();
        TextRulerToolkit.log("Testing Fold Number " + foldNumber + "\t  Slot: " + slotName + "\t  Algorithm: " + algorithmID);
        String inputFolder = foldRootDirectory + foldNumber + "/testing/withouttags/";
        String rulesFile = foldRootDirectory + foldNumber + "/learnResults/" + slotName + "/" + algorithmID + "/results.tm";
        String scriptFile = tempDir + "results.tm";
        File oldScriptFile = new File(scriptFile);
        if (oldScriptFile.exists()) {
            if (!oldScriptFile.delete()) {
                TextRulerToolkit.log("ERROR DELETING OLD SCRIPT FILE: " + scriptFile);
                return;
            }
        }
        if (!new File(rulesFile).exists()) {
            TextRulerToolkit.log("\tSKIPPED, no rules file not found!");
            return;
        }
        try {
            FileUtils.copyFile(new File(rulesFile), new File(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String outputFolder1 = foldRootDirectory + foldNumber + "/testing/markedFromRules";
        String outputFolder2 = foldRootDirectory + foldNumber + "/testing/markedFromRules/" + slotName;
        String outputFolder = foldRootDirectory + foldNumber + "/testing/markedFromRules/" + slotName + "/" + algorithmID;
        new File(outputFolder1).mkdir();
        new File(outputFolder2).mkdir();
        new File(outputFolder).mkdir();
        File[] inputFiles = getXMIFileFromFolder(inputFolder);
        for (File inputFile : inputFiles) {
            sharedCAS = TextRulerToolkit.readCASfromXMIFile(inputFile, ae, sharedCAS);
            try {
                ae.process(sharedCAS);
            } catch (AnalysisEngineProcessException e) {
                e.printStackTrace();
                return;
            }
            TextRulerToolkit.writeCAStoXMIFile(sharedCAS, TextRulerToolkit.addTrailingSlashToPath(outputFolder) + "fromRules_" + inputFile.getName());
        }
    }
}
