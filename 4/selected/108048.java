package ru.ksu.niimm.cll.mocassin.crawl.parser.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import ru.ksu.niimm.cll.mocassin.crawl.parser.latex.LatexDocumentDAO;
import ru.ksu.niimm.cll.mocassin.crawl.parser.latex.LatexDocumentModel;
import ru.ksu.niimm.cll.mocassin.crawl.parser.latex.Parser;
import ru.ksu.niimm.cll.mocassin.util.StringUtil;
import ru.ksu.niimm.cll.mocassin.util.inject.log.InjectLogger;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class LatexDocumentDAOImpl implements LatexDocumentDAO {

    private static final String UNICODE_ENCODING = "utf8";

    private static final String CYRILLIC_ENCODING = "cp866";

    private static final String IZVESTIYA_PREFIX = "ivm";

    @InjectLogger
    private Logger logger;

    private final Parser parser;

    private final String LATEX_DOCUMENT_DIR;

    private final String PATCHED_LATEX_DOCUMENT_DIR;

    @Inject
    public LatexDocumentDAOImpl(Parser parser, @Named("patched.tex.document.dir") String patchedTexDocumentDir, @Named("tex.document.dir") String texDocumentDir) {
        this.parser = parser;
        this.PATCHED_LATEX_DOCUMENT_DIR = patchedTexDocumentDir;
        LATEX_DOCUMENT_DIR = texDocumentDir;
    }

    @Override
    public LatexDocumentModel load(String documentId) {
        String encoding = detectEncoding(documentId);
        String filename = StringUtil.arxivid2filename(documentId, "tex");
        LatexDocumentModel model = null;
        try {
            model = parser.parse(documentId, new FileInputStream(String.format("%s/%s", PATCHED_LATEX_DOCUMENT_DIR, filename)), encoding, true);
        } catch (FileNotFoundException e) {
            logger.error("Couldn't load a Latex document with id='{}'; an empty model will be returned", documentId, e);
        }
        return model;
    }

    @Override
    public void save(String arxivId, InputStream inputStream, String encoding) {
        String filename = StringUtil.arxivid2filename(arxivId, "tex");
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(String.format("%s/%s", LATEX_DOCUMENT_DIR, filename)), encoding);
            IOUtils.copy(inputStream, writer, encoding);
            writer.flush();
            writer.close();
            inputStream.close();
        } catch (IOException e) {
            logger.error("Failed to save the Latex source with id='{}'", arxivId, e);
            throw new RuntimeException(e);
        }
    }

    private String detectEncoding(String documentId) {
        return documentId.contains(IZVESTIYA_PREFIX) ? CYRILLIC_ENCODING : UNICODE_ENCODING;
    }
}
