package ar.uba.fi.tonyvaliente.documents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.io.FileUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import ar.uba.fi.tonyvaliente.commands.CleanResult;
import ar.uba.fi.tonyvaliente.documents.exception.QueryException;
import ar.uba.fi.tonyvaliente.text.extraction.NodeExtractor;
import ar.uba.fi.tonyvaliente.text.extraction.TextExtractor;
import ar.uba.fi.tonyvaliente.text.filtering.AcutesFilter;
import ar.uba.fi.tonyvaliente.text.filtering.CaseFoldingFilter;
import ar.uba.fi.tonyvaliente.text.filtering.SkeepDuplicatedFilter;
import ar.uba.fi.tonyvaliente.text.filtering.StemmingFilter;
import ar.uba.fi.tonyvaliente.text.filtering.StopWordsFilter;
import ar.uba.fi.tonyvaliente.text.filtering.TermsFilter;
import ar.uba.fi.tonyvaliente.text.filtering.TextFilter;
import ar.uba.fi.tonyvaliente.utils.MD5;
import ar.uba.fi.tonyvaliente.utils.RecursosAplicacion;

public class DocumentManager {

    private static final String DOCS_ROOT = RecursosAplicacion.instance().getProperty("docs.root");

    public static CreateIndexResult createIndex(int keys) {
        try {
            File directory = new File(DOCS_ROOT + "/");
            if (directory.exists()) {
            } else {
                directory.mkdirs();
            }
            DocumentHashing documentHashing = new DocumentHashing();
            documentHashing.initilize();
            documentHashing.close();
            Index documentIndex = new DocumentIndex();
            documentIndex.initialize(keys);
            documentIndex.close();
            return new CreateIndexResult(true, "Success!!!");
        } catch (Exception e) {
            e.printStackTrace();
            return new CreateIndexResult(false, e.getMessage());
        }
    }

