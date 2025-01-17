package services.crawler;

import java.io.IOException;
import java.net.URL;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class DocxDownloader implements Downloader {

    private static boolean releaseReady = false;

    public StringBuffer get(URL url) throws IOException {
        String contents = null;
        if (releaseReady) {
            try {
                XWPFDocument docxDocument = new XWPFDocument(url.openStream());
                XWPFWordExtractor wordExtractor = new XWPFWordExtractor(docxDocument);
                contents = new String(wordExtractor.getText().getBytes(), "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else System.err.println("Docx format not supported yet.");
        return new StringBuffer(contents);
    }
}
