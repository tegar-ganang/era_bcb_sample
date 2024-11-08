package com.smartwish.documentburster.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.aop.Profiled;
import com.smartwish.documentburster.context.BurstingContext;
import com.smartwish.documentburster.scripting.Scripting;
import com.smartwish.documentburster.scripting.Scripts;
import com.smartwish.documentburster.sender.Sender;
import com.smartwish.documentburster.sender.factory.SenderFactory;
import com.smartwish.documentburster.settings.Settings;
import com.smartwish.documentburster.settings.model.Attachment;
import com.smartwish.documentburster.utils.Utils;
import com.smartwish.documentburster.variables.Variables;

public abstract class AbstractBurster implements Burstable {

    private static final int BURST_LIMIT_NO = 25;

    private static Log log = LogFactory.getLog(AbstractBurster.class);

    protected Scripting scripting;

    protected String filePath;

    protected String fileName;

    protected String configFilePath = null;

    protected Map<String, String> burstDocumentPaths;

    protected BurstingContext ctx;

    protected void initializeResources() throws Exception {
    }

    protected void closeResources() throws Exception {
    }

    @Profiled
    public List<String> parseBurstingMetaData() throws Exception {
        return parseBurstMetaDataFromInputDocument();
    }

    protected abstract List<String> parseBurstMetaDataFromInputDocument() throws Exception;

    @Profiled
    public void extractOutputFile() throws Exception {
        extractOutputBurstFile();
        ctx.numberOfExtractedFiles++;
    }

    protected abstract void extractOutputBurstFile() throws Exception;

    protected void executeController() throws Exception {
        scripting.executeScript(Scripts.CONTROLLER, ctx);
        ctx.variables = new Variables(fileName, ctx.settings.getLanguage(), ctx.settings.getCountry(), ctx.settings.getNumberOfUserVariables());
    }

    @Profiled(tag = "burst_{$0}_{$1}")
    public void burst(String pathToFile, boolean simulateDistribution) throws Exception {
        log.info("Bursting document: '" + pathToFile + "' ...");
        initializeBursting();
        filePath = pathToFile;
        fileName = FilenameUtils.getName(pathToFile);
        ctx.inputDocumentFilePath = pathToFile;
        try {
            executeController();
            scripting.executeScript(ctx.scripts.startBursting, ctx);
            initializeResources();
            List<String> parsedBurstTokens = parseBurstingMetaData();
            loadCustomSettings();
            if (ctx.burstTokens == null) ctx.burstTokens = parsedBurstTokens;
            log.debug("burstTokens = " + ctx.burstTokens);
            if (ctx.burstTokens.size() == 0) {
                throw new Exception("No burst tokens were provided or fetched for the document : " + pathToFile);
            } else {
                boolean isSendFiles = ctx.settings.isSendFiles();
                boolean doMore = true;
                int outputDocumentIndex = 1;
                ExecutorService concurrentDistributor = null;
                if ((isSendFiles) && (ctx.burstTokens.size() > 0)) {
                    int nThreads = ctx.settings.getConcurrentDistributionThreads();
                    concurrentDistributor = Executors.newFixedThreadPool(nThreads);
                }
                for (String token : ctx.burstTokens) {
                    ctx.token = token;
                    if (doMore) {
                        processReportForCurrentToken(isSendFiles, concurrentDistributor, simulateDistribution);
                        outputDocumentIndex++;
                        doMore = (outputDocumentIndex <= BURST_LIMIT_NO) ? true : false;
                    }
                }
                if (concurrentDistributor != null) {
                    concurrentDistributor.shutdown();
                    while (!concurrentDistributor.isTerminated()) {
                    }
                }
                boolean isDeleteFiles = ctx.settings.isDeleteFiles();
                if (isDeleteFiles) {
                    String backupFilePath = ctx.backupFolder + "/" + FilenameUtils.getName(pathToFile);
                    File backupFile = new File(backupFilePath);
                    if (!FileUtils.deleteQuietly(backupFile)) {
                        log.error("Failed to delete " + backupFilePath);
                    }
                }
                ctx.token = StringUtils.EMPTY;
                ctx.extractFilePath = StringUtils.EMPTY;
                scripting.executeScript(ctx.scripts.endBursting, ctx);
            }
            if (ctx.burstTokens.size() > BURST_LIMIT_NO) throw new Exception("Free version limit - " + "Free DocumentBurster can burst and distribute up to " + BURST_LIMIT_NO + " reports. If you need more please " + "purchase DocumentBurster - http://www.pdfburst.com/ ");
        } catch (Exception t) {
            log.error(t.getMessage(), t);
            throw t;
        } finally {
            closeResources();
        }
        log.info("Done bursting report: '" + pathToFile + "'.");
    }

    private void initializeBursting() {
        ctx = new BurstingContext();
        ctx.scripts = new Scripts();
        ctx.settings = new Settings();
        scripting = new Scripting();
        burstDocumentPaths = new HashMap<String, String>();
    }