    public static AddDocumentResult addDocument(String path) {
        String sourcePath = path;
        String documentHash = MD5.calculateHash(path);
        String targetPath = documentHash;
        if (targetPath.equals("")) return new AddDocumentResult(false, "MD5 invalid hash");
        try {
            File source = new File(sourcePath);
            File target = new File(DOCS_ROOT + "/" + targetPath.substring(0, 1) + "/" + targetPath.substring(1, 2), targetPath);
            path = target.getAbsolutePath();
            if (target.exists()) {
            }
            FileUtils.copyFile(source, target);
        } catch (Exception e) {
            e.printStackTrace();
            return new AddDocumentResult(false, e.getMessage());
        }
        String title = sourcePath;
        NodeList bodyList = new NodeList();
        try {
            Parser parser = new Parser();
            NodeList tags;
            parser.setResource(path);
            tags = parser.parse(null);
            NodeFilter filter;
            NodeList headList = new NodeList();
            filter = new TagNameFilter("HEAD");
            for (NodeIterator it = tags.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(headList, filter);
            NodeList titleList = new NodeList();
            filter = new TagNameFilter("TITLE");
            for (NodeIterator it = headList.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(titleList, filter);
            if (titleList.size() > 0) {
                try {
                    title = titleList.elementAt(0).getChildren().elementAt(0).getText();
                } catch (Exception e) {
                }
            }
            filter = new TagNameFilter("BODY");
            for (NodeIterator it = tags.elements(); it.hasMoreNodes(); ) it.nextNode().collectInto(bodyList, filter);
        } catch (ParserException e) {
            e.printStackTrace();
            return new AddDocumentResult(false, e.getMessage());
        }
        NodeList filteredNodes = performNodeFiltering(bodyList);
        LinkedList<String> extractedText = performTextExtraction(filteredNodes);
        LinkedList<String> filteredText = performTextFiltering(extractedText);
        DocumentHashing documentHashing = new DocumentHashing();
        int documentId = documentHashing.addDocument(documentHash, title);
        documentHashing.close();
        Index documentIndex = new DocumentIndex();
        documentIndex.open();
        for (String term : filteredText) {
            try {
                documentIndex.add(term, documentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        documentIndex.close();
        return new AddDocumentResult(true, "Success!!!");
    }

    public static QueryDocumentsResult queryDocuments(String strQuery, boolean showDetails) {
        try {
            QueryDocumentsResult result = new QueryDocumentsResult();
            DocumentHashing docHash = new DocumentHashing();
            docHash.ensurePersitence();
            DocumentIndex index = new DocumentIndex();
            index.open();
            int docNumber = 0;
            DocumentQueryFactory qf = createQueryFactory(index, docHash);
            DocumentQuery query = (DocumentQuery) qf.createBooleanQuery(strQuery);
            Iterator<Integer> idIterator = query.getResultIterator();
            DocumentInfo info = null;
            final int MAX_BUFFER_SIZE = 64;
            ArrayList<Integer> idBuffer = new ArrayList<Integer>();
            ArrayList<DocumentInfo> infoBuffer = new ArrayList<DocumentInfo>();
            idBuffer.ensureCapacity(MAX_BUFFER_SIZE);
            infoBuffer.ensureCapacity(MAX_BUFFER_SIZE);
            while (idIterator.hasNext()) {
                Integer idIteratorNext = idIterator.next();
                info = docHash.getDocumentInfo(idIteratorNext);
                idBuffer.add(idIteratorNext);
                if (idBuffer.size() == MAX_BUFFER_SIZE) {
                    for (Integer id : idBuffer) {
                        info = docHash.getDocumentInfo(id);
                        infoBuffer.add(info);
                    }
                    for (int i = 0; i < idBuffer.size(); i++) {
                        Integer id = idBuffer.get(i);
                        info = infoBuffer.get(i);
                        String message = getDocumentText(id, info, ++docNumber, showDetails);
                        result.addResult(id);
                        result.addMessage(message);
                    }
                    idBuffer.clear();
                    infoBuffer.clear();
                }
            }
            if (!idBuffer.isEmpty()) {
                for (Integer id : idBuffer) {
                    info = docHash.getDocumentInfo(id);
                    infoBuffer.add(info);
                }
                for (int i = 0; i < idBuffer.size(); i++) {
                    Integer id = idBuffer.get(i);
                    info = infoBuffer.get(i);
                    String message = getDocumentText(id, info, ++docNumber, showDetails);
                    result.addResult(id);
                    result.addMessage(message);
                }
                idBuffer.clear();
                infoBuffer.clear();
            }
            index.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new QueryDocumentsResult();
        }
    }

    private static String getDocumentText(Integer documentId, DocumentInfo documentInfo, int docNumber, boolean showDetails) {
        StringBuffer message = new StringBuffer();
        message.append(documentInfo.getTitle());
        message.append(" (Document #");
        message.append(documentId);
        message.append(")");
        if (showDetails) {
            try {
                String path = documentInfo.getHash();
                File file = new File(DOCS_ROOT + "/" + path.substring(0, 1) + "/" + path.substring(1, 2), path);
                if (file.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    int appended = 0;
                    while ((line = reader.readLine()) != null) {
                        message.append(line);
                        message.append("\n");
                        appended++;
                        if (appended == 6) break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            message.append("\n");
        }
        message.append("\n");
        return message.toString();
    }

    public static Document getDocument(int documentId) {
        DocumentHashing documentHashing = new DocumentHashing();
        DocumentInfo documentInfo = documentHashing.getDocumentInfo(documentId);
        documentHashing.close();
        String documentHash = documentInfo.getHash();
        File file = new File(DOCS_ROOT + "/" + documentHash.substring(0, 1) + "/" + documentHash.substring(1, 2), documentHash);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new Document(documentId, buffer.toString());
    }

    public static void dump(String path) {
        boolean needClose = false;
        PrintStream dump = System.out;
        if (path != null) {
            try {
                dump = new PrintStream(new FileOutputStream(path));
                needClose = true;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        dump.println("");
        dump.println("Index Dump");
        dump.println("------------------------------------------------------------");
        dump.println("");
        DocumentIndex documentIndex = new DocumentIndex();
        documentIndex.open();
        DocumentHashing documentHashing = new DocumentHashing();
        int termCount = 0;
        for (Iterator<String> it = documentIndex.getTermsIterator(); it.hasNext(); ) {
            String term = it.next();
            if (term == null) continue;
            dump.print("Term:      #");
            dump.print(++termCount);
            dump.print(" ");
            dump.println(term);
            boolean first = true;
            LinkedList<Integer> documents = documentIndex.getDocuments(term);
            for (Integer documentId : documents) {
                if (first) dump.print("Documents: "); else dump.print("           ");
                first = false;
                dump.print(documentId);
                DocumentInfo documentInfo = documentHashing.getDocumentInfo(documentId.intValue());
                dump.print(" {hash:");
                dump.print(documentInfo.getHash());
                dump.print(", title:");
                dump.print(documentInfo.getTitle());
                dump.println("}");
            }
            if (documents.size() == 0) dump.println("NOT DOCUMENTS FOUND!!!");
            dump.println("------------------------------------------------------------");
        }
        documentHashing.close();
        documentIndex.close();
        if (needClose) dump.close();
    }

    public static NodeList performNodeFiltering(NodeList nodes) {
        nodes.keepAllNodesThatMatch(new NotFilter(new TagNameFilter("script")), true);
        return nodes;
    }

    public static LinkedList<String> performTextExtraction(NodeList nodes) {
        LinkedList<NodeExtractor> extractors = new LinkedList<NodeExtractor>();
        extractors.add(new TextExtractor());
        return performTextExtraction(nodes, extractors);
    }

    private static LinkedList<String> performTextExtraction(NodeList nodes, LinkedList<NodeExtractor> extractors) {
        LinkedList<String> extracted = new LinkedList<String>();
        for (SimpleNodeIterator nodesIter = nodes.elements(); nodesIter.hasMoreNodes(); ) {
            Node node = nodesIter.nextNode();
            for (NodeExtractor extractor : extractors) {
                LinkedList<String> result = extractor.extract(node);
                if (result != null) {
                    extracted.addAll(result);
                }
            }
            NodeList childrens = node.getChildren();
            if (childrens != null) {
                LinkedList<String> result = performTextExtraction(childrens, extractors);
                if (result != null) {
                    extracted.addAll(result);
                }
            }
        }
        return extracted;
    }

    public static LinkedList<String> performTextFiltering(LinkedList<String> extractedText) {
        LinkedList<TextFilter> filters = new LinkedList<TextFilter>();
        filters.add(new TermsFilter());
        filters.add(new CaseFoldingFilter());
        filters.add(new AcutesFilter());
        filters.add(new SkeepDuplicatedFilter());
        filters.add(new StopWordsFilter());
        filters.add(new StemmingFilter());
        return performTextFiltering(extractedText, filters);
    }

    private static LinkedList<String> performTextFiltering(LinkedList<String> extractedText, LinkedList<TextFilter> filters) {
        LinkedList<String> filtered = extractedText;
        for (TextFilter filter : filters) filtered = filter.filter(filtered);
        return filtered;
    }

    /**
	 * Crea una nueva QueryFactory vinculada al indice invertido
	 * de documentos.
	 * @return Nueva QueryFactory vinculada al indice invertido
	 * de documentos
	 */
    static DocumentQueryFactory createQueryFactory(DocumentIndex index, DocumentHashing docHash) {
        return new DocumentQueryFactory(index, docHash);
    }

    public static CleanResult clean() {
        File directory = new File(DOCS_ROOT + "/");
        if (directory.exists()) {
            try {
                FileUtils.cleanDirectory(directory);
            } catch (IOException e) {
                e.printStackTrace();
                return new CleanResult(true, false, e.getMessage());
            }
        }
        return new CleanResult(true, true, "Success!!");
    }
}

/**
 * QueryFactory vinculada al indice invertido de documentos 
 * @author dmorello
 */
class DocumentQueryFactory extends QueryFactory {

    protected DocumentHashing docHash;

    public DocumentQueryFactory(DocumentIndex index, DocumentHashing docHash) {
        super(index);
        this.docHash = docHash;
    }

    @Override
    public Query createBooleanQuery(String strQuery) throws QueryException {
        return DocumentBooleanQuery.newInstance(strQuery, this);
    }
}
