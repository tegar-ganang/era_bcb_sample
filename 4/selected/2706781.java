package com.smartwish.documentburster.engine.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.smartwish.documentburster.settings.Settings;
import com.smartwish.documentburster.utils.GenUtils;
import com.smartwish.documentburster.utils.Utils;
import com.smartwish.documentburster.variables.Variables;

public class Merger {

    protected Log log = LogFactory.getLog(Merger.class);

    private Settings settings;

    private Variables variables;

    private String outputFolder;

    private String backupFolder;

    private String outputFileName;

    private List<InputStream> streamOfPDFFiles = new ArrayList<InputStream>();

    public Merger(Settings settings) {
        this.settings = settings;
    }

    private void createFoldersAndPrepare(String[] filePaths) throws IOException {
        log.debug("filePaths = " + Arrays.toString(filePaths));
        if (StringUtils.isEmpty(outputFileName)) outputFileName = settings.getMergeFileName();
        if (StringUtils.isEmpty(outputFileName)) outputFileName = "merged.pdf";
        outputFolder = Utils.getStringFromTemplate(settings.getOutputFolder(), variables, "");
        backupFolder = Utils.getStringFromTemplate(settings.getBackupFolder(), variables, "") + "/merge/files";
        File outputDir = new File(outputFolder);
        if (!outputDir.exists()) FileUtils.forceMkdir(outputDir);
        File backupDir = new File(backupFolder);
        if (!backupDir.exists()) FileUtils.forceMkdir(backupDir);
        for (String filePath : filePaths) {
            String fileName = FilenameUtils.getName(filePath);
            File file = new File(backupFolder + "/" + fileName);
            FileUtils.copyFile(new File(filePath), file);
            streamOfPDFFiles.add(new FileInputStream(file));
        }
    }

    public String doMerge(String[] filePaths, String outputFileName) throws Exception {
        log.debug("filePaths = " + Arrays.toString(filePaths) + ", outputFileName = " + outputFileName);
        this.outputFileName = outputFileName;
        variables = new Variables(outputFileName, settings.getLanguage(), settings.getCountry(), settings.getNumberOfUserVariables());
        createFoldersAndPrepare(filePaths);
        String noMetaDataFilePath = outputFolder + "/" + FilenameUtils.getBaseName(outputFileName) + "_no_metadata.pdf";
        List<InputStream> pdfs = streamOfPDFFiles;
        Iterator<InputStream> iteratorPDFs = pdfs.iterator();
        List<PdfReader> readers = new ArrayList<PdfReader>();
        while (iteratorPDFs.hasNext()) {
            InputStream pdf = iteratorPDFs.next();
            PdfReader pdfReader = new PdfReader(pdf);
            readers.add(pdfReader);
            pdf.close();
        }
        OutputStream outputStream = new FileOutputStream(new File(noMetaDataFilePath));
        Document document = new Document();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            int pageOfCurrentReaderPDF = 0;
            Iterator<PdfReader> iteratorPDFReaders = readers.iterator();
            while (iteratorPDFReaders.hasNext()) {
                PdfReader pdfReader = iteratorPDFReaders.next();
                while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
                    document.newPage();
                    pageOfCurrentReaderPDF++;
                    PdfImportedPage page = writer.getImportedPage(pdfReader, pageOfCurrentReaderPDF);
                    cb.addTemplate(page, 0, 0);
                }
                pageOfCurrentReaderPDF = 0;
                pdfReader.close();
            }
            outputStream.flush();
        } finally {
            if (document.isOpen()) document.close();
            if (outputStream != null) outputStream.close();
        }
        return changeMetadata(noMetaDataFilePath);
    }

    private String changeMetadata(String noMetaDataFilePath) throws Exception {
        log.debug("noMetaDataFilePath = " + noMetaDataFilePath);
        String productName = Utils.getProductName(settings.getVersion());
        PdfReader reader = new PdfReader(noMetaDataFilePath);
        String mergedFilePath = outputFolder + "/" + outputFileName;
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(mergedFilePath));
        Map<String, String> info = GenUtils.safeCastMap(reader.getInfo(), String.class, String.class);
        info.put("Creator", productName);
        stamper.setMoreInfo((HashMap<String, String>) info);
        stamper.close();
        reader.close();
        FileUtils.deleteQuietly(new File(noMetaDataFilePath));
        return mergedFilePath;
    }

    public String getBackupFolder() {
        return backupFolder;
    }

    public String getOutputFolder() {
        return outputFolder;
    }
}
