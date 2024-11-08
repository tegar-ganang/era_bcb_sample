package org.gbif.checklistbank.lookup;

import org.gbif.ecat.cfg.DataDirConfig;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.BomSafeInputStreamWrapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class VocabularyFactory {

    class VocabHandler extends DefaultHandler {

        Map<String, String> terms = new HashMap<String, String>();

        private String currID;

        private void addTerm(String term) {
            if (StringUtils.isBlank(currID)) {
                log.warn("Vocabulary id for term >>" + term + "<< is blank");
            } else if (!StringUtils.isBlank(term)) {
                term = term.trim().toUpperCase();
                if (terms.containsKey(term)) {
                    if (!currID.equals(terms.get(term))) {
                        log.warn("Vocabulary term " + term + " is not unique! Used both for " + currID + " and " + terms.get(term));
                    }
                } else {
                    terms.put(term, currID);
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (localName.equalsIgnoreCase("concept")) {
                currID = attributes.getValue(DC_NS, "identifier");
                addTerm(currID);
                addTerm(attributes.getValue(DC_NS, "URI"));
            }
            if (localName.equalsIgnoreCase("term")) {
                addTerm(attributes.getValue(DC_NS, "title"));
            }
        }
    }

    private static final String DC_NS = "http://purl.org/dc/terms/";

    protected final Logger log = LoggerFactory.getLogger(VocabularyFactory.class);

    private DataDirConfig cfg;

    private HttpUtil http;

    @Inject
    public VocabularyFactory(DataDirConfig cfg, HttpUtil http) {
        super();
        this.cfg = cfg;
        this.http = http;
    }

    public Map<String, String> readVocab(String cacheFileName, URI source) {
        Map<String, String> vocab = new HashMap<String, String>();
        File cache = cfg.tmpFile("vocab-" + cacheFileName);
        try {
            File tmp = cfg.tmpFile(cacheFileName);
            http.downloadIfChanged(source.toURL(), tmp);
            FileUtils.copyFile(tmp, cache);
        } catch (Exception e) {
            log.warn("Cannot download vocabulary. Use cached version for " + source, e);
        }
        try {
            InputStream stream = new FileInputStream(cache);
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            saxFactory.setValidating(false);
            SAXParser p = saxFactory.newSAXParser();
            VocabHandler handler = new VocabHandler();
            p.parse(new BomSafeInputStreamWrapper(stream), handler);
            log.debug("Read " + handler.terms.size() + " vocabulary terms from " + source);
            vocab.putAll(handler.terms);
            stream.close();
        } catch (Exception e) {
            log.error("Error reading vocabulary " + source, e);
        }
        log.info(source + " vocabulary contains " + vocab.size() + " terms");
        return vocab;
    }
}
