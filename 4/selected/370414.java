package com.windsor.node.plugin.windsor.log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import com.windsor.node.common.domain.CommonContentType;
import com.windsor.node.common.domain.CommonTransactionStatusCode;
import com.windsor.node.common.domain.DataServiceRequestParameter;
import com.windsor.node.common.domain.Document;
import com.windsor.node.common.domain.NodeTransaction;
import com.windsor.node.common.domain.PaginationIndicator;
import com.windsor.node.common.domain.ProcessContentResult;
import com.windsor.node.common.domain.ServiceType;
import com.windsor.node.data.dao.PluginServiceParameterDescriptor;
import com.windsor.node.plugin.BaseWnosPlugin;
import com.windsor.node.service.helper.CompressionService;
import com.windsor.node.service.helper.settings.SettingServiceProvider;

public class LogFileRetriever extends BaseWnosPlugin {

    public static final String SERVICE_NAME = "RetrieveNodeLogs";

    public static final String ARCHIVE_NAME = "node_logs";

    public static final String TIMESTAMP_FORMAT = "dd-MMM-yyyy_hh.mm.a_z";

    public LogFileRetriever() {
        super();
        setPublishForEN11(false);
        setPublishForEN20(false);
        getSupportedPluginTypes().add(ServiceType.QUERY);
        debug("Plugin instantiated");
    }

    public ProcessContentResult process(NodeTransaction transaction) {
        ProcessContentResult result = new ProcessContentResult();
        result.setSuccess(false);
        result.setStatus(CommonTransactionStatusCode.Failed);
        result.getAuditEntries().add(makeEntry("Preparing to retrieve log files..."));
        try {
            result.getAuditEntries().add(makeEntry("Validating transaction..."));
            validateTransaction(transaction);
            result.getAuditEntries().add(makeEntry("Validating required helpers..."));
            SettingServiceProvider settingService = (SettingServiceProvider) getServiceFactory().makeService(SettingServiceProvider.class);
            if (settingService == null) {
                throw new RuntimeException("Unable to obtain SettingServiceProvider");
            }
            CompressionService compressionService = (CompressionService) getServiceFactory().makeService(CompressionService.class);
            if (compressionService == null) {
                throw new RuntimeException("Unable to obtain CompressionService");
            }
            result.getAuditEntries().add(makeEntry("copying log files..."));
            String tempDirName = settingService.getTempDir().getAbsolutePath();
            File workDir = new File(FilenameUtils.concat(tempDirName, "log.tmp"));
            if (!workDir.exists()) {
                if (!workDir.mkdir()) {
                    throw new RuntimeException("Couldn't create work directory " + workDir.getAbsolutePath());
                }
            }
            File logDir = settingService.getLogDir();
            String[] logFiles = logDir.list();
            for (int i = 0; i < logFiles.length; i++) {
                File src = new File(FilenameUtils.concat(logDir.getAbsolutePath(), logFiles[i]));
                FileUtils.copyFileToDirectory(src, workDir);
            }
            result.getAuditEntries().add(makeEntry("Compressing log files..."));
            SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_FORMAT);
            String timeStamp = format.format(new Date());
            String outputFileName = ARCHIVE_NAME + "_" + timeStamp + ".zip";
            String outputFilePath = FilenameUtils.concat(tempDirName, outputFileName);
            result.getAuditEntries().add(makeEntry("Output file: " + outputFilePath));
            compressionService.zip(outputFilePath, workDir.getAbsolutePath());
            File outputFile = new File(outputFilePath);
            if (!outputFile.exists()) {
                throw new RuntimeException("Output file does not exist");
            }
            Document doc = new Document();
            result.getAuditEntries().add(makeEntry("Creating document..."));
            result.getAuditEntries().add(makeEntry("Result: " + outputFile));
            doc.setType(CommonContentType.ZIP);
            doc.setDocumentName(FilenameUtils.getName(outputFile.getAbsolutePath()));
            doc.setContent(FileUtils.readFileToByteArray(outputFile));
            result.getAuditEntries().add(makeEntry("Setting result..."));
            result.setPaginatedContentIndicator(new PaginationIndicator(transaction.getRequest().getPaging().getStart(), transaction.getRequest().getPaging().getCount(), true));
            result.getDocuments().add(doc);
            result.setSuccess(true);
            result.setStatus(CommonTransactionStatusCode.Processed);
            result.getAuditEntries().add(makeEntry("Done: OK"));
        } catch (Exception ex) {
            error(ex);
            ex.printStackTrace();
            result.setSuccess(false);
            result.setStatus(CommonTransactionStatusCode.Failed);
            result.getAuditEntries().add(makeEntry("Error while executing: " + this.getClass().getName() + "Message: " + ex.getMessage()));
        }
        return result;
    }

    public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }

    @Override
    public List<DataServiceRequestParameter> getServiceRequestParamSpecs(String serviceName) {
        return null;
    }

    @Override
    public List<PluginServiceParameterDescriptor> getParameters() {
        return new ArrayList<PluginServiceParameterDescriptor>();
    }
}