    private void processReportForCurrentToken(boolean isSendFiles, ExecutorService concurrentDistributor, boolean simulateDistribution) throws Exception {
        String skip = ctx.variables.getUserVariables(ctx.token).get(Variables.SKIP).toString();
        ctx.skipCurrentFileDistribution = Boolean.valueOf(skip).booleanValue();
        log.debug("token = " + ctx.token + ", ctx.skipCurrentFileDistribution = " + ctx.skipCurrentFileDistribution);
        extractReport();
        processAttachments();
        if (isSendFiles && !ctx.skipCurrentFileDistribution) {
            distributeReport(concurrentDistributor, simulateDistribution);
        } else {
            if (isSendFiles && ctx.skipCurrentFileDistribution) ctx.numberOfSkippedFiles++;
            log.debug("Burst report " + ctx.extractFilePath + " was not sent to any recipient because {sendfiles} configuration is '" + isSendFiles + "' and ctx.skipCurrentFileDistribution is '" + ctx.skipCurrentFileDistribution + "'");
        }
    }

    private void extractReport() throws Exception {
        ctx.variables.put(Variables.BURST_TOKEN, ctx.token);
        ctx.variables.put(Variables.BURST_INDEX, ctx.numberOfExtractedFiles + 1);
        createRequiredFolders();
        backupFile();
        String burstFileName = Utils.getFileNameOfBurstDocument(ctx.settings.getBurstFileName(), ctx.token);
        ctx.extractFilePath = ctx.outputFolder + "/" + Utils.getStringFromTemplate(burstFileName, ctx.variables, ctx.token);
        scripting.executeScript(ctx.scripts.startExtractDocument, ctx);
        extractOutputFile();
        ctx.variables.put(Variables.EXTRACTED_FILE_PATH, ctx.extractFilePath);
        burstDocumentPaths.put(ctx.token, ctx.extractFilePath);
        scripting.executeScript(ctx.scripts.endExtractDocument, ctx);
        log.info("Document '" + ctx.extractFilePath + "' was extracted for token '" + ctx.token + "'");
    }

    private void processAttachments() throws Exception {
        ctx.attachments.clear();
        ctx.archiveFilePath = StringUtils.EMPTY;
        List<Attachment> attachments = ctx.settings.getAttachments();
        if (attachments.size() > 0) {
            for (Attachment attachment : attachments) {
                String path = Utils.getStringFromTemplate(attachment.path, ctx.variables, ctx.token);
                ctx.attachments.add(path);
            }
            if (ctx.settings.isArchiveAttachments()) {
                String archiveFileName = Utils.getStringFromTemplate(ctx.settings.getArchiveFileName(), ctx.variables, ctx.token);
                if (StringUtils.isNotEmpty(archiveFileName)) {
                    ctx.archiveFilePath = ctx.outputFolder + "/" + archiveFileName;
                    log.debug("Archiving attachments to '" + ctx.archiveFilePath + "'");
                    scripting.executeScript(ctx.scripts.archive, ctx);
                } else throw new Exception("You need to provide a valid 'archiveFileName'");
            }
        } else log.debug("There are no attachments defined for the token '" + ctx.token + "'!");
        log.debug("ctx.attachments = " + ctx.attachments);
    }

    private void distributeReport(ExecutorService concurrentDistributor, boolean simulateDistribution) throws Exception {
        List<Sender> senders = getSenders(simulateDistribution);
        if ((senders != null) && (senders.size() > 0)) {
            for (Sender sender : senders) {
                sender.setScripting(scripting);
                concurrentDistributor.submit(sender).get();
            }
        } else {
            throw new Exception("No email or FTP destinations could be matched for the token : " + ctx.token + "!");
        }
    }

    private void loadCustomSettings() throws Exception {
        log.debug("configFilePath = " + configFilePath);
        if (StringUtils.isNotEmpty(configFilePath)) {
            ctx.settings.loadSettings(configFilePath);
            log.debug("Custom configuration file was found and loaded '" + configFilePath + "'");
        }
    }

    private void backupFile() throws IOException {
        log.debug("filePath = " + filePath);
        File backupFile = new File(ctx.backupFolder + "/" + fileName);
        if (!backupFile.exists()) FileUtils.copyFile(new File(filePath), backupFile);
    }

    private void createRequiredFolders() throws IOException {
        log.debug("createRequiredFolders()");
        ctx.outputFolder = Utils.getStringFromTemplate(ctx.settings.getOutputFolder(), ctx.variables, ctx.token);
        ctx.backupFolder = Utils.getStringFromTemplate(ctx.settings.getBackupFolder(), ctx.variables, ctx.token);
        ctx.quarantineFolder = Utils.getStringFromTemplate(ctx.settings.getQuarantineFolder(), ctx.variables, ctx.token);
        File outputDir = new File(ctx.outputFolder);
        File backupDir = new File(ctx.backupFolder);
        if (!outputDir.exists()) FileUtils.forceMkdir(outputDir);
        if (!backupDir.exists()) FileUtils.forceMkdir(backupDir);
        ctx.variables.put(Variables.OUTPUT_FOLDER, ctx.outputFolder);
    }

    protected List<Sender> getSenders(boolean simulateDistribution) throws Exception {
        log.debug("getSenders()");
        List<Sender> senders = SenderFactory.getSenders(ctx, simulateDistribution);
        return senders;
    }

    public Map<String, String> getBurstDocumentPaths() {
        return burstDocumentPaths;
    }

    public BurstingContext getCtx() {
        return ctx;
    }
}
