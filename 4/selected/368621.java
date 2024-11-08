package net.cordova.justus.coverage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.cordova.justus.coverage.gui.CoverageGUI;
import net.cordova.justus.coverage.gui.CoverageLoad;
import net.cordova.justus.coverage.task.JustusCoverageTask;
import net.cordova.justus.profiler.ExecutionProgram;
import net.cordova.justus.profiler.ProfileInfo;
import net.cordova.justus.profiler.ProfileLoader;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

public class JustusCoverage {

    private static Logger log = Logger.getLogger(JustusCoverage.class.getName());

    public static CoverageInfo loadCoverage(File profFile, File sourceDir, File coverCfg, CoverageProcessingListener listener) throws JustusCoverageException {
        ArrayList<Category> catList;
        log.fine("Loading Configuration");
        if (coverCfg != null) {
            Document doc = null;
            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(coverCfg);
            } catch (Exception e) {
                throw new JustusCoverageException("Coverage Configuration file corrupted: " + coverCfg);
            }
            if (listener != null) listener.loadingCategories();
            catList = Category.loadCategories(doc);
        } else {
            catList = new ArrayList<Category>();
        }
        log.fine("Processing Profiler Information...");
        HashMap<Integer, ExecutionProgram> profileInfo;
        profileInfo = ProfileLoader.loadProfile(profFile, sourceDir, listener);
        ArrayList<ExecutionProgram> programs = new ArrayList<ExecutionProgram>(profileInfo.values());
        log.fine("Processing Coverage...");
        CoverageInfo info = CoverageInfo.processCoverage(catList, programs, listener);
        return info;
    }

    public static void saveCoverage(CoverageInfo coverInfo, String resultFile) throws JustusCoverageException {
        try {
            File rFile = new File(resultFile);
            if (!rFile.getParentFile().exists()) throw new IOException("Invalid destination folder: " + rFile.getParentFile());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            DOMImplementation di = db.getDOMImplementation();
            Document doc = di.createDocument(null, "JustusCoverage", null);
            Element root = doc.getDocumentElement();
            ProcessingInstruction instructions = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"template.xsl\"");
            doc.insertBefore(instructions, root);
            for (Category category : coverInfo.getCategories()) {
                Element catNode = doc.createElement("category");
                catNode.setAttribute("name", category.getName());
                catNode.setAttribute("minCoverageRate", String.valueOf(category.getCoverRate()));
                int failCounter = 0;
                int successCounter = 0;
                for (ExecutionProgram program : coverInfo.getPrograms(category)) {
                    Element item = doc.createElement("program");
                    item.setAttribute("name", program.getProgramName().replace('\\', '/'));
                    boolean success = program.getCoverage() * 100 >= category.getCoverRate();
                    if (success) {
                        item.setAttribute("status", "success");
                        successCounter++;
                    } else {
                        item.setAttribute("status", "fail");
                        failCounter++;
                    }
                    int failStatements = 0;
                    int successStatements = 0;
                    int ignoredStatements = 0;
                    for (ProfileInfo line : program.getSourceInfo()) {
                        if (!line.isValidForCoverage()) ignoredStatements++; else if (line.getUsage() > 0) successStatements++; else failStatements++;
                        Element l = doc.createElement("statement");
                        l.setAttribute("line", String.valueOf(line.getCommandLine()));
                        l.setAttribute("ignored", String.valueOf(!line.isValidForCoverage()));
                        l.setAttribute("usage", String.valueOf(line.getUsage()));
                        l.setAttribute("commandType", line.getStatement().getType());
                        item.appendChild(l);
                        if (line.getCommand() != null) {
                            CDATASection cData = doc.createCDATASection(line.getStatement().getCommand().trim());
                            l.appendChild(cData);
                        }
                    }
                    item.setAttribute("coveredStatements", String.valueOf(successStatements));
                    item.setAttribute("uncoveredStatements", String.valueOf(failStatements));
                    item.setAttribute("ignoredStatements", String.valueOf(ignoredStatements));
                    catNode.appendChild(item);
                }
                catNode.setAttribute("failCounter", String.valueOf(failCounter));
                catNode.setAttribute("successCounter", String.valueOf(successCounter));
                catNode.setAttribute("status", (failCounter > 0) ? "fail" : "success");
                root.appendChild(catNode);
            }
            DOMSource ds = new DOMSource(doc);
            StreamResult sr = new StreamResult(rFile);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.transform(ds, sr);
            copyFile(JustusCoverageTask.class.getResourceAsStream("/resources/templates/default/default.css"), rFile.getParentFile() + "/default.css");
            copyFile(JustusCoverageTask.class.getResourceAsStream("/resources/templates/default/template.xsl"), rFile.getParentFile() + "/template.xsl");
            copyFile(JustusCoverageTask.class.getResourceAsStream("/resources/templates/default/coverage.js"), rFile.getParentFile() + "/coverage.js");
        } catch (Exception e) {
            throw new JustusCoverageException(e.getMessage());
        }
    }

    private static void copyFile(InputStream in, String outFile) throws IOException {
        FileOutputStream outputFile = new FileOutputStream(outFile);
        byte buffer[] = new byte[1024];
        while (true) {
            int readBytes = in.read(buffer);
            if (readBytes < 0) break;
            outputFile.write(buffer, 0, readBytes);
        }
        outputFile.flush();
        outputFile.close();
    }

    public static void main(String[] args) {
        System.out.println("Justus Coverage\n");
        String profileFile = System.getProperty("profileFile");
        String sourceFolder = System.getProperty("sourceFolder");
        String resultFile = System.getProperty("resultFile");
        String coverageConfig = System.getProperty("coverageConfig");
        if (profileFile == null) profileFile = "";
        if (sourceFolder == null) sourceFolder = "";
        if (resultFile == null) resultFile = "";
        if (coverageConfig == null) coverageConfig = "";
        File pFile = new File(profileFile);
        File sFolder = new File(sourceFolder);
        File cConfig = new File(coverageConfig);
        if (args.length == 0) {
            showParameters();
            System.exit(1);
        }
        if (!args[0].equalsIgnoreCase("view") && !args[0].equalsIgnoreCase("save")) {
            System.err.println("** Invalid command: " + args[0]);
            showParameters();
            System.exit(2);
        }
        if (!pFile.exists()) {
            System.err.println("** Profiler result file must be valid.");
            showParameters();
            System.exit(3);
        }
        if (!sFolder.exists()) {
            System.err.println("** Profiler source folder output must be valid.");
            showParameters();
            System.exit(4);
        }
        if (!cConfig.exists()) {
            System.err.println("** Justus Coverage category file must be valid.");
            showParameters();
            System.exit(5);
        }
        if (resultFile == null && args[0].equalsIgnoreCase("save")) {
            System.err.println("** Justus Coverage result file must be informed.");
            showParameters();
            System.exit(6);
        }
        try {
            System.out.println("Processing coverage...");
            CoverageInfo info = JustusCoverage.loadCoverage(pFile, sFolder, cConfig, null);
            if (args[0].equalsIgnoreCase("save")) {
                System.out.println("Saving result file: " + resultFile);
                JustusCoverage.saveCoverage(info, resultFile);
            } else CoverageGUI.showCoverage(info);
        } catch (JustusCoverageException e) {
            e.printStackTrace();
        }
    }

    private static void showParameters() {
        System.out.println("JustusCoverage <parameters> net.cordova.justus.JustusCoverage <view|save>\n");
        System.out.println("Parameters:");
        System.out.println("\t-DprofileFile=<file>   - Progress Profiler's result file");
        System.out.println("\t-DsourceFolder=<dir>   - Progress Profiler's source output folder");
        System.out.println("\t-DresultFile<file>     - Justus Coverage categories configuration");
        System.out.println("\t-DcoverageConfig<file> - Justus Coverage result file");
    }
}
